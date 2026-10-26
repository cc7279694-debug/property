# UI-C2 C3-0 — 原生视觉验证准备

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
