# M10-R3-F1B-G1 门禁决策与边界

状态：**READY_FOR_REVIEW — M10-R3-F1B-G1**，等待ChatGPT独立验收。只完成研究脚本及宿主验证，没有Android预检或生命周期通过结论。

## 采用的最小结构

`完整只读dumpsys capture → ServiceRecord分类 → 独立进程/已装身份/stage事实核对 → 第二次独立快照一致 → PREFLIGHT_ELIGIBLE（仅研究观察窗口）`

1. 不再用全局`.WorkerService`字符串否决。精确匹配`com.cooper.lithic.m10r3.probe/.WorkerService`和当前active分区；排除全局Last ANR历史及其他Service记录。未知头/段/字段、重复或冲突记录、pending/executing/restarting/绑定/关联app均阻塞。
2. `app=null`只是必要字段之一。完整的已知32行目标profile满足字段与身份约束时只能成为`INACTIVE_RECORD_CANDIDATE`，不是资格。无目标记录也必须有完整受支持dump；简略/裁剪文本不允许推断字段缺席。
3. 资格还要求：固定emulator-5596/原AVD/API36/boot1，正式/Probe/Outsider三个不同已装UID及锁定APK字节、Probe服务baseDir等于其唯一安装codePath；严格ps行/UID/进程检查；原stage所有权/receipt/inode/终结检查；两轮独立ID、顺序时间及语义状态一致。
4. 正常正式App进程不是研究消费者；它与研究UID碰撞仍阻塞。目标ps、同研究UID未知进程、UID矛盾、未知app UID格式、空ps或截断失败均阻塞。相对age显示自然变化不算服务状态变化，recordId/包路径/UID/进程/stage事实变化必须停止。
5. 监督器仍只有只读入口；判定过程中不解绑、不停进程、不清ServiceRecord、不删文件。不增加安装/启动入口，不以资格自动运行实验。

## 官方格式依据

实际读取官方AOSP `android-16.0.0_r1`，frameworks/base提交`99b01a65cc4c104933788b3143285ab6bae65827`。

- [ActiveServices当前分区与Last ANR输出](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/services/core/java/com/android/server/am/ActiveServices.java#7786)：历史Last ANR在当前分区之前；[完整/简略模式](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/services/core/java/com/android/server/am/ActiveServices.java#7886)字段不同；[nothing输出](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/services/core/java/com/android/server/am/ActiveServices.java#8153)是空集合终态。
- [ServiceRecord条件输出](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/services/core/java/com/android/server/am/ServiceRecord.java#1038)：Bindings、非零执行/重启指标、非空启动列表等按条件输出，只有完整已识别格式才可利用其缺席；[记录头](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/services/core/java/com/android/server/am/ServiceRecord.java#1772)里的用户号与caller不能替代实际Worker UID。
- 读取字节：ServiceRecord.java 83,711B/SHA `998139068061b562623a467daafddf1ad949149845d5251d491d38f31d4ec051`；ActiveServices.java 463,612B/SHA `64df5e8de4b4d99572f139ab27fe49ef6b542d0daf6b75304b4a3ae41204eedc`。这是官方格式研究，不是模拟器Framework精确构建版本证明。

## 可信采集与剩余风险

- dumpsys没有天然结束标记。完整性依赖可信监督器正常收齐stdout、退出0/未超时/无stderr、严格UTF-8、字节数/hash、必需头/字段与合法段边界。hash防偶然截断，不认证任意外来文本；不能给裁剪文本补一个`complete=true`就把它变成设备事实。未知格式继续保守阻塞。
- 完整dumpsys和全ps原文只在内存里分类，发布范围结果与字节/hash，不归档无关应用内容或Last ANR原文。异常原因也不复制私人账本/图片。
- 两次一致只证明观察窗口一致，**不是原子租约、持续无消费者、FD关闭或进程死亡证明**。仍可能在快照后变化；任何后续安装或运行需要独立新授权和当时预检。不可将HOST_SYNTHETIC资格写成Android实证。
- 严格API36 profile有意拒绝未知版本、未知字段/UID格式、组件别写与不完整状态；可能继续保守阻塞。后续遇到新格式应先保存非敏感证据和独立审查，不能自动放宽。
- 历史F1B裁剪资料及缺失的首轮stage仍不足，保持BLOCKED；没有从回放宣布该设备现在安全。安装签名连续性仍NOT_RUN。

## 不采用的做法与后续

未简单删安全条件、未仅凭app=null放行、未把瞬时guard可取得视为收口；未修改Worker/StageStore/清理协议、未补造Android事实。原F1A N01持久REVOKED授权语义仍OPEN_NOT_TESTED，N02保守残留阻塞、N03实际连接清理未验证。

提交推送G1后停止，先等待ChatGPT审查脚本及[81项宿主证据](M10_JPEG_R3_F1B_G1_TEST_RESULTS.md)。本轮不申请也不执行安装、Outsider或normal。原M10 PENDING/3 JPEG FAIL、R3生命周期BLOCKED、UI-C1冻结与M11 NOT_AUTHORIZED不变。
