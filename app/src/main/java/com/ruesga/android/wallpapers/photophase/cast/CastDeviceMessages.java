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

import android.util.Log;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

public class CastDeviceMessages {

    private static final String TAG = "CastDeviceMessages";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static BaseDeviceMessage parseDeviceMessage(String appMsg) {
        JsonNode json;
        try {
            json = MAPPER.readTree(appMsg);
            String type = json.get("type").asText();
            String msg = json.get("msg").asText();
            switch (type) {
                case OnReadyMessage.TYPE:
                    return MAPPER.readValue(msg, OnReadyMessage.class);
                case OnNewTrackMessage.TYPE:
                    return MAPPER.readValue(msg, OnNewTrackMessage.class);
                case OnPongMessage.TYPE:
                    return MAPPER.readValue(msg, OnPongMessage.class);
            }
        } catch (IOException e) {
            Log.e(TAG, "Can't handle app message: " + appMsg, e);
        }
        return null;
    }

    public static abstract class BaseDeviceMessage {
    }

    public static class OnReadyMessage extends BaseDeviceMessage {
        private static final String TYPE = "ready";
        @JsonProperty("v") public int mVersion;
        public OnReadyMessage() {
        }
    }

    public static class OnNewTrackMessage extends BaseDeviceMessage {
        private static final String TYPE = "track";
        @JsonProperty("k") public String mToken;
        @JsonProperty("s") public String mSender;
        public OnNewTrackMessage() {
        }
    }

    public static class OnPongMessage extends BaseDeviceMessage {
        private static final String TYPE = "pong";
        public OnPongMessage() {
        }
    }
}
