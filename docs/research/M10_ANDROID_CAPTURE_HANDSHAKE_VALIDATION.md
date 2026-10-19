# M10 日志握手与 Android 集中验收

2026-10-19；**BLOCKED — API26 锁定测试 APK 的测试类初始化失败**。来源 `ca79e1c8fe77de050145a463fbd35df95556d413`，交付分支 `codex/m10-android-capture-handshake-validation`。新批次 `m10-final-20261019-014441-e6b2cb97`；[结果/证据入口](m10-android-capture-handshake/evidence/m10-final-20261019-014441-e6b2cb97/RESULTS.json)。本轮不授予 M10 验收通过。

## 修改与验证

- 研究采集器先建立固定 serial 的 reader，再发送一次 `LITHIC_M10_CAPTURE` / 随机 nonce，实际接收到精确原始行且 reader/错误流正常后才 READY；使用原 10 秒总就绪预算。独占 marker 防止重发；READY 后才 Instrumentation。
- 原始 stdout/stderr 全留存。解释器只排除当前认证诊断和同批次前项 PASS、原始 SHA 重验通过的精确旧诊断行；旧诊断仅可处于初始 backlog。错误 nonce、重复、未知事件、错设备、中断或不完整输出拒绝。诊断不计入六阶段退休，不清 logcat、不改缓冲区或 App 日志。
- `campaign.py` 新增当前运行实例的独立准入及握手接缝；保留 API26 包缺失即停止，以及 API36 必须先有本批 API26 全通过和正常退出证明的门禁。API36 profile 接缝仅静态/宿主验证，未操作设备。
- **113 个唯一宿主方法 PASS**：43 个针对性测试＋70 个选定采集回归；完整历史数百项、native 压测、Gradle/Lint/APK 构建均 NOT_RUN。新增握手/解释器 RED→GREEN 和最终输入 SHA、身份列表见 [Host 结果](m10-android-capture-handshake/evidence/m10-final-20261019-014441-e6b2cb97/host/FINAL_113.json)。
- Android 停止后，离线补强了 stderr 已观察但错误尚未发布的短竞态，另 4 方法 RED 4 FAIL→GREEN 4 PASS（包含于上述 43）。最终 observer 只作宿主验证，**没有再跑 Android**；三次实测 stderr 均为 0，原始捕获不改。[过渡 SHA/补丁](m10-android-capture-handshake/evidence/m10-final-20261019-014441-e6b2cb97/host/race/source-transition.json)区分原始 SHA 与逆补丁重建前态，不将重建字节冒充现场采样。

## 本批 Android 结果

| 固定自然测试 | API26 | API36 |
|---|---|---|
| 0 健康 JPEG Worker 退休 | **PASS** | NOT_RUN |
| 1 锁定缺损 JPEG 严格拒绝及退休 | **PASS** | NOT_RUN |
| 2 健康 JPEG 正式导入 | **FAIL：测试类初始化；正文 NOT_RUN** | NOT_RUN |
| 3 原 JPEG 完整性断言 | NOT_RUN | NOT_RUN |
| 4 项目旧封面及金额保护 | NOT_RUN | NOT_RUN |
| 5 订阅旧封面、付款及状态保护 | NOT_RUN | NOT_RUN |
| 6 PNG 原功能 | NOT_RUN | NOT_RUN |
| 7 v1 未提交 JPEG 兼容 | NOT_RUN | NOT_RUN |
| 8 v1 已提交封面保护 | NOT_RUN | NOT_RUN |

**三次真实握手均 PASS**；前两项匹配唯一 attempt/session/Worker，JUnit、六阶段退休偏序、持久 pin/终结/清理凭据及独立进程缺失观察通过，缺损项为严格拒绝。各证据级别原样保留：FD 关闭和 guard 释放来自结构化进程内观察；同步 hello、只读 FD/Host FD 关闭仍是执行代码不变量，原始 Binder hello 字段及写只读 FD 实验 NOT_RUN。不能将本批 PASS 扩大为所有 Binder/FD/取消/恢复场景已通过。见 [健康结果](m10-android-capture-handshake/evidence/m10-final-20261019-014441-e6b2cb97/api26/method-00/result.json)、[缺损结果](m10-android-capture-handshake/evidence/m10-final-20261019-014441-e6b2cb97/api26/method-01/result.json)。

**当前续接唯一阻塞**：第三项 runner 在 `Class.getDeclaredMethods()` 解析测试类时，找不到 `ImageDecoder$DecodeException`；指定方法正文没有运行。[完整原始 trace](m10-android-capture-handshake/evidence/m10-final-20261019-014441-e6b2cb97/api26/method-02/instrumentation.txt)与[锁定 APK DEX 静态分析](m10-android-capture-handshake/evidence/m10-final-20261019-014441-e6b2cb97/audit/api26-runner-static-note.json)一致：虽然诊断代码有 API≥28 分支，编译生成的本类方法签名仍引用 API28 类型。生产 `AndroidImageCodec` 使用 BitmapFactory，**不能由此判定生产健康导入失败**。不修改/重建锁定 APK；其后六项及 API36 立即停止，无重跑。

## 身份、操作次数与结束现场

复用 `Lithic_M10V_API26_20261018 / emulator-5624` 的实际同实例：launcher16720、QEMU26072，重新核验实际路径、创建时间、父子关系、隔离磁盘、端口及 boot1/API26/x86_64/AVD。新[当前实例证明](m10-android-capture-handshake/evidence/m10-final-20261019-014441-e6b2cb97/api26/control/boot-proof.json)不改旧首次启动时限 NOT_PROVEN。

| 锁定产物 | SHA-256 | UID |
|---|---|---|
| App（13,931,874 B） | `0ada263192dbaa1256bff6c613e6d57a69e1160b17d4844d1e1cfcc9cf847dbf` | 10077 |
| Test（2,234,890 B） | `3b0005a31afa753e9a9709323572250c0c5c63153c68ca1f882755c98724282a` | 10078 |

两者证书 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；非导出 `:jpeg_worker`、权限、四 ABI/锁定样本静态核验见 [APK 审计](m10-android-capture-handshake/evidence/m10-final-20261019-014441-e6b2cb97/audit/apk-audit.json)。只在 API26 x86_64 实际执行前两项；其他 ABI 不由静态存在推定加载通过。

API26 **创建0／启动0／安装0／Instrumentation3／诊断写入3／正常退出0**；API36 全0。日志命令共 1,056 次定向 ADB：1,047 只读＋3诊断＋3 Instrumentation＋3自有 Host reader；另14次只读 CIM。journal 命令 exit 均0，但第三项真实 JUnit FAIL，不能由 adb exit0冒称通过。reader 结束由 Windows 自有采集进程受控停止（exit1），不是 Android App/Worker 终止。

[结束只读保护](m10-android-capture-handshake/evidence/m10-final-20261019-014441-e6b2cb97/api26/ending-protection.json)：包 SHA/UID 相同、无活动 App/Worker；4个合成 Stage / 8份观察均可核验，第三项无新增或修改、无未知消费者。专用 API26 **保留运行**；未满足九项全通过，故不退出、不创建 API36。未访问 H2/C2/其他旧设备，不读取私人账本/封面字节，私人字节完整性 NOT_RUN。无 SDK/许可正文、APK、镜像、userdata 或私密数据提交。

正式状态保持 M10 PENDING、三项历史 JPEG FAIL、UI-C1 FROZEN、M11 NOT_AUTHORIZED；旧批次 `m10-final-20261019-005717-97808a30` method-00 BLOCKED、旧缺损 UNKNOWN 与全部历史 NOTE 原样保留。下一步仅交 ChatGPT 独立审查：若另行授权，最小测试兼容方案是把全部 API28 诊断类型隔离到独立 helper，保持原断言，再审核新的测试 APK；本轮未实施，不自动继续。
