/*
 * Copyright (C) 2015 Jorge Ruesga
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

package com.ruesga.android.wallpapers.photophase;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.TaskDescription;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.ArrayRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.util.DisplayMetrics;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.providers.TemporaryContentAccessProvider;

import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * A helper class with useful methods for deal with android.
 */
public final class AndroidHelper {

    private static final String TAG = "AndroidHelper";

    /**
     * This method converts dp unit to equivalent device specific value in pixels.
     *
     * @param ctx The current context
     * @param dp A value in dp (Device independent pixels) unit
     * @return float A float value to represent Pixels equivalent to dp according to device
     */
    public static float convertDpToPixel(Context ctx, float dp) {
        Resources resources = ctx.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    /**
     * Method that returns if the device is running jellybean or greater
     *
     * @return boolean true if is running jellybean or greater
     */
    public static boolean isJellyBeanOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    /**
     * Method that returns if the device is running jellybean MR1 or greater
     *
     * @return boolean true if is running jellybean MR1 or greater
     */
    public static boolean isJellyBeanMr1OrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    /**
     * Method that returns if the device is running kitkat or greater
     *
     * @return boolean true if is running kitkat or greater
     */
    public static boolean isKitKatOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * Method that returns if the device is running lollipop or greater
     *
     * @return boolean true if is running lollipop or greater
     */
    public static boolean isLollipopOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * Method that returns if the device is running marshmallow or greater
     *
     * @return boolean true if is running marshmallow or greater
     */
    public static boolean isMarshmallowOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * Method that returns if the device is running nougat or greater
     *
     * @return boolean true if is running nougat or greater
     */
    public static boolean isNougatOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    /**
     * Calculate the dimension of the status bar
     *
     * @param context The current context
     * @return The height of the status bar
     */
    public static int calculateStatusBarHeight(Context context) {
        // CyanogenMod specific featured (DO NOT RELAY IN INTERNAL VARS)
        boolean hiddenStatusBar = Settings.System.getInt(context.getContentResolver(),
                "expanded_desktop_state", 0) == 1 && Settings.System.getInt(
                context.getContentResolver(), "expanded_desktop_style", 0) == 2;

        // On kitkat we can use the translucent bars to fill all the screen
        int result = 0;
        if (!isKitKatOrGreater() && !hiddenStatusBar && !(context instanceof Activity)) {
            int resourceId = context.getResources().getIdentifier(
                    "status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = context.getResources().getDimensionPixelSize(resourceId);
            }
        }
        return result;
    }

    /**
     * Method that restart the wallpaper
     */
    public static void restartWallpaper() {
        // Restart the service
        Process.killProcess(Process.myPid());
    }

    @TargetApi(value=Build.VERSION_CODES.JELLY_BEAN)
    public static boolean hasReadExternalStoragePermissionGranted(Context context) {
        // We only are interested in MM permissions model. In other cases we explicit have
        // this permission granted by the system
        return !isMarshmallowOrGreater() || ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return !(info == null || !info.isConnectedOrConnecting() || !info.isAvailable());
    }

    public static Pair<String[], String[]> sortEntries(
            Context context, @ArrayRes int labelsResId, @ArrayRes int valuesResId) {
        String[] labels = context.getResources().getStringArray(labelsResId);
        String[] values = context.getResources().getStringArray(valuesResId);
        int count = labels.length;
        List<Pair<String, String>> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Pair<>(labels[i], values[i]));
        }
        final Collator collator = Collator.getInstance(getLocale(context.getResources()));
        Collections.sort(entries, new Comparator<Pair<String, String>>() {
            @Override
            public int compare(Pair<String, String> lhs, Pair<String, String> rhs) {
                // Default value is position at the top most, the rest are sorted by name
                if (lhs.second.equals("0") && rhs.second.equals("0")) {
                    return 0;
                }
                if (lhs.second.equals("0")) {
                    return -1;
                }
                if (rhs.second.equals("0")) {
                    return 1;
                }
                return collator.compare(lhs.first, rhs.first);
            }
        });

        for (int i = 0; i < count; i++) {
            labels[i] = entries.get(i).first;
            values[i] = entries.get(i).second;
        }
        return new Pair<>(labels, values);
    }

    @SuppressLint("PrivateApi")
    public static void tryRegisterActivityDestroyListener(
            PreferenceManager pm, PreferenceManager.OnActivityDestroyListener listener) {
        try {
            Method method = pm.getClass().getDeclaredMethod("registerOnActivityDestroyListener",
                    PreferenceManager.OnActivityDestroyListener.class);
            method.setAccessible(true);
            method.invoke(pm, listener);
        } catch (Exception e) {
            // ignored, nothing we can do
        }
    }

    public static void sharePicture(Context context, Uri uri) {
        // Send the image
        try {
            context.startActivity(getSharePictureIntent(uri));
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Send action not found for " + uri.toString(), ex);
        }
    }

    public static Intent getSharePictureIntent(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setType("image/*");
        Uri temporaryUri = TemporaryContentAccessProvider.createAuthorizationUri(uri);
        intent.putExtra(Intent.EXTRA_STREAM, temporaryUri);
        return intent;
    }

    @SuppressWarnings("deprecation")
    public static Locale getLocale(Resources res) {
        if (isNougatOrGreater()) {
            return res.getConfiguration().getLocales().get(0);
        }
        return res.getConfiguration().locale;
    }

    @TargetApi(value=Build.VERSION_CODES.LOLLIPOP)
    public static void setupRecentBar(Activity activity) {
        if (isLollipopOrGreater()) {
            int color = ContextCompat.getColor(activity, R.color.color_primary);
            if (Color.alpha(color) != 255) {
                // Remove alpha color. TaskDescription needs an opaque color
                color = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
            }
            TaskDescription taskDesc = new TaskDescription(
                    activity.getString(R.string.app_name), null, color);
            activity.setTaskDescription(taskDesc);
        }
    }

    public static Boolean sHighEndDevice = null;
    public static boolean isHighEndDevice(Context context) {
        if (sHighEndDevice != null) {
            return sHighEndDevice;
        }
        if (AndroidHelper.isJellyBeanOrGreater()) {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            // Devices with 1Gb or more RAM
            sHighEndDevice = (mi.totalMem / 1073741824) >= 1;
        } else {
            sHighEndDevice = Boolean.TRUE;
        }
        return sHighEndDevice;
    }
}
