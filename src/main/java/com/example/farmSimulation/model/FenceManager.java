package com.example.farmSimulation.model;

/**
 * Lớp quản lý hệ thống hàng rào.
 * Ch chịu trách nhiệm xử lý logic tự động nối hình ảnh (auto-tiling) và trạng thái đóng hoặc mở của cổng rào.
 */
public class FenceManager {
    private final WorldMap worldMap;

    public FenceManager(WorldMap worldMap) {
        this.worldMap = worldMap;
    }

    /**
     * Tính toán chỉ số hình ảnh cho một ô hàng rào.
     * Hình ảnh hiển thị được xác định dựa trên sự tồn tại của hàng rào ở 4 hướng xung quanh (trên, phải, dưới, trái).
     * * @param col Cột của ô đất
     * @param row Hàng của ô đất
     * @return Chỉ số hình ảnh (từ 0 đến 15) đại diện cho trạng thái kết nối
     */
    public int calculateFencePattern(int col, int row) {
        // Kiểm tra sự tồn tại của hàng rào ở 4 hướng xung quanh
        boolean hasTop = isFence(col, row - 1);
        boolean hasRight = isFence(col + 1, row);
        boolean hasBottom = isFence(col, row + 1);
        boolean hasLeft = isFence(col - 1, row);

        // Tính toán chỉ số bằng cách sử dụng 4 bit tương ứng với 4 hướng
        int pattern = 0;
        if (hasTop) pattern |= 1 << 0;    // Bit 0 đại diện cho hướng trên
        if (hasRight) pattern |= 1 << 1;  // Bit 1 đại diện cho hướng phải
        if (hasBottom) pattern |= 1 << 2; // Bit 2 đại diện cho hướng dưới
        if (hasLeft) pattern |= 1 << 3;   // Bit 3 đại diện cho hướng trái

        return pattern;
    }

    /**
     * Kiểm tra xem một ô đất tại vị trí cụ thể có được coi là hàng rào để kết nối hay không.
     * Lưu ý: Hàng rào đang mở (trạng thái cổng) sẽ được xử lý như không phải là rào,
     * điều này giúp hình ảnh các hàng rào bên cạnh tự động ngắt kết nối với cổng đang mở.
     */
    private boolean isFence(int col, int row) {
        TileData data = worldMap.getTileData(col, row);
        // Chỉ xác nhận kết nối nếu ô đó là hàng rào và đang ở trạng thái đóng
        return data.getBaseTileType() == Tile.FENCE
                && data.getFenceData() != null
                && data.getFenceData().isSolid(); // isSolid là true nghĩa là hàng rào đang đóng
    }

    /**
     * Cập nhật lại hình ảnh kết nối cho một ô hàng rào cụ thể và cả các ô lân cận.
     * Việc cập nhật lân cận là cần thiết để chúng tự động nối lại hoặc ngắt ra khỏi ô hiện tại.
     */
    public void updateFencePattern(int col, int row) {
        // Cập nhật hình ảnh cho chính ô hiện tại nếu nó vẫn là hàng rào
        TileData data = worldMap.getTileData(col, row);
        if (data.getFenceData() != null && data.getBaseTileType() == Tile.FENCE) {
            int pattern = calculateFencePattern(col, row);
            data.getFenceData().setTilePattern(pattern);
            worldMap.setTileData(col, row, data);
        }

        // Cập nhật hình ảnh cho các ô xung quanh để đồng bộ hóa kết nối
        updateFencePatternAt(col, row - 1); // Hướng trên
        updateFencePatternAt(col + 1, row); // Hướng phải
        updateFencePatternAt(col, row + 1); // Hướng dưới
        updateFencePatternAt(col - 1, row); // Hướng trái
    }

    /**
     * Phương thức hỗ trợ để cập nhật hình ảnh tại một vị trí cụ thể.
     */
    private void updateFencePatternAt(int col, int row) {
        TileData data = worldMap.getTileData(col, row);
        // Chỉ thực hiện cập nhật nếu ô tại vị trí đó cũng là hàng rào
        if (data.getBaseTileType() == Tile.FENCE && data.getFenceData() != null) {
            int pattern = calculateFencePattern(col, row);
            data.getFenceData().setTilePattern(pattern);
            worldMap.setTileData(col, row, data);
        }
    }

    /**
     * Chuyển đổi trạng thái đóng hoặc mở của một ô hàng rào.
     */
    public void toggleFence(int col, int row) {
        TileData data = worldMap.getTileData(col, row);
        if (data.getFenceData() != null) {
            FenceData fence = data.getFenceData();
            fence.setOpen(!fence.isOpen());
            fence.setSolid(!fence.isOpen()); // Nếu mở thì không chặn đường, nếu đóng thì chặn vật thể
            worldMap.setTileData(col, row, data);

            // Vì trạng thái đóng mở ảnh hưởng đến logic kết nối nên cần cập nhật lại hình ảnh
            // Cập nhật cho chính ô này và các ô hàng xóm
            updateFencePattern(col, row);
        }
    }

    /**
     * Cập nhật lại hình ảnh kết nối cho toàn bộ hàng rào trên bản đồ.
     * Hàm này thường được gọi sau khi tải dữ liệu game để đảm bảo mọi kết nối hiển thị chính xác.
     */
    public void updateAllFencePatterns() {
        for (java.util.Map.Entry<Long, TileData> entry : worldMap.getTileDataMap().entrySet()) {
            TileData data = entry.getValue();
            if (data.getBaseTileType() == Tile.FENCE && data.getFenceData() != null) {
                long key = entry.getKey();
                int col = (int) (key >> 32);
                int row = (int) key;

                int pattern = calculateFencePattern(col, row);
                data.getFenceData().setTilePattern(pattern);
            }
        }
    }
}