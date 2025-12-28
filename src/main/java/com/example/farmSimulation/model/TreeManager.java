package com.example.farmSimulation.model;

import com.example.farmSimulation.config.TreeConfig;
import com.example.farmSimulation.config.GameLogicConfig;
import com.example.farmSimulation.config.WorldConfig;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Lớp quản lý hệ thống cây tự nhiên.
 * Sử dụng thuật toán sinh ngẫu nhiên theo quy trình để phân bố cây hợp lý trên bản đồ, tránh việc cây mọc chồng chéo lên nhau.
 */
public class TreeManager {
    private final WorldMap worldMap;
    private final Random random;

    private final long worldSeed;
    private long lastUpdateTimeMs = 0;
    private double lastPlayerX = -9999; // Khởi tạo giá trị ảo để đảm bảo cập nhật ngay trong lần đầu tiên
    private double lastPlayerY = -9999;

    private final Set<Long> generatedTiles;

    public TreeManager(WorldMap worldMap) {
        this.worldMap = worldMap;
        this.random = new Random();
        this.worldSeed = random.nextLong();
        this.generatedTiles = new HashSet<>();
    }

    // Chuyển đổi tọa độ x, y thành một khóa duy nhất dạng long để lưu trữ và truy xuất nhanh
    private long toTileKey(int tileX, int tileY) {
        return ((long) tileX << 32) | (tileY & 0xffffffffL);
    }

    /**
     * Kiểm tra mật độ cây xung quanh để đảm bảo khoảng cách hợp lý.
     */
    private boolean hasTreeNearby(int col, int row) {
        // Sử dụng bán kính kiểm tra được quy định trong cấu hình
        int radius = TreeConfig.TREE_SPACING_RADIUS;

        for (int r = row - radius; r <= row + radius; r++) {
            for (int c = col - radius; c <= col + radius; c++) {
                if (c == col && r == row) continue;
                TileData neighbor = worldMap.getTileData(c, r);
                if (neighbor.getBaseTileType() == Tile.TREE || neighbor.getTreeData() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    // Hàm tạo nhiễu xác định dựa trên tọa độ và seed của thế giới.
    // Giúp việc sinh ngẫu nhiên luôn trả về cùng một kết quả tại cùng một vị trí.
    private double getDeterministicNoise(int x, int y) {
        long hash = worldSeed;
        hash ^= (long) x * 73856093;
        hash ^= (long) y * 19349663;
        hash ^= (long) x * y * 83492791;
        hash = (hash ^ (hash >>> 16)) * 0x85ebca6b;
        hash = (hash ^ (hash >>> 13)) * 0xc2b2ae35;
        hash = hash ^ (hash >>> 16);
        return (hash & Long.MAX_VALUE) / (double) Long.MAX_VALUE;
    }

    public boolean updateTrees(long currentTime, double playerX, double playerY) {
        boolean mapNeedsRedraw = false;
        long currentTimeMs = currentTime / 1_000_000;

        // --- PHẦN 1: QUÁ TRÌNH PHÁT TRIỂN CỦA CÂY ---
        if (currentTimeMs - lastUpdateTimeMs >= GameLogicConfig.CROP_UPDATE_INTERVAL_MS) {
            lastUpdateTimeMs = currentTimeMs;
            for (TileData data : worldMap.getAllTileData()) {
                if (data.getTreeData() != null && data.getBaseTileType() == Tile.TREE) {
                    TreeData tree = data.getTreeData();

                    // Xử lý việc gốc cây tự mọc lại sau khi bị chặt
                    if (tree.getChopCount() > 0) {
                        // Gốc cây sẽ bắt đầu mọc lại sau khoảng thời gian quy định
                        if (tree.getRegrowStartTime() > 0) {
                            long timeSinceRegrow = (currentTime - tree.getRegrowStartTime()) / 1_000_000;
                            if (timeSinceRegrow >= TreeConfig.REGROW_TIME_MS) {
                                tree.setChopCount(0); // Đặt lại trạng thái không còn là gốc cây
                                tree.setGrowthStage(TreeConfig.STUMP_REGROW_TARGET_STAGE); // Cây hồi phục ở giai đoạn 2
                                tree.setRegrowStartTime(0);
                                // Điều chỉnh thời gian chặt lần cuối để đồng bộ với logic phát triển.
                                // Việc này đảm bảo khi tính toán lại tuổi của cây, nó sẽ khớp đúng với giai đoạn 2 để tiếp tục lớn lên giai đoạn 3.
                                long targetTimeNanos = TreeConfig.STUMP_REGROW_TARGET_STAGE * TreeConfig.TIME_PER_GROWTH_STAGE_MS * 1_000_000L;
                                tree.setLastChopTime(currentTime - targetTimeNanos);
                                mapNeedsRedraw = true;
                            }
                        }
                    }
                    // Chỉ cho phép cây phát triển tiếp nếu cây đang ở trạng thái bình thường, chưa bị chặt
                    else if (tree.getChopCount() == 0) {
                        // Quy trình phát triển từ hạt giống qua các giai đoạn 0, 1, 2, 3
                        if (tree.getGrowthStage() >= TreeConfig.TREE_SEED_STAGE && tree.getGrowthStage() < TreeConfig.TREE_MAX_GROWTH_STAGE) {
                            long timeSincePlant = tree.getLastChopTime() > 0 ?
                                    (currentTime - tree.getLastChopTime()) / 1_000_000 :
                                    TreeConfig.TIME_PER_GROWTH_STAGE_MS;

                            int targetStage = (int) (timeSincePlant / TreeConfig.TIME_PER_GROWTH_STAGE_MS);
                            targetStage = Math.min(targetStage, TreeConfig.TREE_MAX_GROWTH_STAGE);

                            if (targetStage > tree.getGrowthStage()) {
                                tree.setGrowthStage(targetStage);
                                mapNeedsRedraw = true;
                            }
                        }
                    }
                }
            }
        }

        // --- PHẦN 2: SINH CÂY MỚI TỰ ĐỘNG ---
        // Kiểm tra xem người chơi có di chuyển quá khoảng cách của một ô đất so với lần cập nhật trước hay không
        if (Math.abs(playerX - lastPlayerX) > WorldConfig.TILE_SIZE || Math.abs(playerY - lastPlayerY) > WorldConfig.TILE_SIZE) {
            boolean spawned = generateTreesAroundPlayer(playerX, playerY);
            if (spawned) mapNeedsRedraw = true;

            lastPlayerX = playerX;
            lastPlayerY = playerY;
        }

        return mapNeedsRedraw;
    }

    private boolean generateTreesAroundPlayer(double playerX, double playerY) {
        boolean anyChange = false;
        int playerTileX = (int) Math.floor(playerX / WorldConfig.TILE_SIZE);
        int playerTileY = (int) Math.floor(playerY / WorldConfig.TILE_SIZE);

        int generationRadius = 14;

        for (int row = playerTileY - generationRadius; row <= playerTileY + generationRadius; row++) {
            for (int col = playerTileX - generationRadius; col <= playerTileX + generationRadius; col++) {

                long key = toTileKey(col, row);

                if (generatedTiles.contains(key)) {
                    continue;
                }

                generatedTiles.add(key);

                double noiseValue = getDeterministicNoise(col, row);

                // Sử dụng tỷ lệ xuất hiện cây được định nghĩa trong cấu hình
                if (noiseValue < TreeConfig.TREE_GENERATION_PROBABILITY) {

                    TileData data = worldMap.getTileData(col, row);

                    if (data.getBaseTileType() == Tile.GRASS &&
                            data.getTreeData() == null &&
                            data.getCropData() == null &&
                            data.getFenceData() == null) {

                        if (!hasTreeNearby(col, row)) {
                            data.setBaseTileType(Tile.TREE);
                            // Cây mới sinh ra sẽ luôn ở trạng thái trưởng thành
                            int initialStage = TreeConfig.TREE_MATURE_STAGE;

                            TreeData tree = new TreeData(initialStage);
                            data.setTreeData(tree);
                            worldMap.setTileData(col, row, data);
                            anyChange = true;
                        }
                    }
                }
            }
        }
        return anyChange;
    }
}