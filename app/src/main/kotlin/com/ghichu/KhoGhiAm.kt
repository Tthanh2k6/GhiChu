package com.ghichu

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import java.io.File

/**
 * Ghi am gan trong ghi chu.
 *
 * Tep nam trong `files/ghiam/`, ghi chu chi giu TEN TEP va do dai:
 * `<audio src="ga_xxx.m4a" giay="23" />`.
 *
 * Dinh dang **AAC trong hop m4a**: bo ma hoa co san trong Android, khong phai
 * mang thu vien native nao theo (app Xiaomi mang `libmp3lame.so` chi de ra MP3 —
 * ta khong can, m4a moi may deu mo duoc).
 *
 * **Ghi am KHONG di theo duong dong bo**, giong anh — goi gui len may chu chi co
 * phan chu. May khac keo ve se thay dong ghi am nhung bam khong ra tieng.
 */
object KhoGhiAm {

    /**
     * Chan tren mot ban ghi.
     *
     * Khong co chan thi mot lan quen bam dung la mot tep vai tram MB. 30 phut du
     * cho moi thu goi la "ghi chu bang giong noi".
     */
    const val TOI_DA_GIAY = 30 * 60

    fun thuMuc(context: Context): File =
        File(context.filesDir, "ghiam").apply { if (!exists()) mkdirs() }

    fun tep(context: Context, ten: String): File = File(thuMuc(context), ten)

    fun tenMoi(): String = "ga_" + Uid.moi() + ".m4a"

    /** `0:23`, hoac `1:05:09` khi dai qua mot gio. */
    fun doDai(giay: Int): String {
        val g = giay.coerceAtLeast(0)
        return if (g >= 3600) "%d:%02d:%02d".format(g / 3600, g % 3600 / 60, g % 60)
        else "%d:%02d".format(g / 60, g % 60)
    }

    /**
     * Mot phien ghi am dang chay.
     *
     * Tach thanh lop rieng chu khong de [KhoGhiAm] giu trang thai tinh: hai man
     * hinh cung ghi am mot luc la chuyen khong nen xay ra, nhung neu xay ra thi
     * trang thai tinh khien ban ghi nay dam ban ghi kia ma khong bao gi.
     */
    class Phien(private val context: Context, val ten: String) {

        private var may: MediaRecorder? = null
        private var batDau = 0L

        val tep: File get() = tep(context, ten)

        /** Tra ve false neu may khong cho ghi (mic dang ban, thieu quyen…). */
        fun bat(): Boolean = try {
            val m = MediaRecorder(context).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                // 64 kbps mono: du ro cho giong noi, va mot gio ghi chi ~28 MB.
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(44_100)
                setAudioChannels(1)
                setMaxDuration(TOI_DA_GIAY * 1000)
                setOutputFile(tep.absolutePath)
                prepare()
                start()
            }
            may = m
            batDau = System.currentTimeMillis()
            true
        } catch (_: Exception) {
            huyMay()
            false
        }

        /** Giay da ghi, de hien dong ho dem. */
        fun daGhi(): Int =
            if (batDau == 0L) 0 else ((System.currentTimeMillis() - batDau) / 1000).toInt()

        /**
         * Dung ghi. Tra ve so giay, hoac 0 neu ban ghi hong.
         *
         * `stop()` NEM khi ban ghi qua ngan (chua kip ghi duoc khung nao) — bat
         * lay va xoa tep, khong thi ghi chu co mot dong ghi am bam khong ra gi.
         */
        fun dung(): Int {
            val giay = daGhi()
            val duoc = try {
                may?.stop()
                true
            } catch (_: Exception) {
                false
            }
            huyMay()
            if (!duoc || giay < 1) {
                tep.delete()
                return 0
            }
            return giay
        }

        /** Bo ban ghi dang do va xoa tep. */
        fun bo() {
            try {
                may?.stop()
            } catch (_: Exception) {
                // Ban ghi qua ngan thi stop nem — khong sao, dang xoa den noi.
            }
            huyMay()
            tep.delete()
        }

        private fun huyMay() {
            try {
                may?.release()
            } catch (_: Exception) {
                // Da giai phong roi thi thoi.
            }
            may = null
        }
    }

    /**
     * Phat mot ban ghi. Tra ve `MediaPlayer` de ben goi con dung/giai phong duoc,
     * hoac null neu tep khong con.
     */
    fun phat(context: Context, ten: String, khiXong: () -> Unit): MediaPlayer? {
        val f = tep(context, ten)
        if (!f.exists()) return null
        return try {
            MediaPlayer().apply {
                setDataSource(f.absolutePath)
                setOnCompletionListener { khiXong() }
                prepare()
                start()
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Xoa nhung ban ghi khong con ghi chu nao nhac toi.
     *
     * Cung ly do va cung cach chan nhu [KhoAnh.donTepThua] — ghi chu rieng tu
     * dang khoa thi bo ca luot don, khong doan mo.
     */
    fun donTepThua(context: Context, kho: KhoDuLieu) {
        val dung = HashSet<String>()
        val theGhiAm = Regex("<audio[^>]*src=\"([^\"]+)\"")

        kho.readableDatabase.query(
            KhoDuLieu.BANG_GHI_CHU, arrayOf("noi_dung"), null, null, null, null, null,
        ).use { c ->
            while (c.moveToNext()) {
                val noiDung = c.getString(0).orEmpty()
                if (MaHoa.daMaHoa(noiDung)) return
                for (m in theGhiAm.findAll(noiDung)) dung += m.groupValues[1]
            }
        }
        thuMuc(context).listFiles()?.forEach { if (it.name !in dung) it.delete() }
    }
}
