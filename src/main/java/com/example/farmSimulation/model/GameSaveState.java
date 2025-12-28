package com.example.farmSimulation.model;

import com.example.farmSimulation.config.WeatherConfig;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

// Class này dùng để "đóng gói" dữ liệu game để lưu xuống file
// implement Serializable là bắt buộc để Java biết class này có thể lưu được
public class GameSaveState implements Serializable {
    private static final long serialVersionUID = 1L;

    // 1. Dữ liệu Player
    public String playerName; // Lưu tên người chơi
    public double playerMoney;
    public double playerXP;
    public int playerLevel;
    public double playerStamina;
    public double playerX;
    public double playerY;
    // Lưu inventory dưới dạng mảng đơn giản (ItemType và số lượng)
    public List<SavedItemStack> inventory = new ArrayList<>();

    // 2. Dữ liệu Thời gian
    public double currentDaySeconds; // Lưu giây hiện tại trong ngày
    // currentDay có thể tính từ total seconds hoặc lưu riêng, ở đây ta lưu riêng cho chắc
    public int currentDay;

    // 3. Dữ liệu Động vật (Vị trí, loại, tuổi, đói...)
    public List<SavedAnimal> animals = new ArrayList<>();

    // 4. Dữ liệu Thế giới (Thay thế SavedCrop bằng SavedTileData bao quát hơn)
    // Lưu danh sách các ô đất có sự thay đổi (không lưu ô GRASS mặc định để nhẹ file)
    public List<SavedTileData> worldTiles = new ArrayList<>();

    // 5. Dữ liệu Thời tiết
    public WeatherConfig.WeatherType currentWeather;

    // 6. Dữ liệu Shop (Vật phẩm bán trong ngày)
    public List<SavedShopSlot> dailyShopStock = new ArrayList<>();

    // 7. Dữ liệu Quest (Nhiệm vụ hàng ngày)
    public List<SavedQuest> activeQuests = new ArrayList<>();

    // --- Các class con (Helper) để lưu chi tiết ---
    public static class SavedItemStack implements Serializable {
        public ItemType type;
        public int quantity;
        public int durability;
        public SavedItemStack(ItemType type, int quantity, int durability) {
            this.type = type;
            this.quantity = quantity;
            this.durability = durability;
        }
    }

    public static class SavedAnimal implements Serializable {
        public AnimalType type;
        public double x, y;
        public int age;
        public double hunger;
        public SavedAnimal(AnimalType type, double x, double y, int age, double hunger) {
            this.type = type;
            this.x = x; this.y = y;
            this.age = age; this.hunger = hunger;
        }
    }

    // Class lưu toàn bộ thông tin của một ô đất (Cây, Rào, Đất, Item...)
    public static class SavedTileData implements Serializable {
        public int col, row;
        public Tile baseType; // GRASS, SOIL, SOIL_WET

        // Trạng thái đất
        public boolean isWatered;
        public boolean isFertilized;
        public long lastWateredTime;
        public long fertilizerStartTime;

        // Cây trồng (Crop)
        public boolean hasCrop;
        public CropType cropType;
        public int cropStage;

        // Cây tự nhiên (Tree)
        public boolean hasTree;
        public int treeStage;
        public int treeChopCount;

        // Hàng rào (Fence)
        public boolean hasFence;
        public boolean fenceIsOpen;

        // Item trên đất (Ground Item)
        public boolean hasGroundItem;
        public ItemType groundItemType;
        public int groundItemAmount;
        public int groundItemDurability;
        public double groundItemOffsetX;
        public double groundItemOffsetY;

        public SavedTileData() {}
    }

    // Class lưu thông tin một slot trong shop
    public static class SavedShopSlot implements Serializable {
        public ItemType itemType;
        public int quantity;
        public double discountRate;

        public SavedShopSlot(ItemType itemType, int quantity, double discountRate) {
            this.itemType = itemType;
            this.quantity = quantity;
            this.discountRate = discountRate;
        }
    }

    // Class lưu thông tin nhiệm vụ
    public static class SavedQuest implements Serializable {
        public String description;
        public QuestType type;
        public ItemType targetItem;
        public int targetAmount;
        public int currentAmount;
        public double rewardMoney;
        public double rewardXp;
        public boolean isClaimed;

        public SavedQuest() {}
    }
}