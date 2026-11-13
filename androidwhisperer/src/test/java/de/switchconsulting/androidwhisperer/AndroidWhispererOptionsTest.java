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

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AndroidWhispererOptionsTest {

    @Test
    public void defaults_returnsExpectedValues() {
        AndroidWhispererOptions opts = AndroidWhispererOptions.defaults();
        assertNotNull(opts);
        assertEquals(PauseMode.DEFAULT, opts.getPauseMode());
        assertEquals(AndroidWhispererOptions.DEFAULT_SEGMENT_PAUSE_THRESHOLD_MS, opts.getSegmentPauseThresholdMs());
        assertEquals(AndroidWhispererOptions.DEFAULT_TOKEN_PAUSE_THRESHOLD_MS, opts.getTokenPauseThresholdMs());
        assertNull(opts.getLocale());
    }

    @Test
    public void fluentChaining_setsFieldsCorrectly() {
        AndroidWhispererOptions custom = AndroidWhispererOptions.defaults()
                .withPauseMode(PauseMode.TOKEN)
                .withSegmentPauseThresholdMs(500)
                .withTokenPauseThresholdMs(150);
        assertEquals(PauseMode.TOKEN, custom.getPauseMode());
        assertEquals(500, custom.getSegmentPauseThresholdMs());
        assertEquals(150, custom.getTokenPauseThresholdMs());
        assertNull(custom.getLocale());
    }

    @Test
    public void fluentChaining_withLocale_setsLocaleCorrectly() {
        AndroidWhispererOptions custom = AndroidWhispererOptions.defaults()
                .withPauseMode(PauseMode.TOKEN)
                .withSegmentPauseThresholdMs(500)
                .withTokenPauseThresholdMs(150)
                .withLocale(Locale.GERMAN);
        assertEquals(PauseMode.TOKEN, custom.getPauseMode());
        assertEquals(500, custom.getSegmentPauseThresholdMs());
        assertEquals(150, custom.getTokenPauseThresholdMs());
        assertEquals(Locale.GERMAN, custom.getLocale());
    }

    @Test
    public void fluentChaining_zeroThresholds_allowed() {
        AndroidWhispererOptions zero = AndroidWhispererOptions.defaults()
                .withPauseMode(PauseMode.NONE)
                .withSegmentPauseThresholdMs(0)
                .withTokenPauseThresholdMs(0);
        assertEquals(PauseMode.NONE, zero.getPauseMode());
        assertEquals(0, zero.getSegmentPauseThresholdMs());
        assertEquals(0, zero.getTokenPauseThresholdMs());
        assertNull(zero.getLocale());
    }



    @Test(expected = IllegalArgumentException.class)
    public void options_negativeSegmentThreshold_throws() {
        AndroidWhispererOptions.defaults().withSegmentPauseThresholdMs(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void options_negativeTokenThreshold_throws() {
        AndroidWhispererOptions.defaults().withTokenPauseThresholdMs(-1);
    }
}
