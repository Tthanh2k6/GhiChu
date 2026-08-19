package com.ghichu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Doc cau tra loi tho cua Gemini API.
 *
 * Cac ma loi o day KHONG phai doan: chung lay tu ban demo, noi da do bang API
 * that (404 khi model bi go, 429 khi het luot mien phi). Bai kiem thu giu cho
 * cau bao loi khong bi sua thanh vo nghia.
 */
class GeminiTest {

    private fun batLoi(viec: () -> Unit): String =
        try {
            viec()
            fail("dang le phai nem LoiGemini")
            ""
        } catch (e: LoiGemini) {
            e.message.orEmpty()
        }

    @Test
    fun docDuocCauTraLoiBinhThuong() {
        val than = """
            {"candidates":[{"content":{"parts":[{"text":"Xin chào"}]}}]}
        """.trimIndent()

        assertEquals("Xin chào", Gemini.docTraLoi(200, than))
    }

    @Test
    fun gopNhieuManhTraVeLamMot() {
        val than = """
            {"candidates":[{"content":{"parts":[{"text":"Xin "},{"text":"chào"}]}}]}
        """.trimIndent()

        assertEquals("Xin chào", Gemini.docTraLoi(200, than))
    }

    @Test
    fun khoaSaiThiNoiRoLaKhoaSai() {
        val loi = batLoi {
            Gemini.docTraLoi(400, """{"error":{"message":"API key not valid"}}""")
        }

        assertTrue(loi, loi.contains("API key"))
    }

    @Test
    fun model404ThiBaoDoiModel() {
        val loi = batLoi {
            Gemini.docTraLoi(404, """{"error":{"message":"not found"}}""", "gemini-2.5-pro")
        }

        assertTrue(loi, loi.contains("gemini-2.5-pro"))
        assertTrue(loi, loi.contains("chọn model khác"))
    }

    @Test
    fun het429ThiBaoChoMotPhut() {
        val loi = batLoi { Gemini.docTraLoi(429, """{"error":{"message":"quota"}}""") }

        assertTrue(loi, loi.contains("Hết lượt miễn phí"))
        assertTrue(loi, loi.contains("flash-lite"))
    }

    @Test
    fun khongCoUngVienThiNghiBiLocAnToan() {
        val loi = batLoi { Gemini.docTraLoi(200, """{"candidates":[]}""") }

        assertTrue(loi, loi.contains("bộ lọc an toàn"))
    }

    @Test
    fun noiDungRongCungCoiLaLoi() {
        val loi = batLoi {
            Gemini.docTraLoi(200, """{"candidates":[{"content":{"parts":[{"text":"   "}]}}]}""")
        }

        assertTrue(loi, loi.contains("rỗng"))
    }

    @Test
    fun thanKhongPhaiJsonThiBaoDocKhongDuoc() {
        val loi = batLoi { Gemini.docTraLoi(200, "<html>502 Bad Gateway</html>") }

        assertTrue(loi, loi.contains("không đọc được"))
    }

    // ---- Boc hang rao ma ----

    @Test
    fun bocDuocHangRaoMa() {
        assertEquals("""{"a":1}""", Gemini.bocJson("```json\n{\"a\":1}\n```"))
        assertEquals("""{"a":1}""", Gemini.bocJson("```\n{\"a\":1}\n```"))
    }

    @Test
    fun khongCoHangRaoThiGiuNguyen() {
        assertEquals("""{"a":1}""", Gemini.bocJson("""  {"a":1}  """))
    }

    @Test
    fun hangRaoNamGiuaBaiThiKhongDungVao() {
        // Chi boc khi no om TRON cau tra loi, khong thi an nham dau ``` von la
        // noi dung nguoi dung.
        val raw = "Xem đoạn này:\n```\nma nguon\n```\nvà đó là hết."

        assertEquals(raw, Gemini.bocJson(raw))
    }

    @Test
    fun modelMacDinhPhaiNamTrongDanhSach() {
        assertTrue(Gemini.MODEL_MAC_DINH in Gemini.MODEL)
    }

    // ---- Than yeu cau: hop dong voi Google ----

    @Test
    fun thanYeuCauDungTenTruongCuaGemini() {
        val than = Gemini.dungThan("xin chào", heThong = "bạn là trợ lý", lichSu = emptyList(), jsonMode = true)

        // Sai mot ten truong la ca tinh nang chet ma chi biet luc chay that.
        val noiDung = than.getJSONArray("contents")
        assertEquals(1, noiDung.length())
        assertEquals("user", noiDung.getJSONObject(0).getString("role"))
        assertEquals(
            "xin chào",
            noiDung.getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text"),
        )
        assertEquals(
            "bạn là trợ lý",
            than.getJSONObject("system_instruction").getJSONArray("parts")
                .getJSONObject(0).getString("text"),
        )
        assertEquals(
            "application/json",
            than.getJSONObject("generationConfig").getString("response_mime_type"),
        )
    }

    @Test
    fun khongBatJsonModeThiKhongGuiResponseMimeType() {
        val than = Gemini.dungThan("hỏi", heThong = null, lichSu = emptyList(), jsonMode = false)

        assertTrue(!than.getJSONObject("generationConfig").has("response_mime_type"))
        assertTrue(!than.has("system_instruction"))
    }

    @Test
    fun luotCuaNguoiDungLuonDungCUOISauLichSu() {
        // Gemini doc `contents` theo thu tu; dat truoc lich su la no tra loi cau cu.
        val lichSu = listOf(
            LuotChat(LuotChat.NGUOI, "câu cũ"),
            LuotChat(LuotChat.MAY, "đáp cũ"),
        )
        val than = Gemini.dungThan("câu mới", heThong = null, lichSu = lichSu, jsonMode = false)
        val noiDung = than.getJSONArray("contents")

        assertEquals(3, noiDung.length())
        assertEquals("câu cũ", noiDung.getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text"))
        assertEquals("model", noiDung.getJSONObject(1).getString("role"))
        assertEquals("câu mới", noiDung.getJSONObject(2).getJSONArray("parts").getJSONObject(0).getString("text"))
    }
}
