# M03 实际验证证据

2026-10-11；基线 `0169c76bbf0946a0400b96d00124b3de9aea1cc0`，分支 `feat/m03-subscription-engine`。状态 **READY_FOR_REVIEW**，ChatGPT独立验收 PENDING；本地辅助审查不代替独立验收。M00～M02验收历史不改，本轮重新执行的回归与M02独立未重跑Windows Gradle的NOTES区分。

[核对JSON](M03-verification.json)记录37项RED/GREEN与68项回归的逐项结果、XML/日志哈希、RED编译轮廓、31项最终构建/测试输入哈希、APK/依赖/原件核查。源配置统一CRLF为LF，JAR/APK/XML/日志按原字节；RED到GREEN测试文件原字节SHA-256均 `ebbb8d344f2c21361bcdb0ff7704e963ca8c14d9ba68e8033e8d103fad201e63`。

## 实际执行

仅进程内复用已有Temurin17.0.20.1+1与SDK36，没有安装、升级或全局设置变化。沿用M02中文路径编码修复，不重新引入强制UTF-8。固定Gradle8.14.3/AGP8.13.2/工程Kotlin2.3.10；Gradle自身嵌入Kotlin2.0.21与工程插件不同，不混写版本。

| 验证 | 实际结果 | 本机证据（忽略、不推送原产物） |
|---|---|---|
| `:app:testDebugUnitTest --tests "com.cooper.lithic.domain.subscription.SubscriptionCostEngineTest"` RED | 退出1，BUILD FAILED in 52s；37项/3通过/34 AssertionError失败/0错误/0跳过 | `build/m03-evidence/red.log`、`red-test-results/`、`red-test-report/`、`red-source/` |
| 同一命令 GREEN | 退出0，BUILD SUCCESSFUL in 18s；37通过/0失败/错误/跳过 | `build/m03-evidence/green.log`、`green-test-results/` |
| `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` | 退出0，BUILD SUCCESSFUL in 59s；68测试通过，53任务/12执行/41up-to-date | `build/m03-evidence/regression-build-lint.log`、`app/build/test-results/testDebugUnitTest/`、HTML报告 |
| Lint XML | 0 errors / 12 warnings，四种既有提示：OldTargetApi1、AndroidGradlePluginVersion2、GradleDependency5、NewerVersionAvailable4 | `app/build/reports/lint-results-debug.xml`、HTML |
| `:app:dependencies --configuration debugRuntimeClasspath` | 退出0，182条唯一坐标线与M02一致，无新增运行依赖或JUnit/Hamcrest | `build/m03-evidence/runtime-dependencies.log` |
| `gradlew --version` | 退出0，Gradle8.14.3，Launcher/Daemon JDK17.0.20.1+1 | `build/m03-evidence/toolchain-version.log` |
| 既有 `verify_m01_apk.py` 检查最终APK | 退出0，身份/SDK/合并和包内权限、编译备份排除规则、版本目录和Wrapper一致 | `build/m03-evidence/apk/apk-verification.json`与文本dump |
| 最终 APK DEX/签名 | 11个M03公开领域类＋6个继承M02关键类描述符存在，测试类/框架不存在；apksigner退出0、v2通过、1签名者 | JSON与`build/m03-evidence/apk/signature-verification.log` |
| 归档/范围核查 | 原ZIP/43原件CRC、安全路径、逐字节/哈希与130唯一NOT_RUN通过；Q03～Q06/16跨模块补测不改，旧工程输入受保护 | `build/m03-evidence/static-audit.json`，最终文档检查另记checkpoint |

RED在新测试写好后，用此前不存在接口的编译轮廓执行：模型尚无构造校验、结构/写入验证空实现、成本返回无付款空结果，默认停止helper已能返回边界。34个失败都是业务断言，不是编译/环境失败；原M01/M02正确代码没有被故意改坏。随后实现三文件，测试源码不改，GREEN与全部回归通过。NO-SOURCE编译/资源任务不计为测试PASS。

## 六项原案例及领域补测

| ID | 方法 / 实际断言 | 当前结果 |
|---|---|---|
| M03-T01 | monthly_standard_rate：150/30=5，真实31天覆盖截至1月31日实际31天 | PASS |
| M03-T02 | annual_purchase_not_elapsed_year：3650/365=10，首日1天/4月10日100/年末365，到期停止增长 | PASS |
| M03-T03 | prepaid_next_rate_does_not_start_early：1月20付下一期180，当前仍5，2月1变6；已付330/标准60/历史5.5 | PASS |
| M03-T04 | expired_has_zero_current_rate：2月1排他边界 EXPIRED、贡献0/价格null，实际31天不再增长 | PASS |
| M03-T05 | pause_does_not_erase_paid_history：1月10暂停、20恢复，已付150/标准30不减，暂停实际9天、恢复日10天/月末21 | PASS |
| M03-T06 | overlaps_rejected：重叠拒绝/邻接允许/写入未来付款拒绝 | PASS |
| Q03-V01（仅订阅日期领域） | 原T02的首日1/第100天/365天/到期不增长 | PASS；M21里程碑触发仍NOT_RUN |
| Q03-V02（仅订阅日期领域） | annual_pause_excludes_ten_days_and_next_real_period_reaches_365：年末355，真实邻接下一期至2027-01-10累计365 | PASS；完整跨模块补测仍NOT_RUN |
| Q03-V04（仅订阅闰日起算） | leap_day_start_reaches_365_on_february_27：2024-02-29首日1、2025-02-27累计365 | PASS；长期节点/导入水位/通知7日窗仍NOT_RUN |
| Q04-V01（仅成本区间） | reminder_boundary_does_not_extend_paid_coverage：[09-15,10-15)，10-15至提醒日10-20贡献0、实际30不增 | PASS；提醒资格由M20明确，仍PROPOSED |

37项还覆盖：季度90、改周期、未生效预付为EXPIRED、历史忽略后来真实付款、无付款null原因、付款今天合法、金额0/空反向覆盖/标准天数不匹配拒绝、同日冲突双顺序/同状态重复、列表重排不变、未来暂停/停止不改过去、暂停无覆盖状态、过期恢复不造付款、默认与显式提前停止、覆盖空档/嵌套/重复、闰年自然366与标准365分开、0.01/30的DECIMAL128极小非零、LocalDate.MAX、Long日期跨度和生效日状态边界。

回归分组：MoneyParserTest7、BusinessClockTest2、ProjectCostEngineTest22、SubscriptionCostEngineTest37，共68通过。金额/日期核心输入未改；M02项目溢出断言回归通过，但M03订阅合计溢出未专门运行，不能把Math.addExact源码检查或M02断言替代这一项行为证据。

## 最终本地 APK 与边界

绝对路径 `E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`；10,463,689字节；SHA-256 `924ecf5a0f49239534661516e0c7a3797bb10d052c68b2c42be75139e99d99f5`。com.cooper.lithic.debug、versionCode1、versionName0.1.0-m01-debug；仍M01占位页面，仅构建回归，不发布/上传。唯一权限为AndroidX签名级内部接收器权限；网络/通知/广泛存储/精确闹钟未增加，allowBackup=false及三套九域排除配置已检查。不代表OEM行为已测。

NOT_RUN / N/A：本轮设备重新安装/断网冷启动/Instrumentation、业务数据库/Repository/迁移/完整备份；当前没有对应交互/实现。真机/API26/OEM保留M01未测NOTES。Q03长期100/1000日节点与M21、Q04提醒资格M20、Q05图片M10/M12、Q06回退M04设计/M12实现保持原期限。130原案例和16组完整补测状态不改，本轮只登记实际领域结果；没有M04/M08/M09业务存储/界面或M20/M21功能。

敏感信息、链接、完整差异与Git交付见[M03 checkpoint](../checkpoints/2026-10-11-M03.md)。本机日志、RED快照、测试产物与APK均位于忽略路径；仅提交合成固定日期测试、当前文档和可核对元数据。
