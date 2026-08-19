package com.ghichu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

/**
 * Ve va thao tac ban do tu duy.
 *
 * Ve bang `Canvas` chu khong nhung mot web app: ban goc cua Xiaomi lam ban do tu
 * duy bang mot web app bien dich san 3,3 MB — chep sang la phat tan ma nguon dong
 * cua ho, ma con phai dich nguoc ca cau JS giua hai ben. Ve bang Canvas thi kich
 * thuoc them gan bang khong va toan quyen sua.
 *
 * Phan xep cho nam o [XepBanDo] — thuan Kotlin, kiem thu tren JVM duoc. O day
 * chi con ve, cham va keo.
 */
class BanDoView(context: Context) : View(context) {

    /** Goi khi nguoi dung cham vao mot nut. */
    var khiChonNut: ((Nut) -> Unit)? = null

    var goc: Nut = Nut("Bản đồ tư duy")
        set(v) {
            field = v
            xepLai()
        }

    var dangChon: Nut? = null
        set(v) {
            field = v
            invalidate()
        }

    private val mau = BangMau.cua(context)
    private val md = resources.displayMetrics
    private fun dp(v: Float) = v * md.density

    private var o: List<ONut> = emptyList()

    /** Ti le phong va do doi, deu tinh bang diem anh. */
    private var tiLe = 1f
    private var doiX = 0f
    private var doiY = 0f

    private var daDatChoLanDau = false

    private val sonNut = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sonVien = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val sonChu = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val sonNhanh = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    init {
        setBackgroundColor(mau.nen)
        sonChu.textSize = dp(15f)
    }

    // ------------------------------------------------------------ xep cho

    fun xepLai() {
        // Do be ngang chu do `Paint` tra — day la ly do [XepBanDo.xep] nhan mot
        // ham chu khong tu do: nho vay phan xep cho khong dinh gi den Android.
        o = XepBanDo.xep(goc) { sonChu.measureText(it) / md.density }
        invalidate()
    }

    /** Dua ca ban do vao vua man hinh. */
    fun vuaMan() {
        if (o.isEmpty() || width == 0) return
        val trai = o.minOf { it.x }
        val phai = o.maxOf { it.phai }
        val tren = o.minOf { it.y }
        val duoi = o.maxOf { it.y + it.cao }

        val le = dp(24f)
        val rongCan = (phai - trai) * md.density + le * 2
        val caoCan = (duoi - tren) * md.density + le * 2

        tiLe = minOf(width / rongCan, height / caoCan, 1.6f).coerceAtLeast(0.25f)
        doiX = (width - (phai - trai) * md.density * tiLe) / 2 - trai * md.density * tiLe
        doiY = (height - (duoi - tren) * md.density * tiLe) / 2 - tren * md.density * tiLe
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, cu: Int, caoCu: Int) {
        super.onSizeChanged(w, h, cu, caoCu)
        if (!daDatChoLanDau && w > 0) {
            daDatChoLanDau = true
            vuaMan()
        }
    }

    // ------------------------------------------------------------ ve

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(doiX, doiY)
        canvas.scale(tiLe * md.density, tiLe * md.density)

        sonNhanh.strokeWidth = 2f / md.density
        sonNhanh.color = mau.vach
        for (n in o) veNhanh(canvas, n)
        for (n in o) veNut(canvas, n)

        canvas.restore()
    }

    /**
     * Duong noi cha - con: mot duong cong Bezier, khong phai duong gay goc.
     *
     * Duong cong cho thay huong di cua nhanh ngay ca khi hai nut lech nhau nhieu
     * ve chieu doc; duong gay goc thi mot cay nhieu tang nhin nhu mot bo mach.
     */
    private fun veNhanh(canvas: Canvas, n: ONut) {
        val cha = n.cha ?: return
        val x1 = cha.phai
        val y1 = cha.giua
        val x2 = n.x
        val y2 = n.giua
        val giua = (x1 + x2) / 2

        canvas.drawPath(
            Path().apply {
                moveTo(x1, y1)
                cubicTo(giua, y1, giua, y2, x2, y2)
            },
            sonNhanh,
        )
    }

    private fun veNut(canvas: Canvas, n: ONut) {
        val khung = RectF(n.x, n.y, n.phai, n.y + n.cao)
        val bo = n.cao / 2

        val laGoc = n.cha == null
        val duocChon = n.nut === dangChon

        sonNut.color = when {
            laGoc -> mau.nhan
            duocChon -> BangMau.mauMo(mau.nhan, 0.28f)
            else -> mau.nenThanhDuoi
        }
        canvas.drawRoundRect(khung, bo, bo, sonNut)

        if (duocChon || laGoc) {
            sonVien.color = mau.nhan
            sonVien.strokeWidth = 2f / md.density
            canvas.drawRoundRect(khung, bo, bo, sonVien)
        }

        sonChu.color = if (laGoc) mau.chuTrenNhan else mau.chuChinh
        val chu = XepBanDo.nhan(n.nut)
        // Chu ve o don vi truu tuong nen co chu cung phai chia lai theo mat do.
        sonChu.textSize = 15f
        canvas.drawText(chu, n.x + n.rong / 2, n.giua + 5.4f, sonChu)
    }

    // ------------------------------------------------------------ cham va keo

    private val keo = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(su: MotionEvent) = true

            override fun onScroll(
                dau: MotionEvent?,
                gio: MotionEvent,
                dx: Float,
                dy: Float,
            ): Boolean {
                doiX -= dx
                doiY -= dy
                invalidate()
                return true
            }

            override fun onSingleTapUp(su: MotionEvent): Boolean {
                nutTai(su.x, su.y)?.let {
                    dangChon = it
                    khiChonNut?.invoke(it)
                }
                return true
            }
        },
    )

    private val phong = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(g: ScaleGestureDetector): Boolean {
                val truoc = tiLe
                tiLe = (tiLe * g.scaleFactor).coerceIn(0.25f, 3f)
                // Giu diem giua hai ngon dung yen tren man hinh khi phong to —
                // khong lam vay thi ban do truot di moi lan chum ngon.
                val k = tiLe / truoc
                doiX = g.focusX - (g.focusX - doiX) * k
                doiY = g.focusY - (g.focusY - doiY) * k
                invalidate()
                return true
            }
        },
    )

    override fun onTouchEvent(su: MotionEvent): Boolean {
        phong.onTouchEvent(su)
        // Dang chum hai ngon thi KHONG cho bo keo xu ly: hai bo cung doi `doi`
        // mot luc la ban do giat nay.
        if (!phong.isInProgress) keo.onTouchEvent(su)
        return true
    }

    private fun nutTai(x: Float, y: Float): Nut? {
        val bx = (x - doiX) / (tiLe * md.density)
        val by = (y - doiY) / (tiLe * md.density)
        // Duyet nguoc: nut ve sau nam tren, nen cham trung thi lay cai do.
        return o.lastOrNull { bx >= it.x && bx <= it.phai && by >= it.y && by <= it.y + it.cao }?.nut
    }
}
