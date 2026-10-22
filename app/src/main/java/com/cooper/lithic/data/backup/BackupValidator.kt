package com.cooper.lithic.data.backup

import com.cooper.lithic.data.backup.BackupEntryFiles.hex
import java.io.FilterInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN
import java.nio.channels.FileChannel
import java.nio.charset.CodingErrorAction.REPORT
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.time.DateTimeException
import java.time.ZoneId
import java.util.TreeSet
import java.util.zip.CRC32
import java.util.zip.DataFormatException
import java.util.zip.Inflater
import java.util.zip.ZipFile
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

internal data class ValidatedBackup(val snapshot: BackupSnapshot, val manifest: BackupManifestV1, val directory: Path, val archiveSha256: String)

/** A bounded, read-only admission pass precedes any extraction into the caller's owned empty directory. */
@OptIn(ExperimentalSerializationApi::class)
internal class BackupValidator {
    fun validate(archive: Path, extractionRoot: Path, checkpoint: () -> Unit = {}): ValidatedBackup = try {
        validateInternal(archive, extractionRoot, checkpoint)
    } catch (invalid: DateTimeException) {
        throw IllegalArgumentException("Invalid backup date, instant or zone", invalid)
    } catch (overflow: ArithmeticException) {
        throw IllegalArgumentException("Backup integer budget or business aggregate overflow", overflow)
    }

    private fun validateInternal(archive: Path, extractionRoot: Path, checkpoint: () -> Unit): ValidatedBackup {
        checkpoint()
        val before = BackupArchiveFiles.regularArchive(archive)
        require(extractionRoot == extractionRoot.toAbsolutePath().normalize())
        BackupEntryFiles.requireDirectory(extractionRoot)
        val rootKey = Files.readAttributes(extractionRoot, BasicFileAttributes::class.java, NOFOLLOW_LINKS).fileKey()
        Files.list(extractionRoot).use { require(!it.findAny().isPresent) { "Extraction directory is not empty" } }
        val records = BackupZipStructure.read(archive, before.size(), checkpoint)
        ZipFile(archive.toFile()).use { zip ->
            require(zip.size() == records.size)
            val byName = records.associateBy { it.name }
            val manifestRecord = byName["manifest.json"] ?: throw IllegalArgumentException("Missing manifest")
            val manifest = decode(zip, manifestRecord, BackupManifestV1.serializer(), null, checkpoint)
            requireManifest(manifest, byName)
            val declared = manifest.files.associateBy { it.path }
            val snapshot = if(manifest.formatVersion==1) {
                // Authenticate original v1 fields, declared length/SHA and seven-table counts before explicit DTO migration.
                val legacy=decode(zip,byName.getValue("data.json"),BackupDataV1.serializer(),declared.getValue("data.json"),checkpoint)
                require(legacy.recordCounts()==manifest.recordCounts) { "Legacy record counts disagree" }
                val settings = decode(zip, byName.getValue("settings.json"), BackupSettingsV1.serializer(), declared.getValue("settings.json"), checkpoint)
                BackupSnapshot(legacy.upgrade(), settings, emptyList(), com.cooper.lithic.data.settings.ReminderPreferences())
            } else if(manifest.formatVersion==2) {
                val legacy = decode(zip,byName.getValue("data.json"),BackupDataV2.serializer(),declared.getValue("data.json"),checkpoint)
                require(legacy.recordCounts() == manifest.recordCounts) { "Format-2 original record counts disagree" }
                val settings = decode(zip, byName.getValue("settings.json"), BackupSettingsV1.serializer(), declared.getValue("settings.json"), checkpoint)
                BackupSnapshot(legacy.ordered(), settings, emptyList(), com.cooper.lithic.data.settings.ReminderPreferences())
            } else {
                val data = decode(zip,byName.getValue("data.json"),BackupDataV3.serializer(),declared.getValue("data.json"),checkpoint)
                require(data.recordCounts() == manifest.recordCounts) { "Format-3 record counts disagree" }
                val settings = decode(zip,byName.getValue("settings.json"),BackupSettingsV3.serializer(),declared.getValue("settings.json"),checkpoint)
                BackupSnapshot(data.storedData(), BackupSettingsV1(settings.recordZoneId),
                    data.notificationReceipts.sortedBy { it.logicalKey }, settings.preferences())
            }
            BackupBusinessValidation.validate(snapshot, manifest, checkpoint)
            // Every record, its path, structure, resource budget, format and business relationship
            // has now passed. Extraction has no connection to the active generation.
            for (record in records) {
                checkpoint()
                val destination = extractionRoot.resolve(record.name)
                require(destination.normalize() == destination && destination.startsWith(extractionRoot))
                var parent = extractionRoot
                for (part in extractionRoot.relativize(destination.parent)) {
                    parent = parent.resolve(part)
                    if (!Files.exists(parent, NOFOLLOW_LINKS)) Files.createDirectory(parent)
                    BackupEntryFiles.requireDirectory(parent)
                }
                BackupEntryFiles.requireDirectory(destination.parent)
                readEntry(zip, record, declared[record.name], checkpoint) { input ->
                    Files.newOutputStream(destination, CREATE_NEW, WRITE, NOFOLLOW_LINKS).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            checkpoint()
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(count > 0) { "ZIP reader made no progress" }
                            output.write(buffer, 0, count)
                        }
                        output.flush()
                    }
                }
            }
            BackupEntryFiles.requireDirectory(extractionRoot)
            require(rootKey == Files.readAttributes(extractionRoot, BasicFileAttributes::class.java, NOFOLLOW_LINKS).fileKey()) { "Extraction root was replaced" }
            val digest = Files.newInputStream(archive, NOFOLLOW_LINKS).use { BackupArchiveFiles.hash(it, before.size(), checkpoint) }
            require(BackupArchiveFiles.sameIdentity(before, BackupArchiveFiles.regularArchive(archive))) { "Archive changed during validation" }
            checkpoint()
            return ValidatedBackup(snapshot, manifest, extractionRoot, digest)
        }
    }

    private fun requireManifest(manifest: BackupManifestV1, records: Map<String, BackupZipRecord>) {
        BackupFormats.requireSupported(manifest)
        require(manifest.producerAppVersion.isNotBlank() && manifest.producerAppVersion.length <= 128 && manifest.producerAppVersion.none { it.code < 32 || it.code == 127 })
        BackupBusinessValidation.instant(manifest.createdAt)
        ZoneId.of(manifest.recordZoneId)
        require(manifest.recordCounts.values.all { it >= 0 } && manifest.recordCounts.values.fold(0L, Math::addExact) < BackupLimits.MAX_RECORDS)
        val names = manifest.files.map { it.path }
        require(names.size == names.toSet().size && names.toSet() == records.keys - "manifest.json") { "Missing, duplicate or undeclared payload" }
        require("data.json" in names && "settings.json" in names)
        manifest.files.forEach { require(it.byteLength == records.getValue(it.path).expanded) { "Manifest entry length differs" } }
    }

    private fun <T> decode(zip: ZipFile, record: BackupZipRecord, strategy: DeserializationStrategy<T>, expected: BackupFileEntry?, checkpoint: () -> Unit): T {
        readEntry(zip, record, expected, checkpoint) { input ->
            val decoder = Charsets.UTF_8.newDecoder().onMalformedInput(REPORT).onUnmappableCharacter(REPORT)
            StrictBackupJson(InputStreamReader(input, decoder).buffered(16 * 1024), checkpoint).scan(strategy.descriptor)
        }
        return readEntry(zip, record, expected, checkpoint) { input -> JSON.decodeFromStream(strategy, input) }
    }

    private fun <T> readEntry(zip: ZipFile, record: BackupZipRecord, expected: BackupFileEntry?, checkpoint: () -> Unit, block: (InputStream) -> T): T {
        val entry = zip.getEntry(record.name) ?: throw IllegalArgumentException("Missing ZIP payload")
        require(entry.size == record.expanded && entry.compressedSize == record.compressed && entry.crc == record.crc && entry.method == record.method)
        zip.getInputStream(entry).use { source ->
            val crc = CRC32()
            val sha = MessageDigest.getInstance("SHA-256")
            var length = 0L
            val input = object : FilterInputStream(source) {
                override fun read(): Int {
                    val one = ByteArray(1)
                    return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 255
                }
                override fun read(bytes: ByteArray, offset: Int, count: Int): Int {
                    checkpoint()
                    val read = source.read(bytes, offset, count)
                    if (read > 0) {
                        length = Math.addExact(length, read.toLong())
                        require(length <= record.expanded) { "ZIP expanded beyond declared budget" }
                        crc.update(bytes, offset, read); sha.update(bytes, offset, read)
                    } else require(read < 0 || count == 0) { "ZIP reader made no progress" }
                    return read
                }
            }
            val result = block(input)
            require(input.read() == -1 && length == record.expanded && crc.value == record.crc) { "ZIP length or CRC differs" }
            if (expected != null) require(sha.digest().hex() == expected.sha256) { "ZIP payload SHA-256 differs" }
            return result
        }
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = false; isLenient = false; coerceInputValues = false; explicitNulls = true }
    }
}

private data class BackupZipRecord(val name: String, val nameBytes: ByteArray, val flags: Int, val method: Int,
    val crc: Long, val compressed: Long, val expanded: Long, val localOffset: Long, var dataOffset: Long = -1)

/** Reject ambiguity before ZipFile is allowed to select a record or create an output path. */
private object BackupZipStructure {
    // Parser admission budgets, independent of logical backup format/schema versions.
    // Android's native path components cannot represent names beyond these byte bounds.
    private const val MAX_PATH_BYTES = 4096
    private const val MAX_COMPONENT_BYTES = 255
    private const val MAX_CENTRAL_METADATA_BYTES = 16L * 1024 * 1024

    fun read(path: Path, size: Long, checkpoint: () -> Unit): List<BackupZipRecord> = FileChannel.open(path, READ, NOFOLLOW_LINKS).use { channel ->
        fun bytes(offset: Long, length: Int): ByteBuffer {
            require(offset >= 0 && length >= 0 && Math.addExact(offset, length.toLong()) <= size)
            val buffer = ByteBuffer.allocate(length).order(LITTLE_ENDIAN)
            var position = offset
            while (buffer.hasRemaining()) {
                checkpoint()
                val count = channel.read(buffer, position)
                require(count > 0) { "Truncated ZIP record" }
                position += count
            }
            return buffer.apply { flip() }
        }
        require(size >= 22)
        val end = bytes(size - 22, 22)
        require(end.getInt(0) == 0x06054b50 && end.u16(4) == 0 && end.u16(6) == 0 && end.u16(20) == 0) { "Missing end directory, multi-disk, comment or trailing bytes" }
        val count = end.u16(10)
        require(count in 3 until BackupLimits.MAX_ENTRIES && count == end.u16(8))
        val centralSize = end.u32(12); val centralStart = end.u32(16)
        require(centralSize > 0 && Math.addExact(centralStart, centralSize) == size - 22) { "Invalid central directory range" }
        require(centralSize <= MAX_CENTRAL_METADATA_BYTES) { "ZIP metadata resource limit: central directory exceeds 16 MiB" }
        val records = ArrayList<BackupZipRecord>(count)
        val names = HashSet<String>()
        var cursor = centralStart
        var total = 0L
        repeat(count) {
            checkpoint()
            val header = bytes(cursor, 46)
            require(header.getInt(0) == 0x02014b50 && header.u16(6) <= 20 && header.u16(34) == 0)
            val flags = header.u16(8); val method = header.u16(10)
            require(flags and 0x0808 == flags && method in setOf(0, 8)) { "Unsupported encryption, flags or compression" }
            val nameLength = header.u16(28); val extraLength = header.u16(30); val commentLength = header.u16(32)
            require(nameLength > 0 && commentLength == 0)
            require(nameLength <= MAX_PATH_BYTES) { "ZIP path resource limit: UTF-8 name exceeds 4096 bytes" }
            val nameBytes = ByteArray(nameLength).also { bytes(cursor + 46, nameLength).get(it) }
            var componentBytes = 0
            for (byte in nameBytes) {
                componentBytes = if (byte == '/'.code.toByte()) 0 else componentBytes + 1
                require(componentBytes <= MAX_COMPONENT_BYTES) { "ZIP path resource limit: component exceeds 255 UTF-8 bytes" }
            }
            val name = Charsets.UTF_8.newDecoder().onMalformedInput(REPORT).decode(ByteBuffer.wrap(nameBytes)).toString()
            require(flags and 0x0800 != 0 || nameBytes.all { it.toInt() in 0..127 }) { "Ambiguous ZIP name encoding" }
            if (name != "manifest.json") BackupEntryFiles.requirePath(name)
            require(names.add(name)) { "Duplicate ZIP path" }
            val attrs = header.u32(38)
            val kind = ((attrs ushr 16).toInt() and 0xf000)
            require(kind == 0 || kind == 0x8000) { "Nonregular ZIP entry, including symlink" }
            require(attrs and 0x10L == 0L) { "ZIP directory entry" }
            extra(bytes(cursor + 46 + nameLength, extraLength))
            val expanded = header.u32(24); val compressed = header.u32(20)
            val limit = when (name) { "data.json", "manifest.json" -> BackupLimits.MAX_DATA_BYTES; "settings.json" -> BackupLimits.MAX_SETTINGS_BYTES; else -> BackupLimits.MAX_MEDIA_BYTES }
            require(expanded < limit && compressed < size && compressed != 0xffffffffL && expanded != 0xffffffffL)
            if (method == 0) require(compressed == expanded && flags and 8 == 0)
            total = Math.addExact(total, expanded)
            require(total < BackupLimits.MAX_TOTAL_BYTES) { "Expanded backup reached total budget" }
            records += BackupZipRecord(name, nameBytes, flags, method, header.u32(16), compressed, expanded, header.u32(42))
            cursor = Math.addExact(cursor, 46L + nameLength + extraLength + commentLength)
            require(cursor <= size - 22)
        }
        require(cursor == size - 22 && names.containsAll(setOf("manifest.json", "data.json", "settings.json")))
        val sortedNames = TreeSet(names)
        names.forEach { name ->
            val prefix = "$name/"
            val possibleChild = sortedNames.ceiling(prefix)
            require(possibleChild == null || !possibleChild.startsWith(prefix)) { "File and directory paths collide" }
        }
        var localEnd = 0L
        for (record in records.sortedBy { it.localOffset }) {
            checkpoint()
            require(record.localOffset == localEnd) { "ZIP local records overlap or hide bytes" }
            val local = bytes(record.localOffset, 30)
            require(local.getInt(0) == 0x04034b50 && local.u16(4) <= 20 && local.u16(6) == record.flags && local.u16(8) == record.method)
            val nameLength = local.u16(26); val extraLength = local.u16(28)
            require(nameLength == record.nameBytes.size)
            val localName = ByteArray(nameLength).also { bytes(record.localOffset + 30, nameLength).get(it) }
            require(localName.contentEquals(record.nameBytes)) { "Local and central names disagree" }
            extra(bytes(record.localOffset + 30 + nameLength, extraLength))
            val descriptor = record.flags and 8 != 0
            require(local.u32(14) == record.crc || descriptor && local.u32(14) == 0L)
            require(local.u32(18) == record.compressed || descriptor && local.u32(18) == 0L)
            require(local.u32(22) == record.expanded || descriptor && local.u32(22) == 0L)
            record.dataOffset = Math.addExact(record.localOffset, 30L + nameLength + extraLength)
            localEnd = Math.addExact(record.dataOffset, record.compressed)
            require(localEnd <= centralStart)
            if (descriptor) {
                val first = bytes(localEnd, 4).getInt(0)
                val signed = first == 0x08074b50
                val values = bytes(localEnd + if (signed) 4 else 0, 12)
                require(values.u32(0) == record.crc && values.u32(4) == record.compressed && values.u32(8) == record.expanded) { "ZIP data descriptor differs" }
                localEnd += if (signed) 16 else 12
            }
            require(localEnd <= centralStart)
        }
        require(localEnd == centralStart) { "Unaccounted ZIP local bytes" }
        records.forEach { requireCompleteDeflate(channel, it, checkpoint) }
        records
    }

    /** ZipFile deliberately tolerates bytes after an inflater's end marker. Verify the raw
     * compressed range too, so its declared boundary cannot conceal an extra payload.
     * Memory stays at two 64KiB buffers regardless of archive or expanded entry length.
     */
    private fun requireCompleteDeflate(channel: FileChannel, record: BackupZipRecord, checkpoint: () -> Unit) {
        if (record.method != 8) return
        val inflater = Inflater(true)
        try {
            val compressed = ByteArray(64 * 1024)
            val expanded = ByteArray(64 * 1024)
            val crc = CRC32()
            var position = record.dataOffset
            var remaining = record.compressed
            var length = 0L
            while (!inflater.finished()) {
                checkpoint()
                if (inflater.needsInput()) {
                    require(remaining > 0) { "Truncated raw DEFLATE stream" }
                    val amount = minOf(remaining, compressed.size.toLong()).toInt()
                    val input = ByteBuffer.wrap(compressed, 0, amount)
                    while (input.hasRemaining()) {
                        checkpoint()
                        val count = channel.read(input, position)
                        require(count > 0) { "Truncated compressed ZIP range" }
                        position += count
                    }
                    remaining -= amount
                    inflater.setInput(compressed, 0, amount)
                }
                val count = inflater.inflate(expanded)
                length = Math.addExact(length, count.toLong())
                require(length <= record.expanded && !inflater.needsDictionary()) { "DEFLATE output exceeds its budget or requires a dictionary" }
                if (count > 0) crc.update(expanded, 0, count)
                else require(inflater.finished() || inflater.needsInput()) { "DEFLATE reader made no progress" }
            }
            require(inflater.bytesRead == record.compressed && length == record.expanded && crc.value == record.crc) {
                "DEFLATE boundary, expanded length or CRC differs"
            }
        } catch (invalid: DataFormatException) {
            throw IllegalArgumentException("Invalid raw DEFLATE payload", invalid)
        } finally {
            inflater.end()
        }
    }

    private fun extra(buffer: ByteBuffer) {
        while (buffer.hasRemaining()) {
            require(buffer.remaining() >= 4)
            val type = buffer.short.toInt() and 65535
            val length = buffer.short.toInt() and 65535
            require(length <= buffer.remaining() && type !in setOf(0x0001, 0x7075, 0x6375, 0x9901)) { "Malformed, ZIP64 or ambiguous extra field" }
            buffer.position(buffer.position() + length)
        }
    }
    private fun ByteBuffer.u16(offset: Int) = getShort(offset).toInt() and 65535
    private fun ByteBuffer.u32(offset: Int) = getInt(offset).toLong() and 0xffffffffL
}

/** Streaming JSON grammar plus exact serializer shape. Defaulted/nullable fields remain mandatory,
 * duplicate object/map keys fail, UTF-8 is strict, and no JSON tree or whole-data byte array is built.
 */
@OptIn(ExperimentalSerializationApi::class)
private class StrictBackupJson(private val reader: Reader, private val checkpoint: () -> Unit) {
    private var next = reader.read()
    private var consumed = 0L
    private var arrayItems = 0L
    fun scan(descriptor: SerialDescriptor) { value(descriptor, 0); space(); require(next == -1) { "Trailing JSON content" } }
    private fun take(): Int {
        val result = next
        next = reader.read()
        if (++consumed % 4096L == 0L) checkpoint()
        return result
    }
    private fun space() { while (next == 32 || next == 9 || next == 10 || next == 13) take() }
    private fun expect(char: Char) { space(); require(next == char.code) { "Unexpected JSON token" }; take() }
    private fun value(descriptor: SerialDescriptor, depth: Int) {
        require(depth <= 12) { "JSON nesting exceeded" }
        space()
        if (next == 'n'.code) { require(descriptor.isNullable); literal("null"); return }
        when (descriptor.kind) {
            StructureKind.CLASS, StructureKind.OBJECT -> {
                expect('{'); val keys = HashSet<String>(); space()
                if (next != '}'.code) while (true) {
                    val key = string(true)
                    require(keys.add(key)) { "Duplicate JSON field" }
                    val index = descriptor.getElementIndex(key)
                    require(index >= 0) { "Unknown JSON field" }
                    expect(':'); value(descriptor.getElementDescriptor(index), depth + 1); space()
                    if (next != ','.code) break
                    take()
                }
                expect('}'); require(keys.size == descriptor.elementsCount) { "Missing mandatory field, including explicit null" }
            }
            StructureKind.MAP -> {
                expect('{'); val keys = HashSet<String>(); space()
                if (next != '}'.code) while (true) {
                    require(keys.add(string(true)) && keys.size < BackupLimits.MAX_ENTRIES) { "Duplicate or excessive JSON map key" }
                    expect(':'); value(descriptor.getElementDescriptor(1), depth + 1); space()
                    if (next != ','.code) break
                    take()
                }
                expect('}')
            }
            StructureKind.LIST -> {
                expect('['); space()
                if (next != ']'.code) while (true) {
                    arrayItems = Math.addExact(arrayItems, 1L)
                    require(arrayItems < BackupLimits.MAX_RECORDS) { "JSON record budget reached" }
                    value(descriptor.getElementDescriptor(0), depth + 1); space()
                    if (next != ','.code) break
                    take()
                }
                expect(']')
            }
            PrimitiveKind.STRING -> string(false)
            PrimitiveKind.BOOLEAN -> if (next == 't'.code) literal("true") else literal("false")
            PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.LONG -> {
                val token = StringBuilder()
                while (next != -1 && next.toChar() in "-0123456789.eE+") {
                    require(token.length < 32); token.append(take().toChar())
                }
                require(Regex("-?(0|[1-9][0-9]*)").matches(token)) { "An integer JSON value is required" }
                token.toString().toLong()
            }
            else -> throw IllegalArgumentException("Unsupported JSON value shape")
        }
    }
    private fun literal(text: String) { text.forEach { require(take() == it.code) { "Invalid JSON literal" } } }
    private fun string(collect: Boolean): String {
        expect('"')
        val output = if (collect) StringBuilder() else null
        var high = false
        while (next != '"'.code) {
            require(next >= 32) { "Unterminated or unescaped JSON string" }
            var unit = take()
            if (unit == '\\'.code) {
                unit = when (val escaped = take()) {
                    '"'.code, '\\'.code, '/'.code -> escaped
                    'b'.code -> 8; 'f'.code -> 12; 'n'.code -> 10; 'r'.code -> 13; 't'.code -> 9
                    'u'.code -> {
                        var code = 0
                        repeat(4) { val digit = take().toChar().digitToIntOrNull(16); require(digit != null); code = code * 16 + digit }
                        code
                    }
                    else -> throw IllegalArgumentException("Invalid JSON escape")
                }
            }
            val char = unit.toChar()
            if (high) { require(char.isLowSurrogate()) { "Unpaired JSON surrogate" }; high = false }
            else if (char.isHighSurrogate()) high = true
            else require(!char.isLowSurrogate()) { "Unpaired JSON surrogate" }
            if (output != null) { require(output.length < 1024) { "JSON field name too long" }; output.append(char) }
        }
        require(!high); take()
        return output?.toString().orEmpty()
    }
}
