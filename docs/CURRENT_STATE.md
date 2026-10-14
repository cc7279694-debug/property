# 当前项目状态

状态日期：2026-10-14（Asia/Shanghai）。本文件是唯一实时状态；原件归档和 checkpoints 只记录历史。

## Current Stage

- **M10 — READY_FOR_REVIEW，独立验收PENDING**：已完成本轮授权的本地单封面及安全资源生命周期，实际结果见[14项报告](evidence/M10-verification.md)、[结构化证据](evidence/M10-verification.json)、[文件清单](evidence/M10-files.md)与[checkpoint](checkpoints/2026-10-13-M10.md)。基线 `57de897917473f255d43dfb306a8433b366c00eb`、当前 `feat/m10-local-cover-media`；开工本地/远端一致、工作区干净。Q05 USER_CONFIRMED，完整M11/M12往返仍NOT_RUN；未自授ACCEPTED/FROZEN，不进入M11～M23。

- **M09 — ACCEPTED / FROZEN — PASS WITH NOTES**：依据用户在本会话正式转交的 ChatGPT 独立代码验收结论，评审 HEAD 为 `bfc25378927ca8d04eaecbb9d2b1455d72633da9`，评审及本次回写分支为 `feat/m09-subscription-lifecycle`，来源M08冻结回写 `0063ca504ca48ef9c7231d79dc155e9bf4bc8c82`。M09-T01～T06、原455＋新增94＝549项测试（86 JVM＋463 Android）及API36的20条实际路径等交付证据通过独立审查，未发现确定性阻塞问题。完整12项确认与全部NOTES见[正式验收登记](ACCEPTANCE_MATRIX.md#m09-独立代码验收与正式冻结记录2026-10-13)。本次仅文档回写，未重跑产品测试、构建、安装或核验APK字节；旧执行与checkpoint保留当时READY_FOR_REVIEW/PENDING，正式结论取代其当前效力。M11～M23未授权。
- **M08 — ACCEPTED / FROZEN — PASS WITH NOTES**：依据用户在本会话正式转交的 ChatGPT 独立代码验收结论，评审 HEAD 为 `a716bc5ad9e3ab49cbe858f2c966fecf93e09fbb`，评审分支 `feat/m08-subscription-renewals`。这是M08历史验收，历史交付状态与证据保留；当前M09正式冻结见上一行，M11～M23未授权。
- **M00～M07 — ACCEPTED / FROZEN — PASS WITH NOTES**，均依据用户转交的 ChatGPT 独立结论。M07 评审 HEAD 为 `4089428e7acdc0f2b8e2fd660f437b5bc7d29f61`，冻结回写 `fcb850f549285d310040318a5f7adfd51abb4c33` 是M08当时起点；全部确认项、评审 HEAD 和 NOTES 保留在[验收登记](ACCEPTANCE_MATRIX.md)。旧报告、XML、JSON、截图和 checkpoint 的当时状态不改写。

## M10 已验证交付与保留边界

- 本地受控静态JPEG/PNG、同代私有不可变资源、共享双引用清理及两类封面界面已接入；当前147项完整输入下已核对**652个唯一方法PASS＝86 JVM＋566 Android，原549＋新增103，0失败/错误/跳过，源码/XML EXACT MATCH**。覆盖来自分开执行：`green-full-runner-final`成功的JVM子命令（67.033秒、退出0），以及`green-android-systemui-clean-final`成功的Android/Lint/assemble阶段（639.901秒、退出0，日志10m39s）。第一阶段整体仍FAIL，其557 PASS/9 FAIL的Android结果全部排除；不是一次652项BUILD SUCCESSFUL。输入/ZIP原字节及逐方法依据见[严格覆盖核对](evidence/m10-tests/final-split-verification.json)。Lint0 errors/23 warnings，26完成阶段/44XML与1次中断的原始失败/日志保留。手工路径另有实际证据，不由自动化推定。
- 两处实际SIGKILL探针已PASS：文件落实后DB前（PID4239→4297）、DB提交后旧图清理前（PID4335→4375），旧进程实际消失、同合成generation重开、封面/金额/历史/时区和SQL完整性核对成功，安全清理已知孤儿。仅androidTest独立合成数据；不是断电、真实LMK或M12恢复证明。测试Runner登记失败的先前准备尝试保留；修正后的Runner无action时直接委托AndroidJUnitRunner，显式action才执行不计JUnit的探针，正式APK不包含它。
- 专用API36此前误重建，原AVD目录已不存在，原设备连续升级/保留证据中断。原M09合成DB/WAL/SHM及事实快照完整，严格重建fixture并保留已有空generation后，实际原M09 code7→最终M10 code8覆盖升级PASS：全部七表原字段、同代、GMT、pointer/ready/settings字节一致。不能称原设备连续保留或M12产品恢复。实际系统取消零写、A源删除冷重开、坏图保旧、两类独立导入/替换/移除、共享B及删除QA项目保护订阅、确切旧文件物理清理PASS。断网default network=none，360dp/200%字体、真实IME/raw错误、横竖屏/边缘手势及已保存Task616/PID17203→17927/同准备ID与字节恢复、明确一次保存PASS。早期加载截图和屏外就绪超时如实保留。全部guest设置实际复原，UTC差0.731秒，宿主和其他设备未改；见[最终设备核验](evidence/m10-device/manual/final-storage-device-proof.json)。
- 最终APK code8/0.1.0-m10-debug，12,755,115 bytes，SHA `939b3b20d3c6cd02c800f320ccddc300736b75084b609c404f38b096270f7094`，实际已安装字节与最终产物一致。v2签名/旧证书、无网络/相机/广泛存储权限、九域备份排除及90生产类/测试未打包核对；Room v2、原v1/v2导出、冻结引擎、唯一owner与运行依赖122→122保留。先文件fsync/核验后Room引用，不宣称跨存储原子；未知归属残留保留，真实LMK/断电/Release/真机/API26/OEM/完整TalkBack及完整备份恢复仍NOT_RUN。

## Verified / Completed — M09（独立验收认可的此前实际交付；本次未重跑）

- 复用schema v2七表、M08迁移/版本结构预检、唯一owner/coordinator、Money/BusinessClock及M03引擎。六种Room事务写入完整事实CAS；同日状态/原付款纠错保留身份，精确固定目标重放；无覆盖重开付款＋ENABLED同事务，取消计划/删除只确认CurrentAbsence，最后一笔拒绝。
- 用户A/B规则已实施并实测：STOPPED可同ID重开；覆盖内不付款、未来已付款生效前仍EXPIRED、无任何覆盖显式真实付款；默认停止固定全部已确认覆盖最晚边界与今天中的较晚日，预付/空档/全部到期/提前停止及固定计划冲突均验证，后续付款不会静默延长原计划。来源为用户M09完整指令SHA-256 `d83fb2d77190b0c49df8b5adf9a6e13c8cae8ca203513f9f5e98b0729350eaab`及[原决定记录](DECISIONS.md)、[M09契约](planning/M09_CONTRACT.md)；实施验证由原[M09报告](evidence/M09-verification.md)保留，本次独立审查认可。同身份重开及多期预付停止两项流程风险按模块证据关闭，Q03～Q06期限不变。
- 正式现在管理分订阅中/暂停中/已到期，已停止从回看进入；未来停止不提前归档。单一SavedState信封保存raw/基线/UUID/提交目标；保存不确定/读取错误保留输入与pending，COMMITTED只读；迟到读取不能覆盖后来详情，Nav3来源链有界/双VM守卫保护离开。
- 最终实际四任务 **Gradle日志13m 24s，命令墙钟805.631秒，549/549 PASS＝86 JVM＋463 Android，原455身份保留、新增94，0失败/错误/跳过**。所有测试方法与最终XML一一对应、128份构建输入哈希一致；Lint 0 errors/20 warnings、Debug assemble完成。11个此前真实阶段及最终阶段均归档，早期两次全量失败和旧四测试驱动最小调整不抹除，不把同源码限定PASS当根因证明。
- 最终APK **code7 /0.1.0-m09-debug，12,521,904 bytes**，SHA-256 `becd15b3bbf5de9425419504bde58837d0d6fa6e1cacc32d995f8dc11ec437f2`，与实际旅程使用及最终安装指纹相同；v2签名/旧证书兼容、无网络/禁止权限、九域备份排除、79指定生产类/无JUnit测试打包核对。运行依赖122→122完全相同，TOOLCHAIN_LOCK不改。
- 仅专用API36合成服务：150→预付180，固定3/1计划/边界STOPPED、修正/取消、暂停/恢复/提前停止、原ID重开、2/1真实6元每日、无覆盖空金额/付款日显式240原子启用、原付款纠错225及明确删除错误180，断网冷启仍按事实EXPIRED。实际设置/设备UTC恢复；旧七表原行逐字段相同，同generation/ready/settings/GMT。小屏200%/真实IME/raw错误/旋转/真实手势返回/系统已保存任务SIGKILL后同任务恢复单独留证，不称真实LMK。

13项报告及所有边界见[M09报告](evidence/M09-verification.md)、[结构化证据](evidence/M09-verification.json)、[文件清单](evidence/M09-files.md)与[M09 checkpoint](checkpoints/2026-10-13-M09.md)，均保留原交付字节和历史状态。本地辅助审阅不代替ChatGPT独立验收；本次正式来源是用户转交的独立结论，不将文档核查登记为新的产品测试PASS。

## Verified / Completed — M08（冻结继承的当时交付；M09实施回归另见上节）

- 新增订阅身份、独立续费及状态时间线三表，导出 schema v2；显式注册 1→2 迁移。原 v1 JSON、原四实体及旧业务事实保留；既有库在可写打开前核对版本、Room 身份、实际结构/索引/外键、WAL 和完整性，非法库拒绝并保留。字面内部对象前缀、PK BINARY/ASC 和保留表同名 trigger 的漏检均先复现再限定修复，无破坏性 fallback 或另一份空 generation。
- 首次身份与正额真实付款同一 Room 事务；追加续费复用唯一 owner/coordinator，固定 UUID/operationId，完整事实精确重放，不同内容冲突。全部已付覆盖参与重叠校验；逐笔保留 cycle、standardDays 和四个真实日期。复用冻结 Money/BusinessClock/M03，不复制成本公式、不自动付款或写入 M09 状态操作。
- 四个正式订阅页面、真实首页数量、paidOn 排序/SQL 分页/完整成本与写后刷新已接通。受控输入、可见周期选择、排他结束日说明及明确付款确认；读取失败不伪装空库。唯一 SavedState 信封与双 ViewModel 导航守卫覆盖 pending/dirty、隐藏表单可达、精确成功身份、COMMITTED 只重读及缺失目标安全返回。
- 最终四任务退出 0，**13m30s；86 JVM＋369 Android＝455 个唯一方法 PASS，0 失败/错误/跳过**。源码与 XML **EXACT MATCH**，旧 359 方法身份保留、新增 96，108 源码/schema 输入哈希与实际验证一致；八份最终 GREEN XML。六项原案例及本轮 20 项安全要求只在当前执行登记记录，不改原 130 案例。Lint **0 errors /20 warnings**，运行依赖 122→122 无新增/删除/升级。
- 真实 RED、编译/runner/匹配/准备及同步诊断完整保留。两个旧测试仅适配真实阶段，另两个旧测试加强驱动同步，保留方法身份、原业务断言和超时；不把同步调整声称为生产缺陷修复。此前 10m19s 的完整 455 PASS 已单独归档，之后只补周期可见提示及提醒日期当前不通知文案，再执行最终完整回归。
- 最终 Debug APK code6 / `0.1.0-m08-debug`，**12,386,935 bytes**，SHA-256 `6e04f28cc0998fa772cfeeedf99eb457b71852ee7549eeda39525c6155afbcf6`，与实际安装指纹一致。v2 签名及旧 M07 证书兼容、合并 Manifest/无网络及禁止权限、九域备份排除、63 个指定生产类及无测试框架/合成测试打包均已核对；Release 未构建。
- 仅专用 API36 `emulator-5596`：旧 M07 code5→中间 M08 code6 在原代真实升级，最终 APK 再同身份覆盖并核对，未卸载/清数据。旧四表所有事实与升级前完全相同：3 项目/6 事件；旧纠错结束项目仍净投入 ¥350、10 天、¥35/天。最终 APK 上完整创建合成服务并确认 ¥150→预付 ¥180，取消续费不写；实际 guest 日期 1/19 当前 ¥5/历史150/标准30，1/20 当前 ¥5/历史330/标准60/平均5.5/实际20，2/1 当前 ¥6。恢复10/13后断网冷启动 EXPIRED、当前价格 null/贡献0、历史330/标准60/实际59，无自动付款。保留此前候选合成服务，最终为 **2 订阅/3 续费/0 状态变更**，不删旧参考数据。
- 最终 APK 的 360dp/200% 字体、真实 IME、1.234 精度错误及 raw 保留、返回收键盘、横竖屏已操作。Task3038 已 STOPPED/HaveState=true/Bundle6360bytes 后 UID SIGKILL，旧 PID14365 消失，Recents 恢复同任务/新 PID15775/raw1.234；隐藏命令身份同一性由正式 Room/Nav3 仪器测试证明，不冒称手工界面读出了 UUID。最终只读 SQL integrity_check=ok、FK 无错误，generation/ready/settings SHA/记录时区 GMT 不变。全部临时 guest 网络、屏幕、字体、IME、旋转、明暗、时区及 auto_time/auto_time_zone 已恢复，guest UTC 与宿主相差少于10秒，未修改宿主时钟或其它设备。

详细文件、逐方法/输入/产物、失败诊断、实际设备及来源证据见 [M08 报告](evidence/M08-verification.md)、[JSON](evidence/M08-verification.json)、[文件清单](evidence/M08-files.md)和 [M08 checkpoint](checkpoints/2026-10-13-M08.md)。M08 正式独立验收的11项确认及全部 NOTES 见[验收登记](ACCEPTANCE_MATRIX.md#m08-独立代码验收与正式冻结记录2026-10-13)，来源为用户转交的 ChatGPT 结论，本地辅助审阅不等于独立验收。

## Pending / Current Problems / Technical Debt

- **M07 NOTES 继续有效：**ProjectAbsent 只确认当前不存在，不证明历史删除操作身份；多条旧 END_SETTLEMENT 明确冲突，没有自动修复；媒体数据/文件生命周期清理归 M10。原20条 Lint 提示及历史执行证据保留。M05 写后刷新与分页交接已在 M06 实施并独立验收，不改写历史 NOTES。
- Q01 CLOSED，Q02/Q07 USER_CONFIRMED；Q02 M17/M18 视图集成仍 NOT_RUN。Q03 长期100/1000日节点在 M21 前，Q04 提醒资格在 M20 前，Q05 图片往返在 M10/M12 前；Q06 **ENGINEERING_REQUIRED**，M12 完整导入、切换、验证后回退、中断、共同校验、清理及 Q06-V01～V03 完整流程仍 NOT_RUN。原 ZIP/43 原件/24 模块、130 案例及16组完整跨模块补测不改。
- [M09 两项流程风险](SPEC_ISSUES.md)由用户M09指令明确A/B产品规则：允许同身份重开，默认固定最晚已确认付费边界；保持USER_CONFIRMED / IMPLEMENTED / VERIFIED及原来源，两个流程风险已按模块证据关闭，现获独立审查认可。原全量失败及诊断保留，不由本次回写重新实施或验证。M04 首次初始化中断残留 generation 不得静默清除真实数据；M08 版本预检与无损升级已完成，不能代替 M12 完整恢复/故障回退。
- **CurrentAbsence：**只证明当前缺失，不提供永久删除操作日志；继续保留M07的ProjectAbsent和旧END_SETTLEMENT冲突边界。
- **空间不足：**真实 SQLite 满页可能被 Android/Room 回滚异常表现为普通 DATABASE；继续保留输入、事务原子性及同命令恢复，不按消息猜测或宣称所有磁盘满均准确分类。
- **性能与恢复：**全量存储事实预检在长期大数据下的性能仍待M22验证；1000 条分页是正确性/查询范围验证，不是长期性能基准，必要完整历史计算不保证无限历史恒定耗时。SavedState 不是持久业务草稿；已保存任务的 SIGKILL/Recents 不保证未保存状态、force-stop、断电或真实LMK后的未持久化草稿恢复。
- **数据保障：**完整备份/恢复仍归 M11/M12，未完成；Debug 暂不用于不可丢失的长期个人记录。M10当前本地封面见本轮执行记录；M13 汇总、M14 搜索/类别 CRUD、替换、多段复用、图表/排行、单独资金事件删除及后续集成保持原边界/NOT_RUN。
- **平台与独立执行：**真机、API26、OEM 备份/迁移、完整 TalkBack、真实内存压力 LMK、Release/正式签名升级仍 NOT_RUN；Debug 空 ComponentActivity 测试宿主不得未经 Release 验证正式交付。M01 独立未重装，M02/M03 独立未重跑本机 Gradle，M04～M07 独立未重跑本机 Gradle/安装等全部历史 NOTES 保留。M08 独立审查未在用户 Windows 本机重新运行 Gradle、重新安装 APK 或独立核验 APK 字节；超大 JSON 未整体重新解析，验收依据源码/XML/日志/迁移及设备证据。**M09 ChatGPT独立审查未在用户Windows本机重新运行Gradle或独立核验最终APK字节。**各次Codex验收回写均未重跑产品测试；549项PASS来自M09原实施执行证据，不把独立源码审查或本次文档核查视为重新执行。TypeScript/ESLint 不适用，NO-SOURCE 不计 PASS。

- **后续 schema 升级：**必须同步维护既有数据库版本预检及已知 Room 身份，不因 M08 通过就放过未知版本或结构不匹配库。

## Data / Architecture

当前为 Room schemaVersion=2、七张业务表，显式 Migration1→2。UI → 轻量 ViewModel → 同一 Repository/LocalWriteCoordinator → Room；Settings/DataStore、记录时区、AppContainer/ManagedGeneration、私有媒体及备份排除边界继承。pointer/ready 内部 formatVersion=1 不是 backupFormatVersion；不宣称 DB/DataStore/文件跨存储原子。真实结构与历史阶段区分见 [ARCHITECTURE](ARCHITECTURE.md)。

## Git

- 当前目录 `E:/CODEX/砺石新APP`，origin `https://github.com/cc7279694-debug/property.git`；仅功能分支 `feat/m10-local-cover-media`，来源 `feat/m09-subscription-lifecycle`，正式冻结起点 `57de897917473f255d43dfb306a8433b366c00eb`。开工本地/远端基线一致、工作区干净，目标此前不存在；从指定起点创建，不从main开始。
- M09评审HEAD `bfc25378927ca8d04eaecbb9d2b1455d72633da9`与冻结回写57de897为历史依据。M10本轮提交及推送在最终验证后执行，自身SHA由Git日志/最终回执核对，避免递归引用。main保护基线 `145326ddd4ce28abbf7c2706fe25f9d38c599c3a`；不合并main、不force、不自动PR/公开APK，不提交数据库、私钥、原图或真实个人数据。

## Next Recommended Task

**等待ChatGPT独立验收M10。**本轮仅READY_FOR_REVIEW，不自行冻结或启动M11。M11须独立验收与用户另行授权后才开始；旧历史、全部NOTES、Q03/Q04/Q06期限、Q05完整跨模块NOT_RUN继承。
