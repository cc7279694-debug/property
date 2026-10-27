# UI-C2 C3-0 — 原生视觉验证准备

**C3-0 READY_FOR_SAFE_CAPTURE_REVIEW**

**ANDROID_SCREENSHOTS_NOT_RUN**

## 2026-10-27 桌面构建恢复与最终结果

用户表示内存已足够，本轮只读复核可用RAM **6.143GiB**、E盘 **65.437GiB**，恢复此前获准的桌面预检；设备授权未扩大。起点`bed9df9a195db7cb2e4c296b994a9d7ccc9e1067`，最终APK源码提交 **`2dbd9a121683b051cf05931a9858fcc05551bfcf`**。只修C3专属Manifest，生产Compose/Room5/backup4/领域/Repository/Owner/恢复/Nav3/媒体/提醒代码均未改；原签名密钥、旧交付和M22实物保持。

| 最终实物（不是用户Preview包） | 包名 | 字节 | SHA-256 |
| --- | --- | --- | --- |
| App | `com.cooper.lithic.c2visual.debug` | 15,732,081 | `14b1d55e30f8db3bf70d693034b4d4ab5989b64d8c7a1cf7d36b1e21cc9a79a6` |
| Test | `com.cooper.lithic.c2visual.debug.test` | 3,475,349 | `18a10d5472aa5bff624ed02da763a4efd873811a2ac2b738497693b3b0b51c76` |

本地绝对目录为`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/app/build-c2-visual/outputs/apk/`（App在debug，Test在androidTest/debug）；只保留这对最终生成物，不复制成Preview或重复交付包，APK不入Git。App版本 **code17 / 0.1.0-c2visual-debug**；minSdk26/target36。两份APK签名校验通过，证书SHA均为 **`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`**。完整身份见[实物记录](ui-c2-c3-build-20261027-014905/apk-identities-final.json)及同目录的最终signature/badging/APK-Manifest原始输出。

### 首轮问题及最小修正

- 首轮App/Test已编译打包，但聚合命令因Lint **3错误13警告**退出1，JVM任务尚未执行：[原命令](ui-c2-c3-build-20261027-014905/c3-build-first/command.json)、[stdout](ui-c2-c3-build-20261027-014905/c3-build-first/stdout)、[stderr](ui-c2-c3-build-20261027-014905/c3-build-first/stderr)、[原Lint XML](ui-c2-c3-build-20261027-014905/lint-first.xml)永久保留。
- 一项MissingClass来自专属Manifest的ProfileInstallReceiver移除标记；另两项NotificationPermission来自移除POST声明后仍编译的原Publisher。仅改C3 Manifest为`application tools:node=replace`启动白名单，保留原备份禁止/规则/主题/图标；保留共享生产代码已有的POST声明，**不改Publisher、不禁Lint、不加baseline**。
- 最终App只有plain Application＋空ComponentActivity；无MainActivity/Launcher、Provider、Service、Receiver、shared UID和INTERNET。**POST_NOTIFICATIONS仍声明**，另有本包signature权限；不是已授予或已发布，截图代码不请求/发布/调度。未来API36安装后须核实未授予且不自动授权。Android<33不能依赖运行时拒绝，隔离依据仍是无生产启动链和Gallery无发布调用。
- 本地aapt首次使用含中文的绝对路径出现`Illegal byte sequence`，保留[失败回执](ui-c2-c3-build-20261027-014905/app-badging/command.json)；对同一文件改用ASCII相对参数后成功，不是APK损坏或换包。DEX采集器首个字符串检查也已更正为Dalvik斜杠描述符，未修改Runner或其代码。

### 最终静态启动与质量门禁

- [最终合并/打包Manifest契约](ui-c2-c3-build-20261027-014905/manifest-contract-final.json)及[App XML](ui-c2-c3-build-20261027-014905/app-merged-manifest.xml)/[Test XML](ui-c2-c3-build-20261027-014905/test-merged-manifest.xml)与APK内Manifest一致。Test唯一Instrumentation为`com.cooper.lithic.app.M13IsolatedRunner`、target仅新App，默认platform Application，无Provider/Service/Receiver/shared UID。
- Test保留标准AndroidX `REORDER_TASKS`、`InstrumentationActivityInvoker$BootstrapActivity`/`$EmptyActivity`/`$EmptyFloatingActivity`三个辅助Activity（有LAUNCHER category但没有MAIN action），以及orchestrator/services/google testing.services三项queries；逐项见契约，不能称“Test零权限/零辅助组件”。这些不是生产业务入口，也未实际执行。
- [实际Test DEX的Runner反汇编](ui-c2-c3-build-20261027-014905/runner-dex-final/M13IsolatedRunner.txt)显示`const-class Landroid/app/Application;`并传给`super.newApplication`，未调用AppContainer、LithicApplication或ReminderRuntime。Gallery/合成事实仅在Test DEX，App无这两个类；A～F八方法均实际编译并在Test DEX找到。静态启动链可核对，**真实安装/进程/UID/启动/PNG仍NOT_RUN**。
- [配置矩阵](ui-c2-c3-build-20261027-014905/identity-matrix.json)：普通Debug/普通Release仍com.cooper.lithic.debug/com.cooper.lithic code17；PrivateTrial仍preview.debug/code21/preview.5；M22仍m22verify.debug/code17/M13Runner；只做配置求值，未重建旧包。C3实际仅Debug，五种非法/冲突参数均预期拒绝。旧本地APK逐字节SHA保持。
- 修正后 **14定向JVM PASS，0失败/跳过；Lint 0错误15警告**，见[质量结果](ui-c2-c3-build-20261027-014905/quality-result.json)、[Lint XML](ui-c2-c3-build-20261027-014905/lint-final.xml)、[修正后命令](ui-c2-c3-build-20261027-014905/c3-lint-fix-and-jvm/command.json)。最终[App/Test重打包与配置核验](ui-c2-c3-build-20261027-014905/c3-final-assemble/command.json)退出0；无生产UI/引擎或测试业务断言变动，没有重跑M22历史137/36套件。

未来设备/提取命令见[手册](UI-C2-C3-Capture-Runbook.md)，须先独立核对本次真实SHA，再获用户新的设备授权；只允许新包和本次UUID证据。Test仍含其他历史测试类，不能整包/整类扩跑；只选手册固定八方法。**本轮ADB、设备、安装、截图、提取均0**，没有Preview6；APP_VISUAL_PENDING / MEDIA_DISPLAY_PENDING及M10/M20/M22/M13/G1～G4全部历史状态不变。此结论只是桌面截图准备，不能声明真实Android视觉通过或进入M23。

---

## 2026-10-26 源码准备与构建暂缓记录（以下原文保留，不代表最新结果）

**SOURCE_PREPARED — BUILD_DEFERRED / ANDROID_SCREENSHOTS_NOT_RUN**

来源 `f037de8a1e138647f2089ddf405017bd02d28f11`，分支 `codex/lithic-v1-product-completion`，起点与远端一致、工作区原本干净。用户转交 C2 独立结论 **IMPLEMENTATION PASS WITH NOTES / APP_VISUAL_PENDING**；未改页面风格、未生成 HTML 或 Preview 6。

## 本轮范围及中途授权调整

Goal：只为生产 Compose 的 A～F 原生截图建立独立、启动前隔离的构建入口。Scope：Gradle Opt-in、专属 Manifest、既有 Gallery 和未来精确提取命令；业务、数据、主题、媒体、通知协议及所有历史身份冻结。

首次只读观察可用 RAM 约 **0.7GiB**，后续约 **1.497GiB**、E盘约65.422GiB。没有启动构建。用户随后明确答复 **“先完成源码，构建暂缓”**，本轮据此停止在源码准备，Gradle/JVM/SDK构建与签名工具均未执行，不因之后资源变化自行恢复。

## 独立身份与启动前隔离（源码，非产物证明）

| 项目 | 预定配置 | 本轮产物验证 |
| --- | --- | --- |
| Opt-in | `-Pc2VisualVerification=true`，缺省关闭 | 源码核对；AGP求值NOT_RUN |
| App | `com.cooper.lithic.c2visual.debug`，code17 / `0.1.0-c2visual-debug` | NOT_BUILT，APK SHA未知 |
| Test | `com.cooper.lithic.c2visual.debug.test` | NOT_BUILT，APK SHA未知 |
| Runner | `com.cooper.lithic.app.M13IsolatedRunner`，Opt-in自动选择 | 最终Instrumentation Manifest/DEX未验证 |
| Application | 专属 Manifest 明确为 `android.app.Application` | 源XML确认；最终合并及实际启动NOT_RUN |
| 签名 | 使用既有Debug配置；缺失密钥直接拒绝，不生成替代 | 新产物证书NOT_VERIFIED |
| 输出 | `app/build-c2-visual/`，独立于普通build和其他候选目录 | 本轮未创建产物 |

预期仍用原 Debug 公钥指纹 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，**不是对未生成APK作出的签名声明**。flag与PrivateTrial/M22/process-death互斥、拒绝`m13Isolated=false`、关闭本构建的Release变体；不改普通Debug/Release/PrivateTrial/M22原ID、版本表达式或签名路径。未来需AGP实际求值及产物复核。

[专属Manifest](../../app/src/c2Visual/AndroidManifest.xml)在测试宿主启动前覆盖Application，移除生产MainActivity/Launcher、整个Startup Provider、WorkManager服务/广播、Room多实例服务和ProfileInstaller Receiver，并仅在该构建移除通知/唤醒锁/网络状态/开机权限。保留AndroidX空ComponentActivity宿主；不是把正常App改为测试入口。原[Runner](../../app/src/androidTest/java/com/cooper/lithic/app/M13IsolatedRunner.kt)的`newApplication`仍强制platform Application，不构造LithicApplication；Gallery的`@Before`仅作附加检查。

**能确认的是源配置意图和调用路径：无生产入口、无自动初始化/提醒入口；尚不能确认实际APK的最终组件或启动行为。**未来必须核验合并Manifest只有plain Application及空宿主、无Provider/Service/Receiver/旧authority/shared UID，再核验Test的Runner/target和实际签名/SHA。生产类保留在DEX本身不等于被启动；也不能凭源XML就宣称最终打包隔离PASS。

## A～F截图准备

[Gallery](../../app/src/androidTest/java/com/cooper/lithic/ui/C2ProductionVisualGalleryTest.kt)复用未改的[合成事实夹具](../../app/src/androidTest/java/com/cooper/lithic/ui/C2VisualFixtures.kt)、生产HomeShell/ReviewShell/共享底栏。

- A/B：深色、受控400dp、空事实的现在/回看。
- C/D：浅色、受控400dp、合成项目和订阅的现在/回看；C另留一帧滚动记录。
- E1/E2：深色、受控360dp及LocalDensity两倍字体的现在/回看。
- F1/F2：空事实，**不提供LocalDensity、不强制宽高或字号**，使用宿主实际配置及系统明暗；记录实际画布尺寸、fontScale、densityDpi和screenWidthDp。

强校验新App/Test包名、M13Runner、plain Application及目标进程UID。只对两包自身的ApplicationInfo.sourceDir作有界流式APK SHA核对（拒绝splits，64MiB上限），不会查询旧包或读取其私有数据。PNG记录自己的字节/SHA，元数据区分受控与设备配置、隔离ComponentActivity与MainActivity；不把A～E当真实系统字体/IME验证。

截图只独占写入新包`files/ui-c2-c3-gallery-<新UUID>`，包含归属标记、PNG和capture.json；不删已有文件，不创建Room/Owner/default generation，不打开真实账本。通过Instrumentation状态回报确切目录与文件名，未来只读取该UUID的三个文件；禁止全包/整机拉取、无界目录扫描或提权。准确构建、单方法执行与**有界字节提取示例**见[后续操作手册](UI-C2-C3-Capture-Runbook.md)，本轮没有执行其中的任何设备命令。

## 实际检查与未验证门禁

[源契约审计](ui-c2-c3-source-20261026/source-audit.json)执行27项源码/配置文本与XML结构检查，且对未来提取Python示例只做AST语法解析；这是桌面**SOURCE-ONLY**检查，不是Gradle、JVM、Android或提取运行PASS。旧永久交付和M22本地产物SHA已记录，未新建/改写任何APK。生产`app/src/main`、合成事实源、原Runner、schema、依赖及工具链均零diff，页面及Room5/backup4/Owner/恢复/Nav3保护保持。

本轮 **Gradle/JVM/Lint/AndroidTest编译/Debug与独立App-Test构建均NOT_RUN**；全部八个截图方法的新源码尚未编译，未取得最终Manifest、DEX、App/Test证书和SHA；**安装/真实截图/执行/提取全部NOT_RUN**。C2原50 JVM和原8个Android方法仅编译的历史记录保留，不能移作本轮新结果。

未来最小设备权限须另行批准：核实唯一目标、仅全新两个包安装各一次、设备安装字节与UID核对、仅八个指定截图方法、仅新包本次UUID三文件的run-as只读提取；不启动正常MainActivity、不改系统配置、不访问旧包数据、不清理/覆盖/卸载、不运行T06/T03/历史测试。若新包已存在、未知数据/组件/任务、run-as限制或身份不符，停止而非绕过。

当前不能输出 **C3-0 READY_FOR_SAFE_CAPTURE_REVIEW**；还须先恢复桌面构建，完成AGP/编译/Lint/Manifest/签名/SHA门禁，再交ChatGPT独立核对和申请后续设备授权。**ANDROID_SCREENSHOTS_NOT_RUN / APP_VISUAL_PENDING / MEDIA_DISPLAY_PENDING**；M10/M20/M22/M13、G1～G4及全部历史FAIL/UNKNOWN/NOT_RUN/NOTES不变。提交推送后停止，不发新Preview、不合并main、不进入M23。
