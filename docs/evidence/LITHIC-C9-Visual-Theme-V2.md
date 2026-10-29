# C9 视觉与主题V2集中实施

基线：`d1c752938792f0a88d19713f06d32c183696d34f`；分支：`codex/lithic-v1-product-completion`。本轮仅桌面实施，不生成用户APK、不操作设备。用户新录屏/概念图原件本轮不可访问，按转交任务书、当前生产代码及既有记录实施，不声称看过。

## 实际实现

| 范围 | 生产接线及保护 |
|---|---|
| 三主题 | `LithicTheme`/`LithicVisuals`统一背景、文字、强调、语义反馈和图表角色；只有深色黑灰、纯白、岩白雾蓝，缺省岩白雾蓝。不跟随系统变化，不用动态取色。 |
| 外观选择/持久化 | Settings内同布局三项面板、Radio选中语义、保存中/失败/重试。`AppearanceController`串行与序列保护，成功落盘才确认选中；未知值/读写失败不冒充无偏好，不抹旧值。独立noBackup偏好文件，不进入backup4。 |
| 启动与主题切换 | `AppearanceProvider`仅MAIN准入后访问文件、同Application/文件单实例；SANDBOX/UNKNOWN不初始化。MainActivity业务VM/导航创建位置保持，无appearance key/recreate；系统栏显式跟随选中主题。 |
| 首页/两类详情 | 自然分组、轻数量摘要、精确成本及次级事实、通用身份/合法已有封面；不再用固定深绿卡/白字覆盖三主题。真实0/负数/未知/空库/旧快照及唯一56dp新增入口保持。订阅历史平均与当前套餐分离。 |
| 浏览/历史/搜索/表单 | 紧凑公共EntryCard与共享按钮/输入/反馈；主要页面24dp、小屏16dp，局部列表内保留12～16dp。跨日付款、原输入String、operationId、UNCONFIRMED、纠错与危险确认不改。禁用按钮/输入/Switch/勾选框/可禁用菜单使用可读语义色。 |
| 回看/完整趋势/分析 | 选日准确金额、曲线、贡献、工具的原结构；单项图选日金额提前，分项实线/长短虚线与文字图例辅助区分。年度/类别/复盘轻分组。原C5-R1假空防护、选日未知和全部事件/50条分页仍在。 |
| 媒体/冻结边界 | C6-1认证、解码、预算/取消/Owner及C6-2C隔离机制保持，CoverPreviews仅公共TextButton呈现import调整；不接入导入。Room5、backup4、数据/领域/VM/Controller、恢复、Nav3行为及版本/签名配置不改。 |

文件职责与步骤见[设计/实施计划](../design/LITHIC-C9-Visual-Theme-V2.md)；[边界核对](c9-theme-v2-20261029-093858/boundary-review.md)、[主题核对](c9-theme-v2-20261029-093858/theme-review.md)是内部源码检查，不代替ChatGPT独立审查。

## 真实验证与首次失败

最终桌面门禁退出0：**135个唯一JVM方法全部PASS（22新逻辑/存储/颜色＋113相关回归）**；AndroidTest源码编译、普通Debug构建成功，**Lint 0错误16警告**。Lint三项分析执行、报告命中UP-TO-DATE，原XML字节一致；[重用回执](c9-theme-v2-20261029-093858/complete/lint-validity.json)，不冒称新建XML。编译未执行Android方法。以[最终命令](c9-theme-v2-20261029-093858/complete/command.json)、[JUnit](c9-theme-v2-20261029-093858/complete/summary.json)、[Lint](c9-theme-v2-20261029-093858/complete/lint.json)为准。

- [RED](c9-theme-v2-20261029-093858/red/summary.json)：20项中2 PASS/18 FAIL，先记录缺失行为再实现。
- [第一次编译失败](c9-theme-v2-20261029-093858/green-focused/command.json)：IconButton新版参数顺序导致失败，改用命名传参；测试未执行。该目录曾复制旧RED XML，[有效性说明](c9-theme-v2-20261029-093858/green-focused/junit-validity.json)明确其非本次结果，不累计。
- [首次整合](c9-theme-v2-20261029-093858/integrated/summary.json)：134唯一方法133 PASS/1 FAIL，真实文件第二次保存失败。缓存字节码及[文件探针](c9-theme-v2-20261029-093858/filesystem-probe/command.json)确认Windows JVM模拟SDK0走legacy rename；不是API26+ Android缺陷证据。只为新外观存储采用已有公开Okio重载，保留Preferences格式/原断言并补坏文件保护；[调查证据](c9-theme-v2-20261029-093858/storage-platform-review/review.txt)。
- [补修前GREEN](c9-theme-v2-20261029-093858/final/summary.json)：135 PASS，AndroidTest编译/Debug/Lint退出0，0错误16警告。随后只补两处禁用勾选框与一处付款菜单色及对应对比断言，最终复验另存complete目录，不覆盖此前输出。
- JVM覆盖新外观默认/未知/失败/快速选择、真实偏好保存关闭重开/竞争实例/坏文件不覆盖、三主题颜色及按压焦点/禁用对比，另回归金额、图形几何、C7/P02/Nav3来源、C5-R1、进程角色；不重跑M22历史大套件，不累计重复执行次数。
- 普通文字/操作对比4.5:1，必要图形/边界3:1；低对比分隔仅装饰。数学组合检查不是Android渲染/触控或用户视觉验收。

## 视觉矩阵与未运行

| 准备 | 证据等级 |
|---|---|
| 三主题×首页/项目详情/订阅详情/回看/设置15位 | `C9ThemeMatrixTest`五方法各遍历三主题；生产Composable与androidTest合成事实，语义断言源码；真实截图0张。 |
| 主题面板/长名称与负金额/360dp两倍字体/未确认输入 | Matrix另1方法、`C9RecordThemeTest`3方法；`M18TotalCostTrendSemanticsTest`新增1方法，合计10个新增Android方法，仅编译、运行NOT_RUN。 |
| 原生字体/IME、系统栏、滚动/返回/快速切换、冷启动恢复 | NOT_RUN。LocalDensity夹具不是系统200%字体；纯状态/来源测试不是Activity动态证据。 |
| Android安全隔离/资源/低存储/文件系统异常 | NOT_RUN或原UNKNOWN保持；不因MAIN守卫JVM通过宣称真实进程隔离PASS。 |

本轮Android截图0、Compose桌面渲染0、设计示意新图0；不制作HTML替代，不重开C3或旧故障模拟器，不向真实账本写合成记录。

## 产物与剩余门禁

[APK静态身份](c9-theme-v2-20261029-093858/apk-audit/identity.json)：仅普通Debug `com.cooper.lithic.debug`/code17用于检查，16,399,890字节，SHA-256 `47ce1379d162e9bcf80895e1d991ff458ac06062990e95772bd2ffa899357147`；原Debug证书 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。正常LithicApplication/MainActivity、源码实际声明的androidx-startup authority、无INTERNET；三主题/页面DEX存在、C9与C2夹具不在产品DEX；native条目及字节和受保护Preview10一致。APK与complete编译源SHA逐项相符，未复制为交付物。最初桌面检查误假设存在.files Provider，依未修改的真实Manifest纠正核验断言，未改产品。Preview10/code26及原证书/实物保持，不生成Preview11。坏偏好文件不会自动清空；当前仅重试与持续错误提示，无未经授权的修复/删除通道。

保留M10三JPEG FAIL、M20平台FAIL/收口UNKNOWN、M22 BLOCKED、C3调查关闭/运行UNKNOWN、M13-T06、Q03/Q04与G1～G4全部历史FAIL/UNKNOWN/NOT_RUN/NOTES。C9不解除图片导入，不代表真实升级/备份恢复/正式发行通过。

状态：**C9 VISUAL & THEME V2 — READY_FOR_INDEPENDENT_REVIEW**，仅桌面实施；ANDROID_VISUAL_PENDING / ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED。提交推送当前分支后停止，等待ChatGPT独立审查；本报告及内部源码复核不构成ACCEPTED/FROZEN。
