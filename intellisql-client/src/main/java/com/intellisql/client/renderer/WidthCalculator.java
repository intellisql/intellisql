/*
 * Licensed to the IntelliSQL Project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The IntelliSQL Project licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellisql.client.renderer;

/**
 * Utility class for calculating display width of strings considering CJK characters.
 */
public class WidthCalculator {

    private static final String NULL_PLACEHOLDER = "null";

    /**
     * Calculates the display width of a string.
     * CJK characters count as 2, others count as 1.
     *
     * @param str the string to measure
     * @return the display width
     */
    public static int getWidth(final String str) {
        if (str == null) {
            return 4;
        }
        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (isCjk(c)) {
                width += 2;
            } else {
                width += 1;
            }
        }
        return width;
    }

    /**
     * Checks if a character is a CJK (Chinese, Japanese, Korean) character.
     *
     * @param c the character to check
     * @return true if the character is CJK, false otherwise
     */
    public static boolean isCjk(final char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES;
    }

    /**
     * Pads a string to the specified width with spaces.
     *
     * @param str   the string to pad
     * @param width the target width
     * @return the padded string
     */
    public static String pad(final String str, final int width) {
        return padOrTruncate(str, width, false);
    }

    /**
     * Pads or truncates a string to the specified width.
     *
     * @param input   the string to process
     * @param width   the target width
     * @param truncate whether to truncate if the string exceeds the width
     * @return the processed string
     */
    public static String padOrTruncate(final String input, final int width, final boolean truncate) {
        String str = input == null ? NULL_PLACEHOLDER : input;
        int currentWidth = getWidth(str);
        if (currentWidth > width) {
            if (!truncate) {
                return str;
            }
            return truncateString(str, width);
        } else {
            return padString(str, width - currentWidth);
        }
    }

    /**
     * Truncates a string to fit within the specified width.
     *
     * @param str   the string to truncate
     * @param width the target width
     * @return the truncated and padded string
     */
    private static String truncateString(final String str, final int width) {
        StringBuilder sb = new StringBuilder();
        int w = 0;
        // Reserve space for ellipsis if width allows (e.g. > 3)
        int targetWidth = width;
        boolean useEllipsis = width > 3;
        if (useEllipsis) {
            targetWidth -= 3;
        }
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int cw = isCjk(c) ? 2 : 1;
            if (w + cw > targetWidth) {
                break;
            }
            sb.append(c);
            w += cw;
        }
        if (useEllipsis) {
            sb.append("...");
            w += 3;
        }
        // Pad remaining if any (due to CJK width mismatch or short cut)
        while (w < width) {
            sb.append(' ');
            w++;
        }
        return sb.toString();
    }

    /**
     * Pads a string with the specified number of spaces.
     *
     * @param str           the string to pad
     * @param paddingLength the number of spaces to add
     * @return the padded string
     */
    private static String padString(final String str, final int paddingLength) {
        StringBuilder sb = new StringBuilder(str);
        for (int i = 0; i < paddingLength; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
