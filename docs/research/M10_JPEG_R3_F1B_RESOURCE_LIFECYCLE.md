# M10-R3-F1B — 资源所有权与生命周期

2026-10-17。**BLOCKED — M10-R3-F1B Android Normal**。本轮没有启动消费者；以下严格区分已有残留观察与未执行的生命周期验证。

## 已有资源观察

| 事实 | 证据 | 能说明/不能说明 |
|---|---|---|
| Host PID3928/UID10218、Outsider PID4062/UID10219 | 前后两次只读ps | 既有研究进程保留；不是新的启动、退休或死亡证据 |
| WorkerService记录仍在User0 active services段 | 目标ServiceRecord17f772c | 系统目录中有记录；不能仅由段名断言存在运行中的Worker |
| 目标记录app=null、未显示Bindings段 | 停止后受限dumpsys目标段 | 没有匹配活动app记录/可见Bindings；不能证明所有排队/消费者状态已经安全结束 |
| recentCallingUid10219，c:Outsider | 同上 | 与旧Outsider请求一致；记录与F0拒绝/泄漏的因果仅为推测，不是绑定成功或泄漏仍存在的证明 |
| stage namespace ABSENT | 停止后完整research files清单 | 未发现stage/current/owner/pin/receipt/guard；没有调用清理函数，也没有新任务产生 |
| files下仅合成marker和旧Host trace | 只读stat清单 | marker0字节、trace218字节；未读取/伪造新会话结果，无源/输出提交 |

[初始进程](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/preflight-processes.json) · [结束进程](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/after-processes.json) · [目标服务记录](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/blocked-service-record-detail.txt) · [research文件清单](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/after-files.json) · [snapshot核对](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/after-stage-check.json)。stage数据仅来自停止后只读采集，不能当作原preflight已完成的项目。

本轮没有尝试取得LocalServerSocket lease，不使用canAcquireGuard瞬时成功、旧PID或mtime推断可清理。没有unbind/kill/recover/cleanup，没有删或换代，也没有为修复Outsider而释放旧进程。

## 本轮生命周期结果

- 新Host/Worker启动与UID隔离：NOT_RUN。
- 新session/attempt/generation排他归属、pin在FD前持久：NOT_RUN。
- 只读FD写失败、8192原字节SHA、共享offset修正、GC拒绝活跃consumer：NOT_RUN。
- 结构化SUCCESS、数据FD EBADF关闭、结果后lease仍持有：NOT_RUN。
- 请求退休、匹配Binder死亡、OS死亡、socket释放与无新活跃消费者：NOT_RUN。
- receipt身份、退役标志、完整terminal proof、安全删除源/输出与重复恢复：NOT_RUN。
- 真实Room/正式封面引用：没有新会话，也没有业务实现接入；不能登记本轮业务提交资格PASS。

这些层级均没有执行实证。预先存在的stage缺失不能替代“运行过并清理成功”；没有观察到Worker进程不能替代其死亡/FD关闭。正常合成Java任务本轮也未跑，更不能外推R1 native挂死或任意native进程安全。

## 数据与设备保护

54次ADB均为读取、返回0，未安装/启动/停止/信号注入/清数据/重启/设置写入；[原命令](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/adb-commands.jsonl)和[摘要](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/bounded-result.json)可核对。正式包UID10216和锁定APK SHA前后一致，188条私有metadata聚合一致；settings7项加auto_time/auto_time_zone/timezone一致。研究已装旧APK/UID和两个既有PID保留。

[保护比较](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/protection-comparison.json)只支持所列APK、UID、metadata和settings。未读取账本/图片内容，私人字节完整性NOT_RUN。没有将数据库、图片、签名私钥、APK或未知二进制提交Git。

## 已知约束继承

N01 REVOKED授权语义未修、未测；N02半清理保守阻塞未被绕过；N03资源释放Android复验仍未运行。R3原29计数不变，完整生命周期BLOCKED，B1生产NO-GO。M10 PENDING/三JPEG FAIL，UI-C1冻结PASS WITH NOTES，M11未授权。没有授予取消、故障终止、恢复、JNI/allocator或生产接入权限。
