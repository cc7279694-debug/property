# C1 — SDK许可原件来源索引

这是元数据索引，**不是官方XML、许可正文或脱敏后的官方原件**。7份原件仅移出本分支当前Git跟踪，实际本地文件未删除、未改字节。完整路径、原始SHA-256、Git blob、首次引入提交、官方URL及XML内许可ID见[机器可读清单](evidence/license-inventory.json)。

原始锁定提交：`4ee27bfcbe5903d6a49d4a92203de8a63686ba66`。该提交改动86文件（85新增、1修改）；扫描还包括其继承的全部2388个跟踪文件。4份A2-R原件及3份R1重复原件已定位。

路径以下均相对`docs/research/`：

| 原件 | 字节 | 原始SHA-256 |
|---|---:|---|
| `m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/installed-image-audit/package.xml` | 19209 | `aaab7f4381969c6de68a7c081e0d33d167e90ea6eb9047b98e684df5d635e002` |
| `m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/license-correction-archive-metadata/android-sdk-license.xml-extracted.utf8.txt` | 16962 | `1f8729233617b193fd619213792ae16a41b95d2bbbf525dfe66998252ba68b16` |
| `m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/official-metadata-20261018T023649.606169Z/android-sdk-license.txt` | 17107 | `00eceefbf21af958ba76722fdf3330143d7176e4626f341c3d8925a6e40726ea` |
| `m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/official-metadata-20261018T023649.606169Z/sys-img2-3.xml` | 237880 | `ad71946ba2d002f86d9ece1121b74aec494b420aa6b8fdac83fa5ce52b51dcb4` |
| `m10-jpeg-r1/evidence/android-sdk-license.txt` | 17107 | `00eceefbf21af958ba76722fdf3330143d7176e4626f341c3d8925a6e40726ea` |
| `m10-jpeg-r1/evidence/build-audit-raw/cmake-package.xml` | 18640 | `e6821ad8995aec83b86f40130e283f0994fa6ab7f198dd8bbd1d14ad72d6d621` |
| `m10-jpeg-r1/evidence/build-audit-raw/ndk-package.xml` | 18672 | `67a8c301a21e0a9430fc7236c8c7f3955fb05cffecdbe7ce66316632af7b48b8` |

`package.xml`、CMake和NDK的包XML是SDK生成/安装元数据，不冒充服务器原XML。官方镜像XML含8份许可正文（SDK、preview、Intel、MIPS、GoogleTV、GoogleXR、ARM DBT、microXR），因此整份退出跟踪；没有只删一个节点后以原文件名提交。公开保留[官方镜像元数据URL](https://dl.google.com/android/repository/sys-img/google_apis/sys-img2-3.xml)、[SDK条款入口](https://developer.android.com/studio/terms)及各自原始哈希。

许可规范化的历史结论不变：XML直接抽取的LF文本为16962字节；CRLF副本17107字节是假差异来源；正确SDK规范化标识`24333f8a63b6825ea9c5514f83c2829b004d1fee`匹配既有接受记录。未重算或替换旧记录、未重新接受许可。

历史结果仍在[原解析结果](../m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/official-metadata-20261018T023649.606169Z/result.json)、[许可纠正结果](../m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/license-correction-archive-metadata/result.json)、[安装后置审计](../m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/installed-image-audit/result.json)。它们提到的原件路径是历史来源指针，干净克隆不再包含正文；不能把原件缺失当作哈希改变，也不能重跑写入式历史收集器来覆盖结果。

**旧远端提交仍可经Git历史访问许可正文。**本次是普通追加提交，无force-push、filter、rebase、历史清除或原分支修改，不能宣称历史正文已经消失。当前HEAD仅保留来源和校验元数据。其他工作区和原R1分支没有被操作。
