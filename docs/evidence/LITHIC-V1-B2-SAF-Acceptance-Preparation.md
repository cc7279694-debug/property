# LITHIC V1-B2 — 真实数据安全集中验收准备

2026-11-02。已接受源基线 `cf0512bbac1c4155efac7b8186ff573e49f97746`，新分支 `codex/v1-b2-saf-acceptance`，未合并main。本轮范围仅为独立正常App的构建配置、候选归档及未来人工路线；生产业务、测试断言及历史证据不改。**DESKTOP_PASS — READY_FOR_INDEPENDENT_REVIEW**，手机验收仍未获本轮授权。

## 候选与桌面结果

构建输入提交 `dc9b035c5d8971290235723ad9e1fa5659f65b56`，App树 `b1513e8fafd11b125071dcd12cd1a98638e19cdd`；最终文档提交继续保持该App树。

| 项目 | 唯一归档实物 |
| --- | --- |
| 绝对路径 | `E:\CODEX\.codex\worktrees\lithic-m13\砺石新APP\verification-candidates\v1-b2-b1qa\Lithic-B1-QA-0.1.0-b1qa.1-debug.apk` |
| 用途/身份 | **仅供独立数据验收，不是Preview升级包**；`com.cooper.lithic.b1qa.debug` |
| 版本 | code1 / `0.1.0-b1qa.1-debug` |
| 大小 | **20,506,567字节** |
| SHA-256 | `e52497e2968e9388d1f27f43a8e1aab1775a5c98871391f63417b8487aa95f00` |
| 原Debug证书SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9` |

独占创建非Gradle目录，复制后SHA及逐字节比较一致；遇到预存未知目录即拒绝覆盖。没有Test APK、ZIP或新Preview；APK/密钥不提交Git。[归档与门禁汇合回执](v1-b2-20261102/desktop-1/archive.json)。

- 显式开关 `-Pb1QaVerification=true`，严格true/false，独立 `app/build-b1-qa/`。仅新增 `app/build.gradle.kts` 配置、`src/b1Qa/AndroidManifest.xml` 标签覆盖与忽略路径，不修改main Kotlin/资源/schema/测试。与Private Trial、M22、C3、C6、V1-A1、PHOTO-LITE互斥，拒绝M13及进程死亡实验属性；禁用QA Release及AndroidTest变体，并拒绝相关执行任务。
- 候选身份 `com.cooper.lithic.b1qa.debug`，code1 / `0.1.0-b1qa.1-debug`，显示“砺石·独立数据验收 B1-QA”。正常 `LithicApplication → MainActivity → LithicApp`，开启原PHOTO-LITE开关；不提供plain Application宿主或Test APK。未来正常启动将创建**该包自己的真实业务Owner、Room、偏好和提醒运行时**，不能把它描述为无业务测试宿主。
- PackageManager层面的独立包名、无sharedUserId与包名限定Provider是静态依据；真实安装UID、私有目录归属及运行隔离 **ANDROID_NOT_RUN**。当前路径检查受本包Context/noBackupFilesDir约束，不向Preview/V1-A1/P1指定路径；不读取它们的个人数据。
- 继承已接受[R1](LITHIC-V1-B1-R1-Classification.md)的168 JVM PASS及Lint0E18W，功能源码未变，不重跑全套；原B1/P3 Android、真实SAF与完整恢复仍NOT_RUN。新配置门禁与实际QA构建/Lint以本轮回执为准。
- 锁定[Preview16](LITHIC-V1-B1-R2-Preview16.md)：code32 / `0.1.0-preview.16-debug`，20,506,555字节，SHA `e8b2e2fe7b2db3af986bc9818f1a02a9da583ba160b723e63befc7f4a1eb9e94`，源 `d91069e54d4c040b19a0b013c0b08b7293b4e96e` / App树 `b3eda991b828619317873401696a03ee4367774a`。原Debug证书 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。不重建/重包/覆盖；18份历史归档（含Preview16）前后大小/SHA必须一致。

本轮实际执行 `--offline -Pb1QaVerification=true :app:b2IdentityReceipt :app:assembleDebug :app:lintDebug`，单worker/原JDK17及SDK，**exit0、14分40秒、Lint0E18W**；正式命令前RAM5.394GiB≥4。49个任务执行，未调用clean或下载/升级依赖。[完整命令/退出码/日志指纹](v1-b2-20261102/desktop-1/build-3/command.json)、[原Lint XML及计数](v1-b2-20261102/desktop-1/lint/result.json)、[源码与继承19份JUnit XML/168项复核](v1-b2-20261102/desktop-1/source-checks.json)。本轮没有新业务JVM或AndroidTest编译/执行；不是把上游结果计作本次运行。

- **配置 DESKTOP_PASS**：QA/普通Debug/Private Trial三个实际配置求值exit0；普通Debug仍code17/`com.cooper.lithic.debug`/PHOTO=false，默认Release仍`com.cooper.lithic`及原版本/PHOTO=false，Trial仍code32/preview.16/PHOTO=true。两个预期拒绝（非法`TRUE`及QA与Trial并用）真实exit1并核对错误来源，未进入任何构建任务；不是产品编译失败。QA只有Debug变体、无AndroidTest变体，三个AGP测试汇总任务均无依赖；实际入口拒绝保留。[配置及原始命令](v1-b2-20261102/desktop-1/configuration.json)。未重新打包这些旧身份。
- **APK静态 DESKTOP_PASS**：完整Manifest、实际原证书、40个生产类/24个方法、正常Application/Main接线、PHOTO实物true、P3添加/更换/取消及封面UI、B1检查/统计与三主题入口均有直接断言。GenerationCoverReader及完整导出/恢复协调器不在40/24逐一点名反汇编清单中，按全部main实现与接受基线零差异及App树绑定继承，不扩称端到端证明。R1获取阶段及校验流程实际DEX与Preview16去调试信息后等价；无Instrumentation/M13 Runner/测试框架；包名限定Provider、无sharedUserId，原组件/export保持。权限与native除应用包名归一化后相同，无INTERNET/照片广泛权限/新JPEG库；原PNG图标及四份adaptive图标XML保持。[实际Manifest/签名/DEX/SDK退出码证据](v1-b2-20261102/desktop-1/artifact-1/identity.json)。这些是静态证据，不是动态安全或视觉通过。
- **历史保护 DESKTOP_PASS**：归档前后18份旧APK（含Preview16、V1-A1两包、P1及旧体验包）大小/SHA一致，已接受Preview16审计证据指纹也一致。Room5/backup4及全部生产/测试/schema/依赖输入未改；仅新身份配置/标签/忽略目录与本报告证据变化。

| 证据层级 | 当前结论 |
| --- | --- |
| DESKTOP_PASS | 新身份配置、QA构建/Lint、APK静态审计、稳定复制、旧APK保护、人工路线准备 |
| ANDROID_NOT_RUN | 安装/启动、A～H、真实SAF检查/导出/恢复、照片与统计显示、实际重新打开、AndroidTest；未取得新Android截图 |
| UNKNOWN | 设备是否已有同名QA包、实际UID/私有目录与动态隔离、Owner/文件关闭、资源峰值、异常中断、逐图恢复身份及字节一致性 |

保留两次**检查器失败**：[原记录](v1-b2-20261102/desktop-1/configuration-checker-history.json)。第一轮误认为禁用AndroidTest应使所有汇总任务消失；第二轮已观察真实`testVariants=[]`、三个AGP汇总任务各依赖为空，但Groovy Set与List比较误报。分别保留原init脚本、exit1及原始日志；只修检查器为同类型精确任务比较并核验无实际变体/依赖，未改产品或放松构建禁令。两次均停在配置任务、尚未编译/打包；上述build-3才实际构建唯一QA App。

## 未来一次连续人工路线（A～H，当前全部ANDROID_NOT_RUN）

**执行前提**：先由ChatGPT独立审查本候选，再取得用户对指定手机、该唯一APK安装及下面SAF合成数据操作的明确授权；本任务不授予这些权限。不沿用历史手机序列/PID/安装观察。未来先复核包名、实物SHA、独立UID/目录身份及健康，发现同名旧QA包或非空未知账本即停，不覆盖/清数据。API36、真实系统Photo Picker，三张本地可丢弃合成图片α/β/γ（沿用640px、PNG小于2MiB边界）；不得使用真实私人照片。不给未知照片导入扩大兼容结论。

记本轮唯一编号 **Q（UUID）**、手机观察日期 **D**、App记录时区 **Z**。每次保存/确认仅一次，抄录实际结果；中断、UNKNOWN、金额或引用差异、Owner/文件状态不明即停止所有依赖步骤，保留现场，**不重试、不强停、不卸载、不清理**。未执行项保留NOT_RUN，不按用户未反馈推定PASS。所有操作只在名为B1-QA的独立App中进行。

| 步骤 | 操作及应记录的事实 | 预期与停止条件 |
| --- | --- | --- |
| A 创建 | 空账本确认后，创建`Q-A`物品：初始投入100元，开始/付款日D；创建`Q-B`物品：明确输入0元，开始/付款日D；创建`Q-S`月付订阅：测试付款30元，付款/生效D。抄录订阅到期边界E、提醒日R和状态，保存实际字段清单 | 2物品、1订阅，未进入其他App。月付默认E为D加1日历月、覆盖[D,E)，不能以30天折算推定D+30；用户合法记录的实际边界以表单为准。若同日观察，A持有1天/100元每天；跨日按实际观察日核对，不能固定假定仍1天 |
| B 照片 | A添加α，预览后点“保存为封面”；A更换β并确认；B添加γ并确认。每次返回列表和详情核对，同一图片操作只确认一次 | 已登记/不同当前引用/历史未引用依次1/1/0→2/1/1→3/2/1。A显示β、B显示γ，旧α不再引用但仍登记；取消或失败不得覆盖原封面。若意外多登记或迟到覆盖，立即停 |
| C 统计 | 设置“照片存储情况”，必要时在前一操作已结束后点一次“重新读取”。抄录四项原值与时间，登记长度合计记为S字节 | 3/2/1/S；S是媒体登记长度估算，**不是物理磁盘占用，也不证明逐文件已验证**。引用统计包含物品和订阅及历史记录、相同媒体只计一次；本路线未构造订阅共享引用，不能将该运行场景标PASS |
| D 导出 | “导出完整备份”，在用户选定测试文件夹创建唯一`lithic-b1qa-Q-B0.lithicbackup`；绝不覆盖既有文件。记录提供者、文件名、时间、大小和完整反馈 | 仅真实“完整备份已导出并通过校验。”及其真实残留提示作证。覆盖提示则取消并停；失败/残留未知/中断即停，不用残缺文件继续。备份未加密，仅保存在用户选定位置，不提交Git |
| E 检查 | “检查备份文件”选择刚生成的B0，记录状态与摘要；之后复看A/B/S事实、封面及统计 | 仅真实 `VERIFIED · 文件符合当前完整性规则。`：format4、物品2/订阅1/照片3/登记长度S；业务仍100/0/30、β/γ、3/2/1/S。INVALID/UNSUPPORTED/UNKNOWN各保留原反馈并停；**VERIFIED不是恢复成功** |
| F 修改 | A“记一笔→记录投入”20元，真实发生日期D，保存一次；A“取消封面显示”明确确认一次。保留修改后清单及列表/详情结果 | A投入120/收回0/净投入120，仅新增20一笔；A无封面、B仍γ、统计3/1/2/S。取消显示不等于彻底删除照片，媒体登记和长度不减少。未知写入或重复记录即停 |
| G 覆盖恢复 | **现场另获用户明确确认：只允许用B0覆盖本轮Q的B1-QA合成账本。** 然后“选择完整备份”→B0→“核对备份”，记录全部当前→备份数量和覆盖警告，再仅点一次“确认覆盖恢复” | 保持原校验→摘要→确认流程；不能用E的VERIFIED绕过。仅真实“恢复成功，已重新读取恢复后的本地记录。”作成功来源；失败须保留原数据是否保存/结果未知的实际提示，立即停，不重复确认或自动恢复 |
| H 重开与对照 | 先逐项对照B0清单，再正常返回桌面，重新打开准确B1-QA图标，重复对照A/B/S、付款/生效/E/R、状态、金额事件、β/γ及统计 | A只剩初始100、没有20，B0元，S原30元及原日期；Aβ、Bγ、3/2/1/S。派生成本按当前观察日重算，不能把跨日差异直接当恢复损坏。普通退出重开**不证明进程死亡、Owner优雅关闭或异常中断恢复** |

### 照片与备份证据边界

`BackupMediaSnapshot`沿全部已登记媒体复制/校验，不只复制当前封面。因而B0摘要应包含α、β、γ共3份登记媒体；F取消显示后旧文件仍保留。本路线可核实可见封面、计数和登记长度，但UI不展示assetId/每图SHA/像素尺寸，也不能从3/2/1/S推定历史α已逐字节恢复。

若H成功且后续另有明确SAF权限，可新导出唯一B-H文件并检查3份媒体/S，以验证恢复后旧登记媒体仍可完整导出；**不自动追加执行**。要证明α身份及字节不变，须另行限定只读比较本轮B0/B-H合成备份的media身份、长度、SHA及内容，不能读取App私有目录或个人备份。本轮上述强比较、真实文件关闭及资源峰值为 **UNKNOWN/NOT_RUN**，不以统计或JUnit代替。

### 一份记录模板（只记录必要合成事实）

会话Q：____；候选SHA：____；授权范围/时间：____；确认包名/UID：____；D/Z：____；α/β/γ来源为可丢弃合成图片：____。

| 步骤/时间 | 状态（PASS/FAIL/UNKNOWN/NOT_RUN） | 操作与实际确认次数 | A/B/S金额、事件、日期E/R、状态 | A/B封面 | 登记/引用/历史/S | 唯一备份名及逐字反馈 | 证据编号、差异/停止原因 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| A～H逐步填写 | NOT_RUN | — | — | — | — | — | — |

G覆盖单独确认原话/时间：____；当前→备份十项数量：____；H前后差异：____；未完成项：____。截图若未来授权，只拍本独立合成App，排除通知/私人内容；不公开手机USB序列、个人媒体、真实备份或路径。

## 停止边界

本轮ADB、设备查询/安装/运行、AndroidTest、SAF、覆盖恢复、个人数据操作均0。生产功能、Room5、backup4、金额/日期/订阅规则、Owner/generation/Token/原子切换、照片P3/C6-1、三主题/图标、原版本/签名行为保持。没有GC、没有新Test APK、没有新Preview或照片安全认证。C6临时App缺失历史NOTE不重建。

**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING**及历史M10/M20/M22/C3/G1～G4门禁保持。桌面候选完成后只提交推送本B2分支，停止等待ChatGPT独立审查；不自行安装、恢复或进入下一阶段。
