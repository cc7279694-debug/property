# UI-C2 A段集中样板

**VISUAL_SAMPLE_READY — UI-C2**

**PROTOTYPE_ONLY / APP_VISUAL_PENDING**

来源`9de00d3889180cb1d3c078ade543c75100a4196d`，当前产品分支`codex/lithic-v1-product-completion`。四页生产Compose已落入产品源码；本轮停止供一次集中视觉反馈，未推广其他页面、未生成新用户安装包。

## 看样板

- [离线交互HTML](../design/ui-c2/UI_C2_A_Prototype.html)：四页×浅深8框，以及无记录、长负金额、大字3框；可演示新增/记一笔面板、更多、备注/日期及分项展开。全部为合成设计数据。
- [浅深总览](../design/ui-c2/PROTOTYPE_ONLY-overview.png) · [代表状态](../design/ui-c2/PROTOTYPE_ONLY-states.png)。使用本机已有浏览器离线渲染，已实际打开查看。**不是Compose渲染、MainActivity或APK截图**，大字是浏览器排版模拟，不是系统200%字体/IME/TalkBack通过。
- [原始字节、尺寸和SHA](ui-c2-20261026-031540/prototype-final-observation.json)。无现成Compose Desktop/Layoutlib/Paparazzi/Roborazzi配置，未安装渲染框架、重开旧AVD或新建Web产品。

## 四页变化及真实接线

| 页面 | 实施 | 保留 |
|---|---|---|
| 现在 | 总日均主摘要/两小计；搜索+新增；两类各最多2条真实预览与全部；空库唯一新增；类别按需 | 原当前列表窗口/排序/VM/generation，含历史总数单独标注；0/负值/无当前贡献/读取失败/旧结果分开 |
| 项目详情 | 紧凑身份/日均和持有主指标/次级金额/完整记录线；唯一记一笔+更多 | 编辑、投入/收回、结束/修正、纠错、替换/以旧换新、成本历史及危险删除原保护全部仍在 |
| 新增项目 | 主要字段优先、付款/备注渐进、隐藏错误自动展开定位、底部保存及IME安全区 | 原VM默认/输入/operationId、0与空金额区分、未确认提交/重新读取；没有创建前媒体写入 |
| 回看 | 现有趋势为主、准确读数/日期/分项与精确选日；分析和历史轻入口 | 原图表/负值/峰谷/事件分页及完整趋势路由；仅进入回看按需读，离开暂停，不在现在预算20年 |

主要差异：`LithicTheme/C2Icon`、`HomeShell`、`ProjectDetailScreen`、`ProjectEditorScreen`、`ReviewShell/TotalCostTrendScreen`、`LithicNavigation/LithicRoutes`及小型纯呈现helper。三主入口改真实矢量图标+文字，详情/表单以原来源返回；HOME/REVIEW_HOME仅为UI来源，原requestLeave、dirty/pending/discard和恢复门禁不变。首页成本说明亦返回现在。只选择性复用旧UI主题/12矢量，未merge/cherry-pick旧分支或重画Launcher。

首页复用原列表VM，仍读取既有50条窗口及已加载页，仅显示前2条，非最近活动/全量数量。回看复用既有全事实计算，不谎称固定50条开销。原GenerationUiScope继续隔离旧代模型，新增迟到呈现断言仅编译。

## 真实验证

| 检查 | 实际结果 | 原始证据 |
|---|---|---|
| 定向JVM | 75唯一方法PASS：26新增、49相关回归；不重跑历史大套件 | [逐方法](ui-c2-20261026-031540/jvm-approved-summary.json)、[XML](ui-c2-20261026-031540/jvm-approved/) |
| Debug / AndroidTest源码编译 | exit0；18新增Android/Compose方法编译，运行全部NOT_RUN | [最后命令及退出](ui-c2-20261026-031540/quality-approved-input/command.json)、[stdout](ui-c2-20261026-031540/quality-approved-input/stdout)、[18方法范围](ui-c2-20261026-031540/android-compile-only.json) |
| Lint | 0错误、17警告 | [实际XML](ui-c2-20261026-031540/lint-approved.xml) |
| 包/签名/冻结边界 | 原Debug证书、默认身份/版本配置不变；Room5/backup4/data/domain/写入/Owner/媒体协议不改；旧preview18字节未变 | [最终静态核验](ui-c2-20261026-031540/artifact-approved.json)、[测试输入绑定](ui-c2-20261026-031540/input-binding.json) |

新增呈现断言先于实现；没有故意破坏既有正确行为或冒称业务RED→GREEN。首轮AndroidTest编译因错误导入`assertDoesNotExist`失败（节点断言仍保留），[首次stderr](ui-c2-20261026-031540/quality-initial/stderr)永久保留。移除错误导入后通过；新增图标参数顺序Lint警告亦修正，不改业务断言。历史测试/失败不改写。

## 限制与停止

- **封面接入PENDING**：当前产品基线没有页面可用的MediaRepository/封面管理入口，仅有媒体元数据与备份能力。无图用图标；有coverAssetId但没有受校验读取时明确“封面暂不可用”。仅提供纯UI插槽，没有裸路径/主线程解码/新缓存，也没有把旧M10媒体实现合并。不能宣称已有图片显示或封面管理已完成。
- Android视觉、点击/返回/面板/IME/系统字体/辅助技术/换代实际运行均NOT_RUN。18方法覆盖面板关闭零动作、原回调、错误展开、未知提交、旧代迟到隔离等；编译不充当设备证据。旧旅程部分定位仍基于旧布局，后续需在授权范围内调整定位，不改业务断言；未重开M22-T06/T03/IME循环。
- 全局主题传递到其他页面，但未进行B段布局重做。当前APK仅普通Debug检查产物，未构建新preview/Release/M22验证APK；没有安装/升级或新证书。实际App视觉与未来最终MainActivity/APK逐页一致性仍PENDING。
- M10三JPEG历史FAIL、M20平台FAIL/UNKNOWN、M22 BLOCKED、M13-T06与G1～G4及全部历史NOTES/NOT_RUN保持。没有ADB、AVD、手机、个人账本或受保护现场操作；不合并main、不冻结、不进入M23。

下一步只等待一次集中视觉反馈。确认前不自动推广订阅、列表、搜索、设置或备份恢复深页。
