/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.os.Build;
import android.os.FileObserver;
import android.os.Handler;

import androidx.annotation.RequiresApi;

/**
 * Class which monitors any change in local filesystem and updates the adapter Makes use of inotify
 * in Linux
 */
public class CustomFileObserver extends FileObserver {

  /** Values for what of Handler Message */
  public static final int GOBACK = -1, NEW_ITEM = 0, DELETED_ITEM = 1;

  /**
   * When the bserver stops observing this event is recieved Check:
   * http://rswiki.csie.org/lxr/http/source/include/linux/inotify.h?a=m68k#L45
   */
  private static final int IN_IGNORED = 0x00008000;

  private static final int DEFER_CONSTANT_SECONDS = 5;
  private static final int DEFER_CONSTANT = DEFER_CONSTANT_SECONDS * 1000;
  private static final int MASK = CREATE | MOVED_TO | DELETE | MOVED_FROM | DELETE_SELF | MOVE_SELF;

  private long lastMessagedTime = 0L;
  private boolean messagingScheduled = false;
  private boolean wasStopped = false;

  private Handler handler;
  private String path;
  private final List<String> pathsAdded = Collections.synchronizedList(new ArrayList<>());
  private final List<String> pathsRemoved = Collections.synchronizedList(new ArrayList<>());

  public CustomFileObserver(String path, Handler handler) {
    super(path, MASK);
    this.path = path;
    this.handler = handler;
  }

  public boolean wasStopped() {
    return wasStopped;
  }

  public String getPath() {
    return path;
  }

  @Override
  public void startWatching() {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
      startPollingSystem();
    } else {
      super.startWatching();
    }
  }

  @Override
  public void stopWatching() {
    wasStopped = true;

    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
      stopPollingSystem();
    } else {
      super.stopWatching();
    }
  }

  @Override
  public void onEvent(int event, String path) {
    if (event == IN_IGNORED) {
      wasStopped = true;
      return;
    }

    long deltaTime = Calendar.getInstance().getTimeInMillis() - lastMessagedTime;

    switch (event) {
      case CREATE:
      case MOVED_TO:
        pathsAdded.add(path);
        break;
      case DELETE:
      case MOVED_FROM:
        pathsRemoved.add(path);
        break;
      case DELETE_SELF:
      case MOVE_SELF:
        handler.obtainMessage(GOBACK).sendToTarget();
        return;
    }

    if (deltaTime <= DEFER_CONSTANT) {
      // defer the observer until unless it reports a change after at least 5 secs of last one
      // keep adding files added, if there were any, to the buffer

      new Timer()
          .schedule(
              new TimerTask() {
                @Override
                public void run() {

                  if (messagingScheduled) return;
                  sendMessages();
                }
              },
              DEFER_CONSTANT - deltaTime);

      messagingScheduled = true;
    } else {
      if (messagingScheduled) return;
      sendMessages();
    }
  }

  private synchronized void sendMessages() {
    lastMessagedTime = Calendar.getInstance().getTimeInMillis();

    synchronized (pathsAdded) {
      for (String pathAdded : pathsAdded) {
        handler.obtainMessage(NEW_ITEM, pathAdded).sendToTarget();
      }
    }
    pathsAdded.clear();

    synchronized (pathsRemoved) {
      for (String pathRemoved : pathsRemoved) {
        handler.obtainMessage(DELETED_ITEM, pathRemoved).sendToTarget();
      }
    }
    pathsRemoved.clear();
    messagingScheduled = false;
  }

  private ScheduledExecutorService executor = null;

  /**
   * In Marshmallow FileObserver is broken, this hack will let you know of changes to a directory
   * every DEFER_CONSTANT_SECONDS seconds, calling onEvent as expected EXCEPT when moving, in such
   * cases the event will be creation (if moved into) or deletion (if moved out of) or DELETE_SELF
   * instead of MOVE_SELF.
   */
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void startPollingSystem() {
    executor = Executors.newScheduledThreadPool(1);
    executor.scheduleWithFixedDelay(
        new FileTimerTask(path, this),
        DEFER_CONSTANT_SECONDS,
        DEFER_CONSTANT_SECONDS,
        TimeUnit.SECONDS); // This doesn't work with milliseconds (don't know why)
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void stopPollingSystem() {
    executor.shutdown();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private static class FileTimerTask implements Runnable {
    private FileObserver fileObserver;
    private String[] files = null;
    private File file;

    private FileTimerTask(String path, FileObserver fileObserver) {
      file = new File(path);
      if (!file.isDirectory())
        throw new IllegalArgumentException("Illegal path, you can only watch directories!");
      files = file.list();
      this.fileObserver = fileObserver;
    }

    @Override
    public void run() {
      if (!file.exists()) {
        fileObserver.onEvent(DELETE_SELF, null);
        return;
      }
      if (!file.canRead() || !file.isHidden()) {
        fileObserver.onEvent(IN_IGNORED, null);
        return;
      }

      String[] newFiles = file.list();
      for (String s : compare(newFiles, files)) {
        fileObserver.onEvent(CREATE, s);
      }
      for (String s : compare(files, newFiles)) {
        fileObserver.onEvent(DELETE, s);
      }
    }

    private HashSet<String> compare(String[] s1, String[] s2) {
      HashSet<String> set1 = new HashSet<>(Arrays.asList(s1));
      HashSet<String> set2 = new HashSet<>(Arrays.asList(s2));
      set1.removeAll(set2);
      return set1;
    }
  }
}
