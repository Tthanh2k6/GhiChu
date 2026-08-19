package com.ghichu

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

/**
 * Widget man hinh chinh: vai ghi chu gan nhat, **nen kinh**.
 *
 * VE HAI CHU "KINH" — noi thang de khoi vo mong: widget cua ung dung **khong lam
 * mo duoc hinh nen phia sau**. Android khong mo API do; chi launcher moi lam
 * duoc voi widget cua chinh no. Cai lam duoc, va trong van rat ra kinh, la nen
 * ban trong + chuyen sac cheo + vien sang + bo goc lon — xem
 * `res/drawable/ghichu_kinh_toi.xml`. Mot so launcher (co MIUI) TU lam mo phia
 * sau widget; gap launcher do thi thanh kinh that ma khong phai lam gi them.
 *
 * BON DIEU PHAI BIET — bai hoc da tra gia o du an cu:
 *
 * 1. **KHONG dung the `<View>` tron trong bo cuc widget.** `RemoteViews` chi
 *    nhan LinearLayout / FrameLayout / TextView / ImageView… Sai la widget bao
 *    "Không thể tải tiện ích", ma loi do khong chi ra dong nao sai.
 *
 * 2. **Danh sach KHONG cuon.** Da thu `RemoteViewsService` va
 *    `RemoteCollectionItems` o du an cu roi bo ca hai; gio them thang tung dong
 *    bang `addView`.
 *
 * 3. **Phai goi `removeAllViews` truoc khi them dong**, khong thi moi lan cap
 *    nhat la danh sach dai gap doi.
 *
 * 4. **Doi bo cuc ma widget van ket loi** thi go widget ra roi them lai —
 *    launcher nho trang thai loi.
 */
class GhiChuWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, quanLy: AppWidgetManager, ids: IntArray) {
        for (id in ids) veMot(context, quanLy, id)
    }

    private fun veMot(context: Context, quanLy: AppWidgetManager, widgetId: Int) {
        val o = RemoteViews(context.packageName, R.layout.ghichu_widget_ghichu)
        val mau = BangMau.cua(context)

        o.setInt(
            R.id.ghichu_gc_goc, "setBackgroundResource",
            if (mau.toi) R.drawable.ghichu_kinh_toi else R.drawable.ghichu_kinh_sang,
        )
        o.setTextColor(R.id.ghichu_gc_dau, mau.chuChinh)
        o.setTextColor(R.id.ghichu_gc_them, mau.nhan)
        o.setInt(R.id.ghichu_gc_vach, "setBackgroundColor", mau.vienHang)

        o.setOnClickPendingIntent(R.id.ghichu_gc_goc, moApp(context, moiGhiChu = false))
        o.setOnClickPendingIntent(R.id.ghichu_gc_them, moApp(context, moiGhiChu = true))

        o.removeAllViews(R.id.ghichu_gc_khung)
        val ds = doc(context)

        if (ds.isEmpty()) {
            val hang = RemoteViews(context.packageName, R.layout.ghichu_widget_ghichu_muc)
            hang.setTextViewText(R.id.ghichu_gc_ten, "Chưa có ghi chú")
            hang.setTextColor(R.id.ghichu_gc_ten, mau.chuPhu)
            hang.setViewVisibility(R.id.ghichu_gc_phu, View.GONE)
            o.addView(R.id.ghichu_gc_khung, hang)
        } else {
            for (g in ds) {
                val hang = RemoteViews(context.packageName, R.layout.ghichu_widget_ghichu_muc)
                hang.setTextViewText(R.id.ghichu_gc_ten, g.tieuDeHien().ifBlank { "Không có văn bản" })
                hang.setTextColor(R.id.ghichu_gc_ten, if (g.ghim) mau.nhan else mau.chuChinh)

                val phu = g.xemTruoc(1).ifBlank { NgayVN.nhanDanhSach(g.ngaySua) }
                hang.setTextViewText(R.id.ghichu_gc_phu, phu)
                hang.setTextColor(R.id.ghichu_gc_phu, mau.chuPhu)
                o.addView(R.id.ghichu_gc_khung, hang)
            }
        }

        quanLy.updateAppWidget(widgetId, o)
    }

    /**
     * Ghi chu de hien.
     *
     * Ghi chu RIENG TU khong bao gio len widget: widget nam ngay tren man hinh
     * chinh, ai cam may cung doc duoc — day dung la thu nguoi dung co y giau.
     * [KhoGhiChu.danhSach] mac dinh da loc, nhung ghi ra day de khong ai "tien
     * tay" doi tham so.
     */
    private fun doc(context: Context): List<GhiChu> = try {
        Ung.cua(context).ghiChu.danhSach(riengTu = false).take(TOI_DA_DONG)
    } catch (_: Exception) {
        emptyList()
    }

    private fun moApp(context: Context, moiGhiChu: Boolean): PendingIntent? {
        val mo = if (moiGhiChu) {
            Intent(context, SuaGhiChuActivity::class.java)
        } else {
            context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        }
        mo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        // Ma yeu cau phai KHAC nhau giua hai nut, khong thi cai sau de len cai
        // truoc va ca hai cung mo mot cho.
        return PendingIntent.getActivity(
            context, if (moiGhiChu) 1 else 0, mo,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Them nhieu hon chieu cao widget thi phan duoi bi cat, khong tran. */
        private const val TOI_DA_DONG = 8

        /** Bao widget ve lai ngay. Goi moi khi ghi chu doi. */
        fun capNhat(context: Context) {
            try {
                val ung = context.applicationContext
                val quanLy = AppWidgetManager.getInstance(ung) ?: return
                val ids = quanLy.getAppWidgetIds(ComponentName(ung, GhiChuWidget::class.java))
                if (ids.isEmpty()) return

                ung.sendBroadcast(
                    Intent(ung, GhiChuWidget::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    },
                )
            } catch (_: Exception) {
                // Chua dat widget nao thi thoi — khong duoc lam vo cho goi.
            }
        }
    }
}
