# M19 类别分析、年度趋势与客观回顾执行契约

## Goal / Sources

用户本轮完整附件已明确授权直接实施，从M18冻结`fdee68314dc03a6385a70250e3b456423557186b`建立`codex/m19-category-annual-review`；起始工作区干净、本地/远端来源相同、目标分支不存在。按[原M19计划](original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m19.md)、[产品](original-v1/lithic_v1_plan/docs/specs/PRODUCT_SPEC.md)、[C02/C05～C06/C08～C11/C13～C14](original-v1/lithic_v1_plan/docs/specs/DOMAIN_CONTRACT.md)、[U01～U03/U08～U09](original-v1/lithic_v1_plan/docs/specs/UI_SPEC.md)、[A03～A06](original-v1/lithic_v1_plan/docs/specs/ARCHITECTURE.md)执行。归档原件不改，不再次等待契约授权。

## Scope / Architecture

- 复用ReportRepository一次同generation协调器/Room快照，非破坏性扩展LedgerSnapshot：原始金额事实具有稳定ID、所属记录、三种类型、业务日、Long分、非现金及操作/交易身份；记录名称/当前类别关联独立携带。禁止从TotalCostEvent中文或金额符号推断付款。
- AnnualReportEngine按occurredOn/paidOn与业务截止日聚合，项目投入/收回/净额、订阅真实付款分列；金额checked add/subtract，保留0/负值和事实计数；身份或关联/完整性不明拒绝报告。
- CategoryReportEngine复用冻结Overview当前贡献，历史与年度按同快照当前类别重分类；所有当前/历史记录可进入准确详情；含折抵标注，不造第二笔取得总价。
- 回看新增类别/年度入口，首页类别可直达。两个结果页面使用简洁条形与精确文字金额；年度/类别选择、50条显示窗口、错误/旧结果/空年/真实零分开。共享generation作用域读模型、票据围栏、写后/跨日/前台/恢复刷新，不写报表缓存。
- 保留Nav3的dirty/pending/恢复守卫、类型化记录身份及正确返回来源；不新增底部Tab或类别编辑入口。

## Constraints / Exclusions

Room schema3、backup format2、M02/M03冻结金额日期状态算法、M16交易保护、M12恢复及媒体协议不变。无新依赖/升级，无平均寿命/评分/消费建议/预算/AI/CSV/云；不混入M10或独立UI-C1。版本code14/0.1.0-m19，仅内部Debug候选。M10延期/三JPEG历史FAIL、M13-T06延期/G1、Q02完整16组及所有历史NOTES保持，M19不自动通过G2。

## Execution / Verification

1. 先补纯规则RED，再实现类型化映射与两引擎；M19-T01～T04/T06使用真实发生年、13000/3500/9500、跨年预付与M16三笔、0/负值/溢出/客观输出断言。
2. 实现VM/页面/导航及首页类别入口；保持选择意图和同一快照结果，类别消失明确通知。
3. 定向JVM及M14/M16/M18相关回归、Lint、Debug App/Test构建。Android真实Room覆盖>50/一致快照、真实类别改名合并(M19-T05)、纠错删除、format2往返与旧代拒读；VM/Compose/Nav3/只读及少量截图。原有通过行为直接回归，不扩历史全量。
4. 设备只允许再次严格核验原`Lithic_M18_API36_20261021 / emulator-5692`在线实例/boot/目录/包身份后同Debug证书更新，以plain Application隔离Runner新M19合成根验证；原实例退出即NOT_RUN，不创建/重启/停止任何AVD，不访问其他设备或真实记录。LocalDensity200%、注入日期、受控文件目标如实标注。
5. 一份[M19验证报告](../evidence/M19-verification.md)、必要原始证据/截图、当前记忆及checkpoint；不产大型ZIP/整仓清单/重复APK。只提交推送当前M19分支，READY_FOR_REVIEW后停止，不自行冻结/合并main/M20。
