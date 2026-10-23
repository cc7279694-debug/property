# M22 Test 单次追加安装

2026-10-23，来源`0c45a43ef1d8e6e0c078bc821fafb52cecb8c4da`，分支`codex/m22-experience-accessibility`。用户明确授权“Test安装再来一次”；仅增加锁定Test安装尝试1次，不授权测试、App更新、清理或设置更改。前轮[安装失败及未知现场](M22-Physical-Isolated-Execution.md)保持原样。

- **准入PASS**：明确指定此前同一USB目标，Xiaomi `23127PN0CC` / API36；新App仍UID10420、旧M10仍UID10376，两者版本与安装身份一致。Test包括保留包记录均不存在。本地锁定SHA、签名、Manifest及Runner/targetPackage核对通过；无重建。[准入及输入身份](m22-test-install-retry-20261023-115534/admission.json)。手机硬件serial仅留本地忽略记录，公开命令显式使用脱敏占位符。
- **追加安装FAIL**：北京时间2026-10-23 19:56:05.618～19:56:18.108，实际exit1，`INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`。人工确认、安装弹窗或系统策略的具体触发原因**UNKNOWN**，不只据错误文字归责用户。无覆盖、降级或卸载参数；此次授权1次，连同旧授权累计2次Test尝试；失败后未再安装。[独立不可覆盖claim](m22-test-install-retry-20261023-115534/additional-install-claim.json)、[命令/时间/exit/字节SHA](m22-test-install-retry-20261023-115534/commands/install-test-additional-once/command.json)、[原始stderr](m22-test-install-retry-20261023-115534/commands/install-test-additional-once/stderr)。
- **结束核对PASS（仅安装身份）**：Test含保留记录仍不存在；新App安装SHA仍`b749b771e4f6f9cebcb7355c95834b7ae2eb4ba4f7528536a92bf9e09eefb2d3`、UID10420；旧M10版本/UID/installed元数据未变，未读其APK或私有数据，不声称私人字节一致性已验证。[结束包记录](m22-test-install-retry-20261023-115534/commands/end-package-records/stdout)、[新App SHA](m22-test-install-retry-20261023-115534/commands/end-app-sha/stdout)、[旧M10身份](m22-test-install-retry-20261023-115534/commands/end-old-m10-identity/stdout)。
- **三方法NOT_RUN**：T06继续按用户要求暂停；T03前置未满足；字体/IME未运行。App启动、Instrumentation、App安装/更新、设置写入、数据库读取、任务取消、清理及其他设备操作均0。前轮默认generation/WorkManager现场及Owner/任务UNKNOWN保持，本次没有重新证明关闭。

固定Test SHA为`c3791305aed28899f8df98dc7c89f27c986c022b84a0e528c68c59ebf9f2037f`；证书SHA为`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；Test实际未安装，安装UID及设备APK SHA为N/A。[Test验签](m22-test-install-retry-20261023-115534/commands/local-test-signature/stdout)、[Manifest](m22-test-install-retry-20261023-115534/commands/local-test-manifest/stdout)、[本轮结果](m22-test-install-retry-20261023-115534/install-result.json)。

本轮共12次指定目标ADB命令（11只读＋1安装）、2次本地APK检查。没有修改产品/测试源码，未运行构建/JVM/Android测试；原137 JVM/36 Android PASS及全部历史失败保持。M22继续BLOCKED / CHANGES REQUIRED，尚需解决Test安装拒绝及T06失败现场保留契约，旧Owner/任务未知状态不改判。仅提交本轮文档与小型原始证据；不继续第三次安装，不冻结、不进入M23，等待独立审查与新的明确授权。
