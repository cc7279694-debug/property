# M10-R3-F1B-G2-R — 设备与只读门禁实测

日期：2026-10-17。来源：锁定G2提交 `2de44c79173e7fa52841b55542e5ddcc5399a30a`；研究分支：`research/m10-jpeg-strict-r3-f1b-g2-resume`。

**结论：BLOCKED — M10-R3-F1B-G2-R。** 原AVD一次冷启动成功；唯一一次锁定G1预检因 `unknown ps UID label` 退出2。未修改分类规则，未重试预检，未取得第二份预检快照或 `PREFLIGHT_ELIGIBLE`。

以下均为本轮实际证据；原G2离线失败记录保持不变。证据目录为 [g2r-20261017-01](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/g2r-results.json)。

## 1. 环境恢复

| 检查 | 结果 | 证据与边界 |
| --- | --- | --- |
| 启动前设备、进程及端口 | PASS | 目标ADB行为空；目标及未知模拟器进程为空；5596/5597无监听。查询均完成，未终止任何进程。 |
| 原AVD配置 | PASS | `E:/Android/avd/Lithic_M01_API_36.ini` 指向 `E:/Android/avd/Lithic_M01_API_36_fresh.avd`，ini/config SHA与F0锁定值一致；未读取userdata或snapshot内容。 |
| 启动次数与参数 | PASS | 仅一次原 `emulator.exe` 请求，端口5596，隐藏运行，使用 `-no-snapshot-load -no-snapshot-save`，无wipe、重建或配置修改。 |
| 有界等待 | PASS | 上限240秒；等待循环44.766秒、13轮、26条查询；前两条offline退出1，余24条退出0，0超时。启动请求至身份确认约65.805秒，与循环用时区分。 |
| 实际设备身份 | PASS | `emulator-5596`、`Lithic_M01_API_36`、API36、`boot_completed=1`。 |
| 结束宿主状态 | 已保留 | 原模拟器仍运行，未停止或重启；ini/config SHA前后一致。 |

ini SHA：`a13c3fd2492d875a77a68262842b8912c3df6f67f4f2e329748774b465e3b902`；config SHA：`555d10f2c3fe3bd8cd1c231ceea06c77532d132d39236a7f31c9fc839506bf54`。

来源：[环境检查](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/environment-before.json)、[启动请求](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/launch-request.json)、[等待结果](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/boot-wait-result.json)、[等待原始命令](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/boot-wait-commands.jsonl)、[结束宿主检查](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/environment-after.json)。本次是F0锁定的现有替代AVD，不能据此改写M10曾误重建AVD的历史，也不能宣称从M01起userdata一直连续保留。

## 2. 实际安装身份

下表由本轮sample1实际 `pm path`、`run-as … id -u` 与已安装APK `sha256sum` 命令离线整理，属于原命令结果派生，不是新设备采集或安装。

| 应用 | UID | 已安装APK SHA-256 | 结果 |
| --- | --- | --- | --- |
| `com.cooper.lithic.debug` | 10216 | `4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46` | PASS：正式APK身份符合锁定值 |
| `com.cooper.lithic.m10r3.probe` | 10218 | `230407d8bc98aeac6fcda632b935551e8ca3322cbdf55cb73baf8bc258cb47d0` | PASS：现有旧研究APK身份，未更新为F1-A产物 |
| `com.cooper.lithic.m10r3.outsider` | 10219 | `214a6e98085d77d8fffb0c5be9f9aff70700c0f1e6365a3042b6ccf72385b358` | PASS：现有旧研究APK身份，未更新为F1-A产物 |

三个UID互异，结束保护查询的研究APK身份与sample1一致。来源：[实际命令派生身份与命令索引](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/g2r-sample-1-identities-derived.json)、[结束研究包身份](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/g2-after-research-packages.json)。身份核对不代表研究代码已安装或Android生命周期测试通过。

## 3. 唯一预检与停止点

| 检查或分类 | 结果 | 实际证据与限制 |
| --- | --- | --- |
| 锁定G1入口 | BLOCKED | `verify.py preflight` 仅一次；进程退出2，原因 `unknown ps UID label`。 |
| sample1服务记录 | `NO_TARGET_RECORD` | 完整命令退出0，68字节，`complete=true`、`truncated=false`；目标记录不存在，未用裁剪记录冒充完整输入。 |
| `INACTIVE_RECORD_CANDIDATE`实机路径 | NOT_RUN | 本轮没有目标ServiceRecord可供此路径验证，不能拿“无目标记录”替代。 |
| sample1研究进程与UID关联 | BLOCKED | `ps`命令本身退出0，但G1解析遇到未知UID标签；未取得可接受的研究Host/Worker进程清单，不能确认无活跃Worker。 |
| sample1文件快照 | 已采集，阶段校验未到达 | 快照完整、稳定、未截断；研究stage命名空间缺失，仅见既有合成owner与trace项。预检因进程解析阻塞，未执行后续stage校验与组合判定。 |
| sample2 | NOT_RUN | 停止后未采集第二份预检快照。 |
| 两份预检快照一致性 | NOT_RUN | 必要结束核查不作为sample2。 |
| `PREFLIGHT_ELIGIBLE` | NOT_RUN | 未调用成功的双快照组合资格判定，无安装或执行许可。 |

sample1服务完整输入SHA：`44009d1664d1b66d17ec4859a9e03642b0125d242c05eac9c101806e2397fb7c`。完整输入在采集时用于分类，归档仅保留完整性元数据及研究范围结果，不归档无关应用日志。

**进程证据缺口必须保留：** sample1原 `ps`输出为21913字节，G1失败路径未保存该输出SHA，也未保存触发异常的具体UID token。无法从现有隐私过滤证据还原其具体标签或根因。本轮未追加诊断、放宽解析或重跑。结束核查的 `ps`为另一次21919字节采集，其SHA `5cc2e17f5047774012ded3dfc952eb2d6d5b957e94bd82e47fbbb17814771745` **只属于结束采集**，不得填补sample1缺失的SHA。

来源：[入口执行记录](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/g2r-preflight-execution.json)、[原阻塞](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/blocked.json)、[服务结果](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/preflight-sample-1-services.json)、[进程结果](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/preflight-sample-1-processes.json)、[文件快照](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/preflight-sample-1-files.json)。

## 4. 必要只读结束保护

锁定G2 `end_protection.py`仅运行一次，进程退出0表示只读收集完成，**不表示预检通过**。

| 检查 | 结果 | 保护范围 |
| --- | --- | --- |
| 结束进程检查 | BLOCKED | 仍为 `unknown ps UID label`；未确认研究进程安全闲置。 |
| 结束服务记录 | `NO_TARGET_RECORD` | 与首服务分类相同，68字节与相同输入SHA；不替代双快照一致性。 |
| 结束stage校验 | PASS，限快照 | `namespace=ABSENT`、attempts空、current为空；不证明Android lease释放或Worker死亡。 |
| 正式APK及UID | PASS | 前后SHA、UID相同。 |
| 正式私有文件元数据 | PASS | 前后188条，聚合SHA均为 `326e7f5576fdb03ba0b57012ada3c12b06adf147726946e9f8df2b856d4a8a83`。仅find/stat与聚合，未归档私人路径或内容。 |
| 设备设置 | PASS | 字体、旋转、主题、尺寸、密度及额外时间设置前后一致。 |
| 私人账本与图片字节一致性 | NOT_RUN | 元数据相同不能推定私人字节相同；未读取或复制其内容。 |

上述保护基线在本次冷启动完成后采集，不能反推冷启动前后所有私人字节都一致。来源：[前保护快照](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/snapshot-before.json)、[后保护快照](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/snapshot-after.json)、[结束结果](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/g2-ending-result.json)、[保护比较](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/g2-protection-comparison.json)、[结束进程](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/g2-after-processes.json)。

## 5. 命令统计、未执行项及保留状态

| 范围 | 实际数量与结果 |
| --- | --- |
| 初始ADB设备列表 | 1条，退出0 |
| 冷启动等待与身份确认 | 26条，24退出0／2退出1，0超时 |
| 唯一预检及必要结束保护 | 66条ADB查询，全部退出0；上层预检仍因解析失败退出2 |
| 合计 | 93条已归档ADB查询，**不是93项测试** |
| APK安装、应用启动/停止、清理、normal/Outsider、其他28项场景 | 本轮发出相应命令均0次；未执行这些操作，不推定Android冷启动中没有系统自行启动的进程 |
| 构建、宿主测试重跑、JNI或allocator实验 | NOT_RUN，本轮不授权且未执行 |

原始命令与退出码见[预检/结束命令](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/adb-commands.jsonl)、[启动等待命令](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/boot-wait-commands.jsonl)、[环境命令](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/environment-commands.jsonl)。[完整性登记](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/g2r-command-integrity.json)区分实际采集哈希、由已归档命令文本离线派生的哈希与原采集器未记录的哈希；缺失项不回填猜测值。

- F1-A N01（REVOKED授权语义）保留OPEN，未测试取消且未修复；N02保守残留边界保留；N03实际Outsider连接清理仍NOT_RUN。
- 原R3为 **0 PASS／1 UNKNOWN／28 NOT_RUN**，完整生命周期仍BLOCKED；历史attach与R1超时UNKNOWN不关闭。
- M10继续PENDING、原三项JPEG FAIL；UI-C1继续ACCEPTED / FROZEN — PASS WITH NOTES；M11继续NOT_AUTHORIZED。
- 结论仅供ChatGPT独立审查。下一次进程解析诊断或研究预检需另行授权；本轮已经停止，不自动安装、启动或清理App。
