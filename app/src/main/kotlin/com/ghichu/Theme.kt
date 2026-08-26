package com.ghichu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * Giao dien (theme) cua mot ghi chu — nen giay + mau chu.
 *
 * Y tuong lay tu app Ghi chu Xiaomi: moi ghi chu chon mot "theme" (giay), gom
 * mau/anh nen va mau chu di kem. Id luu o cot `mau` cua [GhiChu] (dung chung o
 * do vi ban dau `mau` chi la mau giay 0..7; theme anh dung id 8 tro len — khong
 * phai them cot, khong phai nang phien ban CSDL).
 *
 * MAU DAC (0..7) van do [MauGhiChu] lo — giu nguyen ghi chu cu. Theme o day la
 * cac id 8+ : gradient hoac anh nen, kem mau chu rieng.
 *
 * Anh nen lay tu app goc (docs: bo theme), chi vai tep webp nhe:
 *   nen_mua_thu (giay lat), nen_chiec_la, nen_keo.
 */
class Theme(
    val id: Int,
    val ten: String,
    /** Ve nen ra khung nay. */
    val nen: (Context, BangMau) -> Drawable,
    /** Mau chu tren nen theme (tieu de/than). null = theo [BangMau] thuong. */
    val chuChinh: Int? = null,
    val chuPhu: Int? = null,
    /** Nen o CHON MAU thu nho (thumbnail) — thuong la chinh [nen]. */
    val xemTruoc: ((Context, BangMau) -> Drawable)? = null,
) {
    fun mauChuChinh(mau: BangMau): Int = chuChinh ?: (if (mau.toi) 0xE6FFFFFF.toInt() else 0xFF000000.toInt())
    fun mauChuPhu(mau: BangMau): Int = chuPhu ?: mau.chuThongTin
}

object BoTheme {

    /** Id nho hon nguong nay la MAU DAC cua [MauGhiChu]; tu day tro len la theme anh. */
    const val NGUONG_ANH = 8

    /** Gradient hai mau, goc tinh bang do (0 = trai sang phai, 90 = tren xuong duoi). */
    private fun gradient(c1: Int, c2: Int, goc: Int): (Context, BangMau) -> Drawable = { _, _ ->
        object : Drawable() {
            private val son = Paint(Paint.ANTI_ALIAS_FLAG)
            override fun draw(canvas: Canvas) {
                val b = bounds
                val r = Math.toRadians(goc.toDouble())
                val dx = Math.cos(r).toFloat(); val dy = Math.sin(r).toFloat()
                son.shader = LinearGradient(
                    b.centerX() - dx * b.width() / 2, b.centerY() - dy * b.height() / 2,
                    b.centerX() + dx * b.width() / 2, b.centerY() + dy * b.height() / 2,
                    c1, c2, Shader.TileMode.CLAMP,
                )
                canvas.drawRect(b, son)
            }
            override fun setAlpha(a: Int) {}
            override fun setColorFilter(f: android.graphics.ColorFilter?) {}
            @Deprecated("deprecated") override fun getOpacity() = android.graphics.PixelFormat.OPAQUE
        }
    }

    /** Bau troi dem + sao: gradient xanh + cac dom sang rai deu (khong can anh). */
    private val bauTroiDem: (Context, BangMau) -> Drawable = { _, _ ->
        object : Drawable() {
            private val nen = Paint(Paint.ANTI_ALIAS_FLAG)
            private val sao = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x80FFFFFF.toInt() }
            override fun draw(canvas: Canvas) {
                val b = bounds
                nen.shader = LinearGradient(
                    b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(),
                    0xFF030943.toInt(), 0xFF018BD9.toInt(), Shader.TileMode.CLAMP,
                )
                canvas.drawRect(b, nen)
                // Sao rai deu bang cong thuc co dinh (khong ngau nhien de ve lai giong nhau).
                var seed = 12345L
                for (i in 0 until 70) {
                    seed = (seed * 1103515245 + 12345) and 0x7FFFFFFF
                    val x = b.left + (seed % b.width())
                    seed = (seed * 1103515245 + 12345) and 0x7FFFFFFF
                    val y = b.top + (seed % (b.height() * 3 / 5))
                    seed = (seed * 1103515245 + 12345) and 0x7FFFFFFF
                    sao.alpha = 60 + (seed % 140).toInt()
                    canvas.drawCircle(x.toFloat(), y.toFloat(), if (seed % 5 == 0L) 3f else 1.8f, sao)
                }
            }
            override fun setAlpha(a: Int) {}
            override fun setColorFilter(f: android.graphics.ColorFilter?) {}
            @Deprecated("deprecated") override fun getOpacity() = android.graphics.PixelFormat.OPAQUE
        }
    }

    /** Nen anh: mau nen dac + anh dat theo [gravity] (khong lat), giu ti le. */
    private fun anhGoc(nenMau: Int, res: Int, gravity: Int, tiLeCao: Float = 0.4f):
        (Context, BangMau) -> Drawable = { ctx, _ ->
        val bm = BitmapFactory.decodeResource(ctx.resources, res)
        object : Drawable() {
            private val son = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            override fun draw(canvas: Canvas) {
                val b = bounds
                canvas.drawColor(nenMau)
                if (bm != null) {
                    val w = b.width() * 0.55f
                    val h = w * bm.height / bm.width
                    val x = if (gravity and android.view.Gravity.END != 0) b.right - w else b.left.toFloat()
                    val y = if (gravity and android.view.Gravity.BOTTOM != 0) b.bottom - h else b.top.toFloat()
                    val dst = android.graphics.RectF(x, y, x + w, y + h)
                    canvas.drawBitmap(bm, null, dst, son)
                }
            }
            override fun setAlpha(a: Int) {}
            override fun setColorFilter(f: android.graphics.ColorFilter?) {}
            @Deprecated("deprecated") override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
        }
    }

    /** Nen lat (tile) mot anh nho — giay Mua thu. */
    private fun anhLat(res: Int): (Context, BangMau) -> Drawable = { ctx, _ ->
        val bm = BitmapFactory.decodeResource(ctx.resources, res)
        object : Drawable() {
            private val son = Paint().apply {
                if (bm != null) shader = BitmapShader(bm, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            }
            override fun draw(canvas: Canvas) { canvas.drawRect(bounds, son) }
            override fun setAlpha(a: Int) {}
            override fun setColorFilter(f: android.graphics.ColorFilter?) {}
            @Deprecated("deprecated") override fun getOpacity() = android.graphics.PixelFormat.OPAQUE
        }
    }

    /**
     * Cac theme anh. Mau chu chon sao cho doc ro tren nen.
     * Ten va mau lay tu app goc (docs/bo theme): tim theme_title_*.
     */
    val DANH_SACH: List<Theme> = listOf(
        Theme(
            8, "Bầu trời đêm", bauTroiDem,
            chuChinh = 0xE6FFFFFE.toInt(), chuPhu = 0xCCFFFFFF.toInt(),
        ),
        Theme(
            9, "Đổ bóng", gradient(0xFFFFF1C2.toInt(), 0xFFFFF9E4.toInt(), 90),
            chuChinh = 0xFF9D6802.toInt(), chuPhu = 0xCC9D6802.toInt(),
        ),
        Theme(
            10, "Chiếc lá",
            anhGoc(0xFF7FCBBE.toInt(), R.drawable.nen_chiec_la, android.view.Gravity.BOTTOM or android.view.Gravity.END, 0.5f),
            chuChinh = 0xE6FFFFFD.toInt(), chuPhu = 0xCCFFFFFF.toInt(),
        ),
        Theme(
            11, "Mùa thu", anhLat(R.drawable.nen_mua_thu),
            chuChinh = 0xFF000000.toInt(), chuPhu = 0x99000000.toInt(),
        ),
        Theme(
            12, "Kẹo",
            anhGoc(0xFFFCE4E9.toInt(), R.drawable.nen_keo, android.view.Gravity.TOP or android.view.Gravity.END, 0.5f),
            chuChinh = 0xFFBF5866.toInt(), chuPhu = 0xCCBF5866.toInt(),
        ),
    )

    fun theo(id: Int): Theme? = DANH_SACH.firstOrNull { it.id == id }

    fun laThemeAnh(id: Int): Boolean = id >= NGUONG_ANH
}
