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

package com.amaze.filemanager.fileoperations.filesystem.cloud

abstract class CloudStreamServer2(private val port: Int) {
//
//    protected val socket: IoAcceptor = NioSocketAcceptor()
//
//    companion object {
//        @JvmStatic
//        private val LOG = LoggerFactory.getLogger(CloudStreamServer2::class.java)
//
//        @JvmStatic
//        protected val gmtFormat: DateFormat = SimpleDateFormat(
//            "E, d MMM yyyy HH:mm:ss 'GMT'",
//            Locale.US
//        ).also {
//            it.timeZone = TimeZone.getTimeZone("GMT")
//        }
//
//        /** Some HTTP response status codes  */
//        const val HTTP_OK = "200 OK"
//        const val HTTP_PARTIALCONTENT = "206 Partial Content"
//        const val HTTP_RANGE_NOT_SATISFIABLE = "416 Requested Range Not Satisfiable"
//        const val HTTP_REDIRECT = "301 Moved Permanently"
//        const val HTTP_FORBIDDEN = "403 Forbidden"
//        const val HTTP_NOTFOUND = "404 Not Found"
//        const val HTTP_BADREQUEST = "400 Bad Request"
//        const val HTTP_INTERNALERROR = "500 Internal Server Error"
//        const val HTTP_NOTIMPLEMENTED = "501 Not Implemented"
//
//        /** Common mime types for dynamic content  */
//        const val MIME_PLAINTEXT = "text/plain"
//        const val MIME_HTML = "text/html"
//        const val MIME_DEFAULT_BINARY = "application/octet-stream"
//        const val MIME_XML = "text/xml"
//    }
//
//    // ==================================================
//    // API parts
//    // ==================================================
//    abstract fun serve(
//        uri: String,
//        method: String,
//        header: Properties,
//        params: Properties,
//        files: Properties
//    ): Response
//
//    init {
//        socket.filterChain.addLast("logger", LoggingFilter(javaClass.simpleName))
//        socket.filterChain.addLast(
//            "message",
//            ProtocolCodecFilter(HttpResponseEncoder(), HttpRequestDecoder())
//        )
//        socket.handler = HttpSessionHandler()
//        socket.sessionConfig.bothIdleTime = 10
//        socket.sessionConfig.readBufferSize = 8192
//        socket.bind(InetSocketAddress(port))
//    }
//
//    fun stop() {
//        socket.unbind()
//    }
//
//    /**
//     * Since CloudStreamServer and Streamer both uses the same port, shutdown the Streamer before
//     * acquiring the port.
//     *
//     * @return ServerSocket
//     */
//    @Throws(IOException::class)
//    private fun tryBind(port: Int): ServerSocket {
//        val socket: ServerSocket = try {
//            ServerSocket(port)
//        } catch (ifPortIsOccupiedByStreamer: BindException) {
//            Streamer.getInstance().stop()
//            ServerSocket(port)
//        }
//        return socket
//    }
//
//    /** HTTP response. Return one of these from serve().  */
//    data class Response(
//        private val status: String,
//        private val mimeType: String? = null,
//        private var data: CloudStreamSource?
//    ) {
//        /** Default constructor: response = HTTP_OK, data = mime = 'null'  */
//        constructor() : this(HTTP_OK, null, null)
//
//        /** Adds given line to the header.  */
//        fun addHeader(name: String, value: String) {
//            header[name] = value
//        }
//
//        /** Headers for the HTTP response. Use addHeader() to add lines.  */
//        var header = Properties()
//    }
//
//    class HttpRequestDecoder : TextLineDecoder(Charsets.UTF_8, LineDelimiter.AUTO) {
//        override fun writeText(session: IoSession?, text: String?, out: ProtocolDecoderOutput?) {
//            LOG.debug(text)
//            throw NotImplementedError()
//        }
//    }
//
//    class HttpResponseEncoder : ProtocolEncoderAdapter() {
//        override fun encode(session: IoSession, message: Any, out: ProtocolEncoderOutput) {
//            TODO("Not yet implemented")
//        }
//    }
//
//    class HttpSessionHandler : IoHandlerAdapter() {
//        override fun messageReceived(session: IoSession?, message: Any?) {
//            super.messageReceived(session, message)
//        }
//    }
}
