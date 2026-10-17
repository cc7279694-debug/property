# M10-R3-F1B-H2 Worker资源生命周期

2026-10-17。**BLOCKED**，不是资源安全完成证明。

## 已执行链与未执行链

锁定代码期望stage→pin→bind→reserve/lease→hello/grant→FD→结果→认证退休→Binder/OS死亡→receipt→清理。本次仅完成合成stage/pin；bind await超时退出runCase。之后Worker迟到绑定，没有reserve/hello/start。

没有源FD交付、pread/写拒绝/EBADF、正常结果、消费者lease与活跃GC保护实验。pin=true不是内核lease证明；consumer.guard是归属inode，实际lease是抽象LocalServerSocket排他占有，不能称文件锁。死亡或静态文件不能补齐缺失步骤。

Worker6610为WORKER/syntheticOwner=false，与Host6423同UID10218、PID不同。它因UNRESERVED_DEADLINE自行退出，Binder和OS双证据匹配。这是未预留Worker的期限退出，不是Host认证退休。无外部ADB信号，不对任意native挂死、R1的120秒UNKNOWN或取消安全作结论。

## 保守保留的stage

[最终inventory](m10-jpeg-r3-f1b/evidence/h2-20261017-01/h2-final-files.json)实际显示：

- current.json存在，指向`586d0904-eaac-403a-895a-48c34cd98810`。
- owner为合成v2；nonce `80e16f4e-f17c-47a7-92ba-ce4ccaed751f`，generation `synthetic-generation-r3`，源预期SHA `3faac63d133ee546e983a131136bc44c9d3c0910d1c6b143d60509ef90a386e7`。
- stage设备65079/inode590815、guard inode590838、源inode590829/8192字节；为实际元数据与owner对应，源字节未被Worker校验。
- state为DELIVERY_UNKNOWN/pin=true。无output、receipt、revoked、cleanup或CLEANED_SYNTHETIC终结proof。
- 原校验器以expected_current=本attempt执行，返回BLOCKED、保留source；返回currentAttempt=null源于提前失败，不代表物理current不存在。

没有cleanup、recover、覆盖current、新建第二attempt或清未知文件。即使Worker退出，也不能把lease瞬时可得当安全收口。未来实验必须先另获授权处理危险残留，不能直接重跑normal。

## 日志与取证边界

启动前后完整读取main/system/crash/events，无清日志或tail裁剪。完整原文只在内存，保留退出码、字节/SHA及格式完整性。最初窗口尚无已知PID，派生未包含全部Host/Worker事件，原件保留。停止后从系统启动事件确定6423/6610，追加有界只读结束采集；最终窗口epoch1792229880.833067040～1792230127.953948353。

| buffer | 最终完整字节 | 原始SHA-256 |
|---|---:|---|
| main | 13733024 | `3451c07a0d6fa07b4054005f9a3e01d21099d67af6afc76e6f23d5b996227009` |
| system | 1847346 | `bb706cf656c73ce2f9fbcd46d8bb2646dbf03993ec01abd5a01c680c0e21cefa` |
| crash | 295 | `b9346e8853fff05dbdfa3846708a9b201cb048fe6ec8c3c9bf61f45c62d9e443` |
| events | 356967 | `704a8267d0473e77146ff466400a5a02016b6707683dae367249623a95bc78e5` |

全部unparsed0，最早时间早于窗口；crash事件早于本窗口、归档0条。只归档研究PID/组件与相关系统事件，没有私人账本/封面或无关应用日志。SHA不能独立重放被隐私丢弃的完整原文。末次Host-only/NO_TARGET_RECORD不能解决首轮process BLOCKED缺失原行。

## 两处收集异常

1. `collect_stopped.py`以exec-out传带引号sh脚本，设备返回inaccessible or not found，ADB仍退出0；本地格式检查明确UNKNOWN。原响应保留，不把退出0当成功读取。
2. `collect_final.py`取得真实FAIL报告，之后Host trace已从旧快照1171字节增长（迟到死亡回调），严格稳定性检查停止UNKNOWN。真实响应已保存于合成范围命令日志；离线无损重建并对照同次原始字节数/SHA完全匹配后另存，不覆盖旧异常。

没有为修取证而修改锁定APK或重发Intent。这些监督器不是已验证的通用运行工具，后续复用须独立审查。原错误、迟到行为与生产安全BLOCKED结论保留。
