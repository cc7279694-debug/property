# M18 Android 补充验收

**READY_FOR_REVIEW — M18；等待ChatGPT独立复验，不自行冻结。**

来源 `6dfc4a6c79918d7e6f2e7c40d8b1c9fc2b5943c1`；分支 `codex/m18-total-cost-history`。起始本地/远端一致、工作区干净。原 [离线交付](M18-verification.md)及 `m18/` 字节保持；本轮未重跑174 JVM，也未改生产代码、Room3、backup2、冻结引擎或M16保护。

## 实际结果

| 最终同一Test APK | PASS | FAIL / NOT_RUN |
|---|---:|---:|
| M18TimelineRepositoryTest | 7 | 0 / 0 |
| M18TotalCostTrendViewModelTest | 4 | 0 / 0 |
| M18TotalCostTrendSemanticsTest | 5 | 0 / 0 |
| M18TotalCostTrendJourneyTest | 1 | 0 / 0 |
| **唯一方法合计** | **17** | **0 / 0** |

[逐方法与完整性索引](m18-android/results.json)、[最终原始stdout/stderr及退出码](m18-android/android-final/summary.json)。四次真实instrumentation均exit0、stderr空、预期方法集合与status_code=0完全对应、无忽略，终结有`OK (N tests)`及`INSTRUMENTATION_CODE: -1`；不只看外层退出0。

真实Room验证63条记录、与同快照首页总额一致、协调器排队写不混合事实、同日56事件/M16非现金标识、纠错删除、损坏引用拒绝、format2业务/设置/媒体原字节往返及旧generation拒读。VM验证迟到请求、写后/注入跨日/前台刷新及读取错误。Compose/真实Nav3验证日期输入、超过50条继续查看、详情准确返回、未保存守卫、浏览前后业务事实不变。

M18-T01/T02沿用原JVM数值参照并补真实Room事实；T03真实Room/首页与>50条PASS；T04真实20年绘图/峰谷与精确选日PASS；T05同日事件/非现金/继续查看/详情返回/只读交互PASS。均为实施方限定证据，不等于独立ACCEPTED。

## 最小修正与图像

首次17项为 **14 PASS / 3 FAIL**，[原执行](m18-android/android-initial/summary.json)、[原旅程](m18-android/android-journey-initial/M18TotalCostTrendJourneyTest.result.json)完整保留。原因是Compose测试把完整描述按短文字全等匹配，及未合并Card父节点读取子Text。仅两份androidTest修改：描述改为实际完整文本精确断言；金额仍要求格式化后的10.00精确文本；事件要求该卡片自身后代的原完整说明。原业务预期、方法身份、导航/53条事实/只读断言未删除或放宽。

旅程补实际Repository写入的原创负净成本与月付合成记录，增加谷<0、峰>0、订阅5元/天及同快照首页一致性断言。[10张原始设备PNG](m18-android/screenshots/README.md)覆盖完整总线、拆分与共同数轴、今天三额、>50事件、正负峰谷精确日期、360dp/200%日期输入和返回。每图有原设备路径、run UUID、长度、SHA及尺寸；主执行者已查看关键图像。所有图是plain Application/隔离ComponentActivity的真实Compose/Nav3，**不是MainActivity、系统字号200%或完整TalkBack**。

仅定向 `assembleDebugAndroidTest -Pm13Isolated=true`，[43秒构建成功](m18-android/build-driver-fix/result.json)；App SHA前后不变。原174 JVM PASS、Lint0错误23警告保持历史证据，本次未重跑，未新增依赖/工具。

## APK、隔离与保护

[最终安装证明](m18-android/install-driver-fix/artifacts.json)：

- App `com.cooper.lithic.debug`，code13 / `0.1.0-m18-debug`，13,164,911字节；SHA `a837fd4ae881efbf042587a53b87414a8a94cad935ba74ee99b87e7cce5b8ed9`。
- Test `com.cooper.lithic.debug.test`，2,923,214字节；最终SHA `f2f01b2d77fb4f12d24338a02218c7175a0ce299b4a9a079178580d4f6d4a257`。原 `8b225f...`只属于首次14/3身份，保留未改写。Test不声明业务versionName，以完整SHA标识。
- 两包Debug证书SHA `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；设备回读APK SHA等于已签名核验原件，UID分别10216/10217。Runner为用户指定`M13IsolatedRunner`；未启动默认MainActivity。

新环境 **Lithic_M18_API36_20261021 / emulator-5692 / API36 / x86_64**，独占根 `E:/CODEX/.codex/research-env/lithic-m18-android-20261021`。独立注册、AVD/data、运行目录及snapshot目录，`-no-snapshot`，无旧数据/个人账号。实际写盘路径都核验在新根内；未由占位符推断。创建1次、启动1次、App安装1次、Test初装1次＋定向修正后同签名更新1次；8次instrumentation包含首次RED与最终GREEN，不累加唯一测试数。

[boot/进程证明](m18-android/device/boot-proof.json)、[实际写盘/端口](m18-android/device/host-instance-proof.json)：launcher5688/QEMU34920、精确创建时间/可执行路径/父子关系一致，5692/5693归QEMU，boot UUID `f9583e05-77d0-4308-9ef8-7b7b5656ee7e`。创建exit0；首次启动外层回执未取得仍记NOT_CAPTURED，实际新实例和boot=1独立证实，未补写旧回执、未重复启动。未安装包的`pm path` exit1、未创建no_backup及一次shell引号错误均保留为监督接缝记录，不冒充产品FAIL或静默丢弃。

[结束保护](m18-android/device-end/summary.json)：同一新实例在线、APK/UID/boot/端口保持，no_backup为空，无默认账本；font_scale=1.0、尺寸/密度/旋转与最终测试前一致。仅定向5692，5672、5686、H2/C2/M10及其他旧设备操作0；不读取私人账本、图片或备份，私人字节比较NOT_RUN。未清理/停止设备，保留PNG及必要现场，APK/镜像/许可/密钥不进Git。

## 验证边界与停止

本轮没有发现确定性产品缺陷；剩余为既定NOT_RUN：真机/API26/OEM、MainActivity真实个人账本、正式签名/Release、系统200%/完整IME/TalkBack、真实午夜、进程终止/LMK/断电、无限历史性能。受控文件format2往返不冒充新的SAF或故障恢复。旧generation拒读不扩展为整个UI恢复流程全覆盖。20年样本不证明无界性能或最坏取消延迟；240基础预算之外仍保留边界，真实计算随完整历史增长。

M10 PENDING / DEFERRED及三JPEG历史FAIL、M13-T06 NOT_RUN / DEFERRED、G1未整体通过、Q02完整16组跨模块及全部历史NOTES/UNKNOWN保留。只提交推送当前分支，停止等待独立复验，不合并main、不进入M19。
