package com.ghichu

import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Man hinh chinh: luoi ghi chu, tim kiem, thu muc, thung rac, rieng tu.
 *
 * DUNG THEO SO DO CUA APP GOC — `docs/SPEC-GOC.md`. Moi con so trong file nay
 * (56dp, 32sp, 44dp, 58dp, 35dp, 64dp…) deu la gia tri trich tu tai nguyen cua
 * app Ghi chu Xiaomi 2.4.2.5, ten goc ghi canh. Dung sua "cho hop mat" ma chua
 * do lai.
 *
 * Cau truc tu tren xuong (nhu miuix ActionBar + NestedHeaderLayout cua ban goc):
 *   1. thanh 56dp: tieu de nho 20sp o GIUA (chi hien khi da cuon) + icon ben phai;
 *   2. tieu de LON 32sp ben trai (mo dan khi cuon — cross-fade voi tieu de nho);
 *   3. thanh tim kiem: hang 56dp chua vien thuoc 44dp;
 *   4. luoi so le hai cot;
 *   5. thanh tab 64dp duoi cung; nut them 58dp noi tren thanh tab 35dp.
 *
 * Ba tab duoi — Ghi chu / Lich trinh / Tro ly — la ba MAN rieng chu khong phai
 * fragment: hai man kia mang nguyen tu ban truoc sang, moi cai da tu lo vong
 * doi cua no.
 */
class ManHinhChinhActivity : Activity() {

    private val md by lazy { resources.displayMetrics }
    private fun dp(v: Int) = (v * md.density).toInt()
    private fun dpF(v: Float) = v * md.density

    private lateinit var mau: BangMau
    private val kho: KhoGhiChu get() = Ung.cua(this).ghiChu

    /** `null` = tat ca thu muc. Chuoi rong = ghi chu chua xep thu muc. */
    private var thuMuc: String? = null
    private var tim = ""
    private var thungRac = false
    private var riengTu = false

    /**
     * Cac ghi chu dang duoc chon (che do chon nhieu).
     *
     * Rong = khong o che do chon. Giu id chu khong giu doi tuong: sau moi lan
     * [nap] danh sach la doi tuong moi, giu doi tuong thi vua sua mot ghi chu la
     * mat dau chon.
     */
    private val dangChon = LinkedHashSet<Long>()

    /** Cach nhau it nhat bay nhieu lau moi dong bo ngam mot luot. */
    private val CACH_DONG_BO_MS = 2 * 60 * 1000L

    private var duLieu: List<GhiChu> = emptyList()
    private val the = HashMap<Long, TheGhiChu>()

    /** Dang o che do tim (thanh tim len dinh, nut Huy, lop mo). */
    private var dangTim = false
    private lateinit var nutHuyTim: TextView
    private lateinit var lopMoTim: View
    private lateinit var hangTim: FrameLayout

    private lateinit var goc: LinearLayout
    private lateinit var tieuDeLon: TextView
    private lateinit var tieuDeNho: TextView
    private lateinit var oTim: EditText
    private lateinit var luoi: LuoiSoLe
    private lateinit var thanhTren: FrameLayout
    private lateinit var thanhChon: LinearLayout
    private lateinit var thanhDuoi: LinearLayout
    private lateinit var thanhDuoiChon: LinearLayout
    private lateinit var oTrong: LinearLayout
    private lateinit var nutThem: View

    private var demDuoi = 0

    /** 0 = Ghi chu, 1 = Lich trinh. Thanh tab LUON hien — hai tab la hai vung noi dung. */
    private var tabHienTai = 0
    private lateinit var khungGhiChu: LinearLayout
    private lateinit var khungNoiDung: FrameLayout
    private var khungLich: View? = null
    private var manLich: ManLichTrinh? = null
    private val cacTab = ArrayList<Pair<ImageView, TextView>>()
    private lateinit var nutQr: View
    private lateinit var nutThuMuc: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mau = BangMau.cua(this)
        setTheme(mau.themeManHinh)

        // Ve tran vien, thanh trang thai trong suot — nhu theme goc
        // (windowDrawsSystemBarBackgrounds + statusBarColor trong suot).
        window.statusBarColor = 0
        window.navigationBarColor = 0
        window.decorView.systemUiVisibility = if (mau.toi) 0 else
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        // Nen cua so = nen trang, khong thi luc mo app chop trang mot cai truoc
        // khi noi dung ve xong (windowBackground cua theme la trang tinh).
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(mau.nen))

        goc = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(mau.nen)
        }
        goc.addView(dungThanhTren())
        goc.addView(dungTieuDeLon())

        // Vung noi dung: hai tab la hai khung chong nhau, doi bang visibility —
        // thanh tren + thanh tab dung yen, dung nhu Navigator cua ban goc.
        khungGhiChu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        khungGhiChu.addView(dungOTim())
        khungGhiChu.addView(dungHangChip())
        khungGhiChu.addView(dungLuoi(), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
        ))
        val noiDung = FrameLayout(this)
        noiDung.addView(khungGhiChu, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        goc.addView(noiDung, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        khungNoiDung = noiDung
        goc.addView(dungThanhDuoi())

        val nen = FrameLayout(this).apply {
            setBackgroundColor(mau.nen)
            addView(goc)
            addView(dungNutThem())
        }
        nen.setOnApplyWindowInsetsListener { _, insets ->
            val thanh = insets.getInsets(WindowInsets.Type.systemBars())
            goc.setPadding(0, thanh.top, 0, 0)
            // Thanh tab: 64dp + thanh dieu huong he thong ben duoi.
            thanhDuoi.setPadding(0, 0, 0, thanh.bottom)
            thanhDuoiChon.setPadding(0, 0, 0, thanh.bottom)
            // Nut them: bottomMargin = inset duoi (thanh tab 64dp + nav bar) + 35dp
            // (floating_action_bar_margin_bottom) — luon noi 35dp tren mep thanh tab.
            (nutThem.layoutParams as FrameLayout.LayoutParams).bottomMargin = dp(64) + thanh.bottom + dp(35)
            nutThem.requestLayout()
            // Luoi cuon xuyen duoi nut them: chua cho the cuoi khong bi che.
            demDuoi = dp(35) + dp(58) + dp(24)
            luoi.datDemDuoi(demDuoi)
            insets
        }
        setContentView(nen)
        // Mo thang tab theo yeu cau (widget Lich trinh, kiem thu): doi layout xong roi doi.
        if (intent.getIntExtra(KHOA_TAB, 0) == 1) nen.post { chuyenTab(1) }

        xinQuyenThongBao()

        // Don dep dinh ky, chay o luong khac: ca ba viec deu quet toan bo CSDL,
        // lam o luong chinh la mo app thay khung mot cai khi co nhieu ghi chu.
        Thread {
            kho.donRacQuaHan()
            KhoAnh.donTepThua(this, Ung.cua(this).kho)
            KhoGhiAm.donTepThua(this, Ung.cua(this).kho)
        }.start()
    }

    /**
     * Xin quyen hien thong bao (Android 13 tro len).
     *
     * Xin o day chu khong xin luc dat nhac nho dau tien: he thong chi cho hoi
     * MOT lan: hoi lan sau la hop khong hien nua.
     */
    private fun xinQuyenThongBao() {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        val quyen = android.Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(quyen) == android.content.pm.PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(quyen), 1)
    }

    override fun onStop() {
        super.onStop()
        // Roi app la khoa lai. Do la ca diem cua tinh nang: mo khoa mot lan roi
        // de mo mai thi ai cam may cung doc duoc, y het nhu khong ma hoa.
        if (isFinishing || !isChangingConfigurations) {
            KhoaRiengTu.khoaLai()
            if (riengTu) doiCho(null, false)
        }
    }

    override fun onResume() {
        super.onResume()
        // Doi giao dien sang/toi trong Cai dat thi phai dung lai ca man hinh —
        // mau da to thang vao tung view, khong tu doi duoc.
        if (mau.toi != BangMau.laToi(this)) {
            recreate()
            return
        }
        if (tabHienTai == 1) manLich?.nap() else nap()
        dongBoNgam()
    }

    override fun onDestroy() {
        manLich?.dong()
        super.onDestroy()
    }

    /**
     * Keo/day mot luot o luong nen moi lan quay lai man hinh chinh.
     *
     * Co gian [CACH_DONG_BO_MS] giua hai luot: `onResume` chay ca khi tat man
     * hinh bat lai hay dong mot hop thoai — khong gian thi mot buoi chieu la vai
     * tram luot goi mang. Loi mang thi im lang: day la luot NGAM.
     */
    private fun dongBoNgam() {
        if (!DongBo.daDangNhap || DongBo.dangChay) return
        if (System.currentTimeMillis() - DongBo.lanCuoi < CACH_DONG_BO_MS) return

        val kho = Ung.cua(this).kho
        Thread {
            val doi = DongBo.motLuot(this, kho)
            if (doi) runOnUiThread { if (!isFinishing && !isDestroyed) nap() }
        }.start()
    }

    // ------------------------------------------------------------ thanh tren

    /**
     * Thanh 56dp (miuix_appcompat_action_bar_default_height): tieu de nho 20sp
     * can giua (an khi o dau trang), icon ben phai, mep phai 20dp.
     *
     * Thanh chon nhieu nam CHONG len trong cung FrameLayout va doi bang alpha
     * (mo dan ~300ms nhu ActionBarContextView cua ban goc).
     */
    private fun dungThanhTren(): View {
        thanhTren = FrameLayout(this)

        tieuDeNho = TextView(this).apply {
            text = "Ghi chú"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(mau.chuChinh)
            alpha = 0f
        }
        thanhTren.addView(tieuDeNho, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
        ).apply { gravity = Gravity.CENTER })

        val phai = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, dp(20) - dp(9), 0)
        }
        // Thu tu ban chot: QR nam BEN TRAI hai nut thu muc va cai dat.
        nutQr = nutAnh(R.drawable.ghichu_ic_qr, "Quét QR") {
            startActivity(Intent(this, QuetQrActivity::class.java))
        }
        nutThuMuc = nutAnh(R.drawable.ic_thu_muc, "Thư mục") { hienThuMuc() }
        phai.addView(nutQr)
        phai.addView(nutThuMuc)
        phai.addView(nutAnh(R.drawable.ic_cai_dat, "Cài đặt") { hienCaiDat() })
        thanhTren.addView(phai, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, dp(56),
        ).apply { gravity = Gravity.END })

        thanhChon = dungThanhChon()
        thanhTren.addView(thanhChon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(56)))

        return thanhTren.also {
            it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
        }
    }

    /**
     * Tieu de LON 32sp thuong (miuix_font_size_title1, MiSans Regular), can trai.
     * Le trai 46dp = 20dp (action bar) + 26dp (title container); tren 4dp, duoi 10dp.
     */
    private fun dungTieuDeLon(): View {
        tieuDeLon = TextView(this).apply {
            text = "Ghi chú"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(mau.chuChinh)
            setPadding(dp(46), dp(4), dp(46), dp(10))
            isSingleLine = true
        }
        return tieuDeLon
    }

    /**
     * Thanh chon nhieu: Huy ben trai, "Da chon N" o giua, Chon tat ca ben phai.
     */
    private fun dungThanhChon(): LinearLayout {
        val thanh = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
            visibility = View.GONE
            alpha = 0f
            setBackgroundColor(mau.nen)
        }
        thanh.addView(nutChu("Huỷ") { thoatChon() })
        thanh.addView(TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(mau.chuChinh)
            gravity = Gravity.CENTER
            tag = "nhan_chon"
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        thanh.addView(nutChu("Chọn tất cả") { chonTatCa() }.also { it.tag = "nut_chon_tat_ca" })
        return thanh
    }

    /** Nut chu cua thanh chon: 14sp medium (miuix_font_size_body2), toi da 40dp cao. */
    private fun nutChu(chu: String, bam: () -> Unit): TextView = TextView(this).apply {
        text = chu
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(mau.chuChinh)
        setPadding(dp(4), dp(10), dp(4), dp(10))
        isClickable = true
        setOnClickListener { bam() }
        nhanMoDan(this)
    }

    // ------------------------------------------------------------ tim kiem

    /**
     * Thanh tim kiem: hang 56dp (miuix_appcompat_search_view_default_height),
     * vien thuoc 44dp (searchbar_bg_height) bo tron han, cach hai mep 12dp, tren
     * 8dp duoi 4dp; kinh lup 24dp khung / 18dp net, alpha 0.3, paddingStart 12dp,
     * cach chu 8dp; chu 17sp medium, hint #4D000000.
     */
    private fun dungOTim(): View {
        val hang = FrameLayout(this)
        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpF(72f)
                setColor(mau.nenTimKiem)
            }
            setPadding(dp(12), 0, 0, 0)
        }
        val kinhLup = ImageView(this).apply {
            setImageResource(R.drawable.ic_tim)
            setPadding(dp(3), dp(3), dp(3), dp(3))
            setColorFilter(if (mau.toi) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            alpha = 0.3f
        }
        pill.addView(kinhLup, LinearLayout.LayoutParams(dp(24), dp(24)).apply { rightMargin = dp(8) })

        oTim = EditText(this).apply {
            hint = "Tìm kiếm ghi chú"
            setHintTextColor(mau.chuGoiY)
            setTextColor(mau.chuChinh)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isSingleLine = true
            background = null
            setPadding(0, 0, dp(16), 0)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    tim = s?.toString().orEmpty()
                    nap()
                }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            })
        }
        pill.addView(oTim, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        // Cham vao o tim = vao che do tim (ban goc: ca thanh la mot nut mo action mode).
        oTim.setOnFocusChangeListener { _, co -> if (co) vaoCheDoTim() }

        // Hang = pill + nut Huy (ban dau an, truot tu ngoai mep phai vao khi tim).
        val hangNgang = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        hangNgang.addView(pill, LinearLayout.LayoutParams(0, dp(44), 1f))
        // Nut Huy: 17sp medium, xanh #3482FF / #4788FF, marginStart 10dp, marginEnd
        // 14dp (tru 12dp le hang), dem ngang 10dp, cao 44dp, khong ripple.
        nutHuyTim = TextView(this).apply {
            text = "Hủy"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(if (mau.toi) 0xFF4788FF.toInt() else 0xFF3482FF.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(10), 0, dp(10), 0)
            visibility = View.GONE
            isClickable = true
            setOnClickListener { thoatCheDoTim() }
            nhanMoDan(this, 1f, 0.7f)
        }
        hangNgang.addView(nutHuyTim, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)).apply {
            leftMargin = dp(10)
            rightMargin = dp(2)
        })

        hang.addView(hangNgang, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
            leftMargin = dp(12)
            rightMargin = dp(12)
            topMargin = dp(8)
            bottomMargin = dp(4)
        })
        hang.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
        hangTim = hang
        return hang
    }

    // ------------------------------------------------------------ che do tim

    /**
     * Vao che do tim (SearchActionModeView cua ban goc): thanh tieu de + hang chip
     * an di nen pill len sat dinh; nut Huy truot tu mep phai vao; lop mo phu noi
     * dung (bam vao thi thoat); ban phim tu bat. 400ms.
     */
    private fun vaoCheDoTim() {
        if (dangTim) return
        dangTim = true
        for (v in listOf(thanhTren, tieuDeLon, hangChip)) {
            v.animate().alpha(0f).setDuration(200).withEndAction { if (dangTim) v.visibility = View.GONE }.start()
        }
        nutThem.visibility = View.GONE
        nutHuyTim.visibility = View.VISIBLE
        nutHuyTim.translationX = dpF(80f)
        nutHuyTim.animate().translationX(0f).setDuration(400).setInterpolator(android.view.animation.DecelerateInterpolator(1.5f)).start()
        lopMoTim.visibility = View.VISIBLE
        lopMoTim.alpha = 0f
        lopMoTim.animate().alpha(1f).setDuration(400).start()
        oTim.requestFocus()
        getSystemService(android.view.inputmethod.InputMethodManager::class.java)?.showSoftInput(oTim, 0)
    }

    private fun thoatCheDoTim() {
        if (!dangTim) return
        dangTim = false
        oTim.setText("")
        oTim.clearFocus()
        getSystemService(android.view.inputmethod.InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(oTim.windowToken, 0)
        for (v in listOf(thanhTren, tieuDeLon, hangChip)) {
            if (v === hangChip && hangChip.tag == "an") continue
            v.visibility = View.VISIBLE
            v.animate().alpha(1f).setDuration(200).start()
        }
        nutThem.visibility = View.VISIBLE
        nutHuyTim.animate().translationX(dpF(80f)).setDuration(300)
            .withEndAction { nutHuyTim.visibility = View.GONE }.start()
        lopMoTim.animate().alpha(0f).setDuration(300).withEndAction { lopMoTim.visibility = View.GONE }.start()
        luoi.smoothScrollTo(0, 0)
        nap()
    }

    // ------------------------------------------------------------ chip thu muc

    private lateinit var hangChip: android.widget.HorizontalScrollView
    private lateinit var dayChip: LinearLayout

    /**
     * Hang chip thu muc ghim tren luoi (sticky_view cua ban goc) — so do o
     * docs/SPEC-GOC-chip.md: hang cao 52dp (chip 40dp + tren 4dp + duoi 8dp), le
     * ngang 12dp, chip cach nhau 12dp, KHONG mo mep. "Tất cả" dau, thu muc nguoi
     * dung giua, "Chưa phân loại" cuoi. An khi chi co mot chip hoac dang o trang
     * rieng tu / thung rac — dung nhu `setFolderVisible` cua ban goc.
     */
    private fun dungHangChip(): View {
        dayChip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        hangChip = android.widget.HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(dp(12), dp(4), dp(12), dp(8))
            clipToPadding = false
            addView(dayChip)
            visibility = View.GONE
        }
        return hangChip
    }

    private fun napChip() {
        val ds = kho.thuMucKemSo().map { it.first }
        if (thungRac || riengTu || ds.isEmpty()) {
            hangChip.visibility = View.GONE
            hangChip.tag = "an"
            return
        }
        hangChip.tag = null
        if (!dangTim) hangChip.visibility = View.VISIBLE
        dayChip.removeAllViews()

        val muc = ArrayList<Pair<String, String?>>()
        muc += "Tất cả" to null
        for (t in ds) muc += t to t
        if (kho.soLuong("") > 0) muc += "Chưa phân loại" to ""

        var chipChon: View? = null
        for ((i, m) in muc.withIndex()) {
            val (ten, gia) = m
            val chon = gia == thuMuc
            val chip = chipThuMuc(ten, chon) { doiCho(gia, false) }
            if (chon) chipChon = chip
            dayChip.addView(chip, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(40),
            ).apply { if (i < muc.size - 1) rightMargin = dp(12) })
        }
        // Chip dang chon luon lo ra — smoothScrollToPosition cua ban goc.
        chipChon?.let { c -> hangChip.post { hangChip.smoothScrollTo((c.left - dp(12)).coerceAtLeast(0), 0) } }
    }

    /**
     * Mot chip: minHeight 40dp, dem ngang 10dp, bo goc 12dp, chu 14sp. Thuong:
     * nen #FFFFFF vien 1dp #24FFFFFF (sang) / trong suot vien #33FFFFFF (toi), chu
     * #99000000 / #80FFFFFF. Chon: nen #0F000000 / #24FFFFFF khong vien, chu
     * #000000 / #CCFFFFFF dam.
     */
    private fun chipThuMuc(ten: String, chon: Boolean, bam: () -> Unit): TextView =
        TextView(this).apply {
            text = ten
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
            maxWidth = dp(227)
            includeFontPadding = false
            gravity = Gravity.CENTER
            setPadding(dp(10), 0, dp(10), 0)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create("sans-serif-medium", if (chon) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(
                when {
                    chon && mau.toi -> 0xCCFFFFFF.toInt()
                    chon -> 0xFF000000.toInt()
                    mau.toi -> 0x80FFFFFF.toInt()
                    else -> 0x99000000.toInt()
                },
            )
            background = GradientDrawable().apply {
                cornerRadius = dpF(12f)
                if (chon) {
                    setColor(if (mau.toi) 0x24FFFFFF else 0x0F000000)
                } else {
                    setColor(if (mau.toi) 0x00000000 else 0xFFFFFFFF.toInt())
                    setStroke(dp(1), if (mau.toi) 0x33FFFFFF else 0x24FFFFFF)
                }
            }
            isClickable = true
            setOnClickListener { bam() }
            nhanMoDan(this)
        }

    // ------------------------------------------------------------ luoi

    private fun dungLuoi(): View {
        val hop = FrameLayout(this)

        luoi = LuoiSoLe(this)
        luoi.datThanhCuon(mau.toi)
        luoi.khiCuon = { t -> thuGonTieuDe(t) }
        hop.addView(luoi, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
        ))

        // Trang thai rong: icon 68dp (note_blank_icon, alpha 0.27) tren, chu 17sp
        // medium duoi cach 8dp, cach dinh 154dp (phone_note_blank_view_margin_top).
        oTrong = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
            setPadding(dp(24), dp(154), dp(24), 0)
        }
        oTrong.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_trong)
            setColorFilter(if (mau.toi) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            alpha = 0.27f
        }, LinearLayout.LayoutParams(dp(68), dp(68)).apply { bottomMargin = dp(8) })
        oTrong.addView(TextView(this).apply {
            tag = "chu_trong"
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(if (mau.toi) 0x80FFFFFF.toInt() else 0x99000000.toInt())
        })
        hop.addView(oTrong, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
        ))

        // Lop mo cua che do tim (search_mask): phu toan bo noi dung duoi thanh tim,
        // #20000000 / #CC000000; co chu thi an, bam vao thi thoat.
        lopMoTim = View(this).apply {
            setBackgroundColor(if (mau.toi) 0xCC000000.toInt() else 0x20000000)
            visibility = View.GONE
            isClickable = true
            setOnClickListener { thoatCheDoTim() }
        }
        hop.addView(lopMoTim, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        return hop
    }

    /**
     * Cuon xuong: tieu de lon mo dan, tieu de nho o giua thanh 56dp hien dan —
     * cross-fade theo alpha, dung nhu hai container title cua miuix ActionBar.
     */
    private fun thuGonTieuDe(t: Int) {
        val cao = tieuDeLon.height.coerceAtLeast(1)
        val f = (t.toFloat() / cao).coerceIn(0f, 1f)
        tieuDeLon.alpha = 1f - f
        tieuDeNho.alpha = f
    }

    private fun dungThe(g: GhiChu): TheGhiChu {
        val v = the[g.id] ?: TheGhiChu(this).also { the[g.id] = it }
        v.gan(g, mau, g.id in dangChon, dangChon.isNotEmpty(), toSang = if (dangTim) tim else "")
        v.setOnClickListener { chamVao(g, v) }
        v.setOnLongClickListener {
            if (!thungRac) {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                doiChon(g.id)
            }
            true
        }
        return v
    }

    // ------------------------------------------------------------ thanh duoi

    /**
     * Thanh tab 64dp (miuix_design_bottom_navigation_height): vach 0.5dp tren,
     * nen = nen trang (tab_bottom_surface #F7F7F7 / #000000), icon 28dp + chu
     * 12sp, padding tren 8dp duoi 16dp. Chon = den/trang, khong chon = mo hon.
     * KHONG phai vang.
     */
    private fun dungThanhDuoi(): View {
        val hop = FrameLayout(this)

        thanhDuoi = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(mau.nen)
        }
        thanhDuoi.addView(View(this).apply { setBackgroundColor(mau.vachThanh) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (0.5f * md.density).toInt().coerceAtLeast(1)))
        val hangTab = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(16))
        }
        // Ban goc co dung HAI tab (Ghi chu / Nhiem vu); o day Nhiem vu thay bang
        // Lich trinh theo yeu cau. KHONG co tab Tro ly — AI vao bang giu nut +.
        hangTab.addView(tabDuoi(R.drawable.ic_ghi_chu, "Ghi chú", true) { chuyenTab(0) })
        hangTab.addView(tabDuoi(R.drawable.ic_lich, "Lịch trình", false) { chuyenTab(1) })
        thanhDuoi.addView(hangTab)
        hop.addView(thanhDuoi)

        // Thanh menu day khi chon nhieu (split action bar cua ban goc): truot len
        // de len thanh tab.
        thanhDuoiChon = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(mau.nen)
            visibility = View.GONE
        }
        thanhDuoiChon.addView(View(this).apply { setBackgroundColor(mau.vachThanh) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (0.5f * md.density).toInt().coerceAtLeast(1)))
        // Thanh menu day cua action mode: dem ngang 8dp, nut chia deu cach 11dp,
        // moi nut icon 28dp tren + chu 11dp duoi, dem doc 18dp, chu #CC000000.
        val hangChon = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        hangChon.addView(nutMenuDay(R.drawable.ic_ghim, "Ẩn") { anDaChon() })
        hangChon.addView(nutMenuDay(R.drawable.ic_goc_ghim, "Ghim") { ghimDaChon() }.also {
            (it.layoutParams as LinearLayout.LayoutParams).leftMargin = dp(11)
        })
        hangChon.addView(nutMenuDay(R.drawable.ic_thu_muc, "Chuyển tới") { chuyenDaChon() }.also {
            (it.layoutParams as LinearLayout.LayoutParams).leftMargin = dp(11)
        })
        hangChon.addView(nutMenuDay(R.drawable.ic_xoa, "Xóa") { xoaDaChon() }.also {
            (it.layoutParams as LinearLayout.LayoutParams).leftMargin = dp(11)
        })
        thanhDuoiChon.addView(hangChon)
        hop.addView(thanhDuoiChon)
        return hop
    }

    /** Nut cua thanh menu day (Widget.ActionMode.ActionButton): icon 28dp + chu 11dp, dem doc 18dp. */
    private fun nutMenuDay(anh: Int, ten: String, bam: () -> Unit): View {
        val cot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isClickable = true
            setPadding(dp(3), dp(18), dp(3), dp(18))
            setOnClickListener { bam() }
        }
        cot.addView(ImageView(this).apply {
            setImageResource(anh)
            setColorFilter(mau.chuToolbar)
        }, LinearLayout.LayoutParams(dp(28), dp(28)))
        cot.addView(TextView(this).apply {
            text = ten
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f)
            setTextColor(mau.chuToolbar)
            maxLines = 2
            gravity = Gravity.CENTER_HORIZONTAL
            includeFontPadding = false
        })
        nhanMoDan(cot)
        cot.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        return cot
    }

    /** "Ẩn" = chuyen vao Rieng tu. Can khoa dang mo de ma hoa. */
    private fun anDaChon() {
        if (!KhoaRiengTu.dangMo) {
            Toast.makeText(this, "Mở khoá mục Riêng tư (Thư mục › Riêng tư) trước", Toast.LENGTH_LONG).show()
            return
        }
        for (id in dangChon) {
            val g = kho.theoId(id) ?: continue
            kho.luu(GhiChu(
                id = g.id, uid = g.uid, tieuDe = g.tieuDe, noiDung = g.noiDung, thuMuc = g.thuMuc,
                ghim = g.ghim, mau = g.mau, riengTu = true, nhacLuc = g.nhacLuc, ngayTao = g.ngayTao,
            ))
        }
        val n = dangChon.size
        thoatChon()
        nap()
        Toast.makeText(this, "Đã ẩn $n ghi chú vào Riêng tư", Toast.LENGTH_SHORT).show()
    }

    private fun tabDuoi(anh: Int, ten: String, dangO: Boolean, vang: Boolean = false, bam: () -> Unit): View {
        val cot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isClickable = true
            setOnClickListener { bam() }
        }
        val to = if (dangO) mau.tabChon else mau.tabThuong
        val icon = ImageView(this).apply {
            setImageResource(anh)
            setColorFilter(to)
        }
        cot.addView(icon, LinearLayout.LayoutParams(dp(28), dp(28)))
        // LinearLayout doc cap MATCH_PARENT mac dinh cho con -> chu can TRAI ca cot;
        // phai tu can giua, khong thi nhan lech han khoi icon.
        val nhan = TextView(this).apply {
            text = ten
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(to)
            isSingleLine = true
            gravity = Gravity.CENTER_HORIZONTAL
        }
        cot.addView(nhan)
        if (!vang) cacTab += icon to nhan
        nhanMoDan(cot)
        return cot.also {
            it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    /**
     * Chuyen tab. Thanh tren va thanh tab dung yen; chi vung noi dung doi.
     *
     * Tab Lich trinh: tieu de lon = thang dang xem (ManLichTrinh bao qua
     * `datTieuDe`), an o tim / chip / nut them cua ghi chu (Lich trinh co nut +
     * rieng o thanh duoi cua no), an nut QR va thu muc — ban goc o tab thu hai chi
     * con banh rang.
     */
    private fun chuyenTab(i: Int) {
        if (i == tabHienTai) return
        if (dangChon.isNotEmpty()) thoatChon()
        if (dangTim) thoatCheDoTim()
        tabHienTai = i

        for ((k, cap) in cacTab.withIndex()) {
            val to = if (k == i) mau.tabChon else mau.tabThuong
            cap.first.setColorFilter(to)
            cap.second.setTextColor(to)
        }

        if (i == 1 && khungLich == null) {
            val man = ManLichTrinh(this)
            man.datTieuDe = { chu ->
                if (tabHienTai == 1) {
                    tieuDeLon.text = chu
                    tieuDeNho.text = chu
                }
            }
            val v = man.taoView()
            manLich = man
            khungLich = v
            khungNoiDung.addView(v, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
            ))
        }

        val laLich = i == 1
        khungGhiChu.visibility = if (laLich) View.GONE else View.VISIBLE
        khungLich?.visibility = if (laLich) View.VISIBLE else View.GONE
        nutThem.visibility = if (laLich) View.GONE else View.VISIBLE
        nutQr.visibility = if (laLich) View.GONE else View.VISIBLE
        nutThuMuc.visibility = if (laLich) View.GONE else View.VISIBLE
        tieuDeLon.alpha = 1f
        tieuDeNho.alpha = 0f

        if (laLich) {
            manLich?.nap()
            manLich?.dongBoNgam()
        } else {
            nap()
        }
    }

    /**
     * Nut them: tron 58dp (floating_action_button_size), nen vang theme_main_color,
     * dau cong trang 58dp (ic_content_add), le phai 35dp, bong mem mau vang
     * (DropShadowConfig blur 20dp offsetY 16dp, mau bong = mau nut).
     *
     * Cham nhanh = ghi chu moi; **giu 0,5 giay = mo tro ly AI** — dung
     * OnTouchListener + hen gio chu khong dung long-press bao ngoai nut.
     */
    private fun dungNutThem(): View {
        val nut = ImageView(this).apply {
            setImageResource(R.drawable.ic_them)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(mau.nhan)
            }
            contentDescription = "Ghi chú mới"
            elevation = dpF(12f)
            outlineSpotShadowColor = mau.nhan
            outlineAmbientShadowColor = mau.nhan
        }

        val chinh = android.os.Handler(mainLooper)
        var daGiu = false
        val moAi = Runnable {
            daGiu = true
            nut.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            startActivity(Intent(this, TroLyAiActivity::class.java))
        }
        nut.setOnTouchListener { v, su ->
            when (su.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    daGiu = false
                    chinh.postDelayed(moAi, 500)
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(150).start()
                }
                MotionEvent.ACTION_UP -> {
                    chinh.removeCallbacks(moAi)
                    v.animate().scaleX(1f).scaleY(1f).setDuration(250).start()
                    if (!daGiu) moGhiChu(null, v)
                }
                MotionEvent.ACTION_CANCEL -> {
                    chinh.removeCallbacks(moAi)
                    v.animate().scaleX(1f).scaleY(1f).setDuration(250).start()
                }
            }
            true
        }

        nutThem = nut
        nut.layoutParams = FrameLayout.LayoutParams(dp(58), dp(58)).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            rightMargin = dp(35)
            bottomMargin = dp(64) + dp(35)
        }
        return nut
    }

    // ------------------------------------------------------------ hanh dong

    private fun chamVao(g: GhiChu, v: View) {
        if (dangChon.isNotEmpty()) {
            doiChon(g.id)
            return
        }
        if (thungRac) {
            hienHopThungRac(g)
            return
        }
        moGhiChu(g.id, v)
    }

    /**
     * Mo man soan, "no ra" tu the (hoac tu nut them) — ban goc dung
     * makeScaleUpDown cua MIUI; khung chuan co makeScaleUpAnimation lam gan giong.
     */
    private fun moGhiChu(id: Long?, tu: View) {
        val i = Intent(this, SuaGhiChuActivity::class.java)
        if (id != null) i.putExtra(SuaGhiChuActivity.KHOA_ID, id)
        // Ghi chu moi tao trong thu muc dang xem.
        thuMuc?.takeIf { it.isNotEmpty() }?.let { i.putExtra(SuaGhiChuActivity.KHOA_THU_MUC, it) }
        val opts = ActivityOptions.makeScaleUpAnimation(tu, 0, 0, tu.width, tu.height)
        startActivity(i, opts.toBundle())
    }

    private fun doiChon(id: Long) {
        val truoc = dangChon.isNotEmpty()
        if (!dangChon.add(id)) dangChon.remove(id)
        capNhatThanhChon(truoc)
        veLaiThe()
    }

    private fun chonTatCa() {
        if (dangChon.size == duLieu.size) dangChon.clear() else dangChon.addAll(duLieu.map { it.id })
        capNhatThanhChon(true)
        veLaiThe()
    }

    private fun thoatChon() {
        if (dangChon.isEmpty()) return
        dangChon.clear()
        capNhatThanhChon(true)
        veLaiThe()
    }

    private fun veLaiThe() {
        for (g in duLieu) the[g.id]?.gan(g, mau, g.id in dangChon, dangChon.isNotEmpty())
    }

    /**
     * Vao/ra che do chon: thanh tren MO DAN (alpha, ~300ms), thanh menu day
     * TRUOT LEN — theo dung mo ta ActionBarContextView + split action bar.
     */
    private fun capNhatThanhChon(truoc: Boolean) {
        val co = dangChon.isNotEmpty()
        thanhChon.findViewWithTag<TextView>("nhan_chon")?.text = "Đã chọn ${dangChon.size} mục"
        thanhChon.findViewWithTag<TextView>("nut_chon_tat_ca")?.text =
            if (dangChon.size == duLieu.size && duLieu.isNotEmpty()) "Bỏ chọn tất cả" else "Chọn tất cả"
        if (co == truoc) return

        hangChip.alpha = if (co) 0.5f else 1f
        for (i in 0 until dayChip.childCount) dayChip.getChildAt(i).isEnabled = !co
        if (co) {
            thanhChon.visibility = View.VISIBLE
            thanhChon.animate().alpha(1f).setDuration(300).setStartDelay(50).start()
            thanhDuoiChon.visibility = View.VISIBLE
            thanhDuoiChon.translationY = thanhDuoiChon.height.toFloat().takeIf { it > 0 } ?: dpF(64f)
            thanhDuoiChon.animate().translationY(0f).setDuration(250).start()
        } else {
            thanhChon.animate().alpha(0f).setDuration(200).setStartDelay(0)
                .withEndAction { thanhChon.visibility = View.GONE }.start()
            thanhDuoiChon.animate().translationY(thanhDuoiChon.height.toFloat()).setDuration(250)
                .withEndAction { thanhDuoiChon.visibility = View.GONE }.start()
        }
    }

    private fun ghimDaChon() {
        // Ghim ca nhom theo trang thai: co mot cai chua ghim thi ghim het.
        val ghimHet = duLieu.filter { it.id in dangChon }.any { !it.ghim }
        for (id in dangChon) kho.datGhim(id, ghimHet)
        thoatChon()
        nap()
    }

    private fun chuyenDaChon() {
        val ds = kho.thuMucKemSo().map { it.first }
        val ten = (listOf("Không thuộc thư mục nào") + ds).toTypedArray()
        HopThoai(this, mau)
            .setTitle("Chuyển vào thư mục")
            .setItems(ten) { _, chon ->
                val dich = if (chon == 0) "" else ds[chon - 1]
                for (id in dangChon) kho.chuyenThuMuc(id, dich)
                thoatChon()
                nap()
            }
            .show()
    }

    private fun xoaDaChon() {
        val n = dangChon.size
        if (thungRac) {
            HopThoai(this, mau)
                .setMessage("Xoá vĩnh viễn $n ghi chú? Không thể hoàn tác.")
                .nguyHiem()
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Xoá") { _, _ ->
                    for (id in dangChon) kho.xoaHan(id)
                    thoatChon()
                    nap()
                }
                .show()
            return
        }
        for (id in dangChon) kho.boVaoThungRac(id)
        thoatChon()
        nap()
        Toast.makeText(this, "Đã chuyển $n ghi chú vào thùng rác", Toast.LENGTH_SHORT).show()
    }

    private fun hienHopThungRac(g: GhiChu) {
        HopThoai(this, mau)
            .setTitle(g.tieuDeHien().ifBlank { "Ghi chú" })
            .setItems(arrayOf("Khôi phục", "Xoá vĩnh viễn")) { _, chon ->
                if (chon == 0) kho.khoiPhuc(g.id) else kho.xoaHan(g.id)
                nap()
            }
            .show()
    }

    // ------------------------------------------------------------ thu muc

    private fun hienThuMuc() {
        val ds = kho.thuMucKemSo()
        val ten = ArrayList<String>()
        val lam = ArrayList<() -> Unit>()

        ten += "Tất cả ghi chú"
        lam += { doiCho(null, false) }
        ten += "Chưa xếp thư mục"
        lam += { doiCho("", false) }
        for ((t, n) in ds) {
            ten += "$t  ($n)"
            lam += { doiCho(t, false) }
        }
        ten += "Riêng tư"
        lam += { moRiengTu() }
        ten += "Thùng rác"
        lam += { doiCho(null, true) }
        ten += "Thư mục mới…"
        lam += { hienThemThuMuc() }

        HopThoai(this, mau)
            .setTitle("Thư mục")
            .setItems(ten.toTypedArray()) { _, chon -> lam[chon]() }
            .show()
    }

    private fun hienThemThuMuc() {
        val o = EditText(this).apply {
            hint = "Tên thư mục"
            isSingleLine = true
        }
        HopThoai(this, mau)
            .setTitle("Thư mục mới")
            .setView(o)
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Tạo") { _, _ ->
                if (kho.themThuMuc(o.text.toString())) doiCho(o.text.toString().trim(), false)
                else Toast.makeText(this, "Tên thư mục đã có hoặc để trống", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun doiCho(tm: String?, rac: Boolean, rieng: Boolean = false) {
        thuMuc = tm
        thungRac = rac
        riengTu = rieng
        thoatChon()
        nap()
    }

    // ------------------------------------------------------------ rieng tu

    /**
     * Vao muc Rieng tu: hoi mat khau, sinh khoa o luong nen roi mo.
     *
     * Sinh khoa co y CHAM (PBKDF2 210.000 vong — xem [MaHoa]) nen bat buoc phai o
     * luong nen: lam o luong chinh la giao dien dung hinh gan mot giay.
     */
    private fun moRiengTu() {
        if (KhoaRiengTu.dangMo) {
            doiCho(null, false, rieng = true)
            return
        }
        if (!KhoaRiengTu.daDatMatKhau) {
            hienDatMatKhau()
            return
        }
        hoiMatKhau("Nhập mật khẩu", "Mở") { mk, xong ->
            Thread {
                val duoc = KhoaRiengTu.mo(mk)
                runOnUiThread {
                    xong()
                    if (duoc) doiCho(null, false, rieng = true)
                    else Toast.makeText(this, "Sai mật khẩu", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
    }

    private fun hienDatMatKhau() {
        HopThoai(this, mau)
            .setTitle("Đặt mật khẩu riêng tư")
            .setMessage(KhoaRiengTu.canhBao)
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Tiếp") { _, _ ->
                hoiMatKhau("Mật khẩu mới", "Đặt") { mk, xong ->
                    if (mk.length < 4) {
                        xong()
                        Toast.makeText(this, "Mật khẩu quá ngắn", Toast.LENGTH_SHORT).show()
                        return@hoiMatKhau
                    }
                    Thread {
                        val duoc = KhoaRiengTu.dat(mk)
                        runOnUiThread {
                            xong()
                            if (duoc) doiCho(null, false, rieng = true)
                        }
                    }.start()
                }
            }
            .show()
    }

    /**
     * Hop nhap mat khau. [lam] nhan mat khau va mot ham `xong` de dong hop cho.
     */
    private fun hoiMatKhau(tieuDe: String, nutOk: String, lam: (String, () -> Unit) -> Unit) {
        val o = EditText(this).apply {
            hint = "Mật khẩu"
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        HopThoai(this, mau)
            .setTitle(tieuDe)
            .setView(o)
            .setNegativeButton("Huỷ", null)
            .setPositiveButton(nutOk) { _, _ ->
                val cho = android.app.ProgressDialog(this, mau.themeHopThoai).apply {
                    setMessage("Đang mở khoá…")
                    setCancelable(false)
                    show()
                }
                lam(o.text.toString()) { if (cho.isShowing) cho.dismiss() }
            }
            .show()
    }

    // ------------------------------------------------------------ cai dat

    private fun hienCaiDat() {
        val muc = arrayOf("Trợ lý AI", "Giao diện", "Đồng bộ tài khoản", "Khoá API Gemini")
        HopThoai(this, mau)
            .setTitle("Cài đặt")
            .setItems(muc) { _, chon ->
                when (chon) {
                    0 -> startActivity(Intent(this, TroLyAiActivity::class.java))
                    1 -> startActivity(Intent(this, CaiDatGiaoDienActivity::class.java))
                    2 -> startActivity(Intent(this, DongBoActivity::class.java))
                    3 -> hienKhoaGemini()
                }
            }
            .show()
    }

    /**
     * Nhap khoa Gemini bang tay. **KHONG nhung khoa vao APK** — ban da chot vay.
     */
    private fun hienKhoaGemini() {
        val o = EditText(this).apply {
            hint = "Dán khoá API vào đây"
            isSingleLine = true
            setText(Gemini.khoa)
        }
        HopThoai(this, mau)
            .setTitle("Khoá API Gemini")
            .setView(o)
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Lưu") { _, _ -> Gemini.khoa = o.text.toString().trim() }
            .show()
    }

    // ------------------------------------------------------------ nap

    private fun nap() {
        duLieu = kho.danhSach(
            thuMuc = if (riengTu) null else thuMuc,
            tim = tim,
            riengTu = riengTu,
            thungRac = thungRac,
        )
        // Bo the cua ghi chu khong con trong danh sach — khong thi bo nho giu mai.
        val conLai = duLieu.map { it.id }.toHashSet()
        the.keys.retainAll(conLai)
        luoi.datThe(duLieu.map { dungThe(it) })
        napChip()

        val ten = when {
            riengTu -> "Riêng tư"
            thungRac -> "Thùng rác"
            thuMuc == null -> "Ghi chú"
            thuMuc!!.isEmpty() -> "Chưa xếp thư mục"
            else -> thuMuc!!
        }
        if (tabHienTai == 0) {
            tieuDeLon.text = ten
            tieuDeNho.text = ten
        }

        if (dangTim) {
            // Co chu thi bo lop mo de thay ket qua; xoa het chu thi phu lai.
            lopMoTim.visibility = if (tim.isBlank()) View.VISIBLE else View.GONE
            if (tim.isBlank()) lopMoTim.alpha = 1f
        }

        oTrong.visibility = if (duLieu.isEmpty()) View.VISIBLE else View.GONE
        oTrong.findViewWithTag<TextView>("chu_trong")?.text = when {
            tim.isNotBlank() -> "Không có ghi chú nào ở đây"
            thungRac -> "Thùng rác trống"
            riengTu -> "Chưa có ghi chú riêng tư"
            else -> "Chưa có ghi chú"
        }
    }

    // ------------------------------------------------------------ tien ich

    /**
     * Nut icon thanh tren: khung 28dp, glyph 21dp (SymbolDrawable cua miuix),
     * alpha thuong 0.8, nhan 0.5. KHONG ripple.
     */
    private fun nutAnh(anh: Int, ten: String, bam: () -> Unit): ImageButton =
        ImageButton(this).apply {
            setImageResource(anh)
            setColorFilter(mau.chuChinh)
            contentDescription = ten
            background = null
            alpha = 0.8f
            val dem = dp(9) + (dp(28) - dp(21)) / 2
            setPadding(dem, dem, dem, dem)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
            setOnClickListener { bam() }
            nhanMoDan(this, 0.8f, 0.5f)
        }

    /**
     * Trang thai nhan kieu miuix: chi doi alpha (thuong -> nhan), khong ripple lan.
     * Khong nuot su kien de click van chay.
     */
    private fun nhanMoDan(v: View, thuong: Float = 1f, nhan: Float = 0.6f) {
        v.setOnTouchListener { _, su ->
            when (su.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.animate().alpha(nhan).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().alpha(thuong).setDuration(200).start()
            }
            false
        }
    }

    companion object {
        /** Extra: 0 = Ghi chu, 1 = Lich trinh. */
        const val KHOA_TAB = "tab"
    }

    override fun onBackPressed() {
        // Back quay ve mot buoc chu khong thoat app ngay — dung y ban da chot.
        when {
            dangChon.isNotEmpty() -> thoatChon()
            dangTim -> thoatCheDoTim()
            // Back o tab thu hai thi ve tab dau, chua thoat app — dung y ban chot.
            tabHienTai != 0 -> chuyenTab(0)
            tim.isNotBlank() -> oTim.setText("")
            riengTu || thungRac || thuMuc != null -> doiCho(null, false)
            else -> super.onBackPressed()
        }
    }
}
