# M10 JPEG Worker / Stage V2 集中候选

**结论：BLOCKED — Android身份/资源生命周期门禁未验证；离线候选已构建，等待独立审查。**

来源 `717d541578ebf761e2e1c70a842ca8b7e5dab5b2`；分支 `codex/m10-jpeg-worker-stage-v2-candidate`。本轮用户明确允许必要的候选私有协议、Worker/JNI/App 接入；独立 worktree 执行，不改变正式 M10 / UI-C1 / main。最终状态与原始结果见 [RESULTS.json](m10-jpeg-worker-stage-v2/RESULTS.json)。

## 实现与边界

- JPEG 新建私有 `.m10-jpeg-v2`；规范版本化 owner 绑定 generation / asset / attempt / session / nonce、源 inode / SHA、guard inode 与固定 decoder/normalization 策略。FD 交付前持久 pin；撤销首次原因固定、回读确认；唯一结果、数据 FD 关闭、匹配 Binder/OS 死亡、解绑与独占 lease 全部成立才写 terminal。成功还须完整严格解码、destroy 和输出身份，才可 prepared。未知字段、残留、身份或同步结果均拒绝准入。空guard用双lstat核验，不在持锁期间临时开闭同inode句柄；Android/POSIX跨进程语义仍待实测。
- 源释放和 GC 在同一独占 lease 内执行归属核对、持久意图、已知文件删除及后置记录；未知子项不删。创建中断仅在同次 create 尚未交付 FD、拥有明确 NO_CONSUMER 证据且所有句柄已闭合时收口；重启看到不完整 stage 仍阻塞。终结 tombstone 保留。成功保留至多 2MiB 的规范化副本供凭据重放，直至安全 GC。
- 非导出同 UID `:jpeg_worker` 只接收受控 FD，严格 native 预检后复用原规范化代码；不初始化 AppContainer/Room/数据 owner，不提交业务引用。Host 仍唯一提交者；全局业务写锁不覆盖源导入/native/IPC 等待。App owner 关闭被 preparing 或未终结 V2 拒绝。
- 独立监督、短内存失败围栏、持久撤销、旧回调拒绝、固定总期限与认证退休。无结果的 Binder 死亡经有界乱序窗口仍无结果即 UNKNOWN；解绑未知不开放下一任务。取消只撤销资格，不推定 native 停止。弃置结果由应用级once回调处理，仅完整已证终结才记录失败凭据及清理；UNKNOWN仍保pin。正常退休先确认解绑再经保留Binder发送认证RETIRE，解绑不是死亡证明。损坏、资源不足、载入、超时和未知分别分类，未诊断退出不冒称 native crash。
- PNG 继续 v1，64MiB / 40MP / EXIF / 1600px / 2MiB 原边界不缩减。旧 v1 JPEG 已提交封面仍可读取；未严格验证的旧准备不能获得新凭据，需重新选择原图。因旧 final ownership 不等于 Room 提交证明，旧 v1 JPEG 自动清理也保守保留。Room/schema、备份格式、领域算法、UI 与原三项 JPEG 断言未修改。

## 实际验证

最终数量、退出码、输入/日志哈希及APK字节以小型[结果索引](m10-jpeg-worker-stage-v2/RESULTS.json)为准。证据目录按Git属性保留原字节；原始日志的固有尾空白保留，未以修剪日志伪造原始哈希。

- Stage 先取得真实 RED（34方法/31失败），再修复真实持久协议。Windows 中间两次失败分别来自 JDK 空 fileKey 与空 guard 的强制锁读取，原日志保留；最终使用实际 NTFS file ID，不用 mtime/路径伪装身份。文件读写、force、FileLock 为真实宿主操作；目录同步为注入适配、Binder/死亡/解绑为合成事实，不计 Android PASS。
- 最终 **236个唯一JVM方法PASS**＝原86＋Stage67＋Worker83。先完整212项PASS，后对最终Worker83项定向回归；重叠59项及中间63项/直接Java执行不重复计数。原测试身份保留，新增弃置结果和退休门禁各12项；真实mutation分别71执行/10、5、3失败及83执行/5失败。另9项Python构建输入测试PASS；不重复既有native压测。原始XML派生汇总及输入哈希见结果索引。
- 最终Lint **0错误/23警告**，Debug构建及Android测试源码编译PASS。首次完整构建因两处API27字段与本地SDK路径转义共3个Lint错误失败，如实保留；现用已实测四ABI/API26 NDK宏的原子O_CLOEXEC值，并修正仅本地properties；未提高minSdk/添加抑制。Application类型登记消除两条新Context警告。
- 四 ABI native 从既有固定3.2.0构建逐 SHA、ELF与源码对应核验后打包；无下载/升级/重编 native。生成资产包含上游 LICENSE.md / README.ijg 和修改说明，不提交 SDK 许可正文或二进制。AGP strip输出通过既有NDK工具逐字节复现，四ABI仍16KiB对齐、API26标识和唯一JNI符号；最终APK内库/许可证字节一致，Worker非导出且独立进程，原签名一致、无新增权限。
- 新增4项 Android兼容测试仅编译，原健康/缺损及旧封面保护测试未运行。ADB、AVD、安装、启动、H2/C2访问均0次。没有新的 CI 运行；既有 [native CI](https://github.com/cc7279694-debug/property/actions/runs/39109960477) 仅继承历史效力。

当前唯一候选APK（未安装/发布）：`E:/CODEX/.codex/worktrees/lithic-jpeg-stage-v2/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`；13,851,571字节；SHA-256 `9100b0b27d283ae83a826b9020ce160c66b3f7a57aed38698c62d8317a0741f3`；`com.cooper.lithic.debug` / version8 / min26 / target36。工具链保持JDK17.0.20.1+1、Gradle8.14.3、AGP8.13.2、Kotlin2.3.10及既有NDK28.2。

## 阻塞与下一独立验收门禁

此为接入候选，不能证明真实 Android 生命周期已安全。B1/R3 继续 BLOCKED；原三项 JPEG FAIL 保留（本轮 NOT_RUN），M10 PENDING。需要另行确认安全独立设备与适用操作权限后，先验证身份/FD/绑定/退休/lease，再验证三 JPEG 断言、旧项目和订阅封面与事实、PNG、取消、超时、进程死亡、持久同步失败及恢复/GC。未终结/UNKNOWN不得通过重试、删除或假终结绕过。

512MiB 只是已研究的 native 分配请求预算，不是进程 RSS/手机总内存硬上限；源流阻塞、Android 最坏取消/磁盘/系统OOM、同 UID 非恶意代码边界、未知退出、未知cache残留、全量stage准入长期性能及跨进程内存影响仍未证明。Worker 不是隔离恶意 native 的安全沙箱。失败持久写入与内存状态不原子，未知继续关闭准入。保留 D2A-N01～N05、A1-N01～N04、历史 UNKNOWN/FAIL/NOT_RUN；UI-C1 FROZEN、H2 DELIVERY_UNKNOWN/pin=true、C2保留、M11 NOT_AUTHORIZED 均不变。提交推送候选后停止，等待 ChatGPT 独立审查，不自行冻结或发布。
