# LITHIC 首页卡片 UI 实施报告

## 范围与基线

分支 `codex/lithic-home-card-ui`，起点 `7d5d6eaf523b40430c33c7634f7eccb796fcb326`。本轮以用户最后上传的双屏样图及明确书面要求为视觉与交互依据。[最终确认样图](lithic-home-card-ui/approved-reference.png)

- 首页改为品牌栏、总览卡、项目/订阅分段与双列 4:3 卡片。顶部「＋」和底部「添加」共用同一原生选择面板。选段驱动卡片、“全部”和对应预览失败重试。现有项目/订阅详情、搜索、列表和原返回来源保留。
- 底栏保留「现在／回看／设置」及原路由守卫，改为轻量的首页、柱状图与齿轮线框。正常字号保持双列，大字号允许名称、状态和金额换行；添加区始终占用布局空间。
- 总览显示来自同一快照的日均、截止日及含历史数量。原非今日扣款解释留在信息入口。成本构成复用项目/订阅小计、分类及跳转，改为可通过系统返回关闭的底部面板。
- 只有无图时显示类型图标，已关联封面读失败保留独立反馈。项目日均成本、订阅历史平均日成本分别沿用原值和无障碍读法。真实金额可为零、负值或未知。

## 需留意的样图差异

- **总投入为「—」。** 首页 `OverviewResult` 没有全账本总投入字段。项目卡片只读最多两条，年度报表也分别记录项目投入、收回及订阅付款。示例金额不写入产品，展示未取得的占位。若要显示真实总投入，后续须明确统计范围和来源。
- **订阅用真实状态。** 领域模型有订阅中、暂停、到期和停止；服务商自动续费状态未记录。
- 最终图中品牌副标题未画出，按本轮文字要求显示「记录拥有，理解成本。」。纯白主题用于样图对照，三主题及既有选择保持。封面只读当前记录的真实关联，无图使用类型图标。样图设备边框、状态栏及合成照片均属于参考图。

## 修改文件

- [HomeShell.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt) · 首页布局、视觉、状态与交互接线。
- [LithicNavigation.kt](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt) 与 `ic_primary_nav_{home,review,settings}.xml` · 顶层底栏视觉及可访问性角色。原 Nav3 的返回、未保存、未确认写入和恢复保护不变。
- `app/src/androidTest/` 和既有 `app/src/p2A1/androidTest/` · 对应首页分段、入口、金额、封面与图库采集。旧布局专属的断言按已确认设计调整，数据与保护断言保留。

数据库、迁移、备份协议、Repository、Owner、金额算法、照片存储、正式包名、签名、版本和 Gradle 配置均未改动。产品仅有原正式身份。已有 P2-A1 仅用于隔离自动化。

## 验证与截图

| 检查 | 本轮实测 |
| --- | --- |
| 生产 Kotlin、AndroidTest Kotlin、既有 P2-A1 测试 App/Test 构建 | PASS。离线 Gradle 执行成功 |
| 定向 JVM | 58 PASS，0 FAIL，0 SKIPPED。覆盖首页快照、搜索、金额格式、导航与转场 |
| 既有 P2-A1 测试包身份 | PASS。核对既有包名、Debug 证书、plain Application、无 Launcher、M13 Runner 和对应 targetPackage |
| 源码边界及 `git diff --check` | PASS |
| Lint | NOT_RUN |
| 项目/订阅端侧截图、分段和回调运行 | NOT_RUN。首次可用内存 3.34 GiB，未启动。复测 4.068 GiB 后只读启动原 `Lithic_P2A1_API36` 一次，150 秒内未取得启动完成。App/Test 安装与 Instrumentation 均为 0。已核对本次进程和端口退出，启动根因 UNKNOWN |

| 图像 | 证据 |
| --- | --- |
| 实施前 | [历史 P2-A1 纯白主题原生截图](lithic-ui-p2-a1/home-pureWhite.png)。可对比当时的纵向预览、底栏和单个新增按钮，历史使用不同合成夹具 |
| 实施目标 | [用户确认的项目与订阅样图](lithic-home-card-ui/approved-reference.png) |
| 实施后 | 两张纯白合成记录的原生截图待取得。对照图未以样图或浏览器预览代替 |

Android 真实系统大字、TalkBack、实际导航/返回以及视觉还原程度待运行核对。本轮桌面实现和测试通过，原生截图门禁仍待补。当前状态 `DESKTOP_PASS / ANDROID_SCREENSHOTS_BLOCKED`。

## 运行补验

补验源码仍为 `154ff0fe3a6e0e60f6cee193daf6062b644d7bfd`。P2-A1 两份既有测试包重新核对包名、证书和 SHA-256，与原构建字节一致。没有重新构建。

| 观察 | 证据 |
| --- | --- |
| 首次超时的补读 | 完整模拟器日志显示 guest 在 97,680 ms 自报启动完成，同时记录 ADB 端口 5589 超出推荐范围，`emulator-5588` 未被 ADB 识别。原 150 秒准入超时及零安装记录保留。 |
| 本次资源与环境 | 启动前可用内存 4.653 GiB，C 盘约 16.95 GiB、E 盘约 31.13 GiB。使用原 `Lithic_P2A1_API36` 的只读启动，不加载或保存快照。改用本机模拟器帮助所列范围内的空闲 5558/5559。 |
| 启动与连接 | guest 日志在 142,064 ms 自报启动完成。150 秒内，`emulator-5558` 仍未被 ADB 识别，未取得健康与身份准入。换端口后问题仍出现，传输未注册的根因 UNKNOWN。 |
| 运行与收口 | App/Test 安装 0，Instrumentation 0，原生截图 0。只关闭本次启动的隔离进程，已核对进程与端口均退出。未操作真实手机。 |

截图、视觉还原和端侧交互继续为 `NOT_RUN`。当前状态 `DESKTOP_PASS / ANDROID_SCREENSHOTS_BLOCKED`。

## 独立 ADB 补验（2026-11-12）

仍使用源码提交 154ff0fe3a6e0e60f6cee193daf6062b644d7bfd 的同一对 P2-A1 测试 APK。复核 Manifest、原 Debug 证书和 SHA-256，未重建。启动前可用内存 5.008 GiB，C/E 盘可用空间约 16.89/31.08 GiB。

| 检查 | 本轮实测 |
| --- | --- |
| 独立连接服务 | Windows 上首次使用显式监听地址的命令返回 3221226505，报告不支持指定主机名监听。修正命令及本地脚本缩进后，专用 ADB 在 127.0.0.1:15038 启动，排除 USB 与 mDNS 自动发现，启动前设备列表为空。默认 5037 服务保持同一 PID。 |
| 模拟器启动 | 原 Lithic_P2A1_API36 使用只读覆盖层冷启动一次，不加载或保存快照。guest 日志报告 Boot completed in 106370 ms。 |
| 连接准入 | 225 秒内对 emulator-5558 的 32 次定向观察始终未取得设备传输。从 92 秒开始检查 5559，始终未观察到监听，因此未尝试直接连接。未取得 guest 身份及健康检查，根因仍为 UNKNOWN。[本次连接观察](lithic-home-card-ui/runtime-dedicated-adb-transport.txt) |
| 退出核对 | 准入脚本退出码 1。自动收口未能结束模拟器，随后按本次进程的父子关系、AVD 名称、端口和只读参数核对并停止。模拟器与专用 ADB 均已退出，5558、5559、15038 无监听。 |
| Android 运行 | App/Test 安装 0、Instrumentation 0、运行截图 0。项目、订阅截图和交互验证仍为 NOT_RUN。没有操作手机、个人账本或正式 APK。 |

当前状态 DESKTOP_PASS / ANDROID_SCREENSHOTS_BLOCKED。本轮仅补录运行证据，生产及测试源码未变。
