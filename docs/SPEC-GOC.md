# So do thiet ke trich tu app Ghi chu Xiaomi 2.4.2.5 (dich nguoc)

> Nguon: D:/Project/Note/reference/miui-notes. Trich bang mot dot fan-out 7 agent + 1 agent ra soat.


---

# SPEC — Đầu màn hình chính (MIUI Notes 2.4.2.5)

## 4. Nền cả trang
- `homepage_primary_color` = **#FFF7F7F7** (sáng, values/colors.xml:455) / **#000000** (tối, values-night/colors.xml:194).
- Dùng cho: `list_root_view`, `NestedHeaderLayout` (new_phone_note_list_fragment.xml) và `android:windowBackground` của theme `NavigatorContentChildTheme` (styles.xml:4607).

## 1. Tiêu đề lớn "Ghi chú"
Chuỗi: `app_name` = "Ghi chú" (values-vi/strings.xml:182). Title do miuix ActionBar vẽ (không nằm trong layout fragment).

**Trạng thái MỞ RỘNG (đầu trang):**
- Cỡ chữ: `miuix_appcompat_expand_title_text_size` → `miuix_font_size_title1` = **32sp**
- Font: `miuix_font_style_misans_regular_font_family` = **sans-serif** (regular)
- Màu: `?actionBarTitleColor` → selector `miuix_appcompat_action_bar_title_text_light/dark` → sáng `miuix_color_black_level1` = **#FF000000**, tối `miuix_color_white_level1_1` = **#F2FFFFFF**
- Căn: trái (`ExpandTitleStyle`: textAlignment=textStart)
- Lề (ActionBarView.java:662,672): container title đệm ngang `miuix_appcompat_action_bar_title_horizontal_padding` = **26dp**, cộng padding của cả ActionBarView `actionBarPaddingStart/End` → `miuix_appcompat_action_bar_horizontal_padding_start/end` = **20dp** ⇒ chữ cách mép trái ≈ **46dp**. Trên: `miuix_appcompat_action_bar_title_top_padding` = **4dp**; dưới: `miuix_appcompat_action_bar_title_bottom_padding` = **10dp**.

**Trạng thái THU GỌN (khi cuộn lên):**
- Cỡ chữ: `miuix_appcompat_title_text_size` → `miuix_font_size_title3` = **20sp**, font **sans-serif-medium**, cùng màu trên, **căn giữa** (foregroundGravity=center)
- Cao thanh thu gọn: `miuix_appcompat_action_bar_default_height` = **56dp**; đệm dọc title `miuix_appcompat_action_bar_title_collapse_padding_vertical` = **14.5dp**

**Hành vi cuộn:** fragment gọi `getActionBar().registerCoordinatedScrollView(mNestedHeaderLayout)` (NoteListPageFragment.java:643). Hai container title **cross-fade theo alpha**: cuộn lên → title 32sp trái mờ dần + vùng title co lại, title 20sp giữa hiện dần; cuộn về đầu thì ngược lại. Khi header cuộn qua, `ActionBarContainer.updateBackground()` bật nền/blur thanh bar (`bgBlurOptions=splitActionBar`).

## 2. Thanh tìm kiếm (include `miuix_appcompat_search_stub`, id `search_bar`)
- Hàng chứa (`search_mode_stub`): cao `miuix_appcompat_search_view_default_height` = **56dp**; nền chỉ là layer-list tạo đệm (`miuix_appcompat_search_mode_bg_light/dark`): ngang `miuix_appcompat_search_mode_bg_padding` → `miuix_theme_padding_horizontal_common` = **12dp**, trên `miuix_appcompat_search_mode_bg_padding_top` = **4dp**, dưới **0dp** ⇒ viên thuốc cách 2 mép **12dp**, cách trên ~8dp, dưới ~4dp (con 44dp căn giữa dọc vùng 52dp).
- Viên thuốc (`inputArea`): cao `miuix_appcompat_searchbar_bg_height` = **44dp**; bo góc `miuix_appcompat_search_mode_bg_radius` = **72dp** (pill tròn hẳn); nền sáng `miuix_appcompat_edit_text_search_bg_color_light` → `miuix_color_black_level9` = **#0F000000**, tối → `miuix_color_white_level8` = **#24FFFFFF**.
- Kính lúp: nằm ĐẦU (trái) pill; SymbolDrawable `miuix_appcompat_edit_text_search_light/dark`: khung **24×24dp** (`miuix_appcompat_edit_text_search_icon_width/height`), nét glyph **18dp** (symbolIconSize), alpha **0.3**, tint **#000000** (sáng)/**#FFFFFF** (tối); paddingStart `miuix_appcompat_edit_text_search_bg_icon_padding_start` = **12dp**, paddingEnd `..._end` = **8dp**.
- Chữ gợi ý: đặt bằng code (NoteListPageFragment.java:678) `search_hint` = **"Tìm kiếm ghi chú"** (values-vi:1138); style `Miuix.AppCompat.TextAppearance.Widget.EditText.Search`: cỡ `miuix_appcompat_search_edit_text_size` → `miuix_font_size_headline1` = **17sp**, font **sans-serif-medium**, màu hint `miuix_appcompat_edit_text_search_hint_color_light` → `miuix_color_black_level6` = **#4D000000** / tối `miuix_color_white_level6` = **#4DFFFFFF**; paddingEnd `miuix_appcompat_search_edit_padding_end` = **16dp**.
- Cả thanh là một NÚT (setOnClickListener mở chế độ tìm kiếm) — TextView không nhập trực tiếp.

**Hành vi cuộn — NestedHeaderLayout (mô tả để tái hiện bằng view thường):**
- `headerView=@id/search_bar`: thanh tìm kiếm là "header co giãn" — cuộn danh sách lên thì nó TRƯỢT LÊN biến mất (danh sách chiếm chỗ), kéo xuống ở đầu danh sách thì trượt về.
- `headerClose="true"` (NestedScrollingLayout.java:116,261): lúc khởi tạo header ở trạng thái ĐÓNG — thanh tìm kiếm ẨN, phải kéo danh sách xuống mới lộ ra.
- `stickyView=@id/sticky_view` (hàng tab thư mục, data_folder_list_layout): GHIM cố định trên cùng, danh sách cuộn bên dưới nó.
- `triggerView=@id/micloud_state_view`: vùng trạng thái đồng bộ MiCloud nằm trên header, chỉ hiện khi KÉO QUÁ ĐÀ xuống.
- `rangeOffset=0px`. `SpringBackLayout` bọc RecyclerView = hiệu ứng lò xo/nảy khi kéo quá 2 đầu (overscroll đàn hồi, chính lực kéo này mở header/trigger).

## 3. Hàng icon trên cùng (menu)
- Menu màn danh sách: `NotesListActivity.getNavigationOptionMenu()` → **res/menu/navigation_immersion.xml** — CHỈ 1 nút: `action_settings` (Cài đặt), `showAsAction=always`, nằm cuối (góc phải) thanh action bar.
- Icon: `@drawable/miui_ic_settings` — alias values/drawables.xml:32 → `miuix_action_ic_settings_light` (sáng), values-night/drawables.xml:30 → `miuix_action_ic_settings_dark` (tối). SymbolDrawable: khung **28×28dp** (symbolIconWidth/Height), glyph **21dp** (symbolIconSize), alpha thường **0.8**, nhấn **0.5**; tint `miuix_default_color_action_icon_tint_light` = **#FF000000** / dark = **#F2FFFFFF**.
- Kích thước nút action bar: `miuix_appcompat_action_button_width/height` = **26dp**, khoảng cách nút `miuix_appcompat_action_button_gap` = **0dp**; mép phải bar `miuix_appcompat_action_bar_horizontal_padding_end` = **20dp**.
- Menu chọn-nhiều (action mode, tham khảo): `getActionModeNoteListMenuRes()` = **res/menu/notes_list_options.xml** (NoteListPageFragment.java:284): hide (`miui_ic_secret`), rename (`?actionBarRenameIcon` → `miuix_action_icon_rename_light/dark`), stick (`miui_ic_stick`), move (`miui_ic_move`), group (`v12_menu_group`), delete (`miui_ic_delete`) — đều `withText|always`.
- `notes_page_options.xml` chỉ có 1 item chữ `notes_setting` (không icon).

## Ghi chú thêm
- Khoảng trống danh sách rỗng: `phone_note_blank_view_margin_top` = **154dp** (EmptyStateView icon `note_blank_icon` + chữ `data_empty_note`).
- Grep "search_text_hint" trong dimens/colors: KHÔNG TÌM THẤY tên đó (đã tìm values/dimens.xml, values/colors.xml) — giá trị hint thực tế nằm ở các tài nguyên `searchEditTextHintColor`/`search_hint` nêu trên.
- Lề trái 46dp của title mở rộng là 20dp+26dp cộng từ code (ActionBarView.java) — không có một dimen đơn lẻ nào ghi "46dp".

---

# Spec LUOI ghi chu + THE ghi chu (MIUI Notes 2.4.2.5, man hinh chinh — phone)

## 1. Luoi (PhoneNoteListPageFragment.updateRecyclerViewLayoutManager, che do grid)
- So cot: **2** (`NoteGridLayoutManager(2, 1)`); neu font he thong lon nhat (fontLevel==2) thi 1 cot.
- Kieu luoi: **STAGGERED** — `NoteGridLayoutManager` la ban copy cua `StaggeredGridLayoutManager` (co Span[], LazySpanLookup, GAP_HANDLING); the cao thap khac nhau xep so le, KHONG can bang hang.
- Khoang cach: ItemDecoration (`BaseNoteListPageFragment.mItemDecoration`) set `outRect` trai/phai/tren/duoi = `mGridItemSpace` cho MOI item; hang dau tien (position/spanCount==0) top=0.
  - `mGridItemSpace = miuix_theme_padding_horizontal_common / 2 = 12dp/2 = 6dp`.
  - → Gap NGANG giua 2 cot = 6+6 = **12dp**; gap DOC giua 2 hang = 6+6 = **12dp**; hang dau khong co gap tren (chi 4dp padding cua RecyclerView).
  - (dimen `v12_grid_item_space = 9.45dp` co ton tai nhung KHONG dung o man hinh chinh phone.)
- RecyclerView padding: start/end = mGridItemSpace = **6dp**, top = `miuix_theme_padding_base` = **4dp** → le trai/phai thuc te cua the toi mep = 6dp (padding) + 6dp (offset item) = **12dp**.
- Vung cuon (SpringBackLayout — container keo dan lo xo cua miuix, keo qua dau/cuoi thi noi dung keo gian roi bat lai): duoc clipToOutline bang roundRect inset ngang `miuix_theme_padding_horizontal_common`=12dp, tren 4dp, bo goc `common_note_list_item_bg_radius`=**16dp** (day keo dai +16dp nen chi thay bo goc TREN). Tai hien: clip noi dung cuon trong khung bo goc tren 16dp, cach mep man hinh 12dp.

## 2. Mau nen the vs nen trang
- Nen man hinh (windowBackground): `page_bg` / `homepage_primary_color` = **#F7F7F7 sang / #000000 toi**.
- Nen the: `v12_theme_content_bg_color` = **#FFFFFF sang / #242424 toi** (drawable `v12_grid_item_bg`: shape rectangle solid mau tren, corners radius `v12_grid_item_shape_radius` = **16dp**, padding trong: start/top/end = `v12_item_grid_padding_start/top/end` = **16dp**, bottom 0).
- KHONG vien, KHONG elevation/bong — the phang, phan biet chi bang mau. (`grid_item_shape_radius`=36px va `v12_grid_item_shape_radius_outer`=44px ton tai nhung khong dung cho the grid chinh.)
- Ca itemView duoc clipToOutline roundRect 16dp (`v12_grid_item_shape_radius`) trong code (CommonNoteGridItem).
- Min-height phan noi dung: `v12_default_theme_grid_bg_min_height` = **102dp**.
- Nhan tay (press): Folme scale itemView xuong **0.9** (spring ease -2, 0.85f, 0.3f), tha ra ve 1.0.

## 3. Trang thai CHON (multi-select)
- The DUOC CHON: set FOREGROUND = `common_note_list_item_press_fg` (shape bo goc `common_note_list_item_bg_radius`=16dp, mau `item_fg_press_color` = **#14000000 sang / #1FFFFFFF toi**) — tuc phu lop xam mo len ca the. Khong doi vien.
- Checkbox: goc **duoi-phai** cua the (`layout_constraintBottom/End`), marginBottom `grid_checkbox_margin_bottom`=**12dp**, marginEnd `grid_checkbox_margin_end`=**16dp**. Kich thuoc tham chieu code `multi_checkbox_size`=**24dp** (list dung `miuix_appcompat_check_widget_width/height`=22dp).
- Style `Note.MultiCheckBox` (styles.xml:4662): parent Widget.CompoundButton.CheckBox, chi set `checkBoxOnDrawableBackgroundColor` = `theme_main_color` = **#FFBB0F sang / #FFBF0F toi** → checkbox tron kieu MIUI, khi check nen vang #FFBB0F voi dau tick.
- Khi vao che do chon, hang chan the (bar) duoc don marginEnd = (16dp + 24dp) = **40dp** de nhuong cho checkbox, animate 0→1 theo tien trinh (setBarMargin).

## 4. The co ANH
- `PreviewImageView` (common_note_item_grid.xml): width match_parent, **height 136dp** (hardcode trong layout), `scaleType="centerCrop"`, nam TREN CUNG the, noi dung chu nam duoi (`content` constraintTop_toBottomOf image).
- Bo goc anh: code goi `setRadius(v12_grid_item_shape_radius=16dp)`, radiusType mac dinh = TOP_ROUND → chi bo **2 goc tren 16dp**, 2 goc duoi vuong (ve bang BitmapShader + Path).
- Phan chu duoi anh doi background sang `v12_grid_item_bottom_radius_bg` (solid `v12_theme_content_bg_color`, chi bo 2 goc DUOI 16dp, padding start/end 16dp); paddingTop phan noi dung giam con mot nua (16dp*0.5 = **8dp**); minHeight = 0.

## 5. Hang chan the (id=bar)
- LinearLayout ngang, paddingTop `v12_item_grid_label_margin_top`=**12dp**, marginBottom `v12_item_grid_padding_bottom`=**16dp** (voi mind-note paddingTop = `common_mind_grid_label_padding_top`=15dp).
- TextView time: style layout `V8.TextAppearance.List.Tertiary` (size `list_item_tertiary_text_size`=**12sp**, mau `v8_text_appearance_list_tertiary_text_color` = **#999999 sang / #7C7C7C toi**) nhung code ghi de bang `V12.TextAppearance.Grid.Tertiary.DayNight`: size `grid_text_tertiary_size`=**12dp**, mau `v12_text_appearance_tertiary_text_color` = **#999999 sang / #7C7C7C toi**, font mipro-regular; weight=1, marginEnd 6dp.
- Icon ghim `ic_data_item_stick`, ghi am `ic_data_item_audio`, dong ho `ic_data_item_clock` (+mind_map/mind_outline/hand_write): vector **14dp x 14dp**, fill **#FFBB0F** (khong co ban night rieng), moi icon marginEnd **6dp**.

## Text tren the (theme mac dinh, mipro)
- Title: `V12.DefaultTheme.TextAppearance.Grid.Primary.DayNight` — size `grid_text_primary_size`=**17sp**, mau `v8_text_appearance_grid_primary_text_color` = **#DE000000 sang / #F2FFFFFF toi**, font mipro-medium; singleLine, ellipsize end, lineSpacingMultiplier 1.2.
- Body: `V12.DefaultTheme.TextAppearance.Grid.Secondary.DayNight` — size `grid_text_secondary_size`=**14sp**, mau `v11_text_appearance_secondary_text_color` = **#666666 sang / #919191 toi**, font mipro-regular; marginTop **12dp**, maxLines 4 (1 neu la ghi am), ellipsize end.

## Ghi chu tham chieu list (common_note_item_list.xml, phu)
- Nen `v12_list_item_bg` (solid `v12_theme_content_bg_color` #FFFFFF/#242424, radius 16dp), marginBottom `list_item_margin_bottom`=**12dp**, minHeight `list_item_height_min`=**100dp**, margin noi dung start/end `list_item_content_margin_start`=**20dp**, top 16dp, bottom 16dp; thumbnail `v12_list_item_thumbnail_size`=**52dp**, radius `list_item_preview_image_radius`=**8dp**, vien `ic_1px_size`=0.36dp.

Nguon file chinh: D:/Project/Note/reference/miui-notes/resources/res/layout/common_note_item_grid.xml; values/dimens.xml; values/colors.xml; values-night/colors.xml; values/styles.xml; drawable/v12_grid_item_bg.xml, v12_grid_item_bottom_radius_bg.xml, common_note_list_item_press_fg.xml; sources/com/miui/notes/page/BaseNoteListPageFragment.java (ItemDecoration), PhoneNoteListPageFragment.java (spanCount/padding), feature/notelist/CommonNoteGridItem.java + CommonNoteBaseItem.java (bg/checkbox/press), theme/DayNightColorTheme.java (style chu), component/PreviewImageView.java (bo goc anh).

KHONG TIM THAY: elevation/bong cho the (da tim trong layout, drawable v12_grid_item_bg, code CommonNoteGridItem — khong co); ban night rieng cua ic_data_item_* (chi co drawable/, khong co drawable-night).

---

# SPEC MAN SOAN THAO GHI CHU (dien thoai) — MIUI Notes 2.4.2.5

Layout goc: `res/layout/phone_note_edit.xml` (dung boi `PhoneNoteEditFragment` -> `R.layout.phone_note_edit`). Header tren cung: `res/layout/v12_phone_edit_header.xml` (gan lam customView cua ActionBar boi `PhoneActionBarController` + `BaseNormalNoteEditFragment.initActionBar()`). Toolbar duoi: `res/layout/small_screen_mode_tool_group.xml`. Dong thong tin: `res/layout/note_extra_info_layout.xml`. Chip thu muc: `res/layout/note_edit_folder_layout.xml`.

## 1. Thanh tren cung (action bar)
- La customView lap day ActionBar miuix, nen ActionBar trong suot (`v8_placeholder` = shape transparent). Cao chuan: `miuix_appcompat_action_bar_default_height = 56dp`.
- LinearLayout ngang, gravity center_vertical:
  - TRAI: nut back `@id/home`, icon `ic_action_back` (selector tro `miui_action_bar_back`, autoMirrored), size `v12_notes_edit_header_ic_size = 40dp` (style `ActionBarIconStyle` 40x40dp).
  - GIUA: View weight=1 (gian cach).
  - PHAI (2 nhom chong nhau trong FrameLayout, doi theo che do):
    - Che do XEM: `share` (ic_action_share), `view_theme` (v12_ic_action_theme), `more` (ic_action_more) — moi nut 40x40dp, marginStart giua cac nut `v12_notes_edit_header_ic_margin = 12dp`.
    - Che do SOAN: `undo` (ic_action_undo), `redo` (ic_action_redo), `done` (ic_action_done) — 40x40dp, cach nhau 12dp.
- Tint icon: `action_button_color` = #000000 (sang) / #ffffff (toi). (Code: PhoneActionBarController.updateStyle setImageTintList).

## 2. Dong thong tin duoi tieu de (note_extra_info_layout)
- Thu tu: `date` + "  " (2 space noi trong code NoteInfoController.updateDataTime) + `time` | vach dung | `text_count` (| vach | icon khoa | vach | bao thuc — an mac dinh).
- Dinh dang: date = `DateUtils.formatDateTime(flags=16, FORMAT_SHOW_DATE)` (vd "19 thang 8"); time = miuix formatDateTime(flags=12) (gio:phut). So ky tu = plurals `word_count` = "%d character(s)".
- Vach dung: drawable `vertical_line` = rect 0.5dp x `secondary_divider_size = 10sp`, mau #000000, paddingStart/End 10dp (style `NoteInfoHeaderDividerStyle`).
- Chu: style `NoteInfoHeaderTextStyle`: size `secondary_text_size = 12sp`, mau `note_edit_extra_info_text_color` = #66000000 (sang) / #66ffffff (toi).
- Vi tri: marginTop 8dp (duoi title), marginBottom 28dp (tren than bai), paddingStart/End = `note_editor_padding = 12dp` (hardcode trong phone_note_edit.xml).

## 3. Tieu de (EditText @id/note_title)
- Co chu `note_edit_title_font_size = 24sp`; minHeight `note_edit_title_height = 40sp`; paddingStart/End `note_editor_padding = 12dp`.
- Font: code set `Typeface mipro-semibold` (MiuiFontName.SEMIBOLD), lineSpacing multiplier 1.5, lineHeight `edit_title_line_height = 39.273sp`, gioi han 100 ky tu (BaseNativeNoteEditFragment.initNoteTitle).
- Mau chu runtime (theme mac dinh): `theme_common_primary_color` = #ff000000 (sang) / #e6ffffff (toi). (XML fallback `editor_text` = #df666666, bi theme ghi de.)
- Hint: string `edit_note_hint_title` = "Title"; mau hint `note_title_hint_color_default` = #cccccc (sang) / #333333 (values-night).

## 4. Than bai (NoteRichEditor @id/rich_editor)
- Co chu: `text_font_size = 16sp` x he so co chu nguoi dung FONT_SCALE {0.8, 1.0, 1.25, 1.5} (ResourceParser.TextAppearanceResources; mac dinh 1.0 => 16sp).
- Gian dong: KHONG TIM THAY setLineSpacing/lineSpacingExtra rieng cho than bai (da tim trong phone_note_edit.xml, TextAppearance.Editor, NoteRichEditor.java, RichEditTextView.java) => multiplier mac dinh 1.0.
- Mau chu: nhu title (#ff000000 / #e6ffffff); mau link `link_text = #0d84ff`.
- Le ngang vung soan: paddingStart/End `note_editor_padding = 12dp`; minHeight 200dp; vung cuon `edit_content` co paddingBottom 120dp (cuon lo ra cuoi trang).
- Con tro: `text_cursor` drawable (NoteTheme).

## 5. Thanh cong cu duoi cung (small_screen_mode_tool_group)
- Cao hang nut: `notes_edit_toolbar_height = 60dp`. Vach ngan phia tren (`panel_divide`): 0.5dp, `toolbar_divide_color` = #0d000000 (sang) / #0dffffff (toi).
- Nen: blur + mix color `note_edit_blur_mix_color_default` = #33ffffff (sang) / #33303030 (toi); khi khong blur: mau dac = `note_edit_blur_mix_color2_default` (#ffffff / #171717) tru 0x06000000 alpha => ~#f9ffffff (sang) / ~#f9171717 (toi) (BlurHelper.setBlurBackground).
- Cau truc ngang: RelativeLayout trai weight 0.81 chua `EditModeToolbar` (paddingStart 15dp, paddingEnd 3dp) + RelativeLayout phai weight 0.19.
- THU TU nut (trai -> phai, moi nut width 0dp weight=1, cao `notes_edit_toolbar_icon_size = 36dp`, marginTop/Bottom `notes_edit_toolbar_icon_margin_vertical = 12dp`):
  1. `ai` — icon_ai (PNG, tinh nang AI)
  2. `audio` — ic_tool_voice (ghi am)
  3. `gallery` — ic_tool_gallery (chen anh)
  4. `edit_image` — ic_tool_doodle (ve tay/doodle)
  5. `check` — ic_tool_checkbox (checklist)
- Nhom phai: vach dung gradient `rich_text_divide` 0.75dp (v12_note_editmodebar_divide_gradient_bg, marginTop 12dp/bottom 16dp, mac dinh invisible) + nut `rich_text_switch` ic_tool_richtext (mo panel dinh dang chu "Aa") 36x36dp, marginEnd 28dp, can giua doc.
- Toolbar chi hien khi dang soan: goc visibility=gone + alpha=0, fade-in 200ms (BaseNoteEditToolbarController). Duoi hang nut co `expand_container` (panel mo rong: kieu chu / ghi am / chon theme) va `navi_placeholder` chua chieu cao navigation bar.

## 6. Nen man soan
- Theme mac dinh (DefaultTheme): `v8_theme_bg_color` = #ffffff (sang) / #ff000000 (toi). WindowBackground cua NoteTheme.Edit = @null (nen ve trong code theo theme ghi chu).

## Chip thu muc + hanh vi cuon (miuix widgets)
- Chip thu muc (note_edit_folder_layout, @id/note_folder_info): nam TREN vung soan, cao `note_dir_indicator_bg_height = 36dp`, marginStart/End `note_edit_default_horizontal_padding = 12dp`, marginBottom `notes_edit_header_margin_bottom = 12dp`. Nen `note_title_bg`: bo tron `note_dir_indicator_bg_radius = 12dp`, vien 0.5dp `note_dir_indicator_border_color_default = #0d000000`, ruot `note_dir_indicator_bg_color_default` = #f7f7f7 (sang) / #40ffffff (toi); runtime dung `common_note_dir_indicator_bg_color_default` = #eeeeee / #242424. Ben trong: icon `ic_note_dir_indicator` (marginStart 16dp) + ten thu muc 12sp font mipro-medium, mau `note_dir_indicator_text_color_default` = #cc000000 / #ffffff, marginStart 4dp, marginEnd 16dp, 1 dong ellipsize end.
- `NestedHeaderLayout` (triggerView = chip thu muc, triggerContentMinHeight 22px): chip la "header co the thu gon" — khi cuon noi dung LEN, chip truot len va bien mat (nhuong cho cho noi dung); khi keo XUONG o dau trang, chip truot ra lai. Thanh action bar tren cung LUON co dinh. Tai hien: cho chip vao header container co dinh chieu cao animate 36dp->0 theo scroll offset.
- `SpringBackLayout` + `FullScreenRichEditScrollView`: ScrollView doc binh thuong nhung co hieu ung keo qua dau/cuoi nay lo xo (overscroll rubber-band, keo qua roi bat lai). Tai hien bang OverScroller/translation khi overscroll.
- Toan bo (chip + editor) cuon; action bar va toolbar duoi co dinh (toolbar la con cua ConstraintLayout goc, neo bottom).

---

# Trích xuất icon vector — MIUI Notes 2.4.2.5 (nguồn: D:/Project/Note/reference/miui-notes/resources/res/)

## PHÁT HIỆN QUAN TRỌNG: icon thanh đầu (folder/settings/search/back) KHÔNG phải vector
Các icon trên action bar của MIUI dùng `miuix.theme.symbol.SymbolDrawable` — một glyph lấy từ **font ký hiệu MiSans Symbol**, không phải path vector. Trong bản dịch ngược jadx, ký tự glyph (`app:symbolText`) đã bị hỏng thành U+FFFD (byte `EF BF BD`), nên **codepoint gốc KHÔNG THỂ khôi phục** từ res này (đã kiểm tra hex trực tiếp). Chuỗi alias:
- `miui_ic_folder` (menu/action_bar_menu.xml) → values/drawables.xml → `miuix_action_icon_folder_light` (sáng) / `miuix_action_icon_folder_dark` (tối, values-night/drawables.xml)
- `miui_ic_settings` → `miuix_action_ic_settings_light` / `miuix_action_ic_settings_dark`
- `miui_action_bar_back` (đích của selector `ic_action_back`) → `miuix_action_icon_back_light` / `_dark`
- Kính lúp trong ô search: `miuix_appcompat_ic_edit_text_search_light` / `_dark`

Thông số thị giác của SymbolDrawable (đủ để tái hiện bằng ImageView + vector tự vẽ):
- Icon action bar (folder/settings): vùng chạm **28dp x 28dp** (`symbolIconWidth/Height=28dp`), glyph **21dp** (`symbolIconSize`), alpha thường **0.8**, nhấn **0.5**, disabled **0.3**; tint sáng `#000000`, tối = `@color/miuix_default_color_action_icon_tint_dark` (trắng). Hình dạng thật (nhìn từ app chạy): folder = thư mục viền tròn kiểu MIUI, settings = bánh răng.
- Kính lúp search: glyph **18dp** (`symbolIconSize=18dp`), alpha **0.3**, tint sáng `#000000` (bản dark tint trắng).
- Back: kích thước theo `miuix_theme_icon_size` / `miuix_theme_title_button_width/height`, `symbolAutoMirroredCompat=true` (lật RTL). Hình dạng: mũi tên "<" có đuôi ngang.

Vector THẬT có sẵn trong app dùng thay thế được (in nguyên văn bên dưới): folder → `ic_data_folder_list`; search → `ic_search_black_24`; back → `ic_phone_handwrite_back_normal`; settings → **KHÔNG TÌM THẤY vector bánh răng** (đã tìm toàn bộ drawable*/ theo "settings": chỉ có `ic_settings_arrow_right` là mũi tên; phải tự vẽ hoặc dùng glyph font).

---

## 1. Nút thêm ghi chú (dấu cộng) — `ic_new_note`
Sáng — `drawable/ic_new_note.xml` (vòng tròn nền đen alpha 0.06 + dấu cộng đen alpha 0.8):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="24dp"
    android:width="24dp"
    android:viewportWidth="74"
    android:viewportHeight="74">
    <path
        android:fillColor="#000000"
        android:pathData="M37,37m-37,0a37,37 0,1 1,74 0a37,37 0,1 1,-74 0"
        android:strokeColor="#00000000"
        android:strokeWidth="1"
        android:strokeAlpha="0.06"
        android:fillAlpha="0.06"
        android:fillType="nonZero"/>
    <path
        android:fillColor="#000000"
        android:pathData="M35,36L35,24.5C35,23.67 35.67,23 36.5,23C37.33,23 38,23.67 38,24.5L38,36L49.5,36C50.33,36 51,36.67 51,37.5C51,38.33 50.33,39 49.5,39L38,39L38,50.5C38,51.33 37.33,52 36.5,52C35.67,52 35,51.33 35,50.5L35,39L23.5,39C22.67,39 22,38.33 22,37.5C22,36.67 22.67,36 23.5,36L35,36Z"
        android:strokeColor="#00000000"
        android:strokeWidth="1"
        android:strokeAlpha="0.8"
        android:fillAlpha="0.8"
        android:fillType="evenOdd"/>
</vector>
```
Tối — `drawable-night/ic_new_note.xml` (khác: nền `#ccddff` fillAlpha 0.2, dấu cộng `#ffffff` fillAlpha 1):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="24dp"
    android:width="24dp"
    android:viewportWidth="74"
    android:viewportHeight="74">
    <path
        android:fillColor="#ccddff"
        android:pathData="M37,37m-37,0a37,37 0,1 1,74 0a37,37 0,1 1,-74 0"
        android:strokeColor="#00000000"
        android:strokeWidth="1"
        android:strokeAlpha="0.06"
        android:fillAlpha="0.2"
        android:fillType="nonZero"/>
    <path
        android:fillColor="#ffffff"
        android:pathData="M35,36L35,24.5C35,23.67 35.67,23 36.5,23C37.33,23 38,23.67 38,24.5L38,36L49.5,36C50.33,36 51,36.67 51,37.5C51,38.33 50.33,39 49.5,39L38,39L38,50.5C38,51.33 37.33,52 36.5,52C35.67,52 35,51.33 35,50.5L35,39L23.5,39C22.67,39 22,38.33 22,37.5C22,36.67 22.67,36 23.5,36L35,36Z"
        android:strokeColor="#00000000"
        android:strokeWidth="1"
        android:strokeAlpha="0.8"
        android:fillAlpha="1"
        android:fillType="evenOdd"/>
</vector>
```

## 2. Dấu cộng trắng của FAB tròn — `ic_content_add` (đặt trên nền tròn; `ic_content_add_background` là `<shape>` rectangle với `corners radius=30dp`)
`drawable/ic_content_add.xml` (không có bản night — dấu cộng luôn trắng):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="58dp"
    android:width="58dp"
    android:viewportWidth="58"
    android:viewportHeight="58">
    <group>
        <clip-path android:pathData="M15,15h28v28h-28z"/>
        <group>
            <clip-path android:pathData="M15,15h28v28h-28z"/>
            <path
                android:fillColor="#ffffff"
                android:pathData="M18.25,29C18.25,28.534 18.25,28.301 18.326,28.117C18.428,27.872 18.622,27.678 18.867,27.576C19.051,27.5 19.284,27.5 19.75,27.5L27.5,27.5L27.5,19.75C27.5,19.284 27.5,19.051 27.576,18.867C27.678,18.622 27.872,18.428 28.117,18.326C28.301,18.25 28.534,18.25 29,18.25C29.466,18.25 29.699,18.25 29.883,18.326C30.128,18.428 30.322,18.622 30.424,18.867C30.5,19.051 30.5,19.284 30.5,19.75L30.5,27.5L38.25,27.5C38.716,27.5 38.949,27.5 39.133,27.576C39.378,27.678 39.572,27.872 39.674,28.117C39.75,28.301 39.75,28.534 39.75,29C39.75,29.466 39.75,29.699 39.674,29.883C39.572,30.128 39.378,30.322 39.133,30.424C38.949,30.5 38.716,30.5 38.25,30.5L30.5,30.5L30.5,38.25C30.5,38.716 30.5,38.949 30.424,39.133C30.322,39.378 30.128,39.572 29.883,39.674C29.699,39.75 29.466,39.75 29,39.75C28.534,39.75 28.301,39.75 28.117,39.674C27.872,39.572 27.678,39.378 27.576,39.133C27.5,38.949 27.5,38.716 27.5,38.25L27.5,30.5L19.75,30.5C19.284,30.5 19.051,30.5 18.867,30.424C18.622,30.322 18.428,30.128 18.326,29.883C18.25,29.699 18.25,29.466 18.25,29Z"/>
        </group>
    </group>
</vector>
```

## 3. Icon dòng dữ liệu trong item ghi chú — 14dp, màu vàng `#ffbb0f` (KHÔNG có bản night, giữ nguyên trong dark)
`drawable/ic_data_item_stick.xml` (ghim):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="14dp"
    android:width="14dp"
    android:viewportWidth="14"
    android:viewportHeight="14">
    <group>
        <clip-path android:pathData="M0,0h14v14h-14z"/>
        <path
            android:fillColor="#ffbb0f"
            android:pathData="M13.103,4.809C13.611,5.317 13.611,6.14 13.104,6.648L12.83,6.922C12.476,7.276 11.949,7.395 11.477,7.228L10.692,6.951C10.653,6.938 10.61,6.949 10.583,6.98L10.22,7.399C10.154,7.476 10.131,7.581 10.16,7.678L10.519,8.925C10.681,9.485 10.665,10.081 10.472,10.631L10.423,10.769C10.032,11.88 8.615,12.202 7.782,11.369L5.756,9.343L3.216,11.883C2.902,12.197 2.492,12.395 2.051,12.444C1.747,12.477 1.491,12.221 1.525,11.918C1.574,11.477 1.771,11.066 2.085,10.752L4.625,8.212L2.586,6.173C1.717,5.304 2.111,3.819 3.298,3.497L3.499,3.443C3.984,3.311 4.496,3.312 4.981,3.447L6.282,3.808C6.378,3.835 6.482,3.811 6.558,3.746L6.986,3.381C7.017,3.354 7.029,3.31 7.015,3.271L6.739,2.49C6.572,2.018 6.691,1.492 7.045,1.138L7.319,0.864C7.827,0.356 8.65,0.356 9.158,0.864L13.103,4.809ZM8.701,2.669C8.626,2.594 8.501,2.673 8.536,2.774L8.799,3.515C8.867,3.71 8.808,3.927 8.651,4.061L7.233,5.274C6.98,5.49 6.637,5.566 6.317,5.478L4.553,4.989C4.346,4.931 4.126,4.93 3.918,4.987L3.884,4.996C3.81,5.016 3.785,5.109 3.84,5.163L8.811,10.135C8.859,10.182 8.939,10.164 8.961,10.1C9.044,9.864 9.052,9.609 8.983,9.369L8.486,7.654C8.392,7.329 8.469,6.978 8.691,6.722L9.908,5.314C10.043,5.158 10.259,5.101 10.453,5.17L11.194,5.431C11.294,5.466 11.373,5.341 11.297,5.266L8.701,2.669Z"/>
    </group>
</vector>
```
`drawable/ic_data_item_audio.xml` (5 vạch sóng âm):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="14dp"
    android:width="14dp"
    android:viewportWidth="14"
    android:viewportHeight="14">
    <path
        android:fillColor="#ffbb0f"
        android:pathData="M0.348,4.3C0.348,4.077 0.348,3.965 0.38,3.875C0.436,3.714 0.563,3.588 0.723,3.531C0.813,3.5 0.925,3.5 1.148,3.5C1.372,3.5 1.484,3.5 1.573,3.531C1.734,3.588 1.861,3.714 1.917,3.875C1.948,3.965 1.948,4.077 1.948,4.3L1.948,9.7C1.948,9.923 1.948,10.035 1.917,10.125C1.861,10.286 1.734,10.412 1.573,10.469C1.484,10.5 1.372,10.5 1.148,10.5C0.925,10.5 0.813,10.5 0.723,10.469C0.563,10.412 0.436,10.286 0.38,10.125C0.348,10.035 0.348,9.923 0.348,9.7L0.348,4.3Z"/>
    <path
        android:fillColor="#ffbb0f"
        android:pathData="M3.274,5.3C3.274,5.077 3.274,4.965 3.306,4.875C3.362,4.714 3.488,4.588 3.649,4.531C3.739,4.5 3.851,4.5 4.074,4.5C4.298,4.5 4.409,4.5 4.499,4.531C4.66,4.588 4.786,4.714 4.843,4.875C4.874,4.965 4.874,5.077 4.874,5.3L4.874,8.7C4.874,8.923 4.874,9.035 4.843,9.125C4.786,9.286 4.66,9.412 4.499,9.469C4.409,9.5 4.298,9.5 4.074,9.5C3.851,9.5 3.739,9.5 3.649,9.469C3.488,9.412 3.362,9.286 3.306,9.125C3.274,9.035 3.274,8.923 3.274,8.7L3.274,5.3Z"/>
    <path
        android:fillColor="#ffbb0f"
        android:pathData="M6.2,12.2C6.2,12.423 6.2,12.535 6.231,12.625C6.288,12.786 6.414,12.912 6.575,12.969C6.665,13 6.777,13 7,13C7.223,13 7.335,13 7.425,12.969C7.586,12.912 7.712,12.786 7.769,12.625C7.8,12.535 7.8,12.423 7.8,12.2L7.8,1.8C7.8,1.577 7.8,1.465 7.769,1.375C7.712,1.214 7.586,1.088 7.425,1.031C7.335,1 7.223,1 7,1C6.777,1 6.665,1 6.575,1.031C6.414,1.088 6.288,1.214 6.231,1.375C6.2,1.465 6.2,1.577 6.2,1.8L6.2,12.2Z"/>
    <path
        android:fillColor="#ffbb0f"
        android:pathData="M9.126,9.7C9.126,9.923 9.126,10.035 9.157,10.125C9.213,10.286 9.34,10.412 9.501,10.469C9.591,10.5 9.702,10.5 9.926,10.5C10.149,10.5 10.261,10.5 10.351,10.469C10.512,10.412 10.638,10.286 10.694,10.125C10.726,10.035 10.726,9.923 10.726,9.7L10.726,4.3C10.726,4.077 10.726,3.965 10.694,3.875C10.638,3.714 10.512,3.588 10.351,3.531C10.261,3.5 10.149,3.5 9.926,3.5C9.702,3.5 9.591,3.5 9.501,3.531C9.34,3.588 9.213,3.714 9.157,3.875C9.126,3.965 9.126,4.077 9.126,4.3L9.126,9.7Z"/>
    <path
        android:fillColor="#ffbb0f"
        android:pathData="M12.052,8.2C12.052,8.423 12.052,8.535 12.083,8.625C12.139,8.786 12.266,8.912 12.427,8.969C12.516,9 12.628,9 12.851,9C13.075,9 13.187,9 13.276,8.969C13.437,8.912 13.564,8.786 13.62,8.625C13.651,8.535 13.651,8.423 13.651,8.2L13.651,5.8C13.651,5.577 13.651,5.465 13.62,5.375C13.564,5.214 13.437,5.088 13.276,5.031C13.187,5 13.075,5 12.851,5C12.628,5 12.516,5 12.427,5.031C12.266,5.088 12.139,5.214 12.083,5.375C12.052,5.465 12.052,5.577 12.052,5.8L12.052,8.2Z"/>
</vector>
```
`drawable/ic_data_item_clock.xml` (đồng hồ báo thức):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="14dp"
    android:width="14dp"
    android:viewportWidth="14"
    android:viewportHeight="14">
    <group>
        <clip-path android:pathData="M0,0h14v14h-14z"/>
        <path
            android:fillColor="#ffbb0f"
            android:pathData="M7.014,1.686C10.201,1.687 12.785,4.271 12.785,7.458C12.784,10.645 10.201,13.228 7.014,13.229C3.827,13.229 1.243,10.645 1.243,7.458C1.243,4.271 3.827,1.686 7.014,1.686ZM7.014,3.287C4.711,3.287 2.843,5.155 2.843,7.458C2.843,9.761 4.711,11.628 7.014,11.628C9.317,11.628 11.184,9.761 11.184,7.458C11.184,5.155 9.317,3.287 7.014,3.287ZM8.21,5.126C8.429,4.909 8.783,4.91 9.001,5.128L9.34,5.467C9.56,5.686 9.559,6.042 9.339,6.261L7.931,7.657C7.712,7.874 7.359,7.874 7.141,7.656L6.801,7.316C6.582,7.097 6.583,6.742 6.803,6.523L8.21,5.126ZM1.75,0.751C1.969,0.532 2.324,0.532 2.542,0.751L2.881,1.091C3.1,1.309 3.1,1.664 2.881,1.883L1.595,3.168C1.376,3.387 1.023,3.387 0.804,3.168L0.464,2.829C0.246,2.61 0.246,2.256 0.464,2.037L1.75,0.751ZM11.404,0.721C11.623,0.502 11.977,0.502 12.196,0.721L13.482,2.007C13.7,2.225 13.701,2.58 13.482,2.799L13.142,3.138C12.923,3.356 12.569,3.356 12.35,3.138L11.065,1.852C10.846,1.633 10.846,1.279 11.065,1.061L11.404,0.721Z"/>
    </group>
</vector>
```
`drawable/ic_data_item_mind_map.xml` (sơ đồ tư duy):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="14dp"
    android:width="14dp"
    android:viewportWidth="14"
    android:viewportHeight="14">
    <path
        android:fillColor="#ffbb0f"
        android:pathData="M4.156,10.198C4.847,10.198 5.406,10.757 5.406,11.448C5.406,12.138 4.846,12.698 4.156,12.698C3.466,12.698 2.907,12.138 2.906,11.448C2.906,10.757 3.466,10.198 4.156,10.198ZM12.565,10.647C12.874,10.647 13.125,10.898 13.125,11.207V11.687C13.125,11.996 12.875,12.246 12.565,12.247H7.577C7.268,12.246 7.018,11.996 7.018,11.687V11.207C7.018,10.898 7.268,10.647 7.577,10.647H12.565ZM4.156,5.75C4.847,5.75 5.406,6.31 5.406,7C5.406,7.691 4.847,8.25 4.156,8.25C3.466,8.25 2.906,7.691 2.906,7C2.906,6.31 3.466,5.75 4.156,5.75ZM12.565,6.201C12.875,6.201 13.125,6.451 13.125,6.76V7.24C13.125,7.549 12.875,7.8 12.565,7.8H7.577C7.268,7.8 7.018,7.549 7.018,7.24V6.76C7.018,6.451 7.268,6.201 7.577,6.201H12.565ZM2.369,1.303C3.059,1.303 3.619,1.863 3.619,2.553C3.619,3.244 3.059,3.803 2.369,3.803C1.679,3.803 1.119,3.244 1.119,2.553C1.119,1.863 1.679,1.303 2.369,1.303ZM12.565,1.75C12.875,1.751 13.125,2.001 13.125,2.31V2.791C13.125,3.1 12.875,3.35 12.565,3.35H5.772C5.463,3.35 5.213,3.1 5.213,2.791V2.31C5.213,2.001 5.464,1.751 5.772,1.75H12.565Z"/>
</vector>
```

## 4. Tab dưới màn chính (56dp; thường = `@color/tab_global_normal_color` = **#000000 sáng / #ffffff tối (values-night/colors.xml)** vẽ với alpha 0.4; chọn = `#f9b40f` cả hai chế độ)
`drawable/ic_tab_global_notes_normal.xml` (sổ ghi chú viền + kẹp dọc):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="56dp"
    android:width="56dp"
    android:viewportWidth="56"
    android:viewportHeight="56">
    <path
        android:fillColor="@color/tab_global_normal_color"
        android:pathData="M21.195,17.5H32.8082C33.8364,17.5 34.4767,17.6237 35.1467,17.9819C35.7978,18.3302 36.3214,18.8538 36.6696,19.5049C37.0279,20.1748 37.1516,20.8152 37.1516,21.8434V34.1566C37.1516,35.1848 37.0279,35.8252 36.6696,36.4951C36.3214,37.1462 35.7978,37.6698 35.1467,38.0181C34.4767,38.3763 33.8364,38.5 32.8082,38.5H21.195C20.1667,38.5 19.5264,38.3763 18.8565,38.0181C18.2053,37.6698 17.6817,37.1462 17.3335,36.4951C16.9752,35.8252 16.8516,35.1848 16.8516,34.1566V21.8434C16.8516,20.8152 16.9752,20.1748 17.3335,19.5049C17.6817,18.8538 18.2053,18.3302 18.8565,17.9819C19.5264,17.6237 20.1667,17.5 21.195,17.5ZM32.8078,19.5996L21.0912,19.6001C20.4557,19.6065 20.174,19.6731 19.9245,19.7935L19.8464,19.8334C19.5612,19.9859 19.3374,20.2097 19.1849,20.4949C19.0324,20.7801 18.9512,21.0629 18.9512,21.843L18.9517,34.2596C18.958,34.8951 19.0246,35.1768 19.1451,35.4263L19.1849,35.5044C19.3374,35.7895 19.5612,36.0134 19.8464,36.1659C20.1057,36.3045 20.363,36.3843 20.9938,36.3976L21.1946,36.3996H32.8078C33.5169,36.3996 33.8152,36.3325 34.0778,36.2057L34.1559,36.1659C34.4411,36.0134 34.6649,35.7895 34.8174,35.5044C34.9561,35.2451 35.0358,34.9877 35.0492,34.357L35.0512,34.1562V21.843C35.0512,21.1338 34.984,20.8356 34.8573,20.573L34.8174,20.4949C34.6649,20.2097 34.4411,19.9859 34.1559,19.8334C33.8707,19.6808 33.5879,19.5996 32.8078,19.5996Z"
        android:strokeAlpha="0.4"
        android:fillAlpha="0.4"
        android:fillType="evenOdd"/>
    <path
        android:fillColor="@color/tab_global_normal_color"
        android:pathData="M29.4492,19.5996H31.5492V26.9496C31.5492,27.5295 31.0791,27.9996 30.4992,27.9996C29.9193,27.9996 29.4492,27.5295 29.4492,26.9496V19.5996Z"
        android:strokeAlpha="0.4"
        android:fillAlpha="0.4"/>
</vector>
```
`drawable/ic_tab_global_notes_selected.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="56dp"
    android:width="56dp"
    android:viewportWidth="56"
    android:viewportHeight="56">
    <path
        android:fillColor="#f9b40f"
        android:pathData="M30.4496,26.95C30.4496,27.5299 30.9197,28 31.4996,28C32.0588,28 32.5159,27.5629 32.5478,27.0117L32.5496,26.95V17.5H33.8062C34.8344,17.5 35.4748,17.6237 36.1447,17.9819C36.7959,18.3302 37.3194,18.8538 37.6677,19.5049C38.0089,20.1429 38.1373,20.7541 38.1488,21.6991L38.1496,21.8434V34.1566C38.1496,35.1848 38.026,35.8252 37.6677,36.4951C37.3194,37.1462 36.7959,37.6698 36.1447,38.0181C35.5067,38.3593 34.8955,38.4877 33.9505,38.4991L33.8062,38.5H22.193C21.1648,38.5 20.5244,38.3763 19.8545,38.0181C19.2034,37.6698 18.6798,37.1462 18.3315,36.4951C17.9903,35.8571 17.8619,35.2459 17.8505,34.3009L17.8496,34.1566V21.8434C17.8496,20.8152 17.9733,20.1748 18.3315,19.5049C18.6798,18.8538 19.2034,18.3302 19.8545,17.9819C20.4925,17.6407 21.1037,17.5123 22.0487,17.5009L22.193,17.5H30.4496V26.95Z"
        android:fillType="evenOdd"/>
</vector>
```
`drawable/ic_tab_global_tasks_normal.xml` (khung + dấu tick):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="56dp"
    android:width="56dp"
    android:viewportWidth="56"
    android:viewportHeight="56">
    <path
        android:fillColor="@color/tab_global_normal_color"
        android:pathData="M22.193,17.5H33.8062C34.8344,17.5 35.4748,17.6237 36.1447,17.9819C36.7959,18.3302 37.3194,18.8538 37.6677,19.5049C38.026,20.1748 38.1496,20.8152 38.1496,21.8434V34.1566C38.1496,35.1848 38.026,35.8252 37.6677,36.4951C37.3194,37.1462 36.7959,37.6698 36.1447,38.0181C35.4748,38.3763 34.8344,38.5 33.8062,38.5H22.193C21.1648,38.5 20.5244,38.3763 19.8545,38.0181C19.2034,37.6698 18.6798,37.1462 18.3315,36.4951C17.9733,35.8252 17.8496,35.1848 17.8496,34.1566V21.8434C17.8496,20.8152 17.9733,20.1748 18.3315,19.5049C18.6798,18.8538 19.2034,18.3302 19.8545,17.9819C20.5244,17.6237 21.1648,17.5 22.193,17.5ZM33.8058,19.5996L22.0893,19.6001C21.4537,19.6065 21.1721,19.6731 20.9226,19.7935L20.8445,19.8334C20.5593,19.9859 20.3355,20.2097 20.183,20.4949C20.0304,20.7801 19.9492,21.0629 19.9492,21.843L19.9497,34.2596C19.9561,34.8951 20.0227,35.1768 20.1431,35.4263L20.183,35.5043C20.3355,35.7895 20.5593,36.0133 20.8445,36.1659C21.1037,36.3045 21.3611,36.3843 21.9918,36.3976L22.1926,36.3996H33.8058C34.515,36.3996 34.8132,36.3325 35.0759,36.2057L35.154,36.1659C35.4391,36.0133 35.663,35.7895 35.8155,35.5043C35.9541,35.2451 36.0339,34.9877 36.0472,34.357L36.0492,34.1562V21.843C36.0492,21.1338 35.9821,20.8356 35.8553,20.573L35.8155,20.4949C35.663,20.2097 35.4391,19.9859 35.154,19.8334C34.8688,19.6808 34.5859,19.5996 33.8058,19.5996Z"
        android:strokeAlpha="0.4"
        android:fillAlpha="0.4"
        android:fillType="evenOdd"/>
    <path
        android:fillColor="@color/tab_global_normal_color"
        android:pathData="M32.3602,24.6454C32.7703,25.0555 32.7703,25.7203 32.3602,26.1304L27.658,30.8326C27.4775,31.013 27.2478,31.1141 27.0121,31.1357L26.9477,31.1397H26.8833C26.6254,31.1318 26.3699,31.0294 26.173,30.8326L23.6982,28.3577C23.2881,27.9477 23.2881,27.2829 23.6982,26.8728C24.1082,26.4628 24.773,26.4628 25.1831,26.8728L26.9155,28.6051L30.8753,24.6454C31.2853,24.2354 31.9502,24.2354 32.3602,24.6454Z"
        android:strokeAlpha="0.4"
        android:fillAlpha="0.4"/>
</vector>
```
`drawable/ic_tab_global_tasks_selected.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="56dp"
    android:width="56dp"
    android:viewportWidth="56"
    android:viewportHeight="56">
    <path
        android:fillColor="#f9b40f"
        android:pathData="M33.8062,17.5C34.8345,17.5 35.4748,17.6237 36.1447,17.9819C36.7959,18.3302 37.3194,18.8538 37.6677,19.5049C38.0089,20.1429 38.1373,20.7541 38.1488,21.6991L38.1496,21.8434V34.1566C38.1496,35.1848 38.026,35.8252 37.6677,36.4951C37.3194,37.1462 36.7959,37.6698 36.1447,38.0181C35.5067,38.3593 34.8955,38.4877 33.9505,38.4991L33.8062,38.5H22.193C21.1648,38.5 20.5244,38.3763 19.8545,38.0181C19.2034,37.6698 18.6798,37.1462 18.3315,36.4951C17.9903,35.8571 17.8619,35.2459 17.8505,34.3009L17.8496,34.1566V21.8434C17.8496,20.8152 17.9733,20.1748 18.3315,19.5049C18.6798,18.8538 19.2034,18.3302 19.8545,17.9819C20.4925,17.6407 21.1037,17.5123 22.0487,17.5009L22.193,17.5H33.8062ZM32.3591,24.6445C31.9655,24.2508 31.337,24.2351 30.9246,24.5973L30.8742,24.6445L26.9144,28.6042L25.182,26.8719C24.7719,26.4618 24.1071,26.4618 23.6971,26.8719C23.3034,27.2655 23.2877,27.894 23.6498,28.3064L23.6971,28.3568L26.1719,30.8317C26.3509,31.0106 26.5783,31.1115 26.8119,31.1342L26.8822,31.1387H26.9466L27.011,31.1348C27.2205,31.1155 27.4253,31.0336 27.595,30.8889L27.6569,30.8317L32.3591,26.1294C32.7692,25.7194 32.7692,25.0545 32.3591,24.6445Z"
        android:fillType="evenOdd"/>
</vector>
```

## 5. Icon toolbar soạn thảo — 36dp, tint `?attr/actionBottomButtonColor` → `@color/action_bottom_button_color` = **#cc000000 sáng / #ccffffff tối (values-night/colors.xml)**
Selector: `ic_tool_checkbox` → `ic_checkbox_n` (thường) / `ic_checkbox_s` (chọn) / `ic_checkbox_p` (nhấn); `ic_tool_gallery` → `ic_gallery_n` / `ic_gallery_p`; `ic_tool_voice` → `ic_tool_voice_n` / `ic_tool_voice_s`.

`drawable/ic_checkbox_n.xml` (ô vuông + tick):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="36dp"
    android:width="36dp"
    android:viewportWidth="37"
    android:viewportHeight="37">
    <path
        android:fillColor="?attr/actionBottomButtonColor"
        android:pathData="M22.663,7.841C24.903,7.841 26.023,7.84 26.879,8.276C27.632,8.66 28.243,9.272 28.627,10.024C29.063,10.88 29.063,12.001 29.063,14.241L29.063,21.89C29.064,24.13 29.063,25.251 28.627,26.106C28.243,26.859 27.632,27.471 26.879,27.854C26.023,28.29 24.903,28.291 22.663,28.291H15.018C12.778,28.291 11.657,28.29 10.802,27.854C10.049,27.471 9.437,26.859 9.054,26.106C8.618,25.251 8.617,24.131 8.617,21.89V14.241C8.617,12.001 8.617,10.88 9.053,10.024C9.436,9.272 10.048,8.66 10.801,8.276C11.656,7.84 12.777,7.841 15.018,7.841H22.663ZM14.098,9.642C12.81,9.642 12.165,9.643 11.673,9.893C11.24,10.114 10.889,10.465 10.668,10.898C10.417,11.39 10.417,12.034 10.417,13.322V22.809C10.417,24.097 10.417,24.741 10.668,25.233C10.889,25.666 11.24,26.017 11.673,26.238C12.165,26.489 12.81,26.489 14.098,26.489H23.583C24.871,26.489 25.515,26.489 26.007,26.238C26.44,26.017 26.792,25.666 27.013,25.233C27.263,24.741 27.263,24.097 27.263,22.809V13.322C27.263,12.034 27.263,11.39 27.013,10.898C26.792,10.465 26.44,10.114 26.007,9.893C25.515,9.643 24.871,9.642 23.583,9.642H14.098ZM23.406,12.701C23.596,12.456 23.947,12.411 24.191,12.6L24.73,13.016C24.974,13.205 25.019,13.557 24.83,13.802L18.397,22.12C18.03,22.595 17.329,22.642 16.902,22.219L13.239,18.581C13.02,18.363 13.019,18.008 13.237,17.789L13.716,17.306C13.934,17.087 14.288,17.086 14.508,17.303L17.367,20.143C17.452,20.228 17.593,20.219 17.666,20.124L23.406,12.701Z"/>
</vector>
```
`drawable/ic_gallery_n.xml` (ảnh: khung + núi + mặt trời):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="36dp"
    android:width="36dp"
    android:viewportWidth="37"
    android:viewportHeight="37">
    <path
        android:fillColor="?attr/actionBottomButtonColor"
        android:pathData="M23.19,8.593C25.431,8.593 26.552,8.593 27.407,9.029C28.16,9.412 28.772,10.024 29.155,10.777C29.591,11.632 29.591,12.753 29.591,14.994V21.138C29.591,23.378 29.591,24.498 29.155,25.354C28.772,26.107 28.16,26.719 27.407,27.102C26.552,27.538 25.431,27.538 23.19,27.538H13.991C11.751,27.538 10.63,27.538 9.774,27.102C9.022,26.719 8.41,26.107 8.026,25.354C7.59,24.498 7.591,23.378 7.591,21.138V14.994C7.591,12.753 7.59,11.632 8.026,10.777C8.41,10.024 9.022,9.412 9.774,9.029C10.63,8.593 11.751,8.593 13.991,8.593H23.19ZM22.847,17.387C22.784,17.365 22.716,17.365 22.653,17.385C22.582,17.409 22.52,17.473 22.396,17.603L18.503,21.701C17.896,22.339 17.593,22.658 17.24,22.777C16.93,22.881 16.594,22.881 16.284,22.777C15.931,22.658 15.627,22.339 15.021,21.7L14.019,20.642C13.896,20.512 13.834,20.447 13.763,20.423C13.7,20.403 13.631,20.403 13.568,20.425C13.497,20.451 13.437,20.517 13.316,20.649L9.862,24.441C10.084,24.809 10.405,25.11 10.79,25.306C11.282,25.557 11.926,25.556 13.214,25.556H23.968C25.256,25.556 25.9,25.557 26.392,25.306C26.824,25.086 27.177,24.733 27.397,24.3C27.597,23.909 27.635,23.421 27.644,22.589L23.099,17.611C22.978,17.479 22.918,17.412 22.847,17.387ZM13.214,10.575C11.926,10.575 11.282,10.575 10.79,10.826C10.357,11.046 10.005,11.398 9.784,11.831C9.534,12.323 9.534,12.967 9.534,14.254V21.876C9.534,21.963 9.535,22.047 9.535,22.127L11.898,19.533C12.503,18.869 12.805,18.537 13.161,18.412C13.474,18.301 13.815,18.298 14.13,18.402C14.488,18.52 14.797,18.847 15.414,19.498L16.415,20.552C16.536,20.68 16.597,20.744 16.667,20.768C16.729,20.789 16.796,20.789 16.858,20.768C16.929,20.744 16.99,20.681 17.111,20.553L21.003,16.458C21.62,15.808 21.928,15.483 22.286,15.365C22.601,15.261 22.941,15.264 23.254,15.375C23.61,15.5 23.912,15.83 24.516,16.492L27.647,19.922V14.254C27.647,12.967 27.648,12.323 27.397,11.831C27.177,11.398 26.824,11.046 26.392,10.826C25.9,10.575 25.256,10.575 23.968,10.575H13.214ZM14.578,12.429C15.545,12.429 16.329,13.214 16.329,14.181C16.329,15.148 15.545,15.932 14.578,15.932C13.611,15.932 12.826,15.148 12.826,14.181C12.826,13.214 13.611,12.429 14.578,12.429Z"/>
</vector>
```
`drawable/ic_tool_voice_n.xml` (7 vạch sóng âm):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="36dp"
    android:width="36dp"
    android:viewportWidth="37"
    android:viewportHeight="37">
    <path
        android:fillColor="?attr/actionBottomButtonColor"
        android:pathData="M7.151,15.065L7.831,15.065A0.56,0.56 0,0 1,8.391 15.625L8.391,20.505A0.56,0.56 0,0 1,7.831 21.065L7.151,21.065A0.56,0.56 0,0 1,6.591 20.505L6.591,15.625A0.56,0.56 0,0 1,7.151 15.065z"/>
    <path
        android:fillColor="?attr/actionBottomButtonColor"
        android:pathData="M10.851,11.565L11.531,11.565A0.56,0.56 0,0 1,12.091 12.125L12.091,24.005A0.56,0.56 0,0 1,11.531 24.565L10.851,24.565A0.56,0.56 0,0 1,10.291 24.005L10.291,12.125A0.56,0.56 0,0 1,10.851 11.565z"/>
    <path
        android:fillColor="?attr/actionBottomButtonColor"
        android:pathData="M14.551,13.065L15.231,13.065A0.56,0.56 0,0 1,15.791 13.625L15.791,22.505A0.56,0.56 0,0 1,15.231 23.065L14.551,23.065A0.56,0.56 0,0 1,13.991 22.505L13.991,13.625A0.56,0.56 0,0 1,14.551 13.065z"/>
    <path
        android:fillColor="?attr/actionBottomButtonColor"
        android:pathData="M18.251,29.065L18.931,29.065A0.56,0.56 135,0 0,19.491 28.505L19.491,7.625A0.56,0.56 135,0 0,18.931 7.065L18.251,7.065A0.56,0.56 135,0 0,17.691 7.625L17.691,28.505A0.56,0.56 135,0 0,18.251 29.065z"/>
    <path
        android:fillColor="?attr/actionBottomButtonColor"
        android:pathData="M21.951,24.565L22.631,24.565A0.56,0.56 135,0 0,23.191 24.005L23.191,12.125A0.56,0.56 135,0 0,22.631 11.565L21.951,11.565A0.56,0.56 135,0 0,21.391 12.125L21.391,24.005A0.56,0.56 135,0 0,21.951 24.565z"/>
    <path
        android:fillColor="?attr/actionBottomButtonColor"
        android:pathData="M25.651,22.065L26.331,22.065A0.56,0.56 135,0 0,26.891 21.505L26.891,14.625A0.56,0.56 135,0 0,26.331 14.065L25.651,14.065A0.56,0.56 135,0 0,25.091 14.625L25.091,21.505A0.56,0.56 135,0 0,25.651 22.065z"/>
    <path
        android:fillColor="?attr/actionBottomButtonColor"
        android:pathData="M29.351,20.565L30.031,20.565A0.56,0.56 135,0 0,30.591 20.005L30.591,16.125A0.56,0.56 135,0 0,30.031 15.565L29.351,15.565A0.56,0.56 135,0 0,28.791 16.125L28.791,20.005A0.56,0.56 135,0 0,29.351 20.565z"/>
</vector>
```

## 6. Vector back arrow thật có trong app — `ic_phone_handwrite_back_normal` (mũi tên trái có đuôi; dùng thay glyph back bị mất)
Kích thước `@dimen/ic_action_size` → `@dimen/ic_100px_size` = **36.36dp**. Màu `@color/phone_hand_write_actionbar_icon_color_n` = **#cc000000 sáng / #e6ffffff tối**.
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="@dimen/ic_action_size"
    android:width="@dimen/ic_action_size"
    android:viewportWidth="100"
    android:viewportHeight="100">
    <path
        android:fillColor="@color/phone_hand_write_actionbar_icon_color_n"
        android:pathData="M22.69,48.65C22.23,49.11 22,49.72 22,50.32C21.99,50.93 22.22,51.55 22.69,52.01L39.65,68.98C40.57,69.9 42.06,69.9 42.98,68.98C43.9,68.06 43.9,66.57 42.98,65.65L30.35,53.03H76.53C77.91,53.03 79.03,51.91 79.03,50.53C79.03,49.14 77.91,48.03 76.53,48.03H29.97L42.98,35.01C43.9,34.09 43.9,32.61 42.98,31.69C42.06,30.77 40.57,30.77 39.65,31.69L22.69,48.65Z"
        android:fillType="evenOdd"/>
</vector>
```
Lưu ý: `ic_action_back` (selector, `android:autoMirrored="true"`) là icon back chính của màn soạn nhưng trỏ tới glyph font đã mất; `ic_pad_actionbar_back` tuy tên là back nhưng path vẽ dấu X (đóng) 40dp, tint `?attr/actionButtonColorStateList`.

## 7. Vector folder thật có trong app — `ic_data_folder_list` (27dp, dùng trong danh sách thư mục; thay được cho glyph folder bị mất)
Màu `@color/theme_default_icon_color` = **#000000 sáng / #ffffff tối**, fillAlpha 0.8.
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="27dp"
    android:width="27dp"
    android:viewportWidth="27"
    android:viewportHeight="27">
    <group>
        <clip-path android:pathData="M0.763,0.584h26v26h-26z"/>
        <group>
            <clip-path android:pathData="M-0.237,-0.416h28v28h-28z"/>
            <path
                android:fillColor="@color/theme_default_icon_color"
                android:pathData="M9.786,4.262C10.345,4.262 10.626,4.262 10.888,4.327C11.12,4.385 11.341,4.48 11.542,4.609C11.769,4.755 11.962,4.959 12.346,5.365L12.83,5.878C13.005,6.063 13.092,6.155 13.195,6.222C13.287,6.28 13.388,6.323 13.493,6.35C13.612,6.379 13.739,6.379 13.993,6.379H18.097C20.337,6.379 21.457,6.379 22.313,6.815C23.065,7.199 23.677,7.811 24.06,8.563C24.497,9.419 24.496,10.539 24.496,12.779V16.681C24.496,18.921 24.497,20.041 24.06,20.896C23.677,21.649 23.065,22.261 22.313,22.645C21.457,23.08 20.337,23.08 18.097,23.08H9.337C7.097,23.08 5.977,23.08 5.121,22.645C4.368,22.261 3.756,21.649 3.372,20.896C2.936,20.041 2.936,18.921 2.937,16.681V10.661C2.937,8.421 2.937,7.301 3.373,6.445C3.757,5.693 4.369,5.081 5.121,4.697C5.977,4.261 7.097,4.262 9.337,4.262H9.786ZM22.662,12.346H4.736V17.601C4.736,18.889 4.737,19.532 4.987,20.024C5.208,20.457 5.56,20.809 5.992,21.029C6.484,21.28 7.129,21.28 8.417,21.28H19.017C20.304,21.28 20.948,21.28 21.44,21.029C21.873,20.809 22.225,20.457 22.445,20.024C22.696,19.532 22.696,18.889 22.696,17.601V12.345C22.685,12.345 22.674,12.346 22.662,12.346ZM8.417,6.062C7.129,6.062 6.484,6.062 5.992,6.313C5.56,6.533 5.208,6.885 4.987,7.317C4.737,7.809 4.736,8.453 4.736,9.741V10.546H22.677C22.651,10.06 22.591,9.722 22.445,9.436C22.225,9.003 21.873,8.65 21.44,8.43C20.948,8.179 20.304,8.18 19.017,8.18H13.811C13.252,8.18 12.973,8.179 12.711,8.114C12.479,8.056 12.257,7.961 12.056,7.832C11.828,7.686 11.637,7.483 11.253,7.076L10.768,6.563C10.593,6.379 10.506,6.286 10.402,6.22C10.311,6.161 10.211,6.118 10.106,6.092C9.986,6.062 9.859,6.062 9.604,6.062H8.417Z"
                android:fillAlpha="0.8"/>
        </group>
    </group>
</vector>
```

## 8. Vector kính lúp thật có trong app — `ic_search_black_24` (material, 24dp, tự tint lại khi dùng)
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="24dp"
    android:width="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#ff000000"
        android:pathData="M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z"/>
</vector>
```

## Tóm tắt trạng thái từng mục yêu cầu
| # | Icon | Kết quả |
|---|------|---------|
| 1 | Folder thanh đầu | Glyph font (SymbolDrawable), codepoint mất — dùng vector `ic_data_folder_list` (mục 7) |
| 2 | Settings/bánh răng | Glyph font, codepoint mất; KHÔNG TÌM THẤY vector bánh răng trong drawable*/ — phải tự vẽ |
| 3 | Kính lúp search | Glyph font 18dp alpha 0.3; vector thay thế `ic_search_black_24` (mục 8) |
| 4 | Dấu cộng thêm ghi chú | `ic_new_note` (sáng+tối) + `ic_content_add` — nguyên văn ở mục 1–2 |
| 5 | stick/audio/clock/mind_map | Nguyên văn ở mục 3, đều 14dp `#ffbb0f`, không có bản night |
| 6 | Tab Ghi chú / Nhiệm vụ | 4 vector nguyên văn ở mục 4; màu thường #000/#fff alpha 0.4, chọn #f9b40f |
| 7 | Back màn soạn | `ic_action_back` → glyph font mất; vector thật `ic_phone_handwrite_back_normal` ở mục 6 |
| 8 | Toolbar checklist/ảnh/audio | `ic_checkbox_n`, `ic_gallery_n`, `ic_tool_voice_n` nguyên văn ở mục 5; tint #cc000000/#ccffffff |

---

# BANG MAU CHU DAO + FONT — MIUI Notes 2.4.2.5 (trich tu resources/res)

## 1. Mau nen chinh (light / night)
| Ten tai nguyen goc | Sang (values/colors.xml) | Toi (values-night/colors.xml) | Ghi chu |
|---|---|---|---|
| homepage_primary_color | `#fff7f7f7` | `#000000` | Nen window + nen man danh sach (new_phone_note_list_fragment, fragment_note_search, homepage_fragment) |
| v12_theme_content_bg_color | `#ffffff` | `#242424` | **Nen the ghi chu** — dung trong drawable/v12_list_item_bg.xml (shape rectangle, bo goc `v12_grid_item_shape_radius = 16dp`) |
| pad_homepage_note_list_bg | `#f7f7f7` | `#000000` | Bien the pad, cung gia tri |

## 2. Mau chu danh sach (light / night)
| Ten tai nguyen goc | Sang | Toi |
|---|---|---|
| v8_text_appearance_list_primary_text_color | `#000000` | `#f2ffffff` |
| v8_text_appearance_list_secondary_text_color | `#99000000` | `#80ffffff` |
| v8_text_appearance_list_tertiary_text_color | `#999999` | `#7c7c7c` |
| v8_text_appearance_list_section_text_color | `#fff3ae08` | `#ed9600` (tieu de nhom/section, vang dam) |
| v8_text_appearance_grid_primary_text_color | `#de000000` | `#f2ffffff` (tieu de the o che do luoi) |
| v11_text_appearance_secondary_text_color | `#666666` | `#919191` (chu phu the luoi — style V12.DefaultTheme.TextAppearance.Grid.Secondary.DayNight) |

## 3. Vang nhan (accent)
| Ten tai nguyen goc | Sang | Toi |
|---|---|---|
| theme_main_color | `#ffbb0f` | `#ffbf0f` — mau chu dao cua app (FAB `content_add_floating_action_button_color_n` tro ve day) |
| note_item_stick_icon_color | **KHONG TIM THAY ban sang** (chi co trong values-night/colors.xml:259 va public.xml) | `#ffbf0f` |
| (thay the ban sang) drawable/ic_data_item_stick.xml | vector 14dp co `android:fillColor="#ffbb0f"` hardcode — day la mau ghim sang thuc te | — |
| miuix_color_yellow_light_level1 / miuix_color_yellow_dark_level1 | `#ffffbb0f` | `#ffffbf0f` (he miuix, khop theme_main_color) |

## 4. Thanh tim kiem
Header danh sach nhung `layout/miuix_appcompat_search_stub.xml`: hang cao `miuix_appcompat_search_view_default_height = 56dp`, ben trong 1 "vien thuoc" (pill) LinearLayout cao `miuix_appcompat_searchbar_bg_height = 44dp`, bo goc `miuix_appcompat_search_mode_bg_radius = 72dp` (bo tron toi da), gom icon kinh lup + TextView hint.
| Muc dich | Ten goc (lan den so cuoi) | Sang | Toi |
|---|---|---|---|
| Nen pill | miuix_appcompat_edit_text_search_bg_color_light/dark → miuix_default_color_surface_container_low_* → miuix_color_black_level9 / miuix_color_white_level8 | `#0f000000` | `#24ffffff` |
| Chu hint | miuix_appcompat_edit_text_search_hint_color_* → miuix_default_color_on_surface_octonary_* → black_level6 / white_level6 | `#4d000000` | `#4dffffff` |
| Chu nhap | miuix_search_input_text_color_* → miuix_default_color_on_surface_* → black_level1 / white_level1_1 | `#ff000000` | `#f2ffffff` |
| Man search rieng cua app: nen | search_bg_color | `#f7f7f7` | `#000000` |
| — nen o nhap | search_intput_bg_color (sic, viet sai chinh ta trong goc) | `#0a000000` | `#1affffff` |
| — lop mo phu | search_mask_color | `#20000000` | `#cc000000` |
| — hint action bar | action_bar_search_hint_color | `#564e4e4e` | `#72e6e6e6` |

## 5. Vach ngan
| Ten goc | Sang | Toi |
|---|---|---|
| divider_line | `#33000000` | `#80ffffff` |
| miuix_default_color_outline_* (→ black_level7 / white_level7) | `#1a000000` | `#33ffffff` |
| toolbar_divide_color | `#0d000000` | `#0dffffff` |
| v8_grid_divider_light | `#1a000000` | (khong co ban night) |

## 6. FONT
**Tep that trong D:/dev/notemod/src/assets/fonts/ (giong het assets APK goc):**
- latinmodern-math.otf — 733,736 B; texgyretermes-math.otf — 532,404 B; xits-math.otf — 538,696 B (font toan hoc, kem GUST-FONT-LICENSE.txt / OFL.txt)
- misymbol_vf.ttf — 134,944 B (font bieu tuong Mi, variable font)
- **KHONG co MiSans/mipro .ttf/.otf nao trong assets** — da kiem tra ca D:/dev/notemod/src/assets/fonts/ va reference/miui-notes/resources/assets/fonts/; res/font/ khong ton tai.

**Khai bao mipro:** values/styles.xml dung `android:fontFamily` chuoi: `mipro-regular`, `mipro-medium` (vd dong 9061, 9066, 9124...; tieu de the = mipro-medium, chu phu = mipro-regular). values/strings.xml:953,962: `miuix_font_style_misans_bold_font_family = mipro-bold`, `..._semibold_font_family = mipro-semibold`.
→ Ket luan: **mipro la ho font MiSans dang ky san trong he thong MIUI (khong dong goi trong APK)**. De sao 1:1 tren may thuong: tu nhung MiSans-Regular/Medium/Bold/SemiBold (Xiaomi phat hanh mien phi ke ca thuong mai) va set Typeface tuong ung; fallback sans-serif/sans-serif-medium.

## 7. Co chu man danh sach (values/dimens.xml)
- list_item_primary_text_size = **17sp** (mipro-medium)
- list_item_secondary_text_size = **14sp** (mipro-regular)
- list_item_tertiary_text_size = **12sp**
- grid_text_primary_size = **17sp**; grid_text_secondary_size = **14sp**; grid_text_tertiary_size = **12dp** (chu y: dp, khong phai sp, dung nhu goc)
- float_item_secondary_text_size dung cho excerpt (khong lay so — man phu)
- lineSpacingMultiplier cua title/body trong the (common_note_item_grid.xml) = **1.2**

---

# SPEC: Nut them ghi chu (FAB) + Thanh tab duoi + Trang thai rong — MIUI Notes 2.4.2.5

Nguon: `D:/Project/Note/reference/miui-notes/resources/res/` va `sources/`

## 1. Nut them ghi chu (FAB)

Layout: `layout/new_phone_note_list_fragment.xml` (id `note_add`, style `@style/Note.FloatingActionButton`, widget `com.miui.notes.ui.widget.FloatingActionButton` ke thua `miuix.miuixbasewidget.widget.FloatingActionButton`).

- **Duong kinh**: `floating_action_button_size = 58dp` (view hinh tron — code tao `ShapeDrawable(OvalShape)` do day bang `fabColor`).
- **Mau nen**: `fabColor = @color/content_add_floating_action_button_color_n` -> `@color/theme_main_color` = **#ffbb0f (sang)** / **#ffbf0f (toi, values-night)**. (Vang, KHONG doi sang xam o dark mode.)
- **Icon**: `drawable/miuix_appcompat_action_button_main_new.xml` — dau **cong (+)** vector 58dp, glyph chiem 56/160 viewport (~**20.3dp**), net day 7/160 (~**2.5dp**), dau bo tron. Mau: `miuix_appcompat_action_button_main_color` -> `miuix_appcompat_white` -> `miuix_color_white_solid_100` = **#ffffff** (khong co ban night — trang ca 2 che do). (Layout `homepage_fragment.xml` co FAB thu 2 id `content_add` dung `ic_content_add` — cung kieu cong trang #ffffff, 21.5dp x 3dp.)
- **Le phai**: `floating_action_bar_margin_end = 35dp`. **Le duoi**: `floating_action_bar_margin_bottom = 35dp`, NHUNG code `NoteListPageFragment.onContentInsetChanged()` dat lai `bottomMargin = contentInset.bottom + 35dp` (inset bottom = thanh tab 64dp + system nav bar) → FAB luon noi **35dp phia tren mep tren thanh tab**.
- **Bong**: co — `fabShadowEnabled = true`. Code miuix FloatingActionButton: `DropShadowConfig(blur 20dp, offsetX 0, offsetY 16dp)`; mau bong mac dinh = chinh `fabColor` (style khong set `fabShadowColor`) → bong mem mau vang mo phia duoi. Nhan xuong co hieu ung Folme touch (tint + thu nho nhe), hover FLOATED_WRAPPED.

## 2. Thanh tab duoi (miuix Navigator BottomNavigationView)

Menu: `menu/bottom_nav_menu.xml` — 2 tab, moi tab **icon + chu** (labeled):
- Tab 1: icon `@drawable/miui_ic_notes`, chu `@string/app_name` = "Notes"
- Tab 2: icon `@drawable/miui_ic_todo`, chu `@string/todo_module_name` = "Tasks"
- File vector `miui_ic_notes` / `miui_ic_todo`: **KHONG TIM THAY** trong cay dich nguoc (chi co ID trong values/public.xml; da tim toan bo res/drawable*, tat ca dpi) — can chup/trich tu APK khac.

Thong so (style chain `Widget.BottomNavigationView` → `Base.Widget.BottomNavigationView` → `Widget.BaseComponents.BottomNavigationView` → `Widget.MiuixDesign.BottomNavigationView`):
- **Chieu cao**: `miuix_design_bottom_navigation_height = 64dp` (minHeight; + inset system nav bar ben duoi; font lon: `..._height_in_large_font = 80dp`).
- **Nen**: `drawable/bottom_navigation_background.xml` = layer-list: (a) duong ke tren cao **0.5dp** mau `?miuixBottomNavigationDividerColor` = `miuix_design_default_color_surface_divider` **#1a000000 (sang)** / `miuix_design_dark_default_color_surface_divider` **#26ffffff (toi)**; (b) phan con lai solid `?miuixColorBottomSurface` — app theme `NavigatorActivityTheme` set = `@color/tab_bottom_surface` = **#f7f7f7 (sang)** / **#000000 (toi)** (duc, khong alpha). Tren may HyperOS that, `BottomNavigationView` thay nen bang **blur trong mo** (`Blur.ExtraHeavy`, HyperOS>2 dung `Mask.Pured_Regular`) — noi dung list cuon xuyen phia duoi thanh (RecyclerView paddingBottom = inset, `clipToPadding=false`). Tai hien bang view thuong: nen mau duc #f7f7f7/#000000 + vach 0.5dp la du. elevation cua style bi ghi de ve **0dp**, bong outline tat (`android:outlineSpotShadowColor = transparent`).
- **Icon tab**: **28dp** (`miuix_design_bottom_navigation_icon_size`, ap qua `setIconSize`; XML item `miuix_design_bottom_navigation_item.xml` ghi 26dp nhung bi style ghi de).
- **Chu tab**: **12sp** (`miuix_design_bottom_navigation_active_text_size` trong item layout; textAppearance `Base.TextAppearance.BaseComponents.BottomMenu` = 11sp sans-serif la fallback), 1 dong, nam ngay duoi icon.
- **Mau chon/khong chon** (ap cho ca icon tint va chu, selector `color/miuix_navigation_bar_item_icon_tint.xml` + app theme ghi de): chon = `navigator_bottom_button_color` = **#000000 (sang)** / **#f2ffffff (toi)**; khong chon = `navigator_bottom_button_unchecked_color` = **#66000000 (sang)** / **#80ffffff (toi)**. → **KHONG phai vang** — den/trang, phan biet bang do dam nhat.
- **Khoang cach doc**: padding tren item = `mira_bottom_nav_item_padding_top = 8dp`, padding duoi = `mira_bottom_nav_item_padding_bottom = 16dp`; label ngay sat duoi icon (XML khong co margin rieng giua icon va chu; code chi set margin doc cho icon container = paddingTop).

## 3. Trang thai rong (chua co ghi chu)

Layout: `new_phone_note_list_fragment.xml`, `miuix.miuixbasewidget.widget.EmptyStateView` id `phone_note_list_blank_view` nam trong NestedScrollView `sv_blank` (che RecyclerView khi rong).

- **Hanh vi EmptyStateView** (layout noi bo `miuix_appcompat_empty_state_layout.xml`): cot doc can giua ngang — ImageView icon o TREN, TextView title ngay DUOI; summary/button khong dung o day.
- **Vi tri**: `android:layout_marginTop = @dimen/phone_note_blank_view_margin_top = 154dp`, dong thoi constraint top+bottom toi parent (ConstraintLayout → can giua roi bi keo lech xuong boi margin; thuc te thay o khoang giua-tren man hinh, ~154dp tu dinh vung cuon).
- **Icon**: `drawable/note_blank_icon.xml` — vector **68dp x 68dp** (khop `miuix_appcompat_empty_state_view_icon_size = 68dp`), hinh to giay co 3 dong ke. Mau: sang = **#000000 voi fillAlpha 0.27** (~#45000000); toi (drawable-night) = **#ffffff voi fillAlpha 0.27** (~#45ffffff).
- **Khoang cach icon → chu**: `miuix_appcompat_empty_state_view_icon_margin_bottom = 8dp`.
- **Chu**: `@string/data_empty_note` = "No notes here yet". Style mac dinh `TextAppearance.EmptyStateView.Title`: co `miuix_font_size_headline1 = 17sp`, font MiSans Medium (`miuix_font_weight_headline1`), mau `?colorOnSurfaceTertiary` = `miuix_default_color_on_surface_tertiary_light` → `miuix_color_black_level4` **#99000000 (sang)** / `..._dark` → `miuix_color_white_level4` **#80ffffff (toi)**. (Luu y: style `EmptyStateViewTitleStyle` 14sp mau `empty_state_view_title_text_color` #f7f7f7/#99ffffff chi ap trong theme phu NoteTheme.Light/NoteTheme.Dark, khong phai man hinh chinh.)

## 4. Quan he FAB ↔ thanh tab

**Khong che nhau.** FAB duoc code nang len: `bottomMargin = contentInset.bottom (gom thanh tab 64dp + system nav bar) + floating_action_bar_margin_bottom (35dp)` → mep duoi FAB cach **mep tren thanh tab dung 35dp**; FAB chi de len noi dung list, thanh tab luon lo ra day du. (Land: `values-land` margin bottom 32.55dp / end 29.4dp.)

---

# CHUYEN DONG & CHI TIET HOAN THIEN — MIUI Notes 2.4.2.5

## 1. Mo ghi chu tu the: CO hieu ung "no ra tu the"
Nguon: `sources/com/miui/notes/page/PhoneNoteListPageFragment.java` + `sources/miuix/transition/ActivityOptionsHelper.java`
- App dung API AN cua framework MIUI (reflection): `ActivityOptions.makeScaleUpAnimationFromRoundedView(...)` (mode 102, dung cho nut FAB "+") va `ActivityOptions.makeScaleUpDown(...)` (mode 103, dung khi bam the ghi chu). KHONG dung SharedElement, KHONG dung Folme cho viec mo activity.
- Tham so khi bam the (openNote): rect = toan bo the `(0,0,width,height)`, ban kinh goc = `v12_grid_item_shape_radius = 16dp`, mau nen phia sau anh chup = `anim_fore_ground_color` (SANG `#ffffffff`, TOI `#ff000000`).
- Tham so khi bam FAB (addNoteFromFab): chup bitmap snapshot cua nut, mau nen = `content_add_floating_action_button_color_n = @color/theme_main_color` (SANG `#ffbb0f`, TOI `#ffbf0f`).
- Hanh vi thi giac: he thong ve mot rounded-rect (snapshot cua the, bo goc 16dp) NO RA tu vi tri/kich thuoc the den toan man hinh, cross-fade sang noi dung EditActivity tren nen mau chi dinh; dong lai thi thu nho nguoc ve the (co `updateScaleUpDownData` de huy anim quay ve khi item da bien mat — bundle key `disableBackAnimation`).
- Thoi luong/easing nam trong framework MIUI, KHONG TIM THAY trong APK (da tim ActivityOptionsHelper + PhoneNoteListPageFragment).
- Fallback khi khong ho tro (isSupportScaleUpDown()==false, multi-window, may lite): mo activity thuong, khong anim dac biet.
- Goi y tai hien bang API khung: `ActivityOptions.makeScaleUpAnimation(view, 0, 0, w, h)` hoac Activity Transition scale + fade tu bounds cua the.

## 2. Cham vao the: CO hieu ung co lai (Folme touch scale)
Nguon: `sources/com/miui/notes/feature/notelist/CommonNoteBaseItem.java`, `CommonNoteGridItem.java`, `CommonNoteListItem.java`
- ACTION_DOWN: scale ve **0.9** ca X va Y (`touchDownScale`: SCALE_X=0.9, SCALE_Y=0.9). Nha tay/huy: ve 1.0 (`touchUpScale`).
- Grid item dung spring Folme: `EaseManager.getStyle(-2, 0.85f, 0.3f)` = spring physics, damping ratio **0.85**, response **0.3s**. List item dung config Folme mac dinh (khong truyen ease).
- The grid con duoc bo goc bang `ViewOutlineProvider` + `clipToOutline=true`, radius = `v12_grid_item_shape_radius = 16dp`.
- Tai hien bang khung: `ValueAnimator`/`SpringAnimation` (android.dynamicanimation hoac tu viet) scale 1.0→0.9 khi press, nay ve 1.0 khi release, stiffness thap ~ tuong duong response 0.3s, damping 0.85.

## 3. Thanh trang thai / edge-to-edge: VE TRAN VIEN, status bar TRONG SUOT
Nguon: `resources/res/values/styles.xml` (style goc `Theme`, dong ~7440+; `Theme.Dark` dong 6886)
- `android:windowDrawsSystemBarBackgrounds = true`
- `android:statusBarColor = @color/miuix_color_transparent = #00000000` (ca sang lan toi — Theme.Dark ke thua Theme, khong ghi de statusBarColor)
- SANG: `android:windowLightStatusBar = true`, `android:windowLightNavigationBar = true`. TOI (`Theme.Dark`): ca hai = `false`.
- `android:navigationBarColor = @color/miuix_appcompat_navigation_bar_color` — gia tri cu the KHONG TIM THAY trong res da dich nguoc (chi co ten trong values/public.xml; gia tri nam trong lib miuix), thuc nghiem tren may la trong suot/da theo nen.
- `android:windowLayoutInDisplayCutoutMode = 1` (shortEdges) o `MiuixTheme.DayNight` va `NavigatorActivityTheme`; NavigatorActivityTheme con co `layoutStable=true` (miuix ActionBarOverlayLayout tu xu ly inset, noi dung nam duoi status bar, header tu chua padding-top).
- Tai hien: `WindowCompat... / window.setDecorFitsSystemWindows(false)` tuong duong, statusBarColor transparent, tu cong inset top vao header.

## 4. Overscroll: SpringBackLayout (keo qua dau/cuoi nay dan hoi)
Nguon: `resources/res/layout/new_phone_note_list_fragment.xml` (cay: NestedHeaderLayout > SpringBackLayout > RecyclerView + EmptyStateView), `sources/miuix/springback/view/SpringBackLayout.java`, `SpringOperator.java`, `SpringScroller.java`
- Hanh vi thi giac: keo qua dau/cuoi thi TOAN BO noi dung (ca RecyclerView) truot theo ngon tay voi luc can tang dan (khong phai glow); nha tay thi nay ve 0 theo spring, khong co hieu ung keo gian pixel.
- Cong thuc can khi keo: `distance = range * (t^3/3 - t^2 + t)` voi `t = min(|keo|/range, 1)`, `range = chieu cao man hinh` (keo doc). => keo het co cung chi di duoc range/3 (~1/3 man hinh).
- Spring khi nha tay: `SpringOperator(damping=1.0, response=0.4)` (hoac 0.55 truong hop khac); tick vat ly max 16ms.
- Tai hien bang khung: chap nhan thay bang stretch overscroll mac dinh cua Android 12+ (EdgeEffect stretch) — khac biet nho: ban goc TRUOT ca khoi noi dung thay vi keo gian; neu muon giong het thi tu lam translationY theo cong thuc tren + SpringAnimation khi tha.

## 5. Chuyen che do thuong <-> chon nhieu: MO DAN (fade) thanh tren + TRUOT thanh menu duoi + checkbox pop-in
Nguon: `sources/com/miui/notes/page/BaseNoteListPageFragment.java` (EditModeCallback), `ui/widget/EditableRecyclerViewWrapper.java`, `ui/widget/CheckBoxEditableViewHolder.java`, `sources/miuix/appcompat/internal/app/widget/ActionBarContextView.java`, `ActionBarImpl.java`
- Long-press item goi `EditableRecyclerViewWrapper.startActionMode(...)` -> miuix `EditActionMode` (ActionBarContextView) thay the action bar thuong.
- Thanh tren (Huy / Chon tat ca / tieu de "Da chon x"): MO DAN bang SpringAnimation tren ALPHA 0→1, stiffness VAO = **322.27**, RA = **986.96**, dampingRatio = **0.9**, delay VAO = **50ms**. Action bar thuong bi an (importantForAccessibility thay doi, khong anim rieng — context view de len va fade in).
- Thanh menu DUOI (Ghim/Chuyen/Xoa... — split action bar): TRUOT len bang Folme TRANSLATION_Y tu `collapsedHeight` → 0, ease spring `(-2, 0.95f, 0.25f)` (damping 0.95, response 0.25s); khi thoat truot xuong nguoc lai.
- Tien do (progress) cua anim menu duoi duoc phat cho tung item dang hien: checkbox hien ra voi ALPHA 0→1 va SCALE 0.8→1.0 theo progress; dong thoi noi dung/anh cua item dich sang (marginEnd tang them `multi_checkbox_size` * progress) de nhuong cho checkbox. Item duoc chon co foreground `common_note_list_item_select_bg`.
- Nut Huy: icon `miuix_action_icon_cancel_light` / `_dark` theo che do sang/toi.
- Tai hien bang khung: 1 ValueAnimator ~300ms (hoac SpringAnimation stiffness 322/986, damping 0.9) lai ca 3 thu: alpha thanh tren, translationY thanh duoi, va (alpha+scale 0.8→1 + margin) cua checkbox trong moi item.


---

# RÀ SOÁT: 8 mảng thị giác còn THIẾU cho bản sao 1:1 (xếp theo độ lộ liễu)

1. **Hàng tab thư mục (sticky view) trên màn chính** — spec đầu-màn-chính chỉ nhắc nó tồn tại (`stickyView=@id/sticky_view`) nhưng KHÔNG có spec hình ảnh: đây là hàng chip "Tất cả ghi chú / Chưa phân loại / …" cuộn ngang, ghim ngay trên danh sách, nhìn thấy 100% thời gian. Trích tiếp: `resources/res/layout/data_folder_list_layout.xml` + `data_folder_list_item.xml` (đã xác nhận tồn tại: chip nền `drawable/data_folder_list_item_bg.xml`, chữ mipro-medium, màu `color/data_folder_list_item_text_color_n.xml`, cao `pad_folder_list_item_height`, fadingEdge 20dp), cùng `sources/com/miui/notes/feature/notelist/view/FolderRecyclerView.java`.

2. **Thanh action mode khi chọn nhiều (hình ảnh, không chỉ animation)** — spec chuyển-động đã có spring/translationY nhưng chưa có spec vẽ: thanh trên "Hủy / Đã chọn x / Chọn tất cả" (cỡ chữ, icon cancel/select-all, vị trí) và thanh menu ĐÁY 5-6 nút icon+chữ (cao bao nhiêu, icon size, cỡ chữ, nền blur/màu đặc). Trích: `layout/miuix_appcompat_responsive_action_menu_layout.xml`, `miuix_appcompat_action_menu_item_layout.xml`, `miuix_appcompat_action_mode_title_item.xml`, `menu/notes_list_options.xml`, dimens `miuix_appcompat_action_*`.

3. **Hộp thoại kiểu miuix (xóa, đổi tên, di chuyển vào thư mục)** — chưa có spec nào; dialog MIUI là panel bo tròn lớn neo đáy màn hình, nút to bo tròn full-width — dùng AlertDialog mặc định sẽ lộ ngay lần đầu bấm Xóa. Trích: `layout/miuix_appcompat_alert_dialog.xml` + `_button_panel.xml`, `edit_dialog.xml`/`edit_dialog_content.xml`/`dialog_edit_text.xml` (đổi tên/tạo thư mục), `common_folder_list_dialog_fragment.xml` + `common_folder_dialog_item.xml` (chọn thư mục đích), styles `miuixDialog*` trong `values/styles.xml`.

4. **Chế độ tìm kiếm khi bấm search bar** — spec chỉ nói "cả thanh là một nút mở chế độ tìm kiếm" nhưng không tả màn đó: pill trượt lên sát action bar, nút "Hủy" hiện bên phải, lớp mờ `search_mask_color` phủ danh sách, kết quả có highlight từ khóa. Trích: `layout/fragment_note_search.xml` (đã xác nhận), `layout/miuix_appcompat_search_action_mode_view.xml`, `sources/miuix/appcompat/internal/app/widget/SearchActionModeView.java`, string `miuix_appcompat_search_action_mode_cancel`.

5. **Checklist bên trong màn soạn** — toolbar có nút chèn checklist nhưng chưa có spec hình checkbox trong thân bài (hình dạng ô, kích thước, màu khi tick, có gạch chữ không, thụt lề). Trích: grep `CheckBox|Checkable|input_check` trong `sources/com/miui/notes/editor/` (NoteRichEditor/RichEditTextView spans) và drawables `ic_todo_item_checkbox_*`, `drawable/checkbox_*`.

6. **Popup menu "⋯" ở màn soạn (và menu ngữ cảnh)** — nút `more` đã spec nhưng menu bung ra thì chưa: popup bo tròn kiểu miuix với các dòng lệnh, nền nổi, chiều rộng, cỡ chữ. Trích: `layout/immersion_popup_menu_item.xml`, `miuix_appcompat_hyper_popup_menu_group_title_item.xml`, `sources/miuix/appcompat/internal/view/menu/` (PopupMenu/HyperPopup), drawable nền popup `miuix_appcompat_popup_menu_bg*`.

7. **Thanh cuộn miuix** — chưa spec: thumb bo tròn mảnh, tự mờ dần sau khi ngừng cuộn; scrollbar mặc định Android trông khác. Trích: `drawable/miuix_appcompat_scrollbar_thumb_vertical_light.xml` / `_dark.xml` / `_anim` (đã xác nhận, có dimens width/radius/padding riêng) + style scrollbar trong `values/styles.xml`.

8. **Trạng thái nhấn của nút icon (không phải ripple Android)** — các nút action bar/toolbar khi nhấn chỉ đổi alpha (0.8→0.5 với SymbolDrawable) hoặc Folme tint tròn mờ, KHÔNG có ripple lan; để ripple mặc định sẽ thấy sai ngay mỗi lần bấm. Trích: selector `ic_tool_*_p`/`ic_checkbox_p` trong `res/drawable/`, `sources/miuix/folme/` (ITouchStyle setTint/touchDown), alpha states trong values `miuix_*_alpha`.

Ghi chú thêm (dưới ngưỡng 8 mục): màu chữ section vàng `v8_text_appearance_list_section_text_color` (#f3ae08) đã có trong bảng màu nhưng chưa rõ dùng ở header nhóm nào — nên xem `sources/com/miui/notes/adapter/` khi làm phần ghim/nhóm; và vùng trạng thái MiCloud (`triggerView=@id/micloud_state_view`) chỉ hiện khi kéo quá đà, độ lộ thấp.

---

# DOT 2 (3/8 mang ve duoc; 5 mang con lai bi gioi han phien, se trich sau)


---

# SPEC — O tick (checklist) trong than bai ghi chu (man soan thao)

Nguon: `sources/com/miui/richeditor/style/CheckboxSpan.java` (span dang dung), `VisualStyleFactory.java` (factory tao span), `style/render/DefaultEditorImageRender.getCheckboxDrawable()` -> `Theme.getCheckboxResource()` -> `@drawable/ic_richtext_check`.

## 1. Drawable & hinh dang
- Selector: `ic_richtext_check.xml` — `state_checked=true` -> `ic_richtext_checkbox_c`, mac dinh -> `ic_richtext_checkbox_u`.
- Hinh: **vuong bo goc** (khong tron). Chua tick: vien stroke, goc bo ~3.5/16 canh (~3.5dp o co 16dp). Da tick: khoi dac bo goc, dau tick bi KHOET RONG (fillType evenOdd) — dau tick hien mau NEN trang giay, khong phai ve mau trang rieng.

## 2. Kich thuoc
- Co o tick: `@dimen/checkbox_span_size` = **16dp** (values/dimens.xml:591). Runtime: `16dp * fontInnerScale * (scaledDensity/density)` — tuc scale theo sp va theo muc co chu app.
- Do day vien (chua tick): `strokeWidth="1"` tren viewport 16 = **1dp** o co mac dinh (scale theo size).

## 3. Mau
Attr trong theme mac dinh `NoteTheme` (values/styles.xml:4691-4692):
- CHUA tick — vien `?attr/richTextListElementPrimaryColor` = `@color/theme_default_icon_color_n`:
  - SANG: **#ff8c93b0** (values/colors.xml:2492)
  - TOI: **#ffffffff** (values-night/colors.xml:376)
- DA tick — nen `?attr/richTextListElementCheckedColor` = `@color/theme_default_icon_color_n_c`:
  - SANG: **#ffffbb00** (vang; values/colors.xml:2493) — KHONG phai #ffbb0f
  - TOI: **#ffffffff** (values-night/colors.xml:377)
  - Dau tick: khoet trong suot -> lo mau nen ghi chu (sang: trong nen trang -> tick "trang"; toi: lo nen den).

## 4. Chu cua dong DA tick
`CheckboxSpan.updateDrawState`: checked -> `textPaint.setAlpha(102)` (**40%** cua mau chu hien tai), unchecked -> alpha 255. **KHONG gach ngang, KHONG doi sang mau xam rieng** — span extends StrikethroughSpan nhung override updateDrawState khong goi super. (`LegacyCheckboxSpan.updateDrawState` moi dung `setStrikeThruText(checked)` — legacy, khong duoc tao o dau trong editor hien tai.)

## 5. Khoang cach / thut le / vi tri
- O tick -> chu: `@dimen/note_edit_checkbox_margin_end` = **12dp** (values/dimens.xml:3039).
- LeadingMargin cua dong = imageSize + 12dp = **~28dp** o co mac dinh; cac dong wrap cung thut vao (setSpan flag 17 = SPAN_INCLUSIVE_EXCLUSIVE + paragraph).
- O tick nam sat mep trai vung text (mPaddings luon = 0, khong indent them).
- Vi tri doc: `bounds.bottom = baseline + 0.15*imageSize` (~2.4dp duoi baseline), `top = bottom - imageSize`.

## 6. Chieu cao dong (LineHeightSpan)
- `@dimen/note_edit_stand_line_height` = **30px** (values/dimens.xml:3044) nhan tay voi `scaledDensity` trong code -> hieu qua **30sp** (x1.067 = 32sp neu dong co FontMidSizeSpan, x1.2 = 36sp neu FontLargeSizeSpan; `EditLineHeightSpan` ratios: FONT_MIDDLE=1.067, FONT_LARGE=1.2).
- Baseline: `descent = floor(0.3 * lineHeight)` (FONT_BASE_LINE_RATIO=0.3), `ascent = descent - lineHeight`.
- Cuoi doan (ky tu cuoi la '\n'): descent += `@dimen/note_edit_stand_paragraph_margin` = **12dp** (values/dimens.xml:3045).
- Click toggle: vung cham = bounds cua o tick; nhan doi trang thai (`onClick` -> setChecked(!checked)).

## 7. XML nguyen van

### drawable/ic_richtext_check.xml (selector)
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:state_checked="true"
        android:drawable="@drawable/ic_richtext_checkbox_c"/>
    <item android:drawable="@drawable/ic_richtext_checkbox_u"/>
</selector>
```

### drawable/ic_richtext_checkbox_u.xml (CHUA tick)
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="16dp"
    android:width="16dp"
    android:viewportWidth="16"
    android:viewportHeight="16">
    <path
        android:pathData="M4,0.5L12,0.5A3.5,3.5 0,0 1,15.5 4L15.5,12A3.5,3.5 0,0 1,12 15.5L4,15.5A3.5,3.5 0,0 1,0.5 12L0.5,4A3.5,3.5 0,0 1,4 0.5z"
        android:strokeColor="?attr/richTextListElementPrimaryColor"
        android:strokeWidth="1"/>
</vector>
```

### drawable/ic_richtext_checkbox_c.xml (DA tick)
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="16dp"
    android:width="16dp"
    android:viewportWidth="16"
    android:viewportHeight="16">
    <path
        android:fillColor="?attr/richTextListElementCheckedColor"
        android:pathData="M0.436,2.184C0,3.0397 0,4.1598 0,6.4V9.6C0,11.8402 0,12.9603 0.436,13.816C0.8195,14.5686 1.4314,15.1805 2.184,15.564C3.0397,16 4.1598,16 6.4,16H9.6C11.8402,16 12.9603,16 13.816,15.564C14.5686,15.1805 15.1805,14.5686 15.564,13.816C16,12.9603 16,11.8402 16,9.6V6.4C16,4.1598 16,3.0397 15.564,2.184C15.1805,1.4314 14.5686,0.8195 13.816,0.436C12.9603,0 11.8402,0 9.6,0H6.4C4.1598,0 3.0397,0 2.184,0.436C1.4314,0.8195 0.8195,1.4314 0.436,2.184ZM11.8805,4.7611C12.2437,5.1243 12.2437,5.7131 11.8805,6.0763L7.5408,10.416C7.1776,10.7792 6.5888,10.7792 6.2256,10.416C6.207,10.3974 6.1894,10.3783 6.1727,10.3585C6.1377,10.3323 6.1041,10.3032 6.0723,10.2714L4.3024,8.5015C3.9392,8.1383 3.9392,7.5495 4.3024,7.1864C4.6656,6.8232 5.2544,6.8232 5.6175,7.1864L6.8788,8.4477L10.5654,4.7611C10.9286,4.398 11.5174,4.398 11.8805,4.7611Z"
        android:fillType="evenOdd"/>
</vector>
```

Ghi chu: khong co ban drawable-night rieng — dark mode chi doi qua attr color (values-night). `ic_richtext_checkbox_u_light` / `ic_richtext_share_checkbox_*` chi dung cho render chia se (share), khong dung trong editor. Glob `ic_todo*` trong drawable chi ra icon man To-do rieng (clock/remind), khong lien quan o tick trong bai.

---

# SPEC: Vector con thieu (MIUI Notes 2.4.2.5)

Nguon: `D:/Project/Note/reference/miui-notes/resources/res/`

---

## 1. note_blank_icon (trang thai rong, 68dp)

Co ca 2 ban: `drawable/note_blank_icon.xml` (SANG) va `drawable-night/note_blank_icon.xml` (TOI). Khac nhau o `fillColor` (#000000 vs #ffffff) va pathData lech ~0.76 don vi theo truc X (nen ve chung 1 vector + tint theo theme cung duoc, nhung duoi day la nguyen van).

### BAN SANG — drawable/note_blank_icon.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="68dp"
    android:width="68dp"
    android:viewportWidth="69"
    android:viewportHeight="69">
    <path
        android:fillColor="#000000"
        android:pathData="M54.921,8.232C53.882,7.703 52.522,7.703 49.801,7.703L27.749,7.703C22.308,7.703 19.588,7.703 17.51,8.762C15.682,9.693 14.196,11.179 13.265,13.007C12.206,15.085 12.206,17.805 12.206,23.246L12.206,52.386C12.206,55.106 12.206,56.467 12.735,57.506C13.201,58.419 13.944,59.163 14.858,59.628C15.897,60.158 17.257,60.158 19.977,60.158L42.03,60.158C47.47,60.158 50.191,60.158 52.269,59.099C54.097,58.167 55.583,56.681 56.514,54.854C57.573,52.776 57.573,50.055 57.573,44.615L57.573,15.474C57.573,12.754 57.573,11.394 57.043,10.355C56.578,9.441 55.835,8.698 54.921,8.232ZM22.789,20.957C22.73,21.14 22.73,21.366 22.73,21.818C22.73,22.27 22.73,22.496 22.789,22.679C22.909,23.049 23.199,23.339 23.569,23.459C23.752,23.518 23.978,23.518 24.43,23.518L45.349,23.518C45.801,23.518 46.027,23.518 46.21,23.459C46.58,23.339 46.869,23.049 46.99,22.679C47.049,22.496 47.049,22.27 47.049,21.818C47.049,21.366 47.049,21.14 46.99,20.957C46.869,20.587 46.58,20.298 46.21,20.178C46.027,20.118 45.801,20.118 45.349,20.118L24.43,20.118C23.978,20.118 23.752,20.118 23.569,20.178C23.199,20.298 22.909,20.587 22.789,20.957ZM22.789,33.069C22.73,33.252 22.73,33.478 22.73,33.93C22.73,34.382 22.73,34.608 22.789,34.791C22.909,35.161 23.199,35.451 23.569,35.571C23.752,35.63 23.978,35.63 24.43,35.63L45.349,35.63C45.801,35.63 46.027,35.63 46.21,35.571C46.58,35.451 46.869,35.161 46.99,34.791C47.049,34.608 47.049,34.382 47.049,33.93C47.049,33.478 47.049,33.252 46.99,33.069C46.869,32.7 46.58,32.41 46.21,32.29C46.027,32.23 45.801,32.23 45.349,32.23L24.43,32.23C23.978,32.23 23.752,32.23 23.569,32.29C23.199,32.41 22.909,32.7 22.789,33.069ZM22.73,46.042C22.73,45.59 22.73,45.364 22.789,45.181C22.909,44.812 23.199,44.522 23.569,44.402C23.752,44.342 23.978,44.342 24.43,44.342L35.82,44.342C36.272,44.342 36.498,44.342 36.681,44.402C37.051,44.522 37.341,44.812 37.461,45.181C37.52,45.364 37.52,45.59 37.52,46.042C37.52,46.494 37.52,46.72 37.461,46.903C37.341,47.273 37.051,47.563 36.681,47.683C36.498,47.742 36.272,47.742 35.82,47.742L24.43,47.742C23.978,47.742 23.752,47.742 23.569,47.683C23.199,47.563 22.909,47.273 22.789,46.903C22.73,46.72 22.73,46.494 22.73,46.042Z"
        android:strokeAlpha="0.9"
        android:fillAlpha="0.27"
        android:fillType="evenOdd"/>
</vector>
```

### BAN TOI — drawable-night/note_blank_icon.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="68dp"
    android:width="68dp"
    android:viewportWidth="69"
    android:viewportHeight="69">
    <path
        android:fillColor="#ffffff"
        android:pathData="M54.158,8.232C53.119,7.703 51.759,7.703 49.039,7.703L26.986,7.703C21.546,7.703 18.825,7.703 16.747,8.762C14.919,9.693 13.433,11.179 12.502,13.007C11.443,15.085 11.443,17.805 11.443,23.246L11.443,52.386C11.443,55.106 11.443,56.467 11.973,57.506C12.438,58.419 13.181,59.163 14.095,59.628C15.134,60.158 16.494,60.158 19.215,60.158L41.267,60.158C46.708,60.158 49.428,60.158 51.506,59.099C53.334,58.167 54.82,56.681 55.751,54.854C56.81,52.776 56.81,50.055 56.81,44.615L56.81,15.474C56.81,12.754 56.81,11.394 56.281,10.355C55.815,9.441 55.072,8.698 54.158,8.232ZM22.026,20.957C21.967,21.14 21.967,21.366 21.967,21.818C21.967,22.27 21.967,22.496 22.026,22.679C22.146,23.049 22.436,23.339 22.806,23.459C22.989,23.518 23.215,23.518 23.667,23.518L44.586,23.518C45.038,23.518 45.264,23.518 45.447,23.459C45.817,23.339 46.107,23.049 46.227,22.679C46.286,22.496 46.286,22.27 46.286,21.818C46.286,21.366 46.286,21.14 46.227,20.957C46.107,20.587 45.817,20.298 45.447,20.178C45.264,20.118 45.038,20.118 44.586,20.118L23.667,20.118C23.215,20.118 22.989,20.118 22.806,20.178C22.436,20.298 22.146,20.587 22.026,20.957ZM22.026,33.069C21.967,33.252 21.967,33.478 21.967,33.93C21.967,34.382 21.967,34.608 22.026,34.791C22.146,35.161 22.436,35.451 22.806,35.571C22.989,35.63 23.215,35.63 23.667,35.63L44.586,35.63C45.038,35.63 45.264,35.63 45.447,35.571C45.817,35.451 46.107,35.161 46.227,34.791C46.286,34.608 46.286,34.382 46.286,33.93C46.286,33.478 46.286,33.252 46.227,33.069C46.107,32.7 45.817,32.41 45.447,32.29C45.264,32.23 45.038,32.23 44.586,32.23L23.667,32.23C23.215,32.23 22.989,32.23 22.806,32.29C22.436,32.41 22.146,32.7 22.026,33.069ZM21.967,46.042C21.967,45.59 21.967,45.364 22.026,45.181C22.146,44.812 22.436,44.522 22.806,44.402C22.989,44.342 23.215,44.342 23.667,44.342L35.057,44.342C35.51,44.342 35.736,44.342 35.918,44.402C36.288,44.522 36.578,44.812 36.698,45.181C36.757,45.364 36.757,45.59 36.757,46.042C36.757,46.494 36.757,46.72 36.698,46.903C36.578,47.273 36.288,47.563 35.918,47.683C35.736,47.742 35.51,47.742 35.057,47.742L23.667,47.742C23.215,47.742 22.989,47.742 22.806,47.683C22.436,47.563 22.146,47.273 22.026,46.903C21.967,46.72 21.967,46.494 21.967,46.042Z"
        android:strokeAlpha="0.9"
        android:fillAlpha="0.27"
        android:fillType="evenOdd"/>
</vector>
```

---

## 2. Vector dong ho cho nhan "nhac nho" man soan

Glob `drawable*/ic_*alarm*`: chi co `drawable-xxxhdpi/stat_notify_alarm.webp` — PNG/webp, khong dung.
Glob `drawable*/ic_*clock*` (ngoai ic_data_item_clock): co ic_clock_black_24dp, ic_note_edit_info_clock, va nhom ic_todo_* / pad_* / pc_*.

**Phu hop nhat: `drawable/ic_note_edit_info_clock.xml`** — duoc dung trong `layout/note_extra_info_layout.xml` (chip thong tin them cua man soan, cung layout voi chip thu muc). Khong co ban night (tint theo code/layout). 13x14dp, fill #000000:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="14dp"
    android:width="13dp"
    android:viewportWidth="13"
    android:viewportHeight="14">
    <path
        android:fillColor="#000000"
        android:pathData="M10.7815,2.0721C10.5064,1.7969 10.0603,1.7969 9.7852,2.0721C9.5101,2.3472 9.5101,2.7932 9.7852,3.0683L11.0373,4.3204C11.3124,4.5955 11.7584,4.5955 12.0335,4.3204C12.3086,4.0452 12.3086,3.5992 12.0335,3.3241L10.7815,2.0721ZM0.9623,4.3227C0.687,4.0474 0.687,3.6011 0.9623,3.3258L2.215,2.0731C2.4903,1.7978 2.9366,1.7978 3.2119,2.0731C3.4871,2.3484 3.4871,2.7947 3.2119,3.0699L1.9591,4.3227C1.6839,4.5979 1.2376,4.5979 0.9623,4.3227ZM3.2055,7.6939C3.2055,5.8733 4.6814,4.3975 6.5019,4.3975C8.3225,4.3975 9.7983,5.8733 9.7983,7.6939C9.7983,9.5144 8.3225,10.9903 6.5019,10.9903C4.6814,10.9903 3.2055,9.5144 3.2055,7.6939ZM6.5019,3.0975C3.9634,3.0975 1.9056,5.1554 1.9056,7.6939C1.9056,10.2324 3.9634,12.2903 6.5019,12.2903C9.0405,12.2903 11.0983,10.2324 11.0983,7.6939C11.0983,5.1554 9.0405,3.0975 6.5019,3.0975ZM6.3239,6.9593C6.0657,7.2175 6.0657,7.6361 6.3239,7.8943C6.582,8.1525 7.0007,8.1525 7.2589,7.8943L8.0389,7.1142C8.2971,6.856 8.2971,6.4374 8.0389,6.1792C7.7808,5.921 7.3622,5.921 7.104,6.1792L6.3239,6.9593Z"
        android:fillType="evenOdd"/>
</vector>
```

(Du phong: `drawable/ic_clock_black_24dp.xml` — vector Material chuan 24dp, fill #000000, path dong ho kim — cung co san neu can icon to hon.)

---

## 3. Nut menu chon nhieu: miui_ic_stick / miui_ic_move / miui_ic_delete / miui_ic_secret

**Tat ca 4 cai la GLYPH FONT (miuix.theme.symbol.SymbolDrawable) — MAT, khong co vector path.** Ky tu `app:symbolText` la private-use codepoint da bi hong khi decompile (hien thi U+FFFD), font symbol nam trong lib miuix — khong tai tao duoc tu res.

Chuoi alias (values/drawables.xml va values-night/drawables.xml):

| Alias | SANG tro toi | TOI tro toi | Loai |
|---|---|---|---|
| miui_ic_stick | @drawable/miuix_action_icon_stick_light | @drawable/miuix_action_icon_stick_dark | glyph font, mat |
| miui_ic_move | @drawable/miuix_action_icon_move_light | @drawable/miuix_action_icon_move_dark | glyph font, mat |
| miui_ic_delete | @drawable/miuix_icon_trash_light | @drawable/miuix_icon_trash_dark | glyph font, mat |
| miui_ic_secret | @drawable/miuix_action_icon_add_secret_light | @drawable/miuix_action_icon_add_secret_dark | glyph font, mat |

Thong so style cua SymbolDrawable (de tu ve thay the):
- Tint: `@color/miuix_default_color_action_icon_tint_light` = **#ff000000** (SANG); `..._tint_dark` = **#f2ffffff** (TOI) — values/colors.xml:1790-1791.
- Kich thuoc (file miuix_icon_trash_light ghi cung): symbolIconWidth/Height = 28dp, symbolIconSize = 21dp; alpha: normal 0.8, pressed 0.5, disabled 0.3.

---

## 4. drawable/ic_note_dir_indicator.xml (icon chip thu muc man soan)

La vector, 15x15dp. Fill dung `@color/theme_default_icon_color` = **#000000 (SANG)** / **#ffffff (TOI)** (values/colors.xml:2491, values-night/colors.xml:375). Khong co ban night rieng.

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="15dp"
    android:width="15dp"
    android:viewportWidth="15"
    android:viewportHeight="15">
    <group>
        <clip-path android:pathData="M0.927,0.354h14v14h-14z"/>
        <group>
            <clip-path android:pathData="M0.927,0.354h14v14h-14z"/>
            <path
                android:fillColor="@color/theme_default_icon_color"
                android:pathData="M5.468,1.767C5.964,1.767 6.425,1.961 6.77,2.285H11.227C12.884,2.285 14.227,3.628 14.227,5.285V9.94C14.227,11.597 12.884,12.94 11.227,12.94H4.627C2.97,12.94 1.627,11.597 1.627,9.94V4.767C1.627,3.11 2.97,1.767 4.627,1.767H5.468ZM4.787,3.367C3.926,3.367 3.227,4.066 3.227,4.927V9.78C3.227,10.641 3.926,11.339 4.787,11.339H11.068C11.929,11.339 12.627,10.641 12.627,9.78V8.448C12.627,7.586 11.929,6.887 11.068,6.887H8.186C7.355,6.887 6.62,6.347 6.372,5.554L5.755,3.578C5.716,3.452 5.599,3.367 5.468,3.367H4.787ZM7.9,5.077C7.939,5.202 8.055,5.288 8.186,5.288H11.227C11.733,5.288 12.209,5.414 12.627,5.635V5.445C12.627,4.583 11.928,3.884 11.067,3.884H7.528L7.9,5.077Z"/>
        </group>
    </group>
</vector>
```

---

## 5. ic_tool_richtext* va ic_tool_doodle*

Ca nhom dung fill `?attr/actionBottomButtonColor` → `@color/action_bottom_button_color` = **#cc000000 (SANG)** / **#ccffffff (TOI)** (values/colors.xml:16, values-night/colors.xml:7). Khong co ban night rieng cua file drawable.

### drawable/ic_tool_richtext.xml (nut "Aa" — thuc te la dau "T", vector 36dp)
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="36dp"
    android:width="36dp"
    android:viewportWidth="36"
    android:viewportHeight="36">
    <path
        android:fillColor="?attr/actionBottomButtonColor"
        android:pathData="M9.381,9.225C9.35,9.315 9.35,9.426 9.35,9.65C9.35,9.873 9.35,9.985 9.381,10.075C9.437,10.236 9.564,10.362 9.725,10.418C9.814,10.45 9.926,10.45 10.15,10.45H17.699L17.699,26.05C17.699,26.273 17.699,26.385 17.731,26.475C17.787,26.636 17.913,26.762 18.074,26.818C18.164,26.85 18.276,26.85 18.499,26.85C18.723,26.85 18.834,26.85 18.924,26.818C19.085,26.762 19.212,26.636 19.268,26.475C19.299,26.385 19.299,26.273 19.299,26.05L19.299,10.45H26.849C27.073,10.45 27.185,10.45 27.274,10.418C27.435,10.362 27.562,10.236 27.618,10.075C27.649,9.985 27.649,9.873 27.649,9.65C27.649,9.426 27.649,9.315 27.618,9.225C27.562,9.064 27.435,8.938 27.274,8.881C27.185,8.85 27.073,8.85 26.849,8.85H18.499H10.15C9.926,8.85 9.814,8.85 9.725,8.881C9.564,8.938 9.437,9.064 9.381,9.225Z"
        android:fillType="evenOdd"/>
</vector>
```

### drawable/ic_tool_richtext_reverse.xml (trang thai dong panel — dau X, vector 36dp)
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="36dp"
    android:width="36dp"
    android:viewportWidth="36"
    android:viewportHeight="36">
    <path
        android:fillColor="?attr/actionBottomButtonColor"
        android:pathData="M10.814,25.686C10.971,25.843 11.049,25.921 11.134,25.962C11.289,26.038 11.471,26.038 11.626,25.962C11.711,25.921 11.789,25.843 11.946,25.686L18.5,19.131L25.045,25.677C25.202,25.833 25.28,25.912 25.365,25.953C25.521,26.028 25.702,26.028 25.857,25.953C25.942,25.912 26.02,25.833 26.177,25.677C26.333,25.52 26.412,25.442 26.453,25.357C26.529,25.202 26.529,25.02 26.453,24.865C26.412,24.78 26.333,24.702 26.177,24.545L19.632,18L26.186,11.445C26.343,11.289 26.421,11.211 26.462,11.126C26.538,10.97 26.538,10.789 26.462,10.634C26.421,10.549 26.343,10.471 26.186,10.314C26.03,10.158 25.951,10.079 25.867,10.038C25.711,9.962 25.53,9.962 25.375,10.038C25.29,10.079 25.211,10.158 25.055,10.314L18.5,16.869L11.955,10.323C11.799,10.167 11.72,10.089 11.635,10.047C11.48,9.972 11.299,9.972 11.143,10.047C11.059,10.089 10.98,10.167 10.824,10.323C10.667,10.48 10.589,10.558 10.547,10.643C10.472,10.798 10.472,10.98 10.547,11.135C10.589,11.22 10.667,11.298 10.824,11.455L17.369,18L10.814,24.555C10.658,24.711 10.579,24.789 10.538,24.874C10.462,25.03 10.462,25.211 10.538,25.366C10.579,25.451 10.658,25.529 10.814,25.686Z"/>
</vector>
```

### drawable/ic_tool_doodle.xml — la SELECTOR (khong phai vector truc tiep)
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:state_pressed="true"
        android:drawable="@drawable/ic_doodle_p"/>
    <item android:drawable="@drawable/ic_doodle_n"/>
</selector>
```

### drawable/ic_doodle_n.xml (trang thai thuong, vector 36dp)
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="36dp"
    android:width="36dp"
    android:viewportWidth="37"
    android:viewportHeight="37">
    <group>
        <clip-path android:pathData="M4.591,4.065h28v28h-28z"/>
        <path
            android:fillColor="?attr/actionBottomButtonColor"
            android:pathData="M17.653,10.748C15.091,12.806 12.253,16.141 10.014,20.357C9.863,20.641 9.788,20.783 9.68,20.86C9.584,20.928 9.474,20.964 9.355,20.964C9.223,20.964 9.083,20.893 8.802,20.753L8.705,20.705C8.425,20.565 8.285,20.495 8.205,20.384C8.135,20.289 8.097,20.168 8.098,20.05C8.099,19.913 8.171,19.778 8.314,19.507C10.666,15.066 13.672,11.508 16.463,9.267C17.935,8.084 19.393,7.23 20.702,6.833C21.986,6.442 23.355,6.435 24.342,7.327C25.578,8.446 25.468,9.987 25.022,11.312C24.575,12.64 23.687,14.099 22.831,15.457C22.718,15.637 22.605,15.815 22.493,15.991C21.729,17.198 21.012,18.33 20.539,19.354C20.27,19.935 20.108,20.427 20.052,20.83C20.003,21.179 20.041,21.415 20.118,21.584C20.208,21.68 20.283,21.713 20.342,21.729C20.427,21.751 20.564,21.76 20.786,21.708C21.267,21.597 21.847,21.284 22.591,20.861C22.632,20.838 22.674,20.814 22.716,20.79C23.369,20.419 24.145,19.977 24.896,19.751C25.734,19.5 26.752,19.457 27.618,20.221L27.673,20.27L27.72,20.327C28.269,20.989 28.381,21.72 28.247,22.403C28.126,23.021 27.806,23.593 27.541,24.038C27.465,24.165 27.393,24.282 27.326,24.393C27.127,24.719 26.964,24.985 26.843,25.256C26.688,25.605 26.702,25.757 26.724,25.826C26.744,25.886 26.837,26.047 27.401,26.079C27.494,26.085 27.613,26.084 27.741,26.079C28.101,26.067 28.281,26.061 28.404,26.118C28.516,26.171 28.598,26.248 28.657,26.358C28.722,26.477 28.726,26.645 28.733,26.98L28.736,27.126C28.742,27.382 28.745,27.509 28.691,27.626C28.648,27.72 28.556,27.822 28.467,27.874C28.356,27.94 28.247,27.949 28.029,27.966C27.779,27.986 27.527,27.99 27.293,27.976C26.606,27.937 25.339,27.694 24.92,26.42C24.678,25.687 24.879,24.997 25.108,24.483C25.284,24.088 25.537,23.675 25.749,23.328C25.806,23.235 25.86,23.146 25.909,23.064C26.174,22.62 26.331,22.299 26.382,22.038C26.416,21.867 26.403,21.743 26.304,21.6C26.133,21.477 25.899,21.434 25.442,21.571C24.904,21.733 24.299,22.076 23.566,22.493L23.53,22.514C22.847,22.901 22.017,23.373 21.216,23.559C20.796,23.656 20.325,23.692 19.847,23.563C19.354,23.43 18.929,23.142 18.587,22.717L18.54,22.659L18.503,22.594C18.134,21.951 18.076,21.242 18.17,20.569C18.262,19.905 18.507,19.221 18.814,18.557C19.344,17.41 20.138,16.157 20.893,14.966C21.004,14.791 21.115,14.616 21.223,14.444C22.1,13.054 22.858,11.784 23.221,10.706C23.585,9.624 23.448,9.081 23.067,8.736C22.808,8.502 22.272,8.341 21.254,8.651C20.261,8.952 19.019,9.65 17.653,10.748Z"
            android:fillType="evenOdd"/>
    </group>
</vector>
```

### drawable/ic_doodle_p.xml (trang thai nhan)
Giong het ic_doodle_n (cung path, cung kich thuoc, cung `?attr/actionBottomButtonColor`), chi them dung 1 dong tren path: `android:fillAlpha="0.6"`.

---

## Ghi chu tong hop mau (SANG / TOI)
- note_blank_icon: #000000 / #ffffff, fillAlpha 0.27 ca hai ban.
- ic_note_edit_info_clock: fill cung #000000, khong co ban toi — tint khi dung.
- ic_note_dir_indicator: @color/theme_default_icon_color = #000000 / #ffffff.
- ic_tool_richtext, ic_tool_richtext_reverse, ic_doodle_n/p: ?attr/actionBottomButtonColor → @color/action_bottom_button_color = #cc000000 / #ccffffff.
- miui_ic_stick/move/delete/secret: glyph font, mat — tint #ff000000 / #f2ffffff, icon 28x28dp, glyph size 21dp, alpha normal 0.8 / pressed 0.5 / disabled 0.3.

---

# SPEC: (A) Thanh cuon miuix + (B) Trang thai nhan cua nut icon

## (A) THANH CUON DOC (scrollbar thumb)

### Drawable tinh (duoc theme dung)
- Theme `Theme.Light` (values/styles.xml:7493): `android:scrollbarThumbVertical = @drawable/miuix_appcompat_scrollbar_thumb_vertical_light`
- Theme `Theme.Dark` (values/styles.xml:6896): `android:scrollbarThumbVertical = @drawable/miuix_appcompat_scrollbar_thumb_vertical_dark`
- Alias day/night: `values/drawables.xml` -> `miuix_appcompat_scrollbar_thumb_vertical` = `_light`; `values-night/drawables.xml` = `_dark`.

Ca 2 file la layer-list giong het nhau, chi khac mau solid:
```xml
<layer-list>
    <item android:right="@dimen/miuix_appcompat_scrollbar_thumb_vertical_padding">  <!-- 4dp -->
        <shape>
            <corners android:radius="@dimen/miuix_appcompat_scrollbar_thumb_vertical_radius"/>  <!-- 40dp -->
            <size android:width="@dimen/miuix_appcompat_scrollbar_thumb_vertical_width"/>       <!-- 4dp -->
            <solid android:color="@color/miuix_appcompat_scrollbar_thumb_vertical_bg_light|_dark"/>
        </shape>
    </item>
</layer-list>
```

### Kich thuoc (values/dimens.xml:2343-2345)
| Ten goc | Gia tri |
|---|---|
| `miuix_appcompat_scrollbar_thumb_vertical_width` | **4dp** |
| `miuix_appcompat_scrollbar_thumb_vertical_radius` | **40dp** (thuc te = full pill vi width chi 4dp) |
| `miuix_appcompat_scrollbar_thumb_vertical_padding` | **4dp** (inset `android:right`, cach mep phai 4dp) |

### Mau
| Ten goc | Chuoi tham chieu | Gia tri cuoi |
|---|---|---|
| SANG: `miuix_appcompat_scrollbar_thumb_vertical_bg_light` | -> `@color/miuix_color_black_level7` | **#1A000000** (den 10%) |
| TOI: `miuix_appcompat_scrollbar_thumb_vertical_bg_dark` | -> `@color/miuix_color_white_level7` | **#33FFFFFF** (trang 20%) |

### Bien the _anim (khong duoc theme tham chieu, chi ton tai trong res)
`drawable/miuix_appcompat_scrollbar_thumb_vertical_light_anim.xml` / `_dark_anim.xml` — custom drawable `miuix.appcompat.app.ScrollBarAnimationDrawable` voi `android:width="4dp"`, `android:radius="40dp"`, `app:solidColor` = cung 2 mau tren. Khong tim thay noi su dung trong sources ngoai R.java.

### Hanh vi mo dan (nguon: sources/miuix/appcompat/app/ScrollBarAnimationDrawable.java + framework)
- `ScrollBarAnimationDrawable`: khi alpha TANG (bat dau cuon) chay ValueAnimator fade-IN **250ms, LinearInterpolator** (nhan them vao alpha goc cua mau). Inset trai/phai cua rect = **-4dp** (RIGHT_MARGIN=4 x density).
- Fade-OUT sau khi ngung cuon: app KHONG override `android:scrollbarFadeDuration` / `scrollbarDefaultDelayBeforeFade` trong theme (da tim values/styles.xml, layout/) -> dung mac dinh framework: **cho 300ms** (ViewConfiguration.SCROLL_BAR_DEFAULT_DELAY) roi **mo dan 250ms** (SCROLL_BAR_FADE_DURATION).
- Ngoai le (khong phai list ghi chu): `layout/miuix_appcompat_action_sheet_list_view.xml` va `miuix_appcompat_hyper_popup_list.xml` dat fadeDuration=100 / delayBeforeFade=100.
- Widget rieng `HyperScrollBar` (sources/miuix/miuixbasewidget/widget/HyperScrollBar.java): alpha mac dinh 26/255 (~10%), khi cham 77/255 (~30%), tu an sau **2000ms** (`mDefaultAutoHideDelay`).

## (B) TRANG THAI NHAN CUA NUT ICON (toolbar soan thao)

### Alpha chuan cua miuix (values/dimens.xml:2802-2807)
| Ten goc | Gia tri |
|---|---|
| `miuix_theme_icon_normal_alpha_light` | **1.0** |
| `miuix_theme_icon_normal_alpha_dark` | **0.95** |
| `miuix_theme_icon_pressed_alpha_light` | **0.6** |
| `miuix_theme_icon_pressed_alpha_dark` | **0.5** |
| `miuix_theme_icon_disabled_alpha_light` | **0.3** |
| `miuix_theme_icon_disabled_alpha_dark` | **0.4** |

### Selector `drawable/ic_tool_checkbox.xml` (nguyen van thu tu item)
```xml
<selector>
    <item android:state_selected="true" android:drawable="@drawable/ic_checkbox_s"/>
    <item android:drawable="@drawable/ic_checkbox_n"/>
    <item android:state_pressed="true" android:drawable="@drawable/ic_checkbox_p"/>
</selector>
```
LUU Y: item default dung TRUOC item pressed -> theo luat selector Android, `ic_checkbox_p` KHONG BAO GIO duoc chon (default match het). Day la nguyen van file goc; ban sao 1:1 giu nguyen (hoac biet rang pressed cua checkbox thuc te khong doi hinh qua selector nay).

### Selector `drawable/ic_tool_gallery.xml` (thu tu DUNG)
```xml
<selector>
    <item android:state_pressed="true" android:drawable="@drawable/ic_gallery_p"/>
    <item android:drawable="@drawable/ic_gallery_n"/>
</selector>
```

### Khac biet _n vs _p (vector 36dp, viewport 37x37, pathData GIONG HET nhau)
| File | fillColor | fillAlpha |
|---|---|---|
| `ic_checkbox_n` | `?attr/actionBottomButtonColor` | (khong co = 1.0) |
| `ic_checkbox_p` | `?attr/actionBottomButtonColor` (GIU NGUYEN mau) | **0.6** |
| `ic_checkbox_s` (selected) | `?attr/actionButtonColorSelected` | **0.8** |
| `ic_gallery_n` | `?attr/actionBottomButtonColor` | (khong co = 1.0) |
| `ic_gallery_p` | **`?attr/actionButtonColorSelected`** (DOI sang mau accent — khac voi checkbox) | **0.6** |

### Gia tri cuoi cua cac attr (theme mac dinh, values/styles.xml:4597-4598 va cac theme Note)
| Attr | Ten color goc | SANG | TOI |
|---|---|---|---|
| `actionBottomButtonColor` | `@color/action_bottom_button_color` | **#CC000000** (values/colors.xml:16) | **#CCFFFFFF** (values-night/colors.xml:7) |
| `actionButtonColorSelected` | `@color/action_button_color_selected` | **#FFBB0F** (values/colors.xml:35) | **#FFBB0F** (khong co override trong values-night -> giu nguyen) |

### QUY TAC KET LUAN
Nut icon toolbar KHONG dung ripple. Khi nhan: giu nguyen hinh path, giam `fillAlpha` tu **1.0 xuong 0.6** (khop `miuix_theme_icon_pressed_alpha_light` = 0.6; ban dark cua miuix la 0.5 nhung vector _p hard-code 0.6 cho ca 2 theme). Trang thai selected: doi mau sang `actionButtonColorSelected` (#FFBB0F) voi alpha 0.8. Rieng `ic_gallery_p` con doi ca mau sang accent khi nhan (nguyen van file goc).

### XML nguyen van `drawable/ic_checkbox_p.xml` (khac _n dung 1 dong `fillAlpha`)
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:height="36dp"
    android:width="36dp"
    android:viewportWidth="37"
    android:viewportHeight="37">
    <path
        android:fillColor="?attr/actionBottomButtonColor"
        android:pathData="M22.663,7.841C24.903,7.841 26.023,7.84 26.879,8.276C27.632,8.66 28.243,9.272 28.627,10.024C29.063,10.88 29.063,12.001 29.063,14.241L29.063,21.89C29.064,24.13 29.063,25.251 28.627,26.106C28.243,26.859 27.632,27.471 26.879,27.854C26.023,28.29 24.903,28.291 22.663,28.291H15.018C12.778,28.291 11.657,28.29 10.802,27.854C10.049,27.471 9.437,26.859 9.054,26.106C8.618,25.251 8.617,24.131 8.617,21.89V14.241C8.617,12.001 8.617,10.88 9.053,10.024C9.436,9.272 10.048,8.66 10.801,8.276C11.656,7.84 12.777,7.841 15.018,7.841H22.663ZM14.098,9.642C12.81,9.642 12.165,9.643 11.673,9.893C11.24,10.114 10.889,10.465 10.668,10.898C10.417,11.39 10.417,12.034 10.417,13.322V22.809C10.417,24.097 10.417,24.741 10.668,25.233C10.889,25.666 11.24,26.017 11.673,26.238C12.165,26.489 12.81,26.489 14.098,26.489H23.583C24.871,26.489 25.515,26.489 26.007,26.238C26.44,26.017 26.792,25.666 27.013,25.233C27.263,24.741 27.263,24.097 27.263,22.809V13.322C27.263,12.034 27.263,11.39 27.013,10.898C26.792,10.465 26.44,10.114 26.007,9.893C25.515,9.643 24.871,9.642 23.583,9.642H14.098ZM23.406,12.701C23.596,12.456 23.947,12.411 24.191,12.6L24.73,13.016C24.974,13.205 25.019,13.557 24.83,13.802L18.397,22.12C18.03,22.595 17.329,22.642 16.902,22.219L13.239,18.581C13.02,18.363 13.019,18.008 13.237,17.789L13.716,17.306C13.934,17.087 14.288,17.086 14.508,17.303L17.367,20.143C17.452,20.228 17.593,20.219 17.666,20.124L23.406,12.701Z"
        android:fillAlpha="0.6"/>
</vector>
```

Nguon da doc: D:/Project/Note/reference/miui-notes/resources/res/drawable/{miuix_appcompat_scrollbar_thumb_vertical_light|_dark|_light_anim|_dark_anim,ic_tool_checkbox,ic_tool_gallery,ic_checkbox_n,ic_checkbox_p,ic_checkbox_s,ic_gallery_n,ic_gallery_p}.xml; values/dimens.xml; values/colors.xml; values-night/colors.xml; values/styles.xml; values/drawables.xml; values-night/drawables.xml; sources/miuix/appcompat/app/ScrollBarAnimationDrawable.java; sources/miuix/miuixbasewidget/widget/HyperScrollBar.java