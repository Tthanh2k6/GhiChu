package com.ghichu

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Hop thoai kieu miuix — dung theo so do cua app goc (docs/SPEC-GOC-hopthoai.md).
 *
 * `AlertDialog` cua khung la hop vuong noi giua man hinh voi hai nut chu phang —
 * khac han hop cua MIUI: mot tam panel bo goc 36dp NEO O DAY man hinh, nut to bo
 * goc 16dp cao 50dp, nut chinh XANH DUONG. Bam "Xoa" mot lan la lo ngay.
 *
 * Chu ky ham CO Y giong `AlertDialog.Builder` (setTitle / setMessage / setItems /
 * setView / setNegativeButton / setPositiveButton / show) de thay hang loat
 * `AlertDialog.Builder(this, mau.themeHopThoai)` bang `HopThoai(this, mau)` ma
 * khong phai sua tung cho goi.
 *
 * So do (ten goc):
 *   * panel: le ngang 12dp (miuix_appcompat_dialog_width_margin), toi da 368dp
 *     (…_max_width), le duoi 12dp + thanh dieu huong, bo goc 36dp
 *     (miuix_theme_radius_big), nen #FFFFFF / #242424, dem doc 24dp;
 *   * tieu de 18sp medium can giua, #000000 / #F2FFFFFF, dem ngang 36dp, duoi 16dp;
 *   * noi dung 16sp, #CC000000 / #CCFFFFFF, le ngang 28dp, duoi 16dp;
 *   * nut cao 50dp, bo goc 16dp, chu 17sp medium; thuong nen #0F000000 / #24FFFFFF
 *     chu #E6000000 / #CCFFFFFF; chinh nen #3482FF / #4788FF chu trang; nguy hiem
 *     nen thuong chu #FA382E / #FA4238; hai nut xep ngang cach 12dp, phu trai;
 *   * danh sach (ActionSheet): muc 56dp can giua 17sp medium, dem ngang 24dp, nut
 *     Huy rieng ben duoi cach 16dp;
 *   * o nhap: cao 54dp, bo goc 16dp, dem ngang 16dp, chu 17sp medium, vien 2dp
 *     xanh khi co con tro;
 *   * lop mo 0.3 (sang) / 0.6 (toi); truot len tu day 300ms decelerate.
 */
class HopThoai(private val ctx: Context, private val mau: BangMau) {

    private val md = ctx.resources.displayMetrics
    private fun dp(v: Int) = (v * md.density).toInt()
    private fun dpF(v: Float) = v * md.density

    private var tieuDe: CharSequence? = null
    private var noiDung: CharSequence? = null
    private var muc: Array<out CharSequence>? = null
    private var khiChonMuc: DialogInterface.OnClickListener? = null
    private var viewRieng: View? = null
    private var chuPhu: CharSequence? = null
    private var khiPhu: DialogInterface.OnClickListener? = null
    private var chuChinh: CharSequence? = null
    private var khiChinh: DialogInterface.OnClickListener? = null
    private var chuGiua: CharSequence? = null
    private var khiGiua: DialogInterface.OnClickListener? = null
    private var nguyHiem = false
    private var huyDuoc = true
    /** Nut chinh / giua KHONG tu dong hop — ben goi tu `dismiss()` khi du lieu hop le. */
    private var giuMo = false
    private var chonMot: Array<out CharSequence>? = null
    private var dangChon = -1
    private var khiChonMot: DialogInterface.OnClickListener? = null

    private val xanh = if (mau.toi) 0xFF4788FF.toInt() else 0xFF3482FF.toInt()
    private val nenPanel = if (mau.toi) 0xFF242424.toInt() else 0xFFFFFFFF.toInt()
    private val chuTieuDe = if (mau.toi) 0xF2FFFFFF.toInt() else 0xFF000000.toInt()
    private val chuNoiDung = if (mau.toi) 0xCCFFFFFF.toInt() else 0xCC000000.toInt()
    private val nenNutThuong = if (mau.toi) 0x24FFFFFF else 0x0F000000
    private val chuNutThuong = if (mau.toi) 0xCCFFFFFF.toInt() else 0xE6000000.toInt()
    private val chuNguyHiem = if (mau.toi) 0xFFFA4238.toInt() else 0xFFFA382E.toInt()

    fun setTitle(s: CharSequence?): HopThoai = apply { tieuDe = s }
    fun setMessage(s: CharSequence?): HopThoai = apply { noiDung = s }
    fun setItems(ds: Array<out CharSequence>, lam: DialogInterface.OnClickListener?): HopThoai =
        apply { muc = ds; khiChonMuc = lam }
    fun setView(v: View): HopThoai = apply { viewRieng = v }
    fun setNegativeButton(s: CharSequence, lam: DialogInterface.OnClickListener?): HopThoai =
        apply { chuPhu = s; khiPhu = lam }
    fun setPositiveButton(s: CharSequence, lam: DialogInterface.OnClickListener?): HopThoai =
        apply { chuChinh = s; khiChinh = lam }
    fun setNeutralButton(s: CharSequence, lam: DialogInterface.OnClickListener?): HopThoai =
        apply { chuGiua = s; khiGiua = lam }
    /** Nut chinh la hanh dong pha huy (xoa han) — chu do tren nen thuong, khong xanh. */
    fun nguyHiem(): HopThoai = apply { nguyHiem = true }
    /** Bam nut chinh/giua khong tu dong hop (de kiem du lieu truoc); nut phu van dong. */
    fun giuMo(): HopThoai = apply { giuMo = true }
    /**
     * Danh sach chon MOT (Widget.AlertDialogListItem.SingleChoice): muc 56dp, dem
     * ngang 28dp, chu 17sp medium; muc dang chon chu XANH + dau tick 28dp xanh o cuoi.
     */
    fun setSingleChoiceItems(ds: Array<out CharSequence>, chon: Int, lam: DialogInterface.OnClickListener?): HopThoai =
        apply { chonMot = ds; dangChon = chon; khiChonMot = lam }
    fun setCancelable(co: Boolean): HopThoai = apply { huyDuoc = co }

    fun show(): Dialog = create().also { it.show() }

    fun create(): Dialog {
        val hop = Dialog(ctx)
        hop.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        hop.setCancelable(huyDuoc)
        hop.setCanceledOnTouchOutside(huyDuoc)

        val panel = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(24), 0, dp(24))
            background = GradientDrawable().apply {
                cornerRadius = dpF(36f)
                setColor(nenPanel)
            }
            clipToOutline = true
        }

        tieuDe?.let {
            panel.addView(TextView(ctx).apply {
                text = it
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(chuTieuDe)
                gravity = Gravity.CENTER
                maxLines = 4
                setPadding(dp(36), 0, dp(36), dp(16))
            })
        }

        noiDung?.let {
            panel.addView(TextView(ctx).apply {
                text = it
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setLineSpacing(0f, 1.05f)
                setTextColor(chuNoiDung)
                // TextAlignLayout cua ban goc: mot dong thi can giua, nhieu dong can trai.
                gravity = if (it.length <= 40) Gravity.CENTER else Gravity.START
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { leftMargin = dp(28); rightMargin = dp(28); bottomMargin = dp(16) })
        }

        viewRieng?.let { v ->
            if (v is EditText) trangTriONhap(v)
            (v.parent as? ViewGroup)?.removeView(v)
            panel.addView(v, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { leftMargin = dp(24); rightMargin = dp(24); bottomMargin = dp(16) })
        }

        chonMot?.let { ds ->
            val cuon = ScrollView(ctx)
            val cot = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            for ((i, chu) in ds.withIndex()) {
                val la = i == dangChon
                val hang = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    minimumHeight = dp(56)
                    setPadding(dp(28), (16.5f * md.density).toInt(), dp(28), (16.5f * md.density).toInt())
                    isClickable = true
                    nhan(this)
                    setOnClickListener { khiChonMot?.onClick(hop, i) }
                }
                hang.addView(TextView(ctx).apply {
                    text = chu
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    setTextColor(if (la) xanh else chuTieuDe)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                hang.addView(TextView(ctx).apply {
                    text = if (la) "\u2713" else ""
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    setTextColor(xanh)
                    gravity = Gravity.CENTER
                }, LinearLayout.LayoutParams(dp(28), dp(28)))
                cot.addView(hang, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            }
            cuon.addView(cot)
            panel.addView(cuon, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
            ).apply { bottomMargin = dp(16) })
        }

        muc?.let { ds ->
            val cuon = ScrollView(ctx)
            val cot = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            for ((i, chu) in ds.withIndex()) {
                cot.addView(TextView(ctx).apply {
                    text = chu
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    setTextColor(chuTieuDe)
                    gravity = Gravity.CENTER
                    minHeight = dp(56)
                    setPadding(dp(24), (16.5f * md.density).toInt(), dp(24), (16.5f * md.density).toInt())
                    isClickable = true
                    nhan(this)
                    setOnClickListener {
                        hop.dismiss()
                        khiChonMuc?.onClick(hop, i)
                    }
                })
            }
            cuon.addView(cot)
            panel.addView(cuon, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
            ).apply { bottomMargin = dp(16) })
        }

        // Nhom nut: dem ngang 24dp, cach noi dung 8dp; hai nut xep ngang cach 12dp.
        // Danh sach (ActionSheet) thi chi co nut Huy rieng ben duoi.
        val coPhu = chuPhu != null || ((muc != null || chonMot != null) && chuChinh == null)
        val soNut = (if (coPhu) 1 else 0) + (if (chuChinh != null) 1 else 0) + (if (chuGiua != null) 1 else 0)
        if (soNut > 0) {
            // <= 2 nut: xep NGANG, phu trai chinh phai, cach 12dp.
            // >= 3 nut: xep DOC full be ngang, chinh tren cung (HyperOS > 2), cach 12dp.
            val doc = soNut >= 3
            val hang = LinearLayout(ctx).apply {
                orientation = if (doc) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                setPadding(dp(24), if (muc != null || chonMot != null) 0 else dp(8), dp(24), 0)
            }
            fun lp(dau: Boolean) = if (doc) {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)).apply { if (!dau) topMargin = dp(12) }
            } else {
                LinearLayout.LayoutParams(0, dp(50), 1f).apply { if (!dau) leftMargin = dp(12) }
            }
            val nutPhu = if (coPhu) nut(chuPhu ?: "Huỷ", kieu = 0) {
                hop.dismiss()
                khiPhu?.onClick(hop, DialogInterface.BUTTON_NEGATIVE)
            } else null
            val nutGiua = chuGiua?.let { c -> nut(c, kieu = 0) {
                if (!giuMo) hop.dismiss()
                khiGiua?.onClick(hop, DialogInterface.BUTTON_NEUTRAL)
            } }
            val nutChinh = chuChinh?.let { c -> nut(c, kieu = if (nguyHiem) 2 else 1) {
                if (!giuMo) hop.dismiss()
                khiChinh?.onClick(hop, DialogInterface.BUTTON_POSITIVE)
            } }
            val thuTu = if (doc) listOf(nutChinh, nutGiua, nutPhu) else listOf(nutPhu, nutGiua, nutChinh)
            var dau = true
            for (n in thuTu) {
                if (n == null) continue
                hang.addView(n, lp(dau))
                dau = false
            }
            panel.addView(hang)
        }

        // Boc trong khung de dat le va gioi han be rong 368dp.
        val rongMan = md.widthPixels
        val rongPanel = minOf(rongMan - dp(24), dp(368))
        val khung = FrameLayout(ctx).apply {
            addView(panel, FrameLayout.LayoutParams(rongPanel, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            })
        }
        hop.setContentView(khung)

        hop.window?.let { w ->
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setGravity(Gravity.BOTTOM)
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            w.attributes = w.attributes.apply {
                y = dp(12)
                dimAmount = if (mau.toi) 0.6f else 0.3f
                // Cao toi da 70% man hinh (miuix_appcompat_dialog_max_height_minor).
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            // Truot len tu day: dialog_anim_slide_in_bottom 300ms decelerate 1.5
            // (ban goc con them lo xo Folme; day la ban fallback cua chinh no).
            w.setWindowAnimations(0)
        }
        panel.maxHeightWrap(md.heightPixels * 7 / 10)
        hop.setOnShowListener {
            panel.translationY = panel.height.toFloat().takeIf { it > 0 } ?: dpF(300f)
            panel.animate().translationY(0f).setDuration(300)
                .setInterpolator(DecelerateInterpolator(1.5f)).start()
            (viewRieng as? EditText)?.let { o ->
                o.requestFocus()
                o.postDelayed({
                    ctx.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                        ?.showSoftInput(o, 0)
                }, 100)
            }
        }
        return hop
    }

    /** kieu: 0 thuong, 1 chinh (xanh), 2 nguy hiem (chu do tren nen thuong). */
    private fun nut(chu: CharSequence, kieu: Int, bam: () -> Unit): TextView = TextView(ctx).apply {
        text = chu
        gravity = Gravity.CENTER
        isSingleLine = true
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(
            when (kieu) {
                1 -> if (mau.toi) 0xE6FFFFFF.toInt() else 0xFFFFFFFF.toInt()
                2 -> chuNguyHiem
                else -> chuNutThuong
            },
        )
        background = GradientDrawable().apply {
            cornerRadius = dpF(16f)
            setColor(if (kieu == 1) xanh else nenNutThuong)
        }
        isClickable = true
        nhan(this)
        setOnClickListener { bam() }
    }

    /** O nhap trong hop: cao 54dp, bo goc 16dp, dem ngang 16dp, vien xanh 2dp khi co con tro. */
    private fun trangTriONhap(o: EditText) {
        o.minHeight = dp(54)
        o.setPadding(dp(16), 0, dp(16), 0)
        o.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        o.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        o.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        o.setTextColor(chuTieuDe)
        o.setHintTextColor(if (mau.toi) 0x4DFFFFFF else 0x4D000000)
        o.isSingleLine = true
        val nenO = GradientDrawable().apply {
            cornerRadius = dpF(16f)
            setColor(if (mau.toi) 0xFF4C4C4C.toInt() else 0xFFFFFFFF.toInt())
            setStroke(dp(2), Color.TRANSPARENT)
        }
        o.background = nenO
        o.setOnFocusChangeListener { _, co -> nenO.setStroke(dp(2), if (co) xanh else Color.TRANSPARENT) }
    }

    /** Nhan = phu mau den/trang alpha 0.12 (miuix_appcompat_default_btn_fg), khong ripple. */
    private fun nhan(v: View) {
        v.setOnTouchListener { _, su ->
            when (su.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.foreground =
                    ColorDrawable(if (mau.toi) 0x24FFFFFF else 0x1F000000)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.foreground = null
            }
            false
        }
    }

    private fun View.maxHeightWrap(toiDa: Int) {
        // LinearLayout khong co maxHeight; cat bang cach do lai sau layout dau.
        viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (height > toiDa) {
                    layoutParams = layoutParams.apply { height = toiDa }
                    requestLayout()
                }
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }
}
