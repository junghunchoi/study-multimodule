package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemInfo;
import com.loopers.domain.order.OrderStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderV1Dto {

    public record OrderResponse(
        Long id,
        Long userId,
        OrderStatus status,
        Long totalAmount,
        List<OrderItemResponse> items
    ) {
        public static OrderResponse from(OrderInfo info) {
            return new OrderResponse(
                info.id(),
                info.userId(),
                info.status(),
                info.totalAmount(),
                info.items().stream()
                    .map(OrderItemResponse::from)
                    .collect(Collectors.toList())
            );
        }
    }

    public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        Long price,
        Long totalPrice
    ) {
        public static OrderItemResponse from(OrderItemInfo info) {
            return new OrderItemResponse(
                info.id(),
                info.productId(),
                info.productName(),
                info.quantity(),
                info.price(),
                info.totalPrice()
            );
        }
    }

    public record CreateOrderRequest(Long userId, List<OrderItemRequest> items) {}

    public record OrderItemRequest(Long productId, Integer quantity) {}

    public record OrderListResponse(List<OrderResponse> orders) {
        public static OrderListResponse from(List<OrderInfo> orderInfos) {
            return new OrderListResponse(
                orderInfos.stream()
                    .map(OrderResponse::from)
                    .collect(Collectors.toList())
            );
        }
    }

    public static Map<Long, Integer> toProductQuantityMap(List<OrderItemRequest> items) {
        return items.stream()
            .collect(Collectors.toMap(
                OrderItemRequest::productId,
                OrderItemRequest::quantity
            ));
    }
}
