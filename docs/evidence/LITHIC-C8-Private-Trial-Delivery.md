# C8 Preview 10 个人体验版交付

2026-10-28（Asia/Shanghai）；`codex/lithic-v1-product-completion`；基线 `1c97ff61d575dbb335bfc0390a0fcefb70db2fe5`。

**C8 PRIVATE TRIAL — READY_FOR_INDEPENDENT_REVIEW / ANDROID_VISUAL_PENDING / MEDIA_IMPORT_BLOCKED**

用户已转交 C7 **ACCEPTED / FROZEN — PASS WITH NOTES，仅桌面实施**，登记于 CURRENT_STATE；原报告、首次 RED 和 Android NOT_RUN 保留。C8只递增显式 Private Trial 两行版本，不改生产或测试源码，不进行设备操作。

## 唯一交付 APK

- 绝对路径：`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/deliveries/lithic-preview-0.1.0-preview.10/Lithic-0.1.0-preview.10-debug.apk`。
- 名称：砺石·体验版；包名：`com.cooper.lithic.preview.debug`。
- 版本：**0.1.0-preview.10-debug / code26**；minSdk26、targetSdk36。
- 大小：**15,912,394字节**。
- SHA-256：`55a3646e62661760c78797d14d9912a74a89b852c065037056d8d0fad840bc70`。
- 原 Debug 证书 SHA-256：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；apksigner验证通过，非正式Release签名，未生成密钥。
- 非build目录仅复制一个最终候选，复制后长度/SHA与构建APK一致。Preview9实物仍为code25、15,813,947字节、SHA `f82148a2b6f1775235fd32717a7ced7f855c3e612441aee06a64644878cdf803`，原包名/证书再次核对一致；未覆盖或删除旧交付物。

## 实际桌面门禁

| 检查 | 结果与边界 |
|---|---|
| 构建准入 | 初读5.877GiB、实际构建前5.591GiB可用；没有关闭其他进程。离线、单worker构建。 |
| 定向JVM | **49 PASS，0失败/错误/跳过**：C7来源10、C2导航5、C5-R1趋势呈现18、封面策略6/管线2、启动角色8。只计唯一方法。 |
| Private Trial构建/AndroidTest编译 | 同一组合命令exit0，assembleDebug及AndroidTest源码编译完成；实际Android测试全部 **NOT_RUN**，未生成Test安装包或运行Instrumentation。 |
| Lint | **0错误13警告**；原SDK XML版本警告保留，没有降低检查。 |
| 实物Manifest/签名 | code26及preview.10、正常LithicApplication和唯一MainActivity Launcher；自有未导出Startup Provider位于本包主进程，无sharedUserId、testOnly或Instrumentation；与Preview9相同5项权限，无INTERNET，原证书一致。 |
| 生产接线/DEX | 当前C5首页、两类列表/详情、回看、设置、MoneyMetric、C7两个保存来源helper及C6-1只读读取/显示组件确实进入APK。没有JUnit、AndroidXTest、Compose测试宿主/Runner、截图夹具或C7测试类。静态存在不等于Android视觉通过。 |
| 冻结边界 | 仅Private Trial由25/preview.9变为26/preview.10；普通Debug/Release仍原code17和原身份，M22/C3配置不变。Room5、backup4、全部业务/Owner/generation/恢复/导航守卫及C6-1/C6-2C源码无差异。未接图片Picker、JNI、native库或隔离导入通道用户入口，原native库集合不变。 |

[构建命令/退出码](c8-private-trial-20261028-220445/build-check/command.json)、[stdout](c8-private-trial-20261028-220445/build-check/stdout)、[stderr](c8-private-trial-20261028-220445/build-check/stderr)、[49项汇总与逐方法XML](c8-private-trial-20261028-220445/build-check/jvm-summary.json)、[Lint XML](c8-private-trial-20261028-220445/build-check/lint.xml)、[Preview9/10实物审计](c8-private-trial-20261028-220445/artifact/audit.json)、[构建输入](c8-private-trial-20261028-220445/source-input.json)、[Git绑定与边界](c8-private-trial-20261028-220445/source-binding.json)。原始aapt2/apksigner输出随审计保存。

首次桌面审计把aapt2输出的布尔文本`false`误按数值类型表示匹配，脚本exit1、尚未复制交付物；修正审计表示解析后，同一APK通过。生产/测试源码、构建及APK未因此改变；[首次审计说明](c8-private-trial-20261028-220445/artifact/first-verifier-result.txt)与原始输出保留，工具起止时间未保存部分标NOT_CAPTURED。

## 个人体验与未关闭门禁

按[用户体验说明](LITHIC-C8-User-Trial-Checklist.md)自行选择时间手动安装；已有体验数据先手动备份，同包同证书覆盖升级若失败则停止，不以卸载/清数据解决。真实升级仍未验证。仅个人Debug试用，不作为重要数据唯一副本，不是正式V1.0。

- C7新增1项、C6-1的14项、C6-2C的8项Android继续NOT_RUN；完整旅程、真实视觉/动态/字体/IME/性能及安全升级仍PENDING。用户体验不代替自动化安全验证。
- C6-1只读封面保持限定冻结，媒体资源峰值/取消等UNKNOWN；新增、替换、移除和JPEG导入继续 **MEDIA_IMPORT_BLOCKED**。
- M10延期与三个JPEG历史FAIL、M20平台FAIL/收口UNKNOWN、M22 BLOCKED、C3关闭调查/运行状态UNKNOWN、M13-T06延期、Q03/Q04、G1～G4及全部历史NOTES/NOT_RUN保留。
- ADB、手机、模拟器、安装/卸载/强停、私人账本操作均0。APK和密钥不入Git，不生成ZIP，不合并main、不进入M23；提交后停止等待独立审查。
