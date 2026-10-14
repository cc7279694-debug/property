package com.cooper.lithic.data.media;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** The isolated test-APK process has no target-APK Kotlin classes. Android/Java only. */
public final class M10ImageSourceProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) {
        String mime = uri.getQueryParameter("mime");
        return mime == null ? "image/png" : mime;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        int access;
        if ("r".equals(mode)) access = ParcelFileDescriptor.MODE_READ_ONLY;
        else if ("w".equals(mode) || "wt".equals(mode)) {
            access = ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE;
        } else if ("rw".equals(mode)) access = ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE;
        else throw new FileNotFoundException("Invalid synthetic source access");
        return ParcelFileDescriptor.open(sourceFile(uri), access);
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            File file = sourceFile(uri);
            MatrixCursor cursor = new MatrixCursor(new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
            cursor.addRow(new Object[] {file.getName(), file.length()});
            return cursor;
        } catch (FileNotFoundException invalid) { throw new IllegalArgumentException("Invalid synthetic source"); }
    }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        try { return sourceFile(uri).delete() ? 1 : 0; }
        catch (FileNotFoundException invalid) { return 0; }
    }
    private File sourceFile(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || !name.matches("[a-f0-9-]{36}\\.bin")) throw new FileNotFoundException("Invalid synthetic source");
        if (getContext() == null) throw new FileNotFoundException("Synthetic provider is not attached");
        File root = new File(getContext().getCacheDir(), "m10-synthetic-image-sources");
        if (!root.isDirectory() && !root.mkdirs()) throw new FileNotFoundException("Cannot create synthetic source directory");
        return new File(root, name);
    }
}
