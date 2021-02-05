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

package com.amaze.filemanager.utils;

/** Created by arpitkh996 on 16-01-2016. */
import static com.amaze.filemanager.filesystem.smb.CifsContexts.SMB_URI_PREFIX;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.format.Formatter;

import jcifs.Address;
import jcifs.CIFSException;
import jcifs.NetbiosAddress;
import jcifs.context.SingletonContext;
import jcifs.smb.SmbFile;

public class SubnetScanner extends AsyncTask<Void, ComputerParcelable, Void> {

  private static final String TAG = SubnetScanner.class.getSimpleName();
  private static final int RETRY_COUNT = 5;
  private static boolean initialized = false;

  private Thread bdThread;
  private final Object mLock;
  private List<ComputerParcelable> mResults;
  private ScanObserver observer;
  private ExecutorService pool;
  private List<Future<ComputerParcelable>> tasks;
  private Context context;

  public interface ScanObserver {
    void computerFound(ComputerParcelable computer);

    void searchFinished();
  }

  class Task implements Callable<ComputerParcelable> {
    String addr;

    Task(String str) {
      this.addr = str;
    }

    public ComputerParcelable call() {
      try {
        NetbiosAddress[] allByAddress =
            SingletonContext.getInstance().getNameServiceClient().getNbtAllByAddress(this.addr);
        if (allByAddress == null || allByAddress.length <= 0) {
          return new ComputerParcelable(null, this.addr);
        }
        return new ComputerParcelable(allByAddress[0].getHostName(), this.addr);
      } catch (UnknownHostException e) {
        return new ComputerParcelable(null, this.addr);
      }
    }
  }

  public static void init() {
    Properties props = new Properties();
    props.setProperty("jcifs.resolveOrder", "BCAST");
    props.setProperty("jcifs.smb.client.responseTimeout", "30000");
    props.setProperty("jcifs.netbios.retryTimeout", "5000");
    props.setProperty("jcifs.netbios.cachePolicy", "-1");
    try {
      SingletonContext.init(props);
      initialized = true;
    } catch (CIFSException e) {
      android.util.Log.e(TAG, "Error initializing jcifs", e);
    }
  }

  public SubnetScanner(Context context) {
    this.context = context;
    mLock = new Object();
    tasks = new ArrayList<>(260);
    pool = Executors.newFixedThreadPool(60);
    mResults = new ArrayList<>();
  }

  @Override
  protected Void doInBackground(Void... voids) {

    if (!initialized) init();

    int ipAddress =
        ((WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE))
            .getConnectionInfo()
            .getIpAddress();
    if (ipAddress != 0) {
      tryWithBroadcast();
      String formatIpAddress = Formatter.formatIpAddress(ipAddress);
      String substring = formatIpAddress.substring(0, formatIpAddress.lastIndexOf(46) + 1);
      if (!isCancelled()) {
        for (ipAddress = 0; ipAddress < 100; ipAddress++) {
          this.tasks.add(this.pool.submit(new Task(substring + ipAddress)));
          this.tasks.add(this.pool.submit(new Task(substring + (ipAddress + 100))));
          if (ipAddress < 56) {
            this.tasks.add(this.pool.submit(new Task(substring + (ipAddress + 200))));
          }
        }
        while (!this.tasks.isEmpty()) {
          int size = this.tasks.size();
          int i = 0;
          while (i < size) {
            if (!isCancelled()) {
              try {
                ComputerParcelable computer =
                    (ComputerParcelable) ((Future) this.tasks.get(i)).get(1, TimeUnit.MILLISECONDS);
                this.tasks.remove(i);
                size--;
                if (computer.getName() != null) {
                  publishProgress(computer);
                }
                ipAddress = size;
              } catch (InterruptedException e) {
                return null;
              } catch (ExecutionException e2) {
                ipAddress = size;
              } catch (TimeoutException e3) {
                ipAddress = size;
              }
              i++;
              size = ipAddress;
            } else {
              return null;
            }
          }
        }
        try {
          this.bdThread.join();
        } catch (InterruptedException e4) {
        }
      } else {
        return null;
      }
    }
    synchronized (this.mLock) {
      if (this.observer != null) {
        this.observer.searchFinished();
      }
    }

    return null;
  }

  private void tryWithBroadcast() {
    this.bdThread =
        new Thread() {
          public void run() {
            for (int i = 0; i < SubnetScanner.RETRY_COUNT; i++) {
              try {
                SmbFile smbFile = SmbUtil.create(SMB_URI_PREFIX);
                smbFile.setConnectTimeout(5000);
                SmbFile[] listFiles = smbFile.listFiles();
                for (SmbFile smbFile2 : listFiles) {
                  SmbFile[] listFiles2 = smbFile2.listFiles();
                  for (SmbFile files : listFiles2) {
                    try {
                      String substring = files.getName().substring(0, files.getName().length() - 1);
                      Address byName =
                          SingletonContext.getInstance()
                              .getNameServiceClient()
                              .getByName(substring);
                      if (byName != null) {
                        publishProgress(new ComputerParcelable(substring, byName.getHostAddress()));
                      }
                    } catch (Throwable e) {

                    }
                  }
                }
              } catch (Throwable e2) {

              }
            }
          }
        };
    this.bdThread.start();
  }

  @Override
  protected void onPreExecute() {}

  @Override
  protected void onPostExecute(Void aVoid) {
    this.pool.shutdown();
  }

  @Override
  protected void onProgressUpdate(ComputerParcelable... computers) {
    for (ComputerParcelable computer : computers) {
      mResults.add(computer);
      synchronized (this.mLock) {
        if (this.observer != null) {
          this.observer.computerFound(computer);
        }
      }
    }
  }

  public void setObserver(ScanObserver scanObserver) {
    synchronized (this.mLock) {
      this.observer = scanObserver;
    }
  }

  @Override
  protected void onCancelled(Void aVoid) {
    super.onCancelled(aVoid);
    try {
      this.pool.shutdownNow();
    } catch (Throwable th) {

    }
  }

  public List<ComputerParcelable> getResults() {
    return new ArrayList<>(this.mResults);
  }
}
