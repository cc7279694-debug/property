# M10-R3 — 受阻结论与独立审查问题

日期：2026-10-16。**BLOCKED — M10-R3 Worker Lifecycle**。研究实现与证据可交接，但身份、终止与恢复核心契约没有Android实证，不能返回READY或批准生产修复。

阅读：[执行/输入/APK](M10_JPEG_R3_EXECUTION.md) · [真实结果](M10_JPEG_R3_LIFECYCLE_RESULTS.md) · [FD与恢复模型](M10_JPEG_R3_FD_RECOVERY.md) · [checkpoint](../checkpoints/2026-10-16-M10-JPEG-R3.md)。

## 1. 对九项交付问题的明确回答

| 问题 | 当前判断 |
|---|---|
| B1 worker身份是否可靠 | **未证明**。包UID与APK已核验；两次Host failed attach，尚无worker PID/UID/初始化证据 |
| 同步Binder握手与结果隔离是否成立 | **Android未证明**。强类型AIDL和gate有实现，17项JVM PASS只证明纯Java行为 |
| FD/pin生命周期是否安全 | **未证明**。设备前纠正FileLock及inode预检缺口，socket lease模型仍须实测，不能推断GC通过 |
| worker卡住时Host是否响应 | **NOT_RUN**。没有运行合成忙碌任务；系统启动Host超时不替代取消/响应测试 |
| worker/Host死亡与重启是否收敛 | **NOT_RUN**。原系统start timeout不算计划故障注入；不能只靠旧PID不存在作为内部恢复证明 |
| 哪些结论仅API36 | 实际设备识别、安装SHA/UID、系统Host启动失败及前后元数据保护；没有API36 worker成功结论，更没有API26恢复实证 |
| 哪些问题阻碍B1首选继续推进 | 当前研究Host启动/attach阻塞，底层原因UNKNOWN；另有未运行的全部核心恢复/终止门槛，不能生产GO |
| 是否启动A2 allocator/完整规范化 | **暂不启动**。先经ChatGPT独立审查，另行授权恢复可执行的R3生命周期验证；当前阶段未证明，不把工作跳到更复杂native阶段 |
| 是否转B2或重审架构 | 无证据因本次启动失败就判B1算法失败或自动切B2。保留R2研究候选，先审查阻塞与socket细化；更强沙箱/终止仍需新授权 |

## 2. Blocking与非阻塞资料

**Blocking：**既有API36设备两次Start proc后约10～12秒记录`failed to attach`并由系统`start timeout`结束，正常场景无报告，Application/worker事件未出现。根因UNKNOWN；没有重启、wipe/重建AVD、清数据、改变系统设置或安装新工具来强行解除。详细PID/原日志由生命周期报告承载。

已完成可审查资料：独立工程、typed AIDL、合成stage/lease/receipt/command重放模型、17项JVM行为测试及真实RED/GREEN、最终Lint/研究APK构建和安装字节核验、保护快照。它们不解除Blocking。

设备前本地辅助审阅发现并修正的模型问题：重复guard FD关闭可能释放FileLock；仅按basename删除缺少源/输出inode预检；仅测内存撤销会掩盖持久fence缺口。修正后的Android行为未验证，不能把辅助审阅当ChatGPT独立验收或新安全PASS。

## 3. 后续必须继承的闸门

只有新的明确授权才能再次运行/调整R3、恢复设备环境或进入下一研究阶段。本轮提交后停止。建议独立审查先确认：

1. 如何在保持原AVD/正式数据安全的前提下排查Host attach失败；若需重启、不同设备或工具改变，先定义范围，不自行重建/清数据。
2. socket lease是否值得保留，名称/owner/pin绑定、同Host持久撤销、FD交付间隙、重复FD、IPC/死亡乱序及异常清理能否通过真实实验；无法收敛不能用用户清数据兜底。
3. 所有29计划场景按原始事实执行；错进程终止、活consumer误删、错session有效提交、二次consumer或任何正式数据影响都立即停止扩展。
4. API26真实恢复仍是生产硬门槛；静态min26/Lint、API36及四ABI旧交叉编译均不替代。
5. 本方案只通过会话Binder请求worker自退，不按Host获得的PID发kill；将来若控制线程/整个VM失调度，仍没有可靠强停证据。不得把合作Java任务或系统start timeout称为任意native挂死、SIGSEGV、真实OOM/LMK验证。
6. B1同UID不是账本权限沙箱，socket也不是安全隔离；若威胁要求保护账本免受任意native代码执行，需另审B2及不同UID终止路径。

R1 40MP高RSS、6.141秒检查空档、120秒UNKNOWN没有关闭。allocator/spill/完整规范化/内存CPU上限、正式JNI/APK权限和签名审计、M10原三失败修复及完整回归仍归后续另批范围。

## 4. 模块与Git边界

UI-C1 **ACCEPTED / FROZEN — PASS WITH NOTES**；M00～M09冻结不变；M10 **PENDING / 原三项JPEG FAIL**；Q05 USER_CONFIRMED、M11/M12完整往返及Q06故障回退NOT_RUN；M11 **NOT_AUTHORIZED**。UI-C1历史718项715 PASS/3 FAIL、整体FAIL及N1～N4保持，不以研究unit PASS覆盖。

R0/R1/R2报告、失败和NOT_RUN、原规划130条/16组案例、正式验收矩阵、正式App/Manifest/Gradle/schema/Repository/Nav3均不改。只允许研究源码/脚本/非敏感证据、CURRENT_STATE和新checkpoint；仅推`research/m10-jpeg-strict-r3`，不合并、不发布APK。

最终Git SHA与远端一致性取提交后回执，不在本提交内容递归引用。文档链接、输入哈希、原始日志、范围/敏感信息及保护检查结果由checkpoint登记；本地辅助复核不自授冻结。

**BLOCKED — M10-R3 Worker Lifecycle**
