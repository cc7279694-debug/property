# C3 后续构建与原生截图命令（未执行）

**当前仅 SOURCE_PREPARED / BUILD_DEFERRED。用户已要求构建暂缓。以下构建命令须待用户恢复桌面构建；所有设备命令还须 ChatGPT 独立核对和用户新的明确设备授权。不得在本轮执行。**

## 1. 桌面产物门禁

在本项目根、既有 JDK17/SDK36 环境中，待内存允许且获准恢复构建后：

```powershell
.\gradlew.bat --offline --console=plain --max-workers=1 `
  --project-cache-dir .gradle/c2-visual `
  -Pc2VisualVerification=true `
  :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

App 预定输出 `app/build-c2-visual/outputs/apk/debug/app-debug.apk`，Test 预定输出 `app/build-c2-visual/outputs/apk/androidTest/debug/app-debug-androidTest.apk`，**本轮均未生成**。显式 flag 已自动选择 M13Runner，不需要再混用其他实验 flag。使用独立项目缓存与独立输出目录；不执行 clean，也不重建 Preview/M22 包。

构建之后必须真实读取两份合并 Manifest 及 APK 内 Manifest，使用已锁定 SDK 的 `aapt.exe dump badging` / `aapt.exe dump xmltree <APK> AndroidManifest.xml`、`apksigner.bat verify --verbose --print-certs` 和 `Get-FileHash -Algorithm SHA256`，记录命令/退出码/原始输出：

- App：`com.cooper.lithic.c2visual.debug`、code17 / `0.1.0-c2visual-debug`；Test：`com.cooper.lithic.c2visual.debug.test`。
- App Application 为 `android.app.Application`，只注册 AndroidX 空 `ComponentActivity`；无 MainActivity/Launcher、Provider、Service、Receiver、sharedUserId。无 INTERNET/通知/唤醒锁/网络状态/开机权限；若出现新组件或未知 authority，停止，不能仅相信源 XML 的移除声明。
- Test：唯一 Instrumentation 为 `com.cooper.lithic.app.M13IsolatedRunner`，targetPackage 仅为新 App，无 shared UID 或未知启动组件。检查真实 Test DEX 中 Runner 实现和新 Gallery，不能仅依据源码或包名。
- 两 APK 证书须为既有 Debug 公钥指纹 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；此为预期值，**不是本轮新产物签名结果**。不生成新密钥。
- 实际配置核对普通 Debug、Release、PrivateTrial、M22 的原身份；测试互斥 flag 和仅 Debug 变体规则。未通过不得设备安装。

## 2. 未来设备准入与一次性执行

仅限之后明确授权的唯一设备。先核实 USB/设备身份、资源、系统健康，以及**两个新包连同保留记录均不存在**；已有或归属不明即停止，不覆盖/卸载。使用 `adb -s <已核验序列>`，不操作旧 App/其他设备，不改系统字体/density/网络/主题。

以下仅是后续命令模板；`$Serial` 必须由新的授权与实时核验提供，不能写入公共报告。`$App` / `$Test` 指向上节真正通过门禁的文件，安装前/后 SHA 必须与独立审查值一致，不得替换或重建后冒充同一候选：

```powershell
# 后续获准后各安装一次；不使用 -r/-d/-g，不卸载或清数据。
# -t 仅允许安装AndroidTest包，不自动授予权限，不覆盖旧包。
adb -s $Serial install $App
adb -s $Serial install -t $Test
# 只有两次安装成功、设备回读字节/SHA/UID/Runner/目标确认后，才继续。

# 每次仅选一个方法；下列变量的值从固定白名单选取。
$Method = 'A_dark_400_confirmed_empty_now'
adb -s $Serial shell am instrument -w -r -e class `
  "com.cooper.lithic.ui.C2ProductionVisualGalleryTest#$Method" `
  com.cooper.lithic.c2visual.debug.test/com.cooper.lithic.app.M13IsolatedRunner
```

方法白名单（依次单方法，不能整包或运行历史方法）：

1. `A_dark_400_confirmed_empty_now`
2. `B_dark_400_confirmed_empty_review`
3. `C_light_400_synthetic_project_subscription_now`（会另留一帧记录滚动图）
4. `D_light_400_synthetic_history_review`
5. `E1_dark_360_local_200percent_now`
6. `E2_dark_360_local_200percent_review`
7. `F1_device_configuration_empty_now`
8. `F2_device_configuration_empty_review`

保存逐方法 stdout/stderr、外层退出码及完整 JUnit 结束状态。外层退出 0 本身不等于 PASS；失败/中断/身份不符即停止、不自动重跑，保留新包自己的现场。F 跟随实际配置，不保证恰好400dp或2倍字体；A～E的 LocalDensity 不能代替 F。所有画面为生产组件＋隔离 ComponentActivity，不是 MainActivity/真实业务旅程或系统 IME 验收。

## 3. 仅精确提取新包的三个文件

每张完整截图通过 Instrumentation 状态输出 `c3_capture_directory`、`c3_capture_png`、`c3_capture_sha256`。只使用该次成功方法的真实值；不要扫描 `/data/data`、拉取整个包、遍历截图目录或使用 `adb pull /data/...`。

仅以下限定读法有效；不读其他包、不 root、不 chmod，不使用可能重编码 PNG 的旧 PowerShell `>` 重定向：

```text
adb -s <序列> exec-out run-as com.cooper.lithic.c2visual.debug cat files/<本次UUID目录>/owner.json
adb -s <序列> exec-out run-as com.cooper.lithic.c2visual.debug cat files/<本次UUID目录>/capture.json
adb -s <序列> exec-out run-as com.cooper.lithic.c2visual.debug cat files/<本次UUID目录>/<本次PNG>
```

后续可按下面的有界 Python 读取例子执行。此段本轮未运行；默认缺少授权环境变量即拒绝。`C3_APP_SHA` / `C3_TEST_SHA` 必须填**未来桌面及设备字节核验后的真实值**；UUID/PNG/PNG SHA 必须来自原始 Instrumentation 输出，不能猜测或预填。结果只写全新本地目录，不覆盖旧证据。

```python
import os, re, json, hashlib, subprocess, struct, threading
from pathlib import Path

assert os.environ.get('C3_CAPTURE_AUTHORIZED') == 'YES_AFTER_REVIEW'
serial = os.environ['C3_DEVICE_SERIAL']  # 本地保管，不能提交硬件序列号
assert re.fullmatch(r'[A-Za-z0-9._:-]+', serial)
directory, png = os.environ['C3_DIRECTORY'], os.environ['C3_PNG']
assert re.fullmatch(r'ui-c2-c3-gallery-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}', directory)
assert re.fullmatch(r'(?:A|B|C(?:-records-scrolled)?|D|E1|E2|F1|F2)\.png', png)
expected = [os.environ[k] for k in ('C3_APP_SHA', 'C3_TEST_SHA', 'C3_PNG_SHA')]
assert all(re.fullmatch(r'[0-9a-f]{64}', s) for s in expected)
pkg = 'com.cooper.lithic.c2visual.debug'
dest = Path(os.environ['C3_OUTPUT_PARENT']).resolve() / directory
assert dest.parent.is_dir()
dest.mkdir(exist_ok=False)

def read_exact(name, limit):
    assert name in ('owner.json', 'capture.json', png)
    args = ['adb', '-s', serial, 'exec-out', 'run-as', pkg, 'cat', f'files/{directory}/{name}']
    with (dest / name).open('xb') as output, (dest / (name + '.stderr')).open('xb') as errors:
        process = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=errors)
        total = 0
        expired = threading.Event()
        def stop_this_reader():
            expired.set()
            if process.poll() is None:
                process.terminate()  # 仅本次桌面adb读取客户端，不是手机进程
        timer = threading.Timer(30, stop_this_reader)
        timer.start()
        try:
            while True:
                block = process.stdout.read(65536)
                if not block: break
                total += len(block)
                if total > limit: raise RuntimeError('Read bound exceeded; retain partial evidence')
                output.write(block)
            assert process.wait(timeout=5) == 0 and not expired.is_set()
        except BaseException:
            # Only stop this desktop reader, never the Android app or device; do not retry.
            if process.poll() is None: process.terminate()
            raise
        finally:
            timer.cancel()
    assert 0 < total <= limit
    return (dest / name).read_bytes()

owner = json.loads(read_exact('owner.json', 65536))
meta = json.loads(read_exact('capture.json', 65536))
assert owner['package'] == meta['package'] == pkg
assert owner['kind'] == 'C2_SYNTHETIC_ANDROID_CAPTURE'
assert owner['runId'] == meta['runId'] == directory.removeprefix('ui-c2-c3-gallery-')
assert meta['testPackage'] == pkg + '.test'
assert meta['runner'] == 'com.cooper.lithic.app.M13IsolatedRunner'
assert meta['applicationClass'] == 'android.app.Application'
assert meta['hostClass'] == 'androidx.activity.ComponentActivity' and meta['mainActivity'] is False
assert meta['appApkSha256'] == expected[0] and meta['testApkSha256'] == expected[1]
assert meta['sha256'] == expected[2] and 0 < meta['bytes'] <= 20 * 1024 * 1024
assert meta['syntheticFacts'] is True and meta['storageOwnerCreated'] is False
assert meta['case'] + '.png' == png
data = read_exact(png, meta['bytes'])
assert len(data) == meta['bytes'] and hashlib.sha256(data).hexdigest() == expected[2]
assert data[:8] == b'\x89PNG\r\n\x1a\n'
assert struct.unpack('>II', data[16:24]) == (meta['widthPx'], meta['heightPx'])
print('Only this UUID owner, metadata and verified PNG were read; device files retained.')
```

若 `run-as` 被 OEM 限制、返回非零、输出超限、元数据与SHA/归属不匹配，停止，标记 UNKNOWN，不升级权限、不扩大读取范围、不删除任何设备文件。上例尚未经过真实设备执行；后续必须记录真实退出码，并对提取后的图片人工确认没有无关个人内容再决定归档。
