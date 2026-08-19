package com.ghichu

/**
 * Mot doan chu lien tuc mang cung mot kieu.
 *
 * [anh] khac null nghia la doan nay KHONG phai chu ma la mot buc anh; luc do
 * [chu] la ky tu thay cho anh ([DinhDang.THAY_ANH]) de con dem duoc do dai.
 */
class Doan(
    val chu: String,
    val kieu: Int = 0,
    val mauChu: Int? = null,
    val mauNen: Int? = null,
    val lienKet: String? = null,
    val anh: String? = null,
) {
    fun co(co: Int): Boolean = (kieu and co) != 0

    override fun equals(other: Any?): Boolean = other is Doan &&
        other.chu == chu && other.kieu == kieu && other.mauChu == mauChu &&
        other.mauNen == mauNen && other.lienKet == lienKet && other.anh == anh

    override fun hashCode(): Int =
        chu.hashCode() * 31 + kieu + (mauChu ?: 0) + (mauNen ?: 0)

    override fun toString(): String = "Doan(\"$chu\", kieu=$kieu, anh=$anh)"
}

/**
 * Doi qua lai giua **phan trong mot khoi markup** va danh sach [Doan].
 *
 * CO Y tach khoi Android: o day khong co `Spannable`, khong co `Context`, khong
 * co mau lay tu tai nguyen — nho vay kiem thu duoc tren JVM, khong can dien
 * thoai. Phan doi [Doan] thanh span de hien len man hinh nam o [SoanThao].
 *
 * Bo the o day lay dung cua app Ghi chu Xiaomi (doi chieu
 * `reference/miui-notes/sources/com/miui/richeditor/schema/EditorHtml.java`,
 * ham `withinParagraph`) chu khong tu bia ra:
 *
 * ```text
 * <b>  <i>  <u>  <strike>  <size>  <mid-size>
 * <font color="#rrggbb">   <background color="#rrggbb">
 * <a href="...">           <img src="ten_tep" />
 * ```
 *
 * Giu dung bo the do vi ghi chu di qua may chu chung voi ban mod cu — doi the la
 * ghi chu cu mo ra mat het dinh dang.
 *
 * LUU Y mot cho de vap: ban goc ghi ra `<font color ="#..."` — co **dau cach
 * truoc dau bang**. Bo doc o day nhan ca hai kieu, con khi ghi thi ghi dang
 * chuan khong dau cach.
 */
object DinhDang {

    const val DAM = 1
    const val NGHIENG = 2
    const val GACH_CHAN = 4
    const val GACH_NGANG = 8

    /** Co chu lon (`<size>`) va co chu vua (`<mid-size>`) cua ban goc. */
    const val CHU_TO = 16
    const val CHU_VUA = 32

    /**
     * Ky tu dai dien cho anh trong dong chu.
     *
     * Lay dung ky tu cua ban goc (`NoteSchema.IMAGE_SUBSTITUTE`). Phai la MOT ky
     * tu that nam trong chuoi chu khong phai khoang trong: co vay con tro moi di
     * qua duoc anh, va xoa lui mot cai la anh bien mat nhu moi ky tu khac.
     */
    const val THAY_ANH = "\u263A"

    private val THE = Regex("<\\s*(/?)\\s*([a-zA-Z-]+)([^>]*?)/?\\s*>")
    private val THUOC_TINH = Regex("([a-zA-Z-]+)\\s*=\\s*\"([^\"]*)\"")

    // ------------------------------------------------------------ doc

    /**
     * Phan trong mot khoi -> danh sach doan.
     *
     * The la ma ta khong biet thi **bo the, giu chu** — mat dinh dang con hon
     * mat chu. Ghi chu cua nguoi dung khong duoc phep bien mat vi mot the la.
     */
    fun doc(trong: String): List<Doan> {
        val ra = ArrayList<Doan>()
        val ngan = ArrayList<Kieu>()
        var vt = 0

        fun themChu(chu: String) {
            if (chu.isEmpty()) return
            ra += Doan(
                chu = boThoat(chu),
                kieu = ngan.fold(0) { a, k -> a or k.co },
                // The long nhau thi cai TRONG CUNG thang — dung nhu trinh duyet.
                mauChu = ngan.lastOrNull { it.mauChu != null }?.mauChu,
                mauNen = ngan.lastOrNull { it.mauNen != null }?.mauNen,
                lienKet = ngan.lastOrNull { it.lienKet != null }?.lienKet,
            )
        }

        while (vt < trong.length) {
            val m = THE.find(trong, vt)
            if (m == null) {
                themChu(trong.substring(vt))
                break
            }
            themChu(trong.substring(vt, m.range.first))
            vt = m.range.last + 1

            val dong = m.groupValues[1] == "/"
            val ten = m.groupValues[2].lowercase()
            val phan = m.groupValues[3]

            if (ten == "img") {
                val src = thuocTinh(phan, "src") ?: thuocTinh(phan, "fileid")
                if (src != null) ra += Doan(chu = THAY_ANH, anh = src)
                continue
            }

            val kieu = kieuCua(ten, phan) ?: continue
            if (dong) {
                // Dong the nao thi go DUNG the do khoi ngan, khong phai go cai
                // tren cung: ban goc thinh thoang ghi long the cheo nhau
                // (`<b><i></b></i>`), go tren cung la lech het phan sau.
                val i = ngan.indexOfLast { it.ten == ten }
                if (i >= 0) ngan.removeAt(i)
            } else {
                ngan += kieu
            }
        }
        return ra
    }

    private class Kieu(
        val ten: String,
        val co: Int = 0,
        val mauChu: Int? = null,
        val mauNen: Int? = null,
        val lienKet: String? = null,
    )

    private fun kieuCua(ten: String, phan: String): Kieu? = when (ten) {
        "b", "strong" -> Kieu(ten, DAM)
        "i", "em" -> Kieu(ten, NGHIENG)
        "u" -> Kieu(ten, GACH_CHAN)
        "strike", "s", "del" -> Kieu(ten, GACH_NGANG)
        "size" -> Kieu(ten, CHU_TO)
        "mid-size" -> Kieu(ten, CHU_VUA)
        "font" -> Kieu(ten, mauChu = mau(thuocTinh(phan, "color")))
        "background" -> Kieu(ten, mauNen = mau(thuocTinh(phan, "color")))
        "a" -> Kieu(ten, lienKet = thuocTinh(phan, "href"))
        else -> null
    }

    private fun thuocTinh(phan: String, ten: String): String? =
        THUOC_TINH.findAll(phan).firstOrNull { it.groupValues[1].equals(ten, true) }
            ?.groupValues?.get(2)

    /** `#rrggbb` -> mau ARGB duc hoan toan. Sai dinh dang thi tra null. */
    fun mau(chu: String?): Int? {
        val s = chu?.trim()?.removePrefix("#") ?: return null
        if (s.length != 6) return null
        return try {
            (0xFF000000.toInt()) or s.toInt(16)
        } catch (_: NumberFormatException) {
            null
        }
    }

    // ------------------------------------------------------------ ghi

    /**
     * Danh sach doan -> phan trong mot khoi.
     *
     * Doan lien nhau ma cung kieu thi gop chung mot cap the, khong mo dong lien
     * tuc: go mot cau chu dam thanh ra hai chuc cap `<b></b>` la ghi chu phinh
     * gap may lan ma nhin khong khac gi.
     */
    fun ghi(doan: List<Doan>): String {
        val ra = StringBuilder()
        var i = 0
        while (i < doan.size) {
            val d = doan[i]
            if (d.anh != null) {
                ra.append("<img src=\"").append(thoat(d.anh)).append("\" />")
                i++
                continue
            }
            // Gom cac doan ke tiep giong het kieu.
            val chu = StringBuilder(d.chu)
            var j = i + 1
            while (j < doan.size && doan[j].anh == null && cungKieu(d, doan[j])) {
                chu.append(doan[j].chu)
                j++
            }
            ra.append(boc(d, thoat(chu.toString())))
            i = j
        }
        return ra.toString()
    }

    private fun cungKieu(a: Doan, b: Doan): Boolean =
        a.kieu == b.kieu && a.mauChu == b.mauChu && a.mauNen == b.mauNen && a.lienKet == b.lienKet

    /**
     * Boc chu bang cac the cua kieu.
     *
     * Thu tu the co dinh (dam -> nghieng -> gach chan -> gach ngang -> co chu ->
     * mau chu -> mau nen -> lien ket) chu khong theo thu tu doc vao: nho vay ghi
     * cung mot noi dung hai lan luon ra chuoi y het nhau, va so sanh "co doi
     * khong" truoc khi luu moi dung.
     */
    private fun boc(d: Doan, chu: String): String {
        if (chu.isEmpty()) return ""
        val mo = StringBuilder()
        val dong = StringBuilder()

        fun cap(mocua: String, dongCua: String) {
            mo.append(mocua)
            dong.insert(0, dongCua)
        }

        if (d.co(DAM)) cap("<b>", "</b>")
        if (d.co(NGHIENG)) cap("<i>", "</i>")
        if (d.co(GACH_CHAN)) cap("<u>", "</u>")
        if (d.co(GACH_NGANG)) cap("<strike>", "</strike>")
        if (d.co(CHU_TO)) cap("<size>", "</size>")
        if (d.co(CHU_VUA)) cap("<mid-size>", "</mid-size>")
        d.mauChu?.let { cap("<font color=\"" + hex(it) + "\">", "</font>") }
        d.mauNen?.let { cap("<background color=\"" + hex(it) + "\">", "</background>") }
        d.lienKet?.let { cap("<a href=\"" + thoat(it) + "\">", "</a>") }

        return mo.toString() + chu + dong.toString()
    }

    fun hex(mau: Int): String = "#%06x".format(mau and 0xFFFFFF)

    // ------------------------------------------------------------ thoat

    private fun thoat(chu: String): String = chu
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private val KY_TU_DAC_BIET = mapOf(
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&apos;" to "'",
        "&#39;" to "'",
        "&nbsp;" to " ",
        // `&amp;` phai doi CUOI CUNG, khong thi `&amp;lt;` thanh `<` chu khong
        // phai chuoi `&lt;` nhu nguoi dung da go.
        "&amp;" to "&",
    )

    private fun boThoat(chu: String): String =
        KY_TU_DAC_BIET.entries.fold(chu) { acc, (tu, den) -> acc.replace(tu, den) }
}
