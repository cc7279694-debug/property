# M22 体验、可访问性与长历史验证契约

来源：用户完整M22指令及5712一次复用补充；起点M21冻结登记 `a367b278bb3186cf6a8f5afdcf7f843e688fbce2`。

- Goal：完善360dp/200%字体、48dp触控、真实状态及精确图表交互，实测完整20年数据与离线UI旅程。
- Scope：现有Compose、只读快照内部计算优化、定向测试与code17内部候选；不更改任何冻结公式或领域接口。
- Constraints：Room5/backup4、金额/日期/状态/M16规则、导航守卫、owner与恢复协议保持；不新增依赖/持久缓存，不合并M10/UI-C1，不进入M23。
- Data：1000项目、1000订阅、20000项目事件(含初始)、10000真实付款；20年、固定seed/日期、合法非重叠覆盖；全量后排序/抽样，不截断参与者。
- Verification：T01窄屏/字号/触控；T02真实状态；T03正式规模计时与只读事实；T04拖动/文字日期与冻结引擎一致；T05图标身份如实；T06运行时断网完整UI创建/修改/结束/续费/图表/备份/确认恢复。
- Device：仅M21/5712原AVD，源码/JVM/构建完成后核验启动最多1次、正常关闭1次；不创建，保留旧截图及15个缓存锁。系统设置原值保存并finally恢复。只用全新带归属标记的M22合成根、plain Application Runner，不打开默认账本。
- Evidence：真实系统字体与LocalDensity、测试宿主与MainActivity、断网与Manifest、语义与TalkBack、受控文件与SAF分别记录。失败不冒称空数据；Debug性能不扩大到真机/Release。原始输出放非build目录。
- Stop：提交推送codex/m22-experience-accessibility，READY_FOR_REVIEW — M22，独立审查前不冻结。M10延期、M13-T06延期、M20额外FAIL/UNKNOWN及所有旧门禁保持。

实际停止点：READY_FOR_REVIEW，Android整体可用性BLOCKED（SystemUI ANR）；正式规模压力与完整UI旅程未通过。设计样本不冒称实测规模，最后测试就绪修正仅编译未运行。见[实际报告](../evidence/M22-verification.md)。
