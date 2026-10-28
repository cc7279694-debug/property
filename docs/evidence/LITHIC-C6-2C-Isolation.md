# C6-2C 第一阶段：隔离进程与FD生命周期

2026-10-28；`codex/lithic-v1-product-completion`；起点`9f84c42a955a1042fdd924c1be1ac650a5f847b4`。用户转交C6-2B PASS WITH NOTES / CONDITIONAL GO，**仅批准第一阶段**。

**C6-2C ISOLATION — READY_FOR_INDEPENDENT_REVIEW**

**ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED**。已实现固定合成字节的隔离基础通道，未接JPEG/native、图片Picker、业务写入或生产用户流程；编译/JVM不等于真实隔离安全通过。

## 实现范围

- [AppProcessRole](../../app/src/main/java/com/cooper/lithic/app/AppProcessRole.kt)及[LithicApplication](../../app/src/main/java/com/cooper/lithic/app/LithicApplication.kt)：精确MAIN/SANDBOX/UNKNOWN，API28+平台进程名、API26仅本进程256字节有界读取；在lazy AppContainer和WorkManager配置getter前检查角色。SANDBOX/UNKNOWN不创建/启动Owner、Room、DataStore、提醒；MAIN继续原启动顺序。MainActivity未改，UNKNOWN无法识别时fail closed，实际系统识别仍待验证。
- [Manifest](../../app/src/main/AndroidManifest.xml)：显式非导出`isolatedProcess=true`、`:media_sandbox` Service；AndroidX Startup固定主进程。最终合并Manifest和APK均核对，Startup只剩原ProcessLifecycle/ProfileInstaller元数据，未恢复WorkManager自动初始化；没有新增权限或INTERNET。
- [MediaSandboxService / Protocol](../../app/src/main/java/com/cooper/lithic/data/media/isolation/MediaSandboxService.kt)：单次版本化Messenger，认证平台`Message.sendingUid`，不用消息自报UID；Service还要求真实独立UID和SANDBOX角色。只接本次输入/空输出/Guard三个FD，校验regular、UID、单链接、dev/ino、长度及版本/nonce；仅处理固定约几十字节合成内容，无Bitmap、真实图片、URI/业务路径或数据库对象。`HOLD_UNTIL_RETIRE`只用latch模拟不返回，不是native取消实验。
- [MediaIsolationChannel](../../app/src/main/java/com/cooper/lithic/data/media/isolation/MediaIsolationChannel.kt)：未被UI、Repository、提醒或默认启动调用。Host只读打开合成输入，Worker实际尝试写入应被拒绝；输出是本次独占空文件。Guard采用**可写通道**，Host用独立`Os.open(O_NOFOLLOW/O_NONBLOCK/CLOSE_ON_EXEC)`后核同一dev/ino，确认锁曾被Worker持有再COMPUTE；不拿dup通道或磁盘标记充当归还。
- [IsolationSessionState](../../app/src/main/java/com/cooper/lithic/data/media/isolation/IsolationSessionState.kt)：正常候选退休与不可逆REVOKED分开，拒错误UID/nonce/版本、重复与迟到结果。Binder死亡、Guard归还、Worker输入/输出自报关闭、Host FD关闭、RPC/解绑确认分别记录；仅完整组合可记`PROBE_CLOSED`，**任何结果都不授予媒体写入资格**。不按PID、超时、stopSelf或onDestroy推定安全释放。
- [IsolationProbeFiles](../../app/src/main/java/com/cooper/lithic/data/media/isolation/IsolationProbeFiles.kt)：只写`noBackupFilesDir/c6-isolation-stage1/<UUID>`合成资源；同步ADMITTED后才启动消费者。控制记录固定小文件，结果丢失/未知/旧stage或关闭记录均不自动授权第二次probe；进程级quarantine同时拦截晚连接、解绑不明与旧RPC。日志落盘失败整体UNKNOWN，保留物理关闭证据。所有合成现场保留，无扫描清理、重试、旧目录访问。

Service只在框架通知最后绑定释放后终止**自身隔离进程**，不kill其他PID；Host解绑在前，避免正常退出时主动触发AUTO_CREATE重建。意外重绑定会拒绝复用、Host封锁；watchdog与系统通知的真实时序、SELinux FD许可、进程是否实际退出及不重建仍**NOT_RUN**。若拿不到关闭证明，UNKNOWN并封锁后续；不宣称优雅Owner关闭。

## 实际桌面结果

| 项目 | 实际结果与边界 |
|---|---|
| 资源准入 | 初始3.19GiB，未启动重构建；用户释放后5.559GiB，最终构建前5.412GiB。未关闭他人进程。 |
| 新JVM | **25唯一方法最终25 PASS**（启动角色8＋状态/恢复17）。首轮夹具类型遮蔽导致编译FAIL，修调用、不改断言；随后真实RED为18 FAIL/7 PASS；实现后GREEN。两次GREEN不累计为50项覆盖。 |
| Debug / AndroidTest编译 | 最终组合命令exit0，实际assembleDebug及compileDebugAndroidTestKotlin完成。新增Android **8方法全部NOT_RUN**＝启动4＋通道4；未来需M13 plain Application预启动隔离，不能以@Before代替。 |
| Lint | 首轮3错误15警告（O_CLOEXEC Java字段API27、fcntlInt API30）；沿用已有Linux CLOEXEC常量并按SDK守卫查询，不加NewApi抑制、不降minSdk。最终**0错误15警告，exit0**，原SDK XML版本警告保持。API26 FD权限最终还靠实际拒写/成功输出/可写独占锁断言验证，未运行。 |
| APK/Manifest/签名 | 普通桌面Debug `com.cooper.lithic.debug` / code17；正常LithicApplication/MainActivity、原证书`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；无JPEG/native新库，只有原AndroidX native依赖。不是用户体验包，不生成/复制交付APK，不改Preview版本/身份。 |

[最终命令/退出码](c6-isolation-20261028-113818/final-check/command.json)、[stdout](c6-isolation-20261028-113818/final-check/stdout)、[stderr](c6-isolation-20261028-113818/final-check/stderr)、[角色JVM逐方法](c6-isolation-20261028-113818/final-check/TEST-com.cooper.lithic.app.AppProcessRoleTest.xml)、[会话JVM逐方法](c6-isolation-20261028-113818/final-check/TEST-com.cooper.lithic.data.media.isolation.IsolationSessionStateTest.xml)、[最终Lint XML](c6-isolation-20261028-113818/final-check/lint.xml)、[真实RED原始结果](c6-isolation-20261028-113818/red-assertions/command.json)、[RED stdout](c6-isolation-20261028-113818/red-assertions/stdout)、[首轮编译FAIL](c6-isolation-20261028-113818/red/stderr)、[首次Lint失败](c6-isolation-20261028-113818/green-build/lint.xml)、[身份与Manifest核对](c6-isolation-20261028-113818/identity/summary.json)、[实际APK Manifest](c6-isolation-20261028-113818/identity/manifest.stdout)、[签名原始结果](c6-isolation-20261028-113818/identity/signature.stdout)、[最终源码指纹](c6-isolation-20261028-113818/final-source-sha256.json)。APK SHA/大小仅记身份JSON，无APK/密钥入Git。源码指纹为实际编译工作区字节；[Git换行规范化映射](c6-isolation-20261028-113818/source-git-map.json)逐文件确认暂存内容仅有CRLF/LF差异。

## JPEG 3.2.0 → 3.2.1 版本建议

**建议下一native候选改审3.2.1，不再将3.2.0直接定为最终生产锁；本轮没有引入任何版本，仍须独立批准。** [官方发布](https://github.com/libjpeg-turbo/libjpeg-turbo/releases/tag/3.2.1)时间2026-10-27 20:58:25 UTC（北京时间10-28 04:58），tag提交`7b4993786d856cbccabfc0ed2f923ba000e326cc`。

| 上游修复 | 本项目调用范围判断 |
|---|---|
| Release #7：特制4:2:0、非fancy merged上采样、禁用SIMD并窄crop时越界/崩溃 | **禁用SIMD不能规避这项修复**。旧严格核心是全尺寸逐行，无crop调用，不能声称当前路径已证实命中；后续适配不宜继续忽略已公开修复。[修复源码](https://github.com/libjpeg-turbo/libjpeg-turbo/commit/2646fa33f8d67373c27b558b47e77b2d8d517e31) |
| skip_scanlines错误、零宽sample/block数组防护、RGB565转换越界 | 涉libjpeg API及内存管理；旧核心不skip、不RGB565、每次读一行，直接触发未证实。底层变化仍要求重新测内存、失败释放及薄适配。 |
| CVE-2026-75466等TurboJPEG indexed-PNG加载/transform修复 | 属未采用的TurboJPEG路径，不把该CVE直接归因到旧严格libjpeg API；不因此顺便开启TurboJPEG、crop、RGB565、工具或SIMD。 |
| PIC静态构建、zlib/libspng符号及RVV优化 | 后续显式PIC；未采用的工具/TurboJPEG/SIMD路径不应进入产品。不是本轮升级依赖的理由。 |

[官方完整比较](https://github.com/libjpeg-turbo/libjpeg-turbo/compare/3.2.0...3.2.1)。旧核心只调用create/read_header/start/read_scanlines/finish/destroy，不能把“库版本新了”当原三个缺损JPEG业务FAIL已解决。

官方[LICENSE.md](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/7b4993786d856cbccabfc0ed2f923ba000e326cc/LICENSE.md)/[README.ijg](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/7b4993786d856cbccabfc0ed2f923ba000e326cc/README.ijg)与3.2.0同字节（各SHA见[来源元数据](c6-isolation-20261028-113818/jpeg-version-review.json)）；IJG/BSD及组件许可、修改标识/分发致谢继续。官方tar发布元数据2,549,854字节，API公布digest `c27ad72a68da151177929aa969ae773a2da5ac2af9ce946079314fdac8cece7f`；**未下载、自算归档SHA或验证GPG**，该值不是本地实物校验结果，不用GitHub自动生成Source code包代替官方原包。

若后续批准3.2.1，须重新执行：原同字节baseline/合法progressive/锁定缺损、原三项完整业务断言；三种完整40MP（baseline420、progressive420/444）的分配账本/全链RSS/PSS/主进程增量/耗时及取消异常释放；新版本+新FD/JNI适配同候选的API26/36、arm64/x86_64及真实手机。旧host240,265,637字节、旧设备九项结果不直接沿用为新版PASS。

## 下一次隔离设备验证前提

源码、8项Android断言和桌面构建已具备**申请审查的基础**，尚不具备直接操作现有手机的准入。后续须先独立审查App/Test身份及M13 Runner；普通Debug与既有M10冲突，不能拿本轮APK覆盖安装。只有新授权的独立身份/全新或明确许可的隔离环境，才可执行一次有界验证；不重开C3，不启动旧ANR AVD。

必须实际确认：隔离进程从Application启动即无默认generation/Owner/提醒；UID与sender真实性、SELinux FD/文件锁、只读输入、实际Binder死亡/guard归还/无自动重建；正常/取消/超时/Host丢状态和未知封锁。任何启动或跨UID能力异常立即停止后续依赖项，不提升权限、不自动重试或清理。

本轮无ADB、手机/模拟器、native执行、个人媒体/账本访问；未接新增/替换/移除/CAS。Room5、backup4、C6-1读取、Owner/generation、恢复Token/指针、金额/订阅/提醒规则及原签名/版本配置不变。M10三JPEG FAIL、14项C6-1 Android NOT_RUN、40MP Android全链资源UNKNOWN、native取消/异常恢复NOT_RUN，以及M20/M22/C3/M13-T06/G1～G4全部历史状态保持。提交推送后停止，不进入native整合、媒体写入、main合并或M23。
