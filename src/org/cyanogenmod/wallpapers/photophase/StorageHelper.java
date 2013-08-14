/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package org.cyanogenmod.wallpapers.photophase;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A helper class to deal with android storage
 */
public final class StorageHelper {

    private static final String TAG = "StorageHelper";

    private static final String EXTERNAL_REGEXP = "(?i).*vold.*(fuse|vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";

    /**
     * Method that returns all the external mounts
     *
     * @return List<String> All the external mounts
     */
    public static List<String> getExternalMounts() {
        final List<String> out = new ArrayList<String>();

        // Execute the mount command to list mounts
        final StringBuilder sb = new StringBuilder();
        try {
            final Process process =
                    new ProcessBuilder().command("mount")
                        .redirectErrorStream(true).start();
            process.waitFor();
            final InputStream is = process.getInputStream();
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = is.read(buffer, 0, 1024)) != -1) {
                sb.append(new String(buffer, 0, read));
            }
            is.close();
        } catch (IOException ioex) {
            Log.e(TAG, "Failed to list external mounts", ioex);
        } catch (InterruptedException iex) {
            Log.e(TAG, "Failed to list external mounts", iex);
        }

        // Parse the output
        final String[] lines = sb.toString().split("\n");
        for (String line : lines) {
            if (!line.toLowerCase(Locale.US).contains("asec")) {
                if (line.matches(EXTERNAL_REGEXP)) {
                    String[] parts = line.split(" ");
                    for (String part : parts) {
                        if (part.startsWith("/")) {
                            if (!part.toLowerCase(Locale.US).contains("vold")) {
                                out.add(part);
                            }
                        }
                    }
                }
            }
        }
        return out;
    }
}
