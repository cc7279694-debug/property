package com.cooper.lithic.app

/** Business initialization is permitted only in the exact default application process. */
enum class AppProcessRole { MAIN, SANDBOX, UNKNOWN }

/** Pure, fail-closed process identity policy. Runtime process reads remain in LithicApplication. */
object AppProcessRoles {
    const val MAX_PROCESS_NAME_BYTES = 256
    const val SANDBOX_SUFFIX = ":media_sandbox"

    fun resolve(applicationProcessName: String?, actualProcessName: String?): AppProcessRole {
        if (applicationProcessName == null || actualProcessName == null ||
            applicationProcessName.length !in 1 until MAX_PROCESS_NAME_BYTES ||
            actualProcessName.length !in 1 until MAX_PROCESS_NAME_BYTES ||
            applicationProcessName.any { !(it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' || it == '.') }) {
            return AppProcessRole.UNKNOWN
        }
        return when (actualProcessName) {
            applicationProcessName -> AppProcessRole.MAIN
            applicationProcessName + SANDBOX_SUFFIX -> AppProcessRole.SANDBOX
            else -> AppProcessRole.UNKNOWN
        }
    }

    fun decodeOwnProcessName(bytes: ByteArray): String? {
        if (bytes.size !in 1..MAX_PROCESS_NAME_BYTES) return null
        val terminator = bytes.indexOf(0.toByte())
        if (terminator <= 0 || (0 until terminator).any { bytes[it].toInt() !in 33..126 }) return null
        return bytes.copyOfRange(0, terminator).toString(Charsets.US_ASCII)
    }
}
