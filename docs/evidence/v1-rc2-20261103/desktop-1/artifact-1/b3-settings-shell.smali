.class public final Lcom/cooper/lithic/feature/settings/SettingsShellKt;
.super Ljava/lang/Object;
.source "SettingsShell.kt"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/cooper/lithic/feature/settings/SettingsShellKt$WhenMappings;
    }
.end annotation

.annotation system Ldalvik/annotation/SourceDebugExtension;
    value = "SMAP\nSettingsShell.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SettingsShell.kt\ncom/cooper/lithic/feature/settings/SettingsShellKt\n+ 2 Composer.kt\nandroidx/compose/runtime/ComposerKt\n+ 3 Dp.kt\nandroidx/compose/ui/unit/DpKt\n+ 4 Column.kt\nandroidx/compose/foundation/layout/ColumnKt\n+ 5 Layout.kt\nandroidx/compose/ui/layout/LayoutKt\n+ 6 Composables.kt\nandroidx/compose/runtime/ComposablesKt\n+ 7 Composer.kt\nandroidx/compose/runtime/Updater\n+ 8 Row.kt\nandroidx/compose/foundation/layout/RowKt\n+ 9 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 10 SnapshotState.kt\nandroidx/compose/runtime/SnapshotStateKt__SnapshotStateKt\n*L\n1#1,158:1\n1277#2,6:159\n1277#2,6:165\n1277#2,6:171\n1277#2,6:177\n1277#2,6:183\n1277#2,6:189\n1277#2,6:270\n1277#2,6:276\n1277#2,6:290\n1277#2,6:296\n1277#2,6:303\n1277#2,6:309\n1277#2,6:320\n113#3:195\n113#3:196\n113#3:302\n113#3:315\n113#3:326\n87#4:197\n85#4,8:198\n94#4:289\n87#4:327\n85#4,8:328\n94#4:366\n80#5,6:206\n87#5,3:221\n90#5,2:230\n80#5,6:243\n87#5,3:258\n90#5,2:267\n94#5:284\n94#5:288\n80#5,6:336\n87#5,3:351\n90#5,2:360\n94#5:365\n391#6,9:212\n400#6:232\n391#6,9:249\n400#6:269\n401#6,2:282\n401#6,2:286\n391#6,9:342\n400#6,3:362\n4354#7,6:224\n4354#7,6:261\n4354#7,6:354\n99#8:233\n96#8,9:234\n106#8:285\n1#9:316\n85#10:317\n117#10,2:318\n*S KotlinDebug\n*F\n+ 1 SettingsShell.kt\ncom/cooper/lithic/feature/settings/SettingsShellKt\n*L\n39#1:159,6\n42#1:165,6\n43#1:171,6\n44#1:177,6\n51#1:183,6\n55#1:189,6\n60#1:270,6\n61#1:276,6\n141#1:290,6\n86#1:296,6\n101#1:303,6\n114#1:309,6\n156#1:320,6\n56#1:195\n57#1:196\n89#1:302\n117#1:315\n145#1:326\n56#1:197\n56#1:198,8\n56#1:289\n144#1:327\n144#1:328,8\n144#1:366\n56#1:206,6\n56#1:221,3\n56#1:230,2\n58#1:243,6\n58#1:258,3\n58#1:267,2\n58#1:284\n56#1:288\n144#1:336,6\n144#1:351,3\n144#1:360,2\n144#1:365\n56#1:212,9\n56#1:232\n58#1:249,9\n58#1:269\n58#1:282,2\n56#1:286,2\n144#1:342,9\n144#1:362,3\n56#1:224,6\n58#1:261,6\n144#1:354,6\n58#1:233\n58#1:234,9\n58#1:285\n55#1:317\n55#1:318,2\n*E\n"
.end annotation

.annotation runtime Lkotlin/Metadata;
    d1 = {
        "\u0000>\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0008\u0002\n\u0002\u0018\u0002\n\u0002\u0008\t\n\u0002\u0018\u0002\n\u0002\u0008\u0003\n\u0002\u0018\u0002\n\u0002\u0008\u0004\n\u0002\u0010\u000b\u001a\u00a7\u0002\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0008\u0008\u0002\u0010\u0004\u001a\u00020\u00052\u0008\u0008\u0002\u0010\u0006\u001a\u00020\u00072\u0010\u0008\u0002\u0010\u0008\u001a\n\u0012\u0004\u0012\u00020\u0001\u0018\u00010\t2\u000e\u0008\u0002\u0010\n\u001a\u0008\u0012\u0004\u0012\u00020\u00010\t2\u0008\u0008\u0002\u0010\u000b\u001a\u00020\u000c2\u0010\u0008\u0002\u0010\r\u001a\n\u0012\u0004\u0012\u00020\u0001\u0018\u00010\t2\u000e\u0008\u0002\u0010\u000e\u001a\u0008\u0012\u0004\u0012\u00020\u00010\t2\u000e\u0008\u0002\u0010\u000f\u001a\u0008\u0012\u0004\u0012\u00020\u00010\t2\u000e\u0008\u0002\u0010\u0010\u001a\u0008\u0012\u0004\u0012\u00020\u00010\t2\u0010\u0008\u0002\u0010\u0011\u001a\n\u0012\u0004\u0012\u00020\u0001\u0018\u00010\t2\u0010\u0008\u0002\u0010\u0012\u001a\n\u0012\u0004\u0012\u00020\u0001\u0018\u00010\t2\u0010\u0008\u0002\u0010\u0013\u001a\n\u0012\u0004\u0012\u00020\u0001\u0018\u00010\t2\u0010\u0008\u0002\u0010\u0014\u001a\n\u0012\u0004\u0012\u00020\u0001\u0018\u00010\t2\u0008\u0008\u0002\u0010\u0015\u001a\u00020\u00162\u0010\u0008\u0002\u0010\u0017\u001a\n\u0012\u0004\u0012\u00020\u0001\u0018\u00010\t2\u000e\u0008\u0002\u0010\u0018\u001a\u0008\u0012\u0004\u0012\u00020\u00010\t2\u0008\u0008\u0002\u0010\u0019\u001a\u00020\u001a2\u0010\u0008\u0002\u0010\u001b\u001a\n\u0012\u0004\u0012\u00020\u0001\u0018\u00010\tH\u0001\u00a2\u0006\u0002\u0010\u001c\u00a8\u0006\u001d\u00b2\u0006\n\u0010\u001e\u001a\u00020\u001fX\u008a\u008e\u0002"
    }
    d2 = {
        "SettingsShell",
        "",
        "recordZone",
        "Ljava/time/ZoneId;",
        "modifier",
        "Landroidx/compose/ui/Modifier;",
        "backupState",
        "Lcom/cooper/lithic/feature/settings/BackupExportUiState;",
        "onExportBackup",
        "Lkotlin/Function0;",
        "onCancelBackup",
        "restoreState",
        "Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;",
        "onRestoreBackup",
        "onCancelRestore",
        "onConfirmRestore",
        "onDiscardRestore",
        "onManageCategories",
        "onNotificationClick",
        "onCalculationExplanation",
        "onAboutLithic",
        "inspectionState",
        "Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;",
        "onInspectBackup",
        "onCancelInspection",
        "photoStorageState",
        "Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;",
        "onRefreshPhotoStorage",
        "(Ljava/time/ZoneId;Landroidx/compose/ui/Modifier;Lcom/cooper/lithic/feature/settings/BackupExportUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;Lkotlin/jvm/functions/Function0;Landroidx/compose/runtime/Composer;III)V",
        "app_debug",
        "informationOpen",
        ""
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
.method public static synthetic $r8$lambda$5L8Qf6ecLpOVYYCmZ2pmXNrSgyk(Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Landroidx/compose/foundation/layout/ColumnScope;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 5

    invoke-static {p0, p1, p2, p3, p4}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$8$4(Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Landroidx/compose/foundation/layout/ColumnScope;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;

    move-result-object p0

    return-object p0
.end method

.method public static synthetic $r8$lambda$66teYXJfxgl-vjGgduw0GEeScfo()Landroidx/compose/runtime/MutableState;
    .registers 1

    invoke-static {}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$5$0()Landroidx/compose/runtime/MutableState;

    move-result-object v0

    return-object v0
.end method

.method public static synthetic $r8$lambda$7-H_4TFuURD-8GIKYarElYhBt68(Landroidx/compose/runtime/MutableState;)Lkotlin/Unit;
    .registers 1

    invoke-static {p0}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$8$0$1$0(Landroidx/compose/runtime/MutableState;)Lkotlin/Unit;

    move-result-object p0

    return-object p0
.end method

.method public static synthetic $r8$lambda$J1oq-IGWnxoE5mH7Hc-xAkKJYrU(Lcom/cooper/lithic/feature/settings/BackupExportUiState;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Landroidx/compose/foundation/layout/ColumnScope;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 14

    invoke-static/range {p0 .. p13}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$8$3(Lcom/cooper/lithic/feature/settings/BackupExportUiState;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Landroidx/compose/foundation/layout/ColumnScope;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;

    move-result-object p0

    return-object p0
.end method

.method public static synthetic $r8$lambda$Q7qU8CAlU36GZsCSv4q8GVwpPjM(Lkotlin/jvm/functions/Function0;)Lkotlin/Unit;
    .registers 1

    invoke-static {p0}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$8$3$2$0(Lkotlin/jvm/functions/Function0;)Lkotlin/Unit;

    move-result-object p0

    return-object p0
.end method

.method public static synthetic $r8$lambda$QmQkIVFyO3z6yHhIpk88Y5Iav7Y(Landroidx/compose/runtime/MutableState;)Lkotlin/Unit;
    .registers 1

    invoke-static {p0}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$10$0$0(Landroidx/compose/runtime/MutableState;)Lkotlin/Unit;

    move-result-object p0

    return-object p0
.end method

.method public static synthetic $r8$lambda$TaclcnomNC8hJq50QYtBHlO31i8(Lkotlin/jvm/functions/Function0;)Lkotlin/Unit;
    .registers 1

    invoke-static {p0}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$8$3$0$0(Lkotlin/jvm/functions/Function0;)Lkotlin/Unit;

    move-result-object p0

    return-object p0
.end method

.method public static synthetic $r8$lambda$U-IXtWv3oyuZ3nxVOP9VP1hE6O8()Lkotlin/Unit;
    .registers 1

    invoke-static {}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$3$0()Lkotlin/Unit;

    move-result-object v0

    return-object v0
.end method

.method public static synthetic $r8$lambda$VNq3_gABuXxrB_VpHLS5qrI_WA4(Landroidx/compose/ui/semantics/SemanticsPropertyReceiver;)Lkotlin/Unit;
    .registers 1

    invoke-static {p0}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$8$0$0$0(Landroidx/compose/ui/semantics/SemanticsPropertyReceiver;)Lkotlin/Unit;

    move-result-object p0

    return-object p0
.end method

.method public static synthetic $r8$lambda$ZM3TrAAzmiBjj-O1FYMaF707po4(Lcom/cooper/lithic/feature/settings/BackupExportUiState;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 3

    invoke-static {p0, p1, p2}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$11(Lcom/cooper/lithic/feature/settings/BackupExportUiState;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;

    move-result-object p0

    return-object p0
.end method

.method public static synthetic $r8$lambda$dHDLbbLnuW4iNIlCpYsmUFqgHoY(Landroidx/compose/runtime/MutableState;)Lkotlin/Unit;
    .registers 1

    invoke-static {p0}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$9$0(Landroidx/compose/runtime/MutableState;)Lkotlin/Unit;

    move-result-object p0

    return-object p0
.end method

.method public static synthetic $r8$lambda$gN_yeYjum1PGEwWjEa_FEn8IPWI(Lkotlin/jvm/functions/Function0;Landroidx/compose/foundation/layout/ColumnScope;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 4

    invoke-static {p0, p1, p2, p3}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$8$2$0(Lkotlin/jvm/functions/Function0;Landroidx/compose/foundation/layout/ColumnScope;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;

    move-result-object p0

    return-object p0
.end method

.method public static synthetic $r8$lambda$hWYCG22Eljd6-NZ9cjl4-b0hp9w()Lkotlin/Unit;
    .registers 1

    invoke-static {}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$1$0()Lkotlin/Unit;

    move-result-object v0

    return-object v0
.end method

.method public static synthetic $r8$lambda$lNxz33QNELRT5FbY7j9nBKi-nFI(Landroidx/compose/runtime/MutableState;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 3

    invoke-static {p0, p1, p2}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$10(Landroidx/compose/runtime/MutableState;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;

    move-result-object p0

    return-object p0
.end method

.method public static synthetic $r8$lambda$mKLuyekyp9ggEo_kti02mj7-Plk()Lkotlin/Unit;
    .registers 1

    invoke-static {}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$2$0()Lkotlin/Unit;

    move-result-object v0

    return-object v0
.end method

.method public static synthetic $r8$lambda$o_OeT3w8R5OzQHDjmGr8FcQjrmA()Lkotlin/Unit;
    .registers 1

    invoke-static {}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$0$0()Lkotlin/Unit;

    move-result-object v0

    return-object v0
.end method

.method public static synthetic $r8$lambda$txYTOwvmN9KMHChmfwCV6VFRdJs()Lkotlin/Unit;
    .registers 1

    invoke-static {}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$4$0()Lkotlin/Unit;

    move-result-object v0

    return-object v0
.end method

.method public static synthetic $r8$lambda$uWMZH5-3Ry_xn8to6F1eMyLgHzY(Lkotlin/jvm/functions/Function0;)Lkotlin/Unit;
    .registers 1

    invoke-static {p0}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$8$3$3$0(Lkotlin/jvm/functions/Function0;)Lkotlin/Unit;

    move-result-object p0

    return-object p0
.end method

.method public static synthetic $r8$lambda$vfGauZWiR59c_rO5VS5hwb-wT5w(Ljava/time/ZoneId;Landroidx/compose/ui/Modifier;Lcom/cooper/lithic/feature/settings/BackupExportUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;Lkotlin/jvm/functions/Function0;IIILandroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 24

    invoke-static/range {p0 .. p23}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$12(Ljava/time/ZoneId;Landroidx/compose/ui/Modifier;Lcom/cooper/lithic/feature/settings/BackupExportUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;Lkotlin/jvm/functions/Function0;IIILandroidx/compose/runtime/Composer;I)Lkotlin/Unit;

    move-result-object p0

    return-object p0
.end method

.method public static synthetic $r8$lambda$wq9QpeE-W6D-7k7N51MOouuSA-U(Lkotlin/jvm/functions/Function0;Ljava/time/ZoneId;Landroidx/compose/foundation/layout/ColumnScope;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 5

    invoke-static {p0, p1, p2, p3, p4}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$8$1(Lkotlin/jvm/functions/Function0;Ljava/time/ZoneId;Landroidx/compose/foundation/layout/ColumnScope;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;

    move-result-object p0

    return-object p0
.end method

.method public static final SettingsShell(Ljava/time/ZoneId;Landroidx/compose/ui/Modifier;Lcom/cooper/lithic/feature/settings/BackupExportUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;Lkotlin/jvm/functions/Function0;Landroidx/compose/runtime/Composer;III)V
    .registers 107
    .param p0, "recordZone"    # Ljava/time/ZoneId;
    .param p1, "modifier"    # Landroidx/compose/ui/Modifier;
    .param p2, "backupState"    # Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    .param p3, "onExportBackup"    # Lkotlin/jvm/functions/Function0;
    .param p4, "onCancelBackup"    # Lkotlin/jvm/functions/Function0;
    .param p5, "restoreState"    # Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;
    .param p6, "onRestoreBackup"    # Lkotlin/jvm/functions/Function0;
    .param p7, "onCancelRestore"    # Lkotlin/jvm/functions/Function0;
    .param p8, "onConfirmRestore"    # Lkotlin/jvm/functions/Function0;
    .param p9, "onDiscardRestore"    # Lkotlin/jvm/functions/Function0;
    .param p10, "onManageCategories"    # Lkotlin/jvm/functions/Function0;
    .param p11, "onNotificationClick"    # Lkotlin/jvm/functions/Function0;
    .param p12, "onCalculationExplanation"    # Lkotlin/jvm/functions/Function0;
    .param p13, "onAboutLithic"    # Lkotlin/jvm/functions/Function0;
    .param p14, "inspectionState"    # Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .param p15, "onInspectBackup"    # Lkotlin/jvm/functions/Function0;
    .param p16, "onCancelInspection"    # Lkotlin/jvm/functions/Function0;
    .param p17, "photoStorageState"    # Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .param p18, "onRefreshPhotoStorage"    # Lkotlin/jvm/functions/Function0;
    .param p19, "$composer"    # Landroidx/compose/runtime/Composer;
    .param p20, "$changed"    # I
    .param p21, "$changed1"    # I
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/time/ZoneId;",
            "Landroidx/compose/ui/Modifier;",
            "Lcom/cooper/lithic/feature/settings/BackupExportUiState;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;",
            "Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;",
            "Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;",
            "Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;",
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;",
            "Landroidx/compose/runtime/Composer;",
            "III)V"
        }
    .end annotation

    move-object/from16 v1, p0

    move-object/from16 v0, p17

    move/from16 v2, p20

    move/from16 v3, p21

    move/from16 v4, p22

    const-string v5, "recordZone"

    invoke-static {v1, v5}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    .line 54
    const v5, -0x7eac307c

    move-object/from16 v6, p19

    invoke-interface {v6, v5}, Landroidx/compose/runtime/Composer;->startRestartGroup(I)Landroidx/compose/runtime/Composer;

    move-result-object v5

    .end local p19    # "$composer":Landroidx/compose/runtime/Composer;
    .local v5, "$composer":Landroidx/compose/runtime/Composer;
    const-string v6, "C(SettingsShell)N(recordZone,modifier,backupState,onExportBackup,onCancelBackup,restoreState,onRestoreBackup,onCancelRestore,onConfirmRestore,onDiscardRestore,onManageCategories,onNotificationClick,onCalculationExplanation,onAboutLithic,inspectionState,onInspectBackup,onCancelInspection,photoStorageState,onRefreshPhotoStorage)54@2409L25,54@2392L42,55@2484L21,55@2542L4,55@2439L5030:SettingsShell.kt#e7edxl"

    invoke-static {v5, v6}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    move/from16 v6, p20

    .local v6, "$dirty":I
    move/from16 v7, p21

    .local v7, "$dirty1":I
    and-int/lit8 v8, v2, 0x6

    if-nez v8, :cond_2f

    invoke-interface {v5, v1}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v8

    if-eqz v8, :cond_2d

    const/4 v8, 0x4

    goto :goto_2e

    :cond_2d
    const/4 v8, 0x2

    :goto_2e
    or-int/2addr v6, v8

    :cond_2f
    and-int/lit8 v8, v4, 0x2

    if-eqz v8, :cond_38

    or-int/lit8 v6, v6, 0x30

    move-object/from16 v13, p1

    goto :goto_4d

    :cond_38
    and-int/lit8 v13, v2, 0x30

    if-nez v13, :cond_4b

    move-object/from16 v13, p1

    invoke-interface {v5, v13}, Landroidx/compose/runtime/Composer;->changed(Ljava/lang/Object;)Z

    move-result v14

    if-eqz v14, :cond_47

    const/16 v14, 0x20

    goto :goto_49

    :cond_47
    const/16 v14, 0x10

    :goto_49
    or-int/2addr v6, v14

    goto :goto_4d

    :cond_4b
    move-object/from16 v13, p1

    :goto_4d
    and-int/lit16 v14, v2, 0x180

    const/16 v16, 0x80

    if-nez v14, :cond_69

    and-int/lit8 v14, v4, 0x4

    if-nez v14, :cond_62

    move-object/from16 v14, p2

    invoke-interface {v5, v14}, Landroidx/compose/runtime/Composer;->changed(Ljava/lang/Object;)Z

    move-result v17

    if-eqz v17, :cond_64

    const/16 v17, 0x100

    goto :goto_66

    :cond_62
    move-object/from16 v14, p2

    :cond_64
    move/from16 v17, v16

    :goto_66
    or-int v6, v6, v17

    goto :goto_6b

    :cond_69
    move-object/from16 v14, p2

    :goto_6b
    and-int/lit8 v17, v4, 0x8

    if-eqz v17, :cond_74

    or-int/lit16 v6, v6, 0xc00

    move-object/from16 v9, p3

    goto :goto_8a

    :cond_74
    and-int/lit16 v9, v2, 0xc00

    if-nez v9, :cond_88

    move-object/from16 v9, p3

    invoke-interface {v5, v9}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v18

    if-eqz v18, :cond_83

    const/16 v18, 0x800

    goto :goto_85

    :cond_83
    const/16 v18, 0x400

    :goto_85
    or-int v6, v6, v18

    goto :goto_8a

    :cond_88
    move-object/from16 v9, p3

    :goto_8a
    and-int/lit8 v18, v4, 0x10

    const/16 v19, 0x4000

    if-eqz v18, :cond_95

    or-int/lit16 v6, v6, 0x6000

    move-object/from16 v10, p4

    goto :goto_ab

    :cond_95
    and-int/lit16 v10, v2, 0x6000

    if-nez v10, :cond_a9

    move-object/from16 v10, p4

    invoke-interface {v5, v10}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v21

    if-eqz v21, :cond_a4

    move/from16 v21, v19

    goto :goto_a6

    :cond_a4
    const/16 v21, 0x2000

    :goto_a6
    or-int v6, v6, v21

    goto :goto_ab

    :cond_a9
    move-object/from16 v10, p4

    :goto_ab
    const/high16 v21, 0x30000

    and-int v21, v2, v21

    if-nez v21, :cond_c7

    and-int/lit8 v21, v4, 0x20

    if-nez v21, :cond_c0

    move-object/from16 v11, p5

    invoke-interface {v5, v11}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v22

    if-eqz v22, :cond_c2

    const/high16 v22, 0x20000

    goto :goto_c4

    :cond_c0
    move-object/from16 v11, p5

    :cond_c2
    const/high16 v22, 0x10000

    :goto_c4
    or-int v6, v6, v22

    goto :goto_c9

    :cond_c7
    move-object/from16 v11, p5

    :goto_c9
    and-int/lit8 v22, v4, 0x40

    const/high16 v23, 0x180000

    if-eqz v22, :cond_d4

    or-int v6, v6, v23

    move-object/from16 v12, p6

    goto :goto_ea

    :cond_d4
    and-int v24, v2, v23

    if-nez v24, :cond_e8

    move-object/from16 v12, p6

    invoke-interface {v5, v12}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v25

    if-eqz v25, :cond_e3

    const/high16 v25, 0x100000

    goto :goto_e5

    :cond_e3
    const/high16 v25, 0x80000

    :goto_e5
    or-int v6, v6, v25

    goto :goto_ea

    :cond_e8
    move-object/from16 v12, p6

    :goto_ea
    and-int/lit16 v15, v4, 0x80

    const/high16 v26, 0xc00000

    if-eqz v15, :cond_f5

    or-int v6, v6, v26

    move-object/from16 v2, p7

    goto :goto_10b

    :cond_f5
    and-int v27, v2, v26

    if-nez v27, :cond_109

    move-object/from16 v2, p7

    invoke-interface {v5, v2}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v27

    if-eqz v27, :cond_104

    const/high16 v27, 0x800000

    goto :goto_106

    :cond_104
    const/high16 v27, 0x400000

    :goto_106
    or-int v6, v6, v27

    goto :goto_10b

    :cond_109
    move-object/from16 v2, p7

    :goto_10b
    and-int/lit16 v2, v4, 0x100

    const/high16 v27, 0x6000000

    if-eqz v2, :cond_118

    or-int v6, v6, v27

    move/from16 v28, v2

    move-object/from16 v2, p8

    goto :goto_132

    :cond_118
    and-int v28, p20, v27

    if-nez v28, :cond_12e

    move/from16 v28, v2

    move-object/from16 v2, p8

    invoke-interface {v5, v2}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v29

    if-eqz v29, :cond_129

    const/high16 v29, 0x4000000

    goto :goto_12b

    :cond_129
    const/high16 v29, 0x2000000

    :goto_12b
    or-int v6, v6, v29

    goto :goto_132

    :cond_12e
    move/from16 v28, v2

    move-object/from16 v2, p8

    :goto_132
    and-int/lit16 v2, v4, 0x200

    const/high16 v29, 0x30000000

    if-eqz v2, :cond_13f

    or-int v6, v6, v29

    move/from16 v29, v2

    move-object/from16 v2, p9

    goto :goto_159

    :cond_13f
    and-int v29, p20, v29

    if-nez v29, :cond_155

    move/from16 v29, v2

    move-object/from16 v2, p9

    invoke-interface {v5, v2}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v30

    if-eqz v30, :cond_150

    const/high16 v30, 0x20000000

    goto :goto_152

    :cond_150
    const/high16 v30, 0x10000000

    :goto_152
    or-int v6, v6, v30

    goto :goto_159

    :cond_155
    move/from16 v29, v2

    move-object/from16 v2, p9

    :goto_159
    and-int/lit16 v2, v4, 0x400

    if-eqz v2, :cond_164

    or-int/lit8 v7, v7, 0x6

    move/from16 v30, v2

    move-object/from16 v2, p10

    goto :goto_17e

    :cond_164
    and-int/lit8 v30, v3, 0x6

    if-nez v30, :cond_17a

    move/from16 v30, v2

    move-object/from16 v2, p10

    invoke-interface {v5, v2}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v31

    if-eqz v31, :cond_175

    const/16 v20, 0x4

    goto :goto_177

    :cond_175
    const/16 v20, 0x2

    :goto_177
    or-int v7, v7, v20

    goto :goto_17e

    :cond_17a
    move/from16 v30, v2

    move-object/from16 v2, p10

    :goto_17e
    and-int/lit16 v2, v4, 0x800

    if-eqz v2, :cond_189

    or-int/lit8 v7, v7, 0x30

    move/from16 v20, v2

    move-object/from16 v2, p11

    goto :goto_1a3

    :cond_189
    and-int/lit8 v20, v3, 0x30

    if-nez v20, :cond_19f

    move/from16 v20, v2

    move-object/from16 v2, p11

    invoke-interface {v5, v2}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v31

    if-eqz v31, :cond_19a

    const/16 v21, 0x20

    goto :goto_19c

    :cond_19a
    const/16 v21, 0x10

    :goto_19c
    or-int v7, v7, v21

    goto :goto_1a3

    :cond_19f
    move/from16 v20, v2

    move-object/from16 v2, p11

    :goto_1a3
    and-int/lit16 v2, v4, 0x1000

    if-eqz v2, :cond_1ae

    or-int/lit16 v7, v7, 0x180

    move/from16 v21, v2

    move-object/from16 v2, p12

    goto :goto_1c6

    :cond_1ae
    move/from16 v21, v2

    and-int/lit16 v2, v3, 0x180

    if-nez v2, :cond_1c4

    move-object/from16 v2, p12

    invoke-interface {v5, v2}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v24

    if-eqz v24, :cond_1bf

    const/16 v25, 0x100

    goto :goto_1c1

    :cond_1bf
    move/from16 v25, v16

    :goto_1c1
    or-int v7, v7, v25

    goto :goto_1c6

    :cond_1c4
    move-object/from16 v2, p12

    :goto_1c6
    and-int/lit16 v2, v4, 0x2000

    if-eqz v2, :cond_1d1

    or-int/lit16 v7, v7, 0xc00

    move/from16 v16, v2

    move-object/from16 v2, p13

    goto :goto_1e9

    :cond_1d1
    move/from16 v16, v2

    and-int/lit16 v2, v3, 0xc00

    if-nez v2, :cond_1e7

    move-object/from16 v2, p13

    invoke-interface {v5, v2}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v24

    if-eqz v24, :cond_1e2

    const/16 v24, 0x800

    goto :goto_1e4

    :cond_1e2
    const/16 v24, 0x400

    :goto_1e4
    or-int v7, v7, v24

    goto :goto_1e9

    :cond_1e7
    move-object/from16 v2, p13

    :goto_1e9
    and-int/lit16 v2, v3, 0x6000

    if-nez v2, :cond_201

    and-int/lit16 v2, v4, 0x4000

    if-nez v2, :cond_1fa

    move-object/from16 v2, p14

    invoke-interface {v5, v2}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v24

    if-eqz v24, :cond_1fc

    goto :goto_1fe

    :cond_1fa
    move-object/from16 v2, p14

    :cond_1fc
    const/16 v19, 0x2000

    :goto_1fe
    or-int v7, v7, v19

    goto :goto_203

    :cond_201
    move-object/from16 v2, p14

    :goto_203
    const v19, 0x8000

    and-int v19, v4, v19

    const/high16 v24, 0x30000

    if-eqz v19, :cond_211

    or-int v7, v7, v24

    move-object/from16 v2, p15

    goto :goto_227

    :cond_211
    and-int v24, v3, v24

    if-nez v24, :cond_225

    move-object/from16 v2, p15

    invoke-interface {v5, v2}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v24

    if-eqz v24, :cond_220

    const/high16 v24, 0x20000

    goto :goto_222

    :cond_220
    const/high16 v24, 0x10000

    :goto_222
    or-int v7, v7, v24

    goto :goto_227

    :cond_225
    move-object/from16 v2, p15

    :goto_227
    const/high16 v24, 0x10000

    and-int v24, v4, v24

    if-eqz v24, :cond_232

    or-int v7, v7, v23

    move-object/from16 v2, p16

    goto :goto_248

    :cond_232
    and-int v23, v3, v23

    if-nez v23, :cond_246

    move-object/from16 v2, p16

    invoke-interface {v5, v2}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v23

    if-eqz v23, :cond_241

    const/high16 v23, 0x100000

    goto :goto_243

    :cond_241
    const/high16 v23, 0x80000

    :goto_243
    or-int v7, v7, v23

    goto :goto_248

    :cond_246
    move-object/from16 v2, p16

    :goto_248
    const/high16 v23, 0x20000

    and-int v23, v4, v23

    if-eqz v23, :cond_251

    or-int v7, v7, v26

    goto :goto_26d

    :cond_251
    and-int v25, v3, v26

    if-nez v25, :cond_26d

    const/high16 v25, 0x1000000

    and-int v25, v3, v25

    if-nez v25, :cond_260

    invoke-interface {v5, v0}, Landroidx/compose/runtime/Composer;->changed(Ljava/lang/Object;)Z

    move-result v25

    goto :goto_264

    :cond_260
    invoke-interface {v5, v0}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v25

    :goto_264
    if-eqz v25, :cond_269

    const/high16 v25, 0x800000

    goto :goto_26b

    :cond_269
    const/high16 v25, 0x400000

    :goto_26b
    or-int v7, v7, v25

    :cond_26d
    :goto_26d
    const/high16 v25, 0x40000

    and-int v25, v4, v25

    if-eqz v25, :cond_278

    or-int v7, v7, v27

    move-object/from16 v0, p18

    goto :goto_28e

    :cond_278
    and-int v26, v3, v27

    if-nez v26, :cond_28c

    move-object/from16 v0, p18

    invoke-interface {v5, v0}, Landroidx/compose/runtime/Composer;->changedInstance(Ljava/lang/Object;)Z

    move-result v26

    if-eqz v26, :cond_287

    const/high16 v26, 0x4000000

    goto :goto_289

    :cond_287
    const/high16 v26, 0x2000000

    :goto_289
    or-int v7, v7, v26

    goto :goto_28e

    :cond_28c
    move-object/from16 v0, p18

    :goto_28e
    const v26, 0x12492493

    and-int v0, v6, v26

    const v2, 0x12492492

    if-ne v0, v2, :cond_2a4

    const v0, 0x2492493

    and-int/2addr v0, v7

    const v2, 0x2492492

    if-eq v0, v2, :cond_2a2

    goto :goto_2a4

    :cond_2a2
    const/4 v0, 0x0

    goto :goto_2a5

    :cond_2a4
    :goto_2a4
    const/4 v0, 0x1

    :goto_2a5
    and-int/lit8 v2, v6, 0x1

    invoke-interface {v5, v0, v2}, Landroidx/compose/runtime/Composer;->shouldExecute(ZI)Z

    move-result v0

    if-eqz v0, :cond_adc

    invoke-interface {v5}, Landroidx/compose/runtime/Composer;->startDefaults()V

    const-string v0, "38@1669L2,41@1815L2,42@1854L2,43@1893L2,50@2237L2"

    invoke-static {v5, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    and-int/lit8 v0, p20, 0x1

    const-string v2, "CC(remember):SettingsShell.kt#9igjgp"

    if-eqz v0, :cond_2fe

    invoke-interface {v5}, Landroidx/compose/runtime/Composer;->getDefaultsInvalid()Z

    move-result v0

    if-eqz v0, :cond_2c2

    goto :goto_2fe

    .line 34
    :cond_2c2
    invoke-interface {v5}, Landroidx/compose/runtime/Composer;->skipToGroupEnd()V

    and-int/lit8 v0, v4, 0x4

    if-eqz v0, :cond_2cb

    and-int/lit16 v6, v6, -0x381

    :cond_2cb
    and-int/lit8 v0, v4, 0x20

    if-eqz v0, :cond_2d3

    const v0, -0x70001

    and-int/2addr v6, v0

    :cond_2d3
    and-int/lit16 v0, v4, 0x4000

    if-eqz v0, :cond_2db

    const v0, -0xe001

    and-int/2addr v7, v0

    :cond_2db
    move-object/from16 v8, p10

    move-object/from16 v28, p11

    move-object/from16 v15, p13

    move-object/from16 v16, p14

    move-object/from16 v17, p15

    move-object/from16 v19, p16

    move v3, v7

    move-object/from16 p3, v9

    move-object/from16 p6, v10

    move-object v0, v13

    move-object/from16 p2, v14

    move-object/from16 p10, p7

    move-object/from16 v7, p9

    move-object/from16 v13, p12

    move-object/from16 v9, p17

    move-object/from16 v10, p18

    move v14, v6

    move-object/from16 v6, p8

    goto/16 :goto_503

    .line 54
    :cond_2fe
    :goto_2fe
    if-eqz v8, :cond_305

    .line 36
    sget-object v0, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v0, Landroidx/compose/ui/Modifier;

    move-object v13, v0

    .end local p1    # "modifier":Landroidx/compose/ui/Modifier;
    .local v13, "modifier":Landroidx/compose/ui/Modifier;
    :cond_305
    and-int/lit8 v0, v4, 0x4

    if-eqz v0, :cond_324

    .line 37
    new-instance v31, Lcom/cooper/lithic/feature/settings/BackupExportUiState;

    const/16 v39, 0x7f

    const/16 v40, 0x0

    const/16 v32, 0x0

    const/16 v33, 0x0

    const/16 v34, 0x0

    const/16 v35, 0x0

    const/16 v36, 0x0

    const/16 v37, 0x0

    const/16 v38, 0x0

    invoke-direct/range {v31 .. v40}, Lcom/cooper/lithic/feature/settings/BackupExportUiState;-><init>(Lcom/cooper/lithic/feature/settings/BackupUiPhase;Ljava/lang/String;Ljava/lang/Long;Lcom/cooper/lithic/feature/settings/BackupExportFeedback;Lcom/cooper/lithic/data/backup/BackupFailureReason;ZZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    .end local p2    # "backupState":Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    .local v31, "backupState":Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    and-int/lit16 v6, v6, -0x381

    move-object/from16 v14, v31

    .end local v31    # "backupState":Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    .local v14, "backupState":Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    :cond_324
    if-eqz v17, :cond_328

    .line 38
    const/4 v0, 0x0

    move-object v9, v0

    .end local p3    # "onExportBackup":Lkotlin/jvm/functions/Function0;
    .local v9, "onExportBackup":Lkotlin/jvm/functions/Function0;
    :cond_328
    if-eqz v18, :cond_35a

    .line 39
    const v0, 0x7b9e1966

    invoke-static {v5, v0, v2}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    const/4 v0, 0x0

    .local v0, "invalid\\1":Z
    move-object v8, v5

    .local v8, "$this$cache\\1":Landroidx/compose/runtime/Composer;
    const/16 v17, 0x0

    .line 159
    .local v17, "$i$f$cache\\1\\39":I
    invoke-interface {v8}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v3

    .local v3, "it\\1":Ljava/lang/Object;
    const/16 v26, 0x0

    .line 160
    .local v26, "$i$a$-let-ComposerKt$cache$1\\2\\159\\1":I
    sget-object v27, Landroidx/compose/runtime/Composer;->Companion:Landroidx/compose/runtime/Composer$Companion;

    move/from16 p1, v0

    .end local v0    # "invalid\\1":Z
    .local p1, "invalid\\1":Z
    invoke-virtual/range {v27 .. v27}, Landroidx/compose/runtime/Composer$Companion;->getEmpty()Ljava/lang/Object;

    move-result-object v0

    if-ne v3, v0, :cond_351

    .line 161
    const/4 v0, 0x0

    .local v0, "$i$a$-cache-SettingsShellKt$SettingsShell$1\\3\\161\\0":I
    new-instance v27, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda0;

    invoke-direct/range {v27 .. v27}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda0;-><init>()V

    .end local v0    # "$i$a$-cache-SettingsShellKt$SettingsShell$1\\3\\161\\0":I
    move-object/from16 v0, v27

    .line 162
    .local v0, "value\\2":Ljava/lang/Object;
    invoke-interface {v8, v0}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 163
    move-object v3, v0

    .end local v0    # "value\\2":Ljava/lang/Object;
    goto :goto_352

    .line 164
    :cond_351
    nop

    .line 159
    .end local v3    # "it\\1":Ljava/lang/Object;
    .end local v26    # "$i$a$-let-ComposerKt$cache$1\\2\\159\\1":I
    :goto_352
    nop

    .line 39
    .end local v8    # "$this$cache\\1":Landroidx/compose/runtime/Composer;
    .end local v17    # "$i$f$cache\\1\\39":I
    .end local p1    # "invalid\\1":Z
    move-object v0, v3

    check-cast v0, Lkotlin/jvm/functions/Function0;

    .end local p4    # "onCancelBackup":Lkotlin/jvm/functions/Function0;
    .local v0, "onCancelBackup":Lkotlin/jvm/functions/Function0;
    invoke-static {v5}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    move-object v10, v0

    .end local v0    # "onCancelBackup":Lkotlin/jvm/functions/Function0;
    .local v10, "onCancelBackup":Lkotlin/jvm/functions/Function0;
    :cond_35a
    and-int/lit8 v0, v4, 0x20

    if-eqz v0, :cond_375

    .line 40
    new-instance v31, Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;

    const/16 v36, 0xf

    const/16 v37, 0x0

    const/16 v32, 0x0

    const/16 v33, 0x0

    const/16 v34, 0x0

    const/16 v35, 0x0

    invoke-direct/range {v31 .. v37}, Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;-><init>(Lcom/cooper/lithic/feature/settings/RestoreUiPhase;Lcom/cooper/lithic/data/backup/RestorePreview;Ljava/lang/String;Lcom/cooper/lithic/feature/settings/RestoreFeedbackKind;ILkotlin/jvm/internal/DefaultConstructorMarker;)V

    .end local p5    # "restoreState":Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;
    .local v31, "restoreState":Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;
    const v0, -0x70001

    and-int/2addr v6, v0

    move-object/from16 v11, v31

    .end local v31    # "restoreState":Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;
    .local v11, "restoreState":Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;
    :cond_375
    if-eqz v22, :cond_379

    .line 41
    const/4 v0, 0x0

    move-object v12, v0

    .end local p6    # "onRestoreBackup":Lkotlin/jvm/functions/Function0;
    .local v12, "onRestoreBackup":Lkotlin/jvm/functions/Function0;
    :cond_379
    if-eqz v15, :cond_3aa

    .line 42
    const v0, 0x7b9e2ba6

    invoke-static {v5, v0, v2}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    const/4 v0, 0x0

    .local v0, "invalid\\4":Z
    move-object v3, v5

    .local v3, "$this$cache\\4":Landroidx/compose/runtime/Composer;
    const/4 v8, 0x0

    .line 165
    .local v8, "$i$f$cache\\4\\42":I
    invoke-interface {v3}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v15

    .local v15, "it\\4":Ljava/lang/Object;
    const/16 v17, 0x0

    .line 166
    .local v17, "$i$a$-let-ComposerKt$cache$1\\5\\165\\4":I
    sget-object v22, Landroidx/compose/runtime/Composer;->Companion:Landroidx/compose/runtime/Composer$Companion;

    move/from16 p1, v0

    .end local v0    # "invalid\\4":Z
    .local p1, "invalid\\4":Z
    invoke-virtual/range {v22 .. v22}, Landroidx/compose/runtime/Composer$Companion;->getEmpty()Ljava/lang/Object;

    move-result-object v0

    if-ne v15, v0, :cond_3a1

    .line 167
    const/4 v0, 0x0

    .local v0, "$i$a$-cache-SettingsShellKt$SettingsShell$2\\6\\167\\0":I
    new-instance v22, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda17;

    invoke-direct/range {v22 .. v22}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda17;-><init>()V

    .end local v0    # "$i$a$-cache-SettingsShellKt$SettingsShell$2\\6\\167\\0":I
    move-object/from16 v0, v22

    .line 168
    .local v0, "value\\5":Ljava/lang/Object;
    invoke-interface {v3, v0}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 169
    move-object v15, v0

    .end local v0    # "value\\5":Ljava/lang/Object;
    goto :goto_3a2

    .line 170
    :cond_3a1
    nop

    .line 165
    .end local v15    # "it\\4":Ljava/lang/Object;
    .end local v17    # "$i$a$-let-ComposerKt$cache$1\\5\\165\\4":I
    :goto_3a2
    nop

    .line 42
    .end local v3    # "$this$cache\\4":Landroidx/compose/runtime/Composer;
    .end local v8    # "$i$f$cache\\4\\42":I
    .end local p1    # "invalid\\4":Z
    move-object v0, v15

    check-cast v0, Lkotlin/jvm/functions/Function0;

    .end local p7    # "onCancelRestore":Lkotlin/jvm/functions/Function0;
    .local v0, "onCancelRestore":Lkotlin/jvm/functions/Function0;
    invoke-static {v5}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    goto :goto_3ac

    .line 41
    .end local v0    # "onCancelRestore":Lkotlin/jvm/functions/Function0;
    .restart local p7    # "onCancelRestore":Lkotlin/jvm/functions/Function0;
    :cond_3aa
    move-object/from16 v0, p7

    .line 42
    .end local p7    # "onCancelRestore":Lkotlin/jvm/functions/Function0;
    .restart local v0    # "onCancelRestore":Lkotlin/jvm/functions/Function0;
    :goto_3ac
    if-eqz v28, :cond_3de

    .line 43
    const v3, 0x7b9e3086

    invoke-static {v5, v3, v2}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    const/4 v3, 0x0

    .local v3, "invalid\\7":Z
    move-object v8, v5

    .local v8, "$this$cache\\7":Landroidx/compose/runtime/Composer;
    const/4 v15, 0x0

    .line 171
    .local v15, "$i$f$cache\\7\\43":I
    move-object/from16 p1, v0

    .end local v0    # "onCancelRestore":Lkotlin/jvm/functions/Function0;
    .local p1, "onCancelRestore":Lkotlin/jvm/functions/Function0;
    invoke-interface {v8}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v0

    .local v0, "it\\7":Ljava/lang/Object;
    const/16 v17, 0x0

    .line 172
    .local v17, "$i$a$-let-ComposerKt$cache$1\\8\\171\\7":I
    sget-object v22, Landroidx/compose/runtime/Composer;->Companion:Landroidx/compose/runtime/Composer$Companion;

    move/from16 p2, v3

    .end local v3    # "invalid\\7":Z
    .local p2, "invalid\\7":Z
    invoke-virtual/range {v22 .. v22}, Landroidx/compose/runtime/Composer$Companion;->getEmpty()Ljava/lang/Object;

    move-result-object v3

    if-ne v0, v3, :cond_3d6

    .line 173
    const/4 v3, 0x0

    .local v3, "$i$a$-cache-SettingsShellKt$SettingsShell$3\\9\\173\\0":I
    new-instance v22, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda18;

    invoke-direct/range {v22 .. v22}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda18;-><init>()V

    .end local v3    # "$i$a$-cache-SettingsShellKt$SettingsShell$3\\9\\173\\0":I
    move-object/from16 v3, v22

    .line 174
    .local v3, "value\\8":Ljava/lang/Object;
    invoke-interface {v8, v3}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 175
    move-object v0, v3

    .end local v3    # "value\\8":Ljava/lang/Object;
    goto :goto_3d7

    .line 176
    :cond_3d6
    nop

    .line 171
    .end local v0    # "it\\7":Ljava/lang/Object;
    .end local v17    # "$i$a$-let-ComposerKt$cache$1\\8\\171\\7":I
    :goto_3d7
    nop

    .line 43
    .end local v8    # "$this$cache\\7":Landroidx/compose/runtime/Composer;
    .end local v15    # "$i$f$cache\\7\\43":I
    .end local p2    # "invalid\\7":Z
    check-cast v0, Lkotlin/jvm/functions/Function0;

    .end local p8    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .local v0, "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    invoke-static {v5}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    goto :goto_3e2

    .line 42
    .end local p1    # "onCancelRestore":Lkotlin/jvm/functions/Function0;
    .local v0, "onCancelRestore":Lkotlin/jvm/functions/Function0;
    .restart local p8    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    :cond_3de
    move-object/from16 p1, v0

    .end local v0    # "onCancelRestore":Lkotlin/jvm/functions/Function0;
    .restart local p1    # "onCancelRestore":Lkotlin/jvm/functions/Function0;
    move-object/from16 v0, p8

    .line 43
    .end local p8    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .local v0, "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    :goto_3e2
    if-eqz v29, :cond_414

    .line 44
    const v3, 0x7b9e3566

    invoke-static {v5, v3, v2}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    const/4 v3, 0x0

    .local v3, "invalid\\10":Z
    move-object v8, v5

    .local v8, "$this$cache\\10":Landroidx/compose/runtime/Composer;
    const/4 v15, 0x0

    .line 177
    .local v15, "$i$f$cache\\10\\44":I
    move-object/from16 p2, v0

    .end local v0    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .local p2, "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    invoke-interface {v8}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v0

    .local v0, "it\\10":Ljava/lang/Object;
    const/16 v17, 0x0

    .line 178
    .local v17, "$i$a$-let-ComposerKt$cache$1\\11\\177\\10":I
    sget-object v22, Landroidx/compose/runtime/Composer;->Companion:Landroidx/compose/runtime/Composer$Companion;

    move/from16 p3, v3

    .end local v3    # "invalid\\10":Z
    .local p3, "invalid\\10":Z
    invoke-virtual/range {v22 .. v22}, Landroidx/compose/runtime/Composer$Companion;->getEmpty()Ljava/lang/Object;

    move-result-object v3

    if-ne v0, v3, :cond_40c

    .line 179
    const/4 v3, 0x0

    .local v3, "$i$a$-cache-SettingsShellKt$SettingsShell$4\\12\\179\\0":I
    new-instance v22, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda19;

    invoke-direct/range {v22 .. v22}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda19;-><init>()V

    .end local v3    # "$i$a$-cache-SettingsShellKt$SettingsShell$4\\12\\179\\0":I
    move-object/from16 v3, v22

    .line 180
    .local v3, "value\\11":Ljava/lang/Object;
    invoke-interface {v8, v3}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 181
    move-object v0, v3

    .end local v3    # "value\\11":Ljava/lang/Object;
    goto :goto_40d

    .line 182
    :cond_40c
    nop

    .line 177
    .end local v0    # "it\\10":Ljava/lang/Object;
    .end local v17    # "$i$a$-let-ComposerKt$cache$1\\11\\177\\10":I
    :goto_40d
    nop

    .line 44
    .end local v8    # "$this$cache\\10":Landroidx/compose/runtime/Composer;
    .end local v15    # "$i$f$cache\\10\\44":I
    .end local p3    # "invalid\\10":Z
    check-cast v0, Lkotlin/jvm/functions/Function0;

    .end local p9    # "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    .local v0, "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    invoke-static {v5}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    goto :goto_418

    .line 43
    .end local p2    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .local v0, "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .restart local p9    # "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    :cond_414
    move-object/from16 p2, v0

    .end local v0    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .restart local p2    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    move-object/from16 v0, p9

    .line 44
    .end local p9    # "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    .local v0, "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    :goto_418
    if-eqz v30, :cond_41c

    .line 45
    const/4 v3, 0x0

    .end local p10    # "onManageCategories":Lkotlin/jvm/functions/Function0;
    .local v3, "onManageCategories":Lkotlin/jvm/functions/Function0;
    goto :goto_41e

    .line 44
    .end local v3    # "onManageCategories":Lkotlin/jvm/functions/Function0;
    .restart local p10    # "onManageCategories":Lkotlin/jvm/functions/Function0;
    :cond_41c
    move-object/from16 v3, p10

    .line 45
    .end local p10    # "onManageCategories":Lkotlin/jvm/functions/Function0;
    .restart local v3    # "onManageCategories":Lkotlin/jvm/functions/Function0;
    :goto_41e
    if-eqz v20, :cond_422

    .line 46
    const/4 v8, 0x0

    .end local p11    # "onNotificationClick":Lkotlin/jvm/functions/Function0;
    .local v8, "onNotificationClick":Lkotlin/jvm/functions/Function0;
    goto :goto_424

    .line 45
    .end local v8    # "onNotificationClick":Lkotlin/jvm/functions/Function0;
    .restart local p11    # "onNotificationClick":Lkotlin/jvm/functions/Function0;
    :cond_422
    move-object/from16 v8, p11

    .line 46
    .end local p11    # "onNotificationClick":Lkotlin/jvm/functions/Function0;
    .restart local v8    # "onNotificationClick":Lkotlin/jvm/functions/Function0;
    :goto_424
    if-eqz v21, :cond_428

    .line 47
    const/4 v15, 0x0

    .end local p12    # "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .local v15, "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    goto :goto_42a

    .line 46
    .end local v15    # "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .restart local p12    # "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    :cond_428
    move-object/from16 v15, p12

    .line 47
    .end local p12    # "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .restart local v15    # "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    :goto_42a
    if-eqz v16, :cond_42f

    .line 48
    const/16 v16, 0x0

    .end local p13    # "onAboutLithic":Lkotlin/jvm/functions/Function0;
    .local v16, "onAboutLithic":Lkotlin/jvm/functions/Function0;
    goto :goto_431

    .line 47
    .end local v16    # "onAboutLithic":Lkotlin/jvm/functions/Function0;
    .restart local p13    # "onAboutLithic":Lkotlin/jvm/functions/Function0;
    :cond_42f
    move-object/from16 v16, p13

    .line 48
    .end local p13    # "onAboutLithic":Lkotlin/jvm/functions/Function0;
    .restart local v16    # "onAboutLithic":Lkotlin/jvm/functions/Function0;
    :goto_431
    move-object/from16 p3, v0

    .end local v0    # "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    .local p3, "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    and-int/lit16 v0, v4, 0x4000

    if-eqz v0, :cond_460

    .line 49
    new-instance v0, Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;

    const/16 v17, 0x1f

    const/16 v20, 0x0

    const/16 v21, 0x0

    const/16 v22, 0x0

    const/16 v26, 0x0

    const/16 v27, 0x0

    const/16 v28, 0x0

    move-object/from16 p4, v0

    move/from16 p10, v17

    move-object/from16 p11, v20

    move-object/from16 p5, v21

    move-object/from16 p6, v22

    move-object/from16 p7, v26

    move-object/from16 p8, v27

    move/from16 p9, v28

    invoke-direct/range {p4 .. p11}, Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;-><init>(Lcom/cooper/lithic/feature/settings/BackupInspectionPhase;Ljava/lang/String;Lcom/cooper/lithic/data/backup/BackupInspectionResult;Ljava/lang/String;ZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    .end local p14    # "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .local v0, "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    const v17, -0xe001

    and-int v7, v7, v17

    goto :goto_462

    .line 48
    .end local v0    # "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .restart local p14    # "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    :cond_460
    move-object/from16 v0, p14

    .line 49
    .end local p14    # "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .restart local v0    # "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    :goto_462
    if-eqz v19, :cond_467

    .line 50
    const/16 v17, 0x0

    .end local p15    # "onInspectBackup":Lkotlin/jvm/functions/Function0;
    .local v17, "onInspectBackup":Lkotlin/jvm/functions/Function0;
    goto :goto_469

    .line 49
    .end local v17    # "onInspectBackup":Lkotlin/jvm/functions/Function0;
    .restart local p15    # "onInspectBackup":Lkotlin/jvm/functions/Function0;
    :cond_467
    move-object/from16 v17, p15

    .line 50
    .end local p15    # "onInspectBackup":Lkotlin/jvm/functions/Function0;
    .restart local v17    # "onInspectBackup":Lkotlin/jvm/functions/Function0;
    :goto_469
    if-eqz v24, :cond_4a9

    .line 51
    move-object/from16 p4, v0

    .end local v0    # "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .local p4, "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    const v0, 0x7b9e6066

    invoke-static {v5, v0, v2}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    const/4 v0, 0x0

    .local v0, "invalid\\13":Z
    move-object/from16 p5, v5

    .local p5, "$this$cache\\13":Landroidx/compose/runtime/Composer;
    const/16 v19, 0x0

    .line 183
    .local v19, "$i$f$cache\\13\\51":I
    move/from16 p6, v0

    .end local v0    # "invalid\\13":Z
    .local p6, "invalid\\13":Z
    invoke-interface/range {p5 .. p5}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v0

    .local v0, "it\\13":Ljava/lang/Object;
    const/16 v20, 0x0

    .line 184
    .local v20, "$i$a$-let-ComposerKt$cache$1\\14\\183\\13":I
    sget-object v21, Landroidx/compose/runtime/Composer;->Companion:Landroidx/compose/runtime/Composer$Companion;

    move-object/from16 p7, v3

    .end local v3    # "onManageCategories":Lkotlin/jvm/functions/Function0;
    .local p7, "onManageCategories":Lkotlin/jvm/functions/Function0;
    invoke-virtual/range {v21 .. v21}, Landroidx/compose/runtime/Composer$Companion;->getEmpty()Ljava/lang/Object;

    move-result-object v3

    if-ne v0, v3, :cond_49b

    .line 185
    const/4 v3, 0x0

    .local v3, "$i$a$-cache-SettingsShellKt$SettingsShell$5\\15\\185\\0":I
    new-instance v21, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda1;

    invoke-direct/range {v21 .. v21}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda1;-><init>()V

    .end local v3    # "$i$a$-cache-SettingsShellKt$SettingsShell$5\\15\\185\\0":I
    move-object/from16 v3, v21

    .line 186
    .local v3, "value\\14":Ljava/lang/Object;
    move-object/from16 p8, v0

    move-object/from16 v0, p5

    .end local p5    # "$this$cache\\13":Landroidx/compose/runtime/Composer;
    .local v0, "$this$cache\\13":Landroidx/compose/runtime/Composer;
    .local p8, "it\\13":Ljava/lang/Object;
    invoke-interface {v0, v3}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 187
    nop

    .end local v3    # "value\\14":Ljava/lang/Object;
    goto :goto_4a1

    .line 188
    .end local p8    # "it\\13":Ljava/lang/Object;
    .local v0, "it\\13":Ljava/lang/Object;
    .restart local p5    # "$this$cache\\13":Landroidx/compose/runtime/Composer;
    :cond_49b
    move-object/from16 p8, v0

    move-object/from16 v0, p5

    .end local p5    # "$this$cache\\13":Landroidx/compose/runtime/Composer;
    .local v0, "$this$cache\\13":Landroidx/compose/runtime/Composer;
    .restart local p8    # "it\\13":Ljava/lang/Object;
    move-object/from16 v3, p8

    .line 183
    .end local v20    # "$i$a$-let-ComposerKt$cache$1\\14\\183\\13":I
    .end local p8    # "it\\13":Ljava/lang/Object;
    :goto_4a1
    nop

    .line 51
    .end local v0    # "$this$cache\\13":Landroidx/compose/runtime/Composer;
    .end local v19    # "$i$f$cache\\13\\51":I
    .end local p6    # "invalid\\13":Z
    move-object v0, v3

    check-cast v0, Lkotlin/jvm/functions/Function0;

    .end local p16    # "onCancelInspection":Lkotlin/jvm/functions/Function0;
    .local v0, "onCancelInspection":Lkotlin/jvm/functions/Function0;
    invoke-static {v5}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    goto :goto_4af

    .line 50
    .end local p4    # "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .end local p7    # "onManageCategories":Lkotlin/jvm/functions/Function0;
    .local v0, "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .local v3, "onManageCategories":Lkotlin/jvm/functions/Function0;
    .restart local p16    # "onCancelInspection":Lkotlin/jvm/functions/Function0;
    :cond_4a9
    move-object/from16 p4, v0

    move-object/from16 p7, v3

    .end local v0    # "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .end local v3    # "onManageCategories":Lkotlin/jvm/functions/Function0;
    .restart local p4    # "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .restart local p7    # "onManageCategories":Lkotlin/jvm/functions/Function0;
    move-object/from16 v0, p16

    .line 51
    .end local p16    # "onCancelInspection":Lkotlin/jvm/functions/Function0;
    .local v0, "onCancelInspection":Lkotlin/jvm/functions/Function0;
    :goto_4af
    if-eqz v23, :cond_4b6

    .line 52
    sget-object v3, Lcom/cooper/lithic/feature/settings/PhotoStorageUiState$Idle;->INSTANCE:Lcom/cooper/lithic/feature/settings/PhotoStorageUiState$Idle;

    check-cast v3, Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;

    .end local p17    # "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .local v3, "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    goto :goto_4b8

    .line 51
    .end local v3    # "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .restart local p17    # "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    :cond_4b6
    move-object/from16 v3, p17

    .line 52
    .end local p17    # "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .restart local v3    # "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    :goto_4b8
    if-eqz v25, :cond_4e0

    .line 53
    const/16 v19, 0x0

    move/from16 p6, v6

    move-object/from16 v6, p2

    move-object/from16 p2, v14

    move/from16 v14, p6

    move/from16 p6, v7

    move-object/from16 v7, p3

    move-object/from16 p3, v9

    move-object v9, v3

    move/from16 v3, p6

    move-object/from16 p10, p1

    move-object/from16 v28, v8

    move-object/from16 p6, v10

    move-object/from16 v10, v19

    move-object/from16 v8, p7

    move-object/from16 v19, v0

    move-object v0, v13

    move-object v13, v15

    move-object/from16 v15, v16

    move-object/from16 v16, p4

    .end local p18    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .local v19, "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    goto :goto_503

    .line 52
    .end local v19    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .restart local p18    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    :cond_4e0
    move/from16 p6, v6

    move-object/from16 v6, p2

    move-object/from16 p2, v14

    move/from16 v14, p6

    move/from16 p6, v7

    move-object/from16 v7, p3

    move-object/from16 p3, v9

    move-object v9, v3

    move/from16 v3, p6

    move-object/from16 p10, p1

    move-object/from16 v19, v0

    move-object/from16 v28, v8

    move-object/from16 p6, v10

    move-object v0, v13

    move-object v13, v15

    move-object/from16 v15, v16

    move-object/from16 v16, p4

    move-object/from16 v8, p7

    move-object/from16 v10, p18

    .line 34
    .end local p1    # "onCancelRestore":Lkotlin/jvm/functions/Function0;
    .end local p4    # "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .end local p7    # "onManageCategories":Lkotlin/jvm/functions/Function0;
    .end local p18    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .local v0, "modifier":Landroidx/compose/ui/Modifier;
    .local v3, "$dirty1":I
    .local v6, "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .local v7, "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    .local v8, "onManageCategories":Lkotlin/jvm/functions/Function0;
    .local v9, "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .local v10, "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .local v13, "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .local v14, "$dirty":I
    .local v15, "onAboutLithic":Lkotlin/jvm/functions/Function0;
    .local v16, "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .local v19, "onCancelInspection":Lkotlin/jvm/functions/Function0;
    .local v28, "onNotificationClick":Lkotlin/jvm/functions/Function0;
    .local p2, "backupState":Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    .local p3, "onExportBackup":Lkotlin/jvm/functions/Function0;
    .local p6, "onCancelBackup":Lkotlin/jvm/functions/Function0;
    .local p10, "onCancelRestore":Lkotlin/jvm/functions/Function0;
    :goto_503
    invoke-interface {v5}, Landroidx/compose/runtime/Composer;->endDefaults()V

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v20

    if-eqz v20, :cond_517

    const v4, -0x7eac307c

    move-object/from16 p11, v6

    .end local v6    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .local p11, "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    const-string v6, "com.cooper.lithic.feature.settings.SettingsShell (SettingsShell.kt:53)"

    invoke-static {v4, v14, v3, v6}, Landroidx/compose/runtime/ComposerKt;->traceEventStart(IIILjava/lang/String;)V

    goto :goto_519

    .end local p11    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .restart local v6    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    :cond_517
    move-object/from16 p11, v6

    .end local v6    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .restart local p11    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    :goto_519
    const/4 v4, 0x0

    new-array v6, v4, [Ljava/lang/Object;

    .line 55
    const v4, 0x7b9e75fd

    invoke-static {v5, v4, v2}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    const/4 v4, 0x0

    .local v4, "invalid\\16":Z
    move-object/from16 p1, v5

    .local p1, "$this$cache\\16":Landroidx/compose/runtime/Composer;
    const/16 v20, 0x0

    .line 189
    .local v20, "$i$f$cache\\16\\55":I
    move/from16 v29, v3

    .end local v3    # "$dirty1":I
    .local v29, "$dirty1":I
    invoke-interface/range {p1 .. p1}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v3

    .local v3, "it\\16":Ljava/lang/Object;
    const/16 v21, 0x0

    .line 190
    .local v21, "$i$a$-let-ComposerKt$cache$1\\17\\189\\16":I
    sget-object v22, Landroidx/compose/runtime/Composer;->Companion:Landroidx/compose/runtime/Composer$Companion;

    move/from16 p4, v4

    .end local v4    # "invalid\\16":Z
    .local p4, "invalid\\16":Z
    invoke-virtual/range {v22 .. v22}, Landroidx/compose/runtime/Composer$Companion;->getEmpty()Ljava/lang/Object;

    move-result-object v4

    if-ne v3, v4, :cond_54a

    .line 191
    const/4 v4, 0x0

    .local v4, "$i$a$-cache-SettingsShellKt$SettingsShell$informationOpen$2\\18\\191\\0":I
    new-instance v22, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda2;

    invoke-direct/range {v22 .. v22}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda2;-><init>()V

    .end local v4    # "$i$a$-cache-SettingsShellKt$SettingsShell$informationOpen$2\\18\\191\\0":I
    move-object/from16 v4, v22

    .line 192
    .local v4, "value\\17":Ljava/lang/Object;
    move-object/from16 p5, v3

    move-object/from16 v3, p1

    .end local p1    # "$this$cache\\16":Landroidx/compose/runtime/Composer;
    .local v3, "$this$cache\\16":Landroidx/compose/runtime/Composer;
    .local p5, "it\\16":Ljava/lang/Object;
    invoke-interface {v3, v4}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 193
    nop

    .end local v4    # "value\\17":Ljava/lang/Object;
    goto :goto_550

    .line 194
    .end local p5    # "it\\16":Ljava/lang/Object;
    .local v3, "it\\16":Ljava/lang/Object;
    .restart local p1    # "$this$cache\\16":Landroidx/compose/runtime/Composer;
    :cond_54a
    move-object/from16 p5, v3

    move-object/from16 v3, p1

    .end local p1    # "$this$cache\\16":Landroidx/compose/runtime/Composer;
    .local v3, "$this$cache\\16":Landroidx/compose/runtime/Composer;
    .restart local p5    # "it\\16":Ljava/lang/Object;
    move-object/from16 v4, p5

    .line 189
    .end local v21    # "$i$a$-let-ComposerKt$cache$1\\17\\189\\16":I
    .end local p5    # "it\\16":Ljava/lang/Object;
    :goto_550
    nop

    .line 55
    .end local v3    # "$this$cache\\16":Landroidx/compose/runtime/Composer;
    .end local v20    # "$i$f$cache\\16\\55":I
    .end local p4    # "invalid\\16":Z
    check-cast v4, Lkotlin/jvm/functions/Function0;

    invoke-static {v5}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    const/16 v3, 0x30

    invoke-static {v6, v4, v5, v3}, Landroidx/compose/runtime/saveable/RememberSaveableKt;->rememberSaveable([Ljava/lang/Object;Lkotlin/jvm/functions/Function0;Landroidx/compose/runtime/Composer;I)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Landroidx/compose/runtime/MutableState;

    .line 56
    .local v4, "informationOpen$delegate":Landroidx/compose/runtime/MutableState;
    const/4 v6, 0x0

    move/from16 p1, v3

    const/4 v3, 0x0

    move-object/from16 p12, v7

    const/4 v7, 0x1

    .end local v7    # "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    .local p12, "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    invoke-static {v0, v6, v7, v3}, Landroidx/compose/foundation/layout/SizeKt;->fillMaxSize$default(Landroidx/compose/ui/Modifier;FILjava/lang/Object;)Landroidx/compose/ui/Modifier;

    move-result-object v20

    const/4 v3, 0x0

    invoke-static {v3, v5, v3, v7}, Landroidx/compose/foundation/ScrollKt;->rememberScrollState(ILandroidx/compose/runtime/Composer;II)Landroidx/compose/foundation/ScrollState;

    move-result-object v21

    const/16 v25, 0xe

    const/16 v26, 0x0

    const/16 v22, 0x0

    const/16 v23, 0x0

    const/16 v24, 0x0

    invoke-static/range {v20 .. v26}, Landroidx/compose/foundation/ScrollKt;->verticalScroll$default(Landroidx/compose/ui/Modifier;Landroidx/compose/foundation/ScrollState;ZLandroidx/compose/foundation/gestures/FlingBehavior;ZILjava/lang/Object;)Landroidx/compose/ui/Modifier;

    move-result-object v3

    sget-object v6, Lcom/cooper/lithic/core/design/LithicSpacing;->INSTANCE:Lcom/cooper/lithic/core/design/LithicSpacing;

    const/4 v7, 0x6

    invoke-virtual {v6, v5, v7}, Lcom/cooper/lithic/core/design/LithicSpacing;->getPage(Landroidx/compose/runtime/Composer;I)F

    move-result v6

    move/from16 p4, v7

    const/16 v7, 0x10

    .local v7, "$this$dp\\19":I
    const/16 v20, 0x0

    .line 195
    .local v20, "$i$f$getDp\\19\\56":I
    move-object/from16 v30, v0

    .end local v0    # "modifier":Landroidx/compose/ui/Modifier;
    .local v30, "modifier":Landroidx/compose/ui/Modifier;
    int-to-float v0, v7

    invoke-static {v0}, Landroidx/compose/ui/unit/Dp;->constructor-impl(F)F

    move-result v0

    .line 56
    .end local v7    # "$this$dp\\19":I
    .end local v20    # "$i$f$getDp\\19\\56":I
    invoke-static {v3, v6, v0}, Landroidx/compose/foundation/layout/PaddingKt;->padding-VpY3zN4(Landroidx/compose/ui/Modifier;FF)Landroidx/compose/ui/Modifier;

    move-result-object v0

    .line 57
    const-string v3, "settings-screen"

    invoke-static {v0, v3}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v0

    sget-object v3, Landroidx/compose/foundation/layout/Arrangement;->INSTANCE:Landroidx/compose/foundation/layout/Arrangement;

    const/16 v6, 0x18

    .local v6, "$this$dp\\20":I
    const/4 v7, 0x0

    .line 196
    .local v7, "$i$f$getDp\\20\\57":I
    move-object/from16 p5, v0

    int-to-float v0, v6

    invoke-static {v0}, Landroidx/compose/ui/unit/Dp;->constructor-impl(F)F

    move-result v0

    .line 57
    .end local v6    # "$this$dp\\20":I
    .end local v7    # "$i$f$getDp\\20\\57":I
    invoke-virtual {v3, v0}, Landroidx/compose/foundation/layout/Arrangement;->spacedBy-0680j_4(F)Landroidx/compose/foundation/layout/Arrangement$HorizontalOrVertical;

    move-result-object v0

    check-cast v0, Landroidx/compose/foundation/layout/Arrangement$Vertical;

    .line 56
    move-object v3, v5

    .local v3, "$composer\\21":Landroidx/compose/runtime/Composer;
    move/from16 v6, p1

    .local v6, "$changed\\21":I
    move-object/from16 v7, p5

    .local v0, "verticalArrangement\\21":Landroidx/compose/foundation/layout/Arrangement$Vertical;
    .local v7, "modifier\\21":Landroidx/compose/ui/Modifier;
    const/16 v20, 0x0

    .line 197
    .local v20, "$i$f$Column\\21\\56":I
    move/from16 p13, v6

    .end local v6    # "$changed\\21":I
    .local p13, "$changed\\21":I
    const v6, 0x4ff7456f

    move-object/from16 p14, v7

    .end local v7    # "modifier\\21":Landroidx/compose/ui/Modifier;
    .local p14, "modifier\\21":Landroidx/compose/ui/Modifier;
    const-string v7, "CC(Column)N(modifier,verticalArrangement,horizontalAlignment,content)87@4443L61,88@4509L134:Column.kt#2w3rfo"

    invoke-static {v3, v6, v7}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    .line 198
    sget-object v6, Landroidx/compose/ui/Alignment;->Companion:Landroidx/compose/ui/Alignment$Companion;

    invoke-virtual {v6}, Landroidx/compose/ui/Alignment$Companion;->getStart()Landroidx/compose/ui/Alignment$Horizontal;

    move-result-object v6

    .line 201
    .local v6, "horizontalAlignment\\21":Landroidx/compose/ui/Alignment$Horizontal;
    shr-int/lit8 v7, p13, 0x3

    and-int/lit8 v7, v7, 0xe

    shr-int/lit8 v21, p13, 0x3

    and-int/lit8 v21, v21, 0x70

    or-int v7, v7, v21

    invoke-static {v0, v6, v3, v7}, Landroidx/compose/foundation/layout/ColumnKt;->columnMeasurePolicy(Landroidx/compose/foundation/layout/Arrangement$Vertical;Landroidx/compose/ui/Alignment$Horizontal;Landroidx/compose/runtime/Composer;I)Landroidx/compose/ui/layout/MeasurePolicy;

    move-result-object v7

    .line 205
    .local v7, "measurePolicy\\21":Landroidx/compose/ui/layout/MeasurePolicy;
    shl-int/lit8 v21, p13, 0x3

    and-int/lit8 v21, v21, 0x70

    .line 202
    move-object/from16 p15, p14

    .local v21, "$changed\\22":I
    .local p15, "modifier\\22":Landroidx/compose/ui/Modifier;
    const/16 v22, 0x0

    .line 206
    .local v22, "$i$f$Layout\\22\\202":I
    move-object/from16 p16, v0

    .end local v0    # "verticalArrangement\\21":Landroidx/compose/foundation/layout/Arrangement$Vertical;
    .local p16, "verticalArrangement\\21":Landroidx/compose/foundation/layout/Arrangement$Vertical;
    const v0, -0x451e1427

    move-object/from16 p17, v6

    .end local v6    # "horizontalAlignment\\21":Landroidx/compose/ui/Alignment$Horizontal;
    .local p17, "horizontalAlignment\\21":Landroidx/compose/ui/Alignment$Horizontal;
    const-string v6, "CC(Layout)P(!1,2)80@3267L27,83@3433L360:Layout.kt#80mrfh"

    invoke-static {v3, v0, v6}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    .line 207
    const/4 v0, 0x0

    invoke-static {v3, v0}, Landroidx/compose/runtime/ComposablesKt;->getCurrentCompositeKeyHashCode(Landroidx/compose/runtime/Composer;I)J

    move-result-wide v23

    invoke-static/range {v23 .. v24}, Ljava/lang/Long;->hashCode(J)I

    move-result v0

    .line 208
    .local v0, "compositeKeyHash\\22":I
    invoke-interface {v3}, Landroidx/compose/runtime/Composer;->getCurrentCompositionLocalMap()Landroidx/compose/runtime/CompositionLocalMap;

    move-result-object v6

    .line 209
    .local v6, "localMap\\22":Landroidx/compose/runtime/CompositionLocalMap;
    move/from16 p18, v0

    move-object/from16 p5, v11

    move-object/from16 v0, p15

    .end local v11    # "restoreState":Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;
    .end local p15    # "modifier\\22":Landroidx/compose/ui/Modifier;
    .local v0, "modifier\\22":Landroidx/compose/ui/Modifier;
    .local p5, "restoreState":Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;
    .local p18, "compositeKeyHash\\22":I
    invoke-static {v3, v0}, Landroidx/compose/ui/ComposedModifierKt;->materializeModifier(Landroidx/compose/runtime/Composer;Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;

    move-result-object v11

    .line 211
    .local v11, "materialized\\22":Landroidx/compose/ui/Modifier;
    sget-object v23, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    invoke-virtual/range {v23 .. v23}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getConstructor()Lkotlin/jvm/functions/Function0;

    move-result-object v23

    move-object/from16 v24, v0

    .end local v0    # "modifier\\22":Landroidx/compose/ui/Modifier;
    .local v24, "modifier\\22":Landroidx/compose/ui/Modifier;
    shl-int/lit8 v0, v21, 0x6

    and-int/lit16 v0, v0, 0x380

    or-int/lit8 v0, v0, 0x6

    .line 210
    move-object/from16 p15, v23

    .local v0, "$changed\\23":I
    .local p15, "factory\\23":Lkotlin/jvm/functions/Function0;
    const/16 v23, 0x0

    .line 212
    .local v23, "$i$f$ReusableComposeNode\\23\\210":I
    move/from16 v25, v0

    .end local v0    # "$changed\\23":I
    .local v25, "$changed\\23":I
    const v0, -0x20f7d59c

    move-object/from16 p9, v12

    .end local v12    # "onRestoreBackup":Lkotlin/jvm/functions/Function0;
    .local p9, "onRestoreBackup":Lkotlin/jvm/functions/Function0;
    const-string v12, "CC(ReusableComposeNode)N(factory,update,content)399@15590L9:Composables.kt#9igjgp"

    invoke-static {v3, v0, v12}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    .line 213
    invoke-interface {v3}, Landroidx/compose/runtime/Composer;->getApplier()Landroidx/compose/runtime/Applier;

    move-result-object v0

    instance-of v0, v0, Landroidx/compose/runtime/Applier;

    if-nez v0, :cond_627

    invoke-static {}, Landroidx/compose/runtime/ComposablesKt;->invalidApplier()V

    .line 214
    :cond_627
    invoke-interface {v3}, Landroidx/compose/runtime/Composer;->startReusableNode()V

    .line 215
    invoke-interface {v3}, Landroidx/compose/runtime/Composer;->getInserting()Z

    move-result v0

    if-eqz v0, :cond_636

    .line 216
    move-object/from16 v0, p15

    .end local p15    # "factory\\23":Lkotlin/jvm/functions/Function0;
    .local v0, "factory\\23":Lkotlin/jvm/functions/Function0;
    invoke-interface {v3, v0}, Landroidx/compose/runtime/Composer;->createNode(Lkotlin/jvm/functions/Function0;)V

    goto :goto_63b

    .line 218
    .end local v0    # "factory\\23":Lkotlin/jvm/functions/Function0;
    .restart local p15    # "factory\\23":Lkotlin/jvm/functions/Function0;
    :cond_636
    move-object/from16 v0, p15

    .end local p15    # "factory\\23":Lkotlin/jvm/functions/Function0;
    .restart local v0    # "factory\\23":Lkotlin/jvm/functions/Function0;
    invoke-interface {v3}, Landroidx/compose/runtime/Composer;->useNode()V

    .line 220
    :goto_63b
    invoke-static {v3}, Landroidx/compose/runtime/Updater;->constructor-impl(Landroidx/compose/runtime/Composer;)Landroidx/compose/runtime/Composer;

    move-result-object v12

    .local v12, "$this$Layout_u24lambda_u240\\24":Landroidx/compose/runtime/Composer;
    const/16 v26, 0x0

    .line 221
    .local v26, "$i$a$-ReusableComposeNode-LayoutKt$Layout$1\\24\\220\\22":I
    sget-object v27, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    move-object/from16 v31, v0

    .end local v0    # "factory\\23":Lkotlin/jvm/functions/Function0;
    .local v31, "factory\\23":Lkotlin/jvm/functions/Function0;
    invoke-virtual/range {v27 .. v27}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getSetMeasurePolicy()Lkotlin/jvm/functions/Function2;

    move-result-object v0

    invoke-static {v12, v7, v0}, Landroidx/compose/runtime/Updater;->set-impl(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V

    .line 222
    sget-object v0, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    invoke-virtual {v0}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getSetResolvedCompositionLocals()Lkotlin/jvm/functions/Function2;

    move-result-object v0

    invoke-static {v12, v6, v0}, Landroidx/compose/runtime/Updater;->set-impl(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V

    .line 223
    sget-object v0, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    invoke-virtual {v0}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getSetCompositeKeyHash()Lkotlin/jvm/functions/Function2;

    move-result-object v0

    .local v0, "block\\25":Lkotlin/jvm/functions/Function2;
    const/16 v27, 0x0

    .line 224
    .local v27, "$i$f$set-impl\\25\\223":I
    move-object/from16 p1, v12

    .local p1, "$this$set_impl_u24lambda_u240\\25":Landroidx/compose/runtime/Composer;
    const/16 v32, 0x0

    .line 225
    .local v32, "$i$a$-with-Updater$set$1\\26\\224\\25":I
    invoke-interface/range {p1 .. p1}, Landroidx/compose/runtime/Composer;->getInserting()Z

    move-result v33

    if-nez v33, :cond_67d

    move-object/from16 p15, v3

    .end local v3    # "$composer\\21":Landroidx/compose/runtime/Composer;
    .local p15, "$composer\\21":Landroidx/compose/runtime/Composer;
    invoke-interface/range {p1 .. p1}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v3

    move-object/from16 v33, v6

    .end local v6    # "localMap\\22":Landroidx/compose/runtime/CompositionLocalMap;
    .local v33, "localMap\\22":Landroidx/compose/runtime/CompositionLocalMap;
    invoke-static/range {p18 .. p18}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v6

    invoke-static {v3, v6}, Lkotlin/jvm/internal/Intrinsics;->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_67a

    goto :goto_681

    :cond_67a
    move-object/from16 v6, p1

    goto :goto_691

    .end local v33    # "localMap\\22":Landroidx/compose/runtime/CompositionLocalMap;
    .end local p15    # "$composer\\21":Landroidx/compose/runtime/Composer;
    .restart local v3    # "$composer\\21":Landroidx/compose/runtime/Composer;
    .restart local v6    # "localMap\\22":Landroidx/compose/runtime/CompositionLocalMap;
    :cond_67d
    move-object/from16 p15, v3

    move-object/from16 v33, v6

    .line 226
    .end local v3    # "$composer\\21":Landroidx/compose/runtime/Composer;
    .end local v6    # "localMap\\22":Landroidx/compose/runtime/CompositionLocalMap;
    .restart local v33    # "localMap\\22":Landroidx/compose/runtime/CompositionLocalMap;
    .restart local p15    # "$composer\\21":Landroidx/compose/runtime/Composer;
    :goto_681
    invoke-static/range {p18 .. p18}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v3

    move-object/from16 v6, p1

    .end local p1    # "$this$set_impl_u24lambda_u240\\25":Landroidx/compose/runtime/Composer;
    .local v6, "$this$set_impl_u24lambda_u240\\25":Landroidx/compose/runtime/Composer;
    invoke-interface {v6, v3}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 227
    invoke-static/range {p18 .. p18}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v3

    invoke-interface {v12, v3, v0}, Landroidx/compose/runtime/Composer;->apply(Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V

    .line 229
    :goto_691
    nop

    .line 224
    .end local v6    # "$this$set_impl_u24lambda_u240\\25":Landroidx/compose/runtime/Composer;
    .end local v32    # "$i$a$-with-Updater$set$1\\26\\224\\25":I
    nop

    .line 229
    nop

    .line 230
    .end local v0    # "block\\25":Lkotlin/jvm/functions/Function2;
    .end local v27    # "$i$f$set-impl\\25\\223":I
    sget-object v0, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    invoke-virtual {v0}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getSetModifier()Lkotlin/jvm/functions/Function2;

    move-result-object v0

    invoke-static {v12, v11, v0}, Landroidx/compose/runtime/Updater;->set-impl(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V

    .line 231
    nop

    .line 220
    .end local v12    # "$this$Layout_u24lambda_u240\\24":Landroidx/compose/runtime/Composer;
    .end local v26    # "$i$a$-ReusableComposeNode-LayoutKt$Layout$1\\24\\220\\22":I
    nop

    .line 232
    shr-int/lit8 v0, v25, 0x6

    and-int/lit8 v0, v0, 0xe

    .local v0, "$changed\\27":I
    move-object/from16 v3, p15

    .local v3, "$composer\\27":Landroidx/compose/runtime/Composer;
    const/4 v6, 0x0

    .line 203
    .local v6, "$i$a$-Layout-ColumnKt$Column$1\\27\\232\\21":I
    const v12, 0x7cc0ae6e

    move/from16 v26, v0

    .end local v0    # "$changed\\27":I
    .local v26, "$changed\\27":I
    const-string v0, "C89@4557L9:Column.kt#2w3rfo"

    invoke-static {v3, v12, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    sget-object v0, Landroidx/compose/foundation/layout/ColumnScopeInstance;->INSTANCE:Landroidx/compose/foundation/layout/ColumnScopeInstance;

    shr-int/lit8 v12, p13, 0x6

    and-int/lit8 v12, v12, 0x70

    or-int/lit8 v12, v12, 0x6

    .local v12, "$changed\\28":I
    check-cast v0, Landroidx/compose/foundation/layout/ColumnScope;

    .local v0, "$this$SettingsShell_u24lambda_u248\\28":Landroidx/compose/foundation/layout/ColumnScope;
    move-object/from16 v37, v3

    .local v37, "$composer\\28":Landroidx/compose/runtime/Composer;
    const/16 v27, 0x0

    .line 58
    .local v27, "$i$a$-Column-SettingsShellKt$SettingsShell$6\\28\\203\\0":I
    move-object/from16 v32, v0

    .end local v0    # "$this$SettingsShell_u24lambda_u248\\28":Landroidx/compose/foundation/layout/ColumnScope;
    .local v32, "$this$SettingsShell_u24lambda_u248\\28":Landroidx/compose/foundation/layout/ColumnScope;
    const v0, 0x41d0b456

    move-object/from16 v40, v3

    .end local v3    # "$composer\\27":Landroidx/compose/runtime/Composer;
    .local v40, "$composer\\27":Landroidx/compose/runtime/Composer;
    const-string v3, "C57@2664L407,64@3080L43,65@3150L328,65@3132L346,77@3718L3250,77@3699L3269,129@6977L61,130@7066L397,130@7047L416:SettingsShell.kt#e7edxl"

    move/from16 v41, v6

    move-object/from16 v6, v37

    .end local v37    # "$composer\\28":Landroidx/compose/runtime/Composer;
    .local v6, "$composer\\28":Landroidx/compose/runtime/Composer;
    .local v41, "$i$a$-Layout-ColumnKt$Column$1\\27\\232\\21":I
    invoke-static {v6, v0, v3}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    sget-object v0, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v0, Landroidx/compose/ui/Modifier;

    const/4 v3, 0x0

    .end local v6    # "$composer\\28":Landroidx/compose/runtime/Composer;
    .restart local v37    # "$composer\\28":Landroidx/compose/runtime/Composer;
    const/4 v6, 0x0

    move-object/from16 v42, v7

    const/4 v7, 0x1

    .end local v7    # "measurePolicy\\21":Landroidx/compose/ui/layout/MeasurePolicy;
    .local v42, "measurePolicy\\21":Landroidx/compose/ui/layout/MeasurePolicy;
    invoke-static {v0, v3, v7, v6}, Landroidx/compose/foundation/layout/SizeKt;->fillMaxWidth$default(Landroidx/compose/ui/Modifier;FILjava/lang/Object;)Landroidx/compose/ui/Modifier;

    move-result-object v0

    .local v0, "modifier\\29":Landroidx/compose/ui/Modifier;
    sget-object v3, Landroidx/compose/ui/Alignment;->Companion:Landroidx/compose/ui/Alignment$Companion;

    invoke-virtual {v3}, Landroidx/compose/ui/Alignment$Companion;->getCenterVertically()Landroidx/compose/ui/Alignment$Vertical;

    move-result-object v3

    .local v3, "verticalAlignment\\29":Landroidx/compose/ui/Alignment$Vertical;
    const/16 v6, 0x186

    .local v6, "$changed\\29":I
    move-object/from16 v7, v37

    .local v7, "$composer\\29":Landroidx/compose/runtime/Composer;
    const/16 v34, 0x0

    .line 233
    .local v34, "$i$f$Row\\29\\58":I
    move-object/from16 p1, v0

    .end local v0    # "modifier\\29":Landroidx/compose/ui/Modifier;
    .local p1, "modifier\\29":Landroidx/compose/ui/Modifier;
    const v0, 0x3255a44b

    move/from16 p7, v6

    .end local v6    # "$changed\\29":I
    .local p7, "$changed\\29":I
    const-string v6, "CC(Row)N(modifier,horizontalArrangement,verticalAlignment,content)99@5125L58,100@5188L131:Row.kt#2w3rfo"

    invoke-static {v7, v0, v6}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    .line 234
    sget-object v0, Landroidx/compose/foundation/layout/Arrangement;->INSTANCE:Landroidx/compose/foundation/layout/Arrangement;

    invoke-virtual {v0}, Landroidx/compose/foundation/layout/Arrangement;->getStart()Landroidx/compose/foundation/layout/Arrangement$Horizontal;

    move-result-object v0

    .line 238
    .local v0, "horizontalArrangement\\29":Landroidx/compose/foundation/layout/Arrangement$Horizontal;
    shr-int/lit8 v6, p7, 0x3

    and-int/lit8 v6, v6, 0xe

    shr-int/lit8 v35, p7, 0x3

    and-int/lit8 v35, v35, 0x70

    or-int v6, v6, v35

    invoke-static {v0, v3, v7, v6}, Landroidx/compose/foundation/layout/RowKt;->rowMeasurePolicy(Landroidx/compose/foundation/layout/Arrangement$Horizontal;Landroidx/compose/ui/Alignment$Vertical;Landroidx/compose/runtime/Composer;I)Landroidx/compose/ui/layout/MeasurePolicy;

    move-result-object v6

    .line 242
    .local v6, "measurePolicy\\29":Landroidx/compose/ui/layout/MeasurePolicy;
    shl-int/lit8 v35, p7, 0x3

    and-int/lit8 v35, v35, 0x70

    .line 239
    nop

    .local v35, "$changed\\30":I
    move-object/from16 p8, p1

    .local p8, "modifier\\30":Landroidx/compose/ui/Modifier;
    const/16 v36, 0x0

    .line 243
    .local v36, "$i$f$Layout\\30\\239":I
    move-object/from16 v38, v0

    .end local v0    # "horizontalArrangement\\29":Landroidx/compose/foundation/layout/Arrangement$Horizontal;
    .local v38, "horizontalArrangement\\29":Landroidx/compose/foundation/layout/Arrangement$Horizontal;
    const v0, -0x451e1427

    move-object/from16 v39, v3

    .end local v3    # "verticalAlignment\\29":Landroidx/compose/ui/Alignment$Vertical;
    .local v39, "verticalAlignment\\29":Landroidx/compose/ui/Alignment$Vertical;
    const-string v3, "CC(Layout)P(!1,2)80@3267L27,83@3433L360:Layout.kt#80mrfh"

    invoke-static {v7, v0, v3}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    .line 244
    const/4 v0, 0x0

    invoke-static {v7, v0}, Landroidx/compose/runtime/ComposablesKt;->getCurrentCompositeKeyHashCode(Landroidx/compose/runtime/Composer;I)J

    move-result-wide v43

    invoke-static/range {v43 .. v44}, Ljava/lang/Long;->hashCode(J)I

    move-result v0

    .line 245
    .local v0, "compositeKeyHash\\30":I
    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->getCurrentCompositionLocalMap()Landroidx/compose/runtime/CompositionLocalMap;

    move-result-object v3

    .line 246
    .local v3, "localMap\\30":Landroidx/compose/runtime/CompositionLocalMap;
    move/from16 v43, v0

    move-object/from16 v44, v11

    move-object/from16 v0, p8

    .end local v11    # "materialized\\22":Landroidx/compose/ui/Modifier;
    .end local p8    # "modifier\\30":Landroidx/compose/ui/Modifier;
    .local v0, "modifier\\30":Landroidx/compose/ui/Modifier;
    .local v43, "compositeKeyHash\\30":I
    .local v44, "materialized\\22":Landroidx/compose/ui/Modifier;
    invoke-static {v7, v0}, Landroidx/compose/ui/ComposedModifierKt;->materializeModifier(Landroidx/compose/runtime/Composer;Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;

    move-result-object v11

    .line 248
    .local v11, "materialized\\30":Landroidx/compose/ui/Modifier;
    sget-object v45, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    invoke-virtual/range {v45 .. v45}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getConstructor()Lkotlin/jvm/functions/Function0;

    move-result-object v45

    .end local v0    # "modifier\\30":Landroidx/compose/ui/Modifier;
    .restart local p8    # "modifier\\30":Landroidx/compose/ui/Modifier;
    shl-int/lit8 v0, v35, 0x6

    and-int/lit16 v0, v0, 0x380

    or-int/lit8 v0, v0, 0x6

    .line 247
    nop

    .local v0, "$changed\\31":I
    move-object/from16 v46, v45

    .local v46, "factory\\31":Lkotlin/jvm/functions/Function0;
    const/16 v45, 0x0

    .line 249
    .local v45, "$i$f$ReusableComposeNode\\31\\247":I
    move/from16 v47, v0

    .end local v0    # "$changed\\31":I
    .local v47, "$changed\\31":I
    const v0, -0x20f7d59c

    move/from16 v48, v12

    .end local v12    # "$changed\\28":I
    .local v48, "$changed\\28":I
    const-string v12, "CC(ReusableComposeNode)N(factory,update,content)399@15590L9:Composables.kt#9igjgp"

    invoke-static {v7, v0, v12}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    .line 250
    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->getApplier()Landroidx/compose/runtime/Applier;

    move-result-object v0

    instance-of v0, v0, Landroidx/compose/runtime/Applier;

    if-nez v0, :cond_75b

    invoke-static {}, Landroidx/compose/runtime/ComposablesKt;->invalidApplier()V

    .line 251
    :cond_75b
    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->startReusableNode()V

    .line 252
    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->getInserting()Z

    move-result v0

    if-eqz v0, :cond_76a

    .line 253
    move-object/from16 v0, v46

    .end local v46    # "factory\\31":Lkotlin/jvm/functions/Function0;
    .local v0, "factory\\31":Lkotlin/jvm/functions/Function0;
    invoke-interface {v7, v0}, Landroidx/compose/runtime/Composer;->createNode(Lkotlin/jvm/functions/Function0;)V

    goto :goto_76f

    .line 255
    .end local v0    # "factory\\31":Lkotlin/jvm/functions/Function0;
    .restart local v46    # "factory\\31":Lkotlin/jvm/functions/Function0;
    :cond_76a
    move-object/from16 v0, v46

    .end local v46    # "factory\\31":Lkotlin/jvm/functions/Function0;
    .restart local v0    # "factory\\31":Lkotlin/jvm/functions/Function0;
    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->useNode()V

    .line 257
    :goto_76f
    invoke-static {v7}, Landroidx/compose/runtime/Updater;->constructor-impl(Landroidx/compose/runtime/Composer;)Landroidx/compose/runtime/Composer;

    move-result-object v12

    .local v12, "$this$Layout_u24lambda_u240\\32":Landroidx/compose/runtime/Composer;
    const/16 v46, 0x0

    .line 258
    .local v46, "$i$a$-ReusableComposeNode-LayoutKt$Layout$1\\32\\257\\30":I
    sget-object v49, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    move-object/from16 v50, v0

    .end local v0    # "factory\\31":Lkotlin/jvm/functions/Function0;
    .local v50, "factory\\31":Lkotlin/jvm/functions/Function0;
    invoke-virtual/range {v49 .. v49}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getSetMeasurePolicy()Lkotlin/jvm/functions/Function2;

    move-result-object v0

    invoke-static {v12, v6, v0}, Landroidx/compose/runtime/Updater;->set-impl(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V

    .line 259
    sget-object v0, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    invoke-virtual {v0}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getSetResolvedCompositionLocals()Lkotlin/jvm/functions/Function2;

    move-result-object v0

    invoke-static {v12, v3, v0}, Landroidx/compose/runtime/Updater;->set-impl(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V

    .line 260
    sget-object v0, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    invoke-virtual {v0}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getSetCompositeKeyHash()Lkotlin/jvm/functions/Function2;

    move-result-object v0

    .local v0, "block\\33":Lkotlin/jvm/functions/Function2;
    const/16 v49, 0x0

    .line 261
    .local v49, "$i$f$set-impl\\33\\260":I
    move-object/from16 v51, v12

    .local v51, "$this$set_impl_u24lambda_u240\\33":Landroidx/compose/runtime/Composer;
    const/16 v52, 0x0

    .line 262
    .local v52, "$i$a$-with-Updater$set$1\\34\\261\\33":I
    invoke-interface/range {v51 .. v51}, Landroidx/compose/runtime/Composer;->getInserting()Z

    move-result v53

    if-nez v53, :cond_7b1

    move-object/from16 v53, v3

    .end local v3    # "localMap\\30":Landroidx/compose/runtime/CompositionLocalMap;
    .local v53, "localMap\\30":Landroidx/compose/runtime/CompositionLocalMap;
    invoke-interface/range {v51 .. v51}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v3

    move-object/from16 v54, v6

    .end local v6    # "measurePolicy\\29":Landroidx/compose/ui/layout/MeasurePolicy;
    .local v54, "measurePolicy\\29":Landroidx/compose/ui/layout/MeasurePolicy;
    invoke-static/range {v43 .. v43}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v6

    invoke-static {v3, v6}, Lkotlin/jvm/internal/Intrinsics;->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_7ae

    goto :goto_7b5

    :cond_7ae
    move-object/from16 v6, v51

    goto :goto_7c5

    .end local v53    # "localMap\\30":Landroidx/compose/runtime/CompositionLocalMap;
    .end local v54    # "measurePolicy\\29":Landroidx/compose/ui/layout/MeasurePolicy;
    .restart local v3    # "localMap\\30":Landroidx/compose/runtime/CompositionLocalMap;
    .restart local v6    # "measurePolicy\\29":Landroidx/compose/ui/layout/MeasurePolicy;
    :cond_7b1
    move-object/from16 v53, v3

    move-object/from16 v54, v6

    .line 263
    .end local v3    # "localMap\\30":Landroidx/compose/runtime/CompositionLocalMap;
    .end local v6    # "measurePolicy\\29":Landroidx/compose/ui/layout/MeasurePolicy;
    .restart local v53    # "localMap\\30":Landroidx/compose/runtime/CompositionLocalMap;
    .restart local v54    # "measurePolicy\\29":Landroidx/compose/ui/layout/MeasurePolicy;
    :goto_7b5
    invoke-static/range {v43 .. v43}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v3

    move-object/from16 v6, v51

    .end local v51    # "$this$set_impl_u24lambda_u240\\33":Landroidx/compose/runtime/Composer;
    .local v6, "$this$set_impl_u24lambda_u240\\33":Landroidx/compose/runtime/Composer;
    invoke-interface {v6, v3}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 264
    invoke-static/range {v43 .. v43}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v3

    invoke-interface {v12, v3, v0}, Landroidx/compose/runtime/Composer;->apply(Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V

    .line 266
    :goto_7c5
    nop

    .line 261
    .end local v6    # "$this$set_impl_u24lambda_u240\\33":Landroidx/compose/runtime/Composer;
    .end local v52    # "$i$a$-with-Updater$set$1\\34\\261\\33":I
    nop

    .line 266
    nop

    .line 267
    .end local v0    # "block\\33":Lkotlin/jvm/functions/Function2;
    .end local v49    # "$i$f$set-impl\\33\\260":I
    sget-object v0, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    invoke-virtual {v0}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getSetModifier()Lkotlin/jvm/functions/Function2;

    move-result-object v0

    invoke-static {v12, v11, v0}, Landroidx/compose/runtime/Updater;->set-impl(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V

    .line 268
    nop

    .line 257
    .end local v12    # "$this$Layout_u24lambda_u240\\32":Landroidx/compose/runtime/Composer;
    .end local v46    # "$i$a$-ReusableComposeNode-LayoutKt$Layout$1\\32\\257\\30":I
    nop

    .line 269
    shr-int/lit8 v0, v47, 0x6

    and-int/lit8 v0, v0, 0xe

    .local v0, "$changed\\35":I
    move-object v3, v7

    .local v3, "$composer\\35":Landroidx/compose/runtime/Composer;
    const/4 v6, 0x0

    .line 240
    .local v6, "$i$a$-Layout-RowKt$Row$1\\35\\269\\29":I
    const v12, 0x56ccd6f5

    move/from16 v46, v0

    .end local v0    # "$changed\\35":I
    .local v46, "$changed\\35":I
    const-string v0, "C101@5233L9:Row.kt#2w3rfo"

    invoke-static {v3, v12, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    sget-object v0, Landroidx/compose/foundation/layout/RowScopeInstance;->INSTANCE:Landroidx/compose/foundation/layout/RowScopeInstance;

    shr-int/lit8 v12, p7, 0x6

    and-int/lit8 v12, v12, 0x70

    or-int/lit8 v12, v12, 0x6

    .local v12, "$changed\\36":I
    check-cast v0, Landroidx/compose/foundation/layout/RowScope;

    .local v0, "$this$SettingsShell_u24lambda_u248_u240\\36":Landroidx/compose/foundation/layout/RowScope;
    move-object/from16 v55, v0

    .end local v0    # "$this$SettingsShell_u24lambda_u248_u240\\36":Landroidx/compose/foundation/layout/RowScope;
    .local v55, "$this$SettingsShell_u24lambda_u248_u240\\36":Landroidx/compose/foundation/layout/RowScope;
    move-object v0, v3

    .local v0, "$composer\\36":Landroidx/compose/runtime/Composer;
    const/16 v49, 0x0

    .line 59
    .local v49, "$i$a$-Row-SettingsShellKt$SettingsShell$6$1\\36\\240\\28":I
    move-object/from16 p4, v3

    .end local v3    # "$composer\\35":Landroidx/compose/runtime/Composer;
    .local p4, "$composer\\35":Landroidx/compose/runtime/Composer;
    const v3, -0x71e5bfd9

    move/from16 v51, v6

    .end local v6    # "$i$a$-Layout-RowKt$Row$1\\35\\269\\29":I
    .local v51, "$i$a$-Layout-RowKt$Row$1\\35\\269\\29":I
    const-string v6, "C58@2788L10,59@2872L13,58@2755L131,60@2910L26,60@2899L162:SettingsShell.kt#e7edxl"

    invoke-static {v0, v3, v6}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    sget-object v3, Landroidx/compose/material3/MaterialTheme;->INSTANCE:Landroidx/compose/material3/MaterialTheme;

    sget v6, Landroidx/compose/material3/MaterialTheme;->$stable:I

    invoke-virtual {v3, v0, v6}, Landroidx/compose/material3/MaterialTheme;->getTypography(Landroidx/compose/runtime/Composer;I)Landroidx/compose/material3/Typography;

    move-result-object v3

    invoke-virtual {v3}, Landroidx/compose/material3/Typography;->getHeadlineMedium()Landroidx/compose/ui/text/TextStyle;

    move-result-object v77

    .line 60
    sget-object v3, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    move-object/from16 v56, v3

    check-cast v56, Landroidx/compose/ui/Modifier;

    const/16 v59, 0x2

    const/16 v60, 0x0

    const/high16 v57, 0x3f800000    # 1.0f

    const/16 v58, 0x0

    invoke-static/range {v55 .. v60}, Landroidx/compose/foundation/layout/RowScope;->weight$default(Landroidx/compose/foundation/layout/RowScope;Landroidx/compose/ui/Modifier;FZILjava/lang/Object;)Landroidx/compose/ui/Modifier;

    move-result-object v3

    const v6, -0x24b4c729

    invoke-static {v0, v6, v2}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    const/4 v6, 0x0

    .local v6, "invalid\\37":Z
    move-object/from16 v52, v0

    .local v52, "$this$cache\\37":Landroidx/compose/runtime/Composer;
    const/16 v56, 0x0

    .line 270
    .local v56, "$i$f$cache\\37\\60":I
    move-object/from16 v78, v0

    .end local v0    # "$composer\\36":Landroidx/compose/runtime/Composer;
    .local v78, "$composer\\36":Landroidx/compose/runtime/Composer;
    invoke-interface/range {v52 .. v52}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v0

    .local v0, "it\\37":Ljava/lang/Object;
    const/16 v57, 0x0

    .line 271
    .local v57, "$i$a$-let-ComposerKt$cache$1\\38\\270\\37":I
    sget-object v58, Landroidx/compose/runtime/Composer;->Companion:Landroidx/compose/runtime/Composer$Companion;

    move/from16 v59, v6

    .end local v6    # "invalid\\37":Z
    .local v59, "invalid\\37":Z
    invoke-virtual/range {v58 .. v58}, Landroidx/compose/runtime/Composer$Companion;->getEmpty()Ljava/lang/Object;

    move-result-object v6

    if-ne v0, v6, :cond_84b

    .line 272
    const/4 v6, 0x0

    .local v6, "$i$a$-cache-SettingsShellKt$SettingsShell$6$1$1\\39\\272\\36":I
    new-instance v58, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda3;

    invoke-direct/range {v58 .. v58}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda3;-><init>()V

    .end local v6    # "$i$a$-cache-SettingsShellKt$SettingsShell$6$1$1\\39\\272\\36":I
    move-object/from16 v6, v58

    .line 273
    .local v6, "value\\38":Ljava/lang/Object;
    move-object/from16 v58, v0

    move-object/from16 v0, v52

    .end local v52    # "$this$cache\\37":Landroidx/compose/runtime/Composer;
    .local v0, "$this$cache\\37":Landroidx/compose/runtime/Composer;
    .local v58, "it\\37":Ljava/lang/Object;
    invoke-interface {v0, v6}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 274
    move-object/from16 v58, v6

    .end local v6    # "value\\38":Ljava/lang/Object;
    goto :goto_84f

    .line 275
    .end local v58    # "it\\37":Ljava/lang/Object;
    .local v0, "it\\37":Ljava/lang/Object;
    .restart local v52    # "$this$cache\\37":Landroidx/compose/runtime/Composer;
    :cond_84b
    move-object/from16 v58, v0

    move-object/from16 v0, v52

    .line 270
    .end local v52    # "$this$cache\\37":Landroidx/compose/runtime/Composer;
    .end local v57    # "$i$a$-let-ComposerKt$cache$1\\38\\270\\37":I
    .local v0, "$this$cache\\37":Landroidx/compose/runtime/Composer;
    :goto_84f
    nop

    .line 60
    .end local v0    # "$this$cache\\37":Landroidx/compose/runtime/Composer;
    .end local v56    # "$i$f$cache\\37\\60":I
    .end local v59    # "invalid\\37":Z
    move-object/from16 v0, v58

    check-cast v0, Lkotlin/jvm/functions/Function1;

    invoke-static/range {v78 .. v78}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    const/4 v6, 0x0

    move-object/from16 v52, v7

    move-object/from16 p19, v11

    const/4 v7, 0x0

    const/4 v11, 0x1

    .end local v7    # "$composer\\29":Landroidx/compose/runtime/Composer;
    .end local v11    # "materialized\\30":Landroidx/compose/ui/Modifier;
    .local v52, "$composer\\29":Landroidx/compose/runtime/Composer;
    .local p19, "materialized\\30":Landroidx/compose/ui/Modifier;
    invoke-static {v3, v7, v0, v11, v6}, Landroidx/compose/ui/semantics/SemanticsModifierKt;->semantics$default(Landroidx/compose/ui/Modifier;ZLkotlin/jvm/functions/Function1;ILjava/lang/Object;)Landroidx/compose/ui/Modifier;

    move-result-object v57

    .line 59
    nop

    .line 60
    nop

    .line 59
    const/16 v80, 0x0

    const v81, 0x1fffc

    const-string v56, "\u8bbe\u7f6e"

    const-wide/16 v58, 0x0

    const/16 v60, 0x0

    const-wide/16 v61, 0x0

    const/16 v63, 0x0

    const/16 v64, 0x0

    const/16 v65, 0x0

    const-wide/16 v66, 0x0

    const/16 v68, 0x0

    const/16 v69, 0x0

    const-wide/16 v70, 0x0

    const/16 v72, 0x0

    const/16 v73, 0x0

    const/16 v74, 0x0

    const/16 v75, 0x0

    const/16 v76, 0x0

    const/16 v79, 0x6

    invoke-static/range {v56 .. v81}, Landroidx/compose/material3/TextKt;->Text-Nvy7gAk(Ljava/lang/String;Landroidx/compose/ui/Modifier;JLandroidx/compose/foundation/text/TextAutoSize;JLandroidx/compose/ui/text/font/FontStyle;Landroidx/compose/ui/text/font/FontWeight;Landroidx/compose/ui/text/font/FontFamily;JLandroidx/compose/ui/text/style/TextDecoration;Landroidx/compose/ui/text/style/TextAlign;JIZIILkotlin/jvm/functions/Function1;Landroidx/compose/ui/text/TextStyle;Landroidx/compose/runtime/Composer;III)V

    .line 61
    move-object/from16 v0, v78

    .end local v78    # "$composer\\36":Landroidx/compose/runtime/Composer;
    .local v0, "$composer\\36":Landroidx/compose/runtime/Composer;
    const v3, -0x24b4c25c

    invoke-static {v0, v3, v2}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    invoke-interface {v0, v4}, Landroidx/compose/runtime/Composer;->changed(Ljava/lang/Object;)Z

    move-result v3

    .local v3, "invalid\\40":Z
    move-object v6, v0

    .local v6, "$this$cache\\40":Landroidx/compose/runtime/Composer;
    const/4 v7, 0x0

    .line 276
    .local v7, "$i$f$cache\\40\\61":I
    invoke-interface {v6}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v11

    .local v11, "it\\40":Ljava/lang/Object;
    const/16 v56, 0x0

    .line 277
    .local v56, "$i$a$-let-ComposerKt$cache$1\\41\\276\\40":I
    if-nez v3, :cond_8b0

    sget-object v57, Landroidx/compose/runtime/Composer;->Companion:Landroidx/compose/runtime/Composer$Companion;

    move-object/from16 v78, v0

    .end local v0    # "$composer\\36":Landroidx/compose/runtime/Composer;
    .restart local v78    # "$composer\\36":Landroidx/compose/runtime/Composer;
    invoke-virtual/range {v57 .. v57}, Landroidx/compose/runtime/Composer$Companion;->getEmpty()Ljava/lang/Object;

    move-result-object v0

    if-ne v11, v0, :cond_8af

    goto :goto_8b2

    .line 281
    :cond_8af
    goto :goto_8bf

    .line 277
    .end local v78    # "$composer\\36":Landroidx/compose/runtime/Composer;
    .restart local v0    # "$composer\\36":Landroidx/compose/runtime/Composer;
    :cond_8b0
    move-object/from16 v78, v0

    .line 278
    .end local v0    # "$composer\\36":Landroidx/compose/runtime/Composer;
    .restart local v78    # "$composer\\36":Landroidx/compose/runtime/Composer;
    :goto_8b2
    const/4 v0, 0x0

    .line 61
    .local v0, "$i$a$-cache-SettingsShellKt$SettingsShell$6$1$2\\42\\278\\36":I
    move/from16 v57, v0

    .end local v0    # "$i$a$-cache-SettingsShellKt$SettingsShell$6$1$2\\42\\278\\36":I
    .local v57, "$i$a$-cache-SettingsShellKt$SettingsShell$6$1$2\\42\\278\\36":I
    new-instance v0, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda4;

    invoke-direct {v0, v4}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda4;-><init>(Landroidx/compose/runtime/MutableState;)V

    .line 278
    .end local v57    # "$i$a$-cache-SettingsShellKt$SettingsShell$6$1$2\\42\\278\\36":I
    nop

    .line 279
    .local v0, "value\\41":Ljava/lang/Object;
    invoke-interface {v6, v0}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 280
    move-object v11, v0

    .line 276
    .end local v0    # "value\\41":Ljava/lang/Object;
    .end local v11    # "it\\40":Ljava/lang/Object;
    .end local v56    # "$i$a$-let-ComposerKt$cache$1\\41\\276\\40":I
    :goto_8bf
    nop

    .line 61
    .end local v3    # "invalid\\40":Z
    .end local v6    # "$this$cache\\40":Landroidx/compose/runtime/Composer;
    .end local v7    # "$i$f$cache\\40\\61":I
    move-object/from16 v56, v11

    check-cast v56, Lkotlin/jvm/functions/Function0;

    invoke-static/range {v78 .. v78}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    sget-object v0, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v0, Landroidx/compose/ui/Modifier;

    const-string v3, "settings-information"

    invoke-static {v0, v3}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v57

    sget-object v0, Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;->INSTANCE:Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;

    invoke-virtual {v0}, Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;->getLambda$616603938$app_debug()Lkotlin/jvm/functions/Function2;

    move-result-object v61

    const v63, 0x30030

    const/16 v64, 0x1c

    const/16 v58, 0x0

    const/16 v59, 0x0

    const/16 v60, 0x0

    move-object/from16 v62, v78

    .end local v78    # "$composer\\36":Landroidx/compose/runtime/Composer;
    .local v62, "$composer\\36":Landroidx/compose/runtime/Composer;
    invoke-static/range {v56 .. v64}, Lcom/cooper/lithic/core/design/LithicControlsKt;->LithicIconButton(Lkotlin/jvm/functions/Function0;Landroidx/compose/ui/Modifier;ZLandroidx/compose/material3/IconButtonColors;Landroidx/compose/foundation/interaction/MutableInteractionSource;Lkotlin/jvm/functions/Function2;Landroidx/compose/runtime/Composer;II)V

    .line 59
    .end local v62    # "$composer\\36":Landroidx/compose/runtime/Composer;
    .restart local v78    # "$composer\\36":Landroidx/compose/runtime/Composer;
    invoke-static/range {v78 .. v78}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 64
    nop

    .line 240
    .end local v12    # "$changed\\36":I
    .end local v49    # "$i$a$-Row-SettingsShellKt$SettingsShell$6$1\\36\\240\\28":I
    .end local v55    # "$this$SettingsShell_u24lambda_u248_u240\\36":Landroidx/compose/foundation/layout/RowScope;
    .end local v78    # "$composer\\36":Landroidx/compose/runtime/Composer;
    invoke-static/range {p4 .. p4}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 269
    .end local v46    # "$changed\\35":I
    .end local v51    # "$i$a$-Layout-RowKt$Row$1\\35\\269\\29":I
    .end local p4    # "$composer\\35":Landroidx/compose/runtime/Composer;
    nop

    .line 282
    invoke-interface/range {v52 .. v52}, Landroidx/compose/runtime/Composer;->endNode()V

    .line 249
    invoke-static/range {v52 .. v52}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 283
    nop

    .line 243
    .end local v45    # "$i$f$ReusableComposeNode\\31\\247":I
    .end local v47    # "$changed\\31":I
    .end local v50    # "factory\\31":Lkotlin/jvm/functions/Function0;
    invoke-static/range {v52 .. v52}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 284
    nop

    .line 233
    .end local v35    # "$changed\\30":I
    .end local v36    # "$i$f$Layout\\30\\239":I
    .end local v43    # "compositeKeyHash\\30":I
    .end local v53    # "localMap\\30":Landroidx/compose/runtime/CompositionLocalMap;
    .end local p8    # "modifier\\30":Landroidx/compose/ui/Modifier;
    .end local p19    # "materialized\\30":Landroidx/compose/ui/Modifier;
    invoke-static/range {v52 .. v52}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 285
    nop

    .line 65
    .end local v34    # "$i$f$Row\\29\\58":I
    .end local v38    # "horizontalArrangement\\29":Landroidx/compose/foundation/layout/Arrangement$Horizontal;
    .end local v39    # "verticalAlignment\\29":Landroidx/compose/ui/Alignment$Vertical;
    .end local v52    # "$composer\\29":Landroidx/compose/runtime/Composer;
    .end local v54    # "measurePolicy\\29":Landroidx/compose/ui/layout/MeasurePolicy;
    .end local p1    # "modifier\\29":Landroidx/compose/ui/Modifier;
    .end local p7    # "$changed\\29":I
    sget-object v0, Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;->INSTANCE:Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;

    invoke-virtual {v0}, Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;->getLambda$-1086236171$app_debug()Lkotlin/jvm/functions/Function3;

    move-result-object v36

    const/16 v38, 0x186

    const/16 v39, 0x2

    const-string v34, "\u5916\u89c2"

    const/16 v35, 0x0

    invoke-static/range {v34 .. v39}, Lcom/cooper/lithic/core/design/C2PageKt;->C2Section(Ljava/lang/String;Landroidx/compose/ui/Modifier;Lkotlin/jvm/functions/Function3;Landroidx/compose/runtime/Composer;II)V

    .line 66
    move-object/from16 v6, v37

    .end local v37    # "$composer\\28":Landroidx/compose/runtime/Composer;
    .local v6, "$composer\\28":Landroidx/compose/runtime/Composer;
    new-instance v0, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda5;

    invoke-direct {v0, v8, v1}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda5;-><init>(Lkotlin/jvm/functions/Function0;Ljava/time/ZoneId;)V

    const v3, 0x5e9d63de

    const/16 v7, 0x36

    const/4 v11, 0x1

    invoke-static {v3, v11, v0, v6, v7}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->rememberComposableLambda(IZLjava/lang/Object;Landroidx/compose/runtime/Composer;I)Landroidx/compose/runtime/internal/ComposableLambda;

    move-result-object v0

    move-object/from16 v36, v0

    check-cast v36, Lkotlin/jvm/functions/Function3;

    const-string v34, "\u8bb0\u5f55\u7ba1\u7406"

    .end local v6    # "$composer\\28":Landroidx/compose/runtime/Composer;
    .restart local v37    # "$composer\\28":Landroidx/compose/runtime/Composer;
    invoke-static/range {v34 .. v39}, Lcom/cooper/lithic/core/design/C2PageKt;->C2Section(Ljava/lang/String;Landroidx/compose/ui/Modifier;Lkotlin/jvm/functions/Function3;Landroidx/compose/runtime/Composer;II)V

    .line 73
    .end local v37    # "$composer\\28":Landroidx/compose/runtime/Composer;
    .restart local v6    # "$composer\\28":Landroidx/compose/runtime/Composer;
    if-nez v28, :cond_935

    const v0, 0x41db4bbd

    invoke-interface {v6, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    .line 77
    invoke-interface {v6}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 73
    goto :goto_96b

    :cond_935
    const v0, 0x41db4bbe

    invoke-interface {v6, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v0, "*73@3550L130,73@3534L146"

    invoke-static {v6, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    move-object/from16 v0, v28

    .local v0, "open\\43":Lkotlin/jvm/functions/Function0;
    const/4 v3, 0x0

    .line 74
    .local v3, "$i$a$-let-SettingsShellKt$SettingsShell$6$3\\43\\73\\28":I
    new-instance v11, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda6;

    invoke-direct {v11, v0}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda6;-><init>(Lkotlin/jvm/functions/Function0;)V

    const v12, -0x236ff99e

    move-object/from16 p1, v0

    const/4 v0, 0x1

    .end local v0    # "open\\43":Lkotlin/jvm/functions/Function0;
    .local p1, "open\\43":Lkotlin/jvm/functions/Function0;
    invoke-static {v12, v0, v11, v6, v7}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->rememberComposableLambda(IZLjava/lang/Object;Landroidx/compose/runtime/Composer;I)Landroidx/compose/runtime/internal/ComposableLambda;

    move-result-object v11

    move-object/from16 v36, v11

    check-cast v36, Lkotlin/jvm/functions/Function3;

    const/16 v38, 0x186

    const/16 v39, 0x2

    const-string v34, "\u63d0\u9192"

    const/16 v35, 0x0

    move-object/from16 v37, v6

    .end local v6    # "$composer\\28":Landroidx/compose/runtime/Composer;
    .restart local v37    # "$composer\\28":Landroidx/compose/runtime/Composer;
    invoke-static/range {v34 .. v39}, Lcom/cooper/lithic/core/design/C2PageKt;->C2Section(Ljava/lang/String;Landroidx/compose/ui/Modifier;Lkotlin/jvm/functions/Function3;Landroidx/compose/runtime/Composer;II)V

    .line 77
    .end local v37    # "$composer\\28":Landroidx/compose/runtime/Composer;
    .restart local v6    # "$composer\\28":Landroidx/compose/runtime/Composer;
    nop

    .end local v3    # "$i$a$-let-SettingsShellKt$SettingsShell$6$3\\43\\73\\28":I
    .end local p1    # "open\\43":Lkotlin/jvm/functions/Function0;
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    .line 73
    invoke-interface {v6}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    .line 78
    :goto_96b
    new-instance v0, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda11;

    move-object/from16 p1, v0

    move-object/from16 p4, v16

    move-object/from16 p7, v17

    move-object/from16 p8, v19

    .end local v16    # "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .end local v17    # "onInspectBackup":Lkotlin/jvm/functions/Function0;
    .end local v19    # "onCancelInspection":Lkotlin/jvm/functions/Function0;
    .local p4, "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .local p7, "onInspectBackup":Lkotlin/jvm/functions/Function0;
    .local p8, "onCancelInspection":Lkotlin/jvm/functions/Function0;
    invoke-direct/range {p1 .. p12}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda11;-><init>(Lcom/cooper/lithic/feature/settings/BackupExportUiState;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V

    move-object/from16 v11, p1

    move-object/from16 v0, p2

    move-object/from16 v3, p3

    move-object/from16 v43, p4

    move-object/from16 v45, p5

    move-object/from16 v46, p6

    move-object/from16 v47, p7

    move-object/from16 v49, p8

    move-object/from16 v50, p9

    move-object/from16 v51, p10

    move-object/from16 v52, p11

    move-object/from16 v53, p12

    .end local p2    # "backupState":Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    .end local p3    # "onExportBackup":Lkotlin/jvm/functions/Function0;
    .end local p4    # "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .end local p5    # "restoreState":Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;
    .end local p6    # "onCancelBackup":Lkotlin/jvm/functions/Function0;
    .end local p7    # "onInspectBackup":Lkotlin/jvm/functions/Function0;
    .end local p8    # "onCancelInspection":Lkotlin/jvm/functions/Function0;
    .end local p9    # "onRestoreBackup":Lkotlin/jvm/functions/Function0;
    .end local p10    # "onCancelRestore":Lkotlin/jvm/functions/Function0;
    .end local p11    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .end local p12    # "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    .local v0, "backupState":Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    .local v3, "onExportBackup":Lkotlin/jvm/functions/Function0;
    .local v43, "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .local v45, "restoreState":Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;
    .local v46, "onCancelBackup":Lkotlin/jvm/functions/Function0;
    .local v47, "onInspectBackup":Lkotlin/jvm/functions/Function0;
    .local v49, "onCancelInspection":Lkotlin/jvm/functions/Function0;
    .local v50, "onRestoreBackup":Lkotlin/jvm/functions/Function0;
    .local v51, "onCancelRestore":Lkotlin/jvm/functions/Function0;
    .local v52, "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .local v53, "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    const v12, -0x64d05243

    const/4 v1, 0x1

    invoke-static {v12, v1, v11, v6, v7}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->rememberComposableLambda(IZLjava/lang/Object;Landroidx/compose/runtime/Composer;I)Landroidx/compose/runtime/internal/ComposableLambda;

    move-result-object v11

    move-object/from16 v36, v11

    check-cast v36, Lkotlin/jvm/functions/Function3;

    const/16 v38, 0x186

    const/16 v39, 0x2

    const-string v34, "\u5907\u4efd\u4e0e\u6062\u590d"

    const/16 v35, 0x0

    move-object/from16 v37, v6

    .end local v6    # "$composer\\28":Landroidx/compose/runtime/Composer;
    .restart local v37    # "$composer\\28":Landroidx/compose/runtime/Composer;
    invoke-static/range {v34 .. v39}, Lcom/cooper/lithic/core/design/C2PageKt;->C2Section(Ljava/lang/String;Landroidx/compose/ui/Modifier;Lkotlin/jvm/functions/Function3;Landroidx/compose/runtime/Composer;II)V

    .line 130
    .end local v37    # "$composer\\28":Landroidx/compose/runtime/Composer;
    .restart local v6    # "$composer\\28":Landroidx/compose/runtime/Composer;
    shr-int/lit8 v1, v29, 0x15

    and-int/lit8 v1, v1, 0xe

    shr-int/lit8 v11, v29, 0x15

    and-int/lit8 v11, v11, 0x70

    or-int/2addr v1, v11

    invoke-static {v9, v10, v6, v1}, Lcom/cooper/lithic/feature/settings/DataConfidenceSectionsKt;->PhotoStorageSection(Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;Lkotlin/jvm/functions/Function0;Landroidx/compose/runtime/Composer;I)V

    .line 131
    new-instance v1, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda12;

    invoke-direct {v1, v13, v15}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda12;-><init>(Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V

    const v11, -0x283e0864

    const/4 v12, 0x1

    invoke-static {v11, v12, v1, v6, v7}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->rememberComposableLambda(IZLjava/lang/Object;Landroidx/compose/runtime/Composer;I)Landroidx/compose/runtime/internal/ComposableLambda;

    move-result-object v1

    move-object/from16 v36, v1

    check-cast v36, Lkotlin/jvm/functions/Function3;

    const-string v34, "\u8bf4\u660e\u4e0e\u5173\u4e8e"

    .end local v6    # "$composer\\28":Landroidx/compose/runtime/Composer;
    .restart local v37    # "$composer\\28":Landroidx/compose/runtime/Composer;
    invoke-static/range {v34 .. v39}, Lcom/cooper/lithic/core/design/C2PageKt;->C2Section(Ljava/lang/String;Landroidx/compose/ui/Modifier;Lkotlin/jvm/functions/Function3;Landroidx/compose/runtime/Composer;II)V

    .line 58
    invoke-static/range {v37 .. v37}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 140
    nop

    .line 203
    .end local v27    # "$i$a$-Column-SettingsShellKt$SettingsShell$6\\28\\203\\0":I
    .end local v32    # "$this$SettingsShell_u24lambda_u248\\28":Landroidx/compose/foundation/layout/ColumnScope;
    .end local v37    # "$composer\\28":Landroidx/compose/runtime/Composer;
    .end local v48    # "$changed\\28":I
    invoke-static/range {v40 .. v40}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 232
    .end local v26    # "$changed\\27":I
    .end local v40    # "$composer\\27":Landroidx/compose/runtime/Composer;
    .end local v41    # "$i$a$-Layout-ColumnKt$Column$1\\27\\232\\21":I
    nop

    .line 286
    invoke-interface/range {p15 .. p15}, Landroidx/compose/runtime/Composer;->endNode()V

    .line 212
    invoke-static/range {p15 .. p15}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 287
    nop

    .line 206
    .end local v23    # "$i$f$ReusableComposeNode\\23\\210":I
    .end local v25    # "$changed\\23":I
    .end local v31    # "factory\\23":Lkotlin/jvm/functions/Function0;
    invoke-static/range {p15 .. p15}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 288
    nop

    .line 197
    .end local v21    # "$changed\\22":I
    .end local v22    # "$i$f$Layout\\22\\202":I
    .end local v24    # "modifier\\22":Landroidx/compose/ui/Modifier;
    .end local v33    # "localMap\\22":Landroidx/compose/runtime/CompositionLocalMap;
    .end local v44    # "materialized\\22":Landroidx/compose/ui/Modifier;
    .end local p18    # "compositeKeyHash\\22":I
    invoke-static/range {p15 .. p15}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 289
    nop

    .line 141
    .end local v20    # "$i$f$Column\\21\\56":I
    .end local v42    # "measurePolicy\\21":Landroidx/compose/ui/layout/MeasurePolicy;
    .end local p13    # "$changed\\21":I
    .end local p14    # "modifier\\21":Landroidx/compose/ui/Modifier;
    .end local p15    # "$composer\\21":Landroidx/compose/runtime/Composer;
    .end local p16    # "verticalArrangement\\21":Landroidx/compose/foundation/layout/Arrangement$Vertical;
    .end local p17    # "horizontalAlignment\\21":Landroidx/compose/ui/Alignment$Horizontal;
    invoke-static {v4}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$6(Landroidx/compose/runtime/MutableState;)Z

    move-result v1

    if-eqz v1, :cond_a9d

    const v1, 0x7ba0f5ac

    invoke-interface {v5, v1}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v1, "140@7526L27,155@8419L107,142@7607L794,140@7495L1032"

    invoke-static {v5, v1}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    const v1, 0x7ba0f59f

    invoke-static {v5, v1, v2}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    invoke-interface {v5, v4}, Landroidx/compose/runtime/Composer;->changed(Ljava/lang/Object;)Z

    move-result v1

    .local v1, "invalid\\44":Z
    move-object v2, v5

    .local v2, "$this$cache\\44":Landroidx/compose/runtime/Composer;
    const/4 v6, 0x0

    .line 290
    .local v6, "$i$f$cache\\44\\141":I
    invoke-interface {v2}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v11

    .local v11, "it\\44":Ljava/lang/Object;
    const/4 v12, 0x0

    .line 291
    .local v12, "$i$a$-let-ComposerKt$cache$1\\45\\290\\44":I
    if-nez v1, :cond_a12

    sget-object v16, Landroidx/compose/runtime/Composer;->Companion:Landroidx/compose/runtime/Composer$Companion;

    invoke-virtual/range {v16 .. v16}, Landroidx/compose/runtime/Composer$Companion;->getEmpty()Ljava/lang/Object;

    move-result-object v7

    if-ne v11, v7, :cond_a0f

    goto :goto_a12

    .line 295
    :cond_a0f
    move/from16 p2, v1

    goto :goto_a1f

    .line 292
    :cond_a12
    :goto_a12
    const/4 v7, 0x0

    .line 141
    .local v7, "$i$a$-cache-SettingsShellKt$SettingsShell$7\\46\\292\\0":I
    move/from16 p2, v1

    .end local v1    # "invalid\\44":Z
    .local p2, "invalid\\44":Z
    new-instance v1, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda13;

    invoke-direct {v1, v4}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda13;-><init>(Landroidx/compose/runtime/MutableState;)V

    .line 292
    .end local v7    # "$i$a$-cache-SettingsShellKt$SettingsShell$7\\46\\292\\0":I
    nop

    .line 293
    .local v1, "value\\45":Ljava/lang/Object;
    invoke-interface {v2, v1}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 294
    move-object v11, v1

    .line 290
    .end local v1    # "value\\45":Ljava/lang/Object;
    .end local v11    # "it\\44":Ljava/lang/Object;
    .end local v12    # "$i$a$-let-ComposerKt$cache$1\\45\\290\\44":I
    :goto_a1f
    nop

    .line 141
    .end local v2    # "$this$cache\\44":Landroidx/compose/runtime/Composer;
    .end local v6    # "$i$f$cache\\44\\141":I
    .end local p2    # "invalid\\44":Z
    move-object v6, v11

    check-cast v6, Lkotlin/jvm/functions/Function0;

    invoke-static {v5}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 156
    new-instance v1, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda14;

    invoke-direct {v1, v4}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda14;-><init>(Landroidx/compose/runtime/MutableState;)V

    const v2, -0x21853faf

    const/16 v7, 0x36

    const/4 v11, 0x1

    invoke-static {v2, v11, v1, v5, v7}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->rememberComposableLambda(IZLjava/lang/Object;Landroidx/compose/runtime/Composer;I)Landroidx/compose/runtime/internal/ComposableLambda;

    move-result-object v1

    check-cast v1, Lkotlin/jvm/functions/Function2;

    sget-object v2, Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;->INSTANCE:Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;

    invoke-virtual {v2}, Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;->getLambda$-1477540523$app_debug()Lkotlin/jvm/functions/Function2;

    move-result-object v2

    .line 143
    new-instance v12, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda15;

    invoke-direct {v12, v0}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda15;-><init>(Lcom/cooper/lithic/feature/settings/BackupExportUiState;)V

    move-object/from16 p2, v0

    .end local v0    # "backupState":Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    .local p2, "backupState":Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    const v0, -0x65b4896a

    invoke-static {v0, v11, v12, v5, v7}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->rememberComposableLambda(IZLjava/lang/Object;Landroidx/compose/runtime/Composer;I)Landroidx/compose/runtime/internal/ComposableLambda;

    move-result-object v0

    move-object v12, v0

    check-cast v12, Lkotlin/jvm/functions/Function2;

    .line 141
    move-object v0, v8

    .end local v8    # "onManageCategories":Lkotlin/jvm/functions/Function0;
    .local v0, "onManageCategories":Lkotlin/jvm/functions/Function0;
    const/4 v8, 0x0

    move-object v7, v9

    .end local v9    # "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .local v7, "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    const/4 v9, 0x0

    move-object/from16 v19, v10

    .end local v10    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .local v19, "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    const/4 v10, 0x0

    move-object v11, v13

    .end local v13    # "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .local v11, "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    const/4 v13, 0x0

    move/from16 v16, v14

    move-object/from16 v17, v15

    .end local v14    # "$dirty":I
    .end local v15    # "onAboutLithic":Lkotlin/jvm/functions/Function0;
    .local v16, "$dirty":I
    .local v17, "onAboutLithic":Lkotlin/jvm/functions/Function0;
    const-wide/16 v14, 0x0

    move/from16 v18, v16

    move-object/from16 v20, v17

    .end local v16    # "$dirty":I
    .end local v17    # "onAboutLithic":Lkotlin/jvm/functions/Function0;
    .local v18, "$dirty":I
    .local v20, "onAboutLithic":Lkotlin/jvm/functions/Function0;
    const-wide/16 v16, 0x0

    move/from16 v21, v18

    move-object/from16 v22, v19

    .end local v18    # "$dirty":I
    .end local v19    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .local v21, "$dirty":I
    .local v22, "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    const-wide/16 v18, 0x0

    move-object/from16 v24, v20

    move/from16 v23, v21

    .end local v20    # "onAboutLithic":Lkotlin/jvm/functions/Function0;
    .end local v21    # "$dirty":I
    .local v23, "$dirty":I
    .local v24, "onAboutLithic":Lkotlin/jvm/functions/Function0;
    const-wide/16 v20, 0x0

    move-object/from16 v25, v22

    .end local v22    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .local v25, "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    const/16 v22, 0x0

    move/from16 v26, v23

    .end local v23    # "$dirty":I
    .local v26, "$dirty":I
    const/16 v23, 0x0

    move-object/from16 v27, v25

    .end local v25    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .local v27, "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    const v25, 0x1b0030

    move/from16 v31, v26

    .end local v26    # "$dirty":I
    .local v31, "$dirty":I
    const/16 v26, 0x0

    move-object/from16 v32, v27

    .end local v27    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .local v32, "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    const/16 v27, 0x3f9c

    move-object/from16 v83, v7

    move-object v7, v1

    move-object/from16 v1, v83

    move-object/from16 v83, v11

    move-object v11, v2

    move-object/from16 v2, v83

    move-object/from16 v83, v24

    move-object/from16 v24, v5

    move-object/from16 v5, v83

    .end local v7    # "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .end local v11    # "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .local v1, "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .local v2, "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .local v5, "onAboutLithic":Lkotlin/jvm/functions/Function0;
    .local v24, "$composer":Landroidx/compose/runtime/Composer;
    invoke-static/range {v6 .. v27}, Landroidx/compose/material3/AndroidAlertDialog_androidKt;->AlertDialog-Oix01E0(Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function2;Landroidx/compose/ui/Modifier;Lkotlin/jvm/functions/Function2;Lkotlin/jvm/functions/Function2;Lkotlin/jvm/functions/Function2;Lkotlin/jvm/functions/Function2;Landroidx/compose/ui/graphics/Shape;JJJJFLandroidx/compose/ui/window/DialogProperties;Landroidx/compose/runtime/Composer;III)V

    move-object/from16 v8, v24

    .end local v24    # "$composer":Landroidx/compose/runtime/Composer;
    .local v8, "$composer":Landroidx/compose/runtime/Composer;
    invoke-interface {v8}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    goto :goto_ab1

    .line 156
    .end local v1    # "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .end local v2    # "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .end local v31    # "$dirty":I
    .end local v32    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .end local p2    # "backupState":Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    .local v0, "backupState":Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    .local v5, "$composer":Landroidx/compose/runtime/Composer;
    .local v8, "onManageCategories":Lkotlin/jvm/functions/Function0;
    .restart local v9    # "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .restart local v10    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .restart local v13    # "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .restart local v14    # "$dirty":I
    .restart local v15    # "onAboutLithic":Lkotlin/jvm/functions/Function0;
    :cond_a9d
    move-object/from16 p2, v0

    move-object v0, v8

    move-object v1, v9

    move-object/from16 v32, v10

    move-object v2, v13

    move/from16 v31, v14

    move-object v8, v5

    move-object v5, v15

    .end local v9    # "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .end local v10    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .end local v13    # "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .end local v14    # "$dirty":I
    .end local v15    # "onAboutLithic":Lkotlin/jvm/functions/Function0;
    .local v0, "onManageCategories":Lkotlin/jvm/functions/Function0;
    .restart local v1    # "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .restart local v2    # "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .local v5, "onAboutLithic":Lkotlin/jvm/functions/Function0;
    .local v8, "$composer":Landroidx/compose/runtime/Composer;
    .restart local v31    # "$dirty":I
    .restart local v32    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .restart local p2    # "backupState":Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    const v6, -0x7731e22

    invoke-interface {v8, v6}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    invoke-interface {v8}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    :goto_ab1
    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v6

    if-eqz v6, :cond_aba

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->traceEventEnd()V

    .line 157
    .end local v4    # "informationOpen$delegate":Landroidx/compose/runtime/MutableState;
    :cond_aba
    move-object v11, v0

    move-object/from16 v18, v1

    move-object v13, v2

    move-object v4, v3

    move-object v14, v5

    move-object v0, v8

    move-object/from16 v12, v28

    move-object/from16 v2, v30

    move-object/from16 v19, v32

    move-object/from16 v15, v43

    move-object/from16 v6, v45

    move-object/from16 v5, v46

    move-object/from16 v16, v47

    move-object/from16 v17, v49

    move-object/from16 v7, v50

    move-object/from16 v8, v51

    move-object/from16 v9, v52

    move-object/from16 v10, v53

    move-object/from16 v3, p2

    goto :goto_b03

    .line 34
    .end local v0    # "onManageCategories":Lkotlin/jvm/functions/Function0;
    .end local v1    # "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .end local v2    # "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .end local v3    # "onExportBackup":Lkotlin/jvm/functions/Function0;
    .end local v8    # "$composer":Landroidx/compose/runtime/Composer;
    .end local v28    # "onNotificationClick":Lkotlin/jvm/functions/Function0;
    .end local v29    # "$dirty1":I
    .end local v30    # "modifier":Landroidx/compose/ui/Modifier;
    .end local v31    # "$dirty":I
    .end local v32    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .end local v43    # "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .end local v45    # "restoreState":Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;
    .end local v46    # "onCancelBackup":Lkotlin/jvm/functions/Function0;
    .end local v47    # "onInspectBackup":Lkotlin/jvm/functions/Function0;
    .end local v49    # "onCancelInspection":Lkotlin/jvm/functions/Function0;
    .end local v50    # "onRestoreBackup":Lkotlin/jvm/functions/Function0;
    .end local v51    # "onCancelRestore":Lkotlin/jvm/functions/Function0;
    .end local v52    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .end local v53    # "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    .local v5, "$composer":Landroidx/compose/runtime/Composer;
    .local v6, "$dirty":I
    .local v7, "$dirty1":I
    .local p1, "modifier":Landroidx/compose/ui/Modifier;
    .restart local p3    # "onExportBackup":Lkotlin/jvm/functions/Function0;
    .local p4, "onCancelBackup":Lkotlin/jvm/functions/Function0;
    .restart local p5    # "restoreState":Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;
    .local p6, "onRestoreBackup":Lkotlin/jvm/functions/Function0;
    .local p7, "onCancelRestore":Lkotlin/jvm/functions/Function0;
    .local p8, "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .local p9, "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    .local p10, "onManageCategories":Lkotlin/jvm/functions/Function0;
    .local p11, "onNotificationClick":Lkotlin/jvm/functions/Function0;
    .local p12, "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .local p13, "onAboutLithic":Lkotlin/jvm/functions/Function0;
    .local p14, "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .local p15, "onInspectBackup":Lkotlin/jvm/functions/Function0;
    .local p16, "onCancelInspection":Lkotlin/jvm/functions/Function0;
    .local p17, "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .local p18, "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    :cond_adc
    move-object v8, v5

    .end local v5    # "$composer":Landroidx/compose/runtime/Composer;
    .restart local v8    # "$composer":Landroidx/compose/runtime/Composer;
    invoke-interface {v8}, Landroidx/compose/runtime/Composer;->skipToGroupEnd()V

    move-object/from16 v15, p14

    move-object/from16 v16, p15

    move-object/from16 v17, p16

    move-object/from16 v18, p17

    move-object/from16 v19, p18

    move/from16 v31, v6

    move/from16 v29, v7

    move-object v0, v8

    move-object v4, v9

    move-object v5, v10

    move-object v6, v11

    move-object v7, v12

    move-object v2, v13

    move-object v3, v14

    move-object/from16 v8, p7

    move-object/from16 v9, p8

    move-object/from16 v10, p9

    move-object/from16 v11, p10

    move-object/from16 v12, p11

    move-object/from16 v13, p12

    move-object/from16 v14, p13

    .line 157
    .end local p1    # "modifier":Landroidx/compose/ui/Modifier;
    .end local p2    # "backupState":Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    .end local p3    # "onExportBackup":Lkotlin/jvm/functions/Function0;
    .end local p4    # "onCancelBackup":Lkotlin/jvm/functions/Function0;
    .end local p5    # "restoreState":Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;
    .end local p6    # "onRestoreBackup":Lkotlin/jvm/functions/Function0;
    .end local p7    # "onCancelRestore":Lkotlin/jvm/functions/Function0;
    .end local p8    # "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .end local p9    # "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    .end local p10    # "onManageCategories":Lkotlin/jvm/functions/Function0;
    .end local p11    # "onNotificationClick":Lkotlin/jvm/functions/Function0;
    .end local p12    # "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .end local p13    # "onAboutLithic":Lkotlin/jvm/functions/Function0;
    .end local p14    # "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .end local p15    # "onInspectBackup":Lkotlin/jvm/functions/Function0;
    .end local p16    # "onCancelInspection":Lkotlin/jvm/functions/Function0;
    .end local p17    # "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .end local p18    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .local v0, "$composer":Landroidx/compose/runtime/Composer;
    .local v2, "modifier":Landroidx/compose/ui/Modifier;
    .local v3, "backupState":Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    .local v4, "onExportBackup":Lkotlin/jvm/functions/Function0;
    .local v5, "onCancelBackup":Lkotlin/jvm/functions/Function0;
    .local v6, "restoreState":Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;
    .local v7, "onRestoreBackup":Lkotlin/jvm/functions/Function0;
    .local v8, "onCancelRestore":Lkotlin/jvm/functions/Function0;
    .local v9, "onConfirmRestore":Lkotlin/jvm/functions/Function0;
    .local v10, "onDiscardRestore":Lkotlin/jvm/functions/Function0;
    .local v11, "onManageCategories":Lkotlin/jvm/functions/Function0;
    .local v12, "onNotificationClick":Lkotlin/jvm/functions/Function0;
    .restart local v13    # "onCalculationExplanation":Lkotlin/jvm/functions/Function0;
    .local v14, "onAboutLithic":Lkotlin/jvm/functions/Function0;
    .local v15, "inspectionState":Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .local v16, "onInspectBackup":Lkotlin/jvm/functions/Function0;
    .local v17, "onCancelInspection":Lkotlin/jvm/functions/Function0;
    .local v18, "photoStorageState":Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;
    .restart local v19    # "onRefreshPhotoStorage":Lkotlin/jvm/functions/Function0;
    .restart local v29    # "$dirty1":I
    .restart local v31    # "$dirty":I
    :goto_b03
    invoke-interface {v0}, Landroidx/compose/runtime/Composer;->endRestartGroup()Landroidx/compose/runtime/ScopeUpdateScope;

    move-result-object v1

    if-eqz v1, :cond_b21

    move-object/from16 v24, v0

    .end local v0    # "$composer":Landroidx/compose/runtime/Composer;
    .restart local v24    # "$composer":Landroidx/compose/runtime/Composer;
    new-instance v0, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda16;

    move/from16 v20, p20

    move/from16 v21, p21

    move/from16 v22, p22

    move-object/from16 v82, v1

    move-object/from16 v1, p0

    invoke-direct/range {v0 .. v22}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda16;-><init>(Ljava/time/ZoneId;Landroidx/compose/ui/Modifier;Lcom/cooper/lithic/feature/settings/BackupExportUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;Lkotlin/jvm/functions/Function0;III)V

    move-object v1, v0

    move-object/from16 v0, v82

    invoke-interface {v0, v1}, Landroidx/compose/runtime/ScopeUpdateScope;->updateScope(Lkotlin/jvm/functions/Function2;)V

    goto :goto_b23

    .end local v24    # "$composer":Landroidx/compose/runtime/Composer;
    .restart local v0    # "$composer":Landroidx/compose/runtime/Composer;
    :cond_b21
    move-object/from16 v24, v0

    .end local v0    # "$composer":Landroidx/compose/runtime/Composer;
    .restart local v24    # "$composer":Landroidx/compose/runtime/Composer;
    :goto_b23
    return-void
.end method

.method private static final SettingsShell$lambda$0$0()Lkotlin/Unit;
    .registers 1

    .line 39
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$1$0()Lkotlin/Unit;
    .registers 1

    .line 42
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$10(Landroidx/compose/runtime/MutableState;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 17
    .param p0, "$informationOpen$delegate"    # Landroidx/compose/runtime/MutableState;
    .param p1, "$composer"    # Landroidx/compose/runtime/Composer;
    .param p2, "$changed"    # I

    move-object v10, p1

    move/from16 v13, p2

    const-string v0, "C155@8432L27,155@8421L103:SettingsShell.kt#e7edxl"

    invoke-static {p1, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    and-int/lit8 v0, v13, 0x3

    const/4 v1, 0x2

    if-eq v0, v1, :cond_f

    const/4 v0, 0x1

    goto :goto_10

    :cond_f
    const/4 v0, 0x0

    :goto_10
    and-int/lit8 v1, v13, 0x1

    invoke-interface {p1, v0, v1}, Landroidx/compose/runtime/Composer;->shouldExecute(ZI)Z

    move-result v0

    if-eqz v0, :cond_81

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v0

    if-eqz v0, :cond_27

    const/4 v0, -0x1

    const-string v1, "com.cooper.lithic.feature.settings.SettingsShell.<anonymous> (SettingsShell.kt:155)"

    const v2, -0x21853faf

    invoke-static {v2, v13, v0, v1}, Landroidx/compose/runtime/ComposerKt;->traceEventStart(IIILjava/lang/String;)V

    .line 156
    :cond_27
    const v0, 0x2acbf1cc

    const-string v1, "CC(remember):SettingsShell.kt#9igjgp"

    invoke-static {p1, v0, v1}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    invoke-interface {p1, p0}, Landroidx/compose/runtime/Composer;->changed(Ljava/lang/Object;)Z

    move-result v0

    .local v0, "invalid\\1":Z
    move-object v1, p1

    .local v1, "$this$cache\\1":Landroidx/compose/runtime/Composer;
    const/4 v2, 0x0

    .line 320
    .local v2, "$i$f$cache\\1\\156":I
    invoke-interface {v1}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v3

    .local v3, "it\\1":Ljava/lang/Object;
    const/4 v4, 0x0

    .line 321
    .local v4, "$i$a$-let-ComposerKt$cache$1\\2\\320\\1":I
    if-nez v0, :cond_46

    sget-object v5, Landroidx/compose/runtime/Composer;->Companion:Landroidx/compose/runtime/Composer$Companion;

    invoke-virtual {v5}, Landroidx/compose/runtime/Composer$Companion;->getEmpty()Ljava/lang/Object;

    move-result-object v5

    if-ne v3, v5, :cond_45

    goto :goto_46

    .line 325
    :cond_45
    goto :goto_51

    .line 322
    :cond_46
    :goto_46
    const/4 v5, 0x0

    .line 156
    .local v5, "$i$a$-cache-SettingsShellKt$SettingsShell$8$1\\3\\322\\0":I
    new-instance v6, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda10;

    invoke-direct {v6, p0}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda10;-><init>(Landroidx/compose/runtime/MutableState;)V

    .line 322
    .end local v5    # "$i$a$-cache-SettingsShellKt$SettingsShell$8$1\\3\\322\\0":I
    nop

    .line 323
    .local v6, "value\\2":Ljava/lang/Object;
    invoke-interface {v1, v6}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 324
    move-object v3, v6

    .line 320
    .end local v3    # "it\\1":Ljava/lang/Object;
    .end local v4    # "$i$a$-let-ComposerKt$cache$1\\2\\320\\1":I
    .end local v6    # "value\\2":Ljava/lang/Object;
    :goto_51
    nop

    .line 156
    .end local v0    # "invalid\\1":Z
    .end local v1    # "$this$cache\\1":Landroidx/compose/runtime/Composer;
    .end local v2    # "$i$f$cache\\1\\156":I
    move-object v0, v3

    check-cast v0, Lkotlin/jvm/functions/Function0;

    invoke-static {p1}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    sget-object v1, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v1, Landroidx/compose/ui/Modifier;

    const-string v2, "settings-information-close"

    invoke-static {v1, v2}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v1

    sget-object v2, Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;->INSTANCE:Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;

    invoke-virtual {v2}, Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;->getLambda$1630534009$app_debug()Lkotlin/jvm/functions/Function3;

    move-result-object v9

    const v11, 0x30000030

    const/16 v12, 0x1fc

    const/4 v2, 0x0

    const/4 v3, 0x0

    const/4 v4, 0x0

    const/4 v5, 0x0

    const/4 v6, 0x0

    const/4 v7, 0x0

    const/4 v8, 0x0

    invoke-static/range {v0 .. v12}, Lcom/cooper/lithic/core/design/LithicControlsKt;->LithicTextButton(Lkotlin/jvm/functions/Function0;Landroidx/compose/ui/Modifier;ZLandroidx/compose/ui/graphics/Shape;Landroidx/compose/material3/ButtonColors;Landroidx/compose/material3/ButtonElevation;Landroidx/compose/foundation/BorderStroke;Landroidx/compose/foundation/layout/PaddingValues;Landroidx/compose/foundation/interaction/MutableInteractionSource;Lkotlin/jvm/functions/Function3;Landroidx/compose/runtime/Composer;II)V

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v0

    if-eqz v0, :cond_84

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->traceEventEnd()V

    goto :goto_84

    :cond_81
    invoke-interface {p1}, Landroidx/compose/runtime/Composer;->skipToGroupEnd()V

    :cond_84
    :goto_84
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$10$0$0(Landroidx/compose/runtime/MutableState;)Lkotlin/Unit;
    .registers 2
    .param p0, "$informationOpen$delegate"    # Landroidx/compose/runtime/MutableState;

    .line 156
    const/4 v0, 0x0

    invoke-static {p0, v0}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$7(Landroidx/compose/runtime/MutableState;Z)V

    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$11(Lcom/cooper/lithic/feature/settings/BackupExportUiState;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 54
    .param p0, "$backupState"    # Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    .param p1, "$composer"    # Landroidx/compose/runtime/Composer;
    .param p2, "$changed"    # I

    move-object/from16 v0, p1

    move/from16 v1, p2

    const-string v2, "C143@7652L21,143@7621L770:SettingsShell.kt#e7edxl"

    invoke-static {v0, v2}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    and-int/lit8 v2, v1, 0x3

    const/4 v3, 0x2

    const/4 v4, 0x1

    const/4 v5, 0x0

    if-eq v2, v3, :cond_12

    move v2, v4

    goto :goto_13

    :cond_12
    move v2, v5

    :goto_13
    and-int/lit8 v3, v1, 0x1

    invoke-interface {v0, v2, v3}, Landroidx/compose/runtime/Composer;->shouldExecute(ZI)Z

    move-result v2

    if-eqz v2, :cond_246

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v2

    if-eqz v2, :cond_2a

    const/4 v2, -0x1

    const-string v3, "com.cooper.lithic.feature.settings.SettingsShell.<anonymous> (SettingsShell.kt:143)"

    const v6, -0x65b4896a

    invoke-static {v6, v1, v2, v3}, Landroidx/compose/runtime/ComposerKt;->traceEventStart(IIILjava/lang/String;)V

    .line 144
    :cond_2a
    sget-object v2, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    move-object v6, v2

    check-cast v6, Landroidx/compose/ui/Modifier;

    invoke-static {v5, v0, v5, v4}, Landroidx/compose/foundation/ScrollKt;->rememberScrollState(ILandroidx/compose/runtime/Composer;II)Landroidx/compose/foundation/ScrollState;

    move-result-object v7

    const/16 v11, 0xe

    const/4 v12, 0x0

    const/4 v8, 0x0

    const/4 v9, 0x0

    const/4 v10, 0x0

    invoke-static/range {v6 .. v12}, Landroidx/compose/foundation/ScrollKt;->verticalScroll$default(Landroidx/compose/ui/Modifier;Landroidx/compose/foundation/ScrollState;ZLandroidx/compose/foundation/gestures/FlingBehavior;ZILjava/lang/Object;)Landroidx/compose/ui/Modifier;

    move-result-object v2

    const-string v3, "settings-information-content"

    invoke-static {v2, v3}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v2

    .line 145
    sget-object v3, Landroidx/compose/foundation/layout/Arrangement;->INSTANCE:Landroidx/compose/foundation/layout/Arrangement;

    const/16 v4, 0xc

    .local v4, "$this$dp\\1":I
    const/4 v6, 0x0

    .line 326
    .local v6, "$i$f$getDp\\1\\145":I
    int-to-float v7, v4

    invoke-static {v7}, Landroidx/compose/ui/unit/Dp;->constructor-impl(F)F

    move-result v4

    .line 145
    .end local v4    # "$this$dp\\1":I
    .end local v6    # "$i$f$getDp\\1\\145":I
    invoke-virtual {v3, v4}, Landroidx/compose/foundation/layout/Arrangement;->spacedBy-0680j_4(F)Landroidx/compose/foundation/layout/Arrangement$HorizontalOrVertical;

    move-result-object v3

    check-cast v3, Landroidx/compose/foundation/layout/Arrangement$Vertical;

    .line 144
    move-object/from16 v4, p1

    .local v2, "modifier\\2":Landroidx/compose/ui/Modifier;
    .local v3, "verticalArrangement\\2":Landroidx/compose/foundation/layout/Arrangement$Vertical;
    .local v4, "$composer\\2":Landroidx/compose/runtime/Composer;
    const/16 v6, 0x30

    .local v6, "$changed\\2":I
    const/4 v7, 0x0

    .line 327
    .local v7, "$i$f$Column\\2\\144":I
    const v8, 0x4ff7456f

    const-string v9, "CC(Column)N(modifier,verticalArrangement,horizontalAlignment,content)87@4443L61,88@4509L134:Column.kt#2w3rfo"

    invoke-static {v4, v8, v9}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    .line 328
    sget-object v8, Landroidx/compose/ui/Alignment;->Companion:Landroidx/compose/ui/Alignment$Companion;

    invoke-virtual {v8}, Landroidx/compose/ui/Alignment$Companion;->getStart()Landroidx/compose/ui/Alignment$Horizontal;

    move-result-object v8

    .line 331
    .local v8, "horizontalAlignment\\2":Landroidx/compose/ui/Alignment$Horizontal;
    shr-int/lit8 v9, v6, 0x3

    and-int/lit8 v9, v9, 0xe

    shr-int/lit8 v10, v6, 0x3

    and-int/lit8 v10, v10, 0x70

    or-int/2addr v9, v10

    invoke-static {v3, v8, v4, v9}, Landroidx/compose/foundation/layout/ColumnKt;->columnMeasurePolicy(Landroidx/compose/foundation/layout/Arrangement$Vertical;Landroidx/compose/ui/Alignment$Horizontal;Landroidx/compose/runtime/Composer;I)Landroidx/compose/ui/layout/MeasurePolicy;

    move-result-object v9

    .line 335
    .local v9, "measurePolicy\\2":Landroidx/compose/ui/layout/MeasurePolicy;
    shl-int/lit8 v10, v6, 0x3

    and-int/lit8 v10, v10, 0x70

    .line 332
    nop

    .local v10, "$changed\\3":I
    move-object v11, v2

    .local v11, "modifier\\3":Landroidx/compose/ui/Modifier;
    const/4 v12, 0x0

    .line 336
    .local v12, "$i$f$Layout\\3\\332":I
    const v13, -0x451e1427

    const-string v14, "CC(Layout)P(!1,2)80@3267L27,83@3433L360:Layout.kt#80mrfh"

    invoke-static {v4, v13, v14}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    .line 337
    invoke-static {v4, v5}, Landroidx/compose/runtime/ComposablesKt;->getCurrentCompositeKeyHashCode(Landroidx/compose/runtime/Composer;I)J

    move-result-wide v13

    invoke-static {v13, v14}, Ljava/lang/Long;->hashCode(J)I

    move-result v5

    .line 338
    .local v5, "compositeKeyHash\\3":I
    invoke-interface {v4}, Landroidx/compose/runtime/Composer;->getCurrentCompositionLocalMap()Landroidx/compose/runtime/CompositionLocalMap;

    move-result-object v13

    .line 339
    .local v13, "localMap\\3":Landroidx/compose/runtime/CompositionLocalMap;
    invoke-static {v4, v11}, Landroidx/compose/ui/ComposedModifierKt;->materializeModifier(Landroidx/compose/runtime/Composer;Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;

    move-result-object v14

    .line 341
    .local v14, "materialized\\3":Landroidx/compose/ui/Modifier;
    sget-object v15, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    invoke-virtual {v15}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getConstructor()Lkotlin/jvm/functions/Function0;

    move-result-object v15

    shl-int/lit8 v0, v10, 0x6

    and-int/lit16 v0, v0, 0x380

    or-int/lit8 v0, v0, 0x6

    .line 340
    nop

    .local v0, "$changed\\4":I
    .local v15, "factory\\4":Lkotlin/jvm/functions/Function0;
    const/16 v16, 0x0

    .line 342
    .local v16, "$i$f$ReusableComposeNode\\4\\340":I
    move/from16 v17, v0

    .end local v0    # "$changed\\4":I
    .local v17, "$changed\\4":I
    const v0, -0x20f7d59c

    const-string v1, "CC(ReusableComposeNode)N(factory,update,content)399@15590L9:Composables.kt#9igjgp"

    invoke-static {v4, v0, v1}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    .line 343
    invoke-interface {v4}, Landroidx/compose/runtime/Composer;->getApplier()Landroidx/compose/runtime/Applier;

    move-result-object v0

    instance-of v0, v0, Landroidx/compose/runtime/Applier;

    if-nez v0, :cond_b6

    invoke-static {}, Landroidx/compose/runtime/ComposablesKt;->invalidApplier()V

    .line 344
    :cond_b6
    invoke-interface {v4}, Landroidx/compose/runtime/Composer;->startReusableNode()V

    .line 345
    invoke-interface {v4}, Landroidx/compose/runtime/Composer;->getInserting()Z

    move-result v0

    if-eqz v0, :cond_c3

    .line 346
    invoke-interface {v4, v15}, Landroidx/compose/runtime/Composer;->createNode(Lkotlin/jvm/functions/Function0;)V

    goto :goto_c6

    .line 348
    :cond_c3
    invoke-interface {v4}, Landroidx/compose/runtime/Composer;->useNode()V

    .line 350
    :goto_c6
    invoke-static {v4}, Landroidx/compose/runtime/Updater;->constructor-impl(Landroidx/compose/runtime/Composer;)Landroidx/compose/runtime/Composer;

    move-result-object v0

    .local v0, "$this$Layout_u24lambda_u240\\5":Landroidx/compose/runtime/Composer;
    const/4 v1, 0x0

    .line 351
    .local v1, "$i$a$-ReusableComposeNode-LayoutKt$Layout$1\\5\\350\\3":I
    sget-object v18, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    move/from16 v19, v1

    .end local v1    # "$i$a$-ReusableComposeNode-LayoutKt$Layout$1\\5\\350\\3":I
    .local v19, "$i$a$-ReusableComposeNode-LayoutKt$Layout$1\\5\\350\\3":I
    invoke-virtual/range {v18 .. v18}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getSetMeasurePolicy()Lkotlin/jvm/functions/Function2;

    move-result-object v1

    invoke-static {v0, v9, v1}, Landroidx/compose/runtime/Updater;->set-impl(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V

    .line 352
    sget-object v1, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    invoke-virtual {v1}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getSetResolvedCompositionLocals()Lkotlin/jvm/functions/Function2;

    move-result-object v1

    invoke-static {v0, v13, v1}, Landroidx/compose/runtime/Updater;->set-impl(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V

    .line 353
    sget-object v1, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    invoke-virtual {v1}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getSetCompositeKeyHash()Lkotlin/jvm/functions/Function2;

    move-result-object v1

    .local v1, "block\\6":Lkotlin/jvm/functions/Function2;
    const/16 v18, 0x0

    .line 354
    .local v18, "$i$f$set-impl\\6\\353":I
    move-object/from16 v20, v0

    .local v20, "$this$set_impl_u24lambda_u240\\6":Landroidx/compose/runtime/Composer;
    const/16 v21, 0x0

    .line 355
    .local v21, "$i$a$-with-Updater$set$1\\7\\354\\6":I
    invoke-interface/range {v20 .. v20}, Landroidx/compose/runtime/Composer;->getInserting()Z

    move-result v22

    if-nez v22, :cond_107

    move-object/from16 v22, v2

    .end local v2    # "modifier\\2":Landroidx/compose/ui/Modifier;
    .local v22, "modifier\\2":Landroidx/compose/ui/Modifier;
    invoke-interface/range {v20 .. v20}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v2

    move-object/from16 v23, v3

    .end local v3    # "verticalArrangement\\2":Landroidx/compose/foundation/layout/Arrangement$Vertical;
    .local v23, "verticalArrangement\\2":Landroidx/compose/foundation/layout/Arrangement$Vertical;
    invoke-static {v5}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v3

    invoke-static {v2, v3}, Lkotlin/jvm/internal/Intrinsics;->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_104

    goto :goto_10b

    :cond_104
    move-object/from16 v3, v20

    goto :goto_11b

    .end local v22    # "modifier\\2":Landroidx/compose/ui/Modifier;
    .end local v23    # "verticalArrangement\\2":Landroidx/compose/foundation/layout/Arrangement$Vertical;
    .restart local v2    # "modifier\\2":Landroidx/compose/ui/Modifier;
    .restart local v3    # "verticalArrangement\\2":Landroidx/compose/foundation/layout/Arrangement$Vertical;
    :cond_107
    move-object/from16 v22, v2

    move-object/from16 v23, v3

    .line 356
    .end local v2    # "modifier\\2":Landroidx/compose/ui/Modifier;
    .end local v3    # "verticalArrangement\\2":Landroidx/compose/foundation/layout/Arrangement$Vertical;
    .restart local v22    # "modifier\\2":Landroidx/compose/ui/Modifier;
    .restart local v23    # "verticalArrangement\\2":Landroidx/compose/foundation/layout/Arrangement$Vertical;
    :goto_10b
    invoke-static {v5}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    move-object/from16 v3, v20

    .end local v20    # "$this$set_impl_u24lambda_u240\\6":Landroidx/compose/runtime/Composer;
    .local v3, "$this$set_impl_u24lambda_u240\\6":Landroidx/compose/runtime/Composer;
    invoke-interface {v3, v2}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 357
    invoke-static {v5}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-interface {v0, v2, v1}, Landroidx/compose/runtime/Composer;->apply(Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V

    .line 359
    :goto_11b
    nop

    .line 354
    .end local v3    # "$this$set_impl_u24lambda_u240\\6":Landroidx/compose/runtime/Composer;
    .end local v21    # "$i$a$-with-Updater$set$1\\7\\354\\6":I
    nop

    .line 359
    nop

    .line 360
    .end local v1    # "block\\6":Lkotlin/jvm/functions/Function2;
    .end local v18    # "$i$f$set-impl\\6\\353":I
    sget-object v1, Landroidx/compose/ui/node/ComposeUiNode;->Companion:Landroidx/compose/ui/node/ComposeUiNode$Companion;

    invoke-virtual {v1}, Landroidx/compose/ui/node/ComposeUiNode$Companion;->getSetModifier()Lkotlin/jvm/functions/Function2;

    move-result-object v1

    invoke-static {v0, v14, v1}, Landroidx/compose/runtime/Updater;->set-impl(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V

    .line 361
    nop

    .line 350
    .end local v0    # "$this$Layout_u24lambda_u240\\5":Landroidx/compose/runtime/Composer;
    .end local v19    # "$i$a$-ReusableComposeNode-LayoutKt$Layout$1\\5\\350\\3":I
    nop

    .line 362
    shr-int/lit8 v0, v17, 0x6

    and-int/lit8 v0, v0, 0xe

    .local v0, "$changed\\8":I
    move-object v1, v4

    .local v1, "$composer\\8":Landroidx/compose/runtime/Composer;
    const/4 v2, 0x0

    .line 333
    .local v2, "$i$a$-Layout-ColumnKt$Column$1\\8\\362\\2":I
    const v3, 0x7cc0ae6e

    move/from16 v18, v0

    .end local v0    # "$changed\\8":I
    .local v18, "$changed\\8":I
    const-string v0, "C89@4557L9:Column.kt#2w3rfo"

    invoke-static {v1, v3, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    sget-object v0, Landroidx/compose/foundation/layout/ColumnScopeInstance;->INSTANCE:Landroidx/compose/foundation/layout/ColumnScopeInstance;

    shr-int/lit8 v3, v6, 0x6

    and-int/lit8 v3, v3, 0x70

    or-int/lit8 v3, v3, 0x6

    .local v3, "$changed\\9":I
    check-cast v0, Landroidx/compose/foundation/layout/ColumnScope;

    .local v0, "$this$SettingsShell_u24lambda_u2411_u240\\9":Landroidx/compose/foundation/layout/ColumnScope;
    move-object/from16 v46, v1

    .local v46, "$composer\\9":Landroidx/compose/runtime/Composer;
    const/16 v19, 0x0

    .line 146
    .local v19, "$i$a$-Column-SettingsShellKt$SettingsShell$9$1\\9\\333\\0":I
    move-object/from16 v20, v0

    .end local v0    # "$this$SettingsShell_u24lambda_u2411_u240\\9":Landroidx/compose/foundation/layout/ColumnScope;
    .local v20, "$this$SettingsShell_u24lambda_u2411_u240\\9":Landroidx/compose/foundation/layout/ColumnScope;
    const v0, 0x51ce81f7

    move-object/from16 v21, v1

    .end local v1    # "$composer\\8":Landroidx/compose/runtime/Composer;
    .local v21, "$composer\\8":Landroidx/compose/runtime/Composer;
    const-string v1, "C145@7801L45,146@7863L46,147@7926L37,152@8258L57,153@8332L45:SettingsShell.kt#e7edxl"

    move/from16 v50, v2

    move-object/from16 v2, v46

    .end local v46    # "$composer\\9":Landroidx/compose/runtime/Composer;
    .local v2, "$composer\\9":Landroidx/compose/runtime/Composer;
    .local v50, "$i$a$-Layout-ColumnKt$Column$1\\8\\362\\2":I
    invoke-static {v2, v0, v1}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    const/16 v48, 0x0

    const v49, 0x3fffe

    const/16 v25, 0x0

    const-wide/16 v26, 0x0

    const/16 v28, 0x0

    const-wide/16 v29, 0x0

    const/16 v31, 0x0

    const/16 v32, 0x0

    const/16 v33, 0x0

    const-wide/16 v34, 0x0

    const/16 v36, 0x0

    const/16 v37, 0x0

    const-wide/16 v38, 0x0

    const/16 v40, 0x0

    const/16 v41, 0x0

    const/16 v42, 0x0

    const/16 v43, 0x0

    const/16 v44, 0x0

    const/16 v45, 0x0

    const/16 v47, 0x6

    const-string v24, "\u8bb0\u5f55\u4e3b\u8981\u4fdd\u5b58\u5728\u5f53\u524d\u8bbe\u5907\uff0c\u53ef\u79bb\u7ebf\u4f7f\u7528\u3002\u5378\u8f7d\u53ef\u80fd\u5bfc\u81f4\u672c\u5730\u8bb0\u5f55\u4e22\u5931\uff0c\u8bf7\u624b\u52a8\u5907\u4efd\u3002"

    .end local v2    # "$composer\\9":Landroidx/compose/runtime/Composer;
    .restart local v46    # "$composer\\9":Landroidx/compose/runtime/Composer;
    invoke-static/range {v24 .. v49}, Landroidx/compose/material3/TextKt;->Text-Nvy7gAk(Ljava/lang/String;Landroidx/compose/ui/Modifier;JLandroidx/compose/foundation/text/TextAutoSize;JLandroidx/compose/ui/text/font/FontStyle;Landroidx/compose/ui/text/font/FontWeight;Landroidx/compose/ui/text/font/FontFamily;JLandroidx/compose/ui/text/style/TextDecoration;Landroidx/compose/ui/text/style/TextAlign;JIZIILkotlin/jvm/functions/Function1;Landroidx/compose/ui/text/TextStyle;Landroidx/compose/runtime/Composer;III)V

    .line 147
    const-string v24, "\u5b8c\u6574\u5907\u4efd\u5305\u542b\u8bb0\u5f55\u3001\u8bbe\u7f6e\u548c\u5df2\u767b\u8bb0\u56fe\u7247\uff1b\u6587\u4ef6\u672a\u52a0\u5bc6\uff0c\u8bf7\u59a5\u5584\u4fdd\u7ba1\u3002\u6ca1\u6709\u81ea\u52a8\u4e91\u540c\u6b65\u3002"

    invoke-static/range {v24 .. v49}, Landroidx/compose/material3/TextKt;->Text-Nvy7gAk(Ljava/lang/String;Landroidx/compose/ui/Modifier;JLandroidx/compose/foundation/text/TextAutoSize;JLandroidx/compose/ui/text/font/FontStyle;Landroidx/compose/ui/text/font/FontWeight;Landroidx/compose/ui/text/font/FontFamily;JLandroidx/compose/ui/text/style/TextDecoration;Landroidx/compose/ui/text/style/TextAlign;JIZIILkotlin/jvm/functions/Function1;Landroidx/compose/ui/text/TextStyle;Landroidx/compose/runtime/Composer;III)V

    .line 148
    const-string v24, "\u5916\u89c2\u9009\u62e9\u4ec5\u5c5e\u4e8e\u672c\u673a\u663e\u793a\u504f\u597d\uff0c\u4e0d\u5305\u542b\u5728\u5b8c\u6574\u5907\u4efd\u7684\u4e1a\u52a1\u8bbe\u7f6e\u4e2d\u3002"

    invoke-static/range {v24 .. v49}, Landroidx/compose/material3/TextKt;->Text-Nvy7gAk(Ljava/lang/String;Landroidx/compose/ui/Modifier;JLandroidx/compose/foundation/text/TextAutoSize;JLandroidx/compose/ui/text/font/FontStyle;Landroidx/compose/ui/text/font/FontWeight;Landroidx/compose/ui/text/font/FontFamily;JLandroidx/compose/ui/text/style/TextDecoration;Landroidx/compose/ui/text/style/TextAlign;JIZIILkotlin/jvm/functions/Function1;Landroidx/compose/ui/text/TextStyle;Landroidx/compose/runtime/Composer;III)V

    .line 149
    .end local v46    # "$composer\\9":Landroidx/compose/runtime/Composer;
    .restart local v2    # "$composer\\9":Landroidx/compose/runtime/Composer;
    invoke-virtual/range {p0 .. p0}, Lcom/cooper/lithic/feature/settings/BackupExportUiState;->getPhase()Lcom/cooper/lithic/feature/settings/BackupUiPhase;

    move-result-object v0

    sget-object v1, Lcom/cooper/lithic/feature/settings/BackupUiPhase;->IDLE:Lcom/cooper/lithic/feature/settings/BackupUiPhase;

    if-ne v0, v1, :cond_1e7

    invoke-virtual/range {p0 .. p0}, Lcom/cooper/lithic/feature/settings/BackupExportUiState;->getFeedback()Lcom/cooper/lithic/feature/settings/BackupExportFeedback;

    move-result-object v0

    sget-object v1, Lcom/cooper/lithic/feature/settings/BackupExportFeedback;->NOT_STARTED:Lcom/cooper/lithic/feature/settings/BackupExportFeedback;

    if-ne v0, v1, :cond_1e7

    const v0, 0x51d29b9a

    invoke-interface {v2, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v0, "149@8107L116"

    invoke-static {v2, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    .line 150
    nop

    .line 151
    sget-object v0, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v0, Landroidx/compose/ui/Modifier;

    const-string v1, "backup-not-started"

    invoke-static {v0, v1}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v25

    .line 150
    const-string v24, "\u5c1a\u672a\u5f00\u59cb\u5bfc\u51fa\u3002\u9009\u62e9\u4fdd\u5b58\u4f4d\u7f6e\u540e\uff0c\u53ea\u6709\u5b8c\u6574\u5199\u5165\u5e76\u6821\u9a8c\u901a\u8fc7\u624d\u4f1a\u63d0\u793a\u6210\u529f\u3002"

    const-wide/16 v26, 0x0

    const/16 v28, 0x0

    const-wide/16 v29, 0x0

    const/16 v31, 0x0

    const/16 v32, 0x0

    const/16 v33, 0x0

    const-wide/16 v34, 0x0

    const/16 v36, 0x0

    const/16 v37, 0x0

    const-wide/16 v38, 0x0

    const/16 v40, 0x0

    const/16 v41, 0x0

    const/16 v42, 0x0

    const/16 v43, 0x0

    const/16 v44, 0x0

    const/16 v45, 0x0

    const/16 v47, 0x36

    const/16 v48, 0x0

    const v49, 0x3fffc

    move-object/from16 v46, v2

    .end local v2    # "$composer\\9":Landroidx/compose/runtime/Composer;
    .restart local v46    # "$composer\\9":Landroidx/compose/runtime/Composer;
    invoke-static/range {v24 .. v49}, Landroidx/compose/material3/TextKt;->Text-Nvy7gAk(Ljava/lang/String;Landroidx/compose/ui/Modifier;JLandroidx/compose/foundation/text/TextAutoSize;JLandroidx/compose/ui/text/font/FontStyle;Landroidx/compose/ui/text/font/FontWeight;Landroidx/compose/ui/text/font/FontFamily;JLandroidx/compose/ui/text/style/TextDecoration;Landroidx/compose/ui/text/style/TextAlign;JIZIILkotlin/jvm/functions/Function1;Landroidx/compose/ui/text/TextStyle;Landroidx/compose/runtime/Composer;III)V

    .line 149
    .end local v46    # "$composer\\9":Landroidx/compose/runtime/Composer;
    .restart local v2    # "$composer\\9":Landroidx/compose/runtime/Composer;
    invoke-interface {v2}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    goto :goto_1f0

    .line 152
    :cond_1e7
    const v0, 0x51d4e536

    invoke-interface {v2, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    invoke-interface {v2}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 153
    :goto_1f0
    const/16 v48, 0x0

    const v49, 0x3fffe

    const-string v24, "\u6062\u590d\u5148\u6821\u9a8c\u4e0e\u9884\u89c8\uff0c\u518d\u660e\u786e\u786e\u8ba4\u5b8c\u6574\u8986\u76d6\u5f53\u524d\u8bb0\u5f55\u3001\u56fe\u7247\u548c\u8bbe\u7f6e\uff0c\u4e0d\u5408\u5e76\u3002\u8bf7\u5148\u6838\u5bf9\u9884\u89c8\u4e2d\u7684\u5f53\u524d\u4e0e\u5907\u4efd\u5185\u5bb9\u3002"

    const/16 v25, 0x0

    const-wide/16 v26, 0x0

    const/16 v28, 0x0

    const-wide/16 v29, 0x0

    const/16 v31, 0x0

    const/16 v32, 0x0

    const/16 v33, 0x0

    const-wide/16 v34, 0x0

    const/16 v36, 0x0

    const/16 v37, 0x0

    const-wide/16 v38, 0x0

    const/16 v40, 0x0

    const/16 v41, 0x0

    const/16 v42, 0x0

    const/16 v43, 0x0

    const/16 v44, 0x0

    const/16 v45, 0x0

    const/16 v47, 0x6

    move-object/from16 v46, v2

    .end local v2    # "$composer\\9":Landroidx/compose/runtime/Composer;
    .restart local v46    # "$composer\\9":Landroidx/compose/runtime/Composer;
    invoke-static/range {v24 .. v49}, Landroidx/compose/material3/TextKt;->Text-Nvy7gAk(Ljava/lang/String;Landroidx/compose/ui/Modifier;JLandroidx/compose/foundation/text/TextAutoSize;JLandroidx/compose/ui/text/font/FontStyle;Landroidx/compose/ui/text/font/FontWeight;Landroidx/compose/ui/text/font/FontFamily;JLandroidx/compose/ui/text/style/TextDecoration;Landroidx/compose/ui/text/style/TextAlign;JIZIILkotlin/jvm/functions/Function1;Landroidx/compose/ui/text/TextStyle;Landroidx/compose/runtime/Composer;III)V

    .line 154
    const-string v24, "\u7eed\u8d39\u4e0e\u91cc\u7a0b\u7891\u63d0\u9192\u4e0d\u4fdd\u8bc1\u51c6\u70b9\u9001\u8fbe\u3002\u505c\u6b62App\u4e2d\u7684\u8ba2\u9605\u8bb0\u5f55\u4e0d\u4f1a\u53d6\u6d88\u670d\u52a1\u5546\u6263\u8d39\u3002"

    invoke-static/range {v24 .. v49}, Landroidx/compose/material3/TextKt;->Text-Nvy7gAk(Ljava/lang/String;Landroidx/compose/ui/Modifier;JLandroidx/compose/foundation/text/TextAutoSize;JLandroidx/compose/ui/text/font/FontStyle;Landroidx/compose/ui/text/font/FontWeight;Landroidx/compose/ui/text/font/FontFamily;JLandroidx/compose/ui/text/style/TextDecoration;Landroidx/compose/ui/text/style/TextAlign;JIZIILkotlin/jvm/functions/Function1;Landroidx/compose/ui/text/TextStyle;Landroidx/compose/runtime/Composer;III)V

    .line 146
    invoke-static/range {v46 .. v46}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 155
    nop

    .line 333
    .end local v3    # "$changed\\9":I
    .end local v19    # "$i$a$-Column-SettingsShellKt$SettingsShell$9$1\\9\\333\\0":I
    .end local v20    # "$this$SettingsShell_u24lambda_u2411_u240\\9":Landroidx/compose/foundation/layout/ColumnScope;
    .end local v46    # "$composer\\9":Landroidx/compose/runtime/Composer;
    invoke-static/range {v21 .. v21}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 362
    .end local v18    # "$changed\\8":I
    .end local v21    # "$composer\\8":Landroidx/compose/runtime/Composer;
    .end local v50    # "$i$a$-Layout-ColumnKt$Column$1\\8\\362\\2":I
    nop

    .line 363
    invoke-interface {v4}, Landroidx/compose/runtime/Composer;->endNode()V

    .line 342
    invoke-static {v4}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 364
    nop

    .line 336
    .end local v15    # "factory\\4":Lkotlin/jvm/functions/Function0;
    .end local v16    # "$i$f$ReusableComposeNode\\4\\340":I
    .end local v17    # "$changed\\4":I
    invoke-static {v4}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 365
    nop

    .line 327
    .end local v5    # "compositeKeyHash\\3":I
    .end local v10    # "$changed\\3":I
    .end local v11    # "modifier\\3":Landroidx/compose/ui/Modifier;
    .end local v12    # "$i$f$Layout\\3\\332":I
    .end local v13    # "localMap\\3":Landroidx/compose/runtime/CompositionLocalMap;
    .end local v14    # "materialized\\3":Landroidx/compose/ui/Modifier;
    invoke-static {v4}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 366
    nop

    .end local v4    # "$composer\\2":Landroidx/compose/runtime/Composer;
    .end local v6    # "$changed\\2":I
    .end local v7    # "$i$f$Column\\2\\144":I
    .end local v8    # "horizontalAlignment\\2":Landroidx/compose/ui/Alignment$Horizontal;
    .end local v9    # "measurePolicy\\2":Landroidx/compose/ui/layout/MeasurePolicy;
    .end local v22    # "modifier\\2":Landroidx/compose/ui/Modifier;
    .end local v23    # "verticalArrangement\\2":Landroidx/compose/foundation/layout/Arrangement$Vertical;
    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v0

    if-eqz v0, :cond_249

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->traceEventEnd()V

    goto :goto_249

    .line 143
    :cond_246
    invoke-interface/range {p1 .. p1}, Landroidx/compose/runtime/Composer;->skipToGroupEnd()V

    .line 156
    :cond_249
    :goto_249
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$12(Ljava/time/ZoneId;Landroidx/compose/ui/Modifier;Lcom/cooper/lithic/feature/settings/BackupExportUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;Lkotlin/jvm/functions/Function0;IIILandroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 48

    or-int/lit8 v0, p19, 0x1

    invoke-static {v0}, Landroidx/compose/runtime/RecomposeScopeImplKt;->updateChangedFlags(I)I

    move-result v21

    invoke-static/range {p20 .. p20}, Landroidx/compose/runtime/RecomposeScopeImplKt;->updateChangedFlags(I)I

    move-result v22

    move-object/from16 v1, p0

    move-object/from16 v2, p1

    move-object/from16 v3, p2

    move-object/from16 v4, p3

    move-object/from16 v5, p4

    move-object/from16 v6, p5

    move-object/from16 v7, p6

    move-object/from16 v8, p7

    move-object/from16 v9, p8

    move-object/from16 v10, p9

    move-object/from16 v11, p10

    move-object/from16 v12, p11

    move-object/from16 v13, p12

    move-object/from16 v14, p13

    move-object/from16 v15, p14

    move-object/from16 v16, p15

    move-object/from16 v17, p16

    move-object/from16 v18, p17

    move-object/from16 v19, p18

    move/from16 v23, p21

    move-object/from16 v20, p22

    invoke-static/range {v1 .. v23}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell(Ljava/time/ZoneId;Landroidx/compose/ui/Modifier;Lcom/cooper/lithic/feature/settings/BackupExportUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/PhotoStorageUiState;Lkotlin/jvm/functions/Function0;Landroidx/compose/runtime/Composer;III)V

    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$2$0()Lkotlin/Unit;
    .registers 1

    .line 43
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$3$0()Lkotlin/Unit;
    .registers 1

    .line 44
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$4$0()Lkotlin/Unit;
    .registers 1

    .line 51
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$5$0()Landroidx/compose/runtime/MutableState;
    .registers 3

    .line 55
    const/4 v0, 0x0

    invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v0

    const/4 v1, 0x0

    const/4 v2, 0x2

    invoke-static {v0, v1, v2, v1}, Landroidx/compose/runtime/SnapshotStateKt;->mutableStateOf$default(Ljava/lang/Object;Landroidx/compose/runtime/SnapshotMutationPolicy;ILjava/lang/Object;)Landroidx/compose/runtime/MutableState;

    move-result-object v0

    return-object v0
.end method

.method private static final SettingsShell$lambda$6(Landroidx/compose/runtime/MutableState;)Z
    .registers 5
    .param p0, "$informationOpen$delegate"    # Landroidx/compose/runtime/MutableState;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Landroidx/compose/runtime/MutableState<",
            "Ljava/lang/Boolean;",
            ">;)Z"
        }
    .end annotation

    .line 55
    move-object v0, p0

    check-cast v0, Landroidx/compose/runtime/State;

    .local v0, "$this$getValue\\1":Landroidx/compose/runtime/State;
    const/4 v1, 0x0

    .local v1, "this_\\1":Ljava/lang/Object;
    const/4 v2, 0x0

    .local v2, "property\\1":Lkotlin/reflect/KProperty;
    const/4 v3, 0x0

    .line 317
    .local v3, "$i$f$getValue\\1\\55":I
    invoke-interface {v0}, Landroidx/compose/runtime/State;->getValue()Ljava/lang/Object;

    move-result-object v0

    .end local v0    # "$this$getValue\\1":Landroidx/compose/runtime/State;
    .end local v1    # "this_\\1":Ljava/lang/Object;
    .end local v2    # "property\\1":Lkotlin/reflect/KProperty;
    .end local v3    # "$i$f$getValue\\1\\55":I
    check-cast v0, Ljava/lang/Boolean;

    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v0

    .line 55
    return v0
.end method

.method private static final SettingsShell$lambda$7(Landroidx/compose/runtime/MutableState;Z)V
    .registers 7
    .param p0, "$informationOpen$delegate"    # Landroidx/compose/runtime/MutableState;
    .param p1, "<set-?>"    # Z
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Landroidx/compose/runtime/MutableState<",
            "Ljava/lang/Boolean;",
            ">;Z)V"
        }
    .end annotation

    .line 55
    const/4 v0, 0x0

    .local v0, "this_\\1":Ljava/lang/Object;
    const/4 v1, 0x0

    .local v1, "property\\1":Lkotlin/reflect/KProperty;
    invoke-static {p1}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v2

    .local v2, "value\\1":Ljava/lang/Object;
    move-object v3, p0

    .local v3, "$this$setValue\\1":Landroidx/compose/runtime/MutableState;
    const/4 v4, 0x0

    .line 318
    .local v4, "$i$f$setValue\\1\\55":I
    invoke-interface {v3, v2}, Landroidx/compose/runtime/MutableState;->setValue(Ljava/lang/Object;)V

    .line 319
    nop

    .line 55
    .end local v0    # "this_\\1":Ljava/lang/Object;
    .end local v1    # "property\\1":Lkotlin/reflect/KProperty;
    .end local v2    # "value\\1":Ljava/lang/Object;
    .end local v3    # "$this$setValue\\1":Landroidx/compose/runtime/MutableState;
    .end local v4    # "$i$f$setValue\\1\\55":I
    return-void
.end method

.method private static final SettingsShell$lambda$8$0$0$0(Landroidx/compose/ui/semantics/SemanticsPropertyReceiver;)Lkotlin/Unit;
    .registers 2
    .param p0, "$this$semantics"    # Landroidx/compose/ui/semantics/SemanticsPropertyReceiver;

    const-string v0, "$this$semantics"

    invoke-static {p0, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    .line 60
    invoke-static {p0}, Landroidx/compose/ui/semantics/SemanticsPropertiesKt;->heading(Landroidx/compose/ui/semantics/SemanticsPropertyReceiver;)V

    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$8$0$1$0(Landroidx/compose/runtime/MutableState;)Lkotlin/Unit;
    .registers 2
    .param p0, "$informationOpen$delegate"    # Landroidx/compose/runtime/MutableState;

    .line 61
    const/4 v0, 0x1

    invoke-static {p0, v0}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$7(Landroidx/compose/runtime/MutableState;Z)V

    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$8$1(Lkotlin/jvm/functions/Function0;Ljava/time/ZoneId;Landroidx/compose/foundation/layout/ColumnScope;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 19
    .param p0, "$onManageCategories"    # Lkotlin/jvm/functions/Function0;
    .param p1, "$recordZone"    # Ljava/time/ZoneId;
    .param p2, "$this$C2Section"    # Landroidx/compose/foundation/layout/ColumnScope;
    .param p3, "$composer"    # Landroidx/compose/runtime/Composer;
    .param p4, "$changed"    # I

    move-object/from16 v8, p3

    move/from16 v11, p4

    const-string v0, "$this$C2Section"

    move-object/from16 v12, p2

    invoke-static {v12, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v0, "C70@3380L88:SettingsShell.kt#e7edxl"

    invoke-static {v8, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    and-int/lit8 v0, v11, 0x11

    const/16 v1, 0x10

    if-eq v0, v1, :cond_18

    const/4 v0, 0x1

    goto :goto_19

    :cond_18
    const/4 v0, 0x0

    :goto_19
    and-int/lit8 v1, v11, 0x1

    invoke-interface {v8, v0, v1}, Landroidx/compose/runtime/Composer;->shouldExecute(ZI)Z

    move-result v0

    if-eqz v0, :cond_93

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v0

    if-eqz v0, :cond_30

    const/4 v0, -0x1

    const-string v1, "com.cooper.lithic.feature.settings.SettingsShell.<anonymous>.<anonymous> (SettingsShell.kt:66)"

    const v2, 0x5e9d63de

    invoke-static {v2, v11, v0, v1}, Landroidx/compose/runtime/ComposerKt;->traceEventStart(IIILjava/lang/String;)V

    .line 67
    :cond_30
    if-nez p0, :cond_3c

    const v0, 0x6da9414c

    invoke-interface {v8, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    .line 70
    invoke-interface {v8}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 67
    goto :goto_71

    :cond_3c
    const v0, 0x6da9414d

    invoke-interface {v8, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v0, "*67@3216L101,68@3334L19"

    invoke-static {v8, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    move-object v6, p0

    .local v6, "manage\\1":Lkotlin/jvm/functions/Function0;
    const/4 v13, 0x0

    .line 68
    .local v13, "$i$a$-let-SettingsShellKt$SettingsShell$6$2$1\\1\\67\\0":I
    sget-object v1, Lcom/cooper/lithic/core/design/C2Glyph;->Project:Lcom/cooper/lithic/core/design/C2Glyph;

    sget-object v0, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v0, Landroidx/compose/ui/Modifier;

    const-string v2, "open-category-management"

    invoke-static {v0, v2}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v2

    const/16 v9, 0x1b6

    const/16 v10, 0xb8

    const-string v0, "\u7ba1\u7406\u7c7b\u522b"

    const/4 v3, 0x0

    const/4 v4, 0x0

    const/4 v5, 0x0

    const/4 v7, 0x0

    invoke-static/range {v0 .. v10}, Lcom/cooper/lithic/core/design/C2PageKt;->C2SettingRow(Ljava/lang/String;Lcom/cooper/lithic/core/design/C2Glyph;Landroidx/compose/ui/Modifier;Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;Landroidx/compose/runtime/Composer;II)V

    .line 69
    move-object v7, v6

    .end local v6    # "manage\\1":Lkotlin/jvm/functions/Function0;
    .local v7, "manage\\1":Lkotlin/jvm/functions/Function0;
    const/4 v0, 0x0

    const/4 v1, 0x0

    const-wide/16 v2, 0x0

    const/4 v6, 0x7

    move-object/from16 v4, p3

    invoke-static/range {v0 .. v6}, Landroidx/compose/material3/DividerKt;->HorizontalDivider-9IZ8Weo(Landroidx/compose/ui/Modifier;FJLandroidx/compose/runtime/Composer;II)V

    .line 70
    nop

    .line 67
    .end local v7    # "manage\\1":Lkotlin/jvm/functions/Function0;
    .end local v13    # "$i$a$-let-SettingsShellKt$SettingsShell$6$2$1\\1\\67\\0":I
    invoke-interface/range {p3 .. p3}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 71
    :goto_71
    sget-object v1, Lcom/cooper/lithic/core/design/C2Glyph;->Now:Lcom/cooper/lithic/core/design/C2Glyph;

    invoke-virtual {p1}, Ljava/time/ZoneId;->getId()Ljava/lang/String;

    move-result-object v4

    const v9, 0xc00036

    const/16 v10, 0x6c

    const-string v0, "\u8bb0\u5f55\u65f6\u533a"

    const/4 v2, 0x0

    const/4 v3, 0x0

    const/4 v5, 0x0

    const/4 v6, 0x0

    const-string v7, "record-zone"

    move-object/from16 v8, p3

    invoke-static/range {v0 .. v10}, Lcom/cooper/lithic/core/design/C2PageKt;->C2SettingRow(Ljava/lang/String;Lcom/cooper/lithic/core/design/C2Glyph;Landroidx/compose/ui/Modifier;Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;Landroidx/compose/runtime/Composer;II)V

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v0

    if-eqz v0, :cond_96

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->traceEventEnd()V

    goto :goto_96

    .line 66
    :cond_93
    invoke-interface/range {p3 .. p3}, Landroidx/compose/runtime/Composer;->skipToGroupEnd()V

    .line 72
    :cond_96
    :goto_96
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$8$2$0(Lkotlin/jvm/functions/Function0;Landroidx/compose/foundation/layout/ColumnScope;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 16
    .param p0, "$open"    # Lkotlin/jvm/functions/Function0;
    .param p1, "$this$C2Section"    # Landroidx/compose/foundation/layout/ColumnScope;
    .param p2, "$composer"    # Landroidx/compose/runtime/Composer;
    .param p3, "$changed"    # I

    move v11, p3

    const-string v0, "$this$C2Section"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v0, "C74@3568L98:SettingsShell.kt#e7edxl"

    invoke-static {p2, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    and-int/lit8 v0, v11, 0x11

    const/16 v1, 0x10

    if-eq v0, v1, :cond_13

    const/4 v0, 0x1

    goto :goto_14

    :cond_13
    const/4 v0, 0x0

    :goto_14
    and-int/lit8 v1, v11, 0x1

    invoke-interface {p2, v0, v1}, Landroidx/compose/runtime/Composer;->shouldExecute(ZI)Z

    move-result v0

    if-eqz v0, :cond_50

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v0

    if-eqz v0, :cond_2b

    const/4 v0, -0x1

    const-string v1, "com.cooper.lithic.feature.settings.SettingsShell.<anonymous>.<anonymous>.<anonymous> (SettingsShell.kt:74)"

    const v2, -0x236ff99e

    invoke-static {v2, p3, v0, v1}, Landroidx/compose/runtime/ComposerKt;->traceEventStart(IIILjava/lang/String;)V

    .line 75
    :cond_2b
    sget-object v1, Lcom/cooper/lithic/core/design/C2Glyph;->Info:Lcom/cooper/lithic/core/design/C2Glyph;

    sget-object v0, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v0, Landroidx/compose/ui/Modifier;

    const-string v2, "open-notification-settings"

    invoke-static {v0, v2}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v2

    const/16 v9, 0x1b6

    const/16 v10, 0xb8

    const-string v0, "\u63d0\u9192\u8bbe\u7f6e"

    const/4 v3, 0x0

    const/4 v4, 0x0

    const/4 v5, 0x0

    const/4 v7, 0x0

    move-object v6, p0

    move-object v8, p2

    invoke-static/range {v0 .. v10}, Lcom/cooper/lithic/core/design/C2PageKt;->C2SettingRow(Ljava/lang/String;Lcom/cooper/lithic/core/design/C2Glyph;Landroidx/compose/ui/Modifier;Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;Landroidx/compose/runtime/Composer;II)V

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v0

    if-eqz v0, :cond_53

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->traceEventEnd()V

    goto :goto_53

    .line 74
    :cond_50
    invoke-interface {p2}, Landroidx/compose/runtime/Composer;->skipToGroupEnd()V

    .line 76
    :cond_53
    :goto_53
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$8$3(Lcom/cooper/lithic/feature/settings/BackupExportUiState;Lkotlin/jvm/functions/Function0;Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Landroidx/compose/foundation/layout/ColumnScope;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 44
    .param p0, "$backupState"    # Lcom/cooper/lithic/feature/settings/BackupExportUiState;
    .param p1, "$onExportBackup"    # Lkotlin/jvm/functions/Function0;
    .param p2, "$inspectionState"    # Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;
    .param p3, "$restoreState"    # Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;
    .param p4, "$onCancelBackup"    # Lkotlin/jvm/functions/Function0;
    .param p5, "$onInspectBackup"    # Lkotlin/jvm/functions/Function0;
    .param p6, "$onCancelInspection"    # Lkotlin/jvm/functions/Function0;
    .param p7, "$onRestoreBackup"    # Lkotlin/jvm/functions/Function0;
    .param p8, "$onCancelRestore"    # Lkotlin/jvm/functions/Function0;
    .param p9, "$onConfirmRestore"    # Lkotlin/jvm/functions/Function0;
    .param p10, "$onDiscardRestore"    # Lkotlin/jvm/functions/Function0;
    .param p11, "$this$C2Section"    # Landroidx/compose/foundation/layout/ColumnScope;
    .param p12, "$composer"    # Landroidx/compose/runtime/Composer;
    .param p13, "$changed"    # I

    move-object/from16 v0, p1

    move-object/from16 v1, p5

    move-object/from16 v2, p7

    move-object/from16 v7, p12

    move/from16 v14, p13

    const-string v3, "$this$C2Section"

    move-object/from16 v15, p11

    invoke-static {v15, v3}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v3, "C85@4232L28,78@3732L529,93@4674L19,100@5314L29,94@4706L638,101@5357L61,102@5431L19,113@6134L29,103@5463L701:SettingsShell.kt#e7edxl"

    invoke-static {v7, v3}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    and-int/lit8 v3, v14, 0x11

    const/16 v4, 0x10

    const/16 v16, 0x1

    if-eq v3, v4, :cond_21

    move/from16 v3, v16

    goto :goto_22

    :cond_21
    const/4 v3, 0x0

    :goto_22
    and-int/lit8 v4, v14, 0x1

    invoke-interface {v7, v3, v4}, Landroidx/compose/runtime/Composer;->shouldExecute(ZI)Z

    move-result v3

    if-eqz v3, :cond_3bf

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v3

    if-eqz v3, :cond_39

    const/4 v3, -0x1

    const-string v4, "com.cooper.lithic.feature.settings.SettingsShell.<anonymous>.<anonymous> (SettingsShell.kt:78)"

    const v6, -0x64d05243

    invoke-static {v6, v14, v3, v4}, Landroidx/compose/runtime/ComposerKt;->traceEventStart(IIILjava/lang/String;)V

    .line 79
    :cond_39
    invoke-virtual/range {p0 .. p0}, Lcom/cooper/lithic/feature/settings/BackupExportUiState;->getPhase()Lcom/cooper/lithic/feature/settings/BackupUiPhase;

    move-result-object v3

    sget-object v4, Lcom/cooper/lithic/feature/settings/SettingsShellKt$WhenMappings;->$EnumSwitchMapping$0:[I

    invoke-virtual {v3}, Lcom/cooper/lithic/feature/settings/BackupUiPhase;->ordinal()I

    move-result v3

    aget v3, v4, v3

    packed-switch v3, :pswitch_data_3c6

    new-instance v0, Lkotlin/NoWhenBranchMatchedException;

    invoke-direct {v0}, Lkotlin/NoWhenBranchMatchedException;-><init>()V

    throw v0

    .line 82
    :pswitch_4e
    const-string v3, "\u6b63\u5728\u5bfc\u51fa\u5e76\u6821\u9a8c"

    goto :goto_56

    .line 81
    :pswitch_51
    const-string v3, "\u6b63\u5728\u9009\u62e9\u4fdd\u5b58\u4f4d\u7f6e"

    goto :goto_56

    .line 80
    :pswitch_54
    const-string v3, "\u5bfc\u51fa\u5b8c\u6574\u5907\u4efd"

    .line 83
    :goto_56
    sget-object v4, Lcom/cooper/lithic/core/design/C2Glyph;->Project:Lcom/cooper/lithic/core/design/C2Glyph;

    sget-object v6, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v6, Landroidx/compose/ui/Modifier;

    const-string v8, "export-complete-backup"

    invoke-static {v6, v8}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v6

    .line 84
    nop

    .line 85
    if-eqz v0, :cond_7e

    invoke-virtual/range {p2 .. p2}, Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;->getBusy()Z

    move-result v8

    if-nez v8, :cond_7e

    invoke-virtual/range {p0 .. p0}, Lcom/cooper/lithic/feature/settings/BackupExportUiState;->getPhase()Lcom/cooper/lithic/feature/settings/BackupUiPhase;

    move-result-object v8

    sget-object v9, Lcom/cooper/lithic/feature/settings/BackupUiPhase;->IDLE:Lcom/cooper/lithic/feature/settings/BackupUiPhase;

    if-ne v8, v9, :cond_7e

    invoke-virtual/range {p3 .. p3}, Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;->getPhase()Lcom/cooper/lithic/feature/settings/RestoreUiPhase;

    move-result-object v8

    sget-object v9, Lcom/cooper/lithic/feature/settings/RestoreUiPhase;->IDLE:Lcom/cooper/lithic/feature/settings/RestoreUiPhase;

    if-ne v8, v9, :cond_7e

    move/from16 v8, v16

    goto :goto_7f

    :cond_7e
    const/4 v8, 0x0

    .line 86
    :goto_7f
    const v9, -0x72048d27

    const-string v10, "CC(remember):SettingsShell.kt#9igjgp"

    invoke-static {v7, v9, v10}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    invoke-interface {v7, v0}, Landroidx/compose/runtime/Composer;->changed(Ljava/lang/Object;)Z

    move-result v9

    .local v9, "invalid\\1":Z
    move-object/from16 v11, p12

    .local v11, "$this$cache\\1":Landroidx/compose/runtime/Composer;
    const/4 v12, 0x0

    .line 296
    .local v12, "$i$f$cache\\1\\86":I
    invoke-interface {v11}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v13

    .local v13, "it\\1":Ljava/lang/Object;
    const/16 v17, 0x0

    .line 297
    .local v17, "$i$a$-let-ComposerKt$cache$1\\2\\296\\1":I
    if-nez v9, :cond_a2

    sget-object v18, Landroidx/compose/runtime/Composer;->Companion:Landroidx/compose/runtime/Composer$Companion;

    invoke-virtual/range {v18 .. v18}, Landroidx/compose/runtime/Composer$Companion;->getEmpty()Ljava/lang/Object;

    move-result-object v5

    if-ne v13, v5, :cond_9f

    goto :goto_a2

    .line 301
    :cond_9f
    move-object/from16 v18, v3

    goto :goto_af

    .line 298
    :cond_a2
    :goto_a2
    const/4 v5, 0x0

    .line 86
    .local v5, "$i$a$-cache-SettingsShellKt$SettingsShell$6$4$1\\3\\298\\0":I
    move-object/from16 v18, v3

    new-instance v3, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda7;

    invoke-direct {v3, v0}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda7;-><init>(Lkotlin/jvm/functions/Function0;)V

    .line 298
    .end local v5    # "$i$a$-cache-SettingsShellKt$SettingsShell$6$4$1\\3\\298\\0":I
    nop

    .line 299
    .local v3, "value\\2":Ljava/lang/Object;
    invoke-interface {v11, v3}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 300
    move-object v13, v3

    .line 296
    .end local v3    # "value\\2":Ljava/lang/Object;
    .end local v13    # "it\\1":Ljava/lang/Object;
    .end local v17    # "$i$a$-let-ComposerKt$cache$1\\2\\296\\1":I
    :goto_af
    nop

    .line 86
    .end local v9    # "invalid\\1":Z
    .end local v11    # "$this$cache\\1":Landroidx/compose/runtime/Composer;
    .end local v12    # "$i$f$cache\\1\\86":I
    move-object v9, v13

    check-cast v9, Lkotlin/jvm/functions/Function0;

    invoke-static {v7}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 79
    move-object v5, v6

    const-string v6, "\u6587\u4ef6\u672a\u52a0\u5bc6\uff0c\u8bf7\u59a5\u5584\u4fdd\u7ba1"

    const/4 v7, 0x0

    move-object v3, v10

    const/4 v10, 0x0

    const/16 v12, 0xdb0

    const/16 v13, 0x90

    move-object/from16 v11, p12

    move-object/from16 v29, v3

    move-object/from16 v3, v18

    invoke-static/range {v3 .. v13}, Lcom/cooper/lithic/core/design/C2PageKt;->C2SettingRow(Ljava/lang/String;Lcom/cooper/lithic/core/design/C2Glyph;Landroidx/compose/ui/Modifier;Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;Landroidx/compose/runtime/Composer;II)V

    .line 87
    move-object v7, v11

    invoke-virtual/range {p0 .. p0}, Lcom/cooper/lithic/feature/settings/BackupExportUiState;->getPhase()Lcom/cooper/lithic/feature/settings/BackupUiPhase;

    move-result-object v3

    sget-object v4, Lcom/cooper/lithic/feature/settings/BackupUiPhase;->EXPORTING:Lcom/cooper/lithic/feature/settings/BackupUiPhase;

    const/4 v5, 0x0

    const/4 v6, 0x2

    const/4 v8, 0x0

    if-ne v3, v4, :cond_11d

    const v3, 0x31745f31

    invoke-interface {v7, v3}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v3, "87@4342L148"

    invoke-static {v7, v3}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    .line 88
    nop

    .line 89
    sget-object v3, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v3, Landroidx/compose/ui/Modifier;

    const/16 v4, 0x30

    .local v4, "$this$dp\\4":I
    const/4 v9, 0x0

    .line 302
    .local v9, "$i$f$getDp\\4\\89":I
    int-to-float v10, v4

    invoke-static {v10}, Landroidx/compose/ui/unit/Dp;->constructor-impl(F)F

    move-result v4

    .line 89
    .end local v4    # "$this$dp\\4":I
    .end local v9    # "$i$f$getDp\\4\\89":I
    invoke-static {v3, v4, v8, v6, v5}, Landroidx/compose/foundation/layout/SizeKt;->heightIn-VpY3zN4$default(Landroidx/compose/ui/Modifier;FFILjava/lang/Object;)Landroidx/compose/ui/Modifier;

    move-result-object v3

    const-string v4, "cancel-backup-export"

    invoke-static {v3, v4}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v4

    sget-object v3, Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;->INSTANCE:Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;

    invoke-virtual {v3}, Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;->getLambda$-295273856$app_debug()Lkotlin/jvm/functions/Function3;

    move-result-object v12

    .line 88
    move-object v3, v5

    const/4 v5, 0x0

    move v9, v6

    const/4 v6, 0x0

    const/4 v7, 0x0

    move v10, v8

    const/4 v8, 0x0

    move v11, v9

    const/4 v9, 0x0

    move v13, v10

    const/4 v10, 0x0

    move/from16 v17, v11

    const/4 v11, 0x0

    const v14, 0x30000030

    const/16 v15, 0x1fc

    move-object/from16 v13, p12

    move-object v0, v3

    move-object/from16 v3, p4

    invoke-static/range {v3 .. v15}, Lcom/cooper/lithic/core/design/LithicControlsKt;->LithicTextButton(Lkotlin/jvm/functions/Function0;Landroidx/compose/ui/Modifier;ZLandroidx/compose/ui/graphics/Shape;Landroidx/compose/material3/ButtonColors;Landroidx/compose/material3/ButtonElevation;Landroidx/compose/foundation/BorderStroke;Landroidx/compose/foundation/layout/PaddingValues;Landroidx/compose/foundation/interaction/MutableInteractionSource;Lkotlin/jvm/functions/Function3;Landroidx/compose/runtime/Composer;II)V

    .line 87
    move-object v7, v13

    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    goto :goto_127

    .line 90
    :cond_11d
    move-object v0, v5

    const v3, 0x317702e5

    invoke-interface {v7, v3}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 91
    :goto_127
    invoke-virtual/range {p0 .. p0}, Lcom/cooper/lithic/feature/settings/BackupExportUiState;->getMessage()Ljava/lang/String;

    move-result-object v3

    if-nez v3, :cond_137

    const v3, 0x31779589

    invoke-interface {v7, v3}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    .line 93
    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 91
    goto :goto_155

    :cond_137
    const v4, 0x3177958a

    invoke-interface {v7, v4}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v4, "*91@4572L75"

    invoke-static {v7, v4}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    .local v3, "feedback\\5":Ljava/lang/String;
    const/4 v10, 0x0

    .line 92
    .local v10, "$i$a$-let-SettingsShellKt$SettingsShell$6$4$2\\5\\91\\0":I
    invoke-virtual/range {p0 .. p0}, Lcom/cooper/lithic/feature/settings/BackupExportUiState;->getMessageTone()Lcom/cooper/lithic/core/design/UiMessageTone;

    move-result-object v4

    const/16 v8, 0x180

    const/16 v9, 0x8

    const-string v5, "backup-export-feedback"

    const/4 v6, 0x0

    invoke-static/range {v3 .. v9}, Lcom/cooper/lithic/core/design/UiFeedbackTextKt;->UiFeedbackText(Ljava/lang/String;Lcom/cooper/lithic/core/design/UiMessageTone;Ljava/lang/String;Landroidx/compose/ui/Modifier;Landroidx/compose/runtime/Composer;II)V

    .line 93
    nop

    .line 91
    .end local v3    # "feedback\\5":Ljava/lang/String;
    .end local v10    # "$i$a$-let-SettingsShellKt$SettingsShell$6$4$2\\5\\91\\0":I
    invoke-interface/range {p12 .. p12}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 94
    :goto_155
    const/4 v3, 0x0

    const/4 v4, 0x0

    const-wide/16 v5, 0x0

    const/4 v8, 0x0

    const/4 v9, 0x7

    move-object/from16 v7, p12

    invoke-static/range {v3 .. v9}, Landroidx/compose/material3/DividerKt;->HorizontalDivider-9IZ8Weo(Landroidx/compose/ui/Modifier;FJLandroidx/compose/runtime/Composer;II)V

    .line 95
    invoke-virtual/range {p2 .. p2}, Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;->getPhase()Lcom/cooper/lithic/feature/settings/BackupInspectionPhase;

    move-result-object v3

    sget-object v4, Lcom/cooper/lithic/feature/settings/SettingsShellKt$WhenMappings;->$EnumSwitchMapping$1:[I

    invoke-virtual {v3}, Lcom/cooper/lithic/feature/settings/BackupInspectionPhase;->ordinal()I

    move-result v3

    aget v3, v4, v3

    packed-switch v3, :pswitch_data_3d0

    new-instance v0, Lkotlin/NoWhenBranchMatchedException;

    invoke-direct {v0}, Lkotlin/NoWhenBranchMatchedException;-><init>()V

    throw v0

    .line 98
    :pswitch_175
    invoke-virtual/range {p2 .. p2}, Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;->getCancelling()Z

    move-result v3

    if-eqz v3, :cond_17e

    const-string v3, "\u6b63\u5728\u7ed3\u675f\u68c0\u67e5"

    goto :goto_18f

    :cond_17e
    const-string v3, "\u6b63\u5728\u68c0\u67e5\u5907\u4efd\u6587\u4ef6"

    goto :goto_18f

    .line 97
    :pswitch_181
    invoke-virtual/range {p2 .. p2}, Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;->getCancelling()Z

    move-result v3

    if-eqz v3, :cond_18a

    const-string v3, "\u7b49\u5f85\u4e0a\u6b21\u6587\u4ef6\u9009\u62e9\u7ed3\u675f"

    goto :goto_18f

    :cond_18a
    const-string v3, "\u6b63\u5728\u9009\u62e9\u5f85\u68c0\u67e5\u6587\u4ef6"

    goto :goto_18f

    .line 96
    :pswitch_18d
    const-string v3, "\u68c0\u67e5\u5907\u4efd\u6587\u4ef6"

    .line 99
    :goto_18f
    sget-object v4, Lcom/cooper/lithic/core/design/C2Glyph;->Info:Lcom/cooper/lithic/core/design/C2Glyph;

    sget-object v5, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v5, Landroidx/compose/ui/Modifier;

    const-string v6, "inspect-backup-file"

    invoke-static {v5, v6}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v5

    .line 100
    if-eqz v1, :cond_1b6

    invoke-virtual/range {p2 .. p2}, Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;->getBusy()Z

    move-result v6

    if-nez v6, :cond_1b6

    invoke-virtual/range {p0 .. p0}, Lcom/cooper/lithic/feature/settings/BackupExportUiState;->getPhase()Lcom/cooper/lithic/feature/settings/BackupUiPhase;

    move-result-object v6

    sget-object v8, Lcom/cooper/lithic/feature/settings/BackupUiPhase;->IDLE:Lcom/cooper/lithic/feature/settings/BackupUiPhase;

    if-ne v6, v8, :cond_1b6

    invoke-virtual/range {p3 .. p3}, Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;->getPhase()Lcom/cooper/lithic/feature/settings/RestoreUiPhase;

    move-result-object v6

    sget-object v8, Lcom/cooper/lithic/feature/settings/RestoreUiPhase;->IDLE:Lcom/cooper/lithic/feature/settings/RestoreUiPhase;

    if-ne v6, v8, :cond_1b6

    move/from16 v8, v16

    goto :goto_1b7

    :cond_1b6
    const/4 v8, 0x0

    .line 101
    :goto_1b7
    const v6, -0x720405e6

    move-object/from16 v14, v29

    invoke-static {v7, v6, v14}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    invoke-interface {v7, v1}, Landroidx/compose/runtime/Composer;->changed(Ljava/lang/Object;)Z

    move-result v6

    .local v6, "invalid\\6":Z
    move-object/from16 v9, p12

    .local v9, "$this$cache\\6":Landroidx/compose/runtime/Composer;
    const/4 v10, 0x0

    .line 303
    .local v10, "$i$f$cache\\6\\101":I
    invoke-interface {v9}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v11

    .local v11, "it\\6":Ljava/lang/Object;
    const/4 v12, 0x0

    .line 304
    .local v12, "$i$a$-let-ComposerKt$cache$1\\7\\303\\6":I
    if-nez v6, :cond_1d7

    sget-object v13, Landroidx/compose/runtime/Composer;->Companion:Landroidx/compose/runtime/Composer$Companion;

    invoke-virtual {v13}, Landroidx/compose/runtime/Composer$Companion;->getEmpty()Ljava/lang/Object;

    move-result-object v13

    if-ne v11, v13, :cond_1d6

    goto :goto_1d7

    .line 308
    :cond_1d6
    goto :goto_1e2

    .line 305
    :cond_1d7
    :goto_1d7
    const/4 v13, 0x0

    .line 101
    .local v13, "$i$a$-cache-SettingsShellKt$SettingsShell$6$4$3\\8\\305\\0":I
    new-instance v15, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda8;

    invoke-direct {v15, v1}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda8;-><init>(Lkotlin/jvm/functions/Function0;)V

    .line 305
    .end local v13    # "$i$a$-cache-SettingsShellKt$SettingsShell$6$4$3\\8\\305\\0":I
    nop

    .line 306
    .local v15, "value\\7":Ljava/lang/Object;
    invoke-interface {v9, v15}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 307
    move-object v11, v15

    .line 303
    .end local v11    # "it\\6":Ljava/lang/Object;
    .end local v12    # "$i$a$-let-ComposerKt$cache$1\\7\\303\\6":I
    .end local v15    # "value\\7":Ljava/lang/Object;
    :goto_1e2
    nop

    .line 101
    .end local v6    # "invalid\\6":Z
    .end local v9    # "$this$cache\\6":Landroidx/compose/runtime/Composer;
    .end local v10    # "$i$f$cache\\6\\101":I
    move-object v9, v11

    check-cast v9, Lkotlin/jvm/functions/Function0;

    invoke-static {v7}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 95
    const-string v6, "\u53ea\u68c0\u67e5\u5b8c\u6574\u6027\uff0c\u4e0d\u6062\u590d\u6216\u8986\u76d6\u8bb0\u5f55"

    const/4 v7, 0x0

    const/4 v10, 0x0

    const/16 v12, 0xdb0

    const/16 v13, 0x90

    move-object/from16 v11, p12

    invoke-static/range {v3 .. v13}, Lcom/cooper/lithic/core/design/C2PageKt;->C2SettingRow(Ljava/lang/String;Lcom/cooper/lithic/core/design/C2Glyph;Landroidx/compose/ui/Modifier;Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;Landroidx/compose/runtime/Composer;II)V

    .line 102
    move-object v7, v11

    move-object/from16 v15, p2

    move-object/from16 v10, p6

    const/4 v11, 0x0

    invoke-static {v15, v10, v7, v11}, Lcom/cooper/lithic/feature/settings/DataConfidenceSectionsKt;->BackupInspectionFeedback(Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;Lkotlin/jvm/functions/Function0;Landroidx/compose/runtime/Composer;I)V

    .line 103
    const/4 v3, 0x0

    const/4 v4, 0x0

    const-wide/16 v5, 0x0

    const/4 v8, 0x0

    const/4 v9, 0x7

    invoke-static/range {v3 .. v9}, Landroidx/compose/material3/DividerKt;->HorizontalDivider-9IZ8Weo(Landroidx/compose/ui/Modifier;FJLandroidx/compose/runtime/Composer;II)V

    .line 104
    invoke-virtual/range {p3 .. p3}, Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;->getPhase()Lcom/cooper/lithic/feature/settings/RestoreUiPhase;

    move-result-object v3

    sget-object v4, Lcom/cooper/lithic/feature/settings/SettingsShellKt$WhenMappings;->$EnumSwitchMapping$2:[I

    invoke-virtual {v3}, Lcom/cooper/lithic/feature/settings/RestoreUiPhase;->ordinal()I

    move-result v3

    aget v3, v4, v3

    packed-switch v3, :pswitch_data_3da

    new-instance v0, Lkotlin/NoWhenBranchMatchedException;

    invoke-direct {v0}, Lkotlin/NoWhenBranchMatchedException;-><init>()V

    throw v0

    .line 110
    :pswitch_21d
    const-string v3, "\u6b63\u5728\u5b89\u5168\u6062\u590d"

    goto :goto_22e

    .line 109
    :pswitch_220
    const-string v3, "\u6b63\u5728\u53d6\u6d88\u6062\u590d"

    goto :goto_22e

    .line 108
    :pswitch_223
    const-string v3, "\u8bf7\u6838\u5bf9\u4e0b\u65b9\u5907\u4efd\u6458\u8981"

    goto :goto_22e

    .line 107
    :pswitch_226
    const-string v3, "\u6b63\u5728\u8bfb\u53d6\u5e76\u6821\u9a8c\u5907\u4efd"

    goto :goto_22e

    .line 106
    :pswitch_229
    const-string v3, "\u6b63\u5728\u9009\u62e9\u5907\u4efd"

    goto :goto_22e

    .line 105
    :pswitch_22c
    const-string v3, "\u9009\u62e9\u5b8c\u6574\u5907\u4efd"

    .line 111
    :goto_22e
    sget-object v4, Lcom/cooper/lithic/core/design/C2Glyph;->Review:Lcom/cooper/lithic/core/design/C2Glyph;

    sget-object v5, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v5, Landroidx/compose/ui/Modifier;

    const-string v6, "restore-complete-backup"

    invoke-static {v5, v6}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v5

    .line 112
    nop

    .line 113
    if-eqz v2, :cond_256

    invoke-virtual {v15}, Lcom/cooper/lithic/feature/settings/BackupInspectionUiState;->getBusy()Z

    move-result v6

    if-nez v6, :cond_256

    invoke-virtual/range {p0 .. p0}, Lcom/cooper/lithic/feature/settings/BackupExportUiState;->getPhase()Lcom/cooper/lithic/feature/settings/BackupUiPhase;

    move-result-object v6

    sget-object v8, Lcom/cooper/lithic/feature/settings/BackupUiPhase;->IDLE:Lcom/cooper/lithic/feature/settings/BackupUiPhase;

    if-ne v6, v8, :cond_256

    invoke-virtual/range {p3 .. p3}, Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;->getPhase()Lcom/cooper/lithic/feature/settings/RestoreUiPhase;

    move-result-object v6

    sget-object v8, Lcom/cooper/lithic/feature/settings/RestoreUiPhase;->IDLE:Lcom/cooper/lithic/feature/settings/RestoreUiPhase;

    if-ne v6, v8, :cond_256

    move/from16 v8, v16

    goto :goto_257

    :cond_256
    move v8, v11

    .line 114
    :goto_257
    const v6, -0x72039f66

    invoke-static {v7, v6, v14}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerStart(Landroidx/compose/runtime/Composer;ILjava/lang/String;)V

    invoke-interface {v7, v2}, Landroidx/compose/runtime/Composer;->changed(Ljava/lang/Object;)Z

    move-result v6

    .local v6, "invalid\\9":Z
    move-object/from16 v9, p12

    .local v9, "$this$cache\\9":Landroidx/compose/runtime/Composer;
    const/4 v11, 0x0

    .line 309
    .local v11, "$i$f$cache\\9\\114":I
    invoke-interface {v9}, Landroidx/compose/runtime/Composer;->rememberedValue()Ljava/lang/Object;

    move-result-object v12

    .local v12, "it\\9":Ljava/lang/Object;
    const/4 v13, 0x0

    .line 310
    .local v13, "$i$a$-let-ComposerKt$cache$1\\10\\309\\9":I
    if-nez v6, :cond_275

    sget-object v14, Landroidx/compose/runtime/Composer;->Companion:Landroidx/compose/runtime/Composer$Companion;

    invoke-virtual {v14}, Landroidx/compose/runtime/Composer$Companion;->getEmpty()Ljava/lang/Object;

    move-result-object v14

    if-ne v12, v14, :cond_274

    goto :goto_275

    .line 314
    :cond_274
    goto :goto_280

    .line 311
    :cond_275
    :goto_275
    const/4 v14, 0x0

    .line 114
    .local v14, "$i$a$-cache-SettingsShellKt$SettingsShell$6$4$4\\11\\311\\0":I
    new-instance v0, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda9;

    invoke-direct {v0, v2}, Lcom/cooper/lithic/feature/settings/SettingsShellKt$$ExternalSyntheticLambda9;-><init>(Lkotlin/jvm/functions/Function0;)V

    .line 311
    .end local v14    # "$i$a$-cache-SettingsShellKt$SettingsShell$6$4$4\\11\\311\\0":I
    nop

    .line 312
    .local v0, "value\\10":Ljava/lang/Object;
    invoke-interface {v9, v0}, Landroidx/compose/runtime/Composer;->updateRememberedValue(Ljava/lang/Object;)V

    .line 313
    move-object v12, v0

    .line 309
    .end local v0    # "value\\10":Ljava/lang/Object;
    .end local v12    # "it\\9":Ljava/lang/Object;
    .end local v13    # "$i$a$-let-ComposerKt$cache$1\\10\\309\\9":I
    :goto_280
    nop

    .line 114
    .end local v6    # "invalid\\9":Z
    .end local v9    # "$this$cache\\9":Landroidx/compose/runtime/Composer;
    .end local v11    # "$i$f$cache\\9\\114":I
    move-object v9, v12

    check-cast v9, Lkotlin/jvm/functions/Function0;

    invoke-static {v7}, Landroidx/compose/runtime/ComposerKt;->sourceInformationMarkerEnd(Landroidx/compose/runtime/Composer;)V

    .line 104
    const-string v6, "\u5b8c\u6574\u8986\u76d6\uff0c\u4e0d\u5408\u5e76\uff1b\u5148\u9884\u89c8\u518d\u786e\u8ba4"

    const/4 v7, 0x0

    const/4 v10, 0x0

    const/16 v12, 0xdb0

    const/16 v13, 0x90

    move-object/from16 v11, p12

    invoke-static/range {v3 .. v13}, Lcom/cooper/lithic/core/design/C2PageKt;->C2SettingRow(Ljava/lang/String;Lcom/cooper/lithic/core/design/C2Glyph;Landroidx/compose/ui/Modifier;Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;Landroidx/compose/runtime/Composer;II)V

    .line 115
    move-object v7, v11

    invoke-virtual/range {p3 .. p3}, Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;->getPhase()Lcom/cooper/lithic/feature/settings/RestoreUiPhase;

    move-result-object v0

    sget-object v3, Lcom/cooper/lithic/feature/settings/RestoreUiPhase;->VALIDATING:Lcom/cooper/lithic/feature/settings/RestoreUiPhase;

    if-ne v0, v3, :cond_2e0

    const v0, 0x3191392f

    invoke-interface {v7, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v0, "115@6248L150"

    invoke-static {v7, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    .line 116
    nop

    .line 117
    sget-object v0, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v0, Landroidx/compose/ui/Modifier;

    const/16 v3, 0x30

    .local v3, "$this$dp\\12":I
    const/4 v4, 0x0

    .line 315
    .local v4, "$i$f$getDp\\12\\117":I
    int-to-float v5, v3

    invoke-static {v5}, Landroidx/compose/ui/unit/Dp;->constructor-impl(F)F

    move-result v3

    .line 117
    .end local v3    # "$this$dp\\12":I
    .end local v4    # "$i$f$getDp\\12\\117":I
    const/4 v4, 0x0

    const/4 v9, 0x2

    const/4 v10, 0x0

    invoke-static {v0, v3, v10, v9, v4}, Landroidx/compose/foundation/layout/SizeKt;->heightIn-VpY3zN4$default(Landroidx/compose/ui/Modifier;FFILjava/lang/Object;)Landroidx/compose/ui/Modifier;

    move-result-object v0

    const-string v3, "cancel-backup-restore"

    invoke-static {v0, v3}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v4

    sget-object v0, Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;->INSTANCE:Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;

    invoke-virtual {v0}, Lcom/cooper/lithic/feature/settings/ComposableSingletons$SettingsShellKt;->getLambda$2104214071$app_debug()Lkotlin/jvm/functions/Function3;

    move-result-object v12

    .line 116
    const/4 v5, 0x0

    const/4 v6, 0x0

    const/4 v7, 0x0

    const/4 v8, 0x0

    const/4 v9, 0x0

    const/4 v10, 0x0

    const/4 v11, 0x0

    const v14, 0x30000030

    const/16 v15, 0x1fc

    move-object/from16 v3, p8

    move-object/from16 v13, p12

    invoke-static/range {v3 .. v15}, Lcom/cooper/lithic/core/design/LithicControlsKt;->LithicTextButton(Lkotlin/jvm/functions/Function0;Landroidx/compose/ui/Modifier;ZLandroidx/compose/ui/graphics/Shape;Landroidx/compose/material3/ButtonColors;Landroidx/compose/material3/ButtonElevation;Landroidx/compose/foundation/BorderStroke;Landroidx/compose/foundation/layout/PaddingValues;Landroidx/compose/foundation/interaction/MutableInteractionSource;Lkotlin/jvm/functions/Function3;Landroidx/compose/runtime/Composer;II)V

    .line 115
    move-object v7, v13

    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    goto :goto_2e9

    .line 118
    :cond_2e0
    const v0, 0x3193e465

    invoke-interface {v7, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 119
    :goto_2e9
    invoke-virtual/range {p3 .. p3}, Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;->getPhase()Lcom/cooper/lithic/feature/settings/RestoreUiPhase;

    move-result-object v0

    sget-object v3, Lcom/cooper/lithic/feature/settings/RestoreUiPhase;->PREVIEW:Lcom/cooper/lithic/feature/settings/RestoreUiPhase;

    if-ne v0, v3, :cond_327

    .line 120
    const v0, -0x72036f9f

    invoke-interface {v7, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v0, ""

    invoke-static {v7, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    invoke-virtual/range {p3 .. p3}, Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;->getPreview()Lcom/cooper/lithic/data/backup/RestorePreview;

    move-result-object v3

    if-nez v3, :cond_309

    const v0, 0x31957bc0

    invoke-interface {v7, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    goto :goto_320

    :cond_309
    const v0, 0x31957bc1

    invoke-interface {v7, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v0, "*119@6521L60"

    invoke-static {v7, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    .line 316
    .local v3, "it\\13":Lcom/cooper/lithic/data/backup/RestorePreview;
    const/4 v0, 0x0

    .line 120
    .local v0, "$i$a$-let-SettingsShellKt$SettingsShell$6$4$5\\13\\120\\0":I
    const/4 v8, 0x0

    const/16 v9, 0x8

    const/4 v6, 0x0

    move-object/from16 v4, p9

    move-object/from16 v5, p10

    invoke-static/range {v3 .. v9}, Lcom/cooper/lithic/feature/settings/RestorePreviewScreenKt;->RestorePreviewScreen(Lcom/cooper/lithic/data/backup/RestorePreview;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;ZLandroidx/compose/runtime/Composer;II)V

    .end local v0    # "$i$a$-let-SettingsShellKt$SettingsShell$6$4$5\\13\\120\\0":I
    .end local v3    # "it\\13":Lcom/cooper/lithic/data/backup/RestorePreview;
    :goto_320
    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    goto :goto_330

    .line 121
    :cond_327
    const v0, 0x3196b145

    invoke-interface {v7, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 122
    :goto_330
    invoke-virtual/range {p3 .. p3}, Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;->getPhase()Lcom/cooper/lithic/feature/settings/RestoreUiPhase;

    move-result-object v0

    sget-object v3, Lcom/cooper/lithic/feature/settings/RestoreUiPhase;->RESTORING:Lcom/cooper/lithic/feature/settings/RestoreUiPhase;

    if-ne v0, v3, :cond_37e

    const v0, 0x3197bd9d

    invoke-interface {v7, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v0, "123@6762L10,122@6680L104"

    invoke-static {v7, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    .line 123
    nop

    .line 124
    sget-object v0, Landroidx/compose/material3/MaterialTheme;->INSTANCE:Landroidx/compose/material3/MaterialTheme;

    sget v3, Landroidx/compose/material3/MaterialTheme;->$stable:I

    invoke-virtual {v0, v7, v3}, Landroidx/compose/material3/MaterialTheme;->getTypography(Landroidx/compose/runtime/Composer;I)Landroidx/compose/material3/Typography;

    move-result-object v0

    invoke-virtual {v0}, Landroidx/compose/material3/Typography;->getBodyMedium()Landroidx/compose/ui/text/TextStyle;

    move-result-object v24

    .line 123
    const-string v3, "\u6b63\u5728\u5207\u6362\u5b8c\u6574\u672c\u5730\u8bb0\u5f55\uff0c\u8bf7\u7a0d\u5019\u3002\u5b8c\u6210\u524d\u4e0d\u4f1a\u5f00\u59cb\u53e6\u4e00\u9879\u5907\u4efd\u6216\u6062\u590d\u3002"

    const/4 v4, 0x0

    const-wide/16 v5, 0x0

    const/4 v7, 0x0

    const-wide/16 v8, 0x0

    const/4 v10, 0x0

    const/4 v11, 0x0

    const/4 v12, 0x0

    const-wide/16 v13, 0x0

    const/4 v15, 0x0

    const/16 v16, 0x0

    const-wide/16 v17, 0x0

    const/16 v19, 0x0

    const/16 v20, 0x0

    const/16 v21, 0x0

    const/16 v22, 0x0

    const/16 v23, 0x0

    const/16 v26, 0x6

    const/16 v27, 0x0

    const v28, 0x1fffe

    move-object/from16 v25, p12

    invoke-static/range {v3 .. v28}, Landroidx/compose/material3/TextKt;->Text-Nvy7gAk(Ljava/lang/String;Landroidx/compose/ui/Modifier;JLandroidx/compose/foundation/text/TextAutoSize;JLandroidx/compose/ui/text/font/FontStyle;Landroidx/compose/ui/text/font/FontWeight;Landroidx/compose/ui/text/font/FontFamily;JLandroidx/compose/ui/text/style/TextDecoration;Landroidx/compose/ui/text/style/TextAlign;JIZIILkotlin/jvm/functions/Function1;Landroidx/compose/ui/text/TextStyle;Landroidx/compose/runtime/Composer;III)V

    .line 122
    move-object/from16 v7, v25

    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    goto :goto_387

    .line 125
    :cond_37e
    const v0, 0x3199bc25

    invoke-interface {v7, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 126
    :goto_387
    invoke-virtual/range {p3 .. p3}, Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;->getMessage()Ljava/lang/String;

    move-result-object v3

    if-nez v3, :cond_397

    const v0, 0x319a52e7

    invoke-interface {v7, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    .line 128
    invoke-interface {v7}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 126
    goto :goto_3b5

    :cond_397
    const v0, 0x319a52e8

    invoke-interface {v7, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v0, "*126@6867L77"

    invoke-static {v7, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    .local v3, "feedback\\14":Ljava/lang/String;
    const/4 v0, 0x0

    .line 127
    .local v0, "$i$a$-let-SettingsShellKt$SettingsShell$6$4$6\\14\\126\\0":I
    invoke-virtual/range {p3 .. p3}, Lcom/cooper/lithic/feature/settings/BackupRestoreUiState;->getMessageTone()Lcom/cooper/lithic/core/design/UiMessageTone;

    move-result-object v4

    const/16 v8, 0x180

    const/16 v9, 0x8

    const-string v5, "backup-restore-feedback"

    const/4 v6, 0x0

    invoke-static/range {v3 .. v9}, Lcom/cooper/lithic/core/design/UiFeedbackTextKt;->UiFeedbackText(Ljava/lang/String;Lcom/cooper/lithic/core/design/UiMessageTone;Ljava/lang/String;Landroidx/compose/ui/Modifier;Landroidx/compose/runtime/Composer;II)V

    .line 128
    nop

    .line 126
    .end local v0    # "$i$a$-let-SettingsShellKt$SettingsShell$6$4$6\\14\\126\\0":I
    .end local v3    # "feedback\\14":Ljava/lang/String;
    invoke-interface/range {p12 .. p12}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    :goto_3b5
    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v0

    if-eqz v0, :cond_3c2

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->traceEventEnd()V

    goto :goto_3c2

    .line 78
    :cond_3bf
    invoke-interface/range {p12 .. p12}, Landroidx/compose/runtime/Composer;->skipToGroupEnd()V

    .line 129
    :cond_3c2
    :goto_3c2
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0

    nop

    :pswitch_data_3c6
    .packed-switch 0x1
        :pswitch_54
        :pswitch_51
        :pswitch_4e
    .end packed-switch

    :pswitch_data_3d0
    .packed-switch 0x1
        :pswitch_18d
        :pswitch_181
        :pswitch_175
    .end packed-switch

    :pswitch_data_3da
    .packed-switch 0x1
        :pswitch_22c
        :pswitch_229
        :pswitch_226
        :pswitch_223
        :pswitch_220
        :pswitch_21d
    .end packed-switch
.end method

.method private static final SettingsShell$lambda$8$3$0$0(Lkotlin/jvm/functions/Function0;)Lkotlin/Unit;
    .registers 2
    .param p0, "$onExportBackup"    # Lkotlin/jvm/functions/Function0;

    .line 86
    if-eqz p0, :cond_5

    invoke-interface {p0}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;

    :cond_5
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$8$3$2$0(Lkotlin/jvm/functions/Function0;)Lkotlin/Unit;
    .registers 2
    .param p0, "$onInspectBackup"    # Lkotlin/jvm/functions/Function0;

    .line 101
    if-eqz p0, :cond_5

    invoke-interface {p0}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;

    :cond_5
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$8$3$3$0(Lkotlin/jvm/functions/Function0;)Lkotlin/Unit;
    .registers 2
    .param p0, "$onRestoreBackup"    # Lkotlin/jvm/functions/Function0;

    .line 114
    if-eqz p0, :cond_5

    invoke-interface {p0}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;

    :cond_5
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$8$4(Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Landroidx/compose/foundation/layout/ColumnScope;Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;
    .registers 19
    .param p0, "$onCalculationExplanation"    # Lkotlin/jvm/functions/Function0;
    .param p1, "$onAboutLithic"    # Lkotlin/jvm/functions/Function0;
    .param p2, "$this$C2Section"    # Landroidx/compose/foundation/layout/ColumnScope;
    .param p3, "$composer"    # Landroidx/compose/runtime/Composer;
    .param p4, "$changed"    # I

    move-object/from16 v8, p3

    move/from16 v11, p4

    const-string v0, "$this$C2Section"

    move-object/from16 v12, p2

    invoke-static {v12, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v0, "C:SettingsShell.kt#e7edxl"

    invoke-static {v8, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    and-int/lit8 v0, v11, 0x11

    const/16 v1, 0x10

    if-eq v0, v1, :cond_18

    const/4 v0, 0x1

    goto :goto_19

    :cond_18
    const/4 v0, 0x0

    :goto_19
    and-int/lit8 v1, v11, 0x1

    invoke-interface {v8, v0, v1}, Landroidx/compose/runtime/Composer;->shouldExecute(ZI)Z

    move-result v0

    if-eqz v0, :cond_b2

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v0

    if-eqz v0, :cond_30

    const/4 v0, -0x1

    const-string v1, "com.cooper.lithic.feature.settings.SettingsShell.<anonymous>.<anonymous> (SettingsShell.kt:131)"

    const v2, -0x283e0864

    invoke-static {v2, v11, v0, v1}, Landroidx/compose/runtime/ComposerKt;->traceEventStart(IIILjava/lang/String;)V

    .line 132
    :cond_30
    if-nez p0, :cond_3c

    const v0, -0xad1c06d

    invoke-interface {v8, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    .line 134
    invoke-interface {v8}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 132
    goto :goto_66

    :cond_3c
    const v0, -0xad1c06c

    invoke-interface {v8, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v0, "*132@7136L102"

    invoke-static {v8, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    move-object v6, p0

    .local v6, "open\\1":Lkotlin/jvm/functions/Function0;
    const/4 v13, 0x0

    .line 133
    .local v13, "$i$a$-let-SettingsShellKt$SettingsShell$6$5$1\\1\\132\\0":I
    sget-object v1, Lcom/cooper/lithic/core/design/C2Glyph;->Info:Lcom/cooper/lithic/core/design/C2Glyph;

    sget-object v0, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v0, Landroidx/compose/ui/Modifier;

    const-string v2, "open-calculation-explanation"

    invoke-static {v0, v2}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v2

    const/16 v9, 0x1b6

    const/16 v10, 0xb8

    const-string v0, "\u6210\u672c\u8ba1\u7b97\u8bf4\u660e"

    const/4 v3, 0x0

    const/4 v4, 0x0

    const/4 v5, 0x0

    const/4 v7, 0x0

    invoke-static/range {v0 .. v10}, Lcom/cooper/lithic/core/design/C2PageKt;->C2SettingRow(Ljava/lang/String;Lcom/cooper/lithic/core/design/C2Glyph;Landroidx/compose/ui/Modifier;Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;Landroidx/compose/runtime/Composer;II)V

    .line 134
    nop

    .line 132
    .end local v6    # "open\\1":Lkotlin/jvm/functions/Function0;
    .end local v13    # "$i$a$-let-SettingsShellKt$SettingsShell$6$5$1\\1\\132\\0":I
    invoke-interface {v8}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 135
    :goto_66
    if-nez p1, :cond_72

    const v0, -0xacf1ae8

    invoke-interface {v8, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    .line 138
    invoke-interface {v8}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    .line 135
    goto :goto_a8

    :cond_72
    const v0, -0xacf1ae7

    invoke-interface {v8, v0}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V

    const-string v0, "*135@7310L19,136@7346L93"

    invoke-static {v8, v0}, Landroidx/compose/runtime/ComposerKt;->sourceInformation(Landroidx/compose/runtime/Composer;Ljava/lang/String;)V

    move-object v7, p1

    .local v7, "open\\2":Lkotlin/jvm/functions/Function0;
    const/4 v13, 0x0

    .line 136
    .local v13, "$i$a$-let-SettingsShellKt$SettingsShell$6$5$2\\2\\135\\0":I
    const/4 v0, 0x0

    const/4 v1, 0x0

    const-wide/16 v2, 0x0

    const/4 v5, 0x0

    const/4 v6, 0x7

    move-object v4, v8

    invoke-static/range {v0 .. v6}, Landroidx/compose/material3/DividerKt;->HorizontalDivider-9IZ8Weo(Landroidx/compose/ui/Modifier;FJLandroidx/compose/runtime/Composer;II)V

    .line 137
    sget-object v1, Lcom/cooper/lithic/core/design/C2Glyph;->Settings:Lcom/cooper/lithic/core/design/C2Glyph;

    sget-object v0, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;

    check-cast v0, Landroidx/compose/ui/Modifier;

    const-string v2, "open-about-lithic"

    invoke-static {v0, v2}, Landroidx/compose/ui/platform/TestTagKt;->testTag(Landroidx/compose/ui/Modifier;Ljava/lang/String;)Landroidx/compose/ui/Modifier;

    move-result-object v2

    const/16 v9, 0x1b6

    const/16 v10, 0xb8

    const-string v0, "\u5173\u4e8e\u783a\u77f3"

    const/4 v3, 0x0

    const/4 v4, 0x0

    move-object v6, v7

    .end local v7    # "open\\2":Lkotlin/jvm/functions/Function0;
    .local v6, "open\\2":Lkotlin/jvm/functions/Function0;
    const/4 v7, 0x0

    move-object/from16 v8, p3

    invoke-static/range {v0 .. v10}, Lcom/cooper/lithic/core/design/C2PageKt;->C2SettingRow(Ljava/lang/String;Lcom/cooper/lithic/core/design/C2Glyph;Landroidx/compose/ui/Modifier;Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Ljava/lang/String;Landroidx/compose/runtime/Composer;II)V

    .line 138
    nop

    .line 135
    .end local v6    # "open\\2":Lkotlin/jvm/functions/Function0;
    .end local v13    # "$i$a$-let-SettingsShellKt$SettingsShell$6$5$2\\2\\135\\0":I
    invoke-interface/range {p3 .. p3}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V

    :goto_a8
    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->isTraceInProgress()Z

    move-result v0

    if-eqz v0, :cond_b5

    invoke-static {}, Landroidx/compose/runtime/ComposerKt;->traceEventEnd()V

    goto :goto_b5

    .line 131
    :cond_b2
    invoke-interface/range {p3 .. p3}, Landroidx/compose/runtime/Composer;->skipToGroupEnd()V

    .line 139
    :cond_b5
    :goto_b5
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method private static final SettingsShell$lambda$9$0(Landroidx/compose/runtime/MutableState;)Lkotlin/Unit;
    .registers 2
    .param p0, "$informationOpen$delegate"    # Landroidx/compose/runtime/MutableState;

    .line 141
    const/4 v0, 0x0

    invoke-static {p0, v0}, Lcom/cooper/lithic/feature/settings/SettingsShellKt;->SettingsShell$lambda$7(Landroidx/compose/runtime/MutableState;Z)V

    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

