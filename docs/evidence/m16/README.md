# M16 证据入口

本目录仅记录本轮合成数据验证；不是ChatGPT独立验收结论。最终范围、结果和NOT_RUN见[集中报告](../M16-verification.md)，当前状态以[CURRENT_STATE](../../CURRENT_STATE.md)为准。

- [交付检查](delivery-checks.json)：来源、范围、链接、敏感信息、构建输入与定向设备命令检查。
- [最终结果索引](results.json)：唯一测试方法统计、最终APK与构建输入身份、设备及截图。
- [最终构建原日志](logs/delivery-build.txt)、[最终JVM XML](jvm-delivery/)、[Lint原结果](lint-results-delivery.xml)。`delivery-build-inputs-before/after.json`验证构建期间工作树输入不变；这是工作树字节聚合，不能直接冒充Git规范化后的blob聚合。
- [最终Android核心](android-core-fixed/summary.json)、[页面旅程](android-ui/summary.json)、[受影响回归](android-regression/summary.json)：原始instrumentation输出、逐项状态、源码SHA及实际App/Test身份均在对应目录，重复执行不重复计数。
- [截图来源及原图](screenshots/README.md)：隔离ComponentActivity中的真实LithicApp/Nav3；360dp和200%为LocalDensity画布，不是MainActivity或系统字号设置。
- [最初设备核查](device-preflight/commands.json)、[最终安装身份](install-ui-fix/artifacts.json)、[收尾保护](device-end/summary.json)。仅操作原在线`emulator-5672`；不访问默认账本或其他设备。

## 失败与修复记录

- [最初备份v2契约RED](logs/backup-red-fixture-2.txt)及[原XML](red/)：真实22项运行中的3项失败；此前缺实现/编译接缝错误也保留在logs中，不把编译失败当业务断言失败。
- [断组完整导出RED](android-red/M16BackupRoundTripTest.result.json)：旧实现实际错误导出成功；新实现拒绝断组。测试适配最小差异及原输入重建校验见`red/export-validation-test.patch`和`red/export-validation-source-proof.json`，失败断言未放宽。
- [提交时间边界RED](android-core/M16ReplacementViewModelTest.result.json)：原3项中1通过、2失败。初次信封修订时间严格晚于旧项目，提交后不改信封；测试时间适配与最小生产修复见`red/vm-revision-test.patch`及`red/vm-revision-source-proof.json`。原结果、claim不变。
- [导航真实RED](logs/navigation-red.txt)、[XML](red/navigation-red.xml)、[修复前源码SHA](red/navigation-source-before.json)及[六处最小修复](red/navigation-fix.patch)：同一新测试从丢失比较页来源的断言失败，到最终GREEN。
- [真实旧v1备份来源](legacy-v1-fixture.md)、[schema1/2原字节保护](schema-byte-preservation.json)。旧schema的Git blob未改；Windows换行转换只在工作树修正回原字节，并用`.gitattributes`固定。

`reproduction/`沿用薄命令记录器，不自动启动设备、不执行其他模块或默认账本；install/run入口含实际已消耗阶段，不应作为下一轮重复执行许可。原始规划、旧checkpoint、历史FAIL/UNKNOWN/NOT_RUN保持不变。
