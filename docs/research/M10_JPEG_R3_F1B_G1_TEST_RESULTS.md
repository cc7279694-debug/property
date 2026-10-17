# M10-R3-F1B-G1 测试结果

本轮全部为**HOST_ONLY**；无ADB/Android实验。来源`04561e14d5d5694f99dadf35d9a92e42d851e9bd`，研究分支`research/m10-jpeg-strict-r3-f1b-g1`。

## 最终唯一统计

| 测试组 | 唯一方法 | PASS | FAIL/ERROR/SKIP |
|---|---:|---:|---:|
| ServiceRecord分类 | 38 | 38 | 0/0/0 |
| 组合门禁及真实监督器FakeCollector集成 | 27 | 27 | 0/0/0 |
| F1B既有stage快照回归（源码不改） | 16 | 16 | 0/0/0 |
| 合计 | **81** | **81** | **0/0/0** |

Python3.12.4；命令`python -B docs/research/m10-jpeg-r3-f1b-g1/run_host_tests.py --tag green-final`，退出0，日志`Ran 81 tests in 0.558s / OK`。方法身份、源码/fixture字节SHA和日志SHA逐项记录于[green-final.json](m10-jpeg-r3-f1b-g1/evidence/green-final.json)，原始日志见[green-final.log](m10-jpeg-r3-f1b-g1/evidence/green-final.log)。runner同时核对方法ID无重复，禁止`subprocess.run`执行，且不写Python缓存。

## 验收映射

| 要求 | 代表测试/结论 |
|---|---|
| 历史闲置记录不因名称误拦 | `test_complete_synthetic_idle_record_is_candidate_not_eligibility`＋`test_supervisor_accepts_complete_consistent_synthetic_idle_snapshots` PASS；原body单独仍因不完整BLOCKED。 |
| 实际Worker、Service关联活跃app | `test_actual_worker_blocks_even_idle_service_and_absent_stage`、`test_service_associated_process_blocks_even_when_external_ps_missing` PASS（均验证必须阻塞）。 |
| Binding、执行/启动/restart | `test_binding_section_blocks`、`test_active_start_execution_restart_fields_block`、`test_known_pending_global_section_with_target_blocks` PASS。 |
| stage/current/pin未知、REVOKED及未终结 | `test_revoked_or_nonterminal_stage_never_bypasses_existing_verifier`及原16项所有权/receipt/inode回归PASS；没有Android租约声明。 |
| app=null信息不足/缺capture | `test_missing_mandatory_field_blocks_app_null_shortcut`、`test_service_app_null_alone_is_not_enough` PASS。 |
| 多目标、截断、未知字段/段、身份矛盾 | 分类器对应命名方法PASS；capture退出码、字节数/hash、完整标志、类型、最终换行均检查。 |
| Last ANR及其他应用Service排除 | Last ANR、相邻记录、无关Pending Worker类名、仅以Probe为caller的其他服务均有阳性防误挡；伪造非目标头掩盖研究body会BLOCKED。 |
| 两次不一致必须停止 | `test_two_service_or_process_states_disagree_stop`、`test_two_individually_safe_stage_snapshots_must_still_match`及时间/ID/身份冲突PASS。 |
| 完整一致研究场景合格 | 组合阳性＋原监督器直接集成PASS，仅输出研究范围PREFLIGHT_ELIGIBLE。 |
| 不能改写旧Android预检 | `test_old_actual_f1b_evidence_remains_incomplete_not_android_eligible` PASS。 |

## RED/GREEN保留

| 记录 | 实际结果与用途 |
|---|---|
| [red-supervisor.log](m10-jpeg-r3-f1b-g1/evidence/red-supervisor.log) | 原监督器名称条件导致1方法FAIL，退出1；不是导入失败。初次测试源字节SHA未单独采集，方法与旧源SHA/真实调用栈保留，不补称已采集。 |
| [red-composition.log](m10-jpeg-r3-f1b-g1/evidence/red-composition.log) | 25方法，6个failure事件（含同方法subTest）；暴露正式UID过拦截、包字段和APK路径关联缺口。修复前[gate](m10-jpeg-r3-f1b-g1/evidence/red-composition-gate.py.txt)/[测试](m10-jpeg-r3-f1b-g1/evidence/red-composition-test.py.txt)保留，不把6个failure事件计为6个唯一方法。 |
| [classifier证据目录](m10-jpeg-r3-f1b-g1/evidence/classifier/) | `red-boundary-hardening`31 PASS/2 FAIL；`red-foreign-pending`33 PASS/1 FAIL；`red-conflicting-body`35 PASS/1 FAIL；`red-review-final-methods`35 PASS/3 FAIL。中间`red-review-conflicts`实际37方法、35 PASS/2失败方法（3个failure事件，含subTest）及各次GREEN保留，不叠加总数。 |
| [green-integrated-01.json](m10-jpeg-r3-f1b-g1/evidence/green-integrated-01.json) | 中间79 PASS，后增加空ps和双安全stage变化测试，不代替最终81。 |
| [red-process-completeness.json](m10-jpeg-r3-f1b-g1/evidence/red-process-completeness.json) | **实际81 PASS**，标签虽为red但首次测试只改变一轮，因两轮不一致已被挡，未隔离空ps缺口。原结果不改。 |
| [red-process-completeness-02.json](m10-jpeg-r3-f1b-g1/evidence/red-process-completeness-02.json) | 改成两轮都只有ps表头，实际80 PASS/1 FAIL；最小加固ps必须有实际行，最终81 PASS。修复前源码及测试快照保留。 |

各中间结果均为当时源码，不伪称与最终源码一致；最终输入映射取green-final。旧F1B16项方法身份全部保留；R3/F1A历史17/60项Java/JVM结果未重跑，不合计到Python81项。

## NOT_RUN与限制

| 项目 | 本轮状态 |
|---|---|
| 设备在线/身份、实时服务/进程/stage预检 | NOT_RUN，未调用ADB，不能从F1B历史推定当前状态 |
| APK安装/启动、Outsider拒绝/泄漏消失、一次normal | NOT_RUN，未授权 |
| Binder/FD关闭/lease/worker死亡/实际清理 | NOT_RUN；宿主snapshot PASS不等于系统证明 |
| Gradle、Android Lint、APK构建、Java/JVM回归 | NOT_RUN，本轮只允许Python研究脚本/宿主测试；未改Android源码 |
| 原29场景 | 保持0 PASS／1 UNKNOWN／28 NOT_RUN |
| 生产JPEG、UI及业务回归 | 未重跑；M10三FAIL和UI最终整体FAIL历史不改 |
| 私人账本、图片字节及设备保护复核 | NOT_RUN；没有读取或复制私人内容，没有设备操作 |

Git范围、历史输入、链接和敏感信息检查见交付审计JSON；CRLF研究原件按字节保留，差异空白检查使用`core.whitespace=cr-at-eol`。原始unittest在subTest前留下的进度行尾空格属于日志原字节，evidence范围排除行尾/文件尾空白检查；源码仍检查空白，不重写日志或旧证据。
