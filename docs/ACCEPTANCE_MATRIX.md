# 验收登记

本文件记录M00历史检查/独立验收结论及M01当前实际验证；[原130条Markdown矩阵](planning/original-v1/lithic_v1_plan/docs/ACCEPTANCE_MATRIX.md)及[原JSON](planning/original-v1/lithic_v1_plan/docs/acceptance-cases.json)完整归档且全部保持NOT_RUN，不在原件上改结果。

日期：2026-10-11。PASS为本次实际通过，NOT_RUN为未执行，PENDING为本轮检查尚待完成。独立验收/冻结结论由独立审阅提供，执行者不能自行授予。

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

## M01 当前执行验收（2026-10-11）

授权：本轮用户 M01 指令；执行基线 `77e61b653ebf1ce4838b4acce4b5b366e17891f4`；功能分支 `feat/m01-android-skeleton`。M00独立验收历史不变。本节为Codex实际验证记录，M01独立验收 **PENDING**，实现者仅交付 **READY_FOR_REVIEW — M01**。

| 原案例 | 实际结果 | 证据 / 边界 |
|---|---|---|
| M01-T01 empty_app_cold_starts_offline | PASS（模拟器） | 专用API36/Android16模拟器安装Success；设备airplane=1、wifi=0、mobile_data=0、Active default network=none；force-stop后无进程，am start -W为COLD/Status ok；截图/UI三个文案核对；返回与重新冷启动无应用崩溃；网络状态恢复。未验证用户真机或API26 |
| M01-T02 merged_manifest_has_no_internet | PASS | aapt2检查最终APK及合并Manifest：无INTERNET、两种精确闹钟、三种广泛存储、通知/相机/照片权限；唯一签名级内部接收器权限来自AndroidX Core 1.16.0；allowBackup=false，旧规则/cloud-backup/device-transfer编译资源各排除九域；下载字体自动初始化入口已从最终包移除；不等于所有OEM行为验证 |
| M01-T03 debug_id_is_isolated | PASS | 最终APK为com.cooper.lithic.debug，名称砺石 LITHIC Debug，versionCode=1、versionName=0.1.0-m01-debug；正式namespace/applicationId配置com.cooper.lithic；APK为本地Android Debug证书，未构建正式发行包 |
| M01-T04 versions_are_locked | PASS | 精确稳定目录与官方Wrapper/JAR/分发校验值核对；Gradle8.14.3/JDK17实际输出；根assembleDebug + lintDebug退出0；debugRuntimeClasspath实际解析一致。版本来源见工具链锁，不声称重验现有缓存原ZIP |

具体输出、日志位置、APK/截图哈希及命令见[M01证据](evidence/M01-verification.md)与[M01 checkpoint](checkpoints/2026-10-11-M01.md)。模拟器已安装的base.apk与最终本地APK SHA-256一致。

另行验证：lintDebug PASS（0 errors / 12版本更新提示，未抑制）；最终APK apksigner verify退出0，v2签名通过；原ZIP/43原件字节、130 NOT_RUN案例、Q02～Q07原状态及补测未改变；文档链接/差异/敏感信息检查在交付checkpoint记录。

未执行：业务Unit/Integration Test、Android Instrumentation Test、真实手机、API26运行、OEM备份/迁移行为、业务schema/migration/完整备份往返。原因：本轮为原生空壳，无业务数据与对应实现；完整备份往返不适用且未实现，不标Backup PASS。TypeScript/ESLint不适用于本原生工程。

原130案例保持NOT_RUN（含归档M01），结果只登记本节；M02～M23及16组补测保持NOT_RUN。Q02～Q06状态/期限不变，Q07只明确M01的模板适用边界，不关闭其他模块。M02未授权；M01独立验收PENDING，无ACCEPTED/FROZEN自授。
