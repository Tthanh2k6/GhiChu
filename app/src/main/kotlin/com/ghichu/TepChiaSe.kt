package com.ghichu

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File

/**
 * Dua tep trong app ra cho app khac doc — de "Chia se" gui duoc anh va PDF.
 *
 * Vi sao tu viet chu khong dung `FileProvider`: lop do nam trong `androidx.core`,
 * ma ca app co y **khong keo AndroidX vao** (xem `app/build.gradle.kts`). Phan
 * viec that su can chi la ba ham duoi day.
 *
 * AN TOAN — chi phuc vu tep trong **mot thu muc duy nhat** (`cache/chiase`), va
 * duong dan bi chuan hoa truoc khi so sanh. Khong co buoc chuan hoa do thi mot
 * uri kieu `../../databases/ghichu.db` la app khac doc duoc ca CSDL, ke ca ghi
 * chu rieng tu.
 */
class TepChiaSe : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val ctx = context ?: return null
        val f = tepCua(ctx, uri) ?: throw SecurityException("Không được phép đọc: $uri")
        // Chi cho DOC. App nhan chia se khong co ly do gi phai ghi nguoc lai.
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        cot: Array<out String>?,
        loc: String?,
        thamSo: Array<out String>?,
        sapXep: String?,
    ): Cursor? {
        val ctx = context ?: return null
        val f = tepCua(ctx, uri) ?: return null

        // Nhieu app chi hien duoc ten tep khi hoi hai cot nay; thieu la ho hien
        // mot cai ten may sinh trong nhu rac.
        val ten = cot ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val hang = arrayOfNulls<Any>(ten.size)
        for (i in ten.indices) {
            hang[i] = when (ten[i]) {
                OpenableColumns.DISPLAY_NAME -> f.name
                OpenableColumns.SIZE -> f.length()
                else -> null
            }
        }
        return MatrixCursor(ten, 1).apply { addRow(hang) }
    }

    override fun getType(uri: Uri): String = when {
        uri.path.orEmpty().endsWith(".pdf") -> "application/pdf"
        uri.path.orEmpty().endsWith(".png") -> "image/png"
        else -> "text/plain"
    }

    override fun insert(uri: Uri, gt: ContentValues?): Uri? = null
    override fun delete(uri: Uri, loc: String?, thamSo: Array<out String>?): Int = 0
    override fun update(uri: Uri, gt: ContentValues?, loc: String?, ts: Array<out String>?): Int = 0

    /** Tep ma uri tro toi, hoac null neu no nam ngoai thu muc duoc phep. */
    private fun tepCua(ctx: Context, uri: Uri): File? {
        val ten = uri.lastPathSegment ?: return null
        val goc = thuMuc(ctx).canonicalFile
        val f = File(goc, ten).canonicalFile
        // So sanh tren duong dan DA CHUAN HOA — day la cho chan `../`.
        if (f.parentFile != goc || !f.exists()) return null
        return f
    }

    companion object {
        const val QUYEN = "com.ghichu.tep"

        fun thuMuc(context: Context): File =
            File(context.cacheDir, "chiase").apply { if (!exists()) mkdirs() }

        /**
         * Uri tro toi mot tep trong thu muc chia se.
         *
         * Dung `appendPath` chu KHONG ghep chuoi: ten tep lay tu tieu de ghi chu
         * nen day dau tieng Viet va dau cach — ghep thang vao `Uri.parse` la ra
         * uri sai, app nhan chia se bao khong mo duoc tep.
         */
        fun uriCua(context: Context, f: File): Uri {
            thuMuc(context)
            return Uri.Builder()
                .scheme("content")
                .authority(QUYEN)
                .appendPath(f.name)
                .build()
        }

        /**
         * Xoa tep chia se cu.
         *
         * Day la thu muc cache nen he thong co the tu don, nhung khong chac va
         * khong biet luc nao — xuat vai chuc ban PDF thi no nam do mai.
         */
        fun don(context: Context, quaMs: Long = 24L * 60 * 60 * 1000) {
            val moc = System.currentTimeMillis() - quaMs
            thuMuc(context).listFiles()?.forEach { if (it.lastModified() < moc) it.delete() }
        }
    }
}
