# M10-R1 — 隔离构建与产物解释

日期：2026-10-16。本文件是已固定官方源码、研究CMake与构建驱动的只读复核，**不是生产JNI方案、设备加载证明或M10验收**。此复核者没有执行编译、安装或设备操作；本轮真实执行结果以R1执行/结果报告及每次构建原始manifest为准。未在证据中出现的ABI加载、API26设备、16KiB设备、APK集成均为NOT_RUN。

## 1. 独立构建边界

- `scripts/build_native.py`要求显式SDK/JDK、已验证发行tar路径，检查tar SHA `6f30092cef9fb839779646608f4ee14ae3cbac989c47fa05e841b0841f09878e`；GPG与解包目录对应关系由R1来源核验负责。仅有tar哈希不能代替源树与tar对应核验。
- 固定NDK `28.2.13676358`与SDK CMake `3.22.1`，使用该SDK CMake包内Ninja。脚本检查安装版号与可执行文件，记录实际version输出；不下载、接受许可或更改全局环境。
- 每ABI先单独configure/build/install官方libjpeg-turbo3.2.0，再configure本研究的消费者项目；没有将上游`add_subdirectory()`嵌入，也不调用App Gradle。静态安装位置固定为每ABI的`prefix/include`和`prefix/lib`。
- 上游参数为静态库、PIC、`WITH_SIMD=OFF`、TurboJPEG开、tools/tests/fuzz/JNA关、bundled spng/zlib。关闭上游测试意味着不能把构建成功称作上游完整回归通过。SIMD关闭避免额外assembler依赖，当前CPU数据只适用于此研究配置。
- `generate_fixtures.c`是可选研究源码：configure时存在则增加同名独立可执行目标，链接相同固定libjpeg；不存在则不生成、不伪报缺失为成功。原锁定JPEG不由它生成或替换。首次构建输入快照已经包含此源码。
- 所有命令、退出码、wall时间、日志SHA、上游源树和研究输入快照、库和ELF输出SHA写入忽略的`build/m10-jpeg-r1/`；不会把缓存或二进制作为研究源码提交。构建结束再次比较输入，避免将途中改动混入同一次结果。

## 2. 四ABI与ELF证据的含义

| 请求ABI | 预期ELF class / Machine | 研究编译目标 |
|---|---|---|
| arm64-v8a | ELF64 / AArch64 | Android API26 |
| armeabi-v7a | ELF32 / ARM | Android API26 |
| x86 | ELF32 / Intel 80386 | Android API26 |
| x86_64 | ELF64 / Advanced Micro Devices X86-64 | Android API26 |

脚本对每ABI显式指定NDK toolchain、`ANDROID_ABI`、`ANDROID_PLATFORM=android-26`、`CMAKE_SYSTEM_VERSION=26`和`ANDROID_STL=none`。C源码不需要C++ STL，最终实际依赖仍须看ELF `DT_NEEDED`，不能仅凭配置宣称没有动态依赖。编译的API下限不等于API26设备已经运行。

CLI和shared均显式链接`-Wl,-z,max-page-size=16384`；上游静态对象为PIC。脚本保存未strip及strip后的ELF header/program header/dynamic依赖，要求实际全部`PT_LOAD.p_align >= 16384`且strip不改变对齐。复核时还应核对上表Machine/class，以及LOAD的文件偏移和虚拟地址的对齐关系。静态`.a`本身不是最终可加载ELF，没有可以代替shared检查的页对齐结论。

16KiB **ELF段对齐**、在4KiB模拟器加载、在16KiB设备加载和最终APK ZIP打包对齐是不同证据。此独立PoC不产出或修改正式APK；未执行的设备页大小与APK检查保持NOT_RUN。四ABI COMPILED也不能等同四ABI RUNTIME_VERIFIED。

## 3. 静态对象、动态依赖与体积

已读固定3.2.0实际上游`CMakeLists.txt`：

- 731～759行：只要TurboJPEG、tools或tests任一开启，就配置spng；本配置`WITH_SYSTEM_SPNG=OFF`和`WITH_SYSTEM_ZLIB=OFF`，因此使用固定tar内源码。
- 766～771行：`jpeg-static`由`JPEG_SOURCES`与适用SIMD对象构成，输出`libjpeg.a`，并不直接加入spng/zlib对象。
- 774～780、848～860行：`turbojpeg-static`包含`JPEG_SOURCES`、TurboJPEG包装和图像读写辅助代码，以及`ZLIB_STATIC_SOURCES`/`SPNG_STATIC_SOURCES`，输出`libturbojpeg.a`。关闭tools/tests不会从这个静态归档自动消除其PNG辅助对象。
- `src/spng/CMakeLists.txt`明确把spng和zlib编译成OBJECT目标；它们的对象被收进TurboJPEG归档，不是本配置单独部署的`libspng.so`/`libz.so`。

静态归档含某成员，不等于该成员全部被最终ELF保留。链接器按未解析符号从归档抽取成员，节回收及导出符号还会影响最终体积；必须结合实际归档成员、链接命令、最终符号及`DT_NEEDED`解释。ELF没有`libspng.so`这一NEEDED项也**不能**证明没有静态嵌入spng代码。不能把两个`.a`字节简单相加当作APK增量，或断言JPEG代码会在最终ELF中重复两份。

`libstrictjpeg_probe.so`由同一含`main()`的研究harness链接A1+A2，含诊断、资源测量及错误/取消研究入口所需引用；没有生产JNI接口。其实际strip字节是本实验shared产物大小，**不是**未来仅A2 JNI库的大小，更不是最终APK安装/压缩增量。后者均须在另获授权的生产构建中测量；本轮NOT_MEASURED。

## 4. 许可证与维护登记

从固定tar实际读取`LICENSE.md`、`README.ijg`、`src/spng/LICENSE`、`src/spng/libpng-LICENSE.txt`及`src/spng/zlib/LICENSE`：

- libjpeg API适用IJG；TurboJPEG整体同时满足IJG与Modified BSD-3。
- bundled spng主要为BSD-2，部分为PNG Reference Library License v2；zlib为zlib许可。上游LICENSE解释了整体许可中的兼容和归并关系。保留这些原文及来源清单可以避免将bundled代码误标为完全不存在。
- 若后续分发静态链接产物，产品文档需含IJG归属声明，并在使用TurboJPEG时包含适用BSD许可文本和版权声明；源码修改应标明修改并保留原声明。不应仅提交一个自拟许可证摘要代替原文。
- 本轮不分发生成的native二进制。许可证原文、具体实际打包对象及SBOM须在生产接入审查时一并落实；不能以“只有两个归档名字”为理由漏登记bundled来源。

独立固定版本仍需后续安全公告与发行跟踪；未来升级应重新核对源码签名/哈希，并重跑严格拒绝、资源、异常与ABI矩阵。本笔记不授予生产GO、不关闭M10原三项FAIL、不改变UI-C1冻结或M11未授权状态。
