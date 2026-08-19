package com.ghichu

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan

/**
 * Cau noi giua [Khoi] (thuan Kotlin) va chu co span de hien trong `EditText`.
 *
 * Ca trinh soan thao la **MOT `EditText` duy nhat**, khong phai moi dong mot o.
 * Do la lua chon co y:
 *   * bam Enter, xoa lui, boi den nhieu dong, keo tha, hoan tac — deu la hanh vi
 *     san co cua `EditText`, khong phai viet lai tung cai;
 *   * o tick va anh la KY TU THAT trong chuoi ([DinhDang.THAY_ANH], `☐`, `☑`)
 *     nen con tro di qua duoc, xoa lui mot cai la mat nhu moi ky tu khac —
 *     khong con trang thai an nao ket lai.
 *
 * Doi lai: kieu cua chu song bang span, ma span **khong tu noi dai khi go them
 * chu o cuoi**. Vi vay khi ap dinh dang phai dung co [Spanned.SPAN_EXCLUSIVE_INCLUSIVE]
 * cho doan dang go — xem [ap].
 */
object SoanThao {

    /** Ky tu o tick trong o soan thao. Nguoi dung nhin thay va xoa duoc thang. */
    const val O_TRONG = "☐ "
    const val O_TICK = "☑ "

    /** Mot dong ke ngang, ve bang ky tu — `EditText` khong ve duoc hinh giua chu. */
    const val DUONG_KE = "───────────"

    /** Nhan cua mot dong ghi am trong o soan thao. */
    fun nhanGhiAm(giay: Int): String = "🎙 Ghi âm · " + KhoGhiAm.doDai(giay)

    private const val TI_LE_TO = 1.45f
    private const val TI_LE_VUA = 1.2f

    // ------------------------------------------------------------ markup -> chu

    fun doc(markup: String): Spannable {
        val ra = SpannableStringBuilder()
        val khoi = KhoiNoiDung.doc(markup)

        for ((i, k) in khoi.withIndex()) {
            if (i > 0) ra.append('\n')
            when (k.loai) {
                Khoi.O_CHUA_TICK -> ra.append(O_TRONG)
                Khoi.O_DA_TICK -> ra.append(O_TICK)
                Khoi.DUONG_KE -> {
                    ra.append(DUONG_KE)
                    continue
                }
                Khoi.GHI_AM -> {
                    val dau = ra.length
                    ra.append(nhanGhiAm(k.giay))
                    // Ten tep va do dai song trong SPAN chu khong trong chu hien
                    // ra: nguoi dung khong phai nhin thay `ga_m3k1z9q4.m4a`, ma
                    // ho co go de len nhan thi ban ghi van con nguyen.
                    ra.setSpan(
                        TheGhiAm(k.tep, k.giay), dau, ra.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    continue
                }
            }
            for (d in k.doan) {
                val dau = ra.length
                ra.append(d.chu)
                if (d.anh != null) {
                    ra.setSpan(TheAnh(d.anh), dau, ra.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    continue
                }
                datSpan(ra, dau, ra.length, d)
            }
        }
        return ra
    }

    private fun datSpan(ra: Spannable, dau: Int, cuoi: Int, d: Doan) {
        if (dau >= cuoi) return
        fun dat(s: Any) = ra.setSpan(s, dau, cuoi, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        if (d.co(DinhDang.DAM) && d.co(DinhDang.NGHIENG)) dat(StyleSpan(Typeface.BOLD_ITALIC))
        else if (d.co(DinhDang.DAM)) dat(StyleSpan(Typeface.BOLD))
        else if (d.co(DinhDang.NGHIENG)) dat(StyleSpan(Typeface.ITALIC))

        if (d.co(DinhDang.GACH_CHAN)) dat(UnderlineSpan())
        if (d.co(DinhDang.GACH_NGANG)) dat(StrikethroughSpan())
        if (d.co(DinhDang.CHU_TO)) dat(RelativeSizeSpan(TI_LE_TO))
        if (d.co(DinhDang.CHU_VUA)) dat(RelativeSizeSpan(TI_LE_VUA))
        d.mauChu?.let { dat(ForegroundColorSpan(it)) }
        d.mauNen?.let { dat(BackgroundColorSpan(it)) }
    }

    /**
     * Danh dau cho biet doan nay von la mot buc anh.
     *
     * Chua ve anh o chang nay — moi giu duong dan de khong lam mat khi luu. Ve
     * that se la mot `ImageSpan` dat chong len chinh doan nay.
     */
    class TheAnh(val ten: String)

    /** Danh dau mot dong la ban ghi am; giu ten tep va do dai. */
    class TheGhiAm(val ten: String, val giay: Int)

    // ------------------------------------------------------------ chu -> markup

    fun ghi(chu: CharSequence): String {
        val sp = chu as? Spanned
        val khoi = ArrayList<Khoi>()
        var dong = 0

        while (dong <= chu.length) {
            val cuoi = chu.indexOf('\n', dong).let { if (it < 0) chu.length else it }
            khoi += mot(chu, sp, dong, cuoi)
            if (cuoi >= chu.length) break
            dong = cuoi + 1
        }
        return KhoiNoiDung.ghi(khoi)
    }

    private fun mot(chu: CharSequence, sp: Spanned?, dauDong: Int, cuoiDong: Int): Khoi {
        val nguyen = chu.subSequence(dauDong, cuoiDong).toString()
        if (nguyen == DUONG_KE) return Khoi(Khoi.DUONG_KE)

        // Ghi am: nhan dien bang SPAN chu khong bang chu tren man hinh — nguoi
        // dung sua nhan thi dong do van la ban ghi, con go tay dung chu "🎙 Ghi
        // âm · 0:23" thi khong bong dung thanh ban ghi.
        sp?.getSpans(dauDong, cuoiDong, TheGhiAm::class.java)?.firstOrNull()?.let {
            return Khoi(Khoi.GHI_AM, tep = it.ten, giay = it.giay)
        }

        val loai = when {
            nguyen.startsWith(O_TICK) -> Khoi.O_DA_TICK
            nguyen.startsWith(O_TRONG) -> Khoi.O_CHUA_TICK
            else -> Khoi.CHU
        }
        val dau = dauDong + when (loai) {
            Khoi.CHU -> 0
            else -> O_TRONG.length
        }
        return Khoi(loai, if (sp == null) chuTron(chu, dau, cuoiDong) else doan(sp, dau, cuoiDong))
    }

    private fun chuTron(chu: CharSequence, dau: Int, cuoi: Int): List<Doan> =
        if (dau >= cuoi) emptyList() else listOf(Doan(chu.subSequence(dau, cuoi).toString()))

    /**
     * Cat doan `[dau, cuoi)` thanh cac doan cung kieu.
     *
     * Dung `nextSpanTransition` chu khong duyet tung ky tu: no nhay thang toi vi
     * tri span tiep theo doi, nen mot bai dai khong dinh dang chi mot buoc la
     * xong.
     */
    private fun doan(sp: Spanned, dau: Int, cuoi: Int): List<Doan> {
        val ra = ArrayList<Doan>()
        var i = dau
        while (i < cuoi) {
            val j = sp.nextSpanTransition(i, cuoi, Any::class.java)
            val anh = sp.getSpans(i, j, TheAnh::class.java).firstOrNull()
            if (anh != null) {
                ra += Doan(chu = DinhDang.THAY_ANH, anh = anh.ten)
                i = j
                continue
            }

            var kieu = 0
            for (s in sp.getSpans(i, j, StyleSpan::class.java)) {
                if (s.style and Typeface.BOLD != 0) kieu = kieu or DinhDang.DAM
                if (s.style and Typeface.ITALIC != 0) kieu = kieu or DinhDang.NGHIENG
            }
            if (sp.getSpans(i, j, UnderlineSpan::class.java).isNotEmpty()) kieu = kieu or DinhDang.GACH_CHAN
            if (sp.getSpans(i, j, StrikethroughSpan::class.java).isNotEmpty()) kieu = kieu or DinhDang.GACH_NGANG
            for (s in sp.getSpans(i, j, RelativeSizeSpan::class.java)) {
                kieu = kieu or if (s.sizeChange >= TI_LE_TO) DinhDang.CHU_TO else DinhDang.CHU_VUA
            }

            ra += Doan(
                chu = sp.subSequence(i, j).toString(),
                kieu = kieu,
                mauChu = sp.getSpans(i, j, ForegroundColorSpan::class.java).firstOrNull()?.foregroundColor,
                mauNen = sp.getSpans(i, j, BackgroundColorSpan::class.java).firstOrNull()?.backgroundColor,
            )
            i = j
        }
        return ra
    }

    // ------------------------------------------------------------ ap dinh dang

    /**
     * Bat/tat mot kieu tren doan dang chon.
     *
     * Dang co san tren TOAN BO doan chon thi bam la TAT; con lai thi bam la BAT
     * ca doan. Dung nhu Word: boi mot cau nua dam nua thuong roi bam "B" thi ca
     * cau thanh dam, bam lan nua moi het.
     */
    fun doiKieu(chu: Spannable, dau: Int, cuoi: Int, co: Int) {
        if (dau >= cuoi) return
        val dangCo = phuKin(chu, dau, cuoi, co)
        xoaKieu(chu, dau, cuoi, co)
        if (!dangCo) ap(chu, dau, cuoi, co)
    }

    /** Kieu [co] co phu KIN doan `[dau, cuoi)` khong. */
    fun phuKin(chu: Spanned, dau: Int, cuoi: Int, co: Int): Boolean {
        var i = dau
        while (i < cuoi) {
            val j = chu.nextSpanTransition(i, cuoi, Any::class.java)
            if (!coTai(chu, i, j, co)) return false
            i = j
        }
        return true
    }

    private fun coTai(chu: Spanned, dau: Int, cuoi: Int, co: Int): Boolean = when (co) {
        DinhDang.DAM -> chu.getSpans(dau, cuoi, StyleSpan::class.java)
            .any { it.style and Typeface.BOLD != 0 }
        DinhDang.NGHIENG -> chu.getSpans(dau, cuoi, StyleSpan::class.java)
            .any { it.style and Typeface.ITALIC != 0 }
        DinhDang.GACH_CHAN -> chu.getSpans(dau, cuoi, UnderlineSpan::class.java).isNotEmpty()
        DinhDang.GACH_NGANG -> chu.getSpans(dau, cuoi, StrikethroughSpan::class.java).isNotEmpty()
        DinhDang.CHU_TO -> chu.getSpans(dau, cuoi, RelativeSizeSpan::class.java)
            .any { it.sizeChange >= TI_LE_TO }
        DinhDang.CHU_VUA -> chu.getSpans(dau, cuoi, RelativeSizeSpan::class.java)
            .any { it.sizeChange < TI_LE_TO }
        else -> false
    }

    private fun ap(chu: Spannable, dau: Int, cuoi: Int, co: Int) {
        val s: Any = when (co) {
            DinhDang.DAM -> StyleSpan(Typeface.BOLD)
            DinhDang.NGHIENG -> StyleSpan(Typeface.ITALIC)
            DinhDang.GACH_CHAN -> UnderlineSpan()
            DinhDang.GACH_NGANG -> StrikethroughSpan()
            DinhDang.CHU_TO -> RelativeSizeSpan(TI_LE_TO)
            DinhDang.CHU_VUA -> RelativeSizeSpan(TI_LE_VUA)
            else -> return
        }
        chu.setSpan(s, dau, cuoi, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    /**
     * Go kieu khoi doan `[dau, cuoi)`.
     *
     * Span nam VAT ra ngoai doan chon thi khong xoa han ma **cat lai** phan con
     * ngoai: boi giua mot cau dam roi bam bo dam ma xoa ca span la hai dau cau
     * cung het dam theo.
     */
    private fun xoaKieu(chu: Spannable, dau: Int, cuoi: Int, co: Int) {
        for (s in chu.getSpans(dau, cuoi, Any::class.java)) {
            if (!hopVoi(s, co)) continue
            val sDau = chu.getSpanStart(s)
            val sCuoi = chu.getSpanEnd(s)
            chu.removeSpan(s)
            if (sDau < dau) chu.setSpan(nhanBan(s), sDau, dau, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (sCuoi > cuoi) chu.setSpan(nhanBan(s), cuoi, sCuoi, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun hopVoi(s: Any, co: Int): Boolean = when (co) {
        DinhDang.DAM -> s is StyleSpan && s.style and Typeface.BOLD != 0
        DinhDang.NGHIENG -> s is StyleSpan && s.style and Typeface.ITALIC != 0
        DinhDang.GACH_CHAN -> s is UnderlineSpan
        DinhDang.GACH_NGANG -> s is StrikethroughSpan
        DinhDang.CHU_TO -> s is RelativeSizeSpan && s.sizeChange >= TI_LE_TO
        DinhDang.CHU_VUA -> s is RelativeSizeSpan && s.sizeChange < TI_LE_TO
        else -> false
    }

    /**
     * Ban sao cua mot span.
     *
     * Phai nhan ban chu khong dung lai chinh no: mot doi tuong span chi dat duoc
     * o MOT khoang trong cung mot chuoi — dat lai o khoang thu hai la khoang thu
     * nhat bi doi cho chu khong phai co them.
     */
    private fun nhanBan(s: Any): Any = when (s) {
        is StyleSpan -> StyleSpan(s.style)
        is UnderlineSpan -> UnderlineSpan()
        is StrikethroughSpan -> StrikethroughSpan()
        is RelativeSizeSpan -> RelativeSizeSpan(s.sizeChange)
        is ForegroundColorSpan -> ForegroundColorSpan(s.foregroundColor)
        is BackgroundColorSpan -> BackgroundColorSpan(s.backgroundColor)
        else -> s
    }

    /** Dat mau chu cho doan chon; [mau] null = tra ve mau mac dinh. */
    fun datMauChu(chu: Spannable, dau: Int, cuoi: Int, mau: Int?) {
        if (dau >= cuoi) return
        for (s in chu.getSpans(dau, cuoi, ForegroundColorSpan::class.java)) {
            val sDau = chu.getSpanStart(s)
            val sCuoi = chu.getSpanEnd(s)
            chu.removeSpan(s)
            if (sDau < dau) chu.setSpan(nhanBan(s), sDau, dau, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (sCuoi > cuoi) chu.setSpan(nhanBan(s), cuoi, sCuoi, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (mau != null) {
            chu.setSpan(ForegroundColorSpan(mau), dau, cuoi, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
