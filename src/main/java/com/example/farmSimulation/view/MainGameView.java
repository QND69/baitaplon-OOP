package com.example.farmSimulation.view;

import com.example.farmSimulation.config.*;
import com.example.farmSimulation.model.GameManager;
import com.example.farmSimulation.controller.GameController;
import com.example.farmSimulation.model.ItemStack;
import com.example.farmSimulation.model.ItemType;
import com.example.farmSimulation.model.WorldMap;
import com.example.farmSimulation.view.assets.ImageManager;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MainGameView {

    // ==============================================================================================
    // KHAI BÁO BIẾN VÀ CÁC THÀNH PHẦN GIAO DIỆN
    // ==============================================================================================

    // Các thành phần phụ thuộc cốt lõi
    private final ImageManager assetManager; // Quản lý tài nguyên hình ảnh
    private final WorldMap worldMap;         // Dữ liệu bản đồ để vẽ các ô gạch
    private GameManager gameManager;

    // Pane gốc chứa toàn bộ giao diện
    private Pane rootPane;

    // Các thành phần View con
    private WorldRenderer worldRenderer;
    private HudView hudView;
    private SettingsMenuView settingsMenu;
    private HotbarView hotbarView;
    private ShopView shopView;             // Giao diện cửa hàng
    private QuestBoardView questBoardView; // Giao diện bảng nhiệm vụ

    // Pane tĩnh chứa các thực thể động vật
    private Pane entityPane;

    // Pane hiệu ứng thời tiết
    private WeatherEffectView weatherEffectView;

    // Trình quản lý hiệu ứng hình ảnh
    private final VisualEffectManager visualEffectManager;

    // Lớp phủ giao diện khi trò chơi kết thúc
    private StackPane gameOverOverlay;

    // ==============================================================================================
    // KHỞI TẠO VÀ THIẾT LẬP (INITIALIZATION)
    // ==============================================================================================

    /**
     * Hàm khởi tạo nhận các thành phần phụ thuộc cần thiết
     */
    public MainGameView(ImageManager assetManager, WorldMap worldMap, HotbarView hotbarView) {
        this.assetManager = assetManager;
        this.worldMap = worldMap;
        this.hotbarView = hotbarView;
        this.visualEffectManager = new VisualEffectManager();
    }

    /**
     * Khởi tạo giao diện người dùng, nhận Controller và Sprite người chơi từ bên ngoài.
     * Thiết lập các lớp hiển thị (layer) và vị trí ban đầu.
     */
    public void initUI(Stage primaryStage, GameController gameController, Pane playerSpriteContainer,
                       Rectangle debugBox, Circle debugDot, Circle debugRangeCircle, Rectangle debugCollisionHitbox) {
        this.rootPane = new Pane();

        // Khởi tạo pane tĩnh chứa động vật
        this.entityPane = new Pane();
        this.entityPane.setPrefSize(WindowConfig.SCREEN_WIDTH, WindowConfig.SCREEN_HEIGHT);
        this.entityPane.setMouseTransparent(true); // Cho phép chuột click xuyên qua xuống đất

        // Khởi tạo các View con
        this.worldRenderer = new WorldRenderer(assetManager, worldMap, entityPane);
        this.hudView = new HudView();

        // SettingsMenu cần được khởi tạo. Chúng ta truyền gameManager vào đây.
        // Lưu ý: HudView sẽ được thiết lập gameManager sau.
        this.settingsMenu = new SettingsMenuView(this.gameManager);

        // Khởi tạo hiệu ứng thời tiết
        this.weatherEffectView = new WeatherEffectView();

        // Tạo giao diện Game Over
        createGameOverOverlay();

        // Thêm các thành phần vào rootPane theo thứ tự lớp hiển thị từ dưới lên trên
        rootPane.getChildren().addAll(
                worldRenderer.getWorldPane(),    // Lớp 1: Bản đồ (Đất/Cây)
                worldRenderer.getTileSelector(), // Lớp 2: Ô chọn
                worldRenderer.getGhostPlacement(), // Bóng mờ hiển thị vị trí đặt vật phẩm
                entityPane,                      // Lớp 3: Động vật
                playerSpriteContainer,           // Lớp 4: Khung chứa nhân vật
                weatherEffectView,               // Lớp 4.5: Hiệu ứng thời tiết (mưa)
                hudView,                         // Lớp 5: HUD (Đồng hồ, Text, độ tối)
                hotbarView,                      // Lớp 6: Thanh công cụ
                settingsMenu,                    // Lớp 7: Menu cài đặt (mặc định ẩn)
                gameOverOverlay                  // Lớp 8: Giao diện kết thúc game (mặc định ẩn)
        );

        // Đặt nhân vật vào giữa màn hình
        // Tính toán vị trí dựa trên kích thước sau khi đã phóng to/thu nhỏ để căn giữa chính xác hơn
        double scaledWidth = PlayerSpriteConfig.BASE_PLAYER_FRAME_WIDTH * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
        double scaledHeight = PlayerSpriteConfig.BASE_PLAYER_FRAME_HEIGHT * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
        playerSpriteContainer.setLayoutX(WindowConfig.SCREEN_WIDTH / 2 - scaledWidth / 2);
        playerSpriteContainer.setLayoutY(WindowConfig.SCREEN_HEIGHT / 2 - scaledHeight / 2);

        // Thiết lập hiển thị gỡ lỗi (Debug) nếu được kích hoạt trong cấu hình
        if (PlayerSpriteConfig.DEBUG_PLAYER_BOUNDS) {
            // Thêm các nút debug vào rootPane (lớp trên cùng)
            rootPane.getChildren().addAll(debugDot, debugRangeCircle, debugCollisionHitbox);

            // Gắn tâm chấm debug vào tâm logic của nhân vật
            double logicCenterX = scaledWidth / 2;

            // Cộng thêm offset Y để hiển thị tâm debug đúng vị trí tương tác
            double logicCenterY = (scaledHeight / 2) + PlayerSpriteConfig.INTERACTION_CENTER_Y_OFFSET;

            debugDot.layoutXProperty().bind(playerSpriteContainer.layoutXProperty().add(logicCenterX));
            debugDot.layoutYProperty().bind(playerSpriteContainer.layoutYProperty().add(logicCenterY));

            // Gắn vòng tròn phạm vi tương tác
            debugRangeCircle.layoutXProperty().bind(playerSpriteContainer.layoutXProperty().add(logicCenterX));
            debugRangeCircle.layoutYProperty().bind(playerSpriteContainer.layoutYProperty().add(logicCenterY));

            // Vùng va chạm sẽ được cập nhật động trong phương thức updateCollisionHitbox
        }

        Scene scene = new Scene(rootPane, WindowConfig.SCREEN_WIDTH, WindowConfig.SCREEN_HEIGHT, WindowConfig.BACKGROUND_COLOR);
        gameController.setupInputListeners(scene);

        primaryStage.setTitle(WindowConfig.GAME_TITLE);
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Thiết lập GameManager và khởi tạo các thành phần phụ thuộc vào nó (Shop, Quest, Listener).
     * Phương thức này được gọi từ Game.java sau khi khởi tạo xong GameManager.
     */
    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;

        // Thiết lập tham chiếu cho HudView
        if (hudView != null) {
            hudView.setGameManager(gameManager);
            hudView.setMainGameView(this);
            hudView.setAssetManager(assetManager);
        }

        // Thiết lập sự kiện thả vật phẩm từ Hotbar (để xóa vật phẩm vào thùng rác)
        if (hotbarView != null) {
            hotbarView.setOnItemDropListener((slotIndex, scenePoint) -> {
                // Kiểm tra xem có thả vào thùng rác không
                if (hudView != null && hudView.isMouseOverTrash(scenePoint.getX(), scenePoint.getY())) {
                    // Xóa toàn bộ stack vật phẩm tại slot này
                    if (gameManager != null && gameManager.getMainPlayer() != null) {
                        ItemStack[] items = gameManager.getMainPlayer().getHotbarItems();
                        if (slotIndex >= 0 && slotIndex < items.length && items[slotIndex] != null) {
                            items[slotIndex] = null; // Xóa khỏi túi đồ
                            updateHotbar(); // Cập nhật lại hiển thị
                            return true; // Xác nhận vật phẩm đã bị xóa
                        }
                    }
                }
                return false; // Không thả vào thùng rác
            });
        }

        // Thiết lập GameManager cho SettingsMenuView để các chức năng Resume và Brightness hoạt động đúng
        if (settingsMenu != null) {
            settingsMenu.setGameManager(gameManager);
        }

        // Khởi tạo ShopView sau khi đã có GameManager
        if (gameManager != null && gameManager.getShopManager() != null) {
            this.shopView = new ShopView(gameManager.getShopManager(), assetManager);
            // Đặt kích thước cố định để đảm bảo giao diện cửa hàng không bị co lại
            shopView.setPrefSize(ShopConfig.SHOP_WIDTH, ShopConfig.SHOP_HEIGHT);
            shopView.setMaxSize(ShopConfig.SHOP_WIDTH, ShopConfig.SHOP_HEIGHT);
            // Căn giữa cửa hàng trên màn hình
            shopView.setLayoutX((WindowConfig.SCREEN_WIDTH - ShopConfig.SHOP_WIDTH) / 2);
            shopView.setLayoutY((WindowConfig.SCREEN_HEIGHT - ShopConfig.SHOP_HEIGHT) / 2);
            // Thêm shopView vào rootPane và đưa lên lớp trên cùng
            rootPane.getChildren().add(shopView);
            shopView.toFront();
        }

        // Khởi tạo QuestBoardView sau khi đã có GameManager
        if (gameManager != null && gameManager.getQuestManager() != null && gameManager.getMainPlayer() != null) {
            this.questBoardView = new QuestBoardView(gameManager.getQuestManager(), gameManager.getMainPlayer());
            // Căn giữa bảng nhiệm vụ trên màn hình
            questBoardView.setLayoutX((WindowConfig.SCREEN_WIDTH - QuestConfig.QUEST_BOARD_WIDTH) / 2);
            questBoardView.setLayoutY((WindowConfig.SCREEN_HEIGHT - QuestConfig.QUEST_BOARD_HEIGHT) / 2);
            // Thêm questBoardView vào rootPane và đưa lên lớp trên cùng
            rootPane.getChildren().add(questBoardView);
            questBoardView.toFront();
        }
    }

    // ==============================================================================================
    // CÁC HÀM CẬP NHẬT HIỂN THỊ CHÍNH (CORE RENDER & UPDATES)
    // ==============================================================================================

    /**
     * Cập nhật vị trí hiển thị của bản đồ
     */
    public void updateMap(double worldOffsetX, double worldOffsetY, boolean forceRedraw) {
        worldRenderer.updateMap(worldOffsetX, worldOffsetY, forceRedraw);
    }

    /**
     * Cập nhật vị trí ô được chọn trên bản đồ
     */
    public void updateSelector(int tileSelectedX, int tileSelectedY, double worldOffsetX, double worldOffsetY) {
        worldRenderer.updateSelector(tileSelectedX, tileSelectedY, worldOffsetX, worldOffsetY);
    }

    /**
     * Cập nhật hiển thị bóng mờ của vật phẩm khi chuẩn bị đặt
     */
    public void updateGhostPlacement(int tileX, int tileY, double worldOffsetX, double worldOffsetY, ItemStack currentItem) {
        if (worldRenderer != null) {
            worldRenderer.updateGhostPlacement(tileX, tileY, worldOffsetX, worldOffsetY, currentItem);
        }
    }

    /**
     * Cập nhật hiển thị danh sách động vật
     */
    public void updateAnimals(java.util.List<com.example.farmSimulation.model.Animal> animals, double worldOffsetX, double worldOffsetY) {
        if (worldRenderer != null) {
            worldRenderer.updateAnimals(animals, worldOffsetX, worldOffsetY);
        }
    }

    /**
     * Cập nhật thông tin thời gian hiển thị
     */
    public void updateTimer(int day, String timeString) {
        hudView.updateTimer(day, timeString);
    }

    /**
     * Cập nhật mức độ ánh sáng (chu kỳ ngày đêm)
     */
    public void updateLighting(double intensity) {
        hudView.updateLighting(intensity);
    }

    /**
     * Cập nhật hiển thị thanh công cụ (Hotbar)
     */
    public void updateHotbar() {
        hotbarView.updateView();
    }

    /**
     * Cập nhật số tiền hiển thị của người chơi
     */
    public void updateMoneyDisplay(double amount) {
        if (hudView != null) {
            hudView.updateMoney(amount);
        }
    }

    /**
     * Hiển thị thông báo tạm thời trên đầu nhân vật
     */
    public void showTemporaryText(String message, double playerScreenX, double playerScreenY) {
        // Căn lề văn bản dựa trên chiều rộng cơ sở đã được điều chỉnh tỷ lệ
        double scaledWidth = PlayerSpriteConfig.BASE_PLAYER_FRAME_WIDTH * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
        double playerCenterX = playerScreenX + scaledWidth / 2;
        hudView.showTemporaryText(message, playerCenterX, playerScreenY);
    }

    // ==============================================================================================
    // QUẢN LÝ HIỆU ỨNG HÌNH ẢNH (VISUAL EFFECTS)
    // ==============================================================================================

    /**
     * Cập nhật hiệu ứng thời tiết và ánh sáng liên quan
     */
    public void updateWeather(boolean isRaining) {
        if (weatherEffectView != null) {
            // Chỉ đặt trạng thái mưa khi có thay đổi
            weatherEffectView.setRaining(isRaining);

            // Cập nhật hoạt ảnh mưa mỗi khung hình nếu đang mưa
            if (isRaining) {
                weatherEffectView.updateRain();
            }
        }

        // Làm tối màn hình một chút khi trời mưa để tăng tính chân thực
        if (hudView != null) {
            double currentIntensity = 1.0 - hudView.getDarknessOverlay().getOpacity();
            double rainDarkness = isRaining ? com.example.farmSimulation.config.WeatherConfig.RAIN_DARKNESS_OPACITY : 0.0;
            double newOpacity = Math.min(1.0 - currentIntensity + rainDarkness,
                    GameLogicConfig.MAX_DARKNESS_OPACITY);
            hudView.getDarknessOverlay().setOpacity(newOpacity);
        }
    }

    /**
     * Hiển thị hoạt ảnh thu hoạch vật phẩm bay về túi đồ
     */
    public void playHarvestAnimation(ItemType itemType, int col, int row, double worldOffsetX, double worldOffsetY) {
        // Xác định tọa độ bắt đầu tại ô đất và căn giữa biểu tượng vào ô
        double startX = col * WorldConfig.TILE_SIZE + worldOffsetX + (WorldConfig.TILE_SIZE - HudConfig.HARVEST_ICON_SIZE) / 2;
        double startY = row * WorldConfig.TILE_SIZE + worldOffsetY + (WorldConfig.TILE_SIZE - HudConfig.HARVEST_ICON_SIZE) / 2;

        // Xác định tọa độ đích là ô chứa vật phẩm trong Hotbar
        int targetSlotIndex = findSlotIndexForItem(itemType);

        // Mặc định bay về giữa màn hình dưới nếu không tìm thấy vị trí cụ thể
        double endX = WindowConfig.SCREEN_WIDTH / 2;
        double endY = WindowConfig.SCREEN_HEIGHT - 50;

        if (targetSlotIndex != -1) {
            Point2D slotCenter = hotbarView.getSlotCenter(targetSlotIndex);
            if (slotCenter != null) {
                endX = slotCenter.getX() - (HudConfig.HARVEST_ICON_SIZE / 2); // Căn chỉnh tâm
                endY = slotCenter.getY() - (HudConfig.HARVEST_ICON_SIZE / 2);
            }
        }

        // Gọi trình quản lý xử lý hiệu ứng bay
        visualEffectManager.playItemFlyAnimation(
                rootPane,
                assetManager.getItemIcon(itemType),
                startX, startY,
                endX, endY
        );
    }

    /**
     * Hàm hỗ trợ tìm vị trí slot chứa vật phẩm để làm đích đến cho hiệu ứng bay
     */
    private int findSlotIndexForItem(ItemType type) {
        if (gameManager == null || gameManager.getMainPlayer() == null) return -1;

        ItemStack[] items = gameManager.getMainPlayer().getHotbarItems();
        int selectedSlot = gameManager.getMainPlayer().getSelectedHotbarSlot();

        // Ưu tiên: Nếu slot đang chọn có chứa vật phẩm này thì bay về đây
        if (items[selectedSlot] != null && items[selectedSlot].getItemType() == type) {
            return selectedSlot;
        }

        // Ưu tiên: Tìm slot đầu tiên chứa vật phẩm này
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && items[i].getItemType() == type) {
                return i;
            }
        }

        return selectedSlot; // Mặc định trả về slot đang chọn nếu không tìm thấy
    }

    // ==============================================================================================
    // QUẢN LÝ GIAO DIỆN NGƯỜI DÙNG (MENUS, SHOP, QUESTS, GAME OVER)
    // ==============================================================================================

    /**
     * Hiển thị menu cài đặt với thông tin người chơi
     */
    public void showSettingsMenu(String playerName, int playerLevel) {
        settingsMenu.updatePlayerInfo(playerName, playerLevel);
        settingsMenu.show();
    }

    /**
     * Ẩn menu cài đặt
     */
    public void hideSettingsMenu() {
        settingsMenu.hide();
    }

    /**
     * Bật/Tắt hiển thị cửa hàng.
     * Tự động đóng Menu Cài đặt nếu đang mở để tránh chồng chéo giao diện.
     */
    public void toggleShop() {
        if (shopView != null) {
            boolean wasVisible = shopView.isShopVisible();
            shopView.toggle();

            // Nếu cửa hàng được mở, cần đóng Menu Cài đặt nếu nó đang hiển thị
            if (!wasVisible && shopView.isShopVisible()) {
                if (settingsMenu != null && settingsMenu.isVisible()) {
                    hideSettingsMenu();
                    // Tiếp tục vòng lặp game nếu đang bị tạm dừng bởi Settings
                    if (gameManager != null && gameManager.isPaused()) {
                        gameManager.setPaused(false);
                        if (gameManager.getGameLoop() != null) {
                            gameManager.getGameLoop().start();
                        }
                    }
                }
            }
        }
    }

    /**
     * Kiểm tra xem cửa hàng có đang hiển thị hay không
     */
    public boolean isShopVisible() {
        return shopView != null && shopView.isShopVisible();
    }

    /**
     * Bật/Tắt hiển thị Bảng nhiệm vụ
     */
    public void toggleQuestBoard() {
        if (questBoardView != null) {
            boolean wasVisible = questBoardView.isQuestBoardVisible();
            questBoardView.toggle();

            // Nếu bảng nhiệm vụ được mở, đóng Menu Cài đặt và Cửa hàng nếu đang mở
            if (!wasVisible && questBoardView.isQuestBoardVisible()) {
                if (settingsMenu != null && settingsMenu.isVisible()) {
                    hideSettingsMenu();
                }
                if (shopView != null && shopView.isShopVisible()) {
                    shopView.toggle(); // Đóng cửa hàng
                }
                // Bảng nhiệm vụ không làm tạm dừng game, game vẫn chạy nền
            }

            questBoardView.toFront();
        }
    }

    /**
     * Kiểm tra xem bảng nhiệm vụ có đang hiển thị hay không
     */
    public boolean isQuestBoardVisible() {
        return questBoardView != null && questBoardView.isQuestBoardVisible();
    }

    /**
     * Tạo giao diện lớp phủ Game Over
     */
    private void createGameOverOverlay() {
        gameOverOverlay = new StackPane();
        gameOverOverlay.setPrefSize(WindowConfig.SCREEN_WIDTH, WindowConfig.SCREEN_HEIGHT);
        gameOverOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.8);"); // Nền tối bán trong suốt

        // Hộp chứa nội dung căn giữa
        VBox contentBox = new VBox(30);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setMaxWidth(400);

        // Nhãn "GAME OVER" font lớn, màu đỏ viền trắng
        Label gameOverLabel = new Label("GAME OVER");
        gameOverLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        gameOverLabel.setTextFill(Color.WHITE);
        gameOverLabel.setStyle("-fx-effect: dropshadow(one-pass-box, red, 5, 0, 0, 0);");

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        // Nút quay về màn hình chính
        Button restartButton = new Button("MAIN MENU");
        restartButton.setPrefSize(150, 50);
        restartButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        restartButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        restartButton.setOnAction(e -> {
            if (gameManager != null) {
                gameManager.returnToMainMenu();
            }
        });

        // Nút thoát game
        Button quitButton = new Button("QUIT");
        quitButton.setPrefSize(150, 50);
        quitButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        quitButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        quitButton.setOnAction(e -> Platform.exit());

        buttonBox.getChildren().addAll(restartButton, quitButton);
        contentBox.getChildren().addAll(gameOverLabel, buttonBox);

        gameOverOverlay.getChildren().add(contentBox);

        // Mặc định ẩn
        gameOverOverlay.setVisible(false);
    }

    /**
     * Hiển thị giao diện Game Over
     */
    public void showGameOverUI() {
        if (gameOverOverlay != null) {
            gameOverOverlay.setVisible(true);
            gameOverOverlay.toFront(); // Đảm bảo lớp phủ nằm trên cùng
        }
    }

    /**
     * Ẩn giao diện Game Over
     */
    public void hideGameOverUI() {
        if (gameOverOverlay != null) {
            gameOverOverlay.setVisible(false);
        }
    }

    // ==============================================================================================
    // CÁC HÀM HỖ TRỢ DEBUG
    // ==============================================================================================

    /**
     * Cập nhật vị trí và kích thước của vùng va chạm (Hitbox) dùng cho mục đích debug
     */
    public void updateCollisionHitbox(double playerWorldX, double playerWorldY, double worldOffsetX, double worldOffsetY, javafx.scene.shape.Rectangle debugCollisionHitbox) {
        if (debugCollisionHitbox != null && PlayerSpriteConfig.DEBUG_PLAYER_BOUNDS) {
            // 1. Cập nhật kích thước vùng va chạm theo cấu hình
            if (debugCollisionHitbox.getWidth() != PlayerSpriteConfig.COLLISION_BOX_WIDTH) {
                debugCollisionHitbox.setWidth(PlayerSpriteConfig.COLLISION_BOX_WIDTH);
                debugCollisionHitbox.setHeight(PlayerSpriteConfig.COLLISION_BOX_HEIGHT);
            }

            // 2. Tính toán vị trí hiển thị trên màn hình
            double screenX = playerWorldX + worldOffsetX;
            double screenY = playerWorldY + worldOffsetY;

            // 3. Căn chỉnh vị trí:
            // Nhân vật gốc rộng 192x192 nhưng đã được thu nhỏ tỷ lệ 0.6
            // Kích thước thực tế hiển thị = 192 * 0.6 = 115.2

            double scaledPlayerWidth = PlayerSpriteConfig.BASE_PLAYER_FRAME_WIDTH * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
            double scaledPlayerHeight = PlayerSpriteConfig.BASE_PLAYER_FRAME_HEIGHT * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;

            // Offset X: Căn giữa hitbox theo chiều ngang của nhân vật
            double offsetX = (scaledPlayerWidth - PlayerSpriteConfig.COLLISION_BOX_WIDTH) / 2;

            // Offset Y: Căn xuống dưới chân nhân vật
            // Logic: Chiều cao đã điều chỉnh tỷ lệ trừ đi chiều cao vùng va chạm và khoảng cách đệm phía dưới
            double offsetY = scaledPlayerHeight
                    - PlayerSpriteConfig.COLLISION_BOX_HEIGHT
                    - PlayerSpriteConfig.COLLISION_BOX_BOTTOM_PADDING;

            debugCollisionHitbox.setLayoutX(screenX + offsetX);
            debugCollisionHitbox.setLayoutY(screenY + offsetY);

            debugCollisionHitbox.setVisible(true);
        } else if (debugCollisionHitbox != null) {
            debugCollisionHitbox.setVisible(false);
        }
    }
}