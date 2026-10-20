# M12安全恢复候选 — 集中交付

状态：**READY_FOR_REVIEW（候选）；M12未冻结**。来源 `635e33a57c66c286fa5c19f009a5565a1e70b8e2`，分支 `codex/m12-safe-backup-restore`。用户转交的M11 ACCEPTED / FROZEN — PASS WITH NOTES已登记；原历史不改。M10 PENDING / DEFERRED、UI-C1冻结、原三JPEG FAIL和所有NOTES保留。

## 实际交付

- [验证器](../../app/src/main/java/com/cooper/lithic/data/backup/BackupValidator.kt)：受控私有ZIP副本；中央/本地/完整压缩流、重复与越界路径、长度/CRC/SHA、版本/预算、严格JSON和七表业务关联，复用冻结引擎检查溢出。额外解析准入为UTF8名称4096B/单段255B/中央元数据16MiB，不是整个进程内存硬上限，也不改format1。
- [恢复服务](../../app/src/main/java/com/cooper/lithic/data/backup/RestoreCoordinator.kt)、[新代写入](../../app/src/main/java/com/cooper/lithic/data/backup/RestoredGenerationWriter.kt)：预览绑定旧事实/设置CAS与来源身份；明确确认才填充独立空Room2和原字节媒体，关闭/fsync/重开验证后才获激活资格。令牌一次性、不跨进程自动重放。PNG/JPEG保持M11已登记字节口径，不宣称M10严格熵解码。
- [代切换](../../app/src/main/java/com/cooper/lithic/data/backup/DataGenerationStore.kt)：同代写协调器退休，AtomicFile持久SWITCHING→STABLE；首次打开失败可ROLLED_BACK并显示警告。未知指针拒绝，不生成空账本。后续稳定启动先完整验证当前代，再持久解除唯一前代指针后清理已证明归属的旧文件；未知子项整代保留，失败残留不猜测删除。
- 设置页真实系统选择→七表数量预览→取消/明确覆盖→结果反馈；新代重建Nav3/子ViewModel作用域、拒绝旧回调。关闭与恢复共用生命周期锁。没有恢复合并、自动备份、CSV、云或新权限/依赖/schema。

## 真实验证

| 项目 | 实际结果 |
|---|---|
| 定向JVM | **96 PASS**：33个新M12方法＋63个受影响回归；0失败/错误/跳过。[XML与计数](m12/jvm-results.json) |
| API36 Android | **74个唯一方法PASS**：35新增＋39回归。73项使用Test `88e9c1a…`；真实SAF最终1项使用Test `d426c58e…`。生产App始终同一最终字节；不是一场单一APK全量测试。[逐类结果](m12/android-results.json) |
| Lint/构建 | Lint **0错误/9警告**；离线JVM/Lint/Debug/Test构建成功1m18s；之后只修测试选择器并重建Test/Lint成功40s。[构建](m12/final-green.log)、[测试驱动构建](m12/saf-picker-fix-build.log) |
| 完整往返 | 实际Room七表、设置、PNG/JPEG及未引用登记媒体原字节，恢复→再导出→重开；空媒体合法；按观察日重算，不复制日均缓存 |
| 失败保护 | 损坏/缺图/路径/新版本/外键/溢出、候选写入失败、预览后CAS变化、切前/切后/首次打开异常、未知旧代保留、旧回调/关闭并发均有定向证据 |
| 真实SAF | **PASS**：从系统Downloads选M11的3691B合成备份，预览取消不改旧集，再选择并确认，七表和GMT设置相等，五张JPEG/合法PNG（含palette/gray16/Adam7）SHA/字节一致；3类别→2类别证明覆盖不合并。[证明](m12/saf-restore-proof.json)、[实际截图](m12/saf-restore-success.png) |

原始RED和失败全部保留。[历史索引](m12/failure-history.json)包括压缩流夹带1FAIL、ZIP资源2FAIL、旧回调/关闭竞态/旧代清理真实Android RED、JUnit初始化错误，以及真实SAF两次失败。SAF先因无Compose根立即失败，修正等待后又超时；真实[系统界面树](m12/android/saf-picker-dump-content.txt)证实旧驱动把`preview_icon`误当文件。最后只修测试为精确标题→`item_root`，保留两份旧claim，严格核对旧集＋唯一原哨兵后追加第三claim，成功证据独立，不覆盖旧FAIL或重复插入。

## 原案例边界与剩余门禁

- M12-T01/T02/T05/T06：上述候选范围PASS。T03的受控IO/事务/校验失败保旧PASS；真实磁盘耗尽、永久阻塞提供方仍NOT_RUN。
- **M12-T04和Q06-V01～V03完整进程终止门禁NOT_RUN**：本轮仅授权非杀进程验证。异常检查点/手工持久SWITCHING重开不能代替真实kill/断电；这是正式冻结前仍需独立决策的安全门禁。
- API26、真机/OEM、Release、完整TalkBack、LMK、真实断电/权限撤回NOT_RUN。准备中断的未知缓存/未发布新代保守保留；多份副本需要额外磁盘空间；SAF阻塞取消不保证即时。后续稳定代出现损坏不能静默回滚到含陈旧事实的前代。未宣称恢复所有格式语义/20年规模。
- Q05仅登记M11→M12现有媒体原字节子集实测，不关闭M10导入及完整跨模块边界。Q03/Q04原期限和全部旧NOTES不变。

## 产物、设备与Git边界

App：`app/build/outputs/apk/debug/app-debug.apk`，**12,910,837B**，SHA256 `3bb0d801300889d5c97f85dd768a14701f3a3f67aacd55dca929aa86e8233107`。当前Test：**2,281,085B**，SHA256 `d426c58ec36bb5505c6272c5b3f88037a55d9eb397b4a0ef55ba2070f82c098e`。[签名/Manifest/权限审计](m12/apk/apk-audit.json)。只保留当前本地APK，不提交二进制；四ABI只继承原graphics.path/DataStore库，无M10 JNI/Worker。

只用经用户授权的原M11合成API36 `emulator-5686`，原launcher1284/QEMU29348、boot身份一致；新建/启动/退出AVD均0，App同签名更新3次、Test7次，未卸载/清数据/杀进程。最后新代 `b3f76bf5-27f9-4cc4-b39c-d6feea2f2457` STABLE，前代保留待后续稳定启动清理；M12缓存未观察到残留，测试包PID8987保留，未擅自停止。M11源备份/证据/claims原样相同；仅合成哨兵在确认覆盖后从新代排除。H2/C2/M10/其他设备与真实用户数据均未访问。

[执行汇总](m12/verification.json)、[实际定向命令/退出码/字节SHA](m12/device-commands.json)、[20份生产输入哈希](m12/production-inputs.json)、[保护范围](m12/protected-inputs.json)。JDK17.0.20.1+1/Gradle8.14.3/AGP8.13.2/Kotlin2.3.10沿用；没有下载/升级。原schema、DAO、领域、Gradle/Manifest、format DTO、规划归档及M11历史证据无修改。仅提交推送本候选，禁止main合并/发布；停止等待ChatGPT独立验收，不进入M13或恢复M10。
