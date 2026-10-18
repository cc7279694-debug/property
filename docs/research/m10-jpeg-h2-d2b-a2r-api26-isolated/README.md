# A2-R 研究证据入口

C1当前交付见[整改](../M10_JPEG_H2_D2B_A2R_C1_REMEDIATION.md)和[静态诊断](../M10_JPEG_H2_D2B_A2R_C1_SDK_STATIC_DIAGNOSIS.md)。本地许可/XML原件不再跟踪，原始字节、官方URL及Git blob见[来源索引](../m10-jpeg-h2-d2b-a2r-c1/LICENSE_SOURCE_INDEX.md)。**未提交脱敏XML，索引不能冒充官方原XML；旧远端提交仍可经Git历史访问正文。**

`verify_delivery.py`及其`delivery-verification.json`是当时提交前的历史验证，不适用于C1新HEAD；不要重跑，它会写历史结果且依赖已移出跟踪的本地原件。干净克隆不含这些原件，C1验证入口见[新证据索引](../m10-jpeg-h2-d2b-a2r-c1/README.md)。安装重试的追加授权未核实，预算答复不作为批准依据。

当前结论以[执行报告](../M10_JPEG_H2_D2B_A2R_EXECUTION.md)和[结果](../M10_JPEG_H2_D2B_A2R_RESULTS.md)为准：固定官方镜像已下载/解包，独立文件后置校验通过；SDK管理器异常退出仍FAIL。唯一AVD创建命令因SDK目录定位错误失败，已停止，无AVD/ADB/App运行。

重要证据优先关系：

- `evidence/license-capacity-gate/` 和 `record_gate.py` 是**已纠正的历史CRLF取证错误**，不是当前许可门禁。原件保留，禁止据此要求新许可接受。
- `evidence/license-correction-archive-metadata/` 与 `LicenseCanonical.java --xml` 是有效许可核验：直接解析原XML，正确24333…匹配既有接受记录。
- `evidence/image-install/`、`image-install-retry-1/`保留两次管理器FAIL，不由100%进度或后置校验覆盖。
- `evidence/installed-image-audit/`、`pre-avd-storage-gate/`分别验证全部原件尺寸/CRC/SHA、生成元数据/父路径/目标写入者。没有保留完整ZIP，不宣称独立实测整归档SHA。
- `evidence/create-20261018T025802.416783Z/`是AVD命令发出前的空用户目录误拦；`create-20261018T025903.791601Z/`才包含唯一AVD create命令及真实失败。
- `evidence/closing-host-snapshot/`记录保留的新空数据目录与create marker、缺失的.ini、0模拟器/ADB/安装/Host启动。

所有脚本只属于本轮研究宿主；没有修改APK或生产代码。`smoke_isolated.py`的start/observe未执行，**不要自动运行或重试create**。后续需ChatGPT独立审查，原现场和失败保持。
