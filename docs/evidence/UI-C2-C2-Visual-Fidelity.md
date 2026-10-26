# UI-C2 C2 — 真实视觉差距收口

**UI-C2 C2 IMPLEMENTED — APP_VISUAL_PENDING**。来源 `c35bcb375b6045ae616c5191372cde1673215b1a`，分支 `codex/lithic-v1-product-completion`。用户转交独立结论 **VISUAL REJECTED — PREVIEW 5**；原构建与签名检查不撤销。本轮仅提交生产空态整改及原生视觉夹具，不生成 Preview 6。

## 诊断与证据边界

- 已实际打开 UI-C1 冻结 `0dce7dd0725a54be4f0280870ef6ca47c48ea7ed` 的[浅色](https://github.com/cc7279694-debug/property/blob/0dce7dd0725a54be4f0280870ef6ca47c48ea7ed/docs/design/ui-c1/source-v1/ui-reference-light.png)和[深色](https://github.com/cc7279694-debug/property/blob/0dce7dd0725a54be4f0280870ef6ca47c48ea7ed/docs/design/ui-c1/source-v1/ui-reference-dark.png)原图；来源及 SHA 见[原始观察](ui-c2-c2-20261026-140315/source-observation.json)。本地没有本次 Preview 5 手机实拍原件，按用户明确问题描述及生产源码定位，不声称重新看过实拍或完成像素对照。
- `HomeShell` 原空态隐藏 FAB、增加两层引导及全宽新增，主卡使用较宽松间距；`ReviewShell` 把空态整块包入铺满宽度的单色 Surface。这是可以由源码证明的结构差异，不归因于用户字体设置。
- 当前 `LithicTheme` 主金额 38/46sp、页面标题 24/32sp、正文 16/24sp、辅助 14/20sp；底栏 12/16sp。本轮未改全局字体、行高、色板或系统缩放。手机真实 fontScale、density 及实际可用宽度均 **UNKNOWN**；没有操作手机取证。
- HTML/CSS 原型与 Android 的字体度量、Material 组件、安全区和触控最小尺寸不同；旧 HTML 图不能证明生产排版。空库没有照片、记录和曲线是事实，不能靠演示数据填充；但空态本身仍需合理结构，不能用“没有数据”解释设计失败。

## 生产修改

| 页面 | Preview 5 源码结构 | C2 当前生产结构 |
| --- | --- | --- |
| 现在 | 空态绿卡及真实 0 数量卡，但隐藏圆形＋，另有标题、引导与全宽按钮 | 保留绿卡、紧凑“— 元/天”及已确认空 Overview 的两个真实 0；主卡纵向 padding 16→12dp、组间 8→4dp；仅一句引导。所有状态均保留右下 56dp 圆形＋，打开原两项面板 |
| 现在滚动 | 空态不保留 FAB 安全带 | 统一保留 72dp 独立底部操作带；FAB 右/下各 16dp，不覆盖正文或底栏，无第二新增按钮 |
| 回看空态 | 全宽单色大卡、同层文字和描边动作 | 透明、自然高度的内容组；112×88dp 现有矢量组合身份，单句居中引导、唯一“去首页记录”按钮，不铺大矩形、不绘假图 |
| 有记录 | 真实预览、趋势及分析/历史入口 | 数据来源、排序、分页和金额口径不变；仅确认空账本才走新空态，历史有记录但当前贡献 0 仍保留真实功能 |

源文件：[HomeShell](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt)、[ReviewShell](../../app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt)。读取失败、加载、旧结果及未知状态判定保持；无记录仍不是 0 元，负值/真实 0/小于分位未改。数量来自原 Overview，不增加查询。新增面板关闭不写入、选择沿用原回调。

[LithicNavigation](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt) 仅提取原底栏外观为共享 `C2PrimaryNavigationBar`，供生产及原生夹具使用；生产点击仍由原 `requestLeave(canonicalBackStack(destination))` 处理。未修改来源链、dirty/pending/discard、恢复守卫。

## 真实 Android 视觉机制：已准备，未运行

[C2ProductionVisualGalleryTest](../../app/src/androidTest/java/com/cooper/lithic/ui/C2ProductionVisualGalleryTest.kt) 的六个单方法入口已编译：

| 状态 | 原生生产内容与条件 | 本轮实际结果 |
| --- | --- | --- |
| A | 深色、400dp、已知空事实集、现在 | NOT_RUN / NOT_CAPTURED |
| B | 深色、400dp、已知空事实集、回看 | NOT_RUN / NOT_CAPTURED |
| C | 浅色、400dp、合成项目＋订阅、现在；另备滚动记录帧 | NOT_RUN / NOT_CAPTURED |
| D | 浅色、400dp、合成历史事实、真实趋势回看 | NOT_RUN / NOT_CAPTURED |
| E1 / E2 | 深色、360dp、LocalDensity fontScale=2、现在/回看及滚动可达性 | NOT_RUN / NOT_CAPTURED |

- 直接调用生产 `HomeShell`、`ReviewShell`、共享底栏，匹配生产 Scaffold/safeDrawing 布局。[夹具](../../app/src/androidTest/java/com/cooper/lithic/ui/C2VisualFixtures.kt) 仅在 androidTest 中，以固定 UUID 的合成事实交给原 Overview/项目/订阅/趋势引擎计算；不是生产假记录，也没有打开 Room、创建 Owner 或默认 generation。这里“确认空”是已知不可变空输入的引擎结果，**不是已运行的真实 Room 空账本证明**。
- 宿主是隔离 **ComponentActivity，不是 MainActivity**；宽度及字号为 LocalDensity 控制，不是手机真实 400dp/系统 200% 设置或 IME 验收。夹具本地切页/回调观察不冒充完整 Nav3 旅程；本次 E 仅为现在/回看的窄屏大字，不覆盖旧 C1 的项目详情/录入抽查。
- 未来执行仅捕获合成 Compose 画布，在目标包独立 UUID 目录中独占写入归属标记、PNG、尺寸/字节/SHA/宿主与字体元数据，不删除旧文件；单 PNG 检查上限 20MiB。本轮未运行，因此没有实际 PNG 或该测试私有输出目录。详细边界见[入口状态](ui-c2-c2-20261026-140315/native-gallery-status.json)。
- 运行前仍须新设备授权及真实 Test Manifest/Runner/target/SHA 核验，必须为 `M13IsolatedRunner`＋plain Application。`@Before` 的断言是**启动后**检查，不能代替启动前验包，也不能阻止错误 Runner 已启动生产 Application。

本轮没有设备授权，没有使用 ADB/手机/AVD；没有现成桌面 Android 渲染链，不搭新截图系统或启动已知 ANR 设备。**真实 Android 截图 0；UI-C1 与本次真实 Android 的逐页视觉对照 PENDING，不能申请 VISUAL_ACCEPTED。**

## 实际验证

- **50 个定向 JVM PASS，失败/错误/跳过均 0**：导航、金额完整显示及首页/回看状态相关九个类。见[JUnit 汇总与原 XML](ui-c2-c2-20261026-140315/jvm-result.json)。未重跑 M22 的 137 JVM、36 Android、T06/T03/IME。
- Debug 构建、AndroidTest 源码编译、Lint 首轮均退出 0；Lint **0 错误、21 警告**。见[原命令及退出码](ui-c2-c2-20261026-140315/debug-quality-first/command.json)、[stdout](ui-c2-c2-20261026-140315/debug-quality-first/stdout)、[stderr](ui-c2-c2-20261026-140315/debug-quality-first/stderr)、[Lint XML](ui-c2-c2-20261026-140315/lint.xml)。
- 后续只修截图入口的大字滚动请求及元数据描述，生产源码未变；[定向 AndroidTest 再编译](ui-c2-c2-20261026-140315/gallery-source-final-compile/command.json) 退出 0，未重复 JVM 或扩跑套件。
- 2 个新增回看布局断言＋6 个截图入口仅编译，**8 个新增 Android 方法全部 NOT_RUN**；既有首页/回看 UI 断言仅更新本次新增位置、唯一入口、几何与空态结构要求，原 0/负值/精度/未知断言保留。编译不代表触控、真实字号、滚动或像素断言已通过。

## 保护与停止

[边界核对](ui-c2-c2-20261026-140315/boundary-audit.json)：Room5、backup4、领域/DAO/Repository、Owner/coordinator、恢复、媒体/提醒协议、Theme、Manifest/资源/依赖及构建版本配置无改动。普通构建身份未改，Private Trial 未构建；非 build 交付目录中的 Preview 3/4/5 和 M22 App/Test 旧实物 SHA 保持，无 Preview 6。旧 `app/build/private-trial/.../app-debug.apk` 临时构建副本在本轮普通 Gradle 检查后已不存在，删除原因未独立捕获；未执行显式 clean/删文件，也未重建或复制恢复它。**永久 Preview 5 交付实物仍完整且 SHA 不变**，不把临时副本缺失冒称全路径保持。

真实封面 **MEDIA_DISPLAY_PENDING**；M10 三 JPEG 历史 FAIL、M20 平台 FAIL/UNKNOWN、M22 原 BLOCKED、M13-T06、G1～G4及全部历史 NOTES/NOT_RUN 不变。手机、个人账本、默认 generation、WorkManager、旧截图/锁文件及受保护现场均未操作。

停在 **UI-C2 C2 IMPLEMENTED — APP_VISUAL_PENDING**，提交推送后等待独立审查；不发新用户 APK、不合并 main、不进入 M23。下一步仅在另行获准的隔离 Android 环境获取 A～E，再对照原图和真实画面决定视觉是否通过。
