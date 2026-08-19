package com.ghichu

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Man hinh chinh: luoi ghi chu, tim kiem, thu muc, thung rac.
 *
 * Ba tab duoi cung — Ghi chu / Lich trinh / Tro ly — la ba MAN rieng chu khong
 * phai fragment: hai man kia ([ManLichTrinh], [TroLyAiActivity]) mang nguyen tu
 * ban truoc sang, moi cai da tu lo vong doi cua no. Nhet chung vao mot Activity
 * chi de co chu "fragment" la viet lai ca hai ma khong duoc gi.
 */
class ManHinhChinhActivity : Activity() {

    private val md by lazy { resources.displayMetrics }
    private fun dp(v: Int) = (v * md.density).toInt()

    private lateinit var mau: BangMau
    private val kho: KhoGhiChu get() = Ung.cua(this).ghiChu

    /** `null` = tat ca thu muc. Chuoi rong = ghi chu chua xep thu muc. */
    private var thuMuc: String? = null
    private var tim = ""
    private var thungRac = false
    private var riengTu = false

    /**
     * Cac ghi chu dang duoc chon (che do chon nhieu).
     *
     * Rong = khong o che do chon. Giu id chu khong giu doi tuong: sau moi lan
     * [nap] danh sach la doi tuong moi, giu doi tuong thi vua sua mot ghi chu la
     * mat dau chon.
     */
    private val dangChon = LinkedHashSet<Long>()

    /** Cach nhau it nhat bay nhieu lau moi dong bo ngam mot luot. */
    private val CACH_DONG_BO_MS = 2 * 60 * 1000L

    private var duLieu: List<GhiChu> = emptyList()

    private lateinit var goc: LinearLayout
    private lateinit var oTieuDe: TextView
    private lateinit var oTim: EditText
    private lateinit var luoi: GridView
    private lateinit var thanhTren: LinearLayout
    private lateinit var thanhChon: LinearLayout
    private lateinit var oTrong: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mau = BangMau.cua(this)
        setTheme(mau.themeManHinh)

        goc = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(mau.nen)
        }
        goc.addView(dungThanhTren())
        goc.addView(dungThanhChon())
        goc.addView(dungOTim())
        goc.addView(dungLuoi(), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
        ))
        goc.addView(dungThanhDuoi())

        // Ve tran vien; tu chua khoang cho thanh trang thai va thanh dieu huong.
        goc.setOnApplyWindowInsetsListener { view, insets ->
            val thanh = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(0, thanh.top, 0, thanh.bottom)
            insets
        }
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(mau.nen)
            addView(goc)
            addView(dungNutThem())
        })

        xinQuyenThongBao()

        // Don dep dinh ky, chay o luong khac: ca hai viec deu quet toan bo CSDL,
        // lam o luong chinh la mo app thay khung mot cai khi co nhieu ghi chu.
        Thread {
            // Thung rac khong tu don thi phinh mai, ma nguoi dung khong bao gio
            // nho vao don.
            kho.donRacQuaHan()
            // Xoa ghi chu co anh xong ma khong don thi tep anh nam lai mai.
            KhoAnh.donTepThua(this, Ung.cua(this).kho)
        }.start()
    }

    /**
     * Xin quyen hien thong bao (Android 13 tro len).
     *
     * Xin o day chu khong xin luc dat nhac nho dau tien: he thong chi cho hoi
     * MOT lan: hoi lan sau la hop khong hien nua. Hoi ngay man hinh dau, khi
     * nguoi dung con dang xem app, thi con co co hoi giai thich neu ho tu choi.
     */
    private fun xinQuyenThongBao() {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        val quyen = android.Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(quyen) == android.content.pm.PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(quyen), 1)
    }

    override fun onStop() {
        super.onStop()
        // Roi app la khoa lai. Do la ca diem cua tinh nang: mo khoa mot lan roi
        // de mo mai thi ai cam may cung doc duoc, y het nhu khong ma hoa.
        if (isFinishing || !isChangingConfigurations) {
            KhoaRiengTu.khoaLai()
            if (riengTu) doiCho(null, false)
        }
    }

    override fun onResume() {
        super.onResume()
        // Doi giao dien sang/toi trong Cai dat thi phai dung lai ca man hinh —
        // mau da to thang vao tung view, khong tu doi duoc.
        if (mau.toi != BangMau.laToi(this)) {
            recreate()
            return
        }
        nap()
        dongBoNgam()
    }

    /**
     * Keo/day mot luot o luong nen moi lan quay lai man hinh chinh.
     *
     * Co gian [CACH_DONG_BO_MS] giua hai luot: `onResume` chay ca khi tat man
     * hinh bat lai hay dong mot hop thoai — khong gian thi mot buoi chieu la vai
     * tram luot goi mang.
     *
     * Loi mang thi im lang: day la luot NGAM, nguoi dung khong yeu cau. Ai muon
     * biet ket qua thi vao Cai dat > Dong bo, o do bao ro loi cuoi cung.
     */
    private fun dongBoNgam() {
        if (!DongBo.daDangNhap || DongBo.dangChay) return
        if (System.currentTimeMillis() - DongBo.lanCuoi < CACH_DONG_BO_MS) return

        val kho = Ung.cua(this).kho
        Thread {
            val doi = DongBo.motLuot(this, kho)
            if (doi) runOnUiThread { if (!isFinishing && !isDestroyed) nap() }
        }.start()
    }

    // ------------------------------------------------------------ thanh tren

    private fun dungThanhTren(): View {
        thanhTren = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(8), dp(6))
        }

        oTieuDe = TextView(this).apply {
            text = "Ghi chú"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(mau.chuChinh)
        }
        thanhTren.addView(oTieuDe, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        // Thu tu ban chot: QR nam BEN TRAI hai nut thu muc va cai dat.
        thanhTren.addView(nutAnh(R.drawable.ghichu_ic_qr, "Quét QR") {
            startActivity(Intent(this, QuetQrActivity::class.java))
        })
        thanhTren.addView(nutAnh(R.drawable.ic_thu_muc, "Thư mục") { hienThuMuc() })
        thanhTren.addView(nutAnh(R.drawable.ic_cai_dat, "Cài đặt") { hienCaiDat() })
        return thanhTren
    }

    /**
     * Thanh thay the khi dang chon nhieu ghi chu.
     *
     * Hai thanh nam CHONG len nhau trong cung mot cho va doi nhau bang
     * `visibility`, chu khong phai mot thanh tu doi noi dung: doi noi dung tai
     * cho thi moi lan vao/ra che do chon la ca hang nut nhay mot cai.
     */
    private fun dungThanhChon(): View {
        thanhChon = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(10), dp(8), dp(6))
            visibility = View.GONE
        }
        thanhChon.addView(nutAnh(R.drawable.ic_quay_lai, "Xong") { thoatChon() })

        val nhan = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTextColor(mau.chuChinh)
            tag = "nhan_chon"
        }
        thanhChon.addView(nhan, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        thanhChon.addView(nutAnh(R.drawable.ic_ghim, "Ghim") { ghimDaChon() })
        thanhChon.addView(nutAnh(R.drawable.ic_xoa, "Xoá") { xoaDaChon() })
        return thanhChon
    }

    private fun dungOTim(): View {
        val hop = FrameLayout(this).apply {
            setPadding(dp(16), dp(4), dp(16), dp(10))
        }
        oTim = EditText(this).apply {
            hint = "Tìm ghi chú"
            setHintTextColor(mau.chuMo)
            setTextColor(mau.chuChinh)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            isSingleLine = true
            setPadding(dp(44), dp(10), dp(14), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(mau.nenThanhDuoi)
            }
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    tim = s?.toString().orEmpty()
                    nap()
                }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            })
        }
        hop.addView(oTim, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))

        val kinhLup = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_tim)
            setColorFilter(mau.chuMo)
        }
        hop.addView(kinhLup, FrameLayout.LayoutParams(dp(20), dp(20)).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            leftMargin = dp(30)
        })
        return hop
    }

    // ------------------------------------------------------------ luoi

    private fun dungLuoi(): View {
        val hop = FrameLayout(this)

        luoi = GridView(this).apply {
            numColumns = 2
            horizontalSpacing = dp(12)
            verticalSpacing = dp(12)
            setPadding(dp(16), 0, dp(16), dp(96))
            clipToPadding = false
            // Bo hieu ung chon cua he thong: the tu ve trang thai chon bang vien
            // vang, de ca hai la nhap nhay hai lop.
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            adapter = BoChuyen()
            setOnItemClickListener { _, _, vt, _ -> chamVao(duLieu[vt]) }
            setOnItemLongClickListener { _, _, vt, _ ->
                doiChon(duLieu[vt].id)
                true
            }
        }
        hop.addView(luoi, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
        ))

        oTrong = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(mau.chuMo)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            visibility = View.GONE
        }
        hop.addView(oTrong, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        return hop
    }

    private inner class BoChuyen : BaseAdapter() {
        override fun getCount() = duLieu.size
        override fun getItem(vt: Int) = duLieu[vt]
        override fun getItemId(vt: Int) = duLieu[vt].id

        override fun getView(vt: Int, cu: View?, cha: ViewGroup?): View {
            val the = (cu as? TheGhiChu) ?: TheGhiChu(this@ManHinhChinhActivity)
            val g = duLieu[vt]
            the.gan(g, mau, g.id in dangChon)
            return the
        }
    }

    // ------------------------------------------------------------ thanh duoi

    private fun dungThanhDuoi(): View {
        val thanh = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(mau.nenThanhDuoi)
            setPadding(0, dp(6), 0, dp(6))
        }
        thanh.addView(tabDuoi(R.drawable.ic_ghi_chu, "Ghi chú", true) { })
        thanh.addView(tabDuoi(R.drawable.ic_lich, "Lịch trình", false) {
            startActivity(Intent(this, LichTrinhActivity::class.java))
        })
        thanh.addView(tabDuoi(R.drawable.ic_ai, "Trợ lý", false) {
            startActivity(Intent(this, TroLyAiActivity::class.java))
        })
        return thanh
    }

    private fun tabDuoi(anh: Int, ten: String, dangO: Boolean, bam: () -> Unit): View {
        val cot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            setPadding(0, dp(4), 0, dp(2))
            setOnClickListener { bam() }
        }
        val to = if (dangO) mau.nhan else mau.chuMo
        cot.addView(android.widget.ImageView(this).apply {
            setImageResource(anh)
            setColorFilter(to)
        }, LinearLayout.LayoutParams(dp(22), dp(22)))
        cot.addView(TextView(this).apply {
            text = ten
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(to)
        })
        return cot.also {
            it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    /**
     * Nut tron them ghi chu.
     *
     * Cham nhanh = ghi chu moi; **giu 0,5 giay = mo tro ly AI**. Dung
     * `OnTouchListener` + hen gio chu khong dung `setOnLongClickListener`: nguong
     * cua he thong la 500 ms nhung con phu thuoc cu chi cuon, tung tranh chap
     * o ban truoc.
     */
    private fun dungNutThem(): View {
        val nut = ImageButton(this).apply {
            setImageResource(R.drawable.ic_them)
            setColorFilter(mau.chuTrenNhan)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(mau.nhan)
            }
            contentDescription = "Ghi chú mới"
        }

        val chinh = android.os.Handler(mainLooper)
        var daGiu = false
        val moAi = Runnable {
            daGiu = true
            nut.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            startActivity(Intent(this, TroLyAiActivity::class.java))
        }
        nut.setOnTouchListener { v, su ->
            when (su.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    daGiu = false
                    chinh.postDelayed(moAi, 500)
                    v.isPressed = true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    chinh.removeCallbacks(moAi)
                    v.isPressed = false
                    if (!daGiu) moGhiChu(null)
                }
                android.view.MotionEvent.ACTION_CANCEL -> {
                    chinh.removeCallbacks(moAi)
                    v.isPressed = false
                }
            }
            true
        }

        val o = FrameLayout.LayoutParams(dp(58), dp(58)).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            rightMargin = dp(20)
            bottomMargin = dp(86)
        }
        return FrameLayout(this).apply {
            addView(nut, o)
            // Hop bao chi de dat nut; khong duoc chan cham vao luoi ben duoi.
            isClickable = false
            setOnApplyWindowInsetsListener { view, insets ->
                val thanh = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(0, 0, 0, thanh.bottom)
                insets
            }
        }
    }

    // ------------------------------------------------------------ hanh dong

    private fun chamVao(g: GhiChu) {
        if (dangChon.isNotEmpty()) {
            doiChon(g.id)
            return
        }
        if (thungRac) {
            hienHopThungRac(g)
            return
        }
        moGhiChu(g.id)
    }

    private fun moGhiChu(id: Long?) {
        val i = Intent(this, SuaGhiChuActivity::class.java)
        if (id != null) i.putExtra(SuaGhiChuActivity.KHOA_ID, id)
        // Ghi chu moi tao trong thu muc dang xem — dung cho hon la nem het vao
        // muc goc roi bat nguoi dung chuyen tay.
        thuMuc?.takeIf { it.isNotEmpty() }?.let { i.putExtra(SuaGhiChuActivity.KHOA_THU_MUC, it) }
        startActivity(i)
    }

    private fun doiChon(id: Long) {
        if (!dangChon.add(id)) dangChon.remove(id)
        capNhatThanhChon()
        (luoi.adapter as BaseAdapter).notifyDataSetChanged()
    }

    private fun thoatChon() {
        dangChon.clear()
        capNhatThanhChon()
        (luoi.adapter as BaseAdapter).notifyDataSetChanged()
    }

    private fun capNhatThanhChon() {
        val co = dangChon.isNotEmpty()
        thanhChon.visibility = if (co) View.VISIBLE else View.GONE
        thanhTren.visibility = if (co) View.GONE else View.VISIBLE
        thanhChon.findViewWithTag<TextView>("nhan_chon")?.text = "Đã chọn ${dangChon.size} mục"
    }

    private fun ghimDaChon() {
        // Ghim ca nhom theo trang thai cua ghi chu DAU tien: nua ghim nua khong
        // ma bam "ghim" thi y nguoi dung ro rang la muon ghim het.
        val ghimHet = duLieu.filter { it.id in dangChon }.any { !it.ghim }
        for (id in dangChon) kho.datGhim(id, ghimHet)
        thoatChon()
        nap()
    }

    private fun xoaDaChon() {
        val n = dangChon.size
        if (thungRac) {
            AlertDialog.Builder(this, mau.themeHopThoai)
                .setMessage("Xoá vĩnh viễn $n ghi chú? Không thể hoàn tác.")
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Xoá") { _, _ ->
                    for (id in dangChon) kho.xoaHan(id)
                    thoatChon()
                    nap()
                }
                .show()
            return
        }
        for (id in dangChon) kho.boVaoThungRac(id)
        thoatChon()
        nap()
        Toast.makeText(this, "Đã chuyển $n ghi chú vào thùng rác", Toast.LENGTH_SHORT).show()
    }

    private fun hienHopThungRac(g: GhiChu) {
        AlertDialog.Builder(this, mau.themeHopThoai)
            .setTitle(g.tieuDeHien().ifBlank { "Ghi chú" })
            .setItems(arrayOf("Khôi phục", "Xoá vĩnh viễn")) { _, chon ->
                if (chon == 0) kho.khoiPhuc(g.id) else kho.xoaHan(g.id)
                nap()
            }
            .show()
    }

    // ------------------------------------------------------------ thu muc

    private fun hienThuMuc() {
        val ds = kho.thuMucKemSo()
        val ten = ArrayList<String>()
        val lam = ArrayList<() -> Unit>()

        ten += "Tất cả ghi chú"
        lam += { doiCho(null, false) }
        ten += "Chưa xếp thư mục"
        lam += { doiCho("", false) }
        for ((t, n) in ds) {
            ten += "$t  ($n)"
            lam += { doiCho(t, false) }
        }
        ten += "Riêng tư"
        lam += { moRiengTu() }
        ten += "Thùng rác"
        lam += { doiCho(null, true) }
        ten += "Thư mục mới…"
        lam += { hienThemThuMuc() }

        AlertDialog.Builder(this, mau.themeHopThoai)
            .setTitle("Thư mục")
            .setItems(ten.toTypedArray()) { _, chon -> lam[chon]() }
            .show()
    }

    private fun hienThemThuMuc() {
        val o = EditText(this).apply {
            hint = "Tên thư mục"
            isSingleLine = true
        }
        AlertDialog.Builder(this, mau.themeHopThoai)
            .setTitle("Thư mục mới")
            .setView(o)
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Tạo") { _, _ ->
                if (kho.themThuMuc(o.text.toString())) doiCho(o.text.toString().trim(), false)
                else Toast.makeText(this, "Tên thư mục đã có hoặc để trống", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun doiCho(tm: String?, rac: Boolean, rieng: Boolean = false) {
        thuMuc = tm
        thungRac = rac
        riengTu = rieng
        thoatChon()
        nap()
    }

    // ------------------------------------------------------------ rieng tu

    /**
     * Vao muc Rieng tu: hoi mat khau, sinh khoa o luong nen roi mo.
     *
     * Sinh khoa co y CHAM (PBKDF2 210.000 vong — xem [MaHoa]) nen bat buoc phai o
     * luong nen: lam o luong chinh la giao dien dung hinh gan mot giay moi lan
     * mo khoa, va Android co the bao "ứng dụng không phản hồi".
     */
    private fun moRiengTu() {
        if (KhoaRiengTu.dangMo) {
            doiCho(null, false, rieng = true)
            return
        }
        if (!KhoaRiengTu.daDatMatKhau) {
            hienDatMatKhau()
            return
        }
        hoiMatKhau("Nhập mật khẩu", "Mở") { mk, xong ->
            Thread {
                val duoc = KhoaRiengTu.mo(mk)
                runOnUiThread {
                    xong()
                    if (duoc) doiCho(null, false, rieng = true)
                    else Toast.makeText(this, "Sai mật khẩu", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
    }

    private fun hienDatMatKhau() {
        AlertDialog.Builder(this, mau.themeHopThoai)
            .setTitle("Đặt mật khẩu riêng tư")
            .setMessage(KhoaRiengTu.canhBao)
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Tiếp") { _, _ ->
                hoiMatKhau("Mật khẩu mới", "Đặt") { mk, xong ->
                    if (mk.length < 4) {
                        xong()
                        Toast.makeText(this, "Mật khẩu quá ngắn", Toast.LENGTH_SHORT).show()
                        return@hoiMatKhau
                    }
                    Thread {
                        val duoc = KhoaRiengTu.dat(mk)
                        runOnUiThread {
                            xong()
                            if (duoc) doiCho(null, false, rieng = true)
                        }
                    }.start()
                }
            }
            .show()
    }

    /**
     * Hop nhap mat khau.
     *
     * [lam] nhan mat khau va mot ham `xong` de dong hop cho — sinh khoa mat gan
     * mot giay, khong co gi bao thi nguoi dung tuong app treo va bam lai may lan.
     */
    private fun hoiMatKhau(tieuDe: String, nutOk: String, lam: (String, () -> Unit) -> Unit) {
        val o = EditText(this).apply {
            hint = "Mật khẩu"
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this, mau.themeHopThoai)
            .setTitle(tieuDe)
            .setView(o)
            .setNegativeButton("Huỷ", null)
            .setPositiveButton(nutOk) { _, _ ->
                val cho = android.app.ProgressDialog(this, mau.themeHopThoai).apply {
                    setMessage("Đang mở khoá…")
                    setCancelable(false)
                    show()
                }
                lam(o.text.toString()) { if (cho.isShowing) cho.dismiss() }
            }
            .show()
    }

    private fun hienCaiDat() {
        val muc = arrayOf("Giao diện", "Đồng bộ tài khoản", "Khoá API Gemini")
        AlertDialog.Builder(this, mau.themeHopThoai)
            .setTitle("Cài đặt")
            .setItems(muc) { _, chon ->
                when (chon) {
                    0 -> startActivity(Intent(this, CaiDatGiaoDienActivity::class.java))
                    1 -> startActivity(Intent(this, DongBoActivity::class.java))
                    2 -> hienKhoaGemini()
                }
            }
            .show()
    }

    /**
     * Nhap khoa Gemini bang tay.
     *
     * **KHONG nhung khoa vao APK** — ban da chot vay sau khi nghe ro rui ro: khoa
     * lo la nguoi khac xai het han muc cua ban. Khac han khoa `publishable` cua
     * Supabase (thu do von duoc thiet ke de lo cong khai).
     */
    private fun hienKhoaGemini() {
        val o = EditText(this).apply {
            hint = "Dán khoá API vào đây"
            isSingleLine = true
            setText(Gemini.khoa)
        }
        AlertDialog.Builder(this, mau.themeHopThoai)
            .setTitle("Khoá API Gemini")
            .setView(o)
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Lưu") { _, _ ->
                Gemini.khoa = o.text.toString().trim()
            }
            .show()
    }

    // ------------------------------------------------------------ nap

    private fun nap() {
        duLieu = kho.danhSach(
            thuMuc = if (riengTu) null else thuMuc,
            tim = tim,
            riengTu = riengTu,
            thungRac = thungRac,
        )
        (luoi.adapter as BaseAdapter).notifyDataSetChanged()

        oTieuDe.text = when {
            riengTu -> "Riêng tư"
            thungRac -> "Thùng rác"
            thuMuc == null -> "Ghi chú"
            thuMuc!!.isEmpty() -> "Chưa xếp thư mục"
            else -> thuMuc
        }

        oTrong.visibility = if (duLieu.isEmpty()) View.VISIBLE else View.GONE
        oTrong.text = when {
            tim.isNotBlank() -> "Không tìm thấy ghi chú nào"
            thungRac -> "Thùng rác trống"
            riengTu -> "Chưa có ghi chú riêng tư.\nMở một ghi chú rồi chọn “Chuyển vào riêng tư”."
            else -> "Chưa có ghi chú.\nBấm dấu + để viết ghi chú đầu tiên."
        }
    }

    // ------------------------------------------------------------ tien ich

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

    override fun onBackPressed() {
        // Back quay ve mot buoc chu khong thoat app ngay — dung y ban da chot.
        when {
            dangChon.isNotEmpty() -> thoatChon()
            tim.isNotBlank() -> oTim.setText("")
            riengTu || thungRac || thuMuc != null -> doiCho(null, false)
            else -> super.onBackPressed()
        }
    }
}
