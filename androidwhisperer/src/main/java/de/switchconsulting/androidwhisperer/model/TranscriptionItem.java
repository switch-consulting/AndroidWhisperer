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
 * Common interface for chronological transcription timeline items (segments, tokens, and pauses).
 */
public interface TranscriptionItem {

    /**
     * Returns the start time of this item in milliseconds.
     *
     * @return start time in ms
     */
    long getStartMs();

    /**
     * Returns the end time of this item in milliseconds.
     *
     * @return end time in ms
     */
    long getEndMs();

    /**
     * Returns the duration of this item in milliseconds.
     *
     * @return duration in ms
     */
    long getDurationMs();

    /**
     * Returns a JSON object representation of this transcription item.
     *
     * @return JSON object representation
     * @throws JSONException if an error occurs while creating the JSON object
     */
    @NonNull
    JSONObject toJSON() throws JSONException;
}
