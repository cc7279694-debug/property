# M04 实际交付报告与验证证据

开始2026-10-11，完整验证2026-10-12（Asia/Shanghai）。**READY_FOR_REVIEW — M04**；独立验收PENDING，不自行ACCEPTED/FROZEN。起点 `ffd066ffe0efb6c00f35526ae792b50829d3230e`，来源feat/m03-subscription-engine；实际检查本地/远端相同且干净后创建feat/m04-room-storage-foundation。附件来源SHA、范围和先行Q06设计见[契约](../planning/M04_CONTRACT.md)。

## 1. 实际实现与数据层

Room schemaVersion=1，KSP2实际导出[1.json](../../app/schemas/com.cooper.lithic.data.local.LithicDatabase/1.json)：4表/30字段/7索引（3唯一）/3外键，10,840字节，SHA-256 `351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b`。CategoryDao、ProjectDao、MediaDao均ABORT；DatabaseMigrations仅空注册入口，无伪造v1→v2或破坏性fallback。

| 业务表 | 保存事实与实际约束 |
|---|---|
| categories | UUID/type/name/normalizedName/isFallback；type+normalizedName唯一，两个类型“其他”ID不同 |
| long_term_projects | 名称/类别/开始结束日期/结束原因/可选封面/备注/Instant；类别及封面RESTRICT外键，关联/日期查询索引 |
| project_money_events | UUID/项目/方向/Long分/真实日期/说明/role/operationId；项目RESTRICT外键、operationId唯一、项目日期索引 |
| local_media_assets | UUID/私有media相对路径/mime/尺寸/长度/sha256；路径唯一，资源metadata构造校验 |

LocalDate→Long epochDay；Instant ISO文本保留纳秒；Money复用M02范围0～999999999999分，OUTLAY/RECOVERY分开，收回可推导负净成本。不保存日均/天数/独立购买价，不建订阅/替换/通知表。SQL负责外键/唯一，Entity/Repository负责UUID、规范化、metadata及业务校验；没有SQL CHECK声明。

ProjectWriteRepository使用一个LocalWriteCoordinator Mutex与Room事务：新项目和初始事件同一次提交；第二步事件失败完整回滚；完全相同操作ID及内容重试返回原结果，异内容返回Conflict；Money/日期/关联/项目类别类型在事务内校验，不用IGNORE。LedgerReadRepository在真实读取事务中读取四表，独立SQLite写入的并发测试验证快照一致。设置/文件仅共享协调边界，不能宣称跨存储原子事务。

## 2. Settings、generation、启动与Q06

持久结构：`noBackupFilesDir/active-generation`（AtomicFile）及 `generations/<UUID>/{data.db, media/, settings.preferences_pb, ready.json}`；WAL/SHM/journal、设置tmp与ready辅助文件同代。ready为formatVersion/generationId，pointer为formatVersion/activeId/previousId/stage；内部版本1不是M11备份format版本，本轮仅STABLE、previousId=null。

完整数据库/默认类别/设置/media/ready准备后才激活。重开按指针身份验证路径、ready、数据库和设置；已有证据但指针缺失/异常、未知阶段、库损坏/异库、设置损坏都明确失败，保留原数据和旧代。既存库先只读校验实际SQL版本/Room身份再可写打开，支持提交WAL；只读和Room腐败回调不删除原文件，不自动清空或按目录mtime猜代。

SettingsStore持久record_zone_id，后续设备变化不覆盖；取得canonical文件lease后才观察存在性，DataStore scope取消并join后释放。取消返回时清理本次新获得的资源，取消重复open不关闭既有handle。Application.onCreate捕获zone并后台启动单任务；AppContainer保存Initializing/Ready/Failed，读取等待同任务，失败原因保留并传播，不重试造新代。正式错误页面留M05，M01占位UI保持。

Q06保持ENGINEERING_REQUIRED。本轮只设计与初始数据集实测；M12仍负责完整备份解析、新代真实业务导入、previousId/PENDING_VALIDATION流程、验证后切换/回退、切换前后杀进程、图片设置共同校验与旧代安全清理。Q06-V01～V03完整流程仍NOT_RUN。

## 3. 实际测试与原六案例

完整命令实际退出0（2m52s）：

```powershell
$env:ANDROID_SERIAL = 'emulator-5596'
.\gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug --max-workers=2 --console=plain
```

JVM **74/74 PASS**，原Money7、Clock2、Project22、Subscription37、累计6的方法/源码保留。Instrumentation **86/86 PASS**：App6、Room34、metadata9、Settings13、generation24；0失败/错误/跳过，源码方法集合与XML完全一致，仅砺石自己的API36/Android16 AVD。未操作手机或其他AVD。

| ID | 实际结果 | 关键证明 |
|---|---|---|
| M04-T01 | PASS | 真实SQLite拒绝孤立事件 |
| M04-T02 | PASS | operationId真实唯一索引拒绝重复 |
| M04-T03 | PASS | 项目INSERT后事件主键冲突，真实事务回滚 |
| M04-T04 | PASS | DataStore持久zone、关闭重开及设备zone变化不覆盖 |
| M04-T05 | PASS | 同代完整准备后激活/重开/旧代保留/异常不新空库 |
| M04-T06 | PASS | 两类型“其他”不同ID，同类唯一，事务内拒绝跨类型 |

真实RED→GREEN记录：61项/55断言失败（53 AssertionError、2 ComparisonFailure）；补充20/19；生命周期3/3；启动/旧库9/8。已有行为通过直接保留；SQLite真实页面腐败和取消repeat-open属保护回归，不故意改坏代码造RED。两次夹具编译错误（异类arrayOf推断、RoomDatabase非Closeable）单独保存，不算RED。后续加入边界并修正夹具警告/关闭方式，不能宣称所有测试文件四轮哈希相同；App6断言GREEN未改。

边界实际覆盖：精确幂等/异内容/并发、初始中途失败、外键/类别类型、0元/负净成本/越界、日期/纳秒往返、重开持久、四表读取事务、媒体metadata、设置腐败/缺失/非法值/单owner/并发与取消、pointer/ready辅助与非法阶段/部分初始化/旧代、已有v0/foreignv1不原地修改/真实页面腐败保留/提交WAL预检。没有用JVM代替Android存储实测。

## 4. 工具链、Lint、APK与离线设备

保留Gradle8.14.3、AGP8.13.2、Kotlin2.3.10、Temurin17.0.20.1+1、ComposeBOM2025.11.01、Nav3 1.0.1、SDK26/36/36、BuildTools36.0.0。新增Room2.8.5、KSP2.3.6、DataStore1.2.1、Coroutines1.10.2、AndroidXTest1.7.0/extJUnit1.3.0，官方来源/POM/实际依赖与传递差异见[工具链锁](../TOOLCHAIN_LOCK.md)和[JSON](M04-verification.json)。实际解析选中module坐标（含平台约束/variant）96→121，25新增/5提升/0移除；原工具链锁定值不变，不谎称全部运行依赖未变。

Lint **0 errors / 13 warnings**，均版本提示（1 OldTargetApi、2 AndroidGradlePluginVersion、5 GradleDependency、5 NewerVersionAvailable）；新增Coroutines提示，未抑制或顺便升级。KSP代码生成、JVM/native构建及assembleDebug成功。

APK绝对路径：`E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`。

- 11,809,696字节；SHA-256 `6a9f67a6fb84da44bdca1198a85afe1c1ca5aecc80042fa328bf7f50d3a00a10`。
- com.cooper.lithic.debug，versionCode2、0.1.0-m04-debug，APK/合并Manifest只含原signature级内部接收器权限；无网络/广泛存储/通知/精确闹钟/相机照片权限，备份九域排除及Emoji自动字体初始化移除检查PASS。
- 本地Debug证书，apksigner退出0/v2 true/一个signer；Wrapper官方hash、实际生产存储类存在、JUnit/Hamcrest/runner/测试类不打包检查PASS，不上传APK或私钥。
- 专用emulator-5596实际安装Success，安装base.apk SHA与本地最终包相同。断网default network=none/airplane1/wifi0/mobile_data0，两次force-stop后COLD/Status ok；重启pointer/ready同UUID，设置hash相同，当前进程无崩溃。UI截图/XML及视觉检查保留三句M01占位文案；网络原状态已恢复。未使用个人账本。

## 5. 未执行、风险与下一模块前置

本轮实际覆盖未发现未解决确定性缺陷；不保证所有平台/断电场景。真机、API26、OEM备份、真实v1→v2迁移、M10图片、M11完整备份format、M12完整切换/回退/杀进程/共同校验/清理及正式用户错误界面均NOT_RUN。TypeScript/ESLint不适用原生工程；NO-SOURCE不计PASS。

Q03～Q05原期限、M09 STOPPED再启用与多期预付默认停止风险不变。原ZIP CRC/安全路径/43原件hash核对PASS，阅读版/原130案例/16组完整补测及M00～M03历史保留，原案例仍全NOT_RUN。共享Mutex不能替代跨文件原子事务；metadata校验不等于M10真实图像验证；SQL CHECK未实施，不作既有能力声明。

M05需要M01/M04独立验收与用户新授权；继承schema v1、初始化状态/失败传播、记录时区、Money和真正空数据，不复制公式。本轮立即停止，不进入M05。M08还须扩展已有库可接受版本并显式迁移；M11/M12继续承担完整备份和恢复。

## 6. 文件与Git交付

只提交/推送feat/m04-room-storage-foundation。提交自身SHA、push及最后远端比对以Git日志和最终报告为准；不合并main/force push/PR/Release，不修改WHOCARE，不提交实际SQLite/DataStore、APK、密钥或个人数据。完整54输入（工作字节与Git规范化hash）、86+74逐项结果、四轮RED和最终日志/XML/产物hash见[JSON](M04-verification.json)。原始日志、截图与安装APK只保留在忽略的build/m04-evidence，不上传二进制/临时文件。

下方清单与最终Git差异对应；删除文件为0。文档差异/链接/来源/敏感信息检查结果登记在JSON的documentAndScopeChecks。

实际检查PASS：41项变更（26新增/15修改/0删除）、97项未修改基线文件比对、71份Markdown/350本地链接/3处当前文档章节引用有效、代码围栏平衡、54构建输入工作与Git规范化哈希一致；41项文本敏感模式0命中，无新增二进制/实际数据产物。Q03～Q05三行和M09两项风险正文与基线一致。Git差异空白检查通过；原43原件/ZIP/阅读版/130案例/16组完整补测及历史证据保持。

新增 26，修改 15，删除0。

### 新增

- [app/schemas/com.cooper.lithic.data.local.LithicDatabase/1.json](../../app/schemas/com.cooper.lithic.data.local.LithicDatabase/1.json)
- [app/src/androidTest/java/com/cooper/lithic/app/AppContainerTest.kt](../../app/src/androidTest/java/com/cooper/lithic/app/AppContainerTest.kt)
- [app/src/androidTest/java/com/cooper/lithic/data/backup/DataGenerationStoreTest.kt](../../app/src/androidTest/java/com/cooper/lithic/data/backup/DataGenerationStoreTest.kt)
- [app/src/androidTest/java/com/cooper/lithic/data/local/MetadataValidationTest.kt](../../app/src/androidTest/java/com/cooper/lithic/data/local/MetadataValidationTest.kt)
- [app/src/androidTest/java/com/cooper/lithic/data/local/ProjectDatabaseTest.kt](../../app/src/androidTest/java/com/cooper/lithic/data/local/ProjectDatabaseTest.kt)
- [app/src/androidTest/java/com/cooper/lithic/data/settings/PausedReturnDispatcher.kt](../../app/src/androidTest/java/com/cooper/lithic/data/settings/PausedReturnDispatcher.kt)
- [app/src/androidTest/java/com/cooper/lithic/data/settings/SettingsStoreTest.kt](../../app/src/androidTest/java/com/cooper/lithic/data/settings/SettingsStoreTest.kt)
- [app/src/main/java/com/cooper/lithic/app/AppContainer.kt](../../app/src/main/java/com/cooper/lithic/app/AppContainer.kt)
- [app/src/main/java/com/cooper/lithic/data/backup/DataGenerationStore.kt](../../app/src/main/java/com/cooper/lithic/data/backup/DataGenerationStore.kt)
- [app/src/main/java/com/cooper/lithic/data/local/CategoryDao.kt](../../app/src/main/java/com/cooper/lithic/data/local/CategoryDao.kt)
- [app/src/main/java/com/cooper/lithic/data/local/CategoryNames.kt](../../app/src/main/java/com/cooper/lithic/data/local/CategoryNames.kt)
- [app/src/main/java/com/cooper/lithic/data/local/DatabaseConverters.kt](../../app/src/main/java/com/cooper/lithic/data/local/DatabaseConverters.kt)
- [app/src/main/java/com/cooper/lithic/data/local/DatabaseMigrations.kt](../../app/src/main/java/com/cooper/lithic/data/local/DatabaseMigrations.kt)
- [app/src/main/java/com/cooper/lithic/data/local/LithicDatabase.kt](../../app/src/main/java/com/cooper/lithic/data/local/LithicDatabase.kt)
- [app/src/main/java/com/cooper/lithic/data/local/MediaDao.kt](../../app/src/main/java/com/cooper/lithic/data/local/MediaDao.kt)
- [app/src/main/java/com/cooper/lithic/data/local/ProjectDao.kt](../../app/src/main/java/com/cooper/lithic/data/local/ProjectDao.kt)
- [app/src/main/java/com/cooper/lithic/data/local/ProjectEntities.kt](../../app/src/main/java/com/cooper/lithic/data/local/ProjectEntities.kt)
- [app/src/main/java/com/cooper/lithic/data/local/StoredValueValidation.kt](../../app/src/main/java/com/cooper/lithic/data/local/StoredValueValidation.kt)
- [app/src/main/java/com/cooper/lithic/data/repository/LedgerReadRepository.kt](../../app/src/main/java/com/cooper/lithic/data/repository/LedgerReadRepository.kt)
- [app/src/main/java/com/cooper/lithic/data/repository/LocalWriteCoordinator.kt](../../app/src/main/java/com/cooper/lithic/data/repository/LocalWriteCoordinator.kt)
- [app/src/main/java/com/cooper/lithic/data/repository/ProjectWriteRepository.kt](../../app/src/main/java/com/cooper/lithic/data/repository/ProjectWriteRepository.kt)
- [app/src/main/java/com/cooper/lithic/data/settings/SettingsStore.kt](../../app/src/main/java/com/cooper/lithic/data/settings/SettingsStore.kt)
- [docs/checkpoints/2026-10-12-M04.md](../../docs/checkpoints/2026-10-12-M04.md)
- [docs/evidence/M04-verification.json](../../docs/evidence/M04-verification.json)
- [docs/evidence/M04-verification.md](../../docs/evidence/M04-verification.md)
- [docs/planning/M04_CONTRACT.md](../../docs/planning/M04_CONTRACT.md)

### 修改

- [AGENTS.md](../../AGENTS.md)
- [README.md](../../README.md)
- [app/build.gradle.kts](../../app/build.gradle.kts)
- [app/src/main/java/com/cooper/lithic/app/LithicApplication.kt](../../app/src/main/java/com/cooper/lithic/app/LithicApplication.kt)
- [build.gradle.kts](../../build.gradle.kts)
- [docs/ACCEPTANCE_MATRIX.md](../../docs/ACCEPTANCE_MATRIX.md)
- [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md)
- [docs/CURRENT_STATE.md](../../docs/CURRENT_STATE.md)
- [docs/DECISIONS.md](../../docs/DECISIONS.md)
- [docs/EXECUTION_PROTOCOL.md](../../docs/EXECUTION_PROTOCOL.md)
- [docs/ROADMAP.md](../../docs/ROADMAP.md)
- [docs/SPEC_ISSUES.md](../../docs/SPEC_ISSUES.md)
- [docs/TOOLCHAIN_LOCK.md](../../docs/TOOLCHAIN_LOCK.md)
- [gradle/libs.versions.toml](../../gradle/libs.versions.toml)
- [scripts/verify_m01_apk.py](../../scripts/verify_m01_apk.py)
