# -*- coding: utf-8 -*-
# Theo phan hoi tren may that (2026-08-26):
#  1. bo nut Chen anh + So do tu duy khoi toolbar man soan;
#  2. toolbar man soan sat day (dem thanh dieu huong nam TRONG toolbar);
#  3. header thu gon that: tieu de lon nam trong vung cuon, chip thu muc la mot
#     phan cua header va ghim lai khi cuon qua;
#  4. an thanh tim kiem, thay bang nut kinh lup tren thanh 56dp.
import io, os, sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
GOC = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'kotlin', 'com', 'ghichu')

def doc(t): return io.open(os.path.join(GOC, t), encoding='utf-8').read()
def ghi(t, s): io.open(os.path.join(GOC, t), 'w', encoding='utf-8').write(s)
def thay(s, a, b, ten):
    if a not in s: raise SystemExit('THIEU NEO: ' + ten)
    print('va', ten); return s.replace(a, b, 1)

# ============================================================ LuoiSoLe: o dau trang
s = doc('LuoiSoLe.kt')
s = thay(s, '''    /** Goi moi khi cuon, kem vi tri cuon doc (px). Dung de thu gon tieu de lon. */
    var khiCuon: ((Int) -> Unit)? = null''',
'''    /** Goi moi khi cuon, kem vi tri cuon doc (px). Dung de thu gon tieu de lon. */
    var khiCuon: ((Int) -> Unit)? = null

    /**
     * Dau trang: nam TRONG vung cuon, tren hai cot — tieu de lon + hang chip cua
     * man chinh. Cuon len la no troi di, dung nhu header co gian cua
     * NestedHeaderLayout ban goc.
     */
    val dauTrang = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val than = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }''', 'khai bao dauTrang')
s = thay(s, '''        addView(hang, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }''',
'''        than.addView(dauTrang, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        than.addView(hang, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        addView(than, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }''', 'them dauTrang vao cay')
ghi('LuoiSoLe.kt', s)

# ============================================================ Man chinh
s = doc('ManHinhChinhActivity.kt')

# --- khai bao
s = thay(s, '''    private lateinit var nutQr: View
    private lateinit var nutThuMuc: View''',
'''    private lateinit var nutQr: View
    private lateinit var nutThuMuc: View
    private lateinit var nutTim: View
    /** Ban ghim cua hang chip: hien khi tieu de lon da cuon khuat. */
    private lateinit var hangChipGhim: android.widget.HorizontalScrollView
    private lateinit var dayChipGhim: LinearLayout
    private lateinit var tieuDeLich: TextView''', 'khai bao header')

# --- cau truc: tieu de lon + chip vao dau trang cua luoi; o tim an; chip ghim ngoai luoi
s = thay(s, '''        goc.addView(dungThanhTren())
        goc.addView(dungTieuDeLon())

        // Vung noi dung: hai tab la hai khung chong nhau, doi bang visibility —
        // thanh tren + thanh tab dung yen, dung nhu Navigator cua ban goc.
        khungGhiChu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        khungGhiChu.addView(dungOTim())
        khungGhiChu.addView(dungHangChip())
        khungGhiChu.addView(dungLuoi(), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
        ))''',
'''        goc.addView(dungThanhTren())

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
        luoi.dauTrang.addView(dungHangChip())
        khungGhiChu.addView(luoiView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
        ))''', 'cau truc header trong luoi')

# --- thanh tren: them nut kinh lup
s = thay(s, '''        nutQr = nutAnh(R.drawable.ghichu_ic_qr, "Quét QR") {
            startActivity(Intent(this, QuetQrActivity::class.java))
        }
        nutThuMuc = nutAnh(R.drawable.ic_thu_muc, "Thư mục") { hienThuMuc() }
        phai.addView(nutQr)''',
'''        // Thanh tim kiem AN theo yeu cau; kinh lup o day mo che do tim.
        nutTim = nutAnh(R.drawable.ic_tim, "Tìm kiếm") { vaoCheDoTim() }
        nutQr = nutAnh(R.drawable.ghichu_ic_qr, "Quét QR") {
            startActivity(Intent(this, QuetQrActivity::class.java))
        }
        nutThuMuc = nutAnh(R.drawable.ic_thu_muc, "Thư mục") { hienThuMuc() }
        phai.addView(nutTim)
        phai.addView(nutQr)''', 'nut kinh lup')

# --- o tim: an mac dinh
s = thay(s, '''        hang.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
        hangTim = hang
        return hang
    }''',
'''        hang.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
        // Nguoi dung chot: an thanh tim; chi hien khi vao che do tim.
        hang.visibility = View.GONE
        hangTim = hang
        return hang
    }''', 'o tim an')

# --- chip: hai ban (trong header cuon + ghim)
s = thay(s, '''    private fun napChip() {
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
    }''',
'''    /** Ban ghim cua hang chip: giong het hang trong header, nam ngoai vung cuon. */
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
    }''', 'napChip hai ban')

# --- thu gon tieu de: tieu de nho hien dan, chip ghim hien khi tieu de khuat
s = thay(s, '''    private fun thuGonTieuDe(t: Int) {
        val cao = tieuDeLon.height.coerceAtLeast(1)
        val f = (t.toFloat() / cao).coerceIn(0f, 1f)
        tieuDeLon.alpha = 1f - f
        tieuDeNho.alpha = f
    }''',
'''    private fun thuGonTieuDe(t: Int) {
        if (tabHienTai != 0 || dangTim) return
        val cao = tieuDeLon.height.coerceAtLeast(1)
        val f = (t.toFloat() / cao).coerceIn(0f, 1f)
        tieuDeLon.alpha = 1f - f
        tieuDeNho.alpha = f
        // Tieu de da cuon khuat -> hang chip ghim thay cho hang trong header.
        val ghim = t >= cao && hangChip.tag != "an"
        hangChipGhim.visibility = if (ghim) View.VISIBLE else View.GONE
    }''', 'thuGonTieuDe')

# --- che do tim: an header trong luoi + chip ghim, hien hang tim
s = thay(s, '''        dangTim = true
        for (v in listOf(thanhTren, tieuDeLon, hangChip)) {
            v.animate().alpha(0f).setDuration(200).withEndAction { if (dangTim) v.visibility = View.GONE }.start()
        }
        nutThem.visibility = View.GONE''',
'''        dangTim = true
        for (v in listOf<View>(thanhTren, luoi.dauTrang)) {
            v.animate().alpha(0f).setDuration(200).withEndAction { if (dangTim) v.visibility = View.GONE }.start()
        }
        hangChipGhim.visibility = View.GONE
        hangTim.visibility = View.VISIBLE
        hangTim.alpha = 0f
        hangTim.animate().alpha(1f).setDuration(200).start()
        nutThem.visibility = View.GONE''', 'vao tim')

s = thay(s, '''        for (v in listOf(thanhTren, tieuDeLon, hangChip)) {
            if (v === hangChip && hangChip.tag == "an") continue
            v.visibility = View.VISIBLE
            v.animate().alpha(1f).setDuration(200).start()
        }
        nutThem.visibility = View.VISIBLE''',
'''        for (v in listOf<View>(thanhTren, luoi.dauTrang)) {
            v.visibility = View.VISIBLE
            v.animate().alpha(1f).setDuration(200).start()
        }
        hangTim.animate().alpha(0f).setDuration(200).withEndAction { hangTim.visibility = View.GONE }.start()
        nutThem.visibility = View.VISIBLE''', 'thoat tim')

# --- tab Lich trinh: tieu de lon rieng (tieu de cua ghi chu gio nam trong luoi)
s = thay(s, '''        if (i == 1 && khungLich == null) {
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
        }''',
'''        if (i == 1 && khungLich == null) {
            val man = ManLichTrinh(this)
            // Tab Lich trinh co tieu de lon rieng, cung kieu 32sp, nam tren lich.
            tieuDeLich = TextView(this).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                setTextColor(mau.chuChinh)
                setPadding(dp(46), dp(4), dp(46), dp(10))
                isSingleLine = true
            }
            man.datTieuDe = { chu ->
                tieuDeLich.text = chu
                if (tabHienTai == 1) tieuDeNho.text = chu
            }
            val cot = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            cot.addView(tieuDeLich)
            cot.addView(man.taoView(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            manLich = man
            khungLich = cot
            khungNoiDung.addView(cot, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
            ))
        }''', 'tieu de lich')

s = thay(s, '''        nutQr.visibility = if (laLich) View.GONE else View.VISIBLE
        nutThuMuc.visibility = if (laLich) View.GONE else View.VISIBLE
        tieuDeLon.alpha = 1f
        tieuDeNho.alpha = 0f''',
'''        nutQr.visibility = if (laLich) View.GONE else View.VISIBLE
        nutThuMuc.visibility = if (laLich) View.GONE else View.VISIBLE
        nutTim.visibility = if (laLich) View.GONE else View.VISIBLE
        tieuDeNho.alpha = if (laLich) 0f else (tieuDeNho.alpha)
        if (!laLich) thuGonTieuDe(luoi.scrollY)''', 'chuyenTab tim')

ghi('ManHinhChinhActivity.kt', s)

# ============================================================ Man soan
s = doc('SuaGhiChuActivity.kt')
# 1. bo Chen anh + So do tu duy khoi toolbar (giu code doc/ve noi dung cu)
s = thay(s, '''        trai.addView(nutToolbar(R.drawable.ic_ghi_am, "Ghi âm") { ghiAm() })
        trai.addView(nutToolbar(R.drawable.ic_anh, "Chèn ảnh") { chonAnh() })
        trai.addView(nutToolbar(R.drawable.ic_goc_ban_do, "Bản đồ tư duy") { chenBanDo() })
        trai.addView(nutToolbar(R.drawable.ic_o_tick, "Ô tick") { doiOTick() })''',
'''        trai.addView(nutToolbar(R.drawable.ic_ghi_am, "Ghi âm") { ghiAm() })
        // Nguoi dung chot (2026-08-26): KHONG can chen anh va so do tu duy — bo hai
        // nut khoi toolbar. Ma doc/ve noi dung cu van giu de ghi chu co san khong hong.
        trai.addView(nutToolbar(R.drawable.ic_o_tick, "Ô tick") { doiOTick() })''', 'bo anh + ban do')

# 2. toolbar sat day: dem thanh dieu huong / ban phim nam TRONG toolbar
s = thay(s, '''            view.setPadding(0, thanh.top, 0, maxOf(thanh.bottom, banPhim.bottom))''',
'''            // Dem duoi dat vao CHINH toolbar (navi_placeholder cua ban goc) de nen
            // toolbar om luon vung thanh dieu huong — khong con khoang ho duoi toolbar.
            view.setPadding(0, thanh.top, 0, 0)
            thanhCongCu.setPadding(0, 0, 0, maxOf(thanh.bottom, banPhim.bottom))''', 'toolbar sat day')
s = thay(s, '''    private lateinit var bangDinhDang: View''',
'''    private lateinit var bangDinhDang: View
    private lateinit var thanhCongCu: LinearLayout''', 'khai bao thanhCongCu')
s = thay(s, '''        cot.addView(bangDinhDang)
        return cot
    }''',
'''        cot.addView(bangDinhDang)
        thanhCongCu = cot
        return cot
    }''', 'gan thanhCongCu')
ghi('SuaGhiChuActivity.kt', s)
print('XONG')
