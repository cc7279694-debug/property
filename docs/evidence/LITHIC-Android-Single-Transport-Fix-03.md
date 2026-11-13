# LITHIC_ANDROID_SINGLE_TRANSPORT_FIX_03

**READY_FOR_SINGLE_TRANSPORT_ATTEMPT** · 2026-11-13

`codex/lithic-home-android-acceptance-02` 的本地及远端起点均为 `70a9fc72e5b32e64900a0616b8c5dcc545c0d633`，工作区干净。修正在 `codex/lithic-home-android-single-transport-fix-03` 完成。

## 上轮证据

独立 SDK ADB 5038 已核对 17641 监听属于本次模拟器。原准入约第 12.7 秒发出一次手动回环连接，6 秒后请求超时。约第 21.75 秒的只读列表同时列出 `127.0.0.1:17641 offline` 与 `emulator-17640 offline`。时间顺序和序列号与手动连接和模拟器自动注册重叠相符。现有证据不足以独立确认两个连接的来源或持续离线原因。[原始连接日志](lithic-home-android-acceptance-02/lowram/admission-commands.jsonl) · [上轮报告](LITHIC-Home-Android-Acceptance-02.md)

## 修正后流程

1. 未来获准执行时，控制器清除继承的旧 ADB 地址和端口覆盖，向指定模拟器显式传入 `ANDROID_ADB_SERVER_PORT=5038`，只使用独立 SDK ADB `127.0.0.1:5038`。准入沿用从启动起算的 180 秒总窗口。每条查询和进程归属检查均受独立超时及剩余窗口约束。
2. 准入模块只用 `host:devices-l` 等待自动注册。手动连接请求上限为 **0**，主机请求入口在建立 socket 前拒绝任何其他请求。`17641` 只用于核对 Windows 回环监听归属，唯一可接受的序列号是 `emulator-17640`。设备列表为空或目标持续 `offline` 时继续有界观察。
3. TCP 别名 `127.0.0.1:17641` 即使单独出现也阻断。多个连接、未知连接及非本次监听归属均阻断。只有唯一目标处于 `device`、本次 QEMU 归属通过后，才执行 `sys.boot_completed`、Shell 探针及实际 AVD 名称、QEMU 标志、API 36 和 x86_64 校验。准入返回前复核连接唯一性。未知设备序列仍脱敏。

主机诊断脚本沿用原实现，默认端点 5038 且只接受只读主机查询。准入仍拒绝共享端口 5037。

## 验证与待验范围

- 原固定提交的 17 项离线测试全部通过。修正后套件 22 项通过，继续覆盖原超时、身份、监听归属、协议和端口隔离；原手动连接相关断言按新合同改为验证禁止手动连接。[原套件输出](lithic-android-single-transport-fix-03/baseline-offline-tests.txt) · [修正后输出](lithic-android-single-transport-fix-03/offline-tests.txt)
- 新增覆盖空列表到唯一 `offline` 再到唯一 `device` 的成功路径、全程离线直至 180 秒、TCP 别名单独出现、双连接和身份检查期间出现第二连接。成功路径校验无手动连接请求。
- 三个脚本的 Python 语法检查、`git diff --check` 及生产、构建配置、上轮证据零差异核对通过。未重构建 APK。
- AVD 启动、ADB 实际自动注册、guest 健康、APK 安装、Instrumentation 及截图均为 `NOT_RUN`。现有日志只证明上轮生成的硬件配置为 1536 MiB／2 核，guest 实际内存及低内存下的稳定性仍待确认。
