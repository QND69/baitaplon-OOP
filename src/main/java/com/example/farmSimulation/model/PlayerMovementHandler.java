package com.example.farmSimulation.model;

import com.example.farmSimulation.config.GameLogicConfig;
import com.example.farmSimulation.controller.GameController;
import com.example.farmSimulation.view.MainGameView;
import com.example.farmSimulation.view.PlayerView;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerMovementHandler {
    private final Player mainPlayer;
    private final PlayerView playerView;
    private final GameController gameController;
    private final Camera camera;
    private final MainGameView mainGameView;
    private CollisionManager collisionManager; // Quản lý va chạm, đối tượng này sẽ được thiết lập từ bên ngoài

    public PlayerMovementHandler(Player mainPlayer, PlayerView playerView, GameController gameController, Camera camera, MainGameView mainGameView) {
        this.mainPlayer = mainPlayer;
        this.playerView = playerView;
        this.gameController = gameController;
        this.camera = camera;
        this.mainGameView = mainGameView;
    }

    // Hàm cập nhật chính xử lý việc di chuyển
    public void update(double deltaTime) {
        // Xử lý tín hiệu đầu vào
        Point2D movementDelta = handleInput(deltaTime); // Trả về vector di chuyển dx và dy đã được nhân với thời gian delta
        double dx = movementDelta.getX();
        double dy = movementDelta.getY();

        // Cập nhật dữ liệu logic và giao diện hiển thị
        updatePlayerState(dx, dy); // Cập nhật trạng thái đứng yên hoặc đi bộ
        updatePlayerPosition(dx, dy); // Cập nhật vị trí mới
    }

    /**
     * Xử lý tín hiệu đầu vào và trả về vector di chuyển đã được tính toán theo thời gian
     * @param deltaTime Thời gian trôi qua tính bằng giây, giúp việc di chuyển độc lập với tốc độ khung hình
     */
    private Point2D handleInput(double deltaTime) {
        // Tính toán hướng di chuyển theo trục X và Y
        double dx = 0;
        double dy = 0;

        // Chỉ cho phép di chuyển khi người chơi không thực hiện hành động khác
        if (mainPlayer.getState() == PlayerView.PlayerState.IDLE ||
                mainPlayer.getState() == PlayerView.PlayerState.WALK) {

            // Tính tốc độ di chuyển dựa trên thời gian trôi qua để đảm bảo mượt mà
            double movementSpeed = GameLogicConfig.PLAYER_SPEED * deltaTime;

            // Áp dụng hình phạt giảm tốc độ nếu thể lực thấp
            if (mainPlayer.hasStaminaPenalty()) {
                movementSpeed *= GameLogicConfig.STAMINA_SPEED_PENALTY_MULTIPLIER;
                // Tốc độ hoạt ảnh cũng sẽ giảm tương ứng
            }

            if (gameController.isKeyPressed(KeyCode.W)) { // Di chuyển người chơi lên trên
                dy += movementSpeed;
            }
            if (gameController.isKeyPressed(KeyCode.S)) { // Di chuyển người chơi xuống dưới
                dy -= movementSpeed;
            }
            if (gameController.isKeyPressed(KeyCode.A)) { // Di chuyển người chơi sang trái
                dx += movementSpeed;
            }
            if (gameController.isKeyPressed(KeyCode.D)) { // Di chuyển người chơi sang phải
                dx -= movementSpeed;
            }
        }
        return new Point2D(dx, dy);
    }

    /**
     * Cập nhật trạng thái logic và yêu cầu giao diện hiển thị cập nhật hình ảnh tương ứng
     */
    private void updatePlayerState(double dx, double dy) {
        // Xác định trạng thái logic
        // Chỉ thay đổi khi không thực hiện hành động đặc biệt
        if (mainPlayer.getState() == PlayerView.PlayerState.IDLE ||
                mainPlayer.getState() == PlayerView.PlayerState.WALK) {
            if (dx != 0 || dy != 0) {
                mainPlayer.setState(PlayerView.PlayerState.WALK);
            } else {
                mainPlayer.setState(PlayerView.PlayerState.IDLE);
            }

            // Xác định hướng nhìn
            if (dy > 0) {
                mainPlayer.setDirection(PlayerView.Direction.UP);
            } else if (dy < 0) {
                mainPlayer.setDirection(PlayerView.Direction.DOWN);
            } else if (dx > 0) {
                mainPlayer.setDirection(PlayerView.Direction.LEFT);
            } else if (dx < 0) {
                mainPlayer.setDirection(PlayerView.Direction.RIGHT);
            }

            // Yêu cầu giao diện cập nhật hình ảnh theo trạng thái và hướng mới
            playerView.setState(mainPlayer.getState(), mainPlayer.getDirection());
        }
    }

    /**
     * Cập nhật vị trí người chơi và camera
     */
    private void updatePlayerPosition(double dx, double dy) {
        // Cập nhật bản đồ nếu có sự di chuyển
        if (dx != 0 || dy != 0) {
            // Tính toán vị trí mới của người chơi
            double newX = mainPlayer.getTileX() - dx;
            double newY = mainPlayer.getTileY() - dy;

            // Kiểm tra va chạm trước khi thực hiện di chuyển
            if (collisionManager != null) {
                // Các giá trị kích thước hộp va chạm đã được điều chỉnh theo tỷ lệ trong cấu hình
                double hitboxWidth = com.example.farmSimulation.config.PlayerSpriteConfig.COLLISION_BOX_WIDTH;
                double hitboxHeight = com.example.farmSimulation.config.PlayerSpriteConfig.COLLISION_BOX_HEIGHT;

                // Tâm điểm kiểm tra va chạm nằm tại vị trí chân của nhân vật
                // Sử dụng kích thước đã thay đổi tỷ lệ để tính toán chính xác
                double scaledPlayerWidth = com.example.farmSimulation.config.PlayerSpriteConfig.BASE_PLAYER_FRAME_WIDTH * com.example.farmSimulation.config.PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;
                double scaledPlayerHeight = com.example.farmSimulation.config.PlayerSpriteConfig.BASE_PLAYER_FRAME_HEIGHT * com.example.farmSimulation.config.PlayerSpriteConfig.BASE_PLAYER_FRAME_SCALE;

                double feetCenterX = newX + (scaledPlayerWidth / 2.0);

                // Tính toán tọa độ Y dựa trên chiều cao đã thay đổi tỷ lệ
                // Công thức bao gồm vị trí Y cộng chiều cao nhân vật, trừ đi một nửa hộp va chạm và khoảng cách đệm
                double feetCenterY = newY + scaledPlayerHeight
                        - (hitboxHeight / 2.0)
                        - com.example.farmSimulation.config.PlayerSpriteConfig.COLLISION_BOX_BOTTOM_PADDING;

                // Kiểm tra va chạm tại vị trí chân mới tính toán
                if (collisionManager.checkCollision(feetCenterX, feetCenterY, hitboxWidth, hitboxHeight)) {
                    return;
                }
            }

            // Nếu không có va chạm thì thực hiện di chuyển bình thường
            // Cập nhật vị trí camera
            camera.move(dx, dy);

            // Cập nhật tọa độ logic của người chơi
            mainPlayer.setTileX(newX);
            mainPlayer.setTileY(newY);

            // Yêu cầu vẽ lại bản đồ dựa trên vị trí mới
            // Truyền vào giá trị dịch chuyển của khung nhìn thế giới
            mainGameView.updateMap(camera.getWorldOffsetX(), camera.getWorldOffsetY(), false);
        }
    }
}