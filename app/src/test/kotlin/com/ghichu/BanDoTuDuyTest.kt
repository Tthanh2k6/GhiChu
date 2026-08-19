package com.ghichu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiem thu phan xep cho va doc/ghi ban do tu duy tren JVM.
 *
 * Xep cho la cho de sai ma nhin man hinh khong ra ngay: hai nhanh de len nhau
 * mot phan thi trong van "co ve dung", chi khi cay lon moi thay roi. Nen o day
 * kiem THANG toa do, khong kiem bang mat.
 *
 * Do rong chu duoc gia lap la 10 don vi moi ky tu — khong phu thuoc `Paint`.
 */
class BanDoTuDuyTest {

    private val doRong: (String) -> Float = { it.length * 10f }

    private fun cay(): Nut = Nut("Gốc").apply {
        con += Nut("A").apply {
            con += Nut("A1")
            con += Nut("A2")
        }
        con += Nut("B")
    }

    // ---- cay ----

    @Test
    fun `dem dung so nut ke ca chinh no`() {
        assertEquals(5, cay().demNut())
        assertEquals(1, Nut("một mình").demNut())
    }

    @Test
    fun `tim dung nut cha o bat ky bac nao`() {
        val g = cay()
        val a = g.con[0]
        val a1 = a.con[0]
        assertSame(g, g.chaCua(a))
        assertSame(a, g.chaCua(a1))
        assertNull(g.chaCua(g))
    }

    @Test
    fun `xoa mot nhanh thi ca nhanh con di theo`() {
        val g = cay()
        assertTrue(g.xoaCon(g.con[0]))
        assertEquals(2, g.demNut())
    }

    // ---- xep cho ----

    @Test
    fun `con nam ben phai cha, cach dung mot bac`() {
        val g = cay()
        val o = XepBanDo.xep(g, doRong)
        val oGoc = o.first { it.nut === g }
        val oA = o.first { it.nut === g.con[0] }
        assertEquals(oGoc.phai + XepBanDo.CACH_NGANG, oA.x, 0.01f)
    }

    @Test
    fun `cha nam giua khoi con theo chieu doc`() {
        val g = cay()
        val o = XepBanDo.xep(g, doRong)
        val a = o.first { it.nut === g.con[0] }
        val a1 = o.first { it.nut === g.con[0].con[0] }
        val a2 = o.first { it.nut === g.con[0].con[1] }
        assertEquals((a1.giua + a2.giua) / 2, a.giua, 0.01f)
    }

    @Test
    fun `hai nhanh khong bao gio de len nhau`() {
        // Nhanh A co hai con, nhanh B khong con nao. Neu dat cha truoc roi rai con
        // quanh no thi A2 se de len B — day la cho hay sai nhat.
        val g = cay()
        val o = XepBanDo.xep(g, doRong)
        for (i in o.indices) {
            for (j in i + 1 until o.size) {
                val a = o[i]
                val b = o[j]
                val deNhau = a.x < b.phai && b.x < a.phai &&
                    a.y < b.y + b.cao && b.y < a.y + a.cao
                assertTrue("Nút ${a.nut.chu} đè lên ${b.nut.chu}", !deNhau)
            }
        }
    }

    @Test
    fun `nut chu dai thi o rong ra, nut chu ngan van co be ngang toi thieu`() {
        val o = XepBanDo.xep(Nut("Một tiêu đề khá là dài"), doRong)
        assertTrue(o[0].rong > XepBanDo.RONG_TOI_THIEU)

        val nho = XepBanDo.xep(Nut("A"), doRong)
        assertEquals(XepBanDo.RONG_TOI_THIEU, nho[0].rong, 0.01f)
    }

    @Test
    fun `moi nut deu co dung mot o, va cha tro dung`() {
        val g = cay()
        val o = XepBanDo.xep(g, doRong)
        assertEquals(5, o.size)
        assertNull(o.first { it.nut === g }.cha)
        assertSame(g, o.first { it.nut === g.con[1] }.cha?.nut)
    }

    @Test
    fun `nut chua go chu van hien mot dau nhac chu khong hien rong`() {
        assertEquals("…", XepBanDo.nhan(Nut("")))
        assertEquals("Việc", XepBanDo.nhan(Nut("Việc")))
    }

    // ---- doc/ghi ----

    @Test
    fun `cay di mot vong qua JSON khong doi`() {
        val g = cay()
        val lai = XepBanDo.docJson(XepBanDo.ghiJson(g))
        assertEquals(5, lai.demNut())
        assertEquals("Gốc", lai.chu)
        assertEquals("A1", lai.con[0].con[0].chu)
        assertEquals("B", lai.con[1].chu)
    }

    @Test
    fun `JSON hong thi tra ve cay mot nut chu khong nem loi`() {
        // Ghi chu cua nguoi dung khong duoc phep lam vo man hinh.
        val lai = XepBanDo.docJson("{ đây không phải JSON")
        assertEquals(1, lai.demNut())
    }

    @Test
    fun `khoi ban do di mot vong qua markup khong doi`() {
        val json = XepBanDo.ghiJson(cay())
        val markup = KhoiNoiDung.ghi(listOf(Khoi(Khoi.BAN_DO, duLieu = json)))
        val k = KhoiNoiDung.doc(markup)
        assertEquals(Khoi.BAN_DO, k[0].loai)
        assertEquals(json, k[0].duLieu)
    }

    @Test
    fun `chu co dau lon hon nho hon van qua duoc markup`() {
        // JSON nam giua the nen `<` `>` phai duoc thoat, khong thi bo doc the lac.
        val g = Nut("a < b > c & d")
        val markup = KhoiNoiDung.ghi(listOf(Khoi(Khoi.BAN_DO, duLieu = XepBanDo.ghiJson(g))))
        val lai = XepBanDo.docJson(KhoiNoiDung.doc(markup)[0].duLieu)
        assertEquals("a < b > c & d", lai.chu)
    }
}
