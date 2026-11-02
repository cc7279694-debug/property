# LITHIC V1-B1-R1 — 备份检查结果分类补修

2026-11-02；基线`8f731ec6c122574222f93323016893d299d4d7a4`，分支`codex/v1-b1-data-confidence`，开始时本地/远端一致且工作区干净。

**当前：V1-B1-R1 FIX — READY_FOR_INDEPENDENT_REVIEW（仅桌面）。** 168项定向JVM、编译检查及Lint已完成；Preview15不重包且不含R1修复，Android运行仍NOT_RUN。最新执行与原失败见末节。

**源码准备阶段历史：SOURCE_PREPARED / BUILD_DEFERRED。** 用户在本轮实测RAM3.087GiB（随后1.838GiB）后明确“先完成源码，构建暂缓”。Gradle/SDK执行0，JVM、Kotlin/AndroidTest编译及Lint全部NOT_RUN，没有观察到RED/GREEN；不能据静态审查登记修复验证PASS或READY。

## 根因与最小差异

原`BackupInspectionService.inspect()`的大try同时包含提供者打开/读取/关闭及本地内容校验，外层按异常类型将提供者的IllegalArgumentException、ZipException、CharacterCodingException误认INVALID。

仅修改`BackupInspectionService.copySelected()`的异常边界：

- 完整包住输入打开、复制、私有输出flush/close及提供者close；获取阶段任意Exception转为既有InspectionUnavailable，外层保守返回UNKNOWN。
- CancellationException仍原样重抛，原checkpoint、busy、长度上限及try/use关闭顺序保持。
- 必须复制及关闭成功后，才读取本地文件身份、创建payload并进入BackupValidator/媒体校验。原结构/SHA/CRC/关联及版本异常分类未改，预期仍INVALID或UNSUPPORTED；成功仍VERIFIED。
- 不改目录/UUID/marker、文件身份、已认证精确清理、未知残留、Owner或取消保护；没有恢复/业务/媒体写入能力。

生产差异仅8行新增、1行签名替换；另新增9个JVM案例。Room5、backup4、恢复Token/Owner/generation、P3照片、C6、冻结金额/订阅、UI、版本/身份/签名/依赖及全部其他App文件不变。

## 源码阶段的回归及历史验证边界

新增“打开／读取／关闭 × IllegalArgumentException／ZipException／CharacterCodingException”9项，每项全新合成fixture，避免原3个残留上限干扰。预期UNKNOWN、summary空、一次获取、保留本轮残留、不进入payload；关闭失败样本为完整合法ZIP已读到EOF。并核对源ZIP、合成账本字节文件和合成媒体原字节不变。这不是Android Room/SAF或真实账本证据。

原22项服务测试及所有原断言保留，含合法VERIFIED、真实坏ZIP INVALID、formatVersion不支持UNSUPPORTED、SHA/CRC/关联错误、取消、Owner切换和未知文件保留；服务共31方法源码。新增9项和旧回归本轮均NOT_RUN，未编译；B1此前272 JVM/Lint/构建结果是历史证据，不能替代R1新执行。

本轮仅执行`git diff --check`、源差异/旧测试保留及文件SHA静态核对。[静态范围与17份APK保护回执](v1-b1-r1-20261102/source-checks.json)：除服务及该测试类外App输入全部不变，原校验分类/复制操作/归属与清理函数保持。子审查亦仅源码检查，不是ChatGPT独立验收或编译PASS。

## 源码阶段的旧产物与历史继续点

Preview15仍 **20,506,555字节**、SHA`ad9ac772a7df9d27ba8ad5c1cb801a29ca89fcf3d62849c07531536c0f7ca6a9`，另16份历史APK全部原字节保持。**Preview15不含本次R1修复**；未修改版本、覆盖、重新打包或生成任何APK。原B1报告和全部历史结果保留。

用户恢复构建且实时RAM≥4GiB后，先执行服务共31项（新增9＋原22）及受影响`data.backup.*`/BackupInspectionController定向回归，再做必要Kotlin/AndroidTest源码编译与Lint；不调用assemble/package/clean，不重包Preview15。当前未取得修复前RED证据，不将源码对照冒充执行结果。实际执行前不得宣称任何上述结果通过。

本次提交推送源码准备及本补充后停止；设备/ADB/模拟器/私人数据操作0，不进入后续模块。**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING**及所有历史FAIL/UNKNOWN/NOT_RUN保持，独立复审仍待测试证据补齐。

## 2026-11-02 恢复执行与桌面结果（最新）

从`d543b8a396d38c2e870aab36f81070a044b2202b`恢复，本地/远端一致、App输入干净。初读RAM5.363GiB；首次JVM正式门禁5.440GiB、重跑5.422GiB、编译/Lint5.753GiB，均≥4。只用原离线JDK/SDK，不升级依赖或工具链。

| 检查 | 实际结果 |
| --- | --- |
| 首次JVM命令 | **编译FAIL、JUnit执行0**。新测试漏导入`java.util.zip.ZipException`，3处unresolved；生产Kotlin已编译通过。[原始失败回执及日志](v1-b1-r1-20261102/desktop-1/test-1/result.json)完整保留，不称业务RED，不读取旧B1 XML凑PASS |
| 唯一源码补正 | 仅新增1行上述import，提交`b4302fa133b6a8fca1e8c4bc38773eeff7eb18ac`；没有修改断言、生产逻辑或其他模块 |
| 相同范围JVM重跑 | **168唯一方法PASS，0失败/0错误/0跳过，19份新XML**：服务31（新9＋原22）、其他直接相关备份回归125、InspectionController12。覆盖打开/读/关闭×3异常UNKNOWN、真实坏ZIP/SHA/CRC/关联INVALID、不支持版本UNSUPPORTED、合法VERIFIED、取消/旧Owner及合成原文件保护。[本次逐项XML/计数](v1-b1-r1-20261102/desktop-1/test-2/result.json)；时间戳/哈希与命令起止核对，未复用旧B1结果 |
| 源码编译与Lint | 命令exit0；生产Kotlin已实际编译，AndroidTest源码编译检查exit0（**UP-TO-DATE复用**）。Lint三个分析任务实际执行，**0错误18警告**，报告内容相同而UP-TO-DATE复用；原SDK XML/废弃API/冗余断言等警告保留。[编译/Lint命令](v1-b1-r1-20261102/desktop-1/compile-lint-1/command.json)、[Lint XML及任务状态](v1-b1-r1-20261102/desktop-1/lint/result.json) |
| 范围及旧APK | **17归档APK＋2现有构建输出APK全部原字节**。任务图无APK assemble/package/bundle、安装或设备任务；资源编译/JAR生成不属于用户APK打包。版本、签名及全部配置未改，没有生成新APK。[最终源边界、任务图和字节保护回执](v1-b1-r1-20261102/desktop-1/final-checks.json) |

本次验证源码为`b4302fa`，App树`c07e6e4f47e54e31ebadc59f825cc17867194926`，执行开始App输入已提交且clean。相对原B1基线仍仅服务与其测试类变化；相对源码准备点仅1行import。Room5/backup4、Token/Owner/generation、原校验及归属/清理、P3照片、冻结金额/订阅和其他App文件不变。后续文档收尾不改变该App树。

**Preview15仍为原20,506,555字节、原SHA及原证书，仍不包含R1补修。** 本轮仅验证源码，不交付新包。源码阶段的资源阻塞/NOT_RUN与首轮编译失败均保留；没有修复前动态RED证据，不将编译失败当作业务RED。

分类与文件不变证据仅限host合成输入/字节夹具，真实Android SAF/Room、Provider关闭及资源峰值仍NOT_RUN或UNKNOWN；原12项B1 Android与全部旧运行门禁不改。无手机/ADB/模拟器/个人账本/覆盖恢复操作。**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING**保持；当前可提交ChatGPT独立复审，不自行冻结、重包Preview15、生成其他候选或进入下一模块。
