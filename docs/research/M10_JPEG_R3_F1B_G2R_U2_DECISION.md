# M10-R3-F1B-G2R-U2 决策与保留风险

2026-10-17。**READY_FOR_REVIEW — M10-R3-F1B-G2R-U2**；仅研究只读预检达到门槛，等待ChatGPT独立审查。

## 本轮证据支持的结论

从已获独立PASS WITH NOTES的U1提交建立独立研究分支，7份执行脚本与U1历史测试输入SHA均已核对，未修改旧研究规则。原AVD在线，无需启动。唯一preflight真实采集两份顺序且独立的完整输入，经原G1/U1流程判为PREFLIGHT_ELIGIBLE；一次结束保护核查的进程、stage及保护项也通过。两次服务均NO_TARGET_RECORD，研究进程结果空，stage命名空间ABSENT；uwb正常映射1083，无新增未知UID或格式问题。

这是**本轮只读观察成功**，取代的是当前研究门禁尚无成功双快照的状态，不改写G2离线、G2-R解析阻塞、U1证据限度或原R3生命周期结论。原G2-R首次未保存的具体UID/SHA仍未知，不追认为本次输入。

## 保留的风险与NOTES

1. 两次一致是受限观察窗口内的语义一致，不是原子租约、持续无消费者、FD关闭或进程死亡证明；后续状态可变化。完整ps原文的hash不同如实保留，不伪装成全列表逐字一致。
2. 本次只覆盖**无目标记录**的服务路径；INACTIVE_RECORD_CANDIDATE真实路径NOT_RUN，不能由宿主合成测试或旧app=null事实替代。
3. 研究stage缺失只能说明观察到的命名空间没有current/pin/receipt，不证明所有历史消费者关闭，不批准任何删除；既有合成owner/trace保留未读。
4. 三个包身份未变，但研究包仍是旧APK。未验证装机签名连续性、更新安装、Outsider拒绝后解绑/销毁、正常Host/Worker Binder/FD/退休等。
5. 原始完整ps/dumpsys仅在内存判定，归档的结构化结果和SHA不能独立重放全部原文；这是明确的隐私边界，不通过补造capture原文绕过。
6. 188条正式元数据及设置相同不等于账本/图片字节相同；私人字节完整性NOT_RUN，没有读取或复制私人内容。
7. 外层回执器只观察同一次subprocess结果；原判定代码不变、返回对象原样交付。未知UID失败的补充记录分支本轮未触发，不宣布该新分支经过系统故障验证。原脚本的legacy finish缺口未修，结束复用已审阅G2保护入口。

## 原状态保持

- F1-A N01持久REVOKED授权语义：OPEN_NOT_TESTED，不测试取消、不修改StageStore。
- N02半清理保守阻塞及未知归属保留规则；N03新Outsider实际连接清理NOT_RUN。
- R3原29场景：**0 PASS / 1 UNKNOWN / 28 NOT_RUN**；完整生命周期BLOCKED。
- 历史attach失败、R1晚取消/120秒超时UNKNOWN保持；本轮不进行native/JNI/allocator验证。
- M10 PENDING，原三项JPEG FAIL；Q05 USER_CONFIRMED，M11/M12完整往返NOT_RUN。
- UI-C1 ACCEPTED / FROZEN — PASS WITH NOTES，原产品回归整体FAIL不改；M11 NOT_AUTHORIZED。
- 原M10/UI-C1/main、原130案例、测试断言、验收矩阵及R0～U1历史证据不改。

## 下一步与停止

提交推送唯一`research/m10-jpeg-strict-r3-f1b-g2r-u2`后立即停止，等待ChatGPT独立验收。即使本次PREFLIGHT_ELIGIBLE，也不自动安装/启动APK，不运行Outsider或normal，不申请或执行其他28场景、取消/SIGKILL/恢复、JNI或生产修复。任何下一项实验必须另获明确授权并重新核对当时设备状态。

交接：[执行](M10_JPEG_R3_F1B_G2R_U2_EXECUTION.md)、[真实结果](M10_JPEG_R3_F1B_G2R_U2_DEVICE_RESULTS.md)、[汇总](m10-jpeg-r3-f1b/evidence/u2-20261017-01/u2-results.json)、[checkpoint](../checkpoints/2026-10-17-M10-JPEG-R3-F1B-G2R-U2.md)。
