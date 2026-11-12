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
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TranscriptionResultTest {

    @Test
    public void transcriptionResult_valid() {
        SegmentResult seg = new SegmentResult("Hi", 0, 100, Collections.emptyList());
        PauseSegment pause = new PauseSegment(100, 200, PauseLevel.SEGMENT);
        TranscriptionResult tr = new TranscriptionResult("Hi", Locale.ENGLISH, Collections.singletonList(seg), Collections.singletonList(pause));

        assertEquals("Hi", tr.getText());
        assertEquals(Locale.ENGLISH, tr.getLanguage());
        assertEquals(1, tr.getSegments().size());
        assertEquals(1, tr.getAllPauses().size());

        TranscriptionResult nullSafe = new TranscriptionResult(null, null, null, null);
        assertEquals("", nullSafe.getText());
        assertNull(nullSafe.getLanguage());
        assertNotNull(nullSafe.getSegments());
        assertNotNull(nullSafe.getAllPauses());
    }

    @Test
    public void transcriptionResult_toJSON() throws Exception {
        SegmentResult seg = new SegmentResult("Hi", 0, 100, Collections.emptyList());
        PauseSegment pause = new PauseSegment(100, 200, PauseLevel.SEGMENT);
        TranscriptionResult tr = new TranscriptionResult("Hi", Locale.ENGLISH, Collections.singletonList(seg), Collections.singletonList(pause));
        org.json.JSONObject json = tr.toJSON();
        assertEquals("Hi", json.getString("text"));
        assertEquals("en", json.getString("language"));
        assertEquals(1, json.getJSONArray("segments").length());
        assertEquals(1, json.getJSONArray("pauses").length());
    }
}
