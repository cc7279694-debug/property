# LITHIC V1-RC4 — 两项安全阻断的续修

2026-11-04。基线 `354b99d2434cca650e9d90468ab9de6de3869af0`，分支 `codex/v1-rc4-reminder-coordinator`。结论：**BLOCKED — PREVIEW19_NOT_BUILT**。原 [RC4 报告](LITHIC-V1-RC4-Reminder-Coordinator.md)和历史结果保留。

## 本轮实际修改

`ReminderOwnerBarrier.finishClose()` 在关闭已开始后不再获取入队锁，因此屏障清理本身不会等待卡住的同步入队。`activate()` 在发布后复核终止状态，避免与关闭竞争后永久留下有效资格。新测试以有界等待复现原实现的阻塞；修复并不取消已准入的系统请求，也不证明系统事务结束。

独立源码复核指出：`AppContainer.close()` 在并发恢复已持有 `restoreLifecycle` 并等待 `revokeForSwitch()` 时仍可能等待；入队前已准入的旧调用也可能在关闭后继续。`activate()` 与 close 竞争可能短暂发布一个已失效 lease，运行时入口检查其 `isActive()`，但该竞争尚无真实 Android 证据。本次局部修正不等于完整关闭或恢复合同通过，源码仍不得安装、合并或用于真实数据。

## 未解除的阻断

1. **跨进程 UNKNOWN。** 现有协调器状态仅在进程内；进程结束后，新进程无法辨别之前系统入队是否已生效，启动刷新仍可能提交 REPLACE。现有回归 `new_coordinator_does_not_inherit_old_unknown_process_death_remains_blocked` 是记录缺口，不是安全通过。
2. **恢复切换等待。** `revokeForSwitch()` 仍持有原有屏障协议：同步入队卡住时恢复可能无限等待。将调用简单移到锁外，会在资格检查和系统调用之间引入退役后首次提交的竞态；将超时或协程取消解释为系统事务撤销也不成立。

可能的跨进程持久化方案需要分别证明：写入标记与实际入队之间的崩溃窗口；旧版本首次升级与标记丢失的区别；文件原子替换、目录持久化、损坏/回退；可信终态和迟到回调；以及与备份、恢复的隔离。仅补一个可丢失的标记不能解除门禁。要同时实现无界调用下的非等待关闭和严格阻止退役后首次提交，还需要重新审查系统提交与退役的线性化协议，或对行为合同作明确的独立决策。

## 验证与边界

- 首轮测试命令因未设置本地 JDK 退出 `9009`；第二次因 PowerShell 拆分参数退出 `1`，均未执行测试，[日志](v1-rc4-repair-20261104/red.log)与[日志](v1-rc4-repair-20261104/red-2.log)保留。配置既有 JDK 并正确引用参数后，新测试在修复前实际失败，见[日志](v1-rc4-repair-20261104/red-3.log)与[JUnit 原始结果](v1-rc4-repair-20261104/red-test.xml)。
- 修复后离线、单 worker 执行 12 个受影响测试类，**150 项 JVM PASS，0 失败/错误/跳过**；生产 Kotlin 编译通过，AndroidTest Kotlin 任务为 UP-TO-DATE；Lint exit 0，**0 错误、20 警告**。完整[构建日志](v1-rc4-repair-20261104/verification.log)、[JUnit 结果](v1-rc4-repair-20261104/junit/)和[Lint XML](v1-rc4-repair-20261104/lint.xml)保留。构建前可用内存约 4.61 GiB；执行期间曾下降，不另行启动构建。上述测试只证明其覆盖的 JVM 行为；不代表 Android 系统行为通过。
- [历史 APK 检查](v1-rc4-repair-20261104/protected-apks.json)：RC2 受保护清单 35 份及 Preview18 共 36 份，大小与 SHA 均一致。没有生成 Preview19。

未构建或归档 Preview19，未更改体验版版本、签名和历史 APK；未操作手机、ADB、模拟器、真实通知或账本。M20 历史 FAIL/UNKNOWN 和所有 Android 测试 NOT_RUN 保留。Room5、backup4、媒体、主题、金额及订阅规则未修改；不宣称恢复安全已经通过。

**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING**。
