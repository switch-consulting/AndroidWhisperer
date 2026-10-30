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

/**
 * Exception thrown when an error occurs during audio transcription.
 */
public class TranscriptionException extends RuntimeException {

    /**
     * Constructs a transcription exception with the specified detail message.
     *
     * @param message the detail message
     */
    public TranscriptionException(String message) {
        super(message);
    }

    /**
     * Constructs a transcription exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public TranscriptionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a transcription exception with the specified cause.
     *
     * @param cause the cause
     */
    public TranscriptionException(Throwable cause) {
        super(cause);
    }
}
