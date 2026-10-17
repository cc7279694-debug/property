# H2-D2A — 离线验证与研究产物

2026-10-17。**HOST_ONLY**。研究Java实际使用的helper及编译集成通过，不是Android/产品PASS。[执行](M10_JPEG_H2_D2A_EXECUTION.md) · [设计差异](M10_JPEG_H2_D2A_CODE_REVIEW.md)。

## 最终唯一JUnit身份

| 类 | 唯一方法 | 结果 | 来源 |
|---|---:|---|---|
| SessionGateTest | 17 | PASS | F1-A旧源码/断言逐字节保留 |
| BindingAttemptTest | 12 | PASS | F1-A旧 |
| StageAdmissionTest | 14 | PASS | F1-A旧 |
| StageIdentityTest | 12 | PASS | F1-A旧 |
| AuthorizationFenceTest | 5 | PASS | F1-A旧 |
| SessionGateFailureFenceTest | 4 | PASS | 新 |
| InvocationFenceTest | 22 | PASS | 新 |
| BindingResponsibilityTest | 14 | PASS | 新 |
| HostFdScopeTest | 10 | PASS | 新 |
| BindingFailureFenceIntegrationTest | 2 | PASS | 新，实际共享adapter+InvocationFence |
| StageAuthorizationTest | 20 | PASS | 新 |
| StageReadOnlyAdmissionTest | 13 | PASS | 新 |
| **总计** | **145** | **145 PASS/0 FAIL/0 ERROR/0 SKIP** | **旧60＋新85** |

最终12份XML逐方法与源码精确匹配；重跑或中间分组不累加。[最终逐身份/源码/SHA/实物审计](m10-jpeg-h2-d2a/evidence/build-runs/20261017T122708.815561Z/artifact-audit/result.json)、[38份构建输入](m10-jpeg-h2-d2a/evidence/build-runs/20261017T122708.815561Z/inputs.json)、[完整Gradle日志](m10-jpeg-h2-d2a/evidence/build-runs/20261017T122708.815561Z/gradle.log)。原60项测试文件与锁定D1 Git blob相同；没有删断言或改测试规避失败。

## RED/GREEN真实过程

| 分组 | RED | GREEN/最终 | 原始证据 |
|---|---|---|---|
| 原grant复合表达式提取进SessionGate | 编译0；旧17＋新4共21，2 FAIL，其中1行为/1同步边界结构断言 | 最终43 PASS＝旧17＋Gate4＋Invocation22 | [RED](m10-jpeg-h2-d2a/evidence/gate-red/junit.log)、[最终GREEN](m10-jpeg-h2-d2a/evidence/gate-green-final2/junit.log) |
| 绑定及Host FD责任 | 编译0；24执行/18 FAIL | 24 PASS；后增共享adapter2项后最终26 PASS | [RED](m10-jpeg-h2-d2a/evidence/responsibility/red-test.log)、[GREEN](m10-jpeg-h2-d2a/evidence/responsibility/green-test.log) |
| false/throw先围栏后解绑组合 | 编译0；2执行/2 FAIL | 2 PASS，已含最终26 | [RED](m10-jpeg-h2-d2a/evidence/responsibility-final/red-test.log)、[GREEN](m10-jpeg-h2-d2a/evidence/responsibility-final/green-test.log) |
| 状态/撤销共享策略 | 编译0；18执行/9 FAIL | 最终59 PASS＝新33＋旧26 | [RED](m10-jpeg-h2-d2a/evidence/stage-red/junit.log)、[最终GREEN](m10-jpeg-h2-d2a/evidence/stage-green-final/junit.log) |

RED保留编译成功后的失败日志、初始策略源码快照和SHA；是将旧pin-only/无回读/绑定布尔/FD异常路径等策略透明提取到随后由真实代码使用的Java seam后观察失败，**不冒称未修改Android Runner在宿主真实运行**。新Invocation/准入扩展部分在实现时加入GREEN，不宣称每个新增方法均曾在旧类上独立RED。没有拿编译错误作RED。共享adapter组合测试直接使用真实Java wrapper和InvocationFence，不是只测试复制到测试文件的假adapter。

主要保护：queued/calling bind失败、false/throw、旧epoch/endpoint、grant与failure竞态、首次失败、撤销成功/失败/unknown/重复、REVOKED pin无文件、pending/旧stage阻塞、解绑异常及幂等、FD打开/创建/start异常与suppressed、健康result/death两序、重复result、候选I/O失败与当前终结后报告失败。

## D1模型独立口径

另实际执行原D1 `test_fence_model.py`：**22个唯一Python方法PASS**，其中120个抽象顺序仍只占1方法。模型及旧证据不改，新增日志在[本轮重跑](m10-jpeg-h2-d2a/evidence/d1-model-regression/unittest.log)。不并入145 JVM，不计Android，不证明真实FD/socket/fsync或恢复通过。

## 构建、Lint与实物

最终六任务为probe/outsider各testDebugUnitTest、lintDebug、assembleDebug，offline、原依赖、两worker；最终18秒成功，0错误。第一次中间构建1分1秒成功，输入早于最后责任修正；仅最终 `20261017T122708.815561Z` 是交付APK基准，不能引用中间APK当最终。

Lint：probe0错误/4警告（OldTargetApi、DataExtractionRules、StaticFieldLeak、MissingApplicationIcon）；outsider0错误/3警告（除StaticFieldLeak）。合计7，未压制。没有工具链/Manifest/签名/权限变化。

两最终APK路径/字节/SHA见[执行报告](M10_JPEG_H2_D2A_EXECUTION.md)及[产物记录](m10-jpeg-h2-d2a/evidence/build-runs/20261017T122708.815561Z/result.json)。实际十次工具审计核对Manifest、badging、DEX及新旧证书：applicationId保持研究身份、min26/target36、无权限、Worker非导出且独立进程、无native/JUnit打包，旧F1-A产物SHA未变。没有用源码Manifest替代APK实物核验。

复现：`python -B -X utf8 docs/research/m10-jpeg-h2-d2a/build_offline.py`只在独立研究root创建新ASCII投影及不可覆盖证据目录；不会调用ADB/安装/构建正式App。随后以新证据目录参数运行`audit_build.py`，审计目录也禁止覆盖。类缓存不提交，完整原始日志保留。

## NOT_RUN与保留失败

Android队列/ServiceConnection、真实FD/EBADF/lease、Worker退休和OS死亡、跨进程异常、持久fsync故障/真实卡死、ANR改善、H2恢复、normal/Outsider、原29场景、API26/真机/OEM均 **NOT_RUN**。原H2 FAIL、历史UNKNOWN、M10三JPEG FAIL、原R3 0/1/28不改。正式App/生产APK/JPEG测试未重跑，私人数据字节及设备前后元数据验证NOT_RUN。本轮不接触设备，因此不能由旧188条聚合推定当前一致。

N01及H2-N04仅IMPLEMENTED / PENDING_ANDROID_VERIFICATION。没有把隔离构建成功、JVM或D1模型结果登记成M10-T03/T04通过；M10仍PENDING，UI-C1冻结，M11未授权。提交后停止等待独立审查。
