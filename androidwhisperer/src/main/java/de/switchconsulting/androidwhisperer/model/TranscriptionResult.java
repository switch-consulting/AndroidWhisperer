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
 * Complete transcription result containing full transcribed text, detected language ({@link Locale}),
 * optional target translated language, and a chronological list of {@link TranscriptionItem}s (speech segments and pause intervals).
 * <p>
 * Implements {@link TranscriptionItem} to represent the overall time range spanned by the contained items.
 */
public final class TranscriptionResult implements TranscriptionItem {

    private final String text;
    private final Locale language;
    private final Locale translatedLanguage;
    private final List<TranscriptionItem> items;
    private final long startMs;
    private final long endMs;
    private final long durationMs;

    /**
     * Constructs a transcription result with full text, language, translated language, and chronological items.
     *
     * @param text full transcribed text
     * @param language detected language ({@link Locale})
     * @param translatedLanguage target language if translated ({@link Locale}), or null if not translated
     * @param items chronological list of transcription items (speech segments and pause intervals)
     */
    public TranscriptionResult(
            @Nullable String text,
            @Nullable Locale language,
            @Nullable Locale translatedLanguage,
            @Nullable List<? extends TranscriptionItem> items) {
        this.text = text == null ? "" : text;
        this.language = language;
        this.translatedLanguage = translatedLanguage;
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
     * Constructs a transcription result with full text, language, and chronological items.
     *
     * @param text full transcribed text
     * @param language detected language ({@link Locale})
     * @param items chronological list of transcription items (speech segments and pause intervals)
     */
    public TranscriptionResult(
            @Nullable String text,
            @Nullable Locale language,
            @Nullable List<? extends TranscriptionItem> items) {
        this(text, language, null, items);
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

    /**
     * Returns the target translated language ({@link Locale}), or null if no translation was performed.
     *
     * @return translated language locale or null
     */
    @Nullable
    public Locale getTranslatedLanguage() { return translatedLanguage; }

    /**
     * Returns the start timestamp in milliseconds of the first item in the result, or 0 if empty.
     *
     * @return start timestamp in milliseconds
     */
    @Override
    public long getStartMs() { return startMs; }

    /**
     * Returns the end timestamp in milliseconds of the last item in the result, or 0 if empty.
     *
     * @return end timestamp in milliseconds
     */
    @Override
    public long getEndMs() { return endMs; }

    /**
     * Returns the total duration in milliseconds spanned by the result items.
     *
     * @return duration in milliseconds
     */
    @Override
    public long getDurationMs() { return durationMs; }

    /**
     * Returns the chronological list of transcription items (speech segments and pause intervals).
     *
     * @return unmodifiable list of transcription items
     */
    @NonNull
    public List<TranscriptionItem> getItems() { return items; }

    /**
     * Returns the list of speech segments extracted from the chronological items.
     *
     * @return unmodifiable list of segment results
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
     * Returns a JSON object representation of this transcription result.
     *
     * @return JSON object containing type, text, language, translatedLanguage, startMs, endMs, durationMs, and items
     * @throws JSONException if an error occurs while creating the JSON object
     */
    @NonNull
    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "TRANSCRIPTION");
        json.put("text", text);
        json.put("language", language != null ? language.toLanguageTag() : JSONObject.NULL);
        json.put("translatedLanguage", translatedLanguage != null ? translatedLanguage.toLanguageTag() : JSONObject.NULL);
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
