# H2-D2B-A1 — 离线验证、RED/GREEN与APK实物审计

2026-10-17。**HOST_ONLY / READY_FOR_REVIEW**。本报告仅证明研究Java与离线产物，实际Android启动、主线程耗时、ANR和生命周期均NOT_RUN。

## 唯一方法与独立口径

| 集合 | 唯一方法/检查 | 最终结果 |
|---|---:|---|
| 原D2B研究JUnit | 229 | PASS，原16份测试源码及断言逐字节不变 |
| 新ProcessIdentityGateTest | 66 | PASS，身份/时序/并发/读取/计时失败边界 |
| **完整研究JUnit** | **295** | **295 PASS / 0 FAIL / 0 ERROR / 0 SKIP，17份XML与源码身份完全对应** |
| 原D1模型 | 22 | PASS，单独统计，不计入295或Android |
| 原静态契约 | 8 | PASS，原文件不改，不计为行为或Android测试 |
| 新身份接入静态契约 | 4 | PASS，仅源码接入检查，不替代真实Java或Android验证 |

完整295方法及每项XML身份、原229源文件哈希、新66方法身份见[最终实物审计JSON](m10-jpeg-h2-d2b-api26-a1/evidence/build-runs/20261017T142151.230105Z/artifact-audit/result.json)。D1/静态实际命令、退出码、完整日志及准确Python方法身份见[最终离线补充结果](m10-jpeg-h2-d2b-api26-a1/evidence/offline-identities-final/result.json)。早期offline结果中的Python显示身份曾重复拼接方法名，保留原件，最终清单已规范化；没有增加测试方法或改断言。

## RED/GREEN证据与限度

1. [identity-red](m10-jpeg-h2-d2b-api26-a1/evidence/identity-red/result.json)：以明确标注的旧语义可编译adapter验证4个目标断言。javac退出0，JUnit退出1，4 run /4 FAIL；不是旧Android源码直接运行，也不是设备ANR复现。adapter仅是保留RED接口的研究fixture，正式候选实际使用新的gate。
2. [integration-red](m10-jpeg-h2-d2b-api26-a1/evidence/integration-red/result.json)：实际旧Support/Application上新4项静态接入检查4 FAIL；同次原8静态和D1 22模型PASS。这只证明原接入缺少gate边界，不计作Java行为RED。
3. [identity-review-red](m10-jpeg-h2-d2b-api26-a1/evidence/identity-review-red/result.json)：真实新gate运行66方法，其中3 FAIL（两项负值时钟、等待超期后迟到READY）。修复后[identity-review-green](m10-jpeg-h2-d2b-api26-a1/evidence/identity-review-green/result.json)66 PASS。该次RED记录的phase为green、label为review-red，真实JUnit退出1/3 FAIL原样保留，不能依据label或phase冒称成功。
4. 早期57/62方法GREEN及中间日志保留；最终66方法由完整Gradle再次执行，不重复累计方法数。

新测试验证API26/27主线程角色探测不置READY、后台迟到、API28提示不能授权Binder、未知caller/身份拒绝、后台与同步/框架冲突sticky、Binder早到/超时/中断、并发探测在飞数、读取与关闭异常、256字节/NUL/ASCII限制、单调时间未知/负值/倒退/溢出及100ms边界。子线程Throwable由测试主线程join后核查，不能靠后台线程打印异常获得假绿。

## 完整构建与工具链

沿用研究Gradle 8.14.3、AGP 8.13.2、JDK17.0.20.1+1、Java目标17、SDK36/BuildTools36.0.0、min26/target36、JUnit4.13.2；没有依赖/签名/权限升级。48项输入经原字节核对投影到独立ASCII目录；未使用旧D2B/F1-A产物目录，也未构建正式`:app`。

实际任务：`:probe:testDebugUnitTest :outsider:testDebugUnitTest :probe:lintDebug :outsider:lintDebug :probe:assembleDebug :outsider:assembleDebug --offline --max-workers=2 --console=plain --stacktrace`。

最终[run/result.json](m10-jpeg-h2-d2b-api26-a1/evidence/build-runs/20261017T142151.230105Z/result.json)与[完整原日志](m10-jpeg-h2-d2b-api26-a1/evidence/build-runs/20261017T142151.230105Z/gradle.log)：**Gradle与收集脚本退出0，BUILD SUCCESSFUL in 17s，99 actionable tasks /99 executed**。17份JUnit XML均零失败/错误/跳过。Lint仍为**0错误/8警告**（probe5、outsider3），原OldTargetApi、DiscouragedApi/fixed-rate追赶、DataExtractionRules、StaticFieldLeak、MissingApplicationIcon提示保留，不抑制或升级依赖。

首次[run](m10-jpeg-h2-d2b-api26-a1/evidence/build-runs/20261017T142043.503670Z/result.json)的Gradle实际25秒成功，但外层脚本打印日志尾部时触发GBK UnicodeEncodeError、退出1；[异常记录及原脚本SHA](m10-jpeg-h2-d2b-api26-a1/evidence/build-runs/20261017T142043.503670Z/wrapper-result.json)与原脚本快照/日志保留。只修新收集脚本的控制台JSON转义后完整重跑。原日志包含Windows native编译器编码，不强行标为UTF-8；保留原字节/SHA，以ASCII成功标记及XML判定。该收集错误不是JUnit RED，不改变首次Gradle真实退出0。

## 最终APK（仅待审研究产物，未安装）

| 包 | 绝对路径 | 字节数 | SHA-256 |
|---|---|---:|---|
| `com.cooper.lithic.m10r3.probe` | `E:/CODEX/.codex/worktrees/m10-jpeg-h2-d2b-api26-a1/砺石新APP/build/a1-artifacts/20261017T142151.230105Z/probe-debug.apk` | 83,761 | `0d98316faf6cb18b589bad526a5d8260cea825a408ed949f32855bd7a7d31b8a` |
| `com.cooper.lithic.m10r3.outsider` | `E:/CODEX/.codex/worktrees/m10-jpeg-h2-d2b-api26-a1/砺石新APP/build/a1-artifacts/20261017T142151.230105Z/outsider-debug.apk` | 10,473 | `a51c4984f07986c8df52b3dbbf7f946523e4c259aa67ab5e8f9b3feffdf41879` |

离线SDK工具实际核对APK：包名、min26/target36、零uses-permission、Worker `exported=false`及`:jpeg_worker`；证书SHA-256仍为`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；DEX无JUnit/Hamcrest/AndroidX Test库，无native `.so`。31条审计命令均退出0，stdout/stderr原始字节与SHA见[commands.json](m10-jpeg-h2-d2b-api26-a1/evidence/build-runs/20261017T142151.230105Z/artifact-audit/commands.json)。

F1-A、D2A、D2B各原result所指向的六份旧APK逐一实核字节、SHA、证书，均保持原状；没有覆盖、安装、发布或提交APK/私钥/缓存。

交付核查分别记录工作区原字节及Git文本规范化后的SHA；原始证据的CRLF/空白不改写。暂存核查遇到的文本规范化及Windows长路径问题、限定处理方式另记[宿主核查工具记录](m10-jpeg-h2-d2b-api26-a1/evidence/delivery-tooling-notes.json)，不计作JUnit/Android失败或通过，也没有修改最终APK输入。

## 未执行与停止

真实API26/27/36启动、主线程/proc耗时、ANR、Binder、FD关闭、Service死亡、文件同步、H2恢复及原29场景全部NOT_RUN。本轮0设备命令；不读取私人账本或图片字节。100ms为返回后拒绝策略，不能保证读取硬中止。D2A-N01～N05继续按[兼容/风险说明](M10_JPEG_H2_D2B_A1_COMPATIBILITY.md)保留，不CLOSED。交付仅READY_FOR_REVIEW，不是D2B整体验收或M10通过。
