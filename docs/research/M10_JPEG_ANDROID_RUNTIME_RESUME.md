# M10：现有 API26 安全接续与 Android 验收

**BLOCKED — DAMAGED_GATE_LOG_CONTINUITY**。来源 `3474f639c2a831d26e74eeeff5ec8db0f6ea7a3f`；仅分支 `codex/m10-jpeg-android-runtime-resume`。已经取得 API26 的真实 native/Worker 运行证据，但未完成集中 Android 验收，不授予 M10 冻结。

## 实际结果

| 项目 | 结果 |
|---|---|
| 原实例身份 | **PASS**。launcher32880/QEMU32032、原创建时间、父子关系、真实 ExecutablePath、完整参数、SDK二进制及5624/5625归属匹配。两次完整观察后才准许ADB。运行配置/写入路径通过；本轮观察boot=1、API26/x86_64和正确AVD名。独立 `RESUMED_STEADY_STATE_PROOF` 明确原首次300秒启动时限 **NOT_PROVEN**，未回填旧boot-proof。 |
| 安装 | App、测试APK**各1次**，原SHA/证书/Manifest/实际runner保持，UID分别10077/10078。没有重新构建或替换。 |
| 健康 JPEG | **仅执行1次，JUnit PASS**，2.041s。Host6589/Worker6617，同UID10077。唯一SUCCESS、pin、FD关闭、六阶段认证退休、解绑、死亡、独立进程缺失及终结清理证据通过复核。原外层UNKNOWN保留，新增明确的离线原字节补充核验PASS；没有重跑。 |
| 锁定缺损 JPEG | **仅执行1次，JUnit PASS**，1.258s，真实 `INVALID_IMAGE` / native code117。Host7457/Worker7483，同UID10077；取得结构化终结记录及六阶段日志。但前后日志前缀不连续，完整生命周期门禁 **UNKNOWN**，立即停止后续方法。 |
| 其余 Android | 后续7方法（包含原三项JPEG断言、健康导入、PNG和v1兼容）及API36均 **NOT_RUN**。两个JUnit PASS不能替代这些断言或宣布M10修复。 |
| 结束保护 | 只读核查 **PASS**：无活动App/Worker行；两份合成Stage的终结/归属/无pin及匹配Worker缺失核验通过。**AVD继续运行、数据保留**；没有正常退出，因为API26全部自然门禁未通过。结束安全状态不补足已经缺失的日志连续性。 |

本轮实际 **405条ADB命令＝2安装＋2单方法instrumentation＋401只读查询**，全都定向 `emulator-5624`，原始命令退出码均0；退出0不替代门禁结论。API26新增创建 **0**、新增启动 **0**；API36创建/启动/安装/测试 **0**；清数据、重试测试、故障注入、主动终止、退出设备、H2/C2访问均 **0**。

## 必要监督器修正

1. 新 `runtime_identity.py` 仅对真实ExecutablePath暂缺/端口尚未监听允许只读等待；总窗最多30秒且可缩短、两次完整匹配才PASS。PID、父子、100ns创建时间、参数、路径、未知目标进程或端口冲突立即拒绝。真实首次null输入仍因缺创建时间/端口保持原失败；不补造字段，也不以CommandLine替代路径。
2. 实际控制台返回正确名，但使用`CRCRLF`。旧解析器误拆空行，首次续接exit1已保留；只修正该传输换行，拒绝名称/ACK/额外内容冲突。复用已经采集的原五条查询，加新的只读进程/路径复核建立独立接续证明，没有重发这次接续查询或改变旧start记录。
3. 健康单项的原始instrumentation文件为873字节，SHA `43f01ee4afe4a4cdd19fa0f9e0d07c1e6934e6c9571c7ae73276cfed760f4640`，与当时parser一致。取证层`read_text()`却将CRLF转LF，造成外层SHA不符。现改为严格UTF-8原字节读取。保留原before/after/result及UNKNOWN，只追加 `raw-byte-reassessment.json`；仅这个明确原因、固定首项、四份原文件SHA、精确换行变换及完整生命周期重算全部成立才可作为前置证据，其他UNKNOWN没有旁路。

最终 **234唯一Python方法：232 PASS / 2 SKIP / 0 FAIL**，原191身份保留；2 SKIP为本机symlink创建权限，junction/hardlink实测保留。新增身份、接续、真实console输出、CRLF及追加核验保护均有宿主验证，相关RED/中间失败仍在本地；关键RED随证据提交。没有重跑JVM/native全量、Lint、Gradle或生成APK。

## 当前阻塞及证据边界

缺损项测试前日志含健康项的6条旧phase，测试后环形缓冲仅保留缺损项6条新phase，原前缀消失。监督器按原完整性合同返回UNKNOWN；**没有忽略旋转、筛选SUCCESS行、放宽日志要求或重跑**。原after在此处停止，部分后续外部核验未执行；后来必要结束只读检查证明当前资源状态安全，仍不能证明整个原观察区间无缺失。下一步只交独立审查日志采集连续性与现有证据，不自动开发新采集合同、继续测试或清场。

证据分级保持：同步hello、Host FD责任和源只读模式部分由真实执行代码不变量支持；Worker关闭由结构化观察支持；退休phase、pin/terminal和进程缺失有独立观察。guard独占由测试真实执行的Stage取锁验证，外部只取元数据；没有冒称全部原始Binder字段、写只读FD实验或所有异常路径均已验证。

[真实结果](m10-jpeg-android-runtime-resume/evidence/RESULTS.json)、[小型SHA索引](m10-jpeg-android-runtime-resume/evidence/INDEX.json)、[本轮唯一完整命令journal](m10-jpeg-android-runtime-resume/evidence/commands.jsonl)、[健康原结果](m10-jpeg-android-runtime-resume/evidence/api26/method-00/result.json)、[健康追加核验](m10-jpeg-android-runtime-resume/evidence/api26/method-00/raw-byte-reassessment.json)、[缺损UNKNOWN](m10-jpeg-android-runtime-resume/evidence/api26/method-01/result.json)、[只读结束状态](m10-jpeg-android-runtime-resume/evidence/ending-readonly.json)。没有ZIP、镜像、SDK许可正文、APK、userdata、账本或图片字节归档。

锁定App SHA `0ada263192dbaa1256bff6c613e6d57a69e1160b17d4844d1e1cfcc9cf847dbf`；测试APK SHA `3b0005a31afa753e9a9709323572250c0c5c63153c68ca1f882755c98724282a`。App子树仍 `89679a188c2b6e6793962041383a9f92337e126f`，产品代码/原断言/Room/备份/算法/UI均无修改。M10 **PENDING**、原三JPEG历史 **FAIL**、UI-C1 **FROZEN**、M11 **NOT_AUTHORIZED**；R3原29项、D2A/A1及所有历史NOTE/UNKNOWN保持。只推本分支后停止等待ChatGPT独立验收。
