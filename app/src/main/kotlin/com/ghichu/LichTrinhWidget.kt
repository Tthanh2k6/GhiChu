package com.ghichu

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Widget man hinh chinh: cac su kien SAP TOI.
 *
 * App goc cua Xiaomi da co san widget Ghi chu (2x1, 2x2, 4x2, 4x4, 8x4) — ta giu
 * nguyen. Cai thieu la widget cho phan THEM VAO, tuc Lich trinh; ban demo goi no
 * la widget "Công việc".
 *
 * BON DIEU PHAI BIET — deu la bai hoc da tra gia o du an cu:
 *
 * 1. **Nen kinh dung chung voi [GhiChuWidget]** — xem giai thich ve hai chu
 *    "kinh" o ben do. Tom tat: widget khong lam mo duoc hinh nen phia sau, day
 *    la nen ban trong + vien sang, va no van rat ra kinh.
 *
 * 2. **KHONG dung the `<View>` tron trong bo cuc widget.** `RemoteViews` chi nhan
 *    LinearLayout / FrameLayout / TextView / ImageView… Sai la widget bao
 *    "Không thể tải tiện ích", ma loi do khong chi ra dong nao sai.
 *
 * 3. **Danh sach KHONG cuon.** Da thu `RemoteViewsService` va
 *    `RemoteCollectionItems` o du an cu roi bo ca hai; gio them thang tung dong
 *    bang `addView`, widget cao thi hien nhieu dong, thap thi phan duoi bi cat.
 *
 * 4. **Doi bo cuc ma widget van ket loi** thi go widget ra roi them lai —
 *    launcher nho trang thai loi. Hoac `adb shell am force-stop com.miui.home`.
 */
class LichTrinhWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, quanLy: AppWidgetManager, ids: IntArray) {
        for (id in ids) veMot(context, quanLy, id)
    }

    private fun veMot(context: Context, quanLy: AppWidgetManager, widgetId: Int) {
        val o = RemoteViews(context.packageName, R.layout.ghichu_widget_lich)
        val mau = BangMau.cua(context)
        val zone = ZoneId.systemDefault()

        // --- Nen + mau chu theo giao dien sang/toi ---
        o.setInt(
            R.id.ghichu_widget_goc, "setBackgroundResource",
            if (mau.toi) R.drawable.ghichu_kinh_toi else R.drawable.ghichu_kinh_sang,
        )
        o.setTextColor(R.id.ghichu_widget_dau, mau.chuChinh)
        o.setTextColor(R.id.ghichu_widget_ngay_dau, mau.nhan)
        o.setTextColor(R.id.ghichu_widget_gio_dau, mau.chuPhu)
        o.setInt(R.id.ghichu_widget_vach, "setBackgroundColor", mau.vienHang)

        // Cham vao dau cung mo app. Khong mo thang mot su kien duoc: hop sua su
        // kien la hop thoai nam trong Activity, khong co duong vao rieng.
        moApp(context)?.let { o.setOnClickPendingIntent(R.id.ghichu_widget_goc, it) }

        val sapToi = docSapToi(context, zone)

        // --- Dong tren cung: viec gan nhat ---
        if (sapToi.isEmpty()) {
            o.setTextViewText(R.id.ghichu_widget_ngay_dau, "")
            o.setTextViewText(R.id.ghichu_widget_ten_dau, "Không có sự kiện sắp tới")
            o.setTextColor(R.id.ghichu_widget_ten_dau, mau.chuPhu)
            o.setViewVisibility(R.id.ghichu_widget_gio_dau, View.GONE)
        } else {
            val dau = sapToi.first()
            o.setTextViewText(R.id.ghichu_widget_ngay_dau, nhanNgay(dau.batDau, zone))
            o.setTextViewText(R.id.ghichu_widget_ten_dau, tenCua(dau))
            o.setTextColor(
                R.id.ghichu_widget_ten_dau,
                if (dangDienRa(dau)) mau.nhan else mau.chuChinh,
            )
            o.setViewVisibility(R.id.ghichu_widget_gio_dau, View.VISIBLE)
            o.setTextViewText(R.id.ghichu_widget_gio_dau, nhanGio(dau, zone))
        }

        // --- Cac dong con lai ---
        val khung = R.id.ghichu_widget_khung
        o.removeAllViews(khung)
        val hangBoCuc = R.layout.ghichu_widget_muc

        if (hangBoCuc != 0 && sapToi.size > 1) {
            for (lan in sapToi.drop(1).take(TOI_DA_DONG)) {
                val hang = RemoteViews(context.packageName, hangBoCuc)
                hang.setTextViewText(R.id.ghichu_widget_ngay, nhanNgay(lan.batDau, zone))
                hang.setTextViewText(R.id.ghichu_widget_ten, tenCua(lan))
                hang.setTextColor(
                    R.id.ghichu_widget_ten,
                    if (dangDienRa(lan)) mau.nhan else mau.chuChinh,
                )
                hang.setTextColor(R.id.ghichu_widget_ngay, mau.chuPhu)
                hang.setTextColor(R.id.ghichu_widget_gio, mau.chuPhu)
                hang.setTextViewText(R.id.ghichu_widget_gio, nhanGio(lan, zone))
                o.addView(khung, hang)
            }
        }

        quanLy.updateAppWidget(widgetId, o)
    }

    // ---- Du lieu ----

    /**
     * Su kien tu BAY GIO tro di, da trai san cac lan lap lai, sap theo gio.
     *
     * Lay ca su kien DANG dien ra (bat dau roi ma chua ket thuc) — dang hop ma
     * widget bao "khong co gi" thi vo nghia.
     */
    private fun docSapToi(context: Context, zone: ZoneId): List<LanHien> {
        val bayGio = System.currentTimeMillis()
        val den = bayGio + SO_NGAY_NHIN * 24 * 3_600_000L

        val kho = Ung.cua(context).kho
        return try {
            kho.trongKhoang(bayGio, den)
                .flatMap { LichLapLai.moRong(it, bayGio, den, zone) }
                .filter { it.ketThuc > bayGio }
                .sortedBy { it.batDau }
                .take(TOI_DA_DONG + 1)
        } catch (_: Exception) {
            emptyList()
        } finally {
        }
    }

    private fun dangDienRa(lan: LanHien): Boolean {
        val bayGio = System.currentTimeMillis()
        return lan.batDau <= bayGio && lan.ketThuc > bayGio
    }

    private fun tenCua(lan: LanHien): String = lan.suKien.tieuDe.ifBlank { "(không tiêu đề)" }

    /** `Hôm nay` · `Ngày mai` · `T4 12/8` — cot trai cua moi dong. */
    private fun nhanNgay(moc: Long, zone: ZoneId): String {
        val ngay = Instant.ofEpochMilli(moc).atZone(zone).toLocalDate()
        return when (java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(zone), ngay)) {
            0L -> "Hôm nay"
            1L -> "Ngày mai"
            else -> "${NgayVN.thuNgan(ngay.dayOfWeek)} ${ngay.dayOfMonth}/${ngay.monthValue}"
        }
    }

    private fun nhanGio(lan: LanHien, zone: ZoneId): String =
        if (lan.suKien.caNgay) "Cả ngày"
        else "${NgayVN.gioPhut(lan.batDau, zone)} – ${NgayVN.gioPhut(lan.ketThuc, zone)}"

    private fun moApp(context: Context): PendingIntent? {
        val mo = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        // Widget Lich trinh thi mo thang tab Lich trinh, khong dung o tab Ghi chu.
        mo.putExtra(ManHinhChinhActivity.KHOA_TAB, 1)
        mo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, mo, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // ---- Tra ma tai nguyen luc chay ----



    companion object {
        /** Them nhieu hon chieu cao widget thi phan duoi bi cat, khong tran. */
        private const val TOI_DA_DONG = 15

        /** Nhin truoc bao nhieu ngay. Xa hon thi khong con la "sap toi" nua. */
        private const val SO_NGAY_NHIN = 60L

        /**
         * Bao widget ve lai ngay.
         *
         * Goi moi khi du lieu su kien doi — dung cho het chu ky 30 phut, khong thi
         * nguoi dung vua them viec ma widget van hien cai cu.
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
            } catch (_: Exception) {
                // Chua dat widget nao thi thoi — khong duoc lam vo cho goi.
            }
        }
    }
}
