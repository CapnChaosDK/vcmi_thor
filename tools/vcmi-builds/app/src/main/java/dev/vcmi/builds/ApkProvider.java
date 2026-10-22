package dev.vcmi.builds;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Exposes only the validated APK, read-only, under a temporary URI grant. */
public final class ApkProvider extends ContentProvider {
    public boolean onCreate() { return true; }
    private File apk(Uri uri) {
        if ("/build.apk".equals(uri.getPath())) return new File(getContext().getFilesDir(), "build.apk");
        java.util.List<String> segments=uri.getPathSegments();
        if (segments.size()==3 && segments.get(0).equals("candidates") && segments.get(2).equals("build.apk")) {
            try { return new CandidateStore(getContext()).apk(segments.get(1)); }
            catch (java.io.IOException e) { throw new IllegalArgumentException("Unknown candidate"); }
        }
        throw new IllegalArgumentException("Unknown file");
    }
    public String getType(Uri uri) { apk(uri); return "application/vnd.android.package-archive"; }
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(apk(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        File file = apk(uri);
        String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
        MatrixCursor cursor = new MatrixCursor(columns);
        Object[] row = new Object[columns.length];
        for (int i=0;i<columns.length;i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) row[i]="vcmi-build.apk";
            if (OpenableColumns.SIZE.equals(columns[i])) row[i]=file.length();
        }
        cursor.addRow(row); return cursor;
    }
    public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
