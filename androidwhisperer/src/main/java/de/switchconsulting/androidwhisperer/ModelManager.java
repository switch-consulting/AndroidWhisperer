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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import java.util.concurrent.Executor;

/**
 * Utility class for managing local Whisper model files, downloading remote models,
 * and listing stored models.
 */
public final class ModelManager {

    public static final String DEFAULT_MODELS_DIR = "whisper_models";

    private ModelManager() {
        // Utility class
    }

    /**
     * Returns the internal storage directory where model files are saved.
     */
    @NonNull
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File getModelsDir(@NonNull Context context) {
        File dir = new File(context.getFilesDir(), DEFAULT_MODELS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Checks whether the specified Whisper model is available (present locally
     * or downloadable via a download URL).
     */
    public static boolean isModelAvailable(@Nullable Context context, @Nullable WhisperModel model) {
        if (context == null || model == null) {
            return false;
        }

        // Check if model file already exists locally
        File targetFile = model.getFile(context);
        if (targetFile.exists() && targetFile.length() > 0) {
            return true;
        }

        // Otherwise check if a download URL is available
        String downloadUrl = model.getDownloadUrl();
        return downloadUrl != null && !downloadUrl.trim().isEmpty();
    }

    /**
     * Checks whether the specified Whisper model is already downloaded locally on device storage.
     *
     * @param context the Android context
     * @param model the Whisper model
     * @return true if the model file is available locally, false otherwise
     */
    public static boolean isModelDownloaded(@Nullable Context context, @Nullable WhisperModel model) {
        return isModelDownloadedLocally(context, model);
    }

    /**
     * Ensures the specified Whisper model is available locally, or downloaded to internal storage.
     * Throws {@link WhisperModelNotAvailable} if the model is unavailable and cannot be downloaded.
     */
    @NonNull
    public static File ensureModel(@NonNull Context context, @NonNull WhisperModel model) throws IOException {
        File targetFile = model.getFile(context);
        if (targetFile.exists() && targetFile.length() > 0) {
            return targetFile;
        }

        String downloadUrl = model.getDownloadUrl();
        if (downloadUrl != null && !downloadUrl.trim().isEmpty()) {
            downloadModelFromUrl(downloadUrl, targetFile, model.getDownloadSize(), null);
            return targetFile;
        }

        throw new WhisperModelNotAvailable(
                "Model not available locally or via download URL: " + model.getVariant());
    }


    /**
     * Asynchronously ensures the specified Whisper model is available locally,
     * or downloaded to internal storage.
     *
     * @param context the Android context
     * @param model the Whisper model
     * @param listener callback listener for progress, success, or error
     */
    public static void ensureModelAsync(
            @Nullable Context context, @Nullable WhisperModel model, @Nullable ModelDownloadListener listener) {
        ensureModelAsync(context, model, null, listener);
    }

    /**
     * Asynchronously ensures the specified Whisper model is available locally,
     * or downloaded to internal storage.
     *
     * @param context the Android context
     * @param model the Whisper model
     * @param executor optional custom executor to run the background task
     * @param listener callback listener for progress, success, or error
     */
    public static void ensureModelAsync(
            @Nullable Context context, @Nullable WhisperModel model, @Nullable Executor executor, @Nullable ModelDownloadListener listener) {
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
        Executor exec = executor != null ? executor : WhisperExecutors.getDefaultExecutor();
        exec.execute(() -> {
            try {
                File targetFile = model.getFile(context);
                if (targetFile.exists() && targetFile.length() > 0) {
                    listener.onSuccess(targetFile);
                    return;
                }

                String downloadUrl = model.getDownloadUrl();
                if (downloadUrl != null && !downloadUrl.trim().isEmpty()) {
                    downloadModelFromUrl(downloadUrl, targetFile, model.getDownloadSize(), listener);
                    listener.onSuccess(targetFile);
                    return;
                }

                throw new WhisperModelNotAvailable(
                        "Model not available locally or via download URL: " + model.getVariant());
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
            @Nullable Context context, @Nullable WhisperModel model, @Nullable ModelDownloadListener listener) {
        downloadModelAsync(context, model, null, listener);
    }

    /**
     * Asynchronously downloads the specified Whisper model file to internal storage.
     *
     * @param context the Android context
     * @param model the Whisper model
     * @param executor optional custom executor to run the background task
     * @param listener callback listener for progress, success, or error
     */
    public static void downloadModelAsync(
            @Nullable Context context, @Nullable WhisperModel model, @Nullable Executor executor, @Nullable ModelDownloadListener listener) {
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
        Executor exec = executor != null ? executor : WhisperExecutors.getDefaultExecutor();
        exec.execute(() -> {
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
     * Downloads a model file from the specified HTTP/HTTPS URL into targetFile.
     */
    public static void downloadModelFromUrl(@NonNull String urlString, @NonNull File targetFile) throws IOException {
        downloadModelFromUrl(urlString, targetFile, -1L, null);
    }

    /**
     * Downloads a model file from the specified HTTP/HTTPS URL into targetFile, reporting download progress.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void downloadModelFromUrl(
            @NonNull String urlString, @NonNull File targetFile, long expectedSize, @Nullable ModelDownloadListener listener)
            throws IOException {
        if (urlString.trim().isEmpty()) {
            throw new IllegalArgumentException("urlString is empty");
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
    @NonNull
    @SuppressWarnings("unused")
    public static List<File> listLocalModels(@NonNull Context context) {
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
     * Checks whether the specified Whisper model file is physically stored on internal device storage.
     * Unlike {@link #isModelDownloaded(Context, WhisperModel)}, this checks solely for local file presence.
     *
     * @param context the Android context
     * @param model the Whisper model
     * @return true if the model file exists on local storage and is non-empty, false otherwise
     */
    public static boolean isModelDownloadedLocally(@Nullable Context context, @Nullable WhisperModel model) {
        if (context == null || model == null) {
            return false;
        }
        File targetFile = model.getFile(context);
        return targetFile.exists() && targetFile.length() > 0;
    }

    /**
     * Removes the specified Whisper model file from internal device storage if it exists.
     *
     * @param context the Android context
     * @param model the Whisper model to remove
     * @return true if the model file existed on local storage and was successfully deleted, false otherwise
     */
    public static boolean removeModel(@Nullable Context context, @Nullable WhisperModel model) {
        if (context == null || model == null) {
            return false;
        }
        File targetFile = model.getFile(context);
        return removeModelFile(targetFile);
    }

    /**
     * Removes a model file with the specified filename from internal device storage if it exists.
     *
     * @param context the Android context
     * @param fileName the filename of the model to remove
     * @return true if the model file existed on local storage and was successfully deleted, false otherwise
     */
    public static boolean removeModel(@Nullable Context context, @Nullable String fileName) {
        if (context == null || fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        File targetFile = new File(getModelsDir(context), fileName.trim());
        return removeModelFile(targetFile);
    }

    /**
     * Removes the specified model file from device storage if it exists.
     *
     * @param modelFile the model file to remove
     * @return true if the model file existed and was successfully deleted, false otherwise
     */
    public static boolean removeModelFile(@Nullable File modelFile) {
        if (modelFile != null && modelFile.exists()) {
            return modelFile.delete();
        }
        return false;
    }

    /**
     * Alias for {@link #removeModel(Context, WhisperModel)}.
     */
    @SuppressWarnings("unused")
    public static boolean deleteModel(@Nullable Context context, @Nullable WhisperModel model) {
        return removeModel(context, model);
    }
}
