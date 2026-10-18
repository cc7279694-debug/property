# M10 Android 验收监督器整改与续接

**BLOCKED — WINDOWS_QEMU_IDENTITY_FIRST_SAMPLE_NULL**。来源 `939e88b3fc79f3b9fd160737022f3bd795a07f37`；本分支 `codex/m10-jpeg-android-isolated-resume`。监督器整改与宿主验证完成，Android 验收没有执行。只等待 ChatGPT 独立审查，不授予 M10 冻结。

## 改动与验证

- `avd_identity.py` 使用上一提交保存的真实 CLI config/descriptor 回归，移除必有 `AvdId` 的假设。`<build>` 仅表示启动前待解析身份，`<temp>` 仅表示未证明位置的临时分区。继续核对唯一注册、精确数据根、API/ABI/tag/镜像及 SHA、重复 key、未知占位项、越界与链接。API36 r7 按实际官方 `data/empty_data_disk` 布局处理，不伪造 userdata 模板。
- `validate.py` 新增 API26 静态接管，重新实读原 4 文件逐字节/size/SHA、无未知子项及进程/端口后，独占写入新的 `adoption-proof`；不补写旧 create-proof。启动明确指定 `-datadir/-data/-cache/-no-snapshot`，保留固定 300 秒总期限和单次 marker；运行路径仍须实际核验。
- `natural_tests.py` 实现固定 9 方法的逐项执行、每次 serial/API/APK/签名审计来源/已装 SHA/UID/实际 instrumentation 目标核对、不可覆盖 marker、严格 instrumentation 协议解析和失败停止。`device_evidence.py`、`lifecycle_evidence.py` 实现有界退休日志、合成凭据/guard 元数据、目标 UID 与独立 `/proc` 观察；不读取 guard、账本或图片字节。证据等级区分真实日志、结构化观察与已执行源码断言，不补造原始 HELLO/FD 轨迹。
- `normal_shutdown.py` 仅在 API26 全部 9 门禁和无未知消费者条件成立时，允许一次该新设备的正常 console 退出并确认 OS/端口结束，再开放 API36。**本轮未调用此入口**。
- 最终 **191 唯一 Python 方法：189 PASS / 2 SKIP / 0 FAIL**；原 22 项监督器身份保留。2 SKIP 是本机没有 symlink 创建权限；真实 junction/hardlink 测试 PASS。未重跑 JVM/native 压测、Gradle、Lint或 APK 构建。真实配置旧缺陷、未执行 test 入口及 instrumentation 协议边界 RED 保留；未实现适配器的初始 scaffold ERROR 不冒称生产缺陷。

## 实际运行与停止

| 项目 | 实际结果 |
|---|---|
| SDK / APK | API26 r16 的 11 原件与历史 size/SHA 匹配；API36 r7 当前文件新 SHA 固定，**不是官方归档校验**。两 APK 证书、Manifest、Worker 非导出属性与实际 instrumentation 核对 PASS；没有下载、升级或重建。 |
| API26 | **新增创建 0，启动 1，ADB 0，App 安装 0，测试包安装 0，instrumentation 0**。静态 adoption PASS，真实 boot/API/ABI/AVD/serial 与运行路径门禁 NOT_RUN。 |
| API36 | 创建、启动、安装、测试均 0 / NOT_RUN。API26 条件未满足，不新增环境。 |
| 原三 JPEG / 其他 Android | 全部 NOT_RUN；不将宿主 PASS 当作 JPEG 修复或 Worker 生命周期通过。 |

首次静态 adoption 被一个新增空 `runtime-temp` 拦截，原始 exit1 保留。该目录出现时间与本轮 Windows CIM 子进程启动一致，其 TEMP/TMP 正被监督器设置到这里；精确增加“配置专属、无链接、完全空”核验并记录独立元数据，任何未知子项仍拒绝，不删除目录。CIM 改用已存在的宿主 TEMP/TMP，避免查询副作用影响未来 API36 独占创建；模拟器仍使用隔离 TEMP/TMP。静态 adoption 共调用 2 次，第二次 PASS；这不是第二次 AVD 创建或启动，也没有改写第一次失败。

唯一模拟器启动在 `2026-10-18T13:16:37.561349Z` 发出，launcher PID **32880**。首个 CIM 快照中其实际路径正确，子 QEMU PID **32032**、父 PID、完整启动参数均可见，但 `ExecutablePath=null`；监督器因此在任何 ADB 命令前 fail-closed。**没有等待完整 300 秒、没有启动重试，也没有回退到命令行字符串推定可执行文件身份。**

必要的 Windows 只读结束快照（`13:17:07.5816356Z`）显示同一 PID/父子关系的实际路径已可读，5624/5625 由该 QEMU 监听。支持“冷启动早期属性采样缺失”的假设；首次为空的具体原因仍 UNKNOWN，后次路径存在不能回改首个失败、证明 guest boot 或开放测试。没有再次调用完整预检、ADB 或正常退出。**新 API26 保持运行与数据现场；不得擅自再启动、停止或清理。**

## 产物、证据与剩余边界

- App：13,931,874 B，SHA `0ada263192dbaa1256bff6c613e6d57a69e1160b17d4844d1e1cfcc9cf847dbf`。
- 测试 APK：2,234,890 B，SHA `3b0005a31afa753e9a9709323572250c0c5c63153c68ca1f882755c98724282a`。
- 仅复用本地原件，不复制、提交或公开 APK。App 子树仍为 `89679a188c2b6e6793962041383a9f92337e126f`；Room、备份、冻结算法、UI和原 JPEG 断言均未改。
- [结果矩阵](m10-jpeg-android-isolated-resume/evidence/RESULTS.json)、[小型 SHA 索引](m10-jpeg-android-isolated-resume/evidence/INDEX.json)、[完整最终宿主结果](m10-jpeg-android-isolated-resume/evidence/all-host-final.log)、[原始命令](m10-jpeg-android-isolated-resume/evidence/commands.jsonl)、[启动阻塞](m10-jpeg-android-isolated-resume/evidence/start-blocked.log)、[结束观察](m10-jpeg-android-isolated-resume/evidence/ending-windows.json)。模拟器日志仅为明确标注的不可变前缀快照，原日志仍在本地追加；不是封闭的完整系统日志。未提交 SDK 许可正文、镜像、userdata 或私人数据。

**下一步唯一设备准入阻塞**是冷启动身份属性暂缺时如何保持安全等待/重新观察的门禁合同，以及是否授权继续使用这台已经运行的新设备；须 ChatGPT 独立审查，当前不实现或执行。Worker/Binder/FD/退休、原三断言、PNG/v1/业务保护、取消/超时/故障恢复、未运行 ABI 与资源最坏边界仍 NOT_RUN/UNKNOWN。M10 **PENDING / 三项历史 JPEG FAIL**、UI-C1 **FROZEN**、M11 **NOT_AUTHORIZED**，D2A-N01～N05、A1-N01～N04及全部历史 NOTE/FAIL/UNKNOWN 保留。原 H2 `DELIVERY_UNKNOWN/pin=true`、C2及其他旧 AVD 未访问或操作。交付复核：135 个本地文档链接、证据 SHA 和敏感模式检查通过；App diff 为空，受保护远端分支不变。原始日志中的尾空格/空行保留，不改写取证字节；新证据目录按原字节入 Git。只推送本续接分支后停止。
