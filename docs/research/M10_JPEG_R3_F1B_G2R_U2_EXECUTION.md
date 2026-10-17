# M10-R3-F1B-G2R-U2 执行报告

2026-10-17。**READY_FOR_REVIEW — M10-R3-F1B-G2R-U2**，等待ChatGPT独立审查。

## 契约与来源

用户转交U1独立结论 **PASS WITH NOTES**，授权本轮必要环境准备、一次完整只读preflight和一次结束保护核查。来源为`92cecaef48300629cc4136371f9dd4018d275ae6`，本地/远端U1一致、原工作区干净；新建独立worktree `E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1b-g2r-u2/砺石新APP`，分支`research/m10-jpeg-strict-r3-f1b-g2r-u2`。不从main开始，不覆盖其他工作。

开始前实际读取AGENTS、PROJECT、CURRENT_STATE、相关决策/架构/路线、U1三报告、G1/G2/G2-R历史及verify/gate/service_record/stage检查器和继承采集代码。U1历史GREEN登记的全部输入SHA与当前文件逐项一致；7份执行脚本与锁定Git blob一致，见[契约/脚本SHA](m10-jpeg-r3-f1b/evidence/u2-20261017-01/u2-contract.json)。本轮未重新执行103项宿主测试，不将输入核对写成新测试PASS。

Scope为本轮研究观察及文档，不安装、启动、停止App，不运行Outsider/normal或29项生命周期场景，不清服务/stage/数据，不改正式代码、研究Java/Manifest/Gradle/APK、JPEG断言或G1/U1规则，不进入JNI/M11。

## 环境与唯一执行

初始ADB与Windows进程检查确认原AVD在线：`Lithic_M01_API_36 / emulator-5596`，既有launcher PID16988、QEMU PID20516、5596/5597监听对应既有进程。ini/config SHA与F0锁定值一致；没有读取userdata或snapshot内容。**启动次数0**，没有重启、wipe、重建、升级或snapshot操作。

独占新建证据目录`docs/research/m10-jpeg-r3-f1b/evidence/u2-20261017-01`，不存在时才创建，原目录和历史证据不复用。外层[只读执行回执器](m10-jpeg-r3-f1b-g2r-u2/run_readonly.py)以同一Python解释器运行锁定CLI入口：

```text
docs/research/m10-jpeg-r3-f1b/verify.py preflight --output <本轮新目录>
docs/research/m10-jpeg-r3-f1b-g2/end_protection.py --output <同一本轮目录>
```

外层通过runpy运行实际main及参数，不替换解析器/返回数据，不增加ps或重试。它只观察同一次`subprocess.run`的返回对象，补充原命令原始bytes/SHA及必要UID计数后原样返回。完整ps、dumpsys及正式私有元数据只在内存处理；不归档无关进程行/名称、私人路径/账本/图片。若未知UID再现，会记录必要token/计数/完整性；本次该分支未触发。固定旧脚本可自行覆盖部分输出，故另有独占证据目录及不可重复的执行marker保护；未修改旧脚本。

| 执行 | UTC时间 | 退出码 | ADB命令数 |
|---|---|---:|---:|
| 唯一preflight | 08:28:29.460229～08:28:35.926316 | 0 | 55 |
| 唯一结束核查 | 08:28:43.425739～08:28:46.932836 | 0 | 29 |

另有初始`devices -l` 1次，总85次ADB读取，全部实际退出0、0超时，**不是85个测试PASS**。84条执行期命令有实际原始bytes/SHA；初始devices仅保存PowerShell解码后显示文本的hash，明确不称原始ADB字节hash。原脚本命令日志与外层完整性日志84条按顺序/argv/返回码核对一致。结束宿主进程/config检查不再调用ADB。

执行回执：[preflight](m10-jpeg-r3-f1b/evidence/u2-20261017-01/u2-preflight-execution.json)、[ending](m10-jpeg-r3-f1b/evidence/u2-20261017-01/u2-ending-execution.json)、[实际命令](m10-jpeg-r3-f1b/evidence/u2-20261017-01/adb-commands.jsonl)、[原始完整性](m10-jpeg-r3-f1b/evidence/u2-20261017-01/u2-command-integrity.jsonl)。入口返回0与各项结果分开审阅，没有仅凭进程退出码宣布安全。

## 实际结果与修改范围

两份独立采集经原`evaluate_preflight()`判定 **PREFLIGHT_ELIGIBLE**。两次实际服务分类均NO_TARGET_RECORD，进程UID解析通过并实际消费uwb1083映射；研究进程列表为空，stage namespace均ABSENT，无current/pin/receipt或stage内未知子项。结束对应检查同样通过。正式APK/UID、188条私有元数据聚合及设置前后一致，三包身份保持。逐项事实和原始字节SHA见[设备结果](M10_JPEG_R3_F1B_G2R_U2_DEVICE_RESULTS.md)。

新增U2三报告、证据回执器、新证据目录、新checkpoint，更新唯一CURRENT_STATE。旧verify/gate/service_record/stage/继承采集器和所有旧测试/证据原件不改；没有数据库、Repository、文件所有权协议或正式构建配置变化。只推送U2研究分支，不合并main；最终SHA及远端一致性以Git交付回执为准。

## 停止与限度

本次通过仅证明两个只读观察的受限研究状态达到当前门槛，不授予安装或运行许可，也不证明持续无消费者、FD关闭、lease释放、Worker死亡或正常任务成功。完整ps字节随系统进程自然变化，两轮比较的是原门禁定义的身份/研究进程/服务/stage语义，而不是全进程原文逐字相同。

私人字节完整性NOT_RUN；没有读取、复制或归档私人账本/图片。原R3仍0 PASS/1 UNKNOWN/28 NOT_RUN，N01～N03、历史attach/R1超时UNKNOWN、M10原三JPEG FAIL、UI-C1冻结及M11未授权保持。提交后立即停止，详见[决策](M10_JPEG_R3_F1B_G2R_U2_DECISION.md)。
