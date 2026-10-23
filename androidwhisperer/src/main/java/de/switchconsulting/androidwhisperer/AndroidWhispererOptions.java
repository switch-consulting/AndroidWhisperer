package de.switchconsulting.androidwhisperer;

public final class AndroidWhispererOptions {
    private final AndroidWhisperer.PauseMode pauseMode;
    private final long segmentPauseThresholdMs;
    private final long tokenPauseThresholdMs;

    public AndroidWhispererOptions(
            AndroidWhisperer.PauseMode pauseMode,
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

    public static AndroidWhispererOptions defaults() {
        return new AndroidWhispererOptions(AndroidWhisperer.PauseMode.BOTH, 300, 100);
    }

    public AndroidWhisperer.PauseMode getPauseMode() {
        return pauseMode;
    }
    public long getSegmentPauseThresholdMs() {
        return segmentPauseThresholdMs;
    }
    public long getTokenPauseThresholdMs() {
        return tokenPauseThresholdMs;
    }
}
