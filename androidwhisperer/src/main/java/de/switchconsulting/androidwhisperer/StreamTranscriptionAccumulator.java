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

import androidx.annotation.NonNull;

import de.switchconsulting.androidwhisperer.model.TranscriptionItem;
import de.switchconsulting.androidwhisperer.model.TranscriptionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Package-private helper accumulator to build cumulative transcription results
 * when streaming PCM16 chunks through {@link AndroidWhisperer}.
 *
 * @author Dirk Schnelle-Walka
 */
final class StreamTranscriptionAccumulator {

    private final List<TranscriptionItem> allItems = new ArrayList<>();
    private final StringBuilder fullTranscript = new StringBuilder();
    private Locale detectedLanguage = null;
    private Locale translatedLanguage = null;
    private long cumulativeTimeMs = 0L;

    StreamTranscriptionAccumulator() {
    }

    void accumulate(@NonNull TranscriptionResult chunkResult, int samplesWritten) {
        if (detectedLanguage == null && chunkResult.getLanguage() != null) {
            detectedLanguage = chunkResult.getLanguage();
        }
        if (translatedLanguage == null && chunkResult.getTranslatedLanguage() != null) {
            translatedLanguage = chunkResult.getTranslatedLanguage();
        }

        if (fullTranscript.length() > 0 && !chunkResult.getText().isEmpty()) {
            fullTranscript.append(' ');
        }
        fullTranscript.append(chunkResult.getText());

        long chunkDurationMs = (long) ((samplesWritten / 16000.0) * 1000.0);
        for (TranscriptionItem item : chunkResult.getItems()) {
            allItems.add(AndroidWhisperer.offsetTranscriptionItem(item, cumulativeTimeMs));
        }
        cumulativeTimeMs += chunkDurationMs;
    }

    @NonNull
    TranscriptionResult buildResult() {
        return new TranscriptionResult(
                fullTranscript.toString().trim(),
                detectedLanguage,
                translatedLanguage,
                allItems);
    }
}
