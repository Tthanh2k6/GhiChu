package com.ghichu

import android.content.Context

/**
 * Hang cho mau cho su kien moi.
 *
 * Truoc day su kien moi luon la mau dau bang (xanh duong), nen ca lich mot mau.
 * Gio la mot HANG CHO: lay mau o dau hang, dung xong thi day no xuong CUOI, mau
 * khac duoc uu tien len truoc. Cu the la ca 8 mau lan luot duoc dung het roi
 * moi quay vong.
 *
 * **Nguoi dung TU chon mau thi khong tinh** — hang cho khong nhuc nhich. Do la
 * lua chon co y cua ho, khong phai mau may tu gan; dung no de xoay hang la lam
 * hang lech theo thoi quen chon tay.
 *
 * Phan xoay hang ([dayXuongCuoi]) tach thanh ham thuan de kiem thu tren JVM.
 */
object HangMau {

    private const val TEP_CAI_DAT = "ghichu_lichtrinh"
    private const val K_HANG = "hang_mau"

    /** Mau se dung cho su kien moi — chua lay ra khoi hang. */
    fun dinh(context: Context): Int = doc(context).firstOrNull() ?: 0

    /**
     * Bao rang [mau] vua duoc MAY tu gan cho mot su kien: day no xuong cuoi hang.
     *
     * Nguoi dung tu chon thi DUNG goi ham nay.
     */
    fun daDung(context: Context, mau: Int) {
        ghi(context, dayXuongCuoi(doc(context), mau))
    }

    /**
     * Lay mau tiep theo VA xoay hang luon — tien cho cho tao hang loat (vd AI
     * xep thoi khoa bieu, moi mon mot mau).
     */
    fun layTiep(context: Context): Int {
        val mau = dinh(context)
        daDung(context, mau)
        return mau
    }

    // ---- Phan thuan, kiem thu duoc ----

    /** Hang day du 0..n-1 theo dung thu tu bang mau. */
    fun hangDau(soMau: Int = MauSuKien.BANG.size): List<Int> = (0 until soMau).toList()

    /**
     * Day [mau] xuong cuoi hang. Mau khong co trong hang (du lieu cu, bang mau
     * doi co) thi ghep vao cuoi chu khong lam mat hang.
     */
    fun dayXuongCuoi(hang: List<Int>, mau: Int): List<Int> =
        hang.filter { it != mau } + mau

    /**
     * Doc hang tu chuoi da luu, tu chua neu hong.
     *
     * Phai chua duoc: hang thieu mau (bang mau doi rong ra) thi su kien moi se
     * khong bao gio duoc gan mau do; hang co mau la (rong bot) thi ve ra mau
     * ngoai bang.
     */
    fun docHang(raw: String?, soMau: Int = MauSuKien.BANG.size): List<Int> {
        val hop = raw.orEmpty().split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 0 until soMau }
            .distinct()
        val thieu = (0 until soMau).filter { it !in hop }
        return hop + thieu
    }

    // ---- Doc/ghi cai dat ----

    private fun doc(context: Context): List<Int> =
        docHang(caiDat(context).getString(K_HANG, null))

    private fun ghi(context: Context, hang: List<Int>) {
        caiDat(context).edit().putString(K_HANG, hang.joinToString(",")).apply()
    }

    private fun caiDat(context: Context) =
        context.applicationContext.getSharedPreferences(TEP_CAI_DAT, Context.MODE_PRIVATE)
}
