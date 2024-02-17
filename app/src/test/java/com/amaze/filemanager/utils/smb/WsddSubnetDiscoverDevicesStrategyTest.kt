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

import com.amaze.filemanager.test.randomBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import java.util.*
import kotlin.text.Charsets.UTF_8

/**
 * Test [WsddDiscoverDeviceStrategy].
 */
@Suppress("StringLiteralDuplication")
class WsddSubnetDiscoverDevicesStrategyTest : AbstractSubnetDiscoverDevicesStrategyTests() {

    private val multicastResponseTemplate = javaClass.classLoader!!
        .getResourceAsStream("wsdd/multicast-response.txt")
        .reader(UTF_8).readText()

    private val wsdResponseTemplate = javaClass.classLoader!!
        .getResourceAsStream("wsdd/wsd-response.txt")
        .reader(UTF_8).readText()

    private val parseXmlForResponse:
        (WsddDiscoverDeviceStrategy, Any, Array<String>) -> Map<String, String> =
            { instance, xml, tags ->
                require((xml is ByteArray) or (xml is String))
                ReflectionHelpers.callInstanceMethod(
                    WsddDiscoverDeviceStrategy::class.java,
                    instance,
                    "parseXmlForResponse",
                    ReflectionHelpers.ClassParameter(xml.javaClass, xml),
                    ReflectionHelpers.ClassParameter(Array<String>::class.java, tags)
                )
            }

    private lateinit var wsdMulticastResponseMessageId: String
    private lateinit var deviceId: String

    /**
     * Test for normal parsing of multicast response
     */
    @Test
    fun testParseMulticastResponse() {
        val instance = WsddDiscoverDeviceStrategy()
        val result = parseXmlForResponse.invoke(
            instance,
            createMulticastResponse(),
            arrayOf("wsd:Types", "wsa:Address")
        )
        assertTrue(result.isNotEmpty())
        assertTrue(result.containsKey("wsd:Types"))
        assertTrue(result.containsKey("wsa:Address"))
        assertTrue(true == result["wsd:Types"]?.isNotBlank())
        assertTrue(true == result["wsa:Address"]?.isNotBlank())
    }

    /**
     * Test parsing invalid XML and/or invalid/nonexistent tags in XML.
     */
    @Test
    fun testParseInvalidMulticastResponse() {
        val instance = WsddDiscoverDeviceStrategy()
        assertTrue(parseXmlForResponse.invoke(instance, "", emptyArray()).isEmpty())
        assertTrue(parseXmlForResponse.invoke(instance, "foobar", emptyArray()).isEmpty())
        assertTrue(parseXmlForResponse.invoke(instance, "<test></test>", emptyArray()).isEmpty())
        assertTrue(
            parseXmlForResponse.invoke(
                instance,
                ByteArray(0),
                emptyArray()
            ).isEmpty()
        )
        assertTrue(
            parseXmlForResponse.invoke(
                instance,
                "foobar".toByteArray(),
                emptyArray()
            ).isEmpty()
        )
        assertTrue(
            parseXmlForResponse.invoke(
                instance,
                randomBytes(),
                emptyArray()
            ).isEmpty()
        )
    }

    /**
     * Test parsing of valid XML but with non-matching tags in XML.
     */
    @Test
    fun testParseNonMatchingMulticastResponseParams() {
        val instance = WsddDiscoverDeviceStrategy()
        assertEquals(
            0,
            parseXmlForResponse.invoke(
                instance,
                "<test></test>",
                arrayOf("foobar")
            ).size
        )
        assertEquals(
            0,
            parseXmlForResponse.invoke(
                instance,
                "<foo:bar></foo:bar>",
                arrayOf("test")
            ).size
        )
    }

    private fun createMulticastResponse(): String {
        return multicastResponseTemplate.replace(
            "##DEVICE_UUID##",
            UUID.randomUUID().toString()
        ).replace(
            "##MESSAGE_ID##",
            UUID.randomUUID().toString()
        ).replace(
            "##SRC_MESSAGE_ID##",
            UUID.randomUUID().toString()
        )
    }

    private fun generateWsdResponse(deviceName: String, workgroupName: String = "WORKGROUP") =
        wsdResponseTemplate
            .replace("##THIS_DEVICE_ID##", deviceId)
            .replace("##DEVICE_NAME##", deviceName)
            .replace("##WORKGROUP_NAME##", workgroupName)
            .replace("##PREV_MESSAGE_ID##", wsdMulticastResponseMessageId)
            .replace("##THIS_MESSAGE_ID##", UUID.randomUUID().toString())
            .toByteArray(UTF_8)
}
