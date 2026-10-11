# 砺石 LITHIC｜纯本地 Android V1 规划包

版本：Planning Baseline 1.0 · 2026-10-10  
状态：**仅规划，未创建 Android 工程、未修改 WHOCARE、未编译 APK、未执行 Android 测试。**

这里的 LITHIC 延用前文名称作为工作名；`lithic-android` 是建议的新工程名，`com.cooper.lithic` 是建议的应用标识，均不是已经创建或发布的事实。

## 阅读顺序

先读 [开发总计划](docs/MASTER_PLAN.md)，再读 [产品规格](docs/specs/PRODUCT_SPEC.md) 和 [关键计算与状态契约](docs/specs/DOMAIN_CONTRACT.md)。

交给执行者时：读取 [AGENTS.md](AGENTS.md)、[项目状态](docs/CURRENT_STATE.md)，然后只执行用户明确授权的一个模块。不能把“开始规划”理解为“自动实施全部模块”。

## 文档地图

| 文档 | 负责什么 |
|---|---|
| PROJECT.md | 项目身份、边界与成功标准 |
| AGENTS.md | Codex 的执行和停止规则 |
| docs/MASTER_PLAN.md | 6 个阶段、24 个模块、依赖与交付门槛 |
| docs/specs/PRODUCT_SPEC.md | 用户已确定的功能与明确不做项 |
| docs/specs/DOMAIN_CONTRACT.md | 日期、金额、订阅、退款、结束后结算、历史重算 |
| docs/specs/ARCHITECTURE.md | Android 分层、存储、接口与数据库演进 |
| docs/specs/UI_SPEC.md | 页面、信息层级、交互与图表语义 |
| docs/specs/BACKUP_SPEC.md | 完整导出、校验、覆盖恢复、失败回退 |
| docs/specs/NOTIFICATION_SPEC.md | 续费与里程碑，权限、重试和去重 |
| docs/DECISIONS.md | 已确认决定与本轮补充默认规则 |
| docs/ACCEPTANCE_MATRIX.md | 跨模块验收案例与边界输入 |
| docs/CURRENT_STATE.md | 当前状态：所有实现模块未开始 |
| docs/SOURCES.md | Android 官方资料与使用范围 |
| docs/templates/MODULE_REPORT.md | 模块交付报告模板 |
| docs/templates/EXECUTE_ONE_MODULE.md | 单模块执行提示词模板 |
| docs/superpowers/plans/ | 每个模块的文件、任务、接口、测试和停止条件 |

## 三个不能混淆的东西

“当前日均成本”是本应用定义的混合参考指标，不是今日扣款、真实折旧或生活总开销。

“订阅折算付费时长”是已确认购买的周期数折算，不是已经实际享受服务的天数。

“完整恢复”覆盖当前数据，但必须先验证备份，不能先清空再尝试导入。
