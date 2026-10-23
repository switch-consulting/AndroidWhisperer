package de.switchconsulting.androidwhisperer;

import de.switchconsulting.androidwhisperer.model.SegmentResult;

/**
 * Listener interface for receiving incremental updates during audio transcription.
 */
public interface TranscriptionListener {

    /**
     * Called whenever a new text segment is decoded natively.
     *
     * @param segment the newly decoded segment details
     * @param accumulatedTranscript the full accumulated transcript text up to this segment
     */
    void onNewSegment(SegmentResult segment, String accumulatedTranscript);

    /**
     * Called periodically during transcription with overall progress percentage (0 to 100).
     *
     * @param progressPercent current progress percentage
     */
    void onProgress(int progressPercent);
}
