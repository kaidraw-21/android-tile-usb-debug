# Snap Tiles

Các ô Cài đặt nhanh để bật/tắt cài đặt hệ thống trên Android chỉ bằng một lần chạm — không cần mở ứng dụng Cài đặt.

## Tính năng

- **Ô cố định**: Gỡ lỗi USB, Chế độ nhà phát triển, Trợ năng (luôn bật)
- **Ô tùy chỉnh**: Lên đến 5 ô có thể cấu hình với nhiều hành động trên mỗi ô
- **Nút ô nổi**: Nút lớp phủ có thể kéo, bám vào cạnh, nhấn giữ để mở ứng dụng
- **Bộ nhớ đệm thông minh**: Ghi nhớ các dịch vụ Trợ năng và trạng thái USB, khôi phục khi bật lại
- **Điều khiển hệ thống**: Luôn bật màn hình, Dịch vụ đang chạy, Bố cục từ phải sang trái
- **Gỡ lỗi nâng cao**: Hồ sơ GPU, Chế độ Demo, Tỷ lệ thời gian Animator

## Cập nhật Mới nhất

- **Widget Màn hình chính & Lối tắt**: Triển khai các widget màn hình chính mới và cải tiến lối tắt ứng dụng để truy cập các tính năng nhanh hơn.
- **Cải tiến chung**: Nhiều cập nhật và tối ưu hóa khác nhau.
- **Bản phát hành Mới**: Phiên bản 1.0.2 (build 3) hiện đã có sẵn.

## Ảnh chụp màn hình

| Nút nổi | Bám vào cạnh | Ô tùy chỉnh |
|---|---|---|
| ![Floating Button](media/floating-snap.jpeg) | ![Snap to Edge](media/floating-snap-visual.jpeg) | ![Custom Tiles](media/multiple-and-custom-tile.jpeg) |

## Demo

![Demo](media/howtouse.gif)

## Cài đặt

### Tải về APK

Tải bản phát hành mới nhất từ [Trang bản phát hành](https://github.com/kaidraw-21/android-snap-tiles/blob/main/RELEASES.md).

**[⬇ Tải về snap-tiles-v1.0.2.apk](https://github.com/kaidraw-21/android-snap-tiles/raw/main/download/snap-tiles-v1.0.2.apk)**

### Xây dựng từ mã nguồn

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Cài đặt ADB

### Bật Tùy chọn nhà phát triển và Gỡ lỗi USB

Trước khi tiếp tục, hãy đảm bảo bạn đã bật Tùy chọn nhà phát triển và Gỡ lỗi USB trên thiết bị Android của mình.

1.  **Bật Tùy chọn nhà phát triển**:
    *   Vào `Cài đặt` > `Giới thiệu điện thoại`.
    *   Chạm 7 lần vào `Số bản dựng` cho đến khi bạn thấy thông báo "Bạn đã là nhà phát triển!" hoặc "Đã bật tùy chọn nhà phát triển".
2.  **Bật Gỡ lỗi USB**:
    *   Vào `Cài đặt` > `Hệ thống` > `Tùy chọn nhà phát triển` (hoặc `Cài đặt` > `Tùy chọn nhà phát triển`).
    *   Bật `Gỡ lỗi USB`.

Cấp quyền yêu cầu một lần qua ADB:

```bash
adb shell pm grant com.snap.tiles android.permission.WRITE_SECURE_SETTINGS
```

Sau đó, kéo xuống Cài đặt nhanh → Chỉnh sửa (biểu tượng bút chì) → kéo các ô bạn muốn vào bảng điều khiển của mình.

## Ghi chú

- Cần cấp quyền `WRITE_SECURE_SETTINGS` một lần qua ADB. Sau đó không cần ADB nữa.
- Không yêu cầu root.
- Các ô cố định luôn được bật - không có công tắc bật/tắt.
- Ô Trợ năng lưu vào bộ nhớ đệm các dịch vụ đang hoạt động và khôi phục chúng khi bật lại.

## Lịch sử thay đổi

Xem [RELEASES.md](RELEASES.md) để biết ghi chú phát hành đầy đủ.
