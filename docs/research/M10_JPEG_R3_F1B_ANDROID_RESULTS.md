# M10-R3-F1B — Android实际结果

2026-10-17。**BLOCKED — M10-R3-F1B Android Normal**。

安装前保守服务记录门禁停止。没有执行两项新APK验证，不能登记生命周期PASS。

| 检查 | 状态 | 真实依据/限制 |
|---|---|---|
| 唯一设备/AVD/API36/boot=1 | PASS | [设备身份](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/device-identity.json)、ADB原始读取 |
| 正式APK/UID | PASS | SHA `4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46`，UID10216，前后相同 |
| 研究已装身份/不同UID | PASS | 旧probe UID10218/SHA`230407d8bc98aeac6fcda632b935551e8ca3322cbdf55cb73baf8bc258cb47d0`；旧Outsider UID10219/SHA`214a6e98085d77d8fffb0c5be9f9aff70700c0f1e6365a3042b6ccf72385b358`，前后相同 |
| 当前进程采样 | PASS（仅观察） | 原Host3928、Outsider4062；没有观察到worker，不据此宣称旧消费者死亡/FD关闭 |
| 旧ServiceRecord是否安全闲置 | UNKNOWN | 记录存在但app=null/无目标Bindings段；仅凭名称不是活跃消费者证明，安全分类尚未完成 |
| 安装前安全门禁 | BLOCKED | 初始监督器对任何WorkerService记录采取保守拒绝；不是App运行FAIL |
| stage/current/pin/receipt/未知子项 | PASS（只读停止后snapshot） | 命名空间ABSENT；未认领/清理/创建目录，不是内核lease证明 |
| 锁定F1-A本地APK与27份输入 | PASS_HOST_ONLY | 字节SHA、签名、包名、Manifest/min26/target36/无权限/非导出Worker核对 |
| 当前已装研究APK签名连续性 | NOT_RUN | 门禁已停止；不得安装或卸载强行继续 |
| 两研究包保留数据更新 | NOT_RUN | 安装0次 |
| Outsider跨UID拒绝 | NOT_RUN | 启动0次，F0旧拒绝证据不能替代本轮 |
| Outsider销毁与ServiceConnectionLeaked消失 | NOT_RUN | 没有新APK/onDestroy结果或对应运行窗口 |
| normal App启动 | NOT_RUN | 启动0次/无本轮执行runId |
| normal Binder同步身份/owner隔离 | NOT_RUN | 无hello/Worker PID或新session/attempt |
| normal FD只读/SHA/offset/pin/GC保护 | NOT_RUN | 无FD交付，不用源码代替设备验证 |
| normal结构化SUCCESS/FD关闭 | NOT_RUN | 无新结果 |
| normal Worker退休/死亡 | NOT_RUN | 无新Worker，不能计“正常死亡PASS” |
| normal lease释放/stage收口/receipt | NOT_RUN | 无新stage，不能由ABSENT推定执行过清理 |
| normal业务准备资格/正式引用 | NOT_RUN | 本轮没有会话；研究APK本身无真实Room/媒体提交实现 |
| 正式私有元数据/设备设置保护 | PASS（所列项） | [保护比较](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/protection-comparison.json)；私人字节仍NOT_RUN |

**本轮没有Android场景PASS，也没有新APK场景FAIL。**实际54条ADB只读命令均退出0，preflight监督器因逻辑门禁退出2；不要混淆命令与案例。实际两次进程采样不能证明连续观察期间绝无进程。无新运行的Binder/FD/泄漏日志，相关原始运行证据为NOT_RUN，不编造。

[完整原始命令/时间/返回码](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/adb-commands.jsonl)、[逐阶段摘要](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/bounded-result.json)、[残留ServiceRecord](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/blocked-service-record-detail.txt)、[停止记录](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/blocked.json)、[读取后完成记录](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/finish-readonly.json)。

宿主检查器16项合成单测PASS，只验证snapshot的格式/归属/失败决策；[原始测试日志](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/host-validator-tests.log)与[源码/测试SHA](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/host-validator-tests.json)。不是16项Android验证。F1-A60项JVM/构建是已独立审阅历史，本轮未重跑Gradle/Lint/构建。

N01保留OPEN_NOT_TESTED；N02保留；N03实际系统复验仍NOT_RUN。原R3 29项0PASS/1UNKNOWN/28NOT_RUN不变；两次历史attach与R1 120秒UNKNOWN不关闭。M10三JPEG FAIL/完整生命周期BLOCKED保持，未授权的其他28项/取消/异常终止/恢复/native/allocator/M11全部未运行。
