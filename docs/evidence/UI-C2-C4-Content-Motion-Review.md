# UI-C2 C4 — 内容密度与主导航收口

状态：**UI-C2 C4 READY_FOR_USER_EXPERIENCE_REVIEW / ANDROID_VISUAL_AND_MOTION_PENDING**。日期：2026-10-27。

## 范围与实际差异

从`bb28427da3c6e290d0e670d16d769b0bc3b3f145`、`codex/lithic-v1-product-completion`实施。用户转交截图及7.7秒视频的文字审查；本轮未取得这些媒体原件，不声称已观看。只改现有生产Compose呈现、普通转场及显式体验版版本，不新增产品功能或截图工具。

| 页面 | 本轮生产修改 | 保留的事实和保护 |
|---|---|---|
| 回看 | 总金额、完整选中日期与172dp真实趋势紧邻；项目/订阅贡献短行；按钮改为“显示分项曲线／收起分项曲线” | 精确选日、完整日期、负值、真实0/无贡献区分；完整趋势页与全部分析/历史入口保留 |
| 首页 | 不再用名称首字作身份；使用既有类别或类型矢量；记录金额仅显示一次单位；无当前订阅压成分组短提示 | 空、加载、失败、旧快照与未知不混淆；预览/列表来源、金额精度及FAB/新增守卫不变 |
| 设置 | 常态说明收进信息入口；真实反馈保持原类型 | 导出旁未加密、恢复旁完整覆盖提示仍可见；残留/未知/错误持续显示，预览与确认不变 |
| 主导航 | Home/Review/Settings之间前进及普通返回使用无转场；涉及深层页面用180ms淡入淡出 | 原Nav3栈、来源、requestLeave、dirty/pending/discard、恢复保护及预测返回参数不变 |

普通说明的新位置：记录总数及“含历史”口径在回看信息入口；轴范围、当前/历史零值含义、观察日、重建与舍入口径在“指标说明”；完整趋势页保留详细读数和解释。图表辅助语义始终包含轴范围。已取得快照的计数说明明确提示有读取警示时并非最新结果。加载、读取失败、恢复/结果未知和旧结果警示没有收起。

源码包括`HomeShell.kt`、`ReviewShell.kt`、`TotalCostTrendScreen.kt`、`SettingsShell.kt`、`C2RecordIdentity.kt`、`LithicNavigation.kt`及最小`C4NavigationMotion.kt`。未改Theme、成本引擎、图表采样/投影、VM、DAO或Repository。

## 转场诊断与证据边界

实际锁定的Navigation3 1.0.1默认时长为700ms，原调用未显式覆盖；本轮使用其原生转场配置。已核对Scene从最后NavEntry取得metadata、默认SinglePaneSceneStrategy及单entry传播，不以Scene.key猜测路由。见[实际缓存API](ui-c2-c4-20261027-070740/navigation3-api.txt)、[Scene元数据](ui-c2-c4-20261027-070740/navigation3-scene-metadata.txt)及[默认策略节选](ui-c2-c4-20261027-070740/navigation3-default-strategy.txt)。

这支持对默认转场的定向调整，不能证明视频中的全部迟滞均由动画引起。没有帧时间或真实CPU分析，不能归因于数据库、趋势计算，也不登记手机响应或视觉PASS。未增加首页预跑、20年重算、持久缓存或第二计算引擎。

## 本轮实际验证

| 项目 | 结果 |
|---|---|
| 19个精确选择的JVM类 | **139 PASS，0 FAIL/ERROR/SKIP**，其中6个新增导航/图标断言 |
| Debug构建、AndroidTest源码编译 | exit0；新增9个Compose方法、调整2个说明入口方法，仅编译 |
| 普通Debug Lint | 0错误，21警告；原始XML保留 |
| Private Trial构建及Lint | exit0；0错误，13警告 |
| Manifest/签名/DEX/源树绑定 | 静态核对通过，不能代替Android运行 |
| Android视觉/切换/触控/大字/IME/安装升级 | **NOT_RUN**；新增360dp/2倍字体仅为LocalDensity断言源码 |

断言覆盖主入口与深页转场选择、原导航保护、类别矢量与不推断名称；Compose源码覆盖说明展开、分项曲线与选日、负值与真实0、历史无贡献、错误可见、360dp放大布局、首页空订阅和设置风险入口。没有修改冻结业务断言或运行M22历史套件。

原始：[JVM逐类结果](ui-c2-c4-20261027-070740/jvm-results.json)、[JUnit XML目录](ui-c2-c4-20261027-070740/jvm/)、[Debug命令/退出码](ui-c2-c4-20261027-070740/debug-quality/command.json)、[stdout](ui-c2-c4-20261027-070740/debug-quality/stdout)、[stderr](ui-c2-c4-20261027-070740/debug-quality/stderr)、[普通Lint](ui-c2-c4-20261027-070740/debug-lint.xml)、[体验构建](ui-c2-c4-20261027-070740/private-trial-build-lint/command.json)、[体验Lint](ui-c2-c4-20261027-070740/preview7-lint.xml)、[静态产物审计](ui-c2-c4-20261027-070740/artifact-audit.json)。

体验构建前内存门禁一度未满足，未启动该次构建；核对唯一同版本空闲服务是本轮自建后，用Gradle正常退出它及编译子进程，重新满足4GiB门禁后才继续。仅本轮桌面构建资源受影响；见[身份](ui-c2-c4-20261027-070740/own-build-daemon-identity.json)、[正常退出](ui-c2-c4-20261027-070740/own-build-daemon-normal-stop/command.json)、[资源复核](ui-c2-c4-20261027-070740/private-trial-resource-recheck.json)。

## 唯一Preview 7候选

- 路径：`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/deliveries/lithic-preview-0.1.0-preview.7/Lithic-0.1.0-preview.7-debug.apk`。
- 包名：`com.cooper.lithic.preview.debug`；`0.1.0-preview.7-debug` / code23；大小：**15,748,403字节**。
- APK SHA-256：`c6ec3b0e43fa90f05220754ef581242bea6981c7c41aac4226eadb7f37dae26e`。
- 原Debug证书SHA-256：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；不是正式Release签名。
- 正常`LithicApplication → MainActivity → LithicApp → LithicNavigation`使用本次Home/Review/Settings；见[生产接线](ui-c2-c4-20261027-070740/production-wiring.json)。无JUnit/Instrumentation/测试宿主，Launcher及私有Provider身份精确核对，无INTERNET或新增权限。
- 只递增显式Private Trial；普通Debug/Release与M22/C3身份配置保持。[源绑定](ui-c2-c4-20261027-070740/source-binding.json)记录最终APK输入app树`0520b3bfeb4f8669cf95cb16fc20aa769c828e84`，最终提交的app树须相同。[签名实物核验](ui-c2-c4-20261027-070740/preview7-signature/stdout)、[Manifest](ui-c2-c4-20261027-070740/preview7-merged-manifest.xml)。
- 构建产物复制到非build目录一次，交付副本大小/SHA与构建实物一致；旧Preview5/6及C3两包SHA保持。APK和密钥不进Git。

用户可自愿手动选择同包覆盖更新，不以卸载、清数据解决失败；如已有重要体验记录，先保留独立备份。本轮未安装、覆盖升级或进行手机验收。体验重点是三主入口切换、带记录/空态的回看与首页、说明展开及深页返回，这不是已执行的体验PASS。

## 保持与未完成项

Room5、backup4、金额/日期/订阅/交易、写入事务、Owner、generation、恢复Token/原子切换、媒体及提醒协议未改。真实封面仍**MEDIA_DISPLAY_PENDING**。M10三JPEG历史FAIL、M20平台FAIL/UNKNOWN、M22 BLOCKED及T06/T03/IME未完成、M13稳定签名/升级及G1～G4均保持。

C3 NATIVE CAPTURE INVESTIGATION CLOSED，根因和已装C3运行状态UNKNOWN；不再重跑A～F或处理现场。本轮ADB、手机/模拟器、个人账本、安装、强停、清理和新截图均0。没有HTML替代证据，不宣称ANDROID_VISUAL_PASS或VISUAL_ACCEPTED。提交推送后停止，不合并main、不进入M23。
