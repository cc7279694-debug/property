# LITHIC V1-B1 — Data Confidence

2026-11-02；P3基线`dd31ce4b0613d0ed53e2a8280375739d75550032`，工作分支`codex/v1-b1-data-confidence`。当前为 **IMPLEMENTED / READY_FOR_INDEPENDENT_REVIEW**：桌面门禁及唯一Preview15候选已完成，Android实际仍NOT_RUN；不是独立功能/安全验收PASS。原源码准备及资源阻塞过程保留在下文，最新证据见末节。

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

## 源码准备阶段的历史验证边界

- 开始主机RAM只读采样约4.416GiB；首次实际命令前降为**3.604GiB**，4GiB门禁在启动Gradle之前拦截。Python包装器exit1，**Gradle未启动、没有JUnit结果**。随后用户明确“先完成源码，构建暂缓”；不再尝试构建。见[资源阻塞回执](v1-b1-20261101/red-1/resource-blocked.json)。这不是业务测试FAIL，更不是观察到RED。
- 新增**55项JVM**断言源码：备份服务22、照片汇总10、备份Controller12、照片Controller11。新增**12项Android**断言源码：真实Room统计8、生产Compose呈现4。全部**未编译/NOT_RUN**；没有修改原P3断言或删除历史失败。真实SAF、Android Room/Owner换代、取消/重建/异常中断、资源峰值和视觉均未验证。
- 新host夹具静态发现并纠正了PNG CRC、固定fallback UUID及合法uppercase媒体SHA兼容问题；旧夹具及业务断言不改。这只是源码/样本检查，不能当作执行通过。所谓host“原账本不变”样本为可丢弃合成字节文件，不能替代真实Room与SAF证明。
- 已执行`git diff --check`、源blob及SHA/字节检查；**253份范围外main/schema原文件、全部旧JVM/Android测试、16份历史APK（含Preview14）保持**。Room5、backup4、Owner/generation、恢复Token/原子切换、金额/订阅/提醒、Manifest/权限、SDK/依赖、原签名及版本配置未改。[最终源码/保护回执](v1-b1-20261101/source-preparation.json)。当前Private Trial源配置仍Preview14/code30，**没有Preview15 APK、大小、SHA或新证书实物结果可交付**。

## 源码阶段历史继续点与未关闭门禁

1. 用户明确恢复且新测RAM≥4GiB后，先跑上述55新JVM及受影响的原BackupValidator/Archive/Export/Restore Controller、P3备份与照片策略回归；不重跑无关历史套件。再编译AndroidTest源码、构建与Lint，保留一切真实失败。
2. 桌面门禁满足后，仅显式Trial递增Preview15/code31，构建并核对实物Manifest/DEX/原证书/源码绑定/16旧APK及新文件SHA，唯一复制到非build稳定交付目录；未获设备授权不安装。
3. 真机SAF检查与完整备份/恢复、覆盖升级、异常中断仍需独立授权和隔离数据；VERIFIED不能替代这些门禁。后续旧照片清理必须另审所有项目/订阅共享引用、当前Owner、备份快照占用、未确认写入与物理文件身份/独占资格、故障恢复及backup4完整性；本阶段只统计留存，不授予删除资格。

**V1-B1 SOURCE_PREPARED / BUILD_DEFERRED / PREVIEW15_NOT_BUILT。** 不登记正式实施或产品验收PASS。V1_RELEASE_NO_GO、STRICT_JPEG_IMPORT_BLOCKED、ANDROID_ISOLATION_RUNTIME_PENDING及旧M10/M20/M22/C3/NOT_RUN/UNKNOWN保持。无ADB、手机、AVD、安装、真实账本访问或恢复覆盖；只提交推送B1分支，停止等待继续验证授权，不合并main、不进入V1-B2。

## 2026-11-01 重新准入与阻塞（历史）

用户明确恢复B1定向测试/编译/Lint/Preview15核验。本次B1本地与远端均为`af5528b689cf8c034947ba259b1693bea396e6c8`且干净；重新核对16份历史APK大小/SHA未变，未修改产品或Private Trial版本。

- 初读RAM3.992GiB；独立采样曾到4.053368GiB，但正式命令启动前门禁实测**3.935GiB**，Python包装器exit1、**Gradle没有启动**。静态准备结束前复核降至**2.478428GiB**，继续保持资源阻塞，不以较早的瞬时样本作为启动依据。[本次门禁](v1-b1-20261101/desktop-1/test-1/resource-blocked.json)与[结束资源回执](v1-b1-20261101/desktop-1/final-resource-check.json)保留；不是JVM测试FAIL。
- 只准备本轮离线command receipt、Preview15/code31实物审计及P3原图标审计脚本；Python AST与PowerShell AST静态语法检查通过，**没有执行SDK或构建**。正式构建脚本额外记录启动时App树及App clean状态；将来产物审计必须实际核版本/Manifest/DEX/原证书、Preview14升级身份及16份旧APK，不能把这些脚本当作已核验结果。
- 用户已被告知需额外释放内存，尚未取得足够资源。本轮55新JVM、12新Android及相关回归仍NOT_RUN；编译/构建/Lint/真实SAF及设备运行全部NOT_RUN。没有Preview15目录/产物/SHA，原版本配置仍Preview14/code30，不生成替代包或签名。

当前：**V1-B1 DESKTOP VERIFICATION — RESOURCE_BLOCKED / PREVIEW15_NOT_BUILT**。原恢复授权继续限定桌面；只有再次实测≥4GiB且用户没有新的暂停要求时，才继续原定B1验证与唯一候选交付。不降低门槛、不操作手机、无main/V1-B2，V1_RELEASE_NO_GO与STRICT_JPEG_IMPORT_BLOCKED保持。

## 2026-11-02 桌面验证与Preview15交付（最新）

- 本次恢复点`d0e2d84b90f4577744fbb282dacee14b34f694cb`，本地/远端一致、工作区干净；初读RAM **5.870625GiB**，JVM正式门禁5.728096GiB、构建门禁5.497299GiB，均≥4。前两次资源阻塞不改判。
- **272唯一JVM方法全部PASS，0失败/0错误/0跳过**：55项B1新增＋217相关备份/格式兼容/取消/Owner退役、导出恢复Controller、P3照片备份/引用策略及导航回归；不是整仓历史测试。命令exit0，[JUnit原件与逐类计数](v1-b1-20261101/desktop-1/test-2/result.json)及同目录stdout/stderr保留。测试源码为d0e2d84；之后仅Trial版本两行变更，功能源码未再改变。
- Private Trial Debug App/Test构建、AndroidTest Kotlin源码编译、Lint命令 **exit0**；Lint **0错误18警告**，与P3相同的警告数量；原SDK XML版本提示、废弃API/冗余断言等编译警告未抹除。[构建真实回执](v1-b1-20261101/desktop-1/build-1/command.json)、[Lint XML及计数](v1-b1-20261101/desktop-1/lint/result.json)。12项新Android方法仅编译，真实Room SQL/SAF/换代/视觉/字体/取消时延/峰值/恢复与升级 **NOT_RUN或UNKNOWN**，不扩大为PASS。
- 唯一生产差异续接为显式Trial **30→31、preview.14→preview.15**。默认配置另取exit0回执：Debug仍`com.cooper.lithic.debug`/code17/`0.1.0-m22-debug`/PHOTO=false，Release仍`com.cooper.lithic`、原默认版本及PHOTO=false；未构建或交付Release。原M22/C3/C6/V1-A1身份配置未改。见[配置回执](v1-b1-20261101/desktop-1/configuration-default.json)和[冻结边界](v1-b1-20261101/desktop-1/frozen-boundaries.json)：253份范围外main/schema及281份原JVM/Android测试不变，Room5、backup4/旧兼容、Owner/恢复Token/原子切换、金额/订阅/提醒、C6读取和P3写入、主题/Manifest/权限/依赖均保持。
- 实际APK核对通过：正常`LithicApplication`/`MainActivity`、原Provider/Service及权限；无INTERNET/新增权限/native JPEG。B1检查Service、VM/Picker/设置及统计Reader/VM、原P3封面接线类和方法存在于DEX；测试类/框架不进入用户App。原图标scene1254×1254及像素/编码SHA、16.666667% adaptive前景和背景`#23241F`实际进入APK；Launcher真实观感仍NOT_RUN。[完整实物审计](v1-b1-20261101/desktop-1/artifact/identity.json)及[图标静态审计](v1-b1-20261101/desktop-1/icon/result.json)。
- 编译所得Test为`.preview.debug.test`，注册`AndroidJUnitRunner`；M13仅编入、**未注册plain Application替换**。本次绝无测试执行。未来合成Room方法须另备获授权的独立plain宿主，不能在用户Trial正常业务宿主直接执行。

| 唯一用户候选 | 实物结果 |
| --- | --- |
| 绝对路径 | `E:\CODEX\.codex\worktrees\lithic-m13\砺石新APP\deliveries\lithic-preview-0.1.0-preview.15\Lithic-0.1.0-preview.15-debug.apk` |
| 身份 | `com.cooper.lithic.preview.debug` / code31 / `0.1.0-preview.15-debug` |
| 大小 | **20,506,555字节** |
| SHA-256 | `ad9ac772a7df9d27ba8ad5c1cb801a29ca89fcf3d62849c07531536c0f7ca6a9` |
| Debug证书SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9` |
| 构建绑定 | 源提交`6697cecec0287fc32c1f885bb994c7ca5320cdc2`；App树`d69e8458f9eed83dd98f49defcf151c2cfa604ef`；构建开始App输入已提交且clean，归档时再次一致 |

[独占创建/复制/重新哈希与历史保护回执](v1-b1-20261101/desktop-1/archive.json)：仅一个非build候选，复制逐字节一致；APK不入Git、无ZIP。**16份历史APK均保持**，其中Preview14仍20,457,403字节、SHA`0994fa279ccbdc92c44e1ee963f7d404b113a4334f4194c1afd5def73ce764bf`；未覆盖或删除任何旧包。后续文档收尾提交不改变上述App树。

### 未关闭门禁与交接

- 可后续自愿检查：设置→检查备份文件（选择可丢弃备份、核结果和取消，不点恢复）；照片存储4项元数据及历史留存说明。**本轮不要求用户执行，也未安装或覆盖Preview14**。真实SAF、完整备份恢复、异常中断/重开、覆盖升级及资源峰值仍需独立授权和证据，VERIFIED不能替代它们。
- 已知限制：现有检查规则保留JPEG容器证据，不认证JPEG熵或Android可解码；三次未认证残留会话可使后续检查保守拒绝，临时文件身份/收口未知不自动清理。照片数量/登记长度不是实际磁盘占用或逐文件认证。
- 旧照片删除仍须专项证明所有物品/订阅引用、备份快照占用、Owner资格、文件身份、未确认写入及异常恢复；本阶段不提供永久删除或GC。全部M10 JPEG FAIL、M20/M22/C3及既有NOT_RUN/UNKNOWN保持。
- 当前 **V1-B1 DATA CONFIDENCE — READY_FOR_INDEPENDENT_REVIEW**，仅桌面实现与候选交付；**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING**保持。设备/ADB/AVD/真实账本/恢复覆盖操作0；停止等ChatGPT独立审查，不自动V1-B2、合并main或发行。
