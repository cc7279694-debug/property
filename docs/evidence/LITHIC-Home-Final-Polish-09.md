# LITHIC HOME FINAL POLISH 09

固定起点 `8b8f22b90e9218d188f99d6674e9d0f45fd4bea5`。生产源码仍与 `2232c377bd5ddd174c85edcd747d45bac031e50d` 一致。仅调整现有 AndroidTest：真实 MainActivity 的导航方法先记录语义滚动，再关闭、重新打开面板验证展开和真实上滑。诊断阶段记录滚动量、窗口、节点坐标和完整语义树，原可见性、完整文案及金额断言保留。[Final08 历史证据](LITHIC-Home-Final-Polish-08.md)

## 桌面核对

30 项离线采集与准入测试通过，AndroidTest Debug 构建退出 0。复用 Final08 的生产 Debug APK，重新核对本轮 App/Test 的原证书、SHA、Runner 和目标包。未构建正式 APK，未修改生产源码、Room、备份、正式身份或签名。[离线测试](lithic-home-final-polish-09/offline-tests.txt) · [APK 与源码绑定](lithic-home-final-polish-09/source-apk-binding.json)

第一次准备阶段的 60 秒主机 CPU 平均为 14.08%，控制器读取时距采样结束 30.1 秒，触发我设置的额外时效检查。当次未启动模拟器，独立 5038 已退出。将同一 60 秒测量移到最终启动检查内，资源阈值保持，原停止回执保留。[首次准备停止](lithic-home-final-polish-09/preparation-result.json) · [服务收口](lithic-home-final-polish-09/preparation-adb-receipt.json)

## 四核单次实验

**系统健康 PASS。** 最终 60 秒 CPU 均值 12.58%，主机 8 个逻辑处理器，启动前可用 RAM 5.725 GiB、C/E 盘可用 17.40／25.84 GiB。一次启动原 Repair05 AVD，唯一实验参数为 `-cores 4`。生成硬件配置确认 4 核／1536 MiB。guest 两次读取 `cpu/online=0-3`、`/proc/cpuinfo` 的 processor 0–3，`MemTotal=1502724 kB`。API 36 x86_64、720×1648／320 dpi、SDK ADB 37.0.1／5038 保持。设备约 79 秒完成准入，原 config.ini SHA-256 前后一致，永久配置仍为 2 核。[主机准入](lithic-home-final-polish-09/host-admission.json) · [启动命令](lithic-home-final-polish-09/launch.txt) · [实际 CPU](lithic-home-final-polish-09/effective-guest-cpu.txt) · [硬件配置](lithic-home-final-polish-09/effective-avd.json)

首次健康探针为 434,600 kB。同次启动内约 16 秒后达到 495,584 kB，超过原 450,000 kB 门槛。额外 60 秒观察及结束检查通过，SystemUI 1076、system_server 759、Phone 1219 保持稳定，Activity 服务正常。观察窗口计入 13 份有效 guest 样本和 14 份主机样本。两份已知 Debug 包安装前 SHA 与 Repair06 相符，User 0 私有目录核对通过。原位更新后的 APK 字节与本地一致。[健康时序](lithic-home-final-polish-09/timeline.jsonl) · [内存恢复](lithic-home-final-polish-09/guest-memory-recovery.jsonl) · [系统服务](lithic-home-final-polish-09/system-services.txt) · [App 安装](lithic-home-final-polish-09/install-repaired-app.txt) · [Test 安装](lithic-home-final-polish-09/install-test.txt)

本轮 65 份 guest 样本均有效，MemAvailable 341,280–1,109,984 kB，覆盖启动、观测与测试负载，所有 `oom_kill` 为 0。PSI 的 CPU、内存与 I/O 字段均为 `unavailable`。71 份主机样本中 70 个 CPU 区间约为 15.08–99.61%，QEMU 累计 CPU 时间最大 439.45 秒。采集范围内无新 SystemUI 或 Phone ANR，也无明确 lmkd 杀进程记录。无新 ANR 线程栈，取栈为 NOT_RUN。Final08 的 2 核运行出现两项启动 ANR，本轮 4 核运行未复现。一次对照不足以确认 CPU 是此前 ANR 的根因。[guest 原始样本摘要](lithic-home-final-polish-09/guest-resource-samples.txt) · [Phone PID](lithic-home-final-polish-09/guest-phone-samples.jsonl) · [主机样本](lithic-home-final-polish-09/host-resource-samples.jsonl) · [ANR 记录](lithic-home-final-polish-09/anr-events.txt)

## 成本构成专项

**舍入说明真实可达性 PASS。专项 JUnit 1 FAIL。**

| 真实 MainActivity 阶段 | 舍入说明与面板状态 |
| --- | --- |
| 语义滚动前 | 节点大小 640×80 px。屏幕原点 (40,1677)，根坐标 [40,1677–680,1757]。实际窗口 720×1648，节点的窗口裁切矩形为 0×0。面板根区域 y920–1805，只可见到 y1648。滚动位置／最大值 0／0，仅有 Expand。 |
| `performScrollTo()` 后 | 坐标、裁切和 0／0 滚动范围均未改变。说明仍不可见。 |
| 重新打开并真实拖动展开后 | Expand 变为 Collapse。面板 y667–1552，说明完整进入 [40,1424–680,1504]。 |
| 面板内部真实上滑后 | 说明坐标保持在 [40,1424–680,1504]。实际窗口与节点尺寸检查、`assertIsDisplayed()` 和完整文案断言均通过。 |

根因已确认。Material3 半展开使说明落在实际窗口下方，语义滚动只能作用于内部滚动父节点，当时最大滚动范围为 0。展开后真实用户可阅读完整说明。生产布局保持。截图来自实际 MainActivity，屏幕为 720×1648／360dp，fontScale 1.0。[阶段坐标及完整语义树](lithic-home-final-polish-09/rounding-viewport.txt) · [语义滚动后](lithic-home-final-polish-09/cost-composition-before-gesture.png) · [真实展开后](lithic-home-final-polish-09/cost-composition-rounding-note.png) · [截图元数据](lithic-home-final-polish-09/screenshots.json)

两张截图完成后，原测试按一次 Back 并等待已完全展开的面板消失，12 秒超时。缓存的实际 Material3 1.4.0 字节码显示，Expanded 且存在半展开锚点时，返回处理调用 `partialExpand()`。因此本次专项的尾部操作与组件行为不符。本次收尾将测试改为第一次 Back 后明确检查 Expand 动作，第二次 Back 后仍等待面板完全消失并返回首页。修正后的 `compileDebugAndroidTestKotlin` 退出 0，Android 复跑 NOT_RUN，原实际 FAIL 及截图保留。[编译结果](lithic-home-final-polish-09/post-failure-compile-summary.txt)[失败日志](lithic-home-final-polish-09/instrument-routes-focused.txt) · [逐方法结果](lithic-home-final-polish-09/test-methods.json) · [Material3 返回分支](lithic-home-final-polish-09/material3-back-excerpt.txt)

## 验收与退出

| 项目 | 状态 |
| --- | --- |
| MainActivity 舍入说明手势、完整文案与原生截图 | PASS |
| MainActivity 专项完整方法 | 0 PASS／1 FAIL，失败发生在截图后的返回断言 |
| 专项后的完整 43 项回归 | 0 执行，43 NOT_RUN |
| 三主题六张项目／订阅首页图 | NOT_RUN。面板背景可见装饰柱已删除，完整首页画廊未执行 |
| 系统 200% 复验与两张双列卡片图 | NOT_RUN |
| 原系统字体 | 原值 1.0，finally 恢复并读回 1.0 |
| 合成账本与封面 | 专项夹具发出 `fixture-cleaned` 回执 |
| 模拟器、日志采集、独立 5038 | 正常退出，无本轮进程残留。原 5037 PID 16276 与创建时间不变 |

控制器及工作流退出码 3。原 userdata 两层身份保留，基础层 SHA 不变，QCOW2 正常写入后为 671,481,856 字节，SHA-256 `cf564ab99018c9430788b5c17edc9397614cdf549e699b3746fd11c152fcf31e`。本轮模拟器只启动一次。无正式 APK 发布或真实设备／账本访问。完整原始日志在 `E:/CODEX/lithic-home-final-polish-09-run-local/`。[字体恢复](lithic-home-final-polish-09/font-restore-read.txt) · [退出回执](lithic-home-final-polish-09/controller-result.json) · [原 5037 保护](lithic-home-final-polish-09/receipt.json) · [userdata](lithic-home-final-polish-09/userdata-after-shutdown.json)

**LITHIC_HOME_FINAL_POLISH_09 — READY_FOR_REVIEW_WITH_FAILURES**
