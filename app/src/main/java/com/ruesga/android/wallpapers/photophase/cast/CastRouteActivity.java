package com.ruesga.android.wallpapers.photophase.cast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ruesga.android.wallpapers.photophase.AndroidHelper;
import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences.Cast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import su.litvak.chromecast.api.v2.ChromeCast;

public class CastRouteActivity extends AppCompatActivity {

    private static final String TAG = "CastRouteActivity";

    private class DeviceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final Context mContext;

        private boolean mSeeking;
        private final List<ChromeCast> mDevices;

        public DeviceAdapter(Context context, List<ChromeCast> devices, boolean seeking) {
            mContext = context;
            mDevices = devices;
            mSeeking = seeking;
        }

        private class ProgressBarViewHolder extends RecyclerView.ViewHolder {
            public ProgressBarViewHolder(View view) {
                super(view);
            }
        }

        private class DeviceViewHolder extends RecyclerView.ViewHolder {
            public DeviceViewHolder(View view) {
                super(view);
            }
        }

        public void stopSeeking() {
            if (mSeeking) {
                mSeeking = false;
                notifyDataSetChanged();
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position >= mDevices.size()) {
                return 1;
            }
            return 2;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater li = LayoutInflater.from(mContext);
            switch (viewType) {
                case 1:
                    return new ProgressBarViewHolder(
                            li.inflate(R.layout.cast_device_waiting, parent, false));
                default:
                    return new DeviceViewHolder(
                            li.inflate(R.layout.cast_device_item, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position < mDevices.size()) {
                ChromeCast device = mDevices.get(position);
                TextView tv = (TextView) holder.itemView;
                if (device.getName() == null) {
                    tv.setText(R.string.cast_dialog_no_devices_found);
                    tv.setCompoundDrawables(null, null, null, null);
                    tv.setGravity(Gravity.CENTER);
                    tv.setTag(-1);
                } else {
                    tv.setText(device.getName());
                    tv.setTag(position);
                    tv.setOnClickListener(mOnItemClickListener);
                }
            }
        }

        @Override
        public int getItemCount() {
            return mDevices.size() + (mSeeking ? 1 : 0);
        }
    }

    private final View.OnClickListener mOnItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int position = (Integer) view.getTag();
            if (position >= 0) {
                onDeviceSelected(mDevices.get(position));
            }
        }
    };

    private final AsyncTask<Void, ChromeCast, Void> mDeviceSeekTask = new AsyncTask<Void, ChromeCast, Void>() {
        private final Object mLock = new Object();

        @Override
        protected void onPreExecute() {
            // Add the last discovered devices
            List<ChromeCast> storedDevices = Cast.getLastDiscoveredDevices(CastRouteActivity.this);
            if (storedDevices != null) {
                mDevices.addAll(storedDevices);
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final Context ctx = CastRouteActivity.this;

            // NsdManager is not supported in api 15 or lower
            if (!AndroidHelper.isJellyBeanOrGreater()) {
                Log.d(TAG, "Cast is not supported");
                return null;
            }

            // ChromeCast only works on wifi networks, so it doesn't made sense to
            // try to seek devices in the current network if it isn't a wifi network
            if (!CastUtils.hasValidCastNetwork(CastRouteActivity.this)) {
                Log.d(TAG, "Not active cast network");
                return null;
            }

            final CastDiscover discover = new CastDiscover(ctx, new CastDiscover.DeviceResolverListener() {
                @Override
                public void onDeviceDiscovered(ChromeCast device) {
                    if (isNewDevice(device)) {
                        Log.d(TAG, "Found device " + device.getName() + " at "
                                + device.getAddress() + ":" + device.getPort());
                        publishProgress(device);
                    }
                }
            });

            Log.d(TAG, "Start discovering new devices");
            discover.startDiscovery();
            mSeeking = true;

            // Wait for a seconds while seeking the network
            synchronized (mLock) {
                try {
                    mLock.wait(Cast.getDiscoveryTime(ctx) * 1000L);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
            Log.d(TAG, "Exit wait");

            // Done. We should have all available devices
            mSeeking = false;
            discover.stopDiscovery();
            Log.d(TAG, "Stop discovering new devices");
            return null;
        }

        @Override
        protected void onCancelled() {
            notifyStop();
            synchronized (mLock) {
                mLock.notify();
            }
        }

        @Override
        protected void onPostExecute(Void v) {
            notifyStop();
        }

        @Override
        protected void onProgressUpdate(ChromeCast... devices) {
            Collections.addAll(mLastDiscoveredDevices, devices);
            for (ChromeCast device : devices) {
                if (!mDevices.contains(device)) {
                    mDevices.add(device);
                    mAdapter.notifyItemInserted(mDevices.size() - 1);
                } else {
                    int index = mDevices.indexOf(device);
                    mDevices.set(index, device);
                    mAdapter.notifyItemChanged(index);
                }
            }
        }

        private void notifyStop() {
            // Save the last discovered devices
            Cast.setLastDiscoveredDevices(CastRouteActivity.this, mLastDiscoveredDevices);

            // Stop seeking
            if (mDevices.size() == 0) {
                // Add an empty device to advise the user that there is no devices
                mDevices.add(new ChromeCast(""));
            }
            mAdapter.stopSeeking();
        }
    };

    private final List<ChromeCast> mDevices = new ArrayList<>();
    private final List<ChromeCast> mLastDiscoveredDevices = new ArrayList<>();
    private AlertDialog mDialog;
    private DeviceAdapter mAdapter;
    private boolean mSeeking;

    private String mPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSeeking = true;
        if (savedInstanceState != null) {
            mSeeking = savedInstanceState.getBoolean("cast.seeking", true);
            int count = savedInstanceState.getInt("cast.count", 0);
            for (int i = 0; i < count; i++) {
                String deviceInfo = savedInstanceState.getString("cast." + i + ".device");
                if (deviceInfo != null) {
                    ChromeCast device = CastUtils.string2chromecast(deviceInfo);
                    mDevices.add(device);
                }
            }
            mPath = savedInstanceState.getString(CastServiceConstants.EXTRA_PATH);
        } else {
            // Check if it was routed. In that case just show a toast
            boolean routed = getIntent().getBooleanExtra(CastServiceConstants.EXTRA_ROUTED, false);
            if (routed) {
                ChromeCast device = Cast.getLastConnectedDevice(this);
                String msg = getString(R.string.cast_dialog_sent_to_device,
                        (device != null) ? device.getName() : "-");
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Recovery the path
            mPath = getIntent().getStringExtra(CastServiceConstants.EXTRA_PATH);

            // An error happened? Notify the user
            boolean isError = getIntent().getBooleanExtra(CastServiceConstants.EXTRA_IS_ERROR, false);
            if (isError) {
                Toast.makeText(this, R.string.cast_connect_error, Toast.LENGTH_SHORT).show();
            }
        }
        createDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDeviceSeekTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
            mDeviceSeekTask.cancel(true);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        showDialog();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CastServiceConstants.EXTRA_PATH, mPath);
        outState.putBoolean("seeking", mSeeking);
        int i = 0;
        outState.putInt("cast.count", mDevices.size());
        for (ChromeCast device : mDevices) {
            outState.putString("cast." + i + ".device", CastUtils.chromecast2string(device));
            i++;
        }
    }

    @SuppressLint("InflateParams")
    private void createDialog() {
        // Configure the devices list view
        final LayoutInflater li = LayoutInflater.from(this);
        RecyclerView view = (RecyclerView) li.inflate(R.layout.cast_device_dialog, null, false);
        view.setHasFixedSize(false);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        view.setLayoutManager(layoutManager);
        mAdapter = new DeviceAdapter(this, mDevices, mSeeking);
        view.setAdapter(mAdapter);

        // Configure the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.cast_dialog_title);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (mDialog != null) {
                    mDialog = null;
                    finish();
                }
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mDialog = null;
                finish();
            }
        });
        builder.setView(view);
        mDialog = builder.create();
    }

    private void showDialog() {
        mDialog.show();
        mDeviceSeekTask.execute();
    }

    private void onDeviceSelected(ChromeCast device) {
        // Interrupt the background task
        if (!mDeviceSeekTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
            mDeviceSeekTask.cancel(true);
        }

        // Dismiss the dialog
        if (mDialog != null) {
            mDialog.dismiss();
        }

        Log.d(TAG, "Selected device " + device.getName() + " at "
                + device.getAddress() + ":" + device.getPort());

        // Initialize the cast service with this device
        Intent i = new Intent(this, CastService.class);
        i.setAction(CastServiceConstants.ACTION_DEVICE_SELECTED);
        i.putExtra(CastServiceConstants.EXTRA_PATH, mPath);
        i.putExtra(CastServiceConstants.EXTRA_DEVICE,
                device.getAddress() + ":" + device.getPort() + "/" + device.getName());
        startService(i);
    }

    private boolean isNewDevice(ChromeCast device) {
        for (ChromeCast cc : mDevices) {
            if (cc.getAddress().equals(device.getAddress())
                    && cc.getPort() == device.getPort()) {
                return false;
            }
        }
        return true;
    }
}
