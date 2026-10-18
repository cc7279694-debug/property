# JPEG Worker candidate native assembly

The candidate packages the four already compiled API26 ABIs of
`libstrictjpeg_candidate.so`. No native binary or upstream archive is tracked.
`scripts/build_m10_worker_candidate.py` checks the fixed official archive SHA,
eight actual compiled source inputs, locked ABI byte hashes, installed NDK,
ELF ABI/dependencies/16KiB LOAD alignment and the single JNI export **before**
copying them to ignored `app/build/generated/m10StrictJpeg/`.

Windows Git checkouts may change LF to CRLF. The assembly audit records both
raw hashes and permits only that exact newline conversion; any other source
byte difference is rejected. The ninth historical input was the build driver,
whose ELF parser changed after compilation. It is explicitly not presented as
an unchanged compiled input. The current parser reaudits the binaries.

Gradle requires these explicit local properties (no machine paths are stored):

```text
-PstrictJpegSdk=<existing Android SDK>
-PstrictJpegArchive=<fixed libjpeg-turbo-3.2.0.tar.gz>
-PstrictJpegNativeRoot=<previous verified four-ABI build directory>
-PstrictJpegPython=<existing Python 3.12+, optional>
```

Source reproduction remains the existing independent
`docs/research/m10-jpeg-accelerated/build_android.py` with installed
NDK 28.2.13676358 / CMake 3.22.1 and the fixed archive. It does not download
tools. A rebuilt binary must be independently audited and its new result
explicitly reviewed before replacing the present locked binary inputs.

Upstream: [libjpeg-turbo 3.2.0](https://github.com/libjpeg-turbo/libjpeg-turbo/releases/tag/3.2.0).
Archive SHA-256:
`6f30092cef9fb839779646608f4ee14ae3cbac989c47fa05e841b0841f09878e`.
The build packages the exact upstream `LICENSE.md` and `README.ijg`, plus a
LITHIC change acknowledgement, as assets. This software is based in part on
the work of the Independent JPEG Group. The replacement budget backend and
JNI validator are identified separately; TurboJPEG/SIMD/tools are disabled.

The Java declaration does not auto-load a library in the Host. Only the
dedicated Worker may explicitly load/call this synchronous validator. Packing
the library is not proof of Android loading, cancellation, Binder/FD safety,
native memory hard bounds or successful cover import.
