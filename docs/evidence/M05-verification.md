# M05 页面骨架与基础视觉系统 — 执行证据

记录日期：2026-10-12。当前 **READY_FOR_REVIEW — M05**，ChatGPT独立验收 **PENDING**。本报告与[机器可核对证据](M05-verification.json)登记已完成的实现、最终回归、设备和APK检查；不自行授予ACCEPTED / FROZEN。Git提交/推送将由主执行者按本轮授权交付，最终SHA以Git日志和用户报告为准。

仓库 `cc7279694-debug/property`；起点 `aecbe86fce6e5c4375f2c35d2df8539df0a557bb`，来源 `feat/m04-room-storage-foundation`，当前 `feat/m05-ui-foundation`。M00～M04独立冻结及历史NOTES继承；用户附件SHA-256 `419d5a963729d1c93f94aa935f2a189f455da0a1aa331de4e92119bc10968ece`，边界见[M05契约](../planning/M05_CONTRACT.md)。本地辅助审查不代替ChatGPT独立验收。

## 1. 三个主入口与真实页面层级

底部仅「现在｜回看｜设置」。现在显示当前日均成本的无值位置及“汇总尚未开放”，从真实快照区分暂无记录与真实项目数量；订阅说明尚未开放，schema v1并无订阅表。回看区分真实空/非空记录，复盘、档案、趋势、类别和年度仅为明确未开放的静态层级。设置只读显示实际记录时区与本地保存说明，提示Debug工程及完整备份尚未交付。

唯一二级页面为长期项目只读列表：名称、真实开始/结束日期与“已记录/未记录结束日期”的事实标签；有未来结束日期不会提前声称“已结束”。没有详情、编辑、新增或业务保存入口，不显示假金额、演示曲线或假备份/提醒开关。

四个可序列化NavKey只含路线身份。合法栈为 `[Home]`、`[Home,Review]`、`[Home,Settings]`、`[Home,ProjectList]`；点击Tab替换合法栈，重复点击不压栈。回看/设置及二级系统返回到现在，列表顶部返回到现在，现在系统返回退出Activity。保留旧Skeleton可解码，再安全归一到Home；未知/不合法的已解码栈同样归一。

## 2. 基础视觉与组件

- LithicTheme跟随系统明暗，低饱和中性色，38/24/16/14sp层级与4/8/12/16/24/32dp间距，触控区域至少48dp。
- AmountText输入BigDecimal?，null与真实0区分，普通正负金额HALF_UP两位、无Double或科学计数法；非零日均绝对值<0.01的主数字显示`<¥0.01`或`−<¥0.01`，配独立“元/天”单位及明确正负微值语义，不改数值或成本公式。
- EntryCard支持封面/占位、名称、调用方状态和时间文案、nullable日均；无回调时不伪造可点击行为，不生成生产样本。
- FormFields提供受控名称、金额、日期、标签/错误/焦点/IME及语义。保存/返回仅由AndroidTest专用合成表单宿主验证回调，不写Room或宣称M06业务保存。

保留原临时图标；没有正式Logo接入、主题商店、装饰性动画或完整设计系统框架。

## 3. 存储状态与真实读取

MainActivity经ViewModelProvider取得UI holder，并按Activity生命周期收集状态；旋转保留同一UI owner，Application的AppContainer仍为唯一持久存储所有者。

| 真实状态 | 实际UI行为 |
|---|---|
| Initializing，或快照/时区尚未读完 | 全页加载，不先显示空账本或业务底栏 |
| Ready且LedgerReadRepository.snapshot及SettingsStore.recordZone均成功 | Ready(snapshot, recordZone)，再显示正常导航与真实记录 |
| 初始化Failed，或Ready后真实数据库/设置读失败 | 保护性全页错误，无正常首页/空列表/零值；不显示绝对路径或堆栈 |

UI holder使用自己的ViewModel scope、collectLatest和IO读取；CancellationException继续传播。清理UI只取消读取任务，不关闭、重建或清空M04存储。页面不访问DAO或复制成本计算；错误页没有无效重试，生产退出回调实际调用Activity.finish。

## 4. M05-T01～T04实际执行结果

| 案例 | 实际结果 | 证据与适用边界 |
|---|---|---|
| M05-T01 only_three_primary_destinations | PASS | 三个准确标签与选中页面、重复点击有界栈、顶部返回、真实KEYCODE_BACK及实际边缘手势返回均已验证 |
| M05-T02 empty_screen_has_no_fake_metrics | PASS | 真Room默认两类别/无项目，真实合成项目与事件，初始化失败、真实关闭数据库/设置读取失败均区分；非空不会谎称暂无记录，汇总不冒充¥0.00 |
| M05-T03 large_text_has_no_blocked_action | PASS | 实际360px/density160即360dp、font_scale2.0，三入口/滚动/返回/长值/错误/触控及IME后保存/返回回调均通过；3项原UI方法在该配置下补跑PASS，键盘截图已查看 |
| M05-T04 back_stack_restores | PASS（限已执行场景） | NavKey保存/恢复、旧Skeleton/不合法栈、Activity recreate、物理旋转、实际UID SIGKILL后台进程死亡后Settings恢复、独立force-stop冷启Home均通过；真实低内存自动回收仍NOT_RUN |

ActivityScenario.recreate仅证明Activity重建；后台进程实际消失与force-stop冷启动分别记录。当前没有把合成表单回调视为M06保存，也没有把截图替代自动化或Room回归。

## 5. 360dp、200%字体、返回与进程证据

专用API36/Android16模拟器 `Lithic_M01_API_36`、`emulator-5596`，没有操作手机或其他项目AVD。最终 `build/m05-evidence/device-ui.json` 的安装、断网冷启、明暗/大字体三Tab、旋转、后台进程死亡、边缘手势及独立force-stop冷启均为PASS。实际覆盖360×800像素、density160（宽360dp）、font_scale2.0；浅/深色、三Tab、列表与设置横/竖屏有XML/PNG证据。

安装Success后，断网force-stop再启动为COLD/Status ok（TotalTime4084ms），作为冷启动证据。后台使用HOME并核对Launcher前台，`am kill`首次请求仍存活PID4568，**该尝试不算进程死亡**。随后通过应用UID的SIGKILL模拟实际后台进程死亡，PID4568确实消失；从既有任务恢复，产生新PID5103并回到Settings。该过程不是Activity recreate或force-stop，也不宣称覆盖真实内存压力下的系统自动回收。另行force-stop后的COLD/Status ok启动回到默认Home，单独登记。

旧环境 `hw.mainKeys=yes` 使EdgeBackGestureHandler未附着，先前未形成返回的滑动不算PASS。主执行者只将本项目私有AVD改为 `hw.mainKeys=no`，保留userdata后重启；最终已核对处理器启用/附着并验证真实边缘手势返回，随后完成软件导航环境下完整回归。该私有AVD配置有意保留为no；没有清除应用数据或修改其他项目AVD。网络、明暗、旋转、屏幕尺寸、density和font_scale恢复到原值；补测时IME显示设置1也已恢复为0，配置分别保留在 `device-ui.json` 与 `large-native-config.json`。

实际大字体补测重跑3个原方法：`UIComponentsTest.large_text_synthetic_form_keeps_errors_save_and_return_reachable_after_input`、`UIComponentsTest.large_text_cards_and_long_amounts_wrap_without_losing_the_numeric_value`、`NavigationShellTest.large_text_has_no_blocked_action`。用时51s，3/3 PASS，0失败/错误/跳过；运行配置为360×800、density160、font_scale2.0、IME显示设置1。`build/m05-evidence/large-native-native-0.xml` 与 `large-native-config.json`保留具体结果。截图像素证明软件键盘实际可见且保存按钮可达、错误可见；返回回调由测试断言验证，没有业务持久化。

9张PNG已纳入 `docs/evidence/m05-ui/`，主执行者已逐张查看实际像素，均为空账本或明确合成测试UI，无个人记录；每张大小/SHA-256/原始路径见验证JSON：

- [浅色首页](m05-ui/home-light-360dp-200.png)、[深色首页](m05-ui/home-dark-360dp-200.png)、[回看](m05-ui/review-360dp-200.png)、[设置](m05-ui/settings-360dp-200.png)。
- [长期项目列表](m05-ui/project-list-360dp-200.png)、[合成表单长错误](m05-ui/synthetic-form-error-360dp-200.png)、[合成表单键盘及保存](m05-ui/synthetic-form-keyboard-360dp-200.png)。
- [设置横屏](m05-ui/settings-landscape-200.png)、[后台进程死亡后恢复设置](m05-ui/settings-process-restored-200.png)。

## 6. JVM与Android实际回归及失败记录

实际执行的完整命令：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug --max-workers=2 --console=plain
```

两轮退出均为0：`green-hardware-nav.log`用时3m27s，JVM及全部Android测试实际执行；私有AVD改为软件导航后，`green-full.log`/`green-software-nav.log`用时3m54s，全部118项Android重新执行，JVM/构建任务为UP-TO-DATE，沿用未变化测试源码与此前真实执行的XML，不能冒称JVM又执行一次。`green-summary.json`、JVM XML与 `green-native-0.xml`记录 **82 JVM＋118 Android＝200个唯一测试方法，0失败/错误/跳过**，类/方法集合逐一与源码对应。额外3项大字体重复执行不计新增或增加唯一总数。Android仅专用Lithic_M01_API_36设备报告。原74/86源码及运行结果保留，新增8 JVM＋32 Android分开统计：

| 分类 | 继承 | 新增 | 本轮总数 |
|---|---:|---:|---:|
| JVM领域与金额展示 | 74 | AmountFormatter 8 | 82 |
| Android存储与UI | 86 | Components8 / Navigation15 / Storage7 / Activity2 | 118 |

真实RED为金额8项/7失败、首轮Android31项/30失败；已有正确行为直接回归。早期编译及测试宿主/runner错误不计行为RED。未来结束日期的新增真实断言1项失败后修正为“已记录结束日期”的事实标签，未更改领域日期规则。

第一次GREEN尝试117项/3失败伴随SystemUI ANR窗口干扰；第二次118项/1失败和孤立1项/1失败进一步定位到测试返回驱动/同步。该阶段输出保留，不宣称失败测试当时通过；通过Espresso同步真实KEYCODE_BACK后，相关3项独立3/3通过，再取得完整118项通过。未改生产Nav/dispatcher规避失败。真实状态补测中另外3个返回超时归驱动排查记录，不当作新的生产行为RED。

原输入与新增测试并非整个模块从第一轮起始终字节不变；增加必要边界、实际Theme宿主、截图及修正测试驱动均在证据中区分，没有删除失败断言或生产返回绕过。旧AVD手势未附着、ANR/重启和返回驱动失败尝试保留为本地诊断历史；最终软件导航完整回归与51s实际字体/IME补测均通过。日志/XML在忽略目录，不公开提交；本轮200项结果及来源哈希在验证JSON中可核对。

## 7. 构建、Lint、依赖、APK与数据兼容

Lint为0 errors/20 warnings：17版本更新提示（OldTargetApi1、AndroidGradlePluginVersion2、GradleDependency9、NewerVersionAvailable5）及3个旧M01占位字符串UnusedResources。保留50项原构建输入，不清理/抑制这些警告；NO-SOURCE不是测试通过。

冻结工具链继承，仅必要Lifecycle2.10.0与Compose测试1.9.5、Espresso3.7.0精确声明，见[工具链增量](../TOOLCHAIN_LOCK.md#m05-ui-与测试增量2026-10-12)。主运行报告 `build/m05-evidence/dependencies-main-final.log` 与M04对比121→122个唯一module，仅新增Debug `ui-test-manifest:1.9.5`，没有旧运行坐标提升或移除；Android测试报告 `build/m05-evidence/dependencies-final.log` 另记，Espresso仅用于测试。

| APK事实 | 已取得证据 |
|---|---|
| 绝对路径 | `E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk` |
| 大小 / SHA-256 | 11,837,748字节 / `c70b89fd82b9480bbad8d1e0f06805317f6d6c002637583f5299b0fb769b790e` |
| 应用身份 | `com.cooper.lithic.debug`，versionCode3，`0.1.0-m05-debug` |
| 最终包检查 | APK/合并Manifest/备份九域排除/Wrapper及18个生产描述符检查PASS |
| 权限 | 仅原签名级内部接收器权限，无新增INTERNET或其他平台权限 |
| 签名 | apksigner实际验证v2通过；本地Android Debug证书，非生产发布 |
| 测试隔离 | JUnit/AndroidX Test框架和合成表单未打包；Debug额外空ComponentActivity宿主单独登记 |

Debug清单含主 `com.cooper.lithic.app.MainActivity` 与 `androidx.activity.ComponentActivity`；后者exported=true、非launcher，由debugImplementation测试manifest提供。不能说最终包与M04单Activity清单完全相同；Release排除只是配置结论，Release APK未构建。APK事实来自 `build/m05-evidence/apk/apk-verification.json`、`extra-verification.json`及`signature.txt`。

Room schema仍v1四表，SHA-256 `351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b`；50项M04冻结输入逐项一致，未新增schema/迁移或备份格式。既有存储回归验证owner/事务/时区等行为；设备全部页面/重启/进程流程后的activeId仍为 `2d6c9460-a2f3-4976-ba04-781c416a0b52`，ready不变，设置SHA-256仍为 `fff800a6a09afbc2e6656c8d67b512b74ba8e1693666e32b96701c7dfcf06c44`，没有切换同代身份或覆盖记录时区。已安装APK SHA-256与上表本地APK相同，来源是实际设备读取与 `device-ui.json`，不以源文件哈希替代设备验证。

## 8. 完整修改文件

以下清单已与当前Git差异核对：14个修改、29个新增，共43个文件，0删除；包含最终JSON、9张经查看的截图，以及主执行者已补齐的CURRENT_STATE/ACCEPTANCE_MATRIX/M05 checkpoint。提交前由主执行者再次核对最终范围。忽略目录的日志、APK、数据库、设备数据及辅助脚本不纳入Git。

修改（14）：

- AGENTS.md
- README.md
- app/build.gradle.kts
- app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt
- app/src/main/java/com/cooper/lithic/app/MainActivity.kt
- docs/ACCEPTANCE_MATRIX.md
- docs/ARCHITECTURE.md
- docs/CURRENT_STATE.md
- docs/DECISIONS.md
- docs/EXECUTION_PROTOCOL.md
- docs/ROADMAP.md
- docs/SPEC_ISSUES.md
- docs/TOOLCHAIN_LOCK.md
- gradle/libs.versions.toml

新增（29）：

- app/src/androidTest/java/com/cooper/lithic/ui/ActivityNavigationTest.kt
- app/src/androidTest/java/com/cooper/lithic/ui/NavigationShellTest.kt
- app/src/androidTest/java/com/cooper/lithic/ui/StorageUiStateTest.kt
- app/src/androidTest/java/com/cooper/lithic/ui/UIComponentsTest.kt
- app/src/main/java/com/cooper/lithic/app/LedgerUiState.kt
- app/src/main/java/com/cooper/lithic/app/LedgerUiStateHolder.kt
- app/src/main/java/com/cooper/lithic/app/LithicApp.kt
- app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt
- app/src/main/java/com/cooper/lithic/core/design/AmountText.kt
- app/src/main/java/com/cooper/lithic/core/design/EntryCard.kt
- app/src/main/java/com/cooper/lithic/core/design/FormFields.kt
- app/src/main/java/com/cooper/lithic/core/design/LithicTheme.kt
- app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt
- app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt
- app/src/main/java/com/cooper/lithic/feature/settings/SettingsShell.kt
- app/src/test/java/com/cooper/lithic/core/design/AmountFormatterTest.kt
- docs/checkpoints/2026-10-12-M05.md
- docs/planning/M05_CONTRACT.md
- docs/evidence/M05-verification.json
- docs/evidence/M05-verification.md
- docs/evidence/m05-ui/home-dark-360dp-200.png
- docs/evidence/m05-ui/home-light-360dp-200.png
- docs/evidence/m05-ui/project-list-360dp-200.png
- docs/evidence/m05-ui/review-360dp-200.png
- docs/evidence/m05-ui/settings-360dp-200.png
- docs/evidence/m05-ui/settings-landscape-200.png
- docs/evidence/m05-ui/settings-process-restored-200.png
- docs/evidence/m05-ui/synthetic-form-error-360dp-200.png
- docs/evidence/m05-ui/synthetic-form-keyboard-360dp-200.png

## 9. 未执行、风险与保留问题

- 实现及本轮适用验证已达到READY_FOR_REVIEW；ChatGPT独立验收仍PENDING，Git提交/推送由主执行者完成并报告。旧环境手势/ANR/驱动失败保留诊断记录，最终适用手势与回归已通过，不自行授予冻结。
- 真机、API26、OEM行为、TalkBack完整审查、真实内存压力回收、Release实际构建/正式签名升级仍NOT_RUN。
- M06 CRUD、订阅业务表/付款、M13首页汇总、图表/排行榜、正式备份/恢复等未实施；只读快照目前不提供M06写后自动刷新闭环。
- 原ZIP/43规划、130唯一案例仍完整原样，原130及16组完整跨模块补测仍NOT_RUN；历史交付/JSON/checkpoint不改写。Q03长期节点M21前、Q04提醒资格M20前、Q05图片M10/M12前，Q06仍ENGINEERING_REQUIRED，完整切换/回退/杀进程/共同校验/清理仍M12；M09两项流程风险保持NOT_RUN。
- M04首次初始化残留代仍须安全处理；M08迁移须同步版本预检；Debug空测试宿主为阶段性测试配置。没有私钥、真实个人数据、APK或忽略目录日志的公开推送。

## 10. Git与交付边界

本报告写入时HEAD仍为起点 `aecbe86fce6e5c4375f2c35d2df8539df0a557bb`，分支 `feat/m05-ui-foundation`；本轮尚未提交/推送。已授权将本轮实现与收尾证据提交并推送当前功能分支；最终完整差异/链接/敏感信息审核、提交SHA、远端匹配及工作区状态由主执行者统一核对，并以最终Git日志和用户报告为准，避免提交哈希自引用。

实际提交前范围检查9项全PASS：14修改/29新增/0删除，124项未改基线文件逐项核对，ZIP CRC/安全路径/43原件/130 NOT_RUN、50冻结输入和旧74/86源码、Q03～Q06四行与M09两风险正文均一致；74份Markdown、393个本地文件链接及4个当前文档段落锚点无失效，围栏平衡、差异格式通过。34份文本敏感模式扫描0命中，9张已查看PNG的CRC/尺寸/无额外文本元数据核对通过，没有APK、库、私钥或个人记录纳入范围。70最终构建输入与APK哈希再次核对，结果见JSON的documentAndScopeChecks；这些来源/范围核对不替代运行证据或独立验收。

只提交/推送M05功能分支，不合并main、不force push、不修改WHOCARE或公开发布APK。本报告的READY_FOR_REVIEW不代替独立验收，也不声称写入时Git交付已完成。

## 11. M06前置条件与停止

M04已独立冻结；M05须完成Git交付并取得ChatGPT独立验收，再由用户明确授权M06。后续继承统一Theme/金额展示、合法NavKey、真实UI状态、既有Repository事务与固定记录时区；不复制成本公式、不把错误当空账本。M06还需实际新增项目表单/事务保存、失败保留输入、重复提交保护与写后刷新验证；本轮合成表单回调不是这些前置功能已通过。

当前 **READY_FOR_REVIEW — M05**，独立验收 **PENDING**。本轮交付后停止等待独立验收，不进入M06或自行授予ACCEPTED / FROZEN。
