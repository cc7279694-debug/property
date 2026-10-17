# H2-D2A — 失败围栏研究实现

2026-10-17。**READY_FOR_REVIEW — M10-R3-F1B-H2-D2A**。仅研究代码、宿主测试及研究APK构建；修复状态为 **IMPLEMENTED / PENDING_ANDROID_VERIFICATION**，等待ChatGPT独立代码审查。

## 契约与来源

目标：实现D1已审设计中的短失败围栏、N01持久授权谓词、绑定及Host FD责任。只改研究Host实现、纯Java测试、新证据和当前状态；不实现D2B、恢复、JNI或正式JPEG修复。用户转交D1独立结论为PASS WITH NOTES，仅限离线设计。

来源 `f85b8a9c5d14a745f8d9e3a33ca43c9c41eba890`，开始时来源本地与远端一致、工作区干净。独立worktree：`E:/CODEX/.codex/worktrees/m10-jpeg-h2-d2a-failure-fence/砺石新APP`；分支 `research/m10-jpeg-h2-d2a-failure-fence`，从该来源建立，未从main开始。

实际读取AGENTS、PROJECT、CURRENT_STATE、相关决策/架构/规范、D1三设计与22项模型、H2原始Host/Worker/ANR失败证据、F1-A实现/测试/构建及工具链锁。D1与H2原件保持不变。设计来源：[根因分析](M10_JPEG_H2_D1_ROOT_CAUSE_ANALYSIS.md)、[失败围栏](M10_JPEG_H2_D1_FAILURE_FENCE_DESIGN.md)、[恢复提案](M10_JPEG_H2_D1_SAFE_RECOVERY_PROPOSAL.md)。

## 实际修改

- `ProbeRunner`：每次Invocation持有自己的Ticket/epoch与Session；异常先短内存失败，再做同owner持久撤销、连接责任及报告。拒绝失败后的hello/grant/result，只有接受的result推进成功等待；原recover入口明确拒绝，普通run只验证全部旧stage终结，不自动清理H2。
- `InvocationFence`、`SessionGate`：首次失败固定、旧epoch隔离、grant原子检查、单次CURRENT/RECEIPT候选与慢I/O后的逻辑复核、健康终结后准入。结果与匹配death仍允许乱序。当前终结后的报告失败也会重新关闭本Invocation准入。
- `StageStore`、`StageAuthorization`、`StageAdmission`：真实状态白名单、pending/REVOKED/终结拒绝、撤销回读、recordOutput同谓词及inode保护；普通准入持有全部lease核对终结和pending，不删除旧资源。
- `BindingResponsibility`：真实Context/ServiceConnection匹配，排队/调用中/true/false/异常分别记录；false或throw先执行共享短围栏再解绑；UNKNOWN保留责任。迟到连接不安装新endpoint，Binder helper在短锁之外。
- `HostFdScope`：source/output整体try-with-resources，打开/创建/start异常先失败围栏再关闭；覆盖重复FD路径。Host关闭事件与EBADF适配已编入APK，**未在Android运行**，不能推断Worker副本关闭。旧rebind附加连接也使用同一责任helper。
- 新增7份Java测试、离线构建/实物审计脚本、RED/GREEN与最终XML/日志、本报告组、CURRENT_STATE及checkpoint。没有删除旧测试或修改其断言。

详细边界见[代码及设计差异](M10_JPEG_H2_D2A_CODE_REVIEW.md)，唯一方法及实物见[测试结果](M10_JPEG_H2_D2A_TEST_RESULTS.md)。没有引入第二状态系统；CURRENT_STATE仍唯一当前状态。

## 实际执行与产物

最终145个唯一JUnit方法PASS，其中F1-A原60项源码逐字节相同，新增85项；另行重跑D1原22个Python方法PASS，两口径不合并为Android或产品测试。Lint两模块合计0错误/7警告。研究两模块Debug构建成功，最终日志 `BUILD SUCCESSFUL in 18s`，退出0，99任务。第一次中间研究构建亦成功，后因内部复核修正绑定失败顺序和重复FD责任而重新构建；两次原日志均保留。

复用JDK17.0.20.1+1、Gradle8.14.3、AGP8.13.2、Build Tools36.0.0、min26/target36，无安装、升级或依赖更改。新ASCII投影逐字节核对38份输入，最终源文件与实物审计一致。仅研究Gradle root，未构建正式`:app`。

最终APK绝对目录：`E:/CODEX/.codex/worktrees/m10-jpeg-h2-d2a-failure-fence/砺石新APP/build/d2a-artifacts/20261017T122708.815561Z/`。

| APK | applicationId | 字节 | SHA-256 |
|---|---|---:|---|
| probe-debug.apk | com.cooper.lithic.m10r3.probe | 62501 | c5a8c704e9a40ffd38d698a0b76748b38be308b050d046be2f5238c4277455f9 |
| outsider-debug.apk | com.cooper.lithic.m10r3.outsider | 10473 | a51c4984f07986c8df52b3dbbf7f946523e4c259aa67ab5e8f9b3feffdf41879 |

两实物Manifest均无权限、min26/target36；Worker仍非导出且`:jpeg_worker`。无native库、JUnit/Hamcrest打包；签名证书SHA-256仍`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。旧F1-A原产物实际SHA保持原值（Probe a3ee…、Outsider a51c…），未覆盖；新APK不提交Git、不安装、不公开发布。

## 未执行与停止

**ADB调用0、设备操作0、APK安装/启动0、normal/Outsider/恢复0**。没有重新观察H2设备现场，没有读取私人账本/图片字节。没有修改WorkerService、AIDL、Manifest、权限、Gradle、依赖、正式App/Room/JPEG、原三项JPEG断言或M10/UI-C1/main分支。

H2仍是原FAIL bind timeout/ANR，current `586d0904-eaac-403a-895a-48c34cd98810`、DELIVERY_UNKNOWN/pin=true/source8192只是继承的末次事实，并非本轮重采。现场不恢复、不删除、不覆盖。原R3 29项仍0 PASS/1 UNKNOWN/28 NOT_RUN，完整生命周期BLOCKED；历史attach/R1超时UNKNOWN不变。

N01/H2-N04仅研究实现待Android验证；N02保守阻塞、N03历史及H1单次API36实证边界保留。M10 PENDING/三JPEG FAIL、UI-C1 ACCEPTED/FROZEN — PASS WITH NOTES、M11 NOT_AUTHORIZED保持。只提交推送D2A后停止，不自动进入D2B/恢复/生产修复。最终SHA及远端一致性以本次Git交付回执为准，不在自身文档内循环填写自身SHA。
