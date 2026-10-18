package com.cooper.lithic.data.media.jpegstage

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/** Deliberately small canonical ASCII records, not lenient JSON parsing. */
internal class StageRecords(
    private val probe: (File) -> JpegStageV2.FileIdentity,
    private val sync: (File) -> Unit,
) {
    fun safeDirectory(directory: File) {
        if (!directory.isDirectory || Files.isSymbolicLink(directory.toPath()) || directory.canonicalFile != directory.absoluteFile) blocked("Unsafe stage directory")
    }

    fun child(directory: File, name: String): File {
        safeDirectory(directory)
        if (!name.matches(Regex("[A-Za-z0-9._-]+")) || name == "." || name == "..") blocked("Invalid stage child")
        return File(directory, name).also {
            if (Files.isSymbolicLink(it.toPath()) || it.canonicalFile.parentFile != directory) blocked("Linked stage child")
        }
    }

    fun exists(file: File): Boolean = Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)

    fun fingerprint(file: File): JpegStageV2.FileIdentity {
        if (!file.isFile || Files.isSymbolicLink(file.toPath())) blocked("Stage file is not regular")
        // A probe may hash a nonempty file by opening and closing it. Doing that
        // to a corrupted guard could drop a same-process POSIX lock before reject.
        if (file.name in setOf(".namespace.guard", "consumer.guard") && file.length() != 0L) blocked("JPEG guard is not empty")
        val found = probe(file)
        if (found.device < 0 || found.inode <= 0 || found.bytes < 0 || !found.sha256.matches(Regex("[0-9a-f]{64}"))) blocked("Invalid stage file identity")
        return found
    }

    fun requireIdentity(file: File, expected: JpegStageV2.FileIdentity) {
        if (fingerprint(file) != expected) blocked("Stage file identity changed")
    }

    fun requireInode(file: File, expected: JpegStageV2.FileIdentity) {
        val current = fingerprint(file)
        if (current.device != expected.device || current.inode != expected.inode) blocked("Stage inode changed")
    }

    fun encode(values: Map<String, String>): ByteArray {
        if (values.isEmpty() || values.keys.any { !it.matches(Regex("[a-zA-Z][a-zA-Z0-9]*")) } ||
            values.values.any { it.isEmpty() || it.length > 256 || !it.matches(Regex("[a-zA-Z0-9._:-]+")) }) blocked("Invalid stage record value")
        return values.toSortedMap().entries.joinToString("", transform = { "${it.key}=${it.value}\n" }).toByteArray(Charsets.US_ASCII).also {
            if (it.size > 16_384) blocked("Stage record exceeds limit")
        }
    }

    fun read(file: File, keys: Set<String>): Map<String, String> {
        val before = fingerprint(file)
        if (before.bytes !in 1..16_384) blocked("Invalid stage record length")
        val bytes = FileChannel.open(file.toPath(), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS).use { channel ->
            if (channel.size() != before.bytes) blocked("Stage record length changed")
            val buffer = ByteBuffer.allocate(before.bytes.toInt())
            while (buffer.hasRemaining()) { if (channel.read(buffer) < 0) blocked("Truncated stage record") }
            if (channel.read(ByteBuffer.allocate(1)) != -1) blocked("Growing stage record")
            buffer.array()
        }
        if (fingerprint(file) != before) blocked("Stage record changed while reading")
        if (bytes.any { it != 10.toByte() && (it.toInt() !in 32..126) }) blocked("Noncanonical stage record")
        val text = String(bytes, Charsets.US_ASCII)
        if (!text.endsWith('\n')) blocked("Truncated stage record")
        val rows = text.dropLast(1).split('\n')
        val pairs = rows.map {
            val index = it.indexOf('=')
            if (index < 1 || index != it.lastIndexOf('=')) blocked("Malformed stage record")
            it.substring(0, index) to it.substring(index + 1)
        }
        if (pairs.map { it.first }.toSet().size != pairs.size) blocked("Duplicate stage field")
        val result = pairs.toMap()
        if (result.keys != keys || !encode(result).contentEquals(bytes)) blocked("Unknown or noncanonical stage fields")
        return result
    }

    /** Existing equal records are idempotent; conflicting or partial writes never get overwritten. */
    fun put(file: File, values: Map<String, String>) {
        val bytes = encode(values)
        if (exists(file)) {
            if (read(file, values.keys) != values) blocked("Conflicting durable stage record")
            confirm(file, values.keys)
            return
        }
        FileChannel.open(file.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS).use { channel ->
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining()) channel.write(buffer)
            channel.force(true)
        }
        sync(file.parentFile ?: blocked("Stage record has no parent"))
        if (read(file, values.keys) != values) blocked("Stage record readback failed")
    }

    /** Re-establish durability after an interrupted/unknown fsync, never infer it from visibility. */
    fun confirm(file: File, keys: Set<String>): Map<String, String> {
        val values = read(file, keys)
        val before = fingerprint(file)
        FileChannel.open(file.toPath(), StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS).use { it.force(true) }
        sync(file.parentFile ?: blocked("Stage record has no parent"))
        if (fingerprint(file) != before || read(file, keys) != values) blocked("Stage durability confirmation changed identity")
        return values
    }

    fun digest(file: File): String = fingerprint(file).sha256
    fun digest(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    fun sync(directory: File) { safeDirectory(directory); sync.invoke(directory) }
    fun delete(file: File) {
        if (exists(file) && (!file.isFile || Files.isSymbolicLink(file.toPath()) || !file.delete())) blocked("Cannot delete known stage file")
    }
    private fun blocked(message: String): Nothing = throw JpegStageV2.Blocked(message)
}
