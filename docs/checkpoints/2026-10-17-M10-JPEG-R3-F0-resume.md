# M10-R3-F0 — 原AVD上线后的启动诊断交付

2026-10-17（Asia/Shanghai）。**READY_FOR_REVIEW — M10-R3-F0 Attach Diagnosis**。分类D INTERMITTENT_RECOVERED，仅研究诊断，等待ChatGPT独立审查；本checkpoint不授生产或下一步授权。

## Goal / Source / Scope

从F0首轮`ef25b4d403be026511b036890ccfd3961eb651c5`继续`research/m10-jpeg-strict-r3-f0`，不reset或重建已有分支。用户重复F0附件字节不变（SHA ff0f90631f8cfd78747f3be8f01452f3228990f1c6bc56eb3c3601e37ce953a4），随后明确批准“允许仅启动原模拟器”。据此只启动现有Lithic_M01_API_36，保留AVD/数据；未安装/打包/修改App或研究APK源码，不执行29场景。

## Verified completed

- 原AVD端口5596/API36/x86_64、boot=1，三包实际SHA与预期完全一致，UID10218/10219/10216；实际读出的worker Manifest非导出。
- 1次无参数Host，PID3928，系统fork/Start proc→am_proc_bound约198ms，Application角色及私有trace、onCreate/Resume/Displayed均有日志，1572ms。1次Outsider，PID4062，明确跨UID拒绝Worker，未观察到worker实例。此为F0证据，不改变R3原0PASS/1UNKNOWN/28NOT_RUN。
- 辅助Activity销毁报ServiceConnectionLeaked、堆栈指向OutsiderActivity.java:14。新增独立NOTE及最小清理提案；未修复，不能称辅助流程零错误。
- 正式APK/UID、188条私有元数据聚合及设备设置前后一致；开始聚合与R3旧值一致，私人字节一致性仍NOT_RUN。所有PID只用于即时诊断，未kill/force-stop；原AVD及研究进程保持运行，未观察到worker。

来源：[三份报告入口](../research/M10_JPEG_R3_F0_DIAGNOSIS.md)、[时间线](../research/M10_JPEG_R3_F0_TIMELINE.md)、[根因评估](../research/M10_JPEG_R3_F0_ROOT_CAUSE_ASSESSMENT.md)、[结构化结果](../research/m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/diagnostic-summary.json)、[103条原始命令](../research/m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/adb-commands.jsonl)、[22份输入SHA](../research/m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/source-inputs.json)。首轮不可访问证据与旧F0/R3 checkpoint原字节保留。

## Limits / Notes

历史attach根因UNKNOWN；AVD从离线启动与Intent移除extras两个条件均不同，没有单变量因果证明。R1 120秒UNKNOWN不关闭。

load均值随启动下降，短时CPU忙碌约0.928%；PSI权限被拒，组合cat部分失败保留，RSS/可用内存不是native上限。日志按epoch/研究PID/核心系统筛选，4000行窗口及混包丢行有限制；Host初筛49行、发布40行，9行非核心actor仅保留本机忽略目录。辅助ServiceConnectionLeaked原堆栈仍完整保留。诊断脚本过滤/本地审计目录小修并存版本快照，不改APK。

原runCase guard-only旧attempt收口NOTE未修复。API26、native fatal/OOM/LMK、FD/pin/恢复、JUnit/Lint/Gradle/产品构建均NOT_RUN。M10 PENDING/原3JPEG FAIL、UI-C1冻结PASS WITH NOTES、M11 NOT_AUTHORIZED及其他旧NOTES保持。

## Verification / Git

提交前已实际核对22份输入字节SHA、Host40行/Outsider60行与103条命令计数、JSON/JSONL可解析、诊断脚本AST、报告本地链接、敏感模式和Git范围。只改CURRENT_STATE、本轮三报告、诊断脚本并新增本checkpoint/本轮证据；原研究APK源码、正式App/schema/Gradle/验收登记、R0～R3报告、首次F0证据与旧checkpoint均无变更。未检出凭据/私钥或APK/数据库/私人媒体/缓存；未提交完整设备日志或其他App消息。文档/字节核对不是重跑产品测试。

7个受保护远端分支仍为原基准，6个受保护工作区干净且HEAD未变，F0远端仍为首轮ef25b4d，[保护核查](../research/m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/git-protection-before-commit.json)。当前补充交付的文件清单/原字节哈希与Git转换记录见[补充交付核查](../research/m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/resume-final-verification.json)；首轮final-verification仅代表ef25历史交付，不作为新输入验收。Git空白检查允许保留原字节CRLF后执行，不以改写原始日志消除提示。

实际Manifest导出文本的Windows落盘曾重复CR；按未改动JSONL中的ASCII原始stdout精确恢复该派生文件，错误格式副本仅保留本机忽略目录，输入/命令原件、APK均未改变，未重新执行。恢复证明及SHA见[导出格式登记](../research/m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/manifest-output-recovery.json)。最终允许CRLF的Git空白检查退出0。

只推同一F0分支；最终SHA/远端一致性取Git回执，不递归写入自身。正式验收登记、生产/研究APK源码、R0～R3原报告与规划归档、M10/UI-C1/main等分支不改。

## Next step

设备实验已结束。停止等待ChatGPT独立审查；任何Outsider最小修复、旧attempt恢复/撤销/清理及R3生命周期试验均须新授权。不自动进入资源研究、生产接入或M11。
