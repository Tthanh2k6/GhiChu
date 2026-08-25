package com.ghichu

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import kotlin.math.roundToInt

/**
 * Bang mau cua ca app.
 *
 * Gia tri lay THANG tu tai nguyen cua app Ghi chu Xiaomi da giai nen
 * (`reference/miui-notes/resources/res/values/colors.xml` cho ban sang,
 * `values-night/` cho ban toi; bang tong hop o `docs/SPEC-GOC.md`) — ten tai
 * nguyen goc ghi trong chu thich de con doi chieu. Dung chinh "cho dep" neu
 * chua do lai.
 *
 * Vi sao la LOP chu khong phai hang so: ban dau man Lich trinh dong cung mau
 * toi, may de giao dien sang thi luoi den si giua app trang. Moi man giu mot
 * the [BangMau] va doc lai khi nguoi dung doi cai dat.
 */
class BangMau(val toi: Boolean) {

    /**
     * Nen man hinh chinh. goc: homepage_primary_color.
     *
     * Ban sang la XAM NHAT #F7F7F7 chu khong phai trang tinh — the ghi chu
     * ([nenThe] trang) phai noi tren nen, khong co vien nao ca.
     */
    val nen: Int = if (toi) 0xFF000000.toInt() else 0xFFF7F7F7.toInt()

    /** Nen THE ghi chu. goc: v12_theme_content_bg_color. Khong vien, khong bong. */
    val nenThe: Int = if (toi) 0xFF242424.toInt() else 0xFFFFFFFF.toInt()

    /** Tieu de tren the. goc: v8_text_appearance_grid_primary_text_color. */
    val chuTheTieuDe: Int = if (toi) 0xF2FFFFFF.toInt() else 0xDE000000.toInt()

    /** Than bai tren the. goc: v11_text_appearance_secondary_text_color. */
    val chuTheThan: Int = if (toi) 0xFF919191.toInt() else 0xFF666666.toInt()

    /** Lop phu len the khi duoc CHON. goc: item_fg_press_color. */
    val lopPhuChon: Int = if (toi) 0x1FFFFFFF else 0x14000000

    /** Nen vien thuoc tim kiem. goc: miuix_appcompat_edit_text_search_bg_color. */
    val nenTimKiem: Int = if (toi) 0x24FFFFFF else 0x0F000000

    /** Chu goi y trong o tim kiem. goc: miuix_color_black_level6 / white_level6. */
    val chuGoiY: Int = if (toi) 0x4DFFFFFF else 0x4D000000

    /** Tab duoi dang CHON. goc: navigator_bottom_button_color. KHONG phai vang. */
    val tabChon: Int = if (toi) 0xF2FFFFFF.toInt() else 0xFF000000.toInt()

    /** Tab duoi thuong. goc: navigator_bottom_button_unchecked_color. */
    val tabThuong: Int = if (toi) 0x80FFFFFF.toInt() else 0x66000000.toInt()

    /** Vach 0.5dp tren thanh tab / toolbar. goc: miuix_design_default_color_surface_divider. */
    val vachThanh: Int = if (toi) 0x26FFFFFF else 0x1A000000

    /** Dong thong tin duoi tieu de man soan. goc: note_edit_extra_info_text_color. */
    val chuThongTin: Int = if (toi) 0x66FFFFFF else 0x66000000

    /** Icon toolbar man soan. goc: action_bottom_button_color. */
    val chuToolbar: Int = if (toi) 0xCCFFFFFF.toInt() else 0xCC000000.toInt()

    /** Vach tren toolbar man soan. goc: toolbar_divide_color. */
    val vachToolbar: Int = if (toi) 0x0DFFFFFF else 0x0D000000

    /** goc: v8_text_appearance_list_primary_text_color */
    val chuChinh: Int = if (toi) 0xF2FFFFFF.toInt() else 0xFF000000.toInt()

    /** goc: v8_text_appearance_list_secondary_text_color */
    val chuPhu: Int = if (toi) 0x80FFFFFF.toInt() else 0x99000000.toInt()

    /** goc: v8_text_appearance_list_tertiary_text_color */
    val chuMo: Int = if (toi) 0xFF7C7C7C.toInt() else 0xFF999999.toInt()

    /** goc: theme_main_color — vang chu dao. Sang #FFBB0F, toi #FFBF0F. */
    val nhan: Int = if (toi) 0xFFFFBF0F.toInt() else 0xFFFFBB0F.toInt()
    val chuTrenNhan: Int = 0xFF1A1A1A.toInt()

    /** Vach gio ngang va vach ngan cot. */
    val vach: Int = if (toi) 0xFF232323.toInt() else 0xFFE6E6E6.toInt()

    /** Duong ke duoi hang dau cot / hang "Cả ngày". */
    val vienHang: Int = if (toi) 0xFF2A2A2A.toInt() else 0xFFDCDCDC.toInt()

    /** Nen thanh duoi (su kien sap toi) va o nhap cua Tro ly AI. */
    val nenThanhDuoi: Int = if (toi) 0xFF141414.toInt() else 0xFFF2F2F2.toInt()

    /** Vach do chi gio hien tai. */
    val doBayGio: Int = 0xFFEF4444.toInt()

    /** Bong tin nhan trong man Tro ly AI. */
    val bongNguoi: Int = if (toi) 0xFF2C2C2E.toInt() else 0xFFE9E9EB.toInt()
    val bongMay: Int = if (toi) 0xFF1C1C1E.toInt() else 0xFFF2F2F7.toInt()

    // ---- Theme cua he thong ----

    /**
     * Theme cho Activity cua ban mod.
     *
     * Bat buoc phai dat, khong duoc de theme mac dinh: `Button`, `EditText`,
     * `RadioButton` lay mau tu THEME chu khong tu [BangMau]. May dang che do toi
     * ma nguoi dung chon "Sáng" thi nut van den si tren nen trang.
     *
     * Goi `setTheme` TRUOC `setContentView`, sau do moi dung view.
     */
    val themeManHinh: Int
        get() = if (toi) android.R.style.Theme_DeviceDefault_NoActionBar
        else android.R.style.Theme_DeviceDefault_Light_NoActionBar

    /**
     * Theme cho hop thoai.
     *
     * Cung ly do: chu trong hop do ta tu to mau theo [BangMau], con NEN hop lai
     * do theme quyet dinh. Lech nhau la chu den tren nen den.
     */
    val themeHopThoai: Int
        get() = if (toi) android.R.style.Theme_DeviceDefault_Dialog_Alert
        else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert

    // ---- Khoi su kien ----

    /**
     * Nen khoi su kien: chinh mau do nhung trong bot.
     *
     * Ban sang phai NHAT hon ban toi: mau trong tren nen trang von da sang san,
     * de nguyen 0.30 la khoi va chu deu bac mau.
     */
    fun nenKhoi(mau: Int): Int = mauMo(mau, if (toi) 0.30f else 0.22f)

    /** Chu trong khoi: nen toi thi pha ve TRANG, nen sang thi pha ve DEN. */
    fun chuKhoi(mau: Int): Int =
        if (toi) pha(mau, Color.WHITE, 0.55f) else pha(mau, Color.BLACK, 0.35f)

    fun gioKhoi(mau: Int): Int =
        if (toi) pha(mau, Color.WHITE, 0.30f) else pha(mau, Color.BLACK, 0.20f)

    companion object {
        private const val TEP_CAI_DAT = "ghichu_giao_dien"
        private const val K_KIEU = "kieu"

        const val THEO_HE_THONG = "he_thong"
        const val SANG = "sang"
        const val TOI = "toi"

        val TEN_KIEU = linkedMapOf(
            THEO_HE_THONG to "Theo hệ thống",
            SANG to "Sáng",
            TOI to "Tối",
        )

        fun kieu(context: Context): String =
            caiDat(context).getString(K_KIEU, THEO_HE_THONG).orEmpty()
                .let { if (it in TEN_KIEU) it else THEO_HE_THONG }

        fun datKieu(context: Context, kieu: String) {
            caiDat(context).edit().putString(K_KIEU, kieu).apply()
        }

        /** Bang mau dung cho man hinh, da giai xong "theo hệ thống". */
        fun cua(context: Context): BangMau = BangMau(laToi(context))

        fun laToi(context: Context): Boolean = when (kieu(context)) {
            SANG -> false
            TOI -> true
            else -> theoHeThong(context)
        }

        private fun theoHeThong(context: Context): Boolean =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        private fun caiDat(context: Context) =
            context.applicationContext.getSharedPreferences(TEP_CAI_DAT, Context.MODE_PRIVATE)

        /** Tron [mau] voi [den] theo ti le [ti] — thay cho Color.lerp cua Flutter. */
        fun pha(mau: Int, den: Int, ti: Float): Int = Color.rgb(
            (Color.red(mau) + (Color.red(den) - Color.red(mau)) * ti).roundToInt(),
            (Color.green(mau) + (Color.green(den) - Color.green(mau)) * ti).roundToInt(),
            (Color.blue(mau) + (Color.blue(den) - Color.blue(mau)) * ti).roundToInt(),
        )

        /** Cung mau nhung trong hon. */
        fun mauMo(mau: Int, doDuc: Float): Int =
            Color.argb((255 * doDuc).roundToInt(), Color.red(mau), Color.green(mau), Color.blue(mau))
    }
}
