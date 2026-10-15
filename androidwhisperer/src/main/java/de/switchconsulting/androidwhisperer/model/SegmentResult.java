package de.switchconsulting.androidwhisperer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One Whisper speech segment with optional token-level detail. */
public final class SegmentResult {

    private final String text;
    private final long startMs;
    private final long endMs;
    private final List<PauseSegment> pauses;

    public SegmentResult(
            String text, long startMs, long endMs,
            List<PauseSegment> pauses) {
        if (startMs < 0 || endMs < startMs) {
            throw new IllegalArgumentException("Invalid segment interval");
        }
        this.text = text == null ? "" : text;
        this.startMs = startMs;
        this.endMs = endMs;
        this.pauses = Collections.unmodifiableList(
                new ArrayList<>(pauses == null
                        ? Collections.emptyList() : pauses));
    }

    public String getText() { return text; }
    public long getStartMs() { return startMs; }
    public long getEndMs() { return endMs; }
    public List<PauseSegment> getPauses() { return pauses; }
}