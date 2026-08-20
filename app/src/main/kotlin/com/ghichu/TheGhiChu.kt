package com.ghichu

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Mot the ghi chu trong luoi o man hinh chinh.
 *
 * MOI CON SO O DAY DO TU APP GOC, khong uoc luong bang mat. Nguon:
 * `reference/miui-notes/resources/res/layout/common_note_item_grid.xml` va
 * `values/dimens.xml` — ten tai nguyen goc ghi trong chu thich de con doi chieu.
 * Muon doi thi doi, nhung dung doi vi "nhin thay hop hon" ma chua do lai.
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
    private val hangChan = LinearLayout(context)
    private val oGio = TextView(context)
    private val oDau = TextView(context)

    init {
        orientation = VERTICAL
        // v12_item_grid_padding_start / _top / _end = 16dp
        setPadding(dp(16), dp(16), dp(16), 0)

        // list_item_primary_text_size = 17sp, font mipro-medium.
        // Ban goc dung MOT dong (`singleLine="true"`) — de hai dong thi the cao
        // thap khong deu nhau va luoi trong lo cho.
        oTieuDe.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        oTieuDe.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        oTieuDe.isSingleLine = true
        oTieuDe.ellipsize = TextUtils.TruncateAt.END
        oTieuDe.setLineSpacing(0f, 1.2f)
        addView(oTieuDe)

        // list_item_secondary_text_size = 14sp, cach tieu de 12dp.
        oThan.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        oThan.maxLines = 5
        oThan.ellipsize = TextUtils.TruncateAt.END
        oThan.setLineSpacing(0f, 1.2f)
        addView(oThan, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(12)
        })

        // Hang chan: gio ben TRAI, cac dau hieu (ghim, nhac) ben PHAI — dung thu
        // tu cua ban goc. v12_item_grid_label_margin_top = 12dp,
        // v12_item_grid_padding_bottom = 16dp.
        hangChan.orientation = HORIZONTAL
        hangChan.gravity = Gravity.CENTER_VERTICAL

        oGio.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        oGio.isSingleLine = true
        hangChan.addView(oGio, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        oDau.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        oDau.isSingleLine = true
        hangChan.addView(oDau)

        addView(hangChan, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(12)
            bottomMargin = dp(16)
        })
    }

    fun gan(g: GhiChu, mau: BangMau, chon: Boolean) {
        val tieuDe = g.tieuDeHien()
        val than = g.xemTruoc()

        oTieuDe.text = tieuDe.ifBlank { "Không có văn bản" }
        oTieuDe.setTextColor(if (tieuDe.isBlank()) mau.chuMo else mau.chuChinh)

        oThan.text = than
        oThan.setTextColor(mau.chuPhu)
        oThan.visibility = if (than.isBlank()) View.GONE else View.VISIBLE

        oGio.text = NgayVN.nhanDanhSach(g.ngaySua)
        oGio.setTextColor(mau.chuMo)

        // Ghim va nhac la hai dau hieu mot ky tu, khong phai hai dong chu: ban
        // goc dung bieu tuong nho o goc phai hang chan.
        val dau = StringBuilder()
        if (g.nhacLuc != null) dau.append("⏰")
        if (g.ghim) dau.append(if (dau.isEmpty()) "" else " ").append("📌")
        oDau.text = dau
        oDau.visibility = if (dau.isEmpty()) View.GONE else View.VISIBLE

        background = GradientDrawable().apply {
            // v12_grid_item_shape_radius = 16dp
            cornerRadius = dp(16).toFloat()
            setColor(MauGhiChu.nen(g.mau, mau))
            if (chon) setStroke(dp(2), mau.nhan) else setStroke(dp(1), vienNhat(mau))
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
