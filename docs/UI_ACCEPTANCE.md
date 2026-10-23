# M22 UI体验检查

当前：**READY_FOR_REVIEW，设备完整可用性BLOCKED**。起点M21冻结登记`a367b278bb3186cf6a8f5afdcf7f843e688fbce2`；Room5/backup4与冻结业务规则保持。[集中报告](evidence/M22-verification.md)、[逐方法结果](evidence/m22/android/results.json)。

**重要边界：实际全屏帧和结束窗口均确认SystemUI ANR对话框。** 下面Compose PASS只表示控件/语义断言成功；`captureToImage`为设备上的控件层，不含遮挡它的系统窗口，不能据此宣称完整触摸/键盘/读屏体验已通过。[遮挡原帧](evidence/m22/screenshots/blocker-systemui-anr.png)。

| 页面/组件 | 问题与最小修正 | 实际验证 | 未证明部分 |
| --- | --- | --- | --- |
| 项目列表 | 标题/说明/新增及错误区移入原LazyColumn；不改变排序/分页 | 新组件方法PASS：360dp/LocalDensity2，标题可滚走，首项和返回/新增的宽高均≥48dp | 长历史列表尚未运行；所有真实触摸操作受SystemUI门禁限制 |
| 列表/详情失败 | 有旧内容时加“暂时无法刷新，以下为上次读取记录。”，不把失败伪装空数据 | 2项最终PASS；首轮完整句子匹配错误FAIL保留 | 真实数据库故障不在本轮UI覆盖 |
| 金额与明暗 | 保留原AmountFormatter、真实0、负数/亚分、完整金额和单位；不缩系统字 | 组件PASS及JVM回归；LocalDensity2明暗两图、极大合成组件值可换行 | 组件极大值不冒充合法单笔金额；对比度仪器/实际TalkBack未测 |
| 项目表单 | 保留滚动、imePadding、错误语义、输入保留、保存/返回守卫；不改业务 | 组件测试实际系统360dp/font_scale2.0单独PASS；LocalDensity2下错误与关键宽高/按钮PASS | IME观察为false、窗口无焦点，不能证明键盘弹出后的遮挡；原Espresso焦点FAIL和根因UNKNOWN保留 |
| 首页 | 不新增算法，空/真实0/亚分/无当前有效记录分开 | 原HomeOverviewUiTest6项本轮PASS；前台观察器和Activity重入配合注入日期1项PASS | 注入日期不是真实午夜；完整恢复旅程未完成 |
| 订阅状态/续费 | 保留暂停/过期/停止/未来预付、折算和真实已过去时长文案 | JVM报告参照回归PASS；离线旅程中实际走到续费/单项图，但整方法FAIL，不单拆成完整验收PASS | 本轮未重跑M09/订阅Screens完整套件 |
| 提醒设置 | 两偏好独立、权限拒绝不阻断业务；原整行toggleable | 组件方法PASS，48dp宽高和相邻不重叠，来源为合成回调 | 非系统发布/送达，不启动生产周期队列；不关闭M20历史平台门禁 |
| 备份恢复预览 | 保留先预览、明确覆盖与独立取消，入口可滚动 | 大字组件确认/取消方法PASS | 真实新完整UI旅程未到完成；受控文件不是SAF，不能称本轮恢复往返PASS |
| 单项/总趋势图 | 横拖只选自然日；前后日/文本/语义动作同边界；等待不显示错日金额 | 6项新图表、10项VM、5项M18语义PASS，JVM峰谷/20年参照/随机精确值回归PASS | 读屏服务手势未操作；遮罩下语义注入不是真实无遮挡触摸 |
| 长历史 | 一次完整快照内局部准备；不截断记录/历史，不做持久缓存 | 完整1000+1000/20000+10000/20年测试已编译 | **Android NOT_RUN**，没有实测规模/耗时/性能PASS |

截图：6张设备Compose控件层图的[尺寸/时间/来源/SHA](evidence/m22/screenshots/ui-index.json)。系统200%图不覆盖LocalDensity；其他图明确记录LocalDensity及当时实际系统font_scale。新测试均为plain Application/隔离ComponentActivity，无MainActivity默认账本；失败旅程全屏原帧另列，不混作正常截图。

## 资产与实际接入

| 资产 | 真实接入 | 状态 |
| --- | --- | --- |
| 名称/版本 | `app/src/main/res/values/strings.xml`、Debug字符串覆盖；Manifest label；code17/0.1.0-m22-debug | 最终Debug标签“砺石 LITHIC Debug”，不是正式发行身份 |
| 启动/圆形图标 | Manifest icon与roundIcon均`drawable/ic_launcher_placeholder.xml` | **工程临时图标**，无新获批Logo，未新画或冒称正式品牌接入 |
| 通知小图标 | `NotificationPublisher.kt`续费/里程碑均`setSmallIcon(R.drawable.ic_launcher_placeholder)` | 仍为临时资源；本轮没有新系统通知发布或送达验收 |
| 最终App | 实际安装SHA`328e3dc2fbe4c2a503fc81f2750cd68a62e5bf2b2bf624620b21d63b3f9bf213`；无INTERNET | 既有Debug证书，不代表M13-T06/M23发行门槛 |

## 保留边界

- 真实TalkBack已安装但未启用/操作；Compose语义不替代读屏朗读。真实IME、无遮挡完整UI旅程、长历史、真实午夜、真机/OEM/API26/Release均未取得本轮完整通过。
- 当前最后Test候选仅编译，详情就绪等待未设备复验；实际安装Test与候选SHA分别登记在集中报告，不能混称同包完整回归。
- 只使用5712一次启动会话，已正常结束。旧9个截图相关文件与15个锁保留；新14个零字节锁也保守保留，未确认锁状态。未来再次启动/清理须新授权。
- M21原395 JVM/31 Android不重跑；M20平台FAIL/收口UNKNOWN、M10延期/三JPEG历史FAIL、M13-T06延期、Q03/Q04/Q02完整跨模块及G1/G2/G3历史状态保持。
- 本表是候选验证记录，不自行冻结M22或授权M23。

## 定向补验：SystemUI健康准入未通过

来源1130349后的另一次授权会话，只启动原5712一次。第一轮全屏为Launcher启动画面，第二轮焦点及全屏明确SystemUI ANR；没有安装Test或运行任何方法。真实系统200%/IME新测试已编译但NOT_RUN，未改系统字体/密度/网络；旧T06三次FAIL、T03 NOT_RUN及上表限制保持。旧控件图不能证明无系统遮挡。见[真实全屏/窗口及补验](evidence/M22-Android-supplement.md)。设备已正常关闭、原29锁及旧截图保留，不增加可用性PASS或自行授权下一次启动。

## 干净环境续验仍阻塞

另获条件授权后，新建的5714空API36在砺石安装前已出现SystemUI ANR，两轮窗口/全屏和系统事件吻合。没有新UI/IME/完整旅程或长历史PASS，所有旧限制保持。新建/启动/正常退出各1、安装/测试0，原5712不动；见[本轮报告](evidence/M22-Clean-Android-verification.md)。未经新授权不继续重试。
