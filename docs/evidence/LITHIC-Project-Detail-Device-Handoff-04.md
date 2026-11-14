# 砺石项目详情 · 小米14真机验收准备

**LITHIC_PROJECT_DETAIL_DEVICE_HANDOFF_04 — READY_FOR_USER_REVIEW**

基线 `c1339d98820bbed28d6934981153f3c5d461f1e7`。本轮只核验已有 APK 并准备交接材料。小米14人工验收和 Android 自动化执行均为 **NOT_RUN**。[上轮 Android 阻断证据](LITHIC-UI-V3-Project-Detail-Final-Acceptance-03.md)

## 1. 本地 APK 与身份

| 已核验产物 | 包名 | Manifest 版本 | 大小 | SHA-256 |
| --- | --- | --- | ---: | --- |
| Debug App `app-debug.apk` | `com.cooper.lithic.debug` | `0.1.0-m22-debug` / code `17` | 21,224,212 B | `8f638588cb738f8eec949bfb758b72285f29056fa2747a6601ad077530fcc9c5` |
| AndroidTest `app-debug-androidTest.apk` | `com.cooper.lithic.debug.test` | `versionName`、`versionCode` 均未声明 | 4,089,367 B | `558caf5afbda28cd28ebf578be726f4ad6cd9e616917f19424f077c3e1b63b81` |

两份文件均在本机目录 `E:\CODEX\lithic-ui-v3-project-detail-final-acceptance-03-android-local\`。两包的 APK v2 签名均有效，各有一位签署者，匹配原 Debug 证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。

Debug App 使用生产界面，归属生产源码 `0f6fed74c03a82ee5a90f5e209e6ea6fb3989a25`，图标名称为「砺石 LITHIC Debug」，入口是 `MainActivity`。正式发行包身份为 `com.cooper.lithic`。Test 来自 `70aed39e8708fee14c9a6fa2b6639acb33d3d0f8`，App 树 `1d9d637bd781b2209f39476c9ded45eb7ea67450`。Test 的 `androidx.test.runner.AndroidJUnitRunner` 唯一目标为 `com.cooper.lithic.debug`，无桌面入口。aapt2 的 Test 版本字段为空，apkanalyzer 返回 `UNKNOWN`，安装后的系统版本展示待实测。[本轮签名、Manifest 及配置原始核验](lithic-project-detail-device-handoff-04/apk-offline-audit.txt) · [原构建绑定](lithic-ui-v3-project-detail-final-acceptance-03/desktop/apk-build-inputs.json)

## 2. 小米14人工清单

在独立审查和另行授权后，由用户在小米14上执行。人工流程仅使用 Debug App。开始前核对文件指纹、Debug 包身份和用途。若安装界面提示更新、首次打开已有来源不明的记录，或包身份和账本归属无法确认，立即停止。保留原 App 和数据，不清数据、不导入个人备份。只有明确空白或明确可丢弃的 Debug 账本才能继续。记录设备型号、Android／HyperOS 版本和 App 当前版本即可，截图只保留合成数据，裁去通知、账号等个人信息。

### 合成账本

在 Debug 首页点「添加」→「新增项目」，名称填 **合成旅行摄影设备与备用镜头套装**，类别选「其他」。以详情成本卡「截至」对应的当日为 D，同一天完成金额核对。先创建项目，再按表追加三笔记录。用不同日期固定列表顺序。各步只提交一次，若提交结果未确认就停在原处，记录现象。

| 顺序 | 动作 | 日期 | 金额及说明 |
| --- | --- | --- | --- |
| 1 | 新增项目，开始拥有和初始实际付款同日 | D−9 | 初始实际投入 ¥100.00 |
| 2 | 记一笔 → 记录投入 | D−7 | ¥20.00，说明「合成配件投入」 |
| 3 | 记一笔 → 记录收回 | D−5 | ¥30.00，此时先验收三条 |
| 4 | 记一笔 → 记录投入 | D−3 | ¥10.00，之后验收四条 |

| 检查 | 怎么做 | 应看到什么 | 失败情况 | 建议截图 |
| --- | --- | --- | --- | --- |
| 1. 打开详情 | 从 Debug 首页或项目列表点该合成项目 | 详情正常打开，金额和日期与该项目对应 | 闪退、持续加载、打开其他记录 | `01-detail.png` |
| 2. 4:3 封面 Fit | 仅在另有已核验的合成封面夹具时打开横图、竖图项目 | 两张图在固定 4:3 框里完整、等比显示。留边可见 | 主体边缘被裁、拉伸、读取错误 | `02-cover-fit-landscape.png`、`02-cover-fit-portrait.png` |
| 3. 查看完整封面 | 使用上一项的可用封面，点击图框，再按系统返回 | 打开「完整封面」，关闭后回到同一项目详情 | 无法打开、图像裁切、返回跳出详情 | `03-full-cover.png` |
| 4. 项目信息 | 查看名称下方 | 名称逐字完整，类别「其他」，状态「持有中」，持有 10 天 | 错字、遗漏、状态或天数不符 | 与 01 共用 |
| 5. 当前日均与三卡 | 先在三条时核对，追加第四条后再核对 | 三条时投入 ¥120.00、收回 ¥30.00、净投入 ¥90.00、日均 ¥9.00/天。四条时分别为 ¥130.00、¥30.00、¥100.00、¥10.00/天 | 同一截止日金额错误、单位缺失、财务卡被裁切。若跨日，先按实际持有天数重新计算 | `05-cost-three.png`、`05-cost-four.png` |
| 6. 三条与四条记录 | 三条时看金额记录标题。追加第四条，点「全部」，再点「收起」 | 三条时无「全部」。四条默认仍为前三条，展开显示四条，收起恢复三条，按事实日期顺序 | 提前显示、重复、漏记、顺序错误或无法收起 | `06-three.png`、`06-four-expanded.png`、`06-four-collapsed.png` |
| 7. 单笔事实 | 点 D−7 的 ¥20 投入 | 能看到准确日期、投入类型、实际金额和「合成配件投入」 | 指向其他记录，事实、方向或说明错误 | `07-event-facts.png` |
| 8. 纠错入口 | 在单笔详情点「纠正这条记录」。可临时改说明，按返回后点「放弃修改」 | 打开该笔纠错表单。修改未保存时出现放弃确认，退出后原事实不变 | 进入错误记录、缺少守卫、放弃仍写入 | `08-correction.png`、`08-discard.png` |
| 9. 固定记一笔 | 滚到金额记录末尾，点「记一笔」，确认「记录投入」「记录收回」后返回关闭 | 按钮仍可点，两个入口存在，关闭后记录数不变 | 被内容盖住、回退异常、关菜单竟改变数据 | `09-record-menu.png` |
| 10. 三主题 | 「设置」→「外观与主题」，依次选择纯白、岩白雾蓝、深色黑灰，每次回看同一项目 | 三主题实际应用，文字、状态和分隔线可辨，金额不变 | 主题不生效、文字不可辨、切换后出现数据差异 | `10-pure-white.png`、`10-stone-mist.png`、`10-charcoal.png` |
| 11. 系统 200% 字体 | 记下原系统字号，先用正常字号取图，再在系统设置调整文字大小，返回同一详情并滚动至成本及财务卡。结束后由用户恢复原字号 | 金额与单位完整，财务卡全部可达，记一笔可点。如果系统界面不能证实 200%，记录所选档位，精确 200% 仍记 `NOT_RUN` | 文字裁切、不同区域重叠、内容不可滚动或按钮遮挡 | `11-system-font.png`、`11-detail-large-font.png` |
| 12. 长名称 | 正常和已确认 200% 各查看一次同一名称，向下滚到日均成本和财务卡 | 名称完整，空间不足时自然换行，无省略，与类别、状态和底部操作不重叠 | 缺字、省略、横向溢出或卡片不可达 | `project-detail-long-title-100.png`、`project-detail-long-title-200.png`。未证实 200% 时用实际档位命名 |
| 13. 原生返回 | 从照片查看或金额弹窗返回详情。从详情按系统返回，观察实际进入来源。成功退出菜单后查看记录数 | 先关闭当前面板，再回到原项目入口，固定操作不挡正文，记录数不变 | 一次返回跳过层级、底部遮挡、金额或数量变化 | `13-back.png`，发生问题时附返回前后两张 |

**封面前提。** 实际 APK 的 `PHOTO_LITE_EXPERIMENT`、`PHOTO_LITE_PRODUCTION` 均为 `false`，此包可读取已有合成封面，但空白账本不能通过「添加照片」建立封面。本轮未找到能独立用于真机横／竖图验收的已核验合成夹具，之前隔离模拟器的图片夹具仅在测试运行时创建。当前可用空白 Debug 账本执行无封面的其余人工项目，第 2、3 项记 `NOT_RUN`。准备或恢复专用合成封面的操作需另行授权。此前同一 App APK 的 [横图、三主题和系统 200% 模拟器截图](LITHIC-UI-V3-Project-Detail-Repair-02.md#原生截图) 可供对照，不能作为小米14结果。

建议回传每项 `PASS`、`FAIL` 或 `NOT_RUN`、对应截图名称及简短现象。空白 Debug 账本创建的无封面项目应显示类型图标，可把占位结果记入第 1 项。发生身份、数据或写入不明时直接停止。

## 3. Test APK 静态方法索引

[固定执行清单（原 11 次、首页 43 项、项目专属套件的完整方法名和分组）](lithic-ui-v3-project-detail-final-acceptance-03/desktop/regression-methods.json) · [逐方法 DEX 核验（代码体、JUnit 注解及 DEX 文件）](lithic-project-detail-device-handoff-04/dex-method-evidence.json)

| 执行组 | 静态收录 | 本轮 Android |
| --- | --- | --- |
| 原失败定向 11 次 | 10 个方法。同一项目画廊方法在实际系统字体 1.0、2.0 各执行一次，其余 9 次覆盖此前夹具、金额、纠错及入口 | NOT_RUN |
| 长标题 100% | `V3ProjectDetailAndroidAcceptanceTest#long_project_title_wraps_without_clipping_at_system_font_scale`，实际系统字体 1.0，并验证屏宽 360dp | NOT_RUN |
| 长标题 200% | 同一长标题方法，实际系统字体 2.0，并验证屏宽 360dp | NOT_RUN |
| 首页 43 项 | `HomeAndroidAcceptanceTest` 3、`HomeOverviewUiTest` 6、`P2HomeArchiveContractTest` 6、`ReportRepositoryTest` 19、指定 `CoverPreviewsTest` 7、`ActivityNavigationTest` 2 | NOT_RUN |
| 项目详情专属 43 次 | 42 个方法。包含原 11 次，另 32 次覆盖详情组件、层级和金额、封面、导航保护及生命周期 | NOT_RUN |

去重后全部 **79 个**指定方法均在现有 Test APK 中有实际 DEX 代码体及 `@Test`，其中两套完整回归共享 7 个封面方法。新几何判据已静态核对，原 `hasVisualOverflow` 保留在诊断中，新增逐行宽高、逐字边界、可见末尾及节点裁切检查。[新标题判据的构建与 DEX 证据](LITHIC-UI-V3-Project-Detail-Final-Acceptance-03.md#改动与验收判据)

**真机运行边界。** 当前首页的 3 个原生用例、项目画廊的 2 次以及长标题的 2 次，共 7 次执行受 API 36、x86_64、QEMU 属性和指定 AVD 硬门禁约束。真实系统 360dp、字号和测试夹具由原测试核对，现有自动化运行器仅面向原隔离 AVD。其余项目存在各自的隔离和临时数据合同，包内有代码只表明静态收录，未建立小米14自动化执行资格。

## 4. 本轮交付范围

原模拟器的 Phone、SystemUI、Traceur 等历史启动 ANR 证据继续保留。本轮未启动 ADB、未连设备、未安装或运行 App／Test、未使用手机或真实账本。APK 实物保持原路径和字节，Git 仅提交交接文档和静态核验记录。保留上轮环境阻断，等待 ChatGPT 独立审查。
