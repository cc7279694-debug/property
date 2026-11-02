# LITHIC V1-B1-R2 — Preview16最终集成交付

2026-11-02；基线`08e11f208b2584fea3fa244bb4feb0034e2b89d2`，分支`codex/v1-b1-data-confidence`。用户转交 **R1 ACCEPTED / FROZEN — PASS WITH NOTES（仅桌面）**。当前 **PREVIEW16 — READY_FOR_INDEPENDENT_REVIEW**；不是Android或正式发行通过。

## 唯一候选

| 项目 | 实际结果 |
| --- | --- |
| 绝对路径 | `E:\CODEX\.codex\worktrees\lithic-m13\砺石新APP\deliveries\lithic-preview-0.1.0-preview.16\Lithic-0.1.0-preview.16-debug.apk` |
| 包名 | `com.cooper.lithic.preview.debug` |
| 版本 | **code32 / 0.1.0-preview.16-debug** |
| 大小 | **20,506,555字节** |
| SHA-256 | `e8b2e2fe7b2db3af986bc9818f1a02a9da583ba160b723e63befc7f4a1eb9e94` |
| 原Debug证书SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9` |
| 构建源提交 / App树 | `d91069e54d4c040b19a0b013c0b08b7293b4e96e` / `b3eda991b828619317873401696a03ee4367774a` |

独占创建非build交付目录，复制后重新计算SHA并逐字节比对通过；APK/密钥不入Git、无ZIP、无新Test APK。[唯一归档与汇合门禁回执](v1-b1-r2-20261102/desktop-1/archive.json)。Preview15与新包恰好同字节数，但SHA不同；R1修复已进入新包，不能以长度辨认版本。

## 构建及继承证据

- 开始HEAD/远端均为已审基线且工作区干净。源差异严格只有显式Trial的**31→32、preview.15→preview.16**；其余App/测试/Room5/backup4/Owner/generation/Token/原子切换、P3照片及R1分类、冻结业务/三主题/图标/权限/依赖均未改。[源码边界](v1-b1-r2-20261102/desktop-1/source-checks.json)。
- 复核并继承[R1已接受证据](LITHIC-V1-B1-R1-Classification.md)：19份XML及日志哈希吻合，**168 JVM PASS（含9新增）、Lint0E18W**。本轮功能源码和断言未变，**JVM/Lint/AndroidTest未重跑**，不冒称新执行。
- 唯一App实际构建`--offline -PlithicPrivateTrial=true :app:assembleDebug`及配置回执任务，**exit0**，4分32秒；命令前RAM **4.611526GiB≥4**。原JDK/SDK与单worker/in-process Kotlin沿用，无依赖下载或工具链升级。[真实命令及原始日志](v1-b1-r2-20261102/desktop-1/build-3/command.json)。
- 普通默认身份另做配置求值exit0：Debug仍`com.cooper.lithic.debug`/code17/`0.1.0-m22-debug`、PHOTO=false；Release仍`com.cooper.lithic`、原默认版本/PHOTO=false。其他实验身份/互斥分支源码逐字不改，未构建这些包。[配置回执](v1-b1-r2-20261102/desktop-1/configuration.json)。

## 实际APK静态核验

- 完整Manifest/签名核对：正常`LithicApplication`、唯一Launcher `MainActivity`、原Provider authorities与Service/export状态；min26/target36，原权限集合不变、无INTERNET/广泛存储/新增权限；原证书有效。无Instrumentation、M13Runner、Compose测试宿主或JUnit/Android测试框架进入用户APK；native内容与Preview15/14/12相同，无JPEG native。[实际身份/Manifest/DEX原始证据](v1-b1-r2-20261102/desktop-1/artifact-2/identity.json)。
- P3封面添加/更换/取消显示、PNG管线、受保护Reader及详情/列表接线；B1检查Service/SAF/VM/设置、照片统计Reader/VM；真实三主题enum/AppearanceProvider/选择入口均存在于实际DEX，PHOTO实验开关实物true。不是仅核Gradle文本；静态接线不替代Android体验或资源安全。
- **R1方法体已核对**：新`copySelected`实际异常表覆盖provider打开、读取、写入/flush及正常/异常关闭；取消路径经共享标签重抛原对象，普通Exception创建`InspectionUnavailable`。实际`inspect$3.invokeSuspend`只在获取正常返回后调用Validator，且去除调试行号/局部变量信息后的外层439行与Preview15完全一致，原INVALID/UNSUPPORTED/UNKNOWN内容分类保留。新旧反汇编与SHA及实施侧逐段范围复核记录见[R1静态字节码证据](v1-b1-r2-20261102/desktop-1/r1-dex-review.json)。审计脚本保留R1_REVIEW_REQUIRED，归档同时要求该实施侧字节码复核、身份与图标三门禁满足；不冒称ChatGPT独立验收。
- 原黑石金光scene **1254×1254**、编码SHA`cf98e02ead3e72cdbf4902e0aeff991e28a74583921140029a3f9ac85254ba14`与像素逐字一致；adaptive前景16.666667%、背景`#23241F`及正常/圆形图标均进入实物。[图标静态审计](v1-b1-r2-20261102/desktop-1/icon/result.json)。真实Launcher视觉NOT_RUN。
- **17份归档旧APK全部原字节保持**，Preview15仍20,506,555字节、SHA`ad9ac772a7df9d27ba8ad5c1cb801a29ca89fcf3d62849c07531536c0f7ca6a9`；未覆盖或删除任何旧包。

## 真实失败与修正（全部保留）

| 阶段 | 原结果及处理 |
| --- | --- |
| 构建包装器首次启动 | Windows默认编码读UTF-8脚本使中文常量失真，原路径assert在Gradle前拒绝。保存[原脚本与失败原因](v1-b1-r2-20261102/desktop-1/build-1/result.json)；仅改显式UTF-8读取，原路径assert未放宽 |
| 第一次正式资源门禁 | RAM3.741GiB<4，Gradle未启动，[原回执](v1-b1-r2-20261102/desktop-1/build-2/resource-blocked.json)保留；完成静态准备后新采样4.683GiB，正式4.612GiB通过，才进行上述唯一App构建 |
| 首轮R1反汇编自动识别 | [原失败](v1-b1-r2-20261102/desktop-1/artifact/failure.txt)为核验器在D8共享标签处截断handler，误报Missing cancellation rethrow candidate。原脚本/失败/反汇编完整保留；增加有界fallthrough/goto跟踪，循环/未知跳转/分支/超预算拒绝，原语义断言不删不弱化。[解析修正回执](v1-b1-r2-20261102/desktop-1/artifact/handler-parser-fix.json)。同一APK重新静态核验通过，前后smali SHA相同，**没有修改产品或重建APK** |

## 未运行与停止边界

Android方法、真实SAF检查/完整恢复、覆盖升级/异常中断、照片资源峰值、系统字体/IME/TalkBack、视觉及C6隔离均NOT_RUN或UNKNOWN；P3 API36实验边界和所有M10 JPEG/M20/M22/C3/G1～G4历史门禁保持。用户试用不替代自动化安全验证。设备/ADB/模拟器/私人记录/真实恢复操作0，不自动安装、V1-B2/main/M23或发行。

**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING**保持。提交推送当前B1分支，工作区与远端核对后停止，等待ChatGPT独立交付审查。
