package com.ghichu

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import java.util.concurrent.Executors

/**
 * Tự đẩy đồng bộ khi app **xuống nền**.
 *
 * Màn chính đã tự đồng bộ mỗi lần MỞ app rồi (xem
 * `ManHinhChinhActivity.dongBoNgam` trong `onResume`, tiết lưu 2 phút). Cái còn
 * thiếu là chiều ngược lại: sửa ghi chú / thêm sự kiện xong rồi THOÁT app — nếu
 * không mở lại sớm thì thay đổi nằm ì trong máy, máy khác không thấy. Ở đây bắt
 * đúng lúc activity cuối cùng dừng (app rời màn hình) để đẩy nốt lên máy chủ.
 *
 * Vì sao đếm activity: chuyển giữa các màn TRONG app thì `onStart` của màn mới
 * chạy trước `onStop` của màn cũ, nên số đang-hiện không về 0 — chỉ về 0 khi
 * thật sự rời app. Nhờ vậy không đồng bộ oan mỗi lần mở hộp thoại hay đổi màn.
 *
 * Chạy nền, im lặng nếu lỗi: [DongBo.motLuot] tự nuốt mọi ngoại lệ vào `loiCuoi`,
 * còn cờ `dangChay` chặn hai luợt chồng nhau. App bị đóng băng ngay sau đó thì
 * cùng lắm là lượt này không kịp xong — lần mở kế tiếp `onResume` đẩy bù.
 */
object DongBoTuDong : Application.ActivityLifecycleCallbacks {

    private val nen = Executors.newSingleThreadExecutor()
    private var dangHien = 0

    fun gan(app: Application) = app.registerActivityLifecycleCallbacks(this)

    override fun onActivityStarted(activity: Activity) {
        dangHien++
    }

    override fun onActivityStopped(activity: Activity) {
        dangHien = (dangHien - 1).coerceAtLeast(0)
        if (dangHien == 0) dayNgam(activity.applicationContext)
    }

    /** App vừa rời màn hình — đẩy thay đổi cục bộ lên (chạy nền). */
    private fun dayNgam(context: Context) {
        if (!DongBo.daDangNhap || DongBo.dangChay) return
        val ung = Ung.cua(context)
        nen.execute { DongBo.motLuot(context, ung.kho) }
    }

    // Không cần các mốc còn lại.
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
