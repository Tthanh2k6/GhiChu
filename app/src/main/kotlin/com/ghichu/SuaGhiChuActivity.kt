package com.ghichu

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.ReplacementSpan

/**
 * Soan mot ghi chu.
 *
 * Ca bai nam trong **MOT `EditText`** — ly do va cach lam o [SoanThao].
 *
 * **Tu luu, khong co nut Luu.** Luu o `onPause` — nguoi dung go xong roi bam
 * back hoac chuyen app, khong ai di tim nut luu. Ghi chu rong thi khong tao hang
 * (xem [luu]).
 */
class SuaGhiChuActivity : Activity() {

    private val md by lazy { resources.displayMetrics }
    private fun dp(v: Int) = (v * md.density).toInt()

    private lateinit var mau: BangMau
    private val kho: KhoGhiChu get() = Ung.cua(this).ghiChu

    private var id = 0L
    private var goc: GhiChu? = null
    private var thuMuc = ""
    private var mauNen = 0
    private var ghim = false
    private var nhacLuc: Long? = null

    private lateinit var oTieuDe: EditText
    private lateinit var oThan: EditText
    private lateinit var nen: LinearLayout
    private lateinit var oChan: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mau = BangMau.cua(this)
        setTheme(mau.themeManHinh)

        id = intent.getLongExtra(KHOA_ID, 0L)
        thuMuc = intent.getStringExtra(KHOA_THU_MUC).orEmpty()

        val g = if (id != 0L) kho.theoId(id) else null
        goc = g
        if (g != null) {
            thuMuc = g.thuMuc
            mauNen = g.mau
            ghim = g.ghim
            nhacLuc = g.nhacLuc
        }

        // Ve tran vien, thanh trang thai trong suot — nhu theme goc.
        window.statusBarColor = 0
        window.navigationBarColor = 0
        window.decorView.systemUiVisibility = if (mau.toi) 0 else
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(
            if (mau.toi) 0xFF000000.toInt() else 0xFFFFFFFF.toInt(),
        ))

        nen = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        nen.addView(dungThanhTren())
        nen.addView(dungThan(g), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
        ))
        nen.addView(dungThanhCongCu())

        nen.setOnApplyWindowInsetsListener { view, insets ->
            val thanh = insets.getInsets(WindowInsets.Type.systemBars())
            val banPhim = insets.getInsets(WindowInsets.Type.ime())
            // Lay cai LON hon giua ban phim va thanh dieu huong, khong cong don:
            // ban phim bat len la no da phu len thanh dieu huong roi, cong ca hai
            // thi thua ra mot khoang trong bang chieu cao thanh.
            view.setPadding(0, thanh.top, 0, maxOf(thanh.bottom, banPhim.bottom))
            // Ban phim mo = dang SOAN: ban goc doi nhom nut phai sang undo/redo/done.
            // O day: an share/mau/them, hien "Xong" (khong co undo/redo — EditText
            // cua khung khong mo API undo).
            val dangSoan = banPhim.bottom > 0
            nhomXem.visibility = if (dangSoan) View.GONE else View.VISIBLE
            nutXong.visibility = if (dangSoan) View.VISIBLE else View.GONE
            insets
        }
        setContentView(nen)
        toMau()
    }

    override fun onPause() {
        luu()
        super.onPause()
    }

    // ------------------------------------------------------------ dung giao dien

    private lateinit var nhomXem: LinearLayout
    private lateinit var nutXong: View
    private lateinit var nutThemMenu: View

    /**
     * Thanh tren 56dp (miuix_appcompat_action_bar_default_height), nen trong suot.
     * Trai: nut back 40dp (v12_notes_edit_header_ic_size). Phai: che do XEM =
     * chia se / mau giay / them, moi nut 40dp cach nhau 12dp
     * (v12_notes_edit_header_ic_margin); che do SOAN = nut Xong.
     * Tint icon = action_button_color (#000 / #fff). KHONG ripple.
     */
    private fun dungThanhTren(): View {
        val thanh = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(12), 0)
        }
        thanh.addView(nutAnh(R.drawable.ic_quay_lai, "Quay lại") { finish() })
        thanh.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))

        nhomXem = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        nhomXem.addView(nutChuNho("↗", "Chia sẻ") { hienXuat() })
        nhomXem.addView(nutChuNho("◐", "Màu giấy") { hienChonMauGiay() }.also {
            (it.layoutParams as LinearLayout.LayoutParams).leftMargin = dp(12)
        })
        nutThemMenu = nutChuNho("⋯", "Thêm") { hienThem() }.also {
            (it.layoutParams as LinearLayout.LayoutParams).leftMargin = dp(12)
        }
        nhomXem.addView(nutThemMenu)
        thanh.addView(nhomXem)

        nutXong = nutChuNho("✓", "Xong") {
            oThan.clearFocus()
            oTieuDe.clearFocus()
            getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                ?.hideSoftInputFromWindow(nen.windowToken, 0)
        }.apply { visibility = View.GONE }
        thanh.addView(nutXong)

        thanh.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
        return thanh
    }

    private lateinit var chipThuMuc: TextView
    private lateinit var oDem: TextView

    private fun dungThan(g: GhiChu?): View {
        val cuon = ScrollView(this).apply { isFillViewport = true }
        val cot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // note_editor_padding = 12dp; edit_content paddingBottom 120dp de cuon
            // lo ra cuoi trang.
            setPadding(dp(12), 0, dp(12), dp(120))
        }

        // Chip thu muc (note_edit_folder_layout): cao 36dp, bo goc 12dp, vien
        // 0.5dp #0D000000, ruot #EEEEEE / #242424; icon 15dp cach trai 16dp, chu
        // 12sp medium #CC000000 / #FFFFFF cach icon 4dp, cach phai 16dp; duoi 12dp.
        chipThuMuc = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(if (mau.toi) 0xFFFFFFFF.toInt() else 0xCC000000.toInt())
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            compoundDrawablePadding = dp(4)
            val icon = resources.getDrawable(R.drawable.ic_chip_thu_muc, null).mutate().apply {
                setBounds(0, 0, dp(15), dp(15))
                setTint(if (mau.toi) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            }
            setCompoundDrawables(icon, null, null, null)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (mau.toi) 0xFF242424.toInt() else 0xFFEEEEEE.toInt())
                setStroke((0.5f * md.density).toInt().coerceAtLeast(1), 0x0D000000)
            }
            isClickable = true
            setOnClickListener { hienChonThuMuc() }
        }
        cot.addView(chipThuMuc, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(36),
        ).apply { bottomMargin = dp(12) })

        // Tieu de: note_edit_title_font_size = 24sp, semibold, minHeight 40sp,
        // hint mau note_title_hint_color_default #CCCCCC / #333333.
        oTieuDe = EditText(this).apply {
            hint = "Tiêu đề"
            setHintTextColor(if (mau.toi) 0xFF333333.toInt() else 0xFFCCCCCC.toInt())
            setTextColor(if (mau.toi) 0xE6FFFFFF.toInt() else 0xFF000000.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = null
            isSingleLine = true
            minHeight = (40 * resources.displayMetrics.scaledDensity).toInt()
            setPadding(0, 0, 0, 0)
            setText(g?.tieuDe.orEmpty())
            filters = arrayOf(android.text.InputFilter.LengthFilter(100))
        }
        cot.addView(oTieuDe)

        // Dong thong tin (note_extra_info_layout): "19 tháng 8  21:47 | 123 ký tự",
        // 12sp mau note_edit_extra_info_text_color; vach dung 0.5dp x 10sp cach
        // hai ben 10dp; marginTop 8dp, marginBottom 28dp.
        val hangTin = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        oChan = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(mau.chuThongTin)
            text = nhanChan(g)
        }
        hangTin.addView(oChan)
        hangTin.addView(View(this).apply { setBackgroundColor(mau.chuThongTin) },
            LinearLayout.LayoutParams(
                (0.5f * md.density).toInt().coerceAtLeast(1),
                (10 * resources.displayMetrics.scaledDensity).toInt(),
            ).apply { leftMargin = dp(10); rightMargin = dp(10) })
        oDem = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(mau.chuThongTin)
        }
        hangTin.addView(oDem)
        cot.addView(hangTin, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(8); bottomMargin = dp(28) })

        // Than bai: text_font_size = 16sp, gian dong mac dinh, minHeight 200dp.
        oThan = EditText(this).apply {
            hint = "Bắt đầu viết…"
            setHintTextColor(mau.chuMo)
            setTextColor(if (mau.toi) 0xE6FFFFFF.toInt() else 0xFF000000.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            background = null
            gravity = Gravity.TOP
            minHeight = dp(200)
            setPadding(0, 0, 0, 0)
            setText(SoanThao.doc(g?.noiDung.orEmpty()), TextView.BufferType.SPANNABLE)
            setOnTouchListener { _, su -> chamVaoThan(su) }
            addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    trangTriOTick()
                    capNhatDem()
                }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            })
        }
        cot.addView(oThan, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        trangTriOTick()
        capNhatDem()
        capNhatChip()

        // Phai doi do rong that cua o soan thao roi moi ve anh — luc dung view no
        // van con bang 0, ve luc do la anh be ti mot chuong.
        oThan.post { veAnh() }

        // Ghi chu moi thi mo ban phim luon — nguoi dung bam "+" la de viet.
        if (g == null) {
            oThan.requestFocus()
            oThan.post {
                getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                    ?.showSoftInput(oThan, 0)
            }
        }

        cuon.addView(cot)
        return cuon
    }

    /** "123 ký tự" — plurals word_count cua ban goc. */
    private fun capNhatDem() {
        oDem.text = oThan.text.length.toString() + " ký tự"
    }

    private fun capNhatChip() {
        chipThuMuc.text = thuMuc.ifEmpty { "Chưa xếp thư mục" }
    }

    // ------------------------------------------------------------ o tick trong bai

    /**
     * Ve o tick bang vector goc (ic_richtext_checkbox_u / _c, 16dp) thay cho ky tu
     * ☐ ☑, va lam mo 40% chu cua dong DA tick — dung CheckboxSpan cua ban goc
     * (`textPaint.setAlpha(102)`, khong gach ngang).
     *
     * Du lieu ben duoi VAN la ky tu "☐ " / "☑ " (xem [SoanThao]); o day chi khoac
     * span ve len. Goi sau moi lan chu doi (TextWatcher) — span cu bi lech thi go
     * het roi dat lai, re hon nhieu so voi theo doi tung phep sua.
     */
    private fun trangTriOTick() {
        val chu = oThan.text as? Spannable ?: return
        for (cu in chu.getSpans(0, chu.length, TheOTick::class.java)) chu.removeSpan(cu)
        for (cu in chu.getSpans(0, chu.length, MoChuTick::class.java)) chu.removeSpan(cu)

        val co = (16 * resources.displayMetrics.scaledDensity).toInt()
        var dau = 0
        val toan = chu.toString()
        while (dau <= toan.length) {
            val cuoi = toan.indexOf('\n', dau).let { if (it < 0) toan.length else it }
            val daTick = toan.startsWith(SoanThao.O_TICK, dau)
            if (daTick || toan.startsWith(SoanThao.O_TRONG, dau)) {
                chu.setSpan(TheOTick(daTick, co), dau, dau + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (daTick && cuoi > dau + 2) {
                    chu.setSpan(MoChuTick(), dau + 2, cuoi, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            if (cuoi >= toan.length) break
            dau = cuoi + 1
        }
    }

    /** O tick 16dp + khoang cach 12dp (note_edit_checkbox_margin_end) toi chu. */
    private inner class TheOTick(val daTick: Boolean, val co: Int) : ReplacementSpan() {
        private val hinh: Drawable = resources.getDrawable(
            if (daTick) R.drawable.ic_tick_da else R.drawable.ic_tick_trong, null,
        ).mutate().apply {
            setBounds(0, 0, co, co)
            // richTextListElementPrimaryColor #8C93B0 / #FFF; ...CheckedColor #FFBB00 / #FFF.
            setTint(
                when {
                    daTick && mau.toi -> 0xFFFFFFFF.toInt()
                    daTick -> 0xFFFFBB00.toInt()
                    mau.toi -> 0xFFFFFFFF.toInt()
                    else -> 0xFF8C93B0.toInt()
                },
            )
        }

        override fun getSize(p: Paint, t: CharSequence?, a: Int, b: Int, fm: Paint.FontMetricsInt?): Int =
            co + dp(12)

        override fun draw(c: Canvas, t: CharSequence?, a: Int, b: Int, x: Float, top: Int, y: Int, bottom: Int, p: Paint) {
            // bounds.bottom = baseline + 0.15*size, nhu CheckboxSpan goc.
            val duoi = y + (co * 0.15f).toInt()
            c.save()
            c.translate(x, (duoi - co).toFloat())
            hinh.draw(c)
            c.restore()
        }
    }

    /** Chu cua dong da tick: alpha 102/255 (40%) — CheckboxSpan.updateDrawState goc. */
    private class MoChuTick : CharacterStyle() {
        override fun updateDrawState(tp: TextPaint) { tp.alpha = 102 }
    }

    private lateinit var bangDinhDang: View

    /**
     * Thanh cong cu duoi (small_screen_mode_tool_group): hang 60dp
     * (notes_edit_toolbar_height), vach 0.5dp tren (toolbar_divide_color), nen
     * #F9FFFFFF / #F9171717. Trai (weight 0.81, paddingStart 15dp, paddingEnd 3dp):
     * nam nut 36dp chia deu — AI, ghi am, anh, [ban do thay cho ve tay], o tick.
     * Phai (weight 0.19): nut "T" (ic_tool_richtext) marginEnd 28dp, mo bang dinh
     * dang chu ben duoi (expand_container). Tint icon action_bottom_button_color.
     */
    private fun dungThanhCongCu(): View {
        val cot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(if (mau.toi) 0xF9171717.toInt() else 0xF9FFFFFF.toInt())
        }
        cot.addView(View(this).apply { setBackgroundColor(mau.vachToolbar) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (0.5f * md.density).toInt().coerceAtLeast(1)))

        val hang = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val trai = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(12), dp(3), dp(12))
        }
        trai.addView(nutToolbar(R.drawable.ic_ai, "Trợ lý AI") {
            startActivity(Intent(this, TroLyAiActivity::class.java))
        })
        trai.addView(nutToolbar(R.drawable.ic_ghi_am, "Ghi âm") { ghiAm() })
        trai.addView(nutToolbar(R.drawable.ic_anh, "Chèn ảnh") { chonAnh() })
        trai.addView(nutToolbar(R.drawable.ic_goc_ban_do, "Bản đồ tư duy") { chenBanDo() })
        trai.addView(nutToolbar(R.drawable.ic_o_tick, "Ô tick") { doiOTick() })
        hang.addView(trai, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.81f))

        val phai = FrameLayout(this)
        val nutAa = ImageView(this).apply {
            setImageResource(R.drawable.ic_aa)
            setColorFilter(mau.chuToolbar)
            contentDescription = "Định dạng chữ"
            isClickable = true
            setOnClickListener {
                bangDinhDang.visibility = if (bangDinhDang.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
            nhanMoDan(this)
        }
        phai.addView(nutAa, FrameLayout.LayoutParams(dp(36), dp(36)).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            rightMargin = dp(28)
        })
        hang.addView(phai, LinearLayout.LayoutParams(0, dp(60), 0.19f))
        cot.addView(hang, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)))

        // Bang dinh dang chu (expand_container), an mac dinh.
        val bang = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(12))
            visibility = View.GONE
        }
        bang.addView(nutKieu("B", "Đậm", DinhDang.DAM, dam = true))
        bang.addView(nutKieu("I", "Nghiêng", DinhDang.NGHIENG, nghieng = true))
        bang.addView(nutKieu("U", "Gạch chân", DinhDang.GACH_CHAN))
        bang.addView(nutKieu("S", "Gạch ngang", DinhDang.GACH_NGANG))
        bang.addView(nutKieu("A", "Cỡ chữ lớn", DinhDang.CHU_TO))
        bang.addView(nutChu("🎨", "Màu chữ") { hienMauChu() })
        bang.addView(nutChu("—", "Đường kẻ") { chenDuongKe() })
        bangDinhDang = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(bang)
            visibility = View.GONE
        }
        cot.addView(bangDinhDang)
        return cot
    }

    /** Nut toolbar 36dp, chia deu be ngang (width 0 weight 1), tint #CC000000 / #CCFFFFFF. */
    private fun nutToolbar(anh: Int, ten: String, bam: () -> Unit): View = ImageView(this).apply {
        setImageResource(anh)
        setColorFilter(mau.chuToolbar)
        contentDescription = ten
        scaleType = ImageView.ScaleType.FIT_CENTER
        isClickable = true
        layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f)
        setOnClickListener { bam() }
        nhanMoDan(this)
    }

    /** Nhan: chi doi alpha 1.0 -> 0.6 (miuix_theme_icon_pressed_alpha), khong ripple. */
    private fun nhanMoDan(v: View, thuong: Float = 1f, nhan: Float = 0.6f) {
        v.setOnTouchListener { _, su ->
            when (su.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.animate().alpha(nhan).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().alpha(thuong).setDuration(200).start()
            }
            false
        }
    }

    // ------------------------------------------------------------ dinh dang

    /**
     * Doan dang duoc ap dinh dang.
     *
     * Chua boi den gi thi lay TU dang dat con tro. Khong lam vay thi bam "B" luc
     * con tro dung giua cau khong xay ra gi ca, ma nguoi dung khong hieu tai sao
     * nut khong an.
     */
    private fun vungAp(): Pair<Int, Int> {
        val chu = oThan.text
        var dau = oThan.selectionStart.coerceIn(0, chu.length)
        var cuoi = oThan.selectionEnd.coerceIn(0, chu.length)
        if (dau > cuoi) dau = cuoi.also { cuoi = dau }
        if (dau != cuoi) return dau to cuoi

        while (dau > 0 && !chu[dau - 1].isWhitespace()) dau--
        while (cuoi < chu.length && !chu[cuoi].isWhitespace()) cuoi++
        return dau to cuoi
    }

    private fun apKieu(co: Int) {
        val (dau, cuoi) = vungAp()
        if (dau >= cuoi) {
            Toast.makeText(this, "Bôi đen phần chữ muốn đổi", Toast.LENGTH_SHORT).show()
            return
        }
        SoanThao.doiKieu(oThan.text as Spannable, dau, cuoi, co)
    }

    private fun hienMauChu() {
        val ten = arrayOf("Mặc định", "Đỏ", "Cam", "Xanh lá", "Xanh dương", "Tím")
        val gt = arrayOf<Int?>(
            null, 0xFFE05252.toInt(), 0xFFE8A33D.toInt(),
            0xFF34A853.toInt(), 0xFF4A90D9.toInt(), 0xFF9B6DD6.toInt(),
        )
        HopThoai(this, mau)
            .setTitle("Màu chữ")
            .setItems(ten) { _, chon ->
                val (dau, cuoi) = vungAp()
                SoanThao.datMauChu(oThan.text as Spannable, dau, cuoi, gt[chon])
            }
            .show()
    }

    /**
     * Bat/tat o tick tren dong dang dat con tro.
     *
     * O tick la KY TU dau dong chu khong phai mot trang thai rieng — nho vay xoa
     * lui qua no la no bien mat nhu moi ky tu khac.
     */
    private fun doiOTick() {
        val chu = oThan.text
        val viTri = oThan.selectionStart.coerceIn(0, chu.length)
        val dau = chu.toString().lastIndexOf('\n', (viTri - 1).coerceAtLeast(0))
            .let { if (it < 0) 0 else it + 1 }
        val cuoi = chu.toString().indexOf('\n', viTri).let { if (it < 0) chu.length else it }
        val dong = chu.subSequence(dau, cuoi).toString()

        val moi = when {
            dong.startsWith(SoanThao.O_TRONG) ->
                SoanThao.O_TICK + dong.removePrefix(SoanThao.O_TRONG)
            dong.startsWith(SoanThao.O_TICK) -> dong.removePrefix(SoanThao.O_TICK)
            else -> SoanThao.O_TRONG + dong
        }
        // Chi thay phan DAU dong chu khong ca dong: thay ca dong la moi span dinh
        // dang trong do bi xoa sach theo.
        thayDauDong(dau, dong, moi)
        oThan.setSelection((viTri + moi.length - dong.length).coerceIn(0, oThan.text.length))
        trangTriOTick()
    }

    private fun thayDauDong(dau: Int, cu: String, moi: String) {
        val chu = oThan.text
        when {
            moi.length > cu.length -> chu.insert(dau, moi.substring(0, moi.length - cu.length))
            moi.length < cu.length -> chu.delete(dau, dau + (cu.length - moi.length))
            else -> chu.replace(dau, dau + 2, moi.substring(0, 2))
        }
    }

    /**
     * Cham vao o soan thao — cham dung o tick thi bat/tat no.
     *
     * Tra `false` cho moi truong hop khac de `EditText` xu ly nhu binh thuong:
     * nuot su kien la mat luon dat con tro, boi den, cuon.
     */
    private fun chamVaoThan(su: MotionEvent): Boolean {
        if (su.actionMasked != MotionEvent.ACTION_UP) return false
        val bo = oThan.layout ?: return false

        val x = su.x - oThan.totalPaddingLeft + oThan.scrollX
        val y = su.y - oThan.totalPaddingTop + oThan.scrollY
        val hang = bo.getLineForVertical(y.toInt())
        val dauDong = bo.getLineStart(hang)
        val chu = oThan.text

        // Dong ban do tu duy: cham vao la mo man sua.
        val cuoiDongBanDo = bo.getLineEnd(hang)
        (chu as? Spanned)?.getSpans(dauDong, cuoiDongBanDo, SoanThao.TheBanDo::class.java)
            ?.firstOrNull()?.let {
                moBanDo(it)
                return true
            }

        // Dong ghi am: cham vao dau cung la phat/dung, khong phai dat con tro.
        val cuoiDong = bo.getLineEnd(hang)
        (chu as? Spanned)?.getSpans(dauDong, cuoiDong, SoanThao.TheGhiAm::class.java)
            ?.firstOrNull()?.let {
                batTatPhat(it)
                return true
            }

        if (dauDong + 2 > chu.length) return false
        val dau = chu.subSequence(dauDong, dauDong + 2).toString()
        if (dau != SoanThao.O_TRONG && dau != SoanThao.O_TICK) return false

        // Chi tinh la "cham vao o tick" khi cham dung ben trai o vuong; cham vao
        // chu phia sau van la dat con tro nhu thuong.
        if (x > bo.getPrimaryHorizontal(dauDong + 2)) return false

        oThan.setSelection(dauDong)
        doiOTick()
        return true
    }

    private fun chenDuongKe() {
        val chu = oThan.text
        val vt = oThan.selectionEnd.coerceIn(0, chu.length)
        val truoc = if (vt > 0 && chu[vt - 1] != '\n') "\n" else ""
        chu.insert(vt, truoc + SoanThao.DUONG_KE + "\n")
    }

    // ------------------------------------------------------------ anh

    private fun chonAnh() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            },
            MA_CHON_ANH,
        )
    }

    override fun onActivityResult(ma: Int, ketQua: Int, du: Intent?) {
        super.onActivityResult(ma, ketQua, du)
        if (ketQua != RESULT_OK) return
        if (ma == MA_BAN_DO) {
            capNhatBanDo(
                du?.getStringExtra(BanDoActivity.KHOA_JSON).orEmpty(),
                du?.getIntExtra(BanDoActivity.KHOA_SO_NUT, 1) ?: 1,
            )
            return
        }
        if (ma != MA_CHON_ANH) return
        val nguon = du?.data ?: return

        val ten = KhoAnh.them(this, nguon)
        if (ten == null) {
            Toast.makeText(this, "Không đọc được ảnh", Toast.LENGTH_SHORT).show()
            return
        }
        chenAnh(ten)
    }

    private fun chenAnh(ten: String) {
        val chu = oThan.text
        val vt = oThan.selectionEnd.coerceIn(0, chu.length)
        val truoc = if (vt > 0 && chu[vt - 1] != '\n') "\n" else ""

        val them = SpannableStringBuilder(truoc + DinhDang.THAY_ANH + "\n")
        val dau = truoc.length
        them.setSpan(
            SoanThao.TheAnh(ten), dau, dau + DinhDang.THAY_ANH.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        chu.insert(vt, them)
        veAnh()
    }

    /**
     * Ve anh that len nhung cho da danh dau [SoanThao.TheAnh].
     *
     * Tach khoi [SoanThao] vi ve anh can `Context` va do rong that cua o soan
     * thao; ben do co y giu thuan Kotlin de kiem thu tren JVM duoc.
     */
    private fun veAnh() {
        val chu = oThan.text as? Spannable ?: return
        val rong = (oThan.width - oThan.paddingLeft - oThan.paddingRight).takeIf { it > 0 } ?: return

        for (the in chu.getSpans(0, chu.length, SoanThao.TheAnh::class.java)) {
            val dau = chu.getSpanStart(the)
            val cuoi = chu.getSpanEnd(the)
            if (dau < 0 || cuoi <= dau) continue
            // Da ve roi thi thoi, khong ve chong len lan nua moi lan quay lai man hinh.
            if (chu.getSpans(dau, cuoi, ImageSpan::class.java).isNotEmpty()) continue

            chu.setSpan(
                ImageSpan(KhoAnh.anhVe(this, the.ten, rong, mau), ImageSpan.ALIGN_BASELINE),
                dau, cuoi, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }

    // ------------------------------------------------------------ ghi am

    private var phatGhiAm: android.media.MediaPlayer? = null

    private fun ghiAm() {
        val quyen = android.Manifest.permission.RECORD_AUDIO
        if (checkSelfPermission(quyen) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(quyen), MA_QUYEN_GHI_AM)
            return
        }

        val phien = KhoGhiAm.Phien(this, KhoGhiAm.tenMoi())
        if (!phien.bat()) {
            Toast.makeText(this, "Không mở được micro", Toast.LENGTH_SHORT).show()
            return
        }

        val dongHo = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            setTextColor(mau.chuChinh)
            setPadding(0, dp(24), 0, dp(24))
            text = KhoGhiAm.doDai(0)
        }

        val hop = HopThoai(this, mau)
            .setTitle("Đang ghi âm")
            .setView(dongHo)
            .setCancelable(false)
            .setNegativeButton("Huỷ") { _, _ -> phien.bo() }
            .setPositiveButton("Xong") { _, _ -> xongGhiAm(phien) }
            .create()

        // Dong ho tu dem. Dung `Handler` cua luong chinh chu khong luong rieng:
        // no cham vao view, ma view chi duoc dong tu luong chinh.
        val chinh = android.os.Handler(mainLooper)
        val dem = object : Runnable {
            override fun run() {
                val giay = phien.daGhi()
                dongHo.text = KhoGhiAm.doDai(giay)
                // Cham tran thi tu dung, khong doi nguoi dung — xem KhoGhiAm.TOI_DA_GIAY.
                if (giay >= KhoGhiAm.TOI_DA_GIAY) {
                    hop.dismiss()
                    xongGhiAm(phien)
                } else {
                    chinh.postDelayed(this, 500)
                }
            }
        }
        chinh.postDelayed(dem, 500)

        // KHONG dat setOnShowListener o day: HopThoai da dung no cho hoat anh truot len.
        hop.setOnDismissListener { chinh.removeCallbacks(dem) }
        hop.show()
    }

    private fun xongGhiAm(phien: KhoGhiAm.Phien) {
        val giay = phien.dung()
        if (giay <= 0) {
            Toast.makeText(this, "Bản ghi quá ngắn", Toast.LENGTH_SHORT).show()
            return
        }
        chenGhiAm(phien.ten, giay)
    }

    private fun chenGhiAm(ten: String, giay: Int) {
        val chu = oThan.text
        val vt = oThan.selectionEnd.coerceIn(0, chu.length)
        val truoc = if (vt > 0 && chu[vt - 1] != '\n') "\n" else ""

        val nhan = SoanThao.nhanGhiAm(giay)
        val them = SpannableStringBuilder(truoc + nhan + "\n")
        them.setSpan(
            SoanThao.TheGhiAm(ten, giay), truoc.length, truoc.length + nhan.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        chu.insert(vt, them)
    }

    /** Bam vao mot dong ghi am: dang im thi phat, dang phat thi dung. */
    private fun batTatPhat(the: SoanThao.TheGhiAm) {
        if (phatGhiAm != null) {
            dungPhat()
            return
        }
        val may = KhoGhiAm.phat(this, the.ten) { dungPhat() }
        if (may == null) {
            Toast.makeText(this, "Bản ghi không có trên máy này", Toast.LENGTH_SHORT).show()
            return
        }
        phatGhiAm = may
    }

    private fun dungPhat() {
        try {
            phatGhiAm?.stop()
            phatGhiAm?.release()
        } catch (_: Exception) {
            // Da giai phong roi thi thoi.
        }
        phatGhiAm = null
    }

    override fun onDestroy() {
        dungPhat()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(ma: Int, quyen: Array<out String>, ketQua: IntArray) {
        super.onRequestPermissionsResult(ma, quyen, ketQua)
        if (ma != MA_QUYEN_GHI_AM) return
        if (ketQua.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED) ghiAm()
        else Toast.makeText(this, "Cần quyền micro để ghi âm", Toast.LENGTH_SHORT).show()
    }

    // ------------------------------------------------------------ ban do tu duy

    /**
     * Doan chu cua ban do dang mo, de con thay the khi tro ve.
     *
     * Giu VI TRI chu khong giu doi tuong span: nguoi dung co the go them chu o
     * tren trong luc man ban do dang mo — khong the, nhung `EditText` khong bao
     * dam gi, va vi tri thi tra lai bang `getSpanStart` luc quay ve moi la dung.
     */
    private var theBanDoDangMo: SoanThao.TheBanDo? = null

    private fun chenBanDo() {
        val chu = oThan.text
        val vt = oThan.selectionEnd.coerceIn(0, chu.length)
        val truoc = if (vt > 0 && chu[vt - 1] != '\n') "\n" else ""

        val cay = Nut(tieuDe().ifBlank { "Bản đồ tư duy" })
        val json = XepBanDo.ghiJson(cay)
        val nhan = SoanThao.nhanBanDo(1)

        val them = SpannableStringBuilder(truoc + nhan + "\n")
        val the = SoanThao.TheBanDo(json)
        them.setSpan(
            the, truoc.length, truoc.length + nhan.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        chu.insert(vt, them)
        moBanDo(the)
    }

    private fun moBanDo(the: SoanThao.TheBanDo) {
        theBanDoDangMo = the
        startActivityForResult(
            Intent(this, BanDoActivity::class.java).putExtra(BanDoActivity.KHOA_JSON, the.json),
            MA_BAN_DO,
        )
    }

    /**
     * Thay doan chu cua ban do vua sua bang nhan moi va span moi.
     *
     * Phai thay CA doan chu chu khong chi doi span: so nhanh doi thi nhan cung
     * doi do dai, giu nguyen chu cu la nguoi dung thay "3 nhánh" trong khi ban
     * do co bay.
     */
    private fun capNhatBanDo(json: String, soNut: Int) {
        val cu = theBanDoDangMo ?: return
        theBanDoDangMo = null

        val chu = oThan.text
        val dau = chu.getSpanStart(cu)
        val cuoi = chu.getSpanEnd(cu)
        if (dau < 0 || cuoi < dau) return

        val nhan = SoanThao.nhanBanDo(soNut)
        chu.removeSpan(cu)
        chu.replace(dau, cuoi, nhan)
        chu.setSpan(
            SoanThao.TheBanDo(json), dau, dau + nhan.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }

    // ------------------------------------------------------------ hanh dong

    /**
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

    private fun hienChonMauGiay() {
        HopThoai(this, mau)
            .setTitle("Màu giấy")
            .setItems(MauGhiChu.TEN) { _, chon ->
                mauNen = chon
                toMau()
            }
            .show()
    }

    private fun hienChonThuMuc() {
        val ds = kho.thuMucKemSo().map { it.first }
        val ten = (listOf("Không thuộc thư mục nào") + ds).toTypedArray()
        HopThoai(this, mau)
            .setTitle("Thư mục")
            .setItems(ten) { _, chon ->
                thuMuc = if (chon == 0) "" else ds[chon - 1]
                oChan.text = nhanChan(goc)
                capNhatChip()
            }
            .show()
    }

    /**
     * Chon ngay roi chon gio de nhac.
     *
     * Hai hop noi tiep nhau chu khong mot hop gop: khung co san
     * (`DatePickerDialog`, `TimePickerDialog`) da theo dung thoi quen cua may va
     * cua ngon ngu he thong — tu ve mot hop gop la mat het cai do de doi lay ve
     * ngoai dep hon mot chut.
     */
    private fun hienDatNhac() {
        val moc = java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            // Mac dinh la gio tron ke tiep — khong ai dat nhac vao 14:37.
            add(java.util.Calendar.HOUR_OF_DAY, 1)
            set(java.util.Calendar.MINUTE, 0)
        }

        android.app.DatePickerDialog(
            this, mau.themeHopThoai,
            { _, nam, thang, ngay ->
                android.app.TimePickerDialog(
                    this, mau.themeHopThoai,
                    { _, gio, phut ->
                        moc.set(nam, thang, ngay, gio, phut)
                        moc.set(java.util.Calendar.SECOND, 0)
                        moc.set(java.util.Calendar.MILLISECOND, 0)
                        if (moc.timeInMillis <= System.currentTimeMillis()) {
                            Toast.makeText(this, "Mốc đó đã qua rồi", Toast.LENGTH_SHORT).show()
                        } else {
                            datNhac(moc.timeInMillis)
                        }
                    },
                    moc.get(java.util.Calendar.HOUR_OF_DAY), moc.get(java.util.Calendar.MINUTE),
                    android.text.format.DateFormat.is24HourFormat(this),
                ).show()
            },
            moc.get(java.util.Calendar.YEAR),
            moc.get(java.util.Calendar.MONTH),
            moc.get(java.util.Calendar.DAY_OF_MONTH),
        ).show()
    }

    /**
     * Ghi moc nhac XUONG NGAY, khong doi `onPause`.
     *
     * Bao thuc chi duoc hen lai khi CSDL doi. De den `onPause` thi nguoi dung dat
     * gio xong, bam back, va van phai may man moi kip — con neu ho bam "Chuyen
     * vao thung rac" ngay sau do thi moc vua dat mat luon.
     */
    private fun datNhac(luc: Long?) {
        nhacLuc = luc
        if (id == 0L) luu()
        if (id != 0L) {
            kho.datNhac(id, luc)
            goc = kho.theoId(id)
        }
        oChan.text = nhanChan(goc)
        Toast.makeText(
            this,
            if (luc == null) "Đã bỏ nhắc nhở" else "Sẽ nhắc lúc " + NgayVN.nhanDanhSach(luc),
            Toast.LENGTH_SHORT,
        ).show()
    }

    /**
     * Chuyen ghi chu vao/ra muc rieng tu.
     *
     * Ghi XUONG NGAY chu khong doi `onPause`, va di qua [luu] de noi dung duoc
     * ma hoa (hoac giai ma) dung mot lan. Chua dat mat khau thi phai dat truoc —
     * khong co khoa thi khong ma hoa duoc gi.
     */
    private fun doiRiengTu() {
        val muon = goc?.riengTu != true
        if (muon && !KhoaRiengTu.dangMo) {
            Toast.makeText(
                this,
                if (KhoaRiengTu.daDatMatKhau) "Mở khoá mục Riêng tư ở màn hình chính trước"
                else "Vào Thư mục > Riêng tư để đặt mật khẩu trước",
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        // Phai luu phan dang go TRUOC khi doi co: [luu] doc co tu `goc`, doi co
        // ma chua luu thi phan vua go bi ghi bang co cu.
        luu()
        val g = if (id != 0L) kho.theoId(id) else null
        if (g == null) {
            Toast.makeText(this, "Ghi chú trống", Toast.LENGTH_SHORT).show()
            return
        }
        goc = g
        id = kho.luu(
            GhiChu(
                id = g.id, uid = g.uid, tieuDe = g.tieuDe, noiDung = g.noiDung,
                thuMuc = g.thuMuc, ghim = g.ghim, mau = g.mau,
                riengTu = muon, nhacLuc = g.nhacLuc, ngayTao = g.ngayTao,
            ),
        )
        goc = kho.theoId(id)
        Toast.makeText(
            this,
            if (muon) "Đã chuyển vào Riêng tư" else "Đã bỏ khỏi Riêng tư",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun hienXuat() {
        if (oTieuDe.text.isBlank() && oThan.text.isBlank()) {
            Toast.makeText(this, "Ghi chú trống", Toast.LENGTH_SHORT).show()
            return
        }
        HopThoai(this, mau)
            .setTitle("Chia sẻ / xuất")
            .setItems(arrayOf("Văn bản", "Ảnh PNG", "PDF")) { _, chon ->
                when (chon) {
                    0 -> chiaSeChu()
                    1 -> xuatTep("image/png") { XuatGhiChu.anh(this, tieuDe(), than(), mau) }
                    2 -> xuatTep("application/pdf") { XuatGhiChu.pdf(this, tieuDe(), than(), mau) }
                }
            }
            .show()
    }

    private fun tieuDe(): String = oTieuDe.text.toString().trim()

    /**
     * Noi dung de xuat, GIU nguyen dinh dang.
     *
     * Tra thang `oThan.text` chu khong `.toString()`: doi sang String la mat het
     * span — chu dam, mau chu, anh deu bien thanh chu tron.
     */
    private fun than(): CharSequence = oThan.text

    private fun chiaSeChu() {
        val chu = (tieuDe() + "\n" + than().toString()).trim()
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, chu)
        }, "Chia sẻ ghi chú"))
    }

    /**
     * Dung tep roi mo bang chon chia se.
     *
     * Dung tep o LUONG NEN: ve mot ghi chu dai ra PDF nhieu trang mat vai tram
     * mili giay, du de giao dien khung mot cai ngay sau khi nguoi dung bam.
     */
    private fun xuatTep(kieu: String, dung: () -> java.io.File) {
        TepChiaSe.don(this)
        Thread {
            val f = try {
                dung()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Không xuất được: " + e.message, Toast.LENGTH_LONG).show()
                }
                return@Thread
            }
            val uri = TepChiaSe.uriCua(this, f)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = kieu
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, tieuDe())
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        "Chia sẻ ghi chú",
                    ),
                )
            }
        }.start()
    }

    // ------------------------------------------------------------ luu

    /**
     * Ghi xuong CSDL.
     *
     * Ghi chu rong thi khong tao hang moi — bam "+" roi doi y bam back la chuyen
     * thuong, khong nen de lai mot the trang trong danh sach. Nhung ghi chu DA CO
     * ma bi xoa het chu thi van luu: do la y dinh cua nguoi dung.
     */
    private fun luu() {
        val tieuDe = oTieuDe.text.toString().trim()
        val than = oThan.text
        if (id == 0L && tieuDe.isBlank() && than.isBlank()) return

        val cu = goc
        val moi = GhiChu(
            id = id,
            uid = cu?.uid.orEmpty(),
            tieuDe = tieuDe,
            noiDung = SoanThao.ghi(than),
            thuMuc = thuMuc,
            ghim = ghim,
            mau = mauNen,
            riengTu = cu?.riengTu ?: false,
            nhacLuc = nhacLuc,
            ngayTao = cu?.ngayTao ?: 0L,
        )
        // Khong doi gi thi khong ghi: moi lan ghi la mot lan `ngay_sua` nhay len
        // va ghi chu bi danh dau phai day len may chu lai — chi mo ra xem roi
        // thoat cung sinh mot luot dong bo vo nghia.
        if (cu != null && !daDoi(cu, moi)) return

        id = kho.luu(moi)
        goc = kho.theoId(id)
    }

    private fun daDoi(cu: GhiChu, moi: GhiChu): Boolean =
        cu.tieuDe != moi.tieuDe || cu.noiDung != moi.noiDung || cu.thuMuc != moi.thuMuc ||
            cu.ghim != moi.ghim || cu.mau != moi.mau || cu.nhacLuc != moi.nhacLuc

    // ------------------------------------------------------------ tien ich

    /**
     * Nen man soan: theme mac dinh cua ban goc la v8_theme_bg_color = #FFFFFF /
     * #000000 (KHONG phai mau the #242424); co chon mau giay thi theo mau do.
     */
    private fun toMau() {
        val nenGiay = if (mauNen == 0) (if (mau.toi) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            else MauGhiChu.nen(mauNen, mau)
        nen.setBackgroundColor(nenGiay)
    }

    /**
     * "19 tháng 8  21:47" — ngay (FORMAT_SHOW_DATE) + HAI dau cach + gio, dung
     * NoteInfoController.updateDataTime cua ban goc. Nhac nho (neu co) noi sau
     * bang vach dung.
     */
    private fun nhanChan(g: GhiChu?): String {
        val luc = g?.ngaySua ?: System.currentTimeMillis()
        val t = java.time.Instant.ofEpochMilli(luc).atZone(java.time.ZoneId.systemDefault())
        val chu = StringBuilder()
        chu.append(t.dayOfMonth).append(" tháng ").append(t.monthValue)
        if (t.year != java.time.LocalDate.now().year) chu.append(", ").append(t.year)
        chu.append("  ").append(NgayVN.gioPhut(luc))
        nhacLuc?.let { chu.append("  ·  Nhắc ").append(NgayVN.nhanDanhSach(it)) }
        return chu.toString()
    }

    /** Nut icon thanh tren: 40dp (v12_notes_edit_header_ic_size), icon 24dp, khong ripple. */
    private fun nutAnh(anh: Int, ten: String, bam: () -> Unit): ImageButton =
        ImageButton(this).apply {
            setImageResource(anh)
            setColorFilter(if (mau.toi) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            contentDescription = ten
            background = null
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            setOnClickListener { bam() }
            nhanMoDan(this)
        }

    /** Nut ky tu 40dp cho nhom phai thanh tren (chia se / mau / them / xong). */
    private fun nutChuNho(ky: String, ten: String, bam: () -> Unit): TextView =
        TextView(this).apply {
            text = ky
            contentDescription = ten
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(if (mau.toi) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            isClickable = true
            setOnClickListener { bam() }
            nhanMoDan(this)
        }

    /** Nut dinh dang, chinh no hien dung kieu ma no ap — `B` in dam, `I` nghieng. */
    private fun nutKieu(
        ky: String,
        ten: String,
        co: Int,
        dam: Boolean = false,
        nghieng: Boolean = false,
    ): View = nutChu(ky, ten) { apKieu(co) }.apply {
        val t = this as TextView
        t.setTypeface(
            t.typeface,
            when {
                dam && nghieng -> android.graphics.Typeface.BOLD_ITALIC
                dam -> android.graphics.Typeface.BOLD
                nghieng -> android.graphics.Typeface.ITALIC
                else -> android.graphics.Typeface.NORMAL
            },
        )
    }

    private fun nutChu(ky: String, ten: String, bam: () -> Unit): View =
        TextView(this).apply {
            text = ky
            contentDescription = ten
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTextColor(mau.chuChinh)
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(BangMau.mauMo(mau.chuChinh, 0.06f))
            }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { rightMargin = dp(8) }
            isClickable = true
            setOnClickListener { bam() }
        }

    companion object {
        const val KHOA_ID = "id"
        const val KHOA_THU_MUC = "thu_muc"
        private const val MA_CHON_ANH = 1
        private const val MA_QUYEN_GHI_AM = 2
        private const val MA_BAN_DO = 3
    }
}
