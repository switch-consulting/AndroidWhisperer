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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Complete transcription result containing full text, detected language, segments, and pause intervals.
 */
public final class TranscriptionResult {

    private final String text;
    private final String language;
    private final List<SegmentResult> segments;
    private final List<PauseSegment> pauses;

    /**
     * Constructs a transcription result with full text, language, segments, and pauses.
     *
     * @param text full transcribed text
     * @param language detected language code (e.g. "en", "de")
     * @param segments list of decoded speech segments ({@link SegmentResult})
     * @param pauses list of detected pause intervals ({@link PauseSegment})
     */
    public TranscriptionResult(
            String text,
            String language,
            List<SegmentResult> segments,
            List<PauseSegment> pauses) {
        this.text = text == null ? "" : text;
        this.language = language == null ? "" : language;
        this.segments = Collections.unmodifiableList(
                new ArrayList<>(segments == null
                        ? Collections.emptyList() : segments));
        this.pauses = Collections.unmodifiableList(
                new ArrayList<>(pauses == null
                        ? Collections.emptyList() : pauses));
    }

    /**
     * Returns the full transcribed text.
     *
     * @return full transcript text
     */
    public String getText() { return text; }

    /**
     * Returns the detected language code (e.g. "en", "de").
     *
     * @return language code
     */
    public String getLanguage() { return language; }

    /**
     * Returns the list of speech segments.
     *
     * @return unmodifiable list of segment results
     */
    public List<SegmentResult> getSegments() { return segments; }

    /**
     * Returns the list of all detected pause intervals (segment-level and/or token-level).
     *
     * @return unmodifiable list of pause segments
     */
    public List<PauseSegment> getPauses() { return pauses; }
}