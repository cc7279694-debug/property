cmake_minimum_required(VERSION 3.22.1)
project(lithic_jpeg_candidate C)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
if(NOT ANDROID OR NOT CMAKE_SYSTEM_VERSION STREQUAL "26")
  message(FATAL_ERROR "Research candidate requires the Android API26 toolchain")
endif()
foreach(required JPEG_ARCHIVE JPEG_SOURCE_DIR JPEG_BINARY_DIR)
  if(NOT DEFINED ${required} OR NOT EXISTS "${${required}}")
    message(FATAL_ERROR "Missing isolated input: ${required}")
  endif()
endforeach()
add_library(jpeg_budget STATIC IMPORTED)
set_target_properties(jpeg_budget PROPERTIES IMPORTED_LOCATION "${JPEG_ARCHIVE}")
add_library(strictjpeg_candidate SHARED strict_probe.c budget.c jni_bridge.c)
target_compile_definitions(strictjpeg_candidate PRIVATE E01_NO_MAIN)
target_compile_options(strictjpeg_candidate PRIVATE -Wall -Wextra -Werror -fvisibility=hidden)
target_include_directories(strictjpeg_candidate PRIVATE
  "${CMAKE_CURRENT_SOURCE_DIR}" "${JPEG_SOURCE_DIR}/src" "${JPEG_BINARY_DIR}")
target_link_libraries(strictjpeg_candidate PRIVATE jpeg_budget m)
set_target_properties(strictjpeg_candidate PROPERTIES
  POSITION_INDEPENDENT_CODE ON C_VISIBILITY_PRESET hidden)
file(WRITE "${CMAKE_CURRENT_BINARY_DIR}/jni_exports.map"
  "{ global: Java_com_cooper_lithic_research_StrictJpegCandidate_validateReadonlyFd; local: *; };\n")
target_link_options(strictjpeg_candidate PRIVATE
  "-Wl,--version-script=${CMAKE_CURRENT_BINARY_DIR}/jni_exports.map"
  -Wl,--exclude-libs,ALL -Wl,--no-undefined -Wl,-z,relro,-z,now
  -Wl,-z,max-page-size=16384)
