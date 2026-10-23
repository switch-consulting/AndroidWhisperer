package de.switchconsulting.androidwhisperer.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.List;

import de.switchconsulting.androidwhisperer.model.PauseSegment;
import de.switchconsulting.androidwhisperer.model.SegmentResult;
import de.switchconsulting.androidwhisperer.model.TranscriptionResult;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private Button btnRecord;
    private TextView tvStatus;
    private TextView tvResults;

    private AudioRecorder audioRecorder;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRecord = findViewById(R.id.btnRecord);
        tvStatus = findViewById(R.id.tvStatus);
        tvResults = findViewById(R.id.tvResults);

        audioRecorder = new AudioRecorder();

        btnRecord.setOnClickListener(v -> {
            if (!isRecording) {
                checkPermissionAndStartRecording();
            } else {
                stopRecordingAndTranscribe();
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
                Toast.makeText(this, "Microphone permission is required to record audio.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startRecording() {
        try {
            audioRecorder.startRecording(getCacheDir());
            isRecording = true;
            btnRecord.setText(R.string.stop_recording);
            tvStatus.setText(R.string.status_recording);
            tvResults.setText("Recording audio... Speak now.");
        } catch (Exception e) {
            Toast.makeText(this, "Failed to start recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecordingAndTranscribe() {
        final File pcmFile = audioRecorder.stopRecording();
        isRecording = false;
        btnRecord.setText(R.string.start_recording);
        tvStatus.setText(R.string.status_transcribing);
        tvResults.setText("Transcribing recorded audio...");

        if (pcmFile == null || !pcmFile.exists() || pcmFile.length() == 0) {
            tvStatus.setText(R.string.status_ready);
            tvResults.setText("Error: No audio recorded.");
            return;
        }

        TranscriptionManager.transcribeAudio(this, pcmFile, new TranscriptionManager.TranscriptionCallback() {
            @Override
            public void onNewSegment(final SegmentResult segment, final String accumulatedTranscript) {
                runOnUiThread(() -> tvResults.setText("--- Live Transcription ---\n" + accumulatedTranscript));
            }

            @Override
            public void onProgress(final int progressPercent) {
                runOnUiThread(() -> tvStatus.setText("Transcribing... " + progressPercent + "%"));
            }

            @Override
            public void onSuccess(final TranscriptionResult result) {
                runOnUiThread(() -> {
                    tvStatus.setText(R.string.status_ready);
                    StringBuilder sb = new StringBuilder();
                    sb.append("--- Transcription Text ---\n");
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
                    tvStatus.setText(R.string.status_ready);
                    tvResults.setText("Error during transcription:\n" + e.getMessage());
                    Toast.makeText(MainActivity.this, "Transcription error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
