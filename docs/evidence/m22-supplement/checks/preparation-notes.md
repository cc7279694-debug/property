# 本轮准备阶段异常口径

这些不是Android测试FAIL；全部发生于静态准备或只读查询，不能据此编造设备测试结果。

- `commands/initial-app-certificate`：JAVA_HOME缺失，exit1；使用已锁定JDK的进程环境后，`commands/jdk-app-certificate`和`jdk-test-original-certificate`实际exit0，未安装JDK。
- `commands/initial-app-manifest`：aapt对Windows绝对中文路径报告Illegal byte sequence，exit1；改用同一文件相对路径的`relative-app-manifest`/`relative-test-original-manifest`实际exit0，未复制或替换APK。
- `commands/test-package-build`：实际Gradle exit0和完整stdout/stderr已先落盘；随后Python控制台将含替换字符的摘要输出到GBK时抛出UnicodeEncodeError。构建本身不改判失败，亦未为此重跑。以原command.json、BUILD SUCCESSFUL及APK/DEX为准。
- `pre-start-host`：只读CIM/端口命令实际exit0，摘要解析把编码名写成`utf8-sig`导致LookupError；以原stdout按`utf-8-sig`重新读取，记录`pre-start-host.json`，没有执行设备命令。
- 首次以`powershell.exe -File build/m22-supplement/start-once.ps1`调用，被本机脚本文件执行策略拒绝；本轮工具返回原文为“cannot be loaded because running scripts is disabled on this system”。该错误发生在脚本正文、start claim和Start-Process之前；随后的`started-host.json`进程/端口仍为空。后以当前PowerShell执行同一脚本内容，未修改Windows策略，才生成唯一`start-claim.json`及launcher30120。本段为本轮工具返回的说明性转录，**不是伪造的原始命令日志**。
- `commands/boot-00`：已启动本次AVD后，首次定向只读ADB查询设备尚不可用exit1；后续有界boot查询成功，原非零记录保留，不重复启动AVD。
- `commands/initial-installed`：宿主插值造成远端`for`语法不完整，exit1；只读命令没有取得有效身份。未以该结果准入或安装。SystemUI阻塞后仅用`end-installed-identities`取得结束保护所需的实际包字节/UID。
- 首次退出核验时原launcher/QEMU和端口已消失，仍观察到官方延迟收尾helper；因此没有立即生成PASS退出证明。保留首次观察，待其自然消失后由`final-exit-host.json`支持最终退出证明，没有重复退出、终止helper或清理目录。
