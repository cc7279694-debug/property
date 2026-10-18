# M10-H2-D2B-A2-R-C2 — 隔离API26验证

2026-10-18（Asia/Shanghai）。**BLOCKED — M10-H2-D2B-A2-R-C2 / API26_BOOT_TIMEOUT**。

C2一次AVD创建成功；唯一一次模拟器启动后，300秒启动完成等待期内始终未取得`sys.boot_completed=1`，监督器退出1并停止后续流程。只读结束观察确认实际API26、目标AVD名称匹配，boot属性仍为空。**没有安装APK，没有启动Host，不能声称Host已运行或API26兼容验证通过。**

## 来源、授权及最小修改

来源C1 `797eb591cc8293d9d68503243d2a94462fe79610`，本地/远端匹配且工作区干净。用户转交C1已验收，正式批准C2四步各最多一次。通过管理工具建立短路径worktree `E:/CODEX/.codex/worktrees/lithic-c2/砺石新APP`，分支`research/m10-jpeg-h2-d2b-a2r-c2-isolated`，没有修改C1或正式分支。

按[SCOPE](m10-jpeg-h2-d2b-a2r-c2/SCOPE.md)执行。C2监督器从原A2-R脚本派生，只更换独占环境/状态身份、应用已审查的环境修正，并增加一次发现和证据门禁。原A2-R脚本、失败marker及历史证据不改。C2子进程显式`pop("ANDROID_SDK_HOME", None)`防继承，SDK根保持一致；USER/AVD/EMULATOR home指向新根。只读检查PREFS_ROOT、JVM选项、ADB server覆盖变量，无未审查冲突，全局变量前后未改。

| 对象 | 实际路径/身份 |
|---|---|
| 新C2根 | `E:/CODEX/.codex/research-env/lithic-a2r-c2-api26` |
| AVD | `Lithic_A2RC2_API26_20261018` |
| 注册/用户/模拟器目录 | C2根下`avds`、`android-user`、`emulator-home` |
| 数据/状态 | C2根下`avd-data/Lithic_A2RC2_API26_20261018.avd`、`c2-state` |
| 端口/ADB serial | 5612/5613，`emulator-5612` |
| 复用SDK/镜像 | 原研究根`lithic-a2r-api26/sdk`，API26 / google_apis / x86_64 / r16 |

新根执行前不存在，路径无链接，端口对空闲，磁盘约60.87GiB空闲。复用cmdline-tools23.0、emulator37.1.11、platform-tools37.0.1、既有JDK17，不下载/升级/接受许可。运行前11个镜像原件及536个工具文件逐SHA与原锁定记录一致；生成package XML只核对身份与哈希，**不提交其许可正文**。

## 真实执行与计数

所有时间下列为上海时间；原始JSON保留UTC。

| 步骤 | 次数 | 实际结果 |
|---|---:|---|
| 官方avdmanager create | 1 | **PASS**；12:19:51.453～12:20:22.066，exit0；无force/覆盖参数，新配置及注册路径核对通过 |
| 模拟器spawn | 1 | **PASS（仅进程启动）**；12:21:38.195，PID20024，目标qemu PID22572；`-no-snapshot`、新datadir和显式镜像路径 |
| 全局`adb devices -l` | 1 | **PASS**；12:21:51，原输出只在内存，保存新目标行及完整性元数据 |
| 启动完成门禁 | 1个有界等待期 | **BLOCKED**；61轮get-state/boot读取，boot始终为空；12:26:55.783监督器记录deadline expired、exit1 |
| Probe APK安装 | 0 | **NOT_RUN**；启动门禁未通过 |
| Host Activity启动 | 0 | **NOT_RUN**；启动门禁未通过 |
| 重试/清理/停止 | 0 | 未重发创建、启动、安装或Activity，无kill/force-stop/清数据/卸载 |

运行命令证据共133条（不含APK静态审计的5条）：127条ADB＝1全局发现＋122轮询读取＋4结束读取。126条定向命令均为`-s emulator-5612`；全部命令退出0，**不能覆盖监督器因boot门禁失败而退出1**。每次命令保存退出码、时间、原始长度/SHA及保存输出SHA；全局发现隐私过滤且明确标注。模拟器spawn是进程创建记录，不冒充ADB命令。

## 验收结果矩阵

| 项目 | 状态 | 依据/限制 |
|---|---|---|
| 子进程环境最小修正/新目录静态隔离 | PASS | 无旧SDK_HOME继承，新独占目录、端口及root门禁 |
| 镜像及工具身份 | PASS | 前置11＋536逐文件SHA；无SDK更新 |
| 锁定A1 APK宿主审计 | PASS | 原件83761字节，SHA `0d98316faf6cb18b589bad526a5d8260cea825a408ed949f32855bd7a7d31b8a`；签名/Manifest/权限、48构建输入核对 |
| APK证书/Worker属性 | PASS（宿主） | 单证书`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，v2；min26/target36、零权限、allowBackup=false、Worker非导出且`:jpeg_worker` |
| 一次AVD创建与配置/路径 | PASS | 官方工具exit0，config/descriptor哈希及新路径 |
| 模拟器进程实际启动 | PASS | spawn与目标Windows进程身份；不等于Android启动完成 |
| 新serial与AVD名 | PASS | 单次发现及结束`emu avd name`精确匹配 |
| 实际API26 | PASS（结束观察） | 结束`ro.build.version.sdk=26`；不是完整启动PASS |
| `boot_completed=1` | BLOCKED | 300秒期限内未取得，结束观察仍为空 |
| APK安装身份/UID | NOT_RUN | 未安装，不伪造设备UID |
| Host进程/角色/后台核验 | NOT_RUN | 未发Activity启动命令 |
| 直接READY | UNKNOWN | 无Host运行证据；即使无命令启动，锁定APK也没有直接READY输出，不能用APPLICATION_ROLE替代 |
| 主线程身份探测真实耗时 | NOT_RUN / 未取得 | 未运行Host，100ms策略不作为测量 |
| Host ANR/crash/诊断完整性 | NOT_RUN | 没有Host窗口；模拟器日志不证明Android系统无ANR/crash |
| Worker/Session/stage/current/pin的App侧检查 | NOT_RUN | 未进入App阶段，不宣称实际已检查并不存在 |
| Worker早期绑定/Binder/Watchdog/API27/H2恢复 | NOT_RUN | 不在本轮范围 |
| 旧A2-R宿主marker/空目录保护 | PASS | 227字节marker SHA不变、旧数据目录仍空、旧`.ini`仍不存在 |
| 全局环境/原APK保护 | PASS | 限定字段前后相同、原APK SHA相同 |
| 原API36/H2/私人字节 | NOT_RUN / 未访问 | 没有向旧设备发送命令，没有访问原AVD或H2 stage |

## 证据及未解决问题

入口见[关键证据索引](m10-jpeg-h2-d2b-a2r-c2/README.md)，机器命令/计数见[command-index](m10-jpeg-h2-d2b-a2r-c2/evidence/command-index.json)。6个唯一宿主guard测试通过；三次执行对应小范围证据判定收紧，均6/6，不累计成18项。最终执行输入SHA有独立记录。没有JUnit、Lint、APK构建或产品JPEG测试重跑。

AVD创建成功支持C1环境修正的实用性，但不能仅凭新命名空间下成功就把旧失败的唯一根因宣布已证明。新的阻塞是Android启动未完成，**根因UNKNOWN**。native启动输出报告WHPX可用，不足以证明guest完整正常；本轮没有扩大到系统日志根因诊断，也没有加长期限或重启。

结束宿主观察仍有PID20024/22572，保留运行现场。活跃日志继续仅本地保存并精确忽略；提交的是结束时不可变前缀快照（stdout6158字节、stderr0），带长度/SHA和时间边界，不宣称未来日志也完整。C2配置/注册文件有只读结束副本；userdata/SDK/镜像/APK/许可正文不提交。

结束对镜像全部原件检查尺寸，并复核source.properties/package.xml/kernel/ramdisk哈希；不是第二次完整11＋536 SHA审计。结束结果JSON中的`deviceEnding=NOT_RUN_TARGET_BOOT_OR_DISCOVERY_NOT_CONFIRMED`指完整boot-confirmed identity流程未执行，其后保存的四条独立结束读取真实发生，分别得到device、正确AVD名、API26、空boot值；不将该字段解释为完全没有设备结束观察。结束保护PASS只适用于宿主保护比较，不是C2或启动通过。

保留C1许可整改：7份正文不重新进入跟踪，原始哈希/历史不改；旧远端历史仍可访问正文。旧安装重试的流程偏差不追认。D2A-N01～N05、A1-N01～N04及全部NOTE保留；M10 PENDING/三项JPEG FAIL、UI-C1 FROZEN、M11 NOT_AUTHORIZED、H2 DELIVERY_UNKNOWN/pin=true及R3完整生命周期BLOCKED保持。下一步只等待独立验收决定是否另行授权C2启动阻塞取证，不自动操作该AVD或进入后续阶段。
