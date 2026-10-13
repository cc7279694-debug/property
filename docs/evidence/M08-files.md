# M08 实际文件清单

基线 fcb850f549285d310040318a5f7adfd51abb4c33；分支 feat/m08-subscription-renewals。APK、DB/WAL/SHM、AVD、签名私钥与本机助手均不提交。

新增 97、修改 24、删除 0。本清单包含自身，不记录递归自SHA；最终提交/推送SHA以真实Git日志及交付回执核对。

## 新增

- [2.json](../../app/schemas/com.cooper.lithic.data.local.LithicDatabase/2.json) — `app/schemas/com.cooper.lithic.data.local.LithicDatabase/2.json`
- [SubscriptionMigrationTest.kt](../../app/src/androidTest/java/com/cooper/lithic/data/local/SubscriptionMigrationTest.kt) — `app/src/androidTest/java/com/cooper/lithic/data/local/SubscriptionMigrationTest.kt`
- [V1DatabaseFixture.kt](../../app/src/androidTest/java/com/cooper/lithic/data/local/V1DatabaseFixture.kt) — `app/src/androidTest/java/com/cooper/lithic/data/local/V1DatabaseFixture.kt`
- [SubscriptionRepositoryTest.kt](../../app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionRepositoryTest.kt) — `app/src/androidTest/java/com/cooper/lithic/data/repository/SubscriptionRepositoryTest.kt`
- [SubscriptionNavigationGuardTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionNavigationGuardTest.kt) — `app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionNavigationGuardTest.kt`
- [SubscriptionRenewalFlowTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionRenewalFlowTest.kt) — `app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionRenewalFlowTest.kt`
- [SubscriptionRoutesTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionRoutesTest.kt) — `app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionRoutesTest.kt`
- [SubscriptionScreensTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionScreensTest.kt) — `app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionScreensTest.kt`
- [SubscriptionViewModelTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionViewModelTest.kt) — `app/src/androidTest/java/com/cooper/lithic/feature/subscription/SubscriptionViewModelTest.kt`
- [SubscriptionNavigationPolicy.kt](../../app/src/main/java/com/cooper/lithic/app/SubscriptionNavigationPolicy.kt) — `app/src/main/java/com/cooper/lithic/app/SubscriptionNavigationPolicy.kt`
- [DatabaseSchemaValidation.kt](../../app/src/main/java/com/cooper/lithic/data/local/DatabaseSchemaValidation.kt) — `app/src/main/java/com/cooper/lithic/data/local/DatabaseSchemaValidation.kt`
- [SubscriptionDao.kt](../../app/src/main/java/com/cooper/lithic/data/local/SubscriptionDao.kt) — `app/src/main/java/com/cooper/lithic/data/local/SubscriptionDao.kt`
- [SubscriptionEntities.kt](../../app/src/main/java/com/cooper/lithic/data/local/SubscriptionEntities.kt) — `app/src/main/java/com/cooper/lithic/data/local/SubscriptionEntities.kt`
- [SubscriptionCommands.kt](../../app/src/main/java/com/cooper/lithic/data/repository/SubscriptionCommands.kt) — `app/src/main/java/com/cooper/lithic/data/repository/SubscriptionCommands.kt`
- [SubscriptionRepository.kt](../../app/src/main/java/com/cooper/lithic/data/repository/SubscriptionRepository.kt) — `app/src/main/java/com/cooper/lithic/data/repository/SubscriptionRepository.kt`
- [RenewalEditorScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/RenewalEditorScreen.kt) — `app/src/main/java/com/cooper/lithic/feature/subscription/RenewalEditorScreen.kt`
- [SubscriptionDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt) — `app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt`
- [SubscriptionEditorScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionEditorScreen.kt) — `app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionEditorScreen.kt`
- [SubscriptionListScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionListScreen.kt) — `app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionListScreen.kt`
- [SubscriptionUiModels.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionUiModels.kt) — `app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionUiModels.kt`
- [SubscriptionViewModel.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionViewModel.kt) — `app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionViewModel.kt`
- [2026-10-13-M08.md](../checkpoints/2026-10-13-M08.md) — `docs/checkpoints/2026-10-13-M08.md`
- [M08-files.md](M08-files.md) — `docs/evidence/M08-files.md`
- [M08-verification.json](M08-verification.json) — `docs/evidence/M08-verification.json`
- [M08-verification.md](M08-verification.md) — `docs/evidence/M08-verification.md`
- [manual-cancel-confirm.png](m08-device/manual-cancel-confirm.png) — `docs/evidence/m08-device/manual-cancel-confirm.png`
- [manual-cancel-returned.png](m08-device/manual-cancel-returned.png) — `docs/evidence/m08-device/manual-cancel-returned.png`
- [manual-create-four-dates.png](m08-device/manual-create-four-dates.png) — `docs/evidence/m08-device/manual-create-four-dates.png`
- [manual-expired-elapsed.png](m08-device/manual-expired-elapsed.png) — `docs/evidence/m08-device/manual-expired-elapsed.png`
- [manual-feb1-detail.png](m08-device/manual-feb1-detail.png) — `docs/evidence/m08-device/manual-feb1-detail.png`
- [manual-final-expired.png](m08-device/manual-final-expired.png) — `docs/evidence/m08-device/manual-final-expired.png`
- [manual-future-history.png](m08-device/manual-future-history.png) — `docs/evidence/m08-device/manual-future-history.png`
- [manual-home-offline.png](m08-device/manual-home-offline.png) — `docs/evidence/m08-device/manual-home-offline.png`
- [manual-invalid-amount.png](m08-device/manual-invalid-amount.png) — `docs/evidence/m08-device/manual-invalid-amount.png`
- [manual-jan19-detail.png](m08-device/manual-jan19-detail.png) — `docs/evidence/m08-device/manual-jan19-detail.png`
- [manual-jan20-detail.png](m08-device/manual-jan20-detail.png) — `docs/evidence/m08-device/manual-jan20-detail.png`
- [manual-large-after-ime-back.png](m08-device/manual-large-after-ime-back.png) — `docs/evidence/m08-device/manual-large-after-ime-back.png`
- [manual-large-confirm-ime.png](m08-device/manual-large-confirm-ime.png) — `docs/evidence/m08-device/manual-large-confirm-ime.png`
- [manual-large-ime.png](m08-device/manual-large-ime.png) — `docs/evidence/m08-device/manual-large-ime.png`
- [manual-large-landscape.png](m08-device/manual-large-landscape.png) — `docs/evidence/m08-device/manual-large-landscape.png`
- [manual-large-portrait.png](m08-device/manual-large-portrait.png) — `docs/evidence/m08-device/manual-large-portrait.png`
- [manual-large-validation.png](m08-device/manual-large-validation.png) — `docs/evidence/m08-device/manual-large-validation.png`
- [manual-list-after-prepay.png](m08-device/manual-list-after-prepay.png) — `docs/evidence/m08-device/manual-list-after-prepay.png`
- [manual-list-empty.png](m08-device/manual-list-empty.png) — `docs/evidence/m08-device/manual-list-empty.png`
- [manual-old-project-after-upgrade.png](m08-device/manual-old-project-after-upgrade.png) — `docs/evidence/m08-device/manual-old-project-after-upgrade.png`
- [manual-process-restored.png](m08-device/manual-process-restored.png) — `docs/evidence/m08-device/manual-process-restored.png`
- [manual-recents-after-kill.png](m08-device/manual-recents-after-kill.png) — `docs/evidence/m08-device/manual-recents-after-kill.png`
- [manual-renewal-four-dates.png](m08-device/manual-renewal-four-dates.png) — `docs/evidence/m08-device/manual-renewal-four-dates.png`
- [manual-restarted-expired.png](m08-device/manual-restarted-expired.png) — `docs/evidence/m08-device/manual-restarted-expired.png`
- [manual-restored-discard-confirm.png](m08-device/manual-restored-discard-confirm.png) — `docs/evidence/m08-device/manual-restored-discard-confirm.png`
- [manual-selected-cycle.png](m08-device/manual-selected-cycle.png) — `docs/evidence/m08-device/manual-selected-cycle.png`
- [upgrade-old-corrected.png](m08-device/upgrade-old-corrected.png) — `docs/evidence/m08-device/upgrade-old-corrected.png`
- [upgrade-old-ended.png](m08-device/upgrade-old-ended.png) — `docs/evidence/m08-device/upgrade-old-ended.png`
- [compile-migration-attempt1.log](m08-tests/compile-migration-attempt1.log) — `docs/evidence/m08-tests/compile-migration-attempt1.log`
- [diagnostic-full-final-attempt1-native-0.xml](m08-tests/diagnostic-full-final-attempt1-native-0.xml) — `docs/evidence/m08-tests/diagnostic-full-final-attempt1-native-0.xml`
- [diagnostic-full-final-attempt1.log](m08-tests/diagnostic-full-final-attempt1.log) — `docs/evidence/m08-tests/diagnostic-full-final-attempt1.log`
- [diagnostic-full-final-attempt2-native-0.xml](m08-tests/diagnostic-full-final-attempt2-native-0.xml) — `docs/evidence/m08-tests/diagnostic-full-final-attempt2-native-0.xml`
- [diagnostic-full-final-attempt2.log](m08-tests/diagnostic-full-final-attempt2.log) — `docs/evidence/m08-tests/diagnostic-full-final-attempt2.log`
- [diagnostic-old-synchronization-native-0.xml](m08-tests/diagnostic-old-synchronization-native-0.xml) — `docs/evidence/m08-tests/diagnostic-old-synchronization-native-0.xml`
- [diagnostic-old-synchronization.log](m08-tests/diagnostic-old-synchronization.log) — `docs/evidence/m08-tests/diagnostic-old-synchronization.log`
- [diagnostic-screen-idle-native-0.xml](m08-tests/diagnostic-screen-idle-native-0.xml) — `docs/evidence/m08-tests/diagnostic-screen-idle-native-0.xml`
- [diagnostic-screen-idle.log](m08-tests/diagnostic-screen-idle.log) — `docs/evidence/m08-tests/diagnostic-screen-idle.log`
- [final-apk-backup.txt](m08-tests/final-apk-backup.txt) — `docs/evidence/m08-tests/final-apk-backup.txt`
- [final-apk-badging.txt](m08-tests/final-apk-badging.txt) — `docs/evidence/m08-tests/final-apk-badging.txt`
- [final-apk-extraction.txt](m08-tests/final-apk-extraction.txt) — `docs/evidence/m08-tests/final-apk-extraction.txt`
- [final-apk-manifest.txt](m08-tests/final-apk-manifest.txt) — `docs/evidence/m08-tests/final-apk-manifest.txt`
- [final-signature.txt](m08-tests/final-signature.txt) — `docs/evidence/m08-tests/final-signature.txt`
- [green-foundation-attempt1-native-0.xml](m08-tests/green-foundation-attempt1-native-0.xml) — `docs/evidence/m08-tests/green-foundation-attempt1-native-0.xml`
- [green-foundation-attempt1.log](m08-tests/green-foundation-attempt1.log) — `docs/evidence/m08-tests/green-foundation-attempt1.log`
- [green-full-attempt1-native-0.xml](m08-tests/green-full-attempt1-native-0.xml) — `docs/evidence/m08-tests/green-full-attempt1-native-0.xml`
- [green-full-attempt1.log](m08-tests/green-full-attempt1.log) — `docs/evidence/m08-tests/green-full-attempt1.log`
- [green-full-before-visual-selection-native-0.xml](m08-tests/green-full-before-visual-selection-native-0.xml) — `docs/evidence/m08-tests/green-full-before-visual-selection-native-0.xml`
- [green-full-before-visual-selection.log](m08-tests/green-full-before-visual-selection.log) — `docs/evidence/m08-tests/green-full-before-visual-selection.log`
- [green-full-final-TEST-com.cooper.lithic.core.design.AmountFormatterTest.xml](m08-tests/green-full-final-TEST-com.cooper.lithic.core.design.AmountFormatterTest.xml) — `docs/evidence/m08-tests/green-full-final-TEST-com.cooper.lithic.core.design.AmountFormatterTest.xml`
- [green-full-final-TEST-com.cooper.lithic.core.model.MoneyParserTest.xml](m08-tests/green-full-final-TEST-com.cooper.lithic.core.model.MoneyParserTest.xml) — `docs/evidence/m08-tests/green-full-final-TEST-com.cooper.lithic.core.model.MoneyParserTest.xml`
- [green-full-final-TEST-com.cooper.lithic.core.time.BusinessClockTest.xml](m08-tests/green-full-final-TEST-com.cooper.lithic.core.time.BusinessClockTest.xml) — `docs/evidence/m08-tests/green-full-final-TEST-com.cooper.lithic.core.time.BusinessClockTest.xml`
- [green-full-final-TEST-com.cooper.lithic.domain.project.ProjectCostEngineTest.xml](m08-tests/green-full-final-TEST-com.cooper.lithic.domain.project.ProjectCostEngineTest.xml) — `docs/evidence/m08-tests/green-full-final-TEST-com.cooper.lithic.domain.project.ProjectCostEngineTest.xml`
- [green-full-final-TEST-com.cooper.lithic.domain.project.ProjectLifecycleRulesTest.xml](m08-tests/green-full-final-TEST-com.cooper.lithic.domain.project.ProjectLifecycleRulesTest.xml) — `docs/evidence/m08-tests/green-full-final-TEST-com.cooper.lithic.domain.project.ProjectLifecycleRulesTest.xml`
- [green-full-final-TEST-com.cooper.lithic.domain.subscription.SubscriptionAccumulationTest.xml](m08-tests/green-full-final-TEST-com.cooper.lithic.domain.subscription.SubscriptionAccumulationTest.xml) — `docs/evidence/m08-tests/green-full-final-TEST-com.cooper.lithic.domain.subscription.SubscriptionAccumulationTest.xml`
- [green-full-final-TEST-com.cooper.lithic.domain.subscription.SubscriptionCostEngineTest.xml](m08-tests/green-full-final-TEST-com.cooper.lithic.domain.subscription.SubscriptionCostEngineTest.xml) — `docs/evidence/m08-tests/green-full-final-TEST-com.cooper.lithic.domain.subscription.SubscriptionCostEngineTest.xml`
- [green-full-final-native-0.xml](m08-tests/green-full-final-native-0.xml) — `docs/evidence/m08-tests/green-full-final-native-0.xml`
- [green-full-final.log](m08-tests/green-full-final.log) — `docs/evidence/m08-tests/green-full-final.log`
- [green-subscriptions-attempt1-native-0.xml](m08-tests/green-subscriptions-attempt1-native-0.xml) — `docs/evidence/m08-tests/green-subscriptions-attempt1-native-0.xml`
- [green-subscriptions-attempt1.log](m08-tests/green-subscriptions-attempt1.log) — `docs/evidence/m08-tests/green-subscriptions-attempt1.log`
- [lint-results-debug.xml](m08-tests/lint-results-debug.xml) — `docs/evidence/m08-tests/lint-results-debug.xml`
- [red-foundation-native-0.xml](m08-tests/red-foundation-native-0.xml) — `docs/evidence/m08-tests/red-foundation-native-0.xml`
- [red-foundation.log](m08-tests/red-foundation.log) — `docs/evidence/m08-tests/red-foundation.log`
- [red-migration-native-0.xml](m08-tests/red-migration-native-0.xml) — `docs/evidence/m08-tests/red-migration-native-0.xml`
- [red-migration.log](m08-tests/red-migration.log) — `docs/evidence/m08-tests/red-migration.log`
- [red-preflight-and-guard-native-0.xml](m08-tests/red-preflight-and-guard-native-0.xml) — `docs/evidence/m08-tests/red-preflight-and-guard-native-0.xml`
- [red-preflight-and-guard.log](m08-tests/red-preflight-and-guard.log) — `docs/evidence/m08-tests/red-preflight-and-guard.log`
- [red-reserved-triggers-native-0.xml](m08-tests/red-reserved-triggers-native-0.xml) — `docs/evidence/m08-tests/red-reserved-triggers-native-0.xml`
- [red-reserved-triggers.log](m08-tests/red-reserved-triggers.log) — `docs/evidence/m08-tests/red-reserved-triggers.log`
- [red-root-back-and-real-flow-native-0.xml](m08-tests/red-root-back-and-real-flow-native-0.xml) — `docs/evidence/m08-tests/red-root-back-and-real-flow-native-0.xml`
- [red-root-back-and-real-flow.log](m08-tests/red-root-back-and-real-flow.log) — `docs/evidence/m08-tests/red-root-back-and-real-flow.log`
- [source-provenance.zip](m08-tests/source-provenance.zip) — `docs/evidence/m08-tests/source-provenance.zip`
- [M08_CONTRACT.md](../planning/M08_CONTRACT.md) — `docs/planning/M08_CONTRACT.md`

## 修改

- [AGENTS.md](../../AGENTS.md) — `AGENTS.md`
- [README.md](../../README.md) — `README.md`
- [build.gradle.kts](../../app/build.gradle.kts) — `app/build.gradle.kts`
- [ProjectDatabaseTest.kt](../../app/src/androidTest/java/com/cooper/lithic/data/local/ProjectDatabaseTest.kt) — `app/src/androidTest/java/com/cooper/lithic/data/local/ProjectDatabaseTest.kt`
- [ProjectLifecycleViewModelTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleViewModelTest.kt) — `app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleViewModelTest.kt`
- [ProjectScreensTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectScreensTest.kt) — `app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectScreensTest.kt`
- [NavigationShellTest.kt](../../app/src/androidTest/java/com/cooper/lithic/ui/NavigationShellTest.kt) — `app/src/androidTest/java/com/cooper/lithic/ui/NavigationShellTest.kt`
- [LedgerUiState.kt](../../app/src/main/java/com/cooper/lithic/app/LedgerUiState.kt) — `app/src/main/java/com/cooper/lithic/app/LedgerUiState.kt`
- [LedgerUiStateHolder.kt](../../app/src/main/java/com/cooper/lithic/app/LedgerUiStateHolder.kt) — `app/src/main/java/com/cooper/lithic/app/LedgerUiStateHolder.kt`
- [LithicApp.kt](../../app/src/main/java/com/cooper/lithic/app/LithicApp.kt) — `app/src/main/java/com/cooper/lithic/app/LithicApp.kt`
- [LithicNavigation.kt](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt) — `app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt`
- [LithicRoutes.kt](../../app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt) — `app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt`
- [DatabaseConverters.kt](../../app/src/main/java/com/cooper/lithic/data/local/DatabaseConverters.kt) — `app/src/main/java/com/cooper/lithic/data/local/DatabaseConverters.kt`
- [DatabaseMigrations.kt](../../app/src/main/java/com/cooper/lithic/data/local/DatabaseMigrations.kt) — `app/src/main/java/com/cooper/lithic/data/local/DatabaseMigrations.kt`
- [LithicDatabase.kt](../../app/src/main/java/com/cooper/lithic/data/local/LithicDatabase.kt) — `app/src/main/java/com/cooper/lithic/data/local/LithicDatabase.kt`
- [HomeShell.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt) — `app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt`
- [ACCEPTANCE_MATRIX.md](../ACCEPTANCE_MATRIX.md) — `docs/ACCEPTANCE_MATRIX.md`
- [ARCHITECTURE.md](../ARCHITECTURE.md) — `docs/ARCHITECTURE.md`
- [CURRENT_STATE.md](../CURRENT_STATE.md) — `docs/CURRENT_STATE.md`
- [DECISIONS.md](../DECISIONS.md) — `docs/DECISIONS.md`
- [EXECUTION_PROTOCOL.md](../EXECUTION_PROTOCOL.md) — `docs/EXECUTION_PROTOCOL.md`
- [ROADMAP.md](../ROADMAP.md) — `docs/ROADMAP.md`
- [SPEC_ISSUES.md](../SPEC_ISSUES.md) — `docs/SPEC_ISSUES.md`
- [README.md](../planning/README.md) — `docs/planning/README.md`

## 删除

0。原43份规划/130 NOT_RUN案例/16完整跨模块补测、历史证据、v1 JSON与冻结领域/owner/settings原文件均保留。两份旧测试做真实v1/正式空态阶段适配，另两份旧测试仅加强状态快照和IME触摸同步；原359方法及业务断言保留。
