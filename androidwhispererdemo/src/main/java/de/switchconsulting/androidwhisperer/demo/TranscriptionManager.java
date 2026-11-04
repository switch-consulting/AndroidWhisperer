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

package de.switchconsulting.androidwhisperer.demo;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import de.switchconsulting.androidwhisperer.AndroidWhisperer;
import de.switchconsulting.androidwhisperer.AndroidWhispererOptions;
import de.switchconsulting.androidwhisperer.ModelManager;
import de.switchconsulting.androidwhisperer.PauseMode;
import de.switchconsulting.androidwhisperer.TranscriptionException;
import de.switchconsulting.androidwhisperer.TranscriptionListener;
import de.switchconsulting.androidwhisperer.WhisperModel;
import de.switchconsulting.androidwhisperer.WhisperModelNotAvailable;
import de.switchconsulting.androidwhisperer.model.SegmentResult;
import de.switchconsulting.androidwhisperer.model.TranscriptionResult;

public class TranscriptionManager {
    private static final String TAG = "TranscriptionManager";

    @SuppressWarnings("unused")
    public static void transcribeAudio(final Context context, final File pcmFile, final TranscriptionCallback callback) {
        transcribeAudio(context, WhisperModel.TINY, null, pcmFile, callback);
    }

    @SuppressWarnings("unused")
    public static void transcribeAudio(final Context context, final WhisperModel model, final File pcmFile, final TranscriptionCallback callback) {
        transcribeAudio(context, model, null, pcmFile, callback);
    }

    public static void transcribeAudio(final Context context, final WhisperModel model, final Locale locale, final File pcmFile, final TranscriptionCallback callback) {
        WhisperModel selectedModel = (model != null) ? model : WhisperModel.TINY;
        if (!ModelManager.isModelAvailable(context, selectedModel)) {
            if (callback != null) {
                callback.onError(new WhisperModelNotAvailable("Whisper model '" + selectedModel.getVariant() + "' is not available."));
            }
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try (AndroidWhisperer whisperer = new AndroidWhisperer()) {
                if (callback != null) {
                    whisperer.addListener(new TranscriptionListener() {
                        @Override
                        public void onNewSegment(@NonNull SegmentResult segment, @NonNull String accumulatedTranscript) {
                            callback.onNewSegment(segment, accumulatedTranscript);
                        }

                        @Override
                        public void onProgress(int progressPercent) {
                            callback.onProgress(progressPercent);
                        }

                        @Override
                        public void onError(@NonNull TranscriptionException exception) {
                            callback.onError(exception);
                        }

                        @Override
                        public void onLanguageDetected(@NonNull Locale detectedLocale) {
                            callback.onLanguageDetected(detectedLocale);
                        }

                        @Override
                        public void onTranscriptionCompleted(@NonNull TranscriptionResult result) {
                            callback.onTranscriptionCompleted(result);
                        }
                    });
                }
                whisperer.init(context, selectedModel);

                float[] samples = AudioRecorder.convertPcmToFloat(pcmFile);
                AndroidWhispererOptions options = new AndroidWhispererOptions(
                        PauseMode.BOTH, 300, 100, locale);
                return whisperer.transcribe(samples, options);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((result, throwable) -> {
            if (throwable != null) {
                Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                Log.e(TAG, "Transcription failed", cause);
                if (callback != null) {
                    callback.onError(cause instanceof Exception ? (Exception) cause : new Exception(cause));
                }
            } else {
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }
        });
    }
}
