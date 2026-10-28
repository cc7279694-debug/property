# C5-R1限定登记与Preview 9交付回执

日期：2026-10-28；分支`codex/lithic-v1-product-completion`；起点`ec5ed9e8328cfaaf0522751f1ac04bc04f633fea`。

用户转交ChatGPT独立决定：**C5-R1 ACCEPTED / FROZEN — PASS WITH NOTES（仅桌面实施范围）**，已登记CURRENT_STATE；原报告、CHANGES REQUIRED和所有NOTE保持。Android视觉、导航动态、性能、真实升级及完整产品体验仍**PENDING**。

## 唯一体验APK

- 实际路径：`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/deliveries/lithic-preview-0.1.0-preview.9/Lithic-0.1.0-preview.9-debug.apk`。
- 包名：`com.cooper.lithic.preview.debug`；名称“砺石·体验版”；版本`0.1.0-preview.9-debug`／code**25**。
- 大小：**15,813,947字节**；SHA-256：`f82148a2b6f1775235fd32717a7ced7f855c3e612441aee06a64644878cdf803`。
- 原Debug证书SHA-256：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；apksigner实际验证通过，非正式Release签名，没有生成新密钥。
- 只创建一个非Gradle目录下的Preview9候选，复制后长度/SHA与构建APK一致；Preview8/code24原字节保持，未覆盖或删除。

## 桌面门禁与源码绑定

| 检查 | 实际结果 |
| --- | --- |
| 定向JVM | **24 PASS，0失败/错误/跳过**：C5R1TrendPresentationTest 18＋C1ReviewEmptyPresentationTest 6；本轮实际运行。 |
| AndroidTest源码编译 | 任务exit0，未变输入由Gradle复用；实际Android执行**NOT_RUN**。 |
| Private Trial构建/Lint | exit0；**Lint 0错误、13警告**。原Kotlin和SDK XML编译警告保留，没有降级检查或改业务源码。 |
| APK Manifest/签名/DEX/ZIP | 实物核对code25及preview.9、正常LithicApplication/MainActivity、自有未导出Provider、原5项权限且无INTERNET；无Instrumentation、sharedUserId、testOnly、测试宿主、JUnit/AndroidXTest/Runner或合成夹具类。C5-R1生产类与入口tag已打包，属于静态证据。 |
| 默认配置与保护 | 普通Debug/Release求值仍code17和原包/版本；M22/C3配置不变。产品差异严格只有Private Trial的code24→25、preview.8→preview.9两行。生产/测试源码、Room5、backup4、Owner/generation、恢复Token、金额/日期规则和写入/Nav3守卫无改。 |

构建输入Git app树：`a224863ca0732171dd7e0b8bdb76e6b0d9ab9798`；最终提交`HEAD:app`核对同值。APK与该输入树绑定，不能把DEX存在等同真实UI通过。见[源码边界](preview9-20261028-025921/scope-audit.json)、[绑定记录](preview9-20261028-025921/source-binding.json)、[24项结果/默认身份](preview9-20261028-025921/desktop-summary.json)、[桌面命令](preview9-20261028-025921/desktop-check/command.json)、[体验构建命令](preview9-20261028-025921/private-trial-build-lint/command.json)、[Lint XML](preview9-20261028-025921/preview9-lint.xml)、[实物审计](preview9-20261028-025921/artifact-audit.json)。各命令的原stdout/stderr、退出码、长度和SHA均随记录归档。

## 使用边界与停止

本轮ADB、Instrumentation、手机、模拟器、安装/卸载/清数据及C3现场操作均0。若用户自愿安装，应先手动备份已有体验记录，再选择同包覆盖更新，不以卸载清数据处理问题；真实升级未验证。仅供个人试用，不作为重要记录唯一副本。

保留MEDIA_DISPLAY_PENDING、M10 JPEG历史FAIL、M20平台FAIL/UNKNOWN、M22、M13及G1～G4未完成门禁；没有正式Release、完整产品或真实视觉PASS结论。提交推送后停止，不合并main或进入M23。

**PREVIEW 9 READY_FOR_OPTIONAL_USER_VISUAL_TRIAL / ANDROID_VISUAL_AND_MOTION_PENDING**
