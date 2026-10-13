# M09 环境只读核查

核查时刻：2026-10-13 12:36 Asia/Shanghai / 04:36 UTC。
本报告仅登记环境现状，不替代 CURRENT_STATE，不授予模块验收。
范围：读取本仓库 AGENTS、工具链锁、M08 证据及忽略目录中的已有助手；查询工具版本、缓存和专用设备。未运行 Gradle、未安装或启动 App、未启动/重启模拟器、未改设备设置、未修改源码或 Git。
文件位于 `build/`；已实际执行 `git check-ignore -- build/m09-evidence/environment-audit.md`，返回本路径，确认忽略。

## 1. 工具链与实际位置

| 项目 | 本次实际结果 | 位置 / 说明 |
|---|---|---|
| JDK | Eclipse Temurin 17.0.20.1+1，OpenJDK 64-Bit | `C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1`；调用该目录 `bin/java.exe -version` 核对 |
| SDK | Platform API36 revision2，Build Tools36.0.0，Command-line Tools23.0 | `C:/Users/CDD/AppData/Local/MirraAndroid/sdk`；读取各包 `source.properties`；忽略的 `local.properties` 指向该路径 |
| ADB | Android Debug Bridge1.0.41，Platform Tools37.0.1-15733141 | `sdk/platform-tools/adb.exe`；实际 `version` 输出 |
| Emulator | 37.1.11.0，build15917651 | `sdk/emulator/emulator.exe`；实际 `-version` 输出，仅打印版本，没有启动设备 |
| 系统镜像 | API36 Google APIs x86_64 revision7，extension17 | `sdk/system-images/android-36/google_apis/x86_64`；包元数据实际读取 |
| Gradle | Wrapper锁8.14.3 all；同版本完整展开缓存存在 | `C:/Users/CDD/.gradle/wrapper/dists/gradle-8.14.3-all/10utluxaxniiv4wxiphsi49nj/gradle-8.14.3`，包括 `lib/gradle-launcher-8.14.3.jar`；没有运行 Gradle版本命令 |
| Gradle缓存 | `C:/Users/CDD/.gradle/caches/8.14.3`、`modules-2`、`jars-9`存在 | AGP8.13.2/KotlinGradlePlugin2.3.10 artifact目录实际存在；另有9.7.1缓存，不能因此改变本项目Wrapper |
| 项目缓存 | `.gradle/8.14.3`、`buildOutputCleanup`、`vcs-1`存在 | 不清理、不替换 |

Wrapper JAR实际SHA-256：`7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172`，与锁一致。
Wrapper保留分发SHA-256 `ed1a8d686605fd7c23bdf62c7fc7add1c5b23b2bbc3721e661934ef4a4911d7c`；本次未重新校验原分发ZIP字节，缓存只有展开目录与 `.zip.ok/.zip.lck`。

实际版本目录继续锁定：AGP8.13.2、Kotlin2.3.10、ComposeBOM2025.11.01、Activity1.12.4、Navigation3 1.0.1、Serialization1.9.0、Startup1.1.1、JUnit4.13.2、Room2.8.5、KSP2.3.6、DataStore1.2.1、Coroutines1.10.2、AndroidXTest1.7.0/ext.junit1.3.0、ComposeTest1.9.5、Lifecycle2.10.0、Espresso3.7.0。
本次没有解析或升级依赖；完整离线缓存是否覆盖后续所有任务为 NOT_RUN，不能仅凭目录存在宣称离线构建必过。

项目既有 `org.gradle.jvmargs=-Xmx2048m` 与 `android.overridePathCheck=true` 保持。不要恢复强制UTF-8 JVM参数：M02已经查明该参数会损坏本机JDK17中文路径测试参数文件。

## 2. 专用设备身份与 AVD

**目标专用设备在线：YES。** `adb devices -l`实际目标为 `emulator-5596 device`；本次其他连接条目为0。
只读getprop：API36、Android16、x86_64、`ro.kernel.qemu=1`、`sys.boot_completed=1`、`ro.boot.qemu.avd_name=Lithic_M01_API_36`。
镜像安全补丁属性：2025-07-05；它是当前固定镜像属性，不是本次SDK升级结果。

AVD注册文件：`E:/CODEX/砺石新APP/build/m01-device/avd/Lithic_M01_API_36.ini`。
实际磁盘目录：`E:/CODEX/砺石新APP/build/m01-device/avd/Lithic_M01_API_36.avd`。
ini中的绝对 `path` 与目录一致，target为android-36，config指向既有Google APIs x86_64镜像。
现有两个专用进程只读核对：emulator.exe PID21068；qemu-system-x86_64-headless.exe PID23872。
两者命令行为 `-avd Lithic_M01_API_36 -port 5596 -no-window -no-audio -no-snapshot-load -no-snapshot-save`。
PID只是本次快照，不可当作长期稳定身份。

当前进程环境中JAVA_HOME、ANDROID_HOME、ANDROID_SDK_ROOT、GRADLE_USER_HOME、ANDROID_SERIAL均为空；`ANDROID_AVD_HOME=E:/Android/avd`。
这个全局AVD目录不是本项目专用目录。设备已在线，无需启动AVD；未来如果确需重启，应显式使用本项目 `build/m01-device/avd`，不能按当前全局AVD目录选设备。核查期间没有启动命令。

## 3. 当前设备设置与已安装版本

本次只使用限定序列号的getprop、settings get、dumpsys查询：

| 设置 | 实际值 |
|---|---|
| persist.sys.timezone | GMT |
| auto_time / auto_time_zone | 1 / 1 |
| guest UTC（dumpsys alarm nowRTC） | 2026-10-13T04:36:15.650Z |
| guest与稍后host UTC差 | -1435ms；包含查询耗时，仅证明当前近似同步 |
| airplane_mode_on / wifi_on / mobile_data | 0 / 1 / 1 |
| Active default network | 101；当前有默认网络，尚未进入离线场景 |
| font_scale | 1.0 |
| accelerometer_rotation / user_rotation | 1 / 0 |
| show_ime_with_hard_keyboard | null |
| 显示 | 320×640，density160dpi，rotation0；dumpsys display读到base/override一致分辨率 |
| Night mode | no（mNightMode=1、mComputedNightMode=false） |
| 已安装App | `com.cooper.lithic.debug`；versionCode6；`0.1.0-m08-debug`；minSdk26/targetSdk36 |
| 安装登记 | firstInstallTime2026-10-11 08:26:15；lastUpdateTime2026-10-13 02:55:56（guest GMT） |

上述网络、字体、旋转与时区值和M08 `device-settings-restored.json`一致。未读取/改写用户数据，未实际打开App。已安装版本核对来自dumpsys package，并非本次安装。

## 4. 复用既有 runner 的最小命令

当前 `app/build.gradle.kts`实际runner为 `androidx.test.runner.AndroidJUnitRunner`。沿用M08的标准四任务与最多2个worker。以下只是交给执行者的命令，本审计没有执行；connected任务会构建/安装测试APK，应由主执行者在相应验证阶段运行。

```powershell
# 在 E:/CODEX/砺石新APP 工作目录的独立子进程里设置；不修改全局配置。
$env:JAVA_HOME='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1'
$env:ANDROID_SERIAL='emulator-5596'
.\gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug --max-workers=2 --console=plain
```

SDK由既有local.properties确定，无需另写SDK配置。保持JAVA_HOME与ANDROID_SERIAL设置和任务调用在同一构建进程中；每次还应核对实际Android XML中的device只为 `Lithic_M01_API_36(AVD) - 16`，不凭环境变量宣称设备隔离已证明。
单组行为RED可沿用原Android runner参数，例如：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.cooper.lithic.data.repository.SubscriptionRepositoryTest' --max-workers=2 --console=plain
```

这里只举现有类作为参数格式；M09具体选择测试类由主执行者按实际测试决定。没有新增runner或另一套流程。

已定位可复用来源：

- `build/m08-evidence/collect_results.py`：收集真正Android XML、逐方法结果、源码哈希及JVM XML；标准XML位置为 `app/build/outputs/androidTest-results/connected/debug` 和 `app/build/test-results/testDebugUnitTest`。
- 该助手硬编码M08输出目录，并以M07原359方法检查历史。不可直接执行后覆盖冻结证据；M09如沿用收集逻辑，仅在当前M09忽略目录保留本轮结果，并用M08的455方法身份核对完整回归。
- `build/m08-evidence/device_ui.py`：固定adb路径、专用serial和debug包；contains真正触摸、截图、输入、打开/停止操作。本次只读取，没有执行。
- `build/m08-evidence/device_ledger_snapshot.py`：原有合成数据只读DB/WAL复制及原四表比对，但脚本会force-stop且写M08目录，不能直接当本轮纯只读核查执行。
- `build/m08-evidence/old_device_setup.py`、`clock_stage.py`、`device_finish.py`：包含安装、网络/字体/时钟改变与恢复步骤。它们是M08历史实现，不是本轮自动可执行的审计命令。

M08正式证据登记455个唯一方法（86 JVM＋369 Android）PASS与专用设备旅程；这属于已存在的历史记录，本次没有重跑，不能作为M09测试结果。

## 5. 结论与尚未执行项

环境核查没有发现阻断后续M09构建的缺失工具或离线设备问题。专用API36模拟器在线，锁定JDK/SDK/Wrapper缓存可定位，无需升级工具链。
离线旅程前仍须由主执行者按授权范围登记当前设置、改变本项目专用guest网络并在结束时恢复；当前网络在线不等于离线验收已完成。
本次 Gradle任务、单元/Android测试、APK安装/启动、设备用户旅程、完整依赖缓存解析均 NOT_RUN，原因：只读审计授权范围。
