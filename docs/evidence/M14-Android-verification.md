# M14 — Android 补充验收

2026-10-21；评审基线 `730fd2a47c150526787a7889714fc6b66c8b12f8`；分支 `codex/m14-category-search`。**READY_FOR_REVIEW，未自行冻结。** 原[初次交付](M14-verification.md)和当时的 NOT_RUN 保持原样。

## 实际结果

| 原有测试 | PASS | FAIL | NOT_RUN |
|---|---:|---:|---:|
| 类别 Repository | 11 | 0 | 0 |
| 搜索、排序和分页 Repository | 12 | 0 | 0 |
| 完整备份恢复 | 1 | 0 | 0 |
| 类别 Compose | 3 | 0 | 0 |
| 搜索 Compose | 1 | 0 | 0 |
| **原28项合计** | **28** | **0** | **0** |

另有 **1项真实组件截图旅程 PASS、8张截图**，与原28项分开，共29个唯一方法。每个方法均有实际 instrumentation 完成码0，五类原测试及截图类均有 JUnit OK、runner结束码和外层exit0；[逐方法原始结果](m14-android/README.md)、[统计/源码哈希](m14-android/results.json)。最终 Test APK 身份下的29项全部通过；不将不同APK或早期失败混作最终通过。

- 合并测试验证当前/历史引用、总额和资金事件不变，订阅身份、付款及状态历史不变；真实Room触发器异常时更新和删除完整回滚。跨类型、重名、默认类别、陈旧确认、重复提交及旧代写入拒绝通过。
- 搜索只匹配名称，特殊字符按字面处理；157条跨类型搜索及105条同日期/持有时长分页校验无重复遗漏，当前/历史状态和冻结引擎边界通过。
- 类别新建/改名/合并后，经冻结format v1导出和恢复，完整七表/设置/3份媒体字节、类别映射、恢复后的Search/Report快照相等，旧代迟到写入拒绝。使用合成受控文件目标，**不是新一次SAF用户流程验证**。
- 截图旅程实际操作设置→类别新建/改名/两类型合并→名称搜索→历史订阅详情，核对真实Repository事实仅类别变化、类别计数刷新及准确详情来源。[图库](m14-android/README.md#截图来源)。所有图均为 **隔离ComponentActivity内的真实LithicApp/Nav3，非MainActivity**。360dp画布；最后两张搜索图fontScale=2，系统font_scale仍1.0、density420，未修改设备设置。合并图为实际对话框根截图。

## 最小修改与保留的失败

本轮**没有产品代码、Room schema、备份格式、领域算法、导航、权限或依赖改动**。仅新增截图测试、证据与状态文档，沿用仓库惯例为新证据目录设置Git原字节保留规则，并修正三处原测试接缝：

1. 汇总保留零金额默认类别，原无条件`.single()`错误；改为核对仅源类别消失、目标完整金额/贡献数准确合并、其他类别不变，原总额/资金/引用断言均保留。
2. 合并说明的`assertTextContains`默认要求完整匹配；改为显示并精确核对原完整句子，含源/目标和历史影响数量。
3. 搜索状态处于可点击卡片的未合并语义树；按未合并节点核对完整“状态 · 来源”，原精确记录点击断言保留。

对应真实RED、旧Test APK的截图诊断失败、命令参数/新增测试导入编译失败均[单列保留](m14-android/README.md#证据索引)，不覆盖旧结果。构建隔离环境曾生成另一Debug证书，安装前核验拒绝，随后仅恢复构建使用原Debug证书；该产物未安装。`pm path`对不存在包返回1的前置检查改用精确包列表确认，未发生安装重试。

## 环境、构建与产物

唯一新设备 **Lithic_M14_API36_20261021 / emulator-5672 / API36 / x86_64**。全新独立根目录 `E:/CODEX/.codex/research-env/lithic-m14-android-20261021`，无快照、旧数据或个人账号。官方工具创建实际`<name>.avd`与运行写盘均在此新根内，原请求空目录保留，未重复创建。[路径/进程/端口/boot证据](m14-android/device/host-instance-proof.json)。创建1次、启动1次、App安装1次、Test首次安装1次＋必要同签名更新2次。全部ADB只定向5672；5686、H2、C2、M10及其他旧设备无操作。

现有SDK与镜像复用，无下载/升级：Emulator37.1.11、API36 Google APIs x86_64 r7、JDK17.0.20.1、Gradle8.14.3。经用户另行批准，启动前仅用Gradle正常停止已确认空闲的PID7508构建服务，无强杀。

最终测试包构建及Lint成功（3m51s），**0错误/9条既有警告**；[日志](m14-android/logs/test-ui-semantics-fix-build-lint.log)、[Lint XML](m14-android/lint.xml)。原136项JVM与Debug App构建证据继承，本轮未重跑无关历史JVM，App未重新打包替换。

| 产物 | 字节数 | SHA-256 |
|---|---:|---|
| App：com.cooper.lithic.debug，code9 | 12,771,695 | `59d45ad38b467e5c4bb895c71feab3315bb872d670177ab7457033179c4b35b9` |
| 最终Test：com.cooper.lithic.debug.test | 2,755,987 | `7ef8915685801fbbc3cdeb76ab80ff2e4b6d31264aa761cc95071c91ce1e6ff7` |

两者为原Debug证书 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。隔离运行器 `com.cooper.lithic.app.M13IsolatedRunner` 使用plain Application，不开启默认账本。安装后及结束时实际设备APK字节均匹配，User0 App UID10216 / Test UID10217；[安装证明](m14-android/install/commands.json)、[最终Test更新](m14-android/test-update02/commands.json)、[结束核查](m14-android/device/end/commands.json)。APK、密钥和AVD磁盘均不入Git。

结束时仍为同boot、同QEMU/launcher的5672在线实例；缓存Test进程5993（UID10217 / com.cooper.lithic.debug.test）保留，未终止。`no_backup`无残余合成账本目录，`files`保留profileInstalled标记和截图证据目录（早期4图＋最终8图）；测试owner已按各自finally关闭/清除本次专属合成根。未清数据、未重建、未删除未知资源。

## 仍保留的边界

无未解决的本轮确定性业务缺陷。真实并发压力、系统级200%设置、完整IME/TalkBack、MainActivity发行入口、API26、真机/OEM及Release仍NOT_RUN。M13-T06 NOT_RUN / DEFERRED、M10 PENDING / DEFERRED及三历史JPEG FAIL、UI-C1冻结、Q03～Q06和历史NOTE/UNKNOWN保持。不合并main、不进入M15，停止等待ChatGPT独立复验。
