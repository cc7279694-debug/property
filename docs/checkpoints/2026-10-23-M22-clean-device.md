# M22 干净设备停止点

- Source：bf5e8c4f27efc9f036254d25ea4c241de70afedf；当前M22分支不变。用户另行授权资源足够后新建一台隔离API36，仅补剩余三方法。
- Resource：创建准入5.64GiB可用内存/E盘95.48GiB；新根、新空userdata、新注册/端口，复用已有镜像，无下载/升级/许可接受。
- Actual：Lithic_M22_Clean_API36_20261023_1523 / emulator-5714，创建1/启动1/正常退出1。两轮真实窗口/全屏均SystemUI ANR；独立事件为SystemUIService waited20001ms，根因UNKNOWN。
- Tests：App/Test安装0、Instrumentation0，T06/T03/系统IME均NOT_RUN；未改或重编译源码。原137 JVM/36 Android PASS、T06三FAIL和39方法状态保持。
- Protection：原5712等旧设备ADB/写入0；新设备目标包/进程/Job为空，未开Owner，设置未改，未清数据/删除。最终新实例及端口结束，AVD保留。
- Blocking：**SYSTEMUI HEALTH GATE**，发生于砺石安装前。没有新产品或完整性能PASS；不得自行再次启动/换环境/修复系统。
- Evidence：[集中报告](../evidence/M22-Clean-Android-verification.md)、[结果](../evidence/m22-clean-20261023-1523/results.json)、[健康门禁](../evidence/m22-clean-20261023-1523/systemui-health-gate.json)、[退出](../evidence/m22-clean-20261023-1523/exit-proof.json)。
- Stop：提交推送当前M22分支后等待独立复验；M10/M13-T06/M20/Q03/Q04/Q02/G1/G2/G3及全部历史边界保持，不冻结、合并main或进入M23。
