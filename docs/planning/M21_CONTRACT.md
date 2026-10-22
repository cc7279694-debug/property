# M21 — 长期里程碑与低打扰控制 / Task Contract

## Goal / authority

用户2026-10-22完整M21授权附件为本轮实施依据。从M20限定冻结`3e8f5fdef9853ada726b037cc8559e2117d24da9`建立`codex/m21-milestone-reminders`。Q03为本轮用户明确确认：首日1天，日节点N使用`startedOn.plusDays(N-1)`，周年使用`plusYears(N)`。不改原归档或冻结引擎。

## Scope / decisions

- 项目D100/D1000/Y1/Y3/Y5/Y10；订阅D100/D365/D1000/D1095/D1825/D3650。订阅首次达到日通过冻结M03 elapsedPaidDays的有界单调搜索取得，不按标准天数或逐日数据库扫描。
- 合法补发为目标日到+6日，记录时区09:00后尽力执行。初次纳入、历史时间纠错与恢复合并截至建立日（含当天）已达到节点的抑制；纯资料/金额修改及普通当前/未来付款不推进时间水位。旧水位/成功回执不可倒退或清空。
- 一张`milestone_watermarks`表，联合身份(ownerType,ownerId)，cutoffOn与严格六位suppressedMask；项目位序D100/D1000/Y1/Y3/Y5/Y10，订阅位序D100/D365/D1000/D1095/D1825/D3650。水位不是成功Receipt。业务创建/历史纠错/既有删除在原Room事务维护。
- Receipt kind使用`PROJECT_MILESTONE`/`SUBSCRIPTION_MILESTONE`，boundary为节点代码；logicalKey固定`MILESTONE:<PROJECT|SUBSCRIPTION>:<ownerId>:<nodeCode>`，续费仍为原RENEWAL语义。未知类型拒绝，不能静默过滤损坏数据。
- 同业务发布日一条稳定摘要；增量合法候选更新同tag/id、onlyAlertOnce，已有当日回执保留摘要来源，无新候选不发布。发布成功后本批回执同Room事务提交，保留发布到落盘间有限重试窗口。
- 点击只携带generation/发布日/来源摘要SHA/数量，由真实Receipt及新鲜完整事实重建类型化来源；单条准确详情，多条临时来源列表。重新验证对象/节点，沿用dirty/pending/恢复导航保护，不建通知中心。
- Room4→5新增水位表及真实schema5；format4/minReader4/schema5，保留旧1/2/3独立严格解析与原proof认证，再于未公开恢复候选合并保护、写新proof。active-pointer/ready内部协议版本不改。版本code16。

## Shared interfaces / implementation ownership

纯领域接口位于`domain/reminder/MilestonePlanner.kt`：`MilestoneOwnerType`、`MilestoneWatermark(ownerType,ownerId,cutoffOn,suppressedMask)`、`ProjectMilestoneSource(ownerId,name,startedOn,endedOn)`、`MilestoneCandidate(ownerType,ownerId,nodeCode,threshold,reachedOn,logicalKey,title,text)`；订阅复用`RenewalReminderSource`。

`MilestonePlanner.projectReached(source,asOf)` / `subscriptionReached(source,asOf)`返回截至观察日已达到的候选（不套窗口或水位）；`projectWatermark(source,on,previous?)` / `subscriptionWatermark(source,on,previous?)`合并截止及抑制掩码；`evaluate(projects,subscriptions,recordZoneNow,watermarks,successfulKeys,milestoneEnabled=true)`在完整水位下输出合法窗口候选；`logicalKey(ownerType,ownerId,nodeCode)`与`validateNode(ownerType,nodeCode)`为统一身份校验。候选threshold为对应日数或周年数，不用于跨类型比较。

存储`MilestoneWatermarkEntity(ownerType:MilestoneOwnerType,ownerId:String,cutoffOn:LocalDate,suppressedMask:Int)`及DAO；共用既有枚举/日期转换器。`MilestoneWatermarkStore`在现有同代/事务边界内负责读取、初始化、合并及删除；业务Repository调用它，不建立第二Owner。生产发布独立复核同代完整事实、权限、偏好、水位及回执。

## Plan / verification

1. 先补纯Planner/窗口/身份、format版本和水位契约的RED；再实现最小规则并GREEN，保留原输出。
2. 增加schema5/水位DAO/严格格式4与旧解析/proof兼容；接入事务内业务维护、恢复激活前保护。补迁移/往返/坏关联/故障回滚Android测试源码。
3. 接通独立里程碑调度/摘要Publisher/点击路由及设置，原续费遇到里程碑Receipt仍严格可运行；补并发/权限/开关/批量回执失败/导航回归。
4. 执行定向JVM与受影响回归、Lint、Debug App/Test构建与产物审计。仅root串行运行Gradle，不并发构建；保留真实失败，不改断言遮掩。
5. 更新Q03当前USER_CONFIRMED、项目状态、短报告与checkpoint，审核链接/敏感信息后只推M21分支。

T01～T06按原计划和本轮附件逐项记录实际证据；没有干净获准设备，全部Android运行、真实迁移/通知/点击/重启明确NOT_RUN，不用编译代替实测。M20产品限定冻结与额外平台FAIL/收口UNKNOWN保持；M10/M13-T06延期、Q04 PROPOSED、G1/G2/G3及其他历史边界不扩大。

## Out of scope / stop

不调用ADB、不查询或操作任何AVD/App/系统任务通知；不创建/启动/复用/清理设备，尤其M20/5704、H2/C2/M10等现场。无SDK/依赖升级、新取证系统、M10研究、正式签名或私人数据。不是M22/完整G3或M21冻结授权。完成提交推送后停在**READY_FOR_REVIEW — M21**，等待独立验收。
