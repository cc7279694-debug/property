# M22 集中验证与停止记录（2026-10-23）

**READY_FOR_REVIEW — M22；Android整体可用性门禁BLOCKED，不能冻结。**

起点：M21冻结登记`a367b278bb3186cf6a8f5afdcf7f843e688fbce2`；分支`codex/m22-experience-accessibility`。本轮只改Compose、图表/快照内部准备、定向测试及内部版本code17；Room5/backup4、冻结M02/M03/M16/里程碑规则、owner/恢复协议、依赖和工具链保持。未合并M10或独立UI-C1。

## 实际改动与验收矩阵

- 项目列表标题/说明/新增区并入原LazyColumn；列表、详情保留旧结果时明确尚未刷新。
- 图表横向拖动、文本、前后日和语义动作共用自然日边界；值仍由原引擎精确查询。等待时不显示另一日期金额，旧请求不能覆盖新日期。
- 全量逐日扫描仍保留事实边界、三线峰谷和事件。一次不可变快照内复用订阅恒定区间的原引擎贡献、避免重复求值/分配；项目仍调用冻结引擎，BigDecimal不近似。加入对象级协作取消，没有固定最坏时延保证。

| 案例 | 本轮真实结果与边界 |
| --- | --- |
| M22-T01 narrow_largefont_touch | **部分证据，整体NOT_PROVEN**：8项组件最终PASS，检查部分关键操作宽高均≥48dp及不重叠；真实系统360dp/font_scale2.0与LocalDensity分别登记。实际屏幕存在SystemUI ANR遮挡，不能把控件层截图/语义点击当作无遮罩可用性通过。IME前后均false，真实键盘遮挡NOT_RUN；TalkBack已安装但未启用/操作。 |
| M22-T02 no_fake_or_missing_states | **限定PASS**：空/真0/负亚分、旧结果/错误、精确金额及迟到请求等定向JVM/Compose通过；注入日期配合真实前台观察器/Activity重入刷新PASS，不是真实午夜。完整恢复后刷新仍未完成新旅程验收。 |
| M22-T03 twenty_year_synthetic_dataset | **NOT_RUN**：代码准备1000项目+1000订阅、20000项目事件(含初始)、10000真实付款，固定seed20261023、2006-10-23～2026-10-23、7306自然日。完整规模Android方法未启动，实际表计数、读取/计算/渲染耗时、App内存和卡顿均未取得，绝不登记性能PASS。 |
| M22-T04 chart_interaction_keeps_truth | **限定PASS**：137 JVM中的随机/全范围参照、峰谷/关键边界及10项M17/M18 VM、6项新图表、5项总趋势语义回归通过。日期精确查询、横拖/纵滚、>50事件/旧代隔离有相应测试；真实读屏及无遮罩设备手势未证明。 |
| M22-T05 identity_assets_are_honest | **PASS（登记范围）**：Debug标签“砺石 LITHIC Debug”，code17，启动/圆形/通知图标仍为工程临时`ic_launcher_placeholder`；最终App无INTERNET。不是正式Logo、发行签名或M23门槛通过。 |
| M22-T06 runtime_offline_journey | **FAIL / INCOMPLETE**：真实运行时飞行模式1、Wi-Fi0、移动数据0，无默认IPv4/IPv6路由；使用生产LithicApp/Nav3/VM/Room的隔离宿主。旅程3次失败原样保留：漏填必填结束原因、对固定导航调用滚动、详情金额节点未出现。第三次仅有2子节点的容器及源码支持等待接缝假设，实际根因仍待证明；最终同步修正仅编译，未运行；完整导出/恢复和事实核对未获本轮PASS。受控文件目标不是新SAF验收。 |

[UI分层检查](../UI_ACCEPTANCE.md)、[契约](../planning/M22_CONTRACT.md)、[23个代码/测试/版本输入SHA](m22/checks/source-inputs.json)。大样本是待执行的固定设计，不是已经生成或运行的实测规模。

## 测试、失败与构建

- **JVM 137/137 PASS**（0失败/跳过），原M21的395项未重跑：[逐方法](m22/checks/jvm-results.json)、[XML](m22/checks/jvm/)、[日志](m22/checks/jvm.log)。原RED中3项2失败：缺少取消检查是产品RED；0分续费fixture非法是测试输入错误，已分别处理，不把后者算产品缺陷。[RED原件](m22/red/)。
- **Android计划38个唯一方法：执行37，最新36 PASS/1 FAIL，另1 NOT_RUN**。含重跑47次方法执行=40 PASS/7 FAIL，不累计成覆盖量。[逐方法、每次结果及Test SHA](m22/android/results.json)。首轮UI8项4 PASS/4 FAIL永久保留；其中3处完整句子匹配修正、1处Espresso窗口焦点错误改为真实Insets有界观察。最后观察IME并未显示，不能宣称键盘或焦点根因解决。
- 原旅程失败：[漏填原因](m22/android/commands/r2-journey-07/stdout)、[固定导航滚动](m22/android/commands/r3-journey-07/stdout)、[详情节点缺失、定位待证](m22/android/commands/r4-journey-07/stdout)。未改变原金额、事务或业务断言；测试修正后并未绕过设备安全门禁重新执行。
- App/Test Debug构建成功；最后测试包/Lint命令exit0、`BUILD SUCCESSFUL in 7m`：[最后日志](m22/checks/journey-ready-package.log)。Lint **0错误/17警告**：[结果](m22/checks/ready-lint.json)、[实际XML](m22/checks/lint.xml)。M21历史25警告记录不改；原构建编译失败和其他实际构建日志均保留在[checks](m22/checks/)。TypeScript/ESLint不适用，Release未执行。

## 关键阻塞：实际SystemUI ANR

查看真实`UiAutomation.takeScreenshot()`全屏帧时发现 **“System UI isn't responding”** 对话框遮挡应用：[原PNG](m22/screenshots/blocker-systemui-anr.png)、[来源/SHA](m22/android/systemui-blocker.json)。随后只读窗口核查仍为`Application Not Responding: com.android.systemui`：[原输出](m22/android/commands/end-window-state/stdout)。当前环形am_anr日志无匹配项，不能推翻屏幕/窗口实证，也不能确认首次发生时间及根因；与早期无焦点现象的因果仍UNKNOWN。

发现后未再安装/执行测试、未启动长历史负载、未点“关闭/等待”、未强停、重启或清场。6张已归档图是**真实设备Compose控件层captureToImage**，会排除SystemUI弹窗，不能作为完整屏幕无遮挡证明：[图源、尺寸、SHA](m22/screenshots/ui-index.json)。它们不是MainActivity、真实TalkBack或系统通知送达证据。后续T01/T03/T06需要新的明确设备授权和独立准入，不能据本报告自动重开5712。

## APK、环境和保护

| 产物 | 身份与真实SHA-256 |
| --- | --- |
| App（实际安装，所有Android轮次字节相同） | `com.cooper.lithic.debug` / UID10216 / code17 / `0.1.0-m22-debug` / 13,822,236字节；`328e3dc2fbe4c2a503fc81f2750cd68a62e5bf2b2bf624620b21d63b3f9bf213` |
| 最后实际安装Test | `com.cooper.lithic.debug.test` / UID10217 / 3,764,579字节；`c9788ced5d20b80397adf04ec1ce87ef72d208716925a76cd072fd74e3e2eef7` |
| 最后可构建Test候选（**未安装/运行**） | `f671f7643ffe289f3f19440d84e03d0735f0d9b5be1f183b2c01ee3ea7658d1f`；仅增加详情/列表就绪等待，保留原15秒边界和全部断言。 |

统一Debug证书SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；Runner `com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner`。[实际安装/回读](m22/android/installed-final.json)、[未安装候选及检查](m22/checks/ready-artifacts.json)、[证书](m22/checks/ready-test-signature.txt)。App更新1次，Test同签名更新4次；没有卸载/清数据/正式签名。APK和密钥不入Git。

仅复用`Lithic_M21_API36_20261023 / emulator-5712`：API36/x86_64、实际2560MiB/2核/headless/no-snapshot。[静态归属](m22/android/static-admission.json)、[PID/创建时间/端口](m22/android/runtime-host-proof.json)、[Boot](m22/android/boot-proof.json)。新建0、实际启动1、正常退出1、其他设备操作0。首次日志目录错误发生在emulator未创建进程之前，原错误/claim保留，未重置或掩盖启动次数。

结束前设置回原值（font1.0/density420/浅色、飞行模式0/Wi-Fi1/数据1）；无目标App/Worker、默认generation、数据库文件或目标注册Job。Job全局文本中出现的UID目录/不活跃TopAppTimer不是任务，原宽匹配与后续完整JOB边界分析分别保留，未取消任何任务。[保护汇总](m22/android/end-protection.json)、[Job观察](m22/android/jobs-end-classified.json)。

旧15个零字节锁的UID/inode/同Boot设备号/链接数/大小/类型/路径未变；旧9个截图相关文件SHA均未变。本轮新增14个零字节缓存锁保守保留，锁状态未单独确认；关闭Owner后的新合成业务根已移除，缓存截图/证据保留。仅正常退出5712后，原PID和端口均结束：[退出证明](m22/android/exit-proof.json)。QEMU Windows进程生命周期峰值工作集3,816,259,584字节（约3.55GiB）；AVD文件逻辑长度增加608,650,129字节（约580.45MiB），NTFS实际物理增量NOT_CAPTURED；二者不是App资源或长历史性能结论。[资源原件](m22/android/resources-end.json)。

## 独立审查前保留

M22整体未通过、不得冻结。真实IME、TalkBack、完整无遮挡UI旅程、长历史实测、真实午夜、Release/真机/OEM/API26仍NOT_RUN或NOT_PROVEN；SystemUI ANR根因UNKNOWN。M10 PENDING/DEFERRED及三JPEG历史FAIL、M13-T06延期、M20平台FAIL/第二次收口BLOCKED/UNKNOWN、5704及H2/C2/其他受保护现场、Q03未执行子项/Q04 PROPOSED/Q02完整16组/G1/G2/G3全部继承。只推本轮分支后停止，不合并main、不进入M23。
