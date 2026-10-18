package com.cooper.lithic.data.media

import android.system.Os
import android.system.OsConstants
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.UUID
import org.json.JSONObject
import com.cooper.lithic.data.media.jpegstage.JpegStageV2
import java.security.MessageDigest

/** Ownership receipts identify interrupted files; Room is the committed media truth. */

internal class MediaFileProtocol(private val supplied: File) {
    val generation: File = supplied.canonicalFile
    val media: File get() {
        if (Files.isSymbolicLink(supplied.toPath()) || !generation.isDirectory) path("The current private dataset is unavailable")
        return child(generation, "media").also {
            if (!it.isDirectory) path("The private media directory is unavailable")
        }
    }
    private val preparations: File get() = child(media, ".m10-imports")
    private val jpegPreparations: File get() = child(media, ".m10-jpeg-v2")
    val hasJpegPreparations: Boolean get() = jpegPreparations.exists()
    val jpeg by lazy { JpegStageV2(jpegPreparations, generation.name, ::identity, ::syncDirectory) }
    private val ownerKeys = setOf("protocol", "version", "assetId", "generation")
    private val assetKeys = setOf("id", "relativePath", "mime", "width", "height", "byteLength", "sha256")
    private val knownNames = setOf("owner.json", "asset.json", "owned-final.json", "source.bin", "image.jpg", "image.png", ".encoding")
    private val finalOwnerKeys = setOf("protocol", "version", "assetId", "generation", "device", "inode")

    fun requireId(id: String) {
        if (id.length != 36 || runCatching {
                UUID.fromString(id).toString()
            }.getOrNull() != id) path("The image identity is invalid")
    }

    fun finalFile(asset: LocalMediaAssetEntity): File {
        requireId(asset.id)
        val suffix = when (asset.mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            else -> path("The stored media type is unsupported")
        }
        if (asset.relativePath != "media/${asset.id}.$suffix") path("The stored media path is not a managed private cover")
        return child(media, "${asset.id}.$suffix")
    }

    fun stage(id: String): File {
        requireId(id)
        val old = child(preparations, id)
        val strict = child(jpegPreparations, id)
        if (old.exists() && strict.exists()) path("Conflicting private image preparations must be preserved")
        return if (strict.exists()) strict else old
    }

    fun isJpegStage(id: String): Boolean { requireId(id); return child(jpegPreparations, id).exists() }

    /** Open and hash the same non-linked inode, including a post-read path/size check. */
    fun identity(file: File): JpegStageV2.FileIdentity {
        val empty = Os.lstat(file.path)
        if (OsConstants.S_ISREG(empty.st_mode) && empty.st_size == 0L) {
            // POSIX process locks can be released when ANY descriptor for the same
            // inode is closed. Never open/close a guard while its FileLock is held.
            val checked = Os.lstat(file.path)
            if (!OsConstants.S_ISREG(checked.st_mode) || checked.st_size != 0L ||
                checked.st_dev != empty.st_dev || checked.st_ino != empty.st_ino) path("Private empty JPEG inode changed")
            return JpegStageV2.FileIdentity(empty.st_dev, empty.st_ino, 0L,
                MessageDigest.getInstance("SHA-256").digest().joinToString("") { "%02x".format(it.toInt() and 255) })
        }
        val fd = Os.open(file.path, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or MediaNativeFlags.CLOSE_ON_EXEC, 0)
        try {
            val before = Os.fstat(fd)
            if (!OsConstants.S_ISREG(before.st_mode) || before.st_size !in 0..AndroidImageCodec.SOURCE_LIMIT) path("Invalid private JPEG file identity")
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            var count = 0L
            while (true) {
                val read = Os.read(fd, buffer, 0, buffer.size)
                if (read == 0) break
                if (count > AndroidImageCodec.SOURCE_LIMIT - read) path("Private JPEG changed beyond its limit")
                count += read
                digest.update(buffer, 0, read)
            }
            val after = Os.fstat(fd)
            val named = Os.lstat(file.path)
            if (count != before.st_size || after.st_size != before.st_size ||
                after.st_dev != before.st_dev || after.st_ino != before.st_ino ||
                named.st_dev != before.st_dev || named.st_ino != before.st_ino || !OsConstants.S_ISREG(named.st_mode)) path("Private JPEG identity changed")
            return JpegStageV2.FileIdentity(before.st_dev, before.st_ino, count,
                digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) })
        } finally { Os.close(fd) }
    }

    fun child(parent: File, name: String): File {
        if (name.isBlank() || name.contains('/') || name.contains('\\') || name == "." || name == "..") path("The private media path is invalid")
        if (Files.isSymbolicLink(parent.toPath()) || parent.canonicalFile != parent.absoluteFile) path("A linked private media directory cannot be used")
        val result = File(parent, name)
        if (Files.isSymbolicLink(result.toPath()) || result.canonicalFile.parentFile != parent) path("A linked or escaping private media path cannot be used")
        return result
    }

    fun createStage(id: String): File {
        if (isJpegStage(id)) path("A strict JPEG preparation cannot be reused as v1")
        val area = preparations
        if (!area.isDirectory && !area.mkdir()) throw IOException("Cannot create the private image preparation area")
        val directory = stage(id)
        if (directory.exists()) {
            if (!hasOwner(directory, id)) path("An unknown image preparation must be preserved")
            return directory
        }
        if (!directory.mkdir()) throw IOException("Cannot create the private image preparation")
        writeNewJson(child(directory, "owner.json"), JSONObject().put("protocol", "lithic-m10-import").put("version", 1).put("assetId", id).put("generation", generation.name))
        syncDirectory(directory)
        syncDirectory(area)
        return directory
    }

    fun hasOwner(directory: File, id: String): Boolean {
        if (!directory.isDirectory) return false
        if (isJpegStage(id)) return runCatching {
            val owner = jpeg.ownerFor(id)
            jpeg.snapshot(owner)
            directory == child(jpegPreparations, id)
        }.getOrDefault(false)
        return try {
            val owner = readJson(child(directory, "owner.json"))
            owner.keys().asSequence().toSet() == ownerKeys && owner.getString("protocol") == "lithic-m10-import" && owner.getInt("version") == 1 && owner.getString("assetId") == id && owner.getString("generation") == generation.name
        } catch (_: IOException) {
            false
        } catch (_: org.json.JSONException) {
            false
        }
    }

    fun requireOwner(id: String): File {
        val directory = stage(id)
        if (!directory.exists()) throw ImageStoreException(MediaFailureKind.NOT_FOUND, "The prepared image is no longer available; select it again")
        if (!hasOwner(directory, id)) path("An unknown image preparation cannot be read or removed")
        return directory
    }

    fun saveAsset(directory: File, asset: LocalMediaAssetEntity) {
        finalFile(asset)
        if (isJpegStage(asset.id)) requireStrictAsset(asset)
        writeNewJson(child(directory, "asset.json"), JSONObject().put("id", asset.id).put("relativePath", asset.relativePath).put("mime", asset.mime)
            .put("width", asset.width).put("height", asset.height).put("byteLength", asset.byteLength).put("sha256", asset.sha256))
        syncDirectory(directory)
    }

    fun readAsset(id: String): LocalMediaAssetEntity {
        val json = readJson(child(requireOwner(id), "asset.json"))
        if (json.keys().asSequence().toSet() != assetKeys) path("The prepared image record is incomplete")
        val asset = try {
            LocalMediaAssetEntity(json.getString("id"), json.getString("relativePath"), json.getString("mime"), json.getInt("width"), json.getInt("height"), json.getLong("byteLength"), json.getString("sha256"))
        }
        catch (_: IllegalArgumentException) {
            path("The prepared image metadata is invalid")
        }
        if (asset.id != id || asset.sha256 != asset.sha256.lowercase() || asset.byteLength <= 0 || asset.byteLength>AndroidImageCodec.STORED_LIMIT || maxOf(asset.width, asset.height)>AndroidImageCodec.EDGE_LIMIT) path("The prepared image metadata is invalid")
        finalFile(asset)
        if (isJpegStage(id)) requireStrictAsset(asset)
        return asset
    }

    private fun requireStrictAsset(asset: LocalMediaAssetEntity) {
        val prepared = jpeg.verifyPrepared(jpeg.ownerFor(asset.id))
        if (asset.mime != "image/jpeg" || prepared.output.bytes != asset.byteLength || prepared.output.sha256 != asset.sha256 ||
            prepared.width != asset.width || prepared.height != asset.height) path("Strict JPEG receipt and metadata differ")
    }

    fun preparedFile(asset: LocalMediaAssetEntity): File = child(requireOwner(asset.id), if (asset.mime == "image/png")"image.png" else "image.jpg")

    fun candidates(): List<LocalMediaAssetEntity> {
        val area = preparations
        if (area.exists() && !area.isDirectory)path("The private preparation area is not a directory")
        val children = if (area.exists()) area.listFiles() ?: throw IOException("Cannot inspect the private preparation area") else emptyArray()
        val old = children.sortedBy {
            it.name
        }.mapNotNull { directory ->
            try {
                requireId(directory.name)
                val safe = stage(directory.name)
                if (hasOwner(safe, directory.name)) readAsset(directory.name).takeIf {
                    it.mime != "image/jpeg"
                } else null
            }
            catch (_: IOException) {
                null
            } catch (_: org.json.JSONException) {
                null
            }
        }
        val strict = if (jpegPreparations.isDirectory) (jpegPreparations.listFiles()
            ?: throw IOException("Cannot inspect strict JPEG preparations")).mapNotNull { directory ->
            runCatching {
                requireId(directory.name)
                val owner = jpeg.ownerFor(directory.name)
                jpeg.verifyPrepared(owner)
                readAsset(directory.name)
            }.getOrNull()
        } else emptyList()
        return old + strict
    }

    fun incompleteOwnedIds(): List<String> {
        val area = preparations
        if (!area.isDirectory)return emptyList()
        return (area.listFiles() ?: throw IOException("Cannot inspect image preparations")).mapNotNull {
            try {
                requireId(it.name)
                val directory = stage(it.name)
                if (hasOwner(directory, it.name) && !child(directory, "asset.json").exists())it.name else null
            }
            catch (_: IOException) {
                null
            }
        }
    }

    fun recordOwnedFinal(asset: LocalMediaAssetEntity, device: Long, inode: Long) {
        val directory = requireOwner(asset.id)
        writeNewJson(
            child(directory, "owned-final.json"),
            JSONObject()
                .put("protocol", "lithic-m10-owned-final")
                .put("version", 1)
                .put("assetId", asset.id)
                .put("generation", generation.name)
                .put("device", device)
                .put("inode", inode),
        )
        syncDirectory(directory)
    }

    fun finalIsOwned(asset: LocalMediaAssetEntity): Boolean {
        val target = finalFile(asset)
        if (!target.exists()) return false
        return try {
            val json = readJson(child(requireOwner(asset.id), "owned-final.json"))
            if (json.keys().asSequence().toSet() != finalOwnerKeys ||
                json.getString("protocol") != "lithic-m10-owned-final" ||
                json.getInt("version") != 1 ||
                json.getString("assetId") != asset.id ||
                json.getString("generation") != generation.name
            ) return false
            val stat = Os.lstat(target.path)
            stat.st_dev == json.getLong("device") && stat.st_ino == json.getLong("inode")
        } catch (_: IOException) {
            false
        } catch (_: org.json.JSONException) {
            false
        }
    }

    fun deleteKnownStage(id: String): Boolean {
        if (isJpegStage(id)) {
            val owner = jpeg.ownerFor(id)
            jpeg.revoke(owner, "HOST_DISCARD")
            jpeg.safeCleanup(owner)
            return true // Keep terminal proof/tombstone; never apply the v1 deletion loop.
        }
        val directory = requireOwner(id)
        // Unknown children are preserved even when the operation receipt itself is valid.
        for (name in knownNames.filter {
                it != "owner.json"
            }) {
            val file = child(directory, name)
            if (file.exists() && (!file.isFile || !file.delete()))throw IOException("Cannot remove a private image preparation file")
        }
        val owner = child(directory, "owner.json")
        if (owner.exists() && !owner.delete())throw IOException("Cannot remove the image ownership record")
        if (directory.listFiles()?.isEmpty() == true && !directory.delete())throw IOException("Cannot remove the empty image preparation")
        syncDirectory(preparations)
        return true
    }

    fun syncDirectory(directory: File) {
        if (Files.isSymbolicLink(directory.toPath()) || directory.canonicalFile != directory.absoluteFile) {
            path("A linked private directory cannot be synchronized")
        }
        val descriptor = Os.open(directory.path, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW, 0)
        try {
            if (!OsConstants.S_ISDIR(Os.fstat(descriptor).st_mode)) {
                path("The private media synchronization target is not a directory")
            }
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
    }

    private fun writeNewJson(file: File, json: JSONObject) {
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        if (bytes.size>4096)throw IOException("The private ownership record exceeds its limit")
        FileChannel.open(file.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use {
            channel -> val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining())channel.write(buffer)
            channel.force(true)
        }
    }

    private fun readJson(file: File): JSONObject {
        if (!file.isFile)throw ImageStoreException(MediaFailureKind.NOT_FOUND, "The prepared image record is missing")
        if (file.length() !in 1..4096)path("The private image record exceeds its limit")
        val bytes = file.inputStream().use {
            input -> val output = java.io.ByteArrayOutputStream()
            val block = ByteArray(1024)
            while (output.size() <= 4096) {
                val count = input.read(block)
                if (count<0)break
                output.write(block, 0, count)
            }
            output.toByteArray()
        }
        if (bytes.size>4096)path("The private image record exceeds its limit")
        return JSONObject(String(bytes, Charsets.UTF_8))
    }

    private fun path(message: String): Nothing = throw ImageStoreException(MediaFailureKind.PATH, message)
}
