# M18 总成本历史交付

**READY_FOR_REVIEW — M18；离线候选，不自行冻结。Android实测与新截图NOT_RUN。**

来源M17冻结 `7e74df960e994e3a36f4417f7d83b07ead63763f`；分支 `codex/m18-total-cost-history`；[契约与来源](../planning/M18_CONTRACT.md)。

## 实现与计算范围

“回看→成本趋势”默认总日均线，可展开项目/订阅三线共轴，保留负值、订阅阶梯和0区间。精确日期、文字清单、同日/全部事件50条可继续查看，事件能进入准确详情并按原导航守卫返回。空账本、无有效贡献、真实0、正负抵消、读取失败及普通日期提示分开表达。

[计算](../../app/src/main/java/com/cooper/lithic/domain/report/CostTimelineEngine.kt)复用OverviewEngine/M02/M03；[完整读取](../../app/src/main/java/com/cooper/lithic/data/repository/ReportRepository.kt)在同generation协调器和Room事务中分页捕获全部事实与事件，事务结束后后台计算；[刷新](../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendViewModel.kt)用票据/快照身份匹配发布截止日、选日、三项金额和事件。[页面](../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendScreen.kt)接入既有Nav3/生命周期，不改变首页、数据库写入或冻结引擎。Room3、backup2、M16保护、M12恢复和媒体规则不变，没有新依赖或图表持久字段。

[抽样](../../app/src/main/java/com/cooper/lithic/domain/report/TimelineSampler.kt)：240基础预算对应40桶、每桶三线各min/max；起止、所有事实日期和±1邻点额外保留。三线同日期集合，分项抵消时也保峰。完整日期流式扫描，临时内存约O(基础预算＋事实/事件数)，工作约O(D×C_frozen(F))，C_frozen包含单日冻结引擎的排序/结构校验与求值，另有事件索引和关键点排序，正负解释还调用冻结引擎，不声称只扫描一次或恒定240开销。每天检查协作取消；没有测出取消最坏延迟。精确非显示日期仍从原快照求值。20年样本为2006-01-01～2026-01-01（含首末7306日），与完整日级参照比较三线每桶极值、尖峰/负谷、短暂停与相互抵消；不是无限历史性能证明。

## 原案例与真实结果

| 案例 | 实际状态 |
|---|---|
| M18-T01 every_point_matches_reference | **PASS（JVM）**：固定seed18012026、17项目/11订阅/100观察日，独立逐项调用冻结引擎，三项未展示舍入金额与相加关系一致。 |
| M18-T02 closed_or_expired_removed | **PASS（JVM）**：Q02 1/9贡献1000÷9，1/10起0，1/20单项结算80而总额仍0；暂停/恢复/到期/停止重开/预付与同日开始结束。真实Room子项NOT_RUN。 |
| M18-T03 today_equals_home | **PASS（JVM同快照口径）；NOT_RUN（Android）**：纯参照一致；>50条真实Room、零负值/预付/M16交易合成测试仅编译，不能登记完整案例已通过。 |
| M18-T04 sampling_keeps_spikes | **PASS（JVM）**：7306日完整参考、非均匀点大峰、负谷、分项抵消、关键边界邻点超预算和非显示日精确值。Android绘图区/截图NOT_RUN。 |
| M18-T05 multi_event_day_not_false_causality | **PASS（JVM事件身份/同日73事件）；NOT_RUN（Android）**：同日完整索引保留；实际50条展开、文字入口、详情返回与业务只读证明尚未运行，不登记完整交互PASS。 |

最终 **174 JVM PASS＝30新增（领域24＋导航6）＋144相关回归，0失败/错误/跳过**；14类源码方法与XML逐项一致。[精确统计](m18/results.json)、[XML](m18/jvm-final/)。最终同组 `testDebugUnitTest / lintDebug / assembleDebug / assembleDebugAndroidTest -Pm13Isolated=true` **BUILD SUCCESSFUL in 5m39s**；294份实际构建输入前后指纹一致：[构建记录](m18/final-build.json)、[原始日志](m18/logs/verification-final.txt)。UP-TO-DATE/NO-SOURCE不计新测试。

Lint **0错误23警告**：[原文](m18/lint-final.txt)。原M17为9；本次多显示14条锁定工具/依赖版本提示（2条AGP/Gradle、6条额外GradleDependency、6条NewerVersionAvailable），指向未修改的版本锁文件；原VisibleForTests2、UnusedResources3及OldTargetApi1仍在。没有顺手升级或压制警告，不将23写成9。

真实RED保留：导航首次3项2FAIL/1PASS；领域首次22项21FAIL/1PASS；发现空账本/空日期列表可绕过类别校验，新增2项2FAIL后以原Overview校验最小修复。首轮172 JVM已PASS，但Android测试编译因2处LedgerSnapshot同名导入失败，显式导入后最终编译成功；编译错误不算行为RED。最终174方法不把重跑叠加。[原始证据索引](m18/README.md)。

## Android、截图与产物

只读CIM未发现原5672授权实例的launcher20744/QEMU7356；按约定没有ADB、AVD启停/创建、安装、仪器测试或截图。[实际可用性记录](m18/device-availability.json)。**新增17项Android测试全部NOT_RUN：Repository7、VM4、Compose5、真实Room/Nav3旅程1；新截图0。** 一致快照排队写、写后/跨日/迟到结果/换代、纠错删除、format2往返/旧代拒读、原导航守卫及360dp/200%交互均只有测试源码和编译证据，不能以JVM或旧M17结果替代。未接触H2/C2/M10/5686或默认私人账本。

| 产物（仅构建核验，未安装） | 实际身份 |
|---|---|
| App | `com.cooper.lithic.debug`；code13 / `0.1.0-m18-debug`；13,164,911 bytes；SHA-256 `a837fd4ae881efbf042587a53b87414a8a94cad935ba74ee99b87e7cce5b8ed9` |
| Test | `com.cooper.lithic.debug.test`；未声明业务版本；2,923,214 bytes；SHA-256 `8b225f072e931e42ec62d725973e2425cf5ff8f727293936e59eb58da948f766` |

[APK/证书/Runner核验](m18/apk/identities.json)、[最终App配置核验](m18/apk/apk-verification.json)。同原Debug证书`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，min26/target36，无INTERNET、九域备份排除；M18生产和四个测试类描述符在对应DEX中，但不冒称Android加载或运行通过。计划Runner为`com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner`，实际设备包/UID为NOT_RUN。App绝对路径 `E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`；Test位于同工程`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`。APK与密钥不入Git。

**剩余验收缺口：**在另获授权、身份完整的隔离设备运行上述17项并取得实际完整图表截图；本轮不擅自新建/重启设备。MainActivity/真机/API26/OEM、系统200%/完整TalkBack/IME、真实午夜、长期性能、Release/稳定签名及M13-T06/G1门禁继续NOT_RUN。Q02仅M18领域子断言获本次执行证据，Android和16组完整跨模块门禁未关闭。M10 PENDING / DEFERRED、三JPEG历史FAIL和全部历史NOTES/UNKNOWN保持。仅提交推送本分支，停止等待ChatGPT独立验收，不进入M19。
