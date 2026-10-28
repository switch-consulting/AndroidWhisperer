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

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.switchconsulting.androidwhisperer.ModelDownloadListener;
import de.switchconsulting.androidwhisperer.ModelManager;
import de.switchconsulting.androidwhisperer.WhisperModel;
import de.switchconsulting.androidwhisperer.model.PauseSegment;
import de.switchconsulting.androidwhisperer.model.SegmentResult;
import de.switchconsulting.androidwhisperer.model.TranscriptionResult;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private static final WhisperModel[] AVAILABLE_MODELS = new WhisperModel[]{
            WhisperModel.TINY,
            WhisperModel.TINY_EN,
            WhisperModel.BASE,
            WhisperModel.BASE_EN,
            WhisperModel.SMALL,
            WhisperModel.SMALL_EN,
            WhisperModel.MEDIUM,
            WhisperModel.MEDIUM_EN,
            WhisperModel.LARGE
    };

    private Spinner spinnerModel;
    private Button btnDownloadModel;
    private ProgressBar pbDownload;
    private TextView tvDownloadProgress;
    private Button btnRecord;
    private TextView tvStatus;
    private TextView tvResults;

    private WhisperModel selectedModel = WhisperModel.TINY;
    private AudioRecorder audioRecorder;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinnerModel = findViewById(R.id.spinnerModel);
        btnDownloadModel = findViewById(R.id.btnDownloadModel);
        pbDownload = findViewById(R.id.pbDownload);
        tvDownloadProgress = findViewById(R.id.tvDownloadProgress);
        btnRecord = findViewById(R.id.btnRecord);
        tvStatus = findViewById(R.id.tvStatus);
        tvResults = findViewById(R.id.tvResults);

        audioRecorder = new AudioRecorder();

        setupSpinner();

        btnDownloadModel.setOnClickListener(v -> downloadSelectedModel());

        btnRecord.setOnClickListener(v -> {
            if (!isRecording) {
                checkPermissionAndStartRecording();
            } else {
                stopRecordingAndTranscribe();
            }
        });
    }

    private void setupSpinner() {
        List<String> modelDisplayNames = new ArrayList<>();
        int defaultPosition = 0;

        for (int i = 0; i < AVAILABLE_MODELS.length; i++) {
            WhisperModel model = AVAILABLE_MODELS[i];
            long size = model.getDownloadSize();
            if (size > 0) {
                long sizeMb = size / (1024 * 1024);
                modelDisplayNames.add(model.getVariant() + " (" + sizeMb + " MB)");
            } else {
                modelDisplayNames.add(model.getVariant());
            }

            if (WhisperModel.TINY.equals(model)) {
                defaultPosition = i;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                modelDisplayNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(adapter);

        // Tiny should be default
        spinnerModel.setSelection(defaultPosition);

        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedModel = AVAILABLE_MODELS[position];
                checkAndShowModelStatus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void checkAndShowModelStatus() {
        if (selectedModel == null) return;

        boolean isDownloaded = ModelManager.isModelDownloaded(this, selectedModel);
        if (isDownloaded) {
            btnDownloadModel.setVisibility(View.GONE);
            pbDownload.setVisibility(View.GONE);
            tvDownloadProgress.setVisibility(View.GONE);
            btnRecord.setEnabled(true);
            tvStatus.setText(R.string.status_ready);
        } else {
            btnDownloadModel.setVisibility(View.VISIBLE);
            btnDownloadModel.setEnabled(true);
            btnDownloadModel.setText(getString(R.string.btn_download_model_variant, selectedModel.getVariant()));
            pbDownload.setVisibility(View.GONE);
            tvDownloadProgress.setVisibility(View.GONE);
            btnRecord.setEnabled(false);
            tvStatus.setText(getString(R.string.status_model_not_downloaded, selectedModel.getVariant()));
        }
    }

    private void downloadSelectedModel() {
        if (selectedModel == null) return;

        btnDownloadModel.setEnabled(false);
        spinnerModel.setEnabled(false);
        btnRecord.setEnabled(false);

        pbDownload.setVisibility(View.VISIBLE);
        pbDownload.setIndeterminate(false);
        pbDownload.setProgress(0);

        tvDownloadProgress.setVisibility(View.VISIBLE);
        tvDownloadProgress.setText(R.string.download_starting);
        tvStatus.setText(getString(R.string.status_downloading_model, selectedModel.getVariant()));

        ModelManager.ensureModelAsync(this, selectedModel, new ModelDownloadListener() {
            @Override
            public void onProgress(long bytesDownloaded, long totalBytes) {
                runOnUiThread(() -> {
                    if (totalBytes > 0) {
                        int progress = (int) ((bytesDownloaded * 100) / totalBytes);
                        pbDownload.setIndeterminate(false);
                        pbDownload.setProgress(progress);
                        String downloadedMb = String.format(Locale.US, "%.1f MB", bytesDownloaded / (1024.0 * 1024.0));
                        String totalMb = String.format(Locale.US, "%.1f MB", totalBytes / (1024.0 * 1024.0));
                        tvDownloadProgress.setText(getString(R.string.download_progress_known, progress, downloadedMb, totalMb));
                    } else {
                        pbDownload.setIndeterminate(true);
                        String downloadedMb = String.format(Locale.US, "%.1f MB", bytesDownloaded / (1024.0 * 1024.0));
                        tvDownloadProgress.setText(getString(R.string.download_progress_unknown, downloadedMb));
                    }
                });
            }

            @Override
            public void onSuccess(File modelFile) {
                runOnUiThread(() -> {
                    spinnerModel.setEnabled(true);
                    Toast.makeText(MainActivity.this, getString(R.string.download_model_ready, selectedModel.getVariant()), Toast.LENGTH_SHORT).show();
                    checkAndShowModelStatus();
                });
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    spinnerModel.setEnabled(true);
                    btnDownloadModel.setEnabled(true);
                    pbDownload.setVisibility(View.GONE);
                    String errorMsg = error != null ? error.getMessage() : "";
                    tvDownloadProgress.setText(getString(R.string.download_failed, errorMsg));
                    tvStatus.setText(R.string.status_download_error);
                    Toast.makeText(MainActivity.this, getString(R.string.download_model_failed, errorMsg), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void checkPermissionAndStartRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION
            );
        } else {
            startRecording();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, R.string.permission_mic_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startRecording() {
        try {
            audioRecorder.startRecording(getCacheDir());
            isRecording = true;
            spinnerModel.setEnabled(false);
            btnDownloadModel.setEnabled(false);
            btnRecord.setText(R.string.stop_recording);
            tvStatus.setText(R.string.status_recording);
            tvResults.setText(R.string.recording_audio_speak_now);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.recording_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecordingAndTranscribe() {
        final File pcmFile = audioRecorder.stopRecording();
        isRecording = false;
        btnRecord.setText(R.string.start_recording);
        tvStatus.setText(R.string.status_transcribing);
        tvResults.setText(getString(R.string.transcribing_audio_with_model, selectedModel.getVariant()));

        if (pcmFile == null || !pcmFile.exists() || pcmFile.length() == 0) {
            spinnerModel.setEnabled(true);
            tvStatus.setText(R.string.status_ready);
            tvResults.setText(R.string.error_no_audio_recorded);
            return;
        }

        TranscriptionManager.transcribeAudio(this, selectedModel, pcmFile, new TranscriptionCallback() {
            @Override
            public void onNewSegment(final SegmentResult segment, final String accumulatedTranscript) {
                runOnUiThread(() -> tvResults.setText(getString(R.string.live_transcription_header, selectedModel.getVariant(), accumulatedTranscript)));
            }

            @Override
            public void onProgress(final int progressPercent) {
                runOnUiThread(() -> tvStatus.setText(getString(R.string.status_transcribing_percent, progressPercent)));
            }

            @Override
            public void onSuccess(final TranscriptionResult result) {
                runOnUiThread(() -> {
                    spinnerModel.setEnabled(true);
                    tvStatus.setText(R.string.status_ready);
                    StringBuilder sb = new StringBuilder();
                    sb.append("--- Transcription Text (").append(selectedModel.getVariant()).append(") ---\n");
                    sb.append(result.getText()).append("\n\n");

                    sb.append("--- Segments ---\n");
                    List<SegmentResult> segments = result.getSegments();
                    if (segments != null) {
                        for (SegmentResult seg : segments) {
                            sb.append("[").append(seg.getStartMs()).append("ms - ").append(seg.getEndMs()).append("ms]: ")
                                    .append(seg.getText()).append("\n");
                        }
                    }

                    sb.append("\n--- Pause Segments ---\n");
                    List<PauseSegment> pauses = result.getPauses();
                    if (pauses != null && !pauses.isEmpty()) {
                        for (PauseSegment pause : pauses) {
                            sb.append("Pause: ").append(pause.getDurationMs()).append("ms at ")
                                    .append(pause.getStartMs()).append("ms\n");
                        }
                    } else {
                        sb.append("No significant pauses detected.\n");
                    }

                    tvResults.setText(sb.toString());
                });
            }

            @Override
            public void onError(final Exception e) {
                runOnUiThread(() -> {
                    spinnerModel.setEnabled(true);
                    tvStatus.setText(R.string.status_ready);
                    String errorMsg = e != null ? e.getMessage() : "";
                    tvResults.setText(getString(R.string.error_during_transcription, errorMsg));
                    Toast.makeText(MainActivity.this, getString(R.string.transcription_error_toast, errorMsg), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
