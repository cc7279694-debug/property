# LITHIC HOME FINAL POLISH 08

基线 `2232c377bd5ddd174c85edcd747d45bac031e50d`。仅处理首页舍入说明、已有三根装饰柱删除的视觉验证及其 Android 验收。[Final07 报告](LITHIC-Home-Final-Polish-07.md)

## 变更

- `HomeOverviewUiTest` 保留六项方法、金额精度、负值、合计及可见性断言。将语义滚动与手势分成重新打开面板的两次操作，记录窗口、节点、语义父节点、滚动范围、Expand/Collapse 及实际显示状态。新增完整文字及屏幕矩形检查。
- 现有真实 MainActivity 导航方法复用手势检查和截图。组件诊断图片与 MainActivity 证据分别归档，避免将组件 Activity 画面当成生产 Activity。
- 生产源码与 Final07 一致，保留无装饰柱及 200% 字体分行。本机验收控制器沿用 450,000 kB 门槛，低于门槛时只在同次启动内观察最多 120 秒，保留 180 秒设备准入、额外 60 秒健康窗口、ANR、核心进程及原包身份保护。

## 执行结果

**LITHIC_HOME_FINAL_POLISH_08 — ENVIRONMENT_BLOCKED**

| 项目 | 状态 | 证据 |
| --- | --- | --- |
| Debug / AndroidTest 构建与签名 | PASS | 最终编译退出 0，两份 APK 原 Debug 证书一致，App 与 Final07 逐字节相同。[身份与源码绑定](lithic-home-final-polish-08/source-apk-binding.json) |
| 设备准入 | PASS | 原 Repair05 AVD 本轮启动一次。唯一 `emulator-17640`，SDK ADB 37.0.1／5038，API36 x86_64，实际 1536 MiB／2 核，约 103.5 秒完成启动及身份核对。[准入命令](lithic-home-final-polish-08/admission-commands.jsonl) |
| 安装前系统健康 | BLOCKED | 10:17:23.910 UTC guest 日志记录 SystemUI PID 1004 启动 ANR。10:17:38.048 UTC 又记录 Phone PID 1163 启动 ANR。未进入安装前包查询、APK 更新或测试。[ANR 事件](lithic-home-final-polish-08/anr-events.txt) · [控制器回执](lithic-home-final-polish-08/controller-result.json) |
| 低内存等待与额外 60 秒观察 | NOT_RUN | 安装前资源探针为 482,760 kB，达到原 450,000 kB 门槛，因此未进入 120 秒低内存等待。ANR 守卫在额外健康观察前阻断。[内存探针](lithic-home-final-polish-08/guest-health.txt) · [时间线](lithic-home-final-polish-08/timeline.jsonl) |
| 舍入说明专项 | NOT_RUN | 没有语义树、面板坐标、滚动量或真实手势结果。 |
| 首页完整回归 | NOT_RUN | 43 项计划方法均未运行，0 PASS／0 FAIL／43 NOT_RUN。 |
| MainActivity 舍入说明、纯白首页和 200% 大字截图 | NOT_RUN | 本轮 0 张截图。Repair06 的历史大字截图继续只作为该版本的证据。 |
| 字体与夹具 | NOT_RUN | 本轮没有改字体、创建合成账本或写测试封面，无需恢复或清理。 |
| 安全退出 | PASS | 本轮模拟器、四路日志和独立 5038 正常退出，无残留进程或端口。5037 原 PID 16276 和创建时间前后一致。[退出记录](lithic-home-final-polish-08/emulator-cleanup.json) · [ADB 回执](lithic-home-final-polish-08/receipt.json) |

30 项离线准入与采集回归通过。启动前主机可用 5.473 GiB，C/E 盘剩余约 17.41／26.00 GiB。ANR 前每约 5 秒采样，共 9/9 份 guest 有效样本，MemAvailable 为 846,436–1,130,912 kB。启动结束后单独的 482,760 kB 探针与前述范围处于不同时间段。已保存 15 份主机样本，guest 内核 `oom_kill` 计数均为 0，已捕获日志未见明确 lmkd 杀进程事件。原 07 内存失败记录保留。[离线测试](lithic-home-final-polish-08/offline-tests.txt) · [guest 采样](lithic-home-final-polish-08/guest-resource-samples.txt) · [主机采样](lithic-home-final-polish-08/host-resource-samples.jsonl)

SystemUI 的 DropBox 线程栈与 PID、包名及时间匹配。抓栈时主线程为 Runnable，位于 Provider 初始化中的 SystemUI/WMShell 依赖构建。该栈只说明抓栈时的位置，ANR 的底层原因仍为 UNKNOWN。Phone 只记录事件，未取得归属于其 PID 的线程栈。原始日志、完整栈与控制器保存在本地 `E:/CODEX/lithic-home-final-polish-08-local/`。[SystemUI 主线程](lithic-home-final-polish-08/anr-thread-dump.txt) · [原始文件指纹](lithic-home-final-polish-08/raw-sha256.json)

## 舍入说明结论

Repair06 确认节点存在，旧测试在语义滚动后的可见性断言失败。已核对的 Compose 行为显示，内部语义滚动和面板展开是不同操作，但本轮未取得实测坐标或真实上滑结果。舍入说明不可见的真实原因仍为 **UNKNOWN**，生产布局保持 Final07。已完成的改动仅为测试诊断和真实 MainActivity 截图接线，等待端侧验证。

原 userdata 两层文件身份未变，基础镜像哈希保持。QCOW2 启动后为 635,437,056 字节，SHA-256 `e5563ad80b7f74953bf48fbe7ffcc0173fb6582a7c73d3cd9550341ff6a14d78`。未重启、擦除或访问真实账本，未发布 APK。[退出后 userdata](lithic-home-final-polish-08/userdata-after-shutdown.json)
