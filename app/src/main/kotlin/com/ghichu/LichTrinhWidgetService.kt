package com.ghichu

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Cấp từng dòng cho `ListView` cuộn của [LichTrinhWidget].
 *
 * Đây là thứ làm cho danh sách CUỘN được: `RemoteViews` tự nó không cuộn, nhưng
 * `ListView` + một `RemoteViewsService` thì có. Hệ thống gọi service này qua
 * `setRemoteAdapter`, rồi factory dưới đây dựng mỗi dòng như một `RemoteViews`
 * nhỏ từ layout `ghichu_widget_muc`.
 *
 * Chạy trong TIẾN TRÌNH của app (không phải launcher), nên đọc thẳng CSDL qua
 * `Ung.cua(context)` được. Nhưng nó là tiến trình có thể bị hệ thống dựng dậy
 * bất kỳ lúc nào, nên mọi truy cập CSDL đều bọc try/catch — hỏng thì trả danh
 * sách rỗng, widget hiện "không có sự kiện" chứ không gãy.
 */
class LichTrinhWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        LichTrinhFactory(applicationContext)
}

private class LichTrinhFactory(private val ctx: Context) : RemoteViewsService.RemoteViewsFactory {

    private val zone: ZoneId = ZoneId.systemDefault()

    /** Bản chụp dữ liệu — chỉ đổi ở onDataSetChanged, đọc ở getViewAt. */
    @Volatile private var dong: List<LanHien> = emptyList()
    private var mau: BangMau = BangMau.cua(ctx)

    override fun onCreate() {}

    /**
     * Hệ thống gọi mỗi lần cần nạp lại (lúc gắn adapter và mỗi
     * `notifyAppWidgetViewDataChanged`). Đây là nơi DUY NHẤT chạm CSDL — chạy
     * trên luồng nền của hệ thống, an toàn cho việc đọc lâu.
     */
    override fun onDataSetChanged() {
        mau = BangMau.cua(ctx)
        dong = docSapToi()
    }

    override fun onDestroy() {}

    override fun getCount(): Int = dong.size

    override fun getViewAt(pos: Int): RemoteViews {
        val hang = RemoteViews(ctx.packageName, R.layout.ghichu_widget_muc)
        val lan = dong.getOrNull(pos) ?: return hang

        hang.setTextViewText(R.id.ghichu_widget_ngay, nhanNgay(lan.batDau))
        hang.setTextViewText(R.id.ghichu_widget_ten, tenCua(lan))
        hang.setTextViewText(R.id.ghichu_widget_gio, nhanGio(lan))
        // Việc đang diễn ra: tô vàng cho nổi; còn lại theo màu giao diện.
        hang.setTextColor(
            R.id.ghichu_widget_ten,
            if (dangDienRa(lan)) mau.nhan else mau.chuChinh,
        )
        hang.setTextColor(R.id.ghichu_widget_ngay, mau.chuPhu)
        hang.setTextColor(R.id.ghichu_widget_gio, mau.chuPhu)

        // Chạm một dòng: mở app sang tab Lịch trình (không mở thẳng hộp sửa được
        // — hộp đó nằm trong Activity, không có đường vào riêng). fill-in ghép
        // với pending-intent-template đặt ở [LichTrinhWidget].
        hang.setOnClickFillInIntent(
            R.id.ghichu_widget_hang,
            Intent().putExtra(ManHinhChinhActivity.KHOA_TAB, 1),
        )
        return hang
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(pos: Int): Long = pos.toLong()
    override fun hasStableIds(): Boolean = false

    // ---- Dữ liệu (bê từ LichTrinhWidget cũ sang, một nguồn duy nhất) ----

    private fun docSapToi(): List<LanHien> {
        val bayGio = System.currentTimeMillis()
        val den = bayGio + SO_NGAY_NHIN * 24 * 3_600_000L
        val kho = Ung.cua(ctx).kho
        return try {
            kho.trongKhoang(bayGio, den)
                .flatMap { LichLapLai.moRong(it, bayGio, den, zone) }
                .filter { it.ketThuc > bayGio }
                .sortedBy { it.batDau }
                .take(TOI_DA_DONG)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun dangDienRa(lan: LanHien): Boolean {
        val bayGio = System.currentTimeMillis()
        return lan.batDau <= bayGio && lan.ketThuc > bayGio
    }

    private fun tenCua(lan: LanHien): String = lan.suKien.tieuDe.ifBlank { "(không tiêu đề)" }

    private fun nhanNgay(moc: Long): String {
        val ngay = Instant.ofEpochMilli(moc).atZone(zone).toLocalDate()
        return when (java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(zone), ngay)) {
            0L -> "Hôm nay"
            1L -> "Ngày mai"
            else -> "${NgayVN.thuNgan(ngay.dayOfWeek)} ${ngay.dayOfMonth}/${ngay.monthValue}"
        }
    }

    private fun nhanGio(lan: LanHien): String =
        if (lan.suKien.caNgay) "Cả ngày"
        else "${NgayVN.gioPhut(lan.batDau, zone)} – ${NgayVN.gioPhut(lan.ketThuc, zone)}"

    companion object {
        /** Cuộn được rồi nên nới rộng; vẫn chặn trần cho khỏi vô hạn. */
        private const val TOI_DA_DONG = 200
        private const val SO_NGAY_NHIN = 60L
    }
}
