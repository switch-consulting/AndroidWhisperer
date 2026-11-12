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
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an individual decoded speech token with text and timing.
 */
public final class TokenResult implements TranscriptionItem {

    private final String text;
    private final long startMs;
    private final long endMs;
    private final long durationMs;

    /**
     * Constructs a token result with text, start time, and end time.
     *
     * @param text token text
     * @param startMs start time in milliseconds
     * @param endMs end time in milliseconds
     */
    public TokenResult(@Nullable String text, long startMs, long endMs) {
        if (startMs < 0 || endMs < startMs) {
            throw new IllegalArgumentException("Invalid token interval");
        }
        this.text = text == null ? "" : text;
        this.startMs = startMs;
        this.endMs = endMs;
        this.durationMs = endMs - startMs;
    }

    /**
     * Returns the text of this token.
     *
     * @return token text
     */
    @NonNull
    public String getText() {
        return text;
    }

    @Override
    public long getStartMs() {
        return startMs;
    }

    @Override
    public long getEndMs() {
        return endMs;
    }

    @Override
    public long getDurationMs() {
        return durationMs;
    }

    @NonNull
    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "TOKEN");
        json.put("text", text);
        json.put("startMs", startMs);
        json.put("endMs", endMs);
        json.put("durationMs", durationMs);
        return json;
    }
}
