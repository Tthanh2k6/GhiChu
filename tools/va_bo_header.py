# -*- coding: utf-8 -*-
# Nguoi dung chot: XOA HAN header cuon (tieu de lon + thanh tim trong header + kinh mo
# tren). Thanh 56dp thanh DINH CO DINH cua trang: tieu de "Ghi chu" ben trai + icon
# ben phai (them kinh lup). Hang chip la hang CO DINH ngay duoi thanh, luon hien.
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

# ---- 1. onCreate: cau truc moi — thanh 56dp + hang chip co dinh o dau cot, khong kinh tren
thay('''        goc = LinearLayout(this).apply {
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
        luoi.dauTrang.addView(dungHangChip())
        khungGhiChu.addView(luoiView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
        ))
        val noiDung = FrameLayout(this)
        noiDung.addView(khungGhiChu, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        goc.addView(noiDung, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        khungNoiDung = noiDung

        // Thanh tren va thanh tab nam DE LEN noi dung (khong chiem cho trong cot):
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
        }
        nen.setOnApplyWindowInsetsListener { _, insets ->
            val thanh = insets.getInsets(WindowInsets.Type.systemBars())
            // Thanh tren om luon thanh trang thai; noi dung chua cho no (inset + 56dp).
            caoThanhTren = thanh.top + dp(56)
            // Kinh cao dung = inset + 56dp; thanh 56dp va o tim day xuong duoi inset.
            kinhTren.layoutParams = (kinhTren.layoutParams as FrameLayout.LayoutParams).apply { height = caoThanhTren }
            (thanhTren.layoutParams as? FrameLayout.LayoutParams)?.let { it.topMargin = thanh.top; thanhTren.layoutParams = it }
            (hangTim.layoutParams as? FrameLayout.LayoutParams)?.let { it.topMargin = thanh.top; hangTim.layoutParams = it }
            luoi.dauTrang.setPadding(0, caoThanhTren, 0, 0)
            oTrong.setPadding(dp(24), caoThanhTren + dp(154), dp(24), 0)
            khungLich?.setPadding(0, caoThanhTren, 0, caoThanhTab)''',
'''        goc = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(mau.nen)
        }

        // Nguoi dung chot: KHONG co header cuon. Thanh tren (56dp) la DINH CO DINH
        // cua trang; hang chip co dinh ngay duoi no. Ca hai la hai hang dau cua cot,
        // day xuong duoi thanh trang thai bang padding tren cua `goc`.
        goc.addView(dungKhungThanhTren())
        goc.addView(dungHangChip())

        // Vung noi dung: hai tab la hai khung chong nhau, doi bang visibility.
        khungGhiChu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        khungGhiChu.addView(dungLuoi(), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
        ))
        val noiDung = FrameLayout(this)
        noiDung.addView(khungGhiChu, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        goc.addView(noiDung, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        khungNoiDung = noiDung

        // Thanh tab duoi nam DE LEN noi dung (kinh mo). Thanh tren khong con la
        // overlay — no la hang dau cua cot.
        val nen = FrameLayout(this).apply {
            setBackgroundColor(mau.nen)
            addView(goc)
            addView(dungThanhDuoi(), FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.BOTTOM })
            addView(dungNutThem())
        }
        nen.setOnApplyWindowInsetsListener { _, insets ->
            val thanh = insets.getInsets(WindowInsets.Type.systemBars())
            // Ca cot day xuong duoi thanh trang thai — thanh tren la dinh trang.
            goc.setPadding(0, thanh.top, 0, 0)
            oTrong.setPadding(dp(24), dp(120), dp(24), 0)
            khungLich?.setPadding(0, 0, 0, caoThanhTab)''', 'onCreate cau truc moi')

# ---- 2. Bo padding luoi (khong con header trong luoi)
thay('''            demDuoi = caoThanhTab + dp(35) + dp(58) + dp(24)
            luoi.datDemDuoi(demDuoi)
            insets
        }''',
'''            demDuoi = caoThanhTab + dp(35) + dp(58) + dp(24)
            luoi.datDemDuoi(demDuoi)
            luoi.setPadding(0, 0, 0, 0)
            insets
        }''', 'luoi khong padding tren')

# ---- 3. dungThanhTren: tieu de TRAI + icon phai (them kinh lup); bo tieu de nho center
thay('''    private fun dungThanhTren(): View {
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

        return thanhTren
    }''',
'''    /**
     * Thanh tren la DINH trang: tieu de ben TRAI + icon ben PHAI. Khong con tieu de
     * lon cuon; tieu de nay doi thanh thang (vd "Tháng 8, 2026") khi o tab Lich
     * trinh, va "Riêng tư" / "Thùng rác" khi o cac muc do.
     */
    private fun dungThanhTren(): View {
        thanhTren = FrameLayout(this)

        val hang = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), 0, dp(20) - dp(9), 0)
        }
        tieuDeNho = TextView(this).apply {
            text = "Ghi chú"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(mau.chuChinh)
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        hang.addView(tieuDeNho, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        nutTim = nutAnh(R.drawable.ic_tim, "Tìm kiếm") { vaoCheDoTim() }
        nutQr = nutAnh(R.drawable.ghichu_ic_qr, "Quét QR") {
            startActivity(Intent(this, QuetQrActivity::class.java))
        }
        nutThuMuc = nutAnh(R.drawable.ic_thu_muc, "Thư mục") { hienThuMuc() }
        hang.addView(nutTim)
        hang.addView(nutQr)
        hang.addView(nutThuMuc)
        hang.addView(nutAnh(R.drawable.ic_cai_dat, "Cài đặt") { hienCaiDat() })
        thanhTren.addView(hang, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(56)))

        thanhChon = dungThanhChon()
        thanhTren.addView(thanhChon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(56)))

        return thanhTren
    }

    private lateinit var nutTim: View''', 'dungThanhTren tieu de trai')

# ---- 4. dungKhungThanhTren: bo kinh mo, chi con thanh 56dp + hang tim; nen dac
thay('''    /**
     * Khung ngoai: kinh mo phia sau + thanh 56dp; om luon thanh trang thai.
     *
     * KHONG dat padding tren cho khung nay — padding cha se CONG vao chieu cao kinh
     * (kinh cao caoThanhTren) lam kinh dư ra dung mot khoang bang thanh trang thai,
     * phu de len dau tieu de. Thay vao do, thanh 56dp va o tim tu dat topMargin =
     * inset trang thai (cap lai o listener inset).
     */
    private fun dungKhungThanhTren(): View {
        khungThanhTren = FrameLayout(this)
        kinhTren = KinhMo(
            this, mau,
            mauPhu = if (mau.toi) 0xF01C1C1C.toInt() else 0xF0FFFFFF.toInt(),
        ) { if (::khungNoiDung.isInitialized) khungNoiDung else null }
        khungThanhTren.addView(kinhTren, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0))
        val thanh = dungThanhTren()
        khungThanhTren.addView(thanh, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(56)))
        // Hang tim (che do tim) thay the thanh 56dp, cung mot khung.
        khungThanhTren.addView(dungOTim(), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(56)))
        return khungThanhTren
    }

    /**''',
'''    /** Dinh trang: thanh 56dp (tieu de + icon) + hang tim (che do tim) chong nhau. */
    private fun dungKhungThanhTren(): View {
        khungThanhTren = FrameLayout(this).apply { setBackgroundColor(mau.nen) }
        khungThanhTren.addView(dungThanhTren(), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(56)))
        khungThanhTren.addView(dungOTim(), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(56)))
        khungThanhTren.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
        return khungThanhTren
    }

    /**''', 'dungKhungThanhTren khong kinh')

# ---- 5. Bo dungTieuDeLon va dungNutTim (khong con dung)
thay('''    /**
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

''', '', 'bo dungTieuDeLon')

# ---- 6. dungHangChip: hang co dinh, nen dac, luon hien (khong con trong luoi)
thay('''    private fun dungOTim(): View {''',
'''    /** Hang chip thu muc CO DINH ngay duoi thanh tren — luon hien o tab Ghi chu. */
    private lateinit var hangChipCoDinh: android.widget.HorizontalScrollView
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
            setBackgroundColor(mau.nen)
            addView(dayChip)
        }
        hangChipCoDinh = hangChip
        return hangChip
    }

    private fun dungOTim(): View {''', 'dungHangChip co dinh')

# xoa ham dungHangChip cu neu con (da bi thay o dot truoc bang bien the khac) — bo qua neu khong co
# ---- 7. napChip: chi mot ban, an/hien ca hang
thay('''    private fun napChip() {
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
    }''',
'''    private fun napChip() {
        val ds = kho.thuMucKemSo().map { it.first }
        // Hang chip chi hien o tab Ghi chu, khong o thung rac / rieng tu / tim.
        if (tabHienTai != 0 || thungRac || riengTu || dangTim || ds.isEmpty()) {
            hangChipCoDinh.visibility = View.GONE
            return
        }
        hangChipCoDinh.visibility = View.VISIBLE

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
        chipChon?.let { c -> hangChipCoDinh.post { hangChipCoDinh.smoothScrollTo((c.left - dp(12)).coerceAtLeast(0), 0) } }
    }''', 'napChip mot ban')

# ---- 8. Bo dungNutTim (thanh tim trong header) va nutTimHeader
thay('''    /**
     * Thanh tim TRONG header (hien o dau trang, cuon xuong thi troi theo header).
     * La mot NUT: bam vao mo che do tim voi o nhap that o thanh tren — dung nhu
     * ban goc (ca thanh la nut mo action mode tim).
     */
    private fun dungNutTim(): View {''',
'''    private fun dungNutTim_BO(): View {''', 'vo hieu dungNutTim')
# de ham dungNutTim_BO khong bi goi — khong ai goi no nua. Nhung no tham chieu nutTimHeader.
# Don gian hon: xoa han than ham. Tim va cat.
import re
m = re.search(r'    private fun dungNutTim_BO\(\): View \{', s)
if m:
    # cat tu dau ham den 'private lateinit var nutTimHeader: View' (ket thuc khoi)
    start = m.start()
    end_anchor = '    private lateinit var nutTimHeader: View\n'
    e = s.find(end_anchor, start)
    if e >= 0:
        s = s[:start] + s[e + len(end_anchor):]
        print('va xoa than dungNutTim')

# ---- 9. thuGonTieuDe: khong con (bo moi cho goi)
s = s.replace('        thuGonTieuDe(t)\n', '')
s = s.replace('        if (!laLich) thuGonTieuDe(luoi.scrollY)\n', '')
# ham thuGonTieuDe: bo han
m = re.search(r'    private fun thuGonTieuDe\(t: Int\) \{.*?\n    \}\n', s, re.S)
if m:
    s = s[:m.start()] + s[m.end():]
    print('va xoa thuGonTieuDe')

# ---- 10. Cuon: bo kinhTren invalidate (khong con)
s = s.replace('''        luoi.khiCuon = { t ->
            if (::kinhTab.isInitialized) kinhTab.invalidate()
            if (::kinhTren.isInitialized) kinhTren.invalidate()
        }''',
'''        luoi.khiCuon = {
            if (::kinhTab.isInitialized) kinhTab.invalidate()
        }''')
s = s.replace('        if (::kinhTren.isInitialized) kinhTren.postInvalidate()\n', '')
s = s.replace('        kinhTren.postInvalidate()\n', '')

# ---- 11. Tieu de theo tab / muc: dat truc tiep tieuDeNho (khong con tieuDeLon)
thay('''        if (tabHienTai == 0) {
            tieuDeLon.text = ten
            tieuDeNho.text = ten
        }

        if (dangTim) {''',
'''        if (tabHienTai == 0) tieuDeNho.text = ten

        if (dangTim) {''', 'nap tieu de')

# ---- 12. chuyenTab: bo tieuDeLon, bo tieu de lich rieng (dung tieuDeNho), thanh tren nen
thay('''        if (i == 1 && khungLich == null) {
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
            cot.setPadding(0, caoThanhTren, 0, caoThanhTab)
            khungLich = cot
            khungNoiDung.addView(cot, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
            ))
        }''',
'''        if (i == 1 && khungLich == null) {
            val man = ManLichTrinh(this)
            man.datTieuDe = { chu -> if (tabHienTai == 1) tieuDeNho.text = chu }
            val v = man.taoView()
            manLich = man
            v.setPadding(0, 0, 0, caoThanhTab)
            khungLich = v
            khungNoiDung.addView(v, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
            ))
        }''', 'chuyenTab lich')

thay('''        khungGhiChu.visibility = if (laLich) View.GONE else View.VISIBLE
        khungLich?.visibility = if (laLich) View.VISIBLE else View.GONE
        nutThem.visibility = if (laLich) View.GONE else View.VISIBLE
        nutQr.visibility = if (laLich) View.GONE else View.VISIBLE
        nutThuMuc.visibility = if (laLich) View.GONE else View.VISIBLE
        tieuDeNho.alpha = if (laLich) 0f else (tieuDeNho.alpha)''',
'''        khungGhiChu.visibility = if (laLich) View.GONE else View.VISIBLE
        khungLich?.visibility = if (laLich) View.VISIBLE else View.GONE
        nutThem.visibility = if (laLich) View.GONE else View.VISIBLE
        nutQr.visibility = if (laLich) View.GONE else View.VISIBLE
        nutThuMuc.visibility = if (laLich) View.GONE else View.VISIBLE
        nutTim.visibility = if (laLich) View.GONE else View.VISIBLE
        hangChipCoDinh.visibility = if (laLich) View.GONE else View.VISIBLE
        if (laLich) tieuDeNho.text = "Lịch trình"''', 'chuyenTab visibility')

# ---- 13. Che do tim: an thanh tren + hang chip (khong con luoi.dauTrang)
thay('''        for (v in listOf<View>(thanhTren, luoi.dauTrang)) {
            v.animate().alpha(0f).setDuration(200).withEndAction { if (dangTim) v.visibility = View.GONE }.start()
        }
        hangTim.visibility = View.VISIBLE
        hangTim.alpha = 0f
        hangTim.animate().alpha(1f).setDuration(200).start()
        nutThem.visibility = View.GONE''',
'''        thanhTren.animate().alpha(0f).setDuration(200).withEndAction { if (dangTim) thanhTren.visibility = View.GONE }.start()
        hangChipCoDinh.visibility = View.GONE
        hangTim.visibility = View.VISIBLE
        hangTim.alpha = 0f
        hangTim.animate().alpha(1f).setDuration(200).start()
        nutThem.visibility = View.GONE''', 'vao tim')

thay('''        for (v in listOf<View>(thanhTren, luoi.dauTrang)) {
            v.visibility = View.VISIBLE
            v.animate().alpha(1f).setDuration(200).start()
        }
        hangTim.animate().alpha(0f).setDuration(200).withEndAction { hangTim.visibility = View.GONE }.start()
        nutThem.visibility = View.VISIBLE''',
'''        thanhTren.visibility = View.VISIBLE
        thanhTren.animate().alpha(1f).setDuration(200).start()
        hangTim.animate().alpha(0f).setDuration(200).withEndAction { hangTim.visibility = View.GONE }.start()
        nutThem.visibility = View.VISIBLE
        napChip()''', 'thoat tim')

# ---- 14. Bo khai bao tieuDeLon (khong con)
s = s.replace('    private lateinit var tieuDeLon: TextView\n', '')
s = s.replace('    private lateinit var tieuDeLich: TextView\n', '')

io.open(p, 'w', encoding='utf-8').write(s)
print('XONG')
