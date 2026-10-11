# 当前项目状态

状态日期：2026-10-11。本文件是唯一实时状态；归档同名文件与 checkpoints 是历史记录。

## Current Stage

- **M02 — READY_FOR_REVIEW；独立验收 PENDING，尚未 ACCEPTED / FROZEN。**本轮授权、起点和精确范围见[M02契约](planning/M02_CONTRACT.md)。纯 Kotlin 领域实现及实际测试/构建已完成，Git交付结果见本节与[M02 checkpoint](checkpoints/2026-10-11-M02.md)，最终停止等待独立验收。
- M00：ACCEPTED / FROZEN — PASS WITH NOTES，评审HEAD `59ba290cba69e1e77af7d0376a8b43aaabbf5f88`；M01：同一独立结论，评审HEAD `ca8499e3d3cb2e5bac3ab29a231f399636899c19`，验收回写基线 `a25a06899bd157988c3a4466d7162d8616fb0249`。来源与历史NOTES见[当前验收登记](ACCEPTANCE_MATRIX.md)，不改写当时交付证据。
- 用户本轮明确授权 M02，并正式确认 Q02/Q07；M03～M23 未授权且 NOT_STARTED。禁止 main 合并、WHOCARE 修改与公开发布。

## Verified / Completed

- M01单 :app、单Activity、Compose/Navigation3空壳完整继承；本轮未修改Activity、导航、Manifest、界面资源或备份排除规则，没有业务页面或虚构用户数据。
- M02实际实现 Money/MoneyParser、BusinessClock、LongTermProject/ProjectMoneyEvent/ProjectSnapshot/ProjectCostResult、ProjectCostEngine。金额以Long分表示；解析非负绝对值、最多两位小数、上限999,999,999,999分，方向独立；聚合显式检查溢出，日均采用BigDecimal DECIMAL128元/天，负数与极小非零值保留。
- Q02领域规则已实现：事件按观察日过滤、首末日计时、结束前历史按观察日、结束日起分母固定且日末退出、后结算不重新活跃。开始前为0天、null日均和明确原因，真实已发生预付款仍保留在金额总额。
- 真实 RED：标准测试命令执行31项，1通过/30断言失败；GREEN：31通过、0失败、0错误、0跳过。六原案例及Q02-V01～V03领域补测全部PASS。初次classloading环境错误没有计入RED。详情见[M02证据](evidence/M02-verification.md)和[可核对JSON](evidence/M02-verification.json)。
- `:app:lintDebug :app:assembleDebug`实际退出0，0 errors / 12既有版本提示；固定Gradle8.14.3、JDK17.0.20.1+1、AGP8.13.2、Kotlin2.3.10、BOM2025.11.01、Nav3 1.0.1，SDK26/36/36不变。仅新增测试JUnit4.13.2（传递Hamcrest1.3），182条唯一运行依赖坐标线与M01一致；见[工具链锁](TOOLCHAIN_LOCK.md)。
- 中文路径测试worker编码问题已通过最小项目设置修复：移除daemon强制UTF-8，使用现有JDK17原生默认参数文件编码；没有硬编码GBK、升级依赖或改系统编码。
- 本轮最终本地Debug APK：`E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`，10,448,211字节，SHA-256 `ac72927087d7e8116560f8458ed8660980687b0a3d5c689be1cbb9c1b5f3bf6e`。身份仍com.cooper.lithic.debug、code1/0.1.0-m01-debug，仅构建回归产物；实际APK权限、备份资源、Wrapper和领域类检查通过，JUnit/Hamcrest/测试类未打包。不上传APK、不声称重新安装验证。
- 原ZIP/43文件CRC与逐字节/manifest、阅读版SHA核对通过；130原案例仍NOT_RUN。原16组完整跨模块补测仍NOT_RUN，Q02领域部分结果只记当前验收表；Q03～Q06四行状态/期限与起始基线完全一致。M00/M01 checkpoint与既有证据保持不变。

## Pending / Current Problems

- M02等待ChatGPT独立验收，执行者无权自行冻结。没有已知未解决测试失败或M02规格阻塞；本地辅助代码审查仅是实现核查，不替代独立验收。
- 当前引擎未接UI或持久化；稳定ID/重复写入幂等属M04/M06，时区持久化留待对应模块。展示两位HALF_UP与极小非零文案尚未实施，不把领域精度测试当展示PASS。
- Q01 CLOSED；Q02/Q07 USER_CONFIRMED。Q02领域已验证，M17/M18历史视图/总成本集成仍NOT_RUN；Q07不代表所有模块验证已完成。Q03剩余长期100/1000日节点M21前；Q04提醒边界M20前；Q05图片往返M10/M12前；Q06持久回退M04设计、M12实施/故障验证。见[SPEC_ISSUES](SPEC_ISSUES.md)。
- 12条Lint版本提示保留，未抑制或升级。M01真机、API26、OEM行为未测NOTES保留；本轮未重新安装、断网冷启动或执行Instrumentation。数据库/迁移/业务备份往返：NOT_RUN / N/A（未实现且不属于M02）；TypeScript/ESLint不适用于原生Kotlin工程。

## Data / Architecture

业务SQLite/Room、schema/Migration、DAO/Repository、媒体、DataStore、通知及手动Backup均未实现；无业务schemaVersion/backupFormatVersion变化。只有M01空壳和独立纯领域计算；见[真实架构](ARCHITECTURE.md)。没有新增权限、网络能力或运行依赖。

## Git

- 目录：E:/CODEX/砺石新APP；origin：https://github.com/cc7279694-debug/property.git。
- 当前分支：feat/m02-project-cost-engine；起始HEAD `a25a06899bd157988c3a4466d7162d8616fb0249`，来源feat/m01-android-skeleton。开始时干净，本地/远端M01一致，M02分支事先不存在，没有覆盖其他工作。
- 本轮工程与证据尚待提交/推送；最终代码提交SHA、远端实际一致性及差异检查结果在Git交付后登记[M02 checkpoint](checkpoints/2026-10-11-M02.md)。构建/测试发生于上述基线加本轮输入，不能把构建前基线误当实现提交；提交后按JSON输入哈希核对。
- main保护基线 `145326ddd4ce28abbf7c2706fe25f9d38c599c3a`；不合并main，不force push，不建PR/Release或上传签名私钥。

## Next Recommended Task

完成M02当前功能分支Git交付后立即停止，等待独立验收。M03需M02独立验收通过与用户另行授权，开始前恢复当前记忆、读取原M03计划/规格与当前澄清。Q03不再整体阻塞M03，订阅生效首日口径有M03-T02原案例依据，但M03测试仍未执行；长期日节点、提醒/图片/回退规则继续按各自原期限处理，不在本轮提前实施。
