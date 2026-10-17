# H2-D2B-A1 — 已批准方案A与最小执行范围

2026-10-17。来源`3792917c7938c2ec78789df0a53a27215f4ee099`，新分支`research/m10-jpeg-h2-d2b-api26-a1`，独立worktree。本地/远端D2B来源一致，旧工作区干净，新分支不存在后创建；不用main作来源。

目标：修复API26/27后台身份初始化迟到时，未改动的WorkerService同步onBind误拒合法Worker角色的兼容缺口。用户已明确批准D2B设计选项A与本轮离线实施，不重新要求架构选型。

范围：仅研究ProbeSupport、必要ProbeApplication接入、实际被调用的纯Java身份gate及新测试。API26/27必要main角色入口允许最多256字节的真实`/proc/self/cmdline`读取，不等待后台、不设置全局READY或owner。同步角色、API28+framework提示、完整后台核验分开；reserve/Binder入口只接受完整后台READY且无冲突的身份。冲突/异常/未知保持拒绝，保持D2A安全围栏与D1失败终结门槛。

同步探测以单调时钟记录；100ms是本研究的**返回后拒绝策略阈值**（达到阈值即拒绝），不是手机已测安全时延或可强制中止的硬期限。时钟异常、缺失、倒退、读取/关闭错误、身份不明均拒绝；挂起读取不会因计时器存在就变成已完成。新观测在短锁外读取、发布时重新检查并发冲突，在飞探测未确认前不得把完整身份标为READY。

验证：先保存编译可运行的旧语义RED和真实集成静态RED，再GREEN；测试实际研究身份gate。原229个JUnit方法/源码/断言保持并完整回归，新方法单独计数；D1 22模型与原8静态契约独立执行。构建隔离研究APK、实核Manifest/签名/DEX/SHA，不覆盖D2A/D2B/F1-A原产物。

禁止：ADB、设备/App操作、APK安装/运行、H2恢复或现场变更、normal/Outsider/取消/故障注入/原29场景、JNI、正式JPEG修复、M11。WorkerService、AIDL、Manifest、权限、Gradle、依赖、StageStore/磁盘格式、正式产品代码均不改；若必须触及则停止。

真实API26/27/36启动、主线程耗时、ANR、Binder/FD/死亡/文件同步均NOT_RUN。D2A-N01～N05、历史FAIL/UNKNOWN、M10 PENDING/三JPEG FAIL、UI-C1冻结、M11 NOT_AUTHORIZED保持。只推A1研究分支后停止，等待独立代码审查，不能自行批准设备验证。
