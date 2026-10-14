package de.switchconsulting.androidwhisperer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete transcription and pause-analysis result. */
public final class TranscriptionResult {

    private final String text;
    private final String language;
    private final List<SegmentResult> segments;
    private final List<PauseSegment> pauses;

    public TranscriptionResult(
            String text,
            String language,
            List<SegmentResult> segments,
            List<PauseSegment> pauses) {
        this.text = text == null ? "" : text;
        this.language = language == null ? "" : language;
        this.segments = Collections.unmodifiableList(
                new ArrayList<>(segments == null
                        ? Collections.emptyList() : segments));
        this.pauses = Collections.unmodifiableList(
                new ArrayList<>(pauses == null
                        ? Collections.emptyList() : pauses));
    }

    public String getText() { return text; }
    public String getLanguage() { return language; }
    public List<SegmentResult> getSegments() { return segments; }
    public List<PauseSegment> getPauses() { return pauses; }
}