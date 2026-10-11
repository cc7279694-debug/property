# M00 完整规划包补齐与重新交付

日期：2026-10-11。分类：M00文档交付，等待独立验收；不代表ACCEPTED/FROZEN。继承[首轮checkpoint](2026-10-11-M00.md)，不改写当时缺资料的历史。

## 本轮范围与来源

只补齐规划资料、核对现有文档和验收记录，提交/推送现有功能分支。没有进入M01，没有Android/UI/数据库/业务/云代码，也没有修改WHOCARE或合并main。

工作单所述项目内ZIP路径检查时不存在；使用用户实际附件E:/CODEX/LITHIC_V1_AEE6B4BCE591A7E5928C%85.zip。原ZIP 131,564字节，SHA-256为861cee1adb74cb575c67af8c14191fbb2792b66741c2e99760b9f339979812d7。原ZIP和43文件保留在docs/planning/original-v1，保持包内相对路径。

## 新增 / 修改 / 删除

新增：原ZIP及解包43文件（40 Markdown、3 JSON）、[逐文件来源清单](../planning/SOURCE_MANIFEST.json)、[基线对照](../planning/BASELINE_COMPARISON.md)、[澄清与补测](../planning/CLARIFICATIONS.md)、本checkpoint。

修改：.gitattributes、根AGENTS/PROJECT/README、docs/DECISIONS/EXECUTION_PROTOCOL/ROADMAP/SPEC_ISSUES/CURRENT_STATE/ACCEPTANCE_MATRIX、docs/planning/README/M00_CONTRACT。

删除：无。首轮checkpoint、原阅读版、原ZIP内容均保持。

## 继承规则与对照结果

产品目标、原生Android技术方向、事实/日期/金额算法、手动完整备份和单模块停步不变。六规格及原DECISIONS/SOURCES与阅读版规范化文本一致。精确依赖按24计划和module-index补准，尤其M05/M08/M10；串行编号与单模块授权仍是独立执行闸门。

旧AGENTS/PROJECT/CURRENT_STATE/DECISIONS只作为原包快照保留，不覆盖当前记忆。原PLAN_VALIDATION里的文档PASS只是制作包时的记录；130案例仍NOT_RUN，不继承为本轮产品验证。

## Q01～Q06 / 风险

- Q01 CLOSED：原件已补齐并归档，资料缺口解除。
- Q02 PROPOSED：观察日在结束前的分母分支，M02前明确，关联M17/M18。
- Q03 PARTIALLY_RESOLVED_BY_SOURCE：原M03-T02已有首日1天；长期100/1000日触发边界M21前确认。
- Q04 PROPOSED：赠送间隔提醒筛选边界，M20前明确。
- Q05 PROPOSED：初次图片规范化与恢复字节保留，M10/M12前明确。
- Q06 ENGINEERING_REQUIRED：前一完整代及恢复阶段跨进程持久记录，M04设计/M12故障验证。
- Q07 OPEN：原早期模块共用未来备份/真机模板要求，后续执行前明确适用性；本轮按用户文档范围处理。

Q02～Q06映射原案例并登记16组补测，全部NOT_RUN；没有新增产品功能或修改原130条测试。细节见[问题清单](../SPEC_ISSUES.md)。

## 实际验证

2026-10-11本轮实际检查：

- 原ZIP：131,564字节、43文件、解压240,094字节，CRC全通过，严格UTF-8/3 JSON解析通过；拒绝绝对/父级/非法Windows路径、同名大小写碰撞、符号链接、加密与超限条目，实际0不安全条目。
- 附件与归档ZIP哈希一致；43解包原件与ZIP逐字节一致，manifest大小/CRC/SHA-256逐条一致，0缺失/额外原件。
- 阅读版55,653字节/SHA-256未变；六规格与原DECISIONS/SOURCES经换行规范化后全文匹配阅读版。
- 24计划/index/ROADMAP顺序和直接依赖一致、无前向边/环；130唯一原案例在计划/Markdown矩阵/JSON逐条一致，全部NOT_RUN。
- 16补测组全部NOT_RUN；当前M00-T01/T02/T03分别实际核对范围、未实施状态和D01～D15默认身份，在当前验收表记PASS，未回写原件。
- 57份Markdown、146本地文件链接、0失效，代码围栏无失衡。第一次路线解析误含版本表行，限定主模块表后检查通过，不改来源数据。
- 敏感模式扫描0命中；人工范围/差异/职责审查通过。本地辅助文档审阅无必须修正项，不是独立验收。
- 暂存60项变更：12修改、48新增、0删除；只有本仓库文档/规划JSON/原ZIP/.gitattributes。ZIP/43解包文件/阅读版共45个原件工作blob与暂存blob一致。
- 新写文档git diff检查退出0；归档原件的Markdown硬换行保留，不对原件做格式改写。首轮checkpoint逐字节保持。
- Git远端预检：现功能分支仍d25e9f7，main仍145326d；本轮提交/推送另待实际完成后登记。

TypeScript/ESLint/Unit/Integration/Build/Android Build/APK Manual：NOT_RUN；无产品工程，不适用于M00。独立验收PENDING；本地辅助审查不替代独立验收。

## 数据层变化

SQLite/Migration/Repository/Device Storage/Backup/Android Config：未实现，无变更。JSON是原规划和来源元数据，ZIP是文档归档，不是App备份实现；没有安装新依赖。

## Git

Branch：feat/m00-planning-freeze。开始HEAD：d25e9f7987df0fdb40dfe7129cf1c4ca8b50da01，工作区干净。

本轮提交与推送PENDING，待实际完成后登记主体提交SHA/远端校验。main基线145326ddd4ce28abbf7c2706fe25f9d38c599c3a；无main合并/force push/PR/发布。

## 下一步应继承

重新提交M00独立验收，给出可引用的验收/冻结结论后，由用户另行授权M01。当前停止，不创建Android工程；恢复只认当前唯一CURRENT_STATE及Git事实，归档原件中的执行提示不授予权限。
