package com.amaze.filemanager.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.UtilitiesProviderInterface;

/**
 * Created by rpiotaix on 17/10/16.
 */
public class BasicActivity extends AppCompatActivity implements UtilitiesProviderInterface {
    private Futils utils;

    @Override
    public Futils getFutils() {
        return utils;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utils = new Futils();
    }
}
