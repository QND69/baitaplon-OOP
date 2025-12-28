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
 * Quest Board UI - Hiển thị các nhiệm vụ hàng ngày
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
     * Setup Quest Board UI
     */
    private void setupUI() {
        // Set size
        this.setPrefSize(QuestConfig.QUEST_BOARD_WIDTH, QuestConfig.QUEST_BOARD_HEIGHT);
        this.setMaxSize(QuestConfig.QUEST_BOARD_WIDTH, QuestConfig.QUEST_BOARD_HEIGHT);
        this.setMinSize(QuestConfig.QUEST_BOARD_WIDTH, QuestConfig.QUEST_BOARD_HEIGHT);

        // Background
        this.setBackground(new Background(new BackgroundFill(
                QuestConfig.QUEST_BOARD_BG_COLOR,
                new CornerRadii(10),
                Insets.EMPTY
        )));
        this.setBorder(new Border(new BorderStroke(
                Color.WHITE,
                BorderStrokeStyle.SOLID,
                new CornerRadii(10),
                new BorderWidths(2)
        )));

        // Padding
        this.setPadding(new Insets(QuestConfig.QUEST_BOARD_PADDING));
        this.setSpacing(15);
        this.setAlignment(Pos.TOP_CENTER);

        // Title
        Label titleLabel = new Label("DAILY QUESTS");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, QuestConfig.QUEST_TITLE_FONT_SIZE));
        titleLabel.setTextFill(QuestConfig.QUEST_TEXT_COLOR);

        // Quest list container
        questListBox = new VBox(QuestConfig.QUEST_ROW_SPACING);
        questListBox.setAlignment(Pos.TOP_CENTER);
        questListBox.setPrefWidth(QuestConfig.QUEST_BOARD_WIDTH - QuestConfig.QUEST_BOARD_PADDING * 2);

        // Add to main container
        this.getChildren().addAll(titleLabel, questListBox);

        // Initially hidden
        this.setVisible(false);
        this.setManaged(false);
    }

    /**
     * Cập nhật danh sách quests
     */
    public void updateQuestList() {
        questListBox.getChildren().clear();

        List<Quest> quests = questManager.getActiveQuests();

        if (quests.isEmpty()) {
            Label noQuestsLabel = new Label("No active quests");
            noQuestsLabel.setFont(Font.font("Arial", QuestConfig.QUEST_DESCRIPTION_FONT_SIZE));
            noQuestsLabel.setTextFill(QuestConfig.QUEST_TEXT_COLOR);
            questListBox.getChildren().add(noQuestsLabel);
            return;
        }

        for (Quest quest : quests) {
            VBox questRow = createQuestRow(quest);
            questListBox.getChildren().add(questRow);
        }
    }

    /**
     * Tạo một hàng quest
     */
    private VBox createQuestRow(Quest quest) {
        VBox questRow = new VBox(5);
        questRow.setAlignment(Pos.CENTER_LEFT);
        questRow.setPrefWidth(QuestConfig.QUEST_BOARD_WIDTH - QuestConfig.QUEST_BOARD_PADDING * 2);
        questRow.setPrefHeight(QuestConfig.QUEST_ROW_HEIGHT);
        questRow.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 5; -fx-padding: 10;");

        // Description
        Label descLabel = new Label(quest.getDescription());
        descLabel.setFont(Font.font("Arial", QuestConfig.QUEST_DESCRIPTION_FONT_SIZE));
        descLabel.setTextFill(QuestConfig.QUEST_TEXT_COLOR);
        descLabel.setWrapText(true);

        // Progress bar with label
        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER_LEFT);

        ProgressBar progressBar = new ProgressBar(quest.getProgressPercent());
        progressBar.setPrefWidth(QuestConfig.PROGRESS_BAR_WIDTH);
        progressBar.setPrefHeight(QuestConfig.PROGRESS_BAR_HEIGHT);
        progressBar.setStyle("-fx-accent: " + toHexString(QuestConfig.QUEST_PROGRESS_COLOR) + ";");

        // [SỬA LỖI] Đổi từ getCurrentProgress() thành getCurrentAmount()
        Label progressLabel = new Label(quest.getCurrentAmount() + "/" + quest.getTargetAmount());
        progressLabel.setFont(Font.font("Arial", QuestConfig.QUEST_DESCRIPTION_FONT_SIZE));
        progressLabel.setTextFill(QuestConfig.QUEST_TEXT_COLOR);

        progressBox.getChildren().addAll(progressBar, progressLabel);

        // Reward and Claim button
        HBox rewardBox = new HBox(10);
        rewardBox.setAlignment(Pos.CENTER_LEFT);

        Label rewardLabel = new Label("Reward: $" + (int)quest.getRewardMoney() + " + " + (int)quest.getRewardXp() + " XP");
        rewardLabel.setFont(Font.font("Arial", QuestConfig.QUEST_REWARD_FONT_SIZE));
        rewardLabel.setTextFill(QuestConfig.QUEST_REWARD_COLOR);

        // Claim button
        Button claimButton = new Button();
        claimButton.setPrefSize(QuestConfig.CLAIM_BUTTON_WIDTH, QuestConfig.CLAIM_BUTTON_HEIGHT);
        claimButton.setFont(Font.font("Arial", 12));

        if (quest.isClaimed()) {
            claimButton.setText("Claimed");
            claimButton.setDisable(true);
            claimButton.setStyle("-fx-background-color: " + toHexString(QuestConfig.CLAIM_BUTTON_CLAIMED_COLOR) + "; -fx-text-fill: white;");
        } else if (quest.isCompleted()) {
            claimButton.setText("Claim");
            claimButton.setDisable(false);
            claimButton.setStyle("-fx-background-color: " + toHexString(QuestConfig.CLAIM_BUTTON_ENABLED_COLOR) + "; -fx-text-fill: white;");
            claimButton.setOnAction(e -> {
                boolean success = questManager.claimReward(quest, player);
                if (success) {
                    updateQuestList(); // Refresh UI
                }
            });
        } else {
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
     * Chuyển đổi Paint (Color) sang hex string cho CSS
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
        // Fallback nếu không phải Color
        return "#000000";
    }

    /**
     * Toggle hiển thị quest board
     */
    public void toggle() {
        isVisible = !isVisible;
        this.setVisible(isVisible);
        this.setManaged(isVisible);
        if (isVisible) {
            updateQuestList(); // Refresh quests when opened
            this.toFront(); // Ensure it's on top
        }
    }

    /**
     * Kiểm tra quest board có đang hiển thị không
     */
    public boolean isQuestBoardVisible() {
        return isVisible;
    }

    /**
     * Ẩn quest board
     */
    public void hide() {
        isVisible = false;
        this.setVisible(false);
        this.setManaged(false);
    }

    /**
     * Hiển thị quest board
     */
    public void show() {
        isVisible = true;
        this.setVisible(true);
        this.setManaged(true);
        updateQuestList(); // Refresh quests when shown
        this.toFront();
    }
}