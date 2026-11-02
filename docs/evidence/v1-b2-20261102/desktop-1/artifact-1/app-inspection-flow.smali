
.method public final invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
    .registers 34
    .param p1, "$result"    # Ljava/lang/Object;

    move-object/from16 v1, p0

    iget-object v0, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->L$0:Ljava/lang/Object;

    move-object v2, v0

    check-cast v2, Lkotlin/jvm/functions/Function0;

    .local v2, "cancellationCheckpoint":Lkotlin/jvm/functions/Function0;
    invoke-static {}, Lkotlin/coroutines/intrinsics/IntrinsicsKt;->getCOROUTINE_SUSPENDED()Ljava/lang/Object;

    .line 50
    iget v0, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->label:I

    packed-switch v0, :pswitch_data_278

    new-instance v0, Ljava/lang/IllegalStateException;

    const-string v3, "call to \'resume\' before \'invoke\' with coroutine"

    invoke-direct {v0, v3}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw v0

    :pswitch_17
    invoke-static/range {p1 .. p1}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V

    .line 51
    const/4 v3, 0x0

    .line 57
    .local v3, "temporaryFilesRetained":Z
    nop

    .line 58
    :try_start_1c
    iget-object v0, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->$stillCurrent:Lkotlin/jvm/functions/Function0;

    invoke-static {v2, v0}, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->invokeSuspend$checkpoint(Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V

    .line 59
    iget-object v0, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->this$0:Lcom/cooper/lithic/data/backup/BackupInspectionService;

    invoke-static {v0}, Lcom/cooper/lithic/data/backup/BackupInspectionService;->access$createOperationDirectory(Lcom/cooper/lithic/data/backup/BackupInspectionService;)Ljava/nio/file/Path;

    move-result-object v0
    :try_end_27
    .catch Lcom/cooper/lithic/data/backup/UnsupportedBackupFormatException; {:try_start_1c .. :try_end_27} :catch_267
    .catch Lcom/cooper/lithic/data/backup/BackupMediaException; {:try_start_1c .. :try_end_27} :catch_24d
    .catch Ljava/util/concurrent/CancellationException; {:try_start_1c .. :try_end_27} :catch_23d
    .catch Lcom/cooper/lithic/data/backup/BackupInspectionService$InspectionUnavailable; {:try_start_1c .. :try_end_27} :catch_22d
    .catch Ljava/nio/charset/CharacterCodingException; {:try_start_1c .. :try_end_27} :catch_21d
    .catch Ljava/util/zip/ZipException; {:try_start_1c .. :try_end_27} :catch_20c
    .catch Lkotlinx/serialization/SerializationException; {:try_start_1c .. :try_end_27} :catch_1fb
    .catch Ljava/lang/IllegalArgumentException; {:try_start_1c .. :try_end_27} :catch_1ea
    .catch Ljava/lang/Exception; {:try_start_1c .. :try_end_27} :catch_1d9

    .line 60
    .local v0, "directory":Ljava/nio/file/Path;
    const/4 v3, 0x1

    .line 61
    :try_start_28
    new-instance v4, Lcom/cooper/lithic/data/backup/BackupInspectionService$Operation;

    iget-object v5, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->this$0:Lcom/cooper/lithic/data/backup/BackupInspectionService;

    invoke-static {v5, v0}, Lcom/cooper/lithic/data/backup/BackupInspectionService;->access$attributes(Lcom/cooper/lithic/data/backup/BackupInspectionService;Ljava/nio/file/Path;)Ljava/nio/file/attribute/BasicFileAttributes;

    move-result-object v5

    invoke-interface {v5}, Ljava/nio/file/attribute/BasicFileAttributes;->fileKey()Ljava/lang/Object;

    move-result-object v5

    invoke-static {}, Ljava/util/UUID;->randomUUID()Ljava/util/UUID;

    move-result-object v6

    invoke-virtual {v6}, Ljava/util/UUID;->toString()Ljava/lang/String;

    move-result-object v6

    const-string v7, "toString(...)"

    invoke-static {v6, v7}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-direct {v4, v0, v5, v6}, Lcom/cooper/lithic/data/backup/BackupInspectionService$Operation;-><init>(Ljava/nio/file/Path;Ljava/lang/Object;Ljava/lang/String;)V

    move-object v9, v4

    .line 63
    .local v9, "record":Lcom/cooper/lithic/data/backup/BackupInspectionService$Operation;
    const-string v4, ".inspection-owner"

    invoke-interface {v0, v4}, Ljava/nio/file/Path;->resolve(Ljava/lang/String;)Ljava/nio/file/Path;

    move-result-object v4

    invoke-virtual {v9}, Lcom/cooper/lithic/data/backup/BackupInspectionService$Operation;->getCredential()Ljava/lang/String;

    move-result-object v5

    sget-object v6, Lkotlin/text/Charsets;->UTF_8:Ljava/nio/charset/Charset;

    invoke-virtual {v5, v6}, Ljava/lang/String;->getBytes(Ljava/nio/charset/Charset;)[B

    move-result-object v5

    const-string v6, "getBytes(...)"

    invoke-static {v5, v6}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    const/4 v6, 0x3

    new-array v6, v6, [Ljava/nio/file/OpenOption;

    sget-object v7, Ljava/nio/file/StandardOpenOption;->CREATE_NEW:Ljava/nio/file/StandardOpenOption;

    const/4 v14, 0x0

    aput-object v7, v6, v14

    sget-object v7, Ljava/nio/file/StandardOpenOption;->WRITE:Ljava/nio/file/StandardOpenOption;

    const/4 v15, 0x1

    aput-object v7, v6, v15

    sget-object v7, Ljava/nio/file/LinkOption;->NOFOLLOW_LINKS:Ljava/nio/file/LinkOption;

    const/4 v8, 0x2

    aput-object v7, v6, v8

    invoke-static {v4, v5, v6}, Ljava/nio/file/Files;->write(Ljava/nio/file/Path;[B[Ljava/nio/file/OpenOption;)Ljava/nio/file/Path;

    .line 64
    const-string v4, "source.lithicbackup"

    invoke-interface {v0, v4}, Ljava/nio/file/Path;->resolve(Ljava/lang/String;)Ljava/nio/file/Path;

    move-result-object v4

    .line 66
    .local v4, "source":Ljava/nio/file/Path;
    iget-object v5, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->this$0:Lcom/cooper/lithic/data/backup/BackupInspectionService;

    iget-object v6, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->$openInput:Lkotlin/jvm/functions/Function0;

    invoke-static {v4}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;)V

    new-instance v7, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3$1;

    iget-object v8, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->$stillCurrent:Lkotlin/jvm/functions/Function0;

    invoke-direct {v7, v2, v8}, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3$1;-><init>(Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V

    check-cast v7, Lkotlin/jvm/functions/Function0;

    invoke-static {v5, v6, v4, v7}, Lcom/cooper/lithic/data/backup/BackupInspectionService;->access$copySelected(Lcom/cooper/lithic/data/backup/BackupInspectionService;Lkotlin/jvm/functions/Function0;Ljava/nio/file/Path;Lkotlin/jvm/functions/Function0;)V

    .line 67
    sget-object v5, Lcom/cooper/lithic/data/backup/BackupArchiveFiles;->INSTANCE:Lcom/cooper/lithic/data/backup/BackupArchiveFiles;

    invoke-virtual {v5, v4}, Lcom/cooper/lithic/data/backup/BackupArchiveFiles;->regularArchive$app_debug(Ljava/nio/file/Path;)Ljava/nio/file/attribute/BasicFileAttributes;

    move-result-object v10

    .line 68
    .local v10, "sourceIdentity":Ljava/nio/file/attribute/BasicFileAttributes;
    const-string v5, "payload"

    invoke-interface {v0, v5}, Ljava/nio/file/Path;->resolve(Ljava/lang/String;)Ljava/nio/file/Path;

    move-result-object v5

    new-array v6, v14, [Ljava/nio/file/attribute/FileAttribute;

    invoke-static {v5, v6}, Ljava/nio/file/Files;->createDirectory(Ljava/nio/file/Path;[Ljava/nio/file/attribute/FileAttribute;)Ljava/nio/file/Path;

    move-result-object v5

    .line 69
    .local v5, "payload":Ljava/nio/file/Path;
    iget-object v6, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->this$0:Lcom/cooper/lithic/data/backup/BackupInspectionService;

    invoke-static {v5}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;)V

    invoke-static {v6, v5}, Lcom/cooper/lithic/data/backup/BackupInspectionService;->access$attributes(Lcom/cooper/lithic/data/backup/BackupInspectionService;Ljava/nio/file/Path;)Ljava/nio/file/attribute/BasicFileAttributes;

    move-result-object v6

    invoke-interface {v6}, Ljava/nio/file/attribute/BasicFileAttributes;->fileKey()Ljava/lang/Object;

    move-result-object v11

    .line 70
    .local v11, "payloadKey":Ljava/lang/Object;
    new-instance v6, Lcom/cooper/lithic/data/backup/BackupValidator;

    invoke-direct {v6}, Lcom/cooper/lithic/data/backup/BackupValidator;-><init>()V

    new-instance v7, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3$checked$1;

    iget-object v8, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->$stillCurrent:Lkotlin/jvm/functions/Function0;

    invoke-direct {v7, v2, v8}, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3$checked$1;-><init>(Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V

    check-cast v7, Lkotlin/jvm/functions/Function0;

    invoke-virtual {v6, v4, v5, v7}, Lcom/cooper/lithic/data/backup/BackupValidator;->validate(Ljava/nio/file/Path;Ljava/nio/file/Path;Lkotlin/jvm/functions/Function0;)Lcom/cooper/lithic/data/backup/ValidatedBackup;

    move-result-object v12

    .line 71
    .local v12, "checked":Lcom/cooper/lithic/data/backup/ValidatedBackup;
    iget-object v6, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->this$0:Lcom/cooper/lithic/data/backup/BackupInspectionService;

    new-instance v7, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3$2;

    iget-object v8, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->$stillCurrent:Lkotlin/jvm/functions/Function0;

    invoke-direct {v7, v2, v8}, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3$2;-><init>(Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V

    check-cast v7, Lkotlin/jvm/functions/Function0;

    invoke-static {v6, v12, v7}, Lcom/cooper/lithic/data/backup/BackupInspectionService;->access$inspectMedia(Lcom/cooper/lithic/data/backup/BackupInspectionService;Lcom/cooper/lithic/data/backup/ValidatedBackup;Lkotlin/jvm/functions/Function0;)V

    .line 72
    iget-object v6, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->$stillCurrent:Lkotlin/jvm/functions/Function0;

    invoke-static {v2, v6}, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->invokeSuspend$checkpoint(Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V

    .line 73
    nop

    .line 74
    invoke-virtual {v12}, Lcom/cooper/lithic/data/backup/ValidatedBackup;->getManifest()Lcom/cooper/lithic/data/backup/BackupManifestV1;

    move-result-object v6

    invoke-virtual {v6}, Lcom/cooper/lithic/data/backup/BackupManifestV1;->getFormatVersion()I

    move-result v17

    invoke-virtual {v12}, Lcom/cooper/lithic/data/backup/ValidatedBackup;->getManifest()Lcom/cooper/lithic/data/backup/BackupManifestV1;

    move-result-object v6

    invoke-virtual {v6}, Lcom/cooper/lithic/data/backup/BackupManifestV1;->getCreatedAt()Ljava/lang/String;

    move-result-object v18

    .line 75
    invoke-virtual {v12}, Lcom/cooper/lithic/data/backup/ValidatedBackup;->getSnapshot()Lcom/cooper/lithic/data/backup/BackupSnapshot;

    move-result-object v6

    invoke-virtual {v6}, Lcom/cooper/lithic/data/backup/BackupSnapshot;->completeRecordCounts()Ljava/util/Map;

    move-result-object v6

    invoke-static {v6}, Lkotlin/collections/MapsKt;->toMap(Ljava/util/Map;)Ljava/util/Map;

    move-result-object v19

    .line 76
    invoke-virtual {v12}, Lcom/cooper/lithic/data/backup/ValidatedBackup;->getSnapshot()Lcom/cooper/lithic/data/backup/BackupSnapshot;

    move-result-object v6

    invoke-virtual {v6}, Lcom/cooper/lithic/data/backup/BackupSnapshot;->getData()Lcom/cooper/lithic/data/backup/BackupDataV2;

    move-result-object v6

    invoke-virtual {v6}, Lcom/cooper/lithic/data/backup/BackupDataV2;->getMediaAssets()Ljava/util/List;

    move-result-object v6

    invoke-interface {v6}, Ljava/util/List;->size()I

    move-result v6

    int-to-long v6, v6

    .line 77
    invoke-virtual {v12}, Lcom/cooper/lithic/data/backup/ValidatedBackup;->getSnapshot()Lcom/cooper/lithic/data/backup/BackupSnapshot;

    move-result-object v8

    invoke-virtual {v8}, Lcom/cooper/lithic/data/backup/BackupSnapshot;->getData()Lcom/cooper/lithic/data/backup/BackupDataV2;

    move-result-object v8

    invoke-virtual {v8}, Lcom/cooper/lithic/data/backup/BackupDataV2;->getMediaAssets()Ljava/util/List;

    move-result-object v8

    check-cast v8, Ljava/lang/Iterable;

    const-wide/16 v20, 0x0

    .local v8, "$this$fold\\1":Ljava/lang/Iterable;
    .local v20, "initial\\1":J
    const/4 v13, 0x0

    .line 277
    .local v13, "$i$f$fold\\1\\77":I
    move-wide/from16 v22, v20

    .line 278
    .local v22, "accumulator\\1":J
    invoke-interface {v8}, Ljava/lang/Iterable;->iterator()Ljava/util/Iterator;

    move-result-object v16

    :goto_111
    invoke-interface/range {v16 .. v16}, Ljava/util/Iterator;->hasNext()Z

    move-result v24

    if-eqz v24, :cond_13c

    invoke-interface/range {v16 .. v16}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v24

    .local v24, "element\\1":Ljava/lang/Object;
    move-object/from16 v25, v24

    check-cast v25, Lcom/cooper/lithic/data/backup/BackupMediaAssetV1;

    move-wide/from16 v26, v22

    .local v25, "asset\\2":Lcom/cooper/lithic/data/backup/BackupMediaAssetV1;
    .local v26, "bytes\\2":J
    move-wide/from16 v28, v26

    .end local v26    # "bytes\\2":J
    .local v28, "bytes\\2":J
    const/16 v26, 0x0

    .line 77
    .local v26, "$i$a$-fold-BackupInspectionService$inspect$3$summary$1\\2\\278\\0":I
    invoke-virtual/range {v25 .. v25}, Lcom/cooper/lithic/data/backup/BackupMediaAssetV1;->getByteLength()J

    move-result-wide v14
    :try_end_129
    .catch Lcom/cooper/lithic/data/backup/UnsupportedBackupFormatException; {:try_start_28 .. :try_end_129} :catch_1d2
    .catch Lcom/cooper/lithic/data/backup/BackupMediaException; {:try_start_28 .. :try_end_129} :catch_1cb
    .catch Ljava/util/concurrent/CancellationException; {:try_start_28 .. :try_end_129} :catch_1c4
    .catch Lcom/cooper/lithic/data/backup/BackupInspectionService$InspectionUnavailable; {:try_start_28 .. :try_end_129} :catch_1bd
    .catch Ljava/nio/charset/CharacterCodingException; {:try_start_28 .. :try_end_129} :catch_1b6
    .catch Ljava/util/zip/ZipException; {:try_start_28 .. :try_end_129} :catch_1b0
    .catch Lkotlinx/serialization/SerializationException; {:try_start_28 .. :try_end_129} :catch_1aa
    .catch Ljava/lang/IllegalArgumentException; {:try_start_28 .. :try_end_129} :catch_1a4
    .catch Ljava/lang/Exception; {:try_start_28 .. :try_end_129} :catch_19e

    move/from16 v30, v3

    move-object/from16 v31, v4

    move-wide/from16 v3, v28

    .end local v4    # "source":Ljava/nio/file/Path;
    .end local v28    # "bytes\\2":J
    .local v3, "bytes\\2":J
    .local v30, "temporaryFilesRetained":Z
    .local v31, "source":Ljava/nio/file/Path;
    :try_start_12f
    invoke-static {v3, v4, v14, v15}, Ljava/lang/Math;->addExact(JJ)J

    move-result-wide v14

    .line 278
    .end local v3    # "bytes\\2":J
    .end local v25    # "asset\\2":Lcom/cooper/lithic/data/backup/BackupMediaAssetV1;
    .end local v26    # "$i$a$-fold-BackupInspectionService$inspect$3$summary$1\\2\\278\\0":I
    move-wide/from16 v22, v14

    move/from16 v3, v30

    move-object/from16 v4, v31

    const/4 v14, 0x0

    const/4 v15, 0x1

    .end local v24    # "element\\1":Ljava/lang/Object;
    goto :goto_111

    .line 279
    .end local v30    # "temporaryFilesRetained":Z
    .end local v31    # "source":Ljava/nio/file/Path;
    .local v3, "temporaryFilesRetained":Z
    .restart local v4    # "source":Ljava/nio/file/Path;
    :cond_13c
    move/from16 v30, v3

    move-object/from16 v31, v4

    .line 73
    .end local v3    # "temporaryFilesRetained":Z
    .end local v4    # "source":Ljava/nio/file/Path;
    .end local v8    # "$this$fold\\1":Ljava/lang/Iterable;
    .end local v13    # "$i$f$fold\\1\\77":I
    .end local v20    # "initial\\1":J
    .end local v22    # "accumulator\\1":J
    .restart local v30    # "temporaryFilesRetained":Z
    .restart local v31    # "source":Ljava/nio/file/Path;
    new-instance v16, Lcom/cooper/lithic/data/backup/BackupInspectionSummary;

    move-wide/from16 v20, v6

    .restart local v22    # "accumulator\\1":J
    invoke-direct/range {v16 .. v23}, Lcom/cooper/lithic/data/backup/BackupInspectionSummary;-><init>(ILjava/lang/String;Ljava/util/Map;JJ)V

    .end local v22    # "accumulator\\1":J
    move-object/from16 v3, v16

    .line 80
    .local v3, "summary":Lcom/cooper/lithic/data/backup/BackupInspectionSummary;
    iget-object v8, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->this$0:Lcom/cooper/lithic/data/backup/BackupInspectionService;

    new-instance v4, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3$cleaned$1;

    iget-object v6, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->$stillCurrent:Lkotlin/jvm/functions/Function0;

    invoke-direct {v4, v2, v6}, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3$cleaned$1;-><init>(Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V

    move-object v13, v4

    check-cast v13, Lkotlin/jvm/functions/Function0;

    invoke-static/range {v8 .. v13}, Lcom/cooper/lithic/data/backup/BackupInspectionService;->access$cleanKnown(Lcom/cooper/lithic/data/backup/BackupInspectionService;Lcom/cooper/lithic/data/backup/BackupInspectionService$Operation;Ljava/nio/file/attribute/BasicFileAttributes;Ljava/lang/Object;Lcom/cooper/lithic/data/backup/ValidatedBackup;Lkotlin/jvm/functions/Function0;)Z

    move-result v4

    .line 81
    .local v4, "cleaned":Z
    if-eqz v4, :cond_15e

    const/4 v6, 0x0

    move/from16 v30, v6

    .line 82
    :cond_15e
    iget-object v6, v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->$stillCurrent:Lkotlin/jvm/functions/Function0;

    invoke-static {v2, v6}, Lcom/cooper/lithic/data/backup/BackupInspectionService$inspect$3;->invokeSuspend$checkpoint(Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V

    .line 83
    new-instance v6, Lcom/cooper/lithic/data/backup/BackupInspectionResult;

    sget-object v7, Lcom/cooper/lithic/data/backup/BackupInspectionStatus;->VERIFIED:Lcom/cooper/lithic/data/backup/BackupInspectionStatus;

    if-nez v4, :cond_16b

    const/4 v14, 0x1

    goto :goto_16c

    :cond_16b
    const/4 v14, 0x0

    :goto_16c
    invoke-direct {v6, v7, v3, v14}, Lcom/cooper/lithic/data/backup/BackupInspectionResult;-><init>(Lcom/cooper/lithic/data/backup/BackupInspectionStatus;Lcom/cooper/lithic/data/backup/BackupInspectionSummary;Z)V
    :try_end_16f
    .catch Lcom/cooper/lithic/data/backup/UnsupportedBackupFormatException; {:try_start_12f .. :try_end_16f} :catch_199
    .catch Lcom/cooper/lithic/data/backup/BackupMediaException; {:try_start_12f .. :try_end_16f} :catch_194
    .catch Ljava/util/concurrent/CancellationException; {:try_start_12f .. :try_end_16f} :catch_18f
    .catch Lcom/cooper/lithic/data/backup/BackupInspectionService$InspectionUnavailable; {:try_start_12f .. :try_end_16f} :catch_18a
    .catch Ljava/nio/charset/CharacterCodingException; {:try_start_12f .. :try_end_16f} :catch_185
    .catch Ljava/util/zip/ZipException; {:try_start_12f .. :try_end_16f} :catch_180
    .catch Lkotlinx/serialization/SerializationException; {:try_start_12f .. :try_end_16f} :catch_17b
    .catch Ljava/lang/IllegalArgumentException; {:try_start_12f .. :try_end_16f} :catch_176
    .catch Ljava/lang/Exception; {:try_start_12f .. :try_end_16f} :catch_171

    .end local v0    # "directory":Ljava/nio/file/Path;
    .end local v3    # "summary":Lcom/cooper/lithic/data/backup/BackupInspectionSummary;
    .end local v4    # "cleaned":Z
    .end local v5    # "payload":Ljava/nio/file/Path;
    .end local v9    # "record":Lcom/cooper/lithic/data/backup/BackupInspectionService$Operation;
    .end local v10    # "sourceIdentity":Ljava/nio/file/attribute/BasicFileAttributes;
    .end local v11    # "payloadKey":Ljava/lang/Object;
    .end local v12    # "checked":Lcom/cooper/lithic/data/backup/ValidatedBackup;
    .end local v31    # "source":Ljava/nio/file/Path;
    goto/16 :goto_276

    .line 101
    :catch_171
    move-exception v0

    move/from16 v6, v30

    goto/16 :goto_1db

    .line 99
    :catch_176
    move-exception v0

    move/from16 v6, v30

    goto/16 :goto_1ec

    .line 97
    :catch_17b
    move-exception v0

    move/from16 v6, v30

    goto/16 :goto_1fd

    .line 95
    :catch_180
    move-exception v0

    move/from16 v6, v30

    goto/16 :goto_20e

    .line 93
    :catch_185
    move-exception v0

    move/from16 v6, v30

    goto/16 :goto_21f

    .line 91
    :catch_18a
    move-exception v0

    move/from16 v6, v30

    goto/16 :goto_22f

    .line 89
    :catch_18f
    move-exception v0

    move/from16 v6, v30

    goto/16 :goto_23f

    .line 86
    :catch_194
    move-exception v0

    move/from16 v6, v30

    goto/16 :goto_24f

    .line 84
    :catch_199
    move-exception v0

    move/from16 v6, v30

    goto/16 :goto_269

    .line 101
    .end local v30    # "temporaryFilesRetained":Z
    .local v3, "temporaryFilesRetained":Z
    :catch_19e
    move-exception v0

    move/from16 v30, v3

    move/from16 v6, v30

    .end local v3    # "temporaryFilesRetained":Z
    .restart local v30    # "temporaryFilesRetained":Z
    goto :goto_1db

    .line 99
    .end local v30    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_1a4
    move-exception v0

    move/from16 v30, v3

    move/from16 v6, v30

    .end local v3    # "temporaryFilesRetained":Z
    .restart local v30    # "temporaryFilesRetained":Z
    goto :goto_1ec

    .line 97
    .end local v30    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_1aa
    move-exception v0

    move/from16 v30, v3

    move/from16 v6, v30

    .end local v3    # "temporaryFilesRetained":Z
    .restart local v30    # "temporaryFilesRetained":Z
    goto :goto_1fd

    .line 95
    .end local v30    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_1b0
    move-exception v0

    move/from16 v30, v3

    move/from16 v6, v30

    .end local v3    # "temporaryFilesRetained":Z
    .restart local v30    # "temporaryFilesRetained":Z
    goto :goto_20e

    .line 93
    .end local v30    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_1b6
    move-exception v0

    move/from16 v30, v3

    move/from16 v6, v30

    .end local v3    # "temporaryFilesRetained":Z
    .restart local v30    # "temporaryFilesRetained":Z
    goto/16 :goto_21f

    .line 91
    .end local v30    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_1bd
    move-exception v0

    move/from16 v30, v3

    move/from16 v6, v30

    .end local v3    # "temporaryFilesRetained":Z
    .restart local v30    # "temporaryFilesRetained":Z
    goto/16 :goto_22f

    .line 89
    .end local v30    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_1c4
    move-exception v0

    move/from16 v30, v3

    move/from16 v6, v30

    .end local v3    # "temporaryFilesRetained":Z
    .restart local v30    # "temporaryFilesRetained":Z
    goto/16 :goto_23f

    .line 86
    .end local v30    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_1cb
    move-exception v0

    move/from16 v30, v3

    move/from16 v6, v30

    .end local v3    # "temporaryFilesRetained":Z
    .restart local v30    # "temporaryFilesRetained":Z
    goto/16 :goto_24f

    .line 84
    .end local v30    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_1d2
    move-exception v0

    move/from16 v30, v3

    move/from16 v6, v30

    .end local v3    # "temporaryFilesRetained":Z
    .restart local v30    # "temporaryFilesRetained":Z
    goto/16 :goto_269

    .line 101
    .end local v30    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_1d9
    move-exception v0

    move v6, v3

    .line 103
    .end local v3    # "temporaryFilesRetained":Z
    .local v0, "<unused var>":Ljava/lang/Exception;
    .local v6, "temporaryFilesRetained":Z
    :goto_1db
    new-instance v3, Lcom/cooper/lithic/data/backup/BackupInspectionResult;

    sget-object v4, Lcom/cooper/lithic/data/backup/BackupInspectionStatus;->UNKNOWN:Lcom/cooper/lithic/data/backup/BackupInspectionStatus;

    const/4 v7, 0x2

    const/4 v8, 0x0

    const/4 v5, 0x0

    invoke-direct/range {v3 .. v8}, Lcom/cooper/lithic/data/backup/BackupInspectionResult;-><init>(Lcom/cooper/lithic/data/backup/BackupInspectionStatus;Lcom/cooper/lithic/data/backup/BackupInspectionSummary;ZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    move/from16 v30, v6

    move-object v6, v3

    goto/16 :goto_276

    .line 99
    .end local v0    # "<unused var>":Ljava/lang/Exception;
    .end local v6    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_1ea
    move-exception v0

    move v6, v3

    .line 100
    .end local v3    # "temporaryFilesRetained":Z
    .local v0, "<unused var>":Ljava/lang/IllegalArgumentException;
    .restart local v6    # "temporaryFilesRetained":Z
    :goto_1ec
    new-instance v3, Lcom/cooper/lithic/data/backup/BackupInspectionResult;

    sget-object v4, Lcom/cooper/lithic/data/backup/BackupInspectionStatus;->INVALID:Lcom/cooper/lithic/data/backup/BackupInspectionStatus;

    const/4 v7, 0x2

    const/4 v8, 0x0

    const/4 v5, 0x0

    invoke-direct/range {v3 .. v8}, Lcom/cooper/lithic/data/backup/BackupInspectionResult;-><init>(Lcom/cooper/lithic/data/backup/BackupInspectionStatus;Lcom/cooper/lithic/data/backup/BackupInspectionSummary;ZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    move/from16 v30, v6

    move-object v6, v3

    .end local v0    # "<unused var>":Ljava/lang/IllegalArgumentException;
    goto/16 :goto_276

    .line 97
    .end local v6    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_1fb
    move-exception v0

    move v6, v3

    .line 98
    .end local v3    # "temporaryFilesRetained":Z
    .local v0, "<unused var>":Lkotlinx/serialization/SerializationException;
    .restart local v6    # "temporaryFilesRetained":Z
    :goto_1fd
    new-instance v3, Lcom/cooper/lithic/data/backup/BackupInspectionResult;

    sget-object v4, Lcom/cooper/lithic/data/backup/BackupInspectionStatus;->INVALID:Lcom/cooper/lithic/data/backup/BackupInspectionStatus;

    const/4 v7, 0x2

    const/4 v8, 0x0

    const/4 v5, 0x0

    invoke-direct/range {v3 .. v8}, Lcom/cooper/lithic/data/backup/BackupInspectionResult;-><init>(Lcom/cooper/lithic/data/backup/BackupInspectionStatus;Lcom/cooper/lithic/data/backup/BackupInspectionSummary;ZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    move/from16 v30, v6

    move-object v6, v3

    .end local v0    # "<unused var>":Lkotlinx/serialization/SerializationException;
    goto/16 :goto_276

    .line 95
    .end local v6    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_20c
    move-exception v0

    move v6, v3

    .line 96
    .end local v3    # "temporaryFilesRetained":Z
    .local v0, "<unused var>":Ljava/util/zip/ZipException;
    .restart local v6    # "temporaryFilesRetained":Z
    :goto_20e
    new-instance v3, Lcom/cooper/lithic/data/backup/BackupInspectionResult;

    sget-object v4, Lcom/cooper/lithic/data/backup/BackupInspectionStatus;->INVALID:Lcom/cooper/lithic/data/backup/BackupInspectionStatus;

    const/4 v7, 0x2

    const/4 v8, 0x0

    const/4 v5, 0x0

    invoke-direct/range {v3 .. v8}, Lcom/cooper/lithic/data/backup/BackupInspectionResult;-><init>(Lcom/cooper/lithic/data/backup/BackupInspectionStatus;Lcom/cooper/lithic/data/backup/BackupInspectionSummary;ZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    move/from16 v30, v6

    move-object v6, v3

    .end local v0    # "<unused var>":Ljava/util/zip/ZipException;
    goto/16 :goto_276

    .line 93
    .end local v6    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_21d
    move-exception v0

    move v6, v3

    .line 94
    .end local v3    # "temporaryFilesRetained":Z
    .local v0, "<unused var>":Ljava/nio/charset/CharacterCodingException;
    .restart local v6    # "temporaryFilesRetained":Z
    :goto_21f
    new-instance v3, Lcom/cooper/lithic/data/backup/BackupInspectionResult;

    sget-object v4, Lcom/cooper/lithic/data/backup/BackupInspectionStatus;->INVALID:Lcom/cooper/lithic/data/backup/BackupInspectionStatus;

    const/4 v7, 0x2

    const/4 v8, 0x0

    const/4 v5, 0x0

    invoke-direct/range {v3 .. v8}, Lcom/cooper/lithic/data/backup/BackupInspectionResult;-><init>(Lcom/cooper/lithic/data/backup/BackupInspectionStatus;Lcom/cooper/lithic/data/backup/BackupInspectionSummary;ZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    move/from16 v30, v6

    move-object v6, v3

    .end local v0    # "<unused var>":Ljava/nio/charset/CharacterCodingException;
    goto :goto_276

    .line 91
    .end local v6    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_22d
    move-exception v0

    move v6, v3

    .line 92
    .end local v3    # "temporaryFilesRetained":Z
    .local v0, "<unused var>":Lcom/cooper/lithic/data/backup/BackupInspectionService$InspectionUnavailable;
    .restart local v6    # "temporaryFilesRetained":Z
    :goto_22f
    new-instance v3, Lcom/cooper/lithic/data/backup/BackupInspectionResult;

    sget-object v4, Lcom/cooper/lithic/data/backup/BackupInspectionStatus;->UNKNOWN:Lcom/cooper/lithic/data/backup/BackupInspectionStatus;

    const/4 v7, 0x2

    const/4 v8, 0x0

    const/4 v5, 0x0

    invoke-direct/range {v3 .. v8}, Lcom/cooper/lithic/data/backup/BackupInspectionResult;-><init>(Lcom/cooper/lithic/data/backup/BackupInspectionStatus;Lcom/cooper/lithic/data/backup/BackupInspectionSummary;ZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    move/from16 v30, v6

    move-object v6, v3

    .end local v0    # "<unused var>":Lcom/cooper/lithic/data/backup/BackupInspectionService$InspectionUnavailable;
    goto :goto_276

    .line 89
    .end local v6    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_23d
    move-exception v0

    move v6, v3

    .line 90
    .end local v3    # "temporaryFilesRetained":Z
    .local v0, "<unused var>":Ljava/util/concurrent/CancellationException;
    .restart local v6    # "temporaryFilesRetained":Z
    :goto_23f
    new-instance v3, Lcom/cooper/lithic/data/backup/BackupInspectionResult;

    sget-object v4, Lcom/cooper/lithic/data/backup/BackupInspectionStatus;->UNKNOWN:Lcom/cooper/lithic/data/backup/BackupInspectionStatus;

    const/4 v7, 0x2

    const/4 v8, 0x0

    const/4 v5, 0x0

    invoke-direct/range {v3 .. v8}, Lcom/cooper/lithic/data/backup/BackupInspectionResult;-><init>(Lcom/cooper/lithic/data/backup/BackupInspectionStatus;Lcom/cooper/lithic/data/backup/BackupInspectionSummary;ZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    move/from16 v30, v6

    move-object v6, v3

    .end local v0    # "<unused var>":Ljava/util/concurrent/CancellationException;
    goto :goto_276

    .line 86
    .end local v6    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_24d
    move-exception v0

    move v6, v3

    .line 87
    .end local v3    # "temporaryFilesRetained":Z
    .local v0, "failure":Lcom/cooper/lithic/data/backup/BackupMediaException;
    .restart local v6    # "temporaryFilesRetained":Z
    :goto_24f
    new-instance v3, Lcom/cooper/lithic/data/backup/BackupInspectionResult;

    invoke-virtual {v0}, Lcom/cooper/lithic/data/backup/BackupMediaException;->getNotCertified()Z

    move-result v4

    if-eqz v4, :cond_25a

    sget-object v4, Lcom/cooper/lithic/data/backup/BackupInspectionStatus;->UNSUPPORTED:Lcom/cooper/lithic/data/backup/BackupInspectionStatus;

    goto :goto_25c

    :cond_25a
    sget-object v4, Lcom/cooper/lithic/data/backup/BackupInspectionStatus;->INVALID:Lcom/cooper/lithic/data/backup/BackupInspectionStatus;

    .line 88
    :goto_25c
    nop

    .line 87
    const/4 v7, 0x2

    const/4 v8, 0x0

    const/4 v5, 0x0

    invoke-direct/range {v3 .. v8}, Lcom/cooper/lithic/data/backup/BackupInspectionResult;-><init>(Lcom/cooper/lithic/data/backup/BackupInspectionStatus;Lcom/cooper/lithic/data/backup/BackupInspectionSummary;ZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    move/from16 v30, v6

    move-object v6, v3

    .end local v0    # "failure":Lcom/cooper/lithic/data/backup/BackupMediaException;
    goto :goto_276

    .line 84
    .end local v6    # "temporaryFilesRetained":Z
    .restart local v3    # "temporaryFilesRetained":Z
    :catch_267
    move-exception v0

    move v6, v3

    .line 85
    .end local v3    # "temporaryFilesRetained":Z
    .local v0, "<unused var>":Lcom/cooper/lithic/data/backup/UnsupportedBackupFormatException;
    .restart local v6    # "temporaryFilesRetained":Z
    :goto_269
    new-instance v3, Lcom/cooper/lithic/data/backup/BackupInspectionResult;

    sget-object v4, Lcom/cooper/lithic/data/backup/BackupInspectionStatus;->UNSUPPORTED:Lcom/cooper/lithic/data/backup/BackupInspectionStatus;

    const/4 v7, 0x2

    const/4 v8, 0x0

    const/4 v5, 0x0

    invoke-direct/range {v3 .. v8}, Lcom/cooper/lithic/data/backup/BackupInspectionResult;-><init>(Lcom/cooper/lithic/data/backup/BackupInspectionStatus;Lcom/cooper/lithic/data/backup/BackupInspectionSummary;ZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    move/from16 v30, v6

    move-object v6, v3

    .line 104
    .end local v0    # "<unused var>":Lcom/cooper/lithic/data/backup/UnsupportedBackupFormatException;
    .end local v6    # "temporaryFilesRetained":Z
    .restart local v30    # "temporaryFilesRetained":Z
    :goto_276
    return-object v6

    nop

    :pswitch_data_278
    .packed-switch 0x0
        :pswitch_17
    .end packed-switch
.end method

