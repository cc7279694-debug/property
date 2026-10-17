# H2-D2B-A2 — 隔离API26冷启动验证执行记录

2026-10-17。**BLOCKED — M10-H2-D2B-A2**。已检查现有SDK，未安装API26系统镜像，按用户明确条件停止环境推进。本轮没有创建/启动AVD、安装APK或启动Activity。

## 来源、范围与Git

仓库`cc7279694-debug/property`。用户转交ChatGPT对A1的**PASS WITH NOTES，仅限离线代码和研究APK**；评审/本轮来源为`971f742452422ce6b31462123c351d112eb48101`，开始时本地/远端一致、A1和主工作区干净，新A2分支不存在。未修改A1历史报告、测试或产物。

独立研究分支：`research/m10-jpeg-h2-d2b-a2-api26-smoke`。

实际独立worktree：`E:/CODEX/.codex/worktrees/lithic-a2-api26/砺石新APP`。原生worktree工具首次使用长目录名时因历史XML文件路径超过Windows限制失败；检查确认失败目标目录不存在、未登记worktree，随后仅用较短的新目录名成功创建。没有reset/覆盖旧工作区或修改系统长路径设置。该Git目录问题与AVD完全不同，不是冷启动尝试，详情见[宿主执行附注](m10-jpeg-h2-d2b-a2-api26-smoke/evidence/host-execution-notes.json)。

已读取AGENTS/PROJECT/唯一CURRENT_STATE、必要决策/架构/路线、A1执行/兼容报告及实际ProbeActivity；无command时不dispatch属于源码事实，**本轮没有Android运行来证实**。

## 已执行的环境检查

以A1真实构建记录的SDK为主：`C:/Users/CDD/AppData/Local/MirraAndroid/sdk`；另检查默认`C:/Users/CDD/AppData/Local/Android/Sdk`以及进程/User/Machine级`ANDROID_HOME`、`ANDROID_SDK_ROOT`。六个变量均未配置。Windows应用环境解析后的实际路径及逻辑路径分别记录，未把路径别名当作两个独立SDK。

最终[SDK清单与元数据哈希](m10-jpeg-h2-d2b-a2-api26-smoke/evidence/sdk-inventory/result.json)于`2026-10-17T14:55:52Z`～`14:55:53Z`实际取得：

- 锁定SDK有两个已安装image package：`system-images;android-36;google_apis;x86_64`、`system-images;android-37.0;google_apis;x86_64`。
- `package.xml`实际API字段分别为36和37.0，与对应`source.properties`一致；SDK内无`android-26`目录或API26 image package。
- 默认SDK目录存在，但没有`system-images`；环境变量未提供额外SDK根目录。
- 记录原始环境查询argv、退出0、stdout/stderr字节数/SHA，SDK元数据的原始路径、字节数与SHA。没有调用sdkmanager、下载/升级镜像或接受新许可；SDK目录只读。

结论只针对本轮检查的已配置/默认SDK根目录，不声称已搜索所有磁盘或把未安装的下载包当作可用系统镜像。**缺少已安装API26镜像**是当前确定阻塞，不用API36/37替代，不继续尝试AVD或App。

## 实际修改及核查

仅新增A2环境只读脚本/证据、本执行报告、[结果与风险](M10_JPEG_H2_D2B_A2_RESULTS.md)、checkpoint，并更新CURRENT_STATE。研究Java、WorkerService、StageStore、Manifest、签名、Gradle、测试、正式产品代码、所有原始FAIL/UNKNOWN证据均保持不变。

实际执行文档/JSON/来源/链接/差异/敏感信息核查，结果及文件SHA见[交付清单](m10-jpeg-h2-d2b-a2-api26-smoke/evidence/delivery-verification.json)。本轮没有运行JUnit、D1模型、Lint、Gradle或重新核验APK实物；A1既有295项JVM与APK审计只是历史，不计作A2新PASS。

## 停止及边界

新AVD创建/隔离实证、三方设备身份与UID、APK安装、唯一无命令Host冷启动、系统日志、进程/研究文件元数据、直接READY证据均NOT_RUN；未启动不等同于观察后UNKNOWN。Worker早期onBind本就不在授权范围，继续NOT_RUN。身份门禁Android验证PENDING，watchdog最坏响应继承UNKNOWN，本轮验证NOT_RUN。

ADB/模拟器/App命令0；原`Lithic_M01_API_36`及其数据目录未读取/复制/挂载/修改，原H2 stage完全未访问，不把“本轮未访问”写成原设备私人字节或188项元数据已核对一致。没有normal、Outsider、cancel、SIGKILL、恢复、API27、JNI、JPEG生产修复或M11。

只提交推送A2分支后停止，等待ChatGPT独立验收。后续若要取得缺失镜像需另行明确授权，本轮不下载或建议自动执行安装。M10 PENDING/三JPEG FAIL、R3完整生命周期BLOCKED、UI-C1冻结、M11 NOT_AUTHORIZED及D2A-N01～N05/所有历史NOTE不变。
