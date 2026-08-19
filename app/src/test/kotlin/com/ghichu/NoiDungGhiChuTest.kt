package com.ghichu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Doi qua lai giua markup cua Xiaomi, chu tron, Markdown va Delta cu.
 *
 * Cho nay lam sai la **hong noi dung ghi chu that** cua nguoi dung, nen moi
 * duong deu phai co bai kiem thu.
 */
class NoiDungGhiChuTest {

    // ------------------------------------------------------------ chu tron

    @Test
    fun boTheLayChuTron() {
        val markup = "<text indent=\"1\">Dòng đầu</text>\n<text indent=\"1\">Dòng hai</text>"

        assertEquals("Dòng đầu\nDòng hai", NoiDungGhiChu.chuTron(markup))
    }

    @Test
    fun boiDauKyTuDacBiet() {
        assertEquals("a & b < c", NoiDungGhiChu.chuTron("<text>a &amp; b &lt; c</text>"))
    }

    @Test
    fun chuCuaOTickNamNgoaiThe() {
        val markup = "<input type=\"checkbox\" indent=\"1\" />Việc cần làm"

        assertEquals("Việc cần làm", NoiDungGhiChu.chuTron(markup))
    }

    // ------------------------------------------------------------ Markdown

    @Test
    fun oTickChuaTick() {
        val ra = NoiDungGhiChu.tuMarkdown("- [ ] Mua trứng")

        assertEquals("<input type=\"checkbox\" indent=\"1\" />Mua trứng", ra)
    }

    @Test
    fun oTickDaTick() {
        val ra = NoiDungGhiChu.tuMarkdown("- [x] Xong rồi")

        assertTrue(ra, ra.contains("checked=\"true\""))
        assertTrue(ra, ra.endsWith("Xong rồi"))
    }

    @Test
    fun gachDauDongThanhDongChuThuong() {
        // Markup cua Xiaomi khong co kieu "danh sach gach dau dong" rieng.
        assertEquals("<text indent=\"1\">Mua trứng</text>", NoiDungGhiChu.tuMarkdown("- Mua trứng"))
        assertEquals("<text indent=\"1\">Mua trứng</text>", NoiDungGhiChu.tuMarkdown("* Mua trứng"))
        assertEquals("<text indent=\"1\">Mua trứng</text>", NoiDungGhiChu.tuMarkdown("1. Mua trứng"))
    }

    @Test
    fun boDauThangCuaTieuDe() {
        assertEquals("<text indent=\"1\">Kế hoạch</text>", NoiDungGhiChu.tuMarkdown("## Kế hoạch"))
    }

    @Test
    fun goBoDanhDauTrongDongChuGiuChu() {
        // Nguoi dung da chot: khong bao gio thay `**` hay `#` tren man hinh.
        assertEquals("<text indent=\"1\">rất quan trọng</text>", NoiDungGhiChu.tuMarkdown("**rất quan trọng**"))
        assertEquals("<text indent=\"1\">nghiêng</text>", NoiDungGhiChu.tuMarkdown("*nghiêng*"))
        assertEquals("<text indent=\"1\">mã lệnh</text>", NoiDungGhiChu.tuMarkdown("`mã lệnh`"))
        assertEquals("<text indent=\"1\">bỏ</text>", NoiDungGhiChu.tuMarkdown("~~bỏ~~"))
    }

    @Test
    fun danhDauNamGiuaCauCungGoDuoc() {
        assertEquals(
            "<text indent=\"1\">nhớ mua sữa trước 5h</text>",
            NoiDungGhiChu.tuMarkdown("nhớ mua **sữa** trước 5h"),
        )
    }

    @Test
    fun dauSaoDonDocKhongLamHongCau() {
        // "2 * 3" khong phai chu nghieng.
        assertEquals("<text indent=\"1\">2 * 3 = 6</text>", NoiDungGhiChu.tuMarkdown("2 * 3 = 6"))
    }

    @Test
    fun thoatKyTuDacBietKhiGhiXuong() {
        val ra = NoiDungGhiChu.tuMarkdown("a < b & c")

        assertTrue(ra, ra.contains("a &lt; b &amp; c"))
    }

    @Test
    fun markdownNhieuDongGiuDungThuTu() {
        val ra = NoiDungGhiChu.tuMarkdown("# Đi chợ\n- [ ] Trứng\n- [x] Sữa\nGhi chú thêm")
        val dong = ra.lines()

        assertEquals(4, dong.size)
        assertTrue(dong[0], dong[0].contains("Đi chợ"))
        assertTrue(dong[1], dong[1].contains("checkbox") && !dong[1].contains("checked"))
        assertTrue(dong[2], dong[2].contains("checked=\"true\""))
        assertTrue(dong[3], dong[3].startsWith("<text"))
    }

    @Test
    fun diQuaMarkdownRoiVeChuTronThiGiuNguyenChu() {
        val goc = "Đi chợ\nTrứng\nSữa"

        assertEquals(goc, NoiDungGhiChu.chuTron(NoiDungGhiChu.tuMarkdown(goc)))
    }

    // --------------------------------------------------------- Delta cu

    @Test
    fun docDuocDeltaCuaBanDemoCu() {
        val delta = """[{"insert":"Sinh nhật mình\n"},{"insert":"Hadilao"},
            {"insert":"\n","attributes":{"list":"unchecked"}}]"""

        val ra = NoiDungGhiChu.doc(delta)

        assertTrue(ra, ra.contains("<text indent=\"1\">Sinh nhật mình</text>"))
        assertTrue(ra, ra.contains("<input type=\"checkbox\" indent=\"1\" />Hadilao"))
    }

    @Test
    fun deltaCoOTickDaTick() {
        val delta = """[{"insert":"Xong"},{"insert":"\n","attributes":{"list":"checked"}}]"""

        assertTrue(NoiDungGhiChu.doc(delta).contains("checked=\"true\""))
    }

    @Test
    fun markupCuaXiaomiThiGiuNguyenKhongDungVao() {
        val markup = "<text indent=\"1\">Đã là markup rồi</text>"

        assertEquals(markup, NoiDungGhiChu.doc(markup))
    }

    @Test
    fun deltaHongThiVanGiuLaiChuChuKhongMatBai() {
        // `org.json` von de dai (ca tren Android lan tren JVM) nen JSON hong van
        // boc ra duoc chu. Dieu PHAI giu la: khong duoc mat chu. Con boc ra thanh
        // markup hay tra ve nguyen van thi deu chap nhan duoc.
        val hong = "[{\"insert\": khong phai json}]"

        val ra = NoiDungGhiChu.doc(hong)

        assertTrue(ra, ra.contains("khong phai json"))
    }

    @Test
    fun deltaRongThiRaChuoiRong() {
        assertEquals("", NoiDungGhiChu.doc("[]"))
    }

    @Test
    fun khoiKhongPhaiChuTrongDeltaThiBoQua() {
        // Anh / ghi am trong Delta la object, khong phai chuoi — dot nay chua lam.
        val delta = """[{"insert":{"image":"abc"}},{"insert":"Chú thích"},{"insert":"\n"}]"""

        assertEquals("<text indent=\"1\">Chú thích</text>", NoiDungGhiChu.doc(delta))
    }
}
