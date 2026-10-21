# M17 执行契约

来源：用户本轮完整附件（SHA-256 `428d95b7c8030a0822037f3da6cc55d1caf9a1a2d1c3f61fa5c3f7829bc17684`）、[原M17计划](original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m17.md)及适用原规格；归档不改。起点M16冻结回写`ca289f12a31ce67882a1254f6dbd71a80cad18dc`，本地/远端一致、干净且目标分支不存在后创建`codex/m17-item-cost-charts`。本轮集中实施，不另等重复授权。

## Goal / Scope / Dependencies

在现有项目/订阅详情接入只读历史图，复用冻结M02/M03引擎、完整详情读取、唯一generation/协调器、现有金额展示和Nav3守卫。项目按观察日dailyRate；订阅按观察日currentRate＋displayStatus。不增加DAO每日表或持久图缓存，不新增依赖/底部Tab/主题。

## 历史边界与精度

项目开始前无有效日均；每个点调用ProjectCostEngine.evaluate(snapshot,date)，只按occurredOn，不按录入顺序。持有曲线截至min(截止业务日,真实结束日)，结束日的单项成本不因退出首页而变0。结束后仅真实结算日期的独立终值调整，不与持有段连线，不延长分母；任意过去日期均可回原引擎精确查询。预付款保留事件但不制造开始前曲线。折抵只读M16已有事件/basis，不能另加取得总价。

订阅点调用SubscriptionCostEngine.evaluate，标准30/90/365及原standardDays不变，paidOn与覆盖/状态边界都参与；提前付款不提前改价。绘制阶梯，暂停/停止/到期/无覆盖为0并有状态文字，不连起断档正价格。截止日之后不绘历史/预测；未来计划/已付未生效以明确文字保留。

Long分和BigDecimal保留到展示层，只有坐标使用浮点。精确选日不依赖像素反算金额或显示字符串。零、负数、混合、极小非零、单点都有确定坐标；同日事件完整分组，“该日相关事件”不声称小时级因果。

## 最小实现与接口

- ProjectTimelineEngine/SubscriptionTimelineEngine：build(snapshot,asOf,maxDisplayPoints=240)，同日纯计算point(snapshot,asOf,date)。240仅均匀基础显示点预算，起止/事件/付费与状态边界及必要邻点额外保留，完整事件不丢。日期集合不枚举全部天数；每个保留点仍需原引擎扫描/校验事实，成本随事实量×显示点增长，不宣称恒定或整体O(E+240)。
- ItemTimelineRepository复用已有ProjectRepository/SubscriptionRepository完整详情，单次同代一致快照/固定asOf返回事实，再后台纯计算；不每个点查库、不把列表第一页当历史。返回完整snapshot/实体元数据/typed timeline供精确选日。
- generation作用域ItemTimelineViewModel：后台读取/计算、请求票据拒绝迟到发布；写后/跨日/返回前台刷新，恢复换代清旧模型。读错保留旧图且明确错误，删除明确missing，不伪0/空账本。选择状态不进备份。
- Compose Canvas与纯参数项目/订阅页面：真实日期间距、元/天/零线、选点/日期输入及可读事件列表。默认全跨度＋日期选择，完整事件按50条窗口显示；不另增未明确的范围种类或M18通用采样系统。切换点/事件窗口只读。原详情来源（含替换对比）保留，不绕过pending/dirty/恢复守卫。

## Out of Scope / Constraints

Room schema3、backup2、原金额/日期/订阅算法、M16交易组保护、恢复/媒体协议不改。不做总成本历史、类别/年度图、通知、预测/评分/估值/换新建议、交易组撤销、WebView/大框架或M18。不合并M10/UI-C1候选，不配置签名/真实录入。M10 PENDING/DEFERRED、M13-T06 NOT_RUN/DEFERRED及G1未整体通过保持。

## Acceptance / Verification

落实原M17-T01～T05：发生日历史/Q02精确示例，阶梯与预付/暂停/到期/恢复/未来停止，同日完整可读事件，负/零/小额/单点，持有截止与结束后结算/无预测。补完整>50事实、读后不写、写后/跨日/换代/迟到隔离、M16两项目各计一次、合成format2往返同日重算和旧代拒读。新增行为真实RED→GREEN，已有正确行为直接回归。

仅定向JVM/相关冻结引擎/导航/交易保护回归、Lint和Debug App/Test；不跑无关全量或M10/M12故障矩阵。Android仅经重新核验且仍在线的`Lithic_M14_API36_20261021/emulator-5672`，既有证书更新，plain Application runner、本轮新合成根；不打开MainActivity/default或私人账本，不操作其他设备、不启停/重建/清数据/强杀。实例不符或消失则设备NOT_RUN，继续离线工作、不自行修环境。仅清已关闭且精确属于本轮的合成根；不删旧图/未知目录。

少量实际截图注明隔离ComponentActivity、LocalDensity360dp/200%及文件目标；不冒充系统字号/TalkBack/真实午夜/新SAF。Q02仅登记本轮M17集成，不关闭M18/完整跨模块闸门。

## 执行顺序与停止

1. 纯时间线与坐标行为RED→GREEN，冻结共享接口。
2. 完整事实只读Repo/VM/Nav3接入、语义/Room/往返与页面旅程。
3. 同组最终构建输入、必要设备证据、状态/矩阵/Q02实际集成登记、短报告/checkpoint、范围/链接/敏感检查后提交推送。

只推`codex/m17-item-cost-charts`，交付READY_FOR_REVIEW — M17后停止；不自行冻结、不合并main或进入M18。
