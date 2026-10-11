# 当前项目状态

状态日期：2026-10-11。本文件是唯一实时状态；归档同名文件和 checkpoints 是历史。

## Current Stage

- 当前 M03：**READY_FOR_REVIEW — M03 (RECHECK)**，独立复验 PENDING。用户转交 ChatGPT 对评审HEAD `80cf6473c21d5933963dd55b02dfbf9f97297849` 的结论 **CHANGES REQUESTED**，仅要求补累计金额/标准天数溢出测试，暂不冻结。本轮从该本地/远端一致且干净的基准继续现有 `feat/m03-subscription-engine`，已完成实际补测/回归；范围见[M03 契约补充](planning/M03_CONTRACT.md#m03-独立验收补测授权2026-10-11)，证据见[补测记录](evidence/M03-recheck-verification.md)。未自行授予验收，M04～M23 未授权、NOT_STARTED。
- M00/M01/M02 均 **ACCEPTED / FROZEN — PASS WITH NOTES**。评审 HEAD 分别为 `59ba290cba69e1e77af7d0376a8b43aaabbf5f88`、`ca8499e3d3cb2e5bac3ab29a231f399636899c19`、`ff1a898eaf4da5068c36af3e1c0bdcbe61c0b1f7`。M02 回写基线为 `0169c76bbf0946a0400b96d00124b3de9aea1cc0`；独立验收来源和历史 NOTES 保留在[验收登记](ACCEPTANCE_MATRIX.md)。M02 独立审查未在用户 Windows 本机重跑 Gradle，本轮回归执行与该历史审查明确区分。

## Verified / Completed

- M01 单 :app、单 Activity、Compose/Navigation 3 空壳和 M02 Money/BusinessClock/项目成本引擎原输入继承；27 项原构建/测试输入哈希一致，没有修改旧生产代码、页面、Manifest、权限、资源或 Gradle 配置。
- M03 SubscriptionModels、SubscriptionIntervalRules、SubscriptionCostEngine 和原37项固定日期测试继承；本轮仅提取 internal SubscriptionPaidTotals / accumulateConfirmedPayment，两次Math.addExact保留，evaluate实际调用同一函数。新增6项累计边界/连接测试，合计43项订阅测试。Renewal金额上限、标准周期、日期/覆盖/状态校验均未放宽。
- 写入用 BusinessClock 验证付款不在业务未来；evaluate 按 paidOn <= asOf 过滤，不读取今天或将后来支付的真实记录判为非法。提前续费计入已付金额/标准时长，下一期价格到生效日才贡献。
- ACTIVE/PAUSED/EXPIRED/STOPPED 按当日覆盖与状态历史直接推导。非活跃 currentRate=0、currentPrice=null；无付款 historicalAverage=null 并附 NO_CONFIRMED_PAYMENTS。恢复可复用已有付款，默认停止 helper 使用所选付费期的排他结束边界，明确提前停止按指定日期截断。
- elapsedPaidDays 为已知真实覆盖与 ENABLED 历史区间的交集并集，含观察日；未来未经过日期不计，暂停/停止日排除、恢复日计入。Long epochDay 排序/双指针避免逐日枚举；LocalDate.MAX 和跨 Int 上限日期跨度实际测试通过。钱用 Long 分，计算 BigDecimal DECIMAL128 元/天，不做展示舍入。
- 初次M03的RED37项/34断言失败、GREEN37及68回归证据保留为历史。本轮按现有正确Math.addExact行为的边界补测/回归流程执行，没有制造RED；完整 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` 退出0，74通过（M02 31＋原M03 37＋新增6）、0失败/错误/跳过。新测试验证两个Long精确边界/超限ArithmeticException、不可变种子/无部分更新、5倍数近界及两笔真实付款与生产连接；编译字节码确认evaluate调用累计函数。当前结果见[补测JSON](evidence/M03-recheck-verification.json)与[验收登记](ACCEPTANCE_MATRIX.md)，通知集成边界不变。
- Lint 0 errors / 12既有版本提示，未升级或抑制。Gradle8.14.3、Temurin17.0.20.1+1、AGP8.13.2、工程Kotlin2.3.10、ComposeBOM2025.11.01、Nav3 1.0.1、SDK26/36/36、BuildTools36.0.0不变，无新依赖；182条唯一运行依赖坐标线为初次M03与M02比对结果，本轮未重跑依赖报告，构建配置哈希保持一致。JUnit4.13.2/Hamcrest1.3仍仅测试。
- 最终本地 APK：`E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`，10,464,344字节，SHA-256 `04c323124abec438c741f3099d22cd4ee96f553e68e443199fd45df8a78f6ddf`。仍 com.cooper.lithic.debug、code1/0.1.0-m01-debug，仅构建回归产物。实际APK/合并Manifest权限、编译备份排除资源、Wrapper、18个关键领域类描述符检查通过，测试类/框架未打包，apksigner v2验证通过；没有重新安装或公开上传。
- 本轮重新核对原ZIP/43原件SHA与大小，130原案例全NOT_RUN；CRC/安全路径核查继承原归档证据，本轮不重复声明执行。阅读版、原16组完整跨模块补测、Q03～Q06四行及期限与基线不变。M00～M02历史 checkpoint/证据/契约及来源登记不改，初次M03证据不改，M03 checkpoint/契约保留原文并追加本轮记录。

## Pending / Current Problems

- M03等待 ChatGPT 独立复验；最近独立结论仍登记为原HEAD的CHANGES REQUESTED。累计边界缺口已有实际补测证据，Codex本地只读辅助审查不代替独立复验，不授予 ACCEPTED / FROZEN。
- Q01 CLOSED；Q02/Q07 USER_CONFIRMED。Q02 M17/M18集成仍NOT_RUN；Q03订阅首日/暂停/闰日计算已有本轮领域证据，长期100/1000日节点及 M21触发仍待原期限。Q04成本到期区间已测，提醒资格仍 M20前；Q05图片往返 M10/M12前；Q06持久回退 M04设计/M12实施及故障验证。见[SPEC_ISSUES](SPEC_ISSUES.md)。没有自行关闭这些问题。
- 两项累计边界已按生产同一路径测试；种子为内部合成累计值，增量为合法Renewal，不宣称数百万续费端到端验证。标准天数受List容量及每笔最大365约束，完整历史根本无法到Long极限；合计均为5倍数，精确Long.MAX也不可能由合法完整历史形成。STOPPED再启用、多期预付默认停止选择仍为M09待明确/验证风险，见[问题清单](SPEC_ISSUES.md#m09-订阅流程风险本次独立审查登记当前不实施)。未来Repository的一致快照/稳定ID/幂等边界不变。
- 设备安装/断网冷启动/Instrumentation：NOT_RUN / N/A（纯领域变更无 Android交互目标）。真实手机、API26、OEM备份仍保留 M01未测 NOTES；订阅持久化/UI、展示舍入、里程碑/提醒、业务迁移/完整备份未实现。TypeScript/ESLint不适用于原生工程，NO-SOURCE不计测试通过。

## Data / Architecture

业务 SQLite/Room、schema/Migration、DAO/Repository、DataStore、媒体、通知/WorkManager和手动 Backup仍未实现，无业务schemaVersion/backupFormatVersion变化。订阅身份/付款/状态都是纯内存领域输入，尚未接入空壳界面；不保存每日派生快照。没有权限、网络能力或运行依赖变化，见[真实架构](ARCHITECTURE.md)。

## Git

- 目录 E:/CODEX/砺石新APP；origin `https://github.com/cc7279694-debug/property.git`。
- 当前功能分支 `feat/m03-subscription-engine`；原模块起点 `0169c76bbf0946a0400b96d00124b3de9aea1cc0`、来源M02。本次补测基准 `80cf6473c21d5933963dd55b02dfbf9f97297849`，本地/远端相同、工作区干净；未新建分支或覆盖其他工作。
- 原实现 `270c7a079d7f1220524e4e29bdf08e8f41d90964` 与交付 `80cf647...` 保留。本轮补测已完成32项构建/测试输入哈希（30原输入不变、1引擎调整、1新增测试）；提交/推送与提交blob核对随后记录在[M03 checkpoint补充](checkpoints/2026-10-11-M03.md#m03-独立验收补测-checkpoint2026-10-11)和Git日志/最终报告。只推当前功能分支。
- main保护基准 `145326ddd4ce28abbf7c2706fe25f9d38c599c3a`，M02来源保护基准同起点；实现推送后实际ls-remote核对两者不变。不合并 main、不force push、不修改WHOCARE、不建PR/Release、不公开上传APK或私钥。

## Next Recommended Task

补测交付后立即停止，等待独立复验。M04必须先满足M03独立验收和用户另行授权，再恢复上下文、读取原M04计划并处理Q06持久回退设计边界；本轮不进入M04。M09两项审查风险仅登记，不能借补测改变当前订阅语义。
