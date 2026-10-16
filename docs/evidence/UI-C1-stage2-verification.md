# UI-C1 阶段2 — Task6～Task8验证与交付

日期：2026-10-16。本轮Task6～Task8已实施并完成下述实际验证，停止在 **READY_FOR_REVIEW — UI-C1**。本地实现、自动化结果、实现者看图与用户/ChatGPT独立验收分别登记，后两者不能互相替代。

| 事项 | 当前结论 |
|---|---|
| Task6、Task7展示层实施 | 已实施；实际范围及阶段验证见下文 |
| Task8最终完整自动化回归 | **FAIL：718方法，715 PASS／3原JPEG FAIL／0 SKIPPED** |
| 最终手工／视觉／数据保护收口 | 已完成下述原图/原生操作及七表、媒体、设置的最终核对 |
| UI-C1交付状态及独立验收 | **READY_FOR_REVIEW；独立验收PENDING，未自授接受/冻结** |
| M10正式验收 | **PENDING**；本轮没有修复或接受JPEG缺陷 |

## 1. 授权、来源与最终输入

[本轮用户原指令](../design/ui-c1/source-stage2/USER_INSTRUCTIONS.md)授权Task6～Task8。阶段1精修27图已由用户实际确认，ChatGPT视觉复审为PASS WITH NOTES；这是本轮实施的视觉基线，不是最终UI或M10接受。

| 身份 | 实际记录 |
|---|---|
| 唯一分支 | `feat/ui-c1-visual-redesign` |
| 工作目录 | `E:/CODEX/.codex/worktrees/lithic-ui-c1/砺石新APP` |
| 用户确认的来源SHA | `f000639c62fce71be0a9240425c8e27b1a5ed974` |
| Task6提交 | `e9bb44f4c77a2cacd16c1f4910e6512f56c6327c`；Task7开始前已单独提交 |
| Task7／首轮完整回归HEAD | `45e91163b07bcfe41927310ef37dabe7f003cdfd` |
| 最终产品／测试输入冻结HEAD | `6a25965225ace3212d1653d15b45fc56ef87411e` |
| 最终输入清单 | **187项**；`sourceInputsSha256=6e10034a61e9ffc666be545645b04ea5a13bc022aa510cb5dde97694485a4f2f` |
| 最终输入ZIP | [stage2-final-restored-full-source.zip](ui-c1/stage2/task8/stage2-final-restored-full-source.zip)；SHA-256 `296a8942079262cea9b614136df488facdbab4bab38418deba6abfd855383dcd` |
| 最终交付提交／远端一致性 | 证据提交在最终冻结HEAD之后；最终交付SHA与远端一致性以最后Git回执/答复为准，避免本文递归自引用。仅UI分支，后续证据提交不改变测试输入。 |

最终三条正式命令的输入稳定标记均为true。完整[运行摘要](ui-c1/stage2/task8/stage2-final-restored-full-summary.json)、XML、日志、输入ZIP及[逐方法对照](ui-c1/stage2/task8/final-test-crosswalk.md)共同构成证据，不能只凭方法数量或构建成功验收。

## 2. Task6／Task7实际实施

Task6统一订阅当前列表、详情、停止历史、项目历史、回看和设置。订阅保留订阅中／暂停中／已到期分组、真实付款排序、分页、重试及陈旧数据；详情分开历史平均、当前标准日均价、折算付费时长和实际已过去付费有效天数。未来预付不替代当前套餐；原付款、状态、计划纠错及重新开通回调保留。回看仅展示两个真实历史入口；设置读取实际安装版本和记录时区，保留完整备份恢复未完成、重要记录不能仅存本App的风险说明。

Task7先[枚举真实表单及弹窗](ui-c1/stage2/task7/form-inventory.md)，再统一8个复用表单实现、金额／日期／类别输入、封面操作、危险确认、离开及待核对对话框。原始raw值、零元适用场景、四个付款日期、显式付款确认、失败保留输入、成功后离开、固定操作ID及dirty／IN_FLIGHT／UNCONFIRMED／COMMITTED业务守卫保留。新增FormHeader只接原受保护返回回调；未引入另一套业务状态。

完整新增／修改／删除清单见[实际文件清单](ui-c1/stage2/FILES.md)，包括测试、配置、文档与原始证据。

相对来源共19个生产Kotlin文件发生展示／装配变化：

| 文件组（路径前缀为 `app/src/main/java/com/cooper/lithic/`） | 文件与影响 |
|---|---|
| UI装配，1个 | `app/LithicNavigation.kt`：3处现有媒体facade注入及既有对话框样式／滚动／触控展示；原守卫和回调不替换 |
| 共用展示，2个 | `core/design/FormFields.kt`、新增`FormPresentation.kt`：统一输入、标题与表面样式 |
| 项目，8个 | `CoverPicker.kt`、`DeleteProjectDialog.kt`、`EndProjectDialog.kt`、`MoneyEventCorrectionScreen.kt`、`MoneyEventEditor.kt`、`ProjectEditorScreen.kt`、`ProjectHistoryScreen.kt`、`ProjectInputs.kt`（均在`feature/project/`）：原表单／确认及历史展示一致化 |
| 订阅，6个 | `RenewalEditorScreen.kt`、`SubscriptionDetailScreen.kt`、`SubscriptionEditorScreen.kt`、`SubscriptionHistoryScreen.kt`、`SubscriptionListScreen.kt`、`SubscriptionStateDialog.kt`（均在`feature/subscription/`）：真实指标、列表、付款及状态操作展示一致化 |
| 回看／设置，2个 | `feature/review/ReviewShell.kt`、`feature/settings/SettingsShell.kt`：真实入口和只读信息分组 |

阶段1已确认的首页、项目列表、项目详情、共用主题与CoverPreviewLoader没有源码改动。实现者已直接对照两设计原图检查本轮68张原图；独立视觉接受仍PENDING，详见[逐图检查](ui-c1/stage2/VISUAL_INSPECTION.md)。

[Task6阶段报告](UI-C1-task6-verification.md)保留真实RED、设备连接阻塞、65方法中的2个状态语义失败及修复后14 PASS／字号精修6 PASS。它们是不同运行和输入，不合并成一次全量绿色结果。[Task7阶段报告](ui-c1/stage2/task7/verification.md)保留资料分类错误与有效视觉RED的区别、68方法67 PASS／1新增短标题期望FAIL及独立7 PASS复核；仅修正新增短标题强制换行期望，长标题换行及原断言保留。最终结果以下面的新完整回归为准，旧失败不改写。

## 3. 完整回归、缓存与两轮输入

最终根目录实际执行如下。三条正式命令均带 `--max-workers=2 --console=plain`；Android全量不加class／method过滤。

| 实际命令 | 退出码 | 实际耗时 | Gradle任务执行／缓存状态 | 原日志 |
|---|---:|---:|---|---|
| `.\gradlew.bat :app:cleanTestDebugUnitTest --max-workers=2 --console=plain` | 0 | 6.860秒 | 1任务实际执行；只清可重建JVM测试输出，不清App或设备数据 | [jvm-clean.log](ui-c1/stage2/task8/stage2-final-restored-full-jvm-clean.log) |
| `.\gradlew.bat :app:testDebugUnitTest --max-workers=2 --console=plain` | 0 | 10.446秒 | 26 actionable：2 executed／24 up-to-date；test任务实际执行 | [full-0.log](ui-c1/stage2/task8/stage2-final-restored-full-0.log) |
| `.\gradlew.bat :app:connectedDebugAndroidTest --max-workers=2 --console=plain` | 1 | 1054.941秒 | 71 actionable：1 executed／70 up-to-date；完整设备测试实际执行并有3项失败 | [full-1.log](ui-c1/stage2/task8/stage2-final-restored-full-1.log) |
| `.\gradlew.bat :app:lintDebug :app:assembleDebug --max-workers=2 --console=plain` | 0 | 64.956秒 | 49 actionable：3 executed／46 up-to-date；assembleDebug为UP-TO-DATE，不称所有构建任务重跑 | [full-2.log](ui-c1/stage2/task8/stage2-final-restored-full-2.log) |

8份新鲜JUnit XML逐方法与summary一致：**86 JVM PASS；632 Android＝629 PASS／3 FAIL；合计718唯一方法，715 PASS／3 FAIL／0 SKIPPED，整体FAIL**。原701身份全在本次XML中，实际698 PASS／3 FAIL；新增17个Android方法全部PASS，完整身份及具体断言范围见[逐方法对照](ui-c1/stage2/task8/final-test-crosswalk.md)。P（Compose给定状态／回调）、R（隔离Room／Repository／VM）、J（JVM）、A（实际Activity）、L2（宿主约束字体2）及V（截图producer）证据不能互相冒充；自动化PASS不直接授予整项T验收。

首轮完整回归为**718＝693 PASS／25 FAIL**，输入指纹为 `e0adab7c1081894843db017e900f93839c6bdfe42f04cd7407431c2b0c490a15`。其中22项在`V1DatabaseFixture`原SHA保护检查处因Windows检出将冻结v1资源转成CRLF而失败，另3项是原JPEG失败；不是22个已独立定位的生产迁移错误。[原首轮summary／XML／日志／ZIP](ui-c1/stage2/task8/first-full/stage2-final-full-summary.json)完整保留。

首轮结束后只恢复schema v1／v2的**原Git blob字节**，追加精确的 `app/schemas/** -text` 防检出转换规则；没有改JSON对象、SQL、版本、identity、fixture期望哈希或断言。冻结新输入后从头执行上表三条完整正式命令，未拼接首轮成功结果；原22项在新完整结果中逐身份全部PASS。[原字节恢复记录](ui-c1/stage2/schema-integrity/schema-raw-restoration.json)、[新静态审计](ui-c1/stage2/task8/stage2-final-restored-static.json)和最终XML分别证明恢复、保护范围与实际结果。

## 4. 保留的失败与风险

最终仅以下3项FAIL，全部属于原701身份；API36生产导入链错误接受保留EOI但熵编码数据截断的JPEG。本轮未改图片链路、fixture或拒绝期望，未引入JNI／NDK或修JPEG：

- `com.cooper.lithic.data.media.M10JpegIntegrityTest#jpeg_entropy_truncated_with_eoi_is_rejected`
- `com.cooper.lithic.data.media.M10JpegOldCoverProtectionTest#project_entropy_cut_jpeg_is_rejected_and_old_cover_and_money_are_preserved`
- `com.cooper.lithic.data.media.M10JpegOldCoverProtectionTest#subscription_entropy_cut_jpeg_is_rejected_and_old_cover_payments_and_states_are_preserved`

三项停在拒绝／INVALID_IMAGE断言；不能依据方法名宣称后续旧图、金额、付款和状态保护断言已经执行通过。图片显示正常或有效JPEG导入通过不代表JPEG完整性通过。整体FAIL和M10独立PENDING继续保留。

旧方法`UIComponentsTest#large_text_synthetic_form_keeps_errors_save_and_return_reachable_after_input`本次PASS（最终XML第684行），但历史保存10秒偶发超时仍为 **INTERMITTENT / ROOT_CAUSE_UNKNOWN**。[专项诊断](ui-c1/stage2/task7/old-save-diagnostic.md)区分同步合成回调的时序观测与独立测得的测试宿主Save越过系统安全区；仅测试宿主补safeDrawing，原10,000ms、raw／错误／恰好一次保存／返回断言保留。新增观测会改变时序，复跑PASS不能关闭原根因。历史Digital Wellbeing外部ANR保持独立诊断；合成测试也不能证明真实业务落库或可见native IME。

[工具诊断](ui-c1/stage2/task8/tooling-diagnostics.md)另列APK helper将`minSdkVersion`误识别为`sdkVersion`、UIAutomator空根及数字键盘旧阈值驱动错误；修正只涉及忽略目录的工具，未改产品输入、APK、测试超时或验收阈值。它们不算第二次产品修复，也不关闭历史保存风险；最终手工实际结果见[原生操作记录](ui-c1/stage2/task8/runtime-observations.md)。

## 5. 保护范围与数据层

[最终本地范围审查](ui-c1/stage2/task8/final-local-scope-review.md)是静态辅助审查，不是ChatGPT独立验收：66项选定保护文件Git对象无变，原701方法身份及原断言保持；导航guard／click／dismiss／恢复目标token序列保持。Room实体／DAO／迁移／v2版本、金额与BusinessClock／领域引擎、Repository事务、owner／coordinator／generation、媒体导入／清理协议、三VM／SavedState信封、身份／权限／依赖／工具链、正式矩阵及历史原130案例／16组边界均未据本轮UI改变。

schema v1原字节为10,840字节、SHA-256 `351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b`；v2为21,042字节、SHA-256 `ceb49b458c4c5baa08066586f209f96842935f5456a0a53a01220f6d958f7131`。baseline＝HEAD＝工作区原字节；fixture原常量未变。`.gitattributes`保护规则及审计修正不等于schema变更。此前只看规范化Git差异的静态PASS不能追认为已检查原资源字节。

SQLite／Migration／Repository／真实文件协议／DataStore／Backup：无新增业务实现或数据模型变化；没有完整备份恢复、代切换或M11功能。Q05仍USER_CONFIRMED，M11／M12完整往返仍NOT_RUN。**最终原七表30行、全部字段与集合精确不变；两原封面字节、GMT记录时区、generation/active pointer/ready/settings均不变，SQL完整性ok、FK为空，13项设备设置完整恢复。**见[真实设备保护核对](ui-c1/stage2/device-preservation.json)，原DB/WAL/私有图片保持本地忽略。

## 6. 最终APK

据最终APK检查记录，独立产物未覆盖原M10／阶段1APK；原生产依赖与权限保持，没有将测试类、合成资料或参考图打入生产DEX／资产。原权限、身份、签名、备份资源检查日志在[APK证据目录](ui-c1/stage2/task8/apk/)。

| 项目 | 实际记录 |
|---|---|
| 路径 | `E:/CODEX/.codex/worktrees/lithic-ui-c1/砺石新APP/build/ui-c1-stage2/lithic-ui-c1-stage2-debug.apk` |
| 大小 | **12,982,546字节** |
| SHA-256 | `4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46` |
| applicationId／版本 | `com.cooper.lithic.debug`；versionCode8；`0.1.0-m10-debug` |
| SDK／业务schema | min26／target36；Room schema v2，无版本或迁移变化 |
| 签名证书SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；原Debug证书兼容 |
| 权限 | 仅原AndroidX内部签名级`com.cooper.lithic.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`；无新增网络／相机／广泛存储／通知权限 |
| 已安装包证据 | 检查JSON登记installedApkSha256与上述相同；全部手工操作后的最终保护核对再次确认同一安装APK SHA |

APK检查退出0不证明设备数据未变、OEM备份行为或Release质量，也不覆盖三项JPEG FAIL。

## 7. 最终原图、真实操作与T01～T14

[最终原图画廊](ui-c1/stage2/FINAL_GALLERY.md)首页直接列出8页×Light/Dark的16张正常MainActivity原图；随后36张Main附图、14张隔离订阅/历史/错误图、2张真空Room图，共68张。正常411.43dp/字体1.0，真实压力360dp/系统字体2.0；实际API36/设备/主题/前台/PNG与APK的SHA逐图登记。[原始来源JSON](ui-c1/stage2/task8/screenshot-provenance/registry.json)也以原字节归档。

实现者实际用view_image读取原PNG并对照两参考图，见[检查说明](ui-c1/stage2/VISUAL_INSPECTION.md)；隔离图片复制字节相同，不拿它冒充MainActivity。裸展示错误宿主的系统栏限制、native-submit图未直接显示精度错误等均明确说明。[原生操作](ui-c1/stage2/task8/runtime-observations.md)保留键盘、invalid raw、原Back/旋转/切主题/危险取消及离线边界。Gboard节点未在UIA暴露；键盘由原像素及实际visible IME frame交叉确认。

下表为**实现者实际结果登记**，非用户/ChatGPT接受。PASS仅指该行写明的实际范围；NOTES/NOT_RUN不被整体覆盖。完整代表方法、类型和精确XML行号见[断言对照](ui-c1/stage2/task8/final-test-crosswalk.md)，其53次引用/47个唯一方法均实际PASS，不额外增加718总数。

| 编号 | 实际结果与证据 | 保留边界 |
|---|---|---|
| T01 | PASS：16系统深浅Main图；切主题/原生草稿后七表/媒体/设置精确不变；主题配对自动化PASS | 显式Compose主题的隔离图不证明系统栏行为 |
| T02 | PASS：Main真实4/4历史口径；空Room双主题唯一首次新增；有界预览/历史唯一/单类型为空原测试PASS | 没有M13总成本或伪造数量 |
| T03 | PASS：两原创建回调/关闭零回调及空Room打开关闭零写；Main两原创建路径打开取消，最终默认30行不变 | 主账本未保存真实新增；写入由隔离真实Room流程验证 |
| T04 | PASS：SQL稳定分页/付款日期/三管理分组及历史原测试；两类型Main当前历史截图 | 不是长期性能基准；无假搜索筛选 |
| T05 | PASS WITH NOTES：有效/未设/缺/坏封面、并发2/采样/LRU/代隔离与零写原方法PASS；错误原图单列 | JPEG输入完整性三FAIL不被覆盖；错误图是受控展示注入 |
| T06 | PASS：真实项目创建/补录/结束后纠错/退款/固定身份原Room与领域测试PASS；Main已结束纠错只读打开返回 | 本轮不手改默认账本金钱；既有删除/空间/缺失语义限制保留 |
| T07 | PASS：30/90/365、预付/elapsed/暂停到期停止等原引擎及Room流程PASS；标准730与elapsed288的隔离例，当前套餐另列 | 隔离观察日10/15与Main10/16来源明确，不将购买时长当使用时长 |
| T08 | PASS WITH NOTES：原操作及真实Room付款/状态/纠错/重开/删除守卫方法PASS；实际打开取消项目删除/结束/封面、续费/停止、结束历史纠错 | 未原生手动成功写入每一业务模式；操作清单与准确断言范围另列 |
| T09 | PASS WITH NOTES：三VM原始信封/Back/隐藏pending/同ID重放/COMMITTED/恢复原测试PASS；native项目Back/旋转/Keep/Discard实测 | 多pending按钮200%组合未单独原生演示；非真实LMK/断电恢复证明 |
| T10 | PASS：原初始化/空/读取错/旧数据断言；首次未知数量与陈旧图不同，错误不伪装空账本 | 展示注入错误图不是本轮真实磁盘故障 |
| T11 | PASS WITH NOTES：原生360dp/系统2.0、长QA名/负金额、真实字母与数字IME、1.234错误/Raw/Back/旋转；新4表单L2与原压力方法PASS | 没有全原生模式×状态×主题穷举；详细样本/未测见原生记录 |
| T12 | PASS WITH NOTES：48dp目标、配对对比度、语义名称/焦点/错误及新几何测试PASS；原图无新增遮盖卡片 | 完整TalkBack/所有辅助技术与平台NOT_RUN |
| T13 | PASS：最终APK无网络等新增权限、结构/依赖不变；实际Active default network:none时冷启/浏览/原始校验/返回，完整隔离Room操作也在离线状态 | 无真机/OEM/Release或完整备份恢复承诺 |
| T14 | READY_FOR_REVIEW：已确认阶段1后顺序执行Task6提交→Task7→最终完整输入/16主图＋52附图与来源登记 | 最终用户/ChatGPT视觉/代码独立验收PENDING；不自授接受 |

## 7.1 工具链及产物补核

实际Java17.0.20.1+1、ADB1.0.41/37.0.1-15733141、aapt2 2.20-13193326输出已保留；冻结Gradle wrapper8.14.3、AGP8.13.2、Kotlin2.3.10、Compose BOM2025.11.01、Compose UI1.9.5、Navigation3 1.0.1、Room2.8.5及其余配置无升级。Lint现有报告0errors/23warnings，最终命令成功但部分任务UP-TO-DATE如前表。

对最终APK另外扫描150个源码测试类型/Kt前缀及嵌套前缀，均未进入生产DEX；新合成名称/资料与原JPEG测试资产不在生产包。测试APK中的两schema资产与冻结原Git blob逐字节相同，详见[类与资源清单](ui-c1/stage2/task8/apk/class-and-resource-inventory.json)。原AndroidX Debug空ComponentActivity宿主依赖仍保留，不能当作Release正式交付验证。

## 8. 尚未执行与停止边界

真机、API26、OEM、Release、完整TalkBack、真实LMK、断电及M11／M12完整备份恢复往返继续 **NOT_RUN**。Activity重建、保存任务恢复、force-stop、SIGKILL和真实LMK不得互换；本轮未跑的具体操作或组合应逐项保留缺口。

最终完整回归、原图/手工T表与数据设置保护核对已收口；未发现本轮三JPEG之外的新最终测试失败。停止 `READY_FOR_REVIEW — UI-C1`，等待用户与ChatGPT独立验收；整体FAIL、旧超时根因未知、原平台NOT_RUN和M10 PENDING继续保留。最终提交/推送及工作区状态由本轮最后Git回执确认，不把源码冻结HEAD冒充证据交付提交。

只推授权UI分支；不合并main，不操作M10分支或WHOCARE，不force push、不公开发布APK，不修JPEG、不进入M11。交付后停止，等待用户与ChatGPT独立验收。
