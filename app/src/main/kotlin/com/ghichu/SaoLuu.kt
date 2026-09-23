package com.ghichu

import android.content.Context
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Sao lưu & khôi phục TOÀN BỘ dữ liệu ra / từ một tệp `.zip`.
 *
 * Đồng bộ Supabase chỉ giữ phần CHỮ của ghi chú và lịch trình; ảnh, ghi âm là
 * tệp nằm ngoài CSDL nên không lên máy chủ. Bản sao lưu này gói đủ cả: cơ sở dữ
 * liệu `ghichu.db` (ghi chú, sự kiện, thư mục, hội thoại AI) + thư mục `anh` +
 * thư mục `ghiam`. Một tệp mang đi máy khác, hoặc cất giữ phòng khi mất máy.
 *
 * Khôi phục **thay thế** toàn bộ dữ liệu hiện tại — nên [SaoLuuActivity] hỏi xác
 * nhận trước, và khởi động lại app sau khi thay để mở lại CSDL sạch.
 */
object SaoLuu {

    private const val DUONG_DB = "db/ghichu.db"
    private const val THU_MUC_ANH = "anh"
    private const val THU_MUC_GHIAM = "ghiam"

    // ------------------------------------------------------------ Xuất

    /**
     * Gói toàn bộ dữ liệu vào [ra]. Gọi ở LUỒNG NỀN (đọc/ghi tệp lớn).
     *
     * Gộp WAL vào tệp chính trước khi chép: SQLite mặc định ghi trước vào tệp
     * `-wal`, không checkpoint thì bản chép `.db` thiếu phần vừa sửa.
     */
    fun xuatVao(context: Context, ra: OutputStream) {
        try {
            Ung.cua(context).kho.writableDatabase
                .rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
        } catch (_: Exception) {
            // Checkpoint hỏng thì vẫn chép tiếp — cùng lắm thiếu vài sửa đổi cuối.
        }

        ZipOutputStream(ra.buffered()).use { zip ->
            themTep(zip, DUONG_DB, context.getDatabasePath("ghichu.db"))
            themThuMuc(zip, THU_MUC_ANH, KhoAnh.thuMuc(context))
            themThuMuc(zip, THU_MUC_GHIAM, KhoGhiAm.thuMuc(context))
        }
    }

    private fun themTep(zip: ZipOutputStream, ten: String, f: File) {
        if (!f.exists() || !f.isFile) return
        zip.putNextEntry(ZipEntry(ten))
        f.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun themThuMuc(zip: ZipOutputStream, tien: String, dir: File) {
        val ds = dir.listFiles() ?: return
        for (f in ds) if (f.isFile) themTep(zip, "$tien/${f.name}", f)
    }

    // ------------------------------------------------------------ Nhập

    /**
     * Khôi phục từ [vao]. Gọi ở LUỒNG NỀN. **Thay thế** dữ liệu hiện tại.
     *
     * Sau khi hàm này chạy xong, PHẢI khởi động lại app: CSDL đã đóng và tệp bị
     * thay dưới chân, dùng tiếp bằng kết nối cũ là hỏng.
     */
    fun nhapTu(context: Context, vao: InputStream) {
        val tam = File(context.cacheDir, "khoiphuc_tam").apply {
            deleteRecursively(); mkdirs()
        }
        try {
            ZipInputStream(vao.buffered()).use { giaiNen(it, tam) }

            val dbMoi = File(tam, DUONG_DB)
            if (!dbMoi.exists()) {
                throw IOException("Tệp không phải bản sao lưu Ghi chú (thiếu cơ sở dữ liệu).")
            }

            // Đóng CSDL đang mở trước khi thay tệp dưới chân nó.
            Ung.cua(context).kho.close()

            val dbCu = context.getDatabasePath("ghichu.db")
            dbCu.parentFile?.mkdirs()
            dbMoi.copyTo(dbCu, overwrite = true)
            // Bỏ WAL/SHM cũ: chúng thuộc CSDL vừa bị thay, giữ lại là lẫn dữ liệu.
            File(dbCu.path + "-wal").delete()
            File(dbCu.path + "-shm").delete()

            thayThuMuc(File(tam, THU_MUC_ANH), KhoAnh.thuMuc(context))
            thayThuMuc(File(tam, THU_MUC_GHIAM), KhoGhiAm.thuMuc(context))
        } finally {
            tam.deleteRecursively()
        }
    }

    private fun giaiNen(zip: ZipInputStream, dich: File) {
        val goc = dich.canonicalPath
        var e: ZipEntry? = zip.nextEntry
        while (e != null) {
            val ra = File(dich, e.name)
            // Chặn zip-slip: đường dẫn giải nén phải nằm TRONG thư mục đích.
            if (!ra.canonicalPath.startsWith(goc + File.separator)) {
                throw IOException("Tệp sao lưu có đường dẫn bất thường.")
            }
            if (e.isDirectory) {
                ra.mkdirs()
            } else {
                ra.parentFile?.mkdirs()
                ra.outputStream().use { zip.copyTo(it) }
            }
            zip.closeEntry()
            e = zip.nextEntry
        }
    }

    /** Xoá sạch thư mục đích rồi chép [nguon] vào — nguồn trống thì chỉ xoá. */
    private fun thayThuMuc(nguon: File, dich: File) {
        dich.deleteRecursively()
        dich.mkdirs()
        val ds = nguon.listFiles() ?: return
        for (f in ds) if (f.isFile) f.copyTo(File(dich, f.name), overwrite = true)
    }
}
