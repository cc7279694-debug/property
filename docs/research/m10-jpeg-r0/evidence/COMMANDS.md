# R0实际命令、原始输出与复跑说明

执行地点为独立研究worktree；日期2026-10-16。所有PASS仅指相应预检/签名/清单，不是JPEG解码通过。没有执行native编译、strict decoder、Gradle或ADB。

| 实际操作 | 退出／结果 | 记录 |
|---|---|---|
| `git status/branch/rev-parse/worktree list/ls-remote` | 0，锁定来源匹配，原工作区干净 | 当前报告及最终Git回执；仅研究分支新建/推送 |
| `powershell -NoProfile -ExecutionPolicy Bypass -File docs/research/m10-jpeg-r0/scripts/collect_environment.ps1 -OutputPath docs/research/m10-jpeg-r0/evidence/environment.json` | 0，native PoC BLOCKED | [environment.json](environment.json)、[原stdout](environment-preflight.log)；仅子进程脚本策略，不改系统策略 |
| `python docs/research/m10-jpeg-r0/scripts/verify_locked_inputs.py --output docs/research/m10-jpeg-r0/evidence/locked-inputs.json` | 0，原SHA一致 | [JSON](locked-inputs.json)、[实际stdout](locked-inputs-preflight.log)；不解码entropy |
| `gh api .../releases/tags/3.2.0`、`.../releases/latest`、`.../git/ref/tags/3.2.0` | 0 | [正式release API原数据](release-3.2.0.json)、[latest](latest.json)、[tag](tag-3.2.0.json) |
| `gh release download 3.2.0 --repo libjpeg-turbo/libjpeg-turbo --pattern libjpeg-turbo-3.2.0.tar.gz --pattern libjpeg-turbo-3.2.0.tar.gz.sig --dir build/m10-jpeg-r0/upstream` | 0；仅源码/签名到忽略目录 | 资产大小/实际SHA见[provenance](upstream-provenance.json)，无安装包/新二进制下载或执行 |
| Git自带`gpg --batch --no-options --no-autostart --homedir build/m10-jpeg-r0/upstream/gnupg`后show-only/import/verify | 修正路径后三步均0；VALIDSIG且官方指纹匹配 | [show-only](gpg-relative-0.log)、[导入](gpg-relative-1.log)、[verify](gpg-relative-2.log)；临时公钥环未提交，无系统ownertrust改动 |
| `python docs/research/m10-jpeg-r0/scripts/verify_upstream.py --upstream-dir build/m10-jpeg-r0/upstream --output docs/research/m10-jpeg-r0/evidence/upstream-provenance.json` | 0 | [provenance](upstream-provenance.json)、[实际stdout](upstream-verification.log)；只读已下载资产，不编译/执行源码 |
| 原M10 APK只读`zipfile`枚举`lib/*/*.so`及SHA | 0 | [existing-apk-abi.json](existing-apk-abi.json)，无新APK、无安装 |

复跑时应把脚本输出改到新的`build/...`路径，保留已提交原记录。供应链脚本要求先通过官方CLI取同版本资产/metadata并执行独立GPG验证，不能仅伪造gpg-verification.json；原GOODSIG/VALIDSIG日志及实际tar/hash可互相复核。本轮下载的tar/.sig/公钥和临时ring都只留在忽略目录，不进入Git。

## 工具诊断（不是解码结果）

1. 第一次GPG给Git/MSYS程序传Windows绝对反斜杠homedir，被错误拼到cwd后，三命令退出2/NO_PUBKEY。原[第一步](gpg-first-path-0.log)、[导入](gpg-first-path-1.log)、[验签](gpg-first-path-2.log)保留；改用同研究目录的ASCII相对homedir后，同一tar/.sig/key三步均退出0。没有更新系统工具或放宽签名/指纹核验。
2. 来源清单脚本初版尝试读取正式tar未包含的`.github/SECURITY.md`，出现KeyError，尚未产出provenance成功记录。检查644个真实成员后，11个实际成员独立计算hash；安全政策改按固定GitHub tag单独读取，不伪造为第12个tar成员。本轮没有native RED/GREEN。
3. 只读检查一度请求尚未生成的upstream-verification.log而FileNotFound；后续由成功脚本真实stdout生成。日志文件存在与否不用于推导decoder成功。

上述诊断没有触及锁JPEG、产品/测试断言、schema、正式构建或两原工作区。严格JPEG warning/return/error-code均NOT_RUN；不得把工具退出码填为decoder结果。
