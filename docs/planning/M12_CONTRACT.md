# M12 — 安全覆盖恢复执行契约

用户2026-10-20正式授权；来源635e33a57c66c286fa5c19f009a5565a1e70b8e2，分支codex/m12-safe-backup-restore。M11独立结论ACCEPTED / FROZEN — PASS WITH NOTES，仅覆盖schema2/format1完整手动导出及已登记JPEG/PNG原字节。旧报告/失败/NOT_RUN不改。

- Goal：系统选择.lithicbackup → 完整私有验证 → 七表/设置/图片摘要与覆盖确认 → 独立完整新generation → 原子切换；准备失败旧集不变，首次新集打开失败持久回退并明确反馈。
- Scope：format1/schema2解析/关联/路径/数量/大小/hash校验，Room2/设置/原字节媒体重建，统一owner恢复锁/旧写任务退役，AtomicFile active/previous/stage及首次打开证明，设置页恢复与确认。
- Constraints：继承原M12/B04～B08、Q05原字节、Q06持久回退；不更改schema/format/冻结金额或订阅规则，不合并M10，不云/CSV/自动备份/密码。未知归属资源保守保留，不删除用户数据解决错误。
- Architecture：SAF输入只读拷贝有界私有文件；验证bundle持有精确身份，进程重建不重放确认。候选新代全部写完关闭、重新打开核验后才可切换。切换以旧代快照CAS拒绝预览后的业务变化；旧协调器退休使排队旧写操作失败，不把旧ViewModel重新绑定新代。持久SWITCHING记录保存前一完整代；只在新集完整验证后稳定提交。未知切换结果fail-closed，禁止新空库。
- Acceptance：原M12-T01～T06逐项登记。完整往返/缺图损坏/越界和新版本/重复项/业务外键/写入失败/切换前失败/首次打开回退/按今天重算均有必要测试；杀进程门禁不伪造通过。
- Verification：先RED再GREEN的定向JVM与合成Android，Lint/Debug/Test编译，APK身份/无新增权限与依赖，差异/敏感信息/链接/历史不变检查。不给每个子任务建平行状态或大证据包。
- Device：用户本轮补充许可仅复用原M11合成API36 Lithic_M11B_Final_API36_20261020 / emulator-5686；先核对原实例/boot/包/数据身份，允许同签名App/Test更新及非进程终止的恢复往返、坏备份拒绝、设置确认。实例消失即设备NOT_RUN，不重启、不创建AVD。严禁H2/C2/M10/其他保留设备、真实用户数据、kill/force-stop/清数据/故障终止。
- Stop：一份集中报告、必要代码/真实证据，提交推送当前候选后等待ChatGPT独立验收；不自行M12 FROZEN，不发布/合并main或进入下一模块。
