# UI-C1 Stage2 最终文档一致性复核

日期：2026-10-16。只读复核正式报告、CURRENT_STATE、UI_C1_PLAN阶段2末尾、2026-10-16 checkpoint与已落盘证据。**未发现确定的统计矛盾、状态夸大或阻碍本次文档交付的问题。**这是本地文档与证据核对，不是ChatGPT独立验收，不授予ACCEPTED／FROZEN。

## 实际核对

- `docs/evidence/UI-C1-stage2-verification.md:8,24–29,55–66`与最终summary一致：冻结HEAD为`6a25965225ace3212d1653d15b45fc56ef87411e`；187项输入指纹为`6e10034a61e9ffc666be545645b04ea5a13bc022aa510cb5dde97694485a4f2f`；正式命令退出0／1／0，耗时10.446／1054.941／64.956秒。缓存状态没有被表述为全部任务重新执行。
- 独立重新解析8份归档JUnit XML：718个唯一`kind/class/name`，715 PASS／3 FAIL／0跳过；XML原字节SHA与summary逐份相同。相对实际701身份清单，原701为698 PASS／3 FAIL，新增17为17 PASS。报告、CURRENT_STATE第8行和checkpoint第5行没有把定向运行拼成全量绿色结果。
- 首轮718＝693 PASS／25 FAIL及22个冻结v1资源原字节前置失败的历史保留；恢复原schema字节、保护检出转换后重新完整执行的输入与首轮分开。当前报告没有把原fixture拒绝解释为22个已证实生产迁移缺陷，也未声称通过修改fixture期望解决问题。
- 独立读取68图manifest，重新核对全部68张PNG的SHA、字节数和IHDR尺寸，无不一致；全部图片绑定同一最终APK和最终输入指纹。分类为52张真实MainActivity、8张隔离真实Room、6张受控展示注入、2张空Room，即报告的52／14／2；其中16张必需主图恰为8页×Light／Dark，字体1.0。真实Main压力图中有25张同时登记360dp及系统字体2.0。全部API36，没有以宿主L2替代系统压力条件。
- manifest引用的运行summary及APK检查JSON，归档原字节SHA／大小重新核对一致。最终APK SHA为`4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46`；与manifest及device-preservation的安装APK字段一致。
- `device-preservation.json`明确7表计数2／4／7／2／4／6／5，合计30行；两份媒体及13项恢复设置与正式报告第88行、CURRENT_STATE第10行及checkpoint第6行相符。原记录精确不变是该设备核对的结论，本次没有重新读取设备或原始私有库来扩大证明范围。
- `runtime-observations.md`区分实际打开／取消、原生校验与隔离Room实际写入；明确没有原生手动成功写入每个模式。正式T08／T09／T11保留该限制，没有将只读或取消路径夸称全模式业务手工验收。

## 原生IME与旧风险

- 实际读三份`manual-observations.json`记录：UIA的`visibleImeUiNodes=[]`被保留；数字键盘InsetsSource为`visible=true`且顶部1155px，原输入法身份为Gboard。另直接查看字母键盘原图`final-native-ime-project-light.png`及数字键盘原图`final-native-ime-amount-dark.png`，可见实际软件按键，后者同时显示raw `1.234`和两位小数校验错误。正式报告没有凭UIA缺失节点或`mInputShown=true`独自推导IME结论。
- 原生submit截图本身未显示具体精度错误的边界，正式报告第111行及VISUAL_INSPECTION第7行已有明确说明；后续原错误XML／validation及Dark键盘图补足该具体观察，没有用文件名代替内容。
- 1023px旧字母键盘阈值误用于1155px数字键盘的驱动失败单列在`tooling-diagnostics.md:7`；没有把它写成应用保存故障或借机改产品／原10秒等待。
- `UIComponentsTest`旧合成保存偶发超时仍是`INTERMITTENT / ROOT_CAUSE_UNKNOWN`；其本次PASS、测试宿主safeDrawing修正和真实原生IME样本没有被写成历史根因已解决。

## 状态及停止边界

- 正式报告第3／10／130／140–144行、CURRENT_STATE第7–11／85行、计划Task8末尾及checkpoint第8–9行一致：实现者停在READY_FOR_REVIEW，用户／ChatGPT独立验收PENDING；M10仍PENDING，3个JPEG失败保留，整体FAIL。
- `FINAL_GALLERY.md:3`解释manifest中PENDING为生成器不执行人工视觉审核；另有实现者逐图记录。它没有被当作独立验收通过，和正式报告的“实现者已看图、独立PENDING”不构成矛盾。
- 计划旧T01～T14表格的NOT_RUN被末尾明确标为历史；当前结果单独登记在新报告，未改正式矩阵或将旧平台NOT_RUN整体提升。
- 真机／API26／OEM／Release／完整TalkBack／真实LMK／断电／M11–M12完整往返与未穷举原生组合仍NOT_RUN；未新增M10修复、M11或main合并授权。
- 最终证据提交SHA与远端一致性明确留给最后Git回执，没有把产品测试冻结HEAD递归写成后续文档提交。最终Git交付仍须由root实际完成和确认，本次未验证远端。
- 四份主要文档的Markdown本地文件目标均存在。

## 复核范围

本次只运行本地读取、JSON／XML解析、SHA／PNG头及链接核对，并查看上述两张IME原图；没有重新构建、运行测试、操作ADB／设备、修改正式文档／源码或操作Git。未执行完整独立视觉复审，未重新验证设备全部账本事实。唯一写入为本报告。
