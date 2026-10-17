# H2-D2B — 实施前代码范围

2026-10-17。用户授权：仅研究Host主线程调度、绑定时序、诊断与离线安全回归。来源`59e61cd856b4039c693eb0f280eaf1f2ac838b6b`；独立分支`research/m10-jpeg-h2-d2b-host-scheduling`。本范围文档先于实现提交。

## 目标及修改面

1. `ProbeActivity`、`ProbeApplication`：Activity只复制命令并有界入队；Runner构造、角色/owner核验、持久claim放入同一个受控串行后台入口。角色或claim未确认不创建Invocation、不bind。API26身份读取在后台，未知身份拒绝；不通过main等待后台。
2. 新增小型纯Java串行入口helper及测试，实际Android适配由Host使用。队列有界，拒绝入队/初始化失败显式UNKNOWN；不增加第二条Session执行通道。
3. `ProbeSupport`及新增纯Java有界诊断writer：Host event生产者只取内存/时间/线程快照，不执行logcat、文件或fsync。独立writer记录序号、丢失、错误与黏性UNKNOWN；可在后台有界等待诊断落盘，但不得把诊断丢失当PASS。WorkerService源码保持原字节；共享Support只在确认Host时切换异步路径，未确认角色保守隔离。API26/Worker角色就绪竞态须单独核对，不能用错误默认身份放行。
4. `ProbeRunner`及新增`BindingTimeline`/独立watchdog：记录UI_BIND_POSTED、BIND_CALL_BEGIN、BIND_RETURNED、CONNECTED_ACCEPTED、RESERVE_ISSUED、HELLO_ACCEPTED，另记录reserve返回。允许回调早于同步返回。每阶段与不可重置总预算取较早截止点；watchdog只触发已有短内存围栏，不持慢锁，不做Binder/I/O。真正Android调度及最坏响应时间保持UNKNOWN。
5. N05单独设计：正常取消、资源已证实终结的失败、资源未知失败分别记录。**不改变InvocationFence成功终结口径，不实现markFailedTerminal，不执行恢复**，否则会越过D1独立恢复门槛。

可修改现有研究Host入口/Support/Runner，必要的纯内存helper及新增测试；原145个JUnit方法/断言及D1模型22方法保持不变。新增本轮构建/审计脚本、证据、报告、CURRENT_STATE与checkpoint。不修改StageStore磁盘格式/授权写语义；revoke、receipt、terminal仍同步持久核验，不经过异步trace。

## 禁止与停止门槛

ADB、设备/AVD操作、安装/启动/停止App、normal/Outsider/cancel/29场景、H2恢复/删除、生产App/Room/JPEG、WorkerService、AIDL、Manifest/权限/依赖、JNI、M11全部不在范围。若必须改这些边界或D1失败终结语义，停止并提交独立设计。

H2 FAIL/UNKNOWN、原R3 0 PASS/1 UNKNOWN/28 NOT_RUN、M10三JPEG FAIL/PENDING、UI-C1冻结、M11 NOT_AUTHORIZED继承。新APK只在本worktree生成，不覆盖D2A/F1-A产物。

## 验证

先保留可编译旧行为提取或实际源码契约的RED断言，再实现共享真实Java路径并GREEN。覆盖主线程职责、claim幂等/失败、初始化未就绪拒绝、双向回调先后、阶段/总期限、阻塞UI/工作线程时独立watchdog、重复失败、late回调及诊断满队列/写失败/超时。回归原145及单独D1 22；研究两模块test/Lint/Debug构建、最终APK Manifest/证书/字节SHA、输入哈希与范围/敏感信息核查。合成时序不代表真实ANR已修复。

## 来源缺口

仓库实际搜索没有找到独立编号的D2A-N01～N05审查原文；已向用户请求粘贴。当前可确认的是本轮用户明确授权及N05描述、D2A三报告和D1设计；不把旧F1-A N01～N03重新编号成D2A备注，也不编造缺失内容。后续若补充备注，先核对是否与本范围冲突。
