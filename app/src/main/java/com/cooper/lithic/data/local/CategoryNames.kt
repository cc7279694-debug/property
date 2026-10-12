package com.cooper.lithic.data.local

import java.text.Normalizer
import java.util.Locale

/** Persist a deterministic comparison name independent of the device locale. */
object CategoryNames {
    private val whitespace = Regex("[\\p{javaWhitespace}\\p{javaSpaceChar}]+")

    fun normalized(name: String): String = whitespace
        .replace(Normalizer.normalize(name, Normalizer.Form.NFKC), " ")
        .trim()
        .lowercase(Locale.ROOT)
}
