# LITHIC P03 — 设置、备份恢复与产品说明

**IMPLEMENTED — PENDING FINAL PRODUCT REVIEW**。来源`ba24df68a4ee242912e4b010b8e485b713489e12`，分支`codex/lithic-v1-product-completion`。P02独立桌面实施审查PASS WITH NOTES；用户人工验收继续延期。本轮不是M22冻结或发行验收。

## 用户可见变化

- 设置新增“成本计算说明”“关于砺石与使用边界”。说明以PROJECT与原DOMAIN_CONTRACT C01/C03/C07/C08、UI_SPEC U01为依据：项目真实净投入/起止日/结束后结算，订阅30/90/365折算与生效/真实付款，首页有效贡献与实际扣款不同。仅说明，不新增计算器。
- 关于通过当前包的PackageManager读取应用Label、versionName、versionCode；API26/27和28+分支均编译，读取失败显示未知，不回填code17。包身份＋可调试标志只判断体验/调试状态，不把它当证书证明；不展示设备ID、私有路径或密钥。页面经原导航守卫返回设置。
- 导出新增明确的呈现类别和失败原因/残留元数据；仅真实`BackupExportResult.Success`发布已验证成功。选择取消、导出取消、选择器失败、媒体/来源校验、IO/目标校验失败和中断未知分别反馈；目标可能残留时提醒不能用于恢复。已验证成功但内部临时文件未清理，保留成功事实并警告。
- 恢复仅真实`RestoreResult.Success`发布成功；`currentPreserved=true`的失败说明旧记录已保留，false/确认异常或取消/进程重建为结果未知，提示核对且不要清除数据。选择/校验/预览取消在确认切换前才说明当前未改变。预览展示原有10表两侧计数、设置替换、不合并和完整覆盖范围；注明数量是校验时快照。陈旧确认仍交原快照校验拒绝。
- 复用原phase、锁、ViewModel保存标记、取消、重复回调和Token资格；恢复中不重复确认或开始第二项操作。使用P02文字标签＋颜色分级，保留原testTag/原始反馈节点、48dp与滚动。补充手动未加密备份、卸载风险、无自动云同步、提醒不保证准点、停止记录不等于服务商取消扣费。

关键代码：[设置](../../app/src/main/java/com/cooper/lithic/feature/settings/SettingsShell.kt)、[说明与版本模型](../../app/src/main/java/com/cooper/lithic/feature/settings/ProductInformation.kt)、[当前包读取](../../app/src/main/java/com/cooper/lithic/feature/settings/InstalledAppInformationReader.kt)、[说明页面](../../app/src/main/java/com/cooper/lithic/feature/settings/ProductInformationScreen.kt)、[导出反馈](../../app/src/main/java/com/cooper/lithic/feature/settings/BackupExportController.kt)、[恢复反馈](../../app/src/main/java/com/cooper/lithic/feature/settings/BackupRestoreController.kt)、[预览](../../app/src/main/java/com/cooper/lithic/feature/settings/RestorePreviewScreen.kt)，以及原LithicRoutes/LithicNavigation的设置入口接缝。

## 实际验证

| 项目 | 本轮结果与原始证据 |
|---|---|
| 修改前RED | 3个原API行为断言真实FAIL（导出2、恢复1），错误0；exit1/71.218秒。[原始输出](lithic-p03-20261025-132520/red-interrupted-feedback/stdout)、[JUnit原件](lithic-p03-20261025-132520/red-jvm/TEST-com.cooper.lithic.feature.settings.P03BackupExportFeedbackTest.xml)、[恢复JUnit](lithic-p03-20261025-132520/red-jvm/TEST-com.cooper.lithic.feature.settings.P03BackupRestoreFeedbackTest.xml)。 |
| 定向JVM | 最终92个唯一方法PASS：新增38＋原相关54，失败/错误/跳过0。包含原导出Controller15、恢复Controller10及相关导航/消息回归。[完整类/方法XML索引](lithic-p03-20261025-132520/jvm-result.json)。原3个RED同身份转GREEN，[对应记录](lithic-p03-20261025-132520/red-green-closure.json)。 |
| 构建与Lint | 首轮exit0/411.813秒；交叉审查后修正调试标志文案与预览快照说明，最终同组输入exit0/167.156秒，Debug构建＋AndroidTest Kotlin编译＋Lint。Lint0错误/0Fatal/17警告。[最终命令/原始SHA](lithic-p03-20261025-132520/final-green-build-lint/command.json)、[stdout](lithic-p03-20261025-132520/final-green-build-lint/stdout)、[stderr](lithic-p03-20261025-132520/final-green-build-lint/stderr)、[Lint XML](lithic-p03-20261025-132520/lint-results-debug.xml)。不累计重复执行为覆盖量。 |
| Compose/Android | 新增13方法（设置/说明6、反馈4、预览3）仅源码编译，实际运行均NOT_RUN；[唯一方法和编译类SHA](lithic-p03-20261025-132520/android-compile-only.json)。原Android套件、设备、安装、SAF、真实恢复、PackageManager运行/字体/IME/截图均未执行。 |

原测试源码和断言未修改；Controller JVM使用受控委托/目标验证呈现与调用资格，不代替Android文件关闭、SAF或原子切换实证。首轮与最终输出、输入SHA独立保留；[首轮汇总](lithic-p03-20261025-132520/first-green-result.json)、[最终输入](lithic-p03-20261025-132520/final-inputs.json)。

## 数据与构建边界

data/domain/platform、schema、Manifest、Gradle/依赖、两个备份ViewModel、Owner/RestoreCoordinator/generation指针/Token/事务未改；Room5与backup4和冻结规则不变。仅内存UI元数据，无新持久状态。媒体审查发现现有链路核验归属、长度/SHA、容器、尺寸并复制原字节；Android JPEG可读性不等于严格解码证明，未合并M10 Worker/JNI，未改变JPEG字节口径。

[身份/证书/Manifest审计](lithic-p03-20261025-132520/identity-scope-audit.json)、[边界审计](lithic-p03-20261025-132520/final-boundaries.json)：默认Debug/Release与Private Trial配置匹配P02；正常LithicApplication/MainActivity及自有Provider，无INTERNET/shared UID/Instrumentation。Debug证书SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`保持。

仅研发Debug产物`com.cooper.lithic.debug` / code17 / `0.1.0-m22-debug`，14,425,645字节，SHA-256 `0c24f6960a754c99a11f7b07d24f5ca149b045ad8227ddc3aa0b9a59b0c02a2b`；不作为用户体验版交付、未入Git。Test/Release/新Private Trial APK未构建。旧体验APK本地缺失仍NOT_AVAILABLE；不补造旧SHA或字节。原两份M22隔离APK字节仍匹配，无任何设备、旧账本或受保护现场访问。

## P04集中收敛前仍需处理

1. 本轮页面/导航/取消/未知提示与完整SAF及恢复实际行为仍待集中人工/设备验收；没有编译替代的PASS。包元数据读取与API26兼容也仅静态/编译，未做设备运行。
2. 保留M10三JPEG历史FAIL与媒体安全缺口、M20额外平台FAIL/第二收口BLOCKED/UNKNOWN、M22 T06/T03/真实IME未完成、M13-T06及G1/G2/G3等全部历史门禁；本轮未发现需要停止UI实施的确定性新数据丢失/不安全删除缺陷，不等于旧风险已解决。
3. P04如获授权，先集中核对现有功能/风险清单与受影响定向回归，再考虑统一体验。交付仍须`com.cooper.lithic.preview.debug`同证书、code至少18，重新构建实际唯一产物并复制到非build交付目录。本轮不提前实施、不安装、不要求用户立即体验。

已完成桌面实施；M22仍BLOCKED / CHANGES REQUIRED、人工验收延期，所有历史RED/FAIL/UNKNOWN/NOT_RUN/NOTES保留。提交推送当前分支后停止，不ACCEPTED/FROZEN、不合并main、不自动进入P04或M23。
