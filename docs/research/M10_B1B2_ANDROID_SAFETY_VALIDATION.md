# M10 B1/B2 独立 Android 安全集中验收

**BLOCKED / WAITING_CHATGPT_REVIEW — 2026-10-19。**来源 `08ce4b663b52127f400fa18bd13eb12fe04a8797`，分支 `codex/m10-b1b2-android-safety-validation`。本批只运行一个 API26 类加载预检；JUnit 1 PASS，独立读证据门禁 UNKNOWN，按停止条件未继续 JPEG、40MP 或故障实验。M10 仍 PENDING，不能提交正式冻结结论。

## 1. 实际 Android 结果与现场

| 门禁 / 场景 | API26 实际结果 | API36 |
|---|---|---|
| 全新隔离 AVD、启动实例、boot/API/ABI/serial/路径/端口 | PASS；`Lithic_M10B1B2_API26_20261019 / emulator-5664 / x86_64` | NOT_RUN |
| App/Test 实物、证书、安装字节与 User 0 UID | PASS；10077 / 10078 | NOT_RUN |
| 新类反射/JUnit、nonce 连续采集 | 各 PASS；唯一方法运行一次，JUnit `OK (1 test)` | NOT_RUN |
| 预检独立记录读取、完整入场资格 | **UNKNOWN**；原 result 不改判 | NOT_RUN |
| BOOT_COUNT | 结束时实际可读 `1`；完整稳定性/Worker 对照凭据未完成 | NOT_RUN |
| 健康 JPEG 最小冒烟 | NOT_RUN | NOT_RUN |
| B2 Baseline420 / Progressive420 / 健康 Progressive444 | **三项均 NOT_RUN**，源文件尚未传入设备 | NOT_RUN |
| B1 取消、迟到结果、RPC 延迟、错 session/重复回执、active lease/GC | 全部 NOT_RUN | NOT_RUN |
| B1 Worker 异常死亡、两种 Host 中断、安全恢复 | 全部 NOT_RUN；实际故障终止 0 | NOT_RUN |

[唯一 Android 原始证据入口](m10-b1b2-android-safety/evidence/api26-run/INDEX.json)、[原方法结果](m10-b1b2-android-safety/evidence/api26-run/method-00/result.json)、[JUnit 原文](m10-b1b2-android-safety/evidence/api26-run/method-00/instrumentation.txt)、[连续采集原文](m10-b1b2-android-safety/evidence/api26-run/method-00/continuous-log/stdout.raw)、[结束核查](m10-b1b2-android-safety/evidence/api26-run/ending-readonly.json)。完整命令原件保留本地；交付索引保留每条命令、退出码、长度、原 SHA，关键命令保留原 Base64 字节，未把裁剪索引冒充完整原始输出。

**操作次数：**API26 创建 1、实际模拟器 spawn 1、App 安装 1、Test 安装 1、Instrumentation 1、图片传输 0、故障 kill 0；API36 全部 0。早先空 Java perf 目录及旧 Test 字节数常量阻断均发生在对应 start/install marker 和实际命令之前，记录保留，不计作第二次实际启动/安装；未重置任何 marker。

**结束现场：**launcher `40472`、QEMU `18424` 的创建时间/路径/父子/端口仍匹配；新 API26 保持运行，未退出、重启或清理。目标 App UID 的结束进程列表为空，Worker absent；已知 Stage V2、消费者观察、B2 诊断均 0；仅留下本次独占预检目录中的 `attempt.json` 390B 与 `result.json` 2439B，元数据归属/单链接稳定。没有 FD 交付/消费会话，不能据此声称异常生命周期已通过。H2、C2、保留 API36 和其他历史 AVD 均未定向访问/操作，真实账本及图片未读取/导入。

## 2. 准确阻塞与最小整改

**CONFIRMED：**本次真实 bundle 命令退出 0，但 stderr 为两条 `head: not integer: c`，共 44B；本次 API26 的 `head -c 16385` 调用失败，具体 head 二进制实现未另外查询。管道末端 base64 的成功掩盖了上游 head 失败，stdout 有完整元数据和 END 标记，却没有两文件的 Base64 字节。前后 stat/inode/size 相同，解析器因实际解码长度 0 拒绝。这不能证明文件写竞态或实际记录损坏；原 UNKNOWN 保留。

离线补充只修研究读取器：显式 pipeline 失败传播、有界读取及原有前后 inode/长度/哈希/目录/END 校验。真实旧输入、RED/GREEN与新方案边界见[读取器回归证据](m10-b1b2-android-safety/host/bounded-read-results.json)。修正后 Android 命令兼容性和完整入场证明仍 **NOT_RUN**，未重跑方法或重读设备记录来改判本批。恢复执行首先需要该读取链的独立只读核验；不需要重建 App、重复安装、重新创建 AVD 或重新研究 native。

**BLOCKING：**完整预检证据未取得；B1 实际异常收口及 B2 Android 40MP 全链仍未证明。当前没有新的 JPEG 解码/Worker 产品 RED，可提交审查的是监督器错误原件、候选适配和精确停止现场。M10 尚不具备正式冻结证据。

## 3. 交付代码、产物与验证边界

新增隔离测试：预检；HELLO＋持久 pin 后、数据 FD 交付前取消；真实 DELIVER 已返回而测试 Java 返回延迟；认证 Worker 故障；两种 Host 自终止；仅针对准确已结束 Host 的无结果恢复。失败清理继续要求真实同会话/独立死亡/guard/撤销/ABANDONED 证据，未知不假造 terminal。延迟接缝不模拟永久 kernel Binder 阻塞；错 session/重复回执断言位于真实 Stage API 层，不冒充错误 Binder RPC 已验证。故障入口需要精确许可、UID/BOOT/APK/唯一身份及外部证明，当前依赖门禁阻断，设备执行全部 NOT_RUN。

正式 App/main/debug/release、Manifest/Gradle/权限、Room/schema、业务算法、UI、旧测试/三项 JPEG 断言与来源零差异。仅新增 androidTest 和研究监督/证据文件；没有 App 重构或新 native/JNI 构建。

- **App 原件不变：**13,900,723B；SHA `b7c90277fa6c27e6ab314aa52c293de8e445ba6a3deba9e984ab96f4280e8a4c`。
- **本次唯一安装的最终 Test：**2,118,862B；SHA `37ad9ec91964b6a0b8a870ec35032164ebcdb6a1c41522e780cb7af343fcd338`；本地 `E:/CODEX/.codex/research-build/md-50784c33/app-debug-androidTest.apk`，未入 Git/发布。
- 原锁定 Test `bb00c0f6e34059170d20124eab739b43ccbc257ecc265def28906077fc0cb78c` 保留；按本轮 androidTest 故障适配授权重新编译 5 新源并只增加一个 DEX，原全部非签名 payload、九个旧 DEX、Manifest、4 个 assets 字节不变。旧离线 v2/ece 包未安装，不与本批混用。
- [最终 v3 审计](m10-b1b2-android-safety/test-artifact-audit-v3.json)：同证书 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，min26/target36，instrumentation `com.cooper.lithic.debug.test/com.cooper.lithic.data.media.M10ProcessBoundaryRunner`；26 class 输入、43 DEX 定义、7 新 JUnit `public ()V`、无重复类。缓存 JDK/Kotlin/AGP D8/签名工具，无下载/升级；[必要编译原件](m10-b1b2-android-safety/evidence/testpack-v3/INDEX.json)。原早期编译失败/non-void RED 与中间 v2 证据保留。未重跑完整 Gradle/JVM/Lint/native 压测，相关历史结论不当本批新 PASS。
- 必要宿主回归按各自索引登记：环境20、传输4、初始监督10、设备门禁最终22（原19＋读取器新增3）、故障10；另真实 Java observation helper18。读取器专项原 RED/GREEN 单列，不将历次重跑累计为新方法。所有宿主/静态结果均不替代 Android PASS。

三份固定 40MP 长度/SHA 只在宿主核对，444 精确为 `single444/valid_40mp_progressive_444.jpg`；[锁定输入](m10-b1b2-android-safety/artifact-inputs.json)。本批无 Worker/Host 图像 CPU、wall、RSS/PSS/HWM/native16/FD 的实测结果，均 NOT_RUN。512MiB 仍仅为分配请求预算；手机硬内存上限、采样峰值下界、PSS 节流和退休尾部空档不能被编译或宿主数据补证。

**正式状态保持：**M10 PENDING、原三项 JPEG 历史 FAIL、UI-C1 ACCEPTED/FROZEN、M11 NOT_AUTHORIZED；D2A-N01～N05、A1-N01～N04、原 R3 29 项及全部旧 NOTE/FAIL/UNKNOWN/NOT_RUN 不改。只提交推送本候选分支后停止，等待 ChatGPT 独立审查。
