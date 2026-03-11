# USB Debug Tile

Toggle USB Debugging ngay từ Quick Settings — không cần vào Settings app.

## Vấn đề

Các app ngân hàng Việt Nam (VCB, MB, Techcombank...) chặn chạy khi bật USB Debugging. Mỗi lần muốn dùng app ngân hàng lại phải:

> Settings → Developer Options → USB Debugging → tắt → mở app → bật lại

Tile này giải quyết bằng 1 tap từ thanh thông báo.

## Yêu cầu

- Android 7.0+ (API 24)
- ADB (chỉ cần 1 lần duy nhất để grant permission)

## Cài đặt

**Cách 1: Tải APK trực tiếp**

Tải file [tile-debug-usb.apk](download/tile-debug-usb.apk) rồi cài trực tiếp trên điện thoại.

**Cách 2: Build từ source**

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

**2. Grant permission (1 lần duy nhất)**

```bash
adb shell pm grant com.usb.tiledebug android.permission.WRITE_SECURE_SETTINGS
```

**3. Thêm tile vào Quick Settings**

Kéo thanh thông báo xuống → nhấn nút Edit (bút chì) → tìm **"USB Debugging"** → kéo vào.

## Demo

![Demo](media/howtouse.gif)

## Sử dụng

Tap tile để bật/tắt USB Debugging ngay lập tức. Tile hiển thị trạng thái hiện tại (On/Off).

## Lưu ý

- Permission `WRITE_SECURE_SETTINGS` không thể grant qua UI, bắt buộc dùng ADB **một lần** khi cài. Sau đó không cần ADB nữa.
- App không có giao diện, chỉ có tile.
- Không yêu cầu root.
