.class final Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;
.super Lkotlin/coroutines/jvm/internal/SuspendLambda;
.source "GenerationPhotoStorageReader.kt"

# interfaces
.implements Lkotlin/jvm/functions/Function1;


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1;->invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x18
    name = null
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Lkotlin/coroutines/jvm/internal/SuspendLambda;",
        "Lkotlin/jvm/functions/Function1<",
        "Lkotlin/coroutines/Continuation<",
        "-",
        "Lcom/cooper/lithic/data/media/PhotoStorageSummary;",
        ">;",
        "Ljava/lang/Object;",
        ">;"
    }
.end annotation

.annotation system Ldalvik/annotation/SourceDebugExtension;
    value = "SMAP\nGenerationPhotoStorageReader.kt\nKotlin\n*S Kotlin\n*F\n+ 1 GenerationPhotoStorageReader.kt\ncom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,122:1\n1#2:123\n*E\n"
.end annotation

.annotation runtime Lkotlin/Metadata;
    d1 = {
        "\u0000\u0006\n\u0000\n\u0002\u0018\u0002\u0010\u0000\u001a\u00020\u0001H\n"
    }
    d2 = {
        "<anonymous>",
        "Lcom/cooper/lithic/data/media/PhotoStorageSummary;"
    }
    k = 0x3
    mv = {
        0x2,
        0x3,
        0x0
    }
    xi = 0x30
.end annotation

.annotation runtime Lkotlin/coroutines/jvm/internal/DebugMetadata;
    c = "com.cooper.lithic.data.media.GenerationPhotoStorageReader$read$2$summary$1$1"
    f = "GenerationPhotoStorageReader.kt"
    i = {}
    l = {}
    m = "invokeSuspend"
    n = {}
    nl = {}
    s = {}
    v = 0x2
.end annotation


# instance fields
.field final synthetic $caller:Lkotlin/coroutines/CoroutineContext;

.field label:I

.field final synthetic this$0:Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;


# direct methods
.method constructor <init>(Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;Lkotlin/coroutines/CoroutineContext;Lkotlin/coroutines/Continuation;)V
    .registers 5
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;",
            "Lkotlin/coroutines/CoroutineContext;",
            "Lkotlin/coroutines/Continuation<",
            "-",
            "Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;",
            ">;)V"
        }
    .end annotation

    iput-object p1, p0, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->this$0:Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;

    iput-object p2, p0, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->$caller:Lkotlin/coroutines/CoroutineContext;

    const/4 v0, 0x1

    invoke-direct {p0, v0, p3}, Lkotlin/coroutines/jvm/internal/SuspendLambda;-><init>(ILkotlin/coroutines/Continuation;)V

    return-void
.end method


# virtual methods
.method public final create(Lkotlin/coroutines/Continuation;)Lkotlin/coroutines/Continuation;
    .registers 5
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Lkotlin/coroutines/Continuation<",
            "*>;)",
            "Lkotlin/coroutines/Continuation<",
            "Lkotlin/Unit;",
            ">;"
        }
    .end annotation

    new-instance v0, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;

    iget-object v1, p0, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->this$0:Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;

    iget-object v2, p0, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->$caller:Lkotlin/coroutines/CoroutineContext;

    invoke-direct {v0, v1, v2, p1}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;-><init>(Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;Lkotlin/coroutines/CoroutineContext;Lkotlin/coroutines/Continuation;)V

    check-cast v0, Lkotlin/coroutines/Continuation;

    return-object v0
.end method

.method public bridge synthetic invoke(Ljava/lang/Object;)Ljava/lang/Object;
    .registers 3

    check-cast p1, Lkotlin/coroutines/Continuation;

    invoke-virtual {p0, p1}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->invoke(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;

    move-result-object v0

    return-object v0
.end method

.method public final invoke(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    .registers 4
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Lkotlin/coroutines/Continuation<",
            "-",
            "Lcom/cooper/lithic/data/media/PhotoStorageSummary;",
            ">;)",
            "Ljava/lang/Object;"
        }
    .end annotation

    invoke-virtual {p0, p1}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->create(Lkotlin/coroutines/Continuation;)Lkotlin/coroutines/Continuation;

    move-result-object v0

    check-cast v0, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;

    sget-object v1, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    invoke-virtual {v0, v1}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    return-object v0
.end method

.method public final invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
    .registers 24
    .param p1, "$result"    # Ljava/lang/Object;

    move-object/from16 v1, p0

    invoke-static {}, Lkotlin/coroutines/intrinsics/IntrinsicsKt;->getCOROUTINE_SUSPENDED()Ljava/lang/Object;

    .line 42
    iget v0, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->label:I

    packed-switch v0, :pswitch_data_134

    new-instance v0, Ljava/lang/IllegalStateException;

    const-string v2, "call to \'resume\' before \'invoke\' with coroutine"

    invoke-direct {v0, v2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw v0

    :pswitch_12
    invoke-static/range {p1 .. p1}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V

    .line 43
    iget-object v0, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->$caller:Lkotlin/coroutines/CoroutineContext;

    iget-object v2, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->this$0:Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;

    invoke-static {v0, v2}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2;->access$invokeSuspend$checkpoint(Lkotlin/coroutines/CoroutineContext;Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;)V

    .line 44
    iget-object v0, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->this$0:Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;

    invoke-static {v0}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;->access$getGeneration$p(Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;)Lcom/cooper/lithic/data/backup/ManagedGeneration;

    move-result-object v0

    invoke-virtual {v0}, Lcom/cooper/lithic/data/backup/ManagedGeneration;->getDatabase()Lcom/cooper/lithic/data/local/LithicDatabase;

    move-result-object v0

    invoke-virtual {v0}, Lcom/cooper/lithic/data/local/LithicDatabase;->getOpenHelper()Landroidx/sqlite/db/SupportSQLiteOpenHelper;

    move-result-object v0

    invoke-interface {v0}, Landroidx/sqlite/db/SupportSQLiteOpenHelper;->getReadableDatabase()Landroidx/sqlite/db/SupportSQLiteDatabase;

    move-result-object v2

    .line 45
    .local v2, "database":Landroidx/sqlite/db/SupportSQLiteDatabase;
    const-string v0, "\n            SELECT COUNT(*), COALESCE(SUM(byteLength), 0),\n                COUNT(CASE WHEN typeof(byteLength) <> \'integer\' OR byteLength < 0 THEN 1 END)\n            FROM local_media_assets\n        "

    invoke-interface {v2, v0}, Landroidx/sqlite/db/SupportSQLiteDatabase;->query(Ljava/lang/String;)Landroid/database/Cursor;

    move-result-object v0

    move-object v3, v0

    check-cast v3, Ljava/io/Closeable;

    iget-object v0, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->this$0:Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;

    :try_start_39
    move-object v4, v3

    check-cast v4, Landroid/database/Cursor;

    .local v4, "cursor\\1":Landroid/database/Cursor;
    const/4 v5, 0x0

    .line 46
    .local v5, "$i$a$-use-GenerationPhotoStorageReader$read$2$summary$1$1$metadata$1\\1\\45\\0":I
    invoke-interface {v4}, Landroid/database/Cursor;->getColumnCount()I

    move-result v6

    const/4 v7, 0x3

    const/4 v8, 0x1

    const/4 v9, 0x0

    if-ne v6, v7, :cond_4e

    invoke-interface {v4}, Landroid/database/Cursor;->moveToFirst()Z

    move-result v6

    if-eqz v6, :cond_4e

    move v6, v8

    goto :goto_4f

    :cond_4e
    move v6, v9

    :goto_4f
    if-eqz v6, :cond_11f

    .line 47
    new-instance v6, Lkotlin/Triple;

    invoke-static {v0, v4, v9}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;->access$integer(Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;Landroid/database/Cursor;I)J

    move-result-wide v9

    invoke-static {v9, v10}, Lkotlin/coroutines/jvm/internal/Boxing;->boxLong(J)Ljava/lang/Long;

    move-result-object v7

    invoke-static {v0, v4, v8}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;->access$integer(Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;Landroid/database/Cursor;I)J

    move-result-wide v8

    invoke-static {v8, v9}, Lkotlin/coroutines/jvm/internal/Boxing;->boxLong(J)Ljava/lang/Long;

    move-result-object v8

    const/4 v9, 0x2

    invoke-static {v0, v4, v9}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;->access$integer(Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;Landroid/database/Cursor;I)J

    move-result-wide v9

    invoke-static {v9, v10}, Lkotlin/coroutines/jvm/internal/Boxing;->boxLong(J)Ljava/lang/Long;

    move-result-object v0

    invoke-direct {v6, v7, v8, v0}, Lkotlin/Triple;-><init>(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 48
    .local v6, "result\\1":Lkotlin/Triple;
    invoke-interface {v4}, Landroid/database/Cursor;->moveToNext()Z

    move-result v0
    :try_end_73
    .catchall {:try_start_39 .. :try_end_73} :catchall_12c

    if-nez v0, :cond_112

    .line 49
    nop

    .line 45
    .end local v4    # "cursor\\1":Landroid/database/Cursor;
    .end local v5    # "$i$a$-use-GenerationPhotoStorageReader$read$2$summary$1$1$metadata$1\\1\\45\\0":I
    .end local v6    # "result\\1":Lkotlin/Triple;
    const/4 v0, 0x0

    invoke-static {v3, v0}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    .line 51
    .local v6, "metadata":Lkotlin/Triple;
    iget-object v3, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->$caller:Lkotlin/coroutines/CoroutineContext;

    iget-object v4, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->this$0:Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;

    invoke-static {v3, v4}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2;->access$invokeSuspend$checkpoint(Lkotlin/coroutines/CoroutineContext;Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;)V

    .line 52
    const-string v3, "SELECT COUNT(*) FROM (\n            SELECT coverAssetId FROM long_term_projects WHERE coverAssetId IS NOT NULL\n            UNION SELECT coverAssetId FROM subscriptions WHERE coverAssetId IS NOT NULL\n        )"

    invoke-interface {v2, v3}, Landroidx/sqlite/db/SupportSQLiteDatabase;->query(Ljava/lang/String;)Landroid/database/Cursor;

    move-result-object v3

    check-cast v3, Ljava/io/Closeable;

    iget-object v4, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->this$0:Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;

    :try_start_8b
    move-object v5, v3

    check-cast v5, Landroid/database/Cursor;

    .line 123
    .local v5, "cursor\\4":Landroid/database/Cursor;
    const/4 v7, 0x0

    .line 52
    .local v7, "$i$a$-use-GenerationPhotoStorageReader$read$2$summary$1$1$references$1\\4\\52\\0":I
    invoke-static {v4, v5}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;->access$singleInteger(Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;Landroid/database/Cursor;)J

    move-result-wide v8
    :try_end_93
    .catchall {:try_start_8b .. :try_end_93} :catchall_10a

    .end local v5    # "cursor\\4":Landroid/database/Cursor;
    .end local v7    # "$i$a$-use-GenerationPhotoStorageReader$read$2$summary$1$1$references$1\\4\\52\\0":I
    invoke-static {v3, v0}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    move-wide v12, v8

    .line 53
    .local v12, "references":J
    iget-object v3, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->$caller:Lkotlin/coroutines/CoroutineContext;

    iget-object v4, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->this$0:Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;

    invoke-static {v3, v4}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2;->access$invokeSuspend$checkpoint(Lkotlin/coroutines/CoroutineContext;Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;)V

    .line 54
    const-string v3, "\n            SELECT COUNT(*) FROM (\n            SELECT coverAssetId FROM long_term_projects WHERE coverAssetId IS NOT NULL\n            UNION SELECT coverAssetId FROM subscriptions WHERE coverAssetId IS NOT NULL\n        ) AS covers\n            LEFT JOIN local_media_assets AS media ON covers.coverAssetId = media.id\n            WHERE media.id IS NULL\n        "

    invoke-interface {v2, v3}, Landroidx/sqlite/db/SupportSQLiteDatabase;->query(Ljava/lang/String;)Landroid/database/Cursor;

    move-result-object v3

    check-cast v3, Ljava/io/Closeable;

    iget-object v4, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->this$0:Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;

    :try_start_a8
    move-object v5, v3

    check-cast v5, Landroid/database/Cursor;

    .line 123
    .local v5, "cursor\\5":Landroid/database/Cursor;
    const/4 v7, 0x0

    .line 54
    .local v7, "$i$a$-use-GenerationPhotoStorageReader$read$2$summary$1$1$missing$1\\5\\54\\0":I
    invoke-static {v4, v5}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;->access$singleInteger(Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;Landroid/database/Cursor;)J

    move-result-wide v16
    :try_end_b0
    .catchall {:try_start_a8 .. :try_end_b0} :catchall_102

    .end local v5    # "cursor\\5":Landroid/database/Cursor;
    .end local v7    # "$i$a$-use-GenerationPhotoStorageReader$read$2$summary$1$1$missing$1\\5\\54\\0":I
    invoke-static {v3, v0}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    .line 55
    .local v16, "missing":J
    iget-object v3, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->$caller:Lkotlin/coroutines/CoroutineContext;

    iget-object v4, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->this$0:Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;

    invoke-static {v3, v4}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2;->access$invokeSuspend$checkpoint(Lkotlin/coroutines/CoroutineContext;Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;)V

    .line 56
    const-string v3, "\n            SELECT COALESCE(SUM(media.byteLength), 0) FROM local_media_assets AS media\n            WHERE NOT EXISTS (\n                SELECT 1 FROM (\n            SELECT coverAssetId FROM long_term_projects WHERE coverAssetId IS NOT NULL\n            UNION SELECT coverAssetId FROM subscriptions WHERE coverAssetId IS NOT NULL\n        ) AS covers WHERE covers.coverAssetId = media.id\n            )\n        "

    invoke-interface {v2, v3}, Landroidx/sqlite/db/SupportSQLiteDatabase;->query(Ljava/lang/String;)Landroid/database/Cursor;

    move-result-object v3

    check-cast v3, Ljava/io/Closeable;

    iget-object v4, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->this$0:Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;

    :try_start_c4
    move-object v5, v3

    check-cast v5, Landroid/database/Cursor;

    .line 123
    .local v5, "cursor\\6":Landroid/database/Cursor;
    const/4 v7, 0x0

    .line 56
    .local v7, "$i$a$-use-GenerationPhotoStorageReader$read$2$summary$1$1$historicalBytes$1\\6\\56\\0":I
    invoke-static {v4, v5}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;->access$singleInteger(Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;Landroid/database/Cursor;)J

    move-result-wide v20
    :try_end_cc
    .catchall {:try_start_c4 .. :try_end_cc} :catchall_fa

    .end local v5    # "cursor\\6":Landroid/database/Cursor;
    .end local v7    # "$i$a$-use-GenerationPhotoStorageReader$read$2$summary$1$1$historicalBytes$1\\6\\56\\0":I
    invoke-static {v3, v0}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    .line 57
    .local v20, "historicalBytes":J
    iget-object v0, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->$caller:Lkotlin/coroutines/CoroutineContext;

    iget-object v3, v1, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2$summary$1$1;->this$0:Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;

    invoke-static {v0, v3}, Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader$read$2;->access$invokeSuspend$checkpoint(Lkotlin/coroutines/CoroutineContext;Lcom/cooper/lithic/data/media/GenerationPhotoStorageReader;)V

    .line 58
    invoke-virtual {v6}, Lkotlin/Triple;->getFirst()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/Number;

    invoke-virtual {v0}, Ljava/lang/Number;->longValue()J

    move-result-wide v10

    invoke-virtual {v6}, Lkotlin/Triple;->getSecond()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/Number;

    invoke-virtual {v0}, Ljava/lang/Number;->longValue()J

    move-result-wide v14

    .line 59
    invoke-virtual {v6}, Lkotlin/Triple;->getThird()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/Number;

    invoke-virtual {v0}, Ljava/lang/Number;->longValue()J

    move-result-wide v18

    .line 60
    nop

    .line 58
    invoke-static/range {v10 .. v21}, Lcom/cooper/lithic/data/media/PhotoStorageReaderKt;->photoStorageSummary(JJJJJJ)Lcom/cooper/lithic/data/media/PhotoStorageSummary;

    move-result-object v0

    .line 60
    return-object v0

    .line 56
    .end local v20    # "historicalBytes":J
    :catchall_fa
    move-exception v0

    move-object v4, v0

    .end local v2    # "database":Landroidx/sqlite/db/SupportSQLiteDatabase;
    .end local v6    # "metadata":Lkotlin/Triple;
    .end local v12    # "references":J
    .end local v16    # "missing":J
    .end local p1    # "$result":Ljava/lang/Object;
    :try_start_fc
    throw v4
    :try_end_fd
    .catchall {:try_start_fc .. :try_end_fd} :catchall_fd

    .restart local v2    # "database":Landroidx/sqlite/db/SupportSQLiteDatabase;
    .restart local v6    # "metadata":Lkotlin/Triple;
    .restart local v12    # "references":J
    .restart local v16    # "missing":J
    .restart local p1    # "$result":Ljava/lang/Object;
    :catchall_fd
    move-exception v0

    invoke-static {v3, v4}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    throw v0

    .line 54
    .end local v16    # "missing":J
    :catchall_102
    move-exception v0

    move-object v4, v0

    .end local v2    # "database":Landroidx/sqlite/db/SupportSQLiteDatabase;
    .end local v6    # "metadata":Lkotlin/Triple;
    .end local v12    # "references":J
    .end local p1    # "$result":Ljava/lang/Object;
    :try_start_104
    throw v4
    :try_end_105
    .catchall {:try_start_104 .. :try_end_105} :catchall_105

    .restart local v2    # "database":Landroidx/sqlite/db/SupportSQLiteDatabase;
    .restart local v6    # "metadata":Lkotlin/Triple;
    .restart local v12    # "references":J
    .restart local p1    # "$result":Ljava/lang/Object;
    :catchall_105
    move-exception v0

    invoke-static {v3, v4}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    throw v0

    .line 52
    .end local v12    # "references":J
    :catchall_10a
    move-exception v0

    move-object v4, v0

    .end local v2    # "database":Landroidx/sqlite/db/SupportSQLiteDatabase;
    .end local v6    # "metadata":Lkotlin/Triple;
    .end local p1    # "$result":Ljava/lang/Object;
    :try_start_10c
    throw v4
    :try_end_10d
    .catchall {:try_start_10c .. :try_end_10d} :catchall_10d

    .restart local v2    # "database":Landroidx/sqlite/db/SupportSQLiteDatabase;
    .restart local v6    # "metadata":Lkotlin/Triple;
    .restart local p1    # "$result":Ljava/lang/Object;
    :catchall_10d
    move-exception v0

    invoke-static {v3, v4}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    throw v0

    .line 123
    .restart local v4    # "cursor\\1":Landroid/database/Cursor;
    .local v5, "$i$a$-use-GenerationPhotoStorageReader$read$2$summary$1$1$metadata$1\\1\\45\\0":I
    .local v6, "result\\1":Lkotlin/Triple;
    :cond_112
    const/4 v0, 0x0

    .line 48
    .local v0, "$i$a$-require-GenerationPhotoStorageReader$read$2$summary$1$1$metadata$1$2\\3\\48\\1":I
    :try_start_113
    const-string v7, "Ambiguous photo metadata aggregates"

    .end local v0    # "$i$a$-require-GenerationPhotoStorageReader$read$2$summary$1$1$metadata$1$2\\3\\48\\1":I
    new-instance v0, Ljava/lang/IllegalArgumentException;

    invoke-virtual {v7}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v7

    invoke-direct {v0, v7}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    .end local v2    # "database":Landroidx/sqlite/db/SupportSQLiteDatabase;
    .end local p1    # "$result":Ljava/lang/Object;
    throw v0

    .line 123
    .end local v6    # "result\\1":Lkotlin/Triple;
    .restart local v2    # "database":Landroidx/sqlite/db/SupportSQLiteDatabase;
    .restart local p1    # "$result":Ljava/lang/Object;
    :cond_11f
    const/4 v0, 0x0

    .line 46
    .local v0, "$i$a$-require-GenerationPhotoStorageReader$read$2$summary$1$1$metadata$1$1\\2\\46\\1":I
    const-string v6, "Missing photo metadata aggregates"

    .end local v0    # "$i$a$-require-GenerationPhotoStorageReader$read$2$summary$1$1$metadata$1$1\\2\\46\\1":I
    new-instance v0, Ljava/lang/IllegalArgumentException;

    invoke-virtual {v6}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v6

    invoke-direct {v0, v6}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    .end local v2    # "database":Landroidx/sqlite/db/SupportSQLiteDatabase;
    .end local p1    # "$result":Ljava/lang/Object;
    throw v0
    :try_end_12c
    .catchall {:try_start_113 .. :try_end_12c} :catchall_12c

    .line 45
    .end local v4    # "cursor\\1":Landroid/database/Cursor;
    .end local v5    # "$i$a$-use-GenerationPhotoStorageReader$read$2$summary$1$1$metadata$1\\1\\45\\0":I
    .restart local v2    # "database":Landroidx/sqlite/db/SupportSQLiteDatabase;
    .restart local p1    # "$result":Ljava/lang/Object;
    :catchall_12c
    move-exception v0

    move-object v4, v0

    .end local v2    # "database":Landroidx/sqlite/db/SupportSQLiteDatabase;
    .end local p1    # "$result":Ljava/lang/Object;
    :try_start_12e
    throw v4
    :try_end_12f
    .catchall {:try_start_12e .. :try_end_12f} :catchall_12f

    .restart local v2    # "database":Landroidx/sqlite/db/SupportSQLiteDatabase;
    .restart local p1    # "$result":Ljava/lang/Object;
    :catchall_12f
    move-exception v0

    invoke-static {v3, v4}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V

    throw v0

    :pswitch_data_134
    .packed-switch 0x0
        :pswitch_12
    .end packed-switch
.end method

