# -*- coding: utf-8 -*-
# Nut "Giao dien" tren toolbar man soan -> dai NGANG chon theme (mau + anh) hien
# ngay tren toolbar; ap nen + mau chu cua theme cho ca man soan.
import io, os, sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
p = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'kotlin', 'com', 'ghichu', 'SuaGhiChuActivity.kt')
s = io.open(p, encoding='utf-8').read()

def thay(a, b, ten):
    global s
    if a not in s: raise SystemExit('THIEU NEO: ' + ten)
    s = s.replace(a, b, 1); print('va', ten)

# 1. Nut Giao dien tren toolbar (canh Ghi am / O tick)
thay('''        trai.addView(nutToolbar(R.drawable.ic_o_tick, "Ô tick") { doiOTick() })''',
'''        trai.addView(nutToolbar(R.drawable.ic_o_tick, "Ô tick") { doiOTick() })
        trai.addView(nutToolbar(R.drawable.ic_giao_dien, "Giao diện") { doiDaiTheme() })''', 'nut giao dien')

# 2. Dai theme + bang dinh dang cung nam trong cot toolbar; them dai theme
thay('''        cot.addView(bangDinhDang)
        thanhCongCu = cot
        return cot
    }''',
'''        cot.addView(bangDinhDang)
        cot.addView(dungDaiTheme())
        thanhCongCu = cot
        return cot
    }

    private lateinit var daiTheme: View
    private lateinit var dayTheme: LinearLayout

    /**
     * Dai chon giao dien: HorizontalScrollView keo ngang, moi o la mot theme
     * (mau dac hoac anh nen) ve thu nho. Bam la ap ngay. Nam ngay TREN toolbar,
     * an mac dinh — dung kieu v12_theme_chooser_panel cua ban goc (cao ~90dp).
     */
    private fun dungDaiTheme(): View {
        dayTheme = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        // Truoc: "Không màu" + 7 mau giay (MauGhiChu 0..7).
        for (i in 0 until MauGhiChu.soMau) themOTheme(i, MauGhiChu.TEN[i])
        // Sau: cac theme anh (id 8+).
        for (t in BoTheme.DANH_SACH) themOTheme(t.id, t.ten)

        daiTheme = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            visibility = View.GONE
            addView(dayTheme)
        }
        return daiTheme
    }

    private fun themOTheme(id: Int, ten: String) {
        val o = FrameLayout(this)
        val xem = View(this).apply {
            background = xemTruocTheme(id)
        }
        o.addView(xem, FrameLayout.LayoutParams(dp(56), dp(72)).apply {
            leftMargin = dp(6); rightMargin = dp(6); topMargin = dp(4); bottomMargin = dp(4)
        })
        // Vien vang khi dang chon (v12_ic_theme_cover_frame): bo 12dp, 2.9dp #FFBB0F.
        val vien = View(this).apply { tag = "vien_$id" }
        vien.background = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setStroke((2.9f * md.density).toInt(), mau.nhan)
            setColor(0)
        }
        vien.visibility = if (id == mauNen) View.VISIBLE else View.GONE
        o.addView(vien, FrameLayout.LayoutParams(dp(56), dp(72)).apply {
            leftMargin = dp(6); rightMargin = dp(6); topMargin = dp(4); bottomMargin = dp(4)
        })
        o.isClickable = true
        o.setOnClickListener { chonTheme(id) }
        dayTheme.addView(o)
    }

    /** Nen xem truoc mot theme trong dai (bo goc 12dp). */
    private fun xemTruocTheme(id: Int): android.graphics.drawable.Drawable {
        val nenTh = BoTheme.theo(id)?.nen?.invoke(this, mau)
            ?: GradientDrawable().apply { setColor(MauGhiChu.xemTruoc(id, mau)) }
        // Bo goc: boc trong lop cat goc.
        return object : android.graphics.drawable.Drawable() {
            private val duong = android.graphics.Path()
            override fun draw(canvas: android.graphics.Canvas) {
                duong.reset()
                duong.addRoundRect(
                    android.graphics.RectF(bounds), dp(12).toFloat(), dp(12).toFloat(),
                    android.graphics.Path.Direction.CW,
                )
                canvas.save(); canvas.clipPath(duong)
                nenTh.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
                nenTh.draw(canvas)
                canvas.restore()
            }
            override fun setAlpha(a: Int) {}
            override fun setColorFilter(f: android.graphics.ColorFilter?) {}
            @Deprecated("deprecated") override fun getOpacity() = android.graphics.PixelFormat.OPAQUE
        }
    }

    private fun chonTheme(id: Int) {
        val cu = mauNen
        mauNen = id
        toMau()
        // Doi vien chon: tat o cu, bat o moi.
        dayTheme.findViewWithTag<View>("vien_$cu")?.visibility = View.GONE
        dayTheme.findViewWithTag<View>("vien_$id")?.visibility = View.VISIBLE
    }

    private fun doiDaiTheme() {
        // Mo dai theme thi dong bang dinh dang, va nguoc lai — khong chong nhau.
        bangDinhDang.visibility = View.GONE
        daiTheme.visibility = if (daiTheme.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }''', 'dai theme')

# 3. Nut "T" cung dong dai theme khi mo bang dinh dang
thay('''            setOnClickListener {
                bangDinhDang.visibility = if (bangDinhDang.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }''',
'''            setOnClickListener {
                daiTheme.visibility = View.GONE
                bangDinhDang.visibility = if (bangDinhDang.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }''', 'nut T dong dai theme')

# 4. toMau: ap theme anh (nen + mau chu + statusBar)
thay('''    /**
     * Nen man soan: theme mac dinh cua ban goc la v8_theme_bg_color = #FFFFFF /
     * #000000 (KHONG phai mau the #242424); co chon mau giay thi theo mau do.
     */
    private fun toMau() {
        val nenGiay = if (mauNen == 0) (if (mau.toi) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            else MauGhiChu.nen(mauNen, mau)
        nen.setBackgroundColor(nenGiay)
    }''',
'''    /**
     * Nen man soan: mau dac (0..7 qua MauGhiChu) hoac theme anh (8+ qua BoTheme).
     * Theme anh keo theo mau chu rieng (doc tren nen dam/nhat) va mau chu tieu de,
     * than bai, dong thong tin.
     */
    private fun toMau() {
        val theme = BoTheme.theo(mauNen)
        if (theme != null) {
            nen.background = theme.nen(this, mau)
            val cc = theme.mauChuChinh(mau)
            val cp = theme.mauChuPhu(mau)
            oTieuDe.setTextColor(cc)
            oThan.setTextColor(cc)
            oChan.setTextColor(cp)
            oDem.setTextColor(cp)
        } else {
            val nenGiay = if (mauNen == 0) (if (mau.toi) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                else MauGhiChu.nen(mauNen, mau)
            nen.setBackgroundColor(nenGiay)
            val cc = if (mau.toi) 0xE6FFFFFF.toInt() else 0xFF000000.toInt()
            oTieuDe.setTextColor(cc)
            oThan.setTextColor(cc)
            oChan.setTextColor(mau.chuThongTin)
            oDem.setTextColor(mau.chuThongTin)
        }
    }''', 'toMau theme')

# 5. Back: dai theme dang mo thi dong truoc
thay('''    override fun onPause() {
        luu()
        super.onPause()
    }''',
'''    override fun onPause() {
        luu()
        super.onPause()
    }

    override fun onBackPressed() {
        if (::daiTheme.isInitialized && daiTheme.visibility == View.VISIBLE) {
            daiTheme.visibility = View.GONE
            return
        }
        if (::bangDinhDang.isInitialized && bangDinhDang.visibility == View.VISIBLE) {
            bangDinhDang.visibility = View.GONE
            return
        }
        super.onBackPressed()
    }''', 'back dong dai theme')

io.open(p, 'w', encoding='utf-8').write(s)
print('XONG')
