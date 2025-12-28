package com.example.farmSimulation.config;

// Chứa các hằng số cấu hình logic game, thời gian và vật lý
public class GameLogicConfig {

    // =================================================================================
    // PHẦN 1: CẤU HÌNH THỜI GIAN VÀ MÔI TRƯỜNG
    // =================================================================================

    // --- Chu kỳ Ngày & Đêm ---

    // Thời gian của một ngày trong game tính bằng giây
    public static final double DAY_CYCLE_DURATION_SECONDS = 600.0;

    // Thời gian bắt đầu game tính theo giây trong chu kỳ ngày
    // Công thức này đặt thời gian bắt đầu vào giữa chu kỳ, tức là 12 giờ trưa (thời điểm sáng nhất)
    public static final double PLAYER_START_TIME_SECONDS = DAY_CYCLE_DURATION_SECONDS / 2;

    // Hệ số tốc độ thời gian
    // Giá trị 1.0 là tốc độ bình thường. Tăng số này lên lớn hơn 1.0 sẽ làm thời gian trong game trôi nhanh hơn nữa
    public static final double GAME_TIME_SPEED_MULTIPLIER = 1.0;

    // --- Cấu hình Ánh sáng (Môi trường & Hiển thị) ---

    public static final double MIN_LIGHT_INTENSITY = 0.1; // Độ sáng môi trường tối thiểu (10%)
    public static final double MAX_DARKNESS_OPACITY = 0.8; // Độ tối che phủ tối đa (80%)

    public static final double DEFAULT_BRIGHTNESS = 1.0; // Độ sáng hiển thị mặc định (100%)
    public static final double MIN_BRIGHTNESS = 0.0; // Độ sáng hiển thị thấp nhất (0%)
    public static final double MAX_BRIGHTNESS = 1.0; // Độ sáng hiển thị cao nhất (100%)


    // =================================================================================
    // PHẦN 2: CẤU HÌNH NHÂN VẬT (VẬT LÝ & KHỞI TẠO)
    // =================================================================================

    // Tốc độ di chuyển của nhân vật (đơn vị pixel trên giây)
    public static final double PLAYER_SPEED = 300.0;

    // Tọa độ khởi tạo của người chơi khi bắt đầu game
    public static final double PLAYER_START_X = 0.0;
    public static final double PLAYER_START_Y = 0.0;

    // Số tiền ban đầu mà người chơi sở hữu
    public static final double PLAYER_START_MONEY = 500.0;


    // =================================================================================
    // PHẦN 3: HỆ THỐNG THỂ LỰC (STAMINA)
    // =================================================================================

    // --- Chỉ số Thể lực cơ bản ---

    public static final double PLAYER_MAX_STAMINA = 100.0; // Thể lực tối đa mặc định
    public static final double PLAYER_START_STAMINA = 100.0; // Thể lực khi bắt đầu game

    // Ngưỡng thể lực thấp để bắt đầu áp dụng hình phạt (15% của thể lực tối đa)
    public static final double STAMINA_PENALTY_THRESHOLD = 15.0;

    // Hệ số giảm tốc độ di chuyển khi thể lực xuống dưới ngưỡng thấp (chỉ còn 50% tốc độ gốc)
    public static final double STAMINA_SPEED_PENALTY_MULTIPLIER = 0.5;

    // Tốc độ hồi phục thể lực mỗi giây khi nhân vật đứng yên
    public static final double STAMINA_RECOVERY_RATE = 1.0;

    // Lượng thể lực bị tiêu hao mỗi giây khi nhân vật đang chạy
    public static final double STAMINA_DRAIN_RUNNING = 2.0;

    // --- Chi phí Thể lực cho từng hành động ---

    public static final double STAMINA_COST_HOE = 2.0; // Tiêu hao khi cuốc đất
    public static final double STAMINA_COST_WATERING_CAN = 2.0; // Tiêu hao khi tưới nước
    public static final double STAMINA_COST_AXE = 3.0; // Tiêu hao khi chặt cây
    public static final double STAMINA_COST_PICKAXE = 3.0; // Tiêu hao khi đào đá
    public static final double STAMINA_COST_SHOVEL = 2.0; // Tiêu hao khi xúc đất
    public static final double STAMINA_COST_PLANT = 1.0; // Tiêu hao khi gieo hạt
    public static final double STAMINA_COST_FERTILIZER = 1.0; // Tiêu hao khi bón phân


    // =================================================================================
    // PHẦN 4: HỆ THỐNG TIẾN TRÌNH (LEVEL & XP)
    // =================================================================================

    // --- Cấu hình Level & XP cơ bản ---

    public static final int PLAYER_START_LEVEL = 1; // Cấp độ khởi đầu
    public static final double PLAYER_START_XP = 0.0; // Điểm kinh nghiệm khởi đầu

    // Điểm kinh nghiệm cần thiết để lên cấp độ tiếp theo (cấp 2)
    public static final double PLAYER_START_XP_TO_NEXT_LEVEL = 100.0;

    // Hệ số nhân điểm kinh nghiệm yêu cầu cho mỗi cấp độ sau (tăng 1.2 lần mỗi cấp)
    public static final double XP_MULTIPLIER_PER_LEVEL = 1.2;

    // Lượng thể lực tối đa được cộng thêm khi lên cấp
    public static final double STAMINA_INCREASE_PER_LEVEL = 10.0;

    // --- Điểm kinh nghiệm nhận được từ hành động ---

    public static final double XP_GAIN_HARVEST = 10.0; // Khi thu hoạch nông sản
    public static final double XP_GAIN_PLANT = 2.0; // Khi gieo hạt
    public static final double XP_GAIN_WATER = 1.0; // Khi tưới nước
    public static final double XP_GAIN_HOE = 1.0; // Khi cuốc đất
    public static final double XP_GAIN_AXE = 3.0; // Khi chặt cây hoặc phá hàng rào
    public static final double XP_GAIN_SHOVEL = 2.0; // Khi xúc đất hoặc xóa cây
    public static final double XP_GAIN_PICKAXE = 3.0; // Khi đào đá


    // =================================================================================
    // PHẦN 5: CẤU HÌNH CÔNG CỤ VÀ HÀNH ĐỘNG
    // =================================================================================
    // Phần này quy định phạm vi tương tác, thời gian thực hiện và độ bền của từng công cụ

    // --- Tay không (Mặc định) ---
    public static final double HAND_INTERACTION_RANGE = WorldConfig.TILE_SIZE * 1.2;

    // --- Thời gian chung ---
    // Thời gian chờ cho các hành động không có hoạt ảnh cụ thể
    public static final long GENERIC_ACTION_DURATION_MS = 800;

    // --- Cuốc (Hoe) ---
    public static final double HOE_INTERACTION_RANGE = WorldConfig.TILE_SIZE * 1.1; // Phạm vi tương tác
    public static final int HOE_REPETITIONS = 4; // Số lần lặp lại động tác trong một lần dùng
    public static final long HOE_DURATION_PER_REPETITION_MS = 500; // Thời gian cho mỗi lần lặp (mili giây)
    public static final int MAX_DURABILITY_HOE = 20; // Độ bền tối đa

    // --- Bình tưới (Watering Can) ---
    public static final double WATERING_CAN_INTERACTION_RANGE = WorldConfig.TILE_SIZE * 1.1;
    public static final int WATERING_CAN_REPETITIONS = 1;
    public static final long WATERING_CAN_DURATION_PER_REPETITION_MS = 1000; // Giữ tưới trong 1 giây
    public static final int MAX_WATER_CAPACITY = 10; // Dung tích nước tối đa

    // --- Cúp/Cuốc chim (Pickaxe) ---
    public static final double PICKAXE_INTERACTION_RANGE = WorldConfig.TILE_SIZE * 1.2;
    public static final int PICKAXE_REPETITIONS = 3;
    public static final long PICKAXE_DURATION_PER_REPETITION_MS = 300;
    public static final int MAX_DURABILITY_PICKAXE = 30;

    // --- Xẻng (Shovel) ---
    public static final double SHOVEL_INTERACTION_RANGE = WorldConfig.TILE_SIZE * 1.3;
    public static final int SHOVEL_REPETITIONS = 1;
    public static final long SHOVEL_DURATION_PER_REPETITION_MS = 1000;
    public static final int MAX_DURABILITY_SHOVEL = 25;

    // --- Rìu (Axe) ---
    public static final double AXE_INTERACTION_RANGE = WorldConfig.TILE_SIZE * 1.0;
    public static final int AXE_REPETITIONS = 3;
    public static final long AXE_DURATION_PER_REPETITION_MS = 600;
    public static final int MAX_DURABILITY_AXE = 30;

    // --- Kiếm (Sword) ---
    public static final double SWORD_INTERACTION_RANGE = WorldConfig.TILE_SIZE * 1.5;
    public static final int SWORD_REPETITIONS = 3;
    public static final long SWORD_DURATION_PER_REPETITION_MS = 600;
    public static final int MAX_DURABILITY_SWORD = 30;

    // --- Hành động trồng trọt ---
    public static final double PLANT_INTERACTION_RANGE = WorldConfig.TILE_SIZE * 0.85;
    public static final long PLANT_DURATION_MS = 1200; // Thời gian thực hiện gieo hạt

    // --- Hành động bón phân ---
    public static final double FERTILIZER_INTERACTION_RANGE = WorldConfig.TILE_SIZE * 1.0;
    public static final long FERTILIZER_DURATION_MS = 1400; // Thời gian thực hiện bón phân


    // =================================================================================
    // PHẦN 6: TỐI ƯU HÓA HIỆU NĂNG VÀ HỆ THỐNG KHÁC
    // =================================================================================

    // --- Tối ưu hóa hiệu năng (Performance) ---

    // Tần suất cập nhật trạng thái cây trồng tính bằng mili giây
    // Thay vì cập nhật mỗi khung hình (16ms), game chỉ cập nhật mỗi 100ms để giảm tải cho CPU
    public static final long CROP_UPDATE_INTERVAL_MS = 100;

    // Tần suất cập nhật bản đồ hiển thị, chỉ cập nhật khi thực sự cần thiết
    public static final long MAP_UPDATE_INTERVAL_MS = 50;

    // Số lượng ô đất trồng tối đa được xử lý cập nhật trong một khung hình
    // Giới hạn này giúp tránh hiện tượng giật lag khi có quá nhiều cây trồng cùng lúc
    public static final int MAX_CROPS_UPDATE_PER_FRAME = 100;

    // --- Cơ chế rơi vật phẩm (Item Drop) ---

    // Bán kính tìm kiếm ô trống xung quanh vị trí rơi để đặt vật phẩm (1 nghĩa là tìm trong phạm vi 3x3 ô)
    public static final int ITEM_DROP_SEARCH_RADIUS = 1;

    // Độ phân tán ngẫu nhiên khi vật phẩm rơi xuống đất tính bằng pixel
    // Giúp vật phẩm không nằm cứng nhắc ngay chính giữa ô mà có độ lệch tự nhiên
    public static final double ITEM_DROP_SCATTER_RANGE = 24.0;

    // --- Kết thúc Game và Cheat ---

    // Thời gian chờ trước khi hiển thị màn hình Game Over sau khi nhân vật kiệt sức hoặc thất bại
    public static final double GAME_OVER_DELAY_SECONDS = 3.0;

    // Số tiền nhận được khi sử dụng mã gian lận
    public static final double CHEAT_MONEY_AMOUNT = 10000.0;

    private GameLogicConfig() {}
}