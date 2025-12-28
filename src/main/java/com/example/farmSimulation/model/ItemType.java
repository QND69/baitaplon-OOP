package com.example.farmSimulation.model;

import com.example.farmSimulation.config.GameLogicConfig;
import lombok.Getter;

public enum ItemType {
    // --- CÔNG CỤ (TOOLS) ---
    // Giá mua: 200-500, không bán được (sellPrice = 0)
    HOE("Hoe", 1, false, GameLogicConfig.MAX_DURABILITY_HOE, 200, 0, 0.0),
    WATERING_CAN("Watering Can", 1, false, GameLogicConfig.MAX_WATER_CAPACITY, 250, 0, 0.0),
    PICKAXE("Pickaxe", 1, false, GameLogicConfig.MAX_DURABILITY_PICKAXE, 0, 0, 0.0),
    SHOVEL("Shovel", 1, false, GameLogicConfig.MAX_DURABILITY_SHOVEL, 250, 0, 0.0),
    AXE("Axe", 1, false, GameLogicConfig.MAX_DURABILITY_AXE, 300, 0, 0.0),
    SWORD("Sword", 1, false, GameLogicConfig.MAX_DURABILITY_SWORD, 400, 0, 0.0),
    FERTILIZER("Fertilizer", 36, true, 0, 30, 0, 0.0), // Phân bón: mua 30, không bán

    // --- HẠT GIỐNG (SEEDS) ---
    // Giá mua: 50-80, không bán được
    SEEDS_STRAWBERRY("Strawberry Seeds", 36, true, 0, 50, 0, 0.0),
    SEEDS_DAIKON("Daikon Seeds", 36, true, 0, 50, 0, 0.0),
    SEEDS_POTATO("Potato Seeds", 36, true, 0, 60, 0, 0.0),
    SEEDS_CARROT("Carrot Seeds", 36, true, 0, 60, 0, 0.0),
    SEEDS_WATERMELON("Watermelon Seeds", 36, true, 0, 70, 0, 0.0),
    SEEDS_TOMATO("Tomato Seeds", 36, true, 0, 65, 0, 0.0),
    SEEDS_WHEAT("Wheat Seeds", 36, true, 0, 55, 0, 0.0),
    SEEDS_CORN("Corn Seeds", 36, true, 0, 65, 0, 0.0),
    SEEDS_TREE("Tree Seeds", 36, true, 0, 40, 0, 0.0), // Hạt giống cây

    // --- SẢN PHẨM (CROPS/PRODUCE) ---
    // Không mua được, chỉ bán: 80-120
    STRAWBERRY("Strawberry", 36, true, 0, 0, 100, 10.0), // Hồi 10 stamina
    DAIKON("Daikon", 36, true, 0, 0, 80, 10.0), // Hồi 10 stamina
    POTATO("Potato", 36, true, 0, 0, 100, 15.0), // Hồi 15 stamina
    CARROT("Carrot", 36, true, 0, 0, 100, 10.0), // Hồi 10 stamina
    WATERMELON("Watermelon", 36, true, 0, 0, 140, 20.0), // Hồi 20 stamina
    TOMATO("Tomato", 36, true, 0, 0, 110, 12.0), // Hồi 12 stamina
    WHEAT("Wheat", 36, true, 0, 0, 90, 5.0), // Hồi 5 stamina
    CORN("Corn", 36, true, 0, 0, 120, 15.0), // Hồi 15 stamina
    
    // --- VẬT LIỆU (MATERIALS) ---
    WOOD("Wood", 36, true, 0, 20, 10, 0.0), // Gỗ: mua 20, bán 10
    
    // --- SẢN PHẨM ĐỘNG VẬT (ANIMAL PRODUCTS) ---
    // Không mua được, chỉ bán: 50-150
    EGG("Egg", 9, true, 0, 0, 50, 0.0), // Trứng
    MILK("Milk", 9, true, 0, 0, 80, 0.0), // Sữa
    WOOL("Wool", 9, true, 0, 0, 100, 0.0), // Len
    MEAT_CHICKEN("Chicken Meat", 9, true, 0, 0, 120, 20.0), // Thịt gà - Hồi 20 stamina
    MEAT_COW("Cow Meat", 9, true, 0, 0, 150, 20.0), // Thịt bò - Hồi 20 stamina
    MEAT_PIG("Pig Meat", 9, true, 0, 0, 130, 20.0), // Thịt lợn - Hồi 20 stamina
    MEAT_SHEEP("Sheep Meat", 9, true, 0, 0, 140, 20.0), // Thịt cừu - Hồi 20 stamina
    
    // --- CÔNG CỤ ĐỘNG VẬT (ANIMAL ITEMS) ---
    SHEARS("Shears", 1, false, 50, 300, 0, 0.0), // Kéo: mua 300, không bán
    MILK_BUCKET("Milk Bucket", 18, true, 0, 100, 0, 0.0), // Xô rỗng: mua 100
    FULL_MILK_BUCKET("Full Milk Bucket", 18, true, 0, 0, 150, 0.0), // Xô đầy: bán 150
    
    // --- THỨC ĂN (FEED) ---
    SUPER_FEED("Super Feed", 36, true, 0, 150, 0, 0.0), // Thức ăn siêu: mua 150 (cho mọi động vật)
    
    // --- ĐỒ ĂN NGƯỜI CHƠI (PLAYER FOOD) ---
    ENERGY_DRINK("Energy Drink", 9, true, 0, 100, 0, 50.0), // Nước tăng lực: mua 100, hồi +50 Stamina
    
    // --- VẬT NUÔI (LIVESTOCK ITEMS - để đặt) ---
    // Giá mua: 500-800, không bán được
    ITEM_COW("Cow", 9, true, 0, 800, 0, 0.0), 
    ITEM_CHICKEN("Chicken", 9, true, 0, 500, 0, 0.0), 
    ITEM_SHEEP("Sheep", 9, true, 0, 700, 0, 0.0), 
    ITEM_PIG("Pig", 9, true, 0, 600, 0, 0.0); 

    @Getter
    private final String name;
    @Getter
    private final int maxStackSize;
    @Getter
    private final boolean stackable;
    @Getter
    private final int maxDurability; // Độ bền tối đa
    @Getter
    private final int buyPrice; // Giá mua từ shop (0 = không bán được)
    @Getter
    private final int sellPrice; // Giá bán cho shop (0 = không mua được)
    @Getter
    private final double staminaRestore; // Lượng stamina hồi phục khi ăn (0 = không thể ăn)

    ItemType(String name, int maxStackSize, boolean stackable, int maxDurability, int buyPrice, int sellPrice, double staminaRestore) {
        this.name = name;
        this.maxStackSize = maxStackSize;
        this.stackable = stackable;
        this.maxDurability = maxDurability;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.staminaRestore = staminaRestore;
    }
    
    // Constructor cũ để tương thích (giá mua/bán = 0, staminaRestore = 0)
    ItemType(String name, int maxStackSize, boolean stackable, int maxDurability) {
        this(name, maxStackSize, stackable, maxDurability, 0, 0, 0.0);
    }
    
    // Constructor với staminaRestore (giá mua/bán = 0)
    ItemType(String name, int maxStackSize, boolean stackable, int maxDurability, double staminaRestore) {
        this(name, maxStackSize, stackable, maxDurability, 0, 0, staminaRestore);
    }

    // Kiểm tra xem item này có dùng độ bền không
    public boolean hasDurability() {
        return maxDurability > 0;
    }
}