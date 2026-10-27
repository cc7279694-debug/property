# UI-C2 Preview 6 普通体验候选交付

日期：2026-10-27。基线 `61d85862a21e94311c3bb04bbf8180f075bd4cb5`；分支 `codex/lithic-v1-product-completion`。开始时本地/远端一致、工作区干净。

**PREVIEW 6 READY_FOR_OPTIONAL_USER_VISUAL_TRIAL / APP_VISUAL_PENDING**

## 唯一交付APK

- 路径：`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/deliveries/lithic-preview-0.1.0-preview.6/Lithic-0.1.0-preview.6-debug.apk`
- 包名：`com.cooper.lithic.preview.debug`；名称：砺石·体验版。
- 版本：**code22 / `0.1.0-preview.6-debug`**，高于Preview 5/code21。
- 大小：**15,732,019字节**。
- SHA-256：`154867aebd19372c78ae75879b9d6ce9fc2600fe2d5f7569b5cf7bbb684c13d9`。
- 原Debug证书SHA-256：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。现有本地签名证书在构建前核对，最终APK由apksigner核对；未生成或替换密钥，不是正式Release。

交付目录为全新非Gradle目录，仅一个候选APK，无ZIP，APK/密钥不入Git。复制后重算长度与SHA，与构建产物完全一致。原Preview 5交付文件及C3锁定App/Test的本地字节前后保持；没有访问手机上的任何安装包。

## 生产UI与修改边界

本轮产品源码只修改 `app/build.gradle.kts` 两行：显式Private Trial的21→22、preview.5→preview.6；不修改布局、算法、数据或测试源码。构建输入app树为 `017f8ef19620190f5b63d905bb102807e0144005`，与最终提交的app树核对；其他构建输入继承基线。见[源码绑定](ui-c2-preview6-20261027-054847/source-binding.json)。

正常 [MainActivity:55](../../app/src/main/java/com/cooper/lithic/app/MainActivity.kt#L55) → [LithicApp:42](../../app/src/main/java/com/cooper/lithic/app/LithicApp.kt#L42) → [LithicNavigation:409](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L409) 继续接入当前生产HomeShell/ReviewShell，使用真实Ready/generation及已有状态：

- [HomeShell:90](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L90)：数量卡接真实Overview，确认空库显示0，未知显示“—”；绿主卡和无金额占位保持。
- [HomeShell:134](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L134)：空首页也有56dp圆FAB，打开原两项选择面板；正文底部安全空间保持。
- [ReviewShell:78](../../app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt#L78)：仅确认空事实且无警告时进入轻量空态，保留单句引导和原返回首页操作；有数据继续真实趋势与既有分析。
- 新增、返回、底栏及来源继续走原requestLeave、dirty/pending/discard与恢复保护。项目、订阅、图表、搜索、设置、备份恢复均继承现有生产实现。

上述为源码接线及APK打包静态核对，**不是Android画面、触控、字体/IME或完整导航运行PASS**。没有HTML替代、合成照片或用户账本注入。Room5、backup4、Repository/Owner、恢复Token/原子指针、冻结业务与提醒协议未改。

[与Preview5的打包对照](ui-c2-preview6-20261027-054847/preview5-packaging-comparison.json)确认classes5～7.dex发生变化；旧`home-first-record`标记在新包中移除，新增`review-empty-identity`标记存在。它说明新包包含本轮所指的C2生产差异，不是仅改文件名，仍不替代真实Android视觉确认。

## 实际门禁

| 检查 | 本轮真实结果 |
|---|---|
| 定向JVM | **44 PASS，0失败/错误/跳过**，8个首页/回看/金额/导航相关类；[汇总及逐方法](ui-c2-preview6-20261027-054847/jvm-result.json) |
| Debug构建、AndroidTest源码编译、普通Lint | exit0，382.250秒；Lint **0错误21警告**；[命令/退出](ui-c2-preview6-20261027-054847/debug-quality/command.json)、[原输出](ui-c2-preview6-20261027-054847/debug-quality/stdout)、[质量汇总](ui-c2-preview6-20261027-054847/debug-quality-result.json)、[Lint XML](ui-c2-preview6-20261027-054847/debug-lint.xml) |
| Private Trial构建及Lint | exit0，399.828秒；Lint **0错误13警告**；[命令/退出](ui-c2-preview6-20261027-054847/private-trial-build-lint/command.json)、[原输出](ui-c2-preview6-20261027-054847/private-trial-build-lint/stdout)、[Lint XML](ui-c2-preview6-20261027-054847/preview6-lint.xml) |
| 最终APK身份/签名 | code22、preview包名、原证书PASS；[审计与最终副本SHA](ui-c2-preview6-20261027-054847/artifact-audit.json)、[签名原输出](ui-c2-preview6-20261027-054847/preview6-signature/stdout) |
| Manifest/DEX | 正常LithicApplication及唯一MainActivity Launcher；独立非导出Provider authority `com.cooper.lithic.preview.debug.androidx-startup`；无INTERNET/sharedUserId/Instrumentation/testOnly。用户APK无JUnit/AndroidX Test/Compose测试框架及本项目Test/Runner/合成夹具类。C2生产组件及标签存在只作打包佐证；[合并Manifest](ui-c2-preview6-20261027-054847/preview6-merged-manifest.xml)、[实际APK Manifest](ui-c2-preview6-20261027-054847/preview6-apk-manifest/stdout) |
| 默认身份及旧文件 | 普通Debug仍com.cooper.lithic.debug/code17，Release配置仍com.cooper.lithic/code17；M22/C3分支配置未改，锁定C3本地APK SHA保持；[构建前签名/文件核对](ui-c2-preview6-20261027-054847/preflight.json) |
| Android实际执行、安装、升级、视觉 | **NOT_RUN / APP_VISUAL_PENDING**；本轮ADB、手机/AVD操作、C3 A～F、M22 T06/T03/IME均0 |

本轮使用现有离线构建配置、单Gradle worker及既有审计/身份任务；完整桌面管线开始前可用物理内存通过4GiB门禁，E盘约65GiB，未下载SDK或依赖。没有新增业务测试或截图工具，不重跑M22历史验收。

## C3关闭与体验边界

**C3 NATIVE CAPTURE INVESTIGATION CLOSED**是用户停止调查的产品决策，超时根因仍UNKNOWN；不改判A或其他历史结果。没有修补USB读取器后重试，没有处理、停止、卸载或清理已安装C3 App/Test；其当前进程/Instrumentation状态仍UNKNOWN。原始报告、失败现场及历史证据保留。

**MEDIA_DISPLAY_PENDING**不变。M10三JPEG历史FAIL、M20提醒平台FAIL/UNKNOWN、M22原BLOCKED及未完成项、M13正式签名/升级门禁、G1～G4及全部历史NOTES/NOT_RUN均不因打包而关闭。

用户可自愿把上述APK传至手机，在保留原记录的情况下选择更新同包体验版。本轮没有验证真实覆盖升级；如已有重要体验记录，先保留独立备份，不把本版作为唯一副本。若手机提示签名冲突或更新失败，停止并保留旧版，**不要默认卸载或清数据**。普通体验可关注空首页数量卡/FAB、空回看、已有记录预览、详情返回及日常项目/订阅操作；这份建议不是已执行的流程PASS。

不声明ANDROID_VISUAL_PASS或正式V1.0，不自动安装、继续截图调查、合并main或进入M23。提交推送后停止，等待ChatGPT独立验收。
