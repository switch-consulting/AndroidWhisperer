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
 * Represents a single decoded speech segment from Whisper with timing and optional token-level pauses.
 */
public final class SegmentResult {

    private final String text;
    private final long startMs;
    private final long endMs;
    private final List<PauseSegment> pauses;

    /**
     * Constructs a segment result with transcribed text, start/end timestamps, and token pauses.
     *
     * @param text transcribed text for this segment
     * @param startMs start time in milliseconds
     * @param endMs end time in milliseconds
     * @param pauses list of token pauses within this segment
     */
    public SegmentResult(
            String text, long startMs, long endMs,
            List<PauseSegment> pauses) {
        if (startMs < 0 || endMs < startMs) {
            throw new IllegalArgumentException("Invalid segment interval");
        }
        this.text = text == null ? "" : text;
        this.startMs = startMs;
        this.endMs = endMs;
        this.pauses = Collections.unmodifiableList(
                new ArrayList<>(pauses == null
                        ? Collections.emptyList() : pauses));
    }

    /**
     * Returns the transcribed text of this segment.
     *
     * @return segment text
     */
    public String getText() { return text; }

    /**
     * Returns the start time of this segment in milliseconds.
     *
     * @return start time in ms
     */
    public long getStartMs() { return startMs; }

    /**
     * Returns the end time of this segment in milliseconds.
     *
     * @return end time in ms
     */
    public long getEndMs() { return endMs; }

    /**
     * Returns the list of token-level pauses detected within this segment.
     *
     * @return unmodifiable list of pause segments
     */
    public List<PauseSegment> getPauses() { return pauses; }
}