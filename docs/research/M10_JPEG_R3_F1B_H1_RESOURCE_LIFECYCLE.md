# M10-R3-F1B-H1 连接、结果与资源生命周期

2026-10-17。只描述本次单一Outsider系统复验，不扩展到正常Worker消费者或native资源。

## 连接责任实际闭环

锁定F1-A代码在bind前将清理责任置REQUIRED，SecurityException保留为DENIED，finally对同一Activity-context连接执行unbind；onDestroy对已RELEASED连接幂等返回，不重新绑定。本轮系统deny指向PID5352/UID10219，两个生命周期结果均RELEASED，无意外清理错误或SAFETY_OUTSIDER_BOUND。

本轮日志中的两份Binding outcome DENIED及异常堆栈来自onCreate/onDestroy对同一结果的发布；系统只有一次跨UIDPermission Denial、只有一次启动。没有把拒绝日志抹去或将绑定改为成功。

App在两个阶段写同一个已知合成`files/result.json`，锁定代码在写后同步FD。新最终文件与onDestroy日志一致且不同于旧PID4062结果，证明本次销毁阶段结果可读取；不是断电持久化或结果文件中的runId身份协议证明。

## 日志窗口完整性

启动前后分别对main/system/crash/events执行完整`logcat -d -b <buffer> -v epoch`，没有尾部行数裁剪，没有清log或设置过滤器后假称完整。完整输出仅内存解析，要求退出0、stderr0、UTF-8/尾换行正常、全部行可识别；每buffer记录字节数、SHA及最早/最晚时间。

| Buffer | 结束完整输出字节 | 结束原始SHA-256 | 保留窗口依据 |
|---|---:|---|---|
| main | 9378144 | `44905b7e36376b392651bbbdee027102e45c5422d98b6371be9226bca7278cd2` | 最早1792223058.348，前后相同且早于启动 |
| system | 1590202 | `fd7cd165029f8af0c0b906dbfc5e0d9eb828675fc7209a54ec75725eb18b7566` | 最早1792223063.628，前后相同且早于启动 |
| crash | 0 | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` | 前后均空，正常完整返回 |
| events | 229978 | `715c34ed968ddc60c3ac5f38394fced04adf4ef11fbacb6c26f277cc1c1e6e60` | 最早1792223058.348，前后相同且早于启动 |

全部unparsedLines=0。非空buffer最早边界没有越过窗口开始，支持本次读取未丢掉启动至销毁后区间；crash前后为空。设备时间窗口开始/结束都由只读date获取，没有修改时钟。原始完整性见[动作回执](m10-jpeg-r3-f1b/evidence/h1-20261017-01/h1-actions-integrity.jsonl)，逐buffer摘要保存在同目录`before-outsider-buffer-*.json`、`after-outsider-buffer-*.json`。

归档仅保留目标PID的LITHIC_R3/ActivityThread/AndroidRuntime及研究组件相关系统事件。实际归档main52行、system4行、events9行，crash无内容。系统拒绝来自system_server PID762，没有错误地只用Outsider PID过滤。目标PID及相关事件中未见ServiceConnectionLeaked、越权绑定或Worker启动；没有将“缺日志”替代正向deny、销毁、结果及覆盖证据。

结论仅适用于本PID/此API36/该有界窗口，不承诺无限期无泄漏、其他Android/OEM相同，不能从归档SHA独立重建被隐私策略丢弃的无关日志。

## 结束资源状态与保留

- **连接**：实际RELEASED、系统拒绝、销毁后无对应泄漏记录；没有引入Application Context隐藏问题。
- **Activity**：onDestroy持久结果和系统销毁相符。
- **Outsider进程**：PID5352仍存在，系统记录冻结；不称进程死亡，不force-stop/kill。
- **Worker**：观察窗口与结束检查未见启动或绑定，ServiceRecord为app=null的闲置候选；从未执行normal，不声称Worker消费者、数据FD、lease或退休已通过。
- **研究stage**：ABSENT；未取得cleanup权限，也未删除旧owner/trace或未知残留。
- **正式数据**：无正式业务写入口调用，正式元数据/设置保护通过；业务行、账本及图片字节比较NOT_RUN，不把元数据相等升级为字节证明。

## 监督器限度

本轮新增脚本仅为限定H1取证，固定目录、不可重用marker、精确APK安装和单一Outsider启动，无normal/cancel/kill入口。未修改原G1/U1规则或研究APK。

轮询耗尽等负向分支未在本轮系统故障实验中测试。源码中该分支对旧结果的新鲜度还可进一步硬化；本次明确取得新PID、新SHA、当前onCreate/onDestroy及系统启动/销毁事件，未触发该风险。不将本监督器授予通用自动重跑安全资格，未来复用需单独评审，不能通过重复启动补证。
