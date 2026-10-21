# M19 — 类别分析、年度趋势与客观回顾

**READY_FOR_REVIEW — M19**。来源`fdee68314dc03a6385a70250e3b456423557186b`（M18冻结回写），分支`codex/m19-category-annual-review`。未授予M19冻结或G2发行通过。[契约](../planning/M19_CONTRACT.md) · [结果/唯一方法](m19/results.json) · [原始索引](m19/README.md) · [8图](m19/screenshots/README.md)。

## 已实现与验收

首页类别可进入准确类别；回看接入类别分析和年度回顾。项目当前贡献复用OverviewEngine，历史投入/收回/净额与订阅真实付款分开；年度用occurredOn/paidOn，按当前分类重新计算，0/负值/无记录分别显示。M16三笔原始事实计一次并标注非现金折抵。年份、类别、记录详情/返回及守卫保持，条形图有精确文字替代。读取失败保留匹配旧结果并提示；写入/纠错/删除/改名合并/跨日/前台/换代重算，迟到请求不覆盖新组合。

ReportRepository在原generation协调器＋Room事务中分批读全事实，扩展只读LedgerSnapshot的类型化资金/身份投影；事务结束后后台聚合，不从展示事件字符串猜金额。金额Long分、生产累计addExact/subtractExact；同值合法行不合并，重复/缺少事实、损坏引用及不完整折抵组拒绝整份报告。无新表/备份字段/依赖，Room3、backup2、冻结算法、M16保护和恢复协议未改。

| 原验收 | 实际结果与关键证据 |
|---|---|
| M19-T01 | PASS：8000/800按2026/2028分年；结束后结算、真实Room发生日纠错/删除后重算。 |
| M19-T02 | PASS：13000−3500=9500，0/负净额与空年区分，实际生产累计溢出明确失败。 |
| M19-T03 | PASS：年付归真实付款年，不按未来覆盖拆分；真实Room付款日纠错后重新归年。 |
| M19-T04 | PASS：旧3000折抵收回＋新5000现金/3000折抵投入无重复；订阅另列，页面实际非现金标记。 |
| M19-T05 | PASS：真实Room类别改名及原子合并，2026/2028重分类且原金额事实不变；筛选/刷新/详情匹配。 |
| M19-T06 | PASS：客观金额/状态/事实入口；空年明确“未录入相关记录”，无寿命、评分或建议。 |

跨50读取实测57项目＋57订阅/171资金事实；协调器排队写不混快照。format2合成媒体/设置/资金完整往返后类别、年度一致，旧generation拒读。VM测试覆盖迟到读取、类别合并消失、失败不伪装空数据、跨日/前台刷新；旅程验证只读浏览前后完整快照相同及详情未保存守卫。

## 真实验证与交付身份

- **109唯一JVM PASS**＝新增24＋相关回归85，0失败/错误/跳过。导航RED为1 PASS/2 FAIL，领域RED19 FAIL；首次组合构建的109实际执行通过，但Android测试import歧义导致整体FAIL，已用明确import最小修正并保留原日志。最后复核发现刷新失败后切换选择会清掉旧数据提示，已取得[真实Android RED](m19/android-stale-red/M19ReportAnalysisViewModelTest.result.json)并最小修复，保持错误直到完整重读成功。最终构建exit0/2m42s，109项使用--rerun在最终输入下真实重跑；独立方法总数仍109，不累计各轮执行数。
- **29唯一Android PASS**＝M19 Room7＋VM4＋Compose4＋旅程1（新增16），及M18读取7＋首页6（回归13）。6次类级instrumentation均核对逐方法、结束状态及外层exit0；未重跑历史全量。原始结果[核心](m19/android-final-core/summary.json)、[页面](m19/android-final-ui/summary.json)、[回归](m19/android-final-regression/summary.json)。
- Lint **0错误/23警告**；Debug App/Test构建成功。最终310个构建输入前后摘要同为`0fc64be4dc23f70a185db0183b217e436b242d6c155f3017169722e3ee28e968`，见[构建/输入](m19/stale-green-build/result.json)。
- App `com.cooper.lithic.debug`，code14 / `0.1.0-m19-debug`，13,489,384 bytes，SHA `b4203c3102037752ffb376e4650673942fd43851560db4ced70a8e1d7260f5ac`。
- Test `com.cooper.lithic.debug.test`，2,923,214 bytes，SHA `81e4cf0bfa9119c541e499605fad8d03bcc676d525c7b462e4c198c4a3287a98`；实际Runner `com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner`。
- 原Debug证书SHA `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；设备回读与原物一致，App/Test UID10216/10217不变。APK仅本地`app/build/outputs/apk/`，不进Git。

## 设备、证据边界与继承风险

仅复用重新核验的原5692/API36/x86_64，launcher5688/QEMU34920及创建时间/原boot/隔离磁盘/端口一致；不新建/启动/停止AVD，App/Test各同签名更新两次。6类测试使用全新M19合成根，结束[原实例仍在线、no_backup空、包字节/UID及设置一致](m19/device-end-final/summary.json)。其他设备操作0，未打开默认MainActivity或读取私人账本/图片字节；私人字节比对NOT_RUN。8图来自隔离ComponentActivity，360dp/200%为LocalDensity；恢复为受控文件目标，不冒充MainActivity、系统字号、TalkBack或新SAF验证。Repository部分注入2029业务今天验证2028纠错，页面旅程注入2026-10-21，未改设备时钟。

API26/真机/OEM、Release/长期签名、完整IME/TalkBack、真实午夜/进程终止/LMK/断电仍NOT_RUN。选择意图保存在当前generation模型，不承诺进程死亡持久恢复。完整事实/所有年度聚合开销随数据量及年度跨度增长，50是显示窗口而非计算上限；无限历史性能及最坏取消时延未证明。原M18冻结及全部历史RED/FAIL/UNKNOWN/NOT_RUN/NOTES不改；M10 PENDING/DEFERRED和三JPEG历史FAIL、M13-T06延期、G1未整体通过、Q02完整16组门禁保持。停止等待独立验收，不合并main，不进入M20。
