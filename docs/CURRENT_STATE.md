# 当前项目状态

状态日期：2026-10-11。本文件是唯一实时状态；归档同名文件和 checkpoints 是历史。

## Current Stage

- 当前 M03：**READY_FOR_REVIEW**，ChatGPT 独立验收 PENDING；本轮只实现纯 Kotlin 订阅模型、周期/区间验证、状态/成本/实际有效日期计算及自动测试。用户已授权，从 M02 验收回写基线 `0169c76bbf0946a0400b96d00124b3de9aea1cc0` 创建 `feat/m03-subscription-engine`，见[M03 契约](planning/M03_CONTRACT.md)。M04～M23 未授权、NOT_STARTED，不自动进入。
- M00/M01/M02 均 **ACCEPTED / FROZEN — PASS WITH NOTES**。评审 HEAD 分别为 `59ba290cba69e1e77af7d0376a8b43aaabbf5f88`、`ca8499e3d3cb2e5bac3ab29a231f399636899c19`、`ff1a898eaf4da5068c36af3e1c0bdcbe61c0b1f7`。M02 回写基线为上述 `0169c76...`；独立验收来源和历史 NOTES 保留在[验收登记](ACCEPTANCE_MATRIX.md)。M02 独立审查未在用户 Windows 本机重跑 Gradle，本轮回归执行与该历史审查明确区分。

## Verified / Completed

- M01 单 :app、单 Activity、Compose/Navigation 3 空壳和 M02 Money/BusinessClock/项目成本引擎原输入继承；27 项原构建/测试输入哈希一致，没有修改旧生产代码、页面、Manifest、权限、资源或 Gradle 配置。
- M03 新增 SubscriptionModels、SubscriptionIntervalRules、SubscriptionCostEngine 和37项固定日期 JUnit 测试。Renewal 保存正数 Money、cycle、standardDays 和四个日期；仅月/季/年30/90/365。重叠覆盖和同日多状态拒绝，邻接/空档合法，列表重排不改变结果。
- 写入用 BusinessClock 验证付款不在业务未来；evaluate 按 paidOn <= asOf 过滤，不读取今天或将后来支付的真实记录判为非法。提前续费计入已付金额/标准时长，下一期价格到生效日才贡献。
- ACTIVE/PAUSED/EXPIRED/STOPPED 按当日覆盖与状态历史直接推导。非活跃 currentRate=0、currentPrice=null；无付款 historicalAverage=null 并附 NO_CONFIRMED_PAYMENTS。恢复可复用已有付款，默认停止 helper 使用所选付费期的排他结束边界，明确提前停止按指定日期截断。
- elapsedPaidDays 为已知真实覆盖与 ENABLED 历史区间的交集并集，含观察日；未来未经过日期不计，暂停/停止日排除、恢复日计入。Long epochDay 排序/双指针避免逐日枚举；LocalDate.MAX 和跨 Int 上限日期跨度实际测试通过。钱用 Long 分，计算 BigDecimal DECIMAL128 元/天，不做展示舍入。
- 实际 RED：37项，3通过/34 AssertionError失败/0错误/0跳过。相同测试源码 GREEN：37通过；完整 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` 退出0，共68通过（M02 31＋M03 37）、0失败/错误/跳过。六原 M03 案例及 Q03/Q04 的本轮成本/日期部分另记当前验收，不替代通知集成。证据见[M03 记录](evidence/M03-verification.md)及[JSON](evidence/M03-verification.json)。
- Lint 0 errors / 12既有版本提示，未升级或抑制。Gradle8.14.3、Temurin17.0.20.1+1、AGP8.13.2、工程Kotlin2.3.10、ComposeBOM2025.11.01、Nav3 1.0.1、SDK26/36/36、BuildTools36.0.0不变，无新依赖；182条唯一运行依赖坐标线与M02实际一致。JUnit4.13.2/Hamcrest1.3仍仅测试。
- 最终本地 APK：`E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`，10,463,689字节，SHA-256 `924ecf5a0f49239534661516e0c7a3797bb10d052c68b2c42be75139e99d99f5`。仍 com.cooper.lithic.debug、code1/0.1.0-m01-debug，仅构建回归产物。实际APK/合并Manifest权限、编译备份排除资源、Wrapper、17个关键领域类描述符检查通过，测试类/框架未打包，apksigner v2验证通过；没有重新安装或公开上传。
- ZIP/43原件字节、SHA/CRC/路径安全和阅读版核对通过，130原案例全NOT_RUN。原16组完整跨模块补测、Q03～Q06四行及期限与基线不变。历史 checkpoint、证据、契约与来源登记不改。来源内联授权登记于 planning 入口，没有编造附件路径/校验值。

## Pending / Current Problems

- M03等待 ChatGPT 独立验收，Codex本地只读辅助审查未发现阻塞性问题，不授予 ACCEPTED / FROZEN。
- Q01 CLOSED；Q02/Q07 USER_CONFIRMED。Q02 M17/M18集成仍NOT_RUN；Q03订阅首日/暂停/闰日计算已有本轮领域证据，长期100/1000日节点及 M21触发仍待原期限。Q04成本到期区间已测，提醒资格仍 M20前；Q05图片往返 M10/M12前；Q06持久回退 M04设计/M12实施及故障验证。见[SPEC_ISSUES](SPEC_ISSUES.md)。没有自行关闭这些问题。
- 金额合计使用 Math.addExact，但37项 M03测试没有专门执行数百万续费的 Long 金额合计溢出断言；源码检查不记该项行为 PASS。M02既有溢出测试本轮回归通过，范围仍是项目引擎。未来 Repository 须提供一致快照、稳定记录ID/幂等及事务内校验；本轮不扩展存储。
- 设备安装/断网冷启动/Instrumentation：NOT_RUN / N/A（纯领域变更无 Android交互目标）。真实手机、API26、OEM备份仍保留 M01未测 NOTES；订阅持久化/UI、展示舍入、里程碑/提醒、业务迁移/完整备份未实现。TypeScript/ESLint不适用于原生工程，NO-SOURCE不计测试通过。

## Data / Architecture

业务 SQLite/Room、schema/Migration、DAO/Repository、DataStore、媒体、通知/WorkManager和手动 Backup仍未实现，无业务schemaVersion/backupFormatVersion变化。订阅身份/付款/状态都是纯内存领域输入，尚未接入空壳界面；不保存每日派生快照。没有权限、网络能力或运行依赖变化，见[真实架构](ARCHITECTURE.md)。

## Git

- 目录 E:/CODEX/砺石新APP；origin `https://github.com/cc7279694-debug/property.git`。
- 当前功能分支 `feat/m03-subscription-engine`；来源 `feat/m02-project-cost-engine`，起始 HEAD `0169c76bbf0946a0400b96d00124b3de9aea1cc0`。开始时干净，本地/远端来源一致、新分支未存在，没有覆盖其他工作。
- 实现提交 `270c7a079d7f1220524e4e29bdf08e8f41d90964`（feat(m03): add subscription calculation engine），19文件/8新增/11修改/0删除，已仅推当前M03分支；实际远端HEAD一致，提交后干净，31项构建/测试输入按提交blob哈希全部一致。此次仅补记交付证据，不改工程输入、不重复构建；最后文档交付提交自身SHA以Git日志/最终报告为准，详情见[M03 checkpoint](checkpoints/2026-10-11-M03.md)。
- main保护基准 `145326ddd4ce28abbf7c2706fe25f9d38c599c3a`，M02来源保护基准同起点；实现推送后实际ls-remote核对两者不变。不合并 main、不force push、不修改WHOCARE、不建PR/Release、不公开上传APK或私钥。

## Next Recommended Task

完成当前 M03功能分支交付后立即停止，等待独立验收。M04必须先满足 M03独立验收和用户另行授权，再恢复上下文、读取原 M04计划并处理 Q06持久回退设计边界；本轮不进入 M04。
