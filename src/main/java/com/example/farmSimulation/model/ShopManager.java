package com.example.farmSimulation.model;

import com.example.farmSimulation.config.ShopConfig;
import com.example.farmSimulation.model.GameSaveState.SavedShopSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Quản lý logic mua bán trong Shop
 */
public class ShopManager {
    private final Player player;
    private final Random random;
    private List<ShopSlot> currentDailyStock; // Danh sách items trong shop hôm nay
    private QuestManager questManager; // Quản lý quest (sẽ được set từ bên ngoài)

    public ShopManager(Player player) {
        this.player = player;
        this.random = new Random();
        this.currentDailyStock = new ArrayList<>();

        // Khởi tạo shop stock khi bắt đầu game
        generateDailyStock(true); // Cho phép discount lần đầu
    }

    /**
     * Set QuestManager để track quest progress khi bán hàng
     */
    public void setQuestManager(QuestManager questManager) {
        this.questManager = questManager;
    }

    /**
     * Lấy reference đến Player (để ShopView có thể truy cập hotbar items)
     * @return Player instance
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Lấy danh sách daily stock hiện tại
     * @return List of ShopSlot
     */
    public List<ShopSlot> getCurrentDailyStock() {
        return currentDailyStock;
    }

    /**
     * Tạo danh sách items ngẫu nhiên cho shop
     * Sử dụng "Deck Shuffle" logic để đảm bảo mỗi item chỉ xuất hiện một lần trong daily stock
     * @param allowDiscounts true nếu cho phép discount, false nếu không
     */
    public void generateDailyStock(boolean allowDiscounts) {
        currentDailyStock.clear();

        // Lấy danh sách tất cả items có thể mua (buyPrice > 0)
        List<ItemType> allBuyableItems = new ArrayList<>();
        for (ItemType itemType : ItemType.values()) {
            if (itemType.getBuyPrice() > 0) {
                allBuyableItems.add(itemType);
            }
        }

        // Xáo trộn danh sách để đảm bảo tính ngẫu nhiên và tránh trùng lặp
        Collections.shuffle(allBuyableItems, random);

        // Xác định số lượng items sẽ được chọn (đảm bảo không vượt quá số items có sẵn)
        int count = Math.min(ShopConfig.DAILY_SHOP_SLOTS, allBuyableItems.size());

        // Tạo ShopSlot cho từng item đã được shuffle (đảm bảo unique)
        for (int i = 0; i < count; i++) {
            ItemType selectedItem = allBuyableItems.get(i);

            // Chọn số lượng ngẫu nhiên (MIN đến MAX)
            int quantity = ShopConfig.MIN_ITEM_QUANTITY +
                    random.nextInt(ShopConfig.MAX_ITEM_QUANTITY - ShopConfig.MIN_ITEM_QUANTITY + 1);

            // Xác định discount (nếu cho phép)
            double discountRate = 0.0;
            if (allowDiscounts && random.nextDouble() < ShopConfig.DISCOUNT_CHANCE) {
                // Discount từ 10% đến MAX_DISCOUNT_RATE
                discountRate = 0.1 + random.nextDouble() * (ShopConfig.MAX_DISCOUNT_RATE - 0.1);
            }

            // Tạo ShopSlot và thêm vào danh sách
            ShopSlot slot = new ShopSlot(selectedItem, quantity, discountRate);
            currentDailyStock.add(slot);
        }
    }

    /**
     * Khôi phục stock từ file save
     */
    public void restoreStock(List<SavedShopSlot> savedSlots) {
        currentDailyStock.clear();
        for (SavedShopSlot saved : savedSlots) {
            ShopSlot slot = new ShopSlot(saved.itemType, saved.quantity, saved.discountRate);
            currentDailyStock.add(slot);
        }
    }

    /**
     * Reroll shop stock (trả tiền để refresh items)
     * @return Error message nếu có, null nếu thành công
     */
    public String rerollStock() {
        // Kiểm tra tiền
        if (player.getMoney() < ShopConfig.REROLL_PRICE) {
            return "Not enough money! Need: " + ShopConfig.REROLL_PRICE + ", Have: " + (int)player.getMoney();
        }

        // Trừ tiền
        boolean spent = player.spendMoney(ShopConfig.REROLL_PRICE);
        if (!spent) {
            return "Error deducting money";
        }

        // Generate stock mới (KHÔNG có discount để balance)
        generateDailyStock(false);

        return null; // Thành công
    }

    /**
     * Mua item từ shop (mua từ daily stock slot)
     * @param shopSlotIndex Index trong currentDailyStock
     * @param quantity Số lượng muốn mua
     * @return Chuỗi lỗi nếu có, null nếu thành công
     */
    public String buyItem(int shopSlotIndex, int quantity) {
        // Kiểm tra slot hợp lệ
        if (shopSlotIndex < 0 || shopSlotIndex >= currentDailyStock.size()) {
            return "Invalid shop slot";
        }

        ShopSlot slot = currentDailyStock.get(shopSlotIndex);

        // Kiểm tra item còn hàng không
        if (slot.isSoldOut()) {
            return "Item is sold out";
        }

        // Kiểm tra số lượng
        if (quantity > slot.getQuantity()) {
            quantity = slot.getQuantity(); // Mua tất cả số lượng còn lại
        }

        // Tính giá tiền (đã bao gồm discount)
        double totalPrice = slot.getPrice() * quantity;

        // Kiểm tra tiền
        if (player.getMoney() < totalPrice) {
            return "Not enough money! Need: " + (int)totalPrice + ", Have: " + (int)player.getMoney();
        }

        // Kiểm tra inventory có chỗ không
        boolean canAdd = canAddItemToInventory(slot.getItemType(), quantity);
        if (!canAdd) {
            return "Inventory is full!";
        }

        // Trừ tiền
        boolean spent = player.spendMoney(totalPrice);
        if (!spent) {
            return "Error deducting money";
        }

        // Thêm item vào inventory
        boolean added = player.addItem(slot.getItemType(), quantity);
        if (!added) {
            // Nếu thêm thất bại, hoàn lại tiền
            player.addMoney(totalPrice);
            return "Cannot add item to inventory";
        }

        // Giảm số lượng trong shop slot
        slot.setQuantity(slot.getQuantity() - quantity);

        return null; // Thành công
    }

    /**
     * Bán item cho shop
     * @param slotIndex Slot trong hotbar chứa item cần bán
     * @param quantity Số lượng muốn bán
     * @return Chuỗi lỗi nếu có, null nếu thành công
     */
    public String sellItem(int slotIndex, int quantity) {
        // Kiểm tra slot hợp lệ
        if (slotIndex < 0 || slotIndex >= player.getHotbarItems().length) {
            return "Invalid slot";
        }

        ItemStack stack = player.getHotbarItems()[slotIndex];
        if (stack == null) {
            return "No item in this slot";
        }

        ItemType itemType = stack.getItemType();

        // Kiểm tra item có thể bán được không
        if (itemType.getSellPrice() <= 0) {
            return "Cannot sell this item";
        }

        // Kiểm tra số lượng
        int availableQuantity = stack.getQuantity();
        if (quantity > availableQuantity) {
            quantity = availableQuantity; // Bán tất cả số lượng có
        }

        // Tính tổng tiền nhận được
        double totalPrice = itemType.getSellPrice() * quantity;

        // Xóa item khỏi inventory
        player.consumeItemAtSlot(slotIndex, quantity);

        // Cộng tiền
        player.addMoney(totalPrice);

        // Quest tracking: Sell items
        if (questManager != null) {
            questManager.onEvent(QuestType.SELL, itemType, quantity);
        }

        return null; // Thành công
    }

    /**
     * Kiểm tra xem có thể thêm item vào inventory không
     * @param itemType Loại item
     * @param quantity Số lượng
     * @return true nếu có thể thêm, false nếu không
     */
    private boolean canAddItemToInventory(ItemType itemType, int quantity) {
        ItemStack[] hotbarItems = player.getHotbarItems();
        int remaining = quantity;

        // Kiểm tra stack vào ô có sẵn
        for (ItemStack stack : hotbarItems) {
            if (stack != null && stack.getItemType() == itemType) {
                int canAdd = itemType.getMaxStackSize() - stack.getQuantity();
                remaining -= Math.min(canAdd, remaining);
                if (remaining <= 0) return true;
            }
        }

        // Kiểm tra ô trống
        int emptySlots = 0;
        for (ItemStack stack : hotbarItems) {
            if (stack == null) {
                emptySlots++;
            }
        }

        // Tính số ô cần thiết
        int slotsNeeded = (int) Math.ceil((double) remaining / itemType.getMaxStackSize());
        return emptySlots >= slotsNeeded;
    }
}