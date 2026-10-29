# V1.0 稳定性缺口集中收口

2026-10-29；`codex/lithic-v1-product-completion`；起点`76ae1daf8e56f0205d94a10e3d6d6791dd830439`。目标是暂停照片端侧验证，修复可由源码及桌面定向测试证明的稳定性缺口；不重做UI，不改冻结领域/存储/恢复协议，不操作设备，不生成用户体验APK。本报告为集中实施证据，不自授产品冻结或V1.0发行通过。

## 本轮实际修复

|问题与根因|实际变更|证明及边界|
|---|---|---|
|通知安排的同步异常终结同代监听：Runtime原try包围初次request及整个merge.collect；一旦request抛异常，后续设置/DB事件不再被订阅，普通业务变更又不重发Ready|[ReminderRuntime](../../app/src/main/java/com/cooper/lithic/platform/notification/ReminderRuntime.kt)接入[单次请求失败边界](../../app/src/main/java/com/cooper/lithic/platform/notification/ReminderRequestObservation.kt)。仅request的普通Exception记日志后继续收后续事件；取消原样抛，上游Flow自身异常仍走原外层日志路径|5个有限Flow JVM真实调用该helper，初次/事件失败各曾真实RED。**不增加定时/自动重试**，不改Scheduling、Worker、Planner、Repository、Receipt、任务名称或资格；不证明异步WorkManager Operation入队/系统通知/上游自动重订阅|
|导出/恢复未知警告在重建时丢失：VM把IDLE当作结果已知，确认异常/取消/Failed(false)或重建后的无效晚到回调会清掉incomplete布尔标记|两个Controller增加仅供UI的requiresRecreationWarning，两个VM操作结束、取消或无效回调后的标记更新引用它；IDLE+OUTCOME_UNKNOWN/INTERRUPTED_UNKNOWN仍保留原键true|10个JVM用真实Controller反馈及实际VM引用谓词模拟重建资格。**不保存URI/Token/确认、不新增持久数据格式**；已知成功/失败/取消和忙碌态仍区分。主动新选择→取消按原P03合同处理，不把本次修改扩为永久历史警告；真实Android SavedState/进程死亡仍NOT_RUN|
|旧图假恢复为成功：完整读取失败后选日只重算retained事实，却无条件清error，令总趋势FAILED→READY、单图错误卡消失|[总趋势VM](../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendViewModel.kt)及[单记录VM](../../app/src/main/java/com/cooper/lithic/feature/report/ItemTimelineViewModel.kt)在非持久UIState分离readError来源；选日保留读错、只清该日错误，真正fresh结果才清读错|15项JVM驱动原VM实际调用的纯状态转换，涵盖总/项目/订阅、新日/同日、point失败重选、fresh清错。原M17/M18两个Android方法仅追加断言，原方法/原断言保留。**JVM没有运行真实VM协程/Room或票据竞争**；原read/point票据、取消、active/ID/引用围栏、查询/采样和金额算法保持|

备份UI修改文件：[ExportController](../../app/src/main/java/com/cooper/lithic/feature/settings/BackupExportController.kt)、[ExportVM](../../app/src/main/java/com/cooper/lithic/feature/settings/BackupExportViewModel.kt)、[RestoreController](../../app/src/main/java/com/cooper/lithic/feature/settings/BackupRestoreController.kt)、[RestoreVM](../../app/src/main/java/com/cooper/lithic/feature/settings/BackupRestoreViewModel.kt)。本轮没有发现可确定归因、需要改动冻结恢复底层的数据破坏缺陷；不为“收口”改协调器或弱化校验。

## 实际测试、构建与边界

|检查|实际结果|
|---|---|
|行为RED|先将旧判定/转换作行为等价提炼并接回实际调用点，再运行27新方法：**14 PASS / 13 FAIL，Gradle exit1**（通知2、重建警告5、图表6），原XML/堆栈及旧helper源码保留，不以编译失败冒充复现|
|最终JVM|**180唯一方法PASS，0失败/错误/跳过，14个选定类**。其中新方法30（5通知＋10重建＋15图表）；其余150为相关Controller/P03反馈、备份容器/取消、提醒Planner和时间线/呈现回归。13个原RED方法均仍在且通过；不累计RED/GREEN为双倍覆盖，不重跑整套历史验收|
|构建/编译/Lint|普通Debug assemble、AndroidTest源码编译、Lint组合exit0；**Lint0错误16警告**。GREEN开始RAM4.403GiB，离线/单worker，保留原SDK XML及旧源码warning，不下载/升级环境。Android/Compose/SAF/VM系统重建/设备性能实际未运行|
|研发APK静态|普通Debug仍`com.cooper.lithic.debug`/code17/`0.1.0-m22-debug`，正常LithicApplication/MainActivity、原证书`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，无INTERNET/新权限。APK中实际新增helper/状态类及两readError字段核对，测试类不入App；8个既有native条目与Preview12同字节，无新JPEG库。仅研发产物，不交付/复制为新体验包，不操作安装|
|冻结边界|151份领域/数据/Owner/恢复/媒体/资源/schema/Gradle等保护文件SHA保持，原SavedState键和业务资格不改；11个用户交付APK（Preview12在内）均同字节。**另C6独立App临时构建产物缺失，详下，不能写所有13个初始APK均保留**|

[RED命令与退出码](v1-stability-20261029-203941/red/command.json)、[RED逐方法失败](v1-stability-20261029-203941/red/results.json)、[GREEN命令](v1-stability-20261029-203941/green/command.json)、[最终逐类汇总（逐方法见同目录XML）](v1-stability-20261029-203941/green/jvm-results.json)、[Lint原XML](v1-stability-20261029-203941/green/lint.xml)、[APK身份/实际字段](v1-stability-20261029-203941/artifact-final/identity.json)、[SDK检查](v1-stability-20261029-203941/artifact-final/commands.json)、[源码绑定](v1-stability-20261029-203941/source-binding.json)。相同目录保存原始stdout/stderr、XML和源指纹。JVM只执行有限合成事实/文件，不读取用户账本或照片。

### 构建产物保留NOTE（新增，未解决）

[本轮前检](v1-stability-20261029-203941/preflight.json)含11个用户交付APK及C6候选2包；最终核对发现C6 App `app/build-c6-isolation/outputs/apk/debug/app-debug.apk` **已不存在**，配套Test仍原字节。C6 App原15,976,838字节/SHA`933387de7ce775d10d1f41bc438d650ea8ca273b1719911df51c091273d7627f`只能作历史指纹。没有手动删除、清理命令；在普通Gradle运行后发现，缺少逐文件删除日志，具体步骤UNKNOWN，不能凭推断归因。只在该已知build目录作有界核查，没有扩大环境研究或从别处猜包。

首轮产物审计因FileNotFoundError退出，保留[检查失败](v1-stability-20261029-203941/artifact/audit-first-failure.txt)和原审计源码；随后最终审计明确分开11个交付包PASS、仍存在C6 Test PASS及C6 App MISSING NOTE，**没有删除失败记录或伪造全保留**。[前后实物记录](v1-stability-20261029-203941/apk-preservation-observation.json)。本轮不重建C6、不换候选或补造旧SHA；今后若恢复隔离执行，须先另行授权重建/重新审查并将候选锁入非Gradle临时交付目录，再申请运行，不得直接沿用旧文件路径。此项不解除C6 ENVIRONMENT_BLOCKED/MEDIA_IMPORT_BLOCKED。


## 未关闭门禁及原因

|范围|仍未关闭的事实与原因|
|---|---|
|M20通知平台|原22 Android最终PASS、25唯一方法最新24PASS/1FAIL及首次21PASS/1FAIL保持；额外真实平台窗口断言两次FAIL发生在显式取消通知/周期及Owner.close之前，原实验保留ownerClosed=false。随后唯一冷启动收口出现正常App进程/新默认generation，因无法证明无新消费者而停止，未重开Owner或取消任务。故周期/候选/OS Job/指定通知/应用级Owner收口仍UNKNOWN/BLOCKED；本次同步请求监听修复与这些旧现场无因果证据，不改判|
|备份恢复|现有协调器仍撤销Token后NonCancellable验证/准备，经expectedGeneration+expectedSnapshot交原Owner切换；失败按真实currentPreserved反馈，不从IDLE推断成功。媒体原长度/SHA/字节保留合同、金额/订阅规则及backup4不变（源码/协议核对，不是本轮真实账本实测）。真实SAF、进程中断、磁盘故障、Owner切换/回开、恢复后真实读取一致及升级后的完整恢复，当前均无新Android证据；JVM容器/Controller不能替代|
|M22完整旅程/长历史|原137 JVM及既有36 Android PASS保留；T06历史三次FAIL（漏填原因、导航滚动定位、详情金额节点缺失）不抹除，最后等待修正未获真实重跑证明。后续真机到零默认路由门禁仍未启动T06；dummy0路由存在不直接证明联网或产品Bug。正式T03的1000项目/1000订阅/20000事件/10000续费/20年seed与120秒预算未实跑，本次未缩规模或改预算；真实IME/200%字体仍NOT_RUN。新状态转换回归不能替代完整旅程、正式规模、内存/响应或真实离线|
|照片及其他历史|C6-2D RUNTIME ENVIRONMENT_BLOCKED保留，8项Android及C6-1原14项仍NOT_RUN，C6隔离/native资源未知。M10三JPEG FAIL、M20/M22/C3、M13-T06、Q03/Q04与G1～G4等全部历史FAIL/UNKNOWN/NOT_RUN/NOTES保持；不接JPEG native/Picker/封面写入、替换、移除或GC|

来源：[M20原Android](M20-Android-verification.md)、[M20收口](M20-Platform-closure.md)、[M22原记录](M22-verification.md)、[M22最近T06门禁](M22-T06-Physical-Verification.md)、[C6环境阻塞](LITHIC-C6-2D-Runtime-Verification.md)。这些是历史原始状态，不是本轮重跑结果。

## 综合验收建议与停止点

仅具备本轮**桌面稳定性独立审查**条件，尚不具备宣布V1.0综合验收/正式发行通过的证据。后续仍须安全且明确授权的Android环境完成相应真实旅程、正式规模、升级/恢复、通知与关键交互；本轮不重启照片路线、不自动安排设备或下一阶段。Preview12/code28及原签名/三主题/用户交付APK保持，无新体验包。

最终状态：**V1 STABILITY CLOSURE — READY_FOR_INDEPENDENT_REVIEW（桌面修复，含C6产物缺失NOTE）**；**ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED**。提交推送当前分支后停止等待ChatGPT独立审查，不合并main，不进入M23。
