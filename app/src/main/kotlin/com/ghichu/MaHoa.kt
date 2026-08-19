package com.ghichu

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Chuoi ma hoa hong, hoac mat khau sai. */
class LoiMaHoa(message: String) : Exception(message)

/**
 * Ma hoa noi dung ghi chu rieng tu.
 *
 * Ban da chot: ghi chu rieng tu phai **ma hoa that**, khong phai chi giau khoi
 * danh sach. App Ghi chu cua Xiaomi chi giau (thu muc he thong `_id = -4`) — ai
 * cam duoc may mo khoa, hoac doc thang tep CSDL, la thay het.
 *
 * CACH LAM
 *   * Khoa sinh tu mat khau bang **PBKDF2-HMAC-SHA256, [SO_VONG] vong**. Nhieu
 *     vong lam cham nguoi do mat khau; [SO_VONG] mat khoang mot phan tu giay tren
 *     dien thoai doi nay — nguoi dung khong thay cham, ma do thu thi cham gap
 *     hang tram nghin lan so voi bam SHA mot phat.
 *   * Ma hoa bang **AES-256-GCM**. GCM vua giau vua CHONG SUA: doi mot byte
 *     trong CSDL la giai ra loi chu khong ra chu rac — nen ghi chu hong thi biet
 *     ngay thay vi doc phai thu bia.
 *   * **IV moi lan mot khac**, sinh ngau nhien. Dung lai IV voi cung mot khoa la
 *     pha vo GCM hoan toan, khong phai lam yeu di mot chut.
 *
 * KHONG LAM: khong luu mat khau o bat cu dau. Chi luu muoi va mot **the kiem**
 * (chuoi biet truoc da ma hoa) de biet nguoi dung go dung mat khau chua. Mat mat
 * khau la mat ghi chu rieng tu — noi ro cho nguoi dung truoc khi ho dat.
 *
 * Ca lop nay khong dung API Android nao (javax.crypto co san ca tren JVM) nen
 * kiem thu duoc ma khong can dien thoai.
 */
object MaHoa {

    private const val SO_VONG = 210_000
    private const val DAI_KHOA = 256
    private const val DAI_MUOI = 16
    private const val DAI_IV = 12
    private const val DAI_THE = 128

    /** Dau nhan dang truoc chuoi da ma hoa — de con doi cach lam ve sau. */
    private const val DAU = "gc1:"

    /** Chuoi biet truoc, ma hoa len de kiem mat khau. */
    private const val CAU_KIEM = "ghi chu rieng tu"

    private val nguon = SecureRandom()

    // ------------------------------------------------------------ khoa

    fun muoiMoi(): ByteArray = ByteArray(DAI_MUOI).also { nguon.nextBytes(it) }

    /**
     * Sinh khoa tu mat khau. **Cham co chu y** — dung goi tren luong chinh.
     */
    fun khoa(matKhau: String, muoi: ByteArray): SecretKey {
        val spec = PBEKeySpec(matKhau.toCharArray(), muoi, SO_VONG, DAI_KHOA)
        val bam = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
        return SecretKeySpec(bam.encoded, "AES")
    }

    // ------------------------------------------------------------ ma hoa

    fun maHoa(chu: String, khoa: SecretKey): String {
        if (chu.isEmpty()) return ""
        val iv = ByteArray(DAI_IV).also { nguon.nextBytes(it) }
        val may = Cipher.getInstance("AES/GCM/NoPadding")
        may.init(Cipher.ENCRYPT_MODE, khoa, GCMParameterSpec(DAI_THE, iv))
        val ra = may.doFinal(chu.toByteArray(Charsets.UTF_8))
        return DAU + Base64.getEncoder().encodeToString(iv + ra)
    }

    /**
     * Giai ma. Nem [LoiMaHoa] neu sai khoa hoac du lieu bi sua.
     *
     * Chuoi khong mang dau [DAU] thi tra ve nguyen van: ghi chu duoc dat rieng tu
     * SAU khi da viet thi phan cu con o dang chu thuong cho toi lan luu ke tiep.
     */
    fun giaiMa(chu: String, khoa: SecretKey): String {
        if (chu.isEmpty()) return ""
        if (!chu.startsWith(DAU)) return chu
        return try {
            val goi = Base64.getDecoder().decode(chu.substring(DAU.length))
            if (goi.size <= DAI_IV) throw LoiMaHoa("Dữ liệu quá ngắn")
            val may = Cipher.getInstance("AES/GCM/NoPadding")
            may.init(
                Cipher.DECRYPT_MODE, khoa,
                GCMParameterSpec(DAI_THE, goi, 0, DAI_IV),
            )
            String(may.doFinal(goi, DAI_IV, goi.size - DAI_IV), Charsets.UTF_8)
        } catch (e: LoiMaHoa) {
            throw e
        } catch (_: Exception) {
            // Moi loi cua thu vien deu quy ve mot cau — chi tiet loi mat ma khong
            // giup gi nguoi dung, ma no ro ri thong tin cho nguoi do.
            throw LoiMaHoa("Sai mật khẩu hoặc dữ liệu hỏng")
        }
    }

    fun daMaHoa(chu: String): Boolean = chu.startsWith(DAU)

    // ------------------------------------------------------------ the kiem

    fun theKiem(khoa: SecretKey): String = maHoa(CAU_KIEM, khoa)

    fun dungMatKhau(the: String, khoa: SecretKey): Boolean = try {
        giaiMa(the, khoa) == CAU_KIEM
    } catch (_: LoiMaHoa) {
        false
    }
}
