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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Complete transcription result containing full text, detected language, and chronological items (segments and pauses).
 */
public final class TranscriptionResult implements TranscriptionItem {

    private final String text;
    private final Locale language;
    private final List<TranscriptionItem> items;
    private final long startMs;
    private final long endMs;
    private final long durationMs;

    /**
     * Constructs a transcription result with full text, language, and chronological items.
     *
     * @param text full transcribed text
     * @param language detected language ({@link Locale})
     * @param items chronological list of transcription items (segments and pauses)
     */
    public TranscriptionResult(
            @Nullable String text,
            @Nullable Locale language,
            @Nullable List<? extends TranscriptionItem> items) {
        this.text = text == null ? "" : text;
        this.language = language;
        List<TranscriptionItem> mutableItems = new ArrayList<>(items == null ? Collections.emptyList() : items);
        // Sort items chronologically by startMs
        mutableItems.sort(Comparator.comparingLong(TranscriptionItem::getStartMs));
        this.items = Collections.unmodifiableList(mutableItems);

        if (!this.items.isEmpty()) {
            this.startMs = this.items.get(0).getStartMs();
            this.endMs = this.items.get(this.items.size() - 1).getEndMs();
            this.durationMs = Math.max(0, this.endMs - this.startMs);
        } else {
            this.startMs = 0;
            this.endMs = 0;
            this.durationMs = 0;
        }
    }

    /**
     * Constructs a transcription result with full text, language, segments, and pauses (legacy constructor).
     *
     * @param text full transcribed text
     * @param language detected language ({@link Locale})
     * @param segments list of decoded speech segments ({@link SegmentResult})
     * @param pauses list of detected pause intervals ({@link PauseSegment})
     */
    public TranscriptionResult(
            @Nullable String text,
            @Nullable Locale language,
            @Nullable List<SegmentResult> segments,
            @Nullable List<PauseSegment> pauses) {
        this.text = text == null ? "" : text;
        this.language = language;
        List<TranscriptionItem> combined = new ArrayList<>();
        if (segments != null) {
            combined.addAll(segments);
        }
        if (pauses != null) {
            combined.addAll(pauses);
        }
        combined.sort(Comparator.comparingLong(TranscriptionItem::getStartMs));
        this.items = Collections.unmodifiableList(combined);

        if (!this.items.isEmpty()) {
            this.startMs = this.items.get(0).getStartMs();
            this.endMs = this.items.get(this.items.size() - 1).getEndMs();
            this.durationMs = Math.max(0, this.endMs - this.startMs);
        } else {
            this.startMs = 0;
            this.endMs = 0;
            this.durationMs = 0;
        }
    }

    /**
     * Returns the full transcribed text.
     *
     * @return full transcript text
     */
    @NonNull
    public String getText() { return text; }

    /**
     * Returns the detected language ({@link Locale}), or null if no language was provided or detected.
     *
     * @return language locale or null
     */
    @Nullable
    public Locale getLanguage() { return language; }

    @Override
    public long getStartMs() { return startMs; }

    @Override
    public long getEndMs() { return endMs; }

    @Override
    public long getDurationMs() { return durationMs; }

    /**
     * Returns the chronological list of transcription items (segments and pauses).
     *
     * @return unmodifiable list of transcription items
     */
    @NonNull
    public List<TranscriptionItem> getItems() { return items; }

    /**
     * Returns the list of speech segments.
     *
     * @return list of segment results
     */
    @NonNull
    public List<SegmentResult> getSegments() {
        List<SegmentResult> segments = new ArrayList<>();
        for (TranscriptionItem item : items) {
            if (item instanceof SegmentResult) {
                segments.add((SegmentResult) item);
            }
        }
        return Collections.unmodifiableList(segments);
    }

    /**
     * Returns the list of all detected pause intervals (segment-level and/or token-level).
     *
     * @return list of pause segments
     */
    @NonNull
    public List<PauseSegment> getAllPauses() {
        List<PauseSegment> pauses = new ArrayList<>();
        for (TranscriptionItem item : items) {
            if (item instanceof PauseSegment) {
                pauses.add((PauseSegment) item);
            }
        }
        return Collections.unmodifiableList(pauses);
    }

    /**
     * Returns a JSON object representation of this transcription result.
     *
     * @return JSON object containing type, text, language, startMs, endMs, durationMs, and items
     * @throws JSONException if an error occurs while creating the JSON object
     */
    @NonNull
    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "TRANSCRIPTION");
        json.put("text", text);
        json.put("language", language != null ? language.toLanguageTag() : JSONObject.NULL);
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
