# UI-C2 实施计划

## 当前唯一授权：A段

目标：当前产品分支上的四页生产Compose实现，加同来源视觉样板，集中展示后停止。基线、规格、限制见[UI_C2_SPEC](../design/UI_C2_SPEC.md)。不建立长期UI支线，不merge/cherry-pick旧UI。

1. 核对基线与并行变更；读取P01～P04和旧UI来源，实际查看图片。
2. 先添加新呈现/面板/错误展开/返回/未知提交/旧代隔离断言；既有正确行为直接相关回归，不伪造RED。
3. 选择性复用主题和图标，接入S01现有列表读模型、S02完整业务动作、S03渐进表单及S04既有趋势；锁住数据/写入/恢复协议。
4. 必要JVM、Debug、AndroidTest源码编译、Lint；只跑受影响范围，不运行历史整套或设备。
5. 无现成Compose桌面渲染时，交付单文件离线HTML并标PROTOTYPE_ONLY / APP_VISUAL_PENDING。生产Composable与测试夹具同时编译；不把HTML或编译当真实App视觉。
6. 写[简短样板报告](../evidence/UI-C2-Sample-Review.md)，提交推送当前产品分支，停在VISUAL_SAMPLE_READY — UI-C2，等待一次集中视觉反馈。

## 未授权后续

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
