# M10 JPEG 独立Android验收停止点

- 来源 b756f96b699f3b70d43144f6c92815917fb563f6；分支 codex/m10-jpeg-android-isolated-validation；仅本worktree研究脚本/证据/状态变更，App/断言不改。
- SDK/镜像复用，App锁定原字节；测试APK从原输入离线构建PASS、原证书/runner/两锁样本SHA审计PASS。没有JVM/Lint或宿主native压测重跑；22纯宿主监督器方法PASS不能替代实际模板/设备门禁。
- API26 create=1/exit0，之后HOST_SUPERVISOR_CLI_CONFIG_COMPATIBILITY BLOCKED：监督器错误要求CLI中不存在的AvdId；实际<build>/<temp>模板保留。start/install/instrumentation/ADB=0，API36所有操作0。
- 新API26根 E:/CODEX/.codex/research-env/lithic-m10-android-20261018-v1/api26 含原create marker及4个小文件，不删除/改写、不再次create。没有create/boot PASS proof；未知runtime身份/隔离不能升级。未来先独立审查CLI格式与保留环境的接续条件。
- M10 PENDING；原三JPEG历史FAIL、本轮NOT_RUN；UI-C1冻结；M11 NOT_AUTHORIZED；D2A/A1全部NOTE及所有历史FAIL/UNKNOWN/NOT_RUN保留。H2/C2现场完全未触碰，只读仓库归档格式不等于观察运行现场。
- [一份集中报告](../research/M10_JPEG_ANDROID_ISOLATED_VALIDATION.md)、[结果/SHA索引](../research/m10-jpeg-android-isolated/evidence/RESULTS.json)。只推本分支后停止，等待ChatGPT独立验收，不启动设备或开始修复产品。
