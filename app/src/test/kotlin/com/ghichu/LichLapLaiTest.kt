package com.ghichu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Trai su kien lap lai ra thanh tung lan xuat hien.
 *
 * Phan nay quan trong voi AI: nho AI xep thoi khoa bieu la no tao ra hang loat
 * su kien "lap hang tuan, den het hoc ky". Sai moc dung lap thi lich hien mai
 * mai, doi sang nam sau van thay tiet hoc cua ky truoc.
 */
class LichLapLaiTest {

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    private fun moc(nam: Int, thang: Int, ngay: Int, gio: Int = 0, phut: Int = 0): Long =
        LocalDateTime.of(nam, thang, ngay, gio, phut).atZone(zone).toInstant().toEpochMilli()

    private fun ngayCua(m: Long): LocalDate =
        java.time.Instant.ofEpochMilli(m).atZone(zone).toLocalDate()

    /** Buoi hoc thu Bay 8/8/2026, 12:30–15:00, lap hang tuan. */
    private fun buoiHoc(lapDen: Long? = null) = SuKien(
        id = 1,
        tieuDe = "Xử lí ảnh",
        batDau = moc(2026, 8, 8, 12, 30),
        ketThuc = moc(2026, 8, 8, 15, 0),
        lapLai = SuKien.LAP_TUAN,
        lapDen = lapDen,
    )

    @Test
    fun khongDatMocDungThiLapMai() {
        val lan = LichLapLai.moRong(
            buoiHoc(),
            moc(2027, 3, 1), moc(2027, 4, 1), zone,
        )

        assertTrue("sang nam sau van con lap", lan.isNotEmpty())
    }

    @Test
    fun quaMocDungThiHetLap() {
        // Hoc ky het ngay 9/11/2026 -> thang 12 khong con buoi nao.
        val lan = LichLapLai.moRong(
            buoiHoc(lapDen = moc(2026, 11, 9)),
            moc(2026, 12, 1), moc(2026, 12, 31), zone,
        )

        assertEquals(0, lan.size)
    }

    @Test
    fun buoiRoiDungNgayDungLapThiVanCon() {
        // "lap den 9/11" nghia la buoi ngay 9/11 VAN CON, du no bat dau luc 12:30
        // con moc luu la nua dem 9/11. So theo NGAY chu khong theo gio.
        val thu7 = SuKien(
            id = 1,
            tieuDe = "Buổi cuối",
            batDau = moc(2026, 11, 2, 12, 30),
            ketThuc = moc(2026, 11, 2, 15, 0),
            lapLai = SuKien.LAP_TUAN,
            lapDen = moc(2026, 11, 9),
        )

        val lan = LichLapLai.moRong(thu7, moc(2026, 11, 9), moc(2026, 11, 10), zone)

        assertEquals(1, lan.size)
        assertEquals(LocalDate.of(2026, 11, 9), ngayCua(lan[0].batDau))
    }

    @Test
    fun lapHangTuanRoiDungThuTrongTuan() {
        // 8/8/2026 la thu Bay -> moi lan xuat hien deu phai la thu Bay.
        val lan = LichLapLai.moRong(
            buoiHoc(lapDen = moc(2026, 11, 9)),
            moc(2026, 9, 1), moc(2026, 10, 1), zone,
        )

        assertTrue(lan.isNotEmpty())
        for (l in lan) {
            assertEquals(java.time.DayOfWeek.SATURDAY, ngayCua(l.batDau).dayOfWeek)
        }
    }

    @Test
    fun giuNguyenDoDaiMoiLanLap() {
        val lan = LichLapLai.moRong(buoiHoc(), moc(2026, 9, 1), moc(2026, 10, 1), zone)

        val daiGoc = moc(2026, 8, 8, 15, 0) - moc(2026, 8, 8, 12, 30)
        for (l in lan) assertEquals(daiGoc, l.ketThuc - l.batDau)
    }

    @Test
    fun suKienKhongLapChiHienDungKhoangCuaNo() {
        val mot = SuKien(
            id = 2,
            tieuDe = "Họp",
            batDau = moc(2026, 8, 10, 9, 0),
            ketThuc = moc(2026, 8, 10, 10, 0),
        )

        assertEquals(1, LichLapLai.moRong(mot, moc(2026, 8, 10), moc(2026, 8, 11), zone).size)
        assertEquals(0, LichLapLai.moRong(mot, moc(2026, 8, 11), moc(2026, 8, 12), zone).size)
    }

    @Test
    fun batDauTuRatLauTruocVanNhayDungToiKhoangDangXem() {
        // Su kien hang ngay tao tu nam ngoai — khong duoc lap 365 vong vo ich,
        // va van phai ra dung so lan trong khoang dang xem.
        val hangNgay = SuKien(
            id = 3,
            tieuDe = "Uống thuốc",
            batDau = moc(2025, 1, 1, 8, 0),
            ketThuc = moc(2025, 1, 1, 8, 30),
            lapLai = SuKien.LAP_NGAY,
        )

        val lan = LichLapLai.moRong(hangNgay, moc(2026, 8, 10), moc(2026, 8, 17), zone)

        assertEquals(7, lan.size)
    }
}
