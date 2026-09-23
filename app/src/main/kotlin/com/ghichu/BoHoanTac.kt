package com.ghichu

import android.os.SystemClock
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView

/**
 * Hoàn tác / làm lại cho một [EditText] soạn thảo — GIỮ nguyên định dạng.
 *
 * `EditText` của khung có sẵn undo nội bộ (Ctrl+Z) nhưng KHÔNG mở API để gắn nút
 * ↶ ↷ hay gọi bằng tay, nên phải tự làm. Cách đơn giản mà đúng cho ô có span
 * (chữ đậm, màu, ô tick, ảnh, ghi âm, bản đồ): chụp NGUYÊN chuỗi kèm span vào
 * `SpannableStringBuilder` mỗi mốc, khôi phục là đặt cả chuỗi về. Chép chuỗi giữ
 * lại tham chiếu span nên ảnh/định dạng không mất — khác hẳn cách chỉ nhớ chữ
 * trơn (undo xong là bay hết định dạng).
 *
 * Gộp phím gõ nhanh: một tràng gõ liên tục (cách nhau dưới [GOP_MS]) tính là MỘT
 * bước hoàn tác — bấm ↶ một lần lùi cả cụm, không phải lùi từng ký tự.
 */
class BoHoanTac(private val o: EditText, private val toiDa: Int = 100) {

    private val lui = ArrayDeque<CharSequence>()
    private val toiLaiStack = ArrayDeque<CharSequence>()

    /** Đang tự khôi phục — bỏ qua để không ghi chính thao tác khôi phục thành mốc. */
    private var dangKhoiPhuc = false

    /** Trạng thái NGAY TRƯỚC cụm sửa đang diễn ra (chụp ở beforeTextChanged). */
    private var truoc: CharSequence? = null
    private var mocCuoi = 0L

    /** Báo cho màn hình cập nhật trạng thái bật/tắt hai nút. */
    var khiDoi: (() -> Unit)? = null

    val coLui: Boolean get() = lui.isNotEmpty()
    val coToiLai: Boolean get() = toiLaiStack.isNotEmpty()

    private val theo = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            if (dangKhoiPhuc || s == null) return
            if (SystemClock.uptimeMillis() - mocCuoi > GOP_MS) {
                // Bắt đầu một cụm sửa mới: nhớ trạng thái trước khi cụm này xảy ra.
                truoc = SpannableStringBuilder(s)
            }
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable?) {
            if (dangKhoiPhuc) return
            val gio = SystemClock.uptimeMillis()
            if (gio - mocCuoi > GOP_MS) {
                truoc?.let { day(lui, it) }
                toiLaiStack.clear()
                khiDoi?.invoke()
            }
            mocCuoi = gio
        }
    }

    init {
        o.addTextChangedListener(theo)
    }

    fun lui() {
        if (lui.isEmpty()) return
        day(toiLaiStack, SpannableStringBuilder(o.text))
        apDung(lui.removeLast())
        // Cụm kế tiếp là cụm mới, không dính vào cụm vừa khôi phục.
        mocCuoi = 0L
        khiDoi?.invoke()
    }

    fun toiLai() {
        if (toiLaiStack.isEmpty()) return
        day(lui, SpannableStringBuilder(o.text))
        apDung(toiLaiStack.removeLast())
        mocCuoi = 0L
        khiDoi?.invoke()
    }

    private fun apDung(cs: CharSequence) {
        dangKhoiPhuc = true
        o.setText(cs, TextView.BufferType.SPANNABLE)
        o.setSelection(o.text.length)
        dangKhoiPhuc = false
    }

    private fun day(ngan: ArrayDeque<CharSequence>, cs: CharSequence) {
        ngan.addLast(cs)
        while (ngan.size > toiDa) ngan.removeFirst()
    }

    private companion object {
        const val GOP_MS = 500L
    }
}
