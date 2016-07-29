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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;

import com.ruesga.android.wallpapers.photophase.AndroidHelper;
import com.ruesga.android.wallpapers.photophase.PhotoViewerActivity;
import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.cast.CastService.CastStatusInfo;
import com.ruesga.android.wallpapers.photophase.utils.BitmapUtils;

import java.io.File;

public class CastNotification {

    private static final int CAST_NOTIFICATION_ID = 1;

    public static void showNotification(Context ctx, File media, CastStatusInfo statusInfo) {
        if (media == null) {
            showNoMediaNotification(ctx, statusInfo);
        } else {
            showMediaNotification(ctx, media, statusInfo);
        }
    }

    public static void hideNotification(Context ctx) {
        if (ctx instanceof Service) {
            ((Service) ctx).stopForeground(true);
        } else {
            NotificationManagerCompat nm = NotificationManagerCompat.from(ctx);
            nm.cancel(CAST_NOTIFICATION_ID);
        }
    }

    private static void showNoMediaNotification(Context ctx, CastStatusInfo statusInfo) {
        performShowNotification(ctx, statusInfo, null, null);
    }

    private static void showMediaNotification(Context ctx, File media, CastStatusInfo statusInfo) {
        // Obtain a thumbnail of the current picture media
        Rect r = BitmapUtils.getBitmapDimensions(media);
        if (r == null) {
            return;
        }
        BitmapUtils.adjustRectToMinimumSize(r, 1024);
        Bitmap thumbnail = BitmapUtils.decodeBitmap(media, r.width(), r.height());
        String trackName = CastUtils.getTrackName(media);
        String albumName = CastUtils.getAlbumName(media);

        // Big picture style
        NotificationCompat.BigPictureStyle style =  new NotificationCompat.BigPictureStyle();
        style.setBigContentTitle(trackName);
        style.setSummaryText(albumName);
        style.bigPicture(thumbnail);

        final PendingIntent contentIntent;
        if (statusInfo.mCastMode != CastService.CAST_MODE_SLIDESHOW && media != null) {
            contentIntent = getDisplayPhotoActionIntent(ctx, media);
        } else {
            contentIntent = getShowCastQueueActionIntent(ctx);
        }

        performShowNotification(ctx, statusInfo, style, contentIntent);
    }

    private static void performShowNotification(Context ctx, CastStatusInfo statusInfo,
            NotificationCompat.Style style, PendingIntent contentIntent) {
        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);
        builder.setSmallIcon(R.drawable.ic_cast_notification)
                .setContentTitle(ctx.getString(R.string.app_name))
                .setContentText(ctx.getString(R.string.cast_app_description))
                .setContentIntent(null)
                .setShowWhen(false)
                .setLocalOnly(true)
                .setStyle(style)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (statusInfo.mCastMode == CastService.CAST_MODE_SLIDESHOW) {
            if (statusInfo.mPaused) {
                builder.addAction(R.drawable.ic_play,
                        ctx.getString(R.string.cast_resume),
                        getCastActionIntent(ctx, CastService.COMMAND_RESUME));
            } else {
                builder.addAction(R.drawable.ic_pause,
                        ctx.getString(R.string.cast_pause),
                        getCastActionIntent(ctx, CastService.COMMAND_PAUSE));
            }
            builder.addAction(R.drawable.ic_skip_next,
                    ctx.getString(R.string.cast_next),
                    getCastActionIntent(ctx, CastService.COMMAND_NEXT));
        }
        builder.addAction(R.drawable.ic_stop,
                ctx.getString(R.string.cast_stop),
                getCastActionIntent(ctx, CastService.COMMAND_STOP));

        Notification notification = builder.build();
        if (ctx instanceof Service) {
            ((Service) ctx).startForeground(CAST_NOTIFICATION_ID, notification);
        } else {
            NotificationManagerCompat nm = NotificationManagerCompat.from(ctx);
            nm.notify(CAST_NOTIFICATION_ID, notification);
        }
    }

    private static PendingIntent getCastActionIntent(Context context, int command) {
        Intent i = new Intent(context, CastService.class);
        i.setAction(CastService.ACTION_MEDIA_COMMAND);
        i.putExtra(CastService.EXTRA_COMMAND, command);
        return PendingIntent.getService(context, command, i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static PendingIntent getDisplayPhotoActionIntent(Context context, File media) {
        Intent i = new Intent(context, PhotoViewerActivity.class);
        i.putExtra(PhotoViewerActivity.EXTRA_PHOTO, media.getAbsolutePath());
        //noinspection deprecation
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_HISTORY
                | (AndroidHelper.isLollipopOrGreater()
                    ? Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                    : Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET));
        return PendingIntent.getActivity(context, 2000, i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static PendingIntent getShowCastQueueActionIntent(Context context) {
        Intent i = new Intent(context, CastPhotoQueueActivity.class);
        //noinspection deprecation
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_HISTORY
                | (AndroidHelper.isLollipopOrGreater()
                    ? Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                    : Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET));
        return PendingIntent.getActivity(context, 3000, i, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}

