# M10 API26 测试 APK 兼容整改 checkpoint

日期：2026-10-19。状态：**BLOCKED — M10-ANDROID-TEST-APK-COMPAT-FINAL**，等待 ChatGPT 独立验收。

- Goal：隔离 API28 测试诊断，完成同一新 Test APK 的 API26/API36 九项自然验证。
- 来源：`b2862062391a0c3008fe792db583b4abc5e146f9`；独立分支 `codex/m10-android-test-apk-compat-final`；新批次 `m10-final-20261019-022830-2a602443`。
- 已验证：只修改3份 androidTest 源文件及必要研究门禁；原测试断言、生产源码/schema/构建配置不变。旧 DEX 8处违规 RED，新 DEX 5类76方法（28 synthetic）/21字段无 API26 元数据违规，实际 API26 类加载 PASS。宿主85唯一 PASS、仅 Test APK 离线构建 PASS。
- APK：App `0ada263192dbaa1256bff6c613e6d57a69e1160b17d4844d1e1cfcc9cf847dbf` 保持；新 Test `73d96ec9aceb23919183074a0dd101b1b7460187e4cb1ff5d3ed8ab9f9b3d98f`，同签名、Manifest/资产/样本不变。旧 Test `3b0005…` 原件及历史报告不改。
- API26：原实例复用；新建0/启动0/App安装0/Test更新1，类加载1＋九项9均 PASS，10次真实nonce握手；随后授权正常退出1，目标进程和端口结束证明 PASS。设备已退出，不发送后续命令。
- API36：新建1/启动1，身份及 boot PASS；App安装1实际 Success/字节一致，UID准入 UNKNOWN。真实 dumpsys 使用 appId，原解析器仅匹配 userId；Test安装0/类加载0/九项全 NOT_RUN。没有放宽/重试/清场。
- 结束现场：新 API36 `Lithic_M10V_API36_20261018/emulator-5626` 保持运行（launcher33512/QEMU32252）；只读末次 App/Worker PID为空、目标服务空、no_backup ABSENT。未完成 UID 准入，不用这些观察替代安装/生命周期通过。
- 证据边界：部分 FD/guard 为结构化观察、部分同步身份/Host FD仅代码不变量；原始 Binder字段及写只读FD NOT_RUN。API28诊断helper实际运行、故障/取消/恢复/其他ABI/真机及完整备份恢复未验证。
- 继承：M10 PENDING，三个旧JPEG FAIL/旧 UNKNOWN/全部 NOTE 保留；UI-C1冻结、M11未授权；H2 DELIVERY_UNKNOWN/pin=true、C2及其他历史现场没有访问。
- 唯一下一步：ChatGPT 独立核对 API26 新九项及 API36 appId 原始输入，决定最小解析整改与后续设备授权。不得据本 checkpoint 自动续测。

[集中报告](../research/M10_ANDROID_TEST_APK_COMPAT_FINAL.md)；[实际结果与原始证据索引](../research/m10-android-test-compat/evidence/m10-final-20261019-022830-2a602443/RESULTS.json)。最终提交 SHA 在交付回复/远端登记，不将文档自身 SHA 伪造为预先已知。
