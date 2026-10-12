# M07 实际文件清单

基线 fb0eafdc43efa6e13ebd76e3491fa4331a145abb；分支 feat/m07-project-lifecycle。提交范围仅本表；产物APK、只读DB副本、本机助手和临时证据留在ignored build。

新增 62、修改 21、删除 0。本表包含自身；不记录递归自SHA，Git提交日志为最终版本事实。

## 新增

- [ProjectLifecycleRepositoryTest.kt](../../app/src/androidTest/java/com/cooper/lithic/data/repository/ProjectLifecycleRepositoryTest.kt) — `app/src/androidTest/java/com/cooper/lithic/data/repository/ProjectLifecycleRepositoryTest.kt`
- [ProjectLifecycleFlowTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleFlowTest.kt) — `app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleFlowTest.kt`
- [ProjectLifecycleRoutesTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleRoutesTest.kt) — `app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleRoutesTest.kt`
- [ProjectLifecycleScreensTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleScreensTest.kt) — `app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleScreensTest.kt`
- [ProjectLifecycleViewModelTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleViewModelTest.kt) — `app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleViewModelTest.kt`
- [ProjectLifecycleCommands.kt](../../app/src/main/java/com/cooper/lithic/data/repository/ProjectLifecycleCommands.kt) — `app/src/main/java/com/cooper/lithic/data/repository/ProjectLifecycleCommands.kt`
- [ProjectLifecycleRules.kt](../../app/src/main/java/com/cooper/lithic/domain/project/ProjectLifecycleRules.kt) — `app/src/main/java/com/cooper/lithic/domain/project/ProjectLifecycleRules.kt`
- [DeleteProjectDialog.kt](../../app/src/main/java/com/cooper/lithic/feature/project/DeleteProjectDialog.kt) — `app/src/main/java/com/cooper/lithic/feature/project/DeleteProjectDialog.kt`
- [EndProjectDialog.kt](../../app/src/main/java/com/cooper/lithic/feature/project/EndProjectDialog.kt) — `app/src/main/java/com/cooper/lithic/feature/project/EndProjectDialog.kt`
- [MoneyEventCorrectionScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/MoneyEventCorrectionScreen.kt) — `app/src/main/java/com/cooper/lithic/feature/project/MoneyEventCorrectionScreen.kt`
- [ProjectHistoryScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectHistoryScreen.kt) — `app/src/main/java/com/cooper/lithic/feature/project/ProjectHistoryScreen.kt`
- [ProjectLifecycleRulesTest.kt](../../app/src/test/java/com/cooper/lithic/domain/project/ProjectLifecycleRulesTest.kt) — `app/src/test/java/com/cooper/lithic/domain/project/ProjectLifecycleRulesTest.kt`
- [2026-10-12-M07.md](../checkpoints/2026-10-12-M07.md) — `docs/checkpoints/2026-10-12-M07.md`
- [M07-files.md](M07-files.md) — `docs/evidence/M07-files.md`
- [M07-verification.json](M07-verification.json) — `docs/evidence/M07-verification.json`
- [M07-verification.md](M07-verification.md) — `docs/evidence/M07-verification.md`
- [manual-amended-250.png](m07-device/manual-amended-250.png) — `docs/evidence/m07-device/manual-amended-250.png`
- [manual-amended-300.png](m07-device/manual-amended-300.png) — `docs/evidence/m07-device/manual-amended-300.png`
- [manual-corrected-initial.png](m07-device/manual-corrected-initial.png) — `docs/evidence/m07-device/manual-corrected-initial.png`
- [manual-create-ime.png](m07-device/manual-create-ime.png) — `docs/evidence/m07-device/manual-create-ime.png`
- [manual-delete-cancel.png](m07-device/manual-delete-cancel.png) — `docs/evidence/m07-device/manual-delete-cancel.png`
- [manual-delete-confirm.png](m07-device/manual-delete-confirm.png) — `docs/evidence/m07-device/manual-delete-confirm.png`
- [manual-ended-detail.png](m07-device/manual-ended-detail.png) — `docs/evidence/m07-device/manual-ended-detail.png`
- [manual-history-empty-after-delete.png](m07-device/manual-history-empty-after-delete.png) — `docs/evidence/m07-device/manual-history-empty-after-delete.png`
- [manual-history-first.png](m07-device/manual-history-first.png) — `docs/evidence/m07-device/manual-history-first.png`
- [manual-home-after-delete.png](m07-device/manual-home-after-delete.png) — `docs/evidence/m07-device/manual-home-after-delete.png`
- [manual-home-offline.png](m07-device/manual-home-offline.png) — `docs/evidence/m07-device/manual-home-offline.png`
- [manual-large-end-ime.png](m07-device/manual-large-end-ime.png) — `docs/evidence/m07-device/manual-large-end-ime.png`
- [manual-large-landscape.png](m07-device/manual-large-landscape.png) — `docs/evidence/m07-device/manual-large-landscape.png`
- [manual-large-save-ime-visible.png](m07-device/manual-large-save-ime-visible.png) — `docs/evidence/m07-device/manual-large-save-ime-visible.png`
- [manual-large-validation-error.png](m07-device/manual-large-validation-error.png) — `docs/evidence/m07-device/manual-large-validation-error.png`
- [manual-late-outlay.png](m07-device/manual-late-outlay.png) — `docs/evidence/m07-device/manual-late-outlay.png`
- [manual-negative-lifecycle.png](m07-device/manual-negative-lifecycle.png) — `docs/evidence/m07-device/manual-negative-lifecycle.png`
- [manual-restarted-empty-history.png](m07-device/manual-restarted-empty-history.png) — `docs/evidence/m07-device/manual-restarted-empty-history.png`
- [manual-restarted-negative.png](m07-device/manual-restarted-negative.png) — `docs/evidence/m07-device/manual-restarted-negative.png`
- [manual-restored-end-draft.png](m07-device/manual-restored-end-draft.png) — `docs/evidence/m07-device/manual-restored-end-draft.png`
- [diagnostic-amend-native-0.xml](m07-tests/diagnostic-amend-native-0.xml) — `docs/evidence/m07-tests/diagnostic-amend-native-0.xml`
- [diagnostic-amend.log](m07-tests/diagnostic-amend.log) — `docs/evidence/m07-tests/diagnostic-amend.log`
- [diagnostic-full-attempt1-native-0.xml](m07-tests/diagnostic-full-attempt1-native-0.xml) — `docs/evidence/m07-tests/diagnostic-full-attempt1-native-0.xml`
- [diagnostic-full-attempt2-native-0.xml](m07-tests/diagnostic-full-attempt2-native-0.xml) — `docs/evidence/m07-tests/diagnostic-full-attempt2-native-0.xml`
- [diagnostic-ui-state-native-0.xml](m07-tests/diagnostic-ui-state-native-0.xml) — `docs/evidence/m07-tests/diagnostic-ui-state-native-0.xml`
- [green-full-attempt1.log](m07-tests/green-full-attempt1.log) — `docs/evidence/m07-tests/green-full-attempt1.log`
- [green-full-complete-TEST-com.cooper.lithic.core.design.AmountFormatterTest.xml](m07-tests/green-full-complete-TEST-com.cooper.lithic.core.design.AmountFormatterTest.xml) — `docs/evidence/m07-tests/green-full-complete-TEST-com.cooper.lithic.core.design.AmountFormatterTest.xml`
- [green-full-complete-TEST-com.cooper.lithic.core.model.MoneyParserTest.xml](m07-tests/green-full-complete-TEST-com.cooper.lithic.core.model.MoneyParserTest.xml) — `docs/evidence/m07-tests/green-full-complete-TEST-com.cooper.lithic.core.model.MoneyParserTest.xml`
- [green-full-complete-TEST-com.cooper.lithic.core.time.BusinessClockTest.xml](m07-tests/green-full-complete-TEST-com.cooper.lithic.core.time.BusinessClockTest.xml) — `docs/evidence/m07-tests/green-full-complete-TEST-com.cooper.lithic.core.time.BusinessClockTest.xml`
- [green-full-complete-TEST-com.cooper.lithic.domain.project.ProjectCostEngineTest.xml](m07-tests/green-full-complete-TEST-com.cooper.lithic.domain.project.ProjectCostEngineTest.xml) — `docs/evidence/m07-tests/green-full-complete-TEST-com.cooper.lithic.domain.project.ProjectCostEngineTest.xml`
- [green-full-complete-TEST-com.cooper.lithic.domain.project.ProjectLifecycleRulesTest.xml](m07-tests/green-full-complete-TEST-com.cooper.lithic.domain.project.ProjectLifecycleRulesTest.xml) — `docs/evidence/m07-tests/green-full-complete-TEST-com.cooper.lithic.domain.project.ProjectLifecycleRulesTest.xml`
- [green-full-complete-TEST-com.cooper.lithic.domain.subscription.SubscriptionAccumulationTest.xml](m07-tests/green-full-complete-TEST-com.cooper.lithic.domain.subscription.SubscriptionAccumulationTest.xml) — `docs/evidence/m07-tests/green-full-complete-TEST-com.cooper.lithic.domain.subscription.SubscriptionAccumulationTest.xml`
- [green-full-complete-TEST-com.cooper.lithic.domain.subscription.SubscriptionCostEngineTest.xml](m07-tests/green-full-complete-TEST-com.cooper.lithic.domain.subscription.SubscriptionCostEngineTest.xml) — `docs/evidence/m07-tests/green-full-complete-TEST-com.cooper.lithic.domain.subscription.SubscriptionCostEngineTest.xml`
- [green-full-complete-native-0.xml](m07-tests/green-full-complete-native-0.xml) — `docs/evidence/m07-tests/green-full-complete-native-0.xml`
- [green-full-final.log](m07-tests/green-full-final.log) — `docs/evidence/m07-tests/green-full-final.log`
- [green-full.log](m07-tests/green-full.log) — `docs/evidence/m07-tests/green-full.log`
- [red-android-native-0.xml](m07-tests/red-android-native-0.xml) — `docs/evidence/m07-tests/red-android-native-0.xml`
- [red-android.log](m07-tests/red-android.log) — `docs/evidence/m07-tests/red-android.log`
- [red-conflict-missing-native-0.xml](m07-tests/red-conflict-missing-native-0.xml) — `docs/evidence/m07-tests/red-conflict-missing-native-0.xml`
- [red-conflict-missing.log](m07-tests/red-conflict-missing.log) — `docs/evidence/m07-tests/red-conflict-missing.log`
- [red-late-facts-and-amend-diagnostic-native-0.xml](m07-tests/red-late-facts-and-amend-diagnostic-native-0.xml) — `docs/evidence/m07-tests/red-late-facts-and-amend-diagnostic-native-0.xml`
- [red-late-facts-and-amend-diagnostic.log](m07-tests/red-late-facts-and-amend-diagnostic.log) — `docs/evidence/m07-tests/red-late-facts-and-amend-diagnostic.log`
- [red-rules.xml](m07-tests/red-rules.xml) — `docs/evidence/m07-tests/red-rules.xml`
- [red-state-gap-and-flow-native-0.xml](m07-tests/red-state-gap-and-flow-native-0.xml) — `docs/evidence/m07-tests/red-state-gap-and-flow-native-0.xml`
- [red-state-gap-and-flow.log](m07-tests/red-state-gap-and-flow.log) — `docs/evidence/m07-tests/red-state-gap-and-flow.log`
- [M07_CONTRACT.md](../planning/M07_CONTRACT.md) — `docs/planning/M07_CONTRACT.md`

## 修改

- [AGENTS.md](../../AGENTS.md) — `AGENTS.md`
- [README.md](../../README.md) — `README.md`
- [build.gradle.kts](../../app/build.gradle.kts) — `app/build.gradle.kts`
- [LithicNavigation.kt](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt) — `app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt`
- [LithicRoutes.kt](../../app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt) — `app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt`
- [ProjectDao.kt](../../app/src/main/java/com/cooper/lithic/data/local/ProjectDao.kt) — `app/src/main/java/com/cooper/lithic/data/local/ProjectDao.kt`
- [ProjectRepository.kt](../../app/src/main/java/com/cooper/lithic/data/repository/ProjectRepository.kt) — `app/src/main/java/com/cooper/lithic/data/repository/ProjectRepository.kt`
- [ProjectWriteRepository.kt](../../app/src/main/java/com/cooper/lithic/data/repository/ProjectWriteRepository.kt) — `app/src/main/java/com/cooper/lithic/data/repository/ProjectWriteRepository.kt`
- [ProjectDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt) — `app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt`
- [ProjectListScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectListScreen.kt) — `app/src/main/java/com/cooper/lithic/feature/project/ProjectListScreen.kt`
- [ProjectUiModels.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectUiModels.kt) — `app/src/main/java/com/cooper/lithic/feature/project/ProjectUiModels.kt`
- [ProjectViewModel.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt) — `app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt`
- [ReviewShell.kt](../../app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt) — `app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt`
- [ACCEPTANCE_MATRIX.md](../ACCEPTANCE_MATRIX.md) — `docs/ACCEPTANCE_MATRIX.md`
- [ARCHITECTURE.md](../ARCHITECTURE.md) — `docs/ARCHITECTURE.md`
- [CURRENT_STATE.md](../CURRENT_STATE.md) — `docs/CURRENT_STATE.md`
- [DECISIONS.md](../DECISIONS.md) — `docs/DECISIONS.md`
- [EXECUTION_PROTOCOL.md](../EXECUTION_PROTOCOL.md) — `docs/EXECUTION_PROTOCOL.md`
- [ROADMAP.md](../ROADMAP.md) — `docs/ROADMAP.md`
- [SPEC_ISSUES.md](../SPEC_ISSUES.md) — `docs/SPEC_ISSUES.md`
- [README.md](../planning/README.md) — `docs/planning/README.md`

## 删除
