# C3-2 一次性只读观察

日期：2026-10-27。分支 `codex/lithic-v1-product-completion`，来源、本地/远端起点均为 `94f9e6e699cf8bf7281dfc8ca44987d8efbcb85f`，开始时工作区干净。

**C3-2 INCONCLUSIVE — STOP NATIVE CAPTURE INVESTIGATION**

## 实际停止点

唯一观察会话于 `2026-10-27T04:11:31.036478Z` 开始，`04:11:31.269250Z`停止，耗时 **0.235秒**。仅执行一次现有SDK的 `devices -l` 发现命令，exit0，输出118字节；没有执行任何带目标序列的shell/包/进程查询。单命令、单会话输出和时限均在10秒/64KiB、90秒/256KiB边界内。

只读解析该次已保存清单确认：一个非模拟器项，与原授权硬件序列匹配，状态`device`，型号标签`23127PN0CC`；字段为product/model/device/transport_id，没有`usb:`字段。**这只是设备清单级证据；未再执行getprop/API/Boot及当前用户核验。**

本轮桌面读取器错误地增加了以下前提：`同一序列 AND device状态 AND 存在usb:字段`。前两项实际成立，第三项不成立，触发停止。原始停止标签`AssertionError: USB authorization/identity mismatch`过宽，**不能据此认定手机身份变化、未授权或设备异常**。问题是本轮桌面准入解析条件，不是C3-1的120秒超时根因。没有修补读取器后继续，没有重启计时或再发起观察会话。

## 分项结果

| 项目 | 本轮状态 | 证据及限制 |
|---|---|---|
| 原序列匹配、清单连接状态和型号标签 | CONFIRMED | 仅此次清单；序列号不公开 |
| 独立型号/API/Boot核验 | NOT_RUN | 在设备准入解析处已停止；是否仍同Boot为UNKNOWN |
| 两包当前安装、UID | NOT_RUN | 没有访问PackageManager，不能把历史10435/10437当作本次实测 |
| App/Test当前PID、创建身份 | NOT_RUN / UNKNOWN | 未执行pidof/限定PID查询；不沿用旧PID6206 |
| Instrumentation活动状态 | NOT_AVAILABLE / UNKNOWN | 桌面审查未找到安全独立活动查询入口；未执行全局ActivityManager/processes dump，也未以注册列表冒充活动状态 |
| 新包ComponentActivity状态 | NOT_RUN | 精确组件只读入口已作桌面审查，但本次未执行 |
| 有界目标错误日志 | NOT_RUN | PID/UID/创建身份等前提未建立；没有调用logcat，包括help查询 |
| 手机内存、电量、温度、SystemUI | NOT_RUN | 没有执行目标系统健康读取，不能登记设备健康PASS |

观察前后仅在电脑核对既存ADB服务：PID16216、创建时间一致；两次本机只读协议版本请求均为41。没有请求启动或重启ADB服务。SDK发现命令使用明确的现有本机服务器；没有对其他设备发送命令。

## A的UNKNOWN与停止结论

**没有收敛A的实际运行阶段或根因。** C3-1仍只有JUnit开始通知；A保持UNKNOWN_TIMEOUT，B～F2七项NOT_RUN，可提取截图0张；未追认PASS、FAIL或正常结束。当前进程、Instrumentation、Owner/FD状态均未得到新手机证据。

没有继续自动截图的合理条件或授权。本次只读会话已开始并停止，保留其失败判定和原件；不借“目标shell执行0次”自动发起第二轮，不自行进入C3-3、重跑A或剩余方法。原生截图调查停止，等待ChatGPT独立审查。**APP_VISUAL_PENDING / DEVICE_RUNTIME_STATE_UNKNOWN。**

## 保护与证据

本轮手机写入、安装/更新/卸载、测试、进程终止、设置更改、目录枚举/读取/清理、截图、构建及Preview6均0；没有读取旧Preview/M10/M22或任何业务数据。原安装身份保护结论仅继承历史报告，本轮未重新证明安装字节保持。生产/测试/配置源码、Room5/backup4、业务/Owner/恢复/Nav3/媒体/提醒协议及全部历史FAIL/UNKNOWN/NOT_RUN/NOTES未变。不合并main、不进入M23。

- [本次脱敏回执与状态](ui-c2-c3-observation-20261027-040802/observation.json)：含原命令、退出码、时间/输出边界、原始118字节SHA、原停止标签及更正解释。
- [C3-1D阶段审查及只读建议](UI-C2-C3-Timeout-Desktop-Review.md)；[C3-1原执行报告](UI-C2-C3-Native-Capture-Execution.md)。

原始清单、会话claim和结果只保留于本轮受控忽略目录 `build/c3-2-observation-20261027-040802/`。原始清单SHA为 `2bb427071aa21dc5cf2427ba392496d3cc5eb36ae959530e90d0db6bbb17b5b6`；未公开原文或硬件序列号。读取器源码保留于`build/c3-2-readonly/observe_once.py`，未在运行后修改，其SHA/长度见回执；不把该临时电脑脚本当作产品/测试代码交付。
