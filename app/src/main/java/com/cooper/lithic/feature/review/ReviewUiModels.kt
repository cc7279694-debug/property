package com.cooper.lithic.feature.review

import com.cooper.lithic.data.repository.ReviewRankItem
import com.cooper.lithic.data.repository.ReviewRanking
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import java.time.LocalDate

internal data class ReviewUiState(
    val ranking: ReviewRanking = ReviewRanking.PROJECT_TIME,
    val items: List<ReviewRankItem> = emptyList(),
    val totalCount: Long? = null,
    val rankableCount: Long? = null,
    val asOf: LocalDate? = null,
    val loading: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
)

internal fun reviewSubscriptionStatus(status: SubscriptionDisplayStatus): String = when (status) {
    SubscriptionDisplayStatus.ACTIVE -> "订阅中"
    SubscriptionDisplayStatus.PAUSED -> "已暂停"
    SubscriptionDisplayStatus.EXPIRED -> "已到期"
    SubscriptionDisplayStatus.STOPPED -> "已停止"
}
