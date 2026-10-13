# M09 执行契约 — 订阅状态与真实付款历史

日期：2026-10-13。来源为本轮用户完整执行指令，实际附件 SHA-256 `d83fb2d77190b0c49df8b5adf9a6e13c8cae8ca203513f9f5e98b0729350eaab`。本契约落实已冻结原 M09 计划及用户正式确认的 A、B 决策，不追加产品功能。原计划/规格/案例不改。

## Goal / Scope / Out of Scope

在同一订阅身份、Room v2、唯一 owner/coordinator、记录时区及 M03 引擎上，实现暂停、恢复、到期、停止、计划停止修正/取消、STOPPED 重新开通，以及逐笔真实付款纠错/安全删除。提供正式当前/暂停/到期管理、回看已停止入口、详情及必要确认表单，保持三主入口。

禁止 M10 媒体、M11/M12 备份恢复、M13 汇总、M14 搜索类别 CRUD、日历/待办、通知/Worker、自动付款/退款/补差、服务商登录/取消、评分/AI/云端/网络权限；不修改 WHOCARE，不合并 main、不 force、不公开 APK，不自行冻结或进入 M10。

## Git / Dependencies / Constraints

- M00～M08 已独立冻结；本轮来源 `feat/m08-subscription-renewals`，起点 `0063ca504ca48ef9c7231d79dc155e9bf4bc8c82`。开始已实际确认来源本地/远端相同、工作区干净、目标本地/远端均不存在。
- 仅新分支 `feat/m09-subscription-lifecycle`；最终提交/推送后停止。main 及全部前模块分支保持。
- 原455个唯一方法（86 JVM/369 Android）保留并实际回归，新测试另计；原43件/130 NOT_RUN 案例、16组补测和旧证据/checkpoint不可改写。
- schema v2、v1/v2导出字节、实体/索引、M08迁移和预检不变；不增加表、依赖、owner、另一套成本或状态引擎。Debug code6→7，版本 `0.1.0-m09-debug`；其余已锁定工具链不升级。
- 只在专用 `emulator-5596`/API36 操作合成数据；Gradle/ADB由根执行者协调串行持有，可明确临时授权代理独占设备验证，设备租约期间根执行者不运行ADB或Gradle。Git由根执行者独占，禁止并行设备写入/构建。临时 guest 设置收尾恢复，不改宿主时钟和其他设备。

## 已确认产品决策

### A — STOPPED 可以重新开通

复用原 Subscription ID，保留全部付款与日末状态历史。当前已付款覆盖有效时，明确 ENABLED 操作即可恢复 ACTIVE，不新增付款。仅有已付款未来覆盖时可以明确 ENABLED，但生效前仍由 M03 算为 EXPIRED/当前价空值，说明未来期尚未生效。

无当前也无已付未来覆盖时，恢复/重新开通入口引导用户核对真实续费；真实付款与当日 ENABLED 必须同事务，失败不留下半条事实。正常续费不隐式重新启用 STOPPED。PAUSED 且无覆盖时，在用户完成恢复前仍保持冻结 M03 的 PAUSED 主状态，辅助说明覆盖已到期；不得另造状态真值。

### B — 默认停止最后已付边界

仅两个选择：默认「到期后停止」、次要「提前停止」。默认日期是 `max(业务今天, 所有已确认付款的 paidUntilExclusive)`；包括未来预付和覆盖空档。1/1～2/1 + 1/20预付2/1～3/1，在1/25默认停止应固定3/1，边界前按事实计算，3/1起STOPPED/贡献0。默认日期在确认时作为持久 STOPPED 行保存，不随后来付款/纠错自动变动。

提前停止只能明确选择业务今天或更早于默认边界的未来日期，不退款、不减少已付款金额或标准已购时长。过去已全部到期则默认今天停止；不存在早于今天的提前停止选项。说明仅改变砺石记录，不替服务商取消扣费。

## Repository 事务与重放契约

复用 `SubscriptionRepository`/`SubscriptionDao`/`LocalWriteCoordinator`。为生命周期命令固定完整事实基线：订阅身份＋全部续费＋全部状态行；列表顺序不影响集合比较，重复身份/日期及坏事实明确 CORRUPT/冲突，不以空记录或0元伪装成功。

新增 `SubscriptionLifecycleCommands.kt`：

- `SubscriptionFacts(subscription, renewals, stateChanges)`：仅持久事实，不包含派生成本。
- `SubscriptionStateAction`：PAUSE / RESUME / REOPEN / STOP_AT_END / STOP_EARLY。
- `SetSubscriptionStateCommand(commandId, expected, target, action)`：新日边界；同日已有边界时 target 必须保留原行 ID/operationId，并明确作为修正，完整 CAS。
- `UpdateSubscriptionStateCommand(commandId, expected, expectedState, target)`：原状态事实修正，保留行身份及原 operationId；目标日期不与其他行重复，历史修正明确显示影响。
- `CancelScheduledStopCommand(commandId, expected, expectedState)`：只取消尚未生效 STOPPED，完整基线比较后删除目标行，不删除别的历史。
- `UpdateSubscriptionRenewalCommand(commandId, expected, expectedRenewal, target)`：保留原 Renewal ID/Subscription ID/operationId；完整旧记录 CAS，先校验正额/周期/四日期/全部覆盖，再更新原行。
- `DeleteSubscriptionRenewalCommand(commandId, expected, expectedRenewal)`：明确确认后只删指定错误付款；最后一笔拒绝，不删除服务身份。
- `ReenableSubscriptionWithRenewalCommand(commandId, expected, renewal, targetState)`：原身份上的真实付款与 ENABLED 同事务；独立固定 Renewal/State ID，状态同日修正保留原身份。
- `SubscriptionLifecycleResult`：`Saved(subscriptionId, stateId?, renewalId?, replayed)`、`CurrentAbsence(subscriptionId, factId)`、校验/冲突/存储失败。删除后仅当前缺失不能冒称永久操作重放证明。

接口为 `setState`、`updateState`、`cancelScheduledStop`、`updateRenewal`、`deleteRenewal`、`reenableWithRenewal`。输入 UUID 稳定，原命令信封在写入前保存；事务中先读取/验证真实事实及旧基线，精确已存目标才能返回 replayed，其他变化明确冲突。完整已存事实的精确重放先于新写入的业务今天/默认日期校验，跨日不得重算或拒绝此前已保存的固定停止信封。没有永久操作日志，不宣称仅凭 commandId 永久反查所有编辑/删除。

停止冲突：普通新续费不能越过仍未生效的 STOPPED；已停止状态下普通续费不能静默重新启用，应从明确重新开通流程处理。已提前计划停止可能本来就截断旧付款，因此金额/周期/付款日期等不扩张覆盖的纠错可保留该付款，界面提示固定停止日期。纠错在停止日以后的覆盖不得扩张到原记录该部分之外；若扩张，要求先明确修正/取消计划。付款删改不自动移动停止行。

未来及同日状态按 `asOf`/日末推导，统一调用冻结 M03；PAUSED/STOPPED 区间不累计实际有效日期，金额和标准时长保留。暂停/恢复常规操作采用业务今天，停止可今天/未来；单条状态历史修正可明确纠正日期和状态，不新增复杂编辑器/次数指标。

当前/已停止分页新增 `loadManagementPage(stopped: Boolean, cursor?, pageSize=50, asOf)` 和 `loadManagementWindow(stopped: Boolean, pageCount, asOf)`，返回原 SubscriptionPage；SQL按观察日最后有效操作边界筛选 STOPPED 与其他身份、50+1及稳定paidOn/id排序；PAUSED/ACTIVE/EXPIRED仍由M03给出并在当前页分组。原 M08 全身份分页接口保留兼容；所有读路径检查坏身份/付款/状态，不 INNER JOIN 隐藏异常，当前/历史已加载窗口一次快照重建。

## UI / ViewModel / Navigation

演进已有 SubscriptionViewModel/UiModels 与正式页面，仍只用唯一 `lithic.subscription.activeForm` Bundle；新增状态、付款纠错/删除和原子重开种类，raw、完整旧事实、UUID、固定提交信封一次保存。IN_FLIGHT 阻止连点/编辑，恢复为 UNCONFIRMED 后只核对原信封；COMMITTED 读失败只重读。精确 subscription/fact 身份匹配才成功导航，缺失/读取错误区分；陈旧提交不能覆盖后来详情。

新状态确认表单及已停止历史页复用主题、金额/日期输入、滚动与IME安全区。详情保留付款/标准时长/实际有效时间区分，显示必要状态历史、尚未生效付款及固定计划停止说明。历史付款行提供「修正」「删除错误付款」入口，删除明确不可在App撤回、会重算历史、不是退款；最后一笔阻止并解释。

回看 → 已停止订阅 → 同身份详情 → 重新开通；未来 STOPPED 尚未生效仍在当前管理，暂停不进历史。路由只存 UUID/来源，不存 Repository/派生成本；来源父链有界，历史最多 Home/Review/History/Detail/操作五级。继承项目＋订阅全局 pending/dirty 离开守卫，系统/手势返回、切Tab含重选、工具栏、隐藏恢复表单及成功内部导航都受保护。

## Acceptance / Verification

| 原案例 | 实际实施与必须证明的行为 |
|---|---|
| M09-T01 expiry_needs_no_worker | 固定Clock移动到排他结束日，EXPIRED/0/null；数据库无付款或状态自动写入 |
| M09-T02 cancel_renewal_keeps_paid_access | 双期预付1/25默认3/1，前ACTIVE/边界STOPPED；提前停止与空档、全部到期默认今天 |
| M09-T03 pause_is_not_history | 暂停分组、停止才历史、重开回当前，未来停止不提前移入历史，历史观察不被以后状态覆盖 |
| M09-T04 resume_without_new_payment | 有覆盖不付款恢复；无覆盖+未来付款不假ACTIVE；无任何覆盖真实付款＋启用同事务，同ID重开 |
| M09-T05 future_prepaid_has_own_label | 付款按paidOn、未来期尚未生效，状态操作不改付款或标准时长 |
| M09-T06 stop_reason_does_not_cancel_vendor | 服务商说明/原因、取消零写/主动确认；无网络权限或第三方调用 |

另落实用户22项安全要求：同日CAS、再停止历史、计划取消/修正、空档默认边界、已到期今天、计划后付款冲突、旧付款保留、无覆盖重开、原款纠错、重叠回滚、删除故障回滚/最后一筆拒绝、确认取消零写、并发/同操作异内容、陈旧基线、固定时区、历史/天数、读取错误、360dp/200%/IME、有界恢复/全局守卫和旧455回归。

真实Room行为测试先写，必要未实现接口编译壳仅用于获得有效断言RED；编译/runner问题另记，既有M03正确案例保留GREEN不故意破坏。分阶段保留当时源码/日志/XML/hash，修复失败后限定验证，再取得完整最终GREEN。根执行者实际运行 `:app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug`，最终JVM真实重跑；旧455和新方法分别统计、源码/XML一一对应。

专用 API36 最终APK实际走暂停→恢复→到期→计划停止→修改/取消→提前停止→历史→同ID重开→真实续费→断网重开及付款纠错/删除；检查SQL完整性/FK、同generation/settings/记录时区、最终code7/签名/无网络和备份排除。360dp/200%/真实IME、旋转及系统已保存任务恢复分开证据；真机/API26/OEM/Release/TalkBack完整/真实LMK未执行仍NOT_RUN，不泛化SIGKILL为LMK。

## 执行顺序与交付闸门

1. 事实恢复、基线保护、契约与A/B正式决策登记；无未解决产品阻塞。
2. Repository/DAO命令与真实Room安全测试：RED→最小实现→GREEN。
3. ViewModel原信封/状态恢复、三类正式UI和路由/离开保护：行为RED→GREEN，原455保持。
4. 最终四任务、源码/XML/hash/依赖/schema保护、APK、专用设备旅程及设置收尾。
5. 一次内部整体验证辅助审阅、适用问题修复/验证、文档及13项报告/Git只推M09。

任务进展和原始输出在ignored `build/m09-evidence/`记录，只是执行证据，不建立第二个CURRENT_STATE。正式长期状态只更新现有记忆；原checkpoint和归档不改。完成后 **READY_FOR_REVIEW — M09**，独立PENDING、M10未授权；立即停止。
