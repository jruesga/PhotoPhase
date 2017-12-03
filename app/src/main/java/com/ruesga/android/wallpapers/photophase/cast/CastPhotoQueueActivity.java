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

package com.ruesga.android.wallpapers.photophase.cast;

import android.animation.Animator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.v7.widget.helper.ItemTouchHelper.SimpleCallback;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ruesga.android.wallpapers.photophase.AndroidHelper;
import com.ruesga.android.wallpapers.photophase.ICastService;
import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider;
import com.ruesga.android.wallpapers.photophase.tasks.AsyncPictureLoaderTask;
import com.ruesga.android.wallpapers.photophase.tasks.AsyncPictureLoaderTask.AsyncPictureLoaderRunnable;
import com.ruesga.android.wallpapers.photophase.utils.BitmapUtils;
import com.ruesga.android.wallpapers.photophase.widgets.PlayPauseDrawable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CastPhotoQueueActivity extends AppCompatActivity implements OnClickListener {

    private static final String TAG = "CastPhotoQueueActivity";

    public static final String EXTRA_SHOW_DOZE_WARNING = "doze_warning";

    private class QueueViewHolder extends RecyclerView.ViewHolder {
        private ImageView mPhoto;

        public QueueViewHolder(View itemView) {
            super(itemView);
            mPhoto = itemView.findViewById(R.id.queue_photo);
        }
    }

    private class QueueAdapter extends RecyclerView.Adapter<QueueViewHolder> {
        private final List<String> mQueue;
        private final Context mContext;
        private final LayoutInflater mLayoutInflater;
        private final int mPhotoSize;
        private final OnClickListener mClickListener;

        private String mCurrent;

        public QueueAdapter(Context ctx, List<String> queue, OnClickListener clickListener) {
            mQueue = queue;
            mContext = ctx;
            mLayoutInflater = LayoutInflater.from(ctx);
            mPhotoSize = (int) ctx.getResources().getDimension(R.dimen.queue_photo_size);
            mClickListener = clickListener;
            mCurrent = null;
        }

        @Override
        public QueueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = mLayoutInflater.inflate(R.layout.cast_queue_item, parent, false);
            return new QueueViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final QueueViewHolder holder, int position) {
            synchronized (mQueue) {
                // Cancel any non finished task
                if (holder.mPhoto.getTag() != null) {
                    AsyncPictureLoaderRunnable task = (AsyncPictureLoaderRunnable) holder.mPhoto.getTag();
                    if (task != null) {
                        holder.mPhoto.removeCallbacks(task);
                        if (task.mTask.getStatus() != AsyncTask.Status.FINISHED) {
                            task.mTask.cancel(true);
                        }
                    }
                }

                // Update the item
                final String item = mQueue.get(position);
                final boolean selected = mCurrent != null && mCurrent.equals(item);
                holder.itemView.setTag(position);
                holder.itemView.setOnClickListener(mClickListener);
                if (mCache.containsKey(item)) {
                    holder.itemView.setSelected(selected);
                    holder.mPhoto.setImageBitmap(mCache.get(item));
                    holder.mPhoto.setTag(null);
                } else {
                    holder.mPhoto.setImageBitmap(null);
                    holder.itemView.setSelected(false);
                    File f = new File(item);
                    AsyncPictureLoaderTask task = new AsyncPictureLoaderTask(mContext, holder.mPhoto,
                            mPhotoSize, mPhotoSize, 2, new AsyncPictureLoaderTask.OnPictureLoaded() {
                        @Override
                        public void onPictureLoaded(Object o, Drawable drawable) {
                            holder.itemView.setSelected(selected);
                            if (drawable instanceof BitmapDrawable) {
                                mCache.put(item, ((BitmapDrawable) drawable).getBitmap());
                            }
                        }
                    });
                    task.mFactor = 1;
                    AsyncPictureLoaderRunnable runnable = new AsyncPictureLoaderRunnable(task, f);
                    ViewCompat.postOnAnimation(holder.mPhoto, runnable);
                    holder.mPhoto.setTag(runnable);
                }
            }
        }

        @Override
        public int getItemCount() {
            return mQueue.size();
        }

        public void updateCurrent(String current) {
            mCurrent = current;
        }
    }

    private final ServiceConnection mCastConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mCastService = ICastService.Stub.asInterface(binder);
            refreshQueue(true);
            updateTrackInfo();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mCastService = null;
            finish();
        }
    };

    private BroadcastReceiver mCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCastService != null) {
                String action = intent.getAction();
                switch (action) {
                    case CastServiceConstants.ACTION_MEDIA_CHANGED:
                        updateTrackInfo();
                        break;
                    case CastServiceConstants.ACTION_QUEUE_CHANGED:
                        refreshQueue(true);
                        break;
                    case CastServiceConstants.ACTION_LOADING_MEDIA:
                        showLoading();
                        break;
                    case CastServiceConstants.ACTION_SERVER_STOP:
                        if (!mPlayPauseDrawable.isPlay()) {
                            mPlayPauseDrawable.getPausePlayAnimator().start();
                        }
                        updateCurrentPlaying(null);
                        break;
                    case CastServiceConstants.ACTION_SERVER_EXITED:
                        finish();
                        break;
                }
            }
        }
    };

    private final OnClickListener mOnItemClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            int position = (Integer) view.getTag();
            String media = mQueueList.get(position);
            if (!mLoadingStatus) {
                try {
                    mCastService.show(media);
                    mLoadingStatus = true;
                } catch (RemoteException ex) {
                    Log.w(TAG, "Operation failed (previous)", ex);
                    mLoadingStatus = false;
                }
            }
        }
    };

    private final SimpleCallback mTouchHelperCallback = new SimpleCallback(0, ItemTouchHelper.UP) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            try {
                String item = mQueueList.get(position);
                mCastService.remove(item);
                mCache.remove(item);
                refreshQueue(false);
                mQueueAdapter.notifyItemRemoved(position);
                try {
                    int count = mQueueList.size();
                    for (int i = position; i < count; i++){
                        mQueueAdapter.notifyItemChanged(position);
                    }
                } catch (Exception ex) {
                    // Ignore
                }
            } catch (RemoteException ex) {
                // Ignore
            }
        }
    };

    private View mQueueMediaPanel;
    private ImageView mShuffle;
    private ImageView mRepeat;
    private PlayPauseDrawable mPlayPauseDrawable;

    private View mLogo;
    private ImageView mPhoto;
    private TextView mTitle;
    private TextView mAlbum;
    private ProgressBar mLoading;

    private RecyclerView mQueue;
    private QueueAdapter mQueueAdapter;
    private final List<String> mQueueList = new ArrayList<>();
    private int mOverallXScroll;

    private ICastService mCastService;
    private Point mScreenDim;

    private boolean mLoadingStatus;

    private Map<String, Bitmap> mCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cast_queue_activity);

        if (!PreferencesProvider.Preferences.Cast.isEnabled(this)) {
            finish();
        }

        // Require hardware acceleration and retain screen on
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        Display display = getWindowManager().getDefaultDisplay();
        mScreenDim = new Point();
        display.getSize(mScreenDim);

        AndroidHelper.setupRecentBar(this);

        mQueueMediaPanel = findViewById(R.id.queue_media_panel);
        mShuffle = findViewById(R.id.shuffle);
        mRepeat = findViewById(R.id.repeat);
        ImageView playPause = findViewById(R.id.play_pause);
        mPlayPauseDrawable = new PlayPauseDrawable();
        playPause.setImageDrawable(mPlayPauseDrawable);
        ImageView previous = findViewById(R.id.previous);
        ImageView next = findViewById(R.id.next);

        mLogo = findViewById(R.id.logo);
        mPhoto = findViewById(R.id.photo);
        mTitle = findViewById(R.id.photo_title);
        mAlbum = findViewById(R.id.photo_album);
        mLoading = findViewById(R.id.loading);
        mLoadingStatus = false;

        mQueue = findViewById(R.id.queue);
        mQueue.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mQueueAdapter = new QueueAdapter(this, mQueueList, mOnItemClickListener);
        mQueue.setAdapter(mQueueAdapter);
        mQueue.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mOverallXScroll += dx;
            }
        });
        ItemTouchHelper touchHelper = new ItemTouchHelper(mTouchHelperCallback);
        touchHelper.attachToRecyclerView(mQueue);

        mShuffle.setOnClickListener(this);
        mRepeat.setOnClickListener(this);
        playPause.setOnClickListener(this);
        previous.setOnClickListener(this);
        next.setOnClickListener(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(CastServiceConstants.ACTION_MEDIA_CHANGED);
        filter.addAction(CastServiceConstants.ACTION_QUEUE_CHANGED);
        filter.addAction(CastServiceConstants.ACTION_LOADING_MEDIA);
        filter.addAction(CastServiceConstants.ACTION_SERVER_STOP);
        filter.addAction(CastServiceConstants.ACTION_SERVER_EXITED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mCastReceiver, filter);

        try {
            Intent i = new Intent(this, CastService.class);
            bindService(i, mCastConnection, Context.BIND_AUTO_CREATE);
        } catch (SecurityException se) {
            Log.w(TAG, "Can't bound to CastService", se);
        }

        // Display a warning about doze mode
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_SHOW_DOZE_WARNING, false)) {
            showDozeModeWarning();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mCastService != null) {
            unbindService(mCastConnection);
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRepeat.setSelected(PreferencesProvider.Preferences.Cast.isSlideshowRepeat(this));
        mShuffle.setSelected(PreferencesProvider.Preferences.Cast.isSlideshowShuffle(this));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.shuffle:
                boolean selected = !view.isSelected();
                view.setSelected(selected);
                PreferencesProvider.Preferences.Cast.setSlideshowShuffle(this, selected);
                sendConfigurationChangedEvent("cast_slideshow_shuffle");
                break;
            case R.id.repeat:
                selected = !view.isSelected();
                view.setSelected(selected);
                PreferencesProvider.Preferences.Cast.setSlideshowRepeat(this, selected);
                sendConfigurationChangedEvent("cast_slideshow_repeat");
                break;
            case R.id.play_pause:
                boolean isPlay = mPlayPauseDrawable.isPlay();
                mPlayPauseDrawable.getPausePlayAnimator().start();
                try {
                    if (!isPlay) {
                        // Pause
                        mCastService.pause();
                    } else {
                        // Play
                        mCastService.resume();
                    }
                } catch (RemoteException ex) {
                    Log.w(TAG, "Operation failed (isPlay: " + isPlay + ")", ex);
                }
                break;
            case R.id.previous:
                if (!mLoadingStatus) {
                    try {
                        mCastService.previous();
                        mLoadingStatus = true;
                    } catch (RemoteException ex) {
                        Log.w(TAG, "Operation failed (previous)", ex);
                        mLoadingStatus = false;
                    }
                }
                break;
            case R.id.next:
                if (!mLoadingStatus) {
                    try {
                        mCastService.next();
                        mLoadingStatus = true;
                    } catch (RemoteException ex) {
                        Log.w(TAG, "Operation failed (next)", ex);
                        mLoadingStatus = false;
                    }
                }
                break;
        }
    }

    private void sendConfigurationChangedEvent(String key) {
        Intent intent = new Intent(PreferencesProvider.ACTION_SETTINGS_CHANGED);
        intent.putExtra(PreferencesProvider.EXTRA_FLAG_CAST_CONFIGURATION_CHANGE, Boolean.TRUE);
        intent.putExtra(PreferencesProvider.EXTRA_PREF_KEY, key);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void updateCurrentPlaying(String media) {
        if (media == null) {
            mTitle.setText(null);
            mAlbum.setText(null);
            mQueueAdapter.updateCurrent(null);
            mQueueAdapter.notifyDataSetChanged();
            if (mPhoto.getAlpha() != 0.0f) {
                mPhoto.animate().alpha(0.0f).setDuration(250L).start();
            }
            mLogo.animate().alpha(1.0f).setDuration(250L).start();
            return;
        }

        final File f = new File(media);
        if (!f.exists() || !f.isFile()) {
            Log.w(TAG, "Media " + media + " doesn't exists");
            return;
        }

        // Update the track info
        if (mLogo.getAlpha() != 0.0f) {
            mLogo.animate().alpha(0.0f).setDuration(250L).start();
        }
        mPhoto.animate().alpha(0.2f).setDuration(250L).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mTitle.setAlpha(0f);
                mAlbum.setAlpha(0f);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mPhoto.animate().alpha(1.0f).setDuration(450L).setListener(null).start();
                mPhoto.setImageBitmap(
                        BitmapUtils.createUnscaledBitmap(
                                f, mScreenDim.x, mScreenDim.y, 2));

                mTitle.setText(CastUtils.getTrackName(f));
                mAlbum.setText(CastUtils.getAlbumName(f));
                mTitle.setAlpha(1f);
                mAlbum.setAlpha(1f);

                mLoading.setVisibility(View.GONE);
                mLoadingStatus = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        }).start();
    }

    private void updateTrackInfo() {
        if (mCastService != null) {
            try {
                final int mode = mCastService.getCurrentCastMode();
                final String current = mCastService.getCurrentPlaying();
                final boolean shuffle = PreferencesProvider.Preferences.Cast.isSlideshowShuffle(this);
                updateCurrentPlaying(current);
                mQueueAdapter.updateCurrent(current);
                mQueue.post(new Runnable() {
                    @Override
                    public void run() {
                        int pos = mQueueList.indexOf(current) - 1;
                        if (pos >= -1) {
                            int size = (int) getResources().getDimension(R.dimen.queue_photo_size);
                            int dx = size * pos;
                            if (dx < 0) {
                                dx = 0;
                            }
                            if (mode == CastServiceConstants.CAST_MODE_SLIDESHOW && shuffle) {
                                mQueue.scrollBy(dx - mOverallXScroll, 0);
                            } else {
                                mQueue.smoothScrollBy(dx - mOverallXScroll, 0);
                            }
                            mQueueAdapter.notifyDataSetChanged();
                        }
                    }
                });
            } catch (RemoteException rex) {
                //Ignore
            }
        }
    }

    private void refreshQueue(boolean notify) {
        if (mCastService != null) {
            try {
                if (notify) {
                    final String current = mCastService.getCurrentPlaying();
                    updateCurrentPlaying(current);
                }

                synchronized (mQueueList) {
                    mQueueList.clear();
                    String[] queue = mCastService.getCurrentQueue();
                    if (queue != null && queue.length > 0) {
                        mQueueList.addAll(Arrays.asList(queue));
                    }
                }

                mQueueMediaPanel.setVisibility(mQueueList.size() > 0 ? View.VISIBLE : View.GONE);
                if (notify) {
                    mQueueAdapter.notifyDataSetChanged();
                }
            } catch (RemoteException rex) {
                //Ignore
            }
        }
    }

    private void showLoading() {
        mLoading.setVisibility(View.VISIBLE);
        mLoadingStatus = true;
    }

    private void showDozeModeWarning() {
        if (PreferencesProvider.Preferences.Cast.isShowDozeModeWarning(this)) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.cast_doze_warning_title)
                    .setMessage(R.string.cast_doze_warning_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
            dialog.show();

            // Show warning only once
            PreferencesProvider.Preferences.Cast.setShowDozeModeWarning(this, false);
        }



    }
}
