package com.ghichu

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Quet QR — kiem thu chay tren JVM, KHONG can dien thoai.
 *
 * Lam duoc vi zxing vua ma hoa vua giai ma duoc: bai test tu ve ra mot ma QR,
 * doi thanh mang kenh sang y het thu may anh dua ra, roi bat [QuetQr.giaiMa]
 * doc lai. Duong giai ma vi vay duoc chay that chu khong phai gia lap.
 */
class QuetQrTest {

    /**
     * Ve mot ma QR thanh mang kenh sang (0 = den, 255 = trang) — dung dinh dang
     * ma [QuetQr.giaiMa] nhan tu may anh.
     */
    private fun veMa(noiDung: String, canh: Int = 300): Triple<ByteArray, Int, Int> {
        val luoi = QRCodeWriter().encode(
            noiDung, BarcodeFormat.QR_CODE, canh, canh,
            mapOf(
                EncodeHintType.MARGIN to 2,
                // Ma QR ngoai doi thuc gan nhu deu la UTF-8. Khong dat o day
                // thi zxing ma hoa bang ISO-8859-1 va chinh BAI TEST lam hong
                // chu tieng Viet truoc khi kip giai ma.
                EncodeHintType.CHARACTER_SET to "UTF-8",
            ),
        )
        val sang = ByteArray(luoi.width * luoi.height)
        for (y in 0 until luoi.height) {
            for (x in 0 until luoi.width) {
                sang[y * luoi.width + x] = if (luoi[x, y]) 0 else 255.toByte()
            }
        }
        return Triple(sang, luoi.width, luoi.height)
    }

    // ---- Giai ma ----

    @Test
    fun docDuocLienKet() {
        val (sang, r, c) = veMa("https://example.com/abc")
        assertEquals("https://example.com/abc", QuetQr.giaiMa(sang, r, c))
    }

    @Test
    fun docDuocChuTiengVietCoDau() {
        val chu = "Họp nhóm lúc 14h thứ Tư"
        val (sang, r, c) = veMa(chu)
        assertEquals(chu, QuetQr.giaiMa(sang, r, c))
    }

    @Test
    fun docDuocMaWifi() {
        val chu = "WIFI:T:WPA;S:Nha Toi;P:matkhau123;;"
        val (sang, r, c) = veMa(chu)
        assertEquals(chu, QuetQr.giaiMa(sang, r, c))
    }

    @Test
    fun khungKhongCoMaThiTraVeNull() {
        // Khung trang tron — dung canh nay xay ra vai chuc lan moi giay khi quet.
        val sang = ByteArray(200 * 200) { 255.toByte() }
        assertNull(QuetQr.giaiMa(sang, 200, 200))
    }

    @Test
    fun kichThuocLoanThiKhongVo() {
        assertNull(QuetQr.giaiMa(ByteArray(0), 0, 0))
        assertNull(QuetQr.giaiMa(ByteArray(10), -5, 20))
        // Mang ngan hon rong*cao: khong duoc nem ArrayIndexOutOfBounds.
        assertNull(QuetQr.giaiMa(ByteArray(10), 100, 100))
    }

    @Test
    fun goiGiaiMaNhieuLanLienTiepVanDung() {
        // Bo doc dung lai giua cac khung (decodeWithState + reset). Sai cho nay
        // thi quet duoc ma dau roi cac ma sau doc ra sai hoac khong ra.
        val (s1, r1, c1) = veMa("https://mot.example")
        val (s2, r2, c2) = veMa("https://hai.example")

        assertEquals("https://mot.example", QuetQr.giaiMa(s1, r1, c1))
        assertNull(QuetQr.giaiMa(ByteArray(200 * 200) { 255.toByte() }, 200, 200))
        assertEquals("https://hai.example", QuetQr.giaiMa(s2, r2, c2))
    }

    // ---- Phan loai ----

    @Test
    fun nhanRaTungLoai() {
        assertEquals(QuetQr.Loai.LIEN_KET, QuetQr.loaiCua("https://vnexpress.net"))
        assertEquals(QuetQr.Loai.WIFI, QuetQr.loaiCua("WIFI:T:WPA;S:abc;P:123;;"))
        assertEquals(QuetQr.Loai.DIEN_THOAI, QuetQr.loaiCua("tel:0900123456"))
        assertEquals(QuetQr.Loai.EMAIL, QuetQr.loaiCua("mailto:ai@do.vn"))
        assertEquals(QuetQr.Loai.DANH_THIEP, QuetQr.loaiCua("BEGIN:VCARD\nFN:Thanh\nEND:VCARD"))
        assertEquals(QuetQr.Loai.CHU, QuetQr.loaiCua("chỉ là chữ thôi"))
    }

    // ---- Chi mo http/https ----

    @Test
    fun chiCoiHttpVaHttpsLaLienKet() {
        assertTrue(QuetQr.laLienKet("https://example.com"))
        assertTrue(QuetQr.laLienKet("http://example.com/a?b=1"))
        assertTrue(QuetQr.laLienKet("HTTPS://Example.COM"))
    }

    /**
     * Nhung luoc do duoi day KHONG duoc coi la lien ket.
     *
     * `intent:` mo thang mot man hinh trong app khac; `file:`/`content:` doc tep
     * trong may; `javascript:` chay ma. Nguoi dung chi nhin thay mot o vuong den
     * tren tuong — khong the biet minh sap bam vao cai gi.
     */
    @Test
    fun khongCoiLuocDoNguyHiemLaLienKet() {
        for (xau in listOf(
            "intent://scan/#Intent;scheme=zxing;end",
            "file:///data/data/com.miui.notes.mod/databases/note.db",
            "content://sms/inbox",
            "javascript:alert(1)",
            "market://details?id=abc",
        )) {
            assertFalse(xau, QuetQr.laLienKet(xau))
            assertEquals(xau, QuetQr.Loai.CHU, QuetQr.loaiCua(xau))
        }
    }

    @Test
    fun chuoiKhongPhaiDiaChiThiKhongPhaiLienKet() {
        assertFalse(QuetQr.laLienKet("https://"))
        assertFalse(QuetQr.laLienKet("https://khongcodaucham"))
        assertFalse(QuetQr.laLienKet("https://example.com co khoang trang"))
        assertFalse(QuetQr.laLienKet("mua sữa và bánh mì"))
    }

    // ---- Ten mien hien cho nguoi dung ----

    @Test
    fun layDungTenMien() {
        assertEquals("vnexpress.net", QuetQr.tenMien("https://vnexpress.net/tin-tuc?id=1"))
        assertEquals("a.b.example.com", QuetQr.tenMien("http://a.b.example.com/x"))
    }

    /**
     * Kieu nguy trang co dien: phan truoc dau @ chi la ten dang nhap, trinh
     * duyet se di toi `ke-gian.example`. Phai hien dung cho SE TOI.
     */
    @Test
    fun tenMienBoQuaPhanTruocDauA() {
        assertEquals("ke-gian.example", QuetQr.tenMien("https://google.com@ke-gian.example/abc"))
    }

    @Test
    fun rutGonCoDauBaCham() {
        assertEquals("abc", QuetQr.rutGon("abc", 10))
        assertEquals("abcdefghi…", QuetQr.rutGon("abcdefghijklmn", 10))
        assertEquals("mot hai", QuetQr.rutGon("mot\nhai", 10))
    }
}
