/*
 * Copyright (C) 2013 Jorge Ruesga
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

package com.ruesga.android.wallpapers.photophase.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLColor;
import com.ruesga.android.wallpapers.photophase.effects.Effects.EFFECTS;
import com.ruesga.android.wallpapers.photophase.model.Disposition;
import com.ruesga.android.wallpapers.photophase.transitions.Transitions.TRANSITIONS;
import com.ruesga.android.wallpapers.photophase.utils.DispositionUtil;

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
    public static final String ACTION_SETTINGS_CHANGED = "com.ruesga.android.wallpapers.photophase.actions.SETTINGS_CHANGED";

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
     * An extra setting that indicates that the media reload becomes from a user
     *
     * @see #EXTRA_FLAG_MEDIA_RELOAD
     * {@hide}
     */
    public static final String EXTRA_ACTION_MEDIA_USER_RELOAD_REQUEST = "action_media_user_reload_req";

    /**
     * The shared preferences file
     */
    public static final String PREFERENCES_FILE = "com.ruesga.android.wallpapers.photophase";

    private static Map<String, ?> mPreferences = new HashMap<String, Object>();

    /**
     * @hide
     */
    static int[] TRANSITIONS_INTERVALS;

    /**
     * Method that loads the all the preferences of the application
     *
     * @param context The current context
     */
    public static void reload(Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        mPreferences = preferences.getAll();

        final Resources res = context.getResources();
        TRANSITIONS_INTERVALS = res.getIntArray(R.array.transitions_intervals_values);
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
             * Return the current user preference of the action to do when a frame is tap.
             *
             * @return TouchAction The action (default NONE)
             */
            public static TouchAction getTouchAction() {
                return TouchAction.fromValue(Integer.valueOf(getString("ui_touch_action", "0")));
            }

            /**
             * Return the current user preference about fix or not fix the aspect ratio
             * of the image by cropping the image.
             *
             * @return boolean Indicates if the image should be cropped
             */
            public static boolean isFixAspectRatio() {
                return getBoolean("ui_fix_aspect_ratio", true);
            }

            /**
             * Transitions preferences
             */
            public static class Transitions {
                /**
                 * The default transition interval
                 */
                public static final int DEFAULT_TRANSITION_INTERVAL_INDEX = 2;

                /**
                 * Return the current user preference about the transition to apply to
                 * the pictures of the wallpaper.
                 *
                 * @return TRANSITIONS[] The transition to apply to the wallpaper's pictures
                 */
                public static TRANSITIONS[] getTransitionTypes() {
                    Set<String> set = getStringSet("ui_transition_types", new HashSet<String>());
                    if (set.isEmpty()) {
                        // Return all the transitions if no one is selected
                        return TRANSITIONS.getValidTranstions();
                    }
                    String[] values = set.toArray(new String[set.size()]);
                    int count = values.length;
                    TRANSITIONS[] transitions = new TRANSITIONS[count];
                    for (int i = 0; i < count; i++) {
                        transitions[i] = TRANSITIONS.fromOrdinal(Integer.valueOf(values[i]));
                    }
                    return transitions;
                }

                /**
                 * Method that returns how often the transitions are triggered.
                 *
                 * @return int The milliseconds in which the next transition will be triggered
                 */
                public static int getTransitionInterval() {
                    int interval = getInt("ui_transition_interval",
                            DEFAULT_TRANSITION_INTERVAL_INDEX);
                    return TRANSITIONS_INTERVALS[interval];
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
                 * @return EFFECTS[] The effects to apply to the wallpaper's pictures
                 */
                public static EFFECTS[] getEffectTypes() {
                    Set<String> defaults = new HashSet<String>();
                    defaults.add(String.valueOf(EFFECTS.NO_EFFECT.ordinal()));
                    Set<String> set = getStringSet("ui_effect_types", defaults);
                    String[] values = set.toArray(new String[set.size()]);
                    int count = values.length;
                    EFFECTS[] effects = new EFFECTS[count];
                    for (int i = 0; i < count; i++) {
                        effects[i] = EFFECTS.fromOrdinal(Integer.valueOf(values[i]));
                    }
                    return effects;
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
             * Method that returns if the app must be select new albums when they are discovered.
             *
             * @return boolean If the app must be select new albums when they are discovered.
             */
            public static boolean isAutoSelectNewAlbums() {
                return getBoolean("ui_media_auto_select_new", Boolean.TRUE);
            }

            // Internal settings (non-UI)
            /**
             * Method that returns the list of albums and pictures to be displayed
             *
             * @return Set<String> The list of albums and pictures to be displayed
             */
            public static Set<String> getSelectedMedia() {
                return getStringSet("media_selected_media", new HashSet<String>());
            }

            /**
            * Method that returns the list of albums and pictures to be displayed
            *
            * @param context The current context
            * @param selection The new list of albums and pictures to be displayed
            */
           public static synchronized void setSelectedMedia(Context context, Set<String> selection) {
               SharedPreferences preferences =
                       context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
               Editor editor = preferences.edit();
               editor.putStringSet("media_selected_media", selection);
               editor.commit();
               reload(context);
           }

           /**
            * Method that returns the list of the name of the albums seen by the
            * last media discovery scan.
            *
            * @return Set<String> The list of albums and pictures to be displayed
            */
           public static Set<String> getLastDiscorevedAlbums() {
               // FIXME Typo. Remove when version 1005 is obsolete and unused
               Set<String> oldKey = getStringSet("media_last_disvored_albums", new HashSet<String>());
               if (oldKey.size() > 0) {
                   return oldKey;
               }
               return getStringSet("media_last_discovered_albums", new HashSet<String>());
           }

           /**
            * Method that sets the list of the name of the albums seen by the
            * last media discovery scan.
            *
            * @param context The current context
            * @param albums The albums seen by the last media discovery scan
            */
           public static synchronized void setLastDiscorevedAlbums(Context context, Set<String> albums) {
               SharedPreferences preferences =
                       context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
               Editor editor = preferences.edit();
               // FIXME Typo. Remove when version 1005 is obsolete and unused
               editor.remove("media_last_disvored_albums");
               editor.putStringSet("media_last_discovered_albums", albums);
               editor.commit();
               reload(context);
           }
        }

        /**
         * Layout preferences
         */
        public static class Layout {

            private static final int DEFAULT_COLS = 4;
            private static final int DEFAULT_ROWS = 7;
            public static final String DEFAULT_PORTRAIT_DISPOSITION = "0x0:2x1|0x2:1x3|0x4:3x6|2x2:3x3|3x0:3x0|3x1:3x1";
            public static final String DEFAULT_LANDSCAPE_DISPOSITION = "0x0:2x3|3x0:5x1|3x2:4x3|5x2:6x3|6x0:6x0|6x1:6x1";

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
                return DispositionUtil.toDispositions(
                        getString("ui_layout_portrait_disposition", DEFAULT_PORTRAIT_DISPOSITION));
            }

            /**
             * Sets the disposition of the photo frames in the wallpaper on landscape screen.
             *
             * @param context The current context
             * @param dispositions The photo frames dispositions
             */
            public static void setPortraitDisposition(Context context, List<Disposition> dispositions) {
                SharedPreferences preferences =
                        context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
                Editor editor = preferences.edit();
                editor.putString("ui_layout_portrait_disposition",
                                    DispositionUtil.fromDispositions(dispositions));
                editor.commit();
                reload(context);
            }

            /**
             * Returns the disposition of the photo frames in the wallpaper on landscape screen. The
             * setting is stored as <code>0x0:1x2|2x2:3x4|...</code>, which it means (position x=0, y=0,
             * 1 cells width, 2 cells height, ...).
             *
             * @return List<Disposition> The photo frames dispositions
             */
            public static List<Disposition> getLandscapeDisposition() {
                return DispositionUtil.toDispositions(
                        getString("ui_layout_landscape_disposition", DEFAULT_LANDSCAPE_DISPOSITION));
            }

            /**
             * Sets the disposition of the photo frames in the wallpaper on landscape screen.
             *
             * @param context The current context
             * @param dispositions The photo frames dispositions
             */
            public static void setLandscapeDisposition(Context context, List<Disposition> dispositions) {
                SharedPreferences preferences =
                        context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
                Editor editor = preferences.edit();
                editor.putString("ui_layout_landscape_disposition",
                            DispositionUtil.fromDispositions(dispositions));
                editor.commit();
                reload(context);
            }
        }

    }
}
