# Spec hàng chip thư mục (sticky_view) — MIUI Notes 2.4.2.5

> Phụ lục của SPEC-GOC.md (mảng đợt 2 bị thiếu). Nguồn: `data_folder_list_layout.xml`,
> `data_folder_list_item.xml`, `drawable/data_folder_list_item_bg.xml`,
> `data_folder_list_item_select_bg.xml`, `FolderRecyclerView.java`,
> `component/datafolder/{DataFolderListAdapter,FolderItem,FolderItemEntity}.java`,
> `NoteListPageFragment.initFolderList`, `BaseNoteListPageFragment.queryFolderInfo`.

## Cấu trúc
- `folder_layout` (LinearLayout ngang, KHÔNG background): margin top `miuix_theme_margin_base` = **4dp**,
  bottom **8dp**, start/end `miuix_theme_margin_horizontal_common` = **12dp**.
- Bên trong: `FolderRecyclerView` ngang. fadingEdge khai 20dp nhưng code trả 0 → **không mờ mép**.
- Chiều cao hàng = chip 40dp + 4 + 8 = **52dp**.

## Chip (`data_folder_list_item.xml`)
- minHeight `pad_folder_list_item_height` = **40dp**; paddingStart/End `pad_folder_list_item_horizontal_padding` = **10dp**;
  bo góc `data_folder_list_item_bg_radius` = **12dp**.
- Chữ `common_folder_list_item_text_size` = **14sp**, mipro-medium, singleLine, maxWidth 227dp, includeFontPadding=false.
- Ghim (GONE mặc định): `ic_data_item_stick` 14dp, marginStart 3dp.

## Màu nền chip (HyperOS — bản người dùng nhớ)
| | Sáng | Tối |
|---|---|---|
| Thường: solid `data_folder_item_bg_color` | `#FFFFFF` | `#00000000` |
| Thường: stroke 1dp `data_folder_item_select_bg_stroke_color` | `#24FFFFFF` | `#33FFFFFF` |
| Chọn: gradient 315° `data_folder_item_select_bg_color_start/end`, KHÔNG stroke | `#0F000000` | `#24FFFFFF` |

Không phải HyperOS: thường solid `v12_theme_content_bg_color` #FFFFFF/#242424; chọn solid `data_folder_item_select_bg_color` #F0F0F0/#3E3E3E.

## Chữ
- Thường `data_folder_list_item_text_color_n`: **#99000000** / **#80FFFFFF**, mipro-regular.
- Chọn `data_folder_list_item_text_color_s`: **#000000** / **#CCFFFFFF**, mipro-demibold BOLD.

## Khoảng cách
- `SpacesItemDecoration(space = data_folder_list_item_space = 12dp)`: `outRect.right = 12dp` mọi chip trừ chip cuối.
- Chip đầu sát mép trái vùng (đã có margin 12dp của hàng).

## Nền hàng khi list cuộn dưới
- Hàng trong suốt; NestedHeaderLayout nền `homepage_primary_color` #F7F7F7 / #000000.
- HyperOS: list CHUI DƯỚI header + sticky, blur (`Mask.Pured_Regular`). Không HyperOS: list nằm dưới sticky, nền = homepage_primary_color.

## Dữ liệu chip
- Chip đầu: id -4097, `pc_all_folder_name` = **"Tất cả"** (mặc định chọn).
- Thư mục người dùng (ghim trước, rồi sắp theo tên).
- **"Chưa phân loại"** (`unclassfied_folder_name`) thêm **CUỐI**, chỉ khi có ghi chú chưa phân loại và danh sách không rỗng.
- Hàng ẨN khi số chip ≤ 1, hoặc đang ở trang riêng tư.
- HyperOS: KHÔNG có chip "+" / nút quản lý thư mục (bản non-HyperOS mới có CardView 40dp icon `ic_data_folder_list` tint #FFBB00).

## Hành vi bấm
Chặn bấm nhanh 400ms → tắt search mode → setFolderId → sau 500ms cuộn list về đầu → `notifyDataSetChanged` + `smoothScrollToPosition(chip)`. Vào action mode thì chặn mọi touch trên hàng.
