# M10-R3 — 隔离宿主执行与来源

日期：2026-10-16（Asia/Shanghai）。**BLOCKED — M10-R3 Worker Lifecycle**，等待ChatGPT独立审查。阻塞在研究Host启动/attach阶段；没有取得worker生命周期实证，不批准生产接入。

入口：[生命周期结果](M10_JPEG_R3_LIFECYCLE_RESULTS.md) · [FD/恢复](M10_JPEG_R3_FD_RECOVERY.md) · [决策](M10_JPEG_R3_DECISION.md)。

## 1. 本轮契约与来源

用户授权隔离Android研究工程、编译/安装/测试研究APK及文档/Git交付，未授权正式App接入、allocator、spill、完整JPEG规范化或M11。授权原文位于本机附件`7592e014-5923-4b8c-b60c-e93cfb5111c9/已粘贴的文本.txt`，SHA-256 `aa421ca00dcc1a7d88390fb5bbeb2ab45bc36d94b879a9f8647dba16256c766c`。该原文转交R2独立结论：**PASS WITH NOTES，仅限研究架构**，不是M10通过。

- 来源R2本地/远端锁定：`2912601899f0282ecc6aa32d5cf26a7b44c24c63`。
- 新分支：`research/m10-jpeg-strict-r3`，独立worktree `E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3/砺石新APP`。
- 保护基准：M10 `41596820145dc4a3fa6d041728a59540d828108a`；UI-C1 `0dce7dd0725a54be4f0280870ef6ca47c48ea7ed`；R1 `2cdad4aa40f9503aad13245670adc9a4d13db32b`；main远端 `145326ddd4ce28abbf7c2706fe25f9d38c599c3a`。起始工作区干净，不覆盖、不reset/rebase/merge。
- 实际读取AGENTS/PROJECT/CURRENT_STATE/工具链锁、R2四文档/checkpoint、R1报告及原120秒超时JSONL，现Store/Protocol/Repository/coordinator/generation、Application/AppContainer与Nav3/CoverViewModel守卫。原文件不修改。

Goal是证明受控B1 worker会话、FD/pin及重启恢复。验收必须有实际系统身份、Binder、资源和死亡证据；源码/编译不能替代。当前仅完成隔离工程及JVM门禁，设备核心契约未证。

## 2. 隔离工程与环境

独立[Gradle根](m10-jpeg-r3/host/settings.gradle.kts)，未加入正式settings。两个无共享UID的研究APK：

| 项目 | 配置/实际工具 |
|---|---|
| probe | `com.cooper.lithic.m10r3.probe`；Host＋非导出`:jpeg_worker` Service |
| outsider | `com.cooper.lithic.m10r3.outsider`；计划验证不同普通UID绑定拒绝，实际绑定NOT_RUN |
| 语言/平台 | Java17编译，API26基础路径；min26/compile36/target36，Build Tools36.0.0 |
| 构建 | 既有JDK17.0.20.1+1、Gradle8.14.3、AGP8.13.2；offline解析既有缓存。JUnit4.13.2仅testImplementation |
| ADB/aapt2 | 既有ADB1.0.41 /37.0.1-15733141；aapt2 2.20-13193326 |
| 设备 | 原`Lithic_M01_API_36` /`emulator-5596`，API36、x86_64；没有可用已授权API26设备证据 |

真实版本输出与wrapper哈希见[toolchain](m10-jpeg-r3/evidence/toolchain/tools.json)。未安装或升级工具，未使用NDK/CMake/JNI或libjpeg。Manifest不请求网络、存储、相机、通知/前台Service权限。实际APK无native库和JUnit；见[APK审计](m10-jpeg-r3/evidence/apk/audit.json)、[最终Manifest](m10-jpeg-r3/evidence/apk/probe-manifest.txt)。

### 源码与构建路径

[build_host.py](m10-jpeg-r3/scripts/build_host.py)只执行独立`:probe:testDebugUnitTest :probe:lintDebug :probe:assembleDebug :outsider:assembleDebug`，不执行正式`:app`。首次直接中文路径的AIDL任务报`MalformedInputException`；之后把研究源码逐字节投影到新建ASCII目录`E:/CODEX/.codex/research-build/m10-r3/<timestamp>`，保存源SHA并断言副本相同，不改系统编码或工具链。该现象符合路径/工具编码问题；没有对AIDL底层编码做完整根因证明。

累计10次构建尝试：5退出1、5退出0。前期AIDL路径异常、不可用`O_DIRECTORY`常量、`fcntlInt`实际需API30的Lint错误、回放测试的Activity方法调用错误均保留；修正限研究工程。没有抑制Lint、降低minSdk或改生产代码。

最终输入与原日志：[20261016T145619.048625Z](m10-jpeg-r3/evidence/build-runs/20261016T145619.048625Z/result.json)、[inputs](m10-jpeg-r3/evidence/build-runs/20261016T145619.048625Z/inputs.json)、[Gradle](m10-jpeg-r3/evidence/build-runs/20261016T145619.048625Z/gradle.log)。退出0，日志`BUILD SUCCESSFUL in 16s`，命令wall17.172s。17个唯一JVM方法PASS/0失败/错误/跳过；Lint0错误/4警告：OldTargetApi、DataExtractionRules、StaticFieldLeak、MissingApplicationIcon。重复构建不累计测试方法数。

原构建证据目录`evidence/build`因仓库`**/build/`忽略规则，仅更名为`build-runs`供Git交付；未写改日志内容，旧绝对路径仍保留当时位置。成员SHA与路径映射见[迁移登记](m10-jpeg-r3/evidence/build-evidence-relocation.json)。真实APK/编译缓存不提交。

## 3. 测试输入、RED/GREEN与APK

研究Host源码调用纯Java[SessionGate](m10-jpeg-r3/host/probe/src/main/java/com/cooper/lithic/m10r3/probe/SessionGate.java)，测试不是孤立Math/伪Binder示例；Android中的实际调用仍NOT_RUN。17项[测试](m10-jpeg-r3/host/probe/src/test/java/com/cooper/lithic/m10r3/probe/SessionGateTest.java)覆盖身份、撤销、重复结果、多线程竞争和结果/death重排。

- 最小可编译stub的行为RED：17项中15 FAIL/2 PASS，编译退出0、JUnit退出1；原stub/测试快照、日志及SHA保留于[unit](m10-jpeg-r3/evidence/unit/session-gate-red.json)。不是故意改坏已有生产实现。
- 同测试GREEN：[原记录](m10-jpeg-r3/evidence/unit/session-gate-green.json)17 PASS；最终Gradle XML再次17 PASS。JVM测试不证明Android调用身份或进程死亡。
- 合成输入预定8192B，`byte[i]=(i*31+7)&255`，SHA `3faac63d133ee546e983a131136bc44c9d3c0910d1c6b143d60509ef90a386e7`。这是CALCULATED，Android未执行源创建/消费；[输入清单](m10-jpeg-r3/evidence/synthetic-input.json)明确NOT_RUN。没有修改两锁JPEG。

| 最终研究APK | bytes | SHA-256 | 已安装UID |
|---|---:|---|---:|
| probe | 42,621 | `230407d8bc98aeac6fcda632b935551e8ca3322cbdf55cb73baf8bc258cb47d0` | 10218 |
| outsider | 8,037 | `214a6e98085d77d8fffb0c5be9f9aff70700c0f1e6365a3042b6ccf72385b358` | 10219 |

实际安装字节与最终APK一致，见[安装登记](m10-jpeg-r3/evidence/device/20261016T150602.612810Z/research-installed-apks.json)。早期install超时未改成退出0；检查安装结果后才认定已存在匹配字节。正式包UID10216，与研究包独立。

本机未公开的APK绝对路径：

```text
E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3/砺石新APP/docs/research/m10-jpeg-r3/host/probe/build/outputs/apk/debug/probe-debug.apk
E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3/砺石新APP/docs/research/m10-jpeg-r3/host/outsider/build/outputs/apk/debug/outsider-debug.apk
```

## 4. 设备执行与保护

[监督脚本](m10-jpeg-r3/scripts/run_device.py)限定序列号、AVD、API、包名及APK身份。早期只读/安装预检超时、aapt2字段名称适配和全部退出码在各[device目录](m10-jpeg-r3/evidence/device/)保留，不算生命周期RED/PASS。元数据遍历改批量stat；包UID改从该包`run-as id -u`核验；单ADB命令最多45秒、场景报告20秒，都是研究监督参数，不是产品取消SLA。

两次正常场景启动均无完整报告，且系统记录Host `failed to attach`后按`start timeout`结束。没有取得Application/CASE_BEGIN/worker事件，最终研究`files`目录不存在；停止设备扩展，不运行后续故障/绑定场景。精确时间、PID和判定见[生命周期报告](M10_JPEG_R3_LIFECYCLE_RESULTS.md)。不把系统启动失败当作人为Host SIGKILL、worker退休或native崩溃验证。

最终前后[保护比较](m10-jpeg-r3/evidence/device/20261016T150602.612810Z/protection-comparison.json)PASS：正式APK/UID、188条私有文件/目录元数据聚合、主题/字体/旋转/wm参数一致。正式APK SHA `4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46`；未启动/覆盖正式App、未wipe/重建/清数据/改时钟/改设置。私人账本/封面内容未读取或复制；**字节级私有数据一致性NOT_RUN，元数据相同不等于字节证明**。原始私有路径/元数据输出不归档，仅留聚合SHA/count。

## 5. 停止

R3代码与构建输入可审查，Android核心验证仍BLOCKED，不能自授通过。UI-C1冻结；M10 PENDING/三项JPEG FAIL；R1 120秒UNKNOWN与全部平台NOT_RUN保留；M11 NOT_AUTHORIZED。只提交推送R3分支后停止，不接生产、不开始下一阶段。
