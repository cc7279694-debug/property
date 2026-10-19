# M10 — API36 UID 整改与自然测试续验

2026-10-19。**READY_FOR_REVIEW — M10-API36-UID-RESUME**；仅本轮研究与自然测试交付，不授予 M10 冻结。

来源 `d225dfc2d9ba74227cebab68a98176110145e50a`；分支 `codex/m10-api36-uid-resume`；独立 worktree `E:/CODEX/.codex/worktrees/lithic-uid-resume/砺石新APP`。沿用 campaign `m10-final-20261019-022830-2a602443`，新续接目录 `m10-uid-20261019-040339-e0b26457`。旧 campaign 的历史来源 b286…不改写；新增证明记录本轮 d225…来源。

## 最小整改与离线验证

- `validate.py::installed()` 保留唯一 `pm path` 和设备 APK SHA 检查，将全局 userId 正则替换为研究模块 `installed_uid.py`：只认唯一完整目标 Packages 块、User 0 installed=true、普通应用 UID；userId/appId 各自不得重复，同时出现须相等。
- codePath 必须等于已核验 base.apk 父目录；明确归属目标 sourcePackage 的权限 UID 不得矛盾。再以真实 `pm list packages -U --user 0` 独立查询核对，拒绝其他包混入、跨用户、冲突、缺失与截断。签名由已验证本地 APK＋设备端完全相同 SHA 闭环核验，不以 appId 单独放行。
- 新 `resume.py` 仅开放本次 API36 同实例准入、App只读确认、Test单次安装、类加载、固定九方法及只读结束；禁止 App 安装、AVD创建/启动/退出、清场与重试入口。原 boot、安装 UNKNOWN、marker 和日志不改；新证明/操作 marker 全部独立。
- **101 个唯一宿主方法 PASS，0失败/错误/跳过**＝UID53＋原监督器22＋续接26。保留旧解析器、嵌套包头和相关身份矛盾的实际 RED，以及中间取证/测试变异异常；不重复累计历次 GREEN。[宿主结果](m10-api36-uid-resume/evidence/m10-uid-20261019-040339-e0b26457/host/uid36-host-final/RESULTS.json)
- `app/`（包括 androidTest）、Room、备份、算法、UI、Worker、Stage V2、JNI、Gradle及依赖无差异；本轮没有构建 APK、重跑 native/JVM 全量或 Lint，没有下载安装工具。API26仅用历史文本回归和已验收退出证明，**设备命令0**。

## 原件与新准入

| 固定产物 | SHA-256 |
|---|---|
| App，13,931,874字节 | `0ada263192dbaa1256bff6c613e6d57a69e1160b17d4844d1e1cfcc9cf847dbf` |
| Test，2,235,926字节 | `73d96ec9aceb23919183074a0dd101b1b7460187e4cb1ff5d3ed8ab9f9b3d98f` |

本轮重新核验本地签名/Manifest：同一证书 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，Worker非导出/`:jpeg_worker`，固定 runner `com.cooper.lithic.debug.test/com.cooper.lithic.data.media.M10ProcessBoundaryRunner`。App原件在 `E:/CODEX/.codex/worktrees/lithic-jpeg-review-fix/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`；Test原件在 `E:/CODEX/.codex/worktrees/lithic-test-compat/砺石新APP/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`。不复制或提交 APK。

实际复用 `Lithic_M10V_API36_20261018/emulator-5626`：launcher33512（03:13:03.5805700Z）、QEMU32252（03:13:03.7522540Z）精确创建时间、父子关系、实际可执行文件、参数、端口与隔离磁盘重新观察一致，API36/x86_64/boot1 PASS。[新实例观察证明](m10-api36-uid-resume/evidence/m10-uid-20261019-040339-e0b26457/control/resume-boot-proof.json)

App复用原安装，唯一目标包、路径、SHA、User0 UID、相关权限和私有根归属均核对为 **10216**；新写 `app-uid-admission.json`，不补写旧安装 PASS。测试包两份成功的 User0 清单均确认不存在（含未安装注册查询），随后只安装 Test 一次，实际 SHA/签名/组件和独立 UID **10217** PASS；App字节、UID、已有资源不变。类加载预检一次 PASS，未新增 Stage/消费者。

## API36 九项新结果

| 固定自然测试，每项一次 | 实际结果 |
|---|---|
| 健康 JPEG Worker 完整退休 | PASS |
| 锁定缺损 JPEG 严格拒绝及退休 | PASS |
| 健康 JPEG 正式导入 | PASS |
| 原缺损 JPEG 完整性断言 | PASS |
| 项目旧封面及金额保护 | PASS |
| 订阅旧封面、付款和状态保护 | PASS |
| PNG 正常导入 | PASS |
| v1 未提交 JPEG 兼容 | PASS |
| v1 已提交封面读取与保护 | PASS |

9条真实JUnit、同一Test SHA、逐方法nonce握手、连续原字节日志与资源门禁均PASS。观察到8个唯一Worker/attempt/session、48条退休事件，认证退休/解绑/匹配死亡/guard/终结按既定偏序核验。项目和订阅旧图SHA、金额事件、付款状态及媒体事实保持；失败准备未生成新正式引用。PNG/v1三项没有伪造新的Worker、Stage或严格凭据。[逐项原始结果入口](m10-api36-uid-resume/evidence/m10-uid-20261019-040339-e0b26457/RESULTS.json)

| 本轮实际操作 | 次数 |
|---|---:|
| AVD新建 / 启动 / App安装 | 0 / 0 / 0 |
| Test安装 | 1 |
| Instrumentation：类加载＋自然 | 1＋9 |
| 诊断nonce / 连续reader | 10 / 10 |
| 定向ADB：初始只读诊断＋续验监督器（另计reader） | 9＋2275 |
| 非零命令退出 / UNKNOWN命令 | 0 / 0 |
| API26及其他历史设备操作 / 重试 / 故障注入 | 0 / 0 / 0 |

## 结束状态、证据边界与停止

唯一只读结束核查：resourceStatus PASS，App/Test UID10216/10217，目标App/Worker进程均为空；**2个持久review Stage、10份观察**与最后方法末态一致，旧记录未变、无未知消费者。其他6个invocation的Stage来自锁定测试的自有临时根，不把8个Worker冒称8个独立持久Stage。设备保持运行，没有退出或清理。[结束观察](m10-api36-uid-resume/evidence/m10-uid-20261019-040339-e0b26457/ending-protection.json)

退休日志/OS缺失与持久review Stage为独立观察；部分FD/guard/业务事实来自进程内结构化观察，部分Host FD/同步hello/加载路径仅执行代码不变量。原始Binder hello字段、实际写只读FD、全部FD异常路径、取消/超时终止/故障恢复、native内存硬上限、其他ABI运行、真机/OEM/Release/完整备份恢复仍NOT_RUN/未证明；不能扩大为完整生命周期通过。API28诊断helper没有在九项中另行执行。

**M10继续PENDING**；三个JPEG历史FAIL、旧API36 UID UNKNOWN、H2 DELIVERY_UNKNOWN/pin=true、C2、全部历史NOTE/UNKNOWN/NOT_RUN保留；UI-C1 FROZEN、M11 NOT_AUTHORIZED。H2/C2及其他历史设备未访问；没有读取真实私人账本或图片。API26先前PASS WITH NOTES来源为用户转交的ChatGPT独立验收，本轮不重复执行。

约1.6MB必要新证据（其中收集器复制的原始副本872,911字节，另含明确标注的关键命令子集和结果记录），未生成ZIP、大型文件清单、重复图片或APK；完整命令journal本地保留并登记字节/SHA，归档的原行子集明确标注“不完整journal”。SDK环境旧pin文件仅原字节复用，历史freshUtc不冒充新观察；实际APK/工具/运行身份另行核验。Codex辅助复核不等于ChatGPT独立验收。

本轮没有剩余执行门禁阻塞。只提交推送本分支后停止，等待ChatGPT限定独立审查，不合并、不冻结M10、不进入M11；最终提交SHA与远端一致性在交付回复确认。
