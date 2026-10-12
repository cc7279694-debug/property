# M07 模块交付报告

记录日期：2026-10-12。**工程完整回归及最终APK手工设备验证PASS。**仅交付 **READY_FOR_REVIEW — M07**；ChatGPT 独立验收 **PENDING**，实现者与本地代理不自行授予 ACCEPTED / FROZEN。

最终实际完整回归：**86 JVM＋273 Android＝359 个唯一方法，原284全部保留并通过，新增75，0失败／错误／跳过。**逐方法身份、XML、受测输入及APK／设备证据以已生成的 [M07-verification.json](M07-verification.json) 为机器可核查入口；最终日志与报告保存在 [m07-tests](m07-tests/green-full.log)，实际截图位于 [m07-device](m07-device/manual-ended-detail.png)。Git提交/推送结果另以最终日志和交付回执核对；未来模块及独立验收不因本轮通过而提前通过。

## 1. 授权、基线和范围

本轮从 M06 独立验收回写 `fb0eafdc43efa6e13ebd76e3491fa4331a145abb`／`feat/m06-project-entry` 开始，在用户指定的干净 checkout 创建 `feat/m07-project-lifecycle`；没有从缺少前置模块的 main 开始。授权附件 SHA-256：`66a4810c80deaa3df766294442a23a88ceca06ed10b377501b5f68f3e17309f8`。实施前读取 [M07契约](../planning/M07_CONTRACT.md)、[原M07计划](../planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m07.md) 及适用 DOMAIN／UI／PRODUCT／ARCH 规格。

允许范围是结束项目、唯一关联实际收回、修正结束、身份稳定的历史金额纠错、真实项目删除与基本历史档案。继承原生 Kotlin／Compose／Room／DataStore、Local-only／Offline-first／No-login、冻结成本引擎、Room v1 和唯一数据 owner。

没有新增订阅、替换、图表／排行／汇总、类别 CRUD／搜索、封面处理、媒体清理、单独事件删除、回收站、多段恢复使用、备份／恢复、迁移、云端、网络 API、生产样本或新依赖。M08 及后续模块未授权，WHOCARE 未进入本轮范围。

## 2. 正式功能

详情提供真实结束入口；完整滚动表单记录结束日期和原因。结束不是删除，从结束日期起退出当前列表；未来结束日保持在当前列表，未开始项目仍准确标“尚未开始”。可选实际收回默认关闭，只有用户明确启用才创建关联记录，0元合法；真实到账日与结束日可不同，不记录业务今天之后的到账。

已有结束记录通过修正结束处理。原来有关联收回时只更新该事件的金额、实际日和说明；原来无关联收回时不会悄悄新增金额，也不提供新增关联开关。普通 INITIAL／REGULAR 金额纠错只改金额、真实发生日期和说明，方向、角色及身份只读；真实退款继续通过新增 REGULAR RECOVERY 表达。

删除使用真正二次确认 AlertDialog，明确“将删除项目及金额记录，历史统计也会重算；无法在App内撤回。”取消不写，确认后删除项目及关联金额事实；忙碌和未知结果期间不能误触重复操作或换目标。没有媒体文件清理和单独金额事件删除入口。

回看中的历史档案是正式入口，只筛已到结束日的项目；点入真实详情、保留来源返回。结束后的真实尾款或退款能继续追加，成本由冻结引擎重算，持有天数不增加。详情只有存在 `endedOn < occurredOn <= asOf` 的实际事件才说明已包含结束后结算；无晚款或仅观察日之后的款项不宣称已有结算。其它回看未来入口仍标“尚未开放”。

## 3. Repository／DAO、事务和完整事实 CAS

[ProjectLifecycleCommands](../../app/src/main/java/com/cooper/lithic/data/repository/ProjectLifecycleCommands.kt) 定义结束、结束修正、金额纠错、删除四种固定命令；[ProjectRepository](../../app/src/main/java/com/cooper/lithic/data/repository/ProjectRepository.kt) 委托同一 [ProjectWriteRepository](../../app/src/main/java/com/cooper/lithic/data/repository/ProjectWriteRepository.kt) 与 LocalWriteCoordinator。每次多行操作在同一个协调边界和 Room 事务中完成，失败整笔回滚。

更新先比较完整已加载项目与指定事件事实，再执行 [ProjectDao](../../app/src/main/java/com/cooper/lithic/data/local/ProjectDao.kt) 全字段 CAS。nullable 字段、UUID、createdAt、封面引用、角色、方向、operationId 都纳入核对；资料／生命周期／金额纠错使用固定且严格递增的项目 updatedAt。角色和不可变身份不由纠错改变。每个受影响行必须恰好一行；行数异常抛错以回滚前面的修改，不能返回失败却提交半成品。

所有正式写入口检查唯一 END_SETTLEMENT 关联，多条旧关联明确 Conflict，不猜一条、不清历史。保留旧 addMoneyEvent 对已结束项目新增首条 END_SETTLEMENT 的兼容性；之后第二条关联被拒绝，普通真实收回仍可追加。

删除核对完整项目和全部金额快照，按ID比较而非依赖传入顺序；新增、纠错或资料变化会拒绝陈旧确认。随后逐项删除事件和项目并核对最终不存在。共享或未引用的媒体元数据／真实文件均保留给 M10，不制造项目金额外键悬空。

schema v1 没有持久操作日志。已是完整固定目标才报告重放；缺失删除目标由 writer 核对项目与关联事件均不存在后返回 ProjectAbsent，**只证明该ID当前不存在，不证明是谁或哪次操作执行过删除**。

## 4. 单表单、未知结果、恢复和 Nav3

继续使用原 ProjectViewModel 的单 form Bundle、SavedStateHandle、phase／save／retryRead／leaveDecision，不新建持久 owner、平行草稿或单独 ViewModel。提交前冻结原始输入、完整基线、submitted 信封、command／event／project UUID 和时间；IN_FLIGHT 锁定，结果未知只能重试原信封。写成功后读取失败属于 COMMITTED，只允许重读，不发起第二笔写。

冲突只在用户显式重读后重置基线并重新确认。已明确成功读到外部删除的目标时，普通编辑保留原身份／输入、禁止再保存并允许安全返回；删除仍通过原信封由 Repository 同时核对项目与关联事件。读异常不会被解释为“已不存在”，也不会换UUID猜结果。

新增路线只带 UUID 和可序列化 CURRENT／HISTORY 来源。当前父链最多4项，历史父链最多5项；历史详情／编辑返回回看来源，成功结束和纠错回同ID详情，确认删除或核对 Absent 回来源列表并清旧详情。未知或失败不自动导航。工具栏、系统返回、Tab及重选继续由统一弃改／待确认守卫处理。

首次读恢复与迟到响应核对项目、命令、表单类型和事件身份；原始日期文字与UTC日历编码保持，业务今天只来自记录时区的 BusinessClock。修复活动表单两次发布间出现全空状态的问题：在本地构造完整下一状态后单次发布，不让观察者把输入／提交中的项目误当成无表单。

## 5. 数据库过滤、50分页和写后刷新

current 在 SQL 中筛 `endedOn IS NULL OR endedOn > asOf`；history 筛 `endedOn IS NOT NULL AND endedOn <= asOf`。两者按 startedOn／ID 稳定游标，每页最多查询51行、展示50行，以一行哨兵判断继续；只为可见项目批读事件和类别。整个已加载窗口在同一协调快照／Room事务下重建，避免页间写入产生混合窗口。

M06 的无过滤 loadProjectPage／loadProjectWindow 保持原全项目语义，以兼容既有回归。首页仍显示全部已录入项目 COUNT，不冒称 M13 当前成本汇总；当前列表数量只表达当前筛选集，历史数量只表达已结束集。

真实写入、资料变更、生命周期和金额纠错驱动当前／历史／详情重读。加载、空、错误与保留上次内容分别显示；数据库异常不变成假空档案。单项目准确成本仍需读取其全部必要事件，分页验证不等于无限历史恒定耗时或手机长期性能基准。

## 6. M07-T01～T05 的实际验证

原件案例状态不改，本轮结果单独登记。以下五个同名案例在真实 Room Repository 和生产 Nav3／ViewModel／Room 闭环中均已实际通过。

| 原案例／同名方法 | 本轮结果 | 核心事实断言 |
|---|---|---|
| M07-T01 `end_and_recovery_atomic` | PASS | 真实 SQLite ABORT 失败注入后原项目／事件不变；显式重读重新确认后结束＋唯一关联收回同事务落库 |
| M07-T02 `edit_end_recovery_not_duplicated` | PASS | 结束款连续修正300再250只更新原事件；event／operation／project ID、角色及方向不变，关闭重开事实一致 |
| M07-T03 `late_installment_does_not_reactivate` | PASS | 结束后真实投入增加净投入，固定持有天数、不回当前；真实历史入口和来源返回 |
| M07-T04 `correct_end_date_updates_result` | PASS | 合法结束日改变后由冻结引擎重算；早于开始日拒绝且保留原始输入和旧事实 |
| M07-T05 `delete_requires_confirmation` | PASS | 正式取消不写、确认删净项目／事件、历史列表回到真实空；Repository另验证共享媒体／文件保护和无悬空金额 |

补充实际方法覆盖无收回与显式0的不同事实、结束后退款、INITIAL／REGULAR 原记录纠错、全部正式入口旧多关联冲突、完整陈旧确认、真实失败回滚、并发／重放／结果中断、已保存读错、首次读恢复、来源导航、50分页一致快照、缺失目标和活动表单一致性。第8节手工尾款／退款／取消／确认删除由最终APK实际操作及只读数据库核对另行验证。

## 7. 最终回归、真实 RED／GREEN 与构建

最终实际命令：

```powershell
.\gradlew.bat :app:testDebugUnitTest --rerun :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug --max-workers=2 --console=plain
```

退出0，**BUILD SUCCESSFUL in 7m 38s**。JVM任务实际重新执行，不把之前 UP-TO-DATE 的82项当作新执行。最终86 JVM、273 Android，源码／XML／JSON共359个唯一方法，0失败／错误／跳过。原284保留并全PASS；新增75分别为规则4、Repository22、ViewModel22、Routes3、Screens15、Flow9。TypeScript／ESLint不适用；NO-SOURCE不计PASS。Lint **0 errors／20 warnings**，既有提示不升级、不抑制。

行为失败与后续诊断按实际记录保留在 [测试证据](m07-tests/green-full.log) 和 [JSON](M07-verification.json)，不擦掉失败历史：

- 可编译 stub 阶段规则4项有3个行为FAIL；Android新行为63项中60FAIL／3既有正确PASS。编译／runner／环境失败不计有效行为RED，也没有故意改坏旧测试。
- 初次完整 Android266项262PASS／4FAIL：精确展示文本断言、真实 Conflict 后必须显式重读的测试步骤、连续结束修正触控未达。修正前两项时保留事实／身份断言和生产冲突守卫。
- 外部删除后的冲突重读实测3项2FAIL／1读异常保护PASS，再实现安全缺失处理；活动表单一致性测试实测观察到原两次发布间全空状态，再单次发布完整下一状态。它们是独立缺陷，**不把后来的连续修正超时归因于这两次修复**。
- 连续修正先单项PASS，组合45和随后完整270项仍复现超时；单项偶然通过没有被当成修复证据。诊断初先 catch Exception、再 catch AssertionError 都没有捕获；root实际检查冻结 Compose 1.9.5 AAR，**ComposeTimeoutException直接继承java.lang.Throwable**。不是AGP剥掉信息。随后捕获Throwable，把完整终态与原堆栈文本放入单一顶层断言消息。
- 真终态显示第一次修改到300仍 IDLE／dirty、errors为空、message=null、committedId=null，原款仍200，说明触控没有进入保存，不是CAS、Room或导航慢。非法结束日的另一次10秒等待位于保存点击后等待字段错误，也不是初读超时。只调整新测试driver：正式输入后关闭实际IME、waitForIdle、重新滚动、确认显示且启用、再真实触控；保留原10／15秒限时和完整Room／身份断言。**没有直接调用model.save，也没有用语义回调代替触控。**同组合24项 fresh GREEN全PASS后，最终359完整回归再次全PASS。
- 详情无条件声称已有晚款的事实缺陷新增3案，负例“无晚款／仅观察日后款项”实际FAIL、真正已发生晚款正确；最小条件文案修复后原事实断言与角色分流均保留，最终全PASS。

## 8. 专用 Android 设备和手动状态

仅在砺石专用 `Lithic_M01_API_36`／Android16、`emulator-5596` 运行，不是用户真机或其他项目设备。最终APK实际安装Success；Wi-Fi／移动数据关闭、飞行模式开启、Active default network=none后 force-stop COLD 启动，Status ok。下列手工结果由root实际操作与截图／设备记录核对，未用自动测试结果替代。

| 手工路径 | 当前结果 | 已有证据／边界 |
|---|---|---|
| 最终APK安装、断网冷启动 | PASS | 最终SHA一致，COLD启动成功；原generation与设置记录保持 |
| 正式录入、结束＋实际关联收回、进入历史 | PASS | 正式生产页面与历史入口；实际结束详情截图 |
| 连续关联款修正300再250 | PASS | 手工输入与真实保存后金额改变，非直接ViewModel调用 |
| 360dp／200%字体、实际IME打开／关闭、滚动保存触达 | PASS（已执行路径） | 实际字段、校验错误与保存按钮可达截图；与只关闭测试driver键盘的自动回归分别登记 |
| 旋转后未提交结束raw保留 | PASS（已执行路径） | 横竖屏实际表单、金额／原因／日期核对 |
| 系统已保存任务后SIGKILL并从最近任务恢复 | PASS（限定路径） | Task1729／Record201059537，STOPPED、mHaveState=true、Bundle5944；原PID22880消失，新PID23948，同任务／记录恢复，raw金额／原因／实际日保持 |
| 手工追加真实尾款、普通历史纠错与真实退款 | PASS | 初始纠正900、尾款100、退款800；净−50/8天/−6.25，真实冷重启保持 |
| 手工取消删除、确认删除、历史返回、重启后核对 | PASS | 取消前后全部字段一致3项目8事件；确认后仅删M07项目4事件，返回历史空，冷重启历史0/首页2 |
| 保留M06原2合成项目、只修改本轮合成UUID、数据库完整性 | PASS | 只读DB/WAL副本schema1、integrity=ok/FK无错；M06原2项目4事件逐字段与此前记录完全一致 |
| 临时网络／字体／密度／旋转／IME等恢复 | PASS | 前后network/system/size/density/night/timezone全部一致；只操作本模块专用模拟器guest设置 |

保存任务后的UID SIGKILL是实际进程死亡模拟，**不等于真实内存压力LMK，不保证系统未保存状态、force-stop或断电后的草稿恢复**。自动 Nav3 SavedState／ViewModel Bundle 重建也分别登记，不冒充物理设备覆盖。

对应最终产物截图示例：[断网首页](m07-device/manual-home-offline.png)、[200%真实IME结束表单](m07-device/manual-large-end-ime.png)、[IME打开的保存触达](m07-device/manual-large-save-ime-visible.png)、[旋转](m07-device/manual-large-landscape.png)、[同保存任务恢复](m07-device/manual-restored-end-draft.png)、[结束详情](m07-device/manual-ended-detail.png)、[历史](m07-device/manual-history-first.png)、[修正300](m07-device/manual-amended-300.png)、[修正250](m07-device/manual-amended-250.png)。另见[历史纠错](m07-device/manual-corrected-initial.png)、[尾款](m07-device/manual-late-outlay.png)、[退款负净额](m07-device/manual-negative-lifecycle.png)、[断网冷重启](m07-device/manual-restarted-negative.png)、[删除确认](m07-device/manual-delete-confirm.png)、[取消](m07-device/manual-delete-cancel.png)、[删后历史空](m07-device/manual-history-empty-after-delete.png)及[首页](m07-device/manual-home-after-delete.png)。共20张均来自最终632aAPK；全量清单和截图哈希见JSON。

## 9. schema、文件、设置、权限与依赖

Room仍四表 schemaVersion=1，导出schema SHA-256仍 `351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b`。没有迁移、新表、同步字段、派生成本真值或破坏性修复；成本和持有天数仍由冻结 ProjectCostEngine 唯一计算。backupFormatVersion尚不存在，pointer／ready内部formatVersion1不是完整备份格式版本。图片／备份／恢复不在本轮实现或往返验证范围，M11／M12完整恢复仍NOT_RUN。

最终冷启动读取的activeId／ready generationId均为 `2d6c9460-a2f3-4976-ba04-781c416a0b52`，STABLE、previousId=null，设置SHA仍 `fff800a6a09afbc2e6656c8d67b512b74ba8e1693666e32b96701c7dfcf06c44`。owner、DataStore、recordZone、文件结构和备份配置未改；最终所有设备操作后再次核对同generation/ready/settingsSHA不变，最终已安装APK SHA与交付一致，M06原有事实原样保留。

运行依赖122→122，0新增／删除／升级，锁定版本和原生路线保持。最终合并Manifest与APK仅有AndroidX内部签名级 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`，没有INTERNET、精确闹钟、广泛存储、通知／相机／照片等禁止权限；allowBackup=false，legacy／cloud-backup／device-transfer均排除九域。此为实际配置／APK核查，**不承诺所有OEM备份和迁移行为已验证或绝对禁止**。

## 10. 文件变更和范围核查

实际业务新增文件：

- [ProjectLifecycleRules.kt](../../app/src/main/java/com/cooper/lithic/domain/project/ProjectLifecycleRules.kt)：生命周期合法性／观察日状态，不复制成本公式。
- [ProjectLifecycleCommands.kt](../../app/src/main/java/com/cooper/lithic/data/repository/ProjectLifecycleCommands.kt)：四种固定写命令。
- [EndProjectDialog.kt](../../app/src/main/java/com/cooper/lithic/feature/project/EndProjectDialog.kt)、[MoneyEventCorrectionScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/MoneyEventCorrectionScreen.kt)、[DeleteProjectDialog.kt](../../app/src/main/java/com/cooper/lithic/feature/project/DeleteProjectDialog.kt)、[ProjectHistoryScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectHistoryScreen.kt)：受控表单／确认／历史展示。
- [ProjectLifecycleRulesTest.kt](../../app/src/test/java/com/cooper/lithic/domain/project/ProjectLifecycleRulesTest.kt) 及 [ProjectLifecycleRepositoryTest.kt](../../app/src/androidTest/java/com/cooper/lithic/data/repository/ProjectLifecycleRepositoryTest.kt)、[ProjectLifecycleViewModelTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleViewModelTest.kt)、[ProjectLifecycleRoutesTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleRoutesTest.kt)、[ProjectLifecycleScreensTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleScreensTest.kt)、[ProjectLifecycleFlowTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleFlowTest.kt)：新增75个实际方法。

实际业务修改：ProjectDao、ProjectRepository、ProjectWriteRepository、ProjectViewModel、ProjectUiModels、LithicRoutes、LithicNavigation、ProjectDetailScreen、ProjectListScreen、ReviewShell以及模块debug版本配置。没有删除代码文件、旧测试、用户数据库或备份；原284方法没有为绕过失败而改弱。新UI复用Theme、AmountText、日期raw输入和48dp控件。

本轮文档包括契约、当前状态／路线／决策／真实架构／验收等适用回写、M07 checkpoint、本报告／JSON／XML／日志／实际截图；工具链锁定文件没有改动。[完整文件清单](M07-files.md)逐项登记新增／修改，最终核对663个本地文件链接0失效、161项原受保护输入原字节不变、原ZIP43文件CRC/字节与130 NOT_RUN保持、敏感模式0命中；变更62新增/21修改/0删除。详见JSON及最终Git回执。原43规划原件、130归档案例、16完整跨模块补测和历史验证不回写为本轮PASS，不创建平行当前状态机制。

## 11. 最终APK和Git

| 产物项 | 已核对事实 |
|---|---|
| APK | `E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`；本地忽略构建产物，不公开发布 |
| 身份 | applicationId `com.cooper.lithic.debug`；versionCode=5，versionName=`0.1.0-m07-debug` |
| 大小 | 12,213,370 bytes |
| SHA-256 | `632a402c62bc827552b14f3613b3eb64828a2bd584f7c410a576c4e4220e765a` |
| 签名 | 实际v2 Debug签名PASS，证书SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；没有提交私钥 |
| 内容 | 正式生命周期／历史类已在最终APK中核对；测试框架／合成测试未打包 |
| Release | NOT_RUN；Debug仍有非launcher exported ComponentActivity测试宿主，不声称正式发行验证 |

当前功能分支为 `feat/m07-project-lifecycle`，origin `https://github.com/cc7279694-debug/property.git`。用户仅授权本分支提交和推送；提交采用Conventional Commit，仅推此功能分支；最终提交SHA、推送后远端一致和干净工作区以实际Git日志及最终交付回执为准。报告自身SHA不在被哈希文档内递归自引用；最终提交和推送回执以真实Git日志及本地交付回执为准。

没有main合并、force push、远程历史重置／删除、PR自动合并、生产发布或公开APK。APK、真实数据库、签名私钥及个人样本不纳入提交。

## 12. 已知限制、继承NOTES、独立验收和停止点

最终完整回归与最终APK手工验证没有未解决FAIL；提交/推送及范围检查的实际结果以最终Git回执和范围登记核对。独立ChatGPT验收PENDING；本地代理源码交叉审查不能代替独立验收，也不授予FROZEN。

继承M00～M06全部独立NOTES：各历史独立审查没有在用户Windows本机重跑对应Gradle或重新安装APK，不能把本次M07执行证据改写成此前独立审查的新PASS。M06真实SQLite满页可能被回滚错误掩盖为普通DATABASE的分类边界继续保留；本轮没有按消息猜分类、删除数据修复或宣称全部磁盘满能准确归类。既有20项Lint提示、单项目全部必要事件需求、任务saved-state前提和无持久操作日志限制均保留。

**NOT_RUN：**用户真实手机、API26运行、OEM备份／迁移、完整TalkBack、实际内存压力LMK、Release构建／正式签名升级、M11／M12完整备份恢复、原130归档案例及16完整跨模块套件。M08订阅／v1→v2迁移与版本预检、M09两项流程、M10媒体、M13汇总、M14搜索／类别及后续模块均未实施／未授权，不能以本轮方法总数替代。

Q02／Q07用户确认解释继续继承，Q02 M17／M18视图集成仍待对应模块；Q03长期100／1000日节点须M21前明确，Q04提醒资格须M20前明确，Q05图片往返须M10／M12前明确。Q06仍ENGINEERING_REQUIRED，M12完整导入／切换／回退／中断／共同校验／清理及Q06-V01～V03全流程仍NOT_RUN。M09的STOPPED重新启用入口／历史／纠错、多期已预付默认停止选择两风险原样保留。M04中断残留generation不得静默清除真实数据，M08迁移须同步既存库版本预检；这些边界没有因M07自动关闭。

下一模块只能在M07独立验收与用户新授权后进入M08；继承schema／owner／recordZone／CostEngine、完整命令基线、真实刷新与分页、缺失／未知结果保护。**本轮收尾后停在READY_FOR_REVIEW — M07，不进入M08。**
