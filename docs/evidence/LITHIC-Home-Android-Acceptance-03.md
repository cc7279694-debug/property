# LITHIC 首页 Android 验收 03

**LITHIC_HOME_ANDROID_ACCEPTANCE_03 — READY_FOR_REVIEW_WITH_FAILURES**

2026-11-13，Asia/Shanghai。执行分支 `codex/lithic-home-android-acceptance-03` 从已核对本地和远端一致的 `f05a15dc594289e9ef1a4ffbb21cd520f3e7d042` 建立，开始前工作区干净。使用该提交的 `admit_owned_emulator()`。App 树 `df364af6a333c129c3e14d021c45d11331f90a7b`。

## 主机、包身份与单次启动

- Windows 主机启动前可用 RAM 4.066 GiB，C/E 盘可用约 17.79／29.78 GiB。预检查确认 5038 可绑定，SDK ADB 37.0.1（build 15733141）以 PID 17480 在回环监听。独立用户目录、空额外密钥目录、关闭 mDNS 自动连接和不匹配 USB 标记均启用。CLI 和只读主机请求均确认初始设备列表为空。[启动前记录](lithic-home-android-acceptance-03/prelaunch.json) · [服务身份](lithic-home-android-acceptance-03/controller-adb-identity.json)
- 复用已审 Debug App/Test，重新读取两包大小、哈希、Manifest 和实际签名。两包唯一签名者的证书 SHA-256 均为 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，签名验证退出码均为 0。App 正常入口为 `LithicApplication`／`MainActivity`，code17／`0.1.0-m22-debug`。Test 使用唯一 `androidx.test.runner.AndroidJUnitRunner`，目标为下表 App。桌面审计首次指定了不存在的 JDK 路径，随后有一次检查器的 Manifest 命名空间匹配失败。已用实际 JDK 及精确组件字段复核，两份原始文件与运行目录副本哈希一致。未重新构建或重签。[APK 指纹](lithic-home-android-acceptance-03/apk-audit.json) · [审计命令与结果](lithic-home-android-acceptance-03/apk-audit-commands.txt)
- 仅启动 `Lithic_Home_Acceptance_01_API36` 一次。AVD 配置为 API 36 AOSP x86_64、720×1648、320 dpi，Emulator 37.1.11.0。传递 `ANDROID_ADB_SERVER_PORT=5038` 前清除继承的地址与端口覆盖，使用 `-lowram -memory 1536 -cores 2 -ports 17640,17641`。本次 Run ID `f0f53918-495e-4b06-8e2a-39afbfb64c64`。[完整参数](lithic-home-android-acceptance-03/launch.txt) · [准入合同](lithic-home-android-acceptance-03/admission-claim.json)

| APK | 包名 | 字节 | SHA-256 |
| --- | --- | ---: | --- |
| App | `com.cooper.lithic.debug` | 20,541,670 | `6706499e31ef9dbff1ef060dc6c604cfb7b0fb5ecbaa8082dd6179773c0853d9` |
| Test | `com.cooper.lithic.debug.test` | 4,034,867 | `7c5a7bf5b4ae232666379c8317901bd4abb913978fe62fe52282f28dbbffb254` |

## 单连接、设备准入与 Android 健康

- **PASS，单连接准入。** 首个只读列表为空（0.391 秒）。唯一 `emulator-17640` 于 9.422 秒首次显示 `offline`、75.531 秒首次显示 `device`。`sys.boot_completed=1` 于约 165 秒取得，之后 Shell 探针及实际 AVD、QEMU 标识、API 36、x86_64 校验通过，约 165.27 秒复核唯一传输。无手动连接请求，未观察到 `127.0.0.1:17641` 别名或第二传输。17640 和 17641 均由本次 QEMU PID 5432 持有，父进程为本次 wrapper PID 28844。[完整准入观察](lithic-home-android-acceptance-03/admission-commands.jsonl) · [最终准入结果](lithic-home-android-acceptance-03/admission-result.json) · [端口归属](lithic-home-android-acceptance-03/host-emulator-listeners.json)
- **PASS，安装前健康门禁。** 新生成 `hardware-qemu.ini` 实测 `hw.ramSize=1536`、`hw.cpu.ncore=2`。guest `/proc/meminfo` 的 MemTotal 为 1,503,188 kB，MemAvailable 两次采样为 614,924 和 467,156 kB，数据分区可用 5,604,108 KiB。`wm` 返回物理 720×1648、320 dpi。SystemUI PID 1087、system_server PID 712 在有限观察前后一致，Activity 服务可用，限定 `am_anr` 查询无事件。此检查只覆盖安装前的观察窗。[实际硬件](lithic-home-android-acceptance-03/effective-avd.json) · [guest 内存](lithic-home-android-acceptance-03/effective-guest-memory.txt) · [存储与内存](lithic-home-android-acceptance-03/guest-health.txt) · [ANR 查询](lithic-home-android-acceptance-03/health-admission-anr.txt)

## 本轮离线复验

固定准入脚本的 22 项 Python 测试 PASS，退出码 0。运行控制器做了 Python 语法检查。[离线原始输出](lithic-home-android-acceptance-03/offline-tests.txt)

## 安装与测试

**PASS，安装及安装后身份。** 只在已准入模拟器的 User 0 核对目标 App、Test、正式包和历史 Preview 的预存包状态。目标两包各安装一次，退出码均为 0、返回 `Success`。`pm path` 指向各自唯一 base.apk，guest `sha256sum` 与上表已验签实物完全一致，实际 instrumentation 唯一绑定原 Test Runner 和目标 App。[App 安装](lithic-home-android-acceptance-03/install-app.txt) · [Test 安装](lithic-home-android-acceptance-03/install-test.txt) · [安装后 App 字节](lithic-home-android-acceptance-03/installed-hash-com.cooper.lithic.debug.txt) · [安装后 Test 字节](lithic-home-android-acceptance-03/installed-hash-com.cooper.lithic.debug.test.txt) · [Runner](lithic-home-android-acceptance-03/runner.txt)

## 原生画面与参考图对照

截图来自已安装 Debug App 的真实 `MainActivity`，由 Android UiAutomation 捕获。原图为 720×1648，截图收集校验 PNG、尺寸、CRC、SHA-256、尝试 ID 与方法 ID。正常字号组验证实际 Activity 360dp／1.0 和系统字号读回，并记录了本组专用夹具的清理。[用户批准的参考图](lithic-home-card-ui/approved-reference.png) · [截图指纹及方法 ID](lithic-home-android-acceptance-03/screenshots.jsonl)

| 主题 | 项目 | 订阅 |
| --- | --- | --- |
| 纯白 | [原图](lithic-home-android-acceptance-03/projects-pure-white.png) | [原图](lithic-home-android-acceptance-03/subscriptions-pure-white.png) |
| 岩白雾蓝 | [原图](lithic-home-android-acceptance-03/projects-stone-mist.png) | [原图](lithic-home-android-acceptance-03/subscriptions-stone-mist.png) |
| 深色黑灰 | [原图](lithic-home-android-acceptance-03/projects-charcoal.png) | [原图](lithic-home-android-acceptance-03/subscriptions-charcoal.png) |

- 参考图里的品牌栏、左右总览、分段按钮与「全部」、双列、底部「添加」及三导航都可在运行图定位。正常字号的名称、状态、金额、日期和入口在样本中可读，无可见遮挡。运行图的总览较高、卡片至固定添加区的留白较多，品牌栏有已实现的副标题，Android 系统栏也占用高度。金额沿现有格式显示 `¥62480.00`，参考图使用千位逗号。深色主题的选中段和主操作转为浅色，岩白雾蓝的未选中段与标签使用雾蓝表面。
- 两个项目用合成横向相机、纵向背包。组件确认两个 4:3 封面同排分列，两张样本裁切后主体仍完整可辨。订阅中的无封面与读取失败在原图分别呈现日历类型图标与图片警告图标。实际订阅状态为「订阅中」，测试账本的总览为 2 项目／2 订阅、总投入 `¥62480.00`、日均 `¥16.56/天`。参考图的照片、日期、日均和数量属于设计示例。[空账本原图](lithic-home-android-acceptance-03/empty.png) · [正常字号组原始输出](lithic-home-android-acceptance-03/instrument-gallery.txt)

## 真实 200% 字体缺陷

**FAIL，360dp 项目卡片状态文字溢出。** 系统设置和真实 Activity 的 `configuration.fontScale` 均验证为 2.0。总投入、日均成本、固定添加区与导航的检查已走到总览截图。继续滚动到第一张项目卡片时，Compose `TextLayoutResult.hasVisualOverflow` 对「持有中」返回 true，原断言失败。原始输出有 JUnit 完整终结，执行 1 项、失败 1 项。结果来自滚动后的文字布局探针，总览截图停在状态文字进入视口之前。[200% 总览原图](lithic-home-android-acceptance-03/projects-font-200-summary.png) · [完整失败输出](lithic-home-android-acceptance-03/instrument-system-font-200.txt)

**NOT_RUN**，该方法后续的项目卡片完整检查、订阅卡片检查及两张 200% 卡片截图均未走到。本次实际取得 8 张原生截图。当前 `HomeGridCard` 在双列卡片内部以 `FlowRow` 承载类别与状态，后续修复需核对该状态项在实际可用宽度下的测量。本轮只保存证据。三个真实首页方法均报告已验证各自的合成记录和媒体夹具清理。[执行计数与夹具回执](lithic-home-android-acceptance-03/test-summary.jsonl)

## 交互及定向回归

`home_routes_open_and_back` 在真实 MainActivity 通过。覆盖两个「全部」、两个详情、顶部和底部新增菜单、创建表单入口、成本构成、三个底部入口的选中状态和系统返回。组件回归确认搜索按钮派发正确回调。MainActivity 搜索页完整旅程、真机及 TalkBack 均为 NOT_RUN。真实系统 200% 只跑 360dp，其他宽度未做真实运行。[导航原始输出](lithic-home-android-acceptance-03/instrument-routes.txt)

`HomeOverviewUiTest` 执行 6 项，4 项 FAIL。三项期望裸金额 `¥0.00` 或 `¥0.25`，实际节点含完整单位，语义分别为 `¥0.00/天` 和 `¥0.25/天`。一项对已找到的新增面板选项调用 `performScrollTo()`，该节点无可滚动祖先。它们是可定位的既有测试预期或操作不匹配。其后置断言保持未覆盖，原结果和断言保留。[原始输出](lithic-home-android-acceptance-03/instrument-home-overview.txt)

`P2HomeArchiveContractTest` 6 项 PASS，覆盖受控 Compose 状态，包括历史、空账本、旧快照与读取失败。`ReportRepositoryTest` 19 项 PASS，在 Android Room 测试库验证只累计项目 OUTLAY、包含已结束项目及以旧换新非现金折抵、排除 RECOVERY 和未来投入、订阅付款变化不影响总投入，以及分页和事务内一致性。[首页状态输出](lithic-home-android-acceptance-03/instrument-home-archive.txt) · [Room 回归输出](lithic-home-android-acceptance-03/instrument-report-room.txt)

`CoverPreviewsTest` 7 项 PASS，`ActivityNavigationTest` 2 项 PASS。[封面输出](lithic-home-android-acceptance-03/instrument-cover.txt) · [Activity 导航输出](lithic-home-android-acceptance-03/instrument-activity-navigation.txt)

合计 **43 项 Android JUnit，38 PASS、5 FAIL**。八个执行组都取得完整 JUnit 输出。未走到的断言与两张 200% 卡片截图为 NOT_RUN。真实设备、个人账本、升级、真实备份恢复为 NOT_RUN。[逐组计数及执行 ID](lithic-home-android-acceptance-03/test-summary.jsonl)

## 安全退出

- **PASS，恢复和夹具收口。** 原 `font_scale` 是 1.0，正常与 200% 阶段分别读回 1.0 和 2.0。200% 断言失败后执行 `finally`，读回原值 1.0。三个 MainActivity 方法均输出 `fixture-cleaned`，对应本次各自 UUID。各组之后 SystemUI 和 system_server PID 保持一致，限定 `am_anr` 查询无事件。[原字体](lithic-home-android-acceptance-03/font-original.txt) · [2.0 读回](lithic-home-android-acceptance-03/font-200-read.txt) · [恢复读回](lithic-home-android-acceptance-03/font-restore-read.txt)
- **PASS，退出与端口保护。** 核对本次 QEMU 监听归属及控制台 AVD 名称后，正常关闭。wrapper PID 28844 退出码 0，QEMU 无残留子进程，17640／17641 无监听。核验身份后关闭本次 SDK 5038，`kill-server` 退出码 0，PID 17480 与监听均消失。5037 的 PID 16276、版本 36.0.1 及服务身份前后一致。[模拟器关闭](lithic-home-android-acceptance-03/emulator-cleanup.json) · [ADB 服务生命周期](lithic-home-android-acceptance-03/receipt.json)
- 运行控制器在完成测试和恢复后以 3 表示 `READY_FOR_REVIEW_WITH_FAILURES`。外层脚本将运行控制器的所有非零退出统一记为 `BLOCKED`，本次实际退出码为 1。结合完整 JUnit 输出和安全退出记录，本轮属于已取得产品失败证据的验收。发布的模拟器日志隐藏主机网络地址与 ADB 公钥文本，未脱敏日志和审计过程原件留在本机 `E:/CODEX/lithic-home-android-acceptance-03-local/` 与相邻 `lithic-home-android-acceptance-03-apk-audit.txt`。[控制器回执](lithic-home-android-acceptance-03/controller-result.json) · [模拟器启动日志](lithic-home-android-acceptance-03/emulator.stdout.redacted.txt)

只提交状态、报告与运行证据。生产、AndroidTest、准入模块、Gradle、Manifest、签名和包身份相对固定基线零差异。等待 ChatGPT 独立审查。
