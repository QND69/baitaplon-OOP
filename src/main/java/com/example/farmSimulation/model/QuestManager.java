package com.example.farmSimulation.model;

import com.example.farmSimulation.config.QuestConfig;
import com.example.farmSimulation.model.GameSaveState.SavedQuest;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Quản lý hệ thống quest (nhiệm vụ hàng ngày)
 */
public class QuestManager {
    @Getter
    private final List<Quest> activeQuests;
    private final Random random;

    public QuestManager() {
        this.activeQuests = new ArrayList<>();
        this.random = new Random();
    }

    /**
     * Tạo quest hàng ngày (3 quests ngẫu nhiên)
     */
    public void generateDailyQuests() {
        activeQuests.clear();

        // Tạo danh sách các loại quest có thể
        List<QuestType> questTypes = new ArrayList<>();
        questTypes.add(QuestType.HARVEST);
        questTypes.add(QuestType.CHOP_TREE);
        questTypes.add(QuestType.SELL);

        // Xáo trộn để có thứ tự ngẫu nhiên
        java.util.Collections.shuffle(questTypes, random);

        // Tạo 3 quests
        for (int i = 0; i < QuestConfig.MAX_DAILY_QUESTS && i < questTypes.size(); i++) {
            QuestType type = questTypes.get(i);
            Quest quest = createRandomQuest(type);
            if (quest != null) {
                activeQuests.add(quest);
            }
        }
    }

    /**
     * Khôi phục quests từ file save
     */
    public void restoreQuests(List<SavedQuest> savedQuests) {
        activeQuests.clear();
        for (SavedQuest sq : savedQuests) {
            // Tạo object quest mới với thông tin đã lưu
            Quest q = new Quest(sq.description, sq.type, sq.targetItem, sq.targetAmount, sq.rewardMoney, sq.rewardXp);
            // Set lại tiến độ
            q.setCurrentAmount(sq.currentAmount);
            q.setClaimed(sq.isClaimed);

            activeQuests.add(q);
        }
    }

    /**
     * Tạo quest ngẫu nhiên dựa trên loại
     * @param type Loại quest
     * @return Quest được tạo
     */
    private Quest createRandomQuest(QuestType type) {
        switch (type) {
            case HARVEST:
                return createHarvestQuest();
            case CHOP_TREE:
                return createActionQuest();
            case SELL:
                return createSellQuest();
            default:
                return null;
        }
    }

    /**
     * Tạo quest thu hoạch
     */
    private Quest createHarvestQuest() {
        // Lấy danh sách các crop items (các item có thể thu hoạch từ cây trồng)
        List<ItemType> harvestableItems = new ArrayList<>();
        for (CropType cropType : CropType.values()) {
            ItemType harvestItem = cropType.getHarvestItem();
            if (harvestItem != null && !harvestableItems.contains(harvestItem)) {
                harvestableItems.add(harvestItem);
            }
        }

        if (harvestableItems.isEmpty()) {
            return null; // Không có crop nào
        }

        // Chọn ngẫu nhiên một crop
        ItemType targetItem = harvestableItems.get(random.nextInt(harvestableItems.size()));

        // Số lượng mục tiêu ngẫu nhiên (3-10)
        int targetAmount = 3 + random.nextInt(8); // 3-10

        // Tính phần thưởng dựa trên số lượng
        double rewardMoney = QuestConfig.BASE_REWARD_MONEY * targetAmount / 5.0;
        double rewardXp = QuestConfig.BASE_REWARD_XP * targetAmount / 5.0;

        String description = "Harvest " + targetAmount + " " + targetItem.getName();

        return new Quest(description, QuestType.HARVEST, targetItem, targetAmount, rewardMoney, rewardXp);
    }

    /**
     * Tạo quest hành động (hiện tại chỉ có chặt cây)
     */
    private Quest createActionQuest() {
        // Hiện tại chỉ có quest chặt cây
        ItemType targetItem = ItemType.WOOD; // Dùng WOOD như indicator cho "chặt cây"

        // Số lượng mục tiêu ngẫu nhiên (3-8)
        int targetAmount = 3 + random.nextInt(6); // 3-8

        // Tính phần thưởng
        double rewardMoney = QuestConfig.BASE_REWARD_MONEY * targetAmount / 5.0;
        double rewardXp = QuestConfig.BASE_REWARD_XP * targetAmount / 5.0;

        String description = "Chop " + targetAmount + " Trees";

        return new Quest(description, QuestType.CHOP_TREE, targetItem, targetAmount, rewardMoney, rewardXp);
    }

    /**
     * Tạo quest bán hàng
     */
    private Quest createSellQuest() {
        // Lấy danh sách các item có thể bán (sellPrice > 0)
        List<ItemType> sellableItems = new ArrayList<>();
        for (ItemType itemType : ItemType.values()) {
            if (itemType.getSellPrice() > 0) {
                sellableItems.add(itemType);
            }
        }

        if (sellableItems.isEmpty()) {
            return null; // Không có item nào có thể bán
        }

        // Chọn ngẫu nhiên một item
        ItemType targetItem = sellableItems.get(random.nextInt(sellableItems.size()));

        // Số lượng mục tiêu ngẫu nhiên (5-15)
        int targetAmount = 5 + random.nextInt(11); // 5-15

        // Tính phần thưởng dựa trên số lượng
        double rewardMoney = QuestConfig.BASE_REWARD_MONEY * targetAmount / 10.0;
        double rewardXp = QuestConfig.BASE_REWARD_XP * targetAmount / 10.0;

        String description = "Sell " + targetAmount + " " + targetItem.getName();

        return new Quest(description, QuestType.SELL, targetItem, targetAmount, rewardMoney, rewardXp);
    }

    /**
     * Xử lý event từ các manager khác (harvest, action, sell)
     * @param type Loại quest event
     * @param item Item liên quan (có thể null)
     * @param amount Số lượng
     */
    public void onEvent(QuestType type, ItemType item, int amount) {
        for (Quest quest : activeQuests) {
            // Kiểm tra xem quest có cùng loại và item mục tiêu không
            if (quest.getType() == type) {
                if (type == QuestType.CHOP_TREE && item == ItemType.WOOD) {
                    // Quest ACTION với WOOD = chặt cây
                    if (quest.getTargetItem() == ItemType.WOOD) {
                        quest.incrementProgress(amount);
                    }
                } else if (quest.getTargetItem() == item) {
                    // Quest có item mục tiêu khớp
                    quest.incrementProgress(amount);
                }
            }
        }
    }

    /**
     * Nhận thưởng quest
     * @param quest Quest cần nhận thưởng
     * @param player Player nhận thưởng
     * @return true nếu nhận thành công, false nếu đã nhận rồi hoặc chưa hoàn thành
     */
    public boolean claimReward(Quest quest, Player player) {
        if (quest.isClaimed()) {
            return false; // Đã nhận rồi
        }

        if (!quest.isCompleted()) {
            return false; // Chưa hoàn thành
        }

        // Thêm tiền và XP
        player.addMoney(quest.getRewardMoney());
        player.gainXP(quest.getRewardXp());

        // Đánh dấu đã nhận
        quest.setClaimed(true);

        return true;
    }
}