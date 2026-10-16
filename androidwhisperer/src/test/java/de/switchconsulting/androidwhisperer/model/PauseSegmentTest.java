package de.switchconsulting.androidwhisperer.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PauseSegmentTest {

    @Test
    public void pauseSegment_valid() {
        PauseSegment pause = new PauseSegment(100, 300, PauseSegment.Level.SEGMENT);
        assertEquals(100, pause.getStartMs());
        assertEquals(300, pause.getEndMs());
        assertEquals(200, pause.getDurationMs());
        assertEquals(PauseSegment.Level.SEGMENT, pause.getLevel());
    }

    @Test
    public void pauseSegment_invalidStart_throwsException() {
        try {
            new PauseSegment(-1, 100, PauseSegment.Level.TOKEN);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid pause interval", e.getMessage());
        }
    }

    @Test
    public void pauseSegment_invalidEnd_throwsException() {
        try {
            new PauseSegment(200, 100, PauseSegment.Level.TOKEN);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid pause interval", e.getMessage());
        }
    }

    @Test
    public void pauseSegment_nullLevel_throwsException() {
        try {
            new PauseSegment(0, 100, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("level == null", e.getMessage());
        }
    }
}
