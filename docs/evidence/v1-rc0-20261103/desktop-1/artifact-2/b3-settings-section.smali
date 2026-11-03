
.method public static final PhotoStorageSection(Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;Lkotlin/jvm/functions/Function0;Landroidx/compose/runtime/Composer;I)V
    .registers 11
    .param p0, "state"    # Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .param p1, "onRefresh"    # Lkotlin/jvm/functions/Function0;
    .param p2, "$composer"    # Landroidx/compose/runtime/Composer;
    .param p3, "$changed"    # I
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;",
            "Landroidx/compose/runtime/Composer;",
            "I)V"
        }
    .end annotation

    const-string v0, "state"

    invoke-static {p0, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    .line 44
    const v0, -0x301f6a6d

    invoke-interface {p2, v0}, Landroidx/compose/runtime/Composer;->startRestartGroup(I)Landroidx/compose/runtime/Composer;

    move-result-object v4

    .end local p2    # "$composer":Landroidx/compose/runtime/Composer;
    .local v4, "$composer":Landroidx/compose/runtime/Composer;
    const-string p2, "C(PhotoStorageSection)N(state,onRefresh)44@2464L2495,44@2401L2558:DataConfidenceSections.kt#e7edxl"

    invoke-static {v4, p2}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    move p2, p3

    .local p2, "$dirty":I
    and-int/lit8 v1, p3, 0x6

    if-nez v1, :cond_29

    and-int/lit8 v1, p3, 0x8

    if-nez v1, :cond_1f

    invoke-interface {v4, p0}, Landroidx/compose/runtime/Composer;->changed(Ljava/lang/Object;)Z

    move-result v1

    goto :goto_23

    :cond_1f
    invoke-interface {v4, p0}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v1

    :goto_23
    if-eqz v1, :cond_27

    const/4 v1, 0x4

    goto :goto_28

    :cond_27
    const/4 v1, 0x2

    :goto_28
    or-int/2addr p2, v1

    :cond_29
    and-int/lit8 v1, p3, 0x30

    if-nez v1, :cond_39

    invoke-interface {v4, p1}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_36

    const/16 v1, 0x20

    goto :goto_38

    :cond_36
    const/16 v1, 0x10

    :goto_38
    or-int/2addr p2, v1

    :cond_39
    and-int/lit8 v1, p2, 0x13

    const/16 v2, 0x12

    const/4 v3, 0x1

    if-eq v1, v2, :cond_42

    move v1, v3

    goto :goto_43

    :cond_42
    const/4 v1, 0x0

    :goto_43
    and-int/lit8 v2, p2, 0x1

    invoke-interface {v4, v1, v2}, Landroidx/compose/runtime/Composer;->shouldExecute(ZI)Z

    move-result v1

    if-eqz v1, :cond_84

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v1

    if-eqz v1, :cond_57

    const/4 v1, -0x1

    const-string v2, "com.cooper.lithic.feature.settings.PhotoStorageSection (DataConfidenceSections.kt:43)"

    invoke-static {v0, p2, v1, v2}, Landroidx/compose/runtime/ComposerKt;->traceEventStart(IIILjava/lang/String;)V

    .line 45
    :cond_57
    sget-object v0, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v0, Landroidx/compose/ui/Modifier;

    const-string v1, "photo-storage-section"

    invoke-static {v0, v1}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v2

    new-instance v0, Lcom/cooper/lithic/feature/settings/DataConfidenceSectionsKt$$ExternalSyntheticLambda0;

    invoke-direct {v0, p0, p1}, Lcom/cooper/lithic/feature/settings/DataConfidenceSectionsKt$$ExternalSyntheticLambda0;-><init>(Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;Lkotlin/jvm/functions/Function0;)V

    const/16 v1, 0x36

    const v5, 0x469a96fa

    invoke-static {v5, v3, v0, v4, v1}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->rememberComposableLambda(IZLjava/lang/Object;Landroidx/compose/runtime/Composer;I)Landroidx/compose/runtime/internal/ComposableLambda;

    move-result-object v0

    move-object v3, v0

    check-cast v3, Lkotlin/jvm/functions/Function3;

    const/16 v5, 0x1b6

    const/4 v6, 0x0

    const-string v1, "\u7167\u7247\u7a7a\u95f4\u7ba1\u7406"

    invoke-static/range {v1 .. v6}, Lcom/cooper/lithic/core/design/C2PageKt;->C2Section(Ljava/lang/String;Landroidx/compose/ui/Modifier;Lkotlin/jvm/functions/Function3;Landroidx/compose/runtime/Composer;II)V

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v0

    if-eqz v0, :cond_87

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->traceEventEnd()V

    goto :goto_87

    .line 44
    :cond_84
    invoke-interface {v4}, Landroidx/compose/runtime/Composer;->skipToGroupEnd()V

    .line 78
    :cond_87
    :goto_87
    invoke-interface {v4}, Landroidx/compose/runtime/Composer;->endRestartGroup()Landroidx/compose/runtime/ScopeUpdateScope;

    move-result-object v0

    if-eqz v0, :cond_95

    new-instance v1, Lcom/cooper/lithic/feature/settings/DataConfidenceSectionsKt$$ExternalSyntheticLambda1;

    invoke-direct {v1, p0, p1, p3}, Lcom/cooper/lithic/feature/settings/DataConfidenceSectionsKt$$ExternalSyntheticLambda1;-><init>(Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;Lkotlin/jvm/functions/Function0;I)V

    invoke-interface {v0, v1}, Landroidx/compose/runtime/ScopeUpdateScope;->updateScope(Lkotlin/jvm/functions/Function2;)V

    :cond_95
    return-void
.end method


.method private static final PhotoStorageSection$lambda$0(Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;Lkotlin/jvm/functions/Function0;Landroidx/compose/foundation/layout/ColumnScope;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 33
    .param p0, "$state"    # Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .param p1, "$onRefresh"    # Lkotlin/jvm/functions/Function0;
    .param p2, "$this$C2Section"    # Landroidx/compose/foundation/layout/ColumnScope;
    .param p3, "$composer"    # Landroidx/compose/runtime/Composer;
    .param p4, "$changed"    # I

    move-object/from16 v0, p0

    move-object/from16 v9, p3

    move/from16 v1, p4

    const-string v2, "$this$C2Section"

    move-object/from16 v3, p2

    invoke-static {v3, v2}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v2, "C66@4200L10,67@4257L11,65@4057L229,69@4424L10,70@4481L11,68@4295L215,72@4666L10,73@4723L11,71@4519L233:DataConfidenceSections.kt#e7edxl"

    invoke-static {v9, v2}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    and-int/lit8 v2, v1, 0x11

    const/16 v4, 0x10

    const/16 v27, 0x1

    if-eq v2, v4, :cond_1d

    move/from16 v2, v27

    goto :goto_1e

    :cond_1d
    const/4 v2, 0x0

    :goto_1e
    and-int/lit8 v4, v1, 0x1

    invoke-interface {v9, v2, v4}, Landroidx/compose/runtime/Composer;->shouldExecute(ZI)Z

    move-result v2

    if-eqz v2, :cond_301

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v2

    if-eqz v2, :cond_35

    const/4 v2, -0x1

    const-string v4, "com.cooper.lithic.feature.settings.PhotoStorageSection.<anonymous> (DataConfidenceSections.kt:45)"

    const v5, 0x469a96fa

    invoke-static {v5, v1, v2, v4}, Landroidx/compose/runtime/ComposerKt;->traceEventStart(IIILjava/lang/String;)V

    .line 46
    :cond_35
    nop

    .line 47
    sget-object v2, Lcom/cooper/lithic/feature/settings/PhotoStorageUiState$Idle;->INSTANCE:Lcom/cooper/lithic/feature/settings/PhotoStorageUiState$Idle;

    invoke-static {v0, v2}, Lkotlin/jvm/internal/Intrinsics;->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_83

    const v2, 0xa50c8da

    invoke-interface {v9, v2}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v2, "46@2571L10,46@2529L64"

    invoke-static {v9, v2}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    sget-object v2, Landroidx/compose/material3/MaterialTheme;->INSTANCE:Landroidx/compose/material3/MaterialTheme;

    sget v4, Landroidx/compose/material3/MaterialTheme;->$stable:I

    invoke-virtual {v2, v9, v4}, Landroidx/compose/material3/MaterialTheme;->getTypography(Landroidx/compose/runtime/Composer;I)Landroidx/compose/material3/Typography;

    move-result-object v2

    invoke-virtual {v2}, Landroidx/compose/material3/Typography;->getBodyMedium()Landroidx/compose/ui/text/TextStyle;

    move-result-object v22

    const/16 v25, 0x0

    const v26, 0x1fffe

    const-string v1, "\u5c1a\u672a\u8bfb\u53d6\u7167\u7247\u767b\u8bb0\u4fe1\u606f\u3002"

    const/4 v2, 0x0

    const-wide/16 v3, 0x0

    const/4 v5, 0x0

    const-wide/16 v6, 0x0

    const/4 v8, 0x0

    const/4 v9, 0x0

    const/4 v10, 0x0

    const-wide/16 v11, 0x0

    const/4 v13, 0x0

    const/4 v14, 0x0

    const-wide/16 v15, 0x0

    const/16 v17, 0x0

    const/16 v18, 0x0

    const/16 v19, 0x0

    const/16 v20, 0x0

    const/16 v21, 0x0

    const/16 v24, 0x6

    move-object/from16 v23, p3

    invoke-static/range {v1 .. v26}, Landroidx/compose/material3/TextKt;->Text-Nvy7gAk(Ljava/lang/String;Landroidx/compose/ui/Modifier;JLandroidx/compose/foundation/text/TextAutoSize;JLandroidx/compose/ui/text/font/FontStyle;Landroidx/compose/ui/text/font/FontWeight;Landroidx/compose/ui/text/font/FontFamily;JLandroidx/compose/ui/text/style/TextDecoration;Landroidx/compose/ui/text/style/TextAlign;JIZIILkotlin/jvm/functions/Function1;Landroidx/compose/ui/text/TextStyle;Landroidx/compose/runtime/Composer;III)V

    move-object/from16 v9, v23

    invoke-interface {v9}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    goto/16 :goto_1ec

    .line 48
    :cond_83
    sget-object v1, Lcom/cooper/lithic/feature/settings/PhotoStorageUiState$Loading;->INSTANCE:Lcom/cooper/lithic/feature/settings/PhotoStorageUiState$Loading;

    invoke-static {v0, v1}, Lkotlin/jvm/internal/Intrinsics;->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_d9

    const v1, 0xa50d695

    invoke-interface {v9, v1}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v1, "48@2738L10,47@2637L123"

    invoke-static {v9, v1}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    sget-object v1, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v1, Landroidx/compose/ui/Modifier;

    const-string v2, "photo-storage-loading"

    invoke-static {v1, v2}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v2

    .line 49
    sget-object v1, Landroidx/compose/material3/MaterialTheme;->INSTANCE:Landroidx/compose/material3/MaterialTheme;

    sget v3, Landroidx/compose/material3/MaterialTheme;->$stable:I

    invoke-virtual {v1, v9, v3}, Landroidx/compose/material3/MaterialTheme;->getTypography(Landroidx/compose/runtime/Composer;I)Landroidx/compose/material3/Typography;

    move-result-object v1

    invoke-virtual {v1}, Landroidx/compose/material3/Typography;->getBodyMedium()Landroidx/compose/ui/text/TextStyle;

    move-result-object v22

    .line 48
    const-string v1, "\u6b63\u5728\u8bfb\u53d6\u7167\u7247\u767b\u8bb0\u4fe1\u606f\u2026"

    const-wide/16 v3, 0x0

    const/4 v5, 0x0

    const-wide/16 v6, 0x0

    const/4 v8, 0x0

    const/4 v9, 0x0

    const/4 v10, 0x0

    const-wide/16 v11, 0x0

    const/4 v13, 0x0

    const/4 v14, 0x0

    const-wide/16 v15, 0x0

    const/16 v17, 0x0

    const/16 v18, 0x0

    const/16 v19, 0x0

    const/16 v20, 0x0

    const/16 v21, 0x0

    const/16 v24, 0x36

    const/16 v25, 0x0

    const v26, 0x1fffc

    move-object/from16 v23, p3

    invoke-static/range {v1 .. v26}, Landroidx/compose/material3/TextKt;->Text-Nvy7gAk(Ljava/lang/String;Landroidx/compose/ui/Modifier;JLandroidx/compose/foundation/text/TextAutoSize;JLandroidx/compose/ui/text/font/FontStyle;Landroidx/compose/ui/text/font/FontWeight;Landroidx/compose/ui/text/font/FontFamily;JLandroidx/compose/ui/text/style/TextDecoration;Landroidx/compose/ui/text/style/TextAlign;JIZIILkotlin/jvm/functions/Function1;Landroidx/compose/ui/text/TextStyle;Landroidx/compose/runtime/Composer;III)V

    move-object/from16 v9, v23

    invoke-interface {v9}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    goto/16 :goto_1ec

    .line 50
    :cond_d9
    instance-of v1, v0, Lcom/cooper/lithic/feature/settings/PhotoStorageUiState$Unavailable;

    if-eqz v1, :cond_11a

    const v1, 0xa50ed44

    invoke-interface {v9, v1}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v1, "49@2811L362"

    invoke-static {v9, v1}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    move-object v1, v0

    check-cast v1, Lcom/cooper/lithic/feature/settings/PhotoStorageUiState$Unavailable;

    invoke-virtual {v1}, Lcom/cooper/lithic/feature/settings/PhotoStorageUiState$Unavailable;->getReason()Lcom/cooper/lithic/data/media/PhotoStorageReadFailure;

    move-result-object v1

    sget-object v2, Lcom/cooper/lithic/feature/settings/DataConfidenceSectionsKt$WhenMappings;->$EnumSwitchMapping$0:[I

    invoke-virtual {v1}, Lcom/cooper/lithic/data/media/PhotoStorageReadFailure;->ordinal()I

    move-result v1

    aget v1, v2, v1

    packed-switch v1, :pswitch_data_308

    new-instance v1, Lkotlin/NoWhenBranchMatchedException;

    invoke-direct {v1}, Lkotlin/NoWhenBranchMatchedException;-><init>()V

    throw v1

    .line 53
    :pswitch_100
    const-string v1, "\u7167\u7247\u767b\u8bb0\u4fe1\u606f\u6682\u65f6\u65e0\u6cd5\u8bfb\u53d6\uff0c\u4e0d\u80fd\u636e\u6b64\u5224\u65ad\u7167\u7247\u6570\u91cf\u3002"

    goto :goto_108

    .line 52
    :pswitch_103
    const-string v1, "\u7167\u7247\u767b\u8bb0\u6216\u5f15\u7528\u4fe1\u606f\u672a\u80fd\u4e00\u81f4\u6838\u5bf9\uff0c\u6682\u4e0d\u663e\u793a\u6570\u91cf\u3002"

    goto :goto_108

    .line 51
    :pswitch_106
    const-string v1, "\u5f53\u524d\u8bb0\u5f55\u5df2\u5207\u6362\u6216\u5c1a\u672a\u5c31\u7eea\uff0c\u7167\u7247\u7edf\u8ba1\u6682\u4e0d\u53ef\u7528\u3002"

    .line 54
    :goto_108
    sget-object v2, Lcom/cooper/lithic/core/design/UiMessageTone;->WARNING:Lcom/cooper/lithic/core/design/UiMessageTone;

    .line 50
    const-string v3, "photo-storage-unavailable"

    const/4 v4, 0x0

    const/16 v6, 0x1b0

    const/16 v7, 0x8

    move-object v5, v9

    invoke-static/range {v1 .. v7}, Lcom/cooper/lithic/core/design/UiFeedbackTextKt;->UiFeedbackText(Ljava/lang/String;Lcom/cooper/lithic/core/design/UiMessageTone;Ljava/lang/String;Landroidx/compose/ui/Modifier;Landroidx/compose/runtime/Composer;II)V

    invoke-interface {v9}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    goto/16 :goto_1ec

    .line 55
    :cond_11a
    instance-of v1, v0, Lcom/cooper/lithic/feature/settings/PhotoStorageUiState$Ready;

    if-eqz v1, :cond_2f2

    const v1, 0x3fd31bd4

    invoke-interface {v9, v1}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v1, "56@3274L117,57@3408L174,59@3599L118,60@3734L153,62@3904L120"

    invoke-static {v9, v1}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    .line 56
    move-object v1, v0

    check-cast v1, Lcom/cooper/lithic/feature/settings/PhotoStorageUiState$Ready;

    invoke-virtual {v1}, Lcom/cooper/lithic/feature/settings/PhotoStorageUiState$Ready;->getSummary()Lcom/cooper/lithic/data/media/PhotoStorageSummary;

    move-result-object v12

    .line 57
    .local v12, "s":Lcom/cooper/lithic/data/media/PhotoStorageSummary;
    sget-object v2, Lcom/cooper/lithic/core/design/C2Glyph;->Project:Lcom/cooper/lithic/core/design/C2Glyph;

    invoke-virtual {v12}, Lcom/cooper/lithic/data/media/PhotoStorageSummary;->getRegisteredCount()J

    move-result-wide v3

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v1, v3, v4}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    move-result-object v1

    const-string v13, " \u5f20"

    invoke-virtual {v1, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    const v10, 0xc00036

    const/16 v11, 0x6c

    const-string v1, "\u5df2\u767b\u8bb0\u7167\u7247"

    const/4 v3, 0x0

    const/4 v4, 0x0

    const/4 v6, 0x0

    const/4 v7, 0x0

    const-string v8, "photo-storage-registered"

    invoke-static/range {v1 .. v11}, Lcom/cooper/lithic/core/design/C2PageKt;->C2SettingRow(Ljava/lang/String;Lcom/cooper/lithic/core/design/C2Glyph;Landroidx/compose/ui/Modifier;Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;Landroidx/compose/runtime/Composer;II)V

    .line 58
    sget-object v2, Lcom/cooper/lithic/core/design/C2Glyph;->Project:Lcom/cooper/lithic/core/design/C2Glyph;

    .line 59
    invoke-virtual {v12}, Lcom/cooper/lithic/data/media/PhotoStorageSummary;->getReferencedCount()J

    move-result-wide v3

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v1, v3, v4}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    .line 58
    const-string v1, "\u5f53\u524d\u8bb0\u5f55\u5f15\u7528"

    const/4 v3, 0x0

    const-string v4, "\u7269\u54c1\u4e0e\u8ba2\u9605\uff0c\u5305\u542b\u5386\u53f2\u8bb0\u5f55\uff1b\u76f8\u540c\u7167\u7247\u53ea\u8ba1\u4e00\u6b21"

    const-string v8, "photo-storage-referenced"

    const v10, 0xc00c36

    const/16 v11, 0x64

    move-object/from16 v9, p3

    invoke-static/range {v1 .. v11}, Lcom/cooper/lithic/core/design/C2PageKt;->C2SettingRow(Ljava/lang/String;Lcom/cooper/lithic/core/design/C2Glyph;Landroidx/compose/ui/Modifier;Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;Landroidx/compose/runtime/Composer;II)V

    .line 60
    sget-object v2, Lcom/cooper/lithic/core/design/C2Glyph;->Review:Lcom/cooper/lithic/core/design/C2Glyph;

    invoke-virtual {v12}, Lcom/cooper/lithic/data/media/PhotoStorageSummary;->getHistoricalCount()J

    move-result-wide v3

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v1, v3, v4}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    const v10, 0xc00036

    const/16 v11, 0x6c

    const-string v1, "\u5386\u53f2\u672a\u5f15\u7528\u7167\u7247"

    const/4 v3, 0x0

    const/4 v4, 0x0

    const-string v8, "photo-storage-historical"

    invoke-static/range {v1 .. v11}, Lcom/cooper/lithic/core/design/C2PageKt;->C2SettingRow(Ljava/lang/String;Lcom/cooper/lithic/core/design/C2Glyph;Landroidx/compose/ui/Modifier;Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;Landroidx/compose/runtime/Composer;II)V

    .line 61
    sget-object v2, Lcom/cooper/lithic/core/design/C2Glyph;->Info:Lcom/cooper/lithic/core/design/C2Glyph;

    invoke-virtual {v12}, Lcom/cooper/lithic/data/media/PhotoStorageSummary;->getEstimatedHistoricalBytes()J

    move-result-wide v3

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v1, v3, v4}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    move-result-object v1

    const-string v13, " \u5b57\u8282"

    invoke-virtual {v1, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    .line 62
    nop

    .line 61
    const-string v1, "\u5386\u53f2\u7167\u7247\u767b\u8bb0\u4f53\u79ef"

    const/4 v3, 0x0

    const/4 v4, 0x0

    const-string v8, "photo-storage-historical-bytes"

    invoke-static/range {v1 .. v11}, Lcom/cooper/lithic/core/design/C2PageKt;->C2SettingRow(Ljava/lang/String;Lcom/cooper/lithic/core/design/C2Glyph;Landroidx/compose/ui/Modifier;Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;Landroidx/compose/runtime/Composer;II)V

    .line 63
    sget-object v2, Lcom/cooper/lithic/core/design/C2Glyph;->Info:Lcom/cooper/lithic/core/design/C2Glyph;

    invoke-virtual {v12}, Lcom/cooper/lithic/data/media/PhotoStorageSummary;->getEstimatedRegisteredBytes()J

    move-result-wide v3

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v1, v3, v4}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    const-string v1, "\u9884\u8ba1\u767b\u8bb0\u4f53\u79ef"

    const/4 v3, 0x0

    const/4 v4, 0x0

    const-string v8, "photo-storage-bytes"

    invoke-static/range {v1 .. v11}, Lcom/cooper/lithic/core/design/C2PageKt;->C2SettingRow(Ljava/lang/String;Lcom/cooper/lithic/core/design/C2Glyph;Landroidx/compose/ui/Modifier;Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;Landroidx/compose/runtime/Composer;II)V

    .line 55
    .end local v12    # "s":Lcom/cooper/lithic/data/media/PhotoStorageSummary;
    invoke-interface {v9}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 67
    :goto_1ec
    sget-object v1, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v1, Landroidx/compose/ui/Modifier;

    const-string v2, "photo-storage-estimate-note"

    invoke-static {v1, v2}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v2

    sget-object v1, Landroidx/compose/material3/MaterialTheme;->INSTANCE:Landroidx/compose/material3/MaterialTheme;

    sget v3, Landroidx/compose/material3/MaterialTheme;->$stable:I

    invoke-virtual {v1, v9, v3}, Landroidx/compose/material3/MaterialTheme;->getTypography(Landroidx/compose/runtime/Composer;I)Landroidx/compose/material3/Typography;

    move-result-object v1

    invoke-virtual {v1}, Landroidx/compose/material3/Typography;->getBodyMedium()Landroidx/compose/ui/text/TextStyle;

    move-result-object v22

    .line 68
    sget-object v1, Landroidx/compose/material3/MaterialTheme;->INSTANCE:Landroidx/compose/material3/MaterialTheme;

    sget v3, Landroidx/compose/material3/MaterialTheme;->$stable:I

    invoke-virtual {v1, v9, v3}, Landroidx/compose/material3/MaterialTheme;->getColorScheme(Landroidx/compose/runtime/Composer;I)Landroidx/compose/material3/ColorScheme;

    move-result-object v1

    invoke-virtual {v1}, Landroidx/compose/material3/ColorScheme;->getOnSurfaceVariant-0d7_KjU()J

    move-result-wide v3

    .line 66
    nop

    .line 67
    nop

    .line 68
    nop

    .line 67
    nop

    .line 66
    const/4 v5, 0x0

    const-wide/16 v6, 0x0

    const/4 v8, 0x0

    const/4 v9, 0x0

    const/4 v10, 0x0

    const-wide/16 v11, 0x0

    const/4 v13, 0x0

    const/4 v14, 0x0

    const-wide/16 v15, 0x0

    const/16 v17, 0x0

    const/16 v18, 0x0

    const/16 v19, 0x0

    const/16 v20, 0x0

    const/16 v21, 0x0

    const/16 v24, 0x36

    const/16 v25, 0x0

    const v26, 0x1fff8

    const-string v1, "\u6309\u5df2\u767b\u8bb0\u5a92\u4f53\u957f\u5ea6\u4f30\u7b97\uff0c\u4e0d\u7b49\u4e8e\u5b9e\u9645\u78c1\u76d8\u5360\u7528\uff1b\u4e0d\u542b\u5185\u90e8\u68c0\u67e5\u4e34\u65f6\u526f\u672c\u6216\u5916\u90e8\u5907\u4efd\uff0c\u4e5f\u4e0d\u4ee3\u8868\u7167\u7247\u6587\u4ef6\u5df2\u9010\u4e00\u9a8c\u8bc1\u3002"

    move-object/from16 v23, p3

    invoke-static/range {v1 .. v26}, Landroidx/compose/material3/TextKt;->Text-Nvy7gAk(Ljava/lang/String;Landroidx/compose/ui/Modifier;JLandroidx/compose/foundation/text/TextAutoSize;JLandroidx/compose/ui/text/font/FontStyle;Landroidx/compose/ui/text/font/FontWeight;Landroidx/compose/ui/text/font/FontFamily;JLandroidx/compose/ui/text/style/TextDecoration;Landroidx/compose/ui/text/style/TextAlign;JIZIILkotlin/jvm/functions/Function1;Landroidx/compose/ui/text/TextStyle;Landroidx/compose/runtime/Composer;III)V

    .line 70
    move-object/from16 v9, v23

    sget-object v1, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v1, Landroidx/compose/ui/Modifier;

    const-string v2, "photo-storage-retention-note"

    invoke-static {v1, v2}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v2

    sget-object v1, Landroidx/compose/material3/MaterialTheme;->INSTANCE:Landroidx/compose/material3/MaterialTheme;

    sget v3, Landroidx/compose/material3/MaterialTheme;->$stable:I

    invoke-virtual {v1, v9, v3}, Landroidx/compose/material3/MaterialTheme;->getTypography(Landroidx/compose/runtime/Composer;I)Landroidx/compose/material3/Typography;

    move-result-object v1

    invoke-virtual {v1}, Landroidx/compose/material3/Typography;->getBodyMedium()Landroidx/compose/ui/text/TextStyle;

    move-result-object v22

    .line 71
    sget-object v1, Landroidx/compose/material3/MaterialTheme;->INSTANCE:Landroidx/compose/material3/MaterialTheme;

    sget v3, Landroidx/compose/material3/MaterialTheme;->$stable:I

    invoke-virtual {v1, v9, v3}, Landroidx/compose/material3/MaterialTheme;->getColorScheme(Landroidx/compose/runtime/Composer;I)Landroidx/compose/material3/ColorScheme;

    move-result-object v1

    invoke-virtual {v1}, Landroidx/compose/material3/ColorScheme;->getOnSurfaceVariant-0d7_KjU()J

    move-result-wide v3

    .line 69
    nop

    .line 70
    nop

    .line 71
    nop

    .line 70
    nop

    .line 69
    const/4 v9, 0x0

    const-string v1, "\u53d6\u6d88\u5c01\u9762\u663e\u793a\u4e0d\u4f1a\u7acb\u5373\u5220\u9664\u65e7\u7167\u7247\u3002\u5386\u53f2\u7167\u7247\u4ecd\u53ef\u80fd\u5360\u7528\u7a7a\u95f4\uff0c\u5e76\u53c2\u52a0\u5b8c\u6574\u5907\u4efd\u3002"

    invoke-static/range {v1 .. v26}, Landroidx/compose/material3/TextKt;->Text-Nvy7gAk(Ljava/lang/String;Landroidx/compose/ui/Modifier;JLandroidx/compose/foundation/text/TextAutoSize;JLandroidx/compose/ui/text/font/FontStyle;Landroidx/compose/ui/text/font/FontWeight;Landroidx/compose/ui/text/font/FontFamily;JLandroidx/compose/ui/text/style/TextDecoration;Landroidx/compose/ui/text/style/TextAlign;JIZIILkotlin/jvm/functions/Function1;Landroidx/compose/ui/text/TextStyle;Landroidx/compose/runtime/Composer;III)V

    .line 73
    move-object/from16 v9, v23

    sget-object v1, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v1, Landroidx/compose/ui/Modifier;

    const-string v2, "photo-storage-cleanup-unavailable"

    invoke-static {v1, v2}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v2

    sget-object v1, Landroidx/compose/material3/MaterialTheme;->INSTANCE:Landroidx/compose/material3/MaterialTheme;

    sget v3, Landroidx/compose/material3/MaterialTheme;->$stable:I

    invoke-virtual {v1, v9, v3}, Landroidx/compose/material3/MaterialTheme;->getTypography(Landroidx/compose/runtime/Composer;I)Landroidx/compose/material3/Typography;

    move-result-object v1

    invoke-virtual {v1}, Landroidx/compose/material3/Typography;->getBodyMedium()Landroidx/compose/ui/text/TextStyle;

    move-result-object v22

    .line 74
    sget-object v1, Landroidx/compose/material3/MaterialTheme;->INSTANCE:Landroidx/compose/material3/MaterialTheme;

    sget v3, Landroidx/compose/material3/MaterialTheme;->$stable:I

    invoke-virtual {v1, v9, v3}, Landroidx/compose/material3/MaterialTheme;->getColorScheme(Landroidx/compose/runtime/Composer;I)Landroidx/compose/material3/ColorScheme;

    move-result-object v1

    invoke-virtual {v1}, Landroidx/compose/material3/ColorScheme;->getOnSurfaceVariant-0d7_KjU()J

    move-result-wide v3

    .line 72
    nop

    .line 73
    nop

    .line 74
    nop

    .line 73
    nop

    .line 72
    const/4 v9, 0x0

    const-string v1, "\u5386\u53f2\u7167\u7247\u7684\u767b\u8bb0\u4f53\u79ef\u4ec5\u4f9b\u4f30\u7b97\uff0c\u4e0d\u4ee3\u8868\u5b9e\u9645\u53ef\u91ca\u653e\u7a7a\u95f4\u3002\u6682\u4e0d\u652f\u6301\u9010\u5f20\u67e5\u770b\uff1b\u4e3a\u4fdd\u62a4\u5907\u4efd\uff0c\u624b\u52a8\u6e05\u7406\u6682\u4e0d\u53ef\u7528\u3002"

    invoke-static/range {v1 .. v26}, Landroidx/compose/material3/TextKt;->Text-Nvy7gAk(Ljava/lang/String;Landroidx/compose/ui/Modifier;JLandroidx/compose/foundation/text/TextAutoSize;JLandroidx/compose/ui/text/font/FontStyle;Landroidx/compose/ui/text/font/FontWeight;Landroidx/compose/ui/text/font/FontFamily;JLandroidx/compose/ui/text/style/TextDecoration;Landroidx/compose/ui/text/style/TextAlign;JIZIILkotlin/jvm/functions/Function1;Landroidx/compose/ui/text/TextStyle;Landroidx/compose/runtime/Composer;III)V

    .line 75
    move-object/from16 v9, v23

    if-eqz p1, :cond_2df

    const v1, 0xa51e323

    invoke-interface {v9, v1}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v1, "74@4784L169"

    invoke-static {v9, v1}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    sget-object v1, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v1, Landroidx/compose/ui/Modifier;

    const/16 v2, 0x30

    .local v2, "$this$dp\\1":I
    const/4 v3, 0x0

    .line 123
    .local v3, "$i$f$getDp\\1\\75":I
    int-to-float v4, v2

    invoke-static {v4}, Landroidx/compose/ui/unit/Dp;->constructor-impl(F)F

    move-result v2

    .line 75
    .end local v2    # "$this$dp\\1":I
    .end local v3    # "$i$f$getDp\\1\\75":I
    const/4 v3, 0x2

    const/4 v4, 0x0

    const/4 v5, 0x0

    invoke-static {v1, v2, v5, v3, v4}, Landroidx/compose/foundation/layout/SizeKt;->heightIn-VpY3zN4$default(Landroidx/compose/ui/Modifier;FFILjava/lang/Object;)Landroidx/compose/ui/Modifier;

    move-result-object v1

    const-string v2, "refresh-photo-storage"

    invoke-static {v1, v2}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v2

    .line 76
    sget-object v1, Lcom/cooper/lithic/feature/settings/PhotoStorageUiState$Loading;->INSTANCE:Lcom/cooper/lithic/feature/settings/PhotoStorageUiState$Loading;

    invoke-static {v0, v1}, Lkotlin/jvm/internal/Intrinsics;->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v1

    xor-int/lit8 v3, v1, 0x1

    sget-object v1, Lcom/cooper/lithic/feature/settings/ComposableSingletons$DataConfidenceSectionsKt;->INSTANCE:Lcom/cooper/lithic/feature/settings/ComposableSingletons$DataConfidenceSectionsKt;

    invoke-virtual {v1}, Lcom/cooper/lithic/feature/settings/ComposableSingletons$DataConfidenceSectionsKt;->getLambda$-654582339$app_debug()Lkotlin/jvm/functions/Function3;

    move-result-object v10

    .line 75
    const/4 v4, 0x0

    const/4 v5, 0x0

    const/4 v6, 0x0

    const/4 v7, 0x0

    const/4 v8, 0x0

    const/4 v9, 0x0

    const v12, 0x30000030

    const/16 v13, 0x1f8

    move-object/from16 v1, p1

    move-object/from16 v11, p3

    invoke-static/range {v1 .. v13}, Lcom/cooper/lithic/core/design/LithicControlsKt;->LithicTextButton(Lkotlin/jvm/functions/Function0;Landroidx/compose/ui/Modifier;ZLandroidx/compose/ui/graphics/Shape;Landroidx/compose/material3/ButtonColors;Landroidx/compose/material3/ButtonElevation;Landroidx/compose/foundation/BorderStroke;Landroidx/compose/foundation/layout/PaddingValues;Landroidx/compose/foundation/interaction/MutableInteractionSource;Lkotlin/jvm/functions/Function3;Landroidx/compose/runtime/Composer;II)V

    move-object v9, v11

    invoke-interface {v9}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    goto :goto_2e8

    .line 76
    :cond_2df
    const v1, 0x3fecfba8

    invoke-interface {v9, v1}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    invoke-interface {v9}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    :goto_2e8
    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v1

    if-eqz v1, :cond_304

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->traceEventEnd()V

    goto :goto_304

    .line 46
    :cond_2f2
    const v1, 0xa50c7e0

    invoke-interface {v9, v1}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    .line 65
    invoke-interface {v9}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 46
    new-instance v1, Lkotlin/NoWhenBranchMatchedException;

    invoke-direct {v1}, Lkotlin/NoWhenBranchMatchedException;-><init>()V

    throw v1

    .line 45
    :cond_301
    invoke-interface {v9}, Landroidx/compose/runtime/Composer;->skipToGroupEnd()V

    .line 77
    :cond_304
    :goto_304
    sget-object v1, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v1

    nop

    :pswitch_data_308
    .packed-switch 0x1
        :pswitch_106
        :pswitch_103
        :pswitch_100
    .end packed-switch
.end method
