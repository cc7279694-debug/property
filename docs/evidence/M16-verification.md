# M16 — 替换链与以旧换新

**READY_FOR_REVIEW — M16；独立验收PENDING。** 来源`661d2b5addc4eca377756f08edb84668ba700736`，分支`codex/m16-replacement-trade-in`。[契约](../planning/M16_CONTRACT.md)包含用户明确同意的成组保护，不改归档原规划。

## 实际交付

替换链支持关联、解除与纵向对比，保持一前任/一后任、无环/自指/缺项。纯关联不结束旧项目；普通中间项目删除只断相邻边，不推断重连。以旧换新使用固定命令/角色身份、完整旧事实CAS与单一Room事务：旧回收3000、新现金5000＋非现金折抵3000，新取得成本8000、总净新增5000。保留唯一INITIAL、精确整组重放与UNKNOWN固定信封；金额计算复用冻结引擎。

交易项目删除、取得/既有结束日期和成组金额/日期独立纠错受到保护；名称/类别/备注、普通后续金额、首次正常结束及替换关系仍可用。整组纠错/撤销是用户同意的本轮排除项。

Room3新增replacements及basis/group字段，显式1→2→3，旧schema1/2 Git原字节不改。备份v2八表严格校验/完整往返；真实v1原格式验证后显式补CASH/null/空关系。M12旧五字段proof继续按原v1序列化核验，新proof明确dataFormatVersion2；pointer/ready协议仍1。未知残留与媒体原字节规则保持。

关键代码在`ReplacementRepository/ReplacementStoredFacts/TradeInFacts`、`ReplacementRules`、`ReplacementViewModel/ComparisonScreen/TradeInEditor`、migration/schema预检及backup DTO/validator/proof；详情、导航、首页/回看只补必要整合，不复制金额公式。

## 本轮实际验证

| 原案例 | 实际结果 |
|---|---|
| M16-T01 | PASS：A→B→C；自指、环、重复前任/后任、缺失与陈旧事实拒绝。 |
| M16-T02 | PASS：纯关联/解除不改金额和持有状态。 |
| M16-T03 | PASS：3000/5000/3000实际三事件与8000/净增5000；零现金、同日及重复交易边界。 |
| M16-T04 | PASS：六个SQL故障边界整组回滚、完整目标重放/冲突、身份与组保护。 |
| M16-T05 | PASS：普通中间项目删除断边、不重连；失败回滚与陈旧关系拒绝。 |
| M16-T06 | PASS：真实v1原件→schema3，v2完整八表/关系/三事件/设置/媒体往返，坏备份保护旧集；旧schema2＋五字段proof＋SWITCHING实际生产打开兼容。 |

**252 JVM PASS（46新增＋206受影响回归）；50 Android PASS（34本模块＋16相关回归），总302个唯一方法，0失败/错误/跳过。** 不重复计入多次运行或7张截图。Android含真实迁移5、交易19、备份5、VM3、旅程2、代作用域3、首页6和恢复守卫7。[结果及身份](m16/results.json)、[原始证据入口](m16/README.md)、[截图7张](m16/screenshots/README.md)。

新增行为的RED及编译/环境失败均保留。实际修复了断组导出仍成功、同刻/回拨时提交修订时间及pending返回丢比较上下文三处问题；不改失败断言。最终同一264份工作树输入构建前后聚合相同；JVM/Lint/App/Test构建成功6m51s，Lint0错误9警告。沿用JDK17.0.20.1、Gradle8.14.3、AGP8.13.2、Kotlin2.3.10、SDK36/min26，无依赖升级。App DEX核对无测试类、重复定义或未冻结M10 Worker/Stage V2。[APK审计](m16/apk-audit.json)、[范围审计](m16/scope-audit.json)。

## APK与设备

- App：`com.cooper.lithic.debug`，code11 / `0.1.0-m16-debug`，14,117,110字节，SHA `2dcc54802720c6a5c3d6cebc797dca6db228757506b3acd6a2e86b892cf4fe41`。
- Test：`com.cooper.lithic.debug.test`，code0/版本名空，3,175,312字节，SHA `ba531c1c51cc6bdbee3ad4084bfa4bfd93777307c3e17248cec71563a1eca00d`。
- 本地绝对路径分别为`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`和`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`。APK/密钥不入Git，Debug候选不作长期发行版。
- 仅原`Lithic_M14_API36_20261021/emulator-5672`，API36/x86_64，UID10216/10217；同既有Debug证书、保留数据更新App/Test各3次（RED、首轮GREEN、最终修正版）。最终50项全部使用上述最终字节/同一plain Application隔离runner。原实例及boot保留在线，no_backup空、设置与M15期末值相同；未开默认账本，无AVD启停/重建/清除或其它设备操作。[结束保护](m16/device-end/summary.json)。

## 限制与停止

没有未解决的本模块确定性失败；等待ChatGPT独立审查。API26/真机/OEM、Release/稳定签名、真实个人记录、MainActivity、系统字号/完整IME/TalkBack、真实午夜、大规模长期性能及schema3/format2真实kill/LMK/断电均NOT_RUN。旧proof的SWITCHING是合成兼容验证，不冒充M12三个真实终止点重跑；文件目标往返不是新SAF验收。超大真实DB溢出注入未做，纯引擎/组边界与checked累计已定向回归。

M10 PENDING/DEFERRED及三历史JPEG FAIL、M13-T06延期/G1未整体通过、Q03～Q06和全部历史NOTES保持；不合并main、不进入M17。
