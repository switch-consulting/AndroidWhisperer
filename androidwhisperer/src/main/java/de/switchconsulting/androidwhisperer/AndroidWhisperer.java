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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.sun.jna.Pointer;

import de.switchconsulting.androidwhisperer.audio.AudioUtils;
import de.switchconsulting.androidwhisperer.internal.WhisperBridge;
import de.switchconsulting.androidwhisperer.internal.WhisperProgressCallback;
import de.switchconsulting.androidwhisperer.internal.WhisperSegmentCallback;
import de.switchconsulting.androidwhisperer.model.PauseLevel;
import de.switchconsulting.androidwhisperer.model.PauseSegment;
import de.switchconsulting.androidwhisperer.model.SegmentResult;
import de.switchconsulting.androidwhisperer.model.TokenResult;
import de.switchconsulting.androidwhisperer.model.TranscriptionItem;
import de.switchconsulting.androidwhisperer.model.TranscriptionResult;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * Public Java facade for local whisper.cpp transcription.
 * Native calls on a single engine are serialized. Call expensive operations
 * from a worker thread, not Android's main thread.
 *
 * @author Dirk Schnelle-Walka
 */
public final class AndroidWhisperer implements Closeable {

    private final WhisperBridge nativeApi;
    private final List<TranscriptionListener> listeners = new CopyOnWriteArrayList<>();
    private Pointer context;
    private WhisperModel currentModel;
    private boolean closed;
    private Object cleanable;
    private WhisperCleanerState cleanupState;

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
    public void addListener(@Nullable TranscriptionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /** Unregisters a previously added listener. */
    public void removeListener(@Nullable TranscriptionListener listener) {
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

    private void notifyListenersLanguageDetected(Locale locale) {
        for (TranscriptionListener l : listeners) {
            try {
                l.onLanguageDetected(locale);
            } catch (Exception ignored) {
                // Prevent listener errors from masking transcription execution
            }
        }
    }

    private void notifyListenersTranscriptionCompleted(TranscriptionResult result) {
        for (TranscriptionListener l : listeners) {
            try {
                l.onTranscriptionCompleted(result);
            } catch (Exception ignored) {
                // Prevent listener errors from masking transcription execution
            }
        }
    }

    /**
     * Initializes the engine with the default model ({@link WhisperModel#TINY}).
     *
     * @param context the Android application context
     * @throws IOException if obtaining or downloading the model fails
     * @throws WhisperModelNotAvailable if the model is not available
     */
    @WorkerThread
    @SuppressWarnings("unused")
    public synchronized void init(@NonNull Context context) throws IOException {
        init(context, WhisperModel.TINY);
    }

    /**
     * Initializes the engine with a specified {@link WhisperModel}.
     * The default model is used if {@code model} is null.
     *
     * @param context the Android application context
     * @param model the Whisper model enum value
     * @throws IOException if obtaining or downloading the model fails
     * @throws WhisperModelNotAvailable if the model is not available or not supported
     */
    @WorkerThread
    @SuppressWarnings("unused")
    public synchronized void init(@NonNull Context context, @Nullable WhisperModel model) throws IOException {
        ensureOpen();
        WhisperModel resolvedModel = (model == null) ? WhisperModel.TINY : model;
        if (!ModelManager.isModelAvailable(context, resolvedModel)) {
            throw new WhisperModelNotAvailable("Model not available: " + resolvedModel);
        }
        File modelFile = ModelManager.ensureModel(context, resolvedModel);
        this.currentModel = resolvedModel;
        initModelFromFile(modelFile);
    }

    /**
     * Checks whether the specified Whisper model is available.
     */
    @SuppressWarnings("unused")
    public static boolean isModelAvailable(@Nullable Context context, @Nullable WhisperModel model) {
        return ModelManager.isModelAvailable(context, model);
    }

    /**
     * Checks whether the specified Whisper model is downloaded locally on device storage.
     */
    @SuppressWarnings("unused")
    public static boolean isModelDownloaded(@Nullable Context context, @Nullable WhisperModel model) {
        return ModelManager.isModelDownloaded(context, model);
    }

    /**
     * Checks whether the specified Whisper model file is physically stored on internal device storage.
     */
    @SuppressWarnings("unused")
    public static boolean isModelDownloadedLocally(@Nullable Context context, @Nullable WhisperModel model) {
        return ModelManager.isModelDownloadedLocally(context, model);
    }

    /**
     * Removes the specified Whisper model file from internal device storage if present.
     *
     * @param context the Android context
     * @param model the Whisper model to remove
     * @return true if the model file existed on local storage and was successfully deleted, false otherwise
     */
    @SuppressWarnings("unused")
    public static boolean removeModel(@Nullable Context context, @Nullable WhisperModel model) {
        return ModelManager.removeModel(context, model);
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

        // Clean and free previous model context if any
        if (cleanable != null) {
            WhisperCleaner.clean(cleanable);
            cleanable = null;
        }

        cleanupState = new WhisperCleanerState(nativeApi, replacement);
        context = replacement;
        cleanable = WhisperCleaner.register(this, cleanupState);
    }

    /**
     * Initializes the engine from a local File.
     *
     * @param modelFile the local model file
     */
    public synchronized void initModelFromFile(@NonNull File modelFile) {
        initModel(modelFile.getAbsolutePath());
    }

    /** Detects language and returns its code, e.g. "en" or "de". */
    @NonNull
    public synchronized String detectLanguage(@NonNull float[] samples) {
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
    @NonNull
    public synchronized TranscriptionResult transcribe(@NonNull float[] samples) {
        return transcribe(samples, AndroidWhispererOptions.defaults());
    }

    /**
     * Transcribes audio read from an {@link InputStream} containing 16-bit signed PCM audio bytes
     * (16,000 Hz, single-channel / mono, little-endian) using default options.
     *
     * @param stream the InputStream containing PCM16 audio bytes
     * @return transcription result
     */
    @NonNull
    public synchronized TranscriptionResult transcribe(@NonNull InputStream stream) {
        return transcribe(stream, AndroidWhispererOptions.defaults());
    }

    /**
     * Transcribes audio read from an {@link InputStream} containing 16-bit signed PCM audio bytes
     * (16,000 Hz, single-channel / mono, little-endian) with the specified options.
     *
     * @param stream the InputStream containing PCM16 audio bytes
     * @param options transcription options
     * @return transcription result
     */
    @NonNull
    public synchronized TranscriptionResult transcribe(
            @NonNull InputStream stream, @Nullable AndroidWhispererOptions options) {
        AndroidWhispererOptions resolvedOptions = (options == null)
                ? AndroidWhispererOptions.defaults() : options;

        StreamTranscriptionAccumulator accumulator = new StreamTranscriptionAccumulator();
        byte[] byteBuffer = new byte[8192];
        float[] sampleChunk = new float[4096];
        int leftoverByte = -1;
        int bytesRead;

        try {
            while ((bytesRead = readPcmStream(stream, byteBuffer, leftoverByte)) != -1) {
                PcmChunk chunk = alignPcm16Chunk(byteBuffer, bytesRead, leftoverByte);
                leftoverByte = chunk.leftoverByte();
                if (chunk.totalBytes() <= 0) {
                    continue;
                }

                int samplesWritten = AudioUtils.fromPcm16(byteBuffer, 0, chunk.totalBytes(), sampleChunk, 0);
                if (samplesWritten == 0) {
                    continue;
                }

                float[] chunkSamples = samplesWritten == sampleChunk.length
                        ? sampleChunk
                        : java.util.Arrays.copyOf(sampleChunk, samplesWritten);

                // Call the core whisper transcription method directly on what was read
                TranscriptionResult chunkResult = transcribe(chunkSamples, resolvedOptions);
                accumulator.accumulate(chunkResult, samplesWritten);
            }
        } catch (TranscriptionException e) {
            notifyListenersError(e);
            throw e;
        } catch (Exception e) {
            TranscriptionException te = new TranscriptionException(
                    e.getMessage() != null ? e.getMessage() : "Failed to read audio stream", e);
            notifyListenersError(te);
            throw te;
        }

        return accumulator.buildResult();
    }

    private static int readPcmStream(InputStream stream, byte[] byteBuffer, int leftoverByte) throws IOException {
        int offset = leftoverByte != -1 ? 1 : 0;
        int length = byteBuffer.length - offset;
        return stream.read(byteBuffer, offset, length);
    }

    private static PcmChunk alignPcm16Chunk(byte[] byteBuffer, int bytesRead, int leftoverByte) {
        int totalBytes = bytesRead;
        if (leftoverByte != -1) {
            byteBuffer[0] = (byte) leftoverByte;
            totalBytes += 1;
        }
        int newLeftover = -1;
        if (totalBytes % 2 != 0) {
            newLeftover = byteBuffer[totalBytes - 1] & 0xFF;
            totalBytes--;
        }
        return new PcmChunk(totalBytes, newLeftover);
    }

    static TranscriptionItem offsetTranscriptionItem(TranscriptionItem item, long timeOffsetMs) {
        if (timeOffsetMs == 0) {
            return item;
        }
        if (item instanceof SegmentResult seg) {
            List<TranscriptionItem> offsetSubItems = new ArrayList<>();
            for (TranscriptionItem subItem : seg.getItems()) {
                offsetSubItems.add(offsetTranscriptionItem(subItem, timeOffsetMs));
            }
            return new SegmentResult(
                    seg.getText(),
                    seg.getStartMs() + timeOffsetMs,
                    seg.getEndMs() + timeOffsetMs,
                    offsetSubItems);
        } else if (item instanceof TokenResult token) {
            return new TokenResult(
                    token.getText(),
                    token.getStartMs() + timeOffsetMs,
                    token.getEndMs() + timeOffsetMs);
        } else if (item instanceof PauseSegment pause) {
            return new PauseSegment(
                    pause.getStartMs() + timeOffsetMs,
                    pause.getEndMs() + timeOffsetMs,
                    pause.getLevel());
        }
        return item;
    }

    /**
     * Asynchronously transcribes audio with the default pause-analysis options.
     *
     * @param samples mono float PCM audio samples at 16 kHz
     * @return a CompletableFuture resolving to the transcription result
     */
    @NonNull
    public CompletableFuture<TranscriptionResult> transcribeAsync(@NonNull float[] samples) {
        return transcribeAsync(samples, AndroidWhispererOptions.defaults(), null);
    }

    /**
     * Asynchronously transcribes audio with the default pause-analysis options.
     *
     * @param samples mono float PCM audio samples at 16 kHz
     * @param executor optional custom executor to run the asynchronous task
     * @return a CompletableFuture resolving to the transcription result
     */
    @NonNull
    public CompletableFuture<TranscriptionResult> transcribeAsync(
            @NonNull float[] samples, @Nullable Executor executor) {
        return transcribeAsync(samples, AndroidWhispererOptions.defaults(), executor);
    }

    /**
     * Asynchronously runs transcription and extracts segment- and/or token-level pauses.
     *
     * @param samples mono float PCM audio samples at 16 kHz
     * @param options transcription options
     * @return a CompletableFuture resolving to the transcription result
     */
    @NonNull
    public CompletableFuture<TranscriptionResult> transcribeAsync(
            @NonNull float[] samples, @Nullable AndroidWhispererOptions options) {
        return transcribeAsync(samples, options, null);
    }

    /**
     * Asynchronously runs transcription and extracts segment- and/or token-level pauses.
     *
     * @param samples mono float PCM audio samples at 16 kHz
     * @param options transcription options
     * @param executor optional custom executor to run the asynchronous task
     * @return a CompletableFuture resolving to the transcription result
     */
    @NonNull
    public CompletableFuture<TranscriptionResult> transcribeAsync(
            @NonNull float[] samples, @Nullable AndroidWhispererOptions options, @Nullable Executor executor) {
        Executor exec = executor != null ? executor : WhisperExecutors.getDefaultExecutor();
        return CompletableFuture.supplyAsync(() -> transcribe(samples, options), exec);
    }

    /**
     * Asynchronously transcribes audio read from an {@link InputStream} containing 16-bit signed PCM audio bytes
     * (16,000 Hz, single-channel / mono, little-endian) using default options.
     *
     * @param stream the InputStream containing PCM16 audio bytes
     * @return a CompletableFuture resolving to the transcription result
     */
    @NonNull
    public CompletableFuture<TranscriptionResult> transcribeAsync(@NonNull InputStream stream) {
        return transcribeAsync(stream, AndroidWhispererOptions.defaults(), null);
    }

    /**
     * Asynchronously transcribes audio read from an {@link InputStream} containing 16-bit signed PCM audio bytes
     * (16,000 Hz, single-channel / mono, little-endian) using default options on the specified executor.
     *
     * @param stream the InputStream containing PCM16 audio bytes
     * @param executor optional custom executor to run the asynchronous task
     * @return a CompletableFuture resolving to the transcription result
     */
    @NonNull
    public CompletableFuture<TranscriptionResult> transcribeAsync(
            @NonNull InputStream stream, @Nullable Executor executor) {
        return transcribeAsync(stream, AndroidWhispererOptions.defaults(), executor);
    }

    /**
     * Asynchronously transcribes audio read from an {@link InputStream} containing 16-bit signed PCM audio bytes
     * (16,000 Hz, single-channel / mono, little-endian) with the specified options.
     *
     * @param stream the InputStream containing PCM16 audio bytes
     * @param options transcription options
     * @return a CompletableFuture resolving to the transcription result
     */
    @NonNull
    public CompletableFuture<TranscriptionResult> transcribeAsync(
            @NonNull InputStream stream, @Nullable AndroidWhispererOptions options) {
        return transcribeAsync(stream, options, null);
    }

    /**
     * Asynchronously transcribes audio read from an {@link InputStream} containing 16-bit signed PCM audio bytes
     * (16,000 Hz, single-channel / mono, little-endian) with the specified options.
     *
     * @param stream the InputStream containing PCM16 audio bytes
     * @param options transcription options
     * @param executor optional custom executor to run the asynchronous task
     * @return a CompletableFuture resolving to the transcription result
     */
    @NonNull
    public CompletableFuture<TranscriptionResult> transcribeAsync(
            @NonNull InputStream stream, @Nullable AndroidWhispererOptions options, @Nullable Executor executor) {
        Executor exec = executor != null ? executor : WhisperExecutors.getDefaultExecutor();
        return CompletableFuture.supplyAsync(() -> transcribe(stream, options), exec);
    }

    private boolean isLocaleSupportedByModel(Locale locale) {
        if (locale == null) {
            return false;
        }
        if (currentModel != null) {
            return currentModel.isLocaleSupported(locale);
        }
        int isMultilingual = nativeApi.whisper_is_multilingual(context);
        List<Locale> supported = (isMultilingual == 0)
                ? WhisperModel.ENGLISH_LOCALES
                : WhisperModel.MULTILINGUAL_LOCALES;

        String lang = locale.getLanguage();
        if (lang.isEmpty()) {
            return false;
        }
        for (Locale l : supported) {
            if (l.getLanguage().equalsIgnoreCase(lang)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runs transcription and extracts segment- and/or token-level pauses.
     * Samples must already be mono float PCM at 16 kHz.
     */
    @NonNull
    public synchronized TranscriptionResult transcribe(
            @NonNull float[] samples, @Nullable AndroidWhispererOptions options) {
        requireModel();
        validateSamples(samples);

        AndroidWhispererOptions opts = options != null ? options : AndroidWhispererOptions.defaults();

        try {
            ResolvedLanguage resolvedLang = resolveLanguage(samples, opts);
            boolean tokenMode = opts.getPauseMode() == PauseMode.TOKEN
                    || opts.getPauseMode() == PauseMode.BOTH;

            executeNativeTranscription(samples, tokenMode, resolvedLang.languageParam, opts);

            TranscriptionResult result = buildTranscriptionResult(opts, tokenMode, resolvedLang.resultLocale);
            notifyListenersTranscriptionCompleted(result);
            return result;
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

    private ResolvedLanguage resolveLanguage(float[] samples, AndroidWhispererOptions opts) {
        Locale locale = opts.getLocale();
        if (locale != null) {
            if (!isLocaleSupportedByModel(locale)) {
                throw new LanguageNotSupportedException(locale,
                        "Language not supported by current model: " + locale);
            }
            return new ResolvedLanguage(locale.getLanguage().toLowerCase(Locale.ROOT), locale);
        }

        int langId = nativeApi.whisper_bridge_detect_language(context, samples, samples.length);
        if (langId >= 0) {
            String detected = nativeApi.whisper_lang_str(langId);
            if (detected != null && !detected.isEmpty()) {
                Locale resultLocale = Locale.forLanguageTag(detected);
                notifyListenersLanguageDetected(resultLocale);
                return new ResolvedLanguage(detected, resultLocale);
            }
        }
        return new ResolvedLanguage(null, null);
    }

    private void executeNativeTranscription(
            float[] samples, boolean tokenMode, String languageParam, AndroidWhispererOptions opts) {
        int nThreads = opts.getThreads();
        int strategy = opts.getSamplingStrategy() == SamplingStrategy.BEAM_SEARCH ? 1 : 0;
        int translate = opts.isTranslate() ? 1 : 0;

        int status;
        if (listeners.isEmpty()) {
            status = nativeApi.whisper_bridge_transcribe(
                    context, samples, samples.length, tokenMode ? 1 : 0, languageParam,
                    nThreads, strategy, translate);
        } else {
            final StringBuilder liveTranscript = new StringBuilder();
            WhisperSegmentCallback segmentCb = createSegmentCallback(liveTranscript);
            WhisperProgressCallback progressCb = createProgressCallback();

            status = nativeApi.whisper_bridge_transcribe_with_callback(
                    context, samples, samples.length, tokenMode ? 1 : 0, languageParam,
                    nThreads, strategy, translate,
                    segmentCb, progressCb, null);
        }

        if (status != 0) {
            throw new TranscriptionException("Whisper inference failed with code " + status);
        }
    }

    private TranscriptionResult buildTranscriptionResult(
            AndroidWhispererOptions opts, boolean tokenMode, Locale resultLocale) {
        int count = nativeApi.whisper_full_n_segments(context);
        if (count < 0) {
            throw new TranscriptionException("Invalid segment count");
        }

        List<TranscriptionItem> topLevelItems = new ArrayList<>();
        StringBuilder transcript = new StringBuilder();
        long previousSegmentEnd = -1;

        for (int i = 0; i < count; i++) {
            long start = toMillis(nativeApi.whisper_full_get_segment_t0(context, i));
            long end = toMillis(nativeApi.whisper_full_get_segment_t1(context, i));
            String text = safe(nativeApi.whisper_full_get_segment_text(context, i));

            if (end < start) {
                throw new TranscriptionException("Invalid timestamps in segment " + i);
            }

            if ((opts.getPauseMode() == PauseMode.SEGMENT || opts.getPauseMode() == PauseMode.BOTH)
                    && previousSegmentEnd >= 0 && start >= previousSegmentEnd) {
                PauseSegment pause = makePause(
                        previousSegmentEnd, start,
                        opts.getSegmentPauseThresholdMs(),
                        PauseLevel.SEGMENT);
                if (pause != null) {
                    topLevelItems.add(pause);
                }
            }

            List<TranscriptionItem> segmentItems = tokenMode ? parseTokenItems(i, opts) : Collections.emptyList();

            SegmentResult segmentResult = new SegmentResult(text, start, end, segmentItems);
            topLevelItems.add(segmentResult);

            if (transcript.length() > 0) {
                transcript.append(' ');
            }
            transcript.append(text);

            previousSegmentEnd = Math.max(previousSegmentEnd, end);
        }

        Locale translatedLocale = opts.isTranslate() ? Locale.ENGLISH : null;
        return new TranscriptionResult(
                transcript.toString().trim(), resultLocale, translatedLocale, topLevelItems);
    }

    private List<TranscriptionItem> parseTokenItems(int segmentIndex, AndroidWhispererOptions opts) {
        List<TranscriptionItem> segmentItems = new ArrayList<>();
        int tokenCount = nativeApi.whisper_full_n_tokens(context, segmentIndex);
        long previousTokenEnd = -1;

        for (int t = 0; t < tokenCount; t++) {
            String token = safe(nativeApi.whisper_full_get_token_text(context, segmentIndex, t));

            if (isSpecialToken(token)) {
                continue;
            }

            long tokenStart = toMillis(nativeApi.whisper_full_get_token_t0(context, segmentIndex, t));
            long tokenEnd = toMillis(nativeApi.whisper_full_get_token_t1(context, segmentIndex, t));

            if (tokenEnd < tokenStart) {
                continue;
            }

            if (previousTokenEnd >= 0 && tokenStart >= previousTokenEnd) {
                PauseSegment pause = makePause(
                        previousTokenEnd, tokenStart,
                        opts.getTokenPauseThresholdMs(),
                        PauseLevel.TOKEN);
                if (pause != null) {
                    segmentItems.add(pause);
                }
            }

            segmentItems.add(new TokenResult(token, tokenStart, tokenEnd));
            previousTokenEnd = Math.max(previousTokenEnd, tokenEnd);
        }
        return segmentItems;
    }

    private record ResolvedLanguage(String languageParam, Locale resultLocale) {}

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
    @SuppressWarnings("unused")
    public synchronized void close() {
        if (closed) return;
        closed = true;

        if (cleanable != null) {
            WhisperCleaner.clean(cleanable);
            cleanable = null;
        }

        cleanupState = null;
        context = null;
    }

    /** Alias for close(), for callers that prefer explicit lifecycle naming. */
    @SuppressWarnings("unused")
    public void release() {
        close();
    }
}
