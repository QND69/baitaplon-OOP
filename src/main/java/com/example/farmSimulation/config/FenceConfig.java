package com.example.farmSimulation.config;

import javafx.scene.paint.Color;

/**
 * Chứa các hằng số cấu hình logic liên quan đến Hàng rào (Fences).
 */
public class FenceConfig {
    
    // --- Cấu hình Logic ---
    
    /** Số lượng gỗ cần để xây 1 ô hàng rào */
    public static final int WOOD_COST_PER_FENCE = 1;
    
    // --- Cấu hình Sprite ---
    
    /** Kích thước (pixel) của MỘT frame hàng rào trong spritesheet */
    public static final double FENCE_SPRITE_WIDTH = 64.0;
    public static final double FENCE_SPRITE_HEIGHT = 64.0;
    
    /** Độ dịch chuyển Y của hàng rào (để căn chỉnh) */
    public static final double FENCE_Y_OFFSET = 0.0;
    
    // --- Cấu hình Hitbox Collision ---
    
    /** Chiều rộng hitbox của rào (toàn bộ tile) */
    public static final double FENCE_HITBOX_WIDTH = 16.0;
    
    /** Chiều cao hitbox của rào (toàn bộ tile) */
    public static final double FENCE_HITBOX_HEIGHT = 20.0;

    // Offset để đẩy hitbox lên/xuống (căn chỉnh cho khớp hình vẽ)
    // Nếu fence vẽ ở giữa ô thì offset = 16.0 (để đẩy lên giữa)
    public static final double FENCE_HITBOX_Y_OFFSET_FROM_BOTTOM = 18.0;
    
    // --- Cấu hình Debug Hitbox ---
    
    /** Bật/Tắt hiển thị hitbox collision của rào (chỉ khi DEBUG_PLAYER_BOUNDS = true) */
    public static final boolean DEBUG_FENCE_HITBOX = true;
    
    /** Màu viền hitbox của rào */
    public static final Color DEBUG_FENCE_HITBOX_COLOR = Color.CYAN;
    
    private FenceConfig() {
    }
}

