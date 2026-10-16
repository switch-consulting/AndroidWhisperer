package de.switchconsulting.androidwhisperer;

/**
 * Enumeration of available Whisper models.
 */
public enum WhisperModel {
    TINY("tiny", "ggml-tiny.bin", "models/ggml-tiny.bin", true),
    TINY_EN("tiny.en", "ggml-tiny.en.bin", "models/ggml-tiny.en.bin", false),
    BASE("base", "ggml-base.bin", "models/ggml-base.bin", false),
    BASE_EN("base.en", "ggml-base.en.bin", "models/ggml-base.en.bin", false),
    SMALL("small", "ggml-small.bin", "models/ggml-small.bin", false),
    SMALL_EN("small.en", "ggml-small.en.bin", "models/ggml-small.en.bin", false),
    MEDIUM("medium", "ggml-medium.bin", "models/ggml-medium.bin", false),
    MEDIUM_EN("medium.en", "ggml-medium.en.bin", "models/ggml-medium.en.bin", false),
    LARGE("large", "ggml-large.bin", "models/ggml-large.bin", false);

    private final String variant;
    private final String fileName;
    private final String assetPath;
    private final boolean supported;

    WhisperModel(String variant, String fileName, String assetPath, boolean supported) {
        this.variant = variant;
        this.fileName = fileName;
        this.assetPath = assetPath;
        this.supported = supported;
    }

    public String getVariant() {
        return variant;
    }

    public String getFileName() {
        return fileName;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public boolean isSupported() {
        return supported;
    }
}
