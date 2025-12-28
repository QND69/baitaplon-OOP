package com.example.farmSimulation.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Class đại diện cho một quest (nhiệm vụ)
 */
@Getter
@Setter
public class Quest {
    private String description; // Mô tả quest (e.g., "Harvest 5 Radish")
    private QuestType type; // Loại quest
    private ItemType targetItem; // Item mục tiêu (nullable, có thể null cho một số quest)
    private int targetAmount; // Số lượng mục tiêu

    // [SỬA] Đổi tên từ currentProgress thành currentAmount để khớp với GameManager và GameSaveState
    private int currentAmount; // Tiến độ hiện tại

    private double rewardMoney; // Tiền thưởng
    private double rewardXp; // XP thưởng
    private boolean isClaimed; // Đã nhận thưởng chưa

    /**
     * Constructor
     * @param description Mô tả quest
     * @param type Loại quest
     * @param targetItem Item mục tiêu (có thể null)
     * @param targetAmount Số lượng mục tiêu
     * @param rewardMoney Tiền thưởng
     * @param rewardXp XP thưởng
     */
    public Quest(String description, QuestType type, ItemType targetItem, int targetAmount, double rewardMoney, double rewardXp) {
        this.description = description;
        this.type = type;
        this.targetItem = targetItem;
        this.targetAmount = targetAmount;
        this.currentAmount = 0; // [SỬA] Khởi tạo currentAmount
        this.rewardMoney = rewardMoney;
        this.rewardXp = rewardXp;
        this.isClaimed = false;
    }

    /**
     * Tăng tiến độ quest
     * @param amount Số lượng tăng
     */
    public void incrementProgress(int amount) {
        // [SỬA] Sử dụng currentAmount
        this.currentAmount = Math.min(this.currentAmount + amount, this.targetAmount);
    }

    /**
     * Kiểm tra xem quest đã hoàn thành chưa
     * @return true nếu đã hoàn thành
     */
    public boolean isCompleted() {
        // [SỬA] Sử dụng currentAmount
        return this.currentAmount >= this.targetAmount;
    }

    /**
     * Lấy phần trăm hoàn thành (0.0 - 1.0)
     * @return Phần trăm hoàn thành
     */
    public double getProgressPercent() {
        if (targetAmount == 0) return 1.0;
        // [SỬA] Sử dụng currentAmount
        return Math.min(1.0, (double) currentAmount / targetAmount);
    }
}