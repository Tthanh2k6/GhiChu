package com.ghichu

import java.security.SecureRandom

/**
 * Khoa toan cuc cho mot ban ghi.
 *
 * `_id` tu tang cua SQLite chi co y nghia trong may nay — hai may cung sinh
 * `_id = 7` cho hai ghi chu khac nhau la chuyen thuong. Dong bo phai nhan ra
 * "cung mot ghi chu" giua cac may, nen can mot khoa khong dung hang.
 *
 * Dung [SecureRandom] chu khong phai `Random`: `Random` gieo bang gio he thong,
 * hai may khoi dong cung luc (vd khoi phuc tu ban sao luu) co the ra day so y
 * het nhau.
 */
object Uid {

    private val nguon = SecureRandom()
    private const val CHU = "0123456789abcdefghijklmnopqrstuvwxyz"

    /** Dang `<thoi diem base36>-<12 ky tu ngau nhien>`, vd `m3k1z9q4-a7f2c8d1e5b3`. */
    fun moi(): String {
        val moc = java.lang.Long.toString(System.currentTimeMillis(), 36)
        val duoi = StringBuilder(12)
        repeat(12) { duoi.append(CHU[nguon.nextInt(CHU.length)]) }
        return "$moc-$duoi"
    }
}
