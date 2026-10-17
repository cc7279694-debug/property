# M10-R3-F1B-H2-D1 — H2 stage受控恢复提案

2026-10-17。**PROPOSAL_ONLY / RECOVERY_NOT_AUTHORIZED / NOT_EXECUTED**。D1设备调用0，不能把这里的操作顺序当执行许可。当前H2现场没有恢复资格；原recover不能直接使用。

## 1. 锁定现场与证据新鲜度

以下仅来自锁定H2[最终inventory](m10-jpeg-r3-f1b/evidence/h2-20261017-01/h2-final-files.json)，不是今天设备状态：

| 对象 | 已归档值 |
|---|---|
| attempt/current | `586d0904-eaac-403a-895a-48c34cd98810` |
| nonce/generation | `80e16f4e-f17c-47a7-92ba-ce4ccaed751f` / `synthetic-generation-r3` |
| device | 65079 |
| stage/source/guard inode | 590815 / 590829 / 590838 |
| source | 8192字节，owner预期SHA `3faac63d133ee546e983a131136bc44c9d3c0910d1c6b143d60509ef90a386e7`；未由Worker验证实际字节 |
| state/pin | DELIVERY_UNKNOWN / true |
| absent | output、output-owner、receipt、revoked、cleanup及终结proof |
| current/owner/state SHA | `f6a9a3bdafeb2f703a77da27c25f4034a647a58d13f8509d5dd8e0585c0ee50d` / `dc70a49da23001da623de374c8c0f2fe01bafed4846f6c7ad41313da2b2f8c93` / `4224acb2100b3a7914f26d39f3bd995cd05c0f849068e5650fd7f4e4833b5388` |

Host6423在末次H2观察仍存活。Worker6610有UNRESERVED_DEADLINE、Binder死亡、OS及Zygote死亡证据；系统同时记录“Scheduling restart of crashed service ... in 1000ms for connection”。后续NO_TARGET_RECORD只说明采样时状态，不是旧Host、排队bind或全部潜在消费者永久消亡证明。

未来任何身份漂移、当前指针变化、unknown子项或无法解释的新stage都必须暂停，不能把本表强写回设备。D1没有恢复、删除、重置、覆盖这些数据。

## 2. 为什么现有recover不是安全许可

`ProbeRunner.java:55–70`主要等待canAcquireGuard，再closeAllPriorAttempts，最后撤销本Runner的activeSession。返回字符串包含OLD_CLIENT_DEATH_GRANT_FENCE，但没有对应的旧Host死亡检查；本地static锁也不是跨进程旧authority停止证明。Activity.runner确实static，不能伪造同Host多Runner场景作为事实。

保留既有优点：`StageStore.cleanup:132–140`在同一独占lease内验证、持久撤销、删除已知资源、写/验terminal；有效revoked文件会使现有callback授权拒绝。**没有证据说明其cleanup后一定会重新授权。** 真正不足是前置条件没有证明活着的旧Host、未撤销会话、待执行bind、迟到连接及Service重启已收口；失败后晚连接仍注册监听在H2得到强支持。

lease是抽象LocalServerSocket占有，不是consumer.guard文件锁。取得它只排除当时占有同lease的合作消费者；未reserve的Worker和旧Host可以不持它。单次ps无Worker、FAIL报告、worker死亡或瞬时可得lease都不能单独授予清理资格。

## 3. 推荐方案及另行批准事项

**先审查最小失败围栏补丁，再单独批准H2专项恢复。** 不自动重做Worker/Binder/生产媒体协议，不为恢复而清数据、重建generation或放宽StageAdmission。

当前旧APK没有可审查的合作关闭回执。不能假设新补丁已在旧Host内生效。首选未来修正版Host提供合作关闭/仅恢复门禁；对于目前仍可能存活的旧版Host，必须另行批准**仅研究Probe包的受控退役和必要保留数据更新**，并取得真实旧authority关闭/进程终结/连接与重启收敛证据后，才启动修正版的仅恢复模式。这里不提供可运行停止命令，不授权force-stop、kill、安装或任何设备操作。

如果后续无法证明旧Host已合作完全关闭，或无法通过专门授权的包限定退役验证所有旧authority终结，就继续BLOCKED、原样保留。停止命令退出0、安装成功、一次ps缺失、历史PID本身均不等于证明；PID可能复用，需要新取证中的UID/包/SHA、进程实例与匹配系统终结事实。旧版Host不能与新恢复执行方并存并靠进程内锁互相协调。

后续需分开批准：研究Host最小代码/JVM测试及构建；具体退役/保留数据更新方式和范围；仅恢复操作及读写白名单；恢复后独立验收。**normal仍不在这些授权内**。

## 4. 恢复前提（全部必须成立）

1. 有专门恢复授权、锁定修正版研究APK输入/SHA/签名/UID；正式App不操作。H2-N04及N01依赖有真实实现/测试，不能用D1模型代替。
2. 旧Host/会话已合作关闭或匹配终结；旧Worker/FD消费者终结事实齐全；没有未知同UID进程、未完成绑定、待启动/重启Service。未知即停止，不靠等待一次空采样推断。
3. 新执行方是唯一受控研究Host，进入仅恢复模式；禁止普通run/Session创建/FD授权/current发布。所有未来准入入口遵守同一recovery gate；持有恢复owner排他租约，不能把0字节synthetic-main-owner文件当kernel锁。
4. 完整枚举namespace/current及所有stage。除本attempt外必须不存在其他stage，或它们已有完整可验证terminal；不自动清理其他非终结stage。路径、类型、链接、pending与未知子项全部检查，不能只看current。
5. 对本attempt连续持有真实consumer lease，直至撤销、校验、删除、proof与后置核对完成。**不是先检查可得、释放后再清理**。额外恢复owner租约与per-attempt lease仅约束遵守协议的新执行方，不能替代旧Host终结。
6. 在租约内重新核对完整owner、current、nonce/generation、stage/guard/source device+inode、source8192及实际SHA、所有保留metadata和无未知/pending。复用原严格校验；归档预期SHA不是本次实际字节校验。
7. 先持久同owner revoked.json并核对目录同步/回读，授权谓词明确拒绝REVOKED及持久撤销。失败、未知、挂起写入均保留source与pin，不进入删除。
8. Host及Worker的已交付FD分别关闭或其持有进程真实结束；绑定/Service重启围栏仍成立。新/迟到Worker即使被系统物理启动也不得获得任何授权；若产生不明进程或控制状态仍立即停止，不把逻辑拒绝等同实际资源退出。

这些是尚未满足的未来门槛，不是D1检查通过清单。H2当时没有reserve，不能伪造“已释放consumer lease”；未来要重新证明完整前提。

## 5. 未来单一受控操作顺序

| 步骤 | 操作及证据 | 失败停止条件 |
|---|---|---|
| R0 | 获准执行前保留正式APK/UID、设置与私有元数据保护基线；固定恢复operationId、源HEAD和目标owner指纹。 | 身份/授权不符，不接触stage。 |
| R1 | 证明旧authority退役、绑定和重启收敛；修正版仅恢复模式获取唯一owner租约。 | 活跃/未知旧Host或消费者、异常连接/待重启均保留现场。 |
| R2 | 完整namespace校验；取得所需全部stage lease并保持；重核本attempt各inode/源字节/metadata/current。 | 身份、内容、lease或未知子项冲突，禁止删除。 |
| R3 | 持久撤销并验证；再次确认准入关闭、旧endpoint/FD终结和恢复身份未变。 | FENCE_UNKNOWN或无法证明旧控制者已关闭，pin/source保留。 |
| R4 | 只删除明确归属的本attempt已知source/output及已验证pending（本现场本应无pending；出现就先停）。 | 任何删除/同步失败，记录PARTIAL_UNKNOWN并保守阻塞，不重建source或补造proof。 |
| R5 | 资源确实不存在后，写CLEANED_SYNTHETIC/pin=false，再最后写完整cleanup proof并同步/回读。 | pin可能已false但proof未成；仍BLOCKED，不假称所有失败都保持pin=true。 |
| R6 | 保持同lease核验full-owner revoke、terminal、保留metadata SHA、无资源/pending及未知项未动；保留current指向终结attempt。 | 任一后置条件未知不得开放新会话。 |
| R7 | 释放lease，保留原FAIL/trace和新恢复报告；做正式保护比较，停止独立验收。 | 不追加normal、不清其他stage，不用恢复成功重写H2失败。 |

没有SUCCESS receipt的失败attempt应继续**receipt=ABSENT**；不可调用receipt(id,true)制造数据FD成功。既有terminal格式允许receiptSHA256/outputOwnerSHA256为ABSENT，可复用；不需要改变normal成功receipt断言。原stage_check的expected_current正常模式要求成功receipt/output-owner，失败恢复应另设明确的校验上下文：精确检查current/id，再复用通用terminal检查并要求本失败目的无receipt，不能把正常模式要求删掉。

只允许对已完整终结的同owner/current/保留metadata进行幂等“验证已有终结”，不重复删除。此时source应不存在，其原归属由受控原terminal证明承接；不得要求重新读取已删除source，也不得把任意源缺失视为terminal。未完成部分删除没有足够proof时继续阻塞，专门提出后续方案，不能自动修复。

## 6. 设计验证与停止门槛

纯宿主RecoveryFacts将20项前提作为独立输入，每项False/None均拒绝；测试还覆盖lease可得但旧Host未收口、丢失持续lease、待绑定/重启、部分删除、terminal sync失败和幂等。[22方法最终结果](m10-jpeg-h2-d1/evidence/green-final/result.json)只证明抽象门禁，不证明真实进程、socket、FD、fsync或死亡。

未来独立验收必须取得：旧Host及绑定/重启收口证据；正确补丁不接受late hello/grant/result；租约全程保持；identity/SHA/pending/未知资源拒绝；失败恢复不造SUCCESS及业务引用；中断保守结果；最终终结proof与正式保护比较。所有设备项目仍NOT_RUN，真实H2 source/current/pin未经D1修改。

**结论：方案可提交架构审查，当前现场不得恢复。** 若未来无法取得authority关闭及完整资源前提，应保留BLOCKED，而不是以更宽的解析器、超时或临时lease继续。M10 PENDING/3 JPEG FAIL、UI-C1冻结、R3完整生命周期BLOCKED、历史UNKNOWN、N01～N03及M11 NOT_AUTHORIZED不变。
