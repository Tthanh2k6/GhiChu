package com.ghichu

import org.json.JSONArray
import org.json.JSONObject

/**
 * Doi qua lai giua **markup cua Xiaomi** va chu tron.
 *
 * App Ghi chu cua Xiaomi khong luu chu tron ma luu mot thu markup rieng, moi dong
 * la mot khoi:
 *
 * ```text
 * <input type="checkbox" checked="true" indent="1" />Sinh nhat Thanh
 * <text indent="1">t4-t6: cnnt trong chuyen doi so</text>
 * ```
 *
 * Doi chieu bo phan tich cua ban goc:
 * reference/miui-notes/sources/com/miui/richeditor/schema/NoteSchema.java
 */
object NoiDungGhiChu {

    private val THE = Regex("<[^>]*>")

    private val KY_TU_DAC_BIET = mapOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&apos;" to "'",
        "&#39;" to "'",
        "&nbsp;" to " ",
    )

    /** Bo het the, tra ve chu tron — dung cho cot `plain_text` de tim kiem. */
    fun chuTron(markup: String): String =
        markup.lineSequence()
            .map { boThoat(THE.replace(it, "")).trim() }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

    /** Chu tron -> markup. Moi dong thanh mot khoi `<text>`. */
    fun tuChuTron(chu: String): String =
        chu.lines().joinToString("\n") { "<text indent=\"1\">${thoat(it)}</text>" }

    /**
     * Markdown -> markup cua Xiaomi.
     *
     * Gemini tra ve Markdown (loi nhac he thong da dan nhu vay). App Ghi chu
     * khong hieu Markdown, va nguoi dung da chot **khong dung Markdown** — go chu
     * thay chu, khong lo `**` hay `#`. Nen phai doi truoc khi ghi xuong.
     *
     * Chi doi nhung thu co cho dung trong markup cua Xiaomi:
     *   `- [ ] viec`  -> o tick chua tick
     *   `- [x] viec`  -> o tick da tick
     *   `- muc`, `* muc`, `1. muc` -> dong chu thuong (bo dau dau dong)
     *   `# Tieu de`   -> dong chu thuong (bo dau thang)
     * Danh dau trong dong (`**dam**`, `*nghieng*`, `` `ma` ``) thi **go bo dau**,
     * giu lai chu — markup cua Xiaomi co dinh dang rieng, khong anh xa duoc, ma
     * de nguyen dau la nguoi dung nhin thay `**` giua bai.
     */
    fun tuMarkdown(md: String): String =
        md.lines().joinToString("\n") { dong ->
            val d = dong.trim()

            TICK.find(d)?.let { m ->
                val daTick = m.groupValues[1].lowercase() == "x"
                val chu = thoat(boDanhDauTrongDong(m.groupValues[2]))
                val co = if (daTick) " checked=\"true\"" else ""
                return@joinToString "<input type=\"checkbox\"$co indent=\"1\" />$chu"
            }

            val chu = d
                .replace(DAU_DONG, "")
                .replace(DAU_THANG, "")
                .let(::boDanhDauTrongDong)
            "<text indent=\"1\">${thoat(chu)}</text>"
        }

    /** Bo `**dam**`, `*nghieng*`, `` `ma` ``, `~~gach~~` — giu lai chu ben trong. */
    private fun boDanhDauTrongDong(chu: String): String = chu
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"), "$1")
        .replace(Regex("~~(.+?)~~"), "$1")
        .replace(Regex("`(.+?)`"), "$1")

    private val TICK = Regex("^[-*+]\\s+\\[([ xX])]\\s*(.*)$")
    private val DAU_DONG = Regex("^([-*+]\\s+|\\d+[.)]\\s+)")
    private val DAU_THANG = Regex("^#{1,6}\\s*")

    /**
     * Doc noi dung lay tu may chu ve.
     *
     * Ban Flutter cu luu noi dung bang **Delta cua Quill** (mot mang JSON), con
     * ban nay luu markup cua Xiaomi. Du lieu cu tren Supabase van con nen phai
     * nhan ra ma doi; khong doi thi ghi chu cu mo ra chi thay mot dong JSON.
     * Chi doi MOT CHIEU (Delta -> markup): ban Flutter sap bo, khong can duong ve.
     */
    fun doc(raw: String): String {
        val s = raw.trimStart()
        if (!s.startsWith("[")) return raw
        return try {
            tuDelta(JSONArray(s))
        } catch (_: Exception) {
            // JSON hong thi tha hien ra dang chu tho con hon mat bai.
            raw
        }
    }

    /**
     * Delta -> markup.
     *
     * Trong Delta, thuoc tinh CUA DAU XUONG DONG moi la thu quyet dinh kieu cua
     * dong dung truoc no (`list: checked` = o tick da tick...). Khoi khong phai
     * chu (anh, ghi am, ban do tu duy) bi bo qua — dot nay chi dong bo phan chu.
     */
    private fun tuDelta(ops: JSONArray): String {
        val ra = StringBuilder()
        val dong = StringBuilder()

        for (i in 0 until ops.length()) {
            val op = ops.optJSONObject(i) ?: continue
            val chen = op.opt("insert") as? String ?: continue
            val thuoc = op.optJSONObject("attributes")

            var phan = chen
            while (true) {
                val xuong = phan.indexOf('\n')
                if (xuong < 0) {
                    dong.append(phan)
                    break
                }
                dong.append(phan, 0, xuong)
                ra.append(khoi(dong.toString(), thuoc)).append('\n')
                dong.setLength(0)
                phan = phan.substring(xuong + 1)
            }
        }
        if (dong.isNotEmpty()) ra.append(khoi(dong.toString(), null))
        return ra.toString().trimEnd('\n')
    }

    private fun khoi(chu: String, thuoc: JSONObject?): String {
        val esc = thoat(chu)
        return when (thuoc?.optString("list")) {
            "checked" -> "<input type=\"checkbox\" checked=\"true\" indent=\"1\" />$esc"
            "unchecked" -> "<input type=\"checkbox\" indent=\"1\" />$esc"
            else -> "<text indent=\"1\">$esc</text>"
        }
    }

    private fun thoat(chu: String): String = chu
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun boThoat(chu: String): String =
        KY_TU_DAC_BIET.entries.fold(chu) { acc, (tu, den) -> acc.replace(tu, den) }
}
