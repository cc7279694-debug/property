# M17 验证与交付

**READY_FOR_REVIEW — M17；等待ChatGPT独立验收，不自行冻结。**

来源M16冻结回写 `ca289f12a31ce67882a1254f6dbd71a80cad18dc`；分支 `codex/m17-item-cost-charts`。原始规划/130案例/16组完整跨模块记录与既有NOTES不改。

## 产品与代码

项目详情新增成本历史，订阅详情新增价格历史，保留当前/历史/复盘及替换对比返回路径与原导航守卫。只读完整事实快照、固定观察日，所有显示点和任意日期都回M02/M03引擎；没有新公式、持久图缓存、schema或备份字段。

[领域时间线](../../app/src/main/java/com/cooper/lithic/domain/report/ProjectTimelineEngine.kt)、[订阅时间线](../../app/src/main/java/com/cooper/lithic/domain/report/SubscriptionTimelineEngine.kt)、[完整读取](../../app/src/main/java/com/cooper/lithic/data/repository/ItemTimelineRepository.kt)、[刷新与请求隔离](../../app/src/main/java/com/cooper/lithic/feature/report/ItemTimelineViewModel.kt)、[共用图表](../../app/src/main/java/com/cooper/lithic/core/design/CostChart.kt)与[页面/文字替代](../../app/src/main/java/com/cooper/lithic/core/design/CostChartPage.kt)为主要新增；Holder/Nav3/原详情仅接入。Gradle仅版本更新为code12，不升级依赖。

240仅为均匀基础点预算，全部事实边界及必要邻点额外保留，总数可超过240；不枚举多年全部日期，但计算成本随事实量×保留点数增长，完整读取还继承M16事实校验成本，不宣称固定240或无限历史恒定耗时。事件窗口50条，同日所有记录可展开；日期查询不受采样影响。项目结束后结算不与持有曲线相连；订阅阶梯保留暂停/停止/到期0区间。金额保留Long/BigDecimal，坐标才转浮点。

## 原案例与实际结果

| 案例 | 实际结果与证据 |
|---|---|
| M17-T01 past_event_boundary | PASS：维修前不受后事件影响；按occurredOn，完整105事件读取；Q02的1/2=500、1/9=1000/9、1/10=100、1/19=100、1/20=80，持有10天不变。 |
| M17-T02 subscription_is_step_not_smooth | PASS：1/20预付仍5，2/1生效6；暂停/恢复、到期空档、停止/同ID重开、原standardDays与未来计划边界。 |
| M17-T03 events_are_accessible | PASS：同日完整清单、超过50条、非现金折抵、可读日期/金额/状态/说明；日期输入、事件、返回在360dp/200%可达。非完整TalkBack实测。 |
| M17-T04 negative_values_visible | PASS：真实0/负值/极小非零/全负/单点；负区间不截0。图08及文字替代使用原金额组件。 |
| M17-T05 no_forecast | PASS：不画今天以后；结束日保持真实单项成本，后结算独立；未来预付/停止只作计划文字。图07/09。 |

**144 JVM PASS（33新增＋111相关回归）＋28 Android PASS（19本模块＋9相关回归）＝172个唯一方法，0失败/错误/跳过。** 逐类源码方法与XML/真实Instrumentation完成身份完全对应，见[精确统计](m17/results.json)、[原始输出索引](m17/README.md)。前期口头164 JVM是加总错误，已按11类原始XML纠正为144，未补造20项。

最终同组检查 `testDebugUnitTest` 定向11类、`lintDebug`、`assembleDebug`、`assembleDebugAndroidTest` 成功，[最终日志](m17/logs/capture-final-verification.txt)；Lint **0错误9警告**，[原文](m17/lint-final.txt)。最后一次仅补截图位置，未变的任务为UP-TO-DATE；实际JVM运行及XML保留，不把缓存任务另计新测试。最终[281个构建输入指纹](m17/capture-final-build-inputs.json)构建/设备验证后复核一致。SHA记录的是本机实际构建字节；测试源另列Git规范化SHA，便于区分LF/CRLF文本行尾差异，原始证据/schema按-text保留原字节。

RED/GREEN如实保留：时间线/几何29项初次28 FAIL/1 PASS；真实折抵映射1 FAIL后GREEN；刷新新图瞬间遗忘选日1 FAIL后修为数据/日期/精确点共同发布，并强化缺失和捕获快照身份围栏。首次3处测试编译接缝及语义substring调用错误分别保存，不冒称业务缺陷。所有最终业务断言保留，失败记录不覆盖。

完整Room读取、写后刷新、可变BusinessClock/返回前台、迟到请求隔离、确认删除、读取失败留旧图、M16三事件只计一次、浏览前后事实不变、合成format2往返重算及旧代拒读均通过。恢复用受控文件目标，不是新的SAF验收。

## 实际设备、截图与APK

只复用原 `Lithic_M14_API36_20261021 / emulator-5672 / API36 / x86_64`。前后核对原launcher20744/QEMU7356创建身份、boot、隔离配置、端口与包；仅同证书Debug更新及plain Application runner `com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner`。未打开MainActivity默认账本，全部新UUID合成根在关闭owner后清理；结束no_backup为空，设置与M16记录一致，设备保持在线。[结束核查](m17/device-end/summary.json)。H2/C2/M10/5686等其它现场未操作。

[9张实际截图及来源](m17/screenshots/README.md)：隔离ComponentActivity中的真实LithicApp/Nav3/Room；360dp为LocalDensity画布，03/04为200%字体，非系统字号或真机。08/09补拍完整绘图画布，原首轮7张保留本机build及设备，不改旧结果。

| 产物 | 最终实际身份 |
|---|---|
| App | `com.cooper.lithic.debug`，code12 / `0.1.0-m17-debug`，UID10216，13,543,801 bytes |
| App SHA-256 | `3e1298c3c81f8241f67c7c5f48515af7a2031ec5ee56bf5dd0d88bb50845e43b` |
| Test | `com.cooper.lithic.debug.test`，未声明业务版本号，UID10217，2,923,214 bytes |
| Test SHA-256 | `fd025d53d5fdfdedd1e8ec13c9fdf2754b5d326ea7534b64172fbd0f4b6a85e3` |

[产物/安装字节](m17/install-capture-fix/artifacts.json)、[App Manifest](m17/install-capture-fix/app-manifest.stdout.txt)、[Test Manifest](m17/install-capture-fix/test-manifest.stdout.txt)、[签名](m17/install-capture-fix/app-certificate.stdout.txt)。原Debug证书SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；App min26/target36，无INTERNET，非正式发行签名。当前APK绝对路径 `E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`；APK和密钥不入Git。

## 限制与停止

API26/真机/OEM、MainActivity真实账本、Release/稳定签名、系统200%/完整IME/TalkBack、真实午夜/进程死亡/LMK/断电、长期大数据性能仍NOT_RUN。跨日为注入时钟＋前台生命周期；未穷举所有调度顺序。既有工具链/Kotlin/SDK警告保留。

Room3、backup2、冻结金额/订阅算法、M16交易保护、媒体与恢复协议没有改动。Q02只登记本轮M17子断言，M18/16组完整跨模块门禁不关闭。M10 PENDING / DEFERRED及三JPEG历史FAIL、M13-T06 NOT_RUN / DEFERRED、G1未整体通过全部保持。仅提交推送本分支，不合并main、不进入M18；待独立验收。
