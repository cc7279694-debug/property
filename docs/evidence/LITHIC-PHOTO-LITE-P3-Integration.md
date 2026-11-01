# PHOTO-LITE P3 — 封面更换与取消显示

**最新：PHOTO-LITE P3 — READY_FOR_INDEPENDENT_REVIEW。** 用户恢复构建后已完成桌面验证及唯一Preview14归档，见末节；下方“构建暂缓／没有APK”为先前源码阶段的历史记录，原始证据不改写。Android、SAF及实际升级仍未运行。

2026-11-01；基线`3f04f181472d1db53c93a9a4b38e7c989198c39b`，从干净、远端一致的P2新建`codex/photo-lite-p3`；P1/P2/产品原分支保留。本报告集中记录计划、源码及后续真实验证，不分页面建立报告。

## 执行合同与最小计划

- 目标：已有物品封面更换、明确确认后取消显示，沿用现有刷新与受校验读取，保留backup4完整性；恢复原黑石金光背景质感并去外框。
- 范围：原PhotoLite接口/Writer、仅封面nullable整行CAS、Controller/Panel/详情与导航接线、定向测试及图标；Private Trial预设Preview14/code30。
- 不处理：订阅图片、多图、相机/裁剪/AI、原JPEG认证、媒体删除/GC、Room5/backup4迁移、Owner/generation/恢复协议、金额/订阅/通知规则、设备和个人账本。
- 实现选择：显式ADD/REPLACE/REMOVE意图；保留原ADD已有封面拒绝资格。替换仍先有界缩略→PNG认证/文件fsync→同Owner的withWrite/Room事务插入媒体＋整行CAS；取消只同一受保护边界CAS为null。两者保守拒绝旧Owner/旧行/重复/未知，不重试，不删除失败候选。
- 旧文件策略：全部旧metadata及文件保留，包括共享引用。现有Room快照读取**所有**media行，完整备份复制及校验全部登记媒体；withSnapshot覆盖数据捕获与媒体复制，与withWrite互斥。因此无需新增租约、按当前封面筛媒体或绕过备份校验。旧图缺失/损坏仍会明确阻断完整备份，不能宣称永久清除。

计划顺序：

1. 先补纯JVM意图/整行/单次资格及Android写入/交互断言源码，再修改原实现；不建新模拟框架。
2. 复用一个Controller状态机，取消显示先确认、不Picker/解码，保留pending/UNKNOWN/返回守卫。引用变化沿既有Room invalidation刷新及C6 ticket失效。
3. 补backup4更换/取消后留存媒体的原字节、关联、SHA/尺寸及缺失/损坏拒绝测试；不把host归档准入当SAF或真实恢复。
4. 同步旧图背景质感的launcher资源与桌面模拟，保持原图与P2图不改。只做原生资源接线，不新画另一种品牌。
5. 资源准入后执行定向JVM、AndroidTest编译、Private Trial构建/Lint、身份/签名/DEX/图标核验；全部通过才唯一归档Preview14，保护15旧APK。

## 资源和验证边界

本轮开始RAM3.919GiB，复核3.282GiB；用户明确选择**“先完成源码，构建暂缓”**。因此本轮继续完整源码/测试准备，**不得执行Gradle、JVM、AndroidTest编译、Lint或APK构建**。测试源码先写并不等于观察到RED/GREEN；未执行项如实NOT_RUN，不借静态检查宣布安全PASS。Preview14只有源配置，当前没有可交付APK。

真实Android/SAF恢复、覆盖升级、异常中断与native资源峰值均NOT_RUN或UNKNOWN；用户此前P1人工反馈不扩成这些安全证据。V1_RELEASE_NO_GO、严格JPEG历史阻断、C6隔离运行待验及所有历史状态保持。

## 源码准备结果

| 范围 | 本轮实现与证据边界 |
| --- | --- |
| 意图与写入 | `PhotoLiteCoverWriter.kt`增加明确意图、取消显示的独立结果及单次target保护；`GenerationPhotoLiteCoverWriter.kt`复用原文件认证与事务，不建第二个存储系统。原ADD仍拒绝已有封面；更换/取消都核对整行和原cover；不匹配操作、重复、旧Owner/旧行拒绝，无法确认结果保持UNKNOWN |
| DAO | 原`addPhotoLiteCoverIfUnchanged`和全部旧方法原样保留，新增nullable CAS；静态核对两条SQL完全一致，只更新`coverAssetId/updatedAt`，不改变名称、金额、日期及其他字段。没有schema、索引或迁移变更 |
| UI/导航 | 原Controller增加REMOVE_CONFIRM，共享SAVING、nonce、SavedState未确认标记、watchdog及返回守卫；取消显示不启动Picker、加载或编码。三入口复用原恢复/备份/dirty保护；真实Cleared才报告取消成功，图片Success还须匹配本次nonce；类型或身份不符不冒充成功。详情有图显示“更换照片／取消封面显示”，48dp并可换行；取消预览/确认前不提交 |
| 刷新 | 沿用ProjectRepository/ReportRepository/SearchRepository的Room invalidation及原VM重读；C6封面ticket含assetId/Owner，变更后换图或回图标。未改C6读取、持久缓存或Owner协议；动态列表/详情/返回/重开一致性仍待Android验证 |
| backup4 | 保留全部旧媒体行/文件，原捕获与复制在同一withSnapshot内；新withWrite操作与其互斥。更换后当前引用指向新图，取消后为null；旧图继续参加完整备份与原字节恢复。缺失/损坏的旧图即使不再显示，也不能被忽略以宣称完整备份成功。未登记失败候选按P1继续保留，不自动登记、重试或清扫 |
| JVM源码 | 新增7个意图/整行/单次资格方法；原3个format4集成方法保留，再补3个更换/取消后的双媒体归档与旧图缺失/坏图拒绝方法。新增10方法，均未编译/NOT_RUN；包括原相关回归的未来目标范围为56方法，不能作为本轮PASS计数 |
| Android源码 | 存储21、Controller21、呈现11、原Pipeline8，共61方法（P2原39＋本轮22）。覆盖更换/取消/重开/共享旧图/重复/Owner/异常/备份留存等；原ADD拒绝及核心断言保留，过时的“有图不开放操作”呈现断言按新授权改为新入口断言。**全部未编译、未运行** |
| 当前静态结果 | XML与资源引用、PNG头/CRC、原DAO保护及新SQL、冻结源码边界、方法清单、15旧APK大小/SHA和`git diff --check`通过。见[源码回执](photo-lite-p3-20261101/source-preparation.json)。这是源码/文件检查，不是Kotlin/Room编译或Android安全PASS |

### 旧图标背景回归

使用原`ic_launcher_art.png`编辑延展背景，保留黑石、金色侧光、地面暖光和细纹，移除烘焙内框和纯黑外matte。新`ic_launcher_scene_p3.png`为**1254×1254，RGB不透明完整场景，2,377,296字节**，SHA-256 `cf98e02ead3e72cdbf4902e0aeff991e28a74583921140029a3f9ac85254ba14`。这是编辑衍生，未宣称原图像素完全不变；原用户图和P2透明石头原字节保留。

前景用`16.666667%`补偿完整场景的静态108/72视口，背景为匹配的`#23241F`；两adaptive引用保持。[原图／Preview13／P3对照](../design/launcher-icon-p3-20261101/original-p2-p3-desktop-comparison.png)已实际打开，桌面模拟保留原背景气氛、未见旧嵌套框；[脚本](../design/launcher-icon-p3-20261101/render-preview.ps1)exit0，**不是手机截图**。未来APK审计必须核对新的不透明scene／16.666667%参数，不能照搬P2“透明切片／23%”断言；真实Launcher、启动画面、主题裁切和比例仍NOT_RUN。

## 保留NOTE与停止点

- 本轮**不进行照片文件或metadata删除**。共享引用及已导出的备份不被更改；旧照片累积会增加本地空间和备份体积，达到既有格式限制或旧图损坏时，完整备份仍可能失败。没有宣称永久清除或已具备回收资格。UI在更换、取消确认及成功处明确说明旧照片仍会随完整备份保留。
- 原Room5、backup4、金额/订阅、Owner/generation、恢复Token/原子切换、C6读取及提醒保持；不新增权限/依赖/native/网络或订阅照片功能。API仍恰好36且真实Picker可用，PNG仍<2MiB/640px。
- Preview14仅源配置为`com.cooper.lithic.preview.debug` / code30 / `0.1.0-preview.14-debug`；默认Debug/Release与原签名配置不改。**没有新APK、大小、SHA或实际签名结果可交付**。Preview13及其余14份旧实物保持，未操作用户安装包或账本。
- 用户恢复构建且RAM≥4GiB后，才继续本轮定向JVM、AndroidTest编译、Private Trial构建/Lint、Manifest/DEX/原证书/图标静态审计；全部门禁通过再唯一归档Preview14。普通Trial测试宿主不得执行要求`.photolite`＋M13 plain Application的合成存储方法；设备运行须另行授权。

当前停止：**PHOTO-LITE P3 — SOURCE_PREPARED / BUILD_DEFERRED / PREVIEW14_NOT_BUILT**。编译、Lint、Android/SAF、升级、异常中断及资源峰值仍未验证；V1_RELEASE_NO_GO、正式MEDIA_IMPORT_BLOCKED、STRICT_JPEG_IMPORT_BLOCKED、ANDROID_ISOLATION_RUNTIME_PENDING与全部历史FAIL/UNKNOWN/NOT_RUN保持。只提交推送P3分支，不合并产品分支/main、不安装或开始下一阶段。

## 2026-11-01 内存恢复后的桌面验证与Preview14

用户回复“内存已充足”后重新实测6.848GiB，允许恢复本轮桌面测试及构建。各Gradle命令再次检查≥4GiB；本次均offline、单worker、无clean、无依赖升级。源码准备提交`a564dee92f2095878c29e56543eaed00463c8f46`保留；最终构建源码为`179ac675c1dd1916da4877030641f307244bcc63`，App Git树`a1b509f567434e59c2b3fd18bea1ac0ac0199379`。后续交付文档提交不改变该App树。

| 实际检查 | 结果与证据 |
| --- | --- |
| 首轮JVM | 56项，55 PASS / 1 FAIL，exit1；新增“类别已变化”夹具误用了原类别值，原`matchesProject`正确接受未变化行。仅将夹具改为独立类别并增加“变体必须改变原行”自检，原拒绝断言未削弱，生产代码不改。[原始失败及XML](photo-lite-p3-20261101/desktop-1/test-1/result.json)保留 |
| 修正后定向JVM | 相同范围56项全部PASS，0失败/错误/跳过，exit0；包括意图/CAS前提、PNG边界、backup4双媒体留存与坏图拒绝、封面呈现策略及来源导航。[JUnit结果](photo-lite-p3-20261101/desktop-1/test-2/result.json) |
| 构建与AndroidTest源码 | Private Trial App、Test打包及Kotlin编译exit0，构建12分27秒；相关61项Android方法只编译，运行全部NOT_RUN。[原命令/退出码](photo-lite-p3-20261101/desktop-1/build-1/command.json) |
| Lint | exit0，0错误18警告，[完整XML](photo-lite-p3-20261101/desktop-1/lint/lint-results-debug.xml)保留。包含原资源及新不透明场景的`IconLauncherShape`启发式警告；新图由adaptive背景及实际16.666667%前景inset组合，实际资源与像素另行核对，未抑制警告。真实OEM裁切仍NOT_RUN |
| 构建身份 | 普通Debug仍`.debug`/code17/`0.1.0-m22-debug`，Release仍基础ID/code17/`0.1.0-m22`；二者PHOTO开关false。显式Trial为`.preview.debug`/code30/preview14，开关true，独立`app/build/private-trial`。默认只求值，未另造用户APK。[配置回执](photo-lite-p3-20261101/desktop-1/configuration.json)记录控制台混合编码首轮解析失败；改为从原字节唯一JSON行解析，无Gradle重跑或日志改写 |
| 实物Manifest/DEX/证书 | 正常LithicApplication→MainActivity业务入口、PHOTO开关及更换/取消Controller/Writer/Cleared类方法实际在APK；无JUnit或Compose测试宿主进入App。Provider/权限/native与Preview12/13保持，无INTERNET、广泛照片/存储权限或新JPEG native。原证书一致；[身份及原始命令](photo-lite-p3-20261101/desktop-1/artifact/identity.json) |
| 图标 | 新场景原字节及RGBA像素与APK一致，1254²全不透明；两个adaptive入口、16.666667%前景与#23241F矩形背景实物接线正确。[图标回执](photo-lite-p3-20261101/desktop-1/icon/result.json)。这是静态打包/桌面模拟证据，不是手机图标视觉PASS |
| 冻结与历史保护 | 除既定16个App路径外，250份原生产/schema源文件blob不变，原备份/恢复/Owner/C6读取/金额规则保持；[边界回执](photo-lite-p3-20261101/desktop-1/frozen-boundaries.json)。15个旧APK（Preview2～13、V1-A1两包及P1）大小/SHA均不变，无手机、ADB、模拟器、安装、数据读取或清理 |

### 唯一用户候选

- 文件：`E:\CODEX\.codex\worktrees\lithic-m13\砺石新APP\deliveries\lithic-preview-0.1.0-preview.14\Lithic-0.1.0-preview.14-debug.apk`。
- 身份：`com.cooper.lithic.preview.debug`，`0.1.0-preview.14-debug` / code30，**20,457,403字节**。
- SHA-256：`0994fa279ccbdc92c44e1ee963f7d404b113a4334f4194c1afd5def73ce764bf`。
- 原Debug证书SHA-256：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。
- 非Gradle目录首次创建，复制后与构建APK逐字节核对；APK不进Git。见[归档回执](photo-lite-p3-20261101/desktop-1/archive.json)。Test APK只是临时编译产物，不作第二份用户交付；其普通AndroidJUnitRunner并非M13 plain宿主，禁止直接执行要求`.photolite`隔离身份的合成存储方法。

### 尚未关闭的门禁与集中体验建议

- 可待用户另行选择后，在可丢弃物品上依次添加→更换→取消显示→返回列表→重新打开；取消选择/确认应保留原封面。**本轮没有执行这些手机步骤**，也未尝试覆盖升级。
- backup4纯JVM证明已登记双媒体的归档、校验/恢复准入及缺失/损坏拒绝，不能代替真实SAF导出、恢复激活、进程中断和资源峰值。上述Android行为均NOT_RUN/UNKNOWN；P1人工体验不扩充为P3安全PASS。
- 取消只清显示引用；旧媒体及metadata仍保留并参加完整备份。共享引用、已导出备份不变，旧图累积的空间/体积与坏图阻断完整备份NOTE保持，未实施永久删除/GC。
- 仅个人实验Trial开放API36及真实系统Picker路线，640px、PNG<2MiB保持；正式MEDIA_IMPORT_BLOCKED、STRICT_JPEG_IMPORT_BLOCKED、V1_RELEASE_NO_GO、ANDROID_ISOLATION_RUNTIME_PENDING以及全部历史FAIL/UNKNOWN/NOT_RUN不改判。

**PHOTO-LITE P3 — READY_FOR_INDEPENDENT_REVIEW / PHOTO_LITE_EXPERIMENT_ONLY。** 本轮桌面交付完成，生产Android视觉、实际升级、SAF恢复及资源证据仍待后续授权。只提交推送`codex/photo-lite-p3`，不自动合并产品分支/main、不进入P4/M23、不操作手机。
