package com.example.farmSimulation.model;

import com.example.farmSimulation.config.*;
import com.example.farmSimulation.controller.GameController;
import com.example.farmSimulation.view.MainGameView;
import com.example.farmSimulation.view.PlayerView;
import javafx.animation.AnimationTimer;
import com.example.farmSimulation.model.GameSaveState.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameManager {

    // ==============================================================================================
    // KHAI BÁO CÁC THÀNH PHẦN (FIELDS)
    // ==============================================================================================

    // --- Các thành phần cốt lõi (Model, View, Controller) ---
    private final Player mainPlayer;
    private final WorldMap worldMap;
    private final MainGameView mainGameView;
    private final PlayerView playerView;
    private final GameController gameController;

    // --- Các bộ quản lý chức năng (Sub-Managers) ---
    private final TimeManager timeManager;
    private final ActionManager actionManager;
    private final PlayerMovementHandler movementHandler;
    private final Camera camera;
    private final InteractionManager interactionManager;
    private final CropManager cropManager;
    private final TreeManager treeManager;          // Quản lý cây mọc tự nhiên
    private final FenceManager fenceManager;        // Quản lý hàng rào
    private final CollisionManager collisionManager; // Quản lý va chạm vật lý
    private final AnimalManager animalManager;      // Quản lý vật nuôi
    private final ShopManager shopManager;          // Quản lý cửa hàng
    private final WeatherManager weatherManager;    // Quản lý thời tiết
    private final QuestManager questManager;        // Quản lý nhiệm vụ hàng ngày
    private final com.example.farmSimulation.view.assets.AudioManager audioManager; // Quản lý âm thanh

    // --- Trạng thái Vòng lặp Game (Game Loop State) ---
    private AnimationTimer gameLoop;
    private boolean isPaused = false;
    private long lastUpdateTime = 0;            // Thời điểm cập nhật frame trước đó để tính deltaTime
    private boolean isGameOverSequenceTriggered = false; // Cờ kiểm soát để màn hình Game Over chỉ hiện một lần

    // --- Theo dõi Tọa độ Chuột ---
    private int currentMouseTileX = 0;          // Tọa độ ô lưới (Grid) chuột đang trỏ tới
    private int currentMouseTileY = 0;
    private double currentMouseWorldX = 0;      // Tọa độ thực tế (Pixel) trong thế giới game
    private double currentMouseWorldY = 0;

    // --- Callbacks ---
    private Runnable onReturnToMainMenuHandler; // Hàm gọi lại để chuyển cảnh về Menu chính

    // ==============================================================================================
    // KHỞI TẠO (CONSTRUCTOR)
    // ==============================================================================================

    public GameManager(Player player, WorldMap worldMap, MainGameView mainGameView,
                       PlayerView playerView, GameController gameController) {
        this.mainPlayer = player;
        this.worldMap = worldMap;
        this.mainGameView = mainGameView;
        this.playerView = playerView;
        this.gameController = gameController;

        // Khởi tạo camera và các bộ quản lý thời gian, hành động
        this.camera = new Camera();
        this.timeManager = new TimeManager(mainGameView);
        this.actionManager = new ActionManager(player, playerView);
        this.movementHandler = new PlayerMovementHandler(player, playerView, gameController, camera, mainGameView);

        // Khởi tạo các bộ quản lý môi trường và tương tác
        this.interactionManager = new InteractionManager(this.actionManager);
        this.cropManager = new CropManager(this.worldMap);
        this.treeManager = new TreeManager(this.worldMap);
        this.fenceManager = new FenceManager(this.worldMap);
        this.collisionManager = new CollisionManager(this.worldMap);
        this.animalManager = new AnimalManager(this.worldMap, this.collisionManager);
        this.shopManager = new ShopManager(player);
        this.weatherManager = new WeatherManager();
        this.questManager = new QuestManager();
        this.audioManager = new com.example.farmSimulation.view.assets.AudioManager();

        // Thiết lập liên kết hiển thị thông báo cho người chơi
        player.setMainGameView(mainGameView);

        // Cung cấp tham chiếu Player cho PlayerView để xử lý hoạt ảnh chết
        playerView.setPlayer(player);

        // Thiết lập mối quan hệ phụ thuộc giữa các Manager
        this.actionManager.setFenceManager(this.fenceManager);
        this.actionManager.setAnimalManager(this.animalManager);
        this.actionManager.setQuestManager(this.questManager);

        this.movementHandler.setCollisionManager(this.collisionManager);

        this.interactionManager.setAnimalManager(this.animalManager);
        this.interactionManager.setCollisionManager(this.collisionManager);
        this.interactionManager.setWorldMap(this.worldMap);

        this.cropManager.setWeatherManager(this.weatherManager);
        this.cropManager.setTimeManager(this.timeManager);

        this.shopManager.setQuestManager(this.questManager);
    }

    // ==============================================================================================
    // VÒNG LẶP CHÍNH CỦA GAME (MAIN GAME LOOP)
    // ==============================================================================================

    public void startGame() {
        // Thiết lập vị trí camera ban đầu tập trung vào người chơi
        camera.initializePosition(mainPlayer, playerView);

        // Vẽ bản đồ lần đầu tiên
        mainGameView.updateMap(camera.getWorldOffsetX(), camera.getWorldOffsetY(), true);

        // Khởi tạo và bắt đầu vòng lặp game
        this.gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateGameLogic(now);
            }
        };
        gameLoop.start();

        // Tạo nhiệm vụ hàng ngày khởi đầu nếu danh sách nhiệm vụ đang trống
        if (questManager.getActiveQuests().isEmpty()) {
            questManager.generateDailyQuests();
        }

        System.out.println("Game Started!");
    }

    /**
     * Hàm cập nhật chính, được gọi liên tục mỗi frame.
     * Chịu trách nhiệm điều phối toàn bộ logic game.
     */
    private void updateGameLogic(long now) {
        // Không xử lý logic nếu game đang tạm dừng
        if (this.isPaused) {
            return;
        }

        // Kiểm tra nếu người chơi đã chết thì dừng mọi xử lý logic khác
        if (mainPlayer.getState() == PlayerView.PlayerState.DEAD) {
            playerView.setState(PlayerView.PlayerState.DEAD, mainPlayer.getDirection());

            // Kích hoạt chuỗi sự kiện Game Over một lần duy nhất
            if (!isGameOverSequenceTriggered) {
                isGameOverSequenceTriggered = true;
                triggerGameOverSequence();
            }

            // Chỉ cập nhật hoạt ảnh nhân vật, dừng các logic khác
            playerView.updateAnimation();
            lastUpdateTime = now; // Vẫn cập nhật thời gian để tránh giật lag khi khởi động lại
            return;
        }

        // Tính toán deltaTime (khoảng thời gian giữa 2 frame) bằng giây
        double deltaTime = 0.0;
        if (lastUpdateTime > 0) {
            long deltaNanos = now - lastUpdateTime;
            deltaTime = deltaNanos / 1_000_000_000.0;
            // Giới hạn deltaTime tối đa 0.1s để tránh lỗi vật lý khi máy bị lag
            if (deltaTime > 0.1) {
                deltaTime = 0.1;
            }
        }
        lastUpdateTime = now;

        // 1. Cập nhật hệ thống thời gian
        timeManager.update();

        // Xử lý sự kiện khi bắt đầu ngày mới
        if (timeManager.hasNewDayStarted()) {
            shopManager.generateDailyStock(true); // Làm mới hàng hóa shop
            questManager.generateDailyQuests();   // Tạo nhiệm vụ mới
            System.out.println("New day started! Shop stock refreshed.");
            System.out.println("New daily quests generated!");
        }

        // 2. Logic hồi phục thể lực tự động
        updateStaminaRecovery(deltaTime);

        // 3. Cập nhật di chuyển và trạng thái người chơi
        movementHandler.update(deltaTime);
        playerView.updateAnimation();

        // 4. Xử lý các hành động đang chờ trong hàng đợi
        actionManager.updateTimedActions(worldMap, mainGameView, camera.getWorldOffsetX(), camera.getWorldOffsetY());

        // 5. Cập nhật logic môi trường (Cây trồng, Cây tự nhiên, Động vật)
        boolean cropsUpdated = cropManager.updateCrops(now);
        if (cropsUpdated) {
            actionManager.setMapNeedsUpdate(true);
        }

        boolean treesUpdated = treeManager.updateTrees(now, mainPlayer.getTileX(), mainPlayer.getTileY());
        if (treesUpdated) {
            actionManager.setMapNeedsUpdate(true);
        }

        boolean animalsUpdated = animalManager.updateAnimals(now, mainPlayer);
        if (animalsUpdated) {
            actionManager.setMapNeedsUpdate(true);
        }

        // 6. Cập nhật hiển thị (View)
        mainGameView.updateAnimals(animalManager.getAnimals(), camera.getWorldOffsetX(), camera.getWorldOffsetY());

        weatherManager.updateWeather(now);
        mainGameView.updateWeather(weatherManager.isRaining());

        if (mainGameView.getHudView() != null) {
            mainGameView.getHudView().updatePlayerStats();
            mainGameView.getHudView().updateWeather(weatherManager.isRaining());
        }

        // 7. Cập nhật các thành phần giao diện phụ trợ
        updateMouseSelector();
        updateGhostPlacement();
        updateCollisionHitbox();
        mainGameView.updateMoneyDisplay(mainPlayer.getMoney());
    }

    // ==============================================================================================
    // CÁC HÀM LOGIC PHỤ TRỢ (HELPER LOGIC)
    // ==============================================================================================

    /**
     * Tự động hồi phục hoặc tiêu hao thể lực dựa trên hành động của nhân vật.
     */
    private void updateStaminaRecovery(double deltaTime) {
        PlayerView.PlayerState currentState = mainPlayer.getState();

        if (currentState == PlayerView.PlayerState.WALK) {
            // Chạy làm tiêu hao thể lực
            double drainAmount = GameLogicConfig.STAMINA_DRAIN_RUNNING * deltaTime;
            mainPlayer.reduceStamina(drainAmount);
        } else if (currentState == PlayerView.PlayerState.IDLE) {
            // Đứng yên giúp hồi phục thể lực
            if (mainPlayer.getCurrentStamina() < mainPlayer.getMaxStamina()) {
                double recoveryAmount = GameLogicConfig.STAMINA_RECOVERY_RATE * deltaTime;
                mainPlayer.recoverStamina(recoveryAmount);
            }
        }
        // Các trạng thái khác (Đang làm việc/Bận) sẽ có logic trừ stamina riêng khi thực hiện hành động
    }

    /**
     * Tính toán và cập nhật vị trí ô vuông highlight theo con trỏ chuột.
     */
    private void updateMouseSelector() {
        // Tính tọa độ thực tế của chuột trong thế giới game
        this.currentMouseWorldX = -camera.getWorldOffsetX() + gameController.getMouseX();
        this.currentMouseWorldY = -camera.getWorldOffsetY() + gameController.getMouseY();

        // Quy đổi ra tọa độ ô lưới (Tile)
        this.currentMouseTileX = (int) Math.floor(currentMouseWorldX / WorldConfig.TILE_SIZE);
        this.currentMouseTileY = (int) Math.floor(currentMouseWorldY / WorldConfig.TILE_SIZE);

        mainGameView.updateSelector(
                this.currentMouseTileX,
                this.currentMouseTileY,
                camera.getWorldOffsetX(),
                camera.getWorldOffsetY()
        );
    }

    /**
     * Hiển thị hình ảnh mờ (ghost) của vật phẩm chuẩn bị đặt xuống.
     */
    private void updateGhostPlacement() {
        ItemStack currentItem = mainPlayer.getCurrentItem();
        // Hiện tại logic hiển thị bóng mờ vẫn dựa trên ô lưới (TileX/Y)
        // Nếu muốn bóng mờ di chuyển mượt theo chuột (cho động vật), cần cập nhật WorldRenderer
        mainGameView.updateGhostPlacement(
                this.currentMouseTileX,
                this.currentMouseTileY,
                camera.getWorldOffsetX(),
                camera.getWorldOffsetY(),
                currentItem
        );
    }

    /**
     * Hiển thị khung va chạm (hitbox) khi ở chế độ gỡ lỗi (debug).
     */
    private void updateCollisionHitbox() {
        if (com.example.farmSimulation.config.PlayerSpriteConfig.DEBUG_PLAYER_BOUNDS) {
            mainGameView.updateCollisionHitbox(
                    mainPlayer.getTileX(),
                    mainPlayer.getTileY(),
                    camera.getWorldOffsetX(),
                    camera.getWorldOffsetY(),
                    playerView.getDebugCollisionHitbox()
            );
        }
    }

    // ==============================================================================================
    // XỬ LÝ TƯƠNG TÁC NGƯỜI DÙNG (INTERACTION & INPUT)
    // ==============================================================================================

    /**
     * Kiểm tra xem mục tiêu có nằm trong tầm với của người chơi không.
     */
    private boolean isPlayerInRange(int col, int row, ItemStack currentStack) {
        // Tính tọa độ tâm của nhân vật dựa trên kích thước đã được tỷ lệ hóa
        double scaledPlayerWidth = PlayerSpriteConfig.BASE_PLAYER_FRAME_WIDTH * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
        double scaledPlayerHeight = PlayerSpriteConfig.BASE_PLAYER_FRAME_HEIGHT * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
        double playerX = mainPlayer.getTileX() + scaledPlayerWidth / 2;

        // Điều chỉnh tâm tính toán thấp xuống (ngang hông/chân) để tự nhiên hơn
        double playerY = mainPlayer.getTileY() + scaledPlayerHeight / 2 + PlayerSpriteConfig.INTERACTION_CENTER_Y_OFFSET;

        // Tính tọa độ tâm của ô mục tiêu
        double targetX = (col * WorldConfig.TILE_SIZE) + (WorldConfig.TILE_SIZE / 2.0);
        double targetY = (row * WorldConfig.TILE_SIZE) + (WorldConfig.TILE_SIZE / 2.0);

        double distance = Math.sqrt(Math.pow(playerX - targetX, 2) + Math.pow(playerY - targetY, 2));

        // Tầm với mặc định (tay không)
        double range = GameLogicConfig.HAND_INTERACTION_RANGE * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;

        // Điều chỉnh tầm với tùy theo công cụ đang cầm
        if (currentStack != null) {
            ItemType type = currentStack.getItemType();
            if (type == ItemType.HOE) {
                range = GameLogicConfig.HOE_INTERACTION_RANGE * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
            } else if (type == ItemType.WATERING_CAN) {
                range = GameLogicConfig.WATERING_CAN_INTERACTION_RANGE * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
            } else if (type == ItemType.PICKAXE) {
                range = GameLogicConfig.PICKAXE_INTERACTION_RANGE * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
            } else if (type == ItemType.SHOVEL) {
                range = GameLogicConfig.SHOVEL_INTERACTION_RANGE * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
            } else if (type == ItemType.FERTILIZER) {
                range = GameLogicConfig.FERTILIZER_INTERACTION_RANGE * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
            } else if (type.name().startsWith("SEEDS_")) {
                range = GameLogicConfig.PLANT_INTERACTION_RANGE * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
            } else if (type == ItemType.ITEM_COW || type == ItemType.ITEM_CHICKEN ||
                    type == ItemType.ITEM_PIG || type == ItemType.ITEM_SHEEP ||
                    type == ItemType.EGG) {
                range = AnimalConfig.PLACEMENT_RANGE * WorldConfig.TILE_SIZE * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
            } else {
                range = GameLogicConfig.HAND_INTERACTION_RANGE * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
            }
        }

        return distance <= range;
    }

    /**
     * Cập nhật hướng quay mặt của người chơi về phía mục tiêu.
     */
    private void updatePlayerDirectionTowards(int col, int row) {
        double scaledPlayerWidth = PlayerSpriteConfig.BASE_PLAYER_FRAME_WIDTH * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
        double scaledPlayerHeight = PlayerSpriteConfig.BASE_PLAYER_FRAME_HEIGHT * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
        double playerX = mainPlayer.getTileX() + scaledPlayerWidth / 2;
        double playerY = mainPlayer.getTileY() + scaledPlayerHeight / 2 + PlayerSpriteConfig.INTERACTION_CENTER_Y_OFFSET;

        double targetX = (col * WorldConfig.TILE_SIZE) + (WorldConfig.TILE_SIZE / 2.0);
        double targetY = (row * WorldConfig.TILE_SIZE) + (WorldConfig.TILE_SIZE / 2.0);

        // Tính góc lệch để xác định hướng (Lên, Xuống, Trái, Phải)
        double angleDeg = Math.toDegrees(Math.atan2(targetY - playerY, targetX - playerX));

        if (angleDeg > -45 && angleDeg <= 45) {
            mainPlayer.setDirection(PlayerView.Direction.RIGHT);
        } else if (angleDeg > 45 && angleDeg <= 135) {
            mainPlayer.setDirection(PlayerView.Direction.DOWN);
        } else if (angleDeg > 135 || angleDeg < -135) {
            mainPlayer.setDirection(PlayerView.Direction.LEFT);
        } else {
            mainPlayer.setDirection(PlayerView.Direction.UP);
        }
    }

    /**
     * Xử lý chính khi người chơi click chuột trái vào một ô đất.
     */
    public void interactWithTile(int col, int row) {
        // Chỉ cho phép tương tác khi nhân vật đang rảnh hoặc đang đi bộ
        PlayerView.PlayerState currentState = mainPlayer.getState();
        if (currentState != PlayerView.PlayerState.IDLE && currentState != PlayerView.PlayerState.WALK) {
            return;
        }

        ItemStack currentStack = mainPlayer.getCurrentItem();

        // Kiểm tra khoảng cách
        if (!isPlayerInRange(col, row, currentStack)) {
            double playerScreenX = playerView.getSpriteContainer().getLayoutX();
            double playerScreenY = playerView.getSpriteContainer().getLayoutY() + PlayerSpriteConfig.PLAYER_SPRITE_OFFSET_Y;
            mainGameView.showTemporaryText(HudConfig.TOO_FAR_TEXT, playerScreenX, playerScreenY);
            return;
        }

        // Quay người chơi về hướng mục tiêu
        updatePlayerDirectionTowards(col, row);

        // Sử dụng tọa độ chuột thực tế đã được đồng bộ
        double mouseWorldX = this.currentMouseWorldX;
        double mouseWorldY = this.currentMouseWorldY;

        PlayerView.PlayerState oldState = mainPlayer.getState();

        // 1. Ưu tiên xử lý logic đặt động vật (nếu đang cầm vật phẩm liên quan)
        String animalErrorMsg = interactionManager.processAnimalInteraction(mainPlayer, playerView, mouseWorldX, mouseWorldY);

        if (animalErrorMsg != null) {
            double playerScreenX = playerView.getSpriteContainer().getLayoutX();
            double playerScreenY = playerView.getSpriteContainer().getLayoutY() + PlayerSpriteConfig.PLAYER_SPRITE_OFFSET_Y;
            mainGameView.showTemporaryText(animalErrorMsg, playerScreenX, playerScreenY);
            return;
        }

        // Nếu hành động với động vật thành công (trạng thái nhân vật thay đổi), cập nhật giao diện và dừng lại
        if (mainPlayer.getState() != oldState) {
            mainGameView.updateHotbar();
            return;
        }

        // Cập nhật lại thanh công cụ để đảm bảo đồng bộ số lượng item
        mainGameView.updateHotbar();

        // 2. Nếu không phải tương tác động vật, xử lý tương tác với ô đất (trồng cây, cuốc đất...)
        String errorMsg = interactionManager.processInteraction(mainPlayer, playerView, worldMap, col, row);

        if (errorMsg != null) {
            double playerScreenX = playerView.getSpriteContainer().getLayoutX();
            double playerScreenY = playerView.getSpriteContainer().getLayoutY() + PlayerSpriteConfig.PLAYER_SPRITE_OFFSET_Y;
            mainGameView.showTemporaryText(errorMsg, playerScreenX, playerScreenY);
        }
    }

    /**
     * Mở hoặc đóng cổng rào.
     */
    public void toggleFence(int col, int row) {
        // Kiểm tra tầm với bằng tay không
        if (!isPlayerInRange(col, row, null)) {
            double playerScreenX = playerView.getSpriteContainer().getLayoutX();
            double playerScreenY = playerView.getSpriteContainer().getLayoutY() + PlayerSpriteConfig.PLAYER_SPRITE_OFFSET_Y;
            mainGameView.showTemporaryText(HudConfig.TOO_FAR_TEXT, playerScreenX, playerScreenY);
            return;
        }

        TileData data = worldMap.getTileData(col, row);
        if (data.getFenceData() != null) {
            fenceManager.toggleFence(col, row);
            actionManager.setMapNeedsUpdate(true);
            mainGameView.updateMap(camera.getWorldOffsetX(), camera.getWorldOffsetY(), true);
        }
    }

    /**
     * Kiểm tra nhanh xem ô đất có hàng rào không.
     */
    public boolean hasFenceAt(int col, int row) {
        TileData data = worldMap.getTileData(col, row);
        return data != null && data.getFenceData() != null;
    }

    /**
     * Xử lý logic người chơi ăn thực phẩm.
     */
    public void handlePlayerEating() {
        PlayerView.PlayerState currentState = mainPlayer.getState();
        if (currentState != PlayerView.PlayerState.IDLE && currentState != PlayerView.PlayerState.WALK) {
            return;
        }

        if (mainPlayer.eatCurrentItem()) {
            // Tạo một hành động chờ ảo để giữ trạng thái BUSY trong 0.5 giây
            long eatDurationMs = 500;
            int framesRemaining = (int) (eatDurationMs / (1000.0 / 60.0));

            TimedTileAction eatAction = new TimedTileAction(
                    (int) mainPlayer.getTileX(),
                    (int) mainPlayer.getTileY(),
                    null, // Không thay đổi ô đất
                    framesRemaining,
                    false,
                    -1
            );
            eatAction.setActionState(PlayerView.PlayerState.BUSY);
            actionManager.addPendingAction(eatAction);

            playerView.setState(PlayerView.PlayerState.BUSY, mainPlayer.getDirection());
            mainGameView.updateHotbar();

            double playerScreenX = playerView.getSpriteContainer().getLayoutX();
            double playerScreenY = playerView.getSpriteContainer().getLayoutY() + PlayerSpriteConfig.PLAYER_SPRITE_OFFSET_Y;
            mainGameView.showTemporaryText("Yum!", playerScreenX, playerScreenY);
        }
    }

    // ==============================================================================================
    // QUẢN LÝ THANH CÔNG CỤ (HOTBAR MANAGEMENT)
    // ==============================================================================================

    public void changeHotbarSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < HotbarConfig.HOTBAR_SLOT_COUNT) {
            mainPlayer.setSelectedHotbarSlot(slotIndex);
            mainGameView.updateHotbar();
        }
    }

    public void swapHotbarItems(int indexA, int indexB) {
        mainPlayer.swapHotbarItems(indexA, indexB);
        mainGameView.updateHotbar();
    }

    /**
     * Xác định chỉ số slot của thanh hotbar dựa trên vị trí chuột.
     */
    public int getHotbarSlotFromMouse(double mouseX, double mouseY) {
        if (mainGameView == null || mainGameView.getHotbarView() == null) {
            return -1;
        }
        javafx.geometry.Point2D scenePoint = new javafx.geometry.Point2D(mouseX, mouseY);
        javafx.geometry.Point2D localPoint = mainGameView.getHotbarView().sceneToLocal(scenePoint);
        return mainGameView.getHotbarView().getSlotIndexFromMouse(localPoint.getX(), localPoint.getY());
    }

    /**
     * Ném vật phẩm từ slot chỉ định xuống đất tại vị trí chân người chơi.
     */
    public void dropItemFromHotbar(int slotIndex) {
        PlayerView.PlayerState currentState = mainPlayer.getState();
        if (currentState != PlayerView.PlayerState.IDLE && currentState != PlayerView.PlayerState.WALK) {
            return;
        }

        if (slotIndex < 0 || slotIndex >= mainPlayer.getHotbarItems().length) {
            return;
        }

        ItemStack stackToDrop = mainPlayer.getHotbarItems()[slotIndex];
        if (stackToDrop == null) {
            return;
        }

        // Tính toán vị trí ném (gần chân nhân vật)
        double playerX = mainPlayer.getTileX() + (PlayerSpriteConfig.BASE_PLAYER_FRAME_WIDTH * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE) / 2.0;
        double playerY = mainPlayer.getTileY() + (PlayerSpriteConfig.BASE_PLAYER_FRAME_HEIGHT * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE) - ItemSpriteConfig.ITEM_SPRITE_HEIGHT / 2.0;

        double targetX = playerX;
        double targetY = playerY + WorldConfig.TILE_SIZE * 0.3;

        int targetTileCol = (int) Math.floor(targetX / WorldConfig.TILE_SIZE);
        int targetTileRow = (int) Math.floor(targetY / WorldConfig.TILE_SIZE);

        double offsetX = targetX - (targetTileCol * WorldConfig.TILE_SIZE) - ItemSpriteConfig.ITEM_SPRITE_WIDTH / 2.0;
        double offsetY = targetY - (targetTileRow * WorldConfig.TILE_SIZE) - ItemSpriteConfig.ITEM_SPRITE_HEIGHT / 2.0;

        // Thêm độ phân tán ngẫu nhiên nhỏ để các vật phẩm không chồng khít lên nhau
        double scatter = GameLogicConfig.ITEM_DROP_SCATTER_RANGE * 0.5;
        offsetX += (Math.random() - 0.5) * scatter;
        offsetY += (Math.random() - 0.5) * scatter;

        ItemType itemType = stackToDrop.getItemType();
        int amount = stackToDrop.getQuantity();

        // Tìm vị trí trống hoặc vị trí có item cùng loại để gộp
        int searchRadius = GameLogicConfig.ITEM_DROP_SEARCH_RADIUS;
        int finalCol = -1;
        int finalRow = -1;
        boolean foundSpot = false;

        TileData idealTile = worldMap.getTileData(targetTileCol, targetTileRow);
        if (idealTile.getGroundItem() == null) {
            finalCol = targetTileCol;
            finalRow = targetTileRow;
            foundSpot = true;
        } else if (idealTile.getGroundItem() == itemType) {
            finalCol = targetTileCol;
            finalRow = targetTileRow;
            foundSpot = true;
        } else {
            // Tìm các ô xung quanh nếu ô hiện tại đã đầy
            for (int r = targetTileRow - searchRadius; r <= targetTileRow + searchRadius; r++) {
                for (int c = targetTileCol - searchRadius; c <= targetTileCol + searchRadius; c++) {
                    if (r == targetTileRow && c == targetTileCol) continue;

                    TileData checkTile = worldMap.getTileData(c, r);
                    if (checkTile.getGroundItem() == null) {
                        finalCol = c;
                        finalRow = r;
                        foundSpot = true;
                        break;
                    }
                }
                if (foundSpot) break;
            }
        }

        // Nếu không tìm được chỗ, đè lên ô lý tưởng
        if (!foundSpot) {
            finalCol = targetTileCol;
            finalRow = targetTileRow;
        }

        TileData finalTile = worldMap.getTileData(finalCol, finalRow);
        int itemDurability = stackToDrop.getCurrentDurability();

        if (finalTile.getGroundItem() == itemType) {
            // Cộng dồn số lượng
            finalTile.setGroundItemAmount(finalTile.getGroundItemAmount() + amount);
            if (itemType.hasDurability()) {
                finalTile.setGroundItemDurability(itemDurability);
            }
        } else {
            // Đặt mới
            finalTile.setGroundItem(itemType);
            finalTile.setGroundItemAmount(amount);
            finalTile.setGroundItemDurability(itemType.hasDurability() ? itemDurability : 0);

            if (finalCol == targetTileCol && finalRow == targetTileRow) {
                finalTile.setGroundItemOffsetX(offsetX);
                finalTile.setGroundItemOffsetY(offsetY);
            } else {
                finalTile.setDefaultItemOffset();
                double jitterX = (Math.random() - 0.5) * GameLogicConfig.ITEM_DROP_SCATTER_RANGE;
                double jitterY = (Math.random() - 0.5) * GameLogicConfig.ITEM_DROP_SCATTER_RANGE;
                finalTile.setGroundItemOffsetX(finalTile.getGroundItemOffsetX() + jitterX);
                finalTile.setGroundItemOffsetY(finalTile.getGroundItemOffsetY() + jitterY);
            }
        }

        worldMap.setTileData(finalCol, finalRow, finalTile);
        actionManager.setMapNeedsUpdate(true);

        // Xóa item khỏi inventory
        mainPlayer.getHotbarItems()[slotIndex] = null;
        mainGameView.updateHotbar();
    }

    // ==============================================================================================
    // HỆ THỐNG MENU VÀ ĐIỀU KHIỂN GAME (SYSTEM CONTROL)
    // ==============================================================================================

    public void toggleSettingsMenu() {
        this.isPaused = !this.isPaused;
        if (this.isPaused) {
            // Đóng Shop nếu đang mở
            if (mainGameView != null && mainGameView.getShopView() != null && mainGameView.getShopView().isShopVisible()) {
                mainGameView.getShopView().toggle();
            }

            if (gameLoop != null) {
                gameLoop.stop();
            }

            if (audioManager != null) {
                audioManager.pauseMusic();
            }

            mainGameView.showSettingsMenu(mainPlayer.getName(), mainPlayer.getLevel());
        } else {
            if (gameLoop != null) {
                gameLoop.start();
            }

            if (audioManager != null) {
                audioManager.resumeMusic();
            }

            mainGameView.hideSettingsMenu();
        }
    }

    /**
     * Kích hoạt chuỗi sự kiện Game Over với độ trễ trước khi hiện giao diện.
     */
    private void triggerGameOverSequence() {
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(GameLogicConfig.GAME_OVER_DELAY_SECONDS)
        );
        pause.setOnFinished(e -> {
            if (mainGameView != null) {
                mainGameView.showGameOverUI();
            }
        });
        pause.play();
    }

    /**
     * Dọn dẹp tài nguyên và gọi callback để quay về Menu chính.
     */
    public void returnToMainMenu() {
        if (mainGameView != null) {
            mainGameView.hideGameOverUI();
        }

        if (gameLoop != null) {
            gameLoop.stop();
        }

        if (audioManager != null) {
            audioManager.pauseMusic();
        }

        if (onReturnToMainMenuHandler != null) {
            onReturnToMainMenuHandler.run();
        } else {
            System.err.println("Chưa set Handler cho returnToMainMenu! Game sẽ bị kẹt.");
        }

        isGameOverSequenceTriggered = false;
        isPaused = false;
    }

    public void setOnReturnToMainMenuHandler(Runnable handler) {
        this.onReturnToMainMenuHandler = handler;
    }

    /**
     * Chuyển đổi trạng thái thời tiết (dùng cho mục đích kiểm thử).
     */
    public void toggleWeather() {
        if (weatherManager != null) {
            if (weatherManager.isRaining()) {
                weatherManager.setWeather(com.example.farmSimulation.config.WeatherConfig.WeatherType.SUNNY);
            } else {
                weatherManager.setWeather(com.example.farmSimulation.config.WeatherConfig.WeatherType.RAIN);
            }
        }
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public WeatherManager getWeatherManager() {
        return weatherManager;
    }

    // ==============================================================================================
    // HỆ THỐNG LƯU VÀ TẢI GAME (SAVE & LOAD SYSTEM)
    // ==============================================================================================

    /**
     * Lưu toàn bộ dữ liệu game hiện tại vào file.
     */
    public void saveGameData() {
        GameSaveState state = new GameSaveState();

        // 1. Lưu thông tin người chơi
        Player p = mainPlayer;
        state.playerName = p.getName();
        state.playerMoney = p.getMoney();
        state.playerXP = p.getCurrentXP();
        state.playerLevel = p.getLevel();
        state.playerStamina = p.getCurrentStamina();
        state.playerX = p.getTileX();
        state.playerY = p.getTileY();

        // Lưu túi đồ
        for (ItemStack stack : p.getHotbarItems()) {
            if (stack != null) {
                state.inventory.add(new SavedItemStack(stack.getItemType(), stack.getQuantity(), stack.getCurrentDurability()));
            } else {
                state.inventory.add(null);
            }
        }

        // 2. Lưu danh sách động vật
        for (Animal a : animalManager.getAnimals()) {
            state.animals.add(new SavedAnimal(a.getType(), a.getX(), a.getY(), a.getAge(), a.getHunger()));
        }

        // 3. Lưu thời gian và thời tiết
        state.currentDaySeconds = timeManager.getGameTimeSeconds();
        state.currentDay = timeManager.getCurrentDay();
        state.currentWeather = weatherManager.getCurrentWeather();

        // 4. Lưu dữ liệu Shop
        for (ShopSlot slot : shopManager.getCurrentDailyStock()) {
            state.dailyShopStock.add(new SavedShopSlot(slot.getItemType(), slot.getQuantity(), slot.getDiscountRate()));
        }

        // 5. Lưu danh sách nhiệm vụ
        for (Quest q : questManager.getActiveQuests()) {
            SavedQuest sq = new SavedQuest();
            sq.description = q.getDescription();
            sq.type = q.getType();
            sq.targetItem = q.getTargetItem();
            sq.targetAmount = q.getTargetAmount();
            sq.currentAmount = q.getCurrentAmount();
            sq.rewardMoney = q.getRewardMoney();
            sq.rewardXp = q.getRewardXp();
            sq.isClaimed = q.isClaimed();
            state.activeQuests.add(sq);
        }

        // 6. Lưu dữ liệu bản đồ
        // Sử dụng Reflection để truy cập dữ liệu bản đồ private mà không cần thay đổi tính đóng gói của class WorldMap
        try {
            java.lang.reflect.Field mapField = WorldMap.class.getDeclaredField("tileDataMap");
            mapField.setAccessible(true);
            java.util.HashMap<Long, TileData> rawMap = (java.util.HashMap<Long, TileData>) mapField.get(worldMap);

            for (java.util.Map.Entry<Long, TileData> entry : rawMap.entrySet()) {
                long key = entry.getKey();
                TileData td = entry.getValue();

                int col = (int) (key >> 32);
                int row = (int) key;

                SavedTileData std = new SavedTileData();
                std.col = col;
                std.row = row;
                std.baseType = td.getBaseTileType();

                // Lưu trạng thái ô đất
                std.isWatered = td.isWatered();
                std.isFertilized = td.isFertilized();
                std.lastWateredTime = td.getLastWateredTime();
                std.fertilizerStartTime = td.getFertilizerStartTime();

                // Lưu thông tin cây trồng
                if (td.getCropData() != null) {
                    std.hasCrop = true;
                    std.cropType = td.getCropData().getType();
                    std.cropStage = td.getCropData().getGrowthStage();
                }

                // Lưu thông tin cây tự nhiên
                if (td.getTreeData() != null) {
                    std.hasTree = true;
                    std.treeStage = td.getTreeData().getGrowthStage();
                    std.treeChopCount = td.getTreeData().getChopCount();
                }

                // Lưu hàng rào
                if (td.getFenceData() != null) {
                    std.hasFence = true;
                    std.fenceIsOpen = td.getFenceData().isOpen();
                }

                // Lưu vật phẩm trên mặt đất
                if (td.getGroundItem() != null) {
                    std.hasGroundItem = true;
                    std.groundItemType = td.getGroundItem();
                    std.groundItemAmount = td.getGroundItemAmount();
                    std.groundItemDurability = td.getGroundItemDurability();
                    std.groundItemOffsetX = td.getGroundItemOffsetX();
                    std.groundItemOffsetY = td.getGroundItemOffsetY();
                }

                state.worldTiles.add(std);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error saving world map tiles!");
        }

        SaveManager.saveGame(state);
        if (mainGameView != null) mainGameView.showTemporaryText("Game Saved!", p.getTileX(), p.getTileY());
    }

    /**
     * Tải dữ liệu game từ file save.
     */
    public void loadGameData() {
        GameSaveState state = SaveManager.loadGame();
        if (state == null) {
            System.out.println("No save file found.");
            return;
        }

        // 1. Khôi phục thông tin người chơi
        if (state.playerName != null) mainPlayer.setName(state.playerName);
        mainPlayer.setMoney(state.playerMoney);
        mainPlayer.setExperience((int)state.playerXP);
        mainPlayer.setLevel(state.playerLevel);
        mainPlayer.setStamina(state.playerStamina);
        mainPlayer.setTileX(state.playerX);
        mainPlayer.setTileY(state.playerY);

        // Đặt trạng thái an toàn để tránh nhân vật bị kẹt
        mainPlayer.setState(PlayerView.PlayerState.IDLE);
        this.isPaused = false;

        // Khôi phục túi đồ
        ItemStack[] newHotbar = new ItemStack[com.example.farmSimulation.config.HotbarConfig.HOTBAR_SLOT_COUNT];
        for (int i = 0; i < state.inventory.size() && i < newHotbar.length; i++) {
            SavedItemStack s = state.inventory.get(i);
            if (s != null) {
                ItemStack stack = new ItemStack(s.type, s.quantity);
                stack.setCurrentDurability(s.durability);
                newHotbar[i] = stack;
            }
        }
        mainPlayer.setHotbarItems(newHotbar);

        // 2. Khôi phục động vật (Xóa cũ, tạo mới)
        animalManager.getAnimals().clear();
        for (SavedAnimal sa : state.animals) {
            Animal a = new Animal(sa.type, sa.x, sa.y);
            a.setAge(sa.age);
            a.setHunger(sa.hunger);
            animalManager.addAnimal(a);
        }

        // 3. Khôi phục môi trường
        timeManager.setGameTime(state.currentDaySeconds);

        if (state.currentWeather != null) {
            weatherManager.setWeather(state.currentWeather);
        }

        if (state.dailyShopStock != null && !state.dailyShopStock.isEmpty()) {
            shopManager.restoreStock(state.dailyShopStock);
        }

        if (state.activeQuests != null && !state.activeQuests.isEmpty()) {
            questManager.restoreQuests(state.activeQuests);
        }

        // 4. Khôi phục dữ liệu bản đồ
        // Xóa sạch map hiện tại và nạp lại dữ liệu thông qua Reflection
        try {
            java.lang.reflect.Field mapField = WorldMap.class.getDeclaredField("tileDataMap");
            mapField.setAccessible(true);
            java.util.HashMap<Long, TileData> rawMap = (java.util.HashMap<Long, TileData>) mapField.get(worldMap);
            rawMap.clear();

            for (SavedTileData std : state.worldTiles) {
                TileData td = new TileData(std.baseType);

                // Khôi phục trạng thái
                td.setWatered(std.isWatered);
                td.setFertilized(std.isFertilized);
                td.setLastWateredTime(std.lastWateredTime);
                td.setFertilizerStartTime(std.fertilizerStartTime);

                // Khôi phục cây trồng
                if (std.hasCrop) {
                    // Khởi tạo dữ liệu cây trồng với thời gian bắt đầu được tính lại từ thời điểm tải game
                    CropData cd = new CropData(std.cropType, std.cropStage, System.nanoTime());
                    td.setCropData(cd);
                }

                // Khôi phục cây tự nhiên
                if (std.hasTree) {
                    TreeData trd = new TreeData();
                    trd.setGrowthStage(std.treeStage);
                    trd.setChopCount(std.treeChopCount);
                    td.setTreeData(trd);
                }

                // Khôi phục hàng rào
                if (std.hasFence) {
                    FenceData fd = new FenceData(std.fenceIsOpen);
                    td.setFenceData(fd);
                }

                // Khôi phục vật phẩm trên đất
                if (std.hasGroundItem) {
                    td.setGroundItem(std.groundItemType);
                    td.setGroundItemAmount(std.groundItemAmount);
                    td.setGroundItemDurability(std.groundItemDurability);
                    td.setGroundItemOffsetX(std.groundItemOffsetX);
                    td.setGroundItemOffsetY(std.groundItemOffsetY);
                }

                worldMap.setTileData(std.col, std.row, td);
            }

            // Tính toán lại hình dạng hàng rào sau khi tải xong toàn bộ
            fenceManager.updateAllFencePatterns();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading world map tiles!");
        }

        // 5. Cập nhật lại giao diện và lấy lại tiêu điểm bàn phím để đảm bảo điều khiển hoạt động sau khi tải game
        if (mainGameView != null) {
            mainGameView.showTemporaryText("Game Loaded!", state.playerX, state.playerY);
            mainGameView.updateMoneyDisplay(mainPlayer.getMoney());
            mainGameView.updateHotbar();

            if (camera != null) {
                camera.initializePosition(mainPlayer, playerView);
                // Buộc vẽ lại toàn bộ bản đồ
                mainGameView.updateMap(camera.getWorldOffsetX(), camera.getWorldOffsetY(), true);
            }

            if (mainGameView.getRootPane() != null) {
                mainGameView.getRootPane().requestFocus();
            }
        }
        System.out.println("Game Loaded Successfully!");
    }
}