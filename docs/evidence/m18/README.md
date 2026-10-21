# M18 必要原始证据

- [精确统计](results.json)：174唯一JVM方法，30新增/144回归；17新增Android方法均NOT_RUN，新截图0。
- [最终命令/构建输入](final-build.json)、[完整最终日志](logs/verification-final.txt)、[最终XML](jvm-final/)、[Lint原文](lint-final.txt)。源码SHA按本机实际构建字节；Git文本行尾可能规范化，不把两者冒称同一字节。
- [首次导航RED](logs/navigation-red.txt)及[XML](red/TEST-com.cooper.lithic.app.M18NavigationPolicyTest.xml)；[领域RED](logs/engine-sampler-red.txt)及[XML目录](red/)；[空账本追加RED](logs/empty-ledger-red.txt)及[独立XML](red-empty/)。历史正确行为未人为改坏。
- [首次集成构建](logs/verification-first.txt)：172 JVM通过、Android测试2处同名类型编译失败；原日志保留。外层Python打印该原日志时发生GBK编码异常，不改变已经保存的原始Gradle失败记录；最终捕获脚本已避免此打印异常。原始Gradle日志使用锁定JDK本机编码，不重写历史字节。
- [设备NOT_RUN原因](device-availability.json)：原授权两个进程不存在；没有发出ADB、启动或安装。不借用旧图或旧Android PASS。
- [产物/证书/DEX](apk/identities.json)、[App配置](apk/apk-verification.json)，均为离线核验；无设备UID、安装或加载结论。

只有本轮必要日志、结果和文本元数据入Git；没有ZIP、大型文件清单、SDK、APK、密钥、AVD磁盘、真实账本或私人图片。原M00～M17证据/规划保持原样。[集中报告](../M18-verification.md)。
