package com.example.farmSimulation.view;

import com.example.farmSimulation.config.SettingsMenuConfig;
import com.example.farmSimulation.config.WindowConfig;
import com.example.farmSimulation.model.SaveManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.BiConsumer;

/**
 * Màn hình tạo nhân vật, cho phép người chơi thiết lập thông tin ban đầu trước khi vào game.
 */
public class CharacterCreationView {
    private final VBox root;
    private final TextField nameField;
    private final ComboBox<String> genderComboBox;
    private final Label errorLabel;
    private BiConsumer<String, String> onStartGameCallback; // Callback xử lý khi bắt đầu game mới, nhận vào tên và giới tính
    private Runnable onLoadGameCallback; // Callback xử lý việc tải game từ file lưu trữ

    public CharacterCreationView() {
        // Container gốc chứa toàn bộ giao diện
        root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: rgba(30, 30, 50, 0.95);");

        // Tiêu đề màn hình
        Label titleLabel = new Label("Create Your Farmer");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setStyle("-fx-effect: dropshadow(one-pass-box, black, 3, 0, 0, 2);");

        // Container chứa các trường nhập liệu
        VBox formBox = new VBox(15);
        formBox.setAlignment(Pos.CENTER);
        formBox.setMaxWidth(400);
        formBox.setPadding(new Insets(30));
        formBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-background-radius: 15;");

        // Trường nhập tên nhân vật
        Label nameLabel = new Label("Name:");
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.WHITE);

        nameField = new TextField();
        nameField.setPromptText("Enter your farmer's name");
        nameField.setPrefWidth(300);
        nameField.setPrefHeight(35);
        nameField.setFont(Font.font("Arial", 16));
        nameField.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 5;");

        // Hộp chọn giới tính
        Label genderLabel = new Label("Gender:");
        genderLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        genderLabel.setTextFill(Color.WHITE);

        genderComboBox = new ComboBox<>();
        genderComboBox.getItems().addAll("Male", "Female", "🤨🫵❓");
        genderComboBox.setValue("Male"); // Giá trị mặc định
        genderComboBox.setPrefWidth(300);
        genderComboBox.setPrefHeight(35);
        // Thiết lập font hỗ trợ biểu tượng cảm xúc để hiển thị đúng các emoji đặc biệt
        genderComboBox.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.9); " +
                        "-fx-background-radius: 5; " +
                        "-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'System';"
        );

        // Áp dụng font emoji cho từng dòng trong danh sách thả xuống
        genderComboBox.setCellFactory(listView -> {
            javafx.scene.control.ListCell<String> cell = new javafx.scene.control.ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setStyle("-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'System';");
                    }
                }
            };
            return cell;
        });

        // Áp dụng font emoji cho ô hiển thị giá trị đang được chọn
        genderComboBox.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'System';");
                }
            }
        });

        // Nhãn hiển thị thông báo lỗi, mặc định sẽ ẩn đi
        errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setFont(Font.font("Arial", 14));
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        // Nút bắt đầu trò chơi mới
        Button startButton = new Button(SettingsMenuConfig.START_NEW_GAME_TEXT);
        startButton.setPrefWidth(200);
        startButton.setPrefHeight(45);
        startButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
        startButton.setOnMouseEntered(e -> startButton.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-background-radius: 5;"));
        startButton.setOnMouseExited(e -> startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;"));

        startButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String gender = genderComboBox.getValue();

            // Kiểm tra tên nhân vật có hợp lệ không
            if (name.isEmpty()) {
                errorLabel.setText("Please enter a name!");
                errorLabel.setVisible(true);
                return;
            }

            // Ẩn thông báo lỗi nếu dữ liệu hợp lệ
            errorLabel.setVisible(false);

            // Gọi callback để tiến hành vào game
            if (onStartGameCallback != null) {
                onStartGameCallback.accept(name, gender);
            }
        });

        // Nút tải game đã lưu
        Button loadButton = new Button(SettingsMenuConfig.LOAD_BUTTON_TEXT);
        loadButton.setPrefWidth(200);
        loadButton.setPrefHeight(45);
        loadButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        // Kiểm tra sự tồn tại của file lưu trữ để kích hoạt hoặc vô hiệu hóa nút tải game
        if (SaveManager.hasSaveFile()) {
            loadButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 5;");
            loadButton.setOnMouseEntered(e -> loadButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-background-radius: 5;"));
            loadButton.setOnMouseExited(e -> loadButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 5;"));
            loadButton.setOnAction(e -> {
                if (onLoadGameCallback != null) {
                    onLoadGameCallback.run();
                }
            });
        } else {
            loadButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-background-radius: 5;");
            // Vô hiệu hóa nút nếu không tìm thấy dữ liệu lưu trữ
            loadButton.setDisable(true);
            loadButton.setText("NO SAVE FOUND");
        }

        // Thêm các thành phần giao diện vào form
        formBox.getChildren().addAll(
                nameLabel, nameField,
                genderLabel, genderComboBox,
                errorLabel,
                startButton,
                loadButton // Thêm nút tải game vào danh sách hiển thị
        );

        // Thêm tiêu đề và form vào container gốc
        root.getChildren().addAll(titleLabel, formBox);
    }

    /**
     * Thiết lập hành động sẽ được thực thi khi người chơi nhấn nút bắt đầu.
     * Callback này nhận vào tên và giới tính của nhân vật.
     * @param callback Hàm xử lý nhận hai tham số chuỗi
     */
    public void setOnStartGame(BiConsumer<String, String> callback) {
        this.onStartGameCallback = callback;
    }

    /**
     * Thiết lập hành động sẽ được thực thi khi người chơi nhấn nút tải game.
     */
    public void setOnLoadGame(Runnable callback) {
        this.onLoadGameCallback = callback;
    }

    /**
     * Tạo đối tượng Scene từ giao diện hiện tại.
     * @return Scene chứa CharacterCreationView
     */
    public Scene createScene() {
        return new Scene(root, WindowConfig.SCREEN_WIDTH, WindowConfig.SCREEN_HEIGHT);
    }

    /**
     * Lấy đối tượng container gốc của giao diện.
     * @return VBox gốc
     */
    public VBox getRoot() {
        return root;
    }
}