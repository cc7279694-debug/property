package com.cooper.lithic.testutil

import android.content.Context
import android.os.Process
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import android.util.Log
import java.io.File
import java.nio.file.Files
import java.util.UUID

/** Test-only capabilities for roots created by this object, never a scan of app storage.
 * Owners must explicitly finish closing before cleanup. Unknown/replaced/link resources
 * preserve the root before any unlink; a later failure preserves all remaining resources.
 * Every admitted node is rechecked before its individual unlink.
 */
internal class M20TestRoots {
    private data class Owned(val root: File, val nonce: String, val identity: StructStat,
        val files: MutableSet<String>, val directories: MutableSet<String>, var closed: Boolean = false)
    private val roots = linkedMapOf<File, Owned>()

    fun create(context: Context, purpose: String): File {
        require(Regex("[a-z0-9-]+").matches(purpose))
        val parent = context.noBackupFilesDir.canonicalFile
        val nonce = UUID.randomUUID().toString()
        val root = File(parent, "m20-$purpose-$nonce")
        check(root.canonicalFile == root.absoluteFile && root.mkdir())
        val identity = owned(root, directory = true)
        val marker = File(root, MARKER)
        check(marker.createNewFile())
        marker.writeText(nonce, Charsets.UTF_8)
        roots[root] = Owned(root, nonce, identity, mutableSetOf(MARKER), mutableSetOf())
        return root
    }

    fun files(root: File, vararg paths: String) {
        val record = roots.getValue(root)
        for (path in paths) {
            require(path.isNotEmpty() && path.split('/').all { it.isNotEmpty() && it != "." && it != ".." } && '\\' !in path && ':' !in path)
            record.files += path
            val parts = path.split('/')
            for (size in 1 until parts.size) record.directories += parts.take(size).joinToString("/")
        }
    }

    fun database(root: File, prefix: String = "") {
        files(root, "${prefix}data.db", "${prefix}data.db-wal", "${prefix}data.db-shm", "${prefix}data.db-journal")
    }

    fun generation(root: File, id: String, prefix: String = "") {
        require(UUID.fromString(id).toString() == id)
        val base = "${prefix}generations/$id/"
        database(root, base)
        files(root, "${prefix}active-generation", "${base}settings.preferences_pb", "${base}ready.json", "${base}restore-proof.json")
        roots.getValue(root).directories += "${base}media".removeSuffix("/")
    }

    /** Explicit registered media paths come from the fixture or successfully validated archive. */
    fun media(root: File, paths: Collection<String>, generationId: String? = null) {
        for (path in paths) {
            require(path.startsWith("media/"))
            files(root, if (generationId == null) "payload/$path" else "generations/$generationId/$path")
        }
    }

    fun ownersOpening(root: File) { roots.getValue(root).closed = false }
    fun ownersClosed(root: File) { roots.getValue(root).closed = true }

    fun cleanupClosed() {
        for (record in roots.values) {
            try {
                check(record.closed) { "Owners did not confirm close" }
                check(same(record.identity, owned(record.root, true))) { "Root replaced" }
                val marker = File(record.root, MARKER)
                owned(marker, false)
                check(marker.readText(Charsets.UTF_8) == record.nonce) { "Root capability changed" }
                val nodes = Files.walk(record.root.toPath()).use { stream ->
                    stream.iterator().asSequence().filter { it != record.root.toPath() }.map { it.toFile() }.toList()
                }
                val identities = nodes.associateWith { file ->
                    val path = record.root.toPath().relativize(file.toPath()).joinToString("/")
                    val stat = Os.lstat(file.path)
                    val dir = OsConstants.S_ISDIR(stat.st_mode)
                    check(if (dir) path in record.directories else path in record.files) { "Unknown child: $path" }
                    owned(file, dir)
                }
                // Complete admission is checked before any deletion. No symlinks are followed.
                identities.forEach { (file, stat) -> check(same(stat, owned(file, OsConstants.S_ISDIR(stat.st_mode)))) }
                for (file in nodes.filter { it != marker }.sortedByDescending { it.toPath().nameCount }) {
                    val stat = identities.getValue(file)
                    check(same(stat, owned(file, OsConstants.S_ISDIR(stat.st_mode))))
                    if (OsConstants.S_ISDIR(stat.st_mode)) check(file.listFiles()?.isEmpty() == true)
                    check(file.delete())
                }
                check(same(record.identity, owned(record.root, true)))
                check(marker.readText(Charsets.UTF_8) == record.nonce && same(identities.getValue(marker), owned(marker, false)))
                check(record.root.listFiles()?.toSet() == setOf(marker))
                check(marker.delete() && record.root.delete())
                Log.i("LITHIC_M20_CLEANUP", "REMOVED ${record.root.name}")
            } catch (failure: Exception) {
                Log.w("LITHIC_M20_CLEANUP", "PRESERVED ${record.root.name}: ${failure.message}")
            }
        }
        roots.clear()
    }

    private fun owned(file: File, directory: Boolean): StructStat {
        check(file.canonicalFile == file.absoluteFile) { "Link or escaping path" }
        return Os.lstat(file.path).also { stat ->
            check(stat.st_uid == Process.myUid())
            check(if (directory) OsConstants.S_ISDIR(stat.st_mode) else OsConstants.S_ISREG(stat.st_mode) && stat.st_nlink == 1L)
        }
    }
    private fun same(a: StructStat, b: StructStat) = a.st_dev == b.st_dev && a.st_ino == b.st_ino && a.st_uid == b.st_uid && a.st_mode == b.st_mode
    private companion object { const val MARKER = ".m20-test-owner" }
}
