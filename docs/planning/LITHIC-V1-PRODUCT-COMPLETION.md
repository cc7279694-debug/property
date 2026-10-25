# 砺石 V1 产品完善

来源：用户2026-10-25的P01正式任务；基线`b9e77b3ff364be4bdade79803d0aca2f607c7bd5`，独立分支`codex/lithic-v1-product-completion`。原M22分支及证据保留，不将开发推进改写成历史验收通过。

## 当前策略与固定边界

先持续完善主要产品功能，再集中体验与独立验收；本轮不要求用户安装、截图或操作App。只完善现有V1，不新增业务模块。保留原生Android/Local-first、现在/回看/设置、项目/订阅、Room5、backup4、冻结引擎和M16/通知规则、同代Owner、写协调器、真实事务与导航保护；不新增依赖、网络权限、账户或云服务。

本轮只授权P01实现。下表P02～P04是承接当前源码缺口的**建议顺序**，尚未实施，不构成提前进入后续阶段、设备验证或发行授权。

| 阶段 | 范围 | 实际状态 |
| --- | --- | --- |
| P01 核心体验 | 首页金额/状态/入口，项目录入/详情，订阅周期/付款/状态，共用视觉与反馈细节 | IMPLEMENTED — PENDING FINAL PRODUCT REVIEW；105唯一JVM PASS、12 Compose仅编译/NOT_RUN；见[P01报告](../evidence/LITHIC-P01-Core-Experience.md) |
| P02 浏览与回看闭环 | 在既有列表/历史/名称搜索及图表入口上处理信息层级、真实空/错误/旧结果提示和返回路径；不改计算引擎或新增榜单 | PLANNED / 未实施；建议先处理ReviewShell仅呈现长期项目计数的问题，再区分既有String消息中的错误与普通反馈，统一浏览页状态与说明；不按中文前缀猜测消息类型 |
| P03 设置与数据可带走体验 | 复用现有备份/恢复/提醒，完善状态反馈、覆盖确认和失败保护说明；检查原V1计算说明/关于/版本信息的页面缺口 | PLANNED / 未实施；不改format4或恢复/Owner协议，不开展M20平台收口 |
| P04 集中收敛与体验 | 汇总主要功能后的受影响回归、可访问性/长历史/完整离线旅程的真实验证与用户体验反馈；必要时准备同签名递增版本体验APK | DEFERRED / 需届时确认设备与验证授权；不自动恢复M22旧循环或进入M23 |

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
