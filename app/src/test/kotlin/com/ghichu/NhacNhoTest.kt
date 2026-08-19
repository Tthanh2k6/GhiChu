package com.ghichu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Kiem thu phan tinh gio nhac tren JVM.
 *
 * Cho de sai nhat la su kien LAP LAI co nhac truoc: phai tim su kien trong
 * khoang da DOI VE PHIA SAU mot khoang bang thoi gian nhac truoc, khong phai
 * khoang goc. Lam sai thi nhac nho im lang dung nhung lan giao ranh — ma khong
 * co gi bao loi.
 */
class NhacNhoTest {

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    private fun moc(ngay: Int, gio: Int, phut: Int = 0): Long =
        NgayVN.mocCua(LocalDate.of(2026, 8, ngay), gio, phut, zone)

    private fun ngay(n: Int): Long = 24L * 60 * 60 * 1000 * n

    @Test
    fun `su kien khong dat nhac thi khong sinh lan nhac nao`() {
        val sk = SuKien(id = 1, tieuDe = "Họp", batDau = moc(20, 14), ketThuc = moc(20, 15))
        val ra = NhacNho.sapToi(listOf(sk), emptyList(), moc(19, 0), moc(21, 0), zone)
        assertTrue(ra.isEmpty())
    }

    @Test
    fun `nhac truoc 15 phut thi moc nhac la truoc gio bat dau 15 phut`() {
        val sk = SuKien(
            id = 1, tieuDe = "Họp", batDau = moc(20, 14), ketThuc = moc(20, 15), nhacTruoc = 15,
        )
        val ra = NhacNho.sapToi(listOf(sk), emptyList(), moc(19, 0), moc(21, 0), zone)
        assertEquals(1, ra.size)
        assertEquals(moc(20, 13, 45), ra[0].luc)
        assertEquals("sk:1", ra[0].khoa)
        assertTrue(ra[0].phu.startsWith("Còn 15 phút"))
    }

    @Test
    fun `su kien lap hang ngay sinh mot lan nhac moi ngay`() {
        val sk = SuKien(
            id = 1, tieuDe = "Uống thuốc", batDau = moc(1, 8), ketThuc = moc(1, 8, 15),
            lapLai = SuKien.LAP_NGAY, nhacTruoc = 10,
        )
        val ra = NhacNho.sapToi(listOf(sk), emptyList(), moc(10, 0), moc(13, 0), zone)
        assertEquals(3, ra.size)
        assertEquals(moc(10, 7, 50), ra[0].luc)
        assertEquals(moc(12, 7, 50), ra[2].luc)
    }

    @Test
    fun `moc nhac ngay truoc dau khoang van duoc tinh vao`() {
        // Su kien bat dau 00:05 ngay 20, nhac truoc 30 phut -> moc nhac 23:35
        // ngay 19. Hoi tu 23:30 ngay 19 thi PHAI thay, du su kien nam ngoai khoang.
        val sk = SuKien(
            id = 7, tieuDe = "Tàu chạy", batDau = moc(20, 0, 5), ketThuc = moc(20, 1),
            nhacTruoc = 30,
        )
        val ra = NhacNho.sapToi(listOf(sk), emptyList(), moc(19, 23, 30), moc(19, 23, 59), zone)
        assertEquals(1, ra.size)
        assertEquals(moc(19, 23, 35), ra[0].luc)
    }

    @Test
    fun `ghi chu dat gio sinh dung mot lan nhac`() {
        val g = GhiChu(id = 5, chuTron = "Gọi cho Thành\nsố mới", nhacLuc = moc(20, 9))
        val ra = NhacNho.sapToi(emptyList(), listOf(g), moc(19, 0), moc(21, 0), zone)
        assertEquals(1, ra.size)
        assertEquals("gc:5", ra[0].khoa)
        assertEquals("Gọi cho Thành", ra[0].tieuDe)
        assertEquals("số mới", ra[0].phu)
    }

    @Test
    fun `ghi chu trong thung rac khong nhac nua`() {
        val g = GhiChu(id = 5, chuTron = "Gọi Thành", nhacLuc = moc(20, 9), daXoa = true)
        val ra = NhacNho.sapToi(emptyList(), listOf(g), moc(19, 0), moc(21, 0), zone)
        assertTrue(ra.isEmpty())
    }

    @Test
    fun `nhieu nguon thi tron chung va xep theo thoi gian`() {
        val sk = SuKien(
            id = 1, tieuDe = "Họp", batDau = moc(20, 14), ketThuc = moc(20, 15), nhacTruoc = 0,
        )
        val g = GhiChu(id = 2, chuTron = "Mua sữa", nhacLuc = moc(20, 9))
        val ra = NhacNho.sapToi(listOf(sk), listOf(g), moc(19, 0), moc(21, 0), zone)
        assertEquals(listOf("gc:2", "sk:1"), ra.map { it.khoa })
        assertTrue(ra[1].phu.startsWith("Bắt đầu lúc"))
    }

    @Test
    fun `nhan nhac truoc doc duoc voi moc tron gio`() {
        val sk = SuKien(
            id = 1, tieuDe = "Bay", batDau = moc(20, 14), ketThuc = moc(20, 16), nhacTruoc = 120,
        )
        val ra = NhacNho.sapToi(listOf(sk), emptyList(), moc(19, 0), moc(21, 0), zone)
        assertTrue(ra[0].phu.startsWith("Còn 2 giờ"))
    }

    @Test
    fun `su kien khong tieu de van co chu de hien`() {
        val sk = SuKien(id = 1, batDau = moc(20, 14), ketThuc = moc(20, 15), nhacTruoc = 0)
        val ra = NhacNho.sapToi(listOf(sk), emptyList(), moc(19, 0), moc(21, 0), zone)
        assertEquals("Sự kiện", ra[0].tieuDe)
    }

    @Test
    fun `lap lai co ngay ket thuc thi khong nhac qua ngay do`() {
        val sk = SuKien(
            id = 1, tieuDe = "Học", batDau = moc(1, 8), ketThuc = moc(1, 9),
            lapLai = SuKien.LAP_NGAY, nhacTruoc = 0, lapDen = moc(3, 8),
        )
        val ra = NhacNho.sapToi(listOf(sk), emptyList(), moc(1, 0), moc(1, 0) + ngay(10), zone)
        assertEquals(3, ra.size)
        assertEquals(moc(3, 8), ra.last().luc)
    }
}
