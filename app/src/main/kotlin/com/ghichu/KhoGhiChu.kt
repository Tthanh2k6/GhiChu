package com.ghichu

import android.content.ContentValues
import android.database.Cursor
import org.json.JSONObject

/**
 * Doc/ghi ghi chu, va cau noi giua ghi chu va duong dong bo.
 *
 * BA DIEU PHAI BIET TRUOC KHI SUA FILE NAY
 *
 * 1. **Ten truong trong goi gui di la co dinh.** `title`, `content`, `folder`,
 *    `pinned`, `bg`, `hidden`, `createdAt`, `updatedAt`, `deletedAt`, `remindAt`
 *    — dung y ban mod cu va ban Flutter truoc do. Kho tren Supabase dung chung
 *    cho ca ba; doi ten mot truong la ghi chu cu thanh rac.
 *
 * 2. **Ghi chu rieng tu khong len may chu.** Do la thu nguoi dung co y giau; day
 *    len la pha vo y do. Chung nam trong may, va se duoc ma hoa that o chang
 *    sau. Vi vay [chuaDay] loc `rieng_tu = 0`.
 *
 * 3. **Vao thung rac = gui bia mo.** Ban mod cung lam vay, nen hai ban xoa an
 *    khop nhau. Thung rac la chuyen RIENG cua may nay: hang van con, khoi phuc
 *    duoc, va luc khoi phuc thi day len lai voi `updatedAt` moi — dung do giai
 *    theo "ban sua sau thang" nen may kia se hoi sinh theo.
 */
class KhoGhiChu(private val kho: KhoDuLieu) {

    // ------------------------------------------------------------ doc

    /**
     * Danh sach ghi chu de hien len man hinh chinh.
     *
     * @param thuMuc `null` = tat ca thu muc; chuoi rong = ghi chu chua xep thu muc.
     * @param tim chuoi tim kiem, khong phan biet hoa thuong.
     * @param riengTu lay ghi chu rieng tu thay vi ghi chu thuong.
     * @param thungRac lay ghi chu trong thung rac.
     */
    fun danhSach(
        thuMuc: String? = null,
        tim: String = "",
        riengTu: Boolean = false,
        thungRac: Boolean = false,
    ): List<GhiChu> {
        val dieu = StringBuilder("da_xoa = ? AND rieng_tu = ?")
        val doi = ArrayList<String>()
        doi += if (thungRac) "1" else "0"
        doi += if (riengTu) "1" else "0"

        if (thuMuc != null && !thungRac) {
            dieu.append(" AND thu_muc = ?")
            doi += thuMuc
        }
        val chu = tim.trim()
        if (chu.isNotEmpty()) {
            // Tim ca trong tieu de lan than bai. `chu_tron` da lot the san nen o
            // day khong phai dong den markup.
            dieu.append(" AND (tieu_de LIKE ? OR chu_tron LIKE ?)")
            val mau = "%" + chu + "%"
            doi += mau
            doi += mau
        }

        // Ghim len dau, roi moi nhat truoc — dung thu tu app Ghi chu von co.
        return kho.readableDatabase.query(
            KhoDuLieu.BANG_GHI_CHU, null, dieu.toString(), doi.toTypedArray(),
            null, null, "ghim DESC, ngay_sua DESC",
        ).use { c -> ArrayList<GhiChu>(c.count).apply { while (c.moveToNext()) add(doc(c)) } }
    }

    fun theoId(id: Long): GhiChu? = kho.readableDatabase.query(
        KhoDuLieu.BANG_GHI_CHU, null, "_id = ?", arrayOf(id.toString()), null, null, null,
    ).use { if (it.moveToFirst()) doc(it) else null }

    fun soLuong(thuMuc: String): Int = kho.readableDatabase.query(
        KhoDuLieu.BANG_GHI_CHU, arrayOf("_id"),
        "da_xoa = 0 AND rieng_tu = 0 AND thu_muc = ?", arrayOf(thuMuc), null, null, null,
    ).use { it.count }

    // ------------------------------------------------------------ ghi

    /**
     * Them moi neu `id == 0`, nguoc lai cap nhat. Tra ve id sau khi luu.
     *
     * `chu_tron` va `ngay_sua` do day tinh, khong nhan tu ben goi: de ben goi tu
     * dat thi truoc sau gi cung co mot duong quen, roi tim kiem khong ra ghi chu
     * vua sua ma khong ai hieu vi sao.
     */
    fun luu(g: GhiChu): Long {
        val gio = System.currentTimeMillis()
        val v = ContentValues().apply {
            put("tieu_de", g.tieuDe)
            put("noi_dung", g.noiDung)
            put("chu_tron", NoiDungGhiChu.chuTron(g.noiDung))
            put("thu_muc", g.thuMuc)
            put("ghim", if (g.ghim) 1 else 0)
            put("mau", g.mau)
            put("rieng_tu", if (g.riengTu) 1 else 0)
            if (g.nhacLuc == null) putNull("nhac_luc") else put("nhac_luc", g.nhacLuc)
            put("ngay_sua", gio)
            put("ban_nhap", 1)
        }
        val id = if (g.id == 0L) {
            v.put("ngay_tao", if (g.ngayTao > 0) g.ngayTao else gio)
            v.put("uid", g.uid.ifEmpty { Uid.moi() })
            kho.writableDatabase.insert(KhoDuLieu.BANG_GHI_CHU, null, v)
        } else {
            kho.writableDatabase.update(
                KhoDuLieu.BANG_GHI_CHU, v, "_id = ?", arrayOf(g.id.toString()),
            )
            g.id
        }
        kho.baoDoi()
        return id
    }

    fun datGhim(id: Long, ghim: Boolean) = doiCot(id) { put("ghim", if (ghim) 1 else 0) }

    fun datMau(id: Long, mau: Int) = doiCot(id) { put("mau", mau) }

    fun chuyenThuMuc(id: Long, thuMuc: String) = doiCot(id) { put("thu_muc", thuMuc) }

    fun datNhac(id: Long, luc: Long?) = doiCot(id) {
        if (luc == null) putNull("nhac_luc") else put("nhac_luc", luc)
    }

    /** Vao thung rac. Hang van con — xem doan dau lop. */
    fun boVaoThungRac(id: Long) = doiCot(id) {
        put("da_xoa", 1)
        put("xoa_luc", System.currentTimeMillis())
    }

    fun khoiPhuc(id: Long) = doiCot(id) {
        put("da_xoa", 0)
        putNull("xoa_luc")
    }

    /** Xoa han mot ghi chu khoi may. Khong the hoan tac. */
    fun xoaHan(id: Long) {
        kho.writableDatabase.delete(KhoDuLieu.BANG_GHI_CHU, "_id = ?", arrayOf(id.toString()))
    }

    fun donThungRac() {
        kho.writableDatabase.delete(KhoDuLieu.BANG_GHI_CHU, "da_xoa = 1", null)
    }

    /**
     * Xoa han nhung ghi chu da nam trong thung rac qua [soNgay] ngay.
     *
     * Goi luc mo app. Khong co no thi thung rac phinh mai, ma nguoi dung khong
     * bao gio nho vao don.
     */
    fun donRacQuaHan(soNgay: Int = 30) {
        val moc = System.currentTimeMillis() - soNgay * 24L * 60 * 60 * 1000
        kho.writableDatabase.delete(
            KhoDuLieu.BANG_GHI_CHU, "da_xoa = 1 AND xoa_luc IS NOT NULL AND xoa_luc < ?",
            arrayOf(moc.toString()),
        )
    }

    private inline fun doiCot(id: Long, dat: ContentValues.() -> Unit) {
        val v = ContentValues().apply {
            dat()
            put("ngay_sua", System.currentTimeMillis())
            put("ban_nhap", 1)
        }
        kho.writableDatabase.update(KhoDuLieu.BANG_GHI_CHU, v, "_id = ?", arrayOf(id.toString()))
        kho.baoDoi()
    }

    // ------------------------------------------------------------ thu muc

    /** Ten cac thu muc, kem so ghi chu trong tung thu muc. */
    fun thuMucKemSo(): List<Pair<String, Int>> {
        val ten = LinkedHashSet<String>()
        kho.readableDatabase.query(
            KhoDuLieu.BANG_THU_MUC, arrayOf("ten"), null, null, null, null, "thu_tu ASC, _id ASC",
        ).use { c -> while (c.moveToNext()) ten += c.getString(0).orEmpty() }

        // Ghi chu keo ve tu may chu co the mang ten thu muc chua co hang o bang
        // thu muc — van phai hien, khong thi ghi chu do bien mat khoi moi thu muc.
        kho.readableDatabase.rawQuery(
            "SELECT DISTINCT thu_muc FROM " + KhoDuLieu.BANG_GHI_CHU +
                " WHERE da_xoa = 0 AND rieng_tu = 0 AND thu_muc != ''",
            null,
        ).use { c -> while (c.moveToNext()) ten += c.getString(0).orEmpty() }

        return ten.filter { it.isNotEmpty() }.map { it to soLuong(it) }
    }

    fun themThuMuc(ten: String): Boolean {
        val chu = ten.trim()
        if (chu.isEmpty()) return false
        val v = ContentValues().apply {
            put("ten", chu)
            put("ngay_tao", System.currentTimeMillis())
        }
        return kho.writableDatabase.insert(KhoDuLieu.BANG_THU_MUC, null, v) >= 0
    }

    fun doiTenThuMuc(cu: String, moi: String) {
        val chu = moi.trim()
        if (chu.isEmpty() || chu == cu) return
        chuyenHetGhiChu(cu, chu) { db ->
            db.update(
                KhoDuLieu.BANG_THU_MUC, ContentValues().apply { put("ten", chu) },
                "ten = ?", arrayOf(cu),
            )
        }
    }

    /** Xoa thu muc; ghi chu ben trong tro ve "chua xep thu muc", khong mat. */
    fun xoaThuMuc(ten: String) {
        chuyenHetGhiChu(ten, "") { db ->
            db.delete(KhoDuLieu.BANG_THU_MUC, "ten = ?", arrayOf(ten))
        }
    }

    /**
     * Doi ban thu muc, roi chuyen moi ghi chu tu [tu] sang [den], trong mot giao dich.
     *
     * Chuyen thu muc la SUA ghi chu — moi ghi chu bi anh huong deu phai duoc danh
     * dau day len lai, khong thi may kia van thay ten cu mai.
     */
    private inline fun chuyenHetGhiChu(
        tu: String,
        den: String,
        doiBang: (android.database.sqlite.SQLiteDatabase) -> Unit,
    ) {
        val db = kho.writableDatabase
        db.beginTransaction()
        try {
            doiBang(db)
            db.update(
                KhoDuLieu.BANG_GHI_CHU,
                ContentValues().apply {
                    put("thu_muc", den)
                    put("ngay_sua", System.currentTimeMillis())
                    put("ban_nhap", 1)
                },
                "thu_muc = ?", arrayOf(tu),
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ------------------------------------------------------------ dong bo

    /** Ghi chu chua day len may chu, doi thanh goi de gui di. */
    fun chuaDay(): List<BanGhiDongBo> {
        val ra = ArrayList<BanGhiDongBo>()
        kho.readableDatabase.query(
            KhoDuLieu.BANG_GHI_CHU, null, "ban_nhap = 1 AND rieng_tu = 0", null, null, null, null,
        ).use { c ->
            while (c.moveToNext()) {
                val g = doc(c)
                if (g.uid.isEmpty()) continue
                val goi = JSONObject().apply {
                    put("uid", g.uid)
                    put("title", g.tieuDe)
                    put("content", g.noiDung)
                    put("folder", g.thuMuc)
                    put("pinned", if (g.ghim) 1 else 0)
                    put("bg", g.mau)
                    put("hidden", 0)
                    put("createdAt", g.ngayTao)
                    put("updatedAt", g.ngaySua)
                    put("deletedAt", if (g.daXoa) g.ngaySua else JSONObject.NULL)
                    put("remindAt", g.nhacLuc ?: JSONObject.NULL)
                }
                ra += BanGhiDongBo(KhoDuLieu.LOAI_GHI_CHU, g.uid, g.ngaySua, goi)
            }
        }
        return ra
    }

    fun danhDauDaDay(uids: Collection<String>) {
        if (uids.isEmpty()) return
        val db = kho.writableDatabase
        db.beginTransaction()
        try {
            val v = ContentValues().apply { put("ban_nhap", 0) }
            for (uid in uids) db.update(KhoDuLieu.BANG_GHI_CHU, v, "uid = ?", arrayOf(uid))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Ghi mot ghi chu tu may chu xuong. Tra ve true neu du lieu trong may doi.
     *
     * Dung do: **ban sua sau thang**, so theo tung ghi chu chu khong phai ca CSDL.
     */
    fun apGhiChu(goi: JSONObject): Boolean {
        val uid = chuoi(goi, "uid")
        if (uid.isEmpty()) return false
        val suaXa = goi.optLong("updatedAt", 0L)

        kho.readableDatabase.query(
            KhoDuLieu.BANG_GHI_CHU, arrayOf("ngay_sua"), "uid = ?", arrayOf(uid), null, null, null,
        ).use { c -> if (c.moveToFirst() && c.getLong(0) >= suaXa) return false }

        // Ghi chu cu tren may chu do ban Flutter day len con o dang Delta cua Quill;
        // [NoiDungGhiChu.doc] nhan ra va doi sang markup. Bo buoc nay thi ghi chu cu
        // mo ra chi thay mot dong JSON.
        val noiDung = NoiDungGhiChu.doc(chuoi(goi, "content"))
        val daXoa = !goi.isNull("deletedAt")
        val v = ContentValues().apply {
            put("uid", uid)
            put("tieu_de", chuoi(goi, "title"))
            put("noi_dung", noiDung)
            put("chu_tron", NoiDungGhiChu.chuTron(noiDung))
            put("thu_muc", chuoi(goi, "folder"))
            put("ghim", if (goi.optInt("pinned") != 0) 1 else 0)
            put("mau", goi.optInt("bg"))
            put("rieng_tu", 0)
            if (goi.isNull("remindAt")) putNull("nhac_luc") else put("nhac_luc", goi.optLong("remindAt"))
            put("ngay_tao", goi.optLong("createdAt", suaXa))
            put("ngay_sua", suaXa)
            put("da_xoa", if (daXoa) 1 else 0)
            if (daXoa) put("xoa_luc", goi.optLong("deletedAt", suaXa)) else putNull("xoa_luc")
            // Vua lay tu may chu ve thi dung day nguoc len — khong thi hai may
            // day qua day lai mai khong dut.
            put("ban_nhap", 0)
        }

        val db = kho.writableDatabase
        if (db.update(KhoDuLieu.BANG_GHI_CHU, v, "uid = ?", arrayOf(uid)) == 0) {
            db.insert(KhoDuLieu.BANG_GHI_CHU, null, v)
        }
        kho.baoDoi()
        return true
    }

    // ------------------------------------------------------------ duong cho AI

    /**
     * Tao ghi chu moi tu tro ly AI. Tra ve `_id`, hoac -1 neu that bai.
     *
     * [markup] la markup cua ghi chu (xem [NoiDungGhiChu.tuMarkdown]) chu khong
     * phai chu tron va khong phai Markdown.
     */
    fun taoGhiChu(tieuDe: String, markup: String): Long =
        luu(GhiChu(tieuDe = tieuDe.trim(), noiDung = markup))

    /**
     * Ghi de hoac noi them noi dung vao mot ghi chu da co.
     *
     * @param noiThem true = them vao cuoi, false = viet lai ca bai
     */
    fun suaGhiChu(id: Long, markup: String, noiThem: Boolean): Boolean {
        val cu = theoId(id) ?: return false
        val moi = if (noiThem && cu.noiDung.isNotBlank()) cu.noiDung + "\n" + markup else markup
        doiCot(id) {
            put("noi_dung", moi)
            put("chu_tron", NoiDungGhiChu.chuTron(moi))
        }
        return true
    }

    /**
     * Tim ghi chu theo tu khoa AI dua ra.
     *
     * Uu tien khop TIEU DE, khong co thi moi tim trong noi dung — AI thuong tra
     * ve dung tieu de, tim ca noi dung truoc la de vo nham bai khac.
     */
    fun timTheoTuKhoa(tuKhoa: String): Long? {
        val k = tuKhoa.trim()
        if (k.isEmpty()) return null
        timMot("tieu_de LIKE ?", "%" + k + "%")?.let { return it }
        return timMot("chu_tron LIKE ?", "%" + k + "%")
    }

    private fun timMot(loc: String, thamSo: String): Long? = kho.readableDatabase.query(
        KhoDuLieu.BANG_GHI_CHU, arrayOf("_id"),
        "da_xoa = 0 AND rieng_tu = 0 AND (" + loc + ")", arrayOf(thamSo),
        null, null, "ngay_sua DESC", "1",
    ).use { if (it.moveToFirst()) it.getLong(0) else null }

    /**
     * Ghi chu rut gon, gui kem cho AI biet trong may dang co gi.
     *
     * Ghi chu rieng tu KHONG co trong danh sach nay: gui len Gemini cung la day
     * ra ngoai may, khong khac gi dong bo.
     */
    fun banGon(gioiHan: Int): List<AiLoiNhac.GhiChuNgan> {
        val ra = ArrayList<AiLoiNhac.GhiChuNgan>()
        kho.readableDatabase.query(
            KhoDuLieu.BANG_GHI_CHU, arrayOf("tieu_de", "chu_tron"),
            "da_xoa = 0 AND rieng_tu = 0", null, null, null, "ngay_sua DESC",
        ).use { c ->
            while (c.moveToNext() && ra.size < gioiHan) {
                val chuTron = c.getString(1).orEmpty()
                val tieuDe = c.getString(0).orEmpty()
                    .ifBlank { chuTron.lineSequence().firstOrNull().orEmpty() }
                if (tieuDe.isBlank()) continue
                ra += AiLoiNhac.GhiChuNgan(tieuDe, chuTron.take(120).replace('\n', ' '))
            }
        }
        return ra
    }

    // ------------------------------------------------------------ rieng

    /**
     * Doc chuoi tu JSON cho DUNG tren Android.
     *
     * `JSONObject.optString` cua Android tra ve chuoi **"null"** (bon chu cai) khi
     * gia tri la JSON null, khac han ban org.json goc tra ve chuoi rong. Khong co
     * ham nay thi ghi chu khong tieu de hien chu "null" tren may that, ma kiem
     * thu tren JVM khong bao gio thay.
     */
    private fun chuoi(o: JSONObject, ten: String): String =
        if (o.isNull(ten)) "" else o.optString(ten)

    private fun doc(c: Cursor) = GhiChu(
        id = c.getLong(c.getColumnIndexOrThrow("_id")),
        uid = c.getString(c.getColumnIndexOrThrow("uid")).orEmpty(),
        tieuDe = c.getString(c.getColumnIndexOrThrow("tieu_de")).orEmpty(),
        noiDung = c.getString(c.getColumnIndexOrThrow("noi_dung")).orEmpty(),
        chuTron = c.getString(c.getColumnIndexOrThrow("chu_tron")).orEmpty(),
        thuMuc = c.getString(c.getColumnIndexOrThrow("thu_muc")).orEmpty(),
        ghim = c.getInt(c.getColumnIndexOrThrow("ghim")) != 0,
        mau = c.getInt(c.getColumnIndexOrThrow("mau")),
        riengTu = c.getInt(c.getColumnIndexOrThrow("rieng_tu")) != 0,
        nhacLuc = c.getColumnIndexOrThrow("nhac_luc").let { if (c.isNull(it)) null else c.getLong(it) },
        ngayTao = c.getLong(c.getColumnIndexOrThrow("ngay_tao")),
        ngaySua = c.getLong(c.getColumnIndexOrThrow("ngay_sua")),
        daXoa = c.getInt(c.getColumnIndexOrThrow("da_xoa")) != 0,
    )
}
