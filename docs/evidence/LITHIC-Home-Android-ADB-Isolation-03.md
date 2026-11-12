# LITHIC_HOME_ANDROID_ADB_ISOLATION_03

**ADB_ISOLATION_READY** · 2026-11-12

开始时 `codex/lithic-home-android-acceptance-prep-02` 本地与远端均为 `24bcfe33f8d1b70617f1ba111e63c8a2c398663c`，工作区干净。本轮独立分支 `codex/lithic-home-android-adb-isolation-03`，脚本固定提交 `1c42fcc6b0d9c91f7aecb0b282073912368e89fc`。

## 主机实测

| 阶段 | 结果 |
| --- | --- |
| 5038 可用性 | 启动前无监听或占用。`Get-NetTCPConnection`、进程枚举及 `netsh interface ipv4/ipv6 show excludedportrange protocol=tcp` 通过，5038 不在 IPv4/IPv6 排除段内。`127.0.0.1` 和 `::1` 的独占绑定探针均成功并关闭。 |
| 原 5037 | 回环监听，PID **13756**。`host:version` 报告协议 41（`0029`），`host:server-status` 报告 **36.0.1-13811061** 和 `E:/向日葵/Oppo Connect/daemon/bin/adb.exe`。CIM 未提供该进程的映像路径，程序路径来自服务自报。 |
| 启动 SDK 服务 | `E:/CODEX/MirraAndroid/sdk/platform-tools/adb.exe -P 5038 --one-device <本轮无匹配 USB 标记> start-server`，退出 0，2.219 秒。新监听限于回环，PID **18032**。CIM 映像路径、进程标记和 ADB 服务自报一致，均为上述 SDK 路径。服务端 **37.0.1-15733141**，协议 41。 |
| 空设备列表 | `adb -P 5038 devices -l` 退出 0，只有列表标题。5038 的 `host:version`、`host:server-status`、`host:devices-l` 均返回 `OKAY`，主机查询也返回空列表。 |
| 安全关闭 | 重新核对监听、PID、创建时间和 SDK 身份后执行 `adb -P 5038 kill-server`，退出 0。5038 监听及 PID 18032 均已消失。5037 的 PID、创建时间、监听及服务身份与启动前相同，只进行了身份查询。 |

本次选择独立 `ANDROID_USER_HOME`、空的额外密钥目录和无匹配 USB 标记，并关闭 mDNS 自动连接及模拟器自动扫描。启动子进程清除了继承的 ADB 地址和端口覆盖。ADB 密钥未进入 Git。[服务生命周期、命令、时间及原始结果](lithic-home-android-adb-isolation-03/service-lifecycle.json)

## 脚本变更与验证

- `home_android_admission.py` 支持配置回环 ADB 主机与端口，默认 `127.0.0.1:5038`。在建立连接前拒绝 5037、无效端口和非回环地址。主机查询、一次连接请求和只读 Shell 都走同一个已校验的服务。180 秒准入、单次连接、设备身份和监听所有权检查均保留。实际地址写入 claim、命令日志和最终结果。
- `home_android_host_diagnostics.py` 默认查询 5038，可指定主机与端口。诊断只允许版本、服务身份和设备列表三种只读主机请求。5037 的前后对照属于诊断，准入模块拒绝该端口。
- `test_home_android_admission.py` 共 **17 项离线测试通过**，原 13 项全部保留。新增覆盖 5038 主机与 Shell 地址、自定义回环端点、5037 禁用、非法端口和非回环主机。[测试输出](lithic-home-android-adb-isolation-03/offline-tests.txt)

## 后续验收接线

本轮 5038 服务已经退出。后续经授权运行 Android 验收时，应先按本报告复核并启动独立 SDK 服务。在本次启动器环境清除继承的 `ADB_SERVER_SOCKET`、`ANDROID_ADB_SERVER_ADDRESS` 及旧端口覆盖，给 Emulator 显式传 `ANDROID_ADB_SERVER_PORT=5038`，并将 `adb_host="127.0.0.1", adb_port=5038` 交给准入模块。后续 SDK ADB 命令明确指定 `-P 5038`，设备命令另加已核对的 `-s`。本报告覆盖 PREP-02 中关于使用已有 5037 服务的历史步骤。

`git diff --check` 和 `app/` 相对基线零差异核对通过。Kotlin 未重新编译。未启动或创建 AVD，未连接设备、安装 APK 或执行 AndroidTest，真实 Android 准入仍为 `NOT_RUN`。上次 guest 注册超时根因仍未确认，本轮验证的是独立主机服务及脚本端点隔离。
