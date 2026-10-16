# UI-C1 Task7 — 全部既有表单和弹窗一致化

Task6独立提交e9bb44f后执行；本地Codex审查与实际验证不代表ChatGPT独立接受。范围为8个真实复用表单实现、原类别/日期/删除/离开/待核对弹窗以及共享输入展示。新增FormHeader只接收原受保护onBack；原始输入、日期、四个付款日期、零元场景、所有错误/确认/重试/启用条件保持。没有数据库、领域、Repository、VM或媒体协议变更。

## 实际测试与历史风险

原保存测试诊断1 PASS，保留10秒与原断言；新增观测改变时序，不关闭偶发超时根因。实测保存目标底1920超过系统导航安全1857，新边界测试RED。仅测试宿主补safeDrawing，与生产Scaffold一致，保留合成表单本体和所有旧断言。详见[原超时专项](old-save-diagnostic.md)。

第一次5项RED中3项为测试分类UUID资料错误，不能当视觉RED；另两项为新标题和保存安全区真实失败。修正合成资料后，四项表单视觉测试全部以缺少新标题契约真实RED。对应输入ZIP/XML/日志均保留。

最终定向运行结果见本目录task7-green-summary.json；包含新视觉契约、原UI组件、项目创建/编辑/生命周期/日期及订阅付款/状态展示与新证据宿主。这里只报告该运行，不代替Task8无过滤全量。M10三JPEG仍FAIL，旧偶发保存风险继续UNKNOWN。真机/API26/OEM/Release/完整TalkBack/真实LMK/断电及M11/M12仍NOT_RUN。

Final targeted run: 68 methods, 67 PASS, 1 FAIL, 0 skipped; actualexit 1, 789.138s.

Recheck: 7 PASS; exit0; 203.083s. Corrected only the new short-title mandatory-wrap expectation; long reenable title still must wrap and all original701 assertions remain. Added one genuinely empty Room producer with actual host/APK metadata. These are separate inputs/runs, not one full69-method run. Lint/assemble commandexit0; cached tasks reported as cached.
