package com.example.farmSimulation.model;

import com.example.farmSimulation.config.GameLogicConfig;
import com.example.farmSimulation.view.MainGameView;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimeManager {

    // --- Các biến lưu trữ cấu hình thời gian ---
    private double gameTimeSeconds; // Thời gian hiện tại trong game tính bằng giây
    private final double DAY_CYCLE_DURATION_SECONDS; // Độ dài của một ngày trong game tính bằng giây thực tế
    private final double MIN_LIGHT_INTENSITY;
    private double currentLightIntensity; // Cường độ ánh sáng hiện tại (từ 0.0 đến 1.0)
    private int currentDay; // Ngày hiện tại (bắt đầu tính từ ngày 1)
    private int lastCheckedDay; // Ngày được kiểm tra lần cuối để tránh kích hoạt sự kiện nhiều lần

    // Biến lưu thời gian cập nhật trước đó tính bằng nanoseconds để tính toán delta time
    private long lastUpdateTimeNanos = 0;

    private final MainGameView mainGameView;

    /**
     * Hàm khởi tạo.
     * Tự động lấy các giá trị thiết lập từ cấu hình GameLogicConfig.
     */
    public TimeManager(MainGameView mainGameView) {
        this.mainGameView = mainGameView;

        // Lấy giá trị khởi tạo từ tệp cấu hình
        this.gameTimeSeconds = GameLogicConfig.PLAYER_START_TIME_SECONDS;
        this.DAY_CYCLE_DURATION_SECONDS = GameLogicConfig.DAY_CYCLE_DURATION_SECONDS;
        this.MIN_LIGHT_INTENSITY = GameLogicConfig.MIN_LIGHT_INTENSITY;
        this.currentLightIntensity = 1.0; // Khởi tạo trạng thái ban đầu là trời sáng
        this.currentDay = 1; // Bắt đầu tính từ ngày thứ nhất
        this.lastCheckedDay = 1; // Khởi tạo giá trị ngày kiểm tra lần cuối
    }

    // Phương thức cập nhật chính cho hệ thống thời gian
    public void update() {

        // Tự động tính toán thời gian chênh lệch (delta time) để đảm bảo tốc độ game độc lập với tốc độ khung hình (FPS)
        long currentNanos = System.nanoTime();
        if (lastUpdateTimeNanos == 0) {
            lastUpdateTimeNanos = currentNanos;
            return; // Bỏ qua frame đầu tiên do chưa có dữ liệu delta
        }

        // Tính thời gian thực tế đã trôi qua (đổi từ nanoseconds sang giây)
        double deltaTime = (currentNanos - lastUpdateTimeNanos) / 1_000_000_000.0;
        lastUpdateTimeNanos = currentNanos;

        // Giới hạn thời gian chênh lệch tối đa là 0.1 giây
        // Việc này giúp tránh các lỗi logic nghiêm trọng khi game bị giật lag nặng hoặc khi dừng tại điểm ngắt (breakpoint)
        if (deltaTime > 0.1) deltaTime = 0.1;

        updateGameTime(deltaTime);
        updateDayCycle();
    }

    /**
     * Tính toán cường độ ánh sáng dựa trên tiến độ của chu kỳ ngày đêm.
     * Cường độ biến thiên từ 1.0 (sáng nhất) xuống 0.0 (tối nhất).
     */
    private void updateDayCycle() {
        double cycleProgress = (this.gameTimeSeconds % this.DAY_CYCLE_DURATION_SECONDS) / this.DAY_CYCLE_DURATION_SECONDS;

        // Chuyển đổi tỷ lệ tiến độ thành cường độ ánh sáng (từ 0.0 đến 1.0)
        // Sử dụng hàm sin để tạo hiệu ứng chuyển đổi mượt mà giữa ngày và đêm
        // y=sin(2xπ-π/2) với x là cycleProgress (0<=x<=1)
        double radians = cycleProgress * 2 * Math.PI - (Math.PI / 2.0); // Chuyển sang radian
        double lightIntensity = (Math.sin(radians) + 1.0) / 2.0; // Chuyển khoảng từ [-1,1] thành [0,1]

        // Điều chỉnh cường độ ánh sáng dựa trên cấu hình độ sáng tối thiểu
        lightIntensity = this.MIN_LIGHT_INTENSITY + (1.0 - this.MIN_LIGHT_INTENSITY) * lightIntensity;

        // Cập nhật cường độ ánh sáng hiện tại
        this.currentLightIntensity = lightIntensity;

        // Cập nhật hiệu ứng ánh sáng trên giao diện người dùng
        mainGameView.updateLighting(lightIntensity);
    }

    // Cập nhật thời gian trong game dựa trên thời gian thực tế trôi qua
    private void updateGameTime(double deltaTime) {
        // Tăng thời gian game dựa trên thời gian thực tế và hệ số tốc độ được cấu hình
        this.gameTimeSeconds += deltaTime * GameLogicConfig.GAME_TIME_SPEED_MULTIPLIER;

        // Cập nhật ngày hiện tại dựa trên tổng thời gian đã trôi qua
        updateCurrentDay();

        // Định dạng thời gian hiển thị dạng HH:MM theo chu kỳ 12 giờ
        double timeInCurrentDay = this.gameTimeSeconds % this.DAY_CYCLE_DURATION_SECONDS;

        // Ánh xạ thời gian trong game sang mốc thời gian hiển thị (theo thang 24 giờ)
        // Công thức tỷ lệ: thời gian trong ngày / tổng thời gian một ngày = giờ thực tế / 24
        double gameHours = (timeInCurrentDay / this.DAY_CYCLE_DURATION_SECONDS) * 24.0;

        int hours24 = (int) gameHours;
        int minutes = (int) ((gameHours - hours24) * 60);

        // Chuyển đổi từ định dạng 24 giờ sang 12 giờ
        int hours12 = hours24 % 12;
        if (hours12 == 0) {
            hours12 = 12; // Hiển thị 12 giờ cho cả mốc 0 giờ (nửa đêm) và 12 giờ (trưa)
        }

        String timeString = String.format("%d:%02d", hours12, minutes); // Định dạng chuỗi: "12:10" hoặc "1:05"

        // Cập nhật ngày và giờ lên giao diện người dùng
        mainGameView.updateTimer(this.currentDay, timeString);
    }

    /**
     * Cập nhật số thứ tự ngày hiện tại dựa trên tổng thời gian chơi.
     */
    private void updateCurrentDay() {
        // Tính toán ngày hiện tại bằng cách chia tổng giây cho độ dài một ngày
        // Cộng thêm 1 vì ngày bắt đầu đếm từ 1
        this.currentDay = (int)(this.gameTimeSeconds / this.DAY_CYCLE_DURATION_SECONDS) + 1;
    }

    /**
     * Kiểm tra xem ngày mới đã bắt đầu chưa kể từ lần kiểm tra cuối cùng.
     * @return true nếu đã sang ngày mới, false nếu vẫn là ngày cũ.
     */
    public boolean hasNewDayStarted() {
        if (this.currentDay > this.lastCheckedDay) {
            this.lastCheckedDay = this.currentDay;
            return true; // Đã sang ngày mới
        }
        return false; // Vẫn là ngày cũ
    }

    /**
     * Kiểm tra xem hiện tại có phải là ban đêm không.
     * @return true nếu cường độ ánh sáng thấp (ban đêm).
     */
    public boolean isNight() {
        return currentLightIntensity < 0.5; // Được coi là ban đêm khi cường độ ánh sáng dưới 50%
    }

    /**
     * Thiết lập thời gian game.
     * Thường được sử dụng khi tải lại dữ liệu game từ file lưu trữ.
     */
    public void setGameTime(double seconds) {
        this.gameTimeSeconds = seconds;
        updateCurrentDay(); // Tính toán lại ngày hiện tại ngay lập tức
        this.lastCheckedDay = this.currentDay; // Đặt lại ngày kiểm tra để tránh kích hoạt lại các sự kiện hàng ngày không mong muốn
    }
}