# 当前架构事实

## Verified：当前实际存在

仅有 Git 仓库、原始规划阅读版和 M00 项目记忆文档。没有 Android 工程、Gradle 配置、Kotlin/Compose 源码、Room 数据库/schema、DataStore 文件、资源文件、系统权限配置或 APK。

不描述尚未创建的路径为当前实现，不声称已离线启动、已无网络权限、已能备份或升级。

## Planned：继承的实施契约

来自[阅读版 A01～A08](../LITHIC_V1_产品与开发规划.md)：

```text
Compose → ViewModel → UseCase / Repository → Room / 领域引擎
                                         → 私有媒体文件
                                         → DataStore
```

一个 Gradle :app 模块，按功能和职责分包；轻量 AppContainer/构造注入。金额、项目、订阅、历史、年度等引擎接受可测试时钟与观察日，页面不复制公式。

完整恢复按阅读版 B05 规划为独立完整 generation、校验完成后原子切换小型指针、保留短暂失败回退。数据库、图片与设置须共同验证，不能用 Room 事务声称覆盖全部文件。

schema 计划 v1(M04) → v2(M08) → v3(M16) → v4(M20)；backup format 计划 v1(M11) → v2(M16) → v3(M20)。这仅是规划，不是当前版本。

实际架构在对应获准模块完成并验证后更新此文件；具体版本锁由 M01 产出。本轮只记录约束，不创建上述工程目录或配置。
