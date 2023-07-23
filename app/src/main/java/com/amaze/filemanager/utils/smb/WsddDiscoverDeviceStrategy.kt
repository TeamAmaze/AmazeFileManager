/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.utils.smb

import androidx.annotation.VisibleForTesting
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.Companion.SLASH
import com.amaze.filemanager.utils.ComputerParcelable
import com.amaze.filemanager.utils.NetworkUtil
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.*

/**
 * [SmbDeviceScannerObservable.DiscoverDeviceStrategy] implementation to discover SMB devices using
 * [Web service discovery](https://en.wikipedia.org/wiki/WS-Discovery), which is used by SMBv2 or
 * above.
 *
 * Discovery method goes this way:
 * 1. send a SOAP request to multicast address 239.255.255.250 port 3702 over UDP
 * 2. for each reply as SOAP XML too, extract their URN and record the address the packets are from
 * 3. if the reply indicates sender is a computer, send a HTTP POST to the address recorded in 2, port 5357
 * 4. verify result and send [ComputerParcelable] in callback
 *
 * Implementation is after reference: https://fitzcarraldoblog.wordpress.com/2020/07/08/a-linux-command-line-utility-to-discover-and-list-wsd-enabled-computers-and-printers-on-a-home-network/
 * (Python though).
 *
 * Original implementation calls for UUIDv5 which will use hash value of the device's MAC address;
 * this implementation is not using, since MAC address poses privacy concern, and newer Androids are
 * making difficult to fetch MAC addresses anyway.
 *
 * Manually setting [multicastSocketFactory] Allows customized method to be specified for creating [MulticastSocket]
 * for convenience of testing.
 *
 * @author TranceLove <airwave209gt at gmail.com>
 */
class WsddDiscoverDeviceStrategy : SmbDeviceScannerObservable.DiscoverDeviceStrategy {

    private val multicastRequestTemplate = AppConfig.getInstance()
        .resources.openRawResource(R.raw.wsdd_discovery)
        .reader(Charsets.UTF_8).readText()

    private val wsdRequestTemplate = AppConfig.getInstance()
        .resources.openRawResource(R.raw.wsd_request)
        .reader(Charsets.UTF_8).readText()

    private val wsdRequestHeaders = mutableMapOf(
        Pair("Accept-Encoding", "Identity"),
        Pair("Connection", "Close"),
        Pair("User-Agent", "wsd")
    )

    var multicastSocketFactory: () -> MulticastSocket = DEFAULT_MULTICAST_SOCKET_FACTORY
        @VisibleForTesting
        get

        @VisibleForTesting
        set

    private val queue = OkHttpClient()

    private var cancelled = false

    override fun discoverDevices(callback: (ComputerParcelable) -> Unit) {
        multicastForDevice { addr ->
            callback.invoke(addr)
        }
    }

    @Suppress("LabeledExpression")
    private fun multicastForDevice(callback: (ComputerParcelable) -> Unit) {
        NetworkUtil.getLocalInetAddress(AppConfig.getInstance())?.let { addr ->
            val multicastAddressV4 = InetAddress.getByName(BROADCAST_IPV4)
            val multicastAddressV6 = InetAddress.getByName(BROADCAST_IPV6_LINK_LOCAL)

            while (!cancelled) {
                val socket: MulticastSocket = multicastSocketFactory.invoke()
                socket.timeToLive = 1
                socket.soTimeout = SOCKET_RECEIVE_TIMEOUT
                socket.reuseAddress = true
                socket.joinGroup(multicastAddressV4)
                socket.joinGroup(multicastAddressV6)

                // Specification said UUIDv5 which is device dependent. But random-based UUID should
                // also work here
                val tempDeviceUuid = UUID.randomUUID()
                val request = multicastRequestTemplate
                    .replace("##MY_UUID##", tempDeviceUuid.toString())
                    .toByteArray(Charsets.UTF_8)

                val requestPacket = DatagramPacket(
                    request,
                    request.size,
                    multicastAddressV4,
                    UDP_PORT
                )
                socket.send(requestPacket)

                runCatching {
                    while (!socket.isClosed) {
                        val buffer = ByteArray(4096)
                        val replyPacket = DatagramPacket(buffer, buffer.size)
                        socket.receive(replyPacket)
                        if (replyPacket.data.isNotEmpty() && replyPacket.address != null) {
                            val sentFromAddress = replyPacket.address
                            queryWithResponseAsNecessary(
                                sentFromAddress,
                                tempDeviceUuid.toString(),
                                replyPacket.data,
                                callback
                            )
                        }
                    }
                }.onFailure {
                    if (log.isWarnEnabled) log.warn("Error receiving reply", it)
                    socket.close()
                }
            }
        }
    }

    private fun queryWithResponseAsNecessary(
        sourceAddress: InetAddress,
        tempDeviceId: String,
        response: ByteArray,
        callback: (ComputerParcelable) -> Unit
    ) {
        val values = parseXmlForResponse(response, arrayOf(WSD_TYPES, WSA_ADDRESS))
        val type = values[WSD_TYPES]
        val urn = values[WSA_ADDRESS]

        if (true == type?.isNotEmpty() && true == urn?.isNotEmpty()) {
            queryEndpointForResponse(type, sourceAddress, urn, tempDeviceId, callback)
        }
    }

    private fun queryEndpointForResponse(
        type: String,
        sourceAddress: InetAddress,
        urn: String,
        tempDeviceId: String,
        callback: (ComputerParcelable) -> Unit
    ) {
        if (type.endsWith(PUB_COMPUTER)) {
            val messageId = UUID.randomUUID().toString()

            val endpoint = urn.substringAfter(URN_UUID)
            val dest =
                "http://${sourceAddress.hostAddress}:$TCP_PORT/$endpoint"
            val requestBody = wsdRequestTemplate
                .replace("##MESSAGE_ID##", "$URN_UUID$messageId")
                .replace("##DEST_UUID##", urn)
                .replace("##MY_UUID##", "$URN_UUID$tempDeviceId")
                .toRequestBody("application/soap+xml".toMediaType())
            queue.newCall(
                Request.Builder()
                    .url(dest)
                    .post(requestBody)
                    .headers(wsdRequestHeaders.toHeaders())
                    .build()
            ).execute().use { resp ->
                if (resp.isSuccessful && resp.body != null) {
                    resp.body?.run {
                        if (log.isTraceEnabled) log.trace("Response: $resp")
                        val values = parseXmlForResponse(
                            this.string(),
                            arrayOf(WSDP_TYPES, WSA_ADDRESS, PUB_COMPUTER)
                        )
                        if (PUB_COMPUTER == values[WSDP_TYPES] && urn == values[WSA_ADDRESS]) {
                            if (true == values[PUB_COMPUTER]?.isNotEmpty()) {
                                val computerName: String = values[PUB_COMPUTER].let {
                                    if (it!!.contains(SLASH)) {
                                        it.substringBefore(SLASH)
                                    } else {
                                        it
                                    }
                                }
                                callback(
                                    ComputerParcelable(sourceAddress.hostAddress, computerName)
                                )
                            }
                        }
                    }
                } else {
                    log.error("Error querying endpoint", resp)
                }
            }
        }
    }

    override fun onCancel() {
        cancelled = true
    }

    private fun parseXmlForResponse(xml: ByteArray, tags: Array<String>) =
        parseXmlForResponse(xml.toString(Charsets.UTF_8), tags)

    private fun parseXmlForResponse(xml: String, tags: Array<String>): Map<String, String> {
        if (xml.isEmpty()) {
            return emptyMap()
        } else {
            val xmlParser = XmlPullParserFactory.newInstance().also {
                it.isNamespaceAware = false
                it.isValidating = false
            }.newPullParser().also {
                it.setInput(StringReader(xml))
            }
            val retval = WeakHashMap<String, String>()
            var currentTag: String = ""
            var currentValue: String = ""
            var event = xmlParser.eventType
            try {
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        currentTag = xmlParser.name
                    } else if (event == XmlPullParser.TEXT) {
                        currentValue = xmlParser.text
                    } else if (event == XmlPullParser.END_TAG) {
                        if (tags.contains(currentTag)) {
                            retval[currentTag] = currentValue
                            currentTag = ""
                            currentValue = ""
                        }
                    }
                    event = xmlParser.next()
                }
            } catch (parseError: XmlPullParserException) {
                log.warn("Error parsing XML", parseError)
                // Combination of parsed result is required, hence it's all or nothing situation -
                // if one error found, whole XML will not be valid. Clear for "no result" answer
                retval.clear()
            }
            return retval
        }
    }

    companion object {
        private const val BROADCAST_IPV4 = "239.255.255.250"
        private const val BROADCAST_IPV6_LINK_LOCAL = "[FF02::C]"
        private const val UDP_PORT = 3702
        private const val TCP_PORT = 5357
        private const val SOCKET_RECEIVE_TIMEOUT = 60000 // 1 minute receive timeout

        private const val URN_UUID = "urn:uuid:"
        private const val WSA_ADDRESS = "wsa:Address"
        private const val WSD_TYPES = "wsd:Types"
        private const val WSDP_TYPES = "wsdp:Types"
        private const val PUB_COMPUTER = "pub:Computer"

        private val log: Logger = LoggerFactory.getLogger(WsddDiscoverDeviceStrategy::class.java)

        private val DEFAULT_MULTICAST_SOCKET_FACTORY: () -> MulticastSocket = {
            MulticastSocket()
        }
    }
}
