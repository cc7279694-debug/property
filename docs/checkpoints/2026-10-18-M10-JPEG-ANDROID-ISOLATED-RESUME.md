# M10 Android isolated resume checkpoint

- 来源：`939e88b3fc79f3b9fd160737022f3bd795a07f37`；分支 `codex/m10-jpeg-android-isolated-resume`。
- 监督器真实 CLI 配置兼容、独立 adoption、固定单方法执行器、有界只读证据解析已实现；191 唯一 Python 方法 189 PASS/2 SKIP。产品与 APK 输入不变。
- 原 API26 官方 create exit0/marker/config 原件及 4 文件最终 size/SHA 不变；首次静态 adoption 空 TEMP 目录阻塞如实保留，修正精确目录合同后新的 immutable adoption-proof PASS，不补写旧 create-proof。
- **新增 create0/start1/ADB0/install0/test0**。唯一启动 launcher32880，子 QEMU32032 首次 CIM ExecutablePath=null，fail-closed；结束同 PID 路径已可读，但不能回改失败或推断 Android boot。
- 新场景：`E:/CODEX/.codex/research-env/lithic-m10-android-20261018-v1/api26`，`Lithic_M10V_API26_20261018`，目标 `emulator-5624`，5624/5625。新设备保持运行，未安装 APK；无 boot-proof。下一会话不得再 start、清 marker、重建或自动结束该设备。
- API36未创建；9项 Android 方法均 NOT_RUN。冷启动身份缺字段的安全等待合同及已运行设备后续使用需独立审查和明确授权。
- M10 PENDING/三 JPEG历史FAIL、UI-C1 FROZEN、M11 NOT_AUTHORIZED；所有历史 NOTE/UNKNOWN 保留。H2/C2绝对未触碰。
- [集中报告](../research/M10_JPEG_ANDROID_ISOLATED_RESUME.md)、[真实结果](../research/m10-jpeg-android-isolated-resume/evidence/RESULTS.json)、[证据索引](../research/m10-jpeg-android-isolated-resume/evidence/INDEX.json)。停止等待 ChatGPT 独立验收，不进入下一阶段。
