# 业务事实与保护

`DESIGN_REVIEW_ONLY — NOT_PRODUCTION_UI`

## 读数对应关系

| 界面事实 | 设计约束 |
| --- | --- |
| 项目日均成本 | 截至观察日的净投入除以含首尾日的持有天数。结束日计入持有期。从结束日期起退出当前贡献。结束后补录仍以发生日计入相应观察期的净投入，持有天数冻结。 |
| 项目投入、收回、净投入 | 投入、收回各自累加，再得净投入。逐笔金额为真实发生记录，方向由类型体现。换新包含旧物折抵收回、新物现金补款、新物折抵投入，折抵以文字标记非现金。金额和日期保持成组保护。 |
| 订阅历史平均日成本 | 截至观察日已确认实际付款除以标准折算付费天数（月／季／年为 30／90／365）。已付款的未来覆盖可计入折算分母。另保留实际已过去的有效天数。没有分母时显示 `—`。 |
| 当前套餐与当前标准日均价 | 真实生效日含当日、付费到期边界不含当日。只在启用且有效覆盖时展示当前套餐。暂停、停止、到期或未有当前覆盖时显示「当前无有效套餐」及现有当前日均贡献 `¥0.00/天`。未来已确认付款有「尚未生效」文本状态。 |
| 订阅付款与状态历史 | 以真实付款日降序、同日按 ID 升序保留分页，独立显示实际付款日、覆盖区间、周期及金额。提醒到期日继续在现有详情说明可查。状态事实按原结果显示日期及原因，现有修正入口按资格提供。 |
| 未知、真实零、陈旧 | 读取失败不可当空记录。详情与订阅付款历史分别保留各自同记录旧快照及重读动作。零值由领域结果明确给出。金额、日期及状态均沿用既有显示规则。项目空事件、订阅空付款画板仅对应现有 UI 防御分支。合法项目保留初始投入，合法订阅保留至少一笔实际付款。 |

[项目计算][pengine] · [换新写入][trade] · [订阅计算][sengine] · [付款分页][sdao] · [订阅事实校验][srepo] · [订阅详情][sdetail]

## 写入、返回与确认

| 场景 | 必须保留的内容及行为 |
| --- | --- |
| 返回与来源 | 顶栏返回和系统返回共用现有 `requestLeave`。保留首页、当前/历史列表、搜索、回看、趋势、类别、年度、长期复盘及项目替换对比来源。完成操作回原记录。详情沿用已有读写路由。 |
| 未保存表单 | 「放弃未保存修改？」保留「继续编辑」「放弃修改」。关闭确认保留输入。明确确认后再次检查恢复或提交状态才可离开。 |
| 提交中、结果未确认、回读失败 | 提交中／未确认先完成现有核对，不换对象、不重复创建提交。写入已确认但回读失败时说明已保存并重试读取。仅在现有流程允许时核对或重试同一提交身份。冲突重读按原交互说明会替换旧输入。 |
| 项目结束、纠错与删除 | 结束保留项目及历史，实际到账才记录收回。已有结束关联收回沿原表单修正，无关联时通过「记一笔」补录。永久删除单独确认，显示项目名称、历史重算及不可撤回。有关联换新时沿用禁用及只修正允许说明等保护。 |
| 订阅暂停、恢复、停止、重新开通 | 管理面板和操作确认保留「暂停、恢复或停止只改变砺石中的记录；取消服务商自动续费需到服务商处操作」。停止选择到期后或原规则允许的提前日期。停止日期确认后固定，可按原规则修正或取消尚未生效的计划。恢复、重新开通时，沿用已有覆盖或要求确认真实续费的分流。 |
| 订阅错误付款 | 付款操作区明确区分修正与永久删除错误付款。仅删除目标误录付款，重算历史，无法撤回，最后一笔真实付款保留。真实退款与服务商取消均不可用删除模拟。 |
| 照片、备份和恢复 | 封面读取失败单独反馈。项目照片写入限原资格，照片操作待核对继续阻断相关离开或覆盖动作。取消封面显示保留历史媒体。原恢复阶段锁、GenerationScope 和 Owner 资格继续约束导航与写入。 |

[Nav3 来源][routes] · [返回与恢复守卫][nav] · [未保存与待核对弹窗][leave] · [项目写入仓库][pwrite] · [订阅操作确认][sdialog] · [订阅写入仓库][srepo]

[pengine]: ../../../app/src/main/java/com/cooper/lithic/domain/project/ProjectCostEngine.kt
[trade]: ../../../app/src/main/java/com/cooper/lithic/data/repository/ReplacementRepository.kt#L261
[sengine]: ../../../app/src/main/java/com/cooper/lithic/domain/subscription/SubscriptionCostEngine.kt
[sdao]: ../../../app/src/main/java/com/cooper/lithic/data/local/SubscriptionDao.kt#L78
[sdetail]: ../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt
[routes]: ../../../app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt#L213
[nav]: ../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L288
[leave]: ../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L732
[pwrite]: ../../../app/src/main/java/com/cooper/lithic/data/repository/ProjectWriteRepository.kt
[sdialog]: ../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionStateDialog.kt
[srepo]: ../../../app/src/main/java/com/cooper/lithic/data/repository/SubscriptionRepository.kt
