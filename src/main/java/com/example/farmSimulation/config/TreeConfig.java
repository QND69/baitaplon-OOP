package com.example.farmSimulation.config;

import javafx.scene.paint.Color;

/**
 * Chứa các hằng số cấu hình logic liên quan đến Cây tự nhiên (Trees).
 */
public class TreeConfig {

    // --- Cấu hình Logic ---

    /** Thời gian (ms) cho mỗi giai đoạn phát triển của cây (từ gốc lên cây trưởng thành) */
    public static final long TIME_PER_GROWTH_STAGE_MS = 30_000; // 60 giây / giai đoạn

    /** Thời gian (ms) để cây mọc lại từ gốc (stage 0) lên stage 1 */
    public static final long REGROW_TIME_MS = TIME_PER_GROWTH_STAGE_MS; // 30 giây

    /** Số lượng gỗ thu được từ mỗi stage (stage càng cao càng nhiều gỗ) */
    public static final int WOOD_PER_STAGE_1 = 1; // Stage 1 (cây nhỏ)
    public static final int WOOD_PER_STAGE_2 = 2; // Stage 2 (cây trưởng thành)

    /** Tỷ lệ xuất hiện cây (Procedural Generation) 
     * 0.15 = 15% ô đất hợp lệ sẽ có cây.
     * Muốn thưa hơn -> GIẢM xuống (ví dụ: 0.05 hoặc 0.08)
     * Muốn dày hơn -> TĂNG lên (ví dụ: 0.3)
     */
    public static final double TREE_GENERATION_PROBABILITY = 0.015;

    /** Bán kính "giãn cách xã hội" giữa các cây.
     * Càng lớn thì các cây càng cách xa nhau.
     * 2 = cách nhau ít nhất 2 ô.
     */
    public static final int TREE_SPACING_RADIUS = 3;

    /** Khoảng cách tối thiểu từ player để cây có thể mọc (tiles) */
    public static final int MIN_SPAWN_DISTANCE_FROM_PLAYER = 10;

    /** Khoảng cách tối đa từ player để cây có thể mọc (tiles) */
    public static final int MAX_SPAWN_DISTANCE_FROM_PLAYER = 15;

    // --- Cấu hình Sprite ---

    /** Kích thước (pixel) của MỘT frame cây trong spritesheet */
    public static final double TREE_SPRITE_WIDTH = 64.0;
    public static final double TREE_SPRITE_HEIGHT = 96.0;

    /** Độ dịch chuyển Y của cây (để căn chỉnh, dịch lên trên so với tile) */
    /** * Độ dịch chuyển Y (Offset) để vẽ cây.
     * Logic:
     * 1. (TREE_SPRITE_HEIGHT - WorldConfig.TILE_SIZE): Kéo lên để gốc cây vừa chạm đáy ô đất (96 - 64 = 32).
     * 2. + CropConfig.CROP_Y_OFFSET: Kéo lên tiếp một đoạn nữa cho giống vị trí của Crop (thêm 16).
     * Kết quả: 32 + 16 = 48.0
     */
    public static final double TREE_Y_OFFSET = (TREE_SPRITE_HEIGHT - WorldConfig.TILE_SIZE) + CropConfig.CROP_Y_OFFSET;

    /** Stage tối đa mà cây có thể phát triển (Stage 3 = cây trưởng thành) */
    public static final int TREE_MAX_GROWTH_STAGE = 3;

    /** Stage tối thiểu để có thể chặt lấy gỗ và để lại gốc (Stage 2) */
    public static final int TREE_MIN_CHOP_STAGE = 2;

    /** Frame index cho gốc cây (stump) trong spritesheet */
    public static final int TREE_STUMP_FRAME_INDEX = 4;

    /** Stage hạt giống cây (Seed/Sprout) */
    public static final int TREE_SEED_STAGE = 0;

    /** Stage cây trưởng thành (Mature Tree) */
    public static final int TREE_MATURE_STAGE = 3;

    /** Stage mà gốc cây mọc lại thành (Stump regrows to this stage) */
    public static final int STUMP_REGROW_TARGET_STAGE = 2;

    // --- Cấu hình Hitbox Collision ---

    /** Chiều rộng hitbox của cây (phần gốc cây, căn giữa) */
    public static final double TREE_HITBOX_WIDTH = 22.0;

    /** Chiều cao hitbox của cây (phần gốc cây, nằm ở dưới sprite) */
    public static final double TREE_HITBOX_HEIGHT = 12.0;

    /** Offset Y của hitbox so với đáy sprite cây (tính từ đáy sprite lên trên) */
    public static final double TREE_HITBOX_Y_OFFSET_FROM_BOTTOM = 10.0;

    // --- Cấu hình Debug Hitbox ---

    /** Bật/Tắt hiển thị hitbox collision của cây (chỉ khi DEBUG_PLAYER_BOUNDS = true) */
    public static final boolean DEBUG_TREE_HITBOX = true;

    /** Màu viền hitbox của cây */
    public static final Color DEBUG_TREE_HITBOX_COLOR = Color.MAGENTA;

    private TreeConfig() {}
}