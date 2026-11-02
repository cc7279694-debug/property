
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
    invoke-interface/range {p3 .. p3}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;

    .line 131
    invoke-interface {p1}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;

    move-result-object v0

    move-object v1, v0

    check-cast v1, Ljava/io/Closeable;

    :try_start_a
    move-object v0, v1

    check-cast v0, Ljava/io/InputStream;

    move-object v2, v0

    .local v2, "input\\1":Ljava/io/InputStream;
    const/4 v3, 0x0

    .line 132
    .local v3, "$i$a$-use-BackupInspectionService$copySelected$1\\1\\131\\0":I
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
    :try_end_21
    .catchall {:try_start_a .. :try_end_21} :catchall_7d

    move-object/from16 v4, p2

    :try_start_23
    invoke-static {v4, v0}, Ljava/nio/file/Files;->newOutputStream(Ljava/nio/file/Path;[Ljava/nio/file/OpenOption;)Ljava/io/OutputStream;

    move-result-object v0

    move-object v6, v0

    check-cast v6, Ljava/io/Closeable;
    :try_end_2a
    .catchall {:try_start_23 .. :try_end_2a} :catchall_7b

    :try_start_2a
    move-object v0, v6

    check-cast v0, Ljava/io/OutputStream;

    .local v0, "output\\2":Ljava/io/OutputStream;
    const/4 v7, 0x0

    .line 133
    .local v7, "$i$a$-use-BackupInspectionService$copySelected$1$1\\2\\132\\1":I
    const/high16 v8, 0x10000

    new-array v8, v8, [B

    .line 134
    .local v8, "buffer\\2":[B
    const-wide/16 v9, 0x0

    .line 135
    .local v9, "length\\2":J
    :goto_34
    nop

    .line 136
    invoke-interface/range {p3 .. p3}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;

    .line 137
    invoke-virtual {v2, v8}, Ljava/io/InputStream;->read([B)I

    move-result v11

    .line 138
    .local v11, "count\\2":I
    if-ltz v11, :cond_5f

    .line 139
    if-eqz v11, :cond_57

    .line 140
    int-to-long v12, v11

    invoke-static {v9, v10, v12, v13}, Ljava/lang/Math;->addExact(JJ)J

    move-result-wide v12

    move-wide v9, v12

    .line 141
    const-wide/32 v12, 0x40000000

    cmp-long v12, v9, v12

    if-gez v12, :cond_51

    .line 142
    invoke-virtual {v0, v8, v5, v11}, Ljava/io/OutputStream;->write([BII)V

    .end local v11    # "count\\2":I
    goto :goto_34

    .line 141
    .restart local v11    # "count\\2":I
    :cond_51
    new-instance v5, Lcom/cooper/lithic/data/backup/BackupInspectionService$InspectionUnavailable;

    invoke-direct {v5}, Lcom/cooper/lithic/data/backup/BackupInspectionService$InspectionUnavailable;-><init>()V

    .end local v2    # "input\\1":Ljava/io/InputStream;
    .end local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\131\\0":I
    .end local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .end local p2    # "source":Ljava/nio/file/Path;
    .end local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    throw v5

    .line 139
    .restart local v2    # "input\\1":Ljava/io/InputStream;
    .restart local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\131\\0":I
    .restart local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .restart local p2    # "source":Ljava/nio/file/Path;
    .restart local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :cond_57
    new-instance v5, Ljava/io/IOException;

    const-string v12, "Selected document made no progress"

    invoke-direct {v5, v12}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    .end local v2    # "input\\1":Ljava/io/InputStream;
    .end local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\131\\0":I
    .end local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .end local p2    # "source":Ljava/nio/file/Path;
    .end local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    throw v5

    .line 144
    .end local v11    # "count\\2":I
    .restart local v2    # "input\\1":Ljava/io/InputStream;
    .restart local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\131\\0":I
    .restart local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .restart local p2    # "source":Ljava/nio/file/Path;
    .restart local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :cond_5f
    invoke-virtual {v0}, Ljava/io/OutputStream;->flush()V

    .line 145
    nop

    .end local v0    # "output\\2":Ljava/io/OutputStream;
    .end local v7    # "$i$a$-use-BackupInspectionService$copySelected$1$1\\2\\132\\1":I
    .end local v8    # "buffer\\2":[B
    .end local v9    # "length\\2":J
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
    :try_end_65
    .catchall {:try_start_2a .. :try_end_65} :catchall_73

    .line 132
    const/4 v0, 0x0

    :try_start_66
    invoke-static {v6, v0}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    .line 146
    nop

    .end local v2    # "input\\1":Ljava/io/InputStream;
    .end local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\131\\0":I
    sget-object v2, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
    :try_end_6c
    .catchall {:try_start_66 .. :try_end_6c} :catchall_7b

    .line 131
    invoke-static {v1, v0}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    .line 147
    invoke-interface/range {p3 .. p3}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;

    .line 148
    return-void

    .line 132
    .restart local v2    # "input\\1":Ljava/io/InputStream;
    .restart local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\131\\0":I
    :catchall_73
    move-exception v0

    move-object v5, v0

    .end local v2    # "input\\1":Ljava/io/InputStream;
    .end local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\131\\0":I
    .end local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .end local p2    # "source":Ljava/nio/file/Path;
    .end local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :try_start_75
    throw v5
    :try_end_76
    .catchall {:try_start_75 .. :try_end_76} :catchall_76

    .restart local v2    # "input\\1":Ljava/io/InputStream;
    .restart local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\131\\0":I
    .restart local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .restart local p2    # "source":Ljava/nio/file/Path;
    .restart local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :catchall_76
    move-exception v0

    :try_start_77
    invoke-static {v6, v5}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    .end local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .end local p2    # "source":Ljava/nio/file/Path;
    .end local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    throw v0
    :try_end_7b
    .catchall {:try_start_77 .. :try_end_7b} :catchall_7b

    .line 131
    .end local v2    # "input\\1":Ljava/io/InputStream;
    .end local v3    # "$i$a$-use-BackupInspectionService$copySelected$1\\1\\131\\0":I
    .restart local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .restart local p2    # "source":Ljava/nio/file/Path;
    .restart local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :catchall_7b
    move-exception v0

    goto :goto_80

    :catchall_7d
    move-exception v0

    move-object/from16 v4, p2

    :goto_80
    move-object v2, v0

    .end local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .end local p2    # "source":Ljava/nio/file/Path;
    .end local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :try_start_81
    throw v2
    :try_end_82
    .catchall {:try_start_81 .. :try_end_82} :catchall_82

    .restart local p1    # "openInput":Lkotlin/jvm/functions/Function0;
    .restart local p2    # "source":Ljava/nio/file/Path;
    .restart local p3    # "checkpoint":Lkotlin/jvm/functions/Function0;
    :catchall_82
    move-exception v0

    invoke-static {v1, v2}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    throw v0
.end method
