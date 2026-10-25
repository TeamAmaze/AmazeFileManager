/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.services.ftp

import com.amaze.filemanager.ui.fragments.FtpServerFragment
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Replacement event bus to handle [FtpService] events using Kotlin's Flow.
 *
 * Original idea: https://mirchev.medium.com/its-21st-century-stop-using-eventbus-3ff5d9c6a00f
 *
 * @see [FtpService]
 * @see [FtpTileService]
 * @see [FtpServerFragment]
 */
object FtpEventBus {
    private val _events = MutableSharedFlow<FtpService.FtpReceiverActions>(replay = 0)
    val events = _events.asSharedFlow()

    /**
     * Emit the event signal to the event bus as [MutableSharedFlow].
     *
     * @param event The event to be emitted.
     */
    suspend fun emit(event: FtpService.FtpReceiverActions) {
        _events.emit(event)
    }
}
