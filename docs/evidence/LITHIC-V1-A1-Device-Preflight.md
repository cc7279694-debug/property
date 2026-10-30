# LITHIC V1-A1 小米14只读设备准入

2026-10-30；`codex/lithic-v1-product-completion`；实际基线`dcdc1e0d6354d86b1d40774673b94a41ac574322`，开始时工作区干净，本轮远端核对同基线。依据已审[Readiness](LITHIC-V1-Release-Readiness.md)及[候选报告](LITHIC-V1-A1-Candidate-Preparation.md)，仅执行一次限定只读观察，未取得安装或测试授权。

**V1-A1 DEVICE PREFLIGHT — BLOCKED / UNKNOWN**

目标身份、候选字节及部分健康/包记录事实已确认；**没有足够证据排除SystemUI ANR，且Preview12的User 0实际installed标志未采集**。不能出READ_ONLY_PASS或据此开始安装。这里是证据不足，不是已经发现手机ANR、产品故障或用户操作错误；会话已结束，不补跑、不自动扩大查询。

## 本次实时观察

|项目|事实/状态|
|---|---|
|已有ADB服务|现有`127.0.0.1:5037`监听，进程`adb.exe`/PID15728；只通过已有smart socket发送`host:version`及`host:devices-l`，均回复OKAY，版本`0029`。不调用ADB客户端，不启动/重启daemon。[主机记录](v1-a1-device-preflight-20261030/host-service.json)|
|唯一获授权目标|清单2项，**只有1项**匹配历史小米USB序列SHA-256 `e4f0e1d2361b0ef560ff8ac99a067fb8f540594c827d28b6cd21a720e724af92`，状态device；对另一项0定向查询。历史来源：[M22身份记录](m22-physical-20261023-083945/device-gate.json)。未把缺少usb:字段作为拒绝条件|
|当前手机属性|定向getprop确认Xiaomi / `23127PN0CC`，Android16/API36，主ABI及ABI列表均`arm64-v8a`；不是以旧记录代替本轮观测|
|Boot|`sys.boot_completed=1`；本次Boot标识去掉换行后的SHA-256 `280486436bfc94c1339b86be2f074a18b9fc1fb079cf8454b189dc60b6854c9a`，两次一致；未公开原Boot UUID，不要求与历史Boot相同|
|RAM/存储|MemTotal15,524,336KiB，MemAvailable4,796,964KiB（约**4.57GiB**）；`df -k /data`可用173,068,884KiB（约**165.05GiB**）。仅文件系统容量，无App目录枚举|
|电量/温度|level98/scale100，status2、health2；AC powered=true、USB/Wireless powered=false；temperature341（34.1°C）。原值如实保留，不以USB供电字段推断ADB连接失败|
|基本资源边界|超过本次读取器预设保守下限：可用RAM/存储各2GiB、电量20%、温度低于45°C；**这不是原T03或所有后续测试安全/性能保证**|
|SystemUI有限复核|按精确进程名两次pidof均为7427；仅一次15秒间隔复核，Boot同时一致。只确认名称查询返回PID稳定，**不证明没有ANR、触摸可用、主线程健康或系统未被遮挡**|
|ANR/系统阻断|**UNKNOWN**。没有执行全局Activity/窗口/任务dump、logcat、截图或触摸；仓库未有已确认安全且精确的SystemUI ANR限定查询，未猜测命令过滤能力，也未扩大范围寻找PASS|

完整脱敏查询、真实退出状态、原始输出长度/SHA及允许字段见[observation.json](v1-a1-device-preflight-20261030/observation.json)；[补充判定](v1-a1-device-preflight-20261030/review.json)不改写原观察。

## 三个精确包名：User 0

|包名|本次结果|限制|
|---|---|---|
|`com.cooper.lithic.v1acceptance.debug`|已完成包含卸载保留记录的`-u`查询，未发现该精确包记录|原始PackageManager退出标志0、shell-v2完整结束exit0；不是把超时/失败/任意空响应当不存在。仅描述此User0查询范围|
|`com.cooper.lithic.v1acceptance.debug.test`|同上，未发现该精确包记录|未安装，尚无本轮实际安装UID/私有目录/运行隔离证据；不以不同包名宣布隔离通过|
|`com.cooper.lithic.preview.debug`|**包记录存在**，appId/UID10429、code28、`0.1.0-preview.12-debug`|`-u`可能包含保留记录；**本轮没有证明User0 installed=true**。手机APK字节/签名与私有数据完整性未读取，不作PASS判断|

Preview的元数据过滤仅保留Packages段中的appId/versionCode/versionName，**准备时遗漏了User0 installed字段**。这是本轮读取器的证据缺口，不能归责手机或用户；不把包记录存在改写为已确认安装，不在会话后修改读取器并继续查询。现有三包进程/Owner/Job状态未查询，也未借此收口M20/M22/C6/C3。

静态包名层面V1 App/Test与Preview确实不同，无同名V1候选记录被发现；但完整安装准入仍UNKNOWN。下一次若获授权安装，实际UID、私有目录、进程及plain/正常Main分阶段安全仍须另外验证。

## 本地候选和历史APK保护

|稳定归档|字节数|本轮重新计算SHA-256|
|---|---:|---|
|`verification-candidates/v1-a1/Lithic-V1-A1-App-debug.apk`|15,977,970|`626c9809b7566d56583d49e4dbb43abe90d44e151a5e9357d2ca90c62f35a97a`|
|`verification-candidates/v1-a1/Lithic-V1-A1-AndroidTest.apk`|3,741,308|`1fdfc6ae8b784b8041d8182089bd1bf7af00ccc0e5d1f047de9ad9358f7e1154`|

两文件存在并与已审实物完全同字节，因此原Manifest/DEX/证书审计仍适用；**本轮未重新调用SDK验签或构建**。原证书SHA为`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，源码绑定仍`b9697b4afb0b1302bae2b403f6878e9c4af0d156`/树`73cf57a17808732e92e256c2517533bbfe1893f2`。

11个**本地**用户交付APK大小/SHA保持；本地Preview12仍15,977,934字节/SHA`4b6b3ed03674fc971b67a124171fedbb89fce155d2c6fd181ed597cde6250a1b`。C6 App临时APK仍缺失，未重建、未冒用旧SHA；C6 Test原字节保持。[本地保护结果](v1-a1-device-preflight-20261030/artifact-protection.json)。这些结论不能扩大成手机上已有APK或账本的完整性证明。

## 操作与停止边界

- 单会话18.828秒：已有服务主机协议2次OKAY；明确选择历史serial的只读shell18条，**全部取得exit0**，累计设备stdout/stderr717字节；一次有限复核、0自动重试。读取器预算为会话90秒、单条10秒、累计128KiB，原始序列仅在内存用于`host:transport:<serial>`，公开回执替换为脱敏选择器。主机清单原文及Boot UUID不落盘，保留长度/摘要；其他允许字段保留脱敏原始输出。
- [一次性claim](v1-a1-device-preflight-20261030/session-claim.json)防止重复执行；[实际读取器](v1-a1-device-preflight-20261030/observe-readonly.py)在本次观察后未修改。Host协议没有子进程退出码，以OKAY及完整回复判断；设备命令为真实shell-v2退出码，不伪造ADB CLI运行记录。
- 无安装/更新/卸载/清数据，无App/Service/Worker/Instrumentation启动或终止，无设置写入、ADB服务控制、root/reboot、截图/录屏、旧AVD操作、APK写入、私有账本/照片/备份/数据库读取；未触发权限弹窗。**本轮未修改系统设置，无本轮更改需要恢复。**
- 产品/测试/Gradle/Room5/backup4/Owner/generation/恢复/金额/订阅/媒体协议及APK均未改；本轮只提交本文和小型脱敏证据。全部业务、恢复、升级、Android/隔离运行验收仍**NOT_RUN**，历史FAIL/UNKNOWN不改判。

身份、字节及资源的已知部分可供独立审查，但当前**不具备出具完整READ_ONLY_PASS并进入安装的条件**。停止等待ChatGPT处理上述UNKNOWN；本报告不发起新诊断、安装授权或测试，也不把未知定性为手机/产品缺陷。

**V1_RELEASE_NO_GO / ANDROID_RUNTIME_ACCEPTANCE_NOT_RUN / ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED**
