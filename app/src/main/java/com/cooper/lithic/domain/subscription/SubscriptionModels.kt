package com.cooper.lithic.domain.subscription

import com.cooper.lithic.core.model.Money
import java.math.BigDecimal
import java.time.LocalDate

enum class BillingCycle(val standardDays: Int) {
    MONTHLY(30), QUARTERLY(90), YEARLY(365),
}

data class Subscription(val id: String)

data class Renewal(
    val amount: Money,
    val cycle: BillingCycle,
    val paidOn: LocalDate,
    val effectiveFrom: LocalDate,
    val paidUntilExclusive: LocalDate,
    val reminderDueOn: LocalDate = paidUntilExclusive,
    val standardDays: Int = cycle.standardDays,
) {
    init {
        require(amount.cents > 0) { "A renewal must record a positive real payment" }
        require(effectiveFrom.isBefore(paidUntilExclusive)) { "Paid coverage must be nonempty" }
        require(standardDays == cycle.standardDays) { "Stored standard days must match the fixed cycle" }
    }
}

enum class SubscriptionOperationState { ENABLED, PAUSED, STOPPED }
enum class SubscriptionDisplayStatus { ACTIVE, PAUSED, EXPIRED, STOPPED }

data class SubscriptionStateChange(val effectiveOn: LocalDate, val state: SubscriptionOperationState)
data class SubscriptionSnapshot(
    val subscription: Subscription,
    val renewals: List<Renewal> = emptyList(),
    val stateChanges: List<SubscriptionStateChange> = emptyList(),
)

enum class SubscriptionCostUnavailableReason { NO_CONFIRMED_PAYMENTS }
data class SubscriptionCostResult(
    val displayStatus: SubscriptionDisplayStatus,
    /** Yuan per standard day, DECIMAL128 and unrounded for display; inactive contribution is zero. */
    val currentRate: BigDecimal,
    /** Only an ACTIVE period has an effective current price. */
    val currentPrice: Money?,
    val confirmedPaidCents: Long,
    val standardPurchasedDays: Long,
    val historicalAverage: BigDecimal?,
    val elapsedPaidDays: Long,
    val historicalUnavailableReason: SubscriptionCostUnavailableReason?,
)
