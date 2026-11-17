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

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Dirk Schnelle-Walka
 */
public class TranscriptionResultTest {

    @Test
    public void transcriptionResult_valid() {
        SegmentResult seg = new SegmentResult("Hi", 0, 100, Collections.emptyList());
        PauseSegment pause = new PauseSegment(100, 200, PauseLevel.SEGMENT);
        TranscriptionResult tr = new TranscriptionResult("Hi", Locale.ENGLISH, Arrays.asList(seg, pause));

        assertEquals("Hi", tr.getText());
        assertEquals(Locale.ENGLISH, tr.getLanguage());
        assertNull(tr.getTranslatedLanguage());
        assertEquals(2, tr.getItems().size());
        assertEquals(1, tr.getSegments().size());

        TranscriptionResult nullSafe = new TranscriptionResult(null, null, null, (List<TranscriptionItem>) null);
        assertEquals("", nullSafe.getText());
        assertNull(nullSafe.getLanguage());
        assertNull(nullSafe.getTranslatedLanguage());
        assertNotNull(nullSafe.getItems());
        assertNotNull(nullSafe.getSegments());
    }

    @Test
    public void transcriptionResult_withTranslatedLanguage() {
        SegmentResult seg = new SegmentResult("Hallo", 0, 100, Collections.emptyList());
        TranscriptionResult trItems = new TranscriptionResult("Hello", Locale.GERMAN, Locale.ENGLISH, Collections.singletonList(seg));

        assertEquals("Hello", trItems.getText());
        assertEquals(Locale.GERMAN, trItems.getLanguage());
        assertEquals(Locale.ENGLISH, trItems.getTranslatedLanguage());
    }

    @Test
    public void transcriptionResult_toJSON() throws Exception {
        SegmentResult seg = new SegmentResult("Hi", 0, 100, Collections.emptyList());
        PauseSegment pause = new PauseSegment(100, 200, PauseLevel.SEGMENT);
        TranscriptionResult tr = new TranscriptionResult("Hi", Locale.GERMAN, Locale.ENGLISH, Arrays.asList(seg, pause));
        JSONObject json = tr.toJSON();
        assertEquals("TRANSCRIPTION", json.getString("type"));
        assertEquals("Hi", json.getString("text"));
        assertEquals("de", json.getString("language"));
        assertEquals("en", json.getString("translatedLanguage"));
        JSONArray items = json.getJSONArray("items");
        assertEquals(2, items.length());

        TranscriptionResult trUntranslated = new TranscriptionResult("Hi", Locale.ENGLISH, Arrays.asList(seg, pause));
        JSONObject jsonUntranslated = trUntranslated.toJSON();
        assertEquals(JSONObject.NULL, jsonUntranslated.get("translatedLanguage"));
    }
}
