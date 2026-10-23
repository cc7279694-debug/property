# M22 定向补验停止点

- Source：`1130349e8382aae171aa9259d27bce86dfea4a2a`；branch `codex/m22-experience-accessibility`。
- Goal：原5712一次新冷启动、先SystemUI健康再T06/正式20年T03/真实系统IME。
- Actual：创建0/启动1/正常退出1；两轮健康观察第二轮真实ANR阻塞；安装App0/Test0/Instrumentation0。三个补验方法本轮均NOT_RUN，旧T06三次FAIL未改。原137 JVM/36 Android PASS不重跑。
- Changes：仅androidTest阶段/失败取证及一个真实系统IME方法；Test编译/DEX/签名核对通过但未安装、未运行，不能称修复通过。App SHA不变，Room5/backup4/冻结规则不改。
- Verified：构建exit0，Lint0错误17警告；原29锁、9旧文件、6M22 PNG/旧ANR字节保留，无新默认账本/数据库文件/目标Job/业务进程。设置未改，原值读回。无Owner打开或清理；不推断旧锁释放。
- Device：launcher30120/QEMU24224/新Boot fca0224f-b56a-43da-b6da-86bb5a1f4936，原实例及官方收尾helper/5712与5713端口最终自然结束，原AVD保留。禁止再次自行启动/修复/替换设备或清理。
- Blocking：**SYSTEMUI HEALTH GATE**；真实ANR在M22测试前出现，根因UNKNOWN，不称产品缺陷。T03实际规模/性能和真实IME仍未取得证据。
- Evidence：[补验报告](../evidence/M22-Android-supplement.md)、[本轮结果](../evidence/m22-supplement/results.json)、[健康门禁](../evidence/m22-supplement/systemui-health-gate.json)、[退出](../evidence/m22-supplement/exit-proof.json)。
- Inherit：M10/M13-T06延期、M20平台FAIL/收口UNKNOWN、Q03/Q04/Q02/G1/G2/G3及全部历史NOTE/FAIL/UNKNOWN/NOT_RUN保持。提交推送后停止等待独立复验，不冻结、合并main或进入M23。
