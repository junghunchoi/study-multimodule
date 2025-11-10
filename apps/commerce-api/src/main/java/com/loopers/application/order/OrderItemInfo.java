package com.loopers.application.order;

import com.loopers.domain.order.OrderItem;

public record OrderItemInfo(
    Long id,
    Long productId,
    String productName,
    Integer quantity,
    Long price,
    Long totalPrice
) {
    public static OrderItemInfo from(OrderItem orderItem) {
        return new OrderItemInfo(
            orderItem.getId(),
            orderItem.getProduct().getId(),
            orderItem.getProduct().getName(),
            orderItem.getQuantity(),
            orderItem.getPrice(),
            orderItem.getTotalPrice()
        );
    }
}
