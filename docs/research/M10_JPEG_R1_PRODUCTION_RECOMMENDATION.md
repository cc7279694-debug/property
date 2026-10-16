# M10-R1 — 最小生产修复建议与准入门槛

日期：2026-10-16。**PROPOSED / NOT_APPROVED / NOT_IMPLEMENTED**。R1隔离实验整体停止于 **BLOCKED — M10-R1 isolated native PoC**；M10仍PENDING，M11 NOT_AUTHORIZED。

## 建议

固定libjpeg-turbo3.2.0已在Android真实拒绝锁定缺损；A2作为下一轮资源方案审查的首选，建议**CONDITIONAL_GO仅限继续研究/设计审查**。正式App接入仍**NO_GO**，不能以锁样本四调用通过绕过下列资源/生命周期门槛。A1保留对照，不建议当前同进程实现：其整文件复制与取消缺口更大。

继续沿用R0[最小提案](M10_JPEG_R0_PRODUCTION_PROPOSAL.md)接点，不创建新存储owner或另一个图片格式：

```text
系统单图选择器
→ 既有ContentResolver／受控私有source.bin（原64MiB）
→ 原容器、格式与40MP检查
→ 仅JPEG：严格预检，warning/fatal/未完成/资源异常均不提交
→ 释放严格decoder和缓冲后，既有规范化／EXIF处理
→ 原1600px、2MiB输出及SHA校验
→ 原PreparedImage与文件落实
→ 原完整基线CAS／Room引用事务
```

PNG路径、Q05首次规范化/恢复原字节、现有图片清理和共享引用协议不改变。不得把A1缩略结果用作新的正式封面编码，不新增范围外格式或产品功能。

## 必须先解决的门槛

1. **完整资源预算**：以所有允许输入/采样和32位容量为基础，逐项覆盖库分配、progressive系数、条带、source、stdio、栈、JNI及后续Bitmap生命周期。当前40MP444约232～259MiB RSS，MAXMEMORY不是总堆cap。完成可执行的限制和失败清理；不得偷偷降低40MP或把最终2MiB文件大小当内存限制。
2. **CPU/取消**：解释6.141秒检查空档、首次120秒晚取消UNKNOWN；验证工作量边界和外部Job→native停止→FD/租约释放，而不仅内部deadline。后续同输入成功保留为补充，不能关闭原UNKNOWN。
3. **异常隔离选择**：受控allocator/上游patch、spill、独立Android进程均会扩大维护/协议范围。是否使用须ChatGPT审查并由用户另行批准；本轮没有采用。不能catch SIGSEGV后继续同进程、把crash当INVALID_IMAGE或自动回退宽松成功。
4. **兼容性和供应链**：固定四ABI、真实API26/arm64/32位和16KiB页设备加载，JNI异常边界、最终APK动态库/打包对齐/体积、许可证声明与维护责任。当前只证明四ABI编译及API36 x86_64 CLI/shared。SIMD配置改变需要独立验证，不推算本次时间为生产性能。
5. **范围边界**：严格解码拒绝特定语法/压缩损坏，不是原内容哈希或防篡改。整scan删除、一些entropy变化和尾填充仍能合法解码；对产品不可承诺“任何缺损必拒”。EXIF语义继续归既有规范化路径。

## 错误和JNI接入设计边界

损坏与decoder warning继续映射现有明确`INVALID_IMAGE`；输入/像素/资源超限和加载/环境故障应保留其真实含义，在实施前核对现有错误类型，不擅自新加枚举或把故障谎称普通图片损坏。任一未完成、pending JNI异常、未知结果、初始化/加载失败均fail-closed，不能产生PreparedImage。

A2为纯C统一cleanup；不得让longjmp跨C++析构帧或JVM边界。JNI只在native清理后返回结构化结果；pending exception不继续调用不安全JNI。当前没有生产JNI，所以本报告没有宣称这些已实现/验证。不得为赶进度直接把研究CLI包含main的.so塞进App。

## 之后生产修复必须实跑的保护测试（本轮全部NOT_RUN）

- 原三项JPEG拒绝方法/断言不变，真实ContentResolver→LocalImageStore必须走到`INVALID_IMAGE`后续断言。
- 两类旧封面的文件SHA、原字节、Room元数据/引用不变；完整项目金额事件、订阅付款/状态、类别事实集合不变。
- 不生成已提交缺损资源、正式新增文件或资源元数据；失败活动lease和已知stage按原owner协议释放。native仍读文件时不能先清stage；未知残留继续保守保留。
- 原事务/CAS/幂等/并发、共享双引用、取消及文件落实/DB前后两处SIGKILL继续真实验证，不删除失败测试或放宽断言。
- 同最终生产输入执行完整JVM/Android回归、Lint/Debug构建与最终APK权限/ABI/签名检查；已有FAIL、历史证据和平台NOT_RUN完整保留。研究成功不能登记M10-T03/T04通过。

## 尚需批准的生产范围

本轮仅已批准**研究用**NDK28.2/CMake3.22.1/Ninja及两工具许可范围。新增正式App native依赖、NDK/CMake Gradle接线、JNI、许可证资源、allocator/patch或独立进程/IPC/Manifest，均未授权。先由ChatGPT独立技术审查此R1证据、明确资源方案及最小实现契约，再由用户授权生产修复；不会自动开展。

原M10/UI-C1/main无操作，无APK发布或私人数据上传。原三项FAIL不改，UI-C1冻结NOTES保留，M11维持NOT_AUTHORIZED。
