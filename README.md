# 砺石 LITHIC

纯本地 Android 长期成本记录 App，观察具体项目的陪伴时间、真实投入和日均成本。

M00 已独立验收冻结：ACCEPTED / FROZEN — PASS WITH NOTES。当前执行 M01 原生 Android 工程骨架，仅建立 Kotlin + Compose + Navigation 3 空壳，不包含成本计算、业务数据库、项目或订阅功能。实际验证与待验收状态见[当前状态](docs/CURRENT_STATE.md)。

## 工程入口与本地构建

工程为单 `:app` 模块、单 Activity。正式 applicationId 为 `com.cooper.lithic`，Debug 为 `com.cooper.lithic.debug`；工程验证版使用临时图标，占位页只显示“砺石 LITHIC”“M01 工程验证版”“业务功能尚未接入”。

使用本机 JDK 17 和 Android SDK，按[工具链锁定记录](docs/TOOLCHAIN_LOCK.md)配置环境。SDK 路径写入未跟踪的 `local.properties`；不要提交本机路径配置、签名密钥、缓存或 APK。根目录构建入口：

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

Debug APK 输出在 `app/build/outputs/apk/debug/`。构建下载依赖与 App 运行离线是两个不同环节；安装及设备侧断网冷启动仍须留下实际证据。上述命令是复验入口，不能代替[验收登记](docs/ACCEPTANCE_MATRIX.md)里的实际结果。

## 文档入口

| 阅读内容 | 入口 |
|---|---|
| 项目是什么、明确不做什么 | [PROJECT.md](PROJECT.md) |
| Codex 工作规则与恢复顺序 | [AGENTS.md](AGENTS.md) |
| 当前进度与阻塞 | [CURRENT_STATE.md](docs/CURRENT_STATE.md) |
| M00～M23 顺序、边界与阶段门槛 | [ROADMAP.md](docs/ROADMAP.md) |
| 已继承的决定与变更记录 | [DECISIONS.md](docs/DECISIONS.md) |
| 真实架构状态 | [ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| 单模块执行与独立验收规范 | [EXECUTION_PROTOCOL.md](docs/EXECUTION_PROTOCOL.md) |
| 本轮验收登记及原矩阵缺口 | [ACCEPTANCE_MATRIX.md](docs/ACCEPTANCE_MATRIX.md) |
| 规划资料清单与原始文件校验值 | [规划入口](docs/planning/README.md) |
| M00 历史任务契约 | [M00_CONTRACT.md](docs/planning/M00_CONTRACT.md) |
| 当前 M01 任务契约 | [M01_CONTRACT.md](docs/planning/M01_CONTRACT.md) |
| 实际工具链版本与核查来源 | [TOOLCHAIN_LOCK.md](docs/TOOLCHAIN_LOCK.md) |
| 规格问题与风险 | [SPEC_ISSUES.md](docs/SPEC_ISSUES.md) |
| 原件与当前文档差异 | [BASELINE_COMPARISON.md](docs/planning/BASELINE_COMPARISON.md) |
| Q02～Q06 澄清建议与待执行测试 | [CLARIFICATIONS.md](docs/planning/CLARIFICATIONS.md) |

原始[阅读版](LITHIC_V1_产品与开发规划.md)、[完整ZIP](docs/planning/original-v1/source.zip)和[解包原件](docs/planning/original-v1/lithic_v1_plan/README.md)均保留来源字节。24份计划、130条原案例和6份规格通过规划入口访问；原件中的AGENTS/状态不是当前执行指令或状态。本轮不改产品规则，不把原案例改成PASS。

M01 交付后停止，等待独立验收；M02 尚未授权。
