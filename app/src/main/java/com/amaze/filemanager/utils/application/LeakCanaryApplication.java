package com.amaze.filemanager.utils.application;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * @author Emmanuel
 *         on 28/8/2017, at 18:12.
 */

public class LeakCanaryApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();/*
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);*/
    }

}