# M01 工具链锁

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

Room、KSP、DataStore、WorkManager、网络客户端及其他业务依赖未启用；本轮未为后续库承诺兼容验证。

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
