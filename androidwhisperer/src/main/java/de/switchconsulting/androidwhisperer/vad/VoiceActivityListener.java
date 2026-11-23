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

/**
 * Callback interface for Voice Activity Detector (VAD) events.
 *
 * @author Dirk Schnelle-Walka
 */
public interface VoiceActivityListener {

    /**
     * Called when speech activity is first detected.
     */
    void onSpeechStarted();

    /**
     * Called when continuous silence exceeds the configured hangover duration.
     */
    void onSpeechEnded();

    /**
     * Called on each processed frame with the calculated Root Mean Square (RMS) energy in decibels (dB FS).
     *
     * @param rmsDb current frame RMS energy level in decibels full scale
     */
    void onRmsChanged(float rmsDb);
}
