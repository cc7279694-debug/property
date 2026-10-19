# M10 B1恢复与B2资源候选 checkpoint

2026-10-19；来源 `f1ca58b286381a053c551e6b0383d5141e3b9565`，分支 `codex/m10-b1-recovery-b2-resource-candidate`。**READY_FOR_ANDROID_SAFETY_REVIEW**；不授予M10冻结。

- 实现：原会话真实迟到RPC后主线程唯一清理接续；新Stage持久supervisor与独立abandoned非成功证明，两阶段无扰动资格校验/撤销、真实guard和文件归属、幂等清理；Store恢复入口。不能补造旧源/FD/结果/成功凭据。永久阻塞、无消费者身份、旧无supervisor、boot/归属未知保持保守。
- B2：三份锁定原件重新SHA核对，Debug合成permit资源检查点/采样及Release NOOP；可编译三项Stage→Worker→native→规范化→退休测试，失败操作和安全收口分开。全部Android NOT_RUN，不把计费/采样数当RSS硬cap。
- 验证：297唯一受影响JVM PASS（旧143＋79＋新75），0失败/错误/跳过；RED/GREEN原件；Lint0错误10警告；Debug两APK、Release Kotlin编译、四ABI/声明/签名/Manifest/静态DEX通过。没有重跑旧86业务JVM、18自然或native压测。第二次仅编B2 androidTest证据分支，App/JVM输入一致；后又只增强新11项回调外断言，154组件回归PASS、方法身份及APK输入不变，分别保留不重复计数；旧测试断言保留。
- App SHA `b7c90277fa6c27e6ab314aa52c293de8e445ba6a3deba9e984ab96f4280e8a4c`；Test SHA `bb00c0f6e34059170d20124eab739b43ccbc257ecc265def28906077fc0cb78c`，仅忽略构建目录保留，未安装/发布。旧锁定APK原件保持。
- 交接只读入口：[集中报告与下一轮准确授权范围](../research/M10_B1_RECOVERY_B2_RESOURCE_CANDIDATE.md)、[结果索引](../research/m10-b1b2-candidate/RESULTS.json)。等待ChatGPT独立审查；下一次新合成设备与准确故障方法/次数须另行批准，不复用旧现场。
- 本轮ADB/AVD/安装/运行/故障/下载0；H2 DELIVERY_UNKNOWN/pin=true、C2和保留API36仅继承历史，NOT_OBSERVED_THIS_TASK。M10 PENDING、三JPEG历史FAIL、UI-C1 FROZEN、M11 NOT_AUTHORIZED及D2A-N01～N05/A1-N01～N04和全部旧NOTE/FAIL/UNKNOWN/NOT_RUN不变。
