package com.example.farmSimulation.controller;

import com.example.farmSimulation.config.HotbarConfig;
import com.example.farmSimulation.model.GameManager;
import com.example.farmSimulation.view.MainGameView;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;

@Getter
@Setter
@NoArgsConstructor
public class GameController {
    private MainGameView mainGameView;
    private GameManager gameManager;

    // Hai biến dùng để cập nhật và lưu trữ tọa độ hiện tại của con trỏ chuột
    private double mouseX;
    private double mouseY;

    // Sử dụng HashSet để lưu danh sách các phím đang được giữ
    private HashSet<KeyCode> activeKeys = new HashSet<>();
    /* HashSet là một tập hợp đảm bảo các phần tử không bị trùng lặp.
    KeyCode đại diện cho mã của các phím trên bàn phím (ví dụ: KeyCode.W, KeyCode.SPACE).
    Mục đích của biến này là theo dõi toàn bộ các phím mà người chơi đang nhấn giữ tại một thời điểm.*/

    // Thiết lập các trình lắng nghe sự kiện đầu vào cho Scene
    // MainGameView sẽ gọi phương thức này để chuyển giao quyền xử lý sự kiện của Scene cho Controller
    public void setupInputListeners(Scene scene) {

        // Lắng nghe sự kiện khi người chơi nhấn phím xuống
        scene.setOnKeyPressed(event -> { // Hàm này được kích hoạt ngay khi phím được nhấn
            activeKeys.add(event.getCode()); // Thêm mã phím vừa nhấn vào danh sách đang hoạt động
            /* Lấy mã phím từ sự kiện và đưa vào activeKeys.
            Do đặc tính của HashSet, nếu phím đó đã có trong danh sách thì việc thêm lại sẽ không gây ra lỗi trùng lặp. */

            // Xử lý các phím chức năng hệ thống (luôn hoạt động, ví dụ như phím ESC)
            handleSystemInput(event.getCode());

            // Chặn tất cả các đầu vào khác khi game đang tạm dừng (ví dụ: lúc đang mở Menu cài đặt)
            if (gameManager != null && gameManager.isPaused()) return;

            // Xử lý các phím điều khiển gameplay
            handleGameInput(event.getCode());
        });

        // Lắng nghe sự kiện khi người chơi nhả phím ra
        scene.setOnKeyReleased(event -> { // Hàm này được kích hoạt khi phím được thả ra
            activeKeys.remove(event.getCode()); // Loại bỏ mã phím khỏi danh sách các phím đang hoạt động
        });

        // Cập nhật tọa độ chuột liên tục khi di chuyển
        scene.setOnMouseMoved(event -> {
            this.mouseX = event.getSceneX();
            this.mouseY = event.getSceneY();
        });

        // Đăng ký xử lý sự kiện nhấp chuột
        scene.setOnMouseClicked(this::handleMouseClick);

        // Xử lý sự kiện lăn bánh xe chuột để thay đổi vật phẩm đang chọn
        scene.setOnScroll(event -> {
            if (gameManager == null || gameManager.isPaused()) return; // Không thực hiện cuộn khi game đang tạm dừng

            int currentSlot = gameManager.getMainPlayer().getSelectedHotbarSlot();
            if (event.getDeltaY() < 0) { // Cuộn xuống thì chuyển sang ô vật phẩm kế tiếp
                currentSlot = (currentSlot + 1) % HotbarConfig.HOTBAR_SLOT_COUNT;
            } else if (event.getDeltaY() > 0) { // Cuộn lên thì quay lại ô vật phẩm trước đó
                currentSlot = (currentSlot - 1 + HotbarConfig.HOTBAR_SLOT_COUNT) % HotbarConfig.HOTBAR_SLOT_COUNT;
            }
            gameManager.changeHotbarSlot(currentSlot);
        });
    }

    /**
     * Xử lý các phím hệ thống (Luôn được lắng nghe bất kể trạng thái tạm dừng của game)
     */
    private void handleSystemInput(KeyCode code) {
        if (code == KeyCode.ESCAPE && gameManager != null) {
            gameManager.toggleSettingsMenu(); // Kích hoạt hoặc ẩn menu cài đặt
        }
    }

    /**
     * Xử lý các phím chức năng trong game (Chỉ hoạt động khi game không tạm dừng)
     */
    private void handleGameInput(KeyCode code) {
        if (gameManager == null) return;

        // Phím Q: Ném vật phẩm ra khỏi ô hành trang tại vị trí chuột đang trỏ
        if (code == KeyCode.Q) {
            int slotIndex = gameManager.getHotbarSlotFromMouse(mouseX, mouseY);
            if (slotIndex >= 0) {
                gameManager.dropItemFromHotbar(slotIndex);
            }
        }

        // Phím B: Mở hoặc đóng cửa hàng
        if (code == KeyCode.B && mainGameView != null) {
            mainGameView.toggleShop();
        }

        // Phím M: Chuyển đổi trạng thái thời tiết (chức năng dùng để kiểm thử)
        if (code == KeyCode.M) {
            gameManager.toggleWeather();
        }

        // Phím J: Hiển thị hoặc ẩn bảng nhiệm vụ
        if (code == KeyCode.J && mainGameView != null) {
            mainGameView.toggleQuestBoard();
        }

        // Phím L: Mã gian lận dùng để cộng thêm tiền ngay lập tức
        if (code == KeyCode.L) {
            gameManager.getMainPlayer().addMoney(com.example.farmSimulation.config.GameLogicConfig.CHEAT_MONEY_AMOUNT);
        }

        // Các phím số (0-9): Chọn ô tương ứng trên thanh công cụ
        if (code.isDigitKey()) {
            int slot = getSlotFromDigit(code);
            if (slot != -1) {
                gameManager.changeHotbarSlot(slot);
            }
        }
    }

    /**
     * Chuyển đổi mã phím số sang chỉ số thứ tự của ô chứa đồ (từ 0 đến 9)
     */
    private int getSlotFromDigit(KeyCode code) {
        return switch (code) {
            case DIGIT1 -> 0;
            case DIGIT2 -> 1;
            case DIGIT3 -> 2;
            case DIGIT4 -> 3;
            case DIGIT5 -> 4;
            case DIGIT6 -> 5;
            case DIGIT7 -> 6;
            case DIGIT8 -> 7;
            case DIGIT9 -> 8;
            case DIGIT0 -> 9;
            default -> -1;
        };
    }

    // Phương thức hỗ trợ GameManager kiểm tra xem một phím cụ thể có đang được giữ hay không
    public boolean isKeyPressed(KeyCode key) {
        return activeKeys.contains(key);
    }

    public void handleMouseClick(MouseEvent event) {
        // Chặn mọi tương tác chuột khi game đang tạm dừng (lúc Menu cài đặt đang mở)
        if (gameManager == null || gameManager.isPaused()) return;

        // Xử lý sự kiện chuột phải (Secondary) để đóng/mở hàng rào hoặc ăn thực phẩm
        if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
            // Bước 1: Ưu tiên kiểm tra xem có đang click vào hàng rào để thực hiện đóng/mở hay không
            int tileX = gameManager.getCurrentMouseTileX();
            int tileY = gameManager.getCurrentMouseTileY();

            // Kiểm tra sự tồn tại của hàng rào tại vị trí này
            if (gameManager.hasFenceAt(tileX, tileY)) {
                gameManager.toggleFence(tileX, tileY);
            } else {
                // Bước 2: Nếu không phải hàng rào, kiểm tra xem nhân vật có đang cầm thức ăn không để ăn
                gameManager.handlePlayerEating();
            }
            return;
        }

        // Xử lý sự kiện chuột trái (Primary) cho các tương tác mặc định khác
        if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
        // Gửi yêu cầu tương tác đến GameManager để xử lý logic (đưa hành động vào hàng đợi)
        gameManager.interactWithTile(
                gameManager.getCurrentMouseTileX(),
                gameManager.getCurrentMouseTileY()
        );
    }
}