# M01 执行契约

授权来源：用户本轮明确指令及实际读取的 `E:/CODEX/LITHIC_M01_Codex执行指令.md`。起点为 M00 验收回写提交 `77e61b653ebf1ce4838b4acce4b5b366e17891f4`，从 `feat/m00-planning-freeze` 创建 `feat/m01-android-skeleton`；创建前工作区干净。

本轮附件实际大小 10,106 字节，SHA-256：`f4b21192d707a0a23ec7dc4196f48219cc79b60b7dbf1ef924d38957e172c2cc`。附件被实际读取，未作为旧原包内容改写；原计划、原130案例和来源登记保持原始字节。

- Goal：可构建、可安装、可离线冷启动的原生空壳。
- Scope：单 `:app`、单 Activity、Kotlin/Compose/Navigation 3；工具链和身份、权限及备份配置；必要资源、构建、设备验证、文档、提交与该功能分支推送。
- Out of Scope：M02、业务数据库/schema、项目/订阅/成本逻辑、通知、媒体、完整备份恢复、M05 三栏界面、WHOCARE、main 合并、公开发布、生产签名。
- Constraints：minSdk 26；稳定精确版本；正式身份 `com.cooper.lithic`，Debug 身份 `.debug`；保留原130案例及归档字节、M00验收历史、Q02～Q07期限。
- Dependencies：M00 已独立验收 `ACCEPTED / FROZEN — PASS WITH NOTES`；[原 M01 计划](original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m01.md)和六规格。
- Acceptance Criteria：M01-T01～T04，按本轮指令要求安装/设备断网启动、合并 Manifest 与最终 APK 权限、最终 Debug 身份、版本锁与实际构建。实际结果只登记在[当前验收登记](../ACCEPTANCE_MATRIX.md)。
- Verification：Wrapper `--version`、`:app:assembleDebug :app:lintDebug`；SDK工具检查最终APK身份/签名/编译后的备份规则；可用设备实际安装、断网冷启动、返回与重启。设备不足则如实 NOT_RUN/BLOCKED。

采用既有唯一 CURRENT_STATE 与历史 checkpoint 交接，不另设平行任务状态。工程配置和空宿主以真实编译、lint、APK和设备证据验证；不伪造业务单测或把 NO-SOURCE 当成 PASS。本地审查不能代替 ChatGPT 独立验收。

Q07 在本轮的边界：尚无业务数据，完整备份往返不适用且未实现；这不等于备份 PASS，也不关闭其他模块的 Q07。
