# M10-R3-F0 — 根因分级与最小后续建议

## 当前补充结论 — D INTERMITTENT_RECOVERED

2026-10-17。**READY_FOR_REVIEW — M10-R3-F0 Attach Diagnosis**。用户另行批准仅启动原AVD后，同一锁定Host APK一次无参数启动取得fork、am_proc_bound、APPLICATION_ROLE/trace与Activity显示完整链。**EVIDENCED是这次启动成功；原两次失败的根因仍UNKNOWN。**

| 指定分类 | 当前判定 |
|---|---|
| A DEVICE_OR_RUNTIME_SUSPECTED | 对历史根因仍UNKNOWN。最初设备不可访问已实证且经批准上线；当前负载/权限观察不足证明历史Zygote/ART调度故障 |
| B RESEARCH_HOST_SPECIFIC_SUSPECTED | 目前无持续Host特异性失败证据，不能用本次成功反推所有Host路径无缺陷 |
| C HOST_CODE_DEFECT_CONFIRMED | 历史attach致因未确认。另有辅助Outsider资源清理缺陷实证，不能混为Host attach根因 |
| D INTERMITTENT_RECOVERED | **EVIDENCED，采用本类**：同APK裸启动绑定与Application/Activity成功 |
| E UNRESOLVED | 历史根因仍未解决；首次设备不可访问停止点保留，不再作为当前设备状态 |

当前与历史同时有两项差异：经用户批准从停止状态启动原AVD，且启动Intent无原run/case等extras。未隔离单个变量，没有充分依据说“模拟器重启修复了bug”“command参数导致超时”或“Host源码已被证明正确”。也未更改等待阈值、工具链、签名、APK或App源码。

### 新辅助代码NOTE：ServiceConnectionLeaked

Outsider进程已完成系统attach（am_proc_bound），系统明确拒绝非导出Worker的跨UID访问，随后Activity销毁时出现`android.app.ServiceConnectionLeaked`，堆栈指向`OutsiderActivity.onCreate():14`。这是**EVIDENCED的辅助Activity连接注册清理问题**；没有看到Worker连接成功或实例，不能将异常文字“originally bound here”解读为系统越权绑定成功。对照App不是纯启动基线，更不是零错误通过。

**只提案、不实施：**若另行批准辅助APK最小修复，将ServiceConnection引用置于完整清理路径可见的作用域，明确拒绝/异常/成功后的解注册责任，验证拒绝路径不残留Activity注册；不得仅换Application Context、吞异常或删除告警来冒充资源清理。精确API行为和异常分类须再核对并用同一拒绝场景复验。范围只限Outsider helper，不改变Host/Worker协议、不重新安装正式App，不增加其他实验。

用户原`runCase()`旧stage只查guard、未强制恢复/撤销/清理的静态NOTE仍保留，尚未修复或运行；它与本次辅助告警和历史attach超时均没有已证因果关系。

### 后续边界

本轮设备实验已结束，不再次启动、不执行原29场景。先等待ChatGPT独立审查这次启动链、资源/日志局限和辅助NOTE。若继续，需另行批准最小辅助修复及旧attempt收口，再申请R3生命周期范围；Host本次成功不自动打开后续授权。

**R3 Worker生命周期仍BLOCKED、B1生产NO-GO、M10 PENDING/三项JPEG FAIL、UI-C1冻结PASS WITH NOTES、M11 NOT_AUTHORIZED。**原两次UNKNOWN、R1 120秒UNKNOWN、API26与native/OOM/LMK等未运行事项保持。这里只提交研究结果，不授ACCEPTED/FROZEN。原F0不可访问证据和旧checkpoint完整保留如下。

---

## 首次交付历史评估（ef25b4d）

2026-10-17。**BLOCKED — M10-R3-F0 Attach Diagnosis**。当前可定位到“授权ADB目标不可访问”，无法再定位历史Host attach失败。

[诊断与来源](M10_JPEG_R3_F0_DIAGNOSIS.md) · [实际时间线](M10_JPEG_R3_F0_TIMELINE.md)。

## 证据与推断分离

| 判断 | 证据级别 | 结论 |
|---|---|---|
| 固定serial不可访问、设备清单为空 | EVIDENCED | get-state退出1，devices退出0但无设备行 |
| 当前未观察到两个指定模拟器进程名 | EVIDENCED | Windows只读过滤结果仅adb PID5568；不是全系统服务审计 |
| 原AVD可能未运行 | INFERRED | 与上述观察一致；未查询或修改AVD运行器，不能排除其他连接/命名原因 |
| 历史两次attach源于模拟器/ART问题 | UNKNOWN | 本轮未运行对照、无新设备资源或ART证据；不能由“现在离线”推导“昨天系统故障” |
| 具体Host源码缺陷导致attach | UNKNOWN | 没有定位堆栈或可复现差异；静态审核不提供此因果证明 |
| R3旧stage收口缺口 | EVIDENCED（静态） | 用户独立NOTE与源码一致，未修复；与attach根因没有已证因果关系 |

## 指定五类诊断结果

| 分类 | 本轮适用性 |
|---|---|
| A DEVICE_OR_RUNTIME_SUSPECTED | UNKNOWN：没有多个应用attach异常或系统压力实证，不采用此类确诊 |
| B RESEARCH_HOST_SPECIFIC_SUSPECTED | UNKNOWN：没有可运行的Outsider对照 |
| C HOST_CODE_DEFECT_CONFIRMED | 未确认：没有具体致因堆栈/差异；不得把旧stage NOTE归因于启动超时 |
| D INTERMITTENT_RECOVERED | NOT_RUN：Host未启动，无“此次恢复”证据 |
| E UNRESOLVED | **EVIDENCED：本轮诊断在前置设备可用性处受阻；历史根因UNKNOWN** |

这不是B1算法被否定的实证，也不是B1生产GO。当前不能推荐源码修复、切换架构、allocator或规范化实现来解决本次可用性问题。

## 最小下一步，需要另行授权

1. 用户确认后，使**原有**`Lithic_M01_API_36`重新可用，保留原数据/AVD/系统镜像。不清数据、不重建；若需要启动原AVD或修复ADB连接，先明确具体操作范围。本轮未实施任何该类操作，也不要求用户清App数据。
2. 获授权后，先核对serial/AVD/API、boot、资源、现有worker与实际三包SHA/UID、私有worker Manifest、正式数据/设置保护基线。发现旧worker先记录，不杀。
3. 只有全部门槛满足，才按F0计划一次不含command/case/fault/runId的Host启动；读取相关时间窗口系统日志，记录Application trace。需要Outsider时先实核私有Service及不同UID，且只一次有界启动/拒绝观察。不能用增加等待阈值替代证据。
4. 若需增加研究Host诊断源码，提交最小提案并停；不直接修改。若Host此次能运行，只记INTERMITTENT_RECOVERED，原两次UNKNOWN不关闭。
5. 后续生命周期授权前，先处理`runCase()`旧attempt恢复/撤销/清理静态NOTE。F0不能自动进入29场景或下一研究阶段。

## 保留状态与停止

用户转交的R3工程/有限测试独立PASS WITH NOTES只限原研究交付；R3 Worker生命周期仍BLOCKED、B1生产NO-GO；M10 PENDING、3 JPEG FAIL；UI-C1 ACCEPTED/FROZEN PASS WITH NOTES；M11 NOT_AUTHORIZED。R1 120秒UNKNOWN和其他未运行平台/故障测试均不关闭。

三份新报告、脚本及有限命令证据、当前状态和新checkpoint只在`research/m10-jpeg-strict-r3-f0`提交推送。旧R3报告/失败/测试、正式验收矩阵、App与其他分支保持原样。提交后立即停止，等待ChatGPT独立审查及环境操作授权。
