# LITHIC V1-A1 — 小米14加速集中验收

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
