# LITHIC V1-B1 — Data Confidence

2026-11-01；基线`dd31ce4b0613d0ed53e2a8280375739d75550032`，来源P3远端/本地一致且干净后建立`codex/v1-b1-data-confidence`。当前为SOURCE_PREPARED / BUILD_DEFERRED，验证及候选尚未完成；不是功能验收PASS。

## 执行合同与最小计划

- 目标：设置中的只读完整备份文件检查，以及照片登记/使用/历史留存与登记长度估算。
- 复用：BackupValidator及既有format1～4兼容、媒体PNG/JPEG容器与尺寸规则；当前Owner/coordinator快照、Room只读查询；C9设置分组与既有SAF模式。
- 界限：检查不调用RestoreCoordinator、不产生恢复Token、不激活代、不写当前账本或封面。照片统计包含项目和订阅及历史，引用按不同assetId去重；不读照片路径、不冒充物理磁盘占用，不删除媒体。
- 顺序：①备份检查service/类型化结果与断言；②同代媒体聚合reader与断言；③Controller/VM/SAF/设置及互斥守卫；④定向JVM、AndroidTest源码编译、构建/Lint；⑤实物Manifest/DEX/原签名/图标/16份旧APK审计，再唯一归档Preview15/code31。
- 验收：VERIFIED/INVALID/UNSUPPORTED/UNKNOWN据真实结果区分；取消、重复、旧Owner及迟到不冒充成功；统计错误不显示0；安全说明和私有路径隔离可测试。无Android运行不能报告真实SAF或恢复PASS。
- 不做：Room5/backup4迁移、恢复协议/Token/Owner或冻结算法改造、媒体GC/JPEG native、主题重做、依赖/SDK/权限升级、设备/ADB/AVD/安装/真实账本、main或V1-B2。

源码及执行结果在完成后集中追加；原P3断言、失败历史、Preview14及其余15份旧APK保留。V1_RELEASE_NO_GO、STRICT_JPEG_IMPORT_BLOCKED、ANDROID_ISOLATION_RUNTIME_PENDING保持。

## 实际源码差异

| 范围 | 真实接线与限制 |
| --- | --- |
| 备份只读检查 | 新`BackupInspectionService`仅接收独立cache路径、输入流及只读有效性回调，不持有数据库、RestoreCoordinator、Token或代激活能力。复用`BackupValidator`原ZIP/Manifest、全部长度/SHA、结构/关联与旧格式升级规则，再核对PNG/JPEG容器、实际尺寸和原预算。`BackupManifest`只增加Unsupported异常子类型，四组format/reader/schema支持集合完全不变 |
| 结果 | VERIFIED=通过当前文件完整性规则；INVALID=确定的结构/CRC/SHA/关联等校验失败；UNSUPPORTED=识别出的不支持版本/格式规则；UNKNOWN=取消、IO、资源、Owner切换或终结不明。明确不等同恢复成功、Android可解码或JPEG严格熵认证，不保证未来所有环境能恢复 |
| 暂存 | 仅明确选择后在IO验证原可信Android cache/UID，单级创建专属目录和新UUID。原源/展开各<1GiB，预留2GiB+128MiB并最多允许3个未收口残留会话；不满足返回UNKNOWN。仅已完成完整认证的本UUID精确清单可清理；校验失败或中断中尚未取得归属证明的资源保留，不扫未知子树、不自动重试。取消若晚于已授权精确清理，部分临时文件可能已经关闭/删除，清理状态单独反馈，不把取消等同完整现场保留或FD已关闭。达到残留门槛会拒绝后续检查，不能以清理未知文件绕过 |
| UI/异步 | `BackupInspectionController/ViewModel/DocumentPicker`接系统SAF；状态及摘要无路径、URI或恢复能力。Controller每次新UUID，重建只保留中断布尔/Picker nonce；旧选择仅排空，不会重放URI。选择中换Owner仍占用busy至回调，检查中取消/超时不把请求结束等同FD关闭；旧结果不发布到新Owner。MainActivity→LithicApp→Nav→Settings已接线，检查/导出/恢复及照片修改互斥，原dirty/pending/恢复保护保留 |
| 照片信息 | 新`GenerationPhotoStorageReader`在同一coordinator快照、settings Owner检查及Room事务内做固定只读聚合SQL；COUNT/SUM、两类cover引用UNION及孤儿核对。含已结束/停止的现存业务行，项目/订阅共享一图只计一次；未被任何现存行引用才算历史留存。长度为整数合计估算，负值/缺引用/溢出/读取失败不伪装0；不查路径、不扫描照片文件 |
| 照片UI | Reader接既有Ready及generation UI scope，进入设置或明确重读时刷新；实例/代有效性与请求终结护栏拒绝迟到值。仅轻量分组显示4项事实，错误与loading不显示假0；明确登记长度不是实际磁盘占用、不含检查副本和外部备份、不等同照片已逐一认证。保留用户指定历史留存说明，无永久删除/GC按钮 |

主接线修改：`MainActivity.kt`、`LithicApp.kt`、`LithicNavigation.kt`、`LedgerUiState.kt`、`LedgerUiStateHolder.kt`、`SettingsShell.kt`；数据层仅最小typed分类及两个新只读facade。C9三主题、图标、P3照片Writer/Controller/DAO/PNG管线及所有旧测试原字节不改。

## 本轮验证的真实边界

- 开始主机RAM只读采样约4.416GiB；首次实际命令前降为**3.604GiB**，4GiB门禁在启动Gradle之前拦截。Python包装器exit1，**Gradle未启动、没有JUnit结果**。随后用户明确“先完成源码，构建暂缓”；不再尝试构建。见[资源阻塞回执](v1-b1-20261101/red-1/resource-blocked.json)。这不是业务测试FAIL，更不是观察到RED。
- 新增**55项JVM**断言源码：备份服务22、照片汇总10、备份Controller12、照片Controller11。新增**12项Android**断言源码：真实Room统计8、生产Compose呈现4。全部**未编译/NOT_RUN**；没有修改原P3断言或删除历史失败。真实SAF、Android Room/Owner换代、取消/重建/异常中断、资源峰值和视觉均未验证。
- 新host夹具静态发现并纠正了PNG CRC、固定fallback UUID及合法uppercase媒体SHA兼容问题；旧夹具及业务断言不改。这只是源码/样本检查，不能当作执行通过。所谓host“原账本不变”样本为可丢弃合成字节文件，不能替代真实Room与SAF证明。
- 已执行`git diff --check`、源blob及SHA/字节检查；**253份范围外main/schema原文件、全部旧JVM/Android测试、16份历史APK（含Preview14）保持**。Room5、backup4、Owner/generation、恢复Token/原子切换、金额/订阅/提醒、Manifest/权限、SDK/依赖、原签名及版本配置未改。[最终源码/保护回执](v1-b1-20261101/source-preparation.json)。当前Private Trial源配置仍Preview14/code30，**没有Preview15 APK、大小、SHA或新证书实物结果可交付**。

## 继续点与未关闭门禁

1. 用户明确恢复且新测RAM≥4GiB后，先跑上述55新JVM及受影响的原BackupValidator/Archive/Export/Restore Controller、P3备份与照片策略回归；不重跑无关历史套件。再编译AndroidTest源码、构建与Lint，保留一切真实失败。
2. 桌面门禁满足后，仅显式Trial递增Preview15/code31，构建并核对实物Manifest/DEX/原证书/源码绑定/16旧APK及新文件SHA，唯一复制到非build稳定交付目录；未获设备授权不安装。
3. 真机SAF检查与完整备份/恢复、覆盖升级、异常中断仍需独立授权和隔离数据；VERIFIED不能替代这些门禁。后续旧照片清理必须另审所有项目/订阅共享引用、当前Owner、备份快照占用、未确认写入与物理文件身份/独占资格、故障恢复及backup4完整性；本阶段只统计留存，不授予删除资格。

**V1-B1 SOURCE_PREPARED / BUILD_DEFERRED / PREVIEW15_NOT_BUILT。** 不登记正式实施或产品验收PASS。V1_RELEASE_NO_GO、STRICT_JPEG_IMPORT_BLOCKED、ANDROID_ISOLATION_RUNTIME_PENDING及旧M10/M20/M22/C3/NOT_RUN/UNKNOWN保持。无ADB、手机、AVD、安装、真实账本访问或恢复覆盖；只提交推送B1分支，停止等待继续验证授权，不合并main、不进入V1-B2。
