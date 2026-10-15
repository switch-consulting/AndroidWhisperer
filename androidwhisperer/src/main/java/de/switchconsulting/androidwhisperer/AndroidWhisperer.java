package de.switchconsulting.androidwhisperer;

import com.sun.jna.Pointer;

import de.switchconsulting.androidwhisperer.internal.WhisperBridge;
import de.switchconsulting.androidwhisperer.model.PauseSegment;
import de.switchconsulting.androidwhisperer.model.SegmentResult;
import de.switchconsulting.androidwhisperer.model.TranscriptionResult;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Public Java facade for local whisper.cpp transcription.
 *
 * Native calls on a single engine are serialized. Call expensive operations
 * from a worker thread, not Android's main thread.
 */
public final class AndroidWhisperer implements Closeable {

    public enum PauseMode {
        NONE,
        SEGMENT,
        TOKEN,
        BOTH
    }

    public static final class Options {
        private final PauseMode pauseMode;
        private final long segmentPauseThresholdMs;
        private final long tokenPauseThresholdMs;

        public Options(
                PauseMode pauseMode,
                long segmentPauseThresholdMs,
                long tokenPauseThresholdMs) {
            if (pauseMode == null
                    || segmentPauseThresholdMs < 0
                    || tokenPauseThresholdMs < 0) {
                throw new IllegalArgumentException("Invalid options");
            }
            this.pauseMode = pauseMode;
            this.segmentPauseThresholdMs = segmentPauseThresholdMs;
            this.tokenPauseThresholdMs = tokenPauseThresholdMs;
        }

        public static Options defaults() {
            return new Options(PauseMode.BOTH, 300, 100);
        }
    }

    private final WhisperBridge nativeApi;
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

    /** Initializes or replaces the current model context. */
    public synchronized void initModel(String modelPath) {
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
        return transcribe(samples, Options.defaults());
    }

    /**
     * Runs transcription and extracts segment- and/or token-level pauses.
     * Samples must already be mono float PCM at 16 kHz.
     */
    public synchronized TranscriptionResult transcribe(
            float[] samples, Options options) {
        requireModel();
        validateSamples(samples);

        if (options == null) {
            throw new IllegalArgumentException("options == null");
        }

        boolean tokenMode = options.pauseMode == PauseMode.TOKEN
                || options.pauseMode == PauseMode.BOTH;

        int status = nativeApi.whisper_bridge_transcribe(
                context, samples, samples.length, tokenMode ? 1 : 0);

        if (status != 0) {
            throw new IllegalStateException(
                    "Whisper inference failed with code " + status);
        }

        int count = nativeApi.whisper_full_n_segments(context);
        if (count < 0) {
            throw new IllegalStateException("Invalid segment count");
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
                throw new IllegalStateException(
                        "Invalid timestamps in segment " + i);
            }

            // Segment gaps represent larger inter-segment pauses.
            if ((options.pauseMode == PauseMode.SEGMENT
                    || options.pauseMode == PauseMode.BOTH)
                    && previousSegmentEnd >= 0 && start >= previousSegmentEnd) {
                addPause(allPauses, previousSegmentEnd, start,
                        options.segmentPauseThresholdMs,
                        PauseSegment.Level.SEGMENT);
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
                                options.tokenPauseThresholdMs,
                                PauseSegment.Level.TOKEN);
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

            if (transcript.length() > 0 && !text.isEmpty()) {
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
    }

    /**
     * Whisper timestamps use centiseconds: one tick equals 10 milliseconds.
     */
    private static long toMillis(long timestamp) {
        if (timestamp < 0 || timestamp > Long.MAX_VALUE / 10) {
            throw new IllegalStateException(
                    "Invalid native timestamp: " + timestamp);
        }
        return timestamp * 10L;
    }

    private static void addPause(
            List<PauseSegment> target,
            long start,
            long end,
            long thresholdMs,
            PauseSegment.Level level) {
        PauseSegment pause = makePause(start, end, thresholdMs, level);
        if (pause != null) target.add(pause);
    }

    private static PauseSegment makePause(
            long start, long end, long thresholdMs,
            PauseSegment.Level level) {
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