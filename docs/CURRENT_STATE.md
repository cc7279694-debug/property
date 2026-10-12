# 当前项目状态

状态日期：2026-10-12。唯一实时状态；归档同名文件与 checkpoints 是历史。

## Current Stage

- 当前 **M05 — ACCEPTED / FROZEN — PASS WITH NOTES**。来源为用户在当前会话明确转交的ChatGPT独立代码验收正式结论，仓库 `cc7279694-debug/property`、分支 `feat/m05-ui-foundation`、评审HEAD `6909858cd0c03ec8e3277908e05d407d7f4cafca`；回写前已实际确认本地/远端一致且工作区干净。依据和全部NOTES见[正式验收登记](ACCEPTANCE_MATRIX.md#m05-独立代码验收与正式冻结记录2026-10-12)。原READY_FOR_REVIEW/PENDING交付证据保留为历史，本次仅回写CURRENT_STATE与ACCEPTANCE_MATRIX并提交/推送，不修改业务代码或重新验证产品。M06～M23未授权，回写后立即停止。
- M00～M04均 **ACCEPTED / FROZEN — PASS WITH NOTES**，来源是用户转交的ChatGPT独立结论。评审HEAD依次为 `59ba290cba69e1e77af7d0376a8b43aaabbf5f88`、`ca8499e3d3cb2e5bac3ab29a231f399636899c19`、`ff1a898eaf4da5068c36af3e1c0bdcbe61c0b1f7`、`8b13d20dad89f9c3e76a3d19161cf2232b36b94d`、`1b7058ebb9dfbe904252a08efe6815d9952971ad`；独立依据、NOTES与各轮交付历史见[验收登记](ACCEPTANCE_MATRIX.md)。M04正式冻结已于 `aecbe86fce6e5c4375f2c35d2df8539df0a557bb` 回写提交，不改写历史报告/checkpoint。

## Verified / Completed

以下保留M05此前实际执行与交付证据。本次独立审查确认M05-T01～T04、200个唯一方法与PASS记录对应、3项独立补测及实际截图、真实加载/读取/失败门控和29新增/14修改/0删除的范围；**独立审查未在用户Windows本机重新运行Gradle或重新安装APK**。本次Codex仅状态回写，同样不运行Gradle、构建或设备验证，不能据此新增测试执行PASS；原报告/JSON/checkpoint中的PENDING是当时交付状态。

- MainActivity承载「现在｜回看｜设置」，只增加一个长期项目只读二级列表。Home与Review区分真正空库及真实非空快照，不伪造金额/图表/订阅读取；首页汇总明确尚未开放。设置显示持久记录时区和Debug/备份未交付提示。
- 既有Navigation3原地演进；四个@Serializable NavKey仅含路线身份，合法栈有界，重复Tab不压栈。主Tab与二级原生返回到Home，Home原生返回退出；旧Skeleton及已解码非法栈安全归一。真实按键、实际边缘手势、Activity重建、旋转与后台进程死亡恢复均有证据。
- LithicTheme系统浅深/38-24-16-14sp/六级间距；AmountText使用BigDecimal，null/0/负数/日均微值区分；EntryCard调用方状态/时间/封面与真实点击；FormFields受控标签/错误/IME/日期回调。未来结束日期只显示“已记录结束日期”，不提前宣称已结束。不修改领域日期规则或成本公式。
- AppContainer仍唯一持久数据owner；UI ViewModel观察同一个storageState，经既有Repository与Settings在IO读取，快照与时区均成功才Ready。Initializing无空账本/底栏；初始化失败或实际数据库/设置读取失败显示保护性错误，无假零、空库兜底、原始路径或无效重试；真实退出由Activity.finish实现。
- 完整 `:app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug --max-workers=2` 退出0：**82 JVM＋118 Android＝200个唯一测试全PASS，0失败/错误/跳过**，与源码方法逐项一致。原74 JVM＋86 Android实际回归，新增8金额展示＋32 UI。首轮完整GREEN3m27s实际执行JVM与Android；专用AVD软件导航配置后完整3m54s再次实际执行118 Android，JVM等任务up-to-date且源码/原结果一致。
- 实际宽360px/density160＝360dp、font_scale2.0、IME可显示的补测另重跑3个既有UI方法，51s/3PASS，不计成新增方法。错误可读，长文本/金额无视觉溢出，保存/返回真实回调可达；合成表单仅androidTest，不写业务库或进入生产菜单。9份实际查看的空库/合成截图随[M05报告](evidence/M05-verification.md)交付。
- RED：金额8项/7失败，首轮UI31项/30行为失败；未来结束事实标签1项实际失败后修正。初期编译/runner错误、SystemUI ANR及按键驱动超时单独记录，不冒充行为RED；最终Espresso真按键同步不改变生产导航规则。未删失败断言或通过清空数据绕过问题。
- Lint **0 errors / 20 warnings**：17版本提示、3旧M01占位字符串UnusedResources，不升级/抑制/清理受保护输入。必要Compose测试1.9.5、Lifecycle2.10.0、Espresso3.7.0精确声明；主依赖121→122，仅Debug空测试manifest新增，无旧运行坐标提升或移除。工具链仍冻结，见[工具链锁](TOOLCHAIN_LOCK.md)。
- 最终APK `E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`，11,837,748字节，SHA-256 `c70b89fd82b9480bbad8d1e0f06805317f6d6c002637583f5299b0fb769b790e`；com.cooper.lithic.debug、code3/0.1.0-m05-debug。最终APK/合并Manifest/备份九域排除/Wrapper、18生产类与v2签名检查PASS，无新增INTERNET，测试框架/合成表单未打包。Debug另含exported非launcher空ComponentActivity测试宿主，不能声称仍只一个合并Activity；Release仅配置排除，未实际构建。
- 仅砺石专用API36 emulator-5596：最终APK安装、断网COLD启动、明暗/三栏/二级/旋转/按键/实际边缘手势PASS。HOME后旧PID4568经应用UID SIGKILL真实消失，新PID5103从原任务恢复Settings；这是真实进程死亡模拟，不是Activity.recreate/force-stop，也不冒称真实LMK内存回收。另单独force-stop冷启动回Home。
- 页面/设备重启前后active generation身份、ready和设置SHA不变，安装APK与最终hash相同。原AVD实体导航键配置导致边缘handler未附着；只把本项目私有AVD的hw.mainKeys改no，保留userdata后重启并完成复验。屏幕、字号、IME、明暗、旋转、网络均恢复；软件导航配置保留用于后续测试。没有操作手机/其他项目AVD。
- Room schema仍v1四表，50项M04冻结输入哈希与原74/86测试源码不变；本轮70最终构建输入登记。原ZIP CRC/路径安全/43原件字节及哈希、原130唯一案例和16完整补测NOT_RUN、Q03～Q06期限及M09风险体保持。细节见[证据JSON](evidence/M05-verification.json)、[报告](evidence/M05-verification.md)、[checkpoint](checkpoints/2026-10-12-M05.md)。

## Pending / Current Problems / Technical Debt

M05已按用户转交的独立结论正式冻结；全部独立NOTES继续约束后续工作，不因验收关闭未执行能力：

1. M06必须实现业务写入后的真实数据刷新；当前只读快照不等于写后刷新或业务保存闭环，该流程仍未实施/NOT_RUN。
2. M06正式列表应落实分页或等效的大数据性能保护；当前只读列表不作为长期大数据性能验收，后续实现/验证仍NOT_RUN。
3. Debug测试宿主不得未经验证进入正式生产交付；Release实际构建尚未执行，仍NOT_RUN，不把配置排除视为Release产物已验证。
4. 真机、API26、OEM、完整TalkBack及真实低内存自动回收仍NOT_RUN，不因API36和SIGKILL模拟覆盖这些场景。
5. Q03～Q06、M09两项风险及M04、M08、M12后续安全边界保留；详见下列问题与原规格登记，不擅自关闭或标已实施。
6. 现有20条Lint警告不要求本轮强行升级依赖；本次不升级或抑制，原警告/执行证据保持。

- Q01 CLOSED，Q02/Q07 USER_CONFIRMED；Q03长期100/1000日节点M21前，Q04提醒资格M20前，Q05图片往返M10/M12前。Q06 **ENGINEERING_REQUIRED**，仅M04设计/初始数据集完成，M12导入/切换/回退/杀进程/共同校验/清理及Q06-V01～V03完整流程仍NOT_RUN。Q02 M17/M18、原130案例和16完整跨模块补测仍NOT_RUN，不因本轮UI测试提前通过。
- [M09两项流程风险](SPEC_ISSUES.md)保留：STOPPED重新启用入口/历史/纠错，多期预付默认停止选择；流程NOT_RUN、不改变产品语义。
- 真机、API26运行、OEM备份/迁移、TalkBack完整审查、真实内存压力自动回收、Release构建/正式签名升级仍NOT_RUN。M01独立审查未重新安装、M02/M03未重跑Windows Gradle、M04未重跑Windows Gradle或安装APK、M05未重跑Windows Gradle或重新安装APK等独立审查NOTES保留，不能由既有API36回归或本次状态回写改写。
- 初次初始化中断残留generation必须安全处理，不得静默清除真实数据；M08 v1→v2须同步既有版本预检。媒体M10、完整备份M11/恢复M12均未实施/NOT_RUN。本轮实现并验证M05初始化/实际读取错误保护，解决此项M04交接要求，但不增加恢复能力或整体关闭Q06。
- TypeScript/ESLint不适用原生Kotlin；NO-SOURCE不计PASS。不建议把未具备完整备份/恢复的Debug版本用于无法丢失的真实记录。

## Data / Architecture

schemaVersion=1，backupFormatVersion不存在；四表真实事实、Room读取事务、Money/BusinessClock与固定记录时区继承。pointer/ready内部formatVersion1不是备份版本。UI只读快照，不新建存储owner、云端、订阅表或派生成本。系统结构见[ARCHITECTURE](ARCHITECTURE.md)，不宣称SQLite/DataStore/文件跨存储原子性。

## Git

- 目录 `E:/CODEX/砺石新APP`；origin `https://github.com/cc7279694-debug/property.git`；当前 `feat/m05-ui-foundation`。M05实施来源为 `feat/m04-room-storage-foundation` / `aecbe86fce6e5c4375f2c35d2df8539df0a557bb`；原交付、本次评审与验收回写起点均为 `6909858cd0c03ec8e3277908e05d407d7f4cafca`，本次开始本地/远端一致且工作区干净。只更新两份状态文档并提交/推送当前M05分支；回写提交自身SHA、最终远端匹配及工作区核对以Git日志/最终报告为准，避免哈希自引用。
- 保护基准：main `145326ddd4ce28abbf7c2706fe25f9d38c599c3a`，M02 `0169c76bbf0946a0400b96d00124b3de9aea1cc0`，M03 `ffd066ffe0efb6c00f35526ae792b50829d3230e`，M04 `aecbe86fce6e5c4375f2c35d2df8539df0a557bb`。历史及归档不改，不合并main/force push/建PR/发布/修改WHOCARE，不提交APK、真实库、私钥或个人记录；截图仅已查看的空库或明确合成UI。

## Next Recommended Task

M05正式冻结状态回写后停止，等待用户明确授权下一模块。M06的M04/M05独立冻结前置已满足，但当前未获执行授权；后续必须实现真实写后刷新与分页或等效的大数据保护，继承基础组件、有界可恢复导航、真实加载/失败门控及既有Repository事务，不复制成本公式或引入另一数据owner。本轮不进入M06。
