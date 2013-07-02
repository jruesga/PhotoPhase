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

package org.cyanogenmod.wallpapers.photophase.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.cyanogenmod.wallpapers.photophase.GLESUtil.GLColor;
import org.cyanogenmod.wallpapers.photophase.effects.Effects.EFFECTS;
import org.cyanogenmod.wallpapers.photophase.model.Disposition;
import org.cyanogenmod.wallpapers.photophase.transitions.Transitions.TRANSITIONS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class that holds all the preferences of the wallpaper
 */
@SuppressWarnings("boxing")
public final class PreferencesProvider {

    /**
     * Internal broadcast action to communicate that some setting was changed
     * @see #EXTRA_FLAG_REDRAW
     * {@hide}
     */
    public static final String ACTION_SETTINGS_CHANGED = "org.cyanogenmod.wallpapers.photophase.actions.SETTINGS_CHANGED";

    /**
     * An extra setting that indicates that the changed setting request a whole recreation of the wallpaper world
     * {@hide}
     */
    public static final String EXTRA_FLAG_RECREATE_WORLD = "flag_recreate_world";

    /**
     * An extra setting that indicates that the changed setting request a redraw of the wallpaper
     * {@hide}
     */
    public static final String EXTRA_FLAG_REDRAW = "flag_redraw";

    /**
     * An extra setting that indicates that the changed setting request to empty the texture queue
     * {@hide}
     */
    public static final String EXTRA_FLAG_EMPTY_TEXTURE_QUEUE = "flag_empty_texture_queue";

    /**
     * An extra setting that indicates that the changed setting request a reload of the media data
     * {@hide}
     */
    public static final String EXTRA_FLAG_MEDIA_RELOAD = "flag_media_reload";

    /**
     * An extra setting that indicates that the changed setting notifies that the media
     * interval was changed
     * {@hide}
     */
    public static final String EXTRA_FLAG_MEDIA_INTERVAL_CHANGED = "flag_media_interval_changed";

    /**
     * The shared preferences file
     */
    public static final String PREFERENCES_FILE = "org.cyanogenmod.wallpapers.photophase";

    private static Map<String, ?> mPreferences = new HashMap<String, Object>();

    /**
     * Method that loads the all the preferences of the application
     *
     * @param context The current context
     */
    public static void reload(Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        mPreferences = preferences.getAll();
    }

    /**
     * Method that returns a integer property value.
     *
     * @param key The preference key
     * @param def The default value
     * @return int The integer property value
     */
    static int getInt(String key, int def) {
        return mPreferences.containsKey(key) && mPreferences.get(key) instanceof Integer ?
                (Integer) mPreferences.get(key) : def;
    }

    /**
     * Method that returns a long property value.
     *
     * @param key The preference key
     * @param def The default value
     * @return long The long property value
     */
    static long getLong(String key, long def) {
        return mPreferences.containsKey(key) && mPreferences.get(key) instanceof Long ?
                (Long) mPreferences.get(key) : def;
    }

    /**
     * Method that returns a boolean property value.
     *
     * @param key The preference key
     * @param def The default value
     * @return boolean The boolean property value
     */
    static boolean getBoolean(String key, boolean def) {
        return mPreferences.containsKey(key) && mPreferences.get(key) instanceof Boolean ?
                (Boolean) mPreferences.get(key) : def;
    }

    /**
     * Method that returns a string property value.
     *
     * @param key The preference key
     * @param def The default value
     * @return String The string property value
     */
    static String getString(String key, String def) {
        return mPreferences.containsKey(key) && mPreferences.get(key) instanceof String ?
                (String) mPreferences.get(key) : def;
    }

    /**
     * Method that returns a string set property value.
     *
     * @param key The preference key
     * @param def The default value
     * @return Set<String> The string property value
     */
    @SuppressWarnings("unchecked")
    static Set<String> getStringSet(String key, Set<String> def) {
        return mPreferences.containsKey(key) && mPreferences.get(key) instanceof Set<?> ?
                (Set<String>) mPreferences.get(key) : def;
    }

    /**
     * A class for access to the preferences of the application
     */
    public static class Preferences {
        /**
         * General preferences
         */
        public static class General {
            private static final GLColor DEFAULT_BACKGROUND_COLOR = new GLColor("#ff202020");

            /**
             * Method that returns the wallpaper dimmed value.
             *
             * @return float If the wallpaper dimmed value (0-black, 100-black)
             */
            public static float getWallpaperDim() {
                return getInt("ui_wallpaper_dim", 0);
            }

            /**
             * Method that returns the background color
             *
             * @return GLColor The background color
             */
            public static GLColor getBackgroundColor() {
                int color = getInt("ui_background_color", 0);
                if (color == 0) {
                    return DEFAULT_BACKGROUND_COLOR;
                }
                return new GLColor(color);
            }

            /**
             * Transitions preferences
             */
            public static class Transitions {
                /**
                 * The default transition interval
                 */
                public static final int DEFAULT_TRANSITION_INTERVAL = 2000;
                /**
                 * The minimum transition interval
                 */
                public static final int MIN_TRANSITION_INTERVAL = 1000;
                /**
                 * The maximum transition interval
                 */
                public static final int MAX_TRANSITION_INTERVAL = 8000;

                /**
                 * Return the current user preference about the transition to apply to
                 * the pictures of the wallpaper.
                 *
                 * @return int The transition to apply to the wallpaper's pictures
                 */
                public static int getTransitionTypes() {
                    return Integer.valueOf(getString("ui_transition_types", String.valueOf(TRANSITIONS.RANDOM.ordinal())));
                }

                /**
                 * Method that returns how often the transitions are triggered.
                 *
                 * @return int The milliseconds in which the next transition will be triggered
                 */
                public static int getTransitionInterval() {
                    int def =  (DEFAULT_TRANSITION_INTERVAL / 500) - 2;
                    int interval = getInt("ui_transition_interval", def);
                    return (interval * 500) + 1000;
                }
            }

            /**
             * Effects preferences
             */
            public static class Effects {
                /**
                 * Return the current user preference about the effect to apply to
                 * the pictures of the wallpaper.
                 *
                 * @return int The effect to apply to the wallpaper's pictures
                 */
                public static int getEffectTypes() {
                    return Integer.valueOf(getString("ui_effect_types", String.valueOf(EFFECTS.NO_EFFECT.ordinal())));
                }
            }
        }

        /**
         * Media preferences
         */
        public static class Media {
            /**
             * Constant that indicates that the media reload is disabled
             */
            public static final int MEDIA_RELOAD_DISABLED = 0;

            /**
             * Method that returns the frequency with which the media is updated.
             *
             * @return int The interval in seconds between updates. 0 means that updates are disabled
             */
            public static int getRefreshFrecuency() {
                return Integer.valueOf(getString("ui_media_refresh_interval", String.valueOf(MEDIA_RELOAD_DISABLED)));
            }

            /**
             * Method that returns the list of albums and pictures to be displayed
             *
             * @return Set<String> The list of albums and pictures to be displayed
             */
            public static Set<String> getSelectedAlbums() {
                return getStringSet("ui_media_selected_albums", new HashSet<String>());
            }

            /**
            * Method that returns the list of albums and pictures to be displayed
            *
            * @param context The current context
            * @param selection The new list of albums and pictures to be displayed
            */
           public static synchronized void setSelectedAlbums(Context context, Set<String> selection) {
               SharedPreferences preferences =
                       context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
               Editor editor = preferences.edit();
               editor.remove("ui_media_selected_albums");
               editor.putStringSet("ui_media_selected_albums", selection);
               editor.commit();
               reload(context);
           }
        }

        /**
         * Layout preferences
         */
        public static class Layout {

            private static final int DEFAULT_COLS = 8;
            private static final int DEFAULT_ROWS = 14;
            private static final String DEFAULT_PORTRAIT_DISPOSITION = "0x0:5x4|5x0:3x2|5x2:3x2|0x4:4x4|4x4:4x4|0x8:8x6";
            private static final String DEFAULT_LANDSCAPE_DISPOSITION = "0x0:5x4|5x0:3x2|5x2:3x2|0x4:4x4|4x4:4x4|0x8:8x6";

            /**
             * Method that returns the rows of the wallpaper.
             *
             * @return int The rows of the wallpaper
             */
            public static int getRows() {
                return getInt("ui_layout_rows", DEFAULT_ROWS);
            }

            /**
             * Method that returns the columns of the wallpaper.
             *
             * @return int The columns of the wallpaper
             */
            public static int getCols() {
                return getInt("ui_layout_cols", DEFAULT_COLS);
            }

            /**
             * Returns the disposition of the photo frames in the wallpaper on portrait screen. The
             * setting is stored as <code>0x0:1x2|2x2:3x4|...</code>, which it means (position x=0, y=0,
             * 1 cells width, 2 cells height, ...).
             *
             * @return List<Disposition> The photo frames dispositions
             */
            public static List<Disposition> getPortraitDisposition() {
                return toDispositions(getString("ui_layout_portrait_disposition", DEFAULT_PORTRAIT_DISPOSITION));
            }

            /**
             * Returns the disposition of the photo frames in the wallpaper on landscape screen. The
             * setting is stored as <code>0x0:1x2|2x2:3x4|...</code>, which it means (position x=0, y=0,
             * 1 cells width, 2 cells height, ...).
             *
             * @return List<Disposition> The photo frames dispositions
             */
            public static List<Disposition> getLandscapeDisposition() {
                return toDispositions(getString("ui_layout_landscape_disposition", DEFAULT_LANDSCAPE_DISPOSITION));
            }

            /**
             * Method that converts to dispositions reference
             *
             * @param value The value to convert
             * @return List<Disposition> The  dispositions reference
             */
            private static List<Disposition> toDispositions(String value) {
                String[] v = value.split("\\|");
                List<Disposition> dispositions = new ArrayList<Disposition>(v.length);
                for (String s : v) {
                    String[] s1 = s.split(":");
                    String[] s2 = s1[0].split("x");
                    String[] s3 = s1[1].split("x");
                    Disposition disposition = new Disposition();
                    disposition.x = Integer.parseInt(s2[0]);
                    disposition.y = Integer.parseInt(s2[1]);
                    disposition.w = Integer.parseInt(s3[0]);
                    disposition.h = Integer.parseInt(s3[1]);
                    dispositions.add(disposition);
                }
                return dispositions;
            }
        }

    }
}
