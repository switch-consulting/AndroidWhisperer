package de.switchconsulting.androidwhisperer.model;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TranscriptionResultTest {

    @Test
    public void transcriptionResult_valid() {
        SegmentResult seg = new SegmentResult("Hi", 0, 100, Collections.emptyList());
        PauseSegment pause = new PauseSegment(100, 200, PauseSegment.Level.SEGMENT);
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
