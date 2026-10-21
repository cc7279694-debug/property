# M16 执行契约

来源：用户本轮附件、M15冻结基线661d2b5addc4eca377756f08edb84668ba700736及原始M16计划（归档保持不变）。本轮分支codex/m16-replacement-trade-in；仅实现M16并停止独立验收。

## 目标与边界
实现项目替换链、纵向对比、真实以旧换新；Room2→3、备份1→2。沿用M02/M03算法、唯一generation/owner/协调器、Nav3离开保护和M12安全切换。不得合并M10/UI-C1、修改订阅/金额公式、增加依赖或进入M17。M10延期、M13-T06延期、G1未整体通过均继承。

## 持久事实与身份
替换边为独立UUID，前任/后任唯一、同类型项目外键、禁止自指和环；纯关联不改生命周期和资金。普通中间项目删除只断相邻边，不重连。
交易使用commandId为exchangeGroupId；固定派生是v2持久契约：UTF-8 `lithic-m16:$commandId:$role`经UUID.nameUUIDFromBytes；角色为`event:old-credit/new-cash/new-credit`与`operation:old-credit/new-cash/new-credit`，新项目`new-project`、边`replacement`，不得随意改命名空间/角色导致既有组无效。固定参数和角色派生不同实体ID/operationId：旧END_SETTLEMENT/RECOVERY/TRADE_IN_CREDIT，新INITIAL/OUTLAY/CASH（允许0），新REGULAR/OUTLAY/TRADE_IN_CREDIT。旧收回与新折抵金额相等，同日；新取得成本=现金+折抵，禁止额外INITIAL总价事件。旧结束、新身份、三事件和可选边一次事务。金额使用分、Money上限和checked累计；精确完整目标事实才识别重放。pending保留原命令/原始输入，旧代不能写新代。

## 用户明确确认的成组保护（本轮答复）
现有单笔纠错、结束修正、取得日期修改及项目删除会拆散交易组。用户已同意最小限制：参与交易的项目禁止删除、改变取得/结束日期，禁止单独改变成组金额/日期；名称、类别、项目备注、普通后续资金事件及替换关系仍可操作。新项目首次正常结束及再次作为下一笔换新的旧项仍可操作；已经记录的结束日期不允许单独改动。整组纠错/撤销未授权，本轮不实现。断替换边不撤销交易。此限制是用户确认的新整合边界，不改写归档规格。

## 兼容与安全
旧事件迁移CASH/null，旧项目无自动关系；保留schema1/2原字节及显式1→2→3。生产打开预检识别v3真实Room身份和结构。备份v2严格新增字段/关系/组完整性；真实v1先按原格式验证后显式升级。保留旧恢复proof原hash口径，pointer/ready/proof协议不因业务格式升级盲目涨版本。新旧图片原字节及保守残留规则不变。

## 验收
落实M16-T01～T06、每个事务边界回滚/完整重放/冲突、真实迁移和生产重开、真实旧v1→新v2八表往返、损坏备份保护旧集、关联写后/换代刷新、导航pending/双提交和大字体。
仅操作经完整身份核验且仍在线的Lithic_M14_API36_20261021/emulator-5672；否则Android NOT_RUN。使用plain Application隔离runner和本轮新合成根，不打开默认账本，不启停/重建/清除任何AVD或旧现场。构建/JVM不替代Room、事务及恢复的Android实证。

交付短M16-verification、必要原始证据/截图、checkpoint、CURRENT_STATE/验收登记及GitSHA；READY_FOR_REVIEW — M16，不自行冻结。
