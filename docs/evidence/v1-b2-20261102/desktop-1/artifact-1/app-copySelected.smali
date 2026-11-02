
.method private final copySelected(Lkotlin/jvm/functions/Function0;Ljava/nio/file/Path;Lkotlin/jvm/functions/Function0;)V
    .registers 18
    .param p1, "openInput"    # Lkotlin/jvm/functions/Function0;
    .param p2, "source"    # Ljava/nio/file/Path;
    .param p3, "checkpoint"    # Lkotlin/jvm/functions/Function0;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Lkotlin/jvm/functions/Function0<",
            "+",
            "Ljava/io/InputStream;",
            ">;",
            "Ljava/nio/file/Path;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;)V"
        }
    .end annotation

    .line 130
    nop

    .line 131
    :try_start_1
    invoke-interface/range {p3 .. p3}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;

    .line 132
    invoke-interface {p1}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;

    move-result-object v0

    move-object v1, v0

    check-cast v1, Ljava/io/Closeable;
    :try_end_b
    .catch Ljava/util/concurrent/CancellationException; {:try_start_1 .. :try_end_b} :catch_95
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_b} :catch_8c

    :try_start_b
    move-object v0, v1

    check-cast v0, Ljava/io/InputStream;

    move-object v2, v0

    .local v2, "input\\1":Ljava/io/InputStream;
    const/4 v3, 0x0

    .line 133
    .local v3, "$i$a$-use-BackupInspectionService$copySelected$1\\1\\132\\0":I
    const/4 v0, 0x3

    new-array v0, v0, [Ljava/nio/file/OpenOption;

    sget-object v4, Ljava/nio/file/StandardOpenOption;->CREATE_NEW:Ljava/nio/file/StandardOpenOption;

    const/4 v5, 0x0

    aput-object v4, v0, v5

    sget-object v4, Ljava/nio/file/StandardOpenOption;->WRITE:Ljava/nio/file/StandardOpenOption;

    const/4 v6, 0x1

    aput-object v4, v0, v6

    sget-object v4, Ljava/nio/file/LinkOption;->NOFOLLOW_LINKS:Ljava/nio/file/LinkOption;

    const/4 v6, 0x2

    aput-object v4, v0, v6
    :try_end_22
    .catchall {:try_start_b .. :try_end_22} :catchall_7e

    move-object/from16 v4, p2

    :try_start_24
    invoke-static {v4, v0}, Ljava/nio/file/Files;->newOutputStream(Ljava/nio/file/Path;[Ljava/nio/file/OpenOption;)Ljava/io/OutputStream;

    move-result-object v0

    move-object v6, v0

    check-cast v6, Ljava/io/Closeable;
    :try_end_2b
    .catchall {:try_start_24 .. :try_end_2b} :catchall_7c

    :try_start_2b
    move-object v0, v6

    check-cast v0, Ljava/io/OutputStream;

    .local v0, "output\\2":Ljava/io/OutputStream;
    const/4 v7, 0x0

    .line 134
    .local v7, "$i$a$-use-BackupInspectionService$copySelected$1$1\\2\\133\\1":I
    const/high16 v8, 0x10000

    new-array v8, v8, [B

    .line 135
    .local v8, "buffer\\2":[B
    const-wide/16 v9, 0x0

    .line 136
    .local v9, "length\\2":J
    :goto_35
    nop

    .line 137
    invoke-interface/range {p3 .. p3}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;

    .line 138
    invoke-virtual {v2, v8}, Ljava/io/InputStream;->read([B)I

    move-result v11

    .line 139
    .local v11, "count\\2":I
    if-ltz v11, :cond_60

    .line 140
    if-eqz v11, :cond_58

    .line 141
    int-to-long v12, v11

    invoke-static {v9, v10, v12, v13}, Ljava/lang/Math;->addExact(JJ)J

    move-result-wide v12

    move-wide v9, v12

    .line 142
    const-wide/32 v12, 0x40000000

    cmp-long v12, v9, v12

    if-gez v12, :cond_52

    .line 143
    invoke-virtual {v0, v8, v5, v11}, Ljava/io/OutputStream;->write([BII)V

    .end local v11    # "count\\2":I
    goto :goto_35

    .line 142
    .restart local v11    # "count\\2":I
    :cond_52
    new-instance v5, Lcom/cooper/lithic/data/backup/BackupInspectionService$InspectionUnavailable;

    invoke-direct {v5}, Lcom/cooper/lithic/data/backup/BackupInspectionService$InspectionUnavailable;-><init>()V

    .end local v2    # "input\\1":Ljava/io/InputStream;
    .end local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\132\\0":I
    .end local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .end local p2    # "source":Ljava/nio/file/Path;
    .end local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    throw v5

    .line 140
    .restart local v2    # "input\\1":Ljava/io/InputStream;
    .restart local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\132\\0":I
    .restart local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .restart local p2    # "source":Ljava/nio/file/Path;
    .restart local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :cond_58
    new-instance v5, Ljava/io/IOException;

    const-string v12, "Selected document made no progress"

    invoke-direct {v5, v12}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    .end local v2    # "input\\1":Ljava/io/InputStream;
    .end local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\132\\0":I
    .end local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .end local p2    # "source":Ljava/nio/file/Path;
    .end local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    throw v5

    .line 145
    .end local v11    # "count\\2":I
    .restart local v2    # "input\\1":Ljava/io/InputStream;
    .restart local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\132\\0":I
    .restart local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .restart local p2    # "source":Ljava/nio/file/Path;
    .restart local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :cond_60
    invoke-virtual {v0}, Ljava/io/OutputStream;->flush()V

    .line 146
    nop

    .end local v0    # "output\\2":Ljava/io/OutputStream;
    .end local v7    # "$i$a$-use-BackupInspectionService$copySelected$1$1\\2\\133\\1":I
    .end local v8    # "buffer\\2":[B
    .end local v9    # "length\\2":J
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
    :try_end_66
    .catchall {:try_start_2b .. :try_end_66} :catchall_74

    .line 133
    const/4 v0, 0x0

    :try_start_67
    invoke-static {v6, v0}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    .line 147
    nop

    .end local v2    # "input\\1":Ljava/io/InputStream;
    .end local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\132\\0":I
    sget-object v2, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
    :try_end_6d
    .catchall {:try_start_67 .. :try_end_6d} :catchall_7c

    .line 132
    :try_start_6d
    invoke-static {v1, v0}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    .line 148
    invoke-interface/range {p3 .. p3}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;
    :try_end_73
    .catch Ljava/util/concurrent/CancellationException; {:try_start_6d .. :try_end_73} :catch_8a
    .catch Ljava/lang/Exception; {:try_start_6d .. :try_end_73} :catch_88

    .line 155
    return-void

    .line 133
    .restart local v2    # "input\\1":Ljava/io/InputStream;
    .restart local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\132\\0":I
    :catchall_74
    move-exception v0

    move-object v5, v0

    .end local v2    # "input\\1":Ljava/io/InputStream;
    .end local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\132\\0":I
    .end local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .end local p2    # "source":Ljava/nio/file/Path;
    .end local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :try_start_76
    throw v5
    :try_end_77
    .catchall {:try_start_76 .. :try_end_77} :catchall_77

    .restart local v2    # "input\\1":Ljava/io/InputStream;
    .restart local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\132\\0":I
    .restart local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .restart local p2    # "source":Ljava/nio/file/Path;
    .restart local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :catchall_77
    move-exception v0

    :try_start_78
    invoke-static {v6, v5}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    .end local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .end local p2    # "source":Ljava/nio/file/Path;
    .end local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    throw v0
    :try_end_7c
    .catchall {:try_start_78 .. :try_end_7c} :catchall_7c

    .line 132
    .end local v2    # "input\\1":Ljava/io/InputStream;
    .end local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\132\\0":I
    .restart local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .restart local p2    # "source":Ljava/nio/file/Path;
    .restart local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :catchall_7c
    move-exception v0

    goto :goto_81

    :catchall_7e
    move-exception v0

    move-object/from16 v4, p2

    :goto_81
    move-object v2, v0

    .end local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .end local p2    # "source":Ljava/nio/file/Path;
    .end local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :try_start_82
    throw v2
    :try_end_83
    .catchall {:try_start_82 .. :try_end_83} :catchall_83

    .restart local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .restart local p2    # "source":Ljava/nio/file/Path;
    .restart local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :catchall_83
    move-exception v0

    :try_start_84
    invoke-static {v1, v2}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    .end local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .end local p2    # "source":Ljava/nio/file/Path;
    .end local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    throw v0
    :try_end_88
    .catch Ljava/util/concurrent/CancellationException; {:try_start_84 .. :try_end_88} :catch_8a
    .catch Ljava/lang/Exception; {:try_start_84 .. :try_end_88} :catch_88

    .line 151
    .restart local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .restart local p2    # "source":Ljava/nio/file/Path;
    .restart local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :catch_88
    move-exception v0

    goto :goto_8f

    .line 149
    :catch_8a
    move-exception v0

    goto :goto_98

    .line 151
    :catch_8c
    move-exception v0

    move-object/from16 v4, p2

    .line 154
    .local v0, "<unused var>":Ljava/lang/Exception;
    :goto_8f
    new-instance v1, Lcom/cooper/lithic/data/backup/BackupInspectionService$InspectionUnavailable;

    invoke-direct {v1}, Lcom/cooper/lithic/data/backup/BackupInspectionService$InspectionUnavailable;-><init>()V

    throw v1

    .line 149
    .end local v0    # "<unused var>":Ljava/lang/Exception;
    :catch_95
    move-exception v0

    move-object/from16 v4, p2

    .line 150
    .local v0, "cancelled":Ljava/util/concurrent/CancellationException;
    :goto_98
    throw v0
.end method
