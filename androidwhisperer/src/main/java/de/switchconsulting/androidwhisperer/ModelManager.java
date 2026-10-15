package de.switchconsulting.androidwhisperer;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for managing local Whisper model files, extracting models
 * from Android assets, listing stored models, and constructing remote download URLs.
 */
public final class ModelManager {

    public static final String DEFAULT_MODELS_DIR = "whisper_models";
    public static final String HUGGINGFACE_BASE_URL =
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-";

    private ModelManager() {
        // Utility class
    }

    /**
     * Returns the internal storage directory where model files are saved.
     */
    public static File getModelsDir(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        File dir = new File(context.getFilesDir(), DEFAULT_MODELS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Ensures an asset model (e.g. "models/ggml-tiny.bin" or "ggml-tiny.bin")
     * is extracted to internal storage. Returns the target {@link File}.
     */
    public static File ensureModelFromAssets(Context context, String assetPath)
            throws IOException {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        if (assetPath == null || assetPath.trim().isEmpty()) {
            throw new IllegalArgumentException("assetPath is empty");
        }

        String fileName = new File(assetPath).getName();
        File targetFile = new File(getModelsDir(context), fileName);

        if (!targetFile.exists() || targetFile.length() == 0) {
            copyAssetToFile(context, assetPath, targetFile);
        }

        return targetFile;
    }

    /**
     * Copies an asset file to a specified target file.
     */
    public static void copyAssetToFile(
            Context context, String assetPath, File targetFile) throws IOException {
        if (context == null || targetFile == null) {
            throw new IllegalArgumentException("Context and targetFile must not be null");
        }

        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (InputStream in = context.getAssets().open(assetPath);
             OutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }

    /**
     * Returns a list of all model files currently stored in internal storage.
     */
    public static List<File> listLocalModels(Context context) {
        File dir = getModelsDir(context);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".bin"));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        List<File> modelList = new ArrayList<>();
        Collections.addAll(modelList, files);
        return modelList;
    }

    /**
     * Constructs the official Hugging Face URL for a given model variant
     * (e.g., "tiny", "tiny.en", "base", "small").
     */
    public static String getDownloadUrl(String modelVariant) {
        if (modelVariant == null || modelVariant.trim().isEmpty()) {
            throw new IllegalArgumentException("modelVariant is empty");
        }
        String variant = modelVariant.trim();
        if (variant.startsWith("ggml-")) {
            variant = variant.substring(5);
        }
        if (variant.endsWith(".bin")) {
            variant = variant.substring(0, variant.length() - 4);
        }
        return HUGGINGFACE_BASE_URL + variant + ".bin";
    }
}
