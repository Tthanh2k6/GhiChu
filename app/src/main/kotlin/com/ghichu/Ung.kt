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
        KhoaRiengTu.khoiTao(this)
        // Tu day dong bo khi app xuong nen (bo sung cho luot dong bo luc MO app
        // o ManHinhChinhActivity.onResume).
        DongBoTuDong.gan(this)
        // Hen lai bao thuc nhac nho ngay khi app song lai. Can cho lan dau sau
        // khi cai: chua sua gi thi khong co duong nao khac goi toi [NhacNho].
        //
        // O LUONG KHAC: viec nay mo CSDL va quet ca su kien lan ghi chu — lam o
        // luong chinh la keo dai thoi gian mo app cho mot viec khong ai dang doi.
        Thread { NhacNho.datLai(this) }.start()
    }

    companion object {
        /** Duong tat cho Activity: `Ung.cua(this).ghiChu`. */
        fun cua(context: android.content.Context): Ung =
            context.applicationContext as Ung
    }
}
