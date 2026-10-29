# C9-R1 Preview 12 私人体验版交付

2026-10-29；分支 `codex/lithic-v1-product-completion`；基线 `3aa4f30f038843f91064529b147ccfc2e9bdc39e`。用户转交ChatGPT独立接受C9-R1 **PASS WITH NOTES，仅桌面实施**。本轮不改R1生产/测试/视觉源码，只递增显式Private Trial版本27→28、preview.11→preview.12。

桌面及APK实物静态门禁已通过，真实手机运行与升级仍未执行。

## 唯一APK与升级连续性

- 绝对路径：`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/deliveries/lithic-preview-0.1.0-preview.12/Lithic-0.1.0-preview.12-debug.apk`。
- 包名：`com.cooper.lithic.preview.debug`；名称：砺石·体验版；**code28 / `0.1.0-preview.12-debug`**，minSdk26、targetSdk36。
- 大小：**15,977,934字节**；SHA-256：`4b6b3ed03674fc971b67a124171fedbb89fce155d2c6fd181ed597cde6250a1b`。
- 原Debug证书SHA-256：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；apksigner验证通过，未生成替代密钥，不是正式Release签名。
- 新交付与构建产物逐字节一致；旧Preview11/code27仍15,961,550字节，SHA `5bbf3c1f946fc2da4ab259ab377f8134e222255784fe81e20c7a73337e63cf90`。其包名、版本、原证书在构建前实际核验，构建/复制后再次核对字节未变。只复制一个最终用户候选，不覆盖旧包。

## 实际验证

| 检查 | 实际结果 |
|---|---|
| 定向JVM | **69唯一方法PASS，0失败/错误/跳过**：C9-R1新角色3、C9色板5、外观Controller14、实际偏好文件3、启动角色8、C7来源10、C5-R1呈现18、金额呈现8。 |
| 体验构建/AndroidTest编译 | 同一Private Trial组合命令exit0，assembleDebug及AndroidTest源码编译完成；无Instrumentation/手机测试，未生成用户Test安装包。 |
| Lint | **0错误14警告**，本轮XML实际生成；SDK XML版本警告保留，未下载/升级工具。构建前RAM6.369GiB，离线/单worker，未关闭其他进程。 |
| Manifest/签名 | 正常LithicApplication、唯一MainActivity Launcher、本包Startup authority；无Instrumentation/testOnly/sharedUserId；5项权限与Preview11一致，无INTERNET，原证书一致。 |
| R1实际进入APK | 实际解析24个生产类定义、LithicVisuals的3个新增声明字段及7个独有页面tag，旧Preview11不含这些R1字段/tag；正常Main→Theme/App→Home/两详情/回看/图表/表单接线保持。没有仅用旧类名或版本号冒充R1。 |
| 测试与native隔离 | 产品DEX无JUnit/AndroidXTest/ComposeTest/M13Runner/R1截图或断言夹具类；8个既有native条目字节与Preview11相同，不接JPEG库/图片写入/Picker。 |
| 冻结/身份边界 | 对基线仅app Gradle的Private Trial两行版本变化。生产/测试/资源/schema/依赖、Owner/generation、恢复/导航/媒体源码不改；普通Debug/Release默认code17及M22/C3身份保持，本轮不重复普通/Release构建。 |

[实物审计](c9-r1-preview12-20261029-163944/artifact/identity.json)、[SDK核验命令](c9-r1-preview12-20261029-163944/artifact/commands.json)、[Lint XML](c9-r1-preview12-20261029-163944/build-check/lint.xml)、[源树绑定](c9-r1-preview12-20261029-163944/source-binding.json)。原始Manifest/证书/逐方法输出同目录归档；源码绑定需与最终提交的app树对照，不等于Android视觉通过。

[旧Preview11准入](c9-r1-preview12-20261029-163944/preflight.json)、[本轮组合命令/退出码](c9-r1-preview12-20261029-163944/build-check/command.json)、[逐类/逐方法JVM](c9-r1-preview12-20261029-163944/build-check/jvm-summary.json)及同目录原始stdout/stderr、XML/Lint保留。不会引用原R1的95项结果冒充本次重新运行。

## 简短自愿体验清单

1. 有Preview11记录时先手动备份；自行选择同包覆盖更新。若系统拒绝升级则停止，不以卸载/清数据解决，真实升级仍NOT_RUN。
2. 设置→外观切换岩白雾蓝（默认）／纯白／深色黑灰，观察主要页面颜色及重开App后的选择；出现保存错误时以提示为准。
3. 检查首页真实记录是否优先、物品与订阅详情核心/辅助金额是否清楚、回看与完整趋势的选日和图例是否易读。长名称/金额或大字体有问题时保留具体页面反馈，不改重要业务事实来测试。
4. 主要表单输入、错误提示和返回来源可按需体验，图片导入仍不可用。建议可丢弃记录，不把体验版作为重要数据唯一副本。

以上是用户未来自愿体验说明，不是Codex已执行的手机验收。

## 未关闭门禁与停止点

- 本轮Android实际运行、三主题视觉、动态/几何/字体/IME、性能、覆盖升级及真实恢复仍NOT_RUN/PENDING。C9-R1相关Android/Compose源码仅编译；C9/C7/C6其他未运行案例不改判。
- C6-1只读媒体和C6-2C静态隔离实现保持原范围，资源/取消/真实隔离运行证据不扩大；MEDIA_IMPORT_BLOCKED，不接Picker/JPEG native/媒体写入。
- M10三JPEG FAIL、M20平台FAIL/收口UNKNOWN、M22 BLOCKED、C3关闭调查/运行UNKNOWN、M13-T06、Q03/Q04及G1～G4全部历史门禁保留。
- ADB、手机/AVD、安装/卸载/清理、真实账本操作0。无新依赖/权限/联网；APK和密钥不入Git，不生成ZIP或重复候选，不合并main/M23/正式发行。

状态：**C9-R1 PREVIEW 12 — READY_FOR_INDEPENDENT_REVIEW**；ANDROID_VISUAL_PENDING / ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED。提交推送后停止，等待ChatGPT独立交付审查；不自授视觉、真实升级或正式发行通过。
