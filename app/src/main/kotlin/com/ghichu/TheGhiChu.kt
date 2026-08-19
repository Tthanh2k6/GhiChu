package com.ghichu

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Mot the ghi chu trong luoi o man hinh chinh.
 *
 * Dung bang code chu khong bang XML: ca app khong keo AndroidX vao va mau sac
 * deu do [BangMau] quyet dinh luc chay (nguoi dung chon sang/toi trong app,
 * khong nhat thiet theo he thong) — viet XML thi van phai to lai mau tung o
 * trong code, thanh ra hai noi mot viec.
 */
class TheGhiChu(context: Context) : LinearLayout(context) {

    private val md = resources.displayMetrics
    private fun dp(v: Int) = (v * md.density).toInt()

    private val oTieuDe = TextView(context)
    private val oThan = TextView(context)
    private val oChan = TextView(context)

    /** Vien sang khi the dang duoc chon trong che do chon nhieu. */
    private var dangChon = false
    private var mauVien = 0

    init {
        orientation = VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12))

        oTieuDe.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        oTieuDe.setTypeface(oTieuDe.typeface, android.graphics.Typeface.BOLD)
        oTieuDe.maxLines = 2
        oTieuDe.ellipsize = android.text.TextUtils.TruncateAt.END
        addView(oTieuDe)

        oThan.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        oThan.maxLines = 5
        oThan.ellipsize = android.text.TextUtils.TruncateAt.END
        oThan.setLineSpacing(dp(2).toFloat(), 1f)
        addView(oThan, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(6)
        })

        oChan.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        oChan.gravity = Gravity.END
        addView(oChan, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(10)
        })
    }

    fun gan(g: GhiChu, mau: BangMau, chon: Boolean) {
        dangChon = chon
        mauVien = mau.nhan

        val tieuDe = g.tieuDeHien()
        val than = g.xemTruoc()

        oTieuDe.text = tieuDe.ifBlank { "Không có văn bản" }
        oTieuDe.setTextColor(if (tieuDe.isBlank()) mau.chuMo else mau.chuChinh)

        oThan.text = than
        oThan.setTextColor(mau.chuPhu)
        oThan.visibility = if (than.isBlank()) View.GONE else View.VISIBLE

        // Ghim va nhac nho deu la thong tin mot dong — gop vao chan the de khong
        // ton them mot hang bieu tuong chi de hien mot dau cham.
        val chan = StringBuilder()
        if (g.ghim) chan.append("Ghim · ")
        if (g.nhacLuc != null) chan.append("Nhắc · ")
        chan.append(NgayVN.nhanDanhSach(g.ngaySua))
        oChan.text = chan
        oChan.setTextColor(mau.chuMo)

        background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(MauGhiChu.nen(g.mau, mau))
            if (chon) setStroke(dp(2), mauVien) else setStroke(dp(1), vienNhat(mau))
        }
    }

    /**
     * Vien rat nhat quanh the.
     *
     * Can no vi the "khong mau" gan nhu trung nen man hinh — khong co vien thi
     * hai the canh nhau nhin nhu mot khoi lien.
     */
    private fun vienNhat(mau: BangMau): Int =
        if (mau.toi) Color.argb(20, 255, 255, 255) else Color.argb(18, 0, 0, 0)
}
