# M10 JPEG 集中候选停止点

- 来源 `2d0a60aca9020fccfca192091b421412f2369316`；独立分支 `codex/m10-jpeg-accelerated-candidate`。本轮允许集中研究代码/测试及隔离 JNI 候选，不允许持久 stage/数据归属变更或访问 H2/C2。
- 已测代码 `791d44e8ee2b3636031a1073c3df690585309853`；[最终 CI](https://github.com/cc7279694-debug/property/actions/runs/39109960477) SUCCESS。268 probe（GCC普通134＋Clang sanitizer 134）＋两套各12项分配测试/657断言＋两套各40个FD用例/882断言；另8次JNI调用和载入错误检查、8项Python审计。计数不冒充JUnit/Android方法数。
- 三类40MP使用R1算法新生成，SHA吻合旧锁，但不冒称取得原本地文件。GCC/Clang、ASan/UBSan/泄漏检查均未报告错误；所有观测正常返回清理平衡。计费请求、RSS、CPU/wall区分，不构成手机资源硬限或全路径GO。
- 固定NDK/CMake下四ABI API26 `.so` 已编译并审计；无App接入/APK/Android运行。首次CMake guard失败、分类RED、审计RED及历史114次证据保留，未覆写旧产物。
- **关键阻塞**：B1生命周期仍BLOCKED；v1恢复/GC缺严格源凭据及跨进程pin，安全接入须另审持久协议。不得改用同进程JNI绕过；超时/崩溃不能冒充普通图片拒绝或已释放。
- M10 PENDING、原三JPEG FAIL、UI-C1 FROZEN、M11 NOT_AUTHORIZED；D2A-N01～N05/A1-N01～N04及其他历史NOTE/UNKNOWN均继承。H2 `DELIVERY_UNKNOWN / pin=true`、C2现场未访问。
- 正式M10 app差异为空，原三项测试与规划原件不改；只推本研究分支后停止，等待ChatGPT独立审查。未授权后续设备、恢复或协议实施。

[统一报告](../research/M10_JPEG_ACCELERATED_REPORT.md) · [关键结果/SHA/原始证据索引](../research/m10-jpeg-accelerated/RESULTS.json)
