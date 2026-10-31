# 砺石 — 用户选定图标更新

2026-10-31；`codex/lithic-v1-product-completion`；基线 `a973d9810f093aa1b8b73ea2a01960ab1169ab76`。用户明确要求改用新上传的无字黑石／金色侧光图，保留尺寸比例与清晰度。本轮仅替换启动图标及适配留白，不重绘品牌或修改产品页面。

## 实际修改

- `app/src/main/res/drawable-nodpi/ic_launcher_art.png`：直接使用用户原始 PNG，**1254×1254，2,068,618 字节**，SHA-256 `17e9fe02238d44ae5eb9d099b5dc6d81315fb73caa6d402f20ed7ed06c268d5f`。与上传实物逐字节一致，未放大、裁切、锐化、重新压缩或增加文字；旧资源为同尺寸银色 R 图，保留在 Git 历史和原交付 APK 中。
- `app/src/main/res/drawable/ic_launcher_foreground.xml`：为原图增加每边 **16.666667%** 相对 inset。原先直接 fill 整个 adaptive 前景，静态视口约 108/72 的关系会造成被动放大；现在前景保留约 72/108 的完整构图，标准图标显示尺寸不变，开启原有双线性过滤。
- Manifest、两个 adaptive-icon 引用、黑色背景和通知栏小图标均保持。通知继续使用原矢量 `ic_launcher_placeholder`，不把照片当通知单色图标。

## 清晰度与验证边界

- 已实际查看[32/48/64/96/128px 圆角与圆形预览](../design/launcher-icon-20261031/small-size-preview.png)，浅／深背景下主体和侧光完整，没有拉伸或撑满；64px 下形体及亮面清楚，细纹按自然下采样减弱。另有[64px 原尺寸预览](../design/launcher-icon-20261031/icon-at-64px.png)。这是 **DESKTOP SIMULATION ONLY**，不是 Android 截图，也不是主观视觉独立验收。
- [预览脚本](../design/launcher-icon-20261031/render-preview.ps1)只在电脑生成遮罩示意，不修改生产原图。未运行 HTML、C3、手机、ADB、模拟器或安装操作。
- 离线执行 `:app:processDebugResources :app:lintDebug`，**exit0 / BUILD SUCCESSFUL**；资源编译与链接通过，Lint **0错误16警告**。命令、资源边界、报告指纹及构建后13个APK保护复核见[结果](launcher-icon-20261031/resource-check.json)。没有执行 assemble/安装任务，也未下载或升级依赖。
- 保留 `IconLauncherShape`（原图有不透明黑底）及两条 `MonochromeLauncherIcon`（未另画单色图）警告；未为消除警告重绘用户原图。SDK XML版本提示及其他编译/静态警告未扩为无关修复。
- JVM、AndroidTest、真实 Launcher/OEM 遮罩、桌面缓存、安装升级与真实 Android 视觉均 **NOT_RUN**。纯图标资源变更不重跑业务套件，不以静态预览声称手机已更新。

## 原包及数据保护

[本地保护记录](launcher-icon-20261031/asset-and-protection.json)核对 **11 个历史用户交付 APK + 2 个 V1-A1 稳定归档 APK** 与已审大小/SHA 一致。没有生成新的用户体验 APK、没有递增 Preview12/code28、没有修改包名、证书或任何归档包；**新图标仅进入当前源码，不会追溯进入旧实物或已安装 App**。

业务源码、Gradle/Manifest、Room5、backup4、金额/订阅、Owner/generation、恢复、媒体与通知协议无改动。原[图标交付报告](LITHIC-Launcher-Icon.md)、旧预览和全部失败/UNKNOWN/NOT_RUN 原样保留；V1-A1 设备门禁及安装授权不因本轮图标变更改变。

**V1_RELEASE_NO_GO / ANDROID_RUNTIME_ACCEPTANCE_NOT_RUN / ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED / V1_A1_INSTALLATION_NOT_AUTHORIZED**
