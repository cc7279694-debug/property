# M11-A 备份导出核心候选

2026-10-20 · **READY_FOR_REVIEW — M11-A**，等待ChatGPT独立验收。正式M11未冻结，M12未授权。

## 成果与范围

从M09冻结回写`57de897917473f255d43dfb306a8433b366c00eb`建立`codex/m11a-backup-export-core`，工作区`E:/CODEX/.codex/worktrees/lithic-m11a/砺石新APP`。M10改为PENDING / DEFERRED，保留三JPEG历史FAIL及所有UNKNOWN/NOT_RUN/NOTES、未入Git设备记录和AVD；未访问任何设备，不合并未冻结M10或UI-C1代码。

- **一致读取**：[RoomBackupSnapshotSource](../../app/src/main/java/com/cooper/lithic/data/backup/RoomBackupSnapshotSource.kt)在唯一写协调器内读取SettingsStore和Room事务七表，包含项目、逐笔金额、类别、媒体元数据、订阅、真实付款及状态历史。先限总记录，再分400个订阅ID取关联行并逐表核对数量；不复制运行中的`.db`。
- **稳定格式**：[BackupDtos / Json](../../app/src/main/java/com/cooper/lithic/data/backup/BackupDtos.kt)保存全部v2字段、字符串ID、整数分、ISO日期/时间和枚举；确定行序，不导出日均成本。设置白名单只含M09已实现的记录时区，排除权限、内部任务和缓存。[manifest](../../app/src/main/java/com/cooper/lithic/data/backup/BackupManifest.kt)独立使用format v1，明确版本、时间、记录数及实际文件长度/SHA。
- **实际文件IO**：[BackupEntryFiles](../../app/src/main/java/com/cooper/lithic/data/backup/BackupEntryFiles.kt)流式写data/settings，关闭后重新读取核验；内部ZIP写入器对实际拷贝再次计数/哈希，写入、flush、close或内容变化均不能返回成功。记录数达到200000、data达到128MiB等资源边界明确失败。
- **完整性门禁**：[BackupExporter](../../app/src/main/java/com/cooper/lithic/data/backup/BackupExporter.kt)的完整导出入口固定返回`MEDIA_INTEGRATION_REQUIRED`，连空账本也不打开目标。内部无媒体合成ZIP仅验证IO核心，不能作为用户完整备份。含媒体元数据但没有媒体字节的manifest生成被拒绝。

必要接点只有ManagedGeneration的只读工厂、SettingsStore同协调器内部读取，以及显式声明既有serialization-json 1.9.0；实际运行依赖仍122→122，0新增/删除/升级。没有修改Room schema/DAO、业务算法、UI、Manifest或generation切换协议。新增8份核心源码、4份JVM测试和1份Android测试源码；其余变更为上述接点和当前记忆/证据。

## 实际验证

| 检查 | 本轮结果 | 证据 |
|---|---|---|
| JVM完整回归 | **113 PASS，0失败/错误/跳过**；原86方法源码不变，新增27；源码方法与11份XML完全对应 | [结果和输入SHA](m11a/results.json)、[JUnit XML](m11a/jvm/TEST-com.cooper.lithic.data.backup.BackupFilesTest.xml) |
| Lint / Debug / Android测试源码编译 | **PASS**，Lint 0错误/7警告；完整重执行5m4s，随后仅测试文件末尾空行整理并四任务重验 | [完整重执行](m11a/final-verified.log)、[收尾重验](m11a/post-format.log)、[Lint](m11a/lint-results-debug.txt) |
| Android运行 | **NOT_RUN**；新增5项Room/DataStore场景只编译，旧463项也未重跑 | [Android测试源码](../../app/src/androidTest/java/com/cooper/lithic/data/backup/RoomBackupSnapshotSourceTest.kt) |
| APK静态核验 | PASS：包名、min26/target36、无网络/禁止权限、九域备份排除、无M10 Worker/Stage V2/JNI或JUnit打包；v2签名与M09证书一致 | [APK](m11a/apk-verification.json)、[证书](m11a/signature.txt)、[类/库检查](m11a/defined-classes-check.json)、[依赖](m11a/dependency-comparison.json) |

工具链保持JDK17.0.20.1+1、Gradle8.14.3、AGP8.13.2、Kotlin2.3.10，离线构建，无安装/升级。候选APK只在本地，未安装或提交：`E:/CODEX/.codex/worktrees/lithic-m11a/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`，12,427,572字节，SHA-256 `8824c3e301be8bd6aaf1e4a1dbe2e98f24c13a08599f623e29d4a75a6217ad41`；版本元数据沿用M09的code7 /0.1.0-m09-debug，不是正式发布。

**RED和中间失败不抹除**：[缺API编译RED](m11a/red.log)没有运行JUnit；[内部候选释放RED](m11a/red-candidate-lifetime.log)为11项中2失败。[首次全量](m11a/final.log)112项中1失败：Windows Basic fileKey实际为null，[实测](m11a/filekey-diagnostic.log)后改用私有随机凭据＋精确子项/内容核验，保留未知残留。随后[Lint失败](m11a/final-green.log)为忽略的local.properties转义；[再次任务](m11a/verified-build.log)复用了旧Lint报告。修正本地文件并强制重执行后取得最终GREEN。没有放宽旧测试断言。

## 未完成边界与接续

原M11-T01～T04本轮仅完成核心子集：字段/设置白名单、真实协调器并发、内部条目/manifest哈希及流失败传播；**原完整案例仍NOT_RUN，不登记整个M11通过**。真实Room/DataStore运行、媒体字节、SAF目标取消/失败删除、Android/真机/OEM、完整导出及恢复均未运行。

内部准备失败/取消可能留下未发布私有候选；成功对象有显式close，未知凭据/子项/变化拒绝删除。Windows null fileKey不是inode证明，逐文件清理非原子，也不是跨进程恢复或M10 GC方案。入口尚未开放，不用自动扫盘掩盖这些边界。

下一阶段须另行授权：在同一协调边界复制已核验不可变封面，或经审查的等价租约；覆盖所有媒体引用并核对字节/SHA/尺寸/格式/上限，继承Q05原字节保留，缺损即拒绝。媒体完整后才能接SAF、目标失败处理和正式UI，再执行完整M11-T01～T05与Android门禁。本轮提交后停止，不恢复M10实验、不进入M12。
