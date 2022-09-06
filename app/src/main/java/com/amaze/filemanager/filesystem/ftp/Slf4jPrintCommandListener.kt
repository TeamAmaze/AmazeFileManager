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

package com.amaze.filemanager.filesystem.ftp

import org.apache.commons.net.ProtocolCommandEvent
import org.apache.commons.net.ProtocolCommandListener
import org.apache.commons.net.SocketClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

/**
 * [ProtocolCommandListener] that logs output to a slf4j [Logger].
 *
 * Can adjust the logger level by specifying the [loggerLevel] parameter.
 */
internal class Slf4jPrintCommandListener(
    private val nologin: Boolean = true,
    private val eolMarker: Char = 0.toChar(),
    private val directionMarker: Boolean = false,
    private val loggerLevel: Level = Level.DEBUG
) :
    ProtocolCommandListener {

    private val logger: Logger = LoggerFactory.getLogger(SocketClient::class.java)

    private val logMessage: (String) -> Unit = { msg ->
        when (loggerLevel) {
            Level.INFO -> logger.info(msg)
            Level.DEBUG -> logger.debug(msg)
            Level.ERROR -> logger.error(msg)
            Level.WARN -> logger.warn(msg)
            Level.TRACE -> logger.trace(msg)
        }
    }

    override fun protocolCommandSent(event: ProtocolCommandEvent) {
        val sb = StringBuilder()
        if (directionMarker) {
            sb.append("> ")
        }
        if (nologin) {
            val cmd = event.command
            if ("PASS".equals(cmd, ignoreCase = true) || "USER".equals(cmd, ignoreCase = true)) {
                sb.append(cmd)
                sb.append(" *******") // Don't bother with EOL marker for this!
            } else {
                sb.append(getPrintableString(event.message))
            }
        } else {
            sb.append(getPrintableString(event.message))
        }
        logMessage.invoke(sb.toString())
    }

    override fun protocolReplyReceived(event: ProtocolCommandEvent) {
        val msg = if (directionMarker) {
            "< ${event.message}"
        } else {
            event.message
        }
        logMessage.invoke(msg)
    }

    private fun getPrintableString(msg: String): String {
        if (eolMarker.code == 0) {
            return msg
        }
        val pos = msg.indexOf(SocketClient.NETASCII_EOL)
        if (pos > 0) {
            val sb = StringBuilder()
            sb.append(msg.substring(0, pos))
            sb.append(eolMarker)
            sb.append(msg.substring(pos))
            return sb.toString()
        }
        return msg
    }
}
