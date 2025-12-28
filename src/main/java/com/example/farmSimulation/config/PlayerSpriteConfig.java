package com.example.farmSimulation.config;

import javafx.scene.paint.Color;

// Chứa các hằng số cấu hình cho Sprite và Animation của Player
public class PlayerSpriteConfig {

    // --- Cấu hình MẶC ĐỊNH ---
    public static final double BASE_PLAYER_FRAME_WIDTH = 192;
    public static final double BASE_PLAYER_FRAME_HEIGHT = 192;
    public static final double BASE_PLAYER_FRAME_SCALE = 0.7;

    // --- Định nghĩa loại Animation ---
    public enum AnimationType {
        LOOP,                   // Lặp vô hạn (IDLE, WALK)
        ONE_SHOT,               // Chạy 1 lần rồi dừng ở frame 0 (ATTACK)
        ACTION_LOOP             // Lặp khi state active (HOE)
    }

    // --- Cấu hình Player Sprite (player_128x128.png) ---
    public static final double PLAYER_FRAME_WIDTH = 128;
    public static final double PLAYER_FRAME_HEIGHT = 128;
    public static final long ANIMATION_SPEED = 120; // Tốc độ mỗi khung hình (ms/frame)

    // --- Cấu hình Player Action Sprite (player_action_scaled_4x.png) ---
    public static final double ACTION_FRAME_WIDTH = 192;
    public static final double ACTION_FRAME_HEIGHT = 192;

    // --- Giá trị gốc (Base values) trước khi scale ---
    // Kích thước Hitbox va chạm gốc (nhỏ hơn người, nằm ở chân)
    public static final double BASE_COLLISION_BOX_WIDTH = 28.0;  // Hẹp hơn chiều rộng người
    public static final double BASE_COLLISION_BOX_HEIGHT = 11.0; // Thấp, chỉ lấy phần chân
    public static final double BASE_COLLISION_BOX_BOTTOM_PADDING = 68.0; // Padding đáy gốc
    
    // Offset gốc để căn giữa nhân vật trong khung mặc định
    public static final double BASE_PLAYER_SPRITE_OFFSET_X = (BASE_PLAYER_FRAME_WIDTH - PLAYER_FRAME_WIDTH) / 2;
    public static final double BASE_PLAYER_SPRITE_OFFSET_Y = (BASE_PLAYER_FRAME_HEIGHT - PLAYER_FRAME_HEIGHT) / 2;

    // Cấu hình Tâm Hoạt Động (Interaction Center) gốc
    // Dịch chuyển tâm hoạt động xuống dưới so với tâm hình học (Giữa ảnh)
    // Giá trị dương = dịch xuống dưới (về phía chân), Giá trị âm = dịch lên trên
    // Ví dụ: 35.0 pixel (ở tỉ lệ gốc 1.0)
    public static final double BASE_INTERACTION_CENTER_Y_OFFSET = 15.0;

    // --- Giá trị đã scale (Scaled values) - TẤT CẢ ĐỀU SCALE THEO BASE_PLAYER_FRAME_SCALE ---
    // Offset để căn giữa nhân vật trong khung mặc định (đã scale)
    public static final double PLAYER_SPRITE_OFFSET_X = BASE_PLAYER_SPRITE_OFFSET_X * BASE_PLAYER_FRAME_SCALE;
    public static final double PLAYER_SPRITE_OFFSET_Y = BASE_PLAYER_SPRITE_OFFSET_Y * BASE_PLAYER_FRAME_SCALE;

    // Kích thước Hitbox va chạm (đã scale)
    public static final double COLLISION_BOX_WIDTH = BASE_COLLISION_BOX_WIDTH * BASE_PLAYER_FRAME_SCALE;
    public static final double COLLISION_BOX_HEIGHT = BASE_COLLISION_BOX_HEIGHT * BASE_PLAYER_FRAME_SCALE;
    
    /** Padding đáy để căn chỉnh hitbox collision (tính từ đáy sprite lên trên, đã scale) */
    public static final double COLLISION_BOX_BOTTOM_PADDING = BASE_COLLISION_BOX_BOTTOM_PADDING * BASE_PLAYER_FRAME_SCALE;

    // Offset tâm hoạt động đã scale
    public static final double INTERACTION_CENTER_Y_OFFSET = BASE_INTERACTION_CENTER_Y_OFFSET * BASE_PLAYER_FRAME_SCALE;

    // --- Hằng số Debug ---
    /**
     * Bật/Tắt hiển thị khung viền (bounding box)
     * và tâm (center dot) của player.
     */
    public static final boolean DEBUG_PLAYER_BOUNDS = false;
    public static final Color DEBUG_BOUNDING_BOX_COLOR = Color.RED;
    public static final Color DEBUG_CENTER_DOT_COLOR = Color.CYAN;
    public static final Color DEBUG_RANGE_COLOR = Color.GREENYELLOW;
    public static final Color DEBUG_COLLISION_HITBOX_COLOR = Color.ORANGE;

    // --- DỮ LIỆU ANIMATION ---

    // IDLE (player_128x128.png)
    public static final int IDLE_DOWN_ROW = 0;
    public static final int IDLE_RIGHT_ROW = 1;
    public static final int IDLE_LEFT_ROW = 1; // Dùng chung ảnh với RIGHT
    public static final int IDLE_UP_ROW = 2;
    public static final int IDLE_FRAMES = 6; // Số frame của hành động

    // WALK (player_128x128.png)
    public static final int WALK_DOWN_ROW = 3;
    public static final int WALK_RIGHT_ROW = 4;
    public static final int WALK_LEFT_ROW = 4; // Dùng chung ảnh với RIGHT
    public static final int WALK_UP_ROW = 5;
    public static final int WALK_FRAMES = 6; // Số frame của hành động

    // ATTACK (player_128x128.png)
    public static final int ATTACK_DOWN_ROW = 6;
    public static final int ATTACK_RIGHT_ROW = 7;
    public static final int ATTACK_LEFT_ROW = 7; // Dùng chung ảnh với RIGHT
    public static final int ATTACK_UP_ROW = 8;
    public static final int ATTACK_FRAMES = 4; // Số frame của hành động
    public static final long ATTACK_SPEED = 100; // Tốc độ tấn công

    // HOE (Cuốc)
    public static final int HOE_FRAMES = 2; // Số frame của hành động
    public static final int HOE_DOWN_ROW = 7;
    public static final int HOE_UP_ROW = 8;
    public static final int HOE_RIGHT_ROW = 6;
    public static final int HOE_LEFT_ROW = 6; // Dùng chung ảnh với RIGHT

    // WATER (Tưới nước)
    public static final int WATER_FRAMES = 2; // Số frame của hành động
    public static final int WATER_DOWN_ROW = 9;
    public static final int WATER_UP_ROW = 10;
    public static final int WATER_RIGHT_ROW = 11;
    public static final int WATER_LEFT_ROW = 11;

    // PLANT (Trồng cây)
    public static final int PLANT_FRAMES = 2;
    public static final int PLANT_DOWN_ROW = 12;
    public static final int PLANT_UP_ROW = 14;
    public static final int PLANT_RIGHT_ROW = 13;
    public static final int PLANT_LEFT_ROW = 13;

    // DIG (Xúc đất)
    public static final int DIG_FRAMES = 2;
    public static final int DIG_DOWN_ROW = 16;
    public static final int DIG_UP_ROW = 15;
    public static final int DIG_RIGHT_ROW = 15;
    public static final int DIG_LEFT_ROW = 17;

    // FERTILZED (Bón phân)
    public static final int FERTILZED_FRAMES = 2;
    public static final int FERTILZED_DOWN_ROW = 18;
    public static final int FERTILZED_UP_ROW = 19;
    public static final int FERTILZED_RIGHT_ROW = 20;
    public static final int FERTILZED_LEFT_ROW = 20;

    // AXE (Chặt cây bằng rìu) - Dùng player_action_192x192.png
    public static final int AXE_FRAMES = 2; // Số frame của hành động
    public static final int AXE_DOWN_ROW = 4; // Row 8 trong player_action_192x192.png
    public static final int AXE_UP_ROW = 5; // Row 9 trong player_action_192x192.png
    public static final int AXE_RIGHT_ROW = 3; // Row 10 trong player_action_192x192.png
    public static final int AXE_LEFT_ROW = 3; // Row 11 trong player_action_192x192.png
    public static final long AXE_SPEED = 100; // Tốc độ chặt cây (ms/frame)

    // DEAD (Ngất) - Dùng player_128x128.png
    public static final int DEAD_ROW = 9; // The last row, index 7
    public static final int DEAD_FRAMES = 4; // Số frame của hành động death
    public static final long DEAD_SPEED = 200; // Tốc độ animation death (ms/frame)

    private PlayerSpriteConfig() {}
}