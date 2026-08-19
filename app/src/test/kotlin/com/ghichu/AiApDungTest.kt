package com.ghichu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Gop thao tac sua cua AI vao su kien da co.
 *
 * Diem mau chot: **truong AI khong gui thi phai GIU NGUYEN**. Nguoi dung noi
 * "dời sang 8h tối" ma ta reset luon ngay, do dai, mau, lap lai ve mac dinh thi
 * la pha du lieu cua ho.
 */
class AiApDungTest {

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    private fun moc(nam: Int, thang: Int, ngay: Int, gio: Int = 0, phut: Int = 0): Long =
        LocalDateTime.of(nam, thang, ngay, gio, phut).atZone(zone).toInstant().toEpochMilli()

    /** Hop nhom: 10/8/2026 14:00–15:30, lap hang tuan, nhac truoc 15'. */
    private val cu = SuKien(
        id = 7,
        tieuDe = "Họp nhóm",
        ghiChu = "phòng A",
        batDau = moc(2026, 8, 10, 14, 0),
        ketThuc = moc(2026, 8, 10, 15, 30),
        mau = 3,
        lapLai = SuKien.LAP_TUAN,
        nhacTruoc = 15,
        diaDiem = "Tầng 4",
        lapDen = moc(2026, 12, 31),
    )

    @Test
    fun chiDoiGioThiNgayVaDoDaiGiuNguyen() {
        val moi = AiApDung.gop(cu, SuaSuKienOp(id = 7, batDau = "20:00"), zone)

        assertEquals(moc(2026, 8, 10, 20, 0), moi.batDau)
        assertEquals(90 * 60_000L, moi.ketThuc - moi.batDau)
    }

    @Test
    fun chiDoiNgayThiGioGiuNguyen() {
        val moi = AiApDung.gop(cu, SuaSuKienOp(id = 7, ngay = java.time.LocalDate.of(2026, 8, 14)), zone)

        assertEquals(moc(2026, 8, 14, 14, 0), moi.batDau)
        assertEquals(moc(2026, 8, 14, 15, 30), moi.ketThuc)
    }

    @Test
    fun truongKhongGuiThiKhongDungToi() {
        val moi = AiApDung.gop(cu, SuaSuKienOp(id = 7, batDau = "20:00"), zone)

        assertEquals("Họp nhóm", moi.tieuDe)
        assertEquals("phòng A", moi.ghiChu)
        assertEquals("Tầng 4", moi.diaDiem)
        assertEquals(3, moi.mau)
        assertEquals(SuKien.LAP_TUAN, moi.lapLai)
        assertEquals(15, moi.nhacTruoc)
        assertEquals(moc(2026, 12, 31), moi.lapDen)
    }

    @Test
    fun doiTieuDe() {
        val moi = AiApDung.gop(cu, SuaSuKienOp(id = 7, tieuDe = "Họp ban chấp hành"), zone)

        assertEquals("Họp ban chấp hành", moi.tieuDe)
        assertEquals(cu.batDau, moi.batDau)
    }

    @Test
    fun guiCaGioBatDauVaKetThucThiTheoDungCaHai() {
        val moi = AiApDung.gop(cu, SuaSuKienOp(id = 7, batDau = "08:00", ketThuc = "09:15"), zone)

        assertEquals(moc(2026, 8, 10, 8, 0), moi.batDau)
        assertEquals(moc(2026, 8, 10, 9, 15), moi.ketThuc)
    }

    @Test
    fun gioKetThucSomHonBatDauThiLuiVeGiuDoDaiCu() {
        val moi = AiApDung.gop(cu, SuaSuKienOp(id = 7, batDau = "20:00", ketThuc = "09:00"), zone)

        assertTrue(moi.ketThuc > moi.batDau)
        assertEquals(90 * 60_000L, moi.ketThuc - moi.batDau)
    }

    @Test
    fun tatNhacPhaiPhanBietVoiKhongGui() {
        val tat = AiApDung.gop(cu, SuaSuKienOp(id = 7, coNhacTruoc = true, nhacTruoc = -1), zone)
        val khongDung = AiApDung.gop(cu, SuaSuKienOp(id = 7, batDau = "20:00"), zone)

        assertEquals(-1, tat.nhacTruoc)
        assertEquals(15, khongDung.nhacTruoc)
    }

    @Test
    fun tatLapVaBoMocDungLap() {
        val moi = AiApDung.gop(
            cu,
            SuaSuKienOp(id = 7, lapLai = SuKien.LAP_KHONG, coLapDen = true, lapDen = null),
            zone,
        )

        assertEquals(SuKien.LAP_KHONG, moi.lapLai)
        assertNull(moi.lapDen)
    }

    @Test
    fun giuNguyenIdDeGhiDeChuKhongTaoBanMoi() {
        // Sua ma sinh id moi thi lich se co hai ban trung nhau.
        val moi = AiApDung.gop(cu, SuaSuKienOp(id = 7, batDau = "20:00"), zone)

        assertEquals(7L, moi.id)
    }
}
