# CampusExpense - Ứng dụng Quản lý Chi tiêu Sinh viên

**CampusExpense** là ứng dụng Android dành cho sinh viên và người dùng cá nhân muốn quản lý tài chính hằng ngày một cách trực quan và khoa học. Dự án tập trung vào:

*    Quản lý ngân sách theo danh mục và theo tháng.
*    Theo dõi chi tiêu, lịch sử giao dịch và hóa đơn định kỳ.
*    Phân tích biểu đồ chi tiêu, nhắc nhở thanh toán và hỗ trợ tiền tệ VND/USD.

---

## 🌟 Tính năng chính

### 1. Đăng ký & Đăng nhập
*    Hệ thống đăng ký, đăng nhập nội bộ sử dụng Room Database.
*    Dữ liệu chi tiêu của mỗi tài khoản được lưu riêng biệt.
*    Kiểm tra hợp lệ biểu mẫu ngay khi nhập thông tin.

### 2. Quản lý ngân sách
*    Thiết lập ngân sách cho từng danh mục (Ăn uống, Học tập, Đi lại, ...).
*    Lập ngân sách theo tháng và theo dõi tiến độ chi tiêu.
*    Cho phép điều chỉnh ngân sách giữa các danh mục (transfer).
*    Tự động cộng dồn số dư ngân sách của tháng trước sang tháng sau.

### 3. Theo dõi chi tiêu
*    Ghi chép giao dịch nhanh với thông tin: số tiền, danh mục, mô tả và ngày tháng.
*    Hiển thị danh sách giao dịch theo danh mục và theo ngày.
*    Lưu giao dịch yêu thích để tạo nhanh khi dùng lại.
*    Cảnh báo khi chi tiêu vượt quá hạn mức ngân sách.

### 4. Nhắc nhở hóa đơn
*    Lên lịch hóa đơn định kỳ và nhắc thanh toán.
*    Sử dụng `AlarmManager` và `BroadcastReceiver` để gửi thông báo khi đến hạn.
*    Hiển thị số lượng hóa đơn chưa thanh toán trên badge ứng dụng.
*    Cho phép đánh dấu hóa đơn đã thanh toán và tạo giao dịch chi tiêu tương ứng.

### 5. Phân tích và biểu đồ
*    Sử dụng `MPAndroidChart` để hiển thị biểu đồ chi tiêu trực quan.
*    So sánh chi tiêu thực tế với mục tiêu ngân sách.
*    Thống kê theo danh mục và theo khoảng thời gian.

### 6. Cài đặt cá nhân hóa
*    Hỗ trợ đa tiền tệ: VND và USD.
*    Đồng bộ tỷ giá USD/VND qua API ngoại tuyến.
*    Thay đổi ngôn ngữ hiển thị giữa tiếng Việt và tiếng Anh.
*    Hỗ trợ chế độ giao diện Sáng/Tối.
*    Cho phép đặt lại toàn bộ dữ liệu ứng dụng.

---

## 🛠️ Công nghệ & Kiến trúc

*    Ngôn ngữ: **Java**
*    Nền tảng: **Android Native**
*    Kiến trúc: **MVVM + Repository Pattern**
*    Database: **Room (SQLite)**
*    Min SDK: **33**
*    Target SDK: **36**
*    Java compatibility: **Java 11**

### Thư viện chính

*    `androidx.appcompat`
*    `androidx.constraintlayout`
*    `androidx.cardview`
*    `androidx.lifecycle:viewmodel`
*    `androidx.lifecycle:livedata`
*    `androidx.room:room-runtime`
*    `me.leolin:ShortcutBadger`
*    `com.github.PhilJay:MPAndroidChart`

---

## 📁 Cấu trúc dự án chính

```text
app/
├── src/main/java/com/khanghv/campusexpense
│   ├── CampusExpenseApp.java
│   ├── MainActivity.java
│   ├── base/
│   ├── data/
│   │   ├── database/
│   │   ├── model/
│   │   └── ExpenseRepository.java
│   ├── notifications/
│   ├── ui/
│   │   ├── adapters/
│   │   ├── analytics/
│   │   ├── auth/
│   │   ├── budget/
│   │   ├── category/
│   │   ├── expense/
│   │   ├── fragments/
│   │   └── payment/
│   └── util/
└── src/main/res/
```

---

## 🚀 Hướng dẫn chạy dự án

### 1. Yêu cầu trước khi chạy

*    Android Studio mới nhất (Ladybug / Koala trở lên)
*    JDK 11 hoặc cao hơn
*    Android SDK API 33+

### 2. Mở dự án

*    Mở Android Studio, chọn `File -> Open`, rồi chọn thư mục dự án.
*    Đợi Android Studio đồng bộ Gradle và tải phụ thuộc.

### 3. Chạy ứng dụng

*    Chọn cấu hình `app` và nhấn **Run** hoặc `Shift + F10`.
*    Hoặc chạy từ terminal:

```bash
./gradlew clean assembleDebug
```

### 4. Kiểm thử

*    `./gradlew test` - Chạy unit tests.
*    `./gradlew connectedAndroidTest` - Chạy Android instrumentation tests.

---

## 💡 Gợi ý phát triển

*    Mở rộng chức năng đồng bộ đám mây.
*    Thêm biểu đồ phân tích theo tuần/tháng/quý.
*    Tích hợp đa ngôn ngữ sâu hơn và chuyển tỷ giá theo ngày.
*    Thêm chế độ đăng nhập bảo mật hơn (Firebase, OAuth, mã PIN).

---

## 📌 Thông tin thêm

*    Package name: `com.khanghv.campusexpense`
*    Root project: `CampusExpense`
*    Module app: `:app`

---

## 📝 Đóng góp

Mọi ý kiến đóng góp và báo lỗi vui lòng mở Issue hoặc gửi Pull Request. Chúc bạn phát triển ứng dụng dễ dàng!
