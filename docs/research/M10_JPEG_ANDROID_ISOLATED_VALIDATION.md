# M10 JPEG 独立 Android 集中验收

**BLOCKED — HOST_SUPERVISOR_CLI_CONFIG_COMPATIBILITY**。本轮没有完成 Android 验收，不是新一次 JPEG 测试失败。来源 `b756f96b699f3b70d43144f6c92815917fb563f6`；仅分支 `codex/m10-jpeg-android-isolated-validation`。本轮接受用户限定设备授权，不授予 M10 冻结。

## 实际结果

| 项目 | 结果 |
|---|---|
| SDK / 镜像 | 既有 API26 Google APIs x86_64 r16 的11原件与历史 size/SHA全匹配；API36 r7及既有37.1.11工具身份已核查。没有下载、升级、许可接受或重跑native压测。 |
| App APK | 原锁定文件复用，13,931,874字节，SHA `0ada263192dbaa1256bff6c613e6d57a69e1160b17d4844d1e1cfcc9cf847dbf`。没有重新构建替换。 |
| 测试 APK | 从同一锁定源码离线构建成功，3m12s/exit0；2,234,890字节，SHA `3b0005a31afa753e9a9709323572250c0c5c63153c68ca1f882755c98724282a`。原证书、实际包名/Manifest/仪器入口和两JPEG样本SHA已核对。只留本地忽略产物，不提交APK。 |
| API26 | 官方创建命令**1次/exit0**；后置配置身份核对**BLOCKED**。启动0、安装0、测试0。 |
| API36 | 创建/启动/安装/测试均0；共用监督器格式假设需要先审查，不继续新增环境。不是镜像不存在。 |
| 宿主监督器测试 | 最终22唯一方法PASS；首次18PASS/2ERROR为fixtures未同步SDK/sysdir职责，原日志保留。合成fixtures遗漏真实CLI模板差异，不能据22PASS证明设备准入。 |
| Android与原三JPEG | 全部NOT_RUN；原三项历史FAIL保持。没有实际Worker/Binder/FD/退休、PNG/旧v1/业务回归、任一ABI加载的新证据。 |

APK测试包为 `com.cooper.lithic.debug.test`，实际入口 `com.cooper.lithic.debug.test/com.cooper.lithic.data.media.M10ProcessBoundaryRunner`；目标 `com.cooper.lithic.debug`。两包原证书SHA `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。App权限不变；测试包只有既有REORDER_TASKS及合成ContentProvider。没有执行runner的action/fault入口。

## 阻塞定位与停止

本轮监督器错误假定新 `config.ini` 必须包含 `AvdId=<指定名称>`。官方 CLI 创建的实际文件没有 `AvdId`，而是 `avd.id=<build>` / `avd.name=<build>`；`disk.dataPartition.path=<temp>`也仍为模板值。新 `.ini` 文件名、注册path、目标API26、ABI和镜像路径均与计划一致。这是**监督器对CLI配置格式的兼容性缺口**，不能归因为App、损坏镜像或已经证实的目录越界。

只读对照仓库既有 [C2归档配置](m10-jpeg-h2-d2b-a2r-c2/evidence/host-ending-20261018T042852.720178Z/config-ending.ini) 也有相同 `<build>/<temp>`格式；这只是历史格式依据，不是本次真实设备身份或隔离通过证明。**未访问C2或H2任何运行目录/设备。** 没有临场放宽核验、再次创建、补写PASS proof、启动或清场。

新现场保留在 `E:/CODEX/.codex/research-env/lithic-m10-android-20261018-v1/api26`：创建marker、官方生成config/descriptor和全部4个小文件保持。没有create-proof/boot-proof、没有启动marker。结束宿主只读确认emulator/qemu为0；ADB调用0，全局devices0，App/仪器启动0，清数据/kill/卸载/恢复/重试0。官方创建命令及监督器exit1均保留，不能把命令exit0替换为完整门禁PASS。

## 执行保护与后续审查

Goal原定逐设备先健康Worker门禁，继而缺损及原三断言，再PNG/v1/必要业务回归。新目录/名称/端口5624和5626独立；子进程去旧SDK_HOME并覆盖原ANDROID_AVD_HOME，全局环境未改。短命令/启动均预写独占fsync/readback marker；启动固定300秒期限，宿主1536MiB guest＋1024MiB余量仅为准入估算，不是native/RSS硬cap。命名空间链接/精确进程参数/APK身份/固定serial保护未放宽。

后续须先审查官方CLI身份规则：以独占创建证据、注册文件名/精确路径、image/API/ABI和明确的datadir契约为依据，正确区分模板占位项与真实设备身份，并验证 `<temp>`具体解释。若获明确批准，只验证/配置**这个已创建且从未启动的新AVD**，不能再次调用create、清marker或重置次数。启动后仍须真实AVD名/API/serial/boot=1才能安装。两包SHA/证书、健康图六退休phase＋实际observation＋terminal/pin/guard/独立进程观察的原定门禁仍完整保留。

同步hello、Host FD关闭和源RO模式在锁定代码中是硬门槛，但现有入口不单独导出其全部原始字段；不能补写为独立轨迹、实际写源被拒绝或所有资源已验证。取消/超时/故障注入/恢复GC、永久阻塞读取、迟到RPC、native最坏停止、RSS硬上限和未运行ABI继续NOT_RUN/UNKNOWN；不执行额外高风险场景。

## 精简证据与状态

[结果与逐文件SHA索引](m10-jpeg-android-isolated/evidence/RESULTS.json)、[原始命令journal](m10-jpeg-android-isolated/evidence/api26-commands.jsonl)、[实际生成配置](m10-jpeg-android-isolated/evidence/api26-generated-config.ini)、[监督器失败](m10-jpeg-android-isolated/evidence/api26-create-supervisor.log)、[测试APK构建](m10-jpeg-android-isolated/evidence/test-apk-build.log)、[APK审计](m10-jpeg-android-isolated/evidence/apk-audit.json)。仅必要原件；没有ZIP、SDK/许可正文、userdata、账本/图片或二进制提交。辅助aapt审计的非ASCII argv和布尔格式错误已修正，仅重跑宿主artifact核对；原路径错误保留，APK字节不变。

App源码、原三断言、Room/schema、备份格式、冻结算法及UI均与来源完全相同；app Git子树 `89679a188c2b6e6793962041383a9f92337e126f`。M10 PENDING、三JPEG历史FAIL、UI-C1冻结、M11 NOT_AUTHORIZED、D2A-N01～N05/A1-N01～N04及全部历史NOTE/FAIL/UNKNOWN/NOT_RUN保留。H2 DELIVERY_UNKNOWN/pin=true、C2状态只继承历史。只提交推送本分支，停止等待ChatGPT集中独立验收。
