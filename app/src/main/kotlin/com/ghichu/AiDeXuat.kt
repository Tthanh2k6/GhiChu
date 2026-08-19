package com.ghichu

import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Y dinh AI doan ra tu cau cua nguoi dung.
 */
enum class YDinh { CHAT, TAO_GHI_CHU, SUA_GHI_CHU, SUA_LICH }

/** Mot su kien AI de nghi THEM. Moc thoi gian da doi ra mili-giay. */
class SuKienMoi(
    val tieuDe: String,
    val batDau: Long,
    val ketThuc: Long,
    val caNgay: Boolean,
    val lapLai: Int,
    val lapDen: Long?,
    val nhacTruoc: Int,
)

/**
 * Mot thao tac SUA su kien da co.
 *
 * Truong nao AI khong gui thi de `null` — nghia la GIU NGUYEN. Phan biet duoc
 * "khong gui" voi "gui gia tri rong" la diem mau chot: nguoi dung noi "doi sang
 * 8h toi" thi chi gio doi, ngay va do dai giu y nguyen.
 */
class SuaSuKienOp(
    val id: Long,
    val tieuDe: String? = null,
    val ngay: LocalDate? = null,
    val batDau: String? = null,
    val ketThuc: String? = null,
    val caNgay: Boolean? = null,
    val lapLai: Int? = null,
    val lapDen: Long? = null,
    val coLapDen: Boolean = false,
    val nhacTruoc: Int? = null,
    val coNhacTruoc: Boolean = false,
)

/**
 * Ket qua doc mot cau tra loi cua AI.
 *
 * [loiDap] luon hien cho nguoi dung. Cac truong con lai la **de xuat** — AI
 * KHONG tu chay. Nguoi dung phai bam xac nhan; day la yeu cau da chot, ly do la
 * tranh hieu nham y.
 */
class DeXuat(
    val loiDap: String,
    val yDinh: YDinh = YDinh.CHAT,
    val tieuDeGhiChu: String = "",
    val noiDungGhiChu: String = "",
    val timGhiChu: String = "",
    val vietDe: Boolean = false,
    val themSuKien: List<SuKienMoi> = emptyList(),
    val suaSuKien: List<SuaSuKienOp> = emptyList(),
    val xoaSuKien: List<Long> = emptyList(),
) {
    /** Co gi de nguoi dung bam xac nhan khong? */
    val coViecLam: Boolean
        get() = when (yDinh) {
            YDinh.CHAT -> false
            YDinh.TAO_GHI_CHU, YDinh.SUA_GHI_CHU -> noiDungGhiChu.isNotBlank()
            YDinh.SUA_LICH -> themSuKien.isNotEmpty() || suaSuKien.isNotEmpty() || xoaSuKien.isNotEmpty()
        }

    /** Mot dong tom tat de hien tren the xem truoc. */
    fun tomTat(): String = when (yDinh) {
        YDinh.CHAT -> ""
        YDinh.TAO_GHI_CHU -> "Tạo ghi chú: ${tieuDeGhiChu.ifBlank { "(không tiêu đề)" }}"
        YDinh.SUA_GHI_CHU ->
            "${if (vietDe) "Viết lại" else "Thêm vào"} ghi chú: $timGhiChu"
        YDinh.SUA_LICH -> buildList {
            if (themSuKien.isNotEmpty()) add("thêm ${themSuKien.size}")
            if (suaSuKien.isNotEmpty()) add("sửa ${suaSuKien.size}")
            if (xoaSuKien.isNotEmpty()) add("xoá ${xoaSuKien.size}")
        }.joinToString(", ", prefix = "Lịch trình: ") + " sự kiện"
    }
}

/**
 * Doc JSON AI tra ve thanh [DeXuat].
 *
 * Thuan Kotlin + org.json, khong dung API Android — de kiem thu duoc tren JVM.
 * JSON hong thi KHONG vut di: coi nhu tro chuyen thuong va van cho tao ghi chu
 * tu nguyen van, khoi phi mot luot goi API.
 */
object AiDocDeXuat {

    /**
     * Doc mot truong chu ra khoi JSON.
     *
     * **TUYET DOI KHONG goi thang `optString`** trong tep nay. Ly do: hai ban
     * `org.json` cu xu KHAC NHAU khi gap JSON null —
     *
     * | Noi | `optString("title")` voi `{"title": null}` |
     * |---|---|
     * | Android (may that) | tra ve CHUOI `"null"` |
     * | `org.json:json` (test tren JVM) | tra ve chuoi rong |
     *
     * Loi nhac he thong cho phep model gui null (`remind_minutes`), va model
     * cung hay tu gui `"title": null`, `"end_date": null` cho truong khong co.
     * Goi thang `optString` thi tren may that su kien se mang tieu de la chu
     * "null" — con bai test tren JVM KHONG BAO GIO bat duoc, vi o do no tra ve
     * chuoi rong.
     *
     * `isNull()` thi hai ban giong nhau, nen hoi no truoc.
     */
    fun chuoi(o: JSONObject, ten: String): String =
        if (o.isNull(ten)) "" else o.optString(ten).trim()

    fun doc(raw: String, zone: ZoneId = ZoneId.systemDefault()): DeXuat {
        val data = try {
            JSONObject(Gemini.bocJson(raw))
        } catch (_: Exception) {
            return DeXuat(
                loiDap = raw.trim(),
                yDinh = YDinh.TAO_GHI_CHU,
                noiDungGhiChu = raw.trim(),
            )
        }

        val loiDap = chuoi(data, "reply").ifEmpty { raw.trim() }

        return when (chuoi(data, "intent")) {
            "create_note" -> {
                val noi = chuoi(data, "note_content")
                if (noi.isEmpty()) DeXuat(loiDap)
                else DeXuat(
                    loiDap = loiDap,
                    yDinh = YDinh.TAO_GHI_CHU,
                    tieuDeGhiChu = chuoi(data, "note_title"),
                    noiDungGhiChu = noi,
                )
            }

            "edit_note" -> {
                val noi = chuoi(data, "note_content")
                val tim = chuoi(data, "edit_target")
                if (noi.isEmpty() || tim.isEmpty()) DeXuat(loiDap)
                else DeXuat(
                    loiDap = loiDap,
                    yDinh = YDinh.SUA_GHI_CHU,
                    timGhiChu = tim,
                    noiDungGhiChu = noi,
                    vietDe = chuoi(data, "edit_mode") == "replace",
                )
            }

            // "update_calendar" la ten hien hanh; ba ten cu van nhan de khong vo
            // khi model quen loi nhac cu tra ve kieu cu.
            "update_calendar", "add_event", "edit_event", "delete_event" ->
                docLich(data, loiDap, zone)

            else -> DeXuat(loiDap)
        }
    }

    private fun docLich(data: JSONObject, loiDap: String, zone: ZoneId): DeXuat {
        val them = ArrayList<SuKienMoi>()
        data.optJSONArray("events")?.let { m ->
            for (i in 0 until m.length()) {
                m.optJSONObject(i)?.let { dungSuKien(it, zone) }?.let(them::add)
            }
        }

        val sua = ArrayList<SuaSuKienOp>()
        data.optJSONArray("event_edits")?.let { m ->
            for (i in 0 until m.length()) {
                m.optJSONObject(i)?.let { docThaoTacSua(it, zone) }?.let(sua::add)
            }
        }

        val xoa = ArrayList<Long>()
        data.optJSONArray("delete_ids")?.let { m ->
            for (i in 0 until m.length()) {
                val id = m.optLong(i, -1L)
                if (id > 0) xoa += id
            }
        }

        return DeXuat(
            loiDap = loiDap,
            yDinh = YDinh.SUA_LICH,
            themSuKien = them,
            suaSuKien = sua,
            xoaSuKien = xoa,
        )
    }

    // ---- Doc tung manh ----

    fun docLapLai(raw: String?): Int = when (raw?.trim()?.lowercase()) {
        "daily", "hàng ngày", "hang ngay" -> SuKien.LAP_NGAY
        "weekly", "hàng tuần", "hang tuan" -> SuKien.LAP_TUAN
        "monthly", "hàng tháng", "hang thang" -> SuKien.LAP_THANG
        "yearly", "hàng năm", "hang nam" -> SuKien.LAP_NAM
        else -> SuKien.LAP_KHONG
    }

    /** Tra ve -1 khi "khong nhac" — dung quy uoc cua [SuKien.nhacTruoc]. */
    fun docNhacTruoc(o: JSONObject, ten: String): Int {
        if (o.isNull(ten)) return -1
        val v = o.optInt(ten, -1)
        return if (v < 0) -1 else v.coerceAtMost(525_600)
    }

    /** "YYYY-MM-DD" -> ngay. null neu rong hoac hong. */
    fun docNgay(raw: String?): LocalDate? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        return try {
            LocalDate.parse(s)
        } catch (_: Exception) {
            null
        }
    }

    /** "HH:mm" -> (gio, phut). null neu hong. */
    fun docGio(raw: String?): Pair<Int, Int>? {
        val phan = raw?.trim()?.split(":") ?: return null
        if (phan.size != 2) return null
        val g = phan[0].toIntOrNull() ?: return null
        val p = phan[1].toIntOrNull() ?: return null
        if (g !in 0..23 || p !in 0..59) return null
        return g to p
    }

    /**
     * Dung mot su kien moi tu tham so AI.
     *
     * Chiu duoc su kien KEO DAI NHIEU NGAY: `date` la ngay bat dau, `end_date`
     * la ngay ket thuc (rong = cung ngay).
     */
    fun dungSuKien(o: JSONObject, zone: ZoneId = ZoneId.systemDefault()): SuKienMoi? {
        val ngay = docNgay(chuoi(o, "date")) ?: return null
        val ngayCuoi = docNgay(chuoi(o, "end_date")) ?: ngay
        val gioBatDau = chuoi(o, "start")
        val caNgay = o.optBoolean("all_day") || gioBatDau.isEmpty()

        val batDau: Long
        val ketThuc: Long
        if (caNgay) {
            batDau = mocCua(ngay, 0, 0, zone)
            // Ca ngay keo toi HET ngay ket thuc -> cong them mot ngay.
            val het = mocCua(ngayCuoi.plusDays(1), 0, 0, zone)
            ketThuc = if (het > batDau) het else mocCua(ngay.plusDays(1), 0, 0, zone)
        } else {
            val (g, p) = docGio(gioBatDau) ?: return null
            batDau = mocCua(ngay, g, p, zone)
            val gioCuoi = docGio(chuoi(o, "end"))
            // Gio ket thuc gan vao NGAY ket thuc, khong phai ngay bat dau.
            val tam = if (gioCuoi == null) batDau + 3_600_000L
            else mocCua(ngayCuoi, gioCuoi.first, gioCuoi.second, zone)
            ketThuc = if (tam > batDau) tam else batDau + 3_600_000L
        }

        val lap = docLapLai(chuoi(o, "repeat"))
        return SuKienMoi(
            tieuDe = chuoi(o, "title"),
            batDau = batDau,
            ketThuc = ketThuc,
            caNgay = caNgay,
            lapLai = lap,
            // Moc dung lap chi co nghia khi that su co lap.
            lapDen = if (lap == SuKien.LAP_KHONG) null
            else docNgay(chuoi(o, "repeat_until"))?.let { mocCua(it, 0, 0, zone) },
            nhacTruoc = docNhacTruoc(o, "remind_minutes"),
        )
    }

    /**
     * CHI giu nhung truong AI that su gui — truong vang mat nghia la giu nguyen.
     *
     * Nhan [zone] chu khong tu lay gio may: ca duong doc JSON deu chuyen mui gio
     * xuong theo tham so, rieng cho nay tu goi `systemDefault()` thi bai test
     * khong the kiem mui gio khac, ma loi lech ngay chi lo ra o may nguoi dung.
     */
    fun docThaoTacSua(o: JSONObject, zone: ZoneId = ZoneId.systemDefault()): SuaSuKienOp? {
        val id = o.optLong("id", -1L)
        if (id <= 0) return null

        fun chu(ten: String): String? = chuoi(o, ten).ifEmpty { null }

        // `has` van dung true khi model gui thang JSON null. Voi ba truong duoi
        // day "gui null" chinh la mot y dinh that ("bo nhac", "bo ngay dung
        // lap"), nen giu `has`; con truong chu thi da co `chuoi` lo.
        return SuaSuKienOp(
            id = id,
            tieuDe = chu("title"),
            ngay = docNgay(chuoi(o, "date")),
            batDau = chu("start"),
            ketThuc = chu("end"),
            caNgay = if (o.has("all_day")) o.optBoolean("all_day") else null,
            lapLai = if (o.has("repeat")) docLapLai(chuoi(o, "repeat")) else null,
            lapDen = if (o.has("repeat_until")) {
                docNgay(chuoi(o, "repeat_until"))?.let {
                    it.atStartOfDay(zone).toInstant().toEpochMilli()
                }
            } else {
                null
            },
            coLapDen = o.has("repeat_until"),
            nhacTruoc = if (o.has("remind_minutes")) docNhacTruoc(o, "remind_minutes") else null,
            coNhacTruoc = o.has("remind_minutes"),
        )
    }

    private fun mocCua(ngay: LocalDate, gio: Int, phut: Int, zone: ZoneId): Long =
        LocalDateTime.of(ngay, java.time.LocalTime.of(gio, phut)).atZone(zone).toInstant().toEpochMilli()
}
