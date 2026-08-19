package com.ghichu

import android.app.Application

/**
 * Diem chung cua ca app.
 *
 * Giu **mot** [KhoDuLieu] cho toan app chu khong moi man mot cai:
 * `SQLiteOpenHelper` giu mot ket noi mo, hai cai tro len la hai ket noi vao cung
 * mot tep — ghi dong thoi thi dinh `database is locked`. Mot cai thi SQLite tu
 * xep hang giup.
 */
class Ung : Application() {

    val kho: KhoDuLieu by lazy { KhoDuLieu(this) }
    val ghiChu: KhoGhiChu by lazy { KhoGhiChu(kho) }

    override fun onCreate() {
        super.onCreate()
        DongBo.khoiTao(this)
        Gemini.khoiTao(this)
    }

    companion object {
        /** Duong tat cho Activity: `Ung.cua(this).ghiChu`. */
        fun cua(context: android.content.Context): Ung =
            context.applicationContext as Ung
    }
}
