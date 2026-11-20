# LITHIC_UI_V4_R1_APK_IDENTITY_DIAG_01

日期 2026-11-20（Asia/Shanghai）。**电脑端 APK 身份与回看代码核查通过。手机上实际运行的包和版本仍待确认。**

本次依据用户描述的录屏现象开展排查，未读取录屏原件。固定 R1 源码为 `e3891e3ad26ccfd862bbb0118dc4339b1e0530f6`，原交付证据为 `b756bd9354cf7bdbb75222c0b9f29d834f0ed67d`。执行前 R1 工作区干净，远端 R1 指向原证据提交。两者之间 `app/` 的 Git 差异为零。[原交付报告](LITHIC-UI-V4-R1-REVIEW-TREND-IMPLEMENTATION.md)

## 1. 本机安装包核查

| 项目 | V4-R1 | V4-B2-R1 |
| --- | --- | --- |
| 原始路径 | `E:\CODEX\lithic-v4-r1\app\build\outputs\apk\debug\app-debug.apk` | `E:\CODEX\lithic-v4-b2-segment-repair\app\build\outputs\apk\debug\app-debug.apk` |
| 文件大小 | 20,623,590 字节 | 20,590,822 字节 |
| SHA-256 | `3436682bfc7475b6a72038793dd5398e369850be4238256ad7a9261105b2ee51` | `b9a09f25141b27b3a5732475f0bc6bc9dbe3b2ff92d60c0180036eda38dd37d2` |
| applicationId | `com.cooper.lithic.debug` | `com.cooper.lithic.debug` |
| 版本 | `0.1.0-m22-debug`（17） | `0.1.0-m22-debug`（17） |
| 默认名称 | 砺石 LITHIC Debug | 砺石 LITHIC Debug |
| 入口 | `com.cooper.lithic.app.MainActivity` | `com.cooper.lithic.app.MainActivity` |
| 签名 | 单一 Debug 签名者，APK v2 验签通过 | 单一 Debug 签名者，APK v2 验签通过 |

用 `Get-FileHash -Algorithm SHA256`、SDK Build Tools 36.0.0 的 `aapt dump badging` 和 `apksigner verify --verbose --print-certs` 核查。两份 SHA-256 分别与各自固定交付记录完全一致。证书 SHA-256 同为 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。两份包的 ZIP 条目 CRC 检查也通过。[B2-R1 原交付](LITHIC-UI-V4-B2-SEGMENT-PRESS-REPAIR-01.md)

只读查看了本机已归档的正式 1.0.1。其包名为 `com.cooper.lithic`，版本号 37，默认名称「砺石 LITHIC」。正式包和 Debug 包是两个应用身份，可以共存。同一 Android 用户或资料中的旧、新 Debug 共用包名和签名，版本号、显示名称、图标及证书均不能用来区分它们。小米14上的实际安装清单仍未知。[正式包归档报告](LITHIC-RELEASE-01.md)

## 2. APK 内实际代码

直接从两份 APK 解包 `classes*.dex` 至临时目录，用 SDK `dexdump -d` 反汇编包含目标类型的条目。每份各 15 个 DEX。输出中的位置为该 DEX 内十六进制偏移，以下依据指令及其所属类，不以当前 Kotlin 文本或 APK 字符串搜索代替。

| 证据 | V4-R1 实物所见 | B2-R1 实物所见 |
| --- | --- | --- |
| `ReviewShellKt` | `classes6.dex` 中摘要组合调用 `ReviewTrendSummary`（`0x04132e`）。摘要分支调用 `ReviewSingleDate`（`0x0425de`）或 `ReviewCostPreview`（`0x042646`）。预览高度为 84dp（`0x04391e`）。 | `classes6.dex` 中 `ReviewShell` 直接调用旧版 `TotalCostTrendScreen`（`0x042e3a`）。旧图表选择 172dp／240dp（`0x0507dc`／`0x0507ee`）。没有上述两个新摘要方法。 |
| 历史数量 | `ReviewHeading` 分别装载 `review-summary-project-count`、`review-summary-subscription-count` 和 `review-summary-scope`（`0x040e72`／`0x040f08`／`0x040f32`）。 | 无这三个页头标签。 |
| `TotalTrendPresentationKt.trendHasSingleDate` | `classes6.dex` 的 `0x05c9ca` 起读取 `evaluatedDayCount`、与常量 1 比较、随后取 `from` 和 `asOf` 并比较相等。该函数由摘要（`0x048eca`）和完整趋势（`0x0569d2`）调用。 | 同名方法及调用均未发现。 |
| `TotalCostTrendScreenKt` | `classes6.dex` 中单日 UI 装载 `trend-single-date-facts`（`0x04f266`）和「趋势范围只有一天」（`0x04f51e`）。展示单日截止信息后在 `0x04f59e` 跳过 `TotalCostChart` 调用（`0x04f616`）。 | 单日标签与对应文案均未发现。存在旧版大图路径。 |
| 实际导航调度 | `classes8.dex` 的 `LithicNavigationKt` 分别调用新版 `ReviewShell`（`0x04b8e6`）和新版 `TotalCostTrendScreen`（`0x0540d4`）。 | 该类调用旧版 `TotalCostTrendScreen`（`0x0540e8`），与 R1 入口调用签名不同。 |

涉及页面主要代码的 `classes6.dex` SHA-256 在 R1 为 `63bc88796667f18b741c825fad3f726e8583022ddeedda59ce8f1fbd9bb07240`，在 B2-R1 为 `d8c6e545b0dd9149a218fe639eae0a4d1608f73a18c5e02cdf32fa94412abb86`。DEX 中的新入口、分支和文案均与固定 R1 源码吻合。[回看源文件](https://github.com/cc7279694-debug/property/blob/e3891e3ad26ccfd862bbb0118dc4339b1e0530f6/app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt#L94) · [完整趋势](https://github.com/cc7279694-debug/property/blob/e3891e3ad26ccfd862bbb0118dc4339b1e0530f6/app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendScreen.kt#L177) · [单日判据](https://github.com/cc7279694-debug/property/blob/e3891e3ad26ccfd862bbb0118dc4339b1e0530f6/app/src/main/java/com/cooper/lithic/feature/review/TotalTrendPresentation.kt#L69)

## 3. 对录屏现象的判读

有记录、趋势数据可用且观察区间确实只有一个自然日时，R1 的完整趋势显示「趋势范围只有一天」及总额和分项，没有大 Canvas。只有一笔记录、只选中某一天、金额长期相同，都可能仍有多天观察区间。可在趋势图下方或说明中核对起始日与结果截止日。多日路径保留 240dp 大图及选中日期竖虚线。这两个特征单独出现时，无法凭画面区分新旧包。回看首页在确认为空时会省去数量行。有记录时应优先用页头「含历史」和「全部历史与事件」辨认 R1。[采样区间源文件](https://github.com/cc7279694-debug/property/blob/e3891e3ad26ccfd862bbb0118dc4339b1e0530f6/app/src/main/java/com/cooper/lithic/domain/report/TimelineSampler.kt#L24)

| 分类 | 结论 |
| --- | --- |
| A · 本地交付 APK 构建内容错误 | 排除本次指定文件缺少 R1 回看或单日路径的情况。实际 DEX 含对应实现，导航调度也指向新版。 |
| B · 手机可能安装了另一份 APK | 可能，尚未证实。旧、新 Debug 的可见身份完全相同，原始交付名也都是 `app-debug.apk`。 |
| C · 手机可能打开了另一包名 | 可能，尚未证实。正式包与 Debug 包可共存；录屏对应的包名、当前资料及是否共存均待核实。 |
| D · 当前证据不足 | 对手机侧的最终归因仍是此项。未查到手机已装 APK 的实际 SHA-256、录屏前台包名，也未确认录屏的真实观察区间。 |

## 4. 不用 ADB 的确认方法

1. 电脑端使用明确标记的 `Lithic-V4-R1-e3891e3-code17-debug.apk`。副本的本机位置为 `E:\CODEX\lithic-v4-r1-local\identity-diag-01\Lithic-V4-R1-e3891e3-code17-debug.apk`。复制前后 SHA-256 均为本报告的 R1 哈希。原 APK、签名和字节保持原样。若传输工具能校验 SHA-256，核对收到的文件。
2. 用户可自行在小米14的「设置 → 应用 → 应用管理」搜索「砺石」，看是否同时列出「砺石 LITHIC」和「砺石 LITHIC Debug」。若系统应用详情显示包名，核对 `com.cooper.lithic` 与 `com.cooper.lithic.debug`。若详情只给出名称和版本，截图只能提供线索，同为 17 的两个 Debug 仍分不出来。
3. 如选择重新确认安装，从手机文件管理打开上述精确文件。安装器若显示目标为「砺石 LITHIC Debug」，可用同签名更新路径。安装成功后直接从安装器点「打开」，避免从桌面误开正式包。若目标名称与预期不符、更新失败、提示必须先卸载，或安装器没有「打开」，在该步骤停止；保留现有应用和账本。
4. 在已有记录时检查回看页头的「含历史」和「全部历史与事件」。打开成本趋势，核对真实起始日和截止日，再检查是否满足单日条件。若仍想核对，提供安装文件名、成功页面及回看页头等必要画面即可，金额或记录名称可以遮住。无需为本次检查新建或修改真实记录。

以上为用户可选的本地确认步骤，实际手机安装和运行核验为 `NOT_RUN`。此前 Android 验收状态未升级。未进行 ADB、手机、安装、重新构建或生产源码操作。报告交付后停在 R1 等待独立审查。
