# -*- coding: utf-8 -*-
# Va dot 3: popup "..." man soan, tinh chinh che do chon nhieu, che do tim kiem.
# So do: docs/SPEC-GOC-dot3.md. Moi doan thay deu co NEO; thieu neo la dung, khong va.
import io, os, sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')

GOC = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'kotlin', 'com', 'ghichu')

def doc(t): return io.open(os.path.join(GOC, t), encoding='utf-8').read()
def ghi(t, s): io.open(os.path.join(GOC, t), 'w', encoding='utf-8').write(s)

def thay(s, a, b, ten):
    if a not in s: raise SystemExit('THIEU NEO: ' + ten)
    print('va', ten)
    return s.replace(a, b, 1)

# =============================================================== man soan: menu "..."
s = doc('SuaGhiChuActivity.kt')

s = thay(s, '''        nhomXem.addView(nutChuNho("⋯", "Thêm") { hienThem() }.also {
            (it.layoutParams as LinearLayout.LayoutParams).leftMargin = dp(12)
        })''',
'''        nutThemMenu = nutChuNho("⋯", "Thêm") { hienThem() }.also {
            (it.layoutParams as LinearLayout.LayoutParams).leftMargin = dp(12)
        }
        nhomXem.addView(nutThemMenu)''', 'neo nut ...')

s = thay(s, '''    private lateinit var nhomXem: LinearLayout
    private lateinit var nutXong: View''',
'''    private lateinit var nhomXem: LinearLayout
    private lateinit var nutXong: View
    private lateinit var nutThemMenu: View''', 'khai bao nutThemMenu')

a = s.index('    private fun hienThem() {')
b = s.index('    private fun hienChonMauGiay() {')
s = s[:a] + '''    /**
     * Menu "…" — popup kieu miuix bung ra tu goc tren-phai cua nut (xem [MenuNoi]).
     * Thu tu lenh theo normal_note_edit_options.xml cua ban goc: Nhắc nhở · Ẩn/Bỏ
     * ẩn · Chuyển tới · Xóa; them Ghim (ban goc de o thanh tren, o day khong con cho).
     */
    private fun hienThem() {
        val menu = MenuNoi(this, mau)
        menu.them(if (ghim) "Bỏ ghim" else "Ghim") {
            ghim = !ghim
            Toast.makeText(this, if (ghim) "Đã ghim" else "Bỏ ghim", Toast.LENGTH_SHORT).show()
        }
        menu.them(if (nhacLuc == null) "Nhắc nhở" else "Bỏ nhắc nhở") {
            if (nhacLuc == null) hienDatNhac() else datNhac(null)
        }
        menu.them(if (goc?.riengTu == true) "Bỏ ẩn" else "Ẩn") { doiRiengTu() }
        menu.them("Chuyển tới") { hienChonThuMuc() }
        if (id != 0L) {
            menu.them("Xóa") {
                kho.boVaoThungRac(id)
                // Da xoa thi dung de onPause luu lai — luu lai la ghi chu song day.
                id = 0L
                goc = null
                finish()
            }
        }
        menu.hien(nutThemMenu)
    }

''' + s[b:]
print('va hienThem -> MenuNoi')
ghi('SuaGhiChuActivity.kt', s)

# =============================================================== man chinh
s = doc('ManHinhChinhActivity.kt')

# --- che do chon nhieu: nut chu 14sp medium, doi "Chon tat ca"/"Bo chon tat ca", them Ẩn
s = thay(s, '''    private fun nutChu(chu: String, bam: () -> Unit): TextView = TextView(this).apply {
        text = chu
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTextColor(mau.chuChinh)
        setPadding(dp(4), dp(10), dp(4), dp(10))''',
'''    /** Nut chu cua thanh chon: 14sp medium (miuix_font_size_body2), toi da 40dp cao. */
    private fun nutChu(chu: String, bam: () -> Unit): TextView = TextView(this).apply {
        text = chu
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(mau.chuChinh)
        setPadding(dp(4), dp(10), dp(4), dp(10))''', 'nut chu 14sp')

s = thay(s, '''        thanh.addView(nutChu("Chọn tất cả") { chonTatCa() })
        return thanh''',
'''        thanh.addView(nutChu("Chọn tất cả") { chonTatCa() }.also { it.tag = "nut_chon_tat_ca" })
        return thanh''', 'tag chon tat ca')

s = thay(s, '''        thanhChon.findViewWithTag<TextView>("nhan_chon")?.text = "Đã chọn ${dangChon.size} mục"
        if (co == truoc) return''',
'''        thanhChon.findViewWithTag<TextView>("nhan_chon")?.text = "Đã chọn ${dangChon.size} mục"
        thanhChon.findViewWithTag<TextView>("nut_chon_tat_ca")?.text =
            if (dangChon.size == duLieu.size && duLieu.isNotEmpty()) "Bỏ chọn tất cả" else "Chọn tất cả"
        if (co == truoc) return''', 'doi chu chon tat ca')

# thanh menu day: chu 11dp #CC000000, padding 18dp, gap 11dp; them "Ẩn"
s = thay(s, '''        val hangChon = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(16))
        }
        hangChon.addView(tabDuoi(R.drawable.ic_goc_ghim, "Ghim", true, vang = false) { ghimDaChon() })
        hangChon.addView(tabDuoi(R.drawable.ic_thu_muc, "Chuyển", true) { chuyenDaChon() })
        hangChon.addView(tabDuoi(R.drawable.ic_xoa, "Xoá", true) { xoaDaChon() })''',
'''        // Thanh menu day cua action mode: dem ngang 8dp, nut chia deu cach 11dp,
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
        })''', 'thanh menu day')

s = thay(s, '''    private fun tabDuoi(anh: Int, ten: String, dangO: Boolean, vang: Boolean = false, bam: () -> Unit): View {''',
'''    /** Nut cua thanh menu day (Widget.ActionMode.ActionButton): icon 28dp + chu 11dp, dem doc 18dp. */
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

    private fun tabDuoi(anh: Int, ten: String, dangO: Boolean, vang: Boolean = false, bam: () -> Unit): View {''', 'nutMenuDay + anDaChon')

# --- che do tim kiem
s = thay(s, '''    private var duLieu: List<GhiChu> = emptyList()
    private val the = HashMap<Long, TheGhiChu>()''',
'''    private var duLieu: List<GhiChu> = emptyList()
    private val the = HashMap<Long, TheGhiChu>()

    /** Dang o che do tim (thanh tim len dinh, nut Huy, lop mo). */
    private var dangTim = false
    private lateinit var nutHuyTim: TextView
    private lateinit var lopMoTim: View
    private lateinit var hangTim: FrameLayout''', 'khai bao tim')

s = thay(s, '''        pill.addView(oTim, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))

        hang.addView(pill, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
            leftMargin = dp(12)
            rightMargin = dp(12)
            topMargin = dp(8)
            bottomMargin = dp(4)
        })
        hang.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
        return hang
    }''',
'''        pill.addView(oTim, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
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
    }''', 'o tim + che do tim')

# lop mo trong hop luoi
s = thay(s, '''        hop.addView(oTrong, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
        ))
        return hop
    }''',
'''        hop.addView(oTrong, FrameLayout.LayoutParams(
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
    }''', 'lop mo')

# nap: lop mo theo chu; chu trong khi tim; to sang
s = thay(s, '''        val v = the[g.id] ?: TheGhiChu(this).also { the[g.id] = it }
        v.gan(g, mau, g.id in dangChon, dangChon.isNotEmpty())''',
'''        val v = the[g.id] ?: TheGhiChu(this).also { the[g.id] = it }
        v.gan(g, mau, g.id in dangChon, dangChon.isNotEmpty(), toSang = if (dangTim) tim else "")''', 'to sang')

s = thay(s, '''        oTrong.visibility = if (duLieu.isEmpty()) View.VISIBLE else View.GONE
        oTrong.findViewWithTag<TextView>("chu_trong")?.text = when {
            tim.isNotBlank() -> "Không tìm thấy ghi chú nào"''',
'''        if (dangTim) {
            // Co chu thi bo lop mo de thay ket qua; xoa het chu thi phu lai.
            lopMoTim.visibility = if (tim.isBlank()) View.VISIBLE else View.GONE
            if (tim.isBlank()) lopMoTim.alpha = 1f
        }

        oTrong.visibility = if (duLieu.isEmpty()) View.VISIBLE else View.GONE
        oTrong.findViewWithTag<TextView>("chu_trong")?.text = when {
            tim.isNotBlank() -> "Không có ghi chú nào ở đây"''', 'nap tim')

# napChip: nho trang thai an de thoatCheDoTim khong bat lai nham
s = thay(s, '''        if (thungRac || riengTu || ds.isEmpty()) {
            hangChip.visibility = View.GONE
            return
        }
        hangChip.visibility = View.VISIBLE''',
'''        if (thungRac || riengTu || ds.isEmpty()) {
            hangChip.visibility = View.GONE
            hangChip.tag = "an"
            return
        }
        hangChip.tag = null
        if (!dangTim) hangChip.visibility = View.VISIBLE''', 'napChip tag')

# back: thoat tim truoc
s = thay(s, '''        when {
            dangChon.isNotEmpty() -> thoatChon()
            tim.isNotBlank() -> oTim.setText("")''',
'''        when {
            dangChon.isNotEmpty() -> thoatChon()
            dangTim -> thoatCheDoTim()
            tim.isNotBlank() -> oTim.setText("")''', 'back tim')

ghi('ManHinhChinhActivity.kt', s)
print('XONG')
