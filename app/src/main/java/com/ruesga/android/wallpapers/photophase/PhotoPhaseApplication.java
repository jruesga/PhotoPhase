package com.ruesga.android.wallpapers.photophase;

import android.app.Application;

import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider;

public class PhotoPhaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the application
        PreferencesProvider.reload(this);
        Colors.register(this);
    }
}
