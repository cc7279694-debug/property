# C9 主题实现交叉审查

范围：2026-10-29，只读检查当前生产源码、测试源码及既有原始回执；本审查未运行 Gradle、Android、ADB 或截图，也未修改生产与测试源码。审查属于实施内部交叉检查，不代替 ChatGPT 独立验收。

## 已核对

- **三套固定主题。** `LithicAppearance` 仅含 `charcoal`、`pure_white`、`stone_mist`；默认岩白雾蓝。生产 `LithicTheme` 不读取系统深浅或动态取色；保留的 Boolean 重载仅映射至三项中的黑灰／纯白，不是第四项。基线 `git grep` 只发现旧 `isSystemInDarkTheme()`，未发现旧持久外观键，因此没有虚构旧值迁移。
- **持久化与业务隔离。** `AppearanceProvider` 在取得私有目录前执行 MAIN 门禁；偏好位于独立 `noBackupFilesDir/lithic-appearance.preferences_pb`。Provider 与规范化文件身份均限制单实例。Controller 以序号和 Mutex 串行读写，过期排队选择不写盘，已完成的真实写入作为后续失败的保留值；初读期间面板禁止选择，保存中允许连续选择。读取失败／未知值不写默认值，不抹除原字节；只有写入返回后确认所选外观。
- **生命周期与系统栏。** Controller 在 Activity 的组合外取得，主题变化仅重组，没有以主题为 key 重建 Activity、Owner、ViewModel 或导航。AppContainer 与外观 Provider 的 MAIN 门禁先于主题组合；sandbox/UNKNOWN 没有新增外观初始化入口。原先默认 `enableEdgeToEdge()` 的系统取色问题已改为主题 SideEffect 显式 `SystemBarStyle.light/dark`，并更新图标明暗。实际缓存 Activity 1.12.4 字节码确认上述 API 存在；API26/28 的 scrim 和系统栏真实表现仍 NOT_RUN。
- **统一角色与操作。** 身份、金额、输入、错误／警告反馈、列表、弹层与图表使用共享 Material／Lithic 语义角色；未知类别保留通用类型矢量，不以名称推断品牌。图例具备文字＋实线／长虚线／短虚线阶梯，绘图几何、采样、金额算法未改。主题面板是三个同布局 swatch、可访问选择语义及明确选中标记，切换不会主动关闭面板或写入业务记录。
- **C5-R1 保留。** `TotalTrendPresentation.kt` 本轮未改；记录数量不匹配时不呈现首次空账本引导，旧快照保留截止日，日期不匹配的 selectedPoint 不显示成新日期金额。选日失败时仍可切至 `timeline.events` 的全部历史，保留未知事件提示、50条继续加载、记录跳转及非现金标记。领域、数据、schema、Manifest 与构建版本路径没有本轮差异。

## 已发现并复核的窄补修

1. 结束项目及以旧换新两处 Checkbox 原使用 Material3 默认禁用 alpha。纯白／岩白雾蓝下，默认 onSurface×0.38 在白色表面约为 2.41:1／2.14:1，不能据按钮与输入测试宣称它们也达到本轮目标。当前已统一使用 `lithicCheckboxColors()`，主动与禁用状态均显式取语义角色，原 enabled 与 toggleable 资格未变。
2. 订阅付款纠错菜单原依赖默认禁用文字透明度。当前 `Text` 已依据 enabled 明确取 onSurface／onSurfaceVariant，保留原不可点击资格。项目管理菜单已有相同显式处理；Switch 已覆盖禁用角色，其余 RadioButton、FilterChip、导航项未发现可达的默认动态禁用遗漏。

`C9ThemePaletteTest` 已增加上述 Checkbox 轮廓／勾选及菜单文字组合断言。静态颜色计算不证明设备栅格化、系统栏或实际交互；窄补修后的执行回执以 `complete/` 最终结果为准，不能沿用补修前的构建结果。

## 对比度与证据边界

对当前源码实际前景／背景组合另行计算：正文／次字／错误／警告／成功在页面、表面、分组底色上的最低值，三主题分别约 6.86、6.13、5.71；实际图线／网格／必要轮廓在绘图页面或表面上的最低值约 3.52、3.69、3.70。低对比细分隔线仅作装饰，不作为唯一控件边界。按下／焦点叠色及显式禁用组合另有定向 JVM 断言；真实显示与触控仍待 Android 运行。

- `final/summary.json` 实际记录135项 JVM、0失败／0错误；`final/stdout.log` 有 AndroidTest 编译、Debug 构建与 Lint 成功结束，`final/lint.json` 为16警告、0错误。这是上述最后两项颜色补修**之前**的已保存结果；不在这里提前宣布 `complete/` 已完成。
- **15个检查位只是源码覆盖。** `C9ThemeMatrixTest` 的5个页面方法各遍历3主题，主要断言生产根节点存在及无业务动作；加1个主题选择面板方法、`C9RecordThemeTest` 3个方法、图例1个方法，共10个新增 Android 方法。全部实际运行 **NOT_RUN**，不是15张截图，也没有桌面／Android视觉通过结论。
- 大内容／表单检查使用 androidTest 合成事实及 LocalDensity 的360dp、2倍字体模拟，保留原始金额与 UNCONFIRMED 输入并断言不提交；不涉及真实账本，不能充当真实系统200%字体、IME、Owner身份或手机流畅度证据。

结论：已核对范围未发现需修改冻结业务合同的阻断；最后窄补修源码已复核，仍需最终执行回执绑定。保持 `ANDROID_VISUAL_PENDING`、`ANDROID_ISOLATION_RUNTIME_PENDING`、`MEDIA_IMPORT_BLOCKED` 及全部历史风险，不授予视觉或发行通过。
