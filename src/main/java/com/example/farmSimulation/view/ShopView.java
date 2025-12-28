package com.example.farmSimulation.view;

import com.example.farmSimulation.config.ShopConfig;
import com.example.farmSimulation.model.ItemStack;
import com.example.farmSimulation.model.ItemType;
import com.example.farmSimulation.model.ShopManager;
import com.example.farmSimulation.model.ShopSlot;
import com.example.farmSimulation.view.assets.ImageManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Giao diện Cửa hàng (Shop UI) - Hỗ trợ chức năng Mua (từ kho hàng ngày) và Bán vật phẩm.
 */
public class ShopView extends StackPane {

    // ==============================================================================================
    // KHAI BÁO BIẾN VÀ THÀNH PHẦN GIAO DIỆN
    // ==============================================================================================

    private final ShopManager shopManager;
    private final ImageManager assetManager;

    // Các thành phần UI chính
    private final GridPane itemGrid;    // Lưới hiển thị danh sách vật phẩm
    private final Label messageLabel;   // Nhãn hiển thị thông báo trạng thái
    private final Label moneyLabel;     // Nhãn hiển thị số tiền của người chơi
    private final VBox rerollButtonBox; // Khu vực chứa nút làm mới kho hàng (vị trí cố định)

    // Trạng thái chế độ Mua/Bán
    private boolean isSellingMode = false; // false = Chế độ Mua, true = Chế độ Bán
    private Button buyTabButton;
    private Button sellTabButton;

    public ShopView(ShopManager shopManager, ImageManager assetManager) {
        this.shopManager = shopManager;
        this.assetManager = assetManager;
        this.itemGrid = new GridPane();
        this.messageLabel = new Label();
        this.moneyLabel = new Label();
        this.rerollButtonBox = new VBox(10);

        setupUI();
    }

    // ==============================================================================================
    // KHỞI TẠO VÀ THIẾT LẬP GIAO DIỆN (SETUP UI)
    // ==============================================================================================

    /**
     * Thiết lập cấu trúc giao diện Cửa hàng
     */
    private void setupUI() {
        // Thiết lập kích thước cố định cho ShopView
        this.setPrefSize(ShopConfig.SHOP_WIDTH, ShopConfig.SHOP_HEIGHT);
        this.setMaxSize(ShopConfig.SHOP_WIDTH, ShopConfig.SHOP_HEIGHT);
        this.setMinSize(ShopConfig.SHOP_WIDTH, ShopConfig.SHOP_HEIGHT);

        // Tải ảnh nền cửa hàng
        Image bgImage = new Image(getClass().getResourceAsStream(ShopConfig.SHOP_BG_PATH));
        ImageView bgView = new ImageView(bgImage);
        bgView.setFitWidth(ShopConfig.SHOP_WIDTH);
        bgView.setFitHeight(ShopConfig.SHOP_HEIGHT);
        bgView.setPreserveRatio(false); // Kéo giãn ảnh để lấp đầy kích thước

        // Tạo VBox chứa nội dung chính
        VBox contentBox = new VBox(15);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.setPadding(new Insets(ShopConfig.SHOP_PADDING));
        contentBox.setPrefSize(ShopConfig.SHOP_WIDTH, ShopConfig.SHOP_HEIGHT);

        // Tạo HBox cho phần tiêu đề (Tiêu đề + Hiển thị tiền)
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPrefWidth(ShopConfig.SHOP_WIDTH - ShopConfig.SHOP_PADDING * 2);

        // Tiêu đề Shop
        Label titleLabel = new Label("SHOP");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setStyle("-fx-effect: dropshadow(one-pass-box, black, 2, 0, 0, 1);");

        // Hiển thị tiền (Góc trên bên phải) - Thiết kế nổi bật
        moneyLabel.setFont(Font.font("Arial", FontWeight.BOLD, ShopConfig.MONEY_DISPLAY_FONT_SIZE));
        moneyLabel.setTextFill(ShopConfig.MONEY_DISPLAY_COLOR);
        moneyLabel.setStyle("-fx-effect: dropshadow(one-pass-box, black, 3, 0, 0, 2);");
        updateMoneyDisplay(); // Cập nhật số tiền ban đầu

        // Thêm khoảng cách lề trên cho nhãn tiền tệ để tránh dính sát mép trên giao diện
        HBox.setMargin(moneyLabel, new Insets(15, 0, 0, 0));

        // Sử dụng Region để đẩy nhãn tiền tệ sang phía bên phải
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(titleLabel, spacer, moneyLabel);

        // Các tab chuyển đổi chế độ Mua/Bán
        HBox tabBox = new HBox(10);
        tabBox.setAlignment(Pos.CENTER);

        buyTabButton = new Button(ShopConfig.TAB_BUY_TEXT);
        buyTabButton.setPrefSize(ShopConfig.TAB_BUTTON_WIDTH, ShopConfig.TAB_BUTTON_HEIGHT);
        buyTabButton.setFont(Font.font("Arial", FontWeight.BOLD, ShopConfig.TAB_BUTTON_FONT_SIZE));
        buyTabButton.setOnAction(e -> switchToBuyMode());

        sellTabButton = new Button(ShopConfig.TAB_SELL_TEXT);
        sellTabButton.setPrefSize(ShopConfig.TAB_BUTTON_WIDTH, ShopConfig.TAB_BUTTON_HEIGHT);
        sellTabButton.setFont(Font.font("Arial", FontWeight.BOLD, ShopConfig.TAB_BUTTON_FONT_SIZE));
        sellTabButton.setOnAction(e -> switchToSellMode());

        tabBox.getChildren().addAll(buyTabButton, sellTabButton);
        updateTabStyles(); // Cập nhật giao diện tab

        // Cấu hình lưới hiển thị vật phẩm
        itemGrid.setHgap(ShopConfig.SHOP_ITEM_SPACING);
        itemGrid.setVgap(ShopConfig.SHOP_ITEM_SPACING);
        itemGrid.setAlignment(Pos.CENTER);

        // Cấu hình nhãn thông báo
        messageLabel.setFont(Font.font("Arial", ShopConfig.MESSAGE_FONT_SIZE));
        messageLabel.setVisible(false);
        messageLabel.setAlignment(Pos.CENTER);

        // Thêm các thành phần vào contentBox
        contentBox.getChildren().addAll(headerBox, tabBox, itemGrid, messageLabel);

        // Tạo và định vị nút Làm mới (Reroll) ở góc dưới bên phải
        // Thêm trực tiếp vào StackPane, không nằm trong lớp overlay để tránh chặn thao tác chuột
        rerollButtonBox.setAlignment(Pos.CENTER);
        rerollButtonBox.setPrefSize(ShopConfig.SHOP_ITEM_SLOT_SIZE, ShopConfig.SHOP_ITEM_SLOT_SIZE);
        rerollButtonBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE); // Ngăn StackPane tự động kéo giãn hoặc căn giữa
        rerollButtonBox.setStyle("-fx-background-color: rgba(100, 50, 0, 0.7); -fx-border-color: orange; -fx-border-width: 3;");
        rerollButtonBox.setVisible(false); // Mặc định ẩn, chỉ hiện trong chế độ Mua

        // Định vị nút Reroll ở góc dưới bên phải sử dụng căn chỉnh của StackPane
        StackPane.setAlignment(rerollButtonBox, Pos.BOTTOM_RIGHT);

        // Điều chỉnh lề dưới để đẩy hộp Reroll lên vị trí mong muốn, ngang hàng với lưới vật phẩm
        StackPane.setMargin(rerollButtonBox, new Insets(0, 80, 130, 0));

        // Thêm vào StackPane (nền ở dưới cùng, nội dung ở giữa, nút reroll ở trên cùng)
        this.getChildren().addAll(bgView, contentBox, rerollButtonBox);

        // Mặc định ẩn giao diện cửa hàng khi khởi tạo
        this.setVisible(false);

        // Cập nhật danh sách vật phẩm (mặc định là chế độ Mua)
        updateItemList();
    }

    // ==============================================================================================
    // XỬ LÝ CHUYỂN ĐỔI CHẾ ĐỘ (MUA/BÁN)
    // ==============================================================================================

    /**
     * Chuyển sang chế độ Mua
     */
    private void switchToBuyMode() {
        if (isSellingMode) {
            isSellingMode = false;
            updateTabStyles();
            updateItemList();
        }
    }

    /**
     * Chuyển sang chế độ Bán
     */
    private void switchToSellMode() {
        if (!isSellingMode) {
            isSellingMode = true;
            updateTabStyles();
            updateItemList();
        }
    }

    /**
     * Cập nhật giao diện hiển thị cho các nút tab Mua/Bán
     */
    private void updateTabStyles() {
        if (isSellingMode) {
            // Đang ở chế độ Bán
            buyTabButton.setStyle(getTabButtonStyle(false));
            sellTabButton.setStyle(getTabButtonStyle(true));
        } else {
            // Đang ở chế độ Mua
            buyTabButton.setStyle(getTabButtonStyle(true));
            sellTabButton.setStyle(getTabButtonStyle(false));
        }
    }

    /**
     * Tạo chuỗi định dạng CSS cho nút tab dựa trên trạng thái kích hoạt
     */
    private String getTabButtonStyle(boolean isActive) {
        Paint bgColor = isActive ? ShopConfig.TAB_ACTIVE_BG_COLOR : ShopConfig.TAB_INACTIVE_BG_COLOR;
        return String.format("-fx-background-color: #%02X%02X%02X; -fx-text-fill: white; -fx-font-size: %.0fpx; -fx-border-width: 2px; -fx-border-color: white;",
                (int)(((Color)bgColor).getRed() * 255),
                (int)(((Color)bgColor).getGreen() * 255),
                (int)(((Color)bgColor).getBlue() * 255),
                ShopConfig.TAB_BUTTON_FONT_SIZE);
    }

    // ==============================================================================================
    // CẬP NHẬT DỮ LIỆU VÀ HIỂN THỊ DANH SÁCH (DATA UPDATES)
    // ==============================================================================================

    /**
     * Cập nhật danh sách vật phẩm hiển thị trong cửa hàng (theo chế độ Mua hoặc Bán)
     */
    public void updateItemList() {
        itemGrid.getChildren().clear();

        int col = 0;
        int row = 0;

        if (!isSellingMode) {
            // Chế độ Mua: Hiển thị vật phẩm từ kho hàng hàng ngày
            List<ShopSlot> dailyStock = shopManager.getCurrentDailyStock();
            for (int slotIndex = 0; slotIndex < dailyStock.size(); slotIndex++) {
                ShopSlot slot = dailyStock.get(slotIndex);
                VBox itemBox = createBuyItemBox(slot, slotIndex);
                itemGrid.add(itemBox, col, row);

                col++;
                if (col >= ShopConfig.SHOP_GRID_COLS) {
                    col = 0;
                    row++;
                }
            }

            // Nút Reroll được định vị riêng (không nằm trong lưới) - chỉ hiện ở chế độ Mua
            updateRerollButton(true);
        } else {
            // Chế độ Bán: Hiển thị vật phẩm từ thanh công cụ (Hotbar) của người chơi
            ItemStack[] hotbarItems = shopManager.getPlayer().getHotbarItems();
            for (int slotIndex = 0; slotIndex < hotbarItems.length; slotIndex++) {
                ItemStack stack = hotbarItems[slotIndex];
                if (stack != null && stack.getQuantity() > 0) {
                    ItemType itemType = stack.getItemType();
                    // Chỉ hiển thị các vật phẩm có giá bán lớn hơn 0
                    if (itemType.getSellPrice() > 0) {
                        VBox itemBox = createSellItemBox(stack, slotIndex);
                        itemGrid.add(itemBox, col, row);

                        col++;
                        if (col >= ShopConfig.SHOP_GRID_COLS) {
                            col = 0;
                            row++;
                        }
                    }
                }
            }

            // Ẩn nút Reroll trong chế độ Bán
            updateRerollButton(false);
        }
    }

    /**
     * Cập nhật hiển thị và nội dung của nút Làm mới (Reroll)
     * @param visible true để hiển thị (Chế độ Mua), false để ẩn (Chế độ Bán)
     */
    private void updateRerollButton(boolean visible) {
        rerollButtonBox.setVisible(visible);
        rerollButtonBox.setManaged(visible);

        if (visible && rerollButtonBox.getChildren().isEmpty()) {
            // Tạo nội dung nút Reroll nếu chưa có
            // Icon Reroll - Sử dụng icon tiền tệ từ hệ thống GUI
            ImageView rerollIcon = new ImageView();
            Image moneyIconImage = assetManager.getGuiIcon("MONEY");
            if (moneyIconImage != null) {
                rerollIcon.setImage(moneyIconImage);
            }
            rerollIcon.setFitWidth(48);
            rerollIcon.setFitHeight(48);
            rerollIcon.setPreserveRatio(true);

            // Nhãn Reroll
            Label rerollLabel = new Label("Reroll");
            rerollLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            rerollLabel.setTextFill(Color.WHITE);

            // Nhãn giá tiền
            Label priceLabel = new Label("$" + ShopConfig.REROLL_PRICE);
            priceLabel.setFont(Font.font("Arial", FontWeight.BOLD, ShopConfig.PRICE_FONT_SIZE));
            priceLabel.setTextFill(Color.GOLD);

            // Nút bấm Reroll
            Button rerollButton = new Button("Reroll");
            rerollButton.setPrefSize(ShopConfig.BUTTON_WIDTH, ShopConfig.BUTTON_HEIGHT);
            rerollButton.setStyle(getRerollButtonStyle());

            // Xử lý sự kiện click nút Reroll
            rerollButton.setOnAction(e -> {
                String result = shopManager.rerollStock();
                if (result == null) {
                    showMessage("Shop stock refreshed!", ShopConfig.SUCCESS_TEXT_COLOR);
                    updateMoneyDisplay();
                    updateItemList(); // Làm mới lưới vật phẩm với dữ liệu mới
                } else {
                    showMessage(result, ShopConfig.ERROR_TEXT_COLOR);
                }
            });

            rerollButtonBox.getChildren().addAll(rerollIcon, rerollLabel, priceLabel, rerollButton);
        }
    }

    /**
     * Cập nhật hiển thị số tiền của người chơi
     */
    public void updateMoneyDisplay() {
        double money = shopManager.getPlayer().getMoney();
        moneyLabel.setText("$" + (int)money);
    }

    // ==============================================================================================
    // TẠO CÁC PHẦN TỬ GIAO DIỆN (ITEM RENDERING)
    // ==============================================================================================

    /**
     * Tạo VBox hiển thị một vật phẩm trong chế độ Mua (từ kho hàng ngày)
     */
    private VBox createBuyItemBox(ShopSlot slot, int shopSlotIndex) {
        ItemType itemType = slot.getItemType();
        boolean isSoldOut = slot.isSoldOut();

        // Container chính sử dụng StackPane để cho phép xếp chồng các lớp giao diện (như nhãn SALE)
        StackPane containerPane = new StackPane();
        containerPane.setPrefSize(ShopConfig.SHOP_ITEM_SLOT_SIZE, ShopConfig.SHOP_ITEM_SLOT_SIZE);
        containerPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4); -fx-border-color: gray; -fx-border-width: 2;"); // Giảm độ mờ nền để làm nổi bật icon

        // VBox nội dung - đệm đều cho tất cả các mục
        VBox itemBox = new VBox(8);
        itemBox.setAlignment(Pos.CENTER);
        itemBox.setPadding(new Insets(5));
        itemBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE); // Ngăn việc kéo giãn để giữ bố cục đồng nhất

        // Icon vật phẩm
        ImageView iconView = new ImageView();
        Image itemIcon = assetManager.getItemIcon(itemType);
        if (itemIcon != null) {
            iconView.setImage(itemIcon);
        }
        iconView.setFitWidth(ShopConfig.ITEM_ICON_SIZE);
        iconView.setFitHeight(ShopConfig.ITEM_ICON_SIZE);
        iconView.setPreserveRatio(true);

        // Tên vật phẩm
        Label nameLabel = new Label(itemType.getName());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(ShopConfig.SHOP_ITEM_SLOT_SIZE - 10);
        nameLabel.setAlignment(Pos.CENTER);

        // Nhãn số lượng
        Label qtyLabel = new Label("Quantity: " + slot.getQuantity());
        qtyLabel.setFont(Font.font("Arial", 10));
        qtyLabel.setTextFill(Color.LIGHTGRAY);

        // Hiển thị giá (có giảm giá nếu đang SALE)
        HBox priceBox = createBuyPriceBox(slot);

        // Nút mua
        Button buyButton = new Button(isSoldOut ? "SOLD OUT" : "Buy");
        buyButton.setPrefSize(ShopConfig.BUTTON_WIDTH, ShopConfig.BUTTON_HEIGHT);
        buyButton.setStyle(getButtonStyle());
        buyButton.setDisable(isSoldOut); // Vô hiệu hóa nếu đã hết hàng

        // Làm mờ ô nếu hết hàng
        if (isSoldOut) {
            itemBox.setOpacity(0.5);
        }

        // Xử lý sự kiện click nút mua
        buyButton.setOnAction(e -> {
            String result = shopManager.buyItem(shopSlotIndex, 1);
            if (result == null) {
                showMessage("Bought " + itemType.getName() + "!", ShopConfig.SUCCESS_TEXT_COLOR);
                updateMoneyDisplay();
                updateItemList(); // Làm mới để hiển thị số lượng cập nhật
                // Đồng bộ Hotbar ngay sau khi mua thành công
                if (shopManager.getPlayer().getMainGameView() != null) {
                    shopManager.getPlayer().getMainGameView().updateHotbar();
                }
            } else {
                // Hiển thị thông báo lỗi
                showMessage(result, ShopConfig.ERROR_TEXT_COLOR);
            }
        });

        // Nút Mua tất cả - tính toán số lượng tối đa người chơi có thể mua
        Button buyAllButton = null;
        if (!isSoldOut) {
            int maxBuyable = calculateMaxBuyable(slot, shopManager.getPlayer().getMoney());
            if (maxBuyable > 1) {
                buyAllButton = new Button("Buy All");
                buyAllButton.setPrefSize(ShopConfig.BUTTON_WIDTH, ShopConfig.BUTTON_HEIGHT);
                buyAllButton.setStyle(getButtonStyle());

                buyAllButton.setOnAction(e -> {
                    String result = shopManager.buyItem(shopSlotIndex, maxBuyable);
                    if (result == null) {
                        showMessage("Bought " + maxBuyable + "x " + itemType.getName() + "!", ShopConfig.SUCCESS_TEXT_COLOR);
                        updateMoneyDisplay();
                        updateItemList();
                        if (shopManager.getPlayer().getMainGameView() != null) {
                            shopManager.getPlayer().getMainGameView().updateHotbar();
                        }
                    } else {
                        showMessage(result, ShopConfig.ERROR_TEXT_COLOR);
                    }
                });
            }
        }

        if (buyAllButton != null) {
            itemBox.getChildren().addAll(iconView, nameLabel, qtyLabel, priceBox, buyButton, buyAllButton);
        } else {
            itemBox.getChildren().addAll(iconView, nameLabel, qtyLabel, priceBox, buyButton);
        }

        // Thêm itemBox vào containerPane và căn giữa
        StackPane.setAlignment(itemBox, Pos.CENTER);
        containerPane.getChildren().add(itemBox);

        // Nhãn SALE đè lên góc trên bên phải (nếu đang giảm giá) - không làm lệch nội dung bên dưới
        if (slot.isOnSale() && !isSoldOut) {
            Label saleTag = new Label("SALE -" + (int)(slot.getDiscountRate() * 100) + "%");
            saleTag.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            saleTag.setTextFill(Color.WHITE); // Chữ trắng
            saleTag.setStyle("-fx-background-color: rgba(0, 200, 0, 0.9); -fx-padding: 4px 8px;"); // Nền xanh lá
            saleTag.setAlignment(Pos.CENTER);
            saleTag.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            // Định vị ở góc trên bên phải như một lớp phủ
            StackPane.setAlignment(saleTag, Pos.TOP_RIGHT);
            saleTag.setTranslateX(-5); // Dịch chuyển nhỏ từ mép phải
            saleTag.setTranslateY(5); // Dịch chuyển nhỏ từ mép trên
            containerPane.getChildren().add(saleTag); // Thêm sau itemBox để hiển thị đè lên trên
        }

        // Lớp phủ SOLD OUT (nếu hết hàng)
        if (isSoldOut) {
            Label soldOutLabel = new Label("SOLD OUT");
            soldOutLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            soldOutLabel.setTextFill(Color.WHITE);
            soldOutLabel.setStyle("-fx-background-color: rgba(200, 0, 0, 0.9); -fx-padding: 5px;");
            containerPane.getChildren().add(soldOutLabel);
        }

        return new VBox(containerPane);
    }

    /**
     * Tạo hộp giá cho chế độ Mua (hiển thị giá đã giảm nếu có khuyến mãi)
     */
    private HBox createBuyPriceBox(ShopSlot slot) {
        HBox priceBox = new HBox(5);
        priceBox.setAlignment(Pos.CENTER);
        priceBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 10; -fx-padding: 2 8 2 8;"); // Tạo khung nền bo tròn

        // Icon tiền tệ từ hệ thống GUI
        ImageView moneyIconView = new ImageView();
        Image moneyIconImage = assetManager.getGuiIcon("MONEY");
        if (moneyIconImage != null) {
            moneyIconView.setImage(moneyIconImage);
        }
        moneyIconView.setFitWidth(ShopConfig.COIN_ICON_SIZE);
        moneyIconView.setFitHeight(ShopConfig.COIN_ICON_SIZE);
        moneyIconView.setPreserveRatio(true);

        // Nhãn giá (màu xanh nếu giảm giá, màu vàng nếu giá thường)
        Label priceLabel = new Label(String.valueOf((int)slot.getPrice()));
        priceLabel.setFont(Font.font("Arial", FontWeight.BOLD, ShopConfig.PRICE_FONT_SIZE));

        if (slot.isOnSale()) {
            priceLabel.setTextFill(Color.GREEN);
        } else {
            priceLabel.setTextFill(ShopConfig.PRICE_TEXT_COLOR);
        }
        priceLabel.setStyle("-fx-effect: dropshadow(one-pass-box, black, 1, 0, 0, 0);");

        priceBox.getChildren().addAll(moneyIconView, priceLabel);

        return priceBox;
    }

    /**
     * Tạo VBox hiển thị một vật phẩm trong chế độ Bán (từ kho đồ người chơi)
     */
    private VBox createSellItemBox(ItemStack stack, int slotIndex) {
        ItemType itemType = stack.getItemType();
        int quantity = stack.getQuantity();

        // Container chính - Sử dụng StackPane để đồng bộ cấu trúc với Tab Mua
        StackPane containerPane = new StackPane();
        containerPane.setPrefSize(ShopConfig.SHOP_ITEM_SLOT_SIZE, ShopConfig.SHOP_ITEM_SLOT_SIZE);
        containerPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4); -fx-border-color: gray; -fx-border-width: 2;");

        // VBox nội dung - đệm đều
        VBox itemBox = new VBox(8);
        itemBox.setAlignment(Pos.CENTER);
        itemBox.setPadding(new Insets(5));
        itemBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // Icon vật phẩm
        ImageView iconView = new ImageView();
        Image itemIcon = assetManager.getItemIcon(itemType);
        if (itemIcon != null) {
            iconView.setImage(itemIcon);
        }
        iconView.setFitWidth(ShopConfig.ITEM_ICON_SIZE);
        iconView.setFitHeight(ShopConfig.ITEM_ICON_SIZE);
        iconView.setPreserveRatio(true);

        // Tên vật phẩm + số lượng (đảm bảo xuống dòng và căn giữa)
        Label nameLabel = new Label(itemType.getName() + " x" + quantity);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(ShopConfig.SHOP_ITEM_SLOT_SIZE - 10);
        nameLabel.setAlignment(Pos.CENTER);

        // Giá bán kèm icon tiền
        HBox priceBox = createPriceBox(itemType.getSellPrice());

        // Nút bán
        Button sellButton = new Button("Sell");
        sellButton.setPrefSize(ShopConfig.BUTTON_WIDTH, ShopConfig.BUTTON_HEIGHT);
        sellButton.setStyle(getButtonStyle());

        // Xử lý sự kiện click nút bán
        sellButton.setOnAction(e -> {
            String result = shopManager.sellItem(slotIndex, 1);
            if (result == null) {
                showMessage("Sold " + itemType.getName() + "!", ShopConfig.SUCCESS_TEXT_COLOR);
                updateMoneyDisplay();
                updateItemList(); // Làm mới lưới để cập nhật số lượng
                // Đồng bộ Hotbar ngay sau khi bán thành công
                if (shopManager.getPlayer().getMainGameView() != null) {
                    shopManager.getPlayer().getMainGameView().updateHotbar();
                }
            } else {
                showMessage(result, ShopConfig.ERROR_TEXT_COLOR);
            }
        });

        // Nút Bán tất cả
        Button sellAllButton = null;
        if (quantity > 1) {
            sellAllButton = new Button("Sell All");
            sellAllButton.setPrefSize(ShopConfig.BUTTON_WIDTH, ShopConfig.BUTTON_HEIGHT);
            sellAllButton.setStyle(getButtonStyle());

            sellAllButton.setOnAction(e -> {
                String result = shopManager.sellItem(slotIndex, quantity);
                if (result == null) {
                    showMessage("Sold " + quantity + "x " + itemType.getName() + "!", ShopConfig.SUCCESS_TEXT_COLOR);
                    updateMoneyDisplay();
                    updateItemList();
                    if (shopManager.getPlayer().getMainGameView() != null) {
                        shopManager.getPlayer().getMainGameView().updateHotbar();
                    }
                } else {
                    showMessage(result, ShopConfig.ERROR_TEXT_COLOR);
                }
            });
        }

        // Thêm các thành phần vào itemBox
        if (sellAllButton != null) {
            itemBox.getChildren().addAll(iconView, nameLabel, priceBox, sellButton, sellAllButton);
        } else {
            itemBox.getChildren().addAll(iconView, nameLabel, priceBox, sellButton);
        }

        // Thêm itemBox vào containerPane và căn giữa
        StackPane.setAlignment(itemBox, Pos.CENTER);
        containerPane.getChildren().add(itemBox);

        return new VBox(containerPane);
    }

    /**
     * Tạo HBox hiển thị giá kèm icon tiền (dùng cho chế độ Bán)
     */
    private HBox createPriceBox(int price) {
        HBox priceBox = new HBox(5);
        priceBox.setAlignment(Pos.CENTER);
        priceBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 10; -fx-padding: 2 8 2 8;");

        // Icon tiền tệ
        ImageView moneyIconView = new ImageView();
        Image moneyIconImage = assetManager.getGuiIcon("MONEY");
        if (moneyIconImage != null) {
            moneyIconView.setImage(moneyIconImage);
        }
        moneyIconView.setFitWidth(ShopConfig.COIN_ICON_SIZE);
        moneyIconView.setFitHeight(ShopConfig.COIN_ICON_SIZE);
        moneyIconView.setPreserveRatio(true);

        // Văn bản giá tiền
        Label priceLabel = new Label(String.valueOf(price));
        priceLabel.setFont(Font.font("Arial", FontWeight.BOLD, ShopConfig.PRICE_FONT_SIZE));
        priceLabel.setTextFill(ShopConfig.PRICE_TEXT_COLOR);
        priceLabel.setStyle("-fx-effect: dropshadow(one-pass-box, black, 1, 0, 0, 0);");

        priceBox.getChildren().addAll(moneyIconView, priceLabel);

        return priceBox;
    }

    // ==============================================================================================
    // CÁC HÀM TIỆN ÍCH (UTILITIES & HELPERS)
    // ==============================================================================================

    /**
     * Tính toán số lượng vật phẩm tối đa người chơi có thể mua dựa trên số tiền hiện có và lượng tồn kho
     * @param slot Slot hàng hóa cần mua
     * @param playerMoney Số tiền hiện tại của người chơi
     * @return Số lượng tối đa có thể mua
     */
    private int calculateMaxBuyable(ShopSlot slot, double playerMoney) {
        int stock = slot.getQuantity();
        double price = slot.getPrice();
        if (price <= 0) return 0;

        int maxAffordable = (int)(playerMoney / price);
        return Math.min(stock, maxAffordable);
    }

    /**
     * Tạo chuỗi định dạng CSS cho nút thông thường
     */
    private String getButtonStyle() {
        return String.format("-fx-background-color: #%02X%02X%02X; -fx-text-fill: white; -fx-font-size: %.0fpx;",
                (int)(((Color)ShopConfig.BUTTON_BG_COLOR).getRed() * 255),
                (int)(((Color)ShopConfig.BUTTON_BG_COLOR).getGreen() * 255),
                (int)(((Color)ShopConfig.BUTTON_BG_COLOR).getBlue() * 255),
                ShopConfig.BUTTON_FONT_SIZE);
    }

    /**
     * Tạo chuỗi định dạng CSS cho nút Reroll (màu khác biệt để dễ nhận biết)
     */
    private String getRerollButtonStyle() {
        return String.format("-fx-background-color: #%02X%02X%02X; -fx-text-fill: white; -fx-font-size: %.0fpx; -fx-font-weight: bold;",
                255, 140, 0, // Màu cam
                ShopConfig.BUTTON_FONT_SIZE);
    }

    /**
     * Hiển thị thông báo trạng thái
     */
    private void showMessage(String message, Paint color) {
        messageLabel.setText(message);
        messageLabel.setTextFill(color);
        messageLabel.setVisible(true);

        // Ẩn thông báo sau 3 giây
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(3)
        );
        pause.setOnFinished(e -> messageLabel.setVisible(false));
        pause.play();
    }

    /**
     * Bật/Tắt hiển thị cửa hàng
     */
    public void toggle() {
        boolean currentVisibility = this.isVisible();
        this.setVisible(!currentVisibility);
        if (!currentVisibility) {
            updateMoneyDisplay(); // Cập nhật tiền khi mở cửa hàng
            updateItemList(); // Cập nhật danh sách vật phẩm khi mở
            // Đảm bảo cửa hàng luôn nằm trên cùng khi được mở
            this.toFront();
        }
    }

    /**
     * Kiểm tra xem cửa hàng có đang hiển thị hay không
     */
    public boolean isShopVisible() {
        return this.isVisible();
    }
}