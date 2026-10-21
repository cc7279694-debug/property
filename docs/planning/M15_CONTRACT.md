# M15 — 历史档案与长期复盘

日期：2026-10-21。用户正式授权集中完成M15；来源`codex/m14-category-search`冻结提交`f3f4f694f6c3a7348cb7b8b64e795a631f394d12`，实施分支`codex/m15-history-review`。复用既有隔离worktree，不覆盖原M10工作区。

## Goal / Scope

回看入口提供两个用途：长期复盘包含当前及历史记录；历史档案只含截至同一业务观察日已经结束的项目及STOPPED订阅。复用原归档列表、详情、编辑及Nav3守卫。

RankingEngine.projectRanks / subscriptionRanks复用M02/M03：项目按heldDays降序或未展示舍入dailyRate升序，订阅只按standardPurchasedDays降序；类型分开、同值ID次排序。0与负值保留，不可计算项明确说明、不授予有效成本排名。项目结束后的真实资金修正影响截至观察日成本、不增加持有天数；订阅折算30/90/365天与实际已经过去时间分开，历史平均使用historicalAverage。

一次查询在相同generation、LocalWriteCoordinator和Room事务内固定业务日期，按批读取全部相关事实，全局计算排序后提供每50条显示窗口。全量计算不是固定50条开销，不建立持久榜单、每日快照或复杂缓存。写入、纠错、删除、类别、跨日、前台与恢复换代后重建窗口；旧请求/旧代不覆盖新结果。读取错误与空/零分开，保留旧结果时明确提示。

## Constraints / Dependencies / Out of Scope

- M14 ACCEPTED / FROZEN — PASS WITH NOTES；M13-T01～T05限定认可、T06 NOT_RUN / DEFERRED、G1未整体通过。M10 PENDING / DEFERRED，历史三JPEG FAIL及所有NOTE/UNKNOWN不变。
- Room schema2、backup format1、冻结金额/日期/状态算法、唯一owner与写入/恢复协议保持；不新增依赖，不合并独立UI-C1/M10代码。
- 不开发替换链、趋势、年度统计、通知、平均寿命、价值评分、换新建议、AI或云。无正式签名、真实个人账本、main合并或M16。
- 仅允许核验通过的原在线`Lithic_M14_API36_20261021 / emulator-5672`，专用根`E:/CODEX/.codex/research-env/lithic-m14-android-20261021`。同既有Debug证书更新App/Test；plain Application runner、新合成账本与受控备份文件。原实例缺失/身份不符则设备NOT_RUN；不启动MainActivity默认账本，不新建/重启/清数据/卸载/强杀/删除未知目录。5686/H2/C2/M10及其他设备不接触。

## Acceptance / Verification

M15-T01：仍持有8年项目入榜。T02：仅结束项目与STOPPED订阅归档。T03：两类型分榜。T04：逐行当前/截至今天结果标签，后结算不延长时间。T05：0/负数与未展示舍入排序真实。T06：年付3650今天生效，折算365天、历史10元/天，实际经过1天。

新增行为先真实RED再GREEN；既有正确行为直接回归。定向JVM/冻结引擎/首页/导航、Lint、Debug App/Test构建；真实Room/Compose、跨50条及同值排序、写后/跨日/换代刷新、读取失败、浏览事实不变、小型合成备份往返。少量截图注明隔离宿主与LocalDensity 360dp/200%边界；实际执行结果仅写当前验收登记和证据，不改原130案例或历史报告。

交付更新CURRENT_STATE、ACCEPTANCE_MATRIX、必要架构/决策、短报告及checkpoint；只提交推送M15分支，READY_FOR_REVIEW后停止，等待ChatGPT独立验收。
