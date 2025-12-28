package com.example.farmSimulation;

import com.example.farmSimulation.config.AssetPaths;
import com.example.farmSimulation.controller.GameController;
import com.example.farmSimulation.model.GameManager;
import com.example.farmSimulation.model.Player;
import com.example.farmSimulation.model.WorldMap;
import com.example.farmSimulation.view.CharacterCreationView;
import com.example.farmSimulation.view.HotbarView;
import com.example.farmSimulation.view.MainGameView;
import com.example.farmSimulation.view.PlayerView;
import com.example.farmSimulation.view.assets.ImageManager;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Lớp chịu trách nhiệm khởi tạo và liên kết tất cả các thành phần
 * theo mô hình Model, View, Controller (MVC).
 */
public class Game {
    private Stage primaryStage;
    private ImageManager imageManager;
    private Player player;
    private WorldMap worldMap;

    /**
     * Điểm bắt đầu của trò chơi, tải tài nguyên và hiển thị menu.
     */
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Tải toàn bộ tài nguyên hình ảnh một lần duy nhất khi ứng dụng bắt đầu
        imageManager = new ImageManager();
        imageManager.loadAssets();

        // Thiết lập biểu tượng cho cửa sổ ứng dụng
        primaryStage.getIcons().add(imageManager.getTexture(AssetPaths.LOGO));

        // Hiển thị màn hình menu chính
        showMainMenu();
    }

    /**
     * Hiển thị màn hình chính (Tạo nhân vật hoặc Menu).
     * Phương thức này được tách riêng để có thể gọi lại khi trò chơi kết thúc.
     */
    private void showMainMenu() {
        // Khởi tạo lại các đối tượng Model cho phiên chơi mới
        // Việc này đảm bảo dữ liệu cũ không bị lưu lại khi bắt đầu game mới
        player = new Player();
        worldMap = new WorldMap();

        // Tạo và hiển thị giao diện tạo nhân vật
        CharacterCreationView characterCreationView = new CharacterCreationView();

        // Thiết lập hành động khi người chơi nhấn nút Bắt đầu game mới
        characterCreationView.setOnStartGame((name, gender) -> {
            // Cập nhật thông tin người chơi từ dữ liệu nhập vào
            player.setName(name);
            player.setGender(gender);

            // Khởi tạo và bắt đầu game ở chế độ chơi mới
            initializeAndStartGame(false);
        });

        // Thiết lập hành động khi người chơi nhấn nút Tải game
        characterCreationView.setOnLoadGame(() -> {
            // Khởi tạo và bắt đầu game ở chế độ tải dữ liệu đã lưu
            initializeAndStartGame(true);
        });

        // Hiển thị cảnh tạo nhân vật lên màn hình
        Scene characterCreationScene = characterCreationView.createScene();
        primaryStage.setTitle("Farm Simulation - Character Creation");
        primaryStage.setScene(characterCreationScene);
        primaryStage.show();
    }

    /**
     * Khởi tạo các thành phần cốt lõi và bắt đầu vòng lặp game.
     * @param loadFromSave Nếu true, dữ liệu sẽ được tải từ file lưu trữ thay vì khởi tạo mặc định.
     */
    private void initializeAndStartGame(boolean loadFromSave) {
        // ----------------------------------------------------------------
        // 1. KHỞI TẠO CÁC THÀNH PHẦN VIEW (GIAO DIỆN)
        // ----------------------------------------------------------------

        // Tạo PlayerView và cung cấp texture từ ImageManager
        PlayerView playerView = new PlayerView(
                imageManager.getTexture(AssetPaths.PLAYER_SHEET),
                imageManager.getTexture(AssetPaths.PLAYER_ACTIONS_SHEET)
        );

        // Tạo HotbarView (thanh công cụ)
        HotbarView hotbarView = new HotbarView(player, imageManager);

        // Tạo MainGameView và truyền các thành phần phụ thuộc cần thiết để vẽ bản đồ
        MainGameView mainGameView = new MainGameView(imageManager, worldMap, hotbarView);

        // ----------------------------------------------------------------
        // 2. KHỞI TẠO CONTROLLER VÀ MANAGER (LOGIC)
        // ----------------------------------------------------------------

        // Khởi tạo Controller để xử lý đầu vào từ người dùng
        GameController gameController = new GameController();

        // Khởi tạo GameManager - bộ não logic của trò chơi
        GameManager gameManager = new GameManager(
                player,
                worldMap,
                mainGameView,
                playerView,
                gameController
        );

        // ----------------------------------------------------------------
        // 3. LIÊN KẾT CÁC THÀNH PHẦN (WIRING)
        // ----------------------------------------------------------------

        // Cung cấp GameManager cho Controller để gọi các hàm xử lý logic
        gameController.setGameManager(gameManager);

        // Cung cấp MainGameView cho Controller để xử lý các tác vụ giao diện như bật tắt cửa hàng
        gameController.setMainGameView(mainGameView);

        // Thiết lập sự kiện kéo thả vật phẩm trong Hotbar
        // Khi giao diện phát hiện hành động hoán đổi, nó sẽ gọi logic xử lý trong GameManager
        hotbarView.setOnSwapListener((indexA, indexB) -> {
            gameManager.swapHotbarItems(indexA, indexB);
        });

        // Đăng ký bộ xử lý sự kiện để quay về Menu chính
        // Khi GameManager gọi hàm returnToMainMenu, phương thức showMainMenu sẽ được kích hoạt
        gameManager.setOnReturnToMainMenuHandler(() -> {
            showMainMenu();
        });

        // ----------------------------------------------------------------
        // 4. THIẾT LẬP GIAO DIỆN VÀ BẮT ĐẦU GAME
        // ----------------------------------------------------------------

        // Khởi tạo giao diện người dùng
        // Cần truyền Controller để lắng nghe sự kiện và các thông số đồ họa của nhân vật để hiển thị
        mainGameView.initUI(
                primaryStage,
                gameController,
                playerView.getSpriteContainer(),
                playerView.getDebugBoundingBox(),
                playerView.getDebugCenterDot(),
                playerView.getDebugRangeCircle(),
                playerView.getDebugCollisionHitbox()
        );

        // Gắn GameManager vào View để View có thể truy cập dữ liệu khi cần
        mainGameView.setGameManager(gameManager);

        // Nếu là chế độ Tải game, thực hiện nạp dữ liệu từ file lưu trữ trước khi bắt đầu vòng lặp game
        if (loadFromSave) {
            gameManager.loadGameData();
        }

        // Bắt đầu vòng lặp chính của trò chơi
        gameManager.startGame();

        // Bắt đầu phát nhạc nền
        gameManager.getAudioManager().playMusic(AssetPaths.BACKGROUND_MUSIC);

        // Cập nhật tiêu đề cửa sổ
        primaryStage.setTitle("Farm Simulation");
    }
}