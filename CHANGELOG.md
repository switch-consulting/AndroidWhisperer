# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - Initial Release

### Added
- Initial public release of **AndroidWhisperer** (`de.switch-consulting:androidwhisperer:0.1.0`) on Maven Central.
- High-level, idiomatic Java and Kotlin API wrapping `whisper.cpp` for fast, local, on-device speech-to-text inference on Android.
- Pre-compiled native `whisper.cpp` engines optimized for 64-bit architectures (`arm64-v8a` and `x86_64`). No NDK or native compilation required for app developers.
- `ModelManager` for asynchronous model downloading, verification, and management of quantized GGML models (`tiny`, `base`, `small`, `medium`, `large`, and English-only variants).
- Segment-level and token-level timestamps, plus automatic silence/pause analysis (`SegmentResult`, `PauseSegment`).
- Language auto-detection directly from PCM sample arrays.
- `AudioUtils` WAV parser and PCM converter/resampler (converting PCM16/float audio to 16 kHz mono float PCM).
- Full Kotlin Coroutines and Java `CompletableFuture` support.
