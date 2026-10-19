# M10 Android 最终集中验收批次

**BLOCKED — CONTINUOUS_LOG_CAPTURE_READINESS_UNKNOWN**。来源 `946ac16c217cb1d96cde77558aacb9802bcd5c4f`；分支 `codex/m10-jpeg-android-final-campaign`；新批次 **`m10-final-20261019-005717-97808a30`**。环境恢复成功，首项在日志就绪门禁停止，未执行任何JUnit方法。没有改写旧PASS、UNKNOWN或FAIL。

## 真实环境与操作

复用专用 `Lithic_M10V_API26_20261018 / emulator-5624`。注册/config与官方原始输入字节一致；独立磁盘无链接/越界，3份qcow2的实际backing路径均在本专用数据目录。原目录与所有旧marker保留。**创建0、受控冷启动1、安装0、instrumentation0、正常退出0**。锁定两包直接复用，实际UID为10077/10078。

本次新launcher **16720** / QEMU **26072**、创建时间、父子关系、完整真实路径/参数和5624/5625归属经双观察核验；实际boot=1、API26、x86_64及AVD名匹配。旧PID和旧boot/exit证明不用于新准入。

初次Host监督因审计记录缺QEMU签名字段中断；随后补取真实Google有效签名并核对SHA，独立确认**同一次启动**的进程和系统状态，没有再次启动或将原中断改为PASS。首次300秒启动时限 **NOT_PROVEN**，新的steady-state证明只报告实际观察。另修正`pm list packages`对子串筛选会同时返回App与其测试包的旧监督假设；只接受这两个确切锁定包名，再分别验证真实APK字节和UID，未知名字仍拒绝。原始中断记录保留。

App SHA `0ada263192dbaa1256bff6c613e6d57a69e1160b17d4844d1e1cfcc9cf847dbf`；测试APK SHA `3b0005a31afa753e9a9709323572250c0c5c63153c68ca1f882755c98724282a`。实际证书、Manifest、非导出Worker、instrumentation、四ABI及内置锁定JPEG样本通过静态核对；未重新构建或替换。此处不宣称四ABI均已运行。

## 新批次九项状态

| 项目 | 批次门禁 | JUnit |
|---|---|---|
| 1 健康JPEG Worker完整退休 | **BLOCKED：采集就绪UNKNOWN** | NOT_RUN |
| 2 缺损JPEG拒绝及完整退休 | NOT_RUN，前置阻塞 | NOT_RUN |
| 3 健康JPEG正式导入 | NOT_RUN | NOT_RUN |
| 4 原始缺损完整性断言 | NOT_RUN | NOT_RUN |
| 5 项目旧封面/金额保护 | NOT_RUN | NOT_RUN |
| 6 订阅旧封面/付款/状态保护 | NOT_RUN | NOT_RUN |
| 7 PNG原功能 | NOT_RUN | NOT_RUN |
| 8 旧v1未提交JPEG兼容 | NOT_RUN | NOT_RUN |
| 9 旧v1已提交封面读取/保护 | NOT_RUN | NOT_RUN |

首项的before已证明旧两份合成Stage终结、归属、无活动消费者；其后启动了固定serial的连续logcat observer。**10秒内stdout=0字节、stderr=0字节，ready=null**，因此采集器返回UNKNOWN。监督器没有写入测试attempt marker、没有发送`am instrument`、没有创建新Stage，也没有自动重试或继续第二项。退出的是该采集器自有**Host adb reader**，不是Android App/Worker。

**已确认的唯一执行阻塞**：连续采集器需要先看到main header才能判为就绪，但本次过滤流完全无输出；有进程存活不能代替就绪证据。冷启动后暂无匹配tag事件、header被过滤流延迟输出是证据支持的假设，未通过注入日志或改变过滤器验证，不冒称确定根因。后续可独立审查带唯一nonce的监督诊断哨兵或其他可验证就绪握手；不能伪造六阶段日志。本轮未实现或执行该补强。

## 结束状态与证据

结束只读检查 **PASS**：旧2个Stage及4份观察与本批次前置快照完全相同，无活动App/Worker。**API26保持运行，未退出/清场；API36创建/启动/安装/测试全部0、NOT_RUN**。主journal有360次定向只读ADB（exit均0），另1次连续logcat reader，共361次；命令exit0不替代任何验收。设备操作未涉及H2、C2、其他历史设备或私人账本/图片；私人字节完整性NOT_RUN。

只运行必要的13项Host接缝/退出守卫测试，13PASS，未重跑数百项历史测试或native压测；原RED保留。必要代码仅为新campaign身份/一次性marker/冷启动准入、当前进程anchor和新批次退出凭据接缝，未改连续采集器、正式App、Room、备份、算法、UI或原三JPEG断言。

[批次结果及计数](m10-jpeg-android-final-campaign/evidence/m10-final-20261019-005717-97808a30/RESULTS.json)、[关键原字节索引](m10-jpeg-android-final-campaign/evidence/m10-final-20261019-005717-97808a30/INDEX.json)、[首项阻塞](m10-jpeg-android-final-campaign/evidence/m10-final-20261019-005717-97808a30/api26/method-00/result.json)、[采集器原证明](m10-jpeg-android-final-campaign/evidence/m10-final-20261019-005717-97808a30/api26/method-00/continuous-log/capture.json)、[结束只读状态](m10-jpeg-android-final-campaign/evidence/m10-final-20261019-005717-97808a30/api26/ending-readonly.json)。原始命令、磁盘引用、实际APK/签名审计、冷启动及Host测试证据均在该索引，不含APK、VM磁盘或SDK许可正文。

M10 **PENDING**，原健康限定补验PASS、原缺损UNKNOWN及三个历史JPEG FAIL保持；UI-C1 **FROZEN**，M11 **NOT_AUTHORIZED**。H2 DELIVERY_UNKNOWN/pin=true、C2与全部历史NOTE/UNKNOWN/NOT_RUN不变。只提交推送本批次分支后停止，等待ChatGPT集中独立验收。
