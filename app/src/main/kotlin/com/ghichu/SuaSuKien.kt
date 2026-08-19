package com.ghichu

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Hop thoai them / sua mot su kien.
 *
 * Dung ngay `android.app.AlertDialog` chu khong phai lop cua miuix: hop thoai
 * miuix can tai nguyen style rieng, ma code tiem thi khong tra duoc R.style cua
 * app dich luc bien dich. Doi lai giao dien khong giong het Xiaomi 100%.
 */
class SuaSuKien(
    private val context: Context,
    private val db: KhoDuLieu,
    private val goc: SuKien?,
    mocGoiY: LocalDateTime?,
    private val khiXong: () -> Unit,
) {

    private val zone: ZoneId = ZoneId.systemDefault()

    private var batDau: LocalDateTime
    private var ketThuc: LocalDateTime
    private var caNgay: Boolean
    private var mau: Int
    private var lapLai: Int
    private var nhac: Int

    /**
     * Nguoi dung co TU cham vao bang mau khong.
     *
     * Tu chon thi hang cho khong nhuc nhich — do la lua chon co y cua ho, dung
     * no de xoay hang la lam hang lech theo thoi quen chon tay.
     */
    private var tuChonMau = false

    init {
        if (goc != null) {
            batDau = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(goc.batDau), zone)
            ketThuc = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(goc.ketThuc), zone)
            caNgay = goc.caNgay
            mau = goc.mau
            lapLai = goc.lapLai
            nhac = goc.nhacTruoc
        } else {
            val moc = mocGoiY ?: LocalDateTime.now(zone).withMinute(0).withSecond(0).withNano(0).plusHours(1)
            batDau = moc
            ketThuc = moc.plusHours(1)
            caNgay = false
            // Lay o dau hang cho, CHUA xoay hang — chi xoay khi that su luu va
            // nguoi dung khong tu chon mau khac.
            mau = HangMau.dinh(context)
            lapLai = SuKien.LAP_KHONG
            nhac = -1
        }
    }

    /** Bang mau sang/toi. Ten khac han [mau] o tren — do la CHI SO mau su kien. */
    private val bangMau = BangMau.cua(context)
    private val md = context.resources.displayMetrics
    private fun dp(v: Int) = (v * md.density).toInt()

    private lateinit var oTieuDe: EditText
    private lateinit var oGhiChu: EditText
    private lateinit var oDiaDiem: EditText
    private lateinit var nutBatDau: Button
    private lateinit var nutKetThuc: Button
    private lateinit var nutLap: Button
    private lateinit var nutNhac: Button
    private val oMau = ArrayList<View>()

    fun hien() {
        val than = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }

        oTieuDe = oNhap("Tiêu đề").apply { setText(goc?.tieuDe ?: "") }
        than.themDoc(oTieuDe)

        val congTacCaNgay = Switch(context).apply {
            text = "Cả ngày"
            isChecked = caNgay
            setTextColor(bangMau.chuChinh)
            setOnCheckedChangeListener { _, bat ->
                caNgay = bat
                capNhatNutGio()
            }
        }
        than.themDoc(congTacCaNgay, dp(12))

        nutBatDau = nutPhang { chonMoc(dungBatDau = true) }
        nutKetThuc = nutPhang { chonMoc(dungBatDau = false) }
        than.themDoc(hang("Bắt đầu", nutBatDau))
        than.themDoc(hang("Kết thúc", nutKetThuc))
        capNhatNutGio()

        than.themDoc(nhan("Màu"), dp(12))
        than.themDoc(bangMau(), dp(6))

        nutLap = nutPhang { chonTuDanhSach("Lặp lại", SuKien.TEN_LAP, lapLai) { lapLai = it; capNhatNutChon() } }
        nutNhac = nutPhang {
            val dangChon = SuKien.NHAC_PHUT.indexOf(nhac).coerceAtLeast(0)
            chonTuDanhSach("Nhắc trước", SuKien.TEN_NHAC, dangChon) { nhac = SuKien.NHAC_PHUT[it]; capNhatNutChon() }
        }
        than.themDoc(hang("Lặp lại", nutLap))
        than.themDoc(hang("Nhắc", nutNhac))
        capNhatNutChon()

        oDiaDiem = oNhap("Địa điểm").apply { setText(goc?.diaDiem ?: "") }
        than.themDoc(oDiaDiem, dp(4))

        oGhiChu = oNhap("Ghi chú").apply {
            setText(goc?.ghiChu ?: "")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
        }
        than.themDoc(oGhiChu)

        val cuon = ScrollView(context).apply { addView(than) }

        val hopThoai = AlertDialog.Builder(context, bangMau.themeHopThoai)
            .setTitle(if (goc == null) "Sự kiện mới" else "Sửa sự kiện")
            .setView(cuon)
            .setPositiveButton("Lưu", null)
            .setNegativeButton("Huỷ", null)
            .apply { if (goc != null) setNeutralButton("Xoá", null) }
            .create()

        hopThoai.show()

        // Gan sau khi show() de nut "Luu" khong tu dong dong khi du lieu chua hop le.
        hopThoai.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (luu()) hopThoai.dismiss()
        }
        if (goc != null) {
            hopThoai.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                AlertDialog.Builder(context, bangMau.themeHopThoai)
                    .setMessage("Xoá sự kiện này?")
                    .setPositiveButton("Xoá") { _, _ ->
                        db.xoa(goc.id)
                        hopThoai.dismiss()
                        khiXong()
                    }
                    .setNegativeButton("Huỷ", null)
                    .show()
            }
        }
    }

    private fun luu(): Boolean {
        val tieuDe = oTieuDe.text.toString().trim()
        if (tieuDe.isEmpty()) {
            oTieuDe.error = "Chưa có tiêu đề"
            return false
        }
        if (!ketThuc.isAfter(batDau)) {
            ketThuc = batDau.plusHours(1)
        }

        val (tu, den) = if (caNgay) {
            batDau.toLocalDate().atStartOfDay() to ketThuc.toLocalDate().plusDays(1).atStartOfDay()
        } else {
            batDau to ketThuc
        }

        db.luu(
            SuKien(
                id = goc?.id ?: 0L,
                tieuDe = tieuDe,
                ghiChu = oGhiChu.text.toString(),
                batDau = tu.atZone(zone).toInstant().toEpochMilli(),
                ketThuc = den.atZone(zone).toInstant().toEpochMilli(),
                caNgay = caNgay,
                mau = mau,
                lapLai = lapLai,
                nhacTruoc = nhac,
                diaDiem = oDiaDiem.text.toString().trim(),
            ),
        )

        // Chi xoay hang cho khi su kien MOI va mau do do may tu gan.
        if (goc == null && !tuChonMau) HangMau.daDung(context, mau)

        khiXong()
        return true
    }

    // ---- Manh giao dien ----

    private fun oNhap(goiY: String) = EditText(context).apply {
        hint = goiY
        setTextColor(bangMau.chuChinh)
        setHintTextColor(bangMau.chuMo)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
    }

    private fun nhan(chu: String) = TextView(context).apply {
        text = chu
        setTextColor(bangMau.chuMo)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
    }

    private fun nutPhang(khiBam: () -> Unit) = Button(context).apply {
        setTextColor(bangMau.chuChinh)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setBackgroundColor(Color.TRANSPARENT)
        gravity = Gravity.END or Gravity.CENTER_VERTICAL
        setPadding(0, 0, 0, 0)
        setOnClickListener { khiBam() }
    }

    private fun hang(ten: String, phai: View): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(
            nhan(ten),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        addView(phai, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)))
    }

    private fun bangMau(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        for (i in MauSuKien.BANG.indices) {
            val cham = View(context)
            val lp = LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginEnd = dp(8) }
            cham.setOnClickListener {
                mau = i
                tuChonMau = true
                capNhatChonMau()
            }
            addView(cham, lp)
            oMau.add(cham)
        }
        capNhatChonMau()
    }

    private fun capNhatChonMau() {
        for (i in oMau.indices) {
            oMau[i].background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(MauSuKien.cua(i))
                if (i == mau) setStroke(dp(2), Color.WHITE)
            }
        }
    }

    private fun capNhatNutGio() {
        nutBatDau.text = nhanMoc(batDau)
        nutKetThuc.text = nhanMoc(ketThuc)
    }

    private fun nhanMoc(t: LocalDateTime): String =
        if (caNgay) NgayVN.ngayDayDu(t.toLocalDate())
        else "${NgayVN.ngayDayDu(t.toLocalDate())}  %02d:%02d".format(t.hour, t.minute)

    private fun capNhatNutChon() {
        nutLap.text = SuKien.TEN_LAP[lapLai.coerceIn(0, SuKien.TEN_LAP.size - 1)]
        val i = SuKien.NHAC_PHUT.indexOf(nhac).coerceAtLeast(0)
        nutNhac.text = SuKien.TEN_NHAC[i]
    }

    private fun chonMoc(dungBatDau: Boolean) {
        val t = if (dungBatDau) batDau else ketThuc
        DatePickerDialog(
            context, bangMau.themeHopThoai,
            { _, nam, thangTu0, ngay ->
                val d = LocalDate.of(nam, thangTu0 + 1, ngay)
                if (caNgay) {
                    ganMoc(dungBatDau, d.atTime(t.toLocalTime()))
                } else {
                    TimePickerDialog(
                        context, bangMau.themeHopThoai,
                        { _, gio, phut -> ganMoc(dungBatDau, d.atTime(LocalTime.of(gio, phut))) },
                        t.hour, t.minute, true,
                    ).show()
                }
            },
            t.year, t.monthValue - 1, t.dayOfMonth,
        ).show()
    }

    private fun ganMoc(dungBatDau: Boolean, moi: LocalDateTime) {
        if (dungBatDau) {
            // Keo gio ket thuc theo, giu nguyen do dai su kien.
            val dai = java.time.Duration.between(batDau, ketThuc)
            batDau = moi
            ketThuc = moi.plus(dai)
        } else {
            ketThuc = moi
        }
        capNhatNutGio()
    }

    private fun chonTuDanhSach(tieuDe: String, muc: Array<String>, dangChon: Int, khiChon: (Int) -> Unit) {
        AlertDialog.Builder(context, bangMau.themeHopThoai)
            .setTitle(tieuDe)
            .setSingleChoiceItems(muc, dangChon) { d, i ->
                khiChon(i)
                d.dismiss()
            }
            .show()
    }

    /**
     * Them mot hang, rong het be ngang, co le tren.
     *
     * KHONG dat ten la `addView`: `ViewGroup` da co san `addView(View, int)` voi
     * `int` la VI TRI chen, ma ham thanh vien luon thang ham mo rong — goi nham
     * la truyen so pixel vao cho vi tri.
     */
    private fun LinearLayout.themDoc(v: View, leTren: Int = 0) {
        addView(
            v,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = leTren },
        )
    }

}
