# M19 必要原始证据索引

[集中报告](../M19-verification.md) · [逐方法统计与验收映射](results.json) · [截图来源](screenshots/README.md)

| 证据 | 真实范围 |
|---|---|
| [导航 RED](nav-red/command.json) / [XML](nav-red/TEST-com.cooper.lithic.app.M19NavigationPolicyTest.xml) | 3项：1 PASS/2 FAIL；后续补全到5项，未伪造首轮结果 |
| [领域 RED](domain-red/command.json) | 19项全部FAIL；原stdout/stderr及两份XML保持 |
| [首次组合构建](green-build/result.json) | 109 JVM实际PASS；Android测试编译因LedgerSnapshot import歧义失败，日志不抹除 |
| [最终构建](stale-green-build/result.json) / [stdout](stale-green-build/stdout.txt) / [Lint XML](stale-green-build/lint-results-debug.xml) | exit0，2m42s；310输入前后SHA相同；--rerun真实109 JVM，唯一方法不重复计数 |
| [提示缺陷Android RED](android-stale-red/M19ReportAnalysisViewModelTest.result.json) | 失败后切换选择预期保留error，实际null；同方法加强断言，最终4项VM回归GREEN，原结果不改 |
| [设备入场](device-before/full-admission.json) | 原M18在线5692、进程创建/boot/磁盘/端口/已装包；不依赖旧在线记载 |
| [最终APK静态配置](apk-final/apk-verification.json) / [最终安装身份](install-stale-green/artifacts.json) / [初次更新](install-initial/commands.json) / [测试RED更新](install-stale-red/commands.json) / [修复后更新](install-stale-green/commands.json) | 原Debug证书，同身份App/Test各更新两次，设备回读SHA/UID相同 |
| [Room7与VM4](android-final-core/summary.json) | 11 PASS，原instrumentation stdout/stderr及逐方法结果 |
| [Compose4与旅程1](android-final-ui/summary.json) | 5 PASS，真实页面/返回守卫/只读事实，8张原字节图 |
| [M18读取7与首页6](android-final-regression/summary.json) | 13 PASS，受影响回归；不重复M18全模块验收 |
| [结束保护](device-end-final/summary.json) / [命令](device-end-final/commands.json) | 原实例在线、包SHA/UID及设备设置匹配，no_backup空；私人字节比对NOT_RUN |
| [最小执行脚本](reproduction/) | 仅本轮限定构建、5692安装/选定测试、截图和结束核查；复跑仍需届时授权，不是下一阶段许可 |

原输出按实际字节保存（Windows工具输出可能非UTF-8）；命令JSON记录退出码/长度/SHA，不能按网页乱码重写原日志。APK不入Git，原物位于本工作区app/build/outputs/apk/。不归档SDK、密钥、账本、AVD或旧截图。

初次M19的Android29项/构建/设备结束证据仍保留原字节；最终验收只统计最终同App/Test身份下的29项。交付8图重新取自最终旅程；初次8图原物仅保留本地build/m19-initial-screenshots，不重复入Git。
