# M20 — 续费提醒与权限可靠性

**READY_FOR_REVIEW — M20；独立验收PENDING。** 起点`33ae4f4da61b066d7e2f1ca25cf5f2353208c183`，分支`codex/m20-renewal-notifications`。[契约](../planning/M20_CONTRACT.md)、[结果统计](m20/results.json)、[最终命令/输入一致性](m20/final-candidate/result.json)。本轮完成代码与离线候选，Android与截图未运行，不能冻结M20。

## 实际交付

- 纯Planner基于完整真实付款/状态、记录时区reminderDueOn−3日09:00起；迟到按真实剩余日数，暂停不等于停止或付款，预付/有效停止/纠错抑制旧候选。清晨协调安排当天09:00，系统提前到达保留重试资格，不耗尽最后候选日。
- WorkManager固定2.12.0；唯一每日协调及非精确候选任务，启动/真实付款与状态/设置/恢复重新协调。Application提供唯一容器的WorkerFactory，移除Startup自动初始化及无用前台服务。任务绑定generation、付款行和完整事实revision；同代协调器保护重读→复核→发布→Receipt。发布前检查取消和最新时间，发布成功后的Receipt非取消写入；无权限/开关/校验失败不记假成功。
- 设置→通知与提醒：续费、里程碑两个独立默认开启偏好，权限/渠道状态单列；只有明确点击才请求API33权限，API26～32没有运行时通知请求。里程碑业务与发送明确未接入，仍归M21；原Nav3未保存/待确认/恢复守卫保留。
- Room v4九表、显式1→2→3→4及生产结构/identity预检，真实4.json；旧1/2/3原字节比对一致。format3包含回执/偏好/截止；旧v1/v2先验原字段/计数/SHA后显式升级。proof1/2原字节兼容，未发布恢复候选在激活前写截止/proof3、同步、复验；旧SWITCHING也先认证旧proof，失败保守回旧代。内部generation metadata仍v1。[产物及schema核查](m20/apk/identity.json)。

## 验证与保留失败

最终 **238 JVM PASS＝88新增＋150定向回归，0失败/错误/跳过**，含冻结订阅引擎/累计、M12协调/恢复/取消、M16旧格式/proof/导航、M19导航。Lint **0错误/25警告**；Debug App/Test构建成功（2m15s）。336个构建输入前后摘要一致。新增22个Android方法编译成功，**0实际执行/22 NOT_RUN**；Worker源码使用真实入口/Room和可控Publisher，不能冒称已验证系统持久队列或发布。[最终原始输出](m20/final-candidate/stdout.txt)、[stderr](m20/final-candidate/stderr.txt)、[Lint原件](m20/final-candidate/lint-results-debug.xml)。25份XML在同目录，统计链接列出对应类。

真实RED保持：[首轮40项/32 FAIL](m20/core-red/result.json)（Planner30未实现、format/proof2未实现，设置8 PASS）；[Dispatch25项/3 FAIL](m20/dispatch-red/result.json)（两个取消缺口；另一个为协程异常克隆造成的对象引用比较，改验类型/message/cause语义）；[跨日2 FAIL](m20/clock-red/result.json)；[清晨2 FAIL](m20/morning-red/result.json)。首轮集成[233项/230 PASS/3 FAIL](m20/core-green/result.json)来自旧测试仍要求当前导出format2；仅更新3处当前格式/计数期望，保留测试身份和全部legacy1/2输入、损坏拒绝及旧proof断言。首个[238 PASS构建](m20/final/result.json)后静态审查撤回无类型项目Receipt清理：两类可同UUID，M20只处理订阅Receipt；新增隔离Android保护源码待验。最终产物对应final-candidate，不混算此前次数。

[直接编译诊断](m20/planner-red/commands.json)因缺kotlin-reflect未启动测试，不计行为RED；[首轮集成编译](m20/integration-compile/result.json)缺await扩展，改用WorkManager自带Operation.await与Flow后构建通过。签名检查初次查错AGP测试Manifest目录的[路径诊断](m20/apk/audit-path-correction.json)保留，后按实际packaged路径及APK回读核实。

| 原案例 | 本轮已验证 | 仍未运行 |
|---|---|---|
| M20-T01 | JVM：候选、9点、时区、迟到、唯一键、实际Mutex并发/Receipt复查 | Android持久唯一任务、重启/OEM送达 |
| M20-T02 | JVM：预付/纠错/停止、完整revision、同代租约、旧代拒绝、取消/竞态 | 真实付款/状态/换代与系统Worker联动 |
| M20-T03 | JVM：剩余2/1天、到期不发、权限复核跨日、Q04独立边界 | 实际通知文案截图 |
| M20-T04 | JVM：权限/偏好拒绝不发布不记Receipt、两个偏好与错误反馈 | 系统权限弹窗、拒绝后完整业务页面、360dp/200% |
| M20-T05 | 最终合并Manifest＋实际APK权限审计PASS；无网络/精确闹钟/忽略电池优化/前台服务 | 真机/离线实际后台行为 |
| M20-T06 | JVM：v1/v2原始严格校验→升级、v3九表/偏好/Receipt完整归档、坏字段/关联/时间拒绝、proof1/2兼容、截止取记录时区且max | 真实Room1/2/3→4及重开、完整恢复/媒体、旧SWITCHING与进程中断平台实证 |

Q04-V01～V03的**工程限定JVM子断言PASS**：覆盖到10/15后成本0但10/17提醒仍有资格；10/18剩2天、10/20不发；相邻已确认付款、停止、偏好/替身权限、去重。Q04仍PROPOSED，旧原补测NOT_RUN记录不改；完整Android/16组跨模块没有被关闭。

## 产物、设备与风险

App `com.cooper.lithic.debug`，code15/`0.1.0-m20-debug`，13691164字节，SHA256 `776c00167dd856d39791fca4c9331fdfaf313dab096b2835df6e40b3a66fda38`。Test `com.cooper.lithic.debug.test`，3021343字节，SHA256 `10fad9fd57a45f2a759d306e9b1ff0f97808069b7b8882817b1f802e1c0e9296`。两包均原Debug证书`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，v2签名验证成功；实际Runner `com.cooper.lithic.app.M13IsolatedRunner`。APK只留本地outputs，不入Git。[实际权限](m20/apk/apk-verification.json)：POST_NOTIFICATIONS、WorkManager普通WAKE_LOCK/ACCESS_NETWORK_STATE/RECEIVE_BOOT_COMPLETED及原signature guard；无INTERNET、两种EXACT_ALARM、REQUEST_IGNORE_BATTERY_OPTIMIZATIONS。M01审计脚本默认规则不变，本轮显式m20 profile仅允许上述集合。

设备门禁只读CIM检查原M18锁定PID5688/34920，exit0且0字节，无法证明原5692实例。[原命令及完整性](m20/device-before/host-command.json)、[判定](m20/device-before/admission.json)。**ADB/安装/启动/新建/停止/清理次数全部0**；未访问个人账本、H2/C2/M10或其他现场。Android与全部实际截图NOT_RUN，无替代旧图。真实WorkManager队列/系统发布、权限、迁移重开和恢复集成是下一次独立验收前的明确门禁，不能用编译替代。

WorkManager官方固定POM/模块/AAR元数据已核验，[取证索引](m20/dependency/official-workmanager.json)；官方[Google Maven](https://dl.google.com/dl/android/maven2/androidx/work/work-runtime/2.12.0/work-runtime-2.12.0.pom)声明Apache2.0，当前AGP8.13.2/compile36/JDK17实际编译通过，未升级锁定工具。

已知限制：发布API成功不等于用户收到；发布后Receipt之前崩溃仍可重试，稳定完整tag/ID与onlyAlertOnce仅尽力减扰。系统强停/省电/OEM可影响时机。全量相关事实读取为O(全部事实)，50仅批次。若设备时钟回拨导致manifest.createdAt早于已存Receipt，整份归档拒绝而不丢回执；本轮不扩展时钟校准。M10 PENDING / DEFERRED/三JPEG历史FAIL、M13-T06延期、G1/G2未整体通过、Q04及全部历史RED/FAIL/UNKNOWN/NOT_RUN/NOTES保留。只提交推送本分支，等待ChatGPT独立验收，不冻结、不进入M21。
