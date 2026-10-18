package com.cooper.lithic.data.media

/** Linux UAPI O_CLOEXEC (02000000), verified with the pinned NDK for all four API26 ABIs.
 * The kernel open flag predates API26; only the Java OsConstants field starts at API27.
 * Keep the flag atomic with open, rather than opening an inheritable FD then changing it.
 */
internal object MediaNativeFlags {
    const val CLOSE_ON_EXEC = 0x80000
}
