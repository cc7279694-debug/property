# M03 独立验收补测证据

2026-10-11；当前分支 `feat/m03-subscription-engine`，补测基准及独立评审HEAD `80cf6473c21d5933963dd55b02dfbf9f97297849`。用户转交ChatGPT结论 **CHANGES REQUESTED — 仅补累计溢出，暂不冻结**；六原案例、原37订阅＋31回归证据一致，核心逻辑暂未发现确定性缺陷。本轮完成后的状态是 **READY_FOR_REVIEW — M03 (RECHECK)**，独立复验PENDING，不自行冻结。

## 实现与验证路径

原引擎已有两次Math.addExact。本轮仅在同一文件提取 internal SubscriptionPaidTotals 和 accumulateConfirmedPayment(totals, renewal)，evaluate的paid循环实际调用该函数，再取两个累计字段；日期过滤、覆盖/状态/停止、成本公式不改。函数输入是不可变Long累计值＋合法Renewal，输出新的不可变累计值或ArithmeticException。发生第二字段溢出时也不返回部分更新。

本轮按**边界补测＋回归**执行，已有正确检查保留，没有故意修改为回绕实现或伪造RED。原M03 RED/GREEN历史保留在[旧证据](M03-verification.md)，原JSON、37测试源码与31项M02测试源码不改。[本次JSON](M03-recheck-verification.json)记录74项逐项结果、32项最终输入哈希、XML/日志/APK/编译字节码核对；源配置统一CRLF为LF，二进制/日志/报告按原字节。

## 新增六项实际通过

| 当前补测ID | 测试名 | 输入与实际断言 |
|---|---|---|
| M03-R01 | confirmed_paid_cents_reaches_long_max_exactly | 种子MAX−999999999999分＋合法最大单笔到精确MAX；天数30，原种子不变 |
| M03-R02 | confirmed_paid_cents_overflow_fails_without_wrapping | 上述金额种子多1分＋同笔付款，两次执行均抛ArithmeticException；天数种子0隔离该字段，种子不变 |
| M03-R03 | standard_purchased_days_reaches_long_max_at_arithmetic_boundary | 合成天数种子MAX−365＋合法年付365，精确MAX；金额仅1分，种子不变 |
| M03-R04 | standard_purchased_days_overflow_fails_without_wrapping | 天数种子MAX−365＋1再加年付365，两次执行均抛ArithmeticException；金额种子0＋1分不会先溢出、无部分更新 |
| M03-R05 | largest_cycle_multiple_below_long_max_is_safe_then_next_cycle_fails | 5倍数合成种子MAX−367＋365到MAX−2合法算术边界，再加月付30抛错，前一结果不变 |
| M03-R06 | two_real_payments_use_same_accumulation_in_engine | 月付150＋已付款下一期季付270，累计420元/120标准天，与真实evaluate结果相同 |

这些是生产同一路径的累计运算边界测试，增量Renewal沿用真实模型校验；Long累计种子为合成测试值，**不是海量合法完整历史的端到端证明**。金额至少需9,223,373笔最大单笔才溢出；现有List最大Int.MAX笔×365=783,831,531,155标准天，根本到不了Long极限，且固定周期天数均为5倍数，精确Long.MAX也无法由合法历史形成。没有构造非法Money或修改周期限制来触发异常。

编译后的javap证据显示evaluate调用 `accumulateConfirmedPayment$app_debug`，函数内确有两次Math.addExact；最终APK新增累计类型描述符存在。测试对象不是独立Math.addExact示例，原37项领域测试亦验证接入后业务结果。

## 实际执行与产物

| 验证 | 实际结果 | 本机证据路径（忽略、不上传原产物） |
|---|---|---|
| 新增累计类初次过滤执行 | 退出0，6项通过，BUILD SUCCESSFUL in 23s；自然覆盖fixture之后调整为月/季/年完整日期，以下最终回归核对最终源码 | `build/m03-recheck-evidence/accumulation-tests.log`、`initial-targeted-test-results/` |
| `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain` | 退出0，BUILD SUCCESSFUL in 22s；74通过/0失败/0错误/0跳过，53任务/12执行/41up-to-date | `build/m03-recheck-evidence/regression-build-lint.log`、`final-test-results/`、Gradle HTML报告 |
| Lint XML | 0 errors / 12既有版本提示，不升级或抑制 | `app/build/reports/lint-results-debug.xml` |
| 编译调用链核对 | evaluate调用内部累计函数、函数含两次Math.addExact | `build/m03-recheck-evidence/engine-bytecode.txt` |
| 最终APK检查/签名 | 退出0；身份/SDK/权限/编译备份资源/Wrapper不变，18个关键领域类描述符存在，测试框架/类未打包；v2签名通过 | `build/m03-recheck-evidence/apk/`与本次JSON |

实际分组：金额7＋时钟2＋项目22＋原订阅37＋累计6=74。旧68个方法名称及结果均保留，4份旧测试文件与评审基线按LF核对逐字一致。32项输入中30项原哈希不变，仅引擎调整和新增测试；工具链、权限、依赖、UI/数据库配置未变化。

最终APK绝对路径 `E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`，10,464,344字节，SHA-256 `04c323124abec438c741f3099d22cd4ee96f553e68e443199fd45df8a78f6ddf`。仍com.cooper.lithic.debug、code1/0.1.0-m01-debug，M01空壳；只作本地构建回归，不公开上传或发布。

## 继承边界与停止点

原ZIP/43原件哈希一致、130原案例全NOT_RUN，原M03交付JSON/Markdown不改写，旧checkpoint保留并追加本次补测记录。Q03～Q06状态/期限及16完整补测仍不变；本次累计测试的PASS只登记当前结果。

实际文档/范围核查：3新增/7修改/0删除，生产仅1文件的最小累计提取；68份Markdown、288个本地文件链接、7个本次章节锚点均通过，围栏平衡，10份文本敏感模式0命中，git diff --check通过。80受保护文件与评审基线一致；原M03验收登记、checkpoint、契约保留原全文后追加，Q03～Q06四行不改。当前结果也记入本次JSON的documentAndScopeChecks。

STOPPED后重新启用、多期已预付默认停止边界仅记为[M09待处理风险](../SPEC_ISSUES.md#m09-订阅流程风险本次独立审查登记当前不实施)，本轮不改变语义。设备/真机/API26/OEM/Instrumentation、业务数据库/迁移/备份、M09流程和M20/M21通知仍NOT_RUN / N/A。无新增依赖、签名配置、真实个人数据或上传APK。

最终文档/范围/敏感信息与Git验证见[M03 checkpoint补充](../checkpoints/2026-10-11-M03.md#m03-独立验收补测-checkpoint2026-10-11)。只提交并推送现有M03分支；停止等待ChatGPT独立复验，不进入M04或合并main。
