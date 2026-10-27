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

public final class AndroidWhispererOptions {
    private final AndroidWhisperer.PauseMode pauseMode;
    private final long segmentPauseThresholdMs;
    private final long tokenPauseThresholdMs;

    public AndroidWhispererOptions(
            AndroidWhisperer.PauseMode pauseMode,
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

    public static AndroidWhispererOptions defaults() {
        return new AndroidWhispererOptions(AndroidWhisperer.PauseMode.BOTH, 300, 100);
    }

    public AndroidWhisperer.PauseMode getPauseMode() {
        return pauseMode;
    }
    public long getSegmentPauseThresholdMs() {
        return segmentPauseThresholdMs;
    }
    public long getTokenPauseThresholdMs() {
        return tokenPauseThresholdMs;
    }
}
