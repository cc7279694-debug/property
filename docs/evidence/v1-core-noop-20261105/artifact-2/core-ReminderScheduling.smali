.class public final Lcom/cooper/lithic/platform/notification/ReminderScheduling;
.super Ljava/lang/Object;
.source "ReminderScheduling.kt"


# annotations
.annotation runtime Lkotlin/Metadata;
    d1 = {
        "\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0008\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0008\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0008\u0003\u0008\u00c7\u0002\u0018\u00002\u00020\u0001B\t\u0008\u0002\u00a2\u0006\u0004\u0008\u0002\u0010\u0003J\u000e\u0010\u0006\u001a\u00020\u00072\u0006\u0010\u0008\u001a\u00020\tJ\u000e\u0010\n\u001a\u00020\u00072\u0006\u0010\u0008\u001a\u00020\tJ\u0018\u0010\u000b\u001a\u00020\u00072\u0006\u0010\u0008\u001a\u00020\tH\u0080@\u00a2\u0006\u0004\u0008\u000c\u0010\rJ8\u0010\u000e\u001a\u00020\u00072\u0006\u0010\u0008\u001a\u00020\t2\u0006\u0010\u000f\u001a\u00020\u00102\u000c\u0010\u0011\u001a\u0008\u0012\u0004\u0012\u00020\u00130\u00122\u0008\u0008\u0002\u0010\u0014\u001a\u00020\u0015H\u0080@\u00a2\u0006\u0004\u0008\u0016\u0010\u0017R\u000e\u0010\u0004\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"
    }
    d2 = {
        "Lcom/cooper/lithic/platform/notification/ReminderScheduling;",
        "",
        "<init>",
        "()V",
        "CANDIDATE_TAG",
        "",
        "start",
        "",
        "context",
        "Landroid/content/Context;",
        "request",
        "requestMilestones",
        "requestMilestones$app_debug",
        "(Landroid/content/Context;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;",
        "reconcile",
        "generation",
        "Lcom/cooper/lithic/data/backup/ManagedGeneration;",
        "isCurrent",
        "Lkotlin/Function0;",
        "",
        "clock",
        "Ljava/time/Clock;",
        "reconcile$app_debug",
        "(Landroid/content/Context;Lcom/cooper/lithic/data/backup/ManagedGeneration;Lkotlin/jvm/functions/Function0;Ljava/time/Clock;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;",
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
.field public static final $stable:I = 0x0

.field public static final CANDIDATE_TAG:Ljava/lang/String; = "lithic-renewal-candidate-v1"

.field public static final INSTANCE:Lcom/cooper/lithic/platform/notification/ReminderScheduling;


# direct methods
.method static constructor <clinit>()V
    .registers 1

    new-instance v0, Lcom/cooper/lithic/platform/notification/ReminderScheduling;

    invoke-direct {v0}, Lcom/cooper/lithic/platform/notification/ReminderScheduling;-><init>()V

    sput-object v0, Lcom/cooper/lithic/platform/notification/ReminderScheduling;->INSTANCE:Lcom/cooper/lithic/platform/notification/ReminderScheduling;

    return-void
.end method

.method private constructor <init>()V
    .registers 1

    .line 8
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static synthetic reconcile$app_debug$default(Lcom/cooper/lithic/platform/notification/ReminderScheduling;Landroid/content/Context;Lcom/cooper/lithic/data/backup/ManagedGeneration;Lkotlin/jvm/functions/Function0;Ljava/time/Clock;Lkotlin/coroutines/Continuation;ILjava/lang/Object;)Ljava/lang/Object;
    .registers 14

    .line 15
    and-int/lit8 p6, p6, 0x8

    if-eqz p6, :cond_f

    .line 19
    invoke-static {}, Ljava/time/Clock;->systemUTC()Ljava/time/Clock;

    move-result-object p4

    const-string p6, "systemUTC(...)"

    invoke-static {p4, p6}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    move-object v4, p4

    goto :goto_10

    .line 15
    :cond_f
    move-object v4, p4

    :goto_10
    move-object v0, p0

    move-object v1, p1

    move-object v2, p2

    move-object v3, p3

    move-object v5, p5

    invoke-virtual/range {v0 .. v5}, Lcom/cooper/lithic/platform/notification/ReminderScheduling;->reconcile$app_debug(Landroid/content/Context;Lcom/cooper/lithic/data/backup/ManagedGeneration;Lkotlin/jvm/functions/Function0;Ljava/time/Clock;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;

    move-result-object p0

    return-object p0
.end method


# virtual methods
.method public final reconcile$app_debug(Landroid/content/Context;Lcom/cooper/lithic/data/backup/ManagedGeneration;Lkotlin/jvm/functions/Function0;Ljava/time/Clock;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    .registers 7
    .param p1, "context"    # Landroid/content/Context;
    .param p2, "generation"    # Lcom/cooper/lithic/data/backup/ManagedGeneration;
    .param p3, "isCurrent"    # Lkotlin/jvm/functions/Function0;
    .param p4, "clock"    # Ljava/time/Clock;
    .param p5, "$completion"    # Lkotlin/coroutines/Continuation;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Landroid/content/Context;",
            "Lcom/cooper/lithic/data/backup/ManagedGeneration;",
            "Lkotlin/jvm/functions/Function0<",
            "Ljava/lang/Boolean;",
            ">;",
            "Ljava/time/Clock;",
            "Lkotlin/coroutines/Continuation<",
            "-",
            "Lkotlin/Unit;",
            ">;)",
            "Ljava/lang/Object;"
        }
    .end annotation

    .line 20
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method public final request(Landroid/content/Context;)V
    .registers 3
    .param p1, "context"    # Landroid/content/Context;

    const-string v0, "context"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    .line 13
    return-void
.end method

.method public final requestMilestones$app_debug(Landroid/content/Context;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    .registers 4
    .param p1, "context"    # Landroid/content/Context;
    .param p2, "$completion"    # Lkotlin/coroutines/Continuation;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Landroid/content/Context;",
            "Lkotlin/coroutines/Continuation<",
            "-",
            "Lkotlin/Unit;",
            ">;)",
            "Ljava/lang/Object;"
        }
    .end annotation

    .line 14
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method public final start(Landroid/content/Context;)V
    .registers 3
    .param p1, "context"    # Landroid/content/Context;

    const-string v0, "context"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    .line 12
    return-void
.end method

