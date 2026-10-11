# 当前项目状态

状态日期：2026-10-11。本文件是唯一实时状态；归档同名文件与 checkpoints 是历史记录。

## Current Stage

- M02已由用户转交ChatGPT独立代码验收：**ACCEPTED / FROZEN — PASS WITH NOTES**。评审仓库 `cc7279694-debug/property`，分支 `feat/m02-project-cost-engine`，评审HEAD `ff1a898eaf4da5068c36af3e1c0bdcbe61c0b1f7`；依据与NOTES见[M02独立验收登记](ACCEPTANCE_MATRIX.md#m02-独立验收记录2026-10-11)。本次仅授权验收状态回写、提交与推送，不重新运行Gradle或开发后续模块。
- M00：ACCEPTED / FROZEN — PASS WITH NOTES，评审HEAD `59ba290cba69e1e77af7d0376a8b43aaabbf5f88`；M01：同一独立结论，评审HEAD `ca8499e3d3cb2e5bac3ab29a231f399636899c19`，验收回写基线 `a25a06899bd157988c3a4466d7162d8616fb0249`。来源与历史NOTES见[当前验收登记](ACCEPTANCE_MATRIX.md)，不改写当时交付证据。
- M02实施授权与Q02/Q07正式确认保留于[M02契约](planning/M02_CONTRACT.md)及当前决策；本次只回写独立验收。M03～M23 未授权且 NOT_STARTED。禁止 main 合并、WHOCARE 修改与公开发布。

## Verified / Completed

- M01单 :app、单Activity、Compose/Navigation3空壳完整继承；本轮未修改Activity、导航、Manifest、界面资源或备份排除规则，没有业务页面或虚构用户数据。
- M02实际实现 Money/MoneyParser、BusinessClock、LongTermProject/ProjectMoneyEvent/ProjectSnapshot/ProjectCostResult、ProjectCostEngine。金额以Long分表示；解析非负绝对值、最多两位小数、上限999,999,999,999分，方向独立；聚合显式检查溢出，日均采用BigDecimal DECIMAL128元/天，负数与极小非零值保留。
- Q02领域规则已实现：事件按观察日过滤、首末日计时、结束前历史按观察日、结束日起分母固定且日末退出、后结算不重新活跃。开始前为0天、null日均和明确原因，真实已发生预付款仍保留在金额总额。
- 真实 RED：标准测试命令执行31项，1通过/30断言失败；GREEN：31通过、0失败、0错误、0跳过。六原案例及Q02-V01～V03领域补测全部PASS。初次classloading环境错误没有计入RED。详情见[M02证据](evidence/M02-verification.md)和[可核对JSON](evidence/M02-verification.json)。
- `:app:lintDebug :app:assembleDebug`实际退出0，0 errors / 12既有版本提示；固定Gradle8.14.3、JDK17.0.20.1+1、AGP8.13.2、Kotlin2.3.10、BOM2025.11.01、Nav3 1.0.1，SDK26/36/36不变。仅新增测试JUnit4.13.2（传递Hamcrest1.3），182条唯一运行依赖坐标线与M01一致；见[工具链锁](TOOLCHAIN_LOCK.md)。
- 中文路径测试worker编码问题已通过最小项目设置修复：移除daemon强制UTF-8，使用现有JDK17原生默认参数文件编码；没有硬编码GBK、升级依赖或改系统编码。
- 本轮最终本地Debug APK：`E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`，10,448,211字节，SHA-256 `ac72927087d7e8116560f8458ed8660980687b0a3d5c689be1cbb9c1b5f3bf6e`。身份仍com.cooper.lithic.debug、code1/0.1.0-m01-debug，仅构建回归产物；实际APK权限、备份资源、Wrapper和领域类检查通过，JUnit/Hamcrest/测试类未打包。不上传APK、不声称重新安装验证。
- 原ZIP/43文件CRC与逐字节/manifest、阅读版SHA核对通过；130原案例仍NOT_RUN。原16组完整跨模块补测仍NOT_RUN，Q02领域部分结果只记当前验收表；Q03～Q06四行状态/期限与起始基线完全一致。M00/M01 checkpoint与既有证据保持不变。
- 独立审查已检查金额解析、日期计算、项目成本引擎、31项JUnit测试源码、RED/GREEN证据、核心文件哈希及Git范围，未发现阻塞性代码问题。独立审查未在用户Windows本机重新运行Gradle测试；本次也不重跑或新增测试PASS。M02历史checkpoint和证据中的READY_FOR_REVIEW/PENDING保留为交付时状态，当前正式结论以本文件及验收登记为准。

## Pending / Current Problems

- M02独立代码验收已通过并冻结，结论来源为用户本次明确转交；保留NOTES：独立审查未在用户Windows本机重跑Gradle。既有31项通过、构建/Lint与本地辅助代码审查记录保留，本次不将源码审查记为重新执行测试。
- 当前引擎未接UI或持久化；稳定ID/重复写入幂等属M04/M06，时区持久化留待对应模块。展示两位HALF_UP与极小非零文案尚未实施，不把领域精度测试当展示PASS。
- Q01 CLOSED；Q02/Q07 USER_CONFIRMED。Q02领域已验证，M17/M18历史视图/总成本集成仍NOT_RUN；Q07不代表所有模块验证已完成。Q03剩余长期100/1000日节点M21前；Q04提醒边界M20前；Q05图片往返M10/M12前；Q06持久回退M04设计、M12实施/故障验证。见[SPEC_ISSUES](SPEC_ISSUES.md)。
- 12条Lint版本提示保留，未抑制或升级。M01真机、API26、OEM行为未测NOTES保留；本轮未重新安装、断网冷启动或执行Instrumentation。数据库/迁移/业务备份往返：NOT_RUN / N/A（未实现且不属于M02）；TypeScript/ESLint不适用于原生Kotlin工程。

## Data / Architecture

业务SQLite/Room、schema/Migration、DAO/Repository、媒体、DataStore、通知及手动Backup均未实现；无业务schemaVersion/backupFormatVersion变化。只有M01空壳和独立纯领域计算；见[真实架构](ARCHITECTURE.md)。没有新增权限、网络能力或运行依赖。

## Git

- 目录：E:/CODEX/砺石新APP；origin：https://github.com/cc7279694-debug/property.git。
- 当前分支：feat/m02-project-cost-engine；起始HEAD `a25a06899bd157988c3a4466d7162d8616fb0249`，来源feat/m01-android-skeleton。开始时干净，本地/远端M01一致，M02分支事先不存在，没有覆盖其他工作。
- 既有代码提交 `18e1d41a35e040ac5a130feaa45900e1c3227df8` 与交付记录保留；27项构建/测试输入的既有blob哈希核对见[M02 checkpoint](checkpoints/2026-10-11-M02.md)。本次评审及回写前基准为 `ff1a898eaf4da5068c36af3e1c0bdcbe61c0b1f7`，本地和origin/feat/m02-project-cost-engine实际一致、开始时工作区干净。本次仅修改本文件与ACCEPTANCE_MATRIX；验收回写提交自身SHA、推送和最终远端HEAD以Git日志/最终报告为准。
- 实际远端main仍 `145326ddd4ce28abbf7c2706fe25f9d38c599c3a`，来源M01仍 `a25a06899bd157988c3a4466d7162d8616fb0249`；没有main合并、force push、PR/Release或签名私钥上传。

## Next Recommended Task

M02验收状态回写后立即停止。M03的M02独立验收前置已满足，但M03尚未授权；只有用户另行授权后才能恢复记忆并读取原M03计划/规格开始实施。Q03不再整体阻塞M03，订阅生效首日口径有M03-T02原案例依据，但M03测试仍未执行；长期日节点、提醒/图片/回退规则继续按各自原期限处理，本次不实施或关闭。
