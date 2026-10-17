# M10-R3-F1B-H1 决策与停止边界

2026-10-17。**READY_FOR_REVIEW — M10-R3-F1B-H1**，等待ChatGPT独立验收。

## 实际交付结论

本轮重新执行当前完整G1/U1预检并通过，不使用旧U2资格直接安装。两份原F1-A APK字节/签名/Manifest及代码来源核对通过，逐包install-r更新成功、UID保持、研究文件保护检查通过；唯一一次Outsider取得系统跨UID拒绝、RELEASED、真实销毁及新持久结果，并有完整日志窗口覆盖销毁后10.869993秒，无对应连接泄漏。正式APK/UID、188条元数据聚合及设置前后一致。

两份APK分别为`a3eea12d…0ea684`和`a51c4984…df41879`，完整哈希见[执行](M10_JPEG_R3_F1B_H1_EXECUTION.md)。实际本轮总数为**2研究包更新＋1 Outsider启动**，不是结束采集器JSON的局部0/0。原始文件保留，另以[全任务计数](m10-jpeg-r3-f1b/evidence/h1-20261017-01/h1-actual-command-counts.json)澄清口径。没有新的确定性阻塞或部分更新失败需要回滚；不因此扩大后续权限。

## 必须继承的限制

1. **N01～N03原件保留，不自动CLOSED。** N01持久REVOKED授权语义未修未测；N02半清理保守阻塞/未知归属保护不变；N03原历史NOT_RUN不改写，本次新增单次API36证据等待独立审查，不自动宣布普遍修复。
2. **Outsider不是normal。** Worker进程没有执行真实任务，数据FD/consumer lease/正常结果/退休/回收仍未验证。原R3 29场景仍0 PASS/1 UNKNOWN/28 NOT_RUN，完整生命周期BLOCKED。
3. **Activity销毁不是进程死亡。** 结束Outsider进程5352仍在，系统冻结为其缓存行为；目标服务为INACTIVE_RECORD_CANDIDATE，不清理，也不当作Worker退休证明。
4. **日志结论有界。** 覆盖依据来自实际完整buffer、边界时间及零格式/采集异常；只归档研究范围，不公开无关内容。仍不能独立重放所有原始日志，不能推断无限期或所有设备无泄漏。
5. **外部runId不是App nonce。** APK不读取该编号；本轮靠唯一Intent action、system activity token、PID/UID、文件SHA及时间窗关联。没有为加标识而改变锁定APK。
6. **数据保护有边界。** APK身份、Probe研究元数据和Outsider更新时已知合成结果保护通过，测试随后按设计更新结果文件。正式业务字节/行级核查NOT_RUN，未读取或归档私人账本/图片；没有由188元数据相等断言业务字节相等。
7. **新增监督器不是通用测试平台。** 仅本次正向路径有系统证据；未测试的超时/旧结果耗尽/异常分支保留，不自动重跑或拓展恢复策略。
8. **旧未知与产品状态不变。** 历史attach失败、R1取消/120秒超时UNKNOWN，M10 PENDING/三JPEG FAIL，Q05 USER_CONFIRMED且M11/M12完整往返NOT_RUN，UI-C1 ACCEPTED/FROZEN — PASS WITH NOTES及原整体回归FAIL，M11 NOT_AUTHORIZED全部保留。

## 下一步

只提交推送`research/m10-jpeg-strict-r3-f1b-h1`，停止等待ChatGPT独立验收。本次不开展normal、其余28场景、cancel、SIGKILL、恢复、JNI、allocator或正式App集成，不修改M10/UI-C1/main分支，不合并或发布APK。下一项实验须另获明确授权并重新核对现场。

交接：[Android结果](M10_JPEG_R3_F1B_H1_ANDROID_RESULTS.md)、[资源生命周期](M10_JPEG_R3_F1B_H1_RESOURCE_LIFECYCLE.md)、[汇总JSON](m10-jpeg-r3-f1b/evidence/h1-20261017-01/h1-results.json)、[checkpoint](../checkpoints/2026-10-17-M10-JPEG-R3-F1B-H1.md)。
