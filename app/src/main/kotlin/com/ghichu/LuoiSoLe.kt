package com.ghichu

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * Luoi hai cot xep SO LE (staggered): the cao thap khac nhau, cot nao ngan hon
 * thi nhan the ke tiep — dung kieu `NoteGridLayoutManager` (ban sao cua
 * StaggeredGridLayoutManager) o man hinh chinh cua app goc.
 *
 * Vi sao tu viet: `GridView` cua khung ep moi hang cao bang the cao nhat — luoi
 * thanh o bang, nhin khac han ban goc. `RecyclerView` thi nam trong AndroidX,
 * ma ca app co y khong keo AndroidX vao.
 *
 * Cach lam don gian: mot `ScrollView` chua hai `LinearLayout` doc dat canh nhau.
 * Moi the duoc DO truoc (`measure` voi be rong cot) roi bo vao cot dang ngan
 * hon. Khong tai su dung view — voi vai tram ghi chu, moi the ba `TextView`,
 * bo nho van du va don gian hon nhieu so voi tu viet recycler.
 *
 * So do (docs/SPEC-GOC.md, muc "Luoi"):
 *   * gap ngang giua hai cot = 12dp, gap doc giua hai the = 12dp
 *     (mGridItemSpace 6dp x 2);
 *   * le trai/phai = 12dp; tren = 4dp (miuix_theme_padding_base);
 *   * hang dau khong co gap tren.
 */
class LuoiSoLe(context: Context) : ScrollView(context) {

    private val md = resources.displayMetrics
    private fun dp(v: Int) = (v * md.density).toInt()

    private val hang = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(12), dp(4), dp(12), 0)
        clipChildren = false
        clipToPadding = false
    }
    private val cot = arrayOf(
        LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; clipChildren = false },
        LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; clipChildren = false },
    )

    /** Goi moi khi cuon, kem vi tri cuon doc (px). Dung de thu gon tieu de lon. */
    var khiCuon: ((Int) -> Unit)? = null

    private var choXep: List<View> = emptyList()

    init {
        isVerticalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_ALWAYS
        clipChildren = false
        clipToPadding = false
        hang.addView(cot[0], LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        hang.addView(cot[1], LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            leftMargin = dp(12)
        })
        addView(hang, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    /** Khoang trong duoi cung, de the cuoi khong bi nut them / thanh tab che. */
    fun datDemDuoi(px: Int) {
        hang.setPadding(dp(12), dp(4), dp(12), px)
    }

    /**
     * Thay toan bo the. Chua biet be rong (lan dau) thi doi layout xong moi xep.
     */
    fun datThe(the: List<View>) {
        choXep = the
        if (width == 0) {
            post { xep() }
        } else {
            xep()
        }
    }

    private fun xep() {
        for (c in cot) c.removeAllViews()
        val rongCot = (width - hang.paddingLeft - hang.paddingRight - dp(12)) / 2
        if (rongCot <= 0) return

        val cao = intArrayOf(0, 0)
        for ((i, v) in choXep.withIndex()) {
            (v.parent as? ViewGroup)?.removeView(v)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            v.layoutParams = lp
            v.measure(
                MeasureSpec.makeMeasureSpec(rongCot, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
            // Hai the dau tien di thang vao hai cot — khong thi cot 0 an het the
            // ngan roi cot 1 trong tron vai the dau.
            val c = if (i < 2) i else if (cao[0] <= cao[1]) 0 else 1
            if (cot[c].childCount > 0) lp.topMargin = dp(12)
            cot[c].addView(v, lp)
            cao[c] += v.measuredHeight + (if (cot[c].childCount > 1) dp(12) else 0)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, cu: Int, caoCu: Int) {
        super.onSizeChanged(w, h, cu, caoCu)
        if (w != cu && choXep.isNotEmpty()) xep()
    }

    override fun onScrollChanged(l: Int, t: Int, cuL: Int, cuT: Int) {
        super.onScrollChanged(l, t, cuL, cuT)
        khiCuon?.invoke(t)
    }
}
