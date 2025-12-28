package com.example.farmSimulation.model;

import com.example.farmSimulation.config.AnimalConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * Class (Model) đại diện cho một con vật cụ thể trong game.
 * Lưu trữ trạng thái của động vật: vị trí, hướng, tuổi, độ đói, sản phẩm, v.v.
 */
@Getter
@Setter
public class Animal {

    // --- Thông tin cơ bản ---
    private AnimalType type; // Loại động vật
    private double x; // Tọa độ X thực trên bản đồ (lưu ý không phải tọa độ lưới tileX)
    private double y; // Tọa độ Y thực trên bản đồ (lưu ý không phải tọa độ lưới tileY)

    // Vị trí neo (Anchor) được sử dụng để giới hạn phạm vi di chuyển của con vật
    private double anchorX;
    private double anchorY;

    // --- Hướng & Di chuyển ---
    /**
     * Hướng nhìn của con vật: 0 = Xuống, 1 = Phải, 2 = Trái, 3 = Lên
     * Lưu ý: File ảnh sprite sheet đã có sẵn 4 hàng cho 4 hướng, nên không cần dùng code để lật ảnh
     */
    private int direction; // 0: Xuống, 1: Phải, 2: Trái, 3: Lên

    // --- Biến thể Sprite ---
    /**
     * Biến này dùng riêng cho EGG_ENTITY (trứng) để lưu trạng thái ngẫu nhiên (Đứng hoặc Nằm).
     * Giá trị là 0 hoặc 1.
     */
    private int variant;

    // --- Trạng thái ---
    private int age; // Tuổi thọ (dùng để tính toán lượng thịt rơi ra khi giết)
    private double hunger; // Chỉ số no (0-100, trong đó 100 là no hoàn toàn, 0 là chết đói)
    private boolean isDead; // Cờ đánh dấu con vật đã chết

    // --- Hành động ---
    /**
     * Các trạng thái hành động hiện tại: Đứng yên (IDLE), Đi bộ (WALK), Ăn (EAT)
     */
    public enum Action {
        IDLE,
        WALK,
        EAT
    }
    private Action currentAction;

    // --- Sản phẩm ---
    private long productionTimer; // Bộ đếm thời gian ngược để tạo sản phẩm (đơn vị nano giây)
    private boolean hasProduct; // Cờ đánh dấu sản phẩm đã sẵn sàng để thu hoạch

    // --- Sinh trưởng & Logic ---
    private long spawnTime; // Thời điểm sinh ra (nano giây) - dùng để tính tuổi và sự trưởng thành
    private long lastDirectionChangeTime; // Thời điểm lần cuối đổi hướng (nano giây)
    private long lastHungerUpdateTime; // Thời điểm lần cuối cập nhật chỉ số đói (nano giây)
    private long starvationStartTime; // Thời điểm bắt đầu bị đói (nano giây) - dùng để tính thời gian dẫn đến chết đói

    // Thời điểm kết thúc hành động hiện tại (dùng để khóa trạng thái không cho thực hiện hành động khác)
    private long actionEndTime;

    // Bộ đếm thời gian hồi chiêu sinh sản (Breeding Cooldown) - đơn vị nano giây
    private long breedingCooldownTimer;

    // LOGIC SINH SẢN
    private Animal breedingPartner; // Đối tượng bạn tình mà con vật đang nhắm tới để sinh sản
    private long matingStartTime; // Thời điểm bắt đầu quá trình giao phối (đứng yên cạnh nhau), bằng 0 nếu chưa bắt đầu

    /**
     * Hàm khởi tạo để tạo một con vật mới
     * @param type Loại động vật
     * @param x Tọa độ X ban đầu
     * @param y Tọa độ Y ban đầu
     */
    public Animal(AnimalType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;

        // Gán vị trí neo ban đầu bằng chính vị trí xuất hiện
        this.anchorX = x;
        this.anchorY = y;

        this.direction = 0; // Mặc định hướng nhìn xuống
        this.age = 0;
        this.hunger = com.example.farmSimulation.config.AnimalConfig.MAX_HUNGER; // Bắt đầu ở trạng thái no
        this.isDead = false;
        this.currentAction = Action.IDLE;
        this.productionTimer = 0;
        this.hasProduct = false;
        this.spawnTime = System.nanoTime();
        this.lastDirectionChangeTime = System.nanoTime();
        this.lastHungerUpdateTime = System.nanoTime();
        this.starvationStartTime = 0; // Chưa bị đói
        this.actionEndTime = 0; // Sẵn sàng thực hiện hành động ngay lập tức

        // Kiểm tra cấu hình xem có áp dụng thời gian hồi chiêu sinh sản ngay khi vừa sinh ra không
        if (AnimalConfig.ENABLE_BREEDING_COOLDOWN_ON_SPAWN) {
            // Gán timer bằng thời gian hiện tại để đảm bảo hiệu số (hiện tại - timer) nhỏ hơn thời gian hồi chiêu -> Chưa thể sinh sản
            this.breedingCooldownTimer = System.nanoTime();
        } else {
            this.breedingCooldownTimer = 0; // Có thể sinh sản ngay nếu đủ điều kiện (đã trưởng thành)
        }

        this.breedingPartner = null;
        this.matingStartTime = 0;

        // Chọn ngẫu nhiên trạng thái hiển thị cho trứng (0 hoặc 1)
        if (type == AnimalType.EGG_ENTITY) {
            this.variant = Math.random() < 0.5 ? 0 : 1;
        } else {
            this.variant = 0;
        }
    }

    /**
     * Kiểm tra xem động vật có đang đói không
     */
    public boolean isHungry() {
        return hunger < com.example.farmSimulation.config.AnimalConfig.HUNGER_WARNING_THRESHOLD;
    }

    /**
     * Kiểm tra xem động vật có đủ điều kiện tạo sản phẩm không
     */
    public boolean canProduce() {
        return type.canProduce() && !isDead && !isHungry();
    }

    /**
     * Kiểm tra xem động vật có thể lớn lên (trưởng thành) không
     */
    public boolean canGrow() {
        return type.canGrow() && !isDead;
    }

    /**
     * Tính tuổi của động vật theo đơn vị giây
     */
    public long getAgeInSeconds() {
        return (System.nanoTime() - spawnTime) / 1_000_000_000L;
    }

    /**
     * Tính toán số lượng thịt rơi ra khi con vật bị giết
     * Chỉ động vật trưởng thành mới rơi thịt
     */
    public int calculateMeatDrop() {
        // Con non không rơi thịt
        if (isBaby() || type == AnimalType.EGG_ENTITY) {
            return 0;
        }

        // Đã loại bỏ điều kiện kiểm tra tuổi tối thiểu để đảm bảo cứ giết là có thịt
        // Chỉ cần là con trưởng thành (không phải con non) thì luôn rơi ít nhất 1 đơn vị thịt
        int meat = (int) Math.min(age * com.example.farmSimulation.config.AnimalConfig.MEAT_RATE,
                com.example.farmSimulation.config.AnimalConfig.MAX_MEAT_DROP);
        return Math.max(meat, 1); // Đảm bảo rơi ít nhất 1 miếng thịt
    }

    /**
     * Kiểm tra xem động vật hiện tại có phải là con non không
     */
    public boolean isBaby() {
        return type == AnimalType.BABY_CHICKEN ||
                type == AnimalType.BABY_COW ||
                type == AnimalType.BABY_PIG ||
                type == AnimalType.BABY_SHEEP ||
                type == AnimalType.EGG_ENTITY;
    }

    /**
     * Lấy loại thịt tương ứng với loài động vật
     * Chỉ trả về loại thịt nếu là động vật trưởng thành
     */
    public ItemType getMeatType() {
        // Con non không có thịt
        if (isBaby()) {
            return null;
        }

        switch (type) {
            case CHICKEN:
                return ItemType.MEAT_CHICKEN;
            case COW:
                return ItemType.MEAT_COW;
            case PIG:
                return ItemType.MEAT_PIG;
            case SHEEP:
                return ItemType.MEAT_SHEEP;
            default:
                return null; // Trứng hoặc các loại khác không có thịt
        }
    }

    /**
     * Kiểm tra xem động vật có đang trong quá trình sinh sản hay không
     * (Bao gồm trạng thái đang đi tìm bạn tình HOẶC đang đứng giao phối)
     */
    public boolean isBreeding() {
        return this.breedingPartner != null || this.matingStartTime > 0;
    }

    /**
     * Lấy tỉ lệ kích thước hiển thị (Scale) dựa trên loại động vật
     * Đảm bảo con non được vẽ nhỏ hơn, sử dụng giá trị từ Config thay vì gán cứng
     */
    public double getVisualScale() {
        switch (type) {
            case BABY_COW:
                return AnimalConfig.SCALE_BABY_COW;
            case BABY_PIG:
                return AnimalConfig.SCALE_BABY_PIG;
            case BABY_SHEEP:
                return AnimalConfig.SCALE_BABY_SHEEP;
            case BABY_CHICKEN:
                return AnimalConfig.SCALE_BABY_CHICKEN;
            case EGG_ENTITY:
                return AnimalConfig.SCALE_EGG;
            case CHICKEN:
                return AnimalConfig.SCALE_CHICKEN;
            case COW:
                return AnimalConfig.SCALE_COW;
            case PIG:
                return AnimalConfig.SCALE_PIG;
            case SHEEP:
                return AnimalConfig.SCALE_SHEEP;
            default:
                return 1.0;
        }
    }
}