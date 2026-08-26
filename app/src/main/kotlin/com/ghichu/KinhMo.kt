package com.ghichu

import android.content.Context
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View

/**
 * Lop kinh mo: ve lai phan noi dung nam DUOI no roi lam mo bang RenderEffect —
 * cho ra dung thanh tab "blur" cua HyperOS ma khong can thu vien nao.
 *
 * Cach lam: [nguon] la view chua noi dung (luoi / lich). Moi lan ve, dich canvas
 * ve dung vi tri tuong doi cua nguon roi goi `nguon.draw()` — noi dung duoc ve
 * lai vao day, chi phan long trong khung nay hien ra; RenderEffect lam mo ca lop,
 * roi phu mot lop mau kinh len tren. Khi noi dung cuon thi ben ngoai goi
 * `invalidate()`.
 *
 * Duoi Android 12 khong co RenderEffect: khong mo duoc, chi con lop mau — van tach
 * khoi nen, chi mat hieu ung.
 */
class KinhMo(
    context: Context,
    private val mau: BangMau,
    private val nguon: () -> View?,
) : View(context) {

    private val viTriNguon = IntArray(2)
    private val viTriDay = IntArray(2)

    init {
        if (Build.VERSION.SDK_INT >= 31) {
            val r = 22f * resources.displayMetrics.density
            setRenderEffect(RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP))
        }
    }

    override fun onDraw(c: Canvas) {
        val v = nguon()
        if (v != null && v.width > 0) {
            v.getLocationInWindow(viTriNguon)
            getLocationInWindow(viTriDay)
            c.save()
            c.translate((viTriNguon[0] - viTriDay[0]).toFloat(), (viTriNguon[1] - viTriDay[1]).toFloat())
            v.draw(c)
            c.restore()
        }
        // Mau kinh phu len: trang / den hoi trong (Mask.Pured_Regular cua HyperOS).
        c.drawColor(if (mau.toi) 0xB31C1C1C.toInt() else 0xB8FFFFFF.toInt())
    }
}
