# C9 冻结边界只读复核

- 复核基线：`d1c752938792f0a88d19713f06d32c183696d34f`；当前工作分支 `codex/lithic-v1-product-completion`。复核范围为当前 C9 已跟踪差异、外观新增代码及相关 AndroidTest 差异。
- 方式：只读 `git diff`、源码及路径核对；未运行构建、测试、ADB、设备或用户数据操作。仅新增本报告。

| 边界 | 源码结论与证据 |
|---|---|
| Nav3 / 来源 / 离开守卫 | `LithicNavigation.kt` 相对基线仅将 `TextButton` 导入替换为公共呈现包装；函数体无差异。`LithicRoutes.kt` 无差异。原 backstack、requestLeave、dirty/pending/discard 与 C7 来源未改。包装保留原 onClick/enabled。 |
| 主题切换与业务生命周期 | `MainActivity` 的 container、Ledger/Export/Restore ViewModel 创建仍位于 `setContent` 外，新增内容为外观 StateFlow、CompositionLocal/Theme 与系统栏 SideEffect；没有 Activity.recreate、appearance 作为 key 或换主题创建业务 Owner。`LithicApp` 原 generation key 保留。真实动态行为仍 NOT_RUN。 |
| 表单及未确认提交 | 创建/金额/续费/状态/结束/删除/以旧换新/对照页差异为控件导入、取色与布局；原输入 String、onValueChange、KeyboardOptions/KeyboardActions、enabled、onSave/onRetry/onBack、UNCONFIRMED 分支及危险确认未改。全部既有业务 ViewModel/StateHolder/Controller 无差异，因此 operationId 与提交协议未改。 |
| C6-1 / C6-2C / 资源预算 | 整个 `data` 路径 77 个已跟踪文件差异退出码 0，涵盖 `CoverReader`、`GenerationCoverReader`、隔离协议/Service/FD会话和 generation/备份安全实现。`CoverPreviewPolicy.kt` 无差异；`CoverPreviews.kt` 仅 TextButton 导入替换，读取认证、解码、取消、预算和换代逻辑未改。 |
| Room / backup / 算法 / 依赖 | `data` 77、`domain` 20、`platform` 8、`app/schemas` 5 个已跟踪文件各组 `git diff --quiet` 退出码均 0。AppContainer、AppProcessRole、LithicApplication 无差异。Room5/backup4、冻结引擎与业务通知协议未改。 |
| 安装、签名与权限配置 | AndroidManifest、app/root Gradle、gradle.properties、版本目录及 settings 配置均无差异；无新依赖/native库/权限改动。此项确认配置连续性，不代替最终 APK Manifest/实际证书核验。未递增 Preview10、未生成新用户交付包。 |
| 旧断言 | 首页测试只替换 C9 明确废止的绿摘要卡/固定视口底带约束，改断言中性主题背景及实际末条内容可滚至 FAB 上方。原金额精度、负值、0/未知、空态与关闭新增面板零写入断言保留；趋势测试新增图例源码断言，未移除历史保护。Android 实际运行 NOT_RUN。 |

较早交叉核对发现的禁用态低对比遗漏，现已在 ProjectInputs、MoneyEventEditor 与 SubscriptionEditorScreen 中改为公共语义色；原输入、资格及回调未变。当前差异未发现需要停止交付的确定性冻结边界缺陷。`git diff --check` 退出码 0。

本次为源码边界复核，不授予 Android 视觉、动态导航、IME、系统栏、隔离进程或媒体运行安全 PASS；这些仍按原状态 NOT_RUN / PENDING / UNKNOWN 保留。Git差异为仓库内容比较，未把Windows换行差异或源码无差异解释为设备资源正常关闭。
