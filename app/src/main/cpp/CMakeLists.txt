cmake_minimum_required(VERSION 3.22.1)

project("whisper_bridge" C CXX)

set(BUILD_SHARED_LIBS ON CACHE BOOL "Build shared libraries" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "Build examples" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "Build tests" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "Build server" FORCE)

# Add upstream whisper.cpp library
add_subdirectory(whisper.cpp)

# Add project-owned JNA bridge file into libwhisper.so
target_sources(whisper PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/whisper_bridge.cpp)
target_include_directories(whisper PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/include)
