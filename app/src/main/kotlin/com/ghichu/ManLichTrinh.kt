package com.ghichu

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Dung toan bo man Lich trinh — bam theo bo cuc cua ban demo Flutter
 * (D:\Project\Note, lib/screens/calendar_screen.dart):
 *
 * ```text
 * [ Tháng 8 ⌄                                    (4) ]   thanh tren
 * [ GMT+7 | 3  4  5  6  7  8  9 ]                        dau cot ngay
 * [ Cả ngày | ...                ]                       chi hien khi co
 * [ 06:00 |  luoi 6h -> 24h      ]                       vua khit man hinh
 * [ Họp GLV                   (+) ]                      thanh duoi
 * ```
 *
 * Tach rieng khoi Activity/Fragment vi man nay chay o hai cho: mo thang bang
 * `am start` (LichTrinhActivity) va nam trong tab thu hai cua app (LichTrinhFragment).
 * Hai vo, mot ruot.
 */
class ManLichTrinh(private val context: Context) {

    private val db = Ung.cua(context).kho
    private val zone: ZoneId = ZoneId.systemDefault()
    private val main = Handler(Looper.getMainLooper())
    private val nen = java.util.concurrent.Executors.newSingleThreadExecutor()

    private lateinit var tuanView: TuanView
    private lateinit var lichThang: LichThangView
    private lateinit var khungLich: FrameLayout
    private lateinit var nutCheDo: TextView
    private lateinit var oHomNay: TextView
    private lateinit var oSapToi: TextView
    private lateinit var oSapToiPhu: TextView

    private var ngayTrai: LocalDate = LocalDate.now().minusDays(1)

    /** Đang xem lịch THÁNG (true) hay lưới TUẦN (false). */
    private var cheDoThang = false

    /**
     * Ghi nhan thang dang xem len thanh tieu de CUA APP.
     *
     * Ban demo de "Tháng 8" to o dau man. O day khong dung thanh rieng nua ma muon
     * thang thanh tieu de cua `ContentFragment`: no da chiem san cho do va ve theo
     * dung kieu cua Xiaomi. Dung thanh rieng thi tieu de cua app ve DE LEN — no cao
     * toi ~420px trong khi `onContentInsetChanged` chi bao 265 (chieu cao luc thu lai).
     */
    var datTieuDe: ((String) -> Unit)? = null

    private val md = context.resources.displayMetrics
    private fun dp(v: Int) = (v * md.density).toInt()

    /** Doc mot lan luc dung giao dien; [nap] bao TuanView doc lai neu doi. */
    private val mau = BangMau.cua(context)

    fun taoView(): View {
        val goc = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(mau.nen)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        tuanView = TuanView(context).apply {
            onChon = { lan -> SuaSuKien(context, db, lan.suKien, null) { nap() }.hien() }
            onNgayTraiDoi = { ngay ->
                ngayTrai = ngay
                if (!cheDoThang) datTieuDe?.invoke(NgayVN.nhanThang(ngay))
            }
        }
        lichThang = LichThangView(context, mau).apply {
            visibility = View.GONE
            onChonNgay = { ngay ->
                // Chạm một ngày ở lịch tháng: về lưới TUẦN đúng ngày đó.
                cheDoTuan()
                tuanView.nhayToi(ngay)
            }
            onDoiThang = { m -> datTieuDe?.invoke(NgayVN.nhanThang(m.atDay(1))) }
        }
        khungLich = FrameLayout(context).apply {
            addView(tuanView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(lichThang, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        goc.addView(khungLich, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        goc.addView(thanhDuoi(), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        nap()
        return goc
    }

    /**
     * O vuong vang mang so ngay hom nay — lay tu ban demo.
     * Cham: ve tuan hien tai. Giu: chon ngay bat ky.
     */
    private fun oVuongHomNay(): View = TextView(context).apply {
        oHomNay = this
        text = "${LocalDate.now(zone).dayOfMonth}"
        setTextColor(mau.chuTrenNhan)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(9).toFloat()
            setColor(mau.nhan)
        }
        setOnClickListener { tuanView.veHomNay() }
        setOnLongClickListener {
            chonNgay()
            true
        }
    }

    private fun chonNgay() {
        DatePickerDialog(
            context, mau.themeHopThoai,
            { _, nam, thangTu0, ngay -> tuanView.nhayToi(LocalDate.of(nam, thangTu0 + 1, ngay)) },
            ngayTrai.year, ngayTrai.monthValue - 1, ngayTrai.dayOfMonth,
        ).show()
    }

    // ---- Thanh duoi: su kien sap toi + nut them ----

    private fun thanhDuoi(): View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(mau.nenThanhDuoi)
        setPadding(dp(20), dp(10), dp(12), dp(10))

        val cot = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            oSapToi = TextView(context).apply {
                setTextColor(mau.chuChinh)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            oSapToiPhu = TextView(context).apply {
                setTextColor(mau.chuMo)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                maxLines = 1
            }
            addView(oSapToi)
            addView(oSapToiPhu)
        }
        addView(cot, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(
            nutCheDoLich(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { marginEnd = dp(12) },
        )
        addView(
            oVuongHomNay(),
            LinearLayout.LayoutParams(dp(34), dp(34)).apply { marginEnd = dp(12) },
        )
        addView(nutThem(), LinearLayout.LayoutParams(dp(46), dp(46)))
    }

    /** Nút chuyển Tuần ⇄ Tháng. Chữ đổi theo chế độ đang xem. */
    private fun nutCheDoLich(): View = TextView(context).apply {
        nutCheDo = this
        text = "Tháng"
        setTextColor(mau.chuChinh)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(6), dp(12), dp(6))
        background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(BangMau.mauMo(mau.chuChinh, 0.08f))
        }
        setOnClickListener { doiCheDo() }
    }

    private fun doiCheDo() {
        if (cheDoThang) cheDoTuan() else cheDoThang()
    }

    private fun cheDoThang() {
        cheDoThang = true
        lichThang.datThang(YearMonth.from(ngayTrai))
        lichThang.visibility = View.VISIBLE
        tuanView.visibility = View.GONE
        nutCheDo.text = "Tuần"
        datTieuDe?.invoke(NgayVN.nhanThang(lichThang.thangDangXem().atDay(1)))
    }

    private fun cheDoTuan() {
        cheDoThang = false
        lichThang.visibility = View.GONE
        tuanView.visibility = View.VISIBLE
        nutCheDo.text = "Tháng"
        datTieuDe?.invoke(NgayVN.nhanThang(ngayTrai))
    }

    /**
     * Nut + : cham nhanh = su kien moi, giu 0,5 giay = AI.
     *
     * Dung [MotionEvent] + hen gio chu KHONG dung `setOnLongClickListener` — day la
     * ket luan tu ban demo: boc long-press ngoai nut gay tranh chap cu chi, phai
     * canh moi trung. Phan AI chua lam nen tam bao mot cau, giu dung cu chi de
     * sau nay noi vao khoi phai sua lai.
     */
    private fun nutThem(): View = TextView(context).apply {
        text = "＋"
        setTextColor(mau.chuTrenNhan)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(mau.nhan)
        }

        var daChayAi = false
        val giuDeMoAi = Runnable {
            daChayAi = true
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            context.startActivity(
                android.content.Intent(context, TroLyAiActivity::class.java)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }

        setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    daChayAi = false
                    main.postDelayed(giuDeMoAi, 500L)
                    v.alpha = 0.75f
                }

                MotionEvent.ACTION_UP -> {
                    main.removeCallbacks(giuDeMoAi)
                    v.alpha = 1f
                    if (!daChayAi) themSuKien()
                }

                MotionEvent.ACTION_CANCEL -> {
                    main.removeCallbacks(giuDeMoAi)
                    v.alpha = 1f
                }
            }
            true
        }
    }

    /**
     * Goi y gio bat dau giong ban demo: dang xem hom nay thi lay gio tron ke tiep,
     * xem ngay khac thi 9 gio sang.
     */
    private fun themSuKien() {
        val homNay = LocalDate.now(zone)
        val moc = if (ngayTrai == homNay) {
            java.time.LocalDateTime.now(zone).withMinute(0).withSecond(0).withNano(0).plusHours(1)
        } else {
            ngayTrai.atTime(9, 0)
        }
        SuaSuKien(context, db, null, moc) { nap() }.hien()
    }

    // ---- Nap du lieu ----

    /**
     * Doc lai CSDL roi ve lai.
     *
     * Lay ca mot nam truoc va mot nam sau vi luoi cuon ngang lien tuc trong khoang
     * do; so su kien ca nhan nho nen doc mot lan van nhanh hon doc lai moi lan cuon.
     */
    fun nap() {
        tuanView.doiBangMau()
        val homNay = LocalDate.now(zone)
        val ds = db.trongKhoang(
            NgayVN.dauNgay(homNay.minusDays(TuanView.SO_NGAY_LUI.toLong()), zone),
            NgayVN.dauNgay(homNay.plusDays(TuanView.SO_NGAY_TOI.toLong()), zone),
        )
        tuanView.datDuLieu(ds, zone)
        if (::lichThang.isInitialized) lichThang.datDuLieu(ds, zone)
        oHomNay.text = "${homNay.dayOfMonth}"
        // Doc `ngayTrai` cua minh chu khong hoi TuanView: luc goi lan dau view chua
        // do xong, be rong con bang 0 nen no tra ve ngay dau dai cuon (nam ngoai).
        if (cheDoThang && ::lichThang.isInitialized) {
            datTieuDe?.invoke(NgayVN.nhanThang(lichThang.thangDangXem().atDay(1)))
        } else {
            datTieuDe?.invoke(NgayVN.nhanThang(ngayTrai))
        }
        capNhatSapToi(ds)
    }

    /** Su kien gan nhat con chua ket thuc, tinh ca lan lap ke tiep. */
    private fun capNhatSapToi(ds: List<SuKien>) {
        val bayGio = System.currentTimeMillis()
        val toi = bayGio + 60L * 24 * 3_600_000L

        val gan = ds.flatMap { LichLapLai.moRong(it, bayGio, toi, zone) }.minByOrNull { it.batDau }
        if (gan == null) {
            oSapToi.text = "Không có sự kiện sắp tới"
            oSapToiPhu.visibility = View.GONE
        } else {
            oSapToi.text = gan.suKien.tieuDe.ifBlank { "(không tên)" }
            oSapToiPhu.text = NgayVN.nhanSapToi(gan.suKien, gan.batDau, zone)
            oSapToiPhu.visibility = View.VISIBLE
        }
    }

    /**
     * Dong bo lang le moi lan mo man nay, toi da 2 phut mot lan.
     *
     * Dung thang the CSDL chung cua app: no song suot doi tien trinh, khong ai
     * dong no — nen mot luot dong bo keo dai vai giay van an toan du nguoi dung
     * da roi man hinh.
     */
    fun dongBoNgam() {
        DongBo.khoiTao(context)
        if (!DongBo.daDangNhap || DongBo.dangChay) return
        if (System.currentTimeMillis() - DongBo.lanCuoi < CACH_NHAU_MS) return

        nen.execute {
            val doi = DongBo.motLuot(context, db)
            if (doi) main.post { nap() }
        }
    }

    fun dong() {
        main.removeCallbacksAndMessages(null)
        nen.shutdown()
    }

    private companion object {
        const val CACH_NHAU_MS = 120_000L
    }
}
