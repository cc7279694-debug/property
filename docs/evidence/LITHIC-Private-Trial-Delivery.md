# 砺石·体验版 — 个人交付

**READY_FOR_DELIVERY — LITHIC PRIVATE TRIAL** / **PRIVATE TRIAL DELIVERY — WITH KNOWN RISKS**。用户2026-10-24明确批准停止M22重复验收循环，优先交付个人体验APK；这不等于M22验收PASS、正式签名或公开发布准入。产品来源`3c3e293cb04e92295a4442acd451326ed36028e9`，分支`codex/m22-experience-accessibility`，开始本地/远端一致、工作区干净，无回退功能。

## APK与安装

[本机唯一交付APK](E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/app/build/private-trial/outputs/apk/debug/app-debug.apk)（仅保留本地产物，不提交Git）

```text
E:\CODEX\.codex\worktrees\lithic-m13\砺石新APP\app\build\private-trial\outputs\apk\debug\app-debug.apk
```

| 字段 | 实际值 |
| --- | --- |
| 显示名称 | 砺石·体验版 |
| applicationId | `com.cooper.lithic.preview.debug` |
| 版本 | `0.1.0-preview.1-debug` / versionCode **17** |
| 长度 | **13,822,272 bytes** |
| SHA-256 | `8138bfbebfaf4580cc71bcc1aabe8a0a50505c2262260b99747eea351183a7fa` |
| Android声明范围 | minSdk26 / targetSdk36；不是全部版本/OEM的运行验证 |
| 签名 | 原有本机Debug证书，单一Signer；**不是Release** |
| 证书SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9` |

用户可将该APK复制到手机，使用文件管理器打开，按Android提示安装“砺石·体验版”；是否安装由用户自行选择。本轮没有ADB、安装、卸载、启动或手机数据操作。它与现有M10 `com.cooper.lithic.debug`、M22验证App/Test是不同包，**不自动迁移或读取它们的账本**，不需要卸载旧App。已安装UID/实际目录分配与首次启动本轮均NOT_RUN，包隔离依据是最终APK与源码的静态核验。

体验版数据保存在自己包的私有目录。可从设置页手动导出完整备份；请先用少量体验记录熟悉功能，再自行决定使用范围。不要用卸载重装代替后续更新；卸载可能移除体验数据，系统自动备份已关闭。

## 本轮最小改动与门禁

只增加[Gradle显式开关](../../app/build.gradle.kts) `-PlithicPrivateTrial=true` 和[体验标签Manifest覆盖](../../app/src/privateTrial/AndroidManifest.xml)，输出到`app/build/private-trial`。复用当前项目、订阅、成本/图表、媒体、类别和完整备份恢复代码，没有改生产业务或测试源码、Room5、backup4、冻结金额/日期算法、依赖或工具链。

- **编译PASS**：仅`:app:assembleDebug`，离线复用缓存，exit0，385.844秒；39个构建任务实际执行，未构建AndroidTest或Release。保留SDK XML版本提示、Kotlin copy可见性及旧Parcelable API编译警告，不隐藏、不顺便修改冻结源码。Lint/JVM/Android测试本轮均NOT_RUN。
- **Manifest/PACKAGE PASS**：App为`com.cooper.lithic.app.LithicApplication`，唯一Launcher为`com.cooper.lithic.app.MainActivity`；Provider authority为`com.cooper.lithic.preview.debug.androidx-startup`且非导出，无sharedUserId、Instrumentation、INTERNET或新增产品权限。保留原系统/OEM备份排除；权限仍为通知、唤醒、网络状态读取、启动完成及本包签名级接收器保护。
- **DEX/签名PASS**：实际DEX定义包含正常Application/MainActivity/AppContainer，无M13IsolatedRunner、CompleteJourneyTest或JUnit类；签名校验exit0、原证书精确匹配。签名、包信息及APK Manifest三条桌面工具命令全部exit0。
- **默认构建及旧产物保护PASS**：普通Debug/Release的Gradle配置修改前后JSON完全相同：`com.cooper.lithic.debug` / `com.cooper.lithic`、原code17及版本、Manifest来源/输出目录均保留；原普通及M22验证App/Test四份本地APK长度与SHA重算不变。未操作任何手机或AVD，也未读取个人账本。

正常Application将创建**体验包自己的默认generation和WorkManager调度**，这正是完整产品入口，不是plain Application测试宿主。关闭页面不等于存储Owner已优雅关闭；没有通过本次桌面核验改判任何历史Owner/任务UNKNOWN。此Debug包仍含原有Debug测试宿主Activity和受系统权限保护的诊断组件，不用于公开发行。

重建仅使用Debug目标（JDK/SDK沿用[工具链锁](../TOOLCHAIN_LOCK.md)）：

```powershell
.\gradlew.bat --offline -PlithicPrivateTrial=true :app:assembleDebug
```

不要与`m22DeviceVerification=true`、`m13Isolated`或`m12T04ProcessDeath`混用，配置会拒绝这些组合。该开关只用于本文Debug交付，不提供体验Release；未设置开关的普通Debug/Release保持原配置。

## 后续更新与已知风险

后续体验版更新必须保持**同applicationId、同公开证书指纹**，versionCode严格递增（下一版至少18），重新记录版本/SHA；通过同签名覆盖更新保留数据，不自动替换成Release或其他包。保管现有本机Debug签名文件及凭据，勿删除或更换，不发送密钥/密码到聊天或Git。配置在原签名文件缺失时直接失败，避免自动生成替代密钥；存在文件本身不能证明证书正确，**每次产物仍须核对上述证书**。本轮没有新建/复制密钥，没有将密钥或密码写入Git/报告/备份。长期正式签名及真实安装升级仍是M13-T06延期门禁。

保留的风险与未完成项：

1. **M22整体仍BLOCKED / CHANGES REQUIRED，剩余验收DEFERRED**。T06完整离线UI旅程有三次历史FAIL，近期受门禁阻断未运行；20年正式规模T03与真实系统大字/IME要求未完成，不承诺长历史性能或全部窄屏操作已经通过。
2. **M10 PENDING / DEFERRED，三项JPEG历史FAIL保留**；未合并未冻结Worker/Stage V2/JNI，不把已有封面功能或备份原字节口径冒充严格JPEG安全验证完成。
3. 提醒不保证OEM后台准点送达；M20额外平台FAIL/收口UNKNOWN、M21未测边界均保留。本次没有真实通知或后台实验。
4. 当前只是Debug个人体验交付；真实安装、普通MainActivity完整旅程、同签名覆盖升级、真机/OEM广覆盖和公开发行检查没有在本轮执行。M13-T06、Q03/Q04、G1/G2/G3及其余历史RED/FAIL/UNKNOWN/NOT_RUN/NOTES不改。

用户接受以上已知风险后自行选择安装和日常体验；后续仅按具体反馈做小范围修复，优先保护数据。不自动恢复M22验收循环、不冻结M22、不合并main、不进入M23。

## 必要证据

- [产物总审计与SHA](lithic-private-trial-20261024-104836/artifact-audit.json)
- [构建命令/退出码/时间/原始长度SHA](lithic-private-trial-20261024-104836/private-trial-build/command.json)、[原始构建输出](lithic-private-trial-20261024-104836/private-trial-build/stdout)、[原始stderr](lithic-private-trial-20261024-104836/private-trial-build/stderr)
- [合并Manifest](lithic-private-trial-20261024-104836/merged-manifest.xml)、[实际APK Manifest](lithic-private-trial-20261024-104836/apk-manifest/stdout)、[实际包与标签](lithic-private-trial-20261024-104836/apk-badging/stdout)、[签名原文](lithic-private-trial-20261024-104836/signature/stdout)
- [默认配置修改前](lithic-private-trial-20261024-104836/default-identity-before.json)、[修改后](lithic-private-trial-20261024-104836/default-identity-after.json)、[体验构建配置](lithic-private-trial-20261024-104836/private-trial-identity.json)

只交付一个本地APK及上述有界文本证据，没有ZIP、整仓快照、重复APK、密钥或设备数据归档。提交推送当前功能分支后停止。
