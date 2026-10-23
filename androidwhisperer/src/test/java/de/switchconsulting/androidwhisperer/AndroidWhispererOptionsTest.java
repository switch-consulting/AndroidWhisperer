package de.switchconsulting.androidwhisperer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AndroidWhispererOptionsTest {

    @Test
    public void defaults_returnsExpectedValues() {
        AndroidWhispererOptions opts = AndroidWhispererOptions.defaults();
        assertNotNull(opts);
        assertEquals(AndroidWhisperer.PauseMode.BOTH, opts.getPauseMode());
        assertEquals(300, opts.getSegmentPauseThresholdMs());
        assertEquals(100, opts.getTokenPauseThresholdMs());
    }

    @Test
    public void customConstructor_setsFieldsCorrectly() {
        AndroidWhispererOptions custom = new AndroidWhispererOptions(
                AndroidWhisperer.PauseMode.TOKEN, 500, 150);
        assertEquals(AndroidWhisperer.PauseMode.TOKEN, custom.getPauseMode());
        assertEquals(500, custom.getSegmentPauseThresholdMs());
        assertEquals(150, custom.getTokenPauseThresholdMs());
    }

    @Test
    public void customConstructor_zeroThresholds_allowed() {
        AndroidWhispererOptions zero = new AndroidWhispererOptions(
                AndroidWhisperer.PauseMode.NONE, 0, 0);
        assertEquals(AndroidWhisperer.PauseMode.NONE, zero.getPauseMode());
        assertEquals(0, zero.getSegmentPauseThresholdMs());
        assertEquals(0, zero.getTokenPauseThresholdMs());
    }

    @Test(expected = IllegalArgumentException.class)
    public void options_nullPauseMode_throws() {
        new AndroidWhispererOptions(null, 300, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void options_negativeSegmentThreshold_throws() {
        new AndroidWhispererOptions(AndroidWhisperer.PauseMode.BOTH, -1, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void options_negativeTokenThreshold_throws() {
        new AndroidWhispererOptions(AndroidWhisperer.PauseMode.BOTH, 300, -1);
    }
}
