# M13 — 首页汇总与核心内部候选

2026-10-20用户正式授权；来源`2e298ceb87a30d436a64180ccaf7d819e55ae005`，分支`codex/m13-home-core`。继承原M13计划及PRODUCT/UI/DOMAIN规格，不改归档原件。

- Goal：真实当前日均总额、长期项目/订阅小计及各自类别构成；写后、前台跨日、返回前台、恢复换代后刷新。
- Scope：复用M02/M03的纯汇总引擎；同一协调器及Room只读事务中的有界批量读取；首页与现有导航连接；定向合成测试、截图与内部Debug APK。
- Constraints：Room v2、format v1、冻结算法、业务写入语义不改；不合并M10或独立UI-C1分支，不新增依赖/联网/年度统计/类别管理。M10仍PENDING / DEFERRED，三历史JPEG FAIL与全部NOTE/UNKNOWN/NOT_RUN保留。
- Read contract：一个业务日期、一个一致快照；逐批读取完整身份关联事实，聚合未舍入引擎结果。错误不伪装空库；旧代或旧请求的迟到结果不能覆盖新读数。0/负值合法，存在记录与空库分开。
- UI contract：唯一主视觉“当前日均成本”，两类小计与类别第二层，保留原入口和Nav3守卫；说明当前有效口径及非实际扣款，说明分项舍入差异。无记录显示—；有记录的零成本显示0。
- Verification：原M13-T01～T05用定向JVM、Room/生命周期/Compose合成验证。T06核心流程子项可验证，但稳定签名安装、首次真实记录及正式发行门槛继续NOT_RUN，不以Debug代替。
- Device：用户本轮补充许可只复用`Lithic_M11B_Final_API36_20261020 / emulator-5686`原合成实例；核对原实例、boot、APK/UID与默认账本保护基线后，同签名更新M13 App/Test。在新隔离账本和测试宿主验证/截图，不打开默认owner，不覆盖原账本。实例消失则设备NOT_RUN，不重启/新建AVD；不接触H2/C2/M10/其他设备，不kill/force-stop/清数据。
- Stop：提交推送后READY_FOR_REVIEW，等待ChatGPT独立验收；不自行冻结、合并main、发布或进入M14。
