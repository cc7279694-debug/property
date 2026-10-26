# UI-C2 C1 · 真实Android视觉整改候选

2026-10-26。**UI-C2 C1 — READY_FOR_DEVICE_VISUAL_REVIEW / APP_VISUAL_PENDING**。Preview3的**VISUAL DELIVERY REJECTED — CHANGES REQUIRED**及其原构建/签名静态记录均保留；本轮没有授予VISUAL_ACCEPTED。

## 来源与范围

用户锁定C0 `342c3c355be886daf6a1c5e4931c35f22c13a6cb`；实际干净HEAD为`ed492b7ecf57071608274d849427a48b852046d5`，已包含用户刚确认的图标与Preview4/code20，因此保留其全部修改，不reset。本候选递增为Preview5/code21，不复用已交付code20字节身份。

已实际打开冻结UI-C1 `0dce7dd0725a54be4f0280870ef6ca47c48ea7ed`的[浅图](https://github.com/cc7279694-debug/property/blob/0dce7dd0725a54be4f0280870ef6ca47c48ea7ed/docs/design/ui-c1/source-v1/ui-reference-light.png)、[深图](https://github.com/cc7279694-debug/property/blob/0dce7dd0725a54be4f0280870ef6ca47c48ea7ed/docs/design/ui-c1/source-v1/ui-reference-dark.png)，并读取同ref原规格；它们是设计参考，不是本轮Android截图。两张用户Preview3实拍原件不在本轮可访问工作区，依据用户/ChatGPT明确问题描述实施，**不声称直接看过实拍**。[来源SHA及证据边界](ui-c2-c1-20261026-124116/evidence-boundary.json)。

LOCKED：原色板、业务数据/计算/事务、Room5、backup4、Owner、generation、恢复Token/指针、媒体和提醒协议、原导航守卫与签名。Adjustable：Home/Review空态、金额和单位组合、菜单密度、导航文字；无HTML生成、设备、安装或私人数据操作。

## 缺陷与生产修订

| 确定的问题 | 改前源码原因 | 本轮生产Compose |
|---|---|---|
| 空首页失去深绿卡与数量层次 | HomeShell把empty Surface设为background，并条件隐藏数量卡 | 所有状态保留深绿主卡；真实空Overview显示`— 元/天`和两张真实0数量卡；unknown显示—，旧快照持续说明；不产生假记录 |
| 占位金额与单位左右分离 | 金额在fillMaxWidth行里weight(1f)，单位被推到另一端 | HomeHeadlineAmount用实际TextMeasurer宽度预留单位，数字按所需宽占位、间距4dp；普通38sp优先，极长值仍可横向查看/完整展开；Formatter与金额精度不改 |
| 空回看变成说明与菜单墙 | ReviewHeading空说明、嵌入趋势empty、destinations无条件叠加 | 确认空库仅一个短引导区及真实“去添加记录”；说明放Info入口，无假图表/金额/年度结果。回首页继续原新增面板，经过requestLeave |
| 未知或迟到的零快照可能诱发空态 | 只靠计数不足以排除加载/错误/恢复提示与残留趋势事实 | 明确UI空态资格；Overview未取得、readError、恢复结果未知/RESTORING不当空库；内嵌summaryOnly不再另宣称empty；历史有记录且当前0贡献仍保留真实趋势 |
| 普通菜单与底部文字层级偏大 | 回看每行双层说明至少64dp；导航与辅助同用14/20sp | 菜单56dp最小、16sp标题，副说明移至语义/Info，原六入口及回调保留；导航独立12/16sp。页面24/32、主金额38/46、正文16/24与辅助14/20不整体缩小 |

触控区域仍至少48dp，菜单无固定最大行高；正文保持滚动。C2Page的大字独立返回/正文及IME底部安全结构没有回退。没有修改用户系统fontScale或density，**本轮手机实际fontScale为UNKNOWN**。360/400dp与2.0字号仅已准备LocalDensity测试源，未运行，不等于真实系统字号或IME通过。

主要生产文件：`HomeShell.kt`、`ReviewShell.kt`、`ReviewRecordSummary.kt`、`TotalCostTrendScreen.kt`、`LithicNavigation.kt`、`LithicTheme.kt`；Private Trial版本仅改两行。完整数据来源仍为同代Ready/Overview、既有项目/订阅预览和真实趋势VM，不新查DAO、不建Owner、不复制计算公式。`MainActivity → LithicNavigation → HomeShell/ReviewShell`接线与最终APK的C1标签/生产类均做静态核对。

## 实际验证

| 门禁 | 实际结果 |
|---|---|
| 定向JVM | **54 PASS**：6新增空态资格＋48相关金额/快照/导航回归，0失败/跳过；[逐方法](ui-c2-c1-20261026-124116/jvm-result.json)／[原始JUnit XML](ui-c2-c1-20261026-124116/jvm/) |
| AndroidTest源码编译、Debug构建 | exit0；新增14个生产Compose断言（Home8、Review6）仅编译，**运行NOT_RUN**；[命令/退出](ui-c2-c1-20261026-124116/debug-quality/command.json)／[原始输出](ui-c2-c1-20261026-124116/debug-quality/stdout)／[新增方法](ui-c2-c1-20261026-124116/compose-compile.json) |
| Debug Lint | **0错误21警告**；[原始XML](ui-c2-c1-20261026-124116/debug-lint.xml)。保留图标/旧依赖/现有API等警告，未升级或压制规则。Private Trial未重复Lint |
| Private Trial构建 | exit0；[命令/退出](ui-c2-c1-20261026-124116/trial-build/command.json)／[原始输出](ui-c2-c1-20261026-124116/trial-build/stdout) |
| 最终包静态检查 | 包名/版本/原证书、正常Application/Launcher、隔离Provider、无INTERNET、无测试宿主/Runner/JUnit、C1生产标签PASS；[审计](ui-c2-c1-20261026-124116/artifact-audit.json)／[签名输出](ui-c2-c1-20261026-124116/trial-signature/stdout) |
| 数据与历史产物保护 | [边界核对](ui-c2-c1-20261026-124116/boundary-audit.json)：数据层/引擎/Owner/恢复/资源/Manifest/依赖零改；Preview2/3/4及M22实物SHA未变；原图标也保留 |

新增测试没有在修改前实际运行，**不虚构RED→GREEN**。旧Review两项预期只同步空态tag/文案；负值、日期、事实和原回调断言均保留。本轮54项之外的历史JVM未重跑，137/36及T06/T03/IME历史不改。桌面产物审计首轮因辅助器路径分隔符严格比较拒绝，修正的是ignored辅助器的期望命令拼写，原命令/日志/APK未改、未重构或重测；[说明](ui-c2-c1-20261026-124116/audit-helper-note.json)。

## 真实Android逐页门禁

| 候选检查 | 必查内容 | 本轮 |
|---|---|---|
| A 深色／确认空库／首页 | 绿卡、紧凑—与单位、0项目/0订阅、单新增、无假记录 | **NOT_RUN / PENDING** |
| B 深色／确认空库／回看 | 一个短引导、真实返回首页新增、无菜单墙或假趋势 | **NOT_RUN / PENDING** |
| C 浅色／独立合成记录／首页 | 原排序/状态/精确成本、数量与记录预览、极端金额 | **NOT_RUN / PENDING** |
| D 浅色／独立合成记录／回看 | 真实曲线/选日/贡献和六入口；历史0贡献不为空 | **NOT_RUN / PENDING** |
| E 项目详情与基本录入 | 原来源返回、主指标、记录线、输入/错误/保存可达 | **NOT_RUN / PENDING** |

没有获准且安全可操作的Android渲染会话，本轮未启动/复用任何设备，不修复旧SystemUI ANR AVD。**没有新Android截图，也没有HTML候选。**E的生产页面及C2Page未改，相关旧Compose源码随全AndroidTest源码一起编译，不能据此判E视觉PASS。后续只有在新授权及隔离确认后，逐页取得真实Android图再对照原设计，才可申请VISUAL_ACCEPTED；不向用户账本注入合成记录。

## 视觉核对候选（不是正式交付验收）

- 本地唯一候选：`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/deliveries/lithic-preview-0.1.0-preview.5/Lithic-0.1.0-preview.5-debug.apk`，非build目录、APK不入Git。
- 包名 `com.cooper.lithic.preview.debug`；版本 `0.1.0-preview.5-debug`／code21。
- 15,732,019字节；SHA-256 `059fcf44ae09ee2840d7c96f3a567bcf429dd465f4956c3e965c688fd7bf5670`。
- 原Debug证书SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；没有生成密钥，默认Debug/Release/M22身份保持。
- 对应`app`树 `cdce231c73f68855c78e66ad11604e208bb4e64b`；[源码绑定](ui-c2-c1-20261026-124116/source-binding.json)／[最终文件](ui-c2-c1-20261026-124116/candidate.json)。

本轮未安装或验证同签名覆盖升级。由用户自行决定是否安装核对；如已有重要体验记录，保留原记录和已有备份，不以卸载/清数据解决问题。不把本候选当重要数据唯一副本或正式Release。APP_VISUAL_PENDING／MEDIA_DISPLAY_PENDING；M10三JPEG历史FAIL、M20平台FAIL/UNKNOWN、M22原BLOCKED/T06/T03/IME、M13-T06及G1～G4等全部历史状态保持。未合并main或进入M23。
