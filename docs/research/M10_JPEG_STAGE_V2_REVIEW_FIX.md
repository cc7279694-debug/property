# M10 Stage V2 / Worker Android 准入整改

来源 `8ff9e5a6adefddab68eaf4bec5663b4d376ce2d7`；分支 `codex/m10-jpeg-worker-stage-v2-review-fix`。本轮是候选整改，尚非 M10 验收。离线整改、最终回归与产物核查已完成，等待 ChatGPT 独立审查；Android 准入仍需另行授权。

## 本轮契约与修改原因

目标：消除解绑后依靠裸 Binder 发退休请求的竞态，并避免外部源读取占用 Stage 命名空间锁。仅修改 JPEG 候选生命周期、Stage 并发接缝与相关测试；继承固定 3.2.0/native 预算、同代唯一 owner、原 v1/PNG 和业务 CAS。Room、备份格式、冻结算法、UI-C1、原三项 JPEG 断言均保持；不进行任何 ADB/AVD/安装/启动或 H2/C2 访问。证据标准是先真实 RED、后 GREEN，再在同一组最终输入下完整 JVM/Lint/Debug/Android 测试源码编译；真实 Android 证明另行授权。

## 关键差异

- **退休责任**：Client 在匹配 binding 仍有效时发送 RETIRE/REVOKE，并核对 ACK 的 phase、完整 request 及 Worker 身份；随后释放对应连接责任。正常退休须先认证 ACK、再 detach；未认证或超时 detach 走失败退出，不生成正常退休成功证明，Host 死亡另行处理。删除原 50/100ms 退休延时及提前释放 guard。ACK、解绑、Binder 死亡、匹配 OS 实例终结、独占 guard 与数据 FD 关闭仍是各自独立门槛。绑定关系的风险依据是 [Android 官方生命周期](https://developer.android.com/develop/background-work/services/bound-services)；[`onUnbind`](https://developer.android.com/reference/android/app/Service#onUnbind(android.content.Intent)) 回调本身并不是进程死亡证明。
- **失败发布**：短内存围栏仍先于磁盘/IPC；持久撤销单独执行，已知失败结果发布前须确认该 actor 完成，防止与调用方 Stage 收口竞争。最终发布再次检查固定总单调期限，不能依赖 watchdog 恰好按时被调度。仍绑定且无法 detach 时，不为强杀而触发自动重建，保留 UNKNOWN/pin，未证明 native 最坏终止时延。
- **排队与在途 IPC**：进一步复审发现 Future 超时后的排队 REVOKE 仍可能在解绑后执行，因此增加真正出队时取得、实际返回时结束的匹配 ticket。解绑先永久关闭 RPC 准入；未返回的在途事务不允许解绑，继续 UNKNOWN，不能用超时释放 ticket。仅在有限监督窗口内再次核对已匹配事务确实返回，既有清理才可释放连接责任；最后一次核对之后才返回时仍保留 binding/UNKNOWN/pin，没有自动解绑或收口 hook，不恢复会话资格。连接回调与 bind 返回共用同一个固定 10 秒期限，兼容回调早到，不在主线程等待后台。
- **源与 Stage 并发**：短 namespace 登记 → 持现有 guard 的 producer lease 在锁外读取 → 关闭源 FD → 锁内复核 owner/原 inode/guard/无消费者，确认 producer FD 关闭后发布元数据。没有新磁盘字段或 schema。未发布/未知 stage 阻止消费、恢复、GC 和叠加；只有本次尚无消费者的创建能写持久 NO_CONSUMER 证明后清理自己的文件。进程内 CAS 在任何 namespace guard FD 打开前拒绝竞争；非零 guard 在身份读取器打开前拒绝，避免关闭同 inode FD 影响 POSIX 锁。不同 namespace 的短暂竞争也保守拒绝，无无界路径锁表。
- **取消与测试现场**：JPEG 源读取前后及发布前检查调用方取消，保留原取消对象；无法中止永久阻塞的 provider read。登记/发布仍含有界私有文件 hash/fsync，不承诺硬实时。新增真实 Android 健康/缺损生命周期测试入口；三份既有 Android 测试仅收紧 teardown，UNKNOWN 不删 stage，原三条 JPEG 断言及这三份测试的全部旧 `@Test` 正文不变。Worker 旧 12 项退休门禁方法身份保留，其中 6 处解绑布尔输入随已授权的正确顺序调整；FD/死亡/lease 拒绝条件未放宽。

## 实际验证与产物

同一组 **199 项输入**前后逐字节及 LF 归一化摘要一致；最终单次完整执行 `BUILD SUCCESSFUL in 14m 24s`，退出 0：

| 检查 | 实际结果 |
|---|---|
| 完整 JVM | **308 唯一方法 PASS**，0 失败/错误/跳过；86 领域/展示＋79 Stage＋143 Worker。来源 236 身份全部保留，新增 72。 |
| Lint / Debug / Android 测试源码编译 | 0 错误、23 警告；其余均 PASS。没有 Android 测试运行。 |
| 独立宿主补充 | 9 Python 装配检查 PASS；真实 Java Worker 143 与 Stage 新增 12 定向 GREEN 不重复计入 308。 |
| RED 与中间结果 | 原退休门禁 2 FAIL、原 Stage 6 FAIL、guard 1 FAIL；新接缝 fail-closed stub/mutation 的 RED 原样标注保存。中间 288 PASS 被 RPC 修正后的最终回归取代，不拼接。 |
| 静态 APK | 四 ABI 的缓存/strip/ELF/API26 note/16KiB 对齐、3 许可资产、Manifest 与原签名核对 PASS；实际加载全部 NOT_RUN。 |

证据：[结果与逐文件 SHA 索引](m10-jpeg-stage-v2-review-fix/RESULTS.json)、[最终原始构建日志](m10-jpeg-stage-v2-review-fix/final-gradle.log)、[原始 JVM XML](m10-jpeg-stage-v2-review-fix/junit/)、[Lint XML](m10-jpeg-stage-v2-review-fix/lint.xml)、[产物核查](m10-jpeg-stage-v2-review-fix/artifact-audit.json)。归档仅必要原始结果，不交付大输入 manifest。APK 审计第一次因辅助脚本仅识别 hex false 而拒绝 aapt 的 literal false，已修正审计并对**同一 APK**复核；原异常日志保留，不当作产品失败或静默抹除。

唯一当前 APK：`E:/CODEX/.codex/worktrees/lithic-jpeg-review-fix/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`，**13,931,874 字节**；SHA-256 `0ada263192dbaa1256bff6c613e6d57a69e1160b17d4844d1e1cfcc9cf847dbf`。包名 `com.cooper.lithic.debug`，min26/target36，原证书 SHA `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。只在忽略的本地输出目录，未安装、发布或提交 APK。JDK17/Gradle8.14.3/AGP8.13.2/Kotlin2.3.10/NDK28.2.13676358/CMake3.22.1不升级；固定 libjpeg-turbo3.2.0及请求预算沿用，512MiB不是RSS或手机安全硬上限。

最终构建输入的 LF 摘要 `35a8fec07ae3680b5a2ef748b13771a51ff63b1077995e5cb7199c9c6b17000f`；对应 Git app 子树 `89679a188c2b6e6793962041383a9f92337e126f`，最终提交须保持该子树。三份旧 Android 测试正文按来源核对不变；Room/schema/备份格式/领域算法/UI/Manifest/Gradle/依赖范围检查无变化。静态源码和 NTFS 测试不冒充 Android POSIX、Binder 或真实 native 终止证明。

## 隔离 Android 验收方案（本轮未执行）

1. **准入与权限**：另行明确批准新建/使用全新 API26、API36 独立设备、目标 serial、APK 安装/启动及单独故障实验范围；缺少镜像/新许可/工具下载须另批。AVD 注册、数据根、端口、串号必须与原 H2/API36 和 C2 全部互斥；不得向旧设备发送命令。先锁定本报告最终 APK、测试输入与测试 APK 的 SHA/证书及真实 instrumentation component；不猜测试包名。只给新 serial 定向操作。
2. **首个生命周期门禁**：单独执行 `com.cooper.lithic.data.media.M10WorkerRetirementReviewTest#healthy_jpeg_retires_real_worker_before_stage_completion`。核对 Host/Worker PID 不同、UID 相同、startTicks 与同步 Binder 身份、attempt/session/nonce/generation；持久 pin 先于 FD 交付；源 inode/长度/RO FD/实际 SHA；双方 FD 关闭；一次结果 → 仍绑定期间认证退休 ACK → 匹配解绑 → Worker onUnbind/退出 → Binder 死亡＋OS 实例终结＋同 guard 独占 lease → terminal/prepared/安全 cleanup。关联有界 `LITHIC_JPEG` 日志与测试观察 JSON，不能只读 SUCCESS 行；同时排除自动重启/第二消费者。任一证据缺失或 UNKNOWN 即停止，保留现场。
3. **缺损与旧图保护**：第二个新增方法 `locked_damaged_jpeg_is_rejected_and_real_worker_safely_retires`；然后分别执行原 `M10JpegIntegrityTest#valid_control_jpeg_imports_successfully`、`#jpeg_entropy_truncated_with_eoi_is_rejected`，及 `M10JpegOldCoverProtectionTest#project_entropy_cut_jpeg_is_rejected_and_old_cover_and_money_are_preserved`、`#subscription_entropy_cut_jpeg_is_rejected_and_old_cover_payments_and_states_are_preserved`。每方法独立采集 XML/退出码/源哈希/旧封面与完整业务事实核对，不自动遍历到下一项。原测试的递归 teardown 不适用于 UNKNOWN 消费者故障场景，必须先确认安全收口；不能将测试失败后的删除当作恢复。
4. **兼容及并发**：运行现有 `M10StageV2CompatibilityTest` 四方法、PNG/共享引用/业务事实保护；源读取阻塞时核对其他 Stage 操作及时 fail-closed、主业务锁仍可用；未知 v1/V2 残留保留。现有部分 Repository 回归使用 PNG，只能证明其实际输入路径，不替代 V2 生命周期。
5. **故障门禁另批**：取消、超时、bind false/exception、迟到回调/死亡乱序、Host/Worker 死亡恢复和 GC，需要专用可观察测试入口及明确进程故障许可。每场景独立合成根；先持久撤销，验证 lease/FD/退休终结及重启准入，未知不得清理、重跑或叠加任务。当前自然成功/损坏入口不冒称覆盖这些系统场景；尚缺入口与实证时记 NOT_RUN。

单方法命令模板（**仅未来获批后执行**）：`adb -s <已批准的新serial> shell am instrument -w -r -e class '<实际FQCN>#<方法>' <从测试APK最终Manifest读取的component>`。先 API26 首门禁，后 API36 独立重复相同冻结输入；任一失败即停止该环境后续实验。x86_64 两版本通过也不等于 arm64-v8a / armeabi-v7a / x86 已真实加载。Worker 早期绑定、取消最坏耗时、真实 OOM 和目录 fsync 都须实际证据。

## 仍然保留的边界

M10 PENDING；原三项 JPEG FAIL 保留，本轮设备测试 NOT_RUN；UI-C1 ACCEPTED / FROZEN — PASS WITH NOTES；M11 NOT_AUTHORIZED。H2 DELIVERY_UNKNOWN/pin=true、C2 原现场及 D2A-N01～N05 / A1-N01～N04 / 全部历史 NOTE、FAIL、UNKNOWN、NOT_RUN 保留。编译成功不等于 Binder、FD、退休或 native Android 加载成功。
