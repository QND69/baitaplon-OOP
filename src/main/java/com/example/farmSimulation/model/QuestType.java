package com.example.farmSimulation.model;

/**
 * Enum định nghĩa các loại quest
 */
public enum QuestType {
    /**
     * Quest thu hoạch: Thu hoạch các sản phẩm từ cây trồng
     */
    HARVEST,

    /**
     * Quest hành động: Thực hiện các hành động như chặt cây
     */
    CHOP_TREE,

    /**
     * Quest bán hàng: Bán các item cho shop
     */
    SELL,

    /**
     * Quest mua hàng: Mua các item từ shop
     */
    BUY
}