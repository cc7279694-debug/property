# M01 实际验证证据

日期：2026-10-11；验证对象是当前M01构建产物，不是规划预期。独立验收PENDING，原130案例未改。工作区开始干净，基线77e61b；APK在本次工程变更尚未提交时构建，提交后已逐blob按[构建输入哈希](M01-build-inputs.json)核对一致；对应代码提交为`e33a9aa0d9cf0e756e6e4447ae3a221baa3981c2`，已实际推送。此证据随后仅补登记Git结果，不改变工程输入或APK。

## 构建与检查命令

| 检查 | 实际结果 | 本机日志（不提交构建产物） |
|---|---|---|
| `./gradlew.bat --version` | 退出0，Gradle8.14.3，Temurin17.0.20.1+1 | `build/m01-evidence/gradle-version.log` |
| `./gradlew.bat :app:assembleDebug :app:lintDebug` | 最终退出0，BUILD SUCCESSFUL in 3m 34s；48任务全部执行 | `build/m01-evidence/build-and-lint.log` |
| `./gradlew.bat :app:lintDebug --rerun-tasks` | 退出0，BUILD SUCCESSFUL in 31s；28任务重新执行（修正旧缓存时；最终字体修正由上行重新构建/lint验证） | `build/m01-evidence/lint-fresh.log` |
| lint XML/文本结果 | 0 errors、12 warnings，均为较新版本/target提示 | `app/build/reports/lint-results-debug.xml`、`app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt` |
| `./gradlew.bat :app:dependencies --configuration debugRuntimeClasspath` | 退出0；UI/runtime1.9.5、Material3 1.4.0、AndroidX Core1.16.0、Startup1.1.1 | `build/m01-evidence/runtime-dependencies.log` |
| Python最终APK检查脚本 | 退出0，APK权限/身份/编译备份规则/合并Manifest/版本/Wrapper检查通过 | `build/m01-evidence/apk-verification.json`、`apk-badging.txt`、`apk-manifest.txt`、`apk-backup.txt`、`apk-extraction.txt` |
| SDK `apksigner verify --verbose --print-certs` | 退出0，Verifies，APK v2签名true，1签名者Android Debug/RSA2048 | `build/m01-evidence/apk-signature.txt` |

APK检查脚本入口：[verify_m01_apk.py](../../scripts/verify_m01_apk.py)，使用本机 SDK Build Tools36.0.0/aapt2，Python3.12。合并Manifest路径：`app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml`。正式applicationId/namespace由app构建配置核对，没有正式发行APK。

## 最终 APK

- 绝对路径：`E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`。
- 文件名：app-debug.apk；大小：10,448,037字节。
- SHA-256：`a34cf1d90006eaeb13a7188c8cc2f89b911938ca1fb1286659d8b66487f2b57e`。
- applicationId：`com.cooper.lithic.debug`；versionCode：1；versionName：`0.1.0-m01-debug`；名称：砺石 LITHIC Debug。
- 签名证书DN：`C=US, O=Android, CN=Android Debug`；证书SHA-256：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。仅记录公开证书信息，没有提交密钥。
- 模拟器已安装base.apk的SHA-256与最终本地APK相同；实际核对记录在 `build/m01-evidence/installed-apk-check.json`。
- 没有业务schemaVersion/backupFormatVersion；未建立业务数据或完整备份格式。APK、本机配置、签名密钥、日志、AVD及截图均不进入Git或公开发布。

## 最终权限与备份资源

唯一 uses-permission：`com.cooper.lithic.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`；最终APK protectionLevel=`0x00000002`，合并Manifest为signature，来自AndroidX Core1.16.0，用于内部接收器保护。

INTERNET、SCHEDULE_EXACT_ALARM、USE_EXACT_ALARM、READ_EXTERNAL_STORAGE、WRITE_EXTERNAL_STORAGE、MANAGE_EXTERNAL_STORAGE、POST_NOTIFICATIONS、CAMERA及照片访问权限均无。ProfileInstallReceiver 上的 `android:permission="android.permission.DUMP"`限制调用方，不是App请求DUMP。

最终APK `allowBackup=false`；fullBackupContent与dataExtractionRules资源引用存在，实际解读编译后的两个XML：旧规则、cloud-backup、device-transfer各9项exclude/path="."，域为root/file/database/sharedpref/external/device_root/device_file/device_database/device_sharedpref，没有include。配置检查通过；没有真实业务数据或各OEM设备迁移/备份行为实测，不能宣称所有厂商均已阻止自动迁移。

## M01-T01 设备侧证据

- 类型：本模块专用模拟器 `Lithic_M01_API_36`，serial=`emulator-5580`；Android16/API36，Google APIs x86_64 revision7，WHPX加速，Emulator37.1.11.0。没有把模拟器写成用户真机。
- 设备指纹：`google/sdk_gphone64_x86_64/emu64xa:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys`。
- 安装：`adb -s emulator-5580 install -r <最终APK>` → Performing Streamed Install / Success。
- 原网络：airplane=0、wifi=1、mobile_data=1；测试网络：airplane=1、wifi=0、mobile_data=0，dumpsys connectivity 显示 Active default network: none。
- `am force-stop`后pidof退出1且无PID；随后 `am start -W -n com.cooper.lithic.debug/com.cooper.lithic.app.MainActivity` → Status: ok / LaunchState: COLD / TotalTime:1801ms / WaitTime:1803ms。
- UI dump和实际截图均核对三个指定文本；没有虚构项目、订阅、金额或业务按钮。
- 发送系统返回键后再次force-stop/restart：Status: ok / LaunchState: COLD / TotalTime:2471ms / WaitTime:2482ms；本模块专用设备的crash buffer中没有该包/Activity崩溃。
- finally恢复网络为airplane=0、wifi=1、mobile_data=1，实读一致。
- 证据：`build/m01-evidence/device-verification.json`、`device-commands.json`、`device-ui.xml`、`device-connectivity-offline.txt`、`device-crash-buffer.txt`。
- 截图绝对路径：`E:/CODEX/砺石新APP/build/m01-evidence/offline-cold-start.png`；SHA-256：`c2c7a1ffbe6c1fb66c00de8f3d5155ba22b8ab7febfe84658dab1526609f6f89`。原图已实际查看。

这些时间只是此机此模拟器的单次记录，不是性能承诺。真实手机、API26运行、旋转/大字体/完整UI矩阵没有执行；M01无需未来业务测试。

## 修复与未执行边界

1. 首次构建退出1：AGP阻止Windows非ASCII工作目录；只增加项目级android.overridePathCheck=true。原日志 `build-initial-path-check-failure.log`。
2. assemble成功但首次lint退出1：本机local.properties驱动器冒号未转义；修正为C\:/...，保留忽略规则。原日志 `build-lint-local-properties-failure.log`。
3. 标准重跑仍复用旧lintAnalyzeDebug报告；确认实际文件已修正与UP-TO-DATE证据后，用--rerun-tasks重验lint，再执行标准组合成功，没有抑制规则或baseline。旧日志 `build-lint-stale-cache-failure.log`。
4. 初次APK检查脚本使用旧aapt字段名sdkVersion，改为BuildTools36输出的minSdkVersion及完整XML命名空间后重跑通过；初次设备检查将pidof“进程不存在”的退出1误判为异常，改为明确校验退出1/空PID后重新完整执行通过。两者都是验证辅助工具解析问题，不是业务测试/TDD红灯；初次设备检查也已恢复网络。

构建警告保留：路径选项experimental、初次SDK XML版本解析提示、Debug libandroidx.graphics.path.so未剥离符号；最终lint12项仅为更新提示。未执行业务Unit/Integration/Instrumentation、真实手机、API26/OEM备份行为；完整备份往返不适用且未实现。原130案例及16组补测仍NOT_RUN，Q02～Q07按原期限继承。

5. 收尾按实际Emoji2 1.4.0源码及[官方说明](https://developer.android.com/develop/ui/views/text-and-emoji/emoji2)确认自动Emoji字体初始化会请求可下载字体；仅移除EmojiCompatInitializer，保留其他Startup组件。首次覆盖后lint因显式Manifest类仅在传递runtime classpath报MissingClass；将既有Startup1.1.1显式声明解决compile可见性，未升级其运行版本。再次标准构建/lint退出0，最终APK/权限/签名及设备全重验；此文档哈希已更新为字体修正后的最终包。
6. 新包设备复验时系统Pixel Launcher ANR窗口遮挡UI，导致文本核对未通过；已保存 `device-launcher-anr-check.json`、`device-launcher-anr-ui.xml` 和截图。实际crash buffer无App崩溃且MainActivity仍前台，网络已恢复。关闭本模块专用模拟器的系统窗口后重新完整安装/断网/COLD/截图/返回/重启并恢复网络，结果PASS；未屏蔽App异常或修改业务代码。
