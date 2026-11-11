# LITHIC P2-A2 可启动的隔离体验包

**P2_A2_READY_FOR_INDEPENDENT_REVIEW** · 2026-11-11。正式 App 验收和小米14体验仍按各自证据范围判断。

基线为已接受的 P2-A1-R1 源码 `5e6920056903abbd73444ec9dddbfd57a16e5e50` 和证据 `f38cb97aec3559f79c70c2069c322e4ab67f45c1`。从证据提交创建独立分支 `codex/lithic-ui-p2-a2`。实际构建源码 `eada98b0d0021fd910677ead0cb5be09f4d6272b`，源码树 `8b4500f8176cc40c703a003f5bf2e0af247d2eec`。生产源码和 Room schema 无差异。

## 构建、安全及产物

| 项目 | 实际结果 |
| --- | --- |
| 构建入口 | `-Pp2A2Experience=true`，Debug 独立构建目录。与原测试、体验、签名模式互斥，关闭本模式 Release 和 AndroidTest。 |
| APK | `E:/CODEX/lithic-ui-p2-a2-evidence/candidate/P2-A2-App.apk` · **20,929,219 字节** |
| APK SHA-256 | `1527bf615ced440c85fae7ce139d61a5a712686d23bde0cd4f7603f64117cf9b` |
| 身份 | `com.cooper.lithic.p2a2.debug` · `0.1.0-p2a2-debug` / code1 · `com.cooper.lithic.p2a2.P2A2ExperienceActivity` |
| 签名 | 原单一 Debug 证书，APK v2 验证通过。证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9` |
| Manifest/DEX | 普通 `android.app.Application`，仅一个导出 Launcher，关闭备份。实际权限只有本包作用域签名权限。无生产 Launcher、Provider、Service、Receiver、通知、网络或照片权限。DEX 核对 A2 入口复用生产 Compose，未见 A2 类调用审查中的 Owner、数据库、文件、后台提醒或网络接口。 |
| 保护核对 | 正式 APK 仍为 15,747,081 字节，SHA-256 `ff909ca1eb21e04756d91d067082ca12df1ac17c263d793843fbbf4c67e40c2b`。两套 A1 App/Test 归档原字节一致。 |
| 编译与测试 | Kotlin、Debug 打包和 49 项 JVM 定向回归通过。最终隔离候选加入一条 A2 会话说明后重新编译及打包，独立打包回执退出 0，归档及模拟器安装 SHA 均一致。 |
| Debug Lint | **FAIL，2 错误 / 22 警告**。仅两处 Error 为共享 `NotificationPublisher.kt` 第 42、56 行 `NotificationPermission`。A2 剥离通知权限及启动组件，共享通知发布源码仍纳入 Lint 分析。保留原始 XML，没有加权限或屏蔽规则。安全结论来自本包 Manifest 与入口 DEX 审计。 |

[专用 Manifest](../../app/src/p2A2/AndroidManifest.xml) · [体验入口](../../app/src/p2A2/java/com/cooper/lithic/p2a2/P2A2ExperienceActivity.kt) · [合成事实](../../app/src/p2A2/java/com/cooper/lithic/p2a2/P2A2Fixtures.kt)

## 原隔离模拟器实际操作

复用原 `Lithic_P2A1_API36`，API 36 / x86_64。启动前本包无安装及保留包记录，User 0 全新安装一次，应用安装字节与归档相同。SystemUI PID 1027 在有限观察中稳定，事件日志的限定查询未见 ANR。

| 实际路径 | 结果 |
| --- | --- |
| 桌面冷启动 | 从 AOSP Launcher 应用抽屉点「砺石 · 隔离体验」。点按前无该包进程，点按后确认独立 Activity 为前台，首页正常。 |
| 三主题 | 通过生产主题选择器切换纯白、深色黑灰、岩白雾蓝，回首页检查实际主题、Android 状态栏及导航栏。 |
| 首页与档案 | 生产首页展示两件合成物品和一条订阅，总日均 ¥13.83/天。横图、无封面类型图标和竖图均显示。两类「全部」进入生产列表，点按档案进入详情，系统返回回到来源。 |
| 搜索 | 通过 Android 输入事件键入 `Paper`，找到一条合成物品，进详情后返回时查询保留，再返回首页。 |
| 新增及导航 | 从圆形入口打开新增菜单，选择长期项目显示「体验模式，不会保存真实记录」，再次打开后用系统返回关闭。成本口径能进入完整说明。回看可进入成本趋势并逐层返回。 |
| 200% 字体 | 原模拟器系统字体 1.0→2.0，360dp / 720×1648px。项目、订阅「全部」位于独立新增栏上方，实际点按订阅入口成功。额外取证画面中订阅「全部」完整可见。恢复字号并读回 1.0。 |
| 退出及数据 | 从首页系统返回到桌面，再点 Launcher 图标重开。仍是相同合成账本。只检查 A2 自身私有目录，未见 databases、no_backup、shared_prefs。 |

首次自动观察的 Activity 查询过滤条件未输出前台行，随后针对同次启动读得前台身份。两个 UI 检查初次使用的期望文字与实际不同，分别按原始界面的「记录真实续费」和「成本趋势」完成验证。三处初始探针结果保留，从同一运行现场继续核对。

**原生截图** [岩白雾蓝](lithic-ui-p2-a2/home-stone-mist.png) · [纯白](lithic-ui-p2-a2/home-pure-white.png) · [深色黑灰](lithic-ui-p2-a2/home-charcoal.png) · [系统 200% 顶部](lithic-ui-p2-a2/home-system200.png) · [系统 200% 订阅区](lithic-ui-p2-a2/home-system200-subscription-clear.png)。正常字号沿用圆形 FAB，在双物品样本的初始滚动位置，它与订阅的成本说明下缘接近并有局部遮挡，滚动区域仍可查看完整内容。本轮沿用已验收的生产布局。

## 体验范围与未执行项

体验包支持合成账本的浏览、查询、成本和主题。主题仅在会话内保留。真实新增、编辑、付款、状态变更、照片导入、备份恢复、通知及跨会话账本使用均不可用。历史档案及部分分析入口显示体验提示。没有测试生产 MainActivity、真实账本、TalkBack 或正式版端到端流程。

小米14 **NOT_RUN / 环境不可用**。模拟器操作完成后再次只读枚举，登记 USB 指纹匹配数为 0。没有向手机发设备命令或安装 APK，手机上的身份占用及系统健康仍为 **UNKNOWN**。

本机原始构建、Lint XML、签名/DEX 审计、安装命令、界面树、截图和运行脚本保存在 `E:/CODEX/lithic-ui-p2-a2-evidence/`。仓库收录[运行回执摘录](lithic-ui-p2-a2/runtime-checks.txt)。保留 Lint FAIL，等待 ChatGPT 独立验收。
