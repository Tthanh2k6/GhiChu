package com.ghichu

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Nhan ngay gio tieng Viet cho man Lich trinh.
 *
 * Giu dung cach viet cua app Ghi chu Xiaomi: chu nhat la `CN`, thang viet chu
 * "Tháng 8" chu khong phai `08`.
 */
object NgayVN {

    /** Nhan cot ngay tren dau luoi: `T2`…`T7`, `CN`. */
    fun thuNgan(thu: DayOfWeek): String =
        if (thu == DayOfWeek.SUNDAY) "CN" else "T${thu.value + 1}"

    /** Tieu de tren cung: `Tháng 8, 2026`. */
    fun nhanThang(ngay: LocalDate): String = "Tháng ${ngay.monthValue}, ${ngay.year}"

    /**
     * Tieu de thang cua man Lich trinh: `Tháng 8`, khac nam thi `Tháng 8/2025`.
     * Lay nguyen cach viet cua ban demo — nam nay thi khoi ghi nam cho gon.
     */
    fun thangNgan(ngay: LocalDate, homNay: LocalDate = LocalDate.now()): String =
        if (ngay.year == homNay.year) "Tháng ${ngay.monthValue}"
        else "Tháng ${ngay.monthValue}/${ngay.year}"

    /** Nhan mui gio o goc trai dau luoi: `+7`. */
    fun lechGio(zone: ZoneId): String {
        val gio = zone.rules.getOffset(java.time.Instant.now()).totalSeconds / 3600
        return if (gio < 0) "$gio" else "+$gio"
    }

    /** Dong duoi thanh su kien sap toi: `Hôm nay · 14:30`, `9/8 · 08:00`. */
    fun nhanSapToi(sk: SuKien, batDau: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val ngay = ngayCua(batDau, zone)
        val cach = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(zone), ngay)
        val gio = if (sk.caNgay) "cả ngày" else gioPhut(batDau, zone)
        return when (cach) {
            0L -> "Hôm nay · $gio"
            1L -> "Ngày mai · $gio"
            -1L -> "Hôm qua · $gio"
            else -> "${ngay.dayOfMonth}/${ngay.monthValue} · $gio"
        }
    }

    /**
     * Tieu de khi mot tuan vat qua hai thang: `Tháng 7 – 8, 2026`,
     * hoac qua hai nam: `Th 12, 2026 – Th 1, 2027`.
     */
    fun nhanTuan(dauTuan: LocalDate): String {
        val cuoiTuan = dauTuan.plusDays(6)
        return when {
            dauTuan.year != cuoiTuan.year ->
                "Th ${dauTuan.monthValue}, ${dauTuan.year} – Th ${cuoiTuan.monthValue}, ${cuoiTuan.year}"
            dauTuan.monthValue != cuoiTuan.monthValue ->
                "Tháng ${dauTuan.monthValue} – ${cuoiTuan.monthValue}, ${dauTuan.year}"
            else -> nhanThang(dauTuan)
        }
    }

    fun gioPhut(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val t = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)
        return "%02d:%02d".format(t.hour, t.minute)
    }

    /** Nhan gio o cot ben trai luoi: `07:00`. */
    fun nhanGio(gio: Int): String = "%02d:00".format(gio)

    /** `Th 2, 3 Tháng 8` — dung o dong ngay trong hop sua su kien. */
    fun ngayDayDu(ngay: LocalDate): String =
        "${thuDai(ngay.dayOfWeek)}, ${ngay.dayOfMonth} Tháng ${ngay.monthValue}"

    private fun thuDai(thu: DayOfWeek): String =
        if (thu == DayOfWeek.SUNDAY) "Chủ nhật" else "Thứ ${thu.value + 1}"

    fun ngayCua(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun mocCua(ngay: LocalDate, gio: Int, phut: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
        ngay.atTime(gio, phut).atZone(zone).toInstant().toEpochMilli()

    /** Nua dem dau ngay, tinh bang mili-giay epoch. */
    fun dauNgay(ngay: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        ngay.atStartOfDay(zone).toInstant().toEpochMilli()

    /**
     * Nhan thoi gian tren THE ghi chu o man hinh chinh.
     *
     * Ba dang, dung y app Ghi chu von co (yeu cau chot trong docs/YEU-CAU.md):
     *   * trong 7 ngay gan day  -> `Th 2 22:47` (chu nhat viet `CN`)
     *   * cung nam              -> `12 Tháng 7`
     *   * khac nam              -> `6 Tháng 1, 2025`
     *
     * Vi sao khong dung `DateUtils.getRelativeTimeSpanString`: no tra ve chuoi
     * theo ngon ngu MAY. May dat tieng Anh thi ghi chu hien "2 days ago" xen
     * giua giao dien tieng Viet.
     */
    fun nhanDanhSach(
        millis: Long,
        homNay: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val ngay = ngayCua(millis, zone)
        return when {
            // 7 ngay gan day, ke ca hom nay. Ngay o TUONG LAI cung roi vao day —
            // ghi chu co the mang moc nhac nho o phia truoc.
            !ngay.isBefore(homNay.minusDays(6)) && !ngay.isAfter(homNay) ->
                thuNgan(ngay.dayOfWeek) + " " + gioPhut(millis, zone)
            ngay.year == homNay.year -> "${ngay.dayOfMonth} Tháng ${ngay.monthValue}"
            else -> "${ngay.dayOfMonth} Tháng ${ngay.monthValue}, ${ngay.year}"
        }
    }
}
