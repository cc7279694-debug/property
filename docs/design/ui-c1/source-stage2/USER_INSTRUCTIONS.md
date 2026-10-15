# 砺石 LITHIC — UI-C1 阶段2正式执行指令

## 一、正式授权与任务目标

用户已实际查看 UI-C1 阶段1精修版27张截图，并正式确认视觉效果。

ChatGPT 独立视觉复审结论：**PASS WITH NOTES**。

现授权执行：

- Task6：订阅页面、回看、设置及其余主页面
- Task7：现有表单、弹窗和业务操作界面一致化
- Task8：最终截图、完整测试、证据与独立验收交付

这是 UI-C1 阶段2的实施授权，不是 M10 验收授权，也不是 M11 开发授权。

最终目标：**让砺石所有已有页面与已确认的阶段1样板保持一致的视觉品质，同时不降低现有业务、安全及测试标准。**

不得重新要求用户选择深浅设计，不得另起视觉风格。

## 二、仓库、分支与基线

仓库：`cc7279694-debug/property`

工作目录：

`E:/CODEX/.codex/worktrees/lithic-ui-c1/砺石新APP`

唯一授权开发分支：

`feat/ui-c1-visual-redesign`

本轮已确认的来源提交：

`f000639c62fce71be0a9240425c8e27b1a5ed974`

开始前必须实际执行 Git 检查，核对本地HEAD、远端HEAD、分支、工作区及worktree归属。如果已有新提交、未提交修改或其他工作在进行，先检查差异并报告，不直接覆盖或重置。

必须阅读：

- `AGENTS.md`
- `docs/CURRENT_STATE.md`
- `docs/design/UI_C1_SPEC.md`
- `docs/planning/UI_C1_PLAN.md`
- `docs/evidence/UI-C1-refinement-verification.md`
- `docs/evidence/ui-c1/refinement/GALLERY.md`
- `docs/evidence/M10-JPEG-supplement-verification.md`
- 当前架构、工具链锁和适用的冻结业务规格

原文档里“Task6～Task8未授权”的旧状态，已由本次用户明确授权更新。只更新当前阶段授权记录，保留历史记录的原貌。

阶段1精修版本是视觉基线，不得无故改变首页、项目列表、详情的已确认布局。

## 三、严格保护范围

以下内容禁止修改：

1. Room schema v2、实体、DAO、Migration及数据库版本。
2. 金额解析、Money、BusinessClock、领域计算引擎和统计口径。
3. ProjectRepository、SubscriptionRepository的业务读写及事务协议。
4. 唯一owner/coordinator、DataGenerationStore、持久化代际协议。
5. 图片导入、JPEG/PNG校验、私有资源提交、共享引用及清理协议。
6. Nav3序列化路由身份、操作固定ID、原始SavedState信封及三VM业务守卫。
7. applicationId、versionCode、签名证书、minSdk26/target36、权限及冻结依赖。
8. M00～M09历史冻结记录、原130案例及原16组跨模块边界证据。

本轮只授权 Compose展示层、有限的展示组件、必要的UI装配、相关UI测试及文档证据修改。

不新增服务器、登录、网络、AI、云同步、数据分析、搜索或未批准功能。

如果某个视觉目标必须修改上述保护区，停止该部分，提交原因与替代方案，不自行扩大权限。

## 四、Task6：订阅、回看与设置

### A. 订阅服务列表

重点文件：

`feature/subscription/SubscriptionListScreen.kt`

沿用阶段1项目列表的视觉系统：

- 简洁标题栏、返回和新增图标。
- 统一卡片圆角、封面缩略图及状态标签。
- 名称、状态、付款相关时间和成本具有明确主次。
- 深浅主题使用相同信息架构。

必须保留现有订阅管理分组：

- 订阅中
- 暂停中
- 已到期

已停止订阅仍从回看中的历史入口进入，不重新塞进当前管理列表。

保留实际付款日期排序、稳定身份、分页、继续加载、错误重试及旧数据提示。

不能从设计图增加未实现的搜索、全局筛选或伪筛选按钮。

封面继续使用已实现的有界CoverPreviewLoader。无图和损坏图片必须区分。

### B. 订阅服务详情

重点文件：

`feature/subscription/SubscriptionDetailScreen.kt`

参照已确认的项目详情，采用：

- 顶部封面或服务图标
- 名称与真实状态
- 主要成本数据卡
- 当前套餐独立模块
- 付款历史
- 状态历史
- 备注与其他基本信息
- 明确的业务操作区

**严禁混淆以下指标：**

1. 历史平均日成本。
2. 当前标准日均价。
3. 折算付费时长。
4. 实际已经过去的付费有效天数。

标准周期30/90/365天的口径保持不变。

已支付但尚未生效的未来周期，不得显示成当前已经使用的时长，也不能提前冒充当前有效套餐。

暂停、到期、停止、未来停止计划等状态必须使用原有领域引擎结果。

以下已实现操作必须继续可达：

- 真实续费
- 暂停与恢复
- 停止记录及提前停止
- 修正停止计划、取消未生效停止计划
- 同身份重新开通
- 付款纠错及安全删除
- 状态事实纠错
- 原有历史记录和分页

不得为了精简界面删除这些操作。

停止订阅之前，必须保留说明：

**停止本App记录不等于替用户向服务商取消订阅。**

不得添加不存在的通用“编辑订阅”功能。

### C. 订阅历史页

重点文件：

`feature/subscription/SubscriptionHistoryScreen.kt`

与新的订阅列表使用相同卡片和状态视觉语言。

保留原身份、已停止事实、真实付款与状态历史。

空历史、加载失败及旧数据显示必须明确区分。

停止不等于删除；重新开通必须进入已有同身份流程。

### D. 回看页

重点文件：

`feature/review/ReviewShell.kt`

本阶段只突出两个真正可以使用的入口：

- 历史档案
- 已停止订阅

使用统一图标、标题、简短说明和明确点击区域。

不得显示大面积“尚未开放”的占位文案。

成本趋势、类别分析、年度回顾等未来模块暂时隐藏UI入口，但不能删除路线图中的未来规划。

没有可靠的历史数量来源，就不要显示猜测数量。

不得把当前没有历史记录误写成用户全部账本没有记录。

### E. 设置页

重点文件：

`feature/settings/SettingsShell.kt`

使用统一的设置列表组件，主要分组为：

**记录与数据**

- 实际记录时区
- 本地保存说明
- 当前数据存储风险说明

**关于**

- 当前实际安装版本
- 真实可用的应用信息

只读项目不能添加假箭头、伪开关或不能工作的入口。

版本信息从实际构建获得，不硬编码设计图版本。

继续保留醒目的开发阶段提示：

“当前仍是开发体验版，完整备份与恢复尚未完成，请勿将重要记录仅保存在本App中。”

具体文案允许精简，但不能淡化风险。

不提前开发备份、提醒、云服务、帮助反馈或许可中心。

### Task6验收

逐页检查正常/空/加载/错误/陈旧数据状态。

使用真实回调执行现有操作，验证状态与金额不变。

完成定向测试、深浅截图和代码复核后提交本阶段改动，再进入Task7。

## 五、Task7：全部现有表单和弹窗一致化

这一步非常重要。

当前用户不希望再出现“首页精致，但进入录入界面后又变成旧测试页面”的情况。

首先枚举实际存在的所有表单及对话框，建立修改清单。

优先核查：

`core/design/FormFields.kt`

**长期项目：**

- `feature/project/ProjectEditorScreen.kt`
- `feature/project/MoneyEventEditor.kt`
- `feature/project/MoneyEventCorrectionScreen.kt`
- `feature/project/EndProjectDialog.kt`
- `feature/project/DeleteProjectDialog.kt`
- `feature/project/CoverPicker.kt`

**订阅服务：**

- `feature/subscription/SubscriptionEditorScreen.kt`
- `feature/subscription/RenewalEditorScreen.kt`
- `feature/subscription/SubscriptionStateDialog.kt`

如果上述组件还调用其他共用弹窗或表单，核对实际源码后纳入视觉一致化范围。

### 统一视觉要求

使用阶段1确认的同一套：

- 页面标题和返回按钮
- 表单输入框
- 日期和金额输入区域
- 主按钮、次按钮、危险按钮
- 状态标签
- 错误提示
- 确认对话框
- 加载及提交状态
- 底部弹层

Light和Dark必须都正常显示。

360dp宽度及200%系统字体下，字段、错误、确认和返回操作必须能够实际到达。

触控目标至少48dp，不得靠缩小系统字体解决布局问题。

### 原业务行为不可削弱

完整保留：

- 原始金额raw输入、精度和格式规则
- 日期范围及业务时区判断
- 必填与可选字段
- 允许0元的原有场景
- 每次真实付款确认
- 重复提交保护
- 成功后才离开
- 保存失败保留输入
- 已修改表单离开确认
- 操作结果未知后的核对及恢复
- 旋转、系统Back和任务恢复
- 固定操作ID和完整业务基线
- COMMITTED后的只读核对

原有三VM守卫必须保持：

- dirty：触发原放弃确认
- IN_FLIGHT：保护处理中业务
- UNCONFIRMED：禁止不安全离开并允许核对
- COMMITTED：防止重复写入

新增菜单、底部导航、系统返回、弹窗关闭不能绕过守卫。

不使用SideEffect或新UI状态重置既有业务信封。

不因视觉美化减少二次确认、弱化错误或删除状态事实。

### 封面操作

只统一现有选图、查看、替换、移除的展示。

必须继承原内容URI授权和本地媒体协议。

不能因为图片显示正常就假定JPEG输入完整性通过。

所有原有失败保护、旧封面及清理边界继续保留。

## 六、必须专项处理的旧表单超时

阶段1精修提交的最终主运行曾出现：

`com.cooper.lithic.ui.UIComponentsTest#large_text_synthetic_form_keeps_errors_save_and_return_reachable_after_input`

失败类型为保存点击后的10秒等待超时。

同一输入、未改源码的独立复跑PASS，但**不代表根因已经修复**。

Task7期间必须：

1. 实际复核该测试的操作流程、点击位置、焦点、IME、主线程及可见按钮状态。
2. 区分应用逻辑问题、测试驱动时序问题及系统外部窗口干扰。
3. 保留原失败XML和日志。
4. 若能稳定复现，先保存RED证据，再进行范围内最小修复。
5. 若无法稳定复现，保留不确定性与复跑证据，不能自行关闭风险。

禁止简单延长10秒超时、吞异常、删断言、跳过测试或反复盲跑直到出现PASS。

旧 Digital Wellbeing 外部ANR窗口诊断也必须保留历史，不能把外部系统窗口故障直接认定为砺石自身业务故障。

## 七、Task8：完整回归与独立交付

### 1. 冻结最终测试输入

完成所有产品UI改动后，记录并锁定同一完整源码、资源和构建配置输入。

起点已有701个源码测试方法身份：

- 原阶段1方法692个。
- 精修新增9个。
- 总计701个。

最终方法总数以实际源码为准，不预设701就是最终数量。

必须核对这701个原方法身份及其业务断言没有被悄悄删除、跳过或削弱。

保留本轮新增测试的完整身份清单。

### 2. 执行真正的全量回归

在已核对的项目根目录执行：

```powershell
.\gradlew.bat :app:testDebugUnitTest --max-workers=2 --console=plain

.\gradlew.bat :app:connectedDebugAndroidTest --max-workers=2 --console=plain

.\gradlew.bat :app:lintDebug :app:assembleDebug --max-workers=2 --console=plain
```

Android全量命令不添加只运行指定类的过滤器。

保存每条命令实际退出码、日志、JUnit XML、执行方法集合、耗时与输入指纹。

如果构建或测试中途修改源码，最终完整回归必须针对最终输入重新执行；不得混用不同输入的成功结果。

UP-TO-DATE任务不能表述为本次重新执行完成。

### 3. M10三项JPEG失败继续保留

以下三项已知失败不得删除、忽略、修改fixture或通过更改期望结果换取GREEN：

`M10JpegIntegrityTest#jpeg_entropy_truncated_with_eoi_is_rejected`

`M10JpegOldCoverProtectionTest#project_entropy_cut_jpeg_is_rejected_and_old_cover_and_money_are_preserved`

`M10JpegOldCoverProtectionTest#subscription_entropy_cut_jpeg_is_rejected_and_old_cover_payments_and_states_are_preserved`

已知根因：保留EOI标记但内部图像数据缺失的JPEG，在API36生产导入链路中被错误接受。

本次不修复，不引入libjpeg-turbo、JNI或NDK/CMake。

如果最终Android测试仍因这三项失败，必须报告**整体FAIL**，不能称全部通过。

三项之外出现新的失败，必须判断是真实新回归、环境阻塞还是原有间歇问题，并提供证据。

不能把“除JPEG外测试通过”描述成“整个应用测试通过”。

### 4. UI-C1-T01～T14全部复核

按已有 `UI_C1_PLAN.md` 中14项正式UI验收定义逐项登记。

本阶段特别关注：

- 订阅成本与时长口径。
- 付款、暂停、停止、纠错及重新开通。
- 长期项目已结束后的金额纠错。
- 所有原有业务操作的可达性。
- dirty/pending/COMMITTED及恢复守卫。
- 首页与列表图片缓存的代际隔离。
- 加载、错误、空态与陈旧数据显示。
- 360dp、200%字体、长名称、大额和负金额。
- 真实IME下输入、提交、错误及返回。
- 切换系统深浅主题不得产生业务写入。
- 断网冷启动及已实现的基本离线操作。

不得只统计测试数量，必须记录实际断言范围及缺失覆盖。

### 5. 最终视觉截图

使用**本轮最终输入构建、已安装的同一最终Debug APK**重新截图。

至少需要8组页面，每组Light/Dark各一张，共16张正常字体原图：

1. 现在
2. 长期项目列表
3. 长期项目详情
4. 订阅服务列表
5. 订阅服务详情
6. 回看
7. 设置
8. 新增菜单

额外提供：

- 首页空态深浅截图
- 关键读取错误与封面错误状态
- 360dp／200%字体截图
- 表单、真实IME及提交错误截图
- 订阅历史、项目历史和必要操作弹窗
- 必要的滚动补图及危险操作确认

真实MainActivity、隔离合成样板、空Room测试宿主必须分别标注。

不能拿测试宿主截图冒充最终MainActivity，也不能拿阶段1旧截图冒充阶段2最终结果。

每张截图登记：

- 文件SHA-256
- 最终APK SHA-256
- 设备和Android API
- 分辨率与dp宽度
- 字体比例
- Light/Dark
- 对应页面与状态
- 测试资料来源
- 实际前台Activity或宿主身份

不得使用AI概念图、修过的图片或Compose Preview冒充运行截图。

必须直接对照两张已确认的设计原图检查布局、间距、封面、金额、状态、按钮、导航和视觉一致性。

### 6. APK与数据安全

生成独立命名的最终UI Debug APK，不覆盖原M10交付物或阶段1APK。

核对：

- 实际文件大小与SHA-256
- APK签名与旧证书兼容性
- 实际版本及applicationId
- minSdk、targetSdk和权限
- Room版本及迁移资源
- 打包的类和依赖
- 测试类、合成资料、参考图不得进入生产APK
- 备份排除配置保持原样

继续使用现有授权的API36专用AVD。

不得重建、wipe、卸载清数据、修改generation指针或宿主时钟。

测试开始和结束必须核对原七表完整事实、私有封面字节、记录时区、generation、ready及settings。

恢复改变过的系统主题、字体、密度、旋转、网络与IME设置。

不向正式账本注入合成视觉样本。

真机/API26/OEM/Release/完整TalkBack/真实LMK/断电，如未实际执行，继续NOT_RUN。

## 八、文档、提交与范围收口

允许随着实施更新：

- `docs/CURRENT_STATE.md`
- `docs/design/UI_C1_SPEC.md`（必要的已批准实施澄清）
- `docs/planning/UI_C1_PLAN.md`（记录执行状态）
- 新的UI-C1阶段2验证报告
- 新的截图/测试原始证据
- 对应checkpoint
- 与本轮有关的技术决策记录

历史阶段0、阶段1、视觉精修报告与原始失败证据不得改写。

正式 `ACCEPTANCE_MATRIX.md` 继续保持原有M00～M09及M10验收状态，不写入虚假的UI/M10冻结结论。

Q05仍为USER_CONFIRMED，M11/M12完整往返仍NOT_RUN。

Task6、Task7可分别做范围明确的Conventional Commit；Task8完成最终核验后再提交交付证据。

只推送：

`feat/ui-c1-visual-redesign`

不合并main、不操作M10分支、不操作WHOCARE、不force push、不公开发布APK。

## 九、最终报告必须包含

1. 来源SHA、最终SHA、分支和远端一致性。
2. Task6～Task8实际完成范围及未完成内容。
3. 修改文件与具体业务/视觉影响。
4. 全部16张正式双主题截图及额外压力图。
5. 阶段1视觉基线是否保持，哪些页面做了合理适配。
6. UI-C1-T01～T14逐项状态。
7. 实际完整测试方法数、命令、退出码、XML、日志与最终输入指纹。
8. 已知三项JPEG失败、旧表单超时及所有新增失败的独立说明。
9. 数据库、领域引擎、业务协议、权限、工具链的未改动证据。
10. 最终APK的路径、大小、SHA、签名及版本。
11. 原账本、私有封面与设备设置的前后核查结果。
12. 所有仍为NOT_RUN或BLOCKED的项目，以及正式验收需要关注的风险。

**停止规则：**

如果 Task6～Task8 完成，且不存在未解释的新增UI安全或业务阻塞，则返回：

`READY_FOR_REVIEW — UI-C1`

如果出现未解决的新增业务回归、数据保护风险或需要越过授权边界的修改，则报告具体阻塞，保持PENDING，不得假装交付完成。

无论哪种结果，都不得自行宣布：

`ACCEPTED / FROZEN`

不得修复M10 JPEG、合并main或进入M11。

最终交付后立即停止，等待用户和ChatGPT独立验收。

**本次批准的是整套UI的实施范围，不是提前批准最终测试、视觉和安全结果。**