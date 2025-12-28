# Game Mô Phỏng Nông Trại (Java)

Chào mừng bạn đến với game mô phỏng nông trại được viết bằng ngôn ngữ Java. Trò chơi cho phép bạn trải nghiệm cuộc sống của một người nông dân: trồng trọt, chăn nuôi, buôn bán và hoàn thành các nhiệm vụ trong game.

---

## Mục lục

* Yêu cầu hệ thống
* Cách chạy game
* Các phím điều khiển
* Hướng dẫn chơi game
* Tính năng nổi bật

---

## Yêu cầu hệ thống

* Java Development Kit (JDK): Phiên bản 8 trở lên (khuyến nghị JDK 17 hoặc JDK 21).
* Hệ điều hành: Windows, macOS hoặc Linux.
* IDE (khuyến nghị): IntelliJ IDEA, Eclipse hoặc NetBeans để dễ dàng biên dịch và chạy game.

---

## Cách chạy game

### Cách 1: Chạy bằng IDE (khuyến nghị)

1. Mở IDE (IntelliJ IDEA, Eclipse hoặc IDE tương đương).
2. Chọn File > Open (hoặc Import Project) và trỏ đến thư mục chứa mã nguồn (thư mục cha của `farmSimulation`).
3. Tìm file `farmSimulation/Main.java`.
4. Chuột phải vào file `Main.java` và chọn Run.

### Cách 2: Chạy bằng dòng lệnh (Command Line / Terminal)

1. Mở Terminal hoặc Command Prompt.

2. Di chuyển đến thư mục chứa mã nguồn (thư mục chứa folder `farmSimulation`).

3. Biên dịch mã nguồn:

   javac -d bin farmSimulation/Main.java

   Lưu ý: Nếu lệnh trên báo lỗi thiếu file, hãy biên dịch toàn bộ thư mục mã nguồn.

4. Chạy game:

   java -cp bin farmSimulation.Main

---

## Các phím điều khiển

### Di chuyển nhân vật

* W: Di chuyển lên trên.
* S: Di chuyển xuống dưới.
* A: Di chuyển sang trái.
* D: Di chuyển sang phải.

### Tương tác và công cụ

* Chuột trái:

    * Tương tác với ô đất (cuốc đất, gieo hạt, tưới nước).
    * Tương tác với NPC (cửa hàng, nhiệm vụ).
    * Chọn vật phẩm trong túi đồ.
* Phím số (1-9): Chọn nhanh công cụ hoặc vật phẩm trên thanh hotbar.
* E: Mở hoặc đóng túi đồ (Inventory) hoặc tương tác tùy ngữ cảnh.
* ESC: Mở menu cài đặt hoặc tạm dừng game.

---

## Hướng dẫn chơi game

### Khởi tạo nhân vật

Khi bắt đầu game, bạn sẽ vào màn hình tạo nhân vật.

* Nhập tên nhân vật.
* Chọn giao diện (sprite) nhân vật.
* Nhấn Start Game để bắt đầu chơi.

### Trồng trọt (cơ bản)

Để kiếm tiền, người chơi cần trồng và thu hoạch nông sản:

1. Làm đất: Chọn cuốc (Hoe) trên thanh công cụ và nhấp chuột vào ô đất cỏ để xới đất.
2. Gieo hạt: Mua hạt giống tại cửa hàng, chọn hạt giống và nhấp vào ô đất đã xới.
3. Tưới nước: Chọn bình tưới (Watering Can) và nhấp vào ô đất đã gieo hạt. Đất sẽ sẫm màu lại.

Lưu ý: Cây cần được tưới nước mỗi ngày để phát triển.

4. Thu hoạch: Khi cây trưởng thành (hình ảnh thay đổi), nhấp chuột để thu hoạch. Nông sản sẽ được đưa vào túi đồ.

### Cửa hàng

* Tìm NPC bán hàng hoặc khu vực cửa hàng trên bản đồ.
* Nhấp chuột vào cửa hàng để mở giao diện mua bán.
* Mua: Hạt giống, công cụ và các vật phẩm nâng cấp.
* Bán: Nông sản đã thu hoạch để nhận vàng (Gold).

### Chăn nuôi

* Mua gia súc hoặc gia cầm (gà, bò, v.v.) tại cửa hàng chăn nuôi.
* Cho vật nuôi ăn hằng ngày.
* Thu thập sản phẩm như trứng hoặc sữa sau một khoảng thời gian nhất định.

### Hệ thống thời gian và thời tiết

* Thời gian: Game có chu kỳ ngày và đêm. Người chơi cần quản lý thời gian hợp lý.
* Ngủ: Khi trời tối hoặc năng lượng (Stamina) cạn, hãy về nhà và đi ngủ để hồi phục và chuyển sang ngày mới.
* Thời tiết: Trời có thể nắng hoặc mưa. Khi trời mưa, cây trồng sẽ được tưới tự động.

### Nhiệm vụ

* Kiểm tra bảng nhiệm vụ (Quest Board) để nhận thử thách.
* Hoàn thành nhiệm vụ (ví dụ: thu hoạch 10 củ cà rốt) để nhận phần thưởng.

---

## Tính năng nổi bật

* Hệ thống Inventory và Hotbar: Quản lý vật phẩm trực quan, thao tác nhanh.
* Lưu và tải game (Save/Load): Đảm bảo không mất tiến trình chơi.
* Hiệu ứng hình ảnh: Hiệu ứng thời tiết, cuốc đất và tưới nước sinh động.
* Âm thanh: Nhạc nền thư giãn và âm thanh tương tác trong game.

---

Chúc bạn có những giờ phút thư giãn và trải nghiệm thú vị cùng game mô phỏng nông trại.
