package com.example.farmSimulation.model;

import com.example.farmSimulation.config.ShopConfig;
import com.example.farmSimulation.model.GameSaveState.SavedShopSlot;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Getter
@Setter

/**
 * Lớp quản lý logic mua bán trong Cửa hàng
 */
public class ShopManager {
    private final Player player;
    private final Random random;
    private List<ShopSlot> currentDailyStock; // Danh sách các vật phẩm có sẵn trong cửa hàng hôm nay
    private QuestManager questManager; // Trình quản lý nhiệm vụ, sẽ được thiết lập từ bên ngoài

    public ShopManager(Player player) {
        this.player = player;
        this.random = new Random();
        this.currentDailyStock = new ArrayList<>();

        // Khởi tạo kho hàng của cửa hàng ngay khi bắt đầu game
        generateDailyStock(true); // Cho phép áp dụng giảm giá cho lần tạo đầu tiên
    }

    /**
     * Tạo danh sách vật phẩm ngẫu nhiên cho cửa hàng mỗi ngày.
     * Sử dụng thuật toán xáo trộn bộ bài để đảm bảo mỗi vật phẩm chỉ xuất hiện tối đa một lần trong danh sách.
     * @param allowDiscounts true nếu cho phép giảm giá, false nếu không
     */
    public void generateDailyStock(boolean allowDiscounts) {
        currentDailyStock.clear();

        // Lấy danh sách tất cả các vật phẩm có thể mua được, tức là có giá mua lớn hơn 0
        List<ItemType> allBuyableItems = new ArrayList<>();
        for (ItemType itemType : ItemType.values()) {
            if (itemType.getBuyPrice() > 0) {
                allBuyableItems.add(itemType);
            }
        }

        // Xáo trộn danh sách vật phẩm để tạo sự ngẫu nhiên và tránh trùng lặp khi chọn
        Collections.shuffle(allBuyableItems, random);

        // Xác định số lượng vật phẩm sẽ xuất hiện trong cửa hàng, không vượt quá số lượng vật phẩm khả dụng
        int count = Math.min(ShopConfig.DAILY_SHOP_SLOTS, allBuyableItems.size());

        // Tạo các ô hàng cho từng vật phẩm đã được chọn
        for (int i = 0; i < count; i++) {
            ItemType selectedItem = allBuyableItems.get(i);

            // Chọn số lượng ngẫu nhiên cho mỗi vật phẩm trong khoảng từ MIN đến MAX
            int quantity = ShopConfig.MIN_ITEM_QUANTITY +
                    random.nextInt(ShopConfig.MAX_ITEM_QUANTITY - ShopConfig.MIN_ITEM_QUANTITY + 1);

            // Xác định mức giảm giá nếu được phép
            double discountRate = 0.0;
            if (allowDiscounts && random.nextDouble() < ShopConfig.DISCOUNT_CHANCE) {
                // Mức giảm giá dao động từ 10% đến mức tối đa được cấu hình
                discountRate = 0.1 + random.nextDouble() * (ShopConfig.MAX_DISCOUNT_RATE - 0.1);
            }

            // Tạo đối tượng ô hàng và thêm vào danh sách
            ShopSlot slot = new ShopSlot(selectedItem, quantity, discountRate);
            currentDailyStock.add(slot);
        }
    }

    /**
     * Khôi phục danh sách hàng hóa từ dữ liệu đã lưu
     */
    public void restoreStock(List<SavedShopSlot> savedSlots) {
        currentDailyStock.clear();
        for (SavedShopSlot saved : savedSlots) {
            ShopSlot slot = new ShopSlot(saved.itemType, saved.quantity, saved.discountRate);
            currentDailyStock.add(slot);
        }
    }

    /**
     * Làm mới danh sách hàng hóa trong cửa hàng, người chơi phải trả phí để thực hiện.
     * @return Thông báo lỗi nếu có, hoặc null nếu thành công
     */
    public String rerollStock() {
        // Kiểm tra xem người chơi có đủ tiền không
        if (player.getMoney() < ShopConfig.REROLL_PRICE) {
            return "Not enough money! Need: " + ShopConfig.REROLL_PRICE + ", Have: " + (int)player.getMoney();
        }

        // Trừ tiền của người chơi
        boolean spent = player.spendMoney(ShopConfig.REROLL_PRICE);
        if (!spent) {
            return "Error deducting money";
        }

        // Tạo danh sách hàng hóa mới, nhưng không áp dụng giảm giá để cân bằng game
        generateDailyStock(false);

        return null; // Trả về null báo hiệu thành công
    }

    /**
     * Mua vật phẩm từ cửa hàng.
     * @param shopSlotIndex Chỉ số của ô hàng trong danh sách hàng ngày
     * @param quantity Số lượng muốn mua
     * @return Thông báo lỗi nếu có, hoặc null nếu thành công
     */
    public String buyItem(int shopSlotIndex, int quantity) {
        // Kiểm tra tính hợp lệ của chỉ số ô hàng
        if (shopSlotIndex < 0 || shopSlotIndex >= currentDailyStock.size()) {
            return "Invalid shop slot";
        }

        ShopSlot slot = currentDailyStock.get(shopSlotIndex);

        // Kiểm tra xem vật phẩm còn hàng không
        if (slot.isSoldOut()) {
            return "Item is sold out";
        }

        // Điều chỉnh số lượng mua nếu vượt quá số lượng hiện có
        if (quantity > slot.getQuantity()) {
            quantity = slot.getQuantity(); // Mua hết số lượng còn lại
        }

        // Tính tổng giá tiền, đã bao gồm giảm giá nếu có
        double totalPrice = slot.getPrice() * quantity;

        // Kiểm tra khả năng chi trả của người chơi
        if (player.getMoney() < totalPrice) {
            return "Not enough money! Need: " + (int)totalPrice + ", Have: " + (int)player.getMoney();
        }

        // Kiểm tra túi đồ của người chơi có đủ chỗ chứa không
        boolean canAdd = canAddItemToInventory(slot.getItemType(), quantity);
        if (!canAdd) {
            return "Inventory is full!";
        }

        // Thực hiện trừ tiền
        boolean spent = player.spendMoney(totalPrice);
        if (!spent) {
            return "Error deducting money";
        }

        // Thêm vật phẩm vào túi đồ
        boolean added = player.addItem(slot.getItemType(), quantity);
        if (!added) {
            // Nếu thêm thất bại vì lý do nào đó, hoàn lại tiền cho người chơi
            player.addMoney(totalPrice);
            return "Cannot add item to inventory";
        }

        // Giảm số lượng vật phẩm trong kho của cửa hàng
        slot.setQuantity(slot.getQuantity() - quantity);

        return null; // Giao dịch thành công
    }

    /**
     * Bán vật phẩm từ túi đồ của người chơi cho cửa hàng.
     * @param slotIndex Chỉ số ô trong thanh công cụ chứa vật phẩm cần bán
     * @param quantity Số lượng muốn bán
     * @return Thông báo lỗi nếu có, hoặc null nếu thành công
     */
    public String sellItem(int slotIndex, int quantity) {
        // Kiểm tra tính hợp lệ của ô chứa đồ
        if (slotIndex < 0 || slotIndex >= player.getHotbarItems().length) {
            return "Invalid slot";
        }

        ItemStack stack = player.getHotbarItems()[slotIndex];
        if (stack == null) {
            return "No item in this slot";
        }

        ItemType itemType = stack.getItemType();

        // Kiểm tra xem vật phẩm này có thể bán được hay không
        if (itemType.getSellPrice() <= 0) {
            return "Cannot sell this item";
        }

        // Kiểm tra số lượng bán thực tế so với số lượng hiện có
        int availableQuantity = stack.getQuantity();
        if (quantity > availableQuantity) {
            quantity = availableQuantity; // Bán tất cả số lượng đang có
        }

        // Tính tổng số tiền người chơi nhận được
        double totalPrice = itemType.getSellPrice() * quantity;

        // Xóa vật phẩm khỏi túi đồ
        player.consumeItemAtSlot(slotIndex, quantity);

        // Cộng tiền cho người chơi
        player.addMoney(totalPrice);

        // Theo dõi tiến độ nhiệm vụ bán hàng nếu có
        if (questManager != null) {
            questManager.onEvent(QuestType.SELL, itemType, quantity);
        }

        return null; // Giao dịch thành công
    }

    /**
     * Kiểm tra xem có thể thêm vật phẩm vào túi đồ hay không.
     * Hàm này tính toán dựa trên khả năng xếp chồng vật phẩm và số lượng ô trống.
     * @param itemType Loại vật phẩm
     * @param quantity Số lượng cần thêm
     * @return true nếu có thể thêm, false nếu không
     */
    private boolean canAddItemToInventory(ItemType itemType, int quantity) {
        ItemStack[] hotbarItems = player.getHotbarItems();
        int remaining = quantity;

        // Kiểm tra khả năng xếp chồng vào các ô đã có vật phẩm cùng loại
        for (ItemStack stack : hotbarItems) {
            if (stack != null && stack.getItemType() == itemType) {
                int canAdd = itemType.getMaxStackSize() - stack.getQuantity();
                remaining -= Math.min(canAdd, remaining);
                if (remaining <= 0) return true;
            }
        }

        // Đếm số lượng ô hoàn toàn trống
        int emptySlots = 0;
        for (ItemStack stack : hotbarItems) {
            if (stack == null) {
                emptySlots++;
            }
        }

        // Tính toán số lượng ô trống cần thiết cho phần còn lại
        int slotsNeeded = (int) Math.ceil((double) remaining / itemType.getMaxStackSize());
        return emptySlots >= slotsNeeded;
    }
}