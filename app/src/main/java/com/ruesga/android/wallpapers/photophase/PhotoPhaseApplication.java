package com.ruesga.android.wallpapers.photophase;

import android.app.Application;

public class PhotoPhaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the application
        Colors.register(this);
    }
}
