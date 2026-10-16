# UI-C1 Task8 最终测试与T01～T14精确对照

编制日期：2026-10-16。本文只连接实际自动化结果与既定断言范围；**不授予T01～T14整项验收、视觉PASS、UI-C1接受或M10接受，不替代root的真实IME、系统字体、最终原图及数据安全手工证据。**

## 最终运行身份与实际结果

- phase：`stage2-final-restored-full`。
- frozenSourceHead：`6a25965225ace3212d1653d15b45fc56ef87411e`。
- sourceInputsSha256：`6e10034a61e9ffc666be545645b04ea5a13bc022aa510cb5dde97694485a4f2f`；按最终summary源输入清单的规范JSON重新计算一致。
- 源码ZIP：`stage2-final-restored-full-source.zip`，SHA-256 `296a8942079262cea9b614136df488facdbab4bab38418deba6abfd855383dcd`；实际文件重新校验一致。
- summary：`stage2-final-restored-full-summary.json`，SHA-256 `540d56738930655fa28c2dbacc8c17170e5cd67bac2e94d2610fc6ab9c22f853`。
- 原701目录：`baseline-methods.json`，SHA-256 `4155281b68bcd75fdc98597a280f3efa976dd0867ce820a4561f48052a34dffb`；确切class#method全部在本次XML中。
- 本次实际 **718个唯一方法＝86 JVM＋632 Android；715 PASS／3 FAIL／0 SKIPPED；overall FAIL**。JVM86 PASS；Android629 PASS／3 FAIL。
- 原701实际698 PASS／3 FAIL；相对原701新增17个Android方法，本次17 PASS。方法身份存在不等于原断言保留；原断言/保护区字节审计仍是单独静态证据。
- 本稿对8份XML重新核对SHA-256、字节数和根计数，并将所有718条 `(kind,class,name,result)` 与summary精确比较；无重复方法、无缺失、无未匹配。
- 从早期crosswalk提取的T01～T14 **53次代表方法引用／47个不同方法**，逐个在最终summary和XML精确匹配，全部实际PASS。复用方法与后文新增清单不重复增加718总数。新空Room producer另作明确补充。

原始证据在 `build/ui-c1-stage2/`。本文将归档至 `docs/evidence/ui-c1/stage2/task8/`；以下用原始文件名＋实际XML行号与纯路径code定位，避免假设尚未记录的复制子目录。全量原summary/XML/log必须与本文一并归档，不能只保留此摘要。

| 实际命令 | 退出码 | 时长 | 原日志与核对过的SHA-256 |
|---|---:|---:|---|
| `:app:testDebugUnitTest` | 0 | 10.446秒 | `stage2-final-restored-full-0.log`；`1bf43f828c2575280541de0c6885db3b7891eafbb03e88940486095da4e5ae34` |
| `:app:connectedDebugAndroidTest` | 1 | 1054.941秒 | `stage2-final-restored-full-1.log`；`bbea2288bd42deeab7b6b9aa9ac8853c21172445dff5adb0f4a142cf02b8c75a` |
| `:app:lintDebug :app:assembleDebug` | 0 | 64.956秒 | `stage2-final-restored-full-2.log`；`8c1b53e366565e1af7999f76f33dec2cf395e380c9c3038e34c9e8df331ffa22` |

Android命令无class/method过滤，summary声明只统计新鲜输出；本稿未重跑测试。三个命令的输入稳定标记均为true，日志实际SHA核对一致。Lint/assemble退出0不等于每个缓存任务重跑，也不覆盖Android三项FAIL。

## 覆盖类型与解释

- **P**：实际生产Compose＋给定状态/回调计数。只证明该展示/原始值/回调契约，不证明VM校验或落库。
- **R**：真实VM/Repository/Room/managed generation隔离fixture。部分分页只读fixture直接种DAO事实；不叫真实MainActivity或原用户账本。
- **J**：JVM冻结领域计算；不证明界面、点击或IME。
- **A**：实际MainActivity/Application owner；只有方法明确设置的旅程前提有效。
- **L2**：360dp宿主约束＋LocalDensity字体2.0；不等于Android系统200%字体或可见native IME。
- **V**：运行截图producer；只能证明该次方法完成及其内部断言。图像来源绑定、逐张看图与独立视觉接受分别完成。

下列英文断言与边界继承此前逐方法源码审读crosswalk；每个方法新增的PASS、原XML位置与耗时均来自本次最终原XML。每节“仍待手工/额外证据”明确未由这些自动化方法证明，不把整项T条件直接标PASS。

## T01 — 同结构深浅；切系统主题零业务写入

**本次代表方法：3次精确引用，均实际PASS；整项验收仍需下列范围外证据。**

- `com.cooper.lithic.ui.UIC1ThemeTest#dual_theme_uses_warm_page_and_ink_green_actions_with_readable_pairs` **P**: actual Light/Dark color tokens, 4.5:1 text pairs/card text and 3:1 outline checks. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:677`；1.904秒。

- `com.cooper.lithic.ui.UIC1RefinedVisualEvidenceTest#isolated_synthetic_records_and_original_covers_render_three_pages_in_both_themes` **R/V**: same navigation in explicit composition themes; before/after project/subscription facts and cover bytes equal. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:669`；7.125秒。

- `com.cooper.lithic.ui.UIC1Stage2EvidenceTest#isolated_subscription_history_operations_and_error_states_supply_stage2_evidence` **R/V** plus explicitly injected **P** error states: dual composition themes; fixture rows/cover bytes/record zone unchanged. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:675`；12.692秒。

**仍待手工/额外证据：** capture all 8 normal page pairs from the same installed final APK. Separately switch the actual system theme and compare the real seven-table facts, cover bytes, settings, record zone and generation/pointer; explicit `darkTheme` changes in tests do not prove this system journey.

## T02 — 首页真实卡片/空态，无假汇总

**本次代表方法：4次精确引用，均实际PASS；整项验收仍需下列范围外证据。**

- `com.cooper.lithic.app.UIC1HomePreviewTest#full_identity_counts_and_current_previews_use_existing_bounded_interfaces` **R**: 56/6 identities versus current subsets55/5; previews limited3 each, common observation day and bounded existing interfaces. The fixture is seeded, not claimed as creation UI. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:14`；0.312秒。

- `com.cooper.lithic.feature.home.UIC1HomeSampleTest#recorded_overview_does_not_offer_an_unimplemented_cost_summary` **P**: real count labels and no unimplemented daily-cost total. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:399`；1.952秒。

- `com.cooper.lithic.feature.home.UIC1HomeSampleTest#successful_empty_home_has_one_first_record_action_instead_of_a_fab` **P**: one empty hero action; no duplicate create/FAB controls. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:398`；4.752秒。

- `com.cooper.lithic.feature.home.UIC1RefinedHomeTest#overview_has_two_separate_real_numeric_metrics_with_history_scope` **P**: separate project/subscription count metrics and historical scope, not derived financial aggregates. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:402`；1.749秒。

**仍待手工/额外证据：** final MainActivity populated home Light/Dark; separate isolated empty-room Light/Dark captures clearly labelled. Include history-only behavior and one-type-empty presentation without fabricating official ledger facts.

## T03 — 弹层关闭零写，两原创建路径

**本次代表方法：4次精确引用，均实际PASS；整项验收仍需下列范围外证据。**

- `com.cooper.lithic.feature.home.UIC1HomeSampleTest#dismissing_the_add_sheet_does_not_start_either_creation` **P**: both callbacks remain0 and sheet closes. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:393`；6.377秒。

- `com.cooper.lithic.feature.home.UIC1HomeSampleTest#project_option_only_calls_the_original_project_creation_callback` **P**: project callback1/subscription0 and sheet dismissed. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:394`；6.105秒。

- `com.cooper.lithic.feature.home.UIC1HomeSampleTest#dark_home_uses_the_same_subscription_creation_path` **P**: subscription callback1/project0 in Dark, same sheet behavior. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:397`；4.147秒。

- `com.cooper.lithic.ui.UIC1EmptyVisualEvidenceTest#isolated_empty_home_is_real_in_both_themes_and_cancel_add_does_not_write` **R/V**: real empty repositories remain0 after opening/dismissing the original navigation sheet in both themes. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:664`；4.41秒。

**仍待手工/额外证据：** final MainActivity add-sheet Light/Dark, dismissal and original guarded destinations. Callback-only P tests do not prove persisted zero-write or hidden-pending guards; combine R and T09 evidence.

## T04 — 原排序/分页/当前历史，无假搜索筛选

**本次代表方法：4次精确引用，均实际PASS；整项验收仍需下列范围外证据。**

- `com.cooper.lithic.data.repository.ProjectLifecycleRepositoryTest#current_and_history_are_filtered_in_sql_and_bounded_by_stable_fifty_row_pages` **R**: 101 history/103 current, stable distinct IDs, SQL filtering and bounded50-row pages; future-started facts remain current but heldDays0. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:256`；0.471秒。

- `com.cooper.lithic.feature.subscription.SubscriptionRenewalFlowTest#paid_on_history_and_list_order_with_real_paging` **R**: DAO-seeded read fixture, actual payment-date list ordering and real50+2 history paging, no duplicate IDs and stable order after observation-day change. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:599`；13.308秒。

- `com.cooper.lithic.feature.project.ProjectScreensTest#project_list_opens_stable_identity_and_exposes_database_page_continuation` **P**: original identity click and continuation callback stay reachable. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:515`；3.329秒。

- `com.cooper.lithic.feature.subscription.M09SubscriptionScreensTest#current_management_has_independent_active_paused_expired_groups` **P**: current management's three real domain status groups retained. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:580`；2.006秒。

**仍待手工/额外证据：** final current/history pages and necessary scroll/load-more evidence. Inspect production sources and final captures for absent unimplemented search/filter controls; pagination tests alone do not prove that negative visual requirement.

## T05 — 真实/无/坏/缺图、并发/采样/缓存与零业务写入

**本次代表方法：4次精确引用，均实际PASS；整项验收仍需下列范围外证据。**

- `com.cooper.lithic.feature.media.UIC1CoverPreviewTest#cached_success_does_not_hide_deleted_or_corrupted_private_file` **R**: successful cache does not hide later deleted/corrupted bytes on reentry. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:435`；1.475秒。

- `com.cooper.lithic.feature.media.UIC1CoverPreviewTest#read_and_decode_concurrency_stays_at_two_and_cancel_releases_slot` **R**: real asset + observed read/decode peak≤2; cancellation releases queued work. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:437`；1.454秒。

- `com.cooper.lithic.feature.media.UIC1CoverPreviewTest#repository_switch_rejects_late_old_result_and_never_shares_old_success` **R/P**: delayed old facade result cannot replace the switched facade's visible error. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:426`；1.649秒。

- `com.cooper.lithic.feature.media.UIC1CoverPreviewTest#shared_cover_reads_leave_both_references_metadata_and_file_bytes_unchanged` **R**: two references, metadata, validated bytes and actual private-file bytes remain equal after repeated preview loads. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:430`；1.72秒。

**仍待手工/额外证据：** capture real thumbnail/detail, unset placeholder, unreadable referenced cover and viewer/error/management entry with source labels. Join full class results for other existing unset/progress/sampling/LRU cases. A rendered JPEG is never a JPEG-input integrity PASS; retain the three known rejection FAILs.

## T06 — 项目金额/时间/结束结算不变

**本次代表方法：4次精确引用，均实际PASS；整项验收仍需下列范围外证据。**

- `com.cooper.lithic.feature.project.ProjectEntryFlowTest#create_zero_and_normal_project_are_real_room_records_after_reopen` **R**: actual create UI writes distinct0/12345-cent facts and retains them, count and record zone after fixture reopen. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:446`；10.949秒。

- `com.cooper.lithic.feature.project.ProjectEntryFlowTest#backdated_money_recalculates_detail_list_home_and_negative_recovery_from_real_room` **R**: detail/list values follow real dated facts, negative net remains negative, earlier-day values and reopen facts checked. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:443`；16.755秒。

- `com.cooper.lithic.feature.project.ProjectLifecycleFlowTest#history_correction_and_real_refund_are_distinct_and_preserve_original_identity` **R**: ended-history correction retains event/operation identity, real refund is an extra fact, held days remain5 and current contribution staysfalse. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:456`；21.055秒。

- `com.cooper.lithic.domain.project.ProjectCostEngineTest#post_end_refund_keeps_duration` **J**: post-end refund changes net/daily amount without extending held days or reactivating current contribution. **最终XML：PASS**；`stage2-final-restored-full-jvm-3.xml:19`；0.001秒。

**仍待手工/额外证据：** final project detail plus ended-project money correction/late settlement and dangerous confirmation scroll shots. Rendered amounts must match production-domain results, including zero/negative/subcent values.

## T07 — 订阅主次指标/时长/状态不混淆

**本次代表方法：4次精确引用，均实际PASS；整项验收仍需下列范围外证据。**

- `com.cooper.lithic.feature.subscription.UIC1SubscriptionVisualTest#prepaid_future_period_is_compared_with_elapsed_days_without_replacing_current_plan` **P** with frozen-engine fixture: explicitly compares historical7.50, standard60 days, elapsed25, current5.00 and future-payment label. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:634`；2.38秒。

- `com.cooper.lithic.domain.subscription.SubscriptionCostEngineTest#annual_purchase_not_elapsed_year` **J**: standard365 is fixed while elapsed varies1/100/365; expired current rate becomes0. **最终XML：PASS**；`stage2-final-restored-full-jvm-6.xml:16`；0.0秒。

- `com.cooper.lithic.domain.subscription.SubscriptionCostEngineTest#prepaid_next_rate_does_not_start_early` **J**: a confirmed future payment enters historical totals/standard days without changing current rate before effective boundary. **最终XML：PASS**；`stage2-final-restored-full-jvm-6.xml:20`；0.001秒。

- `com.cooper.lithic.domain.subscription.SubscriptionCostEngineTest#quarterly_standard_rate` **J**: fixed quarterly90-day standard rate. **最终XML：PASS**；`stage2-final-restored-full-jvm-6.xml:13`；0.0秒。

**仍待手工/额外证据：** final subscription detail in both themes; current-plan and payment-history scroll supplements with future coverage and pause/expiry/stop facts. The chosen four are representative; full engine/state tests must supply remaining30-day/status cases. A screenshot of “paid duration” cannot be relabelled “already used”.

## T08 — 原动作可达，结束/删除/服务商说明准确

**本次代表方法：4次精确引用，均实际PASS；整项验收仍需下列范围外证据。**

- `com.cooper.lithic.feature.project.UIC1ProjectSampleTest#more_actions_keep_ending_separate_from_deleting_and_use_existing_callbacks` **P**: menu has both actions, opening writes/calls neither, choosing End invokes only its original callback. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:537`；2.655秒。

- `com.cooper.lithic.feature.project.ProjectLifecycleFlowTest#history_correction_and_real_refund_are_distinct_and_preserve_original_identity` **R**: actual ended-history correction and real-refund route are exercised with persisted facts. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:456`；21.055秒。

- `com.cooper.lithic.feature.subscription.M09SubscriptionLifecycleFlowTest#payment_history_correction_and_delete_require_visible_confirmation` **R**: amount correction cannot write before confirm; wrong-payment deletion preserves last-payment protection and correct totals/standard days. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:548`；7.066秒。

- `com.cooper.lithic.feature.subscription.M09SubscriptionLifecycleFlowTest#stop_reason_does_not_cancel_vendor` **R**: vendor disclaimer visible, Back leaves no state write, explicit confirm persists reason while payment history stays equal. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:546`；5.075秒。

**仍待手工/额外证据：** include delete/end/stop/correct/cancel/reenable/cover operations and confirmation screenshots/scroll supplements. These four do not alone establish every action; join full original lifecycle/navigation classes and an actual-operation inventory, retaining any unexercised mode as NOT_RUN.

## T09 — 三VM、Back/旋转/固定身份/恢复不回归

**本次代表方法：4次精确引用，均实际PASS；整项验收仍需下列范围外证据。**

- `com.cooper.lithic.feature.project.ProjectNavigationGuardTest#dirty_create_form_guards_same_tab_other_tabs_toolbar_and_system_back` **R**: each leave path asks discard; Keep retains raw name/amount and no Room record; explicit discard navigates with zero write. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:499`；15.728秒。

- `com.cooper.lithic.feature.subscription.SubscriptionNavigationGuardTest#hidden_restored_pending_payment_blocks_project_entry_and_has_reachable_original_form` **R**: real commit with lost result, restored envelope blocks cross-type create, recovery replays same payment and yields one renewal/no project. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:591`；6.719秒。

- `com.cooper.lithic.feature.media.M10CoverNavigationGuardTest#home_real_system_back_preserves_hidden_pending_cover_and_has_reachable_recovery_route` **R**: real Back cannot finish host with hidden UNCONFIRMED cover; original saved envelope/recovery remains; project/money/media facts unchanged. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:407`；1.966秒。

- `com.cooper.lithic.ui.ActivityNavigationTest#activity_recreation_preserves_a_legal_selected_route_and_same_storage_owner` **A**: actual MainActivity recreation restores Settings and uses the same application-owned generation. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:640`；2.462秒。

**仍待手工/额外证据：** actual system Back/rotation/task recovery plus dirty/IN_FLIGHT/UNCONFIRMED/COMMITTED combinations, including multiple simultaneous pending recovery buttons at200% font. Activity recreation is not process death/LMK; selected methods do not prove every phase combination.

## T10 — 初始化/空/错/上次数据明确

**本次代表方法：4次精确引用，均实际PASS；整项验收仍需下列范围外证据。**

- `com.cooper.lithic.ui.StorageUiStateTest#initializing_shows_loading_without_normal_empty_navigation` **R**: blocked real initialization shows loading, no fake empty/zero/navigation; readiness only after release. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:660`；1.179秒。

- `com.cooper.lithic.ui.StorageUiStateTest#actual_initialization_failure_has_protective_error_and_working_exit` **R**: thrown initialization failure produces protective UI/working exit, hides private error string and creates no active pointer. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:663`；2.498秒。

- `com.cooper.lithic.feature.subscription.UIC1SubscriptionVisualTest#current_and_stopped_lists_distinguish_loading_failed_empty_and_stale_original_rows` **P**: all four supplied states in both list types, retry and stable identity clicks, never treats failure as successful empty. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:637`；6.098秒。

- `com.cooper.lithic.feature.subscription.UIC1SubscriptionVisualTest#detail_distinguishes_loading_error_missing_and_stale_facts_with_mutations_disabled` **P**: separate missing/error/stale detail and disabled mutations, working read retry/back with mutation counter0. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:635`；5.287秒。

**仍待手工/额外证据：** final-input empty/read-error/stale/cover-error originals with explicit actualMainActivity versus isolated/presentation-injection classifications. Injected state screenshots are not proof of real storage corruption/recovery.

## T11 — 360dp200%/长名长负额/真实IME可达

**本次代表方法：4次精确引用，均实际PASS；整项验收仍需下列范围外证据。**

- `com.cooper.lithic.ui.UIC1ThemeTest#compact_cards_keep_long_negative_subcent_zero_and_missing_values_distinct` **P/L2**: actual text layouts and semantic amounts distinguish long negative, subcent,0 and missing. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:678`；1.687秒。

- `com.cooper.lithic.ui.UIC1FormVisualTest#dark_money_form_keeps_raw_precision_date_and_wrapped_failure_reachable` **P/L2**: whitespace/raw precision, invalid raw date, error semantics/layout, save/back targets and retained description. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:667`；3.873秒。

- `com.cooper.lithic.feature.project.ProjectLifecycleScreensTest#end_and_correction_forms_are_reachable_at_360dp_with_200_percent_font` **P/L2**: scroll reaches end-recovery/correction fields and48dp save targets; callback counters2. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:464`；2.999秒。

- `com.cooper.lithic.feature.subscription.SubscriptionRenewalFlowTest#narrow_large_font_real_ime_saved_task_and_confirmation` **R/L2**: restored VM+saved UI task retains all raw dates/amount, dirty Back/tab guards and explicit one-renewal confirmation. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:598`；18.319秒。

**仍待手工/额外证据：** actual device360dp and Android system font2.0, native IME visible with focus/insets recorded, real typing/keyboard Back/error/save/return reachability, long names/negative values and normal+scroll shots. Despite its name, the selected subscription method uses LocalDensity and `performTextReplacement`/keyboard-close calls; it does not assert an actually visible native IME. Do not promote it to that manual claim.

## T12 — 实际配对/标签/触控/焦点/状态可读

**本次代表方法：4次精确引用，均实际PASS；整项验收仍需下列范围外证据。**

- `com.cooper.lithic.ui.UIComponentsTest#controlled_fields_keep_input_show_associated_errors_and_deliver_ime_callbacks` **P**: labelled controlled fields, matching Error semantics, raw text, actual Compose IME-action callbacks exactly once. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:686`；3.311秒。

- `com.cooper.lithic.feature.project.ProjectDateInputTest#busy_date_input_and_calendar_action_are_disabled_and_touch_target_is_large_enough` **P**: disabled raw field/calendar action and≥48dp calendar target. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:440`；1.764秒。

- `com.cooper.lithic.feature.project.UIC1RefinedDetailTest#long_return_name_at_200_percent_uses_non_overlapping_48dp_controls_with_full_accessible_names` **R/P/L2**: full accessible back/more names, measured≥48dp targets and no overlap, original Back callback. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:539`；5.606秒。

- `com.cooper.lithic.ui.UIC1ThemeTest#dual_theme_uses_warm_page_and_ink_green_actions_with_readable_pairs` **P**: quantitative readable color pairs, including error/container colors. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:677`；1.904秒。

**仍待手工/额外证据：** actual focus order and usable field/error/button states in final forms/dialogs with native IME; confirm TalkBack scope explicitly. These semantics/geometry assertions do not establish full TalkBack navigation or every target's physical48dp reachability.

## T13 — 断网冷启/浏览/旧操作，无新权限/持久结构

**本次代表方法：3次精确引用，均实际PASS；整项验收仍需下列范围外证据。**

- `com.cooper.lithic.ui.ActivityNavigationTest#system_back_returns_to_home_then_finishes_the_actual_activity` **A**: real Activity reaches review/home and its Back dispatcher finishes normally; no offline network precondition is asserted. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:641`；3.579秒。

- `com.cooper.lithic.feature.project.ProjectEntryFlowTest#create_zero_and_normal_project_are_real_room_records_after_reopen` **R**: local creation/reopen retains actual data and zone. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:446`；10.949秒。

- `com.cooper.lithic.feature.subscription.SubscriptionRenewalFlowTest#renewal_needs_confirmation` **R**: local actual renewal pipeline, no pre-confirm write, real SQLite failure/conflict/envelope protection, fresh explicit confirmation and stored payment identities. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:597`；32.464秒。

**仍待手工/额外证据：** actual final MainActivity cold start with Wi-Fi/mobile/VPN off, browse and approved basic local operations; restore device network afterward. Check final APK merged permissions, schema resources/Roomv2, dependencies, backup exclusions and protected-source fingerprints. **None of these three methods alone proves offline cold start, APK permissions, or unchanged schema.**

## T14 — 用户先看样板再扩展；视觉/代码/独立验收分开

**本次代表方法：3次精确引用，均实际PASS；整项验收仍需下列范围外证据。**

- `com.cooper.lithic.ui.UIC1RefinedVisualEvidenceTest#isolated_synthetic_records_and_original_covers_render_three_pages_in_both_themes` **R/V**: three-page runtime fixture capture machinery and no-write assertions; stage1 historical images cannot represent final input. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:669`；7.125秒。

- `com.cooper.lithic.ui.UIC1EmptyVisualEvidenceTest#isolated_empty_home_is_real_in_both_themes_and_cancel_add_does_not_write` **R/V**: separately labelled empty-room fixture, not actual MainActivity. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:664`；4.41秒。

- `com.cooper.lithic.ui.UIC1Stage2EvidenceTest#isolated_subscription_history_operations_and_error_states_supply_stage2_evidence` **R/V + P/V**:14 supplemental runtime captures with explicit navigation/error-source classes, host and image hashes. **最终XML：PASS**；`stage2-final-restored-full-android-0.xml:675`；12.692秒。

**仍待手工/额外证据：** user confirmed f00063927-image baseline and ChatGPT PASS WITH NOTES are the authorization evidence, not automated tests. Acquire fresh same-final-APK eight normal page pairs (16 originals), additional empty/error/font/IME/history/danger/scroll images, full per-image APK/input/device/host manifest, direct comparison with the two approved reference originals, and user/ChatGPT independent final review. This local crosswalk cannot grant ACCEPTED/FROZEN; final implementation stops READY_FOR_REVIEW.

## 新空Room producer补充：T02／T03／T10／T14

`com.cooper.lithic.ui.UIC1Stage2EmptyEvidenceTest#isolated_empty_room_home_supplies_final_stage2_dual_theme_evidence_without_writes` — **PASS**；`stage2-final-restored-full-android-0.xml:674`；5.039秒；类型 **R/V**。

这是新空Room方法，修正早期crosswalk尚为717／新增16的旧清单。实际源码使用独立UUID generation、真实生产readHomeOverview/Repository读取零项目/零订阅/空预览；深浅开启并关闭新增弹层后仍为零，原事实/时区/原active pointer不变，只清理本fixture UUID根。它记录实际有焦点的ComponentActivity、installedApkSha256、system uiMode与显式Compose主题。它**不是MainActivity**，不能替代最终16张MainActivity正常图，也不证明系统主题切换。外部final APK/input/device manifest、最终原图和直接视觉审查仍由root完成。

## 新增17个方法：本次均实际PASS

全部身份由“最终XML集合减原701身份集合”获得，未用源码数量代替执行；本节重复列出的身份不重复计数。P/R/L2/V边界按上文定义，截图产生不等于视觉通过。

| 确切class#method | 类型 | 最终XML实际结果/位置 |
|---|---|---|
| `com.cooper.lithic.feature.subscription.UIC1SubscriptionVisualTest#compact_management_header_keeps_first_service_visible_and_accessible_actions_working` | P | **PASS**；`stage2-final-restored-full-android-0.xml:638` |
| `com.cooper.lithic.feature.subscription.UIC1SubscriptionVisualTest#current_and_stopped_lists_distinguish_loading_failed_empty_and_stale_original_rows` | P | **PASS**；`stage2-final-restored-full-android-0.xml:637` |
| `com.cooper.lithic.feature.subscription.UIC1SubscriptionVisualTest#detail_distinguishes_loading_error_missing_and_stale_facts_with_mutations_disabled` | P | **PASS**；`stage2-final-restored-full-android-0.xml:635` |
| `com.cooper.lithic.feature.subscription.UIC1SubscriptionVisualTest#prepaid_future_period_is_compared_with_elapsed_days_without_replacing_current_plan` | P | **PASS**；`stage2-final-restored-full-android-0.xml:634` |
| `com.cooper.lithic.feature.subscription.UIC1SubscriptionVisualTest#referenced_unreadable_cover_is_distinct_from_no_cover_and_keeps_original_identity_clickable` | P | **PASS**；`stage2-final-restored-full-android-0.xml:639` |
| `com.cooper.lithic.feature.subscription.UIC1SubscriptionVisualTest#stop_record_action_keeps_provider_cancellation_disclaimer_visible_before_callback` | P | **PASS**；`stage2-final-restored-full-android-0.xml:636` |
| `com.cooper.lithic.ui.UIC1FormVisualTest#dark_committed_reenable_keeps_wrapped_header_read_only_values_and_only_read_recovery` | P/L2 | **PASS**；`stage2-final-restored-full-android-0.xml:666` |
| `com.cooper.lithic.ui.UIC1FormVisualTest#dark_money_form_keeps_raw_precision_date_and_wrapped_failure_reachable` | P/L2 | **PASS**；`stage2-final-restored-full-android-0.xml:667` |
| `com.cooper.lithic.ui.UIC1FormVisualTest#light_project_header_wraps_and_zero_amount_keeps_save_and_back_callbacks` | P/L2 | **PASS**；`stage2-final-restored-full-android-0.xml:665` |
| `com.cooper.lithic.ui.UIC1FormVisualTest#light_subscription_form_keeps_four_raw_dates_error_and_explicit_payment_confirmation` | P/L2 | **PASS**；`stage2-final-restored-full-android-0.xml:668` |
| `com.cooper.lithic.ui.UIC1SecondaryPagesTest#project_history_loading_or_read_failure_never_claims_a_successful_empty_archive` | P | **PASS**；`stage2-final-restored-full-android-0.xml:671` |
| `com.cooper.lithic.ui.UIC1SecondaryPagesTest#review_only_offers_real_history_routes_without_guessing_ledger_absence` | P | **PASS**；`stage2-final-restored-full-android-0.xml:673` |
| `com.cooper.lithic.ui.UIC1SecondaryPagesTest#settings_exposes_installed_version_and_persisted_zone_as_read_only_information` | P | **PASS**；`stage2-final-restored-full-android-0.xml:670` |
| `com.cooper.lithic.ui.UIC1SecondaryPagesTest#settings_keeps_backup_warning_prominent_and_has_no_unimplemented_reminder_entry` | P | **PASS**；`stage2-final-restored-full-android-0.xml:672` |
| `com.cooper.lithic.ui.UIC1Stage2EmptyEvidenceTest#isolated_empty_room_home_supplies_final_stage2_dual_theme_evidence_without_writes` | R/V | **PASS**；`stage2-final-restored-full-android-0.xml:674` |
| `com.cooper.lithic.ui.UIC1Stage2EvidenceTest#isolated_subscription_history_operations_and_error_states_supply_stage2_evidence` | R/V＋注入P/V | **PASS**；`stage2-final-restored-full-android-0.xml:675` |
| `com.cooper.lithic.ui.UIComponentsTest#synthetic_form_save_target_stays_above_system_navigation_insets` | P/L2 | **PASS**；`stage2-final-restored-full-android-0.xml:681` |

## 三项原JPEG拒绝失败：本次精确结果

下列是最终全部失败集合，三项均为原701身份。connected退出1、Android629 PASS／3 FAIL与overall FAIL均保留。有效JPEG导入或解码诊断PASS不能取代拒绝断言。

| 确切class#method | 实际结果/位置 | 失败断言范围 |
|---|---|---|
| `com.cooper.lithic.data.media.M10JpegIntegrityTest#jpeg_entropy_truncated_with_eoi_is_rejected` | **FAIL**；`stage2-final-restored-full-android-0.xml:159`；0.651秒 | 要求保留EOI但截断熵编码的JPEG被拒绝，实际返回PreparedImage成功 |
| `com.cooper.lithic.data.media.M10JpegOldCoverProtectionTest#project_entropy_cut_jpeg_is_rejected_and_old_cover_and_money_are_preserved` | **FAIL**；`stage2-final-restored-full-android-0.xml:175`；5.186秒 | 要求真实JPEG来源返回INVALID_IMAGE，实际返回PreparedImage成功 |
| `com.cooper.lithic.data.media.M10JpegOldCoverProtectionTest#subscription_entropy_cut_jpeg_is_rejected_and_old_cover_payments_and_states_are_preserved` | **FAIL**；`stage2-final-restored-full-android-0.xml:193`；5.095秒 | 要求真实JPEG来源返回INVALID_IMAGE，实际返回PreparedImage成功 |

这三项停在拒绝/INVALID_IMAGE断言；不能由方法名推断后续旧图/金额/付款/状态保护断言均已执行或通过。没有修改图片、改拒绝预期或把诊断成功改写为完整性成功。

## 旧合成保存偶发超时：本次PASS，历史根因仍未知

- 原方法 `com.cooper.lithic.ui.UIComponentsTest#large_text_synthetic_form_keeps_errors_save_and_return_reachable_after_input` 本次 **PASS**；`stage2-final-restored-full-android-0.xml:684`；17.324秒。
- 独立安全边界方法 `com.cooper.lithic.ui.UIComponentsTest#synthetic_form_save_target_stays_above_system_navigation_insets` 本次 **PASS**；`stage2-final-restored-full-android-0.xml:681`；2.676秒。

本次结果不撤销精修历史失败和Task7诊断的不确定性。原10,000ms等待与原始输入/错误/恰好一次保存/返回断言保留；测得的系统导航重叠由测试宿主safeDrawing修正，旧同步回调超时仍 **INTERMITTENT / ROOT_CAUSE_UNKNOWN**。合成表单没有VM/Repository/数据库，且其历史keyboard命名照片没有可见软件键盘；不能把其PASS解释为真实业务保存、实际native IME或根因关闭。

## XML身份登记

| 原始XML | 实际方法计数/失败 | bytes | 已重新核对的SHA-256 |
|---|---|---:|---|
| `stage2-final-restored-full-jvm-0.xml` | 8方法／0失败／0错误／0跳过 | 1532 | `b5321891421753a3281ea3e5d2951d30b438d3d75d19daa7d7449b3f22ccd453` |
| `stage2-final-restored-full-jvm-1.xml` | 7方法／0失败／0错误／0跳过 | 1220 | `06e87724c8641796be38fbcbd18f53f4b2364141f7a4fe7cee311363c768ade8` |
| `stage2-final-restored-full-jvm-2.xml` | 2方法／0失败／0错误／0跳过 | 606 | `275531cae0d0a9f2b101d06eb696ab74c470cdc7ff9dada82ddb1cf8c1917545` |
| `stage2-final-restored-full-jvm-3.xml` | 22方法／0失败／0错误／0跳过 | 3512 | `98c4b76d813a21703966339ff0e832fc73d44d55723915eb1389d89a265e6762` |
| `stage2-final-restored-full-jvm-4.xml` | 4方法／0失败／0错误／0跳过 | 985 | `211abca1ce02c6ea73d8575f7b373f5bd36ad42f5d64da7a91f18e2790a4b4eb` |
| `stage2-final-restored-full-jvm-5.xml` | 6方法／0失败／0错误／0跳过 | 1372 | `d784d13bca67f4d085eed4c116560646f0aa97ade4f47c8045f3f0392a1de17a` |
| `stage2-final-restored-full-jvm-6.xml` | 37方法／0失败／0错误／0跳过 | 6101 | `b380ddf390e995e0610ad9ff4e48d7a25628f049b132036ae0de61f3a2a0b3b4` |
| `stage2-final-restored-full-android-0.xml` | 632方法／3失败／0错误／0跳过 | 110326 | `3ecca1a3e0ea64b779ee1d587a0cc2e9c1183875775f1524da95b666fa06726d` |

Android XML真实device property：`Lithic_M01_API_36(AVD) - 16`。该标签不是完整的每图设备/API/字体/密度/实际前台宿主记录；截图须用自己的真实运行元数据。

## 本稿明确未完成/未证明的验收

最终同APK的16张MainActivity原图、所有附图、逐张设计原图对照、系统主题切换零业务写入、真实360dp＋系统200%字体、真实可见IME/焦点/insets/错误/返回/提交可达、实际危险确认/滚动模式、断网冷启/本地操作、最终APK权限/签名/schema/打包身份和原账本完整事实集合/私有文件/设置前后精确不变，均应引用root各自运行证据；**本稿不因自动化PASS而给出这些手工结论**。

真机、API26、OEM、Release、完整TalkBack、真实LMK、断电、M11/M12完整备份恢复往返及未执行的具体模式继续NOT_RUN，除非root附新实际证据。Activity重建/系统保存任务恢复/force-stop/SIGKILL不得互换。M10保持独立PENDING。实现者停止READY_FOR_REVIEW，用户/ChatGPT独立验收仍单独进行。

本次只写 `build/ui-c1-stage2/final-test-crosswalk.md`；未操作设备、源码、正式文档或Git，也未运行构建/测试。
