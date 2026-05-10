/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ftpserver.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Replacement event bus to handle [FtpServerService] events using Kotlin's Flow.
 *
 * Original idea: https://mirchev.medium.com/its-21st-century-stop-using-eventbus-3ff5d9c6a00f
 *
 * @see [FtpServerService]
 */
object FtpEventBus {
    private val _events = MutableSharedFlow<FtpServerEvent>(replay = 0)
    val events = _events.asSharedFlow()

    /**
     * Emit the event signal to the event bus.
     *
     * @param event The event to be emitted.
     */
    suspend fun emit(event: FtpServerEvent) {
        _events.emit(event)
    }
}
