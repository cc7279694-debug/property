# M04 执行契约与 Q06 初始数据集设计

设计先于 DataGenerationStore 实现，2026-10-11。当前用户明确授权 M04；本文件不是独立验收结论。

## 输入与范围

- 来源：本轮用户粘贴指令，SHA-256 `869d51ecf2aa8d4edb087bbb43843f5e66db17c212e8ca1962fbd4dedd06ba4a`；原件位于用户附件目录，不纳入仓库。
- Git 起点 `ffd066ffe0efb6c00f35526ae792b50829d3230e`，来源 `feat/m03-subscription-engine`，工作分支 `feat/m04-room-storage-foundation`。已检查干净工作区及本地/远端一致。
- 继承已冻结 M00～M03、Money、BusinessClock 和原生 Kotlin 路线；不修改不可变归档。
- 目标：四表 Room v1、项目/初始事件事务与幂等、一致读取、固定记录时区、首次完整数据集及持久指针；保留 M01 占位 UI。
- 排除：后续 UI、订阅表、替换/通知表、媒体处理、备份格式、覆盖恢复、清理旧代、云端和 main 合并。

## Q06：M04 负责的持久结构

```text
noBackupFilesDir/
  active-generation              # AtomicFile 管理的持久指针
  generations/
    <UUID>/
      data.db                    # Room v1，WAL/SHM 同目录
      media/
      settings.preferences_pb    # Preferences DataStore 实际格式及要求的扩展名
      ready.json                 # 初始化完成的身份/格式标记
```

指针元数据包含 `formatVersion`、`activeId`、`previousId`、`stage`。M04 只写 `STABLE`、`previousId=null`；保留可表达未来 `PENDING_VALIDATION` 的字段，遇到未支持阶段明确失败。这个指针格式是内部存储元数据，**不是 M11 备份格式版本**。

首次创建顺序：创建新 UUID 目录 → 创建并校验数据库/两个类型的默认类别 → 首次持久化记录时区 → 写 ready 身份标记 → 原子写 active 指针。失败保留未激活目录，不能暴露半成品。ready 不保存会因正常写入变化的数据库/设置初始哈希。

重开只接受持久指针确定的同一代；先核对路径、ready、数据库、设置存在和格式，再打开存储。已有代或 AtomicFile 辅助文件意味着已有数据证据：丢失/损坏指针、目录/数据库/设置异常都明确失败，不新建空账本。SQLite 腐败处理不能删除原数据库，DataStore 不配置默认替换损坏文件的处理器。普通初始化不按目录修改时间选代，不清理任何旧代。

同一根目录与同一 canonical 设置文件有受控单一活跃所有者。关闭旧 DataStore 时取消并等待 scope 完成，再允许重开；数据库关闭与代内文件一起管理。所有路径必须保持在当前私有代目录内。

## 数据与并发契约

- 仅 Category、LongTermProject、ProjectMoneyEvent、LocalMediaAsset 四张业务表。业务 UUID、事件唯一 operationId、实体外键及查询索引必须真实落入 SQLite。
- Category 的 `(type, normalizedName)` 唯一；规范化采用 Unicode NFKC、trim、连续空白归一、Locale.ROOT 小写。两种“其他”使用不同稳定 UUID。项目引用类别的类型在 Repository 事务内验证。
- LocalDate 保存 Long epochDay；Instant 使用 ISO-8601 字符串，保留纳秒。金额使用已有 Money 的 Long 分边界与 OUTLAY/RECOVERY，零元有效；不存派生日均或第二份购买价格。
- LocalWriteCoordinator 使用一个 Mutex 协调写入与快照；Room `withTransaction` 负责项目/初始事件原子性。文件/DataStore 只共享协调边界，不宣称跨存储原子事务。
- CreateProjectCommand 显式携带稳定项目、事件、commandId 及写入时刻；事件 operationId 必须匹配 commandId。完全相同命令重试返回原记录；同 ID 异内容返回明确冲突。业务输入校验失败、存储失败分开表达，不使用 IGNORE。
- LedgerReadRepository 在同一 Room 读取事务中取得四表真实内容；不提供虚构订阅数据。

## 装配与任务顺序

1. 锁定稳定依赖与测试配置，保持既有工具链；建立测试与最小可编译接口。
2. 数据库/Repository 与设置/generation 按以上接口边界并行；主代理独占 Gradle 与设备操作，先执行真实行为 RED，再实现 GREEN。
3. Application 的轻量 AppContainer 持有单一数据层所有者，UI 不添加业务入口。
4. 执行 JVM 74 项回归及新增测试、独立模拟器上的真实 SQLite/DataStore instrumentation、Lint/构建/schema/APK/权限检查。
5. 校核原件、链接、差异及敏感信息；更新唯一当前状态、当前验收、架构/问题/工具链/checkpoint/证据，提交推送 M04 分支后停止。

## M12 仍需实施和验收

完整备份解析、真实新数据集导入、图片/设置/数据库共同校验、切换与回退、持久 previousId/pending-validation 流程、切换前后杀进程故障注入、旧代安全清理全部留给 M12。Q06 保持 ENGINEERING_REQUIRED，原 Q06-V01～V03 全流程补测仍 NOT_RUN；M04 的初始数据集实测另行登记。Q03～Q05 与 M09 两项流程风险不变。

## 验证门槛

M04-T01～T06 与本轮授权的幂等冲突/事务中途失败/类别类型/金额日期/关闭重开/一致快照/初始指针异常边界都需实际证据。环境错误或缺设备不计 RED。原 130 案例原件仍 NOT_RUN；独立审阅前最多 READY_FOR_REVIEW，不自行冻结。
