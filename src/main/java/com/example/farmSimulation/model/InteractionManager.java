package com.example.farmSimulation.model;

import com.example.farmSimulation.config.CropConfig;
import com.example.farmSimulation.config.GameLogicConfig;
import com.example.farmSimulation.config.HudConfig;
import com.example.farmSimulation.config.ItemSpriteConfig;
import com.example.farmSimulation.config.TreeConfig;
import com.example.farmSimulation.view.PlayerView;
import com.example.farmSimulation.config.FenceConfig;
import com.example.farmSimulation.config.PlayerSpriteConfig;
import com.example.farmSimulation.config.WorldConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.Random;

@Getter
@Setter
/**
 * Quản lý logic tương tác dựa trên quy tắc.
 * Chịu trách nhiệm xử lý hành động của người chơi lên ô đất, cây trồng và động vật.
 */
public class InteractionManager {
    private final ActionManager actionManager; // Quản lý việc thêm hành động vào hàng đợi
    private AnimalManager animalManager;       // Quản lý danh sách và trạng thái động vật
    private CollisionManager collisionManager; // Quản lý va chạm vật lý
    private WorldMap worldMap;                 // Tham chiếu đến dữ liệu bản đồ

    // Cấu trúc dữ liệu nội bộ để lưu trữ kết quả tính toán trước khi thực thi
    private record InteractionResult(
            TileData newTileData,               // Dữ liệu mới của ô đất sau khi tương tác
            PlayerView.PlayerState playerState, // Trạng thái hoạt ảnh của người chơi cần kích hoạt
            long totalDurationMs,               // Tổng thời gian thực hiện hành động
            boolean consumeItem,                // Có tiêu thụ vật phẩm trên tay hay không
            ItemType harvestedItem,             // Vật phẩm thu hoạch được (để tạo hiệu ứng)
            int harvestedAmount                 // Số lượng vật phẩm thu hoạch
    ) {
    }

    public InteractionManager(ActionManager actionManager) {
        this.actionManager = actionManager;
    }

    // ==============================================================================================
    // CÁC HÀM TIỆN ÍCH HỖ TRỢ (UTILITY HELPERS)
    // ==============================================================================================

    /**
     * Kiểm tra xem túi đồ của người chơi có còn chỗ trống để thêm vật phẩm không.
     * Hàm này kiểm tra cả việc cộng dồn vào slot có sẵn và slot trống mới.
     */
    private boolean canAddItem(Player player, ItemType type, int amount) {
        ItemStack[] hotbarItems = player.getHotbarItems();
        int maxStackSize = type.getMaxStackSize();

        // Kiểm tra khả năng cộng dồn vào các ô đang chứa vật phẩm cùng loại
        for (ItemStack stack : hotbarItems) {
            if (stack != null && stack.getItemType() == type) {
                if (stack.getQuantity() + amount <= maxStackSize) {
                    return true;
                }
                amount -= (maxStackSize - stack.getQuantity());
                if (amount <= 0) return true;
            }
        }

        // Nếu vẫn còn dư, kiểm tra xem có ô trống hoàn toàn nào không
        for (ItemStack stack : hotbarItems) {
            if (stack == null) {
                return true;
            }
        }

        return false; // Túi đồ đã đầy
    }

    /**
     * Chuyển đổi thời gian từ mili-giây sang số lượng khung hình (frames) dựa trên chuẩn 60 FPS.
     * Đảm bảo luôn có độ trễ tối thiểu để tránh lỗi hiển thị.
     */
    private int getDelayInFrames(long durationMs) {
        int delay = (int) ((durationMs / 1000.0) * 60);

        // Đặt mức tối thiểu là 10 frames cho mọi hành động.
        // Điều này ngăn chặn việc hành động hoàn thành quá nhanh gây lỗi hình ảnh (glitch)
        // hoặc khi bộ đếm thời gian (deltaTime) không ổn định.
        return Math.max(10, delay);
    }

    /**
     * Tạo dữ liệu ô đất mới đã được reset về trạng thái đất khô cơ bản.
     * Giữ lại vật phẩm đang rơi trên đất (nếu có).
     */
    private TileData createResetTileData(TileData currentData) {
        TileData newData = new TileData(currentData);
        newData.setBaseTileType(Tile.SOIL);
        newData.setCropData(null);
        newData.setWatered(false);
        newData.setFertilized(false);
        newData.setDryStartTime(System.nanoTime());
        newData.setFertilizerStartTime(0);
        newData.setStatusIndicator(CropStatusIndicator.NONE);
        // Lưu ý: Không xóa groundItem để tránh mất vật phẩm đang nằm trên ô đó
        return newData;
    }

    // ==============================================================================================
    // HỆ THỐNG XỬ LÝ VA CHẠM (COLLISION HANDLING)
    // ==============================================================================================

    /**
     * Kiểm tra va chạm giữa hai hình chữ nhật (AABB - Axis Aligned Bounding Box).
     */
    private boolean aabbIntersect(double minX1, double maxX1, double minY1, double maxY1,
                                  double minX2, double maxX2, double minY2, double maxY2) {
        return minX1 < maxX2 && maxX1 > minX2 && minY1 < maxY2 && maxY1 > minY2;
    }

    /**
     * Kiểm tra xem một ô có chứa hàng rào rắn hay không.
     * Dùng để tính toán việc nối liền các hàng rào cạnh nhau.
     */
    private boolean isFenceSolid(int col, int row, WorldMap worldMap) {
        TileData data = worldMap.getTileData(col, row);
        return data.getBaseTileType() == Tile.FENCE && data.getFenceData() != null && data.getFenceData().isSolid();
    }

    /**
     * Kiểm tra xem người chơi có đang đứng chắn vị trí tương tác hay không.
     * Tính toán dựa trên hộp va chạm (hitbox) thực tế của người chơi và ô đất/hàng rào.
     */
    private boolean isPlayerBlocking(int col, int row, Player mainPlayer, WorldMap worldMap) {
        // Tính toán Hitbox của người chơi trong không gian thế giới
        double pX = mainPlayer.getTileX();
        double pY = mainPlayer.getTileY();

        double scaledPlayerWidth = PlayerSpriteConfig.BASE_PLAYER_FRAME_WIDTH * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
        double scaledPlayerHeight = PlayerSpriteConfig.BASE_PLAYER_FRAME_HEIGHT * PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;

        double pCenterX = pX + (scaledPlayerWidth / 2.0);
        double pCenterY = pY + scaledPlayerHeight
                - (PlayerSpriteConfig.COLLISION_BOX_HEIGHT / 2.0)
                - PlayerSpriteConfig.COLLISION_BOX_BOTTOM_PADDING;

        double pHalfW = PlayerSpriteConfig.COLLISION_BOX_WIDTH / 2.0;
        double pHalfH = PlayerSpriteConfig.COLLISION_BOX_HEIGHT / 2.0;

        double pMinX = pCenterX - pHalfW;
        double pMaxX = pCenterX + pHalfW;
        double pMinY = pCenterY - pHalfH;
        double pMaxY = pCenterY + pHalfH;

        // Tính toán Hitbox của cọc hàng rào tại vị trí định đặt
        double tileWorldX = col * WorldConfig.TILE_SIZE;
        double tileWorldY = row * WorldConfig.TILE_SIZE;

        double fCenterX = tileWorldX + WorldConfig.TILE_SIZE / 2.0;
        double fCenterY = (tileWorldY + WorldConfig.TILE_SIZE)
                - (FenceConfig.FENCE_HITBOX_HEIGHT / 2.0)
                - FenceConfig.FENCE_HITBOX_Y_OFFSET_FROM_BOTTOM;

        double fHalfW = FenceConfig.FENCE_HITBOX_WIDTH / 2.0;
        double fHalfH = FenceConfig.FENCE_HITBOX_HEIGHT / 2.0;

        double fMinX = fCenterX - fHalfW;
        double fMaxX = fCenterX + fHalfW;
        double fMinY = fCenterY - fHalfH;
        double fMaxY = fCenterY + fHalfH;

        // Kiểm tra va chạm với cọc trung tâm
        if (aabbIntersect(pMinX, pMaxX, pMinY, pMaxY, fMinX, fMaxX, fMinY, fMaxY)) {
            return true;
        }

        // Kiểm tra va chạm với các thanh nối (Rails) sang các ô lân cận
        if (isFenceSolid(col - 1, row, worldMap)) {
            if (aabbIntersect(pMinX, pMaxX, pMinY, pMaxY, tileWorldX, fMinX, fMinY, fMaxY)) return true;
            double neighborPostRightX = (tileWorldX - (WorldConfig.TILE_SIZE / 2.0)) + fHalfW;
            if (aabbIntersect(pMinX, pMaxX, pMinY, pMaxY, neighborPostRightX, tileWorldX, fMinY, fMaxY)) return true;
        }
        if (isFenceSolid(col + 1, row, worldMap)) {
            if (aabbIntersect(pMinX, pMaxX, pMinY, pMaxY, fMaxX, tileWorldX + WorldConfig.TILE_SIZE, fMinY, fMaxY)) return true;
            double neighborPostLeftX = (tileWorldX + WorldConfig.TILE_SIZE + (WorldConfig.TILE_SIZE / 2.0)) - fHalfW;
            if (aabbIntersect(pMinX, pMaxX, pMinY, pMaxY, tileWorldX + WorldConfig.TILE_SIZE, neighborPostLeftX, fMinY, fMaxY)) return true;
        }
        if (isFenceSolid(col, row - 1, worldMap)) {
            double neighborPostBottomY = fCenterY - WorldConfig.TILE_SIZE;
            if (aabbIntersect(pMinX, pMaxX, pMinY, pMaxY, fMinX, fMaxX, tileWorldY, fMinY)) return true;
            if (aabbIntersect(pMinX, pMaxX, pMinY, pMaxY, fMinX, fMaxX, neighborPostBottomY + fHalfH, tileWorldY)) return true;
        }
        if (isFenceSolid(col, row + 1, worldMap)) {
            double neighborPostTopY = fCenterY + WorldConfig.TILE_SIZE;
            if (aabbIntersect(pMinX, pMaxX, pMinY, pMaxY, fMinX, fMaxX, fMaxY, tileWorldY + WorldConfig.TILE_SIZE)) return true;
            if (aabbIntersect(pMinX, pMaxX, pMinY, pMaxY, fMinX, fMaxX, tileWorldY + WorldConfig.TILE_SIZE, neighborPostTopY - fHalfH)) return true;
        }

        return false;
    }

    /**
     * Kiểm tra xem có động vật nào đang đứng tại vị trí ô đất định tương tác không.
     * Sử dụng vùng va chạm của toàn bộ ô đất (64x64) để đảm bảo không đặt vật thể đè lên động vật.
     */
    private boolean isAnimalBlocking(int col, int row) {
        if (animalManager == null) return false;

        double tileMinX = col * WorldConfig.TILE_SIZE;
        double tileMaxX = tileMinX + WorldConfig.TILE_SIZE;

        double tileMinY = row * WorldConfig.TILE_SIZE;
        double tileMaxY = tileMinY + WorldConfig.TILE_SIZE;

        for (Animal animal : animalManager.getAnimals()) {
            if (animal.isDead()) continue;

            double aX = animal.getX();
            double aY = animal.getY();
            double aHalfW = animal.getType().getHitboxWidth() / 2.0;
            double aHalfH = animal.getType().getHitboxHeight() / 2.0;

            double aMinX = aX - aHalfW;
            double aMaxX = aX + aHalfW;
            double aMinY = aY - aHalfH;
            double aMaxY = aY + aHalfH;

            // Kiểm tra va chạm giữa ô đất và động vật
            if (aabbIntersect(tileMinX, tileMaxX, tileMinY, tileMaxY, aMinX, aMaxX, aMinY, aMaxY)) {
                return true;
            }
        }
        return false;
    }

    // ==============================================================================================
    // LOGIC TÍNH TOÁN TƯƠNG TÁC (CALCULATION LOGIC)
    // ==============================================================================================

    /**
     * Trừ thể lực của người chơi dựa trên loại công cụ sử dụng.
     */
    private void consumeStaminaForAction(ItemType itemType, Player player) {
        if (player == null || itemType == null) return;

        double staminaCost = 0.0;
        if (itemType == ItemType.HOE) {
            staminaCost = GameLogicConfig.STAMINA_COST_HOE;
        } else if (itemType == ItemType.WATERING_CAN) {
            staminaCost = GameLogicConfig.STAMINA_COST_WATERING_CAN;
        } else if (itemType == ItemType.AXE) {
            staminaCost = GameLogicConfig.STAMINA_COST_AXE;
        } else if (itemType == ItemType.PICKAXE) {
            staminaCost = GameLogicConfig.STAMINA_COST_PICKAXE;
        } else if (itemType == ItemType.SHOVEL) {
            staminaCost = GameLogicConfig.STAMINA_COST_SHOVEL;
        } else if (itemType.name().startsWith("SEEDS_")) {
            staminaCost = GameLogicConfig.STAMINA_COST_PLANT;
        } else if (itemType == ItemType.FERTILIZER) {
            staminaCost = GameLogicConfig.STAMINA_COST_FERTILIZER;
        }

        if (staminaCost > 0) {
            player.reduceStamina(staminaCost);
        }
    }

    /**
     * Xác định kết quả tương tác dựa trên công cụ đang cầm và dữ liệu ô đất hiện tại.
     * Hàm này chỉ tính toán, không thực hiện thay đổi dữ liệu gốc ngay lập tức.
     */
    private InteractionResult calculateInteractionResult(ItemStack currentStack, TileData currentData, Player mainPlayer, WorldMap worldMap, int col, int row) {
        if (currentStack == null) return null; // Không cầm gì

        ItemType itemType = currentStack.getItemType();
        Tile baseTile = currentData.getBaseTileType();

        // CUỐC (HOE): Chuyển cỏ thành đất trồng
        if (itemType == ItemType.HOE && baseTile == Tile.GRASS) {
            TileData newData = new TileData(currentData);
            newData.setBaseTileType(Tile.SOIL);
            newData.setDryStartTime(System.nanoTime());
            long duration = (long) GameLogicConfig.HOE_REPETITIONS * GameLogicConfig.HOE_DURATION_PER_REPETITION_MS;
            return new InteractionResult(newData, PlayerView.PlayerState.HOE, duration, true, null, 0);
        }

        // HẠT GIỐNG: Gieo trồng
        if (itemType.name().startsWith("SEEDS_")) {
            // Hạt giống cây ăn quả/gỗ (Tree): Chỉ trồng trên cỏ
            if (itemType == ItemType.SEEDS_TREE) {
                if (baseTile == Tile.GRASS && currentData.getCropData() == null &&
                        currentData.getTreeData() == null && currentData.getFenceData() == null) {

                    if (isPlayerBlocking(col, row, mainPlayer, worldMap)) return null;
                    if (isAnimalBlocking(col, row)) return null;

                    TileData newData = new TileData(currentData);
                    newData.setBaseTileType(Tile.TREE);
                    TreeData tree = new TreeData(TreeConfig.TREE_SEED_STAGE);
                    tree.setLastChopTime(System.nanoTime());
                    tree.setRegrowStartTime(0);
                    newData.setTreeData(tree);
                    return new InteractionResult(newData, PlayerView.PlayerState.PLANT, GameLogicConfig.PLANT_DURATION_MS, true, null, 0);
                }
            }
            // Các loại hạt giống nông sản: Chỉ trồng trên đất đã cuốc
            else if ((baseTile == Tile.SOIL || baseTile == Tile.SOIL_WET) && currentData.getCropData() == null) {
                try {
                    CropType type = CropType.valueOf(itemType.name().substring(6));
                    TileData newData = new TileData(currentData);
                    newData.setCropData(new CropData(type, 0, System.nanoTime()));
                    if (baseTile == Tile.SOIL) {
                        newData.setDryStartTime(System.nanoTime());
                    } else {
                        newData.setDryStartTime(0);
                    }
                    return new InteractionResult(newData, PlayerView.PlayerState.PLANT, GameLogicConfig.PLANT_DURATION_MS, true, null, 0);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // BÌNH TƯỚI NƯỚC
        if (itemType == ItemType.WATERING_CAN) {
            if (currentStack.getCurrentDurability() <= 0) return null;

            if (baseTile == Tile.SOIL && currentData.getCropData() != null) {
                TileData newData = new TileData(currentData);
                newData.setBaseTileType(Tile.SOIL_WET);
                newData.setWatered(true);
                newData.setLastWateredTime(System.nanoTime());
                newData.setDryStartTime(0);
                long duration = (long) GameLogicConfig.WATERING_CAN_REPETITIONS * GameLogicConfig.WATERING_CAN_DURATION_PER_REPETITION_MS;
                return new InteractionResult(newData, PlayerView.PlayerState.WATER, duration, true, null, 0);
            }
        }

        // PHÂN BÓN
        if (itemType == ItemType.FERTILIZER) {
            if (currentData.getCropData() != null && !currentData.isFertilized()) {
                int currentStage = currentData.getCropData().getGrowthStage();
                int maxStage = currentData.getCropData().getType().getMaxStages();
                // Chỉ cho phép bón khi cây chưa lớn hẳn và đạt giai đoạn tối thiểu
                if (currentStage >= CropConfig.MIN_GROWTH_STAGE_FOR_FERTILIZER && currentStage < maxStage - 1) {
                    TileData newData = new TileData(currentData);
                    newData.setFertilized(true);
                    newData.setFertilizerStartTime(System.nanoTime());
                    return new InteractionResult(newData, PlayerView.PlayerState.FERTILIZE, GameLogicConfig.FERTILIZER_DURATION_MS, true, null, 0);
                } else {
                    return null;
                }
            }
        }

        // XẺNG (SHOVEL): Dọn dẹp
        if (itemType == ItemType.SHOVEL) {
            // Dọn cây trồng
            if (currentData.getCropData() != null) {
                TileData newData = createResetTileData(currentData);
                long duration = (long) GameLogicConfig.SHOVEL_REPETITIONS * GameLogicConfig.SHOVEL_DURATION_PER_REPETITION_MS;
                return new InteractionResult(newData, PlayerView.PlayerState.SHOVEL, duration, true, null, 0);
            }
            // Dọn hạt giống cây hoặc gốc cây đã chặt
            if (currentData.getTreeData() != null) {
                TreeData tree = currentData.getTreeData();
                if (tree.getGrowthStage() == TreeConfig.TREE_SEED_STAGE || tree.getChopCount() > 0) {
                    TileData newData = new TileData(currentData);
                    newData.setBaseTileType(Tile.GRASS);
                    newData.setTreeData(null);
                    long duration = (long) GameLogicConfig.SHOVEL_REPETITIONS * GameLogicConfig.SHOVEL_DURATION_PER_REPETITION_MS;
                    return new InteractionResult(newData, PlayerView.PlayerState.SHOVEL, duration, true, null, 0);
                }
            }
        }

        // RÌU (AXE): Chặt cây hoặc phá hàng rào
        if (itemType == ItemType.AXE) {
            if (currentData.getTreeData() != null && baseTile == Tile.TREE) {
                TreeData tree = currentData.getTreeData();
                TileData newData = new TileData(currentData);
                int woodAmount = 0;

                // Cho phép chặt cây khi đạt giai đoạn phát triển nhất định
                if (tree.getGrowthStage() >= TreeConfig.TREE_MIN_CHOP_STAGE && tree.getChopCount() == 0) {
                    woodAmount = (tree.getGrowthStage() == TreeConfig.TREE_MATURE_STAGE) ?
                            TreeConfig.WOOD_PER_STAGE_2 : TreeConfig.WOOD_PER_STAGE_1;

                    // Tạo bản sao dữ liệu cây mới để tránh lỗi cập nhật tức thời khi animation chưa xong
                    TreeData newTreeData = new TreeData(tree.getGrowthStage());
                    newTreeData.setChopCount(1); // Đánh dấu là đã chặt (gốc cây)
                    newTreeData.setRegrowStartTime(System.nanoTime());
                    newData.setTreeData(newTreeData);

                    long duration = (long) GameLogicConfig.AXE_REPETITIONS * GameLogicConfig.AXE_DURATION_PER_REPETITION_MS;
                    return new InteractionResult(newData, PlayerView.PlayerState.AXE, duration, true, ItemType.WOOD, woodAmount);
                }
                // Nếu đã là gốc cây, chặt tiếp sẽ xóa hoàn toàn
                else if (tree.getChopCount() > 0) {
                    newData.setBaseTileType(Tile.GRASS);
                    newData.setTreeData(null);
                    long duration = (long) GameLogicConfig.AXE_REPETITIONS * GameLogicConfig.AXE_DURATION_PER_REPETITION_MS;
                    return new InteractionResult(newData, PlayerView.PlayerState.AXE, duration, true, null, 0);
                }
            }
            // Phá hàng rào
            if (currentData.getFenceData() != null && baseTile == Tile.FENCE) {
                TileData newData = new TileData(currentData);
                newData.setBaseTileType(Tile.GRASS);
                newData.setFenceData(null);
                long duration = (long) GameLogicConfig.AXE_REPETITIONS * GameLogicConfig.AXE_DURATION_PER_REPETITION_MS;
                return new InteractionResult(newData, PlayerView.PlayerState.AXE, duration, true, null, 0);
            }
        }

        // GỖ (WOOD): Xây hàng rào
        if (itemType == ItemType.WOOD) {
            if (baseTile == Tile.GRASS &&
                    currentData.getCropData() == null &&
                    currentData.getTreeData() == null &&
                    currentData.getFenceData() == null) {

                if (isPlayerBlocking(col, row, mainPlayer, worldMap)) return null;
                if (isAnimalBlocking(col, row)) return null;

                TileData newData = new TileData(currentData);
                newData.setBaseTileType(Tile.FENCE);
                FenceData fence = new FenceData(false);
                newData.setFenceData(fence);
                return new InteractionResult(newData, PlayerView.PlayerState.BUSY, GameLogicConfig.PLANT_DURATION_MS, true, null, 0);
            }
        }

        return null;
    }

    /**
     * Kiểm tra xem cây trồng trên ô đất có thu hoạch được không.
     */
    private InteractionResult checkHarvest(TileData currentData, Player mainPlayer) {
        CropData crop = currentData.getCropData();
        Random random = new Random();
        if (crop != null && crop.getGrowthStage() >= crop.getType().getMaxStages() - 1) {
            int yield = random.nextInt(crop.getType().getMaxYield() - crop.getType().getMinYield() + 1) + crop.getType().getMinYield();

            // Chỉ kiểm tra xem có thể thêm vào kho không, chưa thêm ngay lập tức
            boolean success = canAddItem(mainPlayer, crop.getType().getHarvestItem(), yield);

            if (!success) {
                return null;
            }

            TileData newData = createResetTileData(currentData);
            return new InteractionResult(newData, PlayerView.PlayerState.BUSY, GameLogicConfig.GENERIC_ACTION_DURATION_MS, false, crop.getType().getHarvestItem(), yield);
        }
        return null;
    }

    // ==============================================================================================
    // LOGIC THỰC THI (PUBLIC API)
    // ==============================================================================================

    /**
     * Hàm chính xử lý tương tác của người chơi với ô đất.
     * Được gọi từ GameManager khi người chơi click chuột.
     */
    public String processInteraction(Player mainPlayer, PlayerView playerView, WorldMap worldMap, int col, int row) {
        ItemStack currentStack = mainPlayer.getCurrentItem();
        TileData currentData = worldMap.getTileData(col, row);
        InteractionResult result = null;

        // 1. Xử lý vật phẩm đặc biệt: Nước tăng lực (Energy Drink)
        if (currentStack != null && currentStack.getItemType() == ItemType.ENERGY_DRINK) {
            mainPlayer.recoverStamina(50.0);
            currentStack.remove(1);
            if (currentStack.getQuantity() <= 0) {
                mainPlayer.getHotbarItems()[mainPlayer.getSelectedHotbarSlot()] = null;
            }

            mainPlayer.setState(PlayerView.PlayerState.BUSY);
            playerView.setState(mainPlayer.getState(), mainPlayer.getDirection());

            TimedTileAction action = new TimedTileAction(
                    -1, -1,
                    null,
                    getDelayInFrames(GameLogicConfig.GENERIC_ACTION_DURATION_MS),
                    false,
                    mainPlayer.getSelectedHotbarSlot()
            );
            actionManager.addPendingAction(action);

            return null; // Thành công
        }

        // 2. Ưu tiên nhặt vật phẩm đang nằm trên đất
        if (currentData.getGroundItem() != null && currentData.getGroundItemAmount() > 0) {
            ItemType groundItemType = currentData.getGroundItem();
            int groundItemAmount = currentData.getGroundItemAmount();
            int groundItemDurability = currentData.getGroundItemDurability();

            // Chỉ kiểm tra khả năng chứa đồ trước, việc thêm đồ sẽ do ActionManager thực hiện sau
            boolean success = canAddItem(mainPlayer, groundItemType, groundItemAmount);

            if (success) {
                // Xóa vật phẩm khỏi ô đất
                TileData newData = new TileData(currentData);
                newData.setGroundItem(null);
                newData.setGroundItemAmount(0);
                newData.setGroundItemDurability(0);
                newData.setDefaultItemOffset();

                TimedTileAction action = new TimedTileAction(
                        col, row,
                        newData,
                        getDelayInFrames(GameLogicConfig.GENERIC_ACTION_DURATION_MS),
                        false,
                        mainPlayer.getSelectedHotbarSlot()
                );
                action.setHarvestedItem(groundItemType);
                action.setHarvestedAmount(groundItemAmount);
                action.setHarvestedDurability(groundItemDurability);

                mainPlayer.setState(PlayerView.PlayerState.BUSY);
                playerView.setState(mainPlayer.getState(), mainPlayer.getDirection());
                actionManager.addPendingAction(action);

                return null;
            } else {
                return HudConfig.TEXT_INVENTORY_FULL;
            }
        }

        // 3. Kiểm tra thu hoạch cây trồng
        CropData crop = currentData.getCropData();
        if (crop != null && crop.getGrowthStage() >= crop.getType().getMaxStages() - 1) {
            result = checkHarvest(currentData, mainPlayer);
            if (result == null) {
                return HudConfig.TEXT_INVENTORY_FULL;
            }
        }
        else {
            // 4. Nếu không phải thu hoạch, tính toán kết quả tương tác công cụ thông thường
            result = calculateInteractionResult(currentStack, currentData, mainPlayer, worldMap, col, row);
        }

        // Xử lý các trường hợp lỗi hoặc không thể tương tác
        if (result == null) {
            if (currentStack != null) {
                if (currentStack.getItemType() == ItemType.WATERING_CAN && currentStack.getCurrentDurability() <= 0) {
                    return HudConfig.TEXT_WATER_EMPTY;
                }
                else if (currentStack.getItemType() == ItemType.FERTILIZER && currentData.getCropData() != null) {
                    if (currentData.getCropData().getGrowthStage() < CropConfig.MIN_GROWTH_STAGE_FOR_FERTILIZER) {
                        return HudConfig.TEXT_PLANT_CAN_NOT_BE_FERTILIZED;
                    }
                }
                else if (currentStack.getItemType() == ItemType.WOOD && currentData.getBaseTileType() == Tile.GRASS) {
                    if (isPlayerBlocking(col, row, mainPlayer, worldMap)) {
                        return HudConfig.TEXT_PLAYER_BLOCKING;
                    }
                    if (isAnimalBlocking(col, row)) {
                        return HudConfig.CANT_PLACE_TEXT;
                    }
                }
            }
            return HudConfig.WRONG_TOOL_TEXT;
        }

        // 5. Nếu tương tác hợp lệ, tạo hành động và đưa vào hàng đợi
        if (result != null) {
            if (currentStack != null) {
                consumeStaminaForAction(currentStack.getItemType(), mainPlayer);
            }

            mainPlayer.setState(result.playerState());
            playerView.setState(mainPlayer.getState(), mainPlayer.getDirection());

            TimedTileAction action = new TimedTileAction(
                    col, row,
                    result.newTileData(),
                    getDelayInFrames(result.totalDurationMs()),
                    result.consumeItem(),
                    mainPlayer.getSelectedHotbarSlot()
            );

            if (result.harvestedItem() != null) {
                action.setHarvestedItem(result.harvestedItem());
                action.setHarvestedAmount(result.harvestedAmount());
            }

            actionManager.addPendingAction(action);
            return null;
        }

        return HudConfig.WRONG_TOOL_TEXT;
    }

    /**
     * Hàm chính xử lý tương tác với động vật.
     * Bao gồm: Nhặt trứng, đặt vật nuôi/trứng mới, cho ăn, thu hoạch sản phẩm, tấn công.
     */
    public String processAnimalInteraction(Player mainPlayer, PlayerView playerView, double worldX, double worldY) {
        if (animalManager == null) return null;

        // --- BƯỚC 1: Ưu tiên nhặt trứng ---
        // Cho phép nhặt kể cả khi tay không, trừ khi đang cầm các vật phẩm đặt (để tránh xung đột)
        Animal animalAtPosition = animalManager.getAnimalAt(worldX, worldY, WorldConfig.TILE_SIZE);
        if (animalAtPosition != null && animalAtPosition.getType() == AnimalType.EGG_ENTITY && !animalAtPosition.isDead()) {
            ItemStack currentStack = mainPlayer.getCurrentItem();
            ItemType itemType = currentStack != null ? currentStack.getItemType() : null;

            // Nếu đang cầm vật nuôi, trứng khác hoặc gỗ thì bỏ qua bước này để xử lý logic đặt ở dưới
            if (itemType != ItemType.ITEM_COW && itemType != ItemType.ITEM_CHICKEN &&
                    itemType != ItemType.ITEM_PIG && itemType != ItemType.ITEM_SHEEP &&
                    itemType != ItemType.EGG && itemType != ItemType.WOOD) {

                if (!canAddItem(mainPlayer, ItemType.EGG, 1)) {
                    return HudConfig.TEXT_INVENTORY_FULL;
                }

                int col = (int) Math.floor(animalAtPosition.getX() / WorldConfig.TILE_SIZE);
                int row = (int) Math.floor(animalAtPosition.getY() / WorldConfig.TILE_SIZE);

                // Lưu vị trí để xóa hình ảnh sau
                double animalWorldX = animalAtPosition.getX();
                double animalWorldY = animalAtPosition.getY();

                mainPlayer.setState(PlayerView.PlayerState.BUSY);
                playerView.setState(mainPlayer.getState(), mainPlayer.getDirection());

                TimedTileAction action = new TimedTileAction(
                        col, row,
                        null,
                        getDelayInFrames(GameLogicConfig.GENERIC_ACTION_DURATION_MS),
                        false,
                        mainPlayer.getSelectedHotbarSlot()
                );
                action.setHarvestedItem(ItemType.EGG);
                action.setHarvestedAmount(1);
                action.setAnimalWorldX(animalWorldX);
                action.setAnimalWorldY(animalWorldY);
                actionManager.addPendingAction(action);

                return null;
            }
        }

        // --- BƯỚC 2: Ưu tiên đặt vật nuôi/trứng từ kho ---
        ItemStack currentStack = mainPlayer.getCurrentItem();
        if (currentStack == null) {
            return null;
        }

        ItemType itemType = currentStack.getItemType();
        AnimalType animalTypeToPlace = null;
        if (itemType == ItemType.ITEM_COW) animalTypeToPlace = AnimalType.COW;
        else if (itemType == ItemType.ITEM_CHICKEN) animalTypeToPlace = AnimalType.CHICKEN;
        else if (itemType == ItemType.ITEM_PIG) animalTypeToPlace = AnimalType.PIG;
        else if (itemType == ItemType.ITEM_SHEEP) animalTypeToPlace = AnimalType.SHEEP;
        else if (itemType == ItemType.EGG) animalTypeToPlace = AnimalType.EGG_ENTITY;

        if (animalTypeToPlace != null) {
            if (collisionManager == null || worldMap == null) return null;

            int tileCol = (int) Math.floor(worldX / WorldConfig.TILE_SIZE);
            int tileRow = (int) Math.floor(worldY / WorldConfig.TILE_SIZE);
            TileData tileData = worldMap.getTileData(tileCol, tileRow);

            // Kiểm tra không được đặt lên cây
            if (tileData.getBaseTileType() == Tile.TREE && tileData.getTreeData() != null) {
                return HudConfig.CANT_PLACE_TEXT;
            }

            // Kiểm tra không được đặt lên rào chắn
            if (tileData.getBaseTileType() == Tile.FENCE &&
                    tileData.getFenceData() != null &&
                    tileData.getFenceData().isSolid()) {
                return HudConfig.CANT_PLACE_TEXT;
            }

            // Kiểm tra va chạm vật lý với môi trường xung quanh
            double checkWidth = animalTypeToPlace.getHitboxWidth();
            double checkHeight = animalTypeToPlace.getHitboxHeight();

            if (collisionManager.checkCollision(worldX, worldY, checkWidth, checkHeight)) {
                return HudConfig.CANT_PLACE_TEXT;
            }

            // Kiểm tra va chạm với các động vật khác (tránh đặt đè lên trứng)
            double newMinX = worldX - checkWidth / 2.0;
            double newMaxX = worldX + checkWidth / 2.0;
            double newMinY = worldY - checkHeight / 2.0;
            double newMaxY = worldY + checkHeight / 2.0;

            for (Animal existing : animalManager.getAnimals()) {
                if (existing.isDead()) continue;

                double exW = existing.getType().getHitboxWidth();
                double exH = existing.getType().getHitboxHeight();
                double exMinX = existing.getX() - exW / 2.0;
                double exMaxX = existing.getX() + exW / 2.0;
                double exMinY = existing.getY() - exH / 2.0;
                double exMaxY = existing.getY() + exH / 2.0;

                if (aabbIntersect(newMinX, newMaxX, newMinY, newMaxY, exMinX, exMaxX, exMinY, exMaxY)) {
                    // Không cho phép đặt đè lên trứng để tránh che khuất
                    if (existing.getType() == AnimalType.EGG_ENTITY) {
                        return HudConfig.CANT_PLACE_TEXT;
                    }
                }
            }

            // Vị trí hợp lệ -> Tạo động vật mới
            Animal newAnimal = new Animal(animalTypeToPlace, worldX, worldY);
            animalManager.addAnimal(newAnimal);

            currentStack.remove(1);
            if (currentStack.getQuantity() <= 0) {
                mainPlayer.getHotbarItems()[mainPlayer.getSelectedHotbarSlot()] = null;
            }

            mainPlayer.setState(PlayerView.PlayerState.BUSY);
            playerView.setState(mainPlayer.getState(), mainPlayer.getDirection());

            TimedTileAction action = new TimedTileAction(
                    -1, -1,
                    null,
                    getDelayInFrames(GameLogicConfig.PLANT_DURATION_MS),
                    false,
                    mainPlayer.getSelectedHotbarSlot()
            );
            actionManager.addPendingAction(action);

            return null;
        }

        // --- BƯỚC 3: Tương tác với động vật (Cho ăn, Thu hoạch, Tấn công) ---
        Animal animal = animalManager.getAnimalAt(worldX, worldY, WorldConfig.TILE_SIZE);
        if (animal == null || animal.isDead()) {
            return null;
        }

        // Chặn tương tác nếu động vật đang sinh sản
        if (animal.isBreeding()) {
            return null;
        }

        // 3.1 Cho ăn
        if (animal.getType().acceptsFood(itemType)) {
            animalManager.feedAnimal(animal);

            currentStack.remove(1);
            if (currentStack.getQuantity() <= 0) {
                mainPlayer.getHotbarItems()[mainPlayer.getSelectedHotbarSlot()] = null;
            }

            mainPlayer.setState(PlayerView.PlayerState.BUSY);
            playerView.setState(mainPlayer.getState(), mainPlayer.getDirection());

            TimedTileAction action = new TimedTileAction(
                    -1, -1,
                    null,
                    getDelayInFrames(GameLogicConfig.PLANT_DURATION_MS),
                    false,
                    mainPlayer.getSelectedHotbarSlot()
            );
            actionManager.addPendingAction(action);

            return null;
        }

        // 3.2 Thu hoạch sữa (Bò)
        if (itemType == ItemType.MILK_BUCKET && animal.getType() == AnimalType.COW && animal.isHasProduct()) {
            animalManager.harvestProduct(animal);

            currentStack.remove(1);
            if (currentStack.getQuantity() <= 0) {
                mainPlayer.getHotbarItems()[mainPlayer.getSelectedHotbarSlot()] = null;
            }

            if (!mainPlayer.addItem(ItemType.FULL_MILK_BUCKET, 1)) {
                mainPlayer.addItem(ItemType.MILK, 1);
            }

            mainPlayer.setState(PlayerView.PlayerState.BUSY);
            playerView.setState(mainPlayer.getState(), mainPlayer.getDirection());

            TimedTileAction action = new TimedTileAction(
                    -1, -1,
                    null,
                    getDelayInFrames(GameLogicConfig.PLANT_DURATION_MS),
                    false,
                    mainPlayer.getSelectedHotbarSlot()
            );
            actionManager.addPendingAction(action);

            return null;
        }

        // 3.3 Thu hoạch lông (Cừu)
        if (itemType == ItemType.SHEARS && animal.getType() == AnimalType.SHEEP && animal.isHasProduct()) {
            if (currentStack.getCurrentDurability() <= 0) {
                return HudConfig.TEXT_WATER_EMPTY;
            }

            animalManager.harvestProduct(animal);

            currentStack.decreaseDurability(1);
            if (currentStack.getCurrentDurability() <= 0) {
                mainPlayer.getHotbarItems()[mainPlayer.getSelectedHotbarSlot()] = null;
            }

            mainPlayer.addItem(ItemType.WOOL, 1);

            mainPlayer.setState(PlayerView.PlayerState.BUSY);
            playerView.setState(mainPlayer.getState(), mainPlayer.getDirection());

            TimedTileAction action = new TimedTileAction(
                    -1, -1,
                    null,
                    getDelayInFrames(GameLogicConfig.AXE_DURATION_PER_REPETITION_MS),
                    false,
                    mainPlayer.getSelectedHotbarSlot()
            );
            actionManager.addPendingAction(action);

            return null;
        }

        // 3.4 Nhặt trứng (Gà) - Logic phụ trợ nếu bước 1 bị bỏ qua
        if (animal.getType() == AnimalType.EGG_ENTITY) {
            // Đảm bảo không nhặt khi đang cầm công cụ xây dựng
            if (itemType == ItemType.ITEM_COW || itemType == ItemType.ITEM_CHICKEN ||
                    itemType == ItemType.ITEM_PIG || itemType == ItemType.ITEM_SHEEP ||
                    itemType == ItemType.EGG || itemType == ItemType.WOOD) {
                return null;
            }

            if (!canAddItem(mainPlayer, ItemType.EGG, 1)) {
                return HudConfig.TEXT_INVENTORY_FULL;
            }

            int col = (int) Math.floor(animal.getX() / WorldConfig.TILE_SIZE);
            int row = (int) Math.floor(animal.getY() / WorldConfig.TILE_SIZE);
            double animalWorldX = animal.getX();
            double animalWorldY = animal.getY();

            mainPlayer.setState(PlayerView.PlayerState.BUSY);
            playerView.setState(mainPlayer.getState(), mainPlayer.getDirection());

            TimedTileAction action = new TimedTileAction(
                    col, row,
                    null,
                    getDelayInFrames(GameLogicConfig.GENERIC_ACTION_DURATION_MS),
                    false,
                    mainPlayer.getSelectedHotbarSlot()
            );
            action.setHarvestedItem(ItemType.EGG);
            action.setHarvestedAmount(1);
            action.setAnimalWorldX(animalWorldX);
            action.setAnimalWorldY(animalWorldY);
            actionManager.addPendingAction(action);

            return null;
        }

        // 3.5 Tấn công (Giết thịt)
        if (itemType == ItemType.AXE || itemType == ItemType.SWORD ||
                (itemType.hasDurability() && itemType != ItemType.SHEARS && itemType != ItemType.MILK_BUCKET)) {

            double animalX = animal.getX();
            double animalY = animal.getY();

            int meatAmount = animalManager.killAnimal(animal);

            if (meatAmount > 0 && worldMap != null) {
                ItemType meatType = animal.getMeatType();
                if (meatType != null) {
                    // Tính toán vị trí tâm hình ảnh động vật để đặt thịt rơi ra đúng chỗ
                    double visualCenterY = animalY - (animal.getType().getSpriteSize() / 2.0);

                    int idealTileCol = (int) Math.floor(animalX / WorldConfig.TILE_SIZE);
                    int idealTileRow = (int) Math.floor(visualCenterY / WorldConfig.TILE_SIZE);

                    double targetItemX = animalX - (ItemSpriteConfig.ITEM_SPRITE_WIDTH / 2.0);
                    double targetItemY = visualCenterY - (ItemSpriteConfig.ITEM_SPRITE_HEIGHT / 2.0);

                    double originalOffsetX = targetItemX - (idealTileCol * WorldConfig.TILE_SIZE);
                    double originalOffsetY = targetItemY - (idealTileRow * WorldConfig.TILE_SIZE);

                    // Tìm ô trống xung quanh để đặt thịt rơi ra, tránh ghi đè vật phẩm cũ
                    int searchRadius = GameLogicConfig.ITEM_DROP_SEARCH_RADIUS;
                    int finalCol = -1;
                    int finalRow = -1;
                    boolean foundSpot = false;

                    TileData idealTile = worldMap.getTileData(idealTileCol, idealTileRow);
                    if (idealTile.getGroundItem() == null) {
                        finalCol = idealTileCol;
                        finalRow = idealTileRow;
                        foundSpot = true;
                    } else if (idealTile.getGroundItem() == meatType) {
                        finalCol = idealTileCol;
                        finalRow = idealTileRow;
                        foundSpot = true;
                    } else {
                        for (int r = idealTileRow - searchRadius; r <= idealTileRow + searchRadius; r++) {
                            for (int c = idealTileCol - searchRadius; c <= idealTileCol + searchRadius; c++) {
                                if (r == idealTileRow && c == idealTileCol) continue;
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

                    if (!foundSpot) {
                        finalCol = idealTileCol;
                        finalRow = idealTileRow;
                    }

                    TileData finalTile = worldMap.getTileData(finalCol, finalRow);

                    if (finalTile.getGroundItem() == meatType) {
                        // Cộng dồn nếu cùng loại
                        finalTile.setGroundItemAmount(finalTile.getGroundItemAmount() + meatAmount);
                    } else {
                        // Đặt mới
                        finalTile.setGroundItem(meatType);
                        finalTile.setGroundItemAmount(meatAmount);

                        if (finalCol == idealTileCol && finalRow == idealTileRow) {
                            finalTile.setGroundItemOffsetX(originalOffsetX);
                            finalTile.setGroundItemOffsetY(originalOffsetY);
                        } else {
                            // Nếu đặt lệch sang ô khác, thêm một chút ngẫu nhiên để trông tự nhiên hơn
                            finalTile.setDefaultItemOffset();
                            double scatter = GameLogicConfig.ITEM_DROP_SCATTER_RANGE;
                            double jitterX = (Math.random() - 0.5) * scatter;
                            double jitterY = (Math.random() - 0.5) * scatter;
                            finalTile.setGroundItemOffsetX(finalTile.getGroundItemOffsetX() + jitterX);
                            finalTile.setGroundItemOffsetY(finalTile.getGroundItemOffsetY() + jitterY);
                        }
                    }
                    worldMap.setTileData(finalCol, finalRow, finalTile);
                }
            }

            mainPlayer.setState(PlayerView.PlayerState.ATTACK);
            playerView.setState(mainPlayer.getState(), mainPlayer.getDirection());

            long attackDuration = PlayerSpriteConfig.ATTACK_SPEED * PlayerSpriteConfig.ATTACK_FRAMES;
            TimedTileAction action = new TimedTileAction(
                    -1, -1,
                    null,
                    getDelayInFrames(attackDuration),
                    false,
                    mainPlayer.getSelectedHotbarSlot()
            );
            actionManager.addPendingAction(action);

            return null;
        }

        return null;
    }
}