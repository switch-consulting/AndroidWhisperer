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

import android.content.Context;

import com.sun.jna.Pointer;

import de.switchconsulting.androidwhisperer.internal.WhisperBridge;
import de.switchconsulting.androidwhisperer.internal.WhisperProgressCallback;
import de.switchconsulting.androidwhisperer.internal.WhisperSegmentCallback;
import de.switchconsulting.androidwhisperer.model.PauseLevel;
import de.switchconsulting.androidwhisperer.model.PauseSegment;
import de.switchconsulting.androidwhisperer.model.SegmentResult;
import de.switchconsulting.androidwhisperer.model.TranscriptionResult;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Public Java facade for local whisper.cpp transcription.
 *
 * Native calls on a single engine are serialized. Call expensive operations
 * from a worker thread, not Android's main thread.
 */
public final class AndroidWhisperer implements Closeable {

    private final WhisperBridge nativeApi;
    private final List<TranscriptionListener> listeners = new CopyOnWriteArrayList<>();
    private Pointer context;
    private boolean closed;

    public AndroidWhisperer() {
        this(WhisperBridge.INSTANCE);
    }

    /** Constructor useful for testing with an injected bridge implementation. */
    AndroidWhisperer(WhisperBridge nativeApi) {
        if (nativeApi == null) {
            throw new IllegalArgumentException("nativeApi == null");
        }
        this.nativeApi = nativeApi;
    }

    /** Registers a listener for incremental transcription updates. */
    public void addListener(TranscriptionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /** Unregisters a previously added listener. */
    public void removeListener(TranscriptionListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifyListenersError(TranscriptionException e) {
        for (TranscriptionListener l : listeners) {
            try {
                l.onError(e);
            } catch (Exception ignored) {
                // Prevent listener errors from masking the original transcription exception
            }
        }
    }

    /**
     * Initializes the engine with the default model ({@link WhisperModel#TINY}).
     *
     * @param context the Android application context
     * @throws IOException if extracting the model asset fails
     * @throws WhisperModelNotAvailable if the model is not available
     */
    public synchronized void init(Context context) throws IOException {
        init(context, WhisperModel.TINY);
    }

    /**
     * Initializes the engine with a specified {@link WhisperModel}.
     * The default model is used if {@code model} is null.
     *
     * @param context the Android application context
     * @param model the Whisper model enum value
     * @throws IOException if extracting the model asset fails
     * @throws WhisperModelNotAvailable if the model is not available or not supported
     */
    public synchronized void init(Context context, WhisperModel model) throws IOException {
        ensureOpen();
        WhisperModel resolvedModel = (model == null) ? WhisperModel.TINY : model;
        if (!ModelManager.isModelAvailable(context, resolvedModel)) {
            throw new WhisperModelNotAvailable("Model not available: " + resolvedModel);
        }
        File modelFile = ModelManager.ensureModel(context, resolvedModel);
        initModelFromFile(modelFile);
    }

    /**
     * Checks whether the specified Whisper model is available.
     */
    @SuppressWarnings("unused")
    public static boolean isModelAvailable(Context context, WhisperModel model) {
        return ModelManager.isModelAvailable(context, model);
    }

    /**
     * Checks whether the specified Whisper model is downloaded locally on device storage or in assets.
     */
    @SuppressWarnings("unused")
    public static boolean isModelDownloaded(Context context, WhisperModel model) {
        return ModelManager.isModelDownloaded(context, model);
    }

    /** Initializes or replaces the current model context using a file path. */
    private synchronized void initModel(String modelPath) {
        ensureOpen();

        if (modelPath == null || modelPath.trim().isEmpty()) {
            throw new IllegalArgumentException("modelPath is empty");
        }

        Pointer replacement = nativeApi.whisper_init_from_file(modelPath);
        if (replacement == null || Pointer.nativeValue(replacement) == 0) {
            throw new IllegalStateException(
                    "Whisper failed to load model: " + modelPath);
        }

        Pointer previous = context;
        context = replacement;

        // Free only after replacement initialization succeeds.
        if (previous != null) {
            nativeApi.whisper_free(previous);
        }
    }

    /**
     * Initializes the engine with a model extracted from Android assets.
     *
     * @param context the Android application context
     * @param assetPath relative path inside assets, e.g. "models/ggml-tiny.bin"
     * @throws IOException if extracting the asset fails
     */
    @SuppressWarnings("unused")
    public synchronized void initModelFromAsset(Context context, String assetPath)
            throws IOException {
        File modelFile = ModelManager.ensureModelFromAssets(context, assetPath);
        initModelFromFile(modelFile);
    }

    /**
     * Initializes the engine from a local File.
     *
     * @param modelFile the local model file
     */
    public synchronized void initModelFromFile(File modelFile) {
        if (modelFile == null) {
            throw new IllegalArgumentException("modelFile == null");
        }
        initModel(modelFile.getAbsolutePath());
    }

    /** Detects language and returns its code, e.g. "en" or "de". */
    public synchronized String detectLanguage(float[] samples) {
        requireModel();
        validateSamples(samples);

        int id = nativeApi.whisper_bridge_detect_language(
                context, samples, samples.length);

        if (id < 0) {
            throw new IllegalStateException("Language detection failed");
        }

        String language = nativeApi.whisper_lang_str(id);
        if (language == null || language.isEmpty()) {
            throw new IllegalStateException(
                    "Native library returned an invalid language ID");
        }
        return language;
    }

    /** Transcribes audio with the default pause-analysis options. */
    public synchronized TranscriptionResult transcribe(float[] samples) {
        return transcribe(samples, AndroidWhispererOptions.defaults());
    }

    /**
     * Runs transcription and extracts segment- and/or token-level pauses.
     * Samples must already be mono float PCM at 16 kHz.
     */
    public synchronized TranscriptionResult transcribe(
            float[] samples, AndroidWhispererOptions options) {
        requireModel();
        validateSamples(samples);

        if (options == null) {
            throw new IllegalArgumentException("options == null");
        }

        try {
            boolean tokenMode = options.getPauseMode() == PauseMode.TOKEN
                    || options.getPauseMode() == PauseMode.BOTH;

            int status;
            if (listeners.isEmpty()) {
                // Direct fallback path when no listeners are registered
                status = nativeApi.whisper_bridge_transcribe(
                        context, samples, samples.length, tokenMode ? 1 : 0);
            } else {
                // Incremental callback path when listeners are registered
                final StringBuilder liveTranscript = new StringBuilder();
                WhisperSegmentCallback segmentCb = createSegmentCallback(liveTranscript);
                WhisperProgressCallback progressCb = createProgressCallback();

                status = nativeApi.whisper_bridge_transcribe_with_callback(
                        context, samples, samples.length, tokenMode ? 1 : 0,
                        segmentCb, progressCb, null);
            }

            if (status != 0) {
                throw new TranscriptionException(
                        "Whisper inference failed with code " + status);
            }

            int count = nativeApi.whisper_full_n_segments(context);
            if (count < 0) {
                throw new TranscriptionException("Invalid segment count");
            }

            List<SegmentResult> segments = new ArrayList<>();
            List<PauseSegment> allPauses = new ArrayList<>();
            StringBuilder transcript = new StringBuilder();

            long previousSegmentEnd = -1;

            for (int i = 0; i < count; i++) {
                long start = toMillis(
                        nativeApi.whisper_full_get_segment_t0(context, i));
                long end = toMillis(
                        nativeApi.whisper_full_get_segment_t1(context, i));
                String text = safe(nativeApi.whisper_full_get_segment_text(
                        context, i));

                if (end < start) {
                    throw new TranscriptionException(
                            "Invalid timestamps in segment " + i);
                }

                // Segment gaps represent larger inter-segment pauses.
                if ((options.getPauseMode() == PauseMode.SEGMENT
                        || options.getPauseMode() == PauseMode.BOTH)
                        && previousSegmentEnd >= 0 && start >= previousSegmentEnd) {
                    addPause(allPauses, previousSegmentEnd, start,
                            options.getSegmentPauseThresholdMs(),
                            PauseLevel.SEGMENT);
                }

                List<PauseSegment> segmentPauses = new ArrayList<>();

                if (tokenMode) {
                    int tokenCount = nativeApi.whisper_full_n_tokens(context, i);
                    long previousTokenEnd = -1;

                    for (int t = 0; t < tokenCount; t++) {
                        String token = safe(
                                nativeApi.whisper_full_get_token_text(
                                        context, i, t));

                        // Do not let special control tokens create false pauses.
                        if (isSpecialToken(token)) {
                            continue;
                        }

                        long tokenStart = toMillis(
                                nativeApi.whisper_full_get_token_t0(context, i, t));
                        long tokenEnd = toMillis(
                                nativeApi.whisper_full_get_token_t1(context, i, t));

                        if (tokenEnd < tokenStart) {
                            continue;
                        }

                        if (previousTokenEnd >= 0
                                && tokenStart >= previousTokenEnd) {
                            PauseSegment pause = makePause(
                                    previousTokenEnd, tokenStart,
                                    options.getTokenPauseThresholdMs(),
                                    PauseLevel.TOKEN);
                            if (pause != null) {
                                segmentPauses.add(pause);
                                allPauses.add(pause);
                            }
                        }

                        previousTokenEnd = Math.max(previousTokenEnd, tokenEnd);
                    }
                }

                segments.add(new SegmentResult(
                        text, start, end, segmentPauses));

                if (transcript.length() > 0) {
                    transcript.append(' ');
                }
                transcript.append(text);

                previousSegmentEnd = Math.max(previousSegmentEnd, end);
            }

            String language = "";
            int langId = nativeApi.whisper_bridge_detect_language(
                    context, samples, samples.length);
            if (langId >= 0) {
                String detected = nativeApi.whisper_lang_str(langId);
                if (detected != null) language = detected;
            }

            return new TranscriptionResult(
                    transcript.toString().trim(), language, segments, allPauses);
        } catch (TranscriptionException e) {
            notifyListenersError(e);
            throw e;
        } catch (Exception e) {
            TranscriptionException te = new TranscriptionException(
                    e.getMessage() != null ? e.getMessage() : "Transcription failed", e);
            notifyListenersError(te);
            throw te;
        }
    }

    private WhisperSegmentCallback createSegmentCallback(StringBuilder liveTranscript) {
        return (segmentIndex, text, t0Ms, t1Ms, userData) -> {
            String segmentText = safe(text);
            SegmentResult segment = new SegmentResult(
                    segmentText, t0Ms, t1Ms, Collections.emptyList());

            synchronized (liveTranscript) {
                if (liveTranscript.length() > 0) {
                    liveTranscript.append(' ');
                }
                liveTranscript.append(segmentText);
            }

            String currentText = liveTranscript.toString();
            for (TranscriptionListener l : listeners) {
                l.onNewSegment(segment, currentText);
            }
        };
    }

    private WhisperProgressCallback createProgressCallback() {
        return (progressPercent, userData) -> {
            for (TranscriptionListener l : listeners) {
                l.onProgress(progressPercent);
            }
        };
    }

    /**
     * Whisper timestamps use centiseconds: one tick equals 10 milliseconds.
     */
    private static long toMillis(long timestamp) {
        if (timestamp < 0 || timestamp > Long.MAX_VALUE / 10) {
            throw new TranscriptionException(
                    "Invalid native timestamp: " + timestamp);
        }
        return timestamp * 10L;
    }

    @SuppressWarnings("SameParameterValue")
    private static void addPause(
            List<PauseSegment> target,
            long start,
            long end,
            long thresholdMs,
            PauseLevel level) {
        PauseSegment pause = makePause(start, end, thresholdMs, level);
        if (pause != null) target.add(pause);
    }

    private static PauseSegment makePause(
            long start, long end, long thresholdMs,
            PauseLevel level) {
        if (start < 0 || end < start) return null;
        if (end - start < thresholdMs || end == start) return null;
        return new PauseSegment(start, end, level);
    }

    /**
     * Heuristic filter for visible control-token spellings.
     * A native token-ID based filter is preferable when the bridge exposes IDs.
     */
    private static boolean isSpecialToken(String token) {
        String value = token.trim();
        return value.startsWith("<|") && value.endsWith("|>");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void validateSamples(float[] samples) {
        if (samples == null) {
            throw new IllegalArgumentException("samples == null");
        }
        for (float sample : samples) {
            if (!Float.isFinite(sample) || sample < -1f || sample > 1f) {
                throw new IllegalArgumentException(
                        "Samples must be finite and normalized to [-1, 1]");
            }
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Engine is closed");
    }

    private void requireModel() {
        ensureOpen();
        if (context == null) {
            throw new IllegalStateException("Call initModel() first");
        }
    }

    /** Releases the native context and makes this engine unusable. */
    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;

        if (context != null) {
            nativeApi.whisper_free(context);
            context = null;
        }
    }

    /** Alias for close(), for callers that prefer explicit lifecycle naming. */
    public void release() {
        close();
    }
}
