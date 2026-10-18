# M10-H2-D2B-A2-R — 官方API26镜像与隔离冷启动执行记录

2026-10-18，Asia/Shanghai。**BLOCKED — M10-H2-D2B-A2-R**。本轮已实际下载并解包固定Google API26 x86_64 Google APIs r16，独立核验全部11个原始文件的尺寸/CRC及SHA清单。SDK管理器异常退出保留，镜像可用后置条件单独证明。随后**唯一AVD创建命令失败**：`Can't locate Android SDK installation directory for the AVD .ini file.`。已停止，不重试创建、不清理残留。模拟器启动、ADB、APK安装和Host启动均为0。

这不是旧A2的“未安装API26”阻塞。许可匹配已有接受记录，用户已明确3GiB只限制下载流量；二者均不再作为当前阻塞。

## 任务契约与来源

- Goal：解决旧A2缺少API26镜像的前提，并仅在来源、预算、许可和隔离门禁通过后，安装锁定A1 Probe、执行一次无command/case/runId的隔离API26 Host冷启动。
- Scope：官方SDK查询、镜像元数据/许可/容量核对、研究宿主取证脚本、APK实物只读审计、当前状态和checkpoint。用户明确批准符合条件的官方镜像下载安装；出现新许可、超过3GiB、官方镜像不存在或不能隔离则停止。
- Out of scope：原API36模拟器和H2现场、normal/Outsider/Worker绑定、其他生命周期场景、研究APK或正式代码修改、APK重建、JNI/allocator、API27、M11。
- Constraints：既有APK字节固定；不自动接受新许可，不批量yes，不升级工具链，不克隆旧AVD、不清数据、不重试App。未执行验证保持NOT_RUN。
- Dependencies：旧A2 `349264371b8b9849dfe83a5ddee74f204129d4ad`；A1 `971f742452422ce6b31462123c351d112eb48101` 及其原始Probe产物。
- Acceptance：先核实官方API26 Google APIs x86_64及许可和预算。环境门槛通过后才允许后续AVD和唯一Host启动；门槛失败则交付新的阻塞证据，不复用旧A2结论。
- Verification：保存真实命令/退出码、原始stdout/stderr与SHA、官方XML和完整许可文本、实际SDK规范化哈希、既有许可文件前后哈希、APK审计及交付范围核查。

来源提交在本地存在，远端旧A2分支仍指向该SHA。独立worktree：`E:/CODEX/.codex/worktrees/lithic-a2r/砺石新APP`。新分支：`research/m10-jpeg-h2-d2b-a2r-api26-isolated`。创建时HEAD精确为旧A2，工作区干净，没有覆盖其他分支。

使用HR Project Memory恢复/继续与checkpoint流程，沿用唯一CURRENT_STATE。已读取现行AGENTS/PROJECT、当前状态、相关决策/架构/路线、旧A2报告、A1构建来源与实际Host初始化代码。当前用户授权覆盖旧AGENTS里的阶段性分支限制；不扩大生产权限。

## 指令恢复限制

本轮当前用户消息的下载安装授权及停止条件可直接读取，见[范围原件](m10-jpeg-h2-d2b-a2r-api26-isolated/USER_SCOPE.md)。读取评审聊天 `M10 补充验证 review` 时，接口只返回助手回复的内容占位，网页读取也失败；因此没有把未读到的完整A2-R文字当作已经恢复。已请求全文，并遵守本轮明确授权及此前A2的隔离/唯一无命令Host限制，不借未读文字扩权。发现展开逻辑尺寸超过3GiB后，用户实际回复 **“只限制下载流量（允许展开及AVD超过3GiB）”**，收到该澄清后才开始安装。无需新的许可接受。

## 实际官方查询与异常

使用已安装官方Command-line Tools 23.0的 `sdkmanager.bat`，指定全新研究SDK：

`E:/CODEX/.codex/research-env/lithic-a2r-api26/sdk`

1. [第一次查询](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/sdk-query-20261018T023518.337117Z/result.json)：隔离ANDROID_USER_HOME下的官方兼容入口尝试引导下载Android CLI，HTTPS连接中断，退出**1**、stdout为空。没有镜像查询结果。这个意外bootstrap尝试如实保留，不能说完全没有发生工具网络请求。
2. 检查发现本机已有官方CLI及缓存。仅将本次子进程的ANDROID_CLI_BIN指定到既有 `C:/Users/CDD/.android/bin/android-cli.exe`，不下载/升级工具，不修改全局环境。研究SDK目标不变；复用的是通用CLI缓存，不是旧AVD数据。
3. [第二次查询](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/sdk-query-20261018T023604.949325Z/result.json)：实际仍经sdkmanager入口，输出124,196字节，明确列出 `system-images/android-26/google_apis/x86_64`、版本16.0.0。进程退出 **3221226505 / 0xC0000409**，必须保留为异常退出，不能改写为查询进程PASS。工具末尾提示有新CLI版本，本轮未更新。

官方当前[SDK管理说明](https://developer.android.com/tools/sdkmanager)确认sdkmanager兼容入口与Android CLI的关系。两次查询的stderr和退出码均完整保留；没有用重复App试验绕过它们。

## 官方包、容量与许可门禁

独立有界读取[Google官方镜像元数据](https://dl.google.com/android/repository/sys-img/google_apis/sys-img2-3.xml)实际HTTP200，237,880字节，SHA-256 `ad71946ba2d002f86d9ece1121b74aec494b420aa6b8fdac83fa5ce52b51dcb4`。归档[XML及解析](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/official-metadata-20261018T023649.606169Z/result.json)与异常查询输出相互核对目标包。

| 字段 | 实际值与限度 |
|---|---|
| 包 | `system-images;android-26;google_apis;x86_64`，r16，stable/channel-0 |
| 官方归档 | `https://dl.google.com/android/repository/sys-img/google_apis/x86_64-26_r16.zip` |
| 元数据声明大小 | **980,740,438字节 / 935.307MiB**，数值低于3GiB；尚不能因此判定整个预算满足 |
| 元数据声明SHA-1 | `0eb13cc91cf164b617eaf85e4030289cb18189de`；完整归档未保留，**不是独立实测整ZIP哈希** |
| 归档HEAD | 首次Python SSL UNEXPECTED_EOF保留；后续curl实际HTTP200/Content-Length980740438，退出0 |
| 当次可用空间 | E盘67,942,899,712字节，C盘16,216,694,784字节 |
| 官方ZIP目录 | curl单次有界Range取得HTTP206/65536字节，Content-Range与文件大小匹配；15项/989字节中央目录完整解析 |
| 展开逻辑尺寸 | **3,376,301,474字节 / 3.144425781GiB**，比3GiB多155,076,002字节；其中system.img2686451712、userdata.img576716800、vendor.img102760448 |
| 压缩包＋展开逻辑重叠 | **4,357,041,912字节 / 4.057811491GiB**；仅算术模型，不是实测磁盘峰值，未计AVD/工具缓存；稀疏分配影响未知 |
| 用户预算口径 | **只限制下载流量，允许展开及AVD超过3GiB**；不是要求总占用≤3GiB |
| 下载控制 | 两个完整镜像传输＋既有CLI文件大小及4MiB元数据的保守模型为2050287940字节，低于3GiB；不是精确网卡计量。首次传输实际约260MiB中断，重试完整一次，无第三次镜像传输 |
| emulator最低依赖 | 官方元数据26.1.3；本轮未安装/升级emulator |

## 许可初次错误与已证实纠正

官方包引用`android-sdk-license`，印刷日期2019-01-16。初次Python `write_text`在Windows把XML中的LF文本转成CRLF；再从该副本执行TrimStringAdapter，产生`9cb2a03b…`假差异。初次“新许可阻塞”判断**错误，已撤销**。首次交付检查也因保存文件哈希不符而真实失败；该宿主取证错误不是产品测试失败或新许可要求。

- 原CRLF副本17107字节、SHA256 `00eceefbf21af958ba76722fdf3330143d7176e4626f341c3d8925a6e40726ea`，以及[原错误判定和命令](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/license-capacity-gate/result.json)均原样保留，明确已被纠正证据取代，不再作为有效门禁。
- 更正为Java安全解析**原始官方XML的license节点**，再调用实际SDK `TrimStringAdapter.unmarshal`与UTF-8 SHA1，退出0，得到 **`24333f8a63b6825ea9c5514f83c2829b004d1fee`**。
- [原XML逐字节抽取的LF许可文本](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/license-correction-archive-metadata/android-sdk-license.xml-extracted.utf8.txt)：16962字节、SHA256 `1f8729233617b193fd619213792ae16a41b95d2bbbf525dfe66998252ba68b16`；与CRLF副本仅换行不同。
- 正确哈希匹配当前MirraAndroid SDK及默认Android SDK已有接受记录。**不需要接受新许可**；本轮没有执行`--licenses`、`yes`、`setAccepted`或写入接受哈希，旧许可文件保持。

完整纠正命令、原始stdout/stderr、HTTP200/206和ZIP目录见[更正及容量取证](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/license-correction-archive-metadata/result.json)。初次研究环境清单6,614,571字节仅反映当时的SDK查询元数据/临时文件；后来新增ZIP尾段65536字节，没有完整image或AVD。没有将临时DLL、缓存、镜像或尾段二进制提交到Git。

上述6,614,571字节是安装前时刻的历史清单；最终镜像和本地工具已在独立SDK中，不能拿早期清单充作当前占用。

## 固定镜像下载、解包与独立后置校验

使用同一已安装官方CLI，精确执行 `sdk install system-images/android-26/google_apis/x86_64@16.0.0`，目的SDK固定在研究根。没有更新CLI或旧SDK；仅把已匹配的既有许可接受文件复制到研究SDK，不接受新条款。

- [首次安装](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/image-install/result.json)：156.485秒，约259.7MiB时 `Peer disconnected`，退出0xC0000409。未称成功。
- [一次受控重试](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/image-install-retry-1/result.json)：309.813秒，下载/解包显示100%，仍退出0xC0000409、stderr为空。**安装器命令仍FAIL**，不把100%进度改写成正常退出。
- 随后独立校验[已提取镜像](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/installed-image-audit/result.json)：API26/google_apis/x86_64/r16，全部11个ZIP原始文件的尺寸及CRC32与Google HTTPS取得的完整中央目录一致，逐文件计算SHA-256；精确核对目录清单，无额外原件。安装器生成的package.xml单独核对，不冒充ZIP原件。
- [创建前补充门禁](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/pre-avd-storage-gate/result.json)：IMAGE到新研究根的所有父层无junction/reparse，包元数据唯一、API/ABI/tag/revision/license一致，本次SDK/Java写入进程观察为空。中央目录无重复/越界/链接类型，989字节原始中央目录以hex归档，可离线重解析。
- 完整下载归档未保留，不能声称独立实测整ZIP SHA-1；官方元数据SHA-1与已提取文件的CRC/尺寸/SHA证据分别登记。**镜像后置条件PASS不覆盖SDK管理器FAIL**。

本机既有官方三工具包完整复制到独立SDK：[536个文件/1279464244字节逐SHA一致](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/local-tools-copy/result.json)，新增工具下载0；cmdline-tools23.0、emulator37.1.11/build15917651、platform-tools37.0.1。没有复制原AVD或用户数据，没有工具升级。

## 唯一AVD创建与停止

拟定新环境：AVD `Lithic_A2R_API26_20261018`，预留serial `emulator-5610`，新SDK/注册目录/数据目录/Android用户目录/模拟器目录全部在 `E:/CODEX/.codex/research-env/lithic-a2r-api26/`。所有未来ADB命令均硬限定新serial，监督器不提供normal/Outsider/恢复入口。

首次监督器create预检发现Android用户目录已经存在，尚未发出AVD命令即停止；核查该目录完全为空、无AVD注册/数据/消费者。对本轮工具可能初始化的空偏好目录建立明确空目录记录，仍拒绝任何非空/链接/AVD旧状态；没有删除目录或跳过未知内容。这是宿主预检修正，不是第二次AVD创建。

第二次监督器调用才持久写入 `create-attempt.json`，实际发出**唯一一次**新SDK avdmanager create命令；未使用force，不复制旧配置。命令返回1，stderr明确：

```text
Error: An error occurred while creating AVD: Can't locate Android SDK installation directory for the AVD .ini file.
null
```

见[实际创建命令/退出码](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/create-20261018T025903.791601Z/002-command.json)及[失败](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/create-20261018T025903.791601Z/failure.json)。未继续修改SDK发现规则、伪造SDK结构、删除残留或重发创建。

[只读结束快照](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/closing-host-snapshot/result.json)确认：AVD `.ini` 不存在；目标数据目录为空且保留；create marker保留；没有start/install/Host launch marker，也未观察到目标模拟器或SDK写入进程。**有效AVD创建/运行隔离未通过**。根因只确认到工具报告的SDK目录定位失败，不能凭推测改成API26不兼容或Host代码缺陷。

## APK与后续证据边界

本轮独立只读核对A1原始Probe实物、签名及Manifest，见[APK审计](m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/apk-audit/result.json)。原APK83,761字节、SHA `0d98316faf6cb18b589bad526a5d8260cea825a408ed949f32855bd7a7d31b8a`；没有重建或安装。

源码核查确认无command的Activity跳过dispatch；Host后台初始化会创建0字节owner及空synthetic-stages/r3-reports、诊断trace，不能把空stage根误认为已建立attempt。`APPLICATION_ROLE`日志在completeBackground之前；没有无命令直接READY输出。**本轮未启动，因此运行时READY仍NOT_RUN**；未来也不能由角色日志或Activity显示推断READY，或额外发命令补证。新增监督器的start/observe入口本轮未执行。

## 交付与停止

本轮只新增A2-R宿主取证/安装/隔离监督脚本、原始证据、[结果/风险](M10_JPEG_H2_D2B_A2R_RESULTS.md)、[checkpoint](../checkpoints/2026-10-18-M10-JPEG-H2-D2B-A2R.md)，并更新CURRENT_STATE。旧A2、R0～A1历史报告/失败、研究APK Java、Manifest、Gradle、APK与正式产品代码不变。新LicenseCanonical.java只是宿主许可取证，不属于研究APK。

实际2次镜像安装命令、1次AVD创建命令失败、0模拟器启动、0ADB、0APK安装/Activity启动。原API36和H2设备现场没有访问或操作；不宣称原私人字节或188项元数据已重新核验。JUnit/Lint/构建未重跑，不能将A1历史295项PASS记作本轮测试。只运行宿主SDK/许可/文件完整性、APK审计和交付核查。

只提交推送本轮A2-R分支后停止，等待ChatGPT独立审查SDK发现/AVD创建错误的最小修复与后续授权边界。**当前阻塞仅是AVD创建失败，不是缺镜像、许可或容量**。不得自行重试创建、启动AVD或Host。M10 PENDING/三JPEG FAIL、UI-C1冻结、M11 NOT_AUTHORIZED、R3完整生命周期BLOCKED及所有历史NOTE/FAIL/UNKNOWN/NOT_RUN保持。
