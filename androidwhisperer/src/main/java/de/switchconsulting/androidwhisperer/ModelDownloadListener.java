package de.switchconsulting.androidwhisperer;

import java.io.File;

/**
 * Callback interface for asynchronous model download and extraction operations.
 */
public interface ModelDownloadListener {

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
