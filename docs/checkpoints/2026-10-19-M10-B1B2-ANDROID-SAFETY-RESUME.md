# M10 B1/B2 Android 安全续验 checkpoint

- 日期：2026-10-19；状态：**BLOCKED / WAITING_CHATGPT_REVIEW**。
- 来源：`532817ebaac5f59e0eb458c7d85d17765112606a`；分支：`codex/m10-b1b2-android-safety-resume`；worktree：`E:/CODEX/.codex/worktrees/lithic-b1b2-resume/砺石新APP`。
- [唯一集中报告](../research/M10_B1B2_ANDROID_SAFETY_RESUME.md)、[实际Android证据索引](../research/m10-b1b2-android-resume/evidence/android/INDEX.json)。历史532817e报告、method-00 UNKNOWN及全部旧证据不改。
- 实现：有界分块读取与原字节/元数据/退出码校验；短命Host的运行中直接身份与结束后进程分离；不可变新预检准入；严格条件正常退出/API36证明。三接缝55唯一宿主PASS。实际原两JSON读回及健康JPEG完整退休PASS；预检JUnit重跑0，健康JUnit1。
- 新阻塞：输入准备对不存在的`files`执行stat，exit1，UNKNOWN/files=[]。图片上传0、B2和B1异常全NOT_RUN；没有尝试第二次操作。随后只离线修复私有父目录安全准备，新5＋旧4宿主PASS，Android NOT_RUN；保留旧已消费claim，禁止复用/重试。
- 最终：同一 `Lithic_M10B1B2_API26_20261019 / emulator-5664` 保持运行；launcher40472/QEMU18424身份一致；UID10077无Host/Worker，唯一健康Stage终结及自有载荷清理成立；旧两预检记录未变、输入目录ABSENT。创建/启动/安装/Process.killProcess/退出/API36均0。H2/C2及其他旧现场未访问。
- 继承：App/Test SHA分别 `b7c90277fa6c27e6ab314aa52c293de8e445ba6a3deba9e984ab96f4280e8a4c` / `37ad9ec91964b6a0b8a870ec35032164ebcdb6a1c41522e780cb7af343fcd338`，不重建/替换。Android2MiB普通文件性能未验证，真实40MP资源尚无本批数据；流吞吐/宿主结果不冒充系统证据。
- 下一次只可在独立审阅后按新授权身份接续输入准备；不清旧claim，不跳过准入，不自行重建或退出设备。本checkpoint不是任何后续设备实验许可。
- M10 PENDING/三JPEG历史FAIL、UI-C1冻结、M11 NOT_AUTHORIZED、R3及全部历史NOTE/FAIL/UNKNOWN/NOT_RUN均保留；不授予ACCEPTED/FROZEN。
