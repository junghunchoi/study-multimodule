package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;

import java.util.List;
import java.util.stream.Collectors;

public record OrderInfo(
    Long id,
    Long userId,
    OrderStatus status,
    Long totalAmount,
    List<OrderItemInfo> items
) {
    public static OrderInfo from(Order order) {
        return new OrderInfo(
            order.getId(),
            order.getUser().getId(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getOrderItems().stream()
                .map(OrderItemInfo::from)
                .collect(Collectors.toList())
        );
    }
}
