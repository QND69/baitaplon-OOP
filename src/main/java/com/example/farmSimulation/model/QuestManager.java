package com.example.farmSimulation.model;

import com.example.farmSimulation.config.QuestConfig;
import com.example.farmSimulation.model.GameSaveState.SavedQuest;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lớp quản lý hệ thống nhiệm vụ hàng ngày
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
     * Tạo danh sách nhiệm vụ hàng ngày.
     * Thường tạo ra 3 nhiệm vụ ngẫu nhiên.
     */
    public void generateDailyQuests() {
        activeQuests.clear();

        // Khởi tạo danh sách các loại nhiệm vụ khả thi
        List<QuestType> questTypes = new ArrayList<>();
        questTypes.add(QuestType.HARVEST);
        questTypes.add(QuestType.CHOP_TREE);
        questTypes.add(QuestType.SELL);

        // Xáo trộn danh sách để đảm bảo tính ngẫu nhiên của các loại nhiệm vụ
        java.util.Collections.shuffle(questTypes, random);

        // Tạo tối đa 3 nhiệm vụ từ danh sách đã xáo trộn
        for (int i = 0; i < QuestConfig.MAX_DAILY_QUESTS && i < questTypes.size(); i++) {
            QuestType type = questTypes.get(i);
            Quest quest = createRandomQuest(type);
            if (quest != null) {
                activeQuests.add(quest);
            }
        }
    }

    /**
     * Khôi phục danh sách nhiệm vụ từ dữ liệu đã lưu trong file
     */
    public void restoreQuests(List<SavedQuest> savedQuests) {
        activeQuests.clear();
        for (SavedQuest sq : savedQuests) {
            // Tạo đối tượng nhiệm vụ mới dựa trên thông tin đã lưu trữ
            Quest q = new Quest(sq.description, sq.type, sq.targetItem, sq.targetAmount, sq.rewardMoney, sq.rewardXp);
            // Cập nhật lại tiến độ và trạng thái nhận thưởng
            q.setCurrentAmount(sq.currentAmount);
            q.setClaimed(sq.isClaimed);

            activeQuests.add(q);
        }
    }

    /**
     * Tạo một nhiệm vụ ngẫu nhiên dựa trên loại được chỉ định
     * @param type Loại nhiệm vụ cần tạo
     * @return Đối tượng nhiệm vụ được tạo ra
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
     * Tạo nhiệm vụ thu hoạch nông sản
     */
    private Quest createHarvestQuest() {
        // Lấy danh sách các loại vật phẩm có thể thu hoạch từ cây trồng
        List<ItemType> harvestableItems = new ArrayList<>();
        for (CropType cropType : CropType.values()) {
            ItemType harvestItem = cropType.getHarvestItem();
            if (harvestItem != null && !harvestableItems.contains(harvestItem)) {
                harvestableItems.add(harvestItem);
            }
        }

        if (harvestableItems.isEmpty()) {
            return null; // Trả về null nếu không tìm thấy loại nông sản nào
        }

        // Chọn ngẫu nhiên một loại nông sản làm mục tiêu
        ItemType targetItem = harvestableItems.get(random.nextInt(harvestableItems.size()));

        // Xác định số lượng mục tiêu ngẫu nhiên trong khoảng từ 3 đến 10
        int targetAmount = 3 + random.nextInt(8);

        // Tính toán phần thưởng tiền và kinh nghiệm dựa trên số lượng mục tiêu
        double rewardMoney = QuestConfig.BASE_REWARD_MONEY * targetAmount / 5.0;
        double rewardXp = QuestConfig.BASE_REWARD_XP * targetAmount / 5.0;

        String description = "Harvest " + targetAmount + " " + targetItem.getName();

        return new Quest(description, QuestType.HARVEST, targetItem, targetAmount, rewardMoney, rewardXp);
    }

    /**
     * Tạo nhiệm vụ hành động.
     * Hiện tại chỉ hỗ trợ hành động chặt cây.
     */
    private Quest createActionQuest() {
        // Sử dụng gỗ làm vật phẩm đại diện cho hành động chặt cây
        ItemType targetItem = ItemType.WOOD;

        // Xác định số lượng mục tiêu ngẫu nhiên trong khoảng từ 3 đến 8
        int targetAmount = 3 + random.nextInt(6);

        // Tính toán phần thưởng
        double rewardMoney = QuestConfig.BASE_REWARD_MONEY * targetAmount / 5.0;
        double rewardXp = QuestConfig.BASE_REWARD_XP * targetAmount / 5.0;

        String description = "Chop " + targetAmount + " Trees";

        return new Quest(description, QuestType.CHOP_TREE, targetItem, targetAmount, rewardMoney, rewardXp);
    }

    /**
     * Tạo nhiệm vụ bán hàng
     */
    private Quest createSellQuest() {
        // Lấy danh sách các vật phẩm có thể bán được, tức là có giá bán lớn hơn 0
        List<ItemType> sellableItems = new ArrayList<>();
        for (ItemType itemType : ItemType.values()) {
            if (itemType.getSellPrice() > 0) {
                sellableItems.add(itemType);
            }
        }

        if (sellableItems.isEmpty()) {
            return null; // Trả về null nếu không có vật phẩm nào bán được
        }

        // Chọn ngẫu nhiên một vật phẩm làm mục tiêu
        ItemType targetItem = sellableItems.get(random.nextInt(sellableItems.size()));

        // Xác định số lượng mục tiêu ngẫu nhiên trong khoảng từ 5 đến 15
        int targetAmount = 5 + random.nextInt(11);

        // Tính toán phần thưởng dựa trên số lượng mục tiêu
        double rewardMoney = QuestConfig.BASE_REWARD_MONEY * targetAmount / 10.0;
        double rewardXp = QuestConfig.BASE_REWARD_XP * targetAmount / 10.0;

        String description = "Sell " + targetAmount + " " + targetItem.getName();

        return new Quest(description, QuestType.SELL, targetItem, targetAmount, rewardMoney, rewardXp);
    }

    /**
     * Xử lý sự kiện nhận được từ các trình quản lý khác như thu hoạch, hành động hoặc bán hàng
     * @param type Loại sự kiện nhiệm vụ
     * @param item Vật phẩm liên quan
     * @param amount Số lượng thực hiện
     */
    public void onEvent(QuestType type, ItemType item, int amount) {
        for (Quest quest : activeQuests) {
            // Duyệt qua danh sách nhiệm vụ và kiểm tra xem loại nhiệm vụ có khớp không
            if (quest.getType() == type) {
                if (type == QuestType.CHOP_TREE && item == ItemType.WOOD) {
                    // Đối với nhiệm vụ hành động chặt cây, nếu vật phẩm là gỗ thì cập nhật tiến độ
                    if (quest.getTargetItem() == ItemType.WOOD) {
                        quest.incrementProgress(amount);
                    }
                } else if (quest.getTargetItem() == item) {
                    // Nếu vật phẩm mục tiêu khớp với sự kiện thì cập nhật tiến độ
                    quest.incrementProgress(amount);
                }
            }
        }
    }

    /**
     * Xử lý việc nhận phần thưởng khi hoàn thành nhiệm vụ
     * @param quest Nhiệm vụ cần nhận thưởng
     * @param player Người chơi nhận thưởng
     * @return true nếu nhận thành công, false nếu đã nhận trước đó hoặc nhiệm vụ chưa hoàn thành
     */
    public boolean claimReward(Quest quest, Player player) {
        if (quest.isClaimed()) {
            return false; // Đã nhận thưởng rồi
        }

        if (!quest.isCompleted()) {
            return false; // Chưa hoàn thành nhiệm vụ
        }

        // Cộng tiền và kinh nghiệm cho người chơi
        player.addMoney(quest.getRewardMoney());
        player.gainXP(quest.getRewardXp());

        // Đánh dấu nhiệm vụ là đã nhận thưởng
        quest.setClaimed(true);

        return true;
    }
}