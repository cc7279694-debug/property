# H2-D2B — 执行与交付

2026-10-17。**BLOCKED — M10-R3-F1B-H2-D2B**。离线研究候选、测试及APK已形成；存在API26/27启动兼容缺口，不具备设备验证或安装资格，等待独立设计/代码审查。

**后续来源补录（2026-10-17）**：用户已补齐D2A-N01～N05原文，见[独立备注及逐条处理](M10_JPEG_H2_D2B_REVIEW_NOTES_SUPPLEMENT.md)。下述实施、测试与来源缺口段落保留`1721f81`交付时点事实；当前来源状态以补录为准，技术BLOCKED及原结果不变。本次只补文档，没有再次测试、构建或设备操作；原交付清单保持原字节，补录文件由独立文档核查清单登记。

## 来源、范围与边界

仓库`cc7279694-debug/property`；用户转交D2A独立PASS WITH NOTES，范围仅研究Java、145 JVM、22模型及研究构建。锁定来源`59e61cd856b4039c693eb0f280eaf1f2ac838b6b`，本地/远端一致，原工作区干净。独立worktree：`E:/CODEX/.codex/worktrees/m10-jpeg-h2-d2b-host-scheduling/砺石新APP`；分支`research/m10-jpeg-h2-d2b-host-scheduling`。

先提交[范围](M10_JPEG_H2_D2B_SCOPE.md)，commit `23b7ad0`，再实现。已读取实际AGENTS/PROJECT/当前记忆、D1三设计/22模型、D2A三报告与全部相关源码/测试、H2原始bind timeout/ANR/中止栈采集证据；未用源码推测补造ANR根因。独立编号D2A-N01～N05原文未找到，已请求补充，不能登记完整逐条验收。

## 实际修改文件与行为

修改四份既有研究Java：

- `ProbeActivity.java`：主线程只复制有界命令、入队；不构造Runner/持久claim。
- `ProbeApplication.java`：角色/owner/Runner初始化与claim使用唯一有界Host后台队列；未确认claim不dispatch。API28+先读framework进程名提示，API26后台读取身份，但与旧同步Worker绑定门槛存在待审冲突。
- `ProbeSupport.java`：Host有界异步诊断、丢失/异常/UNKNOWN及有界后台drain；明确Trace与command observer两个口径。WorkerService源码保持，但共享启动语义不能称完全不变。
- `ProbeRunner.java`：移除第二executor；单次Invocation阶段与绝对总期限、独立watchdog、实际时长、回调与失败再检查、保留原持久围栏/FD责任及成功乱序合同。

新增四个实际被使用的纯Javahelper：`HostCommandQueue`、`BoundedTraceWriter`、`BindingTimeline`、`DeadlineWatchdog`，各有新测试文件。原D2A145方法及断言字节保持。新增本轮脚本、原始证据、四份交付报告、CURRENT_STATE和checkpoint；历史原件不改，未删除文件。最终完整文件/SHA以本轮`evidence/delivery-verification.json`清单为准。

数据库、Room/schema、Repository、正式图片存储/JPEG协议、StageStore磁盘格式/同步写入、WorkerService、AIDL、Manifest、权限、Gradle、依赖与签名均未修改。没有新增服务器/JNI/native；正式产品代码无变更。

## 实际验证

229唯一JUnit PASS＝原145＋新84，16份XML与源码身份对应；原D1模型另22 PASS；源码静态检查另8 PASS。所有口径分列，RED/GREEN原始失败及一次编译错误日志保留。最终研究构建33秒成功，Lint0错误/8警告（新增fixed-rate timer警告未压制）。46份输入哈希与最终APK实物匹配。

APK绝对路径、字节、完整SHA、原D2A/F1-A产物保护、Manifest和证书实物检查见[测试/构建报告](M10_JPEG_H2_D2B_TEST_RESULTS.md)。两研究APK仅供离线审查，从未安装，旧产物未覆盖。没有以APK构建成功冒充生命周期或真实ANR通过。

## 阻塞及最小后续设计

API26/27：后台身份还未就绪时，原WorkerService会在main.onBind检查role(UNKNOWN)并拒绝；main不能等待后台。当前候选安全拒绝但可能破坏原合法启动。若坚持完全异步身份且保持现同步门槛，就需Worker启动/绑定协作改造，超出本轮，因此停止扩大实现。

优先提交独立审查的最小选项是API26/27保留原有必要的256字节身份探测同步例外，重I/O仍后台；它不需修改Worker但须明确接受剩余main探测及时间未知。替代选项是单独审查异步Worker就绪协议。本轮均未实施，不用默认Worker、main await、非SDK反射、提高minSdk或修改Manifest绕过。详见[线程/时序及兼容设计](M10_JPEG_H2_D2B_THREAD_TIMING_DESIGN.md)。

N05未改变D1：取消不等于已清理；失败资源终结须独立恢复授权及完整证明才可能终结。仅形成[三分类与最小方案](M10_JPEG_H2_D2B_N05_AND_RISKS.md)，不自动markTerminal，不执行恢复。其他风险包括pre-POSTED磁盘阻塞、Android调度未知、同步bind仍可能阻塞main、永久Binder调用及fixed-rate追赶；没有宣称全部ANR已修复。

## 保护与停止

ADB/模拟器/App操作、安装/启动/停止、normal/Outsider/cancel/kill/29场景、恢复/删除H2、私人账本/图片内容读取均0。只继承H2末次current `586d0904-eaac-403a-895a-48c34cd98810`、DELIVERY_UNKNOWN/pin=true/source8192，没有重新核对或修改现场。

H2原FAIL/ANR/UNKNOWN、R3完整生命周期BLOCKED及原0 PASS/1 UNKNOWN/28 NOT_RUN、M10 PENDING/三JPEG FAIL、UI-C1 ACCEPTED/FROZEN — PASS WITH NOTES、M11 NOT_AUTHORIZED全部保留。D2A通过仅用户转交的离线范围，不把其NOTE自动CLOSED。

只提交推送本D2B分支；最终SHA与远端一致性取交付回执，不在文档自身循环填SHA。未合并main、未动M10/UI-C1或其他研究分支。提交后立即停止，等待ChatGPT独立审查，不自动安装、恢复或重跑normal。
