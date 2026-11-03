.class public final Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt;
.super Ljava/lang/Object;
.source "ReminderReconciliation.kt"


# annotations
.annotation runtime Lkotlin/Metadata;
    d1 = {
        "\u0000\u0018\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0008\u0003\u001aJ\u0010\u0000\u001a\u00020\u00012\u001c\u0010\u0002\u001a\u0018\u0008\u0001\u0012\n\u0012\u0008\u0012\u0004\u0012\u00020\u00010\u0004\u0012\u0006\u0012\u0004\u0018\u00010\u00050\u00032\u001c\u0010\u0006\u001a\u0018\u0008\u0001\u0012\n\u0012\u0008\u0012\u0004\u0012\u00020\u00010\u0004\u0012\u0006\u0012\u0004\u0018\u00010\u00050\u0003H\u0080@\u00a2\u0006\u0002\u0010\u0007\u00a8\u0006\u0008"
    }
    d2 = {
        "reconcileReminderDomains",
        "",
        "requestMilestones",
        "Lkotlin/Function1;",
        "Lkotlin/coroutines/Continuation;",
        "",
        "reconcileRenewals",
        "(Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;",
        "app_debug"
    }
    k = 0x2
    mv = {
        0x2,
        0x3,
        0x0
    }
    xi = 0x30
.end annotation


# direct methods
.method public static final reconcileReminderDomains(Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    .registers 10
    .param p0, "requestMilestones"    # Lkotlin/jvm/functions/Function1;
    .param p1, "reconcileRenewals"    # Lkotlin/jvm/functions/Function1;
    .param p2, "$completion"    # Lkotlin/coroutines/Continuation;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Lkotlin/jvm/functions/Function1<",
            "-",
            "Lkotlin/coroutines/Continuation<",
            "-",
            "Lkotlin/Unit;",
            ">;+",
            "Ljava/lang/Object;",
            ">;",
            "Lkotlin/jvm/functions/Function1<",
            "-",
            "Lkotlin/coroutines/Continuation<",
            "-",
            "Lkotlin/Unit;",
            ">;+",
            "Ljava/lang/Object;",
            ">;",
            "Lkotlin/coroutines/Continuation<",
            "-",
            "Lkotlin/Unit;",
            ">;)",
            "Ljava/lang/Object;"
        }
    .end annotation

    instance-of v0, p2, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;

    if-eqz v0, :cond_14

    move-object v0, p2

    check-cast v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;

    iget v1, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->label:I

    const/high16 v2, -0x80000000

    and-int/2addr v1, v2

    if-eqz v1, :cond_14

    iget v1, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->label:I

    sub-int/2addr v1, v2

    iput v1, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->label:I

    goto :goto_19

    :cond_14
    new-instance v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;

    invoke-direct {v0, p2}, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;-><init>(Lkotlin/coroutines/Continuation;)V

    .local v0, "$continuation":Lkotlin/coroutines/Continuation;
    :goto_19
    iget-object v1, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->result:Ljava/lang/Object;

    .local v1, "$result":Ljava/lang/Object;
    invoke-static {}, Lkotlin/coroutines/intrinsics/IntrinsicsKt;->getCOROUTINE_SUSPENDED()Ljava/lang/Object;

    move-result-object v2

    .line 7
    iget v3, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->label:I

    packed-switch v3, :pswitch_data_b4

    .end local v0    # "$continuation":Lkotlin/coroutines/Continuation;
    .end local v1    # "$result":Ljava/lang/Object;
    new-instance v0, Ljava/lang/IllegalStateException;

    const-string v1, "call to \'resume\' before \'invoke\' with coroutine"

    invoke-direct {v0, v1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw v0

    .restart local v0    # "$continuation":Lkotlin/coroutines/Continuation;
    .restart local v1    # "$result":Ljava/lang/Object;
    :pswitch_2c
    iget-object v2, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->L$2:Ljava/lang/Object;

    check-cast v2, Ljava/lang/Exception;

    .local v2, "renewalFailure":Ljava/lang/Exception;
    iget-object v3, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->L$1:Ljava/lang/Object;

    move-object p1, v3

    check-cast p1, Lkotlin/jvm/functions/Function1;

    iget-object v3, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->L$0:Ljava/lang/Object;

    move-object p0, v3

    check-cast p0, Lkotlin/jvm/functions/Function1;

    :try_start_3a
    invoke-static {v1}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V
    :try_end_3d
    .catch Ljava/util/concurrent/CancellationException; {:try_start_3a .. :try_end_3d} :catch_40
    .catch Ljava/lang/Exception; {:try_start_3a .. :try_end_3d} :catch_3e

    goto :goto_85

    .line 24
    :catch_3e
    move-exception v3

    goto :goto_90

    .line 21
    :catch_40
    move-exception v3

    goto :goto_a3

    .line 7
    .end local v2    # "renewalFailure":Ljava/lang/Exception;
    :pswitch_42
    iget-object v3, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->L$1:Ljava/lang/Object;

    move-object p1, v3

    check-cast p1, Lkotlin/jvm/functions/Function1;

    iget-object v3, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->L$0:Ljava/lang/Object;

    move-object p0, v3

    check-cast p0, Lkotlin/jvm/functions/Function1;

    :try_start_4c
    invoke-static {v1}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V
    :try_end_4f
    .catch Ljava/util/concurrent/CancellationException; {:try_start_4c .. :try_end_4f} :catch_b1
    .catch Ljava/lang/Exception; {:try_start_4c .. :try_end_4f} :catch_68

    goto :goto_66

    :pswitch_50
    invoke-static {v1}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V

    .line 11
    nop

    .line 12
    :try_start_54
    iput-object p0, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->L$0:Ljava/lang/Object;

    invoke-static {p1}, Lkotlin/coroutines/jvm/internal/SpillingKt;->nullOutSpilledVariable(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    iput-object v3, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->L$1:Ljava/lang/Object;

    const/4 v3, 0x1

    iput v3, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->label:I

    invoke-interface {p1, v0}, Lkotlin/jvm/functions/Function1;->invoke(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3
    :try_end_63
    .catch Ljava/util/concurrent/CancellationException; {:try_start_54 .. :try_end_63} :catch_b1
    .catch Ljava/lang/Exception; {:try_start_54 .. :try_end_63} :catch_68

    if-ne v3, v2, :cond_66

    .line 7
    return-object v2

    .line 13
    :cond_66
    :goto_66
    const/4 v3, 0x0

    goto :goto_6a

    .line 16
    :catch_68
    move-exception v3

    .line 17
    .local v3, "failure":Ljava/lang/Exception;
    nop

    .line 11
    .end local v3    # "failure":Ljava/lang/Exception;
    :goto_6a
    nop

    .line 19
    .local v3, "renewalFailure":Ljava/lang/Exception;
    nop

    .line 20
    :try_start_6c
    invoke-static {p0}, Lkotlin/coroutines/jvm/internal/SpillingKt;->nullOutSpilledVariable(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    iput-object v4, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->L$0:Ljava/lang/Object;

    invoke-static {p1}, Lkotlin/coroutines/jvm/internal/SpillingKt;->nullOutSpilledVariable(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    iput-object v4, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->L$1:Ljava/lang/Object;

    iput-object v3, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->L$2:Ljava/lang/Object;

    const/4 v4, 0x2

    iput v4, v0, Lcom/cooper/lithic/platform/notification/ReminderReconciliationKt$reconcileReminderDomains$1;->label:I

    invoke-interface {p0, v0}, Lkotlin/jvm/functions/Function1;->invoke(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4
    :try_end_81
    .catch Ljava/util/concurrent/CancellationException; {:try_start_6c .. :try_end_81} :catch_9f
    .catch Ljava/lang/Exception; {:try_start_6c .. :try_end_81} :catch_8c

    if-ne v4, v2, :cond_84

    .line 7
    return-object v2

    .line 20
    :cond_84
    move-object v2, v3

    .end local v3    # "renewalFailure":Ljava/lang/Exception;
    .restart local v2    # "renewalFailure":Ljava/lang/Exception;
    :goto_85
    nop

    .line 31
    if-nez v2, :cond_8b

    .line 32
    sget-object v3, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v3

    .line 31
    :cond_8b
    throw v2

    .line 24
    .end local v2    # "renewalFailure":Ljava/lang/Exception;
    .restart local v3    # "renewalFailure":Ljava/lang/Exception;
    :catch_8c
    move-exception v2

    move-object v6, v3

    move-object v3, v2

    move-object v2, v6

    .line 25
    .restart local v2    # "renewalFailure":Ljava/lang/Exception;
    .local v3, "failure":Ljava/lang/Exception;
    :goto_90
    if-eqz v2, :cond_9e

    .line 26
    if-eq v2, v3, :cond_9d

    move-object v4, v2

    check-cast v4, Ljava/lang/Throwable;

    move-object v5, v3

    check-cast v5, Ljava/lang/Throwable;

    invoke-static {v4, v5}, Lkotlin/ExceptionsKt;->addSuppressed(Ljava/lang/Throwable;Ljava/lang/Throwable;)V

    .line 27
    :cond_9d
    throw v2

    .line 29
    :cond_9e
    throw v3

    .line 21
    .end local v2    # "renewalFailure":Ljava/lang/Exception;
    .local v3, "renewalFailure":Ljava/lang/Exception;
    :catch_9f
    move-exception v2

    move-object v6, v3

    move-object v3, v2

    move-object v2, v6

    .line 22
    .restart local v2    # "renewalFailure":Ljava/lang/Exception;
    .local v3, "cancelled":Ljava/util/concurrent/CancellationException;
    :goto_a3
    if-eqz v2, :cond_b0

    if-eq v2, v3, :cond_b0

    move-object v4, v3

    check-cast v4, Ljava/lang/Throwable;

    move-object v5, v2

    check-cast v5, Ljava/lang/Throwable;

    invoke-static {v4, v5}, Lkotlin/ExceptionsKt;->addSuppressed(Ljava/lang/Throwable;Ljava/lang/Throwable;)V

    .line 23
    :cond_b0
    throw v3

    .line 14
    .end local v2    # "renewalFailure":Ljava/lang/Exception;
    .end local v3    # "cancelled":Ljava/util/concurrent/CancellationException;
    :catch_b1
    move-exception v2

    .line 15
    .local v2, "cancelled":Ljava/util/concurrent/CancellationException;
    throw v2

    nop

    :pswitch_data_b4
    .packed-switch 0x0
        :pswitch_50
        :pswitch_42
        :pswitch_2c
    .end packed-switch
.end method

