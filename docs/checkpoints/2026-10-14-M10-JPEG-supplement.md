# M10 JPEG 补充验证 checkpoint

日期：2026-10-14，Asia/Shanghai。历史停止点：**PENDING / BLOCKED_AT_SCOPE_DECISION**，补充证据READY_FOR_REVIEW，不是ACCEPTED/FROZEN。

- Goal：独立评审d7e9的EOI保留但entropy缺损JPEG疑点，核对真实Android导入与旧封面保护。
- Verified completed：原ZIP/14文件/两份合成fixture CRC、大小、SHA与路径校验；原API36 AVD实际ContentResolver → LocalImageStore复现错误Success。健康对照成功，ImageDecoder同样接受且partialErrors=[]；5测试2 PASS /3 FAIL，193.685秒退出1。另跑Lint/assemble82.984秒退出0，0 errors/23 warnings。
- Changes：只新增3测试文件、2合成资产和补充证据，更新CURRENT_STATE/README入口/字节保护；生产代码、schema、依赖不变，正式验收登记不改。152份完整输入两个阶段/ZIP相同，原652方法保留，新5方法单独实跑。
- Data safety：两类合成旧封面、完整业务事实及私有正式文件在错误准备后不变；测试未提交缺损图。拒绝断言失败，失败清理未到达。活动七表所有原事实、两份私有封面、同代/设置/时区及安装APK前后不变；未重建AVD/清数据/改时钟；私人数据与完整日志未提交。
- Inherited decisions：M00～M09冻结历史、Room v2、同代owner、原652证据/原130案例/16组跨模块边界及Q03/Q04/Q06期限保留。Q05 USER_CONFIRMED，M11/M12往返NOT_RUN。
- Known limitations：缺陷尚未修复，3项真实FAIL。原652在新152输入下完整回归NOT_RUN；API26/真机/OEM/Release/TalkBack/LMK/断电和完整备份恢复仍NOT_RUN。生产APK SHA `939b3b20d3c6cd02c800f320ccddc300736b75084b609c404f38b096270f7094`未变，不能称修复版。
- Stop basis：用户补充指令第5节要求新增依赖、改minSdk或显著扩展解析时先交证据/最小方案并停止。已实测ImageDecoder不能解决；严格entropy消费超出现有marker扫描。建议受控libjpeg-turbo/JNI预检与NDK/CMake，需要ChatGPT确认新范围；PROPOSED / NOT_APPROVED / NOT_IMPLEMENTED。
- Git：本次起点 `d7e9a92096b70d6e5976ebcc65dbdfda32b17296`，仅 `feat/m10-local-cover-media`；M09来源57de897，main基准145326不变。新提交SHA及推送以最终Git回执为准。
- Verification and evidence：[完整补充报告](../evidence/M10-JPEG-supplement-verification.md)、[JSON](../evidence/M10-JPEG-supplement-verification.json)、[RED XML](../evidence/m10-jpeg-supplement/android-production-unchanged-android-0.xml)、[实际观察](../evidence/m10-jpeg-supplement/observations/manifest.json)。历史M10报告/checkpoint原字节不改。
- Next step：ChatGPT独立复核本次结果及待授权严格JPEG预检提案。不得启动M11或自行冻结。
