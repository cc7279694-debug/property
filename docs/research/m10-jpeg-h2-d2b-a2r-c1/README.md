# M10-H2-D2B-A2-R-C1 — 证据索引

当前整改见[整改/授权审计](../M10_JPEG_H2_D2B_A2R_C1_REMEDIATION.md)、[SDK静态诊断](../M10_JPEG_H2_D2B_A2R_C1_SDK_STATIC_DIAGNOSIS.md)及[checkpoint](../../checkpoints/2026-10-18-M10-JPEG-H2-D2B-A2R-C1.md)。来源锁定`4ee27bfcbe5903d6a49d4a92203de8a63686ba66`，继续原A2-R研究分支。

| 证据 | 内容与边界 |
|---|---|
| [许可来源索引](LICENSE_SOURCE_INDEX.md)、[原始元数据](evidence/license-inventory.json) | 7原件字节/SHA/blob/官方URL，索引不是原XML；正文仍在本地及旧Git历史 |
| [移出跟踪操作](evidence/tracking-removal.json) | `git rm --cached`真实结果及本地7原件哈希保持；无物理删除 |
| [只读前快照](evidence/readonly-before/result.json) | 研究SDK/marker及目录元数据816项，Process/User/Machine路径变量，原smoke脚本SHA |
| [额外继承变量](evidence/environment-inheritance.json) | 当前PREFS_ROOT与Java选项是否存在、完整性元数据；非历史create环境证据 |
| [提交前远端基准](evidence/git-baseline.json) | 真实退出0，A2-R锁定来源及main/M10/UI-C1受保护远端值 |
| [本地工具静态读取](evidence/tool-static/commands.json)、[跟进](evidence/tool-static-followup/commands.json) | 8次JDK javap静态读取，输入JAR SHA及每次stdout/stderr；不执行SDK目标类 |
| [只读结束比较](evidence/readonly-after/result.json) | 环境、研究目录元数据、所选SDK/marker与原脚本SHA前后比较 |
| [本轮交付验证](evidence/validation.json) | 当前索引正文扫描、跟踪范围、链接、原证据保护及敏感内容检查；不重写历史验证 |
| [未应用环境提案](environment-change.PROPOSAL.diff.txt) | 仅供独立审查；未进入原监督器 |

实际静态结果：11项检查PASS，源2388文件、取证时暂存2421文件扫描7→0正文匹配，173个文档相对链接无缺失。验证JSON自身不在该时点的内容扫描中；加入结果和总结后再做最终提交检查。只读保护比较816→816元数据，聚合、所选文件、环境及原启动脚本均相同。

辅助脚本：`inventory_licenses.py`只生成非覆盖的原件哈希索引；`snapshot_readonly.py`只读研究目录和限定环境字段；`disassemble_static.py`只调用JDK静态阅读器；`validate_c1.py`只核对本次交付，不调用SDK或设备。取证入口使用排他输出，不应为重复运行而删除旧证据。

没有重跑历史`verify_delivery.py`，其85文件/128链接之类结果只属于当时提交前证据，不能借来宣称新HEAD跟踪正文。旧结果提到的本地原件路径由来源索引解释；干净克隆没有这些正文，不能自动重跑依赖原件的历史收集器。

本轮Android/SDK命令0、下载0、APK构建0。AVD创建复试、Host/READY、Worker与原API36/H2/私人数据验证全部NOT_RUN。C1完成静态交付后仍须ChatGPT独立审查及下一次创建的独立授权。
