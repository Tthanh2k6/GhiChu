# Hướng dẫn Thực chiến Đọc-Hiểu & Bảo trì Dự án `GhiChu` (`agent.md`)

Tài liệu này là **quy trình vận hành chuẩn (SOP)** bắt buộc dành cho mọi **AI Agent** và **Lập trình viên** khi tham gia đọc hiểu, sửa lỗi, nâng cấp tính năng hoặc tái cấu trúc dự án ứng dụng Android **`GhiChu`** (`com.ghichu`).

---

## 1. Mục đích & Nguyên tắc Hoạt động

Dự án `GhiChu` là ứng dụng Android viết bằng **100% Kotlin thuần SDK nền tảng Android** (`android.app.Activity`, `android.view.View`, `android.database.sqlite.SQLiteOpenHelper`, `Canvas`, `HttpURLConnection` — **không dùng AndroidX UI / Jetpack Compose / Room / Retrofit**), kết hợp hệ thống thiết kế riêng **v2 "Mực tím"**, đồng bộ đám mây 2 chiều qua **Supabase** và trợ lý thông minh **Google Gemini**.

Để không bao giờ phán đoán mò, không làm gãy kiến trúc siêu nhẹ (~3 MB APK) và không lãng phí thời gian quét lại toàn bộ 157 tệp từ đầu ở mỗi phiên làm việc mới, bạn phải sử dụng bộ tài liệu của dự án theo đúng vai trò sau:

1. **`agent.md` (tệp này — ĐỌC ĐẦU TIÊN ở mọi phiên mới)**: Cung cấp bảng định tuyến nhanh tới đúng vùng mã nguồn, danh sách các bất biến sống còn cấm vi phạm, quy trình đồng bộ tài liệu bắt buộc và các lệnh kiểm chứng trên Windows.
2. **`CODEBASE.md` (Bản đồ & Tham chiếu Kỹ thuật Toàn diện)**: Chứa kiến trúc tổng thể, sơ đồ luồng dữ liệu, cây thư mục 157 tệp, phân tích sâu từng class/hàm/thuật toán của toàn bộ 57 tệp `.kt` chính & 16 tệp unit test, lược đồ chi tiết 5 bảng SQLite `ghichu.db` (`PHIEN_BAN = 2`), 6 tệp `SharedPreferences`, giao thức REST Supabase/Gemini và bảng mã màu v2.
3. **`README.md` & `docs/SPEC-GOC*.md` (Tài liệu hiện có — CHỈ ĐỌC THAM KHẢO, KHÔNG TỰ Ý GHI ĐÈ)**: `README.md` tóm tắt cách cài đặt nhanh; các tệp `docs/SPEC-GOC*.md` lưu giữ đặc tả giai đoạn thiết kế v1 lấy cảm hứng từ Xiaomi Notes (lưu ý: giao diện hiện tại đã nâng cấp lên bản **v2 "Mực tím"**, nên khi có khác biệt giữa `SPEC-GOC.md` và mã nguồn thực tế thì **`CODEBASE.md` + mã nguồn `.kt` hiện hành là chân lý chuẩn**).

---

## 2. Quy trình BẮT BUỘC Trước Khi Sửa Code (Read-First Protocol)

Trước khi chạm vào bất kỳ dòng mã nào, **BẮT BUỘC** thực hiện tuần tự 4 bước sau:

1. **Bước 1 — Tra cứu Bảng Định tuyến Nhanh bên dưới**: Xác định đúng các mục cần đọc trong `CODEBASE.md` tương ứng với yêu cầu của người dùng.
2. **Bước 2 — Đọc kỹ các mục liên quan trong `CODEBASE.md`**: Nắm rõ lược đồ bảng/cột, khóa `SharedPreferences`, luồng gọi hàm và các hiệu ứng phụ (ví dụ `KhoDuLieu.baoDoi()`, `onPause()` tự lưu, lọc `rieng_tu = 0`).
3. **Bước 3 — Đọc trực tiếp tệp nguồn `.kt` (và `.xml` nếu sửa AppWidget/Manifest) hoàn chỉnh**: Luôn dùng công cụ đọc tệp xem toàn bộ ngữ cảnh thực tế xung quanh vùng định sửa, đối chiếu cả tệp kiểm thử đơn vị trong `app/src/test/kotlin/com/ghichu/`.
4. **Bước 4 — Đối chiếu Mục 3 (Các Bất biến Kỹ thuật)**: Đảm bảo giải pháp thiết kế không vi phạm bất kỳ quy tắc an toàn nào của dự án.

### Bảng Định tuyến Nhanh (Quick Routing Table — Riêng cho `GhiChu`)

| Khi cần làm việc với mảng chức năng... | Đọc mục nào trong `CODEBASE.md` | Mở chính xác những tệp nguồn & tệp test nào |
| :--- | :--- | :--- |
| **Màn hình chính, Thanh đỉnh `56dp`, Tìm kiếm, Chip thư mục, Chọn nhiều thẻ, Thanh neo đáy `dock` ("thỏi mực")** | Mục `2.2` (Luồng 1, 2), Mục `4.7`, Mục `6.1–6.2` | `ManHinhChinhActivity.kt`, `LuoiSoLe.kt`, `TheGhiChu.kt`, `ManPhu.kt` |
| **Trình soạn thảo ghi chú, Định dạng `Aa`, Ô tick `☐/☑`, Ghi âm, Đính kèm ảnh, Theme nền ghi chú** | Mục `2.2` (Luồng 2), Mục `4.4`, `4.6`, Mục `5.3`, `6.2.B` | `SuaGhiChuActivity.kt`, `SoanThao.kt`, `DinhDang.kt`, `NoiDungGhiChu.kt`, `BoHoanTac.kt`, `Theme.kt`, `MauGhiChu.kt`, `KhoAnh.kt`, `KhoGhiAm.kt` + Test: `DinhDangTest.kt`, `NoiDungGhiChuTest.kt` |
| **Sơ đồ tư duy nhúng trong ghi chú (`Mind Map`)** | Mục `4.6.7`, `4.6.8`, Mục `5.3` | `BanDoTuDuy.kt`, `BanDoView.kt`, `BanDoActivity.kt`, `SuaGhiChuActivity.kt` (`TheBanDo`) + Test: `BanDoTuDuyTest.kt` |
| **Xuất ghi chú (`.txt`, `.md`, `.pdf`) & Chia sẻ tệp qua `ContentProvider`** | Mục `4.4.3`, `4.6.9`, Mục `5.3` | `XuatGhiChu.kt`, `TepChiaSe.kt`, `Khoi.kt`, `AndroidManifest.xml` |
| **Lịch trình Tuần (`24h x 7`) / Tháng, Sự kiện lặp lại, Dải chip mẫu tạo nhanh** | Mục `2.2` (Luồng 3), Mục `4.2.1`, `4.2.4–4.2.5`, `4.8` | `ManLichTrinh.kt`, `TuanView.kt`, `LichThangView.kt`, `SuaSuKien.kt`, `SuKien.kt`, `HangMau.kt`, `LichTrinhActivity.kt`, `KhoDuLieu.kt` + Test: `LichLapLaiTest.kt`, `HangMauTest.kt` |
| **Trợ lý AI Gemini, Lịch sử hội thoại `hoi_thoai`, Thẻ xem trước & Áp dụng lệnh AI** | Mục `2.2` (Luồng 5), Mục `4.10`, Mục `5.1` (Bảng 5), `5.4.B` | `TroLyAiActivity.kt`, `Gemini.kt`, `AiLoiNhac.kt`, `AiDeXuat.kt`, `AiApDung.kt` + Test: `GeminiTest.kt`, `AiLoiNhacTest.kt`, `AiDocDeXuatTest.kt`, `AiJsonNullTest.kt`, `AiBangSuKienTest.kt`, `AiApDungTest.kt` |
| **Cơ sở dữ liệu SQLite `ghichu.db`, Tìm kiếm tiếng Việt không dấu, Thư mục & Thùng rác** | Mục `4.2.1`, `4.2.2`, Mục `5.1` | `KhoDuLieu.kt`, `KhoGhiChu.kt`, `GhiChu.kt`, `Ung.kt` + Test: `GhiChuTest.kt` |
| **Khu vực Riêng tư, Mã PIN 4–8 số & Mã hóa `AES-256-GCM`** | Mục `2.2` (Luồng 4), Mục `4.3`, Mục `5.2`, `8.2` | `KhoaRiengTu.kt`, `MaHoa.kt`, `ManHinhChinhActivity.kt` (`onStop`), `SuaGhiChuActivity.kt` (`doiRiengTu`) + Test: `MaHoaTest.kt` |
| **Đồng bộ Đám mây 2 chiều Supabase (`sync_items`) & Tự động đồng bộ ngầm** | Mục `2.2` (Luồng 6), Mục `4.11.1`, `4.11.2`, Mục `5.2`, `5.4.A` | `DongBo.kt`, `DongBoTuDong.kt`, `DongBoActivity.kt`, `KhoGhiChu.kt`, `KhoDuLieu.kt` |
| **Sao lưu & Khôi phục toàn bộ ra tệp `.zip`** | Mục `2.2` (Luồng 7), Mục `4.11.3`, Mục `5.3` | `SaoLuu.kt`, `SaoLuuActivity.kt` |
| **Báo thức Nhắc nhở (`AlarmManager` + `NotificationChannel`)** | Mục `2.2` (Luồng 8), Mục `4.9.1` | `NhacNho.kt`, `KhoDuLieu.kt` (`suKienSapNhac`), `KhoGhiChu.kt` (`dsSapNhac`), `AndroidManifest.xml` + Test: `NhacNhoTest.kt` |
| **Quét mã QR bằng Camera2 + Google ML Kit** | Mục `4.9.2`, Mục `5.1` (Bảng 4) | `QuetQr.kt`, `QuetQrActivity.kt`, `KhoDuLieu.kt` (`luuQuetQr`) + Test: `QuetQrTest.kt` |
| **3 Tiện ích Màn hình Chính (`AppWidgets`)** | Mục `4.12`, `4.14`, Mục `5.2` | `GhiChuWidget.kt`, `GhiChuMotWidget.kt`, `ChonGhiChuWidgetActivity.kt`, `LichTrinhWidget.kt`, `LichTrinhWidgetService.kt`, `res/layout/ghichu_widget_*.xml`, `res/xml/ghichu_widget_*_info.xml`, `res/drawable/ghichu_kinh_*.xml` |
| **Bảng màu Sáng/Tối v2 "Mực tím", Phông chữ `Be Vietnam Pro`, Hộp thoại `HopThoai`, Menu nổi `MenuNoi`** | Mục `4.5`, Mục `6.2`, `6.3` | `BangMau.kt`, `Chu.kt`, `HopThoai.kt`, `MenuNoi.kt`, `CaiDatGiaoDienActivity.kt`, `res/values/themes.xml`, `res/drawable/ic_*.xml` |
| **Cấu hình Gradle, SDK, Minify/R8, Quyền Android** | Mục `1.2`, `4.14`, `7.1–7.3`, `8.1` | `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`, `AndroidManifest.xml` |

---

## 3. Các Bất biến Kỹ thuật & Quy tắc An toàn Riêng của Dự án (Project-Specific Guardrails)

Khi viết hoặc sửa bất kỳ đoạn mã nào trong `GhiChu`, bạn **BẮT BUỘC TUÂN THỦ 12 QUY TẮC SỐNG CÒN** sau:

1. **Cấm thêm plugin `org.jetbrains.kotlin.android` vào `app/build.gradle.kts`**:
   - Dự án dùng **AGP `9.0.0`** có Built-in Kotlin sẵn. Khai báo thêm plugin Kotlin cũ sẽ làm hỏng build ngay lập tức.
2. **Cấm kéo thêm thư viện AndroidX UI / Material / Compose / Thư viện nặng**:
   - Mọi `Activity` kế thừa trực tiếp `android.app.Activity`. Mọi hộp thoại dùng `com.ghichu.HopThoai` (tuyệt đối không dùng `AlertDialog` mặc định làm lệch thiết kế bo góc `24dp`). Mọi menu thả xuống dùng `com.ghichu.MenuNoi`.
3. **Luôn gọi `BangMau.apDungChoActivity(this)` trước `setContentView(...)`**:
   - Mọi màn hình mới đều phải lấy bảng màu động từ `val mau = BangMau.cua(this)` và dùng phông chữ từ `Chu.thuong/vua/hoiDam/dat(this)`, không gán cứng mã màu hex trực tiếp trong code giao diện (trừ định nghĩa bảng màu gốc trong `BangMau.kt`, `MauGhiChu.kt`, `Theme.kt`, `SuKien.kt`).
4. **Quy tắc Luồng Giao diện (UI Thread) vs Luồng Nền (Background Thread)**:
   - Tuyệt đối không thực hiện truy vấn mạng (`Gemini`, `DongBo`), nén/giải nén ảnh lớn hay tệp `.zip` (`KhoAnh`, `SaoLuu`) trên Main Thread. Mọi cập nhật `View` từ luồng nền phải bọc qua `runOnUiThread { if (!isFinishing && !isDestroyed) { ... } }`.
5. **Quy tắc Giao dịch SQLite & `KhoDuLieu.baoDoi()`**:
   - Luôn đóng `db.endTransaction()` hoàn tất rồi mới được gọi `kho.baoDoi()`. Không bao giờ gọi `baoDoi()` khi đang giữ transaction mở vì `baoDoi()` kích hoạt `NhacNho.datLai()` và cả 3 AppWidget truy vấn ngược lại DB.
   - Khi nâng cấp lược đồ DB, phải tăng `PHIEN_BAN` trong `KhoDuLieu.kt` và viết nhánh `if (cu < PHIEN_BAN_MOI)` trong `onUpgrade` bằng lệnh `ALTER TABLE` / `CREATE TABLE IF NOT EXISTS` bảo toàn 100% dữ liệu người dùng cũ.
6. **Quy tắc Vàng khi Đọc JSON trên Android (`JSONObject.isNull`)**:
   - Trên Android, `jsonObj.optString("ten_truong")` khi gặp `{"ten_truong": null}` sẽ trả về chuỗi `"null"` thay vì `""`! Bắt buộc kiểm tra `if (!obj.has(k) || obj.isNull(k)) null else obj.optString(k)` ở mọi nơi phân tích JSON từ Gemini hoặc Supabase.
7. **Quy tắc Cô lập Tuyệt đối Ghi chú Riêng tư (`rieng_tu = 1`)**:
   - Ghi chú có `rieng_tu = 1` **KHÔNG BAO GIỜ** được: (a) đẩy lên đám mây Supabase trong `DongBo.kt`, (b) chèn vào lời nhắc gửi Gemini trong `AiLoiNhac.kt`, (c) hiển thị trên bất kỳ AppWidget nào hoặc lộ nội dung trên thông báo màn hình khóa (`NhacNho.kt`).
   - Phải giữ nguyên cơ chế tự động gọi `KhoaRiengTu.khoaLai()` trong `ManHinhChinhActivity.onStop()` khi thoát ra nền.
8. **Quy tắc Trình Soạn thảo `SuaGhiChuActivity` (Chống Lưu Thừa & Chống Hồi sinh Ghi chú Đã Xóa)**:
   - `SuaGhiChuActivity.onPause()` tự động gọi `luu()`. Khi thực hiện thao tác xóa ghi chú trong màn soạn, bắt buộc gán `id = 0L; goc = null` trước khi gọi `finish()` để `onPause()` không ghi sống lại ghi chú vừa xóa!
   - Bảo toàn hàm so sánh `daDoi(cu, moi)` trong `luu()` để việc mở ghi chú ra đọc không làm nhảy `ngay_sua` và không kích hoạt đồng bộ giả.
   - `MoChuTick` (gạch ngang chữ của dòng `☑ `) chỉ là `CharacterStyle` ở tầng hiển thị — không được biến nó thành thẻ `<del>` khi tuần tự hóa xuống `noi_dung`.
9. **Quy tắc Bố cục XML cho AppWidget (`RemoteViews`)**:
   - Tuyệt đối **không dùng thẻ `<View>` trần** trong `res/layout/ghichu_widget_*.xml` (Launcher sẽ báo lỗi *"Không thể tải tiện ích"*). Mọi vạch chia `1dp` phải dùng `<TextView android:layout_width="match_parent" android:layout_height="1dp" ... />`.
10. **Tương thích Chuỗi Đồng bộ Đám mây & Bảng Màu**:
    - Không đổi tên bất kỳ khóa JSON nào trong `DongBo.kt` (`sync_items`: `uid`, `title`, `content`, `folder`, `pinned`, `bg`, `hidden`, `startAt`, `endAt`, `allDay`, `color`, `repeat`, `remindMinutes`, `repeatUntil`, `diaDiem`, ...).
    - Không đảo thứ tự hoặc chèn vào giữa `MauGhiChu.BANG` (`0..7`), `Theme.DANH_SACH` (`8..12`) hay `MauSuKien.BANG` (`0..7`).
11. **Xử lý Tiếng Việt UTF-8 & Tìm kiếm Không dấu**:
    - Mọi tệp nguồn `.kt`, `.xml`, `.md` phải lưu chuẩn **UTF-8 (không BOM)**. Khi chạy lệnh Python hỗ trợ trên Windows PowerShell, luôn truyền cờ `python -X utf8`. Khi so khớp tìm kiếm văn bản tiếng Việt, luôn dùng `KhoGhiChu.boDau(...)` (xử lý đủ cả dấu tổ hợp NFD lẫn `đ/Đ`).
12. **Không sửa đổi `README.md` / `docs/SPEC-GOC*.md` khi không được yêu cầu & Cấm chạy `tools/*.py`**:
    - Các kịch bản trong `tools/` là công cụ lịch sử thời bản v1; chạy lại chúng sẽ phá hỏng bộ icon nét tròn v2 và cấu trúc màn hình hiện tại.

---

## 4. Quy trình BẮT BUỘC Cập nhật Tài liệu Sau Mỗi Lần Sửa Code (Post-Edit Sync Protocol)

> **NGUYÊN TẮC BẤT DI BẤT DỊCH**:
> **Mọi thay đổi đối với mã nguồn, cấu hình, giao diện hay lược đồ dữ liệu của dự án đều BẮT BUỘC phải đi kèm việc cập nhật `CODEBASE.md` (và `agent.md` nếu ảnh hưởng tới định tuyến hoặc quy tắc an toàn) ngay trong CÙNG MỘT PHIÊN LÀM VIỆC, trước khi báo cáo hoàn thành cho người dùng!** Không bao giờ để tài liệu trễ nhịp so với code thực tế.

### 4.1. Ma trận Đồng bộ Tài liệu (Documentation Sync Matrix)

Sau khi sửa code xong, hãy tra bảng dưới đây để biết **chính xác mục nào** phải mở ra cập nhật ngay:

| Loại thay đổi vừa thực hiện trong dự án | Các mục PHẢI cập nhật trong `CODEBASE.md` | Các mục PHẢI cập nhật trong `agent.md` |
| :--- | :--- | :--- |
| **Thêm mới / Xóa / Đổi tên / Tách tệp `.kt`, `.xml`, test** | - **Mục 3.1 & 3.2**: Cây thư mục + Bảng kê khai đầy đủ tệp (đường dẫn, số dòng mới, vai trò, thành phần xuất ra).<br>- **Mục 4**: Thêm mới hoặc xóa/cập nhật tiểu mục phân tích chi tiết của tệp đó.<br>- **Mục 9**: Ghi thêm dòng nhật ký cập nhật. | - **Mục 2 (Bảng Định tuyến Nhanh)**: Bổ sung tên tệp mới hoặc gỡ tên tệp đã xóa ở hàng chức năng tương ứng. |
| **Thêm / Sửa chữ ký hàm, class, thuật toán, hằng số cấu hình, luồng nghiệp vụ** | - **Mục 2.2**: Cập nhật luồng nghiệp vụ nếu thứ tự bước xử lý thay đổi.<br>- **Mục 4**: Cập nhật chữ ký hàm, tham số, biến trạng thái và giải thích thuật toán của tệp tương ứng.<br>- **Mục 7.2**: Cập nhật bảng hằng số cấu hình nếu đổi ngưỡng thời gian/kích thước.<br>- **Mục 9**: Ghi nhật ký cập nhật. | - Không cần sửa nếu không sinh ra quy tắc an toàn mới; nếu có bất biến mới → cập nhật **Mục 3**. |
| **Thay đổi bảng/cột SQLite `ghichu.db`, khóa `SharedPreferences`, định dạng thẻ khối / `.zip` hoặc API Supabase / Gemini** | - **Mục 4.2 / 4.10 / 4.11**: Cập nhật phân tích module lưu trữ/đồng bộ/AI.<br>- **Mục 5.1**: Cập nhật bảng cột SQLite, chỉ mục và logic `onUpgrade` (`PHIEN_BAN`).<br>- **Mục 5.2 / 5.3 / 5.4**: Cập nhật bảng khóa `SharedPreferences`, định dạng tệp hoặc mẫu payload JSON.<br>- **Mục 9**: Ghi nhật ký cập nhật. | - **Mục 2** (nếu thêm nhóm dữ liệu mới) & **Mục 3** (nếu có ràng buộc migration/tương thích ngược mới). |
| **Thay đổi màn hình, hộp thoại, cử chỉ điều hướng, AppWidget hoặc mã màu / font chữ** | - **Mục 4.5 / 4.6 / 4.7 / 4.8 / 4.12**: Cập nhật chi tiết giao diện ở tệp tương ứng.<br>- **Mục 6.1**: Cập nhật sơ đồ chuyển màn hình Mermaid.<br>- **Mục 6.2 & 6.3**: Cập nhật bảng mã màu hex, theme hoặc cỡ chữ.<br>- **Mục 9**: Ghi nhật ký cập nhật. | - **Mục 2**: Cập nhật Bảng Định tuyến Nhanh nếu thêm màn hình/hộp thoại mới. |
| **Thay đổi phiên bản AGP / Gradle / SDK / Dependency hoặc thêm lệnh build/test mới** | - **Mục 1.2**: Cập nhật Bảng Tech Stack & phiên bản.<br>- **Mục 7.1 & 7.3**: Cập nhật yêu cầu công cụ và câu lệnh CLI.<br>- **Mục 9**: Ghi nhật ký cập nhật. | - **Mục 3** (nếu thay đổi ràng buộc build) & **Mục 5** (cập nhật lệnh kiểm tra nhanh). |
| **Phát hiện bug tinh vi, giới hạn nền tảng Android mới hoặc nợ kỹ thuật mới** | - **Mục 8.1 / 8.2 / 8.3**: Bổ sung vào danh sách Bất biến kỹ thuật, Bảo mật hoặc Điểm dễ lỗi.<br>- **Mục 9**: Ghi nhật ký cập nhật. | - **Mục 3**: Đúc kết thành quy tắc phòng tránh trực tiếp cho các phiên sau. |

### 4.2. Quy chuẩn Viết Khi Cập nhật Tài liệu
1. **Dùng `edit_file` chính xác vào đúng mục cần sửa**, giữ nguyên độ chi tiết chuyên sâu như bản gốc, **tuyệt đối không tóm tắt lược bỏ làm mất thông tin cũ vẫn còn hiệu lực**.
2. **Ngôn ngữ & Định dạng**: Viết bằng tiếng Việt kỹ thuật mạch lạc; bọc chính xác mọi tên hàm, tên class, tên biến, tên cột SQL, khóa `SharedPreferences`, đường dẫn tệp và câu lệnh CLI trong dấu backtick `` `...` ``.
3. **An toàn cú pháp Markdown & Mermaid**:
   - Với ký hiệu tiền tệ đô-la trong văn bản thường, luôn escape `\$` hoặc đặt trong backtick để không kích hoạt nhầm khối toán học LaTeX.
   - Trong sơ đồ `mermaid`, mọi nhãn node chứa dấu ngoặc tròn `(...)`, dấu hai chấm hay ký tự đặc biệt **bắt buộc bọc trong dấu ngoặc kép** (ví dụ `Main["ManHinhChinhActivity (Vỏ 2 Tab)"]`), và không dùng thẻ HTML như `<br/>`.
4. **Luôn ghi thêm 1 dòng vào Mục 9 (`Nhật ký Cập nhật Tài liệu`) của `CODEBASE.md`** nêu rõ ngày (`YYYY-MM-DD`), các tệp/mục vừa tác động và tóm tắt nội dung thay đổi.

---

## 5. Lệnh Kiểm tra & Xác minh Nhanh (Verification Commands & Final Checklist)

### 5.1. Bộ Lệnh Xác minh Sau Khi Sửa Code (Chạy tại `d:\Project\GhiChu`)

```powershell
# 1. Chạy toàn bộ 16 bộ kiểm thử đơn vị JVM (BẮT BUỘC XANH 100%)
.\gradlew.bat testDebugUnitTest

# 2. Biên dịch kiểm tra toàn bộ mã nguồn Kotlin, tài nguyên XML và Manifest
.\gradlew.bat assembleDebug

# 3. Kiểm tra trạng thái Git để chắc chắn không sửa nhầm tệp ngoài phạm vi
git status --short
```

*(Nếu sửa logic phân tích dữ liệu, mã hóa, lịch lặp, định dạng văn bản hoặc AI: hãy viết bổ sung ca kiểm thử đơn vị mới vào `app/src/test/kotlin/com/ghichu/` và chạy lại `.\gradlew.bat testDebugUnitTest`!).*

### 5.2. Checklist Hoàn tất Trước Khi Trả lời Người dùng

Trước khi kết thúc phiên làm việc, hãy tự đánh dấu kiểm tra đủ **7 mục** sau:

- [ ] **1. Đúng yêu cầu & Bảo toàn tính năng cũ**: Đã giải quyết trọn vẹn yêu cầu mà không làm hỏng các luồng hiện có (tự lưu `onPause`, khóa `riengTu`, đồng bộ `DongBoTuDong`, cập nhật 3 AppWidget).
- [ ] **2. Tuân thủ 12 Quy tắc An toàn (Mục 3)**: Không thêm AndroidX/plugin thừa, đã kiểm tra `JSONObject.isNull`, không gọi `baoDoi()` giữa transaction, không dùng `<View>` trần trong AppWidget XML.
- [ ] **3. Build & Unit Tests Xanh 100%**: Đã chạy `.\gradlew.bat testDebugUnitTest` và `.\gradlew.bat assembleDebug` thành công không lỗi biên dịch.
- [ ] **4. Đã bổ sung Unit Test (nếu thêm/sửa thuật toán)**: Các hàm xử lý dữ liệu mới đã có test khóa hành vi trong `app/src/test/kotlin/com/ghichu/`.
- [ ] **5. Đã đồng bộ `CODEBASE.md` (và `agent.md` nếu cần)**: Đã cập nhật đúng các mục theo Ma trận Đồng bộ (Mục 4.1), cập nhật số dòng tệp nếu thay đổi đáng kể và ghi thêm dòng mới vào **Mục 9 — Nhật ký Cập nhật Tài liệu**.
- [ ] **6. Kiểm tra cú pháp Tài liệu**: Không lỗi khối `mermaid`, không dính `$` kích hoạt nhầm LaTeX, mã hóa UTF-8 tiếng Việt chuẩn xác.
- [ ] **7. Kiểm tra `git status --short`**: Không để sót tệp rác tạm thời và không vô tình sửa đè các tệp ngoài phạm vi yêu cầu.
