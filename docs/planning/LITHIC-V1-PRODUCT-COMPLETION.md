# 砺石 V1 产品完善

## UI-C2 A段视觉整合（2026-10-26）

用户对P04视觉/交互不满意，现授权在本产品分支选择性继承UI-C1，集中实现四页生产Compose样板后等待一次视觉反馈。工程通过不等于视觉确认；P01～P04历史原结果保留。契约与停止点见[规格](../design/UI_C2_SPEC.md)、[A段计划](UI_C2_IMPLEMENTATION_PLAN.md)、[样板报告](../evidence/UI-C2-Sample-Review.md)。A段不交付新版体验APK、不操作设备、不全面铺开其余页面。HTML样板仅PROTOTYPE_ONLY，实际App视觉PENDING；当前基线封面读取接口缺口明确保留。后续仅在视觉确认后扩展，并最终核对当前生产MainActivity实际使用新组件及新preview APK呈现，同ID/同原证书/code高于18。


来源：用户2026-10-25的P01正式任务；基线`b9e77b3ff364be4bdade79803d0aca2f607c7bd5`，独立分支`codex/lithic-v1-product-completion`。原M22分支及证据保留，不将开发推进改写成历史验收通过。

## 当前策略与固定边界

先持续完善主要产品功能，再集中体验与独立验收；本轮不要求用户安装、截图或操作App。只完善现有V1，不新增业务模块。保留原生Android/Local-first、现在/回看/设置、项目/订阅、Room5、backup4、冻结引擎和M16/通知规则、同代Owner、写协调器、真实事务与导航保护；不新增依赖、网络权限、账户或云服务。

P01、P02、P03已获用户转交的独立桌面实施审查PASS WITH NOTES，均保留IMPLEMENTED / PENDING FINAL PRODUCT REVIEW。用户从`d35f8a1a91112c1cf19fb74950c8edbd39be59c0`正式授权P04桌面整合与新版个人体验候选；本轮未进行人工/设备体验，不自动授权M23。

| 阶段 | 范围 | 实际状态 |
| --- | --- | --- |
| P01 核心体验 | 首页金额/状态/入口，项目录入/详情，订阅周期/付款/状态，共用视觉与反馈细节 | IMPLEMENTED — PENDING FINAL PRODUCT REVIEW；105唯一JVM PASS、12 Compose仅编译/NOT_RUN；见[P01报告](../evidence/LITHIC-P01-Core-Experience.md) |
| P02 浏览与回看闭环 | 在既有列表/历史/名称搜索及图表入口上处理信息层级、真实空/错误/旧结果提示和返回路径；不改计算引擎或新增榜单 | IMPLEMENTED — PENDING FINAL PRODUCT REVIEW；89唯一JVM PASS、17新Compose仅编译/运行NOT_RUN，Debug/Lint通过；见[P02报告](../evidence/LITHIC-P02-Browse-Review.md) |
| P03 设置与数据可带走体验 | 计算说明、运行时版本/构建状态与关于入口；导出/恢复明确结果分级、十表快照对照与完整覆盖风险 | IMPLEMENTED — PENDING FINAL PRODUCT REVIEW；92唯一JVM PASS、13新Compose仅编译/NOT_RUN；见[P03报告](../evidence/LITHIC-P03-Settings-Data-Safety.md)。Room5/backup4及Owner/原子切换/Token不改 |
| P04 产品整合与个人体验候选 | 三入口源码接线、相关回归及实际新版APK；显式trial模式code18/preview.2、同ID/原Debug证书，非build独占交付目录 | IMPLEMENTED — PRIVATE TRIAL CANDIDATE；116 JVM PASS、42 Compose仅编译/NOT_RUN；普通Debug Lint0E17W、体验Lint0E11W。[交付报告](../evidence/LITHIC-P04-Integration-Delivery.md)、[以后集中体验清单](../evidence/LITHIC-V1-User-Trial-Checklist.md)。用户体验/正式发行未验收 |

## P01执行合同

- **Goal**：使现有核心页面更易读、更好操作，避免金额、日期、保存结果和提醒能力的误导。
- **Scope**：HomeShell与项目/订阅Screen、必要的只读呈现helper、定向测试及本记录/交付报告；优先沿用Material3、AmountText与既有回调。
- **Out of scope**：Room/迁移、备份格式、冻结金额/日期/状态、写入/恢复/Owner协议、新Logo、生产签名、任何ADB/AVD/手机操作，M22-T06/T03/IME重跑及不相关全量回归。
- **Acceptance**：主金额及两类小计清楚；真实0/负值不丢失；未取得成本不伪造0；记录计数与成本快照不混淆。表单说明与原事实口径一致，已保存/未确认/需重读不诱导新建重复写入；testTag、原守卫、48dp目标和系统字体适配不被移除。
- **Verification**：新呈现逻辑定向JVM，受影响金额展示/引擎与导航回归；必要Compose断言和AndroidTest编译；Debug构建与Lint；普通Debug/Release/Private Trial身份连续性静态检查。未实际运行的Android方法明确NOT_RUN。
- **Safety stop**：如发现需要改变用户数据、迁移、备份兼容或冻结金额算法才能修复的问题，停止该风险改动并登记，不以UI绕过保护。

## 已有能力与本轮边界

基线已有真实项目/订阅管理、金额事件/付款/状态、类别与搜索、成本/历史图表、完整备份恢复及提醒。P01不把这些重做成另一套系统，也不把源码存在等同本轮设备运行PASS。

独立体验版仍是`com.cooper.lithic.preview.debug`、原证书、code17/`0.1.0-preview.1-debug`。本轮不交付新用户体验APK；旧本地体验APK在原构建输出位置缺失，未重建其他字节冒充，见P01报告。两份原M22隔离APK字节仍一致，任何手机包未操作。今后更新须同ID/同证书且code至少18。研发Debug构建不是体验版更新或正式发行。

M22仍BLOCKED / CHANGES REQUIRED、剩余验收DEFERRED；M10延期/三JPEG历史FAIL、M13-T06延期、M20平台FAIL/UNKNOWN、全部历史RED/FAIL/UNKNOWN/NOT_RUN/NOTES保持。阶段完成只登记 **IMPLEMENTED — PENDING FINAL PRODUCT REVIEW**，不ACCEPTED/FROZEN、不合并main。

## P02执行合同

- **Goal**：浏览当前/历史、名称搜索和回看分析时，用户能识别真实读取状态并返回原来源，不丢失仍有效的查询和浏览上下文。
- **Scope**：ReviewShell与现有Ready计数透传，四个列表/历史页、SearchScreen，必要只读呈现状态、现有导航上下文和消息呈现元数据；沿用现有请求、分页、排序、身份和守卫。
- **Locked**：Room5/backup4、冻结金额/日期/状态/通知规则，Repository/DAO/事务/Owner/恢复协议、三主入口、原testTag/48dp、默认Debug/Release/Private Trial身份/证书、无新增依赖或网络。
- **Adjustable**：标题、空/读取/失败/旧快照说明、分页/重试忙态、源已明确的信息/警告/错误视觉；必要返回上下文不得绕过未确认提交/放弃/恢复保护。
- **Verification**：新增呈现逻辑及真实导航缺陷的定向JVM，既有名称转义/搜索迟到请求/导航守卫回归；新Compose仅源码编译，Debug与Lint，配置/证书连续性静态核对。无ADB/模拟器/真机/安装，运行NOT_RUN。
- **Stop**：需改变冻结业务状态机/数据协议的子项停止，不用字符串前缀、空响应或未知当成功；不做P03/P04，不补造缺失旧体验APK。

## P02已验证停止点

来源7bec69e3，P01桌面实施独立审查限定PASS WITH NOTES保持，最终人工验收仍延期。两类型回看、四列表/搜索状态、浏览父级与消息呈现已实施；原路径9方法实际RED（6失败）到同方法全GREEN，最终89唯一JVM PASS，17 Compose仅编译/NOT_RUN，Lint0错误17警告。见[P02报告](../evidence/LITHIC-P02-Browse-Review.md)。Room5/backup4、查询/业务写入/Owner/恢复与构建身份均保持；没有设备或用户安装操作。此后用户明确授权P03，见下节；原P02停止点和历史报告保持，不构成自动P04授权。

## P03执行合同与已验证停止点

- **Goal / Scope**：让设置入口可解释现有规则、显示当前安装版本，并让完整导出/覆盖恢复的已知结果与未知结果可区分。只改原设置UI、Controller呈现元数据、必要Nav3设置入口及定向测试。
- **Locked / Out of scope**：Room5/backup4、冻结引擎、实际导出/恢复服务、事务/Owner/原子指针/Token/导航守卫、依赖与身份/证书不改；不增加持久状态、自动/云备份；无ADB/设备/个人数据操作，不生成新体验包或实施P04。
- **Verification**：3个真实中断反馈RED到同身份GREEN；最终92唯一JVM PASS（38新/54相关），13新Compose仅编译/NOT_RUN，Debug/AndroidTest编译/Lint exit0，Lint0错误17警告；配置、实际Debug Manifest/DEX/证书及受保护路径审计通过。见[P03报告](../evidence/LITHIC-P03-Settings-Data-Safety.md)。
- **Safety / Next**：读当前包版本失败显示未知；Debug标志不是签名证明。导出/恢复仅真实Success报成功；恢复未知先核对勿清数据，陈旧预览仍由原快照机制拒绝。M10/M20/M22等历史风险进入P04清单，不自动修复或升级结论。本报告与CURRENT_STATE共同作为交接点，无额外大归档。

## P04合同与实际停止点（2026-10-26）

- **Goal / Scope**：复用现有V1功能完成桌面整合与可核验个人体验APK，不新增业务模块。只修正显式体验版本、排除体验APK测试宿主、补明确试用风险说明；原导航、数据与备份恢复控制不改。
- **Locked**：Room5/backup4、冻结金额/日期/订阅/替换/提醒规则、Owner/Token/原子切换/事务、正常Debug/Release和M22身份不变；同preview ID/原Debug证书；不得设备操作、安装、用户账本访问、M22旧套件或M23发行扩展。
- **Verified**：116唯一JVM PASS；42原P01～P03 Compose方法仅编译/NOT_RUN；普通Debug构建/Lint0E17W、体验构建/Lint0E11W成功。APK Manifest/DEX/权限/原证书核验通过；仅生产MainActivity、无测试框架/宿主，明确为Debug个人试用。
- **Binding / Delivery**：构建源码固定`96e4d7c762c2b82e8e245a4d0260bea3744b62e3`，报告登记提交不改其产品树。唯一code18/`0.1.0-preview.2-debug`实物已复制至非build deliveries目录，13,854,809字节，SHA-256 `d39b6bf764720415d88d0d9f6c149b34dcda2d795f823505786fa4130b906ef4`；逐字节一致、Git忽略。实际绝对路径和回执见[P04报告](../evidence/LITHIC-P04-Integration-Delivery.md)，不沿用旧code17已缺失的产物身份。
- **Boundary / Next**：建议以后只用可丢弃记录集中体验；不能把未完成实际恢复验证的版本作为重要数据唯一副本。M10三FAIL、M20平台FAIL/UNKNOWN、M22 BLOCKED/T06/T03/IME、M13-T06及G1/G2/G3/G4等旧门禁保留。APK可核验不等于用户流程、稳定签名或升级已PASS；提交推送后停止，不自行冻结/合并main/进入M23。
