package com.ghichu

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * Tro ly AI — mot cua so tro chuyen kieu ChatGPT.
 *
 * BA QUY UOC DA CHOT VOI NGUOI DUNG, dung tu y doi:
 *
 * 1. **Khong co menu chon chuc nang.** Cu noi y muon, AI tu phan tich roi quyet
 *    dinh lam gi (xem [AiLoiNhac]). Menu chuc nang la thu nguoi dung da bo.
 *
 * 2. **AI KHONG tu chay.** No de xuat, man hinh hien mot THE XEM TRUOC, nguoi
 *    dung bam "Áp dụng" thi moi ghi vao du lieu that. Ly do nguoi dung neu ro:
 *    tranh hieu nham y roi sua bay vao ghi chu.
 *
 * 3. **Khong nhung API key vao APK.** Nguoi dung tu dan mot lan o day. Khoa
 *    Gemini lo la nguoi khac xai het han muc — khac han khoa Supabase.
 *
 * Lich su tro chuyen o NGAN KEO TRAI, nhieu cuoc song song — dung y ban chot.
 * Cac cuoc nam trong CSDL cua app ([KhoDuLieu.BANG_HOI_THOAI]) va **khong dong
 * bo len may chu**: doan chat hay co thu nguoi dung khong nghi la minh dang luu
 * tru.
 */
class TroLyAiActivity : Activity() {

    private lateinit var kho: KhoDuLieu
    private lateinit var khungTin: LinearLayout
    private lateinit var oCuon: ScrollView
    private lateinit var oNhap: EditText
    private lateinit var nutGui: Button

    private val nen = Executors.newSingleThreadExecutor()
    private val chinh = Handler(Looper.getMainLooper())
    private val doanChat = ArrayList<LuotChat>()

    private var dangHoi = false

    /** Cuoc tro chuyen dang mo. 0 = chua ghi xuong (chua noi gi). */
    private var hoiThoaiId = 0L

    private val mau by lazy { BangMau.cua(this) }
    private val md by lazy { resources.displayMetrics }
    private fun dp(v: Int) = (v * md.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Truoc moi thu khac: `Button`/`EditText` lay mau tu THEME, khong
        // tu BangMau — dat sai la nut den si tren nen trang.
        setTheme(BangMau.cua(this).themeManHinh)
        title = "Trợ lý AI"
        Gemini.khoiTao(this)
        kho = Ung.cua(this).kho

        setContentView(dungGiaoDien())
        napDoanChat()

        if (!Gemini.daCoKhoa) hienNhap("Chưa có API key. Bấm ⚙ để dán khóa miễn phí từ Google AI Studio.")
    }

    override fun onDestroy() {
        nen.shutdown()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Ngan keo dang mo thi back la dong no, chua phai roi man hinh.
        if (::bangNganKeo.isInitialized && bangNganKeo.visibility == View.VISIBLE) {
            dongNganKeo()
            return
        }
        super.onBackPressed()
    }

    // ---- Giao dien ----

    private fun dungGiaoDien(): View {
        val goc = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(mau.nen)
            setOnApplyWindowInsetsListener { v, insets ->
                val thanh = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
                v.setPadding(thanh.left, thanh.top, thanh.right, thanh.bottom)
                insets
            }
        }

        goc.addView(thanhTren(), LinearLayout.LayoutParams(MATCH, dp(52)))

        khungTin = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        oCuon = ScrollView(this).apply {
            isFillViewport = true
            addView(khungTin)
        }
        goc.addView(oCuon, LinearLayout.LayoutParams(MATCH, 0, 1f))

        goc.addView(thanhNhap(), LinearLayout.LayoutParams(MATCH, WRAP))

        // Ngan keo nam DE LEN tren cung, trong mot FrameLayout — de trong cot
        // doc thi no chiem cho va day man chat hep lai.
        return FrameLayout(this).apply {
            setBackgroundColor(mau.nen)
            addView(goc, FrameLayout.LayoutParams(MATCH, MATCH))
            addView(dungNganKeo())
        }
    }

    // ---- Ngan keo lich su tro chuyen ----

    private lateinit var manNganKeo: View
    private lateinit var bangNganKeo: LinearLayout
    private lateinit var danhSachChat: LinearLayout

    /**
     * Ngan keo trai: danh sach cac cuoc tro chuyen.
     *
     * Tu dung chu khong dung `DrawerLayout` — lop do nam trong AndroidX, ma ca
     * app co y khong keo AndroidX vao. Phan viec that su chi la mot tam mo va
     * mot bang truot, deu lam bang `translationX`.
     */
    private fun dungNganKeo(): View {
        val hop = FrameLayout(this)

        // Tam mo phia sau: bam vao la dong. Khong co no thi bam ra ngoai ngan keo
        // se cham thang vao man chat dang bi che.
        manNganKeo = View(this).apply {
            setBackgroundColor(0x99000000.toInt())
            alpha = 0f
            visibility = View.GONE
            setOnClickListener { dongNganKeo() }
        }
        hop.addView(manNganKeo, FrameLayout.LayoutParams(MATCH, MATCH))

        danhSachChat = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        bangNganKeo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(mau.nenThanhDuoi)
            visibility = View.GONE
            setPadding(0, 0, 0, dp(16))
            setOnApplyWindowInsetsListener { v, insets ->
                val thanh = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(0, thanh.top, 0, thanh.bottom + dp(16))
                insets
            }
            addView(
                TextView(this@TroLyAiActivity).apply {
                    text = "Lịch sử trò chuyện"
                    setTextColor(mau.chuChinh)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                    setPadding(dp(16), dp(16), dp(16), dp(8))
                },
            )
            addView(
                hangNganKeo("＋  Trò chuyện mới", mau.nhan) { chatMoi() },
                LinearLayout.LayoutParams(MATCH, WRAP),
            )
            addView(ScrollView(this@TroLyAiActivity).apply { addView(danhSachChat) })
        }
        hop.addView(
            bangNganKeo,
            FrameLayout.LayoutParams((md.widthPixels * 0.78f).toInt(), MATCH),
        )
        return hop
    }

    private fun hangNganKeo(chu: String, to: Int, bam: () -> Unit): TextView =
        TextView(this).apply {
            text = chu
            setTextColor(to)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            isClickable = true
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setOnClickListener { bam() }
        }

    private fun moNganKeo() {
        napDanhSachChat()
        val rong = (md.widthPixels * 0.78f)
        bangNganKeo.translationX = -rong
        bangNganKeo.visibility = View.VISIBLE
        manNganKeo.visibility = View.VISIBLE
        bangNganKeo.animate().translationX(0f).setDuration(180).start()
        manNganKeo.animate().alpha(1f).setDuration(180).start()
    }

    private fun dongNganKeo() {
        val rong = (md.widthPixels * 0.78f)
        bangNganKeo.animate().translationX(-rong).setDuration(160)
            .withEndAction { bangNganKeo.visibility = View.GONE }.start()
        manNganKeo.animate().alpha(0f).setDuration(160)
            .withEndAction { manNganKeo.visibility = View.GONE }.start()
    }

    private fun napDanhSachChat() {
        danhSachChat.removeAllViews()
        val ds = kho.danhSachHoiThoai()
        if (ds.isEmpty()) {
            danhSachChat.addView(
                TextView(this).apply {
                    text = "Chưa có cuộc trò chuyện nào"
                    setTextColor(mau.chuMo)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                },
            )
            return
        }
        for (t in ds) {
            val nhan = t.tieuDe.ifBlank { "Chưa đặt tên" } + XUONG + NgayVN.nhanDanhSach(t.ngaySua)
            val hang = hangNganKeo(nhan, if (t.id == hoiThoaiId) mau.nhan else mau.chuChinh) {
                moHoiThoai(t.id)
            }
            hang.setOnLongClickListener {
                hoiXoaHoiThoai(t.id, t.tieuDe)
                true
            }
            danhSachChat.addView(hang, LinearLayout.LayoutParams(MATCH, WRAP))
        }
    }

    private fun hoiXoaHoiThoai(id: Long, tieuDe: String) {
        android.app.AlertDialog.Builder(this, mau.themeHopThoai)
            .setTitle(tieuDe.ifBlank { "Cuộc trò chuyện" })
            .setMessage("Xoá cuộc trò chuyện này?")
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Xoá") { _, _ ->
                kho.xoaHoiThoai(id)
                if (id == hoiThoaiId) chatMoi() else napDanhSachChat()
            }
            .show()
    }

    private fun thanhTren(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4), 0, dp(8), 0)

        addView(nutChu("☰") { moNganKeo() })
        addView(
            TextView(this@TroLyAiActivity).apply {
                text = "Trợ lý AI"
                setTextColor(mau.chuChinh)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            },
            LinearLayout.LayoutParams(0, WRAP, 1f),
        )
        addView(nutChu("⚙") { hienCaiDat() })
    }

    private fun thanhNhap(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.BOTTOM
        setBackgroundColor(mau.nenThanhDuoi)
        setPadding(dp(14), dp(8), dp(8), dp(8))

        oNhap = EditText(this@TroLyAiActivity).apply {
            hint = "Nói bạn muốn gì…"
            setTextColor(mau.chuChinh)
            setHintTextColor(mau.chuMo)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 5
            background = null
        }
        addView(oNhap, LinearLayout.LayoutParams(0, WRAP, 1f))

        nutGui = Button(this@TroLyAiActivity).apply {
            text = "Gửi"
            isAllCaps = false
            setOnClickListener { gui() }
        }
        addView(nutGui)
    }

    private fun nutChu(chu: String, khiBam: () -> Unit) = TextView(this).apply {
        text = chu
        setTextColor(mau.nhan)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(8), dp(12), dp(8))
        isClickable = true
        setOnClickListener { khiBam() }
    }

    // ---- Hien tin nhan ----

    private fun hienNguoi(chu: String) = themTin(chu, laNguoi = true)
    private fun hienMay(chu: String) = themTin(chu, laNguoi = false)
    private fun hienNhap(chu: String) = themTin(chu, laNguoi = false, mo = true)

    private fun themTin(chu: String, laNguoi: Boolean, mo: Boolean = false) {
        val o = TextView(this).apply {
            text = chu
            setTextColor(if (mo) mau.chuMo else mau.chuChinh)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setLineSpacing(0f, 1.25f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (laNguoi) mau.bongNguoi else mau.bongMay)
            }
        }
        val lp = LinearLayout.LayoutParams(WRAP, WRAP).apply {
            topMargin = dp(6)
            gravity = if (laNguoi) Gravity.END else Gravity.START
            if (laNguoi) leftMargin = dp(48) else rightMargin = dp(48)
        }
        khungTin.addView(o, lp)
        cuonXuongCuoi()
    }

    /**
     * The xem truoc: hien AI dinh lam gi, co nut xac nhan.
     *
     * Khong co nut nay thi AI tu sua vao ghi chu — dung thu nguoi dung da tu choi.
     */
    private fun hienTheXemTruoc(dx: DeXuat) {
        val the = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(mau.bongMay)
                setStroke(dp(1), mau.nhan)
            }
        }

        the.addView(
            TextView(this).apply {
                text = dx.tomTat()
                setTextColor(mau.nhan)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            },
        )

        xemTruocChiTiet(dx)?.let { chiTiet ->
            the.addView(
                TextView(this).apply {
                    text = chiTiet
                    setTextColor(mau.chuPhu)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(6), 0, dp(2))
                    setLineSpacing(0f, 1.2f)
                },
            )
        }

        val hangNut = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        lateinit var nutAp: Button
        nutAp = Button(this).apply {
            text = "Áp dụng"
            isAllCaps = false
            setOnClickListener {
                isEnabled = false
                // Ghi CSDL + goi ContentProvider deu la vao dia — day sang luong
                // nen, khong thi ap mot thoi khoa bieu vai chuc buoi la giat man.
                nen.execute {
                    val kq = AiApDung.ap(this@TroLyAiActivity, kho, dx)
                    chinh.post {
                        if (isFinishing || isDestroyed) return@post
                        the.addView(
                            TextView(this@TroLyAiActivity).apply {
                                text = kq.loi
                                setTextColor(if (kq.xong) mau.nhan else mau.chuMo)
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                setPadding(0, dp(6), 0, 0)
                            },
                        )
                        // That bai thi cho bam lai; xong roi thi thoi, tranh ap hai lan.
                        if (!kq.xong) nutAp.isEnabled = true
                    }
                }
            }
        }
        hangNut.addView(nutAp)
        hangNut.addView(
            Button(this).apply {
                text = "Bỏ qua"
                isAllCaps = false
                setOnClickListener {
                    nutAp.isEnabled = false
                    isEnabled = false
                }
            },
        )
        the.addView(hangNut)

        khungTin.addView(
            the,
            LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(6) },
        )
        cuonXuongCuoi()
    }

    /** Vai dong dau cua thu AI dinh ghi — de nguoi dung liec qua truoc khi duyet. */
    private fun xemTruocChiTiet(dx: DeXuat): String? = when (dx.yDinh) {
        YDinh.TAO_GHI_CHU, YDinh.SUA_GHI_CHU ->
            dx.noiDungGhiChu.lines().take(6).joinToString("\n").let {
                if (dx.noiDungGhiChu.lines().size > 6) "$it\n…" else it
            }

        YDinh.SUA_LICH -> dx.themSuKien.take(6).joinToString("\n") { "• ${it.tieuDe}" }
            .ifEmpty { null }

        YDinh.CHAT -> null
    }

    private fun cuonXuongCuoi() {
        oCuon.post { oCuon.fullScroll(View.FOCUS_DOWN) }
    }

    // ---- Hoi AI ----

    private fun gui() {
        val chu = oNhap.text.toString().trim()
        if (chu.isEmpty() || dangHoi) return
        if (!Gemini.daCoKhoa) {
            hienCaiDat()
            return
        }

        oNhap.setText("")
        hienNguoi(chu)
        val lichSu = ArrayList(doanChat)
        doanChat += LuotChat(LuotChat.NGUOI, chu)
        datDangHoi(true)

        nen.execute {
            val ghiChu = KhoGhiChu(kho)
            val homNay = AiLoiNhac.homNay()
            val loiNhac = AiLoiNhac.loiNhac(
                homNay = homNay,
                dsGhiChu = ghiChu.banGon(AiLoiNhac.TRAN_GHI_CHU),
                dsSuKien = AiApDung.bangSuKien(kho, homNay),
            )

            val ketQua = try {
                val raw = Gemini.sinh(chu, heThong = loiNhac, lichSu = lichSu, jsonMode = true)
                AiDocDeXuat.doc(raw) to null
            } catch (e: Exception) {
                null to (e.message ?: e.toString())
            }

            chinh.post {
                if (isFinishing || isDestroyed) return@post
                datDangHoi(false)

                val (dx, loi) = ketQua
                if (dx == null) {
                    hienNhap("⚠️ $loi")
                    return@post
                }
                hienMay(dx.loiDap)
                doanChat += LuotChat(LuotChat.MAY, dx.loiDap)
                if (dx.coViecLam) hienTheXemTruoc(dx)
                luuDoanChat()
            }
        }
    }

    private fun datDangHoi(dang: Boolean) {
        dangHoi = dang
        nutGui.isEnabled = !dang
        nutGui.text = if (dang) "…" else "Gửi"
    }

    // ---- Cai dat khoa / model ----

    private fun hienCaiDat() {
        val than = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(12), dp(24), 0)
        }

        val oKhoa = EditText(this).apply {
            hint = "Dán API key của Google AI Studio"
            setText(Gemini.khoa)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        }
        than.addView(oKhoa)

        val oModel = TextView(this).apply {
            text = "Model: ${Gemini.model}"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(16), 0, dp(8))
            isClickable = true
            setOnClickListener {
                AlertDialog.Builder(this@TroLyAiActivity)
                    .setTitle("Chọn model")
                    .setItems(Gemini.MODEL.toTypedArray()) { _, i ->
                        Gemini.model = Gemini.MODEL[i]
                        text = "Model: ${Gemini.model}"
                    }
                    .show()
            }
        }
        than.addView(oModel)

        AlertDialog.Builder(this)
            .setTitle("Trợ lý AI")
            .setView(than)
            .setPositiveButton("Lưu") { _, _ ->
                Gemini.khoa = oKhoa.text.toString()
                hienNhap(if (Gemini.daCoKhoa) "Đã lưu API key." else "Chưa có API key.")
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    // ---- Nho doan chat ----

    /**
     * Nap cuoc tro chuyen dang mo len man hinh.
     *
     * Mo lai app thi vao dung cuoc gan nhat — nguoi dung khong phai di tim trong
     * lich su thu ho vua noi gi cach day ba phut.
     */
    private fun napDoanChat() {
        val gan = kho.danhSachHoiThoai(1).firstOrNull() ?: return
        moHoiThoai(gan.id, dongKeo = false)
    }

    private fun moHoiThoai(id: Long, dongKeo: Boolean = true) {
        hoiThoaiId = id
        doanChat.clear()
        khungTin.removeAllViews()

        try {
            val m = JSONArray(kho.docHoiThoai(id))
            for (i in 0 until m.length()) {
                val o = m.optJSONObject(i) ?: continue
                val vai = o.optString("vai")
                val chu = o.optString("chu")
                doanChat += LuotChat(vai, chu)
                themTin(chu, laNguoi = vai == LuotChat.NGUOI)
            }
        } catch (_: Exception) {
            // Doan chat cu hong thi bo qua, khong lam vo man hinh.
        }
        if (dongKeo) dongNganKeo()
        cuonXuongCuoi()
    }

    /**
     * Bat dau mot cuoc moi.
     *
     * Chi dat lai [hoiThoaiId] ve 0 — cuoc cu da nam trong CSDL, khong dung gi
     * toi no. Hang moi chi sinh ra khi nguoi dung noi cau dau tien (xem
     * [KhoDuLieu.luuHoiThoai]), nen mo tro ly roi thoat ngay khong de lai muc
     * trong nao.
     */
    private fun chatMoi() {
        hoiThoaiId = 0L
        doanChat.clear()
        khungTin.removeAllViews()
        dongNganKeo()
        hienNhap("Cuộc trò chuyện mới. Cứ nói bạn muốn gì.")
    }

    private fun luuDoanChat() {
        // Chi giu vai chuc luot gan nhat: gui het lich su cho AI vua ton tien
        // vua de vuot han muc ngu canh.
        val giu = doanChat.takeLast(TRAN_LUOT)
        if (giu.isEmpty()) return

        val m = JSONArray()
        for (l in giu) m.put(JSONObject().put("vai", l.vai).put("chu", l.chu))
        hoiThoaiId = kho.luuHoiThoai(hoiThoaiId, tenDoan(giu), m.toString())
    }

    /**
     * Ten cuoc tro chuyen = cau DAU TIEN cua nguoi dung.
     *
     * Khong nho AI dat ten: do la them mot luot goi mang co tien cho mot thu ma
     * cau dau tien von da noi ro.
     */
    private fun tenDoan(luot: List<LuotChat>): String =
        luot.firstOrNull { it.vai == LuotChat.NGUOI }?.chu?.trim()?.take(60).orEmpty()

    private fun caiDat() = getSharedPreferences(TEP_CAI_DAT, Context.MODE_PRIVATE)

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

        const val TEP_CAI_DAT = "ghichu_ai"
        const val TRAN_LUOT = 30
        const val XUONG = "\n"
    }
}
