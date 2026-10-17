# M10-R3-F1B-G2 执行报告

日期：2026-10-17（Asia/Shanghai）。**BLOCKED — M10-R3-F1B-G2**。

唯一一次锁定G1预检在第一条设备在线查询处停止：授权串号`emulator-5596`返回`device not found`。随后仅执行一次必要只读结束核查，同样停止。没有取得ServiceRecord、应用身份、stage或两轮快照，不把环境阻塞写成服务分类失败。

## 契约与来源

- Goal：在原`Lithic_M01_API_36 / emulator-5596`上运行G1实际集成只读preflight，验证真实双快照；任何阻塞后只允许必要结束保护。
- Scope：原G1入口一次执行、必要只读结束采集、新G2证据、报告、唯一CURRENT_STATE与checkpoint；只提交推送G2分支。
- Out of scope：APK安装/启动、Outsider/normal、解绑/清理/信号、设备启动/重启/wipe、改分类规则、生产源码/原断言、JNI、allocator、29场景、M11。
- 仓库`cc7279694-debug/property`；锁定来源`7ff737ecd54746aaed580a15fac92126009d5c44`，来源分支`research/m10-jpeg-strict-r3-f1b-g1`。来源本地/远端一致且干净，新G2分支开始时不存在。
- 独立worktree：`E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1b-g2/砺石新APP`；分支`research/m10-jpeg-strict-r3-f1b-g2`。未覆盖原工作区，未reset、合并或改受保护分支。
- G1独立审查依据：用户本轮正式转交**PASS WITH NOTES，仅限研究监督器和宿主测试**；评审锁定7ff737e。不是Android验收或生产修复授权。

## 开始前实际检查

已读取AGENTS、PROJECT、CURRENT_STATE、相关决策/架构/路线、G1三报告、F1B原阻塞与结果、当前verify.py、gate.py、service_record.py及继承的只读采集链。实际比对G1最终输入登记中的源码、测试与fixture字节数/SHA，全部一致。

本轮未改这些锁定文件。verify.py SHA-256为`ad116cc3ab41a8974cf8a8d997319395fff7c81e6283c73fd35074fc811516e8`。原监督器把输出限制在F1B/evidence，因此使用**全新**`m10-jpeg-r3-f1b/evidence/g2-20261017-01/`；没有借此覆盖F1B旧证据。预先记录的[任务契约](m10-jpeg-r3-f1b/evidence/g2-20261017-01/g2-contract.json)包含来源、禁令、输入SHA及结束入口SHA。

## 实际执行

| 项目 | 结果 |
|---|---|
| 原G1 CLI | `python -B docs/research/m10-jpeg-r3-f1b/verify.py preflight --output docs/research/m10-jpeg-r3-f1b/evidence/g2-20261017-01`，只调用1次 |
| 预检时间 | 2026-10-17 07:19:15.849～07:19:16.444 UTC（本地15:19） |
| 第一条ADB | `adb.exe -s emulator-5596 get-state`，退出1，109ms，未超时，stderr为`error: device 'emulator-5596' not found` |
| 预检进程 | 退出2，保存blocked.json；在AVD/API/boot/保护快照之前停止 |
| 必要结束核查 | 07:19:39 UTC，只调用新G2结束入口1次；第一条同串号get-state退出1，79ms，未超时；结束进程退出2并保存原始错误 |
| 后续操作 | 停止；没有重试preflight、没有设备恢复或实验 |

[真实命令](m10-jpeg-r3-f1b/evidence/g2-20261017-01/adb-commands.jsonl)、[预检进程记录](m10-jpeg-r3-f1b/evidence/g2-20261017-01/g2-preflight-execution.json)、[预检阻塞](m10-jpeg-r3-f1b/evidence/g2-20261017-01/blocked.json)、[结束进程记录](m10-jpeg-r3-f1b/evidence/g2-20261017-01/g2-ending-execution.json)、[结束错误](m10-jpeg-r3-f1b/evidence/g2-20261017-01/g2-ending-error.json)保持原结果。

两条命令stdout均0字节，stderr均41字节；SHA及返回码/时间见[完整性登记](m10-jpeg-r3-f1b/evidence/g2-20261017-01/g2-command-integrity.json)。这些摘要由已归档且无损ASCII的命令文本**离线计算**，不是另一次采集，也不是ServiceRecord原文摘要。本轮没有服务或ps输出，不能声称其完整性已验证。

## 结束入口的范围

新增[end_protection.py](m10-jpeg-r3-f1b-g2/end_protection.py)只调用既有只读采集方法，不调用preflight，不增加安装/启动/清理入口。原因是旧finish依赖预检未生成的`snapshot-research-before.json`，且其服务派生截取至全文末；本轮不修锁定监督器、不补造该文件，另用有独占attempt的结束采集入口。

本地静态辅助审查确认结束入口无设备变更路径；它也只执行了第一条get-state就停止。其余结束采集分支**NOT_RUN**，不能因代码存在称保护比较通过。该辅助审查不代替ChatGPT独立验收。

## 修改与实际验证

- 新增G2三报告、结束采集脚本及其字节保存规则、独立G2原始证据和新checkpoint；更新CURRENT_STATE。
- 没有修改G1/旧研究源码、正式App、Room、Repository、WorkerService、StageStore、Manifest、Gradle、签名或原JPEG测试。
- 实际执行：源码/fixture SHA核对；结束脚本Python AST语法检查；两条真实只读设备查询；离线命令/结果、链接、差异、敏感信息与受保护工作区检查。
- G1原81项宿主测试、Java/JVM、Gradle、Lint、APK构建和所有产品测试本轮**未重跑**。0设备快照，0新增Android案例PASS；两条失败查询不是两项Android测试。

## 停止点

结果详见[真实门禁结果](M10_JPEG_R3_F1B_G2_DEVICE_RESULTS.md)、[风险与决策](M10_JPEG_R3_F1B_G2_DECISION.md)、[checkpoint](../checkpoints/2026-10-17-M10-JPEG-R3-F1B-G2.md)。只推送G2分支后停止，最终SHA/远端一致性以Git回执为准，不自引用。

N01～N03、历史UNKNOWN、原29场景0 PASS/1 UNKNOWN/28 NOT_RUN、M10 PENDING/三JPEG FAIL、UI-C1冻结PASS WITH NOTES、M11 NOT_AUTHORIZED均保留。没有因本次设备不可达推翻G1宿主审查，也没有从历史设备证据推定当前设备状态。
