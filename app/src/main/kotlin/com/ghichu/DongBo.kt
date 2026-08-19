package com.ghichu

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Mot ban ghi sap gui len may chu. */
class BanGhiDongBo(
    val loai: String,
    val uid: String,
    val suaLuc: Long,
    val goi: JSONObject,
)

/** Loi da dich sang tieng nguoi — cu nem ra la man hinh hien nguyen van. */
class LoiDongBo(message: String) : IOException(message)

/**
 * Dong bo ghi chu & lich trinh qua **Supabase**, goi thang REST bang
 * `HttpURLConnection`.
 *
 * CO Y khong keo thu vien nao vao: code nay duoc TIEM vao APK cua Xiaomi, moi
 * lop them deu phai chep smali sang va phai chac app dich chua co lop trung ten.
 * `HttpURLConnection` + `org.json` co san trong Android, khong ton gi.
 *
 * Cach hoat dong (dong bo kieu "local-first"):
 *   1. May van ghi thang vao CSDL trong may — mat mang van dung binh thuong.
 *   2. Ban ghi nao doi thi bi danh dau chua day len.
 *   3. Moi luot: **day** het cai chua day, roi **keo** ve nhung gi may khac doi
 *      ke tu moc lan truoc.
 *   4. Dung do (hai may cung sua mot ban ghi): **ban sua sau thang**, so theo
 *      tung ban ghi chu khong phai ca CSDL.
 *
 * Moc de keo ve la `server_at` — gio cua MAY CHU, khong phai gio may nay. Dong
 * ho dien thoai lech vai phut la chuyen thuong; lay gio may minh lam moc thi se
 * co ban ghi khong bao gio duoc keo ve.
 *
 * Bang tren may chu la `sync_items`, dung chung cho ca ghi chu lan su kien, phan
 * biet bang cot `kind`. Doi chieu: D:\Project\Note\supabase_setup.sql
 */
object DongBo {

    private const val NHAT_KY = "GhiChuExt"

    /**
     * Dia chi va khoa nhung san — may moi cai xong chi viec go email/mat khau.
     *
     * Nhung khoa nay KHONG lam giam an toan: khoa `publishable` von duoc Supabase
     * thiet ke de lo cong khai (thu vien web nao cung nhung no). Thu that su canh
     * cua la Row Level Security — co khoa ma khong dang nhap thi khong doc duoc
     * cua ai. KHAC HAN khoa Gemini: khoa do lo la nguoi khac xai het han muc.
     */
    private const val DIA_CHI_SAN = "https://ckkysapisndylxujcfvf.supabase.co"
    private const val KHOA_SAN = "sb_publishable_Ftnfk-vm2R_-HXs1o6zI7Q_-dFgM94F"

    private const val TEP_CAI_DAT = "ghichu_dongbo"
    private const val BANG = "sync_items"
    private const val MOC_DAU = "1970-01-01T00:00:00Z"

    /** Gui toi da bay nhieu ban ghi mot lan — ghi chu co the dai. */
    private const val CO_MOT_LAN_DAY = 100
    private const val CO_MOT_TRANG_KEO = 400
    private const val CHO_TOI_DA_MS = 30_000

    private const val K_DIA_CHI = "dia_chi"
    private const val K_KHOA = "khoa"
    private const val K_TRUY_CAP = "token_truy_cap"
    private const val K_LAM_MOI = "token_lam_moi"
    private const val K_EMAIL = "email"
    private const val K_NGUOI = "nguoi_dung"
    private const val K_HET_HAN = "het_han"
    private const val K_MOC = "moc"
    private const val K_LAN_CUOI = "lan_cuoi"

    // ---- Trang thai ----

    private var diaChi = ""
    private var khoa = ""
    private var tokenTruyCap: String? = null
    private var tokenLamMoi: String? = null
    private var nguoiDungId = ""
    private var hetHanLuc = 0L

    var email = ""
        private set
    var lanCuoi = 0L
        private set
    var loiCuoi: String? = null
        private set

    @Volatile
    var dangChay = false
        private set

    val daKhaiBao: Boolean get() = diaChi.isNotEmpty() && khoa.isNotEmpty()
    val daDangNhap: Boolean get() = daKhaiBao && !tokenLamMoi.isNullOrEmpty()

    private lateinit var caiDat: SharedPreferences

    fun khoiTao(context: Context) {
        if (::caiDat.isInitialized) return
        caiDat = context.applicationContext.getSharedPreferences(TEP_CAI_DAT, Context.MODE_PRIVATE)
        // Nhap tay de len ban nhung san; chua nhap gi thi dung ban nhung.
        diaChi = caiDat.getString(K_DIA_CHI, "").orEmpty().ifEmpty { DIA_CHI_SAN }
        khoa = caiDat.getString(K_KHOA, "").orEmpty().ifEmpty { KHOA_SAN }
        tokenTruyCap = caiDat.getString(K_TRUY_CAP, null)
        tokenLamMoi = caiDat.getString(K_LAM_MOI, null)
        email = caiDat.getString(K_EMAIL, "").orEmpty()
        nguoiDungId = caiDat.getString(K_NGUOI, "").orEmpty()
        hetHanLuc = caiDat.getLong(K_HET_HAN, 0L)
        lanCuoi = caiDat.getLong(K_LAN_CUOI, 0L)
    }

    // ---- Dang nhap ----

    fun dangKy(kho: KhoDuLieu, email: String, matKhau: String) =
        xacThuc(kho, "/auth/v1/signup", email, matKhau, laDangKy = true)

    fun dangNhap(kho: KhoDuLieu, email: String, matKhau: String) =
        xacThuc(kho, "/auth/v1/token?grant_type=password", email, matKhau, laDangKy = false)

    private fun xacThuc(
        kho: KhoDuLieu,
        duong: String,
        email: String,
        matKhau: String,
        laDangKy: Boolean,
    ) {
        if (!daKhaiBao) throw LoiDongBo("Chưa khai báo máy chủ đồng bộ.")
        if (email.isBlank() || matKhau.isEmpty()) throw LoiDongBo("Nhập email và mật khẩu đã.")
        if (matKhau.length < 6) throw LoiDongBo("Mật khẩu phải từ 6 ký tự trở lên.")

        val than = JSONObject().apply {
            put("email", email.trim())
            put("password", matKhau)
        }
        val tra = goi("POST", "$diaChi$duong", dauThuCong(), than.toString())
        val data = docJson(tra.than)

        if (tra.ma >= 400) throw LoiDongBo(loiXacThuc(tra.ma, data, laDangKy))
        if (data.optString("access_token").isEmpty()) {
            throw LoiDongBo(
                "Đã tạo tài khoản, nhưng Supabase đang bắt xác nhận email. " +
                    "Vào Authentication → Sign In / Providers → tắt \"Confirm email\", rồi đăng nhập lại.",
            )
        }

        luuPhien(data, email.trim())
        // May nay dang co san du lieu -> danh dau tat ca de day het len, coi nhu
        // "gop vao", khong xoa gi cua ai.
        kho.danhDauTatCaChuaDay()
    }

    fun dangXuat() {
        tokenTruyCap = null
        tokenLamMoi = null
        email = ""
        nguoiDungId = ""
        hetHanLuc = 0L
        lanCuoi = 0L
        loiCuoi = null
        caiDat.edit()
            .remove(K_TRUY_CAP).remove(K_LAM_MOI).remove(K_EMAIL)
            .remove(K_NGUOI).remove(K_HET_HAN).remove(K_MOC).remove(K_LAN_CUOI)
            .apply()
    }

    private fun luuPhien(data: JSONObject, emailGoiY: String) {
        tokenTruyCap = data.optString("access_token").ifEmpty { null }
        tokenLamMoi = data.optString("refresh_token").ifEmpty { null }
        hetHanLuc = System.currentTimeMillis() + data.optLong("expires_in", 3600L) * 1000L

        val nguoi = data.optJSONObject("user")
        email = nguoi?.optString("email").orEmpty().ifEmpty { emailGoiY }
        nguoiDungId = nguoi?.optString("id").orEmpty().ifEmpty { nguoiDungId }

        caiDat.edit()
            .putString(K_TRUY_CAP, tokenTruyCap)
            .putString(K_LAM_MOI, tokenLamMoi)
            .putString(K_EMAIL, email)
            .putString(K_NGUOI, nguoiDungId)
            .putLong(K_HET_HAN, hetHanLuc)
            .apply()
    }

    /** Xin token moi bang refresh token. False = phien het hieu luc. */
    private fun lamMoiPhien(): Boolean {
        val rt = tokenLamMoi
        if (rt.isNullOrEmpty()) return false
        val than = JSONObject().put("refresh_token", rt)
        val tra = goi(
            "POST", "$diaChi/auth/v1/token?grant_type=refresh_token",
            dauThuCong(), than.toString(),
        )
        if (tra.ma >= 400) return false
        luuPhien(docJson(tra.than), email)
        return true
    }

    // ---- Mot luot dong bo ----

    /**
     * Day len roi keo ve. Tra ve true neu du lieu trong may co doi (de man hinh
     * nap lai). KHONG nem loi ra ngoai — loi duoc cat vao [loiCuoi] vi ham nay
     * chay ngam la chinh.
     *
     * PHAI goi tu luong nen: no goi mang.
     */
    fun motLuot(context: Context, kho: KhoDuLieu): Boolean {
        if (!daDangNhap || dangChay) return false
        dangChay = true
        loiCuoi = null
        return try {
            val ghiChu = KhoGhiChu(kho)
            day(kho, ghiChu)
            val doi = keo(kho, ghiChu)
            lanCuoi = System.currentTimeMillis()
            caiDat.edit().putLong(K_LAN_CUOI, lanCuoi).apply()
            doi
        } catch (e: LoiDongBo) {
            loiCuoi = e.message
            false
        } catch (e: Exception) {
            loiCuoi = loiMang(e)
            Log.w(NHAT_KY, "dong bo hong", e)
            false
        } finally {
            dangChay = false
        }
    }

    private fun day(kho: KhoDuLieu, ghiChu: KhoGhiChu) {
        val tatCa = kho.suKienChuaDay() + ghiChu.chuaDay()
        if (tatCa.isEmpty()) return

        var i = 0
        while (i < tatCa.size) {
            val phan = tatCa.subList(i, minOf(i + CO_MOT_LAN_DAY, tatCa.size))
            val than = JSONArray()
            for (b in phan) {
                than.put(
                    JSONObject().apply {
                        // Gui kem user_id cho chac: bang da co DEFAULT auth.uid()
                        // nen bo trong cung duoc, nhung gui han thi khong phu
                        // thuoc vao viec PostgREST co chiu dien gia tri mac dinh
                        // cho cot nam trong on_conflict hay khong. Khong so gui
                        // bay: chinh sach RLS chan moi user_id khong phai cua
                        // nguoi dang dang nhap.
                        if (nguoiDungId.isNotEmpty()) put("user_id", nguoiDungId)
                        put("kind", b.loai)
                        put("uid", b.uid)
                        put("updated_at", b.suaLuc)
                        put("payload", b.goi)
                    },
                )
            }

            val tra = rest(
                "POST", "/rest/v1/$BANG?on_conflict=user_id,kind,uid",
                mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
                than.toString(),
            )
            if (tra.ma >= 400) throw LoiDongBo(loiRest(tra, "đẩy dữ liệu lên"))

            kho.danhDauSuKienDaDay(phan.filter { it.loai == KhoDuLieu.LOAI_SU_KIEN }.map { it.uid })
            ghiChu.danhDauDaDay(phan.filter { it.loai == KhoDuLieu.LOAI_GHI_CHU }.map { it.uid })
            i += CO_MOT_LAN_DAY
        }
    }

    private fun keo(kho: KhoDuLieu, ghiChu: KhoGhiChu): Boolean {
        var doi = false
        var moc = caiDat.getString(K_MOC, MOC_DAU).orEmpty().ifEmpty { MOC_DAU }
        val mocDau = moc

        while (true) {
            val mocTrang = moc
            val tra = rest(
                "GET",
                "/rest/v1/$BANG" +
                    "?select=kind,uid,updated_at,payload,server_at" +
                    "&server_at=gt.${URLEncoder.encode(moc, "UTF-8")}" +
                    "&order=server_at.asc" +
                    "&limit=$CO_MOT_TRANG_KEO",
            )
            if (tra.ma >= 400) throw LoiDongBo(loiRest(tra, "tải dữ liệu về"))

            val hang = try {
                JSONArray(tra.than)
            } catch (_: Exception) {
                break
            }
            if (hang.length() == 0) break

            for (i in 0 until hang.length()) {
                val h = hang.optJSONObject(i) ?: continue
                val goiTin = h.optJSONObject("payload") ?: continue
                // uid nam san trong payload, nhung lay theo cot cho chac.
                h.optString("uid").takeIf { it.isNotEmpty() }?.let { goiTin.put("uid", it) }

                val duocDoi = when (h.optString("kind")) {
                    KhoDuLieu.LOAI_SU_KIEN -> kho.apSuKien(goiTin)
                    KhoDuLieu.LOAI_GHI_CHU -> ghiChu.apGhiChu(goiTin)
                    else -> false
                }
                if (duocDoi) doi = true
                moc = h.optString("server_at").ifEmpty { moc }
            }

            // Het trang thi dung. Dieu kien `moc == mocTrang` la chot chan: neu ca
            // trang mang cung mot moc server_at thi moc khong nhich len duoc,
            // khong chan la quay vong mai mai.
            if (hang.length() < CO_MOT_TRANG_KEO || moc == mocTrang) break
        }

        if (moc != mocDau) caiDat.edit().putString(K_MOC, moc).apply()
        return doi
    }

    // ---- Goi HTTP ----

    private class TraLoi(val ma: Int, val than: String)

    /**
     * Dau thu cho cac loi goi CHUA dang nhap.
     *
     * Gui khoa o CA HAI cho `apikey` lan `Authorization` — dung nhu thu vien
     * chinh thuc cua Supabase lam khi chua co phien. Can ca hai vi Supabase dang
     * co hai doi khoa: khoa cu `anon` (chuoi JWT `eyJ…`) nhan o `apikey`, con
     * khoa moi `sb_publishable_…` thi co noi doi o `Authorization`.
     */
    private fun dauThuCong() = mapOf(
        "apikey" to khoa,
        "Authorization" to "Bearer $khoa",
        "Content-Type" to "application/json",
    )

    private fun dauThuCoPhien(): Map<String, String> {
        // Con duoi mot phut la het han thi xin token moi truoc cho chac.
        if (System.currentTimeMillis() > hetHanLuc - 60_000L) lamMoiPhien()
        return mapOf(
            "apikey" to khoa,
            "Authorization" to "Bearer ${tokenTruyCap.orEmpty()}",
            "Content-Type" to "application/json",
        )
    }

    /** Gap 401 thi xin token moi dung mot lan roi thu lai — token song 1 tieng. */
    private fun rest(
        cach: String,
        duong: String,
        dauThem: Map<String, String> = emptyMap(),
        than: String? = null,
    ): TraLoi {
        var tra = goi(cach, "$diaChi$duong", dauThuCoPhien() + dauThem, than)
        if (tra.ma == 401 && lamMoiPhien()) {
            tra = goi(cach, "$diaChi$duong", dauThuCoPhien() + dauThem, than)
        }
        return tra
    }

    private fun goi(
        cach: String,
        diaChiDayDu: String,
        dauThu: Map<String, String>,
        than: String?,
    ): TraLoi {
        val ket = URL(diaChiDayDu).openConnection() as HttpURLConnection
        try {
            ket.requestMethod = cach
            ket.connectTimeout = CHO_TOI_DA_MS
            ket.readTimeout = CHO_TOI_DA_MS
            dauThu.forEach { (k, v) -> ket.setRequestProperty(k, v) }

            if (than != null) {
                ket.doOutput = true
                ket.outputStream.use { it.write(than.toByteArray(Charsets.UTF_8)) }
            }

            val ma = ket.responseCode
            val dong = if (ma >= 400) ket.errorStream else ket.inputStream
            val noi = dong?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
            return TraLoi(ma, noi)
        } catch (e: java.net.SocketTimeoutException) {
            throw LoiDongBo("Máy chủ không trả lời (quá 30 giây). Thử lại sau.")
        } finally {
            ket.disconnect()
        }
    }

    // ---- Dien giai loi ----

    private fun docJson(than: String): JSONObject = try {
        JSONObject(than)
    } catch (_: Exception) {
        JSONObject()
    }

    private fun loiXacThuc(ma: Int, data: JSONObject, laDangKy: Boolean): String {
        val tho = listOf("msg", "error_description", "message", "error")
            .firstNotNullOfOrNull { data.optString(it).ifEmpty { null } }
            .orEmpty()
            .lowercase()

        return when {
            tho.contains("invalid login") -> "Sai email hoặc mật khẩu."
            tho.contains("already registered") || tho.contains("already exists") ->
                "Email này đã có tài khoản — bấm \"Đăng nhập\" thay vì \"Tạo tài khoản\"."
            tho.contains("not confirmed") ->
                "Email chưa xác nhận. Mở hộp thư bấm liên kết, hoặc tắt \"Confirm email\" trong Supabase."
            tho.contains("invalid api key") || ma == 401 -> "Khóa không đúng. Kiểm tra lại phần Máy chủ."
            ma == 404 -> "Không thấy máy chủ ở địa chỉ đó. Kiểm tra lại Project URL."
            ma == 429 -> "Thử quá nhiều lần, đợi một lát rồi làm lại."
            else -> "${if (laDangKy) "Tạo tài khoản" else "Đăng nhập"} không được (lỗi $ma)" +
                if (tho.isEmpty()) "" else ": $tho"
        }
    }

    private fun loiRest(tra: TraLoi, viec: String): String {
        val data = docJson(tra.than)
        val loi = data.optString("message").ifEmpty { data.optString("hint") }
        return when {
            tra.ma == 404 || loi.contains("does not exist") ->
                "Chưa tạo bảng trên máy chủ. Chạy đoạn SQL trong supabase_setup.sql của dự án cũ."
            tra.ma == 401 || tra.ma == 403 ->
                "Phiên đăng nhập hết hạn hoặc không đủ quyền. Đăng nhập lại giúp."
            else -> "Không $viec được (lỗi ${tra.ma})" + if (loi.isEmpty()) "" else ": $loi"
        }
    }

    private fun loiMang(e: Exception): String {
        val s = e.toString().lowercase()
        return if (s.contains("unknownhost") || s.contains("unable to resolve") ||
            s.contains("socket") || s.contains("connect")
        ) {
            "Không nối được mạng. Dữ liệu vẫn nằm an toàn trong máy, có mạng sẽ tự đẩy lên."
        } else {
            "Đồng bộ lỗi: $e"
        }
    }
}
