# -*- coding: utf-8 -*-
# Mo rong HopThoai (giuMo, setSingleChoiceItems) va chuyen SuaSuKien sang HopThoai.
import io, os, sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
GOC = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'kotlin', 'com', 'ghichu')

def va(ten, cap):
    p = os.path.join(GOC, ten)
    s = io.open(p, encoding='utf-8').read()
    for a, b in cap:
        if a not in s:
            raise SystemExit('THIEU NEO ' + ten + ': ' + a[:60])
        s = s.replace(a, b, 1)
    io.open(p, 'w', encoding='utf-8').write(s)
    print('va', ten)

va('HopThoai.kt', [
('''    private var nguyHiem = false
    private var huyDuoc = true''',
'''    private var nguyHiem = false
    private var huyDuoc = true
    /** Nut chinh / giua KHONG tu dong hop — ben goi tu `dismiss()` khi du lieu hop le. */
    private var giuMo = false
    private var chonMot: Array<out CharSequence>? = null
    private var dangChon = -1
    private var khiChonMot: DialogInterface.OnClickListener? = null'''),
('''    /** Nut chinh la hanh dong pha huy (xoa han) — chu do tren nen thuong, khong xanh. */
    fun nguyHiem(): HopThoai = apply { nguyHiem = true }''',
'''    /** Nut chinh la hanh dong pha huy (xoa han) — chu do tren nen thuong, khong xanh. */
    fun nguyHiem(): HopThoai = apply { nguyHiem = true }
    /** Bam nut chinh/giua khong tu dong hop (de kiem du lieu truoc); nut phu van dong. */
    fun giuMo(): HopThoai = apply { giuMo = true }
    /**
     * Danh sach chon MOT (Widget.AlertDialogListItem.SingleChoice): muc 56dp, dem
     * ngang 28dp, chu 17sp medium; muc dang chon chu XANH + dau tick 28dp xanh o cuoi.
     */
    fun setSingleChoiceItems(ds: Array<out CharSequence>, chon: Int, lam: DialogInterface.OnClickListener?): HopThoai =
        apply { chonMot = ds; dangChon = chon; khiChonMot = lam }'''),
('''        muc?.let { ds ->
            val cuon = ScrollView(ctx)''',
'''        chonMot?.let { ds ->
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
                    text = if (la) "\\u2713" else ""
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
            val cuon = ScrollView(ctx)'''),
('''        val coPhu = chuPhu != null || (muc != null && chuChinh == null)''',
'''        val coPhu = chuPhu != null || ((muc != null || chonMot != null) && chuChinh == null)'''),
('''            val nutGiua = chuGiua?.let { c -> nut(c, kieu = 0) {
                hop.dismiss()
                khiGiua?.onClick(hop, DialogInterface.BUTTON_NEUTRAL)
            } }
            val nutChinh = chuChinh?.let { c -> nut(c, kieu = if (nguyHiem) 2 else 1) {
                hop.dismiss()
                khiChinh?.onClick(hop, DialogInterface.BUTTON_POSITIVE)
            } }''',
'''            val nutGiua = chuGiua?.let { c -> nut(c, kieu = 0) {
                if (!giuMo) hop.dismiss()
                khiGiua?.onClick(hop, DialogInterface.BUTTON_NEUTRAL)
            } }
            val nutChinh = chuChinh?.let { c -> nut(c, kieu = if (nguyHiem) 2 else 1) {
                if (!giuMo) hop.dismiss()
                khiChinh?.onClick(hop, DialogInterface.BUTTON_POSITIVE)
            } }'''),
('''            setPadding(dp(24), if (muc != null) 0 else dp(8), dp(24), 0)''',
'''            setPadding(dp(24), if (muc != null || chonMot != null) 0 else dp(8), dp(24), 0)'''),
])

va('SuaSuKien.kt', [
('''        val hopThoai = AlertDialog.Builder(context, bangMau.themeHopThoai)
            .setTitle(if (goc == null) "Sự kiện mới" else "Sửa sự kiện")
            .setView(cuon)
            .setPositiveButton("Lưu", null)
            .setNegativeButton("Huỷ", null)
            .apply { if (goc != null) setNeutralButton("Xoá", null) }
            .create()

        hopThoai.show()

        // Gan sau khi show() de nut "Luu" khong tu dong dong khi du lieu chua hop le.
        hopThoai.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (luu()) hopThoai.dismiss()
        }
        if (goc != null) {
            hopThoai.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                AlertDialog.Builder(context, bangMau.themeHopThoai)
                    .setMessage("Xoá sự kiện này?")
                    .setPositiveButton("Xoá") { _, _ ->
                        db.xoa(goc.id)
                        hopThoai.dismiss()
                        khiXong()
                    }
                    .setNegativeButton("Huỷ", null)
                    .show()
            }
        }
    }''',
'''        // HopThoai.giuMo(): nut "Luu" / "Xoa" khong tu dong hop — chi dong khi du
        // lieu hop le hoac nguoi dung xac nhan xoa.
        HopThoai(context, bangMau)
            .setTitle(if (goc == null) "Sự kiện mới" else "Sửa sự kiện")
            .setView(cuon)
            .giuMo()
            .setNegativeButton("Huỷ", null)
            .apply {
                if (goc != null) {
                    setNeutralButton("Xoá") { hop, _ ->
                        HopThoai(context, bangMau)
                            .setMessage("Xoá sự kiện này?")
                            .nguyHiem()
                            .setNegativeButton("Huỷ", null)
                            .setPositiveButton("Xoá") { _, _ ->
                                db.xoa(goc.id)
                                hop.dismiss()
                                khiXong()
                            }
                            .show()
                    }
                }
            }
            .setPositiveButton("Lưu") { hop, _ -> if (luu()) hop.dismiss() }
            .show()
    }'''),
('''        AlertDialog.Builder(context, bangMau.themeHopThoai)
            .setTitle(tieuDe)
            .setSingleChoiceItems(muc, dangChon) { d, i ->''',
'''        HopThoai(context, bangMau)
            .setTitle(tieuDe)
            .setSingleChoiceItems(muc, dangChon) { d, i ->'''),
])
print('XONG')
