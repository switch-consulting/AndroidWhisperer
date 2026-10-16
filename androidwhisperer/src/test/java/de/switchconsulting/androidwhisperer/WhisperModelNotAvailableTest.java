package de.switchconsulting.androidwhisperer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WhisperModelNotAvailableTest {

    @Test
    public void whisperModelNotAvailable_exceptions() {
        WhisperModelNotAvailable ex1 = new WhisperModelNotAvailable("Not available");
        assertEquals("Not available", ex1.getMessage());

        Throwable cause = new Throwable("Cause");
        WhisperModelNotAvailable ex2 = new WhisperModelNotAvailable("Not available", cause);
        assertEquals("Not available", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
    }
}
