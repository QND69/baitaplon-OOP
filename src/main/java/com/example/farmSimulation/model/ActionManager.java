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
    private final List<TimedTileAction> pendingActions;  // Thêm danh sách hành động chờ
    private boolean mapNeedsUpdate = false;

    // Thêm tham chiếu đến Model và View của Player
    private final Player mainPlayer;
    private final PlayerView playerView;
    private FenceManager fenceManager; // Quản lý hàng rào (sẽ được set từ bên ngoài)
    private AnimalManager animalManager; // Quản lý động vật (sẽ được set từ bên ngoài)
    private QuestManager questManager; // Quản lý quest (sẽ được set từ bên ngoài)

    public ActionManager(Player mainPlayer, PlayerView playerView) {
        this.pendingActions = new ArrayList<>();
        this.mainPlayer = mainPlayer;
        this.playerView = playerView;
    }

    public void setFenceManager(FenceManager fenceManager) {
        this.fenceManager = fenceManager;
    }

    public void setAnimalManager(AnimalManager animalManager) {
        this.animalManager = animalManager;
    }

    public void setQuestManager(QuestManager questManager) {
        this.questManager = questManager;
    }

    public void addPendingAction(TimedTileAction action) {
        pendingActions.add(action);
    }

    /**
     * Hàm này được gọi 60 LẦN/GIÂY.
     * Nhiệm vụ: Lặp qua tất cả hành động chờ, "tick" chúng,
     * và thực thi những hành động đã hết giờ.
     */
    public void updateTimedActions(WorldMap worldMap, MainGameView mainGameView, double worldOffsetX, double worldOffsetY) {
        // Dùng Iterator để chúng ta có thể XÓA phần tử khỏi List pendingActions một cách an toàn
        Iterator<TimedTileAction> iterator = pendingActions.iterator();

        while (iterator.hasNext()) {
            TimedTileAction action = iterator.next();

            // Gọi tick(). Nếu nó trả về "true" (hết giờ)
            if (action.tick()) { // Cho phép action không đổi tile
                // THỰC THI HÀNH ĐỘNG: Thay đổi Model
                if (action.getNewTileData() != null) {
                    // [SỬA] Lưu lại trạng thái cũ để kiểm tra xem có phải vừa phá rào không
                    TileData oldData = worldMap.getTileData(action.getCol(), action.getRow());
                    boolean wasFence = (oldData.getBaseTileType() == Tile.FENCE);

                    TileData newData = action.getNewTileData();
                    worldMap.setTileData(action.getCol(), action.getRow(), newData);
                    this.mapNeedsUpdate = true; // Báo cho View biết cần vẽ lại bản đồ

                    // Nếu vừa ĐẶT hàng rào (GRASS -> FENCE)
                    if (newData.getFenceData() != null && newData.getBaseTileType() == Tile.FENCE && fenceManager != null) {
                        fenceManager.updateFencePattern(action.getCol(), action.getRow());
                    }
                    // [SỬA] Nếu vừa PHÁ hàng rào (FENCE -> GRASS)
                    // Cần gọi update để các ô hàng xóm biết mà ngắt kết nối
                    else if (wasFence && newData.getBaseTileType() != Tile.FENCE && fenceManager != null) {
                        fenceManager.updateFencePattern(action.getCol(), action.getRow());
                    }
                }

                // XỬ LÝ TIÊU THỤ ITEM / ĐỘ BỀN
                // Logic này giờ chạy ĐỒNG BỘ với việc thay đổi Map
                if (action.isConsumeItem()) {
                    // Gọi hàm tiêu thụ item ở slot đã lưu
                    // (Hàm này sẽ xử lý cả việc trừ số lượng stackable hoặc trừ độ bền item)
                    mainPlayer.consumeItemAtSlot(action.getItemSlotIndex(), 1);

                    // Cập nhật UI Hotbar ngay lập tức
                    mainGameView.updateHotbar();
                }

                // XỬ LÝ ĐỘNG VẬT: Xóa động vật sau khi action hoàn thành (cho nhặt trứng)
                if (action.getAnimalWorldX() != 0 || action.getAnimalWorldY() != 0) {
                    if (animalManager != null) {
                        Animal animalToRemove = animalManager.getAnimalAt(action.getAnimalWorldX(), action.getAnimalWorldY(), WorldConfig.TILE_SIZE);
                        if (animalToRemove != null) {
                            animalManager.removeAnimal(animalToRemove);
                        }
                    }
                }

                // KÍCH HOẠT ANIMATION THU HOẠCH VÀ THÊM ITEM VÀO INVENTORY
                if (action.getHarvestedItem() != null && action.getHarvestedAmount() > 0) {
                    // Xác định độ bền để sử dụng
                    ItemType harvestedItem = action.getHarvestedItem();
                    int durabilityToUse = action.getHarvestedDurability();
                    int totalAmount = action.getHarvestedAmount();

                    // Nếu độ bền <= 0, kiểm tra xem có cần dùng max durability không
                    if (durabilityToUse <= 0 && harvestedItem.hasDurability()) {
                        // Item có độ bền nhưng không được lưu (initial spawn) -> dùng max durability
                        durabilityToUse = harvestedItem.getMaxDurability();
                    }

                    // Xử lý đặc biệt cho WOOD: Nếu inventory đầy, đặt xuống đất (giống như thịt)
                    if (harvestedItem == ItemType.WOOD) {
                        // Tính số lượng có thể thêm vào inventory
                        int addableAmount = mainPlayer.calculateAddableAmount(harvestedItem, totalAmount);
                        int remainingAmount = totalAmount - addableAmount;

                        // Thêm phần có thể vào inventory
                        if (addableAmount > 0) {
                            mainPlayer.addItem(harvestedItem, addableAmount, durabilityToUse);
                        }

                        // Nếu còn lại, đặt xuống đất (giống logic thịt)
                        if (remainingAmount > 0) {
                            // Tính vị trí đặt gỗ (tại ô cây bị chặt)
                            int treeCol = action.getCol();
                            int treeRow = action.getRow();

                            // Tìm ô trống xung quanh để đặt gỗ
                            int searchRadius = GameLogicConfig.ITEM_DROP_SEARCH_RADIUS;
                            int finalCol = -1;
                            int finalRow = -1;
                            boolean foundSpot = false;

                            // Kiểm tra ô lý tưởng trước (ô cây bị chặt)
                            TileData idealTile = worldMap.getTileData(treeCol, treeRow);
                            if (idealTile.getGroundItem() == null) {
                                finalCol = treeCol;
                                finalRow = treeRow;
                                foundSpot = true;
                            } else if (idealTile.getGroundItem() == ItemType.WOOD) {
                                // Trùng loại -> Cộng dồn
                                finalCol = treeCol;
                                finalRow = treeRow;
                                foundSpot = true;
                            } else {
                                // Ô lý tưởng đã có item khác -> Tìm xung quanh
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

                            // Nếu vẫn không tìm thấy chỗ -> Bắt buộc phải đè lên ô lý tưởng (Fallback)
                            if (!foundSpot) {
                                finalCol = treeCol;
                                finalRow = treeRow;
                            }

                            // Đặt gỗ vào ô đã chọn
                            TileData finalTile = worldMap.getTileData(finalCol, finalRow);

                            if (finalTile.getGroundItem() == ItemType.WOOD) {
                                // Cộng dồn
                                finalTile.setGroundItemAmount(finalTile.getGroundItemAmount() + remainingAmount);
                            } else {
                                // Đặt mới hoặc đè
                                finalTile.setGroundItem(ItemType.WOOD);
                                finalTile.setGroundItemAmount(remainingAmount);
                                finalTile.setGroundItemDurability(0); // WOOD không có độ bền

                                // Đặt offset để gỗ nằm sát mép dưới của tile
                                // offsetX: căn giữa theo chiều ngang
                                finalTile.setGroundItemOffsetX((WorldConfig.TILE_SIZE - ItemSpriteConfig.ITEM_SPRITE_WIDTH) / 2.0);
                                // offsetY: sát mép dưới của tile
                                finalTile.setGroundItemOffsetY(WorldConfig.TILE_SIZE - ItemSpriteConfig.ITEM_SPRITE_HEIGHT);

                                // Nếu phải đặt sang ô bên cạnh, thêm một chút scatter ngẫu nhiên nhỏ
                                if (finalCol != treeCol || finalRow != treeRow) {
                                    double scatter = GameLogicConfig.ITEM_DROP_SCATTER_RANGE;
                                    double jitterX = (Math.random() - 0.5) * scatter;
                                    // Chỉ scatter theo chiều ngang, giữ nguyên vị trí dọc (sát mép dưới)
                                    finalTile.setGroundItemOffsetX(finalTile.getGroundItemOffsetX() + jitterX);
                                }
                            }

                            worldMap.setTileData(finalCol, finalRow, finalTile);
                            this.mapNeedsUpdate = true; // Báo map cần vẽ lại
                        }

                        // Animation và UI update
                        if (addableAmount > 0) {
                            mainGameView.playHarvestAnimation(harvestedItem, action.getCol(), action.getRow(), worldOffsetX, worldOffsetY);
                        }
                        mainGameView.updateHotbar();

                        // Quest tracking: Chop trees (WOOD)
                        if (questManager != null) {
                            questManager.onEvent(QuestType.CHOP_TREE, ItemType.WOOD, 1); // Mỗi lần chặt cây = 1 tree
                        }
                    } else {
                        // Xử lý các item khác (crops) - logic cũ
                        // Thêm item vào inventory với số lượng và độ bền đúng
                        mainPlayer.addItem(harvestedItem, totalAmount, durabilityToUse);

                        // Truyền offset để View tính toán đúng vị trí trên màn hình
                        mainGameView.playHarvestAnimation(action.getHarvestedItem(), action.getCol(), action.getRow(), worldOffsetX, worldOffsetY);
                        mainGameView.updateHotbar(); // Update lại số lượng

                        // [SỬA LẠI XP LOGIC] Chỉ cộng XP nếu là Crop hoặc Wood
                        // Kiểm tra xem có phải là crop item không
                        boolean isCropItem = false;
                        for (CropType cropType : CropType.values()) {
                            if (cropType.getHarvestItem() == harvestedItem) {
                                isCropItem = true;
                                break;
                            }
                        }

                        // Chỉ cộng XP nếu là Crop hoặc Gỗ (chặt cây)
                        if (isCropItem || harvestedItem == ItemType.WOOD) {
                            mainPlayer.gainXP(GameLogicConfig.XP_GAIN_HARVEST);
                        }

                        // Quest tracking: Harvest crops
                        if (questManager != null) {
                            if (isCropItem) {
                                questManager.onEvent(QuestType.HARVEST, harvestedItem, totalAmount);
                            }
                        }
                    }
                }

                // [ĐÃ SỬA LOGIC LẶP] Reset trạng thái Player về IDLE sau khi hành động xong
                // Logic cũ: Chỉ check HOE, WATER... -> Thiếu các state mới (PLANT, SHOVEL, FERTILIZE) nên bị lặp vô tận
                // Logic mới: Check nếu KHÔNG PHẢI các state cơ bản (IDLE, WALK, DEAD) thì reset hết.
                PlayerView.PlayerState currentState = mainPlayer.getState();
                if (currentState != PlayerView.PlayerState.IDLE &&
                        currentState != PlayerView.PlayerState.WALK &&
                        currentState != PlayerView.PlayerState.DEAD) {

                    // Grant XP based on action type before resetting state
                    if (currentState == PlayerView.PlayerState.PLANT) {
                        mainPlayer.gainXP(com.example.farmSimulation.config.GameLogicConfig.XP_GAIN_PLANT);
                    } else if (currentState == PlayerView.PlayerState.WATER) {
                        mainPlayer.gainXP(com.example.farmSimulation.config.GameLogicConfig.XP_GAIN_WATER);
                    } else if (currentState == PlayerView.PlayerState.HOE) {
                        mainPlayer.gainXP(com.example.farmSimulation.config.GameLogicConfig.XP_GAIN_HOE);
                    } else if (currentState == PlayerView.PlayerState.AXE) {
                        mainPlayer.gainXP(com.example.farmSimulation.config.GameLogicConfig.XP_GAIN_AXE);
                    } else if (currentState == PlayerView.PlayerState.SHOVEL) {
                        mainPlayer.gainXP(com.example.farmSimulation.config.GameLogicConfig.XP_GAIN_SHOVEL);
                    }

                    mainPlayer.setState(PlayerView.PlayerState.IDLE);
                    playerView.setState(mainPlayer.getState(), mainPlayer.getDirection());
                }

                // Xóa hành động này khỏi hàng đợi
                iterator.remove();
            }
        }

        // Update map nếu cần
        if (this.mapNeedsUpdate) {
            mainGameView.updateMap(worldOffsetX, worldOffsetY, true);
            this.mapNeedsUpdate = false;
        }
    }
}