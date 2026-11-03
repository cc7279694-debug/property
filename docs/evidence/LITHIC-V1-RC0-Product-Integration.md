# LITHIC V1-RC0 — 产品整合与发行差距

2026-11-03；用户转交对 `0e9b3610ad306ffd9b266022a672c580945fbc1c` 的 **V1-B3 ACCEPTED / FROZEN — PASS WITH NOTES（仅桌面）**。原报告、历史失败和用户自述不改写；正式V1维持 **NO-GO**。本轮只递增显式Private Trial两处版本，生产功能/测试/Room5/backup4/Owner/恢复/冻结算法/媒体协议未改，不提供永久照片删除或GC。

## Preview17 交付阻断

**BLOCKED — STATIC AUDIT BRANCH ASSERTION；没有可交付候选。** 本轮审计脚本在读取新APK之前错误地要求旧分支`codex/v1-b3-photo-storage`，但本轮按独立模块建立并使用`codex/v1-rc0-product-integration`；实际为审计脚本的错误预期，不能据此判定APK有缺陷，也不能判定其通过。原始脚本、exit1和[失败堆栈](v1-rc0-20261103/desktop-1/artifact-1/failure.txt)与[回执](v1-rc0-20261103/desktop-1/artifact-1/result.json)保留。按用户“门禁失败不生成候选”的要求停止；没有修改断言、重试审计或归档交付。Manifest、证书、权限、DEX及新APK实际SHA均 **NOT_RUN**，不提供可安装验收结论。

源提交 `071fa082cb5e28cf21abaa64db4bda99109596fa`、App树`1fce8ed5a71936bd5981e464cfd81e386a38dbe3`；仅Private Trial更新为 `com.cooper.lithic.preview.debug`、code33 / `0.1.0-preview.17-debug`，为配置意图，尚未经实物审计。普通Debug、Release和其他实验身份保持。配置检查核对独立输出路径后，构建输出到`app/build/rc0-preview17/`；该目录现有构建中间APK，属于**未审计产物**，未复制到`deliveries`、不作为候选交付，也未清理。Preview16和其他旧文件未覆盖。

离线构建和体验版Lint实际exit0，Lint 0错误18警告；命令前RAM 4.484 GiB。构建用原JDK/SDK、单worker和新输出目录，不操作设备。[配置回执](v1-rc0-20261103/desktop-1/config-1/command.json)、[构建回执](v1-rc0-20261103/desktop-1/build-1/command.json)、[Lint](v1-rc0-20261103/desktop-1/lint/result.json)、[版本差异](v1-rc0-20261103/desktop-1/source-checks.json)。32份已存在APK大小/SHA在构建后重新核对保持原字节。

另有一次[只读预探](v1-rc0-20261103/desktop-1/exploratory-precheck.json)：对旧普通Debug APK查询照片Reader类返回exit1，之后对已锁定B2 QA包的相同类查询exit0；该预探不是Preview17审计或产品失败。用户授权后如另立限定修复任务，应明确修正审计脚本实际分支预期并保留本次失败，重新独立核验真实APK的全部身份、安全与DEX门禁；不可将这份未审计构建产物当作已通过候选。

继承已接受B3的**62 JVM PASS、8项合成内存SQLite PASS、AndroidTest源码编译和普通Debug Lint 0错误20警告**；原测试/源码未改变，本轮不重跑，不把历史证据算作本轮新执行。B2 A～H仅为 **USER_REPORTED_MANUAL_PASS_WITH_NOTES**，没有逐项原始记录或独立复验；不扩大为Preview17、异常中断或自动测试PASS。

## 正式发行差距

| 状态与范围 | 已有事实 | 仍需证据／处理 |
| --- | --- | --- |
| 已验证：桌面 | B3只读统计的62项JVM和8项宿主SQLite通过；B1备份检查及P3照片安全边界保留。[B3](LITHIC-V1-B3-Photo-Space-Management.md) | 不代表Android Room、系统文件选择器、恢复或空间回收已通过。 |
| 用户人工确认 | 用户反馈独立B1-QA的A～H正常。 | 用户报告不是逐项日志、异常中断测试、逐图字节一致性或当前Preview17验收。 |
| 历史FAIL：M10 JPEG | 同一锁定缺损JPEG涉及三项历史失败；严格研究候选的限定样本证据保留。[C6-2A](LITHIC-C6-2A-JPEG-Admission.md) | 生产严格JPEG安全门禁未关闭；系统缩略图转PNG的P3实验不能改判严格熵流完整。 |
| 历史FAIL/UNKNOWN：M20 | 真实平台通知窗口历史两次失败；局部调度证据不抹去原始结果。[M20](M20-Android-verification.md) | 指定通知、Job及Owner收口和厂商后台表现仍UNKNOWN/待验。 |
| 历史FAIL/NOT_RUN：M22 | 137 JVM和36 Android历史PASS保留；T06三次历史FAIL保留。[M22](M22-T06-Physical-Verification.md) | 最近一次因离线门禁未通过未启动T06；正式20年T03、真实IME/200%字体及兼容性仍NOT_RUN。 |
| NOT_RUN：C6隔离 | 独立环境不满足条件，8项Android测试未运行。[C6-2D](LITHIC-C6-2D-Runtime-Verification.md) | 独立UID、FD、Guard、Binder、终结和资源收口仍UNKNOWN。 |
| NOT_RUN：恢复和升级 | 完整备份/恢复已有桌面及历史限定证据。 | 真实异常中断、跨代Owner收口、逐图字节比较、Preview同包覆盖升级及正式签名仍未完成完整独立验证。 |
| G1～G4未关闭 | G1核心闭环、G2浏览分析、G3提醒、G4升级发行；相关模块局部历史证据保留。[原门禁定义](../planning/original-v1/lithic_v1_plan/docs/MASTER_PLAN.md) | 局部PASS不等于总门禁PASS；不得自动进入M23或发布。 |
| 建议专项延期，待产品确认 | 历史照片逐张查看、永久删除/GC、严格JPEG写入和更广API/OEM覆盖。P3单项目照片添加/更换/取消已存在，限定API36系统Picker。 | 建议不是自行批准的正式版本降级；先完成删除崩溃恢复、备份一致性、设备资源及安全验证。[B3](LITHIC-V1-B3-Photo-Space-Management.md) |

本轮不操作ADB、手机、模拟器、用户账本或真实SAF，不自动安装、不合并main、不生成正式Release或Preview17交付候选。**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING**保持，停止等待独立审查。
