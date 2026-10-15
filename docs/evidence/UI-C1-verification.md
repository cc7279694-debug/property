# UI-C1 阶段0 — 文档与来源核查

日期：2026-10-15（Asia/Shanghai）。状态：**DESIGN_READY_FOR_CONFIRMATION**；书面方案待用户确认，非UI实现或视觉验收通过。

本轮执行用户[v2契约](../design/ui-c1/source-v2/CODEX_PROMPT.txt)第十节“阶段0：只读检查并落地说明”，交付[设计规格](../design/UI_C1_SPEC.md)、[分阶段计划](../planning/UI_C1_PLAN.md)及[checkpoint](../checkpoints/2026-10-15-UI-C1-stage0.md)。正式ACCEPTANCE_MATRIX不改；UI-C1-T01～T14全部NOT_RUN。

## 实际来源与Git

- 原目录`E:/CODEX/砺石新APP`，M10本地/远端均`41596820145dc4a3fa6d041728a59540d828108a`，开工原工作区干净，origin为`https://github.com/cc7279694-debug/property.git`。M09冻结来源`57de897917473f255d43dfb306a8433b366c00eb`，main保护基准`145326ddd4ce28abbf7c2706fe25f9d38c599c3a`。
- 未发现已有UI目标分支或附着worktree。通过Codex原生worktree工具从精确SHA建立`E:/CODEX/.codex/worktrees/lithic-ui-c1/砺石新APP`，原生创建完成时detached，随后创建用户指定`feat/ui-c1-visual-redesign`。未reset/stash/rebase/cherry-pick/合并，原M10工作区不改。
- ZIP 4,456,671字节，实际4文件，CRC及安全路径/大小/manifest SHA核对。ZIP为旧版正文，无README与v2目录；单独v2 TXT是当前正文，旧TXT与ZIP正文一致。两张PNG实际打开，原字节保留在设计资料区，没有生成/裁切/改写或打包进生产资源。
- 源文件大小、SHA、图片尺寸及覆盖关系见[来源索引](../design/ui-c1/SOURCES.json)。已读项目规则/当前记忆/架构/工具链/正式矩阵/路线/M10契约/JPEG补充报告、原UI_SPEC/DOMAIN_CONTRACT及有关冻结决定，已核对实际Compose/Repository接口。

## 当前交付与实际检查

| 项目 | 本轮实际结果 |
|---|---|
| 设计映射 | 已形成；已选方向Confirmed，具体书面方案待确认；每个数字/操作可追溯到当前来源 |
| 原件完整性 | ZIP CRC/manifest字节与SHA核对，两图实际查看，v2独立来源登记；原件不可作当前AGENTS |
| Git/范围 | 仅文档/来源资产；应用/测试/schema/配置/依赖、正式矩阵和历史证据保持起点内容；完整结果见机器核查 |
| 候选对比度 | 12组精确前景/背景数学配对核查；全部满足对应4.5或3目标。非最终控件/像素/无障碍认证 |
| 文档链接/文件 | 检查当前新增文档相对目标和明确的既有路径；拟新增产品文件明确标注，不当作已存在 |
| 敏感信息/产物 | 检查新增文本的凭据模式和禁止产物；只归档用户设计参考，不提交账本/私人原照片/私钥/APK |
| UI产品代码/页面 | **未实施**；现在/列表/详情等所有运行效果尚未生成 |
| JVM/Android/Lint/assemble/安装 | **NOT_RUN**：当前仅阶段0文档，产品代码实施须先书面确认 |
| 实际UI截图/200%字体/IME/设备 | **NOT_RUN**；不把两张概念参考图标为App截图，不操作AVD或改变设置 |
| UI APK/版本/大小/SHA | **本轮未生成**，不以原M10 APK或旧SHA冒充UI产物 |
| UI-C1-T01～T14 | **全部NOT_RUN**；完整逐项计划在UI_C1_PLAN，后续分阶段登记 |

[机器核查结果](ui-c1/stage0-verification.json)实际核对118个本地文档目标、12组候选配色与657个源码方法身份，检查结果PASS只表示文档、原件和范围的一致性，不能代替产品测试。新worktree仅核对Git内容与来源一致；不把其本地换行等原字节差异假定为上次测试输入。两项本地只读辅助审阅已对照实际代码/冻结边界核查并修正含糊的“未来付款”文案；它们不代替ChatGPT独立评审或用户书面/视觉确认。

## M10已知失败与后续风险

来源[JPEG补充报告](M10-JPEG-supplement-verification.md)保持原字节：原652项仅旧147输入历史PASS；新增5方法真实2PASS/3FAIL，源码657、输入152，新完整回归NOT_RUN。不是654PASS/3FAIL全量。三项拒绝失败的实际XML保留，本轮没有重新运行，也未把诊断PASS算为拒绝PASS：

1. `com.cooper.lithic.data.media.M10JpegIntegrityTest#jpeg_entropy_truncated_with_eoi_is_rejected`
2. `com.cooper.lithic.data.media.M10JpegOldCoverProtectionTest#project_entropy_cut_jpeg_is_rejected_and_old_cover_and_money_are_preserved`
3. `com.cooper.lithic.data.media.M10JpegOldCoverProtectionTest#subscription_entropy_cut_jpeg_is_rejected_and_old_cover_payments_and_states_are_preserved`

拒绝之后的失败清理未到达，仍NOT_RUN。UI允许使用原媒体接口但不能证明JPEG完整性；缓存/并发/首页epoch均是待实现计划，无新能力声明。既有订阅全库分批预检不因预览3条而消失；该性能边界待后续测量。真机/API26/OEM/Release/完整TalkBack/真实LMK/断电和M11/M12完整往返继续NOT_RUN；Q05仍USER_CONFIRMED，Q03/Q04/Q06及原NOTES继承。

## 文件与停止点

- 新增：UI_C1_SPEC、UI_C1_PLAN、原4件ZIP内容与独立v2原文/来源索引、阶段0本报告/机器证据/checkpoint。
- 修改：AGENTS追加当前UI授权、CURRENT_STATE追加独立支线、README入口、`.gitattributes`原件字节保护。
- 删除0；无数据层/迁移/依赖/权限变化；正式矩阵和历史记录不改。
- 仅提交推送UI分支，实际SHA与远端一致性以最终Git回执为准，自身提交不递归引用。无main合并/M10推送/PR/公开APK。

**下一步：用户书面确认规格和计划，然后仅实施三页双主题样板。**门禁来自用户v2第十节；本轮停在DESIGN_READY_FOR_CONFIRMATION，不进入产品实现或M11。
