package com.cooper.lithic.core.model

/** A single absolute CNY amount; the event direction supplies its sign. */
data class Money(val cents: Long) {
    init {
        require(cents in 0..MAX_CENTS) { "A single amount must be between 0 and $MAX_CENTS cents" }
    }

    companion object {
        const val MAX_CENTS: Long = 999_999_999_999L
    }
}

enum class MoneyParseError { INVALID_FORMAT, EXCESS_PRECISION, OUT_OF_RANGE }

sealed interface MoneyParseResult {
    data class Success(val money: Money) : MoneyParseResult
    data class Failure(val reason: MoneyParseError) : MoneyParseResult
}

object MoneyParser {
    private val decimal = Regex("[0-9]+(?:\\.[0-9]+)?")

    /** Parse exact yuan without accepting a sign, exponent or rounded fractional cents. */
    fun parseCny(text: String): MoneyParseResult {
        val value = text.trim()
        if (!decimal.matches(value)) return MoneyParseResult.Failure(MoneyParseError.INVALID_FORMAT)

        val parts = value.split('.', limit = 2)
        val fraction = parts.getOrElse(1) { "" }
        if (fraction.length > 2) return MoneyParseResult.Failure(MoneyParseError.EXCESS_PRECISION)

        val whole = parts[0].trimStart('0').ifEmpty { "0" }
        if (whole.length > 10) return MoneyParseResult.Failure(MoneyParseError.OUT_OF_RANGE)

        // At most ten integer digits and two fractional digits fit safely in Long.
        val cents = whole.toLong() * 100 + fraction.padEnd(2, '0').toLong()
        if (cents > Money.MAX_CENTS) return MoneyParseResult.Failure(MoneyParseError.OUT_OF_RANGE)
        return MoneyParseResult.Success(Money(cents))
    }
}
