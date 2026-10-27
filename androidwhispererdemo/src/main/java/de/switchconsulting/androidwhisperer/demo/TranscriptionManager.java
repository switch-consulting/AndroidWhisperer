package de.switchconsulting.androidwhisperer.demo;

import android.content.Context;
import android.util.Log;

import java.io.File;

import de.switchconsulting.androidwhisperer.AndroidWhisperer;
import de.switchconsulting.androidwhisperer.ModelManager;
import de.switchconsulting.androidwhisperer.TranscriptionListener;
import de.switchconsulting.androidwhisperer.WhisperModel;
import de.switchconsulting.androidwhisperer.WhisperModelNotAvailable;
import de.switchconsulting.androidwhisperer.model.SegmentResult;
import de.switchconsulting.androidwhisperer.model.TranscriptionResult;

public class TranscriptionManager {
    private static final String TAG = "TranscriptionManager";

    public interface TranscriptionCallback {
        void onNewSegment(SegmentResult segment, String accumulatedTranscript);
        void onProgress(int progressPercent);
        void onSuccess(TranscriptionResult result);
        void onError(Exception e);
    }

    @SuppressWarnings("unused")
    public static void transcribeAudio(final Context context, final File pcmFile, final TranscriptionCallback callback) {
        transcribeAudio(context, WhisperModel.TINY, pcmFile, callback);
    }

    public static void transcribeAudio(final Context context, final WhisperModel model, final File pcmFile, final TranscriptionCallback callback) {
        new Thread(() -> {
            WhisperModel selectedModel = (model != null) ? model : WhisperModel.TINY;
            if (!ModelManager.isModelAvailable(context, selectedModel)) {
                if (callback != null) {
                    callback.onError(new WhisperModelNotAvailable("Whisper model '" + selectedModel.getVariant() + "' is not available."));
                }
                return;
            }

            try (AndroidWhisperer whisperer = new AndroidWhisperer()) {
                whisperer.addListener(new TranscriptionListener() {
                    @Override
                    public void onNewSegment(SegmentResult segment, String accumulatedTranscript) {
                        if (callback != null) {
                            callback.onNewSegment(segment, accumulatedTranscript);
                        }
                    }

                    @Override
                    public void onProgress(int progressPercent) {
                        if (callback != null) {
                            callback.onProgress(progressPercent);
                        }
                    }
                });
                whisperer.init(context, selectedModel);

                float[] samples = AudioRecorder.convertPcmToFloat(pcmFile);
                TranscriptionResult result = whisperer.transcribe(samples);

                if (callback != null) {
                    callback.onSuccess(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Transcription failed", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        }, "Transcription Worker Thread").start();
    }
}
