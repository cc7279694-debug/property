# C9 Preview 11 个人体验候选

2026-10-29；`codex/lithic-v1-product-completion`；基线 `9c0956f85dcdc53d2cae2e0260f3dcaf23653078`。用户转交C9 **ACCEPTED / FROZEN — PASS WITH NOTES，仅桌面实施**，已在CURRENT_STATE登记；原C9报告、失败记录及Android NOT_RUN保留。

本轮只修改显式Private Trial版本26→27、preview.10→preview.11；不改生产/测试源码及其他默认身份。全部桌面及实物静态门禁通过；Android实际体验不在本次结论内。

## 最终APK

- 唯一非build交付：`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/deliveries/lithic-preview-0.1.0-preview.11/Lithic-0.1.0-preview.11-debug.apk`。
- 包名：`com.cooper.lithic.preview.debug`；名称：砺石·体验版；**code27 / `0.1.0-preview.11-debug`**，minSdk26/targetSdk36。
- 大小：**15,961,550字节**；SHA-256：`5bbf3c1f946fc2da4ab259ab377f8134e222255784fe81e20c7a73337e63cf90`。
- 原Debug证书SHA-256：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。不是正式Release签名，未生成或替换密钥。
- 复制后和构建产物长度/SHA一致；Preview10/code26原包15,912,394字节、SHA `55a3646e62661760c78797d14d9912a74a89b852c065037056d8d0fad840bc70`及原证书已在构建前后核对，未覆盖/删除。

## 实际桌面验证

| 门禁 | 实际结果 |
|---|---|
| 定向JVM | **66唯一方法PASS，0失败/错误/跳过**：外观Controller14、实际偏好文件3、主题对比5、进程角色8、C7来源10、C5-R1呈现18、金额呈现8。不重复计算原C9的135项历史记录。 |
| 普通Debug | assembleDebug、AndroidTest源码编译及Lint组合命令exit0；**Lint0错误16警告**。普通包实际code17及原debug身份保持。 |
| Private Trial | assembleDebug、AndroidTest源码编译及Lint组合命令exit0；**Lint0错误14警告**，未重复JVM；两个Lint XML均实际本轮生成。 |
| 原始记录 | 两次构建前可用内存6.09/6.53GiB；离线/单worker；SDK XML版本警告保留，未安装或升级工具。stdout/stderr保留原始字节，含Windows默认编码输出。 |
| Manifest/签名 | 原证书通过apksigner；正常LithicApplication和唯一MainActivity Launcher、本包Startup authority；无Instrumentation/testOnly/sharedUserId，无INTERNET，5项权限与Preview10一致。 |
| DEX及入口 | 20个指定生产类定义实际存在，包括三主题/外观保存面板、MainActivity/App/Navigation、首页、两类详情/表单、回看、设置及受控封面组件；JUnit/AndroidXTest/ComposeTest、M13Runner及截图夹具类定义未进入用户APK。源码入口核对与类定义存在均是静态证据。 |
| 冻结边界 | 对9c0956f仅Private Trial两行版本差异；普通Debug/Release默认及M22/C3身份配置保持。Room5/backup4、业务/Owner/generation/恢复/导航/媒体安全源不改；8个原native条目字节不变，未接Picker、JPEG库或媒体写入。 |

[APK身份/复制核对](c9-preview11-20261029-120738/artifact/identity.json)、[四条SDK核验命令](c9-preview11-20261029-120738/artifact/commands.json)、[普通Lint](c9-preview11-20261029-120738/normal-debug/lint.xml)、[体验Lint](c9-preview11-20261029-120738/private-trial/lint.xml)及同目录原始Manifest/证书输出。源树绑定见[source-binding](c9-preview11-20261029-120738/source-binding.json)。

[Preview10准入](c9-preview11-20261029-120738/preflight.json)、[普通Debug命令/退出码](c9-preview11-20261029-120738/normal-debug/command.json)、[66项逐类汇总](c9-preview11-20261029-120738/normal-debug/jvm-summary.json)、[体验构建命令/退出码](c9-preview11-20261029-120738/private-trial/command.json)。原stdout/stderr、JUnit XML、Lint及后续APK审计随目录归档；不复制整仓或生成ZIP。

## 自愿体验时只看这几项

1. 设置→外观，依次选择深色黑灰／纯白／岩白雾蓝；检查选中标记、跨页颜色及下次启动是否保留。遇保存错误时看提示，不把未保存当作成功。
2. 浏览现在、物品和订阅详情、回看及录入页；观察金额完整、长名称/字号、返回来源及错误提示。不要为检查视觉改动重要业务事实。
3. 已有Preview10数据先手动备份，选择普通覆盖更新；若升级失败立即停止，不以卸载/清数据解决。建议先用可丢弃记录，不作重要数据唯一副本。

以上是未来用户可自行选择的检查，不是本轮实际运行结果，不要求现在安装或操作手机。

## 未关闭门禁

- Android视觉/动态、字体/IME、真实启动/主题保存与跨页体验、实际升级及性能：NOT_RUN/PENDING；没有新截图或手机操作。
- C9的10个、C7的1个、C6-1的14个及C6-2C的8个Android案例保持NOT_RUN，用户体验不代替自动化安全测试。
- C6-1只读模块保持桌面限定冻结；资源峰值/取消等原UNKNOWN及 **MEDIA_IMPORT_BLOCKED** 保留。没有图片Picker、JPEG native或媒体新增/替换/移除入口。
- M10三JPEG FAIL、M20平台FAIL/收口UNKNOWN、M22 BLOCKED、C3关闭调查/运行UNKNOWN、M13-T06、Q03/Q04及G1～G4全部历史状态保持。不是正式V1.0/Release。
- ADB、设备、安装/卸载/清理、私人账本操作0。旧Preview10原包保留；APK及密钥不入Git，不合并main、不进入M23。

状态：**C9 PREVIEW 11 — READY_FOR_INDEPENDENT_REVIEW**；ANDROID_VISUAL_PENDING / ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED。提交推送后停止等待ChatGPT独立交付审查，不自授视觉或升级通过。
