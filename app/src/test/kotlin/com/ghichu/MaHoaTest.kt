package com.ghichu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiem thu ma hoa ghi chu rieng tu tren JVM.
 *
 * Chay duoc o day vi [MaHoa] co y khong dung API Android nao — `javax.crypto` co
 * san ca tren JVM. Kiem tren may tinh duoc thi moi lan sua co cho ma bat loi,
 * chu du lieu ma hoa hong thi khong con duong cuu.
 *
 * **Cham co chu y**: moi lan sinh khoa la 210.000 vong PBKDF2. Vi vay muoi va
 * khoa dung chung cho ca lop, chi rieng nhung phep can khoa KHAC moi sinh them.
 */
class MaHoaTest {

    private val muoi = MaHoa.muoiMoi()
    private val khoa = MaHoa.khoa("mật khẩu của tôi", muoi)

    @Test
    fun `ma hoa roi giai ma ra dung chu ban dau`() {
        val chu = "Số thẻ: 1234 5678 — đừng cho ai xem"
        assertEquals(chu, MaHoa.giaiMa(MaHoa.maHoa(chu, khoa), khoa))
    }

    @Test
    fun `chuoi da ma hoa khong con chua chu goc`() {
        val ra = MaHoa.maHoa("mật khẩu wifi nhà", khoa)
        assertFalse(ra.contains("wifi"))
        assertFalse(ra.contains("mật khẩu"))
        assertTrue(MaHoa.daMaHoa(ra))
    }

    @Test
    fun `ma hoa cung mot chu hai lan ra hai chuoi khac nhau`() {
        // IV moi lan mot khac. Neu hai lan ra y het thi IV dang bi dung lai —
        // dieu do pha vo GCM hoan toan, khong phai lam yeu di mot chut.
        val a = MaHoa.maHoa("họp 14h", khoa)
        val b = MaHoa.maHoa("họp 14h", khoa)
        assertNotEquals(a, b)
        assertEquals(MaHoa.giaiMa(a, khoa), MaHoa.giaiMa(b, khoa))
    }

    @Test
    fun `sai mat khau thi nem loi chu khong ra chu rac`() {
        val ra = MaHoa.maHoa("bí mật", khoa)
        val khoaKhac = MaHoa.khoa("mật khẩu khác", muoi)
        try {
            MaHoa.giaiMa(ra, khoaKhac)
            throw AssertionError("Phải ném LoiMaHoa mới đúng")
        } catch (_: LoiMaHoa) {
            // dung nhu mong doi
        }
    }

    @Test
    fun `sua mot ky tu trong chuoi ma hoa thi bi bat`() {
        // Day la cho GCM hon han cac che do chi giau ma khong chong sua: doi mot
        // byte trong CSDL phai ra LOI, chu khong ra mot doan chu rac ma nguoi
        // dung tuong la ghi chu that.
        val ra = MaHoa.maHoa("chuyển khoản 5 triệu", khoa)
        val hong = ra.dropLast(2) + if (ra.endsWith("A=")) "B=" else "A="
        try {
            MaHoa.giaiMa(hong, khoa)
            throw AssertionError("Phải ném LoiMaHoa mới đúng")
        } catch (_: LoiMaHoa) {
            // dung nhu mong doi
        }
    }

    @Test
    fun `muoi khac nhau thi cung mat khau ra khoa khac nhau`() {
        val muoi2 = MaHoa.muoiMoi()
        val khoa2 = MaHoa.khoa("mật khẩu của tôi", muoi2)
        val ra = MaHoa.maHoa("thử", khoa)
        try {
            MaHoa.giaiMa(ra, khoa2)
            throw AssertionError("Phải ném LoiMaHoa mới đúng")
        } catch (_: LoiMaHoa) {
            // dung nhu mong doi
        }
    }

    @Test
    fun `chuoi rong di qua khong doi`() {
        assertEquals("", MaHoa.maHoa("", khoa))
        assertEquals("", MaHoa.giaiMa("", khoa))
    }

    @Test
    fun `chu chua ma hoa thi tra ve nguyen van`() {
        // Ghi chu duoc dat rieng tu SAU khi da viet thi phan cu con o dang chu
        // thuong cho toi lan luu ke tiep — khong duoc nem loi vi no.
        assertEquals("chữ thường", MaHoa.giaiMa("chữ thường", khoa))
        assertFalse(MaHoa.daMaHoa("chữ thường"))
    }

    @Test
    fun `the kiem nhan dung mat khau va tu choi mat khau sai`() {
        val the = MaHoa.theKiem(khoa)
        assertTrue(MaHoa.dungMatKhau(the, khoa))
        assertFalse(MaHoa.dungMatKhau(the, MaHoa.khoa("sai rồi", muoi)))
    }

    @Test
    fun `giu duoc chu tieng Viet co dau va ky tu ngoai BMP`() {
        val chu = "Chuỗi 😀 với dấu tiếng Việt: ườ ẵ ỹ"
        assertEquals(chu, MaHoa.giaiMa(MaHoa.maHoa(chu, khoa), khoa))
    }
}
