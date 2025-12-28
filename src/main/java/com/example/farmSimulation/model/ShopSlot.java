package com.example.farmSimulation.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Lớp đại diện cho một ô hàng trong cửa hàng.
 * Chứa thông tin về loại vật phẩm, số lượng tồn kho và mức giảm giá hiện tại.
 */
@Getter
@Setter
public class ShopSlot {
    private ItemType itemType;
    private int quantity; // Số lượng hàng còn lại trong kho
    private double discountRate; // Tỷ lệ giảm giá. Ví dụ: 0.0 là giá gốc, 0.2 tương ứng với giảm 20%

    public ShopSlot(ItemType itemType, int quantity, double discountRate) {
        this.itemType = itemType;
        this.quantity = quantity;
        this.discountRate = discountRate;
    }

    /**
     * Tính toán và lấy giá bán cuối cùng sau khi đã áp dụng giảm giá.
     * Công thức: Giá gốc * (1 - Tỷ lệ giảm giá)
     */
    public double getPrice() {
        int basePrice = itemType.getBuyPrice();
        return basePrice * (1.0 - discountRate);
    }

    /**
     * Lấy giá gốc ban đầu của vật phẩm khi chưa có giảm giá
     */
    public int getBasePrice() {
        return itemType.getBuyPrice();
    }

    /**
     * Kiểm tra xem vật phẩm này có đang được giảm giá hay không
     * Trả về true nếu tỷ lệ giảm giá lớn hơn 0
     */
    public boolean isOnSale() {
        return discountRate > 0.0;
    }

    /**
     * Kiểm tra xem vật phẩm đã bán hết hay chưa
     * Trả về true nếu số lượng bằng hoặc nhỏ hơn 0
     */
    public boolean isSoldOut() {
        return quantity <= 0;
    }
}