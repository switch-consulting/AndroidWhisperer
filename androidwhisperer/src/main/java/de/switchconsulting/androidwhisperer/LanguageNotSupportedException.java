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

import java.util.Locale;

/**
 * Exception thrown when a requested language or locale is not supported by the current Whisper model.
 */
public class LanguageNotSupportedException extends TranscriptionException {

    private final Locale locale;

    /**
     * Constructs a language not supported exception with the specified detail message.
     *
     * @param message the detail message
     */
    public LanguageNotSupportedException(String message) {
        super(message);
        this.locale = null;
    }

    /**
     * Constructs a language not supported exception for the specified locale.
     *
     * @param locale the unsupported locale
     */
    public LanguageNotSupportedException(Locale locale) {
        super("Language not supported for locale: " + (locale != null ? locale : "null"));
        this.locale = locale;
    }

    /**
     * Constructs a language not supported exception for the specified locale with detail message.
     *
     * @param locale the unsupported locale
     * @param message the detail message
     */
    public LanguageNotSupportedException(Locale locale, String message) {
        super(message);
        this.locale = locale;
    }

    /**
     * Constructs a language not supported exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public LanguageNotSupportedException(String message, Throwable cause) {
        super(message, cause);
        this.locale = null;
    }

    /**
     * Returns the unsupported locale associated with this exception, or {@code null} if not specified.
     *
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }
}
