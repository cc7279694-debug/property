# 当前项目状态

状态日期：2026-10-12。唯一实时状态；原件归档和 checkpoints 只记录历史。

## Current Stage

- **M07 — READY_FOR_REVIEW**，ChatGPT 独立验收 **PENDING**；实现者不授予 ACCEPTED / FROZEN。用户本轮授权结束及唯一关联收回、历史金额纠错、真实删除、基本历史档案，范围以 [M07 契约](planning/M07_CONTRACT.md)为准。最终自动化、最终 APK 手工闭环、数据完整性与临时设备设置恢复已通过；Git自身提交/推送SHA以最终Git日志与交付回执核对。
- **M00～M06 — ACCEPTED / FROZEN — PASS WITH NOTES**，依据均为用户转交的 ChatGPT 独立结论，完整评审 HEAD、确认项及全部 NOTES 保留在[验收登记](ACCEPTANCE_MATRIX.md)。M06 评审 HEAD 为 2d3358724c737ca47093f3d0b3dc8ba3caf0f156，冻结回写提交 fb0eafdc43efa6e13ebd76e3491fa4331a145abb 是 M07 起点；M06 旧报告、JSON/XML、截图和 checkpoint 中当时的 PENDING / NOT_RUN 不改写。
- M08～M23 未授权；M07 交付后停止，等待独立验收及用户下一次明确授权。

## Verified / Completed

- M07 新结束与可选实际收回同一 Room 事务；0 元合法、不预填估值。修正结束只更新已有唯一关联事件，无关联时不能偷偷新增或删除收回；未来结束日期不提前归档。
- 金额纠错只改金额、真实日期和说明，保留项目/事件 UUID、方向、角色与 operationId。真实尾款/退款仍新增 REGULAR 事件，结束后持有天数不增加；成本始终由冻结 ProjectCostEngine 重算。详情仅在观察日前存在真实结束后事件时标注包含结束后的结算，不把未来事件或空历史描述成已发生的事实。
- 删除确认比较完整项目及全部事件基线，事务删除与受影响行数核对，失败整体回滚；取消不写。共享及未引用媒体元数据/文件保留给 M10，不进行媒体清理。缺失删除目标只能表示已核对当前项目和关联事件不存在，不冒称永久操作日志或可证明是哪次操作删除。
- 单 ViewModel / SavedStateHandle 表单保留 raw、完整基线、固定 UUID 和 submitted；未知结果只重放原信封，冲突必须显式重读。显式确认目标已缺失后可安全返回且不会伪造已保存；活动表单状态单次完整发布，首次读恢复与迟到响应核对身份。
- 当前/历史列表由数据库分别筛选，50 条展示及 51 条哨兵查询，整个已加载窗口在同一协调器/Room 快照中刷新。首页数量仍是全部已录入项目 COUNT；Nav3 只保存 UUID/来源 enum，当前链最多 4 级、历史链最多 5 级，历史返回和主 Tab 来源正确，删除后返回来源列表且不保留旧详情。
- 完整四任务退出 0，7m 38s，**86 JVM＋273 Android＝359 个唯一方法 PASS，0 失败/错误/跳过；原 284＋新增 75**。7 份 JVM XML＋1 份 Android XML，源码方法/XML/结果登记精确对应；JVM 明确实跑，不把 up-to-date 或 NO-SOURCE 当新增执行。五个 M07 原命名案例仅在当前执行验收登记 PASS；不可变归档的 130 案例及 16 组完整补测保持 NOT_RUN。Lint **0 errors / 20 warnings**，不升级或抑制。
- 真实 RED、失败 XML/日志和诊断完整保留：缺失目标冲突死锁、活动表单空状态、结束后结算事实标注边界均先实际复现再修复。连续修正曾出现超时；诊断最终确认保存点击没有进入提交，保留原 15 秒与事实断言，关闭真实 IME、等待 idle、重滚动后触摸同一保存按钮修正测试驱动，不把缺失目标修复或状态发布修复冒称该超时根因。
- 最终 Debug APK：code 5 / 0.1.0-m07-debug，12,213,370 bytes，SHA-256 632a402c62bc827552b14f3613b3eb64828a2bd584f7c410a576c4e4220e765a，与实际安装指纹一致；v2 签名、合并权限及九域备份排除核对通过，无网络权限/测试框架或合成测试类打包，Debug 空测试宿主边界保留。
- 仅专用 API36 emulator-5596：最终 APK 全程断网冷启动、200% 字体/真实 IME、横竖屏、已保存任务后的 UID SIGKILL 恢复，创建→结束/关联收回→历史→连续修正→初始金额纠错→尾款 100/退款 800→负净投入 -50/持有 8 天/日均 -6.25→取消/确认删除→重启已通过；force-stop 冷重开保持负净额事实。取消前后 3 项目/8 事件全部字段相同，确认只删除本轮 M07 目标及 4 事件；重启历史 0、首页数量 2。最终 SQL 2 项目/4 事件与 M06 原记录全字段一致，integrity_check=ok、FK 无错误、schema=1；generation/ready/settings SHA 不变。20 张最终 APK 截图已归档；临时网络、屏幕、字体、IME、明暗、旋转及时区设置已恢复，未操作手机/其它 AVD。

完整证据见 [M07 报告](evidence/M07-verification.md)、[逐方法 JSON](evidence/M07-verification.json)与 [M07 checkpoint](checkpoints/2026-10-12-M07.md)；本地执行证据不替代 ChatGPT 独立验收。

## Pending / Current Problems / Technical Debt

- ChatGPT 独立验收 PENDING；最终 Git 回执待 root 收尾。M05 要求的真实写后刷新和正式分页已在 M06 实施并独立验收，保留历史 NOTES，不再列为未实现能力。
- Q01 CLOSED，Q02/Q07 USER_CONFIRMED。Q03 长期 100/1000 日节点在 M21 前，Q04 提醒资格在 M20 前，Q05 图片往返在 M10/M12 前；Q06 **ENGINEERING_REQUIRED**，M04 仅设计/首次数据集，M12 完整导入、切换、验证后回退、中断、共同校验、清理及 Q06-V01～V03 完整流程仍 NOT_RUN。Q02 M17/M18 视图集成仍 NOT_RUN。原 ZIP/43 原件、130 案例和 16 组完整补测不改。
- [M09 两项流程风险](SPEC_ISSUES.md)继续保留：STOPPED 后重新启用入口/历史/纠错、多期预付默认停止；当前未实施/NOT_RUN。M04 首次初始化中断的残留 generation 不得静默清除真实数据；M08 v1→v2 必须同步已有数据库版本预检；M12 完整恢复与故障回退待实施。
- **继承 M06 空间不足 NOTES：**真实 SQLite 满页失败可能被 Android/Room 回滚异常表现为普通 DATABASE。已验证旧数据、事务和同命令安全恢复；后续继续改进分类，但本轮不按消息猜错误、不宣称所有磁盘满都能准确分类。
- **继承分页 NOTES：**1000 条分页是正确性/查询范围验证，不是长期性能基准；单项目完整成本仍读取其必要历史事件，不能保证无限历史恒定耗时。
- **继承恢复 NOTES：**SavedState 不是持久业务草稿。SIGKILL/Recents 仅覆盖系统已经保存任务状态的实际模拟，不保证未保存状态、force-stop、断电或真正低内存自动回收后恢复。
- **继承数据保障 NOTES：**完整备份/恢复归 M11/M12，尚未完成；Debug 暂不用于不可丢失的长期个人记录。冻结或 Debug 验证不等于长期数据保障、完整备份或恢复已经通过。
- **继承平台/独立执行 NOTES：**真机、API26 运行、OEM 备份/迁移、完整 TalkBack、真实内存压力 LMK、Release 构建及正式签名升级仍 NOT_RUN；Debug 的非 launcher/exported 空 ComponentActivity 测试宿主不能未经 Release 验证正式交付。M01 独立未重新安装，M02/M03 独立未重跑本机 Gradle，M04/M05 独立未重跑 Gradle/安装等历史 NOTES 保留；M06 独立审查未在用户 Windows 本机重新执行 Gradle或重新安装 APK，不能把源码审阅登记成新执行 PASS。
- M08 订阅/迁移、M10 媒体、M11/M12 备份恢复、M13 汇总、M14 搜索/类别 CRUD 与后续集成均保持原边界/NOT_RUN。本轮未做替换、多段复用、图表/排行、资金事件删除、云端、生产样本或 WHOCARE 改动。TypeScript/ESLint 不适用；NO-SOURCE 不计 PASS。

## Data / Architecture

Room schemaVersion=1；未新增 Migration、依赖、数据 owner、派生成本真值或备份格式。pointer/ready 内部 formatVersion=1 不是 backupFormatVersion。UI → 现有 ViewModel → 同一 Repository/LocalWriteCoordinator → Room；Settings/DataStore、持久记录时区、generation 与原备份排除边界继承，不宣称 DB/DataStore/文件跨存储原子。实际结构见 [ARCHITECTURE](ARCHITECTURE.md)。

## Git

- 目录 E:/CODEX/砺石新APP，origin https://github.com/cc7279694-debug/property.git；当前 feat/m07-project-lifecycle，来源 feat/m06-project-entry，基线 fb0eafdc43efa6e13ebd76e3491fa4331a145abb。
- 只允许提交/推送 M07 功能分支；最终 commit、远端一致及工作区检查以本功能分支 Git 日志/最终交付回执核对，避免文档自引用 SHA。main 保护基线 145326ddd4ce28abbf7c2706fe25f9d38c599c3a；不合并 main、不 force、不自动 PR/发布，不提交 APK、数据库、私钥或真实个人数据。

## Next Recommended Task

本轮交付停在 **READY_FOR_REVIEW — M07 / 独立 PENDING**。不进入 M08；下一模块需要独立验收及用户新授权。
