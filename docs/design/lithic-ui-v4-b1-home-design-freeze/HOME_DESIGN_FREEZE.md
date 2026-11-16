# 砺石 V4 首页设计冻结候选

**等待独立设计评审和用户确认。** 本目录是使用合成数据的浏览器设计稿。尺寸按 Android 的 dp / sp 描述，浏览器以 CSS 像素模拟。Android 布局、系统字号、输入法、返回手势及 TalkBack 本轮均为 `NOT_RUN`。

[交互样稿](prototype.html) · [视觉评审](VISUAL_REVIEW.md)

| 画板 | 内容 |
| --- | --- |
| [纯白](home-white.png) · [岩白雾蓝](home-stone-mist.png) · [深色黑灰](home-charcoal.png) | 同一账本、相同排版、相同封面和金额 |
| [记录卡片](home-record-cards.png) | 项目与订阅各自的有图、无图表达 |
| [关键状态](home-states.png) | 正常、确认空账本、无当前有效项目、账本读取失败、上次结果及两类预览失败 |
| [宽度适配](home-responsive.png) | 320 / 360 / 400dp 下的双列、滚动区和底部操作 |
| [模拟 200% 字体](home-font-scale-200.png) | 长名称、金额、操作目标和固定底部区域 |

生产对照固定在 `b7ca3ea742fcf960fe36eae94a06d4a418a5682a`。V4 视觉基线为 `af612160efca7b08dc3f071ab7fa3bc7cbada32e`。保留 A 阶段已经由用户在小米14体验确认的按钮形态和按压修正。本阶段只冻结首页的视觉候选。 [V4 设计基线](https://github.com/cc7279694-debug/property/tree/af612160efca7b08dc3f071ab7fa3bc7cbada32e/docs/design/lithic-ui-v4-taosheng-style-port-01) · [导航修复源码与报告](https://github.com/cc7279694-debug/property/tree/b7ca3ea742fcf960fe36eae94a06d4a418a5682a)

## 四个区域

| 区域 | 布局规则 | 操作和语义 |
| --- | --- | --- |
| 品牌 | 左对齐砺石黑石标记和文字，右侧搜索、加号，按同一中心线。字标约 22sp，图标约 24dp。 | 搜索沿用已有能力门禁。搜索和顶部加号各有至少 48×48dp 触控区和独立名称。删去额外品牌说明。 |
| 账本总览 | 让两个数字直接落在页面表面。标准字号并排，200% 或空间不足时顺序纵排。数字约 26sp。底部以一道细线分出两个记录数和「成本构成」。 | 「总投入」按人民币金额显示。「日均成本」自带时间口径，视觉可省略重复单位，完整的人民币／天在无障碍名称、完整金额和口径说明中保留。主区省去日期行，真实观察日保留在可访问的口径入口及其语义中。 |
| 我的拥有 | 区标题约 18sp，行尾「全部」随分段变化。下一行是轻底轨和独立选中块，之后是两列记录。 | 「项目」「订阅」沿用当前互斥选择（`Role.Tab` 与 `selectableGroup`）及保存状态，分别进入各自完整列表。分段和「全部」都有至少 48dp 目标。记录卡点击打开精确记录详情，返回来源沿用首页。 |
| 底部操作 | 「添加」是滚动区之外的独立主操作，导航另外占位。底部两层之间留空，系统导航区由现有壳层处理。 | 顶部加号和底部按钮打开同一个菜单，仍然只有「新增项目」「新增订阅」两个分支。取消或返回只关闭菜单。 |

页面左右 16dp，主要区域之间约 20dp，双列间距 12dp，卡片内容内边距 12dp。比较画板以约 800 高的标准手机视窗表达首屏。宽窗口延用现有单一居中的限宽阅读区。页面滚动容器、底部添加和导航分区，最后一张卡与操作都可以滚动到完整可见的位置。状态变化可以推动正文换行或增加滚动长度。

悬浮胶囊沿用 `C2PrimaryNavigationBar`，最大宽 210dp，高 58dp，内边距 5dp，外部横向留 16dp，顶部留 4dp，距系统导航区 14dp。三个等宽页签保留至少 48dp 高的触控区。原有 40dp 圆形范围限制选中背景和按压反馈，24dp 原图标居中。沿用细边框、亮色 2dp 阴影、深色零阴影及约 200ms 的选中过渡。可见内容为图标，无障碍名称依次为「首页」「回看」「设置」。[现有壳层与隔离按压实现](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L846)

## 记录卡与字号

- 一条记录一张卡，卡面 14dp 圆角、1dp 细描边，亮色仅使用轻阴影，深色靠表面亮度区分。图片采用 4:3 窗口和当前首页 `ContentScale.Crop` 表达；保持原始图片数据。样稿用可被裁成该比例的合成素材，长宽比及实际裁切仍需 Android 对照。
- 名称约 15sp，类别、状态、持有时间约 12sp，卡内金额约 18sp。普通字号时类别和状态可以并列；长名称、字体变大或窄屏时自然换行。200% 保留两列，卡片随内容增高，金额及元信息顺序展开。不要用省略号隐藏名称和金额，不固定用更小字号适配字体放大。
- 项目以名称、真实类别、真实持有状态及 `heldDays` 为事实，显示项目日均成本。项目日均成本为原 `row.cost.dailyRate`，持有时间来自同一项目结果。
- 订阅以名称、真实类别和真实订阅状态为事实，显示历史平均日成本，卡片简写「历史日均」。它使用 `row.cost.historicalAverage`，不会显示项目持有时长，也不会推定续费方式。
- 已验证的封面占视觉主体。没有关联封面时用原类型或类别图标占位，Reader 不读取。正在读取时保留单独状态；已关联但无法读取时保留「封面暂不可用」和原来的封面状态解释，和进入记录详情的动作分开。
- 空间不足的极端人民币值保留完整访问。合适时金额与次要事实分行，必要时继续沿用 `MoneyMetric` 的横向滚动及「查看完整金额」。如金额为未知，用 `—`；已知零值保留 `¥0.00`。项目和订阅的预览都不超过两条，沿用原先顺序。只有一条时保留原列宽。

[首页卡片与分段](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L372) · [金额展示](../../../app/src/main/java/com/cooper/lithic/core/design/MoneyMetric.kt#L100) · [封面流程](../../../app/src/main/java/com/cooper/lithic/feature/media/CoverPreviews.kt#L64)

## 三套颜色角色

下表是直接复用现有主题的首页映射。三主题结构一致。成本数字靠字阶区分；选中的分段通过独立形状、文字层级以及选中语义表达。

| 角色 | 纯白 | 岩白雾蓝 | 深色黑灰 |
| --- | --- | --- | --- |
| 页面及总览 | `#FFFFFF` | `#EDF2F7` | `#121212` |
| 记录卡表面 | `#FFFFFF` | `#FFFFFF` | `#1E1E1E` |
| 次级表面与占位 | `#F7F8FA` | `#E8EFF6` | `#292929` |
| 主文字 | `#191C20` | `#1C2733` | `#F3F3F3` |
| 次文字 | `#565D67` | `#4B5B6D` | `#B2B2B2` |
| 细描边与分隔 | `#E4E7EB` | `#D3DEE9` | `#343434` |
| 添加按钮／文字 | `#223246`／`#FFFFFF` | `#223246`／`#FFFFFF` | `#E9E9E9`／`#161616` |

纯白靠表面边界和文字区分，雾蓝通过明显的雾蓝灰页面和白卡片区分，深色通过黑灰表面亮度和灰白文字区分。按压、错误和状态色取现有角色。最终实际对比度仍取决于 Compose 合成、系统亮度、文字与背景角色组合，Android 端尚待核对。 [主题角色映射](../../../app/src/main/java/com/cooper/lithic/core/design/LithicTheme.kt) · [已用色板](../../../app/src/main/java/com/cooper/lithic/core/design/LithicVisuals.kt#L17)

## 状态、数量和口径

| 真实状态 | 可见结果和允许的操作 |
| --- | --- |
| 正常，有当前有效记录 | 总览使用真实快照和两类总记录数；相同分段可查看对应预览、详情及「全部」。确认的零金额以真零呈现。 |
| 已确认空账本 | 金额显示 `—`，两个数量均为 0，正文「暂无记录」。不展示分段、两个完整列表入口或成本构成。搜索（可用时）、两个新增入口和原口径说明仍可达。 |
| 有记录但无当前有效项目 | 项目分段显示「暂无当前项目」。如果订阅还在贡献，展示真实合计。如果两类都无当前有效记录，保留历史数量和真实总投入，确认的当前日均成本仍以真零显示。 |
| 读取失败，无快照 | 两个金额和两个数量均为 `—`，显示「暂时无法取得成本数据」与既有「重试读取」。无可陈述的观察日，不进入空账本引导，不借用兼容计数。 |
| 保留上次结果 | 显示「上次取得的结果」，金额、数量和可访问的观察日同属上次快照。即使上次快照为空，当前状态仍未确认，不显示确认空账本引导。保留重试。 |
| 项目预览失败 | 项目分段提供「重试读取项目预览」。可以保留带「保留上次结果」的旧卡；订阅分段与账本总览仍使用自己的状态。 |
| 订阅预览失败 | 订阅分段提供「重试读取订阅预览」。可以保留带「保留上次结果」的旧卡；项目分段与账本总览仍使用自己的状态。 |
| 其它既有读取状态 | 未取得、加载中及日期更新分别沿用「预览尚未取得」「正在读取预览」「正在更新预览日期」。恢复未确认沿用已有守卫与「当前记录状态尚未确认」，单独的恢复提醒不判作刷新失败。 |

总览两类数量包含历史。默认两条预览规则、类型片段是否有当期记录，以及总览贡献项数量属于不同事实。旧快照只能显示原来取得的金额、数量和日期，不能拿兼容计数填空，也不能将旧日期标记为本次查询。若两类预览日期与快照不同，沿用日期更新中状态；已有明确预览失败时保留该失败和重试。无快照失败在这里仅覆盖既有受控 Ready 场景，冷启动 `LedgerUiState.Failed` 和 `StorageGate` 继续走原路径。

「总投入」来自原快照里的长期项目投入字段，包含截止观察日的非现金折抵，不扣项目收回，不计入订阅实付和观察日之后的投入。仓库的现有校验及整数分加法继续处理溢出；这一字段未知不把同一快照其他可信字段变为未知。当前日均成本包含项目的日均及订阅当前成本贡献，订阅卡的历史平均日成本不直接加到总览。保持成本构成、原计算说明、正负值金额与舍入说明的可达性。 [总览快照](../../../app/src/main/java/com/cooper/lithic/data/repository/ReportRepository.kt#L51) · [累计分值规则](../../../app/src/main/java/com/cooper/lithic/data/repository/ReportRepository.kt#L348) · [状态映射](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeOverviewPresentation.kt) · [预览派生状态](../../../app/src/main/java/com/cooper/lithic/feature/home/C2HomePresentation.kt)

## 无障碍与生产映射

焦点先到品牌区动作，接着概览、口径、构成和分段后的记录，最后到添加与底栏。标题、控件名称、类别、状态、时间允许自然朗读。日均与历史平均的描述包含金额和人民币每天。选中的分段和页签有明确语义。封面只有在传递信息或可执行现有操作时进入可访问内容，装饰元素不重复朗读。在 320 / 360 / 400dp、100% / 200% 下检查自然换行、48dp 触控区及正文末尾；真实系统大字、手势导航与三键导航、IME、语义树及 TalkBack 后续在原生环境验证。

| 接点 | 实施时保留的现有合同 |
| --- | --- |
| [HomeShell](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L53) | `open-name-search`，`home-open-add-top`，`home-open-add`，`home-create-project`，`home-create-subscription`。两处新增走同一菜单，取消时不写入。 |
| [HomeOverviewCard、HomeDailyRate、RecordCount](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L250) | `home-total-investment`、`home-total-rate`、`home-cost-information`、`home-category-disclosure` 和两个数量的语义。现有 `home-cutoff-date` 合同需随日期迁移到可访问入口，保留真实日期与无日期条件。 |
| [HomeGridCard、HomeBrowseControls](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L372) | 两个分段、`open-project-list` 与 `open-subscription-list`、两类带真实稳定 ID 的记录回调。旧 [HomeArchiveRow](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeArchiveRow.kt) 的 Fit 行式展示留在其调用方。 |
| [MoneyMetric](../../../app/src/main/java/com/cooper/lithic/core/design/MoneyMetric.kt#L100) 与 [RecordCover](../../../app/src/main/java/com/cooper/lithic/feature/media/CoverPreviews.kt#L64) | 通过 `showDailyUnit` 隐藏重复的视觉后缀而保留原格式化及完整金额访问。如需保留原字号，沿用现有 `keepPreferredSize`。封面继续使用有限预算、票据、Owner 及 generation 核验和独立错误状态。 |
| [LithicNavigation](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L455) | 搜索和类别分析保留原有能力判断。沿用现有 Nav3 连接、原返回来源和 `requestLeave` 守卫。底栏继续使用 `tab-now`、`tab-review`、`tab-settings`。 |

本轮的生产对照还检查了 [P2HomeArchiveContractTest](../../../app/src/androidTest/java/com/cooper/lithic/feature/home/P2HomeArchiveContractTest.kt)、[P2HomeArchiveRowTest](../../../app/src/androidTest/java/com/cooper/lithic/feature/home/P2HomeArchiveRowTest.kt)、[P2HomeReadPresentationTest](../../../app/src/androidTest/java/com/cooper/lithic/feature/home/P2HomeReadPresentationTest.kt)、[C2C1HomeVisualContractTest](../../../app/src/androidTest/java/com/cooper/lithic/feature/home/C2C1HomeVisualContractTest.kt)（任务书中简写为 C2HomeVisualContractTest）、[C2HomeSemanticsTest](../../../app/src/androidTest/java/com/cooper/lithic/feature/home/C2HomeSemanticsTest.kt)、[HomeOverviewUiTest](../../../app/src/androidTest/java/com/cooper/lithic/feature/home/HomeOverviewUiTest.kt) 和 [M13HomeScreenshotsTest](../../../app/src/androidTest/java/com/cooper/lithic/feature/home/M13HomeScreenshotsTest.kt)。核对现有判据与入口，本轮未执行 Android 测试。

后续生产实施只涉及首页的排版与首页专用展示。Room、备份、统计、Repository、Owner、媒体协议、全局主题和导航均在本轮设计范围之外。Android 与发布产物不属于本轮交付。
