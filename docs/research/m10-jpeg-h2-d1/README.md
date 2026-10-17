# H2-D1 纯宿主模型与离线证据

**MODEL_ONLY / DESIGN_ONLY**，不是Android实现、恢复工具或设备测试入口。所有模拟source删除只翻转Python布尔值，绝不访问设备/真实stage。RecoveryFacts的True是测试手工构造的独立已证前提，不能从测试数据反向宣称H2已满足。

## 文件与执行

- [analyze_h2.py](analyze_h2.py)：只读锁定仓库源码/证据，验证原SHA并派生30份输入、39条观察的索引/时间线。
- [fence_model.py](fence_model.py)：拟议控制状态模型；单次方法是原子转换的**假设**，不模拟Java锁调度、Binder、FD、socket、fsync或磁盘原子性。
- [test_fence_model.py](test_fence_model.py)：22个唯一方法；120种失败后事件顺序是其中1个方法的内部枚举，20个恢复前提各False/None也是内部子场景，不额外计数。
- [verify_models.py](verify_models.py)：仅启动固定Python unittest子进程，输出目录独占创建，不允许覆盖已有日志。复跑模型可使用下方标准unittest，不触发任何研究APK。

```text
python -B -m unittest discover -s docs/research/m10-jpeg-h2-d1 -p test_fence_model.py -v
python -B docs/research/m10-jpeg-h2-d1/analyze_h2.py
```

本轮没有ADB、Gradle、研究APK构建或设备生命周期操作。原Java/JVM、G1/U1及产品测试均未重跑。

## 实际结果

| 阶段 | 结果 | 真实依据 |
|---|---|---|
| RED相关旧谓词模型 | 1方法FAIL、退出1 | [原始CP936日志](evidence/red.log)、[无损UTF-8解码](evidence/red-decoded-cp936.txt)、[源码/测试快照及SHA](evidence/red-result.json)。失败为True is not false；模型假定身份/原授权有效，失败只写报告。不是运行Java，也不是H2早期bind阶段已发生迟到grant。 |
| 第一GREEN | 22方法PASS、退出0 | [结果](evidence/green-result.json)、[日志](evidence/green.log)。 |
| 最终GREEN | 22方法PASS、退出0 | [最终输入/方法名单](evidence/green-final/result.json)、[日志](evidence/green-final/unittest.log)。仅将“其他attempt均已清点”前提收紧为“其他attempt已终结或不存在”，方法身份不变。 |
| 离线取证派生 | PASS，限输入与算术 | [30份输入](evidence/input_manifest.json)、[39条观察](evidence/timeline.json)、[派生结果](evidence/analysis_results.json)。 |
| Android实际实现/恢复/ANR改善 | NOT_RUN | 当前禁止；不能从模型PASS外推。 |

RED运行的测试结果与原始日志已保存后，父进程首次显示误用UTF-8解码而报错；[解码回执](evidence/red-decode.json)保留此事实。原CP936字节/SHA不改、解码可精确往返；行为RED由其真实断言FAIL证明，编码错误不算测试红灯。最终GREEN明确用UTF-8。

## 模型与设计对应及局限

Fence默认代表已经认证的旧Session，是N04静态反例的起点；H2实际未reserve，所以另用未握手POSTED状态验证失败后不调用绑定。拒绝REVOKED状态是N01依赖的设计检查，原N01没有实施或关闭。

短失败Gate、明确epoch、持久失败阻塞、迟到绑定隔离、解绑未知、结果/死亡乱序、旧endpoint、恢复前提及失败终结是本模型范围。`other_attempts_terminal_or_absent`不是“只认识名字”；它代表全namespace已验证且除目标外全部终结或不存在。

模型终结后重放用的是已验证terminal证明承接原source归属的抽象事实，不要求读取物理上已删除的源；实际terminal身份/哈希和磁盘持久化仍需未来Android验收。模型不证明恢复已获授权、所有真实消费者死亡或真实lease持续占有。prepare/receipt及current发布跨慢I/O与失败的线性化也未建模，列为未来实现硬门槛，不宣称全状态空间证明。

H2原FAIL、UNKNOWN、非终结pin/source和历史三项JPEG FAIL保持。D1没有当前设备观察；下一步只允许独立审查三份设计。
