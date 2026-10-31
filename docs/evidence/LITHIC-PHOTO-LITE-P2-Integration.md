# PHOTO-LITE P2 — 主体验版集成

> **后续图标专项授权（2026-10-31）**：下文“原图标和inset保持”是源码准备提交`e67fc6b`时的历史检查，原始回执不改。用户随后要求去黑边和清晰适配，现仅启动图标资源更新为透明前景／全铺满背景，原始上传图和旧APK保持；见[图标适配报告](LITHIC-Launcher-Icon-Adaptation.md)。P2照片／Gradle／版本源码未追加变更，构建仍暂缓，Preview13未构建。

2026-10-31；P1来源`64c9337f7b05d82d8b7ea78d71ffb1c07cfa88fb`；产品分支来源`b23d0f2aa968cb2c5eda6359b499da1b7fad5a74`。两条分支本地/实时远端各自一致，产品提交为P1祖先（0左/3右）；从产品基线建立`codex/photo-lite-p2`并fast-forward承接P1，无reset、cherry-pick或分叉覆盖。P1和产品原分支均未修改，不合并main。

## 本轮契约

- 目标：复用P1既有单物品PhotoPicker→预览/确认/取消→PNG认证→Owner保护登记→原受校验封面显示，接入新的Private Trial Debug候选。
- 只改最小构建开关/体验版本、定向测试源码及本报告/状态；不重写照片、媒体、数据库、备份或UI系统。
- 继续API36+真实Picker、PNG<2MiB/最长640px；不存原JPEG，不新增订阅图片、多图、相机、裁剪、替换、移除或GC。
- Room5、backup4、金额/订阅、Owner/generation、恢复Token/原子切换与C6读取不变；所有未知/迟到/失败保护直接继承P1。
- 后续唯一候选预设：`com.cooper.lithic.preview.debug` / code29 / `0.1.0-preview.13-debug`，原Debug证书。只构建归档，不自动安装或升级手机。

用户反馈“已测试新版本，没问题，可以并入主版本”，登记为**USER_REPORTED_P1_FUNCTIONAL_OK**。没有逐项Android自动化或SAF备份恢复记录，不能据此把P1的38个Android NOT_RUN、真实恢复/异常关闭/资源峰值UNKNOWN或严格JPEG历史FAIL改判。

## 最小实现与验证计划

1. 保留P1的独立flag/包名/版本、全部处理和提交代码。仅在Private Trial的**Debug** BuildConfig启用同一照片功能；默认及Release保持关闭，防止试用开关误用于正式Release。Private Trial App仍是正常LithicApplication/MainActivity。
2. Private Trial版本仅28→29、preview.12→preview.13；普通Debug/Release与历史验证身份不变。保留原图标原字节及adaptive inset。
3. 复用PNG/backup4真实归档与校验链准备定向JVM断言；补充真实generation恢复后的封面关联/长度/SHA/像素与只读显示Android断言。不得把归档验证当恢复激活，更不能当SAF真机PASS。
4. 用户恢复构建且实测至少4GiB可用RAM后，执行必要定向JVM、Private Trial Debug构建、AndroidTest源码编译/Lint、参数/最终Manifest/DEX/签名/版本/SHA及14份旧APK保护核验；所有设备运行另授权。
5. 只有门禁真实通过才生成并独占归档`deliveries/lithic-preview-0.1.0-preview.13/Lithic-0.1.0-preview.13-debug.apk`。目录未知冲突时停止，不覆盖。APK/密钥不入Git。

## 资源与当前证据边界

开始实测可用RAM **3.030GiB**；用户明确选择“先完成源码，构建暂缓”。本阶段不得运行Gradle/JVM/Android编译/Lint，**Preview13尚未构建，无新APK/SHA/签名实物结论**。P1的43 JVM、构建和签名结果只是原P1证据，不作为P2当前通过结果。

原14份稳定APK（11个Preview历史＋V1-A1两包＋P1独立App）已逐个核对实际大小/SHA保持；Preview13交付目录检查时不存在。两来源提交的全部`app/src/main/res`和主Manifest无差异，启动图标art blob `ad3cf58d438e8edffbf0b3644d7e53d486303ca4`、前景blob `fd0158b18f9eafd185c9ad4369bba0608fa76d09`保持（16.666667% inset）。正式V1仍NO-GO，严格JPEG与尚未实际运行的隔离/媒体/备份恢复门禁保留。

## 源码准备结果

| 范围 | 本轮差异及真实验证状态 |
| --- | --- |
| 产品接线 | 继承全部P1源代码，仅`app/build.gradle.kts`体验版本两行及Debug专属功能字段覆盖；现有Holder、Pipeline、Controller、详情/列表和Owner提交路径逐字保持，无新图片处理或存储系统 |
| 功能开关 | `debug`合并字段为`photoLiteExperiment || lithicPrivateTrial`，默认字段仍为`photoLiteExperiment`。因此仅预期的Private Trial Debug与P1 Debug开放；普通Debug/Release关闭；显式Trial Release也不开放。参数互斥、原签名配置、正常Application不变。**这是源码核对，Gradle求值/DEX仍未运行** |
| JVM新增3方法 | `PhotoLiteBackup4IntegrationTest.kt`使用真实PhotoLitePng与format4 staging/manifest/archive verify/validator，核对封面关联、长度/SHA/尺寸、原字节与负净投入相关真实金额事实；缺失/坏图保持原SHA时拒绝完整归档/恢复前校验。没有伪造Room写入、Android解码或已激活恢复；**未编译/NOT_RUN** |
| Android新增1方法 | `GenerationPhotoLiteCoverWriterTest`原10方法及业务断言保留，追加导出/校验→修改合成资料→prepareRestore/populate→activateRestore→C6读取→关闭重开，核对cover关联/媒体元数据/长度/SHA/尺寸/合成像素/金额及旧Owner拒绝。固定恢复时钟，继承真实提醒导入保护，不改生产协调器。**未编译/NOT_RUN** |
| 失败卫生 | 恢复失败可能产生未返回的回滚Owner。测试仅尽力关闭已知Owner、保留原失败和关闭异常；只有完整成功且确认已知DB关闭才登记关闭/受控清理，不对失败/未知现场发清理资格 |
| 原验证保护 | P1原14 JVM与38 Android方法没有被删除或降低断言；本次照片相关源码共17个JVM、39个Android方法，但**P2全部未执行**，不重用P143 PASS充当新结果。真实Picker/SAF/手机升级与备份恢复均NOT_RUN |
| 静态检查 | `git diff --check` exit0；相对P1的`app/src/main`及schema差异为空。14旧APK实际字节/SHA及图标Git对象保持，见[源码保护回执](photo-lite-p2-20261031/source-preparation.json)；XML经Git正常换行过滤核对，不把CRLF与LF误判为图标变更 |
| 后续身份探针 | 已准备[Gradle只读配置任务](photo-lite-p2-20261031/identity.init.gradle)，读取Debug和Release各自合并字段；未执行，不复用P1只读defaultConfig的旧结果冒称Debug已开放 |
| 构建/交付 | **JVM、AndroidTest编译、构建、Lint、P13实物Manifest/签名/DEX、升级均NOT_RUN；APK_NOT_BUILT**。没有新APK绝对路径、大小或SHA可交付，未改任何既有实物 |

未来执行Private Trial的`assembleDebug / compileDebugAndroidTestKotlin / lintDebug`只编译Android测试；不得运行其普通App测试宿主来执行合成存储用例。新增Android方法仍只能在另行审查/授权的独立环境、M13 plain Application及UUID根下执行；现有P1已安装数据也不被当作可清理夹具。

本次停止于 **PHOTO-LITE P2 — SOURCE_PREPARED / BUILD_DEFERRED / PREVIEW13_NOT_BUILT**。用户P1手机反馈仅支持限定个人功能体验，未关闭真实backup4恢复、SAF、升级、异常中断或资源峰值门禁。源码已可供审查；待用户恢复桌面构建后继续原P2，不自行进入P3、设备操作或正式发行。`STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING / V1_RELEASE_NO_GO`保持。
