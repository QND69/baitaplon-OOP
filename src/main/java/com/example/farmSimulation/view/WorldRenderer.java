package com.example.farmSimulation.view;

import com.example.farmSimulation.config.AnimalConfig;
import com.example.farmSimulation.config.CropConfig;
import com.example.farmSimulation.config.HudConfig;
import com.example.farmSimulation.config.WorldConfig;
import com.example.farmSimulation.config.TreeConfig;
import com.example.farmSimulation.config.FenceConfig;
import com.example.farmSimulation.config.ItemSpriteConfig;
import com.example.farmSimulation.config.PlayerSpriteConfig;
import com.example.farmSimulation.model.Animal;
import com.example.farmSimulation.model.AnimalType;
import com.example.farmSimulation.model.Tile;
import com.example.farmSimulation.model.TileData;
import com.example.farmSimulation.model.WorldMap;
import com.example.farmSimulation.view.assets.ImageManager;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class WorldRenderer {

    // =================================================================================
    // 1. KHAI BÁO BIẾN VÀ TÀI NGUYÊN (FIELDS & RESOURCES)
    // =================================================================================

    // --- Các lớp hiển thị bản đồ (Map Layers) ---
    // Mảng 2D lưu trữ các đối tượng hình ảnh hiển thị trên lưới
    private final ImageView[][] baseTiles;        // Lớp 1: Nền đất (Cỏ, Đất trồng, Nước)
    private final ImageView[][] overlayTiles;     // Lớp 2: Lớp phủ (Phân bón)
    private final ImageView[][] cropTiles;        // Lớp 3: Cây trồng nông nghiệp
    private final ImageView[][] treeTiles;        // Lớp 4: Cây tự nhiên
    private final ImageView[][] fenceTiles;       // Lớp 5: Hàng rào
    private final ImageView[][] groundItemTiles;  // Lớp 6: Vật phẩm rơi trên đất
    private final ImageView[][] statusIconTiles;  // Lớp 7: Biểu tượng trạng thái
    private final ImageView[][] statusBackground; // Lớp 8: Nền mờ cho biểu tượng

    // --- Quản lý tài nguyên và dữ liệu ---
    private final ImageManager assetManager; // Quản lý tải và lấy hình ảnh
    private final WorldMap worldMap;         // Dữ liệu logic của bản đồ

    // --- Thành phần giao diện (UI Components) ---
    private final Pane worldPane;   // Container chứa bản đồ nền
    private final Pane entityPane;  // Container chứa các thực thể động (Động vật)
    private final Rectangle tileSelector; // Khung chọn ô lưới hiện tại

    // --- Hiển thị bóng mờ (Ghost Placement) ---
    // Hiển thị trước vị trí vật phẩm sẽ đặt xuống khi người chơi cầm item
    private final ImageView ghostPlacement;

    // --- Hỗ trợ Debug (Debug Helpers) ---
    private final Rectangle[][] treeHitboxes;  // Hiển thị vùng va chạm của cây
    private final Rectangle[][] fenceHitboxes; // Hiển thị vùng va chạm của hàng rào

    // --- Quản lý hiển thị động vật (Animal Rendering) ---
    // Map ánh xạ từ đối tượng logic sang đối tượng hiển thị
    private final Map<Animal, ImageView> animalViews;
    private final Map<Animal, ImageView> animalStatusIcons;       // Icon trạng thái (đói/sản phẩm)
    private final Map<Animal, ImageView> animalStatusBackgrounds; // Nền của icon trạng thái

    // --- Trạng thái Render ---
    // Lưu vị trí hàng/cột bắt đầu của lần vẽ trước để tối ưu hiệu năng
    private int lastRenderedStartCol = -1;
    private int lastRenderedStartRow = -1;

    // =================================================================================
    // 2. KHỞI TẠO (CONSTRUCTOR)
    // =================================================================================

    public WorldRenderer(ImageManager assetManager, WorldMap worldMap, Pane entityPane) {
        this.assetManager = assetManager;
        this.worldMap = worldMap;
        this.entityPane = entityPane;

        // Khởi tạo các mảng lưu trữ view
        this.baseTiles = new ImageView[WorldConfig.NUM_ROWS_ON_SCREEN][WorldConfig.NUM_COLS_ON_SCREEN];
        this.overlayTiles = new ImageView[WorldConfig.NUM_ROWS_ON_SCREEN][WorldConfig.NUM_COLS_ON_SCREEN];
        this.cropTiles = new ImageView[WorldConfig.NUM_ROWS_ON_SCREEN][WorldConfig.NUM_COLS_ON_SCREEN];
        this.treeTiles = new ImageView[WorldConfig.NUM_ROWS_ON_SCREEN][WorldConfig.NUM_COLS_ON_SCREEN];
        this.fenceTiles = new ImageView[WorldConfig.NUM_ROWS_ON_SCREEN][WorldConfig.NUM_COLS_ON_SCREEN];
        this.groundItemTiles = new ImageView[WorldConfig.NUM_ROWS_ON_SCREEN][WorldConfig.NUM_COLS_ON_SCREEN];
        this.statusIconTiles = new ImageView[WorldConfig.NUM_ROWS_ON_SCREEN][WorldConfig.NUM_COLS_ON_SCREEN];
        this.statusBackground = new ImageView[WorldConfig.NUM_ROWS_ON_SCREEN][WorldConfig.NUM_COLS_ON_SCREEN];

        this.treeHitboxes = new Rectangle[WorldConfig.NUM_ROWS_ON_SCREEN][WorldConfig.NUM_COLS_ON_SCREEN];
        this.fenceHitboxes = new Rectangle[WorldConfig.NUM_ROWS_ON_SCREEN][WorldConfig.NUM_COLS_ON_SCREEN];

        this.animalViews = new HashMap<>();
        this.animalStatusIcons = new HashMap<>();
        this.animalStatusBackgrounds = new HashMap<>();

        this.worldPane = new Pane();

        // Khởi tạo các đối tượng hình ảnh cho từng ô lưới trên màn hình
        for (int r = 0; r < WorldConfig.NUM_ROWS_ON_SCREEN; r++) {
            for (int c = 0; c < WorldConfig.NUM_COLS_ON_SCREEN; c++) {
                baseTiles[r][c] = createTileView(c, r, 0, WorldConfig.TILE_SIZE, WorldConfig.TILE_SIZE);
                overlayTiles[r][c] = createTileView(c, r, 0, WorldConfig.TILE_SIZE, WorldConfig.TILE_SIZE);

                // Các lớp đối tượng có thể có kích thước và vị trí bù trừ (offset) khác nhau
                cropTiles[r][c] = createTileView(c, r, -CropConfig.CROP_Y_OFFSET, CropConfig.CROP_SPRITE_WIDTH, CropConfig.CROP_SPRITE_HEIGHT);
                treeTiles[r][c] = createTileView(c, r, -TreeConfig.TREE_Y_OFFSET, TreeConfig.TREE_SPRITE_WIDTH, TreeConfig.TREE_SPRITE_HEIGHT);
                fenceTiles[r][c] = createTileView(c, r, -FenceConfig.FENCE_Y_OFFSET, FenceConfig.FENCE_SPRITE_WIDTH, FenceConfig.FENCE_SPRITE_HEIGHT);

                groundItemTiles[r][c] = createTileView(c, r, 0, ItemSpriteConfig.ITEM_SPRITE_WIDTH, ItemSpriteConfig.ITEM_SPRITE_HEIGHT);

                // Khởi tạo nền cho icon trạng thái
                ImageView bg = createTileView(c, r, -HudConfig.ICON_Y_OFFSET, HudConfig.ICON_BG_SIZE, HudConfig.ICON_BG_SIZE);
                bg.setImage(assetManager.getIconBG());
                bg.setVisible(false);
                bg.setLayoutX(c * WorldConfig.TILE_SIZE);
                statusBackground[r][c] = bg;

                // Khởi tạo icon trạng thái
                ImageView icon = createTileView(c, r, -HudConfig.ICON_Y_OFFSET, HudConfig.ICON_SIZE, HudConfig.ICON_SIZE);
                double iconOffset = (HudConfig.ICON_BG_SIZE - HudConfig.ICON_SIZE) / 2;
                icon.setLayoutX(c * WorldConfig.TILE_SIZE + iconOffset);
                icon.setLayoutY(r * WorldConfig.TILE_SIZE - HudConfig.ICON_Y_OFFSET + HudConfig.ICON_PADDING_TOP);
                statusIconTiles[r][c] = icon;

                // Khởi tạo hitbox debug nếu chế độ debug được bật
                initDebugHitboxes(r, c);
            }
        }

        // Thêm các view vào pane theo thứ tự lớp để đảm bảo hình ảnh hiển thị chồng nhau chính xác
        addTilesToPane();

        // Khởi tạo khung chọn ô (Selector)
        this.tileSelector = new Rectangle(WorldConfig.TILE_SIZE, WorldConfig.TILE_SIZE);
        this.tileSelector.setFill(null);
        this.tileSelector.setStroke(WorldConfig.SELECTOR_COLOR);
        this.tileSelector.setStrokeWidth(WorldConfig.SELECTOR_STROKE_WIDTH);
        this.tileSelector.setVisible(true);

        // Khởi tạo hiển thị bóng mờ
        this.ghostPlacement = new ImageView();
        this.ghostPlacement.setMouseTransparent(true);
        this.ghostPlacement.setVisible(false);
        this.ghostPlacement.setOpacity(WorldConfig.GHOST_PLACEMENT_OPACITY);
    }

    // =================================================================================
    // 3. LOGIC HIỂN THỊ BẢN ĐỒ (MAP RENDERING LOGIC)
    // =================================================================================

    /**
     * Cập nhật hiển thị bản đồ dựa trên vị trí camera
     *
     * @param worldOffsetX Độ dời X của thế giới (camera)
     * @param worldOffsetY Độ dời Y của thế giới (camera)
     * @param forceRedraw  Bắt buộc vẽ lại toàn bộ kể cả khi không đổi ô lưới
     */
    public void updateMap(double worldOffsetX, double worldOffsetY, boolean forceRedraw) {
        double cameraWorldX = -worldOffsetX;
        double cameraWorldY = -worldOffsetY;

        // Xác định ô lưới bắt đầu dựa trên tọa độ camera
        int startCol = (int) Math.floor(cameraWorldX / WorldConfig.TILE_SIZE);
        int startRow = (int) Math.floor(cameraWorldY / WorldConfig.TILE_SIZE);

        // Tính toán độ lệch pixel mịn để di chuyển mượt mà
        double pixelOffsetX = -(cameraWorldX - (startCol * WorldConfig.TILE_SIZE));
        double pixelOffsetY = -(cameraWorldY - (startRow * WorldConfig.TILE_SIZE));

        worldPane.setLayoutX(pixelOffsetX);
        worldPane.setLayoutY(pixelOffsetY);

        // Chỉ cập nhật texture nếu chuyển sang ô lưới mới hoặc bị buộc vẽ lại
        boolean needsTileUpdate = (startCol != lastRenderedStartCol ||
                startRow != lastRenderedStartRow ||
                forceRedraw);

        if (!needsTileUpdate) {
            return;
        }

        for (int r = 0; r < WorldConfig.NUM_ROWS_ON_SCREEN; r++) {
            for (int c = 0; c < WorldConfig.NUM_COLS_ON_SCREEN; c++) {
                int logicalCol = startCol + c;
                int logicalRow = startRow + r;
                TileData data = worldMap.getTileData(logicalCol, logicalRow);

                // Cập nhật nền đất
                Tile type = data.getBaseTileType();
                // Nếu là cây hoặc rào, nền dưới vẫn là cỏ
                Tile baseType = (type == Tile.TREE || type == Tile.FENCE) ? Tile.GRASS : type;
                this.baseTiles[r][c].setImage(assetManager.getTileTexture(baseType));

                // Cập nhật các lớp phủ
                this.overlayTiles[r][c].setImage(data.isFertilized() ? assetManager.getFertilizerTexture() : null);
                this.cropTiles[r][c].setImage(assetManager.getCropTexture(data.getCropData()));
                this.treeTiles[r][c].setImage(assetManager.getTreeTexture(data.getTreeData()));
                this.fenceTiles[r][c].setImage(assetManager.getFenceTexture(data.getFenceData()));

                // Cập nhật vật phẩm rơi trên đất
                if (data.getGroundItem() != null && data.getGroundItemAmount() > 0) {
                    Image itemTexture = assetManager.getItemIcon(data.getGroundItem());
                    this.groundItemTiles[r][c].setImage(itemTexture);
                    this.groundItemTiles[r][c].setTranslateX(data.getGroundItemOffsetX());
                    this.groundItemTiles[r][c].setTranslateY(data.getGroundItemOffsetY());
                    this.groundItemTiles[r][c].setVisible(true);
                } else {
                    this.groundItemTiles[r][c].setImage(null);
                    this.groundItemTiles[r][c].setVisible(false);
                }

                // Cập nhật hiển thị Debug Hitbox nếu được bật
                updateDebugHitboxVisualization(r, c, data);

                // Cập nhật Icon trạng thái (cần nước, cần thu hoạch...)
                updateStatusIcons(r, c, data);
            }
        }
        this.lastRenderedStartCol = startCol;
        this.lastRenderedStartRow = startRow;
    }

    /**
     * Di chuyển khung chọn theo tọa độ ô lưới
     */
    public void updateSelector(int tileSelectedX, int tileSelectedY, double worldOffsetX, double worldOffsetY) {
        if (this.tileSelector == null) return;
        double tileSelectedOnScreenX = tileSelectedX * WorldConfig.TILE_SIZE + worldOffsetX;
        double tileSelectedOnScreenY = tileSelectedY * WorldConfig.TILE_SIZE + worldOffsetY;
        this.tileSelector.setLayoutX(tileSelectedOnScreenX);
        this.tileSelector.setLayoutY(tileSelectedOnScreenY);
    }

    /**
     * Hiển thị hình ảnh mờ của vật phẩm (hàng rào/hạt giống) tại vị trí con trỏ
     */
    public void updateGhostPlacement(int tileX, int tileY, double worldOffsetX, double worldOffsetY, com.example.farmSimulation.model.ItemStack currentItem) {
        boolean shouldShow = false;
        Image ghostImage = null;
        double yOffsetCorrection = 0.0;

        if (currentItem != null) {
            com.example.farmSimulation.model.ItemType itemType = currentItem.getItemType();

            // Xử lý hiển thị cho Hàng rào
            if (itemType == com.example.farmSimulation.model.ItemType.WOOD) {
                ghostImage = assetManager.getFenceTexture(new com.example.farmSimulation.model.FenceData(false));
                shouldShow = true;
                yOffsetCorrection = FenceConfig.FENCE_Y_OFFSET;
            }
            // Xử lý hiển thị cho Hạt giống
            else if (itemType.name().startsWith("SEEDS_")) {
                if (itemType == com.example.farmSimulation.model.ItemType.SEEDS_TREE) {
                    ghostImage = assetManager.getTreeSeedIcon();
                    shouldShow = true;
                    yOffsetCorrection = TreeConfig.TREE_Y_OFFSET;
                } else {
                    try {
                        com.example.farmSimulation.model.CropType cropType = com.example.farmSimulation.model.CropType.valueOf(itemType.name().substring(6));
                        ghostImage = assetManager.getSeedIcon(cropType);
                        shouldShow = true;
                        yOffsetCorrection = CropConfig.CROP_Y_OFFSET;
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        if (shouldShow && ghostImage != null) {
            double screenX = tileX * WorldConfig.TILE_SIZE + worldOffsetX;
            double screenY = tileY * WorldConfig.TILE_SIZE + worldOffsetY;
            double imageWidth = ghostImage.getWidth();
            double imageHeight = ghostImage.getHeight();
            double offsetX = (WorldConfig.TILE_SIZE - imageWidth) / 2.0;

            double offsetY;
            // Điều chỉnh vị trí hiển thị cho cây để khớp với logic vẽ cây thông thường
            if (currentItem != null && currentItem.getItemType() == com.example.farmSimulation.model.ItemType.SEEDS_TREE) {
                offsetY = -TreeConfig.TREE_Y_OFFSET;
            } else {
                // Căn giữa theo chiều dọc cho các vật phẩm khác
                offsetY = (WorldConfig.TILE_SIZE - imageHeight) / 2.0 - yOffsetCorrection;
            }

            ghostPlacement.setImage(ghostImage);
            ghostPlacement.setFitWidth(imageWidth);
            ghostPlacement.setFitHeight(imageHeight);
            ghostPlacement.setLayoutX(screenX + offsetX);
            ghostPlacement.setLayoutY(screenY + offsetY);
            ghostPlacement.setVisible(true);
        } else {
            ghostPlacement.setVisible(false);
        }
    }

    // =================================================================================
    // 4. LOGIC HIỂN THỊ ĐỘNG VẬT (ANIMAL RENDERING LOGIC)
    // =================================================================================

    /**
     * Quản lý và cập nhật hiển thị cho danh sách động vật
     */
    public void updateAnimals(java.util.List<Animal> animals, double worldOffsetX, double worldOffsetY) {
        // Loại bỏ view của động vật đã chết hoặc không còn trong danh sách
        java.util.List<Animal> toRemove = new java.util.ArrayList<>();
        for (Animal animal : animalViews.keySet()) {
            if (!animals.contains(animal) || animal.isDead()) {
                toRemove.add(animal);
            }
        }
        for (Animal animal : toRemove) {
            ImageView view = animalViews.remove(animal);
            ImageView icon = animalStatusIcons.remove(animal);
            ImageView bg = animalStatusBackgrounds.remove(animal);
            if (view != null) entityPane.getChildren().remove(view);
            if (icon != null) entityPane.getChildren().remove(icon);
            if (bg != null) entityPane.getChildren().remove(bg);
        }

        long now = System.currentTimeMillis();

        // Cập nhật hoặc tạo mới view cho từng động vật
        for (Animal animal : animals) {
            if (animal.isDead()) continue;
            ImageView animalView = animalViews.get(animal);
            if (animalView == null) {
                animalView = new ImageView();
                animalView.setSmooth(false);
                animalView.setPreserveRatio(true);
                animalView.setMouseTransparent(true);
                animalViews.put(animal, animalView);
                entityPane.getChildren().add(animalView);
            }

            // Tính toán khung hình hoạt họa (frame) hiện tại
            int frameIndex = 0;

            // Trường hợp trứng: frame phụ thuộc vào biến thể loại trứng
            if (animal.getType() == com.example.farmSimulation.model.AnimalType.EGG_ENTITY) {
                frameIndex = AnimalConfig.EGG_FRAME_START_INDEX + animal.getVariant();
            }
            // Trường hợp động vật: tính toán animation dựa trên thời gian
            else {
                int frameCount = 1;
                int animationSpeedMs = 200;

                if (animal.getType() == com.example.farmSimulation.model.AnimalType.CHICKEN) {
                    if (animal.getCurrentAction() == Animal.Action.WALK) {
                        frameCount = AnimalConfig.CHICKEN_WALK_FRAMES;
                        animationSpeedMs = AnimalConfig.ANIM_SPEED_CHICKEN_WALK;
                    } else {
                        frameCount = AnimalConfig.CHICKEN_IDLE_FRAMES;
                        animationSpeedMs = AnimalConfig.ANIM_SPEED_CHICKEN_IDLE;
                    }
                } else { // Các loài động vật tiêu chuẩn khác
                    if (animal.getCurrentAction() == Animal.Action.WALK) {
                        frameCount = AnimalConfig.STANDARD_WALK_FRAMES;
                        animationSpeedMs = AnimalConfig.ANIM_SPEED_STANDARD_WALK;
                    } else {
                        frameCount = AnimalConfig.STANDARD_IDLE_FRAMES;
                        animationSpeedMs = AnimalConfig.ANIM_SPEED_STANDARD_IDLE;
                    }
                }

                frameIndex = (int) ((now / animationSpeedMs) % frameCount);
            }

            Image animalTexture = assetManager.getAnimalTexture(animal.getType(), animal.getDirection(), animal.getCurrentAction(), frameIndex);
            animalView.setImage(animalTexture);

            // Tính toán kích thước hiển thị (bao gồm tỷ lệ scale cho con non)
            double spriteSize = animal.getType().getSpriteSize();
            double scale = animal.getType().getScale();
            double displaySize = spriteSize * scale;

            animalView.setFitWidth(displaySize);
            animalView.setFitHeight(displaySize);

            double screenX = animal.getX() + worldOffsetX;
            double screenY = animal.getY() + worldOffsetY + AnimalConfig.ANIMAL_Y_OFFSET;

            // Căn chỉnh vị trí để chân động vật khớp với điểm neo logic
            animalView.setLayoutX(screenX - displaySize / 2.0);
            animalView.setLayoutY(screenY - displaySize);

            // Cập nhật icon trạng thái trên đầu con vật
            updateAnimalStatusIcon(animal, worldOffsetX, worldOffsetY);
        }
    }

    /**
     * Hiển thị icon đói hoặc sản phẩm thu hoạch trên đầu động vật
     */
    private void updateAnimalStatusIcon(Animal animal, double worldOffsetX, double worldOffsetY) {
        ImageView iconView = animalStatusIcons.get(animal);
        ImageView bgView = animalStatusBackgrounds.get(animal);
        boolean needsIcon = false;
        Image iconImage = null;

        // Ưu tiên hiển thị icon đói
        if (animal.isHungry()) {
            iconImage = assetManager.getItemIcon(com.example.farmSimulation.model.ItemType.SUPER_FEED);
            needsIcon = iconImage != null;
        }
        // Hiển thị sản phẩm nếu có (ngoại trừ Gà vì đẻ trứng ra đất)
        else if (animal.isHasProduct() && animal.getType().canProduce() && animal.getType() != AnimalType.CHICKEN) {
            iconImage = assetManager.getItemIcon(animal.getType().getProduct());
            needsIcon = iconImage != null;
        }

        if (needsIcon && iconImage != null) {
            // Đảm bảo khởi tạo và thêm nền trước icon để hiển thị đúng lớp
            if (bgView == null) {
                bgView = new ImageView();
                bgView.setImage(assetManager.getIconBG());
                bgView.setSmooth(false);
                bgView.setMouseTransparent(true);
                animalStatusBackgrounds.put(animal, bgView);
                entityPane.getChildren().add(bgView);
            }
            if (iconView == null) {
                iconView = new ImageView();
                iconView.setSmooth(false);
                iconView.setMouseTransparent(true);
                animalStatusIcons.put(animal, iconView);
                entityPane.getChildren().add(iconView);
            }

            iconView.setImage(iconImage);
            iconView.setFitWidth(HudConfig.ICON_SIZE);
            iconView.setFitHeight(HudConfig.ICON_SIZE);
            bgView.setFitWidth(HudConfig.ICON_BG_SIZE);
            bgView.setFitHeight(HudConfig.ICON_BG_SIZE);

            // Tính toán vị trí icon dựa trên kích thước thực tế của con vật
            double spriteSize = animal.getType().getSpriteSize();
            double scale = animal.getType().getScale();
            double displaySize = spriteSize * scale;

            double screenX = animal.getX() + worldOffsetX;

            // Điều chỉnh độ cao icon tùy thuộc vào loại và độ tuổi động vật
            double typeSpecificOffset = AnimalConfig.ANIMAL_ICON_Y_OFFSET;
            if (animal.getType() == AnimalType.CHICKEN) {
                typeSpecificOffset = AnimalConfig.ICON_OFFSET_CHICKEN;
            } else if (animal.getType() == AnimalType.COW) {
                typeSpecificOffset = AnimalConfig.ICON_OFFSET_COW;
            } else if (animal.getType() == AnimalType.PIG) {
                typeSpecificOffset = AnimalConfig.ICON_OFFSET_PIG;
            } else if (animal.getType() == AnimalType.SHEEP) {
                typeSpecificOffset = AnimalConfig.ICON_OFFSET_SHEEP;
            } else if (animal.isBaby()) {
                typeSpecificOffset = AnimalConfig.ICON_OFFSET_BABY;
            }

            double screenY = animal.getY() + worldOffsetY + AnimalConfig.ANIMAL_Y_OFFSET - displaySize;

            // Đặt vị trí nền
            bgView.setLayoutX(screenX - HudConfig.ICON_BG_SIZE / 2.0);
            bgView.setLayoutY(screenY - HudConfig.ICON_BG_SIZE - typeSpecificOffset);

            // Đặt vị trí icon (căn giữa trong nền)
            double iconOffset = (HudConfig.ICON_BG_SIZE - HudConfig.ICON_SIZE) / 2.0;
            iconView.setLayoutX(screenX - HudConfig.ICON_BG_SIZE / 2.0 + iconOffset);
            iconView.setLayoutY(screenY - HudConfig.ICON_BG_SIZE - typeSpecificOffset + iconOffset + AnimalConfig.ICON_CONTENT_Y_OFFSET);

            iconView.setVisible(true);
            bgView.setVisible(true);
        } else {
            if (iconView != null) iconView.setVisible(false);
            if (bgView != null) bgView.setVisible(false);
        }
    }

    // =================================================================================
    // 5. CÁC HÀM HỖ TRỢ NỘI BỘ (INTERNAL HELPERS)
    // =================================================================================

    /**
     * Tạo một đối tượng ImageView mới với các thiết lập chuẩn
     */
    private ImageView createTileView(int c, int r, double yOffset, double width, double height) {
        ImageView tileView = new ImageView();
        tileView.setFitWidth(width);
        tileView.setFitHeight(height);
        tileView.setPreserveRatio(false);
        tileView.setLayoutX(c * WorldConfig.TILE_SIZE);
        tileView.setLayoutY(r * WorldConfig.TILE_SIZE + yOffset);
        tileView.setSmooth(false);
        return tileView;
    }

    /**
     * Khởi tạo các ô debug hitbox nếu cấu hình cho phép
     */
    private void initDebugHitboxes(int r, int c) {
        if (TreeConfig.DEBUG_TREE_HITBOX && PlayerSpriteConfig.DEBUG_PLAYER_BOUNDS) {
            Rectangle treeHitbox = new Rectangle(TreeConfig.TREE_HITBOX_WIDTH, TreeConfig.TREE_HITBOX_HEIGHT);
            treeHitbox.setFill(null);
            treeHitbox.setStroke(TreeConfig.DEBUG_TREE_HITBOX_COLOR);
            treeHitbox.setStrokeWidth(2.0);
            treeHitbox.setMouseTransparent(true);
            treeHitbox.setVisible(false);
            treeHitboxes[r][c] = treeHitbox;
        } else {
            treeHitboxes[r][c] = null;
        }

        if (FenceConfig.DEBUG_FENCE_HITBOX && PlayerSpriteConfig.DEBUG_PLAYER_BOUNDS) {
            Rectangle fenceHitbox = new Rectangle(FenceConfig.FENCE_HITBOX_WIDTH, FenceConfig.FENCE_HITBOX_HEIGHT);
            fenceHitbox.setFill(null);
            fenceHitbox.setStroke(FenceConfig.DEBUG_FENCE_HITBOX_COLOR);
            fenceHitbox.setStrokeWidth(2.0);
            fenceHitbox.setMouseTransparent(true);
            fenceHitbox.setVisible(false);
            fenceHitboxes[r][c] = fenceHitbox;
        } else {
            fenceHitboxes[r][c] = null;
        }
    }

    /**
     * Thêm các ImageView vào Pane theo đúng thứ tự lớp (Z-Order)
     * Việc tách vòng lặp giúp đảm bảo lớp trên luôn che phủ lớp dưới
     */
    private void addTilesToPane() {
        // Lớp 1: Môi trường (Đất, Cây, Rào...)
        for (int r = 0; r < WorldConfig.NUM_ROWS_ON_SCREEN; r++) {
            for (int c = 0; c < WorldConfig.NUM_COLS_ON_SCREEN; c++) {
                worldPane.getChildren().addAll(
                        baseTiles[r][c],
                        overlayTiles[r][c],
                        cropTiles[r][c],
                        treeTiles[r][c],
                        fenceTiles[r][c]
                );
            }
        }

        // Lớp 2: Vật phẩm trên đất (Nằm trên cây cối của hàng dưới)
        for (int r = 0; r < WorldConfig.NUM_ROWS_ON_SCREEN; r++) {
            for (int c = 0; c < WorldConfig.NUM_COLS_ON_SCREEN; c++) {
                worldPane.getChildren().add(groundItemTiles[r][c]);
            }
        }

        // Lớp 3: UI và Debug (Lớp trên cùng)
        for (int r = 0; r < WorldConfig.NUM_ROWS_ON_SCREEN; r++) {
            for (int c = 0; c < WorldConfig.NUM_COLS_ON_SCREEN; c++) {
                worldPane.getChildren().addAll(
                        statusBackground[r][c],
                        statusIconTiles[r][c]
                );

                if (treeHitboxes[r][c] != null) worldPane.getChildren().add(treeHitboxes[r][c]);
                if (fenceHitboxes[r][c] != null) worldPane.getChildren().add(fenceHitboxes[r][c]);
            }
        }
    }

    /**
     * Cập nhật vị trí và hiển thị cho debug hitbox của cây và hàng rào
     */
    private void updateDebugHitboxVisualization(int r, int c, TileData data) {
        if (TreeConfig.DEBUG_TREE_HITBOX && PlayerSpriteConfig.DEBUG_PLAYER_BOUNDS && treeHitboxes[r][c] != null) {
            if (data.getTreeData() != null && data.getTreeData().getGrowthStage() > 0) {
                double tileLocalX = c * WorldConfig.TILE_SIZE;
                double tileLocalY = r * WorldConfig.TILE_SIZE;
                double layoutX = tileLocalX + (WorldConfig.TILE_SIZE - TreeConfig.TREE_HITBOX_WIDTH) / 2.0;
                double visualTreeBottomY = (tileLocalY + WorldConfig.TILE_SIZE) - CropConfig.CROP_Y_OFFSET;
                double layoutY = visualTreeBottomY - TreeConfig.TREE_HITBOX_HEIGHT - TreeConfig.TREE_HITBOX_Y_OFFSET_FROM_BOTTOM;

                treeHitboxes[r][c].setLayoutX(layoutX);
                treeHitboxes[r][c].setLayoutY(layoutY);
                treeHitboxes[r][c].setVisible(true);
            } else {
                treeHitboxes[r][c].setVisible(false);
            }
        }

        if (FenceConfig.DEBUG_FENCE_HITBOX && PlayerSpriteConfig.DEBUG_PLAYER_BOUNDS && fenceHitboxes[r][c] != null) {
            if (data.getFenceData() != null && data.getFenceData().isSolid()) {
                double tileLocalX = c * WorldConfig.TILE_SIZE;
                double tileLocalY = r * WorldConfig.TILE_SIZE;
                double layoutX = tileLocalX + (WorldConfig.TILE_SIZE - FenceConfig.FENCE_HITBOX_WIDTH) / 2.0;
                double layoutY = (tileLocalY + WorldConfig.TILE_SIZE) - FenceConfig.FENCE_HITBOX_HEIGHT - FenceConfig.FENCE_HITBOX_Y_OFFSET_FROM_BOTTOM;

                fenceHitboxes[r][c].setLayoutX(layoutX);
                fenceHitboxes[r][c].setLayoutY(layoutY);
                fenceHitboxes[r][c].setVisible(true);
            } else {
                fenceHitboxes[r][c].setVisible(false);
            }
        }
    }

    /**
     * Cập nhật hiển thị icon trạng thái trên các ô lưới
     */
    private void updateStatusIcons(int r, int c, TileData data) {
        Image statusIcon = assetManager.getStatusIcon(data.getStatusIndicator());

        // Điều chỉnh kích thước icon đặc biệt nếu cần hiển thị kép
        if (data.getStatusIndicator() == com.example.farmSimulation.model.CropStatusIndicator.NEED_WATER_AND_FERTILIZER) {
            double doubleWidth = HudConfig.ICON_SIZE * 2;
            this.statusIconTiles[r][c].setFitWidth(doubleWidth);
            double iconOffset = (HudConfig.ICON_BG_SIZE - doubleWidth) / 2;
            this.statusIconTiles[r][c].setLayoutX((c) * WorldConfig.TILE_SIZE + iconOffset);
        } else {
            this.statusIconTiles[r][c].setFitWidth(HudConfig.ICON_SIZE);
            double iconOffset = (HudConfig.ICON_BG_SIZE - HudConfig.ICON_SIZE) / 2;
            this.statusIconTiles[r][c].setLayoutX((c) * WorldConfig.TILE_SIZE + iconOffset);
        }

        this.statusIconTiles[r][c].setImage(statusIcon);
        this.statusBackground[r][c].setVisible(statusIcon != null);
        this.statusBackground[r][c].setLayoutX((c) * WorldConfig.TILE_SIZE);
    }
}