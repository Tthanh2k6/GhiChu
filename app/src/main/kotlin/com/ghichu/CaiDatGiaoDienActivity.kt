package com.ghichu

import android.app.Activity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView

/**
 * Chon giao dien Sang / Toi cho man Lich trinh.
 *
 * Vi sao la mot man RIENG chu khong phai `ListPreference` cam thang vao man Cai
 * dat cua app: `android:entries` doi mot mang tai nguyen, ma them tai nguyen moi
 * vao cay apktool la dung toi bang ma tai nguyen cua app dich — rui ro khong
 * dang, chi de doi lay mot dong. Duong `<Preference>` + `<intent>` thi khong sinh
 * tai nguyen nao.
 */
class CaiDatGiaoDienActivity : Activity() {

    private val mau by lazy { BangMau.cua(this) }
    private val md by lazy { resources.displayMetrics }
    private fun dp(v: Int) = (v * md.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Truoc moi thu khac: `Button`/`EditText` lay mau tu THEME, khong
        // tu BangMau — dat sai la nut den si tren nen trang.
        setTheme(BangMau.cua(this).themeManHinh)
        title = "Giao diện Lịch trình"

        val goc = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(mau.nen)
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setOnApplyWindowInsetsListener { v, insets ->
                val thanh = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(thanh.left + dp(20), thanh.top + dp(20), thanh.right + dp(20), thanh.bottom)
                insets
            }
        }

        goc.addView(
            TextView(this).apply {
                text = "Giao diện"
                setTextColor(mau.chuChinh)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                setPadding(0, dp(4), 0, dp(10))
            },
        )
        goc.addView(
            TextView(this).apply {
                text = "Sáng, tối hoặc theo hệ thống — áp dụng cho toàn bộ ứng dụng."
                setTextColor(mau.chuPhu)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setLineSpacing(0f, 1.25f)
                setPadding(0, 0, 0, dp(14))
            },
        )

        val dangChon = BangMau.kieu(this)
        val nhom = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }

        BangMau.TEN_KIEU.entries.forEachIndexed { i, (ma, ten) ->
            nhom.addView(
                RadioButton(this).apply {
                    id = i + 1
                    text = ten
                    isChecked = ma == dangChon
                    setTextColor(mau.chuChinh)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setPadding(dp(8), dp(14), dp(8), dp(14))
                    setOnClickListener {
                        BangMau.datKieu(this@CaiDatGiaoDienActivity, ma)
                        // Dung lai man nay bang mau moi de nguoi dung thay ngay.
                        recreate()
                    }
                },
                RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }
        goc.addView(nhom)

        setContentView(goc as View)
    }
}
