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
    // --- Các thành phần cốt lõi (Model, View, Controller) ---
    private final Player mainPlayer;
    private final WorldMap worldMap;
    private final MainGameView mainGameView;
    private final PlayerView playerView;
    private final GameController gameController;

    // --- Các Manager con (được tách ra) ---
    private final TimeManager timeManager;
    private final ActionManager actionManager;
    private final PlayerMovementHandler movementHandler;
    private final Camera camera;
    private final InteractionManager interactionManager;
    private final CropManager cropManager;
    private final TreeManager treeManager; // Quản lý cây tự nhiên
    private final FenceManager fenceManager; // Quản lý hàng rào
    private final CollisionManager collisionManager; // Quản lý collision
    private final AnimalManager animalManager; // Quản lý động vật
    private final ShopManager shopManager; // Quản lý shop
    private final WeatherManager weatherManager; // Quản lý thời tiết
    private final QuestManager questManager; // Quản lý quest (nhiệm vụ hàng ngày)
    private final com.example.farmSimulation.view.assets.AudioManager audioManager; // Quản lý âm thanh nền

    // --- Trạng thái Game ---
    private AnimationTimer gameLoop; // Khởi tạo gameLoop
    private boolean isPaused = false;
    private long lastUpdateTime = 0; // Thời gian update lần cuối (nanoTime) - để tính deltaTime
    private boolean isGameOverSequenceTriggered = false; // Flag to ensure Game Over sequence runs only once

    // Tọa độ ô chuột đang trỏ tới
    private int currentMouseTileX = 0;
    private int currentMouseTileY = 0;

    // Tọa độ chuột thực tế trong thế giới
    private double currentMouseWorldX = 0;
    private double currentMouseWorldY = 0;

    // Callback để quay về Main Menu
    private Runnable onReturnToMainMenuHandler;

    // Constructor nhận tất cả các thành phần cốt lõi
    public GameManager(Player player, WorldMap worldMap, MainGameView mainGameView,
                       PlayerView playerView, GameController gameController) {
        this.mainPlayer = player;
        this.worldMap = worldMap;
        this.mainGameView = mainGameView;
        this.playerView = playerView;
        this.gameController = gameController;

        // Khởi tạo các Manager con
        this.camera = new Camera();
        this.timeManager = new TimeManager(mainGameView);
        this.actionManager = new ActionManager(player, playerView); // Truyền player/playerView
        this.movementHandler = new PlayerMovementHandler(player, playerView, gameController, camera, mainGameView);
        this.interactionManager = new InteractionManager(this.actionManager);
        this.cropManager = new CropManager(this.worldMap);
        this.treeManager = new TreeManager(this.worldMap);
        this.fenceManager = new FenceManager(this.worldMap);
        this.collisionManager = new CollisionManager(this.worldMap);
        this.animalManager = new AnimalManager(this.worldMap, this.collisionManager);
        this.shopManager = new ShopManager(player); // Khởi tạo ShopManager
        this.weatherManager = new WeatherManager(); // Khởi tạo WeatherManager
        this.questManager = new QuestManager(); // Khởi tạo QuestManager
        this.audioManager = new com.example.farmSimulation.view.assets.AudioManager(); // Khởi tạo AudioManager

        // Liên kết Player với MainGameView để hiển thị thông báo
        player.setMainGameView(mainGameView);

        // Set Player reference in PlayerView for accessing timeOfDeath (for death animation)
        playerView.setPlayer(player);

        // Liên kết các Manager với nhau
        this.actionManager.setFenceManager(this.fenceManager);
        this.actionManager.setAnimalManager(this.animalManager); // Liên kết AnimalManager với ActionManager
        this.actionManager.setQuestManager(this.questManager); // Liên kết QuestManager với ActionManager
        this.movementHandler.setCollisionManager(this.collisionManager);
        this.interactionManager.setAnimalManager(this.animalManager); // Liên kết AnimalManager với InteractionManager
        this.interactionManager.setCollisionManager(this.collisionManager); // Liên kết CollisionManager với InteractionManager
        this.interactionManager.setWorldMap(this.worldMap); // Liên kết WorldMap với InteractionManager
        this.cropManager.setWeatherManager(this.weatherManager); // Liên kết WeatherManager với CropManager
        this.cropManager.setTimeManager(this.timeManager); // Liên kết TimeManager với CropManager
        this.shopManager.setQuestManager(this.questManager); // Liên kết QuestManager với ShopManager
    }

    public void startGame() {
        // Đặt vị trí camera ban đầu
        camera.initializePosition(mainPlayer, playerView);

        // Gọi updateMap để vẽ map lần đầu
        mainGameView.updateMap(camera.getWorldOffsetX(), camera.getWorldOffsetY(), true);

        // Bắt đầu game loop
        this.gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateGameLogic(now);
            }
        };
        gameLoop.start();

        // Generate initial daily quests (chỉ tạo nếu chưa có load game, nhưng ở đây load game đã chạy trước nếu có)
        // Nếu load game thì quest có thể được load (nếu có tính năng đó), hiện tại cứ generate mới
        if (questManager.getActiveQuests().isEmpty()) {
            questManager.generateDailyQuests();
        }

        System.out.println("Game Started!");
    }

    /**
     * Hàm update chính, chỉ điều phối các hàm con
     */
    private void updateGameLogic(long now) {
        // Dừng ngay lập tức nếu game đang tạm dừng
        if (this.isPaused) {
            return;
        }

        // Check if player is DEAD - stop all game logic processing
        if (mainPlayer.getState() == PlayerView.PlayerState.DEAD) {
            // Sync PlayerView state to DEAD
            playerView.setState(PlayerView.PlayerState.DEAD, mainPlayer.getDirection());

            // Trigger Game Over sequence only once
            if (!isGameOverSequenceTriggered) {
                isGameOverSequenceTriggered = true;
                triggerGameOverSequence();
            }
            // Only update death animation, stop everything else
            playerView.updateAnimation();
            // Still update lastUpdateTime to avoid deltaTime spike when restarting
            lastUpdateTime = now;
            return;
        }

        // Tính deltaTime (thời gian trôi qua giữa 2 frame) - tính bằng giây
        double deltaTime = 0.0;
        if (lastUpdateTime > 0) {
            long deltaNanos = now - lastUpdateTime;
            deltaTime = deltaNanos / 1_000_000_000.0; // Chuyển từ nano giây sang giây
            // Giới hạn deltaTime tối đa để tránh lag spike (ví dụ: 0.1 giây = 100ms)
            if (deltaTime > 0.1) {
                deltaTime = 0.1;
            }
        }
        lastUpdateTime = now;

        // Cập nhật thời gian & chu kỳ ngày đêm
        timeManager.update();

        // Check if new day started and refresh shop stock
        if (timeManager.hasNewDayStarted()) {
            shopManager.generateDailyStock(true); // Allow discounts on natural day refresh
            questManager.generateDailyQuests(); // Generate new daily quests
            System.out.println("New day started! Shop stock refreshed.");
            System.out.println("New daily quests generated!");
        }

        // Tự động hồi phục stamina (khi không hoạt động)
        updateStaminaRecovery(deltaTime);

        // Cập nhật di chuyển & trạng thái Player (truyền deltaTime)
        movementHandler.update(deltaTime);

        // Cập nhật animation (View tự chạy)
        playerView.updateAnimation(); // Yêu cầu PlayerView tự chạy animation

        // Cập nhật các hành động chờ
        actionManager.updateTimedActions(worldMap, mainGameView, camera.getWorldOffsetX(), camera.getWorldOffsetY());

        // Cập nhật logic cây trồng
        boolean cropsUpdated = cropManager.updateCrops(now); // Truyền 'now'
        if (cropsUpdated) {
            actionManager.setMapNeedsUpdate(true); // Báo map cần vẽ lại
        }

        // Cập nhật logic cây tự nhiên
        boolean treesUpdated = treeManager.updateTrees(now, mainPlayer.getTileX(), mainPlayer.getTileY());
        if (treesUpdated) {
            actionManager.setMapNeedsUpdate(true); // Báo map cần vẽ lại
        }

        // Cập nhật logic động vật
        // Đã thêm tham số mainPlayer để khớp với logic AnimalManager mới
        boolean animalsUpdated = animalManager.updateAnimals(now, mainPlayer);
        if (animalsUpdated) {
            actionManager.setMapNeedsUpdate(true); // Báo map cần vẽ lại
        }

        // Cập nhật vẽ động vật
        mainGameView.updateAnimals(animalManager.getAnimals(), camera.getWorldOffsetX(), camera.getWorldOffsetY());

        // Cập nhật thời tiết
        weatherManager.updateWeather(now);
        mainGameView.updateWeather(weatherManager.isRaining());

        // Cập nhật HUD (Player Stats, Weather Icon)
        if (mainGameView.getHudView() != null) {
            mainGameView.getHudView().updatePlayerStats();
            mainGameView.getHudView().updateWeather(weatherManager.isRaining());
        }

        // Cập nhật chuột
        updateMouseSelector();

        // Cập nhật ghost placement
        updateGhostPlacement();

        // Cập nhật collision hitbox (debug mode)
        updateCollisionHitbox();

        // Cập nhật hiển thị tiền
        mainGameView.updateMoneyDisplay(mainPlayer.getMoney());
    }

    /**
     * Tự động hồi phục stamina theo thời gian dựa trên trạng thái player
     */
    private void updateStaminaRecovery(double deltaTime) {
        PlayerView.PlayerState currentState = mainPlayer.getState();

        if (currentState == PlayerView.PlayerState.WALK) {
            // Running costs stamina - drain stamina while walking
            double drainAmount = GameLogicConfig.STAMINA_DRAIN_RUNNING * deltaTime;
            mainPlayer.reduceStamina(drainAmount);
        } else if (currentState == PlayerView.PlayerState.IDLE) {
            // Idle regenerates stamina - only recover when standing still
            if (mainPlayer.getCurrentStamina() < mainPlayer.getMaxStamina()) {
                double recoveryAmount = GameLogicConfig.STAMINA_RECOVERY_RATE * deltaTime;
                mainPlayer.recoverStamina(recoveryAmount);
            }
        }
        // Other states (Action/Busy): Do nothing - actions have their own instant stamina costs
    }

    /**
     * Cập nhật vị trí ô vuông chọn
     */
    private void updateMouseSelector() {
        // Tọa độ logic của chuột trong thế giới
        this.currentMouseWorldX = -camera.getWorldOffsetX() + gameController.getMouseX();
        this.currentMouseWorldY = -camera.getWorldOffsetY() + gameController.getMouseY();

        // Tọa độ logic của ô mà chuột trỏ tới
        this.currentMouseTileX = (int) Math.floor(currentMouseWorldX / WorldConfig.TILE_SIZE);
        this.currentMouseTileY = (int) Math.floor(currentMouseWorldY / WorldConfig.TILE_SIZE);

        mainGameView.updateSelector(
                this.currentMouseTileX,       // Vị trí X của ô được chọn
                this.currentMouseTileY,       // Vị trí Y của ô được chọn
                camera.getWorldOffsetX(),     // Vị trí X của thế giới
                camera.getWorldOffsetY()      // Vị trí Y của thế giới
        );
    }

    /**
     * Cập nhật ghost placement (bóng mờ khi cầm item có thể đặt)
     */
    private void updateGhostPlacement() {
        ItemStack currentItem = mainPlayer.getCurrentItem();
        // Truyền thêm thông tin tọa độ thực tế vào MainGameView để hỗ trợ đặt động vật tự do
        // (MainGameView và WorldRenderer cần được sửa để xử lý logic này nếu muốn hiển thị bóng mờ tự do)
        // Hiện tại chúng ta vẫn dùng tile-based cho cây trồng/rào, nhưng động vật sẽ đặt ở mouseWorldX/Y
        // Tạm thời logic hiển thị ghost placement vẫn dựa trên TileX/Y trong WorldRenderer
        // Nếu bạn muốn ghost động vật đi theo chuột, cần sửa WorldRenderer.updateGhostPlacement
        mainGameView.updateGhostPlacement(
                this.currentMouseTileX,
                this.currentMouseTileY,
                camera.getWorldOffsetX(),
                camera.getWorldOffsetY(),
                currentItem
        );
    }

    /**
     * Cập nhật collision hitbox (debug mode)
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

    /**
     * Kiểm tra xem người chơi có trong tầm tương tác không
     */
    private boolean isPlayerInRange(int col, int row, ItemStack currentStack) {
        // Tọa độ pixel logic của Tâm người chơi (sử dụng giá trị đã scale)
        double scaledPlayerWidth = PlayerSpriteConfig.BASE_PLAYER_FRAME_WIDTH * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
        double scaledPlayerHeight = PlayerSpriteConfig.BASE_PLAYER_FRAME_HEIGHT * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
        double playerX = mainPlayer.getTileX() + scaledPlayerWidth / 2;

        // Cộng thêm INTERACTION_CENTER_Y_OFFSET để tâm tính toán hạ thấp xuống (ngang hông/chân)
        double playerY = mainPlayer.getTileY() + scaledPlayerHeight / 2 + PlayerSpriteConfig.INTERACTION_CENTER_Y_OFFSET;

        // Tọa độ pixel logic của TÂM ô target
        double targetX = (col * WorldConfig.TILE_SIZE) + (WorldConfig.TILE_SIZE / 2.0);
        double targetY = (row * WorldConfig.TILE_SIZE) + (WorldConfig.TILE_SIZE / 2.0);

        // Tính khoảng cách
        double distance = Math.sqrt(
                Math.pow(playerX - targetX, 2) + Math.pow(playerY - targetY, 2)
        );

        // Mặc định là HAND range (đã scale theo BASE_PLAYER_FRAME_SCALE)
        double range = GameLogicConfig.HAND_INTERACTION_RANGE * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;

        // Lấy tầm tương tác dựa trên công cụ (tất cả đều scale theo BASE_PLAYER_FRAME_SCALE)
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
                range = GameLogicConfig.PLANT_INTERACTION_RANGE * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE; // Áp dụng cho tất cả loại hạt
            } else if (type == ItemType.ITEM_COW || type == ItemType.ITEM_CHICKEN ||
                    type == ItemType.ITEM_PIG || type == ItemType.ITEM_SHEEP ||
                    type == ItemType.EGG) {
                range = AnimalConfig.PLACEMENT_RANGE * WorldConfig.TILE_SIZE * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
            } else {
                // Các item khác dùng mặc định HAND range (đã scale)
                range = GameLogicConfig.HAND_INTERACTION_RANGE * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
            }
        }

        return distance <= range;
    }

    /**
     * Quay hướng người chơi về phía ô (col, row)
     */
    private void updatePlayerDirectionTowards(int col, int row) {
        // Tọa độ pixel logic của Tâm người chơi (sử dụng giá trị đã scale)
        double scaledPlayerWidth = PlayerSpriteConfig.BASE_PLAYER_FRAME_WIDTH * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
        double scaledPlayerHeight = PlayerSpriteConfig.BASE_PLAYER_FRAME_HEIGHT * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
        double playerX = mainPlayer.getTileX() + scaledPlayerWidth / 2;

        // Cộng thêm INTERACTION_CENTER_Y_OFFSET để tâm quay hướng cũng chính xác
        double playerY = mainPlayer.getTileY() + scaledPlayerHeight / 2 + PlayerSpriteConfig.INTERACTION_CENTER_Y_OFFSET;

        // Tọa độ pixel logic của TÂM ô target
        double targetX = (col * WorldConfig.TILE_SIZE) + (WorldConfig.TILE_SIZE / 2.0);
        double targetY = (row * WorldConfig.TILE_SIZE) + (WorldConfig.TILE_SIZE / 2.0);

        // Tính góc (atan2(y, x) = góc giữa vector (x, y) và trục X dương)
        double angleDeg = Math.toDegrees(Math.atan2(targetY - playerY, targetX - playerX));

        // Quyết định hướng theo trục Oy ngược
        // -45 đến 45 = RIGHT
        // 45 đến 135 = DOWN
        // 135 đến 180 hoặc -180 đến -135 = LEFT
        // -135 đến -45 = UP
        if (angleDeg > -45 && angleDeg <= 45) {
            mainPlayer.setDirection(PlayerView.Direction.RIGHT);
        } else if (angleDeg > 45 && angleDeg <= 135) {
            mainPlayer.setDirection(PlayerView.Direction.DOWN);
        } else if (angleDeg > 135 || angleDeg < -135) {
            mainPlayer.setDirection(PlayerView.Direction.LEFT);
        } else { // (-135 đến -45)
            mainPlayer.setDirection(PlayerView.Direction.UP);
        }
    }

    /**
     * Xử lý click chuột
     * Hàm này chỉ kiểm tra điều kiện chung và ủy quyền
     */
    public void interactWithTile(int col, int row) {
        // Kiểm tra xem player có đang bận (làm hành động khác) không
        PlayerView.PlayerState currentState = mainPlayer.getState();
        if (currentState != PlayerView.PlayerState.IDLE && currentState != PlayerView.PlayerState.WALK) {
            return; // Player đang bận, không cho hành động
        }

        // Lấy công cụ *trước* khi kiểm tra tầm
        ItemStack currentStack = mainPlayer.getCurrentItem();

        // Kiểm tra tầm hoạt động (Sử dụng hàm isPlayerInRange)
        if (!isPlayerInRange(col, row, currentStack)) {
            // Vị trí hiển thị của text
            double playerScreenX = playerView.getSpriteContainer().getLayoutX();
            double playerScreenY = playerView.getSpriteContainer().getLayoutY() + PlayerSpriteConfig.PLAYER_SPRITE_OFFSET_Y;

            // Hiển thị text "It's too far"
            mainGameView.showTemporaryText(HudConfig.TOO_FAR_TEXT, playerScreenX, playerScreenY);
            return; // Quá xa, không làm gì cả
        }

        // Quay người chơi về hướng ô target
        updatePlayerDirectionTowards(col, row);

        // Sử dụng biến currentMouseWorldX/Y đã được cập nhật chính xác trong updateMouseSelector
        // Thay vì tính lại từ đầu, ta dùng giá trị đã được đồng bộ với camera mới nhất
        double mouseWorldX = this.currentMouseWorldX;
        double mouseWorldY = this.currentMouseWorldY;

        // Lưu lại state trước khi hành động
        PlayerView.PlayerState oldState = mainPlayer.getState();

        // Bước 1: Ưu tiên xử lý tương tác động vật (nếu đang cầm item động vật hoặc EGG)
        // Gọi processAnimalInteraction TRƯỚC với tọa độ chuột thực tế (để đặt tự do)
        String animalErrorMsg = interactionManager.processAnimalInteraction(mainPlayer, playerView, mouseWorldX, mouseWorldY);

        // Nếu processAnimalInteraction đã xử lý (trả về lỗi hoặc thành công), hiển thị thông báo và return
        if (animalErrorMsg != null) {
            double playerScreenX = playerView.getSpriteContainer().getLayoutX();
            double playerScreenY = playerView.getSpriteContainer().getLayoutY() + PlayerSpriteConfig.PLAYER_SPRITE_OFFSET_Y;
            mainGameView.showTemporaryText(animalErrorMsg, playerScreenX, playerScreenY);
            return; // Đã xử lý xong, không cần gọi processInteraction
        }

        // Kiểm tra xem hành động với động vật có THÀNH CÔNG hay không (state thay đổi sang BUSY, AXE...)
        // Nếu thành công thì không chạy tiếp xuống logic đào đất/trồng cây
        if (mainPlayer.getState() != oldState) {
            mainGameView.updateHotbar();
            return; // Dừng lại ở đây
        }

        // Nếu đặt động vật thành công (trả về null và không lỗi), cần cập nhật Hotbar
        // Kiểm tra xem có phải vừa đặt động vật không?
        // Nếu InteractionManager.processAnimalInteraction trả về null, nó có thể là "không làm gì" HOẶC "thành công".
        // Để chắc chắn, ta gọi updateHotbar() ở đây để đồng bộ Item
        mainGameView.updateHotbar();

        // Bước 2: Nếu không phải tương tác động vật, xử lý tương tác với tile (cây trồng/đất)
        // Nhận thông báo lỗi trực tiếp từ hàm processInteraction
        String errorMsg = interactionManager.processInteraction(mainPlayer, playerView, worldMap, col, row);

        // Nếu errorMsg != null nghĩa là có lỗi hoặc không hành động được -> Hiển thị text
        if (errorMsg != null) {
            double playerScreenX = playerView.getSpriteContainer().getLayoutX();
            double playerScreenY = playerView.getSpriteContainer().getLayoutY() + PlayerSpriteConfig.PLAYER_SPRITE_OFFSET_Y;
            mainGameView.showTemporaryText(errorMsg, playerScreenX, playerScreenY);
        }
    }
    /**
     * Được gọi từ Controller khi người dùng đổi slot hotbar
     */
    public void changeHotbarSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < HotbarConfig.HOTBAR_SLOT_COUNT) {
            mainPlayer.setSelectedHotbarSlot(slotIndex);
            mainGameView.updateHotbar(); // Yêu cầu View vẽ lại
        }
    }

    /**
     * Được gọi từ HotbarView khi người dùng kéo thả item
     */
    public void swapHotbarItems(int indexA, int indexB) {
        mainPlayer.swapHotbarItems(indexA, indexB);
        mainGameView.updateHotbar(); // Cập nhật lại giao diện ngay
    }

    /**
     * Mở/đóng hàng rào (được gọi từ Controller khi click chuột phải)
     */
    /**
     * Kiểm tra xem có fence tại vị trí chỉ định không
     */
    public boolean hasFenceAt(int col, int row) {
        TileData data = worldMap.getTileData(col, row);
        return data != null && data.getFenceData() != null;
    }

    public void toggleFence(int col, int row) {
        // Thêm kiểm tra tầm hoạt động bằng Tay (Hand)
        // Truyền null vào currentStack để sử dụng HAND_INTERACTION_RANGE mặc định
        if (!isPlayerInRange(col, row, null)) {
            // Hiển thị thông báo "Xa quá"
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
     * Xử lý logic ăn đồ của player khi right-click (nếu không click vào fence)
     */
    public void handlePlayerEating() {
        // Kiểm tra xem player có đang bận không
        PlayerView.PlayerState currentState = mainPlayer.getState();
        if (currentState != PlayerView.PlayerState.IDLE && currentState != PlayerView.PlayerState.WALK) {
            return; // Player đang bận, không cho ăn
        }

        // Thử ăn item hiện tại
        if (mainPlayer.eatCurrentItem()) {
            // Tạo TimedTileAction để giữ BUSY state trong thời gian ngắn (0.5 giây)
            long eatDurationMs = 500; // 0.5 giây
            int framesRemaining = (int) (eatDurationMs / (1000.0 / 60.0)); // Chuyển đổi sang frames (60 FPS)

            // Tạo action không thay đổi tile (newTileData = null)
            TimedTileAction eatAction = new TimedTileAction(
                    (int) mainPlayer.getTileX(), // Không quan trọng vì không thay đổi tile
                    (int) mainPlayer.getTileY(),
                    null, // Không thay đổi tile
                    framesRemaining,
                    false, // Không tiêu thụ item (đã xử lý trong eatCurrentItem)
                    -1
            );
            eatAction.setActionState(PlayerView.PlayerState.BUSY);

            // Thêm action vào hàng đợi
            actionManager.addPendingAction(eatAction);

            // Set PlayerView state to BUSY
            playerView.setState(PlayerView.PlayerState.BUSY, mainPlayer.getDirection());

            // Cập nhật hotbar để hiển thị item đã giảm số lượng
            mainGameView.updateHotbar();

            // Hiển thị thông báo
            double playerScreenX = playerView.getSpriteContainer().getLayoutX();
            double playerScreenY = playerView.getSpriteContainer().getLayoutY() + PlayerSpriteConfig.PLAYER_SPRITE_OFFSET_Y;
            mainGameView.showTemporaryText("Yum!", playerScreenX, playerScreenY);
        }
    }

    public void toggleSettingsMenu() {
        this.isPaused = !this.isPaused; // Sử dụng this.isPaused
        if (this.isPaused) {
            // Đóng Shop nếu đang mở để tránh overlap
            if (mainGameView != null && mainGameView.getShopView() != null && mainGameView.getShopView().isShopVisible()) {
                mainGameView.getShopView().toggle();
            }

            if (gameLoop != null) {
                gameLoop.stop(); // ⬅️ Dừng game loop
                //System.out.println("Game Loop đã dừng.");
            }

            // Tạm dừng nhạc nền khi pause
            if (audioManager != null) {
                audioManager.pauseMusic();
            }

            mainGameView.showSettingsMenu(mainPlayer.getName(), mainPlayer.getLevel());
        } else {
            if (gameLoop != null) {
                gameLoop.start(); // ⬅️ Tiếp tục game loop
                //System.out.println("Game Loop đã tiếp tục.");
            }

            // Tiếp tục phát nhạc nền khi resume
            if (audioManager != null) {
                audioManager.resumeMusic();
            }

            mainGameView.hideSettingsMenu();
        }
    }

    /**
     * Toggle thời tiết (dùng cho test)
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

    /**
     * Getter cho ShopManager
     */
    public ShopManager getShopManager() {
        return shopManager;
    }

    /**
     * Getter cho WeatherManager
     */
    public WeatherManager getWeatherManager() {
        return weatherManager;
    }

    /**
     * Tính slot index từ tọa độ chuột (trong scene coordinates)
     */
    public int getHotbarSlotFromMouse(double mouseX, double mouseY) {
        if (mainGameView == null || mainGameView.getHotbarView() == null) {
            return -1;
        }

        // Chuyển tọa độ scene sang tọa độ local của HotbarView
        javafx.geometry.Point2D scenePoint = new javafx.geometry.Point2D(mouseX, mouseY);
        javafx.geometry.Point2D localPoint = mainGameView.getHotbarView().sceneToLocal(scenePoint);

        // Gọi hàm trong HotbarView để tính slot index
        return mainGameView.getHotbarView().getSlotIndexFromMouse(localPoint.getX(), localPoint.getY());
    }

    /**
     * Ném item từ hotbar slot chỉ định xuống dưới chân player
     * @param slotIndex Slot index cần ném item
     */
    public void dropItemFromHotbar(int slotIndex) {
        // Kiểm tra xem player có đang bận không
        PlayerView.PlayerState currentState = mainPlayer.getState();
        if (currentState != PlayerView.PlayerState.IDLE && currentState != PlayerView.PlayerState.WALK) {
            return; // Player đang bận, không cho ném item
        }

        // Kiểm tra slot index hợp lệ
        if (slotIndex < 0 || slotIndex >= mainPlayer.getHotbarItems().length) {
            return;
        }

        // Lấy item từ slot chỉ định
        ItemStack stackToDrop = mainPlayer.getHotbarItems()[slotIndex];
        if (stackToDrop == null) {
            return; // Không có item để ném
        }

        // Tính toán vị trí player (vị trí để ném item)
        // Item ném ra từ vị trí player (tileX, tileY), giống như thịt rơi từ động vật
        // Vị trí player là góc trên-trái của sprite, item ném từ đó
        double playerX = mainPlayer.getTileX() + (PlayerSpriteConfig.BASE_PLAYER_FRAME_WIDTH * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE) / 2.0; // Tâm ngang
        double playerY = mainPlayer.getTileY() + (PlayerSpriteConfig.BASE_PLAYER_FRAME_HEIGHT * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE) - ItemSpriteConfig.ITEM_SPRITE_HEIGHT / 2.0; // Gần chân player

        // Ném item xuống dưới chân player (không theo hướng chuột)
        // Vị trí đích: ngay dưới chân player, với scatter ngẫu nhiên nhỏ
        double targetX = playerX;
        double targetY = playerY + WorldConfig.TILE_SIZE * 0.3; // Ném xuống dưới một chút

        // Tính tile và offset
        int targetTileCol = (int) Math.floor(targetX / WorldConfig.TILE_SIZE);
        int targetTileRow = (int) Math.floor(targetY / WorldConfig.TILE_SIZE);

        // Tính offset trong tile (để item không dính ở giữa ô)
        double offsetX = targetX - (targetTileCol * WorldConfig.TILE_SIZE);
        double offsetY = targetY - (targetTileRow * WorldConfig.TILE_SIZE);

        // Trừ đi một nửa kích thước item để item nằm giữa điểm đó
        offsetX -= ItemSpriteConfig.ITEM_SPRITE_WIDTH / 2.0;
        offsetY -= ItemSpriteConfig.ITEM_SPRITE_HEIGHT / 2.0;

        // Thêm scatter ngẫu nhiên nhỏ để item không bị dính chặt
        double scatter = GameLogicConfig.ITEM_DROP_SCATTER_RANGE * 0.5; // Scatter nhỏ hơn một chút
        offsetX += (Math.random() - 0.5) * scatter;
        offsetY += (Math.random() - 0.5) * scatter;

        // Lấy item type và số lượng
        ItemType itemType = stackToDrop.getItemType();
        int amount = stackToDrop.getQuantity();

        // Tìm ô trống xung quanh để đặt item (giống như thịt)
        int searchRadius = GameLogicConfig.ITEM_DROP_SEARCH_RADIUS;
        int finalCol = -1;
        int finalRow = -1;
        boolean foundSpot = false;

        // 1. Kiểm tra ô lý tưởng trước
        TileData idealTile = worldMap.getTileData(targetTileCol, targetTileRow);
        if (idealTile.getGroundItem() == null) {
            finalCol = targetTileCol;
            finalRow = targetTileRow;
            foundSpot = true;
        } else if (idealTile.getGroundItem() == itemType) {
            // Trùng loại -> Cộng dồn
            finalCol = targetTileCol;
            finalRow = targetTileRow;
            foundSpot = true;
        } else {
            // Ô lý tưởng đã có item khác -> Tìm xung quanh
            for (int r = targetTileRow - searchRadius; r <= targetTileRow + searchRadius; r++) {
                for (int c = targetTileCol - searchRadius; c <= targetTileCol + searchRadius; c++) {
                    if (r == targetTileRow && c == targetTileCol) continue; // Đã check rồi

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

        // Nếu vẫn không tìm thấy chỗ -> Bắt buộc phải đè lên ô lý tưởng
        if (!foundSpot) {
            finalCol = targetTileCol;
            finalRow = targetTileRow;
        }

        // Đặt item vào ô đã chọn
        TileData finalTile = worldMap.getTileData(finalCol, finalRow);

        // Lấy độ bền hiện tại của item (nếu có)
        int itemDurability = stackToDrop.getCurrentDurability();

        // Nếu cộng dồn
        if (finalTile.getGroundItem() == itemType) {
            finalTile.setGroundItemAmount(finalTile.getGroundItemAmount() + amount);
            // Giữ nguyên offset cũ của item đang có
            // Lưu ý: Với stackable items, độ bền không quan trọng, nhưng vẫn cần lưu cho tools
            if (itemType.hasDurability()) {
                finalTile.setGroundItemDurability(itemDurability); // Cập nhật độ bền khi cộng dồn
            }
        } else {
            // Đặt mới hoặc đè
            finalTile.setGroundItem(itemType);
            finalTile.setGroundItemAmount(amount);
            // Lưu độ bền của item (0 nếu không có độ bền hoặc không áp dụng)
            finalTile.setGroundItemDurability(itemType.hasDurability() ? itemDurability : 0);

            // Nếu đặt đúng ô lý tưởng -> Dùng offset đã tính
            if (finalCol == targetTileCol && finalRow == targetTileRow) {
                finalTile.setGroundItemOffsetX(offsetX);
                finalTile.setGroundItemOffsetY(offsetY);
            } else {
                // Nếu phải đặt sang ô bên cạnh -> Dùng offset mặc định cộng thêm scatter
                finalTile.setDefaultItemOffset();
                double jitterX = (Math.random() - 0.5) * GameLogicConfig.ITEM_DROP_SCATTER_RANGE;
                double jitterY = (Math.random() - 0.5) * GameLogicConfig.ITEM_DROP_SCATTER_RANGE;
                finalTile.setGroundItemOffsetX(finalTile.getGroundItemOffsetX() + jitterX);
                finalTile.setGroundItemOffsetY(finalTile.getGroundItemOffsetY() + jitterY);
            }
        }

        worldMap.setTileData(finalCol, finalRow, finalTile);
        actionManager.setMapNeedsUpdate(true);

        // Xóa item khỏi hotbar slot chỉ định
        mainPlayer.getHotbarItems()[slotIndex] = null;

        // Cập nhật hotbar view
        mainGameView.updateHotbar();
    }

    /**
     * Trigger Game Over sequence with delay before showing UI
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
     * Thay vì Reset nóng, hàm này sẽ dọn dẹp và gọi callback để về Main Menu
     */
    public void returnToMainMenu() {
        // Hide Game Over UI
        if (mainGameView != null) {
            mainGameView.hideGameOverUI();
        }

        // Stop Game Loop
        if (gameLoop != null) {
            gameLoop.stop();
        }

        // Stop Audio
        if (audioManager != null) {
            audioManager.pauseMusic();
        }

        // Gọi callback nếu đã được set (Main Class sẽ xử lý chuyển cảnh)
        if (onReturnToMainMenuHandler != null) {
            onReturnToMainMenuHandler.run();
        } else {
            System.err.println("Chưa set Handler cho returnToMainMenu! Game sẽ bị kẹt.");
        }

        // Reset flag
        isGameOverSequenceTriggered = false;
        isPaused = false;
    }

    // Setter cho Handler về Main Menu
    public void setOnReturnToMainMenuHandler(Runnable handler) {
        this.onReturnToMainMenuHandler = handler;
    }

    // --- THÊM MỚI: Logic Lưu Game (HOÀN CHỈNH) ---
    public void saveGameData() {
        GameSaveState state = new GameSaveState();

        // 1. Lưu Player
        Player p = mainPlayer;
        state.playerName = p.getName(); // Lưu tên người chơi
        state.playerMoney = p.getMoney();
        state.playerXP = p.getCurrentXP();
        state.playerLevel = p.getLevel();
        state.playerStamina = p.getCurrentStamina();
        state.playerX = p.getTileX();
        state.playerY = p.getTileY();

        // Lưu Hotbar
        for (ItemStack stack : p.getHotbarItems()) {
            if (stack != null) {
                state.inventory.add(new SavedItemStack(stack.getItemType(), stack.getQuantity(), stack.getCurrentDurability()));
            } else {
                state.inventory.add(null); // Giữ chỗ cho slot trống
            }
        }

        // 2. Lưu Động vật
        for (Animal a : animalManager.getAnimals()) {
            state.animals.add(new SavedAnimal(a.getType(), a.getX(), a.getY(), a.getAge(), a.getHunger()));
        }

        // 3. Lưu Thời gian
        state.currentDaySeconds = timeManager.getGameTimeSeconds();
        state.currentDay = timeManager.getCurrentDay();

        // 4. Lưu Thời tiết
        state.currentWeather = weatherManager.getCurrentWeather();

        // 5. Lưu Shop
        for (ShopSlot slot : shopManager.getCurrentDailyStock()) {
            state.dailyShopStock.add(new SavedShopSlot(slot.getItemType(), slot.getQuantity(), slot.getDiscountRate()));
        }

        // 6. Lưu Quests
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

        // 7. Lưu Toàn bộ dữ liệu Map (Cây trồng, Cây tự nhiên, Hàng rào, Đất, Item dưới đất...)
        // Duyệt qua tất cả TileData đã được tạo ra trong WorldMap
        // Vì Java Reflection hơi rườm rà, tôi sẽ dùng cách truy cập `worldMap` thông qua `java.lang.reflect.Field` trong GameManager này luôn cho gọn.
        try {
            java.lang.reflect.Field mapField = WorldMap.class.getDeclaredField("tileDataMap");
            mapField.setAccessible(true);
            java.util.HashMap<Long, TileData> rawMap = (java.util.HashMap<Long, TileData>) mapField.get(worldMap);

            for (java.util.Map.Entry<Long, TileData> entry : rawMap.entrySet()) {
                long key = entry.getKey();
                TileData td = entry.getValue();

                // Giải mã key ra col, row
                int col = (int) (key >> 32);
                int row = (int) key;

                // Chỉ lưu những ô có dữ liệu quan trọng (khác mặc định)
                // Hoặc đơn giản là lưu hết những ô đã từng tương tác (nằm trong map)
                SavedTileData std = new SavedTileData();
                std.col = col;
                std.row = row;
                std.baseType = td.getBaseTileType();

                // Tile state
                std.isWatered = td.isWatered();
                std.isFertilized = td.isFertilized();
                std.lastWateredTime = td.getLastWateredTime();
                std.fertilizerStartTime = td.getFertilizerStartTime();

                // Crop
                if (td.getCropData() != null) {
                    std.hasCrop = true;
                    std.cropType = td.getCropData().getType();
                    std.cropStage = td.getCropData().getGrowthStage();
                }

                // Tree
                if (td.getTreeData() != null) {
                    std.hasTree = true;
                    std.treeStage = td.getTreeData().getGrowthStage();
                    std.treeChopCount = td.getTreeData().getChopCount();
                }

                // Fence
                if (td.getFenceData() != null) {
                    std.hasFence = true;
                    std.fenceIsOpen = td.getFenceData().isOpen();
                }

                // Ground Item
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

        // Ghi xuống file
        SaveManager.saveGame(state);
        if (mainGameView != null) mainGameView.showTemporaryText("Game Saved!", p.getTileX(), p.getTileY());
    }

    // --- THÊM MỚI: Logic Tải Game (HOÀN CHỈNH) ---
    public void loadGameData() {
        GameSaveState state = SaveManager.loadGame();
        if (state == null) {
            System.out.println("No save file found.");
            return;
        }

        // 1. Khôi phục Player
        if (state.playerName != null) mainPlayer.setName(state.playerName); // Khôi phục tên
        mainPlayer.setMoney(state.playerMoney);
        mainPlayer.setExperience((int)state.playerXP);
        mainPlayer.setLevel(state.playerLevel);
        mainPlayer.setStamina(state.playerStamina);
        mainPlayer.setTileX(state.playerX);
        mainPlayer.setTileY(state.playerY);

        // Đặt trạng thái an toàn để tránh bị kẹt
        mainPlayer.setState(PlayerView.PlayerState.IDLE);
        this.isPaused = false; // Đảm bảo game không bị pause sau khi load

        // Khôi phục Inventory
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

        // 2. Khôi phục Động vật
        // Xóa hết con cũ
        animalManager.getAnimals().clear();
        // Tạo con mới
        for (SavedAnimal sa : state.animals) {
            Animal a = new Animal(sa.type, sa.x, sa.y);
            a.setAge(sa.age);
            a.setHunger(sa.hunger);
            animalManager.addAnimal(a);
        }

        // 3. Khôi phục Thời gian
        timeManager.setGameTime(state.currentDaySeconds);

        // 4. Khôi phục Thời tiết
        if (state.currentWeather != null) {
            weatherManager.setWeather(state.currentWeather);
        }

        // 5. Khôi phục Shop
        if (state.dailyShopStock != null && !state.dailyShopStock.isEmpty()) {
            shopManager.restoreStock(state.dailyShopStock);
        }

        // 6. Khôi phục Quests
        if (state.activeQuests != null && !state.activeQuests.isEmpty()) {
            questManager.restoreQuests(state.activeQuests);
        }

        // 7. Khôi phục Map (QUAN TRỌNG)
        // Xóa dữ liệu map cũ (bằng cách clear map thông qua reflection hoặc tạo map mới nếu có thể set)
        try {
            java.lang.reflect.Field mapField = WorldMap.class.getDeclaredField("tileDataMap");
            mapField.setAccessible(true);
            java.util.HashMap<Long, TileData> rawMap = (java.util.HashMap<Long, TileData>) mapField.get(worldMap);
            rawMap.clear(); // Xóa sạch map hiện tại

            // Load lại từ file save
            for (SavedTileData std : state.worldTiles) {
                TileData td = new TileData(std.baseType);

                // Restore State
                td.setWatered(std.isWatered);
                td.setFertilized(std.isFertilized);
                td.setLastWateredTime(std.lastWateredTime);
                td.setFertilizerStartTime(std.fertilizerStartTime);

                // Restore Crop
                if (std.hasCrop) {
                    // FIX: Sử dụng đúng constructor 3 tham số của CropData
                    // plantTime được đặt là System.nanoTime() để bắt đầu tính thời gian từ lúc load
                    CropData cd = new CropData(std.cropType, std.cropStage, System.nanoTime());
                    td.setCropData(cd);
                }

                // Restore Tree
                if (std.hasTree) {
                    TreeData trd = new TreeData();
                    trd.setGrowthStage(std.treeStage);
                    trd.setChopCount(std.treeChopCount);
                    td.setTreeData(trd);
                }

                // Restore Fence
                if (std.hasFence) {
                    FenceData fd = new FenceData(std.fenceIsOpen);
                    // Cần set pattern nếu muốn chính xác ngay lập tức, nhưng updateMap sẽ lo visual
                    td.setFenceData(fd);
                }

                // Restore Ground Item
                if (std.hasGroundItem) {
                    td.setGroundItem(std.groundItemType);
                    td.setGroundItemAmount(std.groundItemAmount);
                    td.setGroundItemDurability(std.groundItemDurability);
                    td.setGroundItemOffsetX(std.groundItemOffsetX);
                    td.setGroundItemOffsetY(std.groundItemOffsetY);
                }

                // Put vào map
                worldMap.setTileData(std.col, std.row, td);
            }

            // Recalculate fence patterns after loading all fences
            fenceManager.updateAllFencePatterns();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading world map tiles!");
        }

        // Refresh lại View và Lấy lại Focus (FIX LỖI KHÔNG TƯƠNG TÁC)
        if (mainGameView != null) {
            mainGameView.showTemporaryText("Game Loaded!", state.playerX, state.playerY);
            mainGameView.updateMoneyDisplay(mainPlayer.getMoney());
            mainGameView.updateHotbar();

            // Cập nhật map ngay lập tức
            if (camera != null) {
                camera.initializePosition(mainPlayer, playerView);
                // Force redraw everything
                mainGameView.updateMap(camera.getWorldOffsetX(), camera.getWorldOffsetY(), true);
            }

            // Request Focus về lại RootPane để nhận input bàn phím
            if (mainGameView.getRootPane() != null) {
                mainGameView.getRootPane().requestFocus();
            }
        }
        System.out.println("Game Loaded Successfully!");
    }
}