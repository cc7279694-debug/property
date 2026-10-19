# M10 B1/B2 Android 取证整改与续验

2026-10-19 · **BLOCKED / WAITING_CHATGPT_REVIEW**。来源 `532817ebaac5f59e0eb458c7d85d17765112606a`；分支 `codex/m10-b1b2-android-safety-resume`。本轮只改研究 Python 监督器、宿主测试及交接文档；`app/`、两份 APK 和旧断言不变。

## 已完成的三个接缝

- **读取器**：64KiB 分块、有界 `dd + pipefail`；命令仍限30秒。核对两文件前后 UID/inode/长度、H0/H1 与读回 SHA、完整 END、实际命令退出码及 stderr 中完整 dd 计数；任何管道异常拒绝。实际 API26 原390B/2439B两 JSON 一次读回 **0.349秒 PASS**。新取得的原字节哈希不回填到旧失败记录。
- **短生命周期 Host**：将运行中的 `begin/assertHost` 直接身份报告与结束后的进程观察分开；不要求 Instrumentation 结束后 Host 永久存在。仍核对 App/Test、UID、boot、runId、原 JUnit/nonce 和无消费者；不能由 PID 缺失推断 FD/guard 已释放。原 method-00 保持 UNKNOWN；新建独立、不可变的[续接准入](m10-b1b2-android-resume/evidence/android/preflight-resume/admission.json)，没有重跑预检 JUnit。
- **正常退出/API36门禁**：新增 `fresh_environment.py 26 safe-exit`。必须先有全部7项自然/准入记录、5项异常/恢复记录、同 boot 和两轮无消费者/终结 Stage，再登记一次性退出 marker，通过目标 emulator console 正常退出；随后双重核对原 launcher/QEMU 创建身份与端口真正结束。API36只接受完整绑定的新退出证明。**此入口仅宿主测试，设备 NOT_RUN**；没有提前退出或强杀设备。

## 本轮真实结果

| 项目 | 结果与证据边界 |
|---|---|
| 同实例/安装身份 | PASS：`Lithic_M10B1B2_API26_20261019 / emulator-5664`，原 launcher40472/QEMU18424及创建时间、SDK路径、磁盘、5664/5665端口绑定均核对；API26/x86_64/boot=1；App UID10077、Test UID10078。 |
| 原预检恢复 | PASS（新的有限准入）：两 JSON 分别390/2439B，inode115488/115490、UID10077前后稳定；[原始读回](m10-b1b2-android-resume/evidence/android/preflight-resume/preflight-result.json)。旧 method-00 的 UNKNOWN 不改。 |
| 2MiB读取 | HOST_ONLY普通合成文件1.031秒 PASS；Android只读 `/dev/zero` 三次有界流读0.681秒 PASS，无文件写入。**Android 2MiB普通文件的 inode 稳定性/完整性能仍 NOT_RUN**；不将流吞吐冒充普通文件验证。 |
| 健康 JPEG 最小冒烟 | **PASS，JUnit1/1**：Host7128、Worker7154，同UID10077，Worker startTicks668444；唯一匹配结果、pin/FD、认证退休六阶段、Binder死亡、独立进程结束、guard和真实 Stage终结/自有载荷清理均成立。未重跑旧18项整套，仅复用本轮授权健康方法一次。[实际结果](m10-b1b2-android-resume/evidence/android/method-01/result.json)。 |
| B2三张40MP | **NOT_RUN**：输入准备先失败，未上传图片、未调用三个方法；无本轮40MP wall/CPU/RSS/PSS/HWM/native16数据。 |
| B1取消/迟到RPC、错session/重复授权、lease/GC、Worker/Host故障和恢复 | **NOT_RUN**：前置输入准备 UNKNOWN 后停止；未以旧原型或源码推理替代设备实证。 |
| API26正常退出/API36全部操作 | **NOT_RUN**：API26后续门禁未通过，保持现场。 |

读取原 attempt/result 的 SHA 分别为 `0ecf28718f3618cbde2bba10ea07094e047b0e101367650f657ae6b77702da37`、`d8ee1fab947efa91787a255674714f3e51069bdefcc38ca726343424287dca53`。真实最大读测试的限制见[读取结果](m10-b1b2-android-resume/evidence/android/reader-2m-stream-result.json)及[宿主结果](m10-b1b2-android-resume/reader/results.json)。

## 停止原因、最小后续修复

18:31:52（+08:00），唯一输入准备尝试在 `run-as com.cooper.lithic.debug stat -c %f:%u files` 返回 exit1：`stat: 'files': No such file or directory`。Android私有 `files` 目录尚不存在；传输监督器错误地预设其已存在。发生在创建目标目录及写任何图片字节之前，[原结果](m10-b1b2-android-resume/evidence/android/input-transfer/result.json)为 **UNKNOWN / files=[]**。本轮未重试、未清 marker、未上传字节；该尝试仍已消耗。

停止设备操作后，仅离线补充 `ensure_owned_input_parent`：新独占且未结束的 claim 才能核对 run-as UID、规范私有根、归属，再独占创建不存在的0700 `files` 并回读 device/inode；既有目录只核验，不改写；链接、错UID、身份变化及竞争拒绝。**新5项 RED→GREEN、旧4项回归 PASS，ANDROID_NOT_RUN**。旧 UNKNOWN/result 一律拒绝重用；没有为本次失败加自动续传、清理或绕行入口。

**本批唯一执行阻塞**：缺失私有输入父目录导致输入准备 UNKNOWN、独占尝试已结束。下一次需独立审阅上述最小修复与本次无写入证据，并明确批准新的有限传输续接身份/入口；不能重用旧 claim。无需因此重建App或模拟器。**正式冻结的B1异常与B2大图系统门禁仍未满足**，尚不能提交M10冻结。

## 验证、产物与最终现场

- 三接缝最终同组 **55个唯一宿主方法 PASS**（读取11＋预检15＋退出29），[原始输出](m10-b1b2-android-resume/evidence/final-host/stderr.raw)；前述输入目录新5＋旧4另9 PASS，[原始输出](m10-b1b2-android-resume/evidence/input-parent-final/stderr.raw)。另已执行原读取22及环境20目标回归，不叠加为新测试总数。RED/中间失败原件保留；没有全量JVM/native/Gradle/Lint或APK重建，不将Python结果计作Android PASS。
- App：13,900,723B，SHA `b7c90277fa6c27e6ab314aa52c293de8e445ba6a3deba9e984ab96f4280e8a4c`；Test：2,118,862B，SHA `37ad9ec91964b6a0b8a870ec35032164ebcdb6a1c41522e780cb7af343fcd338`。证书SHA均为 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；instrumentation为 `com.cooper.lithic.debug.test/com.cooper.lithic.data.media.M10ProcessBoundaryRunner`。实际设备安装字节复核未变，无APK提交。
- 本轮API26新增计数：创建0、启动0、App/Test安装各0、Instrumentation1（健康）、预检重跑0、图片上传0、故障终止0、正常退出0；API36全0。实际命令及退出码见[小型命令索引](m10-b1b2-android-resume/evidence/android/command-index.json)，其为隐私过滤的派生索引，非原始完整日志；原始大零字节吞吐日志仅本地保留，提交字节SHA与关键原始证据。
- [结束只读证据](m10-b1b2-android-resume/evidence/android/resume-ending-readonly.json)：同一API26仍运行，目标UID无Host/Worker；唯一健康Stage的归属、撤销、terminal和清理凭据完整，源/输出载荷已按测试协议清除；原两预检文件不变。输入父目录/目标目录均ABSENT，无B2记录或未知消费者。没有执行用户数据删除或恢复；私人账本/图片字节核验NOT_RUN。未访问H2/C2、旧API26或保留API36。
- 测试前余量仅为观察：宿主空闲3,050,228KiB，Guest MemAvailable1,048,052KiB/总1,530,888KiB，不是手机安全阈值或进程cap；512MiB仍仅native计费请求预算。

唯一[关键证据索引](m10-b1b2-android-resume/evidence/android/INDEX.json)；[checkpoint](../checkpoints/2026-10-19-M10-B1B2-ANDROID-SAFETY-RESUME.md)。M10 **PENDING**、三个JPEG历史FAIL、全部NOTE/UNKNOWN/NOT_RUN、D2A-N01～N05、A1-N01～N04、UI-C1 FROZEN、M11 NOT_AUTHORIZED保持。只提交推送当前续接分支，不合并产品分支；停止等待ChatGPT独立验收。
