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

/**
 * The cast service interface
 */
interface ICastService {

    // Near devices

    /**
     * Check whether there are near devices
     */
    boolean hasNearDevices();

    /**
     * Requuest a new scan
     */
    void requestScan();



    // Cast controlling

    /**
     * Cast a photo or album
     */
    void cast(in String path);

    /**
     * Enqueue a photo or album
     */
    void enqueue(in String path);

    /**
     * Pause the current queue
     */
    void pause();

    /**
     * Resume the current queue
     */
    void resume();

    /**
     * Show passed media file
     */
    void show(String media);

    /**
     * Remove passed media file
     */
    void remove(String media);

    /**
     * Show previous item
     */
    void previous();

    /**
     * Show next item
     */
    void next();

    /**
     * Stop the cast app
     */
    void stop();

    /**
     * Exit the cast app
     */
    void exit();




    // Cast info

    /**
     * Returns the current displaying media
     */
    String getCurrentPlaying();

    /**
     * Returns the current enqueue media
     */
    String[] getCurrentQueue();

    /**
     * Returns the current server mode (0-SINGLE, 1-SLIDESHOW)
     */
    int getCurrentCastMode();
}
