package com.ghichu

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Ap mot [DeXuat] cua AI vao du lieu that.
 *
 * CHI chay khi nguoi dung da bam xac nhan — AI **khong tu chay**. Day la yeu cau
 * da chot, ly do nguoi dung neu ro: tranh hieu nham y roi sua bay vao ghi chu.
 */
object AiApDung {

    /** Ket qua mot lan ap, de bao lai cho nguoi dung bang mot cau. */
    class KetQua(val xong: Boolean, val loi: String)

    fun ap(context: Context, kho: KhoDuLieu, dx: DeXuat, zone: ZoneId = ZoneId.systemDefault()): KetQua =
        when (dx.yDinh) {
            YDinh.TAO_GHI_CHU -> taoGhiChu(context, kho, dx)
            YDinh.SUA_GHI_CHU -> suaGhiChu(context, kho, dx)
            YDinh.SUA_LICH -> suaLich(context, kho, dx, zone)
            YDinh.CHAT -> KetQua(false, "Không có gì để làm.")
        }

    private fun taoGhiChu(context: Context, kho: KhoDuLieu, dx: DeXuat): KetQua {
        val markup = NoiDungGhiChu.tuMarkdown(dx.noiDungGhiChu)
        val id = KhoGhiChu(kho).taoGhiChu(dx.tieuDeGhiChu, markup)
        return if (id > 0) KetQua(true, "Đã tạo ghi chú.")
        else KetQua(false, "Không tạo được ghi chú.")
    }

    private fun suaGhiChu(context: Context, kho: KhoDuLieu, dx: DeXuat): KetQua {
        val ghiChu = KhoGhiChu(kho)
        val id = ghiChu.timTheoTuKhoa(dx.timGhiChu)
            ?: return KetQua(false, "Không tìm thấy ghi chú \"${dx.timGhiChu}\".")

        val markup = NoiDungGhiChu.tuMarkdown(dx.noiDungGhiChu)
        return if (ghiChu.suaGhiChu(id, markup, noiThem = !dx.vietDe)) {
            KetQua(true, if (dx.vietDe) "Đã viết lại ghi chú." else "Đã thêm vào ghi chú.")
        } else {
            KetQua(false, "Không sửa được ghi chú.")
        }
    }

    private fun suaLich(context: Context, kho: KhoDuLieu, dx: DeXuat, zone: ZoneId): KetQua {
        var them = 0
        var sua = 0
        var xoa = 0

        for (s in dx.themSuKien) {
            kho.luu(
                SuKien(
                    tieuDe = s.tieuDe,
                    batDau = s.batDau,
                    ketThuc = s.ketThuc,
                    caNgay = s.caNgay,
                    // May tu gan mau -> lay tiep hang cho. Nho vay AI xep mot
                    // thoi khoa bieu thi moi mon mot mau, khong phai ca bang
                    // cung mot mau xanh.
                    mau = HangMau.layTiep(context),
                    lapLai = s.lapLai,
                    nhacTruoc = s.nhacTruoc,
                    lapDen = s.lapDen,
                ),
            )
            them++
        }

        for (op in dx.suaSuKien) {
            val cu = kho.theoId(op.id) ?: continue
            kho.luu(gop(cu, op, zone))
            sua++
        }

        for (id in dx.xoaSuKien) {
            if (kho.theoId(id) == null) continue
            kho.xoa(id)
            xoa++
        }

        if (them + sua + xoa == 0) return KetQua(false, "Không có sự kiện nào khớp.")
        return KetQua(
            true,
            buildList {
                if (them > 0) add("thêm $them")
                if (sua > 0) add("sửa $sua")
                if (xoa > 0) add("xoá $xoa")
            }.joinToString(", ", prefix = "Đã ", postfix = " sự kiện."),
        )
    }

    /**
     * Gop thao tac sua vao su kien cu.
     *
     * Truong AI khong gui thi GIU NGUYEN. Nho vay "dời sang 8h tối" chi doi gio,
     * con ngay va do dai giu y nhu cu — chu khong bi reset ve mac dinh.
     */
    fun gop(cu: SuKien, op: SuaSuKienOp, zone: ZoneId = ZoneId.systemDefault()): SuKien {
        val batDauCu = LocalDateTime.ofInstant(Instant.ofEpochMilli(cu.batDau), zone)
        val doDai = (cu.ketThuc - cu.batDau).coerceAtLeast(0L)

        val ngay: LocalDate = op.ngay ?: batDauCu.toLocalDate()
        val gioBatDau = op.batDau?.let(AiDocDeXuat::docGio) ?: (batDauCu.hour to batDauCu.minute)

        val batDauMoi = LocalDateTime.of(ngay, java.time.LocalTime.of(gioBatDau.first, gioBatDau.second))
            .atZone(zone).toInstant().toEpochMilli()

        // Chi doi gio bat dau thi do dai giu nguyen; co gui gio ket thuc thi theo.
        val ketThucMoi = op.ketThuc?.let(AiDocDeXuat::docGio)?.let { (g, p) ->
            val t = LocalDateTime.of(ngay, java.time.LocalTime.of(g, p))
                .atZone(zone).toInstant().toEpochMilli()
            if (t > batDauMoi) t else batDauMoi + doDai
        } ?: (batDauMoi + doDai)

        return cu.copy(
            tieuDe = op.tieuDe ?: cu.tieuDe,
            batDau = batDauMoi,
            ketThuc = ketThucMoi,
            caNgay = op.caNgay ?: cu.caNgay,
            lapLai = op.lapLai ?: cu.lapLai,
            nhacTruoc = if (op.coNhacTruoc) (op.nhacTruoc ?: -1) else cu.nhacTruoc,
            lapDen = if (op.coLapDen) op.lapDen else cu.lapDen,
        )
    }

    /**
     * Ngay ket thuc de dien vao cot `end_date` cua bang gui cho AI.
     *
     * Cung ngay thi tra ve rong — dung quy uoc da dan cho model. Su kien CA NGAY
     * co `ketThuc` la 00:00 cua ngay HOM SAU (xem [AiDocDeXuat.dungSuKien]), nen
     * phai lui lai mot mili-giay, khong thi su kien ca ngay mot hom nao cung bi
     * bao la keo dai hai ngay.
     */
    fun ngayCuoiCua(s: SuKien, zone: ZoneId = ZoneId.systemDefault()): String {
        val dauNgay = Instant.ofEpochMilli(s.batDau).atZone(zone).toLocalDate()
        val mocCuoi = if (s.caNgay) (s.ketThuc - 1).coerceAtLeast(s.batDau) else s.ketThuc
        val cuoiNgay = Instant.ofEpochMilli(mocCuoi).atZone(zone).toLocalDate()
        return if (cuoiNgay > dauNgay) cuoiNgay.toString() else ""
    }

    /** Bang su kien gui kem cho AI, de no tra ra dung `id` khi muon sua/xoa. */
    fun bangSuKien(kho: KhoDuLieu, homNay: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<AiLoiNhac.SuKienNgan> {
        val tu = homNay.minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        val den = homNay.plusDays(60).atStartOfDay(zone).toInstant().toEpochMilli()

        return kho.trongKhoang(tu, den).take(AiLoiNhac.TRAN_SU_KIEN).map { s ->
            val b = LocalDateTime.ofInstant(Instant.ofEpochMilli(s.batDau), zone)
            val k = LocalDateTime.ofInstant(Instant.ofEpochMilli(s.ketThuc), zone)
            AiLoiNhac.SuKienNgan(
                id = s.id,
                tieuDe = s.tieuDe,
                ngay = b.toLocalDate().toString(),
                ngayCuoi = ngayCuoiCua(s, zone),
                batDau = if (s.caNgay) "" else "%02d:%02d".format(b.hour, b.minute),
                ketThuc = if (s.caNgay) "" else "%02d:%02d".format(k.hour, k.minute),
                caNgay = s.caNgay,
                lapLai = when (s.lapLai) {
                    SuKien.LAP_NGAY -> "daily"
                    SuKien.LAP_TUAN -> "weekly"
                    SuKien.LAP_THANG -> "monthly"
                    SuKien.LAP_NAM -> "yearly"
                    else -> "none"
                },
                lapDen = s.lapDen?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().toString() }.orEmpty(),
                nhacTruoc = s.nhacTruoc,
            )
        }
    }
}
