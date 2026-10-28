# C5-R1 独立审查补修回执

2026-10-28。基线`69f5704d98bb53cd8c2858d97ba609450e5c28bb`，分支`codex/lithic-v1-product-completion`。

**C5-R1 FIX READY_FOR_INDEPENDENT_REVIEW / ANDROID_VISUAL_AND_MOTION_PENDING**。原C5-R1独立结论CHANGES REQUIRED保留，本回执仅说明本轮局部整改与桌面验证，等待再次独立审查。

## 两处修正

1. **跨来源假空态**：原`ReviewShell`只传读取提示，趋势呈现未取得Ready数量，旧空快照仍可能生成首次记录引导。现在只传递两类Ready计数作一致性检查；不一致时阻止空态，保留图表区域和“趋势事实尚需核对”提示。有旧趋势时金额、选日及截止日继续来自该趋势自身state，Ready数量不参与金额或事件组合；双方确认空的原入口保持。既有generation作用域不改，也未建立旧结果缓存。
2. **历史事件入口被选日失败阻断**：原范围按钮位于selectedPoint已取得分支。现在展开事件后范围开关始终可用；选日未知用null表示并显示“尚未取得”，没有0条计数或旧选日金额。切全部日期直接使用原`timeline.events`，不重算金额；原排序、每50条分页、ID、所属记录回调及非现金标签保持。

生产差异仅[ReviewShell](../../app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt)、[TotalCostTrendScreen](../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendScreen.kt)、[TotalTrendPresentation](../../app/src/main/java/com/cooper/lithic/feature/review/TotalTrendPresentation.kt)。两个既有测试文件仅追加6个JVM及3个Compose方法，原方法正文和断言逐字保留。

## 本轮验证

| 检查 | 实际结果 |
| --- | --- |
| 定向JVM | **35 PASS，0失败/错误/跳过**：6新方法＋29相关回归，4类；不是重跑整个C5或M22套件。 |
| Debug／AndroidTest源码编译／Lint | 同一离线命令exit0，500.141秒。Lint分析任务本次实际执行，报告因内容未变`UP-TO-DATE`，读取结果为**0错误、21警告**；复用报告保留原mtime和SHA，不冒充新生成文件。 |
| 编译提示 | 两处冗余`!!`警告（日志有重复输出），以及原Navigation与SDK XML版本警告均保留。未屏蔽检查、调整工具链或修改业务断言。 |
| Compose实际执行 | 新增3个方法及原6个方法均**NOT_RUN**；编译不等于视觉、触控或系统字体PASS。 |

见[真实命令/退出码及原始字节摘要](c5r1-fix-20261028-022809/desktop-check/command.json)、[stdout](c5r1-fix-20261028-022809/desktop-check/stdout)、[stderr](c5r1-fix-20261028-022809/desktop-check/stderr)、[结果统计](c5r1-fix-20261028-022809/summary.json)、[新增测试所在JUnit XML](c5r1-fix-20261028-022809/jvm-results/TEST-com.cooper.lithic.feature.review.C5R1TrendPresentationTest.xml)、[实际读取的Lint XML](c5r1-fix-20261028-022809/lint-results-debug.xml)。输入源码SHA与构建前后一致；[边界核对](c5r1-fix-20261028-022809/source-boundaries.json)记录原图表/事件行及旧测试正文对照。

## 安全边界与停止

VM请求票据、Repository/DAO、Owner/generation、MoneyMetric、Nav3、Room5/backup4、金额日期/提醒/媒体/恢复协议及构建身份配置均未改。原Preview8/code24的15,797,555字节与SHA`09ffc3c0a39d055e6fb89b4a738fe1a4ba579687c86c880f864fb428cd056bbd`保持；没有版本递增或新用户APK。

ADB、手机、模拟器、C3、截图和M22-T06/T03/IME执行均0。真实Android视觉、操作及动态仍PENDING；原M10/M20/M22/M13、G1～G4、MEDIA_DISPLAY_PENDING和全部历史FAIL/UNKNOWN/NOT_RUN保持。提交推送后停止，不合并main、不进入M23。
