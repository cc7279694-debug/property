# 砺石 UI-V3 · 项目与订阅详情信息层级

工作分支 `codex/lithic-ui-v3-detail-information-hierarchy-02`。基线 `4b2cce383cdf7e2b1e1bb6ead9e94643c4ad60f4` 与远端一致，其 App 树与冻结生产提交 `61471857d4398f1ab9e5f08512265dd6c5ed0617` 一致。实施提交和桌面产物的对应关系见验证段。

## 样图与实际调整

用户提供的原 PNG SHA-256 为 `235867260b7a96d179e3c0014cda6921cbfde12ad75c67cbac4d18469fc9bd7b`。左侧是订阅详情完整样图。实现沿用其阅读顺序、雾蓝卡、套餐行、三笔付款及固定主按钮，同时使用已有服务封面或类型图标、真实状态和账本金额。右侧提供项目详情结构，项目视觉细节延续原有组件。三套主题沿用各自的颜色角色，包括黑灰主题原有浅色主按钮。

| 页面 | 改造前 | 改造后 |
| --- | --- | --- |
| 项目 | 主成本卡后并列三张财务卡，默认显示传入列表头部，日期、备注和成本历史直接占据正文 | 4:3 Fit 封面及只读查看保持。名称、类别、状态与持有时间紧凑排列。主成本卡后为默认收起的财务明细，仅直接显示投入和收回。金额记录默认显示最近三条，更多资料按需展开 |
| 订阅 | 指标、套餐、付款行和次要资料直接展示，付款首屏直接使用已加载的整页 | 历史平均日成本是唯一主卡。当前套餐、已确认的下一期分别点开查看。付款历史默认显示已加载的最近三笔。单笔事实和操作通过点行进入，其他资料另行展开，原「继续读取付款历史」保留 |

[三主题的前后结构对照及适配示意](lithic-ui-v3-detail-information-hierarchy-02/detail-hierarchy-structural-comparison.svg)。该图为桌面结构示意。360dp、400dp 和 200% 区域描述布局约束，实际尺寸、字体、滚动和截图仍以 Android 运行证据为准。

较早项目版有原生截图可用于比较历史布局：[纯白](lithic-ui-v3-project-detail-repair-02/android/project-detail-pure-white.png) · [岩白雾蓝](lithic-ui-v3-project-detail-repair-02/android/project-detail-stone-mist.png) · [深色黑灰](lithic-ui-v3-project-detail-repair-02/android/project-detail-charcoal.png) · [真实 200%](lithic-ui-v3-project-detail-repair-02/android/project-detail-font-200.png)。这组图对应较早提交，其颜色和版式细节可能与本轮开始时已有差异。

## 事实、操作及隔离

- 项目默认取 Repository 已按发生日和事件 ID 升序提供的最后三条，展开复用原列表与顺序。投入、收回、净投入复用原成本结果。非现金折抵在相关金额和记录中继续明确标记，单笔原日期、方向、依据、备注、关联与纠错仍可达。结束关联收回继续进入原专用修正，换新关联限制保留。三处展开状态按项目身份与既有 presentationIdentity 重置。
- 订阅默认取分页状态中已按真实付款日倒序、同日 ID 升序的前三条。「全部」仅展开已加载内容，下一页另行请求。记录数待取得可信读取结果才显示。当前套餐内仍可见标准日均价及覆盖边界；下一期仅取截至观察日已付款、且尚未生效的最早一笔。历史平均日成本、标准日均价及累计实际付款各自使用原数据源。无有效套餐继续显示该结果及当前贡献。
- 单笔付款仍显示真实付款日期、金额、周期、实际生效、到期边界与提醒日。纠错和删除误录仍交给原回调，最后一笔付款保护仍由原流程处理。原订阅暂停、恢复、停止、重新开通、计划停止及纠错规则保留，管理面板继续显示服务商自动续费提示。读取失败、旧快照及相应动作禁用仍有对应反馈。
- 封面读取和放大复用原接口。项目「记一笔」、订阅「记录续费」继续固定于滚动区下方并调用原业务路由。生产差异仅涉及两个详情页文件。保留 Room、备份、金额与日期计算、导航守卫、Owner、封面协议、首页、三主题令牌及正式发行配置。

## 测试接口

- 项目新增 `project-financial-summary`、`project-financial-expand`、`project-financial-details`、`project-financial-{outlay,recovery,net}`、`project-more-details-expand`。保留原金额、记录和成本历史入口标签。
- 订阅新增 `subscription-detail-{current-plan,next-plan,show-payments,payment-count,more,other-content}` 及单笔事实标签。原 `subscription-renewal-actions-<id>` 现在落在整行；旧 `subscription-renewal-date-information-<id>` 改为从该行打开事实对话框。原金额、纠错、删除、分页及管理回调仍有直接覆盖。
- 直接受新层级影响的 AndroidTest 先执行展开、点行或打开套餐再做原业务断言。新增项目最近三条顺序与身份重置、订阅同日顺序及仅显式分页、三主题模拟大字号固定操作的源码覆盖。LocalDensity 模拟与 400dp 请求宽度都需要 Android 运行才能确认效果。

## 验证结果

固定源码 `e19e59d9c4038ee365b119618fc331f17d317a6e`，App 树 `6b8eb5233aabdd73f558150748c33bbc92704ffb`。桌面会话从 4.131 GiB 可用内存启动，Gradle 单次退出码为 0。独立子代理只读审查未发现确定性生产阻断。该审查仅覆盖源码。

| 验证 | 结果 |
| --- | --- |
| 定向 JVM | 9 类，97 PASS，0 FAIL／ERROR／SKIP。覆盖项目展示与事件标签、项目成本与生命周期、订阅成本与累计、订阅来源、金额展示及主题 |
| 编译与构建 | 生产 Kotlin、AndroidTest Kotlin、两个 Debug 构建通过。交付一个 App APK。16 个改动的 AndroidTest 文件中共索引到 74 个方法，新增 3 个、移除 0 个 |
| Debug Lint | 0 错误，20 警告。两个生产改动文件没有 Lint 诊断 |
| 运行验证 | AndroidTest、两页真实三主题、360／400dp、系统 200%、TalkBack、滚动、固定操作、付款分页和弹窗返回均为 NOT_RUN。旧隔离 API 36 曾出现系统启动 ANR，本轮没有重新启动，也未安装或连接设备 |

[构建日志](lithic-ui-v3-detail-information-hierarchy-02/build.log) · [构建输入](lithic-ui-v3-detail-information-hierarchy-02/build-start.json) · [JVM 汇总与原始 XML 索引](lithic-ui-v3-detail-information-hierarchy-02/desktop-summary.json) · [Lint](lithic-ui-v3-detail-information-hierarchy-02/lint-results-debug.txt) · [修改文件及 Android 方法索引](lithic-ui-v3-detail-information-hierarchy-02/source-scope.json)

Git 中的构建日志仅去掉行尾空白。本机原日志保留在交付目录上一级。

## 本机 Debug APK

`E:\CODEX\lithic-ui-v3-detail-information-hierarchy-02-local\verified-debug\Lithic-Detail-Hierarchy-02-debug.apk`

| 项目 | 核验值 |
| --- | --- |
| 包名与版本 | `com.cooper.lithic.debug`，`0.1.0-m22-debug`（code 17） |
| 大小 | 20,590,822 字节 |
| APK SHA-256 | `4267e89e569194b51a69cc1da9034296798fa9fec3693d5db804dec763d3b24d` |
| Debug 证书 SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9` |

App/Test APK 的原 Debug 证书 v2 验签通过。完整解出的 Manifest 和权限输出分别与基线 App/Test 一致，无 Internet 权限变化。DEX 核验了两个详情页、付款分页测试代码与两个关闭的 Debug 照片写入开关。基线 App/Test APK 哈希与原报告一致。仅归档 App APK，实物与构建产物逐字节相等。Test APK 留在编译输出目录作为验证记录。GitHub 随分支提交源码和证据。

[APK 身份](lithic-ui-v3-detail-information-hierarchy-02/apk-identity.json) · [完整 Manifest 与签名](lithic-ui-v3-detail-information-hierarchy-02/apk-manifest-and-signature.txt) · [DEX 核验](lithic-ui-v3-detail-information-hierarchy-02/dex-audit.txt)

**READY_FOR_INDEPENDENT_REVIEW**。样图贴合度和设备侧交互待 Android 截图与运行复核。
