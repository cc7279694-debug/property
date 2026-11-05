.class public final Lcom/cooper/lithic/platform/notification/MilestoneScheduling;
.super Ljava/lang/Object;
.source "MilestoneScheduling.kt"


# annotations
.annotation runtime Lkotlin/Metadata;
    d1 = {
        "\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0008\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0008\u0003\u0008\u00c7\u0002\u0018\u00002\u00020\u0001B\t\u0008\u0002\u00a2\u0006\u0004\u0008\u0002\u0010\u0003JD\u0010\u0006\u001a\u00020\u00072\u0006\u0010\u0008\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u000c\u0010\u000c\u001a\u0008\u0012\u0004\u0012\u00020\u000e0\r2\u0008\u0008\u0002\u0010\u000f\u001a\u00020\u00102\n\u0008\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0012H\u0080@\u00a2\u0006\u0004\u0008\u0013\u0010\u0014R\u000e\u0010\u0004\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0015"
    }
    d2 = {
        "Lcom/cooper/lithic/platform/notification/MilestoneScheduling;",
        "",
        "<init>",
        "()V",
        "CANDIDATE_TAG",
        "",
        "reconcile",
        "",
        "context",
        "Landroid/content/Context;",
        "generation",
        "Lcom/cooper/lithic/data/backup/ManagedGeneration;",
        "isCurrent",
        "Lkotlin/Function0;",
        "",
        "clock",
        "Ljava/time/Clock;",
        "publisher",
        "Lcom/cooper/lithic/platform/notification/MilestonePublisher;",
        "reconcile$app_debug",
        "(Landroid/content/Context;Lcom/cooper/lithic/data/backup/ManagedGeneration;Lkotlin/jvm/functions/Function0;Ljava/time/Clock;Lcom/cooper/lithic/platform/notification/MilestonePublisher;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;",
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

.field public static final CANDIDATE_TAG:Ljava/lang/String; = "lithic-milestone-candidate-v1"

.field public static final INSTANCE:Lcom/cooper/lithic/platform/notification/MilestoneScheduling;


# direct methods
.method static constructor <clinit>()V
    .registers 1

    new-instance v0, Lcom/cooper/lithic/platform/notification/MilestoneScheduling;

    invoke-direct {v0}, Lcom/cooper/lithic/platform/notification/MilestoneScheduling;-><init>()V

    sput-object v0, Lcom/cooper/lithic/platform/notification/MilestoneScheduling;->INSTANCE:Lcom/cooper/lithic/platform/notification/MilestoneScheduling;

    return-void
.end method

.method private constructor <init>()V
    .registers 1

    .line 8
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static synthetic reconcile$app_debug$default(Lcom/cooper/lithic/platform/notification/MilestoneScheduling;Landroid/content/Context;Lcom/cooper/lithic/data/backup/ManagedGeneration;Lkotlin/jvm/functions/Function0;Ljava/time/Clock;Lcom/cooper/lithic/platform/notification/MilestonePublisher;Lkotlin/coroutines/Continuation;ILjava/lang/Object;)Ljava/lang/Object;
    .registers 16

    .line 12
    and-int/lit8 p8, p7, 0x8

    if-eqz p8, :cond_f

    .line 16
    invoke-static {}, Ljava/time/Clock;->systemUTC()Ljava/time/Clock;

    move-result-object p4

    const-string p8, "systemUTC(...)"

    invoke-static {p4, p8}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    move-object v4, p4

    goto :goto_10

    .line 12
    :cond_f
    move-object v4, p4

    :goto_10
    and-int/lit8 p4, p7, 0x10

    if-eqz p4, :cond_17

    .line 17
    const/4 p5, 0x0

    move-object v5, p5

    goto :goto_18

    .line 12
    :cond_17
    move-object v5, p5

    :goto_18
    move-object v0, p0

    move-object v1, p1

    move-object v2, p2

    move-object v3, p3

    move-object v6, p6

    invoke-virtual/range {v0 .. v6}, Lcom/cooper/lithic/platform/notification/MilestoneScheduling;->reconcile$app_debug(Landroid/content/Context;Lcom/cooper/lithic/data/backup/ManagedGeneration;Lkotlin/jvm/functions/Function0;Ljava/time/Clock;Lcom/cooper/lithic/platform/notification/MilestonePublisher;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;

    move-result-object p0

    return-object p0
.end method


# virtual methods
.method public final reconcile$app_debug(Landroid/content/Context;Lcom/cooper/lithic/data/backup/ManagedGeneration;Lkotlin/jvm/functions/Function0;Ljava/time/Clock;Lcom/cooper/lithic/platform/notification/MilestonePublisher;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    .registers 8
    .param p1, "context"    # Landroid/content/Context;
    .param p2, "generation"    # Lcom/cooper/lithic/data/backup/ManagedGeneration;
    .param p3, "isCurrent"    # Lkotlin/jvm/functions/Function0;
    .param p4, "clock"    # Ljava/time/Clock;
    .param p5, "publisher"    # Lcom/cooper/lithic/platform/notification/MilestonePublisher;
    .param p6, "$completion"    # Lkotlin/coroutines/Continuation;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Landroid/content/Context;",
            "Lcom/cooper/lithic/data/backup/ManagedGeneration;",
            "Lkotlin/jvm/functions/Function0<",
            "Ljava/lang/Boolean;",
            ">;",
            "Ljava/time/Clock;",
            "Lcom/cooper/lithic/platform/notification/MilestonePublisher;",
            "Lkotlin/coroutines/Continuation<",
            "-",
            "Lkotlin/Unit;",
            ">;)",
            "Ljava/lang/Object;"
        }
    .end annotation

    .line 18
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

