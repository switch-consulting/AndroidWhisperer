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

/** One Whisper speech segment with optional token-level detail. */
public final class SegmentResult {

    private final String text;
    private final long startMs;
    private final long endMs;
    private final List<PauseSegment> pauses;

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

    public String getText() { return text; }
    public long getStartMs() { return startMs; }
    public long getEndMs() { return endMs; }
    public List<PauseSegment> getPauses() { return pauses; }
}