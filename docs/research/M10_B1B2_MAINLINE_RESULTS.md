# M10 B1/B2 主线续验结果

2026-10-19 · **BLOCKED / WAITING_CHATGPT_REVIEW**。从 `45c77007218f8d4e54f43b318c9a3c22c057c90d` 继续同一分支 `codex/m10-b1b2-android-safety-resume`、同一隔离工作区。旧操作记录、原始日志、两个历史UNKNOWN均保留；没有App/Test构建、安装或环境重建。

| 本轮项目 | 真实结果 |
|---|---|
| 当前API26、APK、boot、UID、Stage准入 | **PASS**：原launcher40472/QEMU18424及创建身份、路径和5664/5665端口成立；API26/x86_64/boot=1；App/Test UID10077/10078、锁定字节和安装凭据一致。只有既有健康Stage，已终结且无活动消费者。 |
| 新独占传输claim | **已创建并消耗**：`input-transfer-mainline-45c77007218f`；绑定来源、实际设备/进程、kernel bootId、BOOT_COUNT、App/Test SHA/UID及三个原件长度/SHA。旧`input-transfer`未修改、未重试。 |
| 输入传输 | **UNKNOWN / files=[]**：父目录前置身份检查exit1，发生在目录创建和图片字节写入之前。上传0。 |
| B2 Baseline420 / Progressive420 / Progressive444 | **全部NOT_RUN**：没有取得本轮40MP JUnit、耗时、CPU、RSS/PSS/HWM、native16或Worker资源结果；健康444未被错误样本替代。 |
| B1取消、迟到RPC、Worker故障、两种Host中断及恢复 | **全部NOT_RUN**：输入门禁失败后停止依赖项；主动Worker/Host终止0。 |
| API26正常退出 / API36 | **NOT_RUN**：API26保持原实例运行；API36创建/启动/安装/测试均0。 |

## 已确认的精确阻塞

实际失败命令在输出`M10_INPUT_PARENT_BEGIN`前exit1，stdout/stderr均0B。[原命令与完整性数据](m10-b1b2-mainline/evidence/failed-parent-command.json)及[新UNKNOWN原件](m10-b1b2-mainline/evidence/input-transfer-mainline-45c77007218f/result.json)不改写。

结束只读诊断确认：`run-as`的`pwd -P`实际为`/data/data/com.cooper.lithic.debug`，原helper却硬编码要求`/data/user/0/com.cooper.lithic.debug`。两个绝对App路径与当前目录的实际mode/UID/device/inode均为 **`41c0:10077:64512:115162`**，最终App目录均非链接，单命令exit0。[真实别名对照](m10-b1b2-mainline/evidence/mainline-parent-alias-readonly.json)。这是输入监督器误拒绝合法路径别名，尚不是40MP/native失败，也不是未知消费者。

本轮最小代码差异仅限`stage_inputs.py`和针对性宿主测试：新增固定一次性claim并复用原父目录/传输逻辑；实际失败后，仅离线修正精确两种App根别名并要求三方文件身份一致，拒绝错UID、inode、链接和未知路径。不会仅凭路径字符串放行。**修正后的目录创建/传输在Android仍NOT_RUN**；没有重用已消费claim、重命名尝试或新建第二次传输。

新claim6项及别名修正4项均完成宿主RED→GREEN，旧4＋父目录5项回归保留；最终同组 **19唯一宿主方法PASS＝新增10＋既有9**，4.281秒。见[新claim验证](m10-b1b2-android-resume/evidence/mainline-host/input-transfer-results.json)、[真实失败前缀/别名回归](m10-b1b2-android-resume/evidence/mainline-host/input-alias-results.json)、[最终原始输出](m10-b1b2-android-resume/evidence/mainline-host/final-targeted.stderr.raw)。执行中的输入代码SHA为`874a028263d4f1ab1dc7ecd63d4d03fca34d1470e3b9c77961ad5e11187c439c`；结束后离线修正SHA为`2249d96e95768cd8b7593417edb919f075ebd0aff45da46fd990634303aaf64d`。不将宿主结果计作B1/B2 Android PASS，不重复JVM/native/旧18项自然测试。

## 身份、资源与结束现场

- App SHA：`b7c90277fa6c27e6ab314aa52c293de8e445ba6a3deba9e984ab96f4280e8a4c`；Test SHA：`37ad9ec91964b6a0b8a870ec35032164ebcdb6a1c41522e780cb7af343fcd338`。签名证书与原审计一致。未修改`app/`、Room、备份、算法、UI或JPEG断言。
- 三宿主输入仍是17,163,380B baseline420、15,458,857B progressive420、27,445,246B `single444/valid_40mp_progressive_444.jpg`；固定SHA已核验并写入claim。原图片未复制入Git/未传入设备。
- 运行前宿主空闲2,447,864KiB，Guest MemAvailable1,043,412KiB、/data可用10,020,492KiB；只是余量观察，没有40MP实测，也不是安全内存cap。[余量证据](m10-b1b2-mainline/evidence/mainline-resource-headroom.json)。
- [结束只读核查](m10-b1b2-mainline/evidence/mainline-ending-readonly.json)：同一设备、APK和UID不变，目标UID无Host/Worker，唯一健康Stage及原预检记录与入场前完全相同；`files`和输入目标目录ABSENT。旧业务图片/账本内容未读取，业务金额与付款的异常场景验证NOT_RUN；不把“未运行”扩称数据保护PASS。H2、C2和其他保留AVD均未访问。
- **本轮计数**：AVD创建/启动0；App/Test安装各0；JUnit/Instrumentation0；传输claim1、上传图片0、故障注入0、设备退出0。原API26九项/API36九项和上一健康冒烟只继承历史，不重跑。

**唯一当前执行阻塞**：输入准备因路径别名误拒绝，新的claim已消耗且UNKNOWN，尚未取得三张设备输入；不能继续B2/B1或复用该claim。最小别名修正与原始证据提交独立审查，任何新的传输尝试需明确的新续接边界。正式冻结仍缺B1异常和B2 Android40MP系统证据。

[关键证据索引](m10-b1b2-mainline/evidence/INDEX.json) · [checkpoint](../checkpoints/2026-10-19-M10-B1B2-MAINLINE.md)。M10 PENDING、三JPEG历史FAIL、全部NOTE/UNKNOWN/NOT_RUN、UI-C1 FROZEN、M11 NOT_AUTHORIZED保持；提交推送当前分支后停止，不自行冻结或进入下一模块。
