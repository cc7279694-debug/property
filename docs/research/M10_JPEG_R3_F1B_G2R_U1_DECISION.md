# M10-R3-F1B-G2R-U1 风险与决策

2026-10-17。**READY_FOR_REVIEW — M10-R3-F1B-G2R-U1**。本轮仅研究兼容修复与宿主测试，等待ChatGPT独立审查。

## 证据支持的结论

当前实际ps存在一个旧谓词不支持的ASCII标签`uwb`，出现1次；独立实际UID列为1083，异常行在内存完成USER/PID/PPID/NAME关联。已装研究UID10218/10219及正式UID10216与其不同。

Android 16正式标签[android_filesystem_config.h](https://android.googlesource.com/platform/system/core/+/refs/tags/android-16.0.0_r1/libcutils/include/private/android_filesystem_config.h)第134行定义`AID_UWB 1083`。[来源登记](m10-jpeg-r3-f1b-g2r-u1/evidence/aosp-uid-source.json)记录下载时间、源码13345字节、SHA-256 `ef3a353440f0a1d215d84030577b0a9f6357fa528e9a5495cd4057e7e64b5ea9`及Git blob `b6aded0c78c73f81f2c625713198d1d4054264e9`。先按agent-reach读取网页，再读取官方Gitiles base64源码校验；未引入依赖或下载二进制。源码说明数值身份，设备USER/UID同行观察说明本环境的标签映射，两者不能相互替代。

因此选择**精确增加uwb→1083**，复用原USER列和原采集流程。没有将所有u开头标签视为系统用户，没有忽略所有未知UID，也不引入第二次ps作为门禁fallback。小型共享函数供真实`process_rows()`与宿主证据审计使用，避免写两套映射；原ServiceRecord、stage、双快照、包身份、完整性及研究进程检查不变。103项宿主方法通过，其中原81项身份和断言全部保留。

## 不采用的路径

- **扩大通用正则或忽略未知标签**：无法确定任意身份，可能隐藏研究UID/Worker冲突，拒绝。
- **全预检改用数字列并跨采样合并**：本次无需扩大采集协议或引入全列表竞态；只用已证实的一个固定映射。数字列仅作为本轮独立诊断证据，不授予任何设备资格。
- **清理系统服务/旧stage后重试**：不属于本轮，也不能证明原状态安全，不执行。

## 保留的证据限度与风险

1. **不能追认原G2-R失败token。** 首次输入没有保留UID原值和SHA。新观察证明一个同类可复现缺陷，不证明当时一定是uwb；本轮SHA不能补成历史输入。
2. **完整设备门禁尚未重跑。** 只对异常uwb行做跨采样关联；数字采样时研究行为空不等于全时段无Worker，也不等于两份完整进程状态一致。未采新Service/stage信息，没有PREFLIGHT_ELIGIBLE或安装许可。
3. **隐私优先限制独立重放。** 完整ps只在内存，无关进程名称/PID不保存。受限JSON审计验证保留字段与共享映射一致，不能通过哈希恢复原文或重新验证所有私有行。
4. **其他未支持身份仍会保守阻塞。** 遇到新的u前缀或冲突字段应重新独立取证，不能自动扩白名单。既有非u前缀platform fallback保持原行为，本轮未对全部Android UID类型作安全审计。
5. **诊断工具前置失误不隐藏。** 额外空行、安装路径模式错误导致三次ps之前停止，记录保留；未消耗额外ps，也未改变设备。01脚本SHA及初始5条身份显示原始输出SHA缺失如实登记，不补造。

## 原状态不变

- N01持久REVOKED授权谓词：OPEN_NOT_TESTED，不测试取消、不修改StageStore。
- N02半清理保守阻塞/未知归属保护继续；N03新Outsider实际连接清理仍NOT_RUN。
- 原R3 29场景：0 PASS / 1 UNKNOWN / 28 NOT_RUN；R3完整生命周期BLOCKED。
- 历史attach失败、R1取消/120秒超时UNKNOWN保留，B1生产可行性仍未获证明。
- M10 PENDING，原三项JPEG FAIL；Q05 USER_CONFIRMED，M11/M12完整往返NOT_RUN。
- UI-C1 ACCEPTED / FROZEN — PASS WITH NOTES，原整体回归FAIL不改；M11 NOT_AUTHORIZED。
- 正式产品、M10/UI-C1/main及R0～G2-R历史报告/证据保持；原130案例及验收登记不改。

## 下一步与停止

只提交推送`research/m10-jpeg-strict-r3-f1b-g2r-u1`。随后停止，交ChatGPT审查本次映射、RED/GREEN及证据限度。只有新的明确授权才可运行完整设备只读预检；本轮不自行运行，更不开放安装、Outsider/normal、JNI或生产修复。
