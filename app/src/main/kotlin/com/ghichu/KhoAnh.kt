package com.ghichu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import java.io.File

/**
 * Anh gan trong ghi chu.
 *
 * Anh nam trong thu muc rieng cua app (`files/anh/`), ghi chu chi giu TEN TEP:
 * `<img src="a1.jpg" />`. Khong giu `content://` cua may anh vi quyen doc uri do
 * het han khi khoi dong lai may — luu uri la vai ngay sau mo ghi chu ra chi con
 * o trong.
 *
 * **Anh KHONG di theo duong dong bo.** Goi gui len may chu chi co phan chu; may
 * khac keo ve se thay o cho anh mot khung "Ảnh không có trên máy này". Day la
 * gioi han co y — day anh len la chuyen khac han ve dung luong va rieng tu, de
 * cho mot chang rieng khi nao ban can.
 */
object KhoAnh {

    /**
     * Canh dai nhat sau khi thu nho.
     *
     * Anh may anh gio 4000px, giu nguyen thi mot ghi chu vai tam la vai chuc MB
     * va cuon giat. 1600 la du net khi phong to xem tren dien thoai.
     */
    private const val CANH_TOI_DA = 1600

    private const val CHAT_LUONG = 88

    fun thuMuc(context: Context): File =
        File(context.filesDir, "anh").apply { if (!exists()) mkdirs() }

    fun tep(context: Context, ten: String): File = File(thuMuc(context), ten)

    /**
     * Chep mot anh nguoi dung chon vao trong app, thu nho lai.
     *
     * Tra ve TEN TEP de nhet vao markup, hoac null neu doc khong duoc.
     */
    fun them(context: Context, nguon: Uri): String? {
        val goc = doc(context, nguon) ?: return null
        val nho = thuNho(goc)
        if (nho !== goc) goc.recycle()

        val ten = "anh_" + Uid.moi() + ".jpg"
        return try {
            tep(context, ten).outputStream().use {
                nho.compress(Bitmap.CompressFormat.JPEG, CHAT_LUONG, it)
            }
            ten
        } catch (_: Exception) {
            null
        } finally {
            nho.recycle()
        }
    }

    private fun doc(context: Context, nguon: Uri): Bitmap? = try {
        context.contentResolver.openInputStream(nguon)?.use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) {
        null
    }

    private fun thuNho(b: Bitmap): Bitmap {
        val canh = maxOf(b.width, b.height)
        if (canh <= CANH_TOI_DA) return b
        val ti = CANH_TOI_DA.toFloat() / canh
        return Bitmap.createScaledBitmap(b, (b.width * ti).toInt(), (b.height * ti).toInt(), true)
    }

    /**
     * Anh de ve trong ghi chu, da co san kich thuoc theo be ngang [rong].
     *
     * Tep khong con thi tra ve khung bao thieu ([khungThieu]) chu khong tra null:
     * ghi chu keo tu may khac ve luon roi vao truong hop nay, de trong thi nguoi
     * dung tuong ghi chu bi mat mot doan.
     */
    fun anhVe(context: Context, ten: String, rong: Int, mau: BangMau): Drawable {
        val f = tep(context, ten)
        if (!f.exists()) return khungThieu(context, rong, mau)
        val bm = BitmapFactory.decodeFile(f.absolutePath) ?: return khungThieu(context, rong, mau)

        val cao = (bm.height.toFloat() / bm.width * rong).toInt().coerceAtLeast(1)
        return BitmapDrawable(context.resources, bm).apply { setBounds(0, 0, rong, cao) }
    }

    /** Khung xam bao "anh khong co tren may nay". */
    private fun khungThieu(context: Context, rong: Int, mau: BangMau): Drawable {
        val cao = (rong * 0.4f).toInt().coerceAtLeast(1)
        val bm = Bitmap.createBitmap(rong, cao, Bitmap.Config.ARGB_8888)
        val c = Canvas(bm)
        val son = Paint(Paint.ANTI_ALIAS_FLAG)

        son.color = if (mau.toi) Color.argb(30, 255, 255, 255) else Color.argb(24, 0, 0, 0)
        c.drawRect(0f, 0f, rong.toFloat(), cao.toFloat(), son)

        son.color = mau.chuMo
        son.textSize = rong * 0.055f
        son.textAlign = Paint.Align.CENTER
        c.drawText("Ảnh không có trên máy này", rong / 2f, cao / 2f + son.textSize / 3, son)

        return BitmapDrawable(context.resources, bm).apply { setBounds(0, 0, rong, cao) }
    }

    /**
     * Xoa nhung tep anh khong con ghi chu nao nhac toi.
     *
     * Goi luc mo app. Khong co buoc nay thi xoa ghi chu co anh xong, tep van nam
     * lai mai — vai thang sau app chiem vai tram MB ma nguoi dung khong hieu vi sao.
     */
    fun donTepThua(context: Context, kho: KhoDuLieu) {
        val dung = HashSet<String>()
        val THE_ANH = Regex("<img[^>]*src=\"([^\"]+)\"")
        kho.readableDatabase.query(
            KhoDuLieu.BANG_GHI_CHU, arrayOf("noi_dung"), null, null, null, null, null,
        ).use { c ->
            while (c.moveToNext()) {
                for (m in THE_ANH.findAll(c.getString(0).orEmpty())) dung += m.groupValues[1]
            }
        }
        thuMuc(context).listFiles()?.forEach { if (it.name !in dung) it.delete() }
    }
}
