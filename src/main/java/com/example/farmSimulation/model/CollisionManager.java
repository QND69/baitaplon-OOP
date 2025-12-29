package com.example.farmSimulation.model;

import com.example.farmSimulation.config.CropConfig;
import com.example.farmSimulation.config.FenceConfig;
import com.example.farmSimulation.config.TreeConfig;
import com.example.farmSimulation.config.WorldConfig;

/**
 * Lớp quản lý va chạm (collision) cho người chơi.
 * Chịu trách nhiệm kiểm tra xem người chơi có thể di chuyển đến một vị trí cụ thể hay không.
 */
public class CollisionManager {
    private final WorldMap worldMap;

    public CollisionManager(WorldMap worldMap) {
        this.worldMap = worldMap;
    }

    /**
     * Kiểm tra xem một vị trí tọa độ cụ thể có thể đi qua được không.
     * @param tileX Tọa độ X thực (tính theo pixel)
     * @param tileY Tọa độ Y thực (tính theo pixel)
     * @return true nếu có thể đi qua, false nếu bị chặn bởi vật cản
     */
    public boolean canPassThrough(double tileX, double tileY) {
        int col = (int) Math.floor(tileX / WorldConfig.TILE_SIZE);
        int row = (int) Math.floor(tileY / WorldConfig.TILE_SIZE);

        TileData data = worldMap.getTileData(col, row);

        // Kiểm tra va chạm với hàng rào
        if (data.getFenceData() != null && data.getFenceData().isSolid()) {
            // 1. Tính toán vùng va chạm trung tâm (cột rào)
            double tileWorldX = col * WorldConfig.TILE_SIZE;
            double tileWorldY = row * WorldConfig.TILE_SIZE;

            double tileCenterX = tileWorldX + (WorldConfig.TILE_SIZE / 2.0);
            double tileBottomY = tileWorldY + WorldConfig.TILE_SIZE;

            // Xác định tâm của cột hàng rào
            double centerHitboxX = tileCenterX;
            double centerHitboxY = tileBottomY
                    - (FenceConfig.FENCE_HITBOX_HEIGHT / 2.0)
                    - FenceConfig.FENCE_HITBOX_Y_OFFSET_FROM_BOTTOM;

            double halfW = FenceConfig.FENCE_HITBOX_WIDTH / 2.0;
            double halfH = FenceConfig.FENCE_HITBOX_HEIGHT / 2.0;

            // --- Kiểm tra 1: Cột trung tâm ---
            if (tileX >= centerHitboxX - halfW && tileX <= centerHitboxX + halfW &&
                    tileY >= centerHitboxY - halfH && tileY <= centerHitboxY + halfH) {
                return false;
            }

            // 2. Tính toán các thanh nối dựa trên mẫu hình dạng của hàng rào
            int pattern = data.getFenceData().getTilePattern();

            // Bit 0: Trên, Bit 1: Phải, Bit 2: Dưới, Bit 3: Trái
            boolean hasTop = (pattern & 1) != 0;
            boolean hasRight = (pattern & 2) != 0;
            boolean hasBottom = (pattern & 4) != 0;
            boolean hasLeft = (pattern & 8) != 0;

            // --- Kiểm tra 2: Thanh nối sang TRÁI ---
            if (hasLeft) {
                // Vùng từ mép trái của ô lưới đến mép trái của cột trung tâm
                double railLeftX = tileWorldX;
                double railRightX = centerHitboxX - halfW;

                if (tileX >= railLeftX && tileX <= railRightX &&
                        tileY >= centerHitboxY - halfH && tileY <= centerHitboxY + halfH) { // Tọa độ Y giữ nguyên theo cột
                    return false;
                }
            }

            // --- Kiểm tra 3: Thanh nối sang PHẢI ---
            if (hasRight) {
                // Vùng từ mép phải của cột trung tâm đến mép phải của ô lưới
                double railLeftX = centerHitboxX + halfW;
                double railRightX = tileWorldX + WorldConfig.TILE_SIZE;

                if (tileX >= railLeftX && tileX <= railRightX &&
                        tileY >= centerHitboxY - halfH && tileY <= centerHitboxY + halfH) {
                    return false;
                }
            }

            // --- Kiểm tra 4: Thanh nối lên TRÊN ---
            if (hasTop) {
                // Vùng từ mép trên của ô lưới đến mép trên của cột
                // Lưu ý: về mặt hình ảnh thì rào dọc thường mỏng hơn rào ngang một chút, nhưng để xử lý va chạm thì ta lấy chiều rộng bằng với cột rào
                double railTopY = tileWorldY;
                double railBottomY = centerHitboxY - halfH;

                if (tileX >= centerHitboxX - halfW && tileX <= centerHitboxX + halfW &&
                        tileY >= railTopY && tileY <= railBottomY) {
                    return false;
                }
            }

            // --- Kiểm tra 5: Thanh nối xuống DƯỚI ---
            if (hasBottom) {
                double railTopY = centerHitboxY + halfH;
                double railBottomY = tileWorldY + WorldConfig.TILE_SIZE;

                if (tileX >= centerHitboxX - halfW && tileX <= centerHitboxX + halfW &&
                        tileY >= railTopY && tileY <= railBottomY) {
                    return false;
                }
            }
        }

        // Kiểm tra va chạm với cây trồng
        // Hạt giống (giai đoạn 0) không có va chạm, nhân vật có thể đi qua được
        // Chỉ cây ở giai đoạn lớn hơn 0 mới có va chạm
        if (data.getTreeData() != null && data.getBaseTileType() == Tile.TREE) {
            TreeData tree = data.getTreeData();
            // Hạt giống (giai đoạn 0) cho phép đi qua
            // Chỉ bắt đầu chặn khi cây đã lớn hơn hạt giống
            if (tree.getGrowthStage() > TreeConfig.TREE_SEED_STAGE) {
                double tileWorldX = col * WorldConfig.TILE_SIZE;
                double tileWorldY = row * WorldConfig.TILE_SIZE;

                // Logic tính toán vị trí chuẩn:
                // Đáy của ô lưới là (tileWorldY + 64).
                // Hình ảnh cây được vẽ dịch lên trên 16px.
                // Do đó đáy thực tế của cây (nơi cần chặn va chạm) là đáy ô lưới trừ đi 16px.
                double visualTreeBottomY = (tileWorldY + WorldConfig.TILE_SIZE) - CropConfig.CROP_Y_OFFSET;

                // Tâm vùng va chạm theo trục X: Giữa ô lưới
                double hitboxCenterX = tileWorldX + WorldConfig.TILE_SIZE / 2.0;

                // Tâm vùng va chạm theo trục Y: Từ đáy thực tế, nhích lên một nửa chiều cao vùng va chạm
                // Trừ thêm một khoảng offset để đẩy tâm vùng va chạm lên cao hơn cho khớp với gốc cây
                double hitboxCenterY = visualTreeBottomY
                        - (TreeConfig.TREE_HITBOX_HEIGHT / 2.0)
                        - TreeConfig.TREE_HITBOX_Y_OFFSET_FROM_BOTTOM;

                double halfWidth = TreeConfig.TREE_HITBOX_WIDTH / 2.0;
                double halfHeight = TreeConfig.TREE_HITBOX_HEIGHT / 2.0;

                // Kiểm tra hộp giới hạn thẳng hàng với trục (AABB)
                if (tileX >= hitboxCenterX - halfWidth &&
                        tileX <= hitboxCenterX + halfWidth &&
                        tileY >= hitboxCenterY - halfHeight &&
                        tileY <= hitboxCenterY + halfHeight) {
                    return false; // Phát hiện va chạm -> Chặn lại
                }
            }
        }

        // Kiểm tra va chạm với nước (không thể đi xuống nước)
        if (data.getBaseTileType() == Tile.WATER) {
            return false;
        }

        return true;
    }

    /**
     * Kiểm tra va chạm với một hộp giới hạn hình chữ nhật (Bounding Box).
     * @param centerX Tọa độ X trung tâm của hộp
     * @param centerY Tọa độ Y trung tâm của hộp
     * @param width Chiều rộng hộp
     * @param height Chiều cao hộp
     * @return true nếu có va chạm, false nếu không
     */
    /**
     * Kiểm tra va chạm kỹ hơn bằng cách thêm điểm giữa và các cạnh.
     * Việc này giúp khắc phục lỗi nhân vật đi xuyên qua vật cản khi vùng va chạm của cây quá nhỏ.
     */
    public boolean checkCollision(double centerX, double centerY, double width, double height) {
        double halfW = width / 2.0;
        double halfH = height / 2.0;

        // 1. Kiểm tra 4 góc
        if (!canPassThrough(centerX - halfW, centerY - halfH)) return true; // Góc trên trái
        if (!canPassThrough(centerX + halfW, centerY - halfH)) return true; // Góc trên phải
        if (!canPassThrough(centerX - halfW, centerY + halfH)) return true; // Góc dưới trái
        if (!canPassThrough(centerX + halfW, centerY + halfH)) return true; // Góc dưới phải

        // 2. Kiểm tra 4 trung điểm của các cạnh (quan trọng)
        // Giúp chặn việc đi xuyên khi vật cản nhỏ hơn chiều rộng của người chơi
        if (!canPassThrough(centerX, centerY - halfH)) return true; // Giữa cạnh trên
        if (!canPassThrough(centerX, centerY + halfH)) return true; // Giữa cạnh dưới
        if (!canPassThrough(centerX - halfW, centerY)) return true; // Giữa cạnh trái
        if (!canPassThrough(centerX + halfW, centerY)) return true; // Giữa cạnh phải

        // 3. Kiểm tra tâm (dự phòng)
        if (!canPassThrough(centerX, centerY)) return true;

        return false;
    }
}