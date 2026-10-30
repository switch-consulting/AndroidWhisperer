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

/**
 * Configuration options for transcription behavior and pause analysis.
 */
public final class AndroidWhispererOptions {
    private final PauseMode pauseMode;
    private final long segmentPauseThresholdMs;
    private final long tokenPauseThresholdMs;

    /**
     * Creates new transcription options with specified pause mode and thresholds.
     *
     * @param pauseMode the pause analysis mode ({@link PauseMode})
     * @param segmentPauseThresholdMs minimum duration in milliseconds to detect a segment pause
     * @param tokenPauseThresholdMs minimum duration in milliseconds to detect a token pause
     */
    public AndroidWhispererOptions(
            PauseMode pauseMode,
            long segmentPauseThresholdMs,
            long tokenPauseThresholdMs) {
        if (pauseMode == null
                || segmentPauseThresholdMs < 0
                || tokenPauseThresholdMs < 0) {
            throw new IllegalArgumentException("Invalid options");
        }
        this.pauseMode = pauseMode;
        this.segmentPauseThresholdMs = segmentPauseThresholdMs;
        this.tokenPauseThresholdMs = tokenPauseThresholdMs;
    }

    /**
     * Returns the default transcription options ({@link PauseMode#BOTH}, 300ms segment threshold, 100ms token threshold).
     *
     * @return default options instance
     */
    public static AndroidWhispererOptions defaults() {
        return new AndroidWhispererOptions(PauseMode.BOTH, 300, 100);
    }

    /**
     * Returns the configured pause analysis mode.
     *
     * @return the pause mode
     */
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
}
