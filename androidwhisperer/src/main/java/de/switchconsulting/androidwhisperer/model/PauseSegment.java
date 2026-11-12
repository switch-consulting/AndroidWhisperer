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

package de.switchconsulting.androidwhisperer.model;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A detected silent interval between two speech boundaries.
 */
public final class PauseSegment implements TranscriptionItem {

    private final long startMs;
    private final long endMs;
    private final long durationMs;
    private final PauseLevel level;

    /**
     * Constructs a pause segment with start time, end time, and granularity level.
     *
     * @param startMs start time in milliseconds
     * @param endMs end time in milliseconds
     * @param level the pause granularity level ({@link PauseLevel})
     */
    public PauseSegment(long startMs, long endMs, @NonNull PauseLevel level) {
        if (startMs < 0 || endMs < startMs) {
            throw new IllegalArgumentException("Invalid pause interval");
        }

        this.startMs = startMs;
        this.endMs = endMs;
        this.durationMs = endMs - startMs;
        this.level = level;
    }

    /**
     * Returns the start time of the pause in milliseconds.
     *
     * @return start time in ms
     */
    @Override
    public long getStartMs() { return startMs; }

    /**
     * Returns the end time of the pause in milliseconds.
     *
     * @return end time in ms
     */
    @Override
    public long getEndMs() { return endMs; }

    /**
     * Returns the duration of the pause in milliseconds.
     *
     * @return duration in ms
     */
    @Override
    public long getDurationMs() { return durationMs; }

    /**
     * Returns the granularity level of the pause.
     *
     * @return pause level
     */
    @NonNull
    public PauseLevel getLevel() { return level; }

    /**
     * Returns a JSON object representation of this pause segment.
     *
     * @return JSON object containing type, startMs, endMs, durationMs, and level
     * @throws JSONException if an error occurs while creating the JSON object
     */
    @NonNull
    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "PAUSE");
        json.put("startMs", startMs);
        json.put("endMs", endMs);
        json.put("durationMs", durationMs);
        json.put("level", level.name());
        return json;
    }
}
