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
