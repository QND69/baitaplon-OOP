package com.example.farmSimulation.model;

import com.example.farmSimulation.config.CropConfig;
import com.example.farmSimulation.config.GameLogicConfig;

// Class quản lý logic và trạng thái của hệ thống cây trồng
public class CropManager {
    private final WorldMap worldMap;
    private WeatherManager weatherManager; // Quản lý thời tiết
    private TimeManager timeManager; // Quản lý thời gian trong game

    // Thời điểm cập nhật frame trước đó
    private long lastUpdateTime = 0;

    // Thời điểm cập nhật logic cây trồng lần cuối để kiểm soát tần suất xử lý
    private long lastCropUpdateTimeMs = 0;

    // Chỉ số theo dõi các ô đất đã xử lý, giúp phân chia khối lượng công việc qua nhiều frame
    private int lastProcessedIndex = 0;

    public CropManager(WorldMap worldMap) {
        this.worldMap = worldMap;
    }

    /**
     * Thiết lập đối tượng quản lý thời tiết
     */
    public void setWeatherManager(WeatherManager weatherManager) {
        this.weatherManager = weatherManager;
    }

    /**
     * Thiết lập đối tượng quản lý thời gian
     */
    public void setTimeManager(TimeManager timeManager) {
        this.timeManager = timeManager;
    }

    /**
     * Đưa ô đất về trạng thái đất khô mặc định.
     * Thường được sử dụng khi cây chết hoặc cần làm mới ô đất.
     */
    private void resetTileToSoil(TileData data, long currentTime) {
        data.setBaseTileType(Tile.SOIL);
        data.setCropData(null);
        data.setWatered(false);
        data.setFertilized(false);
        data.setDryStartTime(currentTime); // Đặt lại bộ đếm thời gian để bắt đầu tính giờ mọc cỏ
        data.setFertilizerStartTime(0);
        data.setStatusIndicator(CropStatusIndicator.NONE);
    }

    public boolean updateCrops(long currentTime) {
        boolean mapNeedsRedraw = false;
        long currentTimeMs = currentTime / 1_000_000;

        // Tính toán thời gian trôi qua giữa hai lần gọi hàm update
        if (lastUpdateTime == 0) {
            lastUpdateTime = currentTime;
            lastCropUpdateTimeMs = currentTimeMs;
            return false; // Bỏ qua frame đầu tiên
        }
        long deltaTime = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;

        // Chỉ thực hiện cập nhật logic cây trồng theo chu kỳ quy định để tối ưu hiệu năng
        if (currentTimeMs - lastCropUpdateTimeMs < GameLogicConfig.CROP_UPDATE_INTERVAL_MS) {
            return false;
        }
        lastCropUpdateTimeMs = currentTimeMs;

        // Lấy danh sách tất cả các ô đất để lọc ra những ô cần xử lý
        java.util.Collection<TileData> allTiles = worldMap.getAllTileData();
        java.util.ArrayList<TileData> tilesToUpdate = new java.util.ArrayList<>();

        // Chỉ thêm vào danh sách xử lý nếu ô đất có cây, đang ướt, đã bón phân hoặc là đất thường
        for (TileData data : allTiles) {
            if (data.getCropData() != null ||
                    data.isWatered() ||
                    data.isFertilized() ||
                    data.getBaseTileType() == Tile.SOIL) {
                tilesToUpdate.add(data);
            }
        }

        // Giới hạn số lượng ô đất được cập nhật trong mỗi frame để tránh giật lag
        int processedCount = 0;
        int startIndex = lastProcessedIndex;

        for (int i = 0; i < tilesToUpdate.size() && processedCount < GameLogicConfig.MAX_CROPS_UPDATE_PER_FRAME; i++) {
            int index = (startIndex + i) % tilesToUpdate.size();
            TileData data = tilesToUpdate.get(index);
            boolean changed = false;
            processedCount++;

            // --- LOGIC TƯỚI NƯỚC VÀ ĐẤT ---
            // Tự động tưới ướt đất có cây nếu trời đang mưa
            if (weatherManager != null && weatherManager.isRaining() &&
                    data.getBaseTileType() == Tile.SOIL && data.getCropData() != null && !data.isWatered()) {
                data.setWatered(true);
                data.setBaseTileType(Tile.SOIL_WET);
                data.setLastWateredTime(currentTime);
                data.setDryStartTime(0); // Xóa thời gian bắt đầu khô do đất đã ướt
                changed = true;
            }

            if (data.isWatered()) {
                // Đất ướt sẽ tự khô nếu trời không mưa
                if (weatherManager == null || !weatherManager.isRaining()) {
                    if (currentTimeMs - data.getLastWateredTime() / 1_000_000 > CropConfig.SOIL_DRY_TIME_MS) {
                        data.setWatered(false);
                        data.setBaseTileType(Tile.SOIL);
                        data.setDryStartTime(currentTime); // Bắt đầu tính giờ khô
                        changed = true;
                    }
                }
            } else if (data.getBaseTileType() == Tile.SOIL) { // Trường hợp đất khô
                if (data.getCropData() == null) { // Đất hoang không có cây
                    // Khởi tạo thời gian khô nếu chưa có
                    if (data.getDryStartTime() == 0) {
                        data.setDryStartTime(currentTime);
                    } else {
                        // Đất khô để lâu không canh tác sẽ mọc cỏ trở lại
                        if ((currentTime - data.getDryStartTime()) / 1_000_000 > CropConfig.SOIL_REVERT_TIME_MS) {
                            data.setBaseTileType(Tile.GRASS);
                            data.setDryStartTime(0);
                            changed = true;
                        }
                    }
                } else { // Có cây trên đất khô
                    // Đảm bảo thời gian khô được ghi nhận để tính toán logic chết cây
                    if (data.getDryStartTime() == 0) {
                        data.setDryStartTime(currentTime);
                    }
                    long dryDuration = (currentTime - data.getDryStartTime()) / 1_000_000;
                    long deathTime = CropConfig.WATER_WARNING_DELAY_MS + CropConfig.CROP_DEATH_TIME_MS;

                    // Nếu cây còn sống nhưng đất khô quá giới hạn chịu đựng thì cây sẽ chết
                    if (data.getCropData().getGrowthStage() != -1 && dryDuration > deathTime) {
                        resetTileToSoil(data, currentTime);
                        changed = true;
                    }
                }
            }

            // --- LOGIC PHÂN BÓN ---
            if (data.isFertilized()) {
                // Hiệu ứng hình ảnh phân bón sẽ biến mất sau thời gian quy định
                if ((currentTime - data.getFertilizerStartTime()) / 1_000_000 > CropConfig.FERTILIZER_EFFECT_DURATION_MS) {
                    data.setFertilized(false);
                    changed = true;
                }
            }

            // --- LOGIC PHÁT TRIỂN CỦA CÂY ---
            // Cây chỉ lớn khi tồn tại, chưa chết và chưa đạt giai đoạn trưởng thành tối đa
            CropData crop = data.getCropData();
            if (crop != null && crop.getGrowthStage() != -1 && crop.getGrowthStage() < crop.getType().getMaxStages() - 1) {

                // Xác định cây có đủ điều kiện nước để lớn hay không
                // Cây vẫn có thể lớn nếu đất khô nhưng chưa vượt quá thời gian cảnh báo
                boolean canGrowWater = data.isWatered();
                if (!data.isWatered() && data.getDryStartTime() > 0) {
                    long dryDuration = (currentTime - data.getDryStartTime()) / 1_000_000;
                    if (dryDuration <= CropConfig.WATER_WARNING_DELAY_MS) {
                        canGrowWater = true;
                    }
                }

                if (canGrowWater) {
                    // Kiểm tra hiệu lực tăng tốc của phân bón
                    // Phân bón có tác dụng khi còn hiển thị hoặc trong thời gian ân hạn sau khi hình ảnh biến mất
                    boolean hasBuff = false;
                    if (data.getFertilizerStartTime() > 0) {
                        long timeSinceFertilizer = (currentTime - data.getFertilizerStartTime()) / 1_000_000;
                        if (timeSinceFertilizer <= (CropConfig.FERTILIZER_EFFECT_DURATION_MS + CropConfig.FERTILIZER_WARNING_DELAY_MS)) {
                            hasBuff = true;
                        }
                    }

                    // Tính toán thời gian cần thiết cho mỗi giai đoạn phát triển, có áp dụng buff phân bón
                    double timePerStage = hasBuff ? (CropConfig.TIME_PER_GROWTH_STAGE_MS / CropConfig.FERTILIZER_BUFF) : CropConfig.TIME_PER_GROWTH_STAGE_MS;

                    // Khởi tạo hệ số tốc độ phát triển cơ bản
                    double growthSpeedMultiplier = CropConfig.BASE_GROWTH_SPEED;

                    // Kiểm tra xem có phải ban đêm không dựa trên cường độ ánh sáng
                    boolean isNight = false;
                    if (timeManager != null) {
                        double lightIntensity = timeManager.getCurrentLightIntensity();
                        isNight = (lightIntensity < CropConfig.NIGHT_LIGHT_THRESHOLD);
                    }

                    // Giảm tốc độ phát triển vào ban đêm
                    if (isNight) {
                        growthSpeedMultiplier *= CropConfig.NIGHT_GROWTH_SPEED_MULTIPLIER;
                    }

                    // Giảm tốc độ phát triển khi trời mưa
                    if (weatherManager != null && weatherManager.isRaining()) {
                        growthSpeedMultiplier *= CropConfig.RAIN_GROWTH_SPEED_MULTIPLIER;
                    }

                    // Điều chỉnh thời gian cần thiết cho mỗi giai đoạn dựa trên tổng hệ số tốc độ
                    timePerStage = timePerStage / growthSpeedMultiplier;

                    long timeElapsedMs = (currentTime - crop.getPlantTime()) / 1_000_000;
                    int targetStage = (int) (timeElapsedMs / timePerStage);
                    targetStage = Math.min(targetStage, crop.getType().getMaxStages() - 1);

                    if (targetStage > crop.getGrowthStage()) {
                        crop.setGrowthStage(targetStage);
                        // Nếu cây đã chín hoàn toàn, loại bỏ trạng thái phân bón ngay lập tức
                        if (targetStage >= crop.getType().getMaxStages() - 1) {
                            data.setFertilized(false);
                        }
                        changed = true;
                    }
                } else {
                    // Cơ chế đóng băng thời gian phát triển khi cây thiếu nước quá lâu
                    // Bằng cách cộng thêm khoảng thời gian trôi qua vào thời điểm gieo trồng,
                    // hiệu số giữa hiện tại và thời điểm gieo trồng sẽ được giữ nguyên, khiến cây không lớn thêm.
                    long newPlantTime = crop.getPlantTime() + deltaTime;
                    crop.setPlantTime(newPlantTime);
                    // Không đánh dấu changed là true vì giai đoạn phát triển không thay đổi
                }
            }

            // --- CẬP NHẬT BIỂU TƯỢNG TRẠNG THÁI ---
            CropStatusIndicator newStatus = calculateStatus(data, crop, currentTime);
            if (data.getStatusIndicator() != newStatus) {
                data.setStatusIndicator(newStatus);
                changed = true;
            }

            if (changed) {
                mapNeedsRedraw = true;
            }
        }

        // Lưu lại vị trí chỉ số hiện tại để tiếp tục xử lý các ô còn lại trong frame tiếp theo
        if (tilesToUpdate.size() > 0) {
            lastProcessedIndex = (startIndex + processedCount) % tilesToUpdate.size();
        }

        return mapNeedsRedraw;
    }

    private CropStatusIndicator calculateStatus(TileData data, CropData crop, long currentTime) {
        if (crop == null) return CropStatusIndicator.NONE;
        if (crop.getGrowthStage() == -1) return CropStatusIndicator.DEAD;
        if (crop.getGrowthStage() >= crop.getType().getMaxStages() - 1) return CropStatusIndicator.READY_TO_HARVEST;

        // Cảnh báo nước: Đất khô vượt quá thời gian cảnh báo cho phép
        boolean waterWarning = false;
        if (!data.isWatered() && data.getDryStartTime() > 0) {
            long dryDuration = (currentTime - data.getDryStartTime()) / 1_000_000;
            if (dryDuration > CropConfig.WATER_WARNING_DELAY_MS) {
                waterWarning = true;
            }
        }

        // Cảnh báo phân bón
        boolean fertilizerWarning = false;

        // Chỉ hiển thị cảnh báo phân bón khi cây đã đủ lớn và chưa chín
        if (crop.getGrowthStage() >= CropConfig.MIN_GROWTH_STAGE_FOR_FERTILIZER && crop.getGrowthStage() < crop.getType().getMaxStages() - 1) {
            if (data.isFertilized()) {
                // Đất đang có phân bón thì không cần cảnh báo
                fertilizerWarning = false;
            } else {
                // Đất không có lớp phân bón hoặc đã tan hết
                if (data.getFertilizerStartTime() > 0) {
                    long timeSinceStart = (currentTime - data.getFertilizerStartTime()) / 1_000_000;
                    // Nếu đã qua thời gian hiệu lực và thời gian cảnh báo thì cần bón lại
                    if (timeSinceStart > (CropConfig.FERTILIZER_EFFECT_DURATION_MS + CropConfig.FERTILIZER_WARNING_DELAY_MS)) {
                        fertilizerWarning = true;
                    }
                } else {
                    // Cây chưa từng được bón phân nên cần bón
                    fertilizerWarning = true;
                }
            }
        }

        if (waterWarning && fertilizerWarning) return CropStatusIndicator.NEED_WATER_AND_FERTILIZER;
        if (waterWarning) return CropStatusIndicator.NEEDS_WATER;
        if (fertilizerWarning) return CropStatusIndicator.NEEDS_FERTILIZER;

        return CropStatusIndicator.NONE;
    }
}