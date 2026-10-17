# M10-R3-F1B-G2R-U1 执行报告

2026-10-17。**READY_FOR_REVIEW — M10-R3-F1B-G2R-U1**，等待ChatGPT独立审查；不自授ACCEPTED/FROZEN。

## 契约与来源

Goal：有限只读取证定位UID标签，按证据最小修正研究Python解析器，并执行宿主回归。来源提交`f4de91342410b58d63526182a1402486e12e3990`；原G2-R本地/远端一致、工作区干净。使用原生worktree工具创建独立目录`E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1b-g2r-u1/砺石新APP`，新分支`research/m10-jpeg-strict-r3-f1b-g2r-u1`。修改前读取AGENTS、PROJECT、CURRENT_STATE、相关决策/架构、G1/G2/G2-R报告及实际gate/verify/测试和原始失败记录。

Scope仅UID取证、Python研究解析/宿主测试、新研究文档与checkpoint。完整设备preflight、App安装/启动/停止、Outsider、normal、其余29场景、Service/stage清理、生产代码、Java/Manifest/Gradle/APK、JNI及M11全部不在授权内。本轮未运行这些操作，也未修改正式验收矩阵。

## 实际环境及取证

初始ADB看到`emulator-5596 device`；Windows既有launcher PID16988、QEMU PID20516对应同AVD/5596及禁snapshot-load/save参数。原AVD已运行，**启动次数0**，未重启或操作snapshot。设备命令随后确认`Lithic_M01_API_36`、API36、boot=1。

三包通过`run-as id -u`、`pm path`及设备端APK SHA核查：

| 包 | UID | 实际APK SHA-256 |
|---|---:|---|
| 正式`com.cooper.lithic.debug` | 10216 | `4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46` |
| Probe | 10218 | `230407d8bc98aeac6fcda632b935551e8ca3322cbdf55cb73baf8bc258cb47d0` |
| Outsider | 10219 | `214a6e98085d77d8fffb0c5be9f9aff70700c0f1e6365a3042b6ccf72385b358` |

这是旧研究APK的当前身份；没有安装F1-A新APK。没有读取账本、图片或正式App私有文件内容；本轮没有新的私有元数据前后比较或私人字节验证，均不能继承旧观察为本轮PASS。

08:07:15 UTC仅读取一次`ps -A -o USER,PID,PPID,NAME`，再一次`ps -A -o USER,UID,PID,PPID,NAME`。完整stdout仅在诊断进程内存处理，进程结束即释放，未归档其他应用名称/进程行。两个实际header均与请求一致，数字UID列确实受支持、每行UID为ASCII十进制；不是凭列名推测语义。

| 实际命令列 | 返回码 | 字节数 | 完整stdout SHA-256 |
|---|---:|---:|---|
| USER,PID,PPID,NAME | 0 | 18017 | `6c9b49f1d3511e172fe0829e7726e32c93c5ebe13be093f6b90aac2788851938` |
| USER,UID,PID,PPID,NAME | 0 | 20270 | `0cb516fe5178faa3cfc4c49b8b1508efbad5efeff2f812dd22cefb2d8bb2cb28` |

两次捕获各374行。原谓词唯一不支持标签为ASCII **`uwb`，1行**；第二次数字列为**1083**。诊断在内存按PID定位后严格比对USER、PPID、NAME，异常行关联1/1；记录不保存该无关进程的PID/名称。数字采样时研究相关行为空。这不是全部进程的双快照一致性检查，也不证明整个时段不存在Worker。

[原始受限观察](m10-jpeg-r3-f1b-g2r-u1/evidence/uid-20261017-04/uid-diagnosis.json)SHA为`fe912e2f0fdcd196e63891b1013ca8f0c415bd689e495866d407273b26d180da`，保留每条真实命令、时间、返回码、字节/SHA和最小结果。完整私有进程行无法独立重放，哈希也不等于原文公开或身份认证。

## 前置采集失败如实保留

前三次均在任何ps读取之前停止：[01](m10-jpeg-r3-f1b-g2r-u1/evidence/uid-20261017-01/uid-diagnosis.json)误将Windows emulator-console的额外空行判为AVD不匹配；[02](m10-jpeg-r3-f1b-g2r-u1/evidence/uid-20261017-02/uid-diagnosis.json)/[03](m10-jpeg-r3-f1b-g2r-u1/evidence/uid-20261017-03/uid-diagnosis.json)的诊断工具路径模式漏掉Android安装目录`~~`。期间校正身份文本边界和路径检查为现有gate使用的精确包路径模式，不涉及UID或预检放行规则。三次外层退出均1，原始记录不改；01未记录脚本SHA，不补造。

实际共36次ADB只读查询：31条有逐命令完整性记录，另5条初始devices/身份显示只保存观察事实、无原始输出SHA。**总计ps只有2次**；预检调用0、应用生命周期命令0、设备写入0。数量是命令计数而非测试PASS数，详见[执行回执](m10-jpeg-r3-f1b-g2r-u1/evidence/execution-receipts.json)。

## 最小修改及证据对应

- `m10-jpeg-r3-f1b-g1/gate.py`：将原有UID算法提取为`resolve_ps_uid()`供真正的`process_rows()`和证据审计共用，仅新增精确`uwb → 1083`。没有扩大正则/忽略所有未知UID，没有第二数字采样的预检fallback。
- `m10-jpeg-r3-f1b-g2r-u1/diagnose_uid.py`：本轮固定只读一次性采集器，输出目录已存在时拒绝覆盖；当前源码SHA与04实际执行记录一致。
- `audit_uid.py`：仅在宿主核对已保留的标签/数字UID、关联计数和命令元数据；使用同一个UID解析函数。结果标明`fullRowsReplayable=false`，不是重跑设备或独立验证原始行。
- `test_uid.py`、`run_tests.py`：新增22个唯一方法；保留原81个测试文件、身份及断言；subprocess tripwire禁止宿主测试调用ADB。
- 三份U1报告、唯一CURRENT_STATE、新checkpoint及新证据目录；其余文件不改。没有数据库、Repository、文件/媒体协议或产品配置变化。

先以原解析行为取得真实RED（93 PASS/10 FAIL，103个方法），再加入精确映射取得GREEN（103 PASS）。RED代码快照/输入哈希、日志和结果均保留。[测试报告](M10_JPEG_R3_F1B_G2R_U1_TEST_RESULTS.md)列明方法计数和限度。

本轮确定的是**当前取证时刻可复现的兼容缺陷**。G2-R首次失败输入没有保留UID原值/SHA，不能宣称当时必定也是uwb，不能用本轮SHA回填历史。

## 交付边界

提交/推送仅当前U1研究分支，最终提交SHA与远端一致性取Git回执；不合并main。独立审查后才可另行授权设备完整预检。本轮未取得PREFLIGHT_ELIGIBLE，所有Android生命周期验证仍NOT_RUN。风险及继承状态见[决策](M10_JPEG_R3_F1B_G2R_U1_DECISION.md)。
