package com.ghichu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Kiem thu phan LOI cua the ghi chu tren JVM — khong can dien thoai.
 *
 * Doc/ghi dinh dang nam o [DinhDangTest]; o day chi con hai nhom:
 *   * [GhiChu.tieuDeHien] / [xemTruoc] — sai la the ghi chu lap lai dong dau
 *     hai lan;
 *   * [NgayVN.nhanDanhSach] — sai la moi ghi chu deu hien ngay hom nay.
 */
class GhiChuTest {

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    // ---- tieu de va xem truoc ----

    @Test
    fun `chua dat tieu de thi lay dong dau, than bai bo dong do di`() {
        val g = GhiChu(chuTron = "Họp nhóm\n14h phòng A\nNhớ mang laptop")
        assertEquals("Họp nhóm", g.tieuDeHien())
        assertEquals("14h phòng A\nNhớ mang laptop", g.xemTruoc())
    }

    @Test
    fun `da dat tieu de thi than bai giu nguyen ca dong dau`() {
        val g = GhiChu(tieuDe = "Việc tuần này", chuTron = "Họp nhóm\n14h phòng A")
        assertEquals("Việc tuần này", g.tieuDeHien())
        assertEquals("Họp nhóm\n14h phòng A", g.xemTruoc())
    }

    @Test
    fun `ghi chu rong khong bia ra tieu de`() {
        val g = GhiChu()
        assertEquals("", g.tieuDeHien())
        assertTrue(g.rong)
    }

    // ---- nhan ngay tren the ----

    @Test
    fun `trong bay ngay thi hien thu va gio`() {
        val homNay = LocalDate.of(2026, 8, 19) // thu tu
        val luc = NgayVN.mocCua(LocalDate.of(2026, 8, 17), 22, 47, zone)
        assertEquals("T2 22:47", NgayVN.nhanDanhSach(luc, homNay, zone))
    }

    @Test
    fun `cung nam nhung qua bay ngay thi hien ngay thang`() {
        val homNay = LocalDate.of(2026, 8, 19)
        val luc = NgayVN.mocCua(LocalDate.of(2026, 7, 12), 9, 0, zone)
        assertEquals("12 Tháng 7", NgayVN.nhanDanhSach(luc, homNay, zone))
    }

    @Test
    fun `khac nam thi hien ca nam`() {
        val homNay = LocalDate.of(2026, 8, 19)
        val luc = NgayVN.mocCua(LocalDate.of(2025, 1, 6), 9, 0, zone)
        assertEquals("6 Tháng 1, 2025", NgayVN.nhanDanhSach(luc, homNay, zone))
    }

    @Test
    fun `chu nhat viet tat la CN`() {
        val homNay = LocalDate.of(2026, 8, 19)
        val luc = NgayVN.mocCua(LocalDate.of(2026, 8, 16), 8, 5, zone)
        assertEquals("CN 08:05", NgayVN.nhanDanhSach(luc, homNay, zone))
    }

    @Test
    fun `dung ranh gioi bay ngay - ngay thu sau van la ngay thang`() {
        val homNay = LocalDate.of(2026, 8, 19)
        // 6 ngay truoc van con nam trong khoang "gan day"...
        val trong = NgayVN.mocCua(LocalDate.of(2026, 8, 13), 10, 0, zone)
        assertEquals("T5 10:00", NgayVN.nhanDanhSach(trong, homNay, zone))
        // ...con 7 ngay truoc thi da ra ngoai.
        val ngoai = NgayVN.mocCua(LocalDate.of(2026, 8, 12), 10, 0, zone)
        assertEquals("12 Tháng 8", NgayVN.nhanDanhSach(ngoai, homNay, zone))
    }
}
