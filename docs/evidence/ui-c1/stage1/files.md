# UI-C1阶段1文件清单

本轮基线 `edc8be772a795f40b44f15da79ebf62a529bc074`；无删除。测试/来源/最终APK边界见[报告](../../UI-C1-stage1-verification.md)。

## 产品/测试/当前文档

| 路径 | 类型 |
|---|---|
| `.gitattributes` | 修改 |
| `AGENTS.md` | 修改 |
| `README.md` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/app/UIC1HomePreviewTest.kt` | 新增 |
| `app/src/androidTest/java/com/cooper/lithic/feature/home/UIC1HomeSampleTest.kt` | 新增 |
| `app/src/androidTest/java/com/cooper/lithic/feature/media/M10CoverEntryScreensTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/feature/media/UIC1CoverPreviewTest.kt` | 新增 |
| `app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectEntryFlowTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleFlowTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleScreensTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectNavigationGuardTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/feature/project/UIC1ProjectSampleTest.kt` | 新增 |
| `app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionNavigationGuardTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionNavigationGuardTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionRenewalFlowTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/ui/NavigationShellTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/ui/StorageUiStateTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/ui/UIC1EmptyVisualEvidenceTest.kt` | 新增 |
| `app/src/androidTest/java/com/cooper/lithic/ui/UIC1TestActions.kt` | 新增 |
| `app/src/androidTest/java/com/cooper/lithic/ui/UIC1ThemeTest.kt` | 新增 |
| `app/src/androidTest/java/com/cooper/lithic/ui/UIComponentsTest.kt` | 修改 |
| `app/src/main/java/com/cooper/lithic/app/LedgerUiState.kt` | 修改 |
| `app/src/main/java/com/cooper/lithic/app/LedgerUiStateHolder.kt` | 修改 |
| `app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt` | 修改 |
| `app/src/main/java/com/cooper/lithic/core/design/EntryCard.kt` | 修改 |
| `app/src/main/java/com/cooper/lithic/core/design/LithicTheme.kt` | 修改 |
| `app/src/main/java/com/cooper/lithic/core/design/UiPrimitives.kt` | 新增 |
| `app/src/main/java/com/cooper/lithic/feature/home/AddRecordSheet.kt` | 新增 |
| `app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt` | 修改 |
| `app/src/main/java/com/cooper/lithic/feature/media/CoverDisplay.kt` | 修改 |
| `app/src/main/java/com/cooper/lithic/feature/media/CoverPreviewLoader.kt` | 新增 |
| `app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt` | 修改 |
| `app/src/main/java/com/cooper/lithic/feature/project/ProjectListScreen.kt` | 修改 |
| `app/src/main/res/drawable/ic_ui_add.xml` | 新增 |
| `app/src/main/res/drawable/ic_ui_back.xml` | 新增 |
| `app/src/main/res/drawable/ic_ui_chevron_right.xml` | 新增 |
| `app/src/main/res/drawable/ic_ui_close.xml` | 新增 |
| `app/src/main/res/drawable/ic_ui_image.xml` | 新增 |
| `app/src/main/res/drawable/ic_ui_image_error.xml` | 新增 |
| `app/src/main/res/drawable/ic_ui_more.xml` | 新增 |
| `app/src/main/res/drawable/ic_ui_now.xml` | 新增 |
| `app/src/main/res/drawable/ic_ui_project.xml` | 新增 |
| `app/src/main/res/drawable/ic_ui_review.xml` | 新增 |
| `app/src/main/res/drawable/ic_ui_settings.xml` | 新增 |
| `app/src/main/res/drawable/ic_ui_subscription.xml` | 新增 |
| `docs/ARCHITECTURE.md` | 修改 |
| `docs/CURRENT_STATE.md` | 修改 |
| `docs/DECISIONS.md` | 修改 |
| `docs/ROADMAP.md` | 修改 |
| `docs/checkpoints/2026-10-15-UI-C1-stage1.md` | 新增 |
| `docs/design/UI_C1_SPEC.md` | 修改 |
| `docs/evidence/UI-C1-stage1-verification.md` | 新增 |
| `docs/planning/UI_C1_PLAN.md` | 修改 |

## 证据目录

本目录包含19张实际PNG、8个测试阶段的原始日志/XML/登记输入ZIP/summary、最终APK静态校验和独立Lint/assemble记录、逐图登记、数据/设置校验摘要、复核脚本与图册。

完整新/改文件字节清单见 `file-manifest.json`（排除它自己与后生成的提交前登记，避免递归哈希）。原DB/WAL/SHM、私有图片原文件、APK及密钥均不在提交范围。截图只有已有合成QA与隔离测试宿主。

新增7份测试辅助/测试文件中6个class含35个方法；UIC1TestActions仅驱动helper，不增加测试方法。旧11份测试只做界面入口/等价断言/IME同步适配，原方法身份保留。

阶段0原件、正式矩阵、原130条及旧证据不改。原始输入ZIP是各阶段登记的构建输入快照，不是完整工具环境或活动数据库/用户文件备份；未在逐次快照中的原Debug字符串/启动脚本另以supplemental-inputs.json登记当前零差异。
