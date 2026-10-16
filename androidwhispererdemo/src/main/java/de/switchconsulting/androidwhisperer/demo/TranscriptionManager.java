package de.switchconsulting.androidwhisperer.demo;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import de.switchconsulting.androidwhisperer.AndroidWhisperer;
import de.switchconsulting.androidwhisperer.ModelManager;
import de.switchconsulting.androidwhisperer.WhisperModel;
import de.switchconsulting.androidwhisperer.model.TranscriptionResult;

public class TranscriptionManager {
    private static final String TAG = "TranscriptionManager";

    public interface TranscriptionCallback {
        void onSuccess(TranscriptionResult result);
        void onError(Exception e);
    }

    public static void transcribeAudio(final Context context, final File pcmFile, final TranscriptionCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AndroidWhisperer whisperer = null;
                try {
                    if (!ModelManager.isModelAvailable(context, WhisperModel.TINY)) {
                        throw new IOException("Whisper TINY model is not available in assets or local storage. Please add ggml-tiny.bin to assets/models/");
                    }

                    whisperer = new AndroidWhisperer();
                    whisperer.init(context, WhisperModel.TINY);

                    float[] samples = AudioRecorder.convertPcmToFloat(pcmFile);
                    TranscriptionResult result = whisperer.transcribe(samples);

                    callback.onSuccess(result);
                } catch (Exception e) {
                    Log.e(TAG, "Transcription failed", e);
                    callback.onError(e);
                } finally {
                    if (whisperer != null) {
                        try {
                            whisperer.close();
                        } catch (Exception e) {
                            Log.e(TAG, "Error closing whisperer", e);
                        }
                    }
                }
            }
        }, "Transcription Worker Thread").start();
    }
}
