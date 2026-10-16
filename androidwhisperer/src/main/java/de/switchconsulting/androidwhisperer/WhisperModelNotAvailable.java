package de.switchconsulting.androidwhisperer;

/**
 * Exception thrown when a requested Whisper model is not available or not supported.
 */
public class WhisperModelNotAvailable extends RuntimeException {

    public WhisperModelNotAvailable(String message) {
        super(message);
    }

    public WhisperModelNotAvailable(String message, Throwable cause) {
        super(message, cause);
    }
}
