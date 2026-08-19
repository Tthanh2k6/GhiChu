package com.ghichu

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * Loi giai ma QR — **thuan Java/Kotlin, khong dung mot API Android nao**.
 *
 * Tach roi khoi may anh la co y: nho vay chay duoc tren JVM, va zxing tu sinh
 * duoc ma QR nen bai kiem thu co the ma hoa roi giai ma lai de doi chieu —
 * khong can dien thoai van biet duong giai ma con song hay khong.
 *
 * Phan doc anh tu may anh nam o [QuetQrActivity].
 */
object QuetQr {

    /**
     * Chi doc QR, khong doc barcode 1 chieu.
     *
     * Bat het moi dinh dang thi moi khung hinh phai thu qua chuc bo giai ma —
     * may yeu se giat. Nguoi dung hoi may quet QR, nen chi lam dung viec do.
     */
    private val GOI_Y = mapOf<DecodeHintType, Any>(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        // Anh tu may anh thuong mo, nghieng — chiu ton them chut thoi gian de
        // bat duoc ma xau con hon quet mai khong ra.
        DecodeHintType.TRY_HARDER to true,
        // BAT BUOC voi tieng Viet. Ma QR khong bat buoc mang danh dau bang ma
        // (ECI); thieu no thi zxing doan mac dinh la ISO-8859-1, va chu nao
        // khong co trong bang do se thanh dau hoi:
        //     "Họp nhóm thứ Tư"  ->  "H?p nhóm th? T?"
        // (`ó`/`ú` sot lai vi Latin-1 co, con `ọ`/`ứ`/`ư` thi khong.)
        // Ma nao co ECI that thi ECI van thang, nen dat mac dinh UTF-8 khong
        // lam hong ma nuoc ngoai.
        DecodeHintType.CHARACTER_SET to "UTF-8",
    )

    private val doc = MultiFormatReader().apply { setHints(GOI_Y) }

    /**
     * Giai ma mot khung hinh dang **kenh sang** (Y cua YUV_420_888).
     *
     * @param sang mang kenh sang, moi byte mot diem anh
     * @param rong / [cao] kich thuoc that cua khung
     * @return noi dung ma, hoac null neu khung nay khong co ma nao
     */
    @Synchronized
    fun giaiMa(sang: ByteArray, rong: Int, cao: Int): String? {
        if (rong <= 0 || cao <= 0 || sang.size < rong * cao) return null
        return try {
            val nguon = PlanarYUVLuminanceSource(sang, rong, cao, 0, 0, rong, cao, false)
            doc.decodeWithState(BinaryBitmap(HybridBinarizer(nguon))).text?.trim()
        } catch (_: Exception) {
            // Khung khong co ma la chuyen binh thuong, xay ra vai chuc lan moi
            // giay. KHONG log gi o day.
            null
        } finally {
            doc.reset()
        }
    }

    // ---- Phan loai noi dung ----

    /** Ma QR chua gi, de man hinh biet nen bay nut nao. */
    enum class Loai { LIEN_KET, WIFI, DIEN_THOAI, EMAIL, DANH_THIEP, CHU }

    /**
     * Doan xem noi dung la gi.
     *
     * Chi de chon nut cho tien tay, khong phai de tin tuong: nut "Mo" luon bat
     * nguoi dung nhin dia chi truoc roi moi bam.
     */
    fun loaiCua(noiDung: String): Loai {
        val s = noiDung.trim()
        val thap = s.lowercase()
        return when {
            thap.startsWith("wifi:") -> Loai.WIFI
            thap.startsWith("tel:") -> Loai.DIEN_THOAI
            thap.startsWith("mailto:") || thap.startsWith("smtp:") -> Loai.EMAIL
            thap.startsWith("begin:vcard") || thap.startsWith("mecard:") -> Loai.DANH_THIEP
            laLienKet(s) -> Loai.LIEN_KET
            else -> Loai.CHU
        }
    }

    /**
     * Co phai dia chi web mo duoc khong.
     *
     * CHI nhan `http`/`https`. Cac lugc do khac (`intent:`, `file:`, `content:`,
     * `javascript:`) co the bi dung de mo thang mot man hinh trong app khac hoac
     * doc tep noi bo — ma nguoi dung thi chi thay mot o vuong den tren tuong.
     * Nhung thu do de nguyen dang chu, muon dung thi tu sao chep.
     */
    fun laLienKet(noiDung: String): Boolean {
        val s = noiDung.trim()
        if (s.contains(' ') || s.contains('\n')) return false
        val thap = s.lowercase()
        if (!thap.startsWith("http://") && !thap.startsWith("https://")) return false
        // Phai co ten mien thuc su sau lugc do.
        val sau = s.substringAfter("//", "")
        val mien = sau.substringBefore('/').substringBefore('?').substringBefore('#')
        return mien.isNotEmpty() && mien.contains('.') && !mien.startsWith('.') && !mien.endsWith('.')
    }

    /**
     * Ten mien de hien to cho nguoi dung nhin truoc khi bam Mo.
     *
     * Day la thu duy nhat dang tin trong mot dia chi — phan con lai co the dai
     * loe loet de day ten mien that ra khoi tam mat.
     */
    fun tenMien(lienKet: String): String {
        val sau = lienKet.trim().substringAfter("//", "")
        val mien = sau.substringBefore('/').substringBefore('?').substringBefore('#')
        // Bo phan "user:mat_khau@" — kieu nguy trang co dien:
        // https://google.com@ke-gian.example
        return mien.substringAfterLast('@').ifEmpty { mien }
    }

    /** Rut gon de hien tren danh sach lich su. */
    fun rutGon(noiDung: String, toiDa: Int = 90): String {
        val s = noiDung.trim().replace('\n', ' ')
        return if (s.length <= toiDa) s else s.take(toiDa - 1) + "…"
    }
}
