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

    /**
     * Executes inference with incremental segment and progress callbacks.
     */
    int whisper_bridge_transcribe_with_callback(
            Pointer context,
            float[] samples,
            int sampleCount,
            int tokenTimestamps,
            WhisperSegmentCallback segmentCallback,
            WhisperProgressCallback progressCallback,
            Pointer userData);

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
