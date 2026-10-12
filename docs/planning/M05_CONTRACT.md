# M05 页面骨架与基础视觉契约

2026-10-12，编码前记录。基于已冻结V1/UI_SPEC和本轮明确执行授权，落实工程细节，不重新定义产品/品牌；独立验收尚待进行。

## Goal / Scope / Dependencies

目标：三个主入口、统一基础组件、真实空/非空状态、存储失败保护及可恢复Navigation3。使用M01/已冻结M04，不修改成本引擎、Room v1、数据所有者或Settings生命周期。

起点 `aecbe86fce6e5c4375f2c35d2df8539df0a557bb`，来源feat/m04-room-storage-foundation，本轮feat/m05-ui-foundation；开工核对本地/远端匹配且干净，新分支此前不存在。用户附件SHA-256 `419d5a963729d1c93f94aa935f2a189f455da0a1aa331de4e92119bc10968ece`。原M05计划、UI U01～U03/U10、PRODUCT信息架构、ARCH A01～A03/A07/A08及当前M04接口是事实源；归档不改。

仅实现现在/回看/设置与必要长期项目列表二级空壳、Theme/AmountText/EntryCard/FormFields、轻量UI状态与真实测试。排除CRUD、业务详情/保存、M13汇总、订阅表、曲线/年度/排行、搜索/新增假按钮、备份/提醒、Logo新资产、云端及M06。测试专用合成表单和项目不进入生产数据或菜单。

## 三入口与返回

- 现在：标题→当前日均成本主视觉→长期项目→订阅说明。只有真实快照为空才“暂无记录”；汇总尚未接入，用无值/说明，不默认¥0.00。非空展示真实项目数量及只读列表入口，仍不伪造汇总。
- 回看：明确真实无记录/已有记录，复盘、档案、趋势、类别、年度能力以“尚未开放”静态说明预留，不画模拟统计或提供无动作入口。
- 设置：真实记录时区、本地保存说明、Debug/完整备份未交付提醒；备份/通知只说明未开放，不放假开关。
- 唯一二级路由为“长期项目”：真实数量/名称/生命周期日期或真实空状态；不进入详情/编辑/新增。
- 既有LithicNavigation原地演进。@Serializable NavKey只含路线身份，合法栈为[现在]、[现在,回看]、[现在,设置]、[现在,长期项目]；切Tab替换合法栈，复点不压栈。二级顶部/系统返回到现在，回看/设置系统返回到现在，现在系统返回退出。二级归属现在Tab。
- rememberNavBackStack/Nav3保存状态；只保存路线，不存DB/Context/generation。旋转/recreate恢复合法栈；真正后台系统杀进程按保存状态重开或安全回现在，force-stop另计冷启动，不冒充保存恢复。对旧工程Skeleton/未知无效路线安全归一化，不能升级后落入不可达页。

## Storage → UI状态

单一Application AppContainer继续拥有M04存储；轻量ViewModel/UI holder仅拥有派生UI状态与读取任务，不创建另一数据库/设置/全局数据集。

| M04/读取状态 | UI |
|---|---|
| Initializing或真实快照/zone尚未读完 | 稳定加载页，无正常空账本和底栏业务入口 |
| Ready且LedgerReadRepository.snapshot及settings.recordZone成功 | UI Ready(snapshot, recordZone)，再呈现三入口 |
| Failed或Ready后真实快照/设置读取失败 | 全页保护性错误，无正常首页/空列表/0值 |

保护性文案：“本地数据暂时无法读取。”“为保护已有记录，应用不会自动创建空账本。请不要卸载应用或清除应用数据。”不暴露异常堆栈/绝对路径；不增加无效重试。退出按钮如提供必须真正退出Activity。取消继续传播，不当错误或空数据。Compose不访问DAO；读取经Repository且在IO，页面切换不重新初始化。UI holder跨旋转保留，进程重建重新等待真实数据；M06可再接入写入刷新，此轮只读。

## 基础视觉与组件

已确认：系统明/暗、中性色、稳定金额与时间层级，主数字38sp、页面标题24sp、正文16sp、辅助14sp，4/8/12/16/24/32dp，触控≥48dp。工程选择：低饱和蓝灰作交互强调、温和浅底/深灰底；不以红绿评价成本，不加渐变/发光/装饰动画。沿用临时工程图标，无新品牌断言。页面以层级/间距/分隔表达分组，避免所有说明套统计卡。

- LithicTheme：简单浅深colorScheme/typography与间距，系统字体，无主题商店或框架。
- AmountText：BigDecimal?与明确单位；null显示“—/暂无数据”，0显示¥0.00，负号保留，普通金额HALF_UP两位/toPlainString，不经Double。**按DOMAIN C02，日均非零绝对值<0.01整体显示<¥0.01/天或−<¥0.01/天**，不能仅判断舍入后为0；普通金额±0.005仍正常HALF_UP。单位独立适配，未舍入数值不变，不计算成本。
- EntryCard：可选封面Composable/占位、名称、调用方状态/时间文案、nullable日均成本；两类标签由调用方提供。不生成生产样本；无onClick则不可点击。
- FormFields：Name/Amount/Date受控输入、标签/错误/焦点/IME与可访问性；日期选择由宿主回调管理。仅组件和测试专用表单，不保存Room数据或宣称M06。

## 360dp / 200% / Accessibility

内容单列纵向滚动，主数字/单位在窄屏可分行；不固定文本高度或截断关键错误/金额，长名字自然换行。底栏三项始终可点击，48dp最小触控；安全区/insets与IME避让，表单末端保存/返回可滚动到可视范围。用实际设备wm宽360dp和font_scale2.0及截图/UI语义证明，不以节点存在代替可达/点击。支持系统浅深并实际检查；TalkBack全审/OEM/真机未测不冒称覆盖。

## Verification / Stop

先用可编译最小接口取得真实行为RED，再实现GREEN；已有M04正确行为只回归不破坏。M05-T01三Tab/重复/返回，T02真实Room空与非空/失败非空账本，T03实际360dp200%+合成表单键盘/错误/保存返回，T04旋转/Activity重建/真正进程死亡与force-stop分别记录。

额外测试存储Initializing/Failed、Ready读取失败，金额null/0/负/极小、NavKey保存、主题/大字体组件。保留并重跑原74 JVM与86 Android，完整testDebugUnitTest/connectedDebugAndroidTest/lintDebug/assembleDebug。依赖仅必要稳定Compose测试、当前已解析Lifecycle版本及测试运行已解析的Espresso3.7.0（显式编译真按键驱动），官方/POM/实际解析与运行差异记录；不升级冻结值。

最终APK code3/0.1.0-m05-debug（仅本地Debug交付），检查包名/签名/安装/冷启动/断网/权限；schema v1原hash、generation/zone重启稳定。原43/130和历史不改，Q03～Q06/M09边界保留。只提交/推送M05分支，记录实际未测项，不合并main/公开APK。最后READY_FOR_REVIEW — M05，停止等待独立验收，M06另行授权。
