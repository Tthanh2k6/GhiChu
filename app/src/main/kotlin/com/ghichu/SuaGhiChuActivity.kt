package com.ghichu

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Soan mot ghi chu.
 *
 * Ca bai nam trong **MOT `EditText`** — ly do va cach lam o [SoanThao].
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

        nen = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        nen.addView(dungThanhTren())
        nen.addView(dungThan(g), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
        ))
        nen.addView(dungThanhCongCu())

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
        thanh.addView(nutAnh(R.drawable.ic_quay_lai, "Quay lại") { finish() })
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
            setText(SoanThao.doc(g?.noiDung.orEmpty()), TextView.BufferType.SPANNABLE)
            setOnTouchListener { _, su -> chamVaoThan(su) }
        }
        cot.addView(oThan, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        // Phai doi do rong that cua o soan thao roi moi ve anh — luc dung view no
        // van con bang 0, ve luc do la anh be ti mot chuong.
        oThan.post { veAnh() }

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

    /**
     * Thanh cong cu soan thao.
     *
     * Cuon ngang duoc: chin nut khong vua be ngang may nho, ma bo bot nut nao
     * cung la mot chuc nang bien mat.
     */
    private fun dungThanhCongCu(): View {
        val hang = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(4))
        }
        hang.addView(nutKieu("B", "Đậm", DinhDang.DAM, dam = true))
        hang.addView(nutKieu("I", "Nghiêng", DinhDang.NGHIENG, nghieng = true))
        hang.addView(nutKieu("U", "Gạch chân", DinhDang.GACH_CHAN))
        hang.addView(nutKieu("S", "Gạch ngang", DinhDang.GACH_NGANG))
        hang.addView(nutKieu("A", "Cỡ chữ lớn", DinhDang.CHU_TO))
        hang.addView(nutChu("🎨", "Màu chữ") { hienMauChu() })
        hang.addView(nutChu("☑", "Ô tick") { doiOTick() })
        hang.addView(nutChu("🖼", "Chèn ảnh") { chonAnh() })
        hang.addView(nutChu("—", "Đường kẻ") { chenDuongKe() })

        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(hang)
        }
    }

    // ------------------------------------------------------------ dinh dang

    /**
     * Doan dang duoc ap dinh dang.
     *
     * Chua boi den gi thi lay TU dang dat con tro. Khong lam vay thi bam "B" luc
     * con tro dung giua cau khong xay ra gi ca, ma nguoi dung khong hieu tai sao
     * nut khong an.
     */
    private fun vungAp(): Pair<Int, Int> {
        val chu = oThan.text
        var dau = oThan.selectionStart.coerceIn(0, chu.length)
        var cuoi = oThan.selectionEnd.coerceIn(0, chu.length)
        if (dau > cuoi) dau = cuoi.also { cuoi = dau }
        if (dau != cuoi) return dau to cuoi

        while (dau > 0 && !chu[dau - 1].isWhitespace()) dau--
        while (cuoi < chu.length && !chu[cuoi].isWhitespace()) cuoi++
        return dau to cuoi
    }

    private fun apKieu(co: Int) {
        val (dau, cuoi) = vungAp()
        if (dau >= cuoi) {
            Toast.makeText(this, "Bôi đen phần chữ muốn đổi", Toast.LENGTH_SHORT).show()
            return
        }
        SoanThao.doiKieu(oThan.text as Spannable, dau, cuoi, co)
    }

    private fun hienMauChu() {
        val ten = arrayOf("Mặc định", "Đỏ", "Cam", "Xanh lá", "Xanh dương", "Tím")
        val gt = arrayOf<Int?>(
            null, 0xFFE05252.toInt(), 0xFFE8A33D.toInt(),
            0xFF34A853.toInt(), 0xFF4A90D9.toInt(), 0xFF9B6DD6.toInt(),
        )
        AlertDialog.Builder(this, mau.themeHopThoai)
            .setTitle("Màu chữ")
            .setItems(ten) { _, chon ->
                val (dau, cuoi) = vungAp()
                SoanThao.datMauChu(oThan.text as Spannable, dau, cuoi, gt[chon])
            }
            .show()
    }

    /**
     * Bat/tat o tick tren dong dang dat con tro.
     *
     * O tick la KY TU dau dong chu khong phai mot trang thai rieng — nho vay xoa
     * lui qua no la no bien mat nhu moi ky tu khac.
     */
    private fun doiOTick() {
        val chu = oThan.text
        val viTri = oThan.selectionStart.coerceIn(0, chu.length)
        val dau = chu.toString().lastIndexOf('\n', (viTri - 1).coerceAtLeast(0))
            .let { if (it < 0) 0 else it + 1 }
        val cuoi = chu.toString().indexOf('\n', viTri).let { if (it < 0) chu.length else it }
        val dong = chu.subSequence(dau, cuoi).toString()

        val moi = when {
            dong.startsWith(SoanThao.O_TRONG) ->
                SoanThao.O_TICK + dong.removePrefix(SoanThao.O_TRONG)
            dong.startsWith(SoanThao.O_TICK) -> dong.removePrefix(SoanThao.O_TICK)
            else -> SoanThao.O_TRONG + dong
        }
        // Chi thay phan DAU dong chu khong ca dong: thay ca dong la moi span dinh
        // dang trong do bi xoa sach theo.
        thayDauDong(dau, dong, moi)
        oThan.setSelection((viTri + moi.length - dong.length).coerceIn(0, oThan.text.length))
    }

    private fun thayDauDong(dau: Int, cu: String, moi: String) {
        val chu = oThan.text
        when {
            moi.length > cu.length -> chu.insert(dau, moi.substring(0, moi.length - cu.length))
            moi.length < cu.length -> chu.delete(dau, dau + (cu.length - moi.length))
            else -> chu.replace(dau, dau + 2, moi.substring(0, 2))
        }
    }

    /**
     * Cham vao o soan thao — cham dung o tick thi bat/tat no.
     *
     * Tra `false` cho moi truong hop khac de `EditText` xu ly nhu binh thuong:
     * nuot su kien la mat luon dat con tro, boi den, cuon.
     */
    private fun chamVaoThan(su: MotionEvent): Boolean {
        if (su.actionMasked != MotionEvent.ACTION_UP) return false
        val bo = oThan.layout ?: return false

        val x = su.x - oThan.totalPaddingLeft + oThan.scrollX
        val y = su.y - oThan.totalPaddingTop + oThan.scrollY
        val hang = bo.getLineForVertical(y.toInt())
        val dauDong = bo.getLineStart(hang)
        val chu = oThan.text

        if (dauDong + 2 > chu.length) return false
        val dau = chu.subSequence(dauDong, dauDong + 2).toString()
        if (dau != SoanThao.O_TRONG && dau != SoanThao.O_TICK) return false

        // Chi tinh la "cham vao o tick" khi cham dung ben trai o vuong; cham vao
        // chu phia sau van la dat con tro nhu thuong.
        if (x > bo.getPrimaryHorizontal(dauDong + 2)) return false

        oThan.setSelection(dauDong)
        doiOTick()
        return true
    }

    private fun chenDuongKe() {
        val chu = oThan.text
        val vt = oThan.selectionEnd.coerceIn(0, chu.length)
        val truoc = if (vt > 0 && chu[vt - 1] != '\n') "\n" else ""
        chu.insert(vt, truoc + SoanThao.DUONG_KE + "\n")
    }

    // ------------------------------------------------------------ anh

    private fun chonAnh() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            },
            MA_CHON_ANH,
        )
    }

    override fun onActivityResult(ma: Int, ketQua: Int, du: Intent?) {
        super.onActivityResult(ma, ketQua, du)
        if (ma != MA_CHON_ANH || ketQua != RESULT_OK) return
        val nguon = du?.data ?: return

        val ten = KhoAnh.them(this, nguon)
        if (ten == null) {
            Toast.makeText(this, "Không đọc được ảnh", Toast.LENGTH_SHORT).show()
            return
        }
        chenAnh(ten)
    }

    private fun chenAnh(ten: String) {
        val chu = oThan.text
        val vt = oThan.selectionEnd.coerceIn(0, chu.length)
        val truoc = if (vt > 0 && chu[vt - 1] != '\n') "\n" else ""

        val them = SpannableStringBuilder(truoc + DinhDang.THAY_ANH + "\n")
        val dau = truoc.length
        them.setSpan(
            SoanThao.TheAnh(ten), dau, dau + DinhDang.THAY_ANH.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        chu.insert(vt, them)
        veAnh()
    }

    /**
     * Ve anh that len nhung cho da danh dau [SoanThao.TheAnh].
     *
     * Tach khoi [SoanThao] vi ve anh can `Context` va do rong that cua o soan
     * thao; ben do co y giu thuan Kotlin de kiem thu tren JVM duoc.
     */
    private fun veAnh() {
        val chu = oThan.text as? Spannable ?: return
        val rong = (oThan.width - oThan.paddingLeft - oThan.paddingRight).takeIf { it > 0 } ?: return

        for (the in chu.getSpans(0, chu.length, SoanThao.TheAnh::class.java)) {
            val dau = chu.getSpanStart(the)
            val cuoi = chu.getSpanEnd(the)
            if (dau < 0 || cuoi <= dau) continue
            // Da ve roi thi thoi, khong ve chong len lan nua moi lan quay lai man hinh.
            if (chu.getSpans(dau, cuoi, ImageSpan::class.java).isNotEmpty()) continue

            chu.setSpan(
                ImageSpan(KhoAnh.anhVe(this, the.ten, rong, mau), ImageSpan.ALIGN_BASELINE),
                dau, cuoi, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }

    // ------------------------------------------------------------ hanh dong

    private fun hienThem() {
        val muc = ArrayList<String>()
        val lam = ArrayList<() -> Unit>()

        muc += "Màu giấy"
        lam += { hienChonMauGiay() }

        muc += if (thuMuc.isEmpty()) "Chuyển vào thư mục…" else "Thư mục: $thuMuc"
        lam += { hienChonThuMuc() }

        muc += if (nhacLuc == null) "Đặt nhắc nhở…" else "Bỏ nhắc nhở"
        lam += { if (nhacLuc == null) hienDatNhac() else datNhac(null) }

        muc += if (goc?.riengTu == true) "Bỏ riêng tư" else "Chuyển vào riêng tư"
        lam += { doiRiengTu() }

        muc += "Chia sẻ / xuất…"
        lam += { hienXuat() }

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

    private fun hienChonMauGiay() {
        AlertDialog.Builder(this, mau.themeHopThoai)
            .setTitle("Màu giấy")
            .setItems(MauGhiChu.TEN) { _, chon ->
                mauNen = chon
                toMau()
            }
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

    /**
     * Chon ngay roi chon gio de nhac.
     *
     * Hai hop noi tiep nhau chu khong mot hop gop: khung co san
     * (`DatePickerDialog`, `TimePickerDialog`) da theo dung thoi quen cua may va
     * cua ngon ngu he thong — tu ve mot hop gop la mat het cai do de doi lay ve
     * ngoai dep hon mot chut.
     */
    private fun hienDatNhac() {
        val moc = java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            // Mac dinh la gio tron ke tiep — khong ai dat nhac vao 14:37.
            add(java.util.Calendar.HOUR_OF_DAY, 1)
            set(java.util.Calendar.MINUTE, 0)
        }

        android.app.DatePickerDialog(
            this, mau.themeHopThoai,
            { _, nam, thang, ngay ->
                android.app.TimePickerDialog(
                    this, mau.themeHopThoai,
                    { _, gio, phut ->
                        moc.set(nam, thang, ngay, gio, phut)
                        moc.set(java.util.Calendar.SECOND, 0)
                        moc.set(java.util.Calendar.MILLISECOND, 0)
                        if (moc.timeInMillis <= System.currentTimeMillis()) {
                            Toast.makeText(this, "Mốc đó đã qua rồi", Toast.LENGTH_SHORT).show()
                        } else {
                            datNhac(moc.timeInMillis)
                        }
                    },
                    moc.get(java.util.Calendar.HOUR_OF_DAY), moc.get(java.util.Calendar.MINUTE),
                    android.text.format.DateFormat.is24HourFormat(this),
                ).show()
            },
            moc.get(java.util.Calendar.YEAR),
            moc.get(java.util.Calendar.MONTH),
            moc.get(java.util.Calendar.DAY_OF_MONTH),
        ).show()
    }

    /**
     * Ghi moc nhac XUONG NGAY, khong doi `onPause`.
     *
     * Bao thuc chi duoc hen lai khi CSDL doi. De den `onPause` thi nguoi dung dat
     * gio xong, bam back, va van phai may man moi kip — con neu ho bam "Chuyen
     * vao thung rac" ngay sau do thi moc vua dat mat luon.
     */
    private fun datNhac(luc: Long?) {
        nhacLuc = luc
        if (id == 0L) luu()
        if (id != 0L) {
            kho.datNhac(id, luc)
            goc = kho.theoId(id)
        }
        oChan.text = nhanChan(goc)
        Toast.makeText(
            this,
            if (luc == null) "Đã bỏ nhắc nhở" else "Sẽ nhắc lúc " + NgayVN.nhanDanhSach(luc),
            Toast.LENGTH_SHORT,
        ).show()
    }

    /**
     * Chuyen ghi chu vao/ra muc rieng tu.
     *
     * Ghi XUONG NGAY chu khong doi `onPause`, va di qua [luu] de noi dung duoc
     * ma hoa (hoac giai ma) dung mot lan. Chua dat mat khau thi phai dat truoc —
     * khong co khoa thi khong ma hoa duoc gi.
     */
    private fun doiRiengTu() {
        val muon = goc?.riengTu != true
        if (muon && !KhoaRiengTu.dangMo) {
            Toast.makeText(
                this,
                if (KhoaRiengTu.daDatMatKhau) "Mở khoá mục Riêng tư ở màn hình chính trước"
                else "Vào Thư mục > Riêng tư để đặt mật khẩu trước",
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        // Phai luu phan dang go TRUOC khi doi co: [luu] doc co tu `goc`, doi co
        // ma chua luu thi phan vua go bi ghi bang co cu.
        luu()
        val g = if (id != 0L) kho.theoId(id) else null
        if (g == null) {
            Toast.makeText(this, "Ghi chú trống", Toast.LENGTH_SHORT).show()
            return
        }
        goc = g
        id = kho.luu(
            GhiChu(
                id = g.id, uid = g.uid, tieuDe = g.tieuDe, noiDung = g.noiDung,
                thuMuc = g.thuMuc, ghim = g.ghim, mau = g.mau,
                riengTu = muon, nhacLuc = g.nhacLuc, ngayTao = g.ngayTao,
            ),
        )
        goc = kho.theoId(id)
        Toast.makeText(
            this,
            if (muon) "Đã chuyển vào Riêng tư" else "Đã bỏ khỏi Riêng tư",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun hienXuat() {
        if (oTieuDe.text.isBlank() && oThan.text.isBlank()) {
            Toast.makeText(this, "Ghi chú trống", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this, mau.themeHopThoai)
            .setTitle("Chia sẻ / xuất")
            .setItems(arrayOf("Văn bản", "Ảnh PNG", "PDF")) { _, chon ->
                when (chon) {
                    0 -> chiaSeChu()
                    1 -> xuatTep("image/png") { XuatGhiChu.anh(this, tieuDe(), than(), mau) }
                    2 -> xuatTep("application/pdf") { XuatGhiChu.pdf(this, tieuDe(), than(), mau) }
                }
            }
            .show()
    }

    private fun tieuDe(): String = oTieuDe.text.toString().trim()

    /**
     * Noi dung de xuat, GIU nguyen dinh dang.
     *
     * Tra thang `oThan.text` chu khong `.toString()`: doi sang String la mat het
     * span — chu dam, mau chu, anh deu bien thanh chu tron.
     */
    private fun than(): CharSequence = oThan.text

    private fun chiaSeChu() {
        val chu = (tieuDe() + "\n" + than().toString()).trim()
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, chu)
        }, "Chia sẻ ghi chú"))
    }

    /**
     * Dung tep roi mo bang chon chia se.
     *
     * Dung tep o LUONG NEN: ve mot ghi chu dai ra PDF nhieu trang mat vai tram
     * mili giay, du de giao dien khung mot cai ngay sau khi nguoi dung bam.
     */
    private fun xuatTep(kieu: String, dung: () -> java.io.File) {
        TepChiaSe.don(this)
        Thread {
            val f = try {
                dung()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Không xuất được: " + e.message, Toast.LENGTH_LONG).show()
                }
                return@Thread
            }
            val uri = TepChiaSe.uriCua(this, f)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = kieu
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, tieuDe())
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        "Chia sẻ ghi chú",
                    ),
                )
            }
        }.start()
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
        val than = oThan.text
        if (id == 0L && tieuDe.isBlank() && than.isBlank()) return

        val cu = goc
        val moi = GhiChu(
            id = id,
            uid = cu?.uid.orEmpty(),
            tieuDe = tieuDe,
            noiDung = SoanThao.ghi(than),
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

    /** Nut dinh dang, chinh no hien dung kieu ma no ap — `B` in dam, `I` nghieng. */
    private fun nutKieu(
        ky: String,
        ten: String,
        co: Int,
        dam: Boolean = false,
        nghieng: Boolean = false,
    ): View = nutChu(ky, ten) { apKieu(co) }.apply {
        val t = this as TextView
        t.setTypeface(
            t.typeface,
            when {
                dam && nghieng -> android.graphics.Typeface.BOLD_ITALIC
                dam -> android.graphics.Typeface.BOLD
                nghieng -> android.graphics.Typeface.ITALIC
                else -> android.graphics.Typeface.NORMAL
            },
        )
    }

    private fun nutChu(ky: String, ten: String, bam: () -> Unit): View =
        TextView(this).apply {
            text = ky
            contentDescription = ten
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTextColor(mau.chuChinh)
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(BangMau.mauMo(mau.chuChinh, 0.06f))
            }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { rightMargin = dp(8) }
            isClickable = true
            setOnClickListener { bam() }
        }

    companion object {
        const val KHOA_ID = "id"
        const val KHOA_THU_MUC = "thu_muc"
        private const val MA_CHON_ANH = 1
    }
}
