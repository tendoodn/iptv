# Tendoo IPTV Home 📺

Android TV HOME launcher dùng **giao diện và mã nguồn CleanYourTV của anh** tại [tendoodn/cleanyourtv](https://github.com/tendoodn/cleanyourtv), bổ sung ứng dụng tùy chỉnh và truyền hình IPTV Việt Nam.

## Phiên bản hiện tại: 1.0.0.test5

**[Tải Tendoo-IPTV-Home-1.0.0-test5.apk](https://github.com/tendoodn/iptv/releases/download/iptv-v1.0.0-test5/Tendoo-IPTV-Home-1.0.0-test5.apk)** · [GitHub Release](https://github.com/tendoodn/iptv/releases/tag/iptv-v1.0.0-test5)

- **Biên dịch trực tiếp trên Vercel Sandbox**, không biên dịch APK bằng GitHub Actions.
- Kích thước: **1.963.892 byte** (~1,96 MB).
- SHA-256: `92e552a348a7d748b9e70b9a4cb3316fc3dccd010b514ee8aac08630a041f5d3`.
- Package: `vn.tendoo.iptv`. Android 8.0/API 26 trở lên. Khóa ký debug thử nghiệm.
- Đã kiểm tra APK bằng `aapt`: icon, Android TV banner, các intent `HOME`, `LAUNCHER`, `LEANBACK_LAUNCHER` và 46 logo nằm trong tài nguyên ứng dụng.

## Giao diện CleanYourTV

Giao diện sử dụng bố cục `launcher/app/src/main/res/layout/activity_main.xml`, font Outfit, đồng hồ/ngày tháng, thanh nguồn vào/mạng/cài đặt, thẻ ứng dụng banner thật và khung dock bo tròn từ mã nguồn CleanYourTV của anh. Nền đỏ–đen tĩnh được bổ sung để gần với ảnh TV tham chiếu mà không dùng hiệu ứng blur nặng.

- **Hàng 1–2:** **thẻ ngang 16:9** đúng kiểu ảnh tham chiếu của anh: ưu tiên **banner của activity Android TV Leanback** (không chỉ banner của package), bo tròn cả ảnh, viền focus trắng và phóng nhẹ. Khi app không cung cấp banner ngang, icon thật được căn giữa và phóng lớn trong thẻ sáng, không bị kéo méo. Ứng dụng do người dùng thêm, mặc định có **IPTV Việt Nam** và dấu **+**. Giữ OK/Menu hoặc chọn quản lý để sắp xếp, chuyển vị trí hoặc gỡ shortcut (không gỡ APK).
- **Hàng 3–4:** kênh xem nhanh hiển thị **logo kênh thật** (không phải tên chữ), chạm OK để phát; tên hiện khi focus. Đã đóng gói **46 logo** vào APK, tải offline ngay khi khởi động.
- **Mặc định 5 ô/hàng**, giống TV trong ảnh; vào Quản lý → **Số cột mỗi hàng** để chọn 6 ô/hàng. Bố cục vẫn gồm 4 hàng.
- Có màn hình toàn bộ kênh theo nhóm và điều khiển đổi kênh bằng remote.

## Playlist riêng — cập nhật không cần build APK

- **[Sửa playlist M3U tại GitHub](https://github.com/tendoodn/iptv/edit/main/playlist/vietnam.m3u)**
- **[Link M3U ổn định](https://raw.githubusercontent.com/tendoodn/iptv/main/playlist/vietnam.m3u)**

Ứng dụng kiểm tra danh sách mới theo chu kỳ khoảng 4 giờ khi vào Home; có tùy chọn đồng bộ thủ công. Có bản sao M3U để dùng khi mất mạng.

Mỗi kênh có thể khai báo `tvg-id` và `tvg-logo`. Logo 46 kênh đi kèm APK để hiện ngay; kênh mới thêm có thể tải hình theo `tvg-logo` và lưu cache. Nếu thay logo cố định đã đóng gói, cần build bản APK mới để thay bản offline.

Trình phát sử dụng **Media3 ExoPlayer + HLS**, hỗ trợ thử URL dự phòng cùng kênh; **chưa có kiểm chứng video phát trên TV Box thật**. Nguồn IPTV công khai có thể thay đổi, giới hạn vùng, yêu cầu cookie hoặc ngừng hoạt động. Việc HTTP trả manifest không bảo đảm phát được video.

## Đặt làm màn hình HOME mặc định

1. Cài bản test4 trên Android TV hoặc TV Box Android 8.0+.
2. Mở **Tendoo IPTV Home** từ danh sách ứng dụng.
3. Chọn đổi ứng dụng HOME bằng hộp thoại Android hoặc vào **Quản lý → Cài đặt HOME mặc định**. Một số TV firmware khóa việc đổi HOME.
4. Nhấn HOME trên remote để xác minh. Giữ launcher gốc để có thể chuyển lại.

Test4 và test5 được đóng từ Vercel Sandbox với **cùng SHA-256 chứng chỉ ký debug** (`3c37b1164ee7f7b9241bfad5ed70cd41b366157914700a1bbdad8cb9e3da1346`), đồng thời versionCode tăng từ 4 lên 5, nên có thể thử cài cập nhật trực tiếp. Nếu thiết bị từ chối do firmware hoặc chữ ký bản khác, chỉ gỡ khi cần; gỡ cài đặt sẽ xóa shortcut đã lưu. Không root hoặc vô hiệu hóa launcher gốc.

## Mã nguồn và quy trình build

- `launcher/` — **toàn bộ mã nguồn/resources CleanYourTV gốc** được đồng bộ từ repo của anh và tích hợp patch.
- `custom/java/` — phần HOME 4 hàng, trình phát IPTV, trình duyệt kênh và bộ nhớ logo.
- `custom/logo_pack.json` — dữ liệu nén 46 logo đài truyền hình đóng gói offline.
- `custom/logo_provenance.json` — nguồn ảnh từng kênh.
- `playlist/vietnam.m3u` — playlist riêng có metadata logo, cập nhật từ GitHub.
- `custom/vendor_patch.py` — tạo mã Android từ CleanYourTV và patch tùy chỉnh.
- `scripts/build-vercel-apk.sh` — **đóng APK Android thật bên trong Vercel Sandbox** với JDK 17, Android SDK 35 và Gradle.
- `.github/workflows/sync-cleanyourtv-ui.yml` — chỉ đồng bộ mã nguồn lên repo, không build APK.
- `.github/workflows/publish-vercel-test5.yml` — chỉ đưa **đúng file APK đã build trên Vercel** lên GitHub Releases, có kiểm tra SHA-256; không build lại.

Vercel project: `tendoo-iptv-apk-build`. APK thử nghiệm chưa được nghiệm thu trực tiếp trên thiết bị người dùng. Giấy phép nguồn gốc CleanYourTV lưu tại `CLEAN_YOUR_TV_LICENSE.txt`.
