package com.ghichu

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Gemini gui JSON **null** cho truong khong co.
 *
 * ĐỌC KỸ TRƯỚC KHI SỬA: bai test trong tep nay KHONG the tu bat lai loi goc,
 * vi hai ban `org.json` cu xu khac nhau —
 *
 *   Android (may that)          `optString("title")` voi `{"title":null}` -> "null"
 *   org.json:json (test o day)  cung truong hop                           -> ""
 *
 * Nghia la neu ai do lo quay ve goi thang `optString`, may that se sinh su kien
 * mang tieu de la chu "null" con o day van xanh. Vi vay:
 *
 *   1. [AiDocDeXuat.chuoi] hoi `isNull()` truoc — ham nay giong nhau o ca hai ban.
 *   2. Bai [chuoiChanDuocJsonNull] chot HANH VI cua ham do.
 *   3. Cac bai con lai chot ket qua cuoi khi model gui null tran lan.
 *
 * Muon kiem chung tren may that thi bat AI tao su kien roi xem tieu de co phai
 * chu "null" khong.
 */
class AiJsonNullTest {

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    @Test
    fun chuoiChanDuocJsonNull() {
        val o = JSONObject("""{"co":"Họp nhóm","rong":"","khong_co_gi":null}""")

        assertEquals("Họp nhóm", AiDocDeXuat.chuoi(o, "co"))
        assertEquals("", AiDocDeXuat.chuoi(o, "rong"))
        assertEquals("", AiDocDeXuat.chuoi(o, "khong_co_gi"))
        // Truong vang mat han cung phai ra rong, khong duoc nem.
        assertEquals("", AiDocDeXuat.chuoi(o, "chua_tung_co"))
    }

    @Test
    fun chuoiCatKhoangTrangHaiDau() {
        val o = JSONObject("""{"t":"  Đi chợ  "}""")
        assertEquals("Đi chợ", AiDocDeXuat.chuoi(o, "t"))
    }

    @Test
    fun suKienMoiCoTruongNullVanDungDuoc() {
        // Model hay gui du bo khung roi de null cho truong khong biet.
        val d = AiDocDeXuat.doc(
            """
            {"reply":"Đã thêm.","intent":"update_calendar",
             "events":[{"title":"Họp nhóm","date":"2026-08-12","end_date":null,
                        "start":"14:00","end":null,"all_day":false,
                        "repeat":null,"repeat_until":null,"remind_minutes":null}]}
            """.trimIndent(),
            zone,
        )

        assertEquals(1, d.themSuKien.size)
        val s = d.themSuKien[0]
        assertEquals("Họp nhóm", s.tieuDe)
        // `end` null -> mac dinh dai mot tieng, khong phai loi.
        assertEquals(3_600_000L, s.ketThuc - s.batDau)
        assertEquals(SuKien.LAP_KHONG, s.lapLai)
        assertNull(s.lapDen)
        assertEquals(-1, s.nhacTruoc)
    }

    @Test
    fun tieuDeNullThiRongChuKhongPhaiChuNull() {
        val d = AiDocDeXuat.doc(
            """
            {"reply":"ok","intent":"update_calendar",
             "events":[{"title":null,"date":"2026-08-12","start":"09:00"}]}
            """.trimIndent(),
            zone,
        )

        assertEquals(1, d.themSuKien.size)
        assertEquals("", d.themSuKien[0].tieuDe)
    }

    @Test
    fun thaoTacSuaCoTruongNullThiCoiNhuKhongGui() {
        val o = JSONObject("""{"id":7,"title":null,"start":"20:00","date":null}""")
        val op = AiDocDeXuat.docThaoTacSua(o, zone)

        assertNotNull(op)
        assertNull("title null phai coi la khong gui", op!!.tieuDe)
        assertNull("date null phai coi la khong gui", op.ngay)
        assertEquals("20:00", op.batDau)
    }

    @Test
    fun loiDapNullThiLuiVeNguyenVan() {
        val raw = """{"reply":null,"intent":"chat"}"""
        val d = AiDocDeXuat.doc(raw, zone)
        // Khong duoc hien chu "null" cho nguoi dung.
        assertEquals(raw, d.loiDap)
    }

    @Test
    fun ydinhNullThiCoiLaTroChuyen() {
        val d = AiDocDeXuat.doc("""{"reply":"Chào bạn","intent":null}""", zone)
        assertEquals(YDinh.CHAT, d.yDinh)
        assertEquals("Chào bạn", d.loiDap)
    }

    // ---- Mui gio truyen xuong, khong lay gio may ----

    @Test
    fun thaoTacSuaDungDungMuiGioDuocTruyenVao() {
        val o = JSONObject("""{"id":3,"repeat_until":"2026-11-09"}""")

        val vn = AiDocDeXuat.docThaoTacSua(o, ZoneId.of("Asia/Ho_Chi_Minh"))!!
        val utc = AiDocDeXuat.docThaoTacSua(o, ZoneId.of("UTC"))!!

        val mongDoiVN = LocalDate.of(2026, 11, 9)
            .atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().toEpochMilli()
        val mongDoiUTC = LocalDate.of(2026, 11, 9)
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

        assertEquals(mongDoiVN, vn.lapDen)
        assertEquals(mongDoiUTC, utc.lapDen)
        // Lech dung 7 tieng — chung to mui gio that su di theo tham so.
        assertEquals(7 * 3_600_000L, mongDoiUTC - mongDoiVN)
    }
}
