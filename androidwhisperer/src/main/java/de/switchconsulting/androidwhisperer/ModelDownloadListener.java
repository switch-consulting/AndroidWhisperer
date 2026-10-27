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
