# M15 — 历史档案与长期复盘验证

**READY_FOR_REVIEW — M15**。2026-10-21；来源`f3f4f694f6c3a7348cb7b8b64e795a631f394d12`，分支`codex/m15-history-review`。仅候选交付，不自授冻结或发行通过。

## 实现与数据边界

- 回看保留“现在/回看/设置”；长期复盘包含当前与历史，项目陪伴时间/日均成本及订阅折算付费时长分开。历史档案复用原已结束项目/STOPPED订阅及详情、编辑守卫。
- RankingEngine只复用冻结M02/M03；RankingRepository同generation、同业务日、同协调器＋Room事务按批读完整事实，全局排序后取50条显示窗口；ID次排序。0/负值参与真实排序，未展示舍入精度不丢失，不可计算项排末明确未排名。计算量随该类型全量事实增长，50不是计算上限。
- ReviewController整窗刷新与请求序号隔离；写后/类别/纠错/跨日/前台/恢复换代更新。错误不伪装空列表。新增REVIEW返回来源；定向修正订阅旧来源白名单和保存返回，不改变业务写入或状态规则。
- 关键代码：`domain/report/RankingEngine.kt`、`data/repository/RankingRepository.kt`、`feature/review/*`、`app/LedgerUiState*`与`LithicRoutes/Navigation`；订阅来源策略2文件。新增4份JVM与4份Android测试文件。App仅版本code10/name0.1.0-m15调整，无依赖升级。
- schema v2、备份format v1、冻结金额/日期/状态引擎、DAO及写入/恢复协议与M14基线字节一致。原规划/130案例/历史证据不改，不合并M10或独立UI-C1。

## 原六项验收：本轮实际PASS，等待独立审查

| 案例 | 真实验证 |
|---|---|
| M15-T01 | `review_includes_still_active`＋Room八年ACTIVE全局榜＋实际页面，持有2923天仍在榜。 |
| M15-T02 | Room `history_reuses_real_filters_and_excludes_pause_expiry_and_future_stop`及真实Nav3档案，只出现已结束/STOPPED；暂停/到期/未来停止排除。 |
| M15-T03 | 两类纯排名与实际类型选择，无跨类型评分或额外订阅成本榜。 |
| M15-T04 | 逐行当前/已结束“结果截至今天”；结束后退款写后真实刷新、持有天数91不增，跨日仍不增。 |
| M15-T05 | 原值0、负值、显示相同两位小数但未舍入值不同的顺序；全量105/跨50分页、同值ID、未开始不伪造0。 |
| M15-T06 | 今天年付3650：折算365天、历史10元/天、实际已过去1天；暂停后仍显示历史10而非currentRate0。 |

**JVM 133/133 PASS**：29新增＋104受影响引擎/首页汇总/时钟/金额展示/导航/代退休回归。**Android 32/32 PASS**：本模块20（Room12＋备份重算1＋Compose5＋真实Nav3旅程2）及相关12（代作用域3＋首页6＋订阅导航守卫3）。0失败、0错误/跳过。真实Room测试还验证协调器阻止批间写入、一次业务日期、读错≠空、只读事实不变；真实受控文件备份往返含七表、设置和媒体原字节，新代回看相同且旧代读被拒绝。

RED保留：排名14失败、Controller6失败、导航4失败/原归档1通过、REVIEW来源2失败/原来源2通过；GREEN对应测试未放宽。准备中的SDK偏好环境、命令参数位置和2个新测试导入编译失败另保留，不算行为RED。最终构建 **BUILD SUCCESSFUL in 5m10s**；Lint **0错误/9警告**，Debug App/Test构建及Android源码编译完成。[原始结果与截图索引](m15/README.md)、[最终唯一身份/计数](m15/results.json)、[构建日志](m15/logs/final-build-v2.log)、[JUnit XML](m15/jvm/results.json)。没有重跑全仓或旧18项M10实验。

## 实际产物与设备

| 项 | 身份 |
|---|---|
| App | `com.cooper.lithic.debug`，code10 / `0.1.0-m15-debug`，12,820,847 bytes |
| App SHA-256 | `12e48456bcb6be51c1f47ea71776fa03e4fbd9f99185c8aa2285ff3199f35d57` |
| Test | `com.cooper.lithic.debug.test`，2,814,944 bytes |
| Test SHA-256 | `fc1774b8b0e113d9191699c12559d08b3c2cf8a6c69044f7cd5cc2d4e5dc677f` |
| 签名 | 继承Debug SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；非正式稳定签名 |
| 实际runner | `com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner`（plain Application） |

两APK仅留本地标准build路径，不入Git；绝对路径均以`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/`接表中[产物相对路径](m15/install/artifacts.json)。Manifest无网络权限，min26/target36、证书/包名/安装后SHA与UID已核对。Test构建元数据为code0/name空，Manifest未显式声明版本（不冒称与App版本相同），见安装证据；App/Test UID10216/10217不变。

仅原在线`Lithic_M14_API36_20261021 / emulator-5672`、API36/x86_64；原launcher20744/QEMU7356及创建身份、磁盘/端口、boot ID均核验。App/Test同签保留数据更新各1；创建/启动/重启/停止/卸载/清数据/强杀/旧设备命令均0。不启动MainActivity默认账本，所有测试使用全新合成根，关闭后只清其可证明归属目录；原截图/未知目录不删除。结束no_backup为空、原实例保持在线、APK/UID与设备字号/密度/旋转设置一致。[结束保护](m15/device-end/summary.json)。私人账本/图片未读取，私人字节完整性NOT_RUN。

保留7张实际原始PNG并已目视检查：混合榜、负成本、订阅折算、两类历史、360dp/200%头部与订阅行。均为隔离ComponentActivity中的真实LithicApp/Nav3，后2张LocalDensity字体2倍；不是MainActivity或系统级大字证明。观察日通过BusinessClock模拟10-21→10-22，不改设备时间。图片原始字节设备/本地SHA一致。

## 尚未运行与交接

API26/真机/OEM/Release与稳定签名、默认MainActivity/真实数据、系统200%/完整IME/TalkBack、真实午夜、长期大数据性能及新的SAF/LMK/断电均NOT_RUN。本轮没有未解决的确定性M15缺陷；测试范围不替代这些门槛。

M10 **PENDING / DEFERRED**与三历史JPEG FAIL、M13-T06 **NOT_RUN / DEFERRED**、G1未整体通过、Q03～Q06及全部历史NOTES保持；不进入M16。只推送本功能分支，实际提交SHA和远端一致性由最终Git回执报告，等待ChatGPT独立验收。
