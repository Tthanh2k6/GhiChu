# -*- coding: utf-8 -*-
# Phan hoi tren may (lan 2):
#  * bo hang chip GHIM (cuon xuong thi ca header troi di, khong ghim gi) — cung la
#    nguyen nhan giat: hang ghim bat/tat lam noi dung nhay 52dp;
#  * o dau trang HIEN thanh tim (trong header), cuon xuong thi an theo header;
#  * thanh 56dp thanh kinh mo de len noi dung — tach header khoi noi dung.
import io, os, sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
p = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'kotlin', 'com', 'ghichu', 'ManHinhChinhActivity.kt')
s = io.open(p, encoding='utf-8').read()

def thay(a, b, ten):
    global s
    if a not in s:
        raise SystemExit('THIEU NEO: ' + ten)
    s = s.replace(a, b, 1)
    print('va', ten)

# ---- 1. Cau truc: thanh tren ra ngoai FrameLayout (kinh mo), noi dung cuon xuyen duoi
thay('''        goc = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(mau.nen)
        }
        goc.addView(dungThanhTren())

        // Vung noi dung: hai tab la hai khung chong nhau, doi bang visibility —
        // thanh tren + thanh tab dung yen, dung nhu Navigator cua ban goc.
        //
        // Header cua tab Ghi chu (tieu de lon + hang chip) nam TRONG vung cuon
        // cua luoi: cuon len la tieu de troi di, tieu de nho o thanh 56dp hien
        // dan — dung co che NestedHeaderLayout cua ban goc. Hang chip co them mot
        // ban GHIM ngoai vung cuon, hien khi tieu de da khuat (stickyView).
        khungGhiChu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        khungGhiChu.addView(dungOTim())
        khungGhiChu.addView(dungHangChipGhim())
        val luoiView = dungLuoi()
        luoi.dauTrang.addView(dungTieuDeLon())
        luoi.dauTrang.addView(dungHangChip())''',
'''        goc = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(mau.nen)
        }

        // Vung noi dung: hai tab la hai khung chong nhau, doi bang visibility —
        // thanh tren + thanh tab dung yen, dung nhu Navigator cua ban goc.
        //
        // Header cua tab Ghi chu (tieu de lon + thanh tim + hang chip) nam TRONG
        // vung cuon cua luoi: cuon len la CA header troi di (khong ghim gi), tieu
        // de nho o thanh 56dp hien dan — nhu NestedHeaderLayout cua ban goc. Thanh
        // 56dp la kinh mo de len noi dung (ActionBar blur khi noi dung cuon qua).
        khungGhiChu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val luoiView = dungLuoi()
        luoi.dauTrang.addView(dungTieuDeLon())
        luoi.dauTrang.addView(dungNutTim())
        luoi.dauTrang.addView(dungHangChip())''', 'cau truc header')

thay('''        // Thanh tab nam DE LEN noi dung (khong chiem cho trong cot): luoi cuon xuyen
        // ben duoi thanh kinh hoi trong — nhu blur cua HyperOS.
        val nen = FrameLayout(this).apply {
            setBackgroundColor(mau.nen)
            addView(goc)
            addView(dungThanhDuoi(), FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.BOTTOM })
            addView(dungNutThem())
        }''',
'''        // Thanh tren va thanh tab nam DE LEN noi dung (khong chiem cho trong cot):
        // noi dung cuon xuyen ben duoi hai tam kinh mo — nhu blur cua HyperOS.
        val nen = FrameLayout(this).apply {
            setBackgroundColor(mau.nen)
            addView(goc)
            addView(dungThanhDuoi(), FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.BOTTOM })
            addView(dungKhungThanhTren(), FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.TOP })
            addView(dungNutThem())
        }''', 'thanh tren ra ngoai')

# ---- 2. Inset: thanh tren chua status bar; noi dung chua cho thanh tren
thay('''            val thanh = insets.getInsets(WindowInsets.Type.systemBars())
            goc.setPadding(0, thanh.top, 0, 0)''',
'''            val thanh = insets.getInsets(WindowInsets.Type.systemBars())
            // Thanh tren om luon thanh trang thai; noi dung chua cho no (inset + 56dp).
            caoThanhTren = thanh.top + dp(56)
            khungThanhTren.setPadding(0, thanh.top, 0, 0)
            kinhTren.layoutParams = (kinhTren.layoutParams as FrameLayout.LayoutParams).apply { height = caoThanhTren }
            luoi.dauTrang.setPadding(0, caoThanhTren, 0, 0)
            oTrong.setPadding(dp(24), caoThanhTren + dp(154), dp(24), 0)
            khungLich?.setPadding(0, caoThanhTren, 0, caoThanhTab)''', 'inset thanh tren')
thay('''            khungLich?.setPadding(0, 0, 0, caoThanhTab)
            insets
        }''', '''            insets
        }''', 'bo padding lich cu')
thay('''    private var demDuoi = 0
    private var caoThanhTab = 0''', '''    private var demDuoi = 0
    private var caoThanhTab = 0
    private var caoThanhTren = 0
    private lateinit var khungThanhTren: FrameLayout
    private lateinit var kinhTren: KinhMo''', 'khai bao thanh tren')

# ---- 3. Khung thanh tren = kinh mo + thanh 56dp (+ hang tim thay the khi tim)
thay('''    /**
     * Thanh 56dp (miuix_appcompat_action_bar_default_height): tieu de nho 20sp
     * can giua (an khi o dau trang), icon ben phai, mep phai 20dp.
     *
     * Thanh chon nhieu nam CHONG len trong cung FrameLayout va doi bang alpha
     * (mo dan ~300ms nhu ActionBarContextView cua ban goc).
     */
    private fun dungThanhTren(): View {''',
'''    /** Khung ngoai: kinh mo phia sau + thanh 56dp; om luon thanh trang thai. */
    private fun dungKhungThanhTren(): View {
        khungThanhTren = FrameLayout(this)
        kinhTren = KinhMo(this, mau) { if (::khungNoiDung.isInitialized) khungNoiDung else null }
        khungThanhTren.addView(kinhTren, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0))
        val thanh = dungThanhTren()
        khungThanhTren.addView(thanh, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(56)))
        // Hang tim (che do tim) thay the thanh 56dp, cung mot khung.
        khungThanhTren.addView(dungOTim(), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(56)))
        return khungThanhTren
    }

    /**
     * Thanh 56dp (miuix_appcompat_action_bar_default_height): tieu de nho 20sp
     * can giua (an khi o dau trang), icon ben phai, mep phai 20dp.
     *
     * Thanh chon nhieu nam CHONG len trong cung FrameLayout va doi bang alpha
     * (mo dan ~300ms nhu ActionBarContextView cua ban goc).
     */
    private fun dungThanhTren(): View {''', 'khung thanh tren')

thay('''        return thanhTren.also {
            it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
        }
    }''', '''        return thanhTren
    }''', 'thanhTren khong layoutParams LinearLayout')

# ---- 4. Bo kinh lup tren thanh; thanh tim trong header = nut mo che do tim
thay('''        // Thanh tim kiem AN theo yeu cau; kinh lup o day mo che do tim.
        nutTim = nutAnh(R.drawable.ic_tim, "Tìm kiếm") { vaoCheDoTim() }
        nutQr = nutAnh(R.drawable.ghichu_ic_qr, "Quét QR") {
            startActivity(Intent(this, QuetQrActivity::class.java))
        }
        nutThuMuc = nutAnh(R.drawable.ic_thu_muc, "Thư mục") { hienThuMuc() }
        phai.addView(nutTim)
        phai.addView(nutQr)''',
'''        nutQr = nutAnh(R.drawable.ghichu_ic_qr, "Quét QR") {
            startActivity(Intent(this, QuetQrActivity::class.java))
        }
        nutThuMuc = nutAnh(R.drawable.ic_thu_muc, "Thư mục") { hienThuMuc() }
        phai.addView(nutQr)''', 'bo kinh lup')

thay('''    private lateinit var nutTim: View
    /** Ban ghim cua hang chip: hien khi tieu de lon da cuon khuat. */
    private lateinit var hangChipGhim: android.widget.HorizontalScrollView
    private lateinit var dayChipGhim: LinearLayout
    private lateinit var tieuDeLich: TextView''',
'''    private lateinit var tieuDeLich: TextView''', 'bo khai bao ghim')

thay('''    /** Ban ghim cua hang chip: giong het hang trong header, nam ngoai vung cuon. */
    private fun dungHangChipGhim(): View {
        dayChipGhim = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        hangChipGhim = android.widget.HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(dp(12), dp(4), dp(12), dp(8))
            clipToPadding = false
            setBackgroundColor(mau.nen)
            addView(dayChipGhim)
            visibility = View.GONE
        }
        return hangChipGhim
    }

    private fun napChip() {
        val ds = kho.thuMucKemSo().map { it.first }
        if (thungRac || riengTu || ds.isEmpty()) {
            hangChip.visibility = View.GONE
            hangChipGhim.visibility = View.GONE
            hangChip.tag = "an"
            return
        }
        hangChip.tag = null
        if (!dangTim) hangChip.visibility = View.VISIBLE

        val muc = ArrayList<Pair<String, String?>>()
        muc += "Tất cả" to null
        for (t in ds) muc += t to t
        if (kho.soLuong("") > 0) muc += "Chưa phân loại" to ""

        // Hai ban giong het: mot trong header cuon, mot ghim ngoai vung cuon.
        for ((day, hang) in listOf(dayChip to hangChip, dayChipGhim to hangChipGhim)) {
            day.removeAllViews()
            var chipChon: View? = null
            for ((i, m) in muc.withIndex()) {
                val (ten, gia) = m
                val chon = gia == thuMuc
                val chip = chipThuMuc(ten, chon) { doiCho(gia, false) }
                if (chon) chipChon = chip
                day.addView(chip, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(40),
                ).apply { if (i < muc.size - 1) rightMargin = dp(12) })
            }
            // Chip dang chon luon lo ra — smoothScrollToPosition cua ban goc.
            chipChon?.let { c -> hang.post { hang.smoothScrollTo((c.left - dp(12)).coerceAtLeast(0), 0) } }
        }
        thuGonTieuDe(luoi.scrollY)
    }''',
'''    /**
     * Thanh tim TRONG header (hien o dau trang, cuon xuong thi troi theo header).
     * La mot NUT: bam vao mo che do tim voi o nhap that o thanh tren — dung nhu
     * ban goc (ca thanh la nut mo action mode tim).
     */
    private fun dungNutTim(): View {
        val hang = FrameLayout(this)
        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpF(72f)
                setColor(mau.nenTimKiem)
            }
            setPadding(dp(12), 0, dp(16), 0)
            isClickable = true
            setOnClickListener { vaoCheDoTim() }
        }
        pill.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_tim)
            setPadding(dp(3), dp(3), dp(3), dp(3))
            setColorFilter(if (mau.toi) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            alpha = 0.3f
        }, LinearLayout.LayoutParams(dp(24), dp(24)).apply { rightMargin = dp(8) })
        pill.addView(TextView(this).apply {
            text = "Tìm kiếm ghi chú"
            setTextColor(mau.chuGoiY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isSingleLine = true
        })
        hang.addView(pill, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
            leftMargin = dp(12)
            rightMargin = dp(12)
            topMargin = dp(8)
            bottomMargin = dp(4)
        })
        hang.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
        nutTimHeader = hang
        return hang
    }

    private lateinit var nutTimHeader: View

    private fun napChip() {
        val ds = kho.thuMucKemSo().map { it.first }
        if (thungRac || riengTu || ds.isEmpty()) {
            hangChip.visibility = View.GONE
            hangChip.tag = "an"
            return
        }
        hangChip.tag = null
        hangChip.visibility = View.VISIBLE

        val muc = ArrayList<Pair<String, String?>>()
        muc += "Tất cả" to null
        for (t in ds) muc += t to t
        if (kho.soLuong("") > 0) muc += "Chưa phân loại" to ""

        dayChip.removeAllViews()
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
        thuGonTieuDe(luoi.scrollY)
    }''', 'chip mot ban + nut tim header')

# ---- 5. thu gon: khong con ghim
thay('''        tieuDeLon.alpha = 1f - f
        tieuDeNho.alpha = f
        // Tieu de da cuon khuat -> hang chip ghim thay cho hang trong header.
        val ghim = t >= cao && hangChip.tag != "an"
        hangChipGhim.visibility = if (ghim) View.VISIBLE else View.GONE
    }''', '''        tieuDeLon.alpha = 1f - f
        tieuDeNho.alpha = f
    }''', 'thu gon khong ghim')

# ---- 6. Che do tim: thanh 56dp mo di, hang tim hien trong cung khung; header trong luoi an
thay('''        for (v in listOf<View>(thanhTren, luoi.dauTrang)) {
            v.animate().alpha(0f).setDuration(200).withEndAction { if (dangTim) v.visibility = View.GONE }.start()
        }
        hangChipGhim.visibility = View.GONE
        hangTim.visibility = View.VISIBLE''',
'''        for (v in listOf<View>(thanhTren, luoi.dauTrang)) {
            v.animate().alpha(0f).setDuration(200).withEndAction { if (dangTim) v.visibility = View.GONE }.start()
        }
        hangTim.visibility = View.VISIBLE''', 'vao tim')

thay('''        nutQr.visibility = if (laLich) View.GONE else View.VISIBLE
        nutThuMuc.visibility = if (laLich) View.GONE else View.VISIBLE
        nutTim.visibility = if (laLich) View.GONE else View.VISIBLE
        tieuDeNho.alpha = if (laLich) 0f else (tieuDeNho.alpha)
        if (!laLich) thuGonTieuDe(luoi.scrollY)''',
'''        nutQr.visibility = if (laLich) View.GONE else View.VISIBLE
        nutThuMuc.visibility = if (laLich) View.GONE else View.VISIBLE
        tieuDeNho.alpha = if (laLich) 0f else (tieuDeNho.alpha)
        if (!laLich) thuGonTieuDe(luoi.scrollY)
        kinhTren.postInvalidate()''', 'chuyenTab')

thay('''            cot.setPadding(0, 0, 0, caoThanhTab)
            khungLich = cot''', '''            cot.setPadding(0, caoThanhTren, 0, caoThanhTab)
            khungLich = cot''', 'khung lich padding tren')

# ---- 7. Cuon -> ve lai kinh tren; nap -> ve lai
thay('''        luoi.khiCuon = { t ->
            thuGonTieuDe(t)
            if (::kinhTab.isInitialized) kinhTab.invalidate()
        }''', '''        luoi.khiCuon = { t ->
            thuGonTieuDe(t)
            if (::kinhTab.isInitialized) kinhTab.invalidate()
            if (::kinhTren.isInitialized) kinhTren.invalidate()
        }''', 'cuon ve lai kinh tren')
thay('''        if (::kinhTab.isInitialized) kinhTab.postInvalidate()
        oTrong.visibility''', '''        if (::kinhTab.isInitialized) kinhTab.postInvalidate()
        if (::kinhTren.isInitialized) kinhTren.postInvalidate()
        oTrong.visibility''', 'nap ve lai kinh tren')

# ---- 8. Thanh chon: nen trong suot (kinh phia sau), thanh tren cung vay
thay('''            visibility = View.GONE
            alpha = 0f
            setBackgroundColor(mau.nen)
        }
        thanh.addView(nutChu("Huỷ") { thoatChon() })''',
'''            visibility = View.GONE
            alpha = 0f
        }
        thanh.addView(nutChu("Huỷ") { thoatChon() })''', 'thanh chon trong suot')

io.open(p, 'w', encoding='utf-8').write(s)
print('XONG')
