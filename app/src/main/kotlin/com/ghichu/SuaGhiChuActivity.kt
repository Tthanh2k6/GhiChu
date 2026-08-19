package com.ghichu

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Soan mot ghi chu.
 *
 * Chang nay soan **chu tron co o tick**; chu dam/nghieng, anh va duong ke se
 * them o chang sau. Nhung o tick thi phai co NGAY tu bay gio: ghi chu keo tu may
 * chu ve co the dang la mot danh sach viec, mo ra sua ma o tick bi nuot thanh
 * dong chu thuong la mat du lieu that. Vi vay o soan thao doc/ghi qua
 * [NoiDungGhiChu.deSua] / [NoiDungGhiChu.tuSua] chu KHONG qua
 * [NoiDungGhiChu.chuTron].
 *
 * **Tu luu, khong co nut Luu.** Luu o `onPause` — nguoi dung go xong roi bam
 * back hoac chuyen app, khong ai di tim nut luu. Ghi chu rong thi khong tao hang
 * (xem [luu]).
 */
class SuaGhiChuActivity : Activity() {

    private val md by lazy { resources.displayMetrics }
    private fun dp(v: Int) = (v * md.density).toInt()

    private lateinit var mau: BangMau
    private val kho: KhoGhiChu get() = Ung.cua(this).ghiChu

    private var id = 0L
    private var goc: GhiChu? = null
    private var thuMuc = ""
    private var mauNen = 0
    private var ghim = false
    private var nhacLuc: Long? = null

    private lateinit var oTieuDe: EditText
    private lateinit var oThan: EditText
    private lateinit var nen: LinearLayout
    private lateinit var oChan: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mau = BangMau.cua(this)
        setTheme(mau.themeManHinh)

        id = intent.getLongExtra(KHOA_ID, 0L)
        thuMuc = intent.getStringExtra(KHOA_THU_MUC).orEmpty()

        val g = if (id != 0L) kho.theoId(id) else null
        goc = g
        if (g != null) {
            thuMuc = g.thuMuc
            mauNen = g.mau
            ghim = g.ghim
            nhacLuc = g.nhacLuc
        }

        nen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        nen.addView(dungThanhTren())
        nen.addView(dungThan(g), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
        ))
        nen.addView(dungThanhDuoi())

        nen.setOnApplyWindowInsetsListener { view, insets ->
            val thanh = insets.getInsets(WindowInsets.Type.systemBars())
            val banPhim = insets.getInsets(WindowInsets.Type.ime())
            // Lay cai LON hon giua ban phim va thanh dieu huong, khong cong don:
            // ban phim bat len la no da phu len thanh dieu huong roi, cong ca hai
            // thi thua ra mot khoang trong bang chieu cao thanh.
            view.setPadding(0, thanh.top, 0, maxOf(thanh.bottom, banPhim.bottom))
            insets
        }
        setContentView(nen)
        toMau()
    }

    override fun onPause() {
        luu()
        super.onPause()
    }

    // ------------------------------------------------------------ dung giao dien

    private fun dungThanhTren(): View {
        val thanh = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(2))
        }
        thanh.addView(nutAnh(R.drawable.ic_quay_lai, "Quay lại") {
            // Luu o onPause; day chi dong man hinh.
            finish()
        })
        thanh.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        thanh.addView(nutAnh(R.drawable.ic_ghim, "Ghim") {
            ghim = !ghim
            toMau()
            Toast.makeText(this, if (ghim) "Đã ghim" else "Bỏ ghim", Toast.LENGTH_SHORT).show()
        }.also { it.tag = "nut_ghim" })
        thanh.addView(nutAnh(R.drawable.ic_cai_dat, "Thêm") { hienThem() })
        return thanh
    }

    private fun dungThan(g: GhiChu?): View {
        val cuon = ScrollView(this).apply { isFillViewport = true }
        val cot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), dp(24))
        }

        oTieuDe = EditText(this).apply {
            hint = "Tiêu đề"
            setHintTextColor(mau.chuMo)
            setTextColor(mau.chuChinh)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = null
            isSingleLine = true
            setPadding(0, dp(8), 0, dp(8))
            setText(g?.tieuDe.orEmpty())
        }
        cot.addView(oTieuDe)

        oChan = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(mau.chuMo)
            text = nhanChan(g)
        }
        cot.addView(oChan)

        oThan = EditText(this).apply {
            hint = "Bắt đầu viết…"
            setHintTextColor(mau.chuMo)
            setTextColor(mau.chuChinh)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            background = null
            gravity = Gravity.TOP
            setLineSpacing(dp(4).toFloat(), 1f)
            setPadding(0, dp(12), 0, 0)
            setText(NoiDungGhiChu.deSua(g?.noiDung.orEmpty()))
        }
        cot.addView(oThan, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        // Ghi chu moi thi mo ban phim luon — nguoi dung bam "+" la de viet, khong
        // phai de ngam trang trang roi bam them mot cai nua.
        if (g == null) {
            oThan.requestFocus()
            oThan.post {
                getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                    ?.showSoftInput(oThan, 0)
            }
        }

        cuon.addView(cot)
        return cuon
    }

    /** Thanh cong cu soan thao. Chang nay moi co o tick va mau giay. */
    private fun dungThanhDuoi(): View {
        val thanh = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        thanh.addView(nutChu("☑", "Ô tick") { doiOTick() })
        thanh.addView(nutChu("●", "Màu giấy") { hienChonMau() })
        thanh.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        thanh.addView(nutChu("⋯", "Chia sẻ") { chiaSe() })
        return thanh
    }

    // ------------------------------------------------------------ hanh dong

    /**
     * Bat/tat o tick tren dong dang dat con tro.
     *
     * Lam viec tren CHUOI chu khong tren mot cau truc khoi rieng: o soan thao la
     * mot `EditText` thuong, o tick chi la ky tu dau dong. Doi lai duoc mot thu
     * quan trong — nguoi dung xoa lui qua o tick la no bien mat nhu moi ky tu
     * khac, khong con trang thai an nao ket lai.
     */
    private fun doiOTick() {
        val chu = oThan.text.toString()
        val viTri = oThan.selectionStart.coerceIn(0, chu.length)
        val dau = chu.lastIndexOf('\n', (viTri - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val cuoi = chu.indexOf('\n', viTri).let { if (it < 0) chu.length else it }
        val dong = chu.substring(dau, cuoi)

        val moi = when {
            dong.startsWith(NoiDungGhiChu.O_TRONG) ->
                NoiDungGhiChu.O_TICK + dong.removePrefix(NoiDungGhiChu.O_TRONG)
            dong.startsWith(NoiDungGhiChu.O_TICK) -> dong.removePrefix(NoiDungGhiChu.O_TICK)
            else -> NoiDungGhiChu.O_TRONG + dong
        }
        oThan.text.replace(dau, cuoi, moi)
        oThan.setSelection((viTri + moi.length - dong.length).coerceIn(0, oThan.text.length))
    }

    private fun hienChonMau() {
        AlertDialog.Builder(this, mau.themeHopThoai)
            .setTitle("Màu giấy")
            .setItems(MauGhiChu.TEN) { _, chon ->
                mauNen = chon
                toMau()
            }
            .show()
    }

    private fun hienThem() {
        val muc = ArrayList<String>()
        val lam = ArrayList<() -> Unit>()

        muc += if (thuMuc.isEmpty()) "Chuyển vào thư mục…" else "Thư mục: $thuMuc"
        lam += { hienChonThuMuc() }

        muc += if (nhacLuc == null) "Đặt nhắc nhở…" else "Bỏ nhắc nhở"
        lam += {
            if (nhacLuc == null) {
                Toast.makeText(this, "Nhắc nhở làm ở chặng sau", Toast.LENGTH_SHORT).show()
            } else {
                nhacLuc = null
                oChan.text = nhanChan(goc)
            }
        }

        muc += "Chia sẻ"
        lam += { chiaSe() }

        if (id != 0L) {
            muc += "Chuyển vào thùng rác"
            lam += {
                kho.boVaoThungRac(id)
                // Da xoa thi dung de onPause luu lai — luu lai la ghi chu song day.
                id = 0L
                goc = null
                finish()
            }
        }

        AlertDialog.Builder(this, mau.themeHopThoai)
            .setItems(muc.toTypedArray()) { _, chon -> lam[chon]() }
            .show()
    }

    private fun hienChonThuMuc() {
        val ds = kho.thuMucKemSo().map { it.first }
        val ten = (listOf("Không thuộc thư mục nào") + ds).toTypedArray()
        AlertDialog.Builder(this, mau.themeHopThoai)
            .setTitle("Thư mục")
            .setItems(ten) { _, chon ->
                thuMuc = if (chon == 0) "" else ds[chon - 1]
                oChan.text = nhanChan(goc)
            }
            .show()
    }

    private fun chiaSe() {
        val chu = (oTieuDe.text.toString() + "\n" + oThan.text.toString()).trim()
        if (chu.isBlank()) {
            Toast.makeText(this, "Ghi chú trống", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, chu)
        }, "Chia sẻ ghi chú"))
    }

    // ------------------------------------------------------------ luu

    /**
     * Ghi xuong CSDL.
     *
     * Ghi chu rong thi khong tao hang moi — bam "+" roi doi y bam back la chuyen
     * thuong, khong nen de lai mot the trang trong danh sach. Nhung ghi chu DA CO
     * ma bi xoa het chu thi van luu: do la y dinh cua nguoi dung.
     */
    private fun luu() {
        val tieuDe = oTieuDe.text.toString().trim()
        val than = oThan.text.toString()
        if (id == 0L && tieuDe.isBlank() && than.isBlank()) return

        val cu = goc
        val moi = GhiChu(
            id = id,
            uid = cu?.uid.orEmpty(),
            tieuDe = tieuDe,
            noiDung = NoiDungGhiChu.tuSua(than),
            thuMuc = thuMuc,
            ghim = ghim,
            mau = mauNen,
            riengTu = cu?.riengTu ?: false,
            nhacLuc = nhacLuc,
            ngayTao = cu?.ngayTao ?: 0L,
        )
        // Khong doi gi thi khong ghi: moi lan ghi la mot lan `ngay_sua` nhay len
        // va ghi chu bi danh dau phai day len may chu lai — chi mo ra xem roi
        // thoat cung sinh mot luot dong bo vo nghia.
        if (cu != null && !daDoi(cu, moi)) return

        id = kho.luu(moi)
        goc = kho.theoId(id)
    }

    private fun daDoi(cu: GhiChu, moi: GhiChu): Boolean =
        cu.tieuDe != moi.tieuDe || cu.noiDung != moi.noiDung || cu.thuMuc != moi.thuMuc ||
            cu.ghim != moi.ghim || cu.mau != moi.mau || cu.nhacLuc != moi.nhacLuc

    // ------------------------------------------------------------ tien ich

    private fun toMau() {
        val nenGiay = MauGhiChu.nen(mauNen, mau)
        nen.setBackgroundColor(nenGiay)
        window.statusBarColor = nenGiay
        nen.findViewWithTag<ImageButton>("nut_ghim")?.setColorFilter(
            if (ghim) mau.nhan else mau.chuChinh,
        )
    }

    private fun nhanChan(g: GhiChu?): String {
        val phan = ArrayList<String>()
        phan += NgayVN.nhanDanhSach(g?.ngaySua ?: System.currentTimeMillis())
        if (thuMuc.isNotEmpty()) phan += thuMuc
        nhacLuc?.let { phan += "Nhắc " + NgayVN.nhanDanhSach(it) }
        return phan.joinToString("  ·  ")
    }

    private fun nutAnh(anh: Int, ten: String, bam: () -> Unit): ImageButton =
        ImageButton(this).apply {
            setImageResource(anh)
            setColorFilter(mau.chuChinh)
            contentDescription = ten
            background = null
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            setOnClickListener { bam() }
        }

    /**
     * Nut hien mot ky tu thay vi mot hinh ve.
     *
     * Chang nay chua ve icon cho o tick va mau giay; ky tu ☑ / ● doc duoc ngay va
     * khong ton mot tep vector cho mot thu se doi o chang sau.
     */
    private fun nutChu(ky: String, ten: String, bam: () -> Unit): View =
        TextView(this).apply {
            text = ky
            contentDescription = ten
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(mau.chuChinh)
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(BangMau.mauMo(mau.chuChinh, 0.06f))
            }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                rightMargin = dp(8)
            }
            isClickable = true
            setOnClickListener { bam() }
        }

    companion object {
        const val KHOA_ID = "id"
        const val KHOA_THU_MUC = "thu_muc"
    }
}
