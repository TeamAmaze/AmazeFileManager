package com.amaze.filemanager.activities.superclasses;

import android.support.v7.app.AppCompatActivity;

import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.utils.color.ColorPreference;
import com.amaze.filemanager.utils.provider.UtilitiesProvider;
import com.amaze.filemanager.utils.theme.AppTheme;

/**
 * Created by rpiotaix on 17/10/16.
 */
public class BasicActivity extends AppCompatActivity {
    private boolean initialized = false;
    private UtilitiesProvider utilsProvider;

    private void initialize() {
        utilsProvider = getAppConfig().getUtilsProvider();

        initialized = true;
    }

    protected AppConfig getAppConfig() {
        return (AppConfig) getApplication();
    }

    public ColorPreference getColorPreference() {
        if (!initialized)
            initialize();

        return utilsProvider.getColorPreference();
    }

    public AppTheme getAppTheme() {
        if (!initialized)
            initialize();

        return utilsProvider.getAppTheme();
    }

    public UtilitiesProvider getUtilsProvider() {
        return utilsProvider;
    }
}
