package com.example.farmSimulation.model;

import com.example.farmSimulation.config.GameLogicConfig;
import com.example.farmSimulation.config.HotbarConfig;
import com.example.farmSimulation.view.MainGameView;
import com.example.farmSimulation.view.PlayerView;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Player {
    private String name;
    private String gender; // Giới tính của người chơi
    private double money;
    private double currentXP; // Kinh nghiệm hiện tại
    private double xpToNextLevel; // Kinh nghiệm cần thiết để lên cấp tiếp theo
    private int level;
    private double currentStamina; // Thể lực hiện tại
    private double maxStamina; // Thể lực tối đa

    // Tọa độ logic của người chơi trong thế giới game
    private double tileX;
    private double tileY;

    // Lưu trữ trạng thái logic
    // PlayerView sẽ chịu trách nhiệm lưu trữ trạng thái hiển thị hình ảnh
    private PlayerView.PlayerState state;
    private PlayerView.Direction direction;

    // --- Thanh công cụ ---
    private ItemStack[] hotbarItems;
    private int selectedHotbarSlot;

    // Tham chiếu đến MainGameView để hiển thị thông báo khi lên cấp
    private MainGameView mainGameView;

    // Thời điểm tử vong để tính toán thời gian hiển thị hoạt ảnh chết
    private long timeOfDeath = 0;

    // Hàm khởi tạo
    public Player() {
        this.tileX = GameLogicConfig.PLAYER_START_X;
        this.tileY = GameLogicConfig.PLAYER_START_Y;
        this.state = PlayerView.PlayerState.IDLE; // Trạng thái nghỉ ban đầu
        this.direction = PlayerView.Direction.DOWN; // Hướng nhìn mặc định
        this.money = GameLogicConfig.PLAYER_START_MONEY; // Tiền khởi điểm

        // Khởi tạo các chỉ số thể lực và kinh nghiệm
        this.maxStamina = GameLogicConfig.PLAYER_MAX_STAMINA;
        this.currentStamina = GameLogicConfig.PLAYER_START_STAMINA;
        this.level = GameLogicConfig.PLAYER_START_LEVEL;
        this.currentXP = GameLogicConfig.PLAYER_START_XP;
        this.xpToNextLevel = GameLogicConfig.PLAYER_START_XP_TO_NEXT_LEVEL;

        // Khởi tạo thanh công cụ
        this.hotbarItems = new ItemStack[HotbarConfig.HOTBAR_SLOT_COUNT];
        this.selectedHotbarSlot = 0;

        // Gán các vật phẩm ban đầu vào thanh công cụ
        this.hotbarItems[0] = new ItemStack(ItemType.AXE, 1);
        this.hotbarItems[1] = new ItemStack(ItemType.WATERING_CAN, 1);
        this.hotbarItems[2] = new ItemStack(ItemType.FERTILIZER, 10);
        this.hotbarItems[3] = new ItemStack(ItemType.EGG, 9);
        this.hotbarItems[4] = new ItemStack(ItemType.ITEM_COW, 2);
        this.hotbarItems[5] = new ItemStack(ItemType.ITEM_CHICKEN, 5);
        this.hotbarItems[6] = new ItemStack(ItemType.ITEM_PIG, 3);
        this.hotbarItems[7] = new ItemStack(ItemType.ITEM_SHEEP, 2);
        this.hotbarItems[8] = new ItemStack(ItemType.WOOD, 30);
        this.hotbarItems[9] = new ItemStack(ItemType.SUPER_FEED, 36);

        // Các ô còn lại để trống
    }

    /**
     * Lấy đối tượng ItemStack tại ô đang được chọn trên thanh công cụ
     */
    public ItemStack getCurrentItem() {
        return hotbarItems[selectedHotbarSlot];
    }

    // Hàm thêm vật phẩm vào túi đồ
    public boolean addItem(ItemType type, int amount) {
        boolean addedAny = false;

        // Chỉ thực hiện gộp nhóm nếu vật phẩm cho phép xếp chồng
        if (type.getMaxStackSize() > 1) {
            // Tìm các ô đã có vật phẩm cùng loại để gộp thêm vào
            for (ItemStack stack : hotbarItems) {
                if (stack != null && stack.getItemType() == type) {
                    int remaining = stack.add(amount);
                    if (remaining < amount) addedAny = true; // Đánh dấu là đã thêm được ít nhất một phần
                    if (remaining == 0) return true;
                    amount = remaining;
                }
            }
        }

        // Tìm ô trống đầu tiên để đặt vật phẩm mới
        for (int i = 0; i < hotbarItems.length; i++) {
            if (hotbarItems[i] == null) {
                hotbarItems[i] = new ItemStack(type, amount);
                return true;
            }
        }

        return addedAny; // Trả về false nếu túi đầy và không thêm được gì
    }

    /**
     * Thêm vật phẩm vào túi đồ với chỉ số độ bền cụ thể
     * @param type Loại vật phẩm
     * @param amount Số lượng
     * @param durability Độ bền của vật phẩm. Nếu nhỏ hơn hoặc bằng 0, sẽ sử dụng độ bền tối đa mặc định
     * @return true nếu thêm thành công, false nếu không
     */
    public boolean addItem(ItemType type, int amount, int durability) {
        boolean addedAny = false;

        // Chỉ thực hiện gộp nhóm nếu vật phẩm cho phép xếp chồng
        if (type.getMaxStackSize() > 1) {
            // Kiểm tra các ô có sẵn để gộp
            for (ItemStack stack : hotbarItems) {
                if (stack != null && stack.getItemType() == type) {
                    // Chỉ gộp được nếu cả hai đều không có độ bền, hoặc có cùng mức độ bền hiện tại
                    boolean canStack = !type.hasDurability() ||
                            (stack.getCurrentDurability() == (durability > 0 ? durability : type.getMaxDurability()));

                    if (canStack) {
                        int remaining = stack.add(amount);
                        if (remaining < amount) addedAny = true;
                        if (remaining == 0) return true;
                        amount = remaining;
                    }
                }
            }
        }

        // Tìm ô trống để thêm vật phẩm mới
        for (int i = 0; i < hotbarItems.length; i++) {
            if (hotbarItems[i] == null) {
                ItemStack newStack = new ItemStack(type, amount);
                // Thiết lập độ bền nếu vật phẩm có tính năng này
                if (type.hasDurability()) {
                    // Ưu tiên dùng độ bền được truyền vào, nếu không thì dùng độ bền tối đa
                    int finalDurability = (durability > 0) ? durability : type.getMaxDurability();
                    newStack.setCurrentDurability(finalDurability);
                }
                // Nếu vật phẩm không có độ bền, constructor của ItemStack đã tự xử lý
                hotbarItems[i] = newStack;
                return true;
            }
        }

        return addedAny; // Trả về false nếu túi đầy
    }

    /**
     * Tính toán số lượng vật phẩm tối đa có thể thêm vào túi đồ
     * @param type Loại vật phẩm
     * @param amount Số lượng muốn thêm
     * @return Số lượng thực tế có thể thêm
     */
    public int calculateAddableAmount(ItemType type, int amount) {
        if (amount <= 0) return 0;

        int remainingToAdd = amount;
        int maxStackSize = type.getMaxStackSize();

        // Kiểm tra khả năng gộp vào các ô có sẵn
        for (ItemStack stack : hotbarItems) {
            if (stack != null && stack.getItemType() == type) {
                int spaceAvailable = maxStackSize - stack.getQuantity();
                int canAdd = Math.min(spaceAvailable, remainingToAdd);
                remainingToAdd -= canAdd;
                if (remainingToAdd <= 0) return amount; // Đã đủ chỗ chứa tất cả
            }
        }

        // Kiểm tra số lượng ô trống hoàn toàn
        int emptySlots = 0;
        for (ItemStack stack : hotbarItems) {
            if (stack == null) {
                emptySlots++;
            }
        }

        // Tính tổng số lượng có thể thêm vào các ô trống
        int canAddFromEmptySlots = emptySlots * maxStackSize;
        int totalAddable = amount - remainingToAdd + Math.min(canAddFromEmptySlots, remainingToAdd);

        return Math.min(totalAddable, amount);
    }

    /**
     * Tiêu thụ vật phẩm hoặc giảm độ bền tại ô chỉ định.
     * Hàm này được gọi bởi ActionManager khi hành động kết thúc.
     */
    public void consumeItemAtSlot(int slotIndex, int amount) {
        if (slotIndex < 0 || slotIndex >= hotbarItems.length) return;
        ItemStack stack = hotbarItems[slotIndex];

        if (stack != null) {
            if (stack.getItemType().hasDurability()) {
                // Vật phẩm có độ bền sẽ bị giảm độ bền thay vì số lượng
                boolean broken = stack.decreaseDurability(amount);

                // Nếu độ bền về 0, vật phẩm sẽ bị hủy, bao gồm cả các công cụ như bình tưới
                if (broken) {
                    hotbarItems[slotIndex] = null; // Xóa vật phẩm khỏi ô
                    System.out.println("Item broken/consumed!");
                }
            } else {
                // Vật phẩm thông thường sẽ bị giảm số lượng
                stack.remove(amount);
                if (stack.isEmpty()) {
                    hotbarItems[slotIndex] = null;
                }
            }
        }
    }

    /**
     * Hoán đổi vị trí hai vật phẩm trên thanh công cụ
     */
    public void swapHotbarItems(int indexA, int indexB) {
        if (indexA < 0 || indexA >= hotbarItems.length || indexB < 0 || indexB >= hotbarItems.length) {
            return;
        }
        ItemStack temp = hotbarItems[indexA];
        hotbarItems[indexA] = hotbarItems[indexB];
        hotbarItems[indexB] = temp;

        // Logic chọn vật phẩm dựa trên chỉ số index nên không cần cập nhật lại selectedHotbarSlot
    }

    /**
     * Thêm tiền cho người chơi
     * @param amount Số tiền cần thêm
     * @return true nếu thành công, false nếu số tiền âm
     */
    public boolean addMoney(double amount) {
        if (amount < 0) {
            return false; // Không chấp nhận số âm
        }
        this.money += amount;
        return true;
    }

    /**
     * Trừ tiền của người chơi
     * @param amount Số tiền cần trừ
     * @return true nếu đủ tiền và trừ thành công, false nếu không đủ tiền
     */
    public boolean spendMoney(double amount) {
        if (amount < 0) {
            return false; // Không chấp nhận số âm
        }
        if (this.money < amount) {
            return false; // Tiền không đủ
        }
        this.money -= amount;
        return true;
    }

    // --- Các phương thức quản lý Thể lực ---

    /**
     * Giảm thể lực
     * @param amount Lượng thể lực cần giảm
     */
    public void reduceStamina(double amount) {
        this.currentStamina = Math.max(0, this.currentStamina - amount);

        // Kiểm tra điều kiện thua cuộc nếu thể lực cạn kiệt
        if (this.currentStamina <= 0) {
            this.currentStamina = 0;
            this.state = PlayerView.PlayerState.DEAD;
            this.timeOfDeath = System.currentTimeMillis(); // Ghi nhận thời gian chết để xử lý hoạt ảnh
            // Hiển thị thông báo ngất xỉu
            if (mainGameView != null) {
                mainGameView.showTemporaryText("You passed out!", tileX, tileY);
            }
        }
    }

    /**
     * Hồi phục thể lực
     * @param amount Lượng thể lực cần hồi phục
     */
    public void recoverStamina(double amount) {
        this.currentStamina = Math.min(maxStamina, this.currentStamina + amount);
    }

    /**
     * Kiểm tra xem người chơi có đang bị phạt do thể lực thấp hay không.
     * Hình phạt áp dụng khi thể lực giảm xuống mức cảnh báo đỏ.
     * @return true nếu thể lực dưới ngưỡng quy định
     */
    public boolean hasStaminaPenalty() {
        // Tính tỷ lệ phần trăm thể lực hiện tại
        double percentage = maxStamina > 0 ? (currentStamina / maxStamina) : 0.0;
        return percentage <= 0.15; // Phạt khi thể lực còn dưới 15%
    }

    /**
     * Lấy giá trị thể lực hiện tại
     */
    public double getStamina() {
        return currentStamina;
    }

    /**
     * Thiết lập giá trị thể lực hiện tại
     */
    public void setStamina(double stamina) {
        this.currentStamina = Math.min(maxStamina, Math.max(0, stamina));
    }

    /**
     * Lấy giá trị kinh nghiệm hiện tại
     */
    public int getExperience() {
        return (int) currentXP;
    }

    /**
     * Thiết lập giá trị kinh nghiệm hiện tại
     */
    public void setExperience(int experience) {
        this.currentXP = experience;
    }

    // --- Các phương thức quản lý XP và Cấp độ ---

    /**
     * Tăng kinh nghiệm cho người chơi
     * @param amount Lượng kinh nghiệm nhận được
     */
    public void gainXP(double amount) {
        this.currentXP += amount;

        // Kiểm tra xem đã đủ điều kiện lên cấp chưa
        while (currentXP >= xpToNextLevel) {
            levelUp();
        }
    }

    /**
     * Xử lý logic khi người chơi lên cấp
     */
    private void levelUp() {
        this.level++;
        this.currentXP -= xpToNextLevel;

        // Tăng giới hạn thể lực tối đa
        this.maxStamina += GameLogicConfig.STAMINA_INCREASE_PER_LEVEL;

        // Hồi đầy thể lực
        this.currentStamina = maxStamina;

        // Tăng mốc kinh nghiệm cần thiết cho cấp độ tiếp theo
        this.xpToNextLevel *= GameLogicConfig.XP_MULTIPLIER_PER_LEVEL;

        // Hiển thị thông báo lên cấp
        if (mainGameView != null) {
            mainGameView.showTemporaryText("LEVEL UP! Level " + level, tileX, tileY);
        }
    }

    /**
     * Sử dụng vật phẩm đang cầm để hồi phục thể lực
     * @return true nếu ăn thành công, false nếu không thể ăn
     */
    public boolean eatCurrentItem() {
        ItemStack currentItem = getCurrentItem();
        if (currentItem == null) return false;

        ItemType itemType = currentItem.getItemType();

        // Kiểm tra xem vật phẩm có thuộc loại thức ăn hồi thể lực không
        if (itemType.getStaminaRestore() <= 0) {
            return false;
        }

        // Không cho phép ăn nếu thể lực đã đầy
        if (currentStamina >= maxStamina) {
            return false;
        }

        // Thực hiện hồi phục thể lực
        recoverStamina(itemType.getStaminaRestore());

        // Giảm số lượng vật phẩm đi 1 đơn vị
        currentItem.remove(1);

        // Xóa vật phẩm khỏi ô chứa nếu đã dùng hết
        if (currentItem.isEmpty()) {
            hotbarItems[selectedHotbarSlot] = null;
        }

        // Chuyển trạng thái sang bận để ActionManager xử lý
        this.state = PlayerView.PlayerState.BUSY;

        return true;
    }
}