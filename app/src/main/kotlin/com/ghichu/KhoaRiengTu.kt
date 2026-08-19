package com.ghichu

import android.content.Context
import java.util.Base64
import javax.crypto.SecretKey

/**
 * Quan ly mat khau va phien mo khoa cua ghi chu rieng tu.
 *
 * Khoa chi song TRONG BO NHO va chi trong luc app dang mo. Roi app la khoa bi
 * xoa, lan sau vao phai go mat khau lai — do la ca diem cua tinh nang nay.
 *
 * KHONG luu mat khau o dau. Chi luu:
 *   * `muoi`  — chuoi ngau nhien de sinh khoa (lo cung khong sao);
 *   * `the`   — mot cau biet truoc da ma hoa, de biet mat khau go co dung khong.
 *
 * Vi vay **mat mat khau la mat ghi chu rieng tu**, khong ai khoi phuc duoc, ke
 * ca ban. [canhBao] la cau phai hien cho nguoi dung TRUOC khi ho dat mat khau.
 */
object KhoaRiengTu {

    const val canhBao =
        "Ghi chú riêng tư được mã hoá bằng mật khẩu này. " +
            "Quên mật khẩu là mất hẳn nội dung — không ai khôi phục được, kể cả ứng dụng."

    private const val TEP = "ghichu_rieng_tu"
    private const val K_MUOI = "muoi"
    private const val K_THE = "the"

    private lateinit var caiDat: android.content.SharedPreferences

    /** Khoa cua phien dang mo. `null` = dang khoa. */
    @Volatile
    private var khoaPhien: SecretKey? = null

    fun khoiTao(context: Context) {
        if (::caiDat.isInitialized) return
        caiDat = context.applicationContext.getSharedPreferences(TEP, Context.MODE_PRIVATE)
    }

    val daDatMatKhau: Boolean get() = caiDat.getString(K_MUOI, null) != null

    val dangMo: Boolean get() = khoaPhien != null

    /** Khoa lai. Goi khi nguoi dung roi app hoac tu bam khoa. */
    fun khoaLai() {
        khoaPhien = null
    }

    /**
     * Dat mat khau lan dau. **Cham** — goi tu luong nen.
     *
     * Tra ve false neu da co mat khau roi: doi mat khau phai di duong khac
     * ([doiMatKhau]) vi con phai ma hoa lai toan bo ghi chu cu.
     */
    fun dat(matKhau: String): Boolean {
        if (daDatMatKhau || matKhau.isEmpty()) return false
        val muoi = MaHoa.muoiMoi()
        val k = MaHoa.khoa(matKhau, muoi)
        caiDat.edit()
            .putString(K_MUOI, Base64.getEncoder().encodeToString(muoi))
            .putString(K_THE, MaHoa.theKiem(k))
            .apply()
        khoaPhien = k
        return true
    }

    /**
     * Mo khoa. **Cham** — goi tu luong nen. Tra ve false neu sai mat khau.
     */
    fun mo(matKhau: String): Boolean {
        val muoi = muoi() ?: return false
        val the = caiDat.getString(K_THE, null) ?: return false
        val k = MaHoa.khoa(matKhau, muoi)
        if (!MaHoa.dungMatKhau(the, k)) return false
        khoaPhien = k
        return true
    }

    /**
     * Doi mat khau: ma hoa lai moi ghi chu rieng tu bang khoa moi.
     *
     * **Cham** — goi tu luong nen. Tra ve false neu mat khau cu sai.
     *
     * Lam trong MOT giao dich: dut ganh giua chung (het pin, he thong giet app)
     * thi mot nua ghi chu ma bang khoa cu, mot nua bang khoa moi — va khong khoa
     * nao mo duoc ca hai.
     */
    fun doiMatKhau(kho: KhoDuLieu, cu: String, moi: String): Boolean {
        if (moi.isEmpty()) return false
        val muoi = muoi() ?: return false
        val the = caiDat.getString(K_THE, null) ?: return false
        val kCu = MaHoa.khoa(cu, muoi)
        if (!MaHoa.dungMatKhau(the, kCu)) return false

        val muoiMoi = MaHoa.muoiMoi()
        val kMoi = MaHoa.khoa(moi, muoiMoi)

        val db = kho.writableDatabase
        db.beginTransaction()
        try {
            db.query(
                KhoDuLieu.BANG_GHI_CHU, arrayOf("_id", "tieu_de", "noi_dung"),
                "rieng_tu = 1", null, null, null, null,
            ).use { c ->
                while (c.moveToNext()) {
                    val v = android.content.ContentValues().apply {
                        put("tieu_de", MaHoa.maHoa(MaHoa.giaiMa(c.getString(1).orEmpty(), kCu), kMoi))
                        put("noi_dung", MaHoa.maHoa(MaHoa.giaiMa(c.getString(2).orEmpty(), kCu), kMoi))
                    }
                    db.update(KhoDuLieu.BANG_GHI_CHU, v, "_id = ?", arrayOf(c.getLong(0).toString()))
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        caiDat.edit()
            .putString(K_MUOI, Base64.getEncoder().encodeToString(muoiMoi))
            .putString(K_THE, MaHoa.theKiem(kMoi))
            .apply()
        khoaPhien = kMoi
        return true
    }

    /**
     * Khoa cua phien dang mo.
     *
     * Nem [LoiMaHoa] neu dang khoa. CO Y nem chu khong tra null: moi cho doc/ghi
     * ghi chu rieng tu ma quen kiem tra thi hong ngay va thay ro, con tra null
     * roi ai do lo di la ghi chu rieng tu bi luu ra dang chu thuong.
     */
    fun khoaDangMo(): SecretKey =
        khoaPhien ?: throw LoiMaHoa("Ghi chú riêng tư đang khoá")

    private fun muoi(): ByteArray? = caiDat.getString(K_MUOI, null)
        ?.let { Base64.getDecoder().decode(it) }
}
