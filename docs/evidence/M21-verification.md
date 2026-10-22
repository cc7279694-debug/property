# M21 — 长期里程碑与低打扰控制

2026-10-22；**READY_FOR_REVIEW — M21**，独立验收PENDING。来源M20冻结`3e8f5fdef9853ada726b037cc8559e2117d24da9`，分支`codex/m21-milestone-reminders`。[本轮契约](../planning/M21_CONTRACT.md)。只完成获准的产品实现、离线验证和Android测试准备；设备操作 **0**，没有截图或平台送达PASS。

## 实际实现

- Q03 USER_CONFIRMED：项目开始日1、日节点N−1、周年按日历年；订阅只调用冻结M03 `elapsedPaidDays`并有界二分定位首次达到日。记录时区09:00后、目标日至+6日；结束后不虚增天数，0/负成本不影响时间资格。
- 抑制水位是`(ownerType,ownerId)`、截止日和固定六位掩码，与真实发布成功Receipt分开。首次纳入先初始化；创建/历史时间纠错/既有删除沿用原Room事务，普通资料/金额及当前未来续费不吞合法节点。类型同UUID隔离，旧掩码与回执不倒退。
- 同一发布日稳定tag/id摘要、onlyAlertOnce，同日追加保留原来源；无新候选不发布。发布成功后新回执同事务全写或回滚。与续费严格按kind分支、独立任务取消，继续共用原渠道和两个开关。点击以有界摘要引用重建真实来源，单条准确详情、多条临时50条窗口，保留dirty/pending/恢复守卫。
- Room **5**仅增水位表，真实schema5/已知Room身份及显式4→5迁移；旧schema1～4不改。backup **4/minReader4/schema5**用独立十表DTO；旧1/2/3按原字段、计数和SHA认证，旧proof先认证，再在未公开候选合并保护、回读、写新proof。active-pointer/ready内部版本仍1；唯一Owner、媒体原字节和冻结算法不改。

## 原案例的实际证据边界

| 案例 | 本轮已执行的限定结果 | 尚未运行 |
|---|---|---|
| T01 annual_prepaid_not_milestone | JVM PASS：年付3650元首日实际1天；实际阈值、项目100/1000日、周年/闰日区别 | Android实际发布 |
| T02 old_import_does_not_spam | JVM PASS：初纳/旧格式恢复基线含当天，不伪造Receipt，未来节点仍有效 | Room初始化/恢复真实运行 |
| T03 pause_free_gap_excluded | JVM PASS：暂停/停止/免费/无覆盖/重开/预付按冻结引擎；连续100/365、暂停10天、闰日案例 | Worker真实事实联动 |
| T04 same_day_coalesced | JVM PASS：同日合并/增量/76来源/同UUID类型/过期引用/导航守卫；批量失败和并发 | 真实通知、点击、Compose和权限 |
| T05 backup_watermark_survives | JVM PASS：新v4及真实旧格式/proof兼容、坏类型/关联/掩码拒绝、恢复前保护、历史纠错和7日边界 | 真实1/2/3/4→5迁移重开、Room往返/回滚、系统重启 |
| T06 switch_cancels_future | JVM PASS：独立偏好、权限拒绝不记回执、重开不清水位；准备独立取消/TestDriver方法 | Android持久任务、TestDriver和系统后台 |

Q03规则确认不等于Q03-V01～V04完整跨模块通过；Q04仍PROPOSED，16组完整门禁与G3不关闭。

## 测试、构建与原始输出

- 最终 **395个唯一JVM方法 / 395 PASS / 0失败、错误、跳过**，新增77＋相关回归318，不累计重跑次数。包含Planner23、时间纠错策略15、备份18、摘要派发13、导航8个新方法及冻结引擎/续费/备份/设置回归。[逐类结果](m21/completion/results.json)、[原始JUnit XML](m21/completion/xml/)、[构建命令及输入摘要](m21/completion/build-result.json)、[完整最终日志](m21/completion/gradle.log)。同一367项构建输入前后摘要`8eac8b3f9d5d704f5864e2fced4b42836381ca88496fe8a27fe1ebd7c2d1eccb`一致。
- **31个新增Android方法：编译/打包成功，执行0、PASS0、NOT_RUN31**。事务10、Worker7、Intent4、Compose4、迁移3、恢复2、TestDriver1。尚无设备许可，真实Room/通知/恢复/系统重启/API26/OEM/大字体/TalkBack均NOT_RUN。
- 最终组合构建 **exit0 / BUILD SUCCESSFUL in 8m26s**，Lint **0错误25警告**；App与Test构建成功。[Lint原件](m21/completion/lint.xml)。最终检查发现初产物Test DEX缺新增类，保留初SHA与审计；只对同一已编译输入重打包Test（36s/exit0），核对实际DEX全部31方法及同证书，App字节不变。[打包原记录](m21/packaging/result.json)、[DEX核对](m21/packaging/dex-verification.json)、[最终身份](m21/packaging/artifacts.json)。编译/DEX存在不等于Android类加载或执行通过。
- 历史如实保留：[行为RED37 FAIL](m21/red-behavior/)、[摘要/导航19项4 PASS15 FAIL](m21/summary-navigation-red/)、[集成74项72 PASS2 FAIL](m21/integration-red/)、[中间393项392 PASS1 FAIL](m21/final-green/)、[未来覆盖边界15项13 PASS2 FAIL](m21/future-boundary-red/)。最后两项真实失败用于修正“仅延长未来覆盖结束日”和“未生效未来付款”误推进水位。另保留[最初字符集初始化3错](m21/red/)、[中间编译失败](m21/summary-red/)、[Android测试首次编译失败](m21/final/)；它们不冒充行为RED。旧备份测试仅更新“当前导出版本”断言到4/5，真实旧格式样本/断言不改。

## 候选身份与限制

| 产物 | 实际身份 / SHA-256 |
|---|---|
| App | `com.cooper.lithic.debug`，code16 / `0.1.0-m21-debug`；13,805,852字节；`5f7c585e5e117bc7e77d5f2915429ac1eb8f4ac6b9480ad77aa8fa13529b1c62` |
| 最终Test | `com.cooper.lithic.debug.test`；3,069,193字节；`5b75a421a6e9c9b42046cbd2eafcb117f284dc2788e08aeb59a3a02ac72a9177` |

两个包实际v2签名核验成功，同Debug证书SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；Test Manifest未声明versionCode/name，AGP元数据code0，不冒充App版本。待执行Runner：`com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner`。[App权限/版本锁核对](m21/apk/apk-verification.json)、[最终Test Manifest](m21/packaging/test-manifest.txt)、[签名](m21/packaging/test-signature.txt)。无INTERNET/精确闹钟/常驻前台服务；未安装、未运行，不提交APK或密钥。

仍有发布API成功到Receipt落盘的有限崩溃重试窗口，不能承诺exactly-once、后台准点或用户实际收到。完整事实读取/水位初始化不是固定50条成本，长期性能尚未测。真实跨存储故障/平台迁移恢复仍待独立设备许可。M20额外平台25唯一24 PASS/1 FAIL、第二次收口BLOCKED/UNKNOWN及5704保留现场不变；M10 PENDING/DEFERRED与三JPEG历史FAIL、M13-T06延期、G1/G2/G3及全部历史NOTES/UNKNOWN/NOT_RUN保留。M21不冻结、不合并main、不进入M22，提交后等待ChatGPT独立验收。
