# C3-1D 截图超时桌面审查

日期：2026-10-27。来源、本地及远端起点均为 `27c6598652577dd183358e615c490df4c1d73044`；分支 `codex/lithic-v1-product-completion`，开始时工作区干净。

**C3-1D DESKTOP DIAGNOSTIC READY / DEVICE_RUNTIME_STATE_UNKNOWN**

本轮仅读取仓库、既有证据、本地框架字节码和官方源码；不执行任何ADB、手机/模拟器操作、构建或测试，不修改业务、测试、配置或旧证据。

## 1. 已确认事实及120秒边界

[A原始回执](ui-c2-c3-capture-20261027-025415/A/command.json)记录：`03:06:13.010466Z`启动，120.063秒后桌面等待超时，stdout379字节、stderr0字节、外层退出码null/NOT_CAPTURED。本轮重新核对两份输出的长度及SHA，均与回执一致。[stdout](ui-c2-c3-capture-20261027-025415/A/stdout)只有A的单方法开始通知 `INSTRUMENTATION_STATUS_CODE: 1`，没有JUnit完成/异常堆栈/截图握手。A仍为 **UNKNOWN_TIMEOUT**，B～F2七项NOT_RUN，可提取截图0张；不能据此排除设备上有未完成文件。

[上轮结束观察](ui-c2-c3-capture-20261027-025415/end-protection-check.json)中的UID10435/PID6206存在是历史时点事实，**不是本轮实时进程结论**。Instrumentation是否仍活动、Owner/FD状态及实际停留阶段仍UNKNOWN。

只读检查原桌面监督代码：`build/c3-capture-session/capture_once.py:15`调用`command(..., timeout=120)`；`common.py:16–17`使用`subprocess.run(capture_output=True, timeout=timeout)`，捕获`TimeoutExpired`后保留其stdout/stderr并把退出码记为None。未添加Android端总时限或取消指令。

本机Python3.12.4的标准库实现先`communicate(timeout)`；超时调用该子进程的`kill()`，Windows实际为`TerminateProcess`，再收集输出并抛出异常。因此结束的是本次**桌面ADB客户端**；既无手机kill/force-stop，也不能推定远端shell、Instrumentation或App已结束。该120秒是桌面整条命令等待预算，不是已观察到某个Compose阶段耗时120秒，也不是T03计算预算。

原本地脚本保留且未执行/改写；只读指纹：common.py=`fea67eb84843bf65aae142e28a1302fa929fed844dfa3557f34c95d3cc88ba1a`，capture_once.py=`fef7f95f96615f5c6f8da0ea4d037b2562f5d11ef56019dcc7347785ec189703`。该脚本位于忽略目录，不冒充已提交框架源码。[CPython3.12.4实现](https://github.com/python/cpython/blob/v3.12.4/Lib/subprocess.py)为标准库对照来源。

## 2. 源码执行顺序与阶段表

下表的CONFIRMED仅指有实际运行记录；NOT_OBSERVED指没有越过阶段的独立证据；POSSIBLE_BLOCKER仅标记源码中存在的候选等待，**不是根因结论**。源码顺序不等于这些阶段实际成功。

| 阶段 | 本次运行证据 | 源码可确定的顺序/候选等待 |
|---|---|---|
| 启动Instrumentation并通知A开始 | **CONFIRMED**，仅到JUnit开始通知 | numtests=1、class/test准确；无完成通知 |
| 构造测试实例、初始化Rule | **NOT_OBSERVED** | JUnit4.13.2的`runChild`创建延迟Statement；`runLeaf`先`fireTestStarted`，延迟Statement才调用`methodBlock/createTest/规则初始化`。STATUS1连实例构造完成也不能证明 |
| 创建目标Activity | **NOT_OBSERVED / POSSIBLE_BLOCKER** | Compose环境包裹ActivityScenarioRule；规则`before → ActivityScenario.launch`先于用户@Before。启动前`Instrumentation.waitForIdleSync`，再启动Activity、等待生命周期；45秒生命周期条件等待不覆盖前置idle等待 |
| 执行@Before | **NOT_OBSERVED / POSSIBLE_BLOCKER** | [Gallery:61–78](../../app/src/androidTest/java/com/cooper/lithic/ui/C2ProductionVisualGalleryTest.kt#L61)：核对包/Runner/Application/UID，依次同步SHA读取自身App/Test APK，再在主线程设置edge-to-edge。64MiB是各APK读取字节边界，没有单独墙钟边界 |
| 构建合成事实 | **NOT_OBSERVED** | [Gallery:94](../../app/src/androidTest/java/com/cooper/lithic/ui/C2ProductionVisualGalleryTest.kt#L94)、[Fixtures](../../app/src/androidTest/java/com/cooper/lithic/ui/C2VisualFixtures.kt)：固定日期2026-10-26、内存事实；A的项目/订阅集合为空。[TimelineSampler:24](../../app/src/main/java/com/cooper/lithic/domain/report/TimelineSampler.kt#L24)走空集合返回，不执行20年扫描、不打开Room/业务Owner |
| setContent | **NOT_OBSERVED / POSSIBLE_BLOCKER** | [Gallery:99](../../app/src/androidTest/java/com/cooper/lithic/ui/C2ProductionVisualGalleryTest.kt#L99)挂载生产组件；Compose1.9.5实现内部也可调用waitForIdle，不只后面的显式等待 |
| 等待Compose空闲 | **NOT_OBSERVED / POSSIBLE_BLOCKER** | [Gallery:140](../../app/src/androidTest/java/com/cooper/lithic/ui/C2ProductionVisualGalleryTest.kt#L140)；没有“setContent返回”或“idle返回”的阶段日志 |
| 布局/语义断言 | **NOT_OBSERVED / POSSIBLE_BLOCKER** | [Gallery:141–160](../../app/src/androidTest/java/com/cooper/lithic/ui/C2ProductionVisualGalleryTest.kt#L141)检查导航、FAB、金额、数量、边界；语义查询自身也经过框架同步 |
| 捕获PNG | **NOT_OBSERVED / POSSIBLE_BLOCKER** | [Gallery:176–183](../../app/src/androidTest/java/com/cooper/lithic/ui/C2ProductionVisualGalleryTest.kt#L176)先建UUID目录/写owner，再captureToImage、创建PNG、压缩、fsync及SHA；没有握手不能证明这些文件不存在或写完 |
| 写入metadata | **NOT_OBSERVED / POSSIBLE_BLOCKER** | [Gallery:205](../../app/src/androidTest/java/com/cooper/lithic/ui/C2ProductionVisualGalleryTest.kt#L205)独占创建capture.json并sync/关闭；在PNG之后 |
| 回传截图状态 | **NOT_OBSERVED / POSSIBLE_BLOCKER** | [Gallery:206–210](../../app/src/androidTest/java/com/cooper/lithic/ui/C2ProductionVisualGalleryTest.kt#L206)调用sendStatus(2)，位于完整文件写入之后；379字节输出未包含该状态 |
| 外层Rule结束、JUnit结束 | **NOT_OBSERVED / POSSIBLE_BLOCKER** | ActivityScenarioRule.after会close并等待DESTROYED；方法主体已抛异常但外层关闭尚未返回也是候选情形。没有失败堆栈不能证明此前没有异常；不能反推已经正常结束 |

## 3. 启动合同与确定性缺陷判断

[已归档的实际APK Manifest和Runner证据](ui-c2-c3-capture-20261027-025415/desktop-apk-audit.json)记录锁定SHA。App为platform Application、exported空ComponentActivity，无正常Launcher；[M13Runner:8–10](../../app/src/androidTest/java/com/cooper/lithic/app/M13IsolatedRunner.kt#L8)明确传普通Application。[专属Manifest](../../app/src/c2Visual/AndroidManifest.xml)与该实际XML一致。

**未发现可据现有材料确认的必然死锁或启动合同错误；没有足够证据判定生产UI缺陷。** 无Launcher不能单独解释失败：本地ActivityInvoker实现使用targetContext和目标class构造显式ComponentName/Intent并resolve，不依赖普通用户Launcher。`runOnUiThread { compose.activity... }`也不能直接判死锁：ActivityScenario.onActivity主线程分支直接执行action（先处理受控Looper），非主线程才使用waitForIdleSync/runOnMainSync。并未以“看起来同步”替代等待链证据。

框架核对来自已存在的AAR/classes.jar、只读javap及JUnit4.13.2实现，无构建或依赖下载：Compose ui-test-junit4-android/ui-test-android1.9.5，AndroidX test core/runner1.7.0、ext-junit1.3.0、monitor1.8.0。JUnit通知链参考[BlockJUnit4ClassRunner](https://github.com/junit-team/junit4/blob/r4.13.2/src/main/java/org/junit/runners/BlockJUnit4ClassRunner.java)及[ParentRunner](https://github.com/junit-team/junit4/blob/r4.13.2/src/main/java/org/junit/runners/ParentRunner.java)。本地Compose两份classes.jar SHA分别为`ffacd23befbbdec9549e1ebc7e6a7c2b5c42e91110503385d6c634c3806fecae`、`cd28e74fba5b04d1066fe707b9ee32ce14e07025045ef89107b5061eaafe1893`；core1.7.0为`0d897808fd493386a74364d21b2ae39b9becf7f92040a8ff770aa4af95340384`。这些是桌面控制流证据，不是手机线程栈。

明确缺口是**阶段可观测性不足**：截图握手发生在接近方法末尾，中间没有阶段回执。现有材料不能在Rule启动、@Before、idle、语义查询、截图、I/O/Binder或外层关闭之间定位实际阻塞点；也不能用上轮非空焦点/无明显ANR标记排除所有线程阻塞。本轮不据猜测改测试、预算或生产代码。

## 4. 建议的下一次最小只读观察（仅计划，需新授权）

建议至多**一个有界观察会话**，总上限90秒，单命令上限10秒、单项64KiB/总256KiB；任何不支持、超限或身份不符均停止该项，保留NOT_AVAILABLE/UNKNOWN，不换全量查询。只针对新C3 App/Test；不重跑、不安装、不唤起或停止App。

1. **实时身份与进程**：明确唯一授权USB目标，读取Boot和新两包必要安装元数据；仅返回UID10435/10437匹配的PID/进程名及可正常取得的创建身份。PID6206只作历史线索，不能直接沿用。无目标进程则如实记录，不启动；身份变化或无法排除PID复用则不取历史日志。
2. **Activity/Instrumentation**：只申请新App的精确ComponentActivity状态；Instrumentation的ComponentName.packageName必须严格为 `com.cooper.lithic.c2visual.debug.test`，target必须严格为 `com.cooper.lithic.c2visual.debug`。不得按共用Runner的Java包 `com.cooper.lithic.app` 扩大匹配。`pm list instrumentation`只表示注册，不能证明运行。Android16 [AMS dumpActiveInstruments](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-16.0.0_r1/services/core/java/com/android/server/am/ActivityManagerService.java#L11194)具有包过滤，但完整processes dump仍可能含全局信息；**不把它直接列为获准整段命令**。后续执行前必须离线确认能严格只返回上述目标字段；无安全独立入口/手机OEM不支持时，该项NOT_AVAILABLE，不改用全局activity/process/window dump或借过滤失败推断无活动Instrumentation。
3. **目标错误日志（可选）**：只有新包PID/UID/创建身份核实、且平台能同时限制目标PID、UID及当前进程时间范围时，申请一次main/crash中的错误级记录，至多100行/64KiB/10秒，读取前后复核同一进程身份；不读取system/events全量缓冲，不触发新日志或发送信号。不支持严格限制或权限拒绝则NOT_AVAILABLE，禁止root、SIGQUIT、bugreport、转取其他PID日志。没有错误日志也不能证明无阻塞。
4. **有限系统健康**：仅连接状态、可用内存、电量/温度和SystemUI进程是否存在等必要指标；不取无关焦点窗口、私人屏幕或整机日志。单次观察结束不等于线程栈定位、业务Owner关闭或视觉PASS。

这只是权限/输出边界建议，未执行任何手机命令，也不预先保证OEM能提供全部字段。一次观察不足以定位时继续UNKNOWN，结束并交独立审查，不自动增加第二轮。

## 5. 为什么不直接重跑及保护范围

A的一次额度已消耗，旧实例是否仍执行未知。重跑可能与旧Instrumentation/宿主重叠，新增无法归属的文件，且没有新阶段证据仍难定位；直接延长等待也不能说明根因。保留A UNKNOWN、后七项NOT_RUN、0张已提取PNG和所有历史结果。

本轮只新增本报告。没有读取手机或旧App数据、没有重新核验实时安装/进程状态，没有构建、测试、重新安装、强停、清理、系统设置修改或Preview6。原新包实物SHA/UID及旧包安装保护结论只引用[C3-1报告](UI-C2-C3-Native-Capture-Execution.md)的历史时点，不冒充本轮实测。Room5/backup4、生产UI、业务/Owner/恢复/Nav3/媒体/通知协议和M10/M20/M22/M13/G1～G4所有历史FAIL/UNKNOWN/NOT_RUN/NOTES不变。**APP_VISUAL_PENDING；不冻结、不合并main、不进入设备诊断或新一轮截图。**
