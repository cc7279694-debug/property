# 验收登记

本文件记录M00～M03历史检查、实际执行与独立验收；[原130条Markdown矩阵](planning/original-v1/lithic_v1_plan/docs/ACCEPTANCE_MATRIX.md)及[原JSON](planning/original-v1/lithic_v1_plan/docs/acceptance-cases.json)完整归档且全部保持NOT_RUN，不在原件上改结果。

日期：2026-10-11。PASS为已有执行记录的实际通过，NOT_RUN为未执行，PENDING为尚待确认的事项。独立验收/冻结结论由独立审阅提供，执行者不能自行授予。当前M03正式结论为 **ACCEPTED / FROZEN — PASS WITH NOTES**，依据见末尾M03独立复验记录；下方初次交付和补测的READY_FOR_REVIEW、CHANGES REQUESTED、PENDING是当时历史，不作为当前结论或后续授权。本次仅回写两份状态文档，不重跑Gradle/构建/设备验证；未运行边界及M02/M03独立审查未重跑Windows Gradle的NOTES保留。

## 独立验收记录

- 记录日期：2026-10-11。
- 结论：**ACCEPTED / FROZEN — PASS WITH NOTES**。
- 来源：用户在当前会话明确转交的ChatGPT独立验收结论；本次Codex仅回写结果，不自行授予验收，也不重新运行或代替独立审阅。
- 仓库：cc7279694-debug/property。
- 评审分支：feat/m00-planning-freeze。
- 评审HEAD：59ba290cba69e1e77af7d0376a8b43aaabbf5f88；回写前已实际确认本地及远端功能分支均匹配。
- 独立核对范围：原始ZIP、43个归档原件、24份模块计划、130条案例、规格文件、146个本地链接和Git变更范围。
- Notes：Q01已关闭；Q02～Q07按[SPEC_ISSUES](SPEC_ISSUES.md)保留状态和模块期限，不阻塞M00冻结与M01启动准备。具体建议未因此自动获得批准，工程未实施；M01仍需另行授权。

| 问题 | 保留状态 | 保留处理期限 |
|---|---|---|
| Q02 | PROPOSED | M02前明确，关联M17/M18 |
| Q03 | PARTIALLY_RESOLVED_BY_SOURCE | 长期100/1000日节点M21前明确，不整体阻塞M03；首日口径有原案例依据，实现测试仍NOT_RUN |
| Q04 | PROPOSED | M20前明确，关联M03/M21 |
| Q05 | PROPOSED | M10/M12前明确，关联M11 |
| Q06 | ENGINEERING_REQUIRED | M04设计、M12实施与故障验证；尚未设计/实现/验证 |
| Q07 | OPEN | M02等受影响模块执行前明确通用模板适用性 |

本次授权只回写当前状态与本登记、检查并提交/推送功能分支；未修改产品/补测结果或问题清单。规划及历史checkpoint中的“未冻结/PENDING”保留为当时交付记录，当前正式结论见本节。

## M00 规划补齐交付的文档验收

以下保留评审基准59ba290时的实际检查记录；其中“未冻结/PENDING”是补齐交付时状态，不是当前独立验收结论。

| ID | 条件 | 本轮结果 | 证据 / 说明 |
|---|---|---|---|
| M00-D01 | 文档入口清晰、当前记忆职责唯一 | PASS | 57份Markdown、146个本地文件链接、0失效；归档旧同名文件只作来源，人工职责核对通过 |
| M00-D02 | 产品、原生路线和明确不做清单保持 | PASS | 六规格及原DECISIONS/SOURCES规范化全文均匹配阅读版；核对当前PROJECT/AGENTS，D01～D15明确是原规划补充默认 |
| M00-D03 | 完整读取M00计划、130矩阵及执行规范 | PASS | 实际读取ZIP43文件，CRC/严格UTF-8/3 JSON解析与大小/哈希/清单一致；原计划、矩阵、模板已接入并审查 |
| M00-D04 | 24模块顺序、原依赖、边界与迁移配对 | PASS | 24唯一编号按M00～M23；计划/index/ROADMAP直接依赖一致、无环；130原案例在计划/矩阵/JSON逐条一致 |
| M00-D05 | 问题来源、建议与补测明确，不改变规则 | PASS | Q01关闭、Q03部分来源澄清；16组补测仍NOT_RUN，Q07登记；人工核对引用/输入/闸门，无新产品功能 |
| M00-D06 | 状态可恢复、停止边界与独立验收明确 | PASS | 唯一实时CURRENT_STATE；本轮未冻结，M01～M23 PLANNED且NOT_STARTED；历史checkpoint未改 |
| M00-D07 | ZIP/43原文件/阅读版保持来源字节 | PASS | 附件/归档ZIP SHA-256一致，43原件逐字节/CRC/manifest匹配；45原件文件的工作blob与暂存blob完全相同 |
| M00-D08 | 只有M00资料和文档，无Android/业务/旧仓库改动 | PASS | 本轮60项暂存变更：12修改、48新增、0删除；全部为Markdown/规划JSON/原ZIP及.gitattributes，无业务代码 |
| M00-D09 | 链接、结构、敏感信息与差异检查 | PASS | 146链接0失效、57文档代码围栏无失衡、敏感模式0命中；人工差异审查及新写文档git diff检查通过，归档硬换行原样保留 |
| M00-D10 | 现有功能分支提交及推送、main未合并 | PASS | 主体daf9615792cf85bf2521a98879b8654fbd79925e已推送，实际功能远端SHA匹配、工作区干净、main仍145326d；随后只登记交付证据 |
| M00-D11 | 等待独立验收，不自动ACCEPTED/FROZEN/M01 | PASS | 本轮仅READY_FOR_REVIEW；独立验收仍PENDING，M01须用户另行授权 |

## 原 M00 三项文档案例的本轮执行记录

记录独立于归档NOT_RUN字段；不把本次结果写回原130案例。

| 原ID | 本次实际检查 | 本轮结果 |
|---|---|---|
| M00-T01 | PROJECT/AGENTS明确禁止旧账户/股票/量化/云同步、不复制WHOCARE；60项本仓库文档变更范围已核对 | PASS |
| M00-T02 | 当前M01～M23 PLANNED且NOT_STARTED，无APK/工程/伪造产品PASS；原模块索引仍24项PLANNED | PASS |
| M00-T03 | 当前DECISIONS逐项继承原D01～D15，15行与原默认解释一致，明确不冒称用户逐条确认 | PASS |

## M00 历史验证范围与尚未执行

本轮已检查来源完整性、路径安全、逐字节一致、相对链接、24模块/依赖、130原案例一致性、六规格阅读版一致、当前文档边界及主体Git提交/推送。11项文档检查和3项当前M00文档案例均PASS；详细实际证据见[本轮checkpoint](checkpoints/2026-10-11-M00-planning-package.md)。

原130案例（包括归档M00记录）全部NOT_RUN；其中适用M00三项文档检查仅在上表记录本轮结果。M01～M23产品验证及16组补测未执行。

TypeScript、ESLint、Unit Test、Integration Test、Build、Android Build、APK Manual Verification：NOT_RUN。原因：M00仅文档，没有产品工程或业务代码；不为满足原通用完成模板提前开发未来备份或运行M01。

当前独立验收：ACCEPTED / FROZEN — PASS WITH NOTES，依据见上方独立验收记录。原130案例与16组补测仍全部NOT_RUN；M01～M23仍PLANNED且未实施，本次状态回写后停止。

## M01 独立验收记录（2026-10-11）

- 结论：**ACCEPTED / FROZEN — PASS WITH NOTES**。
- 来源：用户在当前会话明确转交的ChatGPT独立验收结论；Codex本次仅回写，不自行授予验收或代替独立审阅。
- 仓库：cc7279694-debug/property。
- 评审分支：feat/m01-android-skeleton。
- 评审HEAD：`ca8499e3d3cb2e5bac3ab29a231f399636899c19`；回写前实际确认本地与远端功能分支均匹配，工作区干净。
- 独立审查确认：单 `:app`、单Activity、Compose/Navigation 3空壳符合M01范围；20项已提交构建输入SHA-256独立比对一致；正式与Debug applicationId正确；Manifest、系统备份排除配置及权限核查脚本符合阶段要求；构建、Lint、模拟器安装和离线冷启动的既有执行证据已审阅；Git范围26新增、13修改、0删除，未进入M02或M05。

保留NOTES：

1. 模拟器证据已审阅，独立审查未重新安装APK；不新增设备验证PASS。
2. 真实手机、API26运行、OEM备份行为仍NOT_RUN；其他未运行产品验证及16组补测继续NOT_RUN。
3. Lint的12条版本更新提示暂不处理，未抑制或升级依赖。
4. Q02～Q07继续按既有状态和模块期限处理，不因M01冻结而关闭或标记已实施。
5. M02执行前必须明确Q02历史观察日公式和Q07通用验收模板适用性，且须用户另行授权；本次不开始M02。

本次授权只更新唯一CURRENT_STATE和本登记，检查差异后提交/推送当前M01功能分支。原始规划、130案例、问题清单和历史checkpoint不改；历史checkpoint的READY_FOR_REVIEW/PENDING是当时交付状态，本节记录随后获得的独立结论。本次回写的最终提交SHA及远端校验以Git日志/最终报告为准。

## M01 当前执行验收（2026-10-11）

授权：此前用户M01执行指令；执行基线 `77e61b653ebf1ce4838b4acce4b5b366e17891f4`；功能分支 `feat/m01-android-skeleton`。M00独立验收历史不变。本节保留Codex既有实际验证结果，交付时为READY_FOR_REVIEW；随后用户转交的M01独立结论为 **ACCEPTED / FROZEN — PASS WITH NOTES**，依据见上节，本次未重跑测试。

| 原案例 | 实际结果 | 证据 / 边界 |
|---|---|---|
| M01-T01 empty_app_cold_starts_offline | PASS（模拟器） | 专用API36/Android16模拟器安装Success；设备airplane=1、wifi=0、mobile_data=0、Active default network=none；force-stop后无进程，am start -W为COLD/Status ok；截图/UI三个文案核对；返回与重新冷启动无应用崩溃；网络状态恢复。未验证用户真机或API26 |
| M01-T02 merged_manifest_has_no_internet | PASS | aapt2检查最终APK及合并Manifest：无INTERNET、两种精确闹钟、三种广泛存储、通知/相机/照片权限；唯一签名级内部接收器权限来自AndroidX Core 1.16.0；allowBackup=false，旧规则/cloud-backup/device-transfer编译资源各排除九域；下载字体自动初始化入口已从最终包移除；不等于所有OEM行为验证 |
| M01-T03 debug_id_is_isolated | PASS | 最终APK为com.cooper.lithic.debug，名称砺石 LITHIC Debug，versionCode=1、versionName=0.1.0-m01-debug；正式namespace/applicationId配置com.cooper.lithic；APK为本地Android Debug证书，未构建正式发行包 |
| M01-T04 versions_are_locked | PASS | 精确稳定目录与官方Wrapper/JAR/分发校验值核对；Gradle8.14.3/JDK17实际输出；根assembleDebug + lintDebug退出0；debugRuntimeClasspath实际解析一致。版本来源见工具链锁，不声称重验现有缓存原ZIP |

具体输出、日志位置、APK/截图哈希及命令见[M01证据](evidence/M01-verification.md)与[M01 checkpoint](checkpoints/2026-10-11-M01.md)。模拟器已安装的base.apk与最终本地APK SHA-256一致。

另行验证：lintDebug PASS（0 errors / 12版本更新提示，未抑制）；最终APK apksigner verify退出0，v2签名通过；原ZIP/43原件字节、130 NOT_RUN案例、Q02～Q07原状态及补测未改变；文档链接/差异/敏感信息检查在交付checkpoint记录。

NOT_RUN：业务Unit/Integration Test、Android Instrumentation Test、真实手机、API26运行、OEM备份/迁移行为、业务schema/migration/完整备份往返。原因：M01为原生空壳，无业务数据与对应实现；完整备份往返不适用且未实现，不标Backup PASS。TypeScript/ESLint不适用于本原生工程。

原130案例保持NOT_RUN（含归档M01），既有执行结果只登记本节；M02～M23及16组补测保持NOT_RUN。Q02～Q06状态/期限不变，Q07只明确M01的模板适用边界，不关闭其他模块。M01已按用户转交的独立结论冻结；M02未授权，执行前须明确Q02/Q07，不自动进入。

## M02 独立验收记录（2026-10-11）

- 结论：**ACCEPTED / FROZEN — PASS WITH NOTES**。
- 来源：用户在当前会话明确转交的ChatGPT独立代码验收结论；Codex本次仅回写，不自行授予验收，也不以本地辅助审查代替独立验收。
- 仓库：`cc7279694-debug/property`。
- 评审分支：`feat/m02-project-cost-engine`。
- 评审HEAD：`ff1a898eaf4da5068c36af3e1c0bdcbe61c0b1f7`；回写前实际确认本地与远端M02功能分支均匹配，工作区干净。
- 独立检查范围：金额解析、日期计算、项目成本引擎、31项JUnit测试源码、RED/GREEN证据、核心文件哈希及Git范围；未发现阻塞性代码问题。

保留NOTES及后续边界：

1. **本次独立审查未在用户Windows本机重新运行Gradle测试。**本次Codex状态回写同样不重跑Gradle、构建或设备验证；下节保留此前实际执行结果，不把代码/证据审查记为新测试PASS。
2. 原130条规划案例和16组完整跨模块补测仍NOT_RUN，原件不可变；Q02的M17/M18历史视图/总成本集成仍NOT_RUN，不因领域冻结而提前通过。
3. Q03～Q06状态和处理期限保留：Q03长期100/1000日节点M21前，Q04提醒边界M20前，Q05图片往返M10/M12前，Q06持久回退M04设计/M12实施与故障验证，均未在本次实施或关闭。
4. 未运行的Instrumentation、真机/API26/OEM、数据库/迁移/业务备份往返和其他未来能力保持原NOT_RUN / N/A边界；12条既有Lint版本提示不处理。
5. M03尚未授权；本次只更新唯一CURRENT_STATE与本登记、提交并推送当前M02分支，不修改业务代码/WHOCARE，不合并main或进入M03。

M02历史checkpoint与证据的READY_FOR_REVIEW/PENDING是当时交付状态，不改写原记录；当前正式结论见本节。验收回写提交自身SHA、推送与最终远端校验以Git日志/最终报告为准。

## M02 当前执行验收（2026-10-11）

此前用户已授权M02并正式确认Q02/Q07。实施起始基线 `a25a06899bd157988c3a4466d7162d8616fb0249`，分支 `feat/m02-project-cost-engine`；只实现纯Kotlin领域规则，保留M01空壳。本节保留既有实际验证结果，交付时为READY_FOR_REVIEW；随后用户转交的独立结论为 **ACCEPTED / FROZEN — PASS WITH NOTES**，依据见上节，本次未重跑测试。历史阶段限制与本次状态回写授权区分，实施契约见[M02契约](planning/M02_CONTRACT.md)。

| 原案例 / 当前补测 | 实际结果 | 实际断言与边界 |
|---|---|---|
| M02-T01 `same_day_is_one_day` | PASS | 01-01投入1000元/持有1天/1000元每天，初始投入只累计一次 |
| M02-T02 `dated_event_does_not_rewrite_earlier_days` | PASS | 01-11追加200，01-10仍1000/10=100；01-11为1200/11的DECIMAL128结果 |
| M02-T03 `end_day_removes_current_contribution` | PASS | 01-10结束/完整10天/日均100，结束日日末贡献false |
| M02-T04 `post_end_refund_keeps_duration` | PASS | 结束后01-20收回200，净800/10=80；分母固定/不重新活跃 |
| M02-T05 `money_precision_and_zero` | PASS | 0.10+0.20经过解析和实际引擎得30分/0.30元，0有效；超精度/单笔超限拒绝 |
| M02-T06 `negative_cost_is_not_clamped` | PASS | 投入5000收回6000，净-1000元/-100元每天，不钳0 |
| Q02-V01（本轮领域部分） | PASS | 已结束快照回看开始前无有效日均/贡献，01-02为2天/500，01-09为9天/1000÷9 |
| Q02-V02（本轮领域部分） | PASS | 01-10/19为100、01-20/31为80，10天固定，结束日起无贡献 |
| Q02-V03（本轮领域部分） | PASS | 同日开始结束为1天/日末退出，补录结算仅按发生日改变后续结果，不延长/重启 |

实际测试：31通过/0失败/0错误/0跳过，7金额解析+2时钟+22项目引擎。RED为同一标准命令31项/1通过/30 AssertionError失败；初次classloading环境错误不算RED。覆盖非法结束日、预付款、分期、独立退款、负净额、同日/乱序事件、单笔范围及累计Long溢出、0.005/-0.005、闰日与Long日期跨度；固定日期测试，不模拟算法自身。测试源码RED/GREEN哈希相同。

构建和Lint：`:app:lintDebug :app:assembleDebug`退出0，0 errors / 12既有版本提示，不升级/抑制。最终APK实际核对身份/权限/编译备份资源/Wrapper及领域类，不增加运行依赖；测试JUnit4.13.2/Hamcrest1.3没有进入运行classpath/APK。27个最终输入SHA-256、逐项测试结果、日志/XML/产物哈希见[M02证据](evidence/M02-verification.md)与[JSON](evidence/M02-verification.json)。

原ZIP、43原件与阅读版字节/CRC/哈希核对PASS；130原案例及16组完整跨模块补测仍NOT_RUN。Q02/Q07为USER_CONFIRMED，Q02本轮领域已实现，但M17/M18集成尚未执行；Q03～Q06状态及期限逐行与基线一致，不提前关闭。M00/M01历史checkpoint及既有证据不改写。

NOT_RUN / N/A：Instrumentation、设备安装/断网冷启动、业务Room/SQLite/迁移/Repository/完整备份往返，本轮纯领域范围没有对应能力；真机/API26/OEM保留M01未测NOTES；展示舍入/页面和M03～M23尚未实施。TypeScript/ESLint不适用原生Kotlin。NO-SOURCE不计测试PASS。

既有Git/文档检查与交付证据见[M02 checkpoint](checkpoints/2026-10-11-M02.md)。M02已按本次用户转交的独立结论冻结；本次仅回写两份状态文档后停止，不进入M03或合并main。

## M03 当前执行验收（2026-10-11）

授权：用户本轮内联M03请求；基线 `0169c76bbf0946a0400b96d00124b3de9aea1cc0`，来源feat/m02-project-cost-engine，当前feat/m03-subscription-engine。M00～M02已冻结，历史限制是当时授权记录，本轮M03已授权、M04未授权。当前 **READY_FOR_REVIEW**，ChatGPT独立验收 **PENDING**，不自行冻结。

| 原案例 / 当前领域补测 | 实际结果 | 证据与边界 |
|---|---|---|
| M03-T01 monthly_standard_rate | PASS | 150/30=5，真实31天覆盖独立；实际31天、标准30 |
| M03-T02 annual_purchase_not_elapsed_year | PASS | 3650/365=10，首日实际1、第100日100/年末365，到期不增；未实现里程碑通知 |
| M03-T03 prepaid_next_rate_does_not_start_early | PASS | 1月20实付下一期180，当前仍5，2月1生效变6；历史330/标准60=5.5 |
| M03-T04 expired_has_zero_current_rate | PASS | 排他到期日EXPIRED/贡献0/当前价格null，实际31天封顶；不依赖后台 |
| M03-T05 pause_does_not_erase_paid_history | PASS | 1月10暂停、20恢复；付款150/标准30保留，暂停实际9、恢复日10/月末21 |
| M03-T06 overlaps_rejected | PASS | 重叠拒绝、邻接允许，业务今天验证未来付款拒绝；历史观察不误拒后来付款 |
| Q03-V01（仅订阅日期领域） | PASS | 首日1/4月10日100/365及到期不增；M21触发尚NOT_RUN |
| Q03-V02（仅订阅日期领域） | PASS | 暂停10日期年末355；已真实续费邻接到2027-01-10累计365；跨模块仍NOT_RUN |
| Q03-V04（仅订阅闰日日期） | PASS | 2024-02-29首日1，2025-02-27第365日；长期节点/导入通知水位仍NOT_RUN |
| Q04-V01（仅成本区间） | PASS | [09-15,10-15)的150/月；10-15至10-20提醒日贡献0/实际30不增；提醒资格仍PROPOSED、M20前 |

实际RED37项/3通过/34 AssertionError失败/0错误/0跳过；同一测试源码GREEN37通过。完整 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` 退出0，68测试全通过（金额7/时钟2/项目22/订阅37）；Lint0 errors/12既有提示不处理，构建成功。没有编译/环境错误被冒充RED，也不把NO-SOURCE计PASS。覆盖历史付款、预付未来、改周期、暂停恢复/停止、同日冲突/重排、覆盖空档、独立提醒边界、精度、闰日与极限Long日期。

最终APK检查通过：身份/权限/编译备份资源/Wrapper，17个关键领域类描述符存在且测试框架/类未打包，v2签名验证通过。31项最终输入哈希（27继承＋4新增）、逐项测试/XML/日志/产物哈希与实际范围见[M03证据](evidence/M03-verification.md)、[JSON](evidence/M03-verification.json)及[checkpoint](checkpoints/2026-10-11-M03.md)。182条唯一运行依赖坐标线与M02一致，无权限/依赖变化。未安装或公开上传APK。

原ZIP/43原件与阅读版字节/CRC/hash通过，130原案例和16完整跨模块补测保留NOT_RUN；上表仅当前领域结果，不改原件。Q03～Q06状态/期限逐行保持，未关闭提醒/长期节点/图片/回退问题，Q02 M17/M18仍NOT_RUN。

NOT_RUN / N/A：本轮设备安装/断网冷启动/Instrumentation、Room/Repository/迁移/完整业务备份、专门M03订阅合计Long溢出执行；没有对应Android交互或业务存储，源码显式检查不等于该行为PASS。真实手机/API26/OEM继承M01未测NOTES；通知/里程碑/订阅UI与未来集成仍未实施。TypeScript/ESLint不适用。M03等待独立验收，本轮交付后停止，不进入M04或合并main。

## M03 独立源码审查与补测复验（2026-10-11）

用户转交ChatGPT独立源码审查：**CHANGES REQUESTED — 仅补齐累计溢出测试，暂不冻结**。评审分支feat/m03-subscription-engine，评审HEAD `80cf6473c21d5933963dd55b02dfbf9f97297849`。已确认M03-T01～T06覆盖及原37＋31交付证据一致，核心逻辑暂未发现确定性计算缺陷；缺口为累计金额/标准天数溢出未实际测试。额外STOPPED再启用、多期预付默认停止风险归M09，未授权本轮改语义。

本轮从相同本地/远端评审HEAD与干净工作区继续，仅提取生产evaluate实际调用的内部累计函数并补6测试，原37与31测试源码不改。已有Math.addExact行为保留，采用边界补测/回归，未人为制造RED。完成执行状态 **READY_FOR_REVIEW — M03 (RECHECK)**，独立复验 **PENDING**；不自行授予ACCEPTED / FROZEN。

| 当前新增补测 | 实际结果 | 证据 / 边界 |
|---|---|---|
| M03-R01 金额到精确Long.MAX | PASS | 合成累计种子加合法最大单笔，得到精确MAX；另一字段正常、种子不变 |
| M03-R02 金额超过Long.MAX | PASS | 数学结果MAX＋1，重复执行均ArithmeticException，不回绕/修改种子 |
| M03-R03 标准天数到精确Long.MAX | PASS | 合成种子加合法年付365，得到精确MAX；金额仅1分 |
| M03-R04 标准天数超过Long.MAX | PASS | 数学结果MAX＋1，重复执行均ArithmeticException，不暴露部分金额更新 |
| M03-R05 5倍数近界后再续费 | PASS | MAX−367＋365=MAX−2，下一笔月付30明确溢出，前结果不变 |
| M03-R06 两笔付款/生产连接 | PASS | 内部累计与实际evaluate均420元/120标准天；编译字节码核对生产调用同函数 |

这是内部生产同路径运算边界，增量均合法Renewal；合成累计种子不代表可形成的完整合法历史，不声称海量续费端到端溢出验证。现有List的标准天数至多Int.MAX×365，不可能达Long极限，且合法周期合计为5倍数。

实际完整命令 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` 退出0，74通过/0失败/0错误/0跳过（金额7/时钟2/项目22/原订阅37/新增累计6），六原案例继续回归通过。Lint0 errors/12既有提示，构建成功。最终APK身份/权限/备份资源/Wrapper与v2签名核对通过，18关键领域类存在，测试类/框架未打包；32最终输入哈希及逐项XML、日志、APK/编译调用链见[补测证据](evidence/M03-recheck-verification.md)和[JSON](evidence/M03-recheck-verification.json)。

原130案例/16完整跨模块补测NOT_RUN不变，原M03证据和上节初次交付记录保留为历史，当前补测结果在本节登记。Q03～Q06按既定期限，M09两风险[另行记录](SPEC_ISSUES.md#m09-订阅流程风险本次独立审查登记当前不实施)。设备、业务数据库/迁移/备份与通知仍NOT_RUN / N/A。本轮停止等待独立复验，不进入M04或合并main。

## M03 独立复验与正式验收记录（2026-10-11）

- 正式结论：**ACCEPTED / FROZEN — PASS WITH NOTES**。
- 来源：用户在当前会话明确转交的ChatGPT独立复验正式结论；Codex本次仅回写，不自行授予验收或以本地辅助审查代替独立复验。
- 仓库：`cc7279694-debug/property`。
- 评审分支：`feat/m03-subscription-engine`。
- 评审HEAD：`8b13d20dad89f9c3e76a3d19161cf2232b36b94d`；回写前实际确认本地/远端均匹配，工作区干净。
- 独立确认：六项新增累计边界测试与生产代码使用同一累计函数；累计金额与标准天数均由Math.addExact检查溢出；失败不产生部分更新的累计结果；74个JUnit方法与提交的74条PASS结果完全对应；评审补测Git范围3新增/7修改/0删除，未越界进入M04。

保留NOTES与后续边界：

1. **本次独立审查未在用户Windows本机重新运行Gradle。**本次Codex状态回写同样不运行Gradle、构建或设备验证；既有74项结果与RED/GREEN/回归证据保持原样，不把源码/证据审查登记为新测试执行PASS。
2. 原ZIP/43归档原件、原130条规划案例及16组完整跨模块补测保留，原案例/完整补测继续NOT_RUN；历史交付、M03 checkpoint和两组M03证据不改写。内部合成累计种子的边界验证不宣称海量合法完整历史端到端测试。
3. Q03～Q06状态/期限不变：长期100/1000日节点M21前，提醒资格M20前，图片往返M10/M12前，持久回退M04设计/M12实施及故障验证。Q02的M17/M18集成仍NOT_RUN；不因领域引擎冻结而提前关闭问题或集成验证。
4. 保留[M09两项流程风险](SPEC_ISSUES.md#m09-订阅流程风险本次独立审查登记当前不实施)：STOPPED后重新启用的入口/历史/纠错边界、多期已预付默认停止边界，仍待M09明确与验证，NOT_RUN，当前未改产品语义或实施流程。
5. 设备安装/断网冷启动/Instrumentation、真机/API26/OEM、业务Room/SQLite/Repository/迁移/完整备份、M09流程及M20/M21通知等未运行验证继续NOT_RUN / N/A；12条既有Lint版本提示不处理。
6. 本次只更新唯一CURRENT_STATE和本登记、检查差异后提交/推送现有M03功能分支。M04仍须用户另行授权；不修改业务代码、WHOCARE或原规划，不合并main或进入M04。

本节正式结论取代先前CHANGES REQUESTED/复验PENDING的当前效力；旧记录完整保留为历史。回写提交自身SHA、推送与最终远端核对以Git日志/最终报告为准。
