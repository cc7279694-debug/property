# C7 集中产品整合

2026-10-28（Asia/Shanghai）；分支 `codex/lithic-v1-product-completion`；起点 `267fec1623299ac1c42acb19bfb908300c331473`。

## 范围与完成口径

目标是沿真实生产页面核对物品、订阅两条旅程，修复确定的接线或反馈缺口，继承 C5 界面，不重复开发正确功能。范围限 UI 来源、反馈和对应测试；不改 Repository、Room5、backup4、冻结计算、Owner/generation、恢复或提醒协议，不操作设备，不生成用户体验包。下表是源码覆盖结论，**不是 Android 实际旅程 PASS**。

| 旅程 | 已有真实实现／本轮处理 |
|---|---|
| 物品创建与购买事实 | Home/ProjectList → ProjectEditorScreen；名称、类别、开始日、真实初始投入和独立付款日由原 VM 校验、写入；0 元与空输入不同。修复从首页创建后保存详情的返回来源。 |
| 成本、追加投入、维修与收回 | ProjectDetailScreen → 原金额表单；维修使用已有投入与说明，不增设金额类型；精确成本、真实现金/非现金事实和原记录纠错保留。 |
| 以旧换新、替换与结束 | 原 TradeInEditor/ReplacementComparison 预览、确认、幂等及交易组保护保留；结束/修正结束、关联收回与永久删除仍有独立入口和确认。 |
| 物品历史与来源 | ProjectHistory、成本图、搜索、类别和回看来源保持；原分页、失败保旧、写后刷新及迟到详情拒绝不变。 |
| 订阅创建与真实续费 | Home/SubscriptionList → SubscriptionEditor/RenewalEditor；实际金额、付款日、覆盖与提醒日期由原 VM 处理，不自动造付款。修复从首页创建后保存详情的返回来源。 |
| 订阅成本、周期及状态 | 当前套餐与历史平均成本、30/90/365 折算、预付未来覆盖、暂停/恢复/停止和同身份重新启用均有原入口；停止记录不取消服务商扣款。 |
| 订阅纠错与历史 | 原付款/状态纠错、删除付款、取消计划停止、历史分页和写后刷新保留；停止后的 HISTORY 来源与搜索/类别父栈不变。 |
| 订阅重新启用反馈 | 无已确认覆盖时进入真实付款表单；跨日校验原本写入 `errors["form"]`，共享付款操作区却未展示。新增持续错误提示，原返回、确认、付款资格和 operationId 不变。 |
| 全局保护 | 原 requestLeave、dirty/pending/discard、UNCONFIRMED 防重复、恢复守卫及 generation/Repository 实例换代隔离继续使用。图片导入阻断不妨碍上述入口。 |

主要核对来源：[生产导航](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt)、[来源规则](../../app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt)、[项目 VM](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt)、[订阅 VM](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionViewModel.kt)。

## 实际差异

- [LithicRoutes](../../app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt) 与 [LithicNavigation](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt)：集中构造保存后的详情目标，仅精确 `[Home, CreateProject/CreateSubscription]` 使用 HOME；列表新增和其他操作沿用原来源、comparisonId 和已确认订阅状态来源。目标仍在原离开守卫之后经 canonicalBackStack / navigate / 浏览来源适配，没有新导航通道。
- [SubscriptionEditorScreen](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionEditorScreen.kt)：共享付款操作区显示结构化表单错误，不从中文消息猜测状态，不改变写入、重试或确认条件。
- [C7JourneyNavigationTest](../../app/src/test/java/com/cooper/lithic/app/C7JourneyNavigationTest.kt)：覆盖两类首页/列表新增、既有操作来源、comparisonId、搜索、具体类别、停止后 HISTORY 和非法身份。新增 [SubscriptionScreensTest](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionScreensTest.kt) REENABLE 错误呈现断言；Android 实际运行 NOT_RUN。

## 验证

| 检查 | 实际结果与边界 |
|---|---|
| 资源 | 初始约 3.7GiB 未启动构建；用户释放后 5.301GiB，最终组合检查前 5.148GiB；未停止其他进程。 |
| 首轮来源回归 | 原提交目标等价抽取后，7 方法 **5 PASS / 2 FAIL**，真实复现两类首页新增后的错误来源；原 XML、stdout/stderr 和 exit1 保留。归档后终端摘要遇 GBK 打印异常，不影响已落盘的测试输出及真实退出码。 |
| 最终定向 JVM | **56 个唯一方法 PASS**，其中 C7 新增 10 项；另 46 项覆盖既有浏览/导航、订阅来源、项目管理动作和以旧换新提交守卫。重复执行不累计覆盖数量。 |
| AndroidTest | 新增 1 个 REENABLE Compose 方法，仅源码编译；真实 Android 运行 **NOT_RUN**，不作为完整业务旅程或视觉 PASS。 |
| 构建、Lint | 最终组合命令 **exit0 / BUILD SUCCESSFUL**；Debug 构建、AndroidTest Kotlin 源码编译实际完成，Lint **0 错误 / 15 警告**；原 SDK XML 版本警告保留，没有抑制新问题。 |
| 安装身份与协议 | 普通 Debug `com.cooper.lithic.debug` / code17 / `0.1.0-m22-debug`，实际 APK Manifest、正常 Application/Launcher、原 Debug 证书验证 exit0；权限及 native 库集合与起点一致，无 INTERNET 或新 native。没有构建 Private Trial 或生成用户交付物。 |

[真实 RED](c7-product-integration-20261028-213140/red-navigation/command.json)、[RED 逐方法](c7-product-integration-20261028-213140/red-navigation/TEST-com.cooper.lithic.app.C7JourneyNavigationTest.xml)、[最终命令/退出码](c7-product-integration-20261028-213140/final-check/command.json)、[56 项逐类汇总](c7-product-integration-20261028-213140/final-check/jvm-summary.json)、[最终 stdout](c7-product-integration-20261028-213140/final-check/stdout)、[最终 stderr](c7-product-integration-20261028-213140/final-check/stderr)、[Lint XML](c7-product-integration-20261028-213140/final-check/lint.xml)、[APK/Manifest/签名及 DEX 静态身份](c7-product-integration-20261028-213140/identity/summary.json)、[冻结路径差异核对](c7-product-integration-20261028-213140/boundaries.json)、[编译源码与 Git 映射](c7-product-integration-20261028-213140/source-git-map.json)。映射仅记录本轮五个改动源码的实际编译字节与 Git 换行规范化差异，不生成整仓快照。

## 未关闭的边界

- Android 两条完整旅程、返回动态、字体/IME、真实升级、视觉及性能本轮均 **NOT_RUN / PENDING**；源码、JVM 与编译不代替用户体验验收。
- C6-1 保持限定桌面冻结；原 14 项 Android NOT_RUN、媒体性能 UNKNOWN 不改。C6-2C 隔离基础保持原样，原 8 项 Android NOT_RUN；没有接入 Picker、JPEG native 或真实媒体写入。**MEDIA_IMPORT_BLOCKED**，已有只读显示不等于完整图片闭环。
- 订阅名称、类别、备注依 M08 契约仅创建时录入，未擅自增加 Repository 更新接口；需要改变该能力时须另行审查冻结合同。
- M10 延期及三个 JPEG 历史 FAIL、M20 平台 FAIL/收口 UNKNOWN、M22 BLOCKED、C3 关闭调查/运行状态 UNKNOWN、M13-T06 延期、Q03/Q04 与 G1～G4 历史状态及全部 NOTES/NOT_RUN 均保留；无个人数据、设备或受保护现场操作。
- 普通 Debug/Release 与 Private Trial 的包名、版本和签名配置不变；Preview 9/code25 及历史交付字节不变，本轮不交付新 APK，不合并 main、不进入 M23 或后续阶段。

完成后停在 **C7 PRODUCT INTEGRATION — READY_FOR_INDEPENDENT_REVIEW / MEDIA_IMPORT_BLOCKED / ANDROID_VISUAL_PENDING**，等待独立审查，不自行授予产品冻结或发行通过。
