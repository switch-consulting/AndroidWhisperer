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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for managing local Whisper model files, extracting models
 * from Android assets, downloading remote models, and listing stored models.
 */
public final class ModelManager {

    public static final String DEFAULT_MODELS_DIR = "whisper_models";

    private ModelManager() {
        // Utility class
    }

    /**
     * Returns the internal storage directory where model files are saved.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
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
     * Checks whether the specified Whisper model is available (present locally, in assets,
     * or downloadable via a download URL).
     */
    public static boolean isModelAvailable(Context context, WhisperModel model) {
        if (context == null || model == null) {
            return false;
        }

        // Check if model file already exists locally
        File targetFile = model.getFile(context);
        if (targetFile != null && targetFile.exists() && targetFile.length() > 0) {
            return true;
        }

        // Otherwise check if it can be opened from assets (if isAsset() is true)
        if (model.isAsset()) {
            try (InputStream ignored = context.getAssets().open(model.getAssetPath())) {
                return true;
            } catch (IOException e) {
                // Asset not accessible; fall through to check download URL
            }
        }

        // Otherwise check if a download URL is available
        String downloadUrl = model.getDownloadUrl();
        return downloadUrl != null && !downloadUrl.trim().isEmpty();
    }

    /**
     * Checks whether the specified Whisper model is already downloaded locally on device storage
     * or accessible from app assets.
     *
     * @param context the Android context
     * @param model the Whisper model
     * @return true if the model file is available locally or in assets, false otherwise
     */
    public static boolean isModelDownloaded(Context context, WhisperModel model) {
        if (context == null || model == null) {
            return false;
        }

        File targetFile = model.getFile(context);
        if (targetFile != null && targetFile.exists() && targetFile.length() > 0) {
            return true;
        }

        if (model.isAsset()) {
            try (InputStream ignored = context.getAssets().open(model.getAssetPath())) {
                return true;
            } catch (IOException ignored) {
            }
        }

        return false;
    }

    /**
     * Ensures the specified Whisper model is available locally, extracted from assets,
     * or downloaded to internal storage.
     * Throws {@link WhisperModelNotAvailable} if the model is unavailable and cannot be downloaded.
     */
    public static File ensureModel(Context context, WhisperModel model) throws IOException {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        if (model == null) {
            throw new IllegalArgumentException("model == null");
        }

        File targetFile = model.getFile(context);
        if (targetFile != null && targetFile.exists() && targetFile.length() > 0) {
            return targetFile;
        }

        if (model.isAsset()) {
            try {
                return ensureModelFromAssets(context, model.getAssetPath());
            } catch (IOException e) {
                // If asset extraction fails, try downloading if a download URL exists
                if (model.getDownloadUrl() == null || model.getDownloadUrl().trim().isEmpty()) {
                    throw e;
                }
            }
        }

        String downloadUrl = model.getDownloadUrl();
        if (downloadUrl != null && !downloadUrl.trim().isEmpty()) {
            downloadModelFromUrl(downloadUrl, targetFile, model.getDownloadSize(), null);
            return targetFile;
        }

        throw new WhisperModelNotAvailable(
                "Model not available locally, in assets, or via download URL: " + model.getVariant());
    }

    private static final ExecutorService DOWNLOAD_EXECUTOR =
            Executors.newCachedThreadPool();

    /**
     * Asynchronously ensures the specified Whisper model is available locally,
     * extracted from assets, or downloaded to internal storage.
     *
     * @param context the Android context
     * @param model the Whisper model
     * @param listener callback listener for progress, success, or error
     */
    public static void ensureModelAsync(
            Context context, WhisperModel model, ModelDownloadListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }
        if (context == null) {
            listener.onError(new IllegalArgumentException("context == null"));
            return;
        }
        if (model == null) {
            listener.onError(new IllegalArgumentException("model == null"));
            return;
        }
        DOWNLOAD_EXECUTOR.execute(() -> {
            try {
                File targetFile = model.getFile(context);
                if (targetFile != null && targetFile.exists() && targetFile.length() > 0) {
                    listener.onSuccess(targetFile);
                    return;
                }

                if (model.isAsset()) {
                    try {
                        File extractedFile = ensureModelFromAssets(context, model.getAssetPath());
                        listener.onSuccess(extractedFile);
                        return;
                    } catch (IOException e) {
                        if (model.getDownloadUrl() == null || model.getDownloadUrl().trim().isEmpty()) {
                            throw e;
                        }
                    }
                }

                String downloadUrl = model.getDownloadUrl();
                if (downloadUrl != null && !downloadUrl.trim().isEmpty()) {
                    downloadModelFromUrl(downloadUrl, targetFile, model.getDownloadSize(), listener);
                    listener.onSuccess(targetFile);
                    return;
                }

                throw new WhisperModelNotAvailable(
                        "Model not available locally, in assets, or via download URL: " + model.getVariant());
            } catch (Exception e) {
                listener.onError(e);
            }
        });
    }

    /**
     * Asynchronously downloads the specified Whisper model file to internal storage.
     *
     * @param context the Android context
     * @param model the Whisper model
     * @param listener callback listener for progress, success, or error
     */
    public static void downloadModelAsync(
            Context context, WhisperModel model, ModelDownloadListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }
        if (context == null) {
            listener.onError(new IllegalArgumentException("context == null"));
            return;
        }
        if (model == null) {
            listener.onError(new IllegalArgumentException("model == null"));
            return;
        }
        DOWNLOAD_EXECUTOR.execute(() -> {
            try {
                String downloadUrl = model.getDownloadUrl();
                if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
                    throw new WhisperModelNotAvailable(
                            "Model has no valid download URL: " + model.getVariant());
                }
                File targetFile = model.getFile(context);
                downloadModelFromUrl(downloadUrl, targetFile, model.getDownloadSize(), listener);
                listener.onSuccess(targetFile);
            } catch (Exception e) {
                listener.onError(e);
            }
        });
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
    @SuppressWarnings("ResultOfMethodCallIgnored")
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
     * Downloads a model file from the specified HTTP/HTTPS URL into targetFile.
     */
    public static void downloadModelFromUrl(String urlString, File targetFile) throws IOException {
        downloadModelFromUrl(urlString, targetFile, -1L, null);
    }

    /**
     * Downloads a model file from the specified HTTP/HTTPS URL into targetFile, reporting download progress.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void downloadModelFromUrl(
            String urlString, File targetFile, long expectedSize, ModelDownloadListener listener)
            throws IOException {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new IllegalArgumentException("urlString is empty");
        }
        if (targetFile == null) {
            throw new IllegalArgumentException("targetFile == null");
        }

        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        File tempFile = new File(parent, targetFile.getName() + ".tmp");
        if (tempFile.exists()) {
            tempFile.delete();
        }

        String currentUrl = urlString.trim();
        HttpURLConnection connection = null;
        int redirects = 0;
        final int MAX_REDIRECTS = 5;

        try {
            while (redirects < MAX_REDIRECTS) {
                URL url = new URL(currentUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "AndroidWhisperer");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || responseCode == HttpURLConnection.HTTP_SEE_OTHER
                        || responseCode == 307
                        || responseCode == 308) {
                    String loc = connection.getHeaderField("Location");
                    if (loc == null || loc.trim().isEmpty()) {
                        throw new IOException("HTTP " + responseCode + " redirect with no Location header from " + currentUrl);
                    }
                    currentUrl = loc;
                    redirects++;
                    connection.disconnect();
                    continue;
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Failed to download model from " + urlString + ": HTTP " + responseCode);
                }

                break;
            }

            if (redirects >= MAX_REDIRECTS) {
                throw new IOException("Too many redirects downloading model from " + urlString);
            }

            String contentLengthHeader = connection.getHeaderField("Content-Length");
            long contentLength = -1L;
            if (contentLengthHeader != null) {
                try {
                    contentLength = Long.parseLong(contentLengthHeader.trim());
                } catch (NumberFormatException ignored) {
                }
            }
            long totalBytes = contentLength > 0 ? contentLength : (expectedSize > 0 ? expectedSize : -1L);

            try (InputStream in = connection.getInputStream();
                 OutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long bytesDownloaded = 0;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    bytesDownloaded += bytesRead;
                    if (listener != null) {
                        listener.onProgress(bytesDownloaded, totalBytes);
                    }
                }
                out.flush();
            }

            if (targetFile.exists()) {
                targetFile.delete();
            }
            if (!tempFile.renameTo(targetFile)) {
                try (InputStream in = new FileInputStream(tempFile);
                     OutputStream out = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                }
                tempFile.delete();
            }
        } catch (Exception e) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Error downloading model from " + urlString + ": " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Returns a list of all model files currently stored in internal storage.
     */
    @SuppressWarnings("unused")
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
}
