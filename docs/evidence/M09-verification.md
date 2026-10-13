# M09 — 订阅状态与付款历史验证报告

日期：2026-10-13。**READY_FOR_REVIEW — M09；ChatGPT独立验收PENDING，未自授ACCEPTED/FROZEN，M10未授权。**

来源为用户完整M09指令，SHA-256 `d83fb2d77190b0c49df8b5adf9a6e13c8cae8ca203513f9f5e98b0729350eaab`。从M08冻结回写 `0063ca504ca48ef9c7231d79dc155e9bf4bc8c82` /feat/m08-subscription-renewals开始，开工来源本地/远端一致、干净、目标不存在；只feat/m09-subscription-lifecycle。契约见[M09_CONTRACT](../planning/M09_CONTRACT.md)。

最终四任务退出0、**Gradle日志13m 24s，命令墙钟805.631秒，86 JVM＋463 Android＝549唯一方法PASS（旧455＋新94），0失败/错误/跳过**。源码/XML EXACT MATCH、全部128构建输入哈希一致；Lint0errors/20warnings、Debug assemble完成。完整逐方法、工作区SHA/规范Git blob、12阶段及设备合成事实/文件哈希见[JSON](M09-verification.json)，所有修改见[文件清单](M09-files.md)。

## 1. 暂停、恢复、到期和停止

PAUSED/ENABLED/STOPPED按日期持久化，状态和成本继续统一调用冻结M03；覆盖排他边界直接EXPIRED/0/null，无Worker/虚构付款。暂停不改付款或标准已购时长、不进入停止历史；有效覆盖内恢复不付款，暂停日期不计实际有效天数。设备1/25暂停、1/26恢复仍两付款且实际25天；3/1无后台任务到期。列表分组为“暂停中”，详情标签为“已暂停”。

## 2. 用户确认的STOPPED同身份重新开通

复用原Subscription ID，完整付款/状态历史保留。覆盖内明确ENABLED即可ACTIVE，不新增付款；仅未来已付覆盖启用仍EXPIRED直到生效（正式Room/VM/Flow覆盖）。无当前或未来覆盖时，金额及真实付款日初始空白，用户显式填写后真实付款＋ENABLED同一事务；真实SQLite故障全部回滚。

实际合成服务ID `1ad7191c-4bb0-47e1-9c95-0bcd1f408ba0`：1/27覆盖内重开无付款；3/2无覆盖先核对空字段，再显式240、3/2付款与生效、4/2排他结束、4/1提醒，原ID上新增独立付款与当日ENABLED。两者同operationId，原两付款保留。原`atomic-reopened`快照仅失败意图后的SQL安全，不是业务成功；成功另名`atomic-reopened-verified`，不覆盖旧失败。

## 3. 多期预付默认停止及提前停止

默认确认时固定`max(业务今天,全部已确认真实付款的最晚paidUntilExclusive)`，含未来预付/覆盖空档；全部已到期则今天。仅到期后停止/明确提前停止，提前不得早于今天，不退款、不减少历史金额/标准天数。普通新续费或扩张纠错跨固定未来停止须明确修正/取消，不暗改边界；精确已存信封跨日恢复不重算日期。

实际150覆盖1/1～2/1，1/20预付180覆盖2/1～3/1，1/25默认计划3/1；之前ACTIVE，3/1 STOPPED。修正到2/28保留state ID/op，再取消仅目标行；1/26提前停止保留付款。空档、全到期、新付款冲突/金额-only纠错均由真实Room测试覆盖。

## 4. 状态时间线、同日修正及原子事务

六命令使用同一LocalWriteCoordinator＋Room事务：setState/updateState/cancelScheduledStop/updateRenewal/deleteRenewal/reenableWithRenewal。完整身份＋全部付款＋全部状态CAS，集合排序不影响结果，坏事实明确拒绝；同日只修正原state ID/operationId。原UI记录1/26恢复→提前停止同日原行身份不变。新信封UUID/时间/原事实/目标写前固定；只有完整已存目标可精确重放。

CurrentAbsence仅证明当前目标缺失，不证明永久删除/取消历史operation身份；没有新增操作日志/schema。COMMITTED只读，后来事实变化与真实读失败区分，显式核对安全返回；读Committed/resolveConflict均用detailVersion避免迟到结果重定向后来详情，不解除原pending。

## 5. 续费历史纠错与安全删除

修正原Renewal ID/Subscription ID/operationId，正额、固定30/90/365、四日期、未来付款、全覆盖重叠及固定停止边界在事务内校验；更新原事实，不追加金额。只经显式确认删除指定错误付款，提示不是退款/不可App撤回；最后一笔单独删除拒绝。陈旧CAS、真实SQL触发器故障回滚与同命令重试均实测。

实际把新240纠正225，原行/op不变；主动确认删除本次合成服务误录的1/20/180，其余旧付款完整。最终该服务150＋225＝375、标准60、实际有效60天；断网冷启EXPIRED、currentPrice为空/贡献0、历史平均6.25。旧M08所有业务行保持，未删除旧服务或原参考付款。

## 6. 正式UI分组和返回保护

现在管理：订阅中/暂停中/已到期；回看：已停止订阅。未来停止不提前归档，成功来源按新读取M03状态。详情显示真实套餐、历史平均、标准已购/实际有效天数、付款与必要状态事实及适用操作；底部仍三个入口。

单活动SavedState Bundle扩展状态/纠错/取消/删除/原子重开，保存raw、完整基线、固定UUID和submitted信封。IN_FLIGHT锁连点/输入，恢复为UNCONFIRMED；失败保留输入与安全重试；pending/dirty双VM全局守卫覆盖工具栏/Tab重选/系统与真实手势返回/隐藏表单/内部成功导航。UUID/来源NavKey有界，历史父链最多5级，坏恢复栈不暗中丢待确认付款。

## 7. M09-T01～T06实际结果

| 原案例 | 本轮执行结果 | 最终证据 |
|---|---|---|
| M09-T01 expiry_needs_no_worker | PASS | [expiry_needs_no_worker](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleFlowTest.kt)；最终XML及设备补证见JSON |
| M09-T02 cancel_renewal_keeps_paid_access | PASS | [cancel_renewal_keeps_paid_access](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleFlowTest.kt)；最终XML及设备补证见JSON |
| M09-T03 pause_is_not_history | PASS | [pause_is_not_history](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleFlowTest.kt)；最终XML及设备补证见JSON |
| M09-T04 resume_without_new_payment | PASS | [resume_without_new_payment](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleFlowTest.kt)；最终XML及设备补证见JSON |
| M09-T05 future_prepaid_has_own_label | PASS | [future_prepaid_has_own_label](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleFlowTest.kt)；最终XML及设备补证见JSON |
| M09-T06 stop_reason_does_not_cancel_vendor | PASS | [stop_reason_does_not_cancel_vendor](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleFlowTest.kt)；最终XML及设备补证见JSON |

当前登记独立于原130不可变NOT_RUN案例；实现者执行PASS不等于ChatGPT独立验收。

## 8. 455回归与新增94的真实数量

549唯一源码方法逐项对应最终XML，旧455完整保留。新94＝47 Repository＋19 LifecycleVM＋7正式Flow＋2NavFlow＋3Guard＋5Routes＋7Screens＋4基础VM。重复阶段不累计为新增；XML0failure/error/skip，JVM真实重跑而非NO-SOURCE/缓存冒充。方法、输入SHA及规范Git blob全部在JSON。

最终命令中JVM任务显式`--rerun`、Android463方法实际执行。Lint分析/报告与assemble为UP-TO-DATE，复用对应当前未变输入的结果；实际Lint XML为0错误/20警告，最终APK字节再次核对。没有把NO-SOURCE当测试PASS，也不声称本次重新分析了未变化的Lint输入。

四旧测试仅最小适配/驱动强化：read-failure注入改到实际loadManagementWindow；YEARLY真实IME关闭/布局稳定/选中检查；项目保存前捕获目标ID并等同ID详情；付款确认前收IME并检查可见/可用。原方法身份、金额/周期/365等业务断言、原超时保留。ProjectViewModel/Repository及M03生产基线不改。

## 9. 真实RED/GREEN、故障注入、并发和数据安全

## 22项风险

|#|用户风险|真实源码方法（绝对路径/行号）|已证范围与未证边界|
|---|---|---|---|
|1|同日暂停/恢复修正单一事实|[SubscriptionLifecycleRepositoryTest.same_day_resume_corrects_original_id_and_operation_instead_of_appending](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L84)<br>[SubscriptionLifecycleRepositoryTest.same_day_replacement_with_new_row_or_operation_identity_conflicts](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L97)<br>[M09SubscriptionLifecycleViewModelTest.covered_resume_corrects_same_day_boundary_without_new_payment](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleViewModelTest.kt#L57)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|2|停止→重开→再停止，历史日期正确|[SubscriptionLifecycleRepositoryTest.stopped_reopened_and_stopped_again_retains_same_identity_and_historical_days](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L343)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|3|未来计划修正/取消保留其他事实|[SubscriptionLifecycleRepositoryTest.scheduled_stop_correction_preserves_original_id_operation_and_other_history](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L452)<br>[SubscriptionLifecycleRepositoryTest.cancel_future_stop_deletes_only_target_and_retry_reports_current_absence](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L476)<br>[SubscriptionLifecycleRepositoryTest.new_stop_identity_conflicts_with_existing_future_plan_without_moving_its_boundary](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L213)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|4|两期预付空档默认最后边界|[SubscriptionLifecycleRepositoryTest.paid_coverage_gap_still_uses_latest_confirmed_end_for_default_stop](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L226)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|5|全部付款到期默认今天|[SubscriptionLifecycleRepositoryTest.all_expired_payments_default_to_business_today_without_backdating](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L236)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|6|计划后新晚期付款不延长停止|[SubscriptionLifecycleRepositoryTest.ordinary_new_renewal_cannot_cross_fixed_future_stop_boundary](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L325)<br>[SubscriptionLifecycleRepositoryTest.correction_expanding_coverage_after_fixed_stop_requires_explicit_plan_change](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L443)<br>[SubscriptionLifecycleRepositoryTest.amount_only_correction_keeps_existing_coverage_truncated_by_early_plan](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L433)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|7|STOPPED保留历史付款|[SubscriptionLifecycleRepositoryTest.default_stop_is_fixed_at_latest_prepaid_boundary_and_keeps_history](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L172)<br>[SubscriptionLifecycleRepositoryTest.early_stop_rejects_past_dates_and_preserves_purchased_totals_when_valid](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L246)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|8|无覆盖重开不造假付款/ACTIVE|[SubscriptionLifecycleRepositoryTest.reopen_without_current_or_future_payment_requires_real_confirmation](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L286)<br>[SubscriptionLifecycleRepositoryTest.atomic_reopen_with_real_renewal_keeps_subscription_id_and_supports_exact_replay](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L295)<br>[SubscriptionLifecycleRepositoryTest.real_sqlite_state_failure_rolls_back_atomic_reopen_payment_and_state](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L314)<br>[M09SubscriptionLifecycleViewModelTest.uncovered_reopen_requires_explicit_amount_and_paid_date_without_prefill](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleViewModelTest.kt#L71)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|9|金额修正原事件不追加|[SubscriptionLifecycleRepositoryTest.renewal_amount_correction_updates_original_fact_and_exact_target_replays](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L378)<br>[M09SubscriptionLifecycleViewModelTest.payment_correction_updates_original_fact_without_appending_or_changing_operation](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleViewModelTest.kt#L177)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|10|续费重叠纠错整体拒绝|[SubscriptionLifecycleRepositoryTest.renewal_overlap_correction_is_rejected_before_any_partial_update](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L395)<br>[SubscriptionLifecycleRepositoryTest.renewal_correction_validates_positive_amount_cycle_dates_and_preserved_identities](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L403)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|11|删除中途故障全部回滚|[SubscriptionLifecycleRepositoryTest.real_sqlite_after_delete_failure_rolls_back_the_entire_delete](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L530)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|12|最后一笔付款安全拒绝|[SubscriptionLifecycleRepositoryTest.deleting_last_payment_is_rejected_without_deleting_subscription_identity](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L514)<br>[M09SubscriptionLifecycleViewModelTest.last_payment_delete_is_locked_and_explained_without_destroying_identity](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleViewModelTest.kt#L202)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|13|取消状态确认零写入|[M09SubscriptionLifecycleFlowTest.stop_reason_does_not_cancel_vendor](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleFlowTest.kt#L151)<br>[M09SubscriptionNavigationFlowTest.formal_current_detail_opens_pause_confirmation_without_a_database_write](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionNavigationFlowTest.kt#L47)<br>[M09SubscriptionViewModelTest.restored_dirty_state_requires_explicit_discard_without_writing_facts](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionViewModelTest.kt#L78)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|14|相同状态并发仅一事实|[SubscriptionLifecycleRepositoryTest.concurrent_identical_pause_commands_save_only_one_fact](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L125)<br>[M09SubscriptionLifecycleViewModelTest.inflight_state_locks_duplicate_save_and_preserves_reason_and_target](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleViewModelTest.kt#L277)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|15|异内容复用operationId冲突|[SubscriptionLifecycleRepositoryTest.reused_state_operation_with_different_contents_conflicts](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L116)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|16|陈旧状态/付款基线不覆盖|[SubscriptionLifecycleRepositoryTest.changed_payment_invalidates_whole_state_baseline](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L135)<br>[SubscriptionLifecycleRepositoryTest.later_state_fact_invalidates_whole_state_baseline](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L152)<br>[SubscriptionLifecycleRepositoryTest.stale_payment_correction_cannot_overwrite_later_state_or_payment_fact](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L423)<br>[SubscriptionLifecycleRepositoryTest.stale_delete_cannot_remove_corrected_payment](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L521)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|17|记录时区不随设备时区变|[BusinessClockTest.recorded_zone_uses_its_own_calendar_day](../../app/src/test/java/com/cooper/lithic/core/time/BusinessClockTest.kt#L23)<br>[SettingsStoreTest.zone_is_persisted](../../app/src/androidTest/java/com/cooper/lithic/data/settings/SettingsStoreTest.kt#L48)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|18|边界/历史/有效天数一致|[M09SubscriptionLifecycleFlowTest.expiry_needs_no_worker](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleFlowTest.kt#L53)<br>[SubscriptionLifecycleRepositoryTest.resume_with_current_coverage_keeps_old_payment_and_counts_only_enabled_dates](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L260)<br>[SubscriptionLifecycleRepositoryTest.management_and_stopped_queries_use_observation_day_last_boundary](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L539)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|19|读取失败不伪装空列表|[SubscriptionLifecycleRepositoryTest.closed_database_management_read_and_lifecycle_write_have_explicit_failures](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L691)<br>[SubscriptionLifecycleRepositoryTest.corrupt_facts_outside_visible_management_page_are_not_hidden_by_filter_or_limit](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L587)<br>[SubscriptionLifecycleRepositoryTest.full_corruption_preflight_uses_bounded_identity_batches_and_finds_bad_tail](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionLifecycleRepositoryTest.kt#L597)<br>[M09SubscriptionLifecycleViewModelTest.committed_later_facts_require_explicit_current_read_and_safe_conflict_exit](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleViewModelTest.kt#L296)|PASS：最终XML对应方法均通过；手工子路径范围以设备QA/快照为准。|
|20|360dp/200%/真实IME可达||PASS：专用API36实际小屏200%/IME/错误/旋转，见设备QA及截图。|
|21|Nav3恢复/返回/pending不重复写|[M09SubscriptionViewModelTest.restored_inflight_state_keeps_exact_envelope_and_blocks_departure](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionViewModelTest.kt#L57)<br>[M09SubscriptionLifecycleViewModelTest.lost_state_result_restores_same_immutable_command_and_replays_without_duplicates](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleViewModelTest.kt#L213)<br>[M09SubscriptionNavigationGuardTest.hidden_pending_state_blocks_root_system_back_without_finishing_host](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionNavigationGuardTest.kt#L65)<br>[M09SubscriptionLifecycleViewModelTest.slow_committed_read_cannot_redirect_a_later_opened_detail](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleViewModelTest.kt#L326)<br>[M09SubscriptionLifecycleViewModelTest.slow_resolved_conflict_read_cannot_redirect_a_later_opened_detail](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleViewModelTest.kt#L354)|PASS：最终XML对应方法均通过；实际手势及系统已保存同任务恢复另留证，真实LMK NOT_RUN。|
|22|旧项目生命周期与M08续费回归|[ProjectEntryFlowTest.create_zero_and_normal_project_are_real_room_records_after_reopen](../../app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectEntryFlowTest.kt#L98)<br>[SubscriptionScreensTest.renewal_requires_explicit_payment_action_and_keeps_reference_readonly](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionScreensTest.kt#L84)<br>[SubscriptionRenewalFlowTest.renewal_history_is_preserved](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionRenewalFlowTest.kt#L223)|PASS：最终549包含旧455完整身份，0失败/错误/跳过；不以限定重跑拼接。|


| 实际阶段（名称不等于结果） | 退出码 | 秒 | 真实结果 |
|---|---|---|---|
| [green-flow-and-committed-read](m09-tests/green-flow-and-committed-read-summary.json) | 1 | 110.058 | 0 JVM＋9 Android：8 PASS/1 FAIL/0 SKIP |
| [green-full-final](m09-tests/green-full-final-summary.json) | 1 | 688.838 | 0 JVM＋463 Android：462 PASS/1 FAIL/0 SKIP |
| [green-full-recheck](m09-tests/green-full-recheck-summary.json) | 1 | 1456.276 | 86 JVM＋463 Android：547 PASS/2 FAIL/0 SKIP |
| [green-full-verified](m09-tests/green-full-verified-summary.json) | 0 | 805.631 | 86 JVM＋463 Android：549 PASS/0 FAIL/0 SKIP |
| [green-legacy-detail-and-confirm](m09-tests/green-legacy-detail-and-confirm-summary.json) | 0 | 161.677 | 0 JVM＋2 Android：2 PASS/0 FAIL/0 SKIP |
| [green-three-read-risks](m09-tests/green-three-read-risks-summary.json) | 0 | 119.356 | 0 JVM＋3 Android：3 PASS/0 FAIL/0 SKIP |
| [red-committed-read-and-target-validation](m09-tests/red-committed-read-and-target-validation-summary.json) | 1 | 283.066 | 0 JVM＋143 Android：140 PASS/3 FAIL/0 SKIP |
| [red-lifecycle-foundation](m09-tests/red-lifecycle-foundation-summary.json) | 1 | 175.438 | 0 JVM＋63 Android：9 PASS/54 FAIL/0 SKIP |
| [red-lifecycle-save](m09-tests/red-lifecycle-save-summary.json) | 1 | 153.367 | 0 JVM＋58 Android：42 PASS/16 FAIL/0 SKIP |
| [red-preflight-and-stop-plan](m09-tests/red-preflight-and-stop-plan-summary.json) | 1 | 77.273 | 0 JVM＋2 Android：0 PASS/2 FAIL/0 SKIP |
| [red-resolved-conflict-and-sync-check](m09-tests/red-resolved-conflict-and-sync-check-summary.json) | 1 | 43.241 | 0 JVM＋2 Android：1 PASS/1 FAIL/0 SKIP |
| [regression-yearly-cycle](m09-tests/regression-yearly-cycle-summary.json) | 0 | 111.595 | 0 JVM＋1 Android：1 PASS/0 FAIL/0 SKIP |

63/58/预检停止/迟到读取等真实行为RED保留；143阶段包含2业务RED＋1JUnit void初始化错误，初始化错误不算有效业务方法。七Flow修为runBlocking<Unit>而不降断言。green-flow一例同步等待修正保留原断言/超时。首次全量实际仅463 Android：462PASS/1旧Yearly FAIL，JVM未实际运行、Lint/assemble因中断未完成；同源码单项PASS不证明确定生产根因，IME布局触摸假设仅用最小驱动强化验证。第二全量549＝547PASS/2旧FAIL，旧项目等待误中上一详情、付款按钮时序各限定复验PASS；最终重新全量0失败，未拼接或删除失败证据。

真实SQLite trigger分别验证原子重开中付款已插入后状态写入失败、付款删除后失败的完整事务回滚；续费纠错的重叠/非法日期/CAS整体拒绝另有测试，不冒称另做了纠错SQL触发器中断。相同命令并发一次、异内容operationId冲突、陈旧全事实CAS、result loss固定信封、最后付款拒绝/隐藏坏行/读取错误不假空都有正式方法及XML。

提交格式检查覆盖源码、当前文档和结构化JSON。三个已校验的原始输出保留系统CRLF/空白字节（安装、冷启动及一份历史失败日志），未为消除格式提示改写原始证据；实际暂存的128份构建输入Git blob与已测试输入一致。

## 10. API36真实操作、离线、跨日与恢复

仅emulator-5596/Lithic_M01_API_36/Android16 x86_64，最终APK实际覆盖安装Success。1/25～3/2切guest时钟，未改宿主或其他设备；完整暂停→恢复→无Worker到期→默认计划→修正/取消→提前停止→历史→原ID重开→显式真实续费→纠错/删除→断网冷启已操作。初始真实断网airplane1/wifi0/mobile0/defaultnetwork none复核后COLD启动成功；最终离线日期恢复仍EXPIRED并保留事实。

系统时区Pacific/Kiritimati设备日期1/26，持久recordZone GMT业务观察/PAUSED日期仍1/25，settings字节不变。360dp/200%真实IME、bad-date/M09_RAW_DRAFT错误及按钮可达、系统Back先收键盘、横竖屏raw保持、真实gestural overlay边缘返回显式保留/弃改守卫、系统已保存同任务SIGKILL/Recents恢复新PID/raw均留证。具体task/PID/overlay/截图与设置恢复见[设备QA](m09-device/device-qa-report.md)及JSON。

任务必须先STOPPED/HaveState/Bundle再UID SIGKILL并恢复同Recent卡；不叫真实LMK或force-stop恢复。临时global/system/secure命名空间、WM/font/旋转/IME/时区/自动时间/网络/night/gesture额外设置全部恢复，实际guestUTC与宿主少于10秒。驱动早期网络服务过渡、误标签、初始读取早断言、旧CtrlA追加日期/裁切文字命中及后续惯性滚动漏字段诊断保留，未误提交，也不据此改生产。

## 11. Room v2、generation、时区及依赖/权限

schema v2七表/M08显式Migration1→2、v1/v2导出、实体/索引/预检、owner/记录时区和冻结引擎保持。所有只读阶段旧七表原行逐字段相同，integrity_check=ok、foreign_key_check为空；generation `2d6c9460-a2f3-4976-ba04-781c416a0b52`、pointer/ready、settings SHA `fff800a6a09afbc2e6656c8d67b512b74ba8e1693666e32b96701c7dfcf06c44`一致。只公开M09合成事实/计数/哈希/截图，raw数据库/WAL/SHM/preferences/真实原行留本地ignored。

运行依赖122→122无增删升级；Gradle8.14.3/AGP8.13.2/Kotlin2.3.10/ComposeBOM2025.11.01/Nav3 1.0.1/Room2.8.5/KSP2.3.6/DataStore1.2.1/Coroutines1.10.2和JDK17.0.20.1+1不变，TOOLCHAIN_LOCK不改。最终Manifest/APK无INTERNET/其他禁止权限，仅内部signature接收权限；allowBackup=false/九域排除，v2Debug签名及旧证书一致，79指定生产类存在、JUnit/androidxTest/合成测试类不打包。Release未构建，Debug空ComponentActivity宿主继承，不把静态排除承诺泛化OEM绝对行为。

## 12. 最终APK、文件与Git

- Debug code7 /0.1.0-m09-debug，**12,521,904 bytes**。
- 实际构建APK：`E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`；本地交付副本：`E:/CODEX/砺石新APP/build/m09-evidence/app-m09-debug.apk`。
- SHA-256：`becd15b3bbf5de9425419504bde58837d0d6fa6e1cacc32d995f8dc11ec437f2`，最终构建、真实GUI旅程、最终已安装字节相同。
- v2Debug证书SHA-256：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，旧证书兼容，非生产签名；APK/密钥未公开/入Git。
- 第一次code6→7发生于此前instrumentation部署，最终手工安装7→7覆盖，不伪称最终APK第一次升级或新迁移。
- Git只feat/m09-subscription-lifecycle，基线0063ca5；main及前模块引用保护、原43/24/130及16跨模块边界/旧证据字节保持。最终自身提交SHA、实际推送及clean/远端一致以Git日志/交付回执为准，避免递归自引用；不合并main/force/自动PR/公开APK。所有新增/修改/删除见[M09-files](M09-files.md)。

## 13. Known Bugs、NOT_RUN、规格与M10前置

最终验证未留下已确定的阻塞性计算或事务缺陷；旧非稳定UI失败和驱动假设/强化/最终回归证据完整保留，不声称确定了所有偶发根因。CurrentAbsence仅当前缺失。全量坏数据预检按50身份批次但O(total facts)，单服务完整历史加载；分页保护不等于长期性能基准。SQLite满页可能分类DATABASE，不能牺牲输入/原子性/安全重试。SavedState仅已保存任务，不保证系统未保存、force-stop、断电或真实LMK。

真机、API26、OEM、Release/生产签名、完整TalkBack及真实LMK仍NOT_RUN。完整备份恢复M11/M12及Q06持久切换/故障回退未做；Debug仍不存无法丢失的个人记录。Q03长期节点M21、Q04提醒资格M20、Q05图片往返M10/M12、Q06M12按原期限；Q02M17/M18集成/16完整跨模块补测和原130案例NOT_RUN不改。两项M09流程风险仅按用户A/B和本轮实测关闭，不扩大到提醒/媒体/备份/汇总。

继承M04残留generation不得静默清除真实数据、M08以后schema升级维护已知版本/Room身份预检、M07Absent仅当前缺失/多END_SETTLEMENT冲突/共享媒体归M10、所有历次独立审查未重跑本机/安装/部分APK与超大JSON等NOTES。辅助代理源码核对不代替ChatGPT独立验收。

M10前置：M09独立验收通过及用户另行授权，阅读原M10和当前事实、明确Q05初次图片规范化与恢复原字节往返规则，继承Room v2/owner/recordZone、共享引用/原子写及M11/M12安全边界。当前立即停止，不自动M10。

**READY_FOR_REVIEW — M09**
