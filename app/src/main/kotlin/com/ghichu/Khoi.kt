package com.ghichu

/**
 * Mot DONG cua ghi chu: mot khoi markup.
 *
 * Ghi chu luu theo dong, moi dong mot khoi:
 * ```text
 * <text indent="1">Việc tuần này</text>
 * <input type="checkbox" checked="true" indent="1" />Mua sữa
 * ```
 * [doan] la phan chu ben trong, da tach thanh cac doan cung kieu — xem [DinhDang].
 */
class Khoi(val loai: Int, val doan: List<Doan> = emptyList()) {

    /** Chu tron cua ca dong, khong kem dinh dang. */
    val chu: String get() = doan.joinToString("") { it.chu }

    companion object {
        const val CHU = 0
        const val O_CHUA_TICK = 1
        const val O_DA_TICK = 2
        const val DUONG_KE = 3
    }
}

/**
 * Doi qua lai giua markup ca bai va danh sach [Khoi].
 *
 * Thuan Kotlin, khong dung API Android — kiem thu tren JVM duoc.
 *
 * Vi sao tach khoi [NoiDungGhiChu]: ben do lam viec voi CHU TRON (tim kiem, doi
 * tu Markdown cua Gemini, doc du lieu Delta cu). Ben nay giu ca dinh dang. Hai
 * duong khac han nhau, gop mot cho thi som muon co nguoi goi nham ham va o tick
 * hoac chu dam bien mat.
 */
object KhoiNoiDung {

    private val THE_DAU = Regex("^<\\s*([a-zA-Z-]+)([^>]*)>")
    private val DA_TICK = Regex("checked\\s*=\\s*\"true\"")

    fun doc(markup: String): List<Khoi> = markup.lines().map { dong ->
        val d = dong.trim()
        val m = THE_DAU.find(d)
        val ten = m?.groupValues?.get(1)?.lowercase()

        when (ten) {
            "input" -> Khoi(
                if (DA_TICK.containsMatchIn(m!!.groupValues[2])) Khoi.O_DA_TICK else Khoi.O_CHUA_TICK,
                DinhDang.doc(d.substring(m.range.last + 1)),
            )
            "hr" -> Khoi(Khoi.DUONG_KE)
            "text" -> Khoi(Khoi.CHU, DinhDang.doc(boTheDong(d.substring(m!!.range.last + 1), "text")))
            // Khong nhan ra the dau dong thi coi ca dong la chu. Ghi chu cu, hoac
            // ghi chu do may khac ghi, khong nhat thiet dung bo the cua ta — mat
            // dinh dang con hon mat chu.
            else -> Khoi(Khoi.CHU, DinhDang.doc(d))
        }
    }

    private fun boTheDong(chu: String, ten: String): String {
        val dong = "</$ten>"
        val i = chu.lastIndexOf(dong)
        return if (i >= 0) chu.substring(0, i) else chu
    }

    fun ghi(khoi: List<Khoi>): String = khoi.joinToString("\n") { k ->
        val trong = DinhDang.ghi(k.doan)
        when (k.loai) {
            Khoi.O_CHUA_TICK -> "<input type=\"checkbox\" indent=\"1\" />$trong"
            Khoi.O_DA_TICK -> "<input type=\"checkbox\" checked=\"true\" indent=\"1\" />$trong"
            Khoi.DUONG_KE -> "<hr />"
            else -> "<text indent=\"1\">$trong</text>"
        }
    }
}
