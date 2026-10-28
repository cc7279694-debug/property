package com.cooper.lithic.app

import org.junit.Assert.*
import org.junit.Test

class AppProcessRoleTest {
    private val main = "com.cooper.lithic.debug"

    @Test fun exact_main_identity_is_the_only_business_role() {
        listOf(main, "com.cooper.lithic.preview.debug", "com.cooper.lithic.c2visual.debug").forEach { name ->
            assertEquals(AppProcessRole.MAIN, AppProcessRoles.resolve(name, name))
        }
    }

    @Test fun exact_media_process_suffix_selects_sandbox_for_each_install_identity() {
        listOf(main, "com.cooper.lithic.preview.debug").forEach { name ->
            assertEquals(AppProcessRole.SANDBOX, AppProcessRoles.resolve(name, "$name:media_sandbox"))
        }
    }

    @Test fun other_secondary_processes_and_prefix_collisions_are_unknown() {
        listOf("$main:jpeg_worker", "$main:media_sandbox_extra", "$main.other", "other.app:media_sandbox").forEach { name ->
            assertEquals(AppProcessRole.UNKNOWN, AppProcessRoles.resolve(main, name))
        }
    }

    @Test fun unavailable_empty_or_malformed_identities_fail_closed() {
        listOf(null, "", " ", "$main\u0000", "$main\n").forEach { name ->
            assertEquals(AppProcessRole.UNKNOWN, AppProcessRoles.resolve(main, name))
            assertEquals(AppProcessRole.UNKNOWN, AppProcessRoles.resolve(name, main))
        }
    }

    @Test fun proc_name_reader_uses_only_the_first_nul_terminated_argument() {
        val bytes = ("$main:media_sandbox\u0000ignored private-looking arguments\u0000").toByteArray(Charsets.US_ASCII)
        assertEquals("$main:media_sandbox", AppProcessRoles.decodeOwnProcessName(bytes))
    }

    @Test fun absent_terminator_truncation_and_empty_first_argument_are_rejected() {
        assertNull(AppProcessRoles.decodeOwnProcessName(main.toByteArray()))
        assertNull(AppProcessRoles.decodeOwnProcessName(byteArrayOf(0)))
        assertNull(AppProcessRoles.decodeOwnProcessName(ByteArray(256) { 'a'.code.toByte() }))
        assertNull(AppProcessRoles.decodeOwnProcessName(ByteArray(257)))
    }

    @Test fun whitespace_and_non_ascii_proc_names_are_rejected() {
        for (bytes in listOf("bad name\u0000".toByteArray(), byteArrayOf(0xc3.toByte(), 0xa9.toByte(), 0))) {
            assertNull(AppProcessRoles.decodeOwnProcessName(bytes))
        }
    }

    @Test fun valid_maximum_bounded_first_argument_is_not_silently_truncated() {
        val name = "a".repeat(255)
        assertEquals(name, AppProcessRoles.decodeOwnProcessName((name + "\u0000").toByteArray()))
    }
}
