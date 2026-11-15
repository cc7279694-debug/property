# 砺石 UI-V3 · 两个详情页最终精简

起点 `949ed0f0c4fe94b5e9e943790f3a1fc9a0b1c853` 的本地工作区干净，fetch 后与 GitHub 分支 `codex/lithic-ui-v3-detail-information-hierarchy-02` 一致。本轮使用独立分支 `codex/lithic-ui-v3-detail-final-polish-03`。

## 改造前后

| 区域 | 基线 | 本轮 |
| --- | --- | --- |
| 项目成本 | 单张日均成本卡，正文带日期、单位和部分口径 | 「日均成本」和「累计投入」两张同样式卡。常规字号并排，宽度或字号放不下时纵排 |
| 项目财务 | 收起仍显示投入和收回 | 收起只显示标题和展开箭头。点击查看累计投入、累计收回、净投入 |
| 项目记录与资料 | 金额记录标题较重，最近三条、资料折叠 | 标题和类型降低视觉权重，显示记录真实总数，保留最近三条与同列表「全部／收起」。资料和口径按需打开 |
| 订阅成本与身份 | 历史日均成本带单位，套餐价格与周期在身份区重复 | 历史成本卡隐藏可见单位。身份区显示类别及状态，价格与周期集中在当前套餐。次级标题更轻 |
| 订阅记录与资料 | 最近三笔，其他资料和状态历史按需打开 | 沿用顺序及分页行为，统一「更多资料」标题和展开提示 |

## 金额与文案

- 两张项目卡通过同一局部组件取当前主题的成本表面、边线、圆角、内边距和同一首选金额字号。纯白为 `#EFF4FA`，岩白雾蓝为 `#E8EFF6`，黑灰为 `#202C39`。布局按可用宽度选择。纵排后仍放不下的金额按首选字号换行，保留完整金额入口。
- 新的累计投入卡读取既有 `detail.cost.grossOutlayCents`，含长期项目的现金投入与非现金折抵，不扣减收回。净投入和日均继续使用原计算结果。未知日均仍显示 `—`，真实零显示 `¥0.00`，正负不足一分沿用 `<¥0.01` 表示。
- 项目卡省去可见的「截至」、观察日及 `/天`，日均标题统一缩为「日均成本」。未开始说明、日期和结束后结算口径收进「更多资料 → 成本口径」，身份区仍显示未开始或已结束。空记录用总数「0 笔」表达，省去重复空列表提示。
- 订阅的历史平均日成本、当前标准日均价及无有效套餐时的当前日均贡献均隐藏可见 `/天`。这些日均值保留完整金额对话框、复制及无障碍「元/天」读数。
- 订阅说明保留统计日期和月／季／年折算、未来已付款与已过去时长的区别。该日期只在点开说明后可见。移除身份区重复的价格周期，当前与未来已付套餐分别展开真实数据。旧快照、读取失败、无付款及失效提示保留。
- 项目封面与顶部身份布局、非现金记录标识、纠错资格及结束结算路径继续可达。订阅保留付款日期、金额、周期、覆盖期以及管理入口的服务商自动续费警告。「全部」只展开已加载付款，分页仍由原入口触发。底部原操作、返回和写入守卫沿用现有路径。

## 源码与审查

生产实施提交 `0413d6a9ca593b28c58cec026b873ea7f3d011db`，最终构建输入 `e52521c77f9fc889912974c93a902b10da0dcdef`，App 树 `d46a0ebd2352e757b35603359a3bf4e9807ca71f`。

- 生产改动为 `ProjectDetailScreen.kt`、`SubscriptionDetailScreen.kt`，以及两页显式启用的 `MoneyMetric.kt` 展示选项。该组件的默认参数保留原金额样式、顺序、语义和交互。
- 差异审计核对了首页目录、全局三主题、导航、金额格式化、领域、数据、Room schema、Manifest 与 Gradle 的 Git 对象 ID，均与基线相同。[源码范围和测试索引](lithic-ui-v3-detail-final-polish-03/source-scope.json)
- 独立代码审查发现，超长金额即使卡片已纵排仍可能依赖横向滑动。已将两个项目主指标的极端布局改为同字号换行，保留完整金额入口。新增 360dp／模拟 200% 最大金额测试验证文本、字号与裁切判据，三主题测试检查两个卡片的颜色、尺寸及金额字号。实际渲染需要 Android 运行证据。
- 前两次 AndroidTest 编译分别缺少订阅测试的 Compose `assertTextEquals` 和 `assertContentDescriptionEquals` 导入。两个复验提交仅补导入，生产代码与断言未修改。[首轮失败日志](lithic-ui-v3-detail-final-polish-03/attempt-1/build.log) · [第二轮失败日志](lithic-ui-v3-detail-final-polish-03/attempt-2/build.log)

## 桌面验证

| 检查 | 结果 |
| --- | --- |
| 定向 JVM | 10 类、106 PASS，0 FAIL／ERROR／SKIP。首轮实际执行，最终输入仅补 AndroidTest 导入，Gradle 复用同一套结果。主题、金额表示、布局选择和两个成本引擎均在范围内 |
| 编译和构建 | 生产 Kotlin、AndroidTest Kotlin、`assembleDebug` 通过。最终构建前可用内存 5.395 GiB，退出码 0。AndroidTest 仅编译 |
| Debug Lint | 0 错误、20 警告。三个本轮修改的生产文件无 Lint 诊断 |
| 测试范围 | 12 份 AndroidTest 文件共索引 80 个方法，新增 1 个、移除 0 个。实际运行均为 `NOT_RUN` |

[最终构建日志](lithic-ui-v3-detail-final-polish-03/build.log) · [构建参数与资源回执](lithic-ui-v3-detail-final-polish-03/build-start.json) · [JVM 汇总及原始 XML 索引](lithic-ui-v3-detail-final-polish-03/desktop-summary.json) · [Lint 原文](lithic-ui-v3-detail-final-polish-03/lint-results-debug.txt)

Git 中的构建日志仅清理行尾空白。本机交付目录上一级保留原始日志。

## 唯一 Debug APK

`E:\CODEX\lithic-ui-v3-detail-final-polish-03-local\verified-debug\Lithic-Detail-Final-Polish-03-debug.apk`

| 项目 | 核验值 |
| --- | --- |
| 包名、版本 | `com.cooper.lithic.debug`，`0.1.0-m22-debug`，code 17 |
| 大小 | 20,574,438 字节 |
| APK SHA-256 | `b220768e7dcf4c99caa84e6e71e67c41a0bf164189db741f52640870ae95a2f9` |
| Debug 证书 SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9` |

实际 APK v2 验签通过，解出的完整 Manifest 与权限输出等同基线。DEX 核对项目双卡、记录笔数、订阅历史成本及原 Debug 照片写入开关。归档与构建产物逐字节相同，基线 APK 哈希复核一致。仅在本机归档这一份 App APK，GitHub 交付源码与验证证据。

[APK 回执](lithic-ui-v3-detail-final-polish-03/apk-identity.json) · [Manifest 与签名](lithic-ui-v3-detail-final-polish-03/apk-manifest-signature.txt) · [DEX 抽查](lithic-ui-v3-detail-final-polish-03/dex-audit.txt)

## Android 待验证

Android 运行标记 `NOT_RUN`。本轮未操作设备、ADB 或模拟器。待有可用隔离环境后复核两页截图、三主题卡色、真实 360dp 与 200% 字体、长金额换行和滚动、TalkBack、系统返回及管理、纠错和分页交互。当前截图像素和设备视觉一致性仍待确认。

**READY_FOR_INDEPENDENT_REVIEW**
