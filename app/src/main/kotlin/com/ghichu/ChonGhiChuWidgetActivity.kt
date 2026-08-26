package com.ghichu

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Man CHON ghi chu cho widget 2x2 — hien khi nguoi dung vua keo widget ra.
 *
 * He thong goi man nay voi `EXTRA_APPWIDGET_ID`; ta liet ke ghi chu, nguoi dung
 * bam mot cai, luu anh xa (widgetId -> note _id) roi ve `RESULT_OK`. Neu nguoi
 * dung thoat (back) truoc khi chon thi `RESULT_CANCELED` — he thong tu go widget.
 *
 * `exported="true"` la BAT BUOC: he thong widget o tien trinh khac goi man nay.
 */
class ChonGhiChuWidgetActivity : Activity() {

    private val md by lazy { resources.displayMetrics }
    private fun dp(v: Int) = (v * md.density).toInt()
    private lateinit var mau: BangMau
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mau = BangMau.cua(this)
        setTheme(mau.themeManHinh)
        // Thoat khong chon gi -> he thong go widget dang cho.
        setResult(RESULT_CANCELED)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val goc = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(mau.nen)
            setOnApplyWindowInsetsListener { v, insets ->
                val t = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(0, t.top, 0, t.bottom)
                insets
            }
        }
        goc.addView(TextView(this).apply {
            text = "Chọn ghi chú cho widget"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            setTextColor(mau.chuChinh)
            setPadding(dp(20), dp(16), dp(20), dp(12))
        })

        val ds = try {
            Ung.cua(this).ghiChu.danhSach()
        } catch (_: Exception) {
            emptyList()
        }

        if (ds.isEmpty()) {
            goc.addView(TextView(this).apply {
                text = "Chưa có ghi chú nào. Tạo một ghi chú rồi thêm widget lại."
                setTextColor(mau.chuMo)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setPadding(dp(20), dp(8), dp(20), dp(8))
            })
        } else {
            val cot = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            for (g in ds) cot.addView(mucGhiChu(g))
            goc.addView(ScrollView(this).apply { addView(cot) }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
            ))
        }
        setContentView(goc)
    }

    private fun mucGhiChu(g: GhiChu): View {
        val cot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
            isClickable = true
            setOnClickListener { chon(g.id) }
        }
        cot.addView(TextView(this).apply {
            text = g.tieuDeHien().ifBlank { "Không có văn bản" }
            setTextColor(mau.chuChinh)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        val than = g.xemTruoc(1)
        if (than.isNotBlank()) {
            cot.addView(TextView(this).apply {
                text = than
                setTextColor(mau.chuMo)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                isSingleLine = true
                ellipsize = android.text.TextUtils.TruncateAt.END
                gravity = Gravity.START
            })
        }
        return cot
    }

    private fun chon(noteId: Long) {
        GhiChuMotWidget.gan(this, widgetId, noteId)
        // Ve ngay lan dau, khong doi ghi chu doi moi hien.
        val quanLy = AppWidgetManager.getInstance(this)
        GhiChuMotWidget().onUpdate(this, quanLy, intArrayOf(widgetId))

        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
