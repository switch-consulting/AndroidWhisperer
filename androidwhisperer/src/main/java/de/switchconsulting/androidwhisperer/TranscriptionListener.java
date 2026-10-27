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

import de.switchconsulting.androidwhisperer.model.SegmentResult;

/**
 * Listener interface for receiving incremental updates during audio transcription.
 */
public interface TranscriptionListener {

    /**
     * Called whenever a new text segment is decoded natively.
     *
     * @param segment the newly decoded segment details
     * @param accumulatedTranscript the full accumulated transcript text up to this segment
     */
    void onNewSegment(SegmentResult segment, String accumulatedTranscript);

    /**
     * Called periodically during transcription with overall progress percentage (0 to 100).
     *
     * @param progressPercent current progress percentage
     */
    void onProgress(int progressPercent);
}
