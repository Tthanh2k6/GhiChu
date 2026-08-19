package com.ghichu

/**
 * Mot ghi chu.
 *
 * [noiDung] la **markup cua Xiaomi**, khong phai chu tron — xem doan dau
 * [KhoDuLieu] va [NoiDungGhiChu] de biet vi sao giu dinh dang do.
 */
class GhiChu(
    val id: Long = 0L,
    val uid: String = "",
    val tieuDe: String = "",
    val noiDung: String = "",
    val chuTron: String = "",
    val thuMuc: String = "",
    val ghim: Boolean = false,
    val mau: Int = 0,
    val riengTu: Boolean = false,
    val nhacLuc: Long? = null,
    val ngayTao: Long = 0L,
    val ngaySua: Long = 0L,
    val daXoa: Boolean = false,
) {

    /**
     * Dong dau tien co chu, dung lam tieu de khi nguoi dung khong tu dat.
     *
     * App Ghi chu cua Xiaomi cung lam vay va nguoi dung da quen — bat go tieu de
     * rieng moi luu duoc la them mot buoc cho mot ghi chu hai chu.
     */
    fun tieuDeHien(): String {
        if (tieuDe.isNotBlank()) return tieuDe
        val dong = chuTron.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        return if (dong.isEmpty()) "" else dong.take(80)
    }

    /** Vai dong dau de xem truoc trong danh sach, da bo dong dung lam tieu de. */
    fun xemTruoc(soDong: Int = 3): String {
        val dong = chuTron.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val bo = if (tieuDe.isBlank() && dong.isNotEmpty()) 1 else 0
        return dong.drop(bo).take(soDong).joinToString("\n")
    }

    val rong: Boolean get() = tieuDe.isBlank() && chuTron.isBlank()
}
