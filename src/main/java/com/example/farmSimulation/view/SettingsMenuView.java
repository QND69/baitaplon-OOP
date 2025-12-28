package com.example.farmSimulation.view;

import com.example.farmSimulation.config.*;
import com.example.farmSimulation.config.SettingsMenuConfig;
import com.example.farmSimulation.model.GameManager;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class SettingsMenuView extends GridPane {
    private GameManager gameManager; // Không sử dụng final để cho phép thiết lập sau khi khởi tạo
    private final Label nameLabel;
    private final Label levelLabel;
    private double brightness = SettingsMenuConfig.DEFAULT_BRIGHTNESS;
    private Slider brightnessSlider;

    // Cài đặt âm thanh
    private double masterVolume = SettingsMenuConfig.DEFAULT_MASTER_VOLUME;
    private Slider masterVolumeSlider;

    // Thanh điều hướng tùy chỉnh
    private final HBox navBar;
    private final StackPane contentArea;
    private Button generalButton;
    private Button controlsButton;
    private Button tutorialButton;
    private String currentTab = SettingsMenuConfig.TAB_GENERAL; // Theo dõi tab hiện đang hoạt động

    public SettingsMenuView(GameManager gameManager) {
        this.gameManager = gameManager; // Có thể là null tại thời điểm khởi tạo

        this.setStyle(SettingsMenuConfig.SETTINGS_MENU_STYLE_CSS);
        this.setPrefSize(SettingsMenuConfig.SETTINGS_MENU_WIDTH_NEW, SettingsMenuConfig.SETTINGS_MENU_HEIGHT_NEW);
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(30));
        this.setHgap(20);
        this.setVgap(15);

        // Cấu hình các cột cho GridPane
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(40); // Cột 1: Nhãn hiển thị
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(60); // Cột 2: Các điều khiển
        this.getColumnConstraints().addAll(col1, col2);

        int currentRow = 0;

        // Tiêu đề trải rộng trên 2 cột
        Label title = new Label(SettingsMenuConfig.GAME_MENU_LABEL_TEXT);
        title.setTextFill(SettingsMenuConfig.SETTINGS_MENU_FONT_COLOR);
        title.setFont(Font.font(SettingsMenuConfig.SETTINGS_MENU_FONT_FAMILY, SettingsMenuConfig.SETTINGS_MENU_TITLE_FONT_SIZE));
        title.setTextAlignment(TextAlignment.CENTER);
        this.add(title, 0, currentRow++, 2, 1);
        GridPane.setHalignment(title, HPos.CENTER);

        // Thông tin người chơi trải rộng trên 2 cột
        VBox playerInfo = new VBox(SettingsMenuConfig.SETTINGS_PLAYER_INFO_SPACING);
        playerInfo.setAlignment(Pos.CENTER);
        nameLabel = new Label(SettingsMenuConfig.PLAYER_LABEL_PREFIX);
        levelLabel = new Label(SettingsMenuConfig.LEVEL_LABEL_PREFIX);
        nameLabel.setTextFill(SettingsMenuConfig.SETTINGS_MENU_FONT_COLOR);
        levelLabel.setTextFill(SettingsMenuConfig.SETTINGS_MENU_FONT_COLOR);
        nameLabel.setFont(Font.font(SettingsMenuConfig.SETTINGS_MENU_FONT_FAMILY, SettingsMenuConfig.SETTINGS_MENU_BODY_FONT_SIZE));
        levelLabel.setFont(Font.font(SettingsMenuConfig.SETTINGS_MENU_FONT_FAMILY, SettingsMenuConfig.SETTINGS_MENU_BODY_FONT_SIZE));
        playerInfo.getChildren().addAll(nameLabel, levelLabel);
        this.add(playerInfo, 0, currentRow++, 2, 1);
        GridPane.setHalignment(playerInfo, HPos.CENTER);

        // Thanh điều hướng tùy chỉnh
        navBar = new HBox(SettingsMenuConfig.NAV_BAR_SPACING); // Khoảng cách giữa các nút
        navBar.setAlignment(Pos.CENTER);
        navBar.setPadding(new Insets(10, 0, 10, 0));

        // Tạo các nút điều hướng
        generalButton = new Button(SettingsMenuConfig.TAB_GENERAL);
        controlsButton = new Button(SettingsMenuConfig.TAB_CONTROLS);
        tutorialButton = new Button(SettingsMenuConfig.TAB_TUTORIAL);

        // Thiết lập hành động cho nút
        generalButton.setOnAction(e -> switchTab(SettingsMenuConfig.TAB_GENERAL));
        controlsButton.setOnAction(e -> switchTab(SettingsMenuConfig.TAB_CONTROLS));
        tutorialButton.setOnAction(e -> switchTab(SettingsMenuConfig.TAB_TUTORIAL));

        // Thêm các nút vào thanh điều hướng
        navBar.getChildren().addAll(generalButton, controlsButton, tutorialButton);

        // Thêm thanh điều hướng vào lưới giao diện
        this.add(navBar, 0, currentRow++, 2, 1);
        GridPane.setHalignment(navBar, HPos.CENTER);

        // Khu vực nội dung để hiển thị giao diện đang hoạt động
        contentArea = new StackPane();
        contentArea.setPrefSize(SettingsMenuConfig.SETTINGS_MENU_WIDTH_NEW - SettingsMenuConfig.CONTENT_AREA_WIDTH_PADDING, SettingsMenuConfig.CONTENT_AREA_HEIGHT); // Để lại khoảng trống cho phần đệm

        // Thêm khu vực nội dung vào lưới
        this.add(contentArea, 0, currentRow++, 2, 1);
        GridPane.setHgrow(contentArea, Priority.ALWAYS);
        GridPane.setVgrow(contentArea, Priority.ALWAYS);

        // Khởi tạo với tab Chung
        switchTab(SettingsMenuConfig.TAB_GENERAL);

        // Căn chỉnh menu ra giữa màn hình
        this.setLayoutX(WindowConfig.SCREEN_WIDTH / 2 - SettingsMenuConfig.SETTINGS_MENU_WIDTH_NEW / 2);
        this.setLayoutY(WindowConfig.SCREEN_HEIGHT / 2 - SettingsMenuConfig.SETTINGS_MENU_HEIGHT_NEW / 2);

        this.setVisible(false); // Ẩn menu khi mới khởi tạo
    }

    /**
     * Chuyển đổi sang một tab khác và cập nhật kiểu dáng nút
     * @param tabName Tên của tab cần chuyển đổi ("General", "Controls", hoặc "Tutorial")
     */
    private void switchTab(String tabName) {
        currentTab = tabName;

        // Xóa nội dung hiện tại
        contentArea.getChildren().clear();

        // Lấy và hiển thị nội dung mới
        Node content = null;
        switch (tabName) {
            case SettingsMenuConfig.TAB_GENERAL:
                content = createGeneralContent();
                break;
            case SettingsMenuConfig.TAB_CONTROLS:
                content = createControlsContent();
                break;
            case SettingsMenuConfig.TAB_TUTORIAL:
                content = createTutorialContent();
                break;
        }

        if (content != null) {
            contentArea.getChildren().add(content);
        }

        // Cập nhật kiểu dáng nút
        updateNavButtonStyles();
    }

    /**
     * Cập nhật kiểu dáng nút điều hướng dựa trên tab đang hoạt động
     */
    private void updateNavButtonStyles() {
        // Kiểu cho nút đang kích hoạt
        String activeButtonStyle = SettingsMenuConfig.NAV_BUTTON_ACTIVE_STYLE;

        // Kiểu cho nút không kích hoạt
        String inactiveButtonStyle = SettingsMenuConfig.NAV_BUTTON_INACTIVE_STYLE;

        // Áp dụng kiểu dáng dựa trên tab hiện tại
        generalButton.setStyle(currentTab.equals(SettingsMenuConfig.TAB_GENERAL) ? activeButtonStyle : inactiveButtonStyle);
        controlsButton.setStyle(currentTab.equals(SettingsMenuConfig.TAB_CONTROLS) ? activeButtonStyle : inactiveButtonStyle);
        tutorialButton.setStyle(currentTab.equals(SettingsMenuConfig.TAB_TUTORIAL) ? activeButtonStyle : inactiveButtonStyle);
    }

    /**
     * Tạo nội dung "General" chứa Cài đặt âm lượng, Độ sáng, và các nút Tiếp tục/Lưu/Thoát
     */
    private Node createGeneralContent() {
        GridPane contentGrid = new GridPane();
        contentGrid.setHgap(20);
        contentGrid.setVgap(15);
        contentGrid.setPadding(new Insets(20));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(40);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(60);
        contentGrid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        // Thanh trượt điều chỉnh âm lượng tổng
        Label masterVolLabel = new Label(SettingsMenuConfig.SETTINGS_MASTER_VOLUME_LABEL);
        masterVolLabel.setTextFill(SettingsMenuConfig.SETTINGS_MENU_FONT_COLOR);
        masterVolLabel.setFont(Font.font(SettingsMenuConfig.SETTINGS_MENU_FONT_FAMILY, SettingsMenuConfig.SETTINGS_MENU_BODY_FONT_SIZE));
        contentGrid.add(masterVolLabel, 0, row);

        masterVolumeSlider = new Slider(SettingsMenuConfig.SLIDER_MIN_VALUE, SettingsMenuConfig.SLIDER_MAX_VALUE, SettingsMenuConfig.DEFAULT_MASTER_VOLUME);
        masterVolumeSlider.setPrefWidth(SettingsMenuConfig.SLIDER_WIDTH);
        masterVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            masterVolume = newVal.doubleValue();
            // Cập nhật âm lượng tổng vào hệ thống âm thanh theo thời gian thực
            if (gameManager != null && gameManager.getAudioManager() != null) {
                gameManager.getAudioManager().setGlobalVolume(masterVolume);
            }
        });
        contentGrid.add(masterVolumeSlider, 1, row++);

        // Thanh trượt độ sáng
        Label brightnessLabel = new Label(SettingsMenuConfig.BRIGHTNESS_LABEL);
        brightnessLabel.setTextFill(SettingsMenuConfig.SETTINGS_MENU_FONT_COLOR);
        brightnessLabel.setFont(Font.font(SettingsMenuConfig.SETTINGS_MENU_FONT_FAMILY, SettingsMenuConfig.SETTINGS_MENU_BODY_FONT_SIZE));
        contentGrid.add(brightnessLabel, 0, row);

        brightnessSlider = new Slider(SettingsMenuConfig.BRIGHTNESS_MIN, SettingsMenuConfig.BRIGHTNESS_MAX, brightness);
        brightnessSlider.setPrefWidth(SettingsMenuConfig.SLIDER_WIDTH);
        brightnessSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            brightness = newVal.doubleValue();
            // Áp dụng độ sáng vào HudView ngay lập tức
            if (gameManager != null && gameManager.getMainGameView() != null && gameManager.getMainGameView().getHudView() != null) {
                HudView hudView = gameManager.getMainGameView().getHudView();
                hudView.setBrightness(brightness);
                // Cập nhật lại ánh sáng để áp dụng độ sáng mới
                if (gameManager.getTimeManager() != null) {
                    double currentIntensity = gameManager.getTimeManager().getCurrentLightIntensity();
                    hudView.updateLighting(currentIntensity);
                }
            }
        });
        contentGrid.add(brightnessSlider, 1, row++);

        // Khu vực các nút chức năng
        VBox buttonsBox = new VBox(SettingsMenuConfig.BUTTON_BOX_SPACING);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setPadding(new Insets(20, 0, 0, 0));

        // Nút Tiếp tục
        Button resume = new Button(SettingsMenuConfig.SETTINGS_RESUME_BUTTON_TEXT);
        resume.setPrefWidth(SettingsMenuConfig.SETTINGS_MENU_BUTTON_WIDTH);
        resume.setOnAction(e -> {
            if (this.gameManager != null) {
                // Đóng menu cài đặt
                this.gameManager.toggleSettingsMenu();

                // Yêu cầu focus để các phím tắt hoạt động ngay lập tức
                if (this.gameManager.getMainGameView() != null &&
                        this.gameManager.getMainGameView().getRootPane() != null &&
                        this.gameManager.getMainGameView().getRootPane().getScene() != null) {
                    this.gameManager.getMainGameView().getRootPane().requestFocus();
                }
            }
        });

        // Nút Lưu
        Button save = new Button(SettingsMenuConfig.SETTINGS_SAVE_BUTTON_TEXT);
        save.setPrefWidth(SettingsMenuConfig.SETTINGS_MENU_BUTTON_WIDTH);
        save.setOnAction(e -> {
            if (this.gameManager != null) {
                this.gameManager.saveGameData(); // Gọi logic lưu trò chơi từ GameManager
            }
        });

        // Nút Thoát
        Button exit = new Button(SettingsMenuConfig.SETTINGS_EXIT_BUTTON_TEXT);
        exit.setPrefWidth(SettingsMenuConfig.SETTINGS_MENU_BUTTON_WIDTH);
        exit.setOnAction(e -> System.exit(0));

        buttonsBox.getChildren().addAll(resume, save, exit);
        contentGrid.add(buttonsBox, 0, row++, 2, 1);
        GridPane.setHalignment(buttonsBox, HPos.CENTER);

        ScrollPane scrollPane = new ScrollPane(contentGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        return scrollPane;
    }

    /**
     * Tạo nội dung "Controls" hiển thị danh sách phím tắt
     */
    private Node createControlsContent() {
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(20));
        contentBox.setAlignment(Pos.TOP_LEFT);

        Label keyBindingsLabel = new Label(SettingsMenuConfig.CONTROLS_TITLE);
        keyBindingsLabel.setTextFill(SettingsMenuConfig.SETTINGS_MENU_FONT_COLOR);
        keyBindingsLabel.setFont(Font.font(SettingsMenuConfig.SETTINGS_MENU_FONT_FAMILY, 16));
        keyBindingsLabel.setStyle("-fx-font-weight: bold;");

        // Tạo GridPane để căn chỉnh Phím bên trái và Hành động bên phải
        GridPane keyBindingsGrid = new GridPane();
        keyBindingsGrid.setHgap(30);
        keyBindingsGrid.setVgap(8);
        keyBindingsGrid.setAlignment(Pos.CENTER_LEFT);

        // Tạo các nhãn hiển thị chi tiết phím tắt
        int row = 0;

        // Các phím di chuyển
        Label keyW = new Label("W");
        Label actionW = new Label(SettingsMenuConfig.ACTION_MOVE_UP);
        Label keyA = new Label("A");
        Label actionA = new Label(SettingsMenuConfig.ACTION_MOVE_LEFT);
        Label keyS = new Label("S");
        Label actionS = new Label(SettingsMenuConfig.ACTION_MOVE_DOWN);
        Label keyD = new Label("D");
        Label actionD = new Label(SettingsMenuConfig.ACTION_MOVE_RIGHT);

        // Các phím chức năng khác
        Label keyB = new Label("B");
        Label actionB = new Label(SettingsMenuConfig.ACTION_SHOP);
        Label keyQ = new Label("Q");
        Label actionQ = new Label(SettingsMenuConfig.ACTION_DROP);
        Label keyJ = new Label("J");
        Label actionJ = new Label(SettingsMenuConfig.ACTION_QUEST);
        Label keyESC = new Label("ESC");
        Label actionESC = new Label(SettingsMenuConfig.ACTION_PAUSE);
        Label keyNum = new Label(SettingsMenuConfig.KEY_NUMS);
        Label actionNum = new Label(SettingsMenuConfig.ACTION_HOTBAR);
        Label keyMouseLeft = new Label(SettingsMenuConfig.KEY_MOUSE_LEFT);
        Label actionMouseLeft = new Label(SettingsMenuConfig.ACTION_PRIMARY);
        Label keyMouseRight = new Label(SettingsMenuConfig.KEY_MOUSE_RIGHT);
        Label actionMouseRight = new Label(SettingsMenuConfig.ACTION_SECONDARY);

        // Định dạng cho tất cả các nhãn phím
        Label[] keyLabels = {keyW, keyA, keyS, keyD, keyB, keyQ, keyJ, keyESC, keyNum, keyMouseLeft, keyMouseRight};
        for (Label lbl : keyLabels) {
            lbl.setTextFill(SettingsMenuConfig.SETTINGS_MENU_FONT_COLOR);
            lbl.setFont(Font.font(SettingsMenuConfig.SETTINGS_MENU_FONT_FAMILY, FontWeight.BOLD, 14));
        }

        // Định dạng cho tất cả các nhãn hành động
        Label[] actionLabels = {actionW, actionA, actionS, actionD, actionB, actionQ, actionJ, actionESC, actionNum, actionMouseLeft, actionMouseRight};
        for (Label lbl : actionLabels) {
            lbl.setTextFill(SettingsMenuConfig.SETTINGS_MENU_FONT_COLOR);
            lbl.setFont(Font.font(SettingsMenuConfig.SETTINGS_MENU_FONT_FAMILY, 14));
        }

        // Thêm vào lưới
        keyBindingsGrid.add(keyW, 0, row++);
        keyBindingsGrid.add(actionW, 1, row - 1);
        keyBindingsGrid.add(keyA, 0, row++);
        keyBindingsGrid.add(actionA, 1, row - 1);
        keyBindingsGrid.add(keyS, 0, row++);
        keyBindingsGrid.add(actionS, 1, row - 1);
        keyBindingsGrid.add(keyD, 0, row++);
        keyBindingsGrid.add(actionD, 1, row - 1);
        keyBindingsGrid.add(keyB, 0, row++);
        keyBindingsGrid.add(actionB, 1, row - 1);
        keyBindingsGrid.add(keyQ, 0, row++);
        keyBindingsGrid.add(actionQ, 1, row - 1);
        keyBindingsGrid.add(keyJ, 0, row++);
        keyBindingsGrid.add(actionJ, 1, row - 1);
        keyBindingsGrid.add(keyESC, 0, row++);
        keyBindingsGrid.add(actionESC, 1, row - 1);
        keyBindingsGrid.add(keyNum, 0, row++);
        keyBindingsGrid.add(actionNum, 1, row - 1);
        keyBindingsGrid.add(keyMouseLeft, 0, row++);
        keyBindingsGrid.add(actionMouseLeft, 1, row - 1);
        keyBindingsGrid.add(keyMouseRight, 0, row++);
        keyBindingsGrid.add(actionMouseRight, 1, row - 1);

        // Thêm bảng phím tắt vào hộp nội dung
        contentBox.getChildren().addAll(keyBindingsLabel, keyBindingsGrid);

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        return scrollPane;
    }

    /**
     * Tạo nội dung "Tutorial" chứa hướng dẫn chơi game
     */
    private Node createTutorialContent() {
        // Sử dụng Label để hiển thị văn bản hướng dẫn
        Label tutorialLabel = new Label();
        tutorialLabel.setWrapText(true);
        tutorialLabel.setMinHeight(Region.USE_PREF_SIZE); // Buộc nhãn tính toán chiều cao đầy đủ dựa trên văn bản được xuống dòng
        tutorialLabel.setFont(Font.font(SettingsMenuConfig.SETTINGS_MENU_FONT_FAMILY, 14));
        tutorialLabel.setTextFill(javafx.scene.paint.Color.WHITE);
        // Sử dụng hằng số cấu hình cho văn bản hướng dẫn
        tutorialLabel.setText(SettingsMenuConfig.TUTORIAL_TEXT);

        // Container VBox để căn chỉnh lề
        VBox tutorialContainer = new VBox(10);
        tutorialContainer.setPadding(new Insets(20));
        tutorialContainer.setAlignment(Pos.TOP_LEFT);
        tutorialContainer.getChildren().add(tutorialLabel);

        ScrollPane scrollPane = new ScrollPane(tutorialContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        return scrollPane;
    }


    public void updatePlayerInfo(String playerName, int playerLevel) {
        nameLabel.setText(SettingsMenuConfig.PLAYER_LABEL_PREFIX + playerName);
        levelLabel.setText(SettingsMenuConfig.LEVEL_LABEL_PREFIX + playerLevel);
    }

    public void show() {
        // Tải cài đặt độ sáng hiện tại từ HudView khi mở menu
        if (gameManager != null && gameManager.getMainGameView() != null && gameManager.getMainGameView().getHudView() != null) {
            HudView hudView = gameManager.getMainGameView().getHudView();
            brightness = hudView.getBrightness();
            if (brightnessSlider != null) {
                brightnessSlider.setValue(brightness);
            }
        }

        // Tải cài đặt âm lượng hiện tại từ AudioManager khi mở menu
        if (gameManager != null && gameManager.getAudioManager() != null) {
            masterVolume = gameManager.getAudioManager().getCurrentVolume();
            if (masterVolumeSlider != null) {
                masterVolumeSlider.setValue(masterVolume);
            }
        }

        setVisible(true);
        // Đảm bảo menu cài đặt luôn hiển thị trên cùng
        this.toFront();
    }

    public void hide() {
        setVisible(false);
    }

    /**
     * Thiết lập tham chiếu GameManager
     * @param gameManager Đối tượng GameManager
     */
    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }
}