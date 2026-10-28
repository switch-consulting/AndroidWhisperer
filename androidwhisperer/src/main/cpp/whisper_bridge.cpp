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

#include "whisper.h"
#include <vector>
#include <cstdint>
#include <cstring>

#if defined(_WIN32) || defined(__CYGWIN__)
    #define WHISPER_BRIDGE_API __declspec(dllexport)
#else
    #define WHISPER_BRIDGE_API __attribute__((visibility("default")))
#endif

extern "C" {

typedef void (*whisper_bridge_segment_cb)(
        int segment_index,
        const char * text,
        int64_t t0_ms,
        int64_t t1_ms,
        void * user_data);

typedef void (*whisper_bridge_progress_cb)(
        int progress_percent,
        void * user_data);

struct whisper_bridge_callback_data {
    whisper_bridge_segment_cb segment_cb;
    whisper_bridge_progress_cb progress_cb;
    void * user_data;
};

static void bridge_native_new_segment_cb(
        struct whisper_context * ctx,
        struct whisper_state * /*state*/,
        int n_new,
        void * user_data) {
    auto * cb_data = static_cast<whisper_bridge_callback_data *>(user_data);
    if (cb_data == nullptr || cb_data->segment_cb == nullptr || ctx == nullptr) {
        return;
    }

    int n_segments = whisper_full_n_segments(ctx);
    int start_index = n_segments - n_new;
    if (start_index < 0) {
        start_index = 0;
    }

    for (int i = start_index; i < n_segments; ++i) {
        int64_t t0 = whisper_full_get_segment_t0(ctx, i) * 10L;
        int64_t t1 = whisper_full_get_segment_t1(ctx, i) * 10L;
        const char * text = whisper_full_get_segment_text(ctx, i);
        cb_data->segment_cb(i, text != nullptr ? text : "", t0, t1, cb_data->user_data);
    }
}

static void bridge_native_progress_cb(
        struct whisper_context * /*ctx*/,
        struct whisper_state * /*state*/,
        int progress,
        void * user_data) {
    auto * cb_data = static_cast<whisper_bridge_callback_data *>(user_data);
    if (cb_data == nullptr || cb_data->progress_cb == nullptr) {
        return;
    }
    cb_data->progress_cb(progress, cb_data->user_data);
}

/**
 * Executes transcription inference using default greedy sampling parameters.
 *
 * @param ctx Valid whisper_context pointer.
 * @param samples PCM audio sample array (16 kHz, single channel).
 * @param n_samples Total number of samples.
 * @param token_timestamps Non-zero to enable token-level timestamp alignment.
 * @return 0 on success, non-zero on failure.
 */
WHISPER_BRIDGE_API int whisper_bridge_transcribe(
        struct whisper_context * ctx,
        const float * samples,
        int n_samples,
        int token_timestamps) {
    if (ctx == nullptr || samples == nullptr || n_samples <= 0) {
        return -1;
    }

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime   = false;
    params.print_progress   = false;
    params.print_timestamps = false;
    params.print_special    = false;
    params.token_timestamps = token_timestamps != 0;

    return whisper_full(ctx, params, samples, n_samples);
}

/**
 * Executes transcription inference with incremental callbacks for new segments and progress.
 *
 * @param ctx Valid whisper_context pointer.
 * @param samples PCM audio sample array (16 kHz, single channel).
 * @param n_samples Total number of samples.
 * @param token_timestamps Non-zero to enable token-level timestamp alignment.
 * @param segment_cb Callback invoked when new text segments are decoded.
 * @param progress_cb Callback invoked on progress updates (0 to 100).
 * @param user_data Arbitrary pointer forwarded to callbacks.
 * @return 0 on success, non-zero on failure.
 */
WHISPER_BRIDGE_API int whisper_bridge_transcribe_with_callback(
        struct whisper_context * ctx,
        const float * samples,
        int n_samples,
        int token_timestamps,
        whisper_bridge_segment_cb segment_cb,
        whisper_bridge_progress_cb progress_cb,
        void * user_data) {
    if (ctx == nullptr || samples == nullptr || n_samples <= 0) {
        return -1;
    }

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime   = false;
    params.print_progress   = false;
    params.print_timestamps = false;
    params.print_special    = false;
    params.token_timestamps = token_timestamps != 0;

    whisper_bridge_callback_data cb_data = { segment_cb, progress_cb, user_data };

    if (segment_cb != nullptr) {
        params.new_segment_callback = bridge_native_new_segment_cb;
        params.new_segment_callback_user_data = &cb_data;
    }

    if (progress_cb != nullptr) {
        params.progress_callback = bridge_native_progress_cb;
        params.progress_callback_user_data = &cb_data;
    }

    return whisper_full(ctx, params, samples, n_samples);
}

/**
 * Detects the spoken language from raw audio samples.
 *
 * @param ctx Valid whisper_context pointer.
 * @param samples PCM audio sample array (16 kHz, single channel).
 * @param n_samples Total number of samples.
 * @return Language ID on success, or a negative value on failure.
 */
WHISPER_BRIDGE_API int whisper_bridge_detect_language(
        struct whisper_context * ctx,
        const float * samples,
        int n_samples) {
    if (ctx == nullptr || samples == nullptr || n_samples <= 0) {
        return -1;
    }

    if (whisper_pcm_to_mel(ctx, samples, n_samples, 4) != 0) {
        return -1;
    }

    int max_id = whisper_lang_max_id();
    if (max_id < 0) {
        return -1;
    }

    std::vector<float> lang_probs(max_id + 1, 0.0f);
    int lang_id = whisper_lang_auto_detect(ctx, 0, 4, lang_probs.data());
    return lang_id;
}

} // extern "C"
