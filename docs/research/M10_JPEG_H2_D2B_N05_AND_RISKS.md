# H2-D2B — N05与审查来源

2026-10-17。仅设计/源码审查；不更改D1失败终结语义。[线程及阻塞设计](M10_JPEG_H2_D2B_THREAD_TIMING_DESIGN.md)。

## N05准确原因

以D2A锁定源为准：ProbeRunner第201～216行只在expectedSuccess时调用markTerminal；InvocationFence第109～115行还要求receiptCommitted和gate.canPrepare，后者仅允许SUCCESS、唯一结果、FD关闭与匹配死亡。因此非SUCCESS即使资源已证实终结，仍不会释放内存Ticket。

取消不能概括成“清理已完成、只漏markTerminal”：D2A取消分支先gate.revoke，随后结果可能被拒绝，require(s.open())也会阻止cleanup。D2B没有运行这些路径，不伪称已证实取消安全收口。

## 三种情况的最小方案

| 分类 | 当前处理 | 后续独立批准后的最小门槛 |
|---|---|---|
| 正常取消 | CANCELLED / RECOVERY_REQUIRED，准入继续关闭 | 不造SUCCESS receipt；证明持久撤销、连接/消费者终结、完整terminal proof |
| 已验证失败终结 | 仅设计为FAILED_TERMINAL_ELIGIBLE，不自动重开 | 另获恢复授权后，专用markFailedTerminal核对不可变完整恢复证明与Ticket/epoch；保留首次失败，不恢复旧回调授权 |
| 资源未知失败 | RECOVERY_REQUIRED / UNKNOWN，继续关闭 | 不删残留、不造proof、不标terminal；任何缺失条件均阻塞 |

**本轮不实现markFailedTerminal，不将失败Ticket批量terminal。** D1明确“失败→独立获准恢复并核验全部前提→FAILED_TERMINAL”；其20项模型前提包含explicitly_authorized。仅几个任意布尔值、单次ps无Worker、可瞬时取得lease或报告FAIL都不能作为恢复证明。

后续证明至少包括完整owner/attempt/epoch、旧authority及绑定/重启收口、Host和Worker各自FD终结、持续consumer lease、current/namespace、源inode/hash、同owner撤销回读、无pending/未知项、terminal proof及保留元数据哈希。慢校验在短围栏外，短边界只确认已核验不可变证明身份，防止旧证明终结新Ticket。失败恢复应保持receipt=ABSENT，不调用正常成功receipt制造事实。

原D1 22方法本轮单独重跑PASS，只证明原模型；拟议专用失败终结接口、真实恢复、取消、设备proof均NOT_RUN。若要自动为非SUCCESS重开准入，必须先独立审查，不能当作本轮小修。

## D2A审查来源缺口

| 项目 | 可确认来源 | 本轮处理 |
|---|---|---|
| D2A独立PASS WITH NOTES | 当前用户消息，范围仅研究Java/145 JVM/22模型/研究构建 | 记录来源与限度，不扩为Android通过 |
| D2A-N05语义 | 当前用户第5节明确提出的非SUCCESS终结问题、真实源码 | 如上单独审查及方案，不改冻结边界 |
| 独立编号D2A-N01～N05原文 | 仓库无对应归档；已请求用户粘贴，未收到 | NOT_PROVIDED；不宣称已完整读取/逐条实施 |
| 旧F1-A N01～N03与H2-N04 | 原报告、D1/D2A实现和历史证据 | 保留原身份与未关闭状态，不重编号为新审查NOTE |

原始引用ChatGPT对话目前可读取的最新资料仍停留在旧M10交付，不能用来补造D2A审查备注。未向其他对话发送消息或自行授予验收。

## 停止条件

当前API26/27身份异步与同步onBind门槛存在功能兼容缺口；两条最小路线交独立设计审查，代码不越界修改Worker。故D2B为BLOCKED，即使宿主测试与构建成功也不能进入设备验证。

H2原FAIL/ANR/UNKNOWN、N01～N03历史、原R3 0 PASS/1 UNKNOWN/28 NOT_RUN与完整生命周期BLOCKED、M10 PENDING/三JPEG FAIL、UI-C1冻结PASS WITH NOTES、M11 NOT_AUTHORIZED均保留。本轮无ADB、APK安装/运行、stage恢复或私人数据读取。
