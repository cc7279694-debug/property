你继续处理砺石 LITHIC 的 M10 独立评审补充验证。

本轮 ChatGPT 结论：PENDING。不是正式通过，不是重做 M10，也没有授权 M11。

一、来源与范围
仓库：cc7279694-debug/property
本地：E:/CODEX/砺石新APP
分支：feat/m10-local-cover-media
评审 HEAD：d7e9a92096b70d6e5976ebcc65dbdfda32b17296
M09 冻结来源：57de897917473f255d43dfb306a8433b366c00eb
先读取 AGENTS、CURRENT_STATE、M10 原计划、M10_CONTRACT 和本包 README。
确认当前 HEAD、工作区与远端。如果 HEAD 已变化，先核对变化，不 reset、不覆盖用户改动。
本轮仅允许围绕下面一个完整性疑点补充验证；若真实复现，再最小修复。
不得进入 M11、合并 main、操作 WHOCARE、破坏性迁移或清除既有数据。

二、问题及证据层级
AndroidImageCodec.jpeg() 的结构扫描会接受一份保留 SOI/SOF/SOS/EOI、但后段图像数据缺失的 JPEG。
当前宿主 JVM 已验证扫描器接受；宿主 Pillow/OpenCV 能解出缺损图片。
这还不是 Android LocalImageStore 或 App 的端到端复现。
现有 truncated_jpeg_is_rejected 只删最后两个 EOI 字节，覆盖不到此边界。
必须先复核，不得因评审提出疑点就假设必须修改生产代码。

三、锁定输入
使用附件中的原字节，不用替换为“去掉 EOI”的旧样本：
complete.jpg：59,334 bytes，SHA-256
2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907
entropy_cut_eoi_retained.jpg：20,194 bytes，SHA-256
f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a
两者尺寸均为 256×256；第二份移除 39,140 字节图像数据，仍保留 FF D9。
先核对字节数和 SHA。fixture 不匹配时不得继续声称复核了同一样本。
只把必要合成 fixture 纳入 androidTest 资产，不把宿主探针/JAR/脚本放进生产 APK。

四、先验证生产代码未变时的真实 Android 行为
使用已有测试 ContentResolver/provider 和生产 LocalImageStore，不 mock 关键解码器。
通过 content URI，声明 image/jpeg。仅使用已有授权的专用 API36 AVD；不重建 AVD、不清 App 数据、不改宿主时钟。
增加健康对照和缺损样本测试，建议方法：
- valid_control_jpeg_imports_successfully
- jpeg_entropy_truncated_with_eoi_is_rejected
健康样本应成功；缺损样本应明确返回 INVALID_IMAGE（如因格式支持边界返回 UNSUPPORTED，必须说明具体且合理的判定原因）。
记录 inspect、BitmapFactory、normalize/prepareImport 的真实结果及异常来源，不把 logcat 警告等同于 API 已拒绝。
如果新增拒绝断言实际失败，保存生产代码未变的真实 RED XML、日志、命令退出码和源码/fixture 指纹。
如果缺损样本本就被真实链路拒绝，提供明确拒绝位置、原始证据及健康样本成功对照；不要制造 RED，也不要做无意义修复。

五、仅当确实错误接受时做最小修复
先查根因，再实现受控的完整性判断。
不得以重新压缩后的输出可以解码来证明输入完整。
不得只检查 EOI、宽高、MIME 或 Bitmap 非空。
不得把宿主解码器结果直接当作 Android 实现方案。
若考虑 ImageDecoder 等平台能力，须验证其对本 fixture 的实际行为和 API26 兼容路径；不能假设换 API 即修复。
保持源流 64MiB、源图 40MP、输出最长边 1600px、存储 2MiB、有限重试、JPEG 方向及 PNG alpha 规则。
不引入无界全分辨率解码或无界解析；不盲目升级依赖、不增联网权限。
若最小可行修复需要增加依赖、改 minSdk 或显著扩张解析架构，先提供证据与最小方案并停在决策点，不自行扩大实现。

六、补上旧封面保护链路
用真实 Room 与私有文件，分别覆盖长期项目、订阅：
先有正常旧封面，再选择该缺损样本。
验证明确失败；旧 coverAssetId、元数据、私有文件字节/SHA 不变；
项目金额事件、订阅付款/状态和其他业务字段不变；
不提交新正式封面引用，临时残留符合既有安全清理协议。
不得为了让测试变绿跳过选择/导入步骤、删断言、吞异常或把未知结果标成 PASS。

七、证据、回归与交付
保留原 652 方法与历史失败证据，不改写旧运行结果。
任何生产或测试输入变化之后，都不能将原 652 个结果当作新输入的完整通过证明。
若实施修复，运行新增目标测试、完整 JVM/Android 回归、Lint、assemble，保留 XML、日志、实际命令、退出码和输入指纹；如分开执行必须说明且核对相同输入，不能合成“一次全量成功”。
若只补验证且无需生产修复，如实报告新增测试与现有输入差异、实际运行范围；未跑部分不能标 PASS。
报告正常/缺损样本的实际 Android 行为、是否复现、根因或否证、修改文件、原测试保留、新增方法集合、设备与构建边界。
按既有 M10 授权提交并推送当前功能分支，只包含本轮相关文件，返回最终 SHA。
不把 CURRENT_STATE 或 ACCEPTANCE_MATRIX 写为 ACCEPTED/FROZEN。
API26 未实测、真机/OEM/Release/TalkBack/真实 LMK/断电、M11/M12 完整往返继续如实 NOT_RUN；Q05 不整体 CLOSED。
最后返回 READY_FOR_REVIEW — M10 supplemental verification，然后停止，等待 ChatGPT 独立复核。
