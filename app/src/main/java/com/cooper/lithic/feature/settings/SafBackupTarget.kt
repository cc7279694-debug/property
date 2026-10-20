package com.cooper.lithic.feature.settings

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.cooper.lithic.data.backup.BackupTarget
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Only construct for a URI returned by this attempt's ACTION_CREATE_DOCUMENT callback. */
internal class SafBackupTarget(context: Context, private val createdDocument: Uri) : BackupTarget {
    private val resolver: ContentResolver = context.applicationContext.contentResolver
    private val monitor = Any()
    private var writeAttempted = false
    private var deleted = false

    init {
        require(createdDocument.scheme == ContentResolver.SCHEME_CONTENT && DocumentsContract.isDocumentUri(context, createdDocument)) {
            "Backup target must be a system-created document"
        }
    }

    override fun openWrite(): OutputStream = synchronized(monitor) {
        check(!writeAttempted && !deleted) { "Backup target already used" }
        writeAttempted = true
        resolver.openOutputStream(createdDocument, "wt") ?: throw IOException("Selected document cannot be opened for writing")
    }

    override fun openRead(): InputStream = synchronized(monitor) {
        check(writeAttempted && !deleted) { "Backup target is not available for verification" }
        resolver.openInputStream(createdDocument) ?: throw IOException("Selected document cannot be opened for verification")
    }

    override fun deletePartial(): Boolean = synchronized(monitor) {
        if (deleted) true
        else DocumentsContract.deleteDocument(resolver, createdDocument).also { if (it) deleted = true }
    }
}
