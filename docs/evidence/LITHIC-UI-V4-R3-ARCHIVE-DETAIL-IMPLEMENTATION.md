# LITHIC V4-R3 · 项目与订阅档案页面

**状态 `PARTIAL / READY_FOR_INDEPENDENT_CODE_REVIEW`**
**核对日期 2026-11-24（北京时间）**

六个原生页面已按 R3 范围统一。桌面验证与 Debug APK 静态核验通过。原生运行、读屏、真机系统 200% 字号及截图仍为 `NOT_RUN`。

## Git 基线

| 目标 | 固定提交 |
| --- | --- |
| R2 生产与证据基线 | `f809899a35525d8fd6fe3571cc9863e1dfdc6d9b` |
| R0 设计基线 | `4a7e21b62103635c788363a874bbb24c736dab4e`，为 R2 祖先 |
| R3 源码 | `bf48c89d8cf0df2466bbad376b28d3cf8c78fd8a` |
| 起始远端 main | `145326ddd4ce28abbf7c2706fe25f9d38c599c3a` |
| 起始远端 Release | `87d423e7f21722eff851cac445b9ab798ab6996d` |

Fetch 后 R2 本地与远端指向同一提交，从该提交建立 `codex/lithic-ui-v4-r3-archive-detail-implementation`。用户回报 R2 小米14四项快速验收正常，沿用 `CODE_REVIEW_PASS / DEVICE_SMOKE_PASS / FULL_ANDROID_ACCEPTANCE_PENDING`，本报告的 Android 记录仅指 R3。

## 六页改动

| 阶段 | 源文件 | 展示改动与原合同 |
| --- | --- | --- |
| R3-A | [ProjectListScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectListScreen.kt) | 统一封面、名称、类别、状态、持有事实和当前日均成本。保留已有项目排序、顶部新增入口、数量语义与分页。 |
| R3-A | [SubscriptionListScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionListScreen.kt) | 延续订阅中、暂停、到期分组，每行保留最近真实付款日、折算时长及历史平均日成本，点击提交原订阅 ID。 |
| R3-B | [ProjectHistoryScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectHistoryScreen.kt) | 已结束状态、开始和结束日期、持有天数及观察截止日期同一行组。项目日均成本继续由原详情结果提供。 |
| R3-B | [SubscriptionHistoryScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionHistoryScreen.kt) | 已停止状态、折算付费时长与历史平均日成本采用同一套档案行。进入历史的资格继续由原查询与状态提供。 |
| R3-C | [ProjectDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt) | 保留项目身份、4:3 Fit 封面和验证后的放大、日均与累计投入、默认收起财务明细、最近三笔和管理操作。标题、段间、更多资料及主操作对齐 R0。 |
| R3-C | [SubscriptionDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt) | 统一标题、卡片轮廓和底栏。沿用服务身份、历史平均日成本、当前套餐、下一期已付未生效、最近三笔付款、继续加载、管理与合资格的续费。 |

新增纯展示 [ArchiveEntryRow.kt](../../app/src/main/java/com/cooper/lithic/feature/archive/ArchiveEntryRow.kt) 与 [ArchiveDetailFrame.kt](../../app/src/main/java/com/cooper/lithic/feature/archive/ArchiveDetailFrame.kt)。四个列表继续以记录 ID 作为 LazyColumn 键并用原滚动宿主，错误、旧快照、刷新、空记录和继续加载分支保留。档案行使用原生点击反馈，封面占 64×48dp。关联错误仍可在封面内查看，无引用走原类型占位。金额使用现有 MoneyMetric，宽度不足时下移，长金额允许原字号换行及查看完整值。

两类详情沿用原事件及付款顺序、展开键、列表和全部入口。项目金额查看、非现金折抵、收回、纠错、结束、更正结束、置换、以旧换新和危险操作进入原回调。订阅仍分别处理历史平均、当前价格、真实付款日、生效日、付款纠错或删除及暂停／停止资格。页面底部操作处在滚动容器之外。静态对照核查了 163 个旧测试标签调用中出现的不同字面标签（按页计），另逐项检查迁移到控件参数的展开标签及原回调。[保护路径及标签核对](lithic-ui-v4-r3-archive-detail-implementation/source-scope.json)

首页 B2、V4-A、R1、R2、Navigation 3、返回及写入守卫、业务算法、Room 和迁移、Repository、Backup format 4、Owner／generation、封面协议、主题持久化、全局颜色、正式签名及版本配置都在源码差异检查的保护范围内。

## 与 R0 的视觉对照

[档案四页三主题](lithic-ui-v4-r3-archive-detail-implementation/archive-three-themes-design.png) · [项目详情三主题](lithic-ui-v4-r3-archive-detail-implementation/project-three-themes-design.png) · [订阅详情三主题](lithic-ui-v4-r3-archive-detail-implementation/subscription-three-themes-design.png)

对照 R0 的 [list-editor.png](../design/lithic-ui-v4-r0/list-editor.png)、[project-themes.png](../design/lithic-ui-v4-r0/project-themes.png)、[V3 详情冻结](../design/lithic-ui-v3-detail-visual-refinement-02/DESIGN_FREEZE_03.md) 和 [TaoSheng v0.4.0 视觉映射](../design/lithic-ui-v4-r0/TAOSHENG_STYLE_MAPPING.md)。主列两侧 16dp、最大宽 560dp，详情主要区段间隔约 20dp，卡面 14dp 圆角配细边界。档案采用连续行与内缩分隔；金额在 360dp 设计参考下独立成行，宽度足够且金额能完整显示时可移至右侧。详情三主题均复用原背景、表面、成本强调和文字角色。

画板使用合成记录，**浏览器设计对照**包含 6 页 × 3 主题 × 3 宽度 × 2 档字号，共 108 个画框检查，0 页面脚本错误。浏览器宽度与字号为 CSS 模拟。原生 Compose 视觉等待端侧对照。[画板来源、布局记录和 SHA-256](lithic-ui-v4-r3-archive-detail-implementation/browser-reference.json)

## 测试与构建

Windows、Temurin JDK 17.0.20.1、Gradle Wrapper 8.14.3、Android SDK 36，采用离线单 worker。按 A→B→C 顺序验证，每阶段先核对源码差异再前进。

| 项目 | 结果 | 原始证据 |
| --- | --- | --- |
| R3-A 定向 JVM | **19 PASS** | [Gradle](lithic-ui-v4-r3-archive-detail-implementation/r3-a.log) · [测试 XML](lithic-ui-v4-r3-archive-detail-implementation/r3-a-xml/) |
| R3-B 定向 JVM | **36 PASS** | [Gradle](lithic-ui-v4-r3-archive-detail-implementation/r3-b.log) · [测试 XML](lithic-ui-v4-r3-archive-detail-implementation/r3-b-xml/) |
| R3-C 定向 JVM | **40 PASS** | [Gradle](lithic-ui-v4-r3-archive-detail-implementation/r3-c.log) · [测试 XML](lithic-ui-v4-r3-archive-detail-implementation/r3-c-xml/) |
| 最终完整 JVM | **990 PASS，116 个测试类，0 失败、0 跳过**。三个工作包合计覆盖 60 个不同的定向方法，本行已包含 | [执行日志](lithic-ui-v4-r3-archive-detail-implementation/desktop.log) · [逐套汇总及 XML 摘要哈希](lithic-ui-v4-r3-archive-detail-implementation/jvm-results.json) |
| 生产 Kotlin、AndroidTest 编译 | **PASS** | [桌面构建日志](lithic-ui-v4-r3-archive-detail-implementation/desktop.log) |
| Debug Lint | **0 错误、20 条既有警告，与 R2 逐项一致** | [Lint 原始报告](lithic-ui-v4-r3-archive-detail-implementation/lint-results-debug.txt) |
| Debug 构建 | **PASS** | [唯一 APK 构建日志](lithic-ui-v4-r3-archive-detail-implementation/assemble-debug.log) |
| AndroidTest 实机执行 | **0 / 148，NOT_RUN** | [按组与方法清单](lithic-ui-v4-r3-archive-detail-implementation/android-test-inventory.json) |

新增 Compose 方法覆盖四个档案页的原身份、日期、金额语义、点击和窄屏大字，以及详情居中最大宽度与操作区无叠盖。原封面测试增加四页 64×48dp 的可用和失败验证，保留原 Reader 身份及无引用零读取断言。独立只读代码复核指出金额语义文本与绘制细空格有差异，以及原新测试只命中纵排分支。分别校验两种文本，并纳入 320／360／400dp 和模拟 100%／200% 字号、横纵位置及 680dp 详情列边界。

## 单一 Debug 交付

| 项目 | 核验结果 |
| --- | --- |
| 本机文件 | `E:\CODEX\lithic-v4-r3-local\delivery\Lithic-V4-R3-Archive-bf48c89d-code17-debug.apk` |
| 包名和版本 | `com.cooper.lithic.debug`，`0.1.0-m22-debug`，versionCode 17 |
| APK SHA-256 | `f7caa42fe10f24ad2104e4ef21a43184ffb3a57de64f1b0aa6fd1e6d100e4e29` |
| 文件大小 | 20,705,569 字节 |
| Debug 证书 SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，与 R2 一致，v2 验签通过 |
| 内容 | Manifest 与 R2 字节一致，ZIP CRC 通过。DEX 检出 R3 展示组件和受保护页面标记。命名副本与原始产物 SHA-256 相同。 |

[APK 身份与对照](lithic-ui-v4-r3-archive-detail-implementation/apk-identity.json) · [签名原始回执](lithic-ui-v4-r3-archive-detail-implementation/apk-signature.txt) · [APK 元数据](lithic-ui-v4-r3-archive-detail-implementation/apk-badging.txt)

## Android 待验收

只读主机核查时没有运行中的隔离模拟器或预期监听，也没有已确认健康的设备。历史 ANR 环境未再启动。未调用 ADB、连接小米14或安装 APK。[环境记录](lithic-ui-v4-r3-archive-detail-implementation/android-host-check.txt)

1. 用合成数据在三主题下检查 320／360／400dp、系统 100%／200% 字号、长名称和极大金额，核对负值、真实零和未知值。
2. 按四个档案入口检查单条、跨 50 条分页、滚动位置、快速详情进出、空态、刷新、读取失败和旧快照。确认暂停及到期留在当前列表。
3. 检查无封面、加载、读取失败、横竖图以及项目详情 Fit 和关闭放大。核对读屏用语、日期、金额和触控区域。
4. 分别检查项目和订阅最近三笔、全部及收起、单条详情及原危险确认。检查返回来源、键盘可见时固定操作区和取消不写入。使用首页、悬浮导航及 R1／R2 页面作回归对照。

只推送 R3 独立分支。远端最终提交以推送后的 `git ls-remote` 核对为准。交付后等待独立验收。
