# LITHIC V1-A1 — 小米14加速集中验收

## 最新结果：用户选择结束剩余测试

**V1-A1 ACCELERATED CORE — PARTIAL / READY_FOR_REVIEW**（2026-10-31）

首次阻断提交 `aaf9df4b006f6b7650d39d6975b817337287288d` 后，用户先明确回复“重新来一次”，另授权一次App安装；Test首次被拒后又明确回复“再授权Test安装一次，我会留意手机确认提示”。原失败回执不覆盖；这些是**新的明确授权，不是自动重试**。

| 后续阶段 | 实际结果 |
| --- | --- |
| App重试前准入 | 唯一指纹/小米14/API36匹配；V1两包仍无User0含保留记录，Preview12身份符合原值。[回执](v1-a1-accelerated-20261031/retry-1/preflight.json) |
| App新授权安装 | 锁定15,977,970字节/SHA，User0安装完整exit0 / `Success`。[回执](v1-a1-accelerated-20261031/retry-1/install-app.json) |
| App安装后核对 | `com.cooper.lithic.v1acceptance.debug` / code1 / `0.1.0-v1-a1-debug` / installed=true / **UID10455**；Preview12仍installed=true / code28 / UID10429。[回执](v1-a1-accelerated-20261031/retry-1/verify-app.json) |
| Test首次安装 | 原剩余额度1次，真实exit1 / `INSTALL_FAILED_USER_RESTRICTED`，不判业务FAIL，不自行重试。[失败原件](v1-a1-accelerated-20261031/retry-1/install-test.json) |
| Test另授权安装 | 先重新确认无同名记录，App及Preview身份符合；再仅用`--user 0 -t -S`传输锁定Test，完整exit0 / `Success`。[准入](v1-a1-accelerated-20261031/retry-1/test-retry-preflight.json)、[成功回执](v1-a1-accelerated-20261031/retry-1/install-test-retry.json) |
| 两包及Runner | Test installed=true / **UID10458**，系统元数据code0/versionName字符串`null`（Manifest本未声明版本）；三UID各异。唯一注册为独立Test/M13IsolatedRunner，仅target独立App；**注册不等于执行**。Preview12身份保持前述值。[回执](v1-a1-accelerated-20261031/retry-1/verify-test-retry.json) |
| 正常Main启动 | 唯一一次`am start --user 0 -W -n ...MainActivity`，真实exit0 / `Status: ok`；正常生产入口，不是plain测试宿主。[回执](v1-a1-accelerated-20261031/retry-1/launch-normal.json)。AM用时不是视觉/性能PASS |
| 人工空首页 | 用户实际回复“正常空首页，没有旧记录”。仅登记用户人工观察通过，不宣称所有跨包访问均已验证 |
| 人工物品创建 | 用户报告“已完成”；随后明确确认页面**初始投入100元、持有10天、日均10元/天**三项一致。这是用户读数，不是JUnit；开始日/付款日只回复“没问题”，具体日期字符串仍 **NOT_CAPTURED** |
| 其余核心与SAF | 在后续20元投入/30元收回、备注及取消核对批次，用户回复“**不测试那么多了，都算通过**”。按用户决定停止；该批实际完成状态未确认，不能把计划90元/9元每天冒充事实。订阅、列表/搜索/图表、真实SAF导出/覆盖恢复/重开均 **NOT_RUN / DEFERRED_BY_USER** |

人工原话、已确认数值和缺口见[manual-results.json](v1-a1-accelerated-20261031/retry-1/manual-results.json)。其中UUID只是本轮逻辑测试标识，不是读取到的Room/generation/记录ID；SAF未开始，该UUID没有对应已创建备份文件。用户选择延期不等于技术PASS，也不要求用户现在继续补测。

**截至停止：App总2次安装尝试/1次成功，Test总2次尝试/1次成功，正常Main启动1次，Instrumentation0次，自动重试0次。** 精确配置探针/T06不执行：正常Main已进入业务Owner环境，未证明可安全切换plain宿主，且用户已停止后续测试；没有为T06改变或查询网络。T03、200%/IME、M20、旧SAF、C6及C3均未执行。

安装成功证明层级为：本地已审SHA/证书、发送流SHA、系统Success与User0元数据；**未回读设备APK字节或签名，不宣称端侧全字节核验**。末次三包身份核对在Main启动之前；用户结束后未补发设备查询。Owner、全UID子进程、Job及最终资源关闭仍 **NOT_PROVEN / UNKNOWN**，精确pidof空值也只属于启动前时点。正常App及Test留在手机上，当前合成现场保留，没有强停、清理或卸载。

本轮未改系统设置，无本轮设置更改需要恢复；Preview12仅做指定安装元数据查询，未被启动或写入私人数据。两归档及11个历史本地交付APK原字节、产品/Gradle/测试源码保持，见[后续结束记录](v1-a1-accelerated-20261031/retry-1/end-summary.json)。新源码图标仍不在锁定旧候选中。

**V1_RELEASE_NO_GO / ANDROID_RUNTIME_ACCEPTANCE_INCOMPLETE / AUTOMATED_ANDROID_TESTS_NOT_RUN / ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED**。本轮确有正常Main及人工局部观察，不再称全部运行均未开始；但不扩大为完整Android或正式发行通过。全部原M10/M20/M22/C3/G1～G4风险保持。提交本次增量后停止，不自动安排补测、升级、发行或下一阶段。

---

## 以下保留首次执行时的阻断记录

2026-10-31（Asia/Shanghai）；分支 `codex/lithic-v1-product-completion`；实际基线 **`ec6ddfae14a8a64273ede160302d09d681e047b5`**，开始时远端一致、工作区干净。任务书参考 `a973d98` 后仅有用户授权的新图标资源/文档提交；本轮继续使用已审归档，不重建、不把新图标冒充已进入候选。

**V1-A1 ACCELERATED CORE — BLOCKED / INSTALLATION_REJECTED**

## 实际执行与首次结果

| 阶段 | 本轮结果及证据边界 |
| --- | --- |
| 用户手动健康检查 | 用户实际回复“已检查，均正常，手机已连接并解锁”，对应桌面、通知栏、点击滑动及无系统无响应弹窗。属于**用户人工观察**，不是自动化 ANR/全系统健康 PASS；前次报告当时的 UNKNOWN 原样保留 |
| 目标身份 | 现有 ADB smart socket；历史序列 SHA-256 `e4f0e1d2361b0ef560ff8ac99a067fb8f540594c827d28b6cd21a720e724af92` 唯一匹配且状态 `device`。本轮精确目标 getprop 为 Xiaomi / `23127PN0CC` / API36；ABI/资源/Boot/SystemUI 未重复查询。其他设备内部查询0 |
| 安装前包门禁 | V1-A1 App/Test 在 User 0 包含保留记录的精确查询均无记录；Preview12 为 installed=true / UID10429 / code28 / `0.1.0-preview.12-debug`。仅元数据，不读任何旧账本、文件或 APK。5条定向 shell 全部 exit0，见[原始准入回执](v1-a1-accelerated-20261031/preflight.json) |
| 本地候选 | App **15,977,970字节**，SHA `626c9809b7566d56583d49e4dbb43abe90d44e151a5e9357d2ca90c62f35a97a`；Test **3,741,308字节**，SHA `1fdfc6ae8b784b8041d8182089bd1bf7af00ccc0e5d1f047de9ad9358f7e1154`。正常 LithicApplication/MainActivity、唯一 M13IsolatedRunner/独立 targetPackage 与原证书静态核对一致，见[准备记录](v1-a1-accelerated-20261031/preparation.json)及已审[候选报告](LITHIC-V1-A1-Candidate-Preparation.md) |
| App 安装 | **尝试1次，成功0次**。通过明确选定 serial 的 shell-v2 向 User 0 流式普通安装，仅 `cmd package install --user 0 -S 15977970`，无覆盖/降级/自动授权参数；完整结束，真实 shell exit **1**。返回 `INSTALL_FAILED_USER_RESTRICTED`，见[首次安装原始回执](v1-a1-accelerated-20261031/install-app.json) |
| Test 安装 | **NOT_RUN / 0次**：App安装未成功，依赖门禁未通过，不消耗Test安装额度，也不绕过依赖继续 |
| 正常 App 启动/空首页 | **NOT_RUN / 0次**：未发送 MainActivity 启动命令；正常 Application、Owner 与可见首页未取得运行证据 |
| 物品旅程 | 创建、编辑、投入20、收回30、净投入90、持有10日及日均9元、历史/取消/返回均 **NOT_RUN**。这些只是原计划预期，未创建相应事实 |
| 订阅旅程 | 首付、续费、暂停恢复、覆盖日期/成本/历史及返回均 **NOT_RUN**。未生成任何测试付款，未触发提醒实验 |
| 列表/搜索/分类/趋势 | 全部 **NOT_RUN**；不把历史JVM或旧版片段体验移作本轮Android结果 |
| 真实SAF备份恢复 | 导出、备份后新增标记、预览取消、确认覆盖、恢复后重开及事实核对全部 **NOT_RUN**；未打开选择器、创建或读取备份文件 |
| 自动测试 | 两个白名单方法均 **NOT_RUN / 实际执行0项**。原T03、200%/IME、M20、旧SAF自动化、C6及C3均未执行 |

## 安装原始失败与停止

2026-10-31 **11:22:31–11:22:44**（Asia/Shanghai），仅一次安装请求。主机向选定 transport 发送的输入为锁定 App 全部15,977,970字节，发送流SHA与锁定值一致；**发送完成不代表设备已完整校验或安装**。

```text
Failure [INSTALL_FAILED_USER_RESTRICTED: Install canceled by user]
```

stdout **67字节** / SHA-256 `14e130bb48896e74c0bffb023a706b013f507fe56831771e3217734dc8e984a7`；stderr0字节，结束包已收到、exit1，不是桌面超时，也不是业务JUnit FAIL。该英文为**系统原文**；实际是人为取消、厂商安装策略还是其他原因，**UNKNOWN**，不能归责用户或直接判产品缺陷。

按一次安装及失败即停的合同，**没有重试、补发安装后查询、安装Test或启动App**。因此停止后的新包记录、安装器临时会话/文件、目标进程、Owner及资源收口均 **UNKNOWN / NOT_OBSERVED**；不从失败码推断“肯定未安装”“无残留”或“已全部关闭”。保留首次现场，不清理、不强停、不取消系统任务。执行辅助[脚本](v1-a1-accelerated-20261031/device-session.py)为每个action预先exclusive-create回执，阻止同action自动重复。

## 保护、范围及后续

- Preview12只做了安装前元数据查询，未启动、更新、覆盖、停止、卸载、清理或读其私人数据。**停止后身份及业务完整性未复核**，不能把前检扩大为后检/数据完整性PASS。V1-A1两包及包含Preview12在内的11个本地历史交付APK（共13份）原实物核对见[本地结束记录](v1-a1-accelerated-20261031/closure.json)。
- 没有全量日志、系统任务dump、截图、录屏、其他App数据、APK回读或模拟器操作；没有改网络、字体、通知权限等设置，故无本轮设置需要恢复。没有测试UUID业务数据集、SAF文件或用户操作结果可登记为PASS。
- 本轮只新增证据/执行辅助及状态登记；产品/Gradle/测试源码、Room5/backup4、业务计算、Owner/generation、恢复Token、媒体隔离协议及归档APK不改。没有构建、JVM重跑或Android测试；照片写入依旧BLOCKED。
- **本轮没有发现可由运行证据确认的产品代码缺陷**。先解决安装被拒这一执行阻断才有后续正常App/真实SAF证据；拒绝的具体原因尚不明，本轮不改安全设置、不修复/重试。是否再给予一次有限安装机会，交由ChatGPT独立审查及用户新授权；Codex不自动提出或执行第二轮。
- 正式V1仍被当前核心闭环、真实备份恢复、异常/长期规模/IME、提醒平台、升级签名及G1～G4等原门禁阻断；本轮安装前检查不能关闭它们。原[Readiness](LITHIC-V1-Release-Readiness.md)、M10 JPEG、M20、M22、C3及全部历史FAIL/UNKNOWN/NOT_RUN不改判。

**V1_RELEASE_NO_GO / ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED**

**ANDROID_RUNTIME_ACCEPTANCE_NOT_RUN**。本轮App安装额度已用完，停止等待独立审查；未进入Preview12升级、JPEG整合、main、M23或发行。
