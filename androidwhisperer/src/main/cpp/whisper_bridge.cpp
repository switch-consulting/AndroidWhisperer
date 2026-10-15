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
