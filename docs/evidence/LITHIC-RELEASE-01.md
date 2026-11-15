# 砺石 LITHIC 1.0.1 · 正式签名候选

日期 2026-11-15（Asia/Shanghai）。工作分支 `codex/lithic-release-01`，交付渠道为本机正式签名 APK。

**READY_FOR_RELEASE_REVIEW**

## 源码与历史

- Fetch 核对 GitHub `main` 为 `145326ddd4ce28abbf7c2706fe25f9d38c599c3a`，UI 分支为 `849de4bc350c241dcbe2f616fe2b4f4da4a23b39`，与 UI 源码提交 `e52521c77f9fc889912974c93a902b10da0dcdef` 对应同一 App 树。旧正式私发资料分支为 `d42a78125b83bee806add5e3fdd8d2328055b694`。
- 构建提交 `158d7df53f829c0e9f25547e82aa4f9d1e3cd0c1`，App 树 `7c4ed9e88cdd607356d3b6d239d905e3f5a029f1`。相对 UI 基线唯一源码差异在构建配置，Release 版本升至 `1.0.1 / code37`，现有正式签名明确启用 v2 和 v3。首页、项目与订阅详情、全局主题、其他页面、数据协议及 Debug 变体沿用基线。
- 当前 `main` 是 Release 源码的共同祖先，无 main 独有提交。对照为 243 个提交与 9920 个文件，包含既有产品完整历史。仅发布工作分支进入独立审查。

## 旧正式包与覆盖升级

- 已归档旧正式 APK 实测为 `com.cooper.lithic / 1.0.0 / code36`，15,747,081 字节，SHA-256 `ff909ca1eb21e04756d91d067082ca12df1ac17c263d793843fbbf4c67e40c2b`。严格验签通过，单一 v2 签名者。对照本机归档、Git 发布历史和远端标签，code36 是最高已确认正式版本。
- 旧 APK 的签名证书与本机公开证书的指纹同为 `3a10918a24ab4c66fd2f5e2725b0b8a65164d4da43f71a40d6e6d74afc4b1f19`。公开证书从 2026-11-06 16:19:15 至 2053-11-06 16:19:15 有效（Asia/Shanghai）。沿用已有 PKCS12 与别名，Gradle 签署门禁验证私钥与核准公开证书的对应性。
- Room schema 1–5 导出、Room5 当前版本、迁移及数据存储和备份恢复生产源码，均与旧正式构建源码 `a5b58158d98507d80eb619dec1beeeb1e3de2fee` 一致。backup4 继续使用 format/minReader/schema 4/4/5。最新首页总投入沿用已有读事务，并按既有批准口径汇总。
- 新旧 APK 的同包名、同证书及 code36→37 已通过静态更新身份核验。实际覆盖安装、设备上账本保留及真实 SAF 恢复均为 `NOT_RUN`。[Android 更新身份规则](https://developer.android.com/google/play/app-updates)

## 桌面验收

从已固定 Git 源码按离线、单 worker 执行 Release Kotlin、全部 Release JVM 及完整 Release Lint。Gradle 退出 0，共 10 分 36 秒。

| 验证 | 结果 |
| --- | --- |
| Release Kotlin | PASS |
| Release JVM | 116 类、985 项 PASS，0 FAIL／ERROR／SKIP |
| 离线脚本测试 | 30 项 PASS，使用模拟输入 |
| 完整 Release Lint | 0 错误、20 警告，与历史正式门禁数量一致 |
| Release BuildConfig | 正式 id、1.0.1/code37，生产照片开关开、实验照片开关关 |
| Android | 覆盖升级、首次打开、合成账本读回、真实 SAF、三主题及 200% 字体均为 `NOT_RUN`。未启动历史 ANR AVD，未连接设备或读取个人账本 |

[完整桌面回执](lithic-release-01/desktop-results.json) · [Gradle 日志](lithic-release-01/desktop-build.log) · [Release Lint 明细](lithic-release-01/lint-results-release.txt) · [离线 Python 测试记录](lithic-release-01/desktop-python.log)

## 正式签署与归档

受控签名工作区和已通过的桌面验收绑定到同一源码及 App 树。用户在本机隐藏提示输入原密码。离线 `:app:assembleRelease` 于 21:15:47 至 21:22:16 实际执行（Asia/Shanghai），Gradle 退出 0。签署回执保留任务行、退出码与时间。首个终端受 PowerShell 脚本策略阻止，第二个终端也在提示前出错，具体原因未确认。两者都没有启动签署。经文件资源管理器启动的本机入口完成了唯一一次实际签署。

| 项目 | 实物核验 |
| --- | --- |
| 本机 APK | `E:\Lithic-Private-Release\Release01-1.0.1-session\deliveries\lithic-v1.0.1\Lithic-1.0.1-release.apk` |
| 包名、版本、平台 | `com.cooper.lithic`，`1.0.1` / code37，minSdk 26 / targetSdk 36 |
| 文件大小 | 15,785,841 字节 |
| APK SHA-256 | `498b0c84dd780944e5f76c43b81343d95603cc985a1ed5d2c64e8723e0204342` |
| 正式证书 SHA-256 | `3a10918a24ab4c66fd2f5e2725b0b8a65164d4da43f71a40d6e6d74afc4b1f19` |
| 签名 | 严格验签通过，单一签名者，APK v2 和 v3 均有效，证书和旧正式版相同 |
| Manifest 与权限 | 与旧正式版对照，仅版本变化，权限及备份排除规则一致。入口为 `LithicApplication` / `MainActivity`，调试、测试和共享 UID 声明均无 |
| 包内容 | 三个 DEX 可读。核对首页总投入与项目／订阅切换、项目双成本卡、订阅付款历史、Room5、备份检查与恢复。生产照片功能及 API 门禁已打包，实验照片及自动提醒开关关闭 |
| 归档 | ZIP CRC 与 16 KiB 对齐检查通过。本机归档与实际构建产物逐字节相同，旧正式 APK 哈希复核一致 |

[签名构建任务日志](lithic-release-01/signed-release-build.log) · [归档回执](lithic-release-01/delivery.json) · [新旧 APK 对比](lithic-release-01/apk-audit.json) · [正式包签名与 Manifest](lithic-release-01/candidate-apk-audit.txt) · [旧包证据](lithic-release-01/baseline-apk-audit.txt) · [DEX 抽查](lithic-release-01/dex-audit.txt)

独立验收范围为本机正式 APK、桌面证据与静态兼容性。Android 覆盖安装、账本保留、真实 SAF 和照片端侧操作继续为 `NOT_RUN`。下一步等待 ChatGPT 明确返回 `RELEASE ACCEPTED` 后复核 `main` 及发布门禁。GitHub 工作分支交付源码和证据，签名 APK 仅在本机归档。
