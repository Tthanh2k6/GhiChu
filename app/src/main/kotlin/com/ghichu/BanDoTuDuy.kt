package com.ghichu

import org.json.JSONArray
import org.json.JSONObject

/**
 * Mot nut trong ban do tu duy.
 *
 * CO Y de sua duoc tai cho ([chu] va [con] deu doi duoc): ca cay nam gon trong
 * bo nho khi dang mo, moi thao tac cua nguoi dung la mot phep sua nho. Dung cau
 * truc bat bien thi moi lan go mot chu la dung lai ca cay.
 */
class Nut(
    var chu: String = "",
    val con: MutableList<Nut> = ArrayList(),
) {
    /** Dem ca chinh no. */
    fun demNut(): Int = 1 + con.sumOf { it.demNut() }

    /** Xoa [muc] khoi cay. Tra ve true neu tim thay. */
    fun xoaCon(muc: Nut): Boolean {
        if (con.remove(muc)) return true
        return con.any { it.xoaCon(muc) }
    }

    /** Nut cha cua [muc], hoac null neu [muc] la goc hoac khong thuoc cay. */
    fun chaCua(muc: Nut): Nut? {
        if (muc in con) return this
        for (c in con) c.chaCua(muc)?.let { return it }
        return null
    }
}

/**
 * O chu nhat cua mot nut sau khi da xep cho.
 *
 * Toa do tinh bang **don vi truu tuong** (khong phai diem anh): ben ve nhan mot
 * ham do be ngang chu roi tu nhan voi ti le phong. Nho vay phan xep cho khong
 * dinh gi den `Paint` va kiem thu tren JVM duoc.
 */
class ONut(
    val nut: Nut,
    val x: Float,
    val y: Float,
    val rong: Float,
    val cao: Float,
    val cha: ONut?,
) {
    val giua: Float get() = y + cao / 2
    val phai: Float get() = x + rong
}

/**
 * Xep cho va doc/ghi ban do tu duy.
 *
 * Thuan Kotlin, khong dung API Android — kiem thu tren JVM duoc.
 *
 * KIEU XEP: cay nam ngang, goc o ben trai, con toa sang phai. Chon kieu nay chu
 * khong phai toa tron quanh goc vi chu tieng Viet doc theo hang ngang — cay ngang
 * thi moi nut la mot dong chu doc thang, con kieu toa tron thi nua so nhan nam
 * nguoc hoac chong len nhau.
 */
object XepBanDo {

    /** Khoang cach giua hai bac. */
    const val CACH_NGANG = 44f

    /** Khoang cach doc giua hai nut anh em. */
    const val CACH_DOC = 14f

    const val CAO_NUT = 40f
    const val DEM_NGANG = 18f
    const val RONG_TOI_THIEU = 56f

    /**
     * Xep cho cho ca cay.
     *
     * @param doRongChu do be ngang cua mot chuoi chu, cung don vi voi [CAO_NUT].
     */
    fun xep(goc: Nut, doRongChu: (String) -> Float): List<ONut> {
        val ra = ArrayList<ONut>()
        datCho(goc, 0f, 0f, null, ra, doRongChu)
        return ra
    }

    /**
     * Dat mot nhanh, tra ve chieu cao ca nhanh do.
     *
     * Cach lam: **xep con truoc, roi dat cha vao GIUA khoi con**. Lam nguoc lai
     * (dat cha truoc rooi rai con quanh no) thi mot nhanh nhieu con se day cac
     * nhanh khac de len nhau.
     */
    private fun datCho(
        nut: Nut,
        x: Float,
        y: Float,
        cha: ONut?,
        ra: ArrayList<ONut>,
        doRongChu: (String) -> Float,
    ): Float {
        val rong = (doRongChu(nhan(nut)) + DEM_NGANG * 2).coerceAtLeast(RONG_TOI_THIEU)

        if (nut.con.isEmpty()) {
            ra += ONut(nut, x, y, rong, CAO_NUT, cha)
            return CAO_NUT
        }

        // Cho cha mot o tam thoi de con co [cha] tro toi; toa do y that duoc chot
        // ngay sau khi biet ca khoi con cao bao nhieu.
        val xCon = x + rong + CACH_NGANG
        val conO = ArrayList<Pair<Nut, Float>>()
        var caoTong = 0f
        for ((i, c) in nut.con.withIndex()) {
            if (i > 0) caoTong += CACH_DOC
            conO += c to caoTong
            caoTong += caoNhanh(c, doRongChu)
        }

        val yCha = y + (caoTong - CAO_NUT) / 2
        val o = ONut(nut, x, yCha, rong, CAO_NUT, cha)
        ra += o

        for ((c, lech) in conO) datCho(c, xCon, y + lech, o, ra, doRongChu)
        return caoTong
    }

    /** Chieu cao ca nhanh, khong ke khoang cach voi anh em. */
    private fun caoNhanh(nut: Nut, doRongChu: (String) -> Float): Float {
        if (nut.con.isEmpty()) return CAO_NUT
        var cao = 0f
        for ((i, c) in nut.con.withIndex()) {
            if (i > 0) cao += CACH_DOC
            cao += caoNhanh(c, doRongChu)
        }
        // Nhanh khong bao gio thap hon chinh nut cha cua no.
        return maxOf(cao, CAO_NUT)
    }

    /** Chu hien tren nut; nut chua go gi thi hien mot dau nhac. */
    fun nhan(nut: Nut): String = nut.chu.ifBlank { "…" }

    // ------------------------------------------------------------ doc/ghi

    fun ghiJson(goc: Nut): String = mot(goc).toString()

    private fun mot(nut: Nut): JSONObject = JSONObject().apply {
        put("chu", nut.chu)
        if (nut.con.isNotEmpty()) {
            put("con", JSONArray().apply { for (c in nut.con) put(mot(c)) })
        }
    }

    /**
     * Doc cay tu JSON. Du lieu hong thi tra ve mot cay MOT NUT chu khong nem
     * loi — ghi chu cua nguoi dung khong duoc phep lam vo man hinh.
     */
    fun docJson(chu: String): Nut = try {
        docMot(JSONObject(chu))
    } catch (_: Exception) {
        Nut("Bản đồ tư duy")
    }

    private fun docMot(o: JSONObject): Nut {
        val nut = Nut(if (o.isNull("chu")) "" else o.optString("chu"))
        val m = o.optJSONArray("con") ?: return nut
        for (i in 0 until m.length()) {
            m.optJSONObject(i)?.let { nut.con += docMot(it) }
        }
        return nut
    }
}
