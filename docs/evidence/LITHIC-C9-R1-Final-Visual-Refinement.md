# C9-R1 最终视觉与交互集中精修

基线：`c4c39ba40bde561d1f583c76bacc87bf35d0e5b7`；分支 `codex/lithic-v1-product-completion`。

**C9-R1 FINAL VISUAL REFINEMENT — READY_FOR_INDEPENDENT_REVIEW（仅桌面实施）。** 首次RAM3.061GiB未达4GiB，Gradle未启动；用户先要求源码准备/构建暂缓，后明确“内存已充足”恢复验证。恢复构建前实测5.669GiB，首轮实际95项JVM全PASS、AndroidTest源码编译/Debug/Lint命令退出0，Lint0错误16警告。原暂缓和未执行记录保留，不伪造已执行RED。最终提交以本报告所属提交及Git回执为准；[源码树绑定](c9-r1-20261029-142449/source-binding.json)用于核对APK与最终app树。

## 边界与设计依据

按用户转交C9-R1任务书进行Refinement。锁定三主题/默认stone_mist、三主入口、原数据/业务/导航/媒体合同；仅调整层级、表面、身份图标、记录排版和选日视觉。没有本轮可访问的新实拍/录屏，未声称实际查看或获得用户认可。不新增HTML、设计图片或截图路线。

## 五个重点的实际源码实施

| 重点 | 实际修改结果 |
|---|---|
| 主题辨识度 | `LithicVisuals`增加职责明确的brandSurface（仅首页）、identitySurface/identityForeground（仅通用记录身份）。岩白雾蓝首页用#E8EEF2、身份浅蓝/强调#536F86；纯白摘要白底、黑白操作保持；黑灰摘要#1E1E1E、中性图标容器#292929。原底色/主要操作/语义/图表色和主题存储不变，不把所有页面染蓝。 |
| 首页层级 | `HomeShell`保留真实MoneyMetric与截止/错误/旧结果提示；实际项目/订阅预览提前，含历史的真实数量小卡移到记录后。分项成本/类别仍按原入口展开，空库不伪0、不造数据，唯一56dp FAB及原面板/回调/滚动底部空间保持。 |
| 两类详情 | 名称、身份与持有/折算时长、核心准确日均依次分层；主指标用共享HERO，三项辅助金额降ROW并用原完整金额宽度探测。项目与订阅事件日期/类型先扫，金额独立对齐、说明弱化；订阅历史平均、真实过去、当前套餐/覆盖与未来付款分开。全部原管理/纠错/交易组/危险确认和tag保留，不改媒体插槽。详情使用中性表面，不复用首页蓝底。 |
| 回看图表 | 现有阅读顺序已正确，ReviewShell保持。完整/摘要趋势及单项图增加小型“所选日期”标签、明确单位、原真实线型图例及选日竖线/环点。单位槽同时存在于ready/placeholder/无历史路径，范围说明仍在原信息入口；不改几何、采样、选日、全事件/50条分页或金额。 |
| 全局交互 | 复用C9公共按钮/输入/焦点/禁用色/48dp及导航，不重写设置或存储。保留C7来源、原String/operationId/UNCONFIRMED与离开保护。新源码断言准备检查主题变化后的页面/面板与事实，不新增复杂动画或手势。 |

生产修改8文件：`core/design/{LithicVisuals,C2RecordIdentity,CostChart,CostChartPage}.kt`、`feature/home/HomeShell.kt`、`feature/project/ProjectDetailScreen.kt`、`feature/subscription/SubscriptionDetailScreen.kt`、`feature/review/TotalCostTrendScreen.kt`。

## 实际验证与首次暂缓记录

- 新增JVM `C9R1VisualTokensTest` **3方法实际PASS**：主题焦点表面、实际文本/错误/身份/焦点组合对比、黑白中性色。
- 新增Android **8方法仅源码编译，运行NOT_RUN**：`C9R1HomeHierarchyTest`1、`C9R1DetailSemanticsTest`3、`C9R1ReviewSemanticsTest`4；保留原三主题×5页15位矩阵，另覆盖360dp/LocalDensity两倍、长名称/合法长负金额、真0、旧图失败/日期错配、分页与非现金。
- 交叉静态审查发现基线旧`C9RecordThemeTest`将9_000_000_000_000_000分传入Money，超过现有上限，未来运行会在Compose前异常。本轮仅把测试夹具改为`Money.MAX_CENTS=999_999_999_999`，精确输出断言相应为`¥9999999999.99`；不改生产上限/算法、不删断言、不追认该原方法已PASS。
- 实际执行：git diff --check、修改文件空白/身份tag与源接口交叉核对、冻结路径diff及Preview11实物SHA检查。未发现可静态确定的业务合同破坏，但这些不是Kotlin编译或运行证据。
- [轻量RGB计算](c9-r1-20261029-142449/static-contrast.json)：15个新表面前景组合按源码值计算，正文/错误最小5.713:1、图标焦点最小3.648:1；只属设计数学检查，不是JVM或Android视觉通过。
- 首次[资源门禁](c9-r1-20261029-142449/red/resource-gate.json)为历史：当时Gradle0次，RED/GREEN、JVM、编译/Debug/Lint均NOT_RUN，保留不改。
- 恢复后[本次命令/退出码](c9-r1-20261029-142449/verification-20261029-152507/command.json)、[95项逐类/逐方法结果](c9-r1-20261029-142449/verification-20261029-152507/jvm-summary.json)：**95唯一方法PASS，0失败/错误/跳过（3新＋92相关回归）**；含主题/存储/角色、C7来源、C5-R1/空态、金额排版与图形几何。非M22全套，不累计重复次数。
- AndroidTest源码编译、普通Debug构建、Lint整体退出0；[Lint XML](c9-r1-20261029-142449/verification-20261029-152507/lint.xml)实际本轮生成，**0错误16警告**。SDK XML版本警告原样保留，未升级/下载工具。JVM/编译不代替Android动态、TalkBack、字体或IME验证。
- 真实Android截图0、Compose桌面预览0、设计示意新图0；LocalDensity不是系统字体/IME验证。

## 冻结边界、产物检查与待验项

实际diff确认data/domain/platform、业务App/VM/Controller/Owner、Appearance存储/选择面板、Nav3/Routes、Room5/schema、backup4/恢复Token/切换、C6-1媒体读取/安全和C6-2C通道、Manifest/Gradle/依赖/版本签名均未改。Preview11/code27原包15,961,550字节与SHA `5bbf3c1f946fc2da4ab259ab377f8134e222255784fe81e20c7a73337e63cf90`保持；没有新APK或Preview12，没有设备/账本操作。

[普通Debug静态审计](c9-r1-20261029-142449/apk-audit/identity.json)及原aapt2/apksigner输出：正常LithicApplication/MainActivity、`com.cooper.lithic.debug`/code17、原证书29ded26b…7f83b9、无INTERNET；8个变更生产组件及新语义角色DEX标记存在，native条目和字节与旧Preview11一致。只是构建核验产物，未复制成用户交付版。编译源SHA在审计时逐项一致，最终Git app树见绑定记录。

仍待真实Android画面/字体/IME、几何触控与跨页动态等新授权验证，15位矩阵和8新增Android均不登记PASS。没有本轮确定未修的生产源码缺陷；新增/修改的Android断言仅编译，实际运行问题仍未知。已完成本轮桌面门禁，不自授视觉认可或冻结，不生成Preview12。

[原暂缓时源码清单/SHA/执行状态（历史）](c9-r1-20261029-142449/source-preparation.json)。M10三JPEG FAIL、M20平台FAIL/UNKNOWN、M22 BLOCKED、C3调查关闭/运行UNKNOWN、M13-T06、Q03/Q04、G1～G4和全部历史NOT_RUN/NOTES保持。

ANDROID_VISUAL_PENDING / ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED。
