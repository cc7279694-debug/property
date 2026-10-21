package com.cooper.lithic.data.backup

import android.os.Process
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject
import org.json.JSONTokener

/** Only a closed, explicitly detached previous generation is eligible; never scans sibling data. */
internal object RetiredGenerationCleanup {
    fun clean(oldDirectory: File, oldId: String, replacementId: String, expected: BackupSnapshot): Boolean = try {
        requireCanonicalId(oldId)
        requireCanonicalId(replacementId)
        require(oldId != replacementId)
        val old = oldDirectory.toPath()
        require(old == old.toAbsolutePath().normalize() && old.fileName.toString() == oldId)
        val generations = requireNotNull(old.parent)
        require(generations.fileName.toString() == "generations")
        val root = requireNotNull(generations.parent)
        val replacement = generations.resolve(replacementId)
        val rootIdentity = ownedDirectory(root)
        val generationsIdentity = ownedDirectory(generations)
        val oldIdentity = ownedDirectory(old)
        val replacementIdentity = ownedDirectory(replacement)
        val replacementReady = stamp(replacement.resolve("ready.json"))
        fun requireDetached() {
            check(sameDirectory(rootIdentity, ownedDirectory(root)) &&
                sameDirectory(generationsIdentity, ownedDirectory(generations)) &&
                sameDirectory(replacementIdentity, ownedDirectory(replacement)) &&
                sameDirectory(oldIdentity, ownedDirectory(old))) { "Cleanup generation identity changed" }
            require(!Files.exists(root.resolve("active-generation.bak"), NOFOLLOW_LINKS) &&
                !Files.exists(root.resolve("active-generation.new"), NOFOLLOW_LINKS)) { "Committed activation is ambiguous" }
            val pointer = metadata(stamp(root.resolve("active-generation")), POINTER_FIELDS)
            require(pointer.get("formatVersion") == 1 && pointer.get("activeId") == replacementId &&
                pointer.get("previousId") == JSONObject.NULL && pointer.get("stage") == "STABLE") {
                "Old generation remains reachable from activation metadata"
            }
            verify(replacementReady)
            val ready = metadata(replacementReady, READY_FIELDS)
            require(ready.get("formatVersion") == 1 && ready.get("generationId") == replacementId) {
                "Replacement readiness identifies another generation"
            }
        }
        requireDetached()

        val media = expected.data.mediaAssets
        require(media.map { it.relativePath }.toSet().size == media.size)
        media.forEach { asset ->
            BackupEntryFiles.requirePath(asset.relativePath)
            require(asset.relativePath.startsWith("media/") && asset.byteLength in 1 until BackupLimits.MAX_MEDIA_BYTES &&
                Regex("[0-9a-fA-F]{64}").matches(asset.sha256)) { "Previous media ownership is incomplete" }
        }
        val proof = old.resolve("restore-proof.json")
        val files = REQUIRED_FILES + media.map { it.relativePath } +
            if (Files.exists(proof, NOFOLLOW_LINKS)) setOf("restore-proof.json") else emptySet()
        val directories = setOf("media") + files.flatMap { name ->
            val parts = name.split('/')
            (1 until parts.size).map { parts.take(it).joinToString("/") }
        }
        val expectedTree = files + directories
        require(tree(old) == expectedTree) { "Unknown previous-generation resources are preserved" }
        val directoryIdentities = directories.associateWith { ownedDirectory(old.resolve(it)) }
        // No deletion has occurred. Every known file must first be an unlinked, owned regular file.
        val stamps = files.associateWith { stamp(old.resolve(it)) }
        require(stamps.getValue("data.db").stat.st_size >= 100L)
        require(stamps.getValue("settings.preferences_pb").stat.st_size in 1 until BackupLimits.MAX_SETTINGS_BYTES)
        val ready = metadata(stamps.getValue("ready.json"), READY_FIELDS)
        require(ready.get("formatVersion") == 1 && ready.get("generationId") == oldId)
        if ("restore-proof.json" in stamps) {
            val historical = metadata(stamps.getValue("restore-proof.json"), PROOF_FIELDS)
            require(historical.get("formatVersion") == 1 && historical.get("generationId") == oldId)
            requireCanonicalId(historical.getString("nonce"))
            require(listOf("dataSha256", "settingsSha256").all {
                Regex("[0-9a-f]{64}").matches(historical.getString(it))
            })
            // Stable generations can contain legitimate later edits. Their old restoration proof
            // binds ownership, but its original data hash is not the current business snapshot.
        }
        media.forEach { asset ->
            val file = stamps.getValue(asset.relativePath)
            require(file.stat.st_size == asset.byteLength && file.sha256.equals(asset.sha256, ignoreCase = true)) {
                "Previous media differs from its registered facts"
            }
        }
        // Complete admission is repeated before the FIRST unlink, so changed/unknown input is
        // retained in full. Later IO failure can leave a detached partial directory, never active data.
        requireDetached()
        require(tree(old) == expectedTree)
        directoryIdentities.forEach { (path, identity) -> check(sameDirectory(identity, ownedDirectory(old.resolve(path)))) }
        stamps.values.forEach(::verify)
        val mediaFiles = media.map { it.relativePath }.sorted()
        val order = mediaFiles + listOfNotNull("restore-proof.json".takeIf { it in stamps }) +
            listOf("data.db", "settings.preferences_pb", "ready.json")
        for (name in order) {
            requireDetached()
            verify(stamps.getValue(name))
            val path = old.resolve(name)
            Files.delete(path)
            syncDirectory(requireNotNull(path.parent))
        }
        for (name in directories.sortedWith(compareByDescending<String> { it.count { character -> character == '/' } }.thenByDescending { it.length })) {
            requireDetached()
            val path = old.resolve(name)
            check(sameDirectory(directoryIdentities.getValue(name), ownedDirectory(path)))
            require(Files.list(path).use { !it.findAny().isPresent }) { "Unknown new residue is preserved" }
            Files.delete(path)
            syncDirectory(requireNotNull(path.parent))
        }
        requireDetached()
        require(Files.list(old).use { !it.findAny().isPresent })
        Files.delete(old)
        syncDirectory(generations)
        true
    } catch (_: Exception) {
        // No recursive deletion, overwrite, repair or continuation of an unknown partial cleanup.
        false
    }

    private data class FileStamp(val path: Path, val stat: StructStat, val sha256: String)

    private fun stamp(path: Path): FileStamp {
        val before = ownedFile(path)
        val fd = Os.open(path.toString(), OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or CLOSE_ON_EXEC, 0)
        try {
            require(sameFile(before, Os.fstat(fd))) { "Cleanup file changed during open" }
            val digest = MessageDigest.getInstance("SHA-256")
            var count = 0L
            FileInputStream(fd).use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) throw IOException("Cleanup verifier made no progress")
                    count = Math.addExact(count, read.toLong())
                    require(count <= before.st_size) { "Cleanup file grew during verification" }
                    digest.update(buffer, 0, read)
                }
                require(count == before.st_size && sameFile(before, Os.fstat(fd)) && sameFile(before, ownedFile(path))) {
                    "Cleanup file changed during verification"
                }
            }
            return FileStamp(path, before, with(BackupEntryFiles) { digest.digest().hex() })
        } finally { if (fd.valid()) Os.close(fd) }
    }

    private fun verify(expected: FileStamp) {
        val actual = stamp(expected.path)
        require(sameFile(expected.stat, actual.stat) && expected.sha256 == actual.sha256) { "Known cleanup file changed" }
    }

    private fun metadata(file: FileStamp, fields: Set<String>): JSONObject {
        require(file.stat.st_size in 1..MAX_METADATA_BYTES)
        verify(file)
        val bytes = Files.newInputStream(file.path, NOFOLLOW_LINKS).use { input ->
            val buffer = ByteArray(file.stat.st_size.toInt())
            var offset = 0
            while (offset < buffer.size) {
                val read = input.read(buffer, offset, buffer.size - offset)
                if (read <= 0) throw IOException("Cleanup metadata is incomplete")
                offset += read
            }
            require(input.read() == -1)
            buffer
        }
        verify(file)
        val parser = JSONTokener(bytes.toString(Charsets.UTF_8))
        val value = parser.nextValue() as? JSONObject ?: throw IOException("Cleanup metadata is not an object")
        require(parser.nextClean() == '\u0000')
        val names = value.keys()
        val actual=buildSet { while(names.hasNext()) add(names.next()) }
        require(actual==fields || fields==PROOF_FIELDS && actual==PROOF_FIELDS+"dataFormatVersion")
        if(fields==PROOF_FIELDS && "dataFormatVersion" in actual) require(value.get("dataFormatVersion")==2)
        return value
    }

    private fun tree(root: Path): Set<String> = Files.walk(root).use { paths ->
        val result = mutableSetOf<String>()
        val iterator = paths.iterator()
        while (iterator.hasNext()) {
            val path = iterator.next()
            if (path == root) continue
            require(result.size < BackupLimits.MAX_ENTRIES) { "Previous tree exceeds its resource limit" }
            result += root.relativize(path).joinToString("/")
        }
        result
    }

    private fun ownedDirectory(path: Path): StructStat {
        BackupEntryFiles.requireDirectory(path)
        return Os.lstat(path.toString()).also {
            require(OsConstants.S_ISDIR(it.st_mode) && it.st_uid == Process.myUid()) { "Cleanup directory owner is unknown" }
        }
    }

    private fun ownedFile(path: Path): StructStat {
        ownedDirectory(requireNotNull(path.parent))
        return Os.lstat(path.toString()).also {
            require(OsConstants.S_ISREG(it.st_mode) && it.st_uid == Process.myUid() && it.st_nlink == 1L && it.st_size >= 0) {
                "Cleanup file is not uniquely owned"
            }
        }
    }

    private fun syncDirectory(path: Path) {
        val before = ownedDirectory(path)
        val fd = Os.open(path.toString(), OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or CLOSE_ON_EXEC, 0)
        try {
            val held = Os.fstat(fd)
            require(OsConstants.S_ISDIR(held.st_mode) && held.st_uid == Process.myUid() && sameDirectory(before, held))
            Os.fsync(fd)
            require(sameDirectory(before, ownedDirectory(path)))
        } finally { Os.close(fd) }
    }

    private fun requireCanonicalId(value: String) = require(UUID.fromString(value).toString() == value)
    private fun sameDirectory(a: StructStat, b: StructStat): Boolean = a.st_dev == b.st_dev && a.st_ino == b.st_ino &&
        a.st_uid == b.st_uid && a.st_mode == b.st_mode
    private fun sameFile(a: StructStat, b: StructStat): Boolean = sameDirectory(a, b) && a.st_nlink == b.st_nlink &&
        a.st_size == b.st_size && a.st_mtime == b.st_mtime && a.st_ctime == b.st_ctime

    private const val CLOSE_ON_EXEC = 0x80000
    private const val MAX_METADATA_BYTES = 16_384L
    private val REQUIRED_FILES = setOf("data.db", "settings.preferences_pb", "ready.json")
    private val READY_FIELDS = setOf("formatVersion", "generationId")
    private val POINTER_FIELDS = setOf("formatVersion", "activeId", "previousId", "stage")
    private val PROOF_FIELDS = setOf("formatVersion", "generationId", "nonce", "dataSha256", "settingsSha256")
}
