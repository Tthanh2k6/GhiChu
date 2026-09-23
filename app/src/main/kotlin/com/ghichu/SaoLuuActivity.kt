package com.ghichu

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Màn **Sao lưu & khôi phục** — vào từ Cài đặt.
 *
 * Gói cả CSDL lẫn ảnh/ghi âm ra một tệp `.zip` (xem [SaoLuu]). Dùng Storage
 * Access Framework để người dùng tự chọn nơi lưu / tệp khôi phục — không xin
 * quyền lưu trữ, không tự rải tệp ra ngoài.
 */
class SaoLuuActivity : Activity() {

    private val nen = Executors.newSingleThreadExecutor()
    private val chinh = Handler(Looper.getMainLooper())
    private val mau by lazy { BangMau.cua(this) }
    private val md by lazy { resources.displayMetrics }
    private fun dp(v: Int) = (v * md.density).toInt()

    private lateinit var oTrangThai: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(mau.themeManHinh)
        title = "Sao lưu & khôi phục"

        val cuon = ScrollView(this).apply {
            setBackgroundColor(mau.nen)
            setOnApplyWindowInsetsListener { v, insets ->
                val t = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(t.left, t.top, t.right, t.bottom)
                insets
            }
        }
        cuon.addView(than())
        setContentView(cuon)
    }

    override fun onDestroy() {
        nen.shutdown()
        super.onDestroy()
    }

    private fun than(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(28))

        addView(tieuDe("Sao lưu & khôi phục"))
        addView(
            chu(
                "Gói toàn bộ ghi chú, lịch trình, ảnh và ghi âm ra một tệp .zip. " +
                    "Khác với Đồng bộ (chỉ đẩy phần chữ lên máy chủ), bản sao lưu này " +
                    "giữ cả ảnh và ghi âm — mang đi máy khác hoặc cất phòng khi mất máy.",
                mau.chuPhu, 13f,
            ),
        )

        addView(nut("Xuất ra tệp…") { xuat() }, dp(16))
        addView(
            chu(
                "Khôi phục sẽ THAY THẾ toàn bộ dữ liệu hiện tại bằng dữ liệu trong tệp, " +
                    "rồi khởi động lại app.",
                mau.chuPhu, 12.5f,
            ),
        )
        addView(nut("Khôi phục từ tệp…") { hoiKhoiPhuc() }, dp(6))

        oTrangThai = chu("", mau.chuMo, 13f).apply { setPadding(0, dp(16), 0, 0) }
        addView(oTrangThai)
    }

    // ---- Xuất ----

    private fun xuat() {
        val ten = "GhiChu-sao-luu-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".zip"
        try {
            startActivityForResult(
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/zip"
                    putExtra(Intent.EXTRA_TITLE, ten)
                },
                MA_XUAT,
            )
        } catch (_: Exception) {
            bao("Máy không có trình chọn tệp.")
        }
    }

    // ---- Khôi phục ----

    private fun hoiKhoiPhuc() {
        HopThoai(this, mau)
            .setTitle("Khôi phục dữ liệu")
            .setMessage(
                "Toàn bộ ghi chú, lịch, ảnh và ghi âm HIỆN TẠI sẽ bị thay bằng dữ liệu " +
                    "trong tệp sao lưu. Không hoàn tác được. Tiếp tục?",
            )
            .nguyHiem()
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Chọn tệp") { _, _ -> chonTepKhoiPhuc() }
            .show()
    }

    private fun chonTepKhoiPhuc() {
        try {
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    // Một số trình chọn không lọc đúng application/zip, để rộng cho chắc.
                    type = "*/*"
                },
                MA_NHAP,
            )
        } catch (_: Exception) {
            bao("Máy không có trình chọn tệp.")
        }
    }

    override fun onActivityResult(ma: Int, ketQua: Int, du: Intent?) {
        super.onActivityResult(ma, ketQua, du)
        if (ketQua != RESULT_OK) return
        val uri = du?.data ?: return
        when (ma) {
            MA_XUAT -> chayXuat(uri)
            MA_NHAP -> chayNhap(uri)
        }
    }

    private fun chayXuat(uri: Uri) {
        bao("Đang xuất…")
        nen.execute {
            val loi = try {
                (contentResolver.openOutputStream(uri)
                    ?: throw java.io.IOException("Không mở được tệp để ghi."))
                    .use { SaoLuu.xuatVao(this, it) }
                null
            } catch (e: Exception) {
                e.message ?: e.toString()
            }
            chinh.post {
                if (isFinishing || isDestroyed) return@post
                bao(if (loi == null) "Đã xuất xong. Tệp .zip đã được lưu." else "Xuất lỗi: $loi")
            }
        }
    }

    private fun chayNhap(uri: Uri) {
        bao("Đang khôi phục…")
        nen.execute {
            val loi = try {
                (contentResolver.openInputStream(uri)
                    ?: throw java.io.IOException("Không mở được tệp."))
                    .use { SaoLuu.nhapTu(this, it) }
                null
            } catch (e: Exception) {
                e.message ?: e.toString()
            }
            chinh.post {
                if (isFinishing || isDestroyed) return@post
                if (loi == null) khoiDongLai() else bao("Khôi phục lỗi: $loi")
            }
        }
    }

    /** Khởi động lại app: CSDL đã đóng và bị thay tệp, phải mở tiến trình mới. */
    private fun khoiDongLai() {
        val i = packageManager.getLaunchIntentForPackage(packageName)
        if (i == null) {
            bao("Đã khôi phục. Hãy tự mở lại app.")
            return
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(i)
        Runtime.getRuntime().exit(0)
    }

    // ---- Giao diện vụn ----

    private fun bao(s: String) {
        if (::oTrangThai.isInitialized) oTrangThai.text = s
    }

    private fun tieuDe(s: String) = TextView(this).apply {
        text = s
        setTextColor(mau.chuChinh)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
        setPadding(0, dp(4), 0, dp(12))
    }

    private fun chu(s: String, m: Int, co: Float) = TextView(this).apply {
        text = s
        setTextColor(m)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, co)
        setLineSpacing(0f, 1.25f)
    }

    private fun nut(s: String, khiBam: () -> Unit) = Button(this).apply {
        text = s
        isAllCaps = false
        setOnClickListener { khiBam() }
    }

    private fun LinearLayout.addView(v: View, leTren: Int) {
        addView(v, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = leTren })
    }

    private companion object {
        const val MA_XUAT = 11
        const val MA_NHAP = 12
    }
}
