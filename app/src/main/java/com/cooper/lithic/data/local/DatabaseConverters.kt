package com.cooper.lithic.data.local

import androidx.room.TypeConverter
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.time.Instant
import java.time.LocalDate

class DatabaseConverters {
    @TypeConverter fun dateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun epochDayToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)
    @TypeConverter fun instantToText(value: Instant?): String? = value?.toString()
    @TypeConverter fun textToInstant(value: String?): Instant? = value?.let(Instant::parse)
    @TypeConverter fun categoryTypeToText(value: CategoryType): String = value.name
    @TypeConverter fun textToCategoryType(value: String): CategoryType = CategoryType.valueOf(value)
    @TypeConverter fun directionToText(value: MoneyDirection): String = value.name
    @TypeConverter fun textToDirection(value: String): MoneyDirection = MoneyDirection.valueOf(value)
    @TypeConverter fun roleToText(value: ProjectMoneyEventRole): String = value.name
    @TypeConverter fun textToRole(value: String): ProjectMoneyEventRole = ProjectMoneyEventRole.valueOf(value)
    @TypeConverter fun cycleToText(value: BillingCycle): String = value.name
    @TypeConverter fun textToCycle(value: String): BillingCycle = BillingCycle.valueOf(value)
    @TypeConverter fun subscriptionStateToText(value: SubscriptionOperationState): String = value.name
    @TypeConverter fun textToSubscriptionState(value: String): SubscriptionOperationState = SubscriptionOperationState.valueOf(value)
}
