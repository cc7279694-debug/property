# H2-D2B — 离线测试、构建与实物

2026-10-17。**HOST_ONLY / 总体BLOCKED**。宿主通过不覆盖[API26/27兼容性缺口](M10_JPEG_H2_D2B_THREAD_TIMING_DESIGN.md)，不代表真实ANR已修复。

## 唯一测试口径

| 分组 | 唯一方法 | 结果 |
|---|---:|---|
| D2A全部旧JUnit | 145 | PASS；原12个测试文件逐字节保持，所有身份/断言保留 |
| HostCommandQueueTest | 20 | PASS；后台初始化、claim幂等/异常、串行、有界、在途重放与observer错误 |
| BoundedTraceWriterTest | 24 | PASS；阻塞sink、容量/字节上限、丢失、写异常、drain超时及短失败observer |
| BindingTimelineTest | 34 | PASS；六边界、早回调、单调时长、阶段/总期限、重复/错误顺序与UNKNOWN |
| DeadlineWatchdogTest | 6 | PASS；独立timer在合成UI/任务阻塞时仍触发、异常/关闭/旧票 |
| **最终JUnit** | **229** | **229 PASS /0 FAIL /0 ERROR /0 SKIP＝旧145＋新84** |

最终16份XML与源码逐方法相等：[唯一身份、源文件、SHA与实物审计](m10-jpeg-h2-d2b/evidence/build-runs/20261017T132201.765744Z/artifact-audit/result.json)。原D2A测试字节与锁定`59e61cd856b4039c693eb0f280eaf1f2ac838b6b`核对；没有删除或弱化原断言。

另外：原D1模型**22个方法PASS**，仍含120抽象顺序的一方法口径，见[独立重跑](m10-jpeg-h2-d2b/evidence/d1-regression/unittest.log)；Host源码静态契约最终**8个检查PASS**，见[最终静态日志](m10-jpeg-h2-d2b/evidence/source-green-final/unittest.log)。三种口径不合并为Android测试，不用静态检查掩盖API26反例。

## RED/GREEN及中间失败

| 组 | 实际RED | 实际GREEN | 证据入口 |
|---|---|---|---|
| 主线程源码契约 | 原6检查5 FAIL/1 PASS | 最终8 PASS | [RED](m10-jpeg-h2-d2b/evidence/source-red/unittest.log)、[GREEN](m10-jpeg-h2-d2b/evidence/source-green-final/unittest.log) |
| Host队列 | 可编译旧同步调度witness：1断言FAIL | 真正共享helper20 PASS | [目录](m10-jpeg-h2-d2b/evidence/command-queue-red/result.json)、[GREEN](m10-jpeg-h2-d2b/evidence/command-queue-green/result.json) |
| Trace | 可编译同步sink提取：1断言FAIL | 实际异步helper24 PASS | [RED](m10-jpeg-h2-d2b/evidence/trace-red/result.json)、[GREEN](m10-jpeg-h2-d2b/evidence/trace-green-final/result.json) |
| 时序/watchdog | 新增可编译seam的2行为断言FAIL | 最终40 PASS | [RED](m10-jpeg-h2-d2b/evidence/timing-red/result.json)、[GREEN](m10-jpeg-h2-d2b/evidence/timing-durations-green/result.json) |
| 实际时长计算 | 40方法中2断言FAIL | 同一40 PASS，不重复计数 | [RED](m10-jpeg-h2-d2b/evidence/timing-durations-red/result.json) |
| 集成复核回归 | 8静态检查中2 FAIL：reserve finally前返回、grant/result未读时序失败 | 同一8 PASS | [RED](m10-jpeg-h2-d2b/evidence/integration-red/unittest.log) |

提取/未实现seam均保留源码、SHA与日志，不冒称未修改Android组件曾在宿主真实运行。RED均是断言失败，不以缺类/编译错误作红灯；真实新helper已接入候选Runner/Support/Application。内部协作审查不是ChatGPT独立验收。

首次Gradle构建因Runner一处括号语法错误失败，完整保留[日志](m10-jpeg-h2-d2b/evidence/build-runs/20261017T131538.598364Z/gradle.log)，**不是TDD RED**。修正后中间构建229之前227方法成功；单调时长另增2方法后最终重新构建，原失败与中间产物记录不改写。

## 最终构建与Lint

复用JDK17.0.20.1+1、Gradle8.14.3、AGP8.13.2、Build Tools36.0.0、min26/target36；未安装或升级工具。仅独立研究root的probe/outsider各testDebugUnitTest、lintDebug、assembleDebug，offline、max-workers2。46份最终输入逐字节ASCII投影校验。

最终 `BUILD SUCCESSFUL in 33s`，退出0，99任务；[完整日志](m10-jpeg-h2-d2b/evidence/build-runs/20261017T132201.765744Z/gradle.log)、[最终输入](m10-jpeg-h2-d2b/evidence/build-runs/20261017T132201.765744Z/inputs.json)。没有构建正式`:app`。

Lint合计**0错误/8警告**：probe5、outsider3。原7类实例保留，新增DiscouragedApi指向watchdog.scheduleAtFixedRate在Android缓存恢复时可能追赶执行；未压制，列为待审风险。原OldTargetApi、DataExtractionRules、StaticFieldLeak、MissingApplicationIcon未借机升级或修改Manifest。

## APK实物（禁止安装的候选）

绝对目录：`E:/CODEX/.codex/worktrees/m10-jpeg-h2-d2b-host-scheduling/砺石新APP/build/d2b-artifacts/20261017T132201.765744Z/`。

| 文件 | applicationId | 字节 | SHA-256 |
|---|---|---:|---|
| probe-debug.apk | com.cooper.lithic.m10r3.probe | 80297 | 984ffc9b29a5ded2f05c47db657e17ef13ce847ad8fa4a4e5b62fef264a725bc |
| outsider-debug.apk | com.cooper.lithic.m10r3.outsider | 10473 | a51c4984f07986c8df52b3dbbf7f946523e4c259aa67ab5e8f9b3feffdf41879 |

实际Manifest/badging/DEX/证书工具检查：min26/target36、无权限、Worker非导出且`:jpeg_worker`、无native/JUnit/Hamcrest打包；证书SHA-256仍`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。D2A和F1-A两历史APK分别实际核验SHA与证书，均未覆盖：[D2A原件](m10-jpeg-h2-d2b/evidence/historical-apks/d2a-unchanged.json)，F1-A在实物审计结果中。新APK不提交Git、不发布、不安装。

复现本轮离线构建使用新`m10-jpeg-h2-d2b/build_offline.py`，只产生新时间戳目录；`audit_build.py`核对新目录，避免覆盖历史。APK可构建**不代表API26兼容缺口已解决**。

## NOT_RUN与阻塞

真实Android ANR、主线程延迟/StrictMode、watchdog最坏响应、API26/27、API36重新安装/运行、原29场景、Outsider/normal/cancel/kill/恢复、实际Binder/FD/lease/死亡、H2现场核查、私人内容字节及设备保护前后比较全部NOT_RUN。本轮ADB与设备操作0。

API26/27主线程onBind与异步身份就绪竞态为源码支持的兼容缺口；独立D2A-N01～N05原文仍NOT_PROVIDED。N05仅提出设计，不放宽失败终结。H2原FAIL/UNKNOWN、M10三JPEG FAIL、UI-C1冻结和M11 NOT_AUTHORIZED保持。整体状态BLOCKED，停止等待独立设计/代码审查。
