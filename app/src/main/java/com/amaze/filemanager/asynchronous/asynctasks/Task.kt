/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.asynctasks

import androidx.annotation.MainThread
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Callable

interface Task<V, T : Callable<V>> {
    /**
     * This should return a callable to be run on a worker thread
     * The [Callable] cannot return null
     */
    fun getTask(): T

    /**
     * This function will be called on main thread if an exception is thrown
     */
    @MainThread
    fun onError(error: Throwable)

    /**
     * If the task does not return null, and doesn't throw an error this
     * function will be called with the result of the operation on main thread
     */
    @MainThread
    fun onFinish(value: V)
}

/**
 * This creates and starts a [Flowable] from a [Task].
 */
fun <V, T : Callable<V>> fromTask(task: Task<V, T>): Disposable {
    return Flowable.fromCallable(task.getTask())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(task::onFinish, task::onError)
}
