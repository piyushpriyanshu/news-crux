package app.newsbreak;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;

/* loaded from: classes.dex */
public class ShareProvider extends ContentProvider {
    static final String AUTH = "app.newscrux.share";

    static File dir(Context context) {
        File file = new File(context.getCacheDir(), "share");
        file.mkdirs();
        return file;
    }

    static Uri uri(File file) {
        return Uri.parse("content://" + AUTH + "/" + file.getName());
    }

    File file(Uri uri) {
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null || lastPathSegment.contains("/") || lastPathSegment.contains("..") || !(lastPathSegment.endsWith(".png") || lastPathSegment.endsWith(".apk"))) {
            return null;
        }
        File file = new File(lastPathSegment.endsWith(".apk") ? Updater.dir(getContext()) : dir(getContext()), lastPathSegment);
        if (file.exists()) {
            return file;
        }
        return null;
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        return true;
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        String n = uri.getLastPathSegment();
        return n != null && n.endsWith(".apk") ? "application/vnd.android.package-archive" : "image/png";
    }

    @Override // android.content.ContentProvider
    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        File file = file(uri);
        if (file == null) {
            throw new FileNotFoundException(String.valueOf(uri));
        }
        return ParcelFileDescriptor.open(file, 268435456);
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        File file = file(uri);
        if (file == null) {
            return null;
        }
        if (strArr == null) {
            strArr = new String[]{"_display_name", "_size"};
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        Object[] objArr = new Object[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            if ("_display_name".equals(strArr[i])) {
                objArr[i] = file.getName();
            } else if ("_size".equals(strArr[i])) {
                objArr[i] = Long.valueOf(file.length());
            }
        }
        matrixCursor.addRow(objArr);
        return matrixCursor;
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String str, String[] strArr) {
        return 0;
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        return 0;
    }
}
