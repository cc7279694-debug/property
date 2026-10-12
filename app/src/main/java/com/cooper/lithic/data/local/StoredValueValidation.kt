package com.cooper.lithic.data.local

import java.util.UUID

/** Canonical identities avoid two string spellings referring to the same UUID. */
internal object StoredValueValidation {
    fun isUuid(value: String): Boolean = try {
        value.length == 36 && UUID.fromString(value).toString() == value
    } catch (_: IllegalArgumentException) {
        false
    }

    fun requireUuid(value: String) {
        require(isUuid(value)) { "Business identities must be canonical UUID strings" }
    }

    fun isMediaPath(value: String): Boolean {
        if (value.any { it == '\\' || it == ':' || it.code < 32 || it.code == 127 }) return false
        val segments = value.split('/')
        return segments.size >= 2 && segments.first() == "media" &&
            segments.none { it.isEmpty() || it == "." || it == ".." }
    }
}
