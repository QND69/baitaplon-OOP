package com.example.farmSimulation.view;

import com.example.farmSimulation.config.GameLogicConfig;
import com.example.farmSimulation.config.PlayerSpriteConfig;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;

@Getter
public class PlayerView {

    // =================================================================================
    // 1. ĐỊNH NGHĨA CONSTANTS VÀ ENUMS
    // =================================================================================

    // Các trạng thái hành động của nhân vật, được GameManager sử dụng để điều khiển
    public enum PlayerState {
        IDLE,       // Đứng yên
        WALK,       // Di chuyển
        ATTACK,     // Tấn công
        AXE,        // Chặt cây
        HOE,        // Cuốc đất
        WATER,      // Tưới nước
        PLANT,      // Gieo hạt
        SHOVEL,     // Xúc đất
        FERTILIZE,  // Bón phân
        BUSY,       // Trạng thái bận (hiển thị giống đứng yên)
        DEAD        // Nhân vật bị ngất
    }

    // Các hướng di chuyển của nhân vật
    public enum Direction {
        DOWN, UP, RIGHT, LEFT
    }

    // Record lưu trữ cấu hình cho một animation cụ thể
    private record AnimData(int row, int frameCount, long speed, PlayerSpriteConfig.AnimationType type) {
        // Constructor hỗ trợ sử dụng tốc độ mặc định
        AnimData(int row, int frameCount, PlayerSpriteConfig.AnimationType type) {
            this(row, frameCount, PlayerSpriteConfig.ANIMATION_SPEED, type);
        }
    }

    // =================================================================================
    // 2. CÁC BIẾN THÀNH VIÊN (FIELDS)
    // =================================================================================

    // --- Cấu trúc dữ liệu Animation ---
    // Map lồng nhau lưu trữ cấu hình animation theo Trạng thái và Hướng
    private final Map<PlayerState, Map<Direction, AnimData>> animationMap;
    // Map xác định file ảnh (Sprite Sheet) nào được dùng cho trạng thái nào
    private final Map<PlayerState, Image> stateSheetMap;

    // Tài nguyên ảnh
    private final Image playerSheet;        // Ảnh cho hành động cơ bản
    private final Image playerActionsSheet; // Ảnh cho hành động làm nông

    // --- Thành phần giao diện (UI Nodes) ---
    private final Pane spriteContainer; // Container chứa nhân vật
    private final ImageView sprite;     // Đối tượng hiển thị ảnh

    // Kích thước cơ sở đã được tính toán theo tỷ lệ
    private final double baseWidth;
    private final double baseHeight;

    // --- Các thành phần hỗ trợ Debug (có thể null) ---
    private Rectangle debugBoundingBox;     // Khung bao quanh kích thước
    private Circle debugCenterDot;          // Điểm tâm nhân vật
    private Circle debugRangeCircle;        // Vùng tương tác
    private Rectangle debugCollisionHitbox; // Vùng va chạm vật lý

    // --- Trạng thái hiện tại ---
    private PlayerState currentState = PlayerState.IDLE;
    private Direction currentDirection = Direction.DOWN;

    // --- Biến quản lý vòng lặp Animation ---
    private int currentFrame = 0;
    private long lastFrameTime = 0;
    private long frameAccumulator = 0; // Thời gian tích lũy để chuyển frame

    // Tham chiếu đến model Player để đồng bộ thời gian chết
    private com.example.farmSimulation.model.Player player;

    // =================================================================================
    // 3. KHỞI TẠO (CONSTRUCTOR & INIT)
    // =================================================================================

    public PlayerView(Image playerSheet, Image playerActionsSheet) {
        this.playerSheet = playerSheet;
        this.playerActionsSheet = playerActionsSheet;

        // Khởi tạo container và ImageView
        this.spriteContainer = new Pane();
        this.sprite = new ImageView(playerSheet);
        this.spriteContainer.getChildren().add(this.sprite);

        this.animationMap = new EnumMap<>(PlayerState.class);
        this.stateSheetMap = new EnumMap<>(PlayerState.class);

        // Nạp dữ liệu cấu hình cho từng loại animation
        initializeAnimationMap();

        // Tính toán kích thước hiển thị dựa trên cấu hình và tỷ lệ scale
        this.baseWidth = PlayerSpriteConfig.BASE_PLAYER_FRAME_WIDTH * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
        this.baseHeight = PlayerSpriteConfig.BASE_PLAYER_FRAME_HEIGHT * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;

        // Thiết lập kích thước cho container
        this.spriteContainer.setPrefSize(this.baseWidth, this.baseHeight);

        // Cấu hình hiển thị ảnh
        sprite.setSmooth(false); // Tắt làm mịn để giữ phong cách pixel art
        sprite.setPreserveRatio(true); // Giữ nguyên tỷ lệ khung hình

        this.lastFrameTime = System.nanoTime();

        // Khởi tạo các thành phần debug nếu chế độ debug đang bật
        initDebugElements();

        // Cập nhật hiển thị lần đầu
        updateViewport();
    }

    // Khởi tạo các node debug để hiển thị vùng va chạm và tương tác
    private void initDebugElements() {
        if (PlayerSpriteConfig.DEBUG_PLAYER_BOUNDS) {
            this.debugBoundingBox = new Rectangle(this.baseWidth, this.baseHeight);
            this.debugBoundingBox.setFill(null);
            this.debugBoundingBox.setStroke(PlayerSpriteConfig.DEBUG_BOUNDING_BOX_COLOR);
            this.debugBoundingBox.setStrokeWidth(1.0);
            this.debugBoundingBox.setMouseTransparent(true);

            this.debugCenterDot = new Circle(2.0);
            this.debugCenterDot.setFill(PlayerSpriteConfig.DEBUG_CENTER_DOT_COLOR);
            this.debugCenterDot.setMouseTransparent(true);

            this.debugRangeCircle = new Circle(GameLogicConfig.HAND_INTERACTION_RANGE * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE);
            this.debugRangeCircle.setFill(null);
            this.debugRangeCircle.setStroke(PlayerSpriteConfig.DEBUG_RANGE_COLOR);
            this.debugRangeCircle.setStrokeWidth(1.0);
            this.debugRangeCircle.setMouseTransparent(true);

            this.debugCollisionHitbox = new Rectangle(
                    PlayerSpriteConfig.COLLISION_BOX_WIDTH,
                    PlayerSpriteConfig.COLLISION_BOX_HEIGHT
            );
            this.debugCollisionHitbox.setFill(null);
            this.debugCollisionHitbox.setStroke(PlayerSpriteConfig.DEBUG_COLLISION_HITBOX_COLOR);
            this.debugCollisionHitbox.setStrokeWidth(2.0);
            this.debugCollisionHitbox.setMouseTransparent(true);
            this.debugCollisionHitbox.setVisible(false);
        } else {
            this.debugBoundingBox = null;
            this.debugCenterDot = null;
            this.debugRangeCircle = null;
            this.debugCollisionHitbox = null;
        }
    }

    // Thiết lập toàn bộ dữ liệu animation cho các trạng thái
    private void initializeAnimationMap() {
        // --- 1. Liên kết Sheet ảnh ---
        stateSheetMap.put(PlayerState.IDLE, playerSheet);
        stateSheetMap.put(PlayerState.WALK, playerSheet);
        stateSheetMap.put(PlayerState.ATTACK, playerSheet);
        stateSheetMap.put(PlayerState.DEAD, playerSheet);

        stateSheetMap.put(PlayerState.AXE, playerActionsSheet);
        stateSheetMap.put(PlayerState.HOE, playerActionsSheet);
        stateSheetMap.put(PlayerState.WATER, playerActionsSheet);
        stateSheetMap.put(PlayerState.PLANT, playerActionsSheet);
        stateSheetMap.put(PlayerState.SHOVEL, playerActionsSheet);
        stateSheetMap.put(PlayerState.FERTILIZE, playerActionsSheet);

        // --- 2. Cấu hình Animation cơ bản ---

        // Trạng thái đứng yên
        Map<Direction, AnimData> idleMap = new EnumMap<>(Direction.class);
        idleMap.put(Direction.DOWN, new AnimData(PlayerSpriteConfig.IDLE_DOWN_ROW, PlayerSpriteConfig.IDLE_FRAMES, PlayerSpriteConfig.AnimationType.LOOP));
        idleMap.put(Direction.UP, new AnimData(PlayerSpriteConfig.IDLE_UP_ROW, PlayerSpriteConfig.IDLE_FRAMES, PlayerSpriteConfig.AnimationType.LOOP));
        idleMap.put(Direction.RIGHT, new AnimData(PlayerSpriteConfig.IDLE_RIGHT_ROW, PlayerSpriteConfig.IDLE_FRAMES, PlayerSpriteConfig.AnimationType.LOOP));
        idleMap.put(Direction.LEFT, new AnimData(PlayerSpriteConfig.IDLE_LEFT_ROW, PlayerSpriteConfig.IDLE_FRAMES, PlayerSpriteConfig.AnimationType.LOOP));
        animationMap.put(PlayerState.IDLE, idleMap);

        // Trạng thái bận rộn (sử dụng 1 frame của trạng thái đứng yên)
        Map<Direction, AnimData> busyMap = new EnumMap<>(Direction.class);
        busyMap.put(Direction.DOWN, new AnimData(PlayerSpriteConfig.IDLE_DOWN_ROW, 1, PlayerSpriteConfig.AnimationType.LOOP));
        busyMap.put(Direction.UP, new AnimData(PlayerSpriteConfig.IDLE_UP_ROW, 1, PlayerSpriteConfig.AnimationType.LOOP));
        busyMap.put(Direction.RIGHT, new AnimData(PlayerSpriteConfig.IDLE_RIGHT_ROW, 1, PlayerSpriteConfig.AnimationType.LOOP));
        busyMap.put(Direction.LEFT, new AnimData(PlayerSpriteConfig.IDLE_LEFT_ROW, 1, PlayerSpriteConfig.AnimationType.LOOP));
        animationMap.put(PlayerState.BUSY, busyMap);

        // Trạng thái di chuyển
        Map<Direction, AnimData> walkMap = new EnumMap<>(Direction.class);
        walkMap.put(Direction.DOWN, new AnimData(PlayerSpriteConfig.WALK_DOWN_ROW, PlayerSpriteConfig.WALK_FRAMES, PlayerSpriteConfig.AnimationType.LOOP));
        walkMap.put(Direction.UP, new AnimData(PlayerSpriteConfig.WALK_UP_ROW, PlayerSpriteConfig.WALK_FRAMES, PlayerSpriteConfig.AnimationType.LOOP));
        walkMap.put(Direction.RIGHT, new AnimData(PlayerSpriteConfig.WALK_RIGHT_ROW, PlayerSpriteConfig.WALK_FRAMES, PlayerSpriteConfig.AnimationType.LOOP));
        walkMap.put(Direction.LEFT, new AnimData(PlayerSpriteConfig.WALK_LEFT_ROW, PlayerSpriteConfig.WALK_FRAMES, PlayerSpriteConfig.AnimationType.LOOP));
        animationMap.put(PlayerState.WALK, walkMap);

        // Trạng thái tấn công
        Map<Direction, AnimData> attackMap = new EnumMap<>(Direction.class);
        attackMap.put(Direction.DOWN, new AnimData(PlayerSpriteConfig.ATTACK_DOWN_ROW, PlayerSpriteConfig.ATTACK_FRAMES, PlayerSpriteConfig.ATTACK_SPEED, PlayerSpriteConfig.AnimationType.ONE_SHOT));
        attackMap.put(Direction.UP, new AnimData(PlayerSpriteConfig.ATTACK_UP_ROW, PlayerSpriteConfig.ATTACK_FRAMES, PlayerSpriteConfig.ATTACK_SPEED, PlayerSpriteConfig.AnimationType.ONE_SHOT));
        attackMap.put(Direction.RIGHT, new AnimData(PlayerSpriteConfig.ATTACK_RIGHT_ROW, PlayerSpriteConfig.ATTACK_FRAMES, PlayerSpriteConfig.ATTACK_SPEED, PlayerSpriteConfig.AnimationType.ONE_SHOT));
        attackMap.put(Direction.LEFT, new AnimData(PlayerSpriteConfig.ATTACK_LEFT_ROW, PlayerSpriteConfig.ATTACK_FRAMES, PlayerSpriteConfig.ATTACK_SPEED, PlayerSpriteConfig.AnimationType.ONE_SHOT));
        animationMap.put(PlayerState.ATTACK, attackMap);

        // --- 3. Cấu hình Animation làm nông ---

        // Chặt cây
        Map<Direction, AnimData> axeMap = new EnumMap<>(Direction.class);
        axeMap.put(Direction.DOWN, new AnimData(PlayerSpriteConfig.AXE_DOWN_ROW, PlayerSpriteConfig.AXE_FRAMES, GameLogicConfig.AXE_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        axeMap.put(Direction.UP, new AnimData(PlayerSpriteConfig.AXE_UP_ROW, PlayerSpriteConfig.AXE_FRAMES, GameLogicConfig.AXE_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        axeMap.put(Direction.RIGHT, new AnimData(PlayerSpriteConfig.AXE_RIGHT_ROW, PlayerSpriteConfig.AXE_FRAMES, GameLogicConfig.AXE_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        axeMap.put(Direction.LEFT, new AnimData(PlayerSpriteConfig.AXE_LEFT_ROW, PlayerSpriteConfig.AXE_FRAMES, GameLogicConfig.AXE_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        animationMap.put(PlayerState.AXE, axeMap);

        // Cuốc đất
        Map<Direction, AnimData> hoeMap = new EnumMap<>(Direction.class);
        hoeMap.put(Direction.DOWN, new AnimData(PlayerSpriteConfig.HOE_DOWN_ROW, PlayerSpriteConfig.HOE_FRAMES, GameLogicConfig.HOE_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        hoeMap.put(Direction.UP, new AnimData(PlayerSpriteConfig.HOE_UP_ROW, PlayerSpriteConfig.HOE_FRAMES, GameLogicConfig.HOE_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        hoeMap.put(Direction.RIGHT, new AnimData(PlayerSpriteConfig.HOE_RIGHT_ROW, PlayerSpriteConfig.HOE_FRAMES, GameLogicConfig.HOE_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        hoeMap.put(Direction.LEFT, new AnimData(PlayerSpriteConfig.HOE_LEFT_ROW, PlayerSpriteConfig.HOE_FRAMES, GameLogicConfig.HOE_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        animationMap.put(PlayerState.HOE, hoeMap);

        // Tưới nước
        Map<Direction, AnimData> waterMap = new EnumMap<>(Direction.class);
        waterMap.put(Direction.DOWN, new AnimData(PlayerSpriteConfig.WATER_DOWN_ROW, PlayerSpriteConfig.WATER_FRAMES, GameLogicConfig.WATERING_CAN_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        waterMap.put(Direction.UP, new AnimData(PlayerSpriteConfig.WATER_UP_ROW, PlayerSpriteConfig.WATER_FRAMES, GameLogicConfig.WATERING_CAN_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        waterMap.put(Direction.RIGHT, new AnimData(PlayerSpriteConfig.WATER_RIGHT_ROW, PlayerSpriteConfig.WATER_FRAMES, GameLogicConfig.WATERING_CAN_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        waterMap.put(Direction.LEFT, new AnimData(PlayerSpriteConfig.WATER_LEFT_ROW, PlayerSpriteConfig.WATER_FRAMES, GameLogicConfig.WATERING_CAN_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        animationMap.put(PlayerState.WATER, waterMap);

        // Trồng cây
        Map<Direction, AnimData> plantMap = new EnumMap<>(Direction.class);
        plantMap.put(Direction.DOWN, new AnimData(PlayerSpriteConfig.PLANT_DOWN_ROW, PlayerSpriteConfig.PLANT_FRAMES, GameLogicConfig.PLANT_DURATION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        plantMap.put(Direction.UP, new AnimData(PlayerSpriteConfig.PLANT_UP_ROW, PlayerSpriteConfig.PLANT_FRAMES, GameLogicConfig.PLANT_DURATION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        plantMap.put(Direction.RIGHT, new AnimData(PlayerSpriteConfig.PLANT_RIGHT_ROW, PlayerSpriteConfig.PLANT_FRAMES, GameLogicConfig.PLANT_DURATION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        plantMap.put(Direction.LEFT, new AnimData(PlayerSpriteConfig.PLANT_LEFT_ROW, PlayerSpriteConfig.PLANT_FRAMES, GameLogicConfig.PLANT_DURATION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        animationMap.put(PlayerState.PLANT, plantMap);

        // Đào đất (Trạng thái SHOVEL nhưng dùng hình ảnh DIG)
        Map<Direction, AnimData> shovelMap = new EnumMap<>(Direction.class);
        shovelMap.put(Direction.DOWN, new AnimData(PlayerSpriteConfig.DIG_DOWN_ROW, PlayerSpriteConfig.DIG_FRAMES, GameLogicConfig.SHOVEL_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        shovelMap.put(Direction.UP, new AnimData(PlayerSpriteConfig.DIG_UP_ROW, PlayerSpriteConfig.DIG_FRAMES, GameLogicConfig.SHOVEL_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        shovelMap.put(Direction.RIGHT, new AnimData(PlayerSpriteConfig.DIG_RIGHT_ROW, PlayerSpriteConfig.DIG_FRAMES, GameLogicConfig.SHOVEL_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        shovelMap.put(Direction.LEFT, new AnimData(PlayerSpriteConfig.DIG_LEFT_ROW, PlayerSpriteConfig.DIG_FRAMES, GameLogicConfig.SHOVEL_DURATION_PER_REPETITION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        animationMap.put(PlayerState.SHOVEL, shovelMap);

        // Bón phân
        Map<Direction, AnimData> fertilizeMap = new EnumMap<>(Direction.class);
        fertilizeMap.put(Direction.DOWN, new AnimData(PlayerSpriteConfig.FERTILZED_DOWN_ROW, PlayerSpriteConfig.FERTILZED_FRAMES, GameLogicConfig.FERTILIZER_DURATION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        fertilizeMap.put(Direction.UP, new AnimData(PlayerSpriteConfig.FERTILZED_UP_ROW, PlayerSpriteConfig.FERTILZED_FRAMES, GameLogicConfig.FERTILIZER_DURATION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        fertilizeMap.put(Direction.RIGHT, new AnimData(PlayerSpriteConfig.FERTILZED_RIGHT_ROW, PlayerSpriteConfig.FERTILZED_FRAMES, GameLogicConfig.FERTILIZER_DURATION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        fertilizeMap.put(Direction.LEFT, new AnimData(PlayerSpriteConfig.FERTILZED_LEFT_ROW, PlayerSpriteConfig.FERTILZED_FRAMES, GameLogicConfig.FERTILIZER_DURATION_MS, PlayerSpriteConfig.AnimationType.ACTION_LOOP));
        animationMap.put(PlayerState.FERTILIZE, fertilizeMap);

        // --- 4. Trạng thái đặc biệt ---

        // Trạng thái Ngất - Chạy một lần rồi dừng ở frame cuối
        Map<Direction, AnimData> deadMap = new EnumMap<>(Direction.class);
        deadMap.put(Direction.DOWN, new AnimData(PlayerSpriteConfig.DEAD_ROW, PlayerSpriteConfig.DEAD_FRAMES, PlayerSpriteConfig.DEAD_SPEED, PlayerSpriteConfig.AnimationType.ONE_SHOT));
        deadMap.put(Direction.UP, new AnimData(PlayerSpriteConfig.DEAD_ROW, PlayerSpriteConfig.DEAD_FRAMES, PlayerSpriteConfig.DEAD_SPEED, PlayerSpriteConfig.AnimationType.ONE_SHOT));
        deadMap.put(Direction.RIGHT, new AnimData(PlayerSpriteConfig.DEAD_ROW, PlayerSpriteConfig.DEAD_FRAMES, PlayerSpriteConfig.DEAD_SPEED, PlayerSpriteConfig.AnimationType.ONE_SHOT));
        deadMap.put(Direction.LEFT, new AnimData(PlayerSpriteConfig.DEAD_ROW, PlayerSpriteConfig.DEAD_FRAMES, PlayerSpriteConfig.DEAD_SPEED, PlayerSpriteConfig.AnimationType.ONE_SHOT));
        animationMap.put(PlayerState.DEAD, deadMap);
    }

    // =================================================================================
    // 4. LOGIC CẬP NHẬT (GAME LOOP)
    // =================================================================================

    // Gán tham chiếu Player để truy cập thời gian chết
    public void setPlayer(com.example.farmSimulation.model.Player player) {
        this.player = player;
    }

    // Cập nhật trạng thái animation theo thời gian, được gọi liên tục từ GameManager
    public void updateAnimation() {
        // Xử lý riêng cho trường hợp nhân vật bị ngất
        // Animation sẽ được tính toán dựa trên thời gian trôi qua kể từ lúc chết
        if (currentState == PlayerState.DEAD && player != null && player.getTimeOfDeath() > 0) {
            AnimData data = getAnimationData();
            if (data == null) return;

            long elapsedMs = System.currentTimeMillis() - player.getTimeOfDeath();
            long frameSpeed = data.speed();
            int calculatedFrame = (int) (elapsedMs / frameSpeed);

            // Giữ nguyên frame cuối cùng nếu đã chạy hết animation
            if (calculatedFrame >= data.frameCount()) {
                currentFrame = data.frameCount() - 1;
            } else {
                currentFrame = calculatedFrame;
            }

            updateViewport();
            return;
        }

        // Xử lý animation thông thường
        AnimData data = getAnimationData();
        if (data == null) return;

        long now = System.nanoTime();
        long deltaMs = (now - lastFrameTime) / 1_000_000;
        lastFrameTime = now;
        frameAccumulator += deltaMs;

        boolean frameChanged = false;
        long frameSpeed;

        // Tính toán tốc độ chuyển frame
        if (data.type() == PlayerSpriteConfig.AnimationType.ACTION_LOOP) {
            // Với các hành động logic, speed đại diện cho tổng thời gian của một chu kỳ
            frameSpeed = data.speed() / data.frameCount();
            if (frameSpeed <= 0) frameSpeed = 1;
        } else {
            // Với hành động di chuyển/đứng yên, speed là thời gian của mỗi frame
            frameSpeed = data.speed();
        }

        // Vòng lặp xử lý logic chuyển frame và bù đắp thời gian trễ
        while (frameAccumulator > frameSpeed) {
            frameChanged = true;
            frameAccumulator -= frameSpeed;
            int nextFrame = currentFrame + 1;

            switch (data.type()) {
                case LOOP:
                case ACTION_LOOP:
                    // Lặp lại liên tục
                    currentFrame = nextFrame % data.frameCount();
                    break;

                case ONE_SHOT:
                    // Chạy một lần rồi dừng ở frame cuối
                    if (nextFrame >= data.frameCount()) {
                        currentFrame = data.frameCount() - 1;
                        frameAccumulator = 0;
                    } else {
                        currentFrame = nextFrame;
                    }
                    break;
            }

            if (frameAccumulator == 0) {
                break;
            }
        }

        if (frameChanged) {
            updateViewport();
        }
    }

    // Thay đổi trạng thái và hướng của nhân vật
    public void setState(PlayerState newState, Direction newDirection) {
        boolean stateChanged = (this.currentState != newState);
        boolean directionChanged = (this.currentDirection != newDirection);

        this.currentState = newState;
        this.currentDirection = newDirection;

        // Reset bộ đếm frame nếu có sự thay đổi để bắt đầu animation mới mượt mà
        if (stateChanged || directionChanged) {
            currentFrame = 0;
            frameAccumulator = 0;
            lastFrameTime = System.nanoTime();
            updateViewport();
        }
    }

    // =================================================================================
    // 5. CÁC HÀM HỖ TRỢ HIỂN THỊ (VIEW HELPERS)
    // =================================================================================

    // Lấy dữ liệu animation dựa trên trạng thái và hướng hiện tại
    private AnimData getAnimationData() {
        Map<Direction, AnimData> stateMap = animationMap.get(currentState);
        if (stateMap == null) return null;

        AnimData data = stateMap.get(currentDirection);
        if (data == null) return null;

        return data;
    }

    // Cập nhật vùng hiển thị (Viewport) của Sprite
    private void updateViewport() {
        AnimData data = getAnimationData();
        if (data == null) return;

        // Đảm bảo Sprite đang sử dụng đúng file ảnh
        Image requiredSheet = stateSheetMap.getOrDefault(currentState, playerSheet);
        if (sprite.getImage() != requiredSheet) {
            sprite.setImage(requiredSheet);
        }

        double frameWidth;
        double frameHeight;

        // Thiết lập kích thước khung hình và vị trí bù trừ tùy theo loại sheet
        if (requiredSheet == playerSheet) {
            frameWidth = PlayerSpriteConfig.PLAYER_FRAME_WIDTH;
            frameHeight = PlayerSpriteConfig.PLAYER_FRAME_HEIGHT;

            sprite.setTranslateX(PlayerSpriteConfig.PLAYER_SPRITE_OFFSET_X);
            sprite.setTranslateY(PlayerSpriteConfig.PLAYER_SPRITE_OFFSET_Y);
            sprite.setScaleY(1.0);

        } else {
            frameWidth = PlayerSpriteConfig.ACTION_FRAME_WIDTH;
            frameHeight = PlayerSpriteConfig.ACTION_FRAME_HEIGHT;

            sprite.setTranslateX(0.0);
            sprite.setTranslateY(0.0);
            sprite.setScaleY(1.0);
        }

        // Áp dụng kích thước hiển thị theo tỷ lệ cấu hình để khớp với container
        sprite.setFitWidth(frameWidth * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE);
        sprite.setFitHeight(frameHeight * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE);

        // Lật ảnh nếu nhân vật đang quay sang trái
        int flipFactor = (currentDirection == Direction.LEFT) ? -1 : 1;
        sprite.setScaleX(flipFactor);

        // Tính toán tọa độ cắt ảnh trên sheet
        double viewportX = currentFrame * frameWidth;
        double viewportY = data.row() * frameHeight;

        sprite.setViewport(new Rectangle2D(viewportX, viewportY, frameWidth, frameHeight));
    }
}