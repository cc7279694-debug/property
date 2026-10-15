# UI-C1 阶段1视觉精修文件清单

基线 `53fd944fe70a912fd4c1e1fa76d4ec31d1a103d5`。无删除；四个生产展示文件及相关测试/文档/新证据，范围见[精修报告](../../UI-C1-refinement-verification.md)。

| 路径 | 类型 |
|---|---|
| `.gitattributes` | 修改 |
| `AGENTS.md` | 修改 |
| `README.md` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/feature/home/UIC1HomeSampleTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/feature/home/UIC1RefinedHomeTest.kt` | 新增 |
| `app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectEntryFlowTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleFlowTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/feature/project/ProjectLifecycleScreensTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/feature/project/UIC1RefinedDetailTest.kt` | 新增 |
| `app/src/androidTest/java/com/cooper/lithic/feature/project/UIC1RefinedListTest.kt` | 新增 |
| `app/src/androidTest/java/com/cooper/lithic/ui/NavigationShellTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/ui/StorageUiStateTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/ui/UIC1EmptyVisualEvidenceTest.kt` | 修改 |
| `app/src/androidTest/java/com/cooper/lithic/ui/UIC1RefinedVisualEvidenceTest.kt` | 新增 |
| `app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt` | 修改 |
| `app/src/main/java/com/cooper/lithic/feature/media/CoverDisplay.kt` | 修改 |
| `app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt` | 修改 |
| `app/src/main/java/com/cooper/lithic/feature/project/ProjectListScreen.kt` | 修改 |
| `docs/CURRENT_STATE.md` | 修改 |
| `docs/DECISIONS.md` | 修改 |
| `docs/checkpoints/2026-10-15-UI-C1-refinement.md` | 新增 |
| `docs/design/UI_C1_SPEC.md` | 修改 |
| `docs/evidence/UI-C1-refinement-verification.md` | 新增 |
| `docs/planning/UI_C1_PLAN.md` | 修改 |

## 新证据目录

27张复审原始PNG、另列的失败/转场诊断、10个实际执行阶段的日志/鲜取XML/输入ZIP/逐方法summary、最终APK及测试APK指纹/签名/Manifest/备份配置、真实工具输出、Lint、前后数据保护摘要、可复核脚本、图册及来源登记。

原APK/测试APK/DB/WAL/SHM/私有原图/密钥不提交。原43份规划、130案例、16组边界、53fd944原19图/旧报告/checkpoint和M10证据不改。

原692个方法身份保留，本轮4个新测试class共9方法，总701；旧7份测试只做等价计数/返回语义选择器与隔离空态截图窗口适配。旧表单/JPEG源码和Nav3守卫均无改动。

完整字节清单见file-manifest.json；提交前检查见precommit-checks.json。输入ZIP为各阶段登记构建输入，不是完整工具环境或用户数据备份。
