package com.ghichu

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

/**
 * Toan bo du lieu cua app: ghi chu, thu muc, su kien lich trinh, lich su quet QR.
 *
 * Mot CSDL duy nhat chu khong moi thu mot tep: ba nhom du lieu nay deu di qua
 * cung mot duong dong bo, gop lai thi mot giao dich la xong, tach ra thi phai tu
 * bat tay giua hai tep.
 *
 * QUAN TRONG — `noi_dung` cua ghi chu luu bang **markup cua Xiaomi**
 * (`<text indent="1">...`, `<input type="checkbox" checked="true" />`), khong
 * phai chu tron va khong phai Markdown. CO Y giu nguyen dinh dang do du app nay
 * khong con dinh gi den Xiaomi:
 *   * ghi chu cu tren may chu doc duoc ngay, khong can chuyen doi;
 *   * [NoiDungGhiChu] da phan tich/sinh dinh dang nay va da co kiem thu;
 *   * phan AI sinh ra markup nay san.
 * Doi dinh dang la ca ba thu tren phai lam lai.
 */
class KhoDuLieu(context: Context) : SQLiteOpenHelper(
    context.applicationContext, TEN_DB, null, PHIEN_BAN,
) {

    /**
     * Giu lai de bao widget ve lai moi khi su kien doi.
     *
     * CO Y bao tu day chu khong tu noi goi: co ba duong ghi su kien (hop sua tay,
     * tro ly AI, keo ve tu may chu) — de moi noi tu nho goi la truoc sau gi cung
     * quen mot cho, roi widget hien du lieu cu ma khong ai hieu vi sao.
     */
    private val ung: Context = context.applicationContext

    /**
     * Bao ra ngoai rang du lieu vua doi.
     *
     * CO Y bao tu day chu khong tu noi goi: co nhieu duong ghi (hop sua tay, tro
     * ly AI, keo ve tu may chu, sua ghi chu) — de moi noi tu nho goi la truoc sau
     * gi cung quen mot cho, roi widget hien du lieu cu hoac nhac nho im lang ma
     * khong ai hieu vi sao.
     *
     * DUNG goi trong mot giao dich dang mo: [NhacNho.datLai] doc lai CSDL.
     */
    fun baoDoi() {
        LichTrinhWidget.capNhat(ung)
        GhiChuWidget.capNhat(ung)
        GhiChuMotWidget.capNhat(ung)
        NhacNho.datLai(ung)
    }

    override fun onCreate(db: SQLiteDatabase) {
        taoBangSuKien(db)
        taoBangGhiChu(db)
        taoBangThuMuc(db)
        taoBangQr(db)
        taoBangHoiThoai(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, cu: Int, moi: Int) {
        // VIET THEM vao day khi doi schema; dung bao gio xoa bang — du lieu la
        // cua nguoi dung.
        if (cu < 2) taoBangHoiThoai(db)
    }

    /**
     * Cac cuoc tro chuyen voi tro ly AI.
     *
     * `luot` la ca doan chat duoi dang JSON trong MOT o. Co y khong tach moi luot
     * mot hang: khong bao gio can truy van vao giua mot doan chat — luc nao cung
     * doc/ghi tron ca doan — nen tach ra chi them mot phep noi bang ma khong duoc
     * gi.
     *
     * **Khong dong bo len may chu.** Doan chat hay co thu nguoi dung khong nghi
     * la minh dang luu tru; giu trong may la lua chon an toan hon.
     */
    private fun taoBangHoiThoai(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $BANG_HOI_THOAI (
                _id      INTEGER PRIMARY KEY AUTOINCREMENT,
                tieu_de  TEXT    NOT NULL DEFAULT '',
                luot     TEXT    NOT NULL DEFAULT '',
                ngay_sua INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_${BANG_HOI_THOAI}_sua ON $BANG_HOI_THOAI (ngay_sua)")
    }

    // ---- Tro chuyen voi AI ----

    /** Mot cuoc tro chuyen trong danh sach lich su. */
    class TomTatChat(val id: Long, val tieuDe: String, val ngaySua: Long)

    /**
     * Ghi mot cuoc tro chuyen. `id = 0` la tao moi; tra ve id sau khi luu.
     *
     * Doan rong thi KHONG tao hang: mo tro ly len roi thoat ngay la chuyen
     * thuong, khong nen de lai mot muc trong trong lich su.
     */
    fun luuHoiThoai(id: Long, tieuDe: String, luot: String): Long {
        if (id == 0L && luot.isBlank()) return 0L
        val v = ContentValues().apply {
            put("tieu_de", tieuDe.take(80))
            put("luot", luot)
            put("ngay_sua", System.currentTimeMillis())
        }
        if (id != 0L) {
            writableDatabase.update(BANG_HOI_THOAI, v, "_id = ?", arrayOf(id.toString()))
            return id
        }
        return writableDatabase.insert(BANG_HOI_THOAI, null, v)
    }

    fun danhSachHoiThoai(gioiHan: Int = 50): List<TomTatChat> {
        val ra = ArrayList<TomTatChat>()
        readableDatabase.query(
            BANG_HOI_THOAI, arrayOf("_id", "tieu_de", "ngay_sua"),
            null, null, null, null, "ngay_sua DESC", gioiHan.toString(),
        ).use { c ->
            while (c.moveToNext()) {
                ra += TomTatChat(c.getLong(0), c.getString(1).orEmpty(), c.getLong(2))
            }
        }
        return ra
    }

    fun docHoiThoai(id: Long): String = readableDatabase.query(
        BANG_HOI_THOAI, arrayOf("luot"), "_id = ?", arrayOf(id.toString()), null, null, null,
    ).use { if (it.moveToFirst()) it.getString(0).orEmpty() else "" }

    fun xoaHoiThoai(id: Long) {
        writableDatabase.delete(BANG_HOI_THOAI, "_id = ?", arrayOf(id.toString()))
    }

    private fun taoBangSuKien(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $BANG (
                _id         INTEGER PRIMARY KEY AUTOINCREMENT,
                tieu_de     TEXT    NOT NULL DEFAULT '',
                ghi_chu     TEXT    NOT NULL DEFAULT '',
                bat_dau     INTEGER NOT NULL,
                ket_thuc    INTEGER NOT NULL,
                ca_ngay     INTEGER NOT NULL DEFAULT 0,
                mau         INTEGER NOT NULL DEFAULT 0,
                lap_lai     INTEGER NOT NULL DEFAULT 0,
                nhac_truoc  INTEGER NOT NULL DEFAULT -1,
                dia_diem    TEXT    NOT NULL DEFAULT '',
                ngay_tao    INTEGER NOT NULL DEFAULT 0,
                ngay_sua    INTEGER NOT NULL DEFAULT 0,
                da_xoa      INTEGER NOT NULL DEFAULT 0,
                uid         TEXT    NOT NULL DEFAULT '',
                ban_nhap    INTEGER NOT NULL DEFAULT 1,
                lap_den     INTEGER
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_${BANG}_bat_dau ON $BANG (bat_dau)")
        db.execSQL("CREATE INDEX idx_${BANG}_lap_lai ON $BANG (lap_lai)")
        db.execSQL("CREATE UNIQUE INDEX idx_${BANG}_uid ON $BANG (uid)")
    }

    /**
     * Lich su quet QR.
     *
     * CO Y khong dong bo len may chu: ma quet duoc thuong la ma wifi, ve, ma
     * thanh toan — thu nguoi ta chia ve khong co y de len mang. De nguyen trong
     * may, muon bo thi xoa mot nut.
     */
    private fun taoBangQr(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $BANG_QR (
                _id      INTEGER PRIMARY KEY AUTOINCREMENT,
                noi_dung TEXT    NOT NULL,
                luc      INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_${BANG_QR}_luc ON $BANG_QR (luc)")
    }

    // ---- Lich su quet QR ----

    /** Mot lan quet. */
    class LanQuet(val id: Long, val noiDung: String, val luc: Long)

    /**
     * Ghi lai mot lan quet.
     *
     * Quet trung ma vua quet thi CHI cap nhat gio, khong them hang moi — chia
     * may anh vao mot ma la no bat lien tuc vai chuc lan, khong gop thi lich su
     * ngap toan mot dong.
     */
    fun themQuet(noiDung: String, luc: Long = System.currentTimeMillis()) {
        val chu = noiDung.trim()
        if (chu.isEmpty()) return
        val db = writableDatabase
        val da = db.query(
            BANG_QR, arrayOf("_id"), "noi_dung = ?", arrayOf(chu), null, null, null, "1",
        ).use { if (it.moveToFirst()) it.getLong(0) else null }

        if (da != null) {
            db.update(BANG_QR, ContentValues().apply { put("luc", luc) },
                "_id = ?", arrayOf(da.toString()))
        } else {
            db.insert(BANG_QR, null, ContentValues().apply {
                put("noi_dung", chu)
                put("luc", luc)
            })
        }
    }

    /** Lich su, moi nhat truoc. */
    fun lichSuQuet(gioiHan: Int = 200): List<LanQuet> {
        val ra = ArrayList<LanQuet>()
        readableDatabase.query(
            BANG_QR, arrayOf("_id", "noi_dung", "luc"),
            null, null, null, null, "luc DESC", gioiHan.toString(),
        ).use { c ->
            while (c.moveToNext()) ra += LanQuet(c.getLong(0), c.getString(1).orEmpty(), c.getLong(2))
        }
        return ra
    }

    fun xoaQuet(id: Long) {
        writableDatabase.delete(BANG_QR, "_id = ?", arrayOf(id.toString()))
    }

    fun xoaHetQuet() {
        writableDatabase.delete(BANG_QR, null, null)
    }

    /**
     * Ghi chu.
     *
     *   uid       khoa toan cuc, dung chung voi may chu. `_id` chi co nghia
     *             trong may nay nen khong bao gio gui di.
     *   chu_tron  ban da lot het the cua [noi_dung], chi de TIM KIEM. Co y luu
     *             san chu khong lot luc tim: lot mot lan luc ghi thi re, lot moi
     *             lan go mot chu tren o tim kiem thi lag thay ro tu vai tram
     *             ghi chu tro len.
     *   thu_muc   TEN thu muc chu khong phai id — may chu luu ten, giu cung mot
     *             thu thi khong phai anh xa qua lai.
     *   da_xoa    dang o thung rac. Con hang, xoa han la khi don thung rac.
     *   ban_nhap  chua day len may chu; dat 1 o MOI duong ghi.
     */
    private fun taoBangGhiChu(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $BANG_GHI_CHU (
                _id      INTEGER PRIMARY KEY AUTOINCREMENT,
                uid      TEXT    NOT NULL,
                tieu_de  TEXT    NOT NULL DEFAULT '',
                noi_dung TEXT    NOT NULL DEFAULT '',
                chu_tron TEXT    NOT NULL DEFAULT '',
                thu_muc  TEXT    NOT NULL DEFAULT '',
                ghim     INTEGER NOT NULL DEFAULT 0,
                mau      INTEGER NOT NULL DEFAULT 0,
                rieng_tu INTEGER NOT NULL DEFAULT 0,
                nhac_luc INTEGER,
                ngay_tao INTEGER NOT NULL DEFAULT 0,
                ngay_sua INTEGER NOT NULL DEFAULT 0,
                da_xoa   INTEGER NOT NULL DEFAULT 0,
                xoa_luc  INTEGER,
                ban_nhap INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX idx_${BANG_GHI_CHU}_uid ON $BANG_GHI_CHU (uid)")
        db.execSQL("CREATE INDEX idx_${BANG_GHI_CHU}_sua ON $BANG_GHI_CHU (ngay_sua)")
        db.execSQL("CREATE INDEX idx_${BANG_GHI_CHU}_thu_muc ON $BANG_GHI_CHU (thu_muc)")
    }

    /**
     * Thu muc.
     *
     * May chu KHONG luu danh sach thu muc — ghi chu chi mang theo TEN thu muc cua
     * no. Bang nay vi vay chi de nho thu tu hien thi va giu lai thu muc rong (thu
     * muc vua tao, chua co ghi chu nao). Keo ve tu may chu gap ten la thi tu tao
     * hang o day.
     */
    private fun taoBangThuMuc(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $BANG_THU_MUC (
                _id      INTEGER PRIMARY KEY AUTOINCREMENT,
                ten      TEXT    NOT NULL,
                thu_tu   INTEGER NOT NULL DEFAULT 0,
                ngay_tao INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX idx_${BANG_THU_MUC}_ten ON $BANG_THU_MUC (ten)")
    }

    /**
     * Lay cac su kien co the roi vao khoang `[tu, den)`.
     *
     * Su kien lap lai co the bat dau tu rat lau truoc `tu`, nen khong loc theo
     * `bat_dau` duoc — lay het roi de [LichLapLai] trai ra.
     */
    fun trongKhoang(tu: Long, den: Long): List<SuKien> {
        val cur = readableDatabase.query(
            BANG, null,
            "da_xoa = 0 AND (lap_lai != 0 OR (bat_dau < ? AND ket_thuc > ?))",
            arrayOf(den.toString(), tu.toString()),
            null, null, "bat_dau ASC",
        )
        return cur.use { c ->
            ArrayList<SuKien>(c.count).apply {
                while (c.moveToNext()) add(doc(c))
            }
        }
    }

    fun theoId(id: Long): SuKien? {
        val cur = readableDatabase.query(BANG, null, "_id = ?", arrayOf(id.toString()), null, null, null)
        return cur.use { if (it.moveToFirst()) doc(it) else null }
    }

    /** Them moi neu `id == 0`, nguoc lai cap nhat. Tra ve id sau khi luu. */
    fun luu(sk: SuKien): Long {
        val gio = System.currentTimeMillis()
        val v = ContentValues().apply {
            put("tieu_de", sk.tieuDe)
            put("ghi_chu", sk.ghiChu)
            put("bat_dau", sk.batDau)
            put("ket_thuc", sk.ketThuc)
            put("ca_ngay", if (sk.caNgay) 1 else 0)
            put("mau", sk.mau)
            put("lap_lai", sk.lapLai)
            put("nhac_truoc", sk.nhacTruoc)
            put("dia_diem", sk.diaDiem)
            if (sk.lapDen == null) putNull("lap_den") else put("lap_den", sk.lapDen)
            put("ngay_sua", gio)
            // Moi duong ghi vao day deu phai danh dau chua day len, khong thi
            // co sua ma may khac khong bao gio thay.
            put("ban_nhap", 1)
        }
        val id = if (sk.id == 0L) {
            v.put("ngay_tao", gio)
            v.put("uid", Uid.moi())
            writableDatabase.insert(BANG, null, v)
        } else {
            writableDatabase.update(BANG, v, "_id = ?", arrayOf(sk.id.toString()))
            sk.id
        }
        baoDoi()
        return id
    }

    /**
     * Xoa mem — danh dau `da_xoa` chu khong xoa han.
     *
     * Giu lai lam "bia mo" de dong bo con bao may khac xoa theo; xoa han thi may
     * kia khong bao gio biet, roi lan sau lai day nguoc no ve.
     */
    fun xoa(id: Long) {
        val v = ContentValues().apply {
            put("da_xoa", 1)
            put("ban_nhap", 1)
            put("ngay_sua", System.currentTimeMillis())
        }
        writableDatabase.update(BANG, v, "_id = ?", arrayOf(id.toString()))
        baoDoi()
    }

    private fun doc(c: android.database.Cursor) = SuKien(
        id = c.getLong(c.getColumnIndexOrThrow("_id")),
        tieuDe = c.getString(c.getColumnIndexOrThrow("tieu_de")) ?: "",
        ghiChu = c.getString(c.getColumnIndexOrThrow("ghi_chu")) ?: "",
        batDau = c.getLong(c.getColumnIndexOrThrow("bat_dau")),
        ketThuc = c.getLong(c.getColumnIndexOrThrow("ket_thuc")),
        caNgay = c.getInt(c.getColumnIndexOrThrow("ca_ngay")) != 0,
        mau = c.getInt(c.getColumnIndexOrThrow("mau")),
        lapLai = c.getInt(c.getColumnIndexOrThrow("lap_lai")),
        nhacTruoc = c.getInt(c.getColumnIndexOrThrow("nhac_truoc")),
        diaDiem = c.getString(c.getColumnIndexOrThrow("dia_diem")) ?: "",
        lapDen = c.getColumnIndexOrThrow("lap_den").let { if (c.isNull(it)) null else c.getLong(it) },
    )

    // ------------------------------------------------------------ dong bo

    /**
     * Su kien chua day len may chu, doi thanh payload de gui di.
     *
     * Ten truong giu dung cua ban Flutter cu (`startAt`, `allDay`, `remindMinutes`…)
     * — kho tren Supabase dung chung, doi ten la du lieu cu thanh rac.
     */
    fun suKienChuaDay(): List<BanGhiDongBo> {
        val cur = readableDatabase.query(BANG, null, "ban_nhap = 1", null, null, null, null)
        return cur.use { c ->
            ArrayList<BanGhiDongBo>(c.count).apply {
                while (c.moveToNext()) {
                    val uid = c.getString(c.getColumnIndexOrThrow("uid")).orEmpty()
                    if (uid.isEmpty()) continue
                    val ngaySua = c.getLong(c.getColumnIndexOrThrow("ngay_sua"))
                    val daXoa = c.getInt(c.getColumnIndexOrThrow("da_xoa")) != 0
                    val lapDenCot = c.getColumnIndexOrThrow("lap_den")

                    val goi = JSONObject().apply {
                        put("uid", uid)
                        put("title", c.getString(c.getColumnIndexOrThrow("tieu_de")).orEmpty())
                        put("description", c.getString(c.getColumnIndexOrThrow("ghi_chu")).orEmpty())
                        put("startAt", c.getLong(c.getColumnIndexOrThrow("bat_dau")))
                        put("endAt", c.getLong(c.getColumnIndexOrThrow("ket_thuc")))
                        put("allDay", c.getInt(c.getColumnIndexOrThrow("ca_ngay")))
                        put("color", c.getInt(c.getColumnIndexOrThrow("mau")))
                        put("repeat", c.getInt(c.getColumnIndexOrThrow("lap_lai")))
                        put("createdAt", c.getLong(c.getColumnIndexOrThrow("ngay_tao")))
                        put("updatedAt", ngaySua)
                        // -1 trong may = "khong nhac"; tren may chu la null.
                        val nhac = c.getInt(c.getColumnIndexOrThrow("nhac_truoc"))
                        put("remindMinutes", if (nhac < 0) JSONObject.NULL else nhac)
                        put("repeatUntil", if (c.isNull(lapDenCot)) JSONObject.NULL else c.getLong(lapDenCot))
                        put("deletedAt", if (daXoa) ngaySua else JSONObject.NULL)
                        // Truong rieng cua ban nay, ban Flutter khong co.
                        put("diaDiem", c.getString(c.getColumnIndexOrThrow("dia_diem")).orEmpty())
                    }
                    add(BanGhiDongBo(LOAI_SU_KIEN, uid, ngaySua, goi))
                }
            }
        }
    }

    fun danhDauSuKienDaDay(uids: Collection<String>) {
        if (uids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val v = ContentValues().apply { put("ban_nhap", 0) }
            for (uid in uids) db.update(BANG, v, "uid = ?", arrayOf(uid))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Ghi mot su kien tu may chu xuong. Tra ve true neu du lieu trong may doi.
     *
     * Dung do: **ban sua sau thang**, so theo tung su kien chu khong phai ca CSDL.
     */
    fun apSuKien(goi: JSONObject): Boolean {
        val uid = goi.optString("uid").orEmpty()
        if (uid.isEmpty()) return false
        val suaXa = goi.optLong("updatedAt", 0L)

        val db = writableDatabase
        db.query(BANG, arrayOf("_id", "ngay_sua"), "uid = ?", arrayOf(uid), null, null, null)
            .use { c ->
                if (c.moveToFirst() && c.getLong(1) >= suaXa) return false
            }

        val v = ContentValues().apply {
            put("uid", uid)
            put("tieu_de", goi.optString("title"))
            put("ghi_chu", goi.optString("description"))
            put("bat_dau", goi.optLong("startAt"))
            put("ket_thuc", goi.optLong("endAt"))
            put("ca_ngay", if (goi.optBoolean("allDay") || goi.optInt("allDay") != 0) 1 else 0)
            put("mau", goi.optInt("color"))
            put("lap_lai", goi.optInt("repeat"))
            put("nhac_truoc", if (goi.isNull("remindMinutes")) -1 else goi.optInt("remindMinutes"))
            put("dia_diem", goi.optString("diaDiem"))
            put("ngay_tao", goi.optLong("createdAt", suaXa))
            put("ngay_sua", suaXa)
            put("da_xoa", if (goi.isNull("deletedAt")) 0 else 1)
            // Vua lay tu may chu ve thi dung day nguoc len — khong thi hai may
            // day qua day lai mai khong dut.
            put("ban_nhap", 0)
            if (goi.isNull("repeatUntil")) putNull("lap_den") else put("lap_den", goi.optLong("repeatUntil"))
        }

        val sua = db.update(BANG, v, "uid = ?", arrayOf(uid))
        if (sua == 0) db.insert(BANG, null, v)
        baoDoi()
        return true
    }

    /** Danh dau moi thu de day het len — dung ngay sau khi dang nhap. */
    fun danhDauTatCaChuaDay() {
        writableDatabase.execSQL("UPDATE $BANG SET ban_nhap = 1")
        writableDatabase.execSQL("UPDATE $BANG_GHI_CHU SET ban_nhap = 1")
    }

    companion object {
        const val TEN_DB = "ghichu.db"

        /** v2 = them bang cac cuoc tro chuyen voi tro ly AI. */
        const val PHIEN_BAN = 2

        const val BANG = "su_kien"
        const val BANG_GHI_CHU = "ghi_chu"
        const val BANG_THU_MUC = "thu_muc"
        const val BANG_QR = "qr_lich_su"
        const val BANG_HOI_THOAI = "hoi_thoai"

        /** Ten "loai" tren bang `sync_items` — dung chung voi ban Flutter cu. */
        const val LOAI_SU_KIEN = "event"
        const val LOAI_GHI_CHU = "note"
    }
}
