# M22 最后一次 SystemUI ANR 专项取证

交付状态：**READY_FOR_REVIEW — M22 SYSTEMUI ROOT CAUSE**。实际执行：**RESOURCE_ADMISSION_BLOCKED**；M22产品状态仍为 **BLOCKED / CHANGES REQUIRED**。

来源：`4fa4be13b952914b552326a9eeb21320072d980c`，分支`codex/m22-experience-accessibility`，日期2026-10-23。本地与远端起点一致、工作区原先干净。本轮只允许一次全新隔离AVD取证；宿主可用物理内存至少5GiB、目标盘至少10GiB是启动前硬门槛，未满足即停止。

## 本轮实际准入

首次只读CIM观察：`FreePhysicalMemory=3341280 KiB`，约 **3.19GiB**，低于5GiB；该次未独立归档精确采样时间，不补写。已据此停止设备阶段，未关闭其他程序、等待资源后启动或降低Guest配置。

为归档可复核的命令、时间和原始字节，另保存一次只读宿主资源观察；它不是AVD或设备重试，也不撤销首次停止决定：

| 项目 | 实际归档值 | 门槛及判定 |
| --- | --- | --- |
| 采样时间 | `2026-10-23T08:22:21.6355924Z`（北京时间16:22:21） | 当时观察，不外推未来余量 |
| 可用物理内存 | `4045084 KiB / 4142166016 B`，约 **3.86GiB** | `5368709120 B / 5GiB`；**FAIL** |
| E盘可用空间 | `97941581824 B`，约 **91.22GiB** | `10737418240 B / 10GiB`；**PASS** |
| 资源命令 | exit **0**；stdout **293 B**；stderr **0 B** | 只读Windows CIM，无SDK或ADB命令 |

原始[stdout](m22-systemui-rootcause-20261023-082220/stdout)、[stderr](m22-systemui-rootcause-20261023-082220/stderr)及[完整命令、起止时间、退出码与SHA-256](m22-systemui-rootcause-20261023-082220/command.json)。stdout SHA-256：`7e1d9ea93938e58fd28fe9e5b55a367904d75bfd7944a9a6699c91988aa65a8d`。单位明确区分CIM的KiB与字节。

## 线程栈及原因结论

| 层级 | 结论与证据 |
| --- | --- |
| 已证实的历史阻塞 | 5712第二轮窗口为SystemUI ANR；5714未安装砺石App/Test时，`am_anr`记录PID1073的`SystemUIService`执行等待20001ms。只引用原件，不称本轮复现。[5712窗口](m22-supplement/commands/health-2-window/stdout)、[5714原始事件](m22-clean-20261023-1523/systemui-anr-events.json)、[5714健康门禁](m22-clean-20261023-1523/systemui-health-gate.json)。 |
| 本轮直接线程栈 | **TRACE_NOT_AVAILABLE**：资源准入失败，未启动设备、未尝试读取`/data/anr`或执行`adb root`。不是Android权限拒绝、trace文件不存在或新设备没有ANR的证明。 |
| 可能的等待链 | **UNKNOWN**。既有服务超时事件不是主线程栈；不能区分锁、Binder、初始化、渲染、调度或I/O阻塞。5714的`virtio: zero sized buffers are not allowed`无时间戳/设备类型/栈，不能关联到ANR因果。[历史stderr](m22-clean-20261023-1523/emulator.stderr)。 |
| 同期资源 | 本轮只有上述宿主资源观察，没有新ANR时刻、trace生成时刻、SystemUI线程CPU/内存、Guest PSI、I/O或Binder等待样本；相关时间差均 **NOT_CAPTURED**。本轮内存准入失败不能反推历史5714 ANR由内存不足导致。 |
| 根因与修复依据 | 深层根因继续 **UNKNOWN**；没有新增直接证据支持更换GPU、调整内存/CPU、升级镜像或其他环境修复。未执行任何修复。**NO_ANR_OBSERVED不适用**，因为本轮设备未启动。 |

既有共同API36 Google APIs x86_64 r7、Emulator37.1.11、2560MiB/2核/SwiftShader/WHPX仅继承[5714启动日志](m22-clean-20261023-1523/emulator.stdout)与[运行配置证明](m22-clean-20261023-1523/runtime-path-proof.json)；本轮工具/镜像实物身份及整镜像哈希未重新核验，**NOT_RUN**。不把历史元数据SHA写成整镜像字节已验证。

## 操作次数与保护

| 操作 | 本轮次数/状态 |
| --- | --- |
| AVD创建 / 冷启动 / 正常退出 | **0 / 0 / 0**；未创建设备，退出不适用 |
| ADB / adb root / APK安装 / Instrumentation | **0 / 0 / 0 / 0** |
| SDK下载升级 / 构建 / JVM及Android测试 | **0 / 0 / 0** |
| 旧设备访问、修改、清理 / 系统设置修改 / 进程终止 | **全部0** |

没有新AVD名称、serial、Boot ID、SystemUI PID、设备截图或ANR trace，不能编造这些身份或退出证明。没有建立业务Owner、generation、任务或通知。5712、5714、5704、H2、C2、M10及其他历史现场未访问或改动；旧锁文件、截图和原始失败保持。没有全机bugreport、ZIP、APK或大文件归档。

原137 JVM PASS、Android最新36 PASS / 1 FAIL / 2 NOT_RUN、T06历史三次FAIL、T03及真实IME未执行边界全部保持；M10延期/三JPEG历史FAIL、M13-T06延期、M20平台FAIL/收口UNKNOWN、Room5/backup4、Q03/Q04及G1/G2/G3等状态不变。

本轮唯一执行阻塞是**宿主可用物理内存未达到用户规定的5GiB**，不是新的SystemUI根因结论。提交推送后停止等待ChatGPT独立审查；未消费创建/启动次数不代表允许以后自动重试。后续设备验证须再次经独立审查及用户授权，不冻结M22、不合并main、不进入M23。
