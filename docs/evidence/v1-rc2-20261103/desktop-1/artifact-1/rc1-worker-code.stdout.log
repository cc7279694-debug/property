.class public final Lcom/cooper/lithic/platform/notification/ReminderWorker;
.super Landroidx/work/CoroutineWorker;
.source "ReminderWorker.kt"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/cooper/lithic/platform/notification/ReminderWorker$Companion;
    }
.end annotation

.annotation runtime Lkotlin/Metadata;
    d1 = {
        "\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0008\u0003\n\u0002\u0018\u0002\n\u0002\u0008\u0003\u0008\u0007\u0018\u0000 \u00132\u00020\u0001:\u0001\u0013B=\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0008\u0008\u0002\u0010\u0008\u001a\u00020\t\u0012\u0008\u0008\u0002\u0010\n\u001a\u00020\u000b\u0012\u0008\u0008\u0002\u0010\u000c\u001a\u00020\r\u00a2\u0006\u0004\u0008\u000e\u0010\u000fJ\u000e\u0010\u0010\u001a\u00020\u0011H\u0096@\u00a2\u0006\u0002\u0010\u0012R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0008\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000c\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"
    }
    d2 = {
        "Lcom/cooper/lithic/platform/notification/ReminderWorker;",
        "Landroidx/work/CoroutineWorker;",
        "context",
        "Landroid/content/Context;",
        "parameters",
        "Landroidx/work/WorkerParameters;",
        "container",
        "Lcom/cooper/lithic/app/AppContainer;",
        "publisher",
        "Lcom/cooper/lithic/platform/notification/ReminderPublisher;",
        "clock",
        "Ljava/time/Clock;",
        "milestonePublisher",
        "Lcom/cooper/lithic/platform/notification/MilestonePublisher;",
        "<init>",
        "(Landroid/content/Context;Landroidx/work/WorkerParameters;Lcom/cooper/lithic/app/AppContainer;Lcom/cooper/lithic/platform/notification/ReminderPublisher;Ljava/time/Clock;Lcom/cooper/lithic/platform/notification/MilestonePublisher;)V",
        "doWork",
        "Landroidx/work/ListenableWorker$Result;",
        "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;",
        "Companion",
        "app_debug"
    }
    k = 0x1
    mv = {
        0x2,
        0x3,
        0x0
    }
    xi = 0x30
.end annotation


# static fields
.field public static final $stable:I

.field public static final Companion:Lcom/cooper/lithic/platform/notification/ReminderWorker$Companion;

.field public static final GENERATION:Ljava/lang/String; = "generation"

.field public static final KEY:Ljava/lang/String; = "logical_key"

.field public static final MILESTONE_PUBLISH:Ljava/lang/String; = "milestone_publish"

.field public static final MILESTONE_RECONCILE:Ljava/lang/String; = "milestone_reconcile"

.field public static final MODE:Ljava/lang/String; = "mode"

.field public static final PUBLISH:Ljava/lang/String; = "publish"

.field public static final RECONCILE:Ljava/lang/String; = "reconcile"

.field public static final RENEWAL:Ljava/lang/String; = "renewal_id"

.field public static final REVISION:Ljava/lang/String; = "revision"


# instance fields
.field private final clock:Ljava/time/Clock;

.field private final container:Lcom/cooper/lithic/app/AppContainer;

.field private final milestonePublisher:Lcom/cooper/lithic/platform/notification/MilestonePublisher;

.field private final publisher:Lcom/cooper/lithic/platform/notification/ReminderPublisher;


# direct methods
.method static constructor <clinit>()V
    .registers 2

    new-instance v0, Lcom/cooper/lithic/platform/notification/ReminderWorker$Companion;

    const/4 v1, 0x0

    invoke-direct {v0, v1}, Lcom/cooper/lithic/platform/notification/ReminderWorker$Companion;-><init>(Lkotlin/jvm/internal/DefaultConstructorMarker;)V

    sput-object v0, Lcom/cooper/lithic/platform/notification/ReminderWorker;->Companion:Lcom/cooper/lithic/platform/notification/ReminderWorker$Companion;

    const/16 v0, 0x8

    sput v0, Lcom/cooper/lithic/platform/notification/ReminderWorker;->$stable:I

    return-void
.end method

.method public constructor <init>(Landroid/content/Context;Landroidx/work/WorkerParameters;Lcom/cooper/lithic/app/AppContainer;Lcom/cooper/lithic/platform/notification/ReminderPublisher;Ljava/time/Clock;Lcom/cooper/lithic/platform/notification/MilestonePublisher;)V
    .registers 8
    .param p1, "context"    # Landroid/content/Context;
    .param p2, "parameters"    # Landroidx/work/WorkerParameters;
    .param p3, "container"    # Lcom/cooper/lithic/app/AppContainer;
    .param p4, "publisher"    # Lcom/cooper/lithic/platform/notification/ReminderPublisher;
    .param p5, "clock"    # Ljava/time/Clock;
    .param p6, "milestonePublisher"    # Lcom/cooper/lithic/platform/notification/MilestonePublisher;

    const-string v0, "context"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v0, "parameters"

    invoke-static {p2, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v0, "container"

    invoke-static {p3, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v0, "publisher"

    invoke-static {p4, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v0, "clock"

    invoke-static {p5, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v0, "milestonePublisher"

    invoke-static {p6, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    .line 15
    nop

    .line 18
    nop

    .line 15
    invoke-direct {p0, p1, p2}, Landroidx/work/CoroutineWorker;-><init>(Landroid/content/Context;Landroidx/work/WorkerParameters;)V

    iput-object p3, p0, Lcom/cooper/lithic/platform/notification/ReminderWorker;->container:Lcom/cooper/lithic/app/AppContainer;

    .line 16
    iput-object p4, p0, Lcom/cooper/lithic/platform/notification/ReminderWorker;->publisher:Lcom/cooper/lithic/platform/notification/ReminderPublisher;

    .line 17
    iput-object p5, p0, Lcom/cooper/lithic/platform/notification/ReminderWorker;->clock:Ljava/time/Clock;

    .line 18
    iput-object p6, p0, Lcom/cooper/lithic/platform/notification/ReminderWorker;->milestonePublisher:Lcom/cooper/lithic/platform/notification/MilestonePublisher;

    .line 15
    return-void
.end method

.method public synthetic constructor <init>(Landroid/content/Context;Landroidx/work/WorkerParameters;Lcom/cooper/lithic/app/AppContainer;Lcom/cooper/lithic/platform/notification/ReminderPublisher;Ljava/time/Clock;Lcom/cooper/lithic/platform/notification/MilestonePublisher;ILkotlin/jvm/internal/DefaultConstructorMarker;)V
    .registers 16

    .line 15
    and-int/lit8 p8, p7, 0x8

    if-eqz p8, :cond_d

    .line 16
    new-instance p4, Lcom/cooper/lithic/platform/notification/NotificationPublisher;

    invoke-direct {p4, p1}, Lcom/cooper/lithic/platform/notification/NotificationPublisher;-><init>(Landroid/content/Context;)V

    check-cast p4, Lcom/cooper/lithic/platform/notification/ReminderPublisher;

    move-object v4, p4

    goto :goto_e

    .line 15
    :cond_d
    move-object v4, p4

    :goto_e
    and-int/lit8 p4, p7, 0x10

    if-eqz p4, :cond_1d

    .line 17
    invoke-static {}, Ljava/time/Clock;->systemUTC()Ljava/time/Clock;

    move-result-object p5

    const-string p4, "systemUTC(...)"

    invoke-static {p5, p4}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    move-object v5, p5

    goto :goto_1e

    .line 15
    :cond_1d
    move-object v5, p5

    :goto_1e
    and-int/lit8 p4, p7, 0x20

    if-eqz p4, :cond_2c

    .line 18
    new-instance p4, Lcom/cooper/lithic/platform/notification/NotificationPublisher;

    invoke-direct {p4, p1}, Lcom/cooper/lithic/platform/notification/NotificationPublisher;-><init>(Landroid/content/Context;)V

    move-object p6, p4

    check-cast p6, Lcom/cooper/lithic/platform/notification/MilestonePublisher;

    move-object v6, p6

    goto :goto_2d

    .line 15
    :cond_2c
    move-object v6, p6

    :goto_2d
    move-object v0, p0

    move-object v1, p1

    move-object v2, p2

    move-object v3, p3

    invoke-direct/range {v0 .. v6}, Lcom/cooper/lithic/platform/notification/ReminderWorker;-><init>(Landroid/content/Context;Landroidx/work/WorkerParameters;Lcom/cooper/lithic/app/AppContainer;Lcom/cooper/lithic/platform/notification/ReminderPublisher;Ljava/time/Clock;Lcom/cooper/lithic/platform/notification/MilestonePublisher;)V

    .line 18
    return-void
.end method

.method public static final synthetic access$doWork$current(Lcom/cooper/lithic/platform/notification/ReminderWorker;Lcom/cooper/lithic/data/backup/ManagedGeneration;)Z
    .registers 3
    .param p0, "this$0"    # Lcom/cooper/lithic/platform/notification/ReminderWorker;
    .param p1, "generation"    # Lcom/cooper/lithic/data/backup/ManagedGeneration;

    .line 15
    invoke-static {p0, p1}, Lcom/cooper/lithic/platform/notification/ReminderWorker;->doWork$current(Lcom/cooper/lithic/platform/notification/ReminderWorker;Lcom/cooper/lithic/data/backup/ManagedGeneration;)Z

    move-result v0

    return v0
.end method

.method public static final synthetic access$getClock$p(Lcom/cooper/lithic/platform/notification/ReminderWorker;)Ljava/time/Clock;
    .registers 2
    .param p0, "$this"    # Lcom/cooper/lithic/platform/notification/ReminderWorker;

    .line 15
    iget-object v0, p0, Lcom/cooper/lithic/platform/notification/ReminderWorker;->clock:Ljava/time/Clock;

    return-object v0
.end method

.method private static final doWork$current(Lcom/cooper/lithic/platform/notification/ReminderWorker;Lcom/cooper/lithic/data/backup/ManagedGeneration;)Z
    .registers 5
    .param p0, "this$0"    # Lcom/cooper/lithic/platform/notification/ReminderWorker;
    .param p1, "generation"    # Lcom/cooper/lithic/data/backup/ManagedGeneration;

    .line 22
    iget-object v0, p0, Lcom/cooper/lithic/platform/notification/ReminderWorker;->container:Lcom/cooper/lithic/app/AppContainer;

    invoke-virtual {v0}, Lcom/cooper/lithic/app/AppContainer;->getStorageState()Lkotlinx/coroutines/flow/StateFlow;

    move-result-object v0

    invoke-interface {v0}, Lkotlinx/coroutines/flow/StateFlow;->getValue()Ljava/lang/Object;

    move-result-object v0

    instance-of v1, v0, Lcom/cooper/lithic/app/StorageInitializationState$Ready;

    const/4 v2, 0x0

    if-eqz v1, :cond_12

    check-cast v0, Lcom/cooper/lithic/app/StorageInitializationState$Ready;

    goto :goto_13

    :cond_12
    move-object v0, v2

    :goto_13
    if-eqz v0, :cond_19

    invoke-virtual {v0}, Lcom/cooper/lithic/app/StorageInitializationState$Ready;->getGeneration()Lcom/cooper/lithic/data/backup/ManagedGeneration;

    move-result-object v2

    :cond_19
    if-ne v2, p1, :cond_1d

    const/4 v0, 0x1

    goto :goto_1e

    :cond_1d
    const/4 v0, 0x0

    :goto_1e
    return v0
.end method


# virtual methods
.method public doWork(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    .registers 15
    .param p1, "$completion"    # Lkotlin/coroutines/Continuation;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Lkotlin/coroutines/Continuation<",
            "-",
            "Landroidx/work/ListenableWorker$Result;",
            ">;)",
            "Ljava/lang/Object;"
        }
    .end annotation

    instance-of v0, p1, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;

    if-eqz v0, :cond_14

    move-object v0, p1

    check-cast v0, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;

    iget v1, v0, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->label:I

    const/high16 v2, -0x80000000

    and-int/2addr v1, v2

    if-eqz v1, :cond_14

    iget v1, v0, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->label:I

    sub-int/2addr v1, v2

    iput v1, v0, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->label:I

    goto :goto_19

    :cond_14
    new-instance v0, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;

    invoke-direct {v0, p0, p1}, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;-><init>(Lcom/cooper/lithic/platform/notification/ReminderWorker;Lkotlin/coroutines/Continuation;)V

    :goto_19
    move-object v7, v0

    .local v7, "$continuation":Lkotlin/coroutines/Continuation;
    iget-object v10, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->result:Ljava/lang/Object;

    .local v10, "$result":Ljava/lang/Object;
    invoke-static {}, Lkotlin/coroutines/intrinsics/IntrinsicsKt;->getCOROUTINE_SUSPENDED()Ljava/lang/Object;

    move-result-object v0

    .line 19
    iget v1, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->label:I

    const-string v2, "retry(...)"

    const/4 v11, 0x5

    packed-switch v1, :pswitch_data_226

    .end local v7    # "$continuation":Lkotlin/coroutines/Continuation;
    .end local v10    # "$result":Ljava/lang/Object;
    new-instance v0, Ljava/lang/IllegalStateException;

    const-string v1, "call to \'resume\' before \'invoke\' with coroutine"

    invoke-direct {v0, v1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw v0

    .restart local v7    # "$continuation":Lkotlin/coroutines/Continuation;
    .restart local v10    # "$result":Ljava/lang/Object;
    :pswitch_30
    iget-object v0, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->L$3:Ljava/lang/Object;

    check-cast v0, Ljava/lang/String;

    .local v0, "revision":Ljava/lang/String;
    iget-object v1, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->L$2:Ljava/lang/Object;

    check-cast v1, Ljava/lang/String;

    .local v1, "row":Ljava/lang/String;
    iget-object v3, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->L$1:Ljava/lang/Object;

    check-cast v3, Ljava/lang/String;

    .local v3, "key":Ljava/lang/String;
    iget-object v4, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->L$0:Ljava/lang/Object;

    check-cast v4, Lcom/cooper/lithic/data/backup/ManagedGeneration;

    .local v4, "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :try_start_40
    invoke-static {v10}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V
    :try_end_43
    .catch Ljava/util/concurrent/CancellationException; {:try_start_40 .. :try_end_43} :catch_6b
    .catch Ljava/lang/Exception; {:try_start_40 .. :try_end_43} :catch_68

    move-object v5, v4

    move-object v4, v10

    goto/16 :goto_1eb

    .end local v0    # "revision":Ljava/lang/String;
    .end local v1    # "row":Ljava/lang/String;
    .end local v3    # "key":Ljava/lang/String;
    .end local v4    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :pswitch_47
    iget-object v0, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->L$0:Ljava/lang/Object;

    check-cast v0, Lcom/cooper/lithic/data/backup/ManagedGeneration;

    .local v0, "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :try_start_4b
    invoke-static {v10}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V
    :try_end_4e
    .catch Ljava/util/concurrent/CancellationException; {:try_start_4b .. :try_end_4e} :catch_6b
    .catch Ljava/lang/Exception; {:try_start_4b .. :try_end_4e} :catch_68

    move-object v1, v10

    goto/16 :goto_141

    .end local v0    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :pswitch_51
    iget-object v0, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->L$0:Ljava/lang/Object;

    check-cast v0, Lcom/cooper/lithic/data/backup/ManagedGeneration;

    .restart local v0    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :try_start_55
    invoke-static {v10}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V
    :try_end_58
    .catch Ljava/util/concurrent/CancellationException; {:try_start_55 .. :try_end_58} :catch_6b
    .catch Ljava/lang/Exception; {:try_start_55 .. :try_end_58} :catch_68

    goto/16 :goto_cf

    .end local v0    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :pswitch_5a
    iget-object v0, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->L$0:Ljava/lang/Object;

    check-cast v0, Lcom/cooper/lithic/data/backup/ManagedGeneration;

    .restart local v0    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :try_start_5e
    invoke-static {v10}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V

    goto/16 :goto_fb

    .end local v0    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :pswitch_63
    invoke-static {v10}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V
    :try_end_66
    .catch Ljava/util/concurrent/CancellationException; {:try_start_5e .. :try_end_66} :catch_6b
    .catch Ljava/lang/Exception; {:try_start_5e .. :try_end_66} :catch_68

    move-object v1, v10

    goto :goto_7e

    .line 48
    :catch_68
    move-exception v0

    goto/16 :goto_211

    .line 47
    :catch_6b
    move-exception v0

    goto/16 :goto_224

    .line 19
    :pswitch_6e
    invoke-static {v10}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V

    .line 20
    nop

    .line 21
    :try_start_72
    iget-object v1, p0, Lcom/cooper/lithic/platform/notification/ReminderWorker;->container:Lcom/cooper/lithic/app/AppContainer;

    const/4 v3, 0x1

    iput v3, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->label:I

    invoke-virtual {v1, v7}, Lcom/cooper/lithic/app/AppContainer;->openStorage(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;

    move-result-object v1

    if-ne v1, v0, :cond_7e

    .line 19
    return-object v0

    :cond_7e
    :goto_7e
    move-object v3, v1

    check-cast v3, Lcom/cooper/lithic/data/backup/ManagedGeneration;

    .line 23
    .local v3, "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    invoke-virtual {p0}, Lcom/cooper/lithic/platform/notification/ReminderWorker;->getInputData()Landroidx/work/Data;

    move-result-object v1

    const-string v4, "mode"

    invoke-virtual {v1, v4}, Landroidx/work/Data;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1
    :try_end_8b
    .catch Ljava/util/concurrent/CancellationException; {:try_start_72 .. :try_end_8b} :catch_6b
    .catch Ljava/lang/Exception; {:try_start_72 .. :try_end_8b} :catch_68

    const-string v4, "failure(...)"

    if-eqz v1, :cond_209

    :try_start_8f
    invoke-virtual {v1}, Ljava/lang/String;->hashCode()I

    move-result v5
    :try_end_93
    .catch Ljava/util/concurrent/CancellationException; {:try_start_8f .. :try_end_93} :catch_6b
    .catch Ljava/lang/Exception; {:try_start_8f .. :try_end_93} :catch_68

    const-string v6, "success(...)"

    const-string v8, "generation"

    sparse-switch v5, :sswitch_data_236

    goto/16 :goto_209

    :sswitch_9c
    :try_start_9c
    const-string v2, "milestone_reconcile"

    invoke-virtual {v1, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_a6

    goto/16 :goto_209

    .line 30
    :cond_a6
    sget-object v1, Lcom/cooper/lithic/platform/notification/MilestoneScheduling;->INSTANCE:Lcom/cooper/lithic/platform/notification/MilestoneScheduling;

    invoke-virtual {p0}, Lcom/cooper/lithic/platform/notification/ReminderWorker;->getApplicationContext()Landroid/content/Context;

    move-result-object v2

    const-string v4, "getApplicationContext(...)"

    invoke-static {v2, v4}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    new-instance v4, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$4;

    invoke-direct {v4, p0, v3}, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$4;-><init>(Lcom/cooper/lithic/platform/notification/ReminderWorker;Lcom/cooper/lithic/data/backup/ManagedGeneration;)V

    check-cast v4, Lkotlin/jvm/functions/Function0;

    iget-object v5, p0, Lcom/cooper/lithic/platform/notification/ReminderWorker;->clock:Ljava/time/Clock;

    invoke-static {v3}, Lkotlin/coroutines/jvm/internal/SpillingKt;->nullOutSpilledVariable(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    iput-object v6, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->L$0:Ljava/lang/Object;

    const/4 v6, 0x3

    iput v6, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->label:I

    const/4 v6, 0x0

    const/16 v8, 0x10

    const/4 v9, 0x0

    invoke-static/range {v1 .. v9}, Lcom/cooper/lithic/platform/notification/MilestoneScheduling;->reconcile$app_debug$default(Lcom/cooper/lithic/platform/notification/MilestoneScheduling;Landroid/content/Context;Lcom/cooper/lithic/data/backup/ManagedGeneration;Lkotlin/jvm/functions/Function0;Ljava/time/Clock;Lcom/cooper/lithic/platform/notification/MilestonePublisher;Lkotlin/coroutines/Continuation;ILjava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    if-ne v1, v0, :cond_ce

    .line 19
    return-object v0

    .line 30
    :cond_ce
    move-object v0, v3

    .end local v3    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    .restart local v0    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :goto_cf
    goto/16 :goto_1f8

    .line 23
    .end local v0    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    .restart local v3    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :sswitch_d1
    const-string v2, "reconcile"

    invoke-virtual {v1, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_db

    goto/16 :goto_209

    .line 25
    :cond_db
    new-instance v1, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$2;

    const/4 v2, 0x0

    invoke-direct {v1, p0, v2}, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$2;-><init>(Lcom/cooper/lithic/platform/notification/ReminderWorker;Lkotlin/coroutines/Continuation;)V

    check-cast v1, Lkotlin/jvm/functions/Function1;

    new-instance v4, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$3;

    invoke-direct {v4, p0, v3, v2}, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$3;-><init>(Lcom/cooper/lithic/platform/notification/ReminderWorker;Lcom/cooper/lithic/data/backup/ManagedGeneration;Lkotlin/coroutines/Continuation;)V

    check-cast v4, Lkotlin/jvm/functions/Function1;

    invoke-static {v3}, Lkotlin/coroutines/jvm/internal/SpillingKt;->nullOutSpilledVariable(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    iput-object v2, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->L$0:Ljava/lang/Object;

    const/4 v2, 0x2

    iput v2, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->label:I

    invoke-static {v1, v4, v7}, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt;->reconcileReminderDomains(Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;

    move-result-object v1

    if-ne v1, v0, :cond_fa

    .line 19
    return-object v0

    .line 25
    :cond_fa
    move-object v0, v3

    .end local v3    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    .restart local v0    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :goto_fb
    goto/16 :goto_1f8

    .line 23
    .end local v0    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    .restart local v3    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :sswitch_fd
    const-string v5, "milestone_publish"

    invoke-virtual {v1, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_107

    goto/16 :goto_209

    .line 32
    :cond_107
    invoke-virtual {p0}, Lcom/cooper/lithic/platform/notification/ReminderWorker;->getInputData()Landroidx/work/Data;

    move-result-object v1

    invoke-virtual {v1, v8}, Landroidx/work/Data;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v3}, Lcom/cooper/lithic/data/backup/ManagedGeneration;->getId()Ljava/lang/String;

    move-result-object v4

    invoke-static {v1, v4}, Lkotlin/jvm/internal/Intrinsics;->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_14d

    invoke-static {p0, v3}, Lcom/cooper/lithic/platform/notification/ReminderWorker;->doWork$current(Lcom/cooper/lithic/platform/notification/ReminderWorker;Lcom/cooper/lithic/data/backup/ManagedGeneration;)Z

    move-result v1

    if-nez v1, :cond_120

    goto :goto_14d

    .line 33
    :cond_120
    new-instance v1, Lcom/cooper/lithic/data/repository/MilestoneRepository;

    new-instance v4, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$5;

    invoke-direct {v4, p0, v3}, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$5;-><init>(Lcom/cooper/lithic/platform/notification/ReminderWorker;Lcom/cooper/lithic/data/backup/ManagedGeneration;)V

    check-cast v4, Lkotlin/jvm/functions/Function0;

    .line 34
    iget-object v5, p0, Lcom/cooper/lithic/platform/notification/ReminderWorker;->milestonePublisher:Lcom/cooper/lithic/platform/notification/MilestonePublisher;

    iget-object v6, p0, Lcom/cooper/lithic/platform/notification/ReminderWorker;->clock:Ljava/time/Clock;

    .line 33
    invoke-direct {v1, v3, v4, v5, v6}, Lcom/cooper/lithic/data/repository/MilestoneRepository;-><init>(Lcom/cooper/lithic/data/backup/ManagedGeneration;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/platform/notification/MilestonePublisher;Ljava/time/Clock;)V

    .line 34
    invoke-static {v3}, Lkotlin/coroutines/jvm/internal/SpillingKt;->nullOutSpilledVariable(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    iput-object v4, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->L$0:Ljava/lang/Object;

    const/4 v4, 0x4

    iput v4, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->label:I

    invoke-virtual {v1, v7}, Lcom/cooper/lithic/data/repository/MilestoneRepository;->publish(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;

    move-result-object v1

    if-ne v1, v0, :cond_140

    .line 19
    return-object v0

    .line 34
    :cond_140
    move-object v0, v3

    .line 33
    .end local v3    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    .restart local v0    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :goto_141
    sget-object v3, Lcom/cooper/lithic/data/repository/ReminderDispatchResult;->WAITING_FOR_NINE:Lcom/cooper/lithic/data/repository/ReminderDispatchResult;

    if-ne v1, v3, :cond_1f8

    .line 34
    invoke-static {}, Landroidx/work/ListenableWorker$Result;->retry()Landroidx/work/ListenableWorker$Result;

    move-result-object v1

    invoke-static {v1, v2}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    return-object v1

    .line 32
    .end local v0    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    .restart local v3    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :cond_14d
    :goto_14d
    invoke-static {}, Landroidx/work/ListenableWorker$Result;->success()Landroidx/work/ListenableWorker$Result;

    move-result-object v0

    invoke-static {v0, v6}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    return-object v0

    .line 23
    :sswitch_155
    const-string v5, "publish"

    invoke-virtual {v1, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_15f

    goto/16 :goto_209

    .line 37
    :cond_15f
    invoke-virtual {p0}, Lcom/cooper/lithic/platform/notification/ReminderWorker;->getInputData()Landroidx/work/Data;

    move-result-object v1

    invoke-virtual {v1, v8}, Landroidx/work/Data;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v3}, Lcom/cooper/lithic/data/backup/ManagedGeneration;->getId()Ljava/lang/String;

    move-result-object v5

    invoke-static {v1, v5}, Lkotlin/jvm/internal/Intrinsics;->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_201

    invoke-static {p0, v3}, Lcom/cooper/lithic/platform/notification/ReminderWorker;->doWork$current(Lcom/cooper/lithic/platform/notification/ReminderWorker;Lcom/cooper/lithic/data/backup/ManagedGeneration;)Z

    move-result v1

    if-nez v1, :cond_179

    goto/16 :goto_201

    .line 38
    :cond_179
    invoke-virtual {p0}, Lcom/cooper/lithic/platform/notification/ReminderWorker;->getInputData()Landroidx/work/Data;

    move-result-object v1

    const-string v5, "logical_key"

    invoke-virtual {v1, v5}, Landroidx/work/Data;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    if-nez v1, :cond_18d

    invoke-static {}, Landroidx/work/ListenableWorker$Result;->failure()Landroidx/work/ListenableWorker$Result;

    move-result-object v0

    invoke-static {v0, v4}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    return-object v0

    .line 39
    .local v1, "key":Ljava/lang/String;
    :cond_18d
    invoke-virtual {p0}, Lcom/cooper/lithic/platform/notification/ReminderWorker;->getInputData()Landroidx/work/Data;

    move-result-object v5

    const-string v6, "renewal_id"

    invoke-virtual {v5, v6}, Landroidx/work/Data;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    if-nez v5, :cond_1a1

    invoke-static {}, Landroidx/work/ListenableWorker$Result;->failure()Landroidx/work/ListenableWorker$Result;

    move-result-object v0

    invoke-static {v0, v4}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    return-object v0

    .line 40
    .local v5, "row":Ljava/lang/String;
    :cond_1a1
    invoke-virtual {p0}, Lcom/cooper/lithic/platform/notification/ReminderWorker;->getInputData()Landroidx/work/Data;

    move-result-object v6

    const-string v8, "revision"

    invoke-virtual {v6, v8}, Landroidx/work/Data;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v6

    if-nez v6, :cond_1b5

    invoke-static {}, Landroidx/work/ListenableWorker$Result;->failure()Landroidx/work/ListenableWorker$Result;

    move-result-object v0

    invoke-static {v0, v4}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    return-object v0

    .line 41
    .local v6, "revision":Ljava/lang/String;
    :cond_1b5
    new-instance v4, Lcom/cooper/lithic/data/repository/ReminderRepository;

    new-instance v8, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$6;

    invoke-direct {v8, p0, v3}, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$6;-><init>(Lcom/cooper/lithic/platform/notification/ReminderWorker;Lcom/cooper/lithic/data/backup/ManagedGeneration;)V

    check-cast v8, Lkotlin/jvm/functions/Function0;

    iget-object v9, p0, Lcom/cooper/lithic/platform/notification/ReminderWorker;->publisher:Lcom/cooper/lithic/platform/notification/ReminderPublisher;

    iget-object v12, p0, Lcom/cooper/lithic/platform/notification/ReminderWorker;->clock:Ljava/time/Clock;

    invoke-direct {v4, v3, v8, v9, v12}, Lcom/cooper/lithic/data/repository/ReminderRepository;-><init>(Lcom/cooper/lithic/data/backup/ManagedGeneration;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/platform/notification/ReminderPublisher;Ljava/time/Clock;)V

    invoke-static {v3}, Lkotlin/coroutines/jvm/internal/SpillingKt;->nullOutSpilledVariable(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v8

    iput-object v8, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->L$0:Ljava/lang/Object;

    invoke-static {v1}, Lkotlin/coroutines/jvm/internal/SpillingKt;->nullOutSpilledVariable(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v8

    iput-object v8, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->L$1:Ljava/lang/Object;

    invoke-static {v5}, Lkotlin/coroutines/jvm/internal/SpillingKt;->nullOutSpilledVariable(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v8

    iput-object v8, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->L$2:Ljava/lang/Object;

    invoke-static {v6}, Lkotlin/coroutines/jvm/internal/SpillingKt;->nullOutSpilledVariable(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v8

    iput-object v8, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->L$3:Ljava/lang/Object;

    iput v11, v7, Lcom/cooper/lithic/platform/notification/ReminderWorker$doWork$1;->label:I

    invoke-virtual {v4, v1, v5, v6, v7}, Lcom/cooper/lithic/data/repository/ReminderRepository;->publish(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;

    move-result-object v4

    if-ne v4, v0, :cond_1e6

    .line 19
    return-object v0

    .line 41
    :cond_1e6
    move-object v0, v3

    move-object v3, v1

    move-object v1, v5

    move-object v5, v0

    move-object v0, v6

    .line 42
    .end local v6    # "revision":Ljava/lang/String;
    .local v0, "revision":Ljava/lang/String;
    .local v1, "row":Ljava/lang/String;
    .local v3, "key":Ljava/lang/String;
    .local v5, "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :goto_1eb
    sget-object v6, Lcom/cooper/lithic/data/repository/ReminderDispatchResult;->WAITING_FOR_NINE:Lcom/cooper/lithic/data/repository/ReminderDispatchResult;

    .line 41
    if-ne v4, v6, :cond_1f7

    .line 42
    invoke-static {}, Landroidx/work/ListenableWorker$Result;->retry()Landroidx/work/ListenableWorker$Result;

    move-result-object v4

    invoke-static {v4, v2}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    return-object v4

    .line 41
    :cond_1f7
    move-object v0, v5

    .line 46
    .end local v1    # "row":Ljava/lang/String;
    .end local v3    # "key":Ljava/lang/String;
    .end local v5    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    .local v0, "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :cond_1f8
    :goto_1f8
    invoke-static {}, Landroidx/work/ListenableWorker$Result;->success()Landroidx/work/ListenableWorker$Result;

    move-result-object v1

    .line 20
    .end local v0    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    nop

    .line 44
    invoke-static {v1}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;)V

    goto :goto_223

    .line 37
    .local v3, "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    :cond_201
    :goto_201
    invoke-static {}, Landroidx/work/ListenableWorker$Result;->success()Landroidx/work/ListenableWorker$Result;

    move-result-object v0

    invoke-static {v0, v6}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    return-object v0

    .line 44
    :cond_209
    :goto_209
    invoke-static {}, Landroidx/work/ListenableWorker$Result;->failure()Landroidx/work/ListenableWorker$Result;

    move-result-object v0

    invoke-static {v0, v4}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    :try_end_210
    .catch Ljava/util/concurrent/CancellationException; {:try_start_9c .. :try_end_210} :catch_6b
    .catch Ljava/lang/Exception; {:try_start_9c .. :try_end_210} :catch_68

    return-object v0

    .line 48
    .end local v3    # "generation":Lcom/cooper/lithic/data/backup/ManagedGeneration;
    .local v0, "<unused var>":Ljava/lang/Exception;
    :goto_211
    invoke-virtual {p0}, Lcom/cooper/lithic/platform/notification/ReminderWorker;->getRunAttemptCount()I

    move-result v1

    if-ge v1, v11, :cond_21c

    invoke-static {}, Landroidx/work/ListenableWorker$Result;->retry()Landroidx/work/ListenableWorker$Result;

    move-result-object v1

    goto :goto_220

    :cond_21c
    invoke-static {}, Landroidx/work/ListenableWorker$Result;->failure()Landroidx/work/ListenableWorker$Result;

    move-result-object v1

    :goto_220
    invoke-static {v1}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;)V

    .line 20
    .end local v0    # "<unused var>":Ljava/lang/Exception;
    :goto_223
    return-object v1

    .line 47
    .local v0, "cancelled":Ljava/util/concurrent/CancellationException;
    :goto_224
    throw v0

    nop

    :pswitch_data_226
    .packed-switch 0x0
        :pswitch_6e
        :pswitch_63
        :pswitch_5a
        :pswitch_51
        :pswitch_47
        :pswitch_30
    .end packed-switch

    :sswitch_data_236
    .sparse-switch
        -0xe0762f1 -> :sswitch_155
        0x7d3ed40 -> :sswitch_fd
        0x3affab4e -> :sswitch_d1
        0x4747b33f -> :sswitch_9c
    .end sparse-switch
.end method

