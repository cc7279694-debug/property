# M08 — 固定周期订阅、真实续费与 Room v2 验证报告

报告日期：2026-10-13（Asia/Shanghai）。本文件为本轮实际证据登记；唯一当前状态见 [CURRENT_STATE](../CURRENT_STATE.md)，任务边界见 [M08 契约](../planning/M08_CONTRACT.md)。

**验证交付状态：READY_FOR_REVIEW — M08；独立验收 PENDING，M09 未授权。**最终四任务退出 0，86 JVM＋369 Android＝455 个唯一方法全部 PASS，0 失败/错误/跳过；最终 APK 的实际安装、离线操作、日期生效边界、任务恢复和临时设置恢复已完成。Git 提交/推送须以第 12 项的实际分支日志及最终回执为准，本报告不提前宣称已推送。

证据入口：[逐方法及校验 JSON](M08-verification.json)、[实际文件清单](M08-files.md)、[当前验收登记](../ACCEPTANCE_MATRIX.md)、[测试阶段源码与输入归档](m08-tests/source-provenance.zip)。历史失败和诊断证据保留，最终结果与各历史阶段分别登记。

## 1. 三张新表、关系、索引与 schema v2

Room `@Database(version=2, exportSchema=true)` 包含原四张业务表和三张订阅事实表。实体见 [SubscriptionEntities](../../app/src/main/java/com/cooper/lithic/data/local/SubscriptionEntities.kt)，导出见 [schema v2](../../app/schemas/com.cooper.lithic.data.local.LithicDatabase/2.json)。

| 新表 | 持久事实 | 关系及索引 |
|---|---|---|
| `subscriptions` | UUID、名称、类别、可选封面/备注、创建和更新时间 | 类别/媒体 RESTRICT 外键；类别、封面索引 |
| `subscription_renewals` | UUID、关联服务、四个日期、Long 分金额、本次 cycle/standardDays、operationId | 服务 RESTRICT 外键；operationId 唯一；`(subscriptionId,paidOn,id)`、`(subscriptionId,effectiveFrom,id)` 索引 |
| `subscription_state_changes` | UUID、关联服务、状态生效日、操作状态、原因/备注、operationId | 服务 RESTRICT 外键；operationId 唯一；`(subscriptionId,effectiveOn)` 唯一；日期查询索引 |

订阅身份表不存累计付款、标准日均价或已购时长等派生真值。每次付款独立保留周期和标准天数；类型转换拒绝未知周期/状态。M08 仅建立状态时间线的结构、转换和读取能力，没有暂停、恢复、停止的用户操作或自动补写 ENABLED；空历史由冻结 M03 默认 ENABLED。

原 v1 JSON SHA-256：`351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b`。v2 JSON SHA-256：`ceb49b458c4c5baa08066586f209f96842935f5456a0a53a01220f6d958f7131`。最终 APK 核查已逐对象比较导出中的原四实体一致；原四表实体、DAO 与领域引擎未为本轮复制或重写。完整变更清单与提交前范围复核由第 12 项的交付登记承载。

## 2. 显式 v1→v2 Migration 与版本预检

[DatabaseMigrations](../../app/src/main/java/com/cooper/lithic/data/local/DatabaseMigrations.kt) 的 `MIGRATION_1_2` 仅创建三张新表及索引，并在 `ALL` 注册。它没有删除/重建旧表、改写旧事件或初始化另一份空数据集。

[LithicDatabase.open](../../app/src/main/java/com/cooper/lithic/data/local/LithicDatabase.kt) 在可写 Room 打开前，先只读检查既有数据库。预检读取 SQLite 的实际版本与 Room 身份，包含已提交 WAL 中的事实；检查文件头、已知 v1/v2、表列/PK、索引、外键、对象集合、`quick_check` 和 `foreign_key_check`。已知合法 v1 交给正式迁移；合法 v2 正常重开。v0、未知版本/身份、缺索引、额外列/表、无效外键等明确拒绝并保留原文件。

[DatabaseSchemaValidation](../../app/src/main/java/com/cooper/lithic/data/local/DatabaseSchemaValidation.kt) 进一步核对主键自动索引的唯一性、列序、ASC 和 BINARY 比较语义，按字面 `sqlite_` 前缀排除 SQLite 内部对象。仅排除类型为 table 的 `room_master_table`/`android_metadata`，因此与这两个表同名的 trigger 仍被识别为非法对象。这三类预检缺口均先有真实失败断言，再做限定修复；最终 Migration 22 项与完整 455 项全部 PASS。首次补测后完整轮的 Screen 驱动失败及随后的同步调整仍作为历史保留。

Room 使用保留文件的 corruption handler，禁用 data-loss recovery，无 destructive fallback。真实 Room DDL 故障注入证明失败整体回滚，原 v1 结构、身份和旧事实仍可读取。M12 的完整备份导入、切换和故障回退未在本轮实现。

## 3. v1 旧数据逐字段验证

真实 v1 SQLite fixture 覆盖 4 条类别、3 个项目、10 条资金事件、3 条媒体元数据，包含普通/结束项目、初始金额纠错、投入/收回、END_SETTLEMENT 和媒体引用。`v1_to_v2_preserves_projects` 对迁移前后四表所有行进行完整比较，检查 UUID、金额、日期、方向、角色、operationId、关联、备注、创建/更新时间和结束事实；关闭后重新打开再比对，验证完整性和外键。

另一真实升级测试使用原 `DataGenerationStore.openExisting()`：同一 active generation、pointer、ready、设置字节、记录时区与媒体文件不变。新建 v2 空库另有独立案例，不能替代上述升级。

跨 APK 路径已实际运行 M07 code5→中间 M08 code6，同 applicationId/兼容签名覆盖安装，没有卸载、清数据或新建 generation。首次升级是在本轮中间 APK 的目标 Application 启动时发生；**不能把它描述为最终 APK 首次触发迁移**。

专用 AVD 升级前后均为 3 项目、6 事件，四表逐行相同。保留 M06 的 2 项目/4 事件；本轮旧版新增的合成 `M08_UPGRADE_PROJECT` 从初始 ¥500 修正为 ¥450、结束收回 ¥100，净投入 ¥350、持有 10 天、日均 ¥35。升级后 `schemaVersion=2`、`integrity_check=ok`、外键无错误。中断后原 AVD 重开，再次只读核对数据和同代指针一致。

实际 activeId：`2d6c9460-a2f3-4976-ba04-781c416a0b52`；pointer/ready `formatVersion=1`、STABLE、previousId=null。设置 SHA-256：`fff800a6a09afbc2e6656c8d67b512b74ba8e1693666e32b96701c7dfcf06c44`；记录时区 GMT。它们是合成验证数据，不是用户私人记录。最终 APK 覆盖后实际重读旧项目，净投入 ¥350、持有 10 天、日均 ¥35 保持；最终只读快照再次确认原四表完整事实相同，并与本轮 2 个合成订阅/3 笔付款共存。

## 4. SubscriptionRepository 创建与续费事务

[SubscriptionRepository](../../app/src/main/java/com/cooper/lithic/data/repository/SubscriptionRepository.kt) 复用唯一 `LocalWriteCoordinator` 和同一个 Room owner。首次创建身份与真实首付款在同一 Room 事务中提交；付款插入失败时身份也回滚。续费只向已存在的服务追加一条已确认付款，不覆盖旧金额、周期或日期。

事务内校验规范 UUID、SUBSCRIPTION 类别、可选媒体关联、金额大于零且不超范围、周期与本次 standardDays 一致、`paidOn <= BusinessClock.today()`、四日期/覆盖区间和全部既有覆盖。首尾相邻及真实空档合法，已付未来覆盖也参与重叠判断；两个并发重叠命令不能都提交。

真实 SQLite `RAISE(ABORT)` 故障注入表现为明确 Conflict，并证明完整回滚和原输入保留；本报告不把该约束异常改称磁盘 I/O。关闭数据库等读写失败明确暴露错误；异常身份、未知 cycle 或无付款的订阅不伪装为正常空列表。继承 SQLite 空间不足可能归类为普通 DATABASE 的 NOTES，不以错误文字猜测来牺牲事务或输入安全。

## 5. 固定 UUID、重放、冲突与提交恢复

[SubscriptionCommands](../../app/src/main/java/com/cooper/lithic/data/repository/SubscriptionCommands.kt) 和 [SubscriptionViewModel](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionViewModel.kt) 使用固定 commandId/subscriptionId/renewalId/operationId。重复、并发相同命令或已提交但结果丢失后的重放，比较完整已存事实并返回原结果，不生成第二笔；同身份用于不同内容明确冲突，不暗中换身份。

唯一 SavedStateHandle Bundle `lithic.subscription.activeForm` 在写入前保存 raw 输入、稳定身份、固定 Instant 和 submitted 信封。IN_FLIGHT 锁定输入与重复确认；系统保存任务恢复后变为 UNCONFIRMED，只核对/重放同一信封。成功身份不匹配时不导航。COMMITTED 状态下读失败只提供重新读取，明确“已保存，暂时无法读取”，不再确认付款；读到正确身份才完成内部导航。

已核对续费目标不存在时，表单保留输入但明确不可提交，可安全返回；读取失败本身不能证明目标不存在。迟到响应核对原命令/服务身份，不覆盖后来打开的详情或输入。

真实 Room＋正式 Nav3 旅程包含“提交成功后丢失返回结果→SavedState 恢复→同命令重放→数据库仍只有一笔”。此处 task restoration 为可重复测试及已保存任务状态恢复，不能泛化成 force-stop、断电、未保存状态或真正 LMK 的保证。

## 6. M03 映射与预付日期结果

Repository 映射持久事实到冻结 `Renewal`/`SubscriptionSnapshot`，统一调用 `SubscriptionIntervalRules`/`SubscriptionCostEngine`。MoneyParser、Long 分、BigDecimal 和标准周期 30/90/365 继续复用，没有另一套金额或日均公式。

实际 Repository 和正式 UI＋Room 测试使用 ¥150 月付覆盖 `[2026-01-01,2026-02-01)`，于 1 月 20 日实际预付 ¥180 覆盖 `[2026-02-01,2026-03-01)`：

| 观察日 | 当前标准日均价 | 已确认付款 | 已购标准天数 | 历史平均日成本 |
|---|---:|---:|---:|---:|
| 2026-01-19 | ¥5/天 | ¥150 | 30 | ¥5/天 |
| 2026-01-20 | ¥5/天 | ¥330 | 60 | ¥5.50/天 |
| 2026-02-01 | ¥6/天 | ¥330 | 60 | ¥5.50/天 |

上述自动化三观察日断言已 PASS。截至观察日才确认的付款进入历史，提前付款不会提前改当前套餐；实际已经过去的有效日期另算，预付 60 标准天不会冒称已经使用 60 天。无覆盖时 `currentRenewal=null`、当前贡献为 0，详情明确无当前有效套餐。历史成本的分母使用完整已购标准时长，不用当前分页或自然经过天数。

最终 APK 上实际创建 `M08_FINAL_SUBSCRIPTION` 并按专用 guest 的 1月19日、1月20日、2月1日业务日期完成同一组结果，截图、日期控制回执和持久事实均核对。1月20日实际经过有效日期为20天，标准已购60天；恢复10月13日后显示 EXPIRED、当前套餐空值/0贡献、累计 ¥330/60标准天/历史平均 ¥5.50，实际经过有效日期59天，无第三笔自动付款。日期控制只限 `emulator-5596`，不修改宿主时钟或持久 GMT；原 `auto_time/auto_time_zone` 已恢复，guest UTC 与宿主差小于10秒。

## 7. 正式列表、详情、新建与续费 UI

新增正式 [列表](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionListScreen.kt)、[详情](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt)、[新建](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionEditorScreen.kt)、[续费](../../app/src/main/java/com/cooper/lithic/feature/subscription/RenewalEditorScreen.kt)；首页展示同库真实数量和入口，保留现在/回看/设置三个 Tab，不提前做 M13 总成本汇总。

列表真实按 `MAX(paidOn) DESC / UUID ASC`，历史按 `paidOn DESC / UUID ASC`；SQL 50 条展示＋51 条哨兵，类别/付款/状态批量读取，已加载窗口在同一快照刷新。完整成本读取必要的完整事实，分页不截断分母。加载、真实空状态、已有行刷新失败和错误明确区分。

表单使用受控输入、固定月/季/年、SUBSCRIPTION 专属类别及四个独立日期；前次金额/周期与自然日期仅参考。只有明确确认已付款按钮写入，打开、编辑、放弃或到期不写。金额错误定位且保留 raw，解释付费结束为排他边界。详情主展示历史平均成本、折算付费时长，当前套餐独立，未来已付覆盖标“尚未生效”。

新 UUID 路由最多 4 级，旧项目当前 4 级/历史 5 级保持。双 ViewModel 汇总离开守卫：pending 优先阻止、dirty 提示弃改，系统返回、工具栏、Tab 重选及所有业务入口一致。隐藏恢复的 pending 表单有核对入口；根首页系统返回同样保护 pending/dirty，无表单时正常退出。

360dp、200% 字体、真实 IME、保存任务恢复和付款确认均有自动化旅程及最终 APK 手工证据。最终页面在真实 IME 下可滚动到并触摸确认按钮，三位小数报错保留 raw，系统返回收键盘；横竖屏保留金额/周期/四日期。已保存任务的 UID SIGKILL/Recents 恢复见第10项，不冒充完整 TalkBack 或真实 LMK。

最终截图检查补充两处说明文案：`固定付费周期：月付（当前选择）` 明确所选周期；提醒日期说明当前仅记日期、不发送通知。它们没有增加业务语义、通知能力或写入行为；本轮未为两处可逆文案机械新增测试，随后重跑完整455项与最终设备旅程。

## 8. M08-T01～T06 逐项结果

下面 PASS 来自最终完整 GREEN 的实际方法结果，并保留各增量/历史全量记录。原件 130 案例的 NOT_RUN 不回写为 PASS，本轮结果只登记当前验收矩阵。

| 案例 | 已执行实际证明 | 增量结果 / 最终结果 |
|---|---|---|
| T01 `v1_to_v2_preserves_projects` | 真实 v1 fixture 完整四表迁移/重开；新 v2 空库另测；同 generation/settings/media 保留；升级后原项目仍可写 | PASS / PASS |
| T02 `create_and_first_payment_atomic` | 真实 Room 身份＋首付款事务、插入故障全回滚、相同命令重放/并发、异内容冲突 | PASS / PASS |
| T03 `renewal_needs_confirmation` | 正式 UI＋Room 打开/编辑/弃改/到期无自动付款；验证错误和 SQL ABORT 后原输入保留；确认才追加 | PASS / PASS |
| T04 `renewal_history_is_preserved` | ¥150 月付→¥180 月付→年付，各自金额/cycle/standardDays/四日期保持，实际覆盖切价、重读保留 | PASS / PASS |
| T05 `accurate_dates_retained` | 四日期彼此不同、提醒日与覆盖结束不同；真实持久化关闭重开；无当前覆盖无假套餐 | PASS / PASS |
| T06 `currency_change_is_allowed` | 同 MONTHLY ¥146.83→¥148.10，固定周期不变、两笔独立真实记录 | PASS / PASS |

方法位置：[MigrationTest](../../app/src/androidTest/java/com/cooper/lithic/data/local/SubscriptionMigrationTest.kt)、[RepositoryTest](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionRepositoryTest.kt)、[正式 Room/Nav3 FlowTest](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionRenewalFlowTest.kt)。最终逐方法结果与源码哈希放入 [JSON](M08-verification.json)，不以测试名或源码存在替代执行记录。

本轮授权的 20 项额外安全要求对应如下。表中的测试方法/测试组在最终源码与455条PASS记录中对应，构件与设备项另有实际核查；既有增量/历史全量证据不重写：

| # | 安全要求 | 主要实际测试方法/测试组 |
|---:|---|---|
| 1 | 合法 v1/v2 与非法版本识别 | Migration 的 known_v1/known_v2、unknown_version、version_zero、arbitrary_room_identity；额外结构/PK/trigger 8 项 |
| 2 | 迁移失败保留旧库、无假空库 | `create_failure_during_real_room_upgrade_rolls_back_v1_schema_and_facts`；既有 StorageUiState 回归 |
| 3 | schema 导出与真实结构 | `fresh_database_is_v2_with_empty_subscription_tables`、v1→v2 Room 验证、APK/schema 独立比对 |
| 4 | 拒绝 PROJECT 类别 | `project_category_cannot_be_used_for_subscription`；受控 Screen 类别筛选 |
| 5 | 无服务续费外键拒绝 | `dao_rejects_orphan_renewal_and_duplicate_operation`、`confirm_requires_existing_subscription` |
| 6 | 同 operation 不同事实冲突 | `reused_operation_with_different_content_conflicts`、`confirm_exact_replay_precedes_overlap_check_and_changed_replay_conflicts` |
| 7 | 0/负额/三位小数/超限 | `positive_bounded_amount_and_fixed_cycle_days_are_required`、`invalid_real_payment_preserves_raw_fields_and_reports_amount_error`、MoneyParser 回归 |
| 8 | 固定业务今天拒绝未来付款 | `payment_after_fixed_business_today_is_rejected`、BusinessClock 回归 |
| 9 | 已真实预付、生效尚未来合法 | `prepaid_first_period_has_purchased_days_but_no_elapsed_time_or_current_price`、预付 Flow |
| 10 | 相邻/空档允许，重叠拒绝 | `adjacent_and_gapped_paid_coverage_are_allowed_but_overlap_is_rejected` |
| 11 | 并发重叠不可双成功 | `concurrent_overlapping_commands_cannot_both_commit` |
| 12 | 预付只改历史，生效才切当前 | `prepaid_price_waits_for_effective_date_and_keeps_purchased_days_separate`、`renewal_history_is_preserved` Flow |
| 13 | 无当前覆盖的空价格/0 贡献 | `expired_service_has_no_fake_price_or_automatic_payment`、T05 Flow |
| 14 | 30/90/365 与自然日期分开 | 周期一致性 Repository、预付 Flow、冻结 M03 全部回归 |
| 15 | 最近续费按 paidOn | `real_recent_payment_sort_is_not_effective_date_sort`、`paid_on_history_and_list_order_with_real_paging` |
| 16 | 新旧业务共存不损项目 | `known_v2_reopens_and_old_project_writes_still_work`、原 359 项、跨 APK 合成事实核对 |
| 17 | 同 generation/ready/时区 | `v1_generation_upgrade_keeps_pointer_ready_settings_zone_and_media`；跨 APK 只读快照 |
| 18 | 刷新失败不伪空 | `failed_list_refresh_retains_real_rows_and_an_explicit_read_error`、Screen loading/error、异常无付款/未知 cycle 读取测试 |
| 19 | 360dp/200%/IME/返回 | `narrow_large_font_real_ime_saved_task_and_confirmation`、`narrow_large_font_form_actions_and_errors_remain_reachable`、NavigationGuard 全组 |
| 20 | 中断原命令恢复与 raw 保留 | lost_create/lost_renewal ViewModel、`real_renewal_commit_with_lost_result_replays_original_payment_after_saved_task_restore` |

## 9. 原 359 项、新增数量及真实 RED/GREEN

原 M07 的 86 JVM＋273 Android＝359 个唯一方法身份保留并实际通过。本轮新增 96 个 Android 方法：Migration 22、Repository 30、ViewModel 12、Routes 6、NavigationGuard 7、Screens 12、真实 Flow 7。最终实际运行86 JVM＋369 Android＝455，全部 PASS、0失败/错误/跳过，四任务退出0；源代码、XML及JSON的完整方法集合 EXACT MATCH，108项测试/生产/schema输入哈希与最终运行捕获一致。

仅两处旧断言做阶段适配并保留原方法身份：`schema_v1_has_only_four_business_tables` 使用真正的 v1 fixture 继续证明四表；`empty_screen_has_no_fake_metrics` 从“订阅尚未开放”改为真实订阅空态/入口，同样验证不显示伪成本。没有删除旧测试来通过新阶段。

随后完整回归又暴露两处既有测试的同步缺口，另修改两个旧测试文件：[ProjectLifecycleViewModelTest](../../app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleViewModelTest.kt) 和 [ProjectScreensTest](../../app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectScreensTest.kt)。因此本轮旧测试调整共四个文件：两个模块适用性适配，加两个驱动同步加强；原 359 个方法身份与原业务断言保留。冻结 ProjectViewModel 和全部旧业务代码没有因此修改。两个同步调整的具体事实和复验状态如下，不能把测试驱动改动说成已修复生产缺陷。

| 实际阶段 | 已执行结果 | 定性与保留证据 |
|---|---|---|
| 迁移编译首尝试 | 未执行测试 | 混合 SQLite 绑定数组类型推断失败；明确 Any 修正；不计行为 RED |
| red-migration | 13：4 PASS、9 FAIL；59s | 在旧生产 v1 上实际缺失 v2 迁移的断言 RED |
| red-foundation | 60：20 PASS、40 FAIL；2m38s | 可编译 Repository/状态/UI 空壳缺行为；迁移 14 项已 PASS |
| green-foundation-attempt1 | 72：68 PASS、4 FAIL；2m39s | 1 项关闭库后才构造 Repository 的准备错误；3 项部分文字误用精确匹配；不算生产缺陷 RED |
| red-preflight-and-guard | 63：59 PASS、4 FAIL；2m40s | 已知 v1/v2 的额外 `sqliteXforeign` 表与 PK NOCASE 各两项真实预检漏检 RED |
| red-root-back-and-real-flow | XML 86：80 PASS、6 FAIL；4m24s | 4 项有效行为 RED：根首页 pending/dirty 返回、缺失目标 VM/Screen；另 2 项是 Activity 同步生命周期断言及 Flow runner 非 Unit 的诊断，7 个 Flow 方法未执行，不能计 7 项 RED |
| green-subscriptions-attempt1 | 92 PASS；5m6s | Migration18/Repository30/VM12/Routes6/Guard7/Screens12/Flow7，全无失败/错误/跳过 |
| green-full-attempt1 | 86 JVM＋365 Android＝451 PASS；12m39s | 原 359＋新增92；四任务成功；Lint 0 errors/20 warnings；此为四项 trigger 补测前版本 |
| red-reserved-triggers | 22：18 PASS、4 FAIL；8m38s | v1/v2×两保留表同名 trigger 的真实 Assertion RED，前后完整事实比较 |
| diagnostic-full-final-attempt1 | 86 JVM PASS；369 Android：368 PASS、1 FAIL；11m31s | trigger22已 PASS；`renewal_requires_explicit_payment_action_and_keeps_reference_readonly` 触摸后计数同步失败，整轮 BUILD FAILED 保留 |
| diagnostic-screen-idle | 12 Screen 全 PASS；2m19s | 仅在同一 performClick 后增加 `compose.waitForIdle()`；生产代码、触摸按钮与 `assertEquals(1)` 未改；完整回归仍待重跑 |
| diagnostic-full-final-attempt2 | 369 Android：367 PASS、2 FAIL；9m9s | 上述新 Screen 的 waitForIdle 案例通过；失败为旧删除恢复测试读取消息的等待条件、旧项目类别菜单触摸后的节点缺失。整轮失败日志/XML保留，不改写此前结果 |
| diagnostic-old-synchronization | 2 PASS；1m25s | 两个旧失败方法的限定复验通过；原超时、业务断言和生产实现保留 |
| green-full-before-visual-selection | 86 JVM＋369 Android＝455 PASS；10m19s | 四任务成功；这轮捕获输入与当时已冻结的原输入哈希精确相同；独立归档，不作为最终文案版证据 |
| green-full-final | 86 JVM＋369 Android＝455 PASS；13m30s | 最终文案版本四任务成功；JVM以--rerun真实执行，0失败/错误/跳过；Lint0 errors/20 warnings；方法集合和108项输入精确核对 |

失败日志、XML、当时源码/生产输入哈希保留；测试准备、runner、编译或环境问题单列，不伪装为产品 RED。12项Screen限定复验之后又实际运行两轮完整455项，分别记录，未以一次限定偶然通过替代最终完整回归。

第二轮完整回归的 `interrupted_delete_retries_same_snapshot_and_only_claims_current_absence` 原等待条件只检查 deletedProjectId，随后立即读取尚未完成发布的消息。限定审阅确认等待条件不足；本轮仍保持 5 秒上限及全部“同命令/已核对/不存在/不称重放成功/事实删除”断言，改为等待同一状态同时包含正确 ID、COMMITTED 和非空 message，并捕获该次完整状态后断言。没有增加超时或修改生产状态发布逻辑。

`create_form_exposes_real_payment_date_zero_amount_and_controlled_category` 在真实文本输入后触摸类别入口，第二类别菜单节点没有出现。调整仅关闭真实 IME、等待 Compose idle、触摸同一类别入口、再次等待 idle，并先断言目标菜单可见再选择；所有金额、日期、类别及类型筛选断言保持。IME/触摸时序是合理诊断假设，不能声称已经确定唯一根因。两例限定复验与后续两轮完整455项均已实际通过；PASS来自这些真实记录，不改写此前失败为成功。

最终证据已归档：[最终日志](m08-tests/green-full-final.log)、[最终 Android XML](m08-tests/green-full-final-native-0.xml)，同目录保留七份最终 JVM XML，共8份最终结果。上述历史阶段日志/XML按原basename保留在 `m08-tests/`。精确清单、逐方法结果、源码/schema及构建输入哈希见 [JSON](M08-verification.json)，匹配各阶段的测试/生产源码及哈希见 [source-provenance.zip](m08-tests/source-provenance.zip)。

## 10. API36 操作、离线、任务恢复及覆盖升级

仅使用 `ANDROID_SERIAL=emulator-5596`，已有专用 `Lithic_M01_API_36`（Android16/API36）及私有 AVD userdata；没有操作手机、其它 AVD 或宿主网络。真实 SQLite/Room、Compose/Nav3、360dp/200%字体/IME、任务恢复和各原案例已在该设备执行自动化，覆盖升级证据见第 3 项。

| 最终 APK 手工项目 | 实际结果与证据 |
|---|---|
| 同身份覆盖安装、实际安装 APK 指纹 | PASS：同applicationId覆盖安装code6，实际安装SHA等于最终APK；没有卸载/清数据 |
| 首付款→列表→详情→提前续费→历史 | PASS：最终APK创建目标服务首付150，列表/详情真实读取；确认180后第二笔独立四日期，历史标尚未生效 |
| 1月19日/20日/2月1日真实业务日期 | PASS：专用guest日期回执及截图，当前¥5/¥5/¥6；历史150/330/330、标准30/60/60；1月20日实际经过20天 |
| 不合法输入/取消续费不写 | PASS：1.234报精度错误且raw保留；编辑180再放弃，SQL仍2服务/2付款。真实SQL失败及同命令重放由最终Room/Nav3自动化另证，不称手工主App故障注入 |
| 到期无自动付款、无假当前套餐 | PASS：恢复10月13日后EXPIRED/无当前有效套餐/0贡献；目标历史330/60标准天/实际59天，无第三笔自动付款 |
| 断网 force-stop 冷重开 | PASS：最终手工全程airplane1/wifi0/mobile0/无默认网络；冷重开原数据和目标历史保留，仍过期无自动付款 |
| 360dp/200%字体、真实IME、横竖屏 | PASS：滚动到真实确认按钮并触摸，精度错误保留raw；系统返回收键盘，横竖屏保留raw/日期/周期 |
| 已保存任务→UID SIGKILL→Recents | PASS：Task3038 STOPPED/HaveState=true/Bundle6360；旧PID14365消失，Recents恢复同任务新PID15775，raw1.234；不称真实LMK |
| 最终旧四表逐字段、同代、完整性/FK | PASS：原3项目/6事件完整相同，同generation/pointer/ready/settings/GMT；最终2服务/3付款/0状态，integrity_check=ok、FK=[] |
| 临时 guest 设置和时钟恢复 | PASS：原网络/320x640/160dpi/字体1.0/IME null/自动旋转1/rotation0/night no/GMT恢复；auto_time/auto_time_zone恢复，guestUTC与宿主差<10秒，未改宿主 |

本地 ignored DB/WAL/SHM、APK 和 AVD 不提交。交付仅包含合成数据核对结果、日志和28张实际截图，关键页面已实际查看，精确清单见 [JSON](M08-verification.json)。典型证据：[首次四日期](m08-device/manual-create-four-dates.png)、[1月19日](m08-device/manual-jan19-detail.png)、[1月20日](m08-device/manual-jan20-detail.png)、[预付未来期](m08-device/manual-future-history.png)、[2月1日](m08-device/manual-feb1-detail.png)、[过期冷重开](m08-device/manual-restarted-expired.png)、[200%字体精度错误](m08-device/manual-large-validation.png)、[保存任务恢复](m08-device/manual-process-restored.png)、[旧项目升级后](m08-device/manual-old-project-after-upgrade.png)。

最终手工目标为 `M08_FINAL_SUBSCRIPTION`（UUID `99160f9a-932e-41f7-be01-a05f2c61ea37`），两笔MONTHLY分别为150/180、standardDays各30。另一个候选APK阶段创建的参考 `M08_SUBSCRIPTION` 首付款150原样保留，没有为凑数量删除；所以最终全库是2服务/3付款，目标服务单独两笔。任务恢复后再次放弃输入并只读核对，全库仍2/3。手工界面没有直接暴露commandId；精确同命令身份由最终真实Room/Nav3/SavedState测试证明，不能把此身份说成手工肉眼观测。

## 11. schema、generation、时区、依赖与最终 APK

继承工具链：Gradle 8.14.3、AGP 8.13.2、Kotlin 2.3.10、JDK17、Compose BOM 2025.11.01、Navigation3 1.0.1、Room 2.8.5、KSP 2.3.6、DataStore 1.2.1；minSdk26/compileSdk36/targetSdk36、Build Tools36.0.0。专用现有 emulator 37.1.11.0；本轮未安装或升级工具链。runtime 依赖解析与 M07 比较为 122→122，无新增、删除或版本变化；只增加 schema 的 androidTest assets 入口。

已对下列最终 APK 实际重验构件、v2签名、63个指定生产类及打包边界，实际安装指纹一致。63是本轮列出的指定类数量，不是APK总类数：

| 项目 | 实际核查值 |
|---|---|
| 绝对路径 | `E:\CODEX\砺石新APP\app\build\outputs\apk\debug\app-debug.apk` |
| applicationId | `com.cooper.lithic.debug`；正式配置 `com.cooper.lithic` |
| versionCode / versionName | `6` / `0.1.0-m08-debug` |
| 大小 | 12,386,935 bytes |
| SHA-256 | `6e04f28cc0998fa772cfeeedf99eb457b71852ee7549eeda39525c6155afbcf6` |
| Debug 证书 SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；v2 签名，与旧 M07 兼容 |
| 权限 | 只有应用内部 signature permission `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`；无 INTERNET 或新增网络/付款权限 |
| 备份排除 | allowBackup=false；legacy/cloud/device-transfer 九域排除保留；不宣称所有 OEM 行为已经验证 |
| DEX/打包 | 63个指定生产类存在，并非APK总类数；测试框架/合成测试未打包；schema fixture仅测试assets |
| Debug Activity | MainActivity 为 launcher；既有 exported 非 launcher 空 ComponentActivity 为 Debug 测试宿主，Release 未跑，不能直接用于正式交付 |

实际 schema v2 Room 身份为 `a8fd277b0ef08ce9cf59b704734d0c25`，合法 v1/v2 与 legacy 身份按实际导出/KSP核对。pointer/ready 内部 formatVersion=1 不等于备份格式版本。原 settings/recordZone、generation、AppContainer 和 DB/DataStore/文件生命周期边界继承，不宣称跨存储原子。

最终完整Lint为0 errors/20 warnings，与既有基线相同；未抑制提示或升级依赖。完整四任务最终 `BUILD SUCCESSFUL in 13m 30s`，退出0。工具链/运行依赖、最终合并Manifest、APK签名与九域排除核查均有实际文件证据见 [JSON](M08-verification.json)及 `m08-tests/final-*` 构件核查文本。

## 12. 文件、Git、未运行项及独立验收边界

提交前实际核对为新增97、修改24、删除0；882个本地文件链接均存在，指定敏感信息模式匹配为0。source-provenance.zip的755份Kotlin源码CRC、路径与相同敏感信息模式检查均通过。完整清单见 [M08-files](M08-files.md)，最终自提交SHA与推送状态依据实际Git日志及交付回执，不递归写入本提交。原始ZIP/43个不可变原件/24模块/130 NOT_RUN案例、16组完整跨模块补测和所有旧交付证据保留；本轮结果只登记当前验收矩阵，不伪造后续PASS。

| Git 项目 | 当前已知事实 / 待补结果 |
|---|---|
| 仓库 | `cc7279694-debug/property`，指定工作目录 `E:/CODEX/砺石新APP` |
| 来源/起点 | `feat/m07-project-lifecycle` / `fcb850f549285d310040318a5f7adfd51abb4c33`；开始本地/远端一致、工作区干净 |
| 当前目标分支 | `feat/m08-subscription-renewals`，从冻结 M07 创建；不从 main 开始 |
| 提交 SHA / 推送 | 本报告编写时尚未提交/推送，不提前声明成功；只提交/推送当前M08分支，实际SHA由提交后的分支log及最终Git回执给出，避免文档引用自己的递归SHA。无force、main合并或APK发布授权 |
| 交付前范围/敏感信息/链接 | 已实际核对原文件/旧证据哈希、完整变更清单、本地文件链接及指定keys/env/DB/APK模式；97新增/24修改/0删除，882链接无缺失，指定敏感模式0匹配。受保护分支与最终提交blob仍在Git交付时再次核对，结果以实际回执为准 |
| 交付后校验 | 必须依据推送后的实际本地/远端HEAD、工作区、测试输入/提交blob及受保护引用核对；结果以最终Git回执为准，不预测远端状态 |
| 独立验收 | **PENDING**；内部代码审阅不是 ChatGPT 独立验收，不授予 ACCEPTED/FROZEN |

提交前标准 `git diff --cached --check` 返回空白提示：已实测 `SubscriptionUiModels.kt` 第71行末尾空行，以及原始 `red-root-back-and-real-flow.log` 第139行仅含tab。逐项核对仅这两处，无其它空白问题；登记为格式检查 PASS_WITH_NOTES，保留实际测试输入与失败日志字节，不将标准命令的非零退出写成退出0。它们不影响编译或产品断言。

继续保留如下 NOT_RUN 或模块边界：

- 手机、API26 运行、OEM 迁移/备份、完整 TalkBack、真实低内存自动回收、Release 构建、正式签名与 G1 均 NOT_RUN；模拟器不称真机。
- SavedState 仅覆盖系统已经保存的任务，不保证未保存状态、force-stop 或断电后的草稿恢复。分页是查询范围/正确性证明，不是长期性能基准；必要完整历史的成本计算不保证无限数据恒定耗时。
- M09 的 STOPPED 后重新启用、历史/纠错和多期预付默认停止边界尚未定案/实施；M08 没有暂停恢复停止 UX、退款、期中补差、自动付款。
- Q03 在 M21、Q04 在 M20、Q05 在 M10/M12、Q06 在 M12 的处理期限/状态保留。Q02 的 M17/M18 集成与 Q06-V01～V03 完整切换/回退仍 NOT_RUN。四独立日期不意味着本轮关闭 Q04 或实现通知。
- M04 初始化中断后的残留 generation 不得静默清除真实数据。M11/M12 完整备份恢复未完成；Debug 暂不用于不可丢失的长期个人记录。M10 媒体文件生命周期未实施。
- 继承 M07 ProjectAbsent 只证明当前不存在、多条旧 END_SETTLEMENT 明确冲突且无自动修复等 NOTES，保留全部历史独立审查未重跑本机 Gradle/安装的记录。
- 不新增 M13 汇总、云端、服务器/账号、AI、预算/估值/评分、图片管理、替换链、报告/排行榜、第二套数据 owner；未触碰 WHOCARE 或 main。TypeScript/ESLint 不适用，NO-SOURCE 不计 PASS。

最终四任务与设备收尾已取得本报告记录的真实证据。按本轮授权只完成当前分支的提交/推送及实际回执，随后停止等待独立验收；M09不启动，不自行宣布ACCEPTED/FROZEN。

**READY_FOR_REVIEW — M08**

独立验收：**PENDING**。
