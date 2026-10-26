package de.switchconsulting.androidwhisperer;

import java.io.File;

/**
 * Callback interface for asynchronous model download and extraction operations.
 */
public interface ModelDownloadListener {

    /**
     * Called periodically during model download to report progress.
     * A totalBytes value of -1 indicates that the overall download size is not known.
     *
     * @param bytesDownloaded number of bytes downloaded so far
     * @param totalBytes total expected download size in bytes, or -1 if unknown
     */
    default void onProgress(long bytesDownloaded, long totalBytes) {}

    /**
     * Called when the model file has been successfully downloaded or extracted.
     *
     * @param modelFile the local model file on the device
     */
    void onSuccess(File modelFile);

    /**
     * Called when an error occurs during model download or extraction.
     *
     * @param error the exception encountered
     */
    void onError(Exception error);
}
