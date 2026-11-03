# LITHIC V1-RC2 — 提醒入口审查与 Preview18

2026-11-03。用户转交 `8c79671960119bd836e95a45e30b4b1bde226eaf` 的 **RC1 ACCEPTED / FROZEN — PASS WITH NOTES（桌面）**；[RC1 原记录](LITHIC-V1-RC1-Reminder-Reliability.md)及其全部失败与未运行结果保留。本轮不改提醒产品源码，只将显式 Private Trial 从 code33/preview.17 递增到 code34/preview.18；普通 Debug、Release 和其他身份不变。

## 入口审查与未关闭 NOTE

`ReminderScheduling.start()` 丢弃周期入队 Operation，并调用同样丢弃一次性入队结果的 `request()`；后者还由 Ready 代变化监听、设置保存及设置页恢复前台调用。本机 WorkManager 2.12.0 字节码显示返回 Operation 后由执行器异步完成；[Android 官方 WorkManager 文档](https://developer.android.com/reference/androidx/work/WorkManager)也说明 Operation 用于确定入队何时完成。因此异步失败可能未被观察，设置页已有同步失败提示不能证明异步失败已反馈。`ensureChannel()` 同步失败还可能跳过同一 try 内的周期安排；具体设备是否发生过这些失败 **UNKNOWN**。

**本轮未修复这些入口，不能宣称全部提醒入口可靠。** 直接在初始请求时等待会推迟对 Room `emitInitialState=false` 变更的订阅；设置回调也不能在主线程阻塞。简单 timeout 后重发可能以 REPLACE 取消较新的工作；本机 `await()` 对结果 Future 的取消不证明底层入队或 Worker 已撤销。须另行设计和验证有界后台观察、明确失败/UNKNOWN 分类、跨代迟到结果与设置页反馈；仅在确知失败且当前 Owner 仍有效时考虑有限恢复。当前沿用原唯一名称、KEEP/REPLACE 和原产品规则，不增加循环、自行拉起或未知状态自动重试。RC1 续费先协调、里程碑入队等待及取消传播仍存在于新 APK，但不代表提醒送达或 Owner/任务终结。

## 本轮桌面验证

- [构建命令与结果](v1-rc2-20261103/build/command.json)：离线、单 worker、独立 `app/build/rc2-preview18/`，启动前可用内存 4.508 GiB；Gradle exit 0。实际执行 5 类 **80 项定向 JVM PASS**（0 失败、错误或跳过），[逐类 XML](v1-rc2-20261103/build/junit/)；覆盖现有同步失败、取消、调度编排、权限、旧代/重复发布以及停止/恢复规则。启动/设置入口真实异步入队失败、去重及旧 Owner 迟到的系统行为 **NOT_RUN**。RC1 118 项为先前证据，本轮没有宣称重跑全部 118。
- Private Trial Debug 构建、生产 Kotlin 与 AndroidTest Kotlin 编译及 Lint exit 0；[Lint XML](v1-rc2-20261103/build/lint.xml) 为 **0 错误、18 警告**。AndroidTest 仅编译，真实 Android/通知/设备测试全部 NOT_RUN。
- [APK 审计](v1-rc2-20261103/desktop-1/artifact-1/identity.json)和[执行回执](v1-rc2-20261103/desktop-1/execution.json) exit 0：实际 Manifest、Application/MainActivity、组件/Provider、权限、原证书和 native 边界；RC1 Worker/协调器及里程碑 Operation await 的生产 DEX；P3、B1/B3、R1、三主题和图标资源均核对。无 INTERNET、测试框架或测试 Runner 进入用户 APK。源码 `04cf20c5f600d0474f223c6d927e20d59b400211` / App 树 `fa84443f0569427390767fbc5df0d7f43b3a899a`；相对于 RC1 唯一 App 变动为两处体验版版本值。
- 35 份既有 APK 在构建、审计和归档前后按大小及 SHA 核对；Preview17 保留。没有设备、ADB、真实账本、签名密钥生成或旧包修改。未执行正式 Release、main 合并或照片 GC。

## 唯一私人体验候选

| 项目 | 实际值 |
| --- | --- |
| 绝对路径 | `E:\CODEX\.codex\worktrees\lithic-m13\砺石新APP\deliveries\lithic-preview-0.1.0-preview.18\Lithic-0.1.0-preview.18-debug.apk` |
| 包名 / 版本 | `com.cooper.lithic.preview.debug` / `0.1.0-preview.18-debug` / code34 |
| 大小 | 20,506,559 字节 |
| SHA-256 | `afa9b49da1952342a56403eaa5150261d641f3ef2f6cba68d43501e1555d8e4a` |
| Debug 证书 SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9` |

[归档回执](v1-rc2-20261103/desktop-1/archive.json)：候选与本轮构建产物逐字节一致，归档目录独占创建。APK 不提交 Git，不自动安装或覆盖。已有真实记录时应先保护备份；实际同包升级与数据保留仍 NOT_RUN。

## 历史发行风险

M20 通知窗口可见性两次历史 FAIL、第二次 Owner/周期任务 UNKNOWN 保留；系统调度、拒权/送达、进程重启和 OEM 表现待真实 Android 验证。M22 T06 三次 FAIL、T03 长历史与真实 IME/大字未关闭。C6 隔离运行、严格 JPEG 三项历史失败、备份恢复中断及同包升级仍未被本轮桌面结果关闭。历史 G1～G4 和其他 NOT_RUN/UNKNOWN 保持，V1 仍 **NO-GO**。

## 用户自行体验清单（以后自愿进行）

1. 安装前先保护现有个人记录和备份；按手机正常升级流程操作，不以卸载或清数据处理异常。
2. 查看物品、订阅、三主题及照片添加/更换/取消显示，确认原有记录与界面正常。
3. 用可丢弃数据检查备份文件和照片空间统计；不要把体验当作正式恢复或异常中断安全验收。
4. 自行查看提醒开关和权限反馈；实际通知出现时间、后台运行与重复情况需要在真实合法时间窗口另行观察。
5. 遇到异常或结果未知时保留现场，不重复提交；向独立审查反馈页面和实际结果。

**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING**。本候选仅供个人体验与独立审查，不是正式 V1 发布或 Android 验收通过。
