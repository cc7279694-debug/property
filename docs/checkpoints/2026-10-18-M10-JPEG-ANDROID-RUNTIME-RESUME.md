# M10 API26 runtime resume checkpoint

- 来源 `3474f639c2a831d26e74eeeff5ec8db0f6ea7a3f`；分支 `codex/m10-jpeg-android-runtime-resume`。本轮新授权不是旧失败改判。
- 原 `Lithic_M10V_API26_20261018/emulator-5624`，launcher32880/QEMU32032/创建时间仍匹配。新增create0/start0；独立steady-state proof观察boot1，原300秒时限NOT_PROVEN、旧marker/失败不改。
- 原两APK各安装一次，405条定向ADB中401只读、2安装、2instrumentation；234宿主方法232PASS/2SKIP。
- 健康单次JUnitPASS，原外层CRLF读取UNKNOWN保留，精确原字节追加复核PASS且不重跑。缺损单次JUnitPASS、严格code117拒绝，完整门禁因日志前缀旋转UNKNOWN。
- 后续7项/原三JPEG断言/API36均NOT_RUN。结束只读无活动App/Worker，2个合成Stage终结；AVD继续运行。不得自动重测、退出、清数据或重建；不访问H2/C2。
- M10 PENDING/三历史JPEG FAIL、UI-C1冻结、M11 NOT_AUTHORIZED和全部历史NOTE保留。下一步只独立审查日志连续性采集和现有证据。
- [集中报告](../research/M10_JPEG_ANDROID_RUNTIME_RESUME.md)、[结果](../research/m10-jpeg-android-runtime-resume/evidence/RESULTS.json)、[原始证据索引](../research/m10-jpeg-android-runtime-resume/evidence/INDEX.json)。
