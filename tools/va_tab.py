# -*- coding: utf-8 -*-
# Nhung Lich trinh vao man chinh (thanh tab LUON hien, nhu ban goc) va bo tab Tro ly
# (ban goc chi co HAI tab; AI vao bang giu nut +, dung yeu cau da chot).
import io, os, sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
p = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'kotlin', 'com', 'ghichu', 'ManHinhChinhActivity.kt')
s = io.open(p, encoding='utf-8').read()

def thay(a, b, ten):
    global s
    if a not in s:
        raise SystemExit('THIEU NEO: ' + ten)
    s = s.replace(a, b, 1)
    print('va', ten)

# 1. Khai bao
thay('''    private var demDuoi = 0''',
'''    private var demDuoi = 0

    /** 0 = Ghi chu, 1 = Lich trinh. Thanh tab LUON hien — hai tab la hai vung noi dung. */
    private var tabHienTai = 0
    private lateinit var khungGhiChu: LinearLayout
    private var khungLich: View? = null
    private var manLich: ManLichTrinh? = null
    private val cacTab = ArrayList<Pair<ImageView, TextView>>()
    private lateinit var nutQr: View
    private lateinit var nutThuMuc: View''', 'khai bao tab')

# 2. Cau truc: phan noi dung ghi chu gom vao khungGhiChu; them khungLich canh no
thay('''        goc.addView(dungThanhTren())
        goc.addView(dungTieuDeLon())
        goc.addView(dungOTim())
        goc.addView(dungHangChip())
        goc.addView(dungLuoi(), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
        ))
        goc.addView(dungThanhDuoi())''',
'''        goc.addView(dungThanhTren())
        goc.addView(dungTieuDeLon())

        // Vung noi dung: hai tab la hai khung chong nhau, doi bang visibility —
        // thanh tren + thanh tab dung yen, dung nhu Navigator cua ban goc.
        khungGhiChu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        khungGhiChu.addView(dungOTim())
        khungGhiChu.addView(dungHangChip())
        khungGhiChu.addView(dungLuoi(), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
        ))
        val noiDung = FrameLayout(this)
        noiDung.addView(khungGhiChu, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        goc.addView(noiDung, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        khungNoiDung = noiDung
        goc.addView(dungThanhDuoi())''', 'cau truc noi dung')

thay('''    private lateinit var khungGhiChu: LinearLayout''',
'''    private lateinit var khungGhiChu: LinearLayout
    private lateinit var khungNoiDung: FrameLayout''', 'khai bao khungNoiDung')

# 3. Nut QR / thu muc giu tham chieu de an khi o tab Lich trinh (ban goc chi co banh rang)
thay('''        phai.addView(nutAnh(R.drawable.ghichu_ic_qr, "Quét QR") {
            startActivity(Intent(this, QuetQrActivity::class.java))
        })
        phai.addView(nutAnh(R.drawable.ic_thu_muc, "Thư mục") { hienThuMuc() })
        phai.addView(nutAnh(R.drawable.ic_cai_dat, "Cài đặt") { hienCaiDat() })''',
'''        nutQr = nutAnh(R.drawable.ghichu_ic_qr, "Quét QR") {
            startActivity(Intent(this, QuetQrActivity::class.java))
        }
        nutThuMuc = nutAnh(R.drawable.ic_thu_muc, "Thư mục") { hienThuMuc() }
        phai.addView(nutQr)
        phai.addView(nutThuMuc)
        phai.addView(nutAnh(R.drawable.ic_cai_dat, "Cài đặt") { hienCaiDat() })''', 'nut qr/thu muc')

# 4. Thanh tab: HAI tab, doi vung noi dung
thay('''        hangTab.addView(tabDuoi(R.drawable.ic_ghi_chu, "Ghi chú", true) { })
        hangTab.addView(tabDuoi(R.drawable.ic_lich, "Lịch trình", false) {
            startActivity(Intent(this, LichTrinhActivity::class.java))
        })
        hangTab.addView(tabDuoi(R.drawable.ic_ai, "Trợ lý", false) {
            startActivity(Intent(this, TroLyAiActivity::class.java))
        })
        thanhDuoi.addView(hangTab)''',
'''        // Ban goc co dung HAI tab (Ghi chu / Nhiem vu); o day Nhiem vu thay bang
        // Lich trinh theo yeu cau. KHONG co tab Tro ly — AI vao bang giu nut +.
        hangTab.addView(tabDuoi(R.drawable.ic_ghi_chu, "Ghi chú", true) { chuyenTab(0) })
        hangTab.addView(tabDuoi(R.drawable.ic_lich, "Lịch trình", false) { chuyenTab(1) })
        thanhDuoi.addView(hangTab)''', 'hai tab')

thay('''        val to = if (dangO) mau.tabChon else mau.tabThuong
        cot.addView(ImageView(this).apply {
            setImageResource(anh)
            setColorFilter(to)
        }, LinearLayout.LayoutParams(dp(28), dp(28)))
        // LinearLayout doc cap MATCH_PARENT mac dinh cho con -> chu can TRAI ca cot;
        // phai tu can giua, khong thi nhan lech han khoi icon.
        cot.addView(TextView(this).apply {
            text = ten
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(to)
            isSingleLine = true
            gravity = Gravity.CENTER_HORIZONTAL
        })
        nhanMoDan(cot)''',
'''        val to = if (dangO) mau.tabChon else mau.tabThuong
        val icon = ImageView(this).apply {
            setImageResource(anh)
            setColorFilter(to)
        }
        cot.addView(icon, LinearLayout.LayoutParams(dp(28), dp(28)))
        // LinearLayout doc cap MATCH_PARENT mac dinh cho con -> chu can TRAI ca cot;
        // phai tu can giua, khong thi nhan lech han khoi icon.
        val nhan = TextView(this).apply {
            text = ten
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(to)
            isSingleLine = true
            gravity = Gravity.CENTER_HORIZONTAL
        }
        cot.addView(nhan)
        if (!vang) cacTab += icon to nhan
        nhanMoDan(cot)''', 'tab giu tham chieu')

# 5. Ham chuyen tab
thay('''    /**
     * Nut them: tron 58dp (floating_action_button_size)''',
'''    /**
     * Chuyen tab. Thanh tren va thanh tab dung yen; chi vung noi dung doi.
     *
     * Tab Lich trinh: tieu de lon = thang dang xem (ManLichTrinh bao qua
     * `datTieuDe`), an o tim / chip / nut them cua ghi chu (Lich trinh co nut +
     * rieng o thanh duoi cua no), an nut QR va thu muc — ban goc o tab thu hai chi
     * con banh rang.
     */
    private fun chuyenTab(i: Int) {
        if (i == tabHienTai) return
        if (dangChon.isNotEmpty()) thoatChon()
        if (dangTim) thoatCheDoTim()
        tabHienTai = i

        for ((k, cap) in cacTab.withIndex()) {
            val to = if (k == i) mau.tabChon else mau.tabThuong
            cap.first.setColorFilter(to)
            cap.second.setTextColor(to)
        }

        if (i == 1 && khungLich == null) {
            val man = ManLichTrinh(this)
            man.datTieuDe = { chu ->
                if (tabHienTai == 1) {
                    tieuDeLon.text = chu
                    tieuDeNho.text = chu
                }
            }
            val v = man.taoView()
            manLich = man
            khungLich = v
            khungNoiDung.addView(v, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
            ))
        }

        val laLich = i == 1
        khungGhiChu.visibility = if (laLich) View.GONE else View.VISIBLE
        khungLich?.visibility = if (laLich) View.VISIBLE else View.GONE
        nutThem.visibility = if (laLich) View.GONE else View.VISIBLE
        nutQr.visibility = if (laLich) View.GONE else View.VISIBLE
        nutThuMuc.visibility = if (laLich) View.GONE else View.VISIBLE
        tieuDeLon.alpha = 1f
        tieuDeNho.alpha = 0f

        if (laLich) {
            manLich?.nap()
            manLich?.dongBoNgam()
        } else {
            nap()
        }
    }

    /**
     * Nut them: tron 58dp (floating_action_button_size)''', 'chuyenTab')

# 6. onResume / onDestroy / back
thay('''        nap()
        dongBoNgam()
    }

    /**
     * Keo/day mot luot o luong nen moi lan quay lai man hinh chinh.''',
'''        if (tabHienTai == 1) manLich?.nap() else nap()
        dongBoNgam()
    }

    override fun onDestroy() {
        manLich?.dong()
        super.onDestroy()
    }

    /**
     * Keo/day mot luot o luong nen moi lan quay lai man hinh chinh.''', 'onResume/onDestroy')

thay('''        when {
            dangChon.isNotEmpty() -> thoatChon()
            dangTim -> thoatCheDoTim()''',
'''        when {
            dangChon.isNotEmpty() -> thoatChon()
            dangTim -> thoatCheDoTim()
            // Back o tab thu hai thi ve tab dau, chua thoat app — dung y ban chot.
            tabHienTai != 0 -> chuyenTab(0)''', 'back ve tab dau')

# 7. nap(): khong ghi de tieu de khi dang o tab Lich trinh
thay('''        tieuDeLon.text = ten
        tieuDeNho.text = ten

        if (dangTim) {''',
'''        if (tabHienTai == 0) {
            tieuDeLon.text = ten
            tieuDeNho.text = ten
        }

        if (dangTim) {''', 'nap tieu de theo tab')

# 8. Cai dat: them loi vao Tro ly AI (khong con tab)
thay('''        val muc = arrayOf("Giao diện", "Đồng bộ tài khoản", "Khoá API Gemini")
        HopThoai(this, mau)
            .setTitle("Cài đặt")
            .setItems(muc) { _, chon ->
                when (chon) {
                    0 -> startActivity(Intent(this, CaiDatGiaoDienActivity::class.java))
                    1 -> startActivity(Intent(this, DongBoActivity::class.java))
                    2 -> hienKhoaGemini()
                }
            }
            .show()''',
'''        val muc = arrayOf("Trợ lý AI", "Giao diện", "Đồng bộ tài khoản", "Khoá API Gemini")
        HopThoai(this, mau)
            .setTitle("Cài đặt")
            .setItems(muc) { _, chon ->
                when (chon) {
                    0 -> startActivity(Intent(this, TroLyAiActivity::class.java))
                    1 -> startActivity(Intent(this, CaiDatGiaoDienActivity::class.java))
                    2 -> startActivity(Intent(this, DongBoActivity::class.java))
                    3 -> hienKhoaGemini()
                }
            }
            .show()''', 'cai dat co Tro ly')

io.open(p, 'w', encoding='utf-8').write(s)
print('XONG')
