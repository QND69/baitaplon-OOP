package com.example.farmSimulation.model;

import java.util.Collection;
import java.util.HashMap;


// Class này lưu trữ TOÀN BỘ dữ liệu bản đồ (Model).
// Đây là bản đồ hữu hạn
public class WorldMap {
    private HashMap<Long, TileData> tileDataMap; // Lưu trữ loại ô tại mỗi tọa độ [hàng][cột] và loại ô

    public WorldMap() {
        tileDataMap = new HashMap<>();
    }

    //hàm tạo khóa
    /* Dùng phép dịch bit để ép 2 số int 32-bit col và row vào 1 key long 64-bit
    0xffffffffL là mask để lấy 32 bit thấp và loại bỏ sign-extension khi xử lý int như unsigned dưới dạng long*/
    private long toKey(int col, int row) {
        return ((long) col << 32) | (row & 0xffffffffL);
    }

    /**
     * Hàm quan trọng: Lấy TOÀN BỘ DỮ LIỆU của một ô.
     * Nếu ô đó không tồn tại trong map (ví dụ: vùng đất mới),
     * nó sẽ tự động tạo một TileData (GRASS) mặc định, lưu lại và trả về.
     */
    public TileData getTileData(int col, int row) {
        long key = toKey(col, row);
        // Sử dụng computeIfAbsent để code gọn hơn
        // computeIfAbsent là một method của Map (Java 8+).
        // computeIfAbsent: Nếu 'path' chưa có trong cache,
        // nó sẽ chạy hàm lambda (v -> new Image(...)) để tải ảnh,
        // sau đó tự động 'put' vào cache và return ảnh đó.
        // Cú pháp nếu ko dùng ->: V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
        return tileDataMap.computeIfAbsent(key, k -> new TileData(Tile.GRASS));
    }

    /**
     * Hàm helper để lưu lại TileData sau khi logic (ví dụ: InteractionManager)
     * đã thay đổi nó.
     */
    public void setTileData(int col, int row, TileData data) {
        tileDataMap.put(toKey(col, row), data);
    }

    /**
     * Trả về tất cả các TileData đang được lưu trữ.
     * (Dùng cho CropManager để cập nhật cây trồng)
     */
    public Collection<TileData> getAllTileData() {
        return tileDataMap.values();
    }

    /**
     * Trả về map gốc để truy cập cả Key (tọa độ) và Value (dữ liệu).
     * Cần thiết cho việc lưu game và cập nhật Fence toàn cục.
     */
    public java.util.Map<Long, TileData> getTileDataMap() {
        return tileDataMap;
    }
}