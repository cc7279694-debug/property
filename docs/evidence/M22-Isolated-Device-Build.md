# M22 独立真机测试包准备

**READY_FOR_REVIEW — M22 ISOLATED DEVICE BUILD**。仅桌面构建/静态核验；M22产品仍 **BLOCKED / CHANGES REQUIRED**，未获得手机安装或运行许可。

来源`99bce1160f608d8b785da2d18614c0e8a342d49f`，分支`codex/m22-experience-accessibility`，2026-10-23；起点本地/远端一致、工作区干净。目标是保留手机旧`com.cooper.lithic.debug`及数据，准备不同安装身份；不修改业务、测试断言、Room5、backup4、算法、依赖或工具链。

## 最小实现与构建

唯一构建代码改动为`app/build.gradle.kts`：显式`-Pm22DeviceVerification=true`时，Debug后缀改为`.m22verify.debug`；必须同时`-Pm13Isolated=true`，拒绝任何`m12T04ProcessDeath`属性及非true/false开关值。namespace仍为`com.cooper.lithic`。只在开关开启时使用`app/build/m22-device-verification`产物目录，不覆盖普通Debug App/Test。未增加flavor、模块、依赖或测试框架。

实际执行`-Pm22DeviceVerification=true -Pm13Isolated=true :app:assembleDebug :app:assembleDebugAndroidTest`，复用锁定JDK17/Gradle8.14.3/AGP8.13.2/Build Tools36.0.0，offline、单worker、1GiB Gradle堆，未运行clean或下载。**Gradle exit0，543.578秒（9分03秒）**；App/Test均成功打包。[构建stdout](m22-isolated-device-build-20261023-091602/commands/isolated-build/stdout)、[stderr](m22-isolated-device-build-20261023-091602/commands/isolated-build/stderr)、[命令/时间/退出码/原字节SHA](m22-isolated-device-build-20261023-091602/commands/isolated-build/command.json)。旧SDK XML版本提示和编译warning保留，未升级工具。

构建完成并保存原始输出/receipt后，外层Python打印日志时遇到GBK编码错误，**外层exit1保留**；Gradle实际exit0不改，未重复构建。[外层错误分层记录](m22-isolated-device-build-20261023-091602/capture-wrapper-result.json)。

不带开关时实际生成普通Debug/Release合并Manifest并读取Gradle配置：仍为`com.cooper.lithic.debug` / `com.cooper.lithic`，原默认Runner不变，原Debug签名文件已存在；未生成密钥或构建Release APK。[默认配置执行](m22-isolated-device-build-20261023-091602/commands/default-manifests/command.json)、[实际配置输出](m22-isolated-device-build-20261023-091602/commands/default-manifests/stdout)、[仅打印配置的临时脚本原件](m22-isolated-device-build-20261023-091602/identity-model.gradle)。三项配置拒绝检查均按预期exit1，不是业务JUnit失败或新增覆盖：[配置门禁结果](m22-isolated-device-build-20261023-091602/configuration-guards.json)。

## 最终APK与隔离核验

| 实物（仅本地，未安装，不入Git） | 字节数 / SHA-256 |
| --- | --- |
| `app/build/m22-device-verification/outputs/apk/debug/app-debug.apk` | **13822252** / `b749b771e4f6f9cebcb7355c95834b7ae2eb4ba4f7528536a92bf9e09eefb2d3` |
| `app/build/m22-device-verification/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | **3209049** / `c3791305aed28899f8df98dc7c89f27c986c022b84a0e528c68c59ebf9f2037f` |

新SHA均不同于旧候选，未冒称相同字节；普通Debug原App `328e…f213`和Test `ce5f…a614`实际重算仍不变。[完整APK/DEX/源码字节核验](m22-isolated-device-build-20261023-091602/artifact-audit.json)、[构建前普通产物](m22-isolated-device-build-20261023-091602/ordinary-artifacts-before.json)。

| 项目 | 桌面实物结果 |
| --- | --- |
| 包身份 | App **`com.cooper.lithic.m22verify.debug`**；Test **`com.cooper.lithic.m22verify.debug.test`**；App code17 / `0.1.0-m22-debug`、min26/target36。 |
| Runner/target | 唯一Instrumentation为`com.cooper.lithic.app.M13IsolatedRunner`，targetPackage严格为新App。Runner原源码强制plain Application，不初始化正常账本/提醒Runtime。 |
| 组件与Provider | 类名仍是原namespace下的完整类名；例如新包中的`com.cooper.lithic.app.MainActivity`，不能误写成新包下的`.app.MainActivity`。唯一Provider authority为 **`com.cooper.lithic.m22verify.debug.androidx-startup`**，exported=false；无sharedUserId。14项非系统组件均在App/Test DEX中存在。 |
| 权限/备份 | 动态receiver signature权限使用新包前缀；其余App权限与普通Debug一致：POST_NOTIFICATIONS、WAKE_LOCK、ACCESS_NETWORK_STATE、RECEIVE_BOOT_COMPLETED；无INTERNET/广泛存储权限。Test仅REORDER_TASKS。allowBackup=false，完整备份排除规则保持；WorkManager自动Initializer仍被移除。 |
| 签名 | 两包实际apksigner exit0、v2校验成功，证书均为原Debug **`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`**。未生成密钥，未输出密码。 |
| DEX/ABI | App13份DEX、Test11份DEX；三个目标方法和Runner.newApplication均存在；组件与目标类核验仅静态，不冒充执行。App保留既有AndroidX/DataStore的四ABI，未引入M10/JNI库；真实arm64加载仍NOT_RUN。 |
| 私有数据 | 指定三方法使用新target Context的noBackupFilesDir/filesDir/cacheDir；DataGenerationStore校验根不可越界。没有跨包Context、旧debug绝对目录或旧Provider访问。预期为新包私有沙箱；**实际安装UID和运行路径NOT_RUN**，同证书不能证明或要求共享UID。 |

完整合并Manifest：[新App](m22-isolated-device-build-20261023-091602/manifests/isolated-app.xml)、[新Test](m22-isolated-device-build-20261023-091602/manifests/isolated-test.xml)、[默认Debug](m22-isolated-device-build-20261023-091602/manifests/ordinary-debug.xml)、[默认Release](m22-isolated-device-build-20261023-091602/manifests/ordinary-release.xml)；[核验摘要](m22-isolated-device-build-20261023-091602/manifest-audit.json)。实际打包二进制Manifest及身份亦由aapt核对：[App](m22-isolated-device-build-20261023-091602/commands/app-manifest/stdout)、[Test](m22-isolated-device-build-20261023-091602/commands/test-manifest/stdout)；[App验签](m22-isolated-device-build-20261023-091602/commands/app-signature/stdout)、[Test验签](m22-isolated-device-build-20261023-091602/commands/test-signature/stdout)。

## 手机兼容性与未运行边界

此前手机观察为API36/arm64、font1.1、物理1200×2670/density480；本轮未重新连接，不称当前实测。名义宽度换算约400dp，**不是Activity实际screenWidthDp证明**。原IME测试仍要求真实font_scale=2.0且screenWidthDp在359～361，真实键盘可见、保存可达、隐藏后输入保留；不满足则NOT_RUN，不改手机density或断言。T06真实离线路由仍需另行授权及用户配合；T03固定规模、seed和120秒计算预算原样保持。三份测试源的实际长度/SHA与前轮索引完全一致。

App标签/图标仍与旧版相同，未来不得凭图标判断目标。完整Test仍含历史M12/M20/SAF测试中的固定旧身份或外部路径；它们未被修改，也**不适用于本验证包许可**。后续只允许经明确批准的三个M22单方法，不能整类/整包执行；Runner组件将是`com.cooper.lithic.m22verify.debug.test/com.cooper.lithic.app.M13IsolatedRunner`。本轮没有生成或执行任何设备命令。

本轮ADB、手机/模拟器操作、安装/卸载、Instrumentation、系统设置修改、个人账本读取均 **0**；137 JVM及36 Android既有PASS未重跑，T06/T03/真实200%IME仍NOT_RUN，Lint未重跑。旧FAIL/UNKNOWN/NOT_RUN/NOTES和M10/M13-T06/M20/Q03/Q04/G1/G2/G3状态不变。只修改构建身份开关及必要文档/字节保留规则；无业务或测试源码、schema/backup变更。

静态隔离核验未发现阻塞；**构建就绪不代表个人手机安装或运行许可**。提交推送后停止等待ChatGPT独立审查和用户进一步明确授权；M22不冻结、不合并main、不进入M23。
