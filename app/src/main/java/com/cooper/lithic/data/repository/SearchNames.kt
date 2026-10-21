package com.cooper.lithic.data.repository

/** LIKE uses ! as its explicit escape; user input is always a bound value. */
object SearchNames {
    const val MAX_QUERY_LENGTH = 200
    fun query(input: String): String = input.trim().also { value ->
        require(value.length <= MAX_QUERY_LENGTH) { "Search names cannot exceed $MAX_QUERY_LENGTH characters" }
        require(value.none { it.code < 32 || it.code == 127 }) { "Search names cannot contain control characters" }
    }

    fun literalPattern(input: String): String = "%" + query(input)
        .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%"
}
