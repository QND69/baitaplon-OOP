package com.example.farmSimulation.view;

import com.example.farmSimulation.config.QuestConfig;
import com.example.farmSimulation.model.Quest;
import com.example.farmSimulation.model.QuestManager;
import com.example.farmSimulation.model.Player;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Giao diện bảng nhiệm vụ, chịu trách nhiệm hiển thị danh sách các nhiệm vụ hàng ngày.
 */
public class QuestBoardView extends VBox {
    private final QuestManager questManager;
    private final Player player;
    private boolean isVisible = false;
    private VBox questListBox;

    public QuestBoardView(QuestManager questManager, Player player) {
        this.questManager = questManager;
        this.player = player;

        setupUI();
        updateQuestList();
    }

    /**
     * Thiết lập cấu trúc và giao diện cho bảng nhiệm vụ.
     */
    private void setupUI() {
        // Thiết lập kích thước cố định cho bảng
        this.setPrefSize(QuestConfig.QUEST_BOARD_WIDTH, QuestConfig.QUEST_BOARD_HEIGHT);
        this.setMaxSize(QuestConfig.QUEST_BOARD_WIDTH, QuestConfig.QUEST_BOARD_HEIGHT);
        this.setMinSize(QuestConfig.QUEST_BOARD_WIDTH, QuestConfig.QUEST_BOARD_HEIGHT);

        // Cấu hình hình nền và bo tròn góc
        this.setBackground(new Background(new BackgroundFill(
                QuestConfig.QUEST_BOARD_BG_COLOR,
                new CornerRadii(10),
                Insets.EMPTY
        )));

        // Tạo viền trắng cho bảng
        this.setBorder(new Border(new BorderStroke(
                Color.WHITE,
                BorderStrokeStyle.SOLID,
                new CornerRadii(10),
                new BorderWidths(2)
        )));

        // Căn chỉnh khoảng cách lề và vị trí nội dung
        this.setPadding(new Insets(QuestConfig.QUEST_BOARD_PADDING));
        this.setSpacing(15);
        this.setAlignment(Pos.TOP_CENTER);

        // Tạo tiêu đề bảng
        Label titleLabel = new Label("DAILY QUESTS");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, QuestConfig.QUEST_TITLE_FONT_SIZE));
        titleLabel.setTextFill(QuestConfig.QUEST_TEXT_COLOR);

        // Khởi tạo khung chứa danh sách nhiệm vụ
        questListBox = new VBox(QuestConfig.QUEST_ROW_SPACING);
        questListBox.setAlignment(Pos.TOP_CENTER);
        questListBox.setPrefWidth(QuestConfig.QUEST_BOARD_WIDTH - QuestConfig.QUEST_BOARD_PADDING * 2);

        // Thêm tiêu đề và danh sách nhiệm vụ vào giao diện chính
        this.getChildren().addAll(titleLabel, questListBox);

        // Mặc định ẩn bảng khi khởi tạo
        this.setVisible(false);
        this.setManaged(false);
    }

    /**
     * Cập nhật lại danh sách nhiệm vụ hiển thị trên bảng dựa trên dữ liệu mới nhất.
     */
    public void updateQuestList() {
        questListBox.getChildren().clear();

        List<Quest> quests = questManager.getActiveQuests();

        // Hiển thị thông báo nếu không còn nhiệm vụ nào đang hoạt động
        if (quests.isEmpty()) {
            Label noQuestsLabel = new Label("No active quests");
            noQuestsLabel.setFont(Font.font("Arial", QuestConfig.QUEST_DESCRIPTION_FONT_SIZE));
            noQuestsLabel.setTextFill(QuestConfig.QUEST_TEXT_COLOR);
            questListBox.getChildren().add(noQuestsLabel);
            return;
        }

        // Tạo giao diện cho từng nhiệm vụ và thêm vào danh sách
        for (Quest quest : quests) {
            VBox questRow = createQuestRow(quest);
            questListBox.getChildren().add(questRow);
        }
    }

    /**
     * Tạo giao diện hiển thị chi tiết cho một nhiệm vụ cụ thể.
     */
    private VBox createQuestRow(Quest quest) {
        VBox questRow = new VBox(5);
        questRow.setAlignment(Pos.CENTER_LEFT);
        questRow.setPrefWidth(QuestConfig.QUEST_BOARD_WIDTH - QuestConfig.QUEST_BOARD_PADDING * 2);
        questRow.setPrefHeight(QuestConfig.QUEST_ROW_HEIGHT);
        // Thiết lập nền bán trong suốt cho từng hàng nhiệm vụ
        questRow.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 5; -fx-padding: 10;");

        // Nhãn mô tả nội dung nhiệm vụ
        Label descLabel = new Label(quest.getDescription());
        descLabel.setFont(Font.font("Arial", QuestConfig.QUEST_DESCRIPTION_FONT_SIZE));
        descLabel.setTextFill(QuestConfig.QUEST_TEXT_COLOR);
        descLabel.setWrapText(true);

        // Khu vực hiển thị thanh tiến độ
        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER_LEFT);

        ProgressBar progressBar = new ProgressBar(quest.getProgressPercent());
        progressBar.setPrefWidth(QuestConfig.PROGRESS_BAR_WIDTH);
        progressBar.setPrefHeight(QuestConfig.PROGRESS_BAR_HEIGHT);
        progressBar.setStyle("-fx-accent: " + toHexString(QuestConfig.QUEST_PROGRESS_COLOR) + ";");

        // Hiển thị tiến độ dưới dạng số lượng hiện tại trên tổng mục tiêu
        Label progressLabel = new Label(quest.getCurrentAmount() + "/" + quest.getTargetAmount());
        progressLabel.setFont(Font.font("Arial", QuestConfig.QUEST_DESCRIPTION_FONT_SIZE));
        progressLabel.setTextFill(QuestConfig.QUEST_TEXT_COLOR);

        progressBox.getChildren().addAll(progressBar, progressLabel);

        // Khu vực hiển thị phần thưởng và nút thao tác
        HBox rewardBox = new HBox(10);
        rewardBox.setAlignment(Pos.CENTER_LEFT);

        Label rewardLabel = new Label("Reward: $" + (int)quest.getRewardMoney() + " + " + (int)quest.getRewardXp() + " XP");
        rewardLabel.setFont(Font.font("Arial", QuestConfig.QUEST_REWARD_FONT_SIZE));
        rewardLabel.setTextFill(QuestConfig.QUEST_REWARD_COLOR);

        // Cấu hình nút nhận thưởng
        Button claimButton = new Button();
        claimButton.setPrefSize(QuestConfig.CLAIM_BUTTON_WIDTH, QuestConfig.CLAIM_BUTTON_HEIGHT);
        claimButton.setFont(Font.font("Arial", 12));

        if (quest.isClaimed()) {
            // Trạng thái: Đã nhận thưởng
            claimButton.setText("Claimed");
            claimButton.setDisable(true);
            claimButton.setStyle("-fx-background-color: " + toHexString(QuestConfig.CLAIM_BUTTON_CLAIMED_COLOR) + "; -fx-text-fill: white;");
        } else if (quest.isCompleted()) {
            // Trạng thái: Đã hoàn thành và có thể nhận thưởng
            claimButton.setText("Claim");
            claimButton.setDisable(false);
            claimButton.setStyle("-fx-background-color: " + toHexString(QuestConfig.CLAIM_BUTTON_ENABLED_COLOR) + "; -fx-text-fill: white;");
            claimButton.setOnAction(e -> {
                boolean success = questManager.claimReward(quest, player);
                if (success) {
                    updateQuestList(); // Làm mới giao diện ngay sau khi nhận thưởng thành công
                }
            });
        } else {
            // Trạng thái: Chưa hoàn thành
            claimButton.setText("Claim");
            claimButton.setDisable(true);
            claimButton.setStyle("-fx-background-color: " + toHexString(QuestConfig.CLAIM_BUTTON_DISABLED_COLOR) + "; -fx-text-fill: white;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        rewardBox.getChildren().addAll(rewardLabel, spacer, claimButton);

        questRow.getChildren().addAll(descLabel, progressBox, rewardBox);

        return questRow;
    }

    /**
     * Chuyển đổi đối tượng màu sắc sang chuỗi mã hex để sử dụng trong CSS.
     */
    private String toHexString(javafx.scene.paint.Paint paint) {
        if (paint instanceof Color) {
            Color color = (Color) paint;
            return String.format("#%02X%02X%02X",
                    (int)(color.getRed() * 255),
                    (int)(color.getGreen() * 255),
                    (int)(color.getBlue() * 255)
            );
        }
        // Trả về màu đen mặc định nếu tham số truyền vào không phải là Color
        return "#000000";
    }

    /**
     * Bật hoặc tắt trạng thái hiển thị của bảng nhiệm vụ.
     */
    public void toggle() {
        isVisible = !isVisible;
        this.setVisible(isVisible);
        this.setManaged(isVisible);
        if (isVisible) {
            updateQuestList(); // Cập nhật dữ liệu mới nhất khi mở bảng
            this.toFront(); // Đảm bảo bảng hiển thị trên cùng
        }
    }

    /**
     * Kiểm tra xem bảng nhiệm vụ có đang hiển thị hay không.
     */
    public boolean isQuestBoardVisible() {
        return isVisible;
    }

    /**
     * Ẩn bảng nhiệm vụ.
     */
    public void hide() {
        isVisible = false;
        this.setVisible(false);
        this.setManaged(false);
    }

    /**
     * Hiển thị bảng nhiệm vụ.
     */
    public void show() {
        isVisible = true;
        this.setVisible(true);
        this.setManaged(true);
        updateQuestList(); // Cập nhật dữ liệu khi hiển thị
        this.toFront();
    }
}