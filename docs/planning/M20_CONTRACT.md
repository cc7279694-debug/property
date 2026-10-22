# M20 — 续费提醒与权限可靠性契约 / Implementation Plan

**Goal**：在M19冻结基线33ae4f4da61b066d7e2f1ca25cf5f2353208c183上交付尽力续费通知、独立权限状态、两个偏好、Receipt、schema4/format3迁移与限定验证。
**Architecture**：既有唯一AppContainer/ManagedGeneration/coordinator，纯Planner→发送前同代重读→平台Publisher→持久Receipt；WorkManager唯一持久任务协调，不复制账本owner。
**Spec**：本轮用户完整M20指令优先；原M20/N01～N05/B02～B08，Q04原状态PROPOSED保持。本契约直接实施，不是再次需求访谈。

## Scope / constraints
- 仅codex/m20-renewal-notifications；code15；只新增WorkManager2.12.0及最小必要支持，不升级其余锁定工具。
- renewal candidate = reminderDueOn−3日，记录时区09:00起，今天<due，可迟到补一次且写真实剩余日。成本EXPIRED不单独否定Q04提醒；无虚构付款/时长。
- 已确认后继覆盖衔接原付费边界且在reminderDueOn之前/当天生效，抑制原边界；STOPPED按真实有效日判定，晚于提醒边界的未来停止不提前抑制，暂停本身不抑制。
- key=RENEWAL:ownerId:boundary，稳定系统tag/id/onlyAlertOnce；发布成功调用后写Receipt，权限/开关拒绝不写假成功。发布后落盘前崩溃窗口如实保留，不承诺exactly-once。
- 发布前同协调器重新核对原行/完整事实/偏好/权限/Receipt与当前generation，旧代/对象删除不能发布或写回。
- schema1/2/3→4显式迁移，保留旧schema原字节；format1/2严格原字段校验后显式升级，format3必需Receipt/偏好/导入截止。恢复候选激活前写max(原截止,恢复业务日)，只重建triggerOn严格晚于截止的未来候选，不伪造历史Receipt。
- 两偏好默认开；只发送续费，里程碑界面明确M21未接入。POST_NOTIFICATIONS仅用户操作触发，拒绝不阻断业务。无网络/精确闹钟/忽略电池优化/常驻前台服务。

## Task / verification
1. 纯Planner与稳定key/文案规则：先真实RED，再GREEN；M20-T01～T03及Q04-V01～V03限定子项。
2. schema4、真实开库预检/完整迁移链、Settings、format3/旧格式升级/旧proof兼容；定向JVM与实际Android（可用时）覆盖M20-T06。
3. Worker/Publisher/唯一调度与设置权限页面；同代并发/失败Receipt/换代围栏；T02/T04/T05，实际平台渠道/Worker验证不能用纯模型替代。
4. 定向JVM、受影响M09/M12/M16回归、Lint、Debug App/Test与最终Manifest/APK；只核验原5692在线身份后复用新M20合成根，否则Android NOT_RUN，禁止创建/重启/停止/清数据/操作其他设备。
5. 少量实际截图、简短M20-verification及checkpoint/当前记忆，检查差异/敏感信息/链接后提交推送，READY_FOR_REVIEW停止。

## Shared implementation interfaces
- data/settings: ReminderPreferences(renewalEnabled=true,milestoneEnabled=true,importCutoff:LocalDate?=null); SettingsStore.reminderPreferences(), updateReminderPreferences(renewalEnabled:Boolean?=null,milestoneEnabled:Boolean?=null), reminderPreferencesWithinSnapshot(expectedCoordinator)。设置/恢复写入仍使用原协调器。
- receipt字段logicalKey:String,ownerId:String,kind:String,boundary:String,publishedAt:Instant；实际DAO notificationReceiptDao()。本模块Publisher只写RENEWAL；任何无类型归属/非法边界的导入拒绝。
- domain/reminder纯输入由完整真实renewal行ID+Renewal+状态事实组成，外部不靠显示字符串推断。平台适配由root完成。

## Excluded / stop
不进入M21，不改变冻结金额/日期/订阅规则，不合并M10或UI-C1，不触碰H2/C2/M10及其他保留现场。M10延期、三JPEG历史FAIL、M13-T06延期、G1/G2与Q04整体未通过及历史NOTES/FAIL/UNKNOWN/NOT_RUN保持。独立验收前不得冻结M20。
