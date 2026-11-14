# LITHIC UI-V3 · 项目详情实施 01

**PARTIAL — DESKTOP PASS / ANDROID ENVIRONMENT_BLOCKED**

设计基线 `03b4dc7bab91f0fed2a2723fcffaf87464153ce9`。实施分支 `codex/lithic-ui-v3-project-detail-implement-01`，构建源码 `0f6fed74c03a82ee5a90f5e209e6ea6fb3989a25`，`app` 树 `45effd76597fd68bca2eb515115e496f8f44f584`。评审以固定设计稿为视觉依据。[设计收口](../design/lithic-ui-v3-detail-visual-refinement-02/DESIGN_FREEZE_03.md) · [纯白主图](../design/lithic-ui-v3-detail-visual-refinement-02/boards/renders/project-white.png) · [三主题](../design/lithic-ui-v3-detail-visual-refinement-02/boards/renders/project-themes.png) · [金额记录](../design/lithic-ui-v3-detail-visual-refinement-02/boards/renders/history.png) · [设计稿模拟 200%](../design/lithic-ui-v3-detail-visual-refinement-02/boards/renders/font-200.png) · [实际文件清单](lithic-ui-v3-project-detail-implement-01/changed-files.txt)

## 生产改动与视觉对照

| 文件 | 旧界面与本轮变化 |
| --- | --- |
| `feature/project/ProjectDetailScreen.kt` | 照片由标题后的宽框移至顶部居中 4:3 框。身份信息可换行，无图保留原类型标记。成本采用浅雾蓝／深蓝灰卡，金额旁显示截至日期和说明入口。三项财务事实成为独立有边框的卡片，长金额、非现金说明、大字使用纵排。默认金额行改为日期、类型、金额和查看箭头，整行可点。历史多于三条才有「全部」，在原 `LazyColumn` 按原排序展开，用事件 ID 稳定定位。页脚固定按钮继续打开原投入／收回菜单。 |
| `feature/media/CoverPreviews.kt` | 项目详情可选择将已验证的封面画幅等比完整显示，并以同一受预算控制的位图打开只读原生弹窗。仍使用原读取票据。项目、图片引用、读取者或数据代变化时退役旧图。其他调用保持原默认显示方式。 |
| `app/LithicNavigation.kt` | 项目详情入口核对数据 ID 与路由 ID，并接入原 generation 会话及局部 Fit／完整封面参数。路径、Nav3 栈、来源与现有 `requestLeave` 回调保持原合同。 |
| `core/design/C5Information.kt` | 项目成本口径用现有说明对话框的可选紧凑图标入口，48dp 触控与口径文字均保留。既有调用保持默认文本按钮。 |

封面放大显示的是同一张经验证的已解码位图，分辨率仍受现有预算限制。单笔查看和展开均为只读。单笔查看保留现金或折抵、真实日期、金额、方向、角色与备注，显式点纠错才进入原路径。结束结算继续调用 `onUpdateEnd`，其他事件继续 `onCorrectMoney(id)`，以旧换新仍受原关联限制。已结束、旧快照、读取失败、危险操作与未确认写入继续由已有状态及守卫处理。项目页面的订阅对照、Home、业务算法、Room、备份、Owner 实现、构建身份与签名均无源码差异。[项目页源码](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt)

## 测试改动

- 新增 `V3ProjectDetailScreenTest` 的 4 个 Compose 场景和 `V3ProjectDetailAndroidAcceptanceTest` 的真实 `MainActivity` 用例。项目详情通过原 UI 路径打开，设备画廊使用现有具备严格归属检查的临时合成账本夹具。
- `CoverPreviewsTest` 新增两项完整封面回归，覆盖实际画幅四边、单次读取、系统返回／关闭、同代切换读取者。既有读取失败、空引用、不放大默认行为继续覆盖。
- 更新 `C2ProjectDetailSemanticsTest`、`C2R1ProjectDetailLayoutTest`、`C2R2ProjectHierarchyTest`、`ProjectEntryFlowTest`、`ProjectLifecycleFlowTest`、`ProjectLifecycleScreensTest`、`ProjectScreensTest`、`C9RecordThemeTest`。测试先到达新入口再断言原 ID、金额、纠错及写入风险；字号、金额单位与真实节点对齐。

## 运行结果

- 定向 JVM **51 PASS／0 FAIL**，共 7 类。包含项目成本 22、生命周期 4、详情呈现 5、事件标签 4、金额呈现 8、封面管线和策略 8。[JUnit XML](lithic-ui-v3-project-detail-implement-01/desktop/jvm/)
- 最终生产和 AndroidTest Kotlin 编译、普通 Debug App APK 和 Test APK 构建通过。首轮 PowerShell 错误拆分 Gradle 属性，退出 1，随后改用参数数组。第二轮完成全量构建，因交叉复核调整了项目身份校验和整行点按，再按固定源码增量重跑。[首次失败原始日志](lithic-ui-v3-project-detail-implement-01/desktop/initial-command-failed.log) · [最终固定源构建](lithic-ui-v3-project-detail-implement-01/desktop/build.log)
- `lintDebug` 退出 0，0 错误／20 警告。Gradle 离线，JDK 17、SDK 36，两 APK 仅供隔离验证。[运行命令](lithic-ui-v3-project-detail-implement-01/desktop/build-commands.ps1) · [Lint 原始报告](lithic-ui-v3-project-detail-implement-01/desktop/lint-results-debug.txt)
- App 为既有 `com.cooper.lithic.debug`，code17／`0.1.0-m22-debug`，21,224,212 字节，SHA-256 `8f638588cb738f8eec949bfb758b72285f29056fa2747a6601ad077530fcc9c5`。Test 为 `com.cooper.lithic.debug.test`，4,639,558 字节，SHA-256 `8809fcd30189525e7438f0e525eb550e33f66231299b33d984903701fee1736d`，唯一 instrumentation 指向原 Debug App。两份同用原 Debug 证书 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。APK 不入 Git。[源码与实物绑定](lithic-ui-v3-project-detail-implement-01/desktop/apk-build-inputs.json) · [签名与 Manifest 原始输出](lithic-ui-v3-project-detail-implement-01/desktop/apk-audit-full.txt)

## Android 准入及未验证事项

指定隔离环境为上次成功的 `Lithic_Home_Acceptance_Repair05_API36`。本机可用内存的第一次 60 秒观测结束为 5.070 GiB。以 SDK ADB 37.0.1／5038 启动仅本机的隔离服务后，最后 60 秒启动前检查为 4.892→4.932 GiB，低于该隔离环境沿用的 5.000 GiB 门槛。此时 CPU 均值 15.10%，C/E 可用 17.618／24.731 GiB。控制器退出 2，外层工作流退出 1。无模拟器启动、安装、Instrumentation、系统字体调整或业务数据操作。本轮 5038 整理退出码 0，原 5037 的 PID 和创建身份一致。未连接、读取或操作个人设备。[首次资源检查](lithic-ui-v3-project-detail-implement-01/android/pre-service-host-admission.json) · [最后启动门槛](lithic-ui-v3-project-detail-implement-01/android/host-admission.json) · [控制器结果](lithic-ui-v3-project-detail-implement-01/android/controller-result.json) · [隔离与收口摘要](lithic-ui-v3-project-detail-implement-01/android/scope-and-cleanup.json)

Android **0 方法执行，0 张运行截图**。以下均为 **NOT_RUN**：真实 360dp 正常／系统 200% 字体、三主题视觉与真实封面横图／竖图／无图／读取失败、单笔查看／折抵纠错／系统返回、三与四条展开、极端金额／负净投入／真实零值、项目结束和旧快照，以及设备上的零额外写入与现有项目详情／封面回归。相应测试源码已编译，原生实际呈现、滚动、TalkBack 和操作效果仍需端侧验证。设计画板仅用于对照。

## 2026-11-14 Android 补验

复用上述两份 APK，重新核对 SHA-256、签名及源码树。未重建，也未修改生产或测试源码。主机首次 60 秒采样结束为 4.793 GiB，随后一次有界复测为 5.025 GiB。独立 5038 启动后的末次资源采样为 5.004 GiB，执行启动前即时值为 5.054 GiB，超过用户指定的 5 GiB 门槛。[首次采样](lithic-ui-v3-project-detail-implement-01/android-supplement/pre-service-host-admission.json) · [复测](lithic-ui-v3-project-detail-implement-01/android-supplement/settled-host-admission.json) · [末次检查](lithic-ui-v3-project-detail-implement-01/android-supplement/host-admission.json) · [启动回执](lithic-ui-v3-project-detail-implement-01/android-supplement/launch.txt)

原 `Lithic_Home_Acceptance_Repair05_API36` 单次启动，独立 5038 唯一识别 `emulator-17640`，API 36／x86_64、`sys.boot_completed=1`，生成配置为 1536 MiB／4 核。在安装之前，健康观察捕获 `com.android.systemui` 的新事件 `failed to complete startup`，取到对应线程转储。只确认启动无响应，根因仍为 **UNKNOWN**。控制器退出 2，外层退出 1，未尝试二次启动。[设备准入](lithic-ui-v3-project-detail-implement-01/android-supplement/admission-result.json) · [有效硬件配置](lithic-ui-v3-project-detail-implement-01/android-supplement/effective-avd.json) · [ANR 原始事件](lithic-ui-v3-project-detail-implement-01/android-supplement/anr-events.txt) · [控制器结果](lithic-ui-v3-project-detail-implement-01/android-supplement/controller-result.json)

**ANDROID ENVIRONMENT_BLOCKED**。41 个项目详情相关回归方法、两次 MainActivity 画廊（系统字号 100% 和 200%）、三主题与全部截图均为 **NOT_RUN**。安装次数、设备字体写入、测试执行、截图均为 0。模拟器有序退出，5038 关闭，主机原 5037 身份保持一致。启动前后原 userdata 文件身份一致，基础层哈希相同，隔离 QCOW2 正常写入后的新哈希已记录。无个人手机或个人账本操作。保留上一轮因内存阻塞的证据，桌面结论沿用上节。[模拟器退出](lithic-ui-v3-project-detail-implement-01/android-supplement/emulator-cleanup.json) · [范围与清理](lithic-ui-v3-project-detail-implement-01/android-supplement/scope-and-cleanup.json)
