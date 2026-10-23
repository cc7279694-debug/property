# M22 必要原始证据

- [集中报告](../M22-verification.md)是当前解释入口；[逐方法及每轮Test SHA](android/results.json)区分37个唯一方法、47次执行和长历史NOT_RUN。
- [JVM逐方法](checks/jvm-results.json)、[XML](checks/jvm/)、[JVM原日志](checks/jvm.log)；[真实RED](red/)不覆盖。
- [构建执行口径](checks/execution-summary.json)、[最后构建原日志](checks/journey-ready-package.log)、[Lint原XML](checks/lint.xml)、[23个变更输入SHA](checks/source-inputs.json)。SHA为Windows实际构建字节，Git可能规范化CRLF。
- [6张Compose控件层图](screenshots/ui-index.json)；[独立全屏SystemUI ANR](screenshots/blocker-systemui-anr.png)、[来源与SHA](android/systemui-blocker.json)。不混作MainActivity、无遮罩触摸、真实IME或TalkBack证明。
- [实际安装包](android/installed-final.json)是c978 Test；[最后未安装候选](checks/ready-artifacts.json)是f671 Test。历史各包/失败保留，不能把它们称为同一测试包全量PASS。
- [设备起点](android/static-admission.json)、[运行身份](android/runtime-host-proof.json)、[结束保护](android/end-protection.json)、[正常退出](android/exit-proof.json)、[资源口径](android/resources-end.json)。不授权再次启动或清理。
- `android/commands/<operation>/command.json`保存定向serial、实际参数、时间、外层退出码、原始stdout/stderr字节数和SHA；`stdout`/`stderr`为真实输出。六张PNG/来源JSON为避免重复存储，原stdout移至`screenshots/`，对应`stdout-location.json`明确位置，原SHA不变。
- 早期`jobs-before`工具忽略package过滤而返回全局Job；原字节仅本地忽略目录保留，Git只归档原命令/完整性元数据与隐私范围说明。结束两次Job观察只在内存处理全局原字节，保存必要目标行及元数据，明确不是完整原始转储。`TopAppTimer/UID inventory`与注册JOB按边界区分，未取消任务。
- `journey-frame-inspection`的本地诊断PNG在发现阻塞后以相同SHA另存`screenshots/blocker-systemui-anr.png`；原device/local字节均保留。所有其他App/私人账本/图片未归档。APK、SDK、镜像、userdata、密钥和临时构建缓存不入Git。
