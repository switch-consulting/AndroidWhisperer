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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single decoded speech segment from Whisper with timing, tokens, and pause intervals.
 */
public final class SegmentResult implements TranscriptionItem {

    private final String text;
    private final long startMs;
    private final long endMs;
    private final long durationMs;
    private final List<TranscriptionItem> items;

    /**
     * Constructs a segment result with transcribed text, start/end timestamps, and chronological timeline items.
     *
     * @param text transcribed text for this segment
     * @param startMs start time in milliseconds
     * @param endMs end time in milliseconds
     * @param items chronological list of transcription items (tokens and pauses) within this segment
     */
    public SegmentResult(
            @Nullable String text, long startMs, long endMs,
            @Nullable List<? extends TranscriptionItem> items) {
        if (startMs < 0 || endMs < startMs) {
            throw new IllegalArgumentException("Invalid segment interval");
        }
        this.text = text == null ? "" : text;
        this.startMs = startMs;
        this.endMs = endMs;
        this.durationMs = endMs - startMs;
        this.items = Collections.unmodifiableList(
                new ArrayList<>(items == null
                        ? Collections.emptyList() : items));
    }

    /**
     * Returns the transcribed text of this segment.
     *
     * @return segment text
     */
    @NonNull
    public String getText() { return text; }

    /**
     * Returns the start time of this segment in milliseconds.
     *
     * @return start time in ms
     */
    @Override
    public long getStartMs() { return startMs; }

    /**
     * Returns the end time of this segment in milliseconds.
     *
     * @return end time in ms
     */
    @Override
    public long getEndMs() { return endMs; }

    /**
     * Returns the duration of this segment in milliseconds.
     *
     * @return duration in ms
     */
    @Override
    public long getDurationMs() { return durationMs; }

    /**
     * Returns the chronological list of transcription items (tokens and pauses) within this segment.
     *
     * @return unmodifiable list of transcription items
     */
    @NonNull
    public List<TranscriptionItem> getItems() { return items; }

    /**
     * Returns the list of token results detected within this segment.
     *
     * @return list of token results
     */
    @NonNull
    public List<TokenResult> getTokens() {
        List<TokenResult> tokens = new ArrayList<>();
        for (TranscriptionItem item : items) {
            if (item instanceof TokenResult) {
                tokens.add((TokenResult) item);
            }
        }
        return Collections.unmodifiableList(tokens);
    }

    /**
     * Returns the list of pause segments detected within this segment.
     *
     * @return list of pause segments
     */
    @NonNull
    public List<PauseSegment> getSegmentPauses() {
        List<PauseSegment> pauses = new ArrayList<>();
        for (TranscriptionItem item : items) {
            if (item instanceof PauseSegment) {
                pauses.add((PauseSegment) item);
            }
        }
        return Collections.unmodifiableList(pauses);
    }

    /**
     * Returns a JSON object representation of this segment result.
     *
     * @return JSON object containing type, text, startMs, endMs, durationMs, and items
     * @throws JSONException if an error occurs while creating the JSON object
     */
    @NonNull
    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "SEGMENT");
        json.put("text", text);
        json.put("startMs", startMs);
        json.put("endMs", endMs);
        json.put("durationMs", durationMs);
        JSONArray itemsArray = new JSONArray();
        for (TranscriptionItem item : items) {
            itemsArray.put(item.toJSON());
        }
        json.put("items", itemsArray);
        return json;
    }
}
