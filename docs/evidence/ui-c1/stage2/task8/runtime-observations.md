# 最终真实设备操作与保护核对

仅已有Lithic_M01_API_36 / emulator-5596，API36；全程同一最终APK。没有重建/wipe/卸载清数据、改generation指针或宿主时钟。最终68图分52张真实MainActivity（16正式主图＋36附图）、14张隔离资料与2张空Room，逐图元数据见画廊登记。

## 实际原生操作

- 飞行模式1、Wi-Fi0，Active default network:none；实际force-stop/start冷启、浏览当前/历史、封面读取、打开和关闭新增菜单/表单，均正常。原QA只读；全量测试的实际写入/重放在各自隔离Room中完成，未向默认账本加入新记录。
- 系统密度480对应1080px/360dp，系统字体2.0。输入新项目草稿UIC1_DRAFT、原始金额1.234，显示真实Gboard字母及数字键盘。数字IME实际可见顶部1155px，Save文字962..1083px，滚动后在键盘仍显示时点击。随后原校验出现“金额最多保留两位小数。”，raw未被截断或重写。
- 系统Back先关闭IME，下一次显示原放弃确认；选择继续编辑保留输入。横屏实际UIA rotation1、回竖屏rotation0后1.234和错误仍在；系统切Dark也保留。随后再次Back、明确放弃该未保存草稿。原UIA摘录及三份观察JSON保留；没有将Gboard节点缺失视为键盘可见性证明。
- 360dp/200%下打开更多、删除确认并取消、结束表单并返回、封面管理并返回、投入表单并返回、订阅续费/停止表单并返回、首次订阅表单并返回。没有确认删除、结束、封面变更或真实付款。正常字号另开已结束M08项目的原金额纠错后返回，未保存。不能因此声称每个模式的成功写入都被手工执行；真实写入边界由全量原流程测试证明。
- 当前套餐/业务操作/历史及数据卡提供实际滚动补图。压力图保留长QA名称与返回标签换行的真实效果；没有假搜索、假筛选、未开放回看入口或伪开关。

## 实际保护结果

最终七表全部集合、每行每字段与开始完全相同：2类别/4项目/7金额/2媒体/4订阅/6续费/5状态，共30行；没有新默认QA记录。SQLite integrity_check=ok、foreign_key_check为空、Room schema2身份保持。两原封面5645/6005字节SHA完全相同；记录时区GMT、active-generation/ready/settings、AVD描述/config原字节不变。

13项原始系统设置逐项恢复完全相同，包含原null的show_ime_with_hard_keyboard已删除恢复null；字体1.0、密度420、主题Light、旋转/自动时间及网络恢复。最终已安装APK仍4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46。详见上级device-preservation.json和offline-runtime.json；原数据库/WAL/私有封面不入Git。

## 限制

测试宿主L2布局不等于原生IME。本轮已经补上上述原生IME样本，但没有逐一原生手动走完全部表单×状态×深浅×尺寸组合或多pending的200%恢复按钮组合；完整TalkBack、真机/API26/OEM/Release/真实LMK/断电/M11/M12仍NOT_RUN。历史保存偶发超时继续ROOT_CAUSE_UNKNOWN；M10JPEG三FAIL仍导致整体FAIL。
