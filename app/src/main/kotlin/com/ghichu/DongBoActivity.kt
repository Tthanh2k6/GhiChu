package com.ghichu

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Man **Đồng bộ** — vao tu Cai dat.
 *
 * Thay cho muc "Xiaomi Cloud" da bo. Ban mod ky bang khoa khac Xiaomi nen thieu
 * 29 quyen muc `signature`, dong bo Mi Cloud khong bao gio chay duoc — de lai
 * chi lam nguoi dung bam vao roi that vong.
 *
 * Giao dien dung bang code chu khong dung R.layout: ma tai nguyen sinh o module
 * nay khong khop bang tai nguyen cua app dich.
 */
class DongBoActivity : Activity() {

    private lateinit var kho: KhoDuLieu

    private lateinit var oEmail: EditText
    private lateinit var oMatKhau: EditText
    private lateinit var oTrangThai: TextView
    private lateinit var khungChuaDangNhap: LinearLayout
    private lateinit var khungDaDangNhap: LinearLayout
    private lateinit var oTaiKhoan: TextView

    private val nen = Executors.newSingleThreadExecutor()
    private val chinh = Handler(Looper.getMainLooper())

    private val mau by lazy { BangMau.cua(this) }
    private val md by lazy { resources.displayMetrics }
    private fun dp(v: Int) = (v * md.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Truoc moi thu khac: `Button`/`EditText` lay mau tu THEME, khong
        // tu BangMau — dat sai la nut den si tren nen trang.
        setTheme(BangMau.cua(this).themeManHinh)
        title = "Đồng bộ"
        DongBo.khoiTao(this)
        kho = Ung.cua(this).kho

        val cuon = ScrollView(this).apply {
            setBackgroundColor(mau.nen)
            setOnApplyWindowInsetsListener { v, insets ->
                val thanh = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(thanh.left, thanh.top, thanh.right, thanh.bottom)
                insets
            }
        }
        cuon.addView(thanThan())
        setContentView(cuon)
        veLaiTrangThai()
    }

    override fun onDestroy() {
        nen.shutdown()
        super.onDestroy()
    }

    // ---- Dung giao dien ----

    private fun thanThan(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(28))

        addView(tieuDe("Đồng bộ"))
        addView(
            chuPhu(
                "Ghi chú và Lịch trình được đẩy lên máy chủ riêng của bạn (Supabase). " +
                    "Máy vẫn ghi thẳng vào bộ nhớ trong — mất mạng vẫn dùng bình thường.",
            ),
        )

        oTrangThai = chuPhu("").apply { setPadding(0, dp(12), 0, dp(4)) }
        addView(oTrangThai)

        // --- Chua dang nhap ---
        khungChuaDangNhap = LinearLayout(this@DongBoActivity).apply {
            orientation = LinearLayout.VERTICAL

            oEmail = oNhap("Email", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
            oMatKhau = oNhap(
                "Mật khẩu",
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            )
            addView(oEmail)
            addView(oMatKhau)

            addView(
                LinearLayout(this@DongBoActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(nut("Đăng nhập") { vaoTaiKhoan(taoMoi = false) }, cot())
                    addView(nut("Tạo tài khoản") { vaoTaiKhoan(taoMoi = true) }, cot())
                },
            )
        }
        addView(khungChuaDangNhap)

        // --- Da dang nhap ---
        khungDaDangNhap = LinearLayout(this@DongBoActivity).apply {
            orientation = LinearLayout.VERTICAL
            oTaiKhoan = chuChinh("")
            addView(oTaiKhoan)
            addView(
                LinearLayout(this@DongBoActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(nut("Đồng bộ ngay") { dongBoNgay() }, cot())
                    addView(nut("Đăng xuất") { thoatTaiKhoan() }, cot())
                },
            )
        }
        addView(khungDaDangNhap)

        addView(
            chuPhu(
                "\nẢnh, ghi âm, vẽ tay và bản đồ tư duy KHÔNG đồng bộ — chúng là tệp " +
                    "nằm ngoài cơ sở dữ liệu. Ghi chú trong khu vực riêng tư cũng được " +
                    "giữ lại trong máy, không đẩy lên.",
            ),
        )
    }

    /** Tieu de lon kieu miuix: 32sp thuong (miuix_font_size_title1), khong dam. */
    private fun tieuDe(chu: String) = TextView(this).apply {
        text = chu
        setTextColor(mau.chuChinh)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        setPadding(0, dp(4), 0, dp(10))
    }

    private fun chuChinh(chu: String) = TextView(this).apply {
        text = chu
        setTextColor(mau.chuChinh)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setPadding(0, dp(4), 0, dp(8))
    }

    private fun chuPhu(chu: String) = TextView(this).apply {
        text = chu
        setTextColor(mau.chuPhu)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setLineSpacing(0f, 1.25f)
    }

    private fun oNhap(goiY: String, kieu: Int) = EditText(this).apply {
        hint = goiY
        inputType = kieu
        setTextColor(mau.chuChinh)
        setHintTextColor(mau.chuMo)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setPadding(dp(4), dp(12), dp(4), dp(12))
    }

    private fun nut(chu: String, khiBam: () -> Unit) = Button(this).apply {
        text = chu
        isAllCaps = false
        setOnClickListener { khiBam() }
    }

    private fun cot() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

    // ---- Viec ----

    private fun vaoTaiKhoan(taoMoi: Boolean) {
        val email = oEmail.text.toString().trim()
        val matKhau = oMatKhau.text.toString()
        bao(if (taoMoi) "Đang tạo tài khoản…" else "Đang đăng nhập…")

        chayNen({
            if (taoMoi) DongBo.dangKy(kho, email, matKhau) else DongBo.dangNhap(kho, email, matKhau)
            // Vao duoc roi thi dong bo luon mot luot cho co du lieu ngay.
            DongBo.motLuot(this, kho)
        }) {
            oMatKhau.setText("")
            veLaiTrangThai()
        }
    }

    private fun dongBoNgay() {
        bao("Đang đồng bộ…")
        chayNen({ DongBo.motLuot(this, kho) }) { veLaiTrangThai() }
    }

    private fun thoatTaiKhoan() {
        DongBo.dangXuat()
        veLaiTrangThai()
    }

    /**
     * Chay [viec] o luong nen roi quay ve luong chinh.
     *
     * Goi mang o luong chinh la `NetworkOnMainThreadException` — Android chan
     * thang tay, khong co ngoai le.
     */
    private fun chayNen(viec: () -> Unit, xong: () -> Unit) {
        nen.execute {
            val loi = try {
                viec()
                null
            } catch (e: Exception) {
                e.message ?: e.toString()
            }
            chinh.post {
                if (isFinishing || isDestroyed) return@post
                if (loi != null) bao(loi) else xong()
            }
        }
    }

    private fun bao(chu: String) {
        oTrangThai.text = chu
    }

    private fun veLaiTrangThai() {
        val vao = DongBo.daDangNhap
        khungChuaDangNhap.visibility = if (vao) View.GONE else View.VISIBLE
        khungDaDangNhap.visibility = if (vao) View.VISIBLE else View.GONE

        if (vao) oTaiKhoan.text = "Đang đăng nhập: ${DongBo.email}"

        val loi = DongBo.loiCuoi
        bao(
            when {
                loi != null -> loi
                !vao -> "Chưa đăng nhập."
                DongBo.lanCuoi > 0 -> "Đồng bộ lần cuối: ${gioDoc(DongBo.lanCuoi)}"
                else -> "Chưa đồng bộ lần nào."
            },
        )
    }

    private fun gioDoc(moc: Long): String =
        SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(Date(moc))
}
