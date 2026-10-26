# 砺石 · 用户选定启动图标与 Preview 4

2026-10-26。用户明确选用上传的黑色石体／银色标识图；本轮仅做原图生产适配与新版个人体验包交付。来源 `342c3c355be886daf6a1c5e4931c35f22c13a6cb`，分支 `codex/lithic-v1-product-completion`；提交内 `app` 树应为 `a29f72496e85ecea8a0104ee5a92b4fa20e100b4`。

## 实现

- 用户PNG实物1254×1254、RGB、1,634,521字节；SHA-256 `2652e8ddaf56540ba29bb6c1f5b3045a9e6914bc99efdd1f3d0f319d39674659`。原图直接放入 `drawable-nodpi/ic_launcher_art.png`，未重绘、生成替代图、重采样或去背景。最终APK内PNG也与用户原图逐字节一致，见[打包核对](lithic-icon-20261026-115713/icon-apk-audit.json)。
- Manifest启动图标／圆形图标改为两份API26+ adaptive资源；黑色背景，原图填充前景，由启动器裁切外围。minSdk26，没有新增旧平台兼容依赖或单色重绘。通知栏小图标继续使用原placeholder，未修改冻结通知代码。
- 仅显式Private Trial升为code20／`0.1.0-preview.4-debug`；普通Debug/Release/M22包名与版本、原证书保持。Preview2、Preview3和M22本地App/Test实物SHA均未变。没有Kotlin、测试、Room5、backup4、金额/日期、Owner、导航、恢复或提醒协议改动。

## 实际核验

| 项目 | 结果 |
|---|---|
| 普通Debug构建 | PASS，exit0；[命令与退出](lithic-icon-20261026-115713/debug-assemble/command.json)／[原始输出](lithic-icon-20261026-115713/debug-assemble/stdout) |
| Private Trial构建及Lint | PASS，exit0；0错误13警告；[命令与退出](lithic-icon-20261026-115713/trial-build-lint/command.json)／[原始输出](lithic-icon-20261026-115713/trial-build-lint/stdout)／[Lint XML](lithic-icon-20261026-115713/trial-lint.xml) |
| Manifest、原签名、权限、DEX | PASS；正常LithicApplication／唯一MainActivity Launcher、隔离Provider、无INTERNET／Instrumentation／测试宿主或框架；[完整静态核对](lithic-icon-20261026-115713/artifact-audit.json)／[签名原始输出](lithic-icon-20261026-115713/trial-signature/stdout) |
| 原图与APK图标、旧产物保留 | PASS；[图标核对](lithic-icon-20261026-115713/icon-apk-audit.json)／[交付字节核对](lithic-icon-20261026-115713/delivery.json) |
| 圆形／圆角128、64、48px | 桌面静态模拟已实际查看，[预览](../design/launcher-icon/preview.png)／[HTML](../design/launcher-icon/preview.html)／[尺寸与SHA](lithic-icon-20261026-115713/desktop-preview.json)；不是Android截图 |
| JVM、AndroidTest、设备、安装／升级／真实Launcher | 本轮NOT_RUN；纯资源替换未改逻辑，不重跑历史套件，无ADB或手机／模拟器操作 |

保留`IconLauncherShape`警告：输入为带黑底的不透明方图，实际采用自适应遮罩；未为消除警告更改用户原图。OEM遮罩、主题图标和升级后桌面缓存表现没有真实运行证据。首个桌面审计辅助读取受本地Gradle非UTF-8日志影响，改用既有C0身份行解析后通过，原始日志未改，未因此重跑构建，见[说明](lithic-icon-20261026-115713/default-profile-audit.json)。

## 唯一本轮交付

- 本地文件：`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/deliveries/lithic-preview-0.1.0-preview.4/Lithic-0.1.0-preview.4-debug.apk`（非build目录，APK不入Git）。
- 包名：`com.cooper.lithic.preview.debug`；版本：`0.1.0-preview.4-debug`／code20。
- 大小：15,715,635字节；SHA-256：`7971ace1f70c9829a6991ebee482104843061f45987e6223bc4abae99326579a`。
- 原Debug证书SHA-256：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。不是正式Release签名，未创建密钥。

用户可自行选择同包覆盖安装；本轮未安装、卸载或迁移任何手机应用。若覆盖更新报错，保留原App和数据，不以卸载为默认处理。Preview2／Preview3原交付均保留。

本轮图标接入完成，不授予Android整体视觉通过；APP_VISUAL_PENDING／MEDIA_DISPLAY_PENDING、M10三JPEG历史FAIL、M20平台FAIL／UNKNOWN、M22 BLOCKED、M13与G1～G4历史门禁全部保留。继续仅供个人体验，建议可丢弃数据，不作为重要记录唯一副本；未合并main或进入M23。
