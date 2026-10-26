package com.cooper.lithic.feature.home

import java.time.LocalDate

internal enum class C2PreviewStatus { LOADING, ERROR, RETAINED, REFRESHING, EMPTY, READY }

/** Presentation only: no sorting, costs, counts or database queries are created here. */
internal fun c2PreviewStatus(loading: Boolean, error: String?, dates: List<LocalDate>, asOf: LocalDate?): C2PreviewStatus = when {
    error != null && dates.isEmpty() -> C2PreviewStatus.ERROR
    error != null -> C2PreviewStatus.RETAINED
    loading && dates.isEmpty() -> C2PreviewStatus.LOADING
    asOf == null || dates.any { it != asOf } -> C2PreviewStatus.REFRESHING
    dates.isEmpty() -> C2PreviewStatus.EMPTY
    else -> C2PreviewStatus.READY
}
