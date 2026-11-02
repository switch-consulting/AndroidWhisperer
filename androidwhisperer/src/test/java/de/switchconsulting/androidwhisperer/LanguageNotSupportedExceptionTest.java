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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LanguageNotSupportedExceptionTest {

    @Test
    public void constructor_withMessage_setsMessage() {
        LanguageNotSupportedException ex = new LanguageNotSupportedException("Unsupported language");
        assertEquals("Unsupported language", ex.getMessage());
        assertNull(ex.getLocale());
        assertTrue(ex instanceof TranscriptionException);
    }

    @Test
    public void constructor_withLocale_setsLocaleAndMessage() {
        LanguageNotSupportedException ex = new LanguageNotSupportedException(Locale.GERMAN);
        assertEquals(Locale.GERMAN, ex.getLocale());
        assertTrue(ex.getMessage().contains("de"));
    }

    @Test
    public void constructor_withLocaleAndMessage_setsBoth() {
        LanguageNotSupportedException ex = new LanguageNotSupportedException(Locale.FRENCH, "French not supported");
        assertEquals(Locale.FRENCH, ex.getLocale());
        assertEquals("French not supported", ex.getMessage());
    }

    @Test
    public void constructor_withMessageAndCause_setsBoth() {
        Throwable cause = new IllegalArgumentException("Invalid language");
        LanguageNotSupportedException ex = new LanguageNotSupportedException("Failed", cause);
        assertEquals("Failed", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertNull(ex.getLocale());
    }
}
