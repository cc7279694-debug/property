# 砺石 V1 产品完善

来源：用户2026-10-25的P01正式任务；基线`b9e77b3ff364be4bdade79803d0aca2f607c7bd5`，独立分支`codex/lithic-v1-product-completion`。原M22分支及证据保留，不将开发推进改写成历史验收通过。

## 当前策略与固定边界

先持续完善主要产品功能，再集中体验与独立验收；本轮不要求用户安装、截图或操作App。只完善现有V1，不新增业务模块。保留原生Android/Local-first、现在/回看/设置、项目/订阅、Room5、backup4、冻结引擎和M16/通知规则、同代Owner、写协调器、真实事务与导航保护；不新增依赖、网络权限、账户或云服务。

P01、P02已获用户转交的独立桌面实施审查PASS WITH NOTES，均保留IMPLEMENTED / PENDING FINAL PRODUCT REVIEW，人工验收延期。用户从`ba24df68a4ee242912e4b010b8e485b713489e12`正式授权P03；P04仍为建议顺序/未实施，不构成设备或交付授权。

| 阶段 | 范围 | 实际状态 |
| --- | --- | --- |
| P01 核心体验 | 首页金额/状态/入口，项目录入/详情，订阅周期/付款/状态，共用视觉与反馈细节 | IMPLEMENTED — PENDING FINAL PRODUCT REVIEW；105唯一JVM PASS、12 Compose仅编译/NOT_RUN；见[P01报告](../evidence/LITHIC-P01-Core-Experience.md) |
| P02 浏览与回看闭环 | 在既有列表/历史/名称搜索及图表入口上处理信息层级、真实空/错误/旧结果提示和返回路径；不改计算引擎或新增榜单 | IMPLEMENTED — PENDING FINAL PRODUCT REVIEW；89唯一JVM PASS、17新Compose仅编译/运行NOT_RUN，Debug/Lint通过；见[P02报告](../evidence/LITHIC-P02-Browse-Review.md) |
| P03 设置与数据可带走体验 | 计算说明、运行时版本/构建状态与关于入口；导出/恢复明确结果分级、十表快照对照与完整覆盖风险 | IMPLEMENTED — PENDING FINAL PRODUCT REVIEW；92唯一JVM PASS、13新Compose仅编译/NOT_RUN；见[P03报告](../evidence/LITHIC-P03-Settings-Data-Safety.md)。Room5/backup4及Owner/原子切换/Token不改 |
| P04 集中收敛与体验 | 汇总主要功能后的受影响回归、可访问性/长历史/完整离线旅程的真实验证与用户体验反馈；必要时准备同ID/同证书code至少18的唯一体验APK，保存到非build交付目录 | DEFERRED / 需届时确认设备与验证授权；不自动恢复M22旧循环或进入M23 |

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
