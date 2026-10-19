# M10 日志握手与自然测试停止点

- **Goal / Scope**：从 `ca79e1c8fe77de050145a463fbd35df95556d413` 修研究日志 READY 握手，在独立 API26/API36 完成最多九项自然测试；不改锁定 APK、App 或历史证据。
- **Changes**：reader→唯一诊断 nonce→实际回收认证→READY→Instrumentation；原始字节和独立认证 backlog 分离；重新证明当前 API26 实例；最终 observer stderr/EOF 检查离线补强。
- **Verified**：本批 `m10-final-20261019-014441-e6b2cb97` 三次实际握手 PASS；健康/缺损 Worker 两项完整现有分层门禁 PASS。最终43针对＋70选定回归＝113唯一宿主 PASS；没有重跑完整历史或 native/Gradle。
- **Blocking**：method-02 在 API26 JUnit 测试类初始化失败，锁定 Test APK 本类 synthetic 方法签名引用 `ImageDecoder$DecodeException`。正文未执行；后六项和 API36 NOT_RUN。此结果不证明生产健康导入失败。最终 stderr 竞态补强仅宿主验证，未再运行 Android。
- **Scene**：专用 API26 保留运行，launcher16720/QEMU26072、serial5624；创建/启动/安装/退出0，Instrumentation3，各一次。结束无活动 Worker或未知消费者；4个合成 Stage/8份观察保留。不得根据此 checkpoint 自动操作设备；下次先重新核验。
- **Inherited**：M10 PENDING/三JPEG历史FAIL；UI-C1冻结、M11未授权；H2 DELIVERY_UNKNOWN/pin=true、C2及历史 NOTE/FAIL/UNKNOWN/NOT_RUN不改。Room、备份、算法、UI和原三项断言不改，私人字节完整性 NOT_RUN。
- **Next**：仅等待 ChatGPT 独立审查锁定测试 APK 的 API26兼容阻塞及日志证据，另行明确授权才可更换测试 APK/继续设备验证。

完整[集中报告](../research/M10_ANDROID_CAPTURE_HANDSHAKE_VALIDATION.md)与[结果/原始证据](../research/m10-android-capture-handshake/evidence/m10-final-20261019-014441-e6b2cb97/RESULTS.json)。交付分支 `codex/m10-android-capture-handshake-validation`；本 checkpoint 不授予 ACCEPTED/FROZEN。
