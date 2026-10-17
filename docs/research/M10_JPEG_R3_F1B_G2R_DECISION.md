# M10-R3-F1B-G2-R 风险与决策

2026-10-17。**BLOCKED — M10-R3-F1B-G2-R**。

## 已证事实与停止原因

本轮按新授权完成一次原AVD冷启动和设备身份确认，随后只运行一次锁定G1只读预检。首轮服务完整输出被原分类器判为NO_TARGET_RECORD，但`process_rows()`抛出`unknown ps UID label`，原监督器退出2并停止。第二轮与最终PREFLIGHT_ELIGIBLE未执行。没有放宽规则、过滤未知行后继续、重新预检或安装/启动任何研究App。

环境上线成功与门禁通过是两个结论。本次不再是G2的device-not-found阻塞；原[G2失败记录](M10_JPEG_R3_F1B_G2_DEVICE_RESULTS.md)保持原字节和当时状态，不被本轮替换成PASS。

## 证据限度与未决项

1. **UID标签问题未定位到具体原值。** 现有G1失败分支未保存未知标签或首轮ps SHA，只保留返回码/21,913B及错误。不能凭经验断言是哪种Android UID，不能把结束ps当首轮原输入。源码位置已明确为`gate.py/process_rows()`的UID标签检查，但格式兼容性结论仍待取证和独立审查。
2. **没有两轮合格快照。** 首轮包身份、服务和stage原始stat已采集，进程检查阻塞。ending仅是结束保护，不能算第二轮预检或拼成组合资格。
3. **服务分类范围有限。** 本轮实际覆盖NO_TARGET_RECORD，没有真实INACTIVE_RECORD_CANDIDATE、绑定/重启/活跃Worker情形的Android实证。G1宿主测试保留原效力，本轮未重跑。
4. **阶段文件与进程死亡不同。** 结束stage namespace ABSENT不证明所有消费者都已安全关闭，不关闭REVOKED语义风险。进程解析仍BLOCKED，不能宣称已证明没有活跃Worker。
5. **数据保护是有限观察。** 本次正式APK/UID、188条私有元数据聚合及设置前后相同，三包身份核对一致。没有读取账本/图片字节，不能扩展为业务表或封面字节完整性PASS。
6. **AVD历史范围明确。** 继承F0锁定的现有数据目录及配置SHA；不是M01以来现场连续性证明。只按禁snapshot命令行冷启动，不改配置/数据，不宣称从日志摘要独立证明所有内部行为。
7. **原始输入归档受限。** 完整ps/dumpsys和正式App私有路径不归档；保留受限结果、返回码、可用字节数/SHA。缺失的hash、标签或原文不补造，审阅者无法从当前归档独立重放所有输入。

## 本轮决策

保持锁定G1规则和所有旧文件，停止设备操作，提交G2-R环境及门禁失败证据。原模拟器保持上线，不终止、不重启、不清理，不自动开放安装、Outsider/normal、取消或其他28项场景。

下一步仅等待ChatGPT独立审查。可由后续明确授权决定是否做最小只读UID格式取证（仅未知标签和必要研究身份，不归档无关应用名称），然后再判断是否需要单独的监督器兼容性修改。本轮没有开展该诊断或修复，不因门禁过严而自行跳过安全条件。

## 继承状态

- F1A N01持久REVOKED授权语义：OPEN_NOT_TESTED；StageStore未修改。
- N02半清理保守阻塞、未知归属残留保护继续保留；N03真实Outsider连接清理仍NOT_RUN。
- 历史attach及R1 120秒超时UNKNOWN不关闭；R3完整生命周期BLOCKED，原29场景0 PASS/1 UNKNOWN/28 NOT_RUN。
- M10 PENDING，三项JPEG拒绝FAIL；Q05 USER_CONFIRMED，M11/M12完整往返NOT_RUN。
- UI-C1 ACCEPTED / FROZEN — PASS WITH NOTES；M11 NOT_AUTHORIZED。

实际结果见[设备报告](M10_JPEG_R3_F1B_G2R_DEVICE_RESULTS.md)与[结果JSON](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/g2r-results.json)。本文件不授予下一阶段或生产接入许可。
