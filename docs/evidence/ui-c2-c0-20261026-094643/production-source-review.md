# UI-C2 C0 生产接线只读核对

**SOURCE_REVIEW_ONLY / APP_VISUAL_PENDING**。来源`fef1f8efcf9afe170d36f686ee02a7c3d88c2f5f`，当前产品完善分支。未改生产文件，未构建、未执行或准备ADB/设备操作；本笔记只说明源码接线，不能替代最终APK的Manifest/DEX/签名或运行证据。

## 正常入口

- `app/src/main/AndroidManifest.xml:8,29–35`指定`LithicApplication`与Launcher `MainActivity`，不是HTML/WebView或Instrumentation入口；Provider authority以`${applicationId}`隔离（:18–23）。
- `app/src/main/java/com/cooper/lithic/app/LithicApplication.kt:9–19`创建唯一`AppContainer`并正常启动本地存储与已有提醒Runtime。`AppContainer.kt:30–79`以应用scope/既有DataGenerationStore打开真实generation，不是plain Application隔离夹具。
- `MainActivity.kt:55–94`读取该Application的container，创建真实Ledger/导出/恢复ViewModel，使用SAF选择器与原BackupExportService/RestoreCoordinator，`setContent`运行`LithicTheme`和`LithicApp`。
- `LedgerUiStateHolder.kt:43–63`从同一generation/coordinator建立真实Repository并读取Overview；`LithicApp.kt:42–47`仅Ready且以generationId为key进入`LithicNavigation`。初始化/失败走原保护页，不把失败变空账本。

## 当前生产页面确实被调用

以下Nav路径均在`app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt`：

| 范围 | 真实接线 |
|---|---|
| 首页 | :408–418 NavDisplay调用当前HomeShell，传入真实Overview、同代预览和原callbacks；`feature/home/HomeShell.kt:93,142–146,173–184`为已录入数量卡与56dp圆FAB，非设计源集 |
| 项目 | :502–518、558–585调用当前列表/详情/创建编辑/金额/结束/纠错/删除组件；当前列表`ProjectListScreen.kt:45,93`使用C2PageHeader/EntryCard，详情`ProjectDetailScreen.kt:235`使用当前身份区 |
| 订阅 | :587–629调用当前列表、创建、详情、续费与状态操作；`SubscriptionDetailScreen.kt:78,99,133`为当前档案分组，`SubscriptionEditorScreen.kt:65`为当前C2FormScaffold |
| 回看/搜索 | :419–461接当前ReviewShell、类别/年度/完整趋势/复盘；:492–500按真实搜索结果身份进入详情。真实趋势/选日/返回走原VM与route，不使用HTML曲线 |
| 设置/数据 | :463–472接当前SettingsShell/计算说明/About；`SettingsShell.kt:57–101`保留导出恢复互斥、类型化反馈、仅PREVIEW显示原RestorePreviewScreen。`MainActivity.kt:84–93`继续复用原选择→验证预览→confirmOverwrite；没有新绕过确认入口 |

导航仍由`LithicNavigation.kt:238–244`的`requestLeave`检查恢复状态及原dirty/pending决定；:637–673的放弃/未确认保护保留。

## Private Trial与真实版本

- `app/build.gradle.kts:23–29`明确拒绝PrivateTrial与M22验证/隔离/终止实验属性混用；`:62–67`保留独立`.preview.debug`身份。C0仅允许主任务在`:50–51`更新显式PrivateTrial版本，普通Debug/Release条件分支不得变化。
- `:85`使用`src/privateTrial/AndroidManifest.xml`作为Debug overlay，该文件仅替换标签为“砺石·体验版”，不覆盖生产Application/Launcher。`:127`在PrivateTrial排除`ui.test.manifest`宿主依赖；M13IsolatedRunner本体位于`src/androidTest`，不属于App源码。**排除依据是源集/依赖配置，不是虚称overlay手写了Activity删除规则；最终合并Manifest与DEX仍须主任务核验。**
- `feature/settings/ProductInformationScreen.kt:34–46`在运行时调用`readInstalledAppInformation`，`InstalledAppInformationReader.kt:10–19`只读自身PackageManager的versionName/longVersionCode/package/DEBUGGABLE；`ProductInformation.kt:47–53`格式化真实值并明确体验版非Release。不是永久硬编码code18/19。

## 冻结边界与限制

只读`git diff fef1f8e --`核对app/data/domain/core/time、导航/AppContainer、Home、schema及Manifest均为空；Room5见`data/local/LithicDatabase.kt:22`，backup4/schema5见`data/backup/BackupManifest.kt:59,74–77`。本轮未发现由版本交付准备引入的确定性数据安全问题；不据此关闭历史故障或声明运行安全已证明。

真实封面继续 **MEDIA_DISPLAY_PENDING**。真实Android视觉、字体/IME、安装/升级、Owner运行收口及手机数据隔离均未在本次执行；正常生产Application会使用其独立包的正常存储/提醒Runtime，与隔离Runner不同，不能将源码路径核对称为已经启动或验证过用户手机。
