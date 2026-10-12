# 当前项目状态

状态日期：2026-10-12。唯一实时状态；归档同名文件与 checkpoints 是历史。

## Current Stage

- 当前模块 **M04 — ACCEPTED / FROZEN — PASS WITH NOTES**。来源为用户在当前会话明确转交的ChatGPT独立代码验收正式结论，仓库 `cc7279694-debug/property`、分支 `feat/m04-room-storage-foundation`、评审HEAD `1b7058ebb9dfbe904252a08efe6815d9952971ad`；回写前实际核对本地/远端一致且工作区干净。正式依据和NOTES见[验收登记](ACCEPTANCE_MATRIX.md#m04-独立代码验收与正式冻结记录2026-10-12)。原READY_FOR_REVIEW交付证据保留为历史，本次仅回写两份状态文档并提交/推送。
- M00～M03均 **ACCEPTED / FROZEN — PASS WITH NOTES**。评审HEAD依次为 `59ba290cba69e1e77af7d0376a8b43aaabbf5f88`、`ca8499e3d3cb2e5bac3ab29a231f399636899c19`、`ff1a898eaf4da5068c36af3e1c0bdcbe61c0b1f7`、`8b13d20dad89f9c3e76a3d19161cf2232b36b94d`；交付/补测/独立结论与NOTES保留在[验收登记](ACCEPTANCE_MATRIX.md)及历史checkpoints。
- M05～M23未授权、NOT_STARTED。本次验收回写后停止，不自动进入下一模块；M04冻结由用户转交独立结论授予，不是执行者自验。

## Verified / Completed

以下为M04既有实际交付与测试证据，本次回写不重跑Gradle、构建或设备验证。独立审查确认schema/六项案例、86方法与86条PASS对应、74 JVM证据保留及16关键源码/测试/配置SHA-256独立一致，未发现阻塞性代码问题；独立审查未在用户Windows本机重跑Gradle或安装APK，不能据此新增执行PASS。

- Room四表v1、DAO、3外键/7索引（3唯一）、两个独立类型“其他”、空迁移注册入口；实际KSP schema导出已纳入交付。不保存派生成本或第二份购买价，不建未来订阅/替换/通知表。
- ProjectWriteRepository：项目/初始事件同一Room事务、ABORT中途失败完整回滚、同操作精确重放/异内容冲突、Money/日期/UUID及事务内类别类型校验。LedgerReadRepository为同一次Room四表读取事务；共享LocalWriteCoordinator单Mutex，不宣称跨存储原子性。
- noBackupFilesDir/generations/UUID：同目录DB/WAL/SHM、media、settings.preferences_pb、ready，全部准备后才AtomicFile激活。重开校验持久身份；指针/设置/数据库异常明确失败、保留原库和旧代。既存库在可写打开前只读验证真实SQL版本/Room身份，支持WAL并拒绝v0/异库；腐败回调不删除原库。
- SettingsStore时区持久化、设备变化不覆盖，canonical文件单一owner，关闭取消并等待scope后释放；存在性竞态/IO返回取消泄漏已复现修复。Application启动时捕获zone、后台单任务初始化，明确Ready/Failed并传播失败，不自动重试。M01占位页与既有领域规则保持。
- 完整命令 `:app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug --max-workers=2` 退出0：**74 JVM＋86 Android全通过，0失败/错误/跳过**，原74方法一一对应。Android仅专用API36 `emulator-5596`，真实SQLite/DataStore；M04-T01～T06全PASS。
- 真实RED：61项/55断言失败、补充20/19、生命周期3/3、启动/既存库9/8；已有正确行为直接回归。两次测试夹具编译问题不计RED。GREEN86项全PASS；Lint **0 errors / 13版本更新提示**，未抑制/升级。精确新增依赖和实际传递差异见[工具链锁](TOOLCHAIN_LOCK.md)。
- 最终APK `E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`，11,809,696字节，SHA-256 `6a9f67a6fb84da44bdca1198a85afe1c1ca5aecc80042fa328bf7f50d3a00a10`。com.cooper.lithic.debug，code2/0.1.0-m04-debug。APK/合并Manifest/编译备份排除/Wrapper与v2签名检查PASS，仅原签名级内部接收器权限，测试框架未打包。
- 最终APK专用模拟器安装和两次断网COLD/Status ok启动PASS；重启active身份/ready/设置哈希一致，安装APK与本地哈希相同，无当前进程崩溃，占位UI保留，网络状态恢复。未操作手机/其他项目AVD，未公开上传APK。
- ZIP CRC/安全路径/43原件字节及SHA核对PASS；原130唯一案例全NOT_RUN，阅读版、16组完整跨模块补测与历史证据保持。详细结果、54项构建输入哈希见[JSON](evidence/M04-verification.json)、[报告](evidence/M04-verification.md)与[M04 checkpoint](checkpoints/2026-10-12-M04.md)。

## Pending / Current Problems / Technical Debt

- M04独立代码验收已完成并正式冻结；保留 **本次独立审查未在用户Windows本机重跑Gradle或安装APK** 的NOTES。本次Codex仅状态回写，同样不运行这些验证；原执行证据与独立审阅分别登记，Codex内部检查不替代独立验收。
- Q01 CLOSED，Q02/Q07 USER_CONFIRMED；Q03长期100/1000日节点仍M21前，Q04提醒资格仍M20前，Q05图片往返仍M10/M12前。Q06 **ENGINEERING_REQUIRED**：仅M04设计/初始实测完成，M12导入/切换/回退/杀进程/共同校验/清理及Q06-V01～V03仍NOT_RUN，不能CLOSED。Q02 M17/M18及16组完整跨模块补测仍NOT_RUN。
- [M09两项流程风险](SPEC_ISSUES.md)保留：STOPPED重新启用入口/历史/纠错、多期预付默认停止选择；当前未改产品语义，流程NOT_RUN。
- 真实手机、API26运行、OEM备份仍NOT_RUN。M01独立审阅未重新安装、M02/M03独立审查未在用户Windows本机重跑Gradle等历史NOTES不由本轮回归改写。
- 首次初始化中断后残留generation须保持安全处理，不得静默清除真实数据；本次未新增清理/恢复能力或故障验证。M08真实v1→v2迁移须同步调整既有数据库版本预检，仍未实施/NOT_RUN。
- 正式存储错误界面/业务CRUD UI尚未实现；M05必须明确呈现初始化失败，不得显示虚假的正常空账本，此流程仍NOT_RUN且本次未进入M05。媒体M10、备份format v1 M11、正式恢复M12均未实施/NOT_RUN。TypeScript/ESLint不适用；NO-SOURCE不计PASS。13条Lint版本提示不作为普通模块升级授权。

## Data / Architecture

实际schemaVersion=1，backupFormatVersion尚不存在。四表真实事实；LocalDate为Long epochDay、Instant保留纳秒，Money/BusinessClock继承M02。pointer/ready内部formatVersion=1不是备份版本。目录/生命周期见[ARCHITECTURE](ARCHITECTURE.md)，不自动备份/清理旧代，不宣称跨文件原子事务。

## Git

- 目录 `E:/CODEX/砺石新APP`；origin `https://github.com/cc7279694-debug/property.git`；当前 `feat/m04-room-storage-foundation`。M04实施基线/来源 `ffd066ffe0efb6c00f35526ae792b50829d3230e` / feat/m03-subscription-engine；原交付/本次评审及验收回写起点均为 `1b7058ebb9dfbe904252a08efe6815d9952971ad`，本次开始本地/远端一致且工作区干净。
- 本次仅更新CURRENT_STATE与ACCEPTANCE_MATRIX，提交/推送当前M04功能分支。提交自身SHA、推送与最终工作区/远端核对以Git日志和最终报告为准，避免提交哈希自引用；原130案例、历史交付及未运行验证保持。
- 保护基准：main `145326ddd4ce28abbf7c2706fe25f9d38c599c3a`、M02 `0169c76bbf0946a0400b96d00124b3de9aea1cc0`、M03 `ffd066ffe0efb6c00f35526ae792b50829d3230e`。不合并main/force push/建PR/Release/改WHOCARE，不提交APK、实际库、私钥或个人数据。

## Next Recommended Task

本次验收回写完成后停止。M05的M01/M04验收前置已满足，仍须用户另行授权；未来须明确呈现存储初始化失败，继承schema v1、记录时区与真实空数据，不复制公式或伪造空账本。本次不开始M05。
