package com.ghichu

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Mot su kien trong Lich trinh.
 *
 * Moc thoi gian luu bang mili-giay epoch (giong cach app Ghi chu Xiaomi luu
 * `alert_date`, `created_date`) de sau nay noi du lieu voi nhau cho de.
 */
data class SuKien(
    val id: Long = 0L,
    val tieuDe: String = "",
    val ghiChu: String = "",
    val batDau: Long,
    val ketThuc: Long,
    val caNgay: Boolean = false,
    /** Chi so trong [MauSuKien.BANG], 0..7. */
    val mau: Int = 0,
    val lapLai: Int = LAP_KHONG,
    /** So phut nhac truoc gio bat dau. -1 = khong nhac. */
    val nhacTruoc: Int = -1,
    val diaDiem: String = "",
    /**
     * Moc DUNG lap (mili-giay). null = lap mai.
     *
     * Chi co nghia khi [lapLai] khac [LAP_KHONG]. Lan lap cuoi cung la lan bat
     * dau vao hoac truoc NGAY nay — so theo ngay, khong so theo gio.
     */
    val lapDen: Long? = null,
) {
    companion object {
        const val LAP_KHONG = 0
        const val LAP_NGAY = 1
        const val LAP_TUAN = 2
        const val LAP_THANG = 3
        const val LAP_NAM = 4

        val TEN_LAP = arrayOf("Không lặp", "Hằng ngày", "Hằng tuần", "Hằng tháng", "Hằng năm")

        /** Cac moc nhac truoc bay san, tinh bang phut. */
        val NHAC_PHUT = intArrayOf(-1, 0, 5, 10, 15, 30, 60, 120, 1440)
        val TEN_NHAC = arrayOf(
            "Không nhắc", "Đúng giờ", "Trước 5 phút", "Trước 10 phút", "Trước 15 phút",
            "Trước 30 phút", "Trước 1 tiếng", "Trước 2 tiếng", "Trước 1 ngày",
        )
    }
}

/**
 * Mot lan xuat hien cu the tren luoi tuan.
 *
 * Su kien lap lai sinh ra nhieu [LanHien] tu cung mot [SuKien]; [batDau]/[ketThuc]
 * o day la moc that cua lan do, khong phai moc goc.
 */
data class LanHien(
    val suKien: SuKien,
    val batDau: Long,
    val ketThuc: Long,
)

object MauSuKien {

    /**
     * 8 mau khoi — lay nguyen bang mau cua ban demo
     * (D:\Project\Note, lib/models/calendar_event.dart, `EventColors.palette`).
     *
     * Khoi su kien ve bang mau nay o do duc 0.3 tren nen den, chu thi tron voi
     * trang 0.55 — nen mau goc phai dam vua du, doi mau khac la chu mat net.
     */
    val BANG = intArrayOf(
        0xFF4A90D9.toInt(), // xanh duong
        0xFF34A853.toInt(), // xanh la
        0xFFE8A33D.toInt(), // vang ho phach
        0xFFE05252.toInt(), // do
        0xFF9B6DD6.toInt(), // tim
        0xFF00A9A5.toInt(), // ngoc
        0xFFE87BA0.toInt(), // hong
        0xFF8D8D8D.toInt(), // xam
    )

    val TEN = arrayOf("Xanh dương", "Xanh lá", "Vàng", "Đỏ", "Tím", "Ngọc", "Hồng", "Xám")

    fun cua(chiSo: Int): Int = BANG[((chiSo % BANG.size) + BANG.size) % BANG.size]
}

/**
 * Trai su kien lap lai thanh cac lan xuat hien roi vao khoang `[tu, den)`.
 *
 * Cach lam: buoc tu moc goc theo don vi lap cho toi khi vuot qua `den`. Neu moc
 * goc nam truoc `tu` rat xa (vd su kien hang ngay tao tu nam ngoai) thi nhay
 * thang toi gan `tu` truoc, khoi lap vo ich.
 */
object LichLapLai {

    /** Chan tren so lan lap cho mot su kien, de mot du lieu hong khong treo giao dien. */
    private const val TRAN_LAN = 400

    fun moRong(sk: SuKien, tu: Long, den: Long, zone: ZoneId): List<LanHien> {
        val thoiLuong = (sk.ketThuc - sk.batDau).coerceAtLeast(0L)

        if (sk.lapLai == SuKien.LAP_KHONG) {
            return if (sk.batDau < den && sk.ketThuc > tu) {
                listOf(LanHien(sk, sk.batDau, sk.ketThuc))
            } else {
                emptyList()
            }
        }

        val goc = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(sk.batDau), zone)
        val moc = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(tu), zone)
        var hienTai = nhayToiGan(goc, moc, sk.lapLai)

        // Moc dung lap so theo NGAY: "lap den 9/11" nghia la buoi ngay 9/11 van
        // con, du no bat dau luc 14h con moc luu la nua dem.
        val ngayDung = sk.lapDen?.let {
            java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
        }

        val ra = ArrayList<LanHien>()
        var dem = 0
        while (dem++ < TRAN_LAN) {
            if (ngayDung != null && hienTai.toLocalDate().isAfter(ngayDung)) break
            val batDau = hienTai.atZone(zone).toInstant().toEpochMilli()
            if (batDau >= den) break
            val ketThuc = batDau + thoiLuong
            if (ketThuc > tu) ra.add(LanHien(sk, batDau, ketThuc))
            hienTai = buocTiep(hienTai, sk.lapLai)
        }
        return ra
    }

    /**
     * Dua moc goc tien gan toi `dich` ma van giu dung nhip lap.
     *
     * Voi lap ngay/tuan thi tinh thang ra so buoc. Voi thang/nam thi buoc dan -
     * chenh lech toi da cung chi vai tram vong, khong dang ngai.
     */
    private fun nhayToiGan(goc: LocalDateTime, dich: LocalDateTime, kieu: Int): LocalDateTime {
        if (!goc.isBefore(dich)) return goc
        return when (kieu) {
            SuKien.LAP_NGAY -> {
                val soNgay = java.time.temporal.ChronoUnit.DAYS.between(goc.toLocalDate(), dich.toLocalDate())
                goc.plusDays(soNgay.coerceAtLeast(0))
            }
            SuKien.LAP_TUAN -> {
                val soNgay = java.time.temporal.ChronoUnit.DAYS.between(goc.toLocalDate(), dich.toLocalDate())
                goc.plusWeeks((soNgay / 7).coerceAtLeast(0))
            }
            else -> {
                var t = goc
                var dem = 0
                while (t.isBefore(dich) && dem++ < TRAN_LAN) t = buocTiep(t, kieu)
                // Lui mot buoc: su kien bat dau truoc `dich` van co the con dang dien ra.
                if (dem > 0) luiMot(t, kieu) else t
            }
        }
    }

    private fun buocTiep(t: LocalDateTime, kieu: Int): LocalDateTime = when (kieu) {
        SuKien.LAP_NGAY -> t.plusDays(1)
        SuKien.LAP_TUAN -> t.plusWeeks(1)
        SuKien.LAP_THANG -> t.plusMonths(1)
        SuKien.LAP_NAM -> t.plusYears(1)
        else -> t.plusYears(100)
    }

    private fun luiMot(t: LocalDateTime, kieu: Int): LocalDateTime = when (kieu) {
        SuKien.LAP_NGAY -> t.minusDays(1)
        SuKien.LAP_TUAN -> t.minusWeeks(1)
        SuKien.LAP_THANG -> t.minusMonths(1)
        SuKien.LAP_NAM -> t.minusYears(1)
        else -> t
    }
}

/** Thu 2 cua tuan chua [ngay] — tuan o Viet Nam bat dau tu thu 2. */
fun dauTuanCua(ngay: LocalDate): LocalDate =
    ngay.minusDays((ngay.dayOfWeek.value - 1).toLong())
