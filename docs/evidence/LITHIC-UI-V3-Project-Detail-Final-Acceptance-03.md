# LITHIC UI-V3 · 项目详情最终 Android 验收 03

固定起点 `5fcbc4e7ff0c9050c718fa7d86a588e3667fcd35`，生产源码 `0f6fed74c03a82ee5a90f5e209e6ea6fb3989a25`。本轮测试提交 `70aed39e8708fee14c9a6fa2b6639acb33d3d0f8`，App 树 `1d9d637bd781b2209f39476c9ded45eb7ea67450`。

[上一轮原始失败与截图](LITHIC-UI-V3-Project-Detail-Repair-02.md) · [已确认项目详情画板](../design/lithic-ui-v3-detail-visual-refinement-02/boards/renders/project-white.png)

## 改动与验收判据

- `V3ProjectDetailAndroidAcceptanceTest.kt` 将语义聚合值 `hasVisualOverflow` 保留在 Android 原始诊断。只有「仅报宽度、重建段落等于最大宽度且大于先前节点尺寸」这一已取证的模式可由更具体的几何判据裁决，其他未解释的溢出仍失败。
- 新判据采用实际 Compose 节点和文字布局两者较小的可用宽高。保留行宽和行底诊断，逐行检查字宽、行左右、行顶底和偏移连续性，并逐字验证尺寸与范围，核对最后可见索引、最大行数、纵向溢出与无省略。实际未裁切边界必须位于报告的可见边界、父布局及滚动视口内。
- 新增长标题「合成旅行摄影设备与备用镜头套装」走真实 MainActivity、首页及项目详情。正常字体和真实系统 200% 必须各自换行并完整显示。测试核验类别、状态、持有天数和相互边界，实际纵向滑动后检查成本卡、金额及滚动尾部可达，并核对固定按钮与视口分离且滚动前后位置恒定。
- `HomeAndroidAcceptanceFixture.kt` 只允许本轮长标题方法用参数替换原横图项目的名称。首页及原画廊仍取默认合成名称，账本数量、事件、封面、Owner、只读快照及清理合同没有变化。业务回归和所有原始历史日志保留。

### 上轮误报

Compose 1.9.5 的文字语义读取会用上次最大约束重建 `MultiParagraph`，同时保留旧节点的实际测量尺寸。旧判据直接拒绝该语义接口的聚合标志。100% 节点与行宽都是 288px，200% 都是 432px，重建段落宽均为 656px，导致两次都报告宽溢出。两种字号都为 6/6 可见，且无高度溢出、省略或实际裁切。[上轮原始测量](lithic-ui-v3-project-detail-repair-02/android/title-layout.jsonl) · [依赖实现](lithic-ui-v3-project-detail-repair-02/desktop/compose-title-layout-bytecode.txt)

## 结果

### 桌面与身份

- 生产 Kotlin 和 AndroidTest Kotlin 实际编译，测试 APK 实际打包。定向 JVM 7 类共 **51 PASS、0 FAIL、0 SKIP**。Debug Lint **0 错误、20 警告**。离线 Gradle 71 个任务实际执行，退出 0。[构建日志](lithic-ui-v3-project-detail-final-acceptance-03/desktop/desktop-build.log) · [参数](lithic-ui-v3-project-detail-final-acceptance-03/desktop/desktop-arguments.json) · [JVM 原始 XML](lithic-ui-v3-project-detail-final-acceptance-03/desktop/jvm/) · [Lint 原文](lithic-ui-v3-project-detail-final-acceptance-03/desktop/lint-results-debug.txt)
- App 复用原 `com.cooper.lithic.debug` 21,224,212 字节，SHA-256 `8f638588cb738f8eec949bfb758b72285f29056fa2747a6601ad077530fcc9c5`。本轮 Test 为 `com.cooper.lithic.debug.test` 4,089,367 字节，SHA-256 `558caf5afbda28cd28ebf578be726f4ad6cd9e616917f19424f077c3e1b63b81`。两者原 Debug 证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。实际 Test DEX 和编译类与新几何门禁对应，保留原诊断，并包含独立长标题方法。[包审计](lithic-ui-v3-project-detail-final-acceptance-03/desktop/apk-audit.json) · [源码与 APK 绑定](lithic-ui-v3-project-detail-final-acceptance-03/desktop/apk-build-inputs.json)
- 离线执行清单固定了原 11 次、长标题两次、指定首页 43 项，以及项目专属套件的原 43 次（42 个不同方法，画廊按系统字号执行两次）。项目套件有 11 次与定向复验重合，在同批次仅运行一次，再补其余 32 次。首页套件按原清单指定 7 个封面方法，项目套件保留全部 9 个。两套结果单列，不跨套件合计。[选择器清单](lithic-ui-v3-project-detail-final-acceptance-03/desktop/regression-methods.json) · [离线门禁回放](lithic-ui-v3-project-detail-final-acceptance-03/desktop/runner-offline-check.txt)

### 单次 Android 启动与阻断

主机启动瞬时可用内存 **5.398 GiB**，超过 5 GiB 门槛。只读预热耗时 32.828 秒。复用原 Repair05 隔离 AVD、userdata 及 API 36 x86_64 镜像，只冷启动一次。生成配置为 1536 MiB、临时四核，端侧确认 CPU 0–3 和 1,502,716 kB 的 MemTotal。独立 SDK ADB 5038 自动注册唯一 `emulator-17640`，身份检测通过且 `sys.boot_completed=1`。[启动命令](lithic-ui-v3-project-detail-final-acceptance-03/android/launch.txt) · [只读预热](lithic-ui-v3-project-detail-final-acceptance-03/android/boot-read-warmup.json) · [设备身份](lithic-ui-v3-project-detail-final-acceptance-03/android/admission-result.json)

健康门禁在安装和测试之前捕获新发生的系统进程启动 ANR。距启动约 54.5 秒的首项为 `com.android.phone`（PID 1107），随后同一系统包的新进程 PID 1492 也超时，`com.android.traceur`（PID 1568）出现第三项。捕获的三项均报告 `failed to complete startup`。身份通过后仍不能进入测试。未再次启动，未请求安装，未启动产品或 instrumentation，未更改系统字体。两个 Phone 主线程样本在类初始化和 ContentProvider 安装路径，系统启动具体根因仍 UNKNOWN。[原始 ANR 事件](lithic-ui-v3-project-detail-final-acceptance-03/android/anr-events.txt) · [健康与取证时间线](lithic-ui-v3-project-detail-final-acceptance-03/android/timeline.jsonl)

### Android 执行清单

本轮所有组端侧执行数均为 **0**。前置环境为 **BLOCKED**，下表每个用例为 **NOT_RUN**。上一轮 9 PASS／2 FAIL 原样保留。

| 原 11 次定向复验 | 本轮 Android |
| --- | --- |
| MainActivity 画廊，真实系统 100% 字体 | NOT_RUN |
| MainActivity 画廊，真实系统 200% 字体 | NOT_RUN |
| 关联折抵记录、备注与受保护的纠错 | NOT_RUN |
| 三条金额记录可达且无「全部」 | NOT_RUN |
| 大字财务数值与独立固定操作 | NOT_RUN |
| 四条记录同列表展开、收起及项目／generation 重置 | NOT_RUN |
| 未开始项目、未知日均与负净投入 | NOT_RUN |
| 回填金额并从详情、清单、首页读取 | NOT_RUN |
| 项目及金额连点只生成各一条记录 | NOT_RUN |
| 编辑名称和开始日期且保留旧金额历史 | NOT_RUN |
| 历史更正与真实收回，保留原事件身份 | NOT_RUN |

| 后续组 | 计划 | 实跑 | 结果 |
| --- | ---: | ---: | --- |
| MainActivity 长标题，360dp 系统 100% 和 200% | 2 | 0 | NOT_RUN |
| HomeAndroidAcceptanceTest | 3 | 0 | NOT_RUN |
| HomeOverviewUiTest | 6 | 0 | NOT_RUN |
| P2HomeArchiveContractTest | 6 | 0 | NOT_RUN |
| ReportRepositoryTest | 19 | 0 | NOT_RUN |
| 首页历史范围 CoverPreviewsTest | 7 | 0 | NOT_RUN |
| ActivityNavigationTest | 2 | 0 | NOT_RUN |
| 指定首页套件总计 | 43 | 0 | NOT_RUN |
| 项目详情专属套件（含上述 11 次定向复验，其余 32 次） | 43 | 0 | NOT_RUN |

### 截图及测量

上一轮同一生产 App、同一原生 MainActivity、360dp 的真实截图可用于视觉对照。正常字号的封面与新设计保持 4:3 Fit、三张财务卡及固定操作。测试使用合成图案及金额，与画板中的相机照片、样例金额和记录数不同。

- [纯白](lithic-ui-v3-project-detail-repair-02/android/project-detail-pure-white.png) · [岩白雾蓝](lithic-ui-v3-project-detail-repair-02/android/project-detail-stone-mist.png) · [深色黑灰](lithic-ui-v3-project-detail-repair-02/android/project-detail-charcoal.png) · [竖图 Fit](lithic-ui-v3-project-detail-repair-02/android/project-detail-portrait.png)
- 系统 200%：[标题](lithic-ui-v3-project-detail-repair-02/android/project-detail-font-200.png) · [三张财务卡和固定操作](lithic-ui-v3-project-detail-repair-02/android/project-detail-font-200-money.png)

本轮新增两张长标题图 `project-detail-long-title-100.png`、`project-detail-long-title-200.png` 均未生成。新短标题与长标题的真实测量、换行、节点边界、滚动和截图对照尚无结果。上轮短标题数值仅作为对照，不推定本轮新判据结果。

### 收口与数据边界

设备控制器在健康门禁退出 2，外层工作流退出 1，记录为 `ENVIRONMENT_BLOCKED`。按已核对归属的控制台请求关闭，模拟器未在 12 秒内退出。收口流程随后仅结束本轮确认归属的 QEMU 子进程 PID 28216，模拟器包装进程退出 1，残留子进程与监听均为空。5038 已关闭，原 5037 仍为同一 PID 15576、同一创建时间。没有 ADB 请求发送至 5037。[控制器原始终态](lithic-ui-v3-project-detail-final-acceptance-03/android/controller-result.json) · [模拟器收口](lithic-ui-v3-project-detail-final-acceptance-03/android/emulator-cleanup.json) · [ADB 身份收口](lithic-ui-v3-project-detail-final-acceptance-03/android/adb-closeout.json)

基底 userdata 文件身份及 SHA 保持一致，开机后工作覆盖层的身份相同、SHA 变化。未执行 wipe-data、安装、设置修改或真实账本操作。三条 ANR 事件保留，两个 Phone 进程栈按 PID 和事件时间从系统 Dropbox 提取并核验。Traceur 仅确认系统事件，独立栈未取得。直接读取三个 ANR 文件均被拒。全量系统日志留在本机运行目录。[限定栈来源](lithic-ui-v3-project-detail-final-acceptance-03/android/anr-diagnostics.json) · [userdata 退出回执](lithic-ui-v3-project-detail-final-acceptance-03/android/userdata-after-shutdown.json)

生产源树、Room、项目事件、金额算法、照片读取、备份、正式包名签名及历史 APK 与固定基线无差异。没有合并或发布。端侧测试尚未执行，**ENVIRONMENT_BLOCKED**，等待 ChatGPT 独立审查。
