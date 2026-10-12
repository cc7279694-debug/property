# 砺石 LITHIC

纯本地 Android 长期成本记录 App，观察具体项目的陪伴时间、真实投入和日均成本。

M00～M05 已独立验收冻结：ACCEPTED / FROZEN — PASS WITH NOTES；历史NOTES保留。当前 **M06 — READY_FOR_REVIEW**，ChatGPT独立验收PENDING。长期项目录入、资料编辑、详情、真实投入/收回、写后刷新与数据库分页已交付；82 JVM＋202 Android＝284个唯一方法通过，保留旧200方法。见[M06报告](docs/evidence/M06-verification.md)、[契约](docs/planning/M06_CONTRACT.md)及[当前状态](docs/CURRENT_STATE.md)。M07及后续未授权。

## 工程入口与本地构建

工程为单 `:app` 原生模块，业务页面由 MainActivity 承载。正式 applicationId 为 `com.cooper.lithic`，Debug 为 `com.cooper.lithic.debug`；保留临时工程图标。M05已冻结，M06独立验收PENDING。Debug 合并清单另含exported非launcher空ComponentActivity测试宿主；Release产物仍NOT_RUN。完整备份恢复尚未交付，仅使用合成或可丢弃数据。

使用本机 JDK 17 和 Android SDK，按[工具链锁定记录](docs/TOOLCHAIN_LOCK.md)配置环境。SDK 路径写入未跟踪的 `local.properties`；不要提交本机路径配置、签名密钥、缓存或 APK。根目录构建入口：

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
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
| M01 历史任务契约 | [M01_CONTRACT.md](docs/planning/M01_CONTRACT.md) |
| M02 历史任务契约 | [M02_CONTRACT.md](docs/planning/M02_CONTRACT.md) |
| M03 历史任务契约 | [M03_CONTRACT.md](docs/planning/M03_CONTRACT.md) |
| M04 历史任务契约与 Q06 初始设计 | [M04_CONTRACT.md](docs/planning/M04_CONTRACT.md) |
| M05 历史页面与视觉契约 | [M05_CONTRACT.md](docs/planning/M05_CONTRACT.md) |
| 当前 M06 实施契约 | [M06_CONTRACT.md](docs/planning/M06_CONTRACT.md) |
| M06 当前测试、截图与全部文件 | [M06报告](docs/evidence/M06-verification.md) / [JSON](docs/evidence/M06-verification.json) / [checkpoint](docs/checkpoints/2026-10-12-M06.md) |
| M05 历史测试、截图与全部文件 | [M05报告](docs/evidence/M05-verification.md) / [JSON](docs/evidence/M05-verification.json) / [checkpoint](docs/checkpoints/2026-10-12-M05.md) |
| 实际工具链版本与核查来源 | [TOOLCHAIN_LOCK.md](docs/TOOLCHAIN_LOCK.md) |
| 规格问题与风险 | [SPEC_ISSUES.md](docs/SPEC_ISSUES.md) |
| 原件与当前文档差异 | [BASELINE_COMPARISON.md](docs/planning/BASELINE_COMPARISON.md) |
| Q02/Q07 已确认解释、其他建议与待执行测试 | [CLARIFICATIONS.md](docs/planning/CLARIFICATIONS.md) |

原始[阅读版](LITHIC_V1_产品与开发规划.md)、[完整ZIP](docs/planning/original-v1/source.zip)和[解包原件](docs/planning/original-v1/lithic_v1_plan/README.md)均保留来源字节。24份计划、130条原案例和6份规格通过规划入口访问；原件中的AGENTS/状态不是当前执行指令或状态。Q02/Q07 在 M02 获得的用户确认解释继承当前决策，不改写原件；各轮实际结果仅写入当前验收登记。

M05已独立冻结；M06交付后停止等独立验收，M07尚未授权。原43份规划、130条案例及16组完整跨模块补测原件保持，原案例/完整补测仍 NOT_RUN；当前模块实际结果另行登记，原件不改PASS；M05原200项PASS及3项补测保留为历史，M06实际结果另行登记。完整备份格式和覆盖恢复仍归 M11/M12，Q06保持 ENGINEERING_REQUIRED，Q03～Q05期限及M09两项流程风险不变；不因初始底座冻结而关闭未来验证，不为演示新增虚构记录或无动作按钮。
