package com.ghichu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.util.LruCache
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Mot the ghi chu trong luoi o man hinh chinh.
 *
 * MOI CON SO O DAY DO TU APP GOC, khong uoc luong bang mat. Nguon:
 * `docs/SPEC-GOC.md` (muc "Luoi + The"), trich tu
 * `res/layout/common_note_item_grid.xml`, `values/dimens.xml`, va ma
 * `CommonNoteGridItem.java` cua app Ghi chu Xiaomi 2.4.2.5. Ten tai nguyen goc
 * ghi trong chu thich de con doi chieu — dung doi vi "nhin thay hop hon" ma chua
 * do lai.
 *
 * Hinh dang: the PHANG — khong vien, khong bong, chi phan biet voi nen trang
 * bang mau ([BangMau.nenThe] tren [BangMau.nen]). Bo goc 16dp bang outline va
 * `clipToOutline` nhu ban goc, nen anh xem truoc o dau the cung bi cat theo goc.
 */
class TheGhiChu(context: Context) : FrameLayout(context) {

    private val md = resources.displayMetrics
    private fun dp(v: Int) = (v * md.density).toInt()
    private fun dpF(v: Float) = v * md.density

    private val cot = LinearLayout(context)
    private val oAnh = ImageView(context)
    private val noiDung = LinearLayout(context)
    private val oTieuDe = TextView(context)
    private val oThan = TextView(context)
    private val hangChan = LinearLayout(context)
    private val oGio = TextView(context)
    private val dauGhim = ImageView(context)
    private val dauGhiAm = ImageView(context)
    private val dauBanDo = ImageView(context)
    private val dauDongHo = ImageView(context)
    private val oChon = OChon(context)

    init {
        // v12_grid_item_shape_radius = 16dp — bo goc bang outline de clip ca anh.
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, o: Outline) {
                o.setRoundRect(0, 0, v.width, v.height, dpF(16f))
            }
        }
        clipToOutline = true

        cot.orientation = LinearLayout.VERTICAL
        addView(cot, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // Anh xem truoc: height 136dp (hardcode trong layout goc), centerCrop, nam TREN CUNG.
        oAnh.scaleType = ImageView.ScaleType.CENTER_CROP
        oAnh.visibility = View.GONE
        cot.addView(oAnh, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(136)))

        // v12_item_grid_padding_start/top/end = 16dp, bottom 0 (hang chan tu mang marginBottom).
        noiDung.orientation = LinearLayout.VERTICAL
        noiDung.setPadding(dp(16), dp(16), dp(16), 0)
        noiDung.minimumHeight = dp(102) // v12_default_theme_grid_bg_min_height
        cot.addView(noiDung, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        // grid_text_primary_size = 17sp, mipro-medium, MOT dong, gian dong 1.2.
        // Mot dong la co y cua ban goc: hai dong thi the cao thap khong deu nhau.
        oTieuDe.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        oTieuDe.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        oTieuDe.isSingleLine = true
        oTieuDe.ellipsize = TextUtils.TruncateAt.END
        oTieuDe.setLineSpacing(0f, 1.2f)
        noiDung.addView(oTieuDe)

        // grid_text_secondary_size = 14sp, marginTop 12dp, maxLines 4, gian dong 1.2.
        oThan.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        oThan.maxLines = 4
        oThan.ellipsize = TextUtils.TruncateAt.END
        oThan.setLineSpacing(0f, 1.2f)
        noiDung.addView(oThan, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(12) })

        // Hang chan: gio ben TRAI (weight 1, marginEnd 6dp), cac dau hieu 14dp vang
        // ben PHAI, moi dau marginEnd 6dp. paddingTop v12_item_grid_label_margin_top
        // = 12dp, marginBottom v12_item_grid_padding_bottom = 16dp.
        hangChan.orientation = LinearLayout.HORIZONTAL
        hangChan.gravity = Gravity.CENTER_VERTICAL
        hangChan.setPadding(0, dp(12), 0, 0)

        // grid_text_tertiary_size = 12DP — dp chu khong phai sp, dung nhu goc.
        oGio.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        oGio.isSingleLine = true
        oGio.ellipsize = TextUtils.TruncateAt.END
        hangChan.addView(oGio, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = dp(6)
        })
        // Thu tu dau hieu dung nhu layout goc: stick, voice, mind_map, ... alarm cuoi.
        for ((anh, icon) in listOf(
            dauGhim to R.drawable.ic_goc_ghim,
            dauGhiAm to R.drawable.ic_goc_ghi_am,
            dauBanDo to R.drawable.ic_goc_ban_do,
            dauDongHo to R.drawable.ic_goc_dong_ho,
        )) {
            anh.setImageResource(icon)
            anh.visibility = View.GONE
            hangChan.addView(anh, LinearLayout.LayoutParams(dp(14), dp(14)).apply { rightMargin = dp(6) })
        }
        // Dau cuoi cung khong co marginEnd (layout goc: alarm khong marginEnd).
        (dauDongHo.layoutParams as LinearLayout.LayoutParams).rightMargin = 0

        noiDung.addView(hangChan, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(16) })

        // Checkbox che do chon: goc duoi-phai, marginBottom grid_checkbox_margin_bottom
        // = 12dp, marginEnd grid_checkbox_margin_end = 16dp, multi_checkbox_size = 24dp.
        oChon.visibility = View.GONE
        addView(oChon, LayoutParams(dp(24), dp(24)).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            bottomMargin = dp(12)
            rightMargin = dp(16)
        })
    }

    /**
     * @param cheDoChon dang o che do chon nhieu — hien checkbox (ke ca chua tick)
     *   va don hang chan sang trai 40dp nhuong cho, nhu `setBarMargin` cua ban goc.
     */
    fun gan(g: GhiChu, mau: BangMau, chon: Boolean, cheDoChon: Boolean = chon) {
        val tieuDe = g.tieuDeHien()
        val than = g.xemTruoc(4)

        oTieuDe.text = tieuDe.ifBlank { "Không có văn bản" }
        oTieuDe.setTextColor(if (tieuDe.isBlank()) mau.chuMo else mau.chuTheTieuDe)

        oThan.text = than
        oThan.setTextColor(mau.chuTheThan)
        oThan.visibility = if (than.isBlank()) View.GONE else View.VISIBLE

        oGio.text = NgayVN.nhanDanhSach(g.ngaySua)
        oGio.setTextColor(mau.chuMo)

        dauGhim.visibility = if (g.ghim) View.VISIBLE else View.GONE
        dauGhiAm.visibility = if (g.noiDung.contains("<audio")) View.VISIBLE else View.GONE
        dauBanDo.visibility = if (g.noiDung.contains("<mind>")) View.VISIBLE else View.GONE
        dauDongHo.visibility = if (g.nhacLuc != null) View.VISIBLE else View.GONE

        // Anh dau tien trong ghi chu -> xem truoc 136dp o dau the, nhu ban goc.
        val tenAnh = THE_ANH.find(g.noiDung)?.groupValues?.get(1)
        val bm = tenAnh?.let { anhNho(context, it) }
        if (bm != null) {
            oAnh.setImageBitmap(bm)
            oAnh.visibility = View.VISIBLE
            // Co anh thi paddingTop phan chu giam con mot nua (16dp*0.5), minHeight 0.
            noiDung.setPadding(dp(16), dp(8), dp(16), 0)
            noiDung.minimumHeight = 0
        } else {
            oAnh.setImageDrawable(null)
            oAnh.visibility = View.GONE
            noiDung.setPadding(dp(16), dp(16), dp(16), 0)
            noiDung.minimumHeight = dp(102)
        }

        background = ColorDrawable(MauGhiChu.nen(g.mau, mau))

        // Chon: phu lop xam mo len ca the (common_note_list_item_press_fg) — khong doi vien.
        foreground = if (chon) ColorDrawable(mau.lopPhuChon) else null

        oChon.visibility = if (cheDoChon) View.VISIBLE else View.GONE
        oChon.daChon = chon
        oChon.mauNhan = mau.nhan
        oChon.mauVien = if (mau.toi) 0x66FFFFFF else 0x4D000000
        (hangChan.layoutParams as LinearLayout.LayoutParams).rightMargin = if (cheDoChon) dp(40) else 0
    }

    // ------------------------------------------------------------ nhan vao the

    /**
     * Nhan tay: the co ve 0.9 roi nay lai 1.0 — Folme touchDownScale cua ban goc
     * (spring damping 0.85, response 0.3s). Tai hien bang animate() cua khung:
     * co lai nhanh, nay ve co vuot nhe (Overshoot) cho ra chat lo xo.
     *
     * KHONG nuot su kien: chi nhin roi tra cho xu ly cham binh thuong.
     */
    override fun dispatchTouchEvent(su: MotionEvent): Boolean {
        when (su.actionMasked) {
            MotionEvent.ACTION_DOWN -> animate().scaleX(0.9f).scaleY(0.9f)
                .setDuration(180).setInterpolator(DecelerateInterpolator()).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animate().scaleX(1f).scaleY(1f)
                .setDuration(320).setInterpolator(OvershootInterpolator(1.1f)).start()
        }
        return super.dispatchTouchEvent(su)
    }

    /**
     * Checkbox kieu MIUI: hinh TRON, chua tick la vien mo, tick roi la dia vang
     * (theme_main_color) voi dau tick trang. Ve tay vi `CheckBox` cua khung mang
     * theo hinh vuong Material — sai ngay cai nhin dau.
     */
    private class OChon(context: Context) : View(context) {
        var daChon = false
            set(v) { field = v; invalidate() }
        var mauNhan = 0xFFFFBB0F.toInt()
        var mauVien = 0x4D000000

        private val son = Paint(Paint.ANTI_ALIAS_FLAG)
        private val duong = Path()

        override fun onDraw(c: Canvas) {
            val r = width / 2f
            if (daChon) {
                son.style = Paint.Style.FILL
                son.color = mauNhan
                c.drawCircle(r, r, r, son)
                son.style = Paint.Style.STROKE
                son.strokeWidth = width * 0.09f
                son.strokeCap = Paint.Cap.ROUND
                son.strokeJoin = Paint.Join.ROUND
                son.color = 0xFFFFFFFF.toInt()
                duong.reset()
                duong.moveTo(width * 0.28f, height * 0.52f)
                duong.lineTo(width * 0.44f, height * 0.68f)
                duong.lineTo(width * 0.73f, height * 0.36f)
                c.drawPath(duong, son)
            } else {
                son.style = Paint.Style.STROKE
                son.strokeWidth = width * 0.07f
                son.color = mauVien
                c.drawCircle(r, r, r - son.strokeWidth / 2, son)
            }
        }
    }

    companion object {
        private val THE_ANH = Regex("<img[^>]*src=\"([^\"]+)\"")

        /**
         * Anh xem truoc, thu nho san, giu trong bo nho de cuon luoi khong giat.
         *
         * Giai ma anh 1600px cho tung the moi lan ve la cuon khung ngay; giai ma
         * mot lan ve ~400px roi nho lai thi cuon muot. 4 MB du cho vai chuc anh.
         */
        private val boNho = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
            override fun sizeOf(k: String, v: Bitmap) = v.byteCount
        }

        fun anhNho(context: Context, ten: String): Bitmap? {
            boNho.get(ten)?.let { return it }
            val f = KhoAnh.tep(context, ten)
            if (!f.exists()) return null
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, o)
            var mau = 1
            while (o.outWidth / (mau * 2) >= 400) mau *= 2
            val bm = BitmapFactory.decodeFile(
                f.absolutePath, BitmapFactory.Options().apply { inSampleSize = mau },
            ) ?: return null
            boNho.put(ten, bm)
            return bm
        }

        /** Xoa anh khoi bo nho khi ghi chu doi anh — khong thi the van hien anh cu. */
        fun quenAnh(ten: String) { boNho.remove(ten) }
    }
}
