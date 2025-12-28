package com.example.farmSimulation.model;

import com.example.farmSimulation.config.GameLogicConfig;
import com.example.farmSimulation.config.ItemSpriteConfig;
import com.example.farmSimulation.config.WorldConfig;
import com.example.farmSimulation.view.MainGameView;
import com.example.farmSimulation.view.PlayerView;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Getter
@Setter
public class ActionManager {

    private final List<TimedTileAction> pendingActions;  // Danh sách các hành động đang chờ xử lý
    private boolean mapNeedsUpdate = false;

    // Tham chiếu đến Model và View của người chơi chính
    private final Player mainPlayer;
    private final PlayerView playerView;

    // Các trình quản lý phụ thuộc (sẽ được gán giá trị từ bên ngoài sau khi khởi tạo)
    private FenceManager fenceManager;   // Quản lý logic nối hàng rào
    private AnimalManager animalManager; // Quản lý danh sách động vật
    private QuestManager questManager;   // Quản lý nhiệm vụ

    public ActionManager(Player mainPlayer, PlayerView playerView) {
        this.pendingActions = new ArrayList<>();
        this.mainPlayer = mainPlayer;
        this.playerView = playerView;
    }

    public void addPendingAction(TimedTileAction action) {
        pendingActions.add(action);
    }

    /**
     * Hàm này được gọi liên tục trong vòng lặp game (khoảng 60 lần/giây).
     * Nhiệm vụ: Duyệt qua danh sách hành động chờ, cập nhật tiến độ (tick)
     * và thực thi logic khi hành động hoàn tất.
     */
    public void updateTimedActions(WorldMap worldMap, MainGameView mainGameView, double worldOffsetX, double worldOffsetY) {
        // Sử dụng Iterator để có thể xóa phần tử khỏi danh sách pendingActions một cách an toàn trong khi duyệt
        Iterator<TimedTileAction> iterator = pendingActions.iterator();

        while (iterator.hasNext()) {
            TimedTileAction action = iterator.next();

            // Gọi hàm tick() để tăng biến đếm thời gian. Nếu trả về true nghĩa là hành động đã hoàn tất.
            if (action.tick()) {

                // Bước 1: Cập nhật thay đổi trên bản đồ (đất, hàng rào...)
                processTileUpdates(action, worldMap);

                // Bước 2: Xử lý tiêu thụ vật phẩm hoặc giảm độ bền công cụ
                processItemConsumption(action, mainGameView);

                // Bước 3: Xử lý tương tác với động vật (ví dụ: nhặt trứng -> xóa trứng trên map)
                processAnimalInteraction(action);

                // Bước 4: Xử lý thu hoạch, rơi vật phẩm và cộng vật phẩm vào túi đồ
                processHarvestResult(action, worldMap, mainGameView, worldOffsetX, worldOffsetY);

                // Bước 5: Đặt lại trạng thái người chơi và cộng điểm kinh nghiệm (XP)
                resetPlayerStateAndGrantXP(action);

                // Xóa hành động đã hoàn thành khỏi hàng đợi
                iterator.remove();
            }
        }

        // Cập nhật lại hiển thị bản đồ nếu có thay đổi về ô đất
        if (this.mapNeedsUpdate) {
            mainGameView.updateMap(worldOffsetX, worldOffsetY, true);
            this.mapNeedsUpdate = false;
        }
    }

    /**
     * Xử lý việc thay đổi dữ liệu của ô đất (TileData) trên bản đồ.
     * Bao gồm cả logic cập nhật hình ảnh nối của hàng rào khi đặt hoặc phá.
     */
    private void processTileUpdates(TimedTileAction action, WorldMap worldMap) {
        if (action.getNewTileData() != null) {
            // Lưu lại trạng thái ô đất cũ để kiểm tra xem có phải người chơi vừa phá hàng rào hay không
            TileData oldData = worldMap.getTileData(action.getCol(), action.getRow());
            boolean wasFence = (oldData.getBaseTileType() == Tile.FENCE);

            TileData newData = action.getNewTileData();
            worldMap.setTileData(action.getCol(), action.getRow(), newData);
            this.mapNeedsUpdate = true; // Đánh dấu để View vẽ lại bản đồ

            // Trường hợp 1: Vừa đặt hàng rào mới (chuyển từ GRASS sang FENCE)
            if (newData.getFenceData() != null && newData.getBaseTileType() == Tile.FENCE && fenceManager != null) {
                fenceManager.updateFencePattern(action.getCol(), action.getRow());
            }
            // Trường hợp 2: Vừa phá bỏ hàng rào (chuyển từ FENCE sang loại khác)
            // Cần cập nhật các ô lân cận để chúng ngắt kết nối hình ảnh với ô vừa bị phá
            else if (wasFence && newData.getBaseTileType() != Tile.FENCE && fenceManager != null) {
                fenceManager.updateFencePattern(action.getCol(), action.getRow());
            }
        }
    }

    /**
     * Xử lý logic tiêu thụ vật phẩm (hạt giống, phân bón) hoặc trừ độ bền công cụ.
     */
    private void processItemConsumption(TimedTileAction action, MainGameView mainGameView) {
        // Logic này chạy đồng bộ ngay khi thay đổi Map diễn ra
        if (action.isConsumeItem()) {
            // Gọi hàm tiêu thụ item tại vị trí slot đã lưu
            // Hàm này tự động xử lý việc giảm số lượng (nếu xếp chồng) hoặc giảm độ bền
            mainPlayer.consumeItemAtSlot(action.getItemSlotIndex(), 1);

            // Cập nhật lại giao diện thanh công cụ (Hotbar) ngay lập tức
            mainGameView.updateHotbar();
        }
    }

    /**
     * Xử lý tương tác với động vật. Ví dụ: khi nhặt vật phẩm do động vật tạo ra,
     * cần xóa đối tượng động vật đó khỏi danh sách quản lý.
     */
    private void processAnimalInteraction(TimedTileAction action) {
        if (action.getAnimalWorldX() != 0 || action.getAnimalWorldY() != 0) {
            if (animalManager != null) {
                Animal animalToRemove = animalManager.getAnimalAt(action.getAnimalWorldX(), action.getAnimalWorldY(), WorldConfig.TILE_SIZE);
                if (animalToRemove != null) {
                    animalManager.removeAnimal(animalToRemove);
                }
            }
        }
    }

    /**
     * Xử lý kết quả thu hoạch: tính toán số lượng nhận được, xử lý rơi vật phẩm xuống đất (nếu đầy túi),
     * cập nhật nhiệm vụ và chạy hiệu ứng hình ảnh.
     */
    private void processHarvestResult(TimedTileAction action, WorldMap worldMap, MainGameView mainGameView, double worldOffsetX, double worldOffsetY) {
        if (action.getHarvestedItem() != null && action.getHarvestedAmount() > 0) {
            ItemType harvestedItem = action.getHarvestedItem();
            int durabilityToUse = action.getHarvestedDurability();
            int totalAmount = action.getHarvestedAmount();

            // Nếu vật phẩm có độ bền nhưng chưa được lưu thông tin (lần đầu xuất hiện), sử dụng độ bền tối đa mặc định
            if (durabilityToUse <= 0 && harvestedItem.hasDurability()) {
                durabilityToUse = harvestedItem.getMaxDurability();
            }

            // Xử lý riêng cho GỖ (WOOD): Nếu túi đầy thì đặt gỗ rơi xuống đất
            if (harvestedItem == ItemType.WOOD) {
                handleWoodHarvest(action, worldMap, mainGameView, worldOffsetX, worldOffsetY, harvestedItem, totalAmount, durabilityToUse);
            }
            // Xử lý cho các loại nông sản khác (Crops): Logic thông thường
            else {
                handleCropHarvest(action, mainGameView, worldOffsetX, worldOffsetY, harvestedItem, totalAmount, durabilityToUse);
            }
        }
    }

    /**
     * Logic chi tiết cho việc thu hoạch Gỗ (chặt cây).
     * Bao gồm thuật toán tìm vị trí trống xung quanh để rơi gỗ nếu túi đồ đầy.
     */
    private void handleWoodHarvest(TimedTileAction action, WorldMap worldMap, MainGameView mainGameView, double worldOffsetX, double worldOffsetY, ItemType harvestedItem, int totalAmount, int durabilityToUse) {
        // Tính toán số lượng có thể thêm vào túi đồ
        int addableAmount = mainPlayer.calculateAddableAmount(harvestedItem, totalAmount);
        int remainingAmount = totalAmount - addableAmount;

        // Thêm phần có thể chứa vào túi đồ
        if (addableAmount > 0) {
            mainPlayer.addItem(harvestedItem, addableAmount, durabilityToUse);
        }

        // Nếu còn dư (do túi đầy), thực hiện logic rơi vật phẩm xuống đất
        if (remainingAmount > 0) {
            int treeCol = action.getCol();
            int treeRow = action.getRow();

            // Tìm vị trí trống xung quanh ô cây vừa chặt để đặt gỗ
            int searchRadius = GameLogicConfig.ITEM_DROP_SEARCH_RADIUS;
            int finalCol = -1;
            int finalRow = -1;
            boolean foundSpot = false;

            // Ưu tiên 1: Kiểm tra chính ô cây vừa chặt (ô lý tưởng)
            TileData idealTile = worldMap.getTileData(treeCol, treeRow);
            if (idealTile.getGroundItem() == null) {
                finalCol = treeCol;
                finalRow = treeRow;
                foundSpot = true;
            } else if (idealTile.getGroundItem() == ItemType.WOOD) {
                // Nếu đã có gỗ ở đó -> Cộng dồn số lượng
                finalCol = treeCol;
                finalRow = treeRow;
                foundSpot = true;
            } else {
                // Ưu tiên 2: Quét các ô xung quanh theo bán kính
                for (int r = treeRow - searchRadius; r <= treeRow + searchRadius; r++) {
                    for (int c = treeCol - searchRadius; c <= treeCol + searchRadius; c++) {
                        if (r == treeRow && c == treeCol) continue;

                        TileData checkTile = worldMap.getTileData(c, r);
                        if (checkTile.getGroundItem() == null) {
                            finalCol = c;
                            finalRow = r;
                            foundSpot = true;
                            break;
                        } else if (checkTile.getGroundItem() == ItemType.WOOD) {
                            // Trùng loại -> Cộng dồn
                            finalCol = c;
                            finalRow = r;
                            foundSpot = true;
                            break;
                        }
                    }
                    if (foundSpot) break;
                }
            }

            // Phương án dự phòng: Nếu vẫn không tìm thấy vị trí thích hợp, bắt buộc phải đặt đè lên vị trí gốc
            if (!foundSpot) {
                finalCol = treeCol;
                finalRow = treeRow;
            }

            // Thực hiện đặt gỗ vào ô đã chọn trên bản đồ
            TileData finalTile = worldMap.getTileData(finalCol, finalRow);

            if (finalTile.getGroundItem() == ItemType.WOOD) {
                // Cộng dồn số lượng nếu đã có gỗ
                finalTile.setGroundItemAmount(finalTile.getGroundItemAmount() + remainingAmount);
            } else {
                // Đặt mới hoặc đè lên item cũ
                finalTile.setGroundItem(ItemType.WOOD);
                finalTile.setGroundItemAmount(remainingAmount);
                finalTile.setGroundItemDurability(0); // Gỗ nguyên liệu không có thanh độ bền

                // Căn chỉnh vị trí hiển thị của item nằm sát mép dưới ô đất
                finalTile.setGroundItemOffsetX((WorldConfig.TILE_SIZE - ItemSpriteConfig.ITEM_SPRITE_WIDTH) / 2.0); // Căn giữa ngang
                finalTile.setGroundItemOffsetY(WorldConfig.TILE_SIZE - ItemSpriteConfig.ITEM_SPRITE_HEIGHT);      // Sát mép dưới

                // Nếu item rơi sang ô bên cạnh (không phải ô gốc), thêm chút lệch ngẫu nhiên cho tự nhiên
                if (finalCol != treeCol || finalRow != treeRow) {
                    double scatter = GameLogicConfig.ITEM_DROP_SCATTER_RANGE;
                    double jitterX = (Math.random() - 0.5) * scatter;
                    // Chỉ lệch theo chiều ngang, giữ nguyên chiều dọc
                    finalTile.setGroundItemOffsetX(finalTile.getGroundItemOffsetX() + jitterX);
                }
            }

            worldMap.setTileData(finalCol, finalRow, finalTile);
            this.mapNeedsUpdate = true; // Báo hệ thống cần vẽ lại bản đồ
        }

        // Chạy hiệu ứng bay vật phẩm và cập nhật UI
        if (addableAmount > 0) {
            mainGameView.playHarvestAnimation(harvestedItem, action.getCol(), action.getRow(), worldOffsetX, worldOffsetY);
        }
        mainGameView.updateHotbar();

        // Cập nhật tiến độ nhiệm vụ: Chặt cây
        if (questManager != null) {
            questManager.onEvent(QuestType.CHOP_TREE, ItemType.WOOD, 1); // Tính là chặt 1 cây
        }
    }

    /**
     * Logic thu hoạch nông sản thông thường.
     */
    private void handleCropHarvest(TimedTileAction action, MainGameView mainGameView, double worldOffsetX, double worldOffsetY, ItemType harvestedItem, int totalAmount, int durabilityToUse) {
        // Thêm item vào túi đồ với số lượng và độ bền chính xác
        mainPlayer.addItem(harvestedItem, totalAmount, durabilityToUse);

        // Truyền tọa độ offset để View tính toán đúng vị trí hiển thị trên màn hình
        mainGameView.playHarvestAnimation(action.getHarvestedItem(), action.getCol(), action.getRow(), worldOffsetX, worldOffsetY);
        mainGameView.updateHotbar();

        // Kiểm tra xem vật phẩm thu hoạch có phải là nông sản (Crop) hay không để cộng điểm kinh nghiệm
        boolean isCropItem = false;
        for (CropType cropType : CropType.values()) {
            if (cropType.getHarvestItem() == harvestedItem) {
                isCropItem = true;
                break;
            }
        }

        // Chỉ cộng XP nếu là Nông sản hoặc Gỗ (đã xử lý ở trên, nhưng kiểm tra lại cho chắc chắn logic chung)
        if (isCropItem || harvestedItem == ItemType.WOOD) {
            mainPlayer.gainXP(GameLogicConfig.XP_GAIN_HARVEST);
        }

        // Cập nhật tiến độ nhiệm vụ: Thu hoạch nông sản
        if (questManager != null) {
            if (isCropItem) {
                questManager.onEvent(QuestType.HARVEST, harvestedItem, totalAmount);
            }
        }
    }

    /**
     * Đặt lại trạng thái của người chơi về đứng yên (IDLE) sau khi hành động kết thúc
     * để tránh lặp lại hành động vô tận. Đồng thời cộng điểm kinh nghiệm tương ứng với hành động.
     */
    private void resetPlayerStateAndGrantXP(TimedTileAction action) {
        PlayerView.PlayerState currentState = mainPlayer.getState();

        // Chỉ reset nếu trạng thái hiện tại KHÔNG phải là các trạng thái cơ bản (Đứng yên, Đi bộ, Chết)
        if (currentState != PlayerView.PlayerState.IDLE &&
                currentState != PlayerView.PlayerState.WALK &&
                currentState != PlayerView.PlayerState.DEAD) {

            // Cộng điểm kinh nghiệm dựa trên loại hành động trước khi reset trạng thái
            if (currentState == PlayerView.PlayerState.PLANT) {
                mainPlayer.gainXP(GameLogicConfig.XP_GAIN_PLANT);
            } else if (currentState == PlayerView.PlayerState.WATER) {
                mainPlayer.gainXP(GameLogicConfig.XP_GAIN_WATER);
            } else if (currentState == PlayerView.PlayerState.HOE) {
                mainPlayer.gainXP(GameLogicConfig.XP_GAIN_HOE);
            } else if (currentState == PlayerView.PlayerState.AXE) {
                mainPlayer.gainXP(GameLogicConfig.XP_GAIN_AXE);
            } else if (currentState == PlayerView.PlayerState.SHOVEL) {
                mainPlayer.gainXP(GameLogicConfig.XP_GAIN_SHOVEL);
            }

            // Chuyển về trạng thái đứng yên
            mainPlayer.setState(PlayerView.PlayerState.IDLE);
            playerView.setState(mainPlayer.getState(), mainPlayer.getDirection());
        }
    }
}