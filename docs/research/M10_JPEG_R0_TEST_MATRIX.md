# M10-R0 — 实验、证据与未运行矩阵

日期：2026-10-16。总状态：**BLOCKED — M10-R0 feasibility**。本矩阵是隔离研究登记，不改M10-T03/T04、原三项拒绝断言、原130案例或正式验收矩阵。输入SHA与详细分析见[可行性报告](M10_JPEG_R0_FEASIBILITY.md)。

## 已真实执行的预检（不是native解码实验）

| ID | 操作 | 实际结果 | 证据／限制 |
|---|---|---|---|
| E01 | 本地/远端/工作区/来源核对、创建研究worktree | PASS | 4159682及0dce7dd匹配；仅新research分支，未覆盖原工作区 |
| E02 | 检查NDK/CMake/host编译器 | BLOCKED（预检脚本本身退出0） | [环境JSON](m10-jpeg-r0/evidence/environment.json)；只覆盖列出路径；没有安装 |
| E03 | 锁定两JPEG SHA、大小、SOF及精确删除范围 | PASS（身份检查） | [locked-inputs.json](m10-jpeg-r0/evidence/locked-inputs.json)；不解码压缩数据 |
| E04 | 官方3.2.0 release/tag/2资产SHA与644 TAR路径检查 | PASS（来源检查） | [provenance](m10-jpeg-r0/evidence/upstream-provenance.json)；11成员hash，不编译源码 |
| E05 | 官方公钥指纹及.tar.gz独立环GPG验签 | PASS（验签） | [原日志](m10-jpeg-r0/evidence/gpg-relative-2.log)，退出0、VALIDSIG；不改变系统信任 |
| E06 | 既有M10生产链、原补测和官方API/内存源码读取 | 已完成只读审查 | 这是静态分析，不是R0产品测试PASS |
| E07 | 现有M10 APK ZIP ABI/库字节及哈希读取 | PASS（既有产物清单） | [ABI JSON](m10-jpeg-r0/evidence/existing-apk-abi.json)，四ABI、8库、73,584字节；没有新APK |
| E08 | 8000×5000系数/RGBA算术 | CALCULATED_NOT_MEASURED | [输入JSON](m10-jpeg-r0/evidence/locked-inputs.json)，不是native内存峰值或通用硬上界 |

工具诊断单列：首次GPG绝对Windows路径在Git/MSYS GPG中被当相对路径，退出2；保留[gpg-first-path-0.log](m10-jpeg-r0/evidence/gpg-first-path-0.log)至[-2.log](m10-jpeg-r0/evidence/gpg-first-path-2.log)，改用研究目录相对homedir后同一源码包/签名验签成功，没有改digest或换key。来源成员脚本首次假定tar含`.github/SECURITY.md`而KeyError；检查实际官方tar后改为在官方GitHub单独读安全政策，不计该成员的tar哈希。[命令登记](m10-jpeg-r0/evidence/COMMANDS.md)明确两者均非JPEG拒绝或产品缺陷测试。

## 锁样本真实解码闸门

| ID | 输入与所需断言 | TurboJPEG A1 | 逐行libjpeg A2 | 既有Android B |
|---|---|---|---|---|
| D01 | 锁定complete.jpg必须完整消费并成功，无warning；记录真实返回值/错误码 | NOT_RUN／环境BLOCKED | NOT_RUN／环境BLOCKED | 历史API36成功；本轮未重跑 |
| D02 | 锁定entropy_cut_eoi_retained.jpg必须明确拒绝，不能补图成功 | NOT_RUN／环境BLOCKED | NOT_RUN／环境BLOCKED | 历史API36错误接受，B为NO_GO；本轮未重跑 |
| D03 | 证明读取/处理全部scanlines、结束成功；仅header不满足 | NOT_RUN | NOT_RUN | marker跳过entropy不满足 |

**R0没有执行任何新的host或Android严格JPEG解码。**因此本轮真实strict返回值、warning、decoder错误码、CPU及native峰值都是null／NOT_RUN，不能填0或预计警告。GPG/脚本退出0不用于填D01/D02。

## 后续隔离PoC必须补齐（全部NOT_RUN）

| ID | 计划输入/实验 | 必需结果与记录 | 当前状态 |
|---|---|---|---|
| D04 | 健康baseline：灰度、YCbCr/RGB、既有可支持采样及边界尺寸 | success、0 warning、完整消费；不得只看像素存在 | NOT_RUN |
| D05 | 合法progressive：多扫描及合法低/高复杂度，40MP边界 | success或经批准的明确资源拒绝；不能悄悄改变兼容范围 | NOT_RUN |
| D06 | progressive中间entropy截断、扫描损伤但EOI保留 | 明确warning/fatal/拒绝；能仍语法合法的整扫描删除须单列能力限制 | NOT_RUN |
| D07 | 合法restart interval，RST前/后截断、丢失/乱序RST | 合法成功，定义的损伤明确失败；记录具体warning/错误 | NOT_RUN |
| D08 | 合法EXIF方向1～8与含元数据JPEG | 严格门禁不改字节或方向；原规范化方向/GPS剥除结果保持 | NOT_RUN |
| D09 | 非JPEG/PNG、12bit/lossless/算术、超过3组件／CMYK等既有范围外编码 | 不扩格式；明确不支持；不借缩放规则产生容量错误 | NOT_RUN |
| D10 | 64MiB±1、40MP±1、极端长宽、marker多/扫描多 | 在分配前checked长度/乘法/上限；无回绕、无静默fallback | NOT_RUN |
| D11 | 每个native分配点失败、输出容量不足、JNI分配异常 | 单一cleanup，错误分类明确，未完成输出不能当成功 | NOT_RUN |
| D12 | header/扫描/行边界取消、超时，循环调用后余额 | 确定停止粒度、释放decoder/缓冲/lease；Job取消不等于native停 | NOT_RUN |
| D13 | Native库不存在/ABI错误/符号缺失、初始化失败 | 明确fail-closed；不回退BitmapFactory成功，不假装图损坏已证实 | NOT_RUN |
| D14 | crash/SIGABRT/SIGSEGV/OOM/监督超时 | supervisor记CRASH/KILL/TIMEOUT/LOAD_ERROR；不算普通REJECT或解码成功 | NOT_RUN |
| D15 | baseline/progressive最坏工作量与并发1 | 原输入＋库分配＋系数＋条带＋JNI合计预算；peak/CPU/walltime及所有分配跟踪 | NOT_RUN |
| D16 | 四ABI交叉编译/加载、API26及已有API36 | 每ABI编译，设备实际覆盖另列；保持原四ABI，16KiB ELF/APK核对 | NOT_RUN |
| D17 | strip后.so、APK压缩/对齐、依赖/STL与许可证 | 各ABI与最终APK真实增量；不引用Windows安装包作估计 | NOT_RUN |
| D18 | 健康相机/编码器语料的严格warning误拒/未检出率 | 说明语料来源/数量；未采集私人图，不编造误拒百分比 | NOT_RUN |

本轮没有生成任何新fixture。D04～D10未来优先以固定上游cjpeg和原创确定性像素源生成，逐一保存生成命令/工具版本/源数组算法/seed/字节/SHA，并验证合法fixture确实合法；只有计划，没有现成样本或假哈希。锁定两张原件永不替换；新增变体另起文件和身份。

## 后续生产修复门槛（本轮不实施）

| ID | 必须保留并重新验证 | 本轮状态 |
|---|---|---|
| P01 | 原3个JPEG方法身份/拒绝期望不变，最终都须真实执行到拒绝后的保护断言 | NOT_RUN；原3 FAIL保留 |
| P02 | 两类旧封面SHA/元数据不变，项目事件/订阅付款与状态、类别完整事实不变 | NOT_RUN；历史已到达部分见原报告，不代替拒绝后补测 |
| P03 | 无PreparedImage/新元数据/正式缺损文件/活动lease；已知stage清理、unknown保守保留 | NOT_RUN；原失败后部分断言未到达 |
| P04 | 并发、重复提交、stale基线、共享双引用、取消回传及两SIGKILL窗口 | NOT_RUN；原测试不删不改 |
| P05 | 最终同输入完整JVM/Android、Lint/assemble、APK/ABI/权限及新native资源回归 | NOT_RUN；禁止拿R0预检冒充M10-T03/T04通过 |

最终状态仍**BLOCKED**。方案A最多为后续隔离PoC的CONDITIONAL_GO；资源预算/异常/ABI未证明前生产NO_GO，M10仍PENDING、M11 NOT_AUTHORIZED。
