# UI-C1 阶段1 — 三页真实双主题样板

日期：2026-10-15。状态：**VISUAL_REVIEW_REQUIRED — UI-C1**。

用户及ChatGPT已书面批准阶段0，并从`edc8be772a795f40b44f15da79ebf62a529bc074`授权Task1～Task5。本报告只登记本阶段实际执行。样板尚待用户看图确认；没有执行Task6～Task8，没有独立验收或自行冻结。

## 1. 交付与边界

已实现现在、长期项目列表、长期项目详情的同结构深浅主题，以及必要共享主题、卡片/状态/空错组件、12个原创本地矢量图标、受控封面预览、三Tab图标与文字、两选项新增菜单。系统主题切换不增加持久设置。完整图片、逐图设备/字体/主题/APK指纹见[样板图册](ui-c1/stage1/GALLERY.md)和[截图登记](ui-c1/stage1/screenshot-manifest.json)。

数据库/实体/迁移/DAO、领域引擎、Money/BusinessClock、业务Repository/媒体协议、Nav3序列化键/来源链/三VM守卫、依赖/构建配置/权限/应用身份均未改。其它页面仅继承共享主题，未执行其余页面和表单统一。M10分支、WHOCARE和main未改；正式ACCEPTANCE_MATRIX、原130条、16组跨模块边界与历史证据不改写。

## 2. Task1～Task5 实际实施

| Task | 实际结果与核验 |
|---|---|
| 1 主题/组件 | 暖白与深墨绿系统主题、批准字号/圆角、PageHeader/StatusBadge/EmptyState/卡片/原生图标。金额继续复用原格式器，null、零、负值及微小非零语义保留。4个新增主题/布局测试通过。 |
| 2 首页 | 同一个asOf组合四个现有只读API，真实全身份计数明确包含历史/停止；项目用已有50+哨兵窗口后展示最多3条，订阅复用pageSize3及原完整事实预检。独立重试/陈旧提示、请求序号和同代repository身份核对防迟到发布；不宣称四次读取是原子快照或O(3)。7项新增真实存储/交错测试、8项首页样板测试通过。 |
| 3 封面 | 生命周期内共享加载器、IO采样、并发2、按allocationByteCount计费的8MiB LRU，超限不缓存；重入先openAsset重新验证后才能复用图片。repository/目标尺寸参与身份，关闭/失败/换代不展示旧缓存。卡片64dp/详情16:9 Crop/查看Fit，不写图片、库引用或新增锁。14项新增真实图片/缓存/取消/大字体测试通过。 |
| 4 项目样板 | 原排序、SQL分页、历史来源、身份与回调保留。列表一体滚动，详情主指标/封面/资金/资料/事件分组；更多菜单保留编辑/封面/结束或修改结束/删除。普通结束与红色删除分义，原确认继续执行。新增更多菜单测试及现有项目屏幕/资金/生命周期定向测试通过；最终APK大小字体真实留图。 |
| 5 导航/样板门禁 | 只装配原requestLeave/原Nav3页面及三个VM。新增菜单取消零业务回调，两个选择仍进入原创建路径。既有三VM守卫/流程定向回归在候选A运行；最终B重新运行变化相关屏幕和全部新增测试。6张最终APK正常图、2张隔离空态图、360dp/200%首页和详情及菜单/滚动原图已齐。原数据/媒体/设置前后精确核对；停在看图门禁。 |

35个新增方法均在最终候选B实跑PASS，原657个方法身份全部保留，当前源码共692个。实现、测试驱动适配及证据路径由[文件清单](ui-c1/stage1/files.md)和[静态身份检查](ui-c1/stage1/static-current.json)登记。没有为简单视觉机械增加全套业务测试；高风险只读范围、请求交错、媒体身份与预算有针对性重复测试。

## 3. 实际执行及失败历史

8个测试阶段的命令、真实退出码、完整日志、鲜取XML、逐方法结果和**每阶段登记输入ZIP**见[执行登记](ui-c1/stage1/test-executions.json)与[tests目录](ui-c1/stage1/tests)；最后独立Lint/assemble另见[实际命令](ui-c1/stage1/final-artifact-command.json)和[完整日志](ui-c1/stage1/final-artifact.log)。各ZIP/输入/报告哈希已重新核对；最终174份登记输入与工作树逐字节相同，见[输入与测试绑定](ui-c1/stage1/input-and-test-binding.json)。登记覆盖main/test/androidTest全部文件、schema及七份Gradle配置/包装器文件；不包含Debug字符串和两个启动脚本，这三份与起点无Git差异的当前补充核查见[补充输入](ui-c1/stage1/supplemental-inputs.json)，不追溯声称运行时已哈希。外部工具/依赖环境单独锁定，并非完整可重建环境快照。

| 阶段 | 实际执行 | 结果 | 含义 |
|---|---|---|---|
| baseline-production | 原生产未改；56 Android，470.361秒 | 52 PASS /4 FAIL，退出1 | 三项JPEG失败＋旧结束表单页脚在IME打开时不可见，提前存在，不当成UI新回归。 |
| red-ui-contracts | 编译13.375秒 | 0执行，退出1 | 新测试误引入不存在的顶层断言import；完整错误保留，不能当行为RED。 |
| red-ui-contracts-compiled | 14 Android，93.372秒 | 4 PASS /10 FAIL，退出1 | 旧生产下批准的主题/菜单/卡片/详情新契约实际失败；金额/坏图保护等原行为仍通过。 |
| green-samples | 93 Android，459.193秒 | 92 PASS /1 FAIL，退出1 | 旧全页面禁止¥0.00断言不适用于真实零成本预览，后续限定到概览并额外断言真实零卡片可见。 |
| red-large-cover | 1 Android，80.629秒 | 0 PASS /1 FAIL，退出1 | 64dp坏图文本在200%字体被截；改为完整错误语义的32dp警示图标，保留原错误标签/管理入口。 |
| final-stage1（候选A） | 86 JVM＋151 Android，1141.713秒 | 232 PASS /5 FAIL，退出1 | JVM全通过；Android 146 PASS/5 FAIL：三项JPEG、旧IME页脚再次出现、封面空态标签在合并子Text导致原测试滚动找不到。不是最终成功。 |
| red-loading-and-repairs | 4 Android，114.070秒 | 3 PASS /1 FAIL，退出1 | 关闭IME后滚动的页脚及项目/订阅封面入口通过；新增64dp读取文本200%裁切测试失败。随后读取态改24dp原生进度与完整语义。 |
| **final-stage1-recheck（最终B）** | **86 JVM＋102 Android，490.816秒** | **185 PASS /3 FAIL /0跳过/0错误，退出1** | **86 JVM全部PASS；Android99 PASS/3项已知JPEG FAIL；全部35项新增PASS。** |
| final-artifact | 独立Lint＋assemble，6.643秒，日志6秒 | 退出0；BUILD SUCCESSFUL | 最终B相同174输入，包含UP-TO-DATE任务；不是重新执行全量测试。 |

最终B命令依次执行JVM `--rerun-tasks`，然后限定class的Android/Lint/assemble并用`--continue`让已知JPEG失败后仍完成其它适用任务。精确命令以`actualCommandExecutions`为准；summary旧`command`字段是组合任务说明，不能当实际一次执行记录。

候选A→B只改变4份输入：`CoverDisplay.kt`读取态、`ProjectDetailScreen.kt`空封面标签放到原可点击TextButton、旧`ProjectLifecycleScreensTest.kt`关闭IME/等待布局的驱动同步及新增读取态测试。没有修改生命周期生产表单或Nav3守卫。原资金/日期/状态断言与原超时保留，不删除失败测试、不吞异常；IME诊断截图和实际边界保存在[diagnostics](ui-c1/stage1/diagnostics)。旧测试其它适配只是通过真实新增菜单/详情更多入口点击、批准字号、以及把“不得伪造零总额”约束定位到概览，未削弱金额、事务或导航安全断言。

候选A的更广三VM/项目/订阅真实流程结果与最终B是**不同输入**。两阶段合并去重238个执行身份，取较晚结果为235 PASS/3 FAIL，仅表示覆盖历史；不能称一次最终238通过。源码692中454项未在这两次选中；**Task8全量692回归NOT_RUN且未获授权**。本阶段不借用历史652或549的PASS冒充本轮结果。

## 4. 三项M10失败完整保留

最终B原5项JPEG测试实际2 PASS/3 FAIL，失败为：

1. `com.cooper.lithic.data.media.M10JpegIntegrityTest#jpeg_entropy_truncated_with_eoi_is_rejected`
2. `com.cooper.lithic.data.media.M10JpegOldCoverProtectionTest#project_entropy_cut_jpeg_is_rejected_and_old_cover_and_money_are_preserved`
3. `com.cooper.lithic.data.media.M10JpegOldCoverProtectionTest#subscription_entropy_cut_jpeg_is_rejected_and_old_cover_payments_and_states_are_preserved`

生产Android仍错误接受保留EOI但缺损中段数据的JPEG。测试文件/拒绝断言/导入链路未改，没有以跳过、缓存或UI兜底掩盖失败。M10仍PENDING、Q05 USER_CONFIRMED；M11/M12完整往返仍NOT_RUN。UI视觉提交不是M10修复或正式验收回写。

## 5. 实际设备、截图与数据保护

- 只用已有`Lithic_M01_API_36` / `emulator-5596`，API36，1080×1920，正常density420（约411dp）/font1；压力density480（360dp）/font2。复用既有AVD数据目录，没有重建、wipe、卸载或改generation指针。
- 6张正常图、所有非空压力/菜单/滚动图来自**最终APK的MainActivity**，未改像素。沿用设备已有合成QA项目/订阅及蓝色测试封面，不创建新的活动QA行，也不把设计图照片/示例金额放入生产页面。
- 首页全空深浅两图由最终B新增仪器测试在**独立空Room测试宿主**运行真实LithicNavigation取得；未清除已有账本。不是最终MainActivity真实空账本截图。浅色测试宿主状态栏仍白色字形，不能作为MainActivity系统栏验证；六张正常图的系统栏来自真正MainActivity。
- 最终APK真实断网冷启：airplane=1、wifi=0，`am start -W`报告COLD/Status ok，TotalTime20273ms，实际读出4项目/4订阅并浏览三页。见[实际启动](ui-c1/stage1/manual-start.json)。此次慢速AVD的connectivity诊断两次超时，`network:[]`不证明defaultNetwork=none；原诊断[原文](ui-c1/stage1/offline-connectivity.txt)保留。没有性能达标或完整所有写入离线旅程声明。
- 主题/字体/density切换后原记录及route可读取；360dp/200%负金额完整，滚动后原日期/状态/金额/动作可达。普通首页FAB是浮层，可能覆盖当时一部分卡片，滚动后能完整阅读；实际效果交用户看图评审，不宣称像素级视觉已通过。
- 配置切换期间UiAutomator有null-root/超时；只接收新的唯一临时XML路径成功输出，过时共享XML不作为证据。没有为处理设备诊断而重建/清除AVD或改App。手工完整TalkBack、真实LMK/断电、真实手机/API26/OEM、Release仍NOT_RUN。

最终只读前后核对见[数据与设置证据](ui-c1/stage1/device-preservation.json)：

| 检查 | 实测结果 |
|---|---|
| 七表逐行所有字段、没有新活动QA行 | **完全相同**：类别2/项目4/资金7/媒体2/订阅4/续费6/状态5 |
| Room2/identity/integrity/FK | v2 / `a8fd277b0ef08ce9cf59b704734d0c25` / ok / 无违规 |
| active-generation/ready/settings、记录时区 | 与开始时相同，完整storage对象精确相同 |
| 两个原私有封面 | 原大小及SHA-256逐字节相同；未复制到公开证据 |
| 主题/字号/密度/旋转/网络/IME/自动时间时区 | 12项原设置精确复原；没有改宿主/其它设备 |
| AVD描述符/config | 开始/结束SHA相同 |

原DB/WAL/SHM、原私有图片与完整事实快照只保存在忽略的本地build中，提交证据仅含合成截图、计数、摘要及校验值。

## 6. 最终APK与工具链

- 路径：`E:/CODEX/.codex/worktrees/lithic-ui-c1/砺石新APP/build/ui-c1-stage1/lithic-ui-c1-stage1-debug.apk`
- 大小：**13,254,803 bytes**。
- SHA-256：**`f994d7820a1cb26ec0a11fe527b8e0befb3a8f1d415b62f776f915e20b067a78`**。
- 最终安装字节与上项相同；保留原M10交付APK，不公开推送APK。
- package `com.cooper.lithic.debug`、code8、`0.1.0-m10-debug`、min26/target36保持；debug v2签名成功且旧证书SHA256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`一致。
- 最终合并Manifest/APK仅自有signature receiver权限，无网络/相机/广泛存储；allowBackup=false、旧式及cloud/device-transfer九域排除核对完成。选定测试类/fixture/参考图不进入生产APK；已有Debug宿主声明未扩展，Release未构建。见[APK核对](ui-c1/stage1/apk/apk-verification.json)、[签名](ui-c1/stage1/apk/signature.txt)、[打包范围](ui-c1/stage1/apk/package-scope.json)。
- 实测JDK17.0.20.1+1、Gradle8.14.3、AGP8.13.2、Kotlin2.3.10、Compose BOM2025.11.01/UI1.9.5/Material3 1.4.0、Nav3 1.0.1、Room2.8.5；未升级。见[工具版本](ui-c1/stage1/tool-versions.json)。Lint **0 errors/23 warnings**，原提示未借本轮升级；原XML见[Lint结果](ui-c1/stage1/lint-results-debug.xml)。TypeScript/ESLint不适用原生工程。

## 7. 阶段1验收登记（不覆盖正式矩阵）

以下是实现者验证，**用户视觉确认仍PENDING**；不是独立接受。T编号沿用批准计划。

| 项 | 本阶段执行状态 | 证据/剩余边界 |
|---|---|---|
| T01 | PASS（样板功能）/视觉PENDING | 六图、系统主题、零事实写入；其它页面未统一。 |
| T02 | PASS | 真实计数/各≤3、空/错/历史仅存/未来预付/陈旧与交错测试。 |
| T03 | PASS（样板入口） | 关闭零写；原两创建流程在候选A定向执行，新空态测试最终B执行；表单统一未做。 |
| T04 | PASS（项目定向）/其余NOT_RUN | 原排序分页生产未改，窗口数据范围及项目流程；全量大数据性能不是本轮。 |
| T05 | PASS（展示范围） | 14项新增加载器/资源/缓存/大字体；JPEG完整性仍3 FAIL。 |
| T06 | PASS（定向范围） | 86 JVM及项目屏幕/生命周期/流程，旧计算与写入层无改；非692全量。 |
| T07 | NOT_RUN（未改版订阅） | 不以共享主题或部分旧流程冒充订阅样板完成。 |
| T08 | PASS（项目）/其余NOT_RUN | 详情更多原动作、原确认及压力图；其它页面Task6。 |
| T09 | PASS（定向安全回归）/未测场景NOT_RUN | 三VM守卫/Activity恢复在候选A；最终B变化相关屏幕重测。不是全站/真实LMK证明。 |
| T10 | PASS（样板） | 初始化/读取错/成功空/重试陈旧、真实空Room宿主。 |
| T11 | PASS（样板压力）/完整IME与全站NOT_RUN | 实际360dp200%首页/详情/菜单及长负金额组件测试；IME旧结束驱动实际修正并重测。 |
| T12 | PASS（已执行部分）/其余NOT_RUN | 标签/触控规格与自动布局、状态可见；完整TalkBack/焦点遍历/全部配对未完成。 |
| T13 | PASS（静态+冷启浏览）/完整离线写入旅程NOT_RUN | 最终APK权限/存储结构零改，实际airplane1/wifi0冷启三页；connectivity诊断超时不冒认none。 |
| T14 | **PENDING** | 6正常+2空态+压力图已交付，等用户看实际样板；不得进入Task6～8。 |

Q03/Q04/Q06既定期限、Q05 USER_CONFIRMED、M00～M09历史NOTES、M22大数据与M11/M12边界不变。测试总命令仍因三项JPEG退出1；没有新增未解决UI自动化失败。视觉风格及FAB覆盖、空态宿主系统栏等实际观察由图册明确呈现，不能自授视觉通过。

## 8. Git与停止

只在`feat/ui-c1-visual-redesign`独立worktree提交推送。来源代码`41596820145dc4a3fa6d041728a59540d828108a`，本轮文档批准起点`edc8be772a795f40b44f15da79ebf62a529bc074`；M10来源分支和main原SHA需在提交前后实查。提交包含本轮UI/适用测试/文档/合成证据，不含APK、数据库、原私人图片、签名私钥或凭据。最终SHA以Git提交及最终回执为准，避免文件自引用自身SHA。

**VISUAL_REVIEW_REQUIRED — UI-C1**。立即停止，等待用户确认实际三页样板；不自动进入Task6～Task8、修改M10或开始M11。
