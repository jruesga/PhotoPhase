/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.wallpapers.photophase;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;

import org.cyanogenmod.wallpapers.photophase.preferences.PhotoPhasePreferences;
import org.cyanogenmod.wallpapers.photophase.preferences.PreferencesProvider;

/**
 * A testing activity to simulate the PhotoPhase Live Wallpaper inside an GLES activity.
 */
public class PhotoPhaseActivity extends Activity implements OnTouchListener {

    private static final String TAG = "PhotoPhaseActivity";

    private static final boolean DEBUG = false;

    private GLSurfaceView mGLSurfaceView;
    private PhotoPhaseRenderer mRenderer;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        boolean preserveEglCtx = getResources().getBoolean(R.bool.config_preserve_egl_context);

        // Instance the application
        PreferencesProvider.reload(this);
        Colors.register(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Configure the EGL context
        mGLSurfaceView = new GLSurfaceView(getApplicationContext());
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setEGLConfigChooser(false);
        mRenderer = new PhotoPhaseRenderer(this, new GLESSurfaceDispatcher(mGLSurfaceView));
        mGLSurfaceView.setRenderer(mRenderer);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mGLSurfaceView.setPreserveEGLContextOnPause(preserveEglCtx);
        mGLSurfaceView.setOnTouchListener(this);
        setContentView(mGLSurfaceView);

        mRenderer.onCreate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy");
        super.onDestroy();
        mRenderer.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.d(TAG, "onResume");
        mGLSurfaceView.onResume();
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        if (mRenderer != null) {
            mRenderer.onResume();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.d(TAG, "onPause");
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mRenderer.onPause();
        mGLSurfaceView.onPause();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_settings:
                Intent settings = new Intent(this, PhotoPhasePreferences.class);
                startActivity(settings);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_UP:
                mRenderer.onTouch(x, y);
                return true;

            default:
                break;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.i(TAG, "onLowMemory");
        // Pause the wallpaper and destroy the cached textures
        mRenderer.onPause();
        mRenderer.onLowMemory();
    }
}
