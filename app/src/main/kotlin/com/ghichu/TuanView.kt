package com.ghichu

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Luoi Lich trinh — chep lai cach hien thi cua ban demo Flutter
 * (D:\Project\Note, lib/screens/calendar_screen.dart + lib/utils/event_layout.dart).
 *
 * BA QUY UOC LAY NGUYEN VAN TU BAN DEMO — dung doi neu chua doi chieu lai voi no:
 *
 * 1. Chi hien tu [GIO_DAU] den nua dem, AN han 0h–6h. Hau nhu khong ai co su kien
 *    luc 3 gio sang; bo di thi 18 gio con lai duoc chia rong ra.
 *
 * 2. O muc thu nho nhat (ti le 1.0) thi **ca 18 gio va 7 cot vua khit man hinh**,
 *    khong phai cuon gi ca. Chum ngon phong to len moi cuon duoc. Day la diem khac
 *    lon nhat so voi ban truoc: ban truoc dat cung chieu cao gio 56dp roi cuon doc.
 *
 * 3. Cuon ngang la **lien tuc theo ngay**, khong nhay tung tuan. Mo ra thi cot trai
 *    nhat la HOM QUA, roi hom nay va 5 ngay tiep.
 *
 * Toa do: bo cuc tinh theo "phut ke tu [GIO_DAU]" va "chi so ngay ke tu [moc]",
 * doi sang pixel luc ve. Nho vay cuon va phong to khong phai xep lai bo cuc.
 *
 * Giao dien dung bang code chu khong dung R.layout: ma tai nguyen sinh o module
 * nay khong khop bang tai nguyen cua app dich.
 */
@SuppressLint("ViewConstructor")
class TuanView(context: Context) : View(context) {

    // ---- Du lieu ----

    private var zone: ZoneId = ZoneId.systemDefault()
    private var dsSuKien: List<SuKien> = emptyList()

    /** Doc lai moi lan man hinh mo — nguoi dung co the vua doi sang/toi. */
    private var mau = BangMau.cua(context)
    private var coCaNgay = false

    /** Ngay ung voi chi so 0. Cuon duoc mot nam ve truoc, mot nam toi. */
    private var moc: LocalDate = LocalDate.now().minusDays(SO_NGAY_LUI.toLong())
    private val soNgay = SO_NGAY_LUI + SO_NGAY_TOI

    /** Bo cuc tung ngay, tinh mot lan roi dung lai — xoa khi du lieu doi. */
    private val demNgay = HashMap<Int, List<Khoi>>()
    private val demCaNgay = HashMap<Int, List<LanHien>>()

    // ---- Goi nguoc ra ngoai ----

    var onChon: ((LanHien) -> Unit)? = null
    var onNgayTraiDoi: ((LocalDate) -> Unit)? = null

    // ---- Kich thuoc ----

    private val md = resources.displayMetrics
    private fun dp(v: Float) = v * md.density
    private fun sp(v: Float) = v * md.scaledDensity

    private val gutterW = dp(46f)
    private val dauCotH = dp(30f)
    private val caNgayH = dp(28f)

    private var tiLe = TI_LE_MIN
    private var cuonX = 0f
    private var cuonY = 0f
    private var daDatViTriDau = false
    private var ngayTraiCu = -1

    private val luoiTop: Float get() = dauCotH + (if (coCaNgay) caNgayH else 0f)
    private fun khungCao(): Float = max(1f, height - luoiTop)
    private fun chieuCaoGio(): Float = khungCao() / SO_GIO * tiLe
    private fun caoNgay(): Float = chieuCaoGio() * SO_GIO
    private fun rongCot(): Float = max(1f, (width - gutterW) / SO_COT_CHUAN * tiLe)
    private fun rongLuoi(): Float = width - gutterW

    private fun cuonXToiDa(): Float = max(0f, soNgay * rongCot() - rongLuoi())
    private fun cuonYToiDa(): Float = max(0f, caoNgay() - khungCao())

    // ---- But ve ----

    private val pNen = Paint().apply { color = mau.nen }
    private val pVachGio = Paint().apply { color = mau.vach; strokeWidth = dp(0.4f) }
    private val pVachCot = Paint().apply { color = mau.vach; strokeWidth = dp(0.4f) }
    private val pVienHang = Paint().apply { color = mau.vienHang; strokeWidth = dp(0.6f) }

    private val pChuGio = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = mau.chuMo; textSize = sp(11f); textAlign = Paint.Align.RIGHT
    }
    private val pChuGutter = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = mau.chuMo; textSize = sp(10f); textAlign = Paint.Align.CENTER
    }
    private val pSoNgay = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = mau.chuPhu; textSize = sp(14f); textAlign = Paint.Align.CENTER
    }
    private val pSoNgayHomNay = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = mau.chuTrenNhan; textSize = sp(14f); textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val pONhanHomNay = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = mau.nhan }
    private val pKhoi = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pBayGio = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = mau.doBayGio }

    private val pTieuDeKhoi = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = sp(11.5f) }
    private val pGioKhoi = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = sp(10f) }
    private val pChuCaNgay = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = sp(10.5f) }

    // ---- Cu chi ----

    private val scroller = OverScroller(context)

    private val doPhongTo = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean {
                val cu = tiLe
                val moi = (cu * d.scaleFactor).coerceIn(TI_LE_MIN, TI_LE_MAX)
                if (moi == cu) return true

                // Neo vao diem giua hai ngon: o nam duoi ngon tay dung yen.
                val cotCu = rongCot()
                val gioCu = chieuCaoGio()
                val nX = (cuonX + (d.focusX - gutterW)) / cotCu
                val nY = (cuonY + (d.focusY - luoiTop)) / gioCu

                tiLe = moi
                xoaBoCuc()
                ganCuon(
                    nX * rongCot() - (d.focusX - gutterW),
                    nY * chieuCaoGio() - (d.focusY - luoiTop),
                )
                invalidate()
                return true
            }
        },
    )

    private val doCuChi = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                scroller.forceFinished(true)
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
                if (doPhongTo.isInProgress) return false
                ganCuon(cuonX + dx, cuonY + dy)
                invalidate()
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (doPhongTo.isInProgress) return false
                scroller.fling(
                    cuonX.roundToInt(), cuonY.roundToInt(),
                    -vx.roundToInt(), -vy.roundToInt(),
                    0, cuonXToiDa().roundToInt(),
                    0, cuonYToiDa().roundToInt(),
                )
                postInvalidateOnAnimation()
                return true
            }

            /**
             * Ban demo CO Y khong cho cham o trong de them su kien — them chi bang
             * nut +. Nho vay cu cham luon toi dung khoi su kien, khong bao gio lo
             * tay mo ra hop "Su kien moi".
             */
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                timKhoi(e.x, e.y)?.let { onChon?.invoke(it.lan) }
                return true
            }
        },
    )

    init {
        setBackgroundColor(mau.nen)
        isFocusable = true
    }

    // ---- Cap nhat du lieu ----

    /**
     * Doc lai bang mau. Goi moi lan man hinh mo — nguoi dung co the vua doi
     * sang/toi trong Cai dat ma khong dong app.
     */
    fun doiBangMau() {
        val moi = BangMau.cua(context)
        if (moi.toi == mau.toi) return
        mau = moi

        pNen.color = mau.nen
        pVachGio.color = mau.vach
        pVachCot.color = mau.vach
        pVienHang.color = mau.vienHang
        pChuGio.color = mau.chuMo
        pChuGutter.color = mau.chuMo
        pSoNgay.color = mau.chuPhu
        pSoNgayHomNay.color = mau.chuTrenNhan
        pONhanHomNay.color = mau.nhan
        pBayGio.color = mau.doBayGio
        setBackgroundColor(mau.nen)
        invalidate()
    }

    /** Nap toan bo su kien; [boCucNgay] se trai su kien lap lai theo tung ngay. */
    fun datDuLieu(ds: List<SuKien>, zone: ZoneId = ZoneId.systemDefault()) {
        this.dsSuKien = ds
        this.zone = zone
        this.coCaNgay = ds.any { it.caNgay }
        xoaBoCuc()
        invalidate()
    }

    /** Nhay toi tuan chua [ngay], dat no o cot thu hai nhu luc moi mo. */
    fun nhayToi(ngay: LocalDate) {
        val chiSo = (ChronoUnit.DAYS.between(moc, ngay.minusDays(1))).toInt()
        ganCuon(chiSo * rongCot(), cuonY)
        invalidate()
        baoNgayTrai()
    }

    fun veHomNay() = nhayToi(LocalDate.now(zone))

    /** Ngay dang o mep trai — dung cho tieu de thang. */
    fun ngayTrai(): LocalDate = moc.plusDays(chiSoTrai().toLong())

    private fun chiSoTrai(): Int =
        floor(cuonX / rongCot()).toInt().coerceIn(0, soNgay - 1)

    private fun xoaBoCuc() {
        demNgay.clear()
        demCaNgay.clear()
    }

    private fun ganCuon(x: Float, y: Float) {
        cuonX = x.coerceIn(0f, cuonXToiDa())
        cuonY = y.coerceIn(0f, cuonYToiDa())
    }

    private fun baoNgayTrai() {
        val i = chiSoTrai()
        if (i != ngayTraiCu) {
            ngayTraiCu = i
            onNgayTraiDoi?.invoke(moc.plusDays(i.toLong()))
        }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        xoaBoCuc()
        if (!daDatViTriDau && w > 0) {
            daDatViTriDau = true
            // Cot trai nhat la HOM QUA — giong ban demo, de con nhin lai viec vua qua.
            nhayToi(LocalDate.now(zone))
        }
        ganCuon(cuonX, cuonY)
    }

    // ---- Bo cuc mot ngay ----

    /**
     * Xep su kien cua mot ngay thanh cac khoi khong de len nhau.
     *
     * Giong `layoutDay` cua ban demo: cat cho nam gon trong cua so hien thi
     * (su kien ket thuc truoc [GIO_DAU] bi an han), gom thanh tung cum chong lan
     * roi moi cum chia cot rieng.
     */
    private fun boCucNgay(chiSo: Int): List<Khoi> = demNgay.getOrPut(chiSo) {
        val ngay = moc.plusDays(chiSo.toLong())
        val nuaDem = NgayVN.dauNgay(ngay, zone)
        val cuaSoTu = nuaDem + GIO_DAU * 3_600_000L
        val cuoiNgay = NgayVN.dauNgay(ngay.plusDays(1), zone)

        val lan = dsSuKien
            .flatMap { LichLapLai.moRong(it, cuaSoTu, cuoiNgay, zone) }
            .filter { !it.suKien.caNgay }

        val muc = ArrayList<Khoi>()
        for (l in lan) {
            if (l.batDau >= cuoiNgay || l.ketThuc <= cuaSoTu) continue
            val tu = max(l.batDau, cuaSoTu)
            var den = min(l.ketThuc, cuoiNgay)
            // Su kien dai 0 phut van phai bam duoc.
            if (den <= tu) den = tu + 15 * 60_000L
            muc += Khoi(
                lan = l,
                phutTu = (tu - cuaSoTu) / 60_000f,
                phutDen = (den - cuaSoTu) / 60_000f,
            )
        }
        if (muc.isEmpty()) return@getOrPut emptyList()

        muc.sortWith(compareBy({ it.phutTu }, { -it.phutDen }))
        xepCot(muc)
        muc
    }

    /**
     * Chia cot cho cac khoi chong gio nhau.
     *
     * Su kien nao bat dau sau khi CA CUM truoc da ket thuc thi mo cum moi; trong
     * mot cum, moi khoi lay cot trong dau tien.
     */
    private fun xepCot(ds: List<Khoi>) {
        val cum = ArrayList<Khoi>()
        val cuoiCot = ArrayList<Float>()

        fun chot() {
            for (k in cum) k.soCot = cuoiCot.size
            cum.clear()
            cuoiCot.clear()
        }

        for (k in ds) {
            if (cuoiCot.isNotEmpty() && cuoiCot.all { it <= k.phutTu }) chot()
            var c = cuoiCot.indexOfFirst { it <= k.phutTu }
            if (c < 0) {
                cuoiCot.add(0f)
                c = cuoiCot.size - 1
            }
            cuoiCot[c] = k.phutDen
            k.cot = c
            cum += k
        }
        chot()
    }

    /** Su kien ca ngay roi vao ngay [chiSo]. */
    private fun caNgayCua(chiSo: Int): List<LanHien> {
        if (!coCaNgay) return emptyList()
        return demCaNgay.getOrPut(chiSo) {
            val ngay = moc.plusDays(chiSo.toLong())
            val tu = NgayVN.dauNgay(ngay, zone)
            val den = NgayVN.dauNgay(ngay.plusDays(1), zone)
            dsSuKien
                .flatMap { LichLapLai.moRong(it, tu, den, zone) }
                .filter { it.suKien.caNgay }
        }
    }

    // ---- Ve ----

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), pNen)

        val tu = chiSoTrai()
        val den = min(soNgay - 1, ceil((cuonX + rongLuoi()) / rongCot()).toInt())

        veDauCot(canvas, tu, den)
        if (coCaNgay) veCaNgay(canvas, tu, den)

        canvas.save()
        canvas.clipRect(gutterW, luoiTop, width.toFloat(), height.toFloat())
        veLuoiVaKhoi(canvas, tu, den)
        canvas.restore()

        // Nhan gio ve sau cung, ngoai vung clip cua luoi, de khong bi khoi de len.
        canvas.save()
        canvas.clipRect(0f, luoiTop, gutterW, height.toFloat())
        veNhanGio(canvas)
        canvas.restore()
    }

    private fun veDauCot(canvas: Canvas, tu: Int, den: Int) {
        canvas.drawText("GMT${NgayVN.lechGio(zone)}", gutterW / 2f, dauCotH / 2f + dp(4f), pChuGutter)

        val homNay = LocalDate.now(zone)
        val cot = rongCot()
        val o = dp(26f)

        canvas.save()
        canvas.clipRect(gutterW, 0f, width.toFloat(), dauCotH)
        for (i in tu..den) {
            val giua = gutterW + (i + 0.5f) * cot - cuonX
            val ngay = moc.plusDays(i.toLong())
            val laHomNay = ngay == homNay
            val tamY = dauCotH / 2f

            if (laHomNay) {
                val r = RectF(giua - o / 2, tamY - o / 2, giua + o / 2, tamY + o / 2)
                canvas.drawRoundRect(r, dp(7f), dp(7f), pONhanHomNay)
                canvas.drawText("${ngay.dayOfMonth}", giua, tamY + dp(5f), pSoNgayHomNay)
            } else {
                canvas.drawText("${ngay.dayOfMonth}", giua, tamY + dp(5f), pSoNgay)
            }
        }
        canvas.restore()
        canvas.drawLine(0f, dauCotH, width.toFloat(), dauCotH, pVienHang)
    }

    private fun veCaNgay(canvas: Canvas, tu: Int, den: Int) {
        val top = dauCotH
        canvas.drawText("Cả ngày", gutterW / 2f, top + caNgayH / 2f + dp(3f), pChuGutter)

        val cot = rongCot()
        canvas.save()
        canvas.clipRect(gutterW, top, width.toFloat(), top + caNgayH)
        for (i in tu..den) {
            val trong = caNgayCua(i)
            if (trong.isEmpty()) continue
            val dau = trong.first()
            val mauKhoi = MauSuKien.cua(dau.suKien.mau)
            val trai = gutterW + i * cot - cuonX

            val hop = RectF(trai + dp(1.5f), top + dp(4f), trai + cot - dp(1.5f), top + caNgayH - dp(4f))
            pKhoi.color = this.mau.nenKhoi(mauKhoi)
            canvas.drawRoundRect(hop, dp(4f), dp(4f), pKhoi)

            // Nhieu su kien ca ngay cung ngay thi hien cai dau + so con lai.
            val chu = if (trong.size > 1) "${dau.suKien.tieuDe} +${trong.size - 1}" else dau.suKien.tieuDe
            pChuCaNgay.color = this.mau.chuKhoi(mauKhoi)
            val cat = TextUtils.ellipsize(chu.ifBlank { "(không tên)" }, pChuCaNgay, hop.width() - dp(8f), TextUtils.TruncateAt.END)
            canvas.drawText(cat, 0, cat.length, hop.left + dp(4f), hop.centerY() + dp(4f), pChuCaNgay)
        }
        canvas.restore()
        canvas.drawLine(0f, top + caNgayH, width.toFloat(), top + caNgayH, pVienHang)
    }

    private fun veLuoiVaKhoi(canvas: Canvas, tu: Int, den: Int) {
        val cot = rongCot()
        val gio = chieuCaoGio()
        val top = luoiTop

        // Vach gio ngang: bat dau tu sau GIO_DAU vi GIO_DAU chinh la mep tren.
        for (h in GIO_DAU + 1 until 24) {
            val y = top + (h - GIO_DAU) * gio - cuonY
            if (y < top || y > height) continue
            canvas.drawLine(gutterW, y, width.toFloat(), y, pVachGio)
        }

        for (i in tu..den) {
            val trai = gutterW + i * cot - cuonX
            canvas.drawLine(trai, top, trai, height.toFloat(), pVachCot)
            veKhoiNgay(canvas, i, trai, cot, gio, top)
        }

        veVachBayGio(canvas, tu, den, cot, gio, top)
    }

    private fun veKhoiNgay(canvas: Canvas, chiSo: Int, trai: Float, cot: Float, gio: Float, top: Float) {
        for (k in boCucNgay(chiSo)) {
            val rong = cot / k.soCot
            val o = RectF(
                trai + rong * k.cot + dp(1f),
                top + k.phutTu / 60f * gio - cuonY,
                trai + rong * (k.cot + 1) - dp(1f),
                top + k.phutDen / 60f * gio - cuonY,
            )
            if (o.bottom < top || o.top > height) continue
            if (o.height() < dp(3f)) o.bottom = o.top + dp(3f)
            veMotKhoi(canvas, k, o)
        }
    }

    /** Nen mau nhat + vach mau dam ben trai — dung kieu cua ban demo. */
    private fun veMotKhoi(canvas: Canvas, k: Khoi, o: RectF) {
        val mauKhoi = MauSuKien.cua(k.lan.suKien.mau)
        val r = dp(4f)

        pKhoi.color = mau.nenKhoi(mauKhoi)
        canvas.drawRoundRect(o, r, r, pKhoi)

        pKhoi.color = mauKhoi
        canvas.save()
        canvas.clipRect(o.left, o.top, o.left + VIEN_TRAI_DP * md.density, o.bottom)
        canvas.drawRoundRect(o, r, r, pKhoi)
        canvas.restore()

        veChuTrongKhoi(canvas, k, o, mauKhoi)
    }

    /**
     * Chi hien chu khi TIEU DE vua TRON o.
     *
     * Do that bang [StaticLayout] chu khong uoc luong: theo yeu cau o ban demo,
     * thieu cho thi de o mau tron con hon la chu bi cut giua chung.
     */
    private fun veChuTrongKhoi(canvas: Canvas, k: Khoi, o: RectF, mauKhoi: Int) {
        val trai = o.left + VIEN_TRAI_DP * md.density + dp(4f)
        val coRong = (o.right - dp(2f) - trai).toInt()
        val coCao = o.height() - dp(4f)
        if (coRong <= dp(8f) || coCao <= dp(8f)) return

        pTieuDeKhoi.color = mau.chuKhoi(mauKhoi)
        val bo = k.chu(coRong, pTieuDeKhoi)
        if (bo.height > coCao) return

        canvas.save()
        canvas.translate(trai, o.top + dp(2f))
        bo.draw(canvas)
        canvas.restore()

        if (coCao - bo.height >= dp(12f)) {
            pGioKhoi.color = mau.gioKhoi(mauKhoi)
            canvas.drawText(
                NgayVN.gioPhut(k.lan.batDau, zone),
                trai, o.top + dp(2f) + bo.height + dp(10f), pGioKhoi,
            )
        }
    }

    private fun veNhanGio(canvas: Canvas) {
        val gio = chieuCaoGio()
        // Cho hep thi bo bot nhan, khong thi chu chong len nhau.
        val buoc = if (gio < dp(22f)) 3 else if (gio < dp(34f)) 2 else 1
        var h = GIO_DAU
        while (h < 24) {
            // Ghim nhan dau tien xuong duoi mep luoi — o toa do NOI DUNG, khong phai
            // toa do man hinh: ghim theo man hinh thi cuon xuong ma "06:00" van dinh
            // o dau luoi.
            val yNoiDung = max(dp(11f), (h - GIO_DAU) * gio + dp(4f))
            val y = luoiTop + yNoiDung - cuonY
            if (y >= luoiTop && y < height) {
                canvas.drawText(NgayVN.nhanGio(h), gutterW - dp(6f), y, pChuGio)
            }
            h += buoc
        }
    }

    private fun veVachBayGio(canvas: Canvas, tu: Int, den: Int, cot: Float, gio: Float, top: Float) {
        val bayGio = LocalDateTime.now(zone)
        val i = ChronoUnit.DAYS.between(moc, bayGio.toLocalDate()).toInt()
        if (i !in tu..den) return

        // Truoc GIO_DAU thi dang o khoang da an — khong ve vach.
        val phut = (bayGio.hour * 60 + bayGio.minute) - GIO_DAU * 60
        if (phut < 0) return

        val y = top + phut / 60f * gio - cuonY
        if (y < top || y > height) return

        val trai = gutterW + i * cot - cuonX
        canvas.drawCircle(trai + dp(3f), y, dp(3f), pBayGio)
        canvas.drawRect(trai, y - dp(0.7f), trai + cot, y + dp(0.7f), pBayGio)
    }

    // ---- Cham ----

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Man nay nam trong ViewPager cua app (trang Ghi chú | Lịch trình). Khong
        // chan thi vuot ngang de doi ngay se bi ViewPager cuop mat de lat sang trang
        // Ghi chú. Gia lai quyen cuon ngang ngay tu luc ngon tay cham xuong.
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }

        doPhongTo.onTouchEvent(event)
        // Dang chum hai ngon thi bo qua cu chi cuon, khong thi vua phong vua nhay.
        if (!doPhongTo.isInProgress) doCuChi.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP) baoNgayTrai()
        return true
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            ganCuon(scroller.currX.toFloat(), scroller.currY.toFloat())
            baoNgayTrai()
            postInvalidateOnAnimation()
        }
    }

    private fun timKhoi(x: Float, y: Float): Khoi? {
        if (x < gutterW || y < luoiTop) return null
        val cot = rongCot()
        val gio = chieuCaoGio()
        val i = floor((x + cuonX - gutterW) / cot).toInt()
        if (i !in 0 until soNgay) return null

        val trai = gutterW + i * cot - cuonX
        for (k in boCucNgay(i)) {
            val rong = cot / k.soCot
            val o = RectF(
                trai + rong * k.cot,
                luoiTop + k.phutTu / 60f * gio - cuonY,
                trai + rong * (k.cot + 1),
                luoiTop + k.phutDen / 60f * gio - cuonY,
            )
            if (o.height() < dp(3f)) o.bottom = o.top + dp(3f)
            if (o.contains(x, y)) return k
        }
        return null
    }

    // ---- Vach gio tu nhich theo thoi gian ----

    private val nhichVachGio = object : Runnable {
        override fun run() {
            invalidate()
            postDelayed(this, 60_000L)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postDelayed(nhichVachGio, 60_000L)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(nhichVachGio)
        super.onDetachedFromWindow()
    }

    /** Mot khoi su kien trong mot ngay. Vi tri giu bang PHUT, doi ra pixel luc ve. */
    private class Khoi(
        val lan: LanHien,
        val phutTu: Float,
        val phutDen: Float,
        var cot: Int = 0,
        var soCot: Int = 1,
    ) {
        private var bo: StaticLayout? = null
        private var boRong = -1

        /** Dung [StaticLayout] mot lan cho moi be rong — dung lai khi chi cuon. */
        fun chu(rong: Int, but: TextPaint): StaticLayout {
            val san = bo
            if (san != null && boRong == rong) return san
            val chu = lan.suKien.tieuDe.ifBlank { "(không tên)" }
            val moi = StaticLayout.Builder.obtain(chu, 0, chu.length, but, rong)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.2f)
                .setIncludePad(false)
                .build()
            bo = moi
            boRong = rong
            return moi
        }
    }

    companion object {
        /** Gio bat dau hien thi. An 0h–6h — quy uoc lay tu ban demo. */
        const val GIO_DAU = 6
        const val SO_GIO = 24 - GIO_DAU

        /** So cot ngay thay duoc o muc thu nho nhat. */
        const val SO_COT_CHUAN = 7

        const val TI_LE_MIN = 1f
        const val TI_LE_MAX = 6f

        const val SO_NGAY_LUI = 365
        const val SO_NGAY_TOI = 365

        private const val VIEN_TRAI_DP = 2.5f
    }
}
