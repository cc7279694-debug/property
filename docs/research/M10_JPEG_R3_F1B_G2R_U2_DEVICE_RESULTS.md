# M10-R3-F1B-G2R-U2 真实只读门禁结果

2026-10-17。来源U1提交`92cecaef48300629cc4136371f9dd4018d275ae6`。**READY_FOR_REVIEW — M10-R3-F1B-G2R-U2**。

以下是本轮实际设备读取结果，原G2/G2-R失败、U1受限诊断与历史UNKNOWN不改写。[汇总JSON](m10-jpeg-r3-f1b/evidence/u2-20261017-01/u2-results.json)保留逐项状态。

## 逐项验收

| 项目 | 状态 | 本轮实际证据与限度 |
|---|---|---|
| 唯一环境及boot | PASS | 原AVD已在线，实际serial/AVD/API36/boot=1；启动0次。 |
| 正式/Probe/Outsider身份与UID | PASS | 两轮独立读取三包路径、UID及APK SHA，UID10216/10218/10219互异；结束一致。 |
| 完整服务输出分类 | PASS | 两轮退出0、完整68字节、严格格式验证后NO_TARGET_RECORD。 |
| INACTIVE_RECORD_CANDIDATE实机路径 | NOT_RUN | 本轮不存在目标记录，不把NO_TARGET_RECORD算作此路径验证。 |
| 当前进程与UID关联 | PASS | 两轮完整ps实际经U1解析器，均无研究名称/UID行；uwb各1次映射1083，无未知UID/格式错误。 |
| stage/current/pin/receipt及未知子项 | PASS，限快照 | 两轮研究files完整stat且重复采集稳定；stage命名空间ABSENT、attempts空、current=null，无stage内未知残留。不读取旧trace内容。 |
| 两份独立快照 | PASS | 先后分别采集设备、boot、三包、ps、服务及inventory，命令窗口不重叠。 |
| 语义状态一致性 | PASS | 原G1 gate比较环境、三包、研究进程、服务与stage签名一致。全ps字节不同不被伪装为相同。 |
| 最终组合结果 | PASS，PREFLIGHT_ELIGIBLE | 实际preflight入口退出0，原gate的reasons为空；仅研究只读观察资格。 |
| 结束进程/服务/stage | PASS，限结束快照 | 实际processStatus PASS、服务NO_TARGET_RECORD、stage ABSENT/PASS；未清理或获得lease/死亡证明。 |
| 正式APK/UID保护 | PASS | 前后实际相同，见下表。 |
| 188条正式私有元数据聚合 | PASS | 实际188→188、aggregate SHA相同；未预设数量，也不由此推出字节无损。 |
| 设备设置保护 | PASS | 7项原设置及3项时间设置前后一致。 |
| 研究包结束身份保护 | PASS | 由同次两轮命令及结束JSON离线核对，不是额外设备采样。 |
| 私人账本/图片字节完整性 | NOT_RUN | 未读取、复制或归档私人内容。 |
| APK安装/应用启动停止、Outsider/normal及29场景 | NOT_RUN | 本轮相应命令均0次；不从观察通过推定生命周期通过。 |
| 宿主测试重跑、Gradle/Lint/APK、JNI实验 | NOT_RUN | 不在本轮范围；U1 103项仅保留历史效力。 |

## 双快照时间与完整性

下列时间是外层记录的**实际命令窗口**，不是补造原函数未落盘的内存时间戳；原gate仍使用其真实内存内start/finish及独立sampleId完成校验。

| 样本 | UTC命令窗口 | 执行期命令索引 | 服务 | 研究进程 | stage |
|---|---|---|---|---|---|
| sample1 | 08:28:31.742894～08:28:33.845416 | preflight 20～37 | NO_TARGET_RECORD | [] | ABSENT |
| sample2 | 08:28:33.860929～08:28:35.912260 | preflight 38～55 | NO_TARGET_RECORD | [] | ABSENT |

| 完整采集 | 字节数 | 原始stdout SHA-256 |
|---|---:|---|
| sample1 ps | 17942 | `9be81a9482d0a90e4a5c5afbf2930af764c3f3d0b8278fab56e5bbd795b62f04` |
| sample2 ps | 17964 | `bcba77fbe073365627b213628e87d7c9b42f9f6012d67f9199f008f2388977b7` |
| ending ps | 17960 | `116a37b1a78cb62f540f8dbb722adf5cee970303cf65c953532663ffe0c3d56e` |
| 三次服务采集（分别采集） | 各68 | 各`44009d1664d1b66d17ec4859a9e03642b0125d242c05eac9c101806e2397fb7c` |
| 各inventory稳定stat | 各262 | `c7149914d71799bb482a04abd143f86bc1208d7a6e00a12b73fffb3352a96c5a` |

所有命令退出0、完整、无stderr；原解析器在内存消费完整输入，非裁剪文本。归档保留原始完整性元数据和范围结果，完整ps/dumpsys不公开。服务为无目标记录，因此本次没有目标ServiceRecord块可归档；不虚构32字段记录。

本轮1083来自U1已实证且锁定的精确映射；本轮只读取原USER列，没有再读取数字UID列，也不将映射输出冒充新的数字列取证。

研究files只有既有`synthetic-main-owner`与`r3-traces/trace-3928.jsonl`等4项元数据（含目录），均位于stage命名空间之外。它们前后未清理、未读内容；ABSENT不等于整个研究包没有任何文件。

来源：[原gate结果](m10-jpeg-r3-f1b/evidence/u2-20261017-01/preflight-gate.json)、[sample1服务](m10-jpeg-r3-f1b/evidence/u2-20261017-01/preflight-sample-1-services.json)、[sample2服务](m10-jpeg-r3-f1b/evidence/u2-20261017-01/preflight-sample-2-services.json)、[sample1进程](m10-jpeg-r3-f1b/evidence/u2-20261017-01/preflight-sample-1-processes.json)、[sample2进程](m10-jpeg-r3-f1b/evidence/u2-20261017-01/preflight-sample-2-processes.json)、[sample1文件](m10-jpeg-r3-f1b/evidence/u2-20261017-01/preflight-sample-1-files.json)、[sample2文件](m10-jpeg-r3-f1b/evidence/u2-20261017-01/preflight-sample-2-files.json)。

## 三包身份与结束保护

| 包 | 实际UID | 已安装APK SHA-256 |
|---|---:|---|
| 正式`com.cooper.lithic.debug` | 10216 | `4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46` |
| Probe | 10218 | `230407d8bc98aeac6fcda632b935551e8ca3322cbdf55cb73baf8bc258cb47d0` |
| Outsider | 10219 | `214a6e98085d77d8fffb0c5be9f9aff70700c0f1e6365a3042b6ccf72385b358` |

三个现有包未更新；Probe/Outsider仍为旧研究产物，不能宣称F1-A新代码已安装或ServiceConnectionLeaked已消失。包路径/UID/SHA的实际命令索引及离线派生见[身份记录](m10-jpeg-r3-f1b/evidence/u2-20261017-01/u2-identities-derived.json)。

正式私有元数据聚合前后均188条，SHA `326e7f5576fdb03ba0b57012ada3c12b06adf147726946e9f8df2b856d4a8a83`；仅对files/no_backup/databases/shared_prefs做find/stat聚合，原路径不归档。设置前后：font1.0、rotation0、自动旋转1、night setting1/effective no、1080×1920、density420；auto_time1、auto_time_zone1、timezone GMT。仅读取，无设置写入。

来源：[before](m10-jpeg-r3-f1b/evidence/u2-20261017-01/snapshot-before.json)、[after](m10-jpeg-r3-f1b/evidence/u2-20261017-01/snapshot-after.json)、[保护比较](m10-jpeg-r3-f1b/evidence/u2-20261017-01/g2-protection-comparison.json)、[结束结果](m10-jpeg-r3-f1b/evidence/u2-20261017-01/g2-ending-result.json)。原模拟器保持运行，结束宿主检查的PID及ini/config与开始一致；没有用元数据推定私人字节完整性。

本轮没有BLOCKED、UNKNOWN或非零设备退出记录需要隐藏；原阶段所有FAIL/UNKNOWN/NOT_RUN仍原样保留。85条只读查询不计入原29个Android生命周期案例，原案例状态不增加PASS。
