# M18 Android 原始证据索引

本目录是来源 `6dfc4a6` 的补验；原 `../m18/` 完全保留。

- [最终逐方法结果](results.json)：17个唯一方法，全部PASS；重复执行不增加方法数。
- [首次原APK执行](android-initial/summary.json)、[首次旅程FAIL](android-journey-initial/M18TotalCostTrendJourneyTest.result.json)：14 PASS / 3 FAIL，未覆盖。
- [最终四类真实instrumentation](android-final/summary.json)：每类同目录保存原始stdout、stderr、claim、退出码及逐方法status。
- [测试APK定向构建](build-driver-fix/result.json)：原始stdout/stderr，App字节未变。
- [初次安装](install-initial/commands.json)、[最终身份及测试包更新](install-driver-fix/artifacts.json)：签名/Manifest、设备APK字节SHA及独立UID。
- [新设备配置](device/config-proof.json)、[实际进程及写盘路径](device/host-instance-proof.json)、[boot证明](device/boot-proof.json)、[结束保护](device-end/summary.json)。
- [10张原始截图及来源](screenshots/README.md)、[字节/SHA/尺寸](screenshots/sources.json)。
- [本轮实际执行脚本](reproduction/)：仅新设备5692，没有旧设备清理入口；不是后续自动运行授权。

初次 `pm path` 的空输出exit1为未安装；新包未启动时no_backup不存在。两处监督脚本预期过严和一次远端shell引号错误已保留非零记录；未据此重建或重复启动设备。启动外层回执NOT_CAPTURED，实际唯一实例由CIM/创建时间/完整路径/父子关系/端口/hardware-qemu/boot独立证实；不补写旧启动成功回执。设备最后保留在线。没有归档SDK许可、APK、账本或个人图片。
