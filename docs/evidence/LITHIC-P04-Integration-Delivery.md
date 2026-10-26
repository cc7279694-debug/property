# LITHIC P04 — 产品整合与个人体验候选交付

**P04 IMPLEMENTATION COMPLETE — READY FOR USER TRIAL**。起点`d35f8a1a91112c1cf19fb74950c8edbd39be59c0`，分支`codex/lithic-v1-product-completion`。P01～P03独立桌面审查PASS WITH NOTES，均保留IMPLEMENTED / PENDING FINAL PRODUCT REVIEW；本轮未进行用户体验，不是正式Release、M22冻结或M23通过。

## 整合与最小差异

- 源码确认三主入口、生产Ready依赖和实际动作连续；项目/订阅创建、详情、历史、搜索与类别分析的返回上下文仍经原requestLeave/onBack及恢复/pending/未保存守卫。未发现明确空回调或导航断路。搜索实际在“现在”首页，不虚构“回看→搜索”按钮。[逐项源码审查](lithic-p04-20261026-011822/integration-source-review.json)。
- 计算说明复用冻结契约；关于读取当前PackageManager元数据。备份仅真实Success报成功；取消、失败、残留及未知分级不变。恢复仍完整验证→预览→显式确认→覆盖，无新增绕过确认入口；Controller、Token、Owner、原子指针、事务和Repository未改。
- 实际代码仅3文件：`app/build.gradle.kts`在显式trial模式下升code18/preview.2，并仅对该模式排除`ui-test-manifest`；`ProductInformation.kt`补可丢弃数据/不要唯一副本说明；原`P03ProductInformationTest`增加2项文案断言，无新方法或断言放宽。历史体验[Manifest](lithic-private-trial-20261024-104836/merged-manifest.xml)含测试宿主，本次用户APK去除；普通Debug的Compose测试宿主继续保留。
- Room5、backup4、冻结金额/日期/订阅/替换/提醒、数据层、原三主入口、媒体原字节口径和依赖版本保持。未合并M10 Worker/JNI，未接触任何设备或个人账本。

## 实际门禁

| 项目 | 结果与原始证据 |
|---|---|
| 风险文案RED→GREEN | 原8方法中1项新增约束真实断言FAIL，7 PASS，编译/运行错误0；64.453秒。补说明后同方法GREEN。原RED保留：[JUnit](lithic-p04-20261026-011822/TEST-com.cooper.lithic.feature.settings.P03ProductInformationTest.xml)、[命令](lithic-p04-20261026-011822/red-trial-risk-copy/command.json)。 |
| 定向JVM | 116个唯一方法PASS、失败/错误/跳过0，15个相关类；只复用必要P01～P03/金额展示/搜索/导航/导出恢复Controller，不执行旧领域全量或M22-T06/T03。[XML索引](lithic-p04-20261026-011822/jvm-result.json)。不累计重复次数为覆盖量。 |
| 普通Debug | 构建＋AndroidTest源码编译＋Lint exit0，462.000秒。Lint0错误/0Fatal/17警告。[命令与SHA](lithic-p04-20261026-011822/quality-debug-build-lint/command.json)、[stdout](lithic-p04-20261026-011822/quality-debug-build-lint/stdout)、[stderr](lithic-p04-20261026-011822/quality-debug-build-lint/stderr)、[Lint XML](lithic-p04-20261026-011822/lint-results-debug.xml)。 |
| 体验版 | `--offline -PlithicPrivateTrial=true :app:assembleDebug :app:lintDebug`（另带只读身份任务、单worker及锁定JDK17/SDK36），exit0，349.688秒；Lint0错误/0Fatal/11警告。[实际完整命令](lithic-p04-20261026-011822/private-trial-build-lint/command.json)、[stdout](lithic-p04-20261026-011822/private-trial-build-lint/stdout)、[stderr](lithic-p04-20261026-011822/private-trial-build-lint/stderr)、[Lint XML](lithic-p04-20261026-011822/trial-lint-results-debug.xml)。 |
| Android/Compose | P01～P03的42个方法只编译，实际运行全部NOT_RUN；无新增Android方法。[唯一方法与编译SHA](lithic-p04-20261026-011822/android-compile-only.json)。设备/ADB/安装/升级/真实SAF/恢复/字号IME/截图为0或NOT_RUN。 |

源码绑定：APK从已固定提交 **`96e4d7c762c2b82e8e245a4d0260bea3744b62e3`** 构建；本报告之后的登记提交仅文档/证据，不改产品输入。完整app与构建输入Git对象、3文件真实SHA、质量输入见[源码绑定](lithic-p04-20261026-011822/source-binding.json)、[质量输入](lithic-p04-20261026-011822/quality-inputs.json)。不将起点d35直接冒充最终产物源码。

## 唯一交付APK

- 本地绝对路径（不在Git中）：`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/deliveries/lithic-preview-0.1.0-preview.2/Lithic-0.1.0-preview.2-debug.apk`
- 大小：**13,854,809字节**。
- SHA-256：**`d39b6bf764720415d88d0d9f6c149b34dcda2d795f823505786fa4130b906ef4`**。
- 包名`com.cooper.lithic.preview.debug`；显示“砺石·体验版”；code18 / `0.1.0-preview.2-debug`；minAPI26、target36。
- 单一原Debug证书SHA-256：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。实际apksigner校验exit0；不是正式Release密钥，未生成替代密钥。
- 正常LithicApplication、唯一Launcher/MainActivity、自有非导出startup Provider；无shared UID/Instrumentation/testOnly/INTERNET。权限与既有边界一致，未新增授权。[实际Manifest](lithic-p04-20261026-011822/trial-merged-manifest.xml)、[APK Manifest回执](lithic-p04-20261026-011822/trial-apk-manifest/command.json)、[签名](lithic-p04-20261026-011822/trial-signature/command.json)、[badging](lithic-p04-20261026-011822/trial-badging/command.json)。
- ZIP CRC通过；13个DEX/18,832个定义类包含生产P01～P03组件，无JUnit、androidx.test、自有测试/Runner或Compose测试宿主。生产Compose语义/testTag保留；不把字符串里含Test当作测试框架。[产物审计](lithic-p04-20261026-011822/artifact-audit.json)。
- 交付目录新建且独占，只有1个APK；无链接/越界、不覆盖旧文件，位于Gradle build目录外。已逐字节及SHA复核与构建输出一致；APK/密钥未入Git，无ZIP/重复候选。[交付回执](lithic-p04-20261026-011822/delivery-receipt.json)。旧code17体验实物此前缺失的历史不改，该缓存输出位置现在是新v2字节，不能当作旧SHA文件。

普通Debug/Release与M22验证配置前后完全相同、原证书不变，原两份M22本地隔离APK字节不变；普通研发Debug输出按必要构建正常更新，不作为交付APK。[前置快照](lithic-p04-20261026-011822/preflight.json)、[质量边界](lithic-p04-20261026-011822/quality-scope-audit.json)。未打包Test/Release APK，安装UID与覆盖升级仍NOT_RUN。

## 已知风险、体验与正式版后续

**仅供个人试用，建议使用可丢弃数据；不要把未完成实际恢复验证的版本作为重要数据的唯一副本。**体验清单见[用户清单](LITHIC-V1-User-Trial-Checklist.md)，本轮不要求用户安装或操作。

M10三JPEG历史FAIL及媒体安全缺口；M20系统平台FAIL/第二收口UNKNOWN；M22原BLOCKED与T06/T03/真实IME未完成；M13-T06稳定签名/实际升级延期；Q03/Q04、G1/G2/G3/G4及全部历史NOTES/FAIL/UNKNOWN/NOT_RUN保留。静态包隔离不等于实际UID、手机升级、Owner关闭、恢复/提醒送达或完整UI旅程已经通过。

后续集中体验需围绕现有三入口与可丢弃记录，遇到结果未知先保留提示、不清数据、不重复覆盖。正式M23另需稳定正式签名保管和构建身份、真实同签名升级与数据保留、当前schema/各备份格式兼容、相关故障/权限/日期边界和设备可靠性实证，再由独立验收判定；不能用本APK或桌面PASS关闭这些门禁。Codex设备操作必须另获明确授权。

桌面取证辅助脚本曾将自身证据目录计作脏工作区，及误用旧P01索引字段；均仅修正元数据读取，未重跑测试或伪造结果，见[前置说明](lithic-p04-20261026-011822/preflight-harness-note.json)、[元数据说明](lithic-p04-20261026-011822/quality-postprocess-note.json)。独立额外aapt诊断中绝对中文路径exit1、同一实物相对路径exit0；错误只报告了有界前缀，完整stderr为NOT_CAPTURED，不冒充完整日志，见[路径诊断说明](lithic-p04-20261026-011822/independent-path-probe-note.json)。本轮正式归档SDK调用从首次即使用相对路径并通过；未修改SDK或APK。原始结果、历史报告及受保护现场均保持。提交推送后停止，不合并main、不安装、不自行冻结或进入M23。
