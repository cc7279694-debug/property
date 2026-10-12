# 工具链锁：M01 基线与后续获准模块增量

记录日期：2026-10-11。版本取自官方稳定发行资料及本机实际检查。选择兼容的稳定组合，未追逐最新版本；禁止动态版本、预发布依赖及在普通模块中顺便升级。

## 已启用的精确版本

| 项目 | 版本 | 固定位置 / 官方依据 |
|---|---|---|
| Gradle | 8.14.3（all） | [Wrapper 配置](../gradle/wrapper/gradle-wrapper.properties)；[官方发行](https://services.gradle.org/distributions/gradle-8.14.3-all.zip) |
| Android Gradle Plugin | 8.13.2 | [版本目录](../gradle/libs.versions.toml)；[AGP 8.13 发布与兼容表](https://developer.android.com/build/releases/agp-8-13-0-release-notes) |
| Kotlin / Compose Compiler / Serialization 插件 | 2.3.10 | 同一版本；[Kotlin Gradle 兼容表](https://kotlinlang.org/docs/gradle-configure-project.html) |
| Compose BOM | 2025.11.01 | [Google Maven 原件](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2025.11.01/compose-bom-2025.11.01.pom) |
| Compose UI / runtime / Material3 | BOM 管理 1.9.5 / 1.9.5 / 1.4.0 | 已实际核对 debugRuntimeClasspath，与固定 BOM 一致 |
| Activity Compose | 1.12.4 | [官方发布记录](https://developer.android.com/jetpack/androidx/releases/activity) |
| Navigation3 runtime / ui | 1.0.1 | [官方发布记录](https://developer.android.com/jetpack/androidx/releases/navigation3)；[空宿主 DSL](https://developer.android.com/guide/navigation/navigation-3/recipes/basicdsl) |
| kotlinx-serialization-core | 1.9.0 | [官方发布记录](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.9.0) |
| AndroidX Startup runtime | 1.1.1 | [官方发布记录](https://developer.android.com/jetpack/androidx/releases/startup)；从既有传递运行依赖改为显式声明以支持Manifest覆盖，未升级运行版本 |
| Java 编译 / Kotlin JVM 目标 | 17 | [app 构建配置](../app/build.gradle.kts) |
| minSdk / compileSdk / targetSdk | 26 / 36 / 36 | 同上；[Navigation3 SDK 要求](https://developer.android.com/guide/navigation/navigation-3/get-started) |
| Android Build Tools | 36.0.0 | 同上；本机 SDK 包元数据与实际构建验证 |

AGP 8.13 最低 Gradle 8.13、JDK 17；KGP 2.3.10 完整支持 Gradle 7.6.3～9.0.0 与 AGP 8.2.2～9.0.0，包含本组合。Gradle `--version` 中的 Kotlin 2.0.21 是 Gradle 内置 Kotlin DSL 版本，不是应用的 Kotlin 插件版本。

M01～M03 没有启用 Room、KSP、DataStore；该历史验证不替代本轮 M04 的新增依赖检查。WorkManager、网络客户端仍未启用。

Emoji2 1.4.0为传递依赖，其默认启动配置会调用可下载字体提供者（已读取Google Maven该版本sources.jar及[官方说明](https://developer.android.com/develop/ui/views/text-and-emoji/emoji2)）。按本轮无远程字体要求，仅移除EmojiCompatInitializer的合并meta-data，保留其他Startup初始化器；最终APK/合并Manifest另检查该入口不存在。显式Startup声明解决该Manifest引用的compile/lint可见性，不增加新的运行功能。

## 官方 Wrapper 校验

Wrapper 由现有官方 Gradle 8.14.3 的 `wrapper` 任务生成，任务实际退出 0，完整提交 `gradlew`、`gradlew.bat`、JAR 和配置。不是从其他工程拷贝。保留 URL 验证和分发 SHA-256 校验。

- 分发 ZIP SHA-256：`ed1a8d686605fd7c23bdf62c7fc7add1c5b23b2bbc3721e661934ef4a4911d7c`，来自[官方分发校验值](https://downloads.gradle.org/distributions/gradle-8.14.3-all.zip.sha256)。
- Wrapper JAR SHA-256：`7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172`，本地实际计算与[官方 JAR 校验值](https://downloads.gradle.org/distributions/gradle-8.14.3-wrapper.jar.sha256)一致。
- `--version` 实际输出 Gradle 8.14.3、Launcher/Daemon JVM Temurin 17.0.20.1+1。
- 此机已有分发缓存；本次没有重新下载该 ZIP，故不声称对缓存分发原 ZIP 做了重新逐字节验证。配置校验保护今后下载；实际生成 JAR 已另验官方哈希。

## 本机实际环境

- Windows 11 amd64，系统构建 10.0.26200。
- JDK：Eclipse Temurin 17.0.20.1+1，实际 `java -version` 已核对。
- 项目构建只在进程内指定 `JAVA_HOME`，复用 `C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1`；不修改其他项目或全局 JDK。
- SDK：复用 `C:/Users/CDD/AppData/Local/MirraAndroid/sdk`，只在忽略的 `local.properties` 配置。Platform 36 revision 2；Build Tools 36.0.0；Command-line Tools 23.0（安装目录名为 latest，实际包版本已核对）；Platform Tools/ADB 37.0.1。
- Emulator 37.1.11.0（15917651）；WHPX 加速检查返回 0。已有 API 36 Google APIs x86_64 revision 7 镜像。首次设备清单为空，不声称已有真机。
- 本模块专用 AVD `Lithic_M01_API_36` 存于忽略的 `build/m01-device/avd`，不复用或修改其他项目 AVD。
- Windows 中文工作目录触发 AGP 路径预检查，首次构建退出 1；按 AGP 错误给出的项目级选项设置 `android.overridePathCheck=true`，保留指定工作目录并以实际构建验证。此选项不关闭依赖校验或系统安全配置。

## 重现与验证范围

准备 JDK 17、SDK Platform 36 与 Build Tools 36.0.0，在忽略的 `local.properties` 写本机 SDK 路径，然后在根目录执行：

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

开发机可下载官方依赖；运行中的 App 不依赖网络。构建输出、最终 APK 检查及设备证据以[验收登记](ACCEPTANCE_MATRIX.md)和[M01 checkpoint](checkpoints/2026-10-11-M01.md)为准，不用版本表替代通过记录。

本轮构建验证针对 Android API 36 模拟器及 Debug APK；minSdk 26 配置不等于所有 API 26～36/OEM 真机均已验证。没有业务 schema 或完整备份格式，系统排除配置不替代 OEM 迁移行为实测。

最终 assembleDebug + lintDebug 退出 0。修正 local.properties 的 Windows 驱动器冒号转义后，因 lintAnalyzeDebug 曾复用旧报告，另执行 `:app:lintDebug --rerun-tasks`，退出 0；随后根标准命令再次退出 0。lint 报告为 0 errors / 12 warnings：OldTargetApi 1、AndroidGradlePluginVersion 2、GradleDependency 5、NewerVersionAvailable 4，都是 SDK/工具/依赖的新版本提示。保留警告和当前稳定兼容组合，不用 baseline、disable 或忽略规则隐藏错误。初次 SDK XML v4 与旧解析器提示、中文路径选项 experimental 提示、Debug 原生库符号未剥离提示均在日志/证据中保留。

可重复最终 APK 配置检查脚本为[verify_m01_apk.py](../scripts/verify_m01_apk.py)，已在本机 Python 3.12 运行通过（要求 Python 3.11+ 和 Windows SDK Build Tools 36.0.0）。该脚本检查构建 APK 和合并 Manifest；不代替签名或设备验证。

## M02 增量（2026-10-11）

以上 M01 工具链、SDK 和依赖版本全部继承，未升级；当前仅在版本目录添加精确 `junit = "4.13.2"`，通过 `testImplementation(libs.junit)` 用于本地 JVM 测试。采用[JUnit 官方依赖坐标](https://junit.org/junit4/dependency-info.html)与[Android 本地测试配置](https://developer.android.com/training/testing/local-tests)，不引入 Robolectric、Instrumentation、Room 或业务库。实际测试/运行依赖解析结果另记录于[M02证据](evidence/M02-verification.md)。

本机 JDK17 launcher 的原生参数文件解码为 GBK，原配置强制 Gradle daemon `file.encoding=UTF-8` 会使 worker 的 `@argfile` 写出与启动器解码不一致，损坏中文 classpath 并产生 initializationError。已实际核对编译后的测试类、参数文件字节及启动器最终 classpath；[Gradle 官方同类缺陷 #30304](https://github.com/gradle/gradle/issues/30304)与[固定8.14.3 ArgWriter源码](https://raw.githubusercontent.com/gradle/gradle/v8.14.3/platforms/core-runtime/base-services/src/main/java/org/gradle/internal/process/ArgWriter.java)支持该根因。

项目级修复只移除 `org.gradle.jvmargs` 的强制 `-Dfile.encoding=UTF-8`，让现有 JDK17 的写出与启动器默认编码一致；保留2GB堆和既有中文路径预检查选项，不硬编码GBK，不修改系统编码或其他项目，不升级Gradle/JDK。使用相同测试先验证环境修复，再以标准命令取得真实行为RED与GREEN。初次初始化错误不计入有效RED。源文件和验收JSON仍按UTF-8保存；跨平台无需增加Windows专用测试路径。

M02 继续使用 M01 Debug身份和版本名/code1，未作为新安装版本发布。最终 APK 仅用于构建与配置回归；没有重新进行设备安装、真机/API26/OEM验证，M01历史记录和未运行边界保持不变。

## M04 新增存储与 Android 测试依赖（2026-10-11）

| 精确版本 | 用途与官方依据 |
|---|---|
| Room runtime / ktx / compiler / Gradle plugin 2.8.5 | [官方稳定发布与 schema 配置](https://developer.android.com/jetpack/androidx/releases/room)；KSP 编译、schemaDirectory 导出；Room 2.8 最低 API23，本项目 API26 满足 |
| KSP 2.3.6（KSP2） | [官方发行](https://github.com/google/ksp/releases/tag/2.3.6)包含 Windows 模块重编译修复；[KSP2 说明](https://github.com/google/ksp/blob/main/docs/ksp2.md)；不用 kapt，不改 Kotlin2.3.10 |
| Preferences DataStore 1.2.1 | [官方稳定发布](https://developer.android.com/jetpack/androidx/releases/datastore)；真实受控文件 `settings.preferences_pb`，不额外显式声明 JSON Serialization，实际传递依赖另见下文 |
| Coroutines Android / core 1.10.2 | [官方发行](https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.10.2)；存储 suspend/Mutex/IO，精确声明 Android 坐标 |
| AndroidX Test core / runner 1.7.0、ext.junit 1.3.0 | [官方稳定表](https://developer.android.com/jetpack/androidx/releases/test)；仅 androidTestImplementation，runner 为 AndroidJUnitRunner |

实际逐项取得官方 Google Maven 与 Maven Central 的 POM，核对 artifact/version；`debugRuntimeClasspath` 解析退出0，没有 FAILED 依赖。Room Gradle plugin 的 POM、KSP plugin 的 POM均真实可解析，不凭推荐数字宣称可用。最终编译、测试和运行依赖差异以 M04 实际证据为准，解析成功本身不等于兼容测试通过。

再次实际解析 debugRuntimeClasspath，按报告选择版本（解析 `->`，包含平台/约束与 Android/JVM variant 坐标）与 M03 报告比对：96→121 个唯一 module，25 新增、5 个已有坐标提升、0 移除。新增含 Room2.8.5、SQLite2.6.2、DataStore1.2.1、Okio3.9.1 和 DataStore 引入的 serialization-json/json-jvm1.9.0；annotation-experimental1.4.1→1.5.0，四个 Coroutines 坐标1.9.0→1.10.2。Compose/Activity/Nav3/AGP/Kotlin 原显式锁定值未升级；不是所有传递运行依赖都未变。JUnit/AndroidX Test 只在测试配置，不应进入最终APK。完整差异收录于本轮证据 JSON。

冻结的 Gradle8.14.3、AGP8.13.2、Kotlin2.3.10、JDK17、ComposeBOM2025.11.01、Navigation3 1.0.1、SDK26/36/36、BuildTools36.0.0不变。仅本轮 Debug 构建标识为 versionCode2 / `0.1.0-m04-debug`，用于区分实际交付 APK；不是生产发行。

复用砺石自己的 `Lithic_M01_API_36` 私有 AVD，以独立端口5596隐藏运行，不修改其他项目AVD。当前有手机和其他项目模拟器连接，构建进程用 `ANDROID_SERIAL=emulator-5596` 限定：已读取固定AGP8.13.2的 DeviceProviderInstrumentTestTask 实际字节码，确认调用该环境变量并传给 ConnectedDeviceProvider。每次设备报告还需核对只包含此序列号。该设置只用于当前构建进程，不修改全局环境。

### M04 实际兼容验证（2026-10-12）

完整 `:app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug --max-workers=2 --console=plain` 退出0，用时2m52s。74 JVM及86真实Android SQLite/DataStore测试全部PASS，KSP2生成Room v1 schema。资源参数max-workers=2仅减少本机并发，不是工具链升级；JDK17编码方案保持M02修复，不强制UTF8/GBK。

Lint0 errors/13 warnings：OldTargetApi1、AndroidGradlePluginVersion2、GradleDependency5、NewerVersionAvailable5，均版本提示；相比此前12新增Coroutines版本提示，不抑制/升级。最终APK code2/0.1.0-m04-debug检查、v2签名、API36专用设备安装和两次断网冷启动及同代重开PASS；网络恢复。无新增用户数据访问/网络权限，JUnit与AndroidX Test未打包。实际APK、schema、依赖差异、54输入和结果/日志哈希见[M04证据](evidence/M04-verification.json)。

NOT_RUN：真实手机、API26运行、OEM备份行为；M08显式迁移、M10图片、M11/M12完整备份/恢复。不能用本次API36实测或配置minSdk26替代这些验证。
