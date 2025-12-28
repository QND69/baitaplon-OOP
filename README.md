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

    * Sử dụng công cụ (cuốc đất, gieo hạt, tưới nước, thu hoạch).
    * Đặt vật phẩm hoặc tương tác với môi trường.
* Chuột phải:

    * Ăn thức ăn.
    * Bật hoặc tắt hàng rào (Fence).
* Phím số (1-9) hoặc cuộn chuột: Chọn công cụ hoặc vật phẩm đang cầm.
* Q: Thả vật phẩm.
* B: Mở hoặc đóng cửa hàng thông qua giao diện GUI trên màn hình.
* J: Mở hoặc đóng bảng nhiệm vụ thông qua giao diện GUI.
* ESC: Mở menu cài đặt hoặc tạm dừng game (GUI).

---

## Hướng dẫn chơi game

### Khởi tạo nhân vật

Khi bắt đầu game, bạn sẽ vào màn hình tạo nhân vật.

* Nhập tên nhân vật.
* Nhấn Start Game để bắt đầu chơi.

### Trồng trọt

Để kiếm tiền và hoàn thành nhiệm vụ, người chơi cần trồng và thu hoạch nông sản:

1. Làm đất: Dùng cuốc (Hoe) lên ô đất cỏ để xới đất.
2. Gieo hạt: Cầm hạt giống và nhấp chuột trái vào ô đất đã xới.
3. Tưới nước: Dùng bình tưới (Watering Can) để tưới cây mỗi ngày. Khi bình hết nước, có thể ra sông để lấy nước.
4. Bón phân: Dùng phân bón (Fertilizer) cho cây đang phát triển để tăng tốc độ lớn.
5. Thu hoạch: Khi cây trưởng thành, dùng tay không hoặc liềm (Scythe) để thu hoạch.
6. Dọn đất:

    * Xẻng (Shovel): Nhổ bỏ cây hoặc nông sản để làm trống ô đất.
    * Rìu (Axe): Chặt cây lấy gỗ (cần chặt nhiều lần).
7. Xây hàng rào: Cầm gỗ (Wood) và nhấp chuột trái để xây hàng rào bảo vệ khu vực trồng trọt.

### Cửa hàng

* Cửa hàng được truy cập thông qua nút hoặc biểu tượng trên giao diện GUI.
* Mua: Hạt giống, công cụ và các vật phẩm nâng cấp.
* Bán: Nông sản đã thu hoạch để nhận vàng (Gold).

### Chăn nuôi

* Mua vật nuôi tại cửa hàng (Shop) và đặt chúng xuống mặt đất.
* Cho ăn: Cầm thức ăn và nhấp chuột vào vật nuôi.
* Thu thập sản phẩm:

    * Bò: Dùng xô sữa (Milk Bucket) để lấy sữa.
    * Cừu: Dùng kéo (Shears) để lấy lông.
    * Gà: Gà tự động đẻ trứng, nhấp chuột để nhặt.
* Giết mổ:

    * Dùng rìu hoặc kiếm để giết vật nuôi, rơi ra thịt tương ứng.

### Hệ thống thời gian và thời tiết

* Thời gian: Game có chu kỳ ngày và đêm. Người chơi cần quản lý thời gian hợp lý.
* Thời tiết: Trời có thể nắng hoặc mưa. Khi trời mưa, cây trồng sẽ được tưới tự động.

### Nhiệm vụ

* Bảng nhiệm vụ được mở thông qua nút hoặc biểu tượng trên giao diện GUI.
* Hoàn thành nhiệm vụ (ví dụ: trồng hoặc thu hoạch đủ số lượng yêu cầu) để nhận phần thưởng như vàng, vật phẩm hoặc năng lượng.

---

## Lưu ý quan trọng

* Theo dõi thanh năng lượng (Stamina) của nhân vật.
* Ăn thức ăn hoặc uống nước tăng lực (Energy Drink) để hồi phục năng lượng khi cần thiết.

---

## Tính năng nổi bật

* Hệ thống Inventory và Hotbar: Quản lý vật phẩm trực quan, thao tác nhanh.
* Lưu và tải game (Save/Load): Đảm bảo không mất tiến trình chơi.
* Hiệu ứng hình ảnh: Hiệu ứng thời tiết, cuốc đất và tưới nước sinh động.
* Âm thanh: Nhạc nền thư giãn và âm thanh tương tác trong game.

---

Chúc bạn có những giờ phút thư giãn và trải nghiệm thú vị cùng game mô phỏng nông trại.
