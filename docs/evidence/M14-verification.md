# M14 — 类别、名称搜索与列表效率

2026-10-21；来源 `2c5e23a7af713f6a96b345bce2a90e613458f270`，分支 `codex/m14-category-search`。**READY_FOR_REVIEW；未经独立验收，不冻结。**

## 实现

- 两套类别新建、改名、同类型合并；默认“其他”受保护。合并先预览含历史的影响数量，再在原coordinator/Room事务中核对源、目标和数量，移动全部引用后删源；错误回滚。未知提交保留同一UUID/参数与导航围栏，源缺失不冒称历史合并成功。
- 首页顶部轻入口只搜名称，覆盖当前/历史项目和订阅，状态复用M02/M03；`%`、`_`、引号、`!`作为字面输入，绑定参数，不引入FTS。type/ID游标、每页50+1；查询/写入变更重建同一事务内的已加载窗口，迟到请求不能覆盖新查询。
- 设置提供类别管理；保持三个底部入口和原详情/历史栈。类别写后通过既有invalidation刷新首页、列表及详情；恢复换代清旧ViewModel。当前项目startedOn升序；历史项目按冻结持有天数降序，同长按ID，订阅保留真实paidOn降序。
- schema v2、backup format v1、金额/日期引擎、恢复协议、依赖和权限未改。只将内部Debug版本递增为code9 / `0.1.0-m14-debug`。

## 实际验证

| 范围 | 结果 |
|---|---|
| JVM | **136个唯一方法PASS**，0失败/错误/跳过。113项冻结引擎/首页/日期回归＋23项类别标准名、LIKE输入、实际生产controller异步围栏和导航测试。[XML](m14/jvm/)、[统计](m14/results.json)。 |
| RED→GREEN | 输入规则4/4 RED；controller与导航13执行/10失败（其余既有行为保留PASS），最终全部GREEN。首轮并行编辑时KSP产物过时导致编译失败，单列为构建失败，未计行为RED。[原始日志](m14/logs/)、[RED XML](m14/red/)。 |
| 宿主SQLite | 直接提取实际DAO SQL：5个LIKE字面/注入输入，105条同持有期跨页唯一，删除触发器失败完整回滚、合并只改引用且无外键孤儿。另有“早买早结束”旧排序RED→新排序GREEN。**HOST_SQLITE_ONLY，不替代Android Room。** [结果](m14/host-sql.json)、[可复核脚本](m14/host-sql-check.py)。 |
| Lint/构建 | 0 errors / **9 warnings**（既有依赖/VisibleForTests/未用资源提示）；Debug App/Test均成功。首轮Android测试错误导入及后续新增Composable命名警告已修正，旧日志保留；最终37秒构建成功。[最终日志](m14/logs/final-test-style-build.log)、[Lint](m14/lint.xml)。 |
| Android/Room/备份往返 | **28项源码编译通过，实际全部NOT_RUN**：类别事务11、搜索/排序12、完整format1类别/七表/设置/媒体往返1、Compose交互4。原5686实例已消失，用户许可明确不重启/新建。运行中身份、真实Room回滚/恢复新代刷新、窄屏/大字体/键盘和截图均未验证。[主机只读检查](m14/device-availability.json)。 |

最终App：12,771,695字节，SHA-256 `59d45ad38b467e5c4bb895c71feab3315bb872d670177ab7457033179c4b35b9`。Test：2,546,071字节，SHA-256 `9609270b7dde53a91bb4b57f54e763f427625d382f574cda31e818f8dce8d4e4`。两者为既有Debug证书`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，App身份`com.cooper.lithic.debug`；权限/备份排除与Test instrumentation已核对，非正式签名版。[产物身份](m14/results.json)、[APK核查](m14/apk/)。APK只保留本地忽略目录，不进Git。

## 交接与限制

主要差异：CategoryRepository/CategoryDao与类别页面、SearchRepository/SearchDao及搜索页面、历史排序游标、同代状态装配和Nav3守卫接入。当前未发现未解决的确定性代码失败；**真实Android/Room/备份恢复及运行截图是下一验收缺口**，不据宿主PASS宣布产品流程通过。本地辅助审查仅用于修正，不代替ChatGPT独立验收。

ADB、AVD创建/启动/停止、安装及清理均0次；H2/C2/M10/旧账本不访问。M13-T06 NOT_RUN / DEFERRED、G1未整体通过，M10 PENDING / DEFERRED、三历史JPEG FAIL、UI-C1独立冻结、Q03～Q06和全部历史NOTE/UNKNOWN/NOT_RUN保持。仅提交推送M14后停止，不合并main、不开始M15。
