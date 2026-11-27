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

import org.junit.Test;

import de.switchconsulting.androidwhisperer.model.SegmentResult;
import de.switchconsulting.androidwhisperer.model.TranscriptionItem;
import de.switchconsulting.androidwhisperer.model.TranscriptionResult;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link StreamTranscriptionAccumulator}.
 *
 * @author Dirk Schnelle-Walka
 */
public class StreamTranscriptionAccumulatorTest {

    @Test
    public void initialAccumulator_buildsEmptyResult() {
        StreamTranscriptionAccumulator accumulator = new StreamTranscriptionAccumulator();
        TranscriptionResult result = accumulator.buildResult();

        assertTrue(result.getText().isEmpty());
        assertNull(result.getLanguage());
        assertNull(result.getTranslatedLanguage());
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    public void accumulate_singleChunk_aggregatesTextAndLanguage() {
        StreamTranscriptionAccumulator accumulator = new StreamTranscriptionAccumulator();
        SegmentResult segment = new SegmentResult("Hello world", 0, 1000, Collections.emptyList());
        TranscriptionResult chunk1 = new TranscriptionResult(
                "Hello world", Locale.ENGLISH, Locale.ENGLISH, List.of(segment));

        accumulator.accumulate(chunk1, 16000); // 16000 samples = 1000 ms duration

        TranscriptionResult result = accumulator.buildResult();
        assertEquals("Hello world", result.getText());
        assertEquals(Locale.ENGLISH, result.getLanguage());
        assertEquals(Locale.ENGLISH, result.getTranslatedLanguage());
        assertEquals(1, result.getItems().size());
        assertEquals(0, result.getItems().get(0).getStartMs());
        assertEquals(1000, result.getItems().get(0).getEndMs());
    }

    @Test
    public void accumulate_multipleChunks_offsetsTimestampsCorrectly() {
        StreamTranscriptionAccumulator accumulator = new StreamTranscriptionAccumulator();

        SegmentResult seg1 = new SegmentResult("Hello", 0, 500, Collections.emptyList());
        TranscriptionResult chunk1 = new TranscriptionResult(
                "Hello", Locale.ENGLISH, null, List.of(seg1));

        SegmentResult seg2 = new SegmentResult("world", 0, 500, Collections.emptyList());
        TranscriptionResult chunk2 = new TranscriptionResult(
                "world", Locale.ENGLISH, null, List.of(seg2));

        accumulator.accumulate(chunk1, 16000); // 1000 ms chunk
        accumulator.accumulate(chunk2, 16000); // 1000 ms chunk

        TranscriptionResult result = accumulator.buildResult();
        assertEquals("Hello world", result.getText());
        assertEquals(2, result.getItems().size());

        TranscriptionItem item1 = result.getItems().get(0);
        assertEquals(0, item1.getStartMs());
        assertEquals(500, item1.getEndMs());

        TranscriptionItem item2 = result.getItems().get(1);
        assertEquals(1000, item2.getStartMs()); // offset by 1000 ms
        assertEquals(1500, item2.getEndMs());   // offset by 1000 ms
    }
}
