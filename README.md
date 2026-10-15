# 砺石 LITHIC

纯本地 Android 长期成本记录 App，观察具体项目的陪伴时间、真实投入和日均成本。

M00～M09已独立验收冻结：ACCEPTED / FROZEN — PASS WITH NOTES。当前 **M10 — PENDING，JPEG补充验证确认缺损输入被错误接受，尚未修复**。新增5项API36测试为2 PASS /3 FAIL；实际ImageDecoder也未拒绝该输入，按用户补充指令第5节停在新增解码能力的方案决策点。见[补充报告](docs/evidence/M10-JPEG-supplement-verification.md)及[当前状态](docs/CURRENT_STATE.md)。[原M10报告](docs/evidence/M10-verification.md)的652项与设备证据保留历史效力，不代表新输入完整回归通过。M11及后续未授权。

## 工程入口与本地构建

工程为单 `:app` 原生模块，业务页面由 MainActivity 承载。正式 applicationId 为 `com.cooper.lithic`，Debug 为 `com.cooper.lithic.debug`；保留临时工程图标。M00～M09已独立冻结，M10交付待独立验收。Debug 合并清单另含exported非launcher空ComponentActivity测试宿主；Release产物仍NOT_RUN。完整备份恢复尚未交付，仅使用合成或可丢弃数据。

使用本机 JDK 17 和 Android SDK，按[工具链锁定记录](docs/TOOLCHAIN_LOCK.md)配置环境。SDK 路径写入未跟踪的 `local.properties`；不要提交本机路径配置、签名密钥、缓存或 APK。根目录构建入口：

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

Debug APK 输出在 `app/build/outputs/apk/debug/`。构建下载依赖与 App 运行离线是两个不同环节；安装及设备侧断网冷启动仍须留下实际证据。上述命令是复验入口，不能代替[验收登记](docs/ACCEPTANCE_MATRIX.md)里的实际结果。

## 文档入口

UI-C1当前只完成独立UI分支的阶段0[设计规格](docs/design/UI_C1_SPEC.md)与[实施计划](docs/planning/UI_C1_PLAN.md)，待用户书面确认；未修改产品代码、未生成UI APK/运行截图。M10 JPEG仍PENDING，正式验收登记和原分支不改。实际阶段0核查见[UI证据](docs/evidence/UI-C1-verification.md)。

| 阅读内容 | 入口 |
|---|---|
| 项目是什么、明确不做什么 | [PROJECT.md](PROJECT.md) |
| Codex 工作规则与恢复顺序 | [AGENTS.md](AGENTS.md) |
| M08 历史契约、验证与全部文件 | [契约](docs/planning/M08_CONTRACT.md) / [报告](docs/evidence/M08-verification.md) / [JSON](docs/evidence/M08-verification.json) / [文件](docs/evidence/M08-files.md) / [checkpoint](docs/checkpoints/2026-10-13-M08.md) |
| M09 历史契约、验证与全部文件 | [契约](docs/planning/M09_CONTRACT.md) / [报告](docs/evidence/M09-verification.md) / [JSON](docs/evidence/M09-verification.json) / [文件](docs/evidence/M09-files.md) / [checkpoint](docs/checkpoints/2026-10-13-M09.md) |
| M10 当前契约、验证与全部文件 | [契约](docs/planning/M10_CONTRACT.md) / [报告](docs/evidence/M10-verification.md) / [JSON](docs/evidence/M10-verification.json) / [文件](docs/evidence/M10-files.md) / [checkpoint](docs/checkpoints/2026-10-13-M10.md) |
| M10 JPEG 补充验证与待决修复 | [补充报告](docs/evidence/M10-JPEG-supplement-verification.md) / [证据](docs/evidence/M10-JPEG-supplement-verification.json) / [checkpoint](docs/checkpoints/2026-10-14-M10-JPEG-supplement.md) |
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
| 历史 M07 实施契约 | [M07_CONTRACT.md](docs/planning/M07_CONTRACT.md) |
| M07 历史测试、截图与全部文件 | [M07报告](docs/evidence/M07-verification.md) / [JSON](docs/evidence/M07-verification.json) / [checkpoint](docs/checkpoints/2026-10-12-M07.md) |
| 历史 M06 实施契约 | [M06_CONTRACT.md](docs/planning/M06_CONTRACT.md) |
| M06 历史测试、截图与全部文件 | [M06报告](docs/evidence/M06-verification.md) / [JSON](docs/evidence/M06-verification.json) / [checkpoint](docs/checkpoints/2026-10-12-M06.md) |
| M05 历史测试、截图与全部文件 | [M05报告](docs/evidence/M05-verification.md) / [JSON](docs/evidence/M05-verification.json) / [checkpoint](docs/checkpoints/2026-10-12-M05.md) |
| 实际工具链版本与核查来源 | [TOOLCHAIN_LOCK.md](docs/TOOLCHAIN_LOCK.md) |
| 规格问题与风险 | [SPEC_ISSUES.md](docs/SPEC_ISSUES.md) |
| 原件与当前文档差异 | [BASELINE_COMPARISON.md](docs/planning/BASELINE_COMPARISON.md) |
| Q02/Q07 已确认解释、其他建议与待执行测试 | [CLARIFICATIONS.md](docs/planning/CLARIFICATIONS.md) |

原始[阅读版](LITHIC_V1_产品与开发规划.md)、[完整ZIP](docs/planning/original-v1/source.zip)和[解包原件](docs/planning/original-v1/lithic_v1_plan/README.md)均保留来源字节。24份计划、130条原案例和6份规格通过规划入口访问；原件中的AGENTS/状态不是当前执行指令或状态。Q02/Q07 在 M02 获得的用户确认解释继承当前决策，不改写原件；各轮实际结果仅写入当前验收登记。

M10完成交付后立即停止等待ChatGPT独立验收，M11尚未授权。原43份规划/24计划、130 NOT_RUN案例及16组完整跨模块边界、历次报告/XML/checkpoint及独立NOTES不可改写；本轮实际结果另登记。Q03/Q04/Q06期限保持；Q05为USER_CONFIRMED，M11/M12完整往返仍NOT_RUN。M09两项流程风险按原用户A/B确认及原M09实际验证关闭，保留其来源。完整备份恢复仍归M11/M12，Debug不用于不可丢失个人记录；不把模拟器/已保存任务恢复推广成真机、OEM或真实LMK验证。
