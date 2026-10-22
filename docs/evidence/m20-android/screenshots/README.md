# M20 实际截图

设备：Lithic_M20_API36_20261022 / emulator-5704 / API36。5个最终文件，4个不同SHA；01和02同画面，不重复计数。

设置页为隔离ComponentActivity的真实Compose画布（非MainActivity）；新设备实际未授POST权限，未测试用户点击系统拒绝弹窗。03/04是360dp、LocalDensity 200%，系统font_scale保持1.0。最后一张是真实SystemUI通知，发布使用生产NotificationPublisher；其平台JUnit仍FAIL，不因截图可见改成PASS。

|文件|来源|字节|SHA-256|
|---|---|---:|---|
|[01-preferences-and-actual-denial.png](gallery/01-preferences-and-actual-denial.png)|990×2340|290608|`d2562b9a49faa1cb088a715ee7839ec33daec289f179e281dba74b425c02fb84`|
|[02-permission-denial-explanation.png](gallery/02-permission-denial-explanation.png)|990×2340|290608|`d2562b9a49faa1cb088a715ee7839ec33daec289f179e281dba74b425c02fb84`|
|[03-local360-200-switches.png](gallery/03-local360-200-switches.png)|990×2340|265098|`01215db1a5630f14724652566cd7a6e7bc3dc9db1ca46b7f2f87d1c960ef198b`|
|[04-local360-200-return.png](gallery/04-local360-200-return.png)|990×2340|270875|`614ed613e82d566a18380e6246c30375c5037302920eada30eb87fb54864ddfb`|
|[real-notification.png](../platform-final-observation/real-notification.png)|1080×2340|178879|`51c707998a0c986e50fb4d437db24471b06a669ba013e4562542dbf0ccb384ac`|

[原始设置来源](gallery/gallery-source.json)、[SHA/尺寸索引](index.json)、[通知实际执行/失败及收口范围](../results.json)。旧平台首次失败后的独立截图保留于platform-observation，未替换原UNKNOWN。
