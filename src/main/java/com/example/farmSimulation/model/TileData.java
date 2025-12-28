package com.example.farmSimulation.model;

import com.example.farmSimulation.config.ItemSpriteConfig;
import com.example.farmSimulation.config.WorldConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * Class (Model) chứa TOÀN BỘ trạng thái của một ô (tile) trong thế giới.
 * Thay thế cho việc chỉ lưu trữ một enum Tile đơn giản.
 */
@Getter
@Setter
public class TileData {
    // Đây sẽ là GRASS, SOIL (đất khô), hoặc SOIL_WET (đất ướt)
    private Tile baseTileType;

    // Dữ liệu trồng trọt
    private CropData cropData; // Cây đang trồng (null nếu không có)

    // Dữ liệu cây tự nhiên
    private TreeData treeData; // Cây tự nhiên (null nếu không có)

    // Dữ liệu hàng rào
    private FenceData fenceData; // Hàng rào (null nếu không có)

    // Logic tưới nước
    private boolean isWatered = false; // Đã được tưới chưa
    private long lastWateredTime = 0; // Thời điểm tưới lần cuối
    private long dryStartTime = 0; // Thời điểm đất bắt đầu khô (để tính cây chết)

    // Logic Phân bón
    private boolean isFertilized = false; // Đất có đang hiển thị lớp phân bón không
    private long fertilizerStartTime = 0; // Thời điểm bắt đầu bón phân

    // Visual
    private CropStatusIndicator statusIndicator = CropStatusIndicator.NONE;
    
    // Item trên đất (thịt rơi ra khi giết động vật)
    private ItemType groundItem; // Item trên đất (null nếu không có)
    private int groundItemAmount; // Số lượng item trên đất
    private int groundItemDurability; // Độ bền của item trên đất (0 nếu không có hoặc không áp dụng)
    
    // Lưu độ lệch của item so với góc trên trái của ô (để item không bị dính vào giữa ô)
    private double groundItemOffsetX;
    private double groundItemOffsetY;

    /**
     * Constructor mặc định, tạo ra một ô GRASS
     */
    public TileData() {
        this.baseTileType = Tile.GRASS; // Mặc định là CỎ
        setDefaultItemOffset();
    }

    /**
     * Constructor để tạo ô với loại cụ thể
     */
    public TileData(Tile baseTileType) {
        this.baseTileType = baseTileType;
        setDefaultItemOffset();
    }

    // Copy constructor để tạo bản sao (dùng cho InteractionManager)
    public TileData(TileData other) {
        this.baseTileType = other.baseTileType;
        this.cropData = other.cropData; // Lưu ý: CropData là object, nếu cần deep copy phải clone thêm
        this.treeData = other.treeData; // Lưu ý: TreeData là object, nếu cần deep copy phải clone thêm
        this.fenceData = other.fenceData; // Lưu ý: FenceData là object, nếu cần deep copy phải clone thêm
        this.isWatered = other.isWatered;
        this.lastWateredTime = other.lastWateredTime;
        this.dryStartTime = other.dryStartTime;
        this.isFertilized = other.isFertilized;
        this.fertilizerStartTime = other.fertilizerStartTime;
        this.statusIndicator = other.statusIndicator;
        this.groundItem = other.groundItem;
        this.groundItemAmount = other.groundItemAmount;
        this.groundItemDurability = other.groundItemDurability; // Copy độ bền của item trên đất
        
        // Copy offset
        this.groundItemOffsetX = other.groundItemOffsetX;
        this.groundItemOffsetY = other.groundItemOffsetY;
    }
    
    /**
     * Thiết lập offset mặc định (Căn giữa ô)
     * Dùng khi drop item từ túi hoặc khởi tạo
     */
    public void setDefaultItemOffset() {
        // Căn giữa: (TileSize - ItemSize) / 2
        this.groundItemOffsetX = (WorldConfig.TILE_SIZE - ItemSpriteConfig.ITEM_SPRITE_WIDTH) / 2.0;
        this.groundItemOffsetY = (WorldConfig.TILE_SIZE - ItemSpriteConfig.ITEM_SPRITE_HEIGHT) / 2.0;
    }
}