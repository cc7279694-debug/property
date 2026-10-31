# LITHIC V1-A1 — Preview12 User 0 安装状态限定补验

2026-10-31（Asia/Shanghai）；分支 `codex/lithic-v1-product-completion`；实际基线 `64badfe3f211b988138dab5b057c52413e005880`。开始时 HEAD/远端一致、工作区干净。本轮仅补查指定包的 User 0 安装字段，原[Preflight 报告](LITHIC-V1-A1-Device-Preflight.md)及当时的 UNKNOWN 原样保留。

**PREVIEW12_USER0_INSTALLED — CONFIRMED**

## 本轮实际证据

| 项目 | 本轮观察 |
| --- | --- |
| 目标识别 | 只通过已有 `127.0.0.1:5037` smart socket 执行一次 `host:devices-l`；清单 1 项，恰好 1 项匹配已授权 USB 序列 SHA-256 `e4f0e1d2361b0ef560ff8ac99a067fb8f540594c827d28b6cd21a720e724af92`，状态 `device`。未保存原序列或清单原文；未查询其他设备 |
| 唯一设备查询 | 精确 `dumpsys package com.cooper.lithic.preview.debug`，在设备内存中按 `Packages:` → 精确 `Package [com.cooper.lithic.preview.debug]` → `User 0:` 过滤；只回传必要字段，不传输或保存完整转储 |
| 安装字段 | Package 匹配 1、User 0 匹配 1、installed 字段 1；实际值 **`installed=true`**，不是根据 `-u` 包记录、UID、版本或其他用户推断 |
| 完整结束 | dumpsys 源退出码 **0**、过滤器退出码 **0**、shell-v2 实际退出码 **0**；收到结束包，stdout **180 字节**，stderr **0 字节** |
| 输出 SHA-256 | stdout：`44155109895eb5226b0051c29d94e66223c992dfa132fd933f1879de7bb8c530`；空 stderr：`e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` |
| 次数与时间 | 主机枚举 1 次、目标只读 shell 查询 **1 次**、自动重试 **0**；会话 0.422 秒，2026-10-31 10:17:24（Asia/Shanghai）。未进行复核查询 |

最小原始回传如下；完整只读命令、主机 OKAY 回复摘要、真实退出状态、长度和 SHA 见 [observation.json](v1-a1-user0-supplement-20261031/observation.json)。主机协议不是 ADB CLI 子进程，没有可冒充的进程退出码。

```text
PACKAGES_SECTIONS=1
PACKAGE_MATCHES=1
USER0_MATCHES=1
INSTALLED_FIELDS=1
INVALID_FIELDS=0
PACKAGE=com.cooper.lithic.preview.debug
USER=0
INSTALLED=true
SOURCE_EXIT=0
FILTER_EXIT=0
```

## 读取与证据保护

- [一次性读取器](v1-a1-user0-supplement-20261031/query-user0-once.py)只允许上述一个设备命令；[独占会话标记](v1-a1-user0-supplement-20261031/session-claim.json)在连接前创建，禁止同一读取器再次执行。源命令失败、明确转储超时、字段不唯一、其他用户/包、截断或歧义均不能输出 CONFIRMED。
- 执行前使用已有本机 awk 验证合成输出：首轮 [23 项](v1-a1-user0-supplement-20261031/offline-checks.json)，补强尾部超时/服务错误拒绝后最终 [25 项](v1-a1-user0-supplement-20261031/offline-checks-final.json)通过；均为纯主机过滤检查，**不是 Android 测试**。未通过手机命令试探过滤器。
- 未使用 ADB 可执行文件，未启动、停止、修复或重连 ADB 服务；只连接已在运行的服务。执行后读取器未修改，也未补发查询。
- 越界查询 **0**、设备写入 **0**。未读取 RAM、存储、电量、API/ABI、Boot、SystemUI、JobScheduler、其他包、安装 APK 字节或任何 App 私有数据；无日志、屏幕采集、设置更改、App/Service/Worker/Instrumentation 启停、安装、卸载或清理。
- 本轮只新增本文和小型脱敏证据；产品/测试/Gradle 源码、Preview12/V1-A1/其他 APK 及历史报告未改。本轮未重新读取 APK 来重复前次哈希检查，不能把历史字节证据写成本轮实时验证。

## 尚未获证或授权

**SYSTEMUI_MANUAL_HEALTH — PENDING_USER_CONFIRMATION**：截至本报告，尚无用户实际操作反馈。正常解锁/桌面、通知栏展开收起、点击滑动响应及无系统无响应弹窗仍待用户自行检查；不把计划检查登记为通过。

本轮仅确认 Preview12 在 User 0 处于安装状态，**不证明手机 APK 字节、账本完整性或 SystemUI 无 ANR，也不将整个 V1-A1 设备准入提升为 READ_ONLY_PASS**。前次 V1-A1 两包不存在、资源及进程等结论仍只是前次观察，未重复采样；运行时 UID/数据隔离、正常主进程、plain 测试宿主、业务/备份恢复/升级与 Android 验收均未执行。

**V1_A1_INSTALLATION_NOT_AUTHORIZED**：本次未获安装或测试授权。提交推送后停止，等待 ChatGPT 独立审查，不自动进入下一阶段。

**V1_RELEASE_NO_GO / ANDROID_RUNTIME_ACCEPTANCE_NOT_RUN / ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED**
