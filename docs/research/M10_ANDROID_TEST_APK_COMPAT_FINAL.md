# M10 — API26 测试 APK 兼容整改与集中验收

日期：2026-10-19。**BLOCKED — M10-ANDROID-TEST-APK-COMPAT-FINAL**，等待 ChatGPT 独立审查；不是 M10 正式验收。

来源 `b2862062391a0c3008fe792db583b4abc5e146f9`；分支 `codex/m10-android-test-apk-compat-final`；独立 worktree `E:/CODEX/.codex/worktrees/lithic-test-compat/砺石新APP`。新批次 `m10-final-20261019-022830-2a602443`，以下结果均绑定本批新测试 APK，不借用旧 APK 的 PASS。

## 修改与兼容证明

- 仅将 `M10JpegIntegrityTest` 中 ImageDecoder 诊断移至 `M10Api28ImageDecoderDiagnostics`；helper 公共边界为 ByteArray、Bitmap、Function1、JSONObject，原 SDK28 判断、partialErrors/error/message、软件解码与异常记录保留。原测试类及 synthetic 成员不再暴露 ImageDecoder 类型。
- 新增 `M10Api26TestClassLoadingPreflight`，只对九项涉及的四个测试类及自身执行 `Class.forName/getDeclaredMethods`，不构造业务对象、Stage 或解码。
- 原三项 JPEG 断言、两份业务保护类及其他自然测试断言不变。`app/src/main`、schema、Gradle、依赖、Worker、Stage V2、JNI、UI 与原规划无差异。研究监督器仅固定新 SHA、增加 loader 门禁，并核验白名单中的历史诊断 nonce；历史 JUnit 失败从未被借作 PASS。
- 真实旧 DEX RED：4 个 declared methods 含 8 处 API28 参数引用；新 DEX GREEN：5 类、76 个 declared methods（含 28 个 synthetic）、21 个字段及父类/接口，API26 元数据违规 0。API28 helper 的 3 个定义仍保留隔离的 API28 引用。静态检查不覆盖所有方法体/异常表/传递依赖；API26 实际类扫描另行 PASS。[DEX 原始结果](m10-android-test-compat/evidence/m10-final-20261019-022830-2a602443/static/test-compat-dex/new-apk-green.json)
- 只执行 `:app:assembleDebugAndroidTest --offline --no-daemon --max-workers=1`，273.076 秒 exit0，58 份测试输入前后稳定，未生成 App APK。沿用 Gradle 8.14.3 / AGP 8.13.2 / Kotlin 2.3.10 / JDK 17.0.20.1+1。针对性及接缝宿主测试 **85 个唯一方法 PASS**；保留实际 RED/GREEN，不重复累计复跑。完整 JVM/native 压测、Lint、本机全量回归本轮 NOT_RUN。[构建与输入](m10-android-test-compat/evidence/m10-final-20261019-022830-2a602443/static/test-compat-build/INPUT_VERSIONS.json)
- 已核对8份实际输入与Git暂存内容：Integrity测试文件仅CRLF/LF差异，其他7份原字节一致；[双哈希登记](m10-android-test-compat/evidence/m10-final-20261019-022830-2a602443/static/test-compat-build/source-line-ending-map.json)保留构建原始SHA，不用Git换行转换后的SHA改写旧校验。原始设备证据全部按原字节提交。

## APK 身份

| 产物 | 字节数 | SHA-256 |
|---|---:|---|
| 原 App（未重建/替换） | 13,931,874 | `0ada263192dbaa1256bff6c613e6d57a69e1160b17d4844d1e1cfcc9cf847dbf` |
| 新 Test | 2,235,926 | `73d96ec9aceb23919183074a0dd101b1b7460187e4cb1ff5d3ed8ab9f9b3d98f` |
| 历史 Test（原件保留） | 2,234,890 | `3b0005a31afa753e9a9709323572250c0c5c63153c68ca1f882755c98724282a` |

新 Test 本地唯一产物：`E:/CODEX/.codex/worktrees/lithic-test-compat/砺石新APP/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`。不提交 APK。

新旧签名证书均为 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。Test 包 `com.cooper.lithic.debug.test`，组件 `com.cooper.lithic.debug.test/com.cooper.lithic.data.media.M10ProcessBoundaryRunner`。min26/target36、Manifest、权限、依赖及全部 52 个非 DEX/签名条目原字节一致。锁定健康/缺损样本 SHA 分别为 `2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907` / `f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a`。[审计原始输出](m10-android-test-compat/evidence/m10-final-20261019-022830-2a602443/static/test-compat-apk-audit/RESULTS.json)

## Android 实际结果

API26 复用原专用 `Lithic_M10V_API26_20261018/emulator-5624`：launcher16720/QEMU26072 的创建时间、实际路径、端口、磁盘、boot1/API26/x86_64 重新核验。App UID10077、Test UID10078 在唯一一次 Test `install -r` 前后相同；App SHA、私有根归属及已有合成 Stage/观察保持。类加载预检 **1 PASS**，未新增 Stage/Worker。

| 新批次自然测试（每项仅一次） | API26 | API36 |
|---|---|---|
| 健康 JPEG Worker 退休 | PASS | NOT_RUN |
| 缺损 JPEG 严格拒绝与退休 | PASS | NOT_RUN |
| 健康 JPEG 正式导入 | PASS | NOT_RUN |
| 原缺损完整性断言 | PASS | NOT_RUN |
| 项目旧封面及金额保护 | PASS | NOT_RUN |
| 订阅旧封面、付款与状态保护 | PASS | NOT_RUN |
| PNG 原功能 | PASS | NOT_RUN |
| v1 未提交 JPEG 兼容 | PASS | NOT_RUN |
| v1 已提交封面读取与保护 | PASS | NOT_RUN |

API26：9/9 真实 JUnit 与逐项资源门禁 PASS；8 个实际 Worker、48 条退休阶段事件身份与允许顺序核验，Stage/receipt/清理与旧事实核对。PNG/v1 没有新增 Worker/Stage，不伪造退休证据。随后一次授权正常退出 PASS，目标进程消失、5624/5625 端口关闭。[九项索引及操作数](m10-android-test-compat/evidence/m10-final-20261019-022830-2a602443/RESULTS.json)、[正常退出证明](m10-android-test-compat/evidence/m10-final-20261019-022830-2a602443/api26/control/normal-exit-proof.json)

API36：前置 API26 退出门禁满足后，新专用 `Lithic_M10V_API36_20261018/emulator-5626` 创建一次、启动一次，约118.156秒取得 boot1/API36/x86_64 与独立运行路径/进程证明。App 安装命令 exit0/Success、已安装字节 SHA 匹配；随后 **UID 准入 UNKNOWN**，立即停止。真实完整目标 `dumpsys package` 5835B、SHA `f6dc967d8c09a4655d4a84a7dbbc7809c91ab3577f09219456cf561f1b9b5f80` 使用 `appId=10216`，原解析器只接受 `userId=`，确定为字段格式不兼容，不能改判安装整体 PASS 或产品失败。Test 安装/类加载/九项全 NOT_RUN；没有修改解析规则、再次安装或重试。结束只读观察 App/Worker PID 均缺失、目标服务为空、no_backup ABSENT；UID 未补授准入，新 API36 保持运行（launcher33512/QEMU32252），不清理/终止。[停点](m10-android-test-compat/evidence/m10-final-20261019-022830-2a602443/api36/campaign-stop.json)、[结束观察](m10-android-test-compat/evidence/m10-final-20261019-022830-2a602443/api36/ending-protection.json)

| 实际操作 | API26 | API36 |
|---|---:|---:|
| 新建 / 启动 AVD | 0 / 0 | 1 / 1 |
| App 安装 / Test 安装或更新 | 0 / 1 | 1 / 0 |
| Instrumentation（类加载＋自然） | 1＋9 | 0 |
| 诊断 nonce / 连续 reader | 10 / 10 | 0 / 0 |
| 安全正常退出 | 1 | 0 |
| journal 定向 ADB 命令（非测试数；另计连续 reader） | 6372 | 84 |

API36 的19个非零命令退出保留：17次启动窗口 get-state 尚未在线、2次结束 pidof 未找到目标。它们没有被删除或写成 exit0；最终停止原因是成功 dumpsys 输出的 UID 字段不兼容。

## 证据边界、遗留与停止

- PASS 按各结果 `evidence_levels` 限定：退休 ACK/六阶段/OS 缺失/持久终结为独立观察；部分 FD/guard 释放为进程内结构化观察，Host FD/同步 hello/加载路径部分仅执行代码不变量。原始 Binder hello 字段、向只读 FD 实际写入仍 NOT_RUN，不能扩大为所有生命周期/故障恢复通过。
- API36 九项、API28 ImageDecoder helper 实际诊断、取消/超时/崩溃/恢复、永久阻塞读取、进程内存硬上限、其他 ABI/真机/OEM/Release/完整 TalkBack/真实 LMK/完整备份恢复均未由本轮证明。H2/C2/其他历史设备不访问，私人账本及图片字节不读，私人字节完整性 NOT_RUN。
- 原旧 Test 初始化 FAIL、旧缺损窗口 UNKNOWN、原三个 JPEG 历史 FAIL及全部 NOTE 保留；本批三个对应断言在 API26 新证据 PASS，**正式 M10 仍 PENDING**，UI-C1 FROZEN、M11 NOT_AUTHORIZED，不改正式验收登记。
- 当前唯一执行阻塞：API36 安装身份解析不支持实测 appId 字段。下一步先交 ChatGPT 独立审查真实输入与最小解析方案；不自行继续设备操作。
- 仅归档约1.49MB关键证据，无图片/APK/SDK/许可正文。完整重复命令 journal 本地保留，以 SHA/字节数和原行号登记；`commands-key-subset.jsonl` 明确是原行子集，不冒充完整 journal；活跃 emulator.log 仅登记观测前缀元数据。Codex 离线复核不是 ChatGPT 独立验收。

提交并仅推送本分支后停止；精确最终提交 SHA 由交付回复和 Git 远端共同确认。
