# 砺石 LITHIC — M10-R1 正式执行指令

**任务：安装固定 Android Native 研究工具链，执行严格 JPEG 解码隔离 PoC。**

用户已正式批准本轮所需的官方 NDK、CMake/Ninja 工具安装。

ChatGPT 对 M10-R0 的独立审查结论：

- R0研究材料：PASS WITH NOTES
- R0技术可行性：BLOCKED，原Windows环境严格解码PoC为0次
- M10：PENDING
- UI-C1：ACCEPTED / FROZEN — PASS WITH NOTES
- M11：NOT_AUTHORIZED

本轮允许开展真实原生编译与隔离实验，**但不允许接入正式生产App**。

## 一、来源与工作区

仓库：`cc7279694-debug/property`

R0研究分支：

`research/m10-jpeg-strict-r0`

R0最终提交：

`c4cd90b148cce99ba497d864be1176c90408a4da`

本轮新分支建议：

`research/m10-jpeg-strict-r1`

从R0提交建立独立worktree，保留R0原始研究及失败证据。

M10原分支：

`feat/m10-local-cover-media`

UI-C1冻结分支：

`feat/ui-c1-visual-redesign`

开始前核对真实Git状态和远端SHA。如果分支已前进，先报告，不覆盖、reset或强行rebase。

不得修改M10、UI-C1、main及其他项目工作目录。

## 二、阶段R1-1：安装研究工具链

### 目标版本

- NDK r28c：`28.2.13676358`
- CMake：`3.22.1`
- Ninja：优先使用SDK安装包中已提供的版本
- Android API：26作为最低目标
- 不升级Gradle、AGP、Kotlin、Compose、Room等现有项目依赖

先确认现有项目实际SDK路径，并检查：

- 官方sdkmanager是否可用
- SDK稳定渠道是否提供指定版本
- 对应磁盘空间
- 已安装组件与版本
- 官方下载来源和包校验信息

通过核对后，允许使用官方sdkmanager执行类似以下命令。SDK根目录以实际配置为准：

```powershell
$Sdk = "C:\Users\CDD\AppData\Local\MirraAndroid\sdk"
$Manager = Join-Path $Sdk "cmdline-tools\latest\bin\sdkmanager.bat"

& $Manager --sdk_root="$Sdk" --list --channel=0

& $Manager --sdk_root="$Sdk" --install "ndk;28.2.13676358" "cmake;3.22.1" --channel=0
```

以上是预期命令，不允许跳过可用性与路径核验直接执行。

如果 SDK Manager 不提供指定版本，先停止并报告，不自动换成最新版或预览版。

如果需要接受尚未同意的许可证，展示许可范围并等待用户确认；不得批量自动接受所有SDK许可证。

安装后实际验证：

- NDK source.properties版本
- Clang版本与Android目标支持
- CMake版本
- Ninja可执行文件及版本
- 四种ABI交叉编译能力
- 16KiB ELF对齐构建配置

优先使用已有SDK组件中的Ninja。若不存在，允许在用户批准的工具范围内提出官方Ninja固定版本和哈希来源；未核实前不要下载未知二进制。

不要修改系统全局PATH或其他项目配置。使用研究脚本中的显式工具路径。

## 三、阶段R1-2：固定原始研究材料

实际读取R0三份文件：

- `docs/research/M10_JPEG_R0_FEASIBILITY.md`
- `docs/research/M10_JPEG_R0_TEST_MATRIX.md`
- `docs/research/M10_JPEG_R0_PRODUCTION_PROPOSAL.md`

使用已经核验的官方libjpeg-turbo 3.2.0源码，重新核查发行资产SHA、签名与GPG指纹。

源码正式资产SHA-256：

`6f30092cef9fb839779646608f4ee14ae3cbac989c47fa05e841b0841f09878e`

不得使用不明来源的预编译.so或系统隐藏JPEG库。

两份锁定样本必须保持原字节：

正常JPEG：

`complete.jpg`

SHA-256：
`2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907`

缺损JPEG：

`entropy_cut_eoi_retained.jpg`

SHA-256：
`f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a`

不得修改或重新生成锁定样本。

## 四、阶段R1-3：先执行最小严格解码实验

优先比较两个实现。

### A2：libjpeg API逐行严格解码

作为首选实验方案。

要求：

1. 使用C接口和受控错误处理。
2. warning与fatal均不能被当作成功。
3. 完整读取图像扫描行。
4. 必须完成jpeg_finish_decompress。
5. 只保留必要行缓冲，不分配40MP整张RGBA图。
6. 清理所有已初始化decoder、缓冲区和文件句柄。
7. 不允许longjmp越过存在C++析构责任的栈。
8. 正常和异常均记录结构化结果。

### A1：TurboJPEG 3严格模式

作为对照方案。

要求：

1. `TJPARAM_STOPONWARNING=1`。
2. 设置适用的最大像素、扫描与内存控制参数。
3. 检查全部初始化、参数、header、decompress及销毁结果。
4. 不把成功读取header当作图像验证完成。
5. 出现warning、失败返回或未完成数据消费时拒绝。
6. 不分配无界输出缓冲。
7. 不把MAXMEMORY解释为全部native堆内存硬上限。

### 第一轮最小验收

分别运行两个锁定样本。

预期：

- complete.jpg：ACCEPT。
- entropy_cut_eoi_retained.jpg：REJECT。

记录真实返回值、warning、fatal、程序退出码、输入SHA、耗时和内存信息。

**如果固定3.2.0版本不能真实拒绝缺损样本，保留失败并暂停扩展，不准用我此前2.1.5 Linux宿主实验代替。**

## 五、阶段R1-4：扩展安全与资源测试

仅在最小实验结果具有继续价值时执行。

按照R0的D04～D18矩阵，重点验证：

- baseline灰度、RGB及正常编码变体。
- 合法progressive与缺损progressive。
- 合法和损坏的restart interval。
- EXIF方向及元数据。
- 64MiB输入界限。
- 40MP像素界限。
- 极端长宽、整数溢出、marker和scan复杂度。
- 取消、资源不足与异常回收。
- 重复运行后的资源余额。
- 四ABI编译、符号与ELF对齐。

任何新的fixture必须使用可重复生成的方法，保留来源、生成指令、字节数与SHA。

不得从用户私人相册批量收集测试照片。

### 内存门槛

必须区分：

- 输入文件大小
- native堆内存
- progressive DCT系数缓存
- 解码行缓冲
- JNI及文件读取缓冲
- 系统RSS与峰值
- 后续Android Bitmap内存

重点研究40MP progressive图片是否存在无法接受的内存增长。

不能通过随意缩小原有40MP支持范围制造“安全通过”。

无法实现可解释、可执行的内存预算时，报告BLOCKED或CONDITIONAL，不得标GO。

### 异常与取消

native崩溃、SIGSEGV、SIGABRT、OOM、符号缺失和链接失败分别记录。

不能将进程崩溃算作正常的坏图拒绝。

Kotlin Job取消不等于native解码已经停止，必须验证实际中止延迟和资源释放。

若需要新的隔离进程、allocator替换或上游源码补丁，只提出方案，不自行扩大生产实现。

## 六、阶段R1-5：Android与ABI验证

目标ABI：

- arm64-v8a
- armeabi-v7a
- x86
- x86_64

全部必须编译并验证输出格式、依赖与16KiB ELF对齐。

实际运行设备以已有授权设备为准。

已有API36模拟器可以用于隔离native测试，但：

- 不修改已有账本。
- 不清除应用数据。
- 不重建或wipe模拟器。
- 不安装替换正式砺石APK。
- 不触及M10/UI-C1业务记录。

如果现有环境没有API26设备，登记NOT_RUN，不自动安装系统镜像或新建AVD。

四ABI编译成功不等于四ABI均已在真实设备加载成功，必须分别登记COMPILED与RUNTIME_VERIFIED。

PoC源码及独立工具不能修改正式App Gradle、Manifest或生产JNI入口。

## 七、资源和兼容性结论

研究结束时，比较A1/A2：

- 锁定缺损样本的真实拒绝效果。
- 健康JPEG误拒情况。
- baseline/progressive支持。
- native内存与峰值RSS。
- CPU及最坏工作量。
- 取消与错误清理。
- ABI支持范围。
- Android API26/36实际结果。
- 16KiB页兼容证据。
- 预计生产集成复杂度及实际生成.so体积。
- 上游许可证与后续维护成本。

没有实际运行的数据标NOT_MEASURED或NOT_RUN，不得填预测值冒充实测。

只有在上述门槛有充分证据后，才可给出生产接入的技术建议。

**R1即使成功，也不授权自动修改生产App。**

## 八、提交与独立验收

在研究目录新增：

- `M10_JPEG_R1_EXECUTION.md`
- `M10_JPEG_R1_RESULTS.md`
- `M10_JPEG_R1_RESOURCE_REPORT.md`
- `M10_JPEG_R1_PRODUCTION_RECOMMENDATION.md`

保存实际编译命令、退出码、环境版本、测试结果、日志、输入和产物哈希。

可以提交非敏感研究源码和必要实验脚本；不得提交大体积构建缓存、未知二进制、私钥、私人数据或整个Android SDK。

仅向 `research/m10-jpeg-strict-r1` 提交推送。

M10原三项JPEG拒绝断言继续保留FAIL历史，不因隔离PoC成功而改写。

模块状态保持：

- UI-C1：ACCEPTED / FROZEN。
- M10：PENDING。
- M11：NOT_AUTHORIZED。

最终只能返回以下状态之一：

**READY_FOR_REVIEW — M10-R1 isolated native PoC**

或者：

**BLOCKED — M10-R1 isolated native PoC**

并分别说明实验成功部分、未解决风险、是否建议进入生产方案审查。

不得自行宣布M10冻结，不合并main，不修改UI-C1冻结版本，不进入M11。

执行完成后立即停止，等待ChatGPT独立技术验收。