# Tendoo IPTV Home 📺

**Launcher Android TV siêu gọn + trình phát IPTV Việt Nam tích hợp**, phát triển độc lập dựa trên các nguyên tắc tối giản của [CleanYourTV (fork của anh)](https://github.com/tendoodn/cleanyourtv). Không tích hợp thư viện UI, WebView, quảng cáo, analytics, dịch vụ nền hay bộ máy Chromium.

## Bố cục cố định 4 hàng

- **Hàng 1–2 (12 ô):** mặc định có **IPTV Việt Nam** và dấu **+**. Tự chọn ứng dụng đã cài trên máy; tối đa **11 ứng dụng ghim + 1 ô thêm**, mỗi hàng tối đa 6 ô. Giữ OK trên ô, hoặc bấm MENU / Quản lý để **chọn vị trí**, chuyển trái/phải hoặc **gỡ khỏi màn hình** (không gỡ APK của ứng dụng).
- **Hàng 3–4 (12 ô):** kênh TV xem nhanh; bấm bằng remote để phát. Dùng nút ◀ ▶ để chuyển trang danh sách.
- **Màn hình IPTV:** danh sách kênh theo nhóm, nút đồng bộ từ GitHub; phát trực tiếp qua **Android MediaPlayer + SurfaceView**, không cần trình phát ngoài.
- **Đổi kênh:** trong khi phát, nhấn CH+/CH− hoặc ↑/↓. Nhấn BACK để quay lại.

## Link playlist cố định

**[https://raw.githubusercontent.com/tendoodn/iptv/main/playlist/vietnam.m3u](https://raw.githubusercontent.com/tendoodn/iptv/main/playlist/vietnam.m3u)**

Sửa file [playlist/vietnam.m3u](playlist/vietnam.m3u) trong repo GitHub rồi Commit vào nhánh **main**. Launcher tự đồng bộ khi vào Home nếu đã quá **4 giờ** từ lần kiểm tra trước, hoặc anh bấm nút **↻** để cập nhật ngay. Danh sách đã tải được lưu trên thiết bị, khi mạng lỗi vẫn có playlist cũ và playlist đi kèm APK làm phương án dự phòng.

M3U mẫu chứa 46 nguồn kênh Việt Nam công khai, một số luồng có thể ngừng hoạt động, giới hạn vùng hoặc phụ thuộc máy chủ bên thứ ba. **Chưa có xác nhận phát được từng kênh trên TV thật.**

## APK test

- Xem [GitHub Actions → Build lightweight IPTV Home APK](../../actions/workflows/android-apk.yml).
- Sau khi build thành công, tải artifact `Tendoo-IPTV-Home-1.0.0-test1` hoặc xem [Releases](../../releases).
- Đó là APK **debug dùng thử**, chưa phải bản phát hành production ký chính thức.
- Tương thích mục tiêu: **Android 6.0+**, có hỗ trợ HOME intent. Không chạy trên Tizen/webOS.

## Cài làm launcher mặc định

1. Cài APK trên Android TV/Google TV hoặc TV Box.
2. Mở Tendoo IPTV Home từ danh sách ứng dụng.
3. Bấm **Quản lý → Cài làm launcher mặc định** hoặc nhấn nút **HOME** và chọn Tendoo IPTV Home → **Luôn dùng**.
4. Một số firmware chặn launcher bên thứ ba; khi đó có thể cần cấu hình ADB riêng theo model. **Không tự động vô hiệu hóa hay xóa launcher gốc.**

Nếu cần khôi phục, vào **Cài đặt → Ứng dụng mặc định → Ứng dụng Home**, chọn launcher trước đó. Tránh dùng debloat/disable cho TV chưa xác minh tên package hệ thống.

## Build ở máy có Android SDK

Yêu cầu **JDK 17, Android SDK platform 35, Gradle 8.9**.

```sh
gradle --no-daemon :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Không có runtime dependency bên thứ ba nên không kéo các bộ thư viện AndroidX/Compose/Flutter/Media3 vào APK. R8 và shrinkResources được bật cho cấu hình release. Trình phát native có kích thước nhỏ nhưng độ tương thích HLS có thể khác nhau giữa các hãng, nhất là kênh mã hóa DRM, cookie/token hoặc codec đặc thù.

### Cấu trúc

```
app/src/main/java/vn/tendoo/iptv/
  HomeActivity.java       # Launcher 4 hàng
  ChannelsActivity.java   # Danh sách kênh
  PlayerActivity.java     # Bộ phát HLS native
  Playlist.java           # Parser M3U, đồng bộ và cache
  Ui.java                 # UI điều khiển bằng remote
playlist/vietnam.m3u      # Chỉnh trên GitHub không cần rebuild
.github/workflows/android-apk.yml
```

**Tham khảo:** [tendoodn/cleanyourtv](https://github.com/tendoodn/cleanyourtv) (fork từ [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv), MIT). Phần mã nguồn của Tendoo IPTV Home được viết mới, không sao chép mã gốc.
