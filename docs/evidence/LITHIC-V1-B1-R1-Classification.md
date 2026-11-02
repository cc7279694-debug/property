# LITHIC V1-B1-R1 — 备份检查结果分类补修

2026-11-02；基线`8f731ec6c122574222f93323016893d299d4d7a4`，分支`codex/v1-b1-data-confidence`，开始时本地/远端一致且工作区干净。

**SOURCE_PREPARED / BUILD_DEFERRED。** 用户在本轮实测RAM3.087GiB（随后1.838GiB）后明确“先完成源码，构建暂缓”。Gradle/SDK执行0，JVM、Kotlin/AndroidTest编译及Lint全部NOT_RUN，没有观察到RED/GREEN；不能据静态审查登记修复验证PASS或READY。

## 根因与最小差异

原`BackupInspectionService.inspect()`的大try同时包含提供者打开/读取/关闭及本地内容校验，外层按异常类型将提供者的IllegalArgumentException、ZipException、CharacterCodingException误认INVALID。

仅修改`BackupInspectionService.copySelected()`的异常边界：

- 完整包住输入打开、复制、私有输出flush/close及提供者close；获取阶段任意Exception转为既有InspectionUnavailable，外层保守返回UNKNOWN。
- CancellationException仍原样重抛，原checkpoint、busy、长度上限及try/use关闭顺序保持。
- 必须复制及关闭成功后，才读取本地文件身份、创建payload并进入BackupValidator/媒体校验。原结构/SHA/CRC/关联及版本异常分类未改，预期仍INVALID或UNSUPPORTED；成功仍VERIFIED。
- 不改目录/UUID/marker、文件身份、已认证精确清理、未知残留、Owner或取消保护；没有恢复/业务/媒体写入能力。

生产差异仅8行新增、1行签名替换；另新增9个JVM案例。Room5、backup4、恢复Token/Owner/generation、P3照片、C6、冻结金额/订阅、UI、版本/身份/签名/依赖及全部其他App文件不变。

## 回归源码与实际验证边界

新增“打开／读取／关闭 × IllegalArgumentException／ZipException／CharacterCodingException”9项，每项全新合成fixture，避免原3个残留上限干扰。预期UNKNOWN、summary空、一次获取、保留本轮残留、不进入payload；关闭失败样本为完整合法ZIP已读到EOF。并核对源ZIP、合成账本字节文件和合成媒体原字节不变。这不是Android Room/SAF或真实账本证据。

原22项服务测试及所有原断言保留，含合法VERIFIED、真实坏ZIP INVALID、formatVersion不支持UNSUPPORTED、SHA/CRC/关联错误、取消、Owner切换和未知文件保留；服务共31方法源码。新增9项和旧回归本轮均NOT_RUN，未编译；B1此前272 JVM/Lint/构建结果是历史证据，不能替代R1新执行。

本轮仅执行`git diff --check`、源差异/旧测试保留及文件SHA静态核对。[静态范围与17份APK保护回执](v1-b1-r1-20261102/source-checks.json)：除服务及该测试类外App输入全部不变，原校验分类/复制操作/归属与清理函数保持。子审查亦仅源码检查，不是ChatGPT独立验收或编译PASS。

## 旧产物与后续继续点

Preview15仍 **20,506,555字节**、SHA`ad9ac772a7df9d27ba8ad5c1cb801a29ca89fcf3d62849c07531536c0f7ca6a9`，另16份历史APK全部原字节保持。**Preview15不含本次R1修复**；未修改版本、覆盖、重新打包或生成任何APK。原B1报告和全部历史结果保留。

用户恢复构建且实时RAM≥4GiB后，先执行服务共31项（新增9＋原22）及受影响`data.backup.*`/BackupInspectionController定向回归，再做必要Kotlin/AndroidTest源码编译与Lint；不调用assemble/package/clean，不重包Preview15。当前未取得修复前RED证据，不将源码对照冒充执行结果。实际执行前不得宣称任何上述结果通过。

本次提交推送源码准备及本补充后停止；设备/ADB/模拟器/私人数据操作0，不进入后续模块。**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING**及所有历史FAIL/UNKNOWN/NOT_RUN保持，独立复审仍待测试证据补齐。
