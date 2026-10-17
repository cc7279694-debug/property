# M10-R3-F1B-G2R-U1 实际测试结果

2026-10-17。范围为HOST_ONLY及有限只读UID观察，不是Android预检或生命周期验收。

## RED / GREEN

实际执行`python -B docs/research/m10-jpeg-r3-f1b-g2r-u1/run_tests.py --tag red`，然后`--tag green`。

| 记录 | 唯一方法 | PASS | FAIL | ERROR / SKIP | 原G1通过 | 外层退出 |
|---|---:|---:|---:|---:|---:|---:|
| [RED JSON](m10-jpeg-r3-f1b-g2r-u1/evidence/red.json) / [日志](m10-jpeg-r3-f1b-g2r-u1/evidence/red.log) | 103 | 93 | 10 | 0 / 0 | 81/81 | 1 |
| [GREEN JSON](m10-jpeg-r3-f1b-g2r-u1/evidence/green.json) / [日志](m10-jpeg-r3-f1b-g2r-u1/evidence/green.log) | 103 | 103 | 0 | 0 / 0 | 81/81 | 0 |

103＝原38分类＋27组合/原监督器集成＋16 stage＋新增22 UID/证据检查。每个方法ID逐项保存在JSON；GREEN ID集合包含原G1全部81个ID，原三个测试源码未修改。RED日志有13个失败事件（包含subTest），归一为10个失败方法，不能用事件数充当唯一方法数。无import error；主要正向复现是当前`uwb`触发原`unknown ps UID label`，以及预期UID冲突/Worker检查被该更早异常遮挡。

RED前只做等价提取共享解析函数，尚无uwb兼容；[RED gate快照](m10-jpeg-r3-f1b-g2r-u1/evidence/red-gate.py.txt)SHA与RED输入登记一致。GREEN仅增加精确映射，测试文件未调整。各次实际输入字节/SHA和完整日志SHA随JSON登记；宿主测试全程启用subprocess tripwire，不启动真实监督器CLI或ADB。

## 新增22方法验证内容

- 实际锁定`uwb`标签的合成进程行，仍保留真实门禁对研究进程行的处理；共享解析器必须返回1083。
- 完整组合门禁及实际`safe_idle()`通过FakeCollector消费新规则。这里的两轮和PREFLIGHT_ELIGIBLE全为明确的HOST_SYNTHETIC数据，非设备结果。
- 研究包名使用uwb/错误UID、Worker名使用uwb、同研究UID未知名称、研究未知子进程均阻塞。
- uwb近似标签及其他未知u前缀标签继续阻塞；数字UID/原uN_aM/root/usb既有行为保留。
- 没有添加数字列预检profile；错误profile、失败/诊断/截断/hash或长度冲突、缺列/重复PID仍拒绝。
- 对真实受限观察进行宿主审计：数字1084/10218/10219冲突、缺失/多值/错误类型、关联计数缺失、数字列未被实证支持、命令失败/不完整、SHA/长度缺失、包UID冲突、第二捕获缺失、错误serial、未知异常标签/研究行均拒绝。

数字冲突测试属于**本轮取证证据审计**，不是新设备预检模式。真实取证时已在内存对异常行按USER/PID/PPID/NAME关联；宿主只能审计保留下来的结果，不能重放已不保存的完整行。

`python -B .../audit_uid.py`实际退出0，[结果](m10-jpeg-r3-f1b-g2r-u1/evidence/uid-observation-audit.json)引用锁定观察SHA，并明确`devicePreflightRun=false`、`fullRowsReplayable=false`。

## 实际设备观察与未执行项

| 项目 | 状态与限度 |
|---|---|
| 目标ADB/AVD/API36/boot及三包UID/APK SHA | PASS，仅有限诊断时刻 |
| 一个legacy ps和一个实际支持UID列的ps | PASS，返回0、完整字节/SHA记录；并非预检 |
| 异常uwb行关联和数字UID1083 | PASS，本轮1/1；无无关名称/PID归档 |
| 整体进程双快照一致性、完整G1预检 | NOT_RUN，本轮禁止 |
| ServiceRecord、stage/current/pin/receipt检查 | NOT_RUN，不用G2-R旧结果冒充本轮 |
| 正式私有元数据前后比较、账本/图片字节完整性 | NOT_RUN，未读取私人内容 |
| APK安装/启动/停止、Outsider泄漏复验、normal及29场景 | NOT_RUN，本轮禁止 |
| Gradle/Lint/APK重建、Android/JVM Java测试、原JPEG重跑 | NOT_RUN，无产品/Java变更；非本轮范围 |

原G2-R门禁BLOCKED及缺失原UID/SHA保持历史事实，不回填；历史R3原29场景仍0 PASS/1 UNKNOWN/28 NOT_RUN。已有产品FAIL/UNKNOWN不因本轮103个Python PASS改变。
