# Canonical audx-android JNI bridge build.
#
# Intended use:
#   - In the legacy `:audx` Android library: this file is the top-level CMakeLists,
#     reached via android.externalNativeBuild.cmake.path. `project()` is called here.
#   - In each per-ABI module (audx-arm64-v8a, audx-x86_64, ...): a small shim
#     CMakeLists.txt in that module sets AUDX_PREBUILT_ROOT and `include()`s this file.
#     The shim calls `project()`; this file detects that and does NOT call `project()` again.
cmake_minimum_required(VERSION 3.22.1)

if(NOT PROJECT_NAME)
    project("audx-android")
endif()

# AUDX_PREBUILT_ROOT points at the directory that contains <ABI>/libaudx_src.so.
# When invoked from the legacy :audx layout it defaults to ../libs relative to this file
# (i.e. audx/src/main/libs). Per-ABI modules override it before include()'ing this file.
if(NOT DEFINED AUDX_PREBUILT_ROOT)
    set(AUDX_PREBUILT_ROOT "${CMAKE_CURRENT_LIST_DIR}/../libs")
endif()

add_library(audx-android SHARED
        ${CMAKE_CURRENT_LIST_DIR}/audx.cpp
        ${CMAKE_CURRENT_LIST_DIR}/audx.h)

target_include_directories(audx-android PRIVATE
        ${CMAKE_CURRENT_LIST_DIR})

add_library(audx_src SHARED IMPORTED)
set_target_properties(audx_src PROPERTIES
        IMPORTED_LOCATION ${AUDX_PREBUILT_ROOT}/${ANDROID_ABI}/libaudx_src.so)

target_link_libraries(audx-android
        audx_src
        android
        log)
