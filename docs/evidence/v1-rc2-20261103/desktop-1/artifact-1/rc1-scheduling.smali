
.method public final requestMilestones$app_debug(Landroid/content/Context;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    .registers 20
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

    move-object/from16 v0, p2

    instance-of v1, v0, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;

    if-eqz v1, :cond_18

    move-object v1, v0

    check-cast v1, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;

    iget v2, v1, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;->label:I

    const/high16 v3, -0x80000000

    and-int/2addr v2, v3

    if-eqz v2, :cond_18

    iget v2, v1, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;->label:I

    sub-int/2addr v2, v3

    iput v2, v1, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;->label:I

    move-object/from16 v2, p0

    goto :goto_1f

    :cond_18
    new-instance v1, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;

    move-object/from16 v2, p0

    invoke-direct {v1, v2, v0}, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;-><init>(Lcom/cooper/lithic/platform/notification/ReminderScheduling;Lkotlin/coroutines/Continuation;)V

    .local v1, "$continuation":Lkotlin/coroutines/Continuation;
    :goto_1f
    iget-object v3, v1, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;->result:Ljava/lang/Object;

    .local v3, "$result":Ljava/lang/Object;
    invoke-static {}, Lkotlin/coroutines/intrinsics/IntrinsicsKt;->getCOROUTINE_SUSPENDED()Ljava/lang/Object;

    move-result-object v4

    .line 39
    iget v5, v1, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;->label:I

    packed-switch v5, :pswitch_data_ca

    move-object/from16 v6, p1

    .end local v1    # "$continuation":Lkotlin/coroutines/Continuation;
    .end local v3    # "$result":Ljava/lang/Object;
    new-instance v0, Ljava/lang/IllegalStateException;

    const-string v1, "call to \'resume\' before \'invoke\' with coroutine"

    invoke-direct {v0, v1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw v0

    .restart local v1    # "$continuation":Lkotlin/coroutines/Continuation;
    .restart local v3    # "$result":Ljava/lang/Object;
    :pswitch_34
    iget v4, v1, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;->I$0:I

    .local v4, "$i$f$await\\3\\41":I
    iget-object v5, v1, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;->L$1:Ljava/lang/Object;

    check-cast v5, Landroidx/work/Operation;

    .local v5, "$this$await\\3":Landroidx/work/Operation;
    iget-object v6, v1, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;->L$0:Ljava/lang/Object;

    check-cast v6, Landroid/content/Context;

    .end local p1    # "context":Landroid/content/Context;
    .local v6, "context":Landroid/content/Context;
    invoke-static {v3}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V

    move-object v7, v3

    goto/16 :goto_c1

    .end local v4    # "$i$f$await\\3\\41":I
    .end local v5    # "$this$await\\3":Landroidx/work/Operation;
    .end local v6    # "context":Landroid/content/Context;
    .restart local p1    # "context":Landroid/content/Context;
    :pswitch_44
    invoke-static {v3}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V

    .line 40
    sget-object v5, Landroidx/work/WorkManager;->Companion:Landroidx/work/WorkManager$Companion;

    move-object/from16 v6, p1

    invoke-virtual {v5, v6}, Landroidx/work/WorkManager$Companion;->getInstance(Landroid/content/Context;)Landroidx/work/WorkManager;

    move-result-object v5

    sget-object v7, Landroidx/work/ExistingWorkPolicy;->REPLACE:Landroidx/work/ExistingWorkPolicy;

    .line 41
    const/4 v8, 0x0

    .line 95
    .local v8, "$i$f$OneTimeWorkRequestBuilder\\1\\41":I
    new-instance v9, Landroidx/work/OneTimeWorkRequest$Builder;

    const-class v10, Lcom/cooper/lithic/platform/notification/ReminderWorker;

    invoke-direct {v9, v10}, Landroidx/work/OneTimeWorkRequest$Builder;-><init>(Ljava/lang/Class;)V

    .line 41
    .end local v8    # "$i$f$OneTimeWorkRequestBuilder\\1\\41":I
    const/4 v8, 0x1

    new-array v10, v8, [Lkotlin/Pair;

    const-string v11, "mode"

    const-string v12, "milestone_reconcile"

    invoke-static {v11, v12}, Lkotlin/TuplesKt;->to(Ljava/lang/Object;Ljava/lang/Object;)Lkotlin/Pair;

    move-result-object v11

    const/4 v12, 0x0

    aput-object v11, v10, v12

    .local v10, "pairs\\2":[Lkotlin/Pair;
    const/4 v11, 0x0

    .line 96
    .local v11, "$i$f$workDataOf\\2\\41":I
    new-instance v13, Landroidx/work/Data$Builder;

    invoke-direct {v13}, Landroidx/work/Data$Builder;-><init>()V

    .line 97
    .local v13, "dataBuilder\\2":Landroidx/work/Data$Builder;
    array-length v14, v10

    :goto_6e
    if-ge v12, v14, :cond_87

    aget-object v15, v10, v12

    .line 98
    .local v15, "pair\\2":Lkotlin/Pair;
    invoke-virtual {v15}, Lkotlin/Pair;->getFirst()Ljava/lang/Object;

    move-result-object v16

    move-object/from16 v8, v16

    check-cast v8, Ljava/lang/String;

    invoke-virtual {v15}, Lkotlin/Pair;->getSecond()Ljava/lang/Object;

    move-result-object v0

    invoke-virtual {v13, v8, v0}, Landroidx/work/Data$Builder;->put(Ljava/lang/String;Ljava/lang/Object;)Landroidx/work/Data$Builder;

    .line 97
    .end local v15    # "pair\\2":Lkotlin/Pair;
    add-int/lit8 v12, v12, 0x1

    move-object/from16 v0, p2

    const/4 v8, 0x1

    goto :goto_6e

    .line 100
    :cond_87
    invoke-virtual {v13}, Landroidx/work/Data$Builder;->build()Landroidx/work/Data;

    move-result-object v0

    .line 41
    .end local v10    # "pairs\\2":[Lkotlin/Pair;
    .end local v11    # "$i$f$workDataOf\\2\\41":I
    .end local v13    # "dataBuilder\\2":Landroidx/work/Data$Builder;
    invoke-virtual {v9, v0}, Landroidx/work/OneTimeWorkRequest$Builder;->setInputData(Landroidx/work/Data;)Landroidx/work/WorkRequest$Builder;

    move-result-object v0

    check-cast v0, Landroidx/work/OneTimeWorkRequest$Builder;

    invoke-virtual {v0}, Landroidx/work/OneTimeWorkRequest$Builder;->build()Landroidx/work/WorkRequest;

    move-result-object v0

    check-cast v0, Landroidx/work/OneTimeWorkRequest;

    .line 40
    const-string v8, "lithic-milestone-reconcile-v1"

    invoke-virtual {v5, v8, v7, v0}, Landroidx/work/WorkManager;->enqueueUniqueWork(Ljava/lang/String;Landroidx/work/ExistingWorkPolicy;Landroidx/work/OneTimeWorkRequest;)Landroidx/work/Operation;

    move-result-object v0

    .line 41
    move-object v5, v0

    .restart local v5    # "$this$await\\3":Landroidx/work/Operation;
    const/4 v0, 0x0

    .line 101
    .local v0, "$i$f$await\\3\\41":I
    invoke-interface {v5}, Landroidx/work/Operation;->getResult()Lcom/google/common/util/concurrent/ListenableFuture;

    move-result-object v7

    const-string v8, "getResult(...)"

    invoke-static {v7, v8}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-static {v6}, Lkotlin/coroutines/jvm/internal/SpillingKt;->nullOutSpilledVariable(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v8

    iput-object v8, v1, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;->L$0:Ljava/lang/Object;

    invoke-static {v5}, Lkotlin/coroutines/jvm/internal/SpillingKt;->nullOutSpilledVariable(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v8

    iput-object v8, v1, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;->L$1:Ljava/lang/Object;

    iput v0, v1, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;->I$0:I

    const/4 v8, 0x1

    iput v8, v1, Lcom/cooper/lithic/platform/notification/ReminderScheduling$requestMilestones$1;->label:I

    invoke-static {v7, v1}, Landroidx/concurrent/futures/ListenableFutureKt;->await(Lcom/google/common/util/concurrent/ListenableFuture;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;

    move-result-object v7

    if-ne v7, v4, :cond_c0

    .line 39
    return-object v4

    .line 101
    :cond_c0
    move v4, v0

    .end local v0    # "$i$f$await\\3\\41":I
    .end local p1    # "context":Landroid/content/Context;
    .restart local v4    # "$i$f$await\\3\\41":I
    .restart local v6    # "context":Landroid/content/Context;
    :goto_c1
    const-string v0, "await(...)"

    invoke-static {v7, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    .end local v4    # "$i$f$await\\3\\41":I
    .end local v5    # "$this$await\\3":Landroidx/work/Operation;
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    .line 42
    return-object v0

    nop

    :pswitch_data_ca
    .packed-switch 0x0
        :pswitch_44
        :pswitch_34
    .end packed-switch
.end method
