package com.cooper.lithic.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.data.repository.RankingRepository
import com.cooper.lithic.data.repository.ReviewRanking

internal class ReviewViewModel(repository: RankingRepository) : ViewModel() {
    private val controller = ReviewController(viewModelScope, repository.changes,
        { ranking, pages -> repository.loadWindow(ranking, pages) })
    val state = controller.state
    fun changeRanking(ranking: ReviewRanking) = controller.changeRanking(ranking)
    fun refresh() = controller.refresh()
    fun loadMore() = controller.loadMore()
}
