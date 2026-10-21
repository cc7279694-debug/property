# M17 原始证据索引

当前结果以[results.json](results.json)及[集中报告](../M17-verification.md)为准，172个唯一方法不按重跑累加。

- [最终JVM XML](jvm-final/)：11类144方法，源码方法身份精确匹配；[时间线/几何RED XML](red/)和[初次RED日志](logs/timeline-geometry-red.txt)保留。
- [最终读取/VM](android-core-capture-final/summary.json)：7＋4；[最终语义/真实旅程](android-ui-capture-final/summary.json)：6＋2；[最终相关回归](android-regression-capture-final/summary.json)：3＋6。每类同目录含原始stdout/stderr、命令claim、真实退出码与SHA、逐方法结果。
- [最终构建](logs/capture-final-verification.txt)、[Lint](lint-final.txt)、[构建输入](capture-final-build-inputs.json)；源码/配置281文件只登记总指纹，不建立大清单。
- [设备初检](device-preflight/commands.json)、[最后安装身份](install-capture-fix/commands.json)、[设备结束](device-end/commands.json)、[9图](screenshots/README.md)。全部ADB仅5672。
- [折抵真实RED](android-mapping-red/M17TimelineViewModelTest.result.json)、[刷新真实RED](android-core-final/M17TimelineViewModelTest.result.json)、[语义匹配失败](android-ui/ChartSemanticsTest.result.json)及[最终6 PASS](android-ui-capture-final/summary.json)分别保留。语义失败来自测试默认整段匹配；改为其原意的substring，不修改业务预期。
- 必要失败和最终原文入Git；重复成功输出/旧安装原文按[中间历史索引](intermediate-history.json)原字节保留本机build/m17-intermediate-evidence，旧安装命令/产物元数据仍入Git。没有改写原校验值或旧结果；名称中的final不表示正式冻结。原有M00～M16证据不改。
- reproduction下为继承M16的薄层定向命令与取证记录，包含一次性安装目录防覆盖检查；不是下一会话自动执行许可，不能盲目重跑已消耗阶段。首轮截图仅拍到曲线上部，原文件保留本机build/m17-first-screenshot-capture及设备，最终图08/09提供完整画布；无图片编辑。

私人账本/封面未读取；私人字节一致性NOT_RUN。APK、签名私钥、AVD、缓存与个人备份不提交。
