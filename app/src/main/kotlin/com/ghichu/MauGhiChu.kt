package com.ghichu

/**
 * Mau nen cua mot ghi chu.
 *
 * KHAC [MauSuKien]: su kien la khoi nho tren lich nen mau phai dam de doc duoc
 * chu tren no; ghi chu la ca mot the to nen mau phai NHAT, khong thi doc ca bai
 * chu tren nen mau la moi mat.
 *
 * Chi so 0 = "khong mau" (theo nen man hinh). Chi so nay di thang len may chu o
 * truong `bg`, nen **khong duoc chen mau moi vao giua bang** — chen la moi ghi
 * chu cu doi mau. Them thi them vao CUOI.
 */
object MauGhiChu {

    /** Cap (nen sang, nen toi). Phan tu 0 la "khong mau", xu ly rieng. */
    private val BANG = arrayOf(
        intArrayOf(0, 0), // khong mau — dung nen man hinh
        intArrayOf(0xFFFFF3C4.toInt(), 0xFF3A3320.toInt()), // vang
        intArrayOf(0xFFFFE0E0.toInt(), 0xFF3A2626.toInt()), // hong
        intArrayOf(0xFFDDF3E0.toInt(), 0xFF223328.toInt()), // xanh la
        intArrayOf(0xFFDCEBFF.toInt(), 0xFF1F2C3D.toInt()), // xanh duong
        intArrayOf(0xFFEBDFFB.toInt(), 0xFF2E2740.toInt()), // tim
        intArrayOf(0xFFFFE6CC.toInt(), 0xFF3A2C1D.toInt()), // cam
        intArrayOf(0xFFD9F1F0.toInt(), 0xFF1E3332.toInt()), // ngoc
    )

    val TEN = arrayOf(
        "Không màu", "Vàng", "Hồng", "Xanh lá", "Xanh dương", "Tím", "Cam", "Ngọc",
    )

    val soMau: Int get() = BANG.size

    /** Nen cua the ghi chu. Chi so 0 hoac ngoai bang -> nen mac dinh cua man hinh. */
    fun nen(chiSo: Int, mau: BangMau): Int {
        if (chiSo <= 0 || chiSo >= BANG.size) {
            // Nen the mac dinh: hoi lech khoi nen man hinh de con thay ranh the.
            return if (mau.toi) 0xFF161616.toInt() else 0xFFF5F5F5.toInt()
        }
        return BANG[chiSo][if (mau.toi) 1 else 0]
    }

    /** Mau xem truoc cho o chon mau — luon co mau, ke ca o "khong mau". */
    fun xemTruoc(chiSo: Int, mau: BangMau): Int =
        if (chiSo <= 0) (if (mau.toi) 0xFF2A2A2A.toInt() else 0xFFE8E8E8.toInt())
        else nen(chiSo, mau)
}
