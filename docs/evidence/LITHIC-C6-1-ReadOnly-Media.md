# C6-1 已登记封面只读接入

2026-10-28；分支`codex/lithic-v1-product-completion`；基线`17329eda1210e0496c90d381899414a433303e01`。用户转交C6-0独立审查PASS WITH NOTES，仅有条件实施只读路径。

**C6-1 READ-ONLY MEDIA — READY_FOR_INDEPENDENT_REVIEW**

**MEDIA_IMPORT_BLOCKED / ANDROID_VISUAL_PENDING**。代码与桌面门禁完成，不是生产媒体安全或Android视觉验收通过。

## 实施与真实生产接线

- [CoverReader契约](../../app/src/main/java/com/cooper/lithic/data/media/CoverReader.kt)和[GenerationCoverReader](../../app/src/main/java/com/cooper/lithic/data/media/GenerationCoverReader.kt)：绑定当前Owner实例，公开不透明ownerKey而非Owner/路径/FD。在同代协调器下先检查settings Owner及数据库开放状态，再核对记录类型/ID、当前coverAssetId、元数据。目录/UID/普通单链接文件、NOFOLLOW/NONBLOCK/CLOEXEC、FD/路径前后身份、有限读取、长度/SHA、PNG/JPEG容器/尺寸共同校验；拒绝陈旧引用、退代和关闭Owner，不通过旧DAO重开库。
- PNG/JPEG检查器增加ByteArray只读入口，避免显示时写临时文件或按裸路径再次解码。原File入口保留；PNG解析体除return标签、zlib逻辑及JPEG解析主体静态比对保持，见[边界核对](c6-readonly-20261028-083718/source-boundaries.json)。**登记JPEG的容器/摘要/可读性不认证严格熵流完整性**，三个新导入历史FAIL不关闭。
- [CoverPreviews](../../app/src/main/java/com/cooper/lithic/feature/media/CoverPreviews.kt)与[策略/资源账本](../../app/src/main/java/com/cooper/lithic/feature/media/CoverPreviewPolicy.kt)：仅解码读取器Ready字节，在后台核对解码器边界/MIME后按实际槽像素采样，单目标每边最多1024px；全进程单并发覆盖读取、解码及失败收口，源文件严格小于2MiB且每边不超过1600px。16MiB仅计费活跃/在途像素负载，按实际allocationByteCount复核，额度不足立即拒绝；没有全局/磁盘图片缓存或无界预取。
- 请求绑定reader/Owner身份、generation、记录类型/ID、资产和尺寸；变化先清旧图，取消/迟到不发布。原生解码返回前不提前释放全局slot；未发布Bitmap和租约在finally收口，已展示Bitmap不强制recycle，以免Canvas仍在使用。**这些边界不证明native可立即取消、GC/GPU驻留、最坏时延或进程总峰值**，实际仍UNKNOWN。
- `LedgerUiState/Holder`在原Owner作用域注入，`LithicNavigation`包住实际生产Scene；首页两种预览、项目/订阅当前和历史四列表、两详情共8处传真实记录引用。复用EntryCard和项目详情已有图片槽，订阅详情补纯展示槽。无引用使用原类型图标、零媒体读取；有引用但不可读显示“封面暂不可用”语义的错误按钮，点击只读说明可见完整短文，成功后不残留错误段落。没有假照片、Picker、媒体写入、替换/移除或GC入口。

## 实际验证

| 项目 | 结果与范围 |
|---|---|
| 定向JVM | **44 PASS，0失败/错误/跳过**：12新增（字节检查4、采样/预算/票据6、管线取消2）＋32相关回归。含原PNG检查/兼容、JPEG、媒体预算、导航和项目列表；不重跑M22或M10历史套件。 |
| Debug构建 | 首轮exit0；普通Debug用于编译验证，不是新的用户体验包。 |
| AndroidTest源码编译 | 首轮及最终精确等待修订后均exit0；新增14方法＝真实Room/文件7＋Compose显示5＋8处页面接线2。**实际执行全部NOT_RUN**，不能用纯JVM或编译代替Android效果。 |
| Lint | 两轮exit0，**0错误21警告**；原报告内容复用，原始stdout保留。SDK XML解析版本提示及旧Bundle.get弃用警告保留，不升级工具。 |
| 身份及保护边界 | 实际普通APK为`com.cooper.lithic.debug`／code17，正常LithicApplication/MainActivity，原Debug证书SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，无INTERNET。Private Trial配置和Preview版本不变，没有生成或交付新版体验APK。 |

页面接线测试初稿只等待IO请求出现，静态复核发现这不足以证明错误节点已发布；仅补终态节点数量waitUntil，原身份/数量断言及5秒预算保留。随后只重新编译AndroidTest并执行相关Lint，**未重新运行44项JVM，未制造或宣称Android RED→GREEN**。数据层Android合成Owner关闭后检查DB状态，失败/未知现场保留；未来实际执行仍需另行授权及启动前M13/plain Application隔离，不进入现有个人包或账本。

原始证据：[44项逐方法结果](c6-readonly-20261028-083718/jvm-summary.json)、[XML](c6-readonly-20261028-083718/jvm-results/)、[首轮命令/退出码](c6-readonly-20261028-083718/desktop-check/command.json)、[stdout](c6-readonly-20261028-083718/desktop-check/stdout)、[stderr](c6-readonly-20261028-083718/desktop-check/stderr)、[最终编译/Lint](c6-readonly-20261028-083718/android-source-final/command.json)、[最终stdout](c6-readonly-20261028-083718/android-source-final/stdout)、[Lint XML](c6-readonly-20261028-083718/lint-final.xml)、[汇总](c6-readonly-20261028-083718/verification-summary.json)。[测试等待修订](c6-readonly-20261028-083718/android-wait-refinement.json)、[最终21个源文件工作区原字节指纹](c6-readonly-20261028-083718/final-source-sha256.json)和[普通Debug身份](c6-readonly-20261028-083718/debug-identity-check.json)用于核对输入与证据，不代表安装/升级通过。

## 未运行与停止边界

无ADB、手机、模拟器、Instrumentation或私人照片/账本操作；Android真实Room、文件UID/链接/恢复换代、实际图片显示、触控、大字、原生取消/资源及视觉均**NOT_RUN / PENDING**。14方法是隔离测试源码，不是生产MainActivity实际运行证据；JVM“native-like阻塞”仅证明协程slot语义，不是原生codec实验。

生产改动限15个文件：新增读取/显示/资源策略4，原PNG/JPEG入口适配2，现有UI装配与8处展示接线涉及9个文件；另新增6个测试文件。Room5、backup4、金额/日期/订阅/以旧换新规则、DAO/Repository写入、Owner/coordinator、恢复Token/原子切换、导航守卫、依赖/Manifest/包名/版本和签名配置不变。备份恢复仍保留原媒体字节及全部登记资源，不自动修图、清引用或回收文件。

完整导入/替换/移除与M10资源门禁继续BLOCKED；M10三JPEG FAIL、M20平台FAIL/UNKNOWN、M22、C3、M13-T06及G1～G4全部历史状态保持。提交推送后停止等待ChatGPT独立审查，不自动进入C6-2、不合并main、不进入M23。
