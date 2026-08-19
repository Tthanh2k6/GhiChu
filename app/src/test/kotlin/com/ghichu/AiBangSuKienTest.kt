package com.ghichu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Cot `end_date` cua bang su kien gui cho AI.
 *
 * Truoc day cot nay LUON de rong du dong tieu de co khai. Hau qua khong lo ra
 * ngay: model nhin su kien keo dai 3 ngay tuong nhu su kien mot ngay, nen lenh
 * "doi chuyen di sang tuan sau" tra ve mot su kien bi cat con dung mot ngay.
 * May bai duoi day chot lai cho do.
 */
class AiBangSuKienTest {

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    private fun moc(nam: Int, thang: Int, ngay: Int, gio: Int = 0, phut: Int = 0): Long =
        LocalDateTime.of(nam, thang, ngay, gio, phut).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun suKienTrongMotNgayThiKhongCoNgayCuoi() {
        val s = SuKien(
            id = 1,
            tieuDe = "Họp nhóm",
            batDau = moc(2026, 8, 12, 14, 0),
            ketThuc = moc(2026, 8, 12, 15, 30),
        )

        assertEquals("", AiApDung.ngayCuoiCua(s, zone))
    }

    @Test
    fun suKienKeoDaiNhieuNgayThiCoNgayCuoi() {
        val s = SuKien(
            id = 2,
            tieuDe = "Đi Đà Lạt",
            batDau = moc(2026, 8, 12, 7, 0),
            ketThuc = moc(2026, 8, 15, 18, 0),
        )

        assertEquals("2026-08-15", AiApDung.ngayCuoiCua(s, zone))
    }

    /**
     * Su kien CA NGAY luu `ketThuc` la 00:00 cua ngay HOM SAU. Khong lui lai mot
     * mili-giay thi su kien ca ngay mot hom nao cung bi bao la keo dai hai ngay.
     */
    @Test
    fun caNgayMotHomThiVanKhongCoNgayCuoi() {
        val s = SuKien(
            id = 3,
            tieuDe = "Nghỉ lễ",
            batDau = moc(2026, 9, 2),
            ketThuc = moc(2026, 9, 3),
            caNgay = true,
        )

        assertEquals("", AiApDung.ngayCuoiCua(s, zone))
    }

    @Test
    fun caNgayNhieuHomThiLayDungHomCuoi() {
        val s = SuKien(
            id = 4,
            tieuDe = "Nghỉ Tết",
            batDau = moc(2027, 2, 6),
            ketThuc = moc(2027, 2, 11), // ca ngay -> het ngay 10
            caNgay = true,
        )

        assertEquals("2027-02-10", AiApDung.ngayCuoiCua(s, zone))
    }

    @Test
    fun ketThucLoanNgocThiKhongVo() {
        // Du lieu hong (ket thuc truoc bat dau) khong duoc lam vo ca luot hoi AI.
        val s = SuKien(
            id = 5,
            tieuDe = "Hỏng",
            batDau = moc(2026, 8, 12, 10, 0),
            ketThuc = moc(2026, 8, 10, 10, 0),
            caNgay = true,
        )

        assertEquals("", AiApDung.ngayCuoiCua(s, zone))
    }

    // ---- Bang CSV thuc su mang cot do ----

    @Test
    fun bangCsvDienNgayCuoiVaoDungCot() {
        val bang = listOf(
            AiLoiNhac.SuKienNgan(
                id = 9, tieuDe = "Đi Đà Lạt", ngay = "2026-08-12", batDau = "07:00",
                ketThuc = "18:00", caNgay = false, lapLai = "none", lapDen = "",
                nhacTruoc = -1, ngayCuoi = "2026-08-15",
            ),
        )

        val s = AiLoiNhac.loiNhac(LocalDate.of(2026, 8, 11), dsSuKien = bang)
        val dong = s.lineSequence().first { it.startsWith("9,") }

        // id,title,date,end_date,start,end,all_day,repeat,repeat_until,remind_minutes
        assertEquals(
            "9,Đi Đà Lạt,2026-08-12,2026-08-15,07:00,18:00,false,none,,",
            dong,
        )
    }

    @Test
    fun tieuDeCoDauPhayVanKhongVoBang() {
        val bang = listOf(
            AiLoiNhac.SuKienNgan(
                id = 10, tieuDe = "Học Toán, Lý", ngay = "2026-08-12", batDau = "07:00",
                ketThuc = "09:00", caNgay = false, lapLai = "weekly", lapDen = "2026-11-09",
                nhacTruoc = 15,
            ),
        )

        val s = AiLoiNhac.loiNhac(LocalDate.of(2026, 8, 11), dsSuKien = bang)
        val dong = s.lineSequence().first { it.startsWith("10,") }

        assertTrue(dong, dong.contains("\"Học Toán, Lý\""))
        assertTrue(dong, dong.endsWith(",weekly,2026-11-09,15"))
    }
}
