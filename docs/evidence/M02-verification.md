# M02 实际验证证据

日期：2026-10-11。起始基线 `a25a06899bd157988c3a4466d7162d8616fb0249`，分支 `feat/m02-project-cost-engine`。本轮实现状态 READY_FOR_REVIEW，独立验收 PENDING。原130案例仍NOT_RUN，当前实际结果另记[验收表](../ACCEPTANCE_MATRIX.md)。

[核对JSON](M02-verification.json)包含31个RED/GREEN测试逐项结果、失败类型/信息、实现前新API轮廓、27个最终构建/测试输入SHA-256、XML和本机日志哈希、APK元数据、归档与依赖检查。源文本哈希统一CRLF为LF，JAR/APK/XML/日志使用原字节；测试源码在RED到GREEN间未改动。原日志、报告、缓存和APK保留本机，不公开推送。

## 实际命令与结果

仅进程内复用既有Temurin17.0.20.1+1和SDK36，没有安装/升级或修改全局环境。

| 验证 | 实际结果 | 本机证据位置（不提交原产物） |
|---|---|---|
| `:app:testDebugUnitTest` 初次运行 | 退出1，classloading initializationError；不属于有效RED | `build/m02-evidence/test-initial-classloading-failure.log`、`test-classpath-diagnostic.log` |
| 测试worker参数编码假设实验 | 指定daemon与launcher原生编码一致后，31项进入测试断言；退出1 | `build/m02-evidence/test-encoding-hypothesis.log` |
| 标准 `:app:testDebugUnitTest --console=plain` RED | 退出1，BUILD FAILED in 10s；31项/1通过/30 AssertionError失败/0初始化错误/0跳过 | `build/m02-evidence/test-red.log`、`red-results/TEST-*.xml`、`red-inputs/` |
| 同一标准命令 GREEN | 退出0，BUILD SUCCESSFUL in 44s；31通过/0失败/0错误/0跳过 | `build/m02-evidence/test-green.log`、`app/build/test-results/testDebugUnitTest/TEST-*.xml`、`app/build/reports/tests/testDebugUnitTest/index.html` |
| `:app:lintDebug :app:assembleDebug --console=plain` | 退出0，BUILD SUCCESSFUL in 2m20s；48任务，29执行/19up-to-date | `build/m02-evidence/build-and-lint.log` |
| lint XML解析 | 0 errors / 12 warnings：OldTargetApi 1、AndroidGradlePluginVersion 2、GradleDependency 5、NewerVersionAvailable 4 | `app/build/reports/lint-results-debug.xml`、HTML |
| `:app:dependencies --configuration debugUnitTestRuntimeClasspath` | 退出0；JUnit4.13.2及传递Hamcrest1.3仅测试 | `build/m02-evidence/test-dependencies.log` |
| `:app:dependencies --configuration debugRuntimeClasspath` | 退出0；182条唯一依赖坐标线与M01日志完全一致，无JUnit/Hamcrest | `build/m02-evidence/runtime-dependencies.log`、`dependency-checks.json` |
| 既有 `verify_m01_apk.py` 检查本轮最终APK | 退出0；身份/SDK/合并与包内权限、编译备份资源、精确目录与Wrapper一致 | `build/m02-evidence/apk/` |
| 实际APK DEX检查 | 六个关键领域类描述符存在，JUnit/Hamcrest及本轮测试类不存在 | `build/m02-evidence/apk-domain-checks.json` |
| 原件/当前问题边界检查 | ZIP路径安全/CRC/43原件逐字节/哈希、阅读版SHA；130唯一NOT_RUN；Q03～Q06四行和16组NOT_RUN补测与基线一致 | `build/m02-evidence/source-and-lint-checks.json` |

构建下载依赖与App运行联网要求不同。本轮没有增加网络能力，也未重新安装或冷启动设备。NO-SOURCE编译Java/Shader任务没有计为测试PASS。Lint版本提示、中文路径experimental与原生库符号未剥离提示均保留，未加baseline/disable或升级依赖。

## RED 的有效性与环境修复

先完成行为测试，再提供仅能编译的新API轮廓：解析返回格式错误、时钟返回LocalDate.MIN、引擎返回尚未开始的空结果、构造验证尚未实现。它们是此前不存在的接口，未改坏任何M01正确行为。唯一RED通过项为拒绝非法/带符号输入；其余30项在实际行为断言中失败，具体信息保留在JSON。最终实现替换这四份轮廓，三份测试文件哈希不变。

初次测试类已经编译，但Gradle写UTF-8 worker `@argfile`，JDK17在Windows按原生GBK解码，导致中文classpath损坏；这是环境失败。实际核对参数字节、已生成class和launcher classpath后，只移除项目daemon强制UTF-8，让写出/解码使用相同原生默认值。没有硬编码GBK、升级Gradle/JDK或改全局编码。官方依据与跨平台取舍见[工具链锁](../TOOLCHAIN_LOCK.md#m02-增量2026-10-11)。之后标准命令RED与GREEN均真正运行31项。

## 当前案例映射

| ID | 测试方法 | 实际结果与具体断言 |
|---|---|---|
| M02-T01 | `same_day_is_one_day` | PASS：01-01开始/投入1000元；1天/1000元每天/活跃，初始投入只算一次 |
| M02-T02 | `dated_event_does_not_rewrite_earlier_days` | PASS：01-11投入200，01-10为100，01-11为109.0909090909090909090909090909091元每天 |
| M02-T03 | `end_day_removes_current_contribution` | PASS：01-10结束，10天/100元每天且日末不贡献 |
| M02-T04 | `post_end_refund_keeps_duration` | PASS：01-20收回200，净800元/10天=80，未重新活跃 |
| M02-T05 | `money_precision_and_zero` | PASS：解析0.10/0.20后实际引擎得30分/0.30元每天，0元有效，超精度/范围拒绝 |
| M02-T06 | `negative_cost_is_not_clamped` | PASS：投入5000/收回6000，净-1000元，10天为-100元每天 |
| Q02-V01（领域） | `q02_v01_ended_snapshot_uses_historical_observation_day` | PASS：已有结束日期的快照，开始前0天/null/无贡献，01-02为2天/500，01-09为9天/111.1111111111111111111111111111111且活跃 |
| Q02-V02（领域） | `q02_v02_refund_changes_only_its_occurrence_day_and_after` | PASS：01-10/19为100、01-20/31为80，始终10天/结束日及以后无贡献 |
| Q02-V03（领域） | `q02_v03_same_day_end_and_late_settlement_share_one_evaluation` | PASS：同日开始结束1天/不贡献，后退款仅发生日起改变结果，持续1天/不活跃 |

测试分组实际计数：MoneyParserTest 7、BusinessClockTest 2、ProjectCostEngineTest 22。除上表还验证了直接构造不能绕过单笔限制、巨大数字/非法格式、预付款、非法结束日期、零投入、分期、结束后尾款、独立退款、乱序/同日两笔相同真实付款、聚合超过单笔限额与Long溢出、0.005/-0.005、闰日、LocalDate全范围Long天数与固定时区。溢出测试使用合法最大单笔的惰性列表，不制造非法Money或分配百万事件对象。

Q02三项仅为领域部分PASS；历史页面/总成本总线尚未实现，M17/M18仍NOT_RUN。原CLARIFICATIONS的16组完整跨模块记录、原ZIP130案例不改写。

## 最终本地 APK

- 路径：`E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`。
- 大小：10,448,211字节；SHA-256：`ac72927087d7e8116560f8458ed8660980687b0a3d5c689be1cbb9c1b5f3bf6e`。
- applicationId `com.cooper.lithic.debug`；versionCode1、versionName `0.1.0-m01-debug`；保留M01占位UI与版本，不作为新发布版本。
- 唯一签名级内部接收器权限不变；无INTERNET/广泛存储/相机/通知/精确闹钟权限。allowBackup=false，旧规则/cloud-backup/device-transfer各排除九域，Emoji可下载字体初始化入口仍不存在。
- APK、签名密钥与真实数据不进入Git。M01原APK另保存在忽略的 `build/m02-evidence/baseline-m01-app-debug.apk`，原M01证据哈希未改。

## 未执行 / 不适用

Instrumentation、安装/断网冷启动、真机/API26/OEM：NOT_RUN，本轮无新的Android交互或界面，保留M01历史NOTES。Room/SQLite/迁移/Repository/完整备份往返：NOT_RUN / N/A，未实现且不属于M02。TypeScript/ESLint：N/A，原生Kotlin项目。展示舍入/页面、M03～M23及ChatGPT独立验收：尚未执行，不能由本轮JVM通过替代。

源与范围、文档链接/敏感信息、提交/远端一致性核对记录于[M02 checkpoint](../checkpoints/2026-10-11-M02.md)。本文件只证明实际执行，不自行授予ACCEPTED/FROZEN。
