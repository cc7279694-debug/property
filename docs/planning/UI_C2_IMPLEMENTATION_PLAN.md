# UI-C2 实施计划

## A段历史计划（本轮B段授权见文末）

目标：当前产品分支上的四页生产Compose实现，加同来源视觉样板，集中展示后停止。基线、规格、限制见[UI_C2_SPEC](../design/UI_C2_SPEC.md)。不建立长期UI支线，不merge/cherry-pick旧UI。

1. 核对基线与并行变更；读取P01～P04和旧UI来源，实际查看图片。
2. 先添加新呈现/面板/错误展开/返回/未知提交/旧代隔离断言；既有正确行为直接相关回归，不伪造RED。
3. 选择性复用主题和图标，接入S01现有列表读模型、S02完整业务动作、S03渐进表单及S04既有趋势；锁住数据/写入/恢复协议。
4. 必要JVM、Debug、AndroidTest源码编译、Lint；只跑受影响范围，不运行历史整套或设备。
5. 无现成Compose桌面渲染时，交付单文件离线HTML并标PROTOTYPE_ONLY / APP_VISUAL_PENDING。生产Composable与测试夹具同时编译；不把HTML或编译当真实App视觉。
6. 写[简短样板报告](../evidence/UI-C2-Sample-Review.md)，提交推送当前产品分支，停在VISUAL_SAMPLE_READY — UI-C2，等待一次集中视觉反馈。

## 原A段停止边界（历史；B段现已另行授权）

订阅、列表、搜索、设置、备份恢复及其他深页推广需等用户看A段。最终用户APK须来自本产品分支新UI，preview同ID/原证书、code高于18；核对生产MainActivity使用新组件及APK实际显示。A段不做新用户安装包、不操作设备、不开始M23。

## 边界与阻塞

- 没有已验证页面媒体读取接口：封面展示/管理仍PENDING，不用旧M10或裸路径补齐。
- Android运行、大字体/IME/无障碍/性能和真实截图未授权，维持NOT_RUN。
- 全局主题随A段改变，其他页面只受主题传递，不进行B段结构重构。
- 预览复用原50条窗口，趋势保持原完整计算成本，不谎称固定2条或50条计算。


## 本轮A-R1（视觉CHANGES REQUIRED后的限定精修）

Goal：解决长金额破碎、大字表单拥挤、空态重复、详情层次与回看首屏曲线位置五项问题，交付可再次集中查看的样板。
Scope：四页生产Composable、AmountText与必要输入标签可选呈现参数、同一离线HTML及两张图、定向断言和本报告。
Out of scope：B段推广、设备/ADB/AVD、用户安装包、媒体入口补建、数据/计算/写入/Owner/导航或恢复协议。
Verification：定向JVM；Debug/AndroidTest编译/Lint；浏览器实际查看四页浅深与四个代表状态，核对单行完整金额、可发现横向查看、正文/保存区边界和可见绘图区。Android真实布局/字体/IME保持NOT_RUN。
Stop：VISUAL_REFINEMENT_READY — UI-C2 A-R1，附PROTOTYPE_ONLY / APP_VISUAL_PENDING；等待ChatGPT与用户一次集中视觉确认。


## A-R2定稿候选续接

Goal：恢复UI-C1原图的主卡/小摘要/身份指标/分组层次，保留A-R1已经解决的布局问题。Scope：四页生产Compose、极长金额完整查看、纯UI图标与离线视觉对照。Out of scope：所有业务/媒体实现、B段、设备和用户安装包。Verification：受影响JVM、Android源码编译、Debug/Lint、离线浅深四页+代表状态+原图并排，不把HTML当Android视觉PASS。媒体仅只读核对并单列MEDIA_DISPLAY_PENDING。完成后VISUAL_CANDIDATE_READY — UI-C2 A-R2；集中确认前停止。


## B段授权与实施合同（2026-10-26）

起点e24703b，A-R2独立结论仅PASS WITH NOTES — PROTOTYPE VISUAL DIRECTION ONLY。Goal：沿用其浅深语言统一其余真实产品页面。Scope：B1列表/历史/搜索与共享呈现组件，B2订阅档案/操作，B3项目深层表单/交易，B4回看/设置/数据管理，分别提交生产代码；B5只读媒体建议。Locked：Room5/backup4、冻结规则、DAO/Repository/VM写入/Owner/恢复/导航/提醒、版本签名及A主要结构。Adjustable：信息层级、身份/状态/金额、间距分组、表单/主次操作外观。无需新增数据设施或依赖。

Verification：各部分定向JVM回归与AndroidTest编译，最终集成Debug/Lint与身份审计；不运行ADB/设备/真实Android，不重跑M22历史套件，不生成新体验包。仅少量PROTOTYPE_ONLY对照，生产视觉/字体/IME/封面不判PASS。B1～B4按顺序集成与提交，其他部分先存当前任务scratch副本，避免把未经编译的并行修改混入前一部分。完成后UI-C2 B IMPLEMENTATION COMPLETE — READY FOR INTEGRATION REVIEW，停止；C段、设备与高于code18新版体验包须后续授权。


## B段实施停止点

B1～B4已分别形成生产代码提交；每部分执行定向JVM及AndroidTest编译，编译失败和修复原样保存。最终集成、身份审计和NOT_RUN范围统一见[UI-C2-B-Integration](../evidence/UI-C2-B-Integration.md)，不累加重复测试次数为覆盖数。新增21个Compose方法仅编译，未设备执行。四帧订阅/设置浅深样板为合成HTML，未生成新的用户APK。B5只读媒体建议见报告，不实施resolver、解码或M10协议。后续C必须另获设备与交付授权，核对生产MainActivity、新组件、浅深/字体/IME/返回/未知状态；最终体验包需同preview ID/证书且code>18。本轮停止等待独立整合审查。


## C1 真实视觉整改停止点（2026-10-26）

Goal：针对Preview3真机拒绝原因修Home/Review生产空态及菜单/导航层级。继承当前`ed492b7`图标，不回退C0或B段；不再生成HTML。Scope仅UI与相关定向断言/版本候选，Locked业务/Room5/backup4/Owner/恢复/媒体/通知/导航资格不变。54 JVM PASS，AndroidTest仅源码编译（14新Compose），Debug构建/Lint0E21W；Private Trial候选同ID/证书升code21（已有code20历史），静态接线与身份核对见[C1报告](../evidence/UI-C2-C1-Real-Visual-Remediation.md)。

Stop：UI-C2 C1 — READY_FOR_DEVICE_VISUAL_REVIEW，APP_VISUAL_PENDING。A～E没有实际Android图，不据编译申请VISUAL_ACCEPTED；后续需用户明确选择安装/核对及数据隔离，不能自动操作设备、旧AVD、用户账本或进入M23。


## C2 原生空态整改及截图入口停止点（2026-10-26）

Goal：针对Preview5真实视觉拒绝收口Home/Review空态；不能用编译或HTML证明视觉通过。Scope仅生产UI结构、共享原底栏、相关布局断言及androidTest不可变事实截图入口。Locked：Room5/backup4、引擎/数据/Owner/恢复/媒体/提醒/导航资格/Theme/版本证书；不操作设备、不生成Preview6。

生产空态已调整，50定向JVM PASS、Debug/AndroidTest编译/Lint0E21W；6截图入口及2布局方法仅编译/运行NOT_RUN。A/B深色400空事实、C/D浅色400合成记录、E1/E2 360与LocalDensity2倍；仅Home/Review，不代表项目详情/录入、IME、MainActivity或真实Room账本。没有实际Android图，原设计与真实Android对照PENDING。详见[C2报告](../evidence/UI-C2-C2-Visual-Fidelity.md)。

Stop：UI-C2 C2 IMPLEMENTED — APP_VISUAL_PENDING。之后先独立审查，取得新的隔离设备授权和启动前Manifest/Runner/target/SHA门禁，才可真实采集；当前不安装、不启动旧ANR环境、不交付新用户APK、不合并main或进入M23。


## C3-0 独立原生截图准备（2026-10-26）

Goal：只准备独立包与安全截图入口，不重做视觉。Scope：显式Gradle flag/独立build目录和源Manifest，原Gallery的真实配置F1/F2与身份/元数据，未来限定命令。Locked：生产UI/引擎/Room5/backup4/Owner/恢复/导航/媒体/提醒与原四类构建身份。依赖原M13Runner、AndroidX空宿主与既有事实夹具，零新依赖。

已按源配置落实新App/Test、启动前platform Application、组件移除/强Runner/互斥及仅Debug、A～F八方法、自包UUID及元数据；源检查和命令示例语法核对完成。用户中途明确“先完成源码，构建暂缓”，因此没有Gradle求值、JVM/Android编译、Lint或新APK，不能确认合并Manifest、签名/SHA。独立复核后需先恢复桌面门禁，再单独授权设备安装/八方法/本次三文件提取；不自动恢复构建或开始设备。

Stop：SOURCE_PREPARED — BUILD_DEFERRED / ANDROID_SCREENSHOTS_NOT_RUN；非C3-0 READY_FOR_SAFE_CAPTURE_REVIEW。见[C3预检](../evidence/UI-C2-C3-Native-Visual-Preflight.md)和[未来命令](../evidence/UI-C2-C3-Capture-Runbook.md)。保留C2及全部历史证据与APP_VISUAL_PENDING，不生成Preview6、不合并main、不进入M23。


## C3-0 桌面预检恢复完成（2026-10-27）

用户表示资源足够，核对6.143GiB内存/65.437GiB磁盘后只恢复桌面构建。App/Test与八方法编译完成；首轮Lint3错误保留，仅C3 Manifest改为Application白名单、继承原POST声明，未改业务/禁Lint。14定向JVM PASS、最终Lint0E15W、重打包/签名/实际Manifest/Runner DEX/四配置身份/五种冲突门禁核对完成；源码提交2dbd9a1，APK路径、真实SHA和证据见[C3报告](../evidence/UI-C2-C3-Native-Visual-Preflight.md)。原源码暂缓段和所有失败保持历史。

Stop：C3-0 READY_FOR_SAFE_CAPTURE_REVIEW / ANDROID_SCREENSHOTS_NOT_RUN。不是Android视觉通过，App仍无业务启动入口；POST声明不等于授权或送达，Test辅助组件明确保留。实际设备/安装/截图/提取须另行独立审查及用户授权；不使用旧ANR设备，不生成Preview6、不合并main、不进入M23。
