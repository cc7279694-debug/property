# M09 实际设备 QA 报告

结果：PASS（专用 API36/code7 debug APK 范围）；实现者交付由root汇总；ChatGPT独立验收仍PENDING。
设备 emulator-5596 / Lithic_M01_API_36；APK SHA256 `becd15b3bbf5de9425419504bde58837d0d6fa6e1cacc32d995f8dc11ec437f2`。

只操作新合成服务 M09_SUBSCRIPTION。未修改生产源码、正式文档、Gradle 或 Git。全部证据仅写 ignored build/m09-evidence。

## 最终真实结果

- 累计实际付款375.00元；标准付费时长60天；实际已过去有效60天。
- 当前已到期，当前套餐为空、贡献0元/天；历史平均6.25元/天。
- 总计3订阅/5付款/5状态；原2订阅3付款、3项目6事件逐行完全保留。
- 无覆盖重新开通的金额及付款日期真实为空。随后用户路径显式填写240元、Mar2付款/生效、Apr2到期边界、Apr1提醒；付款与ENABLED共享原子操作ID。
- 新付款ID `86a66dd0-68a4-4d61-9943-d9616257acd9`；启用状态ID `c2201138-d214-41b2-9af5-07e7146ac4cd`；共同opID `33924c0d-ae89-4fa7-81c7-4ddd90c340b2`。
- 该付款240→225保留原ID/opID；仅删除新服务Jan20/180错误付款，已真实看到日期/金额/不可撤回/不是退款提示并确认。

## 实际逐项验证

| 旅程 | 结果 | 观察及证据 |
|---|---|---|
| apk_and_old_data | PASS | code7 APK SHA exact; Room2 integrity/FK, complete old rows, active generation/ready/preferences preserved；ledger-installed.json, ledger-before.json, ledger-final-restored.json |
| first_actual_payment | PASS | Same new service: actual150, Jan1-Feb1；ledger-created.json, m09-created-detail.png, m09-created-detail.xml |
| prepaid_future_coverage | PASS | Actual180 paidJan20, coverageFeb1-Mar1; current period distinct；ledger-prepaid.json, m09-prepaid-detail.png, m09-prepaid-detail.xml |
| fixed_default_stop | PASS | Default fixed Mar1 latest paid boundary; future STOP stays current before boundary；ledger-planned.json, m09-default-stop-confirmation.png, m09-plan-before-boundary.png |
| stop_boundary_without_worker | PASS | Real clock at fixed boundary showed STOPPED in history; no Worker introduced；ledger-plan-boundary.json, m09-plan-boundary-stopped.png, m09-plan-boundary-stopped.xml |
| plan_same_id_correction_cancel | PASS | Mar1→Feb28 correction retained stateID/opID; explicit cancellation removed only plan；ledger-plan-corrected.json, ledger-plan-cancelled.json, m09-plan-corrected.png, m09-plan-cancelled.png |
| record_timezone | PASS | GuestK+14 Jan26, persisted recordGMT Jan25; PAUSED factJan25；record-zone-proof.json, ledger-paused.json, m09-record-zone-paused-verified.png |
| same_day_resume_early_stop | PASS | Jan26 RESUME then early STOP changed same existing rowID/opID；ledger-resumed.json, ledger-early-stopped.json, m09-early-stopped.png |
| covered_same_identity_reopen | PASS | Jan27 same service ENABLED, exactly no new payment；ledger-covered-reopened.json, m09-covered-reopened.png |
| next_period_current_rate | PASS | Feb1 actual current standard rate6.00, separate from historical average；ledger-next-period.json, m09-next-period-current-six.png, m09-next-period-current-six.xml |
| expiry_then_today_stop | PASS | Mar1 EXPIRED without Worker; then explicit today STOP routed to history；m09-expired-without-worker.png, m09-expired-without-worker.xml, ledger-expired-stopped.json, m09-expired-then-stopped.png |
| uncovered_empty_and_atomic_reopen | PASS | Amount/paidOn actually empty; explicit240+Mar2-Apr2/reminderApr1 plus ENABLED share operationID, same service identity；qa-uncovered-fields.json, qa-uncovered-empty-amount.png, qa-uncovered-empty-paid-date.png, qa-atomic-reopen-verified.json, ledger-atomic-reopened-verified.json, qa-uncovered-reopened-confirmed.png |
| correct_one_payment | PASS | Only newest240→225 changed; rowID/opID and all other facts exact；qa-payment-correction.json, ledger-payment-corrected-225.json, qa-payment-corrected-225.png |
| delete_wrong_payment | PASS | Explicit warning/date/amount confirmation removed only new serviceJan20/180; not refund；qa-payment-deletion.json, qa-delete-payment-explicit-confirmation.png, qa-delete-payment-explicit-confirmation.xml, ledger-payment-180-deleted.json |
| real_date_offline_metrics | PASS | GMT real host date, no default network cold start: EXPIRED/375/60/60/6.25/current0/null；qa-real-date-offline.json, ledger-real-date-offline.json, qa-real-date-offline-expired-top.png, qa-real-date-offline-zero-current.png, qa-real-date-offline-elapsed.png |
| font_ime_scroll_validation_back | PASS | 360dp/200% with real IME: save button reached, bad-date rejected; first Back dismisses IME and retains raw；qa-font-ime-back.json, qa-200percent-real-ime-raw.png, qa-200percent-real-ime-save-reachable.png, qa-200percent-date-validation.png, qa-back-first-dismisses-ime.png |
| rotation_raw | PASS | Both directions retained bad-date/M09_RAW_DRAFT and original Jan25 state target；qa-rotation-raw.json, qa-landscape-raw-date.png, qa-landscape-raw-reason.png, qa-portrait-raw-date.png, qa-portrait-raw-reason.png |
| actual_edge_back_keep | PASS | Actual gestural overlay/mode2, left-edge swipe shows dirty dialog; Keep retains raw；qa-edge-back.json, qa-gesture-before.json, qa-gesture-actual-mode.png, qa-edge-back-dirty-dialog.png |
| saved_task_process_restore | PASS | System STOPPED/HaveState Bundle11428; UID SIGKILL5523, same Recents task3674 newPID8327, both raw fields retained；process-pre-resume.json, target-task-before-kill.txt, qa-saved-task-recents-before-resume.png, qa-saved-task-process-restored.json, qa-process-restored-raw-date.png, qa-process-restored-raw-reason.png |
| discard_no_write_restore_final | PASS | Explicit discard; seven tables exact before/after draft. Guest settings and overlay/mode restored; UTC delta<10s; final cold read metrics exact；qa-draft-no-writes.json, qa-explicit-discard-confirmation.png, qa-final-check.json, ledger-final-restored.json, guest-settings-restored.json, qa-gesture-restored.json, qa-final-restored-expired-historical.png, qa-final-restored-zero-current.png, qa-final-restored-total-elapsed.png |

## 草稿、返回和进程恢复

360dp/200%字体下真实IME可见，保存按钮可滚动到达；bad-date本地拒绝，原输入保留。Android Back第一下只收IME。真实gestural overlay/mode2的左边缘滑动出现dirty对话，继续编辑保留原输入。横/竖屏均回读bad-date及M09_RAW_DRAFT。

系统确认Activity STOPPED、HaveState=true、Bundle11428字节之后，仅UID SIGKILL旧PID5523。实际同一Recents任务3674卡片恢复新PID8327，两个raw字段保持。全程未使用force-stop/snapshot/relaunch来恢复草稿。此项不是实际内存压力LMK，也不证明force-stop或断电恢复。

显式放弃草稿后，七表事实与草稿前完全一致；原stateID/付款及所有旧事实无写入。

## 设置和数据恢复

全部guest设置按 guest-settings-before-complete.json 精确恢复：320×640@160dpi、font1.0、原旋转/自动时间/时区/网络；systemIME null、secureIME0。gestural overlay enabled及navigation_mode2精确保持/恢复。guestUTC 2026-10-13T08:26:01Z，hostUTC 2026-10-13T08:26:01.842674+00:00，误差0.842674秒（小于10秒）。

最终Room2 identity a8fd277b0ef08ce9cf59b704734d0c25，integrity ok/FK空；active-generation、ready及settings SHA256 fff800a6a09afbc2e6656c8d67b512b74ba8e1693666e32b96701c7dfcf06c44完全保留。最后正常设置冷启动指标截图及ledger-final-restored.json逐项核对。

## 保留的失败与诊断

- root_immediate_blank_assertion：实际驱动失败，原件保留。Initial root assertion failed before printing nodes/text. Old atomic-reopened intention snapshot still had five payments/four states. Later actually visible amount/paidOn were both empty; no product autofill observed. 证据：ledger-atomic-reopened.json, qa-uncovered-fields.json
- offscreen_identity_and_fields：实际驱动失败，原件保留。Uncovered payment driver demanded offscreen name or missed a field through long fling. No confirmation occurred. Fixed ignored driver uses small slow drags and label/value readback; original failures remain. 证据：qa-failure-confirm-uncovered.txt, qa-failure-confirm-uncovered-2.txt, qa-driver-field-after-miss.png, qa-driver-form-at-top.png, qa-field-trace.json
- early_restore_lookup：实际驱动失败，原件保留。After the one real Recents tap, lookup began before an app scroll area was observed. Kept original failure; only waited for actual visible form, with no new process kill/force-stop/relaunch/card tap. Same task/newPID and both raw fields then verified. 证据：qa-failure-resume.txt, qa-process-after-resume-initial.png, qa-saved-task-process-restored.json
- timezone_label_typo：实际驱动失败，原件保留。First screen assertion used nonexistent label paused-instead-of-paused; actual detail label is 已暂停, DB facts already correct. No source changes. 证据：record-zone-proof.json

这些失败不能算成功尝试，也没有据此改生产。每项最终用户路径已用独立实际观察与完整事实重新核对。原 atomic-reopened.json 是未完成动作的意图命名，实际仍旧5付款/4状态；正式成功证据使用新的 atomic-reopened-verified，未覆盖原件。

## NOT_RUN

- physical_phone：NOT_RUN；Dedicated emulator only
- api26：NOT_RUN；API36 only
- oem_variants：NOT_RUN；AOSP/Google emulator only
- release_build：NOT_RUN；Actual debug/code7 APK; Release not installed/run
- full_talkback：NOT_RUN；No complete screen reader journey
- real_lmk：NOT_RUN；Controlled saved-task UID SIGKILL; not actual memory-pressure LMK
- force_stop_or_power_loss_draft_guarantee：NOT_RUN；No force-stop/power-loss draft claim; excluded by contract

结构证据及逐文件SHA见 device-qa-report.json。私有数据库副本仅作本地事实核查，不公开。GUI租约已交还root，子代理ADB已停止。此报告不自行授予ACCEPTED/FROZEN。
