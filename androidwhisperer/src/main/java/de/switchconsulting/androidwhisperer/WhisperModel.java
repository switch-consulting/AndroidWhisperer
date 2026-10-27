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

package de.switchconsulting.androidwhisperer;

import android.content.Context;

import java.io.File;
import java.net.URI;
import java.net.URL;

/**
 * Represents a Whisper model configuration.
 * Can be used with predefined standard models (e.g. {@link #TINY}, {@link #BASE})
 * or instantiated for custom user models.
 */
public class WhisperModel {

    public static final String HUGGINGFACE_BASE_URL =
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-";

    private static URL createUrl(String variant) {
        try {
            return URI.create(HUGGINGFACE_BASE_URL + variant + ".bin").toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL for variant: " + variant, e);
        }
    }

    // Predefined standard models
    public static final WhisperModel TINY = new WhisperModel("tiny", "ggml-tiny.bin", "models/ggml-tiny.bin", createUrl("tiny"), 77691713L);
    public static final WhisperModel TINY_EN = new WhisperModel("tiny.en", "ggml-tiny.en.bin", null, createUrl("tiny.en"), 77704715L);
    public static final WhisperModel BASE = new WhisperModel("base", "ggml-base.bin", null, createUrl("base"), 147951465L);
    public static final WhisperModel BASE_EN = new WhisperModel("base.en", "ggml-base.en.bin", null, createUrl("base.en"), 147964211L);
    public static final WhisperModel SMALL = new WhisperModel("small", "ggml-small.bin", null, createUrl("small"), 487601967L);
    public static final WhisperModel SMALL_EN = new WhisperModel("small.en", "ggml-small.en.bin", null, createUrl("small.en"), 487614201L);
    public static final WhisperModel MEDIUM = new WhisperModel("medium", "ggml-medium.bin", null, createUrl("medium"), 1533763059L);
    public static final WhisperModel MEDIUM_EN = new WhisperModel("medium.en", "ggml-medium.en.bin", null, createUrl("medium.en"), 1533774781L);
    public static final WhisperModel LARGE = new WhisperModel("large", "ggml-large.bin", null, createUrl("large"), 3094623691L);

    private final String variant;
    private final String fileName;
    private final String assetPath;
    private final String downloadUrl;
    private final long downloadSize;

    /**
     * Constructs a custom Whisper model configuration with an asset path.
     *
     * @param variant model variant name (e.g. "tiny", "custom-model")
     * @param fileName file name on device (e.g. "ggml-custom.bin")
     * @param assetPath relative path inside app assets if bundled, or null
     */
    public WhisperModel(String variant, String fileName, String assetPath) {
        this(variant, fileName, assetPath, null, -1L);
    }

    /**
     * Constructs a custom Whisper model configuration with an explicit download URL.
     *
     * @param variant model variant name
     * @param fileName file name on device
     * @param downloadUrl direct download URL
     */
    public WhisperModel(String variant, String fileName, URL downloadUrl) {
        this(variant, fileName, null, downloadUrl, -1L);
    }

    /**
     * Constructs a custom Whisper model configuration with an explicit download URL and download size.
     *
     * @param variant model variant name
     * @param fileName file name on device
     * @param downloadUrl direct download URL
     * @param downloadSize expected download size in bytes, or -1 if unknown
     */
    public WhisperModel(String variant, String fileName, URL downloadUrl, long downloadSize) {
        this(variant, fileName, null, downloadUrl, downloadSize);
    }

    /**
     * Internal constructor accepting all configuration fields.
     */
    public WhisperModel(String variant, String fileName, String assetPath, URL downloadUrl, long downloadSize) {
        if (variant == null || variant.trim().isEmpty()) {
            throw new IllegalArgumentException("variant is empty");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("fileName is empty");
        }
        this.variant = variant.trim();
        this.fileName = fileName.trim();
        this.assetPath = (assetPath != null && !assetPath.trim().isEmpty()) ? assetPath.trim() : null;
        this.downloadUrl = (downloadUrl != null) ? downloadUrl.toString() : null;
        this.downloadSize = downloadSize > 0 ? downloadSize : -1L;
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
     * Returns the expected download size of the model file in bytes, or -1 if unknown.
     *
     * @return download size in bytes, or -1 if unknown
     */
    public long getDownloadSize() {
        return downloadSize;
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
