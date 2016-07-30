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

import android.provider.Settings;

import org.codehaus.jackson.annotate.JsonProperty;

import su.litvak.chromecast.api.v2.Request;

public class CastMessages {
    public static abstract class BaseMessage implements Request {
        private Long mRequestId;
        @JsonProperty("type") public final String mType;

        private BaseMessage(String type) {
            mType = type;
        }

        @Override
        public Long getRequestId() {
            return mRequestId;
        }

        @Override
        public void setRequestId(Long requestId) {
            mRequestId = requestId;
        }

        public String getType() {
            return mType;
        }
    }

    public static class Configuration extends BaseMessage {
        @JsonProperty("w") public boolean mShowWeather = true;
        @JsonProperty("t") public boolean mShowTime = true;
        @JsonProperty("lg") public boolean mShowLogo = true;
        @JsonProperty("tr") public boolean mShowTrack = true;
        @JsonProperty("i") public String mIcon = "";
        @JsonProperty("n") public String mName = "";
        @JsonProperty("l") public String mLabel = "";
        @JsonProperty("d") public String mDeviceName = "";
        @JsonProperty("cc") public boolean mCropCenter = false;
        @JsonProperty("bb") public boolean mBlurBackground = true;
        @JsonProperty("ml") public String mLoadingMsg = "";
        public Configuration() {
            super("conf");
        }
    }

    public static class Cast extends BaseMessage {
        @JsonProperty("u") public final String mUrl;
        @JsonProperty("s") public final String mSender;
        @JsonProperty("k") public final String mToken;
        @JsonProperty("t") public final String mTitle;
        @JsonProperty("a") public final String mAlbum;
        @JsonProperty("w") public final int mWidth;
        @JsonProperty("h") public final int mHeight;
        public Cast(String url, String token, String title, String album, int width, int height) {
            super("cast");
            mUrl = url;
            mSender = Settings.Secure.ANDROID_ID;
            mToken = token;
            mTitle = title;
            mAlbum = album;
            mWidth = width;
            mHeight = height;
        }
    }

    public static class Ping extends BaseMessage {
        public Ping() {
            super("ping");
        }
    }

    public static class Stop extends BaseMessage {
        public Stop() {
            super("stop");
        }
    }
}
