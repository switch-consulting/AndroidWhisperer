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

package de.switchconsulting.androidwhisperer.vad;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Contract for Voice Activity Detection (VAD) algorithms.
 * Implementations process audio streams and notify listeners of speech start,
 * speech end, and RMS energy levels.
 *
 * @author Dirk Schnelle-Walka
 */
public interface VoiceActivityDetector {

    /**
     * Processes a frame or buffer of float PCM audio samples normalized in [-1.0, 1.0].
     *
     * @param samples audio float samples
     * @param offset starting index in the array
     * @param length number of samples to process
     */
    void process(@NonNull float[] samples, int offset, int length);

    /**
     * Convenience method to process an entire array of float PCM audio samples.
     *
     * @param samples audio float samples
     */
    default void process(@NonNull float[] samples) {
        process(samples, 0, samples.length);
    }

    /**
     * Processes a buffer of 16-bit signed PCM audio bytes (little-endian).
     *
     * @param pcm16 byte array containing interleaved PCM16 audio data
     * @param offset starting index in bytes
     * @param length number of bytes to process
     */
    void processPcm16(@NonNull byte[] pcm16, int offset, int length);

    /**
     * Convenience method to process an entire array of PCM16 bytes.
     *
     * @param pcm16 byte array containing interleaved PCM16 audio data
     */
    default void processPcm16(@NonNull byte[] pcm16) {
        processPcm16(pcm16, 0, pcm16.length);
    }

    /**
     * Checks if speech is currently detected.
     *
     * @return true if currently in speech state, false otherwise
     */
    boolean isSpeechDetected();

    /**
     * Returns the current VAD state.
     *
     * @return current {@link VadState}
     */
    @NonNull
    VadState getState();

    /**
     * Registers a listener for VAD events.
     *
     * @param listener the listener to receive events, or null to unregister
     */
    void setListener(@Nullable VoiceActivityListener listener);

    /**
     * Resets internal buffers and state tracking to initial idle state.
     */
    void reset();
}
