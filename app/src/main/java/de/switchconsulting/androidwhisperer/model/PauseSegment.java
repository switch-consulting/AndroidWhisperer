package de.switchconsulting.androidwhisperer.model;

/** A detected silent interval between two speech boundaries. */
public final class PauseSegment {

    public enum Level {
        SEGMENT,
        TOKEN
    }

    private final long startMs;
    private final long endMs;
    private final long durationMs;
    private final Level level;

    public PauseSegment(long startMs, long endMs, Level level) {
        if (startMs < 0 || endMs < startMs) {
            throw new IllegalArgumentException("Invalid pause interval");
        }
        if (level == null) {
            throw new IllegalArgumentException("level == null");
        }

        this.startMs = startMs;
        this.endMs = endMs;
        this.durationMs = endMs - startMs;
        this.level = level;
    }

    public long getStartMs() { return startMs; }
    public long getEndMs() { return endMs; }
    public long getDurationMs() { return durationMs; }
    public Level getLevel() { return level; }
}