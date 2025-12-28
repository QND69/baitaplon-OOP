package com.example.farmSimulation.model;

import com.example.farmSimulation.config.AnimalConfig;
import com.example.farmSimulation.config.AssetPaths;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * Định nghĩa các loại động vật trong game (Enum).
 * Lớp này chứa các thông tin cố định cho từng loài như tên, đường dẫn hình ảnh, kích thước, sản phẩm thu hoạch, v.v.
 */
public enum AnimalType {
    // Cấu trúc định nghĩa: Tên hiển thị, Đường dẫn ảnh, Kích thước cắt ảnh (sprite size), Chiều rộng vùng va chạm, Chiều cao vùng va chạm,
    // Tốc độ di chuyển, Thức ăn chấp nhận, Sản phẩm tạo ra, Thời gian tạo sản phẩm, Thời gian lớn lên, Tỉ lệ hiển thị (scale)
    CHICKEN("Chicken",
            AssetPaths.CHICKEN_SHEET,
            AnimalConfig.SPRITE_SIZE_CHICKEN,
            AnimalConfig.SMALL_ANIMAL_HITBOX_WIDTH,
            AnimalConfig.SMALL_ANIMAL_HITBOX_HEIGHT,
            AnimalConfig.BASE_MOVEMENT_SPEED,
            Arrays.asList(ItemType.SEEDS_STRAWBERRY, ItemType.SEEDS_DAIKON, ItemType.SEEDS_POTATO, ItemType.SEEDS_CARROT,
                    ItemType.SEEDS_WATERMELON, ItemType.SEEDS_TOMATO, ItemType.SEEDS_WHEAT, ItemType.SEEDS_CORN, ItemType.SUPER_FEED),
            ItemType.EGG,
            AnimalConfig.BASE_PRODUCTION_TIME_MS,
            0L,
            AnimalConfig.SCALE_CHICKEN), // Sử dụng tỉ lệ hiển thị riêng biệt

    COW("Cow",
            AssetPaths.COW_SHEET, // Kích thước ảnh gốc 96x96
            AnimalConfig.SPRITE_SIZE_COW,
            AnimalConfig.LARGE_ANIMAL_HITBOX_WIDTH,
            AnimalConfig.LARGE_ANIMAL_HITBOX_HEIGHT,
            AnimalConfig.BASE_MOVEMENT_SPEED * 0.8, // Bò di chuyển chậm hơn so với tốc độ cơ bản
            Arrays.asList(ItemType.CORN, ItemType.WHEAT, ItemType.SUPER_FEED),
            ItemType.MILK,
            AnimalConfig.BASE_PRODUCTION_TIME_MS * 2L, // Thời gian sản xuất sữa lâu hơn bình thường
            0L,
            AnimalConfig.SCALE_COW), // Sử dụng tỉ lệ hiển thị riêng biệt

    PIG("Pig",
            AssetPaths.PIG_SHEET, // Kích thước ảnh gốc 96x96
            AnimalConfig.SPRITE_SIZE_PIG,
            AnimalConfig.LARGE_ANIMAL_HITBOX_WIDTH,
            AnimalConfig.LARGE_ANIMAL_HITBOX_HEIGHT,
            AnimalConfig.BASE_MOVEMENT_SPEED * 0.9,
            Arrays.asList(ItemType.STRAWBERRY, ItemType.DAIKON, ItemType.POTATO, ItemType.CARROT,
                    ItemType.WATERMELON, ItemType.TOMATO, ItemType.CORN, ItemType.SUPER_FEED), // Lợn là loài ăn tạp (chấp nhận hầu hết nông sản trừ lúa mì)
            null, // Lợn không tạo ra sản phẩm thu hoạch định kỳ
            AnimalConfig.BASE_PRODUCTION_TIME_MS,
            0L,
            AnimalConfig.SCALE_PIG), // Sử dụng tỉ lệ hiển thị riêng biệt

    SHEEP("Sheep",
            AssetPaths.SHEEP_SHEET, // Kích thước ảnh gốc 64x64
            AnimalConfig.SPRITE_SIZE_SHEEP,
            AnimalConfig.LARGE_ANIMAL_HITBOX_WIDTH,
            AnimalConfig.LARGE_ANIMAL_HITBOX_HEIGHT,
            AnimalConfig.BASE_MOVEMENT_SPEED * 0.85,
            Arrays.asList(ItemType.CORN, ItemType.WHEAT, ItemType.SUPER_FEED),
            ItemType.WOOL,
            (long)(AnimalConfig.BASE_PRODUCTION_TIME_MS * 1.5), // Thời gian mọc lông cừu
            0L,
            AnimalConfig.SCALE_SHEEP), // Sử dụng tỉ lệ hiển thị riêng biệt

    BABY_COW("Baby Cow",
            AssetPaths.COW_SHEET, // Dùng chung tệp hình ảnh với bò trưởng thành
            AnimalConfig.SPRITE_SIZE_COW, // Sử dụng kích thước gốc là 96 để cắt khung hình chính xác
            AnimalConfig.SMALL_ANIMAL_HITBOX_WIDTH,
            AnimalConfig.SMALL_ANIMAL_HITBOX_HEIGHT,
            AnimalConfig.BASE_MOVEMENT_SPEED * 0.7, // Di chuyển chậm hơn con trưởng thành
            Arrays.asList(ItemType.CORN, ItemType.WHEAT, ItemType.SUPER_FEED),
            null, // Bò con chưa thể cho sữa
            AnimalConfig.BASE_PRODUCTION_TIME_MS,
            AnimalConfig.BABY_ANIMAL_GROWTH_TIME_MS, // Thời gian cần thiết để trưởng thành
            AnimalConfig.SCALE_BABY_COW), // Tỉ lệ 0.5 để vẽ hình ảnh nhỏ hơn

    BABY_PIG("Baby Pig",
            AssetPaths.PIG_SHEET, // Dùng chung tệp hình ảnh với lợn trưởng thành
            AnimalConfig.SPRITE_SIZE_PIG, // Sử dụng kích thước gốc là 96
            AnimalConfig.SMALL_ANIMAL_HITBOX_WIDTH,
            AnimalConfig.SMALL_ANIMAL_HITBOX_HEIGHT,
            AnimalConfig.BASE_MOVEMENT_SPEED * 0.7, // Di chuyển chậm hơn con trưởng thành
            Arrays.asList(ItemType.STRAWBERRY, ItemType.DAIKON, ItemType.POTATO, ItemType.CARROT,
                    ItemType.WATERMELON, ItemType.TOMATO, ItemType.CORN, ItemType.SUPER_FEED), // Lợn con ăn tạp giống lợn lớn
            null, // Lợn con không có sản phẩm
            AnimalConfig.BASE_PRODUCTION_TIME_MS,
            AnimalConfig.BABY_ANIMAL_GROWTH_TIME_MS, // Thời gian cần thiết để trưởng thành
            AnimalConfig.SCALE_BABY_PIG), // Tỉ lệ 0.5

    BABY_SHEEP("Baby Sheep",
            AssetPaths.SHEEP_SHEET, // Dùng chung tệp hình ảnh với cừu trưởng thành
            AnimalConfig.SPRITE_SIZE_SHEEP, // Sử dụng kích thước gốc là 64
            AnimalConfig.SMALL_ANIMAL_HITBOX_WIDTH,
            AnimalConfig.SMALL_ANIMAL_HITBOX_HEIGHT,
            AnimalConfig.BASE_MOVEMENT_SPEED * 0.7, // Di chuyển chậm hơn con trưởng thành
            Arrays.asList(ItemType.CORN, ItemType.WHEAT, ItemType.SUPER_FEED),
            null, // Cừu con chưa thể cho len
            AnimalConfig.BASE_PRODUCTION_TIME_MS,
            AnimalConfig.BABY_ANIMAL_GROWTH_TIME_MS, // Thời gian cần thiết để trưởng thành
            AnimalConfig.SCALE_BABY_SHEEP), // Tỉ lệ 0.5

    BABY_CHICKEN("Baby Chicken",
            AssetPaths.BABY_CHICKEN_EGG_SHEET, // Sử dụng tệp hình ảnh riêng cho gà con (chung với trứng)
            AnimalConfig.SPRITE_SIZE_BABY_CHICKEN_EGG, // Kích thước ảnh 32x32
            AnimalConfig.BABY_CHICKEN_HITBOX_WIDTH,
            AnimalConfig.BABY_CHICKEN_HITBOX_HEIGHT,
            AnimalConfig.BASE_MOVEMENT_SPEED * 0.7, // Di chuyển chậm hơn
            Arrays.asList(ItemType.SEEDS_STRAWBERRY, ItemType.SEEDS_DAIKON, ItemType.SEEDS_POTATO, ItemType.SEEDS_CARROT,
                    ItemType.SEEDS_WATERMELON, ItemType.SEEDS_TOMATO, ItemType.SEEDS_WHEAT, ItemType.SEEDS_CORN, ItemType.SUPER_FEED),
            null, // Gà con chưa thể đẻ trứng
            AnimalConfig.BASE_PRODUCTION_TIME_MS,
            AnimalConfig.BABY_CHICKEN_GROWTH_TIME_MS, // Thời gian cần thiết để trưởng thành
            AnimalConfig.SCALE_BABY_CHICKEN), // Giữ nguyên tỉ lệ 1.0 vì ảnh gốc 32px đã đủ nhỏ

    EGG_ENTITY("Egg",
            AssetPaths.BABY_CHICKEN_EGG_SHEET, // Trứng dùng chung tệp hình ảnh với gà con (32x32)
            AnimalConfig.SPRITE_SIZE_BABY_CHICKEN_EGG, // Kích thước ảnh 32x32 (dùng chung hằng số)
            AnimalConfig.BABY_CHICKEN_HITBOX_WIDTH,
            AnimalConfig.BABY_CHICKEN_HITBOX_HEIGHT,
            0.0, // Trứng không thể di chuyển
            Arrays.asList(), // Trứng không ăn gì cả
            null,
            0L,
            AnimalConfig.EGG_HATCH_TIME_MS, // Thời gian ấp nở
            AnimalConfig.SCALE_EGG); // Sử dụng tỉ lệ hiển thị riêng biệt

    @Getter private final String displayName;
    @Getter private final String assetPath;
    @Getter private final double spriteSize; // Kích thước khung hình gốc trong ảnh (để cắt sprite)
    @Getter private final double hitboxWidth;
    @Getter private final double hitboxHeight;
    @Getter private final double movementSpeed;
    @Getter private final List<ItemType> acceptedFood;
    @Getter private final ItemType product; // Sản phẩm thu hoạch được (trả về null nếu không có)
    @Getter private final long productionTimeMs; // Thời gian cần thiết để tạo ra sản phẩm
    @Getter private final long growthTimeMs; // Thời gian để trưởng thành (bằng 0 nếu đã là con trưởng thành)
    @Getter private final double scale; // Tỉ lệ phóng to hoặc thu nhỏ hình ảnh khi vẽ lên màn hình

    AnimalType(String displayName, String assetPath, double spriteSize,
               double hitboxWidth, double hitboxHeight, double movementSpeed,
               List<ItemType> acceptedFood, ItemType product, long productionTimeMs, long growthTimeMs, double scale) {
        this.displayName = displayName;
        this.assetPath = assetPath;
        this.spriteSize = spriteSize;
        this.hitboxWidth = hitboxWidth;
        this.hitboxHeight = hitboxHeight;
        this.movementSpeed = movementSpeed;
        this.acceptedFood = acceptedFood;
        this.product = product;
        this.productionTimeMs = productionTimeMs;
        this.growthTimeMs = growthTimeMs;
        this.scale = scale;
    }

    /**
     * Kiểm tra xem loại thức ăn này có được chấp nhận bởi động vật không
     */
    public boolean acceptsFood(ItemType food) {
        return acceptedFood.contains(food);
    }

    /**
     * Kiểm tra xem động vật này có khả năng tạo ra sản phẩm không
     */
    public boolean canProduce() {
        return product != null;
    }

    /**
     * Kiểm tra xem động vật này có thể lớn lên (trưởng thành) không
     */
    public boolean canGrow() {
        return growthTimeMs > 0;
    }

    // Phương thức hỗ trợ lấy loại con non tương ứng (phục vụ cho tính năng lai tạo)
    public AnimalType getBabyType() {
        switch (this) {
            case COW: return BABY_COW;
            case PIG: return BABY_PIG;
            case SHEEP: return BABY_SHEEP;
            case CHICKEN: return BABY_CHICKEN; // Hoặc trả về EGG_ENTITY tùy theo logic game
            default: return null;
        }
    }
}