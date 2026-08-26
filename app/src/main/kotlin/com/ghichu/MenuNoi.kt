package com.ghichu

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView

/**
 * Popup menu kieu miuix (ImmersionMenu) — so do o docs/SPEC-GOC-dot3.md muc C.
 *
 * Dung cho nut "…" cua man soan va cac menu ngu canh. `PopupMenu` cua khung mo
 * mot hop vuong Material co ripple; ban goc la mot tam bo goc 16dp noi cao, bung
 * ra tu goc tren-phai cua nut va PHU LEN nut.
 *
 * So do (ten goc):
 *   * rong 200..288dp, bo goc 16dp (miuix_theme_radius_common), nen #FFFFFF /
 *     #2C2C2C (surface_highest), elevation 31dp, bong #4D000000;
 *   * dong minHeight 50dp, dem ngang 20dp, dem doc: dau 20/12, giua 12/12, cuoi
 *     12/20, mot dong 20/20; chu 16sp medium #000000 / #F2FFFFFF, toi da 2 dong;
 *   * mep phai va mep TREN trung voi nut; vao: scale 0.8 -> 1 tu goc tren-phai +
 *     alpha, 150ms decelerate 1.5.
 */
class MenuNoi(private val ctx: Context, private val mau: BangMau) {

    private val md = ctx.resources.displayMetrics
    private fun dp(v: Int) = (v * md.density).toInt()
    private fun dpF(v: Float) = v * md.density

    private val muc = ArrayList<Pair<String, () -> Unit>>()

    fun them(chu: String, lam: () -> Unit): MenuNoi = apply { muc += chu to lam }

    fun hien(neo: View) {
        if (muc.isEmpty()) return
        val cot = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpF(16f)
                setColor(if (mau.toi) 0xFF2C2C2C.toInt() else 0xFFFFFFFF.toInt())
            }
            clipToOutline = true
            minimumWidth = dp(200)
        }

        lateinit var cuaSo: PopupWindow
        for ((i, m) in muc.withIndex()) {
            val dau = i == 0
            val cuoi = i == muc.size - 1
            cot.addView(TextView(ctx).apply {
                text = m.first
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(if (mau.toi) 0xF2FFFFFF.toInt() else 0xFF000000.toInt())
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                minHeight = dp(50)
                setPadding(dp(20), dp(if (dau) 20 else 12), dp(20), dp(if (cuoi) 20 else 12))
                isClickable = true
                setOnTouchListener { v, su ->
                    when (su.actionMasked) {
                        MotionEvent.ACTION_DOWN -> v.foreground =
                            ColorDrawable(if (mau.toi) 0x1FFFFFFF else 0x0D000000)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.foreground = null
                    }
                    false
                }
                setOnClickListener {
                    cuaSo.dismiss()
                    m.second()
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        val cuon = ScrollView(ctx).apply { addView(cot) }
        cuon.measure(
            View.MeasureSpec.makeMeasureSpec(dp(288), View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(dp(436), View.MeasureSpec.AT_MOST),
        )
        val rong = cuon.measuredWidth.coerceIn(dp(200), dp(288))
        val cao = cuon.measuredHeight.coerceAtMost(dp(436))

        cuaSo = PopupWindow(cuon, rong, cao, true).apply {
            setBackgroundDrawable(ColorDrawable(0))
            isOutsideTouchable = true
            elevation = dpF(31f)
            animationStyle = 0
        }

        // Mep phai trung mep phai nut, mep tren trung mep tren nut (phu len nut).
        // Giu cach mep man hinh 16dp.
        val viTri = IntArray(2)
        neo.getLocationInWindow(viTri)
        val x = (viTri[0] + neo.width - rong).coerceIn(dp(16), md.widthPixels - rong - dp(16))
        val y = viTri[1].coerceAtLeast(dp(8))
        neo.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        cuaSo.showAtLocation(neo, Gravity.NO_GRAVITY, x, y)

        // Vao: scale 0.8 -> 1 quanh goc tren-phai + alpha, 150ms decelerate 1.5.
        cuon.pivotX = rong.toFloat()
        cuon.pivotY = 0f
        cuon.scaleX = 0.8f
        cuon.scaleY = 0.8f
        cuon.alpha = 0f
        cuon.animate().scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(150).setInterpolator(DecelerateInterpolator(1.5f)).start()
    }
}
