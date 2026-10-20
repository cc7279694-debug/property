# M11-B 最小整改与真实导出验收

2026-10-20 · **READY_FOR_REVIEW — M11-B**。来源`3d018e9aedb89052983e6c54af40d03c47ecc9fb`；分支`codex/m11b-final-export-validation`。本报告只陈述执行证据，不自行冻结正式M11。

## 产品结果

**已发生一次真实Android完整导出。**全新隔离API36上，从实际MainActivity设置页点击“导出完整备份”，系统DocumentsUI选择Downloads及新文件名，生产服务完成ContentResolver写入、关闭、重新读取同长度/SHA后显示“完整备份已导出并通过校验”。独立读取实际文件，ZIP CRC、8个条目、manifest的7个payload长度/SHA全部一致。

归档`m11b-synthetic-0a21510d-ab12-4dda-a226-70848d48865f.lithicbackup`：**3691字节**，SHA-256 `761efcabd55718d0ea6f9bec56f9e4ec490da6766b3dbca0c82a4fba96be5818`。七表均有合成记录（类别2、其余业务表各1、媒体5），设置GMT；JPEG与RGBA8、索引1-bit、灰阶16-bit、Adam7 PNG全部原字节备份，含3份未被封面引用的登记资源。源文件身份/字节、项目12345分、订阅付款18000分及状态事实均不变。备份实物只保留在本worktree的`build/m11b-final/`和新设备合成目录，不提交二进制备份或APK。

[设备验证记录](m11b-final/android/export-proof.stdout) · [独立归档核验](m11b-final/archive-independent-verification.json) · [实际设置成功截图](m11b-final/settings-saf-success.png) · [全部结果及源输入SHA](m11b-final/results.json)

## 最小代码变化

- `BackupJpegInspection`新增已登记JPEG容器/尺寸检查，支持8-bit SOF0/1/2；`BackupPngInspection`覆盖15种合法颜色/色深和Adam7，CRC、zlib、行/通道边界继续严格验证。备份不新增导入、不重新编码，路径/UID/dev/inode/nlink、登记长度/SHA、复制前后身份及系统受限可读性均保留。缺失、变化、不匹配或不支持格式拒绝整份导出。
- **JPEG登记字节完整性与M10严格熵流验证分开。**Bitmap可读性不是严格解码证明；本轮不接M10 native/Worker/Stage V2，不擅自将原三项JPEG FAIL改为PASS。未知或登记前已经存在的、宽松解码可容忍的熵流缺损不由本备份功能认证为新导入安全。
- 真实Android首次导出失败暴露缓存根问题：实际应用进程将`/data/user/0`识别为系统链接。仅对可信Context的数据/cache根核对真实目录、UID和精确父子归属后canonical化，随后仍执行原严格祖先检查；媒体源与payload门禁没有放宽。[同进程原始RED](m11b-final/android/path-diagnostic.stdout)
- 原系统保存器测试误匹配Downloads标题；仅修正可见可点击根定位，并增加绑定旧claim SHA/generation、校验原合成七表及5图片的续接入口。旧FAIL和claim保留，没有重种数据或修改原M10断言。

Room v2、schema/DAO、金额/订阅引擎、backup format v1、Gradle/依赖/Manifest、UI-C1均未改。生产仅4个备份文件变化；另3个Android和2个JVM测试文件新增/修改。

## 实际验证

| 验证 | 真实结果 |
|---|---|
| JVM定向新增与回归 | **149 PASS /0失败/错误/跳过**，18 XML、原143＋新增6；PNG单方法循环90输入不算90方法。[XML](m11b-final/jvm/TEST-com.cooper.lithic.data.backup.BackupPngCompatibilityTest.xml) |
| 构建/Lint | 最终生产四任务exit0，2m50s；测试驱动修正后的Test构建/Lint exit0，2m23s；**0错误/7警告**，未升级工具链。[生产日志](m11b-final/platform-fixed-build.log) · [Test日志](m11b-final/saf-resume-test-build.log) |
| 实际Android | 最终App下**16项PASS**：平台1、完整导出/故障/并发6、一致快照5、设置UI3、真实取消1；新Test包再执行**SAF续接1 PASS**。两Test身份分开登记，没有宣称17项同一Test包重跑。[原16项及原驱动失败](m11b-final/android/fixed-results.json) · [新SAF方法](m11b-final/android/saf-resume-result.json) |
| 失败保护 | 空媒体成功；缺失/损坏媒体整体失败；可控目标写入、关闭及错误读回拒绝成功，断言确认实际到达故障出口；实际系统保存器取消不显示成功。真实提供方磁盘满/权限撤回/断电**NOT_RUN** |

历史RED完整保留：初次6项Android中3失败，随后平台直通/同进程路径诊断失败；修复后PASS。原SAF方法在保存前定位失败，新续接7.854秒PASS，不回写旧FAIL。JVM首轮缺接口/测试编译依赖RED及PNG两个断言RED均保留。辅助只读`find -printf`命令exit1，改用元数据目录列表，无文件变更；启动早期4次ADB offline及console CRCRLF辅助解析失败不改成PASS。[全部ADB命令、退出码、字节哈希](m11b-final/android/commands.json) · [原启动命令结果](m11b-final/environment/boot-commands.json)

## 环境、产物和停止边界

唯一新设备`Lithic_M11B_Final_API36_20261020 / emulator-5686`，专属目录`E:/CODEX/.codex/research-env/lithic-m11b-final-20261020`；**创建1、启动1、App安装2、Test安装5**，后续安装均为真实故障定位和测试驱动修正后的同签名更新，未卸载、清数据、重启或重建。SDK官方偏好目录/handler先核实非null后创建exit0；旧AVD创建错误的底层异常仍UNKNOWN，旧claim/证据不动。[静态环境](m11b-final/environment/static-preflight.json) · [创建](m11b-final/environment/create-result.json) · [boot证明](m11b-final/environment/boot-proof.json)

当前App **12,620,652字节**，SHA `500cc5349012a20acad630ffa0c05d992f1dd466db20ad7004588f6f7c4dd92a`；当前Test **2,080,220字节**，SHA `2e6e87838ac726be42d4d4b92e5863c92b400ccb454ea15d60f920fa9f21c9c2`。原16项Test SHA `3f07c5533c8f040ed488d51f90554d3d03e724ef350835ad9cff2ac196e124ee`保留。原证书相同、min26/target36、无网络/新增权限及M10库；最终设备字节再次核对。[最终静态产物审计](m11b-final/apk-resume/apk-audit.json)

本轮限定目标无已知剩余Blocking，**等待ChatGPT独立验收**。API26/真机/OEM、Release、完整格式语义穷尽、提供方故障/断电持久性、M12恢复往返仍未验证；中断未知临时文件保守保留，不能承诺SAF阻塞调用即时取消。新API36保持在线，原失败claim及合成缓存未清理；H2/C2/M10和私人数据未访问。M10保持PENDING / DEFERRED、三JPEG历史FAIL、UI-C1冻结、全部历史NOTES保持；不进入M12、不合并main、不公开APK。
