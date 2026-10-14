# M10 独立评审补充：JPEG 图像数据截断但结束标记保留

评审对象：cc7279694-debug/property
分支：feat/m10-local-cover-media
评审 HEAD：d7e9a92096b70d6e5976ebcc65dbdfda32b17296
M09 冻结来源：57de897917473f255d43dfb306a8433b366c00eb

本轮结论：PENDING。此文件不是仓库的正式冻结回写，也不是完整 M10 验收报告。
本轮没有修改远端仓库，没有运行用户 Windows Gradle 或 Android 模拟器。

## 已经实际验证的内容

1. complete.jpg 是本轮生成的 256×256 合成 JPEG，不含用户个人图片。
2. entropy_cut_eoi_retained.jpg 保留同一图片的头部、尺寸与扫描头，截去后段图像压缩数据，再保留原 FF D9 结束标记。
3. 完整样本 59,334 字节；缺损样本 20,194 字节；移除了 39,140 字节图像数据。
4. 在当前 Linux 容器执行从被评审源码抽取的同逻辑 JPEG 扫描函数，两份文件都被返回为 image/jpeg 256×256。
5. 当前容器的 Pillow 和 OpenCV 都能返回缺损样本的图片。OpenCV 同时输出 premature end of data segment 警告；Pillow 解码结果底部 64 行均为灰色 128。
6. 文件 SHA-256、截断构造关系和 JVM 输出均已重新核验。原始输出在本包中。

完整样本 SHA-256：
2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907
缺损样本 SHA-256：
f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a

## 证据边界

- JpegScannerProbe.kt 只抽取 jpeg() 扫描逻辑，周围类型和异常为宿主运行适配。它没有执行完整 AndroidImageCodec.normalize()。
- 宿主 Pillow/OpenCV 的结果不能直接当成 Android BitmapFactory 或真实 App 的结果。
- 已查看的 AOSP BitmapFactory.cpp 普通解码路径允许 SkCodec::kIncompleteInput 继续返回 Bitmap，故“非空 Bitmap + 正确宽高”本身不能证明输入完整。
- 尚未将这份缺损样本送入该项目真实 ContentResolver → LocalImageStore → MediaRepository 链路。
- 不能据此声称已经在 Android 上复现旧封面被替换，也不能声称金融数据受损。
- 此问题需要 API36 的真实、带正常对照的回归验证；不能只据宿主实验盲目改生产代码。
- 本轮没有完成所有 147 项源码输入、两份完整源码 ZIP、全部 652 个方法、旧 549 方法和最终 APK 字节的逐项独立复算，不授予总体验收 PASS。

## 对应源码

app/src/main/java/com/cooper/lithic/data/media/AndroidImageCodec.kt
- jpeg()：约 236–288 行，SOF/SOS/EOI 与分段边界检查，没有证明扫描压缩数据覆盖完整图像。
- normalize()/verifyDecode()：BitmapFactory 返回值及宽高检查，不等于完整解码状态检查。
- 被评审文件 Git blob：e6cd708ea0b3465f9c10baf2fc7b302e9c90d749。

app/src/androidTest/java/com/cooper/lithic/data/media/LocalImageStoreTest.kt
- 第 226 行 truncated_jpeg_is_rejected 仅将末尾两个字节删除，未覆盖“EOI 存在而图像数据缺失”。

对应契约：M10-T04 损坏输入明确拒绝，以及 M10-T03 替换失败保护旧封面。

## 重跑宿主扫描器

在此目录使用已有 JDK/Kotlin 编译器：

```sh
kotlinc JpegScannerProbe.kt -include-runtime -d scanner-probe.jar
java -jar scanner-probe.jar complete.jpg entropy_cut_eoi_retained.jpg
```

预期输出是两个 ACCEPTED_BY_SCANNER；这是风险复现，不是“损坏图片测试通过”。

make_fixtures.py 仅用于解释和复现样本构造，需要 Pillow、NumPy 和 OpenCV。
本轮的具体版本记录在 host-environment.json。不同编码器版本生成字节可能不同，
Android 复核应优先使用本包中已经锁定 SHA 的两份 JPEG，而不是重新生成后仍沿用旧 SHA。

## 下一步

将本包与 CODEX_FOLLOW_UP.md 一并交给 Codex。
先在生产代码未变的情况下跑真实 Android 正常/缺损对照，确认发生在哪一层。
若真实链路错误接受缺损输入，再做最小修复和完整回归。
不冻结 M10，不进入 M11，不改变 Q05 或现有产品范围。
