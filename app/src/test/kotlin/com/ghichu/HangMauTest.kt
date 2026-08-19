package com.ghichu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hang cho mau cho su kien moi.
 *
 * Y do: truoc day su kien nao cung mot mau xanh. Gio mau dung xong bi day xuong
 * CUOI hang, mau khac duoc uu tien — ca 8 mau lan luot duoc dung het roi moi
 * quay vong.
 */
class HangMauTest {

    private val soMau = MauSuKien.BANG.size

    @Test
    fun hangDauLaDungThuTuBangMau() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7), HangMau.hangDau(8))
    }

    @Test
    fun dungMauDauThiNoXuongCuoi() {
        val hang = HangMau.dayXuongCuoi(HangMau.hangDau(8), 0)

        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 0), hang)
    }

    @Test
    fun dungMauGiuaHangCungXuongCuoi() {
        // Nguoi dung khong tu chon ma may gan mau 3 (vd sau khi hang da xoay).
        val hang = HangMau.dayXuongCuoi(listOf(3, 4, 5, 6, 7, 0, 1, 2), 3)

        assertEquals(listOf(4, 5, 6, 7, 0, 1, 2, 3), hang)
    }

    @Test
    fun tamLanLienTiepThiRaBaMauKhacNhau() {
        var hang = HangMau.hangDau(soMau)
        val daDung = ArrayList<Int>()

        repeat(soMau) {
            val mau = hang.first()
            daDung += mau
            hang = HangMau.dayXuongCuoi(hang, mau)
        }

        assertEquals("phải dùng hết cả bảng màu rồi mới lặp", soMau, daDung.distinct().size)
    }

    @Test
    fun quaMotVongThiQuayVeMauDauTien() {
        var hang = HangMau.hangDau(soMau)
        repeat(soMau) { hang = HangMau.dayXuongCuoi(hang, hang.first()) }

        assertEquals(HangMau.hangDau(soMau), hang)
    }

    @Test
    fun mauLaThiGhepVaoCuoiChuKhongLamMatHang() {
        // Du lieu cu co the mang mau ngoai bang; day xuong cuoi chu dung vut hang.
        val hang = HangMau.dayXuongCuoi(HangMau.hangDau(8), 99)

        assertEquals(9, hang.size)
        assertEquals(99, hang.last())
        assertTrue(hang.containsAll(HangMau.hangDau(8)))
    }

    // ---- Doc hang da luu ----

    @Test
    fun docDuocHangDaLuu() {
        assertEquals(listOf(3, 4, 5, 6, 7, 0, 1, 2), HangMau.docHang("3,4,5,6,7,0,1,2", 8))
    }

    @Test
    fun chuaLuoGiThiRaHangDau() {
        assertEquals(HangMau.hangDau(8), HangMau.docHang(null, 8))
        assertEquals(HangMau.hangDau(8), HangMau.docHang("", 8))
    }

    @Test
    fun hangThieuMauThiBoSungVaoCuoi() {
        // Bang mau rong ra (8 -> 10 mau) thi hang cu thieu 2 mau; khong bo sung
        // la hai mau moi khong bao gio duoc gan cho su kien nao.
        val hang = HangMau.docHang("0,1,2,3,4,5,6,7", 10)

        assertEquals(10, hang.size)
        assertEquals(listOf(8, 9), hang.takeLast(2))
    }

    @Test
    fun hangCoMauNgoaiBangThiLoaiBo() {
        // Bang mau hep lai (10 -> 8) thi mau 8,9 phai bi loai, khong thi ve ra
        // mau ngoai bang -> vang chi so.
        val hang = HangMau.docHang("9,0,1,8,2,3,4,5,6,7", 8)

        assertEquals(HangMau.hangDau(8).sorted(), hang.sorted())
        assertEquals(0, hang.first())
    }

    @Test
    fun hangHongVaTrungLapVanChuaDuoc() {
        val hang = HangMau.docHang("2,2,x,,1", 8)

        assertEquals(8, hang.size)
        assertEquals(listOf(2, 1), hang.take(2))
        assertEquals(8, hang.distinct().size)
    }
}
