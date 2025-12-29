package com.example.farmSimulation.view.assets;

import com.example.farmSimulation.config.*;
import com.example.farmSimulation.model.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lớp chịu trách nhiệm tải, lưu trữ (cache) và quản lý toàn bộ tài nguyên hình ảnh của trò chơi.
 */
public class ImageManager {

    // ==============================================================================================
    // 1. KHAI BÁO CACHE (BỘ NHỚ ĐỆM)
    // ==============================================================================================

    // Bộ nhớ đệm lưu trữ các ảnh gốc tải từ file, dùng đường dẫn làm khóa
    private final Map<String, Image> textureCache = new HashMap<>();

    // Bộ nhớ đệm riêng cho Texture của các ô đất, sử dụng EnumMap để tối ưu hiệu năng
    private final Map<Tile, Image> tileTextureMap = new EnumMap<>(Tile.class);

    // Bộ nhớ đệm cho các sprite đã được cắt nhỏ từ ảnh gốc (ví dụ: từng frame của nhân vật)
    private final Map<String, Image> spriteCache = new HashMap<>();

    // Bộ nhớ đệm cho icon của vật phẩm, dùng để hiển thị trong kho đồ và cửa hàng
    private final Map<ItemType, Image> itemIconCache = new EnumMap<>(ItemType.class);

    // Bộ nhớ đệm cho các biểu tượng trạng thái của cây trồng (ví dụ: cần nước, cần phân bón)
    private final Map<CropStatusIndicator, Image> statusIconCache = new EnumMap<>(CropStatusIndicator.class);

    // Bộ nhớ đệm cho các biểu tượng giao diện người dùng (Cài đặt, Tiền, Thời tiết...)
    private final Map<String, Image> guiIconCache = new HashMap<>();

    // ==============================================================================================
    // 2. KHỞI TẠO VÀ TẢI TÀI NGUYÊN GỐC
    // ==============================================================================================

    /**
     * Tải toàn bộ tài nguyên cần thiết vào bộ nhớ khi bắt đầu game.
     * Hàm này chỉ nên được gọi một lần duy nhất.
     */
    public void loadAssets() {
        // Tải tài nguyên nhân vật
        getTexture(AssetPaths.PLAYER_SHEET);
        getTexture(AssetPaths.PLAYER_ACTIONS_SHEET);

        // Tải tài nguyên giao diện
        getTexture(AssetPaths.LOGO);
        Image itemsSheet = getTexture(AssetPaths.ITEMS_SHEET);
        getTexture(AssetPaths.ANIMAL_ITEM_SHEET);
        getTexture(AssetPaths.ICON_BG);

        // Tải texture cho nền đất
        Image grass = getTexture(AssetPaths.GRASS);
        Image soil = getTexture(AssetPaths.SOIL);
        Image water = getTexture(AssetPaths.WATER);
        Image soilWet = getTexture(AssetPaths.SOIL_WET);

        // Tải tài nguyên trồng trọt
        getTexture(AssetPaths.FERTILIZER_OVERLAY);
        getTexture(AssetPaths.CROP_SHEET);

        // Tải tài nguyên môi trường (cây cối, hàng rào)
        // Lưu ý: Cây và Hàng rào sẽ được cắt từ sprite sheet chứ không dùng ảnh đơn lẻ
        getTexture(AssetPaths.TREE_SHEET);
        getTexture(AssetPaths.FENCE_SHEET);

        // Tải tài nguyên động vật
        loadAnimalTextures();

        // Liên kết dữ liệu Tile (Model) với hình ảnh (View)
        tileTextureMap.put(Tile.GRASS, grass);
        tileTextureMap.put(Tile.SOIL, soil);
        tileTextureMap.put(Tile.WATER, water);
        tileTextureMap.put(Tile.SOIL_WET, soilWet);

        // Cắt và lưu trữ các icon trạng thái từ bảng vật phẩm
        loadStatusIcons(itemsSheet);

        // Tải và xử lý các icon giao diện
        Image guiIconSheet = getTexture(AssetPaths.GUI_ICONS);
        loadGuiIcons(guiIconSheet);
    }

    /**
     * Tải sprite sheet cho tất cả các loại động vật định nghĩa trong Enum.
     */
    private void loadAnimalTextures() {
        for (AnimalType animalType : AnimalType.values()) {
            getTexture(animalType.getAssetPath());
        }
    }

    /**
     * Lấy ảnh từ bộ nhớ đệm. Nếu chưa có, hệ thống sẽ tự động tải từ file và lưu vào cache.
     * Sử dụng cơ chế lazy loading để tiết kiệm bộ nhớ.
     *
     * @param path Đường dẫn đến file ảnh.
     */
    public Image getTexture(String path) {
        // computeIfAbsent: Nếu khóa 'path' chưa tồn tại, hàm lambda sẽ được thực thi để tải ảnh mới,
        // sau đó tự động đưa vào map và trả về giá trị đó.
        return textureCache.computeIfAbsent(path, p ->
                new Image(getClass().getResourceAsStream(p))
        );
    }

    // ==============================================================================================
    // 3. XỬ LÝ TEXTURE MÔI TRƯỜNG (TILES, CÂY, HÀNG RÀO)
    // ==============================================================================================

    /**
     * Lấy ảnh texture tương ứng với loại Tile.
     */
    public Image getTileTexture(Tile tileType) {
        // Trả về cỏ mặc định nếu không tìm thấy loại tile yêu cầu
        return tileTextureMap.getOrDefault(tileType, tileTextureMap.get(Tile.GRASS));
    }

    /**
     * Lấy ảnh lớp phủ hiển thị trạng thái đã bón phân.
     */
    public Image getFertilizerTexture() {
        return getTexture(AssetPaths.FERTILIZER_OVERLAY);
    }

    /**
     * Cắt và lấy sprite của cây trồng dựa trên loại cây và giai đoạn phát triển.
     */
    public Image getCropTexture(CropData cropData) {
        if (cropData == null) {
            return null;
        }

        // Kiểm tra tính hợp lệ của dữ liệu
        if (cropData.getGrowthStage() < 0) {
            return null;
        }

        // Tạo khóa cache duy nhất dựa trên tên cây và giai đoạn
        String key = cropData.getType().name() + "_" + cropData.getGrowthStage();

        return spriteCache.computeIfAbsent(key, k -> {
            Image cropSheet = getTexture(AssetPaths.CROP_SHEET);
            if (cropSheet == null) return null;

            PixelReader reader = cropSheet.getPixelReader();

            // Tính toán tọa độ cắt ảnh
            double w = CropConfig.CROP_SPRITE_WIDTH;
            double h = CropConfig.CROP_SPRITE_HEIGHT;
            int x = (int) (cropData.getGrowthStage() * w);
            int y = (int) (cropData.getType().getSpriteRow() * h);

            if (x < 0 || y < 0 || x + w > cropSheet.getWidth() || y + h > cropSheet.getHeight()) return null;

            return new WritableImage(reader, x, y, (int) w, (int) h);
        });
    }

    /**
     * Cắt và lấy sprite của cây tự nhiên (cây gỗ).
     * Xử lý cả trạng thái cây đang lớn và gốc cây sau khi chặt.
     */
    public Image getTreeTexture(TreeData treeData) {
        if (treeData == null) {
            return null;
        }

        // Trường hợp 1: Cây đã bị chặt thành gốc
        if (treeData.getChopCount() > 0) {
            String key = "tree_stump";
            return spriteCache.computeIfAbsent(key, k -> {
                Image treeSheet = getTexture(AssetPaths.TREE_SHEET);
                if (treeSheet == null) return null;

                PixelReader reader = treeSheet.getPixelReader();
                double w = TreeConfig.TREE_SPRITE_WIDTH;
                double h = TreeConfig.TREE_SPRITE_HEIGHT;

                // Frame gốc cây được quy định tại chỉ số cố định
                int x = (int) (TreeConfig.TREE_STUMP_FRAME_INDEX * w);
                int y = 0;

                if (x < 0 || y < 0 || x + w > treeSheet.getWidth() || y + h > treeSheet.getHeight()) return null;

                return new WritableImage(reader, x, y, (int) w, (int) h);
            });
        }

        // Trường hợp 2: Cây đang phát triển bình thường
        int stage = treeData.getGrowthStage();
        String key = "tree_" + stage;

        return spriteCache.computeIfAbsent(key, k -> {
            Image treeSheet = getTexture(AssetPaths.TREE_SHEET);
            if (treeSheet == null) return null;

            PixelReader reader = treeSheet.getPixelReader();
            double w = TreeConfig.TREE_SPRITE_WIDTH;
            double h = TreeConfig.TREE_SPRITE_HEIGHT;

            int x = (int) (stage * w);
            int y = 0;

            if (x < 0 || y < 0 || x + w > treeSheet.getWidth() || y + h > treeSheet.getHeight()) return null;

            return new WritableImage(reader, x, y, (int) w, (int) h);
        });
    }

    /**
     * Lấy hình ảnh hàng rào dựa trên mẫu kết nối (pattern) và trạng thái đóng/mở.
     * Sử dụng kỹ thuật Bitmasking để xác định hình dạng kết nối.
     */
    public Image getFenceTexture(FenceData fenceData) {
        if (fenceData == null) return null;

        int width = (int) FenceConfig.FENCE_SPRITE_WIDTH;
        int height = (int) FenceConfig.FENCE_SPRITE_HEIGHT;

        // Nếu là cổng đang mở, lấy hình cái cọc đơn
        if (fenceData.isOpen()) {
            return spriteCache.computeIfAbsent("fence_open", k -> {
                Image fenceSheet = getTexture(AssetPaths.FENCE_SHEET);
                return new WritableImage(fenceSheet.getPixelReader(),
                        0, 3 * height, width, height); // Vị trí Cột 0, Hàng 3
            });
        }

        // Nếu rào đóng, xác định hình ảnh dựa trên mẫu kết nối (0-15).
        // Mẫu này được tính toán dựa trên việc kiểm tra 4 hướng xung quanh (Trên, Phải, Dưới, Trái).
        // Mỗi hướng tương ứng với một trọng số nhị phân: Trên=1, Phải=2, Dưới=4, Trái=8.
        int pattern = fenceData.getTilePattern();
        String key = "fence_" + pattern;

        return spriteCache.computeIfAbsent(key, k -> {
            Image fenceSheet = getTexture(AssetPaths.FENCE_SHEET);
            PixelReader reader = fenceSheet.getPixelReader();

            int col, row;

            switch (pattern) {
                // --- NHÓM 1: CÁC ĐẦU MÚT VÀ ĐƯỜNG THẲNG ---
                case 0:  col = 0; row = 3; break; // Cọc đơn, không nối
                case 1:  col = 0; row = 2; break; // Nối lên trên (Đầu dưới)
                case 4:  col = 0; row = 0; break; // Nối xuống dưới (Đầu trên)
                case 5:  col = 0; row = 1; break; // Thẳng đứng

                case 2:  col = 1; row = 0; break; // Nối sang phải (Đầu trái)
                case 8:  col = 3; row = 0; break; // Nối sang trái (Đầu phải)
                case 10: col = 2; row = 0; break; // Nằm ngang

                // --- NHÓM 2: CÁC GÓC ---
                case 6:  col = 1; row = 1; break; // Góc Trên-Trái
                case 12: col = 3; row = 1; break; // Góc Trên-Phải
                case 3:  col = 1; row = 3; break; // Góc Dưới-Trái
                case 9:  col = 3; row = 3; break; // Góc Dưới-Phải

                // --- NHÓM 3: NGÃ BA (CHỮ T) ---
                case 7:  col = 1; row = 2; break; // Chữ T quay phải
                case 11: col = 2; row = 3; break; // Chữ T quay lên
                case 13: col = 3; row = 2; break; // Chữ T quay trái
                case 14: col = 2; row = 1; break; // Chữ T quay xuống

                // --- NHÓM 4: NGÃ TƯ ---
                case 15: col = 2; row = 2; break; // Nối cả 4 hướng

                // Mặc định về cọc đơn nếu có lỗi
                default: col = 0; row = 3; break;
            }

            return new WritableImage(reader, col * width, row * height, width, height);
        });
    }

    // ==============================================================================================
    // 4. XỬ LÝ TEXTURE ĐỘNG VẬT
    // ==============================================================================================

    /**
     * Lấy sprite động vật dựa trên loại, hướng, hành động và frame hiện tại.
     * Hàm này xử lý logic phức tạp do cấu trúc spritesheet của Gà khác với các động vật còn lại.
     *
     * @param animalType Loại động vật
     * @param direction Hướng nhìn (0: Xuống, 1: Phải, 2: Trái, 3: Lên)
     * @param action Hành động (Đứng yên, Đi bộ, Ăn)
     * @param frameIndex Chỉ số frame hoạt hình
     */
    public Image getAnimalTexture(AnimalType animalType, int direction, Animal.Action action, int frameIndex) {
        if (animalType == null) return null;

        // Tạo khóa cache bao gồm cả frameIndex để lưu từng frame riêng biệt
        String key = animalType.name() + "_" + direction + "_" + action.name() + "_" + frameIndex;

        return spriteCache.computeIfAbsent(key, k -> {
            Image animalSheet = getTexture(animalType.getAssetPath());
            if (animalSheet == null) return null;

            PixelReader reader = animalSheet.getPixelReader();
            double spriteSize = animalType.getSpriteSize();

            int row = 0;
            int col = 0;

            // Xử lý riêng cho TRỨNG (đặt dưới đất)
            if (animalType == AnimalType.EGG_ENTITY) {
                // Trứng nằm ở hàng quy định cho Idle Down
                row = AnimalConfig.STANDARD_ROW_IDLE_DOWN;
                // Chọn ngẫu nhiên một biến thể hình ảnh trứng để tăng tính đa dạng
                int eggFrameStart = AnimalConfig.EGG_FRAME_START_INDEX;
                col = eggFrameStart + ThreadLocalRandom.current().nextInt(2);
            }
            // Xử lý riêng cho GÀ (do cấu trúc sheet khác biệt)
            else if (animalType == AnimalType.CHICKEN) {
                // Phân nhóm hướng: Nhóm A (Trái/Xuống) và Nhóm B (Phải/Trên)
                boolean isGroupA = (direction == 2 || direction == 0);

                if (action == Animal.Action.IDLE) {
                    row = isGroupA ? AnimalConfig.CHICKEN_ROW_IDLE_LEFT : AnimalConfig.CHICKEN_ROW_IDLE_RIGHT;
                } else if (action == Animal.Action.WALK) {
                    row = isGroupA ? AnimalConfig.CHICKEN_ROW_WALK_LEFT : AnimalConfig.CHICKEN_ROW_WALK_RIGHT;
                } else {
                    // Mặc định hành động ăn sẽ dùng sprite đứng yên
                    row = isGroupA ? AnimalConfig.CHICKEN_ROW_IDLE_LEFT : AnimalConfig.CHICKEN_ROW_IDLE_RIGHT;
                }
                col = frameIndex;
            }
            // Xử lý cho các động vật khác theo chuẩn chung (Bò, Cừu, Lợn...)
            else {
                if (action == Animal.Action.WALK) {
                    // Đi bộ sử dụng 4 hàng đầu tiên
                    switch (direction) {
                        case 0: row = AnimalConfig.STANDARD_ROW_WALK_DOWN; break;
                        case 3: row = AnimalConfig.STANDARD_ROW_WALK_UP; break;
                        case 2: row = AnimalConfig.STANDARD_ROW_WALK_LEFT; break;
                        case 1: row = AnimalConfig.STANDARD_ROW_WALK_RIGHT; break;
                        default: row = AnimalConfig.STANDARD_ROW_WALK_DOWN; break;
                    }
                } else { // Đứng yên hoặc Ăn
                    // Sử dụng 4 hàng tiếp theo
                    switch (direction) {
                        case 0: row = AnimalConfig.STANDARD_ROW_IDLE_DOWN; break;
                        case 3: row = AnimalConfig.STANDARD_ROW_IDLE_UP; break;
                        case 2: row = AnimalConfig.STANDARD_ROW_IDLE_LEFT; break;
                        case 1: row = AnimalConfig.STANDARD_ROW_IDLE_RIGHT; break;
                        default: row = AnimalConfig.STANDARD_ROW_IDLE_DOWN; break;
                    }
                }
                col = frameIndex;
            }

            int x = (int) (col * spriteSize);
            int y = (int) (row * spriteSize);

            // Kiểm tra biên để tránh lỗi nếu spritesheet thiếu frame
            if (x < 0 || y < 0 || x + spriteSize > animalSheet.getWidth() || y + spriteSize > animalSheet.getHeight()) {
                x = 0;
                y = (int) (row * spriteSize);
            }

            return new WritableImage(reader, x, y, (int) spriteSize, (int) spriteSize);
        });
    }

    // ==============================================================================================
    // 5. XỬ LÝ ICON VẬT PHẨM VÀ GUI
    // ==============================================================================================

    /**
     * Lấy icon nền cho các ô chứa vật phẩm.
     */
    public Image getIconBG() {
        return getTexture(AssetPaths.ICON_BG);
    }

    /**
     * Lấy icon trạng thái của cây trồng (đã được cache).
     */
    public Image getStatusIcon(CropStatusIndicator status) {
        return statusIconCache.get(status);
    }

    /**
     * Lấy icon GUI đã được cache theo tên.
     * @param iconName Tên icon (ví dụ: "SETTINGS", "MONEY", "SUNNY"...)
     */
    public Image getGuiIcon(String iconName) {
        return guiIconCache.get(iconName);
    }

    /**
     * Lấy icon hạt giống của cây trồng để hiển thị hình bóng mờ khi đặt.
     */
    public Image getSeedIcon(CropType type) {
        Image cropSheet = getTexture(AssetPaths.CROP_SHEET);
        if (cropSheet == null) return null;

        PixelReader reader = cropSheet.getPixelReader();
        int x = (int) (CropConfig.CROP_SEED_FRAME_INDEX * CropConfig.CROP_SPRITE_WIDTH);
        int y = (int) (type.getSpriteRow() * CropConfig.CROP_SPRITE_HEIGHT);

        return new WritableImage(reader, x, y, (int) CropConfig.CROP_SPRITE_WIDTH, (int) CropConfig.CROP_SPRITE_HEIGHT);
    }

    /**
     * Lấy icon hạt giống cây tự nhiên để hiển thị hình bóng mờ khi đặt.
     */
    public Image getTreeSeedIcon() {
        Image treeSheet = getTexture(AssetPaths.TREE_SHEET);
        if (treeSheet == null) return null;

        PixelReader reader = treeSheet.getPixelReader();
        int x = (int) (TreeConfig.TREE_SEED_STAGE * TreeConfig.TREE_SPRITE_WIDTH);
        int y = 0;

        return new WritableImage(reader, x, y, (int) TreeConfig.TREE_SPRITE_WIDTH, (int) TreeConfig.TREE_SPRITE_HEIGHT);
    }

    /**
     * Lấy icon sản phẩm thu hoạch (frame cuối cùng của cây trồng).
     */
    public Image getHarvestIcon(CropType type) {
        Image cropSheet = getTexture(AssetPaths.CROP_SHEET);
        if (cropSheet == null) return null;

        PixelReader reader = cropSheet.getPixelReader();
        int x = (int) (CropConfig.CROP_HARVEST_FRAME_INDEX * CropConfig.CROP_SPRITE_WIDTH);
        int y = (int) (type.getSpriteRow() * CropConfig.CROP_SPRITE_HEIGHT);

        return new WritableImage(reader, x, y, (int) CropConfig.CROP_SPRITE_WIDTH, (int) CropConfig.CROP_SPRITE_HEIGHT);
    }

    /**
     * Lưu trữ thủ công icon vật phẩm vào cache.
     */
    public void cacheItemIcon(ItemType type, Image icon) {
        itemIconCache.put(type, icon);
    }

    /**
     * Lấy icon của bất kỳ loại vật phẩm nào.
     * Hệ thống sẽ tự động xác định nguồn ảnh (Item thường hay Item vật nuôi) để cắt.
     */
    public Image getItemIcon(ItemType type) {
        if (type == null) return null;

        if (itemIconCache.containsKey(type)) {
            return itemIconCache.get(type);
        }

        Image icon;
        if (isAnimalItem(type)) {
            icon = getClippedAnimalItemIcon(type);
        } else {
            icon = getClippedGeneralItemIcon(type);
        }

        if (icon != null) {
            itemIconCache.put(type, icon);
        }
        return icon;
    }

    // Kiểm tra xem vật phẩm có phải là thẻ mua vật nuôi không
    private boolean isAnimalItem(ItemType type) {
        return type == ItemType.ITEM_COW || type == ItemType.ITEM_CHICKEN ||
                type == ItemType.ITEM_SHEEP || type == ItemType.ITEM_PIG;
    }

    // Cắt icon từ bảng vật phẩm động vật
    private Image getClippedAnimalItemIcon(ItemType itemType) {
        int col = -1;
        if (itemType == ItemType.ITEM_COW) col = ItemSpriteConfig.ANIMAL_ITEM_COW_COL;
        else if (itemType == ItemType.ITEM_CHICKEN) col = ItemSpriteConfig.ANIMAL_ITEM_CHICKEN_COL;
        else if (itemType == ItemType.ITEM_SHEEP) col = ItemSpriteConfig.ANIMAL_ITEM_SHEEP_COL;
        else if (itemType == ItemType.ITEM_PIG) col = ItemSpriteConfig.ANIMAL_ITEM_PIG_COL;

        if (col < 0) return null;

        Image sheet = getTexture(AssetPaths.ANIMAL_ITEM_SHEET);
        return clipItemFromSheet(sheet, col, 0);
    }

    // Cắt icon từ bảng vật phẩm chung
    private Image getClippedGeneralItemIcon(ItemType type) {
        int col;
        switch (type) {
            case AXE: col = ItemSpriteConfig.ITEM_AXE_COL; break;
            case HOE: col = ItemSpriteConfig.ITEM_HOE_COL; break;
            case WATERING_CAN: col = ItemSpriteConfig.ITEM_WATERING_CAN_COL; break;
            case FERTILIZER: col = ItemSpriteConfig.ITEM_FERTILISER_COL; break;
            case PICKAXE: col = ItemSpriteConfig.ITEM_PICKAXE_COL; break;
            case SHOVEL: col = ItemSpriteConfig.ITEM_SHOVEL_COL; break;
            case SWORD: col = ItemSpriteConfig.ITEM_SWORD_COL; break;
            case SHEARS: col = ItemSpriteConfig.ITEM_SCISSORS_COL; break;
            case MILK_BUCKET: col = ItemSpriteConfig.ITEM_MILK_BUCKET_COL; break;
            case FULL_MILK_BUCKET: col = ItemSpriteConfig.ITEM_FULL_MILK_BUCKET_COL; break;
            case EGG: col = ItemSpriteConfig.ITEM_EGG_COL; break;
            case WOOD: col = ItemSpriteConfig.ITEM_WOOD_COL; break;
            case WOOL: col = ItemSpriteConfig.ITEM_WOOL_COL; break;
            case MILK: col = ItemSpriteConfig.ITEM_FULL_MILK_BUCKET_COL; break;
            case MEAT_CHICKEN: col = ItemSpriteConfig.ITEM_MEAT_CHICKEN_COL; break;
            case MEAT_COW: col = ItemSpriteConfig.ITEM_MEAT_COW_COL; break;
            case MEAT_PIG: col = ItemSpriteConfig.ITEM_MEAT_PIG_COL; break;
            case MEAT_SHEEP: col = ItemSpriteConfig.ITEM_MEAT_SHEEP_COL; break;
            case ENERGY_DRINK: col = ItemSpriteConfig.ITEM_ENERGY_DRINK_COL; break;
            case SUPER_FEED: col = ItemSpriteConfig.ITEM_SUPER_FEED_COL; break;
            default: col = ItemSpriteConfig.ITEM_SEEDS_BAGS_COL; break;
        }

        Image sheet = getTexture(AssetPaths.ITEMS_SHEET);
        return clipItemFromSheet(sheet, col, 0);
    }

    // Hàm tiện ích để cắt ảnh 32x32 từ sheet
    private Image clipItemFromSheet(Image sheet, int col, int row) {
        if (sheet == null) return null;
        PixelReader reader = sheet.getPixelReader();
        double w = ItemSpriteConfig.ITEM_SPRITE_WIDTH;
        double h = ItemSpriteConfig.ITEM_SPRITE_HEIGHT;

        int x = (int) (col * w);
        int y = (int) (row * h);

        if (x < 0 || y < 0 || x + w > sheet.getWidth() || y + h > sheet.getHeight()) return null;
        return new WritableImage(reader, x, y, (int) w, (int) h);
    }

    // Cắt và cache các icon trạng thái từ bảng vật phẩm
    private void loadStatusIcons(Image itemsSheet) {
        if (itemsSheet == null) return;
        PixelReader reader = itemsSheet.getPixelReader();

        // Icon Cần nước
        WritableImage waterIcon = new WritableImage(reader,
                (int) (ItemSpriteConfig.ITEM_WATERING_CAN_COL * ItemSpriteConfig.ITEM_SPRITE_WIDTH), 0,
                (int) ItemSpriteConfig.ITEM_SPRITE_WIDTH, (int) ItemSpriteConfig.ITEM_SPRITE_HEIGHT);
        statusIconCache.put(CropStatusIndicator.NEEDS_WATER, waterIcon);

        // Icon Cần phân bón
        WritableImage fertilizerIcon = new WritableImage(reader,
                (int) (ItemSpriteConfig.ITEM_FERTILISER_COL * ItemSpriteConfig.ITEM_SPRITE_WIDTH), 0,
                (int) ItemSpriteConfig.ITEM_SPRITE_WIDTH, (int) ItemSpriteConfig.ITEM_SPRITE_HEIGHT);
        statusIconCache.put(CropStatusIndicator.NEEDS_FERTILIZER, fertilizerIcon);

        // Icon kết hợp
        WritableImage combinedIcon = new WritableImage(reader,
                (int) (ItemSpriteConfig.ITEM_WATERING_CAN_COL * ItemSpriteConfig.ITEM_SPRITE_WIDTH), 0,
                (int) (ItemSpriteConfig.ITEM_SPRITE_WIDTH * 2), (int) ItemSpriteConfig.ITEM_SPRITE_HEIGHT);
        statusIconCache.put(CropStatusIndicator.NEED_WATER_AND_FERTILIZER, combinedIcon);

        // Icon Thu hoạch
        WritableImage harvestIcon = new WritableImage(reader,
                (int) (ItemSpriteConfig.ITEM_SCYTHE_COL * ItemSpriteConfig.ITEM_SPRITE_WIDTH), 0,
                (int) ItemSpriteConfig.ITEM_SPRITE_WIDTH, (int) ItemSpriteConfig.ITEM_SPRITE_HEIGHT);
        statusIconCache.put(CropStatusIndicator.READY_TO_HARVEST, harvestIcon);
    }

    // Cắt và cache các icon giao diện người dùng
    private void loadGuiIcons(Image guiIconSheet) {
        if (guiIconSheet == null) return;

        PixelReader reader = guiIconSheet.getPixelReader();
        double iconSize = HudConfig.GUI_ICON_SIZE;
        int row = 0;

        // Định nghĩa các icon cần cắt và khóa tương ứng
        Map<String, Integer> iconMap = new HashMap<>();
        iconMap.put("SETTINGS", HudConfig.GUI_ICON_SETTINGS_COL);
        iconMap.put("SHOP", HudConfig.GUI_ICON_SHOP_COL);
        iconMap.put("MONEY", HudConfig.GUI_ICON_MONEY_COL);
        iconMap.put("SUNNY", HudConfig.GUI_ICON_SUNNY_COL);
        iconMap.put("RAIN", HudConfig.GUI_ICON_RAIN_COL);
        iconMap.put("ENERGY_EMPTY", HudConfig.GUI_ICON_ENERGY_EMPTY_COL);
        iconMap.put("ENERGY_FULL", HudConfig.GUI_ICON_ENERGY_FULL_COL);
        iconMap.put("TRASH", HudConfig.GUI_ICON_TRASH_COL);
        iconMap.put("QUEST", HudConfig.GUI_ICON_QUEST_COL);
        iconMap.put("STAMINA", HudConfig.GUI_ICON_STAMINA_COL);
        iconMap.put("EXP", HudConfig.GUI_ICON_EXP_COL);

        // Thực hiện cắt và lưu vào cache
        for (Map.Entry<String, Integer> entry : iconMap.entrySet()) {
            WritableImage icon = new WritableImage(reader,
                    (int) (entry.getValue() * iconSize), (int) (row * iconSize),
                    (int) iconSize, (int) iconSize);
            guiIconCache.put(entry.getKey(), icon);
        }
    }
}