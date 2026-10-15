package de.switchconsulting.androidwhisperer.internal;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Stable, project-owned C ABI.
 *
 * The bridge owns the native whisper_full_params structure and calls
 * whisper_full_default_params() and whisper_full() internally.
 */
public interface WhisperBridge extends Library {

    WhisperBridge INSTANCE = Native.load("whisper", WhisperBridge.class);

    Pointer whisper_init_from_file(String modelPath);
    void whisper_free(Pointer context);

    /**
     * Executes inference. Returns zero on success.
     * tokenTimestamps enables native token alignment/timestamps.
     */
    int whisper_bridge_transcribe(
            Pointer context,
            float[] samples,
            int sampleCount,
            int tokenTimestamps);

    /** Returns detected language ID, or a negative value on failure. */
    int whisper_bridge_detect_language(
            Pointer context, float[] samples, int sampleCount);

    int whisper_full_n_segments(Pointer context);
    long whisper_full_get_segment_t0(Pointer context, int segmentIndex);
    long whisper_full_get_segment_t1(Pointer context, int segmentIndex);
    String whisper_full_get_segment_text(Pointer context, int segmentIndex);

    int whisper_full_n_tokens(Pointer context, int segmentIndex);
    long whisper_full_get_token_t0(
            Pointer context, int segmentIndex, int tokenIndex);
    long whisper_full_get_token_t1(
            Pointer context, int segmentIndex, int tokenIndex);
    String whisper_full_get_token_text(
            Pointer context, int segmentIndex, int tokenIndex);

    String whisper_lang_str(int languageId);
}