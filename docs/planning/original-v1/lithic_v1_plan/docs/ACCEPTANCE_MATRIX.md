# 验收矩阵

**以下全部为计划测试，当前状态NOT_RUN。** 不存在“已通过这些测试”的含义。

## 验收层次
纯JVM：金额、日期、成本、状态、趋势、排行和年度计算。
Room/Instrumented：事务、FK、唯一键、迁移、重启持久化、完整恢复故障注入。
Compose UI：录入、修改、返回、确认、列表、图表读屏、小屏和字体放大。
真机：断网、通知权限、重启/省电/强制停止、系统文件选择器和签名覆盖升级。

建议合成压力集：2000个项目/服务、20000笔金额事件、10000笔续费、20年跨度、200张受控封面。是测试目标，不是已测性能结果。M22记录设备、构建类型、冷热状态、P50/P95、峰值内存和是否ANR/OOM；不编造精确性能承诺。

## 按模块逐项验收

| 编号 | 模块 | 案例 | 断言 | 当前状态 |
|---|---|---|---|---|
| M00-T01 | M00 | `scope_does_not_include_legacy_finance` | 明确禁止旧账户/股票/量化/云同步，不复制WHOCARE工程。 | NOT_RUN |
| M00-T02 | M00 | `all_modules_start_planned` | M01–M23均为PLANNED，没有伪造APK和测试通过记录。 | NOT_RUN |
| M00-T03 | M00 | `defaults_are_distinguished` | D01–D15标本轮补充，不冒称每条用户已单独确认。 | NOT_RUN |
| M01-T01 | M01 | `empty_app_cold_starts_offline` | 关闭网络能打开空壳，尚无伪造业务数据。 | NOT_RUN |
| M01-T02 | M01 | `merged_manifest_has_no_internet` | 构建后的合并Manifest没有INTERNET、精确闹钟、广泛存储权限。 | NOT_RUN |
| M01-T03 | M01 | `debug_id_is_isolated` | debug的applicationId为com.cooper.lithic.debug，正式为com.cooper.lithic。 | NOT_RUN |
| M01-T04 | M01 | `versions_are_locked` | 稳定工具链精确版本、来源、JDK和Gradle锁定；assembleDebug实际退出0。 | NOT_RUN |
| M02-T01 | M02 | `same_day_is_one_day` | 2026-01-01开始，当天初始1000元，持有1天，日均1000元。 | NOT_RUN |
| M02-T02 | M02 | `dated_event_does_not_rewrite_earlier_days` | 1月11日新增200元，1月10日仍1000/10=100元，1月11日1200/11≈109.09元。 | NOT_RUN |
| M02-T03 | M02 | `end_day_removes_current_contribution` | 1月10日结束，持有10天，日末总成本贡献0。 | NOT_RUN |
| M02-T04 | M02 | `post_end_refund_keeps_duration` | 结束后1月20日收回200元，最终800/10=80元，持有仍10天。 | NOT_RUN |
| M02-T05 | M02 | `money_precision_and_zero` | 0.10+0.20=0.30；0元允许；超过两位小数及溢出拒绝。 | NOT_RUN |
| M02-T06 | M02 | `negative_cost_is_not_clamped` | 累计5000收回6000，净成本-1000，不钳成0。 | NOT_RUN |
| M03-T01 | M03 | `monthly_standard_rate` | 月付150元得5元/天，即使真实区间31天。 | NOT_RUN |
| M03-T02 | M03 | `annual_purchase_not_elapsed_year` | 刚预付3650元年费：折算365天、平均10元；生效当天实际已过去仅1天，无一年里程碑。 | NOT_RUN |
| M03-T03 | M03 | `prepaid_next_rate_does_not_start_early` | 1月20日付180元、2月1日生效；1月20日仍150/30=5，2月1日变6。 | NOT_RUN |
| M03-T04 | M03 | `expired_has_zero_current_rate` | [1月1日,2月1日)到2月1日无续费时当前贡献0。 | NOT_RUN |
| M03-T05 | M03 | `pause_does_not_erase_paid_history` | 暂停不改已付金额和已购标准天数，只截断当前有效贡献/实际经过时间。 | NOT_RUN |
| M03-T06 | M03 | `overlaps_rejected` | 同服务重叠区间拒绝，相邻区间允许；付费日未来拒绝。 | NOT_RUN |
| M04-T01 | M04 | `fk_rejects_orphan_event` | 金额事件必须引用现存项目。 | NOT_RUN |
| M04-T02 | M04 | `operation_id_is_unique` | 同operationId重试不产生第二笔事件。 | NOT_RUN |
| M04-T03 | M04 | `transaction_rolls_back` | 项目与初始投入任一步失败不留半条项目。 | NOT_RUN |
| M04-T04 | M04 | `zone_is_persisted` | 重启与设备改时区不改记录时区。 | NOT_RUN |
| M04-T05 | M04 | `generation_is_self_contained` | 库、图片目录、设置路径均在一个数据集下，首次指针指向完整初始集。 | NOT_RUN |
| M04-T06 | M04 | `defaults_are_typed` | 两套其他类别独立，跨类型归类在事务内拒绝。 | NOT_RUN |
| M05-T01 | M05 | `only_three_primary_destinations` | 底部仅现在/回看/设置。 | NOT_RUN |
| M05-T02 | M05 | `empty_screen_has_no_fake_metrics` | 空页面显示引导而非演示趋势或伪造金额。 | NOT_RUN |
| M05-T03 | M05 | `large_text_has_no_blocked_action` | 360dp、200%字体仍可访问保存/返回。 | NOT_RUN |
| M05-T04 | M05 | `back_stack_restores` | 返回、旋转和进程重建保持可恢复的合法路由，不丢入不可达详情。 | NOT_RUN |
| M06-T01 | M06 | `create_zero_and_normal_project` | 0元及普通项目都能保存并重启读取。 | NOT_RUN |
| M06-T02 | M06 | `initial_cost_counted_once` | 初始1000元只计1000，不从主表和事件各加一遍。 | NOT_RUN |
| M06-T03 | M06 | `double_tap_no_duplicate` | 快速双击保存同操作只产生一个项目/金额事件。 | NOT_RUN |
| M06-T04 | M06 | `failed_save_preserves_form` | 写入失败留住输入，返回放弃有确认。 | NOT_RUN |
| M06-T05 | M06 | `backdated_money_recalculates` | 补录真实历史金额后详情与列表同时刷新。 | NOT_RUN |
| M07-T01 | M07 | `end_and_recovery_atomic` | 结束和卖出回收同事务，任一失败全回滚。 | NOT_RUN |
| M07-T02 | M07 | `edit_end_recovery_not_duplicated` | 多次改卖出金额只更新同一收回记录。 | NOT_RUN |
| M07-T03 | M07 | `late_installment_does_not_reactivate` | 结束后分期付款增加净投入但不增加天数、不回当前。 | NOT_RUN |
| M07-T04 | M07 | `correct_end_date_updates_result` | 修改合法结束日重算最终值；早于开始拒绝。 | NOT_RUN |
| M07-T05 | M07 | `delete_requires_confirmation` | 取消不删；确认后项目和事件删净且无孤立封面引用。 | NOT_RUN |
| M08-T01 | M08 | `v1_to_v2_preserves_projects` | 旧项目/事件/图片元数据逐条保持。 | NOT_RUN |
| M08-T02 | M08 | `create_and_first_payment_atomic` | 身份+首次付款同事务，不留空付款。 | NOT_RUN |
| M08-T03 | M08 | `renewal_needs_confirmation` | 只打开表单/到达日期不生成付款。 | NOT_RUN |
| M08-T04 | M08 | `renewal_history_is_preserved` | 150月付→180月付→年付旧金额与周期均保留。 | NOT_RUN |
| M08-T05 | M08 | `accurate_dates_retained` | 付款、生效、付费到期和提醒到期各自持久化。 | NOT_RUN |
| M08-T06 | M08 | `currency_change_is_allowed` | 同固定月周期146.83元→148.10元可记录，不误认按量服务。 | NOT_RUN |
| M09-T01 | M09 | `expiry_needs_no_worker` | 冻结后台任务，跨到期日打开仍EXPIRED且贡献0。 | NOT_RUN |
| M09-T02 | M09 | `cancel_renewal_keeps_paid_access` | 设期末停止时，在付费有效期内仍计成本，期末后停止。 | NOT_RUN |
| M09-T03 | M09 | `pause_is_not_history` | 暂停放当前管理暂停组，停止才进历史。 | NOT_RUN |
| M09-T04 | M09 | `resume_without_new_payment` | 有效覆盖内恢复不造付款，覆盖外要求真实续费。 | NOT_RUN |
| M09-T05 | M09 | `future_prepaid_has_own_label` | 未来付费期显示未生效，最近续费排序按paidOn。 | NOT_RUN |
| M09-T06 | M09 | `stop_reason_does_not_cancel_vendor` | 停止确认明确只改变本App记录。 | NOT_RUN |
| M10-T01 | M10 | `copy_survives_source_removal` | 导入后删除相册原图不影响App封面。 | NOT_RUN |
| M10-T02 | M10 | `no_broad_storage_permission` | 不申请广泛存储/相册访问，不依赖远程图片。 | NOT_RUN |
| M10-T03 | M10 | `replacement_failure_keeps_old_cover` | 压缩/写入失败保留旧图与引用。 | NOT_RUN |
| M10-T04 | M10 | `bounded_decode` | 过大尺寸、损坏、非图片输入不OOM并明确失败。 | NOT_RUN |
| M10-T05 | M10 | `private_relative_paths` | 数据库不保存跨设备绝对路径或临时content URI。 | NOT_RUN |
| M11-T01 | M11 | `snapshot_contains_every_v2_entity` | 所有业务实体、图片、设置逐项覆盖，不能漏状态历史。 | NOT_RUN |
| M11-T02 | M11 | `concurrent_write_snapshot_is_consistent` | 导出同时发起修改，结果只能属于单个一致版本。 | NOT_RUN |
| M11-T03 | M11 | `hashes_match` | manifest条目字节数与SHA-256匹配实际输出。 | NOT_RUN |
| M11-T04 | M11 | `export_failure_is_not_success` | 目标写入/关闭失败不显示成功。 | NOT_RUN |
| M11-T05 | M11 | `plain_manual_only` | 没有密码、自动备份计划、提醒或CSV出口。 | NOT_RUN |
| M12-T01 | M12 | `full_round_trip` | 数据、图片哈希、类别、备注、续费、状态、设置完整往返。 | NOT_RUN |
| M12-T02 | M12 | `invalid_zip_preserves_current` | 路径穿越、损坏、缺图、新格式拒绝，旧数据不变。 | NOT_RUN |
| M12-T03 | M12 | `staging_failure_preserves_current` | 解压/建新库/设置写入/空间不足时旧集可读。 | NOT_RUN |
| M12-T04 | M12 | `atomic_switch_kill_points` | 切换前后杀进程，下次只打开完整旧集或完整新集。 | NOT_RUN |
| M12-T05 | M12 | `restore_date_is_today` | 导入后用今天重算天数和过期状态，不照抄旧缓存。 | NOT_RUN |
| M12-T06 | M12 | `no_partial_or_merge_restore` | 不静默丢字段/图片，不合并重复数据。 | NOT_RUN |
| M13-T01 | M13 | `home_uses_same_engines` | 首页各项取M02/M03结果，不独立复制公式。 | NOT_RUN |
| M13-T02 | M13 | `active_only_aggregate` | 结束、暂停、过期不进入；未来套餐不提前进入。 | NOT_RUN |
| M13-T03 | M13 | `category_sum_matches_type` | 未舍入类别和等于该类型汇总；展示分位差有说明。 | NOT_RUN |
| M13-T04 | M13 | `no_zero_confused_with_empty` | 无记录与存在0元项目区分。 | NOT_RUN |
| M13-T05 | M13 | `cross_day_refresh` | 前台跨日、返回前台、重启，成本按记录日期更新。 | NOT_RUN |
| M13-T06 | M13 | `backup_before_real_use` | 完整核心流程通过，并安装稳定签名内部版作首次真实录入基线。 | NOT_RUN |
| M14-T01 | M14 | `merge_is_atomic` | 同类型类别合并后全部项目迁移，汇总不变、无悬挂引用。 | NOT_RUN |
| M14-T02 | M14 | `cross_type_merge_rejected` | 长期项目类别不得并入订阅类别。 | NOT_RUN |
| M14-T03 | M14 | `search_current_and_history` | 名称匹配同时返回当前/结束/暂停/停止，状态明确。 | NOT_RUN |
| M14-T04 | M14 | `wildcards_are_literal` | 输入%/_/引号不会扩大查询或破坏SQL。 | NOT_RUN |
| M14-T05 | M14 | `default_sorts` | 项目持有最长优先；订阅paidOn最近优先；分页不重复漏行。 | NOT_RUN |
| M15-T01 | M15 | `review_includes_still_active` | 用了8年仍ACTIVE的项目能进入陪伴榜。 | NOT_RUN |
| M15-T02 | M15 | `history_excludes_paused` | 历史只已结束/已停止，不混暂停。 | NOT_RUN |
| M15-T03 | M15 | `types_rank_separately` | 项目和订阅不跨类型比“谁更值得”。 | NOT_RUN |
| M15-T04 | M15 | `current_final_labels` | 混合榜逐行标当前或截至目前最终。 | NOT_RUN |
| M15-T05 | M15 | `zero_negative_are_honest` | 0与负成本不剔除，也不称为价值评分。 | NOT_RUN |
| M15-T06 | M15 | `subscription_time_label` | 订阅时间榜明确折算付费时长，预付不伪装已陪伴。 | NOT_RUN |
| M16-T01 | M16 | `one_predecessor_successor_no_cycles` | A→B→C合法，自指、B→A成环、一对多拒绝。 | NOT_RUN |
| M16-T02 | M16 | `link_does_not_end_old` | 只建替换关系不偷偷结束旧项目。 | NOT_RUN |
| M16-T03 | M16 | `trade_in_balance` | 旧回收3000、新现金5000+折抵3000，新成本8000，整体净新增5000。 | NOT_RUN |
| M16-T04 | M16 | `trade_in_all_or_nothing` | 旧结束、新项目、3笔金额、替换关系任一步失败全回滚。 | NOT_RUN |
| M16-T05 | M16 | `delete_middle_does_not_guess` | 删中间项目只断相关边，不自动A→C。 | NOT_RUN |
| M16-T06 | M16 | `v1_backup_upgrade` | 旧备份默认CASH/无关系；新v2备份全量往返不丢折抵。 | NOT_RUN |
| M17-T01 | M17 | `past_event_boundary` | 后加维修不改变维修前日均；退款后按负事件变化。 | NOT_RUN |
| M17-T02 | M17 | `subscription_is_step_not_smooth` | 价格变更按生效日阶梯，暂停空档不连成持续订阅。 | NOT_RUN |
| M17-T03 | M17 | `events_are_accessible` | 同日多个事件点可打开完整列表，屏幕阅读器可读。 | NOT_RUN |
| M17-T04 | M17 | `negative_values_visible` | 负日均有真实负区间，不裁到0。 | NOT_RUN |
| M17-T05 | M17 | `no_forecast` | 曲线截至今天/真实结束后结算节点，不外推未来。 | NOT_RUN |
| M18-T01 | M18 | `every_point_matches_reference` | 随机观察日总线=该日各引擎有效贡献合计。 | NOT_RUN |
| M18-T02 | M18 | `closed_or_expired_removed` | 项目结束日、订阅到期/暂停按契约退出总线。 | NOT_RUN |
| M18-T03 | M18 | `today_equals_home` | 最后观察日未舍入值与首页完全同口径。 | NOT_RUN |
| M18-T04 | M18 | `sampling_keeps_spikes` | 20年历史中新增当天大峰和事件邻点不被抽样丢掉。 | NOT_RUN |
| M18-T05 | M18 | `multi_event_day_not_false_causality` | 同日多事件统一列该日相关事件，不说唯一归因。 | NOT_RUN |
| M19-T01 | M19 | `money_year_is_occurred_year` | 2026买8000，2028修800，各入真实发生年。 | NOT_RUN |
| M19-T02 | M19 | `outlay_recovery_net` | 投入13000收回3500，分别展示且净投入9500。 | NOT_RUN |
| M19-T03 | M19 | `subscription_year_is_paid_year` | 跨年年付全部入付款年，不伪造每月续费。 | NOT_RUN |
| M19-T04 | M19 | `types_not_merged_into_cash` | 项目与订阅分开；折抵年标含非现金折抵。 | NOT_RUN |
| M19-T05 | M19 | `category_changes_restate_history` | 改类别/合并后按当前分类重算，标示口径。 | NOT_RUN |
| M19-T06 | M19 | `annual_summary_no_judgement` | 不产出平均寿命、主观总结、消费建议或数量目标。 | NOT_RUN |
| M20-T01 | M20 | `three_days_before_once` | 10月20日到期，候选10月17日，稳定逻辑键最多正常提醒一次。 | NOT_RUN |
| M20-T02 | M20 | `renewed_before_send_suppresses` | 发送前已续费/停止则旧候选不发。 | NOT_RUN |
| M20-T03 | M20 | `late_delivery_updates_copy` | 目标日错过仍未到期，用真实剩余天数；过期不发3天后。 | NOT_RUN |
| M20-T04 | M20 | `permission_denial_nonblocking` | 拒绝权限仍可新增/续费/查记录，设置如实标状态。 | NOT_RUN |
| M20-T05 | M20 | `no_exact_alarm_or_network` | 合并Manifest无精确闹钟及联网权限，任务不造付款。 | NOT_RUN |
| M20-T06 | M20 | `backup_and_migration` | schema v4、format v3含必要回执；旧备份导入不追发历史。 | NOT_RUN |
| M21-T01 | M21 | `annual_prepaid_not_milestone` | 年费刚付不可触发365天陪伴，实际经过达到阈值才触发。 | NOT_RUN |
| M21-T02 | M21 | `old_import_does_not_spam` | 录入10年前项目不一次推送所有旧里程碑。 | NOT_RUN |
| M21-T03 | M21 | `pause_free_gap_excluded` | 订阅暂停/免费/无覆盖日期不计实际有效时长。 | NOT_RUN |
| M21-T04 | M21 | `same_day_coalesced` | 同日多条合为低打扰通知，来源仍可追溯。 | NOT_RUN |
| M21-T05 | M21 | `backup_watermark_survives` | 恢复、改历史、系统重启不重发已越过的旧节点。 | NOT_RUN |
| M21-T06 | M21 | `switch_cancels_future` | 关闭里程碑只停该类，续费开关互不影响。 | NOT_RUN |
| M22-T01 | M22 | `narrow_largefont_touch` | 360dp/200%字体/48dp触控关键操作可达。 | NOT_RUN |
| M22-T02 | M22 | `no_fake_or_missing_states` | 空、忙、失败、已到期、暂停、结束、负成本、亚分成本均明确。 | NOT_RUN |
| M22-T03 | M22 | `twenty_year_synthetic_dataset` | 合成2000项目/服务、20000金额事件、10000续费、20年跨度，无OOM/ANR，分页稳定。 | NOT_RUN |
| M22-T04 | M22 | `chart_interaction_keeps_truth` | 拖动与读屏读到原始准确点，显示抽样不改数据。 | NOT_RUN |
| M22-T05 | M22 | `identity_assets_are_honest` | 实际图标资产接入情况与截图对应，不把临时标记冒称获批Logo。 | NOT_RUN |
| M22-T06 | M22 | `runtime_offline_journey` | 断网完整跑创建→补录→结束→续费→查图→备份→恢复。 | NOT_RUN |
| M23-T01 | M23 | `all_schema_paths_migrate` | 已发行schema到当前版本全覆盖；禁止破坏性回退。 | NOT_RUN |
| M23-T02 | M23 | `all_backup_formats_restore` | 格式v1/v2/v3合成样本均正确恢复，更新未知格式安全拒绝。 | NOT_RUN |
| M23-T03 | M23 | `signed_update_preserves_data` | 稳定applicationId与签名覆盖安装保留记录/图片/设置。 | NOT_RUN |
| M23-T04 | M23 | `wrong_key_or_downgrade_not_silent` | 错误签名不建议卸载解决；旧APK不打开新schema。 | NOT_RUN |
| M23-T05 | M23 | `fault_matrix_passed_or_blocked` | 空间不足、损坏备份、杀进程、拒权限、日期边界均有真实报告；关键NOT_RUN阻止冻结。 | NOT_RUN |
| M23-T06 | M23 | `deliverable_is_verifiable` | APK路径存在，记录签名摘要/文件SHA-256/版本/commit；所有数值来自实际命令。 | NOT_RUN |

## 发布阻断
任何成本数学错误、重复真实付款、历史边界错误、失败恢复破坏现有数据、旧备份不能迁移、升级丢图丢记录、核心路径必须联网，都阻止冻结。
真机通知因系统限制延迟可作为已验证限制说明，但不得把未运行的权限/去重/恢复测试写成PASS WITH NOTES。
视觉非阻断差异必须记录具体页面、复现条件和后续修复边界，不能用“优化体验”掩盖不可操作的按钮。
