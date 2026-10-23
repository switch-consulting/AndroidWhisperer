package de.switchconsulting.androidwhisperer;

import android.content.Context;

import java.io.File;

/**
 * Represents a Whisper model configuration.
 * Can be used with predefined standard models (e.g. {@link #TINY}, {@link #BASE})
 * or instantiated for custom user models.
 */
public class WhisperModel {

    // Predefined standard models
    public static final WhisperModel TINY = new WhisperModel("tiny", "ggml-tiny.bin", "models/ggml-tiny.bin");
    public static final WhisperModel TINY_EN = new WhisperModel("tiny.en", "ggml-tiny.en.bin", null);
    public static final WhisperModel BASE = new WhisperModel("base", "ggml-base.bin", null);
    public static final WhisperModel BASE_EN = new WhisperModel("base.en", "ggml-base.en.bin", null);
    public static final WhisperModel SMALL = new WhisperModel("small", "ggml-small.bin", null);
    public static final WhisperModel SMALL_EN = new WhisperModel("small.en", "ggml-small.en.bin", null);
    public static final WhisperModel MEDIUM = new WhisperModel("medium", "ggml-medium.bin", null);
    public static final WhisperModel MEDIUM_EN = new WhisperModel("medium.en", "ggml-medium.en.bin", null);
    public static final WhisperModel LARGE = new WhisperModel("large", "ggml-large.bin", null);

    private final String variant;
    private final String fileName;
    private final String assetPath;
    private final String downloadUrl;

    /**
     * Constructs a custom Whisper model configuration.
     *
     * @param variant model variant name (e.g. "tiny", "custom-model")
     * @param fileName file name on device (e.g. "ggml-custom.bin")
     * @param assetPath relative path inside app assets if bundled, or null if download-only
     */
    public WhisperModel(String variant, String fileName, String assetPath) {
        this(variant, fileName, assetPath, ModelManager.getDownloadUrl(variant));
    }

    /**
     * Constructs a custom Whisper model configuration with an explicit download URL.
     *
     * @param variant model variant name
     * @param fileName file name on device
     * @param assetPath relative path inside app assets if bundled, or null
     * @param downloadUrl direct download URL if not bundled as an asset
     */
    public WhisperModel(String variant, String fileName, String assetPath, String downloadUrl) {
        if (variant == null || variant.trim().isEmpty()) {
            throw new IllegalArgumentException("variant is empty");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("fileName is empty");
        }
        this.variant = variant.trim();
        this.fileName = fileName.trim();
        this.assetPath = (assetPath != null && !assetPath.trim().isEmpty()) ? assetPath.trim() : null;
        this.downloadUrl = (downloadUrl != null && !downloadUrl.trim().isEmpty())
                ? downloadUrl.trim()
                : ModelManager.getDownloadUrl(this.variant);
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

    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Selector method indicating whether this model should be obtained from an app asset.
     *
     * @return true if an asset path is available, false if it should be obtained via download URL
     */
    public boolean isAsset() {
        return assetPath != null && !assetPath.isEmpty();
    }

    /**
     * Selector method indicating whether this model has an asset path.
     *
     * @return true if an asset path is available, false otherwise
     */
    @SuppressWarnings("unused")
    public boolean hasAsset() {
        return isAsset();
    }

    /**
     * Returns the local {@link File} on the device where the model file is or will be stored.
     */
    public File getFile(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        return new File(ModelManager.getModelsDir(context), fileName);
    }

    /**
     * Alias for {@link #getFile(Context)}, returns the local file on the device.
     */
    public File getLocalFile(Context context) {
        return getFile(context);
    }
}
