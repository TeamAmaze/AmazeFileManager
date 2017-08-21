package com.amaze.filemanager.utils;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Copyright (C) 2017 Sanzhar Zholdiyarov <zholdiyarov@gmail.com>
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

public class AsyncFeature {
	private final Handler handler;
	private final static String HANDLER_NAME = "com-amaze-filemanager-thread";
	private AtomicBoolean isStarted = new AtomicBoolean();
	private final Queue<Runnable> pendingTasks = new ConcurrentLinkedQueue<>();
	
	public static AsyncFeature newInstance(Context context) {
		final HandlerThread looperThread = new HandlerThread(HANDLER_NAME);
		looperThread.start();
		
		Handler handler = new Handler(looperThread.getLooper());
		
		return new AsyncFeature(handler);
	}
	
	private AsyncFeature(Handler handler) {
		this.handler = handler;
	}
	
	public void execute(Runnable runnable) {
		pendingTasks.add(runnable);
		if (!isStarted.getAndSet(true)) {
			runPending();
		}
	}
	
	
	public boolean isRunning() {
		return isStarted.get();
	}
	
	public boolean cancel() {
		pendingTasks.clear();
		isStarted.set(false);
		
		for (Runnable task : pendingTasks) {
			handler.removeCallbacks(task);
		}
		return true;
	}
	
	private void runPending() {
		final Runnable task = pendingTasks.poll();
		if (task == null) {
			isStarted.set(false);
			return;
		}
		
		task.run();
		handler.post(new Runnable() {
			@Override
			public void run() {
				runPending();
			}
		});
	}
	
	
}
