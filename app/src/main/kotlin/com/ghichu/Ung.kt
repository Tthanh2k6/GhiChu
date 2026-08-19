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
        // Hen lai bao thuc nhac nho ngay khi app song lai. Can cho lan dau sau
        // khi cai: chua sua gi thi khong co duong nao khac goi toi [NhacNho].
        NhacNho.datLai(this)
    }

    companion object {
        /** Duong tat cho Activity: `Ung.cua(this).ghiChu`. */
        fun cua(context: android.content.Context): Ung =
            context.applicationContext as Ung
    }
}
