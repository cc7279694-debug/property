# M11-B — 完整备份导出候选

2026-10-20 · **BLOCKED — M11-B**。从已独立冻结的M11-A `79771fcf5978a938d240d517dd4dacb1d21021a6`继续，分支`codex/m11b-complete-backup-export`。正式M11未冻结，等待ChatGPT独立审查，不进入M12。

## 已实现的产品候选

设置页新增“导出完整备份”和明文保管提示，调用系统保存器选择`.lithicbackup`；保留轮转状态、防重复操作，取消/写入/关闭/读回失败均不显示成功，无法删除本次失败目标时明确提示残留。恢复未开放。

[导出服务](../../app/src/main/java/com/cooper/lithic/data/backup/BackupExportService.kt)在唯一协调器内读取原七表和设置，并[只读复制媒体](../../app/src/main/java/com/cooper/lithic/data/backup/BackupMediaSnapshot.kt)：核对路径、UID、dev/inode/nlink、尺寸、长度和SHA，检查前后身份，源文件不重编码、不修改。复制前限制条目/累计体积；空媒体合法。内部[manifest与ZIP](../../app/src/main/java/com/cooper/lithic/data/backup/VerifiedBackupArchive.kt)核对全条目、中央目录及结束边界，再写目标；观察关闭且全量读回同长度/SHA后才成功。任何登记图片失败即拒绝整份导出，不漏图凑成功。

没有修改Room v2/schema/DAO、冻结金额/订阅规则、备份format v1、Gradle/依赖/Manifest。没有合并M10 Worker/Stage V2/JNI或UI-C1。导航仅传递导出状态/回调，原Nav3业务守卫未改。旧设置测试只替换已经过时的“备份未开放”文案断言，原业务测试断言保持。

## 真实验证与证据

| 检查 | 实际结果 |
|---|---|
| JVM | **143 PASS，0失败/错误/跳过**；原113源码不改，新增30，源码与16份XML逐方法一致。[结果/输入哈希](m11b/results.json) |
| 四任务 | **exit0，BUILD SUCCESSFUL in 2m57s**；单元测试、Lint、Debug及Android测试APK构建。[最终日志](m11b/delivery.log) |
| Lint | **0错误/7警告**，原版本提示不升级。[原报告](m11b/lint-results-debug.txt) |
| Android / SAF | **全部NOT_RUN**。新增9项（6媒体/导出、3设置UI）及原468项只编译，共477方法；没有真实Room/媒体/SAF成功证据 |
| 最终APK | 包名、min26/target36、原签名、无网络权限/九域排除核验PASS；仅原graphics/DataStore库，无JPEG native。[两APK身份](m11b/apk/apk-audit.json) |

工具链保持JDK17.0.20.1+1、Gradle8.14.3、AGP8.13.2、Kotlin2.3.10，离线构建，未升级/安装工具。App **12,744,959字节**，SHA `5c227edd00c15687deebb05dfe618c71e56afa2c96f9c0e9b9a42f8a971692d5`；Test **1,952,492字节**，SHA `d84d022b4fd72b2e35349bed3319214425ed257657477d67f72ca9467e881d4b`。实际文件均位于本worktree的`app/build/outputs/apk/`，未安装/推送APK；版本仍code7/0.1.0-m09-debug，不是发行版本。

真实失败保留：[缺API编译RED](m11b/red.log)；[138项中1失败](m11b/green.log)及[XML](m11b/red-controller.xml)，原因取消后UI停在EXPORTING，已修复为取消保护区发布结果。随后[API27字段Lint失败](m11b/final.log)、[API30方法Lint失败](m11b/verified.log)原样保留。最终读取已装官方NDK r28c UAPI header的`O_CLOEXEC=02000000`（header SHA `0344cc9d595fd81deb2c0bdce745c189448f7955ffc0618ffc7be20bbfd4b509`），以API21 Os.open原子传入标志；不提高minSdk、不加JNI，不冒充API26实测。最终UI小补丁后四任务再次成功，即上方最终日志。

## 最后两个阻塞与停止点

1. **BLOCKING — 完整图片兼容未完成。**冻结M09/M11-A没有可信的JPEG严格完整性能力，Android解码成功不能代替证明。本候选明确拒绝所有JPEG/其他未认证编码；PNG目前仅非交错8-bit RGB/RGBA有容器/CRC/zlib/长度与系统解码检查，不等于所有合法PNG均可导出或语义已穷尽验证。该限制是待修缺口，绝非用户批准缩小格式范围；不能宣称目标已全部完成。
2. **BLOCKING — 真实Android导出未执行。**全新`Lithic_M11B_API36_20261020`、独立目录`E:/CODEX/.codex/research-env/lithic-m11b-export-20261020`唯一一次官方创建 **exit1**：`Can't locate Android SDK installation directory for the AVD .ini file.`。保留[命令](m11b/environment/create-command.json)、[结果/原哈希](m11b/environment/create-result.json)、[stderr](m11b/environment/create-stderr.log)及[结束只读状态](m11b/environment/post-create-readonly.json)。实际create1/start0/ADB0/install0/Activity0/instrumentation0；无config或descriptor，仅本次空目录与claim，未重试。没有操作H2/C2/M10保留设备或私人数据。

未知内部半文件保守残留，不递归删除；成功时临时清理失败会提示，已校验目标不误删。阻塞中的提供方调用不保证即时取消，关闭/读回不等于断电持久性；真机/OEM/API26运行、完整格式覆盖、SAF取消/磁盘满/故障及M12往返仍未运行。M11-T01～T05只记录已执行子断言，不登记完整通过，见[当前验收登记](../ACCEPTANCE_MATRIX.md)。

下一步仅等待独立审查本候选、完整媒体验证能力与Android环境阻塞；不扩建M10研究，不自动重试环境。M10 PENDING / DEFERRED、三JPEG历史FAIL及所有UNKNOWN/NOTES、UI-C1冻结不变。提交仅此分支，不合并main、不发布APK。
