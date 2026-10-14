# M10 实际修改文件清单

基线 `57de897917473f255d43dfb306a8433b366c00eb`；功能分支 `feat/m10-local-cover-media`。

2026-10-14本轮实际266路径，与本次提交范围逐项核对；M10 READY_FOR_REVIEW，独立验收PENDING。提交自身SHA和push/远端一致以实际Git日志/最终回执登记，避免递归自引用。

当前计数：239新增、27修改、0删除，共266文件。

不含ignored构建缓存、脚本、APK、数据库/WAL/SHM、preferences或签名私钥。原43份与全部已冻结历史证据逐原字节保留。

## 类型汇总

| 类型 | 新增 | 修改 | 删除 |
|---|---|---|---|
| Debug版本与测试Runner配置 | 0 | 1 | 0 |
| 原始测试证据 | 132 | 0 | 0 |
| 原始证据换行保护 | 0 | 1 | 0 |
| 合成设备证据 | 83 | 0 | 0 |
| 当前文档/契约/checkpoint | 5 | 11 | 0 |
| 测试源码/Manifest/夹具 | 10 | 2 | 0 |
| 生产Kotlin | 9 | 12 | 0 |

## 完整路径

| 状态 | 文件 |
|---|---|
| M | [.gitattributes](../../.gitattributes) |
| M | [AGENTS.md](../../AGENTS.md) |
| M | [README.md](../../README.md) |
| M | [app/build.gradle.kts](../../app/build.gradle.kts) |
| A | [app/src/androidTest/AndroidManifest.xml](../../app/src/androidTest/AndroidManifest.xml) |
| A | [app/src/androidTest/java/com/cooper/lithic/data/media/LocalImageStoreTest.kt](../../app/src/androidTest/java/com/cooper/lithic/data/media/LocalImageStoreTest.kt) |
| A | [app/src/androidTest/java/com/cooper/lithic/data/media/M10ImageSourceFixtures.kt](../../app/src/androidTest/java/com/cooper/lithic/data/media/M10ImageSourceFixtures.kt) |
| A | [app/src/androidTest/java/com/cooper/lithic/data/media/M10ImageSourceProvider.java](../../app/src/androidTest/java/com/cooper/lithic/data/media/M10ImageSourceProvider.java) |
| A | [app/src/androidTest/java/com/cooper/lithic/data/media/M10ProcessBoundaryRunner.kt](../../app/src/androidTest/java/com/cooper/lithic/data/media/M10ProcessBoundaryRunner.kt) |
| A | [app/src/androidTest/java/com/cooper/lithic/data/media/MediaRepositoryTest.kt](../../app/src/androidTest/java/com/cooper/lithic/data/media/MediaRepositoryTest.kt) |
| A | [app/src/androidTest/java/com/cooper/lithic/feature/media/M10CoverEntryScreensTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/media/M10CoverEntryScreensTest.kt) |
| A | [app/src/androidTest/java/com/cooper/lithic/feature/media/M10CoverNavigationGuardTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/media/M10CoverNavigationGuardTest.kt) |
| A | [app/src/androidTest/java/com/cooper/lithic/feature/media/M10CoverRoutesTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/media/M10CoverRoutesTest.kt) |
| A | [app/src/androidTest/java/com/cooper/lithic/feature/media/M10CoverViewModelTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/media/M10CoverViewModelTest.kt) |
| M | [app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleFlowTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/subscription/M09SubscriptionLifecycleFlowTest.kt) |
| M | [app/src/androidTest/java/com/cooper/lithic/ui/UIComponentsTest.kt](../../app/src/androidTest/java/com/cooper/lithic/ui/UIComponentsTest.kt) |
| M | [app/src/main/java/com/cooper/lithic/app/LedgerUiState.kt](../../app/src/main/java/com/cooper/lithic/app/LedgerUiState.kt) |
| M | [app/src/main/java/com/cooper/lithic/app/LedgerUiStateHolder.kt](../../app/src/main/java/com/cooper/lithic/app/LedgerUiStateHolder.kt) |
| M | [app/src/main/java/com/cooper/lithic/app/LithicApp.kt](../../app/src/main/java/com/cooper/lithic/app/LithicApp.kt) |
| M | [app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt) |
| M | [app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt](../../app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt) |
| M | [app/src/main/java/com/cooper/lithic/app/SubscriptionNavigationPolicy.kt](../../app/src/main/java/com/cooper/lithic/app/SubscriptionNavigationPolicy.kt) |
| M | [app/src/main/java/com/cooper/lithic/data/backup/DataGenerationStore.kt](../../app/src/main/java/com/cooper/lithic/data/backup/DataGenerationStore.kt) |
| M | [app/src/main/java/com/cooper/lithic/data/local/MediaDao.kt](../../app/src/main/java/com/cooper/lithic/data/local/MediaDao.kt) |
| M | [app/src/main/java/com/cooper/lithic/data/local/ProjectDao.kt](../../app/src/main/java/com/cooper/lithic/data/local/ProjectDao.kt) |
| M | [app/src/main/java/com/cooper/lithic/data/local/SubscriptionDao.kt](../../app/src/main/java/com/cooper/lithic/data/local/SubscriptionDao.kt) |
| A | [app/src/main/java/com/cooper/lithic/data/media/AndroidImageCodec.kt](../../app/src/main/java/com/cooper/lithic/data/media/AndroidImageCodec.kt) |
| A | [app/src/main/java/com/cooper/lithic/data/media/LocalImageStore.kt](../../app/src/main/java/com/cooper/lithic/data/media/LocalImageStore.kt) |
| A | [app/src/main/java/com/cooper/lithic/data/media/MediaContracts.kt](../../app/src/main/java/com/cooper/lithic/data/media/MediaContracts.kt) |
| A | [app/src/main/java/com/cooper/lithic/data/media/MediaFileProtocol.kt](../../app/src/main/java/com/cooper/lithic/data/media/MediaFileProtocol.kt) |
| A | [app/src/main/java/com/cooper/lithic/data/media/MediaRepository.kt](../../app/src/main/java/com/cooper/lithic/data/media/MediaRepository.kt) |
| A | [app/src/main/java/com/cooper/lithic/feature/media/CoverDisplay.kt](../../app/src/main/java/com/cooper/lithic/feature/media/CoverDisplay.kt) |
| A | [app/src/main/java/com/cooper/lithic/feature/media/CoverSavedState.kt](../../app/src/main/java/com/cooper/lithic/feature/media/CoverSavedState.kt) |
| A | [app/src/main/java/com/cooper/lithic/feature/media/CoverViewModel.kt](../../app/src/main/java/com/cooper/lithic/feature/media/CoverViewModel.kt) |
| A | [app/src/main/java/com/cooper/lithic/feature/project/CoverPicker.kt](../../app/src/main/java/com/cooper/lithic/feature/project/CoverPicker.kt) |
| M | [app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt) |
| M | [app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt) |
| M | [docs/ACCEPTANCE_MATRIX.md](../../docs/ACCEPTANCE_MATRIX.md) |
| M | [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md) |
| M | [docs/CURRENT_STATE.md](../../docs/CURRENT_STATE.md) |
| M | [docs/DECISIONS.md](../../docs/DECISIONS.md) |
| M | [docs/EXECUTION_PROTOCOL.md](../../docs/EXECUTION_PROTOCOL.md) |
| M | [docs/ROADMAP.md](../../docs/ROADMAP.md) |
| M | [docs/SPEC_ISSUES.md](../../docs/SPEC_ISSUES.md) |
| A | [docs/checkpoints/2026-10-13-M10.md](../../docs/checkpoints/2026-10-13-M10.md) |
| A | [docs/evidence/M10-files.md](../../docs/evidence/M10-files.md) |
| A | [docs/evidence/M10-verification.json](../../docs/evidence/M10-verification.json) |
| A | [docs/evidence/M10-verification.md](../../docs/evidence/M10-verification.md) |
| A | [docs/evidence/m10-device/apk-final/apk-backup.txt](../../docs/evidence/m10-device/apk-final/apk-backup.txt) |
| A | [docs/evidence/m10-device/apk-final/apk-badging.txt](../../docs/evidence/m10-device/apk-final/apk-badging.txt) |
| A | [docs/evidence/m10-device/apk-final/apk-extraction.txt](../../docs/evidence/m10-device/apk-final/apk-extraction.txt) |
| A | [docs/evidence/m10-device/apk-final/apk-manifest.txt](../../docs/evidence/m10-device/apk-final/apk-manifest.txt) |
| A | [docs/evidence/m10-device/apk-final/apk-verification.json](../../docs/evidence/m10-device/apk-final/apk-verification.json) |
| A | [docs/evidence/m10-device/apk-final/extra-verification.json](../../docs/evidence/m10-device/apk-final/extra-verification.json) |
| A | [docs/evidence/m10-device/apk-final/signature.txt](../../docs/evidence/m10-device/apk-final/signature.txt) |
| A | [docs/evidence/m10-device/diagnostics/dialog-dismiss-intervention.json](../../docs/evidence/m10-device/diagnostics/dialog-dismiss-intervention.json) |
| A | [docs/evidence/m10-device/diagnostics/systemui-cleared-before-final.json](../../docs/evidence/m10-device/diagnostics/systemui-cleared-before-final.json) |
| A | [docs/evidence/m10-device/manual/actual-bad-source-protection.json](../../docs/evidence/m10-device/manual/actual-bad-source-protection.json) |
| A | [docs/evidence/m10-device/manual/final-storage-device-proof.json](../../docs/evidence/m10-device/manual/final-storage-device-proof.json) |
| A | [docs/evidence/m10-device/manual/guest-settings-final-restored.json](../../docs/evidence/m10-device/manual/guest-settings-final-restored.json) |
| A | [docs/evidence/m10-device/manual/guest-settings-new-before.json](../../docs/evidence/m10-device/manual/guest-settings-new-before.json) |
| A | [docs/evidence/m10-device/manual/guest-settings-new-extra-before.json](../../docs/evidence/m10-device/manual/guest-settings-new-extra-before.json) |
| A | [docs/evidence/m10-device/manual/known-media-physical-cleanup.json](../../docs/evidence/m10-device/manual/known-media-physical-cleanup.json) |
| A | [docs/evidence/m10-device/manual/m10-200-cover-gesture-guard.png](../../docs/evidence/m10-device/manual/m10-200-cover-gesture-guard.png) |
| A | [docs/evidence/m10-device/manual/m10-200-cover-gesture-guard.xml](../../docs/evidence/m10-device/manual/m10-200-cover-gesture-guard.xml) |
| A | [docs/evidence/m10-device/manual/m10-200-gesture-draft-guard.png](../../docs/evidence/m10-device/manual/m10-200-gesture-draft-guard.png) |
| A | [docs/evidence/m10-device/manual/m10-200-gesture-draft-guard.xml](../../docs/evidence/m10-device/manual/m10-200-gesture-draft-guard.xml) |
| A | [docs/evidence/m10-device/manual/m10-200-landscape-raw.png](../../docs/evidence/m10-device/manual/m10-200-landscape-raw.png) |
| A | [docs/evidence/m10-device/manual/m10-200-landscape-raw.xml](../../docs/evidence/m10-device/manual/m10-200-landscape-raw.xml) |
| A | [docs/evidence/m10-device/manual/m10-200-offline-native-picker.png](../../docs/evidence/m10-device/manual/m10-200-offline-native-picker.png) |
| A | [docs/evidence/m10-device/manual/m10-200-offline-native-picker.xml](../../docs/evidence/m10-device/manual/m10-200-offline-native-picker.xml) |
| A | [docs/evidence/m10-device/manual/m10-200-prepared-before-save.png](../../docs/evidence/m10-device/manual/m10-200-prepared-before-save.png) |
| A | [docs/evidence/m10-device/manual/m10-200-prepared-before-save.xml](../../docs/evidence/m10-device/manual/m10-200-prepared-before-save.xml) |
| A | [docs/evidence/m10-device/manual/m10-200-prepared-landscape.png](../../docs/evidence/m10-device/manual/m10-200-prepared-landscape.png) |
| A | [docs/evidence/m10-device/manual/m10-200-prepared-landscape.xml](../../docs/evidence/m10-device/manual/m10-200-prepared-landscape.xml) |
| A | [docs/evidence/m10-device/manual/m10-200-raw-error-visible.png](../../docs/evidence/m10-device/manual/m10-200-raw-error-visible.png) |
| A | [docs/evidence/m10-device/manual/m10-200-raw-error-visible.xml](../../docs/evidence/m10-device/manual/m10-200-raw-error-visible.xml) |
| A | [docs/evidence/m10-device/manual/m10-200-real-ime-raw.png](../../docs/evidence/m10-device/manual/m10-200-real-ime-raw.png) |
| A | [docs/evidence/m10-device/manual/m10-200-real-ime-raw.xml](../../docs/evidence/m10-device/manual/m10-200-real-ime-raw.xml) |
| A | [docs/evidence/m10-device/manual/m10-bad-source-error.png](../../docs/evidence/m10-device/manual/m10-bad-source-error.png) |
| A | [docs/evidence/m10-device/manual/m10-bad-source-error.xml](../../docs/evidence/m10-device/manual/m10-bad-source-error.xml) |
| A | [docs/evidence/m10-device/manual/m10-offline-cold-private-cover.png](../../docs/evidence/m10-device/manual/m10-offline-cold-private-cover.png) |
| A | [docs/evidence/m10-device/manual/m10-offline-cold-private-cover.xml](../../docs/evidence/m10-device/manual/m10-offline-cold-private-cover.xml) |
| A | [docs/evidence/m10-device/manual/m10-photo-a-source-gone-view.png](../../docs/evidence/m10-device/manual/m10-photo-a-source-gone-view.png) |
| A | [docs/evidence/m10-device/manual/m10-photo-a-source-gone-view.xml](../../docs/evidence/m10-device/manual/m10-photo-a-source-gone-view.xml) |
| A | [docs/evidence/m10-device/manual/m10-saved-task2-restored-before-scroll.png](../../docs/evidence/m10-device/manual/m10-saved-task2-restored-before-scroll.png) |
| A | [docs/evidence/m10-device/manual/m10-saved-task2-restored-before-scroll.xml](../../docs/evidence/m10-device/manual/m10-saved-task2-restored-before-scroll.xml) |
| A | [docs/evidence/m10-device/manual/m10-shared-subscription-after-project-delete.png](../../docs/evidence/m10-device/manual/m10-shared-subscription-after-project-delete.png) |
| A | [docs/evidence/m10-device/manual/m10-shared-subscription-after-project-delete.xml](../../docs/evidence/m10-device/manual/m10-shared-subscription-after-project-delete.xml) |
| A | [docs/evidence/m10-device/manual/m10-subscription-alpha-view.png](../../docs/evidence/m10-device/manual/m10-subscription-alpha-view.png) |
| A | [docs/evidence/m10-device/manual/m10-subscription-alpha-view.xml](../../docs/evidence/m10-device/manual/m10-subscription-alpha-view.xml) |
| A | [docs/evidence/m10-device/manual/m10-subscription-recovered-alpha-view.png](../../docs/evidence/m10-device/manual/m10-subscription-recovered-alpha-view.png) |
| A | [docs/evidence/m10-device/manual/m10-subscription-recovered-alpha-view.xml](../../docs/evidence/m10-device/manual/m10-subscription-recovered-alpha-view.xml) |
| A | [docs/evidence/m10-device/manual/m10-system-picker-native.png](../../docs/evidence/m10-device/manual/m10-system-picker-native.png) |
| A | [docs/evidence/m10-device/manual/m10-system-picker-native.xml](../../docs/evidence/m10-device/manual/m10-system-picker-native.xml) |
| A | [docs/evidence/m10-device/manual/manual-attempt-notes.json](../../docs/evidence/m10-device/manual/manual-attempt-notes.json) |
| A | [docs/evidence/m10-device/manual/manual-public-manifest.json](../../docs/evidence/m10-device/manual/manual-public-manifest.json) |
| A | [docs/evidence/m10-device/manual/media-m10-device-final-restored.json](../../docs/evidence/m10-device/manual/media-m10-device-final-restored.json) |
| A | [docs/evidence/m10-device/manual/media-m10-photo-a-private.json](../../docs/evidence/m10-device/manual/media-m10-photo-a-private.json) |
| A | [docs/evidence/m10-device/manual/media-m10-photo-b-private.json](../../docs/evidence/m10-device/manual/media-m10-photo-b-private.json) |
| A | [docs/evidence/m10-device/manual/media-m10-project-deleted-shared-sub-kept.json](../../docs/evidence/m10-device/manual/media-m10-project-deleted-shared-sub-kept.json) |
| A | [docs/evidence/m10-device/manual/media-m10-shared-project-removed.json](../../docs/evidence/m10-device/manual/media-m10-shared-project-removed.json) |
| A | [docs/evidence/m10-device/manual/media-m10-shared-two-refs-confirmed.json](../../docs/evidence/m10-device/manual/media-m10-shared-two-refs-confirmed.json) |
| A | [docs/evidence/m10-device/manual/media-m10-source-gone-cold.json](../../docs/evidence/m10-device/manual/media-m10-source-gone-cold.json) |
| A | [docs/evidence/m10-device/manual/media-m10-subscription-final-ref-removed.json](../../docs/evidence/m10-device/manual/media-m10-subscription-final-ref-removed.json) |
| A | [docs/evidence/m10-device/manual/photo-a-source-removed.json](../../docs/evidence/m10-device/manual/photo-a-source-removed.json) |
| A | [docs/evidence/m10-device/manual/photo-a-to-b-replacement.json](../../docs/evidence/m10-device/manual/photo-a-to-b-replacement.json) |
| A | [docs/evidence/m10-device/manual/reconstructed-m09-installed.json](../../docs/evidence/m10-device/manual/reconstructed-m09-installed.json) |
| A | [docs/evidence/m10-device/manual/reconstructed-m10-upgraded.json](../../docs/evidence/m10-device/manual/reconstructed-m10-upgraded.json) |
| A | [docs/evidence/m10-device/manual/reconstructed-upgrade-preservation.json](../../docs/evidence/m10-device/manual/reconstructed-upgrade-preservation.json) |
| A | [docs/evidence/m10-device/manual/saved-task-kill-confirmed.json](../../docs/evidence/m10-device/manual/saved-task-kill-confirmed.json) |
| A | [docs/evidence/m10-device/manual/saved-task-second-kill.json](../../docs/evidence/m10-device/manual/saved-task-second-kill.json) |
| A | [docs/evidence/m10-device/manual/saved-task-second-pre-kill.json](../../docs/evidence/m10-device/manual/saved-task-second-pre-kill.json) |
| A | [docs/evidence/m10-device/manual/saved-task-second-ready.png](../../docs/evidence/m10-device/manual/saved-task-second-ready.png) |
| A | [docs/evidence/m10-device/manual/saved-task-second-ready.xml](../../docs/evidence/m10-device/manual/saved-task-second-ready.xml) |
| A | [docs/evidence/m10-device/manual/saved-task-second-restored.json](../../docs/evidence/m10-device/manual/saved-task-second-restored.json) |
| A | [docs/evidence/m10-device/manual/share-synthetic-actual.json](../../docs/evidence/m10-device/manual/share-synthetic-actual.json) |
| A | [docs/evidence/m10-device/manual/share-synthetic-actual.log](../../docs/evidence/m10-device/manual/share-synthetic-actual.log) |
| A | [docs/evidence/m10-device/manual/share-synthetic-after-ui-actual.log](../../docs/evidence/m10-device/manual/share-synthetic-after-ui-actual.log) |
| A | [docs/evidence/m10-device/manual/system-picker-cancel-zero-write.json](../../docs/evidence/m10-device/manual/system-picker-cancel-zero-write.json) |
| A | [docs/evidence/m10-device/process/process-boundaries.json](../../docs/evidence/m10-device/process/process-boundaries.json) |
| A | [docs/evidence/m10-device/process/process-db-before-cleanup-kill.log](../../docs/evidence/m10-device/process/process-db-before-cleanup-kill.log) |
| A | [docs/evidence/m10-device/process/process-db-before-cleanup-verify.log](../../docs/evidence/m10-device/process/process-db-before-cleanup-verify.log) |
| A | [docs/evidence/m10-device/process/process-db-before-cleanup.json](../../docs/evidence/m10-device/process/process-db-before-cleanup.json) |
| A | [docs/evidence/m10-device/process/process-file-before-db-kill.log](../../docs/evidence/m10-device/process/process-file-before-db-kill.log) |
| A | [docs/evidence/m10-device/process/process-file-before-db-verify.log](../../docs/evidence/m10-device/process/process-file-before-db-verify.log) |
| A | [docs/evidence/m10-device/process/process-file-before-db.json](../../docs/evidence/m10-device/process/process-file-before-db.json) |
| A | [docs/evidence/m10-device/process/process-runner-build.log](../../docs/evidence/m10-device/process/process-runner-build.log) |
| A | [docs/evidence/m10-device/process/process-runner-registration.json](../../docs/evidence/m10-device/process/process-runner-registration.json) |
| A | [docs/evidence/m10-device/process/process-runner-unregistered-failed.json](../../docs/evidence/m10-device/process/process-runner-unregistered-failed.json) |
| A | [docs/evidence/m10-device/process/process-runner-unregistered-failed.log](../../docs/evidence/m10-device/process/process-runner-unregistered-failed.log) |
| A | [docs/evidence/m10-tests/final-index-inputs.json](../../docs/evidence/m10-tests/final-index-inputs.json) |
| A | [docs/evidence/m10-tests/final-split-methods.json](../../docs/evidence/m10-tests/final-split-methods.json) |
| A | [docs/evidence/m10-tests/final-split-verification.json](../../docs/evidence/m10-tests/final-split-verification.json) |
| A | [docs/evidence/m10-tests/green-android-connected-final-android-0.xml](../../docs/evidence/m10-tests/green-android-connected-final-android-0.xml) |
| A | [docs/evidence/m10-tests/green-android-connected-final-source.zip](../../docs/evidence/m10-tests/green-android-connected-final-source.zip) |
| A | [docs/evidence/m10-tests/green-android-connected-final-summary.json](../../docs/evidence/m10-tests/green-android-connected-final-summary.json) |
| A | [docs/evidence/m10-tests/green-android-connected-final.log](../../docs/evidence/m10-tests/green-android-connected-final.log) |
| A | [docs/evidence/m10-tests/green-android-final-clean-source.zip](../../docs/evidence/m10-tests/green-android-final-clean-source.zip) |
| A | [docs/evidence/m10-tests/green-android-final-clean-summary.json](../../docs/evidence/m10-tests/green-android-final-clean-summary.json) |
| A | [docs/evidence/m10-tests/green-android-final-clean.log](../../docs/evidence/m10-tests/green-android-final-clean.log) |
| A | [docs/evidence/m10-tests/green-android-final-rerun-android-0.xml](../../docs/evidence/m10-tests/green-android-final-rerun-android-0.xml) |
| A | [docs/evidence/m10-tests/green-android-final-rerun-source.zip](../../docs/evidence/m10-tests/green-android-final-rerun-source.zip) |
| A | [docs/evidence/m10-tests/green-android-final-rerun-summary.json](../../docs/evidence/m10-tests/green-android-final-rerun-summary.json) |
| A | [docs/evidence/m10-tests/green-android-final-rerun.log](../../docs/evidence/m10-tests/green-android-final-rerun.log) |
| A | [docs/evidence/m10-tests/green-android-systemui-clean-final-android-0.xml](../../docs/evidence/m10-tests/green-android-systemui-clean-final-android-0.xml) |
| A | [docs/evidence/m10-tests/green-android-systemui-clean-final-source.zip](../../docs/evidence/m10-tests/green-android-systemui-clean-final-source.zip) |
| A | [docs/evidence/m10-tests/green-android-systemui-clean-final-summary.json](../../docs/evidence/m10-tests/green-android-systemui-clean-final-summary.json) |
| A | [docs/evidence/m10-tests/green-android-systemui-clean-final.log](../../docs/evidence/m10-tests/green-android-systemui-clean-final.log) |
| A | [docs/evidence/m10-tests/green-final-lifecycle-android-0.xml](../../docs/evidence/m10-tests/green-final-lifecycle-android-0.xml) |
| A | [docs/evidence/m10-tests/green-final-lifecycle-source.zip](../../docs/evidence/m10-tests/green-final-lifecycle-source.zip) |
| A | [docs/evidence/m10-tests/green-final-lifecycle-summary.json](../../docs/evidence/m10-tests/green-final-lifecycle-summary.json) |
| A | [docs/evidence/m10-tests/green-final-lifecycle.log](../../docs/evidence/m10-tests/green-final-lifecycle.log) |
| A | [docs/evidence/m10-tests/green-full-final-rerun-android-0.xml](../../docs/evidence/m10-tests/green-full-final-rerun-android-0.xml) |
| A | [docs/evidence/m10-tests/green-full-final-rerun-jvm-0.xml](../../docs/evidence/m10-tests/green-full-final-rerun-jvm-0.xml) |
| A | [docs/evidence/m10-tests/green-full-final-rerun-jvm-1.xml](../../docs/evidence/m10-tests/green-full-final-rerun-jvm-1.xml) |
| A | [docs/evidence/m10-tests/green-full-final-rerun-jvm-2.xml](../../docs/evidence/m10-tests/green-full-final-rerun-jvm-2.xml) |
| A | [docs/evidence/m10-tests/green-full-final-rerun-jvm-3.xml](../../docs/evidence/m10-tests/green-full-final-rerun-jvm-3.xml) |
| A | [docs/evidence/m10-tests/green-full-final-rerun-jvm-4.xml](../../docs/evidence/m10-tests/green-full-final-rerun-jvm-4.xml) |
| A | [docs/evidence/m10-tests/green-full-final-rerun-jvm-5.xml](../../docs/evidence/m10-tests/green-full-final-rerun-jvm-5.xml) |
| A | [docs/evidence/m10-tests/green-full-final-rerun-jvm-6.xml](../../docs/evidence/m10-tests/green-full-final-rerun-jvm-6.xml) |
| A | [docs/evidence/m10-tests/green-full-final-rerun-source.zip](../../docs/evidence/m10-tests/green-full-final-rerun-source.zip) |
| A | [docs/evidence/m10-tests/green-full-final-rerun-summary.json](../../docs/evidence/m10-tests/green-full-final-rerun-summary.json) |
| A | [docs/evidence/m10-tests/green-full-final-rerun.log](../../docs/evidence/m10-tests/green-full-final-rerun.log) |
| A | [docs/evidence/m10-tests/green-full-final-source.zip](../../docs/evidence/m10-tests/green-full-final-source.zip) |
| A | [docs/evidence/m10-tests/green-full-final.log](../../docs/evidence/m10-tests/green-full-final.log) |
| A | [docs/evidence/m10-tests/green-full-restored-device-android-0.xml](../../docs/evidence/m10-tests/green-full-restored-device-android-0.xml) |
| A | [docs/evidence/m10-tests/green-full-restored-device-jvm-0.xml](../../docs/evidence/m10-tests/green-full-restored-device-jvm-0.xml) |
| A | [docs/evidence/m10-tests/green-full-restored-device-jvm-1.xml](../../docs/evidence/m10-tests/green-full-restored-device-jvm-1.xml) |
| A | [docs/evidence/m10-tests/green-full-restored-device-jvm-2.xml](../../docs/evidence/m10-tests/green-full-restored-device-jvm-2.xml) |
| A | [docs/evidence/m10-tests/green-full-restored-device-jvm-3.xml](../../docs/evidence/m10-tests/green-full-restored-device-jvm-3.xml) |
| A | [docs/evidence/m10-tests/green-full-restored-device-jvm-4.xml](../../docs/evidence/m10-tests/green-full-restored-device-jvm-4.xml) |
| A | [docs/evidence/m10-tests/green-full-restored-device-jvm-5.xml](../../docs/evidence/m10-tests/green-full-restored-device-jvm-5.xml) |
| A | [docs/evidence/m10-tests/green-full-restored-device-jvm-6.xml](../../docs/evidence/m10-tests/green-full-restored-device-jvm-6.xml) |
| A | [docs/evidence/m10-tests/green-full-restored-device-source.zip](../../docs/evidence/m10-tests/green-full-restored-device-source.zip) |
| A | [docs/evidence/m10-tests/green-full-restored-device-summary.json](../../docs/evidence/m10-tests/green-full-restored-device-summary.json) |
| A | [docs/evidence/m10-tests/green-full-restored-device.log](../../docs/evidence/m10-tests/green-full-restored-device.log) |
| A | [docs/evidence/m10-tests/green-full-runner-final-android-0.xml](../../docs/evidence/m10-tests/green-full-runner-final-android-0.xml) |
| A | [docs/evidence/m10-tests/green-full-runner-final-jvm-0.xml](../../docs/evidence/m10-tests/green-full-runner-final-jvm-0.xml) |
| A | [docs/evidence/m10-tests/green-full-runner-final-jvm-1.xml](../../docs/evidence/m10-tests/green-full-runner-final-jvm-1.xml) |
| A | [docs/evidence/m10-tests/green-full-runner-final-jvm-2.xml](../../docs/evidence/m10-tests/green-full-runner-final-jvm-2.xml) |
| A | [docs/evidence/m10-tests/green-full-runner-final-jvm-3.xml](../../docs/evidence/m10-tests/green-full-runner-final-jvm-3.xml) |
| A | [docs/evidence/m10-tests/green-full-runner-final-jvm-4.xml](../../docs/evidence/m10-tests/green-full-runner-final-jvm-4.xml) |
| A | [docs/evidence/m10-tests/green-full-runner-final-jvm-5.xml](../../docs/evidence/m10-tests/green-full-runner-final-jvm-5.xml) |
| A | [docs/evidence/m10-tests/green-full-runner-final-jvm-6.xml](../../docs/evidence/m10-tests/green-full-runner-final-jvm-6.xml) |
| A | [docs/evidence/m10-tests/green-full-runner-final-source.zip](../../docs/evidence/m10-tests/green-full-runner-final-source.zip) |
| A | [docs/evidence/m10-tests/green-full-runner-final-summary.json](../../docs/evidence/m10-tests/green-full-runner-final-summary.json) |
| A | [docs/evidence/m10-tests/green-full-runner-final.log](../../docs/evidence/m10-tests/green-full-runner-final.log) |
| A | [docs/evidence/m10-tests/green-full-verified-android-0.xml](../../docs/evidence/m10-tests/green-full-verified-android-0.xml) |
| A | [docs/evidence/m10-tests/green-full-verified-source.zip](../../docs/evidence/m10-tests/green-full-verified-source.zip) |
| A | [docs/evidence/m10-tests/green-full-verified-summary.json](../../docs/evidence/m10-tests/green-full-verified-summary.json) |
| A | [docs/evidence/m10-tests/green-full-verified.log](../../docs/evidence/m10-tests/green-full-verified.log) |
| A | [docs/evidence/m10-tests/green-legacy-ui-loading-sync-android-0.xml](../../docs/evidence/m10-tests/green-legacy-ui-loading-sync-android-0.xml) |
| A | [docs/evidence/m10-tests/green-legacy-ui-loading-sync-source.zip](../../docs/evidence/m10-tests/green-legacy-ui-loading-sync-source.zip) |
| A | [docs/evidence/m10-tests/green-legacy-ui-loading-sync-summary.json](../../docs/evidence/m10-tests/green-legacy-ui-loading-sync-summary.json) |
| A | [docs/evidence/m10-tests/green-legacy-ui-loading-sync.log](../../docs/evidence/m10-tests/green-legacy-ui-loading-sync.log) |
| A | [docs/evidence/m10-tests/green-media-behavior-android-0.xml](../../docs/evidence/m10-tests/green-media-behavior-android-0.xml) |
| A | [docs/evidence/m10-tests/green-media-behavior-source.zip](../../docs/evidence/m10-tests/green-media-behavior-source.zip) |
| A | [docs/evidence/m10-tests/green-media-behavior-summary.json](../../docs/evidence/m10-tests/green-media-behavior-summary.json) |
| A | [docs/evidence/m10-tests/green-media-behavior.log](../../docs/evidence/m10-tests/green-media-behavior.log) |
| A | [docs/evidence/m10-tests/green-media-initial-source.zip](../../docs/evidence/m10-tests/green-media-initial-source.zip) |
| A | [docs/evidence/m10-tests/green-media-initial-summary.json](../../docs/evidence/m10-tests/green-media-initial-summary.json) |
| A | [docs/evidence/m10-tests/green-media-initial.log](../../docs/evidence/m10-tests/green-media-initial.log) |
| A | [docs/evidence/m10-tests/green-media-integrated-android-0.xml](../../docs/evidence/m10-tests/green-media-integrated-android-0.xml) |
| A | [docs/evidence/m10-tests/green-media-integrated-source.zip](../../docs/evidence/m10-tests/green-media-integrated-source.zip) |
| A | [docs/evidence/m10-tests/green-media-integrated-summary.json](../../docs/evidence/m10-tests/green-media-integrated-summary.json) |
| A | [docs/evidence/m10-tests/green-media-integrated.log](../../docs/evidence/m10-tests/green-media-integrated.log) |
| A | [docs/evidence/m10-tests/lint-results-debug.xml](../../docs/evidence/m10-tests/lint-results-debug.xml) |
| A | [docs/evidence/m10-tests/phase-provenance.json](../../docs/evidence/m10-tests/phase-provenance.json) |
| A | [docs/evidence/m10-tests/red-complete-media-android-0.xml](../../docs/evidence/m10-tests/red-complete-media-android-0.xml) |
| A | [docs/evidence/m10-tests/red-complete-media-source.zip](../../docs/evidence/m10-tests/red-complete-media-source.zip) |
| A | [docs/evidence/m10-tests/red-complete-media-summary.json](../../docs/evidence/m10-tests/red-complete-media-summary.json) |
| A | [docs/evidence/m10-tests/red-complete-media.log](../../docs/evidence/m10-tests/red-complete-media.log) |
| A | [docs/evidence/m10-tests/red-detail-entry-android-0.xml](../../docs/evidence/m10-tests/red-detail-entry-android-0.xml) |
| A | [docs/evidence/m10-tests/red-detail-entry-source.zip](../../docs/evidence/m10-tests/red-detail-entry-source.zip) |
| A | [docs/evidence/m10-tests/red-detail-entry-summary.json](../../docs/evidence/m10-tests/red-detail-entry-summary.json) |
| A | [docs/evidence/m10-tests/red-detail-entry.log](../../docs/evidence/m10-tests/red-detail-entry.log) |
| A | [docs/evidence/m10-tests/red-final-lifecycle-android-0.xml](../../docs/evidence/m10-tests/red-final-lifecycle-android-0.xml) |
| A | [docs/evidence/m10-tests/red-final-lifecycle-source.zip](../../docs/evidence/m10-tests/red-final-lifecycle-source.zip) |
| A | [docs/evidence/m10-tests/red-final-lifecycle-summary.json](../../docs/evidence/m10-tests/red-final-lifecycle-summary.json) |
| A | [docs/evidence/m10-tests/red-final-lifecycle.log](../../docs/evidence/m10-tests/red-final-lifecycle.log) |
| A | [docs/evidence/m10-tests/red-m09-flow-capture-source.zip](../../docs/evidence/m10-tests/red-m09-flow-capture-source.zip) |
| A | [docs/evidence/m10-tests/red-m09-flow-capture-summary.json](../../docs/evidence/m10-tests/red-m09-flow-capture-summary.json) |
| A | [docs/evidence/m10-tests/red-m09-flow-capture.log](../../docs/evidence/m10-tests/red-m09-flow-capture.log) |
| A | [docs/evidence/m10-tests/red-m09-flow-captured-android-0.xml](../../docs/evidence/m10-tests/red-m09-flow-captured-android-0.xml) |
| A | [docs/evidence/m10-tests/red-m09-flow-captured-source.zip](../../docs/evidence/m10-tests/red-m09-flow-captured-source.zip) |
| A | [docs/evidence/m10-tests/red-m09-flow-captured-summary.json](../../docs/evidence/m10-tests/red-m09-flow-captured-summary.json) |
| A | [docs/evidence/m10-tests/red-m09-flow-captured.log](../../docs/evidence/m10-tests/red-m09-flow-captured.log) |
| A | [docs/evidence/m10-tests/red-media-behavior-android-0.xml](../../docs/evidence/m10-tests/red-media-behavior-android-0.xml) |
| A | [docs/evidence/m10-tests/red-media-behavior-source.zip](../../docs/evidence/m10-tests/red-media-behavior-source.zip) |
| A | [docs/evidence/m10-tests/red-media-behavior-summary.json](../../docs/evidence/m10-tests/red-media-behavior-summary.json) |
| A | [docs/evidence/m10-tests/red-media-behavior.log](../../docs/evidence/m10-tests/red-media-behavior.log) |
| A | [docs/evidence/m10-tests/red-media-contract-android-0.xml](../../docs/evidence/m10-tests/red-media-contract-android-0.xml) |
| A | [docs/evidence/m10-tests/red-media-contract-source.zip](../../docs/evidence/m10-tests/red-media-contract-source.zip) |
| A | [docs/evidence/m10-tests/red-media-contract-summary.json](../../docs/evidence/m10-tests/red-media-contract-summary.json) |
| A | [docs/evidence/m10-tests/red-media-contract.log](../../docs/evidence/m10-tests/red-media-contract.log) |
| A | [docs/evidence/m10-tests/red-navigation-captured-android-0.xml](../../docs/evidence/m10-tests/red-navigation-captured-android-0.xml) |
| A | [docs/evidence/m10-tests/red-navigation-captured-source.zip](../../docs/evidence/m10-tests/red-navigation-captured-source.zip) |
| A | [docs/evidence/m10-tests/red-navigation-captured-summary.json](../../docs/evidence/m10-tests/red-navigation-captured-summary.json) |
| A | [docs/evidence/m10-tests/red-navigation-captured.log](../../docs/evidence/m10-tests/red-navigation-captured.log) |
| A | [docs/evidence/m10-tests/red-navigation-diagnostic-android-0.xml](../../docs/evidence/m10-tests/red-navigation-diagnostic-android-0.xml) |
| A | [docs/evidence/m10-tests/red-navigation-diagnostic-source.zip](../../docs/evidence/m10-tests/red-navigation-diagnostic-source.zip) |
| A | [docs/evidence/m10-tests/red-navigation-diagnostic-summary.json](../../docs/evidence/m10-tests/red-navigation-diagnostic-summary.json) |
| A | [docs/evidence/m10-tests/red-navigation-diagnostic.log](../../docs/evidence/m10-tests/red-navigation-diagnostic.log) |
| A | [docs/evidence/m10-tests/red-navigation-state-tree-android-0.xml](../../docs/evidence/m10-tests/red-navigation-state-tree-android-0.xml) |
| A | [docs/evidence/m10-tests/red-navigation-state-tree-source.zip](../../docs/evidence/m10-tests/red-navigation-state-tree-source.zip) |
| A | [docs/evidence/m10-tests/red-navigation-state-tree-summary.json](../../docs/evidence/m10-tests/red-navigation-state-tree-summary.json) |
| A | [docs/evidence/m10-tests/red-navigation-state-tree.log](../../docs/evidence/m10-tests/red-navigation-state-tree.log) |
| A | [docs/evidence/m10-tests/red-real-uri-nominal-android-0.xml](../../docs/evidence/m10-tests/red-real-uri-nominal-android-0.xml) |
| A | [docs/evidence/m10-tests/red-real-uri-nominal-source.zip](../../docs/evidence/m10-tests/red-real-uri-nominal-source.zip) |
| A | [docs/evidence/m10-tests/red-real-uri-nominal-summary.json](../../docs/evidence/m10-tests/red-real-uri-nominal-summary.json) |
| A | [docs/evidence/m10-tests/red-real-uri-nominal.log](../../docs/evidence/m10-tests/red-real-uri-nominal.log) |
| A | [docs/evidence/m10-tests/red-repository-guards-android-0.xml](../../docs/evidence/m10-tests/red-repository-guards-android-0.xml) |
| A | [docs/evidence/m10-tests/red-repository-guards-source.zip](../../docs/evidence/m10-tests/red-repository-guards-source.zip) |
| A | [docs/evidence/m10-tests/red-repository-guards-summary.json](../../docs/evidence/m10-tests/red-repository-guards-summary.json) |
| A | [docs/evidence/m10-tests/red-repository-guards.log](../../docs/evidence/m10-tests/red-repository-guards.log) |
| A | [docs/evidence/m10-tests/red-risk-recheck-android-0.xml](../../docs/evidence/m10-tests/red-risk-recheck-android-0.xml) |
| A | [docs/evidence/m10-tests/red-risk-recheck-source.zip](../../docs/evidence/m10-tests/red-risk-recheck-source.zip) |
| A | [docs/evidence/m10-tests/red-risk-recheck-summary.json](../../docs/evidence/m10-tests/red-risk-recheck-summary.json) |
| A | [docs/evidence/m10-tests/red-risk-recheck.log](../../docs/evidence/m10-tests/red-risk-recheck.log) |
| A | [docs/evidence/m10-tests/runtime-dependencies-verified.log](../../docs/evidence/m10-tests/runtime-dependencies-verified.log) |
| A | [docs/evidence/m10-tests/runtime-dependency-delta.json](../../docs/evidence/m10-tests/runtime-dependency-delta.json) |
| A | [docs/evidence/m10-tests/test-provenance.json](../../docs/evidence/m10-tests/test-provenance.json) |
| M | [docs/planning/CLARIFICATIONS.md](../../docs/planning/CLARIFICATIONS.md) |
| A | [docs/planning/M10_CONTRACT.md](../../docs/planning/M10_CONTRACT.md) |
| M | [docs/planning/README.md](../../docs/planning/README.md) |

## 证据范围与保留边界

现有26个完成phase、44份XML与1个无完成summary的中断尝试分别登记；所有已公开原始字节核对相同。
两份旧驱动只允许精确的10秒加载/回调同步增强，15旧方法、117完整断言和原点击次数不变；44旧测试源保持原字节。
测试Runner仅androidTest注册，无action转交原JUnit；相同最终147输入已实际重新运行，86 JVM＋566 Android共652个唯一PASS。第一整体阶段FAIL与9项Android失败保留，最终覆盖明确来自分开执行。
真实两处SIGKILL、系统选图/共享资源/离线/任务恢复/设置复原、最终APK和保护范围见M10报告。先前原AVD误重建/就绪检查失败保留，不冒称原设备连续升级或M12恢复。提交前检查差异/敏感信息/所有本地链接与索引；该文档核查不计产品测试。
本清单不含ignored APK、签名私钥、DB/WAL/SHM/preferences、真实图片或原相册；仅发布明确合成截图/metadata白名单。原130条与16组完整跨模块案例及真机/API26/OEM/Release/完整TalkBack/真实LMK/M11M12仍NOT_RUN，不进入M11。
