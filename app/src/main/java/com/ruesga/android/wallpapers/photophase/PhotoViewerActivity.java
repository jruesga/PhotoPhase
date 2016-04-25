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

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.view.MenuItem;
import android.widget.ImageView;

import com.ruesga.android.wallpapers.photophase.tasks.AsyncPictureLoaderTask;

import java.io.File;

import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoViewerActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO = "photo";

    private File mPhoto;
    PhotoViewAttacher mPhotoViewAttacher;
    private ImageView mPhotoView;
    private AsyncPictureLoaderTask mTask;

    private boolean mHasTransition;

    // To avoid passing a bitmap in a extra
    public static Bitmap sThumbnail;

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
                finish();
                return;
            }

            mPhoto = new File(photo);
            if (!mPhoto.exists()) {
                finish();
                return;
            }
        }

        initToolbar();
        mPhotoView = (ImageView) findViewById(R.id.photo);
        if (mPhotoView != null) {
            mPhotoView.setImageBitmap(sThumbnail);
            mPhotoViewAttacher = new PhotoViewAttacher(mPhotoView);
        }
        addTransitionListener();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void addTransitionListener() {
        getWindow().getSharedElementEnterTransition().addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                performAsyncPhotoLoading();
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
        mTask = new AsyncPictureLoaderTask(
                this, mPhotoView, 1024, 1024, new AsyncPictureLoaderTask.OnPictureLoaded() {
            @Override
            public void onPreloadImage() {
                try {
                    Thread.sleep(300L);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }

            @Override
            public void onPictureLoaded(Object o, Drawable drawable) {
                if (mPhotoView != null) {
                    mPhotoViewAttacher.update();
                }
            }
        });
        mTask.execute(new File(mPhoto.getPath()));
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(mPhoto.getName());
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finishActivity();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishActivity();
    }

    private void finishActivity() {
        if (mPhotoViewAttacher != null) {
            mPhotoViewAttacher.cleanup();
        }
        supportFinishAfterTransition();
    }
}
