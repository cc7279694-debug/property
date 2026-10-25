# P02 浏览、历史与回看体验

**IMPLEMENTED — PENDING FINAL PRODUCT REVIEW**。2026-10-25；来源`7bec69e3c82b4a28168e69964df718b1a3e149b7`，继续`codex/lithic-v1-product-completion`。P01桌面实施独立审查PASS WITH NOTES仅继承其实施证据，人工体验延期。M22历史仍BLOCKED / CHANGES REQUIRED，不登记产品冻结或发行通过。

## 已实现的用户体验

- **回看入口**：从同一个Ready直接透传项目/订阅数量，不新增查询或计算；订阅单独有记录时不再暗示全空。明确含当前与历史，不是当前成本贡献数。有读取/恢复提示时只显示已取得快照，保留原外层说明/重试，不将旧零计数当最新空账本。长期复盘、趋势、类别、年度与两类历史入口保留。
- **四列表与搜索**：区分初次读取、未取得数量、确认空、失败和保留旧结果；标注已显示数量/分页/末尾，忙时不重复重试。错误时原有继续加载合同保留。搜索首次未读不显示“找到0条”，同条件刷新/失败标注旧快照和原业务日期；换条件清空旧结果、迟到票据拒绝机制原来已正确，Controller和查询/排序/分页均未改。已到期仍按冻结引擎定义，增加“当前无生效套餐，可能含未来预付”的解释，不新增状态。
- **返回来源**：搜索→项目/订阅详情及后续表单保留搜索父级和“返回搜索结果”；删除后返回搜索。类别分析保留实际选中类别/类型，删除后也回原类别。仅在已有规范路由上替换浏览父级，错ID、错编辑类型、错自定义类别类型被拒绝；兼容原默认中性类别父级。原pending/discard/恢复与Milestone调用顺序保留，显式三主入口不会被搜索捕获。
- **操作反馈**：新增最小UiMessageTone/UiMessageState与共用反馈Text，保留原文、旧tag和单独Polite语义，不按中文前缀分类。每次String赋值默认恢复ERROR；只有已核对的项目重读和匹配删除/CurrentAbsence结果显式提示级。原CurrentAbsence仍说明无法证明哪次删除，后续读错即恢复ERROR，不因COMMITTED降级。订阅正常回读不再误称读取失败；原错误与独立核对警告分开，未确认输入仍锁定、同提交重试资格不变。

主要修改：[ReviewShell](../../app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt)、[导航连接](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt)/[已有路由规则](../../app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt)，四个List/History Screen、[SearchScreen](../../app/src/main/java/com/cooper/lithic/feature/home/SearchScreen.kt)/UiModels，[反馈呈现](../../app/src/main/java/com/cooper/lithic/core/design/UiFeedbackText.kt)/[元数据](../../app/src/main/java/com/cooper/lithic/core/design/UiFeedback.kt)、项目UiModels/表单和仅呈现元数据的[ProjectViewModel差异](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt)、订阅表单及[UI返回来源](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionReviewOrigin.kt)。未新增NavKey/业务字段、第二搜索引擎或缓存系统；沿用Nav3和LazyColumn既有状态保存机制。

## 真实离线门禁

| 验证 | 实际结果与证据 |
| --- | --- |
| 原缺陷RED | [9方法实际执行：6 FAIL / 3 PASS](lithic-p02-20261025-075647/red-result.json)，不是编译RED；[原始XML](lithic-p02-20261025-075647/red-jvm/TEST-com.cooper.lithic.app.P02BrowseNavigationTest.xml)与命令/完整stdout、stderr保留 |
| 对应GREEN | [同9个原方法最新全PASS](lithic-p02-20261025-075647/red-green-closure.json)，原失败未改写 |
| 定向JVM | 最终**89唯一方法PASS**：新35＋既有相关54（名称转义/搜索迟到响应、UI来源、M14～M21导航规则）；[逐类结果/原始XML索引](lithic-p02-20261025-075647/jvm-result.json)。首轮[87 PASS](lithic-p02-20261025-075647/first-green-result.json)后补2个类别/返回边界及改进迟到完成等待；重复执行不累计覆盖量 |
| 最终同组输入 | JVM、Debug、AndroidTest源码编译及Lint共同exit0，**464.563秒**；[完整命令及日志长度/SHA](lithic-p02-20261025-075647/reviewed-green-build-lint/command.json)、[最终输入SHA](lithic-p02-20261025-075647/reviewed-final-inputs.json)。首轮[构建回执](lithic-p02-20261025-075647/green-build-lint/command.json)独立保留 |
| Compose | **17个新方法仅编译通过，运行0 / NOT_RUN**：回看4、列表5、搜索3、反馈5；[方法及编译类SHA](lithic-p02-20261025-075647/android-compile-only.json) |
| Lint | **0错误、0Fatal、17警告**；[结果](lithic-p02-20261025-075647/lint-result.json)、[原始XML](lithic-p02-20261025-075647/lint-results-debug.xml)。不改历史M22警告口径 |
| 身份/数据范围 | [静态审计](lithic-p02-20261025-075647/identity-scope-audit.json)：默认Debug/Release与P01一致，Private Trial只核配置、未构建；原证书签名核验exit0，无INTERNET或新权限/组件。原两份M22隔离APK SHA仍一致，旧体验APK仍NOT_AVAILABLE，未重建替代 |

开发Debug：`com.cooper.lithic.debug` / code17 / `0.1.0-m22-debug`，SHA `ab3ee182abddbd01651ddd8154f05678fa70e4904d505736a75ea79550b1fc8e`，证书SHA `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。这是构建校验产物，非用户体验包；未构建Release/Test APK，未安装。

桌面后处理曾遇Windows控制台UTF-8解读错误（exit1），已[单独登记](lithic-p02-20261025-075647/console-encoding-note.json)；原日志字节未改，统计来自原XML，任务/身份只解析ASCII标记，未为此重跑构建或测试。

## 未运行范围与数据保护

设备/ADB/AVD/手机/安装/截图均0。实际Room列表/搜索、详情返回与滚动恢复、系统字号/IME/TalkBack、写后回读、跨日/换代/备份恢复旅程仍**NOT_RUN**；只证明纯JVM状态/路由规则和编译，不宣称完整体验PASS。原P01、M22等不相关历史整套测试未重跑，旧测试源码/原断言未修改。

Room5、backup4、Repository/DAO/SQL、金额/日期/排序/付款/状态命令、Owner、恢复协议、GenerationUiScope、SearchController、SubscriptionViewModel、依赖/Manifest/签名配置均无差异。Project VM只新增消息呈现元数据及明确源分支的级别，不改变phase/Bundle/提交/重试/事务。没有读取或修改个人账本，也不从未操作设备推断其Owner/任务已经关闭。M10延期/三JPEG历史FAIL、M13-T06、M20平台FAIL/UNKNOWN、全部历史NOTES及M22 BLOCKED保持。

## P03实际缺口与建议

1. [SettingsShell](../../app/src/main/java/com/cooper/lithic/feature/settings/SettingsShell.kt)尚无原V1的计算说明、关于和实际版本入口；先复用冻结口径和构建身份补只读说明，不把Debug/个人试用称作正式发行。
2. 现有[导出](../../app/src/main/java/com/cooper/lithic/feature/settings/BackupExportController.kt)/[恢复](../../app/src/main/java/com/cooper/lithic/feature/settings/BackupRestoreController.kt)UI主要只有phase/message（恢复另有preview）。P03需按调用源的真实结果区分取消/成功/失败，核对现有覆盖确认、失败保护与恢复后反馈；不能从IDLE或字符串推断备份成功，不改format4/原字节/Owner/切换协议，不预设底层有新缺陷。
3. 交付前必须明确M10三JPEG FAIL与媒体安全边界仍延期，不能用本轮UI/备份文案关闭它们或合并未冻结Worker/JNI。M20平台FAIL/UNKNOWN也不因提醒设置完整而消失，不承诺系统准点送达或操作旧队列。
4. P04再按真实源码构建`com.cooper.lithic.preview.debug`、同证书、code至少18，将唯一交付APK复制至非build目录；旧code17 SHA只作历史，不伪造缺失实物。本轮不提前实施P03/P04。

[产品完善记录](../planning/LITHIC-V1-PRODUCT-COMPLETION.md)已同步。用户无需现在安装、截图或验收；不合并main、不冻结、不进入M23。
