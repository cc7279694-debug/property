# UI-C2 C3-1 原生视觉采集执行记录

日期：2026-10-27（Asia/Shanghai）

结论：**C3-1 BLOCKED — CAPTURE_INCOMPLETE / APP_VISUAL_PENDING**

## 实际停止点

来源及执行前本地/远端 HEAD 均为 `40ee800662a4571cd8670437e48c4b31152336f4`，分支 `codex/lithic-v1-product-completion`，开始时工作区干净。依据用户本次明确授权，完成新视觉 App/Test 各一次首次安装及设备字节核对；只启动 A 方法一次。

A 的桌面命令于 `2026-10-27T03:06:13.010466Z` 开始，120.063秒后触及本轮桌面客户端120秒等待边界。原始输出379字节，仅有该方法的 `INSTRUMENTATION_STATUS_CODE: 1` 开始记录；stderr为0字节，没有 JUnit 完成、失败堆栈、最终 Instrumentation code 或截图 UUID 握手。**A 为超时/UNKNOWN，不能算断言 FAIL 或 PASS；外层退出码 NOT_CAPTURED（JSON null）**。立即停止后续方法，没有重试或补拍。

| 方法 | 次数 | 真实结果 | 可提取PNG |
|---|---:|---|---:|
| A_dark_400_confirmed_empty_now | 1 | UNKNOWN — TIMEOUT；JUnit仅开始 | 0 |
| B_dark_400_confirmed_empty_review | 0 | NOT_RUN — A未完成 | 0 |
| C_light_400_synthetic_project_subscription_now | 0 | NOT_RUN | 0 |
| D_light_400_synthetic_history_review | 0 | NOT_RUN | 0 |
| E1_dark_360_local_200percent_now | 0 | NOT_RUN | 0 |
| E2_dark_360_local_200percent_review | 0 | NOT_RUN | 0 |
| F1_device_configuration_empty_now | 0 | NOT_RUN | 0 |
| F2_device_configuration_empty_review | 0 | NOT_RUN | 0 |

**ANDROID_COMPOSE_CAPTURED：NOT_OBTAINED；MAINACTIVITY_NOT_TESTED。** 没有实际原生图可与 UI-C1 作视觉对照，不能登记 VISUAL_ACCEPTED。没有收到合法 UUID，因此没有枚举截图目录、尝试猜测路径或读取 owner/capture/PNG。设备上是否存在未完成的截图文件仍 UNKNOWN。

## 准入与安装实物

- 唯一在线USB目标与此前授权身份一致：小米14，型号 `23127PN0CC`，API36/arm64-v8a，Android用户0；硬件序列号仅保存在本地忽略目录。
- 新两包连同保留记录在安装前均不存在。原历史包只核对安装存在性、版本和UID，没有打开或读取其私有数据。
- 准入时手机 MemAvailable 4185MiB、电量37%；两次 SystemUI 进程/非空焦点窗口观察未出现阻断ANR标记，设备唤醒且无锁屏输入限制。桌面可用内存1.931GiB、E盘65.227GiB，仅做轻量ADB/SDK核验；没有启动Gradle、模拟器或大型渲染器。
- 本地锁定 APK 的长度、完整SHA、apksigner证书与实际Manifest重新核对。实际Test SHA绑定此前已核对的Runner DEX：`newApplication`传普通 `android.app.Application`。App只声明空ComponentActivity，无Main/Launcher、Provider、Service、Receiver、sharedUserId、INTERNET；POST_NOTIFICATIONS实际未授予，本轮未请求或发布。
- 最初只读 `dumpsys window windows` 的焦点字段筛选为空（grep退出1），后用API36实际提供字段的 `window displays` 独立观察。Package dump使用实际 `appId` 字段与 `pm ... uid` 对照，Instrumentation由目标过滤的 `pm list instrumentation` 核对。这些是桌面只读解析适配，没有重新安装、修改测试或回填原始输出。

| 实物 | 独立包名 | 实际UID | 字节数 | 设备及本地SHA-256 |
|---|---|---:|---:|---|
| App | com.cooper.lithic.c2visual.debug | 10435 | 15,732,081 | `14b1d55e30f8db3bf70d693034b4d4ab5989b64d8c7a1cf7d36b1e21cc9a79a6` |
| Test | com.cooper.lithic.c2visual.debug.test | 10437 | 3,475,349 | `18a10d5472aa5bff624ed02da763a4efd873811a2ac2b738497693b3b0b51c76` |

两份设备APK通过各自PackageManager返回的base.apk精确路径执行正常只读SHA及长度核验，前后保持一致；与桌面apksigner核验的相同字节绑定原Debug证书 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。App版本为 code17 / `0.1.0-c2visual-debug`。新UID与原四包UID不同。

安装命令分别为明确USB目标的 `install <锁定App>` 和 `install -t <锁定Test>`，各一次、exit0/Success；没有-r/-d/-g。实际目标过滤的Instrumentation只有 `com.cooper.lithic.c2visual.debug.test/com.cooper.lithic.app.M13IsolatedRunner`，target为新App。

## 结束保护与证据边界

结束只读复核：同Boot/USB；原四包安装身份未变；新两包UID、长度、SHA及未授予通知权限保持。字体1.1、物理density480、主题Night mode auto与开始一致；本轮没有写入任何系统设置或网络配置。手机MemAvailable4991MiB、电量41%、温度33.8℃，焦点非空且未见阻断窗口标记。

F1/F2未运行，实际Activity的screenWidthDp、effectiveFontScale及PNG尺寸均NOT_CAPTURED；上述系统设置读数不替代生产组件布局元数据。

**新App UID10435/PID6206仍存在**；Test UID未见进程。该观察不能判定Instrumentation已完成、线程阻塞位置或Owner/FD已经释放。超时只结束本次桌面ADB客户端，没有向手机发送kill、force-stop、卸载或清理。保留新App/Test及其现场。

锁定源码和Manifest采用plain Application、内存合成事实，不创建业务Owner或默认账本；但A没有返回capture metadata，运行时隔离断言是否全部执行、资源状态均不得据此补判PASS。没有打开任何旧Room/WorkManager、generation、图片、备份或账本，也没有取整机截图、系统日志或无关私人屏幕。未进行新的环境修复或根因诊断；超时具体位置仍 **UNKNOWN**。

本轮产品/测试/构建配置源码、Room5、backup4、业务算法、Owner/恢复/Nav3/媒体/提醒协议均未修改；Gradle/构建/JVM/历史套件、T06/T03/IME和其他设备操作均0。Preview5/code21及全部历史M10/M20/M22/M13/G1～G4的FAIL、UNKNOWN、NOT_RUN、NOTES不变。后续设备操作需独立审查与新的明确授权。

## 必要原始入口

- [汇总及八方法状态](ui-c2-c3-capture-20261027-025415/result.json)
- [准入及原始设置](ui-c2-c3-capture-20261027-025415/device-admission.json)
- [桌面实际SHA/签名/Manifest和同字节Runner绑定](ui-c2-c3-capture-20261027-025415/desktop-apk-audit.json)
- [安装、设备APK回读、UID、旧身份与设置的原命令/退出码/输出](ui-c2-c3-capture-20261027-025415/device-command-records.json)
- A：[精确命令/时间/120秒边界/退出状态及原字节SHA](ui-c2-c3-capture-20261027-025415/A/command.json) · [原始stdout](ui-c2-c3-capture-20261027-025415/A/stdout) · [原始stderr（0字节）](ui-c2-c3-capture-20261027-025415/A/stderr)
- [结束身份、设置及目标UID进程检查](ui-c2-c3-capture-20261027-025415/end-protection-check.json)
- [此前C3-0预检](UI-C2-C3-Native-Visual-Preflight.md) · [限定提取手册](UI-C2-C3-Capture-Runbook.md)

原始本地采集记录保留于忽略目录 `build/c3-capture-local-20261027-025415/`；没有PNG被提取或提交。A及其他未脱敏命令输出的UTF-8字符串保留原CR/LF，并逐条核验原字节长度与SHA。四份新包package dump仅公开安装身份/版本/自身dataDir/POST权限摘录，省去无关查询可见包清单；各条明确标记派生脱敏文本，分别记录公开字节长度/SHA与原始回执长度/SHA，全文留在本地。只归档必要报告、元数据及A原始输出，硬件序列号、当前私人焦点窗口原文和无关可见包清单不入Git。
