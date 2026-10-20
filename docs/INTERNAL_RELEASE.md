# M13 核心内部候选与真实录入闸门

当前交付仅为 **Debug / 合成验证候选**，等待独立验收。不是稳定签名发行版，不是首次不可丢失个人记录的准入声明。

| 字段 | 当前候选 |
|---|---|
| 源基线 / 功能分支 | `2e298ceb87a30d436a64180ccaf7d819e55ae005` / `codex/m13-home-core` |
| applicationId | `com.cooper.lithic.debug` |
| versionCode / versionName | `8` / `0.1.0-m13-debug` |
| Room / backup format | `2` / `1`，未改变 |
| App bytes / SHA-256 | `12,830,694` / `6b4925409db634a126dfc56cddd6fb345d0692856c6691e5eb6017491ec668da` |
| Debug证书SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9` |
| 本地产物 | `app/build/outputs/apk/debug/app-debug.apk`（忽略文件，不推送或公开发布） |

完整当前证据、最终测试包身份及截图见[M13报告](evidence/M13-verification.md)。Git交付SHA由最终提交对象和回执确定，不把源码起点当作交付SHA。

已实现的核心范围：项目录入/金额/生命周期、固定周期订阅/付款/状态、首页实时日均汇总、M11手动完整导出和M12确认覆盖恢复。M10仍延期，未合并图片导入/Worker/Stage V2/JNI。UI-C1独立分支的冻结结论保持，未混入本候选。

原M13-T06 / G1的稳定签名与首次真实录入部分 **NOT_RUN**。另行核对：稳定生产applicationId、独立长期签名密钥和用户保管、Release构建/权限/测试宿主排除、版本递增、旧记录覆盖升级后读取/修改/备份、首次真实录入许可。密钥不得进Git或数据备份。本轮未生成发行密钥、未构建Release、未公开APK或自动合并。

真机/API26/OEM、完整TalkBack、真实LMK/断电等继承NOT_RUN；M10三历史JPEG FAIL、Q05/Q06与其他NOTES不由首页候选关闭。后续只等待ChatGPT独立验收及用户决定，不自动开始M14。
