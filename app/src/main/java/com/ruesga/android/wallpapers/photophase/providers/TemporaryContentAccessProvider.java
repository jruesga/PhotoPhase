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
package com.ruesga.android.wallpapers.photophase.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.ruesga.android.wallpapers.photophase.AndroidHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TemporaryContentAccessProvider extends ContentProvider {

    public static final String AUTHORITY = "com.ruesga.android.wallpapers.photophase.providers";

    private static final String CONTENT_AUTHORITY = "content://" + AUTHORITY;

    private static final String COLUMN_ID = "auth_id";
    private static final String COLUMN_NAME = OpenableColumns.DISPLAY_NAME;
    private static final String COLUMN_SIZE = OpenableColumns.SIZE;

    private static final String[] COLUMN_PROJECTION = {
            COLUMN_ID, COLUMN_NAME, COLUMN_SIZE
    };

    private static class AuthorizationResource {
        private final File mFile;

        private AuthorizationResource(Uri uri) {
            mFile = new File(uri.getPath());
        }
    }

    private static final Map<UUID, AuthorizationResource> AUTHORIZATIONS =
            (Map<UUID, AuthorizationResource>) Collections.synchronizedMap(
                    new HashMap<UUID, AuthorizationResource>());

    private static ScheduledThreadPoolExecutor sScheduler = new ScheduledThreadPoolExecutor(5);

    public static Uri createAuthorizationUri(Uri uri) {
        // Use this content provider only for Lollipop or greater
        // TODO Just skip this check in case some day PhotoPhase will handle
        // internal paths
        if (!AndroidHelper.isLollipopOrGreater()) {
            return uri;
        }

        // Generate a new authorization for the filesystem
        UUID uuid;
        do {
            uuid = UUID.randomUUID();
            if (!AUTHORIZATIONS.containsKey(uuid)) {
                AuthorizationResource resource = new AuthorizationResource(uri);
                AUTHORIZATIONS.put(uuid, resource);
                break;
            }
        } while(true);

        // Clean up authorization after 1 minute
        final UUID token = uuid;
        sScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                AUTHORIZATIONS.remove(token);
            }
        }, 1, TimeUnit.MINUTES);

        // Return the authorization uri
        return Uri.withAppendedPath(Uri.parse(CONTENT_AUTHORITY), uuid.toString());
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        AuthorizationResource authResource = getAuthorizationResourceForUri(uri);
        if (authResource == null) {
            throw new SecurityException("Authorization not exists");
        }

        // Create an in-memory cursor
        String[] cols = new String[COLUMN_PROJECTION.length];
        Object[] values = new Object[COLUMN_PROJECTION.length];
        for (int i = 0; i < COLUMN_PROJECTION.length; i++) {
            cols[i] = COLUMN_PROJECTION[i];
            switch (i) {
                case 0:
                    values[i] = uri.getLastPathSegment();
                    break;
                case 1:
                    values[i] = authResource.mFile.getName();
                    break;
                case 2:
                    values[i] = authResource.mFile.length();
                    break;

                default:
                    break;
            }
        }

        final MatrixCursor cursor = new MatrixCursor(cols, 1);
        cursor.addRow(values);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        // Retrieve the authorization
        AuthorizationResource authResource = getAuthorizationResourceForUri(uri);
        if (authResource == null) {
            throw new SecurityException("Authorization not exists");
        }

        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(authResource.mFile.getAbsolutePath()));
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {
        return this.openFile(uri, mode, null);
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode,
            final CancellationSignal signal) throws FileNotFoundException {
        final AuthorizationResource authResource = getAuthorizationResourceForUri(uri);
        if (authResource == null) {
            throw new SecurityException("Authorization not exists");
        }

        // Allocate the parcel descriptor
        return ParcelFileDescriptor.open(authResource.mFile, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Nullable
    private static AuthorizationResource getAuthorizationResourceForUri(Uri uri) {
        try {
            String id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(id)) {
                return null;
            }
            UUID uuid = UUID.fromString(id);
            if (uuid == null || !AUTHORIZATIONS.containsKey(uuid)) {
                return null;
            }
            return AUTHORIZATIONS.get(uuid);
        } catch (IllegalArgumentException ex) {
            // Ignore
        }
        return null;
    }
}
