# 当前项目状态

状态日期：2026-10-12。唯一实时状态；归档同名文件与 checkpoints 是历史。

## Current Stage

- **M06 — READY_FOR_REVIEW**。ChatGPT独立代码验收 **PENDING**；本地辅助审查不授予ACCEPTED / FROZEN。用户授权本轮长期项目录入与投入闭环；起点 `4576b03565e58011e2fa78a086039fee33e3352f`，来源feat/m05-ui-foundation，当前feat/m06-project-entry。编码前已建立[M06契约](planning/M06_CONTRACT.md)。本轮交付后停止，M07～M23未授权。
- M00～M05均 **ACCEPTED / FROZEN — PASS WITH NOTES**，来源为用户转交的ChatGPT独立验收。评审HEAD依次为 `59ba290cba69e1e77af7d0376a8b43aaabbf5f88`、`ca8499e3d3cb2e5bac3ab29a231f399636899c19`、`ff1a898eaf4da5068c36af3e1c0bdcbe61c0b1f7`、`8b13d20dad89f9c3e76a3d19161cf2232b36b94d`、`1b7058ebb9dfbe904252a08efe6815d9952971ad`、`6909858cd0c03ec8e3277908e05d407d7f4cafca`。M05验收回写已提交推送4576b03565e58011e2fa78a086039fee33e3352f。正式依据、NOTES与全部历史见[验收登记](ACCEPTANCE_MATRIX.md)；不改旧报告/checkpoint的当时PENDING或NOT_RUN。

## Verified / Completed

- 正式新增项目与唯一INITIAL事件，初始0元合法、真实付款日可早于拥有日；详情、资料编辑、普通投入/收回通过真实Room与冻结M02引擎。编辑只改名称/拥有日/PROJECT类别/备注及updatedAt，保留UUID、createdAt、结束/封面与旧事件；陈旧完整基线CAS拒绝无提示覆盖。
- 相同创建/金额操作继续M04原子事务与operationId唯一约束。当前任务SavedStateHandle保存raw输入、完整基线、稳定命令/项目/事件UUID和提交阶段。写成功但结果中断只能同命令核对/重放；写成功读失败明确已保存，只重读。编辑没有持久操作日志，不声称永久操作查重。
- 正常启动只读COUNT与持久记录时区，Room变化重新查数量、当前列表/详情；后续读错明确保留旧状态。数据库startedOn/id游标，每页最多51查询/50展示，事件/类别批读；整个已加载窗口一次协调边界和Room事务。真实51/100/1000记录、同日排序与并发排序变化验证。解决M05真实刷新与正式列表分页两项交接要求；旧历史NOTES保留。
- Nav3路线只有UUID身份、最多四级；顶部/系统/手势/Tab含重选统一弃改/忙碌守卫。缺失身份可返回，资料首次读取恢复与详情请求身份/版本检查防止旧结果污染。日期raw与UTC日历编码，业务今天仅来自固定recordZone/BusinessClock。
- 完整Gradle四任务退出0，**82 JVM＋202 Android＝284个唯一方法PASS，0失败/错误/跳过**；旧200方法全部保留并实跑、新84方法，源码/XML/JSON精确一致。首次完整5m41s及不改输入复跑4m25s之后，仅整理ReviewShell末尾空行，再对最终输入完整复跑9m53s；所有284方法通过。真实RED与所有后续失败保存；编译/runner错误不计行为RED。Lint0 errors/20 warnings保持，不升级或抑制。详见[报告](evidence/M06-verification.md)、[JSON](evidence/M06-verification.json)及[checkpoint](checkpoints/2026-10-12-M06.md)。
- 仅专用API36 emulator-5596：首次APK（d75549af…）安装、断网冷启动，零元/1000、历史投入200、收回1400、改名和拥有日、详情/列表/首页COUNT刷新、force-stop重开实际通过。持久2个合成项目/4事件/2INITIAL/4operationId，integrity_check=ok、FK无错误。首次APK的360dp/200%字体、真实IME/日期/错误/弃改、横竖及深色截图已查看。最终APK（81b4c698…）另实际重装/断网COLD、首页COUNT和列表、持久零元/负净额详情、1.234校验原文/弃改取消及明确放弃；复验未重新录入全套历史事件或重跑手工200%/进程死亡。
- 进程恢复首轮未等待STOPPED未恢复草稿，不计PASS。复验先确认Task659/Record83054542 STOPPED、mHaveState=true/Bundle4444，UID SIGKILL后PID12436消失，从最近任务恢复同Task/Record、新PID12799，raw名称和1.234保持。此为系统保存任务后的真实进程死亡模拟，不是自动LMK或前台断电保证。
- 系统时区实际改Pacific/Honolulu、OS日期10月11日，而记录GMT/业务日10月12日保持；generation身份/ready/settings SHA未变。最终APK SHA `81b4c698c1d1355b018094bcc391be9b1ada8e4c4e447b7eb43ace8d7c7a2895`、code4/0.1.0-m06-debug、12,076,349bytes；与已安装hash一致。13张初次产物截图与5张最终产物复验截图明确区分，临时网络/屏幕/字体/IME/明暗/旋转/时区及自动时区设置全部恢复。未操作手机/其他AVD，未删除测试库。

## Pending / Current Problems / Technical Debt

- Q01 CLOSED，Q02/Q07 USER_CONFIRMED。Q03长期100/1000日节点M21前，Q04提醒资格M20前，Q05图片往返M10/M12前；Q06 **ENGINEERING_REQUIRED**，M04仅设计/首次数据集，M12完整导入/切换/验证后回退/中断/共同校验/清理及Q06-V01～V03完整流程仍NOT_RUN。Q02 M17/M18视图集成仍NOT_RUN。原43原件/130案例与16组完整补测不改、保持NOT_RUN。
- [M09两项流程风险](SPEC_ISSUES.md)原样保留：STOPPED重新启用入口/历史/纠错，多期预付默认停止选择；未实施/NOT_RUN，不改订阅语义。
- M04首次初始化中断残留generation不得静默清除真实数据；M08 v1→v2须同步已有数据库版本预检，M12完整恢复仍待执行。schema v1、领域引擎、Settings/owner、备份配置未改。
- 真实SQLite满页失败可能被Android/Room回滚错误掩盖为DATABASE。真实存量文件测试复现且旧行/事件/同命令恢复均通过；另一VM场景实际DISK_FULL。未按消息猜错误或宣称所有磁盘满都能准确分类；错误仍保留原提交身份与输入。详见报告与JSON。
- 单项目完整成本仍需读其全部必要事件，列表按50项目批读而非保证单项目无限历史恒定耗时。1000项目是正确性/查询范围验证，不是手机长期性能或正式基准。任务SavedState不等于持久业务草稿；force-stop/系统未保存状态不保证未提交输入恢复。
- 真机、API26运行、OEM备份/迁移、完整TalkBack、真实内存压力LMK、Release构建/正式签名升级仍 **NOT_RUN**。Debug另含exported非launcher空ComponentActivity测试宿主；未验证Release不能正式交付。20条既有Lint提示保留。
- M01独立未重新安装；M02/M03独立未重跑本机Gradle；M04/M05独立未重跑Gradle/安装等历史NOTES不因本轮执行改写。M07结束/删除/历史纠错、M08订阅/迁移、M10媒体、M11/M12备份恢复、M13汇总、M14搜索/类别管理及后续未运行能力均NOT_RUN。TypeScript/ESLint不适用，NO-SOURCE不计PASS。

## Data / Architecture

schemaVersion=1，backupFormatVersion不存在。pointer/ready内部formatVersion1不是备份版本。四表真实事实与冻结Money/BusinessClock继承；UI → ViewModel → 同一Repository/协调器 → Room，未建新数据owner或派生成本真值。DB/DataStore/文件不宣称跨存储原子。实际结构见[ARCHITECTURE](ARCHITECTURE.md)。

## Git

- 目录 `E:/CODEX/砺石新APP`，origin `https://github.com/cc7279694-debug/property.git`，分支feat/m06-project-entry，基线4576b03565e58011e2fa78a086039fee33e3352f。开工本地/远端一致、工作区干净，新分支此前不存在，未覆盖别的工作。
- main保护145326ddd4ce28abbf7c2706fe25f9d38c599c3a；M02=0169c76bbf0946a0400b96d00124b3de9aea1cc0、M03=ffd066ffe0efb6c00f35526ae792b50829d3230e、M04=aecbe86fce6e5c4375f2c35d2df8539df0a557bb、M05=4576b03565e58011e2fa78a086039fee33e3352f。只提交推送M06，最终自身SHA/远端一致/干净工作区以Git日志与推送后本地交付回执为准，避免文档自引用SHA。没有main合并/force/PR/发布/WHOCARE改动；不提交APK、数据库、签名私钥或真实个人数据。

## Next Recommended Task

**停止，等待ChatGPT对M06独立代码验收。**M07须M06正式验收及新授权，并继承本轮刷新、CAS/幂等、固定时区、schema和全部NOT_RUN安全边界；本轮不执行M07。
