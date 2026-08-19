package com.ghichu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Loi nhac he thong gui cho Gemini.
 *
 * Khong the kiem tra AI tra loi "hay" hay khong bang test, nhung co the giu cho
 * mấy thu SAI LA CHET CHAC: ngay hom nay, dung cau truc JSON doi hoi, va bang su
 * kien co cot id de AI con tra ra dung id khi sua/xoa.
 */
class AiLoiNhacTest {

    private val homNay = LocalDate.of(2026, 8, 5) // thu Tu

    @Test
    fun coNgayHomNayVaThuDungDinhDang() {
        val s = AiLoiNhac.loiNhac(homNay)

        assertTrue(s, s.contains("2026-08-05"))
        assertTrue(s, s.contains("Thứ 4"))
    }

    @Test
    fun chuNhatGoiLaChuNhat() {
        val s = AiLoiNhac.loiNhac(LocalDate.of(2026, 8, 9))

        assertTrue(s, s.contains("Chủ nhật"))
    }

    @Test
    fun daySanCauTrucJsonPhaiTraVe() {
        val s = AiLoiNhac.loiNhac(homNay)

        for (khoa in listOf("reply", "intent", "note_title", "note_content",
                            "edit_target", "edit_mode", "events", "event_edits", "delete_ids")) {
            assertTrue("thiếu khoá $khoa", s.contains("\"$khoa\""))
        }
    }

    @Test
    fun keDuBonYDinh() {
        val s = AiLoiNhac.loiNhac(homNay)

        for (y in listOf("chat", "create_note", "edit_note", "update_calendar")) {
            assertTrue("thiếu ý định $y", s.contains(y))
        }
    }

    @Test
    fun khongCoDuLieuThiKhongDinhKemBangNao() {
        // Cum "BẢNG SỰ KIỆN" van nam trong phan huong dan tinh, nen phai kiem
        // dong TIEU DE CSV — do moi la dau hieu bang that su duoc dinh kem.
        val s = AiLoiNhac.loiNhac(homNay)

        assertTrue(s, !s.contains("DANH SÁCH GHI CHÚ HIỆN CÓ"))
        assertTrue(s, !s.contains("id,title,date"))
    }

    @Test
    fun dinhKemDanhSachGhiChu() {
        val s = AiLoiNhac.loiNhac(
            homNay,
            dsGhiChu = listOf(AiLoiNhac.GhiChuNgan("Đi chợ", "Trứng, sữa")),
        )

        assertTrue(s, s.contains("DANH SÁCH GHI CHÚ"))
        assertTrue(s, s.contains("- Đi chợ: Trứng, sữa"))
    }

    @Test
    fun bangSuKienCoCotIdDeAiTraRaDungId() {
        val s = AiLoiNhac.loiNhac(
            homNay,
            dsSuKien = listOf(
                AiLoiNhac.SuKienNgan(
                    id = 42, tieuDe = "Họp nhóm", ngay = "2026-08-10",
                    batDau = "14:00", ketThuc = "15:30", caNgay = false,
                    lapLai = "weekly", lapDen = "2026-12-31", nhacTruoc = 15,
                ),
            ),
        )

        assertTrue(s, s.contains("BẢNG SỰ KIỆN"))
        assertTrue(s, s.contains("id,title,date"))
        assertTrue(s, s.contains("42,Họp nhóm,2026-08-10"))
        assertTrue(s, s.contains("weekly,2026-12-31,15"))
    }

    @Test
    fun tieuDeCoDauPhayThiBocLaiKhongVoBangCsv() {
        val s = AiLoiNhac.loiNhac(
            homNay,
            dsSuKien = listOf(
                AiLoiNhac.SuKienNgan(
                    id = 1, tieuDe = "Họp, ăn trưa", ngay = "2026-08-10",
                    batDau = "", ketThuc = "", caNgay = true,
                    lapLai = "none", lapDen = "", nhacTruoc = -1,
                ),
            ),
        )

        assertTrue(s, s.contains("\"Họp, ăn trưa\""))
    }

    @Test
    fun khongNhacThiDeTrongChuKhongPhaiAmMot() {
        // -1 la quy uoc TRONG MAY. Gui "-1" len cho AI la no tuong nhac truoc
        // am mot phut.
        val s = AiLoiNhac.loiNhac(
            homNay,
            dsSuKien = listOf(
                AiLoiNhac.SuKienNgan(
                    id = 1, tieuDe = "A", ngay = "2026-08-10",
                    batDau = "09:00", ketThuc = "10:00", caNgay = false,
                    lapLai = "none", lapDen = "", nhacTruoc = -1,
                ),
            ),
        )

        assertTrue(s, s.contains("A,2026-08-10,,09:00,10:00,false,none,,\n"))
    }

    @Test
    fun catBotKhiQuaNhieuGhiChu() {
        val nhieu = (1..100).map { AiLoiNhac.GhiChuNgan("Ghi chú $it", "") }
        val s = AiLoiNhac.loiNhac(homNay, dsGhiChu = nhieu)

        assertTrue(s, s.contains("Ghi chú ${AiLoiNhac.TRAN_GHI_CHU}"))
        assertTrue(s, !s.contains("Ghi chú ${AiLoiNhac.TRAN_GHI_CHU + 1}"))
    }

    @Test
    fun giuQuyDoiTietHocIuh() {
        // Bang quy doi tiet -> gio la thu nguoi dung do tay o du an cu.
        val s = AiLoiNhac.loiNhac(homNay)

        assertTrue(s, s.contains("T7 12:30"))
        assertTrue(s, s.contains("T1 6:30"))
        assertEquals(true, s.contains("repeat=\"weekly\"") || s.contains("repeat=\"weekly\"") || s.contains("weekly"))
    }
}
