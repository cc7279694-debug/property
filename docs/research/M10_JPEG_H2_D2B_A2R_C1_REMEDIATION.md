# M10-H2-D2B-A2-R-C1 — 交付整改与流程审计

2026-10-18（Asia/Shanghai）。范围：DOCS / STATIC ONLY。来源为`4ee27bfcbe5903d6a49d4a92203de8a63686ba66`，继续分支`research/m10-jpeg-h2-d2b-a2r-api26-isolated`，worktree为`E:/CODEX/.codex/worktrees/lithic-a2r/砺石新APP`。开始时本地HEAD匹配、工作区干净；没有覆盖其他工作。首次远端读取遇TLS/EOF错误，后续只读读取成功，确认A2-R远端仍为锁定来源，三个受保护远端分支保持原值，见[提交前远端核对](m10-jpeg-h2-d2b-a2r-c1/evidence/git-baseline.json)。

## 契约及实际边界

- Goal：修正许可正文超出交付范围、核对重试授权，静态分析AVD目录错误并提出最小修正。
- Scope：研究文档、当前跟踪范围、精确忽略规则、静态读取及新的C1取证/验证辅助脚本；原启动监督器只提交未应用提案。
- Out of scope：SDK/AVD命令、ADB、任何App或设备操作、下载更新、Host smoke、Worker、JPEG修复及M11。
- Verification：原件与锁定blob逐字节核对、移出跟踪但本地哈希相同、跟踪内容及链接检查、历史证据未改、SDK/marker/环境只读前后对比。

本轮没有调用avdmanager、sdkmanager、emulator、ADB、APK安装或Activity启动；也没有运行原`verify_delivery.py`。仅用JDK的`javap`静态读取既有class字节码，不初始化或执行SDK目标类，不是一次AVD工具运行。

## 许可正文整改

检查锁定A2-R提交全部86项变更，并检查其继承的2388项跟踪文件。定位7份SDK许可正文：A2-R四份（两个TXT、官方XML、生成包XML）及R1三份重复（TXT、CMake包XML、NDK包XML）。正文搜索同时覆盖普通文本、HTML实体和常见JSON/编码线索；未发现额外匹配。索引与扫描结果见[证据入口](m10-jpeg-h2-d2b-a2r-c1/README.md)。此检查不宣称可以检测任意私有编码或加密负载。

操作是对精确7个路径执行`git rm --cached`，并增加逐路径忽略规则；没有物理删除。7个原件移出索引前都与锁定Git blob字节一致，移出后本地原字节仍一致。保留原SHA-256、blob OID、官方URL、各XML许可ID和历史结果入口，见[来源索引](m10-jpeg-h2-d2b-a2r-c1/LICENSE_SOURCE_INDEX.md)与[操作记录](m10-jpeg-h2-d2b-a2r-c1/evidence/tracking-removal.json)。没有生成脱敏XML或复用原哈希为新内容背书。

仅为当前分支纠正跟踪范围；R1原分支/工作区不改。旧远端提交`4ee27bfc…`及更早提交仍可经Git历史访问正文。没有force-push、改写或清除历史，不宣称历史抹除。原结果JSON、失败stdout/stderr、create marker及旧checkpoint不改；历史报告只新增C1说明、纠正一个指向不再跟踪正文的Markdown链接及授权解释。

## 首次安装失败后的重试授权

结论：**PROCESS_DEVIATION — RETRY_WITHOUT_RECORDED_ADDITIONAL_AUTHORIZATION**。

| 事件 | 原证据与性质 |
|---|---|
| 用户批准官方API26镜像下载安装 | 现有明确授权，但不能用未读到的完整旧A2-R文本补造额外许可 |
| 用户答复“只限制下载流量（允许展开及AVD超过3GiB）” | 仅预算口径澄清；没有失败后重试许可 |
| 首次install | UTC 02:46:42.433～02:49:18.949，退出3221226505 / 0xC0000409，FAIL |
| 第二次install | UTC 02:50:23.650～02:55:33.476，同样异常退出；距首次结束约64.701秒 |
| `retryOnce=true`与`budgetApproval` | 助手脚本/记录字段，不构成人类追加授权证据 |
| 后置11文件校验 | 证明技术文件状态，不追认重试权限，不覆盖安装器FAIL |

证据为[首次intent](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/image-install/intent.json)、[首次结果](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/image-install/result.json)、[重试intent](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/image-install-retry-1/intent.json)、[重试结果](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/image-install-retry-1/result.json)、[范围原话及更正](m10-jpeg-h2-d2b-a2r-api26-isolated/USER_SCOPE.md)。未找到明确追加许可；因此登记偏差而不是推定授权。旧记录中的“受控”仅说明次数/预算，并非权限结论。旧JSON、退出码和原始哈希全部保留。

## 静态诊断及仍未知事项

详见[SDK静态诊断](M10_JPEG_H2_D2B_A2R_C1_SDK_STATIC_DIAGNOSIS.md)。确认子进程同时把`ANDROID_USER_HOME`设为`android-user`、把旧`ANDROID_SDK_HOME`设为`legacy-home`；旧变量指向`.android`父目录，两个有效偏好目录不同。

Google文档、WPT精确修复记录和本地23.0工具字节码共同支持该冲突触发错误的假设。原进程堆栈、完整当时继承环境和修正后的运行结果缺失，**运行时唯一根因仍UNKNOWN**。原错误不应被直接解释成SDK安装根目录不存在。

只提交未应用的最小环境修正；原`smoke_isolated.py`字节不改、唯一create marker不重置、空数据目录/研究SDK不改。下一次创建必须另行明确批准：是否应用提案、如何保留旧marker与目录而建立独立新attempt、最多创建次数、隔离与停止门槛；Host启动是单独边界，本轮没有追加许可。

## 验证与停止

C1只读前后快照、跟踪/内容/链接/敏感信息核查结果见[本轮证据索引](m10-jpeg-h2-d2b-a2r-c1/README.md)。JUnit、Lint、APK构建、AVD创建复试、Host/READY/Worker均NOT_RUN。原A2-R创建FAIL、两次安装器FAIL及全部历史UNKNOWN不改写。

实际结果：11项静态交付检查PASS；锁定来源2388文件与取证时暂存2421文件逐blob扫描，7→0许可正文匹配；7原件本地哈希相同且精确忽略；173个相对文档链接无缺失；未发现敏感模式或空白错误。结果自身不包含在该扫描时点，后续加入结果和本段总结后再次进行提交前差异/正文检查。研究目录元数据816→816、聚合SHA相同，选定SDK/许可接受记录/create marker及原smoke脚本SHA不变，限定环境查询前后相同。元数据相同不扩大为整个SDK逐字节重验，更不是原API36/H2保护实测。

状态保持：M10 PENDING / 三项JPEG FAIL；UI-C1 ACCEPTED / FROZEN — PASS WITH NOTES；R3完整生命周期BLOCKED；H2现场未访问；N01～N03、D2A-N01～N05及历史NOTE保留；M11 NOT_AUTHORIZED。整改提交后停止等待ChatGPT独立验收，不自行推进创建或smoke。
