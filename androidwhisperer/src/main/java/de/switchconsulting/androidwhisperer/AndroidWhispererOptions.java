/*
 * Copyright 2026 Switch Consulting (https://switch-consulting.de/)
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.switchconsulting.androidwhisperer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Configuration options for transcription behavior and pause analysis.
 */
public final class AndroidWhispererOptions {
    /** Default segment pause threshold in milliseconds (300 ms). */
    public static final long DEFAULT_SEGMENT_PAUSE_THRESHOLD_MS = 300L;

    /** Default token pause threshold in milliseconds (100 ms). */
    public static final long DEFAULT_TOKEN_PAUSE_THRESHOLD_MS = 100L;

    private PauseMode pauseMode = PauseMode.DEFAULT;
    private long segmentPauseThresholdMs = DEFAULT_SEGMENT_PAUSE_THRESHOLD_MS;
    private long tokenPauseThresholdMs = DEFAULT_TOKEN_PAUSE_THRESHOLD_MS;
    private Locale locale = null;

    private AndroidWhispererOptions() {
    }

    /**
     * Returns the default transcription options ({@link PauseMode#DEFAULT}, {@link #DEFAULT_SEGMENT_PAUSE_THRESHOLD_MS}, {@link #DEFAULT_TOKEN_PAUSE_THRESHOLD_MS}, no language preselection).
     *
     * @return default options instance
     */
    @NonNull
    public static AndroidWhispererOptions defaults() {
        return new AndroidWhispererOptions();
    }

    /**
     * Returns the configured pause analysis mode.
     *
     * @return the pause mode
     */
    @NonNull
    public PauseMode getPauseMode() {
        return pauseMode;
    }

    /**
     * Returns the segment pause threshold in milliseconds.
     *
     * @return segment pause threshold in ms
     */
    public long getSegmentPauseThresholdMs() {
        return segmentPauseThresholdMs;
    }

    /**
     * Returns the token pause threshold in milliseconds.
     *
     * @return token pause threshold in ms
     */
    public long getTokenPauseThresholdMs() {
        return tokenPauseThresholdMs;
    }

    /**
     * Returns the preselected language locale, or {@code null} if no language is preselected.
     *
     * @return the preselected locale
     */
    @Nullable
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the pause mode and returns this options instance for chaining.
     *
     * @param pauseMode the pause analysis mode
     * @return this options instance
     */
    @NonNull
    public AndroidWhispererOptions withPauseMode(@NonNull PauseMode pauseMode) {
        this.pauseMode = pauseMode;
        return this;
    }

    /**
     * Sets the segment pause threshold in milliseconds and returns this options instance for chaining.
     *
     * @param segmentPauseThresholdMs minimum duration in milliseconds to detect a segment pause
     * @return this options instance
     */
    @NonNull
    public AndroidWhispererOptions withSegmentPauseThresholdMs(long segmentPauseThresholdMs) {
        if (segmentPauseThresholdMs < 0) {
            throw new IllegalArgumentException("Segment pause threshold cannot be negative");
        }
        this.segmentPauseThresholdMs = segmentPauseThresholdMs;
        return this;
    }

    /**
     * Sets the token pause threshold in milliseconds and returns this options instance for chaining.
     *
     * @param tokenPauseThresholdMs minimum duration in milliseconds to detect a token pause
     * @return this options instance
     */
    @NonNull
    public AndroidWhispererOptions withTokenPauseThresholdMs(long tokenPauseThresholdMs) {
        if (tokenPauseThresholdMs < 0) {
            throw new IllegalArgumentException("Token pause threshold cannot be negative");
        }
        this.tokenPauseThresholdMs = tokenPauseThresholdMs;
        return this;
    }

    /**
     * Sets the locale and returns this options instance for chaining.
     *
     * @param locale preselected language locale, or {@code null} for default/auto-detection
     * @return this options instance
     */
    @NonNull
    public AndroidWhispererOptions withLocale(@Nullable Locale locale) {
        this.locale = locale;
        return this;
    }
}
