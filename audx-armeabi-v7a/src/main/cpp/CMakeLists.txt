# Per-ABI shim. Sets AUDX_PREBUILT_ROOT to this module's libs/ and
# delegates the actual build to the canonical CMakeLists.txt in :audx-core.
cmake_minimum_required(VERSION 3.22.1)
project("audx-android")

set(AUDX_PREBUILT_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../libs")
include("${CMAKE_CURRENT_SOURCE_DIR}/../../../../audx-core/src/main/cpp/CMakeLists.txt")
