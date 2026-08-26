# Spec hộp thoại miuix (AlertDialog) — MIUI Notes 2.4.2.5

> Phụ lục của SPEC-GOC.md. Nguồn: `miuix_appcompat_alert_dialog*.xml`, `AlertController.java`,
> `DialogPanelBehaviorImpl.java`, `DialogButtonPanel.java`, `PhoneDialogAnim.java`,
> `dialog_edit_text.xml`, `EditTextDialog.java`, `miuix_appcompat_select_dialog*.xml`,
> `miuix_appcompat_action_sheet_alert_content.xml`, `common_folder_dialog_item.xml`.

## Vị trí, kích thước, nền
- Neo **ĐÁY** màn hình (gravity 81) trên điện thoại; giữa chỉ trên tablet.
- Lề dưới `miuix_appcompat_dialog_ime_margin` **12dp + navigation bar**. Lề trên ≥ 12dp.
- Bề rộng: < 360dp → match − 2×8dp; 360–404dp → match − 2×`miuix_appcompat_dialog_width_margin` **12dp**; > 404dp → cố định `miuix_appcompat_dialog_max_width` **368dp**.
- Cao tối đa 70% màn (dọc).
- Bo góc `miuix_appcompat_dialog_bg_corner_radius` = `miuix_theme_radius_big` = **36dp**.
- Nền `miuix_default_color_surface_high`: **#FFFFFF** / **#242424**. Đệm dọc panel **24dp**.
- Lớp mờ `DimToken`: **0.3** sáng / **0.6** tối; fade 300ms decelerate(1.5).
- Hoạt ảnh: panel trượt từ dưới lên, Folme spring damping 0.82 response 0.3 (fallback `dialog_anim_slide_in_bottom` 300ms decelerate 1.5; đóng 150–200ms).

## Tiêu đề
18sp (`miuix_font_size_title4`) sans-serif-medium, **căn giữa**, maxLines 4; màu **#000000** / **#F2FFFFFF**; đệm ngang 36dp, dưới 16dp.

## Nội dung
16sp (`miuix_font_size_body1`), lineSpacing 1.05; màu **#CC000000** / **#CCFFFFFF**; lề ngang 28dp, dưới 16dp; 1 dòng căn giữa, nhiều dòng căn trái.

## Nút
- Nhóm nút: đệm ngang 24dp, cách nội dung 8dp. ≤ 2 nút xếp NGANG weight 1, cách 12dp, thứ tự phụ → chính. ≥ 3 nút xếp DỌC.
- Cao `miuix_appcompat_button_height` **50dp**; bo góc **16dp** (MIUI ≥ 15) hoặc capsule 36dp; chữ 17sp medium; singleLine.
- THƯỜNG: nền **#0F000000** / **#24FFFFFF**; chữ **#E6000000** / **#CCFFFFFF**. Nhấn: phủ đen 0.12 / trắng 0.14.
- CHÍNH (positive): nền **#3482FF** / **#4788FF** (xanh dương — KHÔNG vàng); chữ **#FFFFFF** / **#E6FFFFFF**.
- NGUY HIỂM: nền thường, chữ **#FA382E** / **#FA4238**.

## Ô nhập (dat ten thư mục)
Lề ngang 24dp; minHeight **54dp**; đệm ngang 16dp; 17sp medium; chữ #000000 / #F2FFFFFF; hint #4D000000 / #4DFFFFFF; nền bo góc **16dp** fill #FFFFFF / #4C4C4C; khi focus viền **2dp** xanh #3482FF / #4788FF (fade 200ms); bàn phím tự bật sau 100ms; OK disabled khi rỗng.

## List dialog / ActionSheet (setItems)
- Hàng thường: minHeight **56dp**, đệm ngang 28dp, 17sp medium, màu #000000 / #F2FFFFFF, không tick.
- HyperOS > 2 với setItems → **ActionSheet**: nền #FFFFFF / #242424 radius 36dp; message 14sp #99000000 / #80FFFFFF; mục căn giữa 17sp, minHeight 56dp, đệm dọc 16.5dp ngang 24dp; nút Huỷ tách riêng minHeight 50dp, marginTop 16dp, marginBottom 24dp, marginH 24dp, style nút thường.
- Single choice: tick 28dp màu xanh primary; chữ chọn #3482FF / #4788FF.

## Chọn thư mục đích (Activity toàn màn, không phải AlertDialog)
Nền #F7F7F7 / #000000; action bar back + title 20sp medium #CC000000 / #CCFFFFFF; list lề ngang 18dp, clip radius 16dp, gap hàng 6dp; hàng 56dp nền #FFFFFF / #24FFFFFF radius 16dp, đệm trái 16dp phải 32dp; tick 24dp **#FFBB0F** (invisible khi không chọn); tên 17sp #CC000000 / #F2FFFFFF (chọn → medium); số lượng 12sp #99000000 / #80FFFFFF; nhấn: tint #1F000000 / #24FFFFFF + scale 0.93. Chân "Thư mục mới": icon `+` 28dp #FFBB0F, chữ 12sp.
