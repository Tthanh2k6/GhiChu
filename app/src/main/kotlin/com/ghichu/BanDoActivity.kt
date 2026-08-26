package com.ghichu

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Man sua ban do tu duy.
 *
 * Nhan JSON cua cay qua intent, tra JSON da sua ve qua `setResult` — KHONG tu
 * ghi vao CSDL. Ly do: ban do song trong mot KHOI cua ghi chu, ma ca ghi chu do
 * lai dang mo o [SuaGhiChuActivity]; hai noi cung ghi mot ghi chu la chac chan
 * co luc cai nay de mat cai kia.
 */
class BanDoActivity : Activity() {

    private val md by lazy { resources.displayMetrics }
    private fun dp(v: Int) = (v * md.density).toInt()

    private lateinit var mau: BangMau
    private lateinit var banDo: BanDoView
    private lateinit var nhanChon: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mau = BangMau.cua(this)
        setTheme(mau.themeManHinh)

        val goc = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(mau.nen)
            setOnApplyWindowInsetsListener { v, insets ->
                val thanh = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(0, thanh.top, 0, thanh.bottom)
                insets
            }
        }

        banDo = BanDoView(this).apply {
            this.goc = XepBanDo.docJson(intent.getStringExtra(KHOA_JSON).orEmpty())
            khiChonNut = { capNhatNhan() }
        }

        goc.addView(thanhTren())
        goc.addView(banDo, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
        ))
        goc.addView(thanhDuoi())
        setContentView(goc)
        capNhatNhan()
    }

    private fun thanhTren(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(6), dp(6), dp(10), dp(6))

        addView(nutAnh(R.drawable.ic_quay_lai, "Xong") { xong() })
        nhanChon = TextView(this@BanDoActivity).apply {
            setTextColor(mau.chuPhu)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        addView(nhanChon, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(nutChu("⤢", "Vừa màn hình") { banDo.vuaMan() })
    }

    /**
     * Thanh cong cu: cac phep tren nut DANG CHON.
     *
     * Chua chon gi thi coi nhu dang chon nut goc — nguoi dung vua mo man hinh la
     * bam "thêm nhánh" duoc ngay, khong phai cham vao goc truoc.
     */
    private fun thanhDuoi(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setBackgroundColor(mau.nenThanhDuoi)
        setPadding(dp(10), dp(8), dp(10), dp(8))

        addView(nutChu("＋", "Thêm nhánh con") { themCon() })
        addView(nutChu("✎", "Sửa chữ") { suaChu() })
        addView(nutChu("🗑", "Xoá nhánh") { xoaNhanh() })
    }

    // ------------------------------------------------------------ hanh dong

    private fun dangChon(): Nut = banDo.dangChon ?: banDo.goc

    private fun themCon() {
        val cha = dangChon()
        val moi = Nut("")
        cha.con += moi
        banDo.dangChon = moi
        banDo.xepLai()
        capNhatNhan()
        // Nut moi luon rong — mo thang hop go chu, khong bat nguoi dung bam them
        // mot nut nua chi de lam viec hien nhien tiep theo.
        suaChu()
    }

    private fun suaChu() {
        val nut = dangChon()
        val o = EditText(this).apply {
            setText(nut.chu)
            hint = "Nội dung nhánh"
            setSelection(text.length)
        }
        HopThoai(this, mau)
            .setTitle("Nhánh")
            .setView(o)
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Xong") { _, _ ->
                nut.chu = o.text.toString().trim()
                banDo.xepLai()
                capNhatNhan()
            }
            .show()
    }

    private fun xoaNhanh() {
        val nut = dangChon()
        if (nut === banDo.goc) {
            Toast.makeText(this, "Không xoá được nhánh gốc", Toast.LENGTH_SHORT).show()
            return
        }
        val soCon = nut.demNut() - 1
        val hoi = if (soCon > 0) "Xoá nhánh này và $soCon nhánh con?" else "Xoá nhánh này?"

        HopThoai(this, mau)
            .setMessage(hoi)
            .nguyHiem()
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Xoá") { _, _ ->
                val cha = banDo.goc.chaCua(nut)
                banDo.goc.xoaCon(nut)
                banDo.dangChon = cha
                banDo.xepLai()
                capNhatNhan()
            }
            .show()
    }

    private fun capNhatNhan() {
        val nut = dangChon()
        nhanChon.text = "Đang chọn: " + XepBanDo.nhan(nut)
    }

    private fun xong() {
        setResult(
            RESULT_OK,
            Intent().putExtra(KHOA_JSON, XepBanDo.ghiJson(banDo.goc))
                .putExtra(KHOA_SO_NUT, banDo.goc.demNut()),
        )
        finish()
    }

    override fun onBackPressed() {
        // Back cung la "xong": man nay khong co gi de huy — moi thay doi da hien
        // ngay tren ban do, bat nguoi dung chon Luu/Huy o day chi de gay boi roi.
        xong()
    }

    // ------------------------------------------------------------ tien ich

    private fun nutAnh(anh: Int, ten: String, bam: () -> Unit): ImageButton =
        ImageButton(this).apply {
            setImageResource(anh)
            setColorFilter(mau.chuChinh)
            contentDescription = ten
            background = null
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            setOnClickListener { bam() }
        }

    private fun nutChu(ky: String, ten: String, bam: () -> Unit): View =
        TextView(this).apply {
            text = ky
            contentDescription = ten
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(mau.chuChinh)
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(BangMau.mauMo(mau.chuChinh, 0.08f))
            }
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(44)).apply {
                leftMargin = dp(6)
                rightMargin = dp(6)
            }
            isClickable = true
            setOnClickListener { bam() }
        }

    companion object {
        const val KHOA_JSON = "json"
        const val KHOA_SO_NUT = "so_nut"
    }
}
