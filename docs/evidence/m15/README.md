# M15 验证证据

[集中报告](../M15-verification.md) · [最终计数/身份](results.json) · [构建输入SHA](build-inputs.json)（实际工作区字节，Git换行规范化后的blob哈希可能不同）

- [最终构建](logs/final-build-v2.log)：133项定向JVM、Lint0错误9警告、Debug App/Test完成。并非全模块全量回归。
- [JUnit原始XML与唯一方法](jvm/results.json)：133 PASS，29新增＋104受影响回归。
- [Android本模块20项](android-core/summary.json)、[相关回归12项](android-regression/summary.json)。各类stdout为原始instrumentation，result.json解析真实status0、JUnit OK、runner结束码与命令退出码；不冒充设备JUnit XML。截图status2仅发布路径，不计测试PASS。
- RED：[排名14/14](logs/ranking-red-test.log)、[Controller6/6与Nav4/5](logs/controller-navigation-red.log)、[订阅REVIEW来源2/4](logs/subscription-origin-red.log)；相邻XML保留。SDK偏好环境失败、green-build.log参数位置错误、final-build.log新测试导入编译失败保留，不算行为RED或产品测试执行。
- [原设备核对](device-preflight/summary.json)、[当前安装/证书/Manifest](install/artifacts.json)、[结束核查](device-end/summary.json)。仅5672原实例，App/Test保留数据同签更新各一次，未启动MainActivity默认账本，未操作其他设备。Android控制台空行和时间戳尾零被规范化比较，原始输出保留。
- reproduction是已有薄记录脚本的取证副本，原执行位置build/m15；不作为新测试框架或自动重跑授权。实际命令见各claim/result及install/commands.json。

## 必要实际截图

全部为**隔离ComponentActivity中的真实LithicApp/Nav3、真实Room合成数据**，不是MainActivity或正式用户账本；原始PNG未拼接/重绘。360dp画布；07/08使用LocalDensity fontScale2，未修改系统字号/密度。不重复归档组件测试的5张或入口重复图，设备原截图均保留。[原始路径/尺寸/设备与本地SHA](screenshots/index.json)。

| 图 | 内容 |
|---|---|
| [01](screenshots/01-project-mixed-companionship.png) | 仍持有8年、真实0日均的项目参与榜单 |
| [02](screenshots/02-project-negative-final-cost.png) | 已结束、负成本与截至今天结果标签 |
| [03](screenshots/03-subscription-standard-time.png) | 年付3650、折算365天、历史10元/天 |
| [05](screenshots/05-project-history.png) | 真实已结束档案，跨日后持有天数不增长 |
| [06](screenshots/06-stopped-subscription-history.png) | 仅STOPPED档案，暂停/到期/未来停止不混入 |
| [07](screenshots/07-review-360dp-font200-header.png) | 200%画布的返回、类型选择及折算解释 |
| [08](screenshots/08-review-360dp-font200-subscription.png) | 200%滚动后暂停订阅仍显示历史10元/天 |

01～03为2026-10-21合成业务观察日；05～08为可变BusinessClock跨日后的10-22，设备系统日期未修改。真实午夜/系统大字/完整IME与TalkBack/物理设备/Release均NOT_RUN。
