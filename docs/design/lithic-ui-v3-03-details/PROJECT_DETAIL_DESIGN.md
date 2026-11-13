# 项目详情设计

`DESIGN_REVIEW_ONLY — NOT_PRODUCTION_UI`

## 阅读顺序

| 位置 | 设计与现有能力 |
| --- | --- |
| 顶栏 | 系统返回、页面标题「项目详情」、更多项目操作。名称作为正文一级标题完整换行。沿用首页的黑石品牌识别、线性图标和三主题角色，次级页面优先把空间留给记录。 |
| 档案身份 | 一张居中的 4:3 封面画框，原图按比例完整放入，横图与竖图都保留边缘。正常字号示意上限 240×180dp，图片下方紧接项目名、类别、文字状态与累计持有天数。无封面采用紧凑类型占位，避免将成本推到首屏外。 |
| 日均成本 | 紧接身份区，以单一大读数突出「当前日均成本」，金额和 `/天` 相邻。结果截至日期同区展示，旁边提供「项目成本口径」。已结束用「截至观察日的日均成本」，保留真实尾款或退款提示及冻结天数事实。 |
| 金额事实 | 一组轻分隔区展示累计实际投入、累计实际收回、净投入。有非现金折抵时沿用现有累计指标限定，逐笔明确「非现金折抵」。净投入允许 0 或负数。 |
| 真实金额记录 | 按实际发生日升序，日期相同按既有 ID 次序。每笔同时展示发生日、方向及角色、现金或非现金、金额和已有说明。普通纠错、关联说明、结束关联收回使用各自原入口。结束后补录仍按真实发生日排序。 |
| 资料与历史 | 保留开始拥有日、已有结束日期、结束原因和备注，保留「查看成本历史」及换新保护提示。成本历史仍进入原页面，可按既有能力选日期和查阅结束后结算。 |
| 底部主动作 | 固定动作区「记一笔」，展开原金额类型面板选择「记录投入」或「记录收回」，随后进入原表单。已有结束记录仍可补录实际尾款或退款，持有天数不继续增加。 |

主读数取现有 `ProjectDetail.cost`，日期取 `asOf`，事件使用完整列表。首页的全局总投入与此处单件净投入各自独立。跨段统一复用 `MoneyMetric` 的金额、单位、完整值与复制入口。[项目详情][detail] · [详情菜单和文案映射][presentation] · [详情快照][repository] · [事件查询次序][dao] · [初始投入校验][initial] · [成本历史][chart]

## 「更多」与照片动作

- 更多按原资格提供编辑项目资料、结束项目或修正结束记录、替换链与纵向对比、记录以旧换新、查看成本历史。永久删除独立成组，带清晰的后果提示。关联换新事实时维持删除禁用及金额、日期约束。
- 无封面时显示类型标识，有照片操作资格才显示「添加照片」。读取中保留已分配的框和「正在读取封面」语义。不可用显示错误标识与原有封面状态说明。已有关联且具资格时保留「更换照片」「取消封面显示」。取消显示保留媒体历史。Fit 仅为本次项目详情提案，封面读取、API 36 系统照片选择器及实际写入资格继续走原组件与账本保护。[详情接线][navigation] · [封面读取与状态][cover] · [照片面板][photo]
- 「结束项目」仍为独立全页表单。项目名、结束日期、结束原因，以及可选实际收回和真实到账日同屏可核对。保存后按原路由回到详情。到达结束日期后可在历史档案查看。永久删除走独立危险确认，写明金额记录一同删除、历史统计重算及不可撤回。[结束表单][end] · [删除确认][delete]

## 状态与画板样本

| 状态 | 呈现 |
| --- | --- |
| 正常 / 仅无金额记录 | 正常显示真实读数与事件。现有渲染有「没有金额记录」分支，画板用合成 UI 状态审查该分支。合法账本写入和备份校验要求保留初始投入，即使为零。身份、日期和适用动作仍存在。 |
| 读取失败，有同项目旧快照 | 页首标记「暂时无法刷新，以下为上次读取记录」，保留旧截止日、原金额、状态与列表，提供「重新读取」。 |
| 读取失败，无快照 / 已确认缺失 | 失败保留返回及重读入口，金额用 `—` 或省略空分区。缺失仅对应成功读取后找不到项目，提供返回原入口提示。 |
| 尚未开始 / 已记录结束日期 / 已结束 | 使用现有状态映射。未开始的日均成本无值。已记录结束日期和已结束分别显示。后续真实结算进入金额列表，已结束读数仍有结果截止日。 |
| 200% 字体 / 长名称 / 大金额 | 日期与标签可换行，累计投入、累计收回和净投入按行纵排，事件金额进入独立行。底部动作占用自己的布局空间，滚动至末条可完整显示。 |

正常样本使用「合成横向相机」，类别「其他」，持有中。2026-07-16 开始，截至 2026-11-13 含首日共 121 天。初始投入 ¥1,000.00（07-16），投入 ¥400.00（08-01），收回 ¥200.00（10-26）。累计实际投入 ¥1,400.00，累计实际收回 ¥200.00，净投入 ¥1,200.00，日均 ¥9.92/天。该明细是配对设计的合成事实。原首页 Android 截图用于核对视觉系统和已展示读数。[已冻结首页](../../evidence/lithic-home-final-polish-09-retry/projects-pure-white.png) · [详情状态][state] · [日均成本引擎][engine]

[detail]: ../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt
[presentation]: ../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailPresentation.kt
[repository]: ../../../app/src/main/java/com/cooper/lithic/data/repository/ProjectRepository.kt#L60
[dao]: ../../../app/src/main/java/com/cooper/lithic/data/local/ProjectDao.kt#L58
[chart]: ../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectCostChart.kt
[navigation]: ../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L581
[cover]: ../../../app/src/main/java/com/cooper/lithic/feature/media/CoverPreviews.kt#L125
[photo]: ../../../app/src/main/java/com/cooper/lithic/feature/media/PhotoLiteCoverPanel.kt
[end]: ../../../app/src/main/java/com/cooper/lithic/feature/project/EndProjectDialog.kt
[delete]: ../../../app/src/main/java/com/cooper/lithic/feature/project/DeleteProjectDialog.kt
[state]: ../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt#L571
[engine]: ../../../app/src/main/java/com/cooper/lithic/domain/project/ProjectCostEngine.kt
[initial]: ../../../app/src/main/java/com/cooper/lithic/data/backup/BackupBusinessValidation.kt#L146
