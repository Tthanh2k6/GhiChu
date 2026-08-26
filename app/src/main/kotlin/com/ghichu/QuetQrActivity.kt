package com.ghichu

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Size
import android.util.TypedValue
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

/**
 * May quet ma QR.
 *
 * HAI QUY UOC AN TOAN, dung tu y bo:
 *
 * 1. **KHONG tu mo bat cu thu gi.** Quet xong thi dung lai, hien noi dung va
 *    **ten mien** to ro, nguoi dung nhin roi moi bam "Mo". Ma QR la o vuong den
 *    tren tuong — khong ai doc duoc no bang mat, nen tu mo la giao quyen dieu
 *    khien cho ke dan cai ma do.
 * 2. **Chi mo `http`/`https`.** Xem [QuetQr.laLienKet]. Cac luoc do khac de
 *    nguyen dang chu, muon dung thi tu sao chep.
 *
 * Giao dien dung bang code, khong dung R.layout — ma tai nguyen sinh o module
 * nay khong khop bang tai nguyen cua app dich.
 */
class QuetQrActivity : Activity() {

    private lateinit var kho: KhoDuLieu
    private lateinit var khungXem: TextureView
    private lateinit var bangKetQua: LinearLayout
    private lateinit var chuHuongDan: TextView
    private lateinit var thanhTren: LinearLayout

    private val mau by lazy { BangMau.cua(this) }
    private val md by lazy { resources.displayMetrics }
    private fun dp(v: Int) = (v * md.density).toInt()

    private var may: CameraDevice? = null
    private var phien: CameraCaptureSession? = null
    private var docAnh: ImageReader? = null
    private var luongNen: HandlerThread? = null
    private var tayNen: Handler? = null
    private val chinh = Handler(Looper.getMainLooper())

    /** Dang dung de nguoi dung xem ket qua — khung hinh moi bo qua het. */
    @Volatile
    private var tamDung = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(BangMau.cua(this).themeManHinh)
        title = "Quét QR"
        kho = Ung.cua(this).kho
        setContentView(dungGiaoDien())

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), XIN_MAY_ANH)
        }
    }

    override fun onRequestPermissionsResult(ma: Int, quyen: Array<out String>, ketQua: IntArray) {
        super.onRequestPermissionsResult(ma, quyen, ketQua)
        if (ma != XIN_MAY_ANH) return
        if (ketQua.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            moMayNeuSan()
        } else {
            chuHuongDan.text = "Cần quyền máy ảnh để quét. Vào Cài đặt → Ứng dụng để bật."
        }
    }

    override fun onResume() {
        super.onResume()
        luongNen = HandlerThread("quet_qr").apply { start() }
        tayNen = Handler(luongNen!!.looper)
        moMayNeuSan()
    }

    override fun onPause() {
        dongMay()
        luongNen?.quitSafely()
        luongNen = null
        tayNen = null
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // ---------- Giao dien ----------

    private fun dungGiaoDien(): View {
        val goc = FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()) }

        khungXem = TextureView(this).apply {
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) = moMayNeuSan()
                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = Unit
                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean = true
                override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
            }
        }
        goc.addView(khungXem, FrameLayout.LayoutParams(MATCH, MATCH))

        // Khung ngam giua man: vien trang bo goc, khong nen — de nguoi dung biet
        // dua ma vao dau. Khong co no thi ca man deu la vung quet, nhin rat vo dinh.
        goc.addView(
            View(this).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setStroke(dp(3), 0xCCFFFFFF.toInt())
                    setColor(0x00000000)
                }
            },
            FrameLayout.LayoutParams(dp(240), dp(240)).apply { gravity = Gravity.CENTER },
        )

        chuHuongDan = TextView(this).apply {
            text = "Đưa mã QR vào khung hình"
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(9), dp(16), dp(9))
            // Chu trang tren nen anh that thi luc doc duoc luc khong — phai co
            // nen mo phia sau.
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(0x99000000.toInt())
            }
        }
        thanhTren = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            addView(chuHuongDan, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
        goc.addView(
            thanhTren,
            FrameLayout.LayoutParams(MATCH, WRAP).apply { gravity = Gravity.TOP },
        )

        bangKetQua = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(20))
            background = GradientDrawable().apply {
                cornerRadii = floatArrayOf(
                    dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(),
                    0f, 0f, 0f, 0f,
                )
                setColor(mau.nen)
            }
            visibility = View.GONE
        }
        goc.addView(
            bangKetQua,
            FrameLayout.LayoutParams(MATCH, WRAP).apply { gravity = Gravity.BOTTOM },
        )

        thanhTren.addView(
            TextView(this).apply {
                text = "Lịch sử"
                setTextColor(0xFFFFFFFF.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(dp(16), dp(9), dp(16), dp(9))
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(0x99000000.toInt())
                }
                isClickable = true
                setOnClickListener { hienLichSu() }
            },
            LinearLayout.LayoutParams(WRAP, WRAP).apply { leftMargin = dp(10) },
        )

        // Chua le he thong: khong lam viec nay thi chu chui len sau dong ho va
        // cot song wifi, con bang ket qua bi thanh dieu huong che mat nut.
        goc.setOnApplyWindowInsetsListener { _, insets ->
            val le = insets.getInsets(WindowInsets.Type.systemBars())
            thanhTren.setPadding(dp(14) + le.left, dp(10) + le.top, dp(14) + le.right, dp(10))
            bangKetQua.setPadding(dp(18) + le.left, dp(16), dp(18) + le.right, dp(20) + le.bottom)
            insets
        }
        return goc
    }

    // ---------- May anh ----------

    private fun moMayNeuSan() {
        if (may != null || !khungXem.isAvailable) return
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return

        val qlMay = getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        val id = maySau(qlMay) ?: run {
            chuHuongDan.text = "Máy này không có máy ảnh sau."
            return
        }

        val co = chonCo(qlMay, id)
        docAnh = ImageReader.newInstance(co.width, co.height, android.graphics.ImageFormat.YUV_420_888, 2).apply {
            setOnImageAvailableListener({ r -> docKhung(r) }, tayNen)
        }
        khungXem.surfaceTexture?.setDefaultBufferSize(co.width, co.height)

        try {
            qlMay.openCamera(
                id,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(m: CameraDevice) {
                        may = m
                        moPhien(m)
                    }

                    override fun onDisconnected(m: CameraDevice) {
                        m.close()
                        may = null
                    }

                    override fun onError(m: CameraDevice, loi: Int) {
                        m.close()
                        may = null
                        chinh.post { chuHuongDan.text = "Không mở được máy ảnh (lỗi $loi)." }
                    }
                },
                tayNen,
            )
        } catch (e: SecurityException) {
            chuHuongDan.text = "Chưa có quyền máy ảnh."
        } catch (e: Exception) {
            chuHuongDan.text = "Không mở được máy ảnh: ${e.message}"
        }
    }

    private fun moPhien(m: CameraDevice) {
        val mat = Surface(khungXem.surfaceTexture ?: return)
        val matDoc = docAnh?.surface ?: return

        val yeuCau = m.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(mat)
            addTarget(matDoc)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        }

        val goi = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(p: CameraCaptureSession) {
                phien = p
                try {
                    p.setRepeatingRequest(yeuCau.build(), null, tayNen)
                } catch (_: Exception) {
                }
            }

            override fun onConfigureFailed(p: CameraCaptureSession) {
                chinh.post { chuHuongDan.text = "Không dựng được luồng xem." }
            }
        }

        try {
            m.createCaptureSession(
                SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    listOf(OutputConfiguration(mat), OutputConfiguration(matDoc)),
                    Executors.newSingleThreadExecutor(),
                    goi,
                ),
            )
        } catch (_: Exception) {
        }
    }

    private fun dongMay() {
        try {
            phien?.close()
        } catch (_: Exception) {
        }
        phien = null
        may?.close()
        may = null
        docAnh?.close()
        docAnh = null
    }

    private fun maySau(ql: CameraManager): String? {
        for (id in ql.cameraIdList) {
            val huong = ql.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
            if (huong == CameraCharacteristics.LENS_FACING_BACK) return id
        }
        return ql.cameraIdList.firstOrNull()
    }

    /** Co gan 1280x720 nhat: du net de bat ma, du nho de moi khung giai kip. */
    private fun chonCo(ql: CameraManager, id: String): Size {
        val bang = ql.getCameraCharacteristics(id)
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?.getOutputSizes(android.graphics.ImageFormat.YUV_420_888)
            ?.toList()
            .orEmpty()
        if (bang.isEmpty()) return Size(1280, 720)
        return bang.minByOrNull { kotlin.math.abs(it.width * it.height - 1280 * 720) } ?: bang[0]
    }

    // ---------- Doc khung hinh ----------

    private fun docKhung(r: ImageReader) {
        val anh = try {
            r.acquireLatestImage()
        } catch (_: Exception) {
            null
        } ?: return

        try {
            if (tamDung) return

            val phang = anh.planes[0]
            val dem = phang.buffer
            val rong = anh.width
            val cao = anh.height
            val buoc = phang.rowStride

            // rowStride co the LON HON chieu rong (co dem cuoi dong). Cu do ca
            // dem vao la anh bi xeo, giai ma khong bao gio ra.
            val sang = ByteArray(rong * cao)
            if (buoc == rong) {
                dem.get(sang, 0, minOf(dem.remaining(), sang.size))
            } else {
                val dong = ByteArray(buoc)
                var v = 0
                for (y in 0 until cao) {
                    if (dem.remaining() < buoc) break
                    dem.get(dong, 0, buoc)
                    System.arraycopy(dong, 0, sang, v, rong)
                    v += rong
                }
            }

            val ket = QuetQr.giaiMa(sang, rong, cao) ?: return
            tamDung = true
            chinh.post { batDuoc(ket) }
        } catch (_: Exception) {
            // Khung hong thi bo, khung sau lai toi.
        } finally {
            anh.close()
        }
    }

    // ---------- Bat duoc ma ----------

    private fun batDuoc(noiDung: String) {
        if (isFinishing || isDestroyed) return
        kho.themQuet(noiDung)
        hienKetQua(noiDung)
    }

    private fun hienKetQua(noiDung: String) {
        val loai = QuetQr.loaiCua(noiDung)
        val laLink = loai == QuetQr.Loai.LIEN_KET

        bangKetQua.removeAllViews()
        bangKetQua.visibility = View.VISIBLE
        chuHuongDan.text = "Đã quét được"

        // Ten mien to ro o tren cung — day la thu duy nhat dang tin trong mot
        // dia chi, phan duoi co the dai loe loet de day no ra khoi tam mat.
        if (laLink) {
            bangKetQua.addView(
                TextView(this).apply {
                    text = QuetQr.tenMien(noiDung)
                    setTextColor(mau.nhan)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                },
            )
        }

        bangKetQua.addView(
            ScrollView(this).apply {
                addView(
                    TextView(this@QuetQrActivity).apply {
                        text = noiDung
                        setTextColor(mau.chuChinh)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                        setTextIsSelectable(true)
                        setPadding(0, dp(6), 0, dp(10))
                    },
                )
            },
            LinearLayout.LayoutParams(MATCH, dp(140)),
        )

        if (!laLink && noiDung.trimStart().lowercase().let {
                it.startsWith("intent:") || it.startsWith("file:") ||
                    it.startsWith("content:") || it.startsWith("javascript:")
            }
        ) {
            bangKetQua.addView(canhBao("Mã này yêu cầu mở một thứ trong máy, không phải trang web. App không mở loại này — muốn dùng thì tự sao chép."))
        }

        val hang = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }
        if (laLink) hang.addView(nut("Mở") { moLienKet(noiDung) })
        hang.addView(nut("Sao chép") { saoChep(noiDung) })
        hang.addView(nut("Quét tiếp") { quetTiep() })
        bangKetQua.addView(hang)
    }

    private fun canhBao(chu: String) = TextView(this).apply {
        text = chu
        setTextColor(mau.chuPhu)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setPadding(dp(10), dp(8), dp(10), dp(8))
        background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(mau.bongMay)
        }
    }

    private fun nut(chu: String, khiBam: () -> Unit) = Button(this).apply {
        text = chu
        isAllCaps = false
        setOnClickListener { khiBam() }
        layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f).apply {
            leftMargin = dp(4)
            rightMargin = dp(4)
        }
    }

    private fun quetTiep() {
        bangKetQua.visibility = View.GONE
        chuHuongDan.text = "Đưa mã QR vào khung hình"
        tamDung = false
    }

    private fun moLienKet(dia: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(dia)))
        } catch (_: Exception) {
            bao("Không mở được địa chỉ này.")
        }
    }

    private fun saoChep(chu: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
            ?.setPrimaryClip(ClipData.newPlainText("QR", chu))
        bao("Đã sao chép.")
    }

    private fun bao(chu: String) {
        android.widget.Toast.makeText(this, chu, android.widget.Toast.LENGTH_SHORT).show()
    }

    // ---------- Lich su ----------

    private fun hienLichSu() {
        val ds = kho.lichSuQuet()
        if (ds.isEmpty()) {
            bao("Chưa quét mã nào.")
            return
        }
        val nhan = ds.map {
            val ngay = NgayVN.ngayCua(it.luc)
            "${NgayVN.gioPhut(it.luc)} · ${NgayVN.thangNgan(ngay)}\n${QuetQr.rutGon(it.noiDung)}"
        }.toTypedArray()

        HopThoai(this, mau)
            .setTitle("Lịch sử quét")
            .setItems(nhan) { _, i ->
                tamDung = true
                hienKetQua(ds[i].noiDung)
            }
            .setNeutralButton("Xoá hết") { _, _ ->
                HopThoai(this, mau)
                    .setTitle("Xoá hết lịch sử quét?")
                    .setMessage("Không lấy lại được.")
                    .setPositiveButton("Xoá") { _, _ ->
                        kho.xoaHetQuet()
                        bao("Đã xoá lịch sử.")
                    }
                    .setNegativeButton("Huỷ", null)
                    .show()
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val XIN_MAY_ANH = 71
    }
}
