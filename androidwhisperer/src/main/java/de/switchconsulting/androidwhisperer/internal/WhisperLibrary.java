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
 * Internal mapping for the whisper.cpp C API.
 *
 * ABI NOTE:
 * The native build and this mapping must use matching whisper.h versions.
 * C structs passed or returned by value require exact JNA Structure layouts.
 */
public interface WhisperLibrary extends Library {

    WhisperLibrary INSTANCE = Native.load("whisper", WhisperLibrary.class);

    // Lifecycle and diagnostics.
    Pointer whisper_init_from_file(String modelPath);
    void whisper_free(Pointer context);
    String whisper_print_system_info();

    // Language utilities.
    int whisper_lang_auto_detect(
            Pointer context,
            Pointer state,
            int offsetMs,
            int threadCount,
            Pointer languageProbabilities);

    String whisper_lang_str(int languageId);

    // Segment inspection.
    int whisper_full_n_segments(Pointer context);
    long whisper_full_get_segment_t0(Pointer context, int segmentIndex);
    long whisper_full_get_segment_t1(Pointer context, int segmentIndex);
    String whisper_full_get_segment_text(Pointer context, int segmentIndex);

    // Tokens are indexed within a segment, not globally.
    int whisper_full_n_tokens(Pointer context, int segmentIndex);
    long whisper_full_get_token_t0(
            Pointer context, int segmentIndex, int tokenIndex);
    long whisper_full_get_token_t1(
            Pointer context, int segmentIndex, int tokenIndex);
    String whisper_full_get_token_text(
            Pointer context, int segmentIndex, int tokenIndex);
}
