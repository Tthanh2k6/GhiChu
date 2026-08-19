package com.ghichu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiem thu bo doc/ghi dinh dang tren JVM.
 *
 * Day la cho de mat du lieu nhat cua ca app: ghi chu di mot vong markup -> soan
 * thao -> markup MOI LAN mo ra dong lai. Sai mot the la moi lan mo ghi chu no
 * lai rung bot mot phan dinh dang, ma khong ai nhan ra ngay.
 */
class DinhDangTest {

    // ---- doc ----

    @Test
    fun `chu tron ra dung mot doan khong kieu`() {
        val d = DinhDang.doc("Họp lúc 14h")
        assertEquals(listOf(Doan("Họp lúc 14h")), d)
    }

    @Test
    fun `the long nhau thi doan giua mang ca hai kieu`() {
        val d = DinhDang.doc("a<b>b<i>c</i></b>d")
        assertEquals(4, d.size)
        assertEquals(0, d[0].kieu)
        assertEquals(DinhDang.DAM, d[1].kieu)
        assertEquals(DinhDang.DAM or DinhDang.NGHIENG, d[2].kieu)
        assertEquals(0, d[3].kieu)
    }

    @Test
    fun `the dong cheo nhau van go dung the do`() {
        // Ban goc thinh thoang ghi kieu nay. Go "cai tren cung" la lech het phan sau.
        val d = DinhDang.doc("<b><i>x</b>y</i>")
        assertEquals(DinhDang.DAM or DinhDang.NGHIENG, d[0].kieu)
        assertEquals(DinhDang.NGHIENG, d[1].kieu)
    }

    @Test
    fun `the la thi bo the giu chu`() {
        val d = DinhDang.doc("xin <blink>chào</blink> bạn")
        assertEquals("xin chào bạn", d.joinToString("") { it.chu })
    }

    @Test
    fun `nhan mau chu ke ca khi ban goc them dau cach truoc dau bang`() {
        val chuan = DinhDang.doc("<font color=\"#ff0000\">đỏ</font>")
        val goc = DinhDang.doc("<font color =\"#ff0000\">đỏ</font>")
        assertEquals(0xFFFF0000.toInt(), chuan[0].mauChu)
        assertEquals(0xFFFF0000.toInt(), goc[0].mauChu)
    }

    @Test
    fun `mau sai dinh dang thi bo qua chu khong lam vo ca bai`() {
        assertNull(DinhDang.mau("#xyz"))
        assertNull(DinhDang.mau(null))
        val d = DinhDang.doc("<font color=\"bậy\">chữ</font>")
        assertEquals("chữ", d[0].chu)
        assertNull(d[0].mauChu)
    }

    @Test
    fun `anh thanh mot doan rieng mang ten tep`() {
        val d = DinhDang.doc("trước<img src=\"a1.jpg\" />sau")
        assertEquals(3, d.size)
        assertEquals("a1.jpg", d[1].anh)
        assertEquals(DinhDang.THAY_ANH, d[1].chu)
    }

    // ---- ghi ----

    @Test
    fun `doan lien nhau cung kieu thi gop chung mot cap the`() {
        val ra = DinhDang.ghi(listOf(
            Doan("Xin ", DinhDang.DAM),
            Doan("chào", DinhDang.DAM),
        ))
        assertEquals("<b>Xin chào</b>", ra)
    }

    @Test
    fun `ghi ra chuoi on dinh - cung noi dung hai lan ra y het`() {
        val goc = "<b><i>Xin</i></b> chào"
        val mot = DinhDang.ghi(DinhDang.doc(goc))
        val hai = DinhDang.ghi(DinhDang.doc(mot))
        assertEquals(mot, hai)
    }

    // ---- vong tron markup -> doan -> markup ----

    @Test
    fun `di mot vong khong rung dinh dang`() {
        val goc = "<b>đậm</b> thường <i>nghiêng</i> <u>gạch</u> <strike>xoá</strike>"
        assertEquals(goc, DinhDang.ghi(DinhDang.doc(goc)))
    }

    @Test
    fun `mau chu va mau nen giu nguyen qua mot vong`() {
        val goc = "<font color=\"#ff8800\">cam</font><background color=\"#00ff00\">nền</background>"
        assertEquals(goc, DinhDang.ghi(DinhDang.doc(goc)))
    }

    @Test
    fun `ky tu dac biet duoc thoat va tra lai dung`() {
        val d = DinhDang.doc("a &lt; b &amp; c")
        assertEquals("a < b & c", d[0].chu)
        assertEquals("a &lt; b &amp; c", DinhDang.ghi(d))
    }

    @Test
    fun `chuoi da thoat hai lan khong bi go nham`() {
        // Nguoi dung go dung chuoi `&lt;` thi phai giu lai `&lt;`, khong hoa `<`.
        val d = DinhDang.doc("&amp;lt;")
        assertEquals("&lt;", d[0].chu)
    }

    // ---- khoi ----

    @Test
    fun `doc dung ba loai khoi`() {
        val markup = listOf(
            """<text indent="1">Việc tuần này</text>""",
            """<input type="checkbox" checked="true" indent="1" />Mua sữa""",
            """<input type="checkbox" indent="1" />Gọi Thành""",
            """<hr />""",
        ).joinToString("\n")

        val k = KhoiNoiDung.doc(markup)
        assertEquals(4, k.size)
        assertEquals(Khoi.CHU, k[0].loai)
        assertEquals("Việc tuần này", k[0].chu)
        assertEquals(Khoi.O_DA_TICK, k[1].loai)
        assertEquals("Mua sữa", k[1].chu)
        assertEquals(Khoi.O_CHUA_TICK, k[2].loai)
        assertEquals(Khoi.DUONG_KE, k[3].loai)
    }

    @Test
    fun `khoi di mot vong khong doi`() {
        val markup = listOf(
            """<text indent="1"><b>Việc</b> tuần này</text>""",
            """<input type="checkbox" checked="true" indent="1" />Mua sữa""",
            """<hr />""",
        ).joinToString("\n")
        assertEquals(markup, KhoiNoiDung.ghi(KhoiNoiDung.doc(markup)))
    }

    @Test
    fun `dong khong co the van doc duoc thanh chu`() {
        val k = KhoiNoiDung.doc("chỉ là chữ trơn")
        assertEquals(Khoi.CHU, k[0].loai)
        assertEquals("chỉ là chữ trơn", k[0].chu)
    }

    @Test
    fun `o tick co chu dam thi giu ca hai`() {
        val markup = """<input type="checkbox" indent="1" /><b>Gấp</b>: gọi Thành"""
        val k = KhoiNoiDung.doc(markup)
        assertEquals(Khoi.O_CHUA_TICK, k[0].loai)
        assertTrue(k[0].doan[0].co(DinhDang.DAM))
        assertEquals("Gấp: gọi Thành", k[0].chu)
        assertEquals(markup, KhoiNoiDung.ghi(k))
    }

    @Test
    fun `chu tron cua NoiDungGhiChu van lot sach the dinh dang`() {
        val markup = """<text indent="1"><b>Việc</b> <font color="#ff0000">gấp</font></text>"""
        assertEquals("Việc gấp", NoiDungGhiChu.chuTron(markup))
    }
}
