package com.ghichu

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Loi da dich sang tieng nguoi — cu nem ra la man hinh hien nguyen van. */
class LoiGemini(message: String) : IOException(message)

/** Mot luot trong doan hoi thoai gui kem cho AI. */
class LuotChat(val vai: String, val chu: String) {
    companion object {
        const val NGUOI = "user"
        const val MAY = "model"
    }
}

/**
 * Goi thang Gemini API (Google AI Studio) — dung duoc voi API key mien phi.
 *
 * Chuyen sang Kotlin tu ban demo (`lib/services/gemini_service.dart`), giu nguyen
 * ten model va cach dien giai loi vi nhung thu do da **do bang API that** o du an
 * do; doan lai la mat cong do lai tu dau.
 *
 * KHONG NHUNG API KEY VAO APK. Day la quyet dinh cua nguoi dung sau khi nghe ro
 * rui ro: khoa Gemini lo la nguoi khac xai het han muc cua minh — khac han khoa
 * `publishable` cua Supabase (thu do von duoc thiet ke de lo). Nguoi dung tu dan
 * khoa mot lan trong Cai dat.
 *
 * Phan goi mang tach roi phan doc ket qua ([docTraLoi], [bocJson]) de con kiem thu
 * duoc tren JVM khi khong co dien thoai.
 */
object Gemini {

    private const val TEP_CAI_DAT = "ghichu_ai"
    private const val K_KHOA = "gemini_api_key"
    private const val K_MODEL = "gemini_model"

    private const val CHO_TOI_DA_MS = 90_000

    /**
     * Da kiem chung bang API that (du an cu, 2026-07-15): dong `2.5-*` tra 404
     * "no longer available to new users", con dong Pro co free-tier quota = 0.
     */
    const val MODEL_MAC_DINH = "gemini-3.5-flash"

    val MODEL = listOf(
        "gemini-3.5-flash",
        "gemini-3.1-flash-lite",
        "gemini-flash-latest",
    )

    private lateinit var caiDat: SharedPreferences

    fun khoiTao(context: Context) {
        if (::caiDat.isInitialized) return
        caiDat = context.applicationContext.getSharedPreferences(TEP_CAI_DAT, Context.MODE_PRIVATE)
    }

    var khoa: String
        get() = caiDat.getString(K_KHOA, "").orEmpty()
        set(v) {
            caiDat.edit().putString(K_KHOA, v.trim()).apply()
        }

    /** Model cu bi Google go thi tu lui ve [MODEL_MAC_DINH]. */
    var model: String
        get() = caiDat.getString(K_MODEL, MODEL_MAC_DINH).orEmpty()
            .let { if (it in MODEL) it else MODEL_MAC_DINH }
        set(v) {
            caiDat.edit().putString(K_MODEL, v).apply()
        }

    val daCoKhoa: Boolean get() = khoa.isNotEmpty()

    /**
     * Hoi Gemini mot cau. PHAI goi tu luong nen.
     *
     * @param jsonMode bat `response_mime_type: application/json` — dung khi ta
     *   can may tra ve JSON dung cau truc chu khong phai van xuoi.
     */
    fun sinh(
        loiNhac: String,
        heThong: String? = null,
        lichSu: List<LuotChat> = emptyList(),
        jsonMode: Boolean = false,
    ): String {
        val k = khoa
        if (k.isEmpty()) {
            throw LoiGemini("Chưa có API key. Vào Cài đặt → Trợ lý AI để dán khóa miễn phí từ Google AI Studio.")
        }
        val m = model

        val than = dungThan(loiNhac, heThong, lichSu, jsonMode)

        val diaChi =
            "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$k"

        val ket = try {
            URL(diaChi).openConnection() as HttpURLConnection
        } catch (_: Exception) {
            throw LoiGemini("Không kết nối được Gemini. Kiểm tra mạng rồi thử lại.")
        }

        val ma: Int
        val noi: String
        try {
            ket.requestMethod = "POST"
            ket.connectTimeout = CHO_TOI_DA_MS
            ket.readTimeout = CHO_TOI_DA_MS
            ket.setRequestProperty("Content-Type", "application/json")
            ket.doOutput = true
            ket.outputStream.use { it.write(than.toString().toByteArray(Charsets.UTF_8)) }

            ma = ket.responseCode
            val dong = if (ma >= 400) ket.errorStream else ket.inputStream
            noi = dong?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
        } catch (_: Exception) {
            throw LoiGemini("Không kết nối được Gemini. Kiểm tra mạng rồi thử lại.")
        } finally {
            ket.disconnect()
        }

        return docTraLoi(ma, noi, m)
    }

    /**
     * Dung than yeu cau gui len Gemini.
     *
     * Tach rieng de kiem thu duoc: day la **hop dong voi Google**, sai mot ten
     * truong la ca tinh nang chet ma chi biet luc chay that.
     *
     * Luot cua nguoi dung LUON dung cuoi, sau [lichSu] — Gemini doc `contents`
     * theo thu tu, dat truoc la no tra loi cau cu.
     */
    fun dungThan(
        loiNhac: String,
        heThong: String?,
        lichSu: List<LuotChat>,
        jsonMode: Boolean,
    ): JSONObject = JSONObject().apply {
        put(
            "contents",
            JSONArray().apply {
                for (l in lichSu) {
                    put(
                        JSONObject().apply {
                            put("role", l.vai)
                            put("parts", JSONArray().put(JSONObject().put("text", l.chu)))
                        },
                    )
                }
                put(
                    JSONObject().apply {
                        put("role", LuotChat.NGUOI)
                        put("parts", JSONArray().put(JSONObject().put("text", loiNhac)))
                    },
                )
            },
        )
        if (heThong != null) {
            put(
                "system_instruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", heThong))),
            )
        }
        put(
            "generationConfig",
            JSONObject().apply {
                put("temperature", 0.6)
                if (jsonMode) put("response_mime_type", "application/json")
            },
        )
    }

    /**
     * Doc cau tra loi tho cua API ra chu.
     *
     * Tach rieng khoi phan goi mang de kiem thu duoc tren JVM — moi nhanh loi o
     * day deu co bai kiem thu tuong ung.
     */
    fun docTraLoi(ma: Int, than: String, model: String = MODEL_MAC_DINH): String {
        val data = try {
            JSONObject(than)
        } catch (_: Exception) {
            throw LoiGemini("Gemini trả về dữ liệu không đọc được ($ma).")
        }

        if (ma != 200) {
            val loi = data.optJSONObject("error")?.optString("message").orEmpty()
            throw LoiGemini(
                when {
                    ma == 400 && loi.lowercase().contains("api key") ->
                        "API key không hợp lệ. Kiểm tra lại trong Cài đặt."
                    ma == 404 ->
                        "Model \"$model\" không dùng được nữa. Vào Cài đặt chọn model khác."
                    ma == 429 ->
                        "Hết lượt miễn phí tạm thời. Chờ khoảng 1 phút rồi thử lại, " +
                            "hoặc vào Cài đặt đổi sang model nhẹ hơn (flash-lite)."
                    else -> "Lỗi Gemini ($ma)" + if (loi.isEmpty()) "" else ": $loi"
                },
            )
        }

        val ungVien = data.optJSONArray("candidates")
        if (ungVien == null || ungVien.length() == 0) {
            throw LoiGemini("Gemini không trả về nội dung (có thể bị chặn bởi bộ lọc an toàn).")
        }
        val phan = ungVien.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            ?: throw LoiGemini("Gemini không trả về nội dung.")

        val chu = buildString {
            for (i in 0 until phan.length()) append(phan.optJSONObject(i)?.optString("text").orEmpty())
        }.trim()

        if (chu.isEmpty()) throw LoiGemini("Gemini trả về nội dung rỗng.")
        return chu
    }

    /**
     * Boc JSON khoi cau tra loi.
     *
     * Dan ky may van co luc model boc ket qua trong ```json … ```. Chi boc khi no
     * om TRON cau tra loi, khong thi an nham dau ``` von la noi dung nguoi dung.
     */
    fun bocJson(raw: String): String {
        val s = raw.trim()
        val hangRao = Regex("^```[a-zA-Z]*\\s*([\\s\\S]*?)\\s*```$")
        return hangRao.find(s)?.groupValues?.get(1)?.trim() ?: s
    }
}
