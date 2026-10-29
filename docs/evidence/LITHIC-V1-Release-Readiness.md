# LITHIC V1.0 综合验收准备与发布范围审查

2026-10-29；仓库 `cc7279694-debug/property`；分支 `codex/lithic-v1-product-completion`；核对源码基线 `d0e153fbe30fbce35703a48f6a0aa94b826da620`，开始时本地/远端一致、工作区干净。本轮仅审查源码、历史原始证据及现存交付文件，未运行测试、构建、SDK/ADB或操作设备；只新增本文。

**正式V1.0：NO-GO。** 核心功能已有真实实现，当前主要缺口是当前候选的端到端数据安全、运行及升级证据，不是需要再造一套产品。**建议 CONDITIONAL GO 的对象仅为下一次独立验收候选准备**：先由ChatGPT审查本文和范围，再取得明确构建及设备授权；本轮没有授权安装、执行或发行。

## 1. 当前能力与证据层级

下表“桌面”指对应报告的特定源码/JVM/编译证据；“Android历史”只属于当时提交、设备与宿主，不能累计成当前版本全链PASS。当前完整普通App旅程、真实系统行为及升级，未有相应新证据者均为**未验证/NOT_RUN**。已读[当前状态](../CURRENT_STATE.md)、[限定验收登记](../ACCEPTANCE_MATRIX.md)及冻结报告，不改写它们。

|能力|真实源码已实现|桌面已验证的范围|Android历史与当前缺口|
|---|---|---|---|
|物品创建、编辑|`ProjectEditorScreen`→`ProjectViewModel`→`ProjectRepository`，生产导航已接线|[M06](M06-verification.md)、[P01](LITHIC-P01-Core-Experience.md)、[C7](LITHIC-C7-Product-Integration.md)；含保存后返回来源修复|M06当时Room/UI及普通App合成操作有证据；当前候选完整创建/编辑/返回未验|
|结束、修正结束、删除、纠错|原结束/删除确认、`ProjectWriteRepository`资格与事务保护|[M07](M07-verification.md)、C7；不等于任意记录均可删改|M07当时Android证据；当前危险确认、关联交易及错误恢复未验|
|投入、收回、成本、以旧换新|原金额表单/项目引擎、`ReplacementRepository`及对照页；维修以真实投入及描述记录|[M06](M06-verification.md)、[M16](M16-verification.md)、C7；精确金额及现金/非现金事实保持|M16隔离宿主/合成账本局部Android证据；当前完整物品旅程未验|
|订阅创建、真实续费|`SubscriptionRepository.createWithRenewal/confirmRenewal`及原表单|[M08](M08-verification.md)、C7；30/90/365、历史平均及当前套餐分开|M08当时Android局部证据；当前Main完整旅程未验。**没有通用订阅名称/类别/备注编辑入口**，这些资料在创建时记录，不能宣称全部可编辑|
|暂停、恢复、停止、重新启用、付款纠错|状态及付款资格、确认、原写入/结果未确认保护|[M09](M09-verification.md)、C7重新启用错误反馈|M09当时Android证据；当前预付/到期/暂停/停止、末笔付款及返回流程未验；停止记录不替服务商取消扣费|
|续费及里程碑提醒|Planner、WorkManager、Receipt、水位、受保护通知引用及设置|[M20](M20-verification.md)、[M21](M21-verification.md)及最近同步调度异常修复|M21原31方法首轮PASS仍有效于原候选；M20额外平台FAIL/收口UNKNOWN仍在，**当前系统调度/送达/取消未证明**|
|首页、列表、历史、名称搜索|真实Overview、窗口分页、来源返回；真实空/0/负值/读错分开|[P01](LITHIC-P01-Core-Experience.md)、[P02](LITHIC-P02-Browse-Review.md)、C7及后续列表精修|[M14](M14-Android-verification.md)为原隔离用例；当前候选刷新、页尾、返回及全App视觉未验|
|单项/总趋势、精确选日、事件、长期榜单|原时间线/图表/排行榜、50条继续加载与记录跳转|[M15](M15-verification.md)、[M17](M17-verification.md)、[M18](M18-verification.md)、最新旧图读错保留修复|M17/M18原隔离Android局部证据；当前真实VM/Room并发、全量规模、切换流畅度未验|
|类别分析、年度回顾|真实报告引擎、单类别自动呈现；现金/非现金及两类分析口径不混算|[M19](M19-verification.md)及已接受的单类别呈现修正|M19原隔离Android证据；不是当前Main整体通过，不制造非法跨类型年度总支出|
|完整备份|正常Main接`BackupExportService`，SAF选择`.lithicbackup`，写完关闭并读回校验|[P03](LITHIC-P03-Settings-Data-Safety.md)、[最新稳定性](LITHIC-V1-Stability-Closure.md)；成功/取消/失败/残留/未知分开|历史容器/媒体往返证据不替代当前手机真实SAF完整闭环；本地文件未加密、无自动云备份|
|完整覆盖恢复|先完整验证→计数预览→明确确认；原Token、同代快照CAS及Owner切换|P03、M21 format4/旧proof3限定Android证据、最新重建警告修复|当前Main/真实文件选择器、失败/中断/重开及实际升级恢复未验；不宣称跨Room/文件事务|
|用户JSON/CSV导出|**没有独立用户JSON/CSV出口**；ZIP内部`data.json/settings.json/manifest.json`是备份实现|[原V1规划](../../LITHIC_V1_产品与开发规划.md)明确不做CSV/Excel|属于原范围边界，不是漏做一个已承诺功能；JSON内部编码不能当用户导出已完成|
|三主题、表单与错误提示|炭黑/纯白/石雾；真实外观存储、MoneyMetric、原dirty/提交未知保护|[C9](LITHIC-C9-Visual-Theme-V2.md)、[C9-R1](LITHIC-C9-R1-Final-Visual-Refinement.md)、稳定性修复|用户旧版片段体验不等于当前全页面验收；360/400dp、真实字体/IME、TalkBack及导航动态仍待验|
|已登记封面只读|C6-1同Owner读取/校验及有界缩略图、真实页面接线|[C6-1](LITHIC-C6-1-ReadOnly-Media.md)仅桌面冻结|14项Android NOT_RUN、资源峰值UNKNOWN；新增/替换/移除仍**MEDIA_IMPORT_BLOCKED**|

关键源码核对：[生产Main与备份接线](../../app/src/main/java/com/cooper/lithic/app/MainActivity.kt)、[真实导航](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt)、[设置入口](../../app/src/main/java/com/cooper/lithic/feature/settings/SettingsShell.kt)、[备份服务](../../app/src/main/java/com/cooper/lithic/data/backup/BackupExportService.kt)、[恢复协调器](../../app/src/main/java/com/cooper/lithic/data/backup/RestoreCoordinator.kt)。内部旧`BackupExporter.exportComplete()`占位不是生产UI路径，不能据此说完整备份尚未实现。

最近一次桌面验证为**180唯一JVM PASS、Debug/AndroidTest编译/Lint exit0，Lint0错误16警告**，不是本轮重跑；其RED 13 FAIL及完整原始记录仍保留于[稳定性报告](LITHIC-V1-Stability-Closure.md)。新状态转换测试没有执行真实Android VM协程、系统重建或Room竞争。Preview12原字节未改，**不包含这些后续稳定性修复**，不能用它验证新修复。

## 2. 正式版本范围建议（等待独立确认）

|分类|事项及准入条件|
|---|---|
|必须阻断正式发行|当前同源候选的核心写入/纠错、离线完整旅程、真实SAF备份→完整覆盖恢复→重开事实一致尚未完成；失败/取消/未知、重复确认及恢复后的Owner读取未有完整当前运行证据。不能用免责声明替代数据安全|
|必须阻断相应长期使用承诺|正式T03规模、实际关键输入/保存及大字体/IME待验。建议保留为V1门禁；若用户要限制支持规模/设备宽度，须独立批准明确范围并先实测，不能缩小样本后把原方法登记PASS|
|必须阻断正式发行|Preview同包升级及M13-T06长期签名/保管、发行门禁G1～G4未关闭；Debug证书连续性不是正式Release准入。本任务不启动M23|
|可以提议明确降级|提醒可定位为可选、尽力送达，不能承诺厂商后台准点/真实重启送达。但仍须实测权限拒绝、开关、停止/重新启用、取消与无重复风暴；默认两个提醒开关为true，**不是已经禁用**。若要默认关闭或不提供提醒，需另获产品决定，不能只写一句“不保证”就绕过活动任务/资源收口UNKNOWN|
|可以提议延期|照片新增/替换/移除及C6隔离/JPEG路线独立延期，保留最终功能目标，需用户确认首版范围。只读封面也不宣称设备安全/性能已通过；完整备份仍须保留已有登记媒体原字节，不能借延期丢弃图片|
|原范围之外/可延期|CSV/Excel及独立JSON用户导出本不属当前V1；订阅通用资料编辑无现成入口，列为范围确认项，不在验收任务中顺手新增。更广OEM、全部断电/LMK组合可另排，但关键失败保护和当前支持设备仍须先验|

**历史不改判：** [M20平台记录](M20-Android-verification.md)25唯一方法最新24 PASS/1 FAIL，额外平台窗口两次FAIL；[第二次收口](M20-Platform-closure.md)周期/候选/OS Job/指定通知/应用Owner为UNKNOWN/BLOCKED。同步异常修复不证明平台已收口。M22原137 JVM、36 Android PASS及T06三次历史FAIL全部保留；[最新T06离线门禁](M22-T06-Physical-Verification.md)飞行1/Wi-Fi0/mobile0但`dummy0`仍有默认路由，T06执行0。路由存在不等于可联网或产品Bug，原零默认路由合同**不能自行放宽**。M10三个JPEG FAIL、M13延期、Q03/Q04、G1～G4及C3关闭调查的UNKNOWN保持。

## 3. 下一次只需要一个独立验收候选

建议在本文获审、另获桌面构建授权后准备**V1-A1 App＋Test一对候选**，不是Preview13或正式版：

- 新的显式opt-in身份拟为`com.cooper.lithic.v1acceptance.debug`及`.test`，独立构建目录，与Private Trial/M22/C3/C6开关互斥；目前**尚未实现这个身份**。候选单独标版本，沿用原Debug证书；普通Debug/Release/所有历史包身份保持。
- App保留正常`LithicApplication → MainActivity → LithicApp`，绑定当时获审完整源码提交/树、Manifest、权限、DEX、APK SHA及证书。Test只选明确方法名单，不整类/整包；合成数据仅在测试源集/UUID目录。产物获审实物应复制并重算SHA至非Gradle的稳定候选目录，避免唯一原件依赖临时build目录。
- Test沿用`M13IsolatedRunner`执行原plain Application/ComponentActivity夹具。**MainActivity依赖真实LithicApplication，不能在同一plain宿主中启动并称为普通App验证。** 先完成隔离方法及Owner/Room/进程/任务收口核对，再在独立的正常启动会话验证新包MainActivity及其可丢弃默认账本；若plain宿主终结无法确认，停止，不直接启动Main、不自行强停。正常Main初始化业务Owner/WorkManager是此阶段预期范围，要纳入结尾核验，不能叫“零业务初始化”。
- 为原IME前提预检，建议在同一Test源集另列一个最小只读配置探针，拟名`V1AcceptanceConfigurationTest#plain_host_activity_configuration`：同一M13 plain宿主只读取实际ComponentActivity配置，不建Owner/Room、不截图、不改系统设置。它尚未实现，须随候选单独审查并在设备授权中另计一次额度；不是重开C3。无法取得经批准的Activity实测时，原IME保持NOT_RUN，不偷跑原方法、不以Application资源或全局尺寸换算代替。
- 旧Preview12、M10/M22/C3及其数据/UNKNOWN现场均不动；不使用曾SystemUI ANR的旧AVD，不重开C3采集调查。API36/arm64可先验，API26及其他支持ABI仍单列缺口，不拿一个手机代表全部兼容性。
- [C6-2D环境阻断](LITHIC-C6-2D-Runtime-Verification.md)及稳定性报告所记C6 App临时APK缺失是既知NOTE；旧SHA不是现存文件证明。本轮不重建，不让它阻断非照片验收准备，也不借V1候选执行C6测试。

设备授权应一次写明：目标设备/USB序列仅本地、全新包未安装门禁、一次App/Test安装额度、逐方法一次、正常App操作、用户手动设置及恢复、有界目标包查询/证据提取、异常停止与保留规则。默认**不含**强停、清数据、卸载、root、取消旧任务或处理其他包。建议集中会话上限120分钟、每个原方法外层观察上限15分钟；**不改变原内部超时**，尤其T03的120秒只约束全量趋势计算，不能把它误作整方法时限。外层超时不证明JUnit FAIL或设备进程已结束，须记TIMEOUT/终结UNKNOWN并停止依赖项，不自动重跑。

## 4. 一次集中真机验收执行单（本轮全部NOT_RUN）

执行前锁定候选及清单，经ChatGPT审查并取得用户明确设备授权后再进行。所有步骤登记首次结果；数据安全、ANR、身份异常或Owner/任务UNKNOWN立即停止依赖步骤并保留现场，不自动修复/重试。某项配置前提不满足记NOT_RUN，不能改断言凑PASS；互不依赖的步骤仅按已批准顺序继续。

|顺序/前提|操作|预期结果|失败时停止与证据|
|---|---|---|---|
|01 安装准入|确认唯一授权设备/API/ABI/Boot、健康/电量/可用RAM及存储；新App/Test含保留记录均不存在；候选SHA/证书/Runner/targetPackage一致后按额度安装。保存原网络/字体/density/主题|独立UID、私有目录/Provider，正常App与测试宿主合同清楚；无跨包访问|已存在包、身份不符、资源不足或不健康即不安装。只留脱敏安装元数据/命令退出码；不读取旧包账本、不采个人屏幕|
|02 原T06：仅独立plain阶段|用户手动飞行1、Wi-Fi/数据0，按原合同核对全部IPv4/IPv6表零默认路由；执行`CompleteJourneyTest#runtime_offline_journey`一次，保持全部原金额/日期/备份/恢复断言|完整到终点，UUID合成数据正确；Owner/Room明确关闭后按成功归属条件清理。此法使用受控文件，**不等于Main/真实SAF**|离线不满足不执行；`dummy0`需先独立审查替代合同，否则仍阻断。FAIL保留完整Room/WAL/SHM、备份/媒体/Owner标记、阶段/JUnit/退出码及截图；不重跑。不能凭Runner退出认定关闭|
|03 原T03：仅02 PASS且收口/资源健康|执行`LongHistoryPerformanceTest#full_twenty_year_dataset_reads_exact_global_facts_and_remains_responsive`一次；固定2006-10-23～2026-10-23、seed20261023、1000项目/1000订阅/20000金额事件/10000续费|真实Room表计数达标；完整分页/搜索/统计/7306日全量趋势/渲染/精确选日/取消旧请求正确；原120秒计算预算不变|OOM/ANR/超时/漏重/事实错立即停止；留准备与各阶段耗时、App RSS/PSS/可获取峰值和响应记录、计数、JUnit。测不到标NOT_CAPTURED，不用宿主内存替App峰值|
|04 原IME：仅plain阶段且设备健康、用户同意|用户手动调整后，仅用另获批准的一次只读plain-host探针记录Activity实际`screenWidthDp/fontScale/densityDpi`；只有359～361dp且真实系统font_scale=2.0才执行原`M22AccessibilityTest#system200_real_ime_keeps_production_form_input_and_save_reachable`一次|真实IME显示、输入/保存可达，隐藏后输入保留；实际配置与截图/Insets证据对应|配置取证未获授权/不完整或自然宽度约400dp则原方法NOT_RUN；不改全局density、不用LocalDensity/全局换算代替，不普通启动Activity绕过plain隔离。恢复实测原字体；不额外调用未批准方法|
|05 切换至普通Main阶段|先核对前述合成Owner/Room/协程收口、目标进程/任务；另次正常启动新包Main，确认真实LithicApplication和新包自有默认代|不复用plain宿主、不读旧Preview数据；此后使用正常生产UI/可丢弃账本|关闭证明不足、plain仍活动或归属不明停止；不强停求过门禁。保存宿主类型、代/UUID归属与必要元数据，不公开硬件序列|
|06 空首页→物品旅程|空/未知/读错分开；创建项目、修改、追加投入/维修/收回、纠错、结束/修正结束、历史、删除确认；测试返回保留输入及重复点击保护|同日精确事实可核对，真实0≠无记录；负净投入保留负号；结束冻结天数，后续结算不伪造持有时长；取消删除无写入|金额/日期/重复写入/删除资格或保存结果不明停止；保留前后事实、operationId状态及目标App截图，不引导重复提交|
|07 订阅/以旧换新|创建并确认真实首付/续费，核对月/季/年、未来预付、暂停/恢复/停止/重新启用及历史；另走以旧换新输入→核对→确认及关联纠错保护|当前套餐与历史日均/付费折算分开；未来预付覆盖不虚增已用时长，实际付款日晚于记录时区今天应拒绝；交易非现金不冒充现金；取消无写入，停止不表示取消服务商扣费|出现凭空付款、关联丢失/重复或未知即停；留金额/覆盖日期/状态/关联事实。无通用订阅资料编辑入口按现状登记，不编造操作|
|08 浏览与分析|首页/分页列表/搜索→详情→原来源；单类别/年度/长期榜单/历史；总及单项趋势选日、读旧结果、全部事件/50条继续加载；三主入口及深页返回|精确日期/负值/0/null正确，非现金口径不混；首次占位与同代刷新稳定，旧截止日/错误/重试真实可见，无新旧页面叠读|错误藏匿、跨代旧结果或来源错即停相关路径；留所选日期、当前/保留结果状态、有限目标App录屏。流畅度只记实际观察，不从JVM推断|
|09 正常Main＋真实SAF完整闭环|由生产设置导出完整备份并校验；记录事实清单→修改/新增可丢弃数据→选择原文件→核对当前/备份计数→取消一次确认无变化→重新选取并明确覆盖→退出重开逐项核对|原项目/金额/订阅/状态/交易关联/记录时区与提醒设置符合format4；完整覆盖不合并、无重复记录。只在真实Success显示成功|误覆盖、数据少/错或Owner读取不一致停止；留原/后备份长度SHA、预览/确认、代身份及事实差异。比较同观察日计算，不把跨日成本自然变化当恢复错。外观主题不在现有format4设置DTO中，不承诺随备份迁移|
|10 坏文件/取消/中断|仅新包：取消选择、坏/截断备份、连续确认及正常Activity重建；在**预先批准的测试中断点**检查导出/恢复结果未知重建提示。另用获审的登记PNG/旧proof3合成备份测试原字节兼容，不调用图片导入|校验失败不覆盖，Token/确认不重放；`currentPreserved`未知不假称保留/成功；目标残留文件提示可见，未知标记跨重建保留；登记媒体原字节SHA正确|来源不保或结果不明立即停，完整保留新包合成现场；取消界面不等于事务已取消。系统杀进程/断电未另获授权则NOT_RUN，不擅自force-stop制造中断；媒体缺口不允许丢弃原字节|
|11 普通App离线/字体/IME|用户手动断网并核对批准合同，正常退出重开/浏览/写入/搜索；实际400或360dp记录真实宽度，三主题与获准系统大字体下输入、滚动、键盘、保存/返回；随后恢复原设置|无网络依赖，输入保留、关键动作≥48dp且可达；关闭键盘后仍有原输入；未知写入不重复提交|网络前提不成立则离线项NOT_RUN；真实宽度下的补充检查单列，**不回填原360dp/200%方法**。只截合成App/必要IME，避开个人候选词/通知；没有运行的宽度/字体/横屏/TalkBack继续NOT_RUN|
|12 提醒真实平台与结束核验|仅新包，用户批准通知权限操作；拒绝→允许、开关、符合当前记录时区窗口的真实合成记录、停止/重新启用/取消及重复触发检查；结束还原网络/字体/density/主题并复核包身份及目标Job/进程|区分注册/Pending/Active、实际发布/送达/点击和业务Owner关闭；允许未来注册任务不等于活动。同期状态/无重复证据及用户可见提示真实|无合法时间窗口或有界观察未送达记NOT_RUN/UNKNOWN，不改系统时钟、不刷通知求PASS。未知任务/资源仅保留，不能自动取消/清理。留目标UID/namespace/Job证据及原设置恢复回执；进程消失≠优雅关闭|

普通UI的可复算锚点：以记录时区观察日D，项目开始D−9、初始投入¥100应持有10自然日、¥10/天；D再投入¥20、收回¥30后净¥90、¥9/天，再收回¥100后净−¥10、−¥1/天。另建真实¥0项目与无记录对照。月付¥30/30天的**标准折算**为¥1/天，不能据此推断同日历史平均值或已经使用30天；付款、覆盖及暂停按原引擎事实核对，不调整手机时钟。

每步证据至少绑定：候选SHA/提交、设备API/ABI及Boot摘要、目标UID、方法或人工操作、首次结果、原始stdout/stderr/外层退出码/JUnit结束状态（人工项注明不适用）、合成UUID/阶段、必要截图尺寸与SHA。无结束状态不写PASS/业务FAIL。敏感备份不入Git，不导出私人IP/序列/其他App日志；失败现场只在新包内按授权有限提取，不整机抓取、全量logcat或清理未知文件。

## 5. Preview12升级：另一个必须明确授权的门禁

已重新计算本地Preview12实物：**15,977,934字节**，SHA-256 `4b6b3ed03674fc971b67a124171fedbb89fce155d2c6fd181ed597cde6250a1b`；仅本地保存于`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/deliveries/lithic-preview-0.1.0-preview.12/Lithic-0.1.0-preview.12-debug.apk`，未提交APK。[原交付静态核验](LITHIC-C9-R1-Preview12-Delivery.md)为`com.cooper.lithic.preview.debug` / code28 / `0.1.0-preview.12-debug`，证书SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。本轮未重新调用SDK验签，也未查询手机；**手机当前安装字节/记录仍须将来实时核对**。

1. 先完成独立V1-A1安全验收。通过后才另行批准生成**一次同源、同包名、同证书且code>28**的用户升级候选，保留Preview12原APK；此时仍不等于正式Release。
2. 优先在另获授权的空白设备/Android用户空间，用**原Preview12 APK＋可丢弃合成账本**完成真实同包覆盖升级、重开、事实核对及备份恢复。不克隆用户账号/私人数据，不使用故障AVD；没有这种环境时该升级门禁仍待验，独立新包安装不能代替它。
3. 覆盖用户实际Preview前，取得**该次覆盖的明确许可**；由用户导出到App之外并确认备份可读，重要记录保留独立副本。只有另获许可才把其备份用于新独立包恢复核验，不读取旧包私有数据库。备份不能可靠导出/验证时不升级。
4. 核对手机原包、UID、版本/签名及新APK；只正常同包更新。签名/安装失败、数据不一致或恢复未知则停止并保留现场，**不卸载、清数据、降级或重新安装求过关**。升级后逐项核对真实事实、设置、媒体引用及重开；不能因为安装成功就关闭M13-T06或所有异常中断门禁。

## 6. 本次停止点

当前具备本地物品/订阅成本管理的主要源码能力，建议不再新增普通模块；先完成上述一个独立候选的集中验收。正式发行仍**NO-GO**，首版是否延期图片/限制提醒承诺或数据规模，需要ChatGPT和用户明确决定；本报告不替他们批准降级。

本轮无新增运行结果、无APK、无环境修复；Room5、backup4、领域规则、Owner/generation、恢复Token/原子切换、C6-1及Preview12保持。报告提交后停止，等待独立审查及新的构建/设备授权。

**ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED**
