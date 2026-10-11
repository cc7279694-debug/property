# 验收登记

本文件记录M00/M01历史检查与独立验收、本轮M02实际执行结果；[原130条Markdown矩阵](planning/original-v1/lithic_v1_plan/docs/ACCEPTANCE_MATRIX.md)及[原JSON](planning/original-v1/lithic_v1_plan/docs/acceptance-cases.json)完整归档且全部保持NOT_RUN，不在原件上改结果。

日期：2026-10-11。PASS为已有执行记录的实际通过，NOT_RUN为未执行，PENDING为尚待确认的事项。独立验收/冻结结论由独立审阅提供，执行者不能自行授予。下方M00/M01各节原阶段语句是历史记录；M02已另行授权及确认Q02/Q07，当前结果见末节，不把历史“未授权/未实施”误当本轮状态。

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

## M02 当前执行验收（2026-10-11）

本轮用户已授权M02并正式确认Q02/Q07。起始基线 `a25a06899bd157988c3a4466d7162d8616fb0249`，分支 `feat/m02-project-cost-engine`；只实现纯Kotlin领域规则，保留M01空壳。状态 **READY_FOR_REVIEW；独立验收PENDING**，不自行ACCEPTED/FROZEN。上方M00/M01阶段限制保留为历史记录，当前有效限制见[M02契约](planning/M02_CONTRACT.md)。

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

Git/文档检查与最终交付见[M02 checkpoint](checkpoints/2026-10-11-M02.md)。交付后停止，等待ChatGPT独立验收，不进入M03或合并main。
