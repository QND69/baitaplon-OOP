package com.example.farmSimulation.view;

import com.example.farmSimulation.config.GameLogicConfig;
import com.example.farmSimulation.config.HudConfig;
import com.example.farmSimulation.config.WindowConfig;
import com.example.farmSimulation.config.WorldConfig;
import com.example.farmSimulation.model.GameManager;
import com.example.farmSimulation.view.assets.ImageManager;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Lớp HudView quản lý lớp giao diện hiển thị thông tin trên màn hình (Heads-Up Display).
 * Bao gồm các thanh trạng thái, đồng hồ, thông tin tiền tệ và các nút chức năng.
 */
public class HudView extends Pane {

    // ==============================================================================================
    // KHAI BÁO BIẾN (FIELDS)
    // ==============================================================================================

    // Tham chiếu đến các thành phần quản lý logic và tài nguyên
    private GameManager gameManager;
    private MainGameView mainGameView;
    private ImageManager assetManager;

    // Cài đặt độ sáng màn hình (từ 0.0 đến 1.0)
    private double brightness = GameLogicConfig.DEFAULT_BRIGHTNESS;

    // --- Khu vực Góc Trái-Trên: Chỉ số Người chơi ---
    private final Rectangle levelRectangle; // Khung nền hiển thị cấp độ
    private final Label levelLabel;         // Nhãn hiển thị số cấp độ
    private final Rectangle xpBarBg;        // Nền thanh kinh nghiệm
    private final Rectangle xpBarFill;      // Thanh kinh nghiệm hiện tại
    private final Rectangle staminaBarBg;   // Nền thanh thể lực
    private final Rectangle staminaBarFill; // Thanh thể lực hiện tại

    // Biểu tượng cho Kinh nghiệm và Thể lực
    private ImageView expIconView;
    private ImageView staminaIconView;

    // Hiển thị Tiền tệ
    private final Label moneyLabel;
    private final HBox moneyContainer; // Container chứa biểu tượng và số tiền
    private ImageView moneyIcon;

    // Hiển thị Tọa độ
    private final Label coordsLabel;
    private final HBox coordsContainer; // Container nền mờ cho tọa độ
    private boolean showCoordinates = HudConfig.DEFAULT_SHOW_COORDINATES;

    // --- Khu vực Góc Phải-Trên & Các nút chức năng ---
    private final Label dayLabel;   // Nhãn hiển thị Ngày
    private final Label timerLabel; // Nhãn hiển thị Giờ
    private final StackPane weatherIconPane;
    private ImageView weatherIcon;

    // Các nút chức năng (Icon Button)
    private final StackPane shopIconButtonPane;
    private ImageView shopIconButton;

    private final StackPane questIconButtonPane;
    private ImageView questIconButton;

    private final StackPane settingsIconButtonPane;
    private ImageView settingsIconButton;

    private final StackPane trashIconButtonPane; // Thùng rác (để xóa item)
    private ImageView trashIconButton;

    // --- Các lớp phủ và Hiệu ứng ---
    private final Rectangle darknessOverlay; // Lớp phủ màu đen tạo hiệu ứng ngày đêm
    private final Text temporaryText;        // Text hiển thị thông báo tạm thời
    private SequentialTransition temporaryTextAnimation; // Animation cho text tạm thời

    // ==============================================================================================
    // KHỞI TẠO (CONSTRUCTOR)
    // ==============================================================================================

    public HudView() {
        double currentY = HudConfig.HUD_TOP_LEFT_Y;

        // 1. Khởi tạo các thành phần Góc Trái-Trên (Level, Stats)

        // Khung hiển thị Level dạng thẻ bo tròn
        levelRectangle = new Rectangle(HudConfig.HUD_TOP_LEFT_X, currentY,
                HudConfig.LEVEL_RECTANGLE_WIDTH, HudConfig.LEVEL_RECTANGLE_HEIGHT);
        levelRectangle.setFill(HudConfig.LEVEL_BG_COLOR);
        levelRectangle.setStroke(Color.WHITE);
        levelRectangle.setStrokeWidth(2);
        levelRectangle.setArcWidth(HudConfig.LEVEL_RECTANGLE_CORNER_RADIUS * 2);
        levelRectangle.setArcHeight(HudConfig.LEVEL_RECTANGLE_CORNER_RADIUS * 2);
        levelRectangle.setMouseTransparent(true);

        levelLabel = new Label(HudConfig.LEVEL_TEXT_PREFIX + "1");
        levelLabel.setLayoutX(HudConfig.HUD_TOP_LEFT_X);
        levelLabel.setLayoutY(currentY);
        levelLabel.setPrefSize(HudConfig.LEVEL_RECTANGLE_WIDTH, HudConfig.LEVEL_RECTANGLE_HEIGHT);
        levelLabel.setStyle("-fx-font-size: " + HudConfig.LEVEL_FONT_SIZE + "px; -fx-text-fill: white; -fx-alignment: center; -fx-font-weight: bold;");
        levelLabel.setTextAlignment(TextAlignment.CENTER);
        levelLabel.setAlignment(Pos.CENTER);
        levelLabel.setMouseTransparent(true);

        currentY += HudConfig.LEVEL_RECTANGLE_HEIGHT + HudConfig.HUD_ELEMENT_SPACING;

        // Thanh Kinh nghiệm (XP)
        expIconView = new ImageView();
        expIconView.setFitWidth(HudConfig.GUI_ICON_SIZE);
        expIconView.setFitHeight(HudConfig.GUI_ICON_SIZE);
        expIconView.setLayoutX(HudConfig.HUD_TOP_LEFT_X);
        expIconView.setLayoutY(currentY);

        // Tính toán vị trí thanh bar để căn giữa theo chiều dọc so với icon
        double barOffsetX = HudConfig.GUI_ICON_SIZE + HudConfig.BAR_OFFSET_X;
        double xpBarCenterOffsetY = (HudConfig.GUI_ICON_SIZE - HudConfig.XP_BAR_HEIGHT) / 2.0;

        xpBarBg = new Rectangle(HudConfig.HUD_TOP_LEFT_X + barOffsetX, currentY + xpBarCenterOffsetY, HudConfig.XP_BAR_WIDTH, HudConfig.XP_BAR_HEIGHT);
        xpBarBg.setFill(HudConfig.XP_BAR_BG_COLOR);
        xpBarBg.setStroke(Color.BLACK);
        xpBarBg.setStrokeWidth(1);
        xpBarBg.setMouseTransparent(true);

        xpBarFill = new Rectangle(HudConfig.HUD_TOP_LEFT_X + barOffsetX, currentY + xpBarCenterOffsetY, 0, HudConfig.XP_BAR_HEIGHT);
        xpBarFill.setFill(HudConfig.XP_BAR_FILL_COLOR);
        xpBarFill.setMouseTransparent(true);

        currentY += HudConfig.GUI_ICON_SIZE + HudConfig.HUD_ELEMENT_SPACING;

        // Thanh Thể lực (Stamina)
        staminaIconView = new ImageView();
        staminaIconView.setFitWidth(HudConfig.GUI_ICON_SIZE);
        staminaIconView.setFitHeight(HudConfig.GUI_ICON_SIZE);
        staminaIconView.setLayoutX(HudConfig.HUD_TOP_LEFT_X);
        staminaIconView.setLayoutY(currentY);

        double staminaBarCenterOffsetY = (HudConfig.GUI_ICON_SIZE - HudConfig.STAMINA_BAR_HEIGHT) / 2.0;

        staminaBarBg = new Rectangle(HudConfig.HUD_TOP_LEFT_X + barOffsetX, currentY + staminaBarCenterOffsetY, HudConfig.STAMINA_BAR_WIDTH, HudConfig.STAMINA_BAR_HEIGHT);
        staminaBarBg.setFill(HudConfig.STAMINA_BAR_BG_COLOR);
        staminaBarBg.setStroke(Color.BLACK);
        staminaBarBg.setStrokeWidth(1);
        staminaBarBg.setMouseTransparent(true);

        staminaBarFill = new Rectangle(HudConfig.HUD_TOP_LEFT_X + barOffsetX, currentY + staminaBarCenterOffsetY, 0, HudConfig.STAMINA_BAR_HEIGHT);
        staminaBarFill.setFill(HudConfig.STAMINA_BAR_FULL_COLOR);
        staminaBarFill.setMouseTransparent(true);

        currentY += HudConfig.GUI_ICON_SIZE + HudConfig.HUD_ELEMENT_SPACING;

        // Hiển thị Tiền tệ
        moneyContainer = new HBox(HudConfig.MONEY_CONTAINER_SPACING);
        moneyContainer.setAlignment(Pos.CENTER_LEFT);
        moneyContainer.setStyle(HudConfig.MONEY_CONTAINER_STYLE);
        moneyContainer.setLayoutX(HudConfig.HUD_TOP_LEFT_X);
        moneyContainer.setLayoutY(currentY);
        moneyContainer.setMouseTransparent(true);

        moneyIcon = new ImageView();
        moneyIcon.setFitWidth(HudConfig.MONEY_ICON_SIZE);
        moneyIcon.setFitHeight(HudConfig.MONEY_ICON_SIZE);
        moneyIcon.setPreserveRatio(true);
        moneyContainer.getChildren().add(moneyIcon);

        moneyLabel = new Label(HudConfig.MONEY_PREFIX + "0");
        moneyLabel.setStyle(HudConfig.MONEY_TEXT_STYLE);
        moneyLabel.setMouseTransparent(true);
        moneyContainer.getChildren().add(moneyLabel);

        // Hiển thị Tọa độ (Ngay dưới phần Tiền)
        coordsContainer = new HBox();
        coordsContainer.setAlignment(Pos.CENTER_LEFT);
        coordsContainer.setStyle(HudConfig.COORDS_CONTAINER_STYLE);
        coordsContainer.setLayoutX(HudConfig.HUD_TOP_LEFT_X);
        coordsContainer.setLayoutY(currentY + HudConfig.COORDS_OFFSET_Y_FROM_MONEY);
        coordsContainer.setMouseTransparent(true);
        coordsContainer.setVisible(HudConfig.DEFAULT_SHOW_COORDINATES);

        coordsLabel = new Label(HudConfig.COORDS_DEFAULT_TEXT);
        coordsLabel.setStyle(HudConfig.COORDS_TEXT_STYLE);
        coordsContainer.getChildren().add(coordsLabel);

        // 2. Khởi tạo các thành phần Góc Phải-Trên (Cài đặt, Thời gian, Thời tiết)

        // Tính toán vị trí X để căn lề phải
        double iconRadius = HudConfig.ICON_BUTTON_SIZE / 2;
        double settingsIconCenterX = WindowConfig.SCREEN_WIDTH - HudConfig.HUD_TOP_RIGHT_MARGIN - iconRadius;
        currentY = HudConfig.HUD_TOP_RIGHT_Y;

        // Nút Cài đặt (Settings)
        settingsIconButtonPane = new StackPane();
        settingsIconButtonPane.setLayoutX(settingsIconCenterX - iconRadius);
        settingsIconButtonPane.setLayoutY(currentY);
        settingsIconButtonPane.setPrefSize(HudConfig.ICON_BUTTON_SIZE, HudConfig.ICON_BUTTON_SIZE);

        settingsIconButton = new ImageView();
        settingsIconButton.setFitWidth(HudConfig.ICON_BUTTON_SIZE);
        settingsIconButton.setFitHeight(HudConfig.ICON_BUTTON_SIZE);
        settingsIconButton.setPreserveRatio(true);
        settingsIconButtonPane.getChildren().add(settingsIconButton);

        settingsIconButtonPane.setOnMouseClicked(this::onSettingsIconClicked);

        currentY += HudConfig.ICON_BUTTON_SIZE + HudConfig.HUD_TOP_RIGHT_ELEMENT_SPACING;

        // Nhãn Ngày (Day)
        dayLabel = new Label(HudConfig.DAY_DEFAULT_TEXT);
        dayLabel.setStyle(HudConfig.DAY_STYLE_CSS);
        dayLabel.setPrefWidth(HudConfig.TIMER_LABEL_WIDTH);
        dayLabel.setAlignment(Pos.CENTER_RIGHT);
        dayLabel.setLayoutX(WindowConfig.SCREEN_WIDTH - HudConfig.HUD_TOP_RIGHT_MARGIN - HudConfig.TIMER_LABEL_WIDTH);
        dayLabel.setLayoutY(currentY);
        dayLabel.setMouseTransparent(true);

        currentY += HudConfig.DAY_TIME_SPACING_Y;

        // Nhãn Thời gian (Time)
        timerLabel = new Label(HudConfig.TIME_DEFAULT_TEXT);
        timerLabel.setStyle(HudConfig.TIME_STYLE_CSS);
        timerLabel.setPrefWidth(HudConfig.TIMER_LABEL_WIDTH);
        timerLabel.setAlignment(Pos.CENTER_RIGHT);
        timerLabel.setLayoutX(WindowConfig.SCREEN_WIDTH - HudConfig.HUD_TOP_RIGHT_MARGIN - HudConfig.TIMER_LABEL_WIDTH);
        timerLabel.setLayoutY(currentY);
        timerLabel.setMouseTransparent(true);

        currentY += HudConfig.TIME_WEATHER_SPACING_Y;

        // Biểu tượng Thời tiết
        double weatherIconRadius = HudConfig.WEATHER_ICON_SIZE / 2;
        double weatherIconCenterX = WindowConfig.SCREEN_WIDTH - HudConfig.HUD_TOP_RIGHT_MARGIN - weatherIconRadius;
        weatherIconPane = new StackPane();
        weatherIconPane.setLayoutX(weatherIconCenterX - weatherIconRadius);
        weatherIconPane.setLayoutY(currentY);
        weatherIconPane.setPrefSize(HudConfig.WEATHER_ICON_SIZE, HudConfig.WEATHER_ICON_SIZE);
        weatherIconPane.setMouseTransparent(true);

        weatherIcon = new ImageView();
        weatherIcon.setFitWidth(HudConfig.WEATHER_ICON_SIZE);
        weatherIcon.setFitHeight(HudConfig.WEATHER_ICON_SIZE);
        weatherIcon.setPreserveRatio(true);
        weatherIconPane.getChildren().add(weatherIcon);

        currentY += HudConfig.WEATHER_ICON_SIZE + HudConfig.HUD_TOP_RIGHT_ELEMENT_SPACING;

        // Nút Nhiệm vụ (Quest)
        double questIconRadius = HudConfig.ICON_BUTTON_SIZE / 2;
        double questIconCenterX = WindowConfig.SCREEN_WIDTH - HudConfig.HUD_TOP_RIGHT_MARGIN - questIconRadius;

        questIconButtonPane = new StackPane();
        questIconButtonPane.setLayoutX(questIconCenterX - questIconRadius);
        questIconButtonPane.setLayoutY(currentY);
        questIconButtonPane.setPrefSize(HudConfig.ICON_BUTTON_SIZE, HudConfig.ICON_BUTTON_SIZE);

        questIconButton = new ImageView();
        questIconButton.setFitWidth(HudConfig.ICON_BUTTON_SIZE);
        questIconButton.setFitHeight(HudConfig.ICON_BUTTON_SIZE);
        questIconButton.setPreserveRatio(true);
        questIconButtonPane.getChildren().add(questIconButton);

        questIconButtonPane.setOnMouseClicked(e -> {
            // Chặn tương tác nếu game đang tạm dừng
            if (gameManager != null && gameManager.isPaused()) return;

            if (mainGameView != null) {
                mainGameView.toggleQuestBoard();
            }
        });

        // 3. Khởi tạo các nút Góc Dưới (Cửa hàng, Thùng rác)

        double shopIconRadius = HudConfig.ICON_BUTTON_SIZE / 2;
        double shopIconCenterX = WindowConfig.SCREEN_WIDTH - HudConfig.HUD_BOTTOM_RIGHT_MARGIN - shopIconRadius;
        double shopIconCenterY = WindowConfig.SCREEN_HEIGHT - HudConfig.HUD_BOTTOM_RIGHT_MARGIN - shopIconRadius;

        // Nút Cửa hàng (Shop) - Góc Dưới Phải
        shopIconButtonPane = new StackPane();
        shopIconButtonPane.setLayoutX(shopIconCenterX - shopIconRadius);
        shopIconButtonPane.setLayoutY(shopIconCenterY - shopIconRadius);
        shopIconButtonPane.setPrefSize(HudConfig.ICON_BUTTON_SIZE, HudConfig.ICON_BUTTON_SIZE);

        shopIconButton = new ImageView();
        shopIconButton.setFitWidth(HudConfig.ICON_BUTTON_SIZE);
        shopIconButton.setFitHeight(HudConfig.ICON_BUTTON_SIZE);
        shopIconButton.setPreserveRatio(true);
        shopIconButtonPane.getChildren().add(shopIconButton);

        shopIconButtonPane.setOnMouseClicked(this::onShopIconClicked);

        // Nút Thùng rác (Trash) - Góc Dưới Trái
        double trashIconX = HudConfig.HUD_TOP_LEFT_X;
        double trashIconY = WindowConfig.SCREEN_HEIGHT - HudConfig.HUD_BOTTOM_RIGHT_MARGIN - HudConfig.ICON_BUTTON_SIZE;

        trashIconButtonPane = new StackPane();
        trashIconButtonPane.setLayoutX(trashIconX);
        trashIconButtonPane.setLayoutY(trashIconY);
        trashIconButtonPane.setPrefSize(HudConfig.ICON_BUTTON_SIZE, HudConfig.ICON_BUTTON_SIZE);

        trashIconButton = new ImageView();
        trashIconButton.setFitWidth(HudConfig.ICON_BUTTON_SIZE);
        trashIconButton.setFitHeight(HudConfig.ICON_BUTTON_SIZE);
        trashIconButton.setPreserveRatio(true);
        trashIconButtonPane.getChildren().add(trashIconButton);
        // Thùng rác không nhận sự kiện click, chỉ dùng để kéo thả

        // 4. Khởi tạo các thành phần phụ trợ (Text tạm, Màn hình tối)
        temporaryText = new Text();
        temporaryText.setFont(HudConfig.TEMP_TEXT_FONT);
        temporaryText.setFill(HudConfig.TEMP_TEXT_COLOR);
        temporaryText.setStroke(HudConfig.TEMP_TEXT_STROKE_COLOR);
        temporaryText.setStrokeWidth(HudConfig.TEMP_TEXT_STROKE_WIDTH);
        temporaryText.setOpacity(0);
        temporaryText.setManaged(false);

        // Lớp phủ màu đen
        this.darknessOverlay = new Rectangle(WindowConfig.SCREEN_WIDTH, WindowConfig.SCREEN_HEIGHT);
        this.darknessOverlay.setFill(Color.BLACK);
        this.darknessOverlay.setOpacity(0.0);
        this.darknessOverlay.setMouseTransparent(true);

        // Thêm tất cả vào pane theo thứ tự hiển thị (z-index)
        // Lớp phủ tối nằm dưới cùng để không che khuất các icon HUD
        this.getChildren().add(darknessOverlay);

        this.getChildren().addAll(
                levelRectangle, levelLabel,
                expIconView, xpBarBg, xpBarFill,
                staminaIconView, staminaBarBg, staminaBarFill,
                moneyContainer,
                coordsContainer,
                dayLabel, timerLabel, weatherIconPane, questIconButtonPane,
                shopIconButtonPane,
                trashIconButtonPane,
                settingsIconButtonPane,
                temporaryText
        );

        this.setMouseTransparent(false); // Đảm bảo HUD nhận được sự kiện chuột
    }

    // ==============================================================================================
    // THIẾT LẬP PHỤ THUỘC (DEPENDENCY INJECTION)
    // ==============================================================================================

    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void setMainGameView(MainGameView mainGameView) {
        this.mainGameView = mainGameView;
    }

    /**
     * Thiết lập AssetManager và tải các icon giao diện.
     */
    public void setAssetManager(ImageManager assetManager) {
        this.assetManager = assetManager;
        updateGuiIcons();
    }

    /**
     * Tải và cập nhật hình ảnh cho các icon giao diện từ ImageManager.
     */
    private void updateGuiIcons() {
        if (assetManager == null) return;

        Image moneyIconImage = assetManager.getGuiIcon("MONEY");
        if (moneyIconImage != null && moneyIcon != null) {
            moneyIcon.setImage(moneyIconImage);
        }

        Image settingsIconImage = assetManager.getGuiIcon("SETTINGS");
        if (settingsIconImage != null && settingsIconButton != null) {
            settingsIconButton.setImage(settingsIconImage);
        }

        Image shopIconImage = assetManager.getGuiIcon("SHOP");
        if (shopIconImage != null && shopIconButton != null) {
            shopIconButton.setImage(shopIconImage);
        }

        Image trashIconImage = assetManager.getGuiIcon("TRASH");
        if (trashIconImage != null && trashIconButton != null) {
            trashIconButton.setImage(trashIconImage);
        }

        Image questIconImage = assetManager.getGuiIcon("QUEST");
        if (questIconImage != null && questIconButton != null) {
            questIconButton.setImage(questIconImage);
        }

        Image staminaIconImage = assetManager.getGuiIcon("STAMINA");
        if (staminaIconImage != null && staminaIconView != null) {
            staminaIconView.setImage(staminaIconImage);
        }

        Image expIconImage = assetManager.getGuiIcon("EXP");
        if (expIconImage != null && expIconView != null) {
            expIconView.setImage(expIconImage);
        }
    }

    // ==============================================================================================
    // XỬ LÝ SỰ KIỆN (EVENT HANDLERS)
    // ==============================================================================================

    /**
     * Xử lý sự kiện click vào biểu tượng Cửa hàng.
     */
    private void onShopIconClicked(MouseEvent e) {
        if (gameManager != null && gameManager.isPaused()) return;

        if (mainGameView != null) {
            mainGameView.toggleShop();
        }
    }

    /**
     * Xử lý sự kiện click vào biểu tượng Cài đặt.
     */
    private void onSettingsIconClicked(MouseEvent e) {
        if (gameManager != null) {
            gameManager.toggleSettingsMenu();
        }
    }

    // ==============================================================================================
    // CẬP NHẬT GIAO DIỆN (UI UPDATES)
    // ==============================================================================================

    /**
     * Cập nhật các chỉ số người chơi như Level, XP, Thể lực và Tọa độ.
     */
    public void updatePlayerStats() {
        if (gameManager == null || gameManager.getMainPlayer() == null) return;

        var player = gameManager.getMainPlayer();

        // Cập nhật Level
        levelLabel.setText(HudConfig.LEVEL_TEXT_PREFIX + player.getLevel());

        // Cập nhật Thanh Kinh nghiệm
        double xpProgress = 0.0;
        if (player.getXpToNextLevel() > 0) {
            xpProgress = Math.min(1.0, player.getCurrentXP() / player.getXpToNextLevel());
        }
        xpBarFill.setWidth(HudConfig.XP_BAR_WIDTH * xpProgress);

        // Cập nhật Thanh Thể lực
        double staminaProgress = 0.0;
        if (player.getMaxStamina() > 0) {
            staminaProgress = Math.min(1.0, player.getCurrentStamina() / player.getMaxStamina());
        }
        staminaBarFill.setWidth(HudConfig.STAMINA_BAR_WIDTH * staminaProgress);

        // Đổi màu thanh thể lực dựa trên phần trăm còn lại (Xanh -> Vàng -> Đỏ)
        double percentage = staminaProgress;
        Color staminaColor;
        if (percentage > HudConfig.STAMINA_THRESHOLD_HIGH) {
            staminaColor = HudConfig.STAMINA_COLOR_HIGH;
        } else if (percentage > HudConfig.STAMINA_THRESHOLD_MEDIUM) {
            staminaColor = HudConfig.STAMINA_COLOR_MEDIUM;
        } else {
            staminaColor = HudConfig.STAMINA_COLOR_LOW;
        }
        staminaBarFill.setFill(staminaColor);

        // Cập nhật Tọa độ
        if (showCoordinates && coordsLabel != null) {
            int tileX = (int) Math.floor(player.getTileX() / WorldConfig.TILE_SIZE);
            // Hệ thống tọa độ của JavaFX tăng dần từ trên xuống dưới.
            // Để hiển thị tọa độ theo trục Y hướng từ dưới lên (logic game thông thường), ta cần đảo dấu giá trị này.
            int tileY = -(int) Math.floor(player.getTileY() / WorldConfig.TILE_SIZE);

            coordsLabel.setText(String.format(HudConfig.COORDS_FORMAT, tileX, tileY));
        }
    }

    /**
     * Cập nhật biểu tượng thời tiết.
     */
    public void updateWeather(boolean isRaining) {
        if (assetManager == null || weatherIcon == null) return;

        Image weatherIconImage;
        if (isRaining) {
            weatherIconImage = assetManager.getGuiIcon("RAIN");
        } else {
            weatherIconImage = assetManager.getGuiIcon("SUNNY");
        }

        if (weatherIconImage != null) {
            weatherIcon.setImage(weatherIconImage);
        }
    }

    /**
     * Cập nhật hiển thị đồng hồ thời gian trong game.
     */
    public void updateTimer(int day, String timeString) {
        this.dayLabel.setText(HudConfig.DAY_PREFIX + day);
        this.timerLabel.setText(timeString);
    }

    /**
     * Cập nhật số tiền hiển thị.
     */
    public void updateMoney(double amount) {
        this.moneyLabel.setText(HudConfig.MONEY_PREFIX + (int)amount);
    }

    /**
     * Cập nhật độ tối màn hình (hiệu ứng ngày/đêm) dựa trên cường độ ánh sáng và cài đặt độ sáng.
     */
    public void updateLighting(double intensity) {
        final double MAX_DARKNESS = 0.95;
        double naturalDarkness = 1.0 - intensity;

        // Công thức tính độ mờ: độ tối tự nhiên cộng với hệ số điều chỉnh độ sáng người dùng cài đặt.
        // Điều này đảm bảo màn hình không bao giờ quá tối nếu người chơi tăng độ sáng.
        double finalOpacity = naturalDarkness + (1.0 - brightness) * 0.5;

        // Giới hạn độ tối tối đa để tránh màn hình đen hoàn toàn
        finalOpacity = Math.max(0.0, Math.min(finalOpacity, MAX_DARKNESS));

        this.darknessOverlay.setOpacity(finalOpacity);
    }

    /**
     * Hiển thị một đoạn văn bản thông báo tạm thời trên đầu người chơi.
     */
    public void showTemporaryText(String message, double playerScreenX, double playerScreenY) {
        if (temporaryTextAnimation != null && temporaryTextAnimation.getStatus().equals(javafx.animation.Animation.Status.RUNNING)) {
            temporaryTextAnimation.stop();
        }

        temporaryText.setText(message);
        temporaryText.setLayoutX(playerScreenX - temporaryText.getLayoutBounds().getWidth() / 2);
        temporaryText.setLayoutY(playerScreenY + HudConfig.TEMP_TEXT_OFFSET_Y);
        temporaryText.setOpacity(1);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(HudConfig.TEMP_TEXT_FADE_DURATION), temporaryText);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        PauseTransition pause = new PauseTransition(Duration.millis(HudConfig.TEMP_TEXT_DISPLAY_DURATION - HudConfig.TEMP_TEXT_FADE_DURATION));

        temporaryTextAnimation = new SequentialTransition(temporaryText, pause, fadeOut);
        temporaryTextAnimation.play();
    }

    // ==============================================================================================
    // TIỆN ÍCH & LOGIC PHỤ TRỢ (HELPERS)
    // ==============================================================================================

    public void setBrightness(double brightness) {
        this.brightness = Math.max(GameLogicConfig.MIN_BRIGHTNESS, Math.min(GameLogicConfig.MAX_BRIGHTNESS, brightness));
    }

    public double getBrightness() {
        return brightness;
    }

    /**
     * Kiểm tra xem chuột có đang nằm trên biểu tượng Thùng rác không.
     * Sử dụng để xác định hành động kéo thả xóa vật phẩm.
     */
    public boolean isMouseOverTrash(double screenX, double screenY) {
        if (trashIconButtonPane == null) return false;

        // Chuyển đổi tọa độ màn hình sang tọa độ cục bộ của Pane
        javafx.geometry.Point2D localPoint = this.sceneToLocal(screenX, screenY);

        double x = trashIconButtonPane.getLayoutX();
        double y = trashIconButtonPane.getLayoutY();
        double width = trashIconButtonPane.getPrefWidth();
        double height = trashIconButtonPane.getPrefHeight();

        return localPoint.getX() >= x && localPoint.getX() <= x + width &&
                localPoint.getY() >= y && localPoint.getY() <= y + height;
    }

    public Rectangle getDarknessOverlay() {
        return darknessOverlay;
    }

    public void setCoordinatesVisible(boolean visible) {
        this.showCoordinates = visible;
        if (coordsContainer != null) {
            coordsContainer.setVisible(visible);
        }
    }

    public boolean isCoordinatesVisible() {
        return showCoordinates;
    }
}