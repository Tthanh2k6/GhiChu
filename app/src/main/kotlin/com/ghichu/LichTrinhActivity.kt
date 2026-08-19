package com.ghichu

import android.app.Activity
import android.os.Bundle
import android.view.WindowInsets

/**
 * Man Lich trinh chay doc lap.
 *
 * Giu lai ben canh [LichTrinhFragment] vi day la duong mo nhanh khong phu thuoc
 * navigator cua app (`am start -n com.miui.notes.mod/com.ghichu.LichTrinhActivity`),
 * rat tien khi can thu nhanh phan giao dien ma khong phai vao dung tab.
 */
class LichTrinhActivity : Activity() {

    private lateinit var man: ManLichTrinh

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Truoc moi thu khac: `Button`/`EditText` lay mau tu THEME, khong
        // tu BangMau — dat sai la nut den si tren nen trang.
        setTheme(BangMau.cua(this).themeManHinh)
        man = ManLichTrinh(this)
        // Chay doc lap thi khong co thanh tieu de cua app — muon thanh cua Activity.
        man.datTieuDe = { chu -> title = chu }

        val goc = man.taoView()
        // Chay doc lap thi khong co miuix lo phan thanh trang thai — tu chua lay.
        goc.setOnApplyWindowInsetsListener { view, insets ->
            val thanh = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(thanh.left, thanh.top, thanh.right, thanh.bottom)
            insets
        }
        setContentView(goc)
    }

    override fun onResume() {
        super.onResume()
        man.nap()
    }

    override fun onDestroy() {
        man.dong()
        super.onDestroy()
    }
}
