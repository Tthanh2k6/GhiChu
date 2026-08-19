package com.ghichu

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Doc cau tra loi cua Gemini.
 *
 * Day la cho de vo nhat: AI tra ve chu, ma chu thi khong co gi bao dam. Moi
 * nhanh o [AiDocDeXuat] deu phai co bai kiem thu — chay tren JVM, khong can may.
 */
class AiDocDeXuatTest {

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    private fun moc(nam: Int, thang: Int, ngay: Int, gio: Int = 0, phut: Int = 0): Long =
        LocalDateTime.of(nam, thang, ngay, gio, phut).atZone(zone).toInstant().toEpochMilli()

    // ------------------------------------------------------------ tro chuyen

    @Test
    fun troChuyenThuongThiKhongCoViecLam() {
        val d = AiDocDeXuat.doc("""{"reply":"Chào bạn!","intent":"chat"}""", zone)

        assertEquals("Chào bạn!", d.loiDap)
        assertEquals(YDinh.CHAT, d.yDinh)
        assertFalse(d.coViecLam)
    }

    @Test
    fun jsonHongThiVanDungDuoc() {
        // Khong vut di: hien nguyen van va cho tao ghi chu, khoi phi mot luot goi API.
        val d = AiDocDeXuat.doc("xin chao, day khong phai JSON", zone)

        assertEquals(YDinh.TAO_GHI_CHU, d.yDinh)
        assertEquals("xin chao, day khong phai JSON", d.noiDungGhiChu)
        assertTrue(d.coViecLam)
    }

    @Test
    fun bocDuocJsonNamTrongHangRaoMa() {
        val raw = "```json\n{\"reply\":\"ok\",\"intent\":\"chat\"}\n```"

        assertEquals("ok", AiDocDeXuat.doc(raw, zone).loiDap)
    }

    // ------------------------------------------------------------- ghi chu

    @Test
    fun taoGhiChu() {
        val d = AiDocDeXuat.doc(
            """{"reply":"Đã soạn xong","intent":"create_note",
                "note_title":"Đi chợ","note_content":"- [ ] Trứng\n- [ ] Sữa"}""",
            zone,
        )

        assertEquals(YDinh.TAO_GHI_CHU, d.yDinh)
        assertEquals("Đi chợ", d.tieuDeGhiChu)
        assertTrue(d.coViecLam)
    }

    @Test
    fun taoGhiChuMaRongNoiDungThiCoiNhuTroChuyen() {
        val d = AiDocDeXuat.doc(
            """{"reply":"Bạn muốn ghi gì?","intent":"create_note","note_content":""}""",
            zone,
        )

        assertEquals(YDinh.CHAT, d.yDinh)
        assertFalse(d.coViecLam)
    }

    @Test
    fun suaGhiChuPhanBietThemVaoVaVietDe() {
        val them = AiDocDeXuat.doc(
            """{"intent":"edit_note","edit_target":"Đi chợ","note_content":"Cà rốt","edit_mode":"append"}""",
            zone,
        )
        val de = AiDocDeXuat.doc(
            """{"intent":"edit_note","edit_target":"Đi chợ","note_content":"Cà rốt","edit_mode":"replace"}""",
            zone,
        )

        assertFalse(them.vietDe)
        assertTrue(de.vietDe)
        assertEquals("Đi chợ", them.timGhiChu)
    }

    // --------------------------------------------------------- them su kien

    @Test
    fun themSuKienCoGio() {
        val d = AiDocDeXuat.doc(
            """{"intent":"update_calendar","events":[
                {"title":"Họp nhóm","date":"2026-08-10","start":"14:00","end":"15:30"}]}""",
            zone,
        )

        assertEquals(1, d.themSuKien.size)
        val s = d.themSuKien[0]
        assertEquals("Họp nhóm", s.tieuDe)
        assertEquals(moc(2026, 8, 10, 14, 0), s.batDau)
        assertEquals(moc(2026, 8, 10, 15, 30), s.ketThuc)
        assertFalse(s.caNgay)
    }

    @Test
    fun khongCoGioKetThucThiMacDinhMotTieng() {
        val d = AiDocDeXuat.doc(
            """{"intent":"update_calendar","events":[
                {"title":"Cà phê","date":"2026-08-10","start":"09:00"}]}""",
            zone,
        )

        assertEquals(moc(2026, 8, 10, 10, 0), d.themSuKien[0].ketThuc)
    }

    @Test
    fun khongCoGioThiThanhSuKienCaNgay() {
        val d = AiDocDeXuat.doc(
            """{"intent":"update_calendar","events":[
                {"title":"Nghỉ lễ","date":"2026-09-02"}]}""",
            zone,
        )

        val s = d.themSuKien[0]
        assertTrue(s.caNgay)
        assertEquals(moc(2026, 9, 2), s.batDau)
        assertEquals(moc(2026, 9, 3), s.ketThuc)
    }

    @Test
    fun suKienKeoDaiNhieuNgay() {
        // "di lam tu 22 den 24 thang 8" — gio ket thuc phai gan vao NGAY ket thuc.
        val d = AiDocDeXuat.doc(
            """{"intent":"update_calendar","events":[
                {"title":"Đi làm","date":"2026-08-22","end_date":"2026-08-24","start":"08:00","end":"17:00"}]}""",
            zone,
        )

        assertEquals(moc(2026, 8, 22, 8, 0), d.themSuKien[0].batDau)
        assertEquals(moc(2026, 8, 24, 17, 0), d.themSuKien[0].ketThuc)
    }

    @Test
    fun caNgayNhieuNgayThiKeoToiHetNgayCuoi() {
        val d = AiDocDeXuat.doc(
            """{"intent":"update_calendar","events":[
                {"title":"Du lịch","date":"2026-08-22","end_date":"2026-08-24","all_day":true}]}""",
            zone,
        )

        assertEquals(moc(2026, 8, 22), d.themSuKien[0].batDau)
        assertEquals(moc(2026, 8, 25), d.themSuKien[0].ketThuc)
    }

    @Test
    fun docDuocLapLaiVaMocDungLap() {
        val d = AiDocDeXuat.doc(
            """{"intent":"update_calendar","events":[
                {"title":"Xử lí ảnh","date":"2026-08-08","start":"12:30","end":"15:00",
                 "repeat":"weekly","repeat_until":"2026-11-09"}]}""",
            zone,
        )

        val s = d.themSuKien[0]
        assertEquals(SuKien.LAP_TUAN, s.lapLai)
        assertEquals(moc(2026, 11, 9), s.lapDen)
    }

    @Test
    fun khongLapThiBoLuonMocDungLap() {
        // Moc dung lap chi co nghia khi that su co lap — de lai la du lieu rac.
        val d = AiDocDeXuat.doc(
            """{"intent":"update_calendar","events":[
                {"title":"A","date":"2026-08-08","start":"09:00","repeat":"none","repeat_until":"2026-11-09"}]}""",
            zone,
        )

        assertEquals(SuKien.LAP_KHONG, d.themSuKien[0].lapLai)
        assertNull(d.themSuKien[0].lapDen)
    }

    @Test
    fun nhacTruocNullThanhKhongNhac() {
        val d = AiDocDeXuat.doc(
            """{"intent":"update_calendar","events":[
                {"title":"A","date":"2026-08-08","start":"09:00","remind_minutes":null}]}""",
            zone,
        )
        val co = AiDocDeXuat.doc(
            """{"intent":"update_calendar","events":[
                {"title":"B","date":"2026-08-08","start":"09:00","remind_minutes":30}]}""",
            zone,
        )

        assertEquals(-1, d.themSuKien[0].nhacTruoc)
        assertEquals(30, co.themSuKien[0].nhacTruoc)
    }

    @Test
    fun ngayHongThiBoQuaSuKienDoChuKhongVoCaGoi() {
        val d = AiDocDeXuat.doc(
            """{"intent":"update_calendar","events":[
                {"title":"Hỏng","date":"hôm nay"},
                {"title":"Được","date":"2026-08-10","start":"09:00"}]}""",
            zone,
        )

        assertEquals(1, d.themSuKien.size)
        assertEquals("Được", d.themSuKien[0].tieuDe)
    }

    @Test
    fun nhieuSuKienTraVeDuKhongBiCatBot() {
        val muc = (1..12).joinToString(",") {
            """{"title":"Buổi $it","date":"2026-08-10","start":"08:00"}"""
        }
        val d = AiDocDeXuat.doc("""{"intent":"update_calendar","events":[$muc]}""", zone)

        assertEquals(12, d.themSuKien.size)
    }

    // ---------------------------------------------------------- sua / xoa

    @Test
    fun suaSuKienChiGiuTruongThucSuGui() {
        // "doi sang 8h toi" — chi gio doi, ngay va do dai giu nguyen.
        val op = AiDocDeXuat.docThaoTacSua(JSONObject("""{"id":12,"start":"20:00"}"""))!!

        assertEquals(12L, op.id)
        assertEquals("20:00", op.batDau)
        assertNull(op.ngay)
        assertNull(op.tieuDe)
        assertNull(op.caNgay)
        assertFalse(op.coNhacTruoc)
    }

    @Test
    fun phanBietKhongGuiVoiGuiGiaTriRong() {
        // Gui remind_minutes=null nghia la TAT nhac, khac han voi khong gui.
        val tat = AiDocDeXuat.docThaoTacSua(JSONObject("""{"id":1,"remind_minutes":null}"""))!!
        val khongDung = AiDocDeXuat.docThaoTacSua(JSONObject("""{"id":1,"start":"08:00"}"""))!!

        assertTrue(tat.coNhacTruoc)
        assertEquals(-1, tat.nhacTruoc)
        assertFalse(khongDung.coNhacTruoc)
    }

    @Test
    fun thieuIdThiBoQuaThaoTacSua() {
        assertNull(AiDocDeXuat.docThaoTacSua(JSONObject("""{"start":"20:00"}""")))
        assertNull(AiDocDeXuat.docThaoTacSua(JSONObject("""{"id":0,"start":"20:00"}""")))
    }

    @Test
    fun gopCaThemSuaXoaTrongMotCauTraLoi() {
        val d = AiDocDeXuat.doc(
            """{"intent":"update_calendar",
                "events":[{"title":"Mới","date":"2026-08-10","start":"09:00"}],
                "event_edits":[{"id":5,"start":"20:00"}],
                "delete_ids":[7,8]}""",
            zone,
        )

        assertEquals(1, d.themSuKien.size)
        assertEquals(1, d.suaSuKien.size)
        assertEquals(listOf(7L, 8L), d.xoaSuKien)
        assertTrue(d.coViecLam)
    }

    @Test
    fun lichKhongCoThaoTacNaoThiKhongCoViecLam() {
        val d = AiDocDeXuat.doc("""{"intent":"update_calendar","reply":"Bạn muốn dời cái nào?"}""", zone)

        assertFalse(d.coViecLam)
    }

    // ----------------------------------------------------- manh doc rieng le

    @Test
    fun docGioChanGiaTriVoLy() {
        assertEquals(9 to 30, AiDocDeXuat.docGio("09:30"))
        assertNull(AiDocDeXuat.docGio("25:00"))
        assertNull(AiDocDeXuat.docGio("09:70"))
        assertNull(AiDocDeXuat.docGio("9h30"))
        assertNull(AiDocDeXuat.docGio(""))
    }

    @Test
    fun docNgayChanGiaTriVoLy() {
        assertEquals(LocalDate.of(2026, 8, 10), AiDocDeXuat.docNgay("2026-08-10"))
        assertNull(AiDocDeXuat.docNgay(""))
        assertNull(AiDocDeXuat.docNgay("10/08/2026"))
        assertNull(AiDocDeXuat.docNgay("2026-13-45"))
    }

    @Test
    fun docLapLaiNhanCaTiengVietLanTiengAnh() {
        assertEquals(SuKien.LAP_NGAY, AiDocDeXuat.docLapLai("daily"))
        assertEquals(SuKien.LAP_TUAN, AiDocDeXuat.docLapLai("hàng tuần"))
        assertEquals(SuKien.LAP_THANG, AiDocDeXuat.docLapLai("monthly"))
        assertEquals(SuKien.LAP_KHONG, AiDocDeXuat.docLapLai("thỉnh thoảng"))
        assertEquals(SuKien.LAP_KHONG, AiDocDeXuat.docLapLai(null))
    }
}
