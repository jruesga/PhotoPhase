/*
 * Copyright (C) 2015 Jorge Ruesga
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

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.borders.Borders.BORDERS;
import com.ruesga.android.wallpapers.photophase.effects.Effects.EFFECTS;
import com.ruesga.android.wallpapers.photophase.model.Disposition;
import com.ruesga.android.wallpapers.photophase.transitions.Transitions.TRANSITIONS;
import com.ruesga.android.wallpapers.photophase.utils.DispositionUtil;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLColor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class that holds all the preferences of the wallpaper
 */
@SuppressWarnings("boxing")
public final class PreferencesProvider {

    /**
     * Internal broadcast action to communicate that some setting was changed
     * @see #EXTRA_FLAG_REDRAW
     */
    public static final String ACTION_SETTINGS_CHANGED =
            "com.ruesga.android.wallpapers.photophase.actions.SETTINGS_CHANGED";

    /**
     * An extra setting that indicates that the changed setting request a whole recreation
     * of the wallpaper world
     */
    public static final String EXTRA_FLAG_RECREATE_WORLD = "flag_recreate_world";

    /**
     * An extra setting that indicates that the changed setting request a redraw of the wallpaper
     */
    public static final String EXTRA_FLAG_REDRAW = "flag_redraw";

    /**
     * An extra setting that indicates that the changed setting request to empty the texture queue
     */
    public static final String EXTRA_FLAG_EMPTY_TEXTURE_QUEUE = "flag_empty_texture_queue";

    /**
     * An extra setting that indicates that the changed setting request a reload of the media data
     */
    public static final String EXTRA_FLAG_MEDIA_RELOAD = "flag_media_reload";

    /**
     * An extra setting that indicates that the changed setting notifies that the media
     * interval was changed
     */
    public static final String EXTRA_FLAG_MEDIA_INTERVAL_CHANGED = "flag_media_interval_changed";

    /**
     * An extra setting that indicates that the media reload becomes from a user
     *
     * @see #EXTRA_FLAG_MEDIA_RELOAD
     */
    public static final String EXTRA_ACTION_MEDIA_USER_RELOAD_REQUEST =
            "action_media_user_reload_req";

    /**
     * An extra setting that indicates that the changed setting changed the disposition
     * interval time. Contains the new interval time
     */
    public static final String EXTRA_FLAG_DISPOSITION_INTERVAL_CHANGED =
            "flag_disposition_interval_changed";

    /**
     * The shared preferences file
     */
    public static final String PREFERENCES_FILE = "com.ruesga.android.wallpapers.photophase";

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
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
            public static float getWallpaperDim(Context context) {
                return getSharedPreferences(context).getInt("ui_wallpaper_dim", 0);
            }

            /**
             * Method that returns the background color
             *
             * @return GLColor The background color
             */
            public static GLColor getBackgroundColor(Context context) {
                int color = getSharedPreferences(context).getInt("ui_background_color", -2);
                if (color == -2) {
                    return DEFAULT_BACKGROUND_COLOR;
                }
                return new GLColor(color);
            }

            /**
             * Return the current user preference about using power of two textures.
             *
             * @return boolean Indicates if the image should be converted to a power of two
             */
            public static boolean isPowerOfTwo(Context context) {
                return getSharedPreferences(context).getBoolean("ui_power_of_two", false);
            }

            /**
             * Return the current user preference about fix or not fix the aspect ratio
             * of the image by cropping the image.
             *
             * @return boolean Indicates if the image should be cropped
             */
            public static boolean isFixAspectRatio(Context context) {
                return getSharedPreferences(context).getBoolean("ui_fix_aspect_ratio", true);
            }

            /**
             * Return the current user preference about adding a bit of separation between frames.
             */
            public static boolean isFrameSpacer(Context context) {
                return getSharedPreferences(context).getBoolean("ui_frame_spacer", true);
            }

            /**
             * Touch behaviour preferences
             */
            public static class Touch {

                /**
                 * Return the current user preference of touch detection mode.
                 *
                 * @return boolean true: double tap; false: single tap
                 */
                public static boolean getTouchMode(Context context) {
                    return getSharedPreferences(context).getBoolean("ui_touch_mode", true);
                }


                /**
                 * Return the current user preference of the action to do when a frame is tap.
                 *
                 * @return TouchAction The action (default NONE)
                 */
                public static TouchAction getTouchAction(Context context) {
                    return TouchAction.fromValue(Integer.valueOf(
                            getSharedPreferences(context).getString("ui_touch_action", "0")));
                }

                /**
                 * Return the open the photo with an internal or the external photo viewer
                 *
                 * @return boolean true: internal; false: external
                 */
                public static boolean getTouchOpenWith(Context context) {
                    return getSharedPreferences(context).getBoolean("ui_touch_open_with", true);
                }
            }

            /**
             * Transitions preferences
             */
            public static class Transitions {
                /**
                 * The default transition interval
                 */
                public static final int DEFAULT_TRANSITION_INTERVAL_INDEX = 2;

                public static Set<String> getSelectedTransitions(Context context) {
                    Set<String> defaults = new HashSet<>();
                    return getSharedPreferences(context).getStringSet("ui_transition_types", defaults);
                }

                public static void setSelectedTransitions(Context context, Set<String> values) {
                    SharedPreferences preferences =
                            context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
                    Editor editor = preferences.edit();
                    editor.putStringSet("ui_transition_types", values);
                    editor.putLong("ui_transition_timestamp", System.currentTimeMillis());
                    editor.apply();
                }

                public static TRANSITIONS[] toTransitions(Set<String> set) {
                    String[] values = set.toArray(new String[set.size()]);
                    int count = values.length;
                    TRANSITIONS[] effects = new TRANSITIONS[count];
                    for (int i = 0; i < count; i++) {
                        effects[i] = TRANSITIONS.fromId(Integer.valueOf(values[i]));
                    }
                    return effects;
                }

                /**
                 * Method that returns how often the transitions are triggered.
                 *
                 * @return int The milliseconds in which the next transition will be triggered
                 */
                public static int getTransitionInterval(Context context) {
                    int[] intervals = context.getResources().getIntArray(
                            R.array.transitions_intervals_values);
                    return intervals[getSharedPreferences(context).getInt(
                            "ui_transition_interval", DEFAULT_TRANSITION_INTERVAL_INDEX)];
                }
            }

            /**
             * Effects preferences
             */
            public static class Effects {
                public static Set<String> getSelectedEffects(Context context) {
                    Set<String> defaults = new HashSet<>();
                    return getSharedPreferences(context).getStringSet("ui_effect_types", defaults);
                }

                public static void setSelectedEffects(Context context, Set<String> values) {
                    SharedPreferences preferences =
                            context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
                    Editor editor = preferences.edit();
                    editor.putStringSet("ui_effect_types", values);
                    editor.putLong("ui_effect_timestamp", System.currentTimeMillis());
                    editor.apply();
                }

                public static EFFECTS[] toEFFECTS(Set<String> set) {
                    String[] values = set.toArray(new String[set.size()]);
                    int count = values.length;
                    EFFECTS[] effects = new EFFECTS[count];
                    for (int i = 0; i < count; i++) {
                        effects[i] = EFFECTS.fromId(Integer.valueOf(values[i]));
                    }
                    return effects;
                }

                public static int getEffectSettings(Context context, int effectId, int def) {
                    return getSharedPreferences(context).getInt(
                            "ui_effect_settings_" + effectId, def);
                }

                public static void setEffectSettings(Context context, int effectId, int val) {
                    SharedPreferences preferences =
                            context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
                    Editor editor = preferences.edit();
                    editor.putInt("ui_effect_settings_" + effectId, val);
                    editor.apply();
                }
            }

            /**
             * Border preferences
             */
            public static class Borders {
                /**
                 * Method that returns the border color
                 *
                 * @return GLColor The border color
                 */
                public static GLColor getBorderColor(Context context) {
                    int color = getSharedPreferences(context).getInt("ui_border_color", -2);
                    if (color == -2) {
                        return DEFAULT_BACKGROUND_COLOR;
                    }
                    return new GLColor(color);
                }

                public static Set<String> getSelectedBorders(Context context) {
                    Set<String> defaults = new HashSet<>();
                    return getSharedPreferences(context).getStringSet("ui_border_types", defaults);
                }

                public static void setSelectedBorders(Context context, Set<String> values) {
                    SharedPreferences preferences =
                            context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
                    Editor editor = preferences.edit();
                    editor.putStringSet("ui_border_types", values);
                    editor.putLong("ui_border_timestamp", System.currentTimeMillis());
                    editor.apply();
                }

                public static BORDERS[] toBORDERS(Set<String> set) {
                    String[] values = set.toArray(new String[set.size()]);
                    int count = values.length;
                    BORDERS[] borders = new BORDERS[count];
                    for (int i = 0; i < count; i++) {
                        borders[i] = BORDERS.fromId(Integer.valueOf(values[i]));
                    }
                    return borders;
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
            public static int getRefreshFrequency(Context context) {
                return Integer.valueOf(getSharedPreferences(context).getString(
                        "ui_media_refresh_interval", String.valueOf(MEDIA_RELOAD_DISABLED)));
            }

            /**
             * Method that returns if the photos are displaye randomly or sequentially
             *
             * @return boolean If the app must be select new albums when they are discovered.
             */
            public static boolean isRandomSequence(Context context) {
                return getSharedPreferences(context).getBoolean("ui_media_random", true);
            }

            /**
             * Method that returns if the app must be select new albums when they are discovered.
             *
             * @return boolean If the app must be select new albums when they are discovered.
             */
            public static boolean isAutoSelectNewAlbums(Context context) {
                return getSharedPreferences(context).getBoolean("ui_media_auto_select_new", true);
            }

            // Internal settings (non-UI)
            /**
             * Method that returns the list of albums and pictures to be displayed
             *
             * @return Set<String> The list of albums and pictures to be displayed
             */
            public static Set<String> getSelectedMedia(Context context) {
                Set<String> defaults = new HashSet<>();
                return getSharedPreferences(context).getStringSet("media_selected_media", defaults);
            }

            /**
            * Method that returns the list of albums and pictures to be displayed
            *
            * @param context The current context
            * @param selection The new list of albums and pictures to be displayed
            */
           public static synchronized void setSelectedMedia(
                   Context context, Set<String> selection) {
               SharedPreferences preferences =
                       context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
               Editor editor = preferences.edit();
               editor.putStringSet("media_selected_media", selection);
               editor.apply();
           }

           /**
            * Method that returns the list of the name of the albums seen by the
            * last media discovery scan.
            *
            * @return Set<String> The list of albums and pictures to be displayed
            */
           public static Set<String> getLastDiscorevedAlbums(Context context) {
               Set<String> defaults = new HashSet<>();
               return getSharedPreferences(context).getStringSet("media_last_discovered_albums", defaults);
           }

           /**
            * Method that sets the list of the name of the albums seen by the
            * last media discovery scan.
            *
            * @param context The current context
            * @param albums The albums seen by the last media discovery scan
            */
           public static synchronized void setLastDiscorevedAlbums(
                   Context context, Set<String> albums) {
               SharedPreferences preferences =
                       context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
               Editor editor = preferences.edit();
               editor.putStringSet("media_last_discovered_albums", albums);
               editor.apply();
           }
        }

        /**
         * Layout preferences
         */
        public static class Layout {

            private static final int DEFAULT_COLS = 4;
            private static final int DEFAULT_ROWS = 7;
            private static final boolean DEFAULT_RANDOM_DISPOSITION = false;

            /**
             * The default portrait disposition
             */
            public static final String DEFAULT_PORTRAIT_DISPOSITION =
                    "0x0:2x1|0x2:1x3|0x4:3x6|2x2:3x3|3x0:3x0|3x1:3x1";
            /**
             * The default landscape disposition
             */
            public static final String DEFAULT_LANDSCAPE_DISPOSITION =
                    "0x0:2x3|3x0:5x1|3x2:4x3|5x2:6x3|6x0:6x0|6x1:6x1";

            /**
             * The default transition interval
             */
            public static final int DEFAULT_RANDOM_DISPOSITIONS_INTERVAL_INDEX = 0;

            /**
             * Returns the number of rows of the wallpaper.
             *
             * @return int The rows of the wallpaper
             */
            public static int getRows(Context context) {
                return getSharedPreferences(context).getInt("ui_layout_rows", DEFAULT_ROWS);
            }

            /**
             * Returns the number columns of the wallpaper.
             *
             * @return int The columns of the wallpaper
             */
            public static int getCols(Context context) {
                return getSharedPreferences(context).getInt("ui_layout_cols", DEFAULT_COLS);
            }

            /**
             * Returns if the device should generate random dispositions
             *
             * @return boolean If the system should generate random dispositions
             */
            public static boolean isRandomDispositions(Context context) {
                return getSharedPreferences(context).getBoolean(
                        "ui_disposition_random", DEFAULT_RANDOM_DISPOSITION);
            }

            /**
             * Method that returns how often the random dispositions are triggered.
             *
             * @return int The milliseconds in which the next transition will be triggered
             */
            public static int getRandomDispositionsInterval(Context context) {
            int[] intervals = context.getResources().getIntArray(
                    R.array.random_dispositions_intervals_values);
                return intervals[getSharedPreferences(context).getInt(
                        "ui_disposition_random_interval", DEFAULT_RANDOM_DISPOSITIONS_INTERVAL_INDEX)];
            }

            /**
             * Returns the disposition of the photo frames in the wallpaper on portrait screen. The
             * setting is stored as <code>0x0:1x2|2x2:3x4|...</code>, which it means
             * (position x=0, y=0, 1 cells width, 2 cells height, ...).
             *
             * @return List<Disposition> The photo frames dispositions
             */
            public static List<Disposition> getPortraitDisposition(Context context) {
                String dispositions = getSharedPreferences(context).getString(
                        "ui_layout_portrait_disposition", DEFAULT_PORTRAIT_DISPOSITION);
                return DispositionUtil.toDispositions(dispositions);
            }

            /**
             * Sets the disposition of the photo frames in the wallpaper on landscape screen.
             *
             * @param context The current context
             * @param dispositions The photo frames dispositions
             */
            public static void setPortraitDisposition(Context context,
                    List<Disposition> dispositions) {
                SharedPreferences preferences =
                        context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
                Editor editor = preferences.edit();
                editor.putString("ui_layout_portrait_disposition",
                                    DispositionUtil.fromDispositions(dispositions));
                editor.apply();
            }

            /**
             * Returns the disposition of the photo frames in the wallpaper on landscape screen.
             * The setting is stored as <code>0x0:1x2|2x2:3x4|...</code>, which it means
             * (position x=0, y=0, 1 cells width, 2 cells height, ...).
             *
             * @return List<Disposition> The photo frames dispositions
             */
            public static List<Disposition> getLandscapeDisposition(Context context) {
                String dispositions = getSharedPreferences(context).getString(
                        "ui_layout_landscape_disposition", DEFAULT_LANDSCAPE_DISPOSITION);
                return DispositionUtil.toDispositions(dispositions);
            }

            /**
             * Sets the disposition of the photo frames in the wallpaper on landscape screen.
             *
             * @param context The current context
             * @param dispositions The photo frames dispositions
             */
            public static void setLandscapeDisposition(Context context,
                    List<Disposition> dispositions) {
                SharedPreferences preferences =
                        context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
                Editor editor = preferences.edit();
                editor.putString("ui_layout_landscape_disposition",
                            DispositionUtil.fromDispositions(dispositions));
                editor.apply();
            }
        }

    }
}
