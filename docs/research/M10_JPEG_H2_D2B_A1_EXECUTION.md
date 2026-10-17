# H2-D2B-A1 — 执行与交付

2026-10-17。**READY_FOR_REVIEW — M10-R3-F1B-H2-D2B-A1**。仅离线研究身份兼容修复，等待ChatGPT独立代码审查，不自行授予ACCEPTED/FROZEN或安装许可。

## 来源与授权

仓库`cc7279694-debug/property`；用户转交D2B备注补录PASS WITH NOTES，D2B整体因API26/27身份竞态BLOCKED。本轮用户正式批准方案A及研究Host/Support最小修复。来源`3792917c7938c2ec78789df0a53a27215f4ee099`经本地/远端核对一致，旧工作区干净，新分支原不存在；通过原生worktree工具建立：

`E:/CODEX/.codex/worktrees/m10-jpeg-h2-d2b-api26-a1/砺石新APP`

分支`research/m10-jpeg-h2-d2b-api26-a1`。先以`2557b28`提交[范围](M10_JPEG_H2_D2B_A1_SCOPE.md)，再实施；实际读取AGENTS、项目记忆、D2B设计/来源补录/测试、真实Support/Application/Worker/Runner及研究工具链。原三份M10 JPEG失败及H2现场均不操作。

## 源码差异及原因

- 修改研究`ProbeSupport.java`：将角色判断委托统一身份gate；API26/27必要main入口允许原有有界/proc读取，try-with-resources关闭；不修改processName/owner，单独的同步观测不足以READY。非main不能凭已知名称绕过后台核验。报告追加只读内存身份snapshot。
- 修改研究`ProbeApplication.java`：API28+名称只登记framework hint，后台独立/proc通过gate交叉核验，再完成原owner/Runner初始化并申请READY；异常sticky失败；持久claim前明确检查verifiedRole与mainOwner。
- 新增研究`ProcessIdentityGate.java`及`ProcessIdentityGateTest.java`：纯Java身份/时序决策与实际生产调用路径共享，66项新JUnit。短状态锁外读取，后台完成仍须等待所有在飞同步观测一致；冲突、未知、计时/读取/关闭异常拒绝，首次失败保留。
- 新增A1专用离线脚本、RED/GREEN与实物审计证据、本轮范围/执行/兼容/测试文档和checkpoint；更新唯一CURRENT_STATE。完整文件与SHA见[交付核查](m10-jpeg-h2-d2b-api26-a1/evidence/delivery-verification.json)。

WorkerService、AIDL、Manifest、权限、Gradle、依赖、签名、StageStore磁盘格式及写入、原D2A失败围栏/epoch/FD责任、旧229测试和N05终结逻辑均不改；正式Kotlin/Room/Repository/JPEG/UI无变化。没有引入第二owner或新的Session执行路径。

## 实际验证与结论边界

完整研究JUnit **295 PASS＝原229＋新66，0失败/错误/跳过**；原16测试文件字节/断言不变，17份XML与方法身份一致。D1另22模型PASS，原静态另8、新静态另4 PASS，均不计Android。新gate真实候选的66run/3FAIL与修复后66PASS、旧语义adapter的4FAIL和真实接入静态4FAIL均保存。

最终研究构建17秒、99任务执行、Gradle及收集脚本退出0；Lint0错误/8原警告。48项输入与ASCII投影/SHA一致。两APK实物、Manifest/权限、签名、DEX审计通过，六份F1-A/D2A/D2B原APK保持。首次构建成功后收集脚本控制台编码失败如实保留，并修打印后完整重跑，没有隐藏非零退出。完整日志、版本、方法、绝对APK路径和SHA见[测试/产物报告](M10_JPEG_H2_D2B_A1_TEST_RESULTS.md)。

本轮只证明API26/27兼容决策的研究代码和离线构建；实现状态为**IMPLEMENTED / PENDING_ANDROID_VERIFICATION**。不宣称实际API26/27已能启动、ANR已修复或Android watchdog最坏响应已证明。100ms为事后拒绝阈值，不能抢占挂起syscall。API28提示与完整核验分离、在飞探测延迟READY及剩余风险见[兼容说明](M10_JPEG_H2_D2B_A1_COMPATIBILITY.md)。内部只读审查辅助核对，无权替代ChatGPT独立验收。

## 历史与停止

ADB/设备/App操作、APK安装/运行、H2恢复/清理、normal/Outsider/cancel/SIGKILL/原29场景均0；未读取私人账本或图片内容，也没有根据离线工作推定当前设备元数据一致。

D2A-N01～N05不CLOSED：N01真实ANR根因UNKNOWN，N02实际Android响应UNKNOWN，N03非原子与fail-closed保持，N04 PENDING_ANDROID_VERIFICATION，N05设计不实现失败自动terminal、不改D1门槛。H2原FAIL/UNKNOWN及末次DELIVERY_UNKNOWN/pin=true、R3 0 PASS/1 UNKNOWN/28 NOT_RUN及完整生命周期BLOCKED、M10 PENDING/三JPEG FAIL、UI-C1 ACCEPTED/FROZEN — PASS WITH NOTES、M11 NOT_AUTHORIZED保持。

只提交推送A1研究分支；最终SHA和远端一致性见Git交付回执。提交后停止等待独立代码审查，不自动安装新版APK、恢复旧stage或继续其他场景。
