package com.ghichu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File

/**
 * Xuat mot ghi chu ra tep de chia se: van ban, anh PNG, hoac PDF.
 *
 * CACH DUNG CHUNG cho ca ba: dung mot [StaticLayout] tu noi dung da co dinh dang,
 * roi ve no ra dau can. Nho vay chu dam, mau chu, o tick, anh… hien y het nhu
 * trong app — khong phai dung rieng mot bo ve cho tung dinh dang xuat.
 *
 * Anh trong ghi chu: cac `ImageSpan` da gan san trong chuoi thi `StaticLayout`
 * tu ve. Ghi chu chua tung mo ra (chua co span anh) thi cho anh se trong — noi
 * ro o [SuaGhiChuActivity], vi vay chi xuat tu man dang mo.
 */
object XuatGhiChu {

    /** Be ngang trang xuat, tinh bang diem anh. Xap xi kho A4 o 96 dpi. */
    private const val RONG = 1240

    private const val LE = 80

    /** Chieu cao mot trang PDF (A4 o 96 dpi). */
    private const val CAO_TRANG = 1754

    // ------------------------------------------------------------ van ban

    fun vanBan(context: Context, tieuDe: String, than: CharSequence): File {
        val f = tep(context, tieuDe, "txt")
        f.writeText(listOf(tieuDe, than.toString()).filter { it.isNotBlank() }.joinToString("\n\n"))
        return f
    }

    // ------------------------------------------------------------ anh

    fun anh(context: Context, tieuDe: String, than: CharSequence, mau: BangMau): File {
        val (boTieuDe, boThan) = boCuc(tieuDe, than, mau)
        val cao = LE * 2 + (boTieuDe?.height ?: 0) + (if (boTieuDe != null) 30 else 0) + boThan.height

        val bm = Bitmap.createBitmap(RONG, cao.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bm)
        c.drawColor(if (mau.toi) 0xFF121212.toInt() else 0xFFFFFFFF.toInt())
        ve(c, boTieuDe, boThan)

        val f = tep(context, tieuDe, "png")
        f.outputStream().use { bm.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bm.recycle()
        return f
    }

    // ------------------------------------------------------------ pdf

    /**
     * Xuat PDF, cat trang khi noi dung dai.
     *
     * Cat theo **ranh gioi DONG** chu khong cat theo chieu cao co dinh: cat giua
     * dong thi mot hang chu bi xe doi, nua tren o trang nay nua duoi o trang sau.
     */
    fun pdf(context: Context, tieuDe: String, than: CharSequence, mau: BangMau): File {
        // PDF luon nen trang, chu den — khong theo giao dien toi cua app: nguoi ta
        // xuat PDF phan lon la de in hoac gui di.
        val mauIn = BangMau(toi = false)
        val (boTieuDe, boThan) = boCuc(tieuDe, than, mauIn)

        val tai = PdfDocument()
        val caoDau = LE + (boTieuDe?.height ?: 0) + (if (boTieuDe != null) 30 else 0)

        var dong = 0
        var soTrang = 1
        var daVeDau = false

        while (dong < boThan.lineCount || !daVeDau) {
            val trang = tai.startPage(
                PdfDocument.PageInfo.Builder(RONG, CAO_TRANG, soTrang).create(),
            )
            val c = trang.canvas
            c.drawColor(0xFFFFFFFF.toInt())

            var y = LE
            if (!daVeDau) {
                if (boTieuDe != null) {
                    c.save()
                    c.translate(LE.toFloat(), LE.toFloat())
                    boTieuDe.draw(c)
                    c.restore()
                }
                y = caoDau
                daVeDau = true
            }

            // Bao nhieu dong nua thi vua trang nay.
            val conLai = CAO_TRANG - LE - y
            var cuoi = dong
            while (cuoi < boThan.lineCount &&
                boThan.getLineBottom(cuoi) - boThan.getLineTop(dong) <= conLai
            ) {
                cuoi++
            }
            // Mot dong cao hon ca trang (anh to) thi van phai ve, khong thi vong
            // lap dung yen mai o day.
            if (cuoi == dong && dong < boThan.lineCount) cuoi = dong + 1

            if (cuoi > dong) {
                c.save()
                c.translate(LE.toFloat(), (y - boThan.getLineTop(dong)).toFloat())
                c.clipRect(
                    0, boThan.getLineTop(dong), RONG - LE * 2, boThan.getLineBottom(cuoi - 1),
                )
                boThan.draw(c)
                c.restore()
            }
            dong = cuoi

            tai.finishPage(trang)
            soTrang++
        }

        val f = tep(context, tieuDe, "pdf")
        f.outputStream().use { tai.writeTo(it) }
        tai.close()
        return f
    }

    // ------------------------------------------------------------ rieng

    private fun boCuc(
        tieuDe: String,
        than: CharSequence,
        mau: BangMau,
    ): Pair<StaticLayout?, StaticLayout> {
        val rong = RONG - LE * 2

        val sonTieuDe = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            color = mau.chuChinh
            textSize = 46f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val sonThan = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            color = mau.chuChinh
            textSize = 32f
        }

        val boTieuDe = if (tieuDe.isBlank()) null else dung(tieuDe, sonTieuDe, rong)
        return boTieuDe to dung(than, sonThan, rong)
    }

    private fun dung(chu: CharSequence, son: TextPaint, rong: Int): StaticLayout =
        StaticLayout.Builder.obtain(chu, 0, chu.length, son, rong)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(10f, 1f)
            // Giu nguyen dinh dang trong chuoi neu co; chu tron thi khong anh huong.
            .setIncludePad(true)
            .build()

    private fun ve(c: Canvas, boTieuDe: StaticLayout?, boThan: StaticLayout) {
        var y = LE
        if (boTieuDe != null) {
            c.save()
            c.translate(LE.toFloat(), y.toFloat())
            boTieuDe.draw(c)
            c.restore()
            y += boTieuDe.height + 30
        }
        c.save()
        c.translate(LE.toFloat(), y.toFloat())
        boThan.draw(c)
        c.restore()
    }

    /**
     * Tep dich trong thu muc chia se.
     *
     * Ten tep lay tu tieu de ghi chu de nguoi nhan doc duoc, nhung **loc sach**
     * — tieu de la chu nguoi dung go, trong do co the co `/`, `..`, hoac ky tu
     * dieu khien; ghep thang vao duong dan la mo duong ghi ra ngoai thu muc.
     */
    private fun tep(context: Context, tieuDe: String, duoi: String): File {
        val ten = tieuDe.trim()
            .replace(Regex("[^\\p{L}\\p{N} _-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(60)
            .ifBlank { "Ghi chú" }
        return File(TepChiaSe.thuMuc(context), "$ten.$duoi")
    }
}
