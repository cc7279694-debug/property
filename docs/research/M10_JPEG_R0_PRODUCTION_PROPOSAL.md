# M10-R0 — 最小生产修复提案与风险评估

日期：2026-10-16。**PROPOSED / NOT_APPROVED / NOT_IMPLEMENTED**。本文件不授权安装工具、增加JNI依赖、改变App构建/组件或修改任何生产代码。技术验证目前[BLOCKED](M10_JPEG_R0_FEASIBILITY.md)，[实验矩阵](M10_JPEG_R0_TEST_MATRIX.md)中的native门槛尚未满足。

## 1. 最小接入位置

```text
系统单图选择器
  → 原ContentResolver/同代owner
  → 原64MiB有界私有source.bin（完整写入、fsync、受控归属）
  → 原容器/MIME/40MP与格式校验
  → 仅JPEG：固定版本严格预检，真实消费压缩数据
  → 原BitmapFactory采样、EXIF方向、最长边1600px、2MiB有限重试
  → 原输出容器/尺寸/长度/SHA校验
  → 原PreparedImage / 文件落实 / 完整业务基线CAS
  → 原Room资源元数据与封面引用事务
```

PNG分支与清理协议完全继承，不经JPEG预检、不增加编码/数据格式。预检读取已落盘的同一个受控私有输入，不重新读取外部URI、不创建另一份业务存储，不绕过owner/coordinator。成功通过严格门禁才允许进入原规范化。

具体接点：[AndroidImageCodec.normalize](../../app/src/main/java/com/cooper/lithic/data/media/AndroidImageCodec.kt)现第54～61行，inspect和MIME核对之后、首次BitmapFactory decode之前。来源已由[LocalImageStore.prepareSource](../../app/src/main/java/com/cooper/lithic/data/media/LocalImageStore.kt)第81～100行有界拷入；本轮未修改这些文件或签名。无需改[MediaRepository.commit](../../app/src/main/java/com/cooper/lithic/data/media/MediaRepository.kt)原文件先于Room/完整CAS/重放协议。

## 2. 严格检查与错误语义

- 候选A1：固定libjpeg-turbo3.2.0 TurboJPEG3，STOPONWARNING=1，显式像素/扫描/内存策略，检查每次返回，完整decompress/finish后才成功。
- 候选A2：同上游libjpeg C API自定义fatal/warning出口，逐行/小条带消费，提供可审计取消与预算检查点；优先比较它对输出内存的优势。
- 容器/entropy损坏或decoder warning映射原`MediaFailureKind.INVALID_IMAGE`，错误明确且不泄露路径/原堆栈。不能回退到不严格解码并导入成功。
- 资源限额须区分`TOO_LARGE`或现有经审查可表达的失败；初始化/加载失败应保留环境诊断，不谎称图片本身非法。确切用户语义在后续实施前确认；本轮不新增错误枚举。原来表示明确图片损坏的三项测试仍必须是INVALID_IMAGE。
- 任意负返回、未完成消费、warning、pending JNI异常或预算中止均禁止提交PreparedImage。先清理本调用资源，再以结构化结果跨JNI返回；不能在error回调内跨不安全栈抛异常。

严格模式只证明固定解码器未报告所检查错误，不证明照片与最初原内容一致。合法像素改变或仍合法的渐进扫描删除可能无可检测标记，不能宣称万能防篡改。

## 3. 内存、工作量与取消的未决设计

不分配40MP全幅RGBA，不进行输入ByteArray→native无界复制。优先受控只读文件描述符/小缓冲源和row输出；FD有效期、读取上限、EOF行为及所有权需单一负责方。mmap/文件映射也不能被当作不占内存，需记录RSS/页驻留与截断风险。

progressive可能需要整图DCT系数；MAXMEMORY仅控制部分中间数组，不是硬native堆cap。需要选择受控allocator/内存manager方案，连同输入、系数、row、指针、标记、JNI、栈和后续规范化的生命周期合计，限制同时预检数。若修改上游allocator，需要记录patch、全分配覆盖、free余额、失败注入及升级成本，单独批准；若采用spill，也涉及私有文件上限/清理新协议，不能本轮偷偷加入。

所有尺寸/分量/stride/系数块/长度/容量先checked arithmetic；在32位size_t上也不得溢出。像素/扫描/input上限以既有契约为前提，不自行砍掉40MP以内合法progressive来换小内存数字。若必须收紧，提交产品兼容性建议等待批准。

进度/MCU/行检查点读取取消原子标志、deadline及累计工作量；libjpeg错误出口统一释放。需测量最坏检查点间隔，证明取消后native已停止使用FD/缓冲/准备目录，再允许释放和清理。`withTimeout`、换后台线程或取消Job不等于能强停native。未证明工作量与资源硬边界前，不能GO。

## 4. crash、OOM、加载与JNI边界

[JNI官方异常说明](https://developer.android.com/ndk/guides/jni-tips#exceptions)：managed异常不会自动展开native帧，存在pending异常时不能继续一般JNI调用；C++异常不能越过JNI边界。C层setjmp/longjmp不得跨需要C++析构的对象，清理所有已初始化句柄后正常返回Java层处理异常。

[AOSP native crash说明](https://source.android.com/docs/core/tests/debug/native-crash)中的SIGABRT/SIGSEGV会终止进程。Kotlin `catch(Throwable)`不是native崩溃保护；native malloc返回NULL的受控失败、Java pending OOM、allocator abort和系统杀进程必须分别登记。native崩溃/OOM/链接失败不是“普通坏图拒绝PASS”。

隔离PoC由外部supervisor记录child退出码/signal、stdout/stderr、wall/CPU/peak、timeout及是否完整返回结构化结果；只有decoder明确REJECT且正确cleanup才能算相应拒绝案例成功。进程退出不得自动转换成INVALID_IMAGE。

最小同进程JNI只能在已证明的预算和协作中止边界内给条件性建议。若审查要求强制超时/崩溃与主进程隔离，则应另评估独立Android进程服务，涉及Manifest组件/IPC/进程恢复/FD传递和测试，超出本轮及当前生产授权；本提案不创建它、不设想catch信号后继续执行或强杀native线程。

## 5. 失败时继承的资源与账本保护

原`LocalImageStore`第116～123行的未完成准备finally移除活动lease、只清本操作可识别stage；取消回传时Store第136～145行及Repository第72～80行通过原NonCancellable机制释放内存lease。严格预检应在asset.json/PreparedImage/正式资源/Room写入之前失败，不能让native尚在使用文件时提前清stage。

原三项测试不改；后续必须执行到此前没有到达的拒绝后断言，验证：

1. 两类旧封面引用、元数据、SHA和原字节完整保持。
2. 项目及全部金额事件、订阅及全部续费/状态事实、类别完整集合不变，不仅比较日均结果。
3. 没有新资源元数据、正式`media/<replacement>`文件或已提交缺损准备资源；没有活动lease。
4. 失败租约、已知stage按原协议正确释放/可清；unknown、无有效归属或不确定最终文件仍保守保留，不按UUID猜删。
5. 拒绝后再清理并重复核对旧事实与文件；测试teardown清隔离目录不能当生产cleanup证据。
6. 同原owner/coordinator保持并发、共享项目/订阅双引用、陈旧基线CAS、重放和取消；原文件落实后DB前、DB后GC前两处SIGKILL协议继续实跑。

原补充测试当前在拒绝断言失败，后续lease/cleanup断言未全部到达；R0没有使其通过。完整回归须使用后续生产修复的最终同输入，包含所有原测试、定向拒绝、JVM/Android、Lint/assemble和最终APK检查，不能拼接本研究预检结果。

## 6. 待批准的实际改动范围与下一闸门

本轮提交只含研究/状态资料。下一步需ChatGPT独立技术审查后另行明确：

- 研究环境：固定官方NDK、CMake/Ninja、下载哈希/SDK许可及独立安装位置；保持App冻结版本，不自动更换Gradle/AGP/Kotlin。
- 固定libjpeg-turbo3.2.0源码（已验签），A1/A2隔离native harness及四ABI；先编译、锁样本，再扩矩阵。
- 内存manager、CPU/取消粒度和是否需要进程隔离；这些都是未验证设计，不是已授权生产补丁。
- 后续生产JNI桥接/构建/许可证声明/必要配置须单独批准；真实ABI加载、minSdk26、16KiB页、APK增量及安全维护责任必须有证据。

生产准入条件是D01～D18的适用项、资源证明和四ABI/平台边界审查通过，之后才讨论最小生产接入授权。本轮停止 **BLOCKED — M10-R0 feasibility**；M10 PENDING，UI-C1已冻结，M11 NOT_AUTHORIZED。
