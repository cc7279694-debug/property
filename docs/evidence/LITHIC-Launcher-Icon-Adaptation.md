# 启动图标 — 去边框与小尺寸适配

2026-10-31；`codex/photo-lite-p2`；基线`e67fc6bb65f1f544c42623717f305c70d5466717`。用户补充要求：沿用已选黑石／金色侧光LOGO，去黑边、保持视觉大小、提高小尺寸可辨识度。仅修改启动资源及桌面验证资料；P2照片源码、构建身份和版本配置保持。

## 根因与修改

- 旧`ic_launcher_art.png`自身已包含不透明黑底、圆角底板和外框；再叠加adaptive黑底及留白，形成框中框。原图保留不改：1254×1254，2,068,618字节，SHA-256 `17e9fe02238d44ae5eb9d099b5dc6d81315fb73caa6d402f20ed7ed06c268d5f`。旧报告和旧预览均保留历史身份。
- 基于该原图，使用图像编辑生成透明石头前景，移除底板、边框和地面反光。新`drawable-nodpi/ic_launcher_stone.png`为1254×1254真实RGBA PNG，1,585,043字节，SHA-256 `fe948b4ba0dde04842a4107d68693dd39c389abbb7d8da419ff9f156e4c236a3`。这是原图的**编辑衍生资源**，不是逐像素无损抠图，不宣称纹理与用户原图字节相同；黑石及金色侧光造型沿用，未增加字母或品牌素材。
- `ic_launcher_foreground.xml`改为透明前景及23% inset，以补偿前景去底后主体占画布比例的变化；系统标准图标尺寸不变，避免主体被动放大。保持滤波，不使用低分辨率图回放大。
- 新`ic_launcher_background.xml`为全铺满`#282B28`炭灰表面，无圆角、描边或内框；两个adaptive图标统一引用，由Launcher决定外轮廓。Manifest和通知单色图标不改。

## 实际检查与边界

- 已实际打开[前后对照](../design/launcher-icon-adaptation-20261031/before-after-192px-desktop-simulation.png)、[32/48/64/96/128px对照](../design/launcher-icon-adaptation-20261031/small-size-layer-comparison.png)及原参考。新候选没有旧底板边框；64px下石头形体与侧光清楚，细纹按小尺寸自然减弱。没有额外放大石头以填满图标。
- [桌面脚本](../design/launcher-icon-adaptation-20261031/render-preview.ps1)读取真实XML参数，按背景→透明前景→圆形／圆角遮罩分层绘制，包含浅／深桌面。108/72仅是静态近似，不能证明OEM遮罩、动态视差、启动画面、缓存和升级后的真实显示。全部图片明确标注 **DESKTOP SIMULATION ONLY / Android NOT_RUN**；[几何检查](../design/launcher-icon-adaptation-20261031/geometry.json)区分低Alpha残余与石头主体，不用透明包围盒冒充实际轮廓。
- XML解析、资源引用、PNG透明度／哈希、冻结文件差异及14个旧APK保护检查见[静态回执](launcher-icon-adaptation-20261031/static-check.json)。纯资源修改不增加业务测试，也不重跑照片／历史套件。
- 用户此前明确“先完成源码，构建暂缓”，继续有效。本轮**Gradle、资源编译、JVM、AndroidTest编译、Lint、APK构建、Android视觉、安装升级全部NOT_RUN**。没有新APK，也未自动改变已安装App。旧图标的历史编译通过不作为本次通过结果。

## 保护和停止点

14份既有稳定APK（11个Preview＋V1-A1两包＋PHOTO-LITE P1）大小／SHA均保持，未覆盖或删除。Room5、backup4、P2照片、金额／订阅、Owner/generation、恢复、C6读取、通知、Manifest及Gradle没有本轮差异。没有设备、ADB、模拟器、私人图片或账本操作；没有新包名／证书／版本递增。

**ICON_SOURCE_PREPARED / BUILD_DEFERRED / ANDROID_LAUNCHER_VISUAL_PENDING**。新图标只进入P2源码，未来恢复构建后仍须资源编译、Lint和最终APK核验。Preview13尚未构建，严格JPEG／隔离运行／正式V1门禁保持；不自行开始P3、设备执行、main合并或发行。
