# UI-C2 C0 新版个人体验包

**UI-C2 C0 READY_FOR_USER_TRIAL**

**APP_VISUAL_PENDING / MEDIA_DISPLAY_PENDING**。来源`fef1f8efcf9afe170d36f686ee02a7c3d88c2f5f`，产品完善分支。用户转交B1～B4及B-HOME桌面审查PASS WITH NOTES，不把原型认可扩大为手机视觉或完整流程PASS。

## 范围与实际接线

仅将`app/build.gradle.kts`显式Private Trial升code19/`0.1.0-preview.3-debug`，普通Debug/Release/M22验证分支不变。没有修改生产UI、数据层、Room5、backup4、冻结计算、Owner、恢复Token/原子指针、导航/提醒或媒体协议；原UI-C2与B-HOME全部继承，没有回退P04旧UI。

正常Manifest→LithicApplication/AppContainer→MainActivity→Ready/generation→NavDisplay，连接首页数量卡/圆形＋、项目与订阅列表/详情、创建/记一笔/续费/纠错、回看/趋势/搜索、设置/导出/验证预览及确认恢复。About从自身PackageManager读取真实安装版本。不是HTML包装，也不使用M13IsolatedRunner作为用户入口。源码与最终DEX核对分别保存；它们不证明实际页面已在手机显示。

## 实际质量与唯一交付

- **唯一用户APK**：[Lithic-0.1.0-preview.3-debug.apk](<E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/deliveries/lithic-preview-0.1.0-preview.3/Lithic-0.1.0-preview.3-debug.apk>)，不在Gradle build目录内。绝对路径：`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/deliveries/lithic-preview-0.1.0-preview.3/Lithic-0.1.0-preview.3-debug.apk`。
- 包名`com.cooper.lithic.preview.debug`；名称“砺石·体验版”；**code19 / `0.1.0-preview.3-debug`**；minAPI26、target36；文件 **14,079,087字节**。
- 实际SHA-256：`0d204eae40f2c5304c99b7df4c7aa5add4d786bee648e4befdef7a9c0f652dca`。
- 原单一Debug证书SHA-256：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；apksigner verify exit0，不是正式Release签名，未生成替代密钥。[产物/Manifest/DEX/签名结论](ui-c2-c0-20261026-094643/artifact-audit.json)／[实际签名命令](ui-c2-c0-20261026-094643/trial-signature/command.json)。
- **106个唯一JVM PASS，失败/错误/跳过0，无新增方法**；仅UI/导航/金额呈现/导出恢复Controller相关。[逐方法](ui-c2-c0-20261026-094643/jvm-summary.json)。普通Debug构建、AndroidTest源码编译、Lint exit0，**0错误17警告**：[命令](ui-c2-c0-20261026-094643/debug-quality/command.json)／[Lint XML](ui-c2-c0-20261026-094643/debug-lint.xml)。未运行任何Android方法，编译和既有UP-TO-DATE产物不当设备PASS。
- 显式`--offline -PlithicPrivateTrial=true :app:assembleDebug :app:lintDebug`（锁定JDK17/SDK36、单worker、只读身份任务）exit0；体验版Lint **0错误11警告**：[实际完整命令](ui-c2-c0-20261026-094643/trial-build-lint/command.json)／[stdout](ui-c2-c0-20261026-094643/trial-build-lint/stdout)／[Lint XML](ui-c2-c0-20261026-094643/trial-lint.xml)。不是重新执行M22设备验收。
- APK实际Launcher唯一MainActivity、正常LithicApplication、私有Provider `com.cooper.lithic.preview.debug.androidx-startup`，无INTERNET/sharedUID/Instrumentation/testOnly。权限仅继承POST_NOTIFICATIONS、WAKE_LOCK、ACCESS_NETWORK_STATE、RECEIVE_BOOT_COMPLETED及自身动态接收权限；后者网络状态权限不等于联网权限。[合并Manifest](ui-c2-c0-20261026-094643/trial-merged-manifest.xml)／[APK实际Manifest](ui-c2-c0-20261026-094643/trial-apk-manifest/stdout)。
- ZIP CRC通过；13个DEX/19,008个定义类包含36个要求的生产组件，HomeRecordCounts/RecordCount、数量卡/FAB/操作带标记与风险文案实际存在；JUnit、androidx.test、Compose测试框架、自有Test/Runner/测试宿主和HTML/合成图片/账本资源未进入用户APK。[源码接线复核](ui-c2-c0-20261026-094643/production-source-review.md)与DEX证据分开，均不冒称Android视觉已通过。
- [交付回执](ui-c2-c0-20261026-094643/delivery-receipt.json)：全新独占目录只有1个最终APK，复制后与构建输出逐字节/SHA一致；APK/密钥不入Git，不生成ZIP。原Preview2交付字节及M22本地App/Test未变，默认Debug/Release/M22前后模型完全一致。[冻结边界](ui-c2-c0-20261026-094643/scope-audit.json)／[前置身份](ui-c2-c0-20261026-094643/before-identities.json)。
- [源码绑定](ui-c2-c0-20261026-094643/source-binding.json)：来源fef1f8e＋仅PrivateTrial版本两行，实际app Git tree为`a3d8d3cd12610d1737b8ab3b1e516602df767c75`；最终提交app树必须与之相同，不把原起点字节冒充新版源码。
- 取证后处理曾沿用旧AAPT的`sdkVersion`字段，而本轮AAPT2实际输出`minSdkVersion`，导致一次校验断言失败；原签名/版本/标签均匹配。仅修正解析字段，复核既有exit0原始输出的长度/SHA及合并uses-sdk，未重建或修改APK：[说明](ui-c2-c0-20261026-094643/postprocess-note.json)。

## 用户手动安装与集中体验

- 将上方唯一APK传到手机，使用系统安装器自行选择安装。名称应为“砺石·体验版”，包名`com.cooper.lithic.preview.debug`，版本`0.1.0-preview.3-debug` / code19；不是M10 Debug或M22验证App。
- 如果已有Preview 2记录，**同包同证书覆盖升级尚未经过本轮真机验证**。重要记录先在旧版手动完整导出并另处保留；可暂缓安装。安装器出现签名/冲突/降级等提示时停止，不默认卸载旧版、不清数据、不尝试迁移M10/M22账本。Codex本轮未替用户安装。
- 仅供个人试用，优先使用可丢弃记录，不作为重要数据唯一副本。Debug证书连续不等于正式Release或长期签名门禁通过。

| 主入口 | 集中体验重点（均未在本轮手机执行） |
|---|---|
| 现在 | 数量小卡口径含历史；信息入口与成本展开；圆形＋/空态唯一入口。新建合成项目→投入/收回→查看日均/结束；新建订阅并确认合成付款、续费及状态。退出未保存表单要有原保护。 |
| 回看 | 搜索入口在现在页；搜索/历史/类别进入详情后返回原来源。查看项目/订阅与总趋势、精确选日、类别、年度、长期复盘，区分历史平均/当前日均/折算时长；0和负值不能消失。 |
| 设置 | 类别/提醒/计算说明/About真实版本；仅在完全可丢弃账本上熟悉手动完整导出、恢复校验/预览/明确确认。恢复为完整覆盖，备份未加密；重要账本跳过覆盖练习。 |

错误、未确认写入或恢复结果未知时先保留页面和提示，不重复提交、不清数据。停止App里的订阅记录不会替用户取消服务商扣款，提醒不保证厂商后台准点送达。

## 尚未完成与风险

- **APP_VISUAL_PENDING**：安装、Preview 2→3覆盖升级/数据保留、生产MainActivity实际视觉、触控/系统字体/IME、完整用户旅程均NOT_RUN；本轮Android执行0。HTML原型与源码/DEX均不替代真机证据。
- **MEDIA_DISPLAY_PENDING**：真实封面读取/管理闭环未完成，采用身份图标或“封面暂不可用”，没有合成品牌照片。M10三JPEG历史FAIL及安全缺口不改；不宣称媒体安全已解决。
- M20平台FAIL/收口UNKNOWN、M22 BLOCKED及T06/T03/真实IME未完成、M13-T06稳定签名/真实升级与G1～G4等历史门禁、全部NOTES/FAIL/UNKNOWN/NOT_RUN保持；本次仅个人体验交付，不是正式V1.0、M22冻结或M23通过。

本轮零ADB/设备/安装/卸载/强停/清理/私人数据访问。用户自行安排集中体验，后续按具体问题与新授权处理；不合并main或自动进入M23。
