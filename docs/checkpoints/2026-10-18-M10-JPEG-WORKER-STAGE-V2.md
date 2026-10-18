# M10 JPEG Worker / Stage V2 候选停止点

- 来源：`717d541578ebf761e2e1c70a842ca8b7e5dab5b2`；分支`codex/m10-jpeg-worker-stage-v2-candidate`；独立worktree`E:/CODEX/.codex/worktrees/lithic-jpeg-stage-v2/砺石新APP`。
- 用户本轮集中授权私有版本化临时协议、Worker/JNI/App候选及离线测试/构建；没有设备实验许可。此授权不合并到正式分支、不发布或进入M11。
- 已实现V2源/owner/consumer pin、撤销、严格凭据、恢复准入/同lease清理；独立非导出Worker、受控FD、主进程监管/认证退休与弃置收口。PNG仍v1，旧v1 JPEG保守保留，不能从已拥有final推断新的严格源验证。业务库和算法/UI未改。
- 真实236唯一JVM PASS，另9 Python PASS；Lint0错误/23警告；最终Debug与Android测试源码编译PASS。计数与源文件、XML、失败历史、APK/SHA以[统一报告](../research/M10_JPEG_WORKER_STAGE_V2_REPORT.md)及[结果索引](../research/m10-jpeg-worker-stage-v2/RESULTS.json)为准。
- 原始三JPEG Android FAIL保留/本轮NOT_RUN。Android身份、Binder/FD/死亡/解绑/lease、取消、恢复和内存安全未验证；B1/R3 BLOCKED。源流阻塞、全量stage扫描性能、UNKNOWN残留不作已解决结论。512MiB只为请求计费，不是设备硬内存上限。
- 正式M10 PENDING；UI-C1 ACCEPTED/FROZEN PASS WITH NOTES；M11 NOT_AUTHORIZED。D2A-N01～N05、A1-N01～N04与全部历史FAIL/UNKNOWN/NOT_RUN保留。H2/C2未访问；旧现场状态只继承历史。
- 停止：BLOCKED — M10-JPEG-WORKER-STAGE-V2-CANDIDATE。仅提交/推送本候选，等待ChatGPT独立审查。不得自动安装、重跑normal、恢复H2/C2或扩大产品规则。
