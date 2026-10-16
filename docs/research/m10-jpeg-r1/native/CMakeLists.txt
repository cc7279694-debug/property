cmake_minimum_required(VERSION 3.22.1)
project(lithic_m10_r1_probe VERSION 1.0.0 LANGUAGES C)

# Research-only consumer of a separately configured/installed official source
# build.  No add_subdirectory() of libjpeg-turbo, App Gradle, or JNI entry point.
# NDK r28's legacy toolchain deliberately sets CMAKE_SYSTEM_VERSION=1;
# ANDROID_PLATFORM_LEVEL is its actual target API (also in Clang's triple).
if(NOT ANDROID OR NOT ANDROID_PLATFORM_LEVEL STREQUAL "26")
  message(FATAL_ERROR "This isolated PoC must target Android API 26")
endif()
if(NOT R1_JPEG_PREFIX)
  message(FATAL_ERROR "Set R1_JPEG_PREFIX to this ABI's verified static install")
endif()
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_C_EXTENSIONS ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

foreach(header jpeglib.h jerror.h jmorecfg.h jconfig.h turbojpeg.h)
  if(NOT EXISTS "${R1_JPEG_PREFIX}/include/${header}")
    message(FATAL_ERROR "Missing installed header: ${header}")
  endif()
endforeach()
foreach(lib jpeg turbojpeg)
  if(NOT EXISTS "${R1_JPEG_PREFIX}/lib/lib${lib}.a")
    message(FATAL_ERROR "Missing installed archive: lib${lib}.a")
  endif()
  add_library(R1::${lib} STATIC IMPORTED)
  set_target_properties(R1::${lib} PROPERTIES
    IMPORTED_LOCATION "${R1_JPEG_PREFIX}/lib/lib${lib}.a"
    INTERFACE_INCLUDE_DIRECTORIES "${R1_JPEG_PREFIX}/include")
endforeach()

function(r1_harden target)
  target_compile_options(${target} PRIVATE
    -Wall -Wextra -Wformat=2 -fno-common -fstack-protector-strong)
  target_link_options(${target} PRIVATE
    -Wl,--no-undefined -Wl,-z,relro -Wl,-z,now
    -Wl,-z,max-page-size=16384)
endfunction()

add_executable(strictjpeg_probe strictjpeg_probe.c)
target_link_libraries(strictjpeg_probe PRIVATE R1::jpeg R1::turbojpeg m)
r1_harden(strictjpeg_probe)

# Identical C harness, including main(), compiled as an ELF shared object for
# output-size/symbol/alignment evidence only.  It is not a production JNI API.
add_library(strictjpeg_probe_shared SHARED strictjpeg_probe.c)
set_target_properties(strictjpeg_probe_shared PROPERTIES OUTPUT_NAME strictjpeg_probe)
target_link_libraries(strictjpeg_probe_shared PRIVATE R1::jpeg R1::turbojpeg m)
r1_harden(strictjpeg_probe_shared)

if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/generate_fixtures.c")
  add_executable(generate_fixtures generate_fixtures.c)
  target_link_libraries(generate_fixtures PRIVATE R1::jpeg m)
  r1_harden(generate_fixtures)
endif()
