# Spec đợt 3 — chế độ chọn nhiều, chế độ tìm kiếm, popup menu (MIUI Notes 2.4.2.5)

> Phụ lục của SPEC-GOC.md.

## A. Chế độ CHỌN NHIỀU (action mode)

### Thanh trên (`miuix_appcompat_action_mode_title_item.xml`, `ActionBarContextView`)
- Cao `miuix_appcompat_action_bar_default_height` **56dp**; nền `action_bar_action_mode_bg` = `homepage_primary_color` **#F7F7F7 / #000000** (đặc, không blur); đệm ngang 20dp.
- Bố cục ngang: nút CHỮ **"Hủy"** (đầu) · tiêu đề giữa (weight 1, căn giữa) · nút CHỮ **"Chọn tất cả"** / **"Bỏ chọn tất cả"** (cuối). Không icon.
- Nút: 14sp (`miuix_font_size_body2`) sans-serif-medium, maxHeight 40dp, màu **#000000** / **#F2FFFFFF** (disabled #4D000000 / #66FFFFFF). Nhấn: Folme alpha 0.6.
- Tiêu đề: 20sp (`miuix_font_size_title3`) medium, **#000000 / #F2FFFFFF**; chuỗi "Đã chọn x" do app đặt (chưa chọn: `miuix_appcompat_select_item` "Chọn mục").

### Thanh menu ĐÁY (split action bar, `ResponsiveActionMenuView`)
- Cao `miuix_appcompat_bottom_menu_height` **64dp** (+ inset nav bar); nền `miuix_design_default_color_surface_variant` **#F7F7F7** / **#000000** (HyperOS thì blur).
- Đệm ngang 8dp; các nút **chia đều bề ngang**, gap `mMenuItemGap` **11dp**, hàng căn giữa.
- Mỗi nút (`Widget.ActionMode.ActionButton`): cột dọc — ICON 28dp trên + CHỮ dưới 11dp (đơn vị dp, code ép), maxLines 2, drawablePadding 0; padding top/bottom 18dp, ngang 3.27dp.
- Màu chữ: **#CC000000** (disabled #26000000, pressed #73000000) / **#CCFFFFFF** (disabled #26FFFFFF).
- Lệnh `notes_list_options.xml` theo thứ tự: Ẩn (`miui_ic_secret`) · Đổi tên · Ghim (`miui_ic_stick`) · Chuyển tới (`miui_ic_move`) · Tạo thư mục · Xóa (`miui_ic_delete`). Icon đều là glyph font (mất).

## B. Chế độ TÌM KIẾM (`SearchActionModeView`, `fragment_note_search.xml`)
- Bấm thanh tìm → action mode: panel tìm xuất hiện đúng chỗ thanh cũ rồi trượt về **sát đỉnh** (ngay dưới status bar); ActionBar (tiêu đề + menu) GONE, hàng thư mục INVISIBLE.
- Anim **400ms**, SpringInterpolator(damping 0.98, response 0.75); nội dung trượt theo, cuối cùng paddingTop nội dung = inset + 56dp.
- Panel: khung 56dp, paddingStart 12dp, top 4dp; pill 44dp bo 72dp; nền **#0F000000 / #24FFFFFF**; kính lúp 24dp/18dp alpha 0.3; chữ 17sp; hint **"Tìm kiếm ghi chú"** #4D000000 / #4DFFFFFF; chữ nhập #000000 / #F2FFFFFF; nút xoá (x) drawableEnd khi có chữ; tối đa 32 ký tự.
- Nút **"Hủy"**: 17sp sans-serif-medium, màu **#3482FF** (pressed #3078EB) / **#4788FF** (pressed #3885F8); marginStart 10dp, marginEnd 14dp, paddingH 10dp; cao 44dp; nền null (không ripple); trượt từ ngoài mép phải vào, pill co lại nhường chỗ.
- Lớp mờ `search_mask`: phủ TOÀN BỘ nội dung dưới panel, màu **#20000000 / #CC000000**, alpha 0→1 theo anim; có chữ → GONE, xoá hết chữ → VISIBLE; **bấm mask → thoát**.
- Kết quả: nền #F7F7F7 / #000000, dùng LẠI lưới/thẻ y hệt màn chính, padding top 4dp ngang 12dp; tìm mỗi lần gõ; **highlight** từ khoá bằng `ForegroundColorSpan` **#FF4848** (cả sáng lẫn tối) trên tiêu đề và đoạn trích, không đậm, không nền.
- Không kết quả: EmptyStateView giữa màn — icon `note_blank_icon` 68dp, marginBottom 8dp; chữ **"Không có ghi chú nào ở đây"** 14sp (#F7F7F7 sáng — trùng nền, nguyên văn / #99FFFFFF tối).
- Bàn phím tự bật khi vào; thoát bằng Hủy / bấm mask / Back → anim ngược 400ms, list cuộn về 0.

## C. Popup menu kiểu miuix (`ListPopup` + `miuix_appcompat_immersion_popup_menu_item.xml`)
- Bo góc `miuix_theme_radius_common` **16dp**; nền **#FFFFFF** / **#2C2C2C**; rộng min **200dp** max **288dp**; cao max 436dp; cách mép màn 16dp, cách status bar 8dp; elevation 11 + 20 = **31dp**, bóng #4D000000 alpha 0.3.
- Dòng: minHeight **50dp**; padding trái/phải **20dp**; dọc: dòng đầu top 20 / bottom 12, giữa 12/12, cuối 12/20, menu 1 dòng 20/20.
- Chữ 16sp (`miuix_font_size_headline2`) sans-serif-medium; màu **#000000** / **#F2FFFFFF** (checked #3482FF / #4788FF, disabled #4D000000 / #4DFFFFFF); maxLines 2; căn trái. Icon (nếu có) bên TRÁI, marginEnd 12dp — menu "..." màn soạn KHÔNG icon.
- Vạch ngăn nhóm: 8dp cao, vạch ~0.5dp inset 20dp, màu #1A000000 / #33FFFFFF (menu "..." không có).
- Vị trí: gravity END|TOP, mép phải trùng mép phải nút, **mép trên trùng mép trên nút** (phủ lên nút); không đủ chỗ thì lật lên.
- Anim vào: scale **0.8→1.0** pivot (100%, 0%) + alpha 0→1, **150ms**, decelerate 1.5; ra ngược 150ms. Haptic khi mở.
- Lệnh menu "..." màn soạn (`normal_note_edit_options.xml`): Nhắc nhở · Ẩn / Bỏ ẩn · Thêm vào màn hình chính · Chuyển tới · Xóa.
