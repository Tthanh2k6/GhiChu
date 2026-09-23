package com.ghichu

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Lịch THÁNG dạng lưới: 7 cột (T2…CN) × 6 hàng, mỗi ô một ngày cả tháng.
 *
 * Bù cho lưới TUẦN ([TuanView]) vốn chỉ nhìn được vài ngày quanh hiện tại — lịch
 * tháng cho thấy cả tháng trong một màn, ngày nào có việc thì hiện chấm màu (lấy
 * đúng màu khối sự kiện). Chạm một ngày gọi [onChonNgay] (màn cha nhảy về lưới
 * tuần tại ngày đó); vuốt ngang đổi tháng, báo qua [onDoiThang].
 *
 * Chỉ VẼ, không giữ dữ liệu gốc: nhận danh sách [SuKien] rồi tự trải lịch lặp
 * bằng [LichLapLai.moRong] cho đúng khoảng 6 tuần đang hiện.
 */
@SuppressLint("ViewConstructor")
class LichThangView(context: Context, private val mau: BangMau) : View(context) {

    private val md = resources.displayMetrics
    private fun dp(v: Float) = v * md.density
    private fun sp(v: Float) = v * md.scaledDensity

    private var thang: YearMonth = YearMonth.now()
    private var zone: ZoneId = ZoneId.systemDefault()
    private var suKien: List<SuKien> = emptyList()

    /** Ngày -> tối đa 3 màu chấm, tính lại mỗi khi tháng hoặc dữ liệu đổi. */
    private val chamNgay = HashMap<LocalDate, MutableList<Int>>()

    var onChonNgay: ((LocalDate) -> Unit)? = null
    var onDoiThang: ((YearMonth) -> Unit)? = null

    private val sonChu = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val sonNen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sonCham = Paint(Paint.ANTI_ALIAS_FLAG)

    private val THU = arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    private val cuChi = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val ngay = ngayTai(e.x, e.y) ?: return false
            onChonNgay?.invoke(ngay)
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (kotlin.math.abs(vx) < kotlin.math.abs(vy) || kotlin.math.abs(vx) < dp(400f)) return false
            datThang(if (vx < 0) thang.plusMonths(1) else thang.minusMonths(1))
            onDoiThang?.invoke(thang)
            return true
        }
    })

    fun datThang(m: YearMonth) {
        thang = m
        tinhCham()
        invalidate()
    }

    fun thangDangXem(): YearMonth = thang

    fun datDuLieu(ds: List<SuKien>, z: ZoneId) {
        suKien = ds
        zone = z
        tinhCham()
        invalidate()
    }

    private fun tinhCham() {
        chamNgay.clear()
        val batDauLuoi = dauTuanCua(thang.atDay(1))
        val tu = NgayVN.dauNgay(batDauLuoi, zone)
        val den = NgayVN.dauNgay(batDauLuoi.plusDays(42), zone)
        for (sk in suKien) {
            for (lan in LichLapLai.moRong(sk, tu, den, zone)) {
                val d = Instant.ofEpochMilli(lan.batDau).atZone(zone).toLocalDate()
                val ds = chamNgay.getOrPut(d) { mutableListOf() }
                if (ds.size < 3) ds.add(MauSuKien.cua(sk.mau))
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = cuChi.onTouchEvent(event)

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val caoDau = dp(28f)
        val oRong = w / 7f
        val oCao = (h - caoDau) / 6f

        // Hàng tên thứ.
        sonChu.textSize = sp(11f)
        sonChu.color = mau.chuMo
        for (c in 0 until 7) {
            canvas.drawText(THU[c], oRong * c + oRong / 2f, caoDau * 0.7f, sonChu)
        }

        val homNay = LocalDate.now(zone)
        val batDauLuoi = dauTuanCua(thang.atDay(1))

        for (i in 0 until 42) {
            val ngay = batDauLuoi.plusDays(i.toLong())
            val hang = i / 7
            val cot = i % 7
            val x = oRong * cot
            val y = caoDau + oCao * hang
            val giua = x + oRong / 2f
            val trongThang = ngay.month == thang.month && ngay.year == thang.year

            // Nền ngày hôm nay: ô tròn màu nhấn.
            val laHomNay = ngay == homNay
            val banKinh = kotlin.math.min(oRong, oCao) * 0.30f
            val tamY = y + oCao * 0.42f
            if (laHomNay) {
                sonNen.color = mau.nhan
                canvas.drawCircle(giua, tamY, banKinh, sonNen)
            }

            // Số ngày.
            sonChu.textSize = sp(15f)
            sonChu.color = when {
                laHomNay -> mau.chuTrenNhan
                !trongThang -> mau.chuMo
                else -> mau.chuChinh
            }
            val fm = sonChu.fontMetrics
            canvas.drawText(
                ngay.dayOfMonth.toString(),
                giua, tamY - (fm.ascent + fm.descent) / 2f, sonChu,
            )

            // Chấm sự kiện dưới số (ẩn nếu trùng nền hôm nay cho gọn — vẫn hiện).
            val mauCham = chamNgay[ngay]
            if (!mauCham.isNullOrEmpty()) {
                val rCham = dp(2.5f)
                val cachCham = dp(7f)
                val yCham = tamY + banKinh + dp(6f)
                val tong = mauCham.size
                val batX = giua - (tong - 1) * cachCham / 2f
                for (k in 0 until tong) {
                    sonCham.color = if (laHomNay) mau.chuTrenNhan else mauCham[k]
                    canvas.drawCircle(batX + k * cachCham, yCham, rCham, sonCham)
                }
            }
        }

        // Vạch ngang mảnh giữa các hàng cho dễ đọc.
        sonNen.color = mau.vienHang
        val day = kotlin.math.max(1f, dp(0.5f))
        for (r in 1 until 6) {
            val y = caoDau + oCao * r
            canvas.drawRect(0f, y, w, y + day, sonNen)
        }
    }

    private fun ngayTai(px: Float, py: Float): LocalDate? {
        val caoDau = dp(28f)
        if (py < caoDau) return null
        val oRong = width / 7f
        val oCao = (height - caoDau) / 6f
        val cot = (px / oRong).toInt().coerceIn(0, 6)
        val hang = ((py - caoDau) / oCao).toInt().coerceIn(0, 5)
        return dauTuanCua(thang.atDay(1)).plusDays((hang * 7 + cot).toLong())
    }
}
