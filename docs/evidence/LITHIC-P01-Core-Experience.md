# P01 核心产品体验

**IMPLEMENTED — PENDING FINAL PRODUCT REVIEW**。2026-10-25；来源`b9e77b3ff364be4bdade79803d0aca2f607c7bd5`，新分支`codex/lithic-v1-product-completion`。原M22分支保留；用户延期集中体验，不要求现在安装或操作手机。本次不是独立ACCEPTED/FROZEN，M22仍BLOCKED / CHANGES REQUIRED。

## 用户可感知的变化

- **首页**：突出当前日均成本与项目/订阅小计，新增和列表入口分层；仅订阅账本不再以“0条长期项目”领头。未取得成本显示“—”，保留可信记录计数；没有记录、没有当前生效贡献、真实0及负数仍分别呈现。类别按当前贡献解释，不称为年度支出；日期、精确金额和展示舍入仍来自原引擎/AmountText。
- **项目**：说明取得日与付款日的不同、0元录入、投入/收回选择、结束保留事实与永久删除的区别。保存中/结果未确认/已保存回读有独立提示，保留输入和原提交的重试身份；历史读取失败保留旧行时明确标注。删除的“当前不存在、无法证明哪次删除”原文保留，仅将已核对结果改为中性色。
- **订阅**：明确所选月/季/年、30/90/365折算、未来预付与实际生效、未确认付款防重复及服务商自动续费边界。修正已经过时的“不发送提醒”文案，说明设置/通知权限与非准点保证。状态COMMITTED也可能来自CurrentAbsence，只提示只读核对，不声称本次写入；verifiedCannotWrite/最后付款的保护提示优先。
- **交互**：沿用现有Material3、LithicSpacing与AmountText，保留原testTag、selected语义、48dp控制及滚动表单；项目保存反馈增加Polite播报。未复制公式或改提交资格/回调、导航守卫。

关键源码：[首页](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt)、[呈现状态](../../app/src/main/java/com/cooper/lithic/feature/home/HomeOverviewPresentation.kt)、[项目反馈](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectSubmissionNotice.kt)及6个项目Screen、4个订阅Screen（包括[状态确认](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionStateDialog.kt)）。新增1个JVM类与3个Compose测试类；原历史测试文件和断言未修改。

## 实际验证

| 门禁 | 实际结果及证据 |
| --- | --- |
| 先测试 | [预期编译RED](lithic-p01-20261025-040313/red-home-result.json)：新呈现API尚未实现，Gradle exit1、0方法运行；不是业务断言RED |
| 定向JVM | **105个唯一方法PASS**：新增首页6＋金额展示/概览/项目/订阅引擎及导航回归99；[逐类结果与原始XML索引](lithic-p01-20261025-040313/jvm-result.json)。两轮各105 PASS，重复不累计为210项覆盖 |
| 最终固定输入 | 修正新状态文案与新增测试片段定位后，同组执行JVM、`:app:assembleDebug`、`:app:compileDebugAndroidTestKotlin`、`:app:lintDebug`；exit0、366.484秒，[命令、字节/SHA与原始输出](lithic-p01-20261025-040313/final-green-build-lint/command.json)、[输入SHA](lithic-p01-20261025-040313/final-verification-inputs.json)。先前通过的[构建组](lithic-p01-20261025-040313/green-build-lint/command.json)保留 |
| Compose | **12个新增方法编译通过、执行0 / NOT_RUN**（首页2、项目5、订阅5）；[方法身份及编译类SHA](lithic-p01-20261025-040313/android-compile-only.json)。未将编译当设备PASS |
| Lint | **0错误、0Fatal、17警告**；[汇总](lithic-p01-20261025-040313/lint-result.json)、[原始XML](lithic-p01-20261025-040313/lint-results-debug.xml)。不改写历史M22的Lint口径 |
| 构建身份 | [静态审计](lithic-p01-20261025-040313/identity-audit.json)：默认Debug/Release配置与已提交基线完全相同；Private Trial只查配置，原ID/code17/版本保持。研发Debug签名核验exit0、原证书`29ded26b…9a7f83b9`，正常LithicApplication/MainActivity、独立authority、无INTERNET。未构建Release/新的体验APK或Test APK |

研发Debug：`com.cooper.lithic.debug` / code17 / `0.1.0-m22-debug`，SHA `bf72bdb030db5bf42cfe9c17324b5cd7d97d4b7d48ec75bcf1568a0beb28c84e`。仅编译校验，不是给手机的更新包，不提交APK或密钥。

**本地产物限制**：最终审计发现旧体验APK原`app/build/private-trial/outputs/apk/debug/app-debug.apk`位置已不存在，首次保留核对exit1；原控制记录/历史SHA仍保留，未取得直接删除原因，登记[NOT_AVAILABLE观察](lithic-p01-20261025-040313/retained-artifact-observation.json)。未重建其他字节冒充旧`8138…a7fa`，不改历史交付报告。两份原M22隔离App/Test实物仍与原SHA一致。以后体验交付应先将唯一当前产物保存在非构建缓存位置；本轮不交付新用户APK。手机上的任何包均未操作。

## 数据安全与未运行范围

Room5、backup4、领域引擎、Repository/DAO、ViewModel、写协调器、Owner、恢复/导航状态机、Manifest、Gradle/依赖和品牌资源均未改。没有ADB、AVD、手机、安装、账本读取或迁移操作；没有创建业务Owner。设备UI、窄屏/系统大字/IME/TalkBack、实际写入回读、完整旅程、长历史性能、安装UID与恢复均**NOT_RUN**，不从源码/编译推断通过。原137 JVM/36 Android历史组未整套重跑；所有历史失败及保留现场不变。

## P02实际缺口与建议顺序

1. [ReviewShell](../../app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt)只接收项目计数，订阅单独有记录时入口摘要不完整；先补真实两类型计数及空/错误状态，不复制业务计算。
2. 项目旧`String? message`同时承载错误与“已读取、请再确认”普通反馈，部分表单统一着错误色；后续显式区分呈现严重度，不按中文前缀猜测、不改变写入保护。再核对列表/历史/名称搜索、详情返回、旧结果与换代刷新。
3. 原V1[设置规范](../planning/original-v1/lithic_v1_plan/docs/specs/UI_SPEC.md)有计算说明/关于，[SettingsShell](../../app/src/main/java/com/cooper/lithic/feature/settings/SettingsShell.kt)尚未接入，对应P03再做；不以P01名义提前扩展。

[P01～P04产品记录](../planning/LITHIC-V1-PRODUCT-COMPLETION.md)中的后续项仅建议、未实施。M10延期/三JPEG历史FAIL、M13-T06延期、M20平台FAIL/UNKNOWN、Q03/Q04/G1/G2/G3和全部历史NOTES保持；不合并main、不启动M23、不自行冻结。
