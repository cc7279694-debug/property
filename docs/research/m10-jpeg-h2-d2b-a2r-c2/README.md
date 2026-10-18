# A2-R-C2 — 关键证据索引

**BLOCKED / API26_BOOT_TIMEOUT**。来源C1 `797eb591cc8293d9d68503243d2a94462fe79610`；分支`research/m10-jpeg-h2-d2b-a2r-c2-isolated`。完整范围、结果矩阵和风险见[执行报告](../M10_JPEG_H2_D2B_A2R_C2_EXECUTION.md)，[checkpoint](../../checkpoints/2026-10-18-M10-JPEG-H2-D2B-A2R-C2.md)。

| 证据 | 用途 |
|---|---|
| [SCOPE](SCOPE.md) | 本轮一次性授权边界 |
| [APK审核](evidence/apk-audit/result.json)、[原始命令](evidence/apk-audit/commands.json) | 原A1 APK宿主实物/签名/Manifest/48输入核验，不是设备安装 |
| [最终宿主guard测试](evidence/static-tests-execution-inputs/result.json)、[输出](evidence/static-tests-execution-inputs/stderr.log) | 6唯一方法PASS，精确执行输入；早期两份通过记录保留，不重复累计 |
| [前置门禁](evidence/preflight-20261018T041741.206888Z/result.json)、[输入SHA](evidence/preflight-20261018T041741.206888Z/locked-input-hashes.json) | 11镜像原件＋536工具、隔离端口/目录及环境；未执行设备 |
| [唯一创建结果](evidence/create-20261018T041949.674262Z/result.json)、[创建命令](evidence/create-20261018T041949.674262Z/002-command.json) | avdmanager exit0，1次新AVD、配置/路径/镜像身份 |
| [唯一发现命令](evidence/start-20261018T042136.600903Z/003-command.json)、[保存行](evidence/start-20261018T042136.600903Z/002-single-global-device-discovery.stdout.log) | 全局发现1次，原始元数据及隐私过滤标记 |
| [启动门禁失败](evidence/start-20261018T042136.600903Z/failure.json) | boot deadline expired，不能继续安装或Host启动 |
| [结束保护](evidence/closing-20261018T042703.129674Z/result.json)、[C2状态记录](evidence/closing-20261018T042703.129674Z/c2-state-records.json) | 实际操作1/1/0/0、旧A2-R保护、环境/原APK/所选镜像核查；结束API26且boot仍为空 |
| [native日志快照](evidence/closing-20261018T042703.129674Z/emulator.stdout.snapshot.log) | 结束有界前缀6158字节，活跃原日志只在本地，可能继续增长 |
| [结束宿主进程/配置](evidence/host-ending-20261018T042852.720178Z/result.json) | 只读宿主观察；不刷新启动许可 |
| [完整运行命令索引](evidence/command-index.json) | 133命令，127ADB；唯一全局＋其余126全部定向新serial，0非零命令；监督器仍因boot门禁退出1 |
| [提交范围与完整性核查](evidence/delivery-verification.json) | 当前新增证据/原输出哈希、链接、敏感内容、许可正文及原始源码保护；结果自身不在内容扫描时点 |

监督脚本只属于C2，固定新身份和单次marker，不能作为后续自动重试许可。`observe`本轮未调用；没有原始Host trace/UID/READY可供展示，相关项按UNKNOWN/NOT_RUN登记，不制造合成实机成功。

目录不含SDK、系统镜像、userdata、APK或许可正文；索引里的原始路径和SHA仅为来源引用。原API36/H2未访问，全部历史失败和NOTE保持。提交后停止等待ChatGPT独立验收。
