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

package com.ruesga.android.wallpapers.photophase.borders;

/**
 * A class that defines the own PhotoPhase's borders effects implementation. This class follows the
 * rules of the MCA aosp library.
 */
public class BordersFactory {
    /**
     * <p>Doesn't apply any border.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String BORDER_NULL = "com.ruesga.android.wallpapers.photophase.borders.NullBorder";
    /**
     * <p>A simple border.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String BORDER_SIMPLE = "com.ruesga.android.wallpapers.photophase.borders.SimpleBorder";
    /**
     * <p>A rounded border.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String BORDER_ROUNDED = "com.ruesga.android.wallpapers.photophase.borders.RoundedBorder";
    /**
     * <p>A rounded squares border.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String BORDER_ROUNDED_SQUARES = "com.ruesga.android.wallpapers.photophase.borders.RoundedSquaresBorder";
    /**
     * <p>A horizontal film tape border.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String BORDER_HORIZONTAL_FILM = "com.ruesga.android.wallpapers.photophase.borders.HorizontalFilmBorder";
    /**
     * <p>A vertical film tape border.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String BORDER_VERTICAL_FILM = "com.ruesga.android.wallpapers.photophase.borders.VerticalFilmBorder";
    /**
     * <p>An elegant border.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String BORDER_ELEGANT = "com.ruesga.android.wallpapers.photophase.borders.ElegantBorder";
    /**
     * <p>An double border.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String BORDER_DOUBLE = "com.ruesga.android.wallpapers.photophase.borders.DoubleBorder";
    /**
     * <p>An double joined border.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String BORDER_DOUBLE_JOINED = "com.ruesga.android.wallpapers.photophase.borders.DoubleJoinedBorder";
    /**
     * <p>An inset border.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String BORDER_INSET = "com.ruesga.android.wallpapers.photophase.borders.InsetBorder";
    /**
     * <p>A border with squares.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String BORDER_SQUARES = "com.ruesga.android.wallpapers.photophase.borders.SquaresBorder";
    /**
     * <p>A border with inset squares.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String BORDER_INSET_SQUARES = "com.ruesga.android.wallpapers.photophase.borders.InsetSquaresBorder";
}
