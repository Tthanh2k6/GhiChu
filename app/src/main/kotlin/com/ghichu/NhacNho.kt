package com.ghichu

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.ZoneId

/**
 * Mot lan nhac sap toi.
 *
 * [khoa] la khoa on dinh cua thu can nhac (`sk:<id>` hoac `gc:<id>`) — dung lam
 * ma thong bao de nhac lai cung mot su kien khong de ra hai thong bao.
 */
class LanNhac(val luc: Long, val khoa: String, val tieuDe: String, val phu: String)

/**
 * Nhac nho: su kien lich trinh va ghi chu co dat gio.
 *
 * CACH LAM — chi hen **mot** bao thuc cho lan nhac gan nhat, khi no keu thi hien
 * moi thu den han roi hen tiep cho lan sau.
 *
 * Vi sao khong hen san tat ca: bao thuc chinh xac
 * (`setExactAndAllowWhileIdle`) la tai nguyen he thong dem duoc; mot lich lap
 * hang ngay keo dai mot nam la 365 bao thuc cho MOT su kien. Hen mot cai thi du
 * bao nhieu su kien cung chi ton mot.
 *
 * Doi lai: **moi duong ghi du lieu deu phai goi [datLai]** — them/sua/xoa su
 * kien, doi gio nhac cua ghi chu, keo du lieu tu may chu ve. Quen mot cho la
 * nhac nho im lang ma khong bao loi gi.
 */
object NhacNho {

    private const val KENH = "nhac_nho"
    private const val MA_BAO_THUC = 9001

    /**
     * Cua so quanh moc bao thuc coi la "den han".
     *
     * He thong co the goi tre vai giay den vai phut (may dang ngu sau). Khong co
     * cua so nay thi bao thuc keu tre 30 giay la khong tim thay gi de nhac, roi
     * lan nhac do mat luon.
     */
    private const val CUA_SO_MS = 5 * 60 * 1000L

    /** Nhin truoc bao xa khi tim lan nhac ke tiep. */
    private const val NHIN_TRUOC_MS = 400L * 24 * 60 * 60 * 1000

    // ------------------------------------------------------------ phan thuan

    /**
     * Cac lan nhac roi vao khoang `[tu, den)`, da xep theo thoi gian.
     *
     * Thuan Kotlin, khong dung API Android — kiem thu tren JVM duoc.
     */
    fun sapToi(
        suKien: List<SuKien>,
        ghiChu: List<GhiChu>,
        tu: Long,
        den: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<LanNhac> {
        val ra = ArrayList<LanNhac>()

        for (sk in suKien) {
            if (sk.nhacTruoc < 0) continue
            val truoc = sk.nhacTruoc * 60_000L
            // Nhac truoc N phut nghia la su kien BAT DAU sau khoang do — nen phai
            // tim su kien trong khoang da doi ve phia sau, khong phai khoang goc.
            for (lan in LichLapLai.moRong(sk, tu + truoc, den + truoc, zone)) {
                val luc = lan.batDau - truoc
                if (luc < tu || luc >= den) continue
                ra += LanNhac(
                    luc = luc,
                    khoa = "sk:" + sk.id,
                    tieuDe = sk.tieuDe.ifBlank { "Sự kiện" },
                    phu = nhanTruoc(sk.nhacTruoc) + NgayVN.gioPhut(lan.batDau, zone),
                )
            }
        }

        for (g in ghiChu) {
            val luc = g.nhacLuc ?: continue
            if (g.daXoa || luc < tu || luc >= den) continue
            ra += LanNhac(
                luc = luc,
                khoa = "gc:" + g.id,
                tieuDe = g.tieuDeHien().ifBlank { "Ghi chú" },
                phu = g.xemTruoc(1).ifBlank { "Ghi chú của bạn" },
            )
        }

        return ra.sortedBy { it.luc }
    }

    private fun nhanTruoc(phut: Int): String = when {
        phut <= 0 -> "Bắt đầu lúc "
        phut < 60 -> "Còn $phut phút — "
        phut % 60 == 0 -> "Còn ${phut / 60} giờ — "
        else -> "Còn ${phut / 60} giờ ${phut % 60} phút — "
    }

    // ------------------------------------------------------------ phan Android

    /**
     * Tinh lai va hen bao thuc cho lan nhac gan nhat.
     *
     * Goi sau MOI thay doi du lieu. Khong co lan nhac nao thi huy bao thuc dang
     * treo — khong huy thi bao thuc cu van keu cho mot su kien da xoa.
     */
    fun datLai(context: Context) {
        val ung = context.applicationContext
        val bao = ung.getSystemService(AlarmManager::class.java) ?: return
        val y = y(ung)

        val gio = System.currentTimeMillis()
        val ke = ke(ung, gio) ?: run {
            bao.cancel(y)
            return
        }

        try {
            if (bao.canScheduleExactAlarms()) {
                bao.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ke, y)
            } else {
                // Nguoi dung chua cho hen gio chinh xac — van nhac, chi la co the
                // tre vai phut. Im lang hoan toan con te hon nhac tre.
                bao.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ke, y)
            }
        } catch (_: SecurityException) {
            bao.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ke, y)
        }
    }

    /** Bao thuc vua keu: hien moi thu den han, roi hen cho lan sau. */
    fun reo(context: Context) {
        val ung = context.applicationContext
        val gio = System.currentTimeMillis()
        for (l in trongKhoang(ung, gio - CUA_SO_MS, gio + CUA_SO_MS)) hien(ung, l)
        datLai(ung)
    }

    private fun ke(context: Context, tu: Long): Long? =
        trongKhoang(context, tu, tu + NHIN_TRUOC_MS).firstOrNull()?.luc

    private fun trongKhoang(context: Context, tu: Long, den: Long): List<LanNhac> {
        val ung = Ung.cua(context)
        return sapToi(
            suKien = ung.kho.trongKhoang(tu, den),
            ghiChu = ung.ghiChu.danhSach(),
            tu = tu,
            den = den,
        )
    }

    private fun hien(context: Context, l: LanNhac) {
        val ql = context.getSystemService(NotificationManager::class.java) ?: return
        ql.createNotificationChannel(
            NotificationChannel(KENH, "Nhắc nhở", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Nhắc sự kiện lịch trình và ghi chú đã đặt giờ"
            },
        )

        val mo = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val cham = mo?.let {
            PendingIntent.getActivity(
                context, l.khoa.hashCode(), it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val tb = Notification.Builder(context, KENH)
            .setSmallIcon(R.drawable.ic_ghi_chu)
            .setContentTitle(l.tieuDe)
            .setContentText(l.phu)
            .setAutoCancel(true)
            .also { if (cham != null) it.setContentIntent(cham) }
            .build()

        // Ma thong bao lay tu khoa on dinh — nhac lai cung mot su kien thi de len
        // thong bao cu chu khong sinh ra hang chuc thong bao giong het nhau.
        ql.notify(l.khoa.hashCode(), tb)
    }

    private fun y(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, MA_BAO_THUC,
        Intent(context, BaoThuc::class.java).setAction(HANH_DONG),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    const val HANH_DONG = "com.ghichu.NHAC"
}

/**
 * Nhan bao thuc, va nhan tin khoi dong xong de hen lai.
 *
 * Bao thuc KHONG song qua lan khoi dong may — khong hen lai o
 * `BOOT_COMPLETED` thi tat may mot dem la sang hom sau khong con nhac nho nao.
 */
class BaoThuc : BroadcastReceiver() {

    override fun onReceive(context: Context, y: Intent) {
        when (y.action) {
            NhacNho.HANH_DONG -> NhacNho.reo(context)
            else -> NhacNho.datLai(context)
        }
    }
}
