# 真实录屏反馈定向精修

2026-10-28；分支`codex/lithic-v1-product-completion`；参考`d0538c37aa0eda6ca34bc4fb5ae5b03d443609fc`。

**IMPLEMENTED — READY_FOR_INDEPENDENT_REVIEW / APP_VISUAL_PENDING**。依据用户转交的约31秒录屏独立审查实施；本轮未直接取得该录屏原件，没有新Android运行或截图，不登记视觉通过。

## 用户可见变化

- [ProjectListScreen](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectListScreen.kt)：去掉默认“当前列表N条”段落；已知总数与实际显示一致且不超过原50条单页时，不再显示“已到列表末尾”。总数、未知和旧计数通过既有信息入口与列表stateDescription准确保留。真正的多页进度、加载、数量冲突、失败、旧结果和重试继续可见；原记录身份、金额、回调及48dp控件不变。
- [CategoryAnalysisScreen](../../app/src/main/java/com/cooper/lithic/feature/category/CategoryAnalysisScreen.kt)：没有明确选择及未解决notice，且已有完整snapshot/asOf时，直接展示当前类型唯一的已有CategoryReport。明确选择优先，失效明确ID不被替换。完整旧报告在刷新/失败时保留，原ReportReadStatus及截止日如实提示；首次未读、无类别、无记录、真实0和负值分别处理。没有查询、写入、选择回调或新缓存。
- 类别分页窗口随snapshot、类型、实际显示类别ID重置，仍每50项继续查看。类别金额、年度和记录正文与基线逐字相同。
- P1仅核对：Home、Review及导航源码不改。回看沿用C5-R1相同高度的占位/Canvas（摘要172dp、完整240dp）和固定状态槽；未发现需追加修改的确定性分支。源码不能证明实际切换不跳动，动态仍PENDING；无假记录或媒体绕过。

## 实际验证与原始记录

- **最终37个唯一JVM方法PASS**：列表6、单类别7、C5-R1呈现18、回看真实空态6。见[最终结果](recording-feedback-20261028-050534/verification-final.json)与[逐方法XML](recording-feedback-20261028-050534/jvm-final-results/)。
- **首轮32 PASS／5 FAIL保留**：5个失败均新列表夹具使用非UUID类别ID，在初始化校验即失败，未到呈现断言。仅改为规范合成UUID，不改生产逻辑或任何断言；定向重跑该类6项全PASS，其余31项未重跑，不累计成43项覆盖。见[初轮结果](recording-feedback-20261028-050534/jvm-initial-summary.json)、[初轮原始输出](recording-feedback-20261028-050534/desktop-check/command.json)与[唯一夹具修正](recording-feedback-20261028-050534/fixture-correction.json)。
- **普通Debug构建、AndroidTest源码编译、Lint均exit0**；Lint实际为**0错误21警告**。见[后续命令/退出码](recording-feedback-20261028-050534/desktop-completion/command.json)、[stdout](recording-feedback-20261028-050534/desktop-completion/stdout)、[stderr](recording-feedback-20261028-050534/desktop-completion/stderr)及[Lint XML](recording-feedback-20261028-050534/lint-results-debug.xml)。使用现有离线工具链、单worker及单次构建服务，不停止其他项目服务；保留SDK XML解析版本警告，未升级工具。
- 新增4个Compose方法及原P02呈现断言适配已编译；**Android/Compose实际运行、触控、大字、截图及真实导航动态均NOT_RUN**，不以编译代替体验PASS。
- 初始约2.919GiB可用内存未达4GiB门槛，曾保持[资源阻塞／NOT_RUN](recording-feedback-20261028-050534/verification-status.json)；用户释放后核对约4.34GiB才执行。历史资源记录未覆盖，见[构建准入](recording-feedback-20261028-050534/build-resource-gate.json)。
- `git diff --check`通过；[6文件最终指纹](recording-feedback-20261028-050534/final-source-sha256.json)、[保护边界](recording-feedback-20261028-050534/source-boundaries.json)及[原Preview8/9字节](recording-feedback-20261028-050534/protected-preview-artifacts.json)已核对。普通Debug仅为构建验证，没有新的用户体验APK。

## 数据与停止边界

只改两个生产Compose和四个对应测试文件。VM/Repository/DAO、Owner/generation、Nav3、MoneyMetric、Room5、backup4、金额日期/交易/备份恢复/媒体/提醒规则、依赖及版本签名配置均不变。

未使用ADB、手机、AVD、C3或M22历史验收；MEDIA_DISPLAY_PENDING及M10/M20/M22/M13/G1～G4全部历史NOTE/FAIL/UNKNOWN保留。新源码没有进入原Preview9；本轮提交推送后停止等待独立审查，不自动生成新体验APK、不冻结视觉、不合并main或进入M23。
