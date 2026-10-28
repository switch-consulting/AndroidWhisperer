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

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TranscriptionResultTest {

    @Test
    public void transcriptionResult_valid() {
        SegmentResult seg = new SegmentResult("Hi", 0, 100, Collections.emptyList());
        PauseSegment pause = new PauseSegment(100, 200, PauseLevel.SEGMENT);
        TranscriptionResult tr = new TranscriptionResult("Hi", "en", Collections.singletonList(seg), Collections.singletonList(pause));

        assertEquals("Hi", tr.getText());
        assertEquals("en", tr.getLanguage());
        assertEquals(1, tr.getSegments().size());
        assertEquals(1, tr.getPauses().size());

        TranscriptionResult nullSafe = new TranscriptionResult(null, null, null, null);
        assertEquals("", nullSafe.getText());
        assertEquals("", nullSafe.getLanguage());
        assertNotNull(nullSafe.getSegments());
        assertNotNull(nullSafe.getPauses());
    }
}
