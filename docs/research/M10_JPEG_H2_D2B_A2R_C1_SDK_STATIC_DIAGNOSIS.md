# M10-H2-D2B-A2-R-C1 — SDK环境静态诊断

2026-10-18。只读文件、源码与字节码分析；没有SDK/AVD执行、ADB、下载、全局环境修改或运行时重试。锁定输入`4ee27bfcbe5903d6a49d4a92203de8a63686ba66`。

## 已确认的具体版本和环境路径

读取独立研究SDK的`source.properties`：cmdline-tools **23.0**；emulator **37.1.11 / build15917651**；platform-tools **37.0.1**。JDK静态读取器为既有17.0.20.1+1。镜像仍为API26 / google_apis / x86_64 / revision16，未更新任何组件。

研究根为`E:/CODEX/.codex/research-env/lithic-a2r-api26`。实际`smoke_isolated.py`第85～89行以`dict(os.environ, ...)`生成子进程环境，第105行传给命令执行：

| 变量 | 明确设置的研究根下相对路径 | 语义 |
|---|---|---|
| `ANDROID_HOME` | `sdk` | SDK安装根 |
| `ANDROID_SDK_ROOT` | `sdk` | 已弃用的SDK根别名；与HOME一致，提案保留以减少变化 |
| `ANDROID_USER_HOME` | `android-user` | 工具偏好目录本身 |
| `ANDROID_SDK_HOME` | `legacy-home` | 旧工具的`.android`父目录；有效偏好目录为`legacy-home/.android` |
| `ANDROID_AVD_HOME` | `avds` | 独立AVD注册/查找目录 |
| `ANDROID_EMULATOR_HOME` | `emulator-home` | 独立模拟器用户配置 |

因此USER_HOME与旧SDK_HOME代表的偏好目录冲突是CONFIRMED，并非缺少`ANDROID_HOME`赋值。原创建代码还会建立`legacy-home`，不能简单假定旧变量路径无效而被忽略。

C1当前只读快照中，Process/User的父`ANDROID_AVD_HOME`为`E:/Android/avd`，但研究子进程会明确覆盖为独立`avds`，不得借此访问原目录。其余已采集ANDROID路径和JAVA_HOME未配置。`ANDROID_PREFS_ROOT`及五类Java选项在Process/User/Machine本次采样中均未设置；仅保存存在性/哈希，不归档任意选项值。**这是C1时刻，不是原create时刻的完整环境证明。**

`avdmanager.bat`会透传`JAVA_OPTS`、`AVDMANAGER_OPTS`，JVM还可能读取`JAVA_TOOL_OPTIONS`、`_JAVA_OPTIONS`、`JDK_JAVA_OPTIONS`。未来执行前必须检查路径系统属性覆盖与身份冲突；当前缺少历史完整值，不能追认当时不存在其他影响。

## 来源与本地工具的静态证据

1. [Google环境变量文档](https://developer.android.com/tools/variables)（本次实际读取，页面更新2026-10-05）区分SDK根、偏好目录及旧`.android`父目录。`ANDROID_SDK_HOME`不是SDK安装根。
2. [WPT提交2c53cd1bd03686cef080cdfc46c966e15869e782](https://chromium.googlesource.com/external/github.com/web-platform-tests/wpt/+/2c53cd1bd03686cef080cdfc46c966e15869e782)，作者日期2026-07-28、提交日期2026-09-09；[精确diff](https://chromium.googlesource.com/external/github.com/web-platform-tests/wpt/+/2c53cd1bd03686cef080cdfc46c966e15869e782%5E!/)移除其旧SDK_HOME赋值，记录USER_HOME与SDK_HOME不同路径会导致同一AVD `.ini`错误。它是独立项目的可比记录，不是本次实验。
3. [Google上游AbstractAndroidLocations源码](https://android.googlesource.com/platform/tools/base/+/2ea2481f2afc4382eb3dd55e76e0cd125f9b41db/common/src/main/java/com/android/prefs/AbstractAndroidLocations.kt)佐证偏好路径一致性比较。**此源码revision未证明与本地23.0完全一致。**
4. 对本地锁定工具JAR使用`javap -c -p`读取字节码，命令/退出码/JAR输入SHA与原输出分别见[首批清单](m10-jpeg-h2-d2b-a2r-c1/evidence/tool-static/commands.json)、[跟进清单](m10-jpeg-h2-d2b-a2r-c1/evidence/tool-static-followup/commands.json)。这只执行JDK静态class阅读器，没有运行目标SDK类或其初始化。
5. 本地`AbstractAndroidLocations.computeAndroidFolder()`确实把USER_HOME、PREFS_ROOT、SDK_HOME交给`singlePathOf`；`AvdManager.createAvdIniFile()`在`AndroidSdkHandler.getAndroidFolder()`为空时抛出原错误。它读取的是Android偏好位置字段，不能仅凭错误措辞推断SDK根不存在。证据在[位置类](m10-jpeg-h2-d2b-a2r-c1/evidence/tool-static-followup/locations-abstract.stdout.txt)与[AVD类](m10-jpeg-h2-d2b-a2r-c1/evidence/tool-static/avd-manager.stdout.txt)。完整失败调用栈没有保存，字段为何为空仍是待证因果链。

## 结论置信度

| 判断 | 状态 | 边界 |
|---|---|---|
| 当前脚本明确注入冲突偏好目录 | CONFIRMED | 静态源码及目录含义 |
| 唯一create退出1并报告`.ini`定位错误 | CONFIRMED | 原命令stderr122字节，SHA `c129dd0d965692de49904529e1c7e7d826b584ef08308243c8b6a5c0566eb3fa` |
| 该冲突造成本次相同错误 | SUPPORTED_HYPOTHESIS | 官方说明、WPT同症状、实际工具分支支持；未运行受控对比 |
| 唯一运行时根因/修正后可创建 | UNKNOWN / NOT_RUN | 无堆栈、无原完整继承环境、禁止重发创建 |
| SDK安装器0xC0000409根因 | UNKNOWN | 本轮不诊断或重试管理器，不由目录假设覆盖 |

## 最小未应用提案

[提案文本](m10-jpeg-h2-d2b-a2r-c1/environment-change.PROPOSAL.diff.txt)仅移除子进程`ANDROID_SDK_HOME`，显式`pop`防止父环境重新继承，同时保留独立USER/AVD/EMULATOR目录和一致SDK根。**未修改原`environment()`，未应用patch，未修改Windows全局环境。**不通过补造`.android`、改SDK布局、放宽门禁或删除marker绕过。

原`require(not STATE.exists())`仍应阻止重复创建；下一轮必须独立批准新attempt与保留旧目录的方案。`legacy-home`、空AVD目录、create marker和全部失败证据继续保留。即使未来批准环境修正，也不自动包含AVD创建、模拟器启动、安装或Host smoke授权。
