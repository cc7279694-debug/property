# M22 真机独立安装与定向补验

**READY_FOR_REVIEW — M22 PHYSICAL ISOLATED EXECUTION**（仅交付复验，不代表执行通过）。本轮 **BLOCKED**；M22继续 **BLOCKED / CHANGES REQUIRED**。

来源`0f8297998160479d8c4ef4b6f791f4fb2076091f`，分支`codex/m22-experience-accessibility`，2026-10-23；开始时本地/远端一致、工作区干净。只有原Xiaomi `23127PN0CC` / API36 / arm64-v8a USB手机；完整serial仅保留在被Git忽略的本地控制记录，公开证据使用SHA-256 `e4f0e1d2361b0ef560ff8ac99a067fb8f540594c827d28b6cd21a720e724af92`。[唯一目标发现](m22-physical-isolated-20261023-104335/usb-discovery.json)、[逐项结果和操作数](m22-physical-isolated-20261023-104335/results.json)。

## 安装准入与实际身份

Boot ID为`a9edfd47-8cb5-40a5-92eb-c41925ab00ea`。原M10为User0/code8/`0.1.0-m10-debug`/UID10376；新App/Test含保留记录均不存在。手机可用RAM约4.61GiB、/data约164.21GiB、电量100%、电池温度32.7℃；达到本轮自选的2GiB可用RAM、2GiB磁盘、30%电量保守准入线，该线不是产品资源硬上限。[实际准入](m22-physical-isolated-20261023-104335/before-admission.json)、[资源和原设置原文](m22-physical-isolated-20261023-104335/commands/before-device-resources-settings/stdout)。

两份本地锁定APK实际重新计算SHA，并以原工具验签及读取二进制Manifest；均匹配原构建，无替换或重建。证书SHA均为`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。[App验签](m22-physical-isolated-20261023-104335/commands/before-app-signature/stdout)、[Test验签](m22-physical-isolated-20261023-104335/commands/before-test-signature/stdout)、[App Manifest](m22-physical-isolated-20261023-104335/commands/before-app-manifest/stdout)、[Test Manifest](m22-physical-isolated-20261023-104335/commands/before-test-manifest/stdout)。

| 包 | 唯一安装尝试及真实结果 |
| --- | --- |
| 原`com.cooper.lithic.debug` | 本轮未执行安装、更新、启动或读取私有文件；前后版本/UID/installed状态一致。没有读取旧APK或账本，不能由安装元数据一致推断所有私人字节已核验。 |
| 新App `com.cooper.lithic.m22verify.debug` | **1次，PASS**。无覆盖/降级参数，实际UID **10420≠10376**；code17/`0.1.0-m22-debug`。设备安装字节SHA **`b749b771e4f6f9cebcb7355c95834b7ae2eb4ba4f7528536a92bf9e09eefb2d3`**，与新验签实物一致，因此签名身份沿字节等同核验；未另行提取手机证书。`run-as`实际UID10420、canonical私有路径`/data/user/0/com.cooper.lithic.m22verify.debug`；Provider登记为新包`.androidx-startup`，静态Manifest无sharedUserId。 |
| 新Test `com.cooper.lithic.m22verify.debug.test` | **1次，FAIL**。系统exit1：**`INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`**。具体是人工操作还是系统策略触发 **UNKNOWN**，不能只据错误文案归责用户。锁定本地SHA **`c3791305aed28899f8df98dc7c89f27c986c022b84a0e528c68c59ebf9f2037f`**；结束包管理器无安装/保留记录，安装UID/设备SHA **N/A（未安装）**。未重试、卸载、回滚或替换。 |

[App安装claim](m22-physical-isolated-20261023-104335/app-install-claim.json)、[App安装原始成功](m22-physical-isolated-20261023-104335/commands/install-app-once/stdout)、[实际UID](m22-physical-isolated-20261023-104335/commands/after-app-package/stdout)、[设备APK SHA](m22-physical-isolated-20261023-104335/commands/after-app-apk-sha/stdout)、[私有路径/UID](m22-physical-isolated-20261023-104335/commands/after-app-private-baseline/stdout)、[Provider](m22-physical-isolated-20261023-104335/commands/after-app-provider/stdout)、[Test安装claim](m22-physical-isolated-20261023-104335/test-install-claim.json)、[Test原始失败](m22-physical-isolated-20261023-104335/commands/install-test-once/stderr)、[真实exit1及时间](m22-physical-isolated-20261023-104335/commands/install-test-once/command.json)、[结束新包记录](m22-physical-isolated-20261023-104335/commands/end-new-package-records/stdout)。

## 三方法与用户补充决定

执行前源码审查确认，锁定`CompleteJourneyTest`的`@After`无论成功失败都会关闭Owner后删除自己的合成账本根；失败诊断仅保留阶段、截图/语义树及六表计数，不保留完整业务事实。用户针对这一冲突明确选择：**“暂停T06，先保留严格现场要求”**。本轮没有修改源码或重建不同Test来绕过该决定。

| 方法 | 本轮实际结果 |
| --- | --- |
| T06 `CompleteJourneyTest#runtime_offline_journey` | **NOT_RUN**；用户明确暂停，随后Test安装亦失败。原三次FAIL保持，没有本轮JUnit或业务事实断言结果。 |
| T03 `LongHistoryPerformanceTest#full_twenty_year_dataset_reads_exact_global_facts_and_remains_responsive` | **NOT_RUN**；T06 PASS/Owner收口前置未满足。1000/1000对象、20000/10000事实、原日期/seed及120秒预算未改；Room计数、性能/内存/响应 **NOT_CAPTURED**。 |
| 真实200%字体/IME方法 | **NOT_RUN**；Test未安装、未启动任何目标Activity。实际Activity screenWidthDp **NOT_CAPTURED**；物理1200×2670/density480不替代实际宽度证明。未修改density、字体、断言，也未运行其他方法补数。 |

Instrumentation与App启动命令均 **0**；没有测试截图、JUnit stdout/stderr或运行期性能日志，不能伪造这些未执行证据。原137 JVM PASS/36 Android PASS及全部历史FAIL/UNKNOWN/NOT_RUN不重跑、不改判。

## 后续默认存储变化与安全停止

| UTC时刻 | 实际观察与边界 |
| --- | --- |
| `10:49:22.395` | 新App自己的有界文件采样（深度4）未返回文件；不是无限深度或未来状态证明。[采样](m22-physical-isolated-20261023-104335/commands/end-app-private-baseline/stdout)。 |
| `10:49:22.556～22.812` | 新App `pidof`为空/exit1；JobScheduler内存过滤无新包或UID真实JOB记录。此时样本保留，但不能外推后续始终无进程/Job。[进程](m22-physical-isolated-20261023-104335/commands/end-app-process/command.json)、[目标Job样本](m22-physical-isolated-20261023-104335/end-target-jobs.json)。 |
| **`10:51:55.998～56.540`** | 更晚的结束目录核对发现`no_backup/active-generation`、`generations/919ab3ee-2a7e-4988-8d65-5641a7ddcf2b/`中的Room/settings/ready文件名、WorkManager数据库及`files/profileInstalled`。**监督器的“应不存在存储”断言FAIL**，远端只读命令自身exit0，二者不混淆。[原始目录元数据](m22-physical-isolated-20261023-104335/commands/end-new-app-store-absence/stdout)、[命令/时间/SHA](m22-physical-isolated-20261023-104335/commands/end-new-app-store-absence/command.json)。 |

没有读取上述数据库、设置、ready或active-generation指针内容；目录UUID不冒充已验证的当前指针目标，数据量和完整性未确认。**本轮没有发出App/Instrumentation启动命令。用户随后明确回复：“是，我打开过新M22 App”**，手动打开行为据直接用户陈述登记；其准确时刻、启动调用链及是否产生具体后台任务未取得直接记录，不据此推断全部存储/任务归属已核实。发现变化后停止所有设备操作，不读取账本、不取消任务、不强停、不删除文件或卸载新App；收到回复后也没有追加设备操作。没有在此后重新核验进程/Job；**最终Owner关闭、目标Job、进程及FD/锁状态UNKNOWN**，不能继续用前一个零记录样本称最终收口PASS。

## 设置、保护与剩余阻塞

本轮没有要求切换网络或字体，也没有任何设置写入。已记录的前后值一致：font1.1、density480/1200×2670、airplane0/Wi-Fi1/mobile_data0、主题设置值0及同一Boot ID；无需执行本轮设置恢复。这是记录时刻的核对，不是新存储出现之后再次采样。[结束设置](m22-physical-isolated-20261023-104335/commands/end-device-settings/stdout)、[原M10结束身份](m22-physical-isolated-20261023-104335/commands/end-m10-install-identity/stdout)、[App结束安装SHA](m22-physical-isolated-20261023-104335/commands/end-app-apk-sha/stdout)。

实际ADB发现1次、明确指定同一USB目标的命令24次（22只读＋2唯一安装尝试）；本地验签/Manifest命令4次。旧M10私有数据读写、App启动/Instrumentation、卸载/清数据、root、手机重启、系统设置写入、AVD/其他设备操作和资源清理均 **0**。没有采集个人屏幕、通知、相册、账号；全局Job输出只在内存处理，归档目标筛选结果和整体长度/SHA，不归档其他应用任务。两份新包没有自行卸载：App仍保留；Test未安装；新App默认存储及未知任务现场保留。

当前BLOCKING分别是：**用户要求先修正T06失败保留契约；Test唯一安装失败；新App默认generation/WorkManager出现后的Owner/任务收口UNKNOWN**。用户手动打开的补充确认不关闭最后一项。这些不是新的JPEG/金额算法失败，也不能用已安装App证明M22通过。手机的新M22安装/默认存储须保守保留，未经新授权不得再次测试、启动、收口或清理。本轮仅提交报告与必要原始证据，不修改产品/测试/Room5/backup4/算法，不触碰H2/C2/M10等历史现场。M10/M13-T06延期、M20平台FAIL/收口UNKNOWN及Q03/Q04/G1/G2/G3等历史门禁全部保持。

提交推送后停止，等待ChatGPT独立审查。后续任何设备处理、Test重装或测试包整改/重建需要明确授权；不冻结M22、不合并main、不进入M23。
