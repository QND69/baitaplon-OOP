package com.example.farmSimulation.view;

import com.example.farmSimulation.config.HotbarConfig;
import com.example.farmSimulation.config.WindowConfig;
import com.example.farmSimulation.model.CropType;
import com.example.farmSimulation.model.ItemStack;
import com.example.farmSimulation.model.ItemType;
import com.example.farmSimulation.model.Player;
import com.example.farmSimulation.view.assets.ImageManager;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Lớp giao diện quản lý thanh công cụ (Hotbar) của người chơi.
 * Chịu trách nhiệm hiển thị các ô chứa vật phẩm, xử lý sự kiện kéo thả,
 * và cập nhật giao diện dựa trên dữ liệu từ Player.
 */
public class HotbarView extends Pane {

    // ==============================================================================================
    // KHAI BÁO BIẾN VÀ CẤU HÌNH (FIELDS)
    // ==============================================================================================

    private final Player player;
    private final ImageManager assetManager;

    // Mảng chứa các ô slot, mỗi slot là một StackPane để xếp chồng các lớp (Nền, Icon, Text...)
    private final StackPane[] slots;

    // Hình chữ nhật hiển thị ô đang được chọn
    private final Rectangle slotSelector;

    // Bản đồ lưu trữ hình ảnh texture cho từng loại vật phẩm để truy xuất nhanh
    private final Map<ItemType, Image> itemTextureMap;

    // Nhãn hiển thị tên vật phẩm đang cầm trên tay (nằm giữa HUD, phía trên hotbar)
    private final Text itemNameLabel;

    // Biến lưu trữ tỉ lệ thu phóng hiện tại của hotbar (cập nhật từ Cài đặt)
    private double currentScale = HotbarConfig.DEFAULT_HOTBAR_SCALE;

    // --- Các biến phục vụ chức năng Kéo và Thả (Drag & Drop) ---

    // Hàm gọi lại để xử lý logic hoán đổi vị trí trong GameManager (tham số: slotA, slotB)
    private BiConsumer<Integer, Integer> onSwapListener;

    // Hàm gọi lại để xử lý logic thả vật phẩm ra ngoài hoặc vào thùng rác
    // Tham số: slotIndex, tọa độ thả. Trả về: true nếu thả vào thùng rác
    private java.util.function.BiFunction<Integer, javafx.geometry.Point2D, Boolean> onItemDropListener;

    // Biểu tượng mờ di chuyển theo chuột khi kéo
    private ImageView ghostIcon;

    // Chỉ số slot bắt đầu kéo (-1 là không kéo)
    private int dragSourceIndex = -1;

    // Tọa độ neo chuột để tính toán độ lệch khi kéo hình ảnh
    private double mouseAnchorX, mouseAnchorY;

    // ==============================================================================================
    // KHỞI TẠO (CONSTRUCTOR)
    // ==============================================================================================

    public HotbarView(Player player, ImageManager assetManager) {
        this.player = player;
        this.assetManager = assetManager;
        this.itemTextureMap = new EnumMap<>(ItemType.class);
        this.slots = new StackPane[HotbarConfig.HOTBAR_SLOT_COUNT];

        // Khởi tạo Ghost Icon (mặc định ẩn và không nhận sự kiện chuột)
        this.ghostIcon = new ImageView();
        this.ghostIcon.setMouseTransparent(true);
        this.ghostIcon.setVisible(false);
        // Ghost icon sẽ được thêm vào Pane cuối cùng để đảm bảo nó luôn nằm trên các thành phần khác

        // Tải trước hình ảnh cho các vật phẩm
        loadItemTextures(assetManager);

        // Khởi tạo các ô slot
        for (int i = 0; i < HotbarConfig.HOTBAR_SLOT_COUNT; i++) {
            StackPane slot = new StackPane();
            slot.setAlignment(Pos.CENTER); // Căn giữa nội dung trong slot

            // 1. Lớp Nền
            Rectangle bg = new Rectangle();
            bg.setFill(HotbarConfig.SLOT_BACKGROUND_COLOR);
            bg.setStroke(HotbarConfig.SLOT_BORDER_COLOR);

            // 2. Lớp Icon vật phẩm
            ImageView icon = new ImageView();

            // Thiết lập các bộ xử lý sự kiện chuột cho icon (Kéo & Thả)
            setupDragHandlers(icon, i);

            // 3. Thanh nền độ bền (Ẩn mặc định)
            Rectangle durBg = new Rectangle();
            durBg.setFill(HotbarConfig.DURABILITY_BG_COLOR);
            durBg.setVisible(false);

            // 4. Thanh hiển thị mức độ bền (Ẩn mặc định)
            Rectangle durBar = new Rectangle();
            durBar.setFill(HotbarConfig.DURABILITY_COLOR_HIGH);
            durBar.setVisible(false);

            // 5. Số thứ tự phím tắt (Góc trái trên)
            int keyNum = (i + 1) % 10; // Chuyển đổi: 1->9, 10->0
            Text keyLabel = new Text(String.valueOf(keyNum));
            keyLabel.setFont(HotbarConfig.HOTBAR_NUMBER_FONT);
            keyLabel.setFill(HotbarConfig.HOTBAR_TEXT_COLOR);
            keyLabel.setStroke(HotbarConfig.HOTBAR_TEXT_STROKE_COLOR);
            keyLabel.setStrokeWidth(HotbarConfig.HOTBAR_TEXT_STROKE_WIDTH);

            // 6. Số lượng vật phẩm (Góc phải dưới)
            Text qtyLabel = new Text("");
            qtyLabel.setFont(HotbarConfig.HOTBAR_QUANTITY_FONT);
            qtyLabel.setFill(HotbarConfig.HOTBAR_TEXT_COLOR);
            qtyLabel.setStroke(HotbarConfig.HOTBAR_TEXT_STROKE_COLOR);
            qtyLabel.setStrokeWidth(HotbarConfig.HOTBAR_TEXT_STROKE_WIDTH);

            // Thêm tất cả vào slot theo thứ tự vẽ (Layer dưới -> trên)
            slot.getChildren().addAll(bg, icon, durBg, durBar, keyLabel, qtyLabel);
            this.slots[i] = slot;
            this.getChildren().add(slot);
        }

        // Khởi tạo khung chọn slot (Selector)
        this.slotSelector = new Rectangle();
        this.slotSelector.setFill(null);
        this.slotSelector.setStroke(HotbarConfig.SLOT_SELECTED_BORDER_COLOR);
        this.getChildren().add(slotSelector);

        // Khởi tạo nhãn tên vật phẩm
        this.itemNameLabel = new Text("");
        this.itemNameLabel.setFont(HotbarConfig.HOTBAR_ITEM_NAME_FONT);
        this.itemNameLabel.setFill(HotbarConfig.HOTBAR_TEXT_COLOR);
        this.itemNameLabel.setStroke(HotbarConfig.HOTBAR_TEXT_STROKE_COLOR);
        this.itemNameLabel.setStrokeWidth(HotbarConfig.HOTBAR_TEXT_STROKE_WIDTH);
        this.itemNameLabel.setVisible(false);
        this.getChildren().add(itemNameLabel);

        // Thêm ghost icon vào view (Layer trên cùng)
        this.getChildren().add(ghostIcon);

        // Cập nhật bố cục và hiển thị lần đầu
        updateLayout(HotbarConfig.DEFAULT_HOTBAR_SCALE);
        updateView();
    }

    // ==============================================================================================
    // CÁC HÀM THIẾT LẬP CALLBACK (CALLBACK SETTERS)
    // ==============================================================================================

    public void setOnSwapListener(BiConsumer<Integer, Integer> listener) {
        this.onSwapListener = listener;
    }

    /**
     * Thiết lập hàm xử lý khi thả vật phẩm.
     * Hàm này nhận vào (chỉ số slot nguồn, tọa độ thả) và trả về true nếu thả vào thùng rác.
     */
    public void setOnItemDropListener(java.util.function.BiFunction<Integer, javafx.geometry.Point2D, Boolean> listener) {
        this.onItemDropListener = listener;
    }

    // ==============================================================================================
    // XỬ LÝ KÉO VÀ THẢ (DRAG & DROP LOGIC)
    // ==============================================================================================

    /**
     * Thiết lập toàn bộ logic kéo thả cho hình ảnh vật phẩm.
     */
    private void setupDragHandlers(ImageView icon, int slotIndex) {
        // 1. Xử lý khi bắt đầu nhấn chuột (Mouse Pressed)
        icon.setOnMousePressed(e -> {
            // Chặn thao tác nếu game đang tạm dừng
            if (player.getMainGameView() != null && player.getMainGameView().getGameManager() != null
                    && player.getMainGameView().getGameManager().isPaused()) {
                return;
            }

            // Chỉ bắt đầu kéo nếu có vật phẩm và dùng chuột trái
            if (icon.getImage() != null && e.isPrimaryButtonDown()) {
                dragSourceIndex = slotIndex;

                // Lưu tọa độ chuột hiện tại để tính toán độ lệch di chuyển mượt mà
                mouseAnchorX = e.getX();
                mouseAnchorY = e.getY();

                // Cấu hình Ghost Icon (Icon bóng ma bay theo chuột)
                ghostIcon.setImage(icon.getImage());
                ghostIcon.setFitWidth(icon.getFitWidth());
                ghostIcon.setFitHeight(icon.getFitHeight());

                // Chuyển đổi tọa độ chuột từ hệ tọa độ Scene sang hệ tọa độ cục bộ của HotbarView
                Point2D scenePoint = new Point2D(e.getSceneX(), e.getSceneY());
                Point2D localPoint = this.sceneToLocal(scenePoint);

                // Đặt vị trí ban đầu cho ghost icon sao cho tâm trùng với chuột
                ghostIcon.setLayoutX(localPoint.getX() - (ghostIcon.getFitWidth() / 2));
                ghostIcon.setLayoutY(localPoint.getY() - (ghostIcon.getFitHeight() / 2));

                ghostIcon.setVisible(true);
                ghostIcon.setOpacity(0.7); // Làm mờ ghost icon

                // Làm mờ icon gốc để tạo hiệu ứng đang được nhấc lên
                icon.setOpacity(0.3);
                e.consume();
            }
        });

        // 2. Xử lý khi di chuyển chuột (Mouse Dragged)
        icon.setOnMouseDragged(e -> {
            // Chặn thao tác nếu game đang tạm dừng
            if (player.getMainGameView() != null && player.getMainGameView().getGameManager() != null
                    && player.getMainGameView().getGameManager().isPaused()) {
                return;
            }

            if (dragSourceIndex != -1 && e.isPrimaryButtonDown()) {
                // Cập nhật vị trí Ghost Icon liên tục theo chuột
                Point2D scenePoint = new Point2D(e.getSceneX(), e.getSceneY());
                Point2D localPoint = this.sceneToLocal(scenePoint);

                ghostIcon.setLayoutX(localPoint.getX() - (ghostIcon.getFitWidth() / 2));
                ghostIcon.setLayoutY(localPoint.getY() - (ghostIcon.getFitHeight() / 2));
                e.consume();
            }
        });

        // 3. Xử lý khi thả chuột (Mouse Released)
        icon.setOnMouseReleased(e -> {
            // Nếu game đang tạm dừng, hủy thao tác kéo thả và reset trạng thái
            if (player.getMainGameView() != null && player.getMainGameView().getGameManager() != null
                    && player.getMainGameView().getGameManager().isPaused()) {
                if (dragSourceIndex != -1) {
                    icon.setOpacity(1.0);
                    ghostIcon.setVisible(false);
                    dragSourceIndex = -1;
                }
                return;
            }

            if (dragSourceIndex != -1) {
                // Khôi phục hiển thị của icon gốc
                icon.setOpacity(1.0);

                // Ẩn ghost icon
                ghostIcon.setVisible(false);

                // Xác định slot đích dựa trên vị trí thả chuột
                int targetIndex = -1;
                Point2D scenePoint = new Point2D(e.getSceneX(), e.getSceneY());
                Point2D localPoint = this.sceneToLocal(scenePoint);

                // Kiểm tra xem chuột có nằm trong slot nào không
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i].getBoundsInParent().contains(localPoint)) {
                        targetIndex = i;
                        break;
                    }
                }

                // Trường hợp 1: Thả ra ngoài hotbar (có thể là thùng rác hoặc ném đồ)
                if (targetIndex == -1 && onItemDropListener != null) {
                    // Gọi callback để kiểm tra xem có phải thả vào thùng rác không
                    Boolean isTrash = onItemDropListener.apply(dragSourceIndex, scenePoint);
                    if (isTrash != null && isTrash) {
                        // Nếu là thùng rác, callback đã xử lý việc xóa item, không cần làm gì thêm ở đây
                    }
                }
                // Trường hợp 2: Thả vào một slot khác trên hotbar
                else if (targetIndex != -1 && targetIndex != dragSourceIndex) {
                    // Thực hiện hoán đổi vị trí item
                    if (onSwapListener != null) {
                        onSwapListener.accept(dragSourceIndex, targetIndex);
                    }
                }

                dragSourceIndex = -1; // Reset trạng thái
                e.consume();
            }
        });
    }

    // ==============================================================================================
    // BỐ CỤC VÀ HIỂN THỊ (LAYOUT & RENDERING)
    // ==============================================================================================

    /**
     * Tính toán và cập nhật lại toàn bộ kích thước, vị trí các phần tử dựa trên tỉ lệ thu phóng (scale).
     * Được gọi khi khởi tạo hoặc khi người chơi thay đổi cài đặt kích thước giao diện.
     */
    public void updateLayout(double scale) {
        this.currentScale = scale;

        // Tính toán kích thước động dựa trên scale
        double currentSlotSize = HotbarConfig.BASE_SLOT_SIZE * scale;
        double currentSpacing = HotbarConfig.BASE_SLOT_SPACING * scale;
        double currentStrokeWidth = HotbarConfig.BASE_STROKE_WIDTH * scale;
        double barHeight = HotbarConfig.DURABILITY_BAR_HEIGHT * scale;
        double currentItemSize = currentSlotSize * HotbarConfig.ITEM_SCALE_RATIO;

        // Cập nhật từng slot
        for (int i = 0; i < HotbarConfig.HOTBAR_SLOT_COUNT; i++) {
            double x = i * (currentSlotSize + currentSpacing);

            StackPane slot = slots[i];
            slot.setLayoutX(x);
            slot.setLayoutY(0);
            slot.setPrefSize(currentSlotSize, currentSlotSize);

            // Cập nhật Nền slot
            Rectangle bg = (Rectangle) slot.getChildren().get(0);
            bg.setWidth(currentSlotSize);
            bg.setHeight(currentSlotSize);
            bg.setStrokeWidth(currentStrokeWidth);

            // Cập nhật kích thước Icon
            ImageView icon = (ImageView) slot.getChildren().get(1);
            icon.setFitWidth(currentItemSize);
            icon.setFitHeight(currentItemSize);
            icon.setTranslateY(HotbarConfig.ICON_Y_TRANSLATE * scale);

            // Tính toán vị trí thanh độ bền
            double maxBarWidth = currentSlotSize * HotbarConfig.DURABILITY_BAR_WIDTH_RATIO;
            double sidePadding = (currentSlotSize - maxBarWidth) / 2;
            double barYOffset = HotbarConfig.DURABILITY_BAR_Y_OFFSET * scale;

            // Cập nhật nền thanh độ bền
            Rectangle durBg = (Rectangle) slot.getChildren().get(2);
            durBg.setHeight(barHeight);
            durBg.setWidth(maxBarWidth);
            StackPane.setAlignment(durBg, Pos.BOTTOM_LEFT);
            durBg.setTranslateX(sidePadding);
            durBg.setTranslateY(barYOffset);

            // Cập nhật thanh độ bền
            Rectangle durBar = (Rectangle) slot.getChildren().get(3);
            durBar.setHeight(barHeight);
            StackPane.setAlignment(durBar, Pos.BOTTOM_LEFT);
            durBar.setTranslateX(sidePadding);
            durBar.setTranslateY(barYOffset);

            // Cập nhật vị trí số thứ tự phím tắt
            Text keyLabel = (Text) slot.getChildren().get(4);
            StackPane.setAlignment(keyLabel, Pos.TOP_LEFT);
            keyLabel.setTranslateX(HotbarConfig.HOTBAR_TEXT_PADDING * scale);
            keyLabel.setTranslateY(HotbarConfig.HOTBAR_TEXT_PADDING * scale);

            // Cập nhật vị trí số lượng
            Text qtyLabel = (Text) slot.getChildren().get(5);
            StackPane.setAlignment(qtyLabel, Pos.BOTTOM_RIGHT);
            qtyLabel.setTranslateX(-HotbarConfig.HOTBAR_TEXT_PADDING * scale);
            qtyLabel.setTranslateY(-HotbarConfig.HOTBAR_TEXT_PADDING * scale);
        }

        // Cập nhật kích thước khung chọn
        this.slotSelector.setWidth(currentSlotSize);
        this.slotSelector.setHeight(currentSlotSize);
        this.slotSelector.setStrokeWidth(currentStrokeWidth);
        updateSelectorPosition();

        // Căn giữa toàn bộ thanh Hotbar trên màn hình
        double totalWidth = (HotbarConfig.HOTBAR_SLOT_COUNT * currentSlotSize) + ((HotbarConfig.HOTBAR_SLOT_COUNT - 1) * currentSpacing);
        double yOffset = HotbarConfig.BASE_Y_OFFSET * scale;

        this.setLayoutX((WindowConfig.SCREEN_WIDTH - totalWidth) / 2);
        this.setLayoutY(WindowConfig.SCREEN_HEIGHT - currentSlotSize - yOffset);

        // Cập nhật font và vị trí của nhãn tên vật phẩm
        double itemNameFontSize = HotbarConfig.BASE_ITEM_NAME_FONT_SIZE * scale;
        itemNameLabel.setFont(Font.font("Arial", FontWeight.BOLD, itemNameFontSize));
        itemNameLabel.setStrokeWidth(HotbarConfig.HOTBAR_TEXT_STROKE_WIDTH * scale);

        double itemNameY = -HotbarConfig.ITEM_NAME_Y_OFFSET * scale;
        itemNameLabel.setY(itemNameY);
        itemNameLabel.setTextOrigin(javafx.geometry.VPos.BASELINE);
    }

    /**
     * Cập nhật trạng thái hiển thị của các slot dựa trên dữ liệu inventory của người chơi.
     * Được gọi khi inventory thay đổi hoặc khi đổi slot chọn.
     */
    public void updateView() {
        ItemStack[] items = player.getHotbarItems();
        double slotSize = HotbarConfig.BASE_SLOT_SIZE * currentScale;
        double maxBarWidth = slotSize * HotbarConfig.DURABILITY_BAR_WIDTH_RATIO;

        // Cập nhật nhãn tên vật phẩm đang cầm
        ItemStack currentItem = player.getCurrentItem();
        if (currentItem != null) {
            itemNameLabel.setText(currentItem.getItemType().getName());
            itemNameLabel.setVisible(true);

            // Tính toán lại vị trí để căn giữa nhãn
            double totalWidth = (HotbarConfig.HOTBAR_SLOT_COUNT * slotSize) + ((HotbarConfig.HOTBAR_SLOT_COUNT - 1) * (HotbarConfig.BASE_SLOT_SPACING * currentScale));
            double labelWidth = itemNameLabel.getLayoutBounds().getWidth();
            itemNameLabel.setX((totalWidth - labelWidth) / 2);
        } else {
            itemNameLabel.setVisible(false);
        }

        for (int i = 0; i < HotbarConfig.HOTBAR_SLOT_COUNT; i++) {
            ItemStack stack = items[i];
            ImageView icon = (ImageView) slots[i].getChildren().get(1);
            Rectangle durBg = (Rectangle) slots[i].getChildren().get(2);
            Rectangle durBar = (Rectangle) slots[i].getChildren().get(3);
            Text qtyLabel = (Text) slots[i].getChildren().get(5);

            if (stack != null) {
                icon.setImage(itemTextureMap.get(stack.getItemType()));
                icon.setVisible(true);

                // Xử lý hiển thị thanh độ bền
                if (HotbarConfig.SHOW_DURABILITY_BAR && stack.getItemType().hasDurability()) {
                    durBg.setVisible(true);
                    durBar.setVisible(true);

                    double ratio = (double) stack.getCurrentDurability() / stack.getItemType().getMaxDurability();
                    durBar.setWidth(maxBarWidth * ratio);

                    // Đổi màu thanh độ bền (Xanh -> Vàng -> Đỏ)
                    if (ratio > 0.5) durBar.setFill(HotbarConfig.DURABILITY_COLOR_HIGH);
                    else if (ratio > 0.2) durBar.setFill(HotbarConfig.DURABILITY_COLOR_MEDIUM);
                    else durBar.setFill(HotbarConfig.DURABILITY_COLOR_LOW);
                } else {
                    durBg.setVisible(false);
                    durBar.setVisible(false);
                }

                // Xử lý hiển thị số lượng vật phẩm
                if (stack.getItemType().isStackable()) {
                    qtyLabel.setText(String.valueOf(stack.getQuantity()));
                    qtyLabel.setVisible(true);
                } else {
                    qtyLabel.setVisible(false);
                }
            } else {
                // Slot trống
                icon.setImage(null);
                icon.setVisible(false);
                qtyLabel.setVisible(false);
                durBg.setVisible(false);
                durBar.setVisible(false);
            }
        }
        updateSelectorPosition();
    }

    /**
     * Di chuyển khung chọn đến đúng vị trí slot đang được active.
     */
    private void updateSelectorPosition() {
        double currentSlotSize = HotbarConfig.BASE_SLOT_SIZE * this.currentScale;
        double currentSpacing = HotbarConfig.BASE_SLOT_SPACING * this.currentScale;

        int selectedSlot = player.getSelectedHotbarSlot();
        double selectorX = selectedSlot * (currentSlotSize + currentSpacing);
        slotSelector.setLayoutX(selectorX);
    }

    // ==============================================================================================
    // TẢI TÀI NGUYÊN & TIỆN ÍCH (ASSET LOADING & HELPERS)
    // ==============================================================================================

    /**
     * Tải và cache toàn bộ hình ảnh vật phẩm từ AssetManager vào map nội bộ.
     */
    private void loadItemTextures(ImageManager assetManager) {
        // Tận dụng hàm getItemIcon tập trung của ImageManager để lấy hình ảnh chuẩn
        ItemType[] basicItems = {
                ItemType.HOE, ItemType.WATERING_CAN, ItemType.PICKAXE, ItemType.SHOVEL,
                ItemType.FERTILIZER, ItemType.AXE, ItemType.SWORD, ItemType.SHEARS,
                ItemType.MILK_BUCKET, ItemType.FULL_MILK_BUCKET, ItemType.MEAT_CHICKEN,
                ItemType.MEAT_COW, ItemType.MEAT_PIG, ItemType.MEAT_SHEEP, ItemType.EGG,
                ItemType.WOOL, ItemType.ENERGY_DRINK, ItemType.SUPER_FEED, ItemType.WOOD,
                ItemType.ITEM_COW, ItemType.ITEM_CHICKEN, ItemType.ITEM_SHEEP, ItemType.ITEM_PIG
        };

        for (ItemType type : basicItems) {
            cacheItemSprite(type, assetManager.getItemIcon(type));
        }

        // Tải ảnh các loại hạt giống
        cacheItemSprite(ItemType.SEEDS_STRAWBERRY, assetManager.getSeedIcon(CropType.STRAWBERRY));
        cacheItemSprite(ItemType.SEEDS_DAIKON, assetManager.getSeedIcon(CropType.DAIKON));
        cacheItemSprite(ItemType.SEEDS_POTATO, assetManager.getSeedIcon(CropType.POTATO));
        cacheItemSprite(ItemType.SEEDS_CARROT, assetManager.getSeedIcon(CropType.CARROT));
        cacheItemSprite(ItemType.SEEDS_WATERMELON, assetManager.getSeedIcon(CropType.WATERMELON));
        cacheItemSprite(ItemType.SEEDS_TOMATO, assetManager.getSeedIcon(CropType.TOMATO));
        cacheItemSprite(ItemType.SEEDS_WHEAT, assetManager.getSeedIcon(CropType.WHEAT));
        cacheItemSprite(ItemType.SEEDS_CORN, assetManager.getSeedIcon(CropType.CORN));
        cacheItemSprite(ItemType.SEEDS_TREE, assetManager.getTreeSeedIcon());

        // Tải ảnh các sản phẩm thu hoạch
        cacheItemSprite(ItemType.STRAWBERRY, assetManager.getHarvestIcon(CropType.STRAWBERRY));
        cacheItemSprite(ItemType.DAIKON, assetManager.getHarvestIcon(CropType.DAIKON));
        cacheItemSprite(ItemType.POTATO, assetManager.getHarvestIcon(CropType.POTATO));
        cacheItemSprite(ItemType.CARROT, assetManager.getHarvestIcon(CropType.CARROT));
        cacheItemSprite(ItemType.WATERMELON, assetManager.getHarvestIcon(CropType.WATERMELON));
        cacheItemSprite(ItemType.TOMATO, assetManager.getHarvestIcon(CropType.TOMATO));
        cacheItemSprite(ItemType.WHEAT, assetManager.getHarvestIcon(CropType.WHEAT));
        cacheItemSprite(ItemType.CORN, assetManager.getHarvestIcon(CropType.CORN));
    }

    /**
     * Lưu trữ hình ảnh vào map nội bộ và đồng bộ lại với ImageManager.
     */
    private void cacheItemSprite(ItemType type, Image icon) {
        if (icon != null) {
            itemTextureMap.put(type, icon);
            assetManager.cacheItemIcon(type, icon);
        }
    }

    /**
     * Xác định xem chuột có đang nằm trên slot nào của hotbar không.
     * Sử dụng tọa độ cục bộ của HotbarView.
     * @return Chỉ số slot (0-9) hoặc -1 nếu không tìm thấy.
     */
    public int getSlotIndexFromMouse(double mouseX, double mouseY) {
        double slotSize = HotbarConfig.BASE_SLOT_SIZE * currentScale;
        double spacing = HotbarConfig.BASE_SLOT_SPACING * currentScale;

        // Kiểm tra biên theo chiều dọc
        if (mouseY < 0 || mouseY > slotSize) {
            return -1;
        }

        // Kiểm tra biên theo chiều ngang để tìm slot
        for (int i = 0; i < HotbarConfig.HOTBAR_SLOT_COUNT; i++) {
            double slotX = i * (slotSize + spacing);
            if (mouseX >= slotX && mouseX < slotX + slotSize) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Tính toán tọa độ tâm tuyệt đối (Global/Screen coordinates) của một slot.
     * Dùng để hỗ trợ các hiệu ứng hình ảnh bay từ slot này ra ngoài thế giới.
     */
    public Point2D getSlotCenter(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= HotbarConfig.HOTBAR_SLOT_COUNT) return null;

        double currentSlotSize = HotbarConfig.BASE_SLOT_SIZE * this.currentScale;
        double currentSpacing = HotbarConfig.BASE_SLOT_SPACING * this.currentScale;

        // Tọa độ X cục bộ
        double slotLocalX = slotIndex * (currentSlotSize + currentSpacing);

        // Chuyển đổi sang tọa độ toàn cục bằng cách cộng thêm vị trí của HotbarView trên màn hình
        double centerX = this.getLayoutX() + slotLocalX + (currentSlotSize / 2);
        double centerY = this.getLayoutY() + (currentSlotSize / 2);

        return new Point2D(centerX, centerY);
    }

    public double getCurrentScale() {
        return currentScale;
    }
}