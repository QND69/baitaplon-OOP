package com.example.farmSimulation.model;

import com.example.farmSimulation.config.WeatherConfig;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

// Lớp này dùng để đóng gói toàn bộ dữ liệu game nhằm phục vụ việc lưu trữ xuống file.
// Việc hiện thực giao diện Serializable là bắt buộc để Java có thể tuần tự hóa đối tượng này.
public class GameSaveState implements Serializable {
    private static final long serialVersionUID = 1L;

    // 1. Dữ liệu Người chơi
    public String playerName; // Tên người chơi
    public double playerMoney;
    public double playerXP;
    public int playerLevel;
    public double playerStamina;
    public double playerX;
    public double playerY;
    // Danh sách vật phẩm trong kho, được lưu trữ đơn giản gồm loại và số lượng
    public List<SavedItemStack> inventory = new ArrayList<>();

    // 2. Dữ liệu Thời gian
    public double currentDaySeconds; // Thời điểm hiện tại trong ngày tính bằng giây
    // Ngày hiện tại trong game, được lưu riêng biến này để đảm bảo dữ liệu nhất quán
    public int currentDay;

    // 3. Dữ liệu Động vật bao gồm vị trí, chủng loại, tuổi và chỉ số đói
    public List<SavedAnimal> animals = new ArrayList<>();

    // 4. Dữ liệu Thế giới
    // Chỉ lưu danh sách các ô đất có sự thay đổi so với mặc định để tối ưu dung lượng file
    public List<SavedTileData> worldTiles = new ArrayList<>();

    // 5. Dữ liệu Thời tiết
    public WeatherConfig.WeatherType currentWeather;

    // 6. Dữ liệu Cửa hàng bao gồm các vật phẩm bán trong ngày
    public List<SavedShopSlot> dailyShopStock = new ArrayList<>();

    // 7. Dữ liệu Nhiệm vụ đang thực hiện
    public List<SavedQuest> activeQuests = new ArrayList<>();

    // Các lớp hỗ trợ bên dưới dùng để lưu trữ chi tiết cấu trúc dữ liệu của từng đối tượng

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

    // Lớp lưu trữ toàn bộ thông tin chi tiết của một ô đất bao gồm cây trồng, hàng rào, trạng thái đất và vật phẩm rơi
    public static class SavedTileData implements Serializable {
        public int col, row;
        public Tile baseType; // Loại nền đất cơ bản như cỏ, đất thường, đất ướt

        // Trạng thái của đất
        public boolean isWatered;
        public boolean isFertilized;
        public long lastWateredTime;
        public long fertilizerStartTime;

        // Dữ liệu về cây trồng nông nghiệp
        public boolean hasCrop;
        public CropType cropType;
        public int cropStage;

        // Dữ liệu về cây tự nhiên
        public boolean hasTree;
        public int treeStage;
        public int treeChopCount;

        // Dữ liệu về hàng rào
        public boolean hasFence;
        public boolean fenceIsOpen;

        // Dữ liệu về vật phẩm nằm trên mặt đất
        public boolean hasGroundItem;
        public ItemType groundItemType;
        public int groundItemAmount;
        public int groundItemDurability;
        public double groundItemOffsetX;
        public double groundItemOffsetY;

        public SavedTileData() {}
    }

    // Lớp lưu trữ thông tin về một vật phẩm được bán trong cửa hàng
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

    // Lớp lưu trữ thông tin chi tiết về nhiệm vụ
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