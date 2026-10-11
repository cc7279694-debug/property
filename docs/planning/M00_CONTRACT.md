# M00 — 新项目初始化与规划冻结：任务契约

来源：2026-10-11 当前用户 M00 工作单。此文件是本轮工作单落地，**不是尚未取得 ZIP 的原 M00 模块计划**。

## Goal

在新的 property 仓库接入已提供规划，建立后续会话可恢复的项目记忆，固定产品与工程边界，列出规格问题并交付文档供独立验收。

## Scope

- 文档入口、产品身份、协作规范、当前状态、决策规范、路线与问题清单。
- 原始阅读版保留和校验；记录配套原件是否实际可用。
- 从阅读版登记 M00～M23 的编号、顺序、交付门槛、版本演进计划。
- 检查文档一致性、链接、敏感信息、Git 差异；建立本地 Git、独立分支，按用户授权提交与推送文档。

## Out of Scope

WHOCARE 的任何改动；M01；Android 工程初始化；UI、数据库、Migration、Repository、功能代码；工具链安装；APK；服务器、Supabase、Vercel、云同步；main 合并、PR 自动合并或产品发布。

## Constraints

- 不改变已确认产品定义；原生 Kotlin/Compose/Room 路线保持。
- 原规划缺失不臆造；问题只能报告和建议，不能擅自改核心规则。
- 一个当前状态文件，沿用 HR-Codex Project Memory System 文档职责。
- 规划、执行验证、独立验收三者分开。M00 文档检查不证明产品功能通过。

## Dependencies

已提供阅读版、当前用户工作单与协作规范、可访问的新 GitHub 仓库。完整规划冻结还依赖未取得的规划 ZIP，见 [Q01](../SPEC_ISSUES.md)。

## Acceptance Criteria

以 [M00 验收登记](../ACCEPTANCE_MATRIX.md)为逐项结果来源：清晰入口、稳定身份、可恢复状态、顺序和停止边界、问题清单、原文完整性、文档质量、严格范围与 Git 证据。

完整规划原件接入和独立验收未满足前不能标记 ACCEPTED / FROZEN；文档交付可进入评审，须带阻塞清单。

## Verification

实际检查文件清单、原文 SHA-256、Markdown 本地链接、24 个模块唯一编号与顺序、边界与状态表述、敏感信息、git diff/status；提交后验证提交与远程分支 SHA。不存在工程，TypeScript、ESLint、单元测试、Android Build、APK 真机验证均为 NOT_RUN（不适用 M00）。

## Stop

本轮交付审阅后立即停止。下一步先补齐资料并独立验收 M00，再由用户明确授权 M01。
