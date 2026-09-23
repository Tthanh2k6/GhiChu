package com.ghichu

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

/**
 * Widget màn hình chính: các sự kiện SẮP TỚI, danh sách CUỘN được.
 *
 * App gốc của Xiaomi đã có sẵn widget Ghi chú (2x1, 2x2, 4x2, 4x4, 8x4) — ta giữ
 * nguyên. Cái thiếu là widget cho phần THÊM VÀO, tức Lịch trình.
 *
 * KHÁC bản mod cũ: danh sách nay dùng `ListView` + [LichTrinhWidgetService] nên
 * CUỘN được — lịch dài bao nhiêu cũng xem hết, không còn cắt ở đáy widget. Bản
 * mod cũ phải chèn từng dòng bằng `addView` vì không tiêm nổi một `Service` vào
 * APK hệ thống của Xiaomi; app độc lập này tự chủ manifest nên làm đúng bài được.
 *
 * BA ĐIỀU PHẢI BIẾT:
 *
 * 1. **Nền kính dùng chung với [GhiChuWidget]** — widget không làm mờ được hình
 *    nền phía sau; đây là nền bán trong + viền sáng, vẫn rất ra kính.
 *
 * 2. **`ListView` bắt buộc có `RemoteViewsService`** khai báo trong manifest với
 *    quyền `BIND_REMOTEVIEWS`. Mỗi lần dữ liệu đổi phải gọi
 *    `notifyAppWidgetViewDataChanged` thì danh sách mới nạp lại.
 *
 * 3. **Đổi bố cục mà widget vẫn kẹt lỗi** thì gỡ widget ra rồi thêm lại —
 *    launcher nhớ trạng thái lỗi. Hoặc `adb shell am force-stop <launcher>`.
 */
class LichTrinhWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, quanLy: AppWidgetManager, ids: IntArray) {
        for (id in ids) veMot(context, quanLy, id)
        // Nạp (lại) danh sách sau khi đã gắn adapter cho từng widget.
        quanLy.notifyAppWidgetViewDataChanged(ids, R.id.ghichu_widget_ds)
    }

    private fun veMot(context: Context, quanLy: AppWidgetManager, widgetId: Int) {
        val o = RemoteViews(context.packageName, R.layout.ghichu_widget_lich)
        val mau = BangMau.cua(context)

        // --- Nền + màu chữ theo giao diện sáng/tối ---
        o.setInt(
            R.id.ghichu_widget_goc, "setBackgroundResource",
            if (mau.toi) R.drawable.ghichu_kinh_toi else R.drawable.ghichu_kinh_sang,
        )
        o.setTextColor(R.id.ghichu_widget_dau, mau.chuChinh)
        o.setInt(R.id.ghichu_widget_vach, "setBackgroundColor", mau.vienHang)
        o.setTextColor(R.id.ghichu_widget_trong, mau.chuPhu)

        // --- Gắn nguồn dòng cho ListView ---
        val nguon = Intent(context, LichTrinhWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            // Mỗi widget một factory riêng: hệ thống phân biệt adapter theo cả
            // data-uri, thiếu cái này là nhiều widget dùng chung một factory.
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        o.setRemoteAdapter(R.id.ghichu_widget_ds, nguon)
        o.setEmptyView(R.id.ghichu_widget_ds, R.id.ghichu_widget_trong)
        o.setPendingIntentTemplate(R.id.ghichu_widget_ds, moAppTemplate(context))

        // Chạm tiêu đề cũng mở app (vùng ngoài danh sách).
        moApp(context)?.let { o.setOnClickPendingIntent(R.id.ghichu_widget_dau, it) }

        quanLy.updateAppWidget(widgetId, o)
    }

    // ---- Mở app ----

    /**
     * Template cho các dòng: mỗi dòng ghép thêm phần riêng qua fill-in intent
     * (xem [LichTrinhWidgetService]). Template BẮT BUỘC dùng
     * `FLAG_UPDATE_CURRENT | FLAG_MUTABLE` để fill-in ghép vào được.
     */
    private fun moAppTemplate(context: Context): PendingIntent? {
        val mo = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        mo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 1, mo, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private fun moApp(context: Context): PendingIntent? {
        val mo = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        // Widget Lịch trình thì mở thẳng tab Lịch trình, không dừng ở tab Ghi chú.
        mo.putExtra(ManHinhChinhActivity.KHOA_TAB, 1)
        mo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, mo, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /**
         * Báo widget vẽ lại NGAY.
         *
         * Gọi mỗi khi dữ liệu sự kiện đổi — đừng chờ hết chu kỳ 30 phút, không thì
         * người dùng vừa thêm việc mà widget vẫn hiện cái cũ. Vừa đẩy lại lớp
         * ngoài (nền/màu theo giao diện) vừa báo ListView nạp lại dòng.
         */
        fun capNhat(context: Context) {
            try {
                val ung = context.applicationContext
                val quanLy = AppWidgetManager.getInstance(ung) ?: return
                val ids = quanLy.getAppWidgetIds(ComponentName(ung, LichTrinhWidget::class.java))
                if (ids.isEmpty()) return

                ung.sendBroadcast(
                    Intent(ung, LichTrinhWidget::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    },
                )
                // Đề phòng broadcast tới muộn: giục ListView nạp lại ngay.
                quanLy.notifyAppWidgetViewDataChanged(ids, R.id.ghichu_widget_ds)
            } catch (_: Exception) {
                // Chưa đặt widget nào thì thôi — không được làm vỡ chỗ gọi.
            }
        }
    }
}
