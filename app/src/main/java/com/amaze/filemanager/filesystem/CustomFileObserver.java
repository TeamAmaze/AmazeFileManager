package com.amaze.filemanager.filesystem;

import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class which monitors any change in local filesystem and updates the adapter
 * Makes use of inotify in Linux
 */
public class CustomFileObserver extends FileObserver {

    /**
     * Values for what of Handler Message
     */
    public static final int GOBACK = -1, NEW_ITEM = 0, DELETED_ITEM = 1;

    /**
     * When the bserver stops observing this event is recieved
     * Check: http://rswiki.csie.org/lxr/http/source/include/linux/inotify.h?a=m68k#L45
     */
    private static final int IN_IGNORED = 0x00008000;
    private static final int DEFER_CONSTANT = 5000;
    private static final int MASK = CREATE | MOVED_TO | DELETE | MOVED_FROM | DELETE_SELF | MOVE_SELF;

    private long lastMessagedTime = 0L;
    private boolean messagingScheduled = false;
    private boolean wasStopped = false;

    private Handler handler;
    private String path;
    private ArrayList<String> pathsAdded = new ArrayList<>();
    private ArrayList<String> pathsRemoved = new ArrayList<>();

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
    public void stopWatching() {
        wasStopped = true;
        super.stopWatching();
    }

    @Override
    public void onEvent(int event, String path) {
        if(event == IN_IGNORED) {
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

        if(messagingScheduled) return;

        if(deltaTime <= DEFER_CONSTANT) {
            // defer the observer until unless it reports a change after at least 5 secs of last one
            // keep adding files added, if there were any, to the buffer

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    sendMessages();
                }
            }, DEFER_CONSTANT - deltaTime);

            messagingScheduled = true;
        } else {
            sendMessages();
        }
    }

    private void sendMessages() {
        lastMessagedTime = Calendar.getInstance().getTimeInMillis();
        messagingScheduled = false;

        for (String pathAdded : pathsAdded) {
            handler.obtainMessage(NEW_ITEM, pathAdded).sendToTarget();
        }
        pathsAdded.clear();

        for (String pathRemoved : pathsRemoved) {
            handler.obtainMessage(DELETED_ITEM, pathRemoved).sendToTarget();
        }
        pathsRemoved.clear();
    }

}