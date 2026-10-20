# M13 — 首页与核心内部Debug候选

2026-10-20，来源`2e298ceb87a30d436a64180ccaf7d819e55ae005`，分支`codex/m13-home-core`。**READY_FOR_REVIEW；未经独立验收，不冻结M13。**

## 交付与边界

首页已显示真实当前日均总额、长期项目/订阅小计及各自类别构成，保留三栏及原录入、列表、详情和恢复守卫。金额复用M02/M03，分类使用未舍入结果；支持负值、0、历史/暂停/到期/预付生效边界。空库显示—，有记录但0元显示0，不从金额判断空库。

新增`OverviewEngine`、`ReportRepository`及前台日期观察器。读取在原coordinator的一次Room事务中、按50个身份分批读取完整关联事实；日期锁内取一次。七表变更、返回前台、前台跨日和M12换代触发刷新，旧请求/旧代迟到不能覆盖新结果。错误保留明确标注日期的上次快照，不伪装0。DAO只加孤儿事件只读查询；Room schema v2、format v1、冻结算法/写入语义、依赖和权限未改。

## 实际验证

| 原案例 | 本轮结果与证据 |
|---|---|
| M13-T01 同引擎 | PASS：纯聚合对照M02/M03；真实Room与UI创建/追加/订阅后总额变化。 |
| M13-T02 当前有效 | PASS：0/负、未来/已结束项目、PAUSED/EXPIRED/STOPPED、预付款与生效日；无虚构付款。 |
| M13-T03 类别一致 | PASS：精确类别和=类型小计，两小计和=总额；保留真实0/负类别、类型隔离、分位差说明。103项目＋103订阅覆盖跨批读取与并发一致性。 |
| M13-T04 空与0 | PASS：空库、已录入0、正负抵消、全不活跃、读取失败分开验证。 |
| M13-T05 刷新 | PASS：写后invalidation、真实前台循环与Activity停止/返回、新owner恢复、旧代迟到拒绝。跨日使用可注入BusinessClock，不改设备日期/时区；前台保留真实最长60秒观察周期，未把模型当设备午夜实测。冷启动初始读取经新holder初始化覆盖，未另测进程强杀。 |
| M13-T06 核心/稳定版 | 合成核心UI子流程PASS；**原案例整体NOT_RUN**：稳定生产签名安装/首次真实录入及升级门槛尚未执行，Debug不替代。见[内部版闸门](../INTERNAL_RELEASE.md)。 |

最终同一App/Test身份：**113 JVM＋36 Android＝149个唯一方法PASS，0失败/错误/跳过**。其中新增27 JVM、35 Android，另含1项既有跨代迟到回归；不是原历史全部测试重跑。计数与源码方法/原JUnit输出逐一核对。[结果](m13/results.json)、[JVM XML](m13/jvm/)、[Android原始结果](m13/android/)、[17项源码Git字节SHA](m13/inputs.sha256)与[构建时工作树字节SHA](m13/inputs-working-tree.sha256)（6个既有文件受autocrlf换行转换，分别列证，不冒称字节相同）。

纯引擎真实RED为23执行/22断言失败，随后23 PASS；日期观察器4项RED→GREEN。首轮测试夹具语法失败单列，未算RED。[日志](m13/logs/)。定向构建命令：`:app:testDebugUnitTest --tests com.cooper.lithic.domain.* --tests com.cooper.lithic.core.* --tests com.cooper.lithic.app.* :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest -Pm13Isolated=true --offline --max-workers=2`，最终1m02s成功。后续只修截图测试宿主，再Lint/Test构建34s成功；App字节未变。Lint **0 errors /9 warnings**，未升级/隐藏依赖提示。

核心UI旅程：Jan10创建100元Jan1项目→追加20元，首页10→12；150元月付后17；设置导出完整归档，再暂停后12；恢复预览显示当前1条状态/备份0条，确认覆盖才换代，Jan11首页15.91（项目10.91/订阅5.00）。核对2金额事件、1付款、无暂停、记录时区；选择结果通过真实ViewModel/服务接入合成私有文件，**没有声称本轮新测系统SAF**。M11/M12既有真实SAF及登记图片往返冻结证据继承，未重跑。

## 截图与设备保护

用户明确批准后，仅复用`Lithic_M11B_Final_API36_20261020 / emulator-5686`同一原实例（launcher1284/QEMU29348及创建时间、端口、boot逐项核对）。App同签名更新1次、Test更新2次；创建/启动/停止AVD、kill、force-stop、清数据均0次。仅plain Application runner＋ComponentActivity及新私有合成owner，未启动MainActivity/default owner；H2/C2/M10与其他设备不操作。

前后默认账本/旧证据哈希元数据聚合均`43c8082f41f5b50c88dfb5742de29ba662cabd1f66c5f4297361e1b538bbccd6`，App/Test UID保持10216/10217；设备设置不变，结束无目标App进程、无m13合成账本残留，原AVD继续在线。未归档私人账本/图片字节。[最终保护证明](m13/device/end-proof.json)。

截图全部来自**隔离ComponentActivity中的真实HomeShell**，合成相机/电脑/月付事实由冻结引擎计算，并非MainActivity或真实用户记录。画布360dp，按设备可见像素映射测试密度；大字体fontScale=2，不改系统字体。[有数据](m13/screenshots/data-light.png)、[下半页/类别与入口](m13/screenshots/data-lower.png)、[空库](m13/screenshots/empty-dark.png)、[360dp/200%](m13/screenshots/large-text.png)。已人工查看最终四图、检查边距与可滚动入口。

初稿测试宿主360dp超出物理画布导致边缘裁切，已仅修测试宿主并加未裁切像素边界断言；旧四图与首轮完整执行保留在本地`build/m13`。最终36项用最终Test重新执行，不混用旧包PASS。一次辅助窗口筛选返回1也保留本地，未据此截图或冒充门禁PASS。

## 产物与剩余门槛

App：`app/build/outputs/apk/debug/app-debug.apk`，**12,830,694字节**，`com.cooper.lithic.debug`，code8 / `0.1.0-m13-debug`，SHA-256 **`6b4925409db634a126dfc56cddd6fb345d0692856c6691e5eb6017491ec668da`**。最终Test：2,588,328字节，SHA **`4ff081b6cf12d9d965f1d7c9fb70df3f8cf10045c3703eea55c39db9c55c4ba3`**；初稿Test `b90363022c0a4eaa60b429505e38e1dd148d6d592b752e9456e4e94fa7e64918`仅作历史。两者证书、实际已安装SHA/UID、Test instrumentation已核对；无网络/禁止权限，系统备份九域排除。[APK核查](m13/apk-verification.json)、[产物](m13/artifacts.json)。APK只保留本地，不进Git/不公开发布。

本轮实现/定向验证无未解决失败。**T06稳定签名/Release/长期真实录入仍NOT_RUN**，真机/API26/OEM、完整TalkBack、真实LMK/断电等未扩展。批处理减少历史同时驻留，未声称长期性能基准或恒定计算时间。M10 PENDING / DEFERRED、三历史JPEG FAIL、UI-C1独立冻结、Q03～Q06及所有旧NOTE/UNKNOWN/NOT_RUN保持。提交推送本分支后停止，等待ChatGPT独立审查，不合并main、不自动M14。
