/*
 * Copyright (C) 2017 Jorge Ruesga
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

public final class CastServiceConstants {

    public static final String ACTION_DEVICE_SELECTED =
            "com.ruesga.android.wallpapers.photophase.actions.CAST_DEVICE_SELECTED";
    public static final String ACTION_CONNECTIVITY_CHANGED =
            "com.ruesga.android.wallpapers.photophase.actions.CAST_CONNECTIVITY_CHANGED";
    public static final String ACTION_MEDIA_COMMAND =
            "com.ruesga.android.wallpapers.photophase.actions.CAST_MEDIA_COMMAND";

    public static final String ACTION_ON_RELEASE_NETWORK =
            "com.ruesga.android.wallpapers.photophase.broadcast.CAST_NETWORK_RELEASED";
    public static final String ACTION_SCAN_FINISHED =
            "com.ruesga.android.wallpapers.photophase.broadcast.CAST_SCAN_FINISHED";
    public static final String ACTION_MEDIA_CHANGED =
            "com.ruesga.android.wallpapers.photophase.broadcast.CAST_MEDIA_CHANGED";
    public static final String ACTION_QUEUE_CHANGED =
            "com.ruesga.android.wallpapers.photophase.broadcast.CAST_QUEUE_CHANGED";
    public static final String ACTION_LOADING_MEDIA =
            "com.ruesga.android.wallpapers.photophase.broadcast.CAST_LOADING_MEDIA";
    public static final String ACTION_SERVER_STOP =
            "com.ruesga.android.wallpapers.photophase.broadcast.SERVER_STOP";
    public static final String ACTION_SERVER_EXITED =
            "com.ruesga.android.wallpapers.photophase.broadcast.CAST_SERVER_EXITED";

    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_DEVICE = "device";
    public static final String EXTRA_IS_ERROR = "is_error";
    public static final String EXTRA_ROUTED = "routed";
    public static final String EXTRA_COMMAND = "command";

    public static final int CAST_MODE_NONE = -1;
    public static final int CAST_MODE_SINGLE = 0;
    public static final int CAST_MODE_SLIDESHOW = 1;

    public static final int INVALID_COMMAND = -1;
    public static final int COMMAND_PAUSE = 0;
    public static final int COMMAND_RESUME = 1;
    public static final int COMMAND_NEXT = 2;
    public static final int COMMAND_STOP = 3;
}
