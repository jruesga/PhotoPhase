/*
 * Copyright (C) 2016 Jorge Ruesga
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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.media.ExifInterface;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.transition.Transition;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.ruesga.android.wallpapers.photophase.cast.CastService;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider;
import com.ruesga.android.wallpapers.photophase.tasks.AsyncPictureLoaderTask;
import com.ruesga.android.wallpapers.photophase.utils.BitmapUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoViewerActivity extends AppCompatActivity {
    private static final String TAG = "PhotoViewerActivity";

    public static final String EXTRA_PHOTO = "photo";

    private File mPhoto;

    private PhotoViewAttacher mPhotoViewAttacher;
    private ImageView mPhotoView;
    private View mDetails;
    private MenuItem mDetailsMenu;
    private MenuItem mShareMenu;
    private MenuItem mCastMenu;
    private Toolbar mToolbar;

    private AsyncPictureLoaderTask mTask;

    private boolean mHasTransition;
    private boolean mInDetails;

    private final float[] mLocation = new float[2];
    boolean mHasLocation = false;

    // To avoid passing a bitmap in an extra
    private Bitmap mThumbnail;
    private boolean mPictureLoaded;

    private ICastService mCastService;
    private NfcAdapter mNfcAdapter;

    private final ServiceConnection mCastConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mCastService = ICastService.Stub.asInterface(binder);
            boolean hasDevices = hasNearDevices();
            if (!hasDevices) {
                try {
                    mCastService.requestScan();
                } catch (RemoteException ex) {
                    // Ignore
                }
            }

            if (mCastMenu != null && mPictureLoaded) {
                mCastMenu.setVisible(hasDevices);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mCastService = null;
            if (mCastMenu != null) {
                mCastMenu.setVisible(false);
            }
        }
    };

    private final BroadcastReceiver mCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCastMenu != null && mPictureLoaded) {
                mCastMenu.setVisible(hasNearDevices());
            }
        }
    };

    private final AsyncTask<Float, Void, Bitmap> mMapLoaderTask = new AsyncTask<Float, Void, Bitmap>() {
        private static final String OPEN_STREETMAP_URL =
                "http://staticmap.openstreetmap.de/staticmap.php/";
        private static final String IMAGE_URL = OPEN_STREETMAP_URL +
                "?center=%f,%f&zoom=14&size=800x600&maptype=osmarenderer&markers=%f,%f,red-pushpin";

        @Override
        @SuppressWarnings("ConstantConditions")
        protected void onPreExecute() {
            findViewById(R.id.details_map_progress).setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(Float... params) {
            // Check if we have connectivity
            if (!AndroidHelper.isNetworkAvailable(PhotoViewerActivity.this)) {
                Log.w(TAG, "No network available. Cannot download map");
                return null;
            }

            try {
                // Obtain the map image
                String url = String.format(
                        Locale.US, IMAGE_URL, params[0], params[1], params[0], params[1]);
                Log.d(TAG, "Obtain map image from " + url);
                Request request = new Request.Builder().url(url).build();
                OkHttpClient okClient = new OkHttpClient.Builder().build();
                Response response = okClient.newCall(request).execute();

                // Extract the bitmap
                InputStream is = response.body().byteStream();
                try {
                    return BitmapUtils.decodeBitmap(is);
                } finally {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        // Ignore
                    }
                }

            } catch (IOException ex) {
                Log.w(TAG, "Failed to download map", ex);
            }
            return null;
        }

        @Override
        @SuppressWarnings("ConstantConditions")
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap == null) {
                findViewById(R.id.details_map_progress).setVisibility(View.GONE);
                findViewById(R.id.details_map_no_data).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.details_map_progress).setVisibility(View.GONE);
                ImageView iv = (ImageView) findViewById(R.id.details_map);
                iv.setImageBitmap(bitmap);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_viewer);

        if (savedInstanceState != null) {
            mHasTransition = savedInstanceState.getBoolean("has_transition", true);
        }

        if (getIntent() != null) {
            String photo = getIntent().getStringExtra(EXTRA_PHOTO);
            if (photo == null) {
                finishActivity();
                return;
            }

            mPhoto = new File(photo);
            if (!mPhoto.exists()) {
                finishActivity();
                return;
            }
        }

        initToolbar();
        AndroidHelper.setupRecentBar(this);

        // Initialize the nfc adapter if available
        if (AndroidHelper.isJellyBeanMr1OrGreater()
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        }

        mDetails = findViewById(R.id.photo_details);
        mPhotoView = (ImageView) findViewById(R.id.photo);
        if (mPhotoView != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mThumbnail = BitmapUtils.createUnscaledBitmap(
                    mPhoto, metrics.widthPixels / 8, metrics.heightPixels / 8);
            mPhotoView.setImageBitmap(mThumbnail);
            mPhotoViewAttacher = new PhotoViewAttacher(mPhotoView);
            mPhotoViewAttacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                @Override
                public void onViewTap(View view, float v, float v1) {
                    if (mToolbar != null) {
                        boolean hide = mToolbar.getAlpha() > 0.0f;
                        mToolbar.animate()
                                .translationY(hide ? mToolbar.getHeight() * -1 : 0)
                                .alpha(hide ? 0.0f : 1.0f)
                                .setDuration(350L)
                                .setInterpolator(new AccelerateInterpolator())
                                .start();
                    }
                }
            });
        }
        if (AndroidHelper.isLollipopOrGreater() && !mHasTransition) {
            addTransitionListener();
        }

        if (PreferencesProvider.Preferences.Cast.isEnabled(this)) {
            try {
                Intent i = new Intent(this, CastService.class);
                bindService(i, mCastConnection, Context.BIND_AUTO_CREATE);
            } catch (SecurityException se) {
                Log.w(TAG, "Can't bound to CastService", se);
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(CastService.ACTION_SCAN_FINISHED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mCastReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPictureLoaded) {
            Drawable drawable = mPhotoView.getDrawable();
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        }
        if (mThumbnail != null) {
            mThumbnail.recycle();
            mThumbnail = null;
        }

        if (mCastService != null) {
            unbindService(mCastConnection);
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.photoviewer, menu);
        mDetailsMenu = menu.findItem(R.id.mnu_details);
        mShareMenu = menu.findItem(R.id.mnu_share);
        mCastMenu = menu.findItem(R.id.mnu_cast);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void addTransitionListener() {
        getWindow().getSharedElementEnterTransition().addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                performAsyncPhotoLoading();
                getWindow().getSharedElementEnterTransition().removeListener(this);
            }

            @Override
            public void onTransitionEnd(Transition transition) {
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }
        });
    }

    private void performAsyncPhotoLoading() {
        if (mTask == null) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mTask = new AsyncPictureLoaderTask(this, mPhotoView, metrics.widthPixels,
                    metrics.heightPixels, new AsyncPictureLoaderTask.OnPictureLoaded() {
                @Override
                public void onPreloadImage() {
                    try {
                        Thread.sleep(600L);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }

                @Override
                public void onPictureLoaded(Object o, Drawable drawable) {
                    if (mPhotoView != null) {
                        mPhotoViewAttacher.update();
                    }

                    // Update the details information
                    updateDetailsInformation();
                    if (mDetailsMenu != null) {
                        mDetailsMenu.setVisible(true);
                    }
                    if (mShareMenu != null) {
                        mShareMenu.setVisible(true);
                    }
                    if (mCastMenu != null) {
                        mCastMenu.setVisible(hasNearDevices());
                    }
                    mPictureLoaded = true;

                    // publish the photo via nfc if available
                    shareViaNfc();
                }
            });
            mTask.execute(new File(mPhoto.getAbsolutePath()));
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Transition is not supported
        if (!AndroidHelper.isLollipopOrGreater() || !mHasTransition) {
            performAsyncPhotoLoading();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mTask != null && (mTask.getStatus() == AsyncTask.Status.RUNNING ||
                mTask.getStatus() == AsyncTask.Status.PENDING)) {
            mTask.cancel(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("has_transition", false);
    }

    private void initToolbar() {
        // Add a toolbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(" ");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                if (mInDetails) {
                    hideDetails();
                } else {
                    finishActivity();
                }
                return true;
            case R.id.mnu_share:
                AndroidHelper.sharePicture(this, Uri.fromFile(mPhoto));
                return true;
            case R.id.mnu_cast:
                try {
                    mCastService.cast(mPhoto.toString());
                } catch (RemoteException e) {
                    Log.w(TAG, "Got a remote exception while casting " + mPhoto, e);
                }
                return true;
            case R.id.mnu_details:
                displayDetails();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onBackPressed() {
        if (mInDetails) {
            hideDetails();
        } else {
            super.onBackPressed();
            finishActivity();
        }
    }

    private void finishActivity() {
        if (mPhotoViewAttacher != null) {
            mPhotoViewAttacher.cleanup();
        }
        supportFinishAfterTransition();
    }

    @SuppressWarnings("ConstantConditions")
    private void displayDetails() {
        final AnimatorListener listener = new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mDetails.setAlpha(0f);
                mDetails.setVisibility(View.VISIBLE);
                mDetailsMenu.setVisible(false);
                mShareMenu.setVisible(false);
                mCastMenu.setVisible(false);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getString(R.string.mnu_details));
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        };
        mDetails.animate().alpha(1).setListener(listener).setDuration(650L).start();
        mInDetails = true;

        // Request the image of the map
        if (mHasLocation) {
            ImageView iv = (ImageView) findViewById(R.id.details_map);
            if (iv.getDrawable() == null && mMapLoaderTask.getStatus() != AsyncTask.Status.RUNNING) {
                mMapLoaderTask.execute(mLocation[0], mLocation[1]);
            }
        }
    }

    private void hideDetails() {
        final AnimatorListener listener = new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mDetails.setAlpha(0f);
                mDetails.setVisibility(View.GONE);
                mDetailsMenu.setVisible(true);
                mShareMenu.setVisible(true);
                mCastMenu.setVisible(hasNearDevices());
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(" ");
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        };
        mDetails.animate().alpha(0).setListener(listener).setDuration(250L).start();
        mInDetails = false;
    }

    @SuppressWarnings("ConstantConditions")
    private void updateDetailsInformation() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
        DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
        DecimalFormat nf1 = new DecimalFormat("0");
        DecimalFormat nf2 = new DecimalFormat("0.#");

        String notAvailable = getString(R.string.photoviewer_details_not_available);
        String title = mPhoto.getName();
        Date datetime = null;
        String location = null;
        String manufacturer = null;
        String model = null;
        double exposure = -1d;
        double aperture = -1d;
        String iso = null;
        int flash = -1;
        int w = -1;
        int h = -1;
        int orientation = -1;

        ExifInterface exif;
        try {
            exif = new ExifInterface(mPhoto.getAbsolutePath());

            // Title
            if (AndroidHelper.isNougatOrGreater()) {
                title = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION);
                if (TextUtils.isEmpty(title)) {
                    title = exif.getAttribute(ExifInterface.TAG_USER_COMMENT);
                    if (TextUtils.isEmpty(title)) {
                        title = mPhoto.getName();
                    }
                }
            }

            // Date
            String date = exif.getAttribute(ExifInterface.TAG_DATETIME);
            if (date != null) {
                try {
                    datetime = sdf.parse(date);
                } catch (ParseException e) {
                    // Ignore
                }
            }

            // Location
            if (exif.getLatLong(mLocation)) {
                mHasLocation = true;
                if (Geocoder.isPresent()) {
                    Geocoder geocoder = new Geocoder(this, AndroidHelper.getLocale(getResources()));
                    List<Address> addresses = geocoder.getFromLocation(mLocation[0], mLocation[1], 1);
                    if (!addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        int max = address.getMaxAddressLineIndex();
                        location = "";
                        for (int i = 0; i <= max; i++) {
                            if (i > 0) {
                                location += ", ";
                            }
                            location += address.getAddressLine(i);
                        }
                    }
                }
            }

            // Camera
            manufacturer = exif.getAttribute(ExifInterface.TAG_MAKE);
            model = exif.getAttribute(ExifInterface.TAG_MODEL);
            try {
                exposure = Double.parseDouble(exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME));
                if (exposure > 0) {
                    exposure = Math.floor(1 / exposure);
                }
            } catch (NullPointerException | NumberFormatException ex) {
                // Ignore
            }
            try {
                if (AndroidHelper.isNougatOrGreater()) {
                    aperture = Double.parseDouble(exif.getAttribute(ExifInterface.TAG_F_NUMBER));
                } else {
                    //noinspection deprecation
                    aperture = Double.parseDouble(exif.getAttribute(ExifInterface.TAG_APERTURE));
                }
            } catch (NullPointerException | NumberFormatException ex) {
                // Ignore
            }
            if (AndroidHelper.isNougatOrGreater()) {
                iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS);
            } else {
                //noinspection deprecation
                iso = exif.getAttribute(ExifInterface.TAG_ISO);
            }
            flash = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1);

            // Resolution
            w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, -1);
            h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, -1);

            // Orientation
            if (orientation == ExifInterface.ORIENTATION_NORMAL) {
                orientation = 0;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                orientation = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                orientation = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                orientation = 270;
            }

        } catch (IOException ioEx) {
            Log.w(TAG, "Not exif information for "+ mPhoto.getAbsolutePath(), ioEx);
        }

        // Ensure we can have information about the picture size
        if (w <= 0 || h <= 0) {
            Rect r = BitmapUtils.getBitmapDimensions(mPhoto);
            w = r.width();
            h = r.height();
        }

        TextView tv;

        // Date
        tv = (TextView) findViewById(R.id.details_datetime);
        tv.setText(datetime == null
                ? notAvailable
                : getString(R.string.photoviewer_details_format,
                    df.format(datetime), tf.format(datetime)));

        // Location
        if (!mHasLocation) {
            findViewById(R.id.details_lat_lon).setVisibility(View.GONE);
            findViewById(R.id.details_map_block).setVisibility(View.GONE);
            tv = (TextView) findViewById(R.id.details_location);
            tv.setText(notAvailable);
        } else {
            tv = (TextView) findViewById(R.id.details_location);
            if (location == null) {
                tv.setVisibility(View.GONE);
            } else {
                tv.setText(location);
            }
            tv = (TextView) findViewById(R.id.details_lat_lon);
            tv.setText(getString(R.string.photoviewer_details_latitude_longitude,
                    mLocation[0], mLocation[1]));

            // Check if map can be open in an external app
            if (hasExternalMapApp(mLocation[0], mLocation[1])) {
                View v = findViewById(R.id.details_location_block);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openLocationInExternalApp(mLocation[0], mLocation[1]);
                    }
                });
            }
        }

        // Camera
        if (manufacturer == null && model == null ) {
            findViewById(R.id.details_camera).setVisibility(View.GONE);
        } else {
            tv = (TextView) findViewById(R.id.details_manufacturer);
            tv.setText(manufacturer == null
                    ? getString(R.string.photoviewer_details_manufacturer, notAvailable)
                    : getString(R.string.photoviewer_details_manufacturer, manufacturer));
            tv = (TextView) findViewById(R.id.details_model);
            tv.setText(model == null
                    ? getString(R.string.photoviewer_details_model, notAvailable)
                    : getString(R.string.photoviewer_details_model, model));
            tv = (TextView) findViewById(R.id.details_exposure);
            tv.setText(exposure == -1
                    ? getString(R.string.photoviewer_details_exposure, notAvailable)
                    : getString(R.string.photoviewer_details_exposure, nf1.format(exposure)));
            tv = (TextView) findViewById(R.id.details_aperture);
            tv.setText(aperture == -1
                    ? getString(R.string.photoviewer_details_aperture, notAvailable)
                    : getString(R.string.photoviewer_details_aperture, nf2.format(aperture)));
            tv = (TextView) findViewById(R.id.details_iso);
            tv.setText(iso == null
                    ? getString(R.string.photoviewer_details_iso, notAvailable)
                    : getString(R.string.photoviewer_details_iso, iso));
            tv = (TextView) findViewById(R.id.details_flash);
            tv.setText(flash == -1
                    ? getString(R.string.photoviewer_details_flash, notAvailable)
                    : getString(R.string.photoviewer_details_flash, ((flash & 0x01) == 0x1)
                        ? getString(R.string.photoviewer_details_flash_used)
                        : getString(R.string.photoviewer_details_flash_not_used)));
        }

        // Info
        tv = (TextView) findViewById(R.id.details_name);
        tv.setText(getString(R.string.photoviewer_details_name, title));
        tv = (TextView) findViewById(R.id.details_size);
        tv.setText(getString(R.string.photoviewer_details_size, mPhoto.length() / 1024));
        tv = (TextView) findViewById(R.id.details_resolution);
        if (w == -1 || h == -1) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setText(getString(R.string.photoviewer_details_resolution, w, h));
        }
        tv = (TextView) findViewById(R.id.details_orientation);
        if (orientation == -1) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setText(getString(R.string.photoviewer_details_orientation, orientation));
        }
        tv = (TextView) findViewById(R.id.details_path);
        tv.setText(getString(R.string.photoviewer_details_path, mPhoto.getParent()));

    }

    private boolean hasExternalMapApp(float latitude, float longitude) {
        String uri = String.format(Locale.ENGLISH, "geo:%f,%f", latitude, longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        PackageManager manager = getPackageManager();
        return manager.queryIntentActivities(intent, 0).size() > 0;
    }

    private void openLocationInExternalApp(float latitude, float longitude) {
        String uri = String.format(Locale.ENGLISH, "geo:%f,%f", latitude, longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    private boolean hasNearDevices() {
        if (mCastService != null) {
            try {

                return mCastService.hasNearDevices();
            } catch (RemoteException ex) {
                // Ignore
            }
        }
        return false;
    }

    @TargetApi(value=Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void shareViaNfc() {
        if (mNfcAdapter != null) {
            mNfcAdapter.setBeamPushUrisCallback(new NfcAdapter.CreateBeamUrisCallback() {
                @Override
                public Uri[] createBeamUris(NfcEvent nfcEvent) {
                    return new Uri[]{Uri.fromFile(mPhoto)};
                }
            }, this);
        }
    }
}
