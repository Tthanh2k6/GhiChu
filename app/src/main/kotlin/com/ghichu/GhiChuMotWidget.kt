package com.ghichu

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Widget 2x2 ghim MOT ghi chu duoc chi dinh. Bam vao mo dung ghi chu do.
 *
 * Ghi chu nao thi do man chon [ChonGhiChuWidgetActivity] luu luc keo widget ra —
 * moi widgetId ung mot `note _id`, giu trong SharedPreferences. Bam la mo thang
 * [SuaGhiChuActivity] cua ghi chu do.
 *
 * Nen kinh va cac bai hoc RemoteViews: xem [GhiChuWidget].
 */
class GhiChuMotWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, quanLy: AppWidgetManager, ids: IntArray) {
        for (id in ids) veMot(context, quanLy, id)
    }

    /** Go widget thi xoa luon anh xa cua no — khong de rac lai trong prefs. */
    override fun onDeleted(context: Context, ids: IntArray) {
        val e = prefs(context).edit()
        for (id in ids) e.remove(KHOA + id)
        e.apply()
    }

    private fun veMot(context: Context, quanLy: AppWidgetManager, widgetId: Int) {
        val mau = BangMau.cua(context)
        val o = RemoteViews(context.packageName, R.layout.ghichu_widget_mot)
        o.setInt(
            R.id.ghichu_mot_goc, "setBackgroundResource",
            if (mau.toi) R.drawable.ghichu_kinh_toi else R.drawable.ghichu_kinh_sang,
        )
        o.setTextColor(R.id.ghichu_mot_tieu_de, mau.chuChinh)
        o.setTextColor(R.id.ghichu_mot_than, mau.chuPhu)
        o.setTextColor(R.id.ghichu_mot_gio, mau.chuMo)

        val noteId = prefs(context).getLong(KHOA + widgetId, 0L)
        val g = if (noteId > 0) doc(context, noteId) else null

        if (g == null) {
            o.setTextViewText(R.id.ghichu_mot_tieu_de, "Ghi chú không còn")
            o.setTextViewText(R.id.ghichu_mot_than, "Ghi chú đã bị xoá. Gỡ widget rồi thêm lại.")
            o.setTextViewText(R.id.ghichu_mot_gio, "")
            o.setOnClickPendingIntent(R.id.ghichu_mot_goc, moApp(context))
        } else {
            o.setTextViewText(R.id.ghichu_mot_tieu_de, g.tieuDeHien().ifBlank { "Không có văn bản" })
            val than = g.xemTruoc(6)
            o.setTextViewText(R.id.ghichu_mot_than, than)
            o.setTextViewText(R.id.ghichu_mot_gio, NgayVN.nhanDanhSach(g.ngaySua))
            o.setOnClickPendingIntent(R.id.ghichu_mot_goc, moGhiChu(context, noteId, widgetId))
        }
        quanLy.updateAppWidget(widgetId, o)
    }

    /**
     * Ghi chu de hien. Ghi chu RIENG TU khong hien noi dung tren widget (nam ngay
     * tren man hinh chinh, ai cam may cung doc) — chi hien tieu de mo khoa.
     */
    private fun doc(context: Context, id: Long): GhiChu? = try {
        Ung.cua(context).ghiChu.theoId(id)?.takeIf { !it.daXoa }
    } catch (_: Exception) {
        null
    }

    private fun moGhiChu(context: Context, noteId: Long, widgetId: Int): PendingIntent {
        val i = Intent(context, SuaGhiChuActivity::class.java)
            .putExtra(SuaGhiChuActivity.KHOA_ID, noteId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        // Ma yeu cau = widgetId de moi widget co PendingIntent rieng, khong de len nhau.
        return PendingIntent.getActivity(
            context, widgetId, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun moApp(context: Context): PendingIntent? {
        val mo = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        mo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, mo,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TEP = "ghichu_widget_mot"
        private const val KHOA = "wm_"

        fun prefs(context: Context) =
            context.applicationContext.getSharedPreferences(TEP, Context.MODE_PRIVATE)

        /** Man chon goi khi nguoi dung da chon ghi chu cho widget. */
        fun gan(context: Context, widgetId: Int, noteId: Long) {
            prefs(context).edit().putLong(KHOA + widgetId, noteId).apply()
        }

        /** Bao moi widget mot-ghi-chu ve lai. Goi khi ghi chu doi (qua KhoDuLieu.baoDoi). */
        fun capNhat(context: Context) {
            try {
                val ung = context.applicationContext
                val quanLy = AppWidgetManager.getInstance(ung) ?: return
                val ids = quanLy.getAppWidgetIds(ComponentName(ung, GhiChuMotWidget::class.java))
                if (ids.isEmpty()) return
                ung.sendBroadcast(
                    Intent(ung, GhiChuMotWidget::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    },
                )
            } catch (_: Exception) {
                // Chua dat widget nao thi thoi.
            }
        }
    }
}
