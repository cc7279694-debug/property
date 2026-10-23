# M22 Android真机定向补验

交付：**READY_FOR_REVIEW — M22 PHYSICAL DEVICE**。执行准入：**BLOCKED_EXISTING_TARGET_PACKAGE**。M22继续 **BLOCKED / CHANGES REQUIRED**；没有运行产品测试。

来源`6d5ee63b5a4c59642ef435d644188907f76244b8`，分支`codex/m22-experience-accessibility`，2026-10-23。开始时本地/远端HEAD一致、工作区干净。只允许目标Debug包此前不存在时安装；发现既有包即停止，不覆盖、卸载、清理或读取其账本。

## 实际只读准入

首次USB发现只有一台在线目标，后续7条只读命令均明确指定该serial。公开证据使用serial的SHA-256 `e4f0e1d2361b0ef560ff8ac99a067fb8f540594c827d28b6cd21a720e724af92`；原serial仅在被Git忽略的本地控制记录，不公开完整硬件标识。命令中`<USB_TARGET_SHA256:…>`是明确的脱敏表示，不是实际执行时使用的serial。[设备准入结论](m22-physical-20261023-083945/device-gate.json)、[连接状态](m22-physical-20261023-083945/commands/get-state/stdout)。

| 项目 | 实际观察 |
| --- | --- |
| 手机 | Xiaomi，型号`23127PN0CC`；Android16 / API36 / arm64-v8a |
| 资源 | MemAvailable `4703748 KiB`，约4.49GiB；/data可用`174015756 KiB`，约165.95GiB；电量100%，处于充电/满电状态。仅准入采样，不是性能峰值。 |
| 原显示设置 | `font_scale=1.1`，物理1200×2670，density480；未修改，未测Activity的`screenWidthDp`，不能声称满足360dp前提。 |
| **已有App** | **`com.cooper.lithic.debug`已安装**；User0 installed=true，appId/UID10376，versionCode8，`0.1.0-m10-debug`，存在登记数据目录`/data/user/0/com.cooper.lithic.debug`。未读取其中任何业务内容。 |
| Test | `pm path`返回exit1、无路径；目标包查询说明找不到`com.cooper.lithic.debug.test`，未安装。非零退出码原样保留。 |
| 最终门禁 | **既有App与现存数据触发立即停止条件**；本地M22候选身份不能代替手机旧包身份，未读取旧APK字节或证书，相关手机SHA/证书 **NOT_CAPTURED**。 |

[资源与原设置](m22-physical-20261023-083945/commands/identity-resources/stdout)、[实际App路径](m22-physical-20261023-083945/commands/app-path/stdout)、[目标包元数据提取](m22-physical-20261023-083945/commands/app-record/target-package-extract.json)、[Test查询与退出码](m22-physical-20261023-083945/commands/test-path/command.json)。

预检命令在一个只读批次中顺序排队，识别结果前，同批目标包元数据查询已完成；人工确认既有包后没有再发任何ADB命令。没有执行安装、启动或设置写入。`dumpsys package`响应附带非目标包查询元数据，完整原始响应只保留在被Git忽略的本地取证目录；提交的JSON仅白名单提取目标包身份，**不是原始dumpsys文本**。原响应长度/SHA仍保留在[命令记录](m22-physical-20261023-083945/commands/app-record/command.json)，没有把过滤内容冒称原始输出。其他归档命令均带时间、退出码、原始字节数及SHA；没有采集个人屏幕、相册、账号、通知历史或账本内容。

## 本地候选身份与测试结果

本轮实际重算两个本地APK长度/SHA，与原候选一致；没有重建、替换或安装。原成功验签与Manifest证据按完全相同的APK字节继承，本轮未重新运行SDK验签，不将其称为手机安装身份。三个当前测试源文件与[既有源码/DEX索引](m22-supplement/checks/final-artifacts.json)一致；Runner仍为`com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner`、plain Application。[本地实物索引](m22-physical-20261023-083945/local-artifacts.json)、[原验签身份](m22-clean-20261023-1523/artifacts.json)。

| 本地候选 | 字节数 / SHA-256 |
| --- | --- |
| App | 13822236 / `328e3dc2fbe4c2a503fc81f2750cd68a62e5bf2b2bf624620b21d63b3f9bf213` |
| Test | 3764680 / `ce5f5d15abe3c6549f7b23777f2d11ea822a6d4a0417dda165dbfa16e3d6a614` |

| 方法/证据 | 本轮状态 |
| --- | --- |
| T06 `CompleteJourneyTest#runtime_offline_journey` | **NOT_RUN**；未请求用户切换网络、未做离线路由核验；历史三次FAIL不改。 |
| T03 `LongHistoryPerformanceTest#full_twenty_year_dataset_reads_exact_global_facts_and_remains_responsive` | **NOT_RUN**；未准备1000/1000对象、20000/10000事实；实际Room计数、120秒计算预算、耗时/内存/响应均未测。 |
| `M22AccessibilityTest#system200_real_ime_keeps_production_form_input_and_save_reachable` | **NOT_RUN**；未设font2.0、未打开IME、未改density；没有360dp或系统200%操作的新证据。 |
| 截图、JUnit、运行日志、性能记录 | **NOT_CAPTURED / NOT_RUN**；没有启动任何测试页面或采集个人屏幕。 |

## 保护、结束及下一步边界

ADB发现1次＋定向只读7次；App/Test安装、Instrumentation、App启动、设置修改、App数据写入、卸载/清数据、其他设备ADB均 **0**。原字体保持1.1，网络未修改；没有需要恢复的本轮设置。没有打开任何合成Owner或业务generation，Owner关闭 **N/A（未打开）**。未修改、停止或清理手机既有App；未操作任何AVD或H2/C2/M10保留现场。原已安装包的实际账本内容与字节完整性未检查，不从“只读准入”推断其历史数据全部完好。

此前137 JVM PASS、36 Android PASS、最新36 PASS / 1 FAIL / 2 NOT_RUN、T06历史三次FAIL及所有旧UNKNOWN/NOT_RUN/NOTES保持；M10延期、M13-T06延期、M20平台FAIL/收口UNKNOWN及其余门禁不变。无产品/测试代码、Room5/backup4、算法或依赖变化；没有重跑已通过套件。

本轮唯一阻塞是**用户手机已有目标Debug包，不能按当前授权覆盖或清理**。提交推送后停止等待独立审查；不自行改包名、卸载旧App、重建APK、另找设备或执行恢复，不冻结M22、不合并main、不进入M23。
