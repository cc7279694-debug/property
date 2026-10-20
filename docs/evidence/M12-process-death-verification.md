# M12-T04 / Q06：三个真实进程终止点

**READY_FOR_REVIEW；M12未冻结。** 来源 `b5691419ad8f113b837a4cb16e4c1e47227e2752`，分支 `codex/m12-process-death-validation`。本轮只执行用户明确授权的三个自身终止点和首次独立重启核验，不重跑此前96 JVM / 74 Android、M10或M11。

## 结果

| 持久化检查点 | 实际终止进程 → 核验进程 | 重启后的 active generation | 实际结果 |
|---|---|---|---|
| BEFORE_SWITCH | 9900 / startTicks1969997 → 10092 | 旧A `05677e90-4ce3-4d4b-8a22-8ee25a56f470` | **PASS**：完整旧集，新候选未生效 |
| SWITCHING | 10360 → 10534 | 新B `06191838-8999-481a-a5c0-fabadc1712e8` | **PASS**：完整新集，未混入旧事实 |
| STABLE | 10799 → 10975 | 新B `34e1b4cd-618c-4457-8ff0-64ddd5164e2d` | **PASS**：完整新集，没有回到过时旧集 |

三个案例各仅调用一次 `Process.killProcess(Process.myPid())`，共3次；终止前由宿主实际核对PID、UID10216、`/proc`启动身份、cmdline、boot及APK身份，再发布绑定owner/检查点SHA的独占许可。落盘kill-intent之后自身退出；Android原始输出均为预期`Process crashed`，**这些中断调用不计JUnit PASS**。旧PID实际消失后，三个新的Instrumentation分别首次启动并各取得 **1项JUnit PASS**，再分别第二次关闭/重开选定generation。两种新JUnit方法以三个不同case调用，不能计为6个唯一PASS方法。

每个案例在终止前将独立A/B预期快照写入并同步私有文件，重启核验从原文件读取，不重新生成期望值。七表全部非空，A/B项目初始投入分别100000/230000分、记录时区UTC/Asia/Shanghai、对象/操作身份及JPEG/PNG字节均不同。对**重启选定的active代**，七表与设置逐字段精确相等，媒体与冻结副本原字节相等，SQLite完整性ok、外键0错，二次打开仍完全一致。未把非选中代的ready/文件/媒体检查冒称两代全部数据库均逐字段重读。

## 隔离与实际变更

- 原合成API36 `Lithic_M11B_Final_API36_20261020 / emulator-5686`，launcher1284/QEMU29348及创建时间、端口、磁盘、boot `837312dd-125c-41f8-92a9-eb2d96e7795e`前后一致。AVD创建/启动/停止 **0/0/0**，App安装0，Test同签名更新1；结束时无活动目标App进程，模拟器保持在线。
- 生产App始终为 **12,910,837B**，SHA256 **`3bb0d801300889d5c97f85dd768a14701f3a3f67aacd55dca929aa86e8233107`**。未重建或安装App，`app/src/main`、Room schema、金额/订阅、备份format1、UI及原断言无变更。未访问H2、C2、M10或私人数据。
- 仅新增[专用测试Runner](../../app/src/androidTest/java/com/cooper/lithic/data/backup/M12ProcessDeathRunner.kt)、[测试](../../app/src/androidTest/java/com/cooper/lithic/data/backup/M12ProcessDeathTest.kt)及[限定监督器](../../scripts/m12_process_death.py)。`-Pm12T04ProcessDeath=true`只选择Test构建的Runner，默认构建仍使用原Runner；生产依赖、权限和签名不变。专用Runner替换为普通Application，避免打开/清理原默认账本。STABLE例在原`activateRestore()`返回且指针回读后终止，没有新增生产检查点。
- 每例独占 `no_backup/m12-t04-947977ca-8d20-4c60-97f0-86899be58a45/<case>/`；全部案例及预期文件保留。默认两代、M11/M12既有证据树与active指针前后哈希/身份清单一致；原指针仍为 `b3f76bf5-27f9-4cc4-b39c-d6feea2f2457` STABLE、previous `5886ffc8-317c-4342-a5fe-1ac5f7f85767`。没有手工删库/删代、清数据、force-stop、任意PID终止或模拟器重启。

## 构建、取证限制与历史保留

最终Test **2,444,799B**，SHA256 **`e23be35a8f5b11b8dcf93f10ac3c24103d48736c3a96cef9bb61f0de4ae8e91f`**；与三例实际安装/参数身份一致，证书为原`29ded26b…`，包`com.cooper.lithic.debug.test`，专用Instrumentation目标为原App。原App与当前Test实物保留在本地构建目录，不提交APK。离线Test构建/Lint成功 **1m1s**，Lint **0错误/9警告**；未执行无关全量测试。[结果、输入SHA与状态](m12-process-death/results.json)，[最终构建](m12-process-death/runner-test-build.log)，[签名/清单](m12-process-death/apk/new-test-manifest.stdout)。

取证过程的已知限制均保留：AVD名字CRCRLF和安装`Performing Streamed Install`前缀曾被过严解析拒绝，后据同一原始字节核实，未重复创建/安装；AGP曾覆写清单Runner，安装前已拦截，改用Test专用构建属性后才安装。**第一例**许可的shell回读由LF变为CRLF，监督器在自身终止后中断；原许可实际485B、设备SHA与随后`exec-out`原字节严格一致（`a48ec5b0…`），没有重发终止，首次独立重启核验随后通过。该例Windows外层ADB客户端exit未取得，明确为NOT_CAPTURED；Android中断输出、落盘intent、独立PID消失和完整重启核验均保留。不得将这个外层exit字段写成0或旧观察写成PASS。[原始差异及精确原字节证明](m12-process-death/BEFORE_SWITCH/permit-readback-analysis.json)。后两例直接使用原始字节通道，外层exit均实际取得。

三个真实安全门禁内**未发现数据混合、旧集被意外激活或默认账本变化**。本轮指定的M12-T04/Q06进程终止子门禁可提交独立冻结审查；不等于整个Q06全部故障矩阵关闭。坏新代后再次终止、旧代清理过程中终止、真实断电/LMK/ENOSPC、API26、真机/OEM和Release仍NOT_RUN；此前受控回退与未知残留保护证据继续有效，不冒充这些未运行路径。本轮不再追加故障场景，**等待ChatGPT独立审查**。

M10 PENDING / DEFERRED、三项历史JPEG FAIL、所有旧UNKNOWN/NOT_RUN/NOTES、UI-C1冻结均不改；M13未授权。
