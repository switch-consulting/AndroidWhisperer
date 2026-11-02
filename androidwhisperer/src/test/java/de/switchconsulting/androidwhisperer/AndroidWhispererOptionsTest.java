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
        assertEquals(PauseMode.BOTH, opts.getPauseMode());
        assertEquals(300, opts.getSegmentPauseThresholdMs());
        assertEquals(100, opts.getTokenPauseThresholdMs());
        assertNull(opts.getLocale());
    }

    @Test
    public void customConstructor_setsFieldsCorrectly() {
        AndroidWhispererOptions custom = new AndroidWhispererOptions(
                PauseMode.TOKEN, 500, 150);
        assertEquals(PauseMode.TOKEN, custom.getPauseMode());
        assertEquals(500, custom.getSegmentPauseThresholdMs());
        assertEquals(150, custom.getTokenPauseThresholdMs());
        assertNull(custom.getLocale());
    }

    @Test
    public void customConstructor_withLocale_setsLocaleCorrectly() {
        AndroidWhispererOptions custom = new AndroidWhispererOptions(
                PauseMode.TOKEN, 500, 150, Locale.GERMAN);
        assertEquals(PauseMode.TOKEN, custom.getPauseMode());
        assertEquals(500, custom.getSegmentPauseThresholdMs());
        assertEquals(150, custom.getTokenPauseThresholdMs());
        assertEquals(Locale.GERMAN, custom.getLocale());
    }

    @Test
    public void customConstructor_zeroThresholds_allowed() {
        AndroidWhispererOptions zero = new AndroidWhispererOptions(
                PauseMode.NONE, 0, 0);
        assertEquals(PauseMode.NONE, zero.getPauseMode());
        assertEquals(0, zero.getSegmentPauseThresholdMs());
        assertEquals(0, zero.getTokenPauseThresholdMs());
        assertNull(zero.getLocale());
    }

    @Test(expected = IllegalArgumentException.class)
    public void options_nullPauseMode_throws() {
        new AndroidWhispererOptions(null, 300, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void options_negativeSegmentThreshold_throws() {
        new AndroidWhispererOptions(PauseMode.BOTH, -1, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void options_negativeTokenThreshold_throws() {
        new AndroidWhispererOptions(PauseMode.BOTH, 300, -1);
    }
}
